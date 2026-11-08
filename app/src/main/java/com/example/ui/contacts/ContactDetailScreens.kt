package com.example.ui.contacts

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
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Contact
import com.example.data.model.UserAccount
import com.example.ui.theme.CallGreen
import com.example.ui.theme.OneUiCardBackground
import com.example.ui.theme.OneUiCardElevated
import com.example.ui.theme.OneUiDarkBackground
import com.example.ui.theme.OneUiNavPill
import com.example.ui.theme.OneUiTextMuted
import com.example.ui.theme.OneUiTextPrimary
import com.example.ui.theme.OneUiTextSecondary

/**
 * Contact Details Screen precisely matching Image 7:
 * - Back button `<`
 * - Large rounded card with contact info, avatar circle, and quick action buttons (Call, Message, Video)
 * - "History" pill card
 * - "Storage locations" card
 * - Bottom floating pill toolbar: Star (Favourites), Edit, Share, More
 */
@Composable
fun ContactDetailScreen(
  contact: Contact,
  onBack: () -> Unit,
  onCall: (Contact) -> Unit,
  onToggleFavorite: (Contact) -> Unit,
  onDelete: (Contact) -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier.fillMaxSize(),
    color = OneUiDarkBackground
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)
        .padding(horizontal = 16.dp)
    ) {
      // Top Bar: Back Button
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("contact_detail_back")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = OneUiTextPrimary,
            modifier = Modifier.size(24.dp)
          )
        }
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Hero Card (Image 7)
        Card(
          shape = RoundedCornerShape(26.dp),
          colors = CardDefaults.cardColors(containerColor = OneUiCardBackground),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Top
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = contact.name,
                  style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                  ),
                  color = OneUiTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Mobile ${contact.phoneNumber}",
                  style = MaterialTheme.typography.bodyMedium,
                  color = OneUiTextSecondary
                )
              }

              // Circular Avatar on the right
              Box(
                modifier = Modifier
                  .size(56.dp)
                  .clip(CircleShape)
                  .background(Color(0xFFFCA5A5)), // Soft peach from screenshot
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Person,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(34.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons Row inside Card: Call (Green), Message (Blue), Video
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              // Call Button
              Surface(
                onClick = { onCall(contact) },
                shape = CircleShape,
                color = Color(0xFF26282E),
                modifier = Modifier
                  .size(48.dp)
                  .testTag("detail_call_btn")
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Call",
                    tint = CallGreen,
                    modifier = Modifier.size(22.dp)
                  )
                }
              }

              // Audio / Message Button
              Surface(
                onClick = { onCall(contact) },
                shape = CircleShape,
                color = Color(0xFF26282E),
                modifier = Modifier.size(48.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "Message",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(20.dp)
                  )
                }
              }

              // Video / Meet Button
              Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.size(48.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Video",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(24.dp)
                  )
                }
              }
            }
          }
        }

        // "Line Call Voice" Card
        Surface(
          shape = RoundedCornerShape(22.dp),
          color = OneUiCardBackground,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "Line Call Voice",
              style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
              color = OneUiTextPrimary
            )
            Icon(
              imageVector = Icons.Default.Phone,
              contentDescription = null,
              tint = CallGreen,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        // "History" Pill Button (Image 7)
        Surface(
          shape = RoundedCornerShape(22.dp),
          color = OneUiCardElevated,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = "History",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
              color = OneUiTextPrimary
            )
          }
        }

        // "Storage locations" Card (Image 7)
        Surface(
          shape = RoundedCornerShape(22.dp),
          color = OneUiCardElevated,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = "Storage locations",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
              color = OneUiTextPrimary
            )
          }
        }
      }

      // Bottom Floating Action Pill Toolbar (Image 7): Star (Favourites), Edit, Share, More
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .windowInsetsPadding(WindowInsets.navigationBars)
          .padding(bottom = 12.dp),
        contentAlignment = Alignment.Center
      ) {
        Surface(
          shape = RoundedCornerShape(32.dp),
          color = OneUiNavPill,
          tonalElevation = 6.dp,
          shadowElevation = 8.dp
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Favourites toggle
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.clickable { onToggleFavorite(contact) }
            ) {
              Icon(
                imageVector = if (contact.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                contentDescription = "Favourites",
                tint = if (contact.isFavorite) Color(0xFFFBBF24) else OneUiTextSecondary,
                modifier = Modifier.size(22.dp)
              )
              Text(
                text = "Favourites",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = OneUiTextSecondary
              )
            }

            // Edit
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit",
                tint = OneUiTextSecondary,
                modifier = Modifier.size(22.dp)
              )
              Text(
                text = "Edit",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = OneUiTextSecondary
              )
            }

            // Delete / Share
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.clickable {
                onDelete(contact)
                onBack()
              }
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = OneUiTextSecondary,
                modifier = Modifier.size(22.dp)
              )
              Text(
                text = "Delete",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = OneUiTextSecondary
              )
            }

            // More
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More",
                tint = OneUiTextSecondary,
                modifier = Modifier.size(22.dp)
              )
              Text(
                text = "More",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = OneUiTextSecondary
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Add / Edit Contact Screen matching Samsung One UI form style (Image 3):
 * - Top account selector pill
 * - Two top options: "Profile card" and "Picture"
 * - Grouped card form inputs: Name, Phone, Work info, Email, Address, Groups
 * - Bottom floating pill button: "Cancel | Save"
 */
@Composable
fun AddContactOneUiScreen(
  initialPhone: String = "",
  currentUser: UserAccount?,
  onCancel: () -> Unit,
  onSave: (name: String, phone: String) -> Unit,
  modifier: Modifier = Modifier
) {
  val focusManager = LocalFocusManager.current
  var name by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf(initialPhone) }
  var email by remember { mutableStateOf("") }
  var workInfo by remember { mutableStateOf("") }

  Surface(
    modifier = modifier.fillMaxSize(),
    color = OneUiDarkBackground
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)
        .padding(horizontal = 20.dp)
    ) {
      // Top Account Selector Pill (Image 3)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 12.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(Color.White),
          contentAlignment = Alignment.Center
        ) {
          Text("G", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFEA4335))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = currentUser?.email ?: "omolayojoy568@gmail.com",
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
          color = OneUiTextPrimary
        )
        Icon(
          imageVector = Icons.Default.KeyboardArrowDown,
          contentDescription = null,
          tint = OneUiTextSecondary,
          modifier = Modifier.size(18.dp)
        )
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Profile Card / Picture Selector (Image 3)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Profile card
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 14.dp)
          ) {
            Box(
              modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF26282E))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = Color(0xFF26282E)
            ) {
              Text(
                text = "Profile card",
                style = MaterialTheme.typography.labelSmall,
                color = OneUiTextPrimary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
              )
            }
          }

          // Picture
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 14.dp)
          ) {
            Box(
              modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF26282E))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = Color(0xFF26282E)
            ) {
              Text(
                text = "Picture",
                style = MaterialTheme.typography.labelSmall,
                color = OneUiTextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Group 1: Name & Phone & Work in One UI Rounded Card (Image 3)
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = OneUiCardBackground),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            OneUiFormField(
              icon = Icons.Default.Person,
              value = name,
              onValueChange = { name = it },
              placeholder = "Name",
              tag = "new_contact_name_input"
            )
            HorizontalDivider(color = Color(0xFF26282E), thickness = 0.5.dp)
            OneUiFormField(
              icon = Icons.Default.Phone,
              value = phone,
              onValueChange = { phone = it },
              placeholder = "Phone",
              keyboardType = KeyboardType.Phone,
              tag = "new_contact_phone_input"
            )
            HorizontalDivider(color = Color(0xFF26282E), thickness = 0.5.dp)
            OneUiFormField(
              icon = Icons.Default.Business,
              value = workInfo,
              onValueChange = { workInfo = it },
              placeholder = "Work info"
            )
          }
        }

        // Group 2: Email in Rounded Card (Image 3)
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = OneUiCardBackground),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            OneUiFormField(
              icon = Icons.Default.Email,
              value = email,
              onValueChange = { email = it },
              placeholder = "Email",
              keyboardType = KeyboardType.Email
            )
          }
        }

        // Group 3: Groups in Rounded Card (Image 3)
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = OneUiCardBackground),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            OneUiFormField(
              icon = Icons.Default.Group,
              value = "",
              onValueChange = { },
              placeholder = "Groups"
            )
          }
        }
      }

      // Bottom Floating Pill: [ Cancel  |  Save ] exactly as in Image 3
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .windowInsetsPadding(WindowInsets.navigationBars)
          .padding(bottom = 12.dp),
        contentAlignment = Alignment.Center
      ) {
        Surface(
          shape = RoundedCornerShape(32.dp),
          color = OneUiNavPill,
          tonalElevation = 6.dp,
          shadowElevation = 8.dp
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            Text(
              text = "Cancel",
              style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
              color = OneUiTextPrimary,
              modifier = Modifier
                .clickable {
                  focusManager.clearFocus(force = true)
                  onCancel()
                }
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("cancel_add_contact_btn")
            )

            Box(
              modifier = Modifier
                .width(1.dp)
                .height(20.dp)
                .background(Color(0xFF383B44))
            )

            Text(
              text = "Save",
              style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = if (name.isNotBlank() && phone.isNotBlank()) CallGreen else OneUiTextSecondary
              ),
              modifier = Modifier
                .clickable(enabled = name.isNotBlank() && phone.isNotBlank()) {
                  focusManager.clearFocus(force = true)
                  onSave(name, phone)
                }
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("save_add_contact_btn")
            )
          }
        }
      }
    }
  }
}

@Composable
private fun OneUiFormField(
  icon: ImageVector,
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  keyboardType: KeyboardType = KeyboardType.Text,
  tag: String = ""
) {
  val focusManager = LocalFocusManager.current

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = Color(0xFF60A5FA), // Samsung blue icon tint from screenshot
      modifier = Modifier.size(22.dp)
    )
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      placeholder = { Text(placeholder, color = OneUiTextMuted) },
      singleLine = true,
      keyboardOptions = KeyboardOptions(
        keyboardType = keyboardType,
        imeAction = ImeAction.Done
      ),
      keyboardActions = KeyboardActions(onDone = {
        focusManager.clearFocus(force = true)
      }),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent,
        focusedTextColor = OneUiTextPrimary,
        unfocusedTextColor = OneUiTextPrimary,
        cursorColor = OneUiTextPrimary
      ),
      modifier = Modifier
        .fillMaxWidth()
        .testTag(tag)
    )
  }
}
