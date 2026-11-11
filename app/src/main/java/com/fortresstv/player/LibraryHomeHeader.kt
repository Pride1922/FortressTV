package com.fortresstv.player

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortresstv.player.profile.HouseholdProfile
import com.fortresstv.player.profile.ProfileAvatar
import com.fortresstv.player.ui.LocalAppStrings
import com.fortresstv.player.ui.tvFocusHighlight

@Composable
internal fun LibraryHomeHeader(
    onHome: () -> Unit,
    onSwitchProfile: (() -> Unit)?,
    onSettings: () -> Unit,
    onCloseApp: () -> Unit,
    profile: HouseholdProfile?,
    modifier: Modifier = Modifier,
    homeModifier: Modifier = Modifier,
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
                    ProfileAvatar(profile.name, profile.isChild, profile.id == "restricted_default", size = 30.dp, fontSize = 13.sp)
                }
            }
            DropdownMenu(expanded = profileOpen, onDismissRequest = { profileOpen = false }) {
                if (onSwitchProfile != null) {
                    DropdownMenuItem(text = { Text(strings.switchProfile) }, onClick = { profileOpen = false; onSwitchProfile() })
                }
                DropdownMenuItem(text = { Text(strings.settings) }, onClick = { profileOpen = false; onSettings() })
                DropdownMenuItem(text = { Text(strings.closeApp) }, onClick = { profileOpen = false; onCloseApp() })
            }
        }
    }
}

@Composable
internal fun LibrarySearchButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val strings = LocalAppStrings.current
    TextButton(onClick = onClick, modifier = modifier.tvFocusHighlight(shape = RoundedCornerShape(12.dp), scaleOnFocus = 1f)) {
        Icon(Icons.Default.Search, contentDescription = strings.search, modifier = Modifier.size(22.dp))
    }
}
