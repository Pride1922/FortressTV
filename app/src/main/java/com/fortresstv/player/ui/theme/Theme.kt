package com.fortresstv.player.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColorScheme = darkColorScheme(
    primary = FortressGreen,
    secondary = FortressGreen,
    primaryContainer = FortressGreenDark,
    onPrimaryContainer = FortressText,
    background = FortressNavy,
    surface = FortressNavy,
    surfaceVariant = FortressCard,
    surfaceContainer = FortressSurface,
    surfaceContainerHigh = FortressCard,
    surfaceContainerHighest = FortressCard,
    onPrimary = FortressNavy,
    onBackground = FortressText,
    onSurface = FortressText,
    onSurfaceVariant = FortressMuted,
    outline = FortressOutline
)

private val LightColorScheme = lightColorScheme(
    primary = FortressGreenDark,
    secondary = FortressGreenDark,
    background = FortressSurfaceLight,
    surface = FortressSurfaceLight,
    onPrimary = FortressText,
    onBackground = FortressTextDark,
    onSurface = FortressTextDark
)

@Composable
fun FortressTVTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        CompositionLocalProvider(
            LocalContentColor provides colorScheme.onBackground,
            content = content
        )
    }
}
