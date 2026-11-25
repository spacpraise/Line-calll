package com.example.ui.recents

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallRecord
import com.example.data.model.CallType
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CallRed
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Recents, redesigned around a WhatsApp/Telegram calling pattern rather than a
 * system phone-app call log: each row leads with the caller's identity (avatar,
 * name, number) instead of a direction arrow, consecutive calls from the same
 * number collapse into one row with a count badge, and swiping a row calls back
 * immediately instead of requiring a tap-in first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentsScreen(
  callHistory: List<CallRecord>,
  missedCallBannerDismissed: Boolean,
  filterType: CallType?,
  onDismissMissedBanner: () -> Unit,
  onFilterSelected: (CallType?) -> Unit,
  onCallRecord: (CallRecord) -> Unit,
  onClearHistory: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showFilterSheet by remember { mutableStateOf(false) }
  val sheetState = rememberModalBottomSheetState()

  val missedCalls = remember(callHistory) { callHistory.filter { it.type == CallType.MISSED } }

  val filteredHistory = remember(callHistory, filterType) {
    if (filterType == null) callHistory else callHistory.filter { it.type == filterType }
  }

  // Collapse consecutive calls from the same number into one group, newest first.
  // A different caller in between starts a new group, so history of A, B, A stays
  // three groups rather than merging the two A's across the gap.
  val groupedHistory = remember(filteredHistory) {
    val groups = mutableListOf<MutableList<CallRecord>>()
    for (record in filteredHistory) {
      val lastGroup = groups.lastOrNull()
      if (lastGroup != null && lastGroup.first().phoneNumber == record.phoneNumber) {
        lastGroup.add(record)
      } else {
        groups.add(mutableListOf(record))
      }
    }
    groups
  }

  LazyColumn(
    modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
    contentPadding = PaddingValues(bottom = 96.dp)
  ) {
    if (!missedCallBannerDismissed && missedCalls.isNotEmpty()) {
      val latestMissed = missedCalls.first()
      val caller = latestMissed.contactName ?: latestMissed.phoneNumber
      val missedCount = missedCalls.count { (it.contactName ?: it.phoneNumber) == caller }

      item {
        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("missed_call_banner_card")
        ) {
          Box(modifier = Modifier.padding(16.dp)) {
            IconButton(
              onClick = onDismissMissedBanner,
              modifier = Modifier.size(24.dp).align(Alignment.TopEnd)
            ) {
              Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "$missedCount missed calls from $caller",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(14.dp))
              Surface(
                onClick = { onCallRecord(latestMissed) },
                shape = RoundedCornerShape(18.dp),
                color = CallGreen,
                modifier = Modifier.testTag("callback_banner_btn")
              ) {
                Text(
                  text = "Call back",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = Color.White,
                  modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                )
              }
            }
          }
        }
      }
    }

    if (filterType != null) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = "Filter: ${filterType.name.lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.labelSmall,
                color = CallGreen
              )
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Clear filter",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp).clickable { onFilterSelected(null) }
              )
            }
          }
          TextButton(onClick = onClearHistory) {
            Text("Clear all", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
          }
        }
      }
    } else if (filteredHistory.isNotEmpty()) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = { showFilterSheet = true }) {
            Text("Filter", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
          }
        }
      }
    }

    if (filteredHistory.isEmpty()) {
      item {
        Column(
          modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier.size(68.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
          }
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = "No recent calls",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Calls you make or receive will show up here",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
          )
        }
      }
    } else {
      item {
        Text(
          text = "Recent",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 6.dp)
        )
      }

      items(items = groupedHistory, key = { it.first().id }) { group ->
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
        ) {
          RecentCallRow(group = group, onClick = { onCallRecord(group.first()) })
        }
      }
    }
  }

  if (showFilterSheet) {
    ModalBottomSheet(
      onDismissRequest = { showFilterSheet = false },
      sheetState = sheetState,
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text(
          text = "Filter calls",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterPill(label = "All", isSelected = filterType == null, onClick = { onFilterSelected(null); showFilterSheet = false })
          FilterPill(label = "Missed", isSelected = filterType == CallType.MISSED, onClick = { onFilterSelected(CallType.MISSED); showFilterSheet = false })
          FilterPill(label = "Outgoing", isSelected = filterType == CallType.OUTGOING, onClick = { onFilterSelected(CallType.OUTGOING); showFilterSheet = false })
          FilterPill(label = "Incoming", isSelected = filterType == CallType.INCOMING, onClick = { onFilterSelected(CallType.INCOMING); showFilterSheet = false })
        }
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

private val avatarColors = listOf(
  Color(0xFFFCA5A5), Color(0xFF86EFAC), Color(0xFF93C5FD), Color(0xFFFDE047), Color(0xFFF472B6)
)

@Composable
private fun RecentCallRow(group: List<CallRecord>, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val record = group.first()
  val view = LocalView.current
  val density = LocalDensity.current
  val scope = rememberCoroutineScope()
  val offsetX = remember { Animatable(0f) }
  val callThreshold = with(density) { 72.dp.toPx() }
  val maxDrag = with(density) { 96.dp.toPx() }
  val revealProgress = (offsetX.value / callThreshold).coerceIn(0f, 1f)

  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val pressScale by animateFloatAsState(
    targetValue = if (pressed) 0.985f else 1f,
    animationSpec = spring(dampingRatio = 0.62f, stiffness = 560f),
    label = "recent_row_press"
  )

  val initials = (record.contactName?.trim()?.takeIf { it.isNotBlank() }
    ?.split(Regex("\\s+"))?.take(2)?.mapNotNull { it.firstOrNull()?.toString() }?.joinToString("")
    ?: record.phoneNumber.filter { it.isLetterOrDigit() }.take(2)).ifBlank { "?" }.uppercase()
  val avatarColor = avatarColors[(record.phoneNumber.hashCode().mod(avatarColors.size)).let { if (it < 0) it + avatarColors.size else it }]

  Box(modifier = modifier.fillMaxWidth()) {
    // Call-back affordance revealed as the row is swiped right.
    Row(
      modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Rounded.Phone,
        contentDescription = "Call back",
        tint = CallGreen.copy(alpha = revealProgress),
        modifier = Modifier.size(22.dp).graphicsLayer { scaleX = 0.7f + 0.3f * revealProgress; scaleY = 0.7f + 0.3f * revealProgress }
      )
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
        .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
        .background(MaterialTheme.colorScheme.surface)
        .pointerInput(record.id) {
          detectHorizontalDragGestures(
            onDragEnd = {
              val completed = offsetX.value >= callThreshold
              scope.launch {
                offsetX.animateTo(0f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f))
              }
              if (completed) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                onClick()
              }
            },
            onDragCancel = {
              scope.launch { offsetX.animateTo(0f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)) }
            },
            onHorizontalDrag = { change, dragAmount ->
              change.consume()
              val next = (offsetX.value + dragAmount).coerceIn(0f, maxDrag)
              if (offsetX.value < callThreshold && next >= callThreshold) {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
              }
              scope.launch { offsetX.snapTo(next) }
            }
          )
        }
        .clickable(
          interactionSource = interaction,
          indication = null,
          onClick = { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK); onClick() }
        )
        .padding(horizontal = 14.dp, vertical = 11.dp)
        .testTag("recent_row_${record.phoneNumber}"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
      Box(
        modifier = Modifier.size(46.dp).clip(CircleShape).background(avatarColor),
        contentAlignment = Alignment.Center
      ) {
        Text(initials, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
      }

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = record.contactName ?: record.phoneNumber,
            style = MaterialTheme.typography.bodyLarge.copy(
              fontWeight = FontWeight.SemiBold,
              color = if (record.type == CallType.MISSED) CallRed else MaterialTheme.colorScheme.onSurface
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
          )
          if (group.size > 1) {
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
              Text(
                text = "×${group.size}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
              )
            }
          }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
          val (icon, tint) = when (record.type) {
            CallType.OUTGOING -> Icons.AutoMirrored.Rounded.CallMade to CallGreen
            CallType.INCOMING -> Icons.AutoMirrored.Rounded.CallReceived to MaterialTheme.colorScheme.onSurfaceVariant
            CallType.MISSED -> Icons.AutoMirrored.Rounded.CallMissed to CallRed
          }
          Icon(icon, contentDescription = record.type.name.lowercase(), tint = tint, modifier = Modifier.size(13.dp))
          Text(record.formattedTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }

      IconButton(onClick = { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK); onClick() }, modifier = Modifier.testTag("recent_row_call_${record.phoneNumber}")) {
        Icon(Icons.Rounded.Phone, contentDescription = "Call ${record.contactName ?: record.phoneNumber}", tint = CallGreen, modifier = Modifier.size(20.dp))
      }
    }
  }
}

@Composable
private fun FilterPill(label: String, isSelected: Boolean, onClick: () -> Unit) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(16.dp),
    color = if (isSelected) CallGreen else MaterialTheme.colorScheme.surfaceVariant
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
      color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
    )
  }
}
