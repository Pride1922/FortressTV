package com.fortresstv.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortresstv.player.playlist.M3uParser
import com.fortresstv.player.playlist.Playlist
import com.fortresstv.player.playlist.PlaylistChannel
import com.fortresstv.player.epg.EpgGuide
import com.fortresstv.player.epg.XmlTvParser
import com.fortresstv.player.ui.theme.FortressTVTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.util.zip.GZIPInputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FortressTVTheme { FortressTVApp() } }
    }
}

@Composable
private fun FortressTVApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var playlist by remember { mutableStateOf<Playlist?>(null) }
    var sourceName by remember { mutableStateOf<String?>(null) }
    var epgGuide by remember { mutableStateOf<EpgGuide?>(null) }
    var epgSourceName by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        loading = true
        error = null
        scope.launch {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                withContext(Dispatchers.IO) {
                    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("The selected playlist could not be opened")
                    M3uParser.parse(text)
                }
            }.onSuccess { parsed ->
                if (parsed.channels.isEmpty()) {
                    error = "No channels were found in this file."
                } else {
                    playlist = parsed
                    sourceName = uri.lastPathSegment?.substringAfterLast('/') ?: "M3U playlist"
                }
            }.onFailure { error = it.message ?: "Could not load this playlist." }
            loading = false
        }
    }

    val epgPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        loading = true
        error = null
        scope.launch {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                withContext(Dispatchers.IO) {
                    val raw = context.contentResolver.openInputStream(uri)
                        ?: error("The selected EPG file could not be opened")
                    BufferedInputStream(raw).use { buffered ->
                        buffered.mark(2)
                        val first = buffered.read()
                        val second = buffered.read()
                        buffered.reset()
                        val stream = if (first == 0x1f && second == 0x8b) GZIPInputStream(buffered) else buffered
                        XmlTvParser.parse(stream)
                    }
                }
            }.onSuccess { guide ->
                epgGuide = guide
                epgSourceName = uri.lastPathSegment?.substringAfterLast('/') ?: "XMLTV guide"
            }.onFailure { error = it.message ?: "Could not load this EPG file." }
            loading = false
        }
    }

    if (playlist == null) {
        WelcomeScreen(
            loading = loading,
            error = error,
            onOpenFile = { filePicker.launch(arrayOf("audio/x-mpegurl", "application/x-mpegURL", "text/*", "*/*")) }
        )
    } else {
        ChannelBrowser(
            playlist = playlist!!,
            sourceName = sourceName ?: "M3U playlist",
            epgGuide = epgGuide,
            epgSourceName = epgSourceName,
            loading = loading,
            error = error,
            onReplacePlaylist = { filePicker.launch(arrayOf("audio/x-mpegurl", "application/x-mpegURL", "text/*", "*/*")) },
            onOpenEpg = { epgPicker.launch(arrayOf("application/xml", "text/xml", "application/gzip", "*/*")) }
        )
    }
}

@Composable
private fun WelcomeScreen(loading: Boolean, error: String?, onOpenFile: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.widthIn(max = 680.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ShieldMark()
                Spacer(Modifier.height(28.dp))
                Text("FortressTV", fontSize = 42.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text("Your channels. Your fortress.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp)
                Spacer(Modifier.height(48.dp))
                Text("Add your playlist to start watching", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = onOpenFile, enabled = !loading, modifier = Modifier.weight(1f).height(56.dp)) {
                        Text(if (loading) "Loading…" else "Open M3U file")
                    }
                    OutlinedButton(onClick = { }, enabled = false, modifier = Modifier.weight(1f).height(56.dp)) {
                        Text("Playlist URL — next")
                    }
                }
                if (error != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(20.dp))
                Text("Playlist details stay on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ChannelBrowser(
    playlist: Playlist,
    sourceName: String,
    epgGuide: EpgGuide?,
    epgSourceName: String?,
    loading: Boolean,
    error: String?,
    onReplacePlaylist: () -> Unit,
    onOpenEpg: () -> Unit
) {
    val allGroup = "All channels"
    var selectedGroup by remember(playlist) { mutableStateOf(allGroup) }
    val visibleChannels = if (selectedGroup == allGroup) playlist.channels else playlist.channels.filter { it.group == selectedGroup }

    Row(Modifier.fillMaxSize().systemBarsPadding()) {
        Column(
            modifier = Modifier.width(250.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant).padding(20.dp)
        ) {
            Text("FortressTV", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(sourceName, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            GroupItem(allGroup, playlist.channels.size, selectedGroup == allGroup) { selectedGroup = allGroup }
            playlist.groups.forEach { group ->
                GroupItem(group, playlist.channels.count { it.group == group }, selectedGroup == group) { selectedGroup = group }
            }
            Spacer(Modifier.weight(1f))
            if (epgGuide == null) {
                Button(onClick = onOpenEpg, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                    Text(if (loading) "Loading EPG…" else "Load XMLTV EPG")
                }
            } else {
                val matched = playlist.channels.count { it.tvgId != null && epgGuide.channels.containsKey(it.tvgId) }
                Text("EPG: $matched/${playlist.channels.size} matched", fontWeight = FontWeight.SemiBold)
                Text(
                    epgSourceName ?: "XMLTV guide",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                TextButton(onClick = onOpenEpg, enabled = !loading) { Text("Replace EPG") }
            }
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onReplacePlaylist, modifier = Modifier.fillMaxWidth()) { Text("Replace playlist") }
        }
        Column(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 28.dp, vertical = 20.dp)) {
            Text(selectedGroup, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("${visibleChannels.size} channels · original playlist order", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visibleChannels, key = { "${it.position}:${it.streamUrl}" }) { channel ->
                    ChannelRow(channel, epgGuide?.channels?.get(channel.tvgId))
                }
            }
        }
    }
}

@Composable
private fun GroupItem(name: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .18f) else Color.Transparent)
            .clickable(onClick = onClick).padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(count.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChannelRow(channel: PlaylistChannel, epgChannel: com.fortresstv.player.epg.EpgChannel?) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) { Text(channel.name.take(1).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(channel.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    when {
                        channel.tvgId == null -> "No EPG ID"
                        epgChannel != null -> "EPG matched · ${channel.tvgId}"
                        else -> channel.tvgId
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
            Text("#${channel.position + 1}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ShieldMark() {
    Box(
        Modifier.size(104.dp).clip(RoundedCornerShape(30.dp)).background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) { Text("F", color = Color(0xFF061B19), fontSize = 52.sp, fontWeight = FontWeight.Black) }
}

@Preview(showBackground = true, widthDp = 1024, heightDp = 600)
@Composable
private fun WelcomePreview() = FortressTVTheme { WelcomeScreen(false, null) {} }
