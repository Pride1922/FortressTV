package com.fortresstv.player.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fortresstv.player.ui.LocalAppStrings
import com.fortresstv.player.ui.tvFocusHighlight
import kotlinx.coroutines.launch

@Composable
fun ProfileSelectionDialog(
    onProfileSelected: (HouseholdProfile) -> Unit,
    onDismiss: () -> Unit,
    isMandatory: Boolean = false
) {
    val repository = ProfileRepository.get(androidx.compose.ui.platform.LocalContext.current)
    val languageTag = LocalAppStrings.current.languageTag
    val copy = remember(languageTag) {
        ProfileStrings(languageTag.startsWith("pt"))
    }
    val profiles by repository.profiles.collectAsState()
    val activeProfile by repository.activeProfile.collectAsState()
    val lastUsedProfileId = remember(profiles, activeProfile) { repository.getLastActiveProfileId() }
    val scope = rememberCoroutineScope()

    var showPinDialogForProfile by remember { mutableStateOf<HouseholdProfile?>(null) }
    var showPinDialogForManagement by remember { mutableStateOf(false) }
    var showManagementDialog by remember { mutableStateOf(false) }

    if (showPinDialogForProfile != null) {
        ParentalPinDialog(
            mode = HouseholdPinDialogMode.VERIFY,
            onSuccess = {
                val target = showPinDialogForProfile!!
                showPinDialogForProfile = null
                scope.launch {
                    val ok = runCatching { repository.selectProfile(target) }.getOrDefault(false)
                    if (ok) {
                        onProfileSelected(target)
                        onDismiss()
                    }
                }
            },
            onDismiss = { showPinDialogForProfile = null }
        )
    }

    if (showPinDialogForManagement) {
        ParentalPinDialog(
            mode = HouseholdPinDialogMode.VERIFY,
            onSuccess = {
                showPinDialogForManagement = false
                showManagementDialog = true
            },
            onDismiss = { showPinDialogForManagement = false }
        )
    }

    if (showManagementDialog) {
        ProfileManagementDialog(
            onDismiss = { showManagementDialog = false }
        )
    }

    Dialog(
        onDismissRequest = { if (!isMandatory) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .padding(24.dp)
                .wrapContentSize()
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isMandatory) copy.whoIsWatching else copy.profilesTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    for (prof in profiles) {
                        val isActive = (prof.id == activeProfile.id && activeProfile.id != "restricted_default")
                        val isLastUsed = (prof.id == lastUsedProfileId)
                        val status = repository.getPinStatus()
                        val requiresPin = prof.isAdult && (status.hasPin || activeProfile.isChild) && !repository.isAdultUnlocked()

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (requiresPin) {
                                        showPinDialogForProfile = prof
                                    } else {
                                        scope.launch {
                                            val ok = runCatching { repository.selectProfile(prof) }.getOrDefault(false)
                                            if (ok) {
                                                onProfileSelected(prof)
                                                onDismiss()
                                            }
                                        }
                                    }
                                }
                                .tvFocusHighlight()
                                .padding(8.dp)
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                ProfileAvatar(
                                    name = prof.name,
                                    isChild = prof.isChild,
                                    size = 96.dp,
                                    fontSize = 44.sp,
                                    shape = RoundedCornerShape(12.dp),
                                    usePalette = true,
                                    avatarUrl = prof.avatarUrl,
                                    modifier = if (isLastUsed) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                    } else Modifier
                                )
                                if (prof.isAdult && status.hasPin) {
                                    Box(
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .size(24.dp)
                                            .background(Color.Black.copy(alpha = 0.75f), CircleShape)
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = copy.pinProtected,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = prof.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (isLastUsed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                OutlinedButton(
                    onClick = {
                        val status = repository.getPinStatus()
                        if (status.hasPin && !repository.isAdultUnlocked()) {
                            showPinDialogForManagement = true
                        } else {
                            showManagementDialog = true
                        }
                    },
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.tvFocusHighlight()
                ) {
                    Text(
                        text = copy.manageProfiles,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                }

                if (!isMandatory) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDismiss, modifier = Modifier.tvFocusHighlight()) {
                        Text(copy.close)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProfileManagementDialog(
    onDismiss: () -> Unit
) {
    val repository = ProfileRepository.get(androidx.compose.ui.platform.LocalContext.current)
    val languageTag = LocalAppStrings.current.languageTag
    val copy = remember(languageTag) {
        ProfileStrings(languageTag.startsWith("pt"))
    }
    val profiles by repository.profiles.collectAsState()
    val scope = rememberCoroutineScope()

    var editingProfile by remember { mutableStateOf<HouseholdProfile?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var profileToDelete by remember { mutableStateOf<HouseholdProfile?>(null) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showPinRemoveDialog by remember { mutableStateOf(false) }

    if (showPinSetupDialog) {
        ParentalPinDialog(
            mode = HouseholdPinDialogMode.SETUP,
            onSuccess = {
                showPinSetupDialog = false
            },
            onDismiss = { showPinSetupDialog = false }
        )
    }

    if (showPinRemoveDialog) {
        ParentalPinDialog(
            mode = HouseholdPinDialogMode.VERIFY,
            title = copy.removePin,
            onSuccess = { unlockToken ->
                showPinRemoveDialog = false
                scope.launch {
                    runCatching { repository.removePin(unlockToken) }
                }
            },
            onDismiss = { showPinRemoveDialog = false }
        )
    }

    if (profileToDelete != null) {
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = { Text(copy.deleteProfile) },
            text = { Text(copy.deleteConfirm) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        val target = profileToDelete!!
                        profileToDelete = null
                        scope.launch { runCatching { repository.deleteProfile(target.id) } }
                    }
                ) {
                    Text(copy.delete)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { profileToDelete = null }) {
                    Text(copy.cancel)
                }
            }
        )
    }

    if (editingProfile != null || isCreatingNew) {
        ProfileEditDialog(
            profile = editingProfile,
            existingProfiles = profiles,
            onSave = { saved ->
                scope.launch {
                    runCatching { repository.saveProfile(saved) }
                    editingProfile = null
                    isCreatingNew = false
                }
            },
            onDismiss = {
                editingProfile = null
                isCreatingNew = false
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .width(460.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = copy.manageProfiles,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = { isCreatingNew = true },
                        modifier = Modifier.tvFocusHighlight()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = copy.addProfile)
                    }
                }

                Spacer(Modifier.height(12.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    for (prof in profiles) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProfileAvatar(
                                    name = prof.name,
                                    isChild = prof.isChild,
                                    size = 40.dp,
                                    fontSize = 18.sp,
                                    avatarUrl = prof.avatarUrl
                                )

                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(prof.name, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (prof.isChild) "${copy.child} (≤ ${prof.maximumAge ?: 12}) · ${prof.language}"
                                        else "${copy.adult} · ${prof.language}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { editingProfile = prof },
                                    modifier = Modifier.tvFocusHighlight()
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = copy.editProfile)
                                }

                                if (profiles.size > 1) {
                                    IconButton(
                                        onClick = { profileToDelete = prof },
                                        modifier = Modifier.tvFocusHighlight()
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = copy.deleteProfile,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Parental PIN Settings Button
                    val status = repository.getPinStatus()
                    OutlinedButton(
                        onClick = {
                            if (status.hasPin) {
                                showPinRemoveDialog = true
                            } else {
                                showPinSetupDialog = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvFocusHighlight()
                    ) {
                        Icon(
                            imageVector = if (status.hasPin) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (status.hasPin) copy.removePin else copy.setPin)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvFocusHighlight()
                ) {
                    Text(copy.cancel)
                }
            }
        }
    }
}

@Composable
internal fun ProfileEditDialog(
    profile: HouseholdProfile?,
    existingProfiles: List<HouseholdProfile> = emptyList(),
    onSave: (HouseholdProfile) -> Unit,
    onDismiss: () -> Unit
) {
    val languageTag = LocalAppStrings.current.languageTag
    val copy = remember(languageTag) {
        ProfileStrings(languageTag.startsWith("pt"))
    }
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var kind by remember { mutableStateOf(profile?.kind ?: ProfileKind.ADULT) }
    var language by remember {
        mutableStateOf(
            profile?.language ?: if (languageTag.startsWith("pt")) "pt-PT" else "en"
        )
    }
    var maximumAge by remember { mutableIntStateOf(profile?.maximumAge ?: 12) }
    var blockUnrated by remember { mutableStateOf(profile?.blockUnrated ?: true) }
    var hideAdultContent by remember { mutableStateOf(profile?.hideAdultContent ?: true) }
    var isSaving by remember { mutableStateOf(false) }

    val trimmedName = name.trim()
    val isDuplicateName = remember(trimmedName, profile, existingProfiles) {
        trimmedName.isNotEmpty() && existingProfiles.any {
            it.id != profile?.id && it.name.trim().equals(trimmedName, ignoreCase = true)
        }
    }

    val ageOptions = listOf(6, 10, 12, 14, 16, 18)

    Dialog(onDismissRequest = { if (!isSaving) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .width(420.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (profile != null) copy.editProfile else copy.addProfile,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 60) name = it },
                    label = { Text(copy.profileName) },
                    isError = isDuplicateName,
                    supportingText = if (isDuplicateName) {
                        { Text(copy.profileNameExists, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    enabled = !isSaving,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Profile Kind: Adult / Child
                Text(copy.profile, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = kind == ProfileKind.ADULT,
                        onClick = { kind = ProfileKind.ADULT },
                        label = { Text(copy.adult) },
                        modifier = Modifier.tvFocusHighlight()
                    )
                    FilterChip(
                        selected = kind == ProfileKind.CHILD,
                        onClick = {
                            kind = ProfileKind.CHILD
                            hideAdultContent = true
                            blockUnrated = true
                        },
                        label = { Text(copy.child) },
                        modifier = Modifier.tvFocusHighlight()
                    )
                }

                // Language: English / Portuguese
                Text(copy.language, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = language == "en",
                        onClick = { language = "en" },
                        label = { Text(copy.english) },
                        modifier = Modifier.tvFocusHighlight()
                    )
                    FilterChip(
                        selected = language == "pt-PT",
                        onClick = { language = "pt-PT" },
                        label = { Text(copy.portugueseLang) },
                        modifier = Modifier.tvFocusHighlight()
                    )
                }

                // Child restrictions
                if (kind == ProfileKind.CHILD) {
                    Text(copy.ageLimit, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (age in ageOptions) {
                            FilterChip(
                                selected = maximumAge == age,
                                onClick = { maximumAge = age },
                                label = { Text("$age") },
                                modifier = Modifier.tvFocusHighlight()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(copy.blockUnrated, fontSize = 14.sp)
                        Switch(checked = true, onCheckedChange = null, enabled = false)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(copy.hideAdultContent, fontSize = 14.sp)
                        Switch(checked = true, onCheckedChange = null, enabled = false)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(copy.hideAdultContent, fontSize = 14.sp)
                        Switch(
                            checked = hideAdultContent,
                            onCheckedChange = { hideAdultContent = it },
                            modifier = Modifier.tvFocusHighlight()
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSaving,
                        modifier = Modifier.tvFocusHighlight()
                    ) {
                        Text(copy.cancel)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = trimmedName.isNotEmpty() && !isDuplicateName && !isSaving,
                        onClick = {
                            if (isSaving) return@Button
                            isSaving = true
                            val result = HouseholdProfile(
                                id = profile?.id ?: java.util.UUID.randomUUID().toString(),
                                name = trimmedName,
                                kind = kind,
                                language = language,
                                hideAdultContent = if (kind == ProfileKind.CHILD) true else hideAdultContent,
                                maximumAge = if (kind == ProfileKind.CHILD) maximumAge else null,
                                blockUnrated = if (kind == ProfileKind.CHILD) true else blockUnrated
                            )
                            onSave(result)
                        },
                        modifier = Modifier.tvFocusHighlight()
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(copy.saving)
                        } else {
                            Text(copy.save)
                        }
                    }
                }
            }
        }
    }
}
