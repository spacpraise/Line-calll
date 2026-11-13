package com.example.services

import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * Authenticated Supabase Realtime subscription for this user's durable call event inbox.
 * Reconnects with bounded exponential backoff. HTTP polling remains the recovery path.
 */
class CallRealtimeClient(private val supabase: LineCallSupabaseClient) {
  private val http = OkHttpClient.Builder()
    .pingInterval(25, TimeUnit.SECONDS)
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(0, TimeUnit.MILLISECONDS)
    .build()

  fun start(
    userId: String,
    scope: CoroutineScope,
    onEvent: (JSONObject) -> Unit,
    onConnectionChanged: (Boolean) -> Unit
  ): Job = scope.launch(Dispatchers.IO) {
    var attempt = 0
    while (isActive) {
      val credentials = runCatching { supabase.realtimeCredentials() }.getOrNull()
      if (credentials == null) {
        onConnectionChanged(false)
        delay(backoff(attempt++))
        continue
      }

      val joined = CompletableDeferred<Boolean>()
      val disconnected = CompletableDeferred<Unit>()
      val topic = "realtime:public:call_events"
      val joinRef = "1"
      val encodedKey = URLEncoder.encode(credentials.anonKey, "UTF-8")
      val wsBase = credentials.supabaseUrl.trimEnd('/')
        .replaceFirst("https://", "wss://")
        .replaceFirst("http://", "ws://")
      val request = Request.Builder()
        .url("$wsBase/realtime/v1/websocket?apikey=$encodedKey&vsn=1.0.0")
        .header("apikey", credentials.anonKey)
        .header("Authorization", "Bearer ${credentials.accessToken}")
        .build()

      lateinit var socket: WebSocket
      val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
          socket = webSocket
          val filters = JSONArray().put(
            JSONObject()
              .put("event", "*")
              .put("schema", "public")
              .put("table", "call_events")
              .put("filter", "user_id=eq.$userId")
          )
          val config = JSONObject()
            .put("broadcast", JSONObject().put("ack", false).put("self", false))
            .put("presence", JSONObject().put("key", ""))
            .put("postgres_changes", filters)
          val payload = JSONObject()
            .put("config", config)
            .put("access_token", credentials.accessToken)
          webSocket.send(envelope(topic, "phx_join", payload, joinRef, joinRef))
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
          runCatching {
            val message = JSONObject(text)
            when (message.optString("event")) {
              "phx_reply" -> {
                val payload = message.optJSONObject("payload")
                if (payload?.optString("status") == "ok") joined.complete(true)
                else {
                  joined.complete(false)
                  disconnected.complete(Unit)
                }
              }
              "postgres_changes" -> {
                val data = message.optJSONObject("payload")?.optJSONObject("data")
                val record = data?.optJSONObject("record")
                if (record != null && record.optString("user_id") == userId) onEvent(record)
              }
              "phx_error", "phx_close" -> disconnected.complete(Unit)
            }
          }.onFailure { Log.w(TAG, "Ignoring malformed Realtime frame", it) }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
          disconnected.complete(Unit)
          webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
          disconnected.complete(Unit)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
          Log.w(TAG, "Realtime socket disconnected", t)
          disconnected.complete(Unit)
        }
      }

      socket = http.newWebSocket(request, listener)
      val didJoin = withTimeoutOrNull(12_000L) { joined.await() } ?: false
      if (!didJoin) {
        socket.cancel()
        onConnectionChanged(false)
        disconnected.complete(Unit)
      } else {
        attempt = 0
        onConnectionChanged(true)
        val heartbeat = launch(Dispatchers.IO) {
          var ref = 2
          while (isActive) {
            delay(25_000L)
            if (!socket.send(envelope("phoenix", "heartbeat", JSONObject(), ref.toString(), null))) {
              disconnected.complete(Unit)
              break
            }
            ref++
          }
        }
        disconnected.await()
        heartbeat.cancel()
        socket.close(1000, "Reconnect")
        onConnectionChanged(false)
      }
      if (isActive) delay(backoff(attempt++))
    }
  }

  private fun envelope(topic: String, event: String, payload: JSONObject, ref: String, joinRef: String?): String =
    JSONObject()
      .put("topic", topic)
      .put("event", event)
      .put("payload", payload)
      .put("ref", ref)
      .put("join_ref", joinRef ?: JSONObject.NULL)
      .toString()

  private fun backoff(attempt: Int): Long = min(30_000L, 1_000L shl min(attempt, 5))

  private companion object {
    const val TAG = "LineCallRealtime"
  }
}
