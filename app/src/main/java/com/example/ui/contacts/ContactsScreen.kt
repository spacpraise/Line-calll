package com.example.ui.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Contact
import com.example.data.model.UserAccount
import com.example.ui.theme.OneUiCardBackground
import com.example.ui.theme.OneUiTextPrimary
import com.example.ui.theme.OneUiTextSecondary
import com.example.ui.theme.getFavoriteGradient

/**
 * Contacts and Favourites Screen strictly implementing Samsung One UI Phone design (Images 1 & 4):
 * - Banner tip: "Your favourite contacts now appear as cards..."
 * - "★ Favourites" section with 3-column pastel gradient cards
 * - "My profile" pill card
 * - "Groups" pill card
 * - Alphabetical contact list items inside dark rounded containers
 */
@Composable
fun ContactsScreen(
  contacts: List<Contact>,
  currentUser: UserAccount?,
  searchQuery: String,
  onSearchQueryChanged: (String) -> Unit,
  onContactClick: (Contact) -> Unit,
  onProfileClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showBannerTip by remember { mutableStateOf(true) }
  val favorites = remember(contacts) { contacts.filter { it.isFavorite } }
  val regularContacts = remember(contacts) { contacts.filter { !it.isFavorite } }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(bottom = 96.dp) // Clearance for floating bottom nav
  ) {
    // 1. Dismissable Banner Card (Image 1)
    if (showBannerTip && favorites.isNotEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = OneUiCardBackground),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
        ) {
          Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text(
              text = "Your favourite contacts now appear as cards. You can change to list view in the More menu.",
              style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
              color = OneUiTextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "View as list",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = OneUiTextPrimary,
                modifier = Modifier
                  .clickable { showBannerTip = false }
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              )
              Spacer(modifier = Modifier.width(16.dp))
              Text(
                text = "OK",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = OneUiTextPrimary,
                modifier = Modifier
                  .clickable { showBannerTip = false }
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }
      }
    }

    // 2. "★ Favourites" Header & Grid (Image 1 & 4)
    if (favorites.isNotEmpty() && searchQuery.isEmpty()) {
      item {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = OneUiTextPrimary,
            modifier = Modifier.size(16.dp)
          )
          Text(
            text = "Favourites",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp
            ),
            color = OneUiTextPrimary
          )
        }
      }

      // Favourites 3-column card grid
      item {
        val chunked = favorites.chunked(3)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          chunked.forEach { rowItems ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              rowItems.forEachIndexed { index, contact ->
                Box(modifier = Modifier.weight(1f)) {
                  FavoriteCard(
                    contact = contact,
                    onClick = { onContactClick(contact) }
                  )
                }
              }
              // Fill remaining columns if last row has less than 3
              if (rowItems.size < 3) {
                repeat(3 - rowItems.size) {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(18.dp))
      }
    }

    // 3. "My profile" pill card (Image 4)
    if (searchQuery.isEmpty()) {
      item {
        Text(
          text = "My profile",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
          color = OneUiTextSecondary,
          modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 6.dp)
        )

        Surface(
          onClick = onProfileClick,
          shape = RoundedCornerShape(22.dp),
          color = OneUiCardBackground,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("my_profile_pill_card")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF818CF8)), // Soft indigo avatar
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
              )
            }
            Text(
              text = currentUser?.displayName?.ifBlank { "Omolayo" } ?: "Omolayo",
              style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
              color = OneUiTextPrimary
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // "Groups" pill card (Image 4)
        Surface(
          onClick = { },
          shape = RoundedCornerShape(22.dp),
          color = OneUiCardBackground,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF374151)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                tint = Color(0xFFD1D5DB),
                modifier = Modifier.size(22.dp)
              )
            }
            Text(
              text = "Groups",
              style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
              color = OneUiTextPrimary
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))
      }
    }

    // 4. Contacts List Grouped in Card (Image 4)
    item {
      Text(
        text = if (searchQuery.isEmpty()) "&" else "Search results",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = OneUiTextSecondary,
        modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
      )

      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = OneUiCardBackground),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          val listToRender = if (searchQuery.isEmpty()) regularContacts else contacts
          listToRender.forEachIndexed { index, contact ->
            ContactListRow(
              contact = contact,
              onClick = { onContactClick(contact) }
            )
            if (index < listToRender.lastIndex) {
              HorizontalDivider(
                color = Color(0xFF26282E),
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 64.dp)
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Pastel Favourites Card matching Image 1:
 * - Rounded square/rectangle with soft pastel gradient
 * - Large initial in a translucent white circular ring
 * - Name in white text at bottom
 */
@Composable
fun FavoriteCard(
  contact: Contact,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
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

      // Avatar circle with white stroke
      Box(
        modifier = Modifier
          .size(54.dp)
          .clip(CircleShape)
          .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = contact.avatarInitials.take(1),
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
          ),
          color = Color.White
        )
      }

      // Name at bottom
      Text(
        text = contact.name,
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp
        ),
        color = Color.White,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun ContactListRow(
  contact: Contact,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val avatarColors = listOf(
    Color(0xFFFCA5A5), // Soft peach/salmon
    Color(0xFF86EFAC), // Soft green
    Color(0xFF93C5FD), // Soft blue
    Color(0xFFFDE047), // Soft yellow
    Color(0xFFF472B6)  // Soft pink
  )
  val avatarColor = avatarColors[contact.id.toInt().mod(avatarColors.size)]

  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .testTag("contact_row_${contact.name}"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(avatarColor),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.Person,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(24.dp)
      )
    }

    Text(
      text = contact.name,
      style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
      color = OneUiTextPrimary,
      modifier = Modifier.weight(1f)
    )
  }
}
