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
val DarkBackground = Color(0xFF020B12)
val DarkBackgroundGradientStart = Color(0xFF020B12)
val DarkBackgroundGradientEnd = Color(0xFF06283D)
val DarkSurface = Color(0xFF0B1F2A)
val DarkElevatedCard = Color(0xFF102D3A)
val DarkPrimaryAqua = Color(0xFF00B4D8)
val DarkDeepAqua = Color(0xFF0077B6)
val DarkCyanGlow = Color(0xFF48CAE4)
val DarkCardBorder = Color(0x3848CAE4) // rgba(72, 202, 228, 0.22)
val DarkGlassCard = Color(0xD1102D3A)  // rgba(16, 45, 58, 0.82)

val DarkPrimaryText = Color(0xFFF4FBFF)
val DarkSecondaryText = Color(0xFFB6D3E0)
val DarkMutedText = Color(0xFF7FA7B8)

val DarkSuccess = Color(0xFF22C55E)
val DarkWarning = Color(0xFFF59E0B)
val DarkDanger = Color(0xFFEF4444)
val DarkPaidBackground = Color(0x2322C55E) // rgba(34, 197, 94, 0.14)
val DarkDueTodayBackground = Color(0x28F59E0B) // rgba(245, 158, 11, 0.16)
val DarkOverdueBackground = Color(0x28EF4444) // rgba(239, 68, 68, 0.16)
val DarkUpcomingBackground = Color(0x2800B4D8) // rgba(0, 180, 216, 0.16)

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
