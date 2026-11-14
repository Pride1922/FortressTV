package com.fortresstv.player.profile

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fortresstv.player.R

internal data class ProfileAvatarPreset(
    val id: String,
    @param:DrawableRes val drawableRes: Int
)

internal object ProfileAvatarPresets {
    val presets: List<ProfileAvatarPreset> = listOf(
        ProfileAvatarPreset("preset:shield", R.drawable.avatar_shield),
        ProfileAvatarPreset("preset:fox", R.drawable.avatar_fox),
        ProfileAvatarPreset("preset:robot", R.drawable.avatar_robot),
        ProfileAvatarPreset("preset:astronaut", R.drawable.avatar_astronaut),
        ProfileAvatarPreset("preset:cat", R.drawable.avatar_cat),
        ProfileAvatarPreset("preset:owl", R.drawable.avatar_owl),
        ProfileAvatarPreset("preset:popcorn", R.drawable.avatar_popcorn),
        ProfileAvatarPreset("preset:dino", R.drawable.avatar_dino),
        ProfileAvatarPreset("preset:panda", R.drawable.avatar_panda),
        ProfileAvatarPreset("preset:wolf", R.drawable.avatar_wolf)
    )

    private val byId = presets.associateBy { it.id }

    @DrawableRes
    fun resolveDrawableRes(avatarUrl: String?): Int? {
        if (avatarUrl.isNullOrBlank()) return null
        return byId[avatarUrl]?.drawableRes
    }
}

// Vibrant, Netflix-style avatar palette
private val AvatarPalette = listOf(
    Color(0xFFE50914) to Color.White, // Red
    Color(0xFF1E88E5) to Color.White, // Blue
    Color(0xFF43A047) to Color.White, // Green
    Color(0xFF8E24AA) to Color.White, // Purple
    Color(0xFF00ACC1) to Color.White, // Cyan
    Color(0xFFFB8C00) to Color.White, // Orange
    Color(0xFFD81B60) to Color.White, // Pink
    Color(0xFF3949AB) to Color.White, // Indigo
    Color(0xFF00897B) to Color.White  // Teal
)

@Composable
fun ProfileAvatar(
    name: String,
    isChild: Boolean = false,
    isRestrictedDefault: Boolean = false,
    size: Dp = 32.dp,
    fontSize: TextUnit = 16.sp,
    shape: Shape = CircleShape,
    usePalette: Boolean = false,
    avatarUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val (backgroundColor, contentColor) = when {
        isRestrictedDefault -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        usePalette -> {
            val idx = kotlin.math.abs(name.trim().lowercase().hashCode()) % AvatarPalette.size
            AvatarPalette[idx]
        }
        isChild -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (isRestrictedDefault) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(size * 0.55f)
            )
        } else {
            val presetRes = ProfileAvatarPresets.resolveDrawableRes(avatarUrl)
            if (presetRes != null) {
                Image(
                    painter = painterResource(presetRes),
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = initial,
                    color = contentColor,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
