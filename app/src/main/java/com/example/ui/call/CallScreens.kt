package com.example.ui.call

import android.view.HapticFeedbackConstants

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
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
          verticalArrangement = Arrangement.spacedBy(22.dp),
          modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
        ) {
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
            "Answer to connect over the internet",
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
fun ActiveCallScreen(
  session: ActiveCallSession,
  onMuteToggle: () -> Unit,
  onSpeakerToggle: () -> Unit,
  onEndCall: () -> Unit,
  modifier: Modifier = Modifier
) {
  val minutes = session.durationSeconds / 60
  val seconds = session.durationSeconds % 60
  val timerString = String.format("%02d:%02d", minutes, seconds)

  val stateText = when (session.state) {
    CallSessionState.DIALING -> "Connecting..."
    CallSessionState.RINGING -> "Ringing..."
    CallSessionState.CONNECTED -> timerString
    CallSessionState.ENDED -> "Call Ended"
    CallSessionState.IDLE -> ""
  }

  Surface(
    modifier = modifier.fillMaxSize(),
    color = Color(0xFF090909)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)
        .padding(horizontal = 32.dp, vertical = 40.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Top Section: Encryption status & Call state
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1D1D1D))
            .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = CallGreen,
            modifier = Modifier.size(13.dp)
          )
          Text(
            text = "End-to-End Encrypted Voice",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = Color(0xFFD4D4D4)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = stateText,
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
          ),
          color = if (session.state == CallSessionState.CONNECTED) Color.White else Color(0xFFAAAAAA),
          modifier = Modifier.testTag("call_duration_timer")
        )
      }

      // Middle Section: Caller Identity
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        val initials = (session.contactName?.take(2)
          ?: session.phoneNumber.replace("+", "").take(2)).uppercase()

        Box(
          modifier = Modifier
            .size(112.dp)
            .clip(CircleShape)
            .background(
              Brush.linearGradient(
                colors = listOf(Color(0xFF1D1D1D), Color(0xFF111111))
              )
            )
            .border(2.dp, Color(0xFF333333), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = initials,
            style = MaterialTheme.typography.headlineLarge.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 38.sp
            ),
            color = Color.White
          )
        }

        Text(
          text = session.contactName ?: "Line Call User",
          style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Bold,
            color = Color.White
          ),
          textAlign = TextAlign.Center
        )

        Text(
          text = session.phoneNumber,
          style = MaterialTheme.typography.bodyLarge.copy(
            color = Color(0xFFAAAAAA),
            fontWeight = FontWeight.Medium
          )
        )

        // Subtle live audio wave badge
        if (session.state == CallSessionState.CONNECTED) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(CallGreen.copy(alpha = 0.12f))
              .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(CallGreen)
            )
            Text(
              text = "LiveKit WebRTC Audio",
              style = MaterialTheme.typography.labelSmall,
              color = CallGreen
            )
          }
        }
      }

      // Bottom Section: Audio Controls & Prominent Red End Call Button
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp),
        modifier = Modifier.padding(bottom = 20.dp)
      ) {
        // Toggles Row: Mute and Speaker
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Mute Button
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
              onClick = onMuteToggle,
              shape = CircleShape,
              color = if (session.isMuted) Color.White else Color(0xFF1D1D1D),
              modifier = Modifier
                .size(60.dp)
                .testTag("toggle_mute_button")
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = if (session.isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                  contentDescription = if (session.isMuted) "Unmute" else "Mute",
                  tint = if (session.isMuted) Color.Black else Color.White,
                  modifier = Modifier.size(28.dp)
                )
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = if (session.isMuted) "Unmute" else "Mute",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFFAAAAAA)
            )
          }

          // Speakerphone Button
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
              onClick = onSpeakerToggle,
              shape = CircleShape,
              color = if (session.isSpeakerOn) Color.White else Color(0xFF1D1D1D),
              modifier = Modifier
                .size(60.dp)
                .testTag("toggle_speaker_button")
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = if (session.isSpeakerOn) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeDown,
                  contentDescription = if (session.isSpeakerOn) "Speaker Off" else "Speaker On",
                  tint = if (session.isSpeakerOn) Color.Black else Color.White,
                  modifier = Modifier.size(28.dp)
                )
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = if (session.isSpeakerOn) "Speaker" else "Earpiece",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFFAAAAAA)
            )
          }
        }

        // Prominent Red End Call Button
        Surface(
          onClick = onEndCall,
          shape = CircleShape,
          color = CallRed,
          shadowElevation = 4.dp,
          modifier = Modifier
            .size(72.dp)
            .testTag("end_call_button")
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.CallEnd,
              contentDescription = "End Call",
              tint = Color.White,
              modifier = Modifier.size(34.dp)
            )
          }
        }
      }
    }
  }
}
