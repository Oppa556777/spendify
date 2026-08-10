package com.myexpense.tracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.myexpense.tracker.data.model.Accent
import com.myexpense.tracker.data.model.FontSize
import com.myexpense.tracker.data.model.ThemeMode

/** Applies an accent's brand color to the primary/secondary/tertiary slots. */
private fun accentColors(accent: Accent, dark: Boolean): Triple<Color, Color, Color> {
    val base = Color(accent.color)
    val secondary = if (dark) base.copy(alpha = 0.8f) else base.copy(alpha = 0.85f)
    val tertiary = if (dark) Color(0xFFA0CFCB) else Color(0xFF3A6462)
    return Triple(base, secondary, tertiary)
}

private fun lightScheme(accent: Accent): androidx.compose.material3.ColorScheme {
    val (primary, secondary, tertiary) = accentColors(accent, false)
    return lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE8DEF8),
        onPrimaryContainer = Color(0xFF21005D),
        secondary = secondary,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE8DEF8),
        onSecondaryContainer = Color(0xFF1D192B),
        tertiary = tertiary,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFFD8E4),
        onTertiaryContainer = Color(0xFF31111D),
        background = LightBackground,
        onBackground = LightOnBackground,
        surface = LightSurface,
        onSurface = LightOnSurface,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightOnSurfaceVariant,
        surfaceContainer = LightSurfaceContainer,
        surfaceContainerHigh = LightSurfaceContainerHigh,
        surfaceContainerHighest = LightSurfaceContainerHighest,
        outline = LightOutline,
        outlineVariant = LightOutlineVariant,
        error = LightError,
        onError = LightOnError,
        errorContainer = LightErrorContainer,
        onErrorContainer = LightOnErrorContainer,
    )
}

private fun darkScheme(accent: Accent, amoled: Boolean): androidx.compose.material3.ColorScheme {
    val (primary, secondary, tertiary) = accentColors(accent, true)
    val bg = if (amoled) Color.Black else DarkBackground
    val surface = if (amoled) Color.Black else DarkSurface
    val container = if (amoled) Color(0xFF111111) else DarkSurfaceContainer
    val containerHigh = if (amoled) Color(0xFF1A1A1A) else DarkSurfaceContainerHigh
    val containerHighest = if (amoled) Color(0xFF242424) else DarkSurfaceContainerHighest
    return darkColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = DarkPrimaryContainer,
        onPrimaryContainer = DarkOnPrimaryContainer,
        secondary = secondary,
        onSecondary = Color(0xFF2A2A2A),
        secondaryContainer = DarkSecondaryContainer,
        onSecondaryContainer = DarkOnSecondaryContainer,
        tertiary = tertiary,
        onTertiary = Color(0xFF003735),
        tertiaryContainer = DarkTertiaryContainer,
        onTertiaryContainer = DarkOnTertiaryContainer,
        background = bg,
        onBackground = DarkOnBackground,
        surface = surface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkOnSurfaceVariant,
        surfaceContainer = container,
        surfaceContainerHigh = containerHigh,
        surfaceContainerHighest = containerHighest,
        outline = DarkOutline,
        outlineVariant = DarkOutlineVariant,
        error = DarkError,
        onError = DarkOnError,
        errorContainer = DarkErrorContainer,
        onErrorContainer = DarkOnErrorContainer,
    )
}

@Composable
fun MoneyMateTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColors: Boolean = true,
    accent: Accent = Accent.PURPLE,
    fontSize: FontSize = FontSize.MEDIUM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AMOLED -> true
    }

    val colorScheme = when {
        dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && themeMode != ThemeMode.AMOLED -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkScheme(accent, themeMode == ThemeMode.AMOLED)
        else -> lightScheme(accent)
    }

    val typography = if (fontSize == FontSize.MEDIUM) {
        MoneyMateTypography
    } else {
        scaleTypography(MoneyMateTypography, fontSize.scale)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = MoneyMateShapes,
        content = content,
    )
}
