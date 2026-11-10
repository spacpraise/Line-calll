package com.example.ui.dialer

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallRecord
import com.example.data.model.Contact
import com.example.data.model.Country
import com.example.ui.theme.CallGreen
import com.example.ui.theme.OneUiTextPrimary
import com.example.ui.theme.OneUiTextSecondary

@Composable
fun KeypadScreen(
  enteredNumber: String,
  selectedCountry: Country,
  callHistory: List<CallRecord>,
  contacts: List<Contact>,
  onNumberChanged: (String) -> Unit,
  onCall: () -> Unit,
  onSelectSuggestion: (CallRecord) -> Unit,
  onSaveContact: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val view = LocalView.current
  var fieldValue by remember { mutableStateOf(TextFieldValue(enteredNumber, TextRange(enteredNumber.length))) }
  LaunchedEffect(enteredNumber) {
    if (enteredNumber != fieldValue.text) {
      fieldValue = TextFieldValue(enteredNumber, TextRange(enteredNumber.length))
    }
  }
  val queryDigits = enteredNumber.filter { it.isDigit() }
  val matchingContacts = remember(queryDigits, contacts) {
    if (queryDigits.isEmpty()) emptyList() else contacts.filter { it.phoneNumber.filter(Char::isDigit).contains(queryDigits) }.take(8)
  }
  val matchingHistory = remember(queryDigits, callHistory, matchingContacts) {
    if (queryDigits.isEmpty()) emptyList() else callHistory.filter { record ->
      record.phoneNumber.filter(Char::isDigit).contains(queryDigits) && matchingContacts.none { it.phoneNumber == record.phoneNumber }
    }.distinctBy { it.phoneNumber }.take(8)
  }
  fun insertDigit(value: String) {
    val start = fieldValue.selection.min.coerceIn(0, fieldValue.text.length)
    val end = fieldValue.selection.max.coerceIn(0, fieldValue.text.length)
    val updated = fieldValue.text.replaceRange(start, end, value)
    val cursor = start + value.length
    fieldValue = TextFieldValue(updated, TextRange(cursor))
    onNumberChanged(updated)
  }

  Column(
    modifier = modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // The number display owns the flexible space. The keypad below has fixed
    // dimensions, so entering digits never pushes or resizes the keys.
    Box(
      modifier = Modifier.fillMaxWidth().weight(1f),
      contentAlignment = Alignment.Center
    ) {
      BasicTextField(
        value = fieldValue,
        onValueChange = {
          fieldValue = it
          onNumberChanged(it.text)
        },
        readOnly = true,
        singleLine = true,
        cursorBrush = SolidColor(CallGreen),
        textStyle = MaterialTheme.typography.headlineLarge.copy(
          fontWeight = FontWeight.Medium, fontSize = 30.sp, letterSpacing = 1.sp,
          color = OneUiTextPrimary, textAlign = TextAlign.Center
        ),
        modifier = Modifier.fillMaxWidth().testTag("dialer_number_display"),
        decorationBox = { inner ->
          Box(contentAlignment = Alignment.Center) {
            if (fieldValue.text.isEmpty()) {
              Text("Enter phone number", color = OneUiTextSecondary, fontSize = 18.sp)
            }
            inner()
          }
        }
      )
    }

    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9")).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          row.forEach { digit -> DialDigit(digit, onClick = { insertDigit(digit) }) }
        }
      }
      Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Spacer(Modifier.size(64.dp))
        DialDigit("0", sub = "+", onClick = { insertDigit("0") }, onLongClick = { insertDigit("+") })
        Surface(
          onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            if (fieldValue.text.isNotEmpty()) {
              val startIndex = fieldValue.selection.min.coerceIn(0, fieldValue.text.length)
              val endIndex = fieldValue.selection.max.coerceIn(0, fieldValue.text.length)
              val from = if (startIndex == endIndex && startIndex > 0) startIndex - 1 else startIndex
              val updated = fieldValue.text.removeRange(from, endIndex)
              fieldValue = TextFieldValue(updated, TextRange(from))
              onNumberChanged(updated)
            }
          },
          shape = CircleShape,
          color = Color.Transparent,
          modifier = Modifier.size(64.dp).testTag("keypad_backspace")
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              Icons.Rounded.Close,
              contentDescription = "Clear last digit",
              tint = OneUiTextPrimary,
              modifier = Modifier.size(27.dp)
            )
          }
        }
      }

      Surface(
        onClick = { view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); onCall() },
        shape = CircleShape,
        color = CallGreen,
        shadowElevation = 5.dp,
        modifier = Modifier.padding(top = 2.dp).size(82.dp).testTag("call_action_button")
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(Icons.Rounded.Phone, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(36.dp))
        }
      }
    }
    Spacer(Modifier.height(18.dp))
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialDigit(
  digit: String,
  sub: String = when (digit) {
    "2" -> "ABC"; "3" -> "DEF"; "4" -> "GHI"; "5" -> "JKL"
    "6" -> "MNO"; "7" -> "PQRS"; "8" -> "TUV"; "9" -> "WXYZ"; else -> ""
  },
  onClick: () -> Unit,
  onLongClick: (() -> Unit)? = null
) {
  val view = LocalView.current
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (pressed) 0.90f else 1f,
    animationSpec = spring(dampingRatio = 0.55f, stiffness = 600f),
    label = "digit_press"
  )
  Box(
    Modifier.size(width = 86.dp, height = 68.dp).graphicsLayer { scaleX = scale; scaleY = scale }.clip(CircleShape)
      .combinedClickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onClick() },
        onLongClick = onLongClick
      ).testTag("keypad_btn_$digit"),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(digit, color = OneUiTextPrimary, fontSize = 34.sp, fontWeight = FontWeight.Medium)
      if (sub.isNotEmpty()) Text(sub, color = OneUiTextSecondary, fontSize = 10.sp, letterSpacing = 1.sp)
    }
  }
}
