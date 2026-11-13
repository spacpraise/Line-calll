package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// One UI Style Palette - Minimalist Samsung Phone Dialer Design Language
val CallGreen = Color(0xFF22C55E) // Samsung One UI vibrant call green
val CallGreenPressed = Color(0xFF16A34A)
val CallGreenLight = Color(0xFFDCFCE7)
val CallGreenContainer = Color(0xFF14532D)

val CallRed = Color(0xFFEF4444)
val CallRedPressed = Color(0xFFDC2626)
val CallRedContainer = Color(0xFF7F1D1D)

val NotificationOrange = Color(0xFFFF6433) // Orange dot badge on More menu

// One UI Dark Theme (Primary aesthetic as per user screenshots)
val OneUiDarkBackground = Color(0xFF0B0C0E)
val OneUiCardBackground = Color(0xFF1B1C20)
val OneUiCardBorder = Color(0xFF26282E)
val OneUiCardElevated = Color(0xFF25272E)
val OneUiNavPill = Color(0xFF23252B)
val OneUiNavActivePill = Color(0xFF383B44)

val OneUiTextPrimary = Color(0xFFFFFFFF)
val OneUiTextSecondary = Color(0xFF8E929B)
val OneUiTextMuted = Color(0xFF6B7280)

// Favourites Card Pastel Gradients (Matching user's Favourites cards in screenshots)
val PastelLilac = listOf(Color(0xFF9E8EFF), Color(0xFFBCAEFF))
val PastelMint = listOf(Color(0xFF66E2B4), Color(0xFF8FF5CE))
val PastelPeach = listOf(Color(0xFFF79696), Color(0xFFFABEBE))
val PastelYellow = listOf(Color(0xFFF6D268), Color(0xFFFDE496))
val PastelPink = listOf(Color(0xFFF894BC), Color(0xFFFDB9D4))
val PastelBlue = listOf(Color(0xFF77BDF6), Color(0xFFA5D6FA))

fun getFavoriteGradient(index: Int): Brush {
  val palette = listOf(PastelLilac, PastelMint, PastelPeach, PastelYellow, PastelPink, PastelBlue)
  val colors = palette[index % palette.size]
  return Brush.verticalGradient(colors)
}

// Light theme fallbacks
val LightBackground = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFF3F4F6)
val LightSurfaceVariant = Color(0xFFE5E7EB)
val LightBorder = Color(0xFFE5E7EB)
val LightTextPrimary = Color(0xFF111827)
val LightTextSecondary = Color(0xFF6B7280)
val LightTextMuted = Color(0xFF9CA3AF)
