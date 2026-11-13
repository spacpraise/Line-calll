package com.example.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

/**
 * Owns the bounded incoming-ringing phase. It is deliberately a shortService:
 * incoming ringing is capped at 60 seconds and must never become an indefinite FGS.
 * The connected call is handed to LineCallOngoingCallService after acceptance.
 */
class IncomingCallLifecycleService : Service() {
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var lifecycleJob: Job? = null
  private var realtimeJob: Job? = null
  private var ringtone: Ringtone? = null
  private var activeCallId: String? = null
  private var expiryAtMs: Long = 0L
  private var callerName: String = ""
  private var callerPhone: String = ""
  private var callerAvatarUrl: String? = null
  private var isStopping = false

  override fun onCreate() {
    super.onCreate()
    createChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent == null || intent.action != ACTION_START) {
      stopSelf(startId)
      return START_NOT_STICKY
    }
    val callId = intent.getStringExtra(EXTRA_CALL_ID)?.takeIf { it.isNotBlank() }
    if (callId == null) {
      stopSelf(startId)
      return START_NOT_STICKY
    }

    // A duplicate FCM/Realtime delivery for the same call updates its metadata,
    // but never creates a second ringtone or a second notification.
    if (activeCallId == callId && lifecycleJob?.isActive == true) {
      val incomingExpiry = intent.getLongExtra(EXTRA_EXPIRES_AT, expiryAtMs)
      if (incomingExpiry in 1 until expiryAtMs) {
        expiryAtMs = incomingExpiry
        updateNotification()
      }
      return START_NOT_STICKY
    }

    stopCurrentResources()
    activeCallId = callId
    callerName = intent.getStringExtra(EXTRA_CALLER_NAME).orEmpty()
    callerPhone = intent.getStringExtra(EXTRA_CALLER_PHONE).orEmpty()
    callerAvatarUrl = intent.getStringExtra(EXTRA_AVATAR_URL)?.takeIf { it.startsWith("https://") }
    val suppliedExpiry = intent.getLongExtra(EXTRA_EXPIRES_AT, 0L)
    val fallbackExpiry = System.currentTimeMillis() + IncomingCallMessagingService.MAX_RING_DURATION_MS
    expiryAtMs = if (suppliedExpiry > 0L) minOf(suppliedExpiry, fallbackExpiry) else fallbackExpiry
    if (expiryAtMs <= System.currentTimeMillis()) {
      stopSelf(startId)
      return START_NOT_STICKY
    }

    persistState()
    startForegroundCallNotification()
    startRingtone()
    lifecycleJob = serviceScope.launch { monitorCall(callId, expiryAtMs) }
    return START_NOT_STICKY
  }

  private fun startForegroundCallNotification() {
    val notification = buildNotification(null)
    val id = IncomingCallMessagingService.notificationId(activeCallId ?: return)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)
    } else {
      startForeground(id, notification)
    }
  }

  private fun updateNotification(bitmap: android.graphics.Bitmap? = null) {
    val id = activeCallId?.let(IncomingCallMessagingService::notificationId) ?: return
    getSystemService(NotificationManager::class.java).notify(id, buildNotification(bitmap))
  }

  private fun buildNotification(bitmap: android.graphics.Bitmap?): Notification {
    val callId = activeCallId.orEmpty()
    val displayName = callerName.ifBlank { callerPhone.ifBlank { "Unknown caller" } }
    val requestBase = callId.hashCode()
    val openIntent = Intent(this, MainActivity::class.java).apply {
      putExtra("linecall_call_id", callId)
      putExtra("linecall_call_action", "open")
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val fullScreen = PendingIntent.getActivity(
      this, requestBase, openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val answerIntent = Intent(this, MainActivity::class.java).apply {
      putExtra("linecall_call_id", callId)
      putExtra("linecall_call_action", "answer")
      putExtra("linecall_caller_name", displayName)
      putExtra("linecall_caller_phone", callerPhone)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val answer = PendingIntent.getActivity(
      this, requestBase + 1, answerIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val declineIntent = Intent(this, IncomingCallActionReceiver::class.java).apply {
      putExtra("linecall_call_id", callId)
      putExtra("linecall_call_action", "decline")
      putExtra("linecall_caller_name", displayName)
      putExtra("linecall_caller_phone", callerPhone)
    }
    val decline = PendingIntent.getBroadcast(
      this, requestBase + 2, declineIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val personBuilder = Person.Builder().setName(displayName).setImportant(true)
    if (callerPhone.isNotBlank()) personBuilder.setUri("tel:$callerPhone")
    if (bitmap != null) personBuilder.setIcon(IconCompat.createWithBitmap(bitmap))
    val person = personBuilder.build()
    val remaining = (expiryAtMs - System.currentTimeMillis()).coerceAtLeast(1L)
    val builder = NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(displayName)
      .setContentText(if (callerPhone.isBlank()) "Incoming call" else "Incoming call · $callerPhone")
      .setSubText(callerPhone)
      .setCategory(NotificationCompat.CATEGORY_CALL)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setOngoing(true)
      .setAutoCancel(false)
      .setTimeoutAfter(remaining)
      .setOnlyAlertOnce(true)
      .setContentIntent(fullScreen)
      .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
      .setStyle(NotificationCompat.CallStyle.forIncomingCall(person, decline, answer))
    val manager = getSystemService(NotificationManager::class.java)
    if (Build.VERSION.SDK_INT < 34 || manager.canUseFullScreenIntent()) {
      builder.setFullScreenIntent(fullScreen, true)
    }
    return builder.build()
  }

  private fun startRingtone() {
    runCatching {
      val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE) ?: return
      ringtone = RingtoneManager.getRingtone(applicationContext, uri)?.apply {
        audioAttributes = AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
        play()
      }
    }.onFailure { android.util.Log.w(TAG, "Unable to start incoming ringtone", it) }
  }

  private suspend fun monitorCall(callId: String, expiresAt: Long) {
    val backend = LineCallSupabaseClient(applicationContext)
    val user = withTimeoutOrNull(5_000L) { runCatching { backend.restoreSession() }.getOrNull() }
    if (user == null) {
      android.util.Log.w(TAG, "No authenticated session for incoming call lifecycle")
      stopLifecycle(callId)
      return
    }

    realtimeJob = CallRealtimeClient(backend).start(
      userId = user.id,
      scope = serviceScope,
      onEvent = { event ->
        if (event.optString("call_id") == callId &&
          event.optString("event_type") in TERMINAL_OR_ANSWER_EVENTS
        ) {
          stopLifecycle(callId)
        }
      },
      onConnectionChanged = { connected ->
        android.util.Log.d(TAG, "Incoming call Realtime connected=$connected")
      }
    )

    // Realtime is the fast path; bounded status checks are the recovery path.
    while (serviceScope.isActive && activeCallId == callId) {
      val remaining = expiresAt - System.currentTimeMillis()
      if (remaining <= 0L) {
        stopLifecycle(callId)
        return
      }
      val status = withTimeoutOrNull(4_000L) {
        runCatching {
          backend.callFunction(JSONObject().put("action", "status").put("call_id", callId))
            .optJSONObject("call")?.optString("status").orEmpty()
        }.getOrDefault("")
      }.orEmpty()
      if (status.isNotBlank() && status != "ringing") {
        stopLifecycle(callId)
        return
      }
      if (callerAvatarUrl != null) {
        val url = callerAvatarUrl
        callerAvatarUrl = null
        val bitmap = withTimeoutOrNull(3_000L) {
          runCatching {
            java.net.URL(url).openConnection().apply {
              connectTimeout = 2_000
              readTimeout = 2_000
            }.getInputStream().use { BitmapFactory.decodeStream(it) }
          }.getOrNull()
        }
        if (bitmap != null && activeCallId == callId) updateNotification(bitmap)
      }
      delay(minOf(2_500L, (expiresAt - System.currentTimeMillis()).coerceAtLeast(100L)))
    }
  }

  private fun persistState() {
    getSharedPreferences(IncomingCallMessagingService.PREFS, MODE_PRIVATE).edit()
      .putString("active_call_id", activeCallId)
      .putString("active_caller_name", callerName)
      .putString("active_caller_phone", callerPhone)
      .putString("active_caller_avatar", callerAvatarUrl)
      .putLong("ring_expires_at", expiryAtMs)
      .putLong("ring_started_at", System.currentTimeMillis())
      .apply()
  }

  private fun stopLifecycle(callId: String) {
    if (isStopping || activeCallId != callId) return
    isStopping = true
    runCatching { ringtone?.stop() }
    ringtone = null
    stopSelf()
  }

  private fun stopCurrentResources() {
    lifecycleJob?.cancel()
    lifecycleJob = null
    realtimeJob?.cancel()
    realtimeJob = null
    runCatching { ringtone?.stop() }
    ringtone = null
  }

  override fun onTimeout(startId: Int) {
    android.util.Log.w(TAG, "Incoming call shortService reached Android timeout")
    stopLifecycle(activeCallId.orEmpty())
  }

  override fun onTimeout(startId: Int, fgsType: Int) {
    android.util.Log.w(TAG, "Incoming call foreground service timed out (type=$fgsType)")
    stopLifecycle(activeCallId.orEmpty())
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onDestroy() {
    val callId = activeCallId
    stopCurrentResources()
    if (callId != null) {
      val prefs = getSharedPreferences(IncomingCallMessagingService.PREFS, MODE_PRIVATE)
      if (prefs.getString("active_call_id", null) == callId) {
        getSystemService(NotificationManager::class.java)
          .cancel(IncomingCallMessagingService.notificationId(callId))
        prefs.edit().remove("active_call_id").remove("active_caller_name")
          .remove("active_caller_phone").remove("active_caller_avatar")
          .remove("ring_expires_at").remove("ring_started_at").apply()
      }
    }
    serviceScope.cancel()
    super.onDestroy()
  }

  private fun createChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = getSystemService(NotificationManager::class.java)
    if (manager.getNotificationChannel(CHANNEL_ID) == null) {
      manager.createNotificationChannel(NotificationChannel(
        CHANNEL_ID, "Incoming call lifecycle", NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Live incoming call alerts and ringing"
        setSound(null, null)
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 700, 400, 700)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
      })
    }
  }

  companion object {
    private const val TAG = "LineCallIncoming"
    private const val CHANNEL_ID = "linecall_incoming_lifecycle"
    private const val ACTION_START = "com.aistudio.linecall.action.START_INCOMING"
    private const val EXTRA_CALL_ID = "linecall_call_id"
    private const val EXTRA_CALLER_NAME = "linecall_caller_name"
    private const val EXTRA_CALLER_PHONE = "linecall_caller_phone"
    private const val EXTRA_AVATAR_URL = "linecall_caller_avatar_url"
    private const val EXTRA_EXPIRES_AT = "linecall_ring_expires_at"

    fun start(
      context: Context,
      callId: String,
      callerName: String,
      callerPhone: String,
      avatarUrl: String?,
      expiresAtMs: Long
    ): Boolean {
      val intent = Intent(context, IncomingCallLifecycleService::class.java).apply {
        action = ACTION_START
        putExtra(EXTRA_CALL_ID, callId)
        putExtra(EXTRA_CALLER_NAME, callerName)
        putExtra(EXTRA_CALLER_PHONE, callerPhone)
        putExtra(EXTRA_AVATAR_URL, avatarUrl)
        putExtra(EXTRA_EXPIRES_AT, expiresAtMs)
      }
      return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
        else context.startService(intent)
        true
      } catch (error: Exception) {
        android.util.Log.w(TAG, "Android did not allow the incoming-call FGS start", error)
        false
      }
    }

    fun stop(context: Context, callId: String? = null) {
      context.stopService(Intent(context, IncomingCallLifecycleService::class.java))
      val prefs = context.getSharedPreferences(IncomingCallMessagingService.PREFS, Context.MODE_PRIVATE)
      val active = prefs.getString("active_call_id", null)
      if (callId == null || active == callId) {
        if (active != null) {
          context.getSystemService(NotificationManager::class.java)
            .cancel(IncomingCallMessagingService.notificationId(active))
        }
        prefs.edit().remove("active_call_id").remove("active_caller_name")
          .remove("active_caller_phone").remove("active_caller_avatar")
          .remove("ring_expires_at").remove("ring_started_at").apply()
      }
    }

    fun recoverIfNeeded(context: Context) {
      val prefs = context.getSharedPreferences(IncomingCallMessagingService.PREFS, Context.MODE_PRIVATE)
      val callId = prefs.getString("active_call_id", null) ?: return
      val expires = prefs.getLong("ring_expires_at", 0L)
      if (expires <= System.currentTimeMillis()) {
        stop(context, callId)
        return
      }
      start(
        context, callId,
        prefs.getString("active_caller_name", "").orEmpty(),
        prefs.getString("active_caller_phone", "").orEmpty(),
        prefs.getString("active_caller_avatar", null),
        expires
      )
    }
  }
}
