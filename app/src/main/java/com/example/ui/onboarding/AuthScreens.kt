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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
  busy: Boolean,
  onIdentifierChanged: (String) -> Unit,
  onPasswordChanged: (String) -> Unit,
  onLogin: () -> Unit,
  onForgotPassword: () -> Unit,
  onRegister: () -> Unit
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
          onRegister()
        },
        modifier = Modifier.testTag("login_back_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
        text = "Log in with your registered email address and password to start internet voice calling.",
        style = MaterialTheme.typography.bodyMedium,
        color = OneUiTextSecondary
      )

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
          // Email field
          OutlinedTextField(
            value = identifier,
            onValueChange = onIdentifierChanged,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("login_email_input"),
            singleLine = true,
            label = { Text("Email address", color = OneUiTextSecondary) },
            leadingIcon = {
              Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = OneUiTextSecondary)
            },
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Email,
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
              Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = OneUiTextSecondary)
            },
            trailingIcon = {
              IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                  imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
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

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        TextButton(
          onClick = {
            focusManager.clearFocus(force = true)
            onForgotPassword()
          },
          modifier = Modifier.testTag("forgot_password_btn")
        ) {
          Text(
            text = "Forgot password?",
            color = OneUiTextSecondary,
            style = MaterialTheme.typography.labelLarge
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
fun PasswordSetupScreen(
  title: String,
  subtitle: String,
  password: String,
  confirmPassword: String,
  error: String?,
  busy: Boolean,
  onPasswordChanged: (String) -> Unit,
  onConfirmPasswordChanged: (String) -> Unit,
  onSave: () -> Unit
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
        .padding(horizontal = 24.dp, vertical = 24.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 26.sp),
        color = OneUiTextPrimary
      )

      Spacer(Modifier.height(8.dp))

      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = OneUiTextSecondary
      )

      Spacer(Modifier.height(24.dp))

      Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = OneUiCardBackground),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          OutlinedTextField(
            value = password,
            onValueChange = onPasswordChanged,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("create_password_input"),
            singleLine = true,
            label = { Text("Create password (min 8 chars)", color = OneUiTextSecondary) },
            leadingIcon = {
              Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = OneUiTextSecondary)
            },
            trailingIcon = {
              IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                  imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = null,
                  tint = OneUiTextSecondary
                )
              }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Password,
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

          OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChanged,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("confirm_password_input"),
            singleLine = true,
            label = { Text("Confirm password", color = OneUiTextSecondary) },
            leadingIcon = {
              Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = OneUiTextSecondary)
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Password,
              imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
              onDone = {
                focusManager.clearFocus(force = true)
                if (password.length >= 8 && confirmPassword.isNotBlank() && !busy) onSave()
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
        Spacer(Modifier.height(10.dp))
        Text(
          text = error,
          color = CallRed,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(horizontal = 4.dp)
        )
      }

      Spacer(Modifier.height(24.dp))

      Button(
        onClick = {
          focusManager.clearFocus(force = true)
          onSave()
        },
        enabled = !busy && password.length >= 8 && confirmPassword.isNotBlank(),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("save_password_btn"),
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
          Text("Save Password", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
        }
      }
    }
  }
}

@Composable
fun ForgotPasswordScreen(
  email: String,
  sent: Boolean,
  error: String?,
  busy: Boolean,
  onEmailChanged: (String) -> Unit,
  onSend: () -> Unit,
  onBack: () -> Unit
) {
  val focusManager = LocalFocusManager.current

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
      IconButton(
        onClick = {
          focusManager.clearFocus(force = true)
          onBack()
        },
        modifier = Modifier.testTag("forgot_password_back")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = OneUiTextPrimary,
          modifier = Modifier.size(24.dp)
        )
      }

      Spacer(Modifier.height(20.dp))

      Text(
        text = if (sent) "Check your email" else "Reset password",
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp),
        color = OneUiTextPrimary
      )

      Spacer(Modifier.height(8.dp))

      Text(
        text = if (sent)
          "We sent a secure password reset link to $email. Open it on this device to set a new password."
        else "Enter the email address linked to your Line Call account.",
        style = MaterialTheme.typography.bodyMedium,
        color = OneUiTextSecondary
      )

      Spacer(Modifier.height(24.dp))

      if (!sent) {
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = OneUiCardBackground),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            OutlinedTextField(
              value = email,
              onValueChange = onEmailChanged,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("recovery_email_input"),
              singleLine = true,
              label = { Text("Email address", color = OneUiTextSecondary) },
              leadingIcon = {
                Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = OneUiTextSecondary)
              },
              keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
              ),
              keyboardActions = KeyboardActions(
                onDone = {
                  focusManager.clearFocus(force = true)
                  if (!busy && email.contains("@")) onSend()
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
          Spacer(Modifier.height(8.dp))
          Text(
            text = error,
            color = CallRed,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 4.dp)
          )
        }

        Spacer(Modifier.height(24.dp))

        Button(
          onClick = {
            focusManager.clearFocus(force = true)
            onSend()
          },
          enabled = !busy && email.contains("@"),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("send_recovery_btn"),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = CallGreen,
            contentColor = Color.White,
            disabledContainerColor = OneUiCardElevated,
            disabledContentColor = OneUiTextMuted
          )
        ) {
          if (busy) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
          } else {
            Text("Send Reset Link", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
          }
        }
      }

      Spacer(Modifier.height(16.dp))

      TextButton(
        onClick = {
          focusManager.clearFocus(force = true)
          onBack()
        },
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Back to Log In", color = OneUiTextSecondary)
      }
    }
  }
}
