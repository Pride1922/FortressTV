package com.fortresstv.player.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fortresstv.player.profile.HouseholdProfile
import com.fortresstv.player.profile.ProfileRepository
import com.fortresstv.player.ui.LocalAppStrings
import com.fortresstv.player.ui.tvFocusHighlight
import kotlinx.coroutines.launch

@Composable
fun LocalDataImportDialog(
    householdId: String,
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    val profileRepository = remember { ProfileRepository.get(context) }
    val migrationService = remember { LocalDataMigrationService(context) }

    val adultProfiles = remember {
        profileRepository.profiles.value.filter { it.isAdult }
    }
    var selectedProfileId by remember {
        mutableStateOf(adultProfiles.firstOrNull()?.id.orEmpty())
    }

    var busy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                modifier = Modifier
                    .widthIn(min = 340.dp, max = 460.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                Text(
                    text = strings.importLocalDataTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = strings.importLocalDataMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (adultProfiles.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        adultProfiles.forEach { prof ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !busy) { selectedProfileId = prof.id }
                                    .tvFocusHighlight(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedProfileId == prof.id)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedProfileId == prof.id,
                                        onClick = { selectedProfileId = prof.id },
                                        enabled = !busy
                                    )
                                    Text(
                                        text = prof.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }

                if (busy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            migrationService.skipMigration(householdId)
                            onDismiss()
                        },
                        enabled = !busy,
                        modifier = Modifier.tvFocusHighlight()
                    ) {
                        Text(strings.importLocalDataDismiss)
                    }

                    Button(
                        onClick = {
                            if (selectedProfileId.isBlank()) return@Button
                            busy = true
                            errorMessage = null
                            scope.launch {
                                val unlockToken = profileRepository.currentUnlockToken()
                                val result = migrationService.importToAdultProfile(
                                    targetAdultProfileId = selectedProfileId,
                                    unlockToken = unlockToken
                                )
                                busy = false
                                result.onSuccess {
                                    onComplete()
                                }.onFailure {
                                    errorMessage = it.message ?: "Failed to import local data"
                                }
                            }
                        },
                        enabled = !busy && selectedProfileId.isNotBlank(),
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .tvFocusHighlight()
                    ) {
                        Text(strings.importLocalDataConfirm)
                    }
                }
            }
        }
    }
}
}
