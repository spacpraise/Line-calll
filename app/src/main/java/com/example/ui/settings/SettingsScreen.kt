package com.example.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Phone
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
import coil.compose.AsyncImage
import com.example.data.model.UserAccount
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CallRed

@Composable
fun SettingsScreen(
  currentUser: UserAccount?,
  onUpdateDisplayName: (String) -> Unit,
  onUpdatePhoneNumber: (String) -> Unit,
  onUpdateEmail: (String) -> Unit,
  onAvatarSelected: (android.net.Uri) -> Unit,
  onSignOut: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var editField by remember { mutableStateOf<String?>(null) }
  var editValue by remember { mutableStateOf("") }
  val focusManager = LocalFocusManager.current
  val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    uri?.let(onAvatarSelected)
  }

  fun openEditor(field: String, value: String) {
    editField = field
    editValue = value
  }

  Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
        Text("Settings", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground)
      }

      Column(
        modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
      ) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier.size(76.dp).clip(CircleShape).background(Color(0xFF171717)),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  currentUser?.displayName?.take(2)?.uppercase()
                    ?: currentUser?.email?.take(2)?.uppercase() ?: "LC",
                  color = Color.White, fontWeight = FontWeight.Bold
                )
                if (!currentUser?.avatarUrl.isNullOrBlank()) {
                  AsyncImage(
                    model = currentUser?.avatarUrl,
                    contentDescription = "Profile photo",
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                  )
                }
              }
              Spacer(Modifier.width(14.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(currentUser?.displayName?.ifBlank { "Line Call user" } ?: "Line Call user",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                Text("Your Line Call profile", style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.secondary)
              }
            }
            Button(
              onClick = { imagePicker.launch("image/*") },
              modifier = Modifier.fillMaxWidth().height(46.dp),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242424))
            ) {
              Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = Color.White)
              Spacer(Modifier.width(8.dp))
              Text(if (currentUser?.avatarUrl.isNullOrBlank()) "Add profile photo" else "Change profile photo",
                color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ProfileDetailRow("Display name", currentUser?.displayName?.ifBlank { "Not set" } ?: "Not set",
              onEdit = { openEditor("name", currentUser?.displayName.orEmpty()) })
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ProfileDetailRow("Phone number", currentUser?.phoneNumber?.ifBlank { "Not added" } ?: "Not added",
              trailing = if (currentUser?.isPhoneVerified == true) "Verified" else "Not verified",
              onEdit = { openEditor("phone", currentUser?.phoneNumber.orEmpty()) })
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ProfileDetailRow("Email address", currentUser?.email.orEmpty(),
              onEdit = { openEditor("email", currentUser?.email.orEmpty()) })
          }
        }

        Text("CALL SETTINGS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.secondary)
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Rounded.Email, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
              Spacer(Modifier.width(14.dp))
              Column {
                Text("Account email", fontWeight = FontWeight.SemiBold)
                Text("Email changes require confirmation from your new inbox.",
                  style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
              }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Rounded.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
              Spacer(Modifier.width(14.dp))
              Column {
                Text("Phone number", fontWeight = FontWeight.SemiBold)
                Text("Use the full international format, for example +2348012345678.",
                  style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
              }
            }
          }
        }

        TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().height(52.dp)) {
          Icon(Icons.Rounded.ExitToApp, contentDescription = null, tint = CallRed)
          Spacer(Modifier.width(8.dp))
          Text("Sign out", color = CallRed, fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }

  editField?.let { field ->
    val title = when (field) {
      "name" -> "Edit display name"
      "phone" -> "Edit phone number"
      else -> "Change email address"
    }
    val label = when (field) {
      "name" -> "Display name"
      "phone" -> "International phone number"
      else -> "New email address"
    }
    AlertDialog(
      onDismissRequest = { focusManager.clearFocus(force = true); editField = null },
      title = { Text(title) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = editValue,
            onValueChange = { editValue = it },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
              keyboardType = when (field) {
                "phone" -> KeyboardType.Phone
                "email" -> KeyboardType.Email
                else -> KeyboardType.Text
              },
              imeAction = ImeAction.Done
            )
          )
          if (field == "email") {
            Text("A confirmation link will be sent to the new address. Your sign-in email changes only after confirmation.",
              style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
          } else if (field == "phone") {
            Text("Your phone number will be marked as unverified until ownership verification is added.",
              style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            focusManager.clearFocus(force = true)
            when (field) {
              "name" -> onUpdateDisplayName(editValue.trim())
              "phone" -> onUpdatePhoneNumber(editValue.trim())
              "email" -> onUpdateEmail(editValue.trim())
            }
            editField = null
          },
          enabled = editValue.trim().isNotBlank(),
          colors = ButtonDefaults.buttonColors(containerColor = CallGreen)
        ) { Text("Save", color = Color.White) }
      },
      dismissButton = {
        TextButton(onClick = { focusManager.clearFocus(force = true); editField = null }) { Text("Cancel") }
      }
    )
  }
}

@Composable
private fun ProfileDetailRow(
  label: String,
  value: String,
  trailing: String? = null,
  onEdit: () -> Unit
) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Column(modifier = Modifier.weight(1f)) {
      Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
      Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
      if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
    }
    TextButton(onClick = onEdit) {
      Icon(Icons.Rounded.Edit, contentDescription = null, tint = CallGreen, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(4.dp))
      Text("Edit", color = CallGreen)
    }
  }
}
