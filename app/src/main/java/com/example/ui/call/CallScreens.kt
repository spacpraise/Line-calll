package com.example.ui.call

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
  val infiniteTransition = rememberInfiniteTransition(label = "ring_pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.15f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_scale"
  )

  Surface(
    modifier = modifier.fillMaxSize(),
    color = Color(0xFF090909) // High-contrast telecommunications near-black
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)
        .padding(horizontal = 32.dp, vertical = 40.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Top Status Pill
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1D1D1D))
            .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(CallGreen)
          )
          Text(
            text = "Line Call Internet Audio",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "INCOMING CALL",
          style = MaterialTheme.typography.labelMedium.copy(
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold
          ),
          color = Color(0xFFAAAAAA)
        )
      }

      // Middle: Caller Identity with pulsing ring effect
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.size(160.dp)
        ) {
          // Pulsing decorative wave
          Box(
            modifier = Modifier
              .size(140.dp)
              .scale(pulseScale)
              .clip(CircleShape)
              .background(CallGreen.copy(alpha = 0.12f))
          )

          // Avatar Box
          val initials = (session.contactName?.take(2)
            ?: session.phoneNumber.replace("+", "").take(2)).uppercase()

          Box(
            modifier = Modifier
              .size(108.dp)
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
                fontSize = 36.sp
              ),
              color = Color.White
            )
          }
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
      }

      // Bottom Action Buttons: Decline (Red) and Accept (Green)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Decline Button
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Surface(
            onClick = onDecline,
            shape = CircleShape,
            color = CallRed,
            modifier = Modifier
              .size(72.dp)
              .testTag("decline_call_button")
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.CallEnd,
                contentDescription = "Decline Call",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Decline",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFFAAAAAA)
          )
        }

        // Accept Button
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Surface(
            onClick = onAccept,
            shape = CircleShape,
            color = CallGreen,
            modifier = Modifier
              .size(72.dp)
              .testTag("accept_call_button")
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = "Accept Call",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Accept",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White
          )
        }
      }
    }
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
            imageVector = Icons.Outlined.Lock,
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
                  imageVector = if (session.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
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
                  imageVector = if (session.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
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
              imageVector = Icons.Default.CallEnd,
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
