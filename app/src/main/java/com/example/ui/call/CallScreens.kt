package com.example.ui.call

import android.view.HapticFeedbackConstants

import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActiveCallSession
import com.example.data.model.CallAudioRoute
import com.example.data.model.CallSessionState
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CallRed
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun IncomingCallScreen(
  session: ActiveCallSession,
  onAccept: () -> Unit,
  onDecline: () -> Unit,
  modifier: Modifier = Modifier
) {
  val view = LocalView.current
  val infiniteTransition = rememberInfiniteTransition(label = "incoming_call_rings")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.94f,
    targetValue = 1.12f,
    animationSpec = infiniteRepeatable(
      animation = tween(1250, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "caller_pulse"
  )
  val callerName = session.contactName?.takeIf { it.isNotBlank() } ?: "Line Call user"
  val callerNumber = session.phoneNumber.takeIf { it.isNotBlank() } ?: "Internet voice call"
  val initials = (session.contactName?.trim()?.split(Regex("\\s+"))?.take(2)
    ?.mapNotNull { it.firstOrNull()?.toString() }?.joinToString("")
    ?.takeIf { it.isNotBlank() }
    ?: session.phoneNumber.filter { it.isLetterOrDigit() }.take(2).ifBlank { "LC" }).uppercase()

  Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background)
          )
        )
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.statusBars)
          .padding(horizontal = 28.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
              .clip(RoundedCornerShape(24.dp))
              .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f))
              .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
              .padding(horizontal = 14.dp, vertical = 8.dp)
          ) {
            Box(
              Modifier.size(7.dp).clip(CircleShape).background(CallGreen)
            )
            Text(
              "LINE CALL",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp
              ),
              color = MaterialTheme.colorScheme.onBackground
            )
          }
          Text(
            "INCOMING VOICE CALL",
            style = MaterialTheme.typography.labelMedium.copy(
              letterSpacing = 2.2.sp,
              fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f)
          )
        }

        androidx.compose.animation.AnimatedVisibility(
          visible = true,
          enter = androidx.compose.animation.fadeIn(animationSpec = tween(500)) +
            androidx.compose.animation.slideInVertically(
              animationSpec = tween(550, easing = FastOutSlowInEasing),
              initialOffsetY = { it / 12 }
            )
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(13.dp)
          ) {
            Box(
              modifier = Modifier.size(190.dp),
              contentAlignment = Alignment.Center
            ) {
              Box(
                Modifier.size(176.dp).scale(pulseScale)
                  .border(1.dp, CallGreen.copy(alpha = 0.18f), CircleShape)
              )
              Box(
                Modifier.size(150.dp).scale(2f - pulseScale)
                  .border(1.dp, CallGreen.copy(alpha = 0.12f), CircleShape)
              )
              Box(
                modifier = Modifier
                  .size(124.dp)
                  .clip(CircleShape)
                  .background(
                    Brush.linearGradient(
                      colors = listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.surfaceVariant)
                    )
                  )
                  .border(2.dp, CallGreen.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  initials,
                  style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp
                  ),
                  color = MaterialTheme.colorScheme.onBackground
                )
              }
            }

            Text(
              callerName,
              style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 29.sp
              ),
              color = MaterialTheme.colorScheme.onBackground,
              textAlign = TextAlign.Center,
              maxLines = 2
            )
            Text(
              callerNumber,
              style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
              ),
              color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
              textAlign = TextAlign.Center
            )
            Text(
              "is calling you",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.48f)
            )
          }
        }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(18.dp),
          modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
          ) {
            HoldSwipeUpAction(
              label = "Decline",
              hint = "Swipe to decline",
              icon = Icons.Rounded.CallEnd,
              color = CallRed,
              tag = "decline_call_button",
              onComplete = {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onDecline()
              }
            )
            HoldSwipeUpAction(
              label = "Answer",
              hint = "Swipe to answer",
              icon = Icons.Rounded.Phone,
              color = CallGreen,
              tag = "accept_call_button",
              onComplete = {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onAccept()
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun HoldSwipeUpAction(
  label: String,
  hint: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  color: Color,
  tag: String,
  onComplete: () -> Unit
) {
  val offset = remember { Animatable(0f) }
  val scope = rememberCoroutineScope()
  val density = LocalDensity.current
  val view = LocalView.current
  val threshold = with(density) { 42.dp.toPx() }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(9.dp),
    modifier = Modifier.width(132.dp)
  ) {
    Box(
      modifier = Modifier
        .size(82.dp)
        .offset { IntOffset(0, offset.value.roundToInt()) }
        .clip(CircleShape)
        .background(color)
        .border(1.dp, Color.White.copy(alpha = 0.24f), CircleShape)
        .pointerInput(threshold) {
          var totalDrag = 0f
          detectDragGestures(
            onDragStart = {
              totalDrag = 0f
              view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            },
            onDrag = { change, amount ->
              change.consume()
              totalDrag += kotlin.math.hypot(amount.x, amount.y)
              scope.launch {
                offset.snapTo((offset.value + amount.y).coerceIn(-threshold, threshold))
              }
            },
            onDragEnd = {
              if (totalDrag >= threshold) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                onComplete()
              } else {
                scope.launch {
                  offset.animateTo(0f, spring(dampingRatio = 0.58f, stiffness = 520f))
                }
              }
            },
            onDragCancel = {
              scope.launch {
                offset.animateTo(0f, spring(dampingRatio = 0.58f, stiffness = 520f))
              }
            }
          )
        }
        .testTag(tag),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = "$label: swipe to activate",
        tint = Color.White,
        modifier = Modifier.size(34.dp)
      )
    }
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
      color = MaterialTheme.colorScheme.onBackground
    )
    Text(
      text = hint,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.48f),
      textAlign = TextAlign.Center
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveCallScreen(
  session: ActiveCallSession,
  onMuteToggle: () -> Unit,
  onSpeakerToggle: () -> Unit,
  audioRoutes: List<CallAudioRoute> = emptyList(),
  selectedAudioRouteId: Int? = null,
  onRefreshAudioRoutes: () -> Unit = {},
  onAudioRouteSelected: (Int) -> Unit = {},
  onEndCall: () -> Unit,
  onRecall: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val view = LocalView.current
  var showAudioSheet by remember { androidx.compose.runtime.mutableStateOf(false) }
  val selectedAudioRoute = audioRoutes.firstOrNull { it.id == selectedAudioRouteId }
  val minutes = session.durationSeconds / 60
  val seconds = session.durationSeconds % 60
  val timerString = String.format("%02d:%02d", minutes, seconds)
  val stateText = when (session.state) {
    CallSessionState.DIALING -> "Dialing…"
    CallSessionState.RINGING -> "Ringing…"
    CallSessionState.ACCEPTING -> "Answering…"
    CallSessionState.CONNECTED -> timerString
    CallSessionState.ENDED -> "Call ended"
    CallSessionState.IDLE -> ""
  }
  val connected = session.state == CallSessionState.CONNECTED
  val initials = (session.contactName?.trim()?.split(Regex("\\s+"))
    ?.take(2)?.mapNotNull { it.firstOrNull()?.toString() }?.joinToString("")
    ?.takeIf { it.isNotBlank() }
    ?: session.phoneNumber.filter { it.isLetterOrDigit() }.take(2).ifBlank { "LC" }).uppercase()

  Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Brush.verticalGradient(listOf(Color(0xFF142019), MaterialTheme.colorScheme.background, Color(0xFF070807))))
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.statusBars)
          .padding(horizontal = 22.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier
              .clip(RoundedCornerShape(24.dp))
              .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f))
              .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.09f), RoundedCornerShape(24.dp))
              .padding(horizontal = 13.dp, vertical = 8.dp)
          ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(CallGreen))
            Text(
              "LINE CALL",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp),
              color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f)
            )
          }

          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.055f),
            shadowElevation = 0.dp
          ) {
            Column(
              modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f), RoundedCornerShape(30.dp))
                .padding(horizontal = 20.dp, vertical = 22.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(104.dp)
                  .clip(CircleShape)
                  .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.surfaceVariant)))
                  .border(2.dp, CallGreen.copy(alpha = if (connected) 0.72f else 0.35f), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  initials,
                  style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 36.sp),
                  color = MaterialTheme.colorScheme.onBackground
                )
              }
              Text(
                session.contactName?.takeIf { it.isNotBlank() } ?: "Line Call user",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 2
              )
              Text(
                session.phoneNumber,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f),
                textAlign = TextAlign.Center
              )
              Text(
                stateText,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.SemiBold,
                  letterSpacing = if (connected) 1.2.sp else 0.2.sp
                ),
                color = if (connected) CallGreen else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                modifier = Modifier.testTag("call_duration_timer")
              )
              if (connected) ActiveCallWaveform()
            }
          }
        }

        Surface(
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
          shape = RoundedCornerShape(30.dp),
          color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.055f),
          shadowElevation = 0.dp
        ) {
          Column(
            modifier = Modifier
              .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f), RoundedCornerShape(30.dp))
              .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
          ) {
            if (session.state == CallSessionState.ENDED) {
              Surface(
                onClick = onRecall,
                shape = RoundedCornerShape(24.dp),
                color = CallGreen,
                modifier = Modifier.fillMaxWidth().height(56.dp).testTag("recall_call_button")
              ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Rounded.Phone, contentDescription = null, tint = Color.White)
                  Text("Call again", modifier = Modifier.padding(start = 10.dp), color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                }
              }
            } else {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
              ) {
                ActiveCallControl(
                  label = if (session.isMuted) "Unmute" else "Mute",
                  icon = if (session.isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                  selected = session.isMuted,
                  testTag = "toggle_mute_button"
                ) {
                  view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                  onMuteToggle()
                }
                ActiveCallControl(
                  label = "Audio",
                  icon = Icons.Rounded.VolumeUp,
                  selected = selectedAudioRouteId != null,
                  testTag = "call_audio_route_button"
                ) {
                  view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                  onRefreshAudioRoutes()
                  showAudioSheet = true
                }
                ActiveCallControl(
                  label = "Record",
                  icon = Icons.Rounded.FiberManualRecord,
                  selected = false,
                  testTag = "record_call_button"
                ) {
                  // Recording is a visual placeholder for a later local recorder implementation.
                }
              }
              Surface(
                onClick = {
                  view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                  onEndCall()
                },
                shape = RoundedCornerShape(28.dp),
                color = CallRed,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth(0.78f).height(58.dp).testTag("end_call_button")
              ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Rounded.CallEnd, contentDescription = "End call", tint = Color.White, modifier = Modifier.size(25.dp))
                  Text("End call", modifier = Modifier.padding(start = 10.dp),
                    color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
              }
            }
          }
        }
      }
    }
  }

  if (showAudioSheet) {
    ModalBottomSheet(
      onDismissRequest = { showAudioSheet = false },
      sheetState = rememberModalBottomSheetState(),
      containerColor = MaterialTheme.colorScheme.surface
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text("Audio output", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Text("Choose where you want to hear this call.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        if (audioRoutes.isEmpty()) {
          Text("No external audio devices are currently available.", modifier = Modifier.padding(vertical = 18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
          audioRoutes.forEach { route ->
            Surface(
              onClick = {
                onAudioRouteSelected(route.id)
                showAudioSheet = false
              },
              shape = RoundedCornerShape(16.dp),
              color = if (route.id == selectedAudioRouteId) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Icon(
                  imageVector = if (route.id == selectedAudioRouteId) Icons.Rounded.CheckCircle else Icons.Rounded.VolumeUp,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurface
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(route.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                  Text(route.type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (route.id == selectedAudioRouteId) Text("Active", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
              }
            }
          }
        }
        Spacer(Modifier.height(20.dp))
      }
    }
  }
}

@Composable
private fun ActiveCallControl(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  selected: Boolean,
  testTag: String,
  onClick: () -> Unit
) {
  val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val scale by androidx.compose.animation.core.animateFloatAsState(
    targetValue = if (pressed) 0.91f else 1f,
    animationSpec = spring(dampingRatio = 0.58f, stiffness = 500f),
    label = "active_call_control_scale"
  )
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Surface(
      onClick = onClick,
      interactionSource = interaction,
      shape = CircleShape,
      color = if (selected) CallGreen else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.09f),
      modifier = Modifier.size(66.dp).graphicsLayer {
        scaleX = scale
        scaleY = scale
      }.testTag(testTag)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.size(27.dp)
        )
      }
    }
    Text(
      label,
      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
      color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f)
    )
  }
}

@Composable
private fun ActiveCallWaveform() {
  val transition = androidx.compose.animation.core.rememberInfiniteTransition(
    label = "active_call_waveform"
  )
  val heights = listOf(
    transition.animateFloat(
      7f, 22f,
      androidx.compose.animation.core.infiniteRepeatable(tween(520), RepeatMode.Reverse),
      label = "wave_1"
    ),
    transition.animateFloat(
      12f, 30f,
      androidx.compose.animation.core.infiniteRepeatable(tween(680), RepeatMode.Reverse),
      label = "wave_2"
    ),
    transition.animateFloat(
      8f, 25f,
      androidx.compose.animation.core.infiniteRepeatable(tween(590), RepeatMode.Reverse),
      label = "wave_3"
    ),
    transition.animateFloat(
      5f, 18f,
      androidx.compose.animation.core.infiniteRepeatable(tween(740), RepeatMode.Reverse),
      label = "wave_4"
    ),
    transition.animateFloat(
      10f, 27f,
      androidx.compose.animation.core.infiniteRepeatable(tween(610), RepeatMode.Reverse),
      label = "wave_5"
    )
  )
  Row(
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .clip(RoundedCornerShape(18.dp))
      .background(CallGreen.copy(alpha = 0.10f))
      .padding(horizontal = 12.dp, vertical = 8.dp)
  ) {
    heights.forEachIndexed { index, height ->
      val value by height
      Box(
        Modifier
          .width(3.dp)
          .height(value.dp)
          .clip(RoundedCornerShape(3.dp))
          .background(CallGreen.copy(alpha = if (index == 2) 1f else 0.72f))
      )
    }
    Spacer(Modifier.width(5.dp))
    Text(
      "Connected",
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
      color = CallGreen
    )
  }
}
