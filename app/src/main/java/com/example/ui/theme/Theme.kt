package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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

private val LineCallShapes = Shapes(
  extraSmall = RoundedCornerShape(8.dp),
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(18.dp),
  large = RoundedCornerShape(24.dp),
  extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun LineCallTheme(
  darkTheme: Boolean = true,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = DarkColorScheme,
    typography = Typography,
    shapes = LineCallShapes,
    content = content
  )
}
