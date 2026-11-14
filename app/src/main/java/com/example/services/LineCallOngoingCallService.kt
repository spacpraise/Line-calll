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
import com.example.data.model.ActiveCallSession

/** Reuses the incoming notification ID and upgrades it to Android's ongoing CallStyle. */
class LineCallOngoingCallService : Service() {
  override fun onCreate() {
    super.onCreate()
    createChannel()
    // Keep the Telecom manager available from the foreground call lifecycle.
    LineCallTelecom.initialize(this)
  }
  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val callIntent = intent
    val callId = callIntent?.getStringExtra(EXTRA_CALL_ID).orEmpty()
    if (callIntent == null || callId.isBlank()) { stopSelf(startId); return START_NOT_STICKY }
    val callerName = callIntent.getStringExtra(EXTRA_CALLER_NAME).orEmpty().ifBlank { callIntent.getStringExtra(EXTRA_CALLER_PHONE).orEmpty() }
    val callerPhone = callIntent.getStringExtra(EXTRA_CALLER_PHONE).orEmpty()
    val personBuilder = Person.Builder().setName(callerName.ifBlank { "Line Call" }).setImportant(true)
    if (callerPhone.isNotBlank()) personBuilder.setUri("tel:$callerPhone")
    val person = personBuilder.build()
    val openApp = PendingIntent.getActivity(this, callId.hashCode(), Intent(this, MainActivity::class.java).apply {
      putExtra("linecall_call_id", callId); putExtra("linecall_call_action", "open")
      addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val hangup = PendingIntent.getBroadcast(this, callId.hashCode() xor 0x4C43,
      Intent(this, IncomingCallActionReceiver::class.java).apply {
        putExtra("linecall_call_id", callId); putExtra("linecall_call_action", "end")
      }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.sym_action_call)
      .setContentTitle(callerName.ifBlank { "Line Call" })
      .setContentText(if (callerPhone.isBlank()) "Ongoing call · Tap to return" else callerPhone)
      .setSubText("Line Call · Ongoing").setCategory(NotificationCompat.CATEGORY_CALL)
      .setPriority(NotificationCompat.PRIORITY_HIGH).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setOngoing(true).setOnlyAlertOnce(true).setContentIntent(openApp)
      .setStyle(NotificationCompat.CallStyle.forOngoingCall(person, hangup)).build()
    val id = IncomingCallMessagingService.notificationId(callId)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      startForeground(
        id,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
      )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
    } else {
      startForeground(id, notification)
    }
    return START_NOT_STICKY
  }
  override fun onBind(intent: Intent?): IBinder? = null
  override fun onDestroy() { stopForeground(STOP_FOREGROUND_REMOVE); super.onDestroy() }
  private fun createChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = getSystemService(NotificationManager::class.java)
    if (manager.getNotificationChannel(CHANNEL_ID) == null) manager.createNotificationChannel(
      NotificationChannel(CHANNEL_ID, "Active call", NotificationManager.IMPORTANCE_LOW).apply {
        description = "Ongoing Line Call voice calls"; setSound(null, null); enableVibration(false)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
      })
  }
  companion object {
    private const val CHANNEL_ID = "line_call_active_call"
    private const val EXTRA_CALL_ID = "linecall_call_id"
    private const val EXTRA_CALLER_NAME = "linecall_caller_name"
    private const val EXTRA_CALLER_PHONE = "linecall_caller_phone"
    fun start(context: Context, session: ActiveCallSession) {
      val intent = Intent(context, LineCallOngoingCallService::class.java).apply {
        putExtra(EXTRA_CALL_ID, session.callId); putExtra(EXTRA_CALLER_NAME, session.contactName.orEmpty())
        putExtra(EXTRA_CALLER_PHONE, session.phoneNumber)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
    }
    fun stop(context: Context) { context.stopService(Intent(context, LineCallOngoingCallService::class.java)) }
  }
}
