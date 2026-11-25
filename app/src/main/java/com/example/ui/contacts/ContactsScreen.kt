package com.example.ui.contacts

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Contact
import com.example.data.model.UserAccount
import com.example.ui.theme.getFavoriteGradient

/**
 * Contacts, redesigned around a WhatsApp/Telegram identity-first pattern:
 * - Your own profile sits as a slim top row, not a mid-list pill card, since
 *   it's "you", not a contact you're browsing.
 * - Favourites keep the pastel gradient card grid (already close to the
 *   WhatsApp/Telegram feel) with consistent avatar-initial styling shared
 *   with the regular contact rows and Recents.
 * - No placeholder display name is ever shown: a signed-in user with no name
 *   set sees "Add your name" as an actionable prompt instead of fake data.
 */
@Composable
fun ContactsScreen(
  contacts: List<Contact>,
  currentUser: UserAccount?,
  searchQuery: String,
  onSearchQueryChanged: (String) -> Unit,
  onContactClick: (Contact) -> Unit,
  onProfileClick: () -> Unit,
  onImportContacts: () -> Unit,
  syncStatus: String,
  modifier: Modifier = Modifier
) {
  var showBannerTip by remember { mutableStateOf(true) }
  var selectedFilter by remember { mutableStateOf("all") }
  val filteredContacts = remember(contacts, selectedFilter) {
    when (selectedFilter) {
      "registered" -> contacts.filter { it.isRegistered }
      "online" -> contacts.filter { it.presence == "online" }
      else -> contacts
    }
  }
  val favorites = remember(filteredContacts) { filteredContacts.filter { it.isFavorite } }
  val regularContacts = remember(filteredContacts) { filteredContacts.filter { !it.isFavorite } }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(bottom = 96.dp) // Clearance for floating bottom nav
  ) {
    // Your own profile, as a slim top-of-list row -- it's "you", not a
    // contact you're browsing, so it doesn't share visual weight with the
    // favourites grid or the contact list below it.
    if (searchQuery.isEmpty()) {
      item {
        Surface(
          onClick = onProfileClick,
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 12.dp).testTag("my_profile_pill_card")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            val initial = currentUser?.displayName?.trim()?.firstOrNull()?.uppercaseChar()
            Box(
              modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF818CF8)),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = initial?.toString() ?: "+",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = currentUser?.displayName?.trim()?.takeIf { it.isNotBlank() } ?: "Add your name",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = currentUser?.phoneNumber?.takeIf { it.isNotBlank() } ?: "Set up your profile",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Icon(
              imageVector = Icons.Rounded.ChevronRight,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf("all" to "All", "registered" to "Registered", "online" to "Online").forEach { (filter, label) ->
          Surface(
            onClick = { selectedFilter = filter },
            shape = RoundedCornerShape(18.dp),
            color = if (selectedFilter == filter) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
            modifier = Modifier.testTag("contact_filter_$filter")
          ) {
            Text(
              text = label,
              color = if (selectedFilter == filter) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
              style = MaterialTheme.typography.labelLarge,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
            )
          }
        }
      }
    }

    if (showBannerTip && favorites.isNotEmpty() && searchQuery.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
          Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text(
              text = "Your favourite contacts appear as cards here. Tap one to call instantly.",
              style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
              Text(
                text = "Got it",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable { showBannerTip = false }.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }
      }
    }

    if (favorites.isNotEmpty() && searchQuery.isEmpty()) {
      item {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
        ) {
          Icon(Icons.Rounded.Star, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
          Text(
            text = "Favourites",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }

      item {
        val chunked = favorites.chunked(3)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          chunked.forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              rowItems.forEach { contact ->
                Box(modifier = Modifier.weight(1f)) {
                  FavoriteCard(contact = contact, onClick = { onContactClick(contact) })
                }
              }
              if (rowItems.size < 3) {
                repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(18.dp))
      }
    }

    if (searchQuery.isEmpty()) {
      item {
        Surface(
          onClick = { },
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFF374151)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Rounded.Group, contentDescription = null, tint = Color(0xFFD1D5DB), modifier = Modifier.size(20.dp))
            }
            Text(
              text = "Groups",
              style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        TextButton(onClick = onImportContacts, modifier = Modifier.fillMaxWidth()) {
          Text("Import phone contacts", color = MaterialTheme.colorScheme.primary)
        }
        Text(
          text = "Contacts stay on this phone. Line Call checks only saved phone numbers and emails to find registered accounts.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 6.dp)
        )
        if (syncStatus.isNotBlank()) {
          Text(
            text = syncStatus,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
          )
        }
        Spacer(modifier = Modifier.height(18.dp))
      }
    }

    val listToRender = if (searchQuery.isEmpty()) regularContacts else filteredContacts
    if (listToRender.isEmpty()) {
      item {
        Text(
          text = if (searchQuery.isEmpty() && selectedFilter == "all") "No contacts yet" else "No matching contacts",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
          textAlign = TextAlign.Center
        )
      }
    } else {
      item {
        Text(
          text = if (searchQuery.isEmpty()) "All contacts" else "Search results",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
        )
      }
      items(listToRender, key = { it.id }) { contact ->
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
        ) {
          ContactListRow(contact = contact, onClick = { onContactClick(contact) })
        }
      }
    }
  }
}

private val avatarColors = listOf(
  Color(0xFFFCA5A5), Color(0xFF86EFAC), Color(0xFF93C5FD), Color(0xFFFDE047), Color(0xFFF472B6)
)

private fun contactInitials(contact: Contact): String {
  val fromName = contact.name.trim().takeIf { it.isNotBlank() }
    ?.split(Regex("\\s+"))?.take(2)?.mapNotNull { it.firstOrNull()?.toString() }?.joinToString("")
  return (fromName ?: contact.avatarInitials.takeIf { it.isNotBlank() } ?: "?").uppercase()
}

@Composable
fun FavoriteCard(contact: Contact, onClick: () -> Unit, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(132.dp)
      .clip(RoundedCornerShape(22.dp))
      .background(getFavoriteGradient(contact.gradientIndex))
      .clickable { onClick() }
      .padding(12.dp)
      .testTag("fav_card_${contact.name}"),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxSize()
    ) {
      Spacer(modifier = Modifier.height(2.dp))
      Box(
        modifier = Modifier.size(54.dp).clip(CircleShape).border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = contactInitials(contact).take(1),
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp),
          color = Color.White
        )
      }
      Text(
        text = contact.name,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
        color = Color.White,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    }
    Row(
      modifier = Modifier.align(Alignment.TopEnd),
      horizontalArrangement = Arrangement.spacedBy(5.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (contact.isRegistered) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = "Registered Line Call account", tint = Color.White, modifier = Modifier.size(15.dp))
      }
      if (contact.presence == "online") {
        Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFF22C55E)))
      }
    }
  }
}

@Composable
fun ContactListRow(contact: Contact, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val view = LocalView.current
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val pressScale by animateFloatAsState(
    targetValue = if (pressed) 0.985f else 1f,
    animationSpec = spring(dampingRatio = 0.62f, stiffness = 560f),
    label = "contact_row_press"
  )

  val avatarColor = avatarColors[(contact.id.hashCode().mod(avatarColors.size)).let { if (it < 0) it + avatarColors.size else it }]
  val initials = contactInitials(contact)

  Row(
    modifier = modifier
      .fillMaxWidth()
      .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
      .clickable(
        interactionSource = interaction,
        indication = null,
        onClick = { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK); onClick() }
      )
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .testTag("contact_row_${contact.name}"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Box(
      modifier = Modifier.size(42.dp).clip(CircleShape).background(avatarColor),
      contentAlignment = Alignment.Center
    ) {
      Text(initials, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }

    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = contact.name,
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = contact.phoneNumber,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    if (contact.isRegistered) {
      Icon(Icons.Rounded.CheckCircle, contentDescription = "Registered Line Call account", tint = Color(0xFF22C55E), modifier = Modifier.size(18.dp))
    }
    when (contact.presence) {
      "online" -> Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFF22C55E)))
      "in_call" -> Text("In call", color = MaterialTheme.colorScheme.tertiary, fontSize = 10.sp)
      else -> Unit
    }
  }
}
