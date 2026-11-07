package com.example.services

import android.app.Application
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import com.example.data.local.CallRecordDao
import com.example.data.model.ActiveCallSession
import com.example.data.model.CallRecord
import com.example.data.model.CallSessionState
import com.example.data.model.CallType
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

sealed class CallInitiationResult {
  data class Started(val session: ActiveCallSession) : CallInitiationResult()
  data class InvalidNumber(val reason: String) : CallInitiationResult()
  data class BackendNotConnected(val e164: String, val message: String) : CallInitiationResult()
  data class UserNotRegistered(val e164: String) : CallInitiationResult()
}

sealed class CallEvent {
  data class CallStarted(val session: ActiveCallSession) : CallEvent()
  data class CallEnded(val durationSeconds: Int) : CallEvent()
  data class Error(val message: String) : CallEvent()
}

/**
 * Real one-to-one voice calls. Supabase coordinates call state and issues short-lived
 * LiveKit participant tokens; LiveKit carries the encrypted WebRTC audio.
 */
class CallingService(
  private val application: Application,
  private val callRecordDao: CallRecordDao,
  private val userProfileService: UserProfileService,
  private val supabase: LineCallSupabaseClient,
  private val coroutineScope: CoroutineScope
) {
  private val _activeCall = MutableStateFlow<ActiveCallSession?>(null)
  val activeCall: StateFlow<ActiveCallSession?> = _activeCall.asStateFlow()

  private val _callEvents = MutableSharedFlow<CallEvent>()
  val callEvents: SharedFlow<CallEvent> = _callEvents.asSharedFlow()

  private val audioManager = application.getSystemService(AudioManager::class.java)
  private var liveKitRoom: Room? = null
  private var incomingRingtone: Ringtone? = null
  private var timerJob: Job? = null
  private var incomingPollJob: Job? = null
  private var outgoingPollJob: Job? = null

  fun validateNumber(e164: String): Boolean = userProfileService.validateE164(e164)

  fun startIncomingCallPolling() {
    if (incomingPollJob?.isActive == true) return
    incomingPollJob = coroutineScope.launch(Dispatchers.IO) {
      while (isActive) {
        if (_activeCall.value == null) {
          runCatching {
            val response = supabase.callFunction(JSONObject().put("action", "poll"))
            val incoming = response.optJSONObject("incoming_call")
            if (incoming != null && _activeCall.value == null) {
              val session = ActiveCallSession(
                callId = incoming.getString("call_id"),
                contactName = incoming.optString("display_name").takeIf { it.isNotBlank() && it != "null" },
                phoneNumber = incoming.optString("phone_number"),
                state = CallSessionState.RINGING,
                isIncoming = true
              )
              _activeCall.value = session
              startIncomingRingtone()
              _callEvents.emit(CallEvent.CallStarted(session))
            }
          }
        }
        delay(2500)
      }
    }
  }

  fun stopIncomingCallPolling() {
    incomingPollJob?.cancel()
    incomingPollJob = null
  }

  suspend fun initiateCall(
    e164PhoneNumber: String,
    contactName: String?,
    forceDevelopmentTestMode: Boolean = false
  ): CallInitiationResult {
    if (!validateNumber(e164PhoneNumber)) {
      return CallInitiationResult.InvalidNumber(
        "Enter a valid international phone number including its country code."
      )
    }
    if (forceDevelopmentTestMode) {
      return CallInitiationResult.BackendNotConnected(
        e164PhoneNumber,
        "Test calls have been disabled. Line Call only displays calls that are connected through the real calling service."
      )
    }
    if (_activeCall.value != null) {
      return CallInitiationResult.BackendNotConnected(e164PhoneNumber, "Finish your current call before starting another.")
    }

    return try {
      val response = supabase.callFunction(
        JSONObject().put("action", "initiate").put("phone_number", e164PhoneNumber)
      )
      val session = ActiveCallSession(
        callId = response.getString("call_id"),
        contactName = response.optString("display_name").takeIf { it.isNotBlank() && it != "null" } ?: contactName,
        phoneNumber = response.optString("phone_number", e164PhoneNumber),
        state = CallSessionState.DIALING,
        isIncoming = false
      )
      _activeCall.value = session
      outgoingPollJob?.cancel()
      outgoingPollJob = coroutineScope.launch(Dispatchers.IO) { monitorOutgoingCall(session) }
      _callEvents.emit(CallEvent.CallStarted(session))
      CallInitiationResult.Started(session)
    } catch (error: Exception) {
      val message = error.message ?: "Unable to start the call."
      if (message.contains("not registered", ignoreCase = true)) {
        CallInitiationResult.UserNotRegistered(e164PhoneNumber)
      } else {
        CallInitiationResult.BackendNotConnected(e164PhoneNumber, message)
      }
    }
  }

  private suspend fun monitorOutgoingCall(session: ActiveCallSession) {
    while (coroutineScope.isActive && _activeCall.value?.callId == session.callId) {
      try {
        val response = supabase.callFunction(
          JSONObject().put("action", "status").put("call_id", session.callId)
        )
        val status = response.optJSONObject("call")?.optString("status").orEmpty()
        when (status) {
          "accepted" -> {
            connectToLiveKit(session.callId, session)
            return
          }
          "declined", "missed", "ended", "failed" -> {
            _callEvents.emit(CallEvent.Error(
              when (status) {
                "declined" -> "The person declined your call."
                "missed" -> "The person did not answer."
                else -> "The call has ended."
              }
            ))
            stopCallSession()
            return
          }
        }
      } catch (error: Exception) {
        _callEvents.emit(CallEvent.Error(error.message ?: "Connection lost while waiting for an answer."))
        stopCallSession()
        return
      }
      delay(1800)
    }
  }

  fun acceptIncomingCall() {
    val current = _activeCall.value ?: return
    if (!current.isIncoming || current.state != CallSessionState.RINGING) return
    stopIncomingRingtone()
    coroutineScope.launch(Dispatchers.IO) {
      try {
        supabase.callFunction(JSONObject().put("action", "accept").put("call_id", current.callId))
        connectToLiveKit(current.callId, current)
      } catch (error: Exception) {
        _callEvents.emit(CallEvent.Error(error.message ?: "Could not accept this call."))
        stopCallSession()
      }
    }
  }

  fun declineIncomingCall() {
    val current = _activeCall.value ?: return
    stopIncomingRingtone()
    coroutineScope.launch(Dispatchers.IO) {
      runCatching {
        supabase.callFunction(JSONObject().put("action", "decline").put("call_id", current.callId))
      }
      callRecordDao.insertCallRecord(
        CallRecord(
          phoneNumber = current.phoneNumber,
          contactName = current.contactName,
          type = CallType.MISSED,
          durationSeconds = 0
        )
      )
      stopCallSession()
    }
  }

  private suspend fun connectToLiveKit(callId: String, session: ActiveCallSession) {
    val credentials = supabase.callFunction(
      JSONObject()
        .put("action", "token")
        .put("call_id", callId)
        .put("participant_name", userProfileService.currentUser.value?.displayName.orEmpty())
    )
    val serverUrl = credentials.getString("livekit_url")
    val participantToken = credentials.getString("token")

    val room = LiveKit.create(application.applicationContext)
    liveKitRoom = room
    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
    audioManager.isSpeakerphoneOn = false

    room.connect(serverUrl, participantToken)
    room.localParticipant.setMicrophoneEnabled(true)

    if (_activeCall.value?.callId == callId) {
      _activeCall.value = session.copy(state = CallSessionState.CONNECTED)
      startDurationTimer()
    }
  }

  fun toggleMute() {
    val current = _activeCall.value ?: return
    val muted = !current.isMuted
    _activeCall.value = current.copy(isMuted = muted)
    coroutineScope.launch {
      runCatching { liveKitRoom?.localParticipant?.setMicrophoneEnabled(!muted) }
        .onFailure { _callEvents.emit(CallEvent.Error("Could not change microphone state.")) }
    }
  }

  fun toggleSpeaker() {
    val current = _activeCall.value ?: return
    val speakerOn = !current.isSpeakerOn
    _activeCall.value = current.copy(isSpeakerOn = speakerOn)
    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
    audioManager.isSpeakerphoneOn = speakerOn
  }

  fun endCall() {
    val current = _activeCall.value ?: return
    stopIncomingRingtone()
    coroutineScope.launch(Dispatchers.IO) {
      val duration = current.durationSeconds
      runCatching {
        supabase.callFunction(JSONObject().put("action", "end").put("call_id", current.callId))
      }
      runCatching { liveKitRoom?.disconnect() }
      liveKitRoom = null
      audioManager.isSpeakerphoneOn = false
      audioManager.mode = AudioManager.MODE_NORMAL
      callRecordDao.insertCallRecord(
        CallRecord(
          phoneNumber = current.phoneNumber,
          contactName = current.contactName,
          type = if (current.isIncoming) CallType.INCOMING else CallType.OUTGOING,
          durationSeconds = duration
        )
      )
      _callEvents.emit(CallEvent.CallEnded(duration))
      stopCallSession()
    }
  }

  private fun startIncomingRingtone() {
    runCatching {
      val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
      incomingRingtone = RingtoneManager.getRingtone(application, ringtoneUri)
      incomingRingtone?.play()
    }
  }

  private fun stopIncomingRingtone() {
    runCatching { incomingRingtone?.stop() }
    incomingRingtone = null
  }

  private fun startDurationTimer() {
    timerJob?.cancel()
    timerJob = coroutineScope.launch {
      while (isActive) {
        delay(1000)
        _activeCall.value = _activeCall.value?.let { current ->
          if (current.state == CallSessionState.CONNECTED) {
            current.copy(durationSeconds = current.durationSeconds + 1)
          } else current
        }
      }
    }
  }

  private fun stopCallSession() {
    stopIncomingRingtone()
    timerJob?.cancel()
    timerJob = null
    outgoingPollJob?.cancel()
    outgoingPollJob = null
    coroutineScope.launch { runCatching { liveKitRoom?.disconnect() }; liveKitRoom = null }
    _activeCall.value = null
    audioManager.isSpeakerphoneOn = false
    audioManager.mode = AudioManager.MODE_NORMAL
  }
}
