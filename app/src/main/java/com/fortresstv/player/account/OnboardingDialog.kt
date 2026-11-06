package com.fortresstv.player.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fortresstv.player.playlist.PlaylistProfile
import com.fortresstv.player.profile.HouseholdPinDialogMode
import com.fortresstv.player.profile.ParentalPinDialog
import com.fortresstv.player.profile.ProfileRepository
import com.fortresstv.player.source.CloudSourceSyncService
import com.fortresstv.player.source.ContentSource
import com.fortresstv.player.source.SourceSettingsStore
import com.fortresstv.player.ui.LocalAppStrings
import com.fortresstv.player.ui.tvFocusHighlight
import com.fortresstv.player.xtream.XtreamClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class OnboardingStep {
    AUTH,
    PIN_SETUP,
    ADD_SOURCE,
    COMPLETE
}

@Composable
fun OnboardingDialog(
    onDismiss: (() -> Unit)? = null,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AccountRepository.get(context) }
    val profileRepository = remember { ProfileRepository.get(context) }
    val sourceSyncService = remember { CloudSourceSyncService.get(context) }
    val settingsStore = remember { SourceSettingsStore(context) }

    val languageTag = LocalAppStrings.current.languageTag
    val copy = remember(languageTag) { OnboardingStrings(languageTag.startsWith("pt")) }
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(OnboardingStep.AUTH) }
    var busy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusNotice by remember { mutableStateOf<String?>(null) }

    // Xtream inputs
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var sourceName by remember { mutableStateOf("") }

    var showPinDialog by remember { mutableStateOf(false) }
    var showUnlockPinDialog by remember { mutableStateOf(false) }

    fun proceedAfterAuth() {
        busy = true
        statusNotice = copy.restoreNotice
        errorMessage = null
        scope.launch {
            try {
                val remoteProfiles = withContext(Dispatchers.IO) {
                    profileRepository.refresh()
                    profileRepository.profiles.value
                }
                val pinStatus = profileRepository.getPinStatus()
                if (pinStatus.hasPin && !profileRepository.isAdultUnlocked()) {
                    busy = false
                    showUnlockPinDialog = true
                    return@launch
                }

                val token = profileRepository.currentUnlockToken()
                val restoreResult = withContext(Dispatchers.IO) {
                    sourceSyncService.restoreSourcesFromCloud(token)
                }

                busy = false
                val restoredSources = restoreResult.getOrThrow()
                if (remoteProfiles.size > 1 || pinStatus.hasPin || restoredSources.isNotEmpty()) {
                    // Existing household restored -> done, proceed to profile selection
                    onComplete()
                } else {
                    // New household -> configure PIN and Xtream
                    step = OnboardingStep.PIN_SETUP
                }
            } catch (e: Exception) {
                busy = false
                errorMessage = e.message ?: copy.genericError
            }
        }
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = copy.welcomeTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                if (step == OnboardingStep.AUTH) {
                    Text(
                        text = copy.welcomeSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (busy) {
                        LinearProgressIndicator(Modifier.fillMaxWidth(0.6f))
                        statusNotice?.let { Text(it) }
                    }

                    // Embed existing AccountTab for sign in / registration
                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        AccountTab(
                            onSignedOut = {},
                            onSignedIn = {
                                proceedAfterAuth()
                            }
                        )
                    }
                } else if (step == OnboardingStep.PIN_SETUP) {
                    Text(
                        text = copy.stepPinSetup,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = copy.pinSetupDescription,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Button(
                        onClick = { showPinDialog = true },
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .heightIn(min = 48.dp)
                            .tvFocusHighlight()
                    ) {
                        Text(copy.stepPinSetup)
                    }

                    TextButton(
                        onClick = { step = OnboardingStep.ADD_SOURCE },
                        modifier = Modifier.tvFocusHighlight()
                    ) {
                        Text(copy.skipForNow)
                    }

                    if (showPinDialog) {
                        ParentalPinDialog(
                            mode = HouseholdPinDialogMode.SETUP,
                            onSuccess = {
                                showPinDialog = false
                                step = OnboardingStep.ADD_SOURCE
                            },
                            onDismiss = { showPinDialog = false }
                        )
                    }
                } else if (step == OnboardingStep.ADD_SOURCE) {
                    Text(
                        text = copy.stepAddSource,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = copy.addSourceDescription,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = sourceName,
                        onValueChange = { sourceName = it },
                        label = { Text(copy.sourceName) },
                        modifier = Modifier.fillMaxWidth(0.7f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text(copy.serverUrl) },
                        modifier = Modifier.fillMaxWidth(0.7f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(copy.username) },
                        modifier = Modifier.fillMaxWidth(0.7f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(copy.password) },
                        modifier = Modifier.fillMaxWidth(0.7f),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    if (busy) {
                        LinearProgressIndicator(Modifier.fillMaxWidth(0.7f))
                        Text(copy.validating)
                    }

                    errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }

                    Button(
                        onClick = {
                            if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) return@Button
                            busy = true
                            errorMessage = null
                            scope.launch {
                                val contentSource = ContentSource.Xtream(
                                    value = serverUrl.trim(),
                                    displayName = sourceName.ifBlank { "Xtream IPTV" },
                                    username = username.trim(),
                                    password = password.trim()
                                )
                                val valid = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val client = XtreamClient(context, "temp_onboard", contentSource)
                                        client.authenticate()
                                        true
                                    }.getOrDefault(false)
                                }
                                busy = false
                                if (valid) {
                                    val profile = PlaylistProfile(
                                        id = java.util.UUID.randomUUID().toString(),
                                        name = contentSource.displayName,
                                        source = contentSource
                                    )
                                    settingsStore.savePlaylistProfile(profile)
                                    sourceSyncService.syncSourcesToCloud(null)
                                    step = OnboardingStep.COMPLETE
                                } else {
                                    errorMessage = copy.connectionFailed
                                }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .heightIn(min = 48.dp)
                            .tvFocusHighlight()
                    ) {
                        Text(copy.validateAndSave)
                    }

                    TextButton(
                        onClick = { step = OnboardingStep.COMPLETE },
                        enabled = !busy,
                        modifier = Modifier.tvFocusHighlight()
                    ) {
                        Text(copy.skipForNow)
                    }
                } else if (step == OnboardingStep.COMPLETE) {
                    Text(
                        text = copy.onboardingComplete,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = onComplete,
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .heightIn(min = 48.dp)
                            .tvFocusHighlight()
                    ) {
                        Text(copy.finish)
                    }
                }
            }
        }
    }

    if (showUnlockPinDialog) {
        ParentalPinDialog(
            mode = HouseholdPinDialogMode.VERIFY,
            onSuccess = {
                showUnlockPinDialog = false
                proceedAfterAuth()
            },
            onDismiss = {
                showUnlockPinDialog = false
                busy = false
            }
        )
    }
}
