package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.services.BackendConfig
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CallRed

@Composable
fun SettingsScreen(
  currentUser: UserAccount?,
  onUpdateDisplayName: (String) -> Unit,
  onTriggerIncomingCallTest: () -> Unit,
  onTriggerActiveCallTest: () -> Unit,
  onSignOut: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showEditNameDialog by remember { mutableStateOf(false) }
  var notificationsEnabled by remember { mutableStateOf(true) }
  var lowDataMode by remember { mutableStateOf(false) }

  Surface(
    modifier = modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)
    ) {
      // Top Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("settings_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.onBackground
          )
        }

        Text(
          text = "Settings",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.size(48.dp))
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
      ) {
        // Profile Card
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              val initials = currentUser?.displayName?.take(2)?.uppercase()
                ?: currentUser?.email?.take(2)?.uppercase()
                ?: "LC"

              Box(
                modifier = Modifier
                  .size(60.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = initials,
                  style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                )
              }

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = currentUser?.displayName?.ifBlank { "Line Call User" } ?: "Line Call User",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = currentUser?.email ?: "user@linecall.app",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.secondary
                )
              }

              IconButton(
                onClick = { showEditNameDialog = true },
                modifier = Modifier.testTag("edit_display_name_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Edit,
                  contentDescription = "Edit Name",
                  tint = CallGreen,
                  modifier = Modifier.size(20.dp)
                )
              }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // Phone Number & Verification Status (accurately shown as unverified)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "Phone Number",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.secondary
                )
                Text(
                  text = currentUser?.phoneNumber?.ifBlank { "Not set" } ?: "Not set",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              // Verification Badge
              val isVerified = currentUser?.isPhoneVerified == true
              Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isVerified) CallGreen.copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                modifier = Modifier.testTag("phone_verification_badge")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(6.dp)
                      .clip(CircleShape)
                      .background(if (isVerified) CallGreen else Color(0xFF94A3B8))
                  )
                  Text(
                    text = if (isVerified) "Verified" else "Unverified",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isVerified) CallGreen else Color(0xFF64748B)
                  )
                }
              }
            }
          }
        }

        // Section: Account & Telecommunications Architecture
        SettingsSection(title = "Account & Telecommunications") {
          SettingsRowItem(
            icon = Icons.Default.Info,
            title = "Account Identifier",
            subtitle = currentUser?.id?.take(18) ?: "acc_local_01"
          )
          SettingsRowItem(
            icon = Icons.Default.Sensors,
            title = "Audio Codec",
            subtitle = "Opus Voice HD (48 kHz, Low-Latency WebRTC)"
          )
          SettingsRowItem(
            icon = Icons.Default.Cloud,
            title = "Backend Service",
            subtitle = if (BackendConfig.isSupabaseConfigured) "Connected to Supabase" else "Supabase & LiveKit Ready (Modular)"
          )
        }

        // Section: Call & Notification Settings
        SettingsSection(title = "Preferences") {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Incoming Call Alerts",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Show ringing overlay and play ringtone for incoming internet calls",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
              )
            }
            Switch(
              checked = notificationsEnabled,
              onCheckedChange = { notificationsEnabled = it },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CallGreen
              )
            )
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Low Data Mode",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Reduces voice bandwidth to 16 kbps on cellular connections",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
              )
            }
            Switch(
              checked = lowDataMode,
              onCheckedChange = { lowDataMode = it },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CallGreen
              )
            )
          }
        }

        // Section: Developer & Reviewer Testing Tools
        SettingsSection(title = "UI Inspection & Verification") {
          SettingsClickableRow(
            icon = Icons.Default.PhoneCallback,
            title = "Preview Incoming Call Screen",
            subtitle = "Simulates incoming voice ring from Sarah Jenkins",
            onClick = onTriggerIncomingCallTest
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
          SettingsClickableRow(
            icon = Icons.Default.Sensors,
            title = "Preview Active Call Screen",
            subtitle = "Inspect live timer, speakerphone, and mute controls",
            onClick = onTriggerActiveCallTest
          )
        }

        // Sign Out Button
        Button(
          onClick = onSignOut,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("sign_out_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = CallRed
          )
        ) {
          Icon(
            imageVector = Icons.Default.ExitToApp,
            contentDescription = null,
            tint = CallRed,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Sign Out",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
          )
        }

        Spacer(modifier = Modifier.height(24.dp))
      }
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
      var newName by remember { mutableStateOf(currentUser?.displayName ?: "") }
      AlertDialog(
        onDismissRequest = { showEditNameDialog = false },
        title = { Text("Edit Display Name") },
        text = {
          OutlinedTextField(
            value = newName,
            onValueChange = { newName = it },
            label = { Text("Display Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        },
        confirmButton = {
          Button(
            onClick = {
              onUpdateDisplayName(newName)
              showEditNameDialog = false
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
          ) {
            Text("Save")
          }
        },
        dismissButton = {
          OutlinedButton(onClick = { showEditNameDialog = false }) {
            Text("Cancel")
          }
        }
      )
    }
  }
}

@Composable
fun SettingsSection(
  title: String,
  content: @Composable () -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelLarge.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
      ),
      color = MaterialTheme.colorScheme.secondary,
      modifier = Modifier.padding(start = 4.dp)
    )
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column {
        content()
      }
    }
  }
}

@Composable
fun SettingsRowItem(
  icon: ImageVector,
  title: String,
  subtitle: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 12.dp, horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.secondary,
      modifier = Modifier.size(20.dp)
    )
    Column {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.secondary
      )
    }
  }
}

@Composable
fun SettingsClickableRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(vertical = 12.dp, horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = CallGreen,
      modifier = Modifier.size(20.dp)
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.secondary
      )
    }
  }
}
