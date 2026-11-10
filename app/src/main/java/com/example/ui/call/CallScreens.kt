package com.example.ui.call

import android.view.HapticFeedbackConstants

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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActiveCallSession
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

  Surface(modifier = modifier.fillMaxSize(), color = Color(0xFF080A09)) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(Color(0xFF142019), Color(0xFF090C0A), Color(0xFF070807))
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
              .background(Color.White.copy(alpha = 0.07f))
              .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
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
              color = Color.White
            )
          }
          Text(
            "INCOMING VOICE CALL",
            style = MaterialTheme.typography.labelMedium.copy(
              letterSpacing = 2.2.sp,
              fontWeight = FontWeight.SemiBold
            ),
            color = Color.White.copy(alpha = 0.58f)
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
                      colors = listOf(Color(0xFF344B3C), Color(0xFF18241C))
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
                  color = Color.White
                )
              }
            }

            Text(
              callerName,
              style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 29.sp
              ),
              color = Color.White,
              textAlign = TextAlign.Center,
              maxLines = 2
            )
            Text(
              callerNumber,
              style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
              ),
              color = Color.White.copy(alpha = 0.62f),
              textAlign = TextAlign.Center
            )
            Text(
              "is calling you",
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White.copy(alpha = 0.48f)
            )
          }
        }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(18.dp),
          modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
        ) {
          IncomingCallSlider(
            onAnswer = {
              view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
              onAccept()
            },
            onDecline = {
              view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
              onDecline()
            }
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
          ) {
            IncomingCallAction(
              label = "Decline",
              icon = Icons.Rounded.CallEnd,
              color = CallRed,
              tag = "decline_call_button",
              onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onDecline()
              }
            )
            IncomingCallAction(
              label = "Answer",
              icon = Icons.Rounded.Phone,
              color = CallGreen,
              tag = "accept_call_button",
              onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onAccept()
              }
            )
          }
          Text(
            "Slide to answer or decline, or use the buttons",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.42f),
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}

@Composable
private fun IncomingCallSlider(
  onAnswer: () -> Unit,
  onDecline: () -> Unit
) {
  val drag = remember { Animatable(0f) }
  val scope = rememberCoroutineScope()
  val density = LocalDensity.current

  BoxWithConstraints(
    modifier = Modifier.fillMaxWidth().height(76.dp),
    contentAlignment = Alignment.Center
  ) {
    val maxDrag = with(density) { (maxWidth - 76.dp).toPx().coerceAtLeast(0f) / 2f }
    val threshold = maxDrag * 0.72f

    Row(
      modifier = Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(40.dp))
        .background(Color.White.copy(alpha = 0.075f))
        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(40.dp)),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier.weight(1f).fillMaxSize()
          .background(CallRed.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          "←  DECLINE",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
          ),
          color = Color.White.copy(alpha = 0.78f)
        )
      }
      Box(
        modifier = Modifier.weight(1f).fillMaxSize()
          .background(CallGreen.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          "ANSWER  →",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
          ),
          color = Color.White.copy(alpha = 0.78f)
        )
      }
    }

    Box(
      modifier = Modifier
        .offset { IntOffset(drag.value.roundToInt(), 0) }
        .size(64.dp)
        .clip(CircleShape)
        .background(
          Brush.linearGradient(
            colors = listOf(Color(0xFF344B3C), CallGreen)
          )
        )
        .border(1.dp, Color.White.copy(alpha = 0.32f), CircleShape)
        .pointerInput(maxDrag, threshold) {
          detectDragGestures(
            onDrag = { change, amount ->
              change.consume()
              scope.launch {
                drag.snapTo((drag.value + amount.x).coerceIn(-maxDrag, maxDrag))
              }
            },
            onDragEnd = {
              val position = drag.value
              when {
                position >= threshold -> {
                  onAnswer()
                }
                position <= -threshold -> {
                  onDecline()
                }
                else -> scope.launch {
                  drag.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f)
                  )
                }
              }
            },
            onDragCancel = {
              scope.launch {
                drag.animateTo(
                  targetValue = 0f,
                  animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f)
                )
              }
            }
          )
        },
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Rounded.Phone,
        contentDescription = "Slide right to answer or left to decline",
        tint = Color.White,
        modifier = Modifier.size(27.dp)
      )
    }
  }
}

@Composable
private fun IncomingCallAction(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  color: Color,
  tag: String,
  onClick: () -> Unit
) {
  val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val scale by androidx.compose.animation.core.animateFloatAsState(
    targetValue = if (pressed) 0.91f else 1f,
    animationSpec = androidx.compose.animation.core.spring(
      dampingRatio = 0.58f,
      stiffness = 520f
    ),
    label = "incoming_action_press"
  )
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Surface(
      onClick = onClick,
      interactionSource = interactionSource,
      shape = CircleShape,
      color = color,
      shadowElevation = 12.dp,
      modifier = Modifier.size(76.dp).graphicsLayer {
        scaleX = scale
        scaleY = scale
      }.testTag(tag)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = Color.White,
          modifier = Modifier.size(32.dp)
        )
      }
    }
    Spacer(Modifier.height(10.dp))
    Text(
      label,
      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
      color = Color.White.copy(alpha = 0.9f)
    )
  }
}

@Composable
@Composable
fun ActiveCallScreen(
  session: ActiveCallSession,
  onMuteToggle: () -> Unit,
  onSpeakerToggle: () -> Unit,
  onEndCall: () -> Unit,
  modifier: Modifier = Modifier
) {
  val view = LocalView.current
  val minutes = session.durationSeconds / 60
  val seconds = session.durationSeconds % 60
  val timerString = String.format("%02d:%02d", minutes, seconds)
  val stateText = when (session.state) {
    CallSessionState.DIALING -> "Connecting securely..."
    CallSessionState.RINGING -> "Ringing..."
    CallSessionState.CONNECTED -> timerString
    CallSessionState.ENDED -> "Call ended"
    CallSessionState.IDLE -> ""
  }
  val connected = session.state == CallSessionState.CONNECTED
  val transition = androidx.compose.animation.core.updateTransition(
    targetState = connected,
    label = "active_call_connection_state"
  )
  val avatarScale by transition.animateFloat(
    transitionSpec = { spring(dampingRatio = 0.72f, stiffness = 260f) },
    label = "active_call_avatar_scale"
  ) { if (it) 1f else 0.92f }

  Surface(modifier = modifier.fillMaxSize(), color = Color(0xFF090D0A)) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(Color(0xFF142019), Color(0xFF090D0A), Color(0xFF070807))
          )
        )
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.statusBars)
          .padding(horizontal = 28.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier
              .clip(RoundedCornerShape(24.dp))
              .background(Color.White.copy(alpha = 0.07f))
              .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(24.dp))
              .padding(horizontal = 13.dp, vertical = 8.dp)
          ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(CallGreen))
            Text(
              "LINE CALL  ·  PRIVATE VOICE",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
              ),
              color = Color.White.copy(alpha = 0.86f)
            )
          }
          androidx.compose.animation.AnimatedContent(
            targetState = stateText,
            transitionSpec = {
              (androidx.compose.animation.fadeIn(tween(220)) +
                androidx.compose.animation.slideInVertically(
                  animationSpec = tween(260),
                  initialOffsetY = { it / 3 }
                )) togetherWith
                androidx.compose.animation.fadeOut(tween(140))
            },
            label = "call_status_text"
          ) { text ->
            Text(
              text = text,
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = if (connected) 1.4.sp else 0.3.sp
              ),
              color = if (connected) Color.White else Color.White.copy(alpha = 0.66f),
              modifier = Modifier.testTag("call_duration_timer")
            )
          }
        }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
          Box(
            modifier = Modifier.size(196.dp),
            contentAlignment = Alignment.Center
          ) {
            if (connected) {
              val pulse = androidx.compose.animation.core.rememberInfiniteTransition(
                label = "connected_avatar_pulse"
              )
              val ringScale by pulse.animateFloat(
                initialValue = 0.96f,
                targetValue = 1.10f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                  animation = tween(1800, easing = FastOutSlowInEasing),
                  repeatMode = RepeatMode.Reverse
                ),
                label = "connected_avatar_ring"
              )
              Box(
                Modifier.size(168.dp).scale(ringScale)
                  .border(1.dp, CallGreen.copy(alpha = 0.20f), CircleShape)
              )
              Box(
                Modifier.size(144.dp).border(
                  1.dp, CallGreen.copy(alpha = 0.12f), CircleShape
                )
              )
            }
            Box(
              modifier = Modifier
                .size(122.dp)
                .scale(avatarScale)
                .clip(CircleShape)
                .background(
                  Brush.linearGradient(
                    listOf(Color(0xFF344B3C), Color(0xFF18241C))
                  )
                )
                .border(2.dp, CallGreen.copy(alpha = if (connected) 0.62f else 0.30f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              val initials = (session.contactName?.trim()?.split(Regex("\\s+"))
                ?.take(2)?.mapNotNull { it.firstOrNull()?.toString() }?.joinToString("")
                ?.takeIf { it.isNotBlank() }
                ?: session.phoneNumber.filter { it.isLetterOrDigit() }.take(2).ifBlank { "LC" })
                .uppercase()
              Text(
                initials,
                style = MaterialTheme.typography.headlineLarge.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 39.sp
                ),
                color = Color.White
              )
            }
          }

          Text(
            text = session.contactName?.takeIf { it.isNotBlank() } ?: "Line Call user",
            style = MaterialTheme.typography.headlineMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 28.sp
            ),
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2
          )
          Text(
            text = session.phoneNumber,
            style = MaterialTheme.typography.bodyLarge.copy(
              color = Color.White.copy(alpha = 0.60f),
              fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
          )
          androidx.compose.animation.AnimatedVisibility(
            visible = connected,
            enter = androidx.compose.animation.fadeIn(tween(300)) +
              androidx.compose.animation.expandVertically(tween(320)),
            exit = androidx.compose.animation.fadeOut(tween(120)) +
              androidx.compose.animation.shrinkVertically(tween(180))
          ) {
            ActiveCallWaveform()
          }
        }

        Column(
          modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
          ) {
            ActiveCallControl(
              label = if (session.isMuted) "Unmute" else "Mute",
              icon = if (session.isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
              selected = session.isMuted,
              testTag = "toggle_mute_button",
              onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onMuteToggle()
              }
            )
            ActiveCallControl(
              label = if (session.isSpeakerOn) "Speaker" else "Earpiece",
              icon = if (session.isSpeakerOn) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeDown,
              selected = session.isSpeakerOn,
              testTag = "toggle_speaker_button",
              onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onSpeakerToggle()
              }
            )
          }

          val endInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
          val endPressed by endInteraction.collectIsPressedAsState()
          val endScale by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (endPressed) 0.90f else 1f,
            animationSpec = spring(dampingRatio = 0.58f, stiffness = 520f),
            label = "end_call_press_scale"
          )
          Surface(
            onClick = {
              view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
              onEndCall()
            },
            interactionSource = endInteraction,
            shape = CircleShape,
            color = CallRed,
            shadowElevation = 12.dp,
            modifier = Modifier.size(76.dp).graphicsLayer {
              scaleX = endScale
              scaleY = endScale
            }.testTag("end_call_button")
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.CallEnd,
                contentDescription = "End call",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
              )
            }
          }
          Text(
            "End call",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White.copy(alpha = 0.78f)
          )
        }
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
      color = if (selected) CallGreen else Color.White.copy(alpha = 0.09f),
      modifier = Modifier.size(66.dp).graphicsLayer {
        scaleX = scale
        scaleY = scale
      }.testTag(testTag)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = if (selected) Color(0xFF071009) else Color.White,
          modifier = Modifier.size(27.dp)
        )
      }
    }
    Text(
      label,
      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
      color = Color.White.copy(alpha = 0.78f)
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
