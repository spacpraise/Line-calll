package com.example.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserAccount
import com.example.ui.BackendNoticeState
import com.example.ui.MainTab
import com.example.ui.theme.CallGreen
import com.example.ui.theme.NotificationOrange
import com.example.ui.theme.OneUiCardBackground
import com.example.ui.theme.OneUiNavActivePill
import com.example.ui.theme.OneUiNavPill
import com.example.ui.theme.OneUiTextMuted
import com.example.ui.theme.OneUiTextPrimary
import com.example.ui.theme.OneUiTextSecondary

/**
 * Top header matching Samsung One UI Phone / Contacts app:
 * - In Keypad: clean top bar with Search and 3-dots More menu (with orange dot)
 * - In Contacts: large "Phone" title + contact count subtitle, with +, Search, 3-dots More menu
 * - In Recents: Filter icon, Search, 3-dots More menu
 */
@Composable
fun OneUiTopBar(
  currentTab: MainTab,
  contactsCount: Int,
  currentUser: UserAccount?,
  onAddContactClick: () -> Unit,
  onSearchClick: () -> Unit,
  onFilterClick: () -> Unit,
  onSettingsClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showMenu by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .windowInsetsPadding(WindowInsets.statusBars)
      .padding(horizontal = 20.dp, vertical = 6.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (currentTab == MainTab.CONTACTS) {
        // Large "Phone" + Subtitle "XXXX contacts" as in Image 1
        Column {
          Text(
            text = "Phone",
            style = MaterialTheme.typography.headlineMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 28.sp
            ),
            color = OneUiTextPrimary
          )
          Text(
            text = "$contactsCount contacts",
            style = MaterialTheme.typography.bodySmall,
            color = OneUiTextSecondary
          )
        }
      } else {
        // Line Call Brand Pill or empty spacer
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(CallGreen)
          )
          Text(
            text = "Line Call",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = OneUiTextPrimary
          )
        }
      }

      // Top Action Icons
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        if (currentTab == MainTab.CONTACTS) {
          IconButton(
            onClick = onAddContactClick,
            modifier = Modifier
              .size(42.dp)
              .testTag("oneui_add_contact_top")
          ) {
            Icon(
              imageVector = Icons.Rounded.Add,
              contentDescription = "Add Contact",
              tint = OneUiTextPrimary,
              modifier = Modifier.size(24.dp)
            )
          }
        } else if (currentTab == MainTab.RECENTS) {
          IconButton(
            onClick = onFilterClick,
            modifier = Modifier.size(42.dp)
          ) {
            Icon(
              imageVector = Icons.Rounded.FilterList,
              contentDescription = "Filters",
              tint = OneUiTextPrimary,
              modifier = Modifier.size(22.dp)
            )
          }
        }

        IconButton(
          onClick = onSearchClick,
          modifier = Modifier
            .size(42.dp)
            .testTag("oneui_search_top")
        ) {
          Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = "Search",
            tint = OneUiTextPrimary,
            modifier = Modifier.size(22.dp)
          )
        }

        // More Menu with Orange Notification Dot (Image 1, 2, 4, 6)
        Box {
          IconButton(
            onClick = { showMenu = true },
            modifier = Modifier
              .size(42.dp)
              .testTag("oneui_more_menu_top")
          ) {
            Box {
              Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "More",
                tint = OneUiTextPrimary,
                modifier = Modifier.size(22.dp)
              )
              // Distinct orange indicator dot
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .clip(CircleShape)
                  .background(NotificationOrange)
                  .align(Alignment.TopEnd)
              )
            }
          }

          DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(OneUiCardBackground)
          ) {
            DropdownMenuItem(
              text = { Text("Settings", color = OneUiTextPrimary) },
              onClick = {
                showMenu = false
                onSettingsClick()
              }
            )
            if (currentTab == MainTab.CONTACTS) {
              DropdownMenuItem(
                text = { Text("Add new contact", color = OneUiTextPrimary) },
                onClick = {
                  showMenu = false
                  onAddContactClick()
                }
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Floating Dark Pill Navigation Bar exactly matching Samsung One UI phone screenshots (Images 1, 2, 4, 6):
 * - Floats gracefully above the bottom system navigation insets
 * - Pill container: RoundedCornerShape(32.dp) with OneUiNavPill background
 * - 3 Tabs: Keypad, Recents, Contacts
 * - Selected tab highlighted by an active inner pill (OneUiNavActivePill)
 */
@Composable
fun OneUiFloatingBottomNav(
  selectedTab: MainTab,
  onTabSelected: (MainTab) -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .windowInsetsPadding(WindowInsets.navigationBars)
      .padding(bottom = 12.dp),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      shape = RoundedCornerShape(32.dp),
      color = OneUiNavPill,
      tonalElevation = 6.dp,
      shadowElevation = 8.dp,
      modifier = Modifier.padding(horizontal = 24.dp)
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OneUiNavTabItem(
          label = "Keypad",
          icon = Icons.Rounded.Dialpad,
          isSelected = selectedTab == MainTab.KEYPAD,
          onClick = { onTabSelected(MainTab.KEYPAD) },
          tag = "nav_keypad"
        )

        OneUiNavTabItem(
          label = "Recents",
          icon = Icons.Rounded.History,
          isSelected = selectedTab == MainTab.RECENTS,
          onClick = { onTabSelected(MainTab.RECENTS) },
          tag = "nav_recents"
        )

        OneUiNavTabItem(
          label = "Contacts",
          icon = Icons.Rounded.Person,
          isSelected = selectedTab == MainTab.CONTACTS,
          onClick = { onTabSelected(MainTab.CONTACTS) },
          tag = "nav_contacts"
        )
      }
    }
  }
}

@Composable
private fun OneUiNavTabItem(
  label: String,
  icon: ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit,
  tag: String
) {
  val interactionSource = remember { MutableInteractionSource() }

  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(24.dp))
      .background(if (isSelected) OneUiNavActivePill else Color.Transparent)
      .clickable(interactionSource = interactionSource, indication = null) { onClick() }
      .padding(horizontal = 18.dp, vertical = 8.dp)
      .testTag(tag),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (isSelected) Color.White else OneUiTextSecondary,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
          fontSize = 11.sp
        ),
        color = if (isSelected) Color.White else OneUiTextSecondary
      )
    }
  }
}

@Composable
fun BackendNoticeDialog(
  notice: BackendNoticeState,
  onDismiss: () -> Unit,
  onPreviewCall: ((String, String?) -> Unit)? = null
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(24.dp),
    containerColor = OneUiCardBackground,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(NotificationOrange)
        )
        Text(
          text = notice.title,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = OneUiTextPrimary
        )
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = notice.message,
          style = MaterialTheme.typography.bodyMedium,
          color = OneUiTextSecondary
        )

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF26282E),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "Target: ${notice.targetNumber}",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = OneUiTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Voice calls connect directly over internet via Supabase Edge Functions & LiveKit WebRTC.",
              style = MaterialTheme.typography.bodySmall,
              color = OneUiTextSecondary
            )
          }
        }
      }
    },
    confirmButton = {
      if (onPreviewCall != null) {
        Button(
          onClick = { onPreviewCall(notice.targetNumber, notice.targetName) },
          colors = ButtonDefaults.buttonColors(containerColor = CallGreen),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("preview_call_button")
        ) {
          Text("Preview Call UI", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
      } else {
        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26282E)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("dismiss_notice_button")
        ) {
          Text("OK", color = Color.White)
        }
      }
    },
    dismissButton = {
      if (onPreviewCall != null) {
        OutlinedButton(
          onClick = onDismiss,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = OneUiTextPrimary),
          modifier = Modifier.testTag("dismiss_notice_button")
        ) {
          Text("Understood")
        }
      }
    }
  )
}
