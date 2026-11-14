package com.example.services

import android.app.Application
import android.media.AudioManager
import android.media.AudioDeviceInfo
import android.os.Build
import android.provider.Settings
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import com.example.data.local.CallRecordDao
import com.example.data.model.ActiveCallSession
import com.example.data.model.CallRecord
import com.example.data.model.CallAudioRoute
import com.example.data.model.CallSessionState
import com.example.data.model.CallType
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.track.RemoteTrackPublication
import io.livekit.android.room.track.Track
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
import java.util.concurrent.ConcurrentHashMap

enum class CallConnectionState { IDLE, CONNECTING, CONNECTED, RECONNECTING, ENDED }

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
  private val _connectionState = MutableStateFlow(CallConnectionState.IDLE)
  val connectionState: StateFlow<CallConnectionState> = _connectionState.asStateFlow()

  private val _callEvents = MutableSharedFlow<CallEvent>()
  val callEvents: SharedFlow<CallEvent> = _callEvents.asSharedFlow()

  private val _audioRoutes = MutableStateFlow<List<CallAudioRoute>>(emptyList())
  val audioRoutes: StateFlow<List<CallAudioRoute>> = _audioRoutes.asStateFlow()
  private val _selectedAudioRouteId = MutableStateFlow<Int?>(null)
  val selectedAudioRouteId: StateFlow<Int?> = _selectedAudioRouteId.asStateFlow()

  private val audioManager = application.getSystemService(AudioManager::class.java)
  private var liveKitRoom: Room? = null
  @Volatile private var telecomMuted = false
  @Volatile private var telecomInactive = false
  private val telecomPausedAudioPublications = ConcurrentHashMap.newKeySet<RemoteTrackPublication>()
  private var liveKitEventsJob: Job? = null
  private var endingCallId: String? = null
  private var incomingRingtone: Ringtone? = null
  private var ringbackTone: ToneGenerator? = null
  private var ringbackJob: Job? = null
  private var timerJob: Job? = null
  private var incomingPollJob: Job? = null
  private var outgoingPollJob: Job? = null
  private var incomingWatchJob: Job? = null
  private var callHeartbeatJob: Job? = null
  private var endedCallJob: Job? = null
  private val cancelledInitiationIds = ConcurrentHashMap.newKeySet<String>()
  private val connectingCallIds = ConcurrentHashMap.newKeySet<String>()
  private val seenRealtimeEventIds = ConcurrentHashMap.newKeySet<String>()
  private val lastRealtimeVersions = ConcurrentHashMap<String, Long>()
  private val realtimeClient = CallRealtimeClient(supabase)
  private var realtimeJob: Job? = null
  @Volatile private var realtimeConnected = false

  init {
    coroutineScope.launch {
      LineCallTelecom.isMuted.collect { muted ->
        // Telecom mute (for example a supported system/headset surface) is
        // independent of the selected speaker/headset output and combines
        // safely with the app's own microphone mute.
        telecomMuted = muted
        applyMicrophoneMute()
      }
    }
    coroutineScope.launch {
      LineCallTelecom.availableEndpoints.collect {
        if (LineCallTelecom.hasActiveCall()) refreshAudioRoutes()
      }
    }
    coroutineScope.launch {
      LineCallTelecom.currentEndpoint.collect { endpoint ->
        if (LineCallTelecom.hasActiveCall()) {
          _selectedAudioRouteId.value = endpoint?.identifier?.hashCode()
          _activeCall.value = _activeCall.value?.copy(
            isSpeakerOn = endpoint?.type == androidx.core.telecom.CallEndpointCompat.TYPE_SPEAKER
          )
        }
      }
    }
  }

  fun validateNumber(e164: String): Boolean = userProfileService.validateE164(e164)

  private suspend fun saveCallRecord(record: CallRecord) {
    callRecordDao.insertCallRecord(record)
    LocalBackupScheduler.request(application)
  }

  fun startIncomingCallPolling() {
    ensureRealtimeSignalingStarted()
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
              if (_activeCall.compareAndSet(null, session)) {
                LineCallTelecom.registerIncoming(
                  application, session.callId, session.contactName.orEmpty(), session.phoneNumber,
                  onAnswer = { LineCallTelecom.incomingAnswerIntent(application, session.callId) },
                  onSetActive = { setCallActiveFromTelecom(true) },
                  onSetInactive = { setCallActiveFromTelecom(false) },
                  onDisconnect = { cause ->
                    if (cause == android.telecom.DisconnectCause.REJECTED) declineIncomingCall() else endCall()
                  }
                )
                startIncomingRingtone()
                watchIncomingCall(session)
                _callEvents.emit(CallEvent.CallStarted(session))
              }
            }
          }
        }
        // Realtime is the primary path. Poll less often while the socket is healthy,
        // but retain a short fallback interval whenever the socket is unavailable.
        delay(if (realtimeConnected) 10_000L else 2_500L)
      }
    }
  }

  private fun ensureRealtimeSignalingStarted() {
    if (realtimeJob?.isActive == true) return
    realtimeJob = coroutineScope.launch(Dispatchers.IO) {
      val account = runCatching { supabase.restoreSession() }.getOrNull() ?: return@launch
      realtimeClient.start(
        userId = account.id,
        scope = this,
        onEvent = { record ->
          coroutineScope.launch { handleRealtimeCallEvent(record) }
        },
        onConnectionChanged = { connected -> realtimeConnected = connected }
      ).join()
    }
  }

  fun stopIncomingCallPolling() {
    incomingPollJob?.cancel()
    incomingPollJob = null
    realtimeJob?.cancel()
    realtimeJob = null
    realtimeConnected = false
  }

  private suspend fun handleRealtimeCallEvent(event: JSONObject) {
    val eventId = event.optString("id")
    if (eventId.isNotBlank() && !seenRealtimeEventIds.add(eventId)) return
    val callId = event.optString("call_id").takeIf { it.isNotBlank() && it != "null" }
    val eventType = event.optString("event_type")
    if (callId != null) {
      val version = event.optLong("state_version", 0L)
      val previous = lastRealtimeVersions[callId] ?: Long.MIN_VALUE
      if (version < previous) return
      if (version > previous) lastRealtimeVersions[callId] = version
    }
    val current = _activeCall.value
    when (eventType) {
      "incoming_call" -> {
        if (current == null && callId != null) {
          val payload = event.optJSONObject("payload") ?: JSONObject()
          val phone = payload.optString("caller_phone")
          val session = ActiveCallSession(
            callId = callId,
            contactName = null,
            phoneNumber = phone,
            state = CallSessionState.RINGING,
            isIncoming = true
          )
          if (_activeCall.compareAndSet(null, session)) {
            LineCallTelecom.registerIncoming(
              application, session.callId, session.contactName.orEmpty(), session.phoneNumber,
              onAnswer = { LineCallTelecom.incomingAnswerIntent(application, session.callId) },
              onSetActive = { setCallActiveFromTelecom(true) },
              onSetInactive = { setCallActiveFromTelecom(false) },
              onDisconnect = { cause ->
                if (cause == android.telecom.DisconnectCause.REJECTED) declineIncomingCall() else endCall()
              }
            )
            startIncomingRingtone()
            watchIncomingCall(session)
            _callEvents.emit(CallEvent.CallStarted(session))
          }
        }
      }
      "ringing_confirmation" -> {
        if (callId != null && current?.callId == callId && !current.isIncoming) {
          _activeCall.value = current.copy(state = CallSessionState.RINGING)
          startRingbackTone()
        }
      }
      "call_accepted" -> {
        if (callId != null && current?.callId == callId) {
          if (current.isIncoming) {
            if (current.state == CallSessionState.RINGING) {
              stopIncomingRingtone()
              showEndedCall(current) // another device on this account answered
            }
          } else if (current.state != CallSessionState.CONNECTED) {
            stopRingbackTone()
            connectToLiveKit(callId, current)
          }
        }
      }
      "call_declined", "call_ended", "caller_cancelled" -> {
        if (callId != null && current?.callId == callId && current.state != CallSessionState.ENDED) {
          stopIncomingRingtone()
          stopRingbackTone()
          // The device that pressed Decline/End records locally. Other devices
          // record the remote terminal event exactly once here.
          if (!(current.isIncoming && current.state == CallSessionState.ACCEPTING)) {
            val payload = event.optJSONObject("payload") ?: JSONObject()
            val missedIncoming = current.isIncoming && current.state == CallSessionState.RINGING
            saveCallRecord(CallRecord(
              phoneNumber = current.phoneNumber,
              contactName = current.contactName,
              type = if (missedIncoming) CallType.MISSED
                else if (current.isIncoming) CallType.INCOMING else CallType.OUTGOING,
              durationSeconds = if (missedIncoming) 0 else payload.optInt("duration_seconds", current.durationSeconds)
            ))
          }
          showEndedCall(current)
        }
      }
      "recipient_busy" -> {
        if (current != null && !current.isIncoming && current.state != CallSessionState.ENDED) {
          stopRingbackTone()
          _callEvents.emit(CallEvent.Error("The person you're calling is currently busy."))
          showEndedCall(current)
        }
      }
      "connection_established" -> {
        if (_activeCall.value?.callId == callId) _connectionState.value = CallConnectionState.CONNECTED
        android.util.Log.i("LineCall", "Realtime: media connection established for $callId")
      }
      "connection_lost" -> {
        if (_activeCall.value?.callId == callId) _connectionState.value = CallConnectionState.RECONNECTING
        android.util.Log.w("LineCall", "Realtime: media connection lost for $callId")
      }
      "reconnection_completed" -> {
        if (_activeCall.value?.callId == callId) _connectionState.value = CallConnectionState.CONNECTED
        android.util.Log.i("LineCall", "Realtime: media reconnection completed for $callId")
      }
    }
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

    val pendingId = "pending-${UUID.randomUUID()}"
    val pendingSession = ActiveCallSession(
      callId = pendingId, contactName = contactName, phoneNumber = e164PhoneNumber,
      state = CallSessionState.DIALING, isIncoming = false
    )
    // Render the call screen before waiting for the network request.
    _activeCall.value = pendingSession
    _callEvents.emit(CallEvent.CallStarted(pendingSession))
    return try {
      val response = supabase.callFunction(
        JSONObject().put("action", "initiate").put("phone_number", e164PhoneNumber)
      )
      val session = ActiveCallSession(
        callId = response.getString("call_id"),
        contactName = response.optString("display_name").takeIf { it.isNotBlank() && it != "null" } ?: contactName,
        phoneNumber = response.optString("phone_number", e164PhoneNumber),
        state = CallSessionState.DIALING, isIncoming = false
      )
      if (cancelledInitiationIds.remove(pendingId)) {
        // The user hung up before the initiate request returned. End the real
        // server call now that its ID is known; never start its ringback monitor.
        closeServerCall(session.callId)
        saveCallRecord(
          CallRecord(
            phoneNumber = session.phoneNumber,
            contactName = session.contactName,
            type = CallType.OUTGOING,
            durationSeconds = 0
          )
        )
        return CallInitiationResult.Started(session)
      }
      if (_activeCall.value?.callId == pendingId) _activeCall.value = session
      outgoingPollJob?.cancel()
      outgoingPollJob = coroutineScope.launch(Dispatchers.IO) { monitorOutgoingCall(session) }
      CallInitiationResult.Started(session)
    } catch (error: Exception) {
      cancelledInitiationIds.remove(pendingId)
      if (_activeCall.value?.callId == pendingId) stopCallSession()
      val message = error.message ?: "Unable to start the call."
      if (message.contains("not registered", ignoreCase = true)) CallInitiationResult.UserNotRegistered(e164PhoneNumber)
      else CallInitiationResult.BackendNotConnected(e164PhoneNumber, message)
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
          "ringing" -> {
            _activeCall.value = _activeCall.value?.takeIf { it.callId == session.callId }
              ?.copy(state = CallSessionState.RINGING)
            startRingbackTone()
          }
          "accepted" -> {
            stopRingbackTone()
            // Core-Telecom requires a valid call notification within five seconds.
            // Register outgoing calls only once the remote party is ready, after posting it.
            LineCallOngoingCallService.start(application, session)
            LineCallTelecom.registerOutgoing(
              application, session.callId, session.contactName, session.phoneNumber,
              onSetActive = { setCallActiveFromTelecom(true) },
              onSetInactive = { setCallActiveFromTelecom(false) },
              onDisconnect = { endCall() }
            )
            connectToLiveKit(session.callId, session)
            return
          }
          "declined", "missed", "ended", "failed" -> {
            stopRingbackTone()
            saveCallRecord(
              CallRecord(
                phoneNumber = session.phoneNumber,
                contactName = session.contactName,
                type = CallType.OUTGOING,
                durationSeconds = 0
              )
            )
            showEndedCall(_activeCall.value ?: session)
            return
          }
        }
      } catch (error: Exception) {
        closeServerCall(session.callId)
        _callEvents.emit(CallEvent.Error(error.message ?: "Connection lost while waiting for an answer."))
        stopCallSession()
        return
      }
      delay(if (realtimeConnected) 10_000L else 1_800L)
    }
  }

  /**
   * Restores the exact call selected by the notification Answer action into the
   * app's one call engine. This prevents a helper service from creating a
   * second LiveKit room while the UI is opening.
   */
  fun answerIncomingCallById(callId: String, callerName: String?, callerPhone: String) {
    val existing = _activeCall.value
    if (existing?.callId == callId) {
      if (existing.isIncoming && existing.state == CallSessionState.RINGING) acceptIncomingCall()
      return
    }
    if (existing != null) return

    _activeCall.value = ActiveCallSession(
      callId = callId,
      contactName = callerName?.takeIf { it.isNotBlank() },
      phoneNumber = callerPhone,
      state = CallSessionState.RINGING,
      isIncoming = true
    )
    acceptIncomingCall()
  }

  private fun clearIncomingNotification(callId: String) {
    IncomingCallLifecycleService.stop(application, callId)
  }


  fun acceptIncomingCall() {
    val current = _activeCall.value ?: return
    if (!current.isIncoming || current.state != CallSessionState.RINGING) return
    stopIncomingRingtone()
    incomingWatchJob?.cancel()
    _activeCall.value = current.copy(state = CallSessionState.ACCEPTING)
    coroutineScope.launch(Dispatchers.IO) {
      try {
        val deviceId = Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID)
          ?.takeIf { it.length >= 8 } ?: UUID.randomUUID().toString()
        val accepted = supabase.callFunction(
          JSONObject().put("action", "accept").put("call_id", current.callId).put("device_id", deviceId)
        )
        if (!accepted.optBoolean("accepted")) throw IllegalStateException("This call is no longer available to answer.")
        LineCallOngoingCallService.start(application, current)
        LineCallTelecom.confirmAnswered()
        connectToLiveKit(current.callId, current)
      } catch (error: Exception) {
        _callEvents.emit(CallEvent.Error(error.message ?: "Could not accept this call."))
        stopCallSession()
      }
    }
  }

  fun declineIncomingCall() {
    val current = _activeCall.value ?: return
    if (!current.isIncoming || current.state != CallSessionState.RINGING) return
    _activeCall.value = current.copy(state = CallSessionState.ACCEPTING)
    stopIncomingRingtone()
    clearIncomingNotification(current.callId)
    incomingWatchJob?.cancel()
    coroutineScope.launch(Dispatchers.IO) {
      runCatching {
        supabase.callFunction(JSONObject().put("action", "decline").put("call_id", current.callId))
      }
      saveCallRecord(
        CallRecord(
          phoneNumber = current.phoneNumber,
          contactName = current.contactName,
          type = CallType.MISSED,
          durationSeconds = 0
        )
      )
      showEndedCall(current)
    }
  }

  private suspend fun closeServerCall(callId: String) {
    runCatching {
      supabase.callFunction(JSONObject().put("action", "end").put("call_id", callId))
    }
  }

  private suspend fun connectToLiveKit(callId: String, session: ActiveCallSession) {
    if (liveKitRoom != null && _activeCall.value?.callId == callId) return
    if (!connectingCallIds.add(callId)) return
    _connectionState.value = CallConnectionState.CONNECTING
    try {
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
    // Core-Telecom owns communication routing while its CallControlScope is active.
    // Do not also force AudioManager speaker/SCO/communication-device selection.
    if (!LineCallTelecom.hasActiveCall()) {
      audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
      applyDefaultAudioRoute()
    }

    room.connect(serverUrl, participantToken)
    runCatching {
      supabase.callFunction(JSONObject().put("action", "signal")
        .put("call_id", callId).put("signal_type", "connection_established"))
    }
    room.localParticipant.setMicrophoneEnabled(!(_activeCall.value?.isMuted ?: false) && !telecomMuted && !telecomInactive)
    LineCallOngoingCallService.start(application, session)

    liveKitEventsJob?.cancel()
    liveKitEventsJob = coroutineScope.launch {
      room.events.collect { event ->
        when (event) {
          is RoomEvent.Reconnecting -> {
            if (_activeCall.value?.callId == callId) _connectionState.value = CallConnectionState.RECONNECTING
            coroutineScope.launch(Dispatchers.IO) {
              runCatching { supabase.callFunction(JSONObject().put("action", "signal")
                .put("call_id", callId).put("signal_type", "connection_lost")) }
            }
          }
          is RoomEvent.Reconnected -> {
            if (_activeCall.value?.callId == callId) _connectionState.value = CallConnectionState.CONNECTED
            coroutineScope.launch(Dispatchers.IO) {
              runCatching { supabase.callFunction(JSONObject().put("action", "signal")
                .put("call_id", callId).put("signal_type", "reconnection_completed")) }
            }
          }
          is RoomEvent.TrackPublished -> {
            // A track may be announced while Telecom has this call on hold.
            // Disable it immediately so held calls do not resume playback early.
            val publication = event.publication as? RemoteTrackPublication
            if (telecomInactive && publication?.kind == Track.Kind.AUDIO) {
              publication.setEnabled(false)
              telecomPausedAudioPublications.add(publication)
            }
          }
          is RoomEvent.ParticipantDisconnected -> {
            if (_activeCall.value?.callId == callId) {
              endCall()
            }
          }
          is RoomEvent.Disconnected -> {
            if (_activeCall.value?.callId == callId) {
              endCall()
            }
          }
          else -> Unit
        }
      }
    }

    if (_activeCall.value?.callId == callId && _activeCall.value?.state != CallSessionState.ENDED) {
      if (!session.isIncoming) LineCallTelecom.setActiveAfterRemoteAnswer()
      _activeCall.value = session.copy(state = CallSessionState.CONNECTED)
      _connectionState.value = CallConnectionState.CONNECTED
      startDurationTimer()
      startCallHeartbeat(callId)
    } else {
      runCatching { room.disconnect() }
      if (liveKitRoom === room) liveKitRoom = null
    }
    } finally {
      connectingCallIds.remove(callId)
    }
  }

  fun toggleMute() {
    val current = _activeCall.value ?: return
    val muted = !current.isMuted
    _activeCall.value = current.copy(isMuted = muted)
    applyMicrophoneMute()
  }

  private fun applyMicrophoneMute() {
    val shouldEnableMicrophone = !(_activeCall.value?.isMuted ?: false) && !telecomMuted && !telecomInactive
    coroutineScope.launch {
      runCatching { liveKitRoom?.localParticipant?.setMicrophoneEnabled(shouldEnableMicrophone) }
        .onFailure { _callEvents.emit(CallEvent.Error("Could not update microphone mute state.")) }
    }
  }

  /**
   * Android Telecom invokes these callbacks when another system call or remote
   * surface (headset, wearable, or car UI) changes this call's active state.
   * Holding must stop both microphone capture and incoming audio playback.
   */
  private suspend fun setCallActiveFromTelecom(active: Boolean) {
    telecomInactive = !active
    if (active) {
      telecomPausedAudioPublications.toList().forEach { publication ->
        runCatching { publication.setEnabled(true) }
      }
      telecomPausedAudioPublications.clear()
      liveKitRoom?.localParticipant?.setMicrophoneEnabled(
        !(_activeCall.value?.isMuted ?: false) && !telecomMuted
      )
    } else {
      liveKitRoom?.localParticipant?.setMicrophoneEnabled(false)
      liveKitRoom?.remoteParticipants?.values?.forEach { participant ->
        participant.trackPublications.values.forEach { publication ->
          val remoteAudio = publication as? RemoteTrackPublication ?: return@forEach
          if (remoteAudio.kind == Track.Kind.AUDIO) {
            remoteAudio.setEnabled(false)
            telecomPausedAudioPublications.add(remoteAudio)
          }
        }
      }
    }
  }

  fun refreshAudioRoutes() {
    if (LineCallTelecom.hasActiveCall()) {
      val endpoints = LineCallTelecom.availableEndpoints.value
      _audioRoutes.value = endpoints.map { endpoint ->
        val type = when (endpoint.type) {
          androidx.core.telecom.CallEndpointCompat.TYPE_EARPIECE -> "Earpiece"
          androidx.core.telecom.CallEndpointCompat.TYPE_SPEAKER -> "Speaker"
          androidx.core.telecom.CallEndpointCompat.TYPE_BLUETOOTH -> "Bluetooth"
          androidx.core.telecom.CallEndpointCompat.TYPE_WIRED_HEADSET -> "Wired headset"
          androidx.core.telecom.CallEndpointCompat.TYPE_STREAMING -> "External audio"
          else -> "Audio device"
        }
        // Telecom supplies the platform-reported endpoint name (including the
        // paired headset name when available); never infer it from AudioManager.
        CallAudioRoute(endpoint.identifier.hashCode(), endpoint.name.toString().ifBlank { type }, type)
      }
      _selectedAudioRouteId.value = LineCallTelecom.currentEndpoint.value?.identifier?.hashCode()
      return
    }

    // Compatibility fallback is used only when Telecom registration is unavailable.
    val devices = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      audioManager.availableCommunicationDevices
    } else {
      audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
    }
    val routes = devices.mapNotNull { device ->
      val type = when (device.type) {
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Earpiece"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Speaker"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth"
        AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER -> "Bluetooth LE"
        AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired headset"
        AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> "USB audio"
        else -> return@mapNotNull null
      }
      val friendlyName = device.productName?.toString()?.takeIf { it.isNotBlank() }
      CallAudioRoute(device.id, if (type.startsWith("Bluetooth") && friendlyName != null) friendlyName else type, type)
    }.distinctBy { it.id }
    _audioRoutes.value = routes
    _selectedAudioRouteId.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      audioManager.communicationDevice?.id
    } else {
      routes.firstOrNull { it.type == "Speaker" && audioManager.isSpeakerphoneOn }?.id
    }
  }

  private fun applyDefaultAudioRoute() {
    if (LineCallTelecom.hasActiveCall()) {
      refreshAudioRoutes()
      val current = LineCallTelecom.currentEndpoint.value
      if (current != null) _selectedAudioRouteId.value = current.identifier.hashCode()
      return
    }
    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
    refreshAudioRoutes()
    val preferred = _audioRoutes.value.firstOrNull { it.type.startsWith("Bluetooth") }
      ?: _audioRoutes.value.firstOrNull { it.type == "Wired headset" || it.type == "USB audio" }
      ?: _audioRoutes.value.firstOrNull { it.type == "Earpiece" }
      ?: _audioRoutes.value.firstOrNull { it.type == "Speaker" }
    if (preferred != null) selectAudioRoute(preferred.id)
    else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) audioManager.clearCommunicationDevice()
    else audioManager.isSpeakerphoneOn = false
  }

  fun selectAudioRoute(deviceId: Int) {
    val route = _audioRoutes.value.firstOrNull { it.id == deviceId } ?: return
    if (LineCallTelecom.hasActiveCall()) {
      coroutineScope.launch {
        val accepted = runCatching { LineCallTelecom.selectEndpoint(deviceId) }.getOrDefault(false)
        if (!accepted) _callEvents.emit(CallEvent.Error("Android could not switch to that audio device."))
        // Do not update selected route optimistically. currentCallEndpoint is authoritative.
      }
      return
    }
    // Legacy fallback only: Telecom remains the sole route owner during a registered call.
    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val device = audioManager.availableCommunicationDevices.firstOrNull { it.id == deviceId } ?: return
      if (audioManager.setCommunicationDevice(device)) _selectedAudioRouteId.value = deviceId
    } else {
      val bluetooth = route.type.startsWith("Bluetooth")
      audioManager.isSpeakerphoneOn = route.type == "Speaker"
      if (bluetooth) {
        runCatching { audioManager.startBluetoothSco(); audioManager.isBluetoothScoOn = true }
      } else {
        runCatching { audioManager.stopBluetoothSco(); audioManager.isBluetoothScoOn = false }
      }
      _selectedAudioRouteId.value = deviceId
    }
    _activeCall.value = _activeCall.value?.copy(isSpeakerOn = route.type == "Speaker")
  }

  fun toggleSpeaker() {
    val current = _activeCall.value ?: return
    refreshAudioRoutes()
    val speaker = _audioRoutes.value.firstOrNull { it.type == "Speaker" } ?: return
    if (current.isSpeakerOn) applyDefaultAudioRoute() else selectAudioRoute(speaker.id)
  }

  fun endCall() {
    val current = _activeCall.value ?: return
    if (endingCallId == current.callId) return
    endingCallId = current.callId
    stopIncomingRingtone()
    val duration = current.durationSeconds

    showEndedCall(current)

    if (current.callId.startsWith("pending-")) {
      // The call-control initiate request is still in flight and has not given
      // us the server call ID yet. Remember the cancellation for its response.
      cancelledInitiationIds.add(current.callId)
      coroutineScope.launch { _callEvents.emit(CallEvent.CallEnded(duration)) }
      return
    }

    // End local UI/audio immediately. Network delivery continues in the
    // background so a slow request cannot keep this phone's call screen ringing.
    coroutineScope.launch(Dispatchers.IO) {
      runCatching {
        supabase.callFunction(JSONObject().put("action", "end").put("call_id", current.callId))
      }.onFailure {
        android.util.Log.e("LineCall", "Could not send remote call-end signal.", it)
      }
      saveCallRecord(
        CallRecord(
          phoneNumber = current.phoneNumber,
          contactName = current.contactName,
          type = if (current.isIncoming) CallType.INCOMING else CallType.OUTGOING,
          durationSeconds = duration
        )
      )
    }
  }

  private fun watchIncomingCall(session: ActiveCallSession) {
    incomingWatchJob?.cancel()
    incomingWatchJob = coroutineScope.launch(Dispatchers.IO) {
      while (isActive && _activeCall.value?.callId == session.callId &&
        _activeCall.value?.state == CallSessionState.RINGING
      ) {
        delay(if (realtimeConnected) 10_000L else 1_800L)
        val status = runCatching {
          supabase.callFunction(JSONObject().put("action", "status").put("call_id", session.callId))
            .optJSONObject("call")?.optString("status").orEmpty()
        }.getOrDefault("")
        if (status == "accepted") {
          // Another device on this account answered. Stop this device's
          // ringtone and stale incoming surface without ending the real call.
          stopIncomingRingtone()
          showEndedCall(session)
          return@launch
        }
        if (status in listOf("declined", "missed", "ended", "failed")) {
          saveCallRecord(
            CallRecord(
              phoneNumber = session.phoneNumber,
              contactName = session.contactName,
              type = CallType.MISSED,
              durationSeconds = 0
            )
          )
          showEndedCall(session)
          return@launch
        }
      }
    }
  }

  private fun startIncomingRingtone() {
    val current = _activeCall.value?.takeIf { it.isIncoming && it.state == CallSessionState.RINGING } ?: return
    IncomingCallLifecycleService.start(
      application, current.callId, current.contactName.orEmpty(), current.phoneNumber, null,
      System.currentTimeMillis() + IncomingCallMessagingService.MAX_RING_DURATION_MS
    )
  }

  private fun stopIncomingRingtone() {
    IncomingCallLifecycleService.stop(application, _activeCall.value?.callId)
  }

  private fun startRingbackTone() {
    if (ringbackJob?.isActive == true) return
    ringbackJob = coroutineScope.launch {
      runCatching {
        ringbackTone = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 65)
        while (isActive && _activeCall.value?.state == CallSessionState.RINGING) {
          ringbackTone?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 1800)
          delay(4000)
        }
      }
    }
  }

  private fun stopRingbackTone() {
    ringbackJob?.cancel()
    ringbackJob = null
    runCatching { ringbackTone?.stopTone() }
    runCatching { ringbackTone?.release() }
    ringbackTone = null
  }

  private fun startCallHeartbeat(callId: String) {
    callHeartbeatJob?.cancel()
    callHeartbeatJob = coroutineScope.launch(Dispatchers.IO) {
      while (isActive && _activeCall.value?.callId == callId) {
        delay(30_000)
        if (!isActive || _activeCall.value?.callId != callId) break
        // A short server-side lease prevents abandoned accepted calls from blocking
        // the caller and recipient forever after a process or network failure.
        runCatching {
          supabase.callFunction(
            JSONObject().put("action", "heartbeat").put("call_id", callId)
          )
        }
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

  private fun showEndedCall(session: ActiveCallSession) {
    endedCallJob?.cancel()
    _connectionState.value = CallConnectionState.ENDED
    _activeCall.value = session.copy(state = CallSessionState.ENDED)
    stopCallSession(keepEndedSession = true)
    _activeCall.value = session.copy(state = CallSessionState.ENDED)
    endedCallJob = coroutineScope.launch {
      repeat(3) {
        runCatching {
          val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 55)
          tone.startTone(ToneGenerator.TONE_PROP_BEEP, 140)
          delay(420)
          tone.release()
        }
      }
      delay(3_500)
      if (_activeCall.value?.callId == session.callId &&
        _activeCall.value?.state == CallSessionState.ENDED
      ) {
        _activeCall.value = null
      }
    }
  }

  fun dismissEndedCall() {
    if (_activeCall.value?.state == CallSessionState.ENDED) {
      endedCallJob?.cancel()
      endedCallJob = null
      _activeCall.value = null
      _connectionState.value = CallConnectionState.IDLE
    }
  }

  private fun stopCallSession(keepEndedSession: Boolean = false) {
    callHeartbeatJob?.cancel()
    callHeartbeatJob = null
    LineCallTelecom.disconnectLocal()
    stopIncomingRingtone()
    stopRingbackTone()
    timerJob?.cancel()
    timerJob = null
    outgoingPollJob?.cancel()
    outgoingPollJob = null
    incomingWatchJob?.cancel()
    incomingWatchJob = null
    liveKitEventsJob?.cancel()
    liveKitEventsJob = null
    val room = liveKitRoom
    liveKitRoom = null
    endingCallId = null
    if (room != null) coroutineScope.launch { runCatching { room.disconnect() } }
    LineCallOngoingCallService.stop(application)
    telecomInactive = false
    telecomPausedAudioPublications.clear()
    if (!keepEndedSession) {
      _activeCall.value = null
      _connectionState.value = CallConnectionState.IDLE
    }
    if (!LineCallTelecom.hasActiveCall()) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) audioManager.clearCommunicationDevice()
      runCatching { audioManager.stopBluetoothSco(); audioManager.isBluetoothScoOn = false }
      audioManager.isSpeakerphoneOn = false
      audioManager.mode = AudioManager.MODE_NORMAL
    }
    _selectedAudioRouteId.value = null
    _audioRoutes.value = emptyList()
  }
}
