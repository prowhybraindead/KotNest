package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 1. Dynamic KotNest Custom Theme Token Structure
data class KotNestColors(
    val isLight: Boolean,
    val background: Color,
    val backgroundGradientStart: Color,
    val backgroundGradientEnd: Color,
    val surface: Color,
    val elevatedCard: Color,
    val primaryAqua: Color,
    val deepAqua: Color,
    val cyanAccent: Color,
    val border: Color,
    val glassCard: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val paidBackground: Color,
    val dueTodayBackground: Color,
    val overdueBackground: Color,
    val upcomingBackground: Color,
    val glassWhite: Color,
    val glassBorder: Color
)

val LocalKotNestColors = staticCompositionLocalOf<KotNestColors> {
    error("No KotNestColors provided for theme setup")
}

// 2. Standard Material 3 Light and Dark Colors mapping
private val KotNestDarkColorScheme = darkColorScheme(
    primary = DarkPrimaryAqua,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = DarkSurface,
    onPrimaryContainer = DarkPrimaryText,
    secondary = DarkDeepAqua,
    onSecondary = Color(0xFFFFFFFF),
    background = DarkBackground,
    onBackground = DarkPrimaryText,
    surface = DarkSurface,
    onSurface = DarkPrimaryText,
    surfaceVariant = DarkElevatedCard,
    onSurfaceVariant = DarkSecondaryText,
    outline = DarkCardBorder,
    error = DarkDanger
)

private val KotNestLightColorScheme = lightColorScheme(
    primary = LightPrimaryAqua,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = LightSurface,
    onPrimaryContainer = LightPrimaryText,
    secondary = LightDeepAqua,
    onSecondary = Color(0xFFFFFFFF),
    background = LightBackground,
    onBackground = LightPrimaryText,
    surface = LightSurface,
    onSurface = LightPrimaryText,
    surfaceVariant = LightSurface,
    onSurfaceVariant = LightSecondaryText,
    outline = LightBorder,
    error = LightDanger
)

@Composable
fun MyApplicationTheme(
    themeMode: String = "System",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        "Light" -> false
        "Dark" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> KotNestDarkColorScheme
        else -> KotNestLightColorScheme
    }

    // Initialize custom design tokens matching exact requirements for UI contrast & separation
    val customColors = if (darkTheme) {
        KotNestColors(
            isLight = false,
            background = DarkBackground,
            backgroundGradientStart = DarkBackgroundGradientStart,
            backgroundGradientEnd = DarkBackgroundGradientEnd,
            surface = DarkSurface,
            elevatedCard = DarkElevatedCard,
            primaryAqua = DarkPrimaryAqua,
            deepAqua = DarkDeepAqua,
            cyanAccent = DarkCyanGlow,
            border = DarkCardBorder,
            glassCard = DarkGlassCard,
            primaryText = DarkPrimaryText,
            secondaryText = DarkSecondaryText,
            mutedText = DarkMutedText,
            success = DarkSuccess,
            warning = DarkWarning,
            danger = DarkDanger,
            paidBackground = DarkPaidBackground,
            dueTodayBackground = DarkDueTodayBackground,
            overdueBackground = DarkOverdueBackground,
            upcomingBackground = DarkUpcomingBackground,
            glassWhite = Color(0xFF070707),
            glassBorder = DarkCardBorder
        )
    } else {
        KotNestColors(
            isLight = true,
            background = LightBackground,
            backgroundGradientStart = LightBackground,
            backgroundGradientEnd = Color(0xFFE6F7FF),
            surface = LightSurface,
            elevatedCard = LightSurface,
            primaryAqua = LightPrimaryAqua,
            deepAqua = LightDeepAqua,
            cyanAccent = LightCyanAccent,
            border = LightBorder,
            glassCard = Color(0xE1FFFFFF), // 88% transparent white glass for premium contrast
            primaryText = LightPrimaryText,
            secondaryText = LightSecondaryText,
            mutedText = LightMutedText,
            success = LightSuccess,
            warning = LightWarning,
            danger = LightDanger,
            paidBackground = LightPaidBackground,
            dueTodayBackground = LightDueTodayBackground,
            overdueBackground = LightOverdueBackground,
            upcomingBackground = LightUpcomingBackground,
            glassWhite = Color(0x120077B6), // premium pale aqua translucent sheet
            glassBorder = Color(0x240077B6)  // beautiful defined outer border
        )
    }

    CompositionLocalProvider(LocalKotNestColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
