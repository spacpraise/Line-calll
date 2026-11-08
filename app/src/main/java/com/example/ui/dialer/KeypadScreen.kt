package com.example.ui.dialer

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Country
import com.example.ui.theme.CallGreen
import com.example.ui.theme.OneUiDarkBackground
import com.example.ui.theme.OneUiTextPrimary
import com.example.ui.theme.OneUiTextSecondary

data class KeypadKey(
  val main: String,
  val sub: String = ""
)

private val keypadRows = listOf(
  listOf(KeypadKey("1", "➿"), KeypadKey("2", "ABC"), KeypadKey("3", "DEF")),
  listOf(KeypadKey("4", "GHI"), KeypadKey("5", "JKL"), KeypadKey("6", "MNO")),
  listOf(KeypadKey("7", "PQRS"), KeypadKey("8", "TUV"), KeypadKey("9", "WXYZ")),
  listOf(KeypadKey("*", ""), KeypadKey("0", "+"), KeypadKey("#", ""))
)

/**
 * Keypad screen precisely following Samsung One UI Phone Dialer style (Image 2):
 * - Pitch black background
 * - Large clean typography for entered number
 * - Borderless clean numeric keypad with subtle letter subtitles
 * - Prominent circular green call button below
 */
@Composable
fun KeypadScreen(
  enteredNumber: String,
  selectedCountry: Country,
  onAppendChar: (Char) -> Unit,
  onBackspace: () -> Unit,
  onClear: () -> Unit,
  onCall: () -> Unit,
  onSaveContact: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val view = LocalView.current

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 24.dp, vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    // Phone Number Display Area
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f, fill = false)
        .padding(top = 18.dp, bottom = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      if (enteredNumber.isEmpty()) {
        Spacer(modifier = Modifier.height(36.dp))
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Text(
            text = enteredNumber,
            style = MaterialTheme.typography.headlineLarge.copy(
              fontWeight = FontWeight.Normal,
              fontSize = 32.sp,
              letterSpacing = 1.sp
            ),
            color = OneUiTextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("dialer_number_display")
          )

          Spacer(modifier = Modifier.width(12.dp))

          IconButton(
            onClick = {
              view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
              onBackspace()
            },
            modifier = Modifier
              .size(36.dp)
              .testTag("keypad_backspace")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Backspace,
              contentDescription = "Backspace",
              tint = OneUiTextSecondary,
              modifier = Modifier.size(22.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // "Add to contacts" link under the number
        Surface(
          onClick = { onSaveContact(enteredNumber) },
          shape = RoundedCornerShape(16.dp),
          color = Color(0xFF1E2026),
          modifier = Modifier.testTag("add_to_contacts_quick_button")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = null,
              tint = CallGreen,
              modifier = Modifier.size(14.dp)
            )
            Text(
              text = "Add to contacts",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
              color = Color.White
            )
          }
        }
      }
    }

    // Keypad 3x4 Grid (Borderless, clean typography matching Image 2)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      keypadRows.forEach { row ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          row.forEach { key ->
            OneUiKeypadButton(
              key = key,
              onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onAppendChar(key.main[0])
              },
              onLongClick = {
                if (key.main == "0") {
                  view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                  onAppendChar('+')
                }
              }
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Call Action Button (Vibrant Green Circle, matching Image 2)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 76.dp), // Leaves room for floating bottom capsule nav
      contentAlignment = Alignment.Center
    ) {
      Surface(
        onClick = {
          view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
          onCall()
        },
        shape = CircleShape,
        color = CallGreen,
        shadowElevation = 4.dp,
        modifier = Modifier
          .size(68.dp)
          .testTag("call_action_button")
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.Phone,
            contentDescription = "Call",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OneUiKeypadButton(
  key: KeypadKey,
  onClick: () -> Unit,
  onLongClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .size(width = 86.dp, height = 64.dp)
      .clip(CircleShape)
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick
      )
      .testTag("keypad_btn_${key.main}"),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        text = key.main,
        style = MaterialTheme.typography.headlineMedium.copy(
          fontWeight = FontWeight.Normal,
          fontSize = if (key.main in listOf("*", "#")) 32.sp else 30.sp
        ),
        color = OneUiTextPrimary
      )
      if (key.sub.isNotEmpty()) {
        Text(
          text = key.sub,
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
            fontSize = 10.sp
          ),
          color = OneUiTextSecondary
        )
      } else {
        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }
}
