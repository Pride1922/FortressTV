package com.fortresstv.player.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Electric Cinema Azure & Rich Dark Slate Palette
val FortressGreen = Color(0xFF00D2FF)
val FortressGreenDark = Color(0xFF0284C7)
val FortressNavy = Color(0xFF0B0F17)
val FortressSurface = Color(0xFF111827)
val FortressCard = Color(0xFF182231)
val FortressSurfaceLight = Color(0xFFF4F7FA)
val FortressText = Color(0xFFF1F5F9)
val FortressTextDark = Color(0xFF111827)
val FortressMuted = Color(0xFF94A3B8)
val FortressOutline = Color(0xFF334155)

// Glassmorphism & Atmospheric elevation
val FortressGlassOverlay = Color(0xDC0B0F17)
val FortressGlassCard = Color(0xD8182231)
val FortressGlowPrimary = Color(0x3300D2FF)
val FortressBorderSubtle = Color(0x20FFFFFF)
val FortressAtmosphericTop = Color(0xFF131D30)

// Official Atmospheric Blue Gradient Background
val FortressBackgroundBrush = androidx.compose.ui.graphics.Brush.verticalGradient(
    0.0f to Color(0xFF142C54), // Vibrant atmospheric sapphire blue at top
    0.42f to Color(0xFF0C192E), // Deep midnight navy
    1.0f to Color(0xFF070B12)  // Deep OLED base
)
