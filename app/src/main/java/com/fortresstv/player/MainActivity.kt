package com.fortresstv.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalConfiguration
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import com.fortresstv.player.playlist.PlaylistProfile
import com.fortresstv.player.update.UpdateManager
import com.fortresstv.player.update.AppUpdateInfo
import com.fortresstv.player.BuildConfig
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
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
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.fortresstv.player.playlist.M3uParser
import com.fortresstv.player.playlist.Playlist
import com.fortresstv.player.playlist.PlaylistChannel
import com.fortresstv.player.playlist.mergeLivePlaylists
import com.fortresstv.player.playlist.SourceRole
import com.fortresstv.player.playlist.searchChannels
import com.fortresstv.player.player.GestureControl
import com.fortresstv.player.player.GestureLevel
import com.fortresstv.player.player.GesturePlayerView
import com.fortresstv.player.epg.EpgGuide
import com.fortresstv.player.epg.EpgProgramme
import com.fortresstv.player.epg.EpgDiskCache
import com.fortresstv.player.epg.XmlTvParser
import com.fortresstv.player.epg.snapshot
import com.fortresstv.player.epg.XmlTvTime
import com.fortresstv.player.source.ContentSource
import com.fortresstv.player.source.SourceReader
import com.fortresstv.player.source.SourceSettingsStore
import com.fortresstv.player.library.UserLibraryStore
import com.fortresstv.player.library.UserLibraryStore.Companion.libraryId
import com.fortresstv.player.ui.theme.FortressTVTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.util.zip.GZIPInputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import coil.compose.AsyncImage
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache

class MainActivity : ComponentActivity() {
    var pictureInPictureMode by mutableStateOf(false)
        private set

    private var pictureInPictureEnabled = true
    private var fullscreenVideoPlaying = false
    private val pictureInPictureHandler = Handler(Looper.getMainLooper())
    private var onPictureInPictureDismissed: (() -> Unit)? = null
    private val confirmPictureInPictureDismissed = Runnable {
        if (!isInPictureInPictureMode && !hasWindowFocus()) {
            onPictureInPictureDismissed?.invoke()
        }
    }

    fun setOnPictureInPictureDismissedListener(listener: (() -> Unit)?) {
        onPictureInPictureDismissed = listener
    }

    fun updatePictureInPictureState(enabled: Boolean, fullscreenPlaying: Boolean) {
        pictureInPictureEnabled = enabled
        fullscreenVideoPlaying = fullscreenPlaying
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setPictureInPictureParams(
                PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            setAutoEnterEnabled(enabled && fullscreenPlaying)
                            setSeamlessResizeEnabled(true)
                        }
                    }
                    .build()
            )
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (
            Build.VERSION.SDK_INT in Build.VERSION_CODES.O until Build.VERSION_CODES.S &&
            pictureInPictureEnabled && fullscreenVideoPlaying
        ) {
            enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
            )
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pictureInPictureMode = isInPictureInPictureMode
        pictureInPictureHandler.removeCallbacks(confirmPictureInPictureDismissed)
        if (!isInPictureInPictureMode) {
            // Android can delay onStop for several seconds after the PiP close button
            // is pressed. Give an expanded PiP a moment to regain window focus; if it
            // does not, stop playback without waiting for that delayed lifecycle event.
            pictureInPictureHandler.postDelayed(confirmPictureInPictureDismissed, 350L)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            pictureInPictureHandler.removeCallbacks(confirmPictureInPictureDismissed)
        }
    }

    override fun onDestroy() {
        pictureInPictureHandler.removeCallbacksAndMessages(null)
        onPictureInPictureDismissed = null
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        UpdateManager.resumePendingInstall(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            // Channel logos are shown in fast-scrolling lazy lists. Crossfading every
            // newly visible logo adds animation and drawing work to the scroll frame.
            .crossfade(false)
            .build()
        Coil.setImageLoader(imageLoader)

        setContent {
            FortressTVTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .semantics { testTagsAsResourceId = true }
                ) {
                    FortressTVApp()
                }
            }
        }
    }
}

@Composable
private fun FortressTVApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { SourceSettingsStore(context.applicationContext) }
    val userLibrary = remember { UserLibraryStore(context.applicationContext) }

    var playlists by remember { mutableStateOf<List<PlaylistProfile>>(emptyList()) }
    var appLanguage by remember { mutableStateOf(AppLanguage.fromSystemLocale()) }
    var playChannelsFullscreen by remember { mutableStateOf(false) }
    var autoPictureInPicture by remember { mutableStateOf(true) }
    val strings = getStringsFor(appLanguage)

    var activeProfile by remember { mutableStateOf<PlaylistProfile?>(null) }
    var playlist by remember { mutableStateOf<Playlist?>(null) }
    val loadedPlaylists = remember { mutableStateMapOf<String, Playlist>() }
    var favoriteChannelIds by remember { mutableStateOf(userLibrary.favoriteChannelIds()) }

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

    var startupUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var startupUpdateProgress by remember { mutableFloatStateOf(-1f) }
    var startupUpdateDownloaded by remember { mutableStateOf(false) }
    var startupUpdateError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val info = UpdateManager.checkForUpdates(BuildConfig.VERSION_CODE)
        if (info != null) {
            val hasPendingInstall = withContext(Dispatchers.IO) {
                UpdateManager.hasPendingInstall(context)
            }
            startupUpdateInfo = info
            if (hasPendingInstall) {
                startupUpdateProgress = 1f
                startupUpdateDownloaded = true
            }
        }
    }

    fun onLanguageChanged(newLang: AppLanguage) {
        appLanguage = newLang
        scope.launch(Dispatchers.IO) {
            settings.setLanguage(newLang.code)
        }
    }

    suspend fun parsePlaylist(stream: java.io.InputStream): Playlist = withContext(Dispatchers.Default) {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
        stream.bufferedReader().use { M3uParser.parse(it.readText()) }
    }

    suspend fun parseEpg(stream: java.io.InputStream): EpgGuide = withContext(Dispatchers.Default) {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
        BufferedInputStream(stream).use { buffered ->
            buffered.mark(2)
            val first = buffered.read()
            val second = buffered.read()
            buffered.reset()
            val decompressed = if (first == 0x1f && second == 0x8b) GZIPInputStream(buffered) else buffered
            XmlTvParser.parse(decompressed)
        }
    }

    suspend fun parseEpgWithCache(
        source: ContentSource,
        stream: java.io.InputStream,
        allowCachedResult: Boolean
    ): EpgGuide = withContext(Dispatchers.IO) {
        val fingerprint = SourceReader.fingerprint(context, source, "epg")
        if (allowCachedResult) {
            EpgDiskCache.load(context, fingerprint)?.let { cached ->
                stream.close()
                return@withContext cached
            }
        }
        val parsed = parseEpg(stream)
        EpgDiskCache.save(context, fingerprint, parsed)
        parsed
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
                val allPlaylists = settings.getPlaylists()
                withContext(Dispatchers.Main) {
                    loadedPlaylists[updatedProfile.id] = parsed
                    playlists = allPlaylists
                    val enabled = allPlaylists.filter { it.enabled }
                    playlist = mergeLivePlaylists(allPlaylists, loadedPlaylists)
                    activeProfile = enabled.minByOrNull { it.order }
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
                parseEpgWithCache(source, stream, allowCachedResult = !forceRefresh)
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
        val savedLangCode = withContext(Dispatchers.IO) { settings.getLanguage() }
        val shift = withContext(Dispatchers.IO) { settings.epgTimeShiftHours() }
        val epgUpdate = withContext(Dispatchers.IO) { settings.lastEpgUpdate() }
        val savedFullscreenPreference = withContext(Dispatchers.IO) { settings.playChannelsFullscreen() }
        val savedPictureInPicturePreference = withContext(Dispatchers.IO) { settings.autoPictureInPicture() }
        appLanguage = AppLanguage.fromCode(savedLangCode)
        epgTimeShiftHours = shift
        epgLastUpdate = epgUpdate
        playChannelsFullscreen = savedFullscreenPreference
        autoPictureInPicture = savedPictureInPicturePreference

        val (savedPlaylists, enabledPlaylists) = withContext(Dispatchers.IO) {
            val all = settings.getPlaylists()
            all to settings.getEnabledPlaylists()
        }
        playlists = savedPlaylists
        activeProfile = enabledPlaylists.firstOrNull()

        var hasFastLoadedPlaylist = false
        val fastLoadedSourceIds = mutableSetOf<String>()
        val startupSourceErrors = mutableListOf<String>()

        withContext(Dispatchers.IO) {
            for (profile in enabledPlaylists) {
                val cacheKey = "playlist_${profile.id}"
                if (profile.source is ContentSource.LocalFile) {
                    runCatching {
                        parsePlaylist(SourceReader.open(context, profile.source))
                    }.onSuccess { parsed ->
                        if (parsed.channels.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                loadedPlaylists[profile.id] = parsed
                                playlist = mergeLivePlaylists(savedPlaylists, loadedPlaylists)
                            }
                            hasFastLoadedPlaylist = true
                            fastLoadedSourceIds += profile.id
                        }
                    }.onFailure { startupSourceErrors += "${profile.name}: ${it.message ?: "could not be loaded"}" }
                } else if (SourceReader.hasCache(context, cacheKey)) {
                    runCatching {
                        parsePlaylist(SourceReader.openCached(context, cacheKey))
                    }.onSuccess { parsed ->
                        if (parsed.channels.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                loadedPlaylists[profile.id] = parsed
                                playlist = mergeLivePlaylists(savedPlaylists, loadedPlaylists)
                            }
                            hasFastLoadedPlaylist = true
                            fastLoadedSourceIds += profile.id
                        }
                    }.onFailure { startupSourceErrors += "${profile.name}: ${it.message ?: "could not be loaded"}" }
                } else if (profile.id == settings.getActivePlaylistId() && SourceReader.hasCache(context, "playlist")) {
                    runCatching {
                        parsePlaylist(SourceReader.openCached(context, "playlist"))
                    }.onSuccess { parsed ->
                        if (parsed.channels.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                loadedPlaylists[profile.id] = parsed
                                playlist = mergeLivePlaylists(savedPlaylists, loadedPlaylists)
                            }
                            hasFastLoadedPlaylist = true
                            fastLoadedSourceIds += profile.id
                        }
                    }.onFailure { startupSourceErrors += "${profile.name}: ${it.message ?: "could not be loaded"}" }
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
                        parseEpgWithCache(
                            savedEpgSource,
                            SourceReader.open(context, savedEpgSource),
                            allowCachedResult = true
                        )
                    }.onSuccess { parsedGuide ->
                        withContext(Dispatchers.Main) { epgGuide = parsedGuide }
                    }
                } else if (SourceReader.hasCache(context, "epg")) {
                    runCatching {
                        parseEpgWithCache(
                            savedEpgSource,
                            SourceReader.openCached(context, "epg"),
                            allowCachedResult = true
                        )
                    }.onSuccess { parsedGuide ->
                        withContext(Dispatchers.Main) { epgGuide = parsedGuide }
                    }
                }
            }
        }

        if (hasFastLoadedPlaylist || enabledPlaylists.isEmpty() || enabledPlaylists.all { it.source is ContentSource.LocalFile }) {
            restoring = false
            if (!hasFastLoadedPlaylist && startupSourceErrors.isNotEmpty()) {
                error = startupSourceErrors.joinToString("\n")
            }
        }

        enabledPlaylists.filter { it.source is ContentSource.RemoteUrl }.forEach { profile ->
            val cacheKey = "playlist_${profile.id}"
            val isFresh = fastLoadedSourceIds.contains(profile.id) && (
                    SourceReader.isCacheFresh(context, cacheKey, profile.lastUpdated) ||
                    (profile.id == settings.getActivePlaylistId() && SourceReader.isCacheFresh(context, "playlist", profile.lastUpdated))
                    )
            if (!isFresh) {
                if (!hasFastLoadedPlaylist) loading = true
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        val stream = SourceReader.fetchAndCache(context, profile.source, cacheKey)
                        val now = System.currentTimeMillis()
                        val parsed = parsePlaylist(stream)
                        val updated = profile.copy(lastUpdated = now, channelCount = parsed.channels.size)
                        settings.savePlaylistProfile(updated)
                        val all = settings.getPlaylists()
                        withContext(Dispatchers.Main) {
                            loadedPlaylists[updated.id] = parsed
                            playlists = all
                            playlist = mergeLivePlaylists(all, loadedPlaylists)
                            activeProfile = all.filter { it.enabled }.minByOrNull { it.order }
                        }
                    }.onFailure { err ->
                        withContext(Dispatchers.Main) {
                            if (!hasFastLoadedPlaylist) error = "${profile.name}: ${err.message ?: "could not be loaded"}"
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
                        parseEpgWithCache(savedEpg, stream, allowCachedResult = false)
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
            source = ContentSource.LocalFile(uri.toString(), name),
            order = (playlists.maxOfOrNull { it.order } ?: -1) + 1
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
            source = ContentSource.LocalFile(uri.toString(), defaultName),
            order = (playlists.maxOfOrNull { it.order } ?: -1) + 1
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
                        source = ContentSource.RemoteUrl(url.trim(), displayName),
                        order = (playlists.maxOfOrNull { it.order } ?: -1) + 1
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
                    loadedPlaylists.remove(profile.id)
                    val remaining = settings.getPlaylists()
                    playlists = remaining
                    playlist = mergeLivePlaylists(remaining, loadedPlaylists)
                    activeProfile = remaining.filter { it.enabled }.minByOrNull { it.order }
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
                        EpgDiskCache.clear(context)
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
                playChannelsFullscreen = playChannelsFullscreen,
                onPlayChannelsFullscreenChanged = { enabled ->
                    playChannelsFullscreen = enabled
                    scope.launch(Dispatchers.IO) { settings.setPlayChannelsFullscreen(enabled) }
                },
                autoPictureInPicture = autoPictureInPicture,
                onAutoPictureInPictureChanged = { enabled ->
                    autoPictureInPicture = enabled
                    scope.launch(Dispatchers.IO) { settings.setAutoPictureInPicture(enabled) }
                },
                epgSource = epgSource,
                epgSourceName = epgSourceName,
                epgLastUpdate = epgLastUpdate,
                epgTimeShiftHours = epgTimeShiftHours,
                playlist = playlist,
                epgGuide = epgGuide,
                loading = loading,
                onDismiss = { showSettings = false },
                onPlaylistEnabledChanged = { profile, enabled ->
                    if (enabled || playlists.count { it.enabled } > 1) {
                        val updated = profile.copy(enabled = enabled)
                        settings.savePlaylistProfile(updated)
                        val all = settings.getPlaylists()
                        playlists = all
                        if (enabled) {
                            loadPlaylistProfile(updated)
                        } else {
                            loadedPlaylists.remove(updated.id)
                            playlist = mergeLivePlaylists(all, loadedPlaylists)
                            activeProfile = all.filter { it.enabled }.minByOrNull { it.order }
                        }
                    }
                },
                onSourceRoleChanged = { profile, role ->
                    val updated = profile.copy(role = role)
                    settings.savePlaylistProfile(updated)
                    val all = settings.getPlaylists()
                    playlists = all
                    playlist = mergeLivePlaylists(all, loadedPlaylists)
                    activeProfile = all.filter { it.enabled }.minByOrNull { it.order }
                },
                onMoveSource = { profile, direction ->
                    val ordered = playlists.sortedBy { it.order }.toMutableList()
                    val from = ordered.indexOfFirst { it.id == profile.id }
                    val to = (from + direction).coerceIn(0, ordered.lastIndex)
                    if (from >= 0 && from != to) {
                        val first = ordered[from]
                        val second = ordered[to]
                        settings.savePlaylistProfile(first.copy(order = second.order))
                        settings.savePlaylistProfile(second.copy(order = first.order))
                        val all = settings.getPlaylists()
                        playlists = all
                        playlist = mergeLivePlaylists(all, loadedPlaylists)
                        activeProfile = all.filter { it.enabled }.minByOrNull { it.order }
                    }
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

        startupUpdateInfo?.let { updateInfo ->
            StartupUpdateDialog(
                updateInfo = updateInfo,
                progress = startupUpdateProgress,
                downloaded = startupUpdateDownloaded,
                error = startupUpdateError,
                onDismiss = {
                    startupUpdateInfo = null
                    startupUpdateProgress = -1f
                    startupUpdateDownloaded = false
                    startupUpdateError = null
                },
                onStartUpdate = {
                    startupUpdateProgress = 0f
                    startupUpdateDownloaded = false
                    startupUpdateError = null
                    scope.launch {
                        val result = UpdateManager.downloadAndInstall(context, updateInfo.downloadUrl, updateInfo.sha256) { p ->
                            startupUpdateProgress = p
                        }
                        if (result.isFailure) {
                            startupUpdateProgress = -1f
                            startupUpdateError = result.exceptionOrNull()?.localizedMessage ?: "Download failed"
                        } else {
                            startupUpdateDownloaded = true
                        }
                    }
                },
                onInstallUpdate = {
                    val result = UpdateManager.retryPendingInstall(context)
                    if (result.isFailure) {
                        startupUpdateDownloaded = false
                        startupUpdateProgress = -1f
                        startupUpdateError = result.exceptionOrNull()?.localizedMessage ?: "Installation failed"
                    }
                }
            )
        }

        if (restoring) {
            WelcomeScreen(loading = true, showActions = false, error = null, onOpenFile = {}, onOpenUrl = {})
        } else if (playlist == null || activeProfile == null) {
            WelcomeScreen(
                loading = loading,
                showActions = !loading && (activeProfile == null || error != null),
                error = error,
                onOpenFile = { welcomeFilePicker.launch(arrayOf("audio/x-mpegurl", "application/x-mpegURL", "text/*", "*/*")) },
                onOpenUrl = { showAddPlaylistDialog = true }
            )
        } else {
            FortressLibrary(
                playlist = playlist!!,
                activeProfile = activeProfile!!,
                enabledSourceCount = playlists.count { it.enabled && it.role.contributesLiveTv() },
                enabledSources = playlists
                    .filter { it.enabled && it.role.contributesLiveTv() }
                    .sortedBy { it.order },
                epgGuide = epgGuide,
                epgSourceName = epgSourceName,
                epgTimeShiftHours = epgTimeShiftHours,
                playChannelsFullscreen = playChannelsFullscreen,
                autoPictureInPicture = autoPictureInPicture,
                loading = loading,
                error = error,
                favoriteChannelIds = favoriteChannelIds,
                onToggleFavorite = { channel ->
                    val isFavorite = channel.libraryId()?.let(favoriteChannelIds::contains) == true
                    favoriteChannelIds = userLibrary.setChannelFavorite(channel, !isFavorite)
                },
                onOpenSettings = { tab ->
                    settingsInitialTab = tab
                    showSettings = true
                }
            )
        }
    }
}

private enum class UrlTarget { PLAYLIST, EPG }

private data class GuideProgrammeBlock(
    val programme: EpgProgramme,
    val startMinutes: Float,
    val durationMinutes: Float,
    val adjustedStartMillis: Long,
    val adjustedStopMillis: Long,
    val timeLabel: String
)

private const val GUIDE_CHANNEL_BATCH_SIZE = 2

@Composable
private fun WelcomeScreen(
    loading: Boolean,
    showActions: Boolean,
    error: String?,
    onOpenFile: () -> Unit,
    onOpenUrl: () -> Unit
) {
    val strings = LocalAppStrings.current
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().padding(if (isCompact) 20.dp else 32.dp), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.widthIn(max = 680.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ShieldMark(Modifier.size(if (isCompact) 110.dp else 164.dp))
                Spacer(Modifier.height(if (isCompact) 14.dp else 22.dp))
                Text(strings.appName, fontSize = if (isCompact) 32.sp else 42.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    strings.welcomeSubtitle,
                    fontSize = if (isCompact) 13.sp else 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(if (isCompact) 24.dp else 32.dp))
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(if (isCompact) 38.dp else 46.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(strings.loadingSavedData, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(20.dp))
                }
                if (showActions && isCompact && isPortrait) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(onClick = onOpenFile, enabled = !loading, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (loading) strings.loadingPlaylist else strings.browseLocalFile)
                        }
                        OutlinedButton(onClick = onOpenUrl, enabled = !loading, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                            Icon(Icons.Default.Link, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.remoteUrl)
                        }
                    }
                } else if (showActions) {
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
private fun ProgrammeGuideScreen(
    playlist: Playlist,
    epgGuide: EpgGuide?,
    epgTimeShiftHours: Int,
    onBack: () -> Unit,
    onWatchChannel: (PlaylistChannel) -> Unit
) {
    BackHandler(onBack = onBack)
    val strings = LocalAppStrings.current
    val guideLocale = remember(strings.languageTag) { Locale.forLanguageTag(strings.languageTag) }
    val density = LocalDensity.current
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    val dates = remember(today) { List(3) { today.plusDays(it.toLong()) } }
    var selectedDate by remember { mutableStateOf(today) }
    var selectedProgramme by remember { mutableStateOf<Pair<PlaylistChannel, EpgProgramme>?>(null) }
    var now by remember { mutableStateOf(Instant.now()) }
    val horizontalState = rememberScrollState()
    var useWideTimelineWindow by remember { mutableStateOf(false) }
    var initialTimelinePositioned by remember { mutableStateOf(false) }
    val channelState = rememberLazyListState()
    val programmeState = rememberLazyListState()
    var timelineViewportPx by remember { mutableIntStateOf(0) }
    val timelineZoomLevels = remember { listOf(180.dp, 270.dp, 360.dp, 540.dp, 720.dp, 1080.dp, 1440.dp) }
    var timelineZoomIndex by remember { mutableIntStateOf(1) }
    val hourWidth = timelineZoomLevels[timelineZoomIndex]
    val halfHourWidth = hourWidth / 2
    val timelineWidth = hourWidth * 24
    val rowHeight = 72.dp
    val channelWidth = 220.dp
    val shiftMillis = epgTimeShiftHours * 3_600_000L

    LaunchedEffect(Unit) {
        while (true) {
            now = Instant.now()
            delay(60_000)
        }
    }
    LaunchedEffect(programmeState) {
        snapshotFlow { programmeState.firstVisibleItemIndex to programmeState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (channelState.firstVisibleItemIndex != index || channelState.firstVisibleItemScrollOffset != offset) {
                    channelState.scrollToItem(index, offset)
                }
            }
    }
    LaunchedEffect(channelState) {
        snapshotFlow { channelState.firstVisibleItemIndex to channelState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (programmeState.firstVisibleItemIndex != index || programmeState.firstVisibleItemScrollOffset != offset) {
                    programmeState.scrollToItem(index, offset)
                }
            }
    }
    LaunchedEffect(selectedDate, timelineViewportPx, hourWidth) {
        val targetMinutes = if (selectedDate == today) {
            val localNow = now.atZone(zone)
            localNow.hour * 60 + localNow.minute
        } else 8 * 60
        val targetPx = with(density) { (hourWidth * (targetMinutes / 60f)).roundToPx() }
        horizontalState.scrollTo((targetPx - timelineViewportPx / 2).coerceIn(0, horizontalState.maxValue))
        initialTimelinePositioned = true
    }
    LaunchedEffect(horizontalState) {
        snapshotFlow { horizontalState.isScrollInProgress }.collect { isScrolling ->
            if (isScrolling && initialTimelinePositioned) useWideTimelineWindow = true
        }
    }

    val selectedCurrent = selectedProgramme?.second?.let { programme ->
        val adjustedNow = now.toEpochMilli()
        val start = programme.startEpochMilli + shiftMillis
        val stop = (programme.stopEpochMilli ?: programme.startEpochMilli) + shiftMillis
        adjustedNow in start until stop
    } == true
    val todayGuideBlocks = remember(epgGuide, playlist, dates, zone, shiftMillis) {
        mutableStateMapOf<PlaylistChannel, List<GuideProgrammeBlock>>()
    }
    var todayGuideReady by remember(epgGuide, playlist, dates, zone, shiftMillis) {
        mutableStateOf(false)
    }
    val guideDayIndex = remember(epgGuide, playlist, dates, zone, shiftMillis) {
        mutableStateMapOf<LocalDate, Map<PlaylistChannel, List<GuideProgrammeBlock>>>(
            today to todayGuideBlocks
        )
    }
    LaunchedEffect(epgGuide, playlist, dates, zone, shiftMillis) {
        // Prepare today first. Publish its initially visible channels in small batches
        // so opening the guide does not compose every programme row in one long frame.
        dates.forEachIndexed { dayOffset, date ->
            val dayIndex = withContext(Dispatchers.Default) {
                val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
                val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                playlist.channels.mapNotNull { channel ->
                    val blocks = epgGuide?.programmes?.get(channel.tvgId).orEmpty().mapNotNull { programme ->
                        val adjustedStart = programme.startEpochMilli + shiftMillis
                        val rawStop = programme.stopEpochMilli ?: (programme.startEpochMilli + 30 * 60_000L)
                        val adjustedStop = rawStop + shiftMillis
                        if (adjustedStart <= 0L || adjustedStop <= adjustedStart || adjustedStart >= dayEnd || adjustedStop <= dayStart) {
                            null
                        } else {
                            val clippedStart = maxOf(adjustedStart, dayStart)
                            val clippedStop = minOf(adjustedStop, dayEnd)
                            GuideProgrammeBlock(
                                programme = programme,
                                startMinutes = (clippedStart - dayStart) / 60_000f,
                                durationMinutes = ((clippedStop - clippedStart) / 60_000f)
                                    .coerceIn(5f, 24f * 60f),
                                adjustedStartMillis = adjustedStart,
                                adjustedStopMillis = adjustedStop,
                                timeLabel = "${formatProgrammeTime(Instant.ofEpochMilli(adjustedStart))}–${formatProgrammeTime(Instant.ofEpochMilli(adjustedStop))}"
                            )
                        }
                    }
                    if (blocks.isEmpty()) null else channel to blocks
                }.toMap()
            }
            if (dayOffset == 0) {
                dayIndex.entries.chunked(GUIDE_CHANNEL_BATCH_SIZE).forEach { batch ->
                    batch.forEach { (channel, blocks) -> todayGuideBlocks[channel] = blocks }
                    withFrameNanos { }
                }
                todayGuideReady = true
            } else {
                // Future days are not visible yet, so publishing their immutable index
                // is an O(1) state change and does not invalidate today's programme rows.
                guideDayIndex[date] = dayIndex
            }
        }
    }
    val guideBlocksByChannel = guideDayIndex[selectedDate].orEmpty()
    val visibleTimelineMinutes by remember(horizontalState, timelineViewportPx, hourWidth, density, useWideTimelineWindow) {
        derivedStateOf {
            val hourWidthPx = with(density) { hourWidth.toPx() }.coerceAtLeast(1f)
            val firstMinute = horizontalState.value / hourWidthPx * 60f
            val visibleMinutes = timelineViewportPx / hourWidthPx * 60f
            if (useWideTimelineWindow) {
                // Once horizontal navigation begins, rebuild at most every two hours.
                val bucketedFirstMinute = (firstMinute / 120f).toInt() * 120f
                (bucketedFirstMinute - 120f).coerceAtLeast(0f) to
                    (bucketedFirstMinute + visibleMinutes + 180f).coerceAtMost(24f * 60f)
            } else {
                // Opening and vertical-only navigation retain the smaller render set.
                val bucketedFirstMinute = (firstMinute / 15f).toInt() * 15f
                (bucketedFirstMinute - 30f).coerceAtLeast(0f) to
                    (bucketedFirstMinute + visibleMinutes + 45f).coerceAtMost(24f * 60f)
            }
        }
    }

    Surface(
        Modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
        color = MaterialTheme.colorScheme.background
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            if (todayGuideReady) {
                Box(Modifier.size(1.dp).testTag("epg_ready"))
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                }
                Spacer(Modifier.width(8.dp))
                ShieldMark(Modifier.size(38.dp))
                Spacer(Modifier.width(10.dp))
                Text(strings.programmeGuide, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = { timelineZoomIndex = (timelineZoomIndex - 1).coerceAtLeast(0) },
                    enabled = timelineZoomIndex > 0
                ) {
                    Icon(Icons.Default.ZoomOut, contentDescription = strings.zoomOutTimeline)
                }
                IconButton(
                    onClick = { timelineZoomIndex = (timelineZoomIndex + 1).coerceAtMost(timelineZoomLevels.lastIndex) },
                    enabled = timelineZoomIndex < timelineZoomLevels.lastIndex
                ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = strings.zoomInTimeline)
                }
                FilledTonalButton(onClick = {
                    selectedDate = today
                    selectedProgramme = null
                }) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(strings.now)
                }
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dates, key = { it.toString() }) { date ->
                    FilterChip(
                        selected = selectedDate == date,
                        onClick = {
                            selectedDate = date
                            selectedProgramme = null
                        },
                        label = {
                            Text(
                                if (date == today) strings.todayDate(date.dayOfMonth)
                                else "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, guideLocale)} ${date.dayOfMonth}"
                            )
                        }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.width(channelWidth).fillMaxHeight().background(MaterialTheme.colorScheme.surface)) {
                    Box(
                        Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(strings.channelList, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider()
                    LazyColumn(
                        state = channelState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(playlist.channels, key = { "epg-channel-${it.position}:${it.streamUrl}" }) { channel ->
                            Surface(
                                onClick = { onWatchChannel(channel) },
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth().height(rowHeight)
                            ) {
                                Row(
                                    Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ChannelLogo(
                                        channel = channel,
                                        epgChannel = epgGuide?.channels?.get(channel.tvgId),
                                        modifier = Modifier.size(42.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(channel.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
                Box(
                    Modifier.weight(1f).fillMaxHeight().onSizeChanged { timelineViewportPx = it.width }
                ) {
                    Column(
                        Modifier.fillMaxHeight().horizontalScroll(horizontalState)
                    ) {
                        Box(Modifier.width(timelineWidth).height(44.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
                            val (renderStartMinute, renderEndMinute) = visibleTimelineMinutes
                            val firstHalfHour = (renderStartMinute / 30f).toInt().coerceIn(0, 47)
                            val lastHalfHour = ((renderEndMinute + 29.999f) / 30f).toInt().coerceIn(firstHalfHour, 47)
                            for (halfHour in firstHalfHour..lastHalfHour) {
                                val minutes = halfHour * 30
                                Text(
                                    "%02d:%02d".format(minutes / 60, minutes % 60),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    modifier = Modifier.offset(x = halfHourWidth * halfHour).padding(start = 8.dp, top = 12.dp)
                                )
                            }
                            if (selectedDate == today) {
                                val localNow = now.atZone(zone)
                                val nowMinutes = localNow.hour * 60 + localNow.minute + localNow.second / 60f
                                Box(
                                    Modifier.offset(x = hourWidth * (nowMinutes / 60f)).width(2.dp).fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                        HorizontalDivider()
                        LazyColumn(
                            state = programmeState,
                            modifier = Modifier.width(timelineWidth).weight(1f).testTag("epg_grid")
                        ) {
                            items(playlist.channels, key = { "epg-row-${it.position}:${it.streamUrl}" }) { channel ->
                                val allProgrammeBlocks = guideBlocksByChannel[channel].orEmpty()
                                val programmeBlocks = remember(allProgrammeBlocks, visibleTimelineMinutes) {
                                    val (visibleStart, visibleEnd) = visibleTimelineMinutes
                                    allProgrammeBlocks.filter { block ->
                                        block.startMinutes + block.durationMinutes >= visibleStart &&
                                            block.startMinutes <= visibleEnd
                                    }
                                }
                                Box(Modifier.fillMaxWidth().height(rowHeight).background(MaterialTheme.colorScheme.background)) {
                                    if (allProgrammeBlocks.isEmpty()) {
                                        Text(
                                            strings.noEpgData,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(14.dp)
                                        )
                                    }
                                    programmeBlocks.forEach { block ->
                                        val blockWidth = hourWidth * (block.durationMinutes / 60f) - 2.dp
                                        val isCurrent = now.toEpochMilli() in block.adjustedStartMillis until block.adjustedStopMillis
                                        val isSelected = selectedProgramme?.first == channel && selectedProgramme?.second == block.programme
                                        key(block.adjustedStartMillis, block.adjustedStopMillis, block.programme.title) {
                                            Surface(
                                            onClick = {
                                                selectedProgramme = channel to block.programme
                                            },
                                            shape = RoundedCornerShape(5.dp),
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.primaryContainer
                                                isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = .22f)
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            modifier = Modifier
                                                .offset(x = hourWidth * (block.startMinutes / 60f), y = 5.dp)
                                                .width(blockWidth)
                                                .height(rowHeight - 10.dp)
                                            ) {
                                                if (blockWidth >= 42.dp) {
                                                    Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                                                        Text(block.programme.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        if (blockWidth >= 72.dp) {
                                                            Text(
                                                                block.timeLabel,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                fontSize = 11.sp,
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                        Text(
                                                            "·",
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (selectedDate == today) {
                                        val localNow = now.atZone(zone)
                                        val nowMinutes = localNow.hour * 60 + localNow.minute + localNow.second / 60f
                                        Box(
                                            Modifier.offset(x = hourWidth * (nowMinutes / 60f)).width(2.dp).fillMaxHeight()
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
            selectedProgramme?.let { (channel, programme) ->
                HorizontalDivider()
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(programme.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "${channel.name} · ${formatProgrammeTime(Instant.ofEpochMilli(programme.startEpochMilli + shiftMillis))}–${formatProgrammeTime(Instant.ofEpochMilli((programme.stopEpochMilli ?: programme.startEpochMilli) + shiftMillis))}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            programme.description?.let {
                                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Button(onClick = { onWatchChannel(channel) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(7.dp))
                            Text(if (selectedCurrent) strings.watchNow else strings.watchLiveChannel)
                        }
                    }
                }
            }
        }
    }
}

private enum class LibraryDestination { LIVE_TV, MOVIES, SERIES, FAVORITES }

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun FortressLibrary(
    playlist: Playlist,
    activeProfile: PlaylistProfile,
    enabledSourceCount: Int,
    enabledSources: List<PlaylistProfile>,
    epgGuide: EpgGuide?,
    epgSourceName: String?,
    epgTimeShiftHours: Int,
    playChannelsFullscreen: Boolean,
    autoPictureInPicture: Boolean,
    loading: Boolean,
    error: String?,
    favoriteChannelIds: Set<String>,
    onToggleFavorite: (PlaylistChannel) -> Unit,
    onOpenSettings: (Int) -> Unit
) {
    val strings = LocalAppStrings.current
    var destination by remember { mutableStateOf(LibraryDestination.LIVE_TV) }
    var requestedChannel by remember { mutableStateOf<PlaylistChannel?>(null) }
    val destinations = listOf(
        LibraryDestination.LIVE_TV to strings.navLiveTv,
        LibraryDestination.MOVIES to strings.navMovies,
        LibraryDestination.SERIES to strings.navSeries,
        LibraryDestination.FAVORITES to strings.navFavorites
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShieldMark(Modifier.size(38.dp))
                Spacer(Modifier.width(4.dp))
                destinations.forEach { (item, label) ->
                    LibraryNavigationItem(
                        label = label,
                        selected = destination == item,
                        onClick = { destination = item },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
            when (destination) {
                LibraryDestination.LIVE_TV -> ChannelBrowser(
                    playlist = playlist,
                    activeProfile = activeProfile,
                    enabledSourceCount = enabledSourceCount,
                    enabledSources = enabledSources,
                    epgGuide = epgGuide,
                    epgSourceName = epgSourceName,
                    epgTimeShiftHours = epgTimeShiftHours,
                    playChannelsFullscreen = playChannelsFullscreen,
                    autoPictureInPicture = autoPictureInPicture,
                    loading = loading,
                    error = error,
                    favoriteChannelIds = favoriteChannelIds,
                    onToggleFavorite = onToggleFavorite,
                    requestedChannel = requestedChannel,
                    onRequestedChannelHandled = { requestedChannel = null },
                    onOpenSettings = onOpenSettings
                )
                LibraryDestination.MOVIES -> LibraryPlaceholder(
                    title = strings.navMovies,
                    message = strings.moviesComingSoon
                )
                LibraryDestination.SERIES -> LibraryPlaceholder(
                    title = strings.navSeries,
                    message = strings.seriesComingSoon
                )
                LibraryDestination.FAVORITES -> FavoriteChannelsScreen(
                    channels = playlist.channels.filter { it.libraryId()?.let(favoriteChannelIds::contains) == true },
                    epgGuide = epgGuide,
                    epgTimeShiftHours = epgTimeShiftHours,
                    favoriteChannelIds = favoriteChannelIds,
                    onToggleFavorite = onToggleFavorite,
                    onWatch = { channel ->
                        requestedChannel = channel
                        destination = LibraryDestination.LIVE_TV
                    }
                )
            }
        }
    }
}

@Composable
private fun LibraryNavigationItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)) else null,
        contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxHeight()
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun LibraryPlaceholder(title: String, message: String) {
    val strings = LocalAppStrings.current
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
            tonalElevation = 4.dp,
            modifier = Modifier.widthIn(max = 560.dp)
        ) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ShieldMark(Modifier.size(72.dp))
                Spacer(Modifier.height(18.dp))
                Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(strings.comingSoon, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
private fun FavoriteChannelsScreen(
    channels: List<PlaylistChannel>,
    epgGuide: EpgGuide?,
    epgTimeShiftHours: Int,
    favoriteChannelIds: Set<String>,
    onToggleFavorite: (PlaylistChannel) -> Unit,
    onWatch: (PlaylistChannel) -> Unit
) {
    val strings = LocalAppStrings.current
    var scheduleNow by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(epgGuide) {
        while (true) {
            scheduleNow = Instant.now()
            delay(60_000)
        }
    }
    if (channels.isEmpty()) {
        Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
            ) {
                Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.StarOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(14.dp))
                    Text(strings.navFavorites, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(strings.noFavoriteChannels, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(strings.navFavorites, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(10.dp))
                    Text("${channels.size}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(4.dp))
            }
            items(channels, key = { "favorite:${it.sourceId}:${it.sourceItemId}" }) { channel ->
                GuideChannelRow(
                    channel = channel,
                    epgGuide = epgGuide,
                    scheduleNow = scheduleNow,
                    epgTimeShiftHours = epgTimeShiftHours,
                    selected = false,
                    isFavorite = channel.libraryId()?.let(favoriteChannelIds::contains) == true,
                    onToggleFavorite = { onToggleFavorite(channel) },
                    onClick = { onWatch(channel) }
                )
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun ChannelBrowser(
    playlist: Playlist,
    activeProfile: PlaylistProfile,
    enabledSourceCount: Int,
    enabledSources: List<PlaylistProfile>,
    epgGuide: EpgGuide?,
    epgSourceName: String?,
    epgTimeShiftHours: Int = 0,
    playChannelsFullscreen: Boolean,
    autoPictureInPicture: Boolean,
    loading: Boolean,
    error: String?,
    favoriteChannelIds: Set<String>,
    onToggleFavorite: (PlaylistChannel) -> Unit,
    requestedChannel: PlaylistChannel?,
    onRequestedChannelHandled: () -> Unit,
    onOpenSettings: (Int) -> Unit
) {
    val strings = LocalAppStrings.current
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val channelPaneWidth = if (isCompact) 280.dp else 430.dp

    val allFilter = "all"
    val uncategorizedFilter = "group:"
    var isSidebarExpanded by remember { mutableStateOf(false) }
    var sidebarInteractionCount by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isSidebarExpanded, sidebarInteractionCount) {
        if (isSidebarExpanded) {
            delay(5_000)
            isSidebarExpanded = false
        }
    }

    var selectedFilter by remember(playlist) { mutableStateOf(allFilter) }
    var searchQuery by remember(playlist) { mutableStateOf("") }
    var selectedChannel by remember(playlist) { mutableStateOf<PlaylistChannel?>(null) }
    var showProgrammeGuide by remember(playlist) { mutableStateOf(false) }
    fun selectChannel(channel: PlaylistChannel) {
        selectedChannel = channel
        isSidebarExpanded = false
    }
    LaunchedEffect(requestedChannel) {
        requestedChannel?.let {
            selectChannel(it)
            onRequestedChannelHandled()
        }
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
    val sourceCounts = remember(playlist) {
        playlist.channels.groupingBy { it.sourceId }.eachCount()
    }
    val selectedFilterLabel = when {
        selectedFilter == allFilter -> strings.allChannels
        selectedFilter.startsWith("source:") -> enabledSources
            .firstOrNull { it.id == selectedFilter.removePrefix("source:") }
            ?.name ?: strings.sources
        selectedFilter == uncategorizedFilter -> strings.uncategorized
        selectedFilter.startsWith("group:") -> selectedFilter.removePrefix("group:")
        else -> strings.allChannels
    }
    val channelsInGroup = remember(playlist, selectedFilter) {
        when {
            selectedFilter == allFilter -> playlist.channels
            selectedFilter.startsWith("source:") -> {
                val sourceId = selectedFilter.removePrefix("source:")
                playlist.channels.filter { it.sourceId == sourceId }
            }
            selectedFilter == uncategorizedFilter -> playlist.channels.filter { it.group.isNullOrBlank() }
            selectedFilter.startsWith("group:") -> {
                val group = selectedFilter.removePrefix("group:")
                playlist.channels.filter { it.group == group }
            }
            else -> playlist.channels
        }
    }
    val visibleChannels = remember(channelsInGroup, searchQuery) {
        channelsInGroup.searchChannels(searchQuery)
    }
    val channelListState = rememberLazyListState()

    if (showProgrammeGuide) {
        ProgrammeGuideScreen(
            playlist = playlist,
            epgGuide = epgGuide,
            epgTimeShiftHours = epgTimeShiftHours,
            onBack = { showProgrammeGuide = false },
            onWatchChannel = { channel ->
                selectChannel(channel)
                showProgrammeGuide = false
            }
        )
        return
    }

    val matchedCount = remember(playlist, epgGuide) {
        if (epgGuide == null) 0 else playlist.channels.count { it.tvgId != null && epgGuide.channels.containsKey(it.tvgId) }
    }
    val unmatchedChannels = remember(playlist, epgGuide) {
        if (epgGuide == null) emptyList()
        else playlist.channels.filter { it.tvgId.isNullOrBlank() || !epgGuide.channels.containsKey(it.tvgId) }
    }
    var showUnmatchedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedFilter, searchQuery) {
        channelListState.scrollToItem(0)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Row(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isSidebarExpanded,
            enter = expandHorizontally() + fadeIn(),
            exit = shrinkHorizontally() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .width(if (isCompact) 280.dp else 260.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            while (true) {
                                awaitPointerEvent()
                                sidebarInteractionCount = System.currentTimeMillis()
                            }
                        }
                    }
                    .padding(16.dp)
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
                                if (enabledSourceCount > 1) strings.sourcesCount(enabledSourceCount) else activeProfile.name,
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
                        GroupItem(strings.allChannels, playlist.channels.size, selectedFilter == allFilter) {
                            selectedFilter = allFilter
                            isSidebarExpanded = false
                        }
                    }
                    if (enabledSources.isNotEmpty()) {
                        item(key = "sources_header") { SidebarSectionLabel(strings.sources) }
                        items(enabledSources, key = { "source:${it.id}" }) { source ->
                            val key = "source:${source.id}"
                            GroupItem(source.name, sourceCounts[source.id] ?: 0, selectedFilter == key) {
                                selectedFilter = key
                                isSidebarExpanded = false
                            }
                        }
                    }
                    if (playlist.groups.isNotEmpty() || (groupCounts[""] ?: 0) > 0) {
                        item(key = "categories_header") { SidebarSectionLabel(strings.categories) }
                    }
                    items(playlist.groups, key = { it }) { group ->
                        val key = "group:$group"
                        GroupItem(group, groupCounts[group] ?: 0, selectedFilter == key) {
                            selectedFilter = key
                            isSidebarExpanded = false
                        }
                    }
                    if ((groupCounts[""] ?: 0) > 0) {
                        item(key = "group_uncategorized") {
                            GroupItem(strings.uncategorized, groupCounts[""] ?: 0, selectedFilter == uncategorizedFilter) {
                                selectedFilter = uncategorizedFilter
                                isSidebarExpanded = false
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                if (epgGuide == null) {
                    Text(strings.epgNotConfigured, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                } else {
                    Surface(
                        onClick = { if (unmatchedChannels.isNotEmpty()) showUnmatchedDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        color = if (unmatchedChannels.isNotEmpty()) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f) else Color.Transparent,
                        border = if (unmatchedChannels.isNotEmpty()) BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(vertical = 6.dp, horizontal = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    strings.epgMatched(matchedCount, playlist.channels.size),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = if (unmatchedChannels.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                                if (unmatchedChannels.isNotEmpty()) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = strings.viewUnmatchedChannels,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                epgSourceName ?: strings.xmltvGuide,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 11.sp
                            )
                            if (unmatchedChannels.isNotEmpty()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    strings.unmatchedEpgCount(unmatchedChannels.size),
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { onOpenSettings(0) }, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.settings)
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight().padding(horizontal = if (isCompact) 12.dp else 24.dp, vertical = if (isCompact) 10.dp else 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = if (selectedChannel != null && !(isCompact && isPortrait)) {
                        Modifier.width(channelPaneWidth)
                    } else {
                        Modifier.weight(1f)
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isSidebarExpanded) {
                        FilledTonalIconButton(onClick = { isSidebarExpanded = true }) {
                            Icon(Icons.Default.Menu, contentDescription = strings.expandSidebar)
                        }
                    }
                    FilledTonalIconButton(
                        onClick = { showProgrammeGuide = true },
                        enabled = epgGuide != null,
                        modifier = Modifier.testTag("programme_guide_button")
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = strings.programmeGuide)
                    }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(strings.searchInGroup(selectedFilterLabel), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Text("×", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        modifier = when {
                            selectedChannel != null && !(isCompact && isPortrait) -> Modifier.weight(1f)
                            isCompact -> Modifier.weight(1f)
                            else -> Modifier.width(360.dp)
                        }
                    )
                }
                if (!isCompact) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        strings.channelsCount(visibleChannels.size),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            if (isCompact && isPortrait) {
                if (selectedChannel != null) {
                    Column(Modifier.weight(1f).fillMaxWidth()) {
                        PlayerPanel(
                            channel = selectedChannel!!,
                            epgGuide = epgGuide,
                            epgTimeShiftHours = epgTimeShiftHours,
                            channels = playlist.channels,
                            groups = playlist.groups,
                            autoEnterFullscreen = playChannelsFullscreen,
                            autoPictureInPicture = autoPictureInPicture,
                            onChannelSelected = ::selectChannel,
                            modifier = Modifier.fillMaxWidth().wrapContentHeight()
                        )
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(6.dp))
                        Text(
                            strings.channelsCount(visibleChannels.size),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        LazyColumn(
                            state = channelListState,
                            modifier = Modifier.weight(1f).fillMaxWidth().testTag("channel_list"),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(
                                items = visibleChannels,
                                key = { "${it.position}:${it.streamUrl}" },
                                contentType = { "channel" }
                            ) { channel ->
                                GuideChannelRow(
                                    channel = channel,
                                    epgGuide = epgGuide,
                                    scheduleNow = scheduleNow,
                                    epgTimeShiftHours = epgTimeShiftHours,
                                    selected = selectedChannel == channel,
                                    isFavorite = channel.libraryId()?.let(favoriteChannelIds::contains) == true,
                                    onToggleFavorite = { onToggleFavorite(channel) },
                                    onClick = { selectChannel(channel) }
                                )
                            }
                            if (visibleChannels.isEmpty()) {
                                item {
                                    Box(Modifier.fillParentMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) {
                                        Text(strings.noChannelsMatch, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = channelListState,
                        modifier = Modifier.weight(1f).fillMaxWidth().testTag("channel_list"),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(
                            items = visibleChannels,
                            key = { "${it.position}:${it.streamUrl}" },
                            contentType = { "channel" }
                        ) { channel ->
                            GuideChannelRow(
                                channel = channel,
                                epgGuide = epgGuide,
                                scheduleNow = scheduleNow,
                                epgTimeShiftHours = epgTimeShiftHours,
                                selected = selectedChannel == channel,
                                isFavorite = channel.libraryId()?.let(favoriteChannelIds::contains) == true,
                                onToggleFavorite = { onToggleFavorite(channel) },
                                onClick = { selectChannel(channel) }
                            )
                        }
                        if (visibleChannels.isEmpty()) {
                            item {
                                Box(Modifier.fillParentMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) {
                                    Text(strings.noChannelsMatch, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            } else {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        state = channelListState,
                        modifier = (if (selectedChannel == null) Modifier.fillMaxSize() else Modifier.width(channelPaneWidth).fillMaxHeight())
                            .testTag("channel_list"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = visibleChannels,
                            key = { "${it.position}:${it.streamUrl}" },
                            contentType = { "channel" }
                        ) { channel ->
                            GuideChannelRow(
                                channel = channel,
                                epgGuide = epgGuide,
                                scheduleNow = scheduleNow,
                                epgTimeShiftHours = epgTimeShiftHours,
                                selected = selectedChannel == channel,
                                isFavorite = channel.libraryId()?.let(favoriteChannelIds::contains) == true,
                                onToggleFavorite = { onToggleFavorite(channel) },
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
                            autoEnterFullscreen = playChannelsFullscreen,
                            autoPictureInPicture = autoPictureInPicture,
                            onChannelSelected = ::selectChannel,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

    if (showUnmatchedDialog) {
        UnmatchedEpgDialog(
            unmatchedChannels = unmatchedChannels,
            onDismiss = { showUnmatchedDialog = false },
            onSelectChannel = { channel ->
                selectChannel(channel)
                searchQuery = ""
                selectedFilter = allFilter
            }
        )
    }
}

@Composable
private fun SidebarSectionLabel(label: String) {
    Text(
        text = label.uppercase(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 2.dp)
    )
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
private fun GuideChannelRow(
    channel: PlaylistChannel,
    epgGuide: EpgGuide?,
    scheduleNow: Instant,
    epgTimeShiftHours: Int,
    selected: Boolean,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val presentation = remember(channel.tvgId, epgGuide, scheduleNow, epgTimeShiftHours, strings) {
        val current = epgGuide?.snapshot(channel.tvgId, scheduleNow, epgTimeShiftHours)?.current
        val shiftMillis = epgTimeShiftHours * 3_600_000L
        val adjustedStart = current?.startEpochMilli?.plus(shiftMillis)
        val adjustedStop = current?.stopEpochMilli?.plus(shiftMillis)
        val progress = if (adjustedStart != null && adjustedStop != null && adjustedStop > adjustedStart) {
            ((scheduleNow.toEpochMilli() - adjustedStart).toFloat() /
                (adjustedStop - adjustedStart).toFloat()).coerceIn(0f, 1f)
        } else {
            null
        }
        ChannelEpgPresentation(
            title = current?.title,
            endTime = adjustedStop?.let { strings.until(formatProgrammeTime(Instant.ofEpochMilli(it))) },
            progress = progress
        )
    }
    ChannelRow(
        channel = channel,
        epgChannel = epgGuide?.channels?.get(channel.tvgId),
        nowPlaying = presentation.title,
        endTime = presentation.endTime,
        progress = presentation.progress,
        selected = selected,
        isFavorite = isFavorite,
        onToggleFavorite = onToggleFavorite,
        onClick = onClick
    )
}

private data class ChannelEpgPresentation(
    val title: String?,
    val endTime: String?,
    val progress: Float?
)

@Composable
private fun ChannelRow(
    channel: PlaylistChannel,
    epgChannel: com.fortresstv.player.epg.EpgChannel?,
    nowPlaying: String?,
    endTime: String? = null,
    progress: Float? = null,
    selected: Boolean,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
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
            onToggleFavorite?.let { toggle ->
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = toggle) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                        contentDescription = if (isFavorite) strings.removeFavorite else strings.addFavorite,
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelLogo(
    channel: PlaylistChannel,
    epgChannel: com.fortresstv.player.epg.EpgChannel?,
    modifier: Modifier = Modifier.size(48.dp)
) {
    val logoUrl = remember(channel.logoUrl, epgChannel?.iconUrl) { channel.logoUrl ?: epgChannel?.iconUrl }
    var failed by remember(logoUrl) { mutableStateOf(false) }
    Box(
        modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (logoUrl != null && !failed) {
            AsyncImage(
                model = logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.Low,
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
    autoEnterFullscreen: Boolean,
    autoPictureInPicture: Boolean,
    onChannelSelected: (PlaylistChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val activity = context.findActivity()
    val lifecycleOwner = activity as? LifecycleOwner
    val renderersFactory = remember(context) {
        DefaultRenderersFactory(context).setExtensionRendererMode(
            DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
        )
    }
    val player = remember {
        ExoPlayer.Builder(context, renderersFactory).build().apply { playWhenReady = true }
    }
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var fullscreen by remember { mutableStateOf(false) }
    val inPictureInPicture = (activity as? MainActivity)?.pictureInPictureMode == true

    SideEffect {
        (activity as? MainActivity)?.updatePictureInPictureState(
            enabled = autoPictureInPicture,
            fullscreenPlaying = fullscreen && player.playWhenReady
        )
    }
    val schedule = epgGuide?.snapshot(channel.tvgId, timeShiftHours = epgTimeShiftHours)

    DisposableEffect(player) {
        var resumePlaybackOnStart = false
        val pictureInPictureDismissedListener = {
            resumePlaybackOnStart = false
            player.pause()
        }
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = error.message ?: strings.playbackError
            }
        }
        val lifecycleObserver = object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                if ((activity as? MainActivity)?.isInPictureInPictureMode == true) return
                resumePlaybackOnStart = player.playWhenReady
                player.pause()
            }

            override fun onStart(owner: LifecycleOwner) {
                if (resumePlaybackOnStart) {
                    resumePlaybackOnStart = false
                    player.play()
                }
            }
        }
        player.addListener(listener)
        (activity as? MainActivity)?.setOnPictureInPictureDismissedListener(
            pictureInPictureDismissedListener
        )
        lifecycleOwner?.lifecycle?.addObserver(lifecycleObserver)
        onDispose {
            (activity as? MainActivity)?.updatePictureInPictureState(
                enabled = autoPictureInPicture,
                fullscreenPlaying = false
            )
            lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
            (activity as? MainActivity)?.setOnPictureInPictureDismissedListener(null)
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(channel.streamUrl) {
        playbackError = null
        player.setMediaItem(MediaItem.fromUri(channel.streamUrl))
        player.prepare()
        player.play()
        if (autoEnterFullscreen) fullscreen = true
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
            },
            inPictureInPicture = inPictureInPicture
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
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
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
            IconButton(
                onClick = onFullscreen,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = .62f), RoundedCornerShape(10.dp))
            ) {
                Icon(
                    Icons.Default.Fullscreen,
                    contentDescription = strings.fullscreen,
                    tint = Color.White
                )
            }
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
    onRetry: () -> Unit,
    inPictureInPicture: Boolean
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
                visible = controlsVisible && !showChannels && !showGuide && !inPictureInPicture,
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
                visible = showChannels && !inPictureInPicture,
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
                visible = showGuide && !inPictureInPicture,
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
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600
    val now = Instant.now()
    val shiftMillis = epgTimeShiftHours * 3_600_000L
    val programmes = epgGuide?.programmes?.get(channel.tvgId).orEmpty()
        .filter { programme -> (programme.stopEpochMilli ?: programme.startEpochMilli) + shiftMillis > now.toEpochMilli() }
        .sortedBy { it.startEpochMilli }
    Surface(
        modifier = modifier.fillMaxHeight().widthIn(max = 560.dp).fillMaxWidth(if (isCompact) 0.88f else 0.5f),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .98f),
        shadowElevation = 12.dp
    ) {
        Column(Modifier.padding(if (isCompact) 14.dp else 20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(strings.programmeGuide, fontSize = if (isCompact) 20.sp else 25.sp, fontWeight = FontWeight.Bold)
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
                    items(programmes, key = { "${it.channelId}:${it.startEpochMilli}:${it.title}" }) { programme ->
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                Text(
                                    formatProgrammeRange(programme.startEpochMilli, programme.stopEpochMilli, epgTimeShiftHours),
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

private fun formatProgrammeRange(startEpochMilli: Long, stopEpochMilli: Long?, timeShiftHours: Int = 0): String {
    val formatter = DateTimeFormatter.ofPattern("EEE d MMM · HH:mm")
    val shiftMillis = timeShiftHours * 3_600_000L
    val startText = Instant.ofEpochMilli(startEpochMilli + shiftMillis).atZone(ZoneId.systemDefault()).format(formatter)
    val stopText = stopEpochMilli?.let {
        Instant.ofEpochMilli(it + shiftMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
    }
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
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600
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
        modifier = modifier.fillMaxHeight().widthIn(max = 460.dp).fillMaxWidth(if (isCompact) 0.88f else 0.42f),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .98f),
        shadowElevation = 16.dp
    ) {
        Column(Modifier.padding(if (isCompact) 14.dp else 18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(strings.channelList, fontSize = if (isCompact) 20.sp else 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
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
                items(
                    items = visibleChannels,
                    key = { "fullscreen:${it.position}:${it.streamUrl}" },
                    contentType = { "channel" }
                ) { item ->
                    GuideChannelRow(
                        channel = item,
                        epgGuide = epgGuide,
                        scheduleNow = scheduleNow,
                        epgTimeShiftHours = epgTimeShiftHours,
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
    playChannelsFullscreen: Boolean,
    onPlayChannelsFullscreenChanged: (Boolean) -> Unit,
    autoPictureInPicture: Boolean,
    onAutoPictureInPictureChanged: (Boolean) -> Unit,
    epgSource: ContentSource?,
    epgSourceName: String?,
    epgLastUpdate: Long,
    epgTimeShiftHours: Int,
    playlist: Playlist?,
    epgGuide: EpgGuide?,
    loading: Boolean,
    onDismiss: () -> Unit,
    onPlaylistEnabledChanged: (PlaylistProfile, Boolean) -> Unit,
    onSourceRoleChanged: (PlaylistProfile, SourceRole) -> Unit,
    onMoveSource: (PlaylistProfile, Int) -> Unit,
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
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val tabTitles = listOf(strings.tabPlaylists, strings.tabEpg, strings.tabGeneral)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(if (isCompact) 18.dp else 24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .widthIn(max = 780.dp)
                .fillMaxWidth(if (isCompact) 0.95f else 0.82f)
                .fillMaxHeight(if (isCompact) 0.92f else 0.88f)
                .padding(if (isCompact) 8.dp else 16.dp)
        ) {
            Column(Modifier.fillMaxSize().padding(if (isCompact) 14.dp else 24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.settingsTitle, fontSize = if (isCompact) 20.sp else 24.sp, fontWeight = FontWeight.Bold)
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
                            text = { Text(title, fontSize = if (isCompact) 12.sp else 14.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
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
                            onPlaylistEnabledChanged = onPlaylistEnabledChanged,
                            onSourceRoleChanged = onSourceRoleChanged,
                            onMoveSource = onMoveSource,
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
                            playlist = playlist,
                            epgGuide = epgGuide,
                            loading = loading,
                            onEpgUrl = onEpgUrl,
                            onEpgFile = onEpgFile,
                            onRefreshEpg = onRefreshEpg,
                            onRemoveEpg = onRemoveEpg,
                            onEpgTimeShiftChanged = onEpgTimeShiftChanged
                        )
                        2 -> AboutTab(
                            appLanguage = appLanguage,
                            onLanguageChanged = onLanguageChanged,
                            playChannelsFullscreen = playChannelsFullscreen,
                            onPlayChannelsFullscreenChanged = onPlayChannelsFullscreenChanged,
                            autoPictureInPicture = autoPictureInPicture,
                            onAutoPictureInPictureChanged = onAutoPictureInPictureChanged
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
    onPlaylistEnabledChanged: (PlaylistProfile, Boolean) -> Unit,
    onSourceRoleChanged: (PlaylistProfile, SourceRole) -> Unit,
    onMoveSource: (PlaylistProfile, Int) -> Unit,
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
                items(playlists.sortedBy { it.order }, key = { it.id }) { item ->
                    val isActive = item.enabled
                    val orderedSources = playlists.sortedBy { it.order }
                    val sourceIndex = orderedSources.indexOfFirst { it.id == item.id }
                    val configuration = LocalConfiguration.current
                    val isCompact = configuration.screenWidthDp < 600

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isActive) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isCompact) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (item.source is ContentSource.RemoteUrl) Icons.Default.Link else Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            if (isActive) {
                                                Spacer(Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                ) {
                                                    Text(
                                                        strings.enabledSource.uppercase(),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            "${if (item.channelCount > 0) "${strings.channelsCount(item.channelCount)} · " else ""}${formatLastUpdated(item.lastUpdated)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        SourceRoleSelector(
                                            role = item.role,
                                            enabled = !loading,
                                            onRoleChanged = { onSourceRoleChanged(item, it) }
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Switch(
                                        checked = item.enabled,
                                        onCheckedChange = { onPlaylistEnabledChanged(item, it) },
                                        enabled = !loading && (!item.enabled || playlists.count { it.enabled } > 1)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { onMoveSource(item, -1) },
                                        enabled = !loading && sourceIndex > 0,
                                        modifier = Modifier.size(34.dp)
                                    ) { Icon(Icons.Default.ArrowUpward, contentDescription = strings.moveSourceUp, modifier = Modifier.size(18.dp)) }
                                    IconButton(
                                        onClick = { onMoveSource(item, 1) },
                                        enabled = !loading && sourceIndex in 0 until orderedSources.lastIndex,
                                        modifier = Modifier.size(34.dp)
                                    ) { Icon(Icons.Default.ArrowDownward, contentDescription = strings.moveSourceDown, modifier = Modifier.size(18.dp)) }
                                    if (item.source is ContentSource.RemoteUrl) {
                                        IconButton(onClick = { onRefreshPlaylist(item) }, enabled = !loading, modifier = Modifier.size(34.dp)) {
                                            Icon(Icons.Default.Refresh, contentDescription = strings.refreshPlaylist, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    IconButton(onClick = { onRenamePlaylist(item) }, enabled = !loading, modifier = Modifier.size(34.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = strings.rename, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { onDeletePlaylist(item) }, enabled = !loading, modifier = Modifier.size(34.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        } else {
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
                                                    strings.enabledSource.uppercase(),
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
                                    SourceRoleSelector(
                                        role = item.role,
                                        enabled = !loading,
                                        onRoleChanged = { onSourceRoleChanged(item, it) }
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Switch(
                                        checked = item.enabled,
                                        onCheckedChange = { onPlaylistEnabledChanged(item, it) },
                                        enabled = !loading && (!item.enabled || playlists.count { it.enabled } > 1)
                                    )
                                    IconButton(onClick = { onMoveSource(item, -1) }, enabled = !loading && sourceIndex > 0) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = strings.moveSourceUp)
                                    }
                                    IconButton(onClick = { onMoveSource(item, 1) }, enabled = !loading && sourceIndex in 0 until orderedSources.lastIndex) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = strings.moveSourceDown)
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
}

@Composable
private fun SourceRoleSelector(
    role: SourceRole,
    enabled: Boolean,
    onRoleChanged: (SourceRole) -> Unit
) {
    val strings = LocalAppStrings.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
        ) {
            Text(
                "${strings.sourceRole}: ${sourceRoleLabel(role, strings)}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SourceRole.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(sourceRoleLabel(option, strings)) },
                    onClick = {
                        expanded = false
                        onRoleChanged(option)
                    },
                    trailingIcon = if (option == role) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }
        }
    }
}

private fun sourceRoleLabel(role: SourceRole, strings: Strings): String = when (role) {
    SourceRole.AUTOMATIC -> strings.roleAutomatic
    SourceRole.LIVE_TV -> strings.roleLiveTv
    SourceRole.MOVIES -> strings.roleMovies
    SourceRole.SERIES -> strings.roleSeries
    SourceRole.MOVIES_AND_SERIES -> strings.roleMoviesAndSeries
    SourceRole.EVERYTHING -> strings.roleEverything
}

@Composable
private fun EpgTab(
    epgSource: ContentSource?,
    epgSourceName: String?,
    epgLastUpdate: Long,
    epgTimeShiftHours: Int,
    playlist: Playlist?,
    epgGuide: EpgGuide?,
    loading: Boolean,
    onEpgUrl: () -> Unit,
    onEpgFile: () -> Unit,
    onRefreshEpg: () -> Unit,
    onRemoveEpg: () -> Unit,
    onEpgTimeShiftChanged: (Int) -> Unit
) {
    val strings = LocalAppStrings.current
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600

    val matchedCount = remember(playlist, epgGuide) {
        if (epgGuide == null || playlist == null) 0 else playlist.channels.count { it.tvgId != null && epgGuide.channels.containsKey(it.tvgId) }
    }
    val unmatchedChannels = remember(playlist, epgGuide) {
        if (epgGuide == null || playlist == null) emptyList()
        else playlist.channels.filter { it.tvgId.isNullOrBlank() || !epgGuide.channels.containsKey(it.tvgId) }
    }
    var showUnmatchedInSettings by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(strings.xmltvSource, fontSize = if (isCompact) 16.sp else 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Schedule listings and channel programme timelines.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(if (isCompact) 14.dp else 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (epgSource is ContentSource.RemoteUrl) Icons.Default.Link else Icons.Default.DateRange,
                        contentDescription = null,
                        tint = if (epgSource != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            epgSourceName ?: strings.noEpgConfiguredMsg,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (epgSource != null) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                formatLastUpdated(epgLastUpdate),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (epgSource is ContentSource.RemoteUrl) {
                        IconButton(onClick = onRefreshEpg, enabled = !loading) {
                            Icon(Icons.Default.Refresh, contentDescription = strings.refresh, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                if (isCompact) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onEpgUrl,
                            enabled = !loading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (epgSource is ContentSource.RemoteUrl) strings.edit else strings.remoteUrl, maxLines = 1, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = onEpgFile,
                            enabled = !loading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (epgSource is ContentSource.LocalFile) strings.edit else strings.localFile, maxLines = 1, fontSize = 12.sp)
                        }
                    }
                    if (epgSource != null) {
                        Spacer(Modifier.height(6.dp))
                        TextButton(
                            onClick = onRemoveEpg,
                            enabled = !loading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.removeEpg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        }
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
        }

        if (epgGuide != null && playlist != null) {
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (unmatchedChannels.isNotEmpty()) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, if (unmatchedChannels.isNotEmpty()) MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                strings.epgMatched(matchedCount, playlist.channels.size),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (unmatchedChannels.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                if (unmatchedChannels.isEmpty()) strings.allEpgMatched else strings.unmatchedEpgCount(unmatchedChannels.size),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (unmatchedChannels.isNotEmpty()) {
                            Button(
                                onClick = { showUnmatchedInSettings = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.viewUnmatchedChannels, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            if (showUnmatchedInSettings) {
                UnmatchedEpgDialog(
                    unmatchedChannels = unmatchedChannels,
                    onDismiss = { showUnmatchedInSettings = false },
                    onSelectChannel = { showUnmatchedInSettings = false }
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text(strings.timeShift, fontSize = if (isCompact) 16.sp else 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            strings.timeShiftDesc,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = { if (epgTimeShiftHours > -23) onEpgTimeShiftChanged(epgTimeShiftHours - 1) },
                        enabled = epgTimeShiftHours > -23 && !loading
                    ) {
                        Text("-1 h", fontWeight = FontWeight.Bold)
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.widthIn(min = 90.dp)
                    ) {
                        Box(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                            val shiftDisplay = when {
                                epgTimeShiftHours > 0 -> "+$epgTimeShiftHours h"
                                epgTimeShiftHours < 0 -> "$epgTimeShiftHours h"
                                else -> "0 h"
                            }
                            Text(shiftDisplay, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    FilledTonalButton(
                        onClick = { if (epgTimeShiftHours < 23) onEpgTimeShiftChanged(epgTimeShiftHours + 1) },
                        enabled = epgTimeShiftHours < 23 && !loading
                    ) {
                        Text("+1 h", fontWeight = FontWeight.Bold)
                    }
                }

                if (epgTimeShiftHours != 0) {
                    Spacer(Modifier.height(6.dp))
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
}

@Composable
private fun AboutTab(
    appLanguage: AppLanguage,
    onLanguageChanged: (AppLanguage) -> Unit,
    playChannelsFullscreen: Boolean,
    onPlayChannelsFullscreenChanged: (Boolean) -> Unit,
    autoPictureInPicture: Boolean,
    onAutoPictureInPictureChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalAppStrings.current

    var checkingUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var upToDateNotice by remember { mutableStateOf(false) }
    var updateError by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableFloatStateOf(-1f) }
    var updateDownloaded by remember { mutableStateOf(false) }

    fun doCheckForUpdates() {
        checkingUpdate = true
        upToDateNotice = false
        updateError = null
        scope.launch {
            val info = UpdateManager.checkForUpdates(BuildConfig.VERSION_CODE)
            checkingUpdate = false
            if (info != null) {
                updateInfo = info
                if (withContext(Dispatchers.IO) { UpdateManager.hasPendingInstall(context) }) {
                    downloadProgress = 1f
                    updateDownloaded = true
                }
            } else {
                upToDateNotice = true
            }
        }
    }

    fun startUpdate(info: AppUpdateInfo) {
        downloadProgress = 0f
        updateDownloaded = false
        updateError = null
        scope.launch {
            val result = UpdateManager.downloadAndInstall(context, info.downloadUrl, info.sha256) { progress ->
                downloadProgress = progress
            }
            if (result.isFailure) {
                downloadProgress = -1f
                updateError = result.exceptionOrNull()?.localizedMessage ?: "Download failed"
            } else {
                updateDownloaded = true
            }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShieldMark(Modifier.size(64.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(strings.appName, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Version ${BuildConfig.VERSION_NAME} · Pride1922", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(20.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlayChannelsFullscreenChanged(!playChannelsFullscreen) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(strings.playback, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(strings.playChannelsFullscreen, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text(
                        strings.playChannelsFullscreenDesc,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.width(16.dp))
                Switch(
                    checked = playChannelsFullscreen,
                    onCheckedChange = onPlayChannelsFullscreenChanged
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAutoPictureInPictureChanged(!autoPictureInPicture) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(strings.autoPictureInPicture, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text(
                        strings.autoPictureInPictureDesc,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.width(16.dp))
                Switch(
                    checked = autoPictureInPicture,
                    onCheckedChange = onAutoPictureInPictureChanged
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Updates Card
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
                        Text(strings.checkForUpdates, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "Installed: v${BuildConfig.VERSION_NAME}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    if (!checkingUpdate && downloadProgress < 0f && updateInfo == null) {
                        FilledTonalButton(onClick = ::doCheckForUpdates) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.checkForUpdates)
                        }
                    }
                }

                if (checkingUpdate) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(strings.checkingForUpdates, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (upToDateNotice) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.upToDate, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    }
                }

                updateInfo?.let { info ->
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    strings.updateAvailableDesc("v${info.versionName}"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (info.releaseNotes.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    info.releaseNotes,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.height(12.dp))

                            if (updateDownloaded) {
                                Text(
                                    strings.updateDownloaded,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    strings.installUpdateHint,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        val result = UpdateManager.retryPendingInstall(context)
                                        if (result.isFailure) {
                                            updateDownloaded = false
                                            downloadProgress = -1f
                                            updateError = result.exceptionOrNull()?.localizedMessage ?: "Installation failed"
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(strings.installUpdate)
                                }
                            } else if (downloadProgress >= 0f) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(strings.downloadingUpdate, fontSize = 12.sp)
                                        Text("${(downloadProgress * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { downloadProgress },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { startUpdate(info) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(strings.updateNow)
                                }
                            }
                        }
                    }
                }

                if (updateError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(strings.updateFailed(updateError.orEmpty()), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

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

        Spacer(Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(strings.openSourceSoftware, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    strings.ffmpegAttribution,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/Pride1922/FortressTV/tree/main/third_party/ffmpeg")
                            )
                        )
                    }
                ) {
                    Text(strings.viewSourceCode)
                }
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

@Composable
private fun StartupUpdateDialog(
    updateInfo: AppUpdateInfo,
    progress: Float,
    downloaded: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onStartUpdate: () -> Unit,
    onInstallUpdate: () -> Unit
) {
    val strings = LocalAppStrings.current
    Dialog(onDismissRequest = { if (progress < 0f) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            strings.newUpdateAvailable,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "v${updateInfo.versionName}",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (updateInfo.releaseNotes.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                updateInfo.releaseNotes,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (downloaded) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        strings.updateDownloaded,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        strings.installUpdateHint,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (progress >= 0f) {
                    Spacer(Modifier.height(16.dp))
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(strings.downloadingUpdate, fontSize = 13.sp)
                            Text("${(progress * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                        )
                    }
                }

                if (error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        strings.updateFailed(error),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (downloaded) {
                        Button(onClick = onInstallUpdate) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.installUpdate)
                        }
                    } else if (progress < 0f) {
                        TextButton(onClick = onDismiss) {
                            Text(strings.later)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = onStartUpdate) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.updateNow)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnmatchedEpgDialog(
    unmatchedChannels: List<PlaylistChannel>,
    onDismiss: () -> Unit,
    onSelectChannel: (PlaylistChannel) -> Unit
) {
    val strings = LocalAppStrings.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .padding(8.dp)
        ) {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            strings.unmatchedEpgChannels,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            strings.unmatchedEpgCount(unmatchedChannels.size),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = strings.close)
                    }
                }

                Spacer(Modifier.height(14.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(unmatchedChannels, key = { "${it.position}:${it.streamUrl}" }) { channel ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onSelectChannel(channel)
                                onDismiss()
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (channel.logoUrl != null) {
                                            AsyncImage(
                                                model = channel.logoUrl,
                                                contentDescription = channel.name,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.LiveTv,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        channel.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!channel.group.isNullOrBlank()) {
                                        Text(
                                            channel.group,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                    val reason = when {
                                        channel.tvgId.isNullOrBlank() -> strings.missingTvgId
                                        else -> strings.tvgIdNotFound(channel.tvgId.orEmpty())
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        reason,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.close)
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 1024, heightDp = 600)
@Composable
private fun WelcomePreview() = FortressTVTheme { WelcomeScreen(false, true, null, {}, {}) }
