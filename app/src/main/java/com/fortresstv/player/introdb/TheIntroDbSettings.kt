package com.fortresstv.player.introdb

import androidx.compose.runtime.compositionLocalOf

data class TheIntroDbSettings(
    val enabled: Boolean = true,
    val autoSkipIntro: Boolean = false,
    val autoSkipRecap: Boolean = false
)

val LocalTheIntroDbSettings = compositionLocalOf { TheIntroDbSettings() }
