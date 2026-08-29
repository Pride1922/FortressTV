package com.fortresstv.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = FortressGreen,
    secondary = FortressGreen,
    background = FortressNavy,
    surface = FortressNavy,
    surfaceVariant = FortressSurface,
    onPrimary = FortressNavy,
    onBackground = FortressText,
    onSurface = FortressText,
    onSurfaceVariant = FortressMuted
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
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
