package com.example.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.graphics.BitmapFactory
import androidx.core.graphics.drawable.IconCompat
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
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
      IncomingCallLifecycleService.stop(applicationContext, callId)
      LineCallOngoingCallService.stop(applicationContext)
      notificationManager.cancel(notificationId(callId))
      getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove("active_call_id").remove("ring_started_at").apply()
      return
    }
    if (data["type"] != "INCOMING_CALL") return

    // FCM can be delayed, duplicated, or arrive after Answer/Decline. Confirm
    // the authoritative server state before displaying a ringing surface.
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        val backend = LineCallSupabaseClient(applicationContext)
        withTimeoutOrNull(5_000L) { backend.restoreSession() } ?: return@launch
        val status = withTimeoutOrNull(5_000L) {
          backend.callFunction(JSONObject().put("action", "status").put("call_id", callId))
            .optJSONObject("call")?.optString("status")
        } ?: return@launch
        if (status != "ringing") return@launch
        val expiresAtMs = data["ring_expires_at_ms"]?.toLongOrNull()
          ?: (System.currentTimeMillis() + MAX_RING_DURATION_MS)
        val remainingRingMs = (expiresAtMs - System.currentTimeMillis()).coerceAtMost(MAX_RING_DURATION_MS)
        if (remainingRingMs <= 0L) return@launch
        val callerPhone = data["caller_phone"]?.takeIf { it.isNotBlank() } ?: "Unknown number"
        val callerName = data["caller_name"]?.takeIf { it.isNotBlank() } ?: callerPhone
        val avatarUrl = data["caller_avatar_url"]?.takeIf { it.startsWith("https://") }
        if (message.priority == RemoteMessage.PRIORITY_HIGH &&
          IncomingCallLifecycleService.start(
            applicationContext, callId, callerName, callerPhone, avatarUrl, expiresAtMs
          )
        ) return@launch
      val callerName = data["caller_name"]?.takeIf { it.isNotBlank() } ?: callerPhone
      val avatarUrl = data["caller_avatar_url"]?.takeIf { it.startsWith("https://") }
      createCallChannel(notificationManager)
  
      val requestBase = callId.hashCode()
      val openIntent = Intent(this@IncomingCallMessagingService, MainActivity::class.java).apply {
        putExtra("linecall_call_id", callId)
        putExtra("linecall_call_action", "open")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
      val fullScreenPendingIntent = PendingIntent.getActivity(
        this@IncomingCallMessagingService, requestBase, openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      // Answer opens the app directly. The app's single CallingService accepts
      // and connects the call; no second LiveKit room is created in a helper service.
      val answerIntent = Intent(this@IncomingCallMessagingService, MainActivity::class.java).apply {
        putExtra("linecall_call_id", callId)
        putExtra("linecall_call_action", "answer")
        putExtra("linecall_caller_name", callerName)
        putExtra("linecall_caller_phone", data["caller_phone"].orEmpty())
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
      val answerPendingIntent = PendingIntent.getActivity(
        this@IncomingCallMessagingService, requestBase + 1, answerIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      val declinePendingIntent = actionPendingIntent(callId, "decline", requestBase + 2, callerName, data["caller_phone"].orEmpty())
      val avatar = avatarUrl?.let { url -> runCatching { java.net.URL(url).openConnection().apply { connectTimeout = 2_000; readTimeout = 2_000 }.getInputStream().use { BitmapFactory.decodeStream(it) } }.getOrNull() }
      val personBuilder = Person.Builder().setName(callerName).setUri("tel:$callerPhone").setImportant(true)
      if (avatar != null) personBuilder.setIcon(IconCompat.createWithBitmap(avatar))
      val person = personBuilder.build()
      getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("active_call_id", callId)
        .putLong("ring_started_at", System.currentTimeMillis()).apply()
      val notificationBuilder = NotificationCompat.Builder(this@IncomingCallMessagingService, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(callerName)
        .setContentText("Incoming call · $callerPhone")
        .setSubText(callerPhone)
        .setCategory(NotificationCompat.CATEGORY_CALL)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setOngoing(true)
        .setAutoCancel(false)
        .setTimeoutAfter(remainingRingMs)
        // Keep the call on the lock screen/shade. Do not auto-launch MainActivity;
        // Answer and Decline must work directly from the notification actions.
        .setContentIntent(fullScreenPendingIntent)
        .setOnlyAlertOnce(true)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .setStyle(NotificationCompat.CallStyle.forIncomingCall(person, declinePendingIntent, answerPendingIntent))
      if (Build.VERSION.SDK_INT < 34 || notificationManager.canUseFullScreenIntent()) notificationBuilder.setFullScreenIntent(fullScreenPendingIntent, true)
      notificationManager.notify(notificationId(callId), notificationBuilder.build())
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
    const val PREFS = "line_call_incoming_notification"
    const val MAX_RING_DURATION_MS = 60_000L
    fun notificationId(callId: String): Int = callId.hashCode().takeIf { it != 0 } ?: 4101
  }
}

class IncomingCallActionReceiver : android.content.BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val callId = intent.getStringExtra("linecall_call_id")?.takeIf { it.isNotBlank() } ?: return
    when (intent.getStringExtra("linecall_call_action")) {
      "decline", "end" -> {
        IncomingCallLifecycleService.stop(context.applicationContext, callId)
        // Dismiss the local ringing surface immediately. The server update is
        // delivered asynchronously so network latency never leaves a dead
        // Answer/Decline notification on the user's screen.
        context.getSystemService(NotificationManager::class.java)
          .cancel(IncomingCallMessagingService.notificationId(callId))
        context.getSharedPreferences(IncomingCallMessagingService.PREFS, Context.MODE_PRIVATE).edit().remove("active_call_id").remove("ring_started_at").apply()
        if (intent.getStringExtra("linecall_call_action") == "end") LineCallOngoingCallService.stop(context.applicationContext)
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
          try {
            val backend = LineCallSupabaseClient(context.applicationContext)
            withTimeoutOrNull(12_000L) {
              backend.restoreSession()
              backend.callFunction(JSONObject().put("action", if (intent.getStringExtra("linecall_call_action") == "end") "end" else "decline").put("call_id", callId))
              if (intent.getStringExtra("linecall_call_action") == "decline") {
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
