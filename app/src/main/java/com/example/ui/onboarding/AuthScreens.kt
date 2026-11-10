package com.example.ui.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CallRed

@Composable
fun LoginScreen(
  identifier: String,
  displayName: String,
  avatarUrl: String?,
  password: String,
  error: String?,
  busy: Boolean,
  onPasswordChanged: (String) -> Unit,
  onLogin: () -> Unit,
  onSwitchAccount: () -> Unit,
  onForgotPassword: () -> Unit
) {
  val focusManager = LocalFocusManager.current
  var passwordVisible by remember { mutableStateOf(false) }
  val maskedEmail = remember(identifier) {
    val parts = identifier.trim().split("@", limit = 2)
    if (parts.size != 2) identifier
    else (if (parts[0].length <= 2) parts[0].take(1) + "•••" else parts[0].take(2) + "••••") + "@" + parts[1]
  }
  Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(
      Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)
        .padding(horizontal = 26.dp, vertical = 18.dp).verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        IconButton(onClick = onSwitchAccount, modifier = Modifier.testTag("login_switch_back")) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
        }
      }
      Spacer(Modifier.height(34.dp))
      Box(Modifier.size(112.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        if (!avatarUrl.isNullOrBlank()) {
          AsyncImage(model = avatarUrl, contentDescription = "Profile picture", contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(CircleShape))
        } else {
          Icon(Icons.Rounded.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(58.dp))
        }
      }
      Spacer(Modifier.height(22.dp))
      Text("Log in to your account", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 27.sp), color = MaterialTheme.colorScheme.onSurface)
      Spacer(Modifier.height(8.dp))
      Text(displayName.ifBlank { identifier.substringBefore("@").replaceFirstChar { it.uppercase() } }, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
      Spacer(Modifier.height(5.dp))
      Text(maskedEmail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
      Spacer(Modifier.height(34.dp))
      Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Password", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
          OutlinedTextField(
            value = password, onValueChange = onPasswordChanged,
            modifier = Modifier.fillMaxWidth().testTag("login_password_input"), singleLine = true,
            placeholder = { Text("Enter your password", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) {
              Icon(if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                contentDescription = if (passwordVisible) "Hide password" else "Show password", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            } },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(force = true); if (password.isNotBlank() && !busy) onLogin() }),
            shape = RoundedCornerShape(15.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CallGreen, unfocusedBorderColor = MaterialTheme.colorScheme.outline,
              focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface, cursorColor = CallGreen)
          )
          TextButton(onClick = { focusManager.clearFocus(force = true); onForgotPassword() },
            modifier = Modifier.align(Alignment.End).testTag("forgot_password_button")) {
            Text("Forgot password?", color = CallGreen, fontWeight = FontWeight.SemiBold)
          }
        }
      }
      if (error != null) {
        Spacer(Modifier.height(12.dp))
        Text(error, color = CallRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
      }
      Spacer(Modifier.height(20.dp))
      Button(onClick = { focusManager.clearFocus(force = true); onLogin() },
        enabled = !busy && password.isNotBlank(),
        modifier = Modifier.fillMaxWidth().height(56.dp).testTag("login_submit_btn"),
        shape = RoundedCornerShape(17.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CallGreen, contentColor = Color.White,
          disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant, disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
        if (busy) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        else Text("Continue", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
      }
      Spacer(Modifier.height(16.dp))
      TextButton(onClick = onSwitchAccount, modifier = Modifier.testTag("switch_account_button")) {
        Text("Switch account", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
      }
    }
  }
}


@Composable
fun ForgotPasswordScreen(
  email: String,
  phoneNumber: String,
  error: String?,
  busy: Boolean,
  onEmailChanged: (String) -> Unit,
  onPhoneChanged: (String) -> Unit,
  onSubmit: () -> Unit,
  onBack: () -> Unit
) {
  val focusManager = LocalFocusManager.current
  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(
      modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)
        .padding(horizontal = 24.dp, vertical = 16.dp).verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      IconButton(onClick = onBack, modifier = Modifier.testTag("forgot_password_back")) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
      }
      Text("Reset your password", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
      Text(
        "Enter both the email address and phone number registered to your Line Call account. Both must match before you can choose a new password.",
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      OutlinedTextField(
        value = email, onValueChange = onEmailChanged, modifier = Modifier.fillMaxWidth().testTag("recovery_email_input"),
        label = { Text("Registered email address") }, placeholder = { Text("name@example.com") },
        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) }, shape = RoundedCornerShape(14.dp)
      )
      OutlinedTextField(
        value = phoneNumber, onValueChange = onPhoneChanged, modifier = Modifier.fillMaxWidth().testTag("recovery_phone_input"),
        label = { Text("Registered phone number") }, placeholder = { Text("+2348012345678") },
        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
        leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) }, shape = RoundedCornerShape(14.dp),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(force = true); if (!busy) onSubmit() })
      )
      if (error != null) Text(error, color = CallRed, style = MaterialTheme.typography.bodySmall)
      Button(
        onClick = { focusManager.clearFocus(force = true); onSubmit() },
        enabled = !busy && email.contains("@") && phoneNumber.trim().startsWith("+"),
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("recovery_continue_button"),
        shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = CallGreen)
      ) {
        if (busy) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        else Text("Verify details", fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
fun NewPasswordScreen(
  password: String,
  confirmPassword: String,
  error: String?,
  busy: Boolean,
  onPasswordChanged: (String) -> Unit,
  onConfirmPasswordChanged: (String) -> Unit,
  onSubmit: () -> Unit
) {
  val focusManager = LocalFocusManager.current
  var passwordVisible by remember { mutableStateOf(false) }
  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(
      modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)
        .padding(horizontal = 24.dp, vertical = 16.dp).verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Text("Create a new password", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
      Text("Your email and phone number matched. Choose a new password to finish resetting your account.", color = MaterialTheme.colorScheme.onSurfaceVariant)
      OutlinedTextField(
        value = password, onValueChange = onPasswordChanged, modifier = Modifier.fillMaxWidth().testTag("new_password_input"),
        label = { Text("New password (at least 8 characters)") }, singleLine = true,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) {
          Icon(if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, contentDescription = "Toggle password visibility")
        } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
        shape = RoundedCornerShape(14.dp)
      )
      OutlinedTextField(
        value = confirmPassword, onValueChange = onConfirmPasswordChanged, modifier = Modifier.fillMaxWidth().testTag("confirm_new_password_input"),
        label = { Text("Confirm new password") }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(force = true); if (!busy) onSubmit() }),
        shape = RoundedCornerShape(14.dp)
      )
      if (error != null) Text(error, color = CallRed, style = MaterialTheme.typography.bodySmall)
      Button(
        onClick = { focusManager.clearFocus(force = true); onSubmit() },
        enabled = !busy && password.length >= 8 && confirmPassword.isNotEmpty(),
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_new_password_button"),
        shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = CallGreen)
      ) {
        if (busy) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        else Text("Save new password", fontWeight = FontWeight.Bold)
      }
    }
  }
}
