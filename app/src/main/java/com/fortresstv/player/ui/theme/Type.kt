package com.fortresstv.player.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * Tabular digits style (tnum) ensures all numeric characters share identical width.
 * Prevents horizontal layout jitter in live clocks, seekbars, countdowns, and timestamps.
 */
val TabularDigitsTextStyle = TextStyle(
    fontFeatureSettings = "tnum"
)

fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")