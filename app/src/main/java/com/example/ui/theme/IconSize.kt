package com.example.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Shared icon sizes so every screen reaches for the same scale instead of each
 * file hand-picking its own close-but-not-quite value (16dp here, 18dp there,
 * 22dp somewhere else for what is conceptually the same role). Inconsistent
 * icon sizing across screens is a large part of why the app read as a set of
 * independently-styled pages rather than one coherent product.
 *
 * - Inline: a small icon sitting inside a row of text (status dots, trailing
 *   icons next to a label, the call-direction glyph in a Recents row).
 * - Standard: the default size for a tappable icon inside an IconButton
 *   (top-bar actions, nav bar icons).
 * - Prominent: a deliberately larger icon that is itself the focal point of a
 *   control (the call/decline icons in the big circular call-screen buttons).
 */
object IconSize {
  val Inline = 16.dp
  val Standard = 22.dp
  val Prominent = 32.dp
}
