package com.example.ui.dialer

import android.view.HapticFeedbackConstants
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Phone
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
import com.example.data.model.Country
import com.example.ui.theme.CallGreen
import com.example.ui.theme.OneUiTextPrimary
import com.example.ui.theme.OneUiTextSecondary

@Composable
fun KeypadScreen(
  enteredNumber: String,
  selectedCountry: Country,
  callHistory: List<CallRecord>,
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
  val suggestions = remember(enteredNumber, callHistory) {
    val query = enteredNumber.filter { it.isDigit() }
    if (query.length < 2) emptyList()
    else callHistory.filter { it.phoneNumber.filter { ch -> ch.isDigit() }.contains(query) }
      .distinctBy { it.phoneNumber }.take(4)
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
    modifier = modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().weight(1f, fill = false).padding(top = 14.dp, bottom = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
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
          fontWeight = FontWeight.Normal, fontSize = 30.sp, letterSpacing = 1.sp,
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
      if (enteredNumber.isNotBlank()) {
        Surface(
          onClick = { onSaveContact(enteredNumber) },
          shape = RoundedCornerShape(16.dp),
          color = Color(0xFF1E2026),
          modifier = Modifier.padding(top = 8.dp)
        ) {
          Text("＋  Add to contacts", color = Color.White, fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        }
      }
    }

    if (suggestions.isNotEmpty()) {
      Column(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("RECENT MATCHES", color = OneUiTextSecondary, fontSize = 10.sp,
          letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold)
        suggestions.forEach { record ->
          Surface(
            onClick = { onSelectSuggestion(record) },
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1C1E23),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(record.contactName ?: record.phoneNumber, color = Color.White,
                  fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!record.contactName.isNullOrBlank()) {
                  Text(record.phoneNumber, color = OneUiTextSecondary, fontSize = 12.sp)
                }
              }
              Text("Call", color = CallGreen, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
          }
        }
      }
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9")).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          row.forEach { digit -> DialDigit(digit, onClick = { insertDigit(digit) }) }
        }
      }
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Surface(
          onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            val start = fieldValue.selection.min.coerceIn(0, fieldValue.text.length)
            val end = fieldValue.selection.max.coerceIn(0, fieldValue.text.length)
            val from = if (start == end && start > 0) start - 1 else start
            val updated = fieldValue.text.removeRange(from, end)
            fieldValue = TextFieldValue(updated, TextRange(from))
            onNumberChanged(updated)
          },
          shape = CircleShape, color = Color(0xFF202228),
          modifier = Modifier.size(64.dp).testTag("keypad_backspace")
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Delete digit",
              tint = OneUiTextPrimary, modifier = Modifier.size(25.dp))
          }
        }
        DialDigit("0", sub = "+", onClick = { insertDigit("0") }, onLongClick = { insertDigit("+") })
        Surface(
          onClick = { view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); onCall() },
          shape = CircleShape, color = CallGreen, shadowElevation = 4.dp,
          modifier = Modifier.size(64.dp).testTag("call_action_button")
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Phone, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(30.dp))
          }
        }
      }
    }
    Spacer(Modifier.height(76.dp))
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
  Box(
    Modifier.size(width = 86.dp, height = 58.dp).clip(CircleShape)
      .combinedClickable(
        onClick = { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onClick() },
        onLongClick = onLongClick
      ).testTag("keypad_btn_$digit"),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(digit, color = OneUiTextPrimary, fontSize = 29.sp, fontWeight = FontWeight.Normal)
      if (sub.isNotEmpty()) Text(sub, color = OneUiTextSecondary, fontSize = 9.sp, letterSpacing = 1.sp)
    }
  }
}
