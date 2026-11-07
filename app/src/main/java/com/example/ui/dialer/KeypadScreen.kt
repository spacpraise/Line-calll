package com.example.ui.dialer

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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

data class KeypadKey(
  val main: String,
  val sub: String = ""
)

private val keypadRows = listOf(
  listOf(KeypadKey("1", ""), KeypadKey("2", "A B C"), KeypadKey("3", "D E F")),
  listOf(KeypadKey("4", "G H I"), KeypadKey("5", "J K L"), KeypadKey("6", "M N O")),
  listOf(KeypadKey("7", "P Q R S"), KeypadKey("8", "T U V"), KeypadKey("9", "W X Y Z")),
  listOf(KeypadKey("*", ""), KeypadKey("0", "+"), KeypadKey("#", ""))
)

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
      .padding(horizontal = 24.dp, vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    // Phone Number Display Area
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f, fill = false)
        .padding(top = 16.dp, bottom = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      if (enteredNumber.isEmpty()) {
        Text(
          text = "Enter phone number",
          style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.5.sp
          ),
          color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
          textAlign = TextAlign.Center
        )
      } else {
        Text(
          text = enteredNumber,
          style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          ),
          color = MaterialTheme.colorScheme.onBackground,
          textAlign = TextAlign.Center,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("dialer_number_display")
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Quick add to contacts action when a number is typed
        Surface(
          onClick = { onSaveContact(enteredNumber) },
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
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
              text = "Add to Contacts",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    // Keypad Grid
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      keypadRows.forEach { row ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          row.forEach { key ->
            KeypadButton(
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

    Spacer(modifier = Modifier.height(16.dp))

    // Call Action Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Empty placeholder or clear button
      Box(
        modifier = Modifier.size(56.dp),
        contentAlignment = Alignment.Center
      ) {
        if (enteredNumber.length > 3) {
          Surface(
            onClick = {
              view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
              onClear()
            },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(44.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = "Clear",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.secondary
              )
            }
          }
        }
      }

      // Main Call Button: Prominent Green Circular Button
      Surface(
        onClick = {
          view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
          onCall()
        },
        shape = CircleShape,
        color = CallGreen,
        shadowElevation = 2.dp,
        modifier = Modifier
          .size(72.dp)
          .testTag("call_action_button")
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.Phone,
            contentDescription = "Call",
            tint = Color.White,
            modifier = Modifier.size(34.dp)
          )
        }
      }

      // Backspace Button
      Box(
        modifier = Modifier.size(56.dp),
        contentAlignment = Alignment.Center
      ) {
        if (enteredNumber.isNotEmpty()) {
          IconButton(
            onClick = {
              view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
              onBackspace()
            },
            modifier = Modifier
              .size(52.dp)
              .testTag("keypad_backspace")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Backspace,
              contentDescription = "Backspace",
              tint = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.size(26.dp)
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadButton(
  key: KeypadKey,
  onClick: () -> Unit,
  onLongClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .size(72.dp)
      .clip(CircleShape)
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
      .border(
        width = 0.5.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
        shape = CircleShape
      )
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
          fontWeight = FontWeight.Medium,
          fontSize = if (key.main in listOf("*", "#")) 26.sp else 28.sp
        ),
        color = MaterialTheme.colorScheme.onBackground
      )
      if (key.sub.isNotEmpty()) {
        Text(
          text = key.sub,
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontSize = 9.sp
          ),
          color = MaterialTheme.colorScheme.secondary
        )
      }
    }
  }
}
