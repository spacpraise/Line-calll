package com.example.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

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
  Column(
    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
    verticalArrangement = Arrangement.Center
  ) {
    Text("Welcome back", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text("Log in with your email address or Line Call phone number.", color = MaterialTheme.colorScheme.secondary)
    Spacer(Modifier.height(24.dp))
    OutlinedTextField(
      value = identifier, onValueChange = onIdentifierChanged,
      modifier = Modifier.fillMaxWidth(), singleLine = true,
      label = { Text("Email or phone number") },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
      value = password, onValueChange = onPasswordChanged,
      modifier = Modifier.fillMaxWidth(), singleLine = true,
      label = { Text("Password") },
      visualTransformation = PasswordVisualTransformation(),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    )
    TextButton(onClick = onForgotPassword, modifier = Modifier.fillMaxWidth()) { Text("Forgot password?") }
    if (error != null) {
      Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
      Spacer(Modifier.height(8.dp))
    }
    Button(
      onClick = onLogin, enabled = !busy && identifier.isNotBlank() && password.isNotBlank(),
      modifier = Modifier.fillMaxWidth().height(52.dp),
      colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF111111))
    ) { Text(if (busy) "Logging in..." else "Log In", color = androidx.compose.ui.graphics.Color.White) }
    TextButton(onClick = onRegister, modifier = Modifier.fillMaxWidth()) { Text("Create an account") }
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
  Column(
    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
    verticalArrangement = Arrangement.Center
  ) {
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(subtitle, color = MaterialTheme.colorScheme.secondary)
    Spacer(Modifier.height(24.dp))
    OutlinedTextField(
      value = password, onValueChange = onPasswordChanged,
      modifier = Modifier.fillMaxWidth(), singleLine = true,
      label = { Text("Create password") },
      visualTransformation = PasswordVisualTransformation(),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
      value = confirmPassword, onValueChange = onConfirmPasswordChanged,
      modifier = Modifier.fillMaxWidth(), singleLine = true,
      label = { Text("Confirm password") },
      visualTransformation = PasswordVisualTransformation(),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    )
    if (error != null) {
      Spacer(Modifier.height(10.dp))
      Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(24.dp))
    Button(
      onClick = onSave, enabled = !busy && password.length >= 8 && confirmPassword.isNotBlank(),
      modifier = Modifier.fillMaxWidth().height(52.dp),
      colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF111111))
    ) { Text(if (busy) "Saving..." else "Save password", color = androidx.compose.ui.graphics.Color.White) }
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
  Column(
    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
    verticalArrangement = Arrangement.Center
  ) {
    Text(if (sent) "Check your email" else "Reset password", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(
      if (sent) "We sent a secure password reset link. Open it on this device to choose a new password."
      else "Enter the email address linked to your Line Call account.",
      color = MaterialTheme.colorScheme.secondary
    )
    if (!sent) {
      Spacer(Modifier.height(24.dp))
      OutlinedTextField(
        value = email, onValueChange = onEmailChanged,
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        label = { Text("Email address") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
      )
      if (error != null) {
        Spacer(Modifier.height(8.dp))
        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
      }
      Spacer(Modifier.height(20.dp))
      Button(onClick = onSend, enabled = !busy && email.contains("@"), modifier = Modifier.fillMaxWidth().height(52.dp)) {
        Text(if (busy) "Sending..." else "Send reset link")
      }
    }
    Spacer(Modifier.height(12.dp))
    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to log in") }
  }
}
