package com.example.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telecom.DisconnectCause
import android.os.Build
import android.util.Log
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.CallsManager
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Android Telecom bridge. Supabase remains authoritative and LiveKit remains the media transport. */
object LineCallTelecom {
  private const val TAG = "LineCallTelecom"
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var callsManager: CallsManager? = null
  @Volatile private var activeCallId: String? = null
  @Volatile private var callControlScope: CallControlScope? = null
  @Volatile private var telecomAnswered = false
  @Volatile private var localDisconnect = false
  private val _availableEndpoints = MutableStateFlow<List<CallEndpointCompat>>(emptyList())
  private val _currentEndpoint = MutableStateFlow<CallEndpointCompat?>(null)
  private val _isMuted = MutableStateFlow(false)
  val availableEndpoints = _availableEndpoints.asStateFlow()
  val currentEndpoint = _currentEndpoint.asStateFlow()
  val isMuted = _isMuted.asStateFlow()

  @Synchronized private fun manager(context: Context): CallsManager {
    callsManager?.let { return it }
    return CallsManager(context.applicationContext).also {
      it.registerAppWithTelecom(CallsManager.CAPABILITY_BASELINE)
      callsManager = it
    }
  }

  fun hasActiveCall(): Boolean = callControlScope != null

  fun registerIncoming(context: Context, callId: String, callerName: String, callerPhone: String,
    onAnswer: () -> Unit, onDisconnect: (Int) -> Unit) =
    register(context, callId, callerName, callerPhone, CallAttributesCompat.DIRECTION_INCOMING, onAnswer, onDisconnect)

  fun registerOutgoing(context: Context, callId: String, calleeName: String?, calleePhone: String,
    onDisconnect: (Int) -> Unit) =
    register(context, callId, calleeName.orEmpty().ifBlank { calleePhone }, calleePhone,
      CallAttributesCompat.DIRECTION_OUTGOING, {}, onDisconnect)

  private fun register(context: Context, callId: String, displayName: String, phone: String,
    direction: Int, onAnswer: () -> Unit, onDisconnect: (Int) -> Unit) {
    // Core-Telecom supports Android 8.0+; retain the existing call UI/notification path below that.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    if (callId.isBlank() || activeCallId == callId) return
    if (activeCallId != null) {
      Log.w(TAG, "Ignoring Telecom registration while another call is active")
      return
    }
    activeCallId = callId
    scope.launch {
      try {
        val attributes = CallAttributesCompat(
          displayName.ifBlank { phone.ifBlank { "Line Call" } },
          Uri.parse(if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) "sip:$phone" else "tel:$phone"),
          direction,
          CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
          0
        )
        manager(context).addCall(
          attributes,
          { _: Int ->
            telecomAnswered = true
            onAnswer()
          },
          { cause: DisconnectCause ->
            if (localDisconnect) localDisconnect = false else onDisconnect(cause.code)
          },
          { },
          { }
        ) {
          callControlScope = this
          launch { currentCallEndpoint.collect { _currentEndpoint.value = it } }
          launch { availableEndpoints.collect { _availableEndpoints.value = it } }
          launch { isMuted.collect { _isMuted.value = it } }
        }
      } catch (error: Exception) {
        Log.w(TAG, "Core-Telecom registration failed; notification fallback remains available", error)
        clearCall(callId)
      } finally {
        clearCall(callId)
      }
    }
  }

  /** Completes a locally accepted call; avoids answering twice after a Telecom surface answered it. */
  suspend fun confirmAnswered() {
    val control = callControlScope ?: return
    if (!telecomAnswered) {
      val result = control.answer(CallAttributesCompat.CALL_TYPE_AUDIO_CALL)
      if (result is androidx.core.telecom.CallControlResult.Error) {
        Log.w(TAG, "Telecom answer failed: ${result.errorCode}")
        return
      }
    }
    // Telecom's onAnswer callback already marks the call active when a remote
    // surface answered it. A local answer uses answer(), which also activates it.
    telecomAnswered = false
  }

  suspend fun setActiveAfterRemoteAnswer() {
    val result = callControlScope?.setActive() ?: return
    if (result is androidx.core.telecom.CallControlResult.Error) Log.w(TAG, "Telecom activation failed: ${result.errorCode}")
  }

  suspend fun requestEndpointChange(endpoint: CallEndpointCompat) {
    callControlScope?.requestEndpointChange(endpoint)
  }

  fun endpointForLegacyId(id: Int): CallEndpointCompat? =
    _availableEndpoints.value.firstOrNull { it.identifier.hashCode() == id }

  fun disconnectLocal() {
    val control = callControlScope ?: return
    localDisconnect = true
    scope.launch {
      runCatching { control.disconnect(DisconnectCause(DisconnectCause.LOCAL)) }
      clearCall(activeCallId)
    }
  }

  private fun clearCall(callId: String?) {
    if (callId == null || activeCallId == callId) {
      activeCallId = null
      callControlScope = null
      telecomAnswered = false
      _availableEndpoints.value = emptyList()
      _currentEndpoint.value = null
      _isMuted.value = false
    }
  }

  private fun launchCallAction(context: Context, callId: String, action: String) {
    val intent = Intent(context, MainActivity::class.java).apply {
      putExtra("linecall_call_id", callId)
      putExtra("linecall_call_action", action)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    runCatching { context.startActivity(intent) }.onFailure {
      Log.w(TAG, "Could not launch app for Telecom action $action", it)
    }
  }

  fun incomingAnswerIntent(context: Context, callId: String) = launchCallAction(context, callId, "answer")
  fun incomingDeclineIntent(context: Context, callId: String) = launchCallAction(context, callId, "decline")
  fun endIntent(context: Context, callId: String) = launchCallAction(context, callId, "end")
}
