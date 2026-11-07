package com.fortresstv.player.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fortresstv.player.R
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
                val token = profileRepository.currentUnlockToken()
                val restoreResult = withContext(Dispatchers.IO) {
                    sourceSyncService.restoreSourcesFromCloud(token)
                }

                busy = false
                val restoredSources = restoreResult.getOrDefault(emptyList())
                if (restoredSources.isEmpty() && settingsStore.getPlaylists().isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        runCatching { sourceSyncService.syncSourcesToCloud(token) }
                    }
                }
                if (remoteProfiles.size > 1 || pinStatus.hasPin || pinStatus.isProtected || restoredSources.isNotEmpty() || settingsStore.getPlaylists().isNotEmpty()) {
                    // Existing household or sources restored -> done, proceed to profile selection
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

    LaunchedEffect(Unit) {
        if (repository.currentUserId() != null) {
            proceedAfterAuth()
        }
    }

    Dialog(
        onDismissRequest = { onDismiss?.invoke() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = onDismiss != null,
            dismissOnClickOutside = false
        )
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
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                modifier = Modifier
                    .widthIn(min = 340.dp, max = 450.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (step) {
                        OnboardingStep.AUTH -> {
                            Image(
                                painter = painterResource(R.drawable.fortress_tv_logo),
                                contentDescription = "FortressTV",
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(18.dp))
                            )

                            if (busy) {
                                LinearProgressIndicator(Modifier.fillMaxWidth())
                                statusNotice?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            errorMessage?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            }

                            AccountTab(
                                modifier = Modifier.fillMaxWidth(),
                                showHeader = false,
                                onSignedOut = {},
                                onSignedIn = {
                                    proceedAfterAuth()
                                }
                            )

                            if (repository.currentUserId() != null && !busy) {
                                Button(
                                    onClick = { proceedAfterAuth() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .tvFocusHighlight()
                                ) {
                                    Text(if (languageTag.startsWith("pt")) "Continuar" else "Continue")
                                }
                            }

                            if (onDismiss != null && !busy) {
                                TextButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .tvFocusHighlight()
                                ) {
                                    Text(
                                        text = if (languageTag.startsWith("pt")) "Continuar localmente" else "Continue locally",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        OnboardingStep.PIN_SETUP -> {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Text(
                                text = copy.stepPinSetup,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = copy.pinSetupDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = { showPinDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .tvFocusHighlight()
                            ) {
                                Text(copy.stepPinSetup)
                            }

                            TextButton(
                                onClick = { step = OnboardingStep.ADD_SOURCE },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tvFocusHighlight()
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
                        }

                        OnboardingStep.ADD_SOURCE -> {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LiveTv,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Text(
                                text = copy.stepAddSource,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = copy.addSourceDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            OutlinedTextField(
                                value = sourceName,
                                onValueChange = { sourceName = it },
                                label = { Text(copy.sourceName) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !busy
                            )
                            OutlinedTextField(
                                value = serverUrl,
                                onValueChange = { serverUrl = it },
                                label = { Text(copy.serverUrl) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !busy
                            )
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text(copy.username) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !busy
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text(copy.password) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                enabled = !busy
                            )

                            if (busy) {
                                LinearProgressIndicator(Modifier.fillMaxWidth())
                                Text(
                                    text = copy.validating,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            errorMessage?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
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
                                enabled = !busy && serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .tvFocusHighlight()
                            ) {
                                Text(copy.validateAndSave)
                            }

                            TextButton(
                                onClick = { step = OnboardingStep.COMPLETE },
                                enabled = !busy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tvFocusHighlight()
                            ) {
                                Text(copy.skipForNow)
                            }
                        }

                        OnboardingStep.COMPLETE -> {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Text(
                                text = copy.onboardingComplete,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = if (languageTag.startsWith("pt"))
                                    "A sua configuração está concluída. Desfrute do FortressTV!"
                                else
                                    "Your setup is complete. Enjoy FortressTV!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = onComplete,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .tvFocusHighlight()
                            ) {
                                Text(copy.finish)
                            }
                        }
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
