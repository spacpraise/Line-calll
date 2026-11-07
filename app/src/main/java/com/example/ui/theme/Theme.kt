package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = CallGreen,
  onPrimary = Color.White,
  primaryContainer = CallGreenPressed,
  onPrimaryContainer = Color.White,
  secondary = DarkTextSecondary,
  onSecondary = DarkTextPrimary,
  tertiary = CallRed,
  onTertiary = Color.White,
  background = DarkBackground,
  onBackground = DarkTextPrimary,
  surface = DarkSurface,
  onSurface = DarkTextPrimary,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkTextSecondary,
  outline = DarkBorder,
  error = CallRed,
  onError = Color.White
)

private val LightColorScheme = lightColorScheme(
  primary = DarkTextPrimary, // Clean near-black primary for telephone UI
  onPrimary = Color.White,
  primaryContainer = CallGreen,
  onPrimaryContainer = Color.White,
  secondary = LightTextSecondary,
  onSecondary = LightTextPrimary,
  tertiary = CallRed,
  onTertiary = Color.White,
  background = LightBackground,
  onBackground = LightTextPrimary,
  surface = LightSurface,
  onSurface = LightTextPrimary,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = LightTextSecondary,
  outline = LightBorder,
  error = CallRed,
  onError = Color.White
)

@Composable
fun LineCallTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
