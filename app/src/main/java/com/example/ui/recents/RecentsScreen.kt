package com.example.ui.recents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallRecord
import com.example.data.model.CallType
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CallRed
import com.example.ui.theme.OneUiCardBackground
import com.example.ui.theme.OneUiCardElevated
import com.example.ui.theme.OneUiTextMuted
import com.example.ui.theme.OneUiTextPrimary
import com.example.ui.theme.OneUiTextSecondary

/**
 * Recents screen matching Samsung One UI Phone Recents (Images 5 & 6):
 * - Missed calls banner card: "2 missed calls from Sunny Wife. [Call back]" with close 'X'
 * - "Today" section header
 * - Unified dark card container with directional arrow icons (green outgoing/incoming, red missed)
 * - Right-aligned time
 * - Filter bottom sheet supporting filters from Image 5 (Call time, Call type)
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

  // Find missed calls for banner
  val missedCalls = remember(callHistory) {
    callHistory.filter { it.type == CallType.MISSED }
  }

  val filteredHistory = remember(callHistory, filterType) {
    if (filterType == null) callHistory
    else callHistory.filter { it.type == filterType }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(bottom = 96.dp)
  ) {
    // 1. Missed Calls Top Banner (Image 6)
    if (!missedCallBannerDismissed && missedCalls.isNotEmpty()) {
      val latestMissed = missedCalls.first()
      val caller = latestMissed.contactName ?: latestMissed.phoneNumber
      val missedCount = missedCalls.count { (it.contactName ?: it.phoneNumber) == caller }

      item {
        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = OneUiCardBackground),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("missed_call_banner_card")
        ) {
          Box(modifier = Modifier.padding(16.dp)) {
            IconButton(
              onClick = onDismissMissedBanner,
              modifier = Modifier
                .size(24.dp)
                .align(Alignment.TopEnd)
            ) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Dismiss",
                tint = OneUiTextSecondary,
                modifier = Modifier.size(16.dp)
              )
            }

            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.fillMaxWidth()
            ) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "$missedCount missed calls from $caller.",
                style = MaterialTheme.typography.bodyLarge.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp
                ),
                color = OneUiTextPrimary,
                textAlign = TextAlign.Center
              )

              Spacer(modifier = Modifier.height(14.dp))

              Surface(
                onClick = { onCallRecord(latestMissed) },
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF26282E),
                modifier = Modifier.testTag("callback_banner_btn")
              ) {
                Text(
                  text = "Call back",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = OneUiTextPrimary,
                  modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                )
              }
            }
          }
        }
      }
    }

    // Active filter pill indicator
    if (filterType != null) {
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = OneUiCardElevated
          ) {
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
                tint = OneUiTextSecondary,
                modifier = Modifier
                  .size(14.dp)
                  .clickable { onFilterSelected(null) }
              )
            }
          }

          TextButton(onClick = onClearHistory) {
            Text("Clear all", color = OneUiTextSecondary, fontSize = 12.sp)
          }
        }
      }
    }

    if (filteredHistory.isEmpty()) {
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier
              .size(68.dp)
              .clip(CircleShape)
              .background(OneUiCardBackground),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Rounded.History,
              contentDescription = null,
              tint = OneUiTextSecondary,
              modifier = Modifier.size(32.dp)
            )
          }
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = "No Recent Calls",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = OneUiTextPrimary
          )
        }
      }
    } else {
      // "Today" Section Header (Image 6)
      item {
        Text(
          text = "Today",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
          color = OneUiTextSecondary,
          modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 6.dp)
        )
      }

      // Keep each call record independently lazy; long histories remain responsive.
      items(
        items = filteredHistory,
        key = { "${it.phoneNumber}_${it.formattedTime}_${it.type.name}" }
      ) { record ->
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = OneUiCardBackground,
          modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
        ) {
          OneUiCallRecordRow(
            record = record,
            onClick = { onCallRecord(record) }
          )
        }
      }
    }
  }

  // Filter Bottom Sheet (Image 5)
  if (showFilterSheet) {
    ModalBottomSheet(
      onDismissRequest = { showFilterSheet = false },
      sheetState = sheetState,
      containerColor = Color(0xFF141519)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 10.dp)
      ) {
        Text(
          text = "Filters",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = OneUiTextPrimary
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "Call type",
          style = MaterialTheme.typography.labelSmall,
          color = OneUiTextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterPill(
            label = "All calls",
            isSelected = filterType == null,
            onClick = {
              onFilterSelected(null)
              showFilterSheet = false
            }
          )
          FilterPill(
            label = "Missed",
            isSelected = filterType == CallType.MISSED,
            onClick = {
              onFilterSelected(CallType.MISSED)
              showFilterSheet = false
            }
          )
          FilterPill(
            label = "Outgoing",
            isSelected = filterType == CallType.OUTGOING,
            onClick = {
              onFilterSelected(CallType.OUTGOING)
              showFilterSheet = false
            }
          )
          FilterPill(
            label = "Incoming",
            isSelected = filterType == CallType.INCOMING,
            onClick = {
              onFilterSelected(CallType.INCOMING)
              showFilterSheet = false
            }
          )
        }

        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

@Composable
fun OneUiCallRecordRow(
  record: CallRecord,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val view = LocalView.current
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val pressScale by animateFloatAsState(
    targetValue = if (pressed) 0.985f else 1f,
    animationSpec = spring(dampingRatio = 0.62f, stiffness = 560f),
    label = "recent_row_press"
  )

  Row(
    modifier = modifier
      .fillMaxWidth()
      .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
      .clickable(
        interactionSource = interaction,
        indication = null,
        onClick = {
          view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
          onClick()
        }
      )
      .padding(horizontal = 16.dp, vertical = 14.dp)
      .testTag("recent_row_${record.phoneNumber}"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.weight(1f)
    ) {
      // Directional arrow icon (green outgoing/incoming, red missed)
      when (record.type) {
        CallType.OUTGOING -> {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.CallMade,
            contentDescription = "Outgoing",
            tint = CallGreen,
            modifier = Modifier.size(16.dp)
          )
        }
        CallType.INCOMING -> {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.CallReceived,
            contentDescription = "Incoming",
            tint = OneUiTextSecondary,
            modifier = Modifier.size(16.dp)
          )
        }
        CallType.MISSED -> {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.CallMissed,
            contentDescription = "Missed",
            tint = CallRed,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      // Contact name and count (Image 6)
      val nameText = record.contactName ?: record.phoneNumber
      Text(
        text = nameText,
        style = MaterialTheme.typography.bodyLarge.copy(
          fontWeight = FontWeight.Medium,
          color = if (record.type == CallType.MISSED) CallRed else OneUiTextPrimary
        ),
        maxLines = 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
      )
    }

    // Right-aligned time (e.g. 10:01 pm)
    Text(
      text = record.formattedTime.substringAfterLast(", "),
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
      color = OneUiTextSecondary
    )
  }
}

@Composable
private fun FilterPill(
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(16.dp),
    color = if (isSelected) CallGreen else Color(0xFF26282E)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium.copy(
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
      ),
      color = if (isSelected) Color.White else OneUiTextPrimary,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
    )
  }
}
