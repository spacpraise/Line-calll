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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserAccount
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CallRed

@Composable
fun SettingsScreen(
  currentUser: UserAccount?,
  onUpdateDisplayName: (String) -> Unit,
  onSignOut: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showEditNameDialog by remember { mutableStateOf(false) }
  var newName by remember(currentUser?.displayName) { mutableStateOf(currentUser?.displayName.orEmpty()) }

  Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
          text = "Settings",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground
        )
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
      ) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF171717)),
                contentAlignment = Alignment.Center
              ) {
                val initials = currentUser?.displayName?.take(2)?.uppercase()
                  ?: currentUser?.email?.take(2)?.uppercase()
                  ?: "LC"
                Text(initials, color = Color.White, fontWeight = FontWeight.Bold)
              }
              Spacer(Modifier.width(14.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  currentUser?.displayName?.ifBlank { "Line Call user" } ?: "Line Call user",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                  currentUser?.email.orEmpty(),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.secondary
                )
              }
              IconButton(onClick = { newName = currentUser?.displayName.orEmpty(); showEditNameDialog = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit name", tint = CallGreen)
              }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
              Text("Phone number", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  currentUser?.phoneNumber?.ifBlank { "Not added" } ?: "Not added",
                  modifier = Modifier.weight(1f),
                  style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                )
                Surface(
                  shape = RoundedCornerShape(20.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                  Text(
                    if (currentUser?.isPhoneVerified == true) "Verified" else "Unverified",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                  )
                }
              }
            }
          }
        }

        Text(
          "CALL SETTINGS",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.secondary
        )

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column {
            Row(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
              Spacer(Modifier.width(14.dp))
              Column {
                Text("Notifications", fontWeight = FontWeight.SemiBold)
                Text(
                  "Allow Line Call to show incoming call alerts.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.secondary
                )
              }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            Row(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
              Spacer(Modifier.width(14.dp))
              Column {
                Text("Microphone", fontWeight = FontWeight.SemiBold)
                Text(
                  "Permission is requested when you make or answer a call.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.secondary
                )
              }
            }
          }
        }

        TextButton(
          onClick = onSignOut,
          modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
          Icon(Icons.Default.ExitToApp, contentDescription = null, tint = CallRed)
          Spacer(Modifier.width(8.dp))
          Text("Sign out", color = CallRed, fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }

  val focusManager = LocalFocusManager.current

  if (showEditNameDialog) {
    AlertDialog(
      onDismissRequest = {
        focusManager.clearFocus(force = true)
        showEditNameDialog = false
      },
      title = { Text("Edit display name") },
      text = {
        OutlinedTextField(
          value = newName,
          onValueChange = { newName = it },
          label = { Text("Display name") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
          keyboardActions = KeyboardActions(onDone = {
            focusManager.clearFocus(force = true)
            onUpdateDisplayName(newName.trim())
            showEditNameDialog = false
          }),
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(
          onClick = {
            focusManager.clearFocus(force = true)
            onUpdateDisplayName(newName.trim())
            showEditNameDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF171717))
        ) { Text("Save", color = Color.White) }
      },
      dismissButton = {
        TextButton(onClick = {
          focusManager.clearFocus(force = true)
          showEditNameDialog = false
        }) { Text("Cancel") }
      }
    )
  }
}
