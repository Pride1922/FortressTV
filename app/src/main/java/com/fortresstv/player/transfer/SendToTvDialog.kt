package com.fortresstv.player.transfer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fortresstv.player.playlist.PlaylistProfile
import com.fortresstv.player.source.ContentSource
import com.fortresstv.player.source.SourceSettingsStore
import com.fortresstv.player.ui.LocalAppStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SendToTvDialog(
    profiles: List<PlaylistProfile>,
    initialProfile: PlaylistProfile? = null,
    directTarget: DiscoveredFortressTv? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    val sendable = remember(profiles) { profiles.filter { it.source !is ContentSource.LocalFile } }
    val epgUrl = remember {
        (SourceSettingsStore(context.applicationContext).epgSource() as? ContentSource.RemoteUrl)?.value
    }
    val discovery = remember { TvNsdManager(context) }
    val devices by discovery.devices.collectAsState()
    var selectedProfile by remember(sendable, initialProfile) {
        mutableStateOf(initialProfile?.takeIf { it.source !is ContentSource.LocalFile } ?: sendable.firstOrNull())
    }
    var selectedTv by remember(directTarget) { mutableStateOf(directTarget) }
    var sending by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(devices) {
        if (selectedTv == null && devices.isNotEmpty()) {
            selectedTv = devices.first()
        }
    }

    DisposableEffect(directTarget) {
        if (directTarget == null) discovery.startDiscovery()
        onDispose { discovery.close() }
    }
    BackHandler(onBack = onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.78f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(0.94f).widthIn(max = 620.dp).padding(12.dp)
            ) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(strings.transferSendToTv, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(strings.transferSendToTvDesc, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (sent) {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(10.dp))
                            Text(strings.transferSentToTv, fontWeight = FontWeight.SemiBold)
                            Text(strings.transferConfirmOnTv, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Text(strings.transferSourceExplanation, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(strings.transferChooseSource, fontWeight = FontWeight.SemiBold)
                        if (sendable.isEmpty()) {
                            Text(strings.transferNoSendableSources, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            LazyColumn(Modifier.fillMaxWidth().height(110.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(sendable, key = { it.id }) { profile ->
                                    SelectionRow(
                                        text = profile.name,
                                        selected = selectedProfile?.id == profile.id,
                                        onClick = { selectedProfile = profile; error = null }
                                    )
                                }
                            }
                        }

                        Text(
                            if (directTarget != null) strings.transferConnectedViaQr else strings.transferAvailableTvs,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (directTarget != null) {
                            SelectionRow(
                                text = directTarget.displayName,
                                selected = true,
                                leadingIcon = true,
                                onClick = {}
                            )
                        } else if (devices.isEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.width(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(10.dp))
                                Text(strings.transferSearchingTvs, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(Modifier.fillMaxWidth().height(120.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(devices, key = { "${it.serviceName}:${it.port}" }) { tv ->
                                    SelectionRow(
                                        text = tv.displayName,
                                        selected = selectedTv == tv,
                                        leadingIcon = true,
                                        onClick = { selectedTv = tv; error = null }
                                    )
                                }
                            }
                        }

                        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        if (directTarget == null && !sending) {
                            val hint = when {
                                selectedTv == null -> strings.transferSearchingTvs
                                selectedTv?.sessionToken == null -> strings.transferQrRequiredForSource
                                else -> null
                            }
                            if (hint != null) {
                                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Button(
                            onClick = {
                                val tv = selectedTv ?: return@Button
                                val profile = selectedProfile
                                sending = true
                                error = null
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        if (profile == null) NativeSendResult.Error("No source selected")
                                        else NativeTransferClient.sendSource(tv, profile, "", epgUrl)
                                    }
                                    sending = false
                                    when (result) {
                                        NativeSendResult.Success -> sent = true
                                        is NativeSendResult.Error -> error = if (result.message == "Scan the TV QR code to send source credentials securely") strings.transferQrRequiredForSource else result.message
                                    }
                                }
                            },
                            enabled = !sending && selectedTv != null && selectedProfile != null && selectedTv?.sessionToken != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (sending) {
                                CircularProgressIndicator(Modifier.width(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(if (sending) strings.transferSending else strings.transferSendNow)
                        }
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                        Text(if (sent) strings.close else strings.cancel)
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionRow(
    text: String,
    selected: Boolean,
    leadingIcon: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon) {
                Icon(Icons.Default.Tv, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
            }
            Text(text, modifier = Modifier.weight(1f), fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}
