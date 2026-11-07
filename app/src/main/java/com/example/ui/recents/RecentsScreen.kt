package com.example.ui.recents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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

@Composable
fun RecentsScreen(
  callHistory: List<CallRecord>,
  onCallRecord: (CallRecord) -> Unit,
  onClearHistory: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp, vertical = 8.dp)
  ) {
    // Header Row with Title and Clear button
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Recents",
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
      )

      if (callHistory.isNotEmpty()) {
        TextButton(
          onClick = onClearHistory,
          modifier = Modifier.testTag("clear_recents_button")
        ) {
          Text(
            text = "Clear",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.secondary
          )
        }
      }
    }

    if (callHistory.isEmpty()) {
      // Empty State
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(36.dp)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "No Recent Calls",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Voice calls you make and receive over Line Call will appear here.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.secondary,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 32.dp)
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        items(callHistory, key = { it.id }) { record ->
          CallRecordItem(
            record = record,
            onClick = { onCallRecord(record) }
          )
          HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
            thickness = 0.5.dp,
            modifier = Modifier.padding(start = 58.dp)
          )
        }
      }
    }
  }
}

@Composable
fun CallRecordItem(
  record: CallRecord,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable { onClick() }
      .padding(vertical = 12.dp, horizontal = 4.dp)
      .testTag("call_record_${record.id}"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Avatar Initials
      val initials = (record.contactName?.take(2)
        ?: record.phoneNumber.replace("+", "").take(2)).uppercase()

      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = initials,
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Column {
        val displayName = record.contactName ?: record.phoneNumber
        Text(
          text = displayName,
          style = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.SemiBold,
            color = if (record.type == CallType.MISSED) CallRed else MaterialTheme.colorScheme.onBackground
          )
        )

        Spacer(modifier = Modifier.height(2.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          when (record.type) {
            CallType.OUTGOING -> {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.CallMade,
                contentDescription = "Outgoing Call",
                tint = CallGreen,
                modifier = Modifier.size(14.dp)
              )
              Text(
                text = "Outgoing • ${record.formattedDuration}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
              )
            }
            CallType.INCOMING -> {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.CallReceived,
                contentDescription = "Incoming Call",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(14.dp)
              )
              Text(
                text = "Incoming • ${record.formattedDuration}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
              )
            }
            CallType.MISSED -> {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.CallMissed,
                contentDescription = "Missed Call",
                tint = CallRed,
                modifier = Modifier.size(14.dp)
              )
              Text(
                text = "Missed Call",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = CallRed
              )
            }
          }
        }
      }
    }

    // Right Column: Date and Call icon
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Text(
        text = record.formattedTime,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary
      )

      IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Phone,
          contentDescription = "Call again",
          tint = CallGreen,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}
