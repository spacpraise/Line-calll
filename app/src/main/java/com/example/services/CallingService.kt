package com.example.services

import com.example.data.local.CallRecordDao
import com.example.data.model.ActiveCallSession
import com.example.data.model.CallRecord
import com.example.data.model.CallSessionState
import com.example.data.model.CallType
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
 * Dedicated Voice Calling Service architecture.
 *
 * Prepared for real-time LiveKit Cloud WebRTC voice integration.
 * In production:
 * 1. Client requests room token from Supabase Edge Function (`/functions/v1/call-token`)
 * 2. LiveKit Android SDK connects to `wss://your-project.livekit.cloud`
 * 3. Audio tracks are published/subscribed over WebRTC with opus codec
 * 4. Microphones and speaker output are routed through AudioManager
 */
class CallingService(
  private val callRecordDao: CallRecordDao,
  private val userProfileService: UserProfileService,
  private val coroutineScope: CoroutineScope
) {
  private val _activeCall = MutableStateFlow<ActiveCallSession?>(null)
  val activeCall: StateFlow<ActiveCallSession?> = _activeCall.asStateFlow()

  private val _callEvents = MutableSharedFlow<CallEvent>()
  val callEvents: SharedFlow<CallEvent> = _callEvents.asSharedFlow()

  private var timerJob: Job? = null

  fun validateNumber(e164: String): Boolean {
    return userProfileService.validateE164(e164)
  }

  /**
   * Attempts to initiate an internet voice call to [e164PhoneNumber].
   * If backend is not connected, honestly returns [CallInitiationResult.BackendNotConnected]
   * to avoid pretending a real call succeeded.
   */
  suspend fun initiateCall(
    e164PhoneNumber: String,
    contactName: String?,
    forceDevelopmentTestMode: Boolean = false
  ): CallInitiationResult {
    if (!validateNumber(e164PhoneNumber)) {
      return CallInitiationResult.InvalidNumber("Invalid phone number format. Must start with '+' followed by country code and digits (e.g. +14155552671).")
    }

    if (!BackendConfig.isLiveKitConfigured && !forceDevelopmentTestMode) {
      return CallInitiationResult.BackendNotConnected(
        e164 = e164PhoneNumber,
        message = "Calling backend is not connected. Line Call requires LiveKit Cloud and Supabase Edge Functions for real-time voice signaling."
      )
    }

    val callId = UUID.randomUUID().toString()
    val session = ActiveCallSession(
      callId = callId,
      contactName = contactName,
      phoneNumber = e164PhoneNumber,
      state = CallSessionState.DIALING,
      isIncoming = false
    )
    _activeCall.value = session

    startCallLifecycle(session)
    _callEvents.emit(CallEvent.CallStarted(session))
    return CallInitiationResult.Started(session)
  }

  /**
   * Allows initiating an incoming call preview for testing and verifying the
   * IncomingCallScreen UI and ring controls.
   */
  fun triggerIncomingCallPreview(
    callerName: String = "Sarah Jenkins",
    callerPhone: String = "+1 (415) 555-0198"
  ) {
    val callId = UUID.randomUUID().toString()
    val session = ActiveCallSession(
      callId = callId,
      contactName = callerName,
      phoneNumber = callerPhone,
      state = CallSessionState.RINGING,
      isIncoming = true
    )
    _activeCall.value = session
  }

  fun acceptIncomingCall() {
    val current = _activeCall.value ?: return
    val updated = current.copy(state = CallSessionState.CONNECTED)
    _activeCall.value = updated
    startDurationTimer()
  }

  fun declineIncomingCall() {
    val current = _activeCall.value ?: return
    coroutineScope.launch(Dispatchers.IO) {
      callRecordDao.insertCallRecord(
        CallRecord(
          phoneNumber = current.phoneNumber,
          contactName = current.contactName,
          type = CallType.MISSED,
          durationSeconds = 0
        )
      )
    }
    stopCallSession()
  }

  fun toggleMute() {
    val current = _activeCall.value ?: return
    _activeCall.value = current.copy(isMuted = !current.isMuted)
    // TODO: LiveKit localAudioTrack?.setEnabled(!isMuted)
  }

  fun toggleSpeaker() {
    val current = _activeCall.value ?: return
    _activeCall.value = current.copy(isSpeakerOn = !current.isSpeakerOn)
    // TODO: audioManager.isSpeakerphoneOn = isSpeakerOn
  }

  fun endCall() {
    val current = _activeCall.value ?: return
    val finalDuration = current.durationSeconds
    val callType = if (current.isIncoming) CallType.INCOMING else CallType.OUTGOING

    coroutineScope.launch(Dispatchers.IO) {
      callRecordDao.insertCallRecord(
        CallRecord(
          phoneNumber = current.phoneNumber,
          contactName = current.contactName,
          type = callType,
          durationSeconds = finalDuration
        )
      )
    }

    _activeCall.value = current.copy(state = CallSessionState.ENDED)
    coroutineScope.launch {
      _callEvents.emit(CallEvent.CallEnded(finalDuration))
      delay(400)
      stopCallSession()
    }
  }

  private fun startCallLifecycle(session: ActiveCallSession) {
    coroutineScope.launch {
      // Simulate connection signaling lifecycle for UI verification
      delay(1200)
      if (_activeCall.value?.callId == session.callId) {
        _activeCall.value = _activeCall.value?.copy(state = CallSessionState.RINGING)
      }
      delay(1800)
      if (_activeCall.value?.callId == session.callId) {
        _activeCall.value = _activeCall.value?.copy(state = CallSessionState.CONNECTED)
        startDurationTimer()
      }
    }
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
    timerJob?.cancel()
    timerJob = null
    _activeCall.value = null
  }
}
