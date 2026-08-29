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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.fortresstv.player.playlist.M3uParser
import com.fortresstv.player.playlist.Playlist
import com.fortresstv.player.playlist.PlaylistChannel
import com.fortresstv.player.playlist.searchChannels
import com.fortresstv.player.epg.EpgGuide
import com.fortresstv.player.epg.XmlTvParser
import com.fortresstv.player.source.ContentSource
import com.fortresstv.player.source.SourceReader
import com.fortresstv.player.source.SourceSettingsStore
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
    val settings = remember { SourceSettingsStore(context.applicationContext) }
    var playlist by remember { mutableStateOf<Playlist?>(null) }
    var playlistSource by remember { mutableStateOf<ContentSource?>(null) }
    var sourceName by remember { mutableStateOf<String?>(null) }
    var epgGuide by remember { mutableStateOf<EpgGuide?>(null) }
    var epgSource by remember { mutableStateOf<ContentSource?>(null) }
    var epgSourceName by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var restoring by remember { mutableStateOf(true) }
    var urlTarget by remember { mutableStateOf<UrlTarget?>(null) }
    var removeTarget by remember { mutableStateOf<UrlTarget?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    suspend fun parsePlaylist(source: ContentSource): Playlist = withContext(Dispatchers.IO) {
        SourceReader.open(context, source).bufferedReader().use { M3uParser.parse(it.readText()) }
    }

    suspend fun parseEpg(source: ContentSource): EpgGuide = withContext(Dispatchers.IO) {
        SourceReader.open(context, source).use { raw ->
            BufferedInputStream(raw).use { buffered ->
                buffered.mark(2)
                val first = buffered.read()
                val second = buffered.read()
                buffered.reset()
                val stream = if (first == 0x1f && second == 0x8b) GZIPInputStream(buffered) else buffered
                XmlTvParser.parse(stream)
            }
        }
    }

    fun loadPlaylist(source: ContentSource, save: Boolean) {
        loading = true
        error = null
        scope.launch {
            runCatching { parsePlaylist(source) }
                .onSuccess { parsed ->
                    if (parsed.channels.isEmpty()) error = "No channels were found in this playlist."
                    else {
                        playlist = parsed
                        playlistSource = source
                        sourceName = source.displayName
                        if (save) settings.savePlaylist(source)
                    }
                }
                .onFailure { error = it.message ?: "Could not load this playlist." }
            loading = false
        }
    }

    fun loadEpg(source: ContentSource, save: Boolean) {
        loading = true
        error = null
        scope.launch {
            runCatching { parseEpg(source) }
                .onSuccess { guide ->
                    epgGuide = guide
                    epgSource = source
                    epgSourceName = source.displayName
                    if (save) settings.saveEpg(source)
                }
                .onFailure { error = it.message ?: "Could not load this EPG source." }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        loading = true
        settings.playlistSource()?.let { source ->
            runCatching { parsePlaylist(source) }.onSuccess {
                if (it.channels.isNotEmpty()) {
                    playlist = it
                    playlistSource = source
                    sourceName = source.displayName
                }
            }.onFailure { error = "Saved playlist: ${it.message ?: "could not be loaded"}" }
        }
        settings.epgSource()?.let { source ->
            runCatching { parseEpg(source) }.onSuccess {
                epgGuide = it
                epgSource = source
                epgSourceName = source.displayName
            }.onFailure { error = "Saved EPG: ${it.message ?: "could not be loaded"}" }
        }
        loading = false
        restoring = false
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        loadPlaylist(ContentSource.LocalFile(uri.toString(), uri.lastPathSegment?.substringAfterLast('/') ?: "M3U playlist"), true)
    }

    val epgPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        loadEpg(ContentSource.LocalFile(uri.toString(), uri.lastPathSegment?.substringAfterLast('/') ?: "XMLTV guide"), true)
    }

    if (urlTarget != null) {
        val editingSource = if (urlTarget == UrlTarget.PLAYLIST) playlistSource else epgSource
        SourceUrlDialog(
            target = urlTarget!!,
            initialValue = (editingSource as? ContentSource.RemoteUrl)?.value.orEmpty(),
            onDismiss = { urlTarget = null },
            onConfirm = { value ->
                val source = ContentSource.RemoteUrl(value.trim(), value.substringAfterLast('/').ifBlank { value })
                if (urlTarget == UrlTarget.PLAYLIST) loadPlaylist(source, true) else loadEpg(source, true)
                urlTarget = null
            }
        )
    }

    if (removeTarget != null) {
        val target = removeTarget!!
        AlertDialog(
            onDismissRequest = { removeTarget = null },
            title = { Text(if (target == UrlTarget.PLAYLIST) "Remove playlist?" else "Remove EPG?") },
            text = {
                Text(
                    if (target == UrlTarget.PLAYLIST)
                        "FortressTV will forget this playlist source. The EPG setting will be kept."
                    else "FortressTV will forget this EPG source. Your playlist will be kept."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (target == UrlTarget.PLAYLIST) {
                        settings.clearPlaylist()
                        playlist = null
                        playlistSource = null
                        sourceName = null
                    } else {
                        settings.clearEpg()
                        epgGuide = null
                        epgSource = null
                        epgSourceName = null
                    }
                    error = null
                    removeTarget = null
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { removeTarget = null }) { Text("Cancel") } }
        )
    }

    if (showSettings) {
        SourceSettingsDialog(
            playlistSource = playlistSource,
            playlistName = sourceName,
            epgSource = epgSource,
            epgName = epgSourceName,
            loading = loading,
            onDismiss = { showSettings = false },
            onPlaylistFile = {
                showSettings = false
                filePicker.launch(arrayOf("audio/x-mpegurl", "application/x-mpegURL", "text/*", "*/*"))
            },
            onPlaylistUrl = {
                showSettings = false
                urlTarget = UrlTarget.PLAYLIST
            },
            onRemovePlaylist = {
                showSettings = false
                removeTarget = UrlTarget.PLAYLIST
            },
            onEpgFile = {
                showSettings = false
                epgPicker.launch(arrayOf("application/xml", "text/xml", "application/gzip", "*/*"))
            },
            onEpgUrl = {
                showSettings = false
                urlTarget = UrlTarget.EPG
            },
            onRemoveEpg = {
                showSettings = false
                removeTarget = UrlTarget.EPG
            }
        )
    }

    if (restoring) {
        WelcomeScreen(true, null, {}, {}, {})
    } else if (playlist == null) {
        WelcomeScreen(
            loading = loading,
            error = error,
            onOpenFile = { filePicker.launch(arrayOf("audio/x-mpegurl", "application/x-mpegURL", "text/*", "*/*")) },
            onOpenUrl = { urlTarget = UrlTarget.PLAYLIST },
            onOpenSettings = { showSettings = true }
        )
    } else {
        ChannelBrowser(
            playlist = playlist!!,
            sourceName = sourceName ?: "M3U playlist",
            epgGuide = epgGuide,
            epgSourceName = epgSourceName,
            loading = loading,
            error = error,
            onOpenSettings = { showSettings = true }
        )
    }
}

private enum class UrlTarget { PLAYLIST, EPG }

@Composable
private fun WelcomeScreen(
    loading: Boolean,
    error: String?,
    onOpenFile: () -> Unit,
    onOpenUrl: () -> Unit,
    onOpenSettings: () -> Unit
) {
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
                if (loading) {
                    LinearProgressIndicator(modifier = Modifier.width(320.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("Loading saved playlist and programme guide…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(20.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = onOpenFile, enabled = !loading, modifier = Modifier.weight(1f).height(56.dp)) {
                        Text(if (loading) "Loading…" else "Open M3U file")
                    }
                    OutlinedButton(onClick = onOpenUrl, enabled = !loading, modifier = Modifier.weight(1f).height(56.dp)) {
                        Text("Enter playlist URL")
                    }
                }
                if (error != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(20.dp))
                Text("Playlist details stay on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onOpenSettings) { Text("Settings") }
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
    onOpenSettings: () -> Unit
) {
    val allGroup = "All channels"
    var selectedGroup by remember(playlist) { mutableStateOf(allGroup) }
    var searchQuery by remember(playlist) { mutableStateOf("") }
    var selectedChannel by remember(playlist) { mutableStateOf<PlaylistChannel?>(null) }
    val channelsInGroup = if (selectedGroup == allGroup) playlist.channels else playlist.channels.filter { it.group == selectedGroup }
    val visibleChannels = channelsInGroup.searchChannels(searchQuery)

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Row(Modifier.fillMaxSize().systemBarsPadding()) {
        Column(
            modifier = Modifier.width(250.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface).padding(20.dp)
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
                Text("EPG not configured", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val matched = playlist.channels.count { it.tvgId != null && epgGuide.channels.containsKey(it.tvgId) }
                Text("EPG: $matched/${playlist.channels.size} matched", fontWeight = FontWeight.SemiBold)
                Text(
                    epgSourceName ?: "XMLTV guide",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onOpenSettings, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                Text("Settings")
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 28.dp, vertical = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search channels") },
                    singleLine = true,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Text("×", fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    modifier = Modifier.width(380.dp)
                )
                Text(
                    "${visibleChannels.size} ${if (visibleChannels.size == 1) "channel" else "channels"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    modifier = if (selectedChannel == null) Modifier.fillMaxSize() else Modifier.width(430.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visibleChannels, key = { "${it.position}:${it.streamUrl}" }) { channel ->
                        ChannelRow(
                            channel = channel,
                            epgChannel = epgGuide?.channels?.get(channel.tvgId),
                            selected = selectedChannel == channel,
                            onClick = { selectedChannel = channel }
                        )
                    }
                    if (visibleChannels.isEmpty()) {
                        item {
                            Box(Modifier.fillParentMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text("No channels match your search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                selectedChannel?.let { channel ->
                    Spacer(Modifier.width(16.dp))
                    PlayerPanel(channel = channel, modifier = Modifier.weight(1f).fillMaxHeight())
                }
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
private fun ChannelRow(
    channel: PlaylistChannel,
    epgChannel: com.fortresstv.player.epg.EpgChannel?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .18f)
        else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
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

@UnstableApi
@Composable
private fun PlayerPanel(channel: PlaylistChannel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply { playWhenReady = true }
    }
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    var playbackError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = error.message ?: "This channel could not be played."
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(channel.streamUrl) {
        playbackError = null
        player.setMediaItem(MediaItem.fromUri(channel.streamUrl))
        player.prepare()
        player.play()
    }

    Column(modifier) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(14.dp)).background(Color.Black)
        ) {
            AndroidView(
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        this.player = player
                        useController = true
                        controllerAutoShow = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        keepScreenOn = true
                    }
                },
                update = { it.player = player },
                modifier = Modifier.fillMaxSize()
            )
            if (playbackState == Player.STATE_BUFFERING) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
            }
            playbackError?.let { message ->
                Column(
                    Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(message, color = Color.White, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        playbackError = null
                        player.prepare()
                        player.play()
                    }) { Text("Retry") }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(channel.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Live channel", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ShieldMark() {
    Box(
        Modifier.size(104.dp).clip(RoundedCornerShape(30.dp)).background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) { Text("F", color = Color(0xFF061B19), fontSize = 52.sp, fontWeight = FontWeight.Black) }
}

@Composable
private fun SourceSettingsDialog(
    playlistSource: ContentSource?,
    playlistName: String?,
    epgSource: ContentSource?,
    epgName: String?,
    loading: Boolean,
    onDismiss: () -> Unit,
    onPlaylistFile: () -> Unit,
    onPlaylistUrl: () -> Unit,
    onRemovePlaylist: () -> Unit,
    onEpgFile: () -> Unit,
    onEpgUrl: () -> Unit,
    onRemoveEpg: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("FortressTV settings") },
        text = {
            Column(Modifier.widthIn(min = 520.dp, max = 680.dp)) {
                Text("Playlist", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    playlistName ?: "Not configured",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onPlaylistFile, enabled = !loading) {
                        Text(if (playlistSource is ContentSource.LocalFile) "Replace file" else "Use file")
                    }
                    OutlinedButton(onClick = onPlaylistUrl, enabled = !loading) {
                        Text(if (playlistSource is ContentSource.RemoteUrl) "Edit URL" else "Use URL")
                    }
                    TextButton(onClick = onRemovePlaylist, enabled = playlistSource != null && !loading) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 20.dp))
                Text("Programme guide (XMLTV)", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    epgName ?: "Not configured",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onEpgFile, enabled = !loading) {
                        Text(if (epgSource is ContentSource.LocalFile) "Replace file" else "Use file")
                    }
                    OutlinedButton(onClick = onEpgUrl, enabled = !loading) {
                        Text(if (epgSource is ContentSource.RemoteUrl) "Edit URL" else "Use URL")
                    }
                    TextButton(onClick = onRemoveEpg, enabled = epgSource != null && !loading) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "Saved source locations are encrypted on this device. Sources are refreshed when FortressTV starts.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun SourceUrlDialog(
    target: UrlTarget,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember(target, initialValue) { mutableStateOf(initialValue) }
    val label = if (target == UrlTarget.PLAYLIST) "Playlist URL" else "XMLTV EPG URL"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialValue.isBlank()) label else "Edit $label") },
        text = {
            Column {
                Text("Enter a direct HTTP or HTTPS URL. It will be encrypted on this device.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(label) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value) },
                enabled = value.startsWith("https://", true) || value.startsWith("http://", true)
            ) { Text("Load") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Preview(showBackground = true, widthDp = 1024, heightDp = 600)
@Composable
private fun WelcomePreview() = FortressTVTheme { WelcomeScreen(false, null, {}, {}, {}) }
