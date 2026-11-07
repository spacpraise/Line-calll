package com.example.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LineCallMessagingService : FirebaseMessagingService() {
  override fun onNewToken(token: String) {
    super.onNewToken(token)
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      LineCallSupabaseClient(applicationContext).saveFcmToken(token)
    }
  }

  override fun onMessageReceived(message: RemoteMessage) {
    super.onMessageReceived(message)
    if (message.data["type"] != "incoming_call") return

    val callId = message.data["call_id"] ?: return
    val callerName = message.data["caller_name"]?.takeIf { it.isNotBlank() }
      ?: message.data["caller_phone"]
      ?: "Someone"

    val manager = getSystemService(NotificationManager::class.java)
    createCallChannel(manager)

    val openApp = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra("incoming_call_id", callId)
    }
    val pendingIntent = PendingIntent.getActivity(
      this,
      callId.hashCode(),
      openApp,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(this, CALL_CHANNEL_ID)
      .setSmallIcon(android.R.drawable.sym_action_call)
      .setContentTitle("$callerName is calling")
      .setContentText("Incoming Line Call")
      .setCategory(NotificationCompat.CATEGORY_CALL)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setAutoCancel(true)
      .setTimeoutAfter(45_000L)
      .setContentIntent(pendingIntent)
      .build()

    NotificationManagerCompat.from(this).notify(callId.hashCode(), notification)
  }

  private fun createCallChannel(manager: NotificationManager) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    if (manager.getNotificationChannel(CALL_CHANNEL_ID) != null) return

    val ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    val audioAttributes = AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
      .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
      .build()
    val channel = NotificationChannel(
      CALL_CHANNEL_ID,
      "Incoming calls",
      NotificationManager.IMPORTANCE_HIGH
    ).apply {
      description = "Alerts for incoming Line Call voice calls"
      setSound(ringtone, audioAttributes)
      enableVibration(true)
    }
    manager.createNotificationChannel(channel)
  }

  companion object {
    const val CALL_CHANNEL_ID = "line_call_incoming_calls"
  }
}
