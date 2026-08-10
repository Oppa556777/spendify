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

private fun lightScheme(accent: Accent): androidx.compose.material3.ColorScheme {
    val primary = Color(accent.color)
    return lightColorScheme(
        primary = primary,
        onPrimary = LightOnPrimary,
        primaryContainer = LightSurfaceVariant,
        onPrimaryContainer = LightOnBackground,
        secondary = LightSecondary,
        onSecondary = Color.Black,
        secondaryContainer = Color(0xFFCCFCF4),
        onSecondaryContainer = Color(0xFF00201B),
        tertiary = LightPrimaryVariant,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFECE9FF),
        onTertiaryContainer = LightOnBackground,
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
    val primary = if (amoled) DarkPrimary else Color(accent.color)
    val bg = if (amoled) Color.Black else DarkBackground
    val surface = if (amoled) Color.Black else DarkSurface
    val container = if (amoled) Color(0xFF101018) else DarkSurfaceContainer
    val containerHigh = if (amoled) Color(0xFF1A1A26) else DarkSurfaceContainerHigh
    val containerHighest = if (amoled) Color(0xFF242434) else DarkSurfaceContainerHighest
    return darkColorScheme(
        primary = primary,
        onPrimary = DarkOnPrimary,
        primaryContainer = DarkSurfaceVariant,
        onPrimaryContainer = DarkOnSurface,
        secondary = DarkSecondary,
        onSecondary = Color.Black,
        secondaryContainer = Color(0xFF004B41),
        onSecondaryContainer = Color(0xFFCCFCF4),
        tertiary = DarkPrimaryVariant,
        onTertiary = Color.Black,
        tertiaryContainer = Color(0xFF3A3590),
        onTertiaryContainer = Color(0xFFE3DFFF),
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
