package com.example.services

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.livekit.android.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import livekit.org.webrtc.RTCStatsCollectorCallback
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Read-only network diagnostics for an active voice call.
 *
 * LiveKit/WebRTC remains responsible for congestion control, ICE fallback and
 * reconnection. This monitor reports the actual transport conditions to the UI;
 * it deliberately does not force a codec, bitrate, or network handover.
 */
data class CallNetworkSnapshot(
  val transport: String = "Unknown",
  val networkAvailable: Boolean = false,
  val estimatedUploadKbps: Int? = null,
  val estimatedDownloadKbps: Int? = null,
  val connectionQuality: String = "Unknown",
  val roundTripTimeMs: Double? = null,
  val jitterMs: Double? = null,
  val packetLossPercent: Double? = null,
  val audioUploadKbps: Double? = null,
  val audioDownloadKbps: Double? = null,
  val reconnecting: Boolean = false
)

class CallNetworkMonitor(
  private val connectivityManager: ConnectivityManager,
  private val scope: CoroutineScope
) {
  private val _snapshot = MutableStateFlow(CallNetworkSnapshot())
  val snapshot: StateFlow<CallNetworkSnapshot> = _snapshot.asStateFlow()

  private var statsJob: Job? = null
  private var room: Room? = null
  private var previousSentBytes: Long? = null
  private var previousReceivedBytes: Long? = null
  private var previousPacketsLost: Long? = null
  private var previousPacketsReceived: Long? = null
  private var previousSampleAtMs: Long? = null
  private val registered = AtomicBoolean(false)

  private val networkCallback = object : ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) = refreshNetwork()
    override fun onLost(network: Network) = refreshNetwork()
    override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
      publishCapabilities(capabilities)
    }
  }

  fun start(activeRoom: Room) {
    room = activeRoom
    refreshNetwork()
    if (registered.compareAndSet(false, true)) {
      runCatching { connectivityManager.registerDefaultNetworkCallback(networkCallback) }
        .onFailure { registered.set(false) }
    }
    statsJob?.cancel()
    statsJob = scope.launch {
      while (isActive && room === activeRoom) {
        collectStats(activeRoom)
        delay(2_000L)
      }
    }
  }

  fun setConnectionQuality(quality: String) {
    _snapshot.value = _snapshot.value.copy(connectionQuality = quality)
  }

  fun setReconnecting(reconnecting: Boolean) {
    _snapshot.value = _snapshot.value.copy(reconnecting = reconnecting)
  }

  fun stop() {
    statsJob?.cancel()
    statsJob = null
    room = null
    if (registered.compareAndSet(true, false)) {
      runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
    }
    previousSentBytes = null
    previousReceivedBytes = null
    previousPacketsLost = null
    previousPacketsReceived = null
    previousSampleAtMs = null
    _snapshot.value = CallNetworkSnapshot()
  }

  private fun refreshNetwork() {
    val active = connectivityManager.activeNetwork
    val capabilities = active?.let { connectivityManager.getNetworkCapabilities(it) }
    if (capabilities == null) {
      _snapshot.value = _snapshot.value.copy(
        transport = "No internet",
        networkAvailable = false,
        estimatedUploadKbps = null,
        estimatedDownloadKbps = null
      )
    } else {
      publishCapabilities(capabilities)
    }
  }

  private fun publishCapabilities(capabilities: NetworkCapabilities) {
    val transport = when {
      capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
      capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
      capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
      capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
      else -> "Other network"
    }
    _snapshot.value = _snapshot.value.copy(
      transport = transport,
      networkAvailable = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
      estimatedUploadKbps = capabilities.linkUpstreamBandwidthKbps.takeIf { it > 0 },
      estimatedDownloadKbps = capabilities.linkDownstreamBandwidthKbps.takeIf { it > 0 }
    )
  }

  private fun collectStats(activeRoom: Room) {
    val publisherCallback = RTCStatsCollectorCallback { report ->
      val values = report.statsMap.values
      val outbound = values.filter {
        it.type == "outbound-rtp" && it.members["kind"] == "audio"
      }
      val selectedPair = values.firstOrNull {
        it.type == "candidate-pair" &&
          it.members["state"] == "succeeded" &&
          (it.members["selected"] == true || it.members["nominated"] == true)
      }
      val sentBytes = outbound.sumOf { (it.members["bytesSent"] as? Number)?.toLong() ?: 0L }
      val now = android.os.SystemClock.elapsedRealtime()
      val previousTime = previousSampleAtMs
      val elapsedSeconds = if (previousTime != null && now > previousTime) (now - previousTime) / 1000.0 else 0.0
      val uploadKbps = if (elapsedSeconds > 0 && previousSentBytes != null && sentBytes >= previousSentBytes!!) {
        (sentBytes - previousSentBytes!!) * 8.0 / elapsedSeconds / 1000.0
      } else null
      previousSentBytes = sentBytes
      previousSampleAtMs = now
      val rttSeconds = selectedPair?.members?.get("currentRoundTripTime") as? Number
      _snapshot.value = _snapshot.value.copy(
        roundTripTimeMs = rttSeconds?.toDouble()?.times(1000.0),
        audioUploadKbps = uploadKbps
      )
    }
    val subscriberCallback = RTCStatsCollectorCallback { report ->
      val inbound = report.statsMap.values.filter {
        it.type == "inbound-rtp" && it.members["kind"] == "audio"
      }
      val receivedBytes = inbound.sumOf { (it.members["bytesReceived"] as? Number)?.toLong() ?: 0L }
      val packetsLost = inbound.sumOf { (it.members["packetsLost"] as? Number)?.toLong() ?: 0L }
      val packetsReceived = inbound.sumOf { (it.members["packetsReceived"] as? Number)?.toLong() ?: 0L }
      val now = android.os.SystemClock.elapsedRealtime()
      val previousTime = previousSampleAtMs
      val elapsedSeconds = if (previousTime != null && now > previousTime) (now - previousTime) / 1000.0 else 0.0
      val downloadKbps = if (elapsedSeconds > 0 && previousReceivedBytes != null && receivedBytes >= previousReceivedBytes!!) {
        (receivedBytes - previousReceivedBytes!!) * 8.0 / elapsedSeconds / 1000.0
      } else null
      val lostDelta = previousPacketsLost?.let { (packetsLost - it).coerceAtLeast(0L) }
      val receivedDelta = previousPacketsReceived?.let { (packetsReceived - it).coerceAtLeast(0L) }
      val totalPackets = (lostDelta ?: 0L) + (receivedDelta ?: 0L)
      val lossPercent = if (totalPackets > 0) (lostDelta ?: 0L) * 100.0 / totalPackets else null
      val jitterSeconds = inbound.mapNotNull { it.members["jitter"] as? Number }.maxOfOrNull { it.toDouble() }
      previousReceivedBytes = receivedBytes
      previousPacketsLost = packetsLost
      previousPacketsReceived = packetsReceived
      _snapshot.value = _snapshot.value.copy(
        jitterMs = jitterSeconds?.times(1000.0),
        packetLossPercent = lossPercent,
        audioDownloadKbps = downloadKbps
      )
    }
    runCatching { activeRoom.getPublisherRTCStats(publisherCallback) }
    runCatching { activeRoom.getSubscriberRTCStats(subscriberCallback) }
  }
}
