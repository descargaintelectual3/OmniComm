package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = TacticalCyanPrimary,
    onPrimary = TacticalCyanOnPrimary,
    primaryContainer = TacticalCyanContainer,
    onPrimaryContainer = TacticalCyanOnContainer,
    secondary = TacticalEmeraldSecondary,
    onSecondary = TacticalEmeraldOnSecondary,
    secondaryContainer = TacticalEmeraldContainer,
    onSecondaryContainer = TacticalEmeraldOnContainer,
    tertiary = TacticalAmberTertiary,
    onTertiary = TacticalAmberOnTertiary,
    tertiaryContainer = TacticalAmberContainer,
    onTertiaryContainer = TacticalAmberOnContainer,
    background = StealthBackground,
    onBackground = StealthOnBackground,
    surface = StealthSurface,
    onSurface = StealthOnSurface,
    surfaceVariant = StealthSurfaceVariant,
    onSurfaceVariant = StealthOnSurfaceVariant,
    surfaceContainer = StealthSurfaceContainer,
    surfaceContainerHigh = StealthSurfaceContainerHigh,
    outline = StealthOutline,
    outlineVariant = StealthOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = LightCyanPrimary,
    onPrimary = LightCyanOnPrimary,
    primaryContainer = LightCyanContainer,
    onPrimaryContainer = LightCyanOnContainer,
    secondary = LightEmeraldSecondary,
    onSecondary = LightEmeraldOnSecondary,
    secondaryContainer = LightEmeraldContainer,
    onSecondaryContainer = LightEmeraldOnContainer,
    tertiary = LightAmberTertiary,
    onTertiary = LightAmberOnTertiary,
    tertiaryContainer = LightAmberContainer,
    onTertiaryContainer = LightAmberOnContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek tactical dark theme
    dynamicColor: Boolean = false, // Keep branded high-contrast tactical styling
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

