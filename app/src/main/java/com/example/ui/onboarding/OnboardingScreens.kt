package com.example.ui.onboarding

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Country
import com.example.data.model.CountryList
import com.example.ui.ScreenState
import com.example.ui.theme.CallGreen

@Composable
fun WelcomeScreen(
  onGetStarted: () -> Unit,
  onLogIn: () -> Unit
) {
  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)
        .padding(horizontal = 28.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Spacer(modifier = Modifier.height(20.dp))

      // Center Brand Identity
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Box(
          modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(Color(0xFF111111)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Phone,
            contentDescription = "Line Call Logo",
            tint = CallGreen,
            modifier = Modifier.size(42.dp)
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "LINE CALL",
          style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
          ),
          color = MaterialTheme.colorScheme.onBackground
        )

        Text(
          text = "Your world. One call away.",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold
          ),
          color = MaterialTheme.colorScheme.onBackground
        )

        Text(
          text = "Make crystal-clear voice calls to people around the world using the internet.",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.secondary,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 16.dp)
        )
      }

      // Bottom Action Buttons
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Button(
          onClick = onGetStarted,
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("get_started_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF111111),
            contentColor = Color.White
          )
        ) {
          Text(
            text = "Get Started",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
          )
        }

        OutlinedButton(
          onClick = onLogIn,
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("log_in_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onBackground
          )
        ) {
          Text(
            text = "Log In",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

@Composable
fun RegistrationScreen(
  email: String,
  magicLinkSent: Boolean,
  authError: String?,
  onEmailChanged: (String) -> Unit,
  onSubmitEmail: () -> Unit,
  onBack: () -> Unit
) {
  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)
        .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier.testTag("auth_back_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = MaterialTheme.colorScheme.onBackground
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      if (!magicLinkSent) {
        // Email Entry Stage
        Text(
          text = "Enter your email",
          style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "We will send a verification link to confirm your email. You will create your password after opening the link.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
          value = email,
          onValueChange = onEmailChanged,
          placeholder = { Text("name@example.com") },
          label = { Text("Email address") },
          leadingIcon = {
            Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
          },
          singleLine = true,
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done
          ),
          keyboardActions = KeyboardActions(onDone = { onSubmitEmail() }),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("email_input"),
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.onBackground,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          )
        )

        if (authError != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = authError,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
          )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
          onClick = onSubmitEmail,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("email_continue_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF111111),
            contentColor = Color.White
          ),
          enabled = email.contains("@") && email.contains(".")
        ) {
          Text(
            text = "Continue",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
          )
        }

        Spacer(modifier = Modifier.height(20.dp))
      } else {
        // Confirmation Stage: Magic Link Sent
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = Modifier
              .size(72.dp)
              .clip(CircleShape)
              .background(CallGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = CallGreen,
              modifier = Modifier.size(40.dp)
            )
          }

          Spacer(modifier = Modifier.height(20.dp))

          Text(
            text = "Check your email",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "A verification link has been sent to:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = email,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
          )

          Spacer(modifier = Modifier.height(16.dp))

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "Open the verification link on this device. Line Call will take you to the password setup screen.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.padding(14.dp),
              textAlign = TextAlign.Center
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneNumberSetupScreen(
  selectedCountry: Country,
  phoneNumber: String,
  error: String?,
  onCountrySelected: (Country) -> Unit,
  onPhoneNumberChanged: (String) -> Unit,
  onSubmit: () -> Unit,
  onBack: () -> Unit
) {
  var showCountryPicker by remember { mutableStateOf(false) }
  val sheetState = rememberModalBottomSheetState()

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)
        .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier.testTag("phone_setup_back")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = MaterialTheme.colorScheme.onBackground
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "Your phone number",
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Your number becomes your Line Call address. Phone ownership is not verified yet, so only enter a number you control.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.secondary
      )

      Spacer(modifier = Modifier.height(24.dp))

      // Country selector pill
      Surface(
        onClick = { showCountryPicker = true },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("country_selector_button")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text(text = selectedCountry.flagEmoji, fontSize = 22.sp)
            Text(
              text = selectedCountry.name,
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = selectedCountry.dialCode,
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.secondary
            )
            Icon(
              imageVector = Icons.Default.KeyboardArrowDown,
              contentDescription = "Select country",
              tint = MaterialTheme.colorScheme.secondary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Phone number input
      OutlinedTextField(
        value = phoneNumber,
        onValueChange = onPhoneNumberChanged,
        prefix = {
          Text(
            text = "${selectedCountry.dialCode} ",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
        },
        placeholder = { Text(selectedCountry.exampleFormat) },
        label = { Text("Phone number") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
          keyboardType = KeyboardType.Phone,
          imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("phone_number_input"),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.onBackground,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
      )

      if (error != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = error,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Important honest status card
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFF737373))
            )
            Text(
              text = "Status: Unverified",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Line Call stores your registered number as unverified until production SMS verification is introduced. Standard E.164 formatting is applied.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
          )
        }
      }

      Spacer(modifier = Modifier.weight(1f))

      Button(
        onClick = onSubmit,
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("phone_continue_button"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFF111111),
          contentColor = Color.White
        ),
        enabled = phoneNumber.trim().length >= 5
      ) {
        Text(
          text = "Continue",
          style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))
    }

    // Modal sheet for Country selection
    if (showCountryPicker) {
      ModalBottomSheet(
        onDismissRequest = { showCountryPicker = false },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
      ) {
        var countrySearch by remember { mutableStateOf("") }
        val filteredCountries = remember(countrySearch) {
          if (countrySearch.isBlank()) CountryList.allCountries
          else CountryList.allCountries.filter {
            it.name.contains(countrySearch, ignoreCase = true) || it.dialCode.contains(countrySearch)
          }
        }

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
          Text(
            text = "Select Country",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )

          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = countrySearch,
            onValueChange = { countrySearch = it },
            placeholder = { Text("Search country or code...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
          )

          Spacer(modifier = Modifier.height(12.dp))

          LazyColumn(modifier = Modifier.height(340.dp)) {
            items(filteredCountries) { country ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    onCountrySelected(country)
                    showCountryPicker = false
                  }
                  .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  Text(text = country.flagEmoji, fontSize = 20.sp)
                  Text(
                    text = country.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
                Text(
                  text = country.dialCode,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.secondary
                )
              }
              HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            }
          }
        }
      }
    }
  }
}

@Composable
fun ProfileSetupScreen(
  displayName: String,
  onDisplayNameChanged: (String) -> Unit,
  onComplete: () -> Unit,
  onSkip: () -> Unit
) {
  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)
        .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        TextButton(
          onClick = onSkip,
          modifier = Modifier.testTag("skip_profile_setup")
        ) {
          Text(
            text = "Skip",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.secondary
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "Profile setup",
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "How would you like to appear when making calls on Line Call?",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.secondary
      )

      Spacer(modifier = Modifier.height(32.dp))

      // Profile avatar placeholder
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier
            .size(90.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
          contentAlignment = Alignment.Center
        ) {
          if (displayName.isNotBlank()) {
            Text(
              text = displayName.take(2).uppercase(),
              style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          } else {
            Icon(
              imageVector = Icons.Default.Person,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(46.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "Profile Picture (Optional)",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.secondary
        )
      }

      Spacer(modifier = Modifier.height(32.dp))

      OutlinedTextField(
        value = displayName,
        onValueChange = onDisplayNameChanged,
        placeholder = { Text("e.g. Alex Morgan") },
        label = { Text("Display Name") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("display_name_input"),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.onBackground,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
      )

      Spacer(modifier = Modifier.weight(1f))

      Button(
        onClick = onComplete,
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("complete_profile_button"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFF111111),
          contentColor = Color.White
        )
      ) {
        Text(
          text = "Complete Setup",
          style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}
