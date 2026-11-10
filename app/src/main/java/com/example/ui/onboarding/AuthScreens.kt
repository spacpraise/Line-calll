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
import com.example.ui.theme.OneUiCardBackground
import com.example.ui.theme.OneUiCardBorder
import com.example.ui.theme.OneUiCardElevated
import com.example.ui.theme.OneUiDarkBackground
import com.example.ui.theme.OneUiTextMuted
import com.example.ui.theme.OneUiTextPrimary
import com.example.ui.theme.OneUiTextSecondary

@Composable
fun LoginScreen(
  identifier: String,
  password: String,
  error: String?,
  notice: String? = null,
  busy: Boolean,
  onIdentifierChanged: (String) -> Unit,
  onPasswordChanged: (String) -> Unit,
  onLogin: () -> Unit,
  onBack: () -> Unit,
  onRegister: () -> Unit,
  onForgotPassword: () -> Unit
) {
  val focusManager = LocalFocusManager.current
  var passwordVisible by remember { mutableStateOf(false) }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = OneUiDarkBackground
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)
        .padding(horizontal = 24.dp, vertical = 16.dp)
        .verticalScroll(rememberScrollState())
    ) {
      // Top row: Back to Welcome
      IconButton(
        onClick = {
          focusManager.clearFocus(force = true)
          onBack()
        },
        modifier = Modifier.testTag("login_back_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = "Back",
          tint = OneUiTextPrimary,
          modifier = Modifier.size(24.dp)
        )
      }

      Spacer(Modifier.height(18.dp))

      Text(
        text = "Welcome back",
        style = MaterialTheme.typography.headlineMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 28.sp
        ),
        color = OneUiTextPrimary
      )

      Spacer(Modifier.height(8.dp))

      Text(
        text = "Log in with your registered email address or international phone number and password.",
        style = MaterialTheme.typography.bodyMedium,
        color = OneUiTextSecondary
      )

      if (notice != null) {
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = OneUiCardElevated), modifier = Modifier.fillMaxWidth()) {
          Text(notice, modifier = Modifier.padding(14.dp), color = OneUiTextPrimary, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(14.dp))
      }

      Spacer(Modifier.height(28.dp))

      Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = OneUiCardBackground),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          // Email or phone number field
          OutlinedTextField(
            value = identifier,
            onValueChange = onIdentifierChanged,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("login_email_input"),
            singleLine = true,
            label = { Text("Email or phone number", color = OneUiTextSecondary) },
            placeholder = { Text("name@example.com or +2348012345678", color = OneUiTextMuted) },
            leadingIcon = {
              Icon(imageVector = Icons.Rounded.Email, contentDescription = null, tint = OneUiTextSecondary)
            },
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Text,
              imeAction = ImeAction.Next
            ),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = CallGreen,
              unfocusedBorderColor = OneUiCardBorder,
              focusedTextColor = OneUiTextPrimary,
              unfocusedTextColor = OneUiTextPrimary,
              cursorColor = CallGreen
            )
          )

          // Password field
          OutlinedTextField(
            value = password,
            onValueChange = onPasswordChanged,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("login_password_input"),
            singleLine = true,
            label = { Text("Password", color = OneUiTextSecondary) },
            leadingIcon = {
              Icon(imageVector = Icons.Rounded.Lock, contentDescription = null, tint = OneUiTextSecondary)
            },
            trailingIcon = {
              IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                  imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                  contentDescription = if (passwordVisible) "Hide password" else "Show password",
                  tint = OneUiTextSecondary
                )
              }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Password,
              imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
              onDone = {
                focusManager.clearFocus(force = true)
                if (identifier.isNotBlank() && password.isNotBlank() && !busy) onLogin()
              }
            ),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = CallGreen,
              unfocusedBorderColor = OneUiCardBorder,
              focusedTextColor = OneUiTextPrimary,
              unfocusedTextColor = OneUiTextPrimary,
              cursorColor = CallGreen
            )
          )
        }
      }


      if (error != null) {
        Spacer(Modifier.height(4.dp))
        Text(
          text = error,
          color = CallRed,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(horizontal = 4.dp)
        )
      }

      Spacer(Modifier.height(20.dp))

      // Main Log In Button
      Button(
        onClick = {
          focusManager.clearFocus(force = true)
          onLogin()
        },
        enabled = !busy && identifier.isNotBlank() && password.isNotBlank(),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("login_submit_btn"),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = CallGreen,
          contentColor = Color.White,
          disabledContainerColor = OneUiCardElevated,
          disabledContentColor = OneUiTextMuted
        )
      ) {
        if (busy) {
          CircularProgressIndicator(
            color = Color.White,
            modifier = Modifier.size(22.dp),
            strokeWidth = 2.dp
          )
        } else {
          Text("Log In", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
        }
      }

      Spacer(Modifier.height(16.dp))

      TextButton(
        onClick = {
          focusManager.clearFocus(force = true)
          onForgotPassword()
        },
        modifier = Modifier.fillMaxWidth().testTag("forgot_password_button")
      ) {
        Text("Forgot password?", color = CallGreen, fontWeight = FontWeight.SemiBold)
      }

      TextButton(
        onClick = {
          focusManager.clearFocus(force = true)
          onRegister()
        },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("login_create_account_btn")
      ) {
        Text(
          text = "Don't have an account? Create one",
          color = CallGreen,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        )
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
  Surface(modifier = Modifier.fillMaxSize(), color = OneUiDarkBackground) {
    Column(
      modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)
        .padding(horizontal = 24.dp, vertical = 16.dp).verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      IconButton(onClick = onBack, modifier = Modifier.testTag("forgot_password_back")) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = OneUiTextPrimary)
      }
      Text("Reset your password", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = OneUiTextPrimary)
      Text(
        "Enter both the email address and phone number registered to your Line Call account. Both must match before you can choose a new password.",
        style = MaterialTheme.typography.bodyMedium, color = OneUiTextSecondary
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
  Surface(modifier = Modifier.fillMaxSize(), color = OneUiDarkBackground) {
    Column(
      modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)
        .padding(horizontal = 24.dp, vertical = 16.dp).verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Text("Create a new password", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = OneUiTextPrimary)
      Text("Your email and phone number matched. Choose a new password to finish resetting your account.", color = OneUiTextSecondary)
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
