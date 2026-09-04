package com.fortresstv.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.draw.scale
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Download
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.foundation.shape.CircleShape
import com.fortresstv.player.playlist.PlaylistProfile
import com.fortresstv.player.update.UpdateManager
import com.fortresstv.player.update.AppUpdateInfo
import com.fortresstv.player.BuildConfig
import com.fortresstv.player.ui.AppLanguage
import com.fortresstv.player.ui.LocalAppStrings
import com.fortresstv.player.ui.getStringsFor
import com.fortresstv.player.ui.Strings
import com.fortresstv.player.ui.AppFormFactor
import com.fortresstv.player.ui.FormFactorDetector
import com.fortresstv.player.ui.LocalFormFactor
import com.fortresstv.player.ui.tvFocusHighlight
import com.fortresstv.player.ui.TvPlayerAction
import com.fortresstv.player.ui.TvPlayerKeyDispatcher
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import com.fortresstv.player.backup.BackupManager
import com.fortresstv.player.backup.BackupRestoreResult
import com.fortresstv.player.backup.BackupExportResult
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.focusable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Key
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.fortresstv.player.playlist.M3uParser
import com.fortresstv.player.playlist.Playlist
import com.fortresstv.player.playlist.PlaylistChannel
import com.fortresstv.player.playlist.PlaylistDiskCache
import com.fortresstv.player.playlist.mergeLivePlaylists
import com.fortresstv.player.playlist.SourceRole
import com.fortresstv.player.playlist.searchChannels
import com.fortresstv.player.player.GestureControl
import com.fortresstv.player.player.GestureLevel
import com.fortresstv.player.player.GesturePlayerView
import com.fortresstv.player.player.PlaybackTrackItem
import com.fortresstv.player.player.TrackLanguageMatcher
import com.fortresstv.player.epg.EpgGuide
import com.fortresstv.player.epg.EpgProgramme
import com.fortresstv.player.epg.EpgDiskCache
import com.fortresstv.player.epg.XmlTvParser
import com.fortresstv.player.epg.EpgRepository
import com.fortresstv.player.epg.DefaultEpgRepository
import com.fortresstv.player.epg.snapshot
import com.fortresstv.player.epg.XmlTvTime
import com.fortresstv.player.source.ContentSource
import com.fortresstv.player.source.SourceReader
import com.fortresstv.player.source.isTransientNetworkFailure
import com.fortresstv.player.source.SourceSettingsStore
import com.fortresstv.player.library.VodProgressEntry
import com.fortresstv.player.library.VodProgressKind
import com.fortresstv.player.library.VodProgressStore
import com.fortresstv.player.library.UserLibraryStore
import com.fortresstv.player.library.UserLibraryStore.Companion.libraryId
import com.fortresstv.player.xtream.XtreamCatalog
import com.fortresstv.player.xtream.XtreamCategory
import com.fortresstv.player.xtream.XtreamClient
import com.fortresstv.player.xtream.XtreamMovie
import com.fortresstv.player.xtream.XtreamMovieDetails
import com.fortresstv.player.xtream.XtreamSeries
import com.fortresstv.player.xtream.XtreamSeriesDetails
import com.fortresstv.player.xtream.XtreamEpisode
import com.fortresstv.player.ui.theme.FortressTVTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream
import java.time.LocalDateTime
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
import coil.request.ImageRequest
import coil.size.Precision

class MainActivity : ComponentActivity() {
    var pictureInPictureMode by mutableStateOf(false)
        private set

    private var pictureInPictureEnabled = true
    private var fullscreenVideoPlaying = false
    private val pictureInPictureHandler = Handler(Looper.getMainLooper())
    private var onPictureInPictureDismissed: (() -> Unit)? = null
    private var isActivityResumed = false

    private val confirmPictureInPictureDismissed = Runnable {
        if (!isInPictureInPictureMode && !isActivityResumed && !hasWindowFocus()) {
            android.util.Log.d("FortressTV", "PiP dismissed confirmed - stopping audio & video")
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
            pictureInPictureHandler.postDelayed(confirmPictureInPictureDismissed, 200L)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            pictureInPictureHandler.removeCallbacks(confirmPictureInPictureDismissed)
        }
    }

    override fun onResume() {
        super.onResume()
        isActivityResumed = true
        pictureInPictureHandler.removeCallbacks(confirmPictureInPictureDismissed)
        UpdateManager.resumePendingInstall(this)
    }

    override fun onPause() {
        isActivityResumed = false
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        isActivityResumed = false
        if (!isInPictureInPictureMode) {
            pictureInPictureHandler.removeCallbacks(confirmPictureInPictureDismissed)
            onPictureInPictureDismissed?.invoke()
        }
    }

    override fun onDestroy() {
        pictureInPictureHandler.removeCallbacksAndMessages(null)
        onPictureInPictureDismissed?.invoke()
        onPictureInPictureDismissed = null
        super.onDestroy()
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
            // A large VOD catalogue can expose dozens of new posters during one fling.
            // Keep bitmap decoding from competing with Compose for every available core.
            .bitmapFactoryMaxParallelism(2)
            // Channel logos are shown in fast-scrolling lazy lists. Crossfading every
            // newly visible logo adds animation and drawing work to the scroll frame.
            .crossfade(false)
            .build()
        Coil.setImageLoader(imageLoader)

        setContent {
            val focusManager = LocalFocusManager.current
            val keyboardController = LocalSoftwareKeyboardController.current
            val view = LocalView.current

            val isTvDevice = FormFactorDetector.detectFromConfiguration(resources.configuration).isTelevision

            val keyboardDismissNestedScroll = remember(keyboardController, isTvDevice) {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        if (!isTvDevice && source == NestedScrollSource.UserInput && (available.x != 0f || available.y != 0f)) {
                            keyboardController?.hide()
                        }
                        return Offset.Zero
                    }
                }
            }

            FortressTVTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .semantics { testTagsAsResourceId = true }
                        .then(if (!isTvDevice) Modifier.nestedScroll(keyboardDismissNestedScroll) else Modifier)
                        .then(
                            if (!isTvDevice) {
                                Modifier.pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            keyboardController?.hide()
                                            val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                            imm?.hideSoftInputFromWindow(view.windowToken, 0)
                                        }
                                    )
                                }
                            } else Modifier
                        )
                ) {
                    FortressTVApp()
                }
            }
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        if (ev.action == android.view.MotionEvent.ACTION_DOWN) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            if (imm?.isAcceptingText == true) {
                val view = currentFocus ?: window.decorView
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}

@Composable
private fun FortressTVApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { SourceSettingsStore(context.applicationContext) }
    val userLibrary = remember { UserLibraryStore(context.applicationContext) }
    val vodProgressStore = remember { VodProgressStore(context.applicationContext) }
    val backupManager = remember { BackupManager(settings, userLibrary, vodProgressStore) }

    var playlists by remember { mutableStateOf<List<PlaylistProfile>>(emptyList()) }
    var appLanguage by remember { mutableStateOf(AppLanguage.fromSystemLocale()) }
    var playChannelsFullscreen by remember { mutableStateOf(false) }
    var autoPictureInPicture by remember { mutableStateOf(true) }
    var preferredAudioLanguage by remember { mutableStateOf("default") }
    var preferredSubtitleLanguage by remember { mutableStateOf("off") }
    var subtitleFallbackEnglish by remember { mutableStateOf(true) }
    val strings = getStringsFor(appLanguage)

    var activeProfile by remember { mutableStateOf<PlaylistProfile?>(null) }
    var playlist by remember { mutableStateOf<Playlist?>(null) }
    val loadedPlaylists = remember { mutableStateMapOf<String, Playlist>() }
    val playlistRetryJobs = remember { mutableMapOf<String, Job>() }
    val xtreamCatalogs = remember { mutableStateMapOf<String, XtreamCatalog>() }
    var favoriteChannelIds by remember { mutableStateOf(userLibrary.favoriteChannelIds()) }
    var recentChannelIds by remember { mutableStateOf(userLibrary.recentChannelIds()) }
    var movieWatchlistIds by remember { mutableStateOf(userLibrary.movieWatchlistIds()) }
    var followedSeriesIds by remember { mutableStateOf(userLibrary.followedSeriesIds()) }
    var watchedItemIds by remember { mutableStateOf(userLibrary.watchedItemIds()) }

    var showCreateBackupDialog by remember { mutableStateOf(false) }
    var showRestoreBackupPasswordDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var backupPasswordForExport by remember { mutableStateOf<String?>(null) }
    var restoreErrorMessage by remember { mutableStateOf<String?>(null) }
    var isRestoringBackup by remember { mutableStateOf(false) }

    var epgGuide by remember { mutableStateOf<EpgGuide?>(null) }
    var epgSource by remember { mutableStateOf<ContentSource?>(null) }
    var epgSourceName by remember { mutableStateOf<String?>(null) }
    var epgLastUpdate by remember { mutableLongStateOf(0L) }
    var epgTimeShiftHours by remember { mutableIntStateOf(0) }
    var epgLoadedDays by remember { mutableIntStateOf(3) }
    val epgRepository = remember { DefaultEpgRepository(context) }

    var error by remember { mutableStateOf<String?>(null) }
    var retryLoad by remember { mutableStateOf<(() -> Unit)?>(null) }
    var loading by remember { mutableStateOf(false) }
    var restoring by remember { mutableStateOf(true) }

    var showSettings by remember { mutableStateOf(false) }
    var settingsInitialTab by remember { mutableIntStateOf(0) }
    var showAddPlaylistDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<PlaylistProfile?>(null) }
    var deletingProfile by remember { mutableStateOf<PlaylistProfile?>(null) }
    var showEpgUrlDialog by remember { mutableStateOf(false) }
    var showRemoveEpgConfirm by remember { mutableStateOf(false) }
    var hideAdultContent by remember { mutableStateOf(settings.hideAdultContent()) }
    var parentalPin by remember { mutableStateOf(settings.getParentalPin()) }
    var parentalControlEnabled by remember { mutableStateOf(settings.parentalControlEnabled()) }
    var activePinDialog by remember { mutableStateOf<PinDialogMode?>(null) }
    var isAdultContentUnlockedByPin by remember { mutableStateOf(false) }
    val effectiveHideAdultContent = if (parentalControlEnabled && !isAdultContentUnlockedByPin) true else hideAdultContent

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

    suspend fun parsePlaylistWithCache(
        profile: PlaylistProfile,
        cacheType: String,
        stream: java.io.InputStream,
        allowCachedResult: Boolean
    ): Playlist = withContext(Dispatchers.IO) {
        val fingerprint = SourceReader.fingerprint(context, profile.source, cacheType)
        if (allowCachedResult) {
            PlaylistDiskCache.load(context, profile.id, fingerprint)?.let { cached ->
                stream.close()
                return@withContext cached
            }
        }
        val parsed = parsePlaylist(stream)
        PlaylistDiskCache.save(context, profile.id, fingerprint, parsed)
        parsed
    }

    suspend fun parseEpg(stream: java.io.InputStream, loadedDays: Int = epgLoadedDays): EpgGuide = withContext(Dispatchers.Default) {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
        BufferedInputStream(stream).use { buffered ->
            buffered.mark(2)
            val first = buffered.read()
            val second = buffered.read()
            buffered.reset()
            val decompressed = if (first == 0x1f && second == 0x8b) GZIPInputStream(buffered) else buffered
            XmlTvParser.parse(decompressed, loadedDays = loadedDays)
        }
    }

    suspend fun parseEpgWithCache(
        source: ContentSource,
        stream: java.io.InputStream,
        allowCachedResult: Boolean,
        loadedDays: Int = epgLoadedDays
    ): EpgGuide = withContext(Dispatchers.IO) {
        val baseFingerprint = SourceReader.fingerprint(context, source, "epg")
        val fingerprint = if (baseFingerprint != null) "$baseFingerprint#days=$loadedDays" else null
        if (allowCachedResult && fingerprint != null) {
            EpgDiskCache.loadTodayGuide(context, fingerprint)?.let { cached ->
                stream.close()
                epgRepository.setGuide(cached)
                return@withContext cached
            }
        }
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
        val manifest = BufferedInputStream(stream).use { buffered ->
            buffered.mark(2)
            val first = buffered.read()
            val second = buffered.read()
            buffered.reset()
            val decompressed = if (first == 0x1f && second == 0x8b) GZIPInputStream(buffered) else buffered
            XmlTvParser.parseToDisk(
                input = decompressed,
                context = context,
                fingerprint = fingerprint.orEmpty(),
                loadedDays = loadedDays
            )
        }
        val guide = EpgDiskCache.loadTodayGuide(context, fingerprint) ?: EpgGuide(channels = manifest.channels, manifest = manifest)
        epgRepository.setGuide(guide)
        guide
    }

    fun loadPlaylistProfile(profile: PlaylistProfile, forceRefresh: Boolean = false) {
        loading = true
        error = null
        retryLoad = null
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
                val parsed = parsePlaylistWithCache(
                    profile = profile,
                    cacheType = cacheKey,
                    stream = stream,
                    allowCachedResult = !forceRefresh
                )
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
                    retryLoad = {
                        playlistRetryJobs.remove(profile.id)?.cancel()
                        loadPlaylistProfile(profile, forceRefresh = true)
                    }
                    if (err.isTransientNetworkFailure() && playlistRetryJobs[profile.id]?.isActive != true) {
                        playlistRetryJobs[profile.id] = scope.launch {
                            delay(15_000)
                            playlistRetryJobs.remove(profile.id)
                            val current = withContext(Dispatchers.IO) {
                                settings.getPlaylists().firstOrNull { it.id == profile.id && it.enabled }
                            }
                            if (current != null) loadPlaylistProfile(current, forceRefresh = true)
                        }
                    }
                }
            }
            withContext(Dispatchers.Main) {
                loading = false
                restoring = false
            }
        }
    }

    fun loadXtreamProfile(profile: PlaylistProfile, forceRefresh: Boolean = false) {
        val source = profile.source as? ContentSource.Xtream ?: return
        settings.savePlaylistProfile(profile)
        playlists = settings.getPlaylists()
        loading = true
        error = null
        retryLoad = null
        scope.launch(Dispatchers.IO) {
            runCatching {
                val client = XtreamClient(context.applicationContext, profile.id, source)
                client.loadCatalog(forceRefresh)
            }.onSuccess { catalog ->
                val totalCount = catalog.liveChannels.size + catalog.movies.size + catalog.series.size
                val updated = profile.copy(
                    lastUpdated = System.currentTimeMillis(),
                    channelCount = if (catalog.liveChannels.isNotEmpty()) catalog.liveChannels.size else totalCount
                )
                settings.savePlaylistProfile(updated)
                val all = settings.getPlaylists()
                withContext(Dispatchers.Main) {
                    xtreamCatalogs[profile.id] = catalog
                    if (catalog.liveChannels.isNotEmpty()) {
                        loadedPlaylists[profile.id] = Playlist(catalog.liveChannels)
                    }
                    playlists = all
                    playlist = mergeLivePlaylists(all, loadedPlaylists)
                    if (activeProfile == null) {
                        activeProfile = all.filter { it.enabled }.minByOrNull { it.order }
                    }
                }
            }.onFailure { failure ->
                withContext(Dispatchers.Main) {
                    error = "${profile.name}: ${failure.message ?: "could not load Xtream catalogue"}"
                    retryLoad = { loadXtreamProfile(profile, forceRefresh = true) }
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
        retryLoad = null
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
                    retryLoad = { loadEpg(source, save = save, forceRefresh = true) }
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
        val days = withContext(Dispatchers.IO) { settings.epgLoadedDays() }
        val epgUpdate = withContext(Dispatchers.IO) { settings.lastEpgUpdate() }
        val savedFullscreenPreference = withContext(Dispatchers.IO) { settings.playChannelsFullscreen() }
        val savedPictureInPicturePreference = withContext(Dispatchers.IO) { settings.autoPictureInPicture() }
        val savedAudioLang = withContext(Dispatchers.IO) { settings.preferredAudioLanguage() }
        val savedSubLang = withContext(Dispatchers.IO) { settings.preferredSubtitleLanguage() }
        val savedSubFallback = withContext(Dispatchers.IO) { settings.subtitleFallbackEnglish() }
        appLanguage = AppLanguage.fromCode(savedLangCode)
        epgTimeShiftHours = shift
        epgLoadedDays = days
        epgLastUpdate = epgUpdate
        playChannelsFullscreen = savedFullscreenPreference
        autoPictureInPicture = savedPictureInPicturePreference
        preferredAudioLanguage = savedAudioLang
        preferredSubtitleLanguage = savedSubLang
        subtitleFallbackEnglish = savedSubFallback

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
                        parsePlaylistWithCache(
                            profile,
                            cacheKey,
                            SourceReader.open(context, profile.source),
                            allowCachedResult = true
                        )
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
                        parsePlaylistWithCache(
                            profile,
                            cacheKey,
                            SourceReader.openCached(context, cacheKey),
                            allowCachedResult = true
                        )
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
                        parsePlaylistWithCache(
                            profile,
                            "playlist",
                            SourceReader.openCached(context, "playlist"),
                            allowCachedResult = true
                        )
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
                        val parsed = parsePlaylistWithCache(
                            profile,
                            cacheKey,
                            stream,
                            allowCachedResult = false
                        )
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
                            if (!hasFastLoadedPlaylist) {
                                error = "${profile.name}: ${err.message ?: "could not be loaded"}"
                                retryLoad = {
                                    playlistRetryJobs.remove(profile.id)?.cancel()
                                    loadPlaylistProfile(profile, forceRefresh = true)
                                }
                            }
                            if (err.isTransientNetworkFailure() && playlistRetryJobs[profile.id]?.isActive != true) {
                                playlistRetryJobs[profile.id] = scope.launch {
                                    delay(15_000)
                                    playlistRetryJobs.remove(profile.id)
                                    val current = withContext(Dispatchers.IO) {
                                        settings.getPlaylists().firstOrNull { it.id == profile.id && it.enabled }
                                    }
                                    if (current != null) loadPlaylistProfile(current, forceRefresh = true)
                                }
                            }
                        }
                    }
                    withContext(Dispatchers.Main) {
                        loading = false
                        restoring = false
                    }
                }
            }
        }

        enabledPlaylists.filter { it.source is ContentSource.Xtream }.forEach { profile ->
            loadXtreamProfile(profile)
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

    val createBackupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val pass = backupPasswordForExport
        backupPasswordForExport = null
        if (uri != null && !pass.isNullOrBlank()) {
            scope.launch(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        val result = backupManager.exportBackup(pass.toCharArray(), os, BuildConfig.VERSION_NAME)
                        withContext(Dispatchers.Main) {
                            when (result) {
                                is BackupExportResult.Success -> {
                                    Toast.makeText(context, strings.backupCreatedSuccess, Toast.LENGTH_LONG).show()
                                }
                                is BackupExportResult.Error -> {
                                    Toast.makeText(context, strings.backupFailed(result.message), Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }.onFailure { err ->
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, strings.backupFailed(err.message ?: "Could not save backup"), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    val restoreBackupPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            if (uri.scheme != "file") {
                try {
                    context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: SecurityException) {}
            }
            pendingRestoreUri = uri
            restoreErrorMessage = null
            showRestoreBackupPasswordDialog = true
        }
    }

    fun openLatestLocalBackup(): Boolean {
        val candidateFiles = mutableListOf<File>()
        context.getExternalFilesDir(null)
            ?.listFiles { file -> file.isFile && file.extension.equals("ftv", ignoreCase = true) }
            ?.let(candidateFiles::addAll)
        runCatching {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                ?.listFiles { file -> file.isFile && file.extension.equals("ftv", ignoreCase = true) }
                ?.let(candidateFiles::addAll)
        }
        val bestCandidate = candidateFiles.maxByOrNull(File::lastModified) ?: return false
        pendingRestoreUri = Uri.fromFile(bestCandidate)
        restoreErrorMessage = null
        showRestoreBackupPasswordDialog = true
        return true
    }

    val restoreStoragePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted || !openLatestLocalBackup()) {
            Toast.makeText(
                context,
                if (strings.languageTag == "pt-PT") "Nenhum ficheiro de cópia (.ftv) encontrado." else "No backup file (.ftv) found.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun triggerRestoreBackup() {
        if (openLatestLocalBackup()) return
        if (
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 &&
            context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            restoreStoragePermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            return
        }
        try {
            restoreBackupPicker.launch(arrayOf("*/*", "application/octet-stream"))
        } catch (_: android.content.ActivityNotFoundException) {
            Toast.makeText(context, if (strings.languageTag == "pt-PT") "Nenhum ficheiro de cópia (.ftv) encontrado." else "No backup file (.ftv) found.", Toast.LENGTH_LONG).show()
        }
    }

    fun executeRestore(password: String, merge: Boolean) {
        val uri = pendingRestoreUri ?: return
        isRestoringBackup = true
        restoreErrorMessage = null
        scope.launch(Dispatchers.IO) {
            runCatching {
                val inputStream = when (uri.scheme) {
                    "file" -> uri.path?.let { java.io.FileInputStream(java.io.File(it)) }
                        ?: context.contentResolver.openInputStream(uri)
                    else -> context.contentResolver.openInputStream(uri)
                } ?: throw java.io.IOException("Could not open backup file")
                val result = inputStream.use { stream ->
                    backupManager.restoreBackup(password.toCharArray(), stream, merge)
                }
                withContext(Dispatchers.Main) {
                    when (result) {
                        is BackupRestoreResult.Success -> {
                            isRestoringBackup = false
                            showRestoreBackupPasswordDialog = false
                            pendingRestoreUri = null

                            // Synchronize UI states with restored data
                            val allPlaylists = settings.getPlaylists()
                            playlists = allPlaylists
                            val newLang = AppLanguage.fromCode(settings.getLanguage())
                            appLanguage = newLang
                            playChannelsFullscreen = settings.playChannelsFullscreen()
                            autoPictureInPicture = settings.autoPictureInPicture()
                            preferredAudioLanguage = settings.preferredAudioLanguage()
                            preferredSubtitleLanguage = settings.preferredSubtitleLanguage()
                            subtitleFallbackEnglish = settings.subtitleFallbackEnglish()
                            hideAdultContent = settings.hideAdultContent()
                            parentalControlEnabled = settings.parentalControlEnabled()
                            parentalPin = settings.getParentalPin()
                            epgSource = settings.epgSource()
                            epgSourceName = epgSource?.displayName
                            epgTimeShiftHours = settings.epgTimeShiftHours()
                            epgLoadedDays = settings.epgLoadedDays()
                            favoriteChannelIds = userLibrary.favoriteChannelIds()
                            recentChannelIds = userLibrary.recentChannelIds()
                            movieWatchlistIds = userLibrary.movieWatchlistIds()
                            followedSeriesIds = userLibrary.followedSeriesIds()
                            watchedItemIds = userLibrary.watchedItemIds()

                            // Clear existing cache memory and trigger reload
                            loadedPlaylists.clear()
                            xtreamCatalogs.clear()
                            val nextActive = settings.getActivePlaylist()
                            activeProfile = nextActive
                            if (nextActive != null) {
                                if (nextActive.source is ContentSource.Xtream) loadXtreamProfile(nextActive, forceRefresh = true)
                                else loadPlaylistProfile(nextActive, forceRefresh = true)
                            }
                            val savedEpg = settings.epgSource()
                            if (savedEpg != null) {
                                loadEpg(savedEpg, save = false, forceRefresh = true)
                            }

                            Toast.makeText(
                                context,
                                strings.backupRestoreSuccess(result.restoredPlaylistsCount, result.restoredFavoritesCount),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        is BackupRestoreResult.InvalidPassword -> {
                            isRestoringBackup = false
                            restoreErrorMessage = strings.backupIncorrectPassword
                        }
                        is BackupRestoreResult.InvalidFormat -> {
                            isRestoringBackup = false
                            restoreErrorMessage = strings.backupCorruptFile
                        }
                        is BackupRestoreResult.Error -> {
                            isRestoringBackup = false
                            restoreErrorMessage = strings.backupFailed(result.message)
                        }
                    }
                }
            }.onFailure { err ->
                withContext(Dispatchers.Main) {
                    isRestoringBackup = false
                    restoreErrorMessage = strings.backupFailed(err.message ?: "Failed to read backup file")
                }
            }
        }
    }

    val configuration = LocalConfiguration.current
    val formFactor = remember(configuration) { FormFactorDetector.detectFromConfiguration(configuration) }

    CompositionLocalProvider(
        LocalAppStrings provides strings,
        LocalFormFactor provides formFactor
    ) {
        Box(Modifier.fillMaxSize()) {
            if (restoring) {
                WelcomeScreen(loading = true, showActions = false, error = null, onRetry = null, onAddSource = {}, onRestoreBackup = {})
            } else if (playlist == null || activeProfile == null) {
                WelcomeScreen(
                    loading = loading,
                    showActions = !loading && (activeProfile == null || error != null),
                    error = error,
                    onRetry = retryLoad,
                    onAddSource = { showAddPlaylistDialog = true },
                    onRestoreBackup = { triggerRestoreBackup() },
                    onEditPlaylist = (activeProfile ?: playlists.firstOrNull())?.let { prof -> { editingProfile = prof } },
                    onOpenSettings = {
                        settingsInitialTab = 1
                        showSettings = true
                    }
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
                    epgLoadedDays = epgLoadedDays,
                    epgRepository = epgRepository,
                    playChannelsFullscreen = playChannelsFullscreen,
                    autoPictureInPicture = autoPictureInPicture,
                    preferredAudioLanguage = preferredAudioLanguage,
                    preferredSubtitleLanguage = preferredSubtitleLanguage,
                    subtitleFallbackEnglish = subtitleFallbackEnglish,
                    hideAdultContent = effectiveHideAdultContent,
                    loading = loading,
                    error = error,
                    onRetryLoad = retryLoad,
                    userLibrary = userLibrary,
                    favoriteChannelIds = favoriteChannelIds,
                    recentChannelIds = recentChannelIds,
                    onRecordChannelWatched = { channel ->
                        recentChannelIds = userLibrary.recordChannelWatched(channel)
                    },
                    movieWatchlistIds = movieWatchlistIds,
                    followedSeriesIds = followedSeriesIds,
                    watchedItemIds = watchedItemIds,
                    xtreamCatalogs = xtreamCatalogs.values.toList(),
                    xtreamProfiles = playlists.filter { it.enabled && it.source is ContentSource.Xtream },
                    onToggleFavorite = { channel ->
                        val isFavorite = channel.libraryId()?.let(favoriteChannelIds::contains) == true
                        favoriteChannelIds = userLibrary.setChannelFavorite(channel, !isFavorite)
                    },
                    onToggleMovieWatchlist = { sourceId, movie ->
                        val isWatchlisted = movie.libraryId(sourceId) in movieWatchlistIds
                        movieWatchlistIds = userLibrary.setMovieWatchlisted(sourceId, movie, !isWatchlisted)
                    },
                    onToggleMovieWatched = { sourceId, movie ->
                        val key = movie.libraryId(sourceId)
                        val willBeWatched = key !in watchedItemIds
                        watchedItemIds = userLibrary.setItemWatched(key, willBeWatched)
                    },
                    onToggleSeriesFollow = { sourceId, series ->
                        val isFollowed = series.libraryId(sourceId) in followedSeriesIds
                        followedSeriesIds = userLibrary.setSeriesFollowed(sourceId, series, !isFollowed)
                    },
                    onToggleEpisodeWatched = { sourceId, episode ->
                        val key = episode.libraryId(sourceId)
                        val willBeWatched = key !in watchedItemIds
                        watchedItemIds = userLibrary.setItemWatched(key, willBeWatched)
                    },
                    onMarkItemWatched = { key, watched ->
                        watchedItemIds = userLibrary.setItemWatched(key, watched)
                    },
                    onOpenSettings = { tab ->
                        settingsInitialTab = tab
                        showSettings = true
                    }
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
                preferredAudioLanguage = preferredAudioLanguage,
                onPreferredAudioLanguageChanged = { lang ->
                    preferredAudioLanguage = lang
                    scope.launch(Dispatchers.IO) { settings.setPreferredAudioLanguage(lang) }
                },
                preferredSubtitleLanguage = preferredSubtitleLanguage,
                onPreferredSubtitleLanguageChanged = { lang ->
                    preferredSubtitleLanguage = lang
                    scope.launch(Dispatchers.IO) { settings.setPreferredSubtitleLanguage(lang) }
                },
                subtitleFallbackEnglish = subtitleFallbackEnglish,
                onSubtitleFallbackEnglishChanged = { enabled ->
                    subtitleFallbackEnglish = enabled
                    scope.launch(Dispatchers.IO) { settings.setSubtitleFallbackEnglish(enabled) }
                },
                hideAdultContent = hideAdultContent,
                onHideAdultContentChanged = { enabled ->
                    if (parentalControlEnabled && !isAdultContentUnlockedByPin) {
                        activePinDialog = PinDialogMode.UNLOCK_ADULT
                    } else {
                        hideAdultContent = enabled
                        scope.launch(Dispatchers.IO) { settings.setHideAdultContent(enabled) }
                    }
                },
                parentalControlEnabled = parentalControlEnabled,
                hasParentalPin = parentalPin != null,
                onParentalControlToggled = { enabled ->
                    if (enabled) {
                        if (parentalPin == null) {
                            activePinDialog = PinDialogMode.SET
                        } else {
                            parentalControlEnabled = true
                            scope.launch(Dispatchers.IO) { settings.setParentalControlEnabled(true) }
                        }
                    } else {
                        activePinDialog = PinDialogMode.VERIFY
                    }
                },
                onChangeParentalPin = { activePinDialog = PinDialogMode.SET },
                onRemoveParentalPin = { activePinDialog = PinDialogMode.REMOVE },
                epgSource = epgSource,
                epgSourceName = epgSourceName,
                epgLastUpdate = epgLastUpdate,
                epgTimeShiftHours = epgTimeShiftHours,
                epgLoadedDays = epgLoadedDays,
                playlist = playlist,
                epgGuide = epgGuide,
                hiddenCategories = activeProfile?.id?.let { userLibrary.hiddenCategories(it) } ?: emptySet(),
                loading = loading,
                onDismiss = { showSettings = false },
                onPlaylistEnabledChanged = { profile, enabled ->
                    if (enabled || playlists.count { it.enabled } > 1) {
                        val updated = profile.copy(enabled = enabled)
                        settings.savePlaylistProfile(updated)
                        val all = settings.getPlaylists()
                        playlists = all
                        if (enabled) {
                            if (updated.source is ContentSource.Xtream) loadXtreamProfile(updated)
                            else loadPlaylistProfile(updated)
                        } else {
                            loadedPlaylists.remove(updated.id)
                            xtreamCatalogs.remove(updated.id)
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
                    if (updated.source is ContentSource.Xtream && (loadedPlaylists[updated.id] == null || updated.role.contributesLiveTv())) {
                        loadXtreamProfile(updated)
                    } else {
                        playlist = mergeLivePlaylists(all, loadedPlaylists)
                        activeProfile = all.filter { it.enabled }.minByOrNull { it.order }
                    }
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
                    if (profile.source is ContentSource.Xtream) loadXtreamProfile(profile, forceRefresh = true)
                    else loadPlaylistProfile(profile, forceRefresh = true)
                },
                onAddPlaylist = {
                    showAddPlaylistDialog = true
                },
                onEditPlaylist = { profile ->
                    editingProfile = profile
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
                },
                onEpgLoadedDaysChanged = { days ->
                    val clamped = days.coerceIn(1, 7)
                    epgLoadedDays = clamped
                    settings.saveEpgLoadedDays(clamped)
                    val currentEpg = epgSource ?: settings.epgSource()
                    currentEpg?.let { loadEpg(it, save = false, forceRefresh = false) }
                },
                onCreateBackup = {
                    showCreateBackupDialog = true
                },
                onRestoreBackup = {
                    triggerRestoreBackup()
                }
            )
        }

        if (showCreateBackupDialog) {
            CreateBackupDialog(
                onDismiss = { showCreateBackupDialog = false },
                onConfirm = { password ->
                    showCreateBackupDialog = false
                    backupPasswordForExport = password
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    createBackupFileLauncher.launch("fortresstv-backup-$dateStr.ftv")
                }
            )
        }

        if (showRestoreBackupPasswordDialog && pendingRestoreUri != null) {
            RestoreBackupPasswordDialog(
                onDismiss = {
                    showRestoreBackupPasswordDialog = false
                    pendingRestoreUri = null
                    restoreErrorMessage = null
                },
                onConfirm = { password, merge ->
                    executeRestore(password, merge)
                },
                errorMessage = restoreErrorMessage,
                isRestoring = isRestoringBackup
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
                    PlaylistDiskCache.clear(context, profile.id)
                    loadedPlaylists.remove(profile.id)
                    xtreamCatalogs.remove(profile.id)
                    val remaining = settings.getPlaylists()
                    playlists = remaining
                    playlist = mergeLivePlaylists(remaining, loadedPlaylists)
                    activeProfile = remaining.filter { it.enabled }.minByOrNull { it.order }
                    if (remaining.isEmpty()) {
                        showSettings = false
                        loading = false
                        error = null
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
                onAddXtream = { name, server, username, password ->
                    showAddPlaylistDialog = false
                    val displayName = name.ifBlank { "Xtream" }
                    val profile = PlaylistProfile(
                        id = UUID.randomUUID().toString(),
                        name = displayName,
                        source = ContentSource.Xtream(server.trim(), displayName, username, password),
                        enabled = true,
                        role = SourceRole.EVERYTHING,
                        order = (playlists.maxOfOrNull { it.order } ?: -1) + 1
                    )
                    loadXtreamProfile(profile, forceRefresh = true)
                },
                onPickFile = {
                    showAddPlaylistDialog = false
                    addPlaylistFilePicker.launch(arrayOf("audio/x-mpegurl", "application/x-mpegURL", "text/*", "*/*"))
                }
            )
        }

        editingProfile?.let { profile ->
            EditPlaylistDialog(
                profile = profile,
                onDismiss = { editingProfile = null },
                onConfirm = { newName, newSource, newRole ->
                    editingProfile = null
                    val sourceChanged = profile.source != newSource
                    val updated = profile.copy(name = newName, source = newSource, role = newRole)
                    settings.savePlaylistProfile(updated)
                    val all = settings.getPlaylists()
                    playlists = all
                    if (activeProfile?.id == profile.id) {
                        activeProfile = updated
                    }
                    if (updated.source is ContentSource.Xtream) {
                        if (sourceChanged || loadedPlaylists[updated.id] == null || updated.role.contributesLiveTv()) {
                            loadXtreamProfile(updated, forceRefresh = sourceChanged)
                        } else {
                            playlist = mergeLivePlaylists(all, loadedPlaylists)
                            activeProfile = all.filter { it.enabled }.minByOrNull { it.order }
                        }
                    } else if (updated.source is ContentSource.RemoteUrl && sourceChanged) {
                        loadPlaylistProfile(updated, forceRefresh = true)
                    } else {
                        playlist = mergeLivePlaylists(all, loadedPlaylists)
                        activeProfile = all.filter { it.enabled }.minByOrNull { it.order }
                    }
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

        activePinDialog?.let { mode ->
            PinEntryDialog(
                mode = mode,
                currentPin = parentalPin,
                onSuccess = { newPin ->
                    when (mode) {
                        PinDialogMode.SET -> {
                            parentalPin = newPin
                            parentalControlEnabled = true
                            scope.launch(Dispatchers.IO) {
                                settings.setParentalPin(newPin)
                                settings.setParentalControlEnabled(true)
                            }
                            Toast.makeText(context, strings.pinSetSuccess, Toast.LENGTH_SHORT).show()
                        }
                        PinDialogMode.VERIFY -> {
                            parentalControlEnabled = false
                            scope.launch(Dispatchers.IO) { settings.setParentalControlEnabled(false) }
                        }
                        PinDialogMode.REMOVE -> {
                            parentalPin = null
                            parentalControlEnabled = false
                            scope.launch(Dispatchers.IO) {
                                settings.setParentalPin(null)
                                settings.setParentalControlEnabled(false)
                            }
                            Toast.makeText(context, strings.pinRemovedSuccess, Toast.LENGTH_SHORT).show()
                        }
                        PinDialogMode.UNLOCK_ADULT -> {
                            isAdultContentUnlockedByPin = true
                        }
                        PinDialogMode.UNLOCK_SETTINGS -> {}
                    }
                    activePinDialog = null
                },
                onDismiss = { activePinDialog = null }
            )
        }
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
    onRetry: (() -> Unit)?,
    onAddSource: () -> Unit,
    onRestoreBackup: () -> Unit,
    onEditPlaylist: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600
    val isTv = LocalFormFactor.current.isTelevision
    val addSourceFocusRequester = remember { FocusRequester() }
    val retryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isTv, showActions, error) {
        if (isTv && showActions) {
            delay(150)
            try {
                if (error != null && onRetry != null) retryFocusRequester.requestFocus()
                else addSourceFocusRequester.requestFocus()
            } catch (_: Throwable) {}
        }
    }

    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().padding(if (isCompact) 20.dp else 32.dp), contentAlignment = Alignment.Center) {
            if (onOpenSettings != null && !loading) {
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .tvFocusHighlight(
                            shape = CircleShape,
                            scaleOnFocus = if (isTv) 1.15f else 1.0f
                        )
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = strings.settingsTitle,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

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
                if (showActions) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isTv) {
                            Button(
                                onClick = onAddSource,
                                modifier = Modifier
                                    .widthIn(min = 240.dp)
                                    .height(56.dp)
                                    .focusRequester(addSourceFocusRequester)
                                    .tvFocusHighlight(
                                        shape = ButtonDefaults.shape,
                                        scaleOnFocus = 1.06f
                                    )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(strings.addPlaylist, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        } else {
                            LargeFloatingActionButton(
                                onClick = onAddSource,
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.focusRequester(addSourceFocusRequester)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = strings.addPlaylist,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Text(strings.addPlaylist, fontWeight = FontWeight.SemiBold)
                        }
                        TextButton(
                            onClick = onRestoreBackup,
                            enabled = !loading,
                            modifier = Modifier.tvFocusHighlight(
                                shape = RoundedCornerShape(8.dp),
                                scaleOnFocus = if (isTv) 1.04f else 1.0f
                            )
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.restoreFromBackup, fontSize = 13.sp)
                        }
                    }
                }
                if (error != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onRetry != null) {
                            Button(
                                onClick = onRetry,
                                enabled = !loading,
                                modifier = Modifier
                                    .focusRequester(retryFocusRequester)
                                    .tvFocusHighlight(
                                        scaleOnFocus = if (isTv) 1.05f else 1.0f
                                    )
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.retry)
                            }
                        }
                        if (onEditPlaylist != null) {
                            OutlinedButton(
                                onClick = onEditPlaylist,
                                enabled = !loading,
                                modifier = Modifier.tvFocusHighlight(
                                    scaleOnFocus = if (isTv) 1.05f else 1.0f
                                )
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.editPlaylist)
                            }
                        }
                    }
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
    epgLoadedDays: Int = 3,
    epgRepository: EpgRepository? = null,
    favoriteChannelIds: Set<String> = emptySet(),
    onToggleFavorite: (PlaylistChannel) -> Unit = {},
    categoryOrder: List<String> = emptyList(),
    hiddenCategories: Set<String> = emptySet(),
    initialCategory: String? = null,
    onBack: () -> Unit,
    onWatchChannel: (PlaylistChannel) -> Unit
) {
    val strings = LocalAppStrings.current
    val guideLocale = remember(strings.languageTag) { Locale.forLanguageTag(strings.languageTag) }
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    val dates = remember(today, epgLoadedDays) { List(epgLoadedDays.coerceIn(1, 7)) { today.plusDays(it.toLong()) } }
    var selectedDate by remember { mutableStateOf(today) }
    var selectedProgramme by remember { mutableStateOf<Pair<PlaylistChannel, EpgProgramme>?>(null) }
    var programmeFocusToRestore by remember { mutableStateOf<FocusRequester?>(null) }

    val isTv = LocalFormFactor.current.isTelevision
    val watchButtonFocusRequester = remember { FocusRequester() }
    LaunchedEffect(selectedProgramme) {
        if (selectedProgramme != null && isTv) {
            delay(100)
            try { watchButtonFocusRequester.requestFocus() } catch (_: Throwable) {}
        } else if (selectedProgramme == null && isTv && programmeFocusToRestore != null) {
            delay(100)
            runCatching { programmeFocusToRestore?.requestFocus() }
            programmeFocusToRestore = null
        }
    }
    var now by remember { mutableStateOf(Instant.now()) }
    val horizontalState = rememberScrollState()
    var useWideTimelineWindow by remember { mutableStateOf(false) }
    var initialTimelinePositioned by remember { mutableStateOf(false) }
    val channelState = rememberLazyListState()
    val programmeState = rememberLazyListState()
    var timelineViewportPx by remember { mutableIntStateOf(0) }
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val timelineZoomLevels = remember { listOf(180.dp, 270.dp, 360.dp, 540.dp, 720.dp, 1080.dp, 1440.dp) }
    var timelineZoomIndex by remember { mutableIntStateOf(1) }
    val hourWidth = timelineZoomLevels[timelineZoomIndex]
    val halfHourWidth = hourWidth / 2
    val timelineWidth = hourWidth * 24
    val rowHeight = if (isCompact && isPortrait) 64.dp else 72.dp
    val channelWidth = when {
        isCompact && isPortrait -> 96.dp
        isCompact -> 130.dp
        else -> 220.dp
    }
    val shiftMillis = epgTimeShiftHours * 3_600_000L

    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }

    val allVisibleChannels = remember(playlist.channels, hiddenCategories) {
        if (hiddenCategories.isEmpty()) playlist.channels
        else playlist.channels.filter { (it.group.orEmpty()) !in hiddenCategories }
    }
    val allGroup = strings.allChannels
    val favoritesGroup = "⭐ ${strings.navFavorites}"
    val favoriteChannels = remember(allVisibleChannels, favoriteChannelIds) {
        val channelMap = allVisibleChannels.associateBy { it.libraryId() }
        favoriteChannelIds.mapNotNull { channelMap[it] }
    }
    val categories = remember(allVisibleChannels, categoryOrder, hiddenCategories) {
        val raw = allVisibleChannels.mapNotNull { it.group?.takeIf(String::isNotBlank) }.distinct().filter { it !in hiddenCategories }
        val orderMap = categoryOrder.withIndex().associate { it.value to it.index }
        raw.sortedWith(compareBy({ orderMap[it] ?: Int.MAX_VALUE }, { it }))
    }
    var selectedGroup by remember(initialCategory) {
        mutableStateOf(
            when {
                initialCategory != null && (initialCategory in categories || initialCategory == favoritesGroup || initialCategory == allGroup) -> initialCategory
                else -> initialCategory ?: allGroup
            }
        )
    }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var categorySearchQuery by remember { mutableStateOf("") }
    val channelsInGroup = remember(allVisibleChannels, selectedGroup, allGroup, favoritesGroup, favoriteChannels) {
        when (selectedGroup) {
            allGroup -> allVisibleChannels
            favoritesGroup -> favoriteChannels
            else -> allVisibleChannels.filter { it.group == selectedGroup }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            now = Instant.now()
            delay(60_000)
        }
    }
    LaunchedEffect(programmeState) {
        snapshotFlow { programmeState.firstVisibleItemIndex to programmeState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                if (channelState.firstVisibleItemIndex != index || channelState.firstVisibleItemScrollOffset != offset) {
                    channelState.scrollToItem(index, offset)
                }
            }
    }
    LaunchedEffect(channelState) {
        snapshotFlow { channelState.firstVisibleItemIndex to channelState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                if (programmeState.firstVisibleItemIndex != index || programmeState.firstVisibleItemScrollOffset != offset) {
                    programmeState.scrollToItem(index, offset)
                }
            }
    }
    LaunchedEffect(timelineViewportPx, hourWidth) {
        if (!initialTimelinePositioned && timelineViewportPx > 0 && horizontalState.maxValue > 0) {
            val localNow = now.atZone(zone)
            val targetMinutes = localNow.hour * 60 + localNow.minute
            val targetPx = with(density) { (hourWidth * (targetMinutes / 60f)).roundToPx() }
            horizontalState.scrollTo((targetPx - timelineViewportPx / 2).coerceIn(0, horizontalState.maxValue))
            initialTimelinePositioned = true
        }
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

    val todayGuideBlocks = remember(epgGuide, playlist, dates, zone, shiftMillis, selectedGroup) {
        mutableStateMapOf<PlaylistChannel, List<GuideProgrammeBlock>>()
    }
    var todayGuideReady by remember(epgGuide, playlist, dates, zone, shiftMillis, selectedGroup) {
        mutableStateOf(false)
    }
    val guideDayIndex = remember(epgGuide, playlist, dates, zone, shiftMillis, selectedGroup) {
        mutableStateMapOf<LocalDate, Map<PlaylistChannel, List<GuideProgrammeBlock>>>(
            today to todayGuideBlocks
        )
    }

    var resolvedChannelMap by remember(playlist.channels, epgGuide) {
        mutableStateOf<Map<PlaylistChannel, String?>>(emptyMap())
    }
    var channelResolutionReady by remember(playlist.channels, epgGuide) { mutableStateOf(epgGuide == null) }

    LaunchedEffect(playlist.channels, epgGuide) {
        val guide = epgGuide
        if (guide == null) {
            resolvedChannelMap = emptyMap()
            channelResolutionReady = true
        } else {
            channelResolutionReady = false
            resolvedChannelMap = withContext(Dispatchers.Default) {
                playlist.channels.associateWith { channel ->
                    guide.resolveChannelId(channel.tvgId, channel.name)
                }
            }
            channelResolutionReady = true
        }
    }

    suspend fun computeDayBlocks(
        date: LocalDate,
        dayProgrammesMap: Map<String, List<EpgProgramme>>?
    ): Map<PlaylistChannel, List<GuideProgrammeBlock>> = withContext(Dispatchers.Default) {
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val blocksByEpgChannel = HashMap<String, List<GuideProgrammeBlock>>()
        channelsInGroup.mapNotNull { channel ->
            val resolvedId = resolvedChannelMap[channel] ?: return@mapNotNull null
            val progs = (dayProgrammesMap?.get(resolvedId))
                ?: (if (dayProgrammesMap == null) epgGuide?.findProgrammes(channel.tvgId, channel.name).orEmpty() else emptyList())
            if (progs.isEmpty()) return@mapNotNull null

            val blocks = blocksByEpgChannel.getOrPut(resolvedId) {
                progs.mapNotNull { programme ->
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
                            durationMinutes = ((clippedStop - clippedStart) / 60_000f).coerceIn(5f, 24f * 60f),
                            adjustedStartMillis = adjustedStart,
                            adjustedStopMillis = adjustedStop,
                            timeLabel = "${formatProgrammeTime(Instant.ofEpochMilli(adjustedStart))}–${formatProgrammeTime(Instant.ofEpochMilli(adjustedStop))}"
                        )
                    }
                }
            }
            if (blocks.isEmpty()) null else channel to blocks
        }.toMap()
    }

    var isDayLoading by remember { mutableStateOf(false) }

    LaunchedEffect(epgGuide, epgRepository, playlist, selectedDate, zone, shiftMillis, selectedGroup, resolvedChannelMap, channelResolutionReady) {
        if (!channelResolutionReady) {
            isDayLoading = true
            return@LaunchedEffect
        }
        if ((selectedDate == today && todayGuideReady) ||
            (selectedDate != today && guideDayIndex.containsKey(selectedDate))
        ) {
            isDayLoading = false
            return@LaunchedEffect
        }
        isDayLoading = true
        val date = selectedDate
        val targetEpochDay = date.toEpochDay()
        val dayLoadStartedNanos = android.os.SystemClock.elapsedRealtimeNanos()
        val loadedDay = if (date != today) epgRepository?.loadDay(targetEpochDay) else null
        val partitionLoadMillis = (android.os.SystemClock.elapsedRealtimeNanos() - dayLoadStartedNanos) / 1_000_000.0
        val dayData = if (date == today) epgGuide?.programmes else loadedDay?.programmes

        val blockBuildStartedNanos = android.os.SystemClock.elapsedRealtimeNanos()
        val dayIndex = computeDayBlocks(date, dayData)
        val blockBuildMillis = (android.os.SystemClock.elapsedRealtimeNanos() - blockBuildStartedNanos) / 1_000_000.0
        val partitionMetadata = epgGuide?.manifest?.partitions?.get(targetEpochDay)
        Log.i(
            "FortressTV.EPG",
            "day epochDay=$targetEpochDay partitionMs=${"%.1f".format(partitionLoadMillis)} " +
                "blocksMs=${"%.1f".format(blockBuildMillis)} playlistChannels=${playlist.channels.size} " +
                "matchedChannels=${dayIndex.size} blocks=${dayIndex.values.sumOf { it.size }} " +
                "programmes=${partitionMetadata?.programmeCount ?: 0} " +
                "compressedBytes=${partitionMetadata?.compressedSize ?: 0}"
        )

        if (selectedDate == date) {
            if (date == today) {
                dayIndex.entries.chunked(GUIDE_CHANNEL_BATCH_SIZE).forEach { batch ->
                    batch.forEach { (channel, blocks) -> todayGuideBlocks[channel] = blocks }
                    withFrameNanos { }
                }
                todayGuideReady = true
            } else {
                guideDayIndex[date] = dayIndex
            }
            isDayLoading = false
        }
    }

    LaunchedEffect(epgGuide, epgRepository, playlist, dates, zone, shiftMillis, selectedGroup, resolvedChannelMap, channelResolutionReady) {
        if (!channelResolutionReady) return@LaunchedEffect
        val repository = epgRepository ?: return@LaunchedEffect
        dates.asSequence()
            .filter { it != today }
            .forEach { date ->
                if (!guideDayIndex.containsKey(date)) {
                    val loadedDay = repository.loadDay(date.toEpochDay()) ?: return@forEach
                    val dayIndex = computeDayBlocks(date, loadedDay.programmes)
                    if (!guideDayIndex.containsKey(date)) {
                        guideDayIndex[date] = dayIndex
                    }
                }
            }
    }
    val guideBlocksByChannel = guideDayIndex[selectedDate].orEmpty()

    val visibleChannels = if (searchQuery.isBlank()) {
        channelsInGroup
    } else {
        remember(channelsInGroup, searchQuery, guideBlocksByChannel) {
            val q = searchQuery.trim().lowercase()
            channelsInGroup.filter { channel ->
                channel.name.lowercase().contains(q) ||
                    guideBlocksByChannel[channel]?.any { block ->
                        block.programme.title.lowercase().contains(q) ||
                            block.programme.description?.lowercase()?.contains(q) == true
                    } == true
            }
        }
    }

    val visibleTimelineMinutes by remember(horizontalState, timelineViewportPx, hourWidth, density, useWideTimelineWindow) {
        derivedStateOf {
            val hourWidthPx = with(density) { hourWidth.toPx() }.coerceAtLeast(1f)
            val firstMinute = horizontalState.value / hourWidthPx * 60f
            val visibleMinutes = timelineViewportPx / hourWidthPx * 60f
            if (useWideTimelineWindow) {
                val bucketedFirstMinute = (firstMinute / 120f).toInt() * 120f
                (bucketedFirstMinute - 120f).coerceAtLeast(0f) to
                    (bucketedFirstMinute + visibleMinutes + 180f).coerceAtMost(24f * 60f)
            } else {
                val bucketedFirstMinute = (firstMinute / 15f).toInt() * 15f
                (bucketedFirstMinute - 30f).coerceAtLeast(0f) to
                    (bucketedFirstMinute + visibleMinutes + 45f).coerceAtMost(24f * 60f)
            }
        }
    }

    val scrollToNow: () -> Unit = {
        val localNow = now.atZone(zone)
        val targetMinutes = if (selectedDate == today) {
            localNow.hour * 60 + localNow.minute
        } else 8 * 60
        val targetPx = with(density) { (hourWidth * (targetMinutes / 60f)).roundToPx() }
        coroutineScope.launch {
            horizontalState.animateScrollTo((targetPx - timelineViewportPx / 2).coerceIn(0, horizontalState.maxValue))
        }
    }

    fun dismissGuideLayer(): Boolean {
        return if (selectedProgramme != null) {
            selectedProgramme = null
            true
        } else if (isSearchExpanded) {
            isSearchExpanded = false
            searchQuery = ""
            true
        } else {
            false
        }
    }

    BackHandler {
        if (!dismissGuideLayer()) {
            onBack()
        }
    }

    val guideInitialFocusRequester = remember { FocusRequester() }
    val channelRowFocusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }
    val rowFirstBlockFocusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }
    val programmeBlockFocusRequesters = remember { mutableStateMapOf<String, FocusRequester>() }
    LaunchedEffect(Unit) {
        if (isTv) {
            delay(100)
            try {
                guideInitialFocusRequester.requestFocus()
            } catch (_: Throwable) {}
        }
    }

    Surface(
        Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
            .onPreviewKeyEvent { keyEvent ->
                val isBackKey = keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_BACK ||
                    keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ESCAPE
                if (isTv && isBackKey && (selectedProgramme != null || isSearchExpanded)) {
                    if (keyEvent.type == KeyEventType.KeyUp) dismissGuideLayer()
                    true
                } else {
                    false
                }
            },
        color = MaterialTheme.colorScheme.background
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            if (todayGuideReady) {
                Box(Modifier.size(1.dp).testTag("epg_ready"))
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = if (isCompact) 8.dp else 16.dp, vertical = if (isCompact) 4.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(if (isCompact) 36.dp else 44.dp)
                        .tvFocusHighlight(
                            shape = CircleShape,
                            scaleOnFocus = if (isTv) 1.15f else 1.0f
                        )
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back, modifier = Modifier.size(if (isCompact) 20.dp else 24.dp))
                }
                Spacer(Modifier.width(if (isCompact) 4.dp else 8.dp))
                Column(Modifier.weight(1f, fill = false)) {
                    Text(
                        strings.programmeGuide,
                        fontSize = if (isCompact) 16.sp else 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${visibleChannels.size} ${strings.channelList.lowercase()}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = if (isCompact) 10.sp else 12.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = { isSearchExpanded = !isSearchExpanded },
                    modifier = Modifier
                        .size(if (isCompact) 36.dp else 44.dp)
                        .tvFocusHighlight(
                            shape = CircleShape,
                            scaleOnFocus = if (isTv) 1.15f else 1.0f
                        )
                ) {
                    Icon(
                        if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = strings.search,
                        tint = if (isSearchExpanded || searchQuery.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(if (isCompact) 18.dp else 22.dp)
                    )
                }
                if (!isCompact || !isPortrait) {
                    IconButton(
                        onClick = { timelineZoomIndex = (timelineZoomIndex - 1).coerceAtLeast(0) },
                        enabled = timelineZoomIndex > 0,
                        modifier = Modifier
                            .size(36.dp)
                            .tvFocusHighlight(
                                shape = CircleShape,
                                scaleOnFocus = if (isTv) 1.15f else 1.0f
                            )
                    ) {
                        Icon(Icons.Default.ZoomOut, contentDescription = strings.zoomOutTimeline, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = { timelineZoomIndex = (timelineZoomIndex + 1).coerceAtMost(timelineZoomLevels.lastIndex) },
                        enabled = timelineZoomIndex < timelineZoomLevels.lastIndex,
                        modifier = Modifier
                            .size(36.dp)
                            .tvFocusHighlight(
                                shape = CircleShape,
                                scaleOnFocus = if (isTv) 1.15f else 1.0f
                            )
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = strings.zoomInTimeline, modifier = Modifier.size(20.dp))
                    }
                }
                if (isCompact && isPortrait) {
                    FilledTonalIconButton(
                        onClick = {
                            selectedDate = today
                            selectedProgramme = null
                            scrollToNow()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .focusRequester(guideInitialFocusRequester)
                            .tvFocusHighlight(
                                shape = CircleShape,
                                scaleOnFocus = if (isTv) 1.15f else 1.0f
                            )
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = strings.now, modifier = Modifier.size(16.dp))
                    }
                } else {
                    FilledTonalButton(
                        onClick = {
                            selectedDate = today
                            selectedProgramme = null
                            scrollToNow()
                        },
                        modifier = Modifier
                            .height(36.dp)
                            .focusRequester(guideInitialFocusRequester)
                            .onPreviewKeyEvent { keyEvent ->
                                if (isTv && (keyEvent.type == KeyEventType.KeyDown || keyEvent.type == KeyEventType.KeyUp) && keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN) {
                                    if (keyEvent.type == KeyEventType.KeyUp) {
                                        channelRowFocusRequesters[0]?.let { requester ->
                                            try {
                                                requester.requestFocus()
                                            } catch (_: Throwable) {}
                                        }
                                    }
                                    return@onPreviewKeyEvent true
                                }
                                false
                            }
                            .tvFocusHighlight(
                                shape = ButtonDefaults.shape,
                                scaleOnFocus = if (isTv) 1.06f else 1.0f
                            ),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.now, fontSize = 12.sp)
                    }
                }
            }

            AnimatedVisibility(
                visible = isSearchExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchEpg, fontSize = if (isCompact) 12.sp else 14.sp) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = if (isCompact) 10.dp else 16.dp, vertical = 4.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = if (isCompact) 10.dp else 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Filter Chip Dropdown
                Box {
                    AssistChip(
                        onClick = { categoryMenuExpanded = true },
                        leadingIcon = {
                            Icon(
                                when (selectedGroup) {
                                    favoritesGroup -> Icons.Default.Star
                                    allGroup -> Icons.Default.Menu
                                    else -> Icons.Default.FolderOpen
                                },
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (selectedGroup == favoritesGroup) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = {
                            val displayCategoryName = when (selectedGroup) {
                                allGroup -> strings.allChannels
                                favoritesGroup -> strings.navFavorites
                                else -> selectedGroup
                            }
                            Text(
                                text = displayCategoryName,
                                fontSize = if (isCompact) 11.sp else 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = if (isCompact && isPortrait) 90.dp else 160.dp)
                            )
                        },
                        modifier = Modifier
                            .height(32.dp)
                            .onPreviewKeyEvent { keyEvent ->
                                if (isTv && (keyEvent.type == KeyEventType.KeyDown || keyEvent.type == KeyEventType.KeyUp) && keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN) {
                                    if (keyEvent.type == KeyEventType.KeyUp) {
                                        channelRowFocusRequesters[0]?.let { requester ->
                                            try {
                                                requester.requestFocus()
                                            } catch (_: Throwable) {}
                                        }
                                    }
                                    return@onPreviewKeyEvent true
                                }
                                false
                            }
                            .tvFocusHighlight(
                                shape = AssistChipDefaults.shape,
                                scaleOnFocus = if (isTv) 1.05f else 1.0f
                            )
                    )

                    DropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = {
                            categoryMenuExpanded = false
                            categorySearchQuery = ""
                        },
                        modifier = Modifier.widthIn(min = 260.dp, max = 340.dp).heightIn(max = 420.dp)
                    ) {
                        if (categories.size > 8) {
                            OutlinedTextField(
                                value = categorySearchQuery,
                                onValueChange = { categorySearchQuery = it },
                                placeholder = { Text(strings.search, fontSize = 12.sp) },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            HorizontalDivider()
                        }

                        DropdownMenuItem(
                            text = { Text("${strings.allChannels} (${allVisibleChannels.size})", fontWeight = if (selectedGroup == allGroup) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Menu, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                selectedGroup = allGroup
                                categoryMenuExpanded = false
                                categorySearchQuery = ""
                            }
                        )

                        if (favoriteChannels.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("$favoritesGroup (${favoriteChannels.size})", fontWeight = if (selectedGroup == favoritesGroup) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    selectedGroup = favoritesGroup
                                    categoryMenuExpanded = false
                                    categorySearchQuery = ""
                                }
                            )
                        }

                        if (categories.isNotEmpty()) {
                            HorizontalDivider()

                            val filteredCategories = if (categorySearchQuery.isBlank()) categories
                            else categories.filter { it.contains(categorySearchQuery.trim(), ignoreCase = true) }

                            filteredCategories.forEach { category ->
                                val count = allVisibleChannels.count { it.group == category }
                                DropdownMenuItem(
                                    text = {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                category,
                                                fontWeight = if (selectedGroup == category) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "$count",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.FolderOpen,
                                            contentDescription = null,
                                            tint = if (selectedGroup == category) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    },
                                    onClick = {
                                        selectedGroup = category
                                        categoryMenuExpanded = false
                                        categorySearchQuery = ""
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.width(6.dp))
                Box(Modifier.width(1.dp).height(18.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)))
                Spacer(Modifier.width(6.dp))

                // Date Selector Chips
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
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
                                    else "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, guideLocale)} ${date.dayOfMonth}",
                                    fontSize = if (isCompact) 11.sp else 13.sp
                                )
                            },
                            modifier = Modifier
                                .height(30.dp)
                                .onPreviewKeyEvent { keyEvent ->
                                    if (isTv && (keyEvent.type == KeyEventType.KeyDown || keyEvent.type == KeyEventType.KeyUp) && keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN) {
                                        if (keyEvent.type == KeyEventType.KeyUp) {
                                            channelRowFocusRequesters[0]?.let { requester ->
                                                try {
                                                    requester.requestFocus()
                                                } catch (_: Throwable) {}
                                            }
                                        }
                                        return@onPreviewKeyEvent true
                                    }
                                    false
                                }
                                .tvFocusHighlight(
                                    shape = FilterChipDefaults.shape,
                                    scaleOnFocus = if (isTv) 1.05f else 1.0f
                                )
                        )
                    }
                }
            }

            if (isDayLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = MaterialTheme.colorScheme.primary)
            } else {
                Spacer(Modifier.height(2.dp))
            }
            HorizontalDivider()

            Row(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.width(channelWidth).fillMaxHeight().background(MaterialTheme.colorScheme.surface)) {
                    Box(
                        Modifier.fillMaxWidth().height(44.dp).padding(horizontal = if (isCompact && isPortrait) 8.dp else 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            if (isCompact && isPortrait) strings.channelList else "${strings.channelList} (${visibleChannels.size})",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = if (isCompact && isPortrait) 11.sp else 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    HorizontalDivider()
                    LazyColumn(
                        state = channelState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(visibleChannels, key = { _, it -> "epg-channel-${it.position}:${it.streamUrl}" }) { index, channel ->
                            val isFav = channel.libraryId()?.let(favoriteChannelIds::contains) == true
                            var isChannelFocused by remember { mutableStateOf(false) }
                            val channelFocusRequester = remember { FocusRequester() }
                            channelRowFocusRequesters[index] = channelFocusRequester
                            Surface(
                                onClick = { onWatchChannel(channel) },
                                color = if (isChannelFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                                border = if (isChannelFocused) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(rowHeight)
                                    .focusRequester(channelFocusRequester)
                                    .onFocusChanged { isChannelFocused = it.isFocused }
                                    .onPreviewKeyEvent { keyEvent ->
                                        if (isTv && (keyEvent.type == KeyEventType.KeyDown || keyEvent.type == KeyEventType.KeyUp)) {
                                            when (keyEvent.nativeKeyEvent.keyCode) {
                                                android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                    if (keyEvent.type == KeyEventType.KeyUp) {
                                                        rowFirstBlockFocusRequesters[index]?.let { requester ->
                                                            try {
                                                                requester.requestFocus()
                                                            } catch (_: Throwable) {}
                                                        }
                                                    }
                                                    return@onPreviewKeyEvent true
                                                }
                                                android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                                    if (index == 0) {
                                                        if (keyEvent.type == KeyEventType.KeyUp) {
                                                            try {
                                                                guideInitialFocusRequester.requestFocus()
                                                            } catch (_: Throwable) {}
                                                        }
                                                        return@onPreviewKeyEvent true
                                                    }
                                                }
                                            }
                                        }
                                        false
                                    }
                                    .tvFocusHighlight(
                                        shape = RoundedCornerShape(8.dp),
                                        scaleOnFocus = if (isTv) 1.02f else 1.0f
                                    )
                            ) {
                                Row(
                                    Modifier.fillMaxSize().padding(horizontal = if (isCompact && isPortrait) 6.dp else 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ChannelLogo(
                                        channel = channel,
                                        epgChannel = epgGuide?.findChannel(channel.tvgId, channel.name),
                                        modifier = Modifier.size(if (isCompact && isPortrait) 28.dp else 38.dp)
                                    )
                                    Spacer(Modifier.width(if (isCompact && isPortrait) 6.dp else 8.dp))
                                    Column(Modifier.weight(1f)) {
                                        if (channel.position > 0) {
                                            Text(
                                                "${channel.position}.",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = if (isCompact && isPortrait) 9.sp else 11.sp
                                            )
                                        }
                                        Text(
                                            channel.name,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = if (isCompact && isPortrait) 11.sp else 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (!isCompact || !isPortrait) {
                                        IconButton(
                                            onClick = { onToggleFavorite(channel) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (isFav) Color(0xFFFFB800) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
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
                            itemsIndexed(visibleChannels, key = { _, it -> "epg-row-${it.position}:${it.streamUrl}" }) { rowIndex, channel ->
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
                                    programmeBlocks.forEachIndexed { blockIndex, block ->
                                        val blockWidth = hourWidth * (block.durationMinutes / 60f) - 2.dp
                                        val isCurrent = now.toEpochMilli() in block.adjustedStartMillis until block.adjustedStopMillis
                                        val isSelected = selectedProgramme?.first == channel && selectedProgramme?.second == block.programme
                                        val isQueryMatch = searchQuery.isNotBlank() && (
                                            block.programme.title.contains(searchQuery.trim(), ignoreCase = true) ||
                                            block.programme.description?.contains(searchQuery.trim(), ignoreCase = true) == true
                                        )
                                        val blockFocusKey = "$rowIndex:${block.adjustedStartMillis}:${block.adjustedStopMillis}:${block.programme.title}"
                                        val blockFocusRequester = programmeBlockFocusRequesters.getOrPut(blockFocusKey) { FocusRequester() }
                                        if (blockIndex == 0 || (isCurrent && rowFirstBlockFocusRequesters[rowIndex] == null)) {
                                            rowFirstBlockFocusRequesters[rowIndex] = blockFocusRequester
                                        }
                                        key(block.adjustedStartMillis, block.adjustedStopMillis, block.programme.title) {
                                            var isBlockFocused by remember { mutableStateOf(false) }
                                            Surface(
                                                onClick = {
                                                    programmeFocusToRestore = blockFocusRequester
                                                    selectedProgramme = channel to block.programme
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                color = when {
                                                    isBlockFocused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
                                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                                    isQueryMatch -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                                    isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                                },
                                                border = if (isBlockFocused || isSelected || isQueryMatch) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                                modifier = Modifier
                                                    .offset(x = hourWidth * (block.startMinutes / 60f), y = 5.dp)
                                                    .width(blockWidth)
                                                    .height(rowHeight - 10.dp)
                                                    .focusRequester(blockFocusRequester)
                                                    .onFocusChanged { isBlockFocused = it.isFocused }
                                                    .onPreviewKeyEvent { keyEvent ->
                                                        if (isTv && (keyEvent.type == KeyEventType.KeyDown || keyEvent.type == KeyEventType.KeyUp)) {
                                                            val allBlockIndex = allProgrammeBlocks.indexOfFirst {
                                                                it.adjustedStartMillis == block.adjustedStartMillis &&
                                                                    it.adjustedStopMillis == block.adjustedStopMillis &&
                                                                    it.programme.title == block.programme.title
                                                            }
                                                            when (keyEvent.nativeKeyEvent.keyCode) {
                                                                android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                                    if (keyEvent.type == KeyEventType.KeyUp) {
                                                                        val previousBlock = allProgrammeBlocks.getOrNull(allBlockIndex - 1)
                                                                        if (previousBlock == null) {
                                                                            channelRowFocusRequesters[rowIndex]?.let { requester ->
                                                                                runCatching { requester.requestFocus() }
                                                                            }
                                                                        } else {
                                                                            coroutineScope.launch {
                                                                                val targetPx = with(density) {
                                                                                    (hourWidth * (previousBlock.startMinutes / 60f)).roundToPx()
                                                                                }
                                                                                horizontalState.animateScrollTo(
                                                                                    (targetPx - timelineViewportPx / 5).coerceIn(0, horizontalState.maxValue)
                                                                                )
                                                                                delay(75)
                                                                                val targetKey = "$rowIndex:${previousBlock.adjustedStartMillis}:${previousBlock.adjustedStopMillis}:${previousBlock.programme.title}"
                                                                                runCatching { programmeBlockFocusRequesters[targetKey]?.requestFocus() }
                                                                            }
                                                                        }
                                                                    }
                                                                    return@onPreviewKeyEvent true
                                                                }
                                                                android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                                    val nextBlock = allProgrammeBlocks.getOrNull(allBlockIndex + 1)
                                                                    if (nextBlock != null) {
                                                                        if (keyEvent.type == KeyEventType.KeyUp) {
                                                                            coroutineScope.launch {
                                                                                val targetPx = with(density) {
                                                                                    (hourWidth * (nextBlock.startMinutes / 60f)).roundToPx()
                                                                                }
                                                                                horizontalState.animateScrollTo(
                                                                                    (targetPx - timelineViewportPx / 5).coerceIn(0, horizontalState.maxValue)
                                                                                )
                                                                                delay(75)
                                                                                val targetKey = "$rowIndex:${nextBlock.adjustedStartMillis}:${nextBlock.adjustedStopMillis}:${nextBlock.programme.title}"
                                                                                runCatching { programmeBlockFocusRequesters[targetKey]?.requestFocus() }
                                                                            }
                                                                        }
                                                                        return@onPreviewKeyEvent true
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        false
                                                    }
                                                    .tvFocusHighlight(
                                                        shape = RoundedCornerShape(6.dp),
                                                        scaleOnFocus = if (isTv) 1.03f else 1.0f
                                                    )
                                            ) {
                                                if (blockWidth >= 42.dp) {
                                                    Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            if (isCurrent) {
                                                                Box(
                                                                    Modifier
                                                                        .size(6.dp)
                                                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                                                )
                                                                Spacer(Modifier.width(4.dp))
                                                            }
                                                            Text(
                                                                block.programme.title,
                                                                fontWeight = if (isSelected || isCurrent || isQueryMatch) FontWeight.Bold else FontWeight.SemiBold,
                                                                fontSize = 13.sp,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
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
                val adjustedStart = programme.startEpochMilli + shiftMillis
                val adjustedStop = (programme.stopEpochMilli ?: programme.startEpochMilli) + shiftMillis
                val isLiveNow = now.toEpochMilli() in adjustedStart until adjustedStop
                val programmeProgress = if (isLiveNow && adjustedStop > adjustedStart) {
                    ((now.toEpochMilli() - adjustedStart).toFloat() / (adjustedStop - adjustedStart).toFloat()).coerceIn(0f, 1f)
                } else null

                HorizontalDivider()
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (isCompact) 12.dp else 18.dp, vertical = if (isCompact) 8.dp else 12.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ChannelLogo(
                                channel = channel,
                                epgChannel = epgGuide?.findChannel(channel.tvgId, channel.name),
                                modifier = Modifier.size(if (isCompact) 36.dp else 44.dp)
                            )
                            Spacer(Modifier.width(if (isCompact) 10.dp else 14.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isLiveNow) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                strings.liveBadge,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    Text(
                                        programme.title,
                                        fontSize = if (isCompact) 15.sp else 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "${channel.name} · ${formatProgrammeTime(Instant.ofEpochMilli(adjustedStart))}–${formatProgrammeTime(Instant.ofEpochMilli(adjustedStop))}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = if (isCompact) 11.sp else 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { onWatchChannel(channel) },
                                modifier = Modifier
                                    .height(36.dp)
                                    .focusRequester(watchButtonFocusRequester)
                                    .tvFocusHighlight(
                                        shape = ButtonDefaults.shape,
                                        scaleOnFocus = if (isTv) 1.05f else 1.0f
                                    ),
                                contentPadding = PaddingValues(horizontal = if (isCompact) 10.dp else 14.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                if (!isCompact || !isPortrait) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (isLiveNow) strings.watchNow else strings.watchLiveChannel, fontSize = 12.sp)
                                }
                            }
                        }

                        if (programmeProgress != null) {
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { programmeProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }

                        programme.description?.let { desc ->
                            if (desc.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    desc,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
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

private val ADULT_CONTENT_PATTERNS = listOf(
    "xxx", "adult", "adulto", "adultos", "porn", "porno", "hardcore",
    "erotic", "erotico", "erótica", "erotica", "erótico", "18+", "+18",
    "brazzers", "playboy", "penthouse", "hustler", "dorcel", "redlight",
    "venus", "sextreme", "vivid", "private tv", "fetish", "for adults", "para adultos"
)

fun isAdultContent(name: String?, groupOrCategory: String?): Boolean {
    val text = "${name.orEmpty()} ${groupOrCategory.orEmpty()}".lowercase()
    if (text.isBlank()) return false
    return ADULT_CONTENT_PATTERNS.any { pattern -> text.contains(pattern) }
}

private enum class LibraryDestination { LIVE_TV, MOVIES, SERIES, FAVORITES }

private data class VodPlaybackRequest(
    val key: String,
    val kind: VodProgressKind,
    val sourceId: String,
    val itemId: String,
    val seriesId: Int? = null,
    val title: String,
    val posterUrl: String?,
    val containerExtension: String,
    val season: Int? = null,
    val episode: Int? = null,
    val streamUrl: String
)

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
    epgLoadedDays: Int = 3,
    epgRepository: EpgRepository? = null,
    playChannelsFullscreen: Boolean,
    autoPictureInPicture: Boolean,
    preferredAudioLanguage: String = "default",
    preferredSubtitleLanguage: String = "off",
    subtitleFallbackEnglish: Boolean = true,
    hideAdultContent: Boolean = false,
    loading: Boolean,
    error: String?,
    userLibrary: UserLibraryStore,
    favoriteChannelIds: Set<String>,
    recentChannelIds: List<String>,
    onRecordChannelWatched: (PlaylistChannel) -> Unit,
    movieWatchlistIds: Set<String>,
    followedSeriesIds: Set<String>,
    watchedItemIds: Set<String>,
    xtreamCatalogs: List<XtreamCatalog>,
    xtreamProfiles: List<PlaylistProfile>,
    onToggleFavorite: (PlaylistChannel) -> Unit,
    onToggleMovieWatchlist: (String, XtreamMovie) -> Unit,
    onToggleMovieWatched: (String, XtreamMovie) -> Unit,
    onToggleSeriesFollow: (String, XtreamSeries) -> Unit,
    onToggleEpisodeWatched: (String, XtreamEpisode) -> Unit,
    onMarkItemWatched: (String, Boolean) -> Unit,
    onRetryLoad: (() -> Unit)?,
    onOpenSettings: (Int) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val progressStore = remember { VodProgressStore(context.applicationContext) }
    var progressEntries by remember { mutableStateOf(progressStore.entries()) }
    var hiddenCategories by remember(activeProfile.id) {
        mutableStateOf(userLibrary.hiddenCategories(activeProfile.id))
    }
    var categoryOrder by remember(activeProfile.id) {
        mutableStateOf(userLibrary.categoryOrder(activeProfile.id))
    }
    val libraryConfiguration = LocalConfiguration.current
    val compactLibraryNavigation = libraryConfiguration.screenWidthDp < 700
    var destination by remember { mutableStateOf(LibraryDestination.LIVE_TV) }
    var requestedChannel by remember { mutableStateOf<PlaylistChannel?>(null) }
    var vodPlayback by remember { mutableStateOf<VodPlaybackRequest?>(null) }
    var globalSidebarExpanded by remember { mutableStateOf(false) }
    // Keep catalogue positions alive while the user moves between primary destinations.
    val movieGridState = rememberLazyGridState()
    val seriesGridState = rememberLazyGridState()
    val destinations = listOf(
        Triple(LibraryDestination.LIVE_TV, strings.navLiveTv, Icons.Default.LiveTv),
        Triple(LibraryDestination.MOVIES, strings.navMovies, Icons.Default.Movie),
        Triple(LibraryDestination.SERIES, strings.navSeries, Icons.Default.VideoLibrary),
        Triple(LibraryDestination.FAVORITES, strings.navFavorites, Icons.Default.Star)
    )

    val effectivePlaylist = remember(playlist, hideAdultContent) {
        if (!hideAdultContent) playlist
        else {
            val filtered = playlist.channels.filterNot { isAdultContent(it.name, it.group) }
            Playlist(filtered)
        }
    }
    val isTv = LocalFormFactor.current.isTelevision
    var sidebarHasFocus by remember { mutableStateOf(false) }
    val selectedItemFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isTv, destination) {
        if (isTv) {
            delay(150)
            try {
                selectedItemFocusRequester.requestFocus()
            } catch (_: Throwable) {}
        }
    }

    BackHandler(globalSidebarExpanded || (isTv && !sidebarHasFocus)) {
        if (globalSidebarExpanded) {
            globalSidebarExpanded = false
        } else if (isTv && !sidebarHasFocus) {
            try {
                selectedItemFocusRequester.requestFocus()
            } catch (_: Throwable) {}
        }
    }

    Row(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        GlobalLibrarySidebar(
            expanded = globalSidebarExpanded,
            destinations = destinations,
            selectedDestination = destination,
            onDestinationSelected = {
                destination = it
                if (compactLibraryNavigation) globalSidebarExpanded = false
            },
            onToggleExpanded = { globalSidebarExpanded = !globalSidebarExpanded },
            onOpenSettings = {
                if (compactLibraryNavigation) globalSidebarExpanded = false
                onOpenSettings(0)
            },
            onFocusChanged = { sidebarHasFocus = it },
            selectedItemFocusRequester = selectedItemFocusRequester,
            contentFocusRequester = contentFocusRequester
        )
        Box(Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.background)) {
            when (destination) {
                LibraryDestination.LIVE_TV -> ChannelBrowser(
                    playlist = effectivePlaylist,
                    activeProfile = activeProfile,
                    contentFocusRequester = contentFocusRequester,
                    sidebarFocusRequester = selectedItemFocusRequester,
                    enabledSourceCount = enabledSourceCount,
                    enabledSources = enabledSources,
                    epgGuide = epgGuide,
                    epgSourceName = epgSourceName,
                    epgTimeShiftHours = epgTimeShiftHours,
                    epgLoadedDays = epgLoadedDays,
                    epgRepository = epgRepository,
                    playChannelsFullscreen = playChannelsFullscreen,
                    autoPictureInPicture = autoPictureInPicture,
                    loading = loading,
                    error = error,
                    onRetryLoad = onRetryLoad,
                    favoriteChannelIds = favoriteChannelIds,
                    recentChannelIds = recentChannelIds,
                    onRecordChannelWatched = onRecordChannelWatched,
                    hiddenCategories = hiddenCategories,
                    categoryOrder = categoryOrder,
                    onToggleCategoryHidden = { category ->
                        val isHidden = category in hiddenCategories
                        hiddenCategories = userLibrary.setCategoryHidden(activeProfile.id, category, !isHidden)
                    },
                    onMoveCategory = { category, moveUp ->
                        val list = (if (categoryOrder.isEmpty()) effectivePlaylist.groups else categoryOrder).toMutableList()
                        if (category !in list) list.add(category)
                        val index = list.indexOf(category)
                        if (moveUp && index > 0) {
                            list.removeAt(index)
                            list.add(index - 1, category)
                        } else if (!moveUp && index < list.size - 1) {
                            list.removeAt(index)
                            list.add(index + 1, category)
                        }
                        categoryOrder = userLibrary.setCategoryOrder(activeProfile.id, list)
                    },
                    onUnhideAllCategories = {
                        hiddenCategories = userLibrary.unhideAllCategories(activeProfile.id)
                    },
                    onHideAllCategories = { cats ->
                        hiddenCategories = userLibrary.hideAllCategories(activeProfile.id, cats)
                    },
                    onResetCategoryOrder = {
                        categoryOrder = userLibrary.resetCategoryOrder(activeProfile.id)
                    },
                    onToggleFavorite = onToggleFavorite,
                    requestedChannel = requestedChannel,
                    onRequestedChannelHandled = { requestedChannel = null },
                    onOpenSettings = onOpenSettings
                )
                LibraryDestination.MOVIES -> XtreamMoviesScreen(
                    catalogs = xtreamCatalogs,
                    profiles = xtreamProfiles,
                    contentFocusRequester = contentFocusRequester,
                    continueWatching = progressEntries.filter { it.kind == VodProgressKind.MOVIE },
                    progressEntries = progressEntries,
                    watchedItemIds = watchedItemIds,
                    onContinueWatching = { entry ->
                        vodPlayback = resolveVodPlayback(entry, xtreamCatalogs, xtreamProfiles, context)
                    },
                    onRemoveContinueWatching = { entry -> progressEntries = progressStore.remove(entry.key) },
                    hideAdultContent = hideAdultContent,
                    gridState = movieGridState,
                    movieWatchlistIds = movieWatchlistIds,
                    onToggleWatchlist = onToggleMovieWatchlist,
                    onToggleMovieWatched = { sourceId, movie ->
                        val key = movie.libraryId(sourceId)
                        val willBeWatched = key !in watchedItemIds
                        if (willBeWatched) progressEntries = progressStore.remove(key)
                        onToggleMovieWatched(sourceId, movie)
                    },
                    onPlay = { catalog, movie ->
                        val source = xtreamProfiles.firstOrNull { it.id == catalog.sourceId }?.source as? ContentSource.Xtream
                        if (source != null) {
                            vodPlayback = VodPlaybackRequest(
                                key = movie.libraryId(catalog.sourceId),
                                kind = VodProgressKind.MOVIE,
                                sourceId = catalog.sourceId,
                                itemId = movie.id.toString(),
                                title = movie.name,
                                posterUrl = movie.posterUrl,
                                containerExtension = movie.containerExtension,
                                streamUrl = XtreamClient(context.applicationContext, catalog.sourceId, source).movieStreamUrl(movie)
                            )
                        }
                    }
                )
                LibraryDestination.SERIES -> XtreamSeriesScreen(
                    catalogs = xtreamCatalogs,
                    profiles = xtreamProfiles,
                    contentFocusRequester = contentFocusRequester,
                    hideAdultContent = hideAdultContent,
                    gridState = seriesGridState,
                    followedSeriesIds = followedSeriesIds,
                    watchedItemIds = watchedItemIds,
                    continueWatching = progressEntries.filter { it.kind == VodProgressKind.EPISODE },
                    progressEntries = progressEntries,
                    onContinueWatching = { entry ->
                        vodPlayback = resolveVodPlayback(entry, xtreamCatalogs, xtreamProfiles, context)
                    },
                    onRemoveContinueWatching = { entry -> progressEntries = progressStore.remove(entry.key) },
                    onToggleFollow = onToggleSeriesFollow,
                    onToggleEpisodeWatched = { sourceId, ep ->
                        val key = ep.libraryId(sourceId)
                        val willBeWatched = key !in watchedItemIds
                        if (willBeWatched) progressEntries = progressStore.remove(key)
                        onToggleEpisodeWatched(sourceId, ep)
                    },
                    onPlay = { catalog, series, episode, url ->
                        vodPlayback = VodPlaybackRequest(
                            key = "episode:${catalog.sourceId}:${episode.id}",
                            kind = VodProgressKind.EPISODE,
                            sourceId = catalog.sourceId,
                            itemId = episode.id,
                            seriesId = series.id,
                            title = "${strings.episodeLabel(episode.season, episode.episodeNumber)} · ${episode.title}",
                            posterUrl = episode.posterUrl ?: series.coverUrl,
                            containerExtension = episode.containerExtension,
                            season = episode.season,
                            episode = episode.episodeNumber,
                            streamUrl = url
                        )
                    }
                )
                LibraryDestination.FAVORITES -> FavoriteLibraryScreen(
                    channels = effectivePlaylist.channels.filter { it.libraryId()?.let(favoriteChannelIds::contains) == true },
                    catalogs = xtreamCatalogs,
                    profiles = xtreamProfiles,
                    contentFocusRequester = contentFocusRequester,
                    epgGuide = epgGuide,
                    epgTimeShiftHours = epgTimeShiftHours,
                    favoriteChannelIds = favoriteChannelIds,
                    movieWatchlistIds = movieWatchlistIds,
                    followedSeriesIds = followedSeriesIds,
                    watchedItemIds = watchedItemIds,
                    progressEntries = progressEntries,
                    hideAdultContent = hideAdultContent,
                    onToggleFavorite = onToggleFavorite,
                    onToggleMovieWatchlist = onToggleMovieWatchlist,
                    onToggleMovieWatched = { sourceId, movie ->
                        val key = movie.libraryId(sourceId)
                        val willBeWatched = key !in watchedItemIds
                        if (willBeWatched) progressEntries = progressStore.remove(key)
                        onToggleMovieWatched(sourceId, movie)
                    },
                    onToggleSeriesFollow = onToggleSeriesFollow,
                    onToggleEpisodeWatched = { sourceId, ep ->
                        val key = ep.libraryId(sourceId)
                        val willBeWatched = key !in watchedItemIds
                        if (willBeWatched) progressEntries = progressStore.remove(key)
                        onToggleEpisodeWatched(sourceId, ep)
                    },
                    onPlayMovie = { catalog, movie ->
                        val source = xtreamProfiles.firstOrNull { it.id == catalog.sourceId }?.source as? ContentSource.Xtream
                        if (source != null) {
                            vodPlayback = VodPlaybackRequest(
                                key = movie.libraryId(catalog.sourceId), kind = VodProgressKind.MOVIE,
                                sourceId = catalog.sourceId, itemId = movie.id.toString(), title = movie.name,
                                posterUrl = movie.posterUrl, containerExtension = movie.containerExtension,
                                streamUrl = XtreamClient(context.applicationContext, catalog.sourceId, source).movieStreamUrl(movie)
                            )
                        }
                    },
                    onPlayEpisode = { catalog, series, episode, url ->
                        vodPlayback = VodPlaybackRequest(
                            key = "episode:${catalog.sourceId}:${episode.id}", kind = VodProgressKind.EPISODE,
                            sourceId = catalog.sourceId, itemId = episode.id, seriesId = series.id,
                            title = "${strings.episodeLabel(episode.season, episode.episodeNumber)} · ${episode.title}",
                            posterUrl = episode.posterUrl ?: series.coverUrl, containerExtension = episode.containerExtension,
                            season = episode.season, episode = episode.episodeNumber, streamUrl = url
                        )
                    },
                    onWatch = { channel ->
                        requestedChannel = channel
                        destination = LibraryDestination.LIVE_TV
                    }
                )
            }
        }
        vodPlayback?.let { request ->
            val previous = progressEntries.firstOrNull { it.key == request.key }
            VodPlayerDialog(
                request = request,
                initialPositionMs = previous?.positionMs ?: 0L,
                preferredAudioLanguage = preferredAudioLanguage,
                preferredSubtitleLanguage = preferredSubtitleLanguage,
                subtitleFallbackEnglish = subtitleFallbackEnglish,
                loadNextEpisode = if (request.kind == VodProgressKind.EPISODE) {
                    suspend { resolveNextEpisodePlayback(request, xtreamProfiles, context) }
                } else null,
                loadAllEpisodes = if (request.kind == VodProgressKind.EPISODE) {
                    suspend { resolveAllSeriesEpisodesPlayback(request, xtreamProfiles, context) }
                } else null,
                onPlayNext = { next -> vodPlayback = next },
                onSaveProgress = { positionMs, durationMs ->
                    val isCompleted = durationMs > 0 && positionMs >= durationMs * 0.95
                    if (isCompleted) {
                        onMarkItemWatched(request.key, true)
                    }
                    progressEntries = progressStore.save(
                        VodProgressEntry(
                            key = request.key, kind = request.kind, sourceId = request.sourceId,
                            itemId = request.itemId, seriesId = request.seriesId, title = request.title,
                            posterUrl = request.posterUrl, containerExtension = request.containerExtension,
                            season = request.season, episode = request.episode, positionMs = positionMs,
                            durationMs = durationMs, updatedAtMs = System.currentTimeMillis()
                        )
                    )
                },
                onDismiss = { vodPlayback = null }
            )
        }
    }
}

@Composable
private fun GlobalLibrarySidebar(
    expanded: Boolean,
    destinations: List<Triple<LibraryDestination, String, ImageVector>>,
    selectedDestination: LibraryDestination,
    onDestinationSelected: (LibraryDestination) -> Unit,
    onToggleExpanded: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    selectedItemFocusRequester: FocusRequester? = null,
    contentFocusRequester: FocusRequester? = null
) {
    val strings = LocalAppStrings.current
    val isTv = LocalFormFactor.current.isTelevision
    val menuToggleFocusRequester = remember { FocusRequester() }
    val effectiveExpanded = expanded

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        shadowElevation = 4.dp,
        modifier = modifier
            .width(if (effectiveExpanded) 220.dp else 76.dp)
            .fillMaxHeight()
            .onFocusChanged { state ->
                onFocusChanged?.invoke(state.hasFocus)
            }
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (effectiveExpanded) {
                Row(
                    Modifier.fillMaxWidth().height(48.dp).padding(start = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.appName, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = onToggleExpanded,
                        modifier = Modifier
                            .focusRequester(menuToggleFocusRequester)
                            .tvFocusHighlight(
                                shape = CircleShape,
                                scaleOnFocus = if (isTv) 1.15f else 1.0f
                            )
                            .onPreviewKeyEvent { keyEvent ->
                                if (isTv && keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT && contentFocusRequester != null) {
                                    if (keyEvent.type == KeyEventType.KeyUp) {
                                        try { contentFocusRequester.requestFocus() } catch (_: Throwable) {}
                                    }
                                    true
                                } else false
                            }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuOpen, contentDescription = strings.collapseSidebar)
                    }
                }
            } else {
                IconButton(
                    onClick = onToggleExpanded,
                    modifier = Modifier
                        .size(48.dp)
                        .focusRequester(menuToggleFocusRequester)
                        .tvFocusHighlight(
                            shape = CircleShape,
                            scaleOnFocus = if (isTv) 1.15f else 1.0f
                        )
                        .onPreviewKeyEvent { keyEvent ->
                            if (isTv && keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT && contentFocusRequester != null) {
                                if (keyEvent.type == KeyEventType.KeyUp) {
                                    try { contentFocusRequester.requestFocus() } catch (_: Throwable) {}
                                }
                                true
                            } else false
                        }
                ) {
                    Icon(Icons.Default.Menu, contentDescription = strings.expandSidebar)
                }
            }
            Spacer(Modifier.height(18.dp))
            destinations.forEachIndexed { index, (item, label, icon) ->
                val isSelected = selectedDestination == item
                LibraryNavigationItem(
                    label = label,
                    icon = icon,
                    expanded = effectiveExpanded,
                    selected = isSelected,
                    testTag = "library_destination_${item.name.lowercase()}",
                    modifier = Modifier
                        .focusProperties {
                            if (index == 0) {
                                up = menuToggleFocusRequester
                            }
                        }
                        .then(if (isSelected && selectedItemFocusRequester != null) {
                            Modifier.focusRequester(selectedItemFocusRequester)
                        } else Modifier)
                        .onPreviewKeyEvent { keyEvent ->
                            if (isTv && keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) {
                                if (contentFocusRequester != null) {
                                    if (keyEvent.type == KeyEventType.KeyUp) {
                                        try {
                                            contentFocusRequester.requestFocus()
                                        } catch (_: Throwable) {}
                                    }
                                    true
                                } else false
                            } else false
                        },
                    onClick = { onDestinationSelected(item) }
                )
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.weight(1f))
            HorizontalDivider(Modifier.padding(horizontal = 4.dp))
            Spacer(Modifier.height(10.dp))
            LibraryNavigationItem(
                label = strings.settings,
                icon = Icons.Default.Settings,
                expanded = effectiveExpanded,
                selected = false,
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun LibraryNavigationItem(
    label: String,
    icon: ImageVector,
    expanded: Boolean,
    selected: Boolean,
    testTag: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isTv = LocalFormFactor.current.isTelevision
    var isFocused by remember { mutableStateOf(false) }

    val surfaceColor = when {
        isFocused && selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
        isFocused -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else -> Color.Transparent
    }
    val surfaceBorder = when {
        isFocused -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        selected -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
        else -> null
    }
    val contentColor = when {
        isFocused && selected -> MaterialTheme.colorScheme.primary
        isFocused -> MaterialTheme.colorScheme.onSurface
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = surfaceColor,
        border = surfaceBorder,
        contentColor = contentColor,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .onFocusChanged {
                isFocused = it.isFocused
            }
            .tvFocusHighlight(
                shape = RoundedCornerShape(12.dp),
                scaleOnFocus = if (isTv) 1.04f else 1.0f
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = if (expanded) 12.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp))
            if (expanded) {
                Spacer(Modifier.width(12.dp))
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
}

private data class CatalogMovie(val catalog: XtreamCatalog, val movie: XtreamMovie)
private data class CatalogSeries(val catalog: XtreamCatalog, val series: XtreamSeries)
private data class CatalogCategory(
    val sourceId: String,
    val sourceName: String,
    val category: XtreamCategory
)

private fun resolveVodPlayback(
    entry: VodProgressEntry,
    catalogs: List<XtreamCatalog>,
    profiles: List<PlaylistProfile>,
    context: Context
): VodPlaybackRequest? {
    val source = profiles.firstOrNull { it.id == entry.sourceId }?.source as? ContentSource.Xtream ?: return null
    val client = XtreamClient(context.applicationContext, entry.sourceId, source)
    val streamUrl = when (entry.kind) {
        VodProgressKind.MOVIE -> {
            val movie = catalogs.firstOrNull { it.sourceId == entry.sourceId }
                ?.movies?.firstOrNull { it.id.toString() == entry.itemId } ?: return null
            client.movieStreamUrl(movie)
        }
        VodProgressKind.EPISODE -> client.episodeStreamUrl(
            XtreamEpisode(
                id = entry.itemId,
                title = entry.title,
                season = entry.season ?: 0,
                episodeNumber = entry.episode ?: 0,
                containerExtension = entry.containerExtension,
                posterUrl = entry.posterUrl,
                plot = null
            )
        )
    }
    return VodPlaybackRequest(
        key = entry.key, kind = entry.kind, sourceId = entry.sourceId, itemId = entry.itemId,
        seriesId = entry.seriesId, title = entry.title, posterUrl = entry.posterUrl,
        containerExtension = entry.containerExtension, season = entry.season, episode = entry.episode,
        streamUrl = streamUrl
    )
}

private suspend fun resolveNextEpisodePlayback(
    current: VodPlaybackRequest,
    profiles: List<PlaylistProfile>,
    context: Context
): VodPlaybackRequest? {
    val seriesId = current.seriesId ?: return null
    val source = profiles.firstOrNull { it.id == current.sourceId }?.source as? ContentSource.Xtream ?: return null
    val client = XtreamClient(context.applicationContext, current.sourceId, source)
    val episodes = client.loadSeriesDetails(seriesId).episodes
    val currentIndex = episodes.indexOfFirst { it.id == current.itemId }
    val next = episodes.getOrNull(currentIndex + 1) ?: return null
    return VodPlaybackRequest(
        key = "episode:${current.sourceId}:${next.id}",
        kind = VodProgressKind.EPISODE,
        sourceId = current.sourceId,
        itemId = next.id,
        seriesId = seriesId,
        title = next.title,
        posterUrl = next.posterUrl ?: current.posterUrl,
        containerExtension = next.containerExtension,
        season = next.season,
        episode = next.episodeNumber,
        streamUrl = client.episodeStreamUrl(next)
    )
}

private suspend fun resolveAllSeriesEpisodesPlayback(
    current: VodPlaybackRequest,
    profiles: List<PlaylistProfile>,
    context: Context
): List<VodPlaybackRequest> {
    val seriesId = current.seriesId ?: return emptyList()
    val source = profiles.firstOrNull { it.id == current.sourceId }?.source as? ContentSource.Xtream ?: return emptyList()
    val client = XtreamClient(context.applicationContext, current.sourceId, source)
    val episodes = client.loadSeriesDetails(seriesId).episodes
    return episodes.map { episode ->
        VodPlaybackRequest(
            key = "episode:${current.sourceId}:${episode.id}",
            kind = VodProgressKind.EPISODE,
            sourceId = current.sourceId,
            itemId = episode.id,
            seriesId = seriesId,
            title = episode.title,
            posterUrl = episode.posterUrl ?: current.posterUrl,
            containerExtension = episode.containerExtension,
            season = episode.season,
            episode = episode.episodeNumber,
            streamUrl = client.episodeStreamUrl(episode)
        )
    }
}

private enum class VodSortOption {
    DEFAULT,
    RATING,
    YEAR,
    ALPHABETICAL
}

@Composable
private fun ContinueWatchingRow(
    entries: List<VodProgressEntry>,
    onClick: (VodProgressEntry) -> Unit,
    onRemove: (VodProgressEntry) -> Unit
) {
    if (entries.isEmpty()) return
    val strings = LocalAppStrings.current
    val featured = entries.first()
    val remaining = entries.drop(1)

    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(strings.continueWatching, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))

        // Hero Spotlight Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onClick(featured) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!featured.posterUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = featured.posterUrl,
                            contentDescription = featured.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(60.dp)
                                .height(84.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Spacer(Modifier.width(14.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    if (featured.kind == VodProgressKind.MOVIE) strings.navMovies else strings.navSeries,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            val pct = (featured.progressFraction * 100).toInt()
                            Text(
                                "$pct% ${strings.watchedProgress.lowercase()}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            featured.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { featured.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                FilledTonalButton(
                    onClick = { onClick(featured) },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.resume, fontWeight = FontWeight.Bold)
                }
                IconButton(
                    onClick = { onRemove(featured) },
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .tvFocusHighlight(
                            shape = CircleShape,
                            scaleOnFocus = 1.15f
                        )
                ) {
                    Icon(Icons.Default.Close, contentDescription = strings.removeFromContinueWatching)
                }
            }
        }

        if (remaining.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(remaining.take(15), key = { "continue:${it.key}" }) { entry ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.width(220.dp)
                    ) {
                        Column {
                            Box(Modifier.fillMaxWidth().height(4.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                Box(
                                    Modifier.fillMaxHeight().fillMaxWidth(entry.progressFraction)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    entry.title,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onClick(entry) }
                                        .padding(start = 12.dp, top = 10.dp, bottom = 10.dp)
                                )
                                IconButton(
                                    onClick = { onRemove(entry) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .tvFocusHighlight(shape = CircleShape, scaleOnFocus = 1.15f)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = strings.removeFromContinueWatching, modifier = Modifier.size(16.dp))
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
private fun XtreamMoviesScreen(
    catalogs: List<XtreamCatalog>,
    profiles: List<PlaylistProfile>,
    contentFocusRequester: FocusRequester,
    continueWatching: List<VodProgressEntry>,
    progressEntries: List<VodProgressEntry>,
    watchedItemIds: Set<String>,
    onContinueWatching: (VodProgressEntry) -> Unit,
    onRemoveContinueWatching: (VodProgressEntry) -> Unit,
    hideAdultContent: Boolean = false,
    gridState: LazyGridState,
    movieWatchlistIds: Set<String>,
    onToggleWatchlist: (String, XtreamMovie) -> Unit,
    onToggleMovieWatched: (String, XtreamMovie) -> Unit,
    onPlay: (XtreamCatalog, XtreamMovie) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var appliedQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(VodSortOption.DEFAULT) }
    var selectedCategory by remember { mutableStateOf<CatalogCategory?>(null) }
    var selectedMovie by remember { mutableStateOf<CatalogMovie?>(null) }
    var visibleMovies by remember { mutableStateOf<List<CatalogMovie>>(emptyList()) }
    var isCatalogLoading by remember { mutableStateOf(true) }
    val progressByKey = remember(progressEntries) { progressEntries.associateBy(VodProgressEntry::key) }
    val showSourceNames = catalogs.size > 1
    val categories = remember(catalogs, hideAdultContent) {
        catalogs.flatMap { catalog ->
            catalog.movieCategories
                .filter { !hideAdultContent || !isAdultContent(it.name, null) }
                .map { CatalogCategory(catalog.sourceId, catalog.sourceName, it) }
        }.distinctBy { it.sourceId to it.category.id }
    }
    LaunchedEffect(query) {
        if (query.isNotBlank()) delay(250)
        appliedQuery = query.trim()
    }
    LaunchedEffect(catalogs, appliedQuery, selectedCategory, hideAdultContent, sortOption) {
        isCatalogLoading = true
        visibleMovies = withContext(Dispatchers.Default) {
            val filtered = catalogs.flatMap { catalog ->
                val categoryNames = catalog.movieCategories.associate { it.id to it.name }
                catalog.movies.asSequence()
                    .filter {
                        selectedCategory == null ||
                            (catalog.sourceId == selectedCategory?.sourceId && it.categoryId == selectedCategory?.category?.id)
                    }
                    .filter { !hideAdultContent || !isAdultContent(it.name, categoryNames[it.categoryId]) }
                    .filter { appliedQuery.isBlank() || it.name.contains(appliedQuery, ignoreCase = true) }
                    .map { CatalogMovie(catalog, it) }
                    .toList()
            }
            when (sortOption) {
                VodSortOption.DEFAULT -> filtered
                VodSortOption.RATING -> filtered.sortedByDescending { it.movie.rating?.toDoubleOrNull() ?: 0.0 }
                VodSortOption.YEAR -> filtered.sortedByDescending { it.movie.year?.toIntOrNull() ?: 0 }
                VodSortOption.ALPHABETICAL -> filtered.sortedBy { it.movie.name.lowercase() }
            }
        }
        isCatalogLoading = false
    }
    LaunchedEffect(appliedQuery, selectedCategory, sortOption) {
        if (gridState.firstVisibleItemIndex > 0) gridState.scrollToItem(0)
    }
    LibraryCatalogScaffold(
        title = strings.navMovies,
        query = query,
        onQueryChanged = { query = it },
        searchPlaceholder = strings.searchMovies,
        categories = categories,
        showCategorySourceNames = showSourceNames,
        selectedCategory = selectedCategory,
        onSelectCategory = { selectedCategory = it },
        sortOption = sortOption,
        onSelectSortOption = { sortOption = it },
        emptyMessage = strings.noMoviesAvailable,
        itemCount = visibleMovies.size,
        isContentLoading = isCatalogLoading,
        gridState = gridState,
        gridTestTag = "movies_grid",
        header = {
            ContinueWatchingRow(continueWatching, onContinueWatching, onRemoveContinueWatching)
        }
    ) {
        gridItemsIndexed(
            visibleMovies,
            key = { _, entry -> "${entry.catalog.sourceId}:${entry.movie.id}" },
            contentType = { _, _ -> "movie_poster" }
        ) { index, entry ->
            val libraryId = entry.movie.libraryId(entry.catalog.sourceId)
            VodPosterCard(
                title = entry.movie.name,
                posterUrl = entry.movie.posterUrl,
                subtitle = formatVodMetadata(entry.movie.year, entry.movie.rating),
                isWatched = libraryId in watchedItemIds,
                progressFraction = progressByKey[libraryId]?.progressFraction,
                modifier = if (index == 0) Modifier.focusRequester(contentFocusRequester) else Modifier,
                onClick = { selectedMovie = entry }
            )
        }
    }
    selectedMovie?.let { entry ->
        val source = profiles.firstOrNull { it.id == entry.catalog.sourceId }?.source as? ContentSource.Xtream
        val client = source?.let {
            remember(entry.catalog.sourceId, it) {
                XtreamClient(context.applicationContext, entry.catalog.sourceId, it)
            }
        }
        val libraryId = entry.movie.libraryId(entry.catalog.sourceId)
        MovieDetailsDialog(
            movie = entry.movie,
            client = client,
            sourceName = entry.catalog.sourceName.takeIf { showSourceNames },
            isWatchlisted = libraryId in movieWatchlistIds,
            isWatched = libraryId in watchedItemIds,
            onDismiss = { selectedMovie = null },
            onToggleWatchlist = { onToggleWatchlist(entry.catalog.sourceId, entry.movie) },
            onToggleWatched = { onToggleMovieWatched(entry.catalog.sourceId, entry.movie) },
            onWatch = {
                selectedMovie = null
                onPlay(entry.catalog, entry.movie)
            }
        )
    }
}

@Composable
private fun XtreamSeriesScreen(
    catalogs: List<XtreamCatalog>,
    profiles: List<PlaylistProfile>,
    contentFocusRequester: FocusRequester,
    hideAdultContent: Boolean = false,
    gridState: LazyGridState,
    followedSeriesIds: Set<String>,
    watchedItemIds: Set<String>,
    continueWatching: List<VodProgressEntry>,
    progressEntries: List<VodProgressEntry>,
    onContinueWatching: (VodProgressEntry) -> Unit,
    onRemoveContinueWatching: (VodProgressEntry) -> Unit,
    onToggleFollow: (String, XtreamSeries) -> Unit,
    onToggleEpisodeWatched: (String, XtreamEpisode) -> Unit,
    onPlay: (XtreamCatalog, XtreamSeries, XtreamEpisode, String) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var appliedQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(VodSortOption.DEFAULT) }
    var selectedCategory by remember { mutableStateOf<CatalogCategory?>(null) }
    var selected by remember { mutableStateOf<CatalogSeries?>(null) }
    var visibleSeries by remember { mutableStateOf<List<CatalogSeries>>(emptyList()) }
    var isCatalogLoading by remember { mutableStateOf(true) }
    val showSourceNames = catalogs.size > 1
    val categories = remember(catalogs, hideAdultContent) {
        catalogs.flatMap { catalog ->
            catalog.seriesCategories
                .filter { !hideAdultContent || !isAdultContent(it.name, null) }
                .map { CatalogCategory(catalog.sourceId, catalog.sourceName, it) }
        }.distinctBy { it.sourceId to it.category.id }
    }
    LaunchedEffect(query) {
        if (query.isNotBlank()) delay(250)
        appliedQuery = query.trim()
    }
    LaunchedEffect(catalogs, appliedQuery, selectedCategory, hideAdultContent, sortOption) {
        isCatalogLoading = true
        visibleSeries = withContext(Dispatchers.Default) {
            val filtered = catalogs.flatMap { catalog ->
                val categoryNames = catalog.seriesCategories.associate { it.id to it.name }
                catalog.series.asSequence()
                    .filter {
                        selectedCategory == null ||
                            (catalog.sourceId == selectedCategory?.sourceId && it.categoryId == selectedCategory?.category?.id)
                    }
                    .filter { !hideAdultContent || !isAdultContent(it.name, categoryNames[it.categoryId]) }
                    .filter { appliedQuery.isBlank() || it.name.contains(appliedQuery, ignoreCase = true) }
                    .map { CatalogSeries(catalog, it) }
                    .toList()
            }
            when (sortOption) {
                VodSortOption.DEFAULT -> filtered
                VodSortOption.RATING -> filtered.sortedByDescending { it.series.rating?.toDoubleOrNull() ?: 0.0 }
                VodSortOption.YEAR -> filtered.sortedByDescending { it.series.year?.toIntOrNull() ?: 0 }
                VodSortOption.ALPHABETICAL -> filtered.sortedBy { it.series.name.lowercase() }
            }
        }
        isCatalogLoading = false
    }
    LaunchedEffect(appliedQuery, selectedCategory, sortOption) {
        if (gridState.firstVisibleItemIndex > 0) gridState.scrollToItem(0)
    }
    LibraryCatalogScaffold(
        title = strings.navSeries,
        query = query,
        onQueryChanged = { query = it },
        searchPlaceholder = strings.searchSeries,
        categories = categories,
        showCategorySourceNames = showSourceNames,
        selectedCategory = selectedCategory,
        onSelectCategory = { selectedCategory = it },
        sortOption = sortOption,
        onSelectSortOption = { sortOption = it },
        emptyMessage = strings.noSeriesAvailable,
        itemCount = visibleSeries.size,
        isContentLoading = isCatalogLoading,
        gridState = gridState,
        gridTestTag = "series_grid",
        header = {
            ContinueWatchingRow(continueWatching, onContinueWatching, onRemoveContinueWatching)
        }
    ) {
        gridItemsIndexed(
            visibleSeries,
            key = { _, entry -> "${entry.catalog.sourceId}:${entry.series.id}" },
            contentType = { _, _ -> "series_poster" }
        ) { index, entry ->
            VodPosterCard(
                title = entry.series.name,
                posterUrl = entry.series.coverUrl,
                subtitle = listOfNotNull(entry.series.year, entry.series.rating).joinToString(" · "),
                modifier = if (index == 0) Modifier.focusRequester(contentFocusRequester) else Modifier,
                onClick = { selected = entry }
            )
        }
    }

    selected?.let { entry ->
        val profile = profiles.firstOrNull { it.id == entry.catalog.sourceId }
        val source = profile?.source as? ContentSource.Xtream
        if (source != null) {
            SeriesEpisodesDialog(
                series = entry.series,
                sourceId = entry.catalog.sourceId,
                client = remember(entry.catalog.sourceId, source) {
                    XtreamClient(context.applicationContext, entry.catalog.sourceId, source)
                },
                isFollowed = entry.series.libraryId(entry.catalog.sourceId) in followedSeriesIds,
                watchedItemIds = watchedItemIds,
                progressEntries = progressEntries,
                onToggleFollow = { onToggleFollow(entry.catalog.sourceId, entry.series) },
                onToggleEpisodeWatched = { onToggleEpisodeWatched(entry.catalog.sourceId, it) },
                onDismiss = { selected = null },
                onPlay = { episode, url -> onPlay(entry.catalog, entry.series, episode, url) }
            )
        }
    }
}

@Composable
private fun LibraryCatalogScaffold(
    title: String,
    query: String,
    onQueryChanged: (String) -> Unit,
    searchPlaceholder: String = LocalAppStrings.current.searchChannels,
    categories: List<CatalogCategory> = emptyList(),
    showCategorySourceNames: Boolean = false,
    selectedCategory: CatalogCategory? = null,
    onSelectCategory: (CatalogCategory?) -> Unit = {},
    sortOption: VodSortOption = VodSortOption.DEFAULT,
    onSelectSortOption: (VodSortOption) -> Unit = {},
    emptyMessage: String,
    itemCount: Int,
    isContentLoading: Boolean = false,
    gridState: LazyGridState,
    gridTestTag: String,
    header: @Composable () -> Unit = {},
    content: androidx.compose.foundation.lazy.grid.LazyGridScope.() -> Unit
) {
    val strings = LocalAppStrings.current
    val isTv = LocalFormFactor.current.isTelevision
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var isSidebarExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(query.isNotEmpty()) }
    var categorySearchQuery by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    val categoryButtonFocusRequester = remember { FocusRequester() }
    val firstCategoryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchExpanded) {
        if (isSearchExpanded) {
            searchFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(isSidebarExpanded) {
        if (isTv && isSidebarExpanded) {
            delay(100)
            runCatching { firstCategoryFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(isTv, categories.isNotEmpty()) {
        if (isTv && categories.isNotEmpty() && !isSidebarExpanded) {
            delay(100)
            runCatching { categoryButtonFocusRequester.requestFocus() }
        }
    }

    BackHandler(isSearchExpanded) {
        onQueryChanged("")
        isSearchExpanded = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    BackHandler(isSidebarExpanded) {
        isSidebarExpanded = false
        if (isTv) runCatching { categoryButtonFocusRequester.requestFocus() }
    }

    val filteredCategories = remember(categories, categorySearchQuery) {
        if (categorySearchQuery.isBlank()) categories
        else categories.filter {
            it.category.name.contains(categorySearchQuery, ignoreCase = true) ||
                (showCategorySourceNames && it.sourceName.contains(categorySearchQuery, ignoreCase = true))
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (!isTv) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                } else Modifier
            ),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(Modifier.fillMaxSize()) {
            // 1. Collapsible Categories Sidebar Drawer
            AnimatedVisibility(
                visible = isSidebarExpanded,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            strings.categories,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = {
                                isSidebarExpanded = false
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .tvFocusHighlight(
                                    shape = CircleShape,
                                    scaleOnFocus = if (isTv) 1.12f else 1f
                                )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MenuOpen, contentDescription = strings.hideChannelFilters)
                        }
                    }

                    if (categories.size > 8) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = categorySearchQuery,
                            onValueChange = { categorySearchQuery = it },
                            placeholder = { Text(strings.search, fontSize = 13.sp) },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            trailingIcon = if (categorySearchQuery.isNotEmpty()) {
                                {
                                    IconButton(
                                        onClick = { categorySearchQuery = "" },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                }
                            } else null,
                            shape = RoundedCornerShape(10.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .tvFocusHighlight(
                                    shape = RoundedCornerShape(8.dp),
                                    scaleOnFocus = if (isTv) 1.03f else 1f
                                )
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item(key = "all_categories_sidebar") {
                            GroupItem(
                                name = strings.allCategories,
                                count = itemCount,
                                selected = selectedCategory == null,
                                modifier = Modifier.focusRequester(firstCategoryFocusRequester),
                                onClick = {
                                    onSelectCategory(null)
                                    isSidebarExpanded = false
                                    if (isTv) runCatching { categoryButtonFocusRequester.requestFocus() }
                                    else focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                            )
                        }
                        items(filteredCategories, key = { "${it.sourceId}:${it.category.id}" }) { cat ->
                            GroupItem(
                                name = if (showCategorySourceNames) "${cat.sourceName} · ${cat.category.name}" else cat.category.name,
                                count = null,
                                selected = selectedCategory == cat,
                                onClick = {
                                    onSelectCategory(cat)
                                    isSidebarExpanded = false
                                    if (isTv) runCatching { categoryButtonFocusRequester.requestFocus() }
                                    else focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                            )
                        }
                    }
                }
            }

            // 2. Main Content Area (Top Bar + Grid)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Compact Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSearchExpanded || query.isNotEmpty()) {
                        // Expandable search bar with distinct back and clear actions.
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        onQueryChanged("")
                                        isSearchExpanded = false
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = strings.cancel,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (query.isEmpty()) {
                                        Text(
                                            searchPlaceholder,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = query,
                                        onValueChange = onQueryChanged,
                                        singleLine = true,
                                        textStyle = LocalTextStyle.current.copy(
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(
                                            onSearch = {
                                                focusManager.clearFocus()
                                                keyboardController?.hide()
                                            }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(searchFocusRequester)
                                    )
                                }
                                if (query.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onQueryChanged("") },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = strings.clearSearch,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        // Sort Icon Button (accessible next to search)
                        Box {
                            Surface(
                                onClick = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    sortMenuExpanded = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (sortOption != VodSortOption.DEFAULT) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (sortOption != VodSortOption.DEFAULT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Tune,
                                        contentDescription = strings.sortDefault,
                                        tint = if (sortOption != VodSortOption.DEFAULT) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(strings.sortDefault, fontSize = 12.sp) },
                                    onClick = {
                                        onSelectSortOption(VodSortOption.DEFAULT)
                                        sortMenuExpanded = false
                                    },
                                    trailingIcon = if (sortOption == VodSortOption.DEFAULT) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.sortByRating, fontSize = 12.sp) },
                                    onClick = {
                                        onSelectSortOption(VodSortOption.RATING)
                                        sortMenuExpanded = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                    trailingIcon = if (sortOption == VodSortOption.RATING) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.sortByYear, fontSize = 12.sp) },
                                    onClick = {
                                        onSelectSortOption(VodSortOption.YEAR)
                                        sortMenuExpanded = false
                                    },
                                    trailingIcon = if (sortOption == VodSortOption.YEAR) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.sortAlphabetical, fontSize = 12.sp) },
                                    onClick = {
                                        onSelectSortOption(VodSortOption.ALPHABETICAL)
                                        sortMenuExpanded = false
                                    },
                                    trailingIcon = if (sortOption == VodSortOption.ALPHABETICAL) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                            }
                        }
                    } else {
                        // Collapsed Header: Category filter + Title + Count + Category chip + (Sort Icon & Search Icon on right)
                        if (categories.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    isSidebarExpanded = !isSidebarExpanded
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .focusRequester(categoryButtonFocusRequester)
                                    .tvFocusHighlight(
                                        shape = CircleShape,
                                        scaleOnFocus = if (isTv) 1.12f else 1f
                                    )
                            ) {
                                Icon(
                                    if (isSidebarExpanded) Icons.AutoMirrored.Filled.MenuOpen else Icons.Default.FilterList,
                                    contentDescription = strings.categories,
                                    tint = if (selectedCategory != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                        }

                        Text(
                            title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = itemCount.toString(),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        if (selectedCategory != null) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                onClick = { onSelectCategory(null) },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        selectedCategory.category.name,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = strings.allCategories,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        // Sort Icon Button
                        Box {
                            Surface(
                                onClick = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    sortMenuExpanded = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (sortOption != VodSortOption.DEFAULT) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (sortOption != VodSortOption.DEFAULT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .size(40.dp)
                                    .tvFocusHighlight(
                                        shape = RoundedCornerShape(10.dp),
                                        scaleOnFocus = if (isTv) 1.08f else 1.0f
                                    )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Tune,
                                        contentDescription = strings.sortDefault,
                                        tint = if (sortOption != VodSortOption.DEFAULT) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(strings.sortDefault, fontSize = 12.sp) },
                                    onClick = {
                                        onSelectSortOption(VodSortOption.DEFAULT)
                                        sortMenuExpanded = false
                                    },
                                    trailingIcon = if (sortOption == VodSortOption.DEFAULT) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.sortByRating, fontSize = 12.sp) },
                                    onClick = {
                                        onSelectSortOption(VodSortOption.RATING)
                                        sortMenuExpanded = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                    trailingIcon = if (sortOption == VodSortOption.RATING) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.sortByYear, fontSize = 12.sp) },
                                    onClick = {
                                        onSelectSortOption(VodSortOption.YEAR)
                                        sortMenuExpanded = false
                                    },
                                    trailingIcon = if (sortOption == VodSortOption.YEAR) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.sortAlphabetical, fontSize = 12.sp) },
                                    onClick = {
                                        onSelectSortOption(VodSortOption.ALPHABETICAL)
                                        sortMenuExpanded = false
                                    },
                                    trailingIcon = if (sortOption == VodSortOption.ALPHABETICAL) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        // Search Icon Button
                        Surface(
                            onClick = {
                                isSearchExpanded = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .size(40.dp)
                                .tvFocusHighlight(
                                    shape = RoundedCornerShape(10.dp),
                                    scaleOnFocus = if (isTv) 1.08f else 1.0f
                                )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = searchPlaceholder,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                header()

                if (isContentLoading && itemCount == 0) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (itemCount == 0) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(if (isTv) 120.dp else 150.dp),
                        state = gridState,
                        contentPadding = PaddingValues(
                            top = if (isTv) 14.dp else 6.dp,
                            bottom = if (isTv) 14.dp else 6.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(if (isTv) 8.dp else 12.dp),
                        verticalArrangement = Arrangement.spacedBy(if (isTv) 8.dp else 12.dp),
                        modifier = Modifier.fillMaxSize().testTag(gridTestTag),
                        content = content
                    )
                }
            }
        }
    }
}

@Composable
private fun VodPosterCard(
    title: String,
    posterUrl: String?,
    subtitle: String,
    isWatched: Boolean = false,
    progressFraction: Float? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val posterRequest = remember(posterUrl) {
        posterUrl?.takeIf(String::isNotBlank)?.let { url ->
            ImageRequest.Builder(context)
                // Cards are 150dp wide. This is sharp enough for tablet grids without
                // decoding provider artwork at its often multi-megapixel source size.
                .data(url)
                .size(360, 540)
                .precision(Precision.INEXACT)
                .allowHardware(true)
                .build()
        }
    }
    val isTv = LocalFormFactor.current.isTelevision
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        modifier = modifier.tvFocusHighlight(
            shape = RoundedCornerShape(16.dp),
            scaleOnFocus = if (isTv) 1.06f else 1.0f
        )
    ) {
        Column {
            Box(
                Modifier.fillMaxWidth().aspectRatio(2f / 3f).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (posterRequest != null) {
                    AsyncImage(
                        model = posterRequest,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.Low,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
                }
                if (isWatched) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp).align(Alignment.TopEnd)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = strings.watched,
                            modifier = Modifier.padding(4.dp).size(18.dp)
                        )
                    }
                }
                if (progressFraction != null && progressFraction > 0f && !isWatched) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier.fillMaxWidth().height(4.dp).align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            Column(Modifier.padding(11.dp)) {
                Text(title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

internal fun formatVodMetadata(year: String?, rating: String?): String {
    val formattedRating = rating
        ?.trim()
        ?.substringBefore('/')
        ?.replace(',', '.')
        ?.toDoubleOrNull()
        ?.takeIf { it.isFinite() }
        ?.let { value -> "★ ${String.format(Locale.ROOT, "%.1f", value)}/10" }
    return listOfNotNull(year?.trim()?.takeIf(String::isNotEmpty), formattedRating).joinToString("  ")
}

@Composable
private fun MovieDetailsDialog(
    movie: XtreamMovie,
    client: XtreamClient?,
    sourceName: String?,
    isWatchlisted: Boolean,
    isWatched: Boolean,
    onDismiss: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onToggleWatched: () -> Unit,
    onWatch: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    var details by remember(movie.id, client) { mutableStateOf<XtreamMovieDetails?>(null) }
    var detailsError by remember(movie.id, client) { mutableStateOf(false) }
    var retryKey by remember(movie.id, client) { mutableIntStateOf(0) }
    LaunchedEffect(movie.id, client, retryKey) {
        if (client == null) return@LaunchedEffect
        detailsError = false
        runCatching { client.loadMovieDetails(movie.id) }
            .onSuccess { details = it }
            .onFailure { detailsError = true }
    }
    val effectivePoster = details?.posterUrl ?: movie.posterUrl
    val posterRequest = remember(effectivePoster) {
        effectivePoster?.takeIf(String::isNotBlank)?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .size(600, 900)
                .precision(Precision.INEXACT)
                .allowHardware(true)
                .build()
        }
    }

    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val useVerticalLayout = isCompact && isPortrait
    val isTv = LocalFormFactor.current.isTelevision
    val watchButtonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (isTv) {
            delay(100)
            try { watchButtonFocusRequester.requestFocus() } catch (_: Throwable) {}
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth(if (isCompact) 0.94f else 0.84f)
                .fillMaxHeight(if (isCompact) 0.90f else 0.82f)
        ) {
            Box(Modifier.fillMaxSize()) {
                if (useVerticalLayout) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Header with poster thumbnail and title
                        Row(
                            Modifier.fillMaxWidth().padding(end = 36.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(76.dp, 114.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (posterRequest != null) {
                                        AsyncImage(
                                            model = posterRequest,
                                            contentDescription = movie.name,
                                            contentScale = ContentScale.Crop,
                                            filterQuality = FilterQuality.Low,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    movie.name,
                                    fontSize = 20.sp,
                                    lineHeight = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val metadata = formatVodMetadata(
                                    details?.releaseDate?.take(4) ?: movie.year,
                                    details?.rating ?: movie.rating
                                )
                                if (metadata.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        metadata,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                sourceName?.let {
                                    Spacer(Modifier.height(3.dp))
                                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Scrollable plot & metadata
                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (client != null && details == null && !detailsError) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(strings.loadingMovieDetails, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                            }
                            if (detailsError) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(strings.movieDetailsUnavailable, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                    Spacer(Modifier.width(6.dp))
                                    TextButton(onClick = { retryKey++ }) { Text(strings.retry, fontSize = 12.sp) }
                                }
                            }
                            val plot = details?.plot ?: movie.plot
                            if (!plot.isNullOrBlank()) {
                                Text(
                                    plot,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                            details?.let { info ->
                                MovieMetadataLine(strings.genre, info.genre)
                                MovieMetadataLine(strings.duration, info.duration)
                                MovieMetadataLine(strings.releaseDate, info.releaseDate)
                                MovieMetadataLine(strings.director, info.director)
                                MovieMetadataLine(strings.cast, info.cast)
                                MovieMetadataLine(strings.country, info.country)
                                MovieMetadataLine(strings.ageRating, info.ageRating)
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Actions for phones: Primary Watch Now button full width + secondary row
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onWatch,
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(strings.watchNow, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onToggleWatchlist,
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        if (isWatchlisted) Icons.Default.Star else Icons.Outlined.StarOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        if (isWatchlisted) strings.removeFromWatchlist else strings.addToWatchlist,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 12.sp
                                    )
                                }
                                OutlinedButton(
                                    onClick = onToggleWatched,
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        if (isWatched) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isWatched) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        if (isWatched) strings.markUnwatched else strings.markWatched,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        Modifier.fillMaxSize().padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxHeight().aspectRatio(2f / 3f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (posterRequest != null) {
                                    AsyncImage(
                                        model = posterRequest,
                                        contentDescription = movie.name,
                                        contentScale = ContentScale.Crop,
                                        filterQuality = FilterQuality.Low,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(64.dp)
                                    )
                                }
                            }
                        }
                        Column(Modifier.weight(1f).fillMaxHeight()) {
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    movie.name,
                                    fontSize = 30.sp,
                                    lineHeight = 36.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                val metadata = formatVodMetadata(
                                    details?.releaseDate?.take(4) ?: movie.year,
                                    details?.rating ?: movie.rating
                                )
                                if (metadata.isNotBlank()) {
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        metadata,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                sourceName?.let {
                                    Spacer(Modifier.height(6.dp))
                                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                                if (client != null && details == null && !detailsError) {
                                    Spacer(Modifier.height(18.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(10.dp))
                                        Text(strings.loadingMovieDetails, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (detailsError) {
                                    Spacer(Modifier.height(14.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(strings.movieDetailsUnavailable, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.width(8.dp))
                                        TextButton(onClick = { retryKey++ }) { Text(strings.retry) }
                                    }
                                }
                                val plot = details?.plot ?: movie.plot
                                if (!plot.isNullOrBlank()) {
                                    Spacer(Modifier.height(22.dp))
                                    Text(
                                        plot,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 16.sp,
                                        lineHeight = 24.sp
                                    )
                                }
                                details?.let { info ->
                                    Spacer(Modifier.height(18.dp))
                                    MovieMetadataLine(strings.genre, info.genre)
                                    MovieMetadataLine(strings.duration, info.duration)
                                    MovieMetadataLine(strings.releaseDate, info.releaseDate)
                                    MovieMetadataLine(strings.director, info.director)
                                    MovieMetadataLine(strings.cast, info.cast)
                                    MovieMetadataLine(strings.country, info.country)
                                    MovieMetadataLine(strings.ageRating, info.ageRating)
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onToggleWatchlist,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                        .tvFocusHighlight(
                                            shape = ButtonDefaults.outlinedShape,
                                            scaleOnFocus = if (isTv) 1.04f else 1.0f
                                        )
                                ) {
                                    Icon(
                                        if (isWatchlisted) Icons.Default.Star else Icons.Outlined.StarOutline,
                                        contentDescription = null
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (isWatchlisted) strings.removeFromWatchlist else strings.addToWatchlist,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                OutlinedButton(
                                    onClick = onToggleWatched,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                        .tvFocusHighlight(
                                            shape = ButtonDefaults.outlinedShape,
                                            scaleOnFocus = if (isTv) 1.04f else 1.0f
                                        )
                                ) {
                                    Icon(
                                        if (isWatched) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isWatched) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (isWatched) strings.markUnwatched else strings.markWatched,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Button(
                                    onClick = onWatch,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                        .focusRequester(watchButtonFocusRequester)
                                        .tvFocusHighlight(
                                            shape = ButtonDefaults.shape,
                                            scaleOnFocus = if (isTv) 1.05f else 1.0f
                                        )
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(strings.watchNow, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                FilledIconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(if (isCompact) 8.dp else 14.dp)
                        .tvFocusHighlight(
                            shape = CircleShape,
                            scaleOnFocus = if (isTv) 1.15f else 1.0f
                        ),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = strings.close, modifier = Modifier.size(if (isCompact) 18.dp else 22.dp))
                }
            }
        }
    }
}

@Composable
private fun MovieMetadataLine(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            "$label:",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.widthIn(min = 100.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(value, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SeriesEpisodesDialog(
    series: XtreamSeries,
    sourceId: String,
    client: XtreamClient,
    isFollowed: Boolean,
    watchedItemIds: Set<String>,
    progressEntries: List<VodProgressEntry>,
    onToggleFollow: () -> Unit,
    onToggleEpisodeWatched: (XtreamEpisode) -> Unit,
    onDismiss: () -> Unit,
    onPlay: (XtreamEpisode, String) -> Unit
) {
    val strings = LocalAppStrings.current
    val isTv = LocalFormFactor.current.isTelevision
    val playNextFocusRequester = remember { FocusRequester() }
    val seriesFollowFocusRequester = remember { FocusRequester() }
    val seriesCloseFocusRequester = remember { FocusRequester() }
    val firstSeasonFocusRequester = remember { FocusRequester() }
    val firstEpisodeFocusRequester = remember { FocusRequester() }
    val seriesRetryFocusRequester = remember { FocusRequester() }
    var focusEpisodeAfterSeasonChange by remember { mutableStateOf(false) }
    var details by remember(series.id) { mutableStateOf<XtreamSeriesDetails?>(null) }
    var loadError by remember(series.id) { mutableStateOf(false) }
    var retryKey by remember(series.id) { mutableIntStateOf(0) }
    LaunchedEffect(series.id, retryKey) {
        loadError = false
        runCatching { client.loadSeriesDetails(series.id) }
            .onSuccess { details = it }
            .onFailure { loadError = true }
    }
    val episodes = details?.episodes
    val sortedEpisodes = remember(episodes) {
        episodes?.sortedWith(compareBy({ it.season }, { it.episodeNumber })) ?: emptyList()
    }
    val nextUnwatchedEpisode = remember(sortedEpisodes, watchedItemIds, sourceId) {
        sortedEpisodes.firstOrNull { ep ->
            ep.libraryId(sourceId) !in watchedItemIds
        }
    }
    LaunchedEffect(details, nextUnwatchedEpisode) {
        if (details != null && isTv) {
            delay(100)
            runCatching {
                if (nextUnwatchedEpisode != null) playNextFocusRequester.requestFocus()
                else seriesFollowFocusRequester.requestFocus()
            }
        }
    }

    LaunchedEffect(loadError) {
        if (isTv && loadError) {
            delay(100)
            runCatching { seriesRetryFocusRequester.requestFocus() }
        }
    }
    val seasons = remember(episodes) {
        episodes?.map { it.season }?.distinct()?.sorted() ?: emptyList()
    }
    var selectedSeason by remember(seasons) {
        mutableStateOf(seasons.firstOrNull())
    }
    val filteredEpisodes = remember(episodes, selectedSeason) {
        val list = episodes ?: return@remember emptyList()
        if (selectedSeason != null && seasons.size > 1) {
            list.filter { it.season == selectedSeason }
        } else {
            list
        }
    }
    LaunchedEffect(selectedSeason, focusEpisodeAfterSeasonChange) {
        if (isTv && focusEpisodeAfterSeasonChange) {
            focusEpisodeAfterSeasonChange = false
            delay(100)
            runCatching { firstEpisodeFocusRequester.requestFocus() }
        }
    }

    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600

    BackHandler(onBack = onDismiss)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(if (isCompact) 0.94f else 0.86f)
                .fillMaxHeight(if (isCompact) 0.90f else 0.86f)
        ) {
            Column(Modifier.fillMaxSize().padding(if (isCompact) 14.dp else 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val effectiveCover = details?.coverUrl ?: series.coverUrl
                    if (!effectiveCover.isNullOrBlank()) {
                        AsyncImage(
                            model = effectiveCover,
                            contentDescription = series.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(if (isCompact) 54.dp else 62.dp, if (isCompact) 80.dp else 92.dp).clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(Modifier.width(if (isCompact) 10.dp else 14.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(series.name, fontSize = if (isCompact) 18.sp else 24.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        val meta = formatVodMetadata(
                            details?.releaseDate?.take(4) ?: series.year,
                            details?.rating ?: series.rating
                        )
                        if (meta.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(meta, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = if (isCompact) 12.sp else 13.sp)
                        }
                        val plot = details?.plot ?: series.plot
                        if (!plot.isNullOrBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text(plot, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        details?.let { info ->
                            Spacer(Modifier.height(4.dp))
                            val highlights = listOfNotNull(info.genre, info.duration, info.ageRating)
                            if (highlights.isNotEmpty()) {
                                Text(
                                    highlights.joinToString("  ·  "),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (isCompact) {
                        IconButton(
                            onClick = onToggleFollow,
                            modifier = Modifier
                                .focusRequester(seriesFollowFocusRequester)
                                .tvFocusHighlight(
                                    shape = CircleShape,
                                    scaleOnFocus = if (isTv) 1.12f else 1f
                                )
                        ) {
                            Icon(
                                if (isFollowed) Icons.Default.Star else Icons.Outlined.StarOutline,
                                contentDescription = if (isFollowed) strings.unfollowSeries else strings.followSeries,
                                tint = if (isFollowed) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = onToggleFollow,
                            modifier = Modifier
                                .focusRequester(seriesFollowFocusRequester)
                                .tvFocusHighlight(
                                    shape = ButtonDefaults.outlinedShape,
                                    scaleOnFocus = if (isTv) 1.05f else 1f
                                )
                        ) {
                            Icon(
                                if (isFollowed) Icons.Default.Star else Icons.Outlined.StarOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (isFollowed) strings.unfollowSeries else strings.followSeries)
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .focusRequester(seriesCloseFocusRequester)
                            .tvFocusHighlight(
                                shape = CircleShape,
                                scaleOnFocus = if (isTv) 1.12f else 1f
                            )
                    ) { Icon(Icons.Default.Close, contentDescription = strings.close) }
                }
                if (sortedEpisodes.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (nextUnwatchedEpisode != null) {
                            Button(
                                onClick = { onPlay(nextUnwatchedEpisode, client.episodeStreamUrl(nextUnwatchedEpisode)) },
                                modifier = Modifier
                                    .height(40.dp)
                                    .focusRequester(playNextFocusRequester)
                                    .tvFocusHighlight(
                                        shape = ButtonDefaults.shape,
                                        scaleOnFocus = if (isTv) 1.05f else 1.0f
                                    )
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    strings.playNextEpisode(nextUnwatchedEpisode.season, nextUnwatchedEpisode.episodeNumber),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(strings.allEpisodesWatched, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                TextButton(onClick = {
                                    val first = sortedEpisodes.firstOrNull()
                                    if (first != null) onPlay(first, client.episodeStreamUrl(first))
                                }) {
                                    Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.restartSeries, fontSize = 12.sp)
                                }
                            }
                        }
                        val unwatchedCount = sortedEpisodes.count { it.libraryId(sourceId) !in watchedItemIds }
                        if (unwatchedCount > 0) {
                            Text(
                                strings.unwatchedEpisodesCount(unwatchedCount),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                if (seasons.size > 1) {
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(seasons, key = { _, season -> season }) { index, season ->
                            FilterChip(
                                selected = (selectedSeason ?: seasons.firstOrNull()) == season,
                                onClick = {
                                    selectedSeason = season
                                    focusEpisodeAfterSeasonChange = true
                                },
                                label = { Text(strings.seasonLabel(season)) },
                                modifier = (if (index == 0) Modifier.focusRequester(firstSeasonFocusRequester) else Modifier)
                                    .tvFocusHighlight(
                                        shape = FilterChipDefaults.shape,
                                        scaleOnFocus = if (isTv) 1.06f else 1f
                                    )
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                when {
                    loadError -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(strings.seriesDetailsUnavailable, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { retryKey++ },
                                modifier = Modifier
                                    .focusRequester(seriesRetryFocusRequester)
                                    .tvFocusHighlight(
                                        shape = ButtonDefaults.shape,
                                        scaleOnFocus = if (isTv) 1.05f else 1f
                                    )
                            ) { Text(strings.retry) }
                        }
                    }
                    details == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                        Text(strings.loadingSeriesDetails, modifier = Modifier.padding(top = 72.dp))
                    }
                    episodes!!.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(strings.noEpisodesAvailable)
                    }
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(filteredEpisodes, key = { _, episode -> "${episode.season}:${episode.id}" }) { index, episode ->
                            val epLibraryId = episode.libraryId(sourceId)
                            val isEpisodeWatched = epLibraryId in watchedItemIds
                            val epProgress = progressEntries.firstOrNull { it.key == epLibraryId }?.progressFraction
                            Surface(
                                onClick = { onPlay(episode, client.episodeStreamUrl(episode)) },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (index == 0) Modifier.focusRequester(firstEpisodeFocusRequester) else Modifier)
                                    .onKeyEvent { keyEvent ->
                                        if (isTv && keyEvent.type == KeyEventType.KeyDown &&
                                            (keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_MENU ||
                                             keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_STAR)
                                        ) {
                                            onToggleEpisodeWatched(episode)
                                            true
                                        } else false
                                    }
                                    .tvFocusHighlight(
                                        shape = RoundedCornerShape(12.dp),
                                        scaleOnFocus = if (isTv) 1.02f else 1.0f
                                    )
                            ) {
                                Column {
                                    Row(
                                        Modifier.fillMaxWidth().padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            strings.episodeLabel(episode.season, episode.episodeNumber),
                                            color = if (isEpisodeWatched) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.width(14.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(episode.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            if (!episode.plot.isNullOrBlank()) {
                                                Spacer(Modifier.height(2.dp))
                                                Text(episode.plot, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        IconButton(
                                            onClick = { onToggleEpisodeWatched(episode) },
                                            enabled = !isTv,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .focusable(!isTv)
                                        ) {
                                            Icon(
                                                imageVector = if (isEpisodeWatched) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                                                contentDescription = if (isEpisodeWatched) strings.markUnwatched else strings.markWatched,
                                                tint = if (isEpisodeWatched) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(4.dp))
                                        Icon(Icons.Default.PlayArrow, contentDescription = strings.watchNow)
                                    }
                                    if (epProgress != null && epProgress > 0f && !isEpisodeWatched) {
                                        LinearProgressIndicator(
                                            progress = { epProgress },
                                            modifier = Modifier.fillMaxWidth().height(3.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
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
}

private fun formatPlaybackTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

private const val LIVE_STALL_TIMEOUT_MILLIS = 12_000L
private const val LIVE_STALL_MIN_PROGRESS_MILLIS = 1_000L
private const val LIVE_STALL_MAX_RECOVERY_ATTEMPTS = 2

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VodPlayerDialog(
    request: VodPlaybackRequest,
    initialPositionMs: Long,
    preferredAudioLanguage: String = "default",
    preferredSubtitleLanguage: String = "off",
    subtitleFallbackEnglish: Boolean = true,
    loadNextEpisode: (suspend () -> VodPlaybackRequest?)?,
    loadAllEpisodes: (suspend () -> List<VodPlaybackRequest>)? = null,
    onPlayNext: (VodPlaybackRequest) -> Unit,
    onSaveProgress: (Long, Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    val renderersFactory = remember(context) {
        DefaultRenderersFactory(context).setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
    }
    val player = remember(request.streamUrl) { ExoPlayer.Builder(context, renderersFactory).build() }
    var resumePositionMs by remember(request.key) { mutableLongStateOf(initialPositionMs) }
    var resumeDecisionMade by remember(request.key) { mutableStateOf(initialPositionMs < 10_000L) }
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var nextEpisode by remember(request.key) { mutableStateOf<VodPlaybackRequest?>(null) }
    var loadingNextEpisode by remember(request.key) { mutableStateOf(false) }

    var controlsVisible by remember { mutableStateOf(true) }
    var autoPlayNext by remember { mutableStateOf(true) }
    var autoPlayCountdown by remember(request.key) { mutableIntStateOf(-1) }

    var currentPositionMs by remember(request.key) { mutableLongStateOf(0L) }
    var totalDurationMs by remember(request.key) { mutableLongStateOf(0L) }
    var isPlaying by remember(request.key) { mutableStateOf(true) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderDragPositionMs by remember { mutableFloatStateOf(0f) }

    var sleepTimerMinutes by remember { mutableStateOf<Int?>(null) }
    var sleepTimerEndEpochMs by remember { mutableStateOf<Long?>(null) }
    var sleepTimerRemainingText by remember { mutableStateOf<String?>(null) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    var currentTracks by remember { mutableStateOf<Tracks?>(null) }
    var userManuallySelectedTracks by remember(request.key) { mutableStateOf(false) }
    var showTracksDialog by remember { mutableStateOf(false) }
    var selectedTrackTab by remember { mutableIntStateOf(0) }

    var allEpisodes by remember(request.seriesId) { mutableStateOf<List<VodPlaybackRequest>>(emptyList()) }
    var showEpisodesDrawer by remember { mutableStateOf(false) }
    var selectedDrawerSeason by remember(request.season) { mutableIntStateOf(request.season ?: 1) }

    LaunchedEffect(player) {
        while (true) {
            if (!isDraggingSlider) {
                currentPositionMs = player.currentPosition.coerceAtLeast(0L)
                totalDurationMs = player.duration.coerceAtLeast(0L)
                isPlaying = player.isPlaying
            }
            delay(250)
        }
    }

    LaunchedEffect(currentTracks, preferredAudioLanguage, preferredSubtitleLanguage, subtitleFallbackEnglish) {
        if (!userManuallySelectedTracks && currentTracks != null) {
            val audioList = TrackLanguageMatcher.extractAudioTracks(currentTracks)
            val subtitleList = TrackLanguageMatcher.extractSubtitleTracks(currentTracks)

            var builder = player.trackSelectionParameters.buildUpon()

            // 1. Preferred Audio
            if (preferredAudioLanguage != "default") {
                val targetAudio = TrackLanguageMatcher.findBestAudioTrack(audioList, preferredAudioLanguage)
                if (targetAudio != null && !targetAudio.isSelected) {
                    builder = builder.setOverrideForType(
                        TrackSelectionOverride(targetAudio.group.mediaTrackGroup, targetAudio.trackIndex)
                    )
                }
            }

            // 2. Preferred Subtitles
            if (preferredSubtitleLanguage == "off") {
                builder = builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            } else {
                val targetSub = TrackLanguageMatcher.findBestSubtitleTrack(
                    subtitleList,
                    preferredSubtitleLanguage,
                    subtitleFallbackEnglish
                )
                if (targetSub != null) {
                    builder = builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .setOverrideForType(
                            TrackSelectionOverride(targetSub.group.mediaTrackGroup, targetSub.trackIndex)
                        )
                } else {
                    builder = builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                }
            }

            player.trackSelectionParameters = builder.build()
        }
    }

    LaunchedEffect(request.seriesId, loadAllEpisodes) {
        if (loadAllEpisodes != null && allEpisodes.isEmpty()) {
            val eps = runCatching { loadAllEpisodes() }.getOrDefault(emptyList())
            allEpisodes = eps
            if (eps.isNotEmpty() && request.season == null) {
                selectedDrawerSeason = eps.firstNotNullOfOrNull { it.season } ?: 1
            }
        }
    }

    val isTv = LocalFormFactor.current.isTelevision
    val playerFocusRequester = remember { FocusRequester() }
    val resumeFocusRequester = remember { FocusRequester() }
    val vodBackFocusRequester = remember { FocusRequester() }
    val vodEpisodesFocusRequester = remember { FocusRequester() }
    val vodTracksFocusRequester = remember { FocusRequester() }
    val vodAutoPlayFocusRequester = remember { FocusRequester() }
    val vodSleepFocusRequester = remember { FocusRequester() }
    val vodRewindFocusRequester = remember { FocusRequester() }
    val vodPlayFocusRequester = remember { FocusRequester() }
    val vodForwardFocusRequester = remember { FocusRequester() }
    val vodTimelineFocusRequester = remember { FocusRequester() }
    val vodDrawerCloseFocusRequester = remember { FocusRequester() }
    var lastInteractionEpoch by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        if (isTv) {
            delay(100)
            try { playerFocusRequester.requestFocus() } catch (_: Throwable) {}
        }
    }

    LaunchedEffect(resumeDecisionMade) {
        if (!resumeDecisionMade && isTv) {
            delay(100)
            try { resumeFocusRequester.requestFocus() } catch (_: Throwable) {}
        }
    }

    val isOverlayActive = showEpisodesDrawer || showTracksDialog || showSleepTimerDialog || controlsVisible

    fun dismissVodOverlay() {
        when {
            showEpisodesDrawer -> showEpisodesDrawer = false
            showTracksDialog -> showTracksDialog = false
            showSleepTimerDialog -> showSleepTimerDialog = false
            controlsVisible -> controlsVisible = false
        }
    }

    BackHandler(enabled = isOverlayActive) {
        dismissVodOverlay()
    }

    LaunchedEffect(controlsVisible, showEpisodesDrawer, showTracksDialog, showSleepTimerDialog, resumeDecisionMade) {
        if (isTv && controlsVisible && !showEpisodesDrawer && !showTracksDialog && !showSleepTimerDialog && resumeDecisionMade) {
            delay(100)
            if (runCatching { vodPlayFocusRequester.requestFocus() }.isFailure) {
                delay(100)
                runCatching { vodPlayFocusRequester.requestFocus() }
            }
        } else if (isTv && !isOverlayActive && resumeDecisionMade) {
            runCatching { playerFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(showEpisodesDrawer) {
        if (isTv && showEpisodesDrawer) {
            delay(100)
            runCatching { vodDrawerCloseFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(controlsVisible, showEpisodesDrawer, showSleepTimerDialog, showTracksDialog, lastInteractionEpoch) {
        if (controlsVisible && !showEpisodesDrawer && !showSleepTimerDialog && !showTracksDialog) {
            delay(if (isTv) 4_000 else 3_000)
            controlsVisible = false
        }
    }

    val audioTracks = remember(currentTracks) { extractAudioTracks(currentTracks) }
    val subtitleTracks = remember(currentTracks) { extractSubtitleTracks(currentTracks) }

    LaunchedEffect(sleepTimerEndEpochMs, sleepTimerMinutes) {
        val targetEpoch = sleepTimerEndEpochMs
        if (targetEpoch != null) {
            while (true) {
                val remainingMs = targetEpoch - System.currentTimeMillis()
                if (remainingMs <= 0L) {
                    Toast.makeText(context, strings.sleepTimerExpired, Toast.LENGTH_SHORT).show()
                    player.pause()
                    onDismiss()
                    break
                } else {
                    val mins = ((remainingMs / 1000L) / 60L).toInt() + 1
                    sleepTimerRemainingText = "${mins}m"
                }
                delay(1000)
            }
        } else {
            sleepTimerRemainingText = null
        }
    }

    LaunchedEffect(autoPlayCountdown, nextEpisode) {
        if (autoPlayCountdown > 0) {
            delay(1000)
            autoPlayCountdown--
            if (autoPlayCountdown == 0) {
                nextEpisode?.let { next ->
                    onPlayNext(next)
                }
            }
        }
    }

    val mainActivity = context as? MainActivity
    DisposableEffect(player) {
        val pictureInPictureDismissedListener = {
            player.stop()
            onDismiss()
        }
        mainActivity?.setOnPictureInPictureDismissedListener(pictureInPictureDismissedListener)
        mainActivity?.updatePictureInPictureState(
            enabled = true,
            fullscreenPlaying = true
        )
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                if (state == Player.STATE_ENDED) {
                    onSaveProgress(player.duration.coerceAtLeast(0L), player.duration.coerceAtLeast(0L))
                    if (sleepTimerMinutes == -1) {
                        Toast.makeText(context, strings.sleepTimerExpired, Toast.LENGTH_SHORT).show()
                        player.pause()
                        onDismiss()
                        return
                    }
                    if (loadNextEpisode != null && !loadingNextEpisode && nextEpisode == null) {
                        loadingNextEpisode = true
                        scope.launch {
                            val next = runCatching { loadNextEpisode() }.getOrNull()
                            loadingNextEpisode = false
                            nextEpisode = next
                            if (next != null && autoPlayNext) {
                                autoPlayCountdown = 5
                            }
                        }
                    }
                }
            }
            override fun onTracksChanged(tracks: Tracks) {
                currentTracks = tracks
            }
            override fun onPlayerError(error: PlaybackException) { playbackError = error.message ?: strings.playbackError }
        }
        player.addListener(listener)
        onDispose {
            mainActivity?.updatePictureInPictureState(enabled = false, fullscreenPlaying = false)
            mainActivity?.setOnPictureInPictureDismissedListener(null)
            onSaveProgress(player.currentPosition.coerceAtLeast(0L), player.duration.coerceAtLeast(0L))
            player.removeListener(listener)
            player.release()
        }
    }
    LaunchedEffect(player, resumeDecisionMade) {
        if (!resumeDecisionMade) return@LaunchedEffect
        player.setMediaItem(MediaItem.fromUri(request.streamUrl))
        player.prepare()
        if (resumePositionMs > 0L) player.seekTo(resumePositionMs)
        player.play()
        while (true) {
            delay(10_000)
            onSaveProgress(player.currentPosition.coerceAtLeast(0L), player.duration.coerceAtLeast(0L))
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        ImmersiveSystemBars()
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(playerFocusRequester)
                .focusable()
                .onPreviewKeyEvent { keyEvent ->
                    if (isTv && isOverlayActive && keyEvent.type == KeyEventType.KeyDown) {
                        lastInteractionEpoch = System.currentTimeMillis()
                    }
                    val isBackKey = keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_BACK ||
                        keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ESCAPE
                    if (isTv && isOverlayActive && isBackKey) {
                        lastInteractionEpoch = System.currentTimeMillis()
                        if (keyEvent.type == KeyEventType.KeyUp) dismissVodOverlay()
                        true
                    } else {
                        false
                    }
                }
                .onKeyEvent { keyEvent ->
                    val action = if (isOverlayActive) {
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            android.view.KeyEvent.KEYCODE_MEDIA_PLAY -> TvPlayerAction.PLAY
                            android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> TvPlayerAction.PAUSE
                            android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                            android.view.KeyEvent.KEYCODE_HEADSETHOOK -> TvPlayerAction.TOGGLE_PLAY_PAUSE
                            android.view.KeyEvent.KEYCODE_MEDIA_STOP -> TvPlayerAction.STOP
                            else -> null
                        }.takeIf { keyEvent.type == KeyEventType.KeyDown }
                    } else {
                        TvPlayerKeyDispatcher.dispatch(
                            keyCode = keyEvent.nativeKeyEvent.keyCode,
                            isActionDown = keyEvent.type == KeyEventType.KeyDown,
                            isLiveStream = false
                        )
                    }
                    if (action != null) {
                        lastInteractionEpoch = System.currentTimeMillis()
                        when (action) {
                            TvPlayerAction.TOGGLE_CONTROLS -> {
                                controlsVisible = !controlsVisible
                                true
                            }
                            TvPlayerAction.SEEK_BACKWARD -> {
                                val newPos = (player.currentPosition - 10_000L).coerceAtLeast(0L)
                                player.seekTo(newPos)
                                controlsVisible = true
                                true
                            }
                            TvPlayerAction.SEEK_FORWARD -> {
                                val newPos = (player.currentPosition + 10_000L).coerceAtMost(player.duration.coerceAtLeast(0L))
                                player.seekTo(newPos)
                                controlsVisible = true
                                true
                            }
                            TvPlayerAction.PLAY -> {
                                player.play()
                                true
                            }
                            TvPlayerAction.PAUSE -> {
                                player.pause()
                                true
                            }
                            TvPlayerAction.TOGGLE_PLAY_PAUSE -> {
                                if (player.isPlaying) player.pause() else player.play()
                                true
                            }
                            TvPlayerAction.STOP -> {
                                player.stop()
                                onDismiss()
                                true
                            }
                            else -> false
                        }
                    } else false
                }
        ) {
            VideoSurface(
                player = player,
                playbackState = playbackState,
                playbackError = playbackError,
                fullscreen = true,
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                onFullscreen = onDismiss,
                onToggleControls = { controlsVisible = !controlsVisible },
                onRetry = { playbackError = null; player.prepare(); player.play() },
                showTrackSettings = false,
                modifier = Modifier.fillMaxSize()
            )

            AnimatedVisibility(
                visible = controlsVisible && !showEpisodesDrawer,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(Modifier.fillMaxSize()) {
                    val configuration = LocalConfiguration.current
                    val isCompact = configuration.screenWidthDp < 600

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                                )
                            )
                            .padding(horizontal = if (isCompact) 12.dp else 16.dp, vertical = if (isCompact) 10.dp else 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = onDismiss,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.7f),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .size(if (isCompact) 36.dp else 40.dp)
                                .tvFullscreenControl(
                                    enabled = isTv,
                                    requester = vodBackFocusRequester,
                                    shape = CircleShape,
                                    scaleOnFocus = 1.15f,
                                    rightTarget = if (request.kind == VodProgressKind.EPISODE || loadAllEpisodes != null) vodEpisodesFocusRequester else vodTracksFocusRequester,
                                    downTarget = vodRewindFocusRequester
                                )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back, modifier = Modifier.size(if (isCompact) 18.dp else 20.dp))
                        }
                        Spacer(Modifier.width(if (isCompact) 8.dp else 12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                request.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isCompact) 14.sp else 17.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (request.kind == VodProgressKind.EPISODE && (request.season != null || request.episode != null)) {
                                Text(
                                    "S${(request.season ?: 1).toString().padStart(2, '0')} E${(request.episode ?: 1).toString().padStart(2, '0')}",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = if (isCompact) 10.sp else 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (request.kind == VodProgressKind.EPISODE || loadAllEpisodes != null) {
                            Surface(
                                onClick = { showEpisodesDrawer = true },
                                shape = if (isCompact) CircleShape else RoundedCornerShape(20.dp),
                                color = if (showEpisodesDrawer) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, if (showEpisodesDrawer) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f)),
                                modifier = Modifier.tvFullscreenControl(
                                    enabled = isTv,
                                    requester = vodEpisodesFocusRequester,
                                    shape = if (isCompact) CircleShape else RoundedCornerShape(20.dp),
                                    scaleOnFocus = 1.08f,
                                    leftTarget = vodBackFocusRequester,
                                    rightTarget = vodTracksFocusRequester,
                                    downTarget = vodPlayFocusRequester
                                )
                            ) {
                                Row(
                                    Modifier.padding(if (isCompact) PaddingValues(9.dp) else PaddingValues(horizontal = 10.dp, vertical = 6.dp)),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.VideoLibrary,
                                        contentDescription = strings.episodesDrawer,
                                        tint = if (showEpisodesDrawer) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    if (!isCompact) {
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            strings.episodesDrawer,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = if (showEpisodesDrawer) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(if (isCompact) 6.dp else 8.dp))
                        }

                        Surface(
                            onClick = { showTracksDialog = true },
                            shape = if (isCompact) CircleShape else RoundedCornerShape(20.dp),
                            color = if (subtitleTracks.any { it.isSelected }) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, if (subtitleTracks.any { it.isSelected }) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier.tvFullscreenControl(
                                enabled = isTv,
                                requester = vodTracksFocusRequester,
                                shape = if (isCompact) CircleShape else RoundedCornerShape(20.dp),
                                scaleOnFocus = 1.08f,
                                leftTarget = if (request.kind == VodProgressKind.EPISODE || loadAllEpisodes != null) vodEpisodesFocusRequester else vodBackFocusRequester,
                                rightTarget = if (loadNextEpisode != null) vodAutoPlayFocusRequester else vodSleepFocusRequester,
                                downTarget = vodPlayFocusRequester
                            )
                        ) {
                            Row(
                                Modifier.padding(if (isCompact) PaddingValues(9.dp) else PaddingValues(horizontal = 10.dp, vertical = 6.dp)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Subtitles,
                                    contentDescription = strings.audioAndSubtitles,
                                    tint = if (subtitleTracks.any { it.isSelected }) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                                if (!isCompact) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        strings.audioAndSubtitles,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (subtitleTracks.any { it.isSelected }) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(if (isCompact) 6.dp else 8.dp))

                        if (loadNextEpisode != null) {
                            Surface(
                                onClick = { autoPlayNext = !autoPlayNext },
                                shape = if (isCompact) CircleShape else RoundedCornerShape(20.dp),
                                color = if (autoPlayNext) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, if (autoPlayNext) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f)),
                                modifier = Modifier.tvFullscreenControl(
                                    enabled = isTv,
                                    requester = vodAutoPlayFocusRequester,
                                    shape = if (isCompact) CircleShape else RoundedCornerShape(20.dp),
                                    scaleOnFocus = 1.08f,
                                    leftTarget = vodTracksFocusRequester,
                                    rightTarget = vodSleepFocusRequester,
                                    downTarget = vodPlayFocusRequester
                                )
                            ) {
                                Row(
                                    Modifier.padding(if (isCompact) PaddingValues(9.dp) else PaddingValues(horizontal = 10.dp, vertical = 6.dp)),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.FastForward,
                                        contentDescription = strings.autoPlayNext,
                                        tint = if (autoPlayNext) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    if (!isCompact) {
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            strings.autoPlayNext,
                                            color = if (autoPlayNext) Color.White else Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.sp,
                                            fontWeight = if (autoPlayNext) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(if (isCompact) 6.dp else 8.dp))
                        }

                        Surface(
                            onClick = { showSleepTimerDialog = true },
                            shape = if (isCompact && sleepTimerRemainingText == null && sleepTimerMinutes != -1) CircleShape else RoundedCornerShape(20.dp),
                            color = if (sleepTimerMinutes != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, if (sleepTimerMinutes != null) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier.tvFullscreenControl(
                                enabled = isTv,
                                requester = vodSleepFocusRequester,
                                shape = if (isCompact && sleepTimerRemainingText == null && sleepTimerMinutes != -1) CircleShape else RoundedCornerShape(20.dp),
                                scaleOnFocus = 1.08f,
                                leftTarget = if (loadNextEpisode != null) vodAutoPlayFocusRequester else vodTracksFocusRequester,
                                downTarget = vodForwardFocusRequester
                            )
                        ) {
                            Row(
                                Modifier.padding(if (isCompact && sleepTimerRemainingText == null && sleepTimerMinutes != -1) PaddingValues(9.dp) else PaddingValues(horizontal = 10.dp, vertical = 6.dp)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Bedtime,
                                    contentDescription = strings.sleepTimer,
                                    tint = if (sleepTimerMinutes != null) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                                if (sleepTimerRemainingText != null) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        sleepTimerRemainingText.orEmpty(),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else if (sleepTimerMinutes == -1 && !isCompact) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        strings.endOfEpisode,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // CENTER PLAYBACK CONTROLS
                    Row(
                        Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = {
                                val target = (player.currentPosition - 10_000L).coerceAtLeast(0L)
                                player.seekTo(target)
                                currentPositionMs = target
                            },
                            modifier = Modifier
                                .size(54.dp)
                                .tvFullscreenControl(
                                    enabled = isTv,
                                    requester = vodRewindFocusRequester,
                                    shape = CircleShape,
                                    scaleOnFocus = 1.15f,
                                    rightTarget = vodPlayFocusRequester,
                                    upTarget = vodBackFocusRequester,
                                    downTarget = vodTimelineFocusRequester
                                ),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.65f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.FastRewind, contentDescription = "-10s", modifier = Modifier.size(28.dp))
                        }

                        FilledIconButton(
                            onClick = {
                                if (player.isPlaying) {
                                    player.pause()
                                    isPlaying = false
                                } else {
                                    player.play()
                                    isPlaying = true
                                }
                            },
                            enabled = playbackState != Player.STATE_BUFFERING,
                            modifier = Modifier
                                .size(72.dp)
                                .tvFullscreenControl(
                                    enabled = isTv,
                                    requester = vodPlayFocusRequester,
                                    shape = CircleShape,
                                    scaleOnFocus = 1.12f,
                                    leftTarget = vodRewindFocusRequester,
                                    rightTarget = vodForwardFocusRequester,
                                    upTarget = vodTracksFocusRequester,
                                    downTarget = vodTimelineFocusRequester
                                ),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (playbackState == Player.STATE_BUFFERING) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(34.dp)
                                )
                            } else {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        FilledIconButton(
                            onClick = {
                                val target = (player.currentPosition + 10_000L).coerceAtMost(player.duration.coerceAtLeast(0L))
                                player.seekTo(target)
                                currentPositionMs = target
                            },
                            modifier = Modifier
                                .size(54.dp)
                                .tvFullscreenControl(
                                    enabled = isTv,
                                    requester = vodForwardFocusRequester,
                                    shape = CircleShape,
                                    scaleOnFocus = 1.15f,
                                    leftTarget = vodPlayFocusRequester,
                                    upTarget = vodSleepFocusRequester,
                                    downTarget = vodTimelineFocusRequester
                                ),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.65f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.FastForward, contentDescription = "+10s", modifier = Modifier.size(28.dp))
                        }
                    }

                    // BOTTOM TIMELINE & PROGRESS BAR
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                            .padding(horizontal = 24.dp, vertical = 18.dp)
                    ) {
                        Slider(
                            value = if (isDraggingSlider) sliderDragPositionMs else currentPositionMs.toFloat().coerceIn(0f, totalDurationMs.coerceAtLeast(1L).toFloat()),
                            onValueChange = {
                                isDraggingSlider = true
                                sliderDragPositionMs = it
                            },
                            onValueChangeFinished = {
                                player.seekTo(sliderDragPositionMs.toLong())
                                currentPositionMs = sliderDragPositionMs.toLong()
                                isDraggingSlider = false
                            },
                            valueRange = 0f..(totalDurationMs.coerceAtLeast(1L).toFloat()),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.tvFullscreenControl(
                                enabled = isTv,
                                requester = vodTimelineFocusRequester,
                                shape = RoundedCornerShape(8.dp),
                                scaleOnFocus = 1.01f,
                                upTarget = vodPlayFocusRequester
                            )
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                formatPlaybackTime(if (isDraggingSlider) sliderDragPositionMs.toLong() else currentPositionMs),
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                formatPlaybackTime(totalDurationMs),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (!resumeDecisionMade) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.align(Alignment.Center).widthIn(max = 430.dp)
                ) {
                    Column(Modifier.padding(24.dp)) {
                        Text(strings.resumePlayback, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(strings.resumePlaybackDescription, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    resumePositionMs = 0L
                                    resumeDecisionMade = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .tvFocusHighlight(
                                        shape = ButtonDefaults.outlinedShape,
                                        scaleOnFocus = if (isTv) 1.05f else 1.0f
                                    )
                            ) { Text(strings.restartPlayback) }
                            Button(
                                onClick = { resumeDecisionMade = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(resumeFocusRequester)
                                    .tvFocusHighlight(
                                        shape = ButtonDefaults.shape,
                                        scaleOnFocus = if (isTv) 1.05f else 1.0f
                                    )
                            ) {
                                Text(strings.resume)
                            }
                        }
                    }
                }
            }

            nextEpisode?.let { next ->
                if (autoPlayCountdown > 0) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp).widthIn(max = 480.dp)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.FastForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        strings.nextEpisodeIn(autoPlayCountdown),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                next.title,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        autoPlayCountdown = -1
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(strings.cancel)
                                }
                                Button(
                                    onClick = {
                                        autoPlayCountdown = -1
                                        onPlayNext(next)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(strings.playNow)
                                }
                            }
                        }
                    }
                } else if (autoPlayCountdown <= 0) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.align(Alignment.Center).widthIn(max = 430.dp)
                    ) {
                        Column(Modifier.padding(24.dp)) {
                            Text(strings.nextEpisode, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(next.title, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(20.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(strings.close) }
                                Button(onClick = { onPlayNext(next) }, modifier = Modifier.weight(1f)) {
                                    Text(strings.playNextEpisode)
                                }
                            }
                        }
                    }
                }
            }

            BackHandler(enabled = showEpisodesDrawer) {
                showEpisodesDrawer = false
            }

            AnimatedVisibility(
                visible = showEpisodesDrawer,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showEpisodesDrawer = false
                        }
                )
            }

            AnimatedVisibility(
                visible = showEpisodesDrawer,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .widthIn(min = 320.dp, max = 400.dp)
                        .fillMaxHeight()
                ) {
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    strings.episodesDrawer,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { showEpisodesDrawer = false },
                                modifier = Modifier
                                    .focusRequester(vodDrawerCloseFocusRequester)
                                    .tvFocusHighlight(
                                        shape = CircleShape,
                                        scaleOnFocus = if (isTv) 1.12f else 1f
                                    )
                            ) {
                                Icon(Icons.Default.Close, contentDescription = strings.close)
                            }
                        }

                        val seasons = remember(allEpisodes) {
                            allEpisodes.mapNotNull { it.season }.distinct().sorted()
                        }
                        if (seasons.size > 1) {
                            Spacer(Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(seasons, key = { "drawer-season-$it" }) { s ->
                                    FilterChip(
                                        selected = selectedDrawerSeason == s,
                                        onClick = { selectedDrawerSeason = s },
                                        label = { Text(strings.seasonNumber(s)) },
                                        modifier = Modifier.tvFocusHighlight(
                                            shape = FilterChipDefaults.shape,
                                            scaleOnFocus = if (isTv) 1.06f else 1f
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))

                        val currentSeasonEpisodes = remember(allEpisodes, selectedDrawerSeason) {
                            allEpisodes.filter { (it.season ?: 1) == selectedDrawerSeason }
                        }

                        if (currentSeasonEpisodes.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(currentSeasonEpisodes, key = { it.key }) { ep ->
                                    val isPlaying = ep.itemId == request.itemId
                                    Surface(
                                        onClick = {
                                            if (!isPlaying) {
                                                showEpisodesDrawer = false
                                                onPlayNext(ep)
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isPlaying) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .tvFocusHighlight(
                                                shape = RoundedCornerShape(12.dp),
                                                scaleOnFocus = if (isTv) 1.04f else 1f
                                            )
                                    ) {
                                        Row(
                                            Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    "E${(ep.episode ?: 1).toString().padStart(2, '0')}",
                                                    color = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    ep.title,
                                                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                                                    fontSize = 14.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (isPlaying) {
                                                    Text(
                                                        strings.nowPlayingBadge,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
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
            }

            if (showTracksDialog) {
                PlaybackTracksDialog(
                    player = player,
                    audioTracks = audioTracks,
                    subtitleTracks = subtitleTracks,
                    onUserTrackSelected = { userManuallySelectedTracks = true },
                    onDismiss = { showTracksDialog = false }
                )
            }

            if (showSleepTimerDialog) {
                AlertDialog(
                    onDismissRequest = { showSleepTimerDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text(strings.sleepTimer, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val options = listOf(
                                null to strings.sleepTimerOff,
                                15 to "15 min",
                                30 to "30 min",
                                45 to "45 min",
                                60 to "60 min",
                                90 to "90 min",
                                120 to "120 min",
                                -1 to strings.endOfEpisode
                            )
                            options.forEach { (mins, label) ->
                                val isSelected = sleepTimerMinutes == mins
                                Surface(
                                    onClick = {
                                        sleepTimerMinutes = mins
                                        if (mins != null && mins > 0) {
                                            sleepTimerEndEpochMs = System.currentTimeMillis() + mins * 60_000L
                                            Toast.makeText(context, strings.sleepTimerSet(mins), Toast.LENGTH_SHORT).show()
                                        } else {
                                            sleepTimerEndEpochMs = null
                                            if (mins == -1) {
                                                Toast.makeText(context, strings.endOfEpisode, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        showSleepTimerDialog = false
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSleepTimerDialog = false }) {
                            Text(strings.close)
                        }
                    }
                )
            }
        }
    }
}

@UnstableApi
private fun extractAudioTracks(currentTracks: Tracks?): List<PlaybackTrackItem> =
    TrackLanguageMatcher.extractAudioTracks(currentTracks)

@UnstableApi
private fun extractSubtitleTracks(currentTracks: Tracks?): List<PlaybackTrackItem> =
    TrackLanguageMatcher.extractSubtitleTracks(currentTracks)

@UnstableApi
@Composable
private fun PlaybackTracksDialog(
    player: Player,
    audioTracks: List<PlaybackTrackItem>,
    subtitleTracks: List<PlaybackTrackItem>,
    onUserTrackSelected: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isTv = LocalFormFactor.current.isTelevision
    val audioFocusRequester = remember { FocusRequester() }
    val selectedAudio = audioTracks.firstOrNull { it.isSelected } ?: audioTracks.firstOrNull()
    val selectedSubtitle = subtitleTracks.firstOrNull { it.isSelected }

    LaunchedEffect(Unit) {
        if (isTv) {
            delay(100)
            runCatching { audioFocusRequester.requestFocus() }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Subtitles, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(strings.audioAndSubtitles, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PlaybackTrackDropdown(
                    title = strings.audioTracks,
                    selectedValue = selectedAudio?.let { localizedPlaybackTrackLabel(it, strings) }
                        ?: strings.noAudioTracksAvailable,
                    options = audioTracks.map { localizedPlaybackTrackLabel(it, strings) },
                    selectedIndex = audioTracks.indexOfFirst { it.isSelected }.coerceAtLeast(0),
                    enabled = audioTracks.isNotEmpty(),
                    modifier = Modifier.focusRequester(audioFocusRequester),
                    onSelected = { index ->
                        val track = audioTracks[index]
                        onUserTrackSelected()
                        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                            .setOverrideForType(TrackSelectionOverride(track.group.mediaTrackGroup, track.trackIndex))
                            .build()
                    }
                )
                PlaybackTrackDropdown(
                    title = strings.subtitles,
                    selectedValue = selectedSubtitle?.let { localizedPlaybackTrackLabel(it, strings) }
                        ?: strings.subtitlesOff,
                    options = listOf(strings.subtitlesOff) + subtitleTracks.map { localizedPlaybackTrackLabel(it, strings) },
                    selectedIndex = subtitleTracks.indexOfFirst { it.isSelected }.let { if (it < 0) 0 else it + 1 },
                    enabled = true,
                    onSelected = { index ->
                        onUserTrackSelected()
                        player.trackSelectionParameters = if (index == 0) {
                            player.trackSelectionParameters.buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                .build()
                        } else {
                            val track = subtitleTracks[index - 1]
                            player.trackSelectionParameters.buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                .setOverrideForType(TrackSelectionOverride(track.group.mediaTrackGroup, track.trackIndex))
                                .build()
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.close)
            }
        }
    )
}

@Composable
private fun PlaybackTrackDropdown(
    title: String,
    selectedValue: String,
    options: List<String>,
    selectedIndex: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onSelected: (Int) -> Unit
) {
    val isTv = LocalFormFactor.current.isTelevision
    var expanded by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        Surface(
            onClick = { expanded = true },
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .tvFocusHighlight(shape = RoundedCornerShape(12.dp), scaleOnFocus = if (isTv) 1.04f else 1f)
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$title:", fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(selectedValue, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 280.dp, max = 440.dp)
        ) {
            options.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelected(index)
                        expanded = false
                    },
                    trailingIcon = if (index == selectedIndex) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }
        }
    }
}

@UnstableApi
private fun localizedPlaybackTrackLabel(track: PlaybackTrackItem, strings: Strings): String = when {
    TrackLanguageMatcher.matchesLanguage(track.language, track.label, "pt") -> strings.langPortuguese
    TrackLanguageMatcher.matchesLanguage(track.language, track.label, "en") -> strings.langEnglish
    TrackLanguageMatcher.matchesLanguage(track.language, track.label, "es") -> strings.langSpanish
    TrackLanguageMatcher.matchesLanguage(track.language, track.label, "fr") -> strings.langFrench
    TrackLanguageMatcher.matchesLanguage(track.language, track.label, "de") -> strings.langGerman
    TrackLanguageMatcher.matchesLanguage(track.language, track.label, "it") -> strings.langItalian
    else -> track.label
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

private enum class FavoriteSection { CHANNELS, MOVIES, SERIES }

@Composable
private fun FavoriteLibraryScreen(
    channels: List<PlaylistChannel>,
    catalogs: List<XtreamCatalog>,
    profiles: List<PlaylistProfile>,
    contentFocusRequester: FocusRequester,
    epgGuide: EpgGuide?,
    epgTimeShiftHours: Int,
    favoriteChannelIds: Set<String>,
    movieWatchlistIds: Set<String>,
    followedSeriesIds: Set<String>,
    watchedItemIds: Set<String>,
    progressEntries: List<VodProgressEntry>,
    hideAdultContent: Boolean,
    onToggleFavorite: (PlaylistChannel) -> Unit,
    onToggleMovieWatchlist: (String, XtreamMovie) -> Unit,
    onToggleMovieWatched: (String, XtreamMovie) -> Unit,
    onToggleSeriesFollow: (String, XtreamSeries) -> Unit,
    onToggleEpisodeWatched: (String, XtreamEpisode) -> Unit,
    onPlayMovie: (XtreamCatalog, XtreamMovie) -> Unit,
    onPlayEpisode: (XtreamCatalog, XtreamSeries, XtreamEpisode, String) -> Unit,
    onWatch: (PlaylistChannel) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var section by remember {
        mutableStateOf(
            when {
                channels.isNotEmpty() -> FavoriteSection.CHANNELS
                movieWatchlistIds.isNotEmpty() -> FavoriteSection.MOVIES
                else -> FavoriteSection.SERIES
            }
        )
    }
    var selectedMovie by remember { mutableStateOf<CatalogMovie?>(null) }
    var selectedSeries by remember { mutableStateOf<CatalogSeries?>(null) }
    val movies = remember(catalogs, movieWatchlistIds, hideAdultContent) {
        catalogs.flatMap { catalog ->
            val categoryNames = catalog.movieCategories.associate { it.id to it.name }
            catalog.movies.filter {
                it.libraryId(catalog.sourceId) in movieWatchlistIds &&
                    (!hideAdultContent || !isAdultContent(it.name, categoryNames[it.categoryId]))
            }.map { CatalogMovie(catalog, it) }
        }
    }
    val series = remember(catalogs, followedSeriesIds, hideAdultContent) {
        catalogs.flatMap { catalog ->
            val categoryNames = catalog.seriesCategories.associate { it.id to it.name }
            catalog.series.filter {
                it.libraryId(catalog.sourceId) in followedSeriesIds &&
                    (!hideAdultContent || !isAdultContent(it.name, categoryNames[it.categoryId]))
            }.map { CatalogSeries(catalog, it) }
        }
    }
    val selectedSectionIsEmpty = when (section) {
        FavoriteSection.CHANNELS -> channels.isEmpty()
        FavoriteSection.MOVIES -> movies.isEmpty()
        FavoriteSection.SERIES -> series.isEmpty()
    }
    var scheduleNow by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(epgGuide) {
        while (true) {
            scheduleNow = Instant.now()
            delay(60_000)
        }
    }
    if (channels.isEmpty() && movies.isEmpty() && series.isEmpty()) {
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
                    Text(strings.noFavorites, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                }
            }
        }
    } else {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        strings.navFavorites,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("${channels.size + movies.size + series.size}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = section == FavoriteSection.CHANNELS,
                        onClick = { section = FavoriteSection.CHANNELS },
                        modifier = if (section == FavoriteSection.CHANNELS && selectedSectionIsEmpty) {
                            Modifier.focusRequester(contentFocusRequester)
                        } else Modifier,
                        label = { Text("${strings.favoriteChannels} (${channels.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = section == FavoriteSection.MOVIES,
                        onClick = { section = FavoriteSection.MOVIES },
                        modifier = if (section == FavoriteSection.MOVIES && selectedSectionIsEmpty) {
                            Modifier.focusRequester(contentFocusRequester)
                        } else Modifier,
                        label = { Text("${strings.movieWatchlist} (${movies.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = section == FavoriteSection.SERIES,
                        onClick = { section = FavoriteSection.SERIES },
                        modifier = if (section == FavoriteSection.SERIES && selectedSectionIsEmpty) {
                            Modifier.focusRequester(contentFocusRequester)
                        } else Modifier,
                        label = { Text("${strings.followedSeries} (${series.size})") }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            when (section) {
                FavoriteSection.CHANNELS -> if (channels.isEmpty()) {
                    FavoriteEmptyMessage(strings.noFavoriteChannels)
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 14.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(channels, key = { _, channel -> "favorite:${channel.sourceId}:${channel.sourceItemId}" }) { index, channel ->
                            GuideChannelRow(
                                channel = channel,
                                epgGuide = epgGuide,
                                scheduleNow = scheduleNow,
                                epgTimeShiftHours = epgTimeShiftHours,
                                selected = false,
                                isFavorite = channel.libraryId()?.let(favoriteChannelIds::contains) == true,
                                modifier = if (index == 0) Modifier.focusRequester(contentFocusRequester) else Modifier,
                                onToggleFavorite = { onToggleFavorite(channel) },
                                onClick = { onWatch(channel) }
                            )
                        }
                    }
                }
                FavoriteSection.MOVIES -> if (movies.isEmpty()) {
                    FavoriteEmptyMessage(strings.noWatchlistMovies)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(120.dp),
                        contentPadding = PaddingValues(top = 14.dp, bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        gridItemsIndexed(movies, key = { _, entry -> "watchlist:${entry.catalog.sourceId}:${entry.movie.id}" }) { index, entry ->
                            val libraryId = entry.movie.libraryId(entry.catalog.sourceId)
                            VodPosterCard(
                                title = entry.movie.name,
                                posterUrl = entry.movie.posterUrl,
                                subtitle = formatVodMetadata(entry.movie.year, entry.movie.rating),
                                isWatched = libraryId in watchedItemIds,
                                progressFraction = progressEntries.firstOrNull { it.key == libraryId }?.progressFraction,
                                modifier = if (index == 0) Modifier.focusRequester(contentFocusRequester) else Modifier,
                                onClick = { selectedMovie = entry }
                            )
                        }
                    }
                }
                FavoriteSection.SERIES -> if (series.isEmpty()) {
                    FavoriteEmptyMessage(strings.noFollowedSeries)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(120.dp),
                        contentPadding = PaddingValues(top = 14.dp, bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        gridItemsIndexed(series, key = { _, entry -> "followed:${entry.catalog.sourceId}:${entry.series.id}" }) { index, entry ->
                            VodPosterCard(
                                title = entry.series.name,
                                posterUrl = entry.series.coverUrl,
                                subtitle = formatVodMetadata(entry.series.year, entry.series.rating),
                                modifier = if (index == 0) Modifier.focusRequester(contentFocusRequester) else Modifier,
                                onClick = { selectedSeries = entry }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedMovie?.let { entry ->
        val source = profiles.firstOrNull { it.id == entry.catalog.sourceId }?.source as? ContentSource.Xtream
        val client = source?.let {
            remember(entry.catalog.sourceId, it) {
                XtreamClient(context.applicationContext, entry.catalog.sourceId, it)
            }
        }
        val libraryId = entry.movie.libraryId(entry.catalog.sourceId)
        MovieDetailsDialog(
            movie = entry.movie,
            client = client,
            sourceName = entry.catalog.sourceName.takeIf { catalogs.size > 1 },
            isWatchlisted = libraryId in movieWatchlistIds,
            isWatched = libraryId in watchedItemIds,
            onDismiss = { selectedMovie = null },
            onToggleWatchlist = { onToggleMovieWatchlist(entry.catalog.sourceId, entry.movie) },
            onToggleWatched = { onToggleMovieWatched(entry.catalog.sourceId, entry.movie) },
            onWatch = {
                selectedMovie = null
                onPlayMovie(entry.catalog, entry.movie)
            }
        )
    }
    selectedSeries?.let { entry ->
        val source = profiles.firstOrNull { it.id == entry.catalog.sourceId }?.source as? ContentSource.Xtream
        if (source != null) {
            SeriesEpisodesDialog(
                series = entry.series,
                sourceId = entry.catalog.sourceId,
                client = remember(entry.catalog.sourceId, source) {
                    XtreamClient(context.applicationContext, entry.catalog.sourceId, source)
                },
                isFollowed = entry.series.libraryId(entry.catalog.sourceId) in followedSeriesIds,
                watchedItemIds = watchedItemIds,
                progressEntries = progressEntries,
                onToggleFollow = { onToggleSeriesFollow(entry.catalog.sourceId, entry.series) },
                onToggleEpisodeWatched = { onToggleEpisodeWatched(entry.catalog.sourceId, it) },
                onDismiss = { selectedSeries = null },
                onPlay = { episode, url -> onPlayEpisode(entry.catalog, entry.series, episode, url) }
            )
        }
    }
}

@Composable
private fun FavoriteEmptyMessage(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
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
    epgLoadedDays: Int = 3,
    epgRepository: EpgRepository? = null,
    playChannelsFullscreen: Boolean,
    autoPictureInPicture: Boolean,
    loading: Boolean,
    error: String?,
    onRetryLoad: (() -> Unit)?,
    favoriteChannelIds: Set<String>,
    recentChannelIds: List<String> = emptyList(),
    onRecordChannelWatched: (PlaylistChannel) -> Unit = {},
    hiddenCategories: Set<String> = emptySet(),
    categoryOrder: List<String> = emptyList(),
    onToggleCategoryHidden: (String) -> Unit = {},
    onMoveCategory: (String, Boolean) -> Unit = { _, _ -> },
    onUnhideAllCategories: () -> Unit = {},
    onHideAllCategories: (List<String>) -> Unit = {},
    onResetCategoryOrder: () -> Unit = {},
    onToggleFavorite: (PlaylistChannel) -> Unit,
    requestedChannel: PlaylistChannel?,
    onRequestedChannelHandled: () -> Unit,
    onOpenSettings: (Int) -> Unit,
    contentFocusRequester: FocusRequester? = null,
    sidebarFocusRequester: FocusRequester? = null
) {
    val strings = LocalAppStrings.current
    val isTv = LocalFormFactor.current.isTelevision
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val channelPaneWidth = if (isCompact) 280.dp else 430.dp

    val filterButtonFocusRequester = remember { FocusRequester() }
    val calendarButtonFocusRequester = remember { FocusRequester() }
    val effectiveContentFocusRequester = contentFocusRequester ?: remember { FocusRequester() }
    val firstFilterFocusRequester = remember { FocusRequester() }

    val allFilter = "all"
    val recentFilter = "recent"
    val uncategorizedFilter = "group:"
    var isSidebarExpanded by remember { mutableStateOf(false) }
    var sidebarInteractionCount by remember { mutableLongStateOf(0L) }
    var showManageCategories by remember { mutableStateOf(false) }
    var focusFirstChannelAfterFilter by remember { mutableStateOf(false) }

    LaunchedEffect(isSidebarExpanded, sidebarInteractionCount) {
        if (isSidebarExpanded && !isTv) {
            delay(5_000)
            isSidebarExpanded = false
        }
    }

    LaunchedEffect(isSidebarExpanded) {
        if (isTv && isSidebarExpanded) {
            delay(100)
            runCatching { firstFilterFocusRequester.requestFocus() }
        } else if (isTv && focusFirstChannelAfterFilter) {
            focusFirstChannelAfterFilter = false
            delay(100)
            runCatching { effectiveContentFocusRequester.requestFocus() }
        }
    }

    BackHandler(enabled = isTv && isSidebarExpanded) {
        isSidebarExpanded = false
        runCatching { filterButtonFocusRequester.requestFocus() }
    }

    val groupCounts = remember(playlist) {
        playlist.channels.groupingBy { it.group.orEmpty() }.eachCount()
    }
    val rawCategories = remember(playlist) {
        groupCounts.keys.filter { it.isNotBlank() }
    }
    val orderedCategories = remember(rawCategories, categoryOrder, hiddenCategories) {
        val nonHidden = rawCategories.filter { it !in hiddenCategories }
        val orderMap = categoryOrder.withIndex().associate { it.value to it.index }
        nonHidden.sortedWith(compareBy({ orderMap[it] ?: Int.MAX_VALUE }, { it }))
    }
    val allVisibleChannels = remember(playlist.channels, hiddenCategories) {
        if (hiddenCategories.isEmpty()) playlist.channels
        else playlist.channels.filter { (it.group.orEmpty()) !in hiddenCategories }
    }

    var selectedFilter by remember(playlist, orderedCategories, hiddenCategories) {
        mutableStateOf(
            if (hiddenCategories.isNotEmpty() && orderedCategories.isNotEmpty()) {
                "group:${orderedCategories.first()}"
            } else {
                allFilter
            }
        )
    }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    fun hideKeyboard() {
        if (!isTv) {
            focusManager.clearFocus(force = true)
        }
        keyboardController?.hide()
        (view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    var searchQuery by remember(playlist) { mutableStateOf("") }
    var selectedChannel by remember(playlist) { mutableStateOf<PlaylistChannel?>(null) }
    var fullscreenTrigger by remember(playlist) { mutableIntStateOf(0) }
    var showProgrammeGuide by remember(playlist) { mutableStateOf(false) }
    fun selectFilter(filter: String) {
        selectedFilter = filter
        hideKeyboard()
        if (isTv) {
            isSidebarExpanded = false
            focusFirstChannelAfterFilter = true
        }
    }
    fun selectChannel(channel: PlaylistChannel) {
        hideKeyboard()
        if (selectedChannel == channel) {
            fullscreenTrigger++
        } else {
            selectedChannel = channel
        }
        isSidebarExpanded = false
        onRecordChannelWatched(channel)
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
    val sourceCounts = remember(allVisibleChannels) {
        allVisibleChannels.groupingBy { it.sourceId }.eachCount()
    }
    val recentChannels = remember(allVisibleChannels, recentChannelIds) {
        val channelMap = allVisibleChannels.associateBy { it.libraryId() }
        recentChannelIds.mapNotNull { channelMap[it] }
    }
    val selectedFilterLabel = when {
        selectedFilter == allFilter -> strings.allChannels
        selectedFilter == recentFilter -> strings.recentlyWatched
        selectedFilter.startsWith("source:") -> enabledSources
            .firstOrNull { it.id == selectedFilter.removePrefix("source:") }
            ?.name ?: strings.sources
        selectedFilter == uncategorizedFilter -> strings.uncategorized
        selectedFilter.startsWith("group:") -> selectedFilter.removePrefix("group:")
        else -> strings.allChannels
    }
    val channelsInGroup = remember(playlist, allVisibleChannels, selectedFilter, recentChannels) {
        when {
            selectedFilter == allFilter -> allVisibleChannels
            selectedFilter == recentFilter -> recentChannels
            selectedFilter.startsWith("source:") -> {
                val sourceId = selectedFilter.removePrefix("source:")
                allVisibleChannels.filter { it.sourceId == sourceId }
            }
            selectedFilter == uncategorizedFilter -> allVisibleChannels.filter { it.group.isNullOrBlank() }
            selectedFilter.startsWith("group:") -> {
                val group = selectedFilter.removePrefix("group:")
                playlist.channels.filter { it.group == group }
            }
            else -> allVisibleChannels
        }
    }
    val visibleChannels = remember(channelsInGroup, searchQuery) {
        channelsInGroup.searchChannels(searchQuery)
    }
    val channelListState = rememberLazyListState()

    if (showProgrammeGuide) {
        val initialCategory = when {
            selectedFilter.startsWith("group:") -> selectedFilter.removePrefix("group:")
            selectedFilter == recentFilter -> strings.navFavorites
            else -> strings.allChannels
        }
        ProgrammeGuideScreen(
            playlist = playlist,
            epgGuide = epgGuide,
            epgTimeShiftHours = epgTimeShiftHours,
            epgLoadedDays = epgLoadedDays,
            epgRepository = epgRepository,
            favoriteChannelIds = favoriteChannelIds,
            onToggleFavorite = onToggleFavorite,
            categoryOrder = categoryOrder,
            hiddenCategories = hiddenCategories,
            initialCategory = initialCategory,
            onBack = { showProgrammeGuide = false },
            onWatchChannel = { channel ->
                selectChannel(channel)
                showProgrammeGuide = false
            }
        )
        return
    }

    var matchedCount by remember(allVisibleChannels, epgGuide) { mutableIntStateOf(0) }
    var unmatchedChannels by remember(allVisibleChannels, epgGuide) {
        mutableStateOf<List<PlaylistChannel>>(emptyList())
    }
    LaunchedEffect(allVisibleChannels, epgGuide) {
        if (epgGuide == null) {
            matchedCount = 0
            unmatchedChannels = allVisibleChannels
        } else {
            val summary = withContext(Dispatchers.Default) {
                var matches = 0
                val unmatched = ArrayList<PlaylistChannel>()
                allVisibleChannels.forEach { channel ->
                    if (epgGuide.resolveChannelId(channel.tvgId, channel.name) != null) {
                        matches++
                    } else {
                        unmatched += channel
                    }
                }
                matches to unmatched
            }
            matchedCount = summary.first
            unmatchedChannels = summary.second
        }
    }
    var showUnmatchedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedFilter, searchQuery) {
        channelListState.scrollToItem(0)
    }

    LaunchedEffect(channelListState.isScrollInProgress) {
        if (!isTv && channelListState.isScrollInProgress) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (!isTv) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                } else Modifier
            ),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(Modifier.fillMaxSize()) {
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
                    Text(strings.appName, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = { isSidebarExpanded = false },
                        modifier = Modifier.tvFocusHighlight(
                            shape = CircleShape,
                            scaleOnFocus = if (isTv) 1.12f else 1f
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuOpen, contentDescription = strings.hideChannelFilters)
                    }
                }
                Spacer(Modifier.height(14.dp))

                Surface(
                    onClick = { onOpenSettings(1) },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvFocusHighlight(
                            shape = RoundedCornerShape(10.dp),
                            scaleOnFocus = if (isTv) 1.03f else 1f
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (activeProfile.source is ContentSource.LocalFile) Icons.Default.FolderOpen else Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (enabledSourceCount > 1) strings.sourcesCount(enabledSourceCount) else activeProfile.name,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (enabledSourceCount > 1) {
                                Text(
                                    activeProfile.name,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.categories, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    IconButton(
                        onClick = {
                            hideKeyboard()
                            showManageCategories = true
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .tvFocusHighlight(
                                shape = CircleShape,
                                scaleOnFocus = if (isTv) 1.12f else 1f
                            )
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = strings.manageCategories, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        GroupItem(
                            name = strings.allChannels,
                            count = allVisibleChannels.size,
                            selected = selectedFilter == allFilter,
                            modifier = Modifier.focusRequester(firstFilterFocusRequester),
                            onClick = { selectFilter(allFilter) }
                        )
                    }
                    if (recentChannels.isNotEmpty()) {
                        item {
                            GroupItem(
                                name = strings.recentlyWatched,
                                count = recentChannels.size,
                                selected = selectedFilter == recentFilter,
                                onClick = { selectFilter(recentFilter) }
                            )
                        }
                    }
                    if (enabledSourceCount > 1) {
                        item {
                            Spacer(Modifier.height(6.dp))
                            Text(strings.sources, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                        }
                        items(enabledSources, key = { "source-filter-${it.id}" }) { profile ->
                            GroupItem(
                                name = profile.name,
                                count = sourceCounts[profile.id] ?: 0,
                                selected = selectedFilter == "source:${profile.id}",
                                onClick = { selectFilter("source:${profile.id}") }
                            )
                        }
                    }
                    val uncategorizedCount = groupCounts[""] ?: 0
                    if (uncategorizedCount > 0) {
                        item {
                            GroupItem(
                                name = strings.uncategorized,
                                count = uncategorizedCount,
                                selected = selectedFilter == uncategorizedFilter,
                                onClick = { selectFilter(uncategorizedFilter) }
                            )
                        }
                    }
                    items(
                        orderedCategories,
                        key = { "group-filter-$it" }
                    ) { group ->
                        GroupItem(
                            name = group,
                            count = groupCounts[group] ?: 0,
                            selected = selectedFilter == "group:$group",
                            onClick = { selectFilter("group:$group") }
                        )
                    }
                }

                if (epgGuide == null || unmatchedChannels.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    if (epgGuide == null) {
                        Text(strings.epgNotConfigured, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    } else {
                        Surface(
                            onClick = { showUnmatchedDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(vertical = 6.dp, horizontal = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        strings.epgMatched(matchedCount, allVisibleChannels.size),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = strings.viewUnmatchedChannels,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    epgSourceName ?: strings.xmltvGuide,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 11.sp
                                )
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
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    if (onRetryLoad != null) {
                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = onRetryLoad,
                            enabled = !loading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .tvFocusHighlight(
                                    shape = ButtonDefaults.shape,
                                    scaleOnFocus = if (isTv) 1.03f else 1f
                                )
                        ) {
                            Text(strings.retry)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = if (isCompact) 12.dp else 24.dp, vertical = if (isCompact) 10.dp else 16.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { hideKeyboard() })
                }
        ) {
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
                        FilledTonalIconButton(
                            onClick = { isSidebarExpanded = true },
                            modifier = Modifier
                                .focusRequester(filterButtonFocusRequester)
                                .onPreviewKeyEvent { keyEvent ->
                                    if (isTv && (keyEvent.type == KeyEventType.KeyDown || keyEvent.type == KeyEventType.KeyUp)) {
                                        when (keyEvent.nativeKeyEvent.keyCode) {
                                            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                if (keyEvent.type == KeyEventType.KeyUp) {
                                                    try {
                                                        calendarButtonFocusRequester.requestFocus()
                                                    } catch (_: Throwable) {}
                                                }
                                                return@onPreviewKeyEvent true
                                            }
                                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                if (keyEvent.type == KeyEventType.KeyUp) {
                                                    try {
                                                        effectiveContentFocusRequester.requestFocus()
                                                    } catch (_: Throwable) {}
                                                }
                                                return@onPreviewKeyEvent true
                                            }
                                        }
                                    }
                                    false
                                }
                                .tvFocusHighlight(
                                    shape = CircleShape,
                                    scaleOnFocus = if (isTv) 1.15f else 1.0f
                                )
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = strings.showChannelFilters)
                        }
                    }
                    FilledTonalIconButton(
                        onClick = { showProgrammeGuide = true },
                        enabled = epgGuide != null,
                        modifier = Modifier
                            .testTag("programme_guide_button")
                            .focusRequester(calendarButtonFocusRequester)
                            .onPreviewKeyEvent { keyEvent ->
                                if (isTv && (keyEvent.type == KeyEventType.KeyDown || keyEvent.type == KeyEventType.KeyUp)) {
                                    when (keyEvent.nativeKeyEvent.keyCode) {
                                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                            if (keyEvent.type == KeyEventType.KeyUp) {
                                                try {
                                                    filterButtonFocusRequester.requestFocus()
                                                } catch (_: Throwable) {}
                                            }
                                            return@onPreviewKeyEvent true
                                        }
                                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                            if (keyEvent.type == KeyEventType.KeyUp) {
                                                try {
                                                    effectiveContentFocusRequester.requestFocus()
                                                } catch (_: Throwable) {}
                                            }
                                            return@onPreviewKeyEvent true
                                        }
                                    }
                                }
                                false
                            }
                            .tvFocusHighlight(
                                shape = CircleShape,
                                scaleOnFocus = if (isTv) 1.15f else 1.0f
                            )
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = strings.programmeGuide)
                    }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(strings.searchInGroup(selectedFilterLabel), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        ),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }) {
                                    Text("×", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        modifier = (when {
                            selectedChannel != null && !(isCompact && isPortrait) -> Modifier.weight(1f)
                            isCompact -> Modifier.weight(1f)
                            else -> Modifier.width(360.dp)
                        }).onPreviewKeyEvent { keyEvent ->
                            if (isTv && keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN) {
                                if (keyEvent.type == KeyEventType.KeyUp) {
                                    try {
                                        effectiveContentFocusRequester.requestFocus()
                                    } catch (_: Throwable) {}
                                }
                                true
                            } else false
                        }.tvFocusHighlight(
                            shape = OutlinedTextFieldDefaults.shape,
                            scaleOnFocus = if (isTv) 1.02f else 1.0f
                        )
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
            val movablePlayerPanel = remember {
                movableContentOf { currentChannel: PlaylistChannel, playerModifier: Modifier ->
                    PlayerPanel(
                        channel = currentChannel,
                        epgGuide = epgGuide,
                        epgTimeShiftHours = epgTimeShiftHours,
                        channels = playlist.channels,
                        groups = orderedCategories,
                        favoriteChannelIds = favoriteChannelIds,
                        recentChannelIds = recentChannelIds,
                        onToggleFavorite = onToggleFavorite,
                        autoEnterFullscreen = playChannelsFullscreen,
                        fullscreenTrigger = fullscreenTrigger,
                        autoPictureInPicture = autoPictureInPicture,
                        onChannelSelected = ::selectChannel,
                        modifier = playerModifier
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            if (isCompact && isPortrait) {
                if (selectedChannel != null) {
                    Column(Modifier.weight(1f).fillMaxWidth()) {
                        movablePlayerPanel(selectedChannel!!, Modifier.fillMaxWidth().wrapContentHeight())
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
                        itemsIndexed(
                            items = visibleChannels,
                            key = { _, it -> "${it.position}:${it.streamUrl}" },
                            contentType = { _, _ -> "channel" }
                        ) { index, channel ->
                            GuideChannelRow(
                                channel = channel,
                                epgGuide = epgGuide,
                                scheduleNow = scheduleNow,
                                epgTimeShiftHours = epgTimeShiftHours,
                                selected = selectedChannel == channel,
                                isFavorite = channel.libraryId()?.let(favoriteChannelIds::contains) == true,
                                modifier = (if (index == 0) Modifier.focusRequester(effectiveContentFocusRequester) else Modifier)
                                    .onPreviewKeyEvent { keyEvent ->
                                        if (isTv && (keyEvent.type == KeyEventType.KeyDown || keyEvent.type == KeyEventType.KeyUp) && keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP && index == 0) {
                                            if (keyEvent.type == KeyEventType.KeyUp) {
                                                try {
                                                    filterButtonFocusRequester.requestFocus()
                                                } catch (_: Throwable) {}
                                            }
                                            return@onPreviewKeyEvent true
                                        }
                                        false
                                    },
                                sidebarFocusRequester = filterButtonFocusRequester,
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
                    if (selectedChannel != null) {
                        Spacer(Modifier.width(16.dp))
                        movablePlayerPanel(selectedChannel!!, Modifier.weight(1f).fillMaxHeight())
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

        if (showManageCategories) {
            ManageCategoriesDialog(
                categories = rawCategories,
                groupCounts = groupCounts,
                hiddenCategories = hiddenCategories,
                categoryOrder = categoryOrder,
                onToggleCategoryHidden = onToggleCategoryHidden,
                onMoveCategory = onMoveCategory,
                onUnhideAll = onUnhideAllCategories,
                onHideAll = { onHideAllCategories(rawCategories) },
                onResetOrder = onResetCategoryOrder,
                onDismiss = { showManageCategories = false }
            )
        }
    }
}
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
private fun GroupItem(
    name: String,
    count: Int?,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isTv = LocalFormFactor.current.isTelevision
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .tvFocusHighlight(
                shape = RoundedCornerShape(10.dp),
                scaleOnFocus = if (isTv) 1.03f else 1.0f
            )
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
            if (count != null) {
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
}

@Composable
private fun GuideChannelRow(
    channel: PlaylistChannel,
    epgGuide: EpgGuide?,
    scheduleNow: Instant,
    epgTimeShiftHours: Int,
    selected: Boolean,
    isFavorite: Boolean = false,
    modifier: Modifier = Modifier,
    sidebarFocusRequester: FocusRequester? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val presentation = remember(channel.tvgId, channel.name, epgGuide, scheduleNow, epgTimeShiftHours, strings) {
        val current = epgGuide?.snapshot(channel.tvgId, channel.name, scheduleNow, epgTimeShiftHours)?.current
        val shiftMillis = epgTimeShiftHours * 3_600_000L
        val adjustedStart = current?.startEpochMilli?.plus(shiftMillis)
        val adjustedStop = current?.stopEpochMilli?.plus(shiftMillis)
        val progress = if (adjustedStart != null && adjustedStop != null && adjustedStop > adjustedStart) {
            ((scheduleNow.toEpochMilli() - adjustedStart).toFloat() /
                (adjustedStop - adjustedStart).toFloat()).coerceIn(0f, 1f)
        } else {
            null
        }
        val timeRange = if (adjustedStart != null && adjustedStop != null) {
            "${formatProgrammeTime(Instant.ofEpochMilli(adjustedStart))}–${formatProgrammeTime(Instant.ofEpochMilli(adjustedStop))}"
        } else if (adjustedStop != null) {
            strings.until(formatProgrammeTime(Instant.ofEpochMilli(adjustedStop)))
        } else {
            null
        }
        ChannelEpgPresentation(
            title = current?.title,
            timeRange = timeRange,
            progress = progress
        )
    }
    ChannelRow(
        channel = channel,
        epgChannel = epgGuide?.findChannel(channel.tvgId, channel.name),
        nowPlaying = presentation.title,
        timeRange = presentation.timeRange,
        progress = presentation.progress,
        selected = selected,
        isFavorite = isFavorite,
        modifier = modifier,
        sidebarFocusRequester = sidebarFocusRequester,
        onToggleFavorite = onToggleFavorite,
        onClick = onClick
    )
}

private data class ChannelEpgPresentation(
    val title: String?,
    val timeRange: String?,
    val progress: Float?
)

@Composable
private fun ChannelRow(
    channel: PlaylistChannel,
    epgChannel: com.fortresstv.player.epg.EpgChannel?,
    nowPlaying: String?,
    timeRange: String? = null,
    progress: Float? = null,
    selected: Boolean,
    isFavorite: Boolean = false,
    modifier: Modifier = Modifier,
    sidebarFocusRequester: FocusRequester? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isTv = LocalFormFactor.current.isTelevision

    var isFocused by remember { mutableStateOf(false) }

    val surfaceBorder = when {
        isFocused -> BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary)
        selected -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    }
    val surfaceColor = when {
        isFocused && selected -> MaterialTheme.colorScheme.primary.copy(alpha = .25f)
        isFocused -> MaterialTheme.colorScheme.surfaceVariant
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = .18f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = surfaceColor,
        border = surfaceBorder,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged {
                isFocused = it.isFocused
            }
            .onPreviewKeyEvent { keyEvent ->
                if (isTv) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_MENU,
                        android.view.KeyEvent.KEYCODE_STAR -> {
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                onToggleFavorite?.invoke()
                            }
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (keyEvent.type == KeyEventType.KeyUp) {
                                if (sidebarFocusRequester != null) {
                                    try {
                                        sidebarFocusRequester.requestFocus()
                                    } catch (_: Throwable) {}
                                }
                            }
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            ChannelLogo(channel, epgChannel, modifier = Modifier.size(44.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        channel.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (timeRange != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            timeRange,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (nowPlaying != null) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        nowPlaying,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (progress != null) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.5.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f)
                    )
                }
            }
            onToggleFavorite?.let { toggle ->
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = toggle,
                    enabled = !isTv,
                    modifier = Modifier
                        .size(36.dp)
                        .focusable(!isTv)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                        contentDescription = if (isFavorite) strings.removeFavorite else strings.addFavorite,
                        tint = if (isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
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
    favoriteChannelIds: Set<String> = emptySet(),
    recentChannelIds: List<String> = emptyList(),
    onToggleFavorite: (PlaylistChannel) -> Unit = {},
    autoEnterFullscreen: Boolean,
    fullscreenTrigger: Int = 0,
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
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var consecutiveStallRecoveries by remember(channel.streamUrl) { mutableIntStateOf(0) }
    var manualFullscreen by rememberSaveable { mutableStateOf(autoEnterFullscreen) }
    var landscapeFullscreenDismissed by rememberSaveable(isPortrait) { mutableStateOf(false) }
    val fullscreen = manualFullscreen || (isCompact && !isPortrait && !landscapeFullscreenDismissed)
    val inPictureInPicture = (activity as? MainActivity)?.pictureInPictureMode == true
    var previousWatchedChannel by remember { mutableStateOf<PlaylistChannel?>(null) }
    var currentTrackedChannel by remember { mutableStateOf(channel) }

    fun handleChannelSwitch(next: PlaylistChannel) {
        if (next != channel) {
            previousWatchedChannel = channel
            onChannelSelected(next)
        }
    }

    SideEffect {
        (activity as? MainActivity)?.updatePictureInPictureState(
            enabled = autoPictureInPicture,
            fullscreenPlaying = fullscreen && player.playWhenReady
        )
    }
    val schedule = epgGuide?.snapshot(channel.tvgId, channel.name, timeShiftHours = epgTimeShiftHours)

    DisposableEffect(player) {
        var resumePlaybackOnStart = false
        val pictureInPictureDismissedListener = {
            resumePlaybackOnStart = false
            manualFullscreen = false
            player.playWhenReady = false
            player.stop()
            player.clearMediaItems()
        }
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) consecutiveStallRecoveries = 0
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
        if (currentTrackedChannel != channel) {
            previousWatchedChannel = currentTrackedChannel
            currentTrackedChannel = channel
        }
        playbackError = null
        player.setMediaItem(MediaItem.fromUri(channel.streamUrl))
        player.prepare()
        player.play()
        if (autoEnterFullscreen) manualFullscreen = true
    }

    LaunchedEffect(fullscreenTrigger) {
        if (fullscreenTrigger > 0) {
            manualFullscreen = true
            landscapeFullscreenDismissed = false
        }
    }

    LaunchedEffect(player, channel.streamUrl, playbackState) {
        if (playbackState != Player.STATE_BUFFERING || !player.playWhenReady || playbackError != null) {
            return@LaunchedEffect
        }
        val positionBefore = player.currentPosition
        val bufferedBefore = player.bufferedPosition
        delay(LIVE_STALL_TIMEOUT_MILLIS)
        if (player.playbackState != Player.STATE_BUFFERING || !player.playWhenReady || playbackError != null) {
            return@LaunchedEffect
        }
        val positionProgress = player.currentPosition - positionBefore
        val bufferProgress = player.bufferedPosition - bufferedBefore
        if (positionProgress >= LIVE_STALL_MIN_PROGRESS_MILLIS || bufferProgress >= LIVE_STALL_MIN_PROGRESS_MILLIS) {
            return@LaunchedEffect
        }
        if (consecutiveStallRecoveries >= LIVE_STALL_MAX_RECOVERY_ATTEMPTS) {
            playbackError = strings.playbackError
            return@LaunchedEffect
        }

        consecutiveStallRecoveries++
        Log.w("FortressTV.Player", "Recovering stalled live playback; attempt=$consecutiveStallRecoveries")
        player.stop()
        player.setMediaItem(MediaItem.fromUri(channel.streamUrl))
        player.prepare()
        player.play()
    }

    Column(modifier) {
        if (!fullscreen) {
            val isTv = LocalFormFactor.current.isTelevision
            var isVideoFocused by remember { mutableStateOf(false) }
            Surface(
                onClick = { manualFullscreen = true; landscapeFullscreenDismissed = false },
                shape = RoundedCornerShape(14.dp),
                color = Color.Black,
                border = if (isVideoFocused) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp))
                    .onFocusChanged { isVideoFocused = it.isFocused }
                    .tvFocusHighlight(
                        shape = RoundedCornerShape(14.dp),
                        scaleOnFocus = if (isTv) 1.04f else 1.0f
                    )
            ) {
                Box(Modifier.fillMaxSize()) {
                    VideoSurface(
                        player = player,
                        playbackState = playbackState,
                        playbackError = playbackError,
                        fullscreen = false,
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                        onFullscreen = { manualFullscreen = true; landscapeFullscreenDismissed = false },
                        onToggleControls = {},
                        onRetry = {
                            playbackError = null
                            player.prepare()
                            player.play()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isTv && isVideoFocused) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Fullscreen,
                                    contentDescription = strings.fullscreen,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(channel.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (previousWatchedChannel != null) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    onClick = { previousWatchedChannel?.let(::handleChannelSwitch) },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            previousWatchedChannel?.name.orEmpty(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        schedule?.current?.let { current ->
            val startEpoch = current.startEpochMilli + (epgTimeShiftHours * 3600_000L)
            val stopEpoch = current.stopEpochMilli?.let { it + (epgTimeShiftHours * 3600_000L) }
            val nowMilli = Instant.now().toEpochMilli()
            val progress = if (stopEpoch != null && stopEpoch > startEpoch) {
                ((nowMilli - startEpoch).toFloat() / (stopEpoch - startEpoch).toFloat()).coerceIn(0f, 1f)
            } else null

            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            color = Color(0xFFE50914).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFFE50914).copy(alpha = 0.5f))
                        ) {
                            Row(
                                Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(6.dp).background(Color(0xFFE50914), CircleShape))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.liveBadge, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (startEpoch > 0L && stopEpoch != null) {
                            Text(
                                "${formatProgrammeTime(Instant.ofEpochMilli(startEpoch))} – ${formatProgrammeTime(Instant.ofEpochMilli(stopEpoch))}",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(current.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (progress != null) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .15f)
                        )
                    }
                    current.description?.let { description ->
                        if (description.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                description,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        } ?: run {
            Spacer(Modifier.height(8.dp))
            Text(strings.noCurrentProgramme, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        schedule?.next?.let { next ->
            val nextStartEpoch = next.startEpochMilli + (epgTimeShiftHours * 3600_000L)
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.next, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        next.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (nextStartEpoch > 0L) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            formatProgrammeTime(Instant.ofEpochMilli(nextStartEpoch)),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
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
            favoriteChannelIds = favoriteChannelIds,
            recentChannelIds = recentChannelIds,
            onToggleFavorite = onToggleFavorite,
            previousWatchedChannel = previousWatchedChannel,
            onChannelSelected = ::handleChannelSwitch,
            onExit = { manualFullscreen = false; landscapeFullscreenDismissed = true },
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
    onToggleControls: () -> Unit = {},
    onControlsVisibilityChanged: ((Boolean) -> Unit)? = null,
    onRetry: () -> Unit,
    showTrackSettings: Boolean = false,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var gestureLevel by remember { mutableStateOf<GestureLevel?>(null) }
    LaunchedEffect(gestureLevel) {
        if (gestureLevel != null) {
            delay(1_500)
            gestureLevel = null
        }
    }
    val onSurfaceClick: () -> Unit = {
        if (fullscreen) {
            onToggleControls()
        } else {
            onFullscreen()
        }
    }
    Box(
        modifier.background(Color.Black)
    ) {
        AndroidView(
            factory = { viewContext ->
                val view = if (fullscreen) {
                    GesturePlayerView(viewContext).apply {
                        onGestureLevelChanged = { gestureLevel = it }
                        onSingleTap = { onSurfaceClick() }
                    }
                } else {
                    PlayerView(viewContext).apply {
                        isClickable = false
                        isFocusable = false
                    }
                }
                view.apply {
                    this.player = player
                    useController = showTrackSettings
                    controllerAutoShow = showTrackSettings
                    controllerShowTimeoutMs = 3_500
                    this.resizeMode = resizeMode
                    keepScreenOn = true
                    if (fullscreen) {
                        setOnClickListener { onSurfaceClick() }
                    }
                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility ->
                            val isVisible = visibility == android.view.View.VISIBLE
                            onControlsVisibilityChanged?.invoke(isVisible)
                        }
                    )
                    setShowSubtitleButton(showTrackSettings)
                    subtitleView?.apply {
                        visibility = android.view.View.VISIBLE
                        setApplyEmbeddedStyles(true)
                        setApplyEmbeddedFontSizes(true)
                    }
                    findViewById<android.view.View>(androidx.media3.ui.R.id.exo_settings)?.visibility =
                        if (showTrackSettings) android.view.View.VISIBLE else android.view.View.GONE
                    findViewById<android.view.View>(androidx.media3.ui.R.id.exo_fullscreen)?.visibility = android.view.View.GONE
                    findViewById<android.view.View>(androidx.media3.ui.R.id.exo_minimal_fullscreen)?.visibility = android.view.View.GONE
                    if (!showTrackSettings) {
                        findViewById<android.view.View>(androidx.media3.ui.R.id.exo_progress)?.visibility = android.view.View.GONE
                        findViewById<android.view.View>(androidx.media3.ui.R.id.exo_bottom_bar)?.visibility = android.view.View.GONE
                        findViewById<android.view.View>(androidx.media3.ui.R.id.exo_center_controls)?.visibility = android.view.View.GONE
                        findViewById<android.view.View>(androidx.media3.ui.R.id.exo_basic_controls)?.visibility = android.view.View.GONE
                    }
                }
            },
            update = {
                it.player = player
                it.resizeMode = resizeMode
                it.useController = showTrackSettings
                it.controllerShowTimeoutMs = 3_500
                it.setControllerVisibilityListener(
                    PlayerView.ControllerVisibilityListener { visibility ->
                        val isVisible = visibility == android.view.View.VISIBLE
                        onControlsVisibilityChanged?.invoke(isVisible)
                    }
                )
                it.setShowSubtitleButton(showTrackSettings)
                it.subtitleView?.apply {
                    visibility = android.view.View.VISIBLE
                    setApplyEmbeddedStyles(true)
                    setApplyEmbeddedFontSizes(true)
                }
                it.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_settings)?.visibility =
                    if (showTrackSettings) android.view.View.VISIBLE else android.view.View.GONE
                if (!showTrackSettings) {
                    it.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_progress)?.visibility = android.view.View.GONE
                    it.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_bottom_bar)?.visibility = android.view.View.GONE
                    it.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_center_controls)?.visibility = android.view.View.GONE
                    it.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_basic_controls)?.visibility = android.view.View.GONE
                }
                (it as? GesturePlayerView)?.onGestureLevelChanged = { gestureLevel = it }
                (it as? GesturePlayerView)?.onSingleTap = { onSurfaceClick() }
                it.setOnClickListener { onSurfaceClick() }
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
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 44.dp)
        ) {
            val level = gestureLevel
            if (level != null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Row(
                        Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (level.control == GestureControl.BRIGHTNESS) Icons.Default.BrightnessMedium else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        LinearProgressIndicator(
                            progress = { level.value.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .width(100.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "${(level.value * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Adds the remote-only focus policy for fullscreen controls. Touch controls keep
 * their normal click and gesture behavior, while TV controls gain explicit
 * directional links and a visible focus treatment. OK/Enter is intentionally
 * left to the control's normal onClick implementation.
 */
private fun Modifier.tvFullscreenControl(
    enabled: Boolean,
    requester: FocusRequester,
    shape: Shape,
    scaleOnFocus: Float,
    leftTarget: FocusRequester? = null,
    rightTarget: FocusRequester? = null,
    upTarget: FocusRequester? = null,
    downTarget: FocusRequester? = null
): Modifier = if (!enabled) {
    this
} else {
    this.then(
        Modifier
            .focusProperties {
                leftTarget?.let { left = it }
                rightTarget?.let { right = it }
                upTarget?.let { up = it }
                downTarget?.let { down = it }
            }
            .focusRequester(requester)
            .tvFocusHighlight(shape = shape, scaleOnFocus = scaleOnFocus)
    )
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
    favoriteChannelIds: Set<String> = emptySet(),
    recentChannelIds: List<String> = emptyList(),
    onToggleFavorite: (PlaylistChannel) -> Unit = {},
    previousWatchedChannel: PlaylistChannel? = null,
    onChannelSelected: (PlaylistChannel) -> Unit,
    onExit: () -> Unit,
    onRetry: () -> Unit,
    inPictureInPicture: Boolean
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    var controlsVisible by remember { mutableStateOf(true) }
    var showChannels by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }
    var showTracksDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var sleepTimerMinutes by remember { mutableStateOf<Int?>(null) }
    var sleepTimerEndEpochMs by remember { mutableStateOf<Long?>(null) }
    var sleepTimerRemainingText by remember { mutableStateOf<String?>(null) }
    var locked by remember { mutableStateOf(false) }
    var lockPromptVisible by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var resizeModeNotice by remember { mutableStateOf<String?>(null) }
    var scheduleNow by remember { mutableStateOf(Instant.now()) }
    var currentTracks by remember { mutableStateOf<Tracks?>(player.currentTracks) }
    var channelSwitchNotice by remember { mutableStateOf<PlaylistChannel?>(null) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                currentTracks = tracks
            }
        }
        player.addListener(listener)
        currentTracks = player.currentTracks
        onDispose {
            player.removeListener(listener)
        }
    }

    LaunchedEffect(channel) {
        channelSwitchNotice = channel
        delay(1_800)
        channelSwitchNotice = null
    }

    val audioTracks = remember(currentTracks) { extractAudioTracks(currentTracks) }
    val subtitleTracks = remember(currentTracks) { extractSubtitleTracks(currentTracks) }

    val isTv = LocalFormFactor.current.isTelevision
    val playerFocusRequester = remember { FocusRequester() }
    val controlsInitialFocusRequester = remember { FocusRequester() }
    val backButtonFocusRequester = remember { FocusRequester() }
    val guideButtonFocusRequester = remember { FocusRequester() }
    val audioButtonFocusRequester = remember { FocusRequester() }
    val sleepTimerFocusRequester = remember { FocusRequester() }
    val aspectRatioFocusRequester = remember { FocusRequester() }
    val lockButtonFocusRequester = remember { FocusRequester() }
    val centerPrevFocusRequester = remember { FocusRequester() }
    val centerNextFocusRequester = remember { FocusRequester() }
    val favButtonFocusRequester = remember { FocusRequester() }
    var lastInteractionEpoch by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        if (isTv) {
            delay(100)
            try { playerFocusRequester.requestFocus() } catch (_: Throwable) {}
        }
    }

    LaunchedEffect(controlsVisible) {
        if (controlsVisible && isTv && !showChannels && !showGuide && !showTracksDialog && !showSleepTimerDialog) {
            delay(50)
            try { controlsInitialFocusRequester.requestFocus() } catch (_: Throwable) {}
        } else if (!controlsVisible && isTv && !showChannels && !showGuide && !showTracksDialog && !showSleepTimerDialog) {
            try { playerFocusRequester.requestFocus() } catch (_: Throwable) {}
        }
    }

    val isOverlayActive = showChannels || showGuide || showTracksDialog || showSleepTimerDialog || controlsVisible

    fun dismissTopmostOverlay() {
        when {
            showChannels -> showChannels = false
            showGuide -> showGuide = false
            showTracksDialog -> showTracksDialog = false
            showSleepTimerDialog -> showSleepTimerDialog = false
            controlsVisible -> controlsVisible = false
        }
    }

    BackHandler(enabled = isOverlayActive) {
        dismissTopmostOverlay()
    }

    LaunchedEffect(controlsVisible, showChannels, showGuide, showTracksDialog, showSleepTimerDialog, locked, lastInteractionEpoch) {
        if (controlsVisible && !showChannels && !showGuide && !showTracksDialog && !showSleepTimerDialog && !locked) {
            delay(if (isTv) 8_000 else 3_500)
            controlsVisible = false
        }
    }
    LaunchedEffect(sleepTimerEndEpochMs, sleepTimerMinutes) {
        val targetEpoch = sleepTimerEndEpochMs
        if (targetEpoch != null) {
            while (true) {
                val remainingMs = targetEpoch - System.currentTimeMillis()
                if (remainingMs <= 0L) {
                    Toast.makeText(context, strings.sleepTimerExpired, Toast.LENGTH_SHORT).show()
                    player.pause()
                    onExit()
                    break
                } else {
                    val mins = ((remainingMs / 1000L) / 60L).toInt() + 1
                    sleepTimerRemainingText = "${mins}m"
                }
                delay(1000)
            }
        } else {
            sleepTimerRemainingText = null
        }
    }
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
    val schedule = epgGuide?.snapshot(channel.tvgId, channel.name, scheduleNow, epgTimeShiftHours)
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
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(playerFocusRequester)
                .focusable()
                .onPreviewKeyEvent { keyEvent ->
                    val isBackKey = keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_BACK ||
                        keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ESCAPE
                    if (isTv && isOverlayActive && isBackKey) {
                        lastInteractionEpoch = System.currentTimeMillis()
                        if (keyEvent.type == KeyEventType.KeyUp) {
                            dismissTopmostOverlay()
                        }
                        true
                    } else {
                        false
                    }
                }
                .onKeyEvent { keyEvent ->
                    if (isOverlayActive) {
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            lastInteractionEpoch = System.currentTimeMillis()
                            when (keyEvent.nativeKeyEvent.keyCode) {
                                android.view.KeyEvent.KEYCODE_MEDIA_PLAY -> { player.play(); true }
                                android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> { player.pause(); true }
                                android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                                android.view.KeyEvent.KEYCODE_HEADSETHOOK -> {
                                    if (player.isPlaying) player.pause() else player.play()
                                    true
                                }
                                android.view.KeyEvent.KEYCODE_MEDIA_STOP -> { player.stop(); onExit(); true }
                                android.view.KeyEvent.KEYCODE_CHANNEL_UP -> { previousChannel?.let { onChannelSelected(it); true } ?: false }
                                android.view.KeyEvent.KEYCODE_CHANNEL_DOWN -> { nextChannel?.let { onChannelSelected(it); true } ?: false }
                                else -> false
                            }
                        } else false
                    } else {
                        val action = TvPlayerKeyDispatcher.dispatch(
                            keyCode = keyEvent.nativeKeyEvent.keyCode,
                            isActionDown = keyEvent.type == KeyEventType.KeyDown,
                            isLiveStream = true
                        )
                        if (action != null) {
                            lastInteractionEpoch = System.currentTimeMillis()
                            when (action) {
                                TvPlayerAction.TOGGLE_CONTROLS -> {
                                    controlsVisible = true
                                    true
                                }
                                TvPlayerAction.CHANNEL_UP -> {
                                    previousChannel?.let {
                                        onChannelSelected(it)
                                        true
                                    } ?: false
                                }
                                TvPlayerAction.CHANNEL_DOWN -> {
                                    nextChannel?.let {
                                        onChannelSelected(it)
                                        true
                                    } ?: false
                                }
                                TvPlayerAction.PLAY -> {
                                    player.play()
                                    true
                                }
                                TvPlayerAction.PAUSE -> {
                                    player.pause()
                                    true
                                }
                                TvPlayerAction.TOGGLE_PLAY_PAUSE -> {
                                    if (player.isPlaying) player.pause() else player.play()
                                    true
                                }
                                TvPlayerAction.STOP -> {
                                    player.stop()
                                    onExit()
                                    true
                                }
                                else -> false
                            }
                        } else false
                    }
                }
                .then(
                    if (!isTv) {
                        Modifier.pointerInput(Unit) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                if (change.position.x < 160f && dragAmount > 18f && !showChannels && !locked && !inPictureInPicture) {
                                    showChannels = true
                                }
                            }
                        }
                    } else Modifier
                )
        ) {
            VideoSurface(
                player = player,
                playbackState = playbackState,
                playbackError = playbackError,
                fullscreen = true,
                resizeMode = resizeMode,
                onFullscreen = onExit,
                onToggleControls = { controlsVisible = !controlsVisible },
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize()
            )

            // CHANNEL SWITCH HUD TOAST
            AnimatedVisibility(
                visible = channelSwitchNotice != null && !controlsVisible && !showChannels && !showGuide && !inPictureInPicture,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 28.dp)
            ) {
                val switchChannel = channelSwitchNotice
                if (switchChannel != null) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Black.copy(alpha = 0.82f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ChannelLogo(switchChannel, epgGuide?.findChannel(switchChannel.tvgId, switchChannel.name), modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(switchChannel.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = controlsVisible && !showChannels && !showGuide && !inPictureInPicture,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(Modifier.fillMaxSize()) {
                    // TOP BAR
                    val configuration = LocalConfiguration.current
                    val isCompact = configuration.screenWidthDp < 600

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                                )
                            )
                            .padding(horizontal = if (isCompact) 12.dp else 16.dp, vertical = if (isCompact) 10.dp else 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledIconButton(
                                onClick = onExit,
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color.Black.copy(alpha = 0.6f),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .size(if (isCompact) 36.dp else 40.dp)
                                    .tvFullscreenControl(
                                        enabled = isTv,
                                        requester = backButtonFocusRequester,
                                        shape = CircleShape,
                                        scaleOnFocus = 1.15f,
                                        rightTarget = controlsInitialFocusRequester,
                                        downTarget = centerPrevFocusRequester
                                    )
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back, modifier = Modifier.size(if (isCompact) 18.dp else 20.dp))
                            }
                            Surface(
                                onClick = { showChannels = true },
                                shape = if (isCompact) CircleShape else RoundedCornerShape(20.dp),
                                color = Color.Black.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                modifier = Modifier
                                    .tvFullscreenControl(
                                        enabled = isTv,
                                        requester = controlsInitialFocusRequester,
                                        shape = if (isCompact) CircleShape else RoundedCornerShape(20.dp),
                                        scaleOnFocus = 1.08f,
                                        leftTarget = backButtonFocusRequester,
                                        rightTarget = guideButtonFocusRequester,
                                        downTarget = centerPrevFocusRequester
                                    )
                            ) {
                                Row(
                                    Modifier.padding(if (isCompact) PaddingValues(9.dp) else PaddingValues(horizontal = 12.dp, vertical = 7.dp)),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Menu,
                                        contentDescription = strings.miniGuide,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    if (!isCompact) {
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            strings.miniGuide,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            Surface(
                                onClick = { showGuide = true },
                                shape = if (isCompact) CircleShape else RoundedCornerShape(20.dp),
                                color = Color.Black.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                modifier = Modifier
                                    .tvFullscreenControl(
                                        enabled = isTv,
                                        requester = guideButtonFocusRequester,
                                        shape = if (isCompact) CircleShape else RoundedCornerShape(20.dp),
                                        scaleOnFocus = 1.08f,
                                        leftTarget = controlsInitialFocusRequester,
                                        rightTarget = if (audioTracks.isNotEmpty() || subtitleTracks.isNotEmpty()) audioButtonFocusRequester else sleepTimerFocusRequester,
                                        downTarget = favButtonFocusRequester
                                    )
                            ) {
                                Row(
                                    Modifier.padding(if (isCompact) PaddingValues(9.dp) else PaddingValues(horizontal = 12.dp, vertical = 7.dp)),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = strings.programmeGuide,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    if (!isCompact) {
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            strings.programmeGuide,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            if (audioTracks.isNotEmpty() || subtitleTracks.isNotEmpty()) {
                                val hasActiveSubs = subtitleTracks.any { it.isSelected }
                                Surface(
                                    onClick = { showTracksDialog = true },
                                    shape = if (isCompact) CircleShape else RoundedCornerShape(20.dp),
                                    color = if (hasActiveSubs) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.6f),
                                    border = BorderStroke(1.dp, if (hasActiveSubs) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f)),
                                    modifier = Modifier
                                        .tvFullscreenControl(
                                            enabled = isTv,
                                            requester = audioButtonFocusRequester,
                                            shape = if (isCompact) CircleShape else RoundedCornerShape(20.dp),
                                            scaleOnFocus = 1.08f,
                                            leftTarget = guideButtonFocusRequester,
                                            rightTarget = sleepTimerFocusRequester,
                                            downTarget = favButtonFocusRequester
                                        )
                                ) {
                                    Row(
                                        Modifier.padding(if (isCompact) PaddingValues(9.dp) else PaddingValues(horizontal = 12.dp, vertical = 7.dp)),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Subtitles,
                                            contentDescription = strings.audioAndSubtitles,
                                            tint = if (hasActiveSubs) MaterialTheme.colorScheme.primary else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        if (!isCompact) {
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                strings.audioAndSubtitles,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = if (hasActiveSubs) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = { showSleepTimerDialog = true },
                                shape = if (isCompact && sleepTimerRemainingText == null && sleepTimerMinutes != -1) CircleShape else RoundedCornerShape(20.dp),
                                color = if (sleepTimerMinutes != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, if (sleepTimerMinutes != null) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f)),
                                modifier = Modifier
                                    .tvFullscreenControl(
                                        enabled = isTv,
                                        requester = sleepTimerFocusRequester,
                                        shape = if (isCompact && sleepTimerRemainingText == null && sleepTimerMinutes != -1) CircleShape else RoundedCornerShape(20.dp),
                                        scaleOnFocus = 1.08f,
                                        leftTarget = if (audioTracks.isNotEmpty() || subtitleTracks.isNotEmpty()) audioButtonFocusRequester else guideButtonFocusRequester,
                                        rightTarget = aspectRatioFocusRequester,
                                        downTarget = centerNextFocusRequester
                                    )
                            ) {
                                Row(
                                    Modifier.padding(if (isCompact && sleepTimerRemainingText == null && sleepTimerMinutes != -1) PaddingValues(9.dp) else PaddingValues(horizontal = 12.dp, vertical = 7.dp)),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Bedtime,
                                        contentDescription = strings.sleepTimer,
                                        tint = if (sleepTimerMinutes != null) MaterialTheme.colorScheme.primary else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    if (sleepTimerRemainingText != null) {
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            sleepTimerRemainingText.orEmpty(),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else if (sleepTimerMinutes == -1 && !isCompact) {
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            strings.endOfProgramme,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Surface(
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
                                shape = if (isCompact) CircleShape else RoundedCornerShape(20.dp),
                                color = Color.Black.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                modifier = Modifier
                                    .tvFullscreenControl(
                                        enabled = isTv,
                                        requester = aspectRatioFocusRequester,
                                        shape = if (isCompact) CircleShape else RoundedCornerShape(20.dp),
                                        scaleOnFocus = 1.08f,
                                        leftTarget = sleepTimerFocusRequester,
                                        rightTarget = lockButtonFocusRequester,
                                        downTarget = centerNextFocusRequester
                                    )
                            ) {
                                Row(
                                    Modifier.padding(if (isCompact) PaddingValues(9.dp) else PaddingValues(horizontal = 12.dp, vertical = 7.dp)),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AspectRatio,
                                        contentDescription = strings.aspectRatio,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    if (!isCompact) {
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            when (resizeMode) {
                                                AspectRatioFrameLayout.RESIZE_MODE_FILL -> strings.aspectFill
                                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> strings.aspectZoom
                                                else -> strings.aspectFit
                                            },
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            FilledIconButton(
                                onClick = {
                                    locked = true
                                    controlsVisible = false
                                },
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color.Black.copy(alpha = 0.6f),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .size(if (isCompact) 36.dp else 40.dp)
                                    .tvFullscreenControl(
                                        enabled = isTv,
                                        requester = lockButtonFocusRequester,
                                        shape = CircleShape,
                                        scaleOnFocus = 1.15f,
                                        leftTarget = aspectRatioFocusRequester,
                                        downTarget = centerNextFocusRequester
                                    )
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = strings.lockControls, modifier = Modifier.size(if (isCompact) 18.dp else 20.dp))
                            }
                        }
                    }

                    // CENTER CHANNEL SWITCH BUTTONS
                    Row(
                        modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = if (isCompact) 16.dp else 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = { previousChannel?.let(onChannelSelected) },
                            enabled = previousChannel != null,
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.6f),
                                contentColor = Color.White,
                                disabledContainerColor = Color.Black.copy(alpha = 0.25f),
                                disabledContentColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .size(if (isCompact) 46.dp else 56.dp)
                                .tvFullscreenControl(
                                    enabled = isTv,
                                    requester = centerPrevFocusRequester,
                                    shape = CircleShape,
                                    scaleOnFocus = 1.15f,
                                    rightTarget = centerNextFocusRequester,
                                    upTarget = controlsInitialFocusRequester,
                                    downTarget = favButtonFocusRequester
                                )
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = null, modifier = Modifier.size(if (isCompact) 24.dp else 28.dp))
                        }
                        FilledIconButton(
                            onClick = { nextChannel?.let(onChannelSelected) },
                            enabled = nextChannel != null,
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.6f),
                                contentColor = Color.White,
                                disabledContainerColor = Color.Black.copy(alpha = 0.25f),
                                disabledContentColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .size(if (isCompact) 46.dp else 56.dp)
                                .tvFullscreenControl(
                                    enabled = isTv,
                                    requester = centerNextFocusRequester,
                                    shape = CircleShape,
                                    scaleOnFocus = 1.15f,
                                    leftTarget = centerPrevFocusRequester,
                                    upTarget = aspectRatioFocusRequester,
                                    downTarget = favButtonFocusRequester
                                )
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(if (isCompact) 24.dp else 28.dp))
                        }
                    }

                    // BOTTOM CHANNEL INFO CARD
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = if (isCompact) 12.dp else 24.dp, vertical = if (isCompact) 10.dp else 20.dp),
                        shape = RoundedCornerShape(if (isCompact) 16.dp else 22.dp),
                        color = Color.Black.copy(alpha = 0.78f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = if (isCompact) 14.dp else 20.dp, vertical = if (isCompact) 10.dp else 14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ChannelLogo(channel, epgGuide?.findChannel(channel.tvgId, channel.name), modifier = Modifier.size(if (isCompact) 32.dp else 40.dp))
                                Spacer(Modifier.width(if (isCompact) 10.dp else 14.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            channel.name,
                                            color = Color.White,
                                            fontSize = if (isCompact) 16.sp else 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        val isFavorited = channel.libraryId()?.let(favoriteChannelIds::contains) == true
                                        IconButton(
                                            onClick = { onToggleFavorite(channel) },
                                            modifier = Modifier
                                                .size(if (isCompact) 28.dp else 32.dp)
                                                .tvFullscreenControl(
                                                    enabled = isTv,
                                                    requester = favButtonFocusRequester,
                                                    shape = CircleShape,
                                                    scaleOnFocus = 1.15f,
                                                    upTarget = centerPrevFocusRequester
                                                )
                                        ) {
                                            Icon(
                                                if (isFavorited) Icons.Default.Star else Icons.Outlined.StarOutline,
                                                contentDescription = if (isFavorited) strings.removeFavorite else strings.addFavorite,
                                                tint = if (isFavorited) Color(0xFFFFD700) else Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier.size(if (isCompact) 18.dp else 20.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(4.dp))
                                        Surface(
                                            color = Color(0xFFE50914).copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, Color(0xFFE50914).copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    Modifier
                                                        .size(5.dp)
                                                        .background(Color(0xFFE50914), CircleShape)
                                                )
                                                Spacer(Modifier.width(3.dp))
                                                Text(
                                                    strings.liveBadge,
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    schedule?.current?.let { current ->
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            current.title,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = if (isCompact) 13.sp else 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } ?: Text(strings.noCurrentProgramme, color = Color.White.copy(alpha = .68f), fontSize = if (isCompact) 11.sp else 13.sp)
                                }
                                if (previousWatchedChannel != null) {
                                    Surface(
                                        onClick = { onChannelSelected(previousWatchedChannel) },
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                        contentColor = Color.White,
                                        modifier = Modifier.tvFocusHighlight(
                                            shape = RoundedCornerShape(10.dp),
                                            scaleOnFocus = if (isTv) 1.08f else 1.0f
                                        )
                                    ) {
                                        Row(
                                            Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                previousWatchedChannel.name,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                }
                                if (!isCompact) {
                                    schedule?.next?.let { next ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color.White.copy(alpha = .1f),
                                            modifier = Modifier.widthIn(max = 240.dp)
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
                            }

                            if (programmeProgress != null && currentStopEpoch != null && currentStartEpoch != null) {
                                Spacer(Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { programmeProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = Color.White.copy(alpha = .2f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
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
            if (showTracksDialog) {
                PlaybackTracksDialog(
                    player = player,
                    audioTracks = audioTracks,
                    subtitleTracks = subtitleTracks,
                    onDismiss = { showTracksDialog = false }
                )
            }
            if (showSleepTimerDialog) {
                AlertDialog(
                    onDismissRequest = { showSleepTimerDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text(strings.sleepTimer, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val options = buildList {
                                add(null to strings.sleepTimerOff)
                                add(15 to "15 min")
                                add(30 to "30 min")
                                add(45 to "45 min")
                                add(60 to "60 min")
                                add(90 to "90 min")
                                add(120 to "120 min")
                                if (currentStopEpoch != null && currentStopEpoch > System.currentTimeMillis()) {
                                    add(-1 to strings.endOfProgramme)
                                }
                            }
                            options.forEach { (mins, label) ->
                                val isSelected = sleepTimerMinutes == mins
                                Surface(
                                    onClick = {
                                        sleepTimerMinutes = mins
                                        if (mins != null && mins > 0) {
                                            sleepTimerEndEpochMs = System.currentTimeMillis() + mins * 60_000L
                                            Toast.makeText(context, strings.sleepTimerSet(mins), Toast.LENGTH_SHORT).show()
                                        } else if (mins == -1 && currentStopEpoch != null) {
                                            sleepTimerEndEpochMs = currentStopEpoch
                                            Toast.makeText(context, strings.endOfProgramme, Toast.LENGTH_SHORT).show()
                                        } else {
                                            sleepTimerEndEpochMs = null
                                        }
                                        showSleepTimerDialog = false
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .tvFocusHighlight(
                                            shape = RoundedCornerShape(10.dp),
                                            scaleOnFocus = if (isTv) 1.05f else 1.0f
                                        )
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSleepTimerDialog = false }) {
                            Text(strings.close)
                        }
                    }
                )
            }
            AnimatedVisibility(
                visible = showChannels && !inPictureInPicture,
                enter = slideInHorizontally { -it } + fadeIn(),
                exit = slideOutHorizontally { -it } + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                FullscreenChannelDrawer(
                    channels = channels,
                    groups = groups,
                    favoriteChannelIds = favoriteChannelIds,
                    recentChannelIds = recentChannelIds,
                    selectedChannel = channel,
                    epgGuide = epgGuide,
                    epgTimeShiftHours = epgTimeShiftHours,
                    scheduleNow = scheduleNow,
                    onChannelSelected = { next ->
                        onChannelSelected(next)
                    },
                    onToggleFavorite = onToggleFavorite,
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
    val context = LocalContext.current
    val isTv = remember(context) {
        val uiModeManager = context.getSystemService(android.content.Context.UI_MODE_SERVICE) as? android.app.UiModeManager
        uiModeManager?.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    }
    val closeFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (isTv) {
            try {
                closeFocusRequester.requestFocus()
            } catch (_: Throwable) {}
        }
    }
    val now = Instant.now()
    val shiftMillis = epgTimeShiftHours * 3_600_000L
    val programmes = epgGuide?.findProgrammes(channel.tvgId, channel.name).orEmpty()
        .filter { programme -> (programme.stopEpochMilli ?: (programme.startEpochMilli + 3_600_000L)) + shiftMillis > now.toEpochMilli() }
        .sortedBy { it.startEpochMilli }
        .distinct()
    Surface(
        modifier = modifier.fillMaxHeight().widthIn(max = 560.dp).fillMaxWidth(if (isCompact) 0.88f else 0.5f),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .98f),
        shadowElevation = 12.dp
    ) {
        Column(Modifier.padding(if (isCompact) 14.dp else 20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(strings.programmeGuide, fontSize = if (isCompact) 20.sp else 25.sp, fontWeight = FontWeight.Bold)
                    Text(channel.name, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .focusRequester(closeFocusRequester)
                        .tvFocusHighlight(
                            shape = CircleShape,
                            scaleOnFocus = if (isTv) 1.15f else 1.0f
                        )
                ) { Icon(Icons.Default.Close, contentDescription = strings.close) }
            }
            Spacer(Modifier.height(16.dp))
            if (programmes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(strings.noUpcomingProgramme, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(
                        programmes,
                        key = { index, programme ->
                            "${programme.channelId}:${programme.startEpochMilli}:${programme.stopEpochMilli}:${programme.title}:$index"
                        }
                    ) { _, programme ->
                        val startAdjusted = programme.startEpochMilli + shiftMillis
                        val stopAdjusted = (programme.stopEpochMilli ?: (programme.startEpochMilli + 3_600_000L)) + shiftMillis
                        val isLive = now.toEpochMilli() in startAdjusted..stopAdjusted
                        val liveProgress = if (isLive && stopAdjusted > startAdjusted) {
                            ((now.toEpochMilli() - startAdjusted).toFloat() / (stopAdjusted - startAdjusted).toFloat()).coerceIn(0f, 1f)
                        } else null

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isLive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = if (isLive) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.tvFocusHighlight(
                                shape = RoundedCornerShape(12.dp),
                                scaleOnFocus = if (isTv) 1.03f else 1.0f
                            )
                        ) {
                            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (isLive) {
                                        Surface(
                                            color = Color(0xFFE50914).copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, Color(0xFFE50914).copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(Modifier.size(5.dp).background(Color(0xFFE50914), CircleShape))
                                                Spacer(Modifier.width(4.dp))
                                                Text(strings.liveBadge, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Text(
                                        formatProgrammeRange(programme.startEpochMilli, programme.stopEpochMilli, epgTimeShiftHours),
                                        color = if (isLive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        fontWeight = if (isLive) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    programme.title,
                                    fontSize = if (isLive) 17.sp else 15.sp,
                                    fontWeight = if (isLive) FontWeight.Bold else FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (liveProgress != null) {
                                    Spacer(Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { liveProgress },
                                        modifier = Modifier.fillMaxWidth().height(3.5.dp).clip(RoundedCornerShape(2.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .15f)
                                    )
                                }
                                programme.description?.let { desc ->
                                    if (desc.isNotBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            desc,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp,
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
    favoriteChannelIds: Set<String> = emptySet(),
    recentChannelIds: List<String> = emptyList(),
    selectedChannel: PlaylistChannel,
    epgGuide: EpgGuide?,
    epgTimeShiftHours: Int = 0,
    scheduleNow: Instant,
    onChannelSelected: (PlaylistChannel) -> Unit,
    onToggleFavorite: (PlaylistChannel) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val isTv = LocalFormFactor.current.isTelevision
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600
    val allGroup = strings.allChannels
    val favoritesGroup = "⭐ ${strings.navFavorites}"
    val recentsGroup = "🕒 ${strings.recentlyWatched}"
    val closeFocusRequester = remember { FocusRequester() }

    val favoriteChannels = remember(channels, favoriteChannelIds) {
        val channelMap = channels.associateBy { it.libraryId() }
        favoriteChannelIds.mapNotNull { channelMap[it] }
    }
    val recentChannels = remember(channels, recentChannelIds) {
        val channelMap = channels.associateBy { it.libraryId() }
        recentChannelIds.mapNotNull { channelMap[it] }
    }
    val effectiveGroups = remember(groups, channels) {
        if (groups.isNotEmpty()) groups else channels.mapNotNull { it.group?.takeIf { g -> g.isNotBlank() } }.distinct()
    }
    val groupCounts = remember(channels, effectiveGroups) {
        channels.groupingBy { it.group.orEmpty() }.eachCount()
    }

    var selectedGroup by remember(selectedChannel, allGroup) {
        mutableStateOf(
            when {
                selectedChannel.group.isNullOrBlank() -> allGroup
                else -> selectedChannel.group ?: allGroup
            }
        )
    }
    var isChoosingCategory by remember { mutableStateOf(false) }
    var categorySearchQuery by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }

    BackHandler {
        if (isChoosingCategory) {
            isChoosingCategory = false
        } else {
            onClose()
        }
    }

    LaunchedEffect(Unit) {
        if (isTv) {
            delay(50)
            try { closeFocusRequester.requestFocus() } catch (_: Throwable) {}
        }
    }

    val channelsInGroup = remember(channels, selectedGroup, allGroup, favoriteChannels, recentChannels) {
        when (selectedGroup) {
            allGroup -> channels
            favoritesGroup -> favoriteChannels
            recentsGroup -> recentChannels
            else -> channels.filter { it.group == selectedGroup }
        }
    }
    val visibleChannels = remember(channelsInGroup, query) {
        channelsInGroup.searchChannels(query)
    }
    val listState = rememberLazyListState()
    val selectedIndex = remember(visibleChannels, selectedChannel) {
        visibleChannels.indexOf(selectedChannel).coerceAtLeast(0)
    }

    LaunchedEffect(selectedGroup, query) {
        if (query.isBlank() && selectedIndex > 0) {
            listState.scrollToItem((selectedIndex - 2).coerceAtLeast(0))
        } else {
            listState.scrollToItem(0)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClose)
        )

        Surface(
            modifier = modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .widthIn(min = 380.dp, max = 680.dp)
                .fillMaxWidth(if (isCompact) 0.94f else 0.54f),
            shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
            color = Color(0xFF121212).copy(alpha = 0.92f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            shadowElevation = 24.dp
        ) {
            Column(Modifier.padding(if (isCompact) 14.dp else 18.dp)) {
                if (isChoosingCategory) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { isChoosingCategory = false },
                                modifier = Modifier.tvFocusHighlight(shape = CircleShape, scaleOnFocus = if (isTv) 1.15f else 1.0f)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back, tint = Color.White)
                            }
                            Spacer(Modifier.width(4.dp))
                            Text(
                                strings.selectCategory,
                                color = Color.White,
                                fontSize = if (isCompact) 18.sp else 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .focusRequester(closeFocusRequester)
                                .tvFocusHighlight(shape = CircleShape, scaleOnFocus = if (isTv) 1.15f else 1.0f)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = strings.close, tint = Color.White)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = categorySearchQuery,
                        onValueChange = { categorySearchQuery = it },
                        placeholder = { Text(strings.search) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) },
                        trailingIcon = {
                            if (categorySearchQuery.isNotEmpty()) {
                                IconButton(onClick = { categorySearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    val allCategoriesList = remember(allGroup, favoritesGroup, recentsGroup, effectiveGroups, favoriteChannels.size, recentChannels.size, groupCounts) {
                        buildList {
                            add(Triple(allGroup, channels.size, Icons.Default.FolderOpen))
                            if (favoriteChannels.isNotEmpty()) {
                                add(Triple(favoritesGroup, favoriteChannels.size, Icons.Default.Star))
                            }
                            if (recentChannels.isNotEmpty()) {
                                add(Triple(recentsGroup, recentChannels.size, Icons.Default.Replay))
                            }
                            effectiveGroups.forEach { g ->
                                add(Triple(g, groupCounts[g] ?: 0, Icons.Default.FolderOpen))
                            }
                        }
                    }

                    val filteredCategories = remember(allCategoriesList, categorySearchQuery) {
                        if (categorySearchQuery.isBlank()) allCategoriesList
                        else allCategoriesList.filter { it.first.contains(categorySearchQuery, ignoreCase = true) }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        items(filteredCategories, key = { "cat:${it.first}" }) { (categoryName, count, icon) ->
                            val isCurrent = categoryName == selectedGroup
                            Surface(
                                onClick = {
                                    selectedGroup = categoryName
                                    isChoosingCategory = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                else Color.White.copy(alpha = 0.08f),
                                border = if (isCurrent) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tvFocusHighlight(
                                        shape = RoundedCornerShape(12.dp),
                                        scaleOnFocus = if (isTv) 1.05f else 1.0f
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            icon,
                                            contentDescription = null,
                                            tint = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            categoryName,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            count.toString(),
                                            color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                strings.miniGuide,
                                color = Color.White,
                                fontSize = if (isCompact) 20.sp else 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${visibleChannels.size} ${strings.channelList.lowercase()}",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .focusRequester(closeFocusRequester)
                                .tvFocusHighlight(shape = CircleShape, scaleOnFocus = if (isTv) 1.15f else 1.0f)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = strings.close, tint = Color.White)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Surface(
                        onClick = { isChoosingCategory = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvFocusHighlight(
                                shape = RoundedCornerShape(12.dp),
                                scaleOnFocus = if (isTv) 1.05f else 1.0f
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(strings.categories, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text(selectedGroup, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(strings.changeCategory, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.width(2.dp))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    val focusManager = LocalFocusManager.current
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(strings.searchInGroup(selectedGroup.removePrefix("⭐ ").removePrefix("🕒 "))) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onPreviewKeyEvent { keyEvent ->
                                if (isTv && (keyEvent.type == KeyEventType.KeyDown || keyEvent.type == KeyEventType.KeyUp)) {
                                    when (keyEvent.nativeKeyEvent.keyCode) {
                                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                            if (keyEvent.type == KeyEventType.KeyUp) {
                                                focusManager.moveFocus(FocusDirection.Down)
                                            }
                                            return@onPreviewKeyEvent true
                                        }
                                        android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                            if (keyEvent.type == KeyEventType.KeyUp) {
                                                focusManager.moveFocus(FocusDirection.Up)
                                            }
                                            return@onPreviewKeyEvent true
                                        }
                                    }
                                }
                                false
                            }
                    )

                    Spacer(Modifier.height(8.dp))

                    if (recentChannels.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Replay,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                strings.quickRecents,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(recentChannels.take(5), key = { "mini-recent-${it.libraryId()}" }) { recentCh ->
                                val isCurrent = recentCh.libraryId() == selectedChannel.libraryId()
                                Surface(
                                    onClick = {
                                        if (!isCurrent) onChannelSelected(recentCh)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.09f),
                                    border = if (isCurrent) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                    modifier = Modifier.tvFocusHighlight(
                                        shape = RoundedCornerShape(8.dp),
                                        scaleOnFocus = if (isTv) 1.08f else 1.0f
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ChannelLogo(
                                            channel = recentCh,
                                            epgChannel = epgGuide?.findChannel(recentCh.tvgId, recentCh.name),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            recentCh.name,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = false,
                                onClick = { isChoosingCategory = true },
                                leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                                label = { Text(strings.categories, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier.tvFocusHighlight(shape = FilterChipDefaults.shape, scaleOnFocus = if (isTv) 1.08f else 1.0f)
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedGroup == allGroup,
                                onClick = { selectedGroup = allGroup },
                                label = { Text("${strings.allChannels} (${channels.size})", fontSize = 12.sp) },
                                modifier = Modifier.tvFocusHighlight(shape = FilterChipDefaults.shape, scaleOnFocus = if (isTv) 1.08f else 1.0f)
                            )
                        }
                        if (favoriteChannels.isNotEmpty()) {
                            item {
                                FilterChip(
                                    selected = selectedGroup == favoritesGroup,
                                    onClick = { selectedGroup = favoritesGroup },
                                    label = { Text("$favoritesGroup (${favoriteChannels.size})", fontSize = 12.sp) },
                                    modifier = Modifier.tvFocusHighlight(shape = FilterChipDefaults.shape, scaleOnFocus = if (isTv) 1.08f else 1.0f)
                                )
                            }
                        }
                        if (recentChannels.isNotEmpty()) {
                            item {
                                FilterChip(
                                    selected = selectedGroup == recentsGroup,
                                    onClick = { selectedGroup = recentsGroup },
                                    label = { Text("$recentsGroup (${recentChannels.size})", fontSize = 12.sp) },
                                    modifier = Modifier.tvFocusHighlight(shape = FilterChipDefaults.shape, scaleOnFocus = if (isTv) 1.08f else 1.0f)
                                )
                            }
                        }
                        items(effectiveGroups, key = { "group-chip-$it" }) { group ->
                            FilterChip(
                                selected = selectedGroup == group,
                                onClick = { selectedGroup = group },
                                label = { Text(group, fontSize = 12.sp) },
                                modifier = Modifier.tvFocusHighlight(shape = FilterChipDefaults.shape, scaleOnFocus = if (isTv) 1.08f else 1.0f)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        items(
                            items = visibleChannels,
                            key = { "mini-guide:${it.position}:${it.streamUrl}" },
                            contentType = { "channel" }
                        ) { item ->
                            val isSelected = item == selectedChannel
                            val isFav = item.libraryId()?.let(favoriteChannelIds::contains) == true
                            val presentation = remember(item.tvgId, item.name, epgGuide, scheduleNow, epgTimeShiftHours, strings) {
                                val current = epgGuide?.snapshot(item.tvgId, item.name, scheduleNow, epgTimeShiftHours)?.current
                                val shiftMillis = epgTimeShiftHours * 3_600_000L
                                val adjustedStart = current?.startEpochMilli?.plus(shiftMillis)
                                val adjustedStop = current?.stopEpochMilli?.plus(shiftMillis)
                                val progress = if (adjustedStart != null && adjustedStop != null && adjustedStop > adjustedStart) {
                                    ((scheduleNow.toEpochMilli() - adjustedStart).toFloat() /
                                        (adjustedStop - adjustedStart).toFloat()).coerceIn(0f, 1f)
                                } else null
                                val timeRange = if (adjustedStart != null && adjustedStop != null) {
                                    "${formatProgrammeTime(Instant.ofEpochMilli(adjustedStart))}–${formatProgrammeTime(Instant.ofEpochMilli(adjustedStop))}"
                                } else if (adjustedStop != null) {
                                    strings.until(formatProgrammeTime(Instant.ofEpochMilli(adjustedStop)))
                                } else {
                                    null
                                }
                                ChannelEpgPresentation(
                                    title = current?.title,
                                    timeRange = timeRange,
                                    progress = progress
                                )
                            }

                            Surface(
                                onClick = {
                                    if (!isSelected) {
                                        onChannelSelected(item)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                else Color.White.copy(alpha = 0.08f),
                                border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tvFocusHighlight(
                                        shape = RoundedCornerShape(12.dp),
                                        scaleOnFocus = if (isTv) 1.05f else 1.0f
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ChannelLogo(item, epgGuide?.findChannel(item.tvgId, item.name))
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (item.position > 0) {
                                                Text(
                                                    "${item.position}.",
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Spacer(Modifier.width(4.dp))
                                            }
                                            Text(
                                                item.name,
                                                color = Color.White,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Spacer(Modifier.height(2.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                presentation.title ?: strings.noCurrentProgramme,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (presentation.timeRange != null) {
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    presentation.timeRange,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                                                    fontSize = 11.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        if (presentation.progress != null) {
                                            Spacer(Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { presentation.progress },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(3.dp)
                                                    .clip(RoundedCornerShape(1.5.dp)),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                                                trackColor = Color.White.copy(alpha = 0.12f)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { onToggleFavorite(item) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (isFav) Color(0xFFFFB800) else Color.White.copy(alpha = 0.25f),
                                            modifier = Modifier.size(18.dp)
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

private fun formatLastUpdated(timestamp: Long, strings: Strings): String {
    if (timestamp == 0L) return strings.notUpdatedYet
    val diff = System.currentTimeMillis() - timestamp
    val hours = diff / (1000 * 60 * 60)
    return when {
        diff < 60_000 -> strings.updatedJustNow
        diff < 3600_000 -> strings.updatedMinutesAgo(diff / 60_000)
        hours < 24 -> strings.updatedHoursAgo(hours)
        else -> {
            val formatter = DateTimeFormatter.ofPattern("d MMM · HH:mm")
            strings.updatedAt(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(formatter))
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
    preferredAudioLanguage: String = "default",
    onPreferredAudioLanguageChanged: (String) -> Unit = {},
    preferredSubtitleLanguage: String = "off",
    onPreferredSubtitleLanguageChanged: (String) -> Unit = {},
    subtitleFallbackEnglish: Boolean = true,
    onSubtitleFallbackEnglishChanged: (Boolean) -> Unit = {},
    hideAdultContent: Boolean,
    onHideAdultContentChanged: (Boolean) -> Unit,
    parentalControlEnabled: Boolean,
    hasParentalPin: Boolean,
    onParentalControlToggled: (Boolean) -> Unit,
    onChangeParentalPin: () -> Unit,
    onRemoveParentalPin: () -> Unit,
    epgSource: ContentSource?,
    epgSourceName: String?,
    epgLastUpdate: Long,
    epgTimeShiftHours: Int,
    epgLoadedDays: Int = 3,
    playlist: Playlist?,
    epgGuide: EpgGuide?,
    hiddenCategories: Set<String> = emptySet(),
    loading: Boolean,
    onDismiss: () -> Unit,
    onPlaylistEnabledChanged: (PlaylistProfile, Boolean) -> Unit,
    onSourceRoleChanged: (PlaylistProfile, SourceRole) -> Unit,
    onMoveSource: (PlaylistProfile, Int) -> Unit,
    onRefreshPlaylist: (PlaylistProfile) -> Unit,
    onAddPlaylist: () -> Unit,
    onEditPlaylist: (PlaylistProfile) -> Unit,
    onDeletePlaylist: (PlaylistProfile) -> Unit,
    onEpgUrl: () -> Unit,
    onEpgFile: () -> Unit,
    onRefreshEpg: () -> Unit,
    onRemoveEpg: () -> Unit,
    onEpgTimeShiftChanged: (Int) -> Unit,
    onEpgLoadedDaysChanged: (Int) -> Unit,
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit
) {
    val strings = LocalAppStrings.current
    val configuration = LocalConfiguration.current
    val isTwoPane = configuration.screenWidthDp >= 640
    val isTv = LocalFormFactor.current.isTelevision

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val tabFocusRequesters = remember { List(4) { FocusRequester() } }
    var settingsContentHasFocus by remember { mutableStateOf(false) }

    LaunchedEffect(isTv) {
        if (isTv) {
            delay(150)
            try {
                tabFocusRequesters[initialTab.coerceIn(tabFocusRequesters.indices)].requestFocus()
            } catch (_: Throwable) {}
        }
    }

    val tabItems = listOf(
        Pair(strings.tabGeneral, Icons.Default.Settings),
        Pair(strings.tabPlaylists, Icons.Default.FolderOpen),
        Pair(strings.tabEpg, Icons.Default.DateRange),
        Pair(strings.tabAbout, Icons.Default.Info)
    )

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    fun hideKeyboard() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    BackHandler(onBack = {
        hideKeyboard()
        if (isTv && settingsContentHasFocus) {
            runCatching { tabFocusRequesters[selectedTab].requestFocus() }
        } else {
            onDismiss()
        }
    })

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .imePadding()
            .then(
                if (!isTv) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { hideKeyboard() }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(if (isTwoPane) 20.dp else 16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = (if (isTwoPane) {
                Modifier
                    .fillMaxWidth(0.88f)
                    .widthIn(max = 940.dp)
                    .fillMaxHeight(0.85f)
                    .heightIn(max = 640.dp)
                    .padding(16.dp)
            } else {
                Modifier
                    .fillMaxWidth(0.96f)
                    .fillMaxHeight(0.94f)
                    .padding(6.dp)
            }).then(
                if (!isTv) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* consume clicks inside settings */ }
                } else Modifier
            )
        ) {
            Column(Modifier.fillMaxSize().padding(if (isTwoPane) 18.dp else 12.dp)) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        strings.settingsTitle,
                        fontSize = if (isTwoPane) 20.sp else 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .tvFocusHighlight(
                                shape = CircleShape,
                                scaleOnFocus = if (isTv) 1.15f else 1.0f
                            )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = strings.close, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (isTwoPane) {
                    Row(Modifier.fillMaxSize()) {
                        // Left Tab Navigation Rail
                        Column(
                            Modifier
                                .width(180.dp)
                                .fillMaxHeight()
                                .padding(end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            tabItems.forEachIndexed { index, (title, icon) ->
                                val isSelected = selectedTab == index
                                Surface(
                                    onClick = { selectedTab = index },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .focusRequester(tabFocusRequesters[index])
                                        .tvFocusHighlight(
                                            shape = RoundedCornerShape(10.dp),
                                            scaleOnFocus = if (isTv) 1.03f else 1.0f
                                        )
                                ) {
                                    Row(
                                        Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        // Right Content Pane
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .onFocusChanged { settingsContentHasFocus = it.hasFocus }
                        ) {
                            when (selectedTab) {
                                0 -> GeneralTab(
                                    appLanguage = appLanguage,
                                    onLanguageChanged = onLanguageChanged,
                                    playChannelsFullscreen = playChannelsFullscreen,
                                    onPlayChannelsFullscreenChanged = onPlayChannelsFullscreenChanged,
                                    autoPictureInPicture = autoPictureInPicture,
                                    onAutoPictureInPictureChanged = onAutoPictureInPictureChanged,
                                    preferredAudioLanguage = preferredAudioLanguage,
                                    onPreferredAudioLanguageChanged = onPreferredAudioLanguageChanged,
                                    preferredSubtitleLanguage = preferredSubtitleLanguage,
                                    onPreferredSubtitleLanguageChanged = onPreferredSubtitleLanguageChanged,
                                    subtitleFallbackEnglish = subtitleFallbackEnglish,
                                    onSubtitleFallbackEnglishChanged = onSubtitleFallbackEnglishChanged,
                                    hideAdultContent = hideAdultContent,
                                    onHideAdultContentChanged = onHideAdultContentChanged,
                                    parentalControlEnabled = parentalControlEnabled,
                                    hasParentalPin = hasParentalPin,
                                    onParentalControlToggled = onParentalControlToggled,
                                    onChangeParentalPin = onChangeParentalPin,
                                    onRemoveParentalPin = onRemoveParentalPin,
                                    onCreateBackup = onCreateBackup,
                                    onRestoreBackup = onRestoreBackup
                                )
                                1 -> PlaylistsTab(
                                    playlists = playlists,
                                    activeProfile = activeProfile,
                                    loading = loading,
                                    onPlaylistEnabledChanged = onPlaylistEnabledChanged,
                                    onSourceRoleChanged = onSourceRoleChanged,
                                    onMoveSource = onMoveSource,
                                    onRefreshPlaylist = onRefreshPlaylist,
                                    onAddPlaylist = onAddPlaylist,
                                    onEditPlaylist = onEditPlaylist,
                                    onDeletePlaylist = onDeletePlaylist
                                )
                                2 -> EpgTab(
                                    epgSource = epgSource,
                                    epgSourceName = epgSourceName,
                                    epgLastUpdate = epgLastUpdate,
                                    epgTimeShiftHours = epgTimeShiftHours,
                                    epgLoadedDays = epgLoadedDays,
                                    playlist = playlist,
                                    epgGuide = epgGuide,
                                    hideAdultContent = hideAdultContent,
                                    hiddenCategories = hiddenCategories,
                                    loading = loading,
                                    onEpgUrl = onEpgUrl,
                                    onEpgFile = onEpgFile,
                                    onRefreshEpg = onRefreshEpg,
                                    onRemoveEpg = onRemoveEpg,
                                    onEpgTimeShiftChanged = onEpgTimeShiftChanged,
                                    onEpgLoadedDaysChanged = onEpgLoadedDaysChanged
                                )
                                3 -> AboutTab()
                            }
                        }
                    }
                } else {
                    // Single pane compact top tabs
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    ) {
                        tabItems.forEachIndexed { index, (title, _) ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        title,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (selectedTab) {
                            0 -> GeneralTab(
                                appLanguage = appLanguage,
                                onLanguageChanged = onLanguageChanged,
                                playChannelsFullscreen = playChannelsFullscreen,
                                onPlayChannelsFullscreenChanged = onPlayChannelsFullscreenChanged,
                                autoPictureInPicture = autoPictureInPicture,
                                onAutoPictureInPictureChanged = onAutoPictureInPictureChanged,
                                preferredAudioLanguage = preferredAudioLanguage,
                                onPreferredAudioLanguageChanged = onPreferredAudioLanguageChanged,
                                preferredSubtitleLanguage = preferredSubtitleLanguage,
                                onPreferredSubtitleLanguageChanged = onPreferredSubtitleLanguageChanged,
                                subtitleFallbackEnglish = subtitleFallbackEnglish,
                                onSubtitleFallbackEnglishChanged = onSubtitleFallbackEnglishChanged,
                                hideAdultContent = hideAdultContent,
                                onHideAdultContentChanged = onHideAdultContentChanged,
                                parentalControlEnabled = parentalControlEnabled,
                                hasParentalPin = hasParentalPin,
                                onParentalControlToggled = onParentalControlToggled,
                                onChangeParentalPin = onChangeParentalPin,
                                onRemoveParentalPin = onRemoveParentalPin,
                                onCreateBackup = onCreateBackup,
                                onRestoreBackup = onRestoreBackup
                            )
                            1 -> PlaylistsTab(
                                playlists = playlists,
                                activeProfile = activeProfile,
                                loading = loading,
                                onPlaylistEnabledChanged = onPlaylistEnabledChanged,
                                onSourceRoleChanged = onSourceRoleChanged,
                                onMoveSource = onMoveSource,
                                onRefreshPlaylist = onRefreshPlaylist,
                                onAddPlaylist = onAddPlaylist,
                                onEditPlaylist = onEditPlaylist,
                                onDeletePlaylist = onDeletePlaylist
                            )
                            2 -> EpgTab(
                                epgSource = epgSource,
                                epgSourceName = epgSourceName,
                                epgLastUpdate = epgLastUpdate,
                                epgTimeShiftHours = epgTimeShiftHours,
                                epgLoadedDays = epgLoadedDays,
                                playlist = playlist,
                                epgGuide = epgGuide,
                                hideAdultContent = hideAdultContent,
                                hiddenCategories = hiddenCategories,
                                loading = loading,
                                onEpgUrl = onEpgUrl,
                                onEpgFile = onEpgFile,
                                onRefreshEpg = onRefreshEpg,
                                onRemoveEpg = onRemoveEpg,
                                onEpgTimeShiftChanged = onEpgTimeShiftChanged,
                                onEpgLoadedDaysChanged = onEpgLoadedDaysChanged
                            )
                            3 -> AboutTab()
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 2.dp)
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val isTv = LocalFormFactor.current.isTelevision
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusHighlight(
                shape = RoundedCornerShape(8.dp),
                scaleOnFocus = if (isTv) 1.02f else 1.0f
            )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedBorderColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = Color(0xFF94A3B8),
                    uncheckedTrackColor = Color(0xFF1E293B),
                    uncheckedBorderColor = Color(0xFF475569),
                    disabledCheckedThumbColor = Color.White.copy(alpha = 0.85f),
                    disabledCheckedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                    disabledCheckedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                    disabledUncheckedThumbColor = Color(0xFF94A3B8).copy(alpha = 0.38f),
                    disabledUncheckedTrackColor = Color(0xFF1E293B).copy(alpha = 0.38f),
                    disabledUncheckedBorderColor = Color(0xFF475569).copy(alpha = 0.38f)
                ),
                modifier = Modifier
                    .scale(0.8f)
                    .focusable(!isTv)
            )
        }
    }
}

@Composable
private fun SettingsDropdownPickerRow(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    selectedValue: String,
    options: List<Pair<String, String>>,
    onValueSelected: (String) -> Unit
) {
    val isTv = LocalFormFactor.current.isTelevision
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = options.firstOrNull { it.first == selectedValue }?.second ?: selectedValue

    Row(
        Modifier
            .fillMaxWidth()
            .focusProperties { canFocus = !isTv }
            .clickable { expanded = true }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Box {
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.tvFocusHighlight(
                    shape = RoundedCornerShape(8.dp),
                    scaleOnFocus = if (isTv) 1.05f else 1.0f
                )
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        currentLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (code, label) ->
                    val isSelected = code == selectedValue
                    DropdownMenuItem(
                        text = {
                            Text(
                                label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            expanded = false
                            onValueSelected(code)
                        },
                        trailingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun GeneralTab(
    appLanguage: AppLanguage,
    onLanguageChanged: (AppLanguage) -> Unit,
    playChannelsFullscreen: Boolean,
    onPlayChannelsFullscreenChanged: (Boolean) -> Unit,
    autoPictureInPicture: Boolean,
    onAutoPictureInPictureChanged: (Boolean) -> Unit,
    preferredAudioLanguage: String = "default",
    onPreferredAudioLanguageChanged: (String) -> Unit = {},
    preferredSubtitleLanguage: String = "off",
    onPreferredSubtitleLanguageChanged: (String) -> Unit = {},
    subtitleFallbackEnglish: Boolean = true,
    onSubtitleFallbackEnglishChanged: (Boolean) -> Unit = {},
    hideAdultContent: Boolean,
    onHideAdultContentChanged: (Boolean) -> Unit,
    parentalControlEnabled: Boolean,
    hasParentalPin: Boolean,
    onParentalControlToggled: (Boolean) -> Unit,
    onChangeParentalPin: () -> Unit,
    onRemoveParentalPin: () -> Unit,
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit
) {
    val strings = LocalAppStrings.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Language Group
        SettingsGroup(title = strings.language) {
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { lang ->
                    val isSelected = lang == appLanguage
                    Surface(
                        onClick = { onLanguageChanged(lang) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Row(
                            Modifier.fillMaxSize().padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(lang.flag, fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                lang.displayName,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Playback Group
        SettingsGroup(title = strings.playback) {
            SettingsToggleRow(
                title = strings.playChannelsFullscreen,
                subtitle = strings.playChannelsFullscreenDesc,
                icon = Icons.Default.Fullscreen,
                checked = playChannelsFullscreen,
                onCheckedChange = onPlayChannelsFullscreenChanged
            )
            HorizontalDivider(Modifier.padding(horizontal = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            SettingsToggleRow(
                title = strings.autoPictureInPicture,
                subtitle = strings.autoPictureInPictureDesc,
                icon = Icons.Default.AspectRatio,
                checked = autoPictureInPicture,
                onCheckedChange = onAutoPictureInPictureChanged
            )
        }

        // Audio & Subtitles Defaults Group
        SettingsGroup(title = strings.audioAndSubtitlesDefaults) {
            val audioOptions = listOf(
                "default" to strings.defaultMediaAudio,
                "pt" to strings.langPortuguese,
                "en" to strings.langEnglish,
                "es" to strings.langSpanish,
                "fr" to strings.langFrench,
                "de" to strings.langGerman,
                "it" to strings.langItalian
            )
            val subOptions = listOf(
                "off" to strings.subtitlesOff,
                "pt" to strings.langPortuguese,
                "en" to strings.langEnglish,
                "es" to strings.langSpanish,
                "fr" to strings.langFrench,
                "de" to strings.langGerman,
                "it" to strings.langItalian
            )
            SettingsDropdownPickerRow(
                title = strings.preferredAudioLanguage,
                subtitle = "",
                icon = Icons.Default.VolumeUp,
                selectedValue = preferredAudioLanguage,
                options = audioOptions,
                onValueSelected = onPreferredAudioLanguageChanged
            )
            HorizontalDivider(Modifier.padding(horizontal = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            SettingsDropdownPickerRow(
                title = strings.preferredSubtitleLanguage,
                subtitle = "",
                icon = Icons.Default.Subtitles,
                selectedValue = preferredSubtitleLanguage,
                options = subOptions,
                onValueSelected = onPreferredSubtitleLanguageChanged
            )
            HorizontalDivider(Modifier.padding(horizontal = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            SettingsToggleRow(
                title = strings.subtitleFallbackEnglish,
                subtitle = strings.subtitleFallbackEnglishDesc,
                icon = Icons.Default.Translate,
                checked = subtitleFallbackEnglish,
                onCheckedChange = onSubtitleFallbackEnglishChanged
            )
        }

        // Backup & Restore Group
        SettingsGroup(title = strings.backupAndRestore) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCreateBackup)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Backup,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(strings.createBackup, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(
                        strings.createBackupDesc,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
                Icon(
                    Icons.Default.FileUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            HorizontalDivider(Modifier.padding(horizontal = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRestoreBackup)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Restore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(strings.restoreBackup, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(
                        strings.restoreBackupDesc,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
                Icon(
                    Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Parental Control Group
        SettingsGroup(title = strings.parentalControl) {
            SettingsToggleRow(
                title = strings.parentalControl,
                subtitle = strings.parentalControlDesc,
                icon = if (parentalControlEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                checked = parentalControlEnabled,
                onCheckedChange = onParentalControlToggled
            )
            if (parentalControlEnabled) {
                HorizontalDivider(Modifier.padding(horizontal = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onChangeParentalPin,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.changeParentalPin, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onRemoveParentalPin,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.removeParentalPin, fontSize = 12.sp)
                    }
                }
            }
        }

        // Content Filter Group
        SettingsGroup(title = strings.categories) {
            SettingsToggleRow(
                title = strings.hideAdultContent,
                subtitle = strings.hideAdultContentSubtitle,
                icon = Icons.Default.VisibilityOff,
                checked = hideAdultContent,
                onCheckedChange = onHideAdultContentChanged
            )
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
    onEditPlaylist: (PlaylistProfile) -> Unit,
    onDeletePlaylist: (PlaylistProfile) -> Unit
) {
    val strings = LocalAppStrings.current
    val isTv = LocalFormFactor.current.isTelevision
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${playlists.size} ${if (playlists.size == 1) strings.sources else strings.sources}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Button(
                onClick = onAddPlaylist,
                enabled = !loading,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(34.dp)
                    .tvFocusHighlight(
                        shape = ButtonDefaults.shape,
                        scaleOnFocus = if (isTv) 1.05f else 1.0f
                    )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.addPlaylist, fontSize = 12.sp)
            }
        }

        if (playlists.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("No sources configured", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Add your M3U / M3U8 URL or Xtream Codes server to get started.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                val orderedSources = playlists.sortedBy { it.order }
                items(orderedSources, key = { it.id }) { item ->
                    val isActive = item.enabled
                    val sourceIndex = orderedSources.indexOfFirst { it.id == item.id }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = if (isActive) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvFocusHighlight(
                                shape = RoundedCornerShape(10.dp),
                                scaleOnFocus = if (isTv) 1.02f else 1.0f
                            )
                    ) {
                        if (isCompact) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                        IconButton(
                                            onClick = { onMoveSource(item, -1) },
                                            enabled = !loading && sourceIndex > 0,
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = strings.moveSourceUp, modifier = Modifier.size(13.dp))
                                        }
                                        IconButton(
                                            onClick = { onMoveSource(item, 1) },
                                            enabled = !loading && sourceIndex in 0 until orderedSources.lastIndex,
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowDownward, contentDescription = strings.moveSourceDown, modifier = Modifier.size(13.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        when (item.source) {
                                            is ContentSource.LocalFile -> Icons.Default.FolderOpen
                                            is ContentSource.RemoteUrl -> Icons.Default.Link
                                            is ContentSource.Xtream -> Icons.Default.LiveTv
                                        },
                                        contentDescription = null,
                                        tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        item.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Switch(
                                        checked = item.enabled,
                                        onCheckedChange = { onPlaylistEnabledChanged(item, it) },
                                        enabled = !loading && (!item.enabled || playlists.count { it.enabled } > 1),
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                                            uncheckedThumbColor = Color(0xFF94A3B8),
                                            uncheckedTrackColor = Color(0xFF1E293B),
                                            uncheckedBorderColor = Color(0xFF475569),
                                            disabledCheckedThumbColor = Color.White.copy(alpha = 0.85f),
                                            disabledCheckedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                            disabledCheckedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                            disabledUncheckedThumbColor = Color(0xFF94A3B8).copy(alpha = 0.38f),
                                            disabledUncheckedTrackColor = Color(0xFF1E293B).copy(alpha = 0.38f),
                                            disabledUncheckedBorderColor = Color(0xFF475569).copy(alpha = 0.38f)
                                        ),
                                        modifier = Modifier.scale(0.75f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                                        SourceRoleSelector(
                                            role = item.role,
                                            enabled = !loading,
                                            onRoleChanged = { onSourceRoleChanged(item, it) }
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        if (item.channelCount > 0) {
                                            Text(
                                                strings.channelsCount(item.channelCount),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (item.source !is ContentSource.LocalFile) {
                                            IconButton(
                                                onClick = { onRefreshPlaylist(item) },
                                                enabled = !loading,
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = strings.refreshPlaylist, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        IconButton(
                                            onClick = { onEditPlaylist(item) },
                                            enabled = !loading,
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = strings.editPlaylist, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { onDeletePlaylist(item) },
                                            enabled = !loading,
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                    IconButton(
                                        onClick = { onMoveSource(item, -1) },
                                        enabled = !loading && sourceIndex > 0,
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = strings.moveSourceUp, modifier = Modifier.size(14.dp))
                                    }
                                    IconButton(
                                        onClick = { onMoveSource(item, 1) },
                                        enabled = !loading && sourceIndex in 0 until orderedSources.lastIndex,
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = strings.moveSourceDown, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    when (item.source) {
                                        is ContentSource.LocalFile -> Icons.Default.FolderOpen
                                        is ContentSource.RemoteUrl -> Icons.Default.Link
                                        is ContentSource.Xtream -> Icons.Default.LiveTv
                                    },
                                    contentDescription = null,
                                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            item.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        SourceRoleSelector(
                                            role = item.role,
                                            enabled = !loading,
                                            onRoleChanged = { onSourceRoleChanged(item, it) }
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (item.channelCount > 0) {
                                            Text(
                                                "${strings.channelsCount(item.channelCount)} · ",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            formatLastUpdated(item.lastUpdated, strings),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(Modifier.width(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Switch(
                                        checked = item.enabled,
                                        onCheckedChange = { onPlaylistEnabledChanged(item, it) },
                                        enabled = !loading && (!item.enabled || playlists.count { it.enabled } > 1),
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                                            uncheckedThumbColor = Color(0xFF94A3B8),
                                            uncheckedTrackColor = Color(0xFF1E293B),
                                            uncheckedBorderColor = Color(0xFF475569),
                                            disabledCheckedThumbColor = Color.White.copy(alpha = 0.85f),
                                            disabledCheckedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                            disabledCheckedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                            disabledUncheckedThumbColor = Color(0xFF94A3B8).copy(alpha = 0.38f),
                                            disabledUncheckedTrackColor = Color(0xFF1E293B).copy(alpha = 0.38f),
                                            disabledUncheckedBorderColor = Color(0xFF475569).copy(alpha = 0.38f)
                                        ),
                                        modifier = Modifier.scale(0.75f)
                                    )
                                    if (item.source !is ContentSource.LocalFile) {
                                        IconButton(
                                            onClick = { onRefreshPlaylist(item) },
                                            enabled = !loading,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = strings.refreshPlaylist, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                    IconButton(
                                        onClick = { onEditPlaylist(item) },
                                        enabled = !loading,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = strings.editPlaylist, modifier = Modifier.size(15.dp))
                                    }
                                    IconButton(
                                        onClick = { onDeletePlaylist(item) },
                                        enabled = !loading,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
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
        Surface(
            onClick = { expanded = true },
            enabled = enabled,
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        ) {
            Row(
                Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    sourceRoleLabel(role, strings),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(1.dp))
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SourceRole.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(sourceRoleLabel(option, strings), fontSize = 12.sp) },
                    onClick = {
                        expanded = false
                        onRoleChanged(option)
                    },
                    trailingIcon = if (option == role) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
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
    epgLoadedDays: Int = 3,
    playlist: Playlist?,
    epgGuide: EpgGuide?,
    hideAdultContent: Boolean = false,
    hiddenCategories: Set<String> = emptySet(),
    loading: Boolean,
    onEpgUrl: () -> Unit,
    onEpgFile: () -> Unit,
    onRefreshEpg: () -> Unit,
    onRemoveEpg: () -> Unit,
    onEpgTimeShiftChanged: (Int) -> Unit,
    onEpgLoadedDaysChanged: (Int) -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val effectiveChannels = remember(playlist, hideAdultContent, hiddenCategories) {
        if (playlist == null) emptyList()
        else {
            val base = if (hiddenCategories.isEmpty()) playlist.channels else playlist.channels.filter { (it.group.orEmpty()) !in hiddenCategories }
            if (hideAdultContent) base.filterNot { isAdultContent(it.name, it.group) } else base
        }
    }
    val matchedCount = remember(effectiveChannels, epgGuide) {
        if (epgGuide == null) 0 else effectiveChannels.count { epgGuide.resolveChannelId(it.tvgId, it.name) != null }
    }
    val unmatchedChannels = remember(effectiveChannels, epgGuide) {
        if (epgGuide == null) effectiveChannels
        else effectiveChannels.filter { epgGuide.resolveChannelId(it.tvgId, it.name) == null }
    }
    var showUnmatchedInSettings by remember { mutableStateOf(false) }

    val currentUnmatched by rememberUpdatedState(unmatchedChannels)
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            val channelsToExport = currentUnmatched
            val text = generateUnmatchedChannelsText(channelsToExport)
            val success = writeTextToUri(context, uri, text)
            if (success) {
                Toast.makeText(context, "${strings.channelsListSaved} (${channelsToExport.size})", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Error: could not write to selected location", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun shareList() {
        val text = generateUnmatchedChannelsText(currentUnmatched)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_TITLE, strings.unmatchedEpgChannels)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, strings.shareChannelsList)
        context.startActivity(shareIntent)
    }

    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // EPG Source
        SettingsGroup(title = strings.xmltvSource) {
            if (isCompact) {
                Column(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (epgSource is ContentSource.RemoteUrl) Icons.Default.Link else Icons.Default.DateRange,
                            contentDescription = null,
                            tint = if (epgSource != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                epgSourceName ?: strings.noEpgConfiguredMsg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (epgSource != null) {
                                Text(
                                    formatLastUpdated(epgLastUpdate, strings),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (epgSource is ContentSource.RemoteUrl) {
                            IconButton(onClick = onRefreshEpg, enabled = !loading, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = strings.refresh, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                        if (epgSource != null) {
                            IconButton(onClick = onRemoveEpg, enabled = !loading, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = strings.removeEpg, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onEpgUrl,
                            enabled = !loading,
                            modifier = Modifier.weight(1f).height(34.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (epgSource is ContentSource.RemoteUrl) strings.edit else strings.remoteUrl, fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = onEpgFile,
                            enabled = !loading,
                            modifier = Modifier.weight(1f).height(34.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (epgSource is ContentSource.LocalFile) strings.edit else strings.localFile, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (epgSource is ContentSource.RemoteUrl) Icons.Default.Link else Icons.Default.DateRange,
                        contentDescription = null,
                        tint = if (epgSource != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            epgSourceName ?: strings.noEpgConfiguredMsg,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (epgSource != null) {
                            Text(
                                formatLastUpdated(epgLastUpdate, strings),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (epgSource is ContentSource.RemoteUrl) {
                            IconButton(onClick = onRefreshEpg, enabled = !loading, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = strings.refresh, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                        OutlinedButton(
                            onClick = onEpgUrl,
                            enabled = !loading,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(if (epgSource is ContentSource.RemoteUrl) strings.edit else strings.remoteUrl, fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = onEpgFile,
                            enabled = !loading,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(if (epgSource is ContentSource.LocalFile) strings.edit else strings.localFile, fontSize = 11.sp)
                        }
                        if (epgSource != null) {
                            IconButton(onClick = onRemoveEpg, enabled = !loading, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = strings.removeEpg, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Channel Matching Status
        if (effectiveChannels.isNotEmpty()) {
            SettingsGroup(title = "EPG Matching") {
                if (isCompact) {
                    Column(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        if (epgGuide != null) strings.epgMatched(matchedCount, effectiveChannels.size) else strings.unmatchedEpgChannels,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (epgGuide == null || unmatchedChannels.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                    if (epgGuide != null && effectiveChannels.isNotEmpty()) {
                                        Spacer(Modifier.width(6.dp))
                                        val pct = (matchedCount * 100) / effectiveChannels.size
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (pct == 100) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                                        ) {
                                            Text(
                                                "$pct%",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (pct == 100) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    if (epgGuide != null && unmatchedChannels.isEmpty()) strings.allEpgMatched else strings.unmatchedEpgCount(unmatchedChannels.size),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (unmatchedChannels.isNotEmpty()) {
                                IconButton(onClick = ::shareList, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Share, contentDescription = strings.shareChannelsList, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        if (unmatchedChannels.isNotEmpty()) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { saveFileLauncher.launch("channels_without_epg.txt") },
                                    modifier = Modifier.weight(1f).height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.saveChannelsList, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { showUnmatchedInSettings = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(1f).height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.viewUnmatchedChannels, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (epgGuide != null) strings.epgMatched(matchedCount, effectiveChannels.size) else strings.unmatchedEpgChannels,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (epgGuide == null || unmatchedChannels.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                if (epgGuide != null && effectiveChannels.isNotEmpty()) {
                                    Spacer(Modifier.width(6.dp))
                                    val pct = (matchedCount * 100) / effectiveChannels.size
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (pct == 100) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                                    ) {
                                        Text(
                                            "$pct%",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (pct == 100) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                if (epgGuide != null && unmatchedChannels.isEmpty()) strings.allEpgMatched else strings.unmatchedEpgCount(unmatchedChannels.size),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (unmatchedChannels.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = ::shareList, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.Share, contentDescription = strings.shareChannelsList, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                                OutlinedButton(
                                    onClick = { saveFileLauncher.launch("channels_without_epg.txt") },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.saveChannelsList, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { showUnmatchedInSettings = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.viewUnmatchedChannels, fontSize = 11.sp)
                                }
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

        // Time Shift
        SettingsGroup(title = strings.timeShift) {
            if (isCompact) {
                Column(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(strings.timeShift, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(strings.timeShiftDesc, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { if (epgTimeShiftHours > -23) onEpgTimeShiftChanged(epgTimeShiftHours - 1) },
                            enabled = epgTimeShiftHours > -23 && !loading,
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("-1 h", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.weight(1.2f).height(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val shiftDisplay = when {
                                    epgTimeShiftHours > 0 -> "+$epgTimeShiftHours h"
                                    epgTimeShiftHours < 0 -> "$epgTimeShiftHours h"
                                    else -> "0 h"
                                }
                                Text(shiftDisplay, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        FilledTonalButton(
                            onClick = { if (epgTimeShiftHours < 23) onEpgTimeShiftChanged(epgTimeShiftHours + 1) },
                            enabled = epgTimeShiftHours < 23 && !loading,
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("+1 h", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        if (epgTimeShiftHours != 0) {
                            TextButton(
                                onClick = { onEpgTimeShiftChanged(0) },
                                enabled = !loading,
                                contentPadding = PaddingValues(horizontal = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(strings.retry, fontSize = 11.sp)
                            }
                        }
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(strings.timeShift, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(strings.timeShiftDesc, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilledTonalButton(
                            onClick = { if (epgTimeShiftHours > -23) onEpgTimeShiftChanged(epgTimeShiftHours - 1) },
                            enabled = epgTimeShiftHours > -23 && !loading,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("-1 h", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.widthIn(min = 60.dp).height(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val shiftDisplay = when {
                                    epgTimeShiftHours > 0 -> "+$epgTimeShiftHours h"
                                    epgTimeShiftHours < 0 -> "$epgTimeShiftHours h"
                                    else -> "0 h"
                                }
                                Text(shiftDisplay, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        FilledTonalButton(
                            onClick = { if (epgTimeShiftHours < 23) onEpgTimeShiftChanged(epgTimeShiftHours + 1) },
                            enabled = epgTimeShiftHours < 23 && !loading,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("+1 h", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        if (epgTimeShiftHours != 0) {
                            TextButton(
                                onClick = { onEpgTimeShiftChanged(0) },
                                enabled = !loading,
                                contentPadding = PaddingValues(horizontal = 6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(strings.retry, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // EPG Days to Load (1 to 7 days)
        SettingsGroup(title = strings.epgLoadedDays) {
            if (isCompact) {
                Column(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(strings.epgLoadedDays, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(strings.epgLoadedDaysDesc, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { if (epgLoadedDays > 1) onEpgLoadedDaysChanged(epgLoadedDays - 1) },
                            enabled = epgLoadedDays > 1 && !loading,
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("-1", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.weight(1.5f).height(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                Text(
                                    strings.epgDaysCount(epgLoadedDays),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = { if (epgLoadedDays < 7) onEpgLoadedDaysChanged(epgLoadedDays + 1) },
                            enabled = epgLoadedDays < 7 && !loading,
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("+1", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(strings.epgLoadedDays, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(strings.epgLoadedDaysDesc, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilledTonalButton(
                            onClick = { if (epgLoadedDays > 1) onEpgLoadedDaysChanged(epgLoadedDays - 1) },
                            enabled = epgLoadedDays > 1 && !loading,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("-1", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.widthIn(min = 70.dp).height(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                Text(
                                    strings.epgDaysCount(epgLoadedDays),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = { if (epgLoadedDays < 7) onEpgLoadedDaysChanged(epgLoadedDays + 1) },
                            enabled = epgLoadedDays < 7 && !loading,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("+1", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalAppStrings.current
    val isTv = LocalFormFactor.current.isTelevision
    val checkUpdateFocusRequester = remember { FocusRequester() }
    val updateNowFocusRequester = remember { FocusRequester() }
    val installUpdateFocusRequester = remember { FocusRequester() }

    var checkingUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var upToDateNotice by remember { mutableStateOf(false) }
    var updateError by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableFloatStateOf(-1f) }
    var updateDownloaded by remember { mutableStateOf(false) }

    LaunchedEffect(isTv, checkingUpdate, updateInfo, updateDownloaded) {
        if (!isTv || checkingUpdate) return@LaunchedEffect
        delay(100)
        when {
            updateDownloaded -> runCatching { installUpdateFocusRequester.requestFocus() }
            updateInfo != null -> runCatching { updateNowFocusRequester.requestFocus() }
        }
    }

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

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShieldMark(Modifier.size(38.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(strings.appName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · Pride1922", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }

        // Disclaimer Alert
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(strings.contentDisclaimerTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        strings.contentDisclaimer,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Updates Group
        SettingsGroup(title = strings.checkForUpdates) {
            Column(Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(strings.checkForUpdates, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Installed: v${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                    if (downloadProgress < 0f && updateInfo == null) {
                        FilledTonalButton(
                            onClick = ::doCheckForUpdates,
                            enabled = !checkingUpdate,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .focusRequester(checkUpdateFocusRequester)
                                .tvFocusHighlight(
                                    shape = ButtonDefaults.filledTonalShape,
                                    scaleOnFocus = if (isTv) 1.05f else 1f
                                )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.checkForUpdates, fontSize = 11.sp)
                        }
                    }
                }

                if (checkingUpdate) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(strings.checkingForUpdates, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (upToDateNotice) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.upToDate, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    }
                }

                updateInfo?.let { info ->
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    strings.updateAvailableDesc("v${info.versionName}"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (info.releaseNotes.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    info.releaseNotes,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            if (updateDownloaded) {
                                Button(
                                    onClick = {
                                        val result = UpdateManager.retryPendingInstall(context)
                                        if (result.isFailure) {
                                            updateDownloaded = false
                                            downloadProgress = -1f
                                            updateError = result.exceptionOrNull()?.localizedMessage ?: "Installation failed"
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(34.dp)
                                        .focusRequester(installUpdateFocusRequester)
                                        .tvFocusHighlight(
                                            shape = ButtonDefaults.shape,
                                            scaleOnFocus = if (isTv) 1.04f else 1f
                                        ),
                                    contentPadding = PaddingValues(vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.installUpdate, fontSize = 11.sp)
                                }
                            } else if (downloadProgress >= 0f) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(strings.downloadingUpdate, fontSize = 11.sp)
                                        Text("${(downloadProgress * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height(3.dp))
                                    LinearProgressIndicator(
                                        progress = { downloadProgress },
                                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { startUpdate(info) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(34.dp)
                                        .focusRequester(updateNowFocusRequester)
                                        .tvFocusHighlight(
                                            shape = ButtonDefaults.shape,
                                            scaleOnFocus = if (isTv) 1.04f else 1f
                                        ),
                                    contentPadding = PaddingValues(vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.updateNow, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                if (updateError != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(strings.updateFailed(updateError.orEmpty()), color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }
            }
        }

        // Security & Open Source Group
        SettingsGroup(title = strings.securityTitle) {
            Column(Modifier.padding(10.dp)) {
                Text(
                    strings.securityDesc,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(strings.openSourceSoftware, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text(strings.ffmpegAttribution, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                    TextButton(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://cloud.ameyerocha.com/s/WLECDww4XQMHzdB"))
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(strings.viewSourceCode, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceMethodCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isTv = LocalFormFactor.current.isTelevision
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .tvFocusHighlight(
                shape = RoundedCornerShape(14.dp),
                scaleOnFocus = if (isTv) 1.03f else 1.0f
            )
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun AddPlaylistDialog(
    onDismiss: () -> Unit,
    onAddUrl: (String, String) -> Unit,
    onAddXtream: (String, String, String, String) -> Unit,
    onPickFile: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isTv = LocalFormFactor.current.isTelevision
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var mode by remember { mutableIntStateOf(-1) }
    var isConnecting by remember { mutableStateOf(false) }
    var connectionError by remember { mutableStateOf<String?>(null) }
    var pendingHttpServerUrl by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val serverFocusRequester = remember { FocusRequester() }
    val usernameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val sourceMethodFocusRequester = remember { FocusRequester() }
    val playlistNameFocusRequester = remember { FocusRequester() }

    fun hideKeyboard() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    LaunchedEffect(isTv, mode) {
        if (isTv) {
            delay(100)
            runCatching {
                if (mode < 0) sourceMethodFocusRequester.requestFocus()
                else playlistNameFocusRequester.requestFocus()
            }
        }
    }

    BackHandler(onBack = {
        hideKeyboard()
        if (isTv && mode >= 0) {
            connectionError = null
            pendingHttpServerUrl = null
            mode = -1
        } else {
            onDismiss()
        }
    })

    Dialog(
        onDismissRequest = { hideKeyboard() },
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .imePadding()
            .then(
                if (!isTv) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { hideKeyboard() }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(0.92f)
                .padding(16.dp)
                .then(
                    if (!isTv) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { hideKeyboard() }
                    } else Modifier
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    if (mode < 0) {
                        if (strings.languageTag == "pt-PT") "Onde está o seu conteúdo?" else "Where is your content?"
                    } else strings.addPlaylist,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (mode < 0) {
                        if (strings.languageTag == "pt-PT") "Escolha os dados que recebeu do fornecedor ou proprietário da lista." else "Choose the details supplied by your provider or playlist owner."
                    } else {
                        when (mode) {
                            0 -> if (strings.languageTag == "pt-PT") "Cole o endereço da lista ou escolha um ficheiro neste dispositivo." else "Paste the playlist address or choose a file on this device."
                            else -> if (strings.languageTag == "pt-PT") "Introduza os dados exatamente como foram fornecidos." else "Enter the credentials exactly as supplied."
                        }
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))

                if (mode < 0) {
                    SourceMethodCard(
                        icon = Icons.Default.Key,
                        title = strings.xtreamCodes,
                        description = if (strings.languageTag == "pt-PT") "Servidor, nome de utilizador e palavra-passe" else "Server, username and password",
                        modifier = Modifier.focusRequester(sourceMethodFocusRequester),
                        onClick = { mode = 2 }
                    )
                    Spacer(Modifier.height(10.dp))
                    SourceMethodCard(
                        icon = Icons.Default.Link,
                        title = if (strings.languageTag == "pt-PT") "Lista M3U" else "M3U playlist",
                        description = if (strings.languageTag == "pt-PT") "Adicionar por endereço ou ficheiro local" else "Add by web address or local file",
                        onClick = { mode = 0 }
                    )
                    Spacer(Modifier.height(18.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.End)
                            .tvFocusHighlight(
                                shape = ButtonDefaults.textShape,
                                scaleOnFocus = if (isTv) 1.05f else 1.0f
                            )
                    ) {
                        Text(strings.cancel)
                    }
                    return@Column
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.playlistName) },
                    placeholder = { Text(strings.playlistNamePlaceholder) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { serverFocusRequester.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(playlistNameFocusRequester)
                        .tvFocusHighlight(
                            shape = OutlinedTextFieldDefaults.shape,
                            scaleOnFocus = if (isTv) 1.02f else 1f
                        )
                )
                Spacer(Modifier.height(12.dp))
                if (mode == 0) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it; connectionError = null; pendingHttpServerUrl = null },
                        label = { Text(strings.playlistM3u) },
                        placeholder = { Text(strings.enterM3uUrl) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { hideKeyboard() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(serverFocusRequester)
                            .tvFocusHighlight(
                                shape = OutlinedTextFieldDefaults.shape,
                                scaleOnFocus = if (isTv) 1.02f else 1f
                            )
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            hideKeyboard()
                            onPickFile()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvFocusHighlight(
                                shape = ButtonDefaults.outlinedShape,
                                scaleOnFocus = if (isTv) 1.02f else 1.0f
                            )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.selectFile)
                    }
                } else {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text(strings.xtreamServer) },
                        placeholder = { Text("http://server.example:port") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { usernameFocusRequester.requestFocus() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(serverFocusRequester)
                            .tvFocusHighlight(
                                shape = OutlinedTextFieldDefaults.shape,
                                scaleOnFocus = if (isTv) 1.02f else 1f
                            )
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; connectionError = null },
                        label = { Text(strings.username) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(usernameFocusRequester)
                            .tvFocusHighlight(
                                shape = OutlinedTextFieldDefaults.shape,
                                scaleOnFocus = if (isTv) 1.02f else 1f
                            )
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; connectionError = null },
                        label = { Text(strings.password) },
                        singleLine = true,
                        visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { hideKeyboard() }),
                        trailingIcon = {
                            IconButton(
                                onClick = { showPassword = !showPassword },
                                modifier = Modifier.focusable(!isTv)
                            ) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFocusRequester)
                            .tvFocusHighlight(
                                shape = OutlinedTextFieldDefaults.shape,
                                scaleOnFocus = if (isTv) 1.02f else 1f
                            )
                    )
                }

                connectionError?.let { message ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            hideKeyboard()
                            connectionError = null
                            pendingHttpServerUrl = null
                            mode = -1
                        },
                        modifier = Modifier.tvFocusHighlight(
                            shape = ButtonDefaults.textShape,
                            scaleOnFocus = if (isTv) 1.05f else 1.0f
                        )
                    ) {
                        Text(strings.back)
                    }
                    Spacer(Modifier.width(8.dp))
                    if (mode == 0) {
                        Button(
                            onClick = {
                                hideKeyboard()
                                onAddUrl(name.trim(), url.trim())
                            },
                            enabled = url.startsWith("http://", true) || url.startsWith("https://", true),
                            modifier = Modifier.tvFocusHighlight(
                                shape = ButtonDefaults.shape,
                                scaleOnFocus = if (isTv) 1.05f else 1.0f
                            )
                        ) { Text(strings.addPlaylist) }
                    } else {
                        Button(
                            onClick = {
                                hideKeyboard()
                                val confirmedHttpUrl = pendingHttpServerUrl
                                if (confirmedHttpUrl != null) {
                                    onAddXtream(name.trim(), confirmedHttpUrl, username.trim(), password)
                                    return@Button
                                }
                                isConnecting = true
                                connectionError = null
                                scope.launch {
                                    val result = runCatching {
                                        withContext(Dispatchers.IO) {
                                            validateXtreamConnection(
                                                url.trim(),
                                                name.trim().ifBlank { "Xtream" },
                                                username.trim(),
                                                password
                                            )
                                        }
                                    }
                                    isConnecting = false
                                    result.onSuccess { check ->
                                        if (check.requiresHttpConfirmation) {
                                            pendingHttpServerUrl = check.resolvedServerUrl
                                            connectionError = if (strings.languageTag == "pt-PT") {
                                                "Este servidor não suporta HTTPS. Utilize HTTP apenas se confiar no fornecedor; as credenciais serão enviadas sem encriptação."
                                            } else {
                                                "This server does not support HTTPS. Use HTTP only if you trust the provider; credentials will be sent without encryption."
                                            }
                                        } else {
                                            onAddXtream(name.trim(), check.resolvedServerUrl, username.trim(), password)
                                        }
                                    }.onFailure { failure ->
                                        connectionError = xtreamConnectionErrorMessage(failure, strings)
                                    }
                                }
                            },
                            enabled = !isConnecting &&
                                    (url.startsWith("http://", true) || url.startsWith("https://", true)) &&
                                    username.isNotBlank() && password.isNotBlank(),
                            modifier = Modifier.tvFocusHighlight(
                                shape = ButtonDefaults.shape,
                                scaleOnFocus = if (isTv) 1.05f else 1.0f
                            )
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                if (pendingHttpServerUrl != null) {
                                    if (strings.languageTag == "pt-PT") "Utilizar HTTP" else "Use HTTP"
                                } else if (isConnecting) {
                                    if (strings.languageTag == "pt-PT") "A ligar…" else "Connecting…"
                                } else strings.connect
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

private data class XtreamConnectionCheck(
    val resolvedServerUrl: String,
    val requiresHttpConfirmation: Boolean
)

private suspend fun validateXtreamConnection(
    serverUrl: String,
    displayName: String,
    username: String,
    password: String
) : XtreamConnectionCheck {
    val source = ContentSource.Xtream(serverUrl, displayName, username, password)
    val client = XtreamClient(null, "connection_test", source)
    client.authenticate()
    val normalizedInput = XtreamClient.normalizeServerUrl(serverUrl)
    val resolved = client.resolvedServerUrl()
    return XtreamConnectionCheck(
        resolvedServerUrl = resolved,
        requiresHttpConfirmation = normalizedInput.startsWith("https://", true) &&
                resolved.startsWith("http://", true)
    )
}

private fun xtreamConnectionErrorMessage(failure: Throwable, strings: Strings): String {
    val causes = generateSequence(failure) { it.cause }.toList()
    val message = causes.firstNotNullOfOrNull { it.message?.takeIf(String::isNotBlank) }.orEmpty()
    val portuguese = strings.languageTag == "pt-PT"
    return when {
        message.contains("account is", ignoreCase = true) -> if (portuguese) {
            when {
                message.contains("expired", true) -> "A conta Xtream expirou."
                message.contains("disabled", true) -> "A conta Xtream está desativada."
                message.contains("banned", true) -> "A conta Xtream está bloqueada."
                else -> "A conta Xtream não está ativa."
            }
        } else {
            when {
                message.contains("expired", true) -> "The Xtream account has expired."
                message.contains("disabled", true) -> "The Xtream account is disabled."
                message.contains("banned", true) -> "The Xtream account is banned."
                else -> "The Xtream account is not active."
            }
        }
        message.contains("authentication failed", true) ->
            if (portuguese) "Nome de utilizador ou palavra-passe incorretos." else "Incorrect username or password."
        causes.any { it is java.net.UnknownHostException } ->
            if (portuguese) "Não foi possível encontrar o servidor." else "The server could not be found."
        causes.any { it is java.net.SocketTimeoutException } ->
            if (portuguese) "A ligação ao servidor excedeu o tempo limite." else "The server connection timed out."
        causes.any { it is javax.net.ssl.SSLException } ->
            if (portuguese) "Não foi possível estabelecer uma ligação segura ao servidor." else "A secure connection to the server could not be established."
        message.contains("HTTP 401") || message.contains("HTTP 403") ->
            if (portuguese) "O servidor recusou as credenciais ($message)." else "The server rejected the credentials ($message)."
        message.contains("HTTP ") ->
            if (portuguese) "O servidor recusou a ligação ($message)." else "The server rejected the connection ($message)."
        message.contains("Invalid Xtream response", true) || message.contains("JSON", true) ->
            if (portuguese) "O servidor devolveu uma resposta Xtream inválida." else "The server returned an invalid Xtream response."
        message.contains("catalogue is empty", true) ->
            if (portuguese) "A ligação foi estabelecida, mas o servidor não devolveu canais, filmes ou séries." else "Connected, but the server returned no channels, movies, or series."
        else -> if (portuguese) {
            "Não foi possível carregar o catálogo Xtream. Verifique o servidor e as credenciais."
        } else {
            "The Xtream catalogue could not be loaded. Check the server and credentials."
        }
    }
}

@Composable
private fun EditPlaylistDialog(
    profile: PlaylistProfile,
    onDismiss: () -> Unit,
    onConfirm: (String, ContentSource, SourceRole) -> Unit
) {
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val serverFocusRequester = remember { FocusRequester() }
    val usernameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    var name by remember(profile.id) { mutableStateOf(profile.name) }
    var role by remember(profile.id) { mutableStateOf(profile.role) }
    var roleDropdownExpanded by remember { mutableStateOf(false) }

    val initialSource = profile.source
    var serverUrl by remember(profile.id) {
        mutableStateOf(
            when (initialSource) {
                is ContentSource.Xtream -> initialSource.value
                is ContentSource.RemoteUrl -> initialSource.value
                is ContentSource.LocalFile -> initialSource.value
            }
        )
    }
    var username by remember(profile.id) {
        mutableStateOf((initialSource as? ContentSource.Xtream)?.username ?: "")
    }
    var password by remember(profile.id) {
        mutableStateOf((initialSource as? ContentSource.Xtream)?.password ?: "")
    }
    var showPassword by remember { mutableStateOf(false) }
    var isConnecting by remember(profile.id) { mutableStateOf(false) }
    var connectionError by remember(profile.id) { mutableStateOf<String?>(null) }
    var pendingHttpServerUrl by remember(profile.id) { mutableStateOf<String?>(null) }

    val canSave = when (initialSource) {
        is ContentSource.Xtream -> name.isNotBlank() &&
                (serverUrl.startsWith("http://", true) || serverUrl.startsWith("https://", true)) &&
                username.isNotBlank() && password.isNotBlank()
        is ContentSource.RemoteUrl -> name.isNotBlank() &&
                (serverUrl.startsWith("http://", true) || serverUrl.startsWith("https://", true))
        is ContentSource.LocalFile -> name.isNotBlank()
    }

    fun hideKeyboard() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    BackHandler(onBack = {
        hideKeyboard()
        onDismiss()
    })

    Dialog(
        onDismissRequest = { hideKeyboard() },
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .imePadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                hideKeyboard()
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(0.92f)
                .padding(16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { hideKeyboard() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    strings.editPlaylist,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.playlistName) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            if (initialSource is ContentSource.LocalFile) hideKeyboard()
                            else serverFocusRequester.requestFocus()
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (initialSource is ContentSource.Xtream) {
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it; connectionError = null; pendingHttpServerUrl = null },
                        label = { Text(strings.xtreamServer) },
                        placeholder = { Text("https://server.example:port") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { usernameFocusRequester.requestFocus() }),
                        modifier = Modifier.fillMaxWidth().focusRequester(serverFocusRequester)
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; connectionError = null },
                        label = { Text(strings.username) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
                        modifier = Modifier.fillMaxWidth().focusRequester(usernameFocusRequester)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; connectionError = null },
                        label = { Text(strings.password) },
                        singleLine = true,
                        visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { hideKeyboard() }),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().focusRequester(passwordFocusRequester)
                    )
                } else if (initialSource is ContentSource.RemoteUrl) {
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text(strings.playlistM3u) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { hideKeyboard() }),
                        modifier = Modifier.fillMaxWidth().focusRequester(serverFocusRequester)
                    )
                }

                connectionError?.let { message ->
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Role Dropdown Box
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = sourceRoleLabel(role, strings),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings.sourceRole) },
                        trailingIcon = {
                            IconButton(onClick = {
                                hideKeyboard()
                                roleDropdownExpanded = !roleDropdownExpanded
                            }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                hideKeyboard()
                                roleDropdownExpanded = true
                            }
                    )
                    DropdownMenu(
                        expanded = roleDropdownExpanded,
                        onDismissRequest = { roleDropdownExpanded = false }
                    ) {
                        SourceRole.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(sourceRoleLabel(option, strings)) },
                                onClick = {
                                    role = option
                                    roleDropdownExpanded = false
                                },
                                trailingIcon = if (role == option) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        hideKeyboard()
                        onDismiss()
                    }) {
                        Text(strings.cancel)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            hideKeyboard()
                            val updatedSource = when (initialSource) {
                                is ContentSource.Xtream -> ContentSource.Xtream(
                                    value = serverUrl.trim(),
                                    displayName = name.trim(),
                                    username = username.trim(),
                                    password = password
                                )
                                is ContentSource.RemoteUrl -> ContentSource.RemoteUrl(
                                    value = serverUrl.trim(),
                                    displayName = name.trim()
                                )
                                is ContentSource.LocalFile -> ContentSource.LocalFile(
                                    value = initialSource.value,
                                    displayName = name.trim()
                                )
                            }
                            if (updatedSource is ContentSource.Xtream) {
                                val confirmedHttpUrl = pendingHttpServerUrl
                                if (confirmedHttpUrl != null) {
                                    onConfirm(
                                        name.trim(),
                                        updatedSource.copy(value = confirmedHttpUrl),
                                        role
                                    )
                                    return@Button
                                }
                                isConnecting = true
                                connectionError = null
                                scope.launch {
                                    val result = runCatching {
                                        withContext(Dispatchers.IO) {
                                            validateXtreamConnection(
                                                updatedSource.value,
                                                name.trim(),
                                                updatedSource.username,
                                                updatedSource.password
                                            )
                                        }
                                    }
                                    isConnecting = false
                                    result.onSuccess { check ->
                                        if (check.requiresHttpConfirmation) {
                                            pendingHttpServerUrl = check.resolvedServerUrl
                                            connectionError = if (strings.languageTag == "pt-PT") {
                                                "Este servidor não suporta HTTPS. Utilize HTTP apenas se confiar no fornecedor; as credenciais serão enviadas sem encriptação."
                                            } else {
                                                "This server does not support HTTPS. Use HTTP only if you trust the provider; credentials will be sent without encryption."
                                            }
                                        } else {
                                            onConfirm(
                                                name.trim(),
                                                updatedSource.copy(value = check.resolvedServerUrl),
                                                role
                                            )
                                        }
                                    }.onFailure { failure ->
                                        connectionError = xtreamConnectionErrorMessage(failure, strings)
                                    }
                                }
                            } else {
                                onConfirm(name.trim(), updatedSource, role)
                            }
                        },
                        enabled = canSave && !isConnecting
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            if (pendingHttpServerUrl != null) {
                                if (strings.languageTag == "pt-PT") "Utilizar HTTP" else "Use HTTP"
                            } else if (isConnecting) {
                                if (strings.languageTag == "pt-PT") "A verificar…" else "Checking…"
                            } else strings.save
                        )
                    }
                }
            }
        }
    }
    }
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = {
            focusManager.clearFocus()
            keyboardController?.hide()
            onDismiss()
        },
        properties = DialogProperties(dismissOnClickOutside = false),
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
    val isTv = LocalFormFactor.current.isTelevision
    val primaryButtonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (isTv) {
            delay(100)
            try { primaryButtonFocusRequester.requestFocus() } catch (_: Throwable) {}
        }
    }

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
                        Button(
                            onClick = onInstallUpdate,
                            modifier = Modifier
                                .focusRequester(primaryButtonFocusRequester)
                                .tvFocusHighlight(
                                    shape = ButtonDefaults.shape,
                                    scaleOnFocus = if (isTv) 1.05f else 1.0f
                                )
                        ) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.installUpdate)
                        }
                    } else if (progress < 0f) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.tvFocusHighlight(
                                shape = ButtonDefaults.textShape,
                                scaleOnFocus = if (isTv) 1.05f else 1.0f
                            )
                        ) {
                            Text(strings.later)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = onStartUpdate,
                            modifier = Modifier
                                .focusRequester(primaryButtonFocusRequester)
                                .tvFocusHighlight(
                                    shape = ButtonDefaults.shape,
                                    scaleOnFocus = if (isTv) 1.05f else 1.0f
                                )
                        ) {
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
private fun ManageCategoriesDialog(
    categories: List<String>,
    groupCounts: Map<String, Int>,
    hiddenCategories: Set<String>,
    categoryOrder: List<String>,
    onToggleCategoryHidden: (String) -> Unit,
    onMoveCategory: (String, Boolean) -> Unit,
    onUnhideAll: () -> Unit,
    onHideAll: () -> Unit,
    onResetOrder: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    val listState = rememberLazyListState()
    var searchQuery by remember { mutableStateOf("") }

    fun hideKeyboard() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
        view.rootView?.let { imm?.hideSoftInputFromWindow(it.windowToken, 0) }
        view.findFocus()?.let { imm?.hideSoftInputFromWindow(it.windowToken, 0) }
        val dialogWin = (view.parent as? DialogWindowProvider)?.window
        if (dialogWin != null) {
            WindowInsetsControllerCompat(dialogWin, dialogWin.decorView).hide(WindowInsetsCompat.Type.ime())
            dialogWin.currentFocus?.clearFocus()
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            hideKeyboard()
        }
    }

    val orderedCategories = remember(categories, categoryOrder) {
        val orderMap = categoryOrder.withIndex().associate { it.value to it.index }
        categories.sortedWith(compareBy({ orderMap[it] ?: Int.MAX_VALUE }, { it }))
    }

    val filteredCategories = remember(orderedCategories, searchQuery) {
        if (searchQuery.isBlank()) orderedCategories
        else orderedCategories.filter { it.contains(searchQuery.trim(), ignoreCase = true) }
    }

    BackHandler(onBack = {
        hideKeyboard()
        onDismiss()
    })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { hideKeyboard() }
            .pointerInput(Unit) {
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press) {
                            hideKeyboard()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .padding(8.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { hideKeyboard() }
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { hideKeyboard() }
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { hideKeyboard() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            strings.manageCategories,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${categories.size - hiddenCategories.size}/${categories.size} ${strings.active.lowercase()}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = strings.close)
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.search) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    ),
                    trailingIcon = if (searchQuery.isNotBlank()) {
                        {
                            IconButton(onClick = {
                                searchQuery = ""
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            hideKeyboard()
                            onUnhideAll()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.showAllCategories, fontSize = 11.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            hideKeyboard()
                            onHideAll()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.hideAllCategories, fontSize = 11.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            hideKeyboard()
                            onResetOrder()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.resetCategoryOrder, fontSize = 11.sp, maxLines = 1)
                    }
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(6.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(filteredCategories, key = { _, cat -> "manage-cat-$cat" }) { index, category ->
                        val isHidden = category in hiddenCategories
                        val count = groupCounts[category] ?: 0
                        Surface(
                            onClick = {
                                hideKeyboard()
                                onToggleCategoryHidden(category)
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isHidden) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        hideKeyboard()
                                        onToggleCategoryHidden(category)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isHidden) strings.showCategory else strings.hideCategory,
                                        tint = if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        category,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        strings.channelsCount(count),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                    )
                                }
                                if (searchQuery.isBlank()) {
                                    IconButton(
                                        onClick = {
                                            hideKeyboard()
                                            onMoveCategory(category, true)
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = strings.moveSourceUp, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            hideKeyboard()
                                            onMoveCategory(category, false)
                                        },
                                        enabled = index < filteredCategories.size - 1,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = strings.moveSourceDown, modifier = Modifier.size(16.dp))
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

private fun copyToClipboard(context: Context, label: String, text: String) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
    } catch (_: Throwable) {}
}

private fun saveToDownloadsFolder(context: Context, filename: String, text: String): Boolean {
    val bytes = text.toByteArray(Charsets.UTF_8)
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(bytes)
                    os.flush()
                }
                true
            } else false
        } else {
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename)
            file.writeBytes(bytes)
            true
        }
    } catch (e: Throwable) {
        Log.e("FortressTV", "Failed to save to Downloads: ${e.message}", e)
        false
    }
}

private fun writeTextToUri(context: Context, uri: Uri, text: String): Boolean {
    val bytes = text.toByteArray(Charsets.UTF_8)
    Log.d("FortressTV", "Writing ${bytes.size} bytes (${text.lines().size} lines) to $uri")

    var success = false

    // Try ParcelFileDescriptor with truncate
    try {
        context.contentResolver.openFileDescriptor(uri, "rwt")?.use { pfd ->
            FileOutputStream(pfd.fileDescriptor).use { fos ->
                fos.channel.truncate(0)
                fos.write(bytes)
                fos.flush()
                try { fos.fd.sync() } catch (_: Throwable) {}
            }
            Log.d("FortressTV", "Successfully wrote ${bytes.size} bytes via ParcelFileDescriptor rwt")
            success = true
        }
    } catch (_: Throwable) {
        try {
            context.contentResolver.openFileDescriptor(uri, "w")?.use { pfd ->
                FileOutputStream(pfd.fileDescriptor).use { fos ->
                    fos.write(bytes)
                    fos.flush()
                    try { fos.fd.sync() } catch (_: Throwable) {}
                }
                Log.d("FortressTV", "Successfully wrote ${bytes.size} bytes via ParcelFileDescriptor w")
                success = true
            }
        } catch (e: Throwable) {
            Log.w("FortressTV", "ParcelFileDescriptor write failed: ${e.message}")
        }
    }

    if (!success) {
        try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { os ->
                os.write(bytes)
                os.flush()
                Log.d("FortressTV", "Successfully wrote ${bytes.size} bytes via openOutputStream wt")
                success = true
            }
        } catch (_: Throwable) {
            try {
                context.contentResolver.openOutputStream(uri, "w")?.use { os ->
                    os.write(bytes)
                    os.flush()
                    Log.d("FortressTV", "Successfully wrote ${bytes.size} bytes via openOutputStream w")
                    success = true
                }
            } catch (e: Throwable) {
                Log.e("FortressTV", "openOutputStream write failed: ${e.message}", e)
            }
        }
    }

    try {
        context.contentResolver.notifyChange(uri, null)
    } catch (_: Throwable) {}

    // Auto-save to tablet's local Downloads folder and clipboard
    saveToDownloadsFolder(context, "channels_without_epg.txt", text)
    copyToClipboard(context, "Unmatched EPG Channels", text)

    return success
}

private fun generateUnmatchedChannelsText(channels: List<PlaylistChannel>): String {
    val sb = StringBuilder()
    sb.appendLine("================================================================================")
    sb.appendLine("FortressTV - Channels Without EPG Match")
    sb.appendLine("Total Channels: ${channels.size}")
    sb.appendLine("Generated: ${DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now())}")
    sb.appendLine("================================================================================")
    sb.appendLine()
    sb.appendLine("Format: [Channel Name] | Group: [Category] | EPG ID: [tvg-id]")
    sb.appendLine("--------------------------------------------------------------------------------")
    channels.forEachIndexed { index, ch ->
        val group = ch.group?.takeIf { it.isNotBlank() } ?: "No Group"
        val tvgId = ch.tvgId?.takeIf { it.isNotBlank() } ?: "No tvg-id"
        sb.appendLine("${index + 1}. ${ch.name} | Group: $group | EPG ID: $tvgId")
    }
    sb.appendLine("--------------------------------------------------------------------------------")
    return sb.toString()
}

@Composable
private fun UnmatchedEpgDialog(
    unmatchedChannels: List<PlaylistChannel>,
    onDismiss: () -> Unit,
    onSelectChannel: (PlaylistChannel) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentUnmatched by rememberUpdatedState(unmatchedChannels)
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            val channelsToExport = currentUnmatched
            val text = generateUnmatchedChannelsText(channelsToExport)
            val success = writeTextToUri(context, uri, text)
            if (success) {
                Toast.makeText(context, "${strings.channelsListSaved} (${channelsToExport.size})", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Error: could not write to selected location", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun shareList() {
        val text = generateUnmatchedChannelsText(currentUnmatched)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_TITLE, strings.unmatchedEpgChannels)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, strings.shareChannelsList)
        context.startActivity(shareIntent)
    }

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
                    IconButton(onClick = ::shareList) {
                        Icon(Icons.Default.Share, contentDescription = strings.shareChannelsList)
                    }
                    IconButton(onClick = { saveFileLauncher.launch("channels_without_epg.txt") }) {
                        Icon(Icons.Default.Download, contentDescription = strings.saveChannelsList)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { saveFileLauncher.launch("channels_without_epg.txt") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.saveChannelsList, fontSize = 13.sp, maxLines = 1)
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(strings.close, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

enum class PinDialogMode {
    SET,
    VERIFY,
    REMOVE,
    UNLOCK_ADULT,
    UNLOCK_SETTINGS
}

@Composable
private fun PinEntryDialog(
    mode: PinDialogMode,
    currentPin: String?,
    onSuccess: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var enteredPin by remember { mutableStateOf("") }
    var firstEnteredPin by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val title = when (mode) {
        PinDialogMode.SET -> if (firstEnteredPin == null) strings.enterNewPin else strings.confirmParentalPin
        PinDialogMode.VERIFY, PinDialogMode.REMOVE -> strings.enterParentalPin
        PinDialogMode.UNLOCK_ADULT -> strings.unlockAdultContent
        PinDialogMode.UNLOCK_SETTINGS -> strings.unlockSettings
    }

    fun onDigit(digit: String) {
        if (enteredPin.length < 4) {
            errorMessage = null
            val newPin = enteredPin + digit
            enteredPin = newPin
            if (newPin.length == 4) {
                when (mode) {
                    PinDialogMode.SET -> {
                        if (firstEnteredPin == null) {
                            firstEnteredPin = newPin
                            enteredPin = ""
                        } else {
                            if (newPin == firstEnteredPin) {
                                onSuccess(newPin)
                            } else {
                                errorMessage = strings.pinsDoNotMatch
                                enteredPin = ""
                                firstEnteredPin = null
                            }
                        }
                    }
                    PinDialogMode.VERIFY, PinDialogMode.UNLOCK_ADULT, PinDialogMode.UNLOCK_SETTINGS -> {
                        if (newPin == currentPin) {
                            onSuccess(newPin)
                        } else {
                            errorMessage = strings.pinIncorrect
                            enteredPin = ""
                        }
                    }
                    PinDialogMode.REMOVE -> {
                        if (newPin == currentPin) {
                            onSuccess(null)
                        } else {
                            errorMessage = strings.pinIncorrect
                            enteredPin = ""
                        }
                    }
                }
            }
        }
    }

    fun onBackspace() {
        if (enteredPin.isNotEmpty()) {
            errorMessage = null
            enteredPin = enteredPin.dropLast(1)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                // PIN dots indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    Spacer(Modifier.height(20.dp))
                }

                // Numeric Keypad
                val buttons = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "⌫")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    buttons.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            row.forEach { key ->
                                Surface(
                                    onClick = {
                                        when (key) {
                                            "C" -> {
                                                enteredPin = ""
                                                errorMessage = null
                                            }
                                            "⌫" -> onBackspace()
                                            else -> onDigit(key)
                                        }
                                    },
                                    shape = CircleShape,
                                    color = if (key == "C" || key == "⌫") MaterialTheme.colorScheme.surfaceVariant
                                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (key == "⌫") {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Backspace,
                                                contentDescription = "Backspace",
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        } else {
                                            Text(
                                                key,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
private fun CreateBackupDialog(
    onDismiss: () -> Unit,
    onConfirm: (password: String) -> Unit
) {
    val strings = LocalAppStrings.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    val focusRequester = remember { FocusRequester() }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun hideKeyboard() {
        runCatching { focusRequester.requestFocus() }
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        view.clearFocus()
        val dialogWin = (view.parent as? DialogWindowProvider)?.window
            ?: (view.context as? ComponentActivity)?.window
        if (dialogWin != null) {
            WindowInsetsControllerCompat(dialogWin, dialogWin.decorView)
                .hide(WindowInsetsCompat.Type.ime())
        }
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
        imm?.hideSoftInputFromWindow(view.rootView.windowToken, 0)
    }

    BackHandler(onBack = {
        hideKeyboard()
        onDismiss()
    })

    Dialog(
        onDismissRequest = {
            hideKeyboard()
        },
        properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .focusRequester(focusRequester)
                .focusable()
                .imePadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    hideKeyboard()
                    onDismiss()
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth(0.92f)
                    .padding(16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* consume clicks on card */ }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        strings.createBackup,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        strings.backupPasswordExplanation,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text(strings.backupPassword) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorMessage = null
                        },
                        label = { Text(strings.confirmBackupPassword) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                hideKeyboard()
                                when {
                                    password.isBlank() -> errorMessage = strings.backupPasswordRequired
                                    password.length < 4 -> errorMessage = strings.backupPasswordMinLength
                                    password != confirmPassword -> errorMessage = strings.backupPasswordMismatch
                                    else -> onConfirm(password)
                                }
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMessage != null) {
                        Text(
                            errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                hideKeyboard()
                                onDismiss()
                            }
                        ) {
                            Text(strings.cancel)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                hideKeyboard()
                                when {
                                    password.isBlank() -> errorMessage = strings.backupPasswordRequired
                                    password.length < 4 -> errorMessage = strings.backupPasswordMinLength
                                    password != confirmPassword -> errorMessage = strings.backupPasswordMismatch
                                    else -> onConfirm(password)
                                }
                            }
                        ) {
                            Text(strings.exportBackupAction)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RestoreBackupPasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (password: String, merge: Boolean) -> Unit,
    errorMessage: String?,
    isRestoring: Boolean
) {
    val strings = LocalAppStrings.current
    val isTv = LocalFormFactor.current.isTelevision
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    val focusRequester = remember { FocusRequester() }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var mergeMode by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val displayError = errorMessage ?: localError
    val passwordFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isTv) {
        if (isTv) {
            delay(150)
            try {
                passwordFocusRequester.requestFocus()
            } catch (_: Throwable) {}
        }
    }

    fun hideKeyboard() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        view.clearFocus()
        val dialogWin = (view.parent as? DialogWindowProvider)?.window
            ?: (view.context as? ComponentActivity)?.window
        if (dialogWin != null) {
            WindowInsetsControllerCompat(dialogWin, dialogWin.decorView)
                .hide(WindowInsetsCompat.Type.ime())
        }
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
        imm?.hideSoftInputFromWindow(view.rootView.windowToken, 0)
    }

    BackHandler(onBack = {
        if (!isRestoring) {
            hideKeyboard()
            onDismiss()
        }
    })

    Dialog(
        onDismissRequest = {
            hideKeyboard()
        },
        properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .imePadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (!isRestoring) {
                        hideKeyboard()
                        onDismiss()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .fillMaxWidth(0.92f)
                    .padding(16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* consume clicks on card */ }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        strings.restoreBackup,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        strings.enterBackupPassword,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            localError = null
                        },
                        label = { Text(strings.backupPassword) },
                        singleLine = true,
                        enabled = !isRestoring,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                hideKeyboard()
                                if (password.isBlank()) {
                                    localError = strings.backupPasswordRequired
                                } else {
                                    onConfirm(password, mergeMode)
                                }
                            }
                        ),
                        trailingIcon = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier.focusable(!isTv)
                            ) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFocusRequester)
                            .tvFocusHighlight(
                                shape = OutlinedTextFieldDefaults.shape,
                                scaleOnFocus = if (isTv) 1.02f else 1.0f
                            )
                    )

                    Text(
                        strings.restoreModePrompt,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .tvFocusHighlight(
                                        shape = RoundedCornerShape(8.dp),
                                        scaleOnFocus = if (isTv) 1.02f else 1.0f
                                    )
                                    .clickable(enabled = !isRestoring) {
                                        hideKeyboard()
                                        mergeMode = false
                                    }
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = !mergeMode,
                                    onClick = null,
                                    modifier = Modifier.focusable(false)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(strings.restoreModeReplace, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(strings.restoreModeReplaceDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .tvFocusHighlight(
                                        shape = RoundedCornerShape(8.dp),
                                        scaleOnFocus = if (isTv) 1.02f else 1.0f
                                    )
                                    .clickable(enabled = !isRestoring) {
                                        hideKeyboard()
                                        mergeMode = true
                                    }
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = mergeMode,
                                    onClick = null,
                                    modifier = Modifier.focusable(false)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(strings.restoreModeMerge, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(strings.restoreModeMergeDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    if (isRestoring) {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(strings.restoreBackup, fontSize = 13.sp)
                        }
                    }

                    if (displayError != null && !isRestoring) {
                        Text(
                            displayError,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                hideKeyboard()
                                onDismiss()
                            },
                            enabled = !isRestoring,
                            modifier = Modifier.tvFocusHighlight(
                                shape = ButtonDefaults.textShape,
                                scaleOnFocus = if (isTv) 1.05f else 1.0f
                            )
                        ) {
                            Text(strings.cancel)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                hideKeyboard()
                                if (password.isBlank()) {
                                    localError = strings.backupPasswordRequired
                                } else {
                                    onConfirm(password, mergeMode)
                                }
                            },
                            enabled = !isRestoring,
                            modifier = Modifier.tvFocusHighlight(
                                shape = ButtonDefaults.shape,
                                scaleOnFocus = if (isTv) 1.05f else 1.0f
                            )
                        ) {
                            Text(strings.restoreBackupAction)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 1024, heightDp = 600)
@Composable
private fun WelcomePreview() = FortressTVTheme {
    WelcomeScreen(false, true, null, onRetry = null, onAddSource = {}, onRestoreBackup = {})
}
