package com.fortresstv.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortresstv.player.profile.HouseholdProfile
import com.fortresstv.player.profile.ProfileAvatar
import com.fortresstv.player.profile.ProfileRepository
import com.fortresstv.player.profile.ProfileStrings
import com.fortresstv.player.profile.ProfileSwitcher
import com.fortresstv.player.ui.LocalAppStrings
import com.fortresstv.player.ui.LocalFormFactor
import com.fortresstv.player.ui.tvFocusHighlight

private const val RESTRICTED_PROFILE_ID = "restricted_default"
private const val QUICK_SWITCH_LIMIT = 4

@Composable
internal fun LibraryHomeHeader(
    onHome: () -> Unit,
    onSwitchProfile: (() -> Unit)?,
    onSettings: () -> Unit,
    onCloseApp: () -> Unit,
    profile: HouseholdProfile?,
    modifier: Modifier = Modifier,
    homeModifier: Modifier = Modifier,
    /** Enables one-tap switching in the menu. Owned by the app so a switch outlives this screen. */
    profileSwitcher: ProfileSwitcher? = null,
    search: @Composable () -> Unit
) {
    val strings = LocalAppStrings.current
    var profileOpen by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Row(modifier.fillMaxWidth().focusGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onHome,
            modifier = homeModifier.size(48.dp).testTag("library_destination_home").tvFocusHighlight(shape = shape, scaleOnFocus = 1f)
        ) { Icon(Icons.Default.Home, contentDescription = strings.navHome) }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { search() }
        Box(Modifier.width(48.dp), contentAlignment = Alignment.CenterEnd) {
            IconButton(
                onClick = { profileOpen = true },
                modifier = Modifier.size(48.dp).semantics { contentDescription = strings.libraryProfileMenu }
                    .tvFocusHighlight(shape = shape, scaleOnFocus = 1f)
            ) {
                if (profile == null) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(26.dp))
                } else {
                    // Same colour and photo as in the profile picker, so a person looks the same everywhere.
                    ProfileAvatar(
                        profile.name,
                        profile.isChild,
                        profile.id == RESTRICTED_PROFILE_ID,
                        size = 30.dp,
                        fontSize = 13.sp,
                        usePalette = true,
                        avatarUrl = profile.avatarUrl
                    )
                }
            }
            ProfileMenu(
                expanded = profileOpen,
                onClose = { profileOpen = false },
                profile = profile,
                onSwitchProfile = onSwitchProfile,
                onSettings = onSettings,
                onCloseApp = onCloseApp,
                profileSwitcher = profileSwitcher
            )
        }
    }
}

/**
 * The menu behind the profile avatar: who is watching, one-tap switching to the other profiles,
 * then the full profile picker, settings and (set apart) closing the app.
 */
@Composable
private fun ProfileMenu(
    expanded: Boolean,
    onClose: () -> Unit,
    profile: HouseholdProfile?,
    onSwitchProfile: (() -> Unit)?,
    onSettings: () -> Unit,
    onCloseApp: () -> Unit,
    profileSwitcher: ProfileSwitcher?
) {
    val strings = LocalAppStrings.current
    val copy = remember(strings.languageTag) { ProfileStrings(strings.languageTag.startsWith("pt")) }
    val isTv = LocalFormFactor.current.isTelevision
    val repository = ProfileRepository.get(LocalContext.current)
    val profiles by repository.profiles.collectAsState()

    val otherProfiles = remember(profiles, profile?.id) {
        profiles.filter { it.id != profile?.id && it.id != RESTRICTED_PROFILE_ID }.take(QUICK_SWITCH_LIMIT)
    }
    val showQuickSwitch = profileSwitcher != null && onSwitchProfile != null && profile != null && otherProfiles.isNotEmpty()
    val firstFocus = remember { FocusRequester() }
    val outline = MaterialTheme.colorScheme.outline

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onClose,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        shadowElevation = 16.dp,
        border = BorderStroke(1.dp, outline.copy(alpha = 0.6f)),
        modifier = Modifier.width(288.dp)
    ) {
        if (isTv) {
            // Start on the first choice so the remote doesn't have to find its way into the menu.
            LaunchedEffect(Unit) {
                withFrameNanos { }
                runCatching { firstFocus.requestFocus() }
            }
        }

        if (profile != null) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileAvatar(
                    profile.name,
                    profile.isChild,
                    profile.id == RESTRICTED_PROFILE_ID,
                    size = 48.dp,
                    fontSize = 20.sp,
                    usePalette = true,
                    avatarUrl = profile.avatarUrl
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (profile.id != RESTRICTED_PROFILE_ID) {
                        Text(
                            if (profile.isChild) copy.childProfile else copy.adultProfile,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (showQuickSwitch) {
            Text(
                copy.switchTo.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                otherProfiles.forEachIndexed { index, other ->
                    QuickSwitchAvatar(
                        profile = other,
                        locked = profileSwitcher?.needsPin(other) == true,
                        modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
                        onClick = {
                            onClose()
                            profileSwitcher?.request(other)
                        }
                    )
                }
            }
        }

        if (profile != null || showQuickSwitch) MenuDivider()

        if (onSwitchProfile != null) {
            ProfileMenuAction(
                icon = Icons.Outlined.People,
                label = if (showQuickSwitch) copy.allProfiles else strings.switchProfile,
                modifier = if (!showQuickSwitch) Modifier.focusRequester(firstFocus) else Modifier,
                onClick = {
                    onClose()
                    onSwitchProfile()
                }
            )
        }
        ProfileMenuAction(
            icon = Icons.Outlined.Settings,
            label = strings.settings,
            modifier = if (onSwitchProfile == null) Modifier.focusRequester(firstFocus) else Modifier,
            onClick = {
                onClose()
                onSettings()
            }
        )
        MenuDivider()
        ProfileMenuAction(
            icon = Icons.Outlined.PowerSettingsNew,
            label = strings.closeApp,
            muted = true,
            onClick = {
                onClose()
                onCloseApp()
            }
        )
    }
}

@Composable
private fun QuickSwitchAvatar(
    profile: HouseholdProfile,
    locked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.1f else 1f, label = "quick_switch_scale")
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier
            .width(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (focused) accent.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
            ProfileAvatar(
                profile.name,
                profile.isChild,
                size = 44.dp,
                fontSize = 18.sp,
                usePalette = true,
                avatarUrl = profile.avatarUrl,
                modifier = if (focused) Modifier.border(2.dp, accent, CircleShape) else Modifier
            )
            if (locked) {
                Box(
                    Modifier
                        .size(18.dp)
                        .background(Color.Black.copy(alpha = 0.8f), CircleShape)
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            profile.name,
            style = MaterialTheme.typography.labelMedium,
            color = if (focused) accent else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun ProfileMenuAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    muted: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val accent = MaterialTheme.colorScheme.primary
    val content = when {
        focused -> accent
        muted -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (focused) accent.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
            color = content
        )
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    )
}

@Composable
internal fun LibrarySearchButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val strings = LocalAppStrings.current
    TextButton(onClick = onClick, modifier = modifier.tvFocusHighlight(shape = RoundedCornerShape(12.dp), scaleOnFocus = 1f)) {
        Icon(Icons.Default.Search, contentDescription = strings.search, modifier = Modifier.size(22.dp))
    }
}
