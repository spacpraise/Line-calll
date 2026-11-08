package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = CallGreen,
  onPrimary = Color.White,
  primaryContainer = OneUiCardElevated,
  onPrimaryContainer = Color.White,
  secondary = OneUiTextSecondary,
  onSecondary = Color.White,
  tertiary = CallRed,
  onTertiary = Color.White,
  background = OneUiDarkBackground,
  onBackground = OneUiTextPrimary,
  surface = OneUiCardBackground,
  onSurface = OneUiTextPrimary,
  surfaceVariant = OneUiCardElevated,
  onSurfaceVariant = OneUiTextSecondary,
  outline = OneUiCardBorder,
  error = CallRed,
  onError = Color.White
)

@Composable
fun LineCallTheme(
  darkTheme: Boolean = true, // Default to One UI sleek dark aesthetic
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = DarkColorScheme,
    typography = Typography,
    content = content
  )
}
