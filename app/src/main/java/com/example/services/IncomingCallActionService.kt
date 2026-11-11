package com.example.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.example.MainActivity
import com.example.R
import com.google.firebase.messaging.RemoteMessage
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Handles notification actions without launching MainActivity. Answer connects the
 * LiveKit room from this foreground service; Decline is handled by the receiver.
 */
class IncomingCallActionService : Service() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var room: Room? = null
  private var callId: String? = null
  private var backend: LineCallSupabaseClient? = null
  private var finished = false

  override fun onCreate() {
    super.onCreate()
    createChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val requestedAction = intent?.action
    val requestedCallId = intent?.getStringExtra(EXTRA_CALL_ID)
    if (requestedAction == ACTION_END) {
      scope.launch { finishCall(requestedCallId, notifyServer = true) }
      return START_NOT_STICKY
    }
    if (requestedAction != ACTION_ANSWER || requestedCallId.isNullOrBlank()) {
      stopSelf(startId)
      return START_NOT_STICKY
    }
    if (callId == requestedCallId && room != null) return START_STICKY
    callId = requestedCallId
    startAsForeground(buildNotification(requestedCallId, "Answering call…", ongoing = true))
    scope.launch { answerCall(requestedCallId) }
    return START_NOT_STICKY
  }

  private suspend fun answerCall(id: String) {
    try {
      val api = LineCallSupabaseClient(applicationContext)
      backend = api
      val user = api.restoreSession()
        ?: throw IllegalStateException("Your Line Call session has expired. Open the app and sign in again.")
      api.callFunction(JSONObject().put("action", "accept").put("call_id", id))

      val credentials = api.callFunction(
        JSONObject().put("action", "token").put("call_id", id)
          .put("participant_name", user.displayName)
      )
      val liveKitRoom = LiveKit.create(applicationContext)
      room = liveKitRoom
      liveKitRoom.connect(credentials.getString("livekit_url"), credentials.getString("token"))
      liveKitRoom.localParticipant.setMicrophoneEnabled(true)

      getSystemService(NotificationManager::class.java)
        .notify(notificationId(id), buildNotification(id, "Connected · Tap to return", ongoing = true))

      scope.launch {
        liveKitRoom.events.collect { event ->
          when (event) {
            is RoomEvent.ParticipantDisconnected, is RoomEvent.Disconnected -> {
              finishCall(id, notifyServer = true)
            }
            else -> Unit
          }
        }
      }
      while (scope.isActive && !finished && callId == id) {
        delay(30_000L)
        if (finished || callId != id) break
        runCatching {
          api.callFunction(JSONObject().put("action", "heartbeat").put("call_id", id))
        }.onFailure {
          android.util.Log.w(TAG, "Call heartbeat failed.", it)
        }
      }
    } catch (error: CancellationException) {
      throw error
    } catch (error: Exception) {
      android.util.Log.e(TAG, "Could not answer incoming call in background.", error)
      runCatching {
        backend?.callFunction(JSONObject().put("action", "end").put("call_id", id))
      }
      getSystemService(NotificationManager::class.java).cancel(notificationId(id))
      stopSelf()
    }
  }

  private suspend fun finishCall(id: String?, notifyServer: Boolean) {
    if (finished) return
    finished = true
    if (notifyServer && !id.isNullOrBlank()) {
      runCatching {
        backend?.callFunction(JSONObject().put("action", "end").put("call_id", id))
      }
    }
    runCatching { room?.disconnect() }
    room = null
    if (!id.isNullOrBlank()) {
      getSystemService(NotificationManager::class.java).cancel(notificationId(id))
    }
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  private fun startAsForeground(notification: Notification) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(
        FOREGROUND_NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
      )
    } else {
      startForeground(FOREGROUND_NOTIFICATION_ID, notification)
    }
  }

  private fun buildNotification(id: String, status: String, ongoing: Boolean): Notification {
    val openApp = PendingIntent.getActivity(
      this, id.hashCode(),
      Intent(this, MainActivity::class.java).apply {
        putExtra("linecall_call_id", id)
        putExtra("linecall_call_action", "open")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
      },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val endIntent = PendingIntent.getService(
      this, id.hashCode() + 20,
      Intent(this, IncomingCallActionService::class.java).apply {
        action = ACTION_END
        putExtra(EXTRA_CALL_ID, id)
      },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("Line Call")
      .setContentText(status)
      .setCategory(NotificationCompat.CATEGORY_CALL)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setOngoing(ongoing)
      .setOnlyAlertOnce(true)
      .setContentIntent(openApp)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, "End call", endIntent)
      .build()
  }

  private fun createChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = getSystemService(NotificationManager::class.java)
    if (manager.getNotificationChannel(CHANNEL_ID) == null) {
      manager.createNotificationChannel(
        NotificationChannel(CHANNEL_ID, "Active Line Call", NotificationManager.IMPORTANCE_LOW).apply {
          description = "Shows calls answered from the incoming-call notification"
          setSound(null, null)
          enableVibration(false)
          lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
      )
    }
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onDestroy() {
    scope.cancel()
    runCatching { room?.disconnect() }
    room = null
    super.onDestroy()
  }

  companion object {
    const val ACTION_ANSWER = "com.example.linecall.ANSWER"
    const val ACTION_END = "com.example.linecall.END"
    const val EXTRA_CALL_ID = "linecall_call_id"
    private const val CHANNEL_ID = "line_call_answered_call"
    private const val FOREGROUND_NOTIFICATION_ID = 4103
    private const val TAG = "LineCallBgCall"

    fun startAnswer(context: Context, callId: String) {
      val intent = Intent(context, IncomingCallActionService::class.java).apply {
        action = ACTION_ANSWER
        putExtra(EXTRA_CALL_ID, callId)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
      else context.startService(intent)
    }

    fun notificationId(callId: String): Int = callId.hashCode()
  }
}
