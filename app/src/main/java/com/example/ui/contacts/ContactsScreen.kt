package com.example.ui.contacts

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Contact
import com.example.ui.theme.CallGreen

@Composable
fun ContactsScreen(
  contacts: List<Contact>,
  searchQuery: String,
  onSearchQueryChanged: (String) -> Unit,
  onCallContact: (Contact) -> Unit,
  onAddContact: (name: String, phone: String) -> Unit,
  onDeleteContact: (Contact) -> Unit,
  modifier: Modifier = Modifier
) {
  var showAddDialog by remember { mutableStateOf(false) }

  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
      // Header with Title and Add Button
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Contacts",
          style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground
        )

        IconButton(
          onClick = { showAddDialog = true },
          modifier = Modifier.testTag("add_contact_button")
        ) {
          Icon(
            imageVector = Icons.Default.PersonAdd,
            contentDescription = "Add Contact",
            tint = CallGreen
          )
        }
      }

      // Search Field
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChanged,
        placeholder = { Text("Search by name or number...") },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary
          )
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { onSearchQueryChanged("") }) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear search",
                tint = MaterialTheme.colorScheme.secondary
              )
            }
          }
        },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("contacts_search_input"),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.onBackground,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
      )

      Spacer(modifier = Modifier.height(14.dp))

      if (contacts.isEmpty()) {
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
              imageVector = Icons.Default.People,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(36.dp)
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = if (searchQuery.isEmpty()) "No Contacts Yet" else "No matching contacts",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = if (searchQuery.isEmpty())
              "Save people you regularly call on Line Call to connect with one tap."
            else "No contacts found matching '$searchQuery'.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
          )

          if (searchQuery.isEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
              onClick = { showAddDialog = true },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111111)),
              modifier = Modifier.testTag("empty_state_add_contact_button")
            ) {
              Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Add Contact")
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          items(contacts, key = { it.id }) { contact ->
            ContactListItem(
              contact = contact,
              onCall = { onCallContact(contact) },
              onDelete = { onDeleteContact(contact) }
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

    // Add Contact Dialog
    if (showAddDialog) {
      AddContactDialog(
        onDismiss = { showAddDialog = false },
        onSave = { name, phone ->
          onAddContact(name, phone)
          showAddDialog = false
        }
      )
    }
  }
}

@Composable
fun ContactListItem(
  contact: Contact,
  onCall: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable { onCall() }
      .padding(vertical = 12.dp, horizontal = 4.dp)
      .testTag("contact_item_${contact.id}"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = contact.avatarInitials,
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Column {
        Text(
          text = contact.name,
          style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = contact.phoneNumber,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.secondary
        )
      }
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      IconButton(
        onClick = onDelete,
        modifier = Modifier.size(36.dp)
      ) {
        Icon(
          imageVector = Icons.Default.DeleteOutline,
          contentDescription = "Delete Contact",
          tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
          modifier = Modifier.size(18.dp)
        )
      }

      // Quick Call Action
      IconButton(
        onClick = onCall,
        modifier = Modifier.size(40.dp)
      ) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(CallGreen.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Phone,
            contentDescription = "Call Contact",
            tint = CallGreen,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

@Composable
fun AddContactDialog(
  initialPhone: String = "",
  onDismiss: () -> Unit,
  onSave: (name: String, phone: String) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf(initialPhone) }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(18.dp),
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Text(
        text = "New Contact",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Full Name") },
          placeholder = { Text("e.g. Jordan Miller") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("new_contact_name_input"),
          shape = RoundedCornerShape(10.dp)
        )

        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Phone Number") },
          placeholder = { Text("+1 (555) 000-0000") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("new_contact_phone_input"),
          shape = RoundedCornerShape(10.dp)
        )

        Text(
          text = "Contacts are saved locally in Line Call for fast internet calling.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.secondary
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(name, phone) },
        enabled = name.isNotBlank() && phone.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111111)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("save_contact_button")
      ) {
        Text("Save Contact", color = Color.White)
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
      }
    }
  )
}
