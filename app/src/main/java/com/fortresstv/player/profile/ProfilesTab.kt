package com.fortresstv.player.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortresstv.player.ui.LocalAppStrings
import com.fortresstv.player.ui.tvFocusHighlight
import kotlinx.coroutines.launch

@Composable
internal fun ProfilesTab(
    onProfileChanged: (HouseholdProfile) -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { ProfileRepository.get(context) }
    val languageTag = LocalAppStrings.current.languageTag
    val copy = remember(languageTag) {
        ProfileStrings(languageTag.startsWith("pt"))
    }
    val activeProfile by repository.activeProfile.collectAsState()
    val scope = rememberCoroutineScope()

    var showSelectionDialog by remember { mutableStateOf(false) }
    var showManagementDialog by remember { mutableStateOf(false) }
    var showPinDialogForManagement by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showPinRemoveDialog by remember { mutableStateOf(false) }

    if (showSelectionDialog) {
        ProfileSelectionDialog(
            onProfileSelected = { newProf ->
                showSelectionDialog = false
                onProfileChanged(newProf)
            },
            onDismiss = { showSelectionDialog = false }
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

    if (showPinSetupDialog) {
        ParentalPinDialog(
            mode = HouseholdPinDialogMode.SETUP,
            onSuccess = { showPinSetupDialog = false },
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = copy.profilesTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Active Profile Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileAvatar(
                    name = activeProfile.name,
                    isChild = activeProfile.isChild,
                    isRestrictedDefault = activeProfile.id == "restricted_default",
                    size = 54.dp,
                    fontSize = 24.sp,
                    avatarUrl = activeProfile.avatarUrl
                )

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeProfile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (activeProfile.isChild) "${copy.childProfile} (≤ ${activeProfile.maximumAge ?: 12}) · ${activeProfile.language}"
                        else "${copy.adultProfile} · ${activeProfile.language}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { showSelectionDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .tvFocusHighlight()
            ) {
                Text(copy.switchProfile)
            }

            OutlinedButton(
                onClick = {
                    val status = repository.getPinStatus()
                    if (status.hasPin && !repository.isAdultUnlocked()) {
                        showPinDialogForManagement = true
                    } else {
                        showManagementDialog = true
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .tvFocusHighlight()
            ) {
                Text(copy.manageProfiles)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Parental Controls Section
        Text(
            text = copy.parentalControls,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        val pinStatus = repository.getPinStatus()
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (pinStatus.hasPin) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (pinStatus.hasPin) copy.pinProtected else copy.parentalPin,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            if (pinStatus.hasPin) {
                                showPinRemoveDialog = true
                            } else {
                                showPinSetupDialog = true
                            }
                        },
                        modifier = Modifier.tvFocusHighlight()
                    ) {
                        Text(if (pinStatus.hasPin) copy.removePin else copy.setPin)
                    }
                }
            }
        }
    }
}
