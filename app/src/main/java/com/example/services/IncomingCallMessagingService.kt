package com.example.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.MainActivity
import com.example.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

class IncomingCallMessagingService : FirebaseMessagingService() {
  override fun onNewToken(token: String) {
    super.onNewToken(token)
    getSharedPreferences("line_call_fcm", Context.MODE_PRIVATE)
      .edit().putString("fcm_token", token).apply()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      runCatching {
        val backend = LineCallSupabaseClient(applicationContext)
        val user = backend.restoreSession()
        if (user != null) backend.saveFcmToken(user.id, token)
      }.onFailure {
        android.util.Log.w("LineCallFCM", "Could not sync refreshed device token.", it)
      }
    }
  }

  override fun onMessageReceived(message: RemoteMessage) {
    super.onMessageReceived(message)
    val data = message.data
    val callId = data["call_id"]?.takeIf { it.isNotBlank() } ?: return
    val notificationManager = getSystemService(NotificationManager::class.java)

    // Process cancellation even when the app is foregrounded so an existing
    // lock-screen or shade notification is dismissed immediately.
    if (data["type"] == "CALL_ENDED") {
      notificationManager.cancel(notificationId(callId))
      return
    }
    if (data["type"] != "INCOMING_CALL") return

    // FCM can be delayed, duplicated, or arrive after Answer/Decline. Confirm
    // the authoritative server state before displaying a ringing surface.
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        val backend = LineCallSupabaseClient(applicationContext)
        val user = withTimeoutOrNull(5_000L) { backend.restoreSession() } ?: return@launch
        val status = withTimeoutOrNull(5_000L) {
          backend.callFunction(JSONObject().put("action", "status").put("call_id", callId))
            .optJSONObject("call")?.optString("status")
        } ?: return@launch
        if (status != "ringing") return@launch
        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return@launch
  
      val callerName = data["caller_name"]?.takeIf { it.isNotBlank() } ?: data["caller_phone"] ?: "Line Call user"
      createCallChannel(notificationManager)
  
      val requestBase = callId.hashCode()
      val openIntent = Intent(this, MainActivity::class.java).apply {
        putExtra("linecall_call_id", callId)
        putExtra("linecall_call_action", "open")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
      val fullScreenPendingIntent = PendingIntent.getActivity(
        this, requestBase, openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      // Answer opens the app directly. The app's single CallingService accepts
      // and connects the call; no second LiveKit room is created in a helper service.
      val answerIntent = Intent(this, MainActivity::class.java).apply {
        putExtra("linecall_call_id", callId)
        putExtra("linecall_call_action", "answer")
        putExtra("linecall_caller_name", callerName)
        putExtra("linecall_caller_phone", data["caller_phone"].orEmpty())
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
      val answerPendingIntent = PendingIntent.getActivity(
        this, requestBase + 1, answerIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      val declinePendingIntent = actionPendingIntent(callId, "decline", requestBase + 2, callerName, data["caller_phone"].orEmpty())
      val person = Person.Builder().setName(callerName).setImportant(true).build()
  
      val notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("Incoming Line Call")
        .setContentText(callerName)
        .setCategory(NotificationCompat.CATEGORY_CALL)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setOngoing(true)
        .setAutoCancel(false)
        .setTimeoutAfter(45_000L)
        // Keep the call on the lock screen/shade. Do not auto-launch MainActivity;
        // Answer and Decline must work directly from the notification actions.
        .setContentIntent(fullScreenPendingIntent)
        // Android launches the incoming-call UI over the lock screen when the
        // user/device allows full-screen notifications for this app.
        .setFullScreenIntent(fullScreenPendingIntent, true)
        .setOnlyAlertOnce(true)
        .setStyle(NotificationCompat.CallStyle.forIncomingCall(person, declinePendingIntent, answerPendingIntent))
        .build()
  
      notificationManager.notify(notificationId(callId), notification)
      } catch (error: Exception) {
        android.util.Log.w("LineCallFCM", "Could not verify incoming call before notification.", error)
      }
    }
  }

  private fun actionPendingIntent(callId: String, action: String, requestCode: Int, callerName: String, callerPhone: String): PendingIntent {
    val intent = Intent(this, IncomingCallActionReceiver::class.java).apply {
      putExtra("linecall_call_id", callId)
      putExtra("linecall_call_action", action)
      putExtra("linecall_caller_name", callerName)
      putExtra("linecall_caller_phone", callerPhone)
    }
    return PendingIntent.getBroadcast(
      this, requestCode, intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun createCallChannel(manager: NotificationManager) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
      val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
      val channel = NotificationChannel(
        CHANNEL_ID, "Incoming calls", NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Notifications for incoming Line Call voice calls"
        setSound(ringtone, attributes)
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 700, 400, 700)
        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
      }
      manager.createNotificationChannel(channel)
    }
  }

  companion object {
    const val CHANNEL_ID = "linecall_incoming_calls"
    fun notificationId(callId: String): Int = callId.hashCode()
  }
}

class IncomingCallActionReceiver : android.content.BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val callId = intent.getStringExtra("linecall_call_id")?.takeIf { it.isNotBlank() } ?: return
    when (intent.getStringExtra("linecall_call_action")) {
      "decline" -> {
        // Dismiss the local ringing surface immediately. The server update is
        // delivered asynchronously so network latency never leaves a dead
        // Answer/Decline notification on the user's screen.
        context.getSystemService(NotificationManager::class.java)
          .cancel(IncomingCallMessagingService.notificationId(callId))
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
          try {
            val backend = LineCallSupabaseClient(context.applicationContext)
            withTimeoutOrNull(12_000L) {
              backend.restoreSession()
              backend.callFunction(JSONObject().put("action", "decline").put("call_id", callId))
              runCatching {
                com.example.data.local.LineCallDatabase.getDatabase(context.applicationContext)
                  .callRecordDao().insertCallRecord(
                    com.example.data.model.CallRecord(
                      phoneNumber = intent.getStringExtra("linecall_caller_phone").orEmpty(),
                      contactName = intent.getStringExtra("linecall_caller_name"),
                      type = com.example.data.model.CallType.MISSED,
                      durationSeconds = 0
                    )
                  )
              }.onFailure {
                android.util.Log.w("LineCallFCM", "Could not save declined incoming call to Recents.", it)
              }
            } ?: android.util.Log.w("LineCallFCM", "Decline request timed out; local notification was still dismissed.")
          } catch (error: Exception) {
            android.util.Log.e("LineCallFCM", "Could not notify caller that this call was declined.", error)
          } finally {
            pending.finish()
          }
        }
      }
    }
  }
}
