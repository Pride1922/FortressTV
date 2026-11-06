package com.fortresstv.player.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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

    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
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
