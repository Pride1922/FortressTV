package com.fortresstv.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import com.fortresstv.player.playlist.PlaylistProfile
import com.fortresstv.player.ui.AppLanguage
import com.fortresstv.player.ui.LocalAppStrings
import com.fortresstv.player.ui.getStringsFor
import com.fortresstv.player.ui.Strings
import java.util.UUID
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.fortresstv.player.player.GestureControl
import com.fortresstv.player.player.GestureLevel
import com.fortresstv.player.player.GesturePlayerView
import com.fortresstv.player.epg.EpgGuide
import com.fortresstv.player.epg.XmlTvParser
import com.fortresstv.player.epg.snapshot
import com.fortresstv.player.epg.XmlTvTime
import com.fortresstv.player.source.ContentSource
import com.fortresstv.player.source.SourceReader
import com.fortresstv.player.source.SourceSettingsStore
import com.fortresstv.player.ui.theme.FortressTVTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.util.zip.GZIPInputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import coil.compose.AsyncImage

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

    var playlists by remember { mutableStateOf<List<PlaylistProfile>>(emptyList()) }
    var appLanguage by remember { mutableStateOf(AppLanguage.ENGLISH) }
    val strings = getStringsFor(appLanguage)

    var activeProfile by remember { mutableStateOf<PlaylistProfile?>(null) }
    var playlist by remember { mutableStateOf<Playlist?>(null) }

    var epgGuide by remember { mutableStateOf<EpgGuide?>(null) }
    var epgSource by remember { mutableStateOf<ContentSource?>(null) }
    var epgSourceName by remember { mutableStateOf<String?>(null) }
    var epgLastUpdate by remember { mutableLongStateOf(0L) }
    var epgTimeShiftHours by remember { mutableIntStateOf(0) }

    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var restoring by remember { mutableStateOf(true) }

    var showSettings by remember { mutableStateOf(false) }
    var settingsInitialTab by remember { mutableIntStateOf(0) }
    var showAddPlaylistDialog by remember { mutableStateOf(false) }
    var renamingProfile by remember { mutableStateOf<PlaylistProfile?>(null) }
    var deletingProfile by remember { mutableStateOf<PlaylistProfile?>(null) }
    var showEpgUrlDialog by remember { mutableStateOf(false) }
    var showRemoveEpgConfirm by remember { mutableStateOf(false) }

    fun onLanguageChanged(newLang: AppLanguage) {
        appLanguage = newLang
        scope.launch(Dispatchers.IO) {
            settings.setLanguage(newLang.code)
        }
    }

    suspend fun parsePlaylist(stream: java.io.InputStream): Playlist = withContext(Dispatchers.IO) {
        stream.bufferedReader().use { M3uParser.parse(it.readText()) }
    }

    suspend fun parseEpg(stream: java.io.InputStream): EpgGuide = withContext(Dispatchers.IO) {
        BufferedInputStream(stream).use { buffered ->
            buffered.mark(2)
            val first = buffered.read()
            val second = buffered.read()
            buffered.reset()
            val decompressed = if (first == 0x1f && second == 0x8b) GZIPInputStream(buffered) else buffered
            XmlTvParser.parse(decompressed)
        }
    }

    fun loadPlaylistProfile(profile: PlaylistProfile, forceRefresh: Boolean = false) {
        loading = true
        error = null
        scope.launch(Dispatchers.IO) {
            runCatching {
                val cacheKey = "playlist_${profile.id}"
                val hasCache = SourceReader.hasCache(context, cacheKey) || SourceReader.hasCache(context, "playlist")
                val stream = if (forceRefresh || !hasCache) {
                    SourceReader.fetchAndCache(context, profile.source, cacheKey)
                } else if (SourceReader.hasCache(context, cacheKey)) {
                    SourceReader.openCached(context, cacheKey)
                } else {
                    SourceReader.openCached(context, "playlist")
                }
                val parsed = parsePlaylist(stream)
                val now = if (forceRefresh || !hasCache) System.currentTimeMillis() else profile.lastUpdated
                val updatedProfile = profile.copy(lastUpdated = now, channelCount = parsed.channels.size)
                settings.savePlaylistProfile(updatedProfile)
                settings.setActivePlaylistId(updatedProfile.id)
                val allPlaylists = settings.getPlaylists()
                withContext(Dispatchers.Main) {
                    playlist = parsed
                    activeProfile = updatedProfile
                    playlists = allPlaylists
                }
            }.onFailure { err ->
                withContext(Dispatchers.Main) {
                    error = err.message ?: "Could not load playlist."
                }
            }
            withContext(Dispatchers.Main) {
                loading = false
                restoring = false
            }
        }
    }

    fun loadEpg(source: ContentSource, save: Boolean, forceRefresh: Boolean = false) {
        loading = true
        error = null
        scope.launch(Dispatchers.IO) {
            runCatching {
                val stream = if (forceRefresh || !SourceReader.hasCache(context, "epg")) {
                    SourceReader.fetchAndCache(context, source, "epg").also {
                        val now = System.currentTimeMillis()
                        settings.setLastEpgUpdate(now)
                        withContext(Dispatchers.Main) { epgLastUpdate = now }
                    }
                } else {
                    SourceReader.openCached(context, "epg")
                }
                parseEpg(stream)
            }.onSuccess { guide ->
                withContext(Dispatchers.Main) {
                    epgGuide = guide
                    epgSource = source
                    epgSourceName = source.displayName
                    if (save) settings.saveEpg(source)
                }
            }.onFailure { err ->
                withContext(Dispatchers.Main) {
                    error = err.message ?: "Could not load EPG source."
                }
            }
            withContext(Dispatchers.Main) {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        val (savedLangCode, shift, epgUpdate) = withContext(Dispatchers.IO) {
            Triple(settings.getLanguage(), settings.epgTimeShiftHours(), settings.lastEpgUpdate())
        }
        appLanguage = AppLanguage.fromCode(savedLangCode)
        epgTimeShiftHours = shift
        epgLastUpdate = epgUpdate

        val (savedPlaylists, currentActive) = withContext(Dispatchers.IO) {
            settings.getPlaylists() to settings.getActivePlaylist()
        }
        playlists = savedPlaylists
        activeProfile = currentActive

        var hasFastLoadedPlaylist = false

        withContext(Dispatchers.IO) {
            if (currentActive != null) {
                val cacheKey = "playlist_${currentActive.id}"
                if (currentActive.source is ContentSource.LocalFile) {
                    runCatching {
                        parsePlaylist(SourceReader.open(context, currentActive.source))
                    }.onSuccess { parsed ->
                        if (parsed.channels.isNotEmpty()) {
                            withContext(Dispatchers.Main) { playlist = parsed }
                            hasFastLoadedPlaylist = true
                        }
                    }
                } else if (SourceReader.hasCache(context, cacheKey)) {
                    runCatching {
                        parsePlaylist(SourceReader.openCached(context, cacheKey))
                    }.onSuccess { parsed ->
                        if (parsed.channels.isNotEmpty()) {
                            withContext(Dispatchers.Main) { playlist = parsed }
                            hasFastLoadedPlaylist = true
                        }
                    }
                } else if (SourceReader.hasCache(context, "playlist")) {
                    runCatching {
                        parsePlaylist(SourceReader.openCached(context, "playlist"))
                    }.onSuccess { parsed ->
                        if (parsed.channels.isNotEmpty()) {
                            withContext(Dispatchers.Main) { playlist = parsed }
                            hasFastLoadedPlaylist = true
                        }
                    }
                }
            }

            val savedEpgSource = settings.epgSource()
            if (savedEpgSource != null) {
                withContext(Dispatchers.Main) {
                    epgSource = savedEpgSource
                    epgSourceName = savedEpgSource.displayName
                }
                if (savedEpgSource is ContentSource.LocalFile) {
                    runCatching {
                        parseEpg(SourceReader.open(context, savedEpgSource))
                    }.onSuccess { parsedGuide ->
                        withContext(Dispatchers.Main) { epgGuide = parsedGuide }
                    }
                } else if (SourceReader.hasCache(context, "epg")) {
                    runCatching {
                        parseEpg(SourceReader.openCached(context, "epg"))
                    }.onSuccess { parsedGuide ->
                        withContext(Dispatchers.Main) { epgGuide = parsedGuide }
                    }
                }
            }
        }

        if (hasFastLoadedPlaylist || currentActive == null) {
            restoring = false
        }

        if (currentActive != null && currentActive.source is ContentSource.RemoteUrl) {
            val cacheKey = "playlist_${currentActive.id}"
            val isFresh = SourceReader.isCacheFresh(context, cacheKey, currentActive.lastUpdated) ||
                    SourceReader.isCacheFresh(context, "playlist", currentActive.lastUpdated)
            if (!isFresh) {
                if (!hasFastLoadedPlaylist) loading = true
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        val stream = SourceReader.fetchAndCache(context, currentActive.source, cacheKey)
                        val now = System.currentTimeMillis()
                        val parsed = parsePlaylist(stream)
                        val updated = currentActive.copy(lastUpdated = now, channelCount = parsed.channels.size)
                        settings.savePlaylistProfile(updated)
                        val all = settings.getPlaylists()
                        withContext(Dispatchers.Main) {
                            playlist = parsed
                            activeProfile = updated
                            playlists = all
                        }
                    }.onFailure { err ->
                        withContext(Dispatchers.Main) {
                            if (!hasFastLoadedPlaylist) error = "Saved playlist: ${err.message ?: "could not be loaded"}"
                        }
                    }
                    withContext(Dispatchers.Main) {
                        loading = false
                        restoring = false
                    }
                }
            }
        }

        val savedEpg = settings.epgSource()
        if (savedEpg is ContentSource.RemoteUrl) {
            val isEpgFresh = SourceReader.isCacheFresh(context, "epg", settings.lastEpgUpdate())
            if (!isEpgFresh) {
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        val stream = SourceReader.fetchAndCache(context, savedEpg, "epg")
                        val now = System.currentTimeMillis()
                        settings.setLastEpgUpdate(now)
                        withContext(Dispatchers.Main) { epgLastUpdate = now }
                        parseEpg(stream)
                    }.onSuccess { guide ->
                        withContext(Dispatchers.Main) { epgGuide = guide }
                    }
                }
            }
        }
    }

    val welcomeFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "My Playlist"
        val profile = PlaylistProfile(
            id = UUID.randomUUID().toString(),
            name = name,
            source = ContentSource.LocalFile(uri.toString(), name)
        )
        loadPlaylistProfile(profile, forceRefresh = true)
    }

    val addPlaylistFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val defaultName = uri.lastPathSegment?.substringAfterLast('/') ?: "My Playlist"
        val profile = PlaylistProfile(
            id = UUID.randomUUID().toString(),
            name = defaultName,
            source = ContentSource.LocalFile(uri.toString(), defaultName)
        )
        loadPlaylistProfile(profile, forceRefresh = true)
    }

    val epgFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        loadEpg(ContentSource.LocalFile(uri.toString(), uri.lastPathSegment?.substringAfterLast('/') ?: "XMLTV guide"), save = true, forceRefresh = true)
    }

    CompositionLocalProvider(LocalAppStrings provides strings) {
        if (showAddPlaylistDialog) {
            AddPlaylistDialog(
                onDismiss = { showAddPlaylistDialog = false },
                onAddUrl = { name, url ->
                    showAddPlaylistDialog = false
                    val displayName = name.ifBlank { url.substringAfterLast('/').ifBlank { "Remote Playlist" } }
                    val profile = PlaylistProfile(
                        id = UUID.randomUUID().toString(),
                        name = displayName,
                        source = ContentSource.RemoteUrl(url.trim(), displayName)
                    )
                    loadPlaylistProfile(profile, forceRefresh = true)
                },
                onPickFile = {
                    showAddPlaylistDialog = false
                    addPlaylistFilePicker.launch(arrayOf("audio/x-mpegurl", "application/x-mpegURL", "text/*", "*/*"))
                }
            )
        }

        renamingProfile?.let { profile ->
            RenamePlaylistDialog(
                initialName = profile.name,
                onDismiss = { renamingProfile = null },
                onConfirm = { newName ->
                    renamingProfile = null
                    val updated = profile.copy(name = newName)
                    settings.savePlaylistProfile(updated)
                    playlists = settings.getPlaylists()
                    if (activeProfile?.id == profile.id) {
                        activeProfile = updated
                    }
                }
            )
        }

        deletingProfile?.let { profile ->
            ConfirmDeletePlaylistDialog(
                playlistName = profile.name,
                onDismiss = { deletingProfile = null },
                onConfirm = {
                    deletingProfile = null
                    settings.deletePlaylistProfile(profile.id)
                    SourceReader.deleteCache(context, "playlist_${profile.id}")
                    val remaining = settings.getPlaylists()
                    playlists = remaining
                    if (activeProfile?.id == profile.id) {
                        val next = settings.getActivePlaylist()
                        activeProfile = next
                        if (next != null) {
                            loadPlaylistProfile(next)
                        } else {
                            playlist = null
                        }
                    }
                }
            )
        }

        if (showEpgUrlDialog) {
            SourceUrlDialog(
                title = "XMLTV EPG URL",
                initialValue = (epgSource as? ContentSource.RemoteUrl)?.value.orEmpty(),
                onDismiss = { showEpgUrlDialog = false },
                onConfirm = { value ->
                    showEpgUrlDialog = false
                    val source = ContentSource.RemoteUrl(value.trim(), value.substringAfterLast('/').ifBlank { value })
                    loadEpg(source, save = true, forceRefresh = true)
                }
            )
        }

        if (showRemoveEpgConfirm) {
            AlertDialog(
                onDismissRequest = { showRemoveEpgConfirm = false },
                title = { Text(strings.removeEpg) },
                text = { Text("FortressTV will forget this EPG source and clear its local schedule cache. Playlists will not be affected.") },
                confirmButton = {
                    TextButton(onClick = {
                        showRemoveEpgConfirm = false
                        settings.clearEpg()
                        SourceReader.deleteCache(context, "epg")
                        epgGuide = null
                        epgSource = null
                        epgSourceName = null
                        epgLastUpdate = 0L
                    }) { Text(strings.delete, color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { showRemoveEpgConfirm = false }) { Text(strings.cancel) } }
            )
        }

        if (showSettings) {
            SettingsDialog(
                initialTab = settingsInitialTab,
                playlists = playlists,
                activeProfile = activeProfile,
                appLanguage = appLanguage,
                onLanguageChanged = ::onLanguageChanged,
                epgSource = epgSource,
                epgSourceName = epgSourceName,
                epgLastUpdate = epgLastUpdate,
                epgTimeShiftHours = epgTimeShiftHours,
                loading = loading,
                onDismiss = { showSettings = false },
                onSelectPlaylist = { profile ->
                    loadPlaylistProfile(profile)
                },
                onRefreshPlaylist = { profile ->
                    loadPlaylistProfile(profile, forceRefresh = true)
                },
                onAddPlaylist = {
                    showAddPlaylistDialog = true
                },
                onRenamePlaylist = { profile ->
                    renamingProfile = profile
                },
                onDeletePlaylist = { profile ->
                    deletingProfile = profile
                },
                onEpgUrl = {
                    showEpgUrlDialog = true
                },
                onEpgFile = {
                    epgFilePicker.launch(arrayOf("application/xml", "text/xml", "application/gzip", "*/*"))
                },
                onRefreshEpg = {
                    epgSource?.let { loadEpg(it, save = false, forceRefresh = true) }
                },
                onRemoveEpg = {
                    showRemoveEpgConfirm = true
                },
                onEpgTimeShiftChanged = { shift ->
                    epgTimeShiftHours = shift
                    settings.saveEpgTimeShiftHours(shift)
                }
            )
        }

        if (restoring) {
            WelcomeScreen(false, null, {}, {})
        } else if (playlist == null || activeProfile == null) {
            WelcomeScreen(
                loading = loading,
                error = error,
                onOpenFile = { welcomeFilePicker.launch(arrayOf("audio/x-mpegurl", "application/x-mpegURL", "text/*", "*/*")) },
                onOpenUrl = { showAddPlaylistDialog = true }
            )
        } else {
            ChannelBrowser(
                playlist = playlist!!,
                activeProfile = activeProfile!!,
                epgGuide = epgGuide,
                epgSourceName = epgSourceName,
                epgTimeShiftHours = epgTimeShiftHours,
                loading = loading,
                error = error,
                onOpenSettings = { tab ->
                    settingsInitialTab = tab
                    showSettings = true
                }
            )
        }
    }
}

private enum class UrlTarget { PLAYLIST, EPG }

@Composable
private fun WelcomeScreen(
    loading: Boolean,
    error: String?,
    onOpenFile: () -> Unit,
    onOpenUrl: () -> Unit
) {
    val strings = LocalAppStrings.current
    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.widthIn(max = 680.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ShieldMark(Modifier.size(164.dp))
                Spacer(Modifier.height(22.dp))
                Text(strings.appName, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(
                    strings.welcomeSubtitle,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(32.dp))
                if (loading) {
                    LinearProgressIndicator(modifier = Modifier.width(320.dp))
                    Spacer(Modifier.height(20.dp))
                }
                Row(
                    Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(onClick = onOpenFile, enabled = !loading, modifier = Modifier.weight(1f).height(56.dp)) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (loading) strings.loadingPlaylist else strings.browseLocalFile)
                    }
                    OutlinedButton(onClick = onOpenUrl, enabled = !loading, modifier = Modifier.weight(1f).height(56.dp)) {
                        Icon(Icons.Default.Link, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.remoteUrl)
                    }
                }
                if (error != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
            Text(
                "© 2026 Pride1922",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
private fun ChannelBrowser(
    playlist: Playlist,
    activeProfile: PlaylistProfile,
    epgGuide: EpgGuide?,
    epgSourceName: String?,
    epgTimeShiftHours: Int = 0,
    loading: Boolean,
    error: String?,
    onOpenSettings: (Int) -> Unit
) {
    val strings = LocalAppStrings.current
    val allGroup = strings.allChannels
    var isSidebarExpanded by remember { mutableStateOf(false) }
    var selectedGroup by remember(playlist, allGroup) { mutableStateOf(allGroup) }
    var searchQuery by remember(playlist) { mutableStateOf("") }
    var selectedChannel by remember(playlist) { mutableStateOf<PlaylistChannel?>(null) }
    fun selectChannel(channel: PlaylistChannel) {
        selectedChannel = channel
        isSidebarExpanded = false
    }
    var scheduleNow by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(epgGuide) {
        while (true) {
            scheduleNow = Instant.now()
            delay(60_000)
        }
    }
    val groupCounts = remember(playlist) {
        playlist.channels.groupingBy { it.group.orEmpty() }.eachCount()
    }
    val channelsInGroup = remember(playlist, selectedGroup, allGroup) {
        if (selectedGroup == allGroup) playlist.channels else playlist.channels.filter { it.group == selectedGroup }
    }
    val visibleChannels = remember(channelsInGroup, searchQuery) {
        channelsInGroup.searchChannels(searchQuery)
    }
    val channelListState = rememberLazyListState()

    val matchedCount = remember(playlist, epgGuide) {
        if (epgGuide == null) 0 else playlist.channels.count { it.tvgId != null && epgGuide.channels.containsKey(it.tvgId) }
    }

    LaunchedEffect(selectedGroup, searchQuery) {
        channelListState.scrollToItem(0)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Row(Modifier.fillMaxSize().systemBarsPadding()) {
        AnimatedVisibility(
            visible = isSidebarExpanded,
            enter = expandHorizontally() + fadeIn(),
            exit = shrinkHorizontally() + fadeOut()
        ) {
            Column(
                modifier = Modifier.width(260.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface).padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ShieldMark(Modifier.size(36.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(strings.appName, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { isSidebarExpanded = false }) {
                        Icon(Icons.Default.MenuOpen, contentDescription = strings.collapseSidebar)
                    }
                }
                Spacer(Modifier.height(14.dp))

                Surface(
                    onClick = { onOpenSettings(0) },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (activeProfile.source is ContentSource.RemoteUrl) Icons.Default.Link else Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                activeProfile.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                strings.channelsCount(playlist.channels.size),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = strings.switchPlaylist,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item(key = "group_all_channels") {
                        GroupItem(allGroup, playlist.channels.size, selectedGroup == allGroup) {
                            selectedGroup = allGroup
                            isSidebarExpanded = false
                        }
                    }
                    items(playlist.groups, key = { it }) { group ->
                        GroupItem(group, groupCounts[group] ?: 0, selectedGroup == group) {
                            selectedGroup = group
                            isSidebarExpanded = false
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                if (epgGuide == null) {
                    Text(strings.epgNotConfigured, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                } else {
                    Text(strings.epgMatched(matchedCount, playlist.channels.size), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text(
                        epgSourceName ?: strings.xmltvGuide,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp
                    )
                }
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { onOpenSettings(0) }, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.settings)
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isSidebarExpanded) {
                        FilledTonalIconButton(onClick = { isSidebarExpanded = true }) {
                            Icon(Icons.Default.Menu, contentDescription = strings.expandSidebar)
                        }
                    }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(strings.searchInGroup(selectedGroup)) },
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Text("×", fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        modifier = Modifier.width(360.dp)
                    )
                }
                Text(
                    strings.channelsCount(visibleChannels.size),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = channelListState,
                    modifier = if (selectedChannel == null) Modifier.fillMaxSize() else Modifier.width(430.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visibleChannels, key = { "${it.position}:${it.streamUrl}" }) { channel ->
                        val currentProg = epgGuide?.snapshot(channel.tvgId, scheduleNow, epgTimeShiftHours)?.current
                        val progress = if (currentProg != null && currentProg.stopEpochMilli != null) {
                            val adjustedStart = currentProg.startEpochMilli + (epgTimeShiftHours * 3600_000L)
                            val adjustedStop = currentProg.stopEpochMilli + (epgTimeShiftHours * 3600_000L)
                            if (adjustedStop > adjustedStart) {
                                ((scheduleNow.toEpochMilli() - adjustedStart).toFloat() / (adjustedStop - adjustedStart).toFloat()).coerceIn(0f, 1f)
                            } else null
                        } else null
                        val endTime = if (currentProg?.stopEpochMilli != null) {
                            val adjustedStop = currentProg.stopEpochMilli + (epgTimeShiftHours * 3600_000L)
                            strings.until(formatProgrammeTime(Instant.ofEpochMilli(adjustedStop)))
                        } else null
                        ChannelRow(
                            channel = channel,
                            epgChannel = epgGuide?.channels?.get(channel.tvgId),
                            nowPlaying = currentProg?.title,
                            endTime = endTime,
                            progress = progress,
                            selected = selectedChannel == channel,
                            onClick = { selectChannel(channel) }
                        )
                    }
                    if (visibleChannels.isEmpty()) {
                        item {
                            Box(Modifier.fillParentMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text(strings.noChannelsMatch, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                selectedChannel?.let { channel ->
                    Spacer(Modifier.width(16.dp))
                    PlayerPanel(
                        channel = channel,
                        epgGuide = epgGuide,
                        epgTimeShiftHours = epgTimeShiftHours,
                        channels = playlist.channels,
                        groups = playlist.groups,
                        onChannelSelected = ::selectChannel,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}
}

@Composable
private fun GroupItem(name: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = count.toString(),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ChannelRow(
    channel: PlaylistChannel,
    epgChannel: com.fortresstv.player.epg.EpgChannel?,
    nowPlaying: String?,
    endTime: String? = null,
    progress: Float? = null,
    selected: Boolean,
    onClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .18f)
        else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ChannelLogo(channel, epgChannel)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(channel.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    when {
                        nowPlaying != null -> nowPlaying
                        channel.tvgId == null -> strings.noEpgId
                        epgChannel != null -> strings.noCurrentProgramme
                        else -> strings.noEpgData
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (progress != null) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .15f)
                    )
                }
            }
            if (endTime != null) {
                Spacer(Modifier.width(10.dp))
                Text(
                    endTime,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ChannelLogo(channel: PlaylistChannel, epgChannel: com.fortresstv.player.epg.EpgChannel?) {
    val logoUrl = remember(channel.logoUrl, epgChannel?.iconUrl) { channel.logoUrl ?: epgChannel?.iconUrl }
    var failed by remember(logoUrl) { mutableStateOf(false) }
    Box(
        Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (logoUrl != null && !failed) {
            AsyncImage(
                model = logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize().padding(4.dp)
            )
        } else {
            Text(channel.name.take(1).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@UnstableApi
@Composable
private fun PlayerPanel(
    channel: PlaylistChannel,
    epgGuide: EpgGuide?,
    epgTimeShiftHours: Int = 0,
    channels: List<PlaylistChannel>,
    groups: List<String> = emptyList(),
    onChannelSelected: (PlaylistChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply { playWhenReady = true }
    }
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var fullscreen by remember { mutableStateOf(false) }
    val schedule = epgGuide?.snapshot(channel.tvgId, timeShiftHours = epgTimeShiftHours)

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = error.message ?: strings.playbackError
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
        if (!fullscreen) {
            VideoSurface(
                player = player,
                playbackState = playbackState,
                playbackError = playbackError,
                fullscreen = false,
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                onFullscreen = { fullscreen = true },
                onControlsVisibilityChanged = {},
                onRetry = {
                    playbackError = null
                    player.prepare()
                    player.play()
                },
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(14.dp))
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(channel.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        schedule?.current?.let { current ->
            val startEpoch = current.startEpochMilli + (epgTimeShiftHours * 3600_000L)
            val stopEpoch = current.stopEpochMilli?.let { it + (epgTimeShiftHours * 3600_000L) }
            val nowMilli = Instant.now().toEpochMilli()
            val progress = if (stopEpoch != null && stopEpoch > startEpoch) {
                ((nowMilli - startEpoch).toFloat() / (stopEpoch - startEpoch).toFloat()).coerceIn(0f, 1f)
            } else null
            Spacer(Modifier.height(8.dp))
            Text(strings.now, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(current.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            if (progress != null && stopEpoch != null) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .15f)
                )
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatProgrammeTime(Instant.ofEpochMilli(startEpoch)), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text(formatProgrammeTime(Instant.ofEpochMilli(stopEpoch)), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
            current.description?.let { description ->
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } ?: Text(strings.noCurrentProgramme, color = MaterialTheme.colorScheme.onSurfaceVariant)
        schedule?.next?.let { next ->
            Spacer(Modifier.height(14.dp))
            Text(strings.next, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(next.title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        }
    }

    if (fullscreen) {
        FullscreenPlayer(
            player = player,
            playbackState = playbackState,
            playbackError = playbackError,
            channel = channel,
            epgGuide = epgGuide,
            epgTimeShiftHours = epgTimeShiftHours,
            channels = channels,
            groups = groups,
            onChannelSelected = onChannelSelected,
            onExit = { fullscreen = false },
            onRetry = {
                playbackError = null
                player.prepare()
                player.play()
            }
        )
    }
}

@UnstableApi
@Composable
private fun VideoSurface(
    player: ExoPlayer,
    playbackState: Int,
    playbackError: String?,
    fullscreen: Boolean,
    resizeMode: Int,
    onFullscreen: () -> Unit,
    onControlsVisibilityChanged: (Boolean) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var controlsVisible by remember { mutableStateOf(true) }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    var gestureLevel by remember { mutableStateOf<GestureLevel?>(null) }
    LaunchedEffect(gestureLevel) {
        if (gestureLevel != null) {
            delay(1_500)
            gestureLevel = null
        }
    }
    LaunchedEffect(fullscreen, controlsVisible, playerView) {
        if (fullscreen && controlsVisible && playerView != null) {
            delay(5_000)
            playerView?.hideController()
            controlsVisible = false
            onControlsVisibilityChanged(false)
        }
    }
    Box(modifier.background(Color.Black)) {
        AndroidView(
            factory = { viewContext ->
                val view = if (fullscreen) {
                    GesturePlayerView(viewContext).apply {
                        onGestureLevelChanged = { gestureLevel = it }
                    }
                } else PlayerView(viewContext)
                view.apply {
                    playerView = this
                    this.player = player
                    useController = true
                    controllerAutoShow = true
                    controllerShowTimeoutMs = 5_000
                    this.resizeMode = resizeMode
                    keepScreenOn = true
                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility ->
                            controlsVisible = visibility == android.view.View.VISIBLE
                            onControlsVisibilityChanged(controlsVisible)
                        }
                    )
                    findViewById<android.view.View>(androidx.media3.ui.R.id.exo_settings)?.visibility = android.view.View.GONE
                    findViewById<android.view.View>(androidx.media3.ui.R.id.exo_fullscreen)?.visibility = android.view.View.GONE
                    findViewById<android.view.View>(androidx.media3.ui.R.id.exo_minimal_fullscreen)?.visibility = android.view.View.GONE
                }
            },
            update = {
                it.player = player
                it.resizeMode = resizeMode
            },
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
                Button(onClick = onRetry) { Text(strings.retry) }
            }
        }
        AnimatedVisibility(
            visible = gestureLevel != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(
                if (gestureLevel?.control == GestureControl.BRIGHTNESS) Alignment.CenterStart else Alignment.CenterEnd
            ).padding(24.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = .76f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (gestureLevel?.control == GestureControl.BRIGHTNESS) strings.brightness else strings.volume,
                        color = Color.White.copy(alpha = .75f),
                        fontSize = 13.sp
                    )
                    Text(
                        "${((gestureLevel?.value ?: 0f) * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        if (!fullscreen) {
            TextButton(
                onClick = onFullscreen,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = Color.Black.copy(alpha = .62f),
                    contentColor = Color.White
                )
            ) { Text(strings.fullscreen) }
        }
    }
}

@UnstableApi
@Composable
private fun FullscreenPlayer(
    player: ExoPlayer,
    playbackState: Int,
    playbackError: String?,
    channel: PlaylistChannel,
    epgGuide: EpgGuide?,
    epgTimeShiftHours: Int = 0,
    channels: List<PlaylistChannel>,
    groups: List<String> = emptyList(),
    onChannelSelected: (PlaylistChannel) -> Unit,
    onExit: () -> Unit,
    onRetry: () -> Unit
) {
    val strings = LocalAppStrings.current
    var controlsVisible by remember { mutableStateOf(true) }
    var showChannels by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(false) }
    var lockPromptVisible by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var resizeModeNotice by remember { mutableStateOf<String?>(null) }
    var scheduleNow by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(channel, epgGuide) {
        while (true) {
            scheduleNow = Instant.now()
            delay(30_000)
        }
    }
    LaunchedEffect(lockPromptVisible) {
        if (lockPromptVisible) {
            delay(2_500)
            lockPromptVisible = false
        }
    }
    LaunchedEffect(resizeModeNotice) {
        if (resizeModeNotice != null) {
            delay(1_200)
            resizeModeNotice = null
        }
    }
    val schedule = epgGuide?.snapshot(channel.tvgId, scheduleNow, epgTimeShiftHours)
    val channelIndex = channels.indexOf(channel)
    val previousChannel = channels.getOrNull(channelIndex - 1)
    val nextChannel = channels.getOrNull(channelIndex + 1)
    val currentStartEpoch = schedule?.current?.let { it.startEpochMilli + (epgTimeShiftHours * 3600_000L) }
    val currentStopEpoch = schedule?.current?.stopEpochMilli?.let { it + (epgTimeShiftHours * 3600_000L) }
    val nowMilli = scheduleNow.toEpochMilli()
    val programmeProgress = if (currentStartEpoch != null && currentStopEpoch != null && currentStopEpoch > currentStartEpoch) {
        ((nowMilli - currentStartEpoch).toFloat() / (currentStopEpoch - currentStartEpoch).toFloat()).coerceIn(0f, 1f)
    } else null
    Dialog(
        onDismissRequest = onExit,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        ImmersiveSystemBars()
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            VideoSurface(
                player = player,
                playbackState = playbackState,
                playbackError = playbackError,
                fullscreen = true,
                resizeMode = resizeMode,
                onFullscreen = onExit,
                onControlsVisibilityChanged = { controlsVisible = it },
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize()
            )
            AnimatedVisibility(
                visible = controlsVisible && !showChannels && !showGuide,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.align(Alignment.TopStart).padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledIconButton(
                            onClick = onExit,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.Black.copy(alpha = .68f),
                                contentColor = Color.White
                            )
                        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back) }
                        FilledIconButton(
                            onClick = { showChannels = true },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.Black.copy(alpha = .68f),
                                contentColor = Color.White
                            )
                        ) { Icon(Icons.Default.Menu, contentDescription = strings.channelList) }
                        FilledIconButton(
                            onClick = { showGuide = true },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.Black.copy(alpha = .68f),
                                contentColor = Color.White
                            )
                        ) { Icon(Icons.Default.DateRange, contentDescription = strings.programmeGuide) }
                    }
                    Row(
                        modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledIconButton(
                            onClick = {
                                resizeMode = when (resizeMode) {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT -> {
                                        resizeModeNotice = strings.aspectFill
                                        AspectRatioFrameLayout.RESIZE_MODE_FILL
                                    }
                                    AspectRatioFrameLayout.RESIZE_MODE_FILL -> {
                                        resizeModeNotice = strings.aspectZoom
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    }
                                    else -> {
                                        resizeModeNotice = strings.aspectFit
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.Black.copy(alpha = .68f),
                                contentColor = Color.White
                            )
                        ) { Icon(Icons.Default.AspectRatio, contentDescription = strings.aspectRatio) }
                        FilledIconButton(
                            onClick = {
                                locked = true
                                controlsVisible = false
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.Black.copy(alpha = .68f),
                                contentColor = Color.White
                            )
                        ) { Icon(Icons.Default.Lock, contentDescription = strings.lockControls) }
                    }
                    Row(
                        modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = { previousChannel?.let(onChannelSelected) },
                            enabled = previousChannel != null,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.Black.copy(alpha = .68f),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.size(54.dp)
                        ) { Icon(Icons.Default.SkipPrevious, contentDescription = null, modifier = Modifier.size(30.dp)) }
                        FilledIconButton(
                            onClick = { nextChannel?.let(onChannelSelected) },
                            enabled = nextChannel != null,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.Black.copy(alpha = .68f),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.size(54.dp)
                        ) { Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(30.dp)) }
                    }
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.Black.copy(alpha = .85f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = .15f))
                    ) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ChannelLogo(channel, epgGuide?.channels?.get(channel.tvgId))
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(channel.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    schedule?.current?.let { current ->
                                        Text(
                                            current.title,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } ?: Text(strings.noCurrentProgramme, color = Color.White.copy(alpha = .68f), fontSize = 13.sp)
                                }
                                schedule?.next?.let { next ->
                                    Spacer(Modifier.width(16.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.White.copy(alpha = .1f),
                                        modifier = Modifier.widthIn(max = 280.dp)
                                    ) {
                                        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(strings.whatsNext, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                val nextStartEpoch = next.startEpochMilli + (epgTimeShiftHours * 3600_000L)
                                                if (nextStartEpoch > 0L) {
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("· ${formatProgrammeTime(Instant.ofEpochMilli(nextStartEpoch))}", color = Color.White.copy(alpha = .6f), fontSize = 10.sp)
                                                }
                                            }
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                next.title,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            if (programmeProgress != null && currentStopEpoch != null && currentStartEpoch != null) {
                                Spacer(Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { programmeProgress },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = Color.White.copy(alpha = .2f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        formatProgrammeTime(Instant.ofEpochMilli(currentStartEpoch)),
                                        color = Color.White.copy(alpha = .65f),
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        strings.endsAt(formatProgrammeTime(Instant.ofEpochMilli(currentStopEpoch))),
                                        color = Color.White.copy(alpha = .65f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = showChannels,
                enter = slideInHorizontally { -it } + fadeIn(),
                exit = slideOutHorizontally { -it } + fadeOut(),
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                FullscreenChannelDrawer(
                    channels = channels,
                    groups = groups,
                    selectedChannel = channel,
                    epgGuide = epgGuide,
                    epgTimeShiftHours = epgTimeShiftHours,
                    scheduleNow = scheduleNow,
                    onChannelSelected = {
                        onChannelSelected(it)
                        showChannels = false
                    },
                    onClose = { showChannels = false }
                )
            }
            AnimatedVisibility(
                visible = showGuide,
                enter = slideInHorizontally { it } + fadeIn(),
                exit = slideOutHorizontally { it } + fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                FullscreenGuideDrawer(
                    channel = channel,
                    epgGuide = epgGuide,
                    epgTimeShiftHours = epgTimeShiftHours,
                    onClose = { showGuide = false }
                )
            }
            AnimatedVisibility(
                visible = resizeModeNotice != null && !locked,
                enter = fadeIn(), exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(color = Color.Black.copy(alpha = .76f), shape = RoundedCornerShape(14.dp)) {
                    Text(resizeModeNotice.orEmpty(), color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp))
                }
            }
            if (locked) {
                Box(
                    Modifier.fillMaxSize().clickable {
                        lockPromptVisible = true
                    }
                ) {
                    AnimatedVisibility(
                        visible = lockPromptVisible,
                        enter = fadeIn(), exit = fadeOut(),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        FilledIconButton(
                            onClick = {
                                locked = false
                                lockPromptVisible = false
                                controlsVisible = true
                            },
                            modifier = Modifier.size(68.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.Black.copy(alpha = .76f), contentColor = Color.White
                            )
                        ) { Icon(Icons.Default.LockOpen, contentDescription = strings.unlockControls, modifier = Modifier.size(32.dp)) }
                    }
                }
            }
        }
    }
}

private val programmeTimeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

private fun formatProgrammeTime(instant: Instant): String = programmeTimeFormatter.format(instant)

@Composable
private fun FullscreenGuideDrawer(
    channel: PlaylistChannel,
    epgGuide: EpgGuide?,
    epgTimeShiftHours: Int = 0,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val now = Instant.now()
    val programmes = epgGuide?.programmes?.get(channel.tvgId).orEmpty()
        .filter { programme -> XmlTvTime.parse(programme.stop ?: programme.start, epgTimeShiftHours)?.let { it > now } == true }
        .sortedBy { it.startEpochMilli }
    Surface(
        modifier = modifier.fillMaxHeight().width(560.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .98f),
        shadowElevation = 12.dp
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(strings.programmeGuide, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text(channel.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = strings.close) }
            }
            Spacer(Modifier.height(16.dp))
            if (programmes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(strings.noUpcomingProgramme, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(programmes, key = { "${it.channelId}:${it.start}:${it.title}" }) { programme ->
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                Text(
                                    formatProgrammeRange(programme.start, programme.stop, epgTimeShiftHours),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(programme.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                                programme.description?.let {
                                    Text(
                                        it,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatProgrammeRange(start: String, stop: String?, timeShiftHours: Int = 0): String {
    val formatter = DateTimeFormatter.ofPattern("EEE d MMM · HH:mm")
    val startText = XmlTvTime.parse(start, timeShiftHours)?.atZone(ZoneId.systemDefault())?.format(formatter) ?: start
    val stopText = stop?.let { XmlTvTime.parse(it, timeShiftHours) }?.atZone(ZoneId.systemDefault())?.format(DateTimeFormatter.ofPattern("HH:mm"))
    return if (stopText == null) startText else "$startText – $stopText"
}

@Composable
private fun FullscreenChannelDrawer(
    channels: List<PlaylistChannel>,
    groups: List<String> = emptyList(),
    selectedChannel: PlaylistChannel,
    epgGuide: EpgGuide?,
    epgTimeShiftHours: Int = 0,
    scheduleNow: Instant,
    onChannelSelected: (PlaylistChannel) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val allGroup = strings.allChannels
    var selectedGroup by remember(selectedChannel, allGroup) {
        mutableStateOf(selectedChannel.group ?: allGroup)
    }
    var query by remember { mutableStateOf("") }
    val channelsInGroup = remember(channels, selectedGroup, allGroup) {
        if (selectedGroup == allGroup) channels else channels.filter { it.group == selectedGroup }
    }
    val visibleChannels = remember(channelsInGroup, query) {
        channelsInGroup.searchChannels(query)
    }
    val listState = rememberLazyListState()
    val selectedIndex = remember(visibleChannels, selectedChannel) {
        visibleChannels.indexOf(selectedChannel).coerceAtLeast(0)
    }
    LaunchedEffect(selectedGroup, query) {
        listState.scrollToItem(0)
    }

    Surface(
        modifier = modifier.fillMaxHeight().width(460.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .98f),
        shadowElevation = 16.dp
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(strings.channelList, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onClose) { Text(strings.close) }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(strings.searchInGroup(selectedGroup)) },
                singleLine = true,
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Text("×", fontSize = 24.sp) }
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (groups.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedGroup == allGroup,
                            onClick = { selectedGroup = allGroup },
                            label = { Text("${strings.allChannels} (${channels.size})", fontSize = 12.sp) }
                        )
                    }
                    items(groups, key = { it }) { group ->
                        FilterChip(
                            selected = selectedGroup == group,
                            onClick = { selectedGroup = group },
                            label = { Text(group, fontSize = 12.sp) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visibleChannels, key = { "fullscreen:${it.position}:${it.streamUrl}" }) { item ->
                    val currentProg = epgGuide?.snapshot(item.tvgId, scheduleNow, epgTimeShiftHours)?.current
                    val progress = if (currentProg != null && currentProg.stopEpochMilli != null) {
                        val adjustedStart = currentProg.startEpochMilli + (epgTimeShiftHours * 3600_000L)
                        val adjustedStop = currentProg.stopEpochMilli + (epgTimeShiftHours * 3600_000L)
                        if (adjustedStop > adjustedStart) {
                            ((scheduleNow.toEpochMilli() - adjustedStart).toFloat() / (adjustedStop - adjustedStart).toFloat()).coerceIn(0f, 1f)
                        } else null
                    } else null
                    val endTime = if (currentProg?.stopEpochMilli != null) {
                        val adjustedStop = currentProg.stopEpochMilli + (epgTimeShiftHours * 3600_000L)
                        strings.until(formatProgrammeTime(Instant.ofEpochMilli(adjustedStop)))
                    } else null
                    ChannelRow(
                        channel = item,
                        epgChannel = epgGuide?.channels?.get(item.tvgId),
                        nowPlaying = currentProg?.title,
                        endTime = endTime,
                        progress = progress,
                        selected = item == selectedChannel,
                        onClick = { onChannelSelected(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ImmersiveSystemBars() {
    val view = LocalView.current
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity, view) {
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        val window = dialogWindow ?: activity?.window
        window?.let { WindowCompat.setDecorFitsSystemWindows(it, false) }
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            window?.let { WindowCompat.setDecorFitsSystemWindows(it, true) }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun ShieldMark(modifier: Modifier = Modifier) {
    AsyncImage(
        model = R.drawable.fortress_tv_logo,
        contentDescription = "FortressTV logo",
        contentScale = ContentScale.Fit,
        modifier = modifier.clip(RoundedCornerShape(22.dp))
    )
}

private fun formatLastUpdated(timestamp: Long): String {
    if (timestamp == 0L) return "Not cached yet"
    val diff = System.currentTimeMillis() - timestamp
    val hours = diff / (1000 * 60 * 60)
    return when {
        diff < 60_000 -> "Updated just now"
        diff < 3600_000 -> "Updated ${diff / 60_000}m ago"
        hours < 24 -> "Updated ${hours}h ago"
        else -> {
            val formatter = DateTimeFormatter.ofPattern("d MMM · HH:mm")
            "Updated " + Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(formatter)
        }
    }
}

@Composable
private fun SettingsDialog(
    initialTab: Int = 0,
    playlists: List<PlaylistProfile>,
    activeProfile: PlaylistProfile?,
    appLanguage: AppLanguage,
    onLanguageChanged: (AppLanguage) -> Unit,
    epgSource: ContentSource?,
    epgSourceName: String?,
    epgLastUpdate: Long,
    epgTimeShiftHours: Int,
    loading: Boolean,
    onDismiss: () -> Unit,
    onSelectPlaylist: (PlaylistProfile) -> Unit,
    onRefreshPlaylist: (PlaylistProfile) -> Unit,
    onAddPlaylist: () -> Unit,
    onRenamePlaylist: (PlaylistProfile) -> Unit,
    onDeletePlaylist: (PlaylistProfile) -> Unit,
    onEpgUrl: () -> Unit,
    onEpgFile: () -> Unit,
    onRefreshEpg: () -> Unit,
    onRemoveEpg: () -> Unit,
    onEpgTimeShiftChanged: (Int) -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val tabTitles = listOf(strings.tabPlaylists, strings.tabEpg, strings.tabGeneral)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .widthIn(min = 600.dp, max = 780.dp)
                .fillMaxHeight(0.88f)
                .padding(16.dp)
        ) {
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.settingsTitle, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = strings.close)
                    }
                }

                Spacer(Modifier.height(12.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (selectedTab) {
                        0 -> PlaylistsTab(
                            playlists = playlists,
                            activeProfile = activeProfile,
                            loading = loading,
                            onSelectPlaylist = onSelectPlaylist,
                            onRefreshPlaylist = onRefreshPlaylist,
                            onAddPlaylist = onAddPlaylist,
                            onRenamePlaylist = onRenamePlaylist,
                            onDeletePlaylist = onDeletePlaylist
                        )
                        1 -> EpgTab(
                            epgSource = epgSource,
                            epgSourceName = epgSourceName,
                            epgLastUpdate = epgLastUpdate,
                            epgTimeShiftHours = epgTimeShiftHours,
                            loading = loading,
                            onEpgUrl = onEpgUrl,
                            onEpgFile = onEpgFile,
                            onRefreshEpg = onRefreshEpg,
                            onRemoveEpg = onRemoveEpg,
                            onEpgTimeShiftChanged = onEpgTimeShiftChanged
                        )
                        2 -> AboutTab(
                            appLanguage = appLanguage,
                            onLanguageChanged = onLanguageChanged
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistsTab(
    playlists: List<PlaylistProfile>,
    activeProfile: PlaylistProfile?,
    loading: Boolean,
    onSelectPlaylist: (PlaylistProfile) -> Unit,
    onRefreshPlaylist: (PlaylistProfile) -> Unit,
    onAddPlaylist: () -> Unit,
    onRenamePlaylist: (PlaylistProfile) -> Unit,
    onDeletePlaylist: (PlaylistProfile) -> Unit
) {
    val strings = LocalAppStrings.current
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${playlists.size} ${if (playlists.size == 1) "Playlist" else "Playlists"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Button(onClick = onAddPlaylist, enabled = !loading) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.addPlaylist)
            }
        }

        Spacer(Modifier.height(12.dp))

        if (playlists.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("No playlists configured", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Add your M3U / M3U8 URL or local file to get started.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(playlists, key = { it.id }) { item ->
                    val isActive = item.id == activeProfile?.id
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isActive) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (item.source is ContentSource.RemoteUrl) Icons.Default.Link else Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    if (isActive) {
                                        Spacer(Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ) {
                                            Text(
                                                strings.active.uppercase(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "${if (item.channelCount > 0) "${strings.channelsCount(item.channelCount)} · " else ""}${formatLastUpdated(item.lastUpdated)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    item.source.value,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (!isActive) {
                                    OutlinedButton(onClick = { onSelectPlaylist(item) }, enabled = !loading) {
                                        Text(strings.switchAction)
                                    }
                                }
                                if (item.source is ContentSource.RemoteUrl) {
                                    IconButton(onClick = { onRefreshPlaylist(item) }, enabled = !loading) {
                                        Icon(Icons.Default.Refresh, contentDescription = strings.refreshPlaylist)
                                    }
                                }
                                IconButton(onClick = { onRenamePlaylist(item) }, enabled = !loading) {
                                    Icon(Icons.Default.Edit, contentDescription = strings.rename)
                                }
                                IconButton(onClick = { onDeletePlaylist(item) }, enabled = !loading) {
                                    Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpgTab(
    epgSource: ContentSource?,
    epgSourceName: String?,
    epgLastUpdate: Long,
    epgTimeShiftHours: Int,
    loading: Boolean,
    onEpgUrl: () -> Unit,
    onEpgFile: () -> Unit,
    onRefreshEpg: () -> Unit,
    onRemoveEpg: () -> Unit,
    onEpgTimeShiftChanged: (Int) -> Unit
) {
    val strings = LocalAppStrings.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(strings.xmltvSource, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Schedule listings and channel programme timelines.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            epgSourceName ?: strings.noEpgConfiguredMsg,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (epgSource != null) {
                            Text(
                                formatLastUpdated(epgLastUpdate),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (epgSource is ContentSource.RemoteUrl) {
                        Button(onClick = onRefreshEpg, enabled = !loading) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.refresh)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onEpgUrl, enabled = !loading) {
                        Text(if (epgSource is ContentSource.RemoteUrl) strings.edit else strings.remoteUrl)
                    }
                    OutlinedButton(onClick = onEpgFile, enabled = !loading) {
                        Text(if (epgSource is ContentSource.LocalFile) strings.edit else strings.localFile)
                    }
                    if (epgSource != null) {
                        TextButton(onClick = onRemoveEpg, enabled = !loading) {
                            Text(strings.removeEpg, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(20.dp))

        Text(strings.timeShift, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            strings.timeShiftDesc,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { if (epgTimeShiftHours > -23) onEpgTimeShiftChanged(epgTimeShiftHours - 1) },
                enabled = epgTimeShiftHours > -23 && !loading
            ) {
                Text("-1 h", fontWeight = FontWeight.Bold)
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.defaultMinSize(minWidth = 110.dp)
            ) {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                    val shiftDisplay = when {
                        epgTimeShiftHours > 0 -> "+$epgTimeShiftHours h"
                        epgTimeShiftHours < 0 -> "$epgTimeShiftHours h"
                        else -> "0 h"
                    }
                    Text(shiftDisplay, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            OutlinedButton(
                onClick = { if (epgTimeShiftHours < 23) onEpgTimeShiftChanged(epgTimeShiftHours + 1) },
                enabled = epgTimeShiftHours < 23 && !loading
            ) {
                Text("+1 h", fontWeight = FontWeight.Bold)
            }
            if (epgTimeShiftHours != 0) {
                TextButton(
                    onClick = { onEpgTimeShiftChanged(0) },
                    enabled = !loading
                ) {
                    Text(strings.retry)
                }
            }
        }
    }
}

@Composable
private fun AboutTab(
    appLanguage: AppLanguage,
    onLanguageChanged: (AppLanguage) -> Unit
) {
    val strings = LocalAppStrings.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShieldMark(Modifier.size(64.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(strings.appName, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("${strings.appVersion} · Pride1922", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(20.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(strings.language, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppLanguage.entries.forEach { lang ->
                        val isSelected = lang == appLanguage
                        Surface(
                            onClick = { onLanguageChanged(lang) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(lang.flag, fontSize = 22.sp)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        lang.displayName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(strings.securityTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    strings.securityDesc,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun AddPlaylistDialog(
    onDismiss: () -> Unit,
    onAddUrl: (String, String) -> Unit,
    onPickFile: () -> Unit
) {
    val strings = LocalAppStrings.current
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var mode by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addPlaylist) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = mode,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(selected = mode == 0, onClick = { mode = 0 }, text = { Text(strings.remoteUrl) })
                    Tab(selected = mode == 1, onClick = { mode = 1 }, text = { Text(strings.localFile) })
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.playlistName) },
                    placeholder = { Text(strings.playlistNamePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                if (mode == 0) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text(strings.playlistM3u) },
                        placeholder = { Text(strings.enterM3uUrl) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        strings.orSelectLocalFile,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            if (mode == 0) {
                Button(
                    onClick = { onAddUrl(name.trim(), url.trim()) },
                    enabled = url.startsWith("http://", true) || url.startsWith("https://", true)
                ) { Text(strings.addPlaylist) }
            } else {
                Button(onClick = onPickFile) { Text(strings.selectFile) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun RenamePlaylistDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.rename) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(strings.playlistName) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun ConfirmDeletePlaylistDialog(
    playlistName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.deletePlaylistConfirmTitle) },
        text = {
            Text(strings.deletePlaylistConfirmMsg(playlistName))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(strings.delete, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun SourceUrlDialog(
    title: String = "Source URL",
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text("Enter a direct HTTP or HTTPS URL. It will be encrypted on this device.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value) },
                enabled = value.startsWith("https://", true) || value.startsWith("http://", true)
            ) { Text(strings.loadPlaylist) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Preview(showBackground = true, widthDp = 1024, heightDp = 600)
@Composable
private fun WelcomePreview() = FortressTVTheme { WelcomeScreen(false, null, {}, {}) }
