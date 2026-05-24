package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// KotNest High-Contrast Premium Design System Colors
// Defined here as standalone constants for compatibility with existing code where needed.

// Light Theme Palette
val LightBackground = Color(0xFFF5FBFF)
val LightSurface = Color(0xFFFFFFFF)
val LightElevatedCard = Color(0xFFFFFFFF)
val LightPrimaryAqua = Color(0xFF00AEEF)
val LightDeepAqua = Color(0xFF0077B6)
val LightCyanAccent = Color(0xFF48CAE4)
val LightBorder = Color(0xFFD6EEF8)
val LightPrimaryText = Color(0xFF082032)
val LightSecondaryText = Color(0xFF516A7A)
val LightMutedText = Color(0xFF78909C)

val LightSuccess = Color(0xFF12B76A)
val LightWarning = Color(0xFFF79009)
val LightDanger = Color(0xFFF04438)
val LightPaidBackground = Color(0xFFE9FBEF)
val LightDueTodayBackground = Color(0xFFFFF4E5)
val LightOverdueBackground = Color(0xFFFEECEC)
val LightUpcomingBackground = Color(0xFFE6F7FF)

// Dark Theme Palette
val DarkBackground = Color(0xFF000000)
val DarkBackgroundGradientStart = Color(0xFF000000)
val DarkBackgroundGradientEnd = Color(0xFF000000)
val DarkSurface = Color(0xFF050505)
val DarkElevatedCard = Color(0xFF0A0A0A)
val DarkPrimaryAqua = Color(0xFF00CFE8)
val DarkDeepAqua = Color(0xFF00A9C0)
val DarkCyanGlow = Color(0xFF67E8F9)
val DarkCardBorder = Color(0xFF00CFE8)
val DarkGlassCard = Color(0xFF090909)

val DarkPrimaryText = Color(0xFFF4FBFF)
val DarkSecondaryText = Color(0xFFD7F5FF)
val DarkMutedText = Color(0xFFA4DDE8)

val DarkSuccess = Color(0xFF22C55E)
val DarkWarning = Color(0xFFF59E0B)
val DarkDanger = Color(0xFFEF4444)
val DarkPaidBackground = Color(0xFF072012)
val DarkDueTodayBackground = Color(0xFF231806)
val DarkOverdueBackground = Color(0xFF280808)
val DarkUpcomingBackground = Color(0xFF062229)

// Base Fallback Legacy Constants (To avoid any unresolved reference bugs during migration)
val KotNestPrimaryNavy = DarkBackground
val KotNestDeepBlue = DarkSurface
val KotNestElectricBlue = DarkPrimaryAqua
val KotNestCyanGlow = DarkCyanGlow
val KotNestGlassWhite = Color(0x1BFFFFFF)
val KotNestGlassBorder = DarkCardBorder
val KotNestSuccessGreen = DarkSuccess
val KotNestWarningAmber = DarkWarning
val KotNestDueOrange = DarkWarning
val KotNestDangerRed = DarkDanger
val KotNestTextWhite = DarkPrimaryText
val KotNestTextMuted = DarkSecondaryText
val KotNestDarkSurface = DarkSurface

// Legacy mapping system tokens to avoid compiler errors on other screens if referenced
val SophisticatedBackground = DarkBackground
val SophisticatedOnBackground = DarkPrimaryText
val SophisticatedSurface = DarkSurface
val SophisticatedOnSurface = DarkPrimaryText
val SophisticatedSurfaceVariant = DarkElevatedCard
val SophisticatedOnSurfaceVariant = DarkSecondaryText
val SophisticatedPrimary = DarkPrimaryAqua
val SophisticatedOnPrimary = Color(0xFFFFFFFF)
val SophisticatedPrimaryContainer = DarkSurface
val SophisticatedOnPrimaryContainer = DarkPrimaryText
val SophisticatedOverdue = DarkDanger
val SophisticatedDueToday = DarkWarning
val SophisticatedPaid = DarkSuccess
val SophisticatedUpcoming = DarkPrimaryAqua
val SophisticatedBorder = DarkCardBorder
