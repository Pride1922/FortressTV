package com.fortresstv.player.trakt

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fortresstv.player.transfer.QrCodeGenerator
import com.fortresstv.player.ui.LocalAppStrings
import com.fortresstv.player.ui.LocalFormFactor
import com.fortresstv.player.ui.tvFocusHighlight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed class PairingUiState {
    object Generating : PairingUiState()
    data class Waiting(val deviceCode: TraktDeviceCode, val remainingSeconds: Int) : PairingUiState()
    object Expired : PairingUiState()
    object Denied : PairingUiState()
    data class Error(val message: String) : PairingUiState()
    object Success : PairingUiState()
}

@Composable
fun TraktDeviceCodeDialog(
    clientId: String,
    client: TraktClient = remember { TraktClient() },
    onSuccess: (TraktToken, TraktUserProfile) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val isTv = LocalFormFactor.current.isTelevision
    val coroutineScope = rememberCoroutineScope()

    var state by remember { mutableStateOf<PairingUiState>(PairingUiState.Generating) }
    val cancelFocusRequester = remember { FocusRequester() }
    val retryFocusRequester = remember { FocusRequester() }

    fun startPairing() {
        state = PairingUiState.Generating
        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) {
                client.generateDeviceCode(clientId)
            }
            when (result) {
                is TraktAuthResult.Success -> {
                    val code = result.data
                    state = PairingUiState.Waiting(code, code.expiresInSeconds)

                    // Launch countdown timer
                    launch {
                        var remaining = code.expiresInSeconds
                        while (isActive && remaining > 0) {
                            delay(1000L)
                            remaining--
                            val current = state
                            if (current is PairingUiState.Waiting) {
                                state = current.copy(remainingSeconds = remaining)
                            }
                        }
                        if (isActive && state is PairingUiState.Waiting) {
                            state = PairingUiState.Expired
                        }
                    }

                    // Launch polling loop
                    launch {
                        val pollResult = withContext(Dispatchers.IO) {
                            client.pollUntilAuthorized(code, clientId)
                        }
                        when (pollResult) {
                            is TraktPollResult.Success -> {
                                state = PairingUiState.Success
                                // Fetch profile in background
                                val profileResult = withContext(Dispatchers.IO) {
                                    client.fetchUserProfile(pollResult.token.accessToken, clientId)
                                }
                                val profile = when (profileResult) {
                                    is TraktAuthResult.Success -> profileResult.data
                                    else -> TraktUserProfile(
                                        username = "trakt_user",
                                        name = "Trakt User",
                                        isVip = false,
                                        avatarUrl = null
                                    )
                                }
                                onSuccess(pollResult.token, profile)
                            }
                            is TraktPollResult.Expired -> {
                                state = PairingUiState.Expired
                            }
                            is TraktPollResult.Denied -> {
                                state = PairingUiState.Denied
                            }
                            is TraktPollResult.Error -> {
                                state = PairingUiState.Error(pollResult.message)
                            }
                            else -> {}
                        }
                    }
                }
                is TraktAuthResult.Failure -> {
                    state = PairingUiState.Error(result.message)
                }
            }
        }
    }

    LaunchedEffect(clientId) {
        startPairing()
    }

    LaunchedEffect(state, isTv) {
        if (isTv) {
            when (state) {
                is PairingUiState.Waiting -> cancelFocusRequester.requestFocus()
                is PairingUiState.Expired, is PairingUiState.Denied, is PairingUiState.Error -> retryFocusRequester.requestFocus()
                else -> {}
            }
        }
    }

    BackHandler(onBack = onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 560.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.traktPairingTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    when (val uiState = state) {
                        is PairingUiState.Generating -> {
                            Spacer(Modifier.height(24.dp))
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = strings.traktPairingGenerating,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(24.dp))
                        }

                        is PairingUiState.Waiting -> {
                            Text(
                                text = strings.traktPairingInstructions,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // User Code Display
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = uiState.deviceCode.userCode,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 4.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.clickable {
                                            runCatching {
                                                val intent = Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse(uiState.deviceCode.verificationUrl)
                                                )
                                                context.startActivity(intent)
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = uiState.deviceCode.verificationUrl,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.OpenInBrowser,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }

                            // QR Code
                            val qrBitmap = remember(uiState.deviceCode.verificationUrl) {
                                QrCodeGenerator.generateImageBitmap(uiState.deviceCode.verificationUrl, 380)
                            }
                            if (qrBitmap != null) {
                                Box(
                                    modifier = Modifier
                                        .size(190.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White)
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = qrBitmap,
                                        contentDescription = "Trakt activation QR code",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            // Expiry countdown and waiting status
                            val minutes = uiState.remainingSeconds / 60
                            val seconds = uiState.remainingSeconds % 60
                            val timeStr = "%02d:%02d".format(minutes, seconds)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = strings.traktPairingWaiting,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = strings.traktPairingExpiresIn.format(timeStr),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )

                            // Cancel Button
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(cancelFocusRequester)
                                    .tvFocusHighlight(shape = RoundedCornerShape(10.dp))
                            ) {
                                Text(strings.cancel)
                            }
                        }

                        is PairingUiState.Expired -> {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = strings.traktPairingExpired,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Button(
                                onClick = { startPairing() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(retryFocusRequester)
                                    .tvFocusHighlight(shape = RoundedCornerShape(10.dp))
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(strings.traktPairingRetry)
                            }
                        }

                        is PairingUiState.Denied -> {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = strings.traktPairingDenied,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Button(
                                onClick = { startPairing() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(retryFocusRequester)
                                    .tvFocusHighlight(shape = RoundedCornerShape(10.dp))
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(strings.traktPairingRetry)
                            }
                        }

                        is PairingUiState.Error -> {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = uiState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { startPairing() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(retryFocusRequester)
                                    .tvFocusHighlight(shape = RoundedCornerShape(10.dp))
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(strings.traktPairingRetry)
                            }
                        }

                        is PairingUiState.Success -> {
                            CircularProgressIndicator(modifier = Modifier.size(40.dp))
                            Text(
                                text = strings.traktPairingSuccess,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
