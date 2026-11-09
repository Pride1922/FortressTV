package com.fortresstv.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.BatteryManager
import android.util.Rational
import kotlin.math.roundToInt
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.zIndex
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.foundation.shape.CircleShape
import com.fortresstv.player.playlist.PlaylistProfile
import com.fortresstv.player.update.UpdateManager
import com.fortresstv.player.update.AppUpdateInfo
import com.fortresstv.player.BuildConfig
import com.fortresstv.player.ui.AppLanguage
import com.fortresstv.player.account.AccountRepository
import com.fortresstv.player.account.AccountTab
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ChildCare
import com.fortresstv.player.source.CloudSourceSyncService
import com.fortresstv.player.profile.HouseholdProfile
import com.fortresstv.player.profile.HouseholdPinDialogMode
import com.fortresstv.player.profile.ParentalControls
import com.fortresstv.player.profile.ParentalPinDialog
import com.fortresstv.player.profile.ProfileAvatar
import com.fortresstv.player.profile.ProfileRepository
import com.fortresstv.player.profile.ProfileSelectionDialog
import com.fortresstv.player.profile.ProfilesTab
import com.fortresstv.player.ui.LocalAppStrings
import com.fortresstv.player.ui.getStringsFor
import com.fortresstv.player.ui.Strings
import com.fortresstv.player.ui.AppFormFactor
import com.fortresstv.player.ui.FormFactorDetector
import com.fortresstv.player.ui.LocalFormFactor
import com.fortresstv.player.ui.tvFocusHighlight
import com.fortresstv.player.ui.theme.TabularDigitsTextStyle
import com.fortresstv.player.ui.TvPlayerAction
import com.fortresstv.player.ui.TvPlayerKeyDispatcher
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.material.icons.filled.BatteryFull
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
import androidx.compose.ui.text.input.KeyboardType
import java.time.LocalTime
import com.fortresstv.player.epg.EpgChannelMapping
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
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Speed
import com.fortresstv.player.transfer.LocalTransferServer
import com.fortresstv.player.transfer.TransferKind
import com.fortresstv.player.transfer.TransferPreview
import com.fortresstv.player.transfer.TransferRequestResult
import com.fortresstv.player.transfer.TransferSessionState
import com.fortresstv.player.transfer.TransferSourcePayload
import com.fortresstv.player.transfer.SendToTvDialog
import com.fortresstv.player.transfer.DirectTransferLink
import com.fortresstv.player.transfer.DiscoveredFortressTv
import com.fortresstv.player.transfer.TvNsdManager
import com.fortresstv.player.transfer.sanitizeHostForDisplay
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
import kotlinx.coroutines.isActive
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
import com.fortresstv.player.player.FortressPlayerFactory
import com.fortresstv.player.player.LocalPlaybackMode
import com.fortresstv.player.player.PlaybackContentType
import com.fortresstv.player.player.PlaybackMode
import com.fortresstv.player.player.PlaybackTransferProgress
import com.fortresstv.player.player.PlaybackFrameProgress
import com.fortresstv.player.player.ReadyFreezePolicy
import com.fortresstv.player.player.StallDecision
import com.fortresstv.player.player.BufferingSample
import com.fortresstv.player.player.awaitBufferingStall
import com.fortresstv.player.player.isPlaybackNetworkAvailable
import com.fortresstv.player.player.rememberPlaybackNetworkGeneration
import com.fortresstv.player.player.PlaybackTrackItem
import com.fortresstv.player.player.PlaybackVideoTrackItem
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
import com.fortresstv.player.source.StartupDestination
import com.fortresstv.player.library.VodProgressEntry
import com.fortresstv.player.library.VodProgressKind
import com.fortresstv.player.library.VodProgressStore
import com.fortresstv.player.library.CatchupProgressKey
import com.fortresstv.player.library.profileProgressNamespace
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
import com.fortresstv.player.subtitles.OpenSubtitlesClient
import com.fortresstv.player.subtitles.OpenSubtitlesItem
import com.fortresstv.player.subtitles.OpenSubtitlesSearchDialog
import com.fortresstv.player.subtitles.OpenSubtitlesSettings
import com.fortresstv.player.subtitles.OpenSubtitlesSettingsStore
import com.fortresstv.player.subtitles.SubtitleDiskCache
import com.fortresstv.player.subtitles.openSubtitlesLanguageQuery
import com.fortresstv.player.subtitles.shouldAutoSearchOpenSubtitles
import com.fortresstv.player.tmdb.LocalTmdbSettings
import com.fortresstv.player.tmdb.TmdbClient
import com.fortresstv.player.tmdb.TmdbLanguages
import com.fortresstv.player.tmdb.TmdbMetadata
import com.fortresstv.player.tmdb.TmdbMetadataResolver
import com.fortresstv.player.tmdb.TmdbSeasonMetadata
import com.fortresstv.player.tmdb.TmdbSettings
import com.fortresstv.player.omdb.LocalOmdbSettings
import com.fortresstv.player.omdb.OmdbAwardsRow
import com.fortresstv.player.omdb.OmdbClient
import com.fortresstv.player.omdb.OmdbMetadata
import com.fortresstv.player.omdb.OmdbMetadataResolver
import com.fortresstv.player.omdb.OmdbRatingsRow
import com.fortresstv.player.omdb.OmdbSettings
import com.fortresstv.player.omdb.OmdbSettingsStore
import com.fortresstv.player.introdb.EpisodeSegments
import com.fortresstv.player.introdb.LocalTheIntroDbSettings
import com.fortresstv.player.introdb.MediaSegment
import com.fortresstv.player.introdb.SegmentType
import com.fortresstv.player.introdb.TheIntroDbClient
import com.fortresstv.player.introdb.TheIntroDbSettings
import com.fortresstv.player.introdb.TheIntroDbSettingsStore
import com.fortresstv.player.tmdb.TmdbSettingsStore
import androidx.media3.common.MimeTypes
import com.fortresstv.player.ui.theme.FortressTVTheme
import com.fortresstv.player.ui.theme.FortressBackgroundBrush
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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
    private companion object {
        const val PIP_DISMISS_CONFIRMATION_DELAY_MS = 1_500L
        const val INACTIVITY_LOCK_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutes
        var lastBackgroundTimestamp = 0L
    }

    var pictureInPictureMode by mutableStateOf(false)
        private set

    private var pictureInPictureEnabled = true
    private var fullscreenVideoPlaying = false
    private val pictureInPictureHandler = Handler(Looper.getMainLooper())
    private var onPictureInPictureDismissed: (() -> Unit)? = null
    private var isActivityResumed = false
    private var transferDeepLink by mutableStateOf<String?>(null)
    private var foregroundGeneration by mutableIntStateOf(0)
    private var inactivityLockTriggered by mutableStateOf(false)

    internal var sleepTimerState by mutableStateOf(PlaybackSleepTimerState())
        private set
    private val sleepTimerHandler = Handler(Looper.getMainLooper())
    private val sleepTimerPlayers = mutableMapOf<Player, () -> Unit>()
    internal val playbackSleepTimer = PlaybackSleepTimer(
        elapsedRealtime = android.os.SystemClock::elapsedRealtime,
        schedule = { delayMs, action ->
            val runnable = Runnable { action() }
            sleepTimerHandler.postDelayed(runnable, delayMs)
            val cancel: () -> Unit = { sleepTimerHandler.removeCallbacks(runnable) }
            cancel
        },
        onStateChanged = { sleepTimerState = it },
        onExpired = {
            // Pause immediately. The visible player asks whether the user is still watching.
            updatePictureInPictureState(enabled = false, fullscreenPlaying = false)
            sleepTimerPlayers.keys.toList().forEach { it.pause() }
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        },
        onAutoStop = {
            val registrations = sleepTimerPlayers.toList()
            registrations.forEach { (player, _) -> player.stop() }
            registrations.forEach { (_, dismiss) -> dismiss() }
        }
    )

    internal fun registerSleepTimerPlayer(player: Player, onAutoStop: () -> Unit): () -> Unit {
        val listener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                playbackSleepTimer.checkDeadline()
                if (playWhenReady && playbackSleepTimer.expired) {
                    player.pause()
                }
            }
        }
        sleepTimerPlayers[player] = onAutoStop
        player.addListener(listener)
        playbackSleepTimer.checkDeadline()
        if (playbackSleepTimer.expired) player.pause()
        return {
            player.removeListener(listener)
            sleepTimerPlayers.remove(player)
        }
    }

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
        pictureInPictureEnabled = enabled && !playbackSleepTimer.expired
        fullscreenVideoPlaying = fullscreenPlaying && !playbackSleepTimer.expired
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setPictureInPictureParams(
                PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            setAutoEnterEnabled(pictureInPictureEnabled && fullscreenVideoPlaying)
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
            // Expanding PiP can report "not in PiP" well before the activity regains
            // focus (observed at roughly 700 ms on Xiaomi tablets). Give expansion a
            // safe window to cancel this callback; a real dismissal remains stopped
            // promptly even when the platform delays onStop.
            pictureInPictureHandler.postDelayed(
                confirmPictureInPictureDismissed,
                PIP_DISMISS_CONFIRMATION_DELAY_MS
            )
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
        playbackSleepTimer.checkDeadline()
        if (lastBackgroundTimestamp > 0L) {
            val elapsed = android.os.SystemClock.elapsedRealtime() - lastBackgroundTimestamp
            lastBackgroundTimestamp = 0L
            if (elapsed > INACTIVITY_LOCK_TIMEOUT_MS) {
                inactivityLockTriggered = true
            }
        }
        foregroundGeneration++
        isActivityResumed = true
        pictureInPictureHandler.removeCallbacks(confirmPictureInPictureDismissed)
        UpdateManager.resumePendingInstall(this)
    }

    override fun onPause() {
        isActivityResumed = false
        super.onPause()
        if (!pictureInPictureMode) {
            lastBackgroundTimestamp = android.os.SystemClock.elapsedRealtime()
        }
    }

    override fun onStop() {
        super.onStop()
        isActivityResumed = false
        if (!pictureInPictureMode && lastBackgroundTimestamp == 0L) {
            lastBackgroundTimestamp = android.os.SystemClock.elapsedRealtime()
        }
        // Do not infer PiP dismissal from onStop: some vendors dispatch it while an
        // expanded PiP is rebuilding its fullscreen window. The confirmation callback
        // above distinguishes expansion (focus/resume returns) from an actual close.
    }

    override fun onDestroy() {
        playbackSleepTimer.dispose()
        pictureInPictureHandler.removeCallbacksAndMessages(null)
        onPictureInPictureDismissed?.invoke()
        onPictureInPictureDismissed = null
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        transferDeepLink = intent?.dataString
        enableEdgeToEdge()
        FormFactorDetector.detect(this)

        val imageLoader = ImageLoader.Builder(this)
            .components {
                add(coil.decode.SvgDecoder.Factory())
            }
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
            .bitmapFactoryMaxParallelism(4)
            .crossfade(false)
            .build()
        Coil.setImageLoader(imageLoader)

        setContent {
            val focusManager = LocalFocusManager.current
            val keyboardController = LocalSoftwareKeyboardController.current
            val view = LocalView.current

            val isTvDevice = FormFactorDetector.detectFromConfiguration(resources.configuration).isTelevision

            FortressTVTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .semantics { testTagsAsResourceId = true }
                ) {
                    FortressTVApp(
                        transferDeepLink = transferDeepLink,
                        onTransferDeepLinkConsumed = { transferDeepLink = null },
                        foregroundGeneration = foregroundGeneration,
                        inactivityLockTriggered = inactivityLockTriggered,
                        onInactivityLockConsumed = { inactivityLockTriggered = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        transferDeepLink = intent.dataString
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
private fun FortressTVApp(
    transferDeepLink: String?,
    onTransferDeepLinkConsumed: () -> Unit,
    foregroundGeneration: Int,
    inactivityLockTriggered: Boolean = false,
    onInactivityLockConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { SourceSettingsStore(context.applicationContext) }
    val initialProfileId: String = remember { ProfileRepository.get(context).activeProfile.value.id }
    val initialUserId: String? = remember { com.fortresstv.player.account.AccountRepository.get(context).currentUserId() }
    val initialNs: String = remember(initialUserId, initialProfileId) { profileProgressNamespace(initialUserId, initialProfileId) }
    val userLibrary = remember { UserLibraryStore(context.applicationContext, initialNs) }
    val vodProgressStore = remember { VodProgressStore(context.applicationContext, initialNs) }
    val openSubtitlesStore = remember { OpenSubtitlesSettingsStore(context.applicationContext) }
    var openSubtitlesSettings by remember { mutableStateOf(openSubtitlesStore.loadSettings()) }
    val tmdbStore = remember { TmdbSettingsStore(context.applicationContext) }
    var tmdbSettings by remember { mutableStateOf(tmdbStore.loadSettings()) }
    val introDbStore = remember { TheIntroDbSettingsStore(context.applicationContext) }
    var introDbSettings by remember { mutableStateOf(introDbStore.loadSettings()) }
    val omdbStore = remember { OmdbSettingsStore(context.applicationContext) }
    var omdbSettings by remember { mutableStateOf(omdbStore.loadSettings()) }

    var playlists by remember { mutableStateOf<List<PlaylistProfile>>(emptyList()) }
    var appLanguage by remember { mutableStateOf(AppLanguage.fromSystemLocale()) }
    var startupDestination by remember { mutableStateOf(settings.startupDestination()) }
    var homeHiddenSections by remember { mutableStateOf(settings.homeHiddenSections(initialProfileId)) }
    var showCustomizeHomeDialog by remember { mutableStateOf(false) }
    var playChannelsFullscreen by remember { mutableStateOf(false) }
    var autoPictureInPicture by remember { mutableStateOf(true) }
    var telemetryEnabled by remember { mutableStateOf(settings.telemetryEnabled()) }
    var playbackMode by remember { mutableStateOf(PlaybackMode.AUTO) }
    var preferredAudioLanguage by remember { mutableStateOf(settings.preferredAudioLanguage(initialProfileId)) }
    var preferredSubtitleLanguage by remember { mutableStateOf(settings.preferredSubtitleLanguage(initialProfileId)) }
    var subtitleFallbackEnglish by remember { mutableStateOf(settings.subtitleFallbackEnglish(initialProfileId)) }
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
    var progressRefreshTrigger by remember { mutableStateOf(0) }
    var hiddenCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var categoryOrder by remember { mutableStateOf<List<String>>(emptyList()) }

    var epgGuide by remember { mutableStateOf<EpgGuide?>(null) }
    val initialEpgSource = remember { settings.epgSource() }
    var epgSource by remember { mutableStateOf<ContentSource?>(initialEpgSource) }
    var epgSourceName by remember { mutableStateOf<String?>(initialEpgSource?.displayName) }
    var epgLastUpdate by remember { mutableLongStateOf(0L) }
    var epgScheduleCorrectionMinutes by remember { mutableIntStateOf(settings.epgScheduleCorrectionMinutes()) }
    val epgTimeShiftHours = epgScheduleCorrectionMinutes / 60
    var epgSourceTimezone by remember { mutableStateOf(settings.epgSourceTimezone().orEmpty()) }
    val epgMappingStore = remember { com.fortresstv.player.epg.EpgMappingStore(context.applicationContext) }
    var epgMappings by remember { mutableStateOf(epgMappingStore.getAllMappings()) }
    var epgLoadedDays by remember { mutableIntStateOf(3) }
    val epgRepository = remember { DefaultEpgRepository(context) }
    var currentAppEpochDay by remember { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var activeEpgEpochDay by remember { mutableLongStateOf(currentAppEpochDay) }

    var error by remember { mutableStateOf<String?>(null) }
    var retryLoad by remember { mutableStateOf<(() -> Unit)?>(null) }
    var loading by remember { mutableStateOf(false) }
    var restoring by remember { mutableStateOf(true) }

    var showSettings by remember { mutableStateOf(false) }
    var playbackResetGeneration by remember { mutableIntStateOf(0) }
    var settingsInitialTab by remember { mutableIntStateOf(0) }
    var showAddPlaylistDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showSendToTvDialog by remember { mutableStateOf(false) }
    var sendToTvProfile by remember { mutableStateOf<PlaylistProfile?>(null) }
    var directTransferTarget by remember { mutableStateOf<DiscoveredFortressTv?>(null) }
    var editingProfile by remember { mutableStateOf<PlaylistProfile?>(null) }
    var deletingProfile by remember { mutableStateOf<PlaylistProfile?>(null) }
    var showEpgUrlDialog by remember { mutableStateOf(false) }
    var showRemoveEpgConfirm by remember { mutableStateOf(false) }
    var hideAdultContent by remember { mutableStateOf(settings.hideAdultContent()) }
    var parentalPin by remember { mutableStateOf(settings.getParentalPin()) }
    var parentalControlEnabled by remember { mutableStateOf(settings.parentalControlEnabled()) }
    var activePinDialog by remember { mutableStateOf<PinDialogMode?>(null) }
    var isAdultContentUnlockedByPin by remember { mutableStateOf(false) }

    val profileRepository = remember { ProfileRepository.get(context) }
    val activeHouseholdProfile by profileRepository.activeProfile.collectAsState()
    val householdProfiles by profileRepository.profiles.collectAsState()
    var showProfileSelectionDialog by remember { mutableStateOf(false) }
    var pinPromptAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingAdultTokenAction by remember { mutableStateOf<((String?) -> Unit)?>(null) }
    var requireProfileSelection by rememberSaveable { mutableStateOf(false) }

    val accountRepository = remember { com.fortresstv.player.account.AccountRepository.get(context) }
    var currentUserId by remember { mutableStateOf(accountRepository.currentUserId()) }
    val cloudSourceSyncService = remember { com.fortresstv.player.source.CloudSourceSyncService.get(context) }
    val cloudLibrarySyncService = remember { com.fortresstv.player.library.CloudLibrarySyncService.get(context) }
    val migrationService = remember { com.fortresstv.player.account.LocalDataMigrationService(context) }

    fun requestAdultAction(action: (String?) -> Unit) {
        val status = profileRepository.getPinStatus()
        if (!activeHouseholdProfile.isChild && !status.hasPin) {
            action(null)
        } else if (profileRepository.isAdultUnlocked()) {
            action(profileRepository.currentUnlockToken())
        } else {
            pendingAdultTokenAction = action
        }
    }

    fun handleSaveEpgMapping(channel: PlaylistChannel, xmltvChannel: com.fortresstv.player.epg.EpgChannel, unlockToken: String?) {
        val mapping = com.fortresstv.player.epg.EpgChannelMapping(
            iptvSourceId = channel.sourceId,
            iptvChannelId = channel.sourceItemId,
            epgSourceId = epgSource?.displayName,
            xmltvChannelId = xmltvChannel.id,
            isAvailable = true
        )
        epgMappingStore.saveMapping(mapping)
        epgMappings = epgMappingStore.getAllMappings()
        val accountState = accountRepository.getAccountState()
        if (accountState is com.fortresstv.player.account.AccountState.AuthenticatedOnline) {
            scope.launch(Dispatchers.IO) {
                cloudSourceSyncService.syncEpgMappingToCloud(mapping, unlockToken)
            }
        }
    }

    fun handleResetEpgMapping(channel: PlaylistChannel, unlockToken: String?) {
        epgMappingStore.removeMapping(channel.sourceId, channel.sourceItemId)
        epgMappings = epgMappingStore.getAllMappings()
        val accountState = accountRepository.getAccountState()
        if (accountState is com.fortresstv.player.account.AccountState.AuthenticatedOnline) {
            scope.launch(Dispatchers.IO) {
                cloudSourceSyncService.deleteEpgMappingFromCloud(channel.sourceId, channel.sourceItemId, unlockToken)
            }
        }
    }

    val shouldShowCloudOnboarding = remember {
        currentUserId == null && !settings.hasSeenCloudOnboardingPrompt()
    }
    var showOnboardingDialog by rememberSaveable { mutableStateOf(shouldShowCloudOnboarding) }
    var showLocalImportDialog by remember { mutableStateOf(false) }

    val pinStatus = remember(currentUserId, householdProfiles, requireProfileSelection) { profileRepository.getPinStatus() }
    val shouldGateWithProfilePicker = requireProfileSelection

    LaunchedEffect(inactivityLockTriggered) {
        if (inactivityLockTriggered) {
            profileRepository.revokeAdultUnlock()
            onInactivityLockConsumed()
        }
    }

    LaunchedEffect(profileRepository) {
        val userId = withContext(Dispatchers.IO) {
            accountRepository.currentUserId()
        }
        currentUserId = userId
        profileRepository.onAccountChanged(userId)
        profileRepository.refresh()
    }

    LaunchedEffect(currentUserId) {
        val uid = currentUserId
        cloudLibrarySyncService.onAccountChanged(uid)
        if (uid != null) {
            cloudLibrarySyncService.triggerSync()
            if (migrationService.isMigrationNeeded(uid)) {
                showLocalImportDialog = true
            }
        }
    }

    suspend fun reconnectFavouritesAndProgress(sourceId: String, catalog: XtreamCatalog) = withContext(Dispatchers.Default) {
        val favs = userLibrary.favoriteChannelIds().toMutableSet()
        val watchlist = userLibrary.movieWatchlistIds().toMutableSet()
        val followed = userLibrary.followedSeriesIds().toMutableSet()
        val watched = userLibrary.watchedItemIds().toMutableSet()

        val channelPrefix = "channel:$sourceId:"
        val moviePrefix = "movie:$sourceId:"
        val seriesPrefix = "series:$sourceId:"

        val hasHashedChannels = favs.any { it.startsWith("channel:") && it.length > 40 }
        if (hasHashedChannels) {
            for (channel in catalog.liveChannels) {
                val hash = com.fortresstv.player.library.CloudIdentityHasher.hashItemId(channel.sourceItemId)
                if (favs.contains("$channelPrefix$hash") || favs.any { it.endsWith(":$hash") }) {
                    favs.add("$channelPrefix${channel.sourceItemId}")
                }
            }
            userLibrary.setFavoriteChannelIds(favs)
        }

        val hasHashedMovies = watchlist.any { it.startsWith("movie:") && it.length > 38 } ||
                watched.any { it.startsWith("movie:") && it.length > 38 }
        if (hasHashedMovies) {
            for (movie in catalog.movies) {
                val hash = com.fortresstv.player.library.CloudIdentityHasher.hashItemId(movie.id.toString())
                val movieKey = "$moviePrefix$hash"
                if (watchlist.contains(movieKey) || watchlist.any { it.endsWith(":$hash") }) {
                    watchlist.add("$moviePrefix${movie.id}")
                }
                if (watched.contains(movieKey) || watched.any { it.endsWith(":$hash") }) {
                    watched.add("$moviePrefix${movie.id}")
                }
            }
            userLibrary.setMovieWatchlistIds(watchlist)
            userLibrary.setWatchedItemIds(watched)
        }

        val hasHashedSeries = followed.any { it.startsWith("series:") && it.length > 39 }
        if (hasHashedSeries) {
            for (series in catalog.series) {
                val hash = com.fortresstv.player.library.CloudIdentityHasher.hashItemId(series.id.toString())
                if (followed.contains("$seriesPrefix$hash") || followed.any { it.endsWith(":$hash") }) {
                    followed.add("$seriesPrefix${series.id}")
                }
            }
            userLibrary.setFollowedSeriesIds(followed)
        }

        val currentEntries = vodProgressStore.entries().toMutableList()
        val movieMap = mutableMapOf<String, XtreamMovie>()
        for (movie in catalog.movies) {
            movieMap[movie.id.toString()] = movie
        }
        val seriesMap = mutableMapOf<String, XtreamSeries>()
        for (series in catalog.series) {
            seriesMap[series.id.toString()] = series
        }

        var progressChanged = false
        for (i in currentEntries.indices) {
            val entry = currentEntries[i]
            if (entry.kind == com.fortresstv.player.library.VodProgressKind.MOVIE && (entry.title.isBlank() || entry.posterUrl.isNullOrBlank())) {
                var matchingMovie = movieMap[entry.itemId]
                if (matchingMovie == null) {
                    matchingMovie = catalog.movies.firstOrNull {
                        com.fortresstv.player.library.CloudIdentityHasher.hashItemId(it.id.toString()) == entry.itemId
                    }
                }
                if (matchingMovie != null) {
                    currentEntries[i] = entry.copy(
                        key = "movie:$sourceId:${matchingMovie.id}",
                        sourceId = sourceId,
                        itemId = matchingMovie.id.toString(),
                        title = matchingMovie.name,
                        posterUrl = matchingMovie.posterUrl,
                        containerExtension = matchingMovie.containerExtension.ifBlank { "mp4" }
                    )
                    progressChanged = true
                }
            } else if (entry.kind == com.fortresstv.player.library.VodProgressKind.EPISODE && (entry.title.isBlank() || entry.posterUrl.isNullOrBlank())) {
                val matchingSeries = entry.seriesId?.let { sId -> seriesMap[sId.toString()] }
                    ?: catalog.series.firstOrNull {
                        val sIdStr = it.id.toString()
                        val sHash = com.fortresstv.player.library.CloudIdentityHasher.hashItemId(sIdStr)
                        it.id == entry.seriesId ||
                            sIdStr == entry.itemId ||
                            sHash == entry.itemId
                    }
                if (matchingSeries != null) {
                    currentEntries[i] = entry.copy(
                        sourceId = sourceId,
                        seriesId = matchingSeries.id,
                        title = if (entry.title.isNotBlank()) entry.title else matchingSeries.name,
                        posterUrl = entry.posterUrl ?: matchingSeries.coverUrl
                    )
                    progressChanged = true
                }
            }
        }

        val seriesWithProgress = currentEntries
            .filter { it.kind == com.fortresstv.player.library.VodProgressKind.EPISODE && it.sourceId == sourceId }
            .mapNotNull { it.seriesId }
            .toSet()

        val filesDir = context.applicationContext.filesDir
        val xtreamSource = playlists.firstOrNull { it.id == sourceId }?.source as? ContentSource.Xtream
        val client = XtreamClient(context.applicationContext, sourceId, xtreamSource ?: ContentSource.Xtream("", "", "", ""))

        for (series in catalog.series) {
            if (series.id in seriesWithProgress) continue
            if (vodProgressStore.isDismissed("episode:$sourceId:${series.id}", series.id.toString())) continue

            val cacheFile = java.io.File(filesDir, "xtream_${sourceId}_get_series_info_series_id_${series.id}.json")
            if (!cacheFile.exists() || cacheFile.length() <= 0) continue

            val details = runCatching { client.parseSeriesDetails(cacheFile.readText()) }.getOrNull() ?: continue
            val episodes = details.episodes
            if (episodes.isEmpty()) continue

            val watchedIndices = episodes.indices.filter { idx ->
                val ep = episodes[idx]
                val epKey = ep.libraryId(sourceId)
                epKey in watched || userLibrary.isItemWatched(epKey, watched)
            }
            if (watchedIndices.isEmpty()) continue

            val lastWatchedIndex = watchedIndices.maxOrNull() ?: continue
            val nextEpisode = episodes.getOrNull(lastWatchedIndex + 1) ?: continue

            val nextKey = nextEpisode.libraryId(sourceId)
            if (nextKey in watched || userLibrary.isItemWatched(nextKey, watched)) continue
            if (vodProgressStore.isDismissed(nextKey, series.id.toString())) continue

            val strings = if (java.util.Locale.getDefault().language.lowercase().startsWith("pt")) {
                com.fortresstv.player.ui.PortugueseStrings
            } else {
                com.fortresstv.player.ui.EnglishStrings
            }

            val nextEntry = VodProgressEntry(
                key = nextKey,
                kind = com.fortresstv.player.library.VodProgressKind.EPISODE,
                sourceId = sourceId,
                itemId = nextEpisode.id,
                seriesId = series.id,
                title = "${series.name} · ${strings.episodeLabel(nextEpisode.season, nextEpisode.episodeNumber)} · ${nextEpisode.title}",
                posterUrl = nextEpisode.posterUrl ?: series.coverUrl,
                containerExtension = nextEpisode.containerExtension.ifBlank { "mp4" },
                season = nextEpisode.season,
                episode = nextEpisode.episodeNumber,
                positionMs = 0L,
                durationMs = (nextEpisode.durationSecs ?: 0) * 1000L,
                updatedAtMs = cacheFile.lastModified().coerceAtLeast(System.currentTimeMillis() - 24 * 3600 * 1000L)
            )
            currentEntries.add(0, nextEntry)
            progressChanged = true
        }

        if (progressChanged) {
            vodProgressStore.setAll(currentEntries)
        }

        withContext(Dispatchers.Main) {
            favoriteChannelIds = userLibrary.favoriteChannelIds()
            recentChannelIds = userLibrary.recentChannelIds()
            movieWatchlistIds = userLibrary.movieWatchlistIds()
            followedSeriesIds = userLibrary.followedSeriesIds()
            watchedItemIds = userLibrary.watchedItemIds()
            progressRefreshTrigger++
        }
    }

    LaunchedEffect(activeHouseholdProfile.id, currentUserId) {
        playbackResetGeneration++
        isAdultContentUnlockedByPin = false

        val targetProfileId = activeHouseholdProfile.id
        val uid = currentUserId
        val ns = profileProgressNamespace(uid, targetProfileId)
        userLibrary.switchNamespace(ns)
        vodProgressStore.switchNamespace(ns)
        favoriteChannelIds = userLibrary.favoriteChannelIds()
        recentChannelIds = userLibrary.recentChannelIds()
        movieWatchlistIds = userLibrary.movieWatchlistIds()
        followedSeriesIds = userLibrary.followedSeriesIds()
        watchedItemIds = userLibrary.watchedItemIds()
        progressRefreshTrigger++

        preferredAudioLanguage = settings.preferredAudioLanguage(targetProfileId)
        preferredSubtitleLanguage = settings.preferredSubtitleLanguage(targetProfileId)
        subtitleFallbackEnglish = settings.subtitleFallbackEnglish(targetProfileId)
        startupDestination = settings.startupDestination(targetProfileId)
        homeHiddenSections = settings.homeHiddenSections(targetProfileId)
        epgScheduleCorrectionMinutes = settings.epgScheduleCorrectionMinutes(targetProfileId)

        val activeSourceId = activeProfile?.id
        if (activeSourceId != null) {
            hiddenCategories = userLibrary.hiddenCategories(activeSourceId)
            categoryOrder = userLibrary.categoryOrder(activeSourceId)
        }

        val isPrimaryAdult = !activeHouseholdProfile.isChild && (householdProfiles.firstOrNull { !it.isChild }?.id == targetProfileId || householdProfiles.isEmpty())
        val legacyPrefs = context.getSharedPreferences("fortress_legacy_migration", Context.MODE_PRIVATE)
        val migrationKey = "legacy_local_migrated_v1_$targetProfileId"
        val legacyMigrated = legacyPrefs.getBoolean(migrationKey, false)
        if (!legacyMigrated && isPrimaryAdult && targetProfileId != "restricted_default" && ns != "local") {
            val localLib = UserLibraryStore(context.applicationContext, "local")
            val targetLib = UserLibraryStore(context.applicationContext, ns)
            val targetProg = VodProgressStore(context.applicationContext, ns)
            val localProg = VodProgressStore(context.applicationContext, "local")

            val localFollowed = localLib.followedSeriesIds()
            if (localFollowed.isNotEmpty() && targetLib.followedSeriesIds().isEmpty()) {
                targetLib.setFollowedSeriesIds(localFollowed)
                for (item in localFollowed) {
                    val parts = item.split(":")
                    if (parts.size >= 3) {
                        cloudLibrarySyncService.enqueueLibraryMutation(
                            profileId = targetProfileId,
                            sourceId = parts[1],
                            itemKind = "series",
                            itemId = parts[2],
                            isFavourite = true,
                            isWatchlisted = true,
                            isWatched = false
                        )
                    }
                }
            }
            val localWatchlist = localLib.movieWatchlistIds()
            if (localWatchlist.isNotEmpty() && targetLib.movieWatchlistIds().isEmpty()) {
                targetLib.setMovieWatchlistIds(localWatchlist)
                for (item in localWatchlist) {
                    val parts = item.split(":")
                    if (parts.size >= 3) {
                        cloudLibrarySyncService.enqueueLibraryMutation(
                            profileId = targetProfileId,
                            sourceId = parts[1],
                            itemKind = "movie",
                            itemId = parts[2],
                            isFavourite = true,
                            isWatchlisted = true,
                            isWatched = false
                        )
                    }
                }
            }
            val localFavs = localLib.favoriteChannelIds()
            if (localFavs.isNotEmpty() && targetLib.favoriteChannelIds().isEmpty()) {
                targetLib.setFavoriteChannelIds(localFavs)
                for (item in localFavs) {
                    val parts = item.split(":")
                    if (parts.size >= 3) {
                        cloudLibrarySyncService.enqueueLibraryMutation(
                            profileId = targetProfileId,
                            sourceId = parts[1],
                            itemKind = "channel",
                            itemId = parts[2],
                            isFavourite = true,
                            isWatchlisted = false,
                            isWatched = false
                        )
                    }
                }
            }
            val localEntries = localProg.entries()
            if (localEntries.isNotEmpty() && targetProg.entries().isEmpty()) {
                targetProg.setAll(localEntries)
                for (entry in localEntries) {
                    cloudLibrarySyncService.enqueueProgressMutation(
                        profileId = targetProfileId,
                        sourceId = entry.sourceId,
                        itemKind = entry.kind.wireValue,
                        itemId = entry.itemId,
                        positionMs = entry.positionMs,
                        durationMs = entry.durationMs,
                        completed = entry.progressFraction >= 0.95f
                    )
                }
            }
            legacyPrefs.edit()
                .putBoolean(migrationKey, true)
                .putBoolean("legacy_local_migrated", true)
                .apply()
            progressRefreshTrigger++
        }

        if (uid != null && targetProfileId != "restricted_default") {
            val currentFollowed = userLibrary.followedSeriesIds()
            for (item in currentFollowed) {
                val parts = item.split(":")
                if (parts.size >= 3 && parts[2].length < 64) {
                    cloudLibrarySyncService.enqueueLibraryMutation(
                        profileId = targetProfileId,
                        sourceId = parts[1],
                        itemKind = "series",
                        itemId = parts[2],
                        isFavourite = true,
                        isWatchlisted = true,
                        isWatched = false
                    )
                }
            }
            val currentWatchlist = userLibrary.movieWatchlistIds()
            for (item in currentWatchlist) {
                val parts = item.split(":")
                if (parts.size >= 3 && parts[2].length < 64) {
                    cloudLibrarySyncService.enqueueLibraryMutation(
                        profileId = targetProfileId,
                        sourceId = parts[1],
                        itemKind = "movie",
                        itemId = parts[2],
                        isFavourite = true,
                        isWatchlisted = true,
                        isWatched = false
                    )
                }
            }
            val currentProgress = vodProgressStore.entries()
            for (entry in currentProgress) {
                if (entry.itemId.length < 64) {
                    cloudLibrarySyncService.enqueueProgressMutation(
                        profileId = targetProfileId,
                        sourceId = entry.sourceId,
                        itemKind = when (entry.kind) {
                            VodProgressKind.MOVIE -> "movie"
                            VodProgressKind.EPISODE -> "episode"
                            VodProgressKind.CATCHUP -> "catchup"
                        },
                        itemId = entry.itemId,
                        positionMs = entry.positionMs,
                        durationMs = entry.durationMs,
                        completed = entry.progressFraction >= 0.95f
                    )
                }
            }
            cloudLibrarySyncService.triggerSync()
            val profileLibrary = UserLibraryStore(context.applicationContext, ns)
            val profileProgress = VodProgressStore(context.applicationContext, ns)
            cloudLibrarySyncService.pullAndApplyProfileData(
                profileId = targetProfileId,
                userLibrary = profileLibrary,
                progressStore = profileProgress,
                settingsStore = settings,
                onUpdated = {
                    if (activeHouseholdProfile.id == targetProfileId) {
                        scope.launch(Dispatchers.Default) {
                            xtreamCatalogs.forEach { (sourceId, catalog) ->
                                reconnectFavouritesAndProgress(sourceId, catalog)
                            }
                            withContext(Dispatchers.Main) {
                                favoriteChannelIds = userLibrary.favoriteChannelIds()
                                recentChannelIds = userLibrary.recentChannelIds()
                                movieWatchlistIds = userLibrary.movieWatchlistIds()
                                followedSeriesIds = userLibrary.followedSeriesIds()
                                watchedItemIds = userLibrary.watchedItemIds()
                                progressRefreshTrigger++
                            }
                        }
                        preferredAudioLanguage = settings.preferredAudioLanguage(targetProfileId)
                        preferredSubtitleLanguage = settings.preferredSubtitleLanguage(targetProfileId)
                        subtitleFallbackEnglish = settings.subtitleFallbackEnglish(targetProfileId)
                        startupDestination = settings.startupDestination(targetProfileId)
                        homeHiddenSections = settings.homeHiddenSections(targetProfileId)
                        epgScheduleCorrectionMinutes = settings.epgScheduleCorrectionMinutes(targetProfileId)
                        if (activeSourceId != null) {
                            hiddenCategories = userLibrary.hiddenCategories(activeSourceId)
                            categoryOrder = userLibrary.categoryOrder(activeSourceId)
                        }
                    }
                }
            )
        }
    }

    LaunchedEffect(activeProfile?.id) {
        val activeSourceId = activeProfile?.id
        if (activeSourceId != null) {
            hiddenCategories = userLibrary.hiddenCategories(activeSourceId)
            categoryOrder = userLibrary.categoryOrder(activeSourceId)
        }
    }

    LaunchedEffect(activeHouseholdProfile.id, activeHouseholdProfile.language) {
        if (activeHouseholdProfile.id != "restricted_default") {
            val target = if (activeHouseholdProfile.language.startsWith("pt")) AppLanguage.PORTUGUESE else AppLanguage.ENGLISH
            if (appLanguage != target) {
                appLanguage = target
                settings.setLanguage(target.code)
            }
        }
    }

    LaunchedEffect(activeHouseholdProfile.id, activeHouseholdProfile.hideAdultContent, activeHouseholdProfile.kind) {
        hideAdultContent = if (activeHouseholdProfile.isChild) true else activeHouseholdProfile.hideAdultContent
    }

    val effectiveHideAdultContent = when {
        activeHouseholdProfile.isChild -> true
        else -> hideAdultContent
    }

    val handleHideAdultContentChanged: (Boolean) -> Unit = { enabled ->
        if (!activeHouseholdProfile.isChild) {
            if (!enabled) {
                requestAdultAction { unlockToken ->
                    hideAdultContent = false
                    scope.launch(Dispatchers.IO) {
                        settings.setHideAdultContent(false)
                        profileRepository.setProfileHideAdultContent(
                            activeHouseholdProfile.id,
                            false,
                            unlockToken
                        )
                    }
                }
            } else {
                hideAdultContent = true
                scope.launch(Dispatchers.IO) {
                    settings.setHideAdultContent(true)
                    profileRepository.setProfileHideAdultContent(
                        activeHouseholdProfile.id,
                        true,
                        null
                    )
                }
            }
        }
    }
    val effectiveMaximumAge = if (activeHouseholdProfile.isChild) (activeHouseholdProfile.maximumAge ?: 12) else activeHouseholdProfile.maximumAge
    val effectiveBlockUnrated = if (activeHouseholdProfile.isChild) true else activeHouseholdProfile.blockUnrated

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
            val current = activeHouseholdProfile
            if (current.id != "restricted_default") {
                val profileLang = if (newLang == AppLanguage.PORTUGUESE) "pt-PT" else "en"
                if (current.language != profileLang) {
                    profileRepository.saveProfile(current.copy(language = profileLang))
                }
            }
        }
    }

    val onPreferredAudioLanguageChanged: (String) -> Unit = { lang ->
        preferredAudioLanguage = lang
        scope.launch(Dispatchers.IO) { settings.setPreferredAudioLanguage(lang, activeHouseholdProfile.id) }
        if (currentUserId != null && activeHouseholdProfile.id != "restricted_default") {
            cloudLibrarySyncService.recordPreferencesMutation(
                profileId = activeHouseholdProfile.id,
                audioLang = lang,
                subtitleLang = preferredSubtitleLanguage,
                subFallback = subtitleFallbackEnglish,
                startupDestination = startupDestination.persistedValue,
                epgTimeShiftHours = epgScheduleCorrectionMinutes / 60
            )
        }
    }

    val onPreferredSubtitleLanguageChanged: (String) -> Unit = { lang ->
        preferredSubtitleLanguage = lang
        scope.launch(Dispatchers.IO) { settings.setPreferredSubtitleLanguage(lang, activeHouseholdProfile.id) }
        if (currentUserId != null && activeHouseholdProfile.id != "restricted_default") {
            cloudLibrarySyncService.recordPreferencesMutation(
                profileId = activeHouseholdProfile.id,
                audioLang = preferredAudioLanguage,
                subtitleLang = lang,
                subFallback = subtitleFallbackEnglish,
                startupDestination = startupDestination.persistedValue,
                epgTimeShiftHours = epgScheduleCorrectionMinutes / 60
            )
        }
    }

    suspend fun parsePlaylist(stream: java.io.InputStream): Playlist = withContext(Dispatchers.Default) {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
        SizeLimitedInputStream(stream, 100L * 1024L * 1024L, "Playlist").bufferedReader().use {
            M3uParser.parse(it)
        }
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
            XmlTvParser.parse(
                SizeLimitedInputStream(decompressed, 250L * 1024L * 1024L, "Decompressed EPG"),
                loadedDays = loadedDays
            )
        }
    }

    suspend fun parseEpgWithCache(
        source: ContentSource,
        stream: java.io.InputStream,
        allowCachedResult: Boolean,
        loadedDays: Int = epgLoadedDays
    ): EpgGuide = withContext(Dispatchers.IO) {
        val fallbackZone = runCatching { java.time.ZoneId.of(epgSourceTimezone) }.getOrDefault(java.time.ZoneId.systemDefault())
        val baseFingerprint = SourceReader.fingerprint(context, source, "epg")
        val fingerprint = if (baseFingerprint != null) "$baseFingerprint#days=$loadedDays#tz=${fallbackZone.id}" else null
        if (allowCachedResult && fingerprint != null) {
            val manifest = EpgDiskCache.readCurrentManifest(context, fingerprint)
            val zone = manifest?.let { runCatching { java.time.ZoneId.of(it.timezoneId) }.getOrNull() } ?: java.time.ZoneId.systemDefault()
            val currentTodayEpoch = java.time.LocalDate.now(zone).toEpochDay()
            val hasFullWindow = manifest != null &&
                manifest.todayEpochDay == currentTodayEpoch &&
                (0 until loadedDays).all { manifest.partitions.containsKey(currentTodayEpoch + it) }

            if (hasFullWindow) {
                EpgDiskCache.loadTodayGuide(context, fingerprint)?.let { cached ->
                    stream.close()
                    epgRepository.setGuide(cached)
                    return@withContext cached
                }
            } else if (manifest != null && manifest.partitions.isNotEmpty()) {
                EpgDiskCache.loadTodayGuide(context, fingerprint)?.let { partialCached ->
                    epgRepository.setGuide(partialCached)
                    withContext(Dispatchers.Main) { epgGuide = partialCached }
                }
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
                input = SizeLimitedInputStream(decompressed, 250L * 1024L * 1024L, "Decompressed EPG"),
                context = context,
                fingerprint = fingerprint.orEmpty(),
                loadedDays = loadedDays,
                fallbackSourceZone = fallbackZone
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

                    val favs = userLibrary.favoriteChannelIds().toMutableSet()
                    var changed = false
                    for (ch in parsed.channels) {
                        val hash = com.fortresstv.player.library.CloudIdentityHasher.hashItemId(ch.sourceItemId)
                        if (favs.contains("channel:${updatedProfile.id}:$hash")) {
                            favs.add("channel:${updatedProfile.id}:${ch.sourceItemId}")
                            changed = true
                        }
                    }
                    if (changed) {
                        userLibrary.setFavoriteChannelIds(favs)
                        favoriteChannelIds = userLibrary.favoriteChannelIds()
                    }
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
                reconnectFavouritesAndProgress(profile.id, catalog)
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
                    if (save) {
                        settings.saveEpg(source)
                        val accountState = accountRepository.getAccountState()
                        if (accountState is com.fortresstv.player.account.AccountState.AuthenticatedOnline) {
                            scope.launch(Dispatchers.IO) {
                                cloudSourceSyncService.syncSourcesToCloud(profileRepository.currentUnlockToken())
                            }
                        }
                    }
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

    fun advanceEpgWindow(source: ContentSource, epochDay: Long) {
        scope.launch {
            val cachedTodayGuide = withContext(Dispatchers.IO) {
                val manifest = epgRepository.manifest.value
                val day = epgRepository.loadDay(epochDay)
                if (manifest != null && day != null) {
                    EpgGuide(
                        channels = manifest.channels,
                        programmes = day.programmes,
                        manifest = manifest,
                        epochDay = epochDay
                    )
                } else null
            }
            if (cachedTodayGuide != null) {
                epgRepository.setGuide(cachedTodayGuide)
                epgGuide = cachedTodayGuide
            }

            when (source) {
                is ContentSource.RemoteUrl -> loadEpg(source, save = false, forceRefresh = true)
                is ContentSource.LocalFile -> if (cachedTodayGuide == null) {
                    loadEpg(source, save = false, forceRefresh = false)
                }
                is ContentSource.Xtream -> Unit
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentAppEpochDay = LocalDate.now().toEpochDay()
            delay(60_000)
        }
    }

    LaunchedEffect(foregroundGeneration) {
        currentAppEpochDay = LocalDate.now().toEpochDay()
    }

    LaunchedEffect(currentAppEpochDay) {
        if (currentAppEpochDay != activeEpgEpochDay) {
            activeEpgEpochDay = currentAppEpochDay
            val source = withContext(Dispatchers.IO) { settings.epgSource() }
            if (source != null) advanceEpgWindow(source, currentAppEpochDay)
        }
    }

    LaunchedEffect(Unit) {
        val savedLangCode = withContext(Dispatchers.IO) { settings.getLanguage() }
        val correction = withContext(Dispatchers.IO) { settings.epgScheduleCorrectionMinutes() }
        val sourceTz = withContext(Dispatchers.IO) { settings.epgSourceTimezone().orEmpty() }
        val days = withContext(Dispatchers.IO) { settings.epgLoadedDays() }
        val epgUpdate = withContext(Dispatchers.IO) { settings.lastEpgUpdate() }
        val savedFullscreenPreference = withContext(Dispatchers.IO) { settings.playChannelsFullscreen() }
        val savedPictureInPicturePreference = withContext(Dispatchers.IO) { settings.autoPictureInPicture() }
        val savedTelemetryPreference = withContext(Dispatchers.IO) { settings.telemetryEnabled() }
        val savedPlaybackMode = withContext(Dispatchers.IO) { settings.playbackMode() }
        appLanguage = AppLanguage.fromCode(savedLangCode)
        epgScheduleCorrectionMinutes = correction
        epgSourceTimezone = sourceTz
        epgLoadedDays = days
        epgLastUpdate = epgUpdate
        playChannelsFullscreen = savedFullscreenPreference
        autoPictureInPicture = savedPictureInPicturePreference
        telemetryEnabled = savedTelemetryPreference
        playbackMode = savedPlaybackMode

        val (savedPlaylists, enabledPlaylists) = withContext(Dispatchers.IO) {
            val all = settings.getPlaylists()
            all to settings.getEnabledPlaylists()
        }
        playlists = savedPlaylists
        activeProfile = enabledPlaylists.firstOrNull()
    }

    var sourcesLoadedForSession by rememberSaveable { mutableStateOf(false) }
    var isProfileLoadingScreenVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isProfileLoadingScreenVisible, playlist, activeProfile) {
        if (isProfileLoadingScreenVisible && playlist != null && activeProfile != null) {
            isProfileLoadingScreenVisible = false
        }
    }

    LaunchedEffect(shouldGateWithProfilePicker, playbackResetGeneration) {
        if (shouldGateWithProfilePicker) return@LaunchedEffect
        if (sourcesLoadedForSession && playbackResetGeneration == 0) {
            isProfileLoadingScreenVisible = false
            return@LaunchedEffect
        }
        sourcesLoadedForSession = true
        isProfileLoadingScreenVisible = true

        try {
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

        // Restore cloud sources for authenticated households
        val cloudSourceService = CloudSourceSyncService.get(context)
        val authenticatedUid = currentUserId ?: AccountRepository.get(context).currentUserId()
        if (authenticatedUid != null) {
            scope.launch(Dispatchers.IO) {
                val restoredResult = cloudSourceService.restoreSourcesFromCloud(profileRepository.currentUnlockToken())
                val restored = restoredResult.getOrDefault(emptyList())
                if (restored.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        playlists = settings.getPlaylists()
                        restored.filter { it.enabled && it.source is ContentSource.Xtream }.forEach { p ->
                            loadXtreamProfile(p)
                        }
                    }
                }
                val restoredEpg = settings.epgSource()
                if (restoredEpg != null) {
                    withContext(Dispatchers.Main) {
                        val needsLoad = epgSource != restoredEpg || epgGuide == null || epgGuide?.programmes.isNullOrEmpty()
                        epgSource = restoredEpg
                        epgSourceName = restoredEpg.displayName
                        epgSourceTimezone = settings.epgSourceTimezone().orEmpty()
                        epgScheduleCorrectionMinutes = settings.epgScheduleCorrectionMinutes()
                        if (needsLoad) {
                            loadEpg(restoredEpg, save = false, forceRefresh = false)
                        }
                    }
                }
                val restoredMappings = cloudSourceService.restoreEpgMappingsFromCloud()
                if (restoredMappings.isNotEmpty()) {
                    epgMappingStore.replaceAllMappings(restoredMappings)
                    withContext(Dispatchers.Main) {
                        epgMappings = epgMappingStore.getAllMappings()
                    }
                }
                val restoredSubs = cloudSourceService.restoreOpenSubtitlesFromCloud(profileRepository.currentUnlockToken())
                if (restoredSubs != null && (restoredSubs.apiKey.isNotBlank() || restoredSubs.username.isNotBlank())) {
                    openSubtitlesStore.saveSettings(restoredSubs)
                    withContext(Dispatchers.Main) {
                        openSubtitlesSettings = restoredSubs
                    }
                } else if (openSubtitlesSettings.apiKey.isNotBlank() || openSubtitlesSettings.username.isNotBlank()) {
                    cloudSourceService.syncOpenSubtitlesToCloud(openSubtitlesSettings, profileRepository.currentUnlockToken())
                }
                val restoredTmdb = cloudSourceService.restoreTmdbFromCloud(profileRepository.currentUnlockToken())
                if (restoredTmdb != null && restoredTmdb.apiKey.isNotBlank()) {
                    tmdbStore.saveSettings(restoredTmdb)
                    withContext(Dispatchers.Main) {
                        tmdbSettings = restoredTmdb
                    }
                } else if (tmdbSettings.apiKey.isNotBlank()) {
                    cloudSourceService.syncTmdbToCloud(tmdbSettings, profileRepository.currentUnlockToken())
                }
                val restoredOmdb = cloudSourceService.restoreOmdbFromCloud(profileRepository.currentUnlockToken())
                if (restoredOmdb != null && restoredOmdb.apiKey.isNotBlank()) {
                    omdbStore.saveSettings(restoredOmdb)
                    withContext(Dispatchers.Main) {
                        omdbSettings = restoredOmdb
                    }
                } else if (omdbSettings.apiKey.isNotBlank()) {
                    cloudSourceService.syncOmdbToCloud(omdbSettings, profileRepository.currentUnlockToken())
                }
            }
        }

        val savedEpg = settings.epgSource()
        if (savedEpg is ContentSource.RemoteUrl) {
            val isEpgFresh = SourceReader.isCacheFresh(context, "epg", settings.lastEpgUpdate())
            if (!isEpgFresh || epgGuide == null || epgGuide?.programmes.isNullOrEmpty()) {
                loadEpg(savedEpg, save = false, forceRefresh = false)
            }
        }
        } finally {
            withContext(Dispatchers.Main) {
                delay(300)
                isProfileLoadingScreenVisible = false
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

    val configuration = LocalConfiguration.current
    val formFactor = remember(configuration) { FormFactorDetector.detectFromConfiguration(configuration) }
    LaunchedEffect(transferDeepLink, formFactor.isTelevision) {
        val rawLink = transferDeepLink ?: return@LaunchedEffect
        if (!formFactor.isTelevision) {
            DirectTransferLink.parse(rawLink)?.let { target ->
                directTransferTarget = target
                sendToTvProfile = null
                showSendToTvDialog = true
            }
        }
        onTransferDeepLinkConsumed()
    }

    val effectiveTmdbSettings = remember(tmdbSettings, activeHouseholdProfile.language) {
        if (tmdbSettings.language == TmdbLanguages.AUTO || tmdbSettings.language.isBlank()) {
            val resolvedLang = when {
                activeHouseholdProfile.language.startsWith("pt-BR", ignoreCase = true) -> "pt-BR"
                activeHouseholdProfile.language.startsWith("pt", ignoreCase = true) -> "pt-PT"
                activeHouseholdProfile.language.startsWith("es", ignoreCase = true) -> "es-ES"
                activeHouseholdProfile.language.startsWith("fr", ignoreCase = true) -> "fr-FR"
                activeHouseholdProfile.language.startsWith("de", ignoreCase = true) -> "de-DE"
                activeHouseholdProfile.language.startsWith("it", ignoreCase = true) -> "it-IT"
                else -> "en-US"
            }
            tmdbSettings.copy(language = resolvedLang)
        } else {
            tmdbSettings
        }
    }

    CompositionLocalProvider(
        LocalAppStrings provides strings,
        LocalFormFactor provides formFactor,
        LocalPlaybackMode provides playbackMode,
        LocalTmdbSettings provides effectiveTmdbSettings,
        LocalTheIntroDbSettings provides introDbSettings,
        LocalOmdbSettings provides omdbSettings
    ) {
        Box(Modifier.fillMaxSize()) {
            if (showOnboardingDialog) {
                com.fortresstv.player.account.OnboardingDialog(
                    onDismiss = {
                        showOnboardingDialog = false
                        settings.setSeenCloudOnboardingPrompt(true)
                    },
                    onComplete = {
                        showOnboardingDialog = false
                        settings.setSeenCloudOnboardingPrompt(true)
                        val uid = accountRepository.currentUserId()
                        currentUserId = uid
                        requireProfileSelection = true
                        scope.launch {
                            val all = settings.getPlaylists()
                            playlists = all
                            for (p in all.filter { it.enabled }) {
                                if (p.source is ContentSource.Xtream) loadXtreamProfile(p)
                                else loadPlaylistProfile(p)
                            }
                            val restoredEpg = settings.epgSource()
                            if (restoredEpg != null) {
                                epgSource = restoredEpg
                                epgSourceName = restoredEpg.displayName
                                epgSourceTimezone = settings.epgSourceTimezone().orEmpty()
                                epgScheduleCorrectionMinutes = settings.epgScheduleCorrectionMinutes()
                                loadEpg(restoredEpg, save = false, forceRefresh = false)
                            }
                        }
                    }
                )
            } else if (showLocalImportDialog && currentUserId != null) {
                com.fortresstv.player.account.LocalDataImportDialog(
                    householdId = currentUserId!!,
                    onDismiss = { showLocalImportDialog = false },
                    onComplete = {
                        showLocalImportDialog = false
                        scope.launch {
                            val all = settings.getPlaylists()
                            playlists = all
                            for (p in all.filter { it.enabled }) {
                                if (p.source is ContentSource.Xtream) loadXtreamProfile(p)
                                else loadPlaylistProfile(p)
                            }
                        }
                    }
                )
            } else if (shouldGateWithProfilePicker) {
                BackHandler(enabled = shouldGateWithProfilePicker) {
                    (context as? Activity)?.finish()
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    ProfileSelectionDialog(
                        isMandatory = true,
                        onProfileSelected = {
                            requireProfileSelection = false
                            isProfileLoadingScreenVisible = true
                            playbackResetGeneration++
                        },
                        onDismiss = {}
                    )
                }
            } else if (isProfileLoadingScreenVisible || (playlist == null && (loading || restoring || !sourcesLoadedForSession))) {
                ProfileLoadingScreen(
                    profile = activeHouseholdProfile,
                    statusText = strings.loadingPlaylistAndSettings
                )
            } else if (playlist == null || activeProfile == null) {
                WelcomeScreen(
                    loading = false,
                    showActions = (activeProfile == null || error != null),
                    error = error,
                    onRetry = retryLoad,
                    onAddSource = { showAddPlaylistDialog = true },
                    onEditPlaylist = (activeProfile ?: playlists.firstOrNull())?.let { prof -> { editingProfile = prof } },
                    onOpenSettings = {
                        val status = profileRepository.getPinStatus()
                        if (activeHouseholdProfile.isChild && status.hasPin && !profileRepository.isAdultUnlocked()) {
                            pinPromptAction = {
                                settingsInitialTab = 1
                                showSettings = true
                            }
                        } else {
                            settingsInitialTab = 1
                            showSettings = true
                        }
                    }
                )
            } else {
                FortressLibrary(
                    playlist = playlist!!,
                    activeProfile = activeProfile!!,
                    startupDestination = startupDestination,
                    enabledSourceCount = playlists.count { it.enabled && it.role.contributesLiveTv() },
                    enabledSources = playlists
                        .filter { it.enabled && it.role.contributesLiveTv() }
                        .sortedBy { it.order },
                    epgGuide = epgGuide,
                    epgSourceName = epgSourceName,
                    epgTimeShiftHours = epgTimeShiftHours,
                    epgScheduleCorrectionMinutes = epgScheduleCorrectionMinutes,
                    epgMappings = epgMappings,
                    epgLoadedDays = epgLoadedDays,
                    epgRepository = epgRepository,
                    playChannelsFullscreen = playChannelsFullscreen,
                    autoPictureInPicture = autoPictureInPicture,
                    preferredAudioLanguage = preferredAudioLanguage,
                    preferredSubtitleLanguage = preferredSubtitleLanguage,
                    subtitleFallbackEnglish = subtitleFallbackEnglish,
                    onPreferredAudioLanguageChanged = onPreferredAudioLanguageChanged,
                    onPreferredSubtitleLanguageChanged = onPreferredSubtitleLanguageChanged,
                    openSubtitlesSettings = openSubtitlesSettings,
                    hideAdultContent = effectiveHideAdultContent,
                    onHideAdultContentChanged = handleHideAdultContentChanged,
                    loading = loading,
                    error = error,
                    onRetryLoad = retryLoad,
                    userLibrary = userLibrary,
                    progressStore = vodProgressStore,
                    hiddenCategories = hiddenCategories,
                    categoryOrder = categoryOrder,
                    progressRefreshTrigger = progressRefreshTrigger,
                    onCategoriesChanged = { newHidden, newOrder ->
                        hiddenCategories = newHidden
                        categoryOrder = newOrder
                        val sourceId = activeProfile?.id
                        if (sourceId != null && currentUserId != null && activeHouseholdProfile.id != "restricted_default") {
                            cloudLibrarySyncService.recordCategoriesMutation(
                                profileId = activeHouseholdProfile.id,
                                sourceId = sourceId,
                                hiddenCategories = newHidden,
                                categoryOrder = newOrder
                            )
                        }
                    },
                    favoriteChannelIds = favoriteChannelIds,
                    recentChannelIds = recentChannelIds,
                    onRecordChannelWatched = { channel ->
                        recentChannelIds = userLibrary.recordChannelWatched(channel)
                    },
                    movieWatchlistIds = movieWatchlistIds,
                    followedSeriesIds = followedSeriesIds,
                    watchedItemIds = watchedItemIds,
                    homeHiddenSections = homeHiddenSections,
                    onOpenCustomizeHome = { showCustomizeHomeDialog = true },
                    xtreamCatalogs = xtreamCatalogs.values.toList(),
                    xtreamProfiles = playlists.filter { it.enabled && it.source is ContentSource.Xtream },
                    onToggleFavorite = { channel ->
                        val isFavorite = channel.libraryId()?.let(favoriteChannelIds::contains) == true
                        val newFavorite = !isFavorite
                        favoriteChannelIds = userLibrary.setChannelFavorite(channel, newFavorite)
                        if (currentUserId != null && activeHouseholdProfile.id != "restricted_default") {
                            cloudLibrarySyncService.recordLibraryMutation(
                                profileId = activeHouseholdProfile.id,
                                sourceId = channel.sourceId,
                                itemKind = "channel",
                                itemId = channel.sourceItemId,
                                isFavourite = newFavorite,
                                isWatchlisted = false,
                                isWatched = false
                            )
                        }
                    },
                    onToggleMovieWatchlist = { sourceId, movie ->
                        val isWatchlisted = userLibrary.isMovieWatchlisted(sourceId, movie, movieWatchlistIds)
                        val newWatchlisted = !isWatchlisted
                        movieWatchlistIds = userLibrary.setMovieWatchlisted(sourceId, movie, newWatchlisted)
                        if (currentUserId != null && activeHouseholdProfile.id != "restricted_default") {
                            cloudLibrarySyncService.recordLibraryMutation(
                                profileId = activeHouseholdProfile.id,
                                sourceId = sourceId,
                                itemKind = "movie",
                                itemId = movie.id.toString(),
                                isFavourite = newWatchlisted,
                                isWatchlisted = newWatchlisted,
                                isWatched = movie.libraryId(sourceId) in watchedItemIds
                            )
                        }
                    },
                    onToggleMovieWatched = { sourceId, movie ->
                        val key = movie.libraryId(sourceId)
                        val willBeWatched = key !in watchedItemIds
                        watchedItemIds = userLibrary.setItemWatched(key, willBeWatched)
                        if (currentUserId != null && activeHouseholdProfile.id != "restricted_default") {
                            cloudLibrarySyncService.recordLibraryMutation(
                                profileId = activeHouseholdProfile.id,
                                sourceId = sourceId,
                                itemKind = "movie",
                                itemId = movie.id.toString(),
                                isFavourite = key in movieWatchlistIds,
                                isWatchlisted = key in movieWatchlistIds,
                                isWatched = willBeWatched
                            )
                        }
                    },
                    onToggleSeriesFollow = { sourceId, series ->
                        val isFollowed = userLibrary.isSeriesFollowed(sourceId, series, followedSeriesIds)
                        val newFollowed = !isFollowed
                        followedSeriesIds = userLibrary.setSeriesFollowed(sourceId, series, newFollowed)
                        if (currentUserId != null && activeHouseholdProfile.id != "restricted_default") {
                            cloudLibrarySyncService.recordLibraryMutation(
                                profileId = activeHouseholdProfile.id,
                                sourceId = sourceId,
                                itemKind = "series",
                                itemId = series.id.toString(),
                                isFavourite = newFollowed,
                                isWatchlisted = newFollowed,
                                isWatched = false
                            )
                        }
                    },
                    onToggleEpisodeWatched = { sourceId, episode ->
                        val key = episode.libraryId(sourceId)
                        val willBeWatched = key !in watchedItemIds
                        watchedItemIds = userLibrary.setItemWatched(key, willBeWatched)
                        if (currentUserId != null && activeHouseholdProfile.id != "restricted_default") {
                            cloudLibrarySyncService.recordLibraryMutation(
                                profileId = activeHouseholdProfile.id,
                                sourceId = sourceId,
                                itemKind = "episode",
                                itemId = episode.id.toString(),
                                isFavourite = false,
                                isWatchlisted = false,
                                isWatched = willBeWatched
                            )
                        }
                    },
                    onMarkItemWatched = { key, watched ->
                        watchedItemIds = userLibrary.setItemWatched(key, watched)
                        if (currentUserId != null && activeHouseholdProfile.id != "restricted_default") {
                            val parts = key.split(":")
                            if (parts.size >= 3) {
                                val isFavOrWatch = key in movieWatchlistIds || key in followedSeriesIds
                                cloudLibrarySyncService.recordLibraryMutation(
                                    profileId = activeHouseholdProfile.id,
                                    sourceId = parts[1],
                                    itemKind = parts[0],
                                    itemId = parts[2],
                                    isFavourite = isFavOrWatch,
                                    isWatchlisted = isFavOrWatch,
                                    isWatched = watched
                                )
                            }
                        }
                    },
                    activeHouseholdProfile = activeHouseholdProfile,
                    onOpenProfiles = { showProfileSelectionDialog = true },
                    onOpenSettings = { tab ->
                        val status = profileRepository.getPinStatus()
                        if (activeHouseholdProfile.isChild && status.hasPin && !profileRepository.isAdultUnlocked()) {
                            pinPromptAction = {
                                settingsInitialTab = tab
                                showSettings = true
                            }
                        } else {
                            settingsInitialTab = tab
                            showSettings = true
                        }
                    },
                    onSaveMapping = { channel, xmltvCh, token ->
                        handleSaveEpgMapping(channel, xmltvCh, token)
                    },
                    onResetMapping = { channel, token ->
                        handleResetEpgMapping(channel, token)
                    },
                    onRequestAdultAuth = { onAuthorized ->
                        requestAdultAction(onAuthorized)
                    }
                )
            }

        if (showProfileSelectionDialog) {
            ProfileSelectionDialog(
                isMandatory = false,
                onProfileSelected = {
                    showProfileSelectionDialog = false
                    playbackResetGeneration++
                },
                onOpenAccount = {
                    settingsInitialTab = 4
                    showSettings = true
                },
                onDismiss = { showProfileSelectionDialog = false }
            )
        }

        if (showSettings) {
            SettingsDialog(
                initialTab = settingsInitialTab,
                onAccountSignedOut = {
                    currentUserId = null
                    playbackResetGeneration++
                    scope.launch(Dispatchers.IO) {
                        profileRepository.onAccountChanged(null)
                    }
                },
                onAccountSignedIn = {
                    val uid = accountRepository.currentUserId()
                    currentUserId = uid
                    playbackResetGeneration++
                    scope.launch(Dispatchers.IO) {
                        profileRepository.onAccountChanged(uid)
                        profileRepository.refresh()
                        val token = profileRepository.currentUnlockToken()
                        cloudSourceSyncService.restoreSourcesFromCloud(token)
                        cloudSourceSyncService.syncSourcesToCloud(token)
                        val restoredSubs = cloudSourceSyncService.restoreOpenSubtitlesFromCloud(token)
                        if (restoredSubs != null && (restoredSubs.apiKey.isNotBlank() || restoredSubs.username.isNotBlank())) {
                            openSubtitlesStore.saveSettings(restoredSubs)
                            withContext(Dispatchers.Main) {
                                openSubtitlesSettings = restoredSubs
                            }
                        } else if (openSubtitlesSettings.apiKey.isNotBlank() || openSubtitlesSettings.username.isNotBlank()) {
                            cloudSourceSyncService.syncOpenSubtitlesToCloud(openSubtitlesSettings, token)
                        }
                        val restoredTmdb = cloudSourceSyncService.restoreTmdbFromCloud(token)
                        if (restoredTmdb != null && restoredTmdb.apiKey.isNotBlank()) {
                            tmdbStore.saveSettings(restoredTmdb)
                            withContext(Dispatchers.Main) {
                                tmdbSettings = restoredTmdb
                            }
                        } else if (tmdbSettings.apiKey.isNotBlank()) {
                            cloudSourceSyncService.syncTmdbToCloud(tmdbSettings, token)
                        }
                        val restoredOmdb = cloudSourceSyncService.restoreOmdbFromCloud(token)
                        if (restoredOmdb != null && restoredOmdb.apiKey.isNotBlank()) {
                            omdbStore.saveSettings(restoredOmdb)
                            withContext(Dispatchers.Main) {
                                omdbSettings = restoredOmdb
                            }
                        } else if (omdbSettings.apiKey.isNotBlank()) {
                            cloudSourceSyncService.syncOmdbToCloud(omdbSettings, token)
                        }
                    }
                },
                playlists = playlists,
                activeProfile = activeProfile,
                appLanguage = appLanguage,
                onLanguageChanged = ::onLanguageChanged,
                startupDestination = startupDestination,
                onStartupDestinationChanged = { destination ->
                    startupDestination = destination
                    scope.launch(Dispatchers.IO) { settings.setStartupDestination(destination, activeHouseholdProfile.id) }
                    if (currentUserId != null && activeHouseholdProfile.id != "restricted_default") {
                        cloudLibrarySyncService.recordPreferencesMutation(
                            profileId = activeHouseholdProfile.id,
                            audioLang = preferredAudioLanguage,
                            subtitleLang = preferredSubtitleLanguage,
                            subFallback = subtitleFallbackEnglish,
                            startupDestination = destination.persistedValue,
                            epgTimeShiftHours = epgScheduleCorrectionMinutes / 60
                        )
                    }
                },
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
                telemetryEnabled = telemetryEnabled,
                onTelemetryEnabledChanged = { enabled ->
                    telemetryEnabled = enabled
                    scope.launch(Dispatchers.IO) { settings.setTelemetryEnabled(enabled) }
                },
                playbackMode = playbackMode,
                onPlaybackModeChanged = { mode ->
                    playbackMode = mode
                    scope.launch(Dispatchers.IO) { settings.setPlaybackMode(mode) }
                },
                preferredAudioLanguage = preferredAudioLanguage,
                onPreferredAudioLanguageChanged = onPreferredAudioLanguageChanged,
                preferredSubtitleLanguage = preferredSubtitleLanguage,
                onPreferredSubtitleLanguageChanged = onPreferredSubtitleLanguageChanged,
                subtitleFallbackEnglish = subtitleFallbackEnglish,
                onSubtitleFallbackEnglishChanged = { enabled ->
                    subtitleFallbackEnglish = enabled
                    scope.launch(Dispatchers.IO) { settings.setSubtitleFallbackEnglish(enabled, activeHouseholdProfile.id) }
                    if (currentUserId != null && activeHouseholdProfile.id != "restricted_default") {
                        cloudLibrarySyncService.recordPreferencesMutation(
                            profileId = activeHouseholdProfile.id,
                            audioLang = preferredAudioLanguage,
                            subtitleLang = preferredSubtitleLanguage,
                            subFallback = enabled,
                            startupDestination = startupDestination.persistedValue,
                            epgTimeShiftHours = epgScheduleCorrectionMinutes / 60
                        )
                    }
                },
                openSubtitlesSettings = openSubtitlesSettings,
                onOpenSubtitlesSettingsChanged = { newSettings ->
                    openSubtitlesSettings = newSettings
                    scope.launch(Dispatchers.IO) {
                        openSubtitlesStore.saveSettings(newSettings)
                        if (currentUserId != null) {
                            cloudSourceSyncService.syncOpenSubtitlesToCloud(newSettings)
                        }
                    }
                },
                tmdbSettings = tmdbSettings,
                onTmdbSettingsChanged = { newTmdb ->
                    tmdbSettings = newTmdb
                    scope.launch(Dispatchers.IO) {
                        tmdbStore.saveSettings(newTmdb)
                        if (currentUserId != null && newTmdb.apiKey.isNotBlank()) {
                            cloudSourceSyncService.syncTmdbToCloud(newTmdb)
                        }
                    }
                },
                introDbSettings = introDbSettings,
                onIntroDbSettingsChanged = { newIntroDb ->
                    introDbSettings = newIntroDb
                    introDbStore.saveSettings(newIntroDb)
                },
                omdbSettings = omdbSettings,
                onOmdbSettingsChanged = { newOmdb ->
                    omdbSettings = newOmdb
                    scope.launch(Dispatchers.IO) {
                        omdbStore.saveSettings(newOmdb)
                        if (currentUserId != null && newOmdb.apiKey.isNotBlank()) {
                            cloudSourceSyncService.syncOmdbToCloud(newOmdb)
                        }
                    }
                },
                hideAdultContent = hideAdultContent,
                isChildProfile = activeHouseholdProfile.isChild,
                onHideAdultContentChanged = handleHideAdultContentChanged,
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
                epgScheduleCorrectionMinutes = epgScheduleCorrectionMinutes,
                epgSourceTimezone = epgSourceTimezone,
                epgMappings = epgMappings,
                epgLoadedDays = epgLoadedDays,
                playlist = playlist,
                epgGuide = epgGuide,
                hiddenCategories = hiddenCategories,
                loading = loading,
                onDismiss = {
                    showSettings = false
                    if (currentUserId != null && (openSubtitlesSettings.apiKey.isNotBlank() || openSubtitlesSettings.username.isNotBlank())) {
                        scope.launch(Dispatchers.IO) {
                            cloudSourceSyncService.syncOpenSubtitlesToCloud(openSubtitlesSettings)
                        }
                    }
                    if (currentUserId != null && tmdbSettings.apiKey.isNotBlank()) {
                        scope.launch(Dispatchers.IO) {
                            cloudSourceSyncService.syncTmdbToCloud(tmdbSettings)
                        }
                    }
                    if (currentUserId != null && omdbSettings.apiKey.isNotBlank()) {
                        scope.launch(Dispatchers.IO) {
                            cloudSourceSyncService.syncOmdbToCloud(omdbSettings)
                        }
                    }
                },
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
                onSendPlaylistToTv = { profile ->
                    sendToTvProfile = profile
                    showSendToTvDialog = true
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
                    epgScheduleCorrectionMinutes = shift * 60
                    settings.saveEpgScheduleCorrectionMinutes(shift * 60, activeHouseholdProfile.id)
                    if (currentUserId != null && activeHouseholdProfile.id != "restricted_default") {
                        cloudLibrarySyncService.recordPreferencesMutation(
                            profileId = activeHouseholdProfile.id,
                            audioLang = preferredAudioLanguage,
                            subtitleLang = preferredSubtitleLanguage,
                            subFallback = subtitleFallbackEnglish,
                            startupDestination = startupDestination.persistedValue,
                            epgTimeShiftHours = shift
                        )
                    }
                },
                onEpgScheduleCorrectionChanged = { minutes ->
                    epgScheduleCorrectionMinutes = minutes
                    settings.saveEpgScheduleCorrectionMinutes(minutes, activeHouseholdProfile.id)
                    if (currentUserId != null && activeHouseholdProfile.id != "restricted_default") {
                        cloudLibrarySyncService.recordPreferencesMutation(
                            profileId = activeHouseholdProfile.id,
                            audioLang = preferredAudioLanguage,
                            subtitleLang = preferredSubtitleLanguage,
                            subFallback = subtitleFallbackEnglish,
                            startupDestination = startupDestination.persistedValue,
                            epgTimeShiftHours = minutes / 60
                        )
                    }
                    val accountState = accountRepository.getAccountState()
                    if (accountState is com.fortresstv.player.account.AccountState.AuthenticatedOnline) {
                        scope.launch(Dispatchers.IO) {
                            cloudSourceSyncService.syncSourcesToCloud(profileRepository.currentUnlockToken())
                        }
                    }
                },
                onEpgSourceTimezoneChanged = { zoneId ->
                    epgSourceTimezone = zoneId
                    settings.saveEpgSourceTimezone(zoneId)
                    val currentEpg = epgSource ?: settings.epgSource()
                    currentEpg?.let { loadEpg(it, save = false, forceRefresh = true) }
                    val accountState = accountRepository.getAccountState()
                    if (accountState is com.fortresstv.player.account.AccountState.AuthenticatedOnline) {
                        scope.launch(Dispatchers.IO) {
                            cloudSourceSyncService.syncSourcesToCloud(profileRepository.currentUnlockToken())
                        }
                    }
                },
                onSaveMapping = { channel, xmltvCh, token ->
                    handleSaveEpgMapping(channel, xmltvCh, token)
                },
                onResetMapping = { channel, token ->
                    handleResetEpgMapping(channel, token)
                },
                onRequestAdultAuth = { onAuthorized ->
                    requestAdultAction(onAuthorized)
                },
                onEpgLoadedDaysChanged = { days ->
                    val clamped = days.coerceIn(1, 7)
                    epgLoadedDays = clamped
                    settings.saveEpgLoadedDays(clamped)
                    val currentEpg = epgSource ?: settings.epgSource()
                    currentEpg?.let { loadEpg(it, save = false, forceRefresh = false) }
                },
                onOpenCustomizeHome = { showCustomizeHomeDialog = true }
            )
        }

        if (showCustomizeHomeDialog) {
            com.fortresstv.player.ui.CustomizeHomeDialog(
                hiddenSections = homeHiddenSections,
                onToggleSection = { sectionId ->
                    val updated = if (sectionId in homeHiddenSections) {
                        homeHiddenSections - sectionId
                    } else {
                        homeHiddenSections + sectionId
                    }
                    homeHiddenSections = updated
                    scope.launch(Dispatchers.IO) {
                        settings.setHomeHiddenSections(updated, activeHouseholdProfile.id)
                    }
                },
                onDismiss = { showCustomizeHomeDialog = false }
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
                    val accountState = AccountRepository.get(context).getAccountState()
                    if (accountState is com.fortresstv.player.account.AccountState.AuthenticatedOnline) {
                        scope.launch(Dispatchers.IO) {
                            CloudSourceSyncService.get(context).deleteSourceFromCloud(profile.id, profileRepository.currentUnlockToken())
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
                text = { Text(strings.removeEpgConfirmation) },
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
                        val accountState = accountRepository.getAccountState()
                        if (accountState is com.fortresstv.player.account.AccountState.AuthenticatedOnline) {
                            scope.launch(Dispatchers.IO) {
                                cloudSourceSyncService.deleteCustomEpgFromCloud(profileRepository.currentUnlockToken())
                            }
                        }
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
                    val accountState = AccountRepository.get(context).getAccountState()
                    if (accountState is com.fortresstv.player.account.AccountState.AuthenticatedOnline) {
                        scope.launch(Dispatchers.IO) {
                            CloudSourceSyncService.get(context).syncSourcesToCloud(profileRepository.currentUnlockToken())
                        }
                    }
                },
                onPickFile = {
                    showAddPlaylistDialog = false
                    addPlaylistFilePicker.launch(arrayOf("audio/x-mpegurl", "application/x-mpegURL", "text/*", "*/*"))
                },
                  onSendFromPhone = {
                    showTransferDialog = true
                  }
              )
          }

        if (showSendToTvDialog) {
            SendToTvDialog(
                profiles = playlists,
                initialProfile = sendToTvProfile,
                directTarget = directTransferTarget,
                onDismiss = {
                    showSendToTvDialog = false
                    sendToTvProfile = null
                    directTransferTarget = null
                }
            )
        }

        if (showTransferDialog) {
            TvReceiveSourceDialog(
                onDismiss = { showTransferDialog = false },
                onSourceConfirmed = { payload ->
                    showTransferDialog = false
                    val displayName = payload.name.ifBlank {
                        when (payload.kind) {
                            TransferKind.XTREAM -> "Xtream"
                            TransferKind.M3U -> payload.url?.substringAfterLast('/')?.ifBlank { "Remote Playlist" } ?: "Remote Playlist"
                        }
                    }
                    when (payload.kind) {
                        TransferKind.M3U -> {
                            val profile = PlaylistProfile(
                                id = UUID.randomUUID().toString(),
                                name = displayName,
                                source = ContentSource.RemoteUrl(payload.url!!.trim(), displayName),
                                order = (playlists.maxOfOrNull { it.order } ?: -1) + 1
                            )
                            loadPlaylistProfile(profile, forceRefresh = true)
                        }
                        TransferKind.XTREAM -> {
                            val profile = PlaylistProfile(
                                id = UUID.randomUUID().toString(),
                                name = displayName,
                                source = ContentSource.Xtream(
                                    payload.url!!.trim(), displayName,
                                    payload.username ?: "", payload.password ?: ""
                                ),
                                enabled = true,
                                role = SourceRole.EVERYTHING,
                                order = (playlists.maxOfOrNull { it.order } ?: -1) + 1
                            )
                            loadXtreamProfile(profile, forceRefresh = true)
                    val accountState = AccountRepository.get(context).getAccountState()
                    if (accountState is com.fortresstv.player.account.AccountState.AuthenticatedOnline) {
                        scope.launch(Dispatchers.IO) {
                            CloudSourceSyncService.get(context).syncSourcesToCloud(profileRepository.currentUnlockToken())
                        }
                    }
                        }
                    }
                    payload.epgUrl?.takeIf { it.isNotBlank() }?.let { transferredEpgUrl ->
                        val transferredEpg = ContentSource.RemoteUrl(
                            transferredEpgUrl.trim(),
                            transferredEpgUrl.substringAfterLast('/').ifBlank { "EPG" }
                        )
                        loadEpg(transferredEpg, save = true, forceRefresh = true)
                    }
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
                            hideAdultContent = false
                            scope.launch(Dispatchers.IO) { settings.setHideAdultContent(false) }
                            if (activeHouseholdProfile.isAdult && activeHouseholdProfile.hideAdultContent) {
                                scope.launch(Dispatchers.IO) {
                                    runCatching {
                                        profileRepository.setProfileHideAdultContent(
                                            activeHouseholdProfile.id,
                                            false,
                                            profileRepository.currentUnlockToken()
                                        )
                                    }
                                }
                            }
                        }
                        PinDialogMode.UNLOCK_SETTINGS -> {}
                    }
                    activePinDialog = null
                },
                onDismiss = { activePinDialog = null }
            )
        }

        if (pinPromptAction != null) {
            ParentalPinDialog(
                mode = HouseholdPinDialogMode.VERIFY,
                onSuccess = { _ ->
                    val action = pinPromptAction
                    pinPromptAction = null
                    action?.invoke()
                },
                onDismiss = {
                    pinPromptAction = null
                }
            )
        }

        if (pendingAdultTokenAction != null) {
            ParentalPinDialog(
                mode = HouseholdPinDialogMode.VERIFY,
                onSuccess = { unlockToken ->
                    val action = pendingAdultTokenAction
                    pendingAdultTokenAction = null
                    action?.invoke(unlockToken)
                },
                onDismiss = {
                    pendingAdultTokenAction = null
                }
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
private fun ProfileLoadingScreen(
    profile: HouseholdProfile,
    statusText: String
) {
    val strings = LocalAppStrings.current
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (isCompact) 14.dp else 18.dp)
            ) {
                ShieldMark(Modifier.size(if (isCompact) 100.dp else 140.dp))

                val displayName = profile.name.takeIf { it.isNotBlank() && profile.id != "restricted_default" } ?: ""
                val welcomeText = if (displayName.isNotBlank()) strings.welcomeUser(displayName) else strings.appName

                Text(
                    text = welcomeText,
                    style = if (isCompact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(Modifier.height(4.dp))

                CircularProgressIndicator(
                    modifier = Modifier.size(if (isCompact) 32.dp else 40.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WelcomeScreen(
    loading: Boolean,
    showActions: Boolean,
    error: String?,
    onRetry: (() -> Unit)?,
    onAddSource: () -> Unit,
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
    epgTimeShiftHours: Int = 0,
    epgScheduleCorrectionMinutes: Int = epgTimeShiftHours * 60,
    epgMappings: Map<String, com.fortresstv.player.epg.EpgChannelMapping> = emptyMap(),
    epgLoadedDays: Int = 3,
    epgRepository: EpgRepository? = null,
    favoriteChannelIds: Set<String> = emptySet(),
    onToggleFavorite: (PlaylistChannel) -> Unit = {},
    categoryOrder: List<String> = emptyList(),
    hiddenCategories: Set<String> = emptySet(),
    initialCategory: String? = null,
    onBack: () -> Unit,
    onWatchChannel: (PlaylistChannel) -> Unit,
    onWatchCatchup: ((PlaylistChannel, EpgProgramme) -> Unit)? = null,
    progressEntries: List<VodProgressEntry> = emptyList(),
    dockedPlayer: (@Composable () -> Unit)? = null,
    activeChannel: PlaylistChannel? = null,
    onDismissDockedPlayer: (() -> Unit)? = null
) {
    val strings = LocalAppStrings.current
    var isDockedPlayerVisible by remember { mutableStateOf(true) }
    val guideLocale = remember(strings.languageTag) { Locale.forLanguageTag(strings.languageTag) }
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    var now by remember { mutableStateOf(Instant.now()) }
    val zone = remember { ZoneId.systemDefault() }
    val today = remember(now, zone) { now.atZone(zone).toLocalDate() }
    val dates = remember(today, epgLoadedDays) { List(epgLoadedDays.coerceIn(1, 7)) { today.plusDays(it.toLong()) } }
    var selectedDate by remember { mutableStateOf(today) }
    var previousToday by remember { mutableStateOf(today) }
    LaunchedEffect(today) {
        if (selectedDate == previousToday) {
            selectedDate = today
        }
        previousToday = today
    }
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
    val shiftMillis = epgScheduleCorrectionMinutes * 60_000L

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

    var resolvedChannelMap by remember(playlist.channels, epgGuide, epgMappings) {
        mutableStateOf<Map<PlaylistChannel, String?>>(emptyMap())
    }
    var channelResolutionReady by remember(playlist.channels, epgGuide, epgMappings) { mutableStateOf(epgGuide == null) }

    LaunchedEffect(playlist.channels, epgGuide, epgMappings) {
        val guide = epgGuide
        if (guide == null) {
            resolvedChannelMap = emptyMap()
            channelResolutionReady = true
        } else {
            channelResolutionReady = false
            resolvedChannelMap = withContext(Dispatchers.Default) {
                playlist.channels.associateWith { channel ->
                    val manualXmltvId = epgMappings["${channel.sourceId}:${channel.sourceItemId}"]?.xmltvChannelId
                    guide.resolveChannelId(channel.tvgId, channel.name, manualXmltvId)
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
                ?: (if (dayProgrammesMap == null) epgGuide?.findProgrammes(channel.tvgId, channel.name, epgMappings["${channel.sourceId}:${channel.sourceItemId}"]?.xmltvChannelId).orEmpty() else emptyList())
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
        val baseProgrammes = if (date == today) epgGuide?.programmes else loadedDay?.programmes
        val adjacentDay = when {
            shiftMillis > 0L -> epgRepository?.loadDay(targetEpochDay - 1)
            shiftMillis < 0L -> epgRepository?.loadDay(targetEpochDay + 1)
            else -> null
        }
        val dayData = if (adjacentDay != null && adjacentDay.programmes.isNotEmpty()) {
            val combined = HashMap<String, MutableList<EpgProgramme>>()
            baseProgrammes?.forEach { (k, v) -> combined.getOrPut(k) { ArrayList() }.addAll(v) }
            adjacentDay.programmes.forEach { (k, v) -> combined.getOrPut(k) { ArrayList() }.addAll(v) }
            combined.mapValues { (_, list) -> list.distinctBy { it.startEpochMilli }.sortedBy { it.startEpochMilli } }
        } else {
            baseProgrammes
        }
        val partitionLoadMillis = (android.os.SystemClock.elapsedRealtimeNanos() - dayLoadStartedNanos) / 1_000_000.0

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
                    val targetEpochDay = date.toEpochDay()
                    val loadedDay = repository.loadDay(targetEpochDay) ?: return@forEach
                    val adjacentDay = when {
                        shiftMillis > 0L -> repository.loadDay(targetEpochDay - 1)
                        shiftMillis < 0L -> repository.loadDay(targetEpochDay + 1)
                        else -> null
                    }
                    val dayData = if (adjacentDay != null && adjacentDay.programmes.isNotEmpty()) {
                        val combined = HashMap<String, MutableList<EpgProgramme>>()
                        loadedDay.programmes.forEach { (k, v) -> combined.getOrPut(k) { ArrayList() }.addAll(v) }
                        adjacentDay.programmes.forEach { (k, v) -> combined.getOrPut(k) { ArrayList() }.addAll(v) }
                        combined.mapValues { (_, list) -> list.distinctBy { it.startEpochMilli }.sortedBy { it.startEpochMilli } }
                    } else {
                        loadedDay.programmes
                    }
                    val dayIndex = computeDayBlocks(date, dayData)
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
        Box(Modifier.fillMaxSize()) {
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
                            val isPast = adjustedStop <= now.toEpochMilli()
                            val canCatchup = isPast && channel.hasCatchup && onWatchCatchup != null
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (canCatchup) {
                                        onWatchCatchup(channel, programme)
                                    } else {
                                        onWatchChannel(channel)
                                    }
                                },
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
                                    val canonicalChannel = channel.tvgId?.trim()?.takeIf { it.isNotEmpty() }
                                        ?: channel.name.trim().lowercase().takeIf { it.isNotEmpty() }
                                        ?: channel.sourceItemId
                                    val catchupItemId = CatchupProgressKey(canonicalChannel, Instant.ofEpochMilli(programme.startEpochMilli)).toItemId()
                                    val hasCatchupProgress = progressEntries.any {
                                        it.kind == VodProgressKind.CATCHUP && (
                                            it.itemId == catchupItemId ||
                                            it.itemId.contains("${programme.startEpochMilli}") ||
                                            it.key.contains(canonicalChannel)
                                        )
                                    }
                                    Text(
                                        when {
                                            isLiveNow -> strings.watchNow
                                            canCatchup && hasCatchupProgress -> strings.resume
                                            canCatchup -> strings.watchCatchup
                                            else -> strings.watchLiveChannel
                                        },
                                        fontSize = 12.sp
                                    )
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

        if (isDockedPlayerVisible && dockedPlayer != null && activeChannel != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = if (isCompact) 12.dp else 24.dp,
                            bottom = if (selectedProgramme != null) (if (isCompact) 130.dp else 150.dp) else (if (isCompact) 16.dp else 24.dp)
                        )
                        .width(if (isCompact) 210.dp else 290.dp)
                ) {
                    Column {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                        ) {
                            dockedPlayer()
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                activeChannel.name,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        onWatchChannel(activeChannel)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Fullscreen,
                                        contentDescription = strings.fullscreen,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        isDockedPlayerVisible = false
                                        onDismissDockedPlayer?.invoke()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = strings.closeDockedPlayer,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
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



@Composable
private fun SleepTimerConfirmationDialog(
    secondsRemaining: Int,
    canContinue: Boolean,
    canPlayNextThenStop: Boolean,
    onStop: () -> Unit,
    onContinue: () -> Unit,
    onPlayNextThenStop: () -> Unit,
    onSetTimer: (Int) -> Unit
) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = {},
        title = { Text(strings.areYouStillWatching, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings.stillWatchingCountdown(secondsRemaining))
                if (canPlayNextThenStop) {
                    Button(onClick = onPlayNextThenStop, modifier = Modifier.fillMaxWidth()) {
                        Text(strings.playNextThenStop)
                    }
                }
                if (canContinue) {
                    OutlinedButton(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                        Text(strings.continueWithoutTimer)
                    }
                }
                Text(strings.setAnotherTimer, fontWeight = FontWeight.SemiBold)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(15, 30, 45, 60, 90, 120).forEach { minutes ->
                        OutlinedButton(onClick = { onSetTimer(minutes) }) {
                            Text("$minutes min")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onStop) { Text(strings.stopPlayback) }
        }
    )
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun VodPlayerDialog(
    request: VodPlaybackRequest,
    initialPositionMs: Long,
    preferredAudioLanguage: String = "default",
    preferredSubtitleLanguage: String = "off",
    subtitleFallbackEnglish: Boolean = true,
    onPreferredAudioLanguageChanged: ((String) -> Unit)? = null,
    onPreferredSubtitleLanguageChanged: ((String) -> Unit)? = null,
    openSubtitlesSettings: OpenSubtitlesSettings = OpenSubtitlesSettings(),
    loadNextEpisode: (suspend () -> VodPlaybackRequest?)?,
    loadAllEpisodes: (suspend () -> List<VodPlaybackRequest>)? = null,
    onPlayNext: (VodPlaybackRequest) -> Unit,
    onSaveProgress: (Long, Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    val subtitleCache = remember { SubtitleDiskCache(context.applicationContext) }
    var attachedSubtitles by remember(request.key) { mutableStateOf<List<Pair<OpenSubtitlesItem, java.io.File>>>(emptyList()) }
    var pendingSubtitleOverrideLabel by remember(request.key) { mutableStateOf<String?>(null) }
    var showOpenSubtitlesSearchDialog by remember { mutableStateOf(false) }

    val playbackMode = LocalPlaybackMode.current
    val player = remember(request.streamUrl, playbackMode) {
        FortressPlayerFactory.create(context, PlaybackContentType.VOD, playbackMode)
    }
    val telemetryStore = remember { com.fortresstv.player.telemetry.TelemetryStore.get(context.applicationContext) }
    val currentHouseholdId = remember { com.fortresstv.player.account.AccountRepository.get(context.applicationContext).currentUserId() ?: "local" }
    val currentProfileId = remember { com.fortresstv.player.profile.ProfileRepository.get(context.applicationContext).activeProfile.value.id }
    val streamKind = if (request.kind == VodProgressKind.CATCHUP) {
        com.fortresstv.player.telemetry.StreamKindWire.CATCHUP
    } else {
        com.fortresstv.player.telemetry.StreamKindWire.VOD
    }
    val telemetryChannelId = if (request.kind == VodProgressKind.CATCHUP) request.itemId else ""
    val channelHealthRepo = remember { com.fortresstv.player.telemetry.ChannelHealthRepository.get(context.applicationContext) }
    val tracker = remember(request.key, player) {
        com.fortresstv.player.telemetry.PlaybackHealthTracker(
            householdId = currentHouseholdId,
            profileId = currentProfileId,
            channelId = telemetryChannelId,
            streamKind = streamKind,
            providerId = request.sourceId,
            onActiveMetricsUpdated = { startMs, stalls, stallMs, errCode, reg ->
                channelHealthRepo.recordActiveMetrics(
                    channelId = telemetryChannelId,
                    startupMs = startMs,
                    stallCount = stalls,
                    totalStallMs = stallMs,
                    errorCode = errCode,
                    region = reg
                )
            },
            onFinalized = { summary ->
                channelHealthRepo.recordSummary(summary)
                telemetryStore.enqueue(summary)
            }
        )
    }
    val vodNetworkGeneration = rememberPlaybackNetworkGeneration()
    var resumePositionMs by remember(request.key) { mutableLongStateOf(initialPositionMs) }
    var resumeDecisionMade by remember(request.key) { mutableStateOf(initialPositionMs < 10_000L) }
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var nextEpisode by remember(request.key) { mutableStateOf<VodPlaybackRequest?>(null) }
    var loadingNextEpisode by remember(request.key) { mutableStateOf(false) }

    var controlsVisible by remember { mutableStateOf(true) }
    var autoPlayNext by remember { mutableStateOf(true) }
    var autoPlayCountdown by remember(request.key) { mutableIntStateOf(-1) }
    var autoPlayCountdownDismissed by remember(request.key) { mutableStateOf(false) }

    var currentPositionMs by remember(request.key) { mutableLongStateOf(initialPositionMs.coerceAtLeast(0L)) }
    var totalDurationMs by remember(request.key) { mutableLongStateOf(0L) }
    var isPlaying by remember(request.key) { mutableStateOf(true) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderDragPositionMs by remember { mutableFloatStateOf(0f) }

    val sleepTimerActivity = context.findActivity() as? MainActivity
    val sleepTimerMinutes = sleepTimerActivity?.sleepTimerState?.minutes
    val sleepTimerRemainingText = sleepTimerActivity?.sleepTimerState?.remainingMinutes?.let { "${it}m" }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    var currentTracks by remember(request.key) { mutableStateOf<Tracks?>(null) }
    var userManuallySelectedTracks by remember(request.key) { mutableStateOf(false) }
    var showTracksDialog by remember { mutableStateOf(false) }
    var selectedTrackTab by remember { mutableIntStateOf(0) }
    var seekOverlayPositionMs by remember { mutableLongStateOf(-1L) }
    var seekOverlayDeltaMs by remember { mutableLongStateOf(0L) }

    var allEpisodes by remember(request.seriesId) { mutableStateOf<List<VodPlaybackRequest>>(emptyList()) }
    var showEpisodesDrawer by remember { mutableStateOf(false) }
    var selectedDrawerSeason by remember(request.season) { mutableIntStateOf(request.season ?: 1) }

    LaunchedEffect(vodNetworkGeneration) {
        if (vodNetworkGeneration > 0 && player.currentMediaItem != null &&
            (player.playbackState == Player.STATE_BUFFERING || player.playerError != null)
        ) {
            val position = player.currentPosition.coerceAtLeast(0L)
            delay(750)
            player.prepare()
            player.seekTo(position)
            player.play()
        }
    }

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

    LaunchedEffect(request.key, loadNextEpisode) {
        if (loadNextEpisode != null && nextEpisode == null && !loadingNextEpisode) {
            loadingNextEpisode = true
            nextEpisode = runCatching { loadNextEpisode() }.getOrNull()
            loadingNextEpisode = false
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
    val vodSkipIntroFocusRequester = remember { FocusRequester() }
    val tmdbSettings = LocalTmdbSettings.current
    val introDbSettings = LocalTheIntroDbSettings.current
    var episodeSegments by remember(request.key) { mutableStateOf<EpisodeSegments?>(null) }
    var hasAutoSkippedIntro by remember(request.key) { mutableStateOf(false) }
    var hasAutoSkippedRecap by remember(request.key) { mutableStateOf(false) }

    LaunchedEffect(request.key, introDbSettings.enabled) {
        if (!introDbSettings.enabled || request.kind != VodProgressKind.EPISODE || request.season == null || request.episode == null) {
            episodeSegments = null
            return@LaunchedEffect
        }
        val client = TheIntroDbClient.getInstance(context.applicationContext)
        val tmdbId = request.tmdbId ?: if (tmdbSettings.enabled && tmdbSettings.apiKey.isNotBlank() && !request.seriesName.isNullOrBlank()) {
            TmdbMetadataResolver.resolveSeries(
                context = context,
                settings = tmdbSettings,
                rawTitle = request.seriesName,
                year = null
            )?.tmdbId?.toString()
        } else null

        val segments = client.getSegments(
            tmdbId = tmdbId,
            imdbId = request.imdbId,
            season = request.season,
            episode = request.episode
        )
        episodeSegments = segments
    }

    val activeSegment = remember(episodeSegments, currentPositionMs, totalDurationMs) {
        episodeSegments?.activeSegment(currentPositionMs, totalDurationMs)
    }

    LaunchedEffect(activeSegment, introDbSettings) {
        val segment = activeSegment ?: return@LaunchedEffect
        if (segment.type == SegmentType.INTRO && introDbSettings.autoSkipIntro && !hasAutoSkippedIntro) {
            hasAutoSkippedIntro = true
            segment.endMs?.let { player.seekTo(it) }
        } else if (segment.type == SegmentType.RECAP && introDbSettings.autoSkipRecap && !hasAutoSkippedRecap) {
            hasAutoSkippedRecap = true
            segment.endMs?.let { player.seekTo(it) }
        }
    }
    val vodInteractionSignal = remember { Channel<Unit>(Channel.CONFLATED) }

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

    LaunchedEffect(controlsVisible, showEpisodesDrawer, showSleepTimerDialog, showTracksDialog) {
        if (controlsVisible && !showEpisodesDrawer && !showSleepTimerDialog && !showTracksDialog) {
            val timeoutMs = if (isTv) 4_000L else 3_000L
            while (true) {
                val ping = withTimeoutOrNull(timeoutMs) {
                    vodInteractionSignal.receive()
                }
                if (ping == null) {
                    controlsVisible = false
                    break
                }
            }
        }
    }

    val audioTracks = remember(currentTracks) { extractAudioTracks(currentTracks) }
    val subtitleTracks = remember(currentTracks) { extractSubtitleTracks(currentTracks) }
    val videoTracks = remember(currentTracks) { extractVideoTracks(currentTracks) }

    LaunchedEffect(autoPlayCountdown, nextEpisode, isPlaying, sleepTimerMinutes) {
        if (sleepTimerMinutes == -1) {
            autoPlayCountdown = -1
            return@LaunchedEffect
        }
        if (autoPlayCountdown > 0 && isPlaying) {
            delay(1000)
            autoPlayCountdown--
            if (autoPlayCountdown == 0) {
                sleepTimerActivity?.playbackSleepTimer?.checkDeadline()
                if (sleepTimerActivity?.playbackSleepTimer?.expired == true) return@LaunchedEffect
                nextEpisode?.let { next ->
                    onPlayNext(next)
                }
            }
        }
    }

    LaunchedEffect(currentPositionMs, totalDurationMs, nextEpisode, autoPlayNext, autoPlayCountdownDismissed, isPlaying, sleepTimerMinutes) {
        if (isPlaying && shouldStartNextEpisodeCountdown(
                positionMs = currentPositionMs,
                durationMs = totalDurationMs,
                autoPlayEnabled = autoPlayNext,
                dismissed = autoPlayCountdownDismissed,
                nextEpisodeAvailable = nextEpisode != null,
                currentCountdown = autoPlayCountdown,
                stopAtEnd = sleepTimerMinutes == -1
            )
        ) {
            autoPlayCountdown = NEXT_EPISODE_COUNTDOWN_SECONDS
        }
    }

    val mainActivity = context as? MainActivity
    val inPictureInPicture = mainActivity?.pictureInPictureMode == true
    DisposableEffect(player) {
        val unregisterSleepTimerPlayer = sleepTimerActivity?.registerSleepTimerPlayer(player, onDismiss)
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
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) {
                    onSaveProgress(player.currentPosition.coerceAtLeast(0L), player.duration.coerceAtLeast(0L))
                }
            }
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                if (state == Player.STATE_ENDED) {
                    onSaveProgress(player.duration.coerceAtLeast(0L), player.duration.coerceAtLeast(0L))
                    sleepTimerActivity?.playbackSleepTimer?.mediaEnded()
                    if (sleepTimerActivity?.playbackSleepTimer?.expired == true) return
                    if (loadNextEpisode != null && !loadingNextEpisode && nextEpisode == null) {
                        loadingNextEpisode = true
                        scope.launch {
                            val next = runCatching { loadNextEpisode() }.getOrNull()
                            loadingNextEpisode = false
                            nextEpisode = next
                        }
                    }
                }
            }
            override fun onTracksChanged(tracks: Tracks) {
                currentTracks = tracks
                val pendingLabel = pendingSubtitleOverrideLabel
                if (pendingLabel != null) {
                    var targetGroup: Tracks.Group? = null
                    var targetIndex = -1
                    for (group in tracks.groups) {
                        if (group.type == C.TRACK_TYPE_TEXT) {
                            for (i in 0 until group.length) {
                                val format = group.getTrackFormat(i)
                                if (format.label == pendingLabel || format.label?.startsWith("OpenSubtitles") == true) {
                                    targetGroup = group
                                    targetIndex = i
                                    break
                                }
                            }
                        }
                        if (targetGroup != null) break
                    }
                    if (targetGroup != null) {
                        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                            .setOverrideForType(TrackSelectionOverride(targetGroup.mediaTrackGroup, targetIndex))
                            .build()
                        pendingSubtitleOverrideLabel = null
                    }
                }
            }
            override fun onPlayerError(error: PlaybackException) { playbackError = error.message ?: strings.playbackError }
        }
        player.addListener(listener)
        player.addAnalyticsListener(tracker)
        onDispose {
            tracker.finalizeSession()
            player.removeAnalyticsListener(tracker)
            mainActivity?.updatePictureInPictureState(enabled = false, fullscreenPlaying = false)
            mainActivity?.setOnPictureInPictureDismissedListener(null)
            val pos = maxOf(player.currentPosition.coerceAtLeast(0L), currentPositionMs)
            val dur = maxOf(player.duration.coerceAtLeast(0L), totalDurationMs)
            if (pos >= 10_000L) {
                onSaveProgress(pos, dur)
            }
            player.removeListener(listener)
            unregisterSleepTimerPlayer?.invoke()
            player.release()
        }
    }

    fun attachAndSelectSubtitle(item: OpenSubtitlesItem, file: java.io.File) {
        userManuallySelectedTracks = true
        val updated = attachedSubtitles.filter { it.first.fileId != item.fileId } + (item to file)
        attachedSubtitles = updated
        val label = "OpenSubtitles · ${item.release}"
        pendingSubtitleOverrideLabel = label
        val detected = if (item.language.lowercase().startsWith("pt")) "pt" else item.language.lowercase().take(2)
        onPreferredSubtitleLanguageChanged?.invoke(detected)
        val currentPos = player.currentPosition
        val wasPlaying = player.playWhenReady
        val newMediaItem = MediaItem.Builder()
            .setUri(request.streamUrl)
            .setSubtitleConfigurations(
                updated.map { (subItem, f) ->
                    MediaItem.SubtitleConfiguration.Builder(android.net.Uri.fromFile(f))
                        .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                        .setLanguage(subItem.language)
                        .setLabel("OpenSubtitles · ${subItem.release}")
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                }
            )
            .build()
        player.setMediaItem(newMediaItem, currentPos)
        player.prepare()
        player.playWhenReady = wasPlaying
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setPreferredTextLanguage(item.language)
            .build()
    }

    var autoSearchAttempted by remember(request.key) { mutableStateOf(false) }
    LaunchedEffect(currentTracks, openSubtitlesSettings, preferredSubtitleLanguage) {
        if (autoSearchAttempted) return@LaunchedEffect
        if (currentTracks == null) return@LaunchedEffect

        val subTracks = TrackLanguageMatcher.extractSubtitleTracks(currentTracks)
        val shouldSearch = shouldAutoSearchOpenSubtitles(
            enabled = openSubtitlesSettings.enabled,
            autoSearch = openSubtitlesSettings.autoSearch,
            apiKey = openSubtitlesSettings.apiKey,
            username = openSubtitlesSettings.username,
            password = openSubtitlesSettings.password,
            preferredLanguage = preferredSubtitleLanguage,
            availableTracks = subTracks.map { it.language to it.label }
        )
        if (!shouldSearch) return@LaunchedEffect

        autoSearchAttempted = true
        val queryTitle = request.seriesName ?: request.title
        val languages = openSubtitlesLanguageQuery(
            preferredSubtitleLanguage,
            openSubtitlesSettings.preferPtPt
        ) ?: return@LaunchedEffect
        val client = OpenSubtitlesClient(
            apiKey = openSubtitlesSettings.apiKey,
            username = openSubtitlesSettings.username,
            password = openSubtitlesSettings.password
        )
        val loginResult = client.login()
        if (loginResult.isFailure) return@LaunchedEffect
        val results = client.searchSubtitles(
            query = queryTitle,
            season = request.season,
            episode = request.episode,
            languages = languages
        ).getOrDefault(emptyList())

        if (results.isNotEmpty()) {
            val sorted = if (openSubtitlesSettings.preferPtPt && preferredSubtitleLanguage == "pt") {
                results.sortedWith(
                    compareByDescending<OpenSubtitlesItem> { it.language == "pt-pt" }
                        .thenByDescending { it.downloads }
                )
            } else {
                results.sortedByDescending { it.downloads }
            }
            val best = sorted.first()
            val cacheKey = "sub_${request.sourceId}_${request.itemId}_${best.fileId}"
            val cachedFile = subtitleCache.subtitleFile(cacheKey)
            if (cachedFile.exists() && cachedFile.length() > 0) {
                attachAndSelectSubtitle(best, cachedFile)
                android.widget.Toast.makeText(context, strings.subtitleDownloaded, android.widget.Toast.LENGTH_SHORT).show()
            } else {
                val srtContent = client.downloadSubtitle(best.fileId).getOrNull()
                if (srtContent != null) {
                    val file = subtitleCache.saveSubtitle(cacheKey, srtContent)
                    attachAndSelectSubtitle(best, file)
                    android.widget.Toast.makeText(context, strings.subtitleDownloaded, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(player, resumeDecisionMade) {
        if (!resumeDecisionMade) return@LaunchedEffect
        val mediaItem = MediaItem.Builder()
            .setUri(request.streamUrl)
            .apply {
                if (attachedSubtitles.isNotEmpty()) {
                    setSubtitleConfigurations(
                        attachedSubtitles.map { (subItem, f) ->
                            MediaItem.SubtitleConfiguration.Builder(android.net.Uri.fromFile(f))
                                .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                                .setLanguage(subItem.language)
                                .setLabel("OpenSubtitles · ${subItem.release}")
                                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                                .build()
                        }
                    )
                }
            }
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
        if (resumePositionMs > 0L) player.seekTo(resumePositionMs)
        player.play()
        while (true) {
            delay(10_000)
            onSaveProgress(player.currentPosition.coerceAtLeast(0L), player.duration.coerceAtLeast(0L))
        }
    }
    val dismissAndSaveProgress = {
        tracker.finalizeSession()
        val pos = maxOf(player.currentPosition.coerceAtLeast(0L), currentPositionMs)
        val dur = maxOf(player.duration.coerceAtLeast(0L), totalDurationMs)
        if (pos >= 10_000L) {
            onSaveProgress(pos, dur)
        }
        onDismiss()
    }
    Dialog(onDismissRequest = dismissAndSaveProgress, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        ImmersiveSystemBars()
        BackHandler { dismissAndSaveProgress() }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(playerFocusRequester)
                .focusProperties { canFocus = !isTv || !isOverlayActive }
                .focusable()
                .onPreviewKeyEvent { keyEvent ->
                    if (isTv && isOverlayActive && keyEvent.type == KeyEventType.KeyDown) {
                        vodInteractionSignal.trySend(Unit)
                    }
                    val isBackKey = keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_BACK ||
                        keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ESCAPE
                    if (isTv && isOverlayActive && isBackKey) {
                        vodInteractionSignal.trySend(Unit)
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
                        vodInteractionSignal.trySend(Unit)
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
                                dismissAndSaveProgress()
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
                onHorizontalSeek = { distanceX, finished ->
                    if (!finished) {
                        val deltaMs = ((distanceX / 600f) * 60_000L).toLong()
                        seekOverlayDeltaMs = deltaMs
                        seekOverlayPositionMs = (player.currentPosition + deltaMs).coerceIn(0L, player.duration.coerceAtLeast(0L))
                    } else {
                        if (seekOverlayPositionMs >= 0L) {
                            player.seekTo(seekOverlayPositionMs)
                        }
                        seekOverlayPositionMs = -1L
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            AnimatedVisibility(
                visible = seekOverlayPositionMs >= 0L,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + scaleOut(targetScale = 0.9f),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                ) {
                    Row(
                        Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (seekOverlayDeltaMs >= 0) Icons.Default.FastForward else Icons.Default.FastRewind,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "${if (seekOverlayDeltaMs >= 0) "+" else ""}${seekOverlayDeltaMs / 1000}s (${formatPlaybackTime(seekOverlayPositionMs)} / ${formatPlaybackTime(totalDurationMs)})",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = controlsVisible && !showEpisodesDrawer,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { controlsVisible = false })
                        }
                ) {
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
                            onClick = dismissAndSaveProgress,
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

                        if (!isTv) {
                            BatteryStatusBadge(contentDescription = strings.battery)
                            Spacer(Modifier.width(if (isCompact) 6.dp else 8.dp))
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
                                onClick = {
                                    autoPlayNext = !autoPlayNext
                                    if (!autoPlayNext) {
                                        autoPlayCountdown = -1
                                        autoPlayCountdownDismissed = true
                                    } else {
                                        autoPlayCountdownDismissed = false
                                    }
                                },
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
                                        fontWeight = FontWeight.Bold,
                                        style = TabularDigitsTextStyle
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
                                fontWeight = FontWeight.SemiBold,
                                style = TabularDigitsTextStyle
                            )
                            Text(
                                formatPlaybackTime(totalDurationMs),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                style = TabularDigitsTextStyle
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
                                        color = MaterialTheme.colorScheme.primary,
                                        style = TabularDigitsTextStyle
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
                                        autoPlayCountdownDismissed = true
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
                } else if (playbackState == Player.STATE_ENDED) {
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

            activeSegment?.let { segment ->
                AnimatedVisibility(
                    visible = autoPlayCountdown <= 0 && resumeDecisionMade && !showEpisodesDrawer && !showTracksDialog && !showSleepTimerDialog,
                    enter = fadeIn() + slideInHorizontally { it / 2 },
                    exit = fadeOut() + slideOutHorizontally { it / 2 },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = 28.dp,
                            bottom = if (controlsVisible) 115.dp else 36.dp
                        )
                ) {
                    val label = when (segment.type) {
                        SegmentType.INTRO -> strings.skipIntro
                        SegmentType.RECAP -> strings.skipRecap
                        SegmentType.CREDITS -> strings.skipCredits
                        SegmentType.PREVIEW -> strings.skipPreview
                    }
                    val icon = when (segment.type) {
                        SegmentType.CREDITS -> Icons.Default.SkipNext
                        else -> Icons.Default.FastForward
                    }

                    Surface(
                        onClick = {
                            when (segment.type) {
                                SegmentType.CREDITS -> {
                                    if (segment.endMs != null) {
                                        player.seekTo(segment.endMs)
                                    } else if (nextEpisode != null) {
                                        onPlayNext(nextEpisode!!)
                                    } else if (totalDurationMs > 1000L) {
                                        player.seekTo(totalDurationMs - 1000L)
                                    }
                                }
                                else -> {
                                    segment.endMs?.let { player.seekTo(it) }
                                }
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Black.copy(alpha = 0.85f),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
                        shadowElevation = 8.dp,
                        modifier = Modifier.tvFullscreenControl(
                            enabled = isTv,
                            requester = vodSkipIntroFocusRequester,
                            shape = RoundedCornerShape(24.dp),
                            scaleOnFocus = 1.08f,
                            upTarget = vodPlayFocusRequester,
                            downTarget = vodTimelineFocusRequester
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = label,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
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

            AnimatedVisibility(
                visible = showTracksDialog && !inPictureInPicture,
                enter = slideInHorizontally { it } + fadeIn(),
                exit = slideOutHorizontally { it } + fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                PlayerControlsDrawer(
                    player = player,
                    audioTracks = audioTracks,
                    subtitleTracks = subtitleTracks,
                    videoTracks = videoTracks,
                    isLive = false,
                    onSearchOpenSubtitles = if (openSubtitlesSettings.enabled && openSubtitlesSettings.apiKey.isNotBlank()) {
                        {
                            showTracksDialog = false
                            showOpenSubtitlesSearchDialog = true
                        }
                    } else null,
                    onUserTrackSelected = { userManuallySelectedTracks = true },
                    onAudioLanguageSelected = onPreferredAudioLanguageChanged,
                    onSubtitleLanguageSelected = onPreferredSubtitleLanguageChanged,
                    onClose = { showTracksDialog = false }
                )
            }

            if (showOpenSubtitlesSearchDialog) {
                OpenSubtitlesSearchDialog(
                    initialQuery = request.seriesName ?: request.title,
                    season = request.season,
                    episode = request.episode,
                    preferredLanguage = preferredSubtitleLanguage,
                    settings = openSubtitlesSettings,
                    onSubtitleSelected = { item, srtContent ->
                        val cacheKey = "sub_${request.sourceId}_${request.itemId}_${item.fileId}"
                        val file = subtitleCache.saveSubtitle(cacheKey, srtContent)
                        attachAndSelectSubtitle(item, file)
                        showOpenSubtitlesSearchDialog = false
                        android.widget.Toast.makeText(context, strings.subtitleDownloaded, android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onDismiss = { showOpenSubtitlesSearchDialog = false }
                )
            }

            if (sleepTimerActivity?.sleepTimerState?.awaitingConfirmation == true) {
                val videoEnded = playbackState == Player.STATE_ENDED
                SleepTimerConfirmationDialog(
                    secondsRemaining = sleepTimerActivity.sleepTimerState.confirmationSeconds ?: 30,
                    canContinue = !videoEnded || nextEpisode != null,
                    canPlayNextThenStop = videoEnded && nextEpisode != null,
                    onStop = { sleepTimerActivity.playbackSleepTimer.stopNow() },
                    onContinue = {
                        sleepTimerActivity.playbackSleepTimer.select(null)
                        if (videoEnded) nextEpisode?.let(onPlayNext) else player.play()
                    },
                    onPlayNextThenStop = {
                        sleepTimerActivity.playbackSleepTimer.select(-1)
                        nextEpisode?.let(onPlayNext)
                    },
                    onSetTimer = { minutes ->
                        sleepTimerActivity.playbackSleepTimer.select(minutes)
                        if (videoEnded) nextEpisode?.let(onPlayNext) else player.play()
                    }
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
                            Text(strings.sleepTimerHint, style = MaterialTheme.typography.bodySmall)
                            options.forEach { (mins, label) ->
                                val isSelected = sleepTimerMinutes == mins
                                Surface(
                                    onClick = {
                                        sleepTimerActivity?.playbackSleepTimer?.select(mins)
                                        if (mins != null && mins > 0) {
                                            Toast.makeText(context, strings.sleepTimerSet(mins), Toast.LENGTH_SHORT).show()
                                        } else {
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
private fun extractVideoTracks(currentTracks: Tracks?): List<PlaybackVideoTrackItem> =
    TrackLanguageMatcher.extractVideoTracks(currentTracks)

@UnstableApi
@Composable
private fun PlayerControlsDrawer(
    player: Player,
    audioTracks: List<PlaybackTrackItem>,
    subtitleTracks: List<PlaybackTrackItem>,
    videoTracks: List<PlaybackVideoTrackItem>,
    isLive: Boolean,
    onSearchOpenSubtitles: (() -> Unit)? = null,
    onUserTrackSelected: () -> Unit = {},
    onAudioLanguageSelected: ((String) -> Unit)? = null,
    onSubtitleLanguageSelected: ((String) -> Unit)? = null,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val isTv = LocalFormFactor.current.isTelevision
    val isCompact = LocalConfiguration.current.screenWidthDp < 600
    val closeFocusRequester = remember { FocusRequester() }

    var selectedTab by remember { mutableIntStateOf(0) }

    val tabFocusRequesters = remember { List(3) { FocusRequester() } }
    val optionCount = when (selectedTab) {
        0 -> audioTracks.size + 1 + subtitleTracks.size + if (onSearchOpenSubtitles != null) 1 else 0
        1 -> 1 + videoTracks.size
        else -> if (isLive) 1 else 0
    }
    val optionFocusRequesters = remember(selectedTab, optionCount) { List(optionCount) { FocusRequester() } }
    fun optionNavigation(index: Int): Modifier = Modifier
        .focusRequester(optionFocusRequesters[index])
        .focusProperties {
            if (isTv) {
                up = optionFocusRequesters.getOrNull(index - 1) ?: tabFocusRequesters[selectedTab]
                down = optionFocusRequesters.getOrNull(index + 1) ?: FocusRequester.Cancel
                left = FocusRequester.Cancel
                right = FocusRequester.Cancel
            }
        }

    val selectedSubtitle = subtitleTracks.firstOrNull { it.isSelected }
    val isAutoVideo = remember(player.trackSelectionParameters) {
        player.trackSelectionParameters.overrides.values.none { it.type == C.TRACK_TYPE_VIDEO }
    }

    var liveOffsetMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(player, isLive) {
        if (isLive) {
            while (true) {
                liveOffsetMs = player.currentLiveOffset.coerceAtLeast(0L)
                delay(1000)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (isTv) {
            repeat(10) {
                withFrameNanos { }
                if (runCatching { tabFocusRequesters[0].requestFocus() }.getOrDefault(false)) {
                    return@LaunchedEffect
                }
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
        color = Color(0xFF131722).copy(alpha = 0.96f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        shadowElevation = 24.dp,
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(min = 320.dp, max = if (isCompact) 360.dp else 430.dp)
            .navigationBarsPadding()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Header: Title & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        strings.audioAndSubtitles,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .focusRequester(closeFocusRequester)
                        .focusProperties {
                            if (isTv) {
                                down = tabFocusRequesters[selectedTab]
                                left = tabFocusRequesters[selectedTab]
                                up = FocusRequester.Cancel
                                right = FocusRequester.Cancel
                            }
                        }
                        .tvFocusHighlight(shape = CircleShape, scaleOnFocus = if (isTv) 1.15f else 1f)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = strings.close,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Navigation Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    0 to strings.audioTracks,
                    1 to strings.videoQuality,
                    2 to strings.streamResilience
                ).forEach { (tabIndex, title) ->
                    val isSelected = selectedTab == tabIndex
                    Surface(
                        onClick = { selectedTab = tabIndex },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(tabFocusRequesters[tabIndex])
                            .focusProperties {
                                if (isTv) {
                                    up = closeFocusRequester
                                    down = optionFocusRequesters.firstOrNull() ?: FocusRequester.Cancel
                                    left = tabFocusRequesters.getOrNull(tabIndex - 1) ?: FocusRequester.Cancel
                                    right = tabFocusRequesters.getOrNull(tabIndex + 1) ?: closeFocusRequester
                                }
                            }
                            .tvFocusHighlight(shape = RoundedCornerShape(12.dp), scaleOnFocus = 1.06f)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Content Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Audio Tracks Section
                        Text(
                            strings.audioTracks,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (audioTracks.isEmpty()) {
                            Text(
                                strings.noAudioTracksAvailable,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        } else {
                            audioTracks.forEachIndexed { index, track ->
                                val isSelected = track.isSelected
                                Surface(
                                    onClick = {
                                        onUserTrackSelected()
                                        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                            .setOverrideForType(TrackSelectionOverride(track.group.mediaTrackGroup, track.trackIndex))
                                            .build()
                                        TrackLanguageMatcher.detectLanguageCode(track)?.let { code ->
                                            onAudioLanguageSelected?.invoke(code)
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f),
                                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f)),
                                    modifier = Modifier.fillMaxWidth().then(optionNavigation(index)).tvFocusHighlight(shape = RoundedCornerShape(10.dp), scaleOnFocus = 1.04f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            localizedPlaybackTrackLabel(track, strings),
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Subtitles Section
                        Text(
                            strings.subtitles,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val isOff = selectedSubtitle == null
                        Surface(
                            onClick = {
                                onUserTrackSelected()
                                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                    .build()
                                onSubtitleLanguageSelected?.invoke("off")
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isOff) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, if (isOff) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth().then(optionNavigation(audioTracks.size)).tvFocusHighlight(shape = RoundedCornerShape(10.dp), scaleOnFocus = 1.04f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Subtitles, contentDescription = null, tint = if (isOff) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    strings.subtitlesOff,
                                    color = if (isOff) Color.White else Color.White.copy(alpha = 0.85f),
                                    fontWeight = if (isOff) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isOff) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        subtitleTracks.forEachIndexed { index, track ->
                            val isSelected = track.isSelected
                            Surface(
                                onClick = {
                                    onUserTrackSelected()
                                    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                        .setOverrideForType(TrackSelectionOverride(track.group.mediaTrackGroup, track.trackIndex))
                                        .build()
                                    TrackLanguageMatcher.detectLanguageCode(track)?.let { code ->
                                        onSubtitleLanguageSelected?.invoke(code)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f),
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth().then(optionNavigation(audioTracks.size + 1 + index)).tvFocusHighlight(shape = RoundedCornerShape(10.dp), scaleOnFocus = 1.04f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Subtitles, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        localizedPlaybackTrackLabel(track, strings),
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        if (onSearchOpenSubtitles != null) {
                            OutlinedButton(
                                onClick = onSearchOpenSubtitles,
                                modifier = Modifier.fillMaxWidth().then(optionNavigation(optionCount - 1)).tvFocusHighlight(shape = RoundedCornerShape(10.dp), scaleOnFocus = 1.04f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(strings.searchOnlineSubtitles, fontSize = 12.sp)
                            }
                        }

                    }

                    1 -> {
                        // Video Quality Tab
                        Text(
                            strings.videoQuality,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Surface(
                            onClick = {
                                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                    .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                                    .build()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isAutoVideo) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, if (isAutoVideo) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth().then(optionNavigation(0)).tvFocusHighlight(shape = RoundedCornerShape(10.dp), scaleOnFocus = 1.04f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = if (isAutoVideo) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        strings.qualityAuto,
                                        color = if (isAutoVideo) Color.White else Color.White.copy(alpha = 0.85f),
                                        fontWeight = if (isAutoVideo) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        strings.qualityAdaptive,
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 11.sp
                                    )
                                }
                                if (isAutoVideo) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        if (videoTracks.isNotEmpty()) {
                            videoTracks.forEachIndexed { index, track ->
                                val isSelected = !isAutoVideo && track.isSelected
                                Surface(
                                    onClick = {
                                        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                                            .setOverrideForType(TrackSelectionOverride(track.group.mediaTrackGroup, track.trackIndex))
                                            .build()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f),
                                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f)),
                                    modifier = Modifier.fillMaxWidth().then(optionNavigation(index + 1)).tvFocusHighlight(shape = RoundedCornerShape(10.dp), scaleOnFocus = 1.04f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Tune, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                track.label,
                                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f),
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                            if (track.frameRate > 0f) {
                                                Text(
                                                    "${track.frameRate.toInt()} fps",
                                                    color = Color.White.copy(alpha = 0.5f),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Stream Resilience Tab
                        Text(
                            strings.streamResilience,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (isLive) {
                            val isDrifted = liveOffsetMs > 12_000L
                            Surface(
                                onClick = {
                                    player.seekToDefaultPosition()
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDrifted) Color(0xFFE50914).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                border = BorderStroke(1.dp, if (isDrifted) Color(0xFFE50914) else Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth().then(optionNavigation(0)).tvFocusHighlight(shape = RoundedCornerShape(12.dp), scaleOnFocus = 1.04f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isDrifted) Color(0xFFE50914) else Color(0xFF4CAF50),
                                        modifier = Modifier.size(10.dp)
                                    ) {}
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            strings.goLive,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            if (isDrifted) strings.liveBehind(liveOffsetMs / 1000) else strings.liveSynchronized,
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 11.sp,
                                            style = TabularDigitsTextStyle
                                        )
                                    }
                                    if (isDrifted) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFE50914),
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                strings.liveBadge,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
    track.label.startsWith("OpenSubtitles") -> track.label
    TrackLanguageMatcher.matchesLanguage(track.language, track.label, "pt") -> strings.langPortuguese
    TrackLanguageMatcher.matchesLanguage(track.language, track.label, "en") -> strings.langEnglish
    TrackLanguageMatcher.matchesLanguage(track.language, track.label, "es") -> strings.langSpanish
    TrackLanguageMatcher.matchesLanguage(track.language, track.label, "fr") -> strings.langFrench
    TrackLanguageMatcher.matchesLanguage(track.language, track.label, "de") -> strings.langGerman
    TrackLanguageMatcher.matchesLanguage(track.language, track.label, "it") -> strings.langItalian
    else -> track.label
}



@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun ChannelBrowser(
    playlist: Playlist,
    activeProfile: PlaylistProfile,
    enabledSourceCount: Int,
    enabledSources: List<PlaylistProfile>,
    epgGuide: EpgGuide?,
    epgSourceName: String?,
    epgTimeShiftHours: Int = 0,
    epgScheduleCorrectionMinutes: Int = epgTimeShiftHours * 60,
    epgMappings: Map<String, EpgChannelMapping> = emptyMap(),
    epgLoadedDays: Int = 3,
    epgRepository: EpgRepository? = null,
    playChannelsFullscreen: Boolean,
    autoPictureInPicture: Boolean,
    preferredAudioLanguage: String = "default",
    preferredSubtitleLanguage: String = "off",
    subtitleFallbackEnglish: Boolean = true,
    onPreferredAudioLanguageChanged: ((String) -> Unit)? = null,
    onPreferredSubtitleLanguageChanged: ((String) -> Unit)? = null,
    loading: Boolean,
    error: String?,
    onRetryLoad: (() -> Unit)?,
    favoriteChannelIds: Set<String>,
    recentChannelIds: List<String> = emptyList(),
    onRecordChannelWatched: (PlaylistChannel) -> Unit = {},
    hiddenCategories: Set<String> = emptySet(),
    categoryOrder: List<String> = emptyList(),
    hideAdultContent: Boolean = false,
    onToggleCategoryHidden: (String) -> Unit = {},
    onMoveCategory: (String, Boolean) -> Unit = { _, _ -> },
    onUnhideAllCategories: () -> Unit = {},
    onHideAllCategories: (List<String>) -> Unit = {},
    onResetCategoryOrder: () -> Unit = {},
    onToggleFavorite: (PlaylistChannel) -> Unit,
    requestedChannel: PlaylistChannel?,
    onRequestedChannelHandled: () -> Unit,
    onOpenSettings: (Int) -> Unit,
    onSaveMapping: (PlaylistChannel, com.fortresstv.player.epg.EpgChannel, String?) -> Unit = { _, _, _ -> },
    onResetMapping: (PlaylistChannel, String?) -> Unit = { _, _ -> },
    onRequestAdultAuth: (((String?) -> Unit)) -> Unit = {},
    progressEntries: List<VodProgressEntry> = emptyList(),
    onWatchCatchup: ((PlaylistChannel, EpgProgramme) -> Unit)? = null,
    contentFocusRequester: FocusRequester? = null,
    sidebarFocusRequester: FocusRequester? = null
) {
    val strings = LocalAppStrings.current
    val isTv = LocalFormFactor.current.isTelevision
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val isLandscapeTablet = configuration.screenWidthDp >= 700 && !isPortrait
    val sidebarWidth = when {
        isCompact -> 280.dp
        isLandscapeTablet -> 230.dp
        else -> 260.dp
    }

    val filterButtonFocusRequester = remember { FocusRequester() }
    val calendarButtonFocusRequester = remember { FocusRequester() }
    val selectedCategoryFocusRequester = remember { FocusRequester() }
    val channelListFocusRequester = remember { FocusRequester() }
    val firstFilterFocusRequester = remember { FocusRequester() }
    val playerFocusRequester = remember { FocusRequester() }

    val allFilter = "all"
    val recentFilter = "recent"
    val uncategorizedFilter = "group:"
    var isSidebarExpanded by rememberSaveable(isLandscapeTablet) { mutableStateOf(isLandscapeTablet) }
    var sidebarInteractionCount by remember { mutableLongStateOf(0L) }
    var showManageCategories by remember { mutableStateOf(false) }
    var focusFirstChannelAfterFilter by remember { mutableStateOf(false) }
    var hadInitialComposition by remember { mutableStateOf(false) }

    val categoryFocusRequester = selectedCategoryFocusRequester
    val effectiveChannelFocusRequester = contentFocusRequester ?: channelListFocusRequester
    val categoryListState = rememberLazyListState()

    LaunchedEffect(isSidebarExpanded, sidebarInteractionCount, isLandscapeTablet) {
        if (isSidebarExpanded && !isTv && !isLandscapeTablet) {
            delay(5_000)
            isSidebarExpanded = false
        }
    }

    LaunchedEffect(isSidebarExpanded) {
        if (!hadInitialComposition) {
            hadInitialComposition = true
            return@LaunchedEffect
        }
        if (isTv && isSidebarExpanded) {
            delay(100)
            val handled = runCatching { categoryFocusRequester.requestFocus() }.isSuccess
            if (!handled) runCatching { firstFilterFocusRequester.requestFocus() }
        }
    }

    val isCategoryHidden: (String) -> Boolean = remember(hiddenCategories, hideAdultContent) {
        { category ->
            category in hiddenCategories || (hideAdultContent && isAdultContent(null, category))
        }
    }
    val groupCounts = remember(playlist) {
        playlist.channels.groupingBy { it.group.orEmpty() }.eachCount()
    }
    val rawCategories = remember(playlist) {
        groupCounts.keys.filter { it.isNotBlank() }
    }
    val orderedCategories = remember(rawCategories, categoryOrder, hiddenCategories, hideAdultContent) {
        val nonHidden = rawCategories.filter { !isCategoryHidden(it) }
        val orderMap = categoryOrder.withIndex().associate { it.value to it.index }
        nonHidden.sortedWith(compareBy({ orderMap[it] ?: Int.MAX_VALUE }, { it }))
    }
    val allVisibleChannels = remember(playlist.channels, hiddenCategories, hideAdultContent) {
        playlist.channels.filter { channel ->
            val group = channel.group.orEmpty()
            !isCategoryHidden(group) && (!hideAdultContent || !isAdultContent(channel.name, group))
        }
    }

    var selectedFilter by remember(playlist) {
        mutableStateOf(allFilter)
    }

    LaunchedEffect(hiddenCategories, hideAdultContent) {
        if (selectedFilter.startsWith("group:")) {
            val group = selectedFilter.removePrefix("group:")
            if (isCategoryHidden(group)) {
                selectedFilter = allFilter
            }
        }
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
            if (!isLandscapeTablet) isSidebarExpanded = false
            focusFirstChannelAfterFilter = true
        } else if (!isLandscapeTablet) {
            isSidebarExpanded = false
        }
    }
    fun selectChannel(channel: PlaylistChannel) {
        hideKeyboard()
        if (selectedChannel == channel) {
            fullscreenTrigger++
        } else {
            selectedChannel = channel
        }
        if (!isLandscapeTablet) {
            isSidebarExpanded = false
        }
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
        if (recentChannelIds.isEmpty()) emptyList()
        else {
            recentChannelIds.mapNotNull { id ->
                val raw = id.substringAfterLast(':')
                allVisibleChannels.firstOrNull { ch ->
                    ch.libraryId() == id || "${ch.sourceId}:${ch.sourceItemId}" == id || ch.sourceItemId == id || ch.sourceItemId == raw
                }
            }
        }
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
                allVisibleChannels.filter { it.group == group }
            }
            else -> allVisibleChannels
        }
    }
    val visibleChannels = remember(channelsInGroup, searchQuery) {
        channelsInGroup.searchChannels(searchQuery)
    }
    val channelEntryFocusIndex = remember(visibleChannels, selectedChannel) {
        visibleChannels.indexOfFirst { it == selectedChannel }.coerceAtLeast(0)
    }
    val channelListState = rememberLazyListState()

    val channelPaneWidth = when {
        isCompact -> 280.dp
        isLandscapeTablet && isSidebarExpanded && selectedChannel != null -> 310.dp
        isLandscapeTablet -> 340.dp
        else -> 430.dp
    }
    val movablePlayerPanel = remember(playerFocusRequester, effectiveChannelFocusRequester, preferredAudioLanguage, preferredSubtitleLanguage, subtitleFallbackEnglish) {
        movableContentOf { currentChannel: PlaylistChannel, playerModifier: Modifier, isCompactPreview: Boolean ->
            PlayerPanel(
                channel = currentChannel,
                epgGuide = epgGuide,
                epgTimeShiftHours = epgTimeShiftHours,
                channels = allVisibleChannels,
                groups = orderedCategories,
                favoriteChannelIds = favoriteChannelIds,
                recentChannelIds = recentChannelIds,
                onToggleFavorite = onToggleFavorite,
                autoEnterFullscreen = playChannelsFullscreen,
                fullscreenTrigger = fullscreenTrigger,
                autoPictureInPicture = autoPictureInPicture,
                preferredAudioLanguage = preferredAudioLanguage,
                preferredSubtitleLanguage = preferredSubtitleLanguage,
                subtitleFallbackEnglish = subtitleFallbackEnglish,
                onPreferredAudioLanguageChanged = onPreferredAudioLanguageChanged,
                onPreferredSubtitleLanguageChanged = onPreferredSubtitleLanguageChanged,
                onChannelSelected = ::selectChannel,
                modifier = playerModifier,
                compactPreview = isCompactPreview,
                playerFocusRequester = playerFocusRequester,
                channelListFocusRequester = effectiveChannelFocusRequester
            )
        }
    }

    if (showProgrammeGuide) {
        val initialCategory = when {
            selectedFilter.startsWith("group:") -> selectedFilter.removePrefix("group:")
            selectedFilter == recentFilter -> strings.navFavorites
            else -> strings.allChannels
        }
        ProgrammeGuideScreen(
            playlist = playlist,
            epgGuide = epgGuide,
            epgScheduleCorrectionMinutes = epgScheduleCorrectionMinutes,
            epgMappings = epgMappings,
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
            },
            onWatchCatchup = { channel, programme ->
                showProgrammeGuide = false
                onWatchCatchup?.invoke(channel, programme)
            },
            progressEntries = progressEntries,
            activeChannel = selectedChannel,
            dockedPlayer = if (selectedChannel != null) {
                {
                    selectedChannel?.let { curChannel ->
                        movablePlayerPanel(curChannel, Modifier.fillMaxSize(), true)
                    }
                }
            } else null
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
        // A pending TV focus transfer scrolls to its own target below.
        if (!isTv || !focusFirstChannelAfterFilter) channelListState.scrollToItem(0)
    }

    var channelPaneHasFocus by remember { mutableStateOf(false) }
    var categorySidebarHasFocus by remember { mutableStateOf(false) }

    BackHandler(enabled = isTv && (channelPaneHasFocus || (isSidebarExpanded && !isLandscapeTablet))) {
        if (channelPaneHasFocus) {
            if (isSidebarExpanded) {
                val handled = runCatching { categoryFocusRequester.requestFocus() }.isSuccess
                if (!handled) {
                    runCatching { firstFilterFocusRequester.requestFocus() }
                }
            } else if (sidebarFocusRequester != null) {
                runCatching { sidebarFocusRequester.requestFocus() }
            }
        } else if (isSidebarExpanded && !isLandscapeTablet) {
            isSidebarExpanded = false
            runCatching { filterButtonFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(focusFirstChannelAfterFilter, selectedFilter, searchQuery, visibleChannels, channelEntryFocusIndex) {
        if (isTv && focusFirstChannelAfterFilter) {
            // The wide layout keeps the filters open, so focus must not depend on
            // the sidebar closing. Compose the row that actually owns the requester.
            if (visibleChannels.isNotEmpty()) {
                channelListState.scrollToItem(channelEntryFocusIndex)
                withFrameNanos { }
                effectiveChannelFocusRequester.requestFocus()
            }
            focusFirstChannelAfterFilter = false
        }
    }

    LaunchedEffect(selectedChannel) {
        if (selectedChannel != null) {
            val target = visibleChannels.indexOfFirst { it == selectedChannel }
            if (target >= 0) {
                val layoutInfo = channelListState.layoutInfo
                val visibleIndices = layoutInfo.visibleItemsInfo.map { it.index }
                if (target !in visibleIndices) {
                    runCatching { channelListState.scrollToItem((target - 1).coerceAtLeast(0)) }
                }
            }
        }
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
        color = Color.Transparent
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
                    .width(sidebarWidth)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .onFocusChanged { categorySidebarHasFocus = it.hasFocus }
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
                        modifier = Modifier
                            .focusProperties {
                                if (sidebarFocusRequester != null) left = sidebarFocusRequester
                                right = effectiveChannelFocusRequester
                            }
                            .onPreviewKeyEvent { event ->
                                if (isTv && (event.type == KeyEventType.KeyDown || event.type == KeyEventType.KeyUp)) {
                                    when (event.nativeKeyEvent.keyCode) {
                                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                            if (event.type == KeyEventType.KeyDown && sidebarFocusRequester != null) {
                                                runCatching { sidebarFocusRequester.requestFocus() }
                                            }
                                            return@onPreviewKeyEvent true
                                        }
                                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                            if (event.type == KeyEventType.KeyDown) {
                                                runCatching { effectiveChannelFocusRequester.requestFocus() }
                                            }
                                            return@onPreviewKeyEvent true
                                        }
                                    }
                                }
                                false
                            }
                            .tvFocusHighlight(
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
                        .focusProperties {
                            if (sidebarFocusRequester != null) left = sidebarFocusRequester
                            right = effectiveChannelFocusRequester
                        }
                        .onPreviewKeyEvent { event ->
                            if (isTv && (event.type == KeyEventType.KeyDown || event.type == KeyEventType.KeyUp)) {
                                when (event.nativeKeyEvent.keyCode) {
                                    android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                        if (event.type == KeyEventType.KeyDown && sidebarFocusRequester != null) {
                                            runCatching { sidebarFocusRequester.requestFocus() }
                                        }
                                        return@onPreviewKeyEvent true
                                    }
                                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                        if (event.type == KeyEventType.KeyDown) {
                                            runCatching { effectiveChannelFocusRequester.requestFocus() }
                                        }
                                        return@onPreviewKeyEvent true
                                    }
                                }
                            }
                            false
                        }
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
                            .focusProperties {
                                if (sidebarFocusRequester != null) left = sidebarFocusRequester
                                right = effectiveChannelFocusRequester
                            }
                            .onPreviewKeyEvent { event ->
                                if (isTv && (event.type == KeyEventType.KeyDown || event.type == KeyEventType.KeyUp)) {
                                    when (event.nativeKeyEvent.keyCode) {
                                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                            if (event.type == KeyEventType.KeyDown && sidebarFocusRequester != null) {
                                                runCatching { sidebarFocusRequester.requestFocus() }
                                            }
                                            return@onPreviewKeyEvent true
                                        }
                                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                            if (event.type == KeyEventType.KeyDown) {
                                                runCatching { effectiveChannelFocusRequester.requestFocus() }
                                            }
                                            return@onPreviewKeyEvent true
                                        }
                                    }
                                }
                                false
                            }
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
                    state = categoryListState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        val isSelected = selectedFilter == allFilter
                        GroupItem(
                            name = strings.allChannels,
                            count = allVisibleChannels.size,
                            selected = isSelected,
                            modifier = (if (isSelected) Modifier.focusRequester(categoryFocusRequester) else Modifier)
                                .focusRequester(firstFilterFocusRequester),
                            sidebarFocusRequester = sidebarFocusRequester,
                            channelListFocusRequester = effectiveChannelFocusRequester,
                            onClick = { selectFilter(allFilter) }
                        )
                    }
                    if (recentChannels.isNotEmpty()) {
                        item {
                            val isSelected = selectedFilter == recentFilter
                            GroupItem(
                                name = strings.recentlyWatched,
                                count = recentChannels.size,
                                selected = isSelected,
                                modifier = if (isSelected) Modifier.focusRequester(categoryFocusRequester) else Modifier,
                                sidebarFocusRequester = sidebarFocusRequester,
                                channelListFocusRequester = effectiveChannelFocusRequester,
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
                            val isSelected = selectedFilter == "source:${profile.id}"
                            GroupItem(
                                name = profile.name,
                                count = sourceCounts[profile.id] ?: 0,
                                selected = isSelected,
                                modifier = if (isSelected) Modifier.focusRequester(categoryFocusRequester) else Modifier,
                                sidebarFocusRequester = sidebarFocusRequester,
                                channelListFocusRequester = effectiveChannelFocusRequester,
                                onClick = { selectFilter("source:${profile.id}") }
                            )
                        }
                    }
                    val uncategorizedCount = groupCounts[""] ?: 0
                    if (uncategorizedCount > 0) {
                        item {
                            val isSelected = selectedFilter == uncategorizedFilter
                            GroupItem(
                                name = strings.uncategorized,
                                count = uncategorizedCount,
                                selected = isSelected,
                                modifier = if (isSelected) Modifier.focusRequester(categoryFocusRequester) else Modifier,
                                sidebarFocusRequester = sidebarFocusRequester,
                                channelListFocusRequester = effectiveChannelFocusRequester,
                                onClick = { selectFilter(uncategorizedFilter) }
                            )
                        }
                    }
                    items(
                        orderedCategories,
                        key = { "group-filter-$it" }
                    ) { group ->
                        val isSelected = selectedFilter == "group:$group"
                        GroupItem(
                            name = group,
                            count = groupCounts[group] ?: 0,
                            selected = isSelected,
                            modifier = if (isSelected) Modifier.focusRequester(categoryFocusRequester) else Modifier,
                            sidebarFocusRequester = sidebarFocusRequester,
                            channelListFocusRequester = effectiveChannelFocusRequester,
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
                .onFocusChanged { channelPaneHasFocus = it.hasFocus }
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
                    FilledTonalIconButton(
                        onClick = { isSidebarExpanded = !isSidebarExpanded },
                        modifier = Modifier
                            .focusRequester(filterButtonFocusRequester)
                            .onPreviewKeyEvent { keyEvent ->
                                if (isTv && (keyEvent.type == KeyEventType.KeyDown || keyEvent.type == KeyEventType.KeyUp)) {
                                    when (keyEvent.nativeKeyEvent.keyCode) {
                                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                            if (keyEvent.type == KeyEventType.KeyDown) {
                                                if (isSidebarExpanded) {
                                                    val handled = runCatching { categoryFocusRequester.requestFocus() }.isSuccess
                                                    if (!handled) runCatching { firstFilterFocusRequester.requestFocus() }
                                                } else if (sidebarFocusRequester != null) {
                                                    runCatching { sidebarFocusRequester.requestFocus() }
                                                }
                                            }
                                            return@onPreviewKeyEvent true
                                        }
                                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                            if (keyEvent.type == KeyEventType.KeyDown) {
                                                try {
                                                    calendarButtonFocusRequester.requestFocus()
                                                } catch (_: Throwable) {}
                                            }
                                            return@onPreviewKeyEvent true
                                        }
                                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                            if (keyEvent.type == KeyEventType.KeyDown) {
                                                val handled = runCatching { effectiveChannelFocusRequester.requestFocus() }.isSuccess
                                                if (!handled) focusFirstChannelAfterFilter = true
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
                        Icon(
                            if (isSidebarExpanded) Icons.AutoMirrored.Filled.MenuOpen else Icons.Default.FilterList,
                            contentDescription = if (isSidebarExpanded) strings.collapseSidebar else strings.expandSidebar
                        )
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
                                            if (keyEvent.type == KeyEventType.KeyDown) {
                                                try {
                                                    filterButtonFocusRequester.requestFocus()
                                                } catch (_: Throwable) {}
                                            }
                                            return@onPreviewKeyEvent true
                                        }
                                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                            if (keyEvent.type == KeyEventType.KeyDown) {
                                                val handled = runCatching { effectiveChannelFocusRequester.requestFocus() }.isSuccess
                                                if (!handled) focusFirstChannelAfterFilter = true
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
                            if (isTv && (keyEvent.type == KeyEventType.KeyDown || keyEvent.type == KeyEventType.KeyUp) && keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN) {
                                if (keyEvent.type == KeyEventType.KeyDown) {
                                    val handled = runCatching { effectiveChannelFocusRequester.requestFocus() }.isSuccess
                                    if (!handled) focusFirstChannelAfterFilter = true
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

            Spacer(Modifier.height(10.dp))
            if (isCompact && isPortrait) {
                if (selectedChannel != null) {
                    Column(Modifier.weight(1f).fillMaxWidth()) {
                        movablePlayerPanel(selectedChannel!!, Modifier.fillMaxWidth().wrapContentHeight(), false)
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
                                    epgScheduleCorrectionMinutes = epgScheduleCorrectionMinutes,
                                    manualXmltvId = epgMappings["${channel.sourceId}:${channel.sourceItemId}"]?.xmltvChannelId,
                                    selected = selectedChannel == channel,
                                    isFavorite = channel.libraryId()?.let(favoriteChannelIds::contains) == true,
                                    onToggleFavorite = { onToggleFavorite(channel) },
                                    onClick = { selectChannel(channel) }
                                )
                            }
                            if (visibleChannels.isEmpty()) {
                                item {
                                    Box(Modifier.fillParentMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) {
                                        if (loading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(36.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            Text(strings.noChannelsMatch, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
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
                                epgScheduleCorrectionMinutes = epgScheduleCorrectionMinutes,
                                manualXmltvId = epgMappings["${channel.sourceId}:${channel.sourceItemId}"]?.xmltvChannelId,
                                selected = selectedChannel == channel,
                                isFavorite = channel.libraryId()?.let(favoriteChannelIds::contains) == true,
                                onToggleFavorite = { onToggleFavorite(channel) },
                                onClick = { selectChannel(channel) }
                            )
                        }
                        if (visibleChannels.isEmpty()) {
                            item {
                                Box(Modifier.fillParentMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) {
                                    if (loading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(36.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Text(strings.noChannelsMatch, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
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
                            val isSelected = selectedChannel == channel
                            val shouldAttachEffectiveFocus = index == channelEntryFocusIndex
                            GuideChannelRow(
                                channel = channel,
                                epgGuide = epgGuide,
                                scheduleNow = scheduleNow,
                                epgTimeShiftHours = epgTimeShiftHours,
                                epgScheduleCorrectionMinutes = epgScheduleCorrectionMinutes,
                                manualXmltvId = epgMappings["${channel.sourceId}:${channel.sourceItemId}"]?.xmltvChannelId,
                                selected = isSelected,
                                isFavorite = channel.libraryId()?.let(favoriteChannelIds::contains) == true,
                                modifier = (if (shouldAttachEffectiveFocus) Modifier.focusRequester(effectiveChannelFocusRequester) else Modifier)
                                    .onPreviewKeyEvent { keyEvent ->
                                        if (isTv && (keyEvent.type == KeyEventType.KeyDown || keyEvent.type == KeyEventType.KeyUp) && keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP && index == 0) {
                                            if (keyEvent.type == KeyEventType.KeyDown) {
                                                try {
                                                    filterButtonFocusRequester.requestFocus()
                                                } catch (_: Throwable) {}
                                            }
                                            return@onPreviewKeyEvent true
                                        }
                                        false
                                     },
                                sidebarFocusRequester = if (isSidebarExpanded) categoryFocusRequester else sidebarFocusRequester,
                                fallbackFocusRequester = firstFilterFocusRequester,
                                playerFocusRequester = if (selectedChannel != null) playerFocusRequester else null,
                                onToggleFavorite = { onToggleFavorite(channel) },
                                onClick = { selectChannel(channel) }
                            )
                        }
                        if (visibleChannels.isEmpty()) {
                            item {
                                Box(Modifier.fillParentMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                    if (loading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(36.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Text(strings.noChannelsMatch, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    if (selectedChannel != null) {
                        Spacer(Modifier.width(16.dp))
                        movablePlayerPanel(selectedChannel!!, Modifier.weight(1f).fillMaxHeight(), false)
                    }
                }
            }
        }

        if (showUnmatchedDialog) {
            UnmatchedEpgDialog(
                unmatchedChannels = unmatchedChannels,
                allChannels = allVisibleChannels,
                epgGuide = epgGuide,
                epgMappings = epgMappings,
                onSaveMapping = onSaveMapping,
                onResetMapping = onResetMapping,
                onRequestAdultAuth = onRequestAdultAuth,
                onDismiss = { showUnmatchedDialog = false },
                onSelectChannel = { channel ->
                    selectChannel(channel)
                    searchQuery = ""
                    selectedFilter = allFilter
                }
            )
        }

        if (showManageCategories) {
            val effectivelyHidden = remember(rawCategories, hiddenCategories, hideAdultContent) {
                rawCategories.filter { isCategoryHidden(it) }.toSet()
            }
            ManageCategoriesDialog(
                categories = rawCategories,
                groupCounts = groupCounts,
                hiddenCategories = effectivelyHidden,
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
internal fun GroupItem(
    name: String,
    count: Int?,
    selected: Boolean,
    modifier: Modifier = Modifier,
    sidebarFocusRequester: FocusRequester? = null,
    channelListFocusRequester: FocusRequester? = null,
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
            .focusProperties {
                if (sidebarFocusRequester != null) {
                    left = sidebarFocusRequester
                }
                if (channelListFocusRequester != null) {
                    right = channelListFocusRequester
                }
            }
            .onPreviewKeyEvent { keyEvent ->
                if (isTv && (keyEvent.type == KeyEventType.KeyDown || keyEvent.type == KeyEventType.KeyUp)) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (keyEvent.type == KeyEventType.KeyDown && sidebarFocusRequester != null) {
                                runCatching { sidebarFocusRequester.requestFocus() }
                            }
                            return@onPreviewKeyEvent true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                if (!selected) {
                                    onClick()
                                } else {
                                    val handled = channelListFocusRequester?.let { req ->
                                        runCatching { req.requestFocus() }.isSuccess
                                    } ?: false
                                    if (!handled) {
                                        onClick()
                                    }
                                }
                            }
                            return@onPreviewKeyEvent true
                        }
                    }
                }
                false
            }
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
internal fun GuideChannelRow(
    channel: PlaylistChannel,
    epgGuide: EpgGuide?,
    scheduleNow: Instant,
    epgTimeShiftHours: Int = 0,
    epgScheduleCorrectionMinutes: Int = epgTimeShiftHours * 60,
    manualXmltvId: String? = null,
    selected: Boolean,
    isFavorite: Boolean = false,
    healthSnapshot: com.fortresstv.player.telemetry.ChannelHealthSnapshot? = null,
    modifier: Modifier = Modifier,
    sidebarFocusRequester: FocusRequester? = null,
    fallbackFocusRequester: FocusRequester? = null,
    playerFocusRequester: FocusRequester? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val healthSnapshots by remember { com.fortresstv.player.telemetry.ChannelHealthRepository.get(context.applicationContext).snapshots }.collectAsState()
    val effectiveHealth = healthSnapshot
        ?: channel.sourceItemId.takeIf { it.isNotBlank() }?.let { healthSnapshots[it] }
        ?: channel.tvgId?.takeIf { it.isNotBlank() }?.let { healthSnapshots[it] }
        ?: healthSnapshots[channel.name]
    val strings = LocalAppStrings.current
    val presentation = remember(channel.tvgId, channel.name, epgGuide, scheduleNow, epgScheduleCorrectionMinutes, manualXmltvId, strings) {
        val current = epgGuide?.snapshot(channel.tvgId, channel.name, scheduleNow, correctionMinutes = epgScheduleCorrectionMinutes, manualXmltvId = manualXmltvId)?.current
        val shiftMillis = epgScheduleCorrectionMinutes * 60_000L
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
        healthSnapshot = effectiveHealth,
        modifier = modifier,
        sidebarFocusRequester = sidebarFocusRequester,
        fallbackFocusRequester = fallbackFocusRequester,
        playerFocusRequester = playerFocusRequester,
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
    healthSnapshot: com.fortresstv.player.telemetry.ChannelHealthSnapshot? = null,
    modifier: Modifier = Modifier,
    sidebarFocusRequester: FocusRequester? = null,
    fallbackFocusRequester: FocusRequester? = null,
    playerFocusRequester: FocusRequester? = null,
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
                if (isTv && (keyEvent.type == KeyEventType.KeyDown || keyEvent.type == KeyEventType.KeyUp)) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_MENU,
                        android.view.KeyEvent.KEYCODE_STAR -> {
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                onToggleFavorite?.invoke()
                            }
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                if (sidebarFocusRequester != null) {
                                    val handled = runCatching { sidebarFocusRequester.requestFocus() }.isSuccess
                                    if (!handled && fallbackFocusRequester != null) {
                                        runCatching { fallbackFocusRequester.requestFocus() }
                                    }
                                }
                            }
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (playerFocusRequester != null) {
                                if (keyEvent.type == KeyEventType.KeyDown) {
                                    runCatching { playerFocusRequester.requestFocus() }
                                }
                                true
                            } else false
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        if (healthSnapshot != null) {
                            com.fortresstv.player.telemetry.ConnectionHealthDot(snapshot = healthSnapshot)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            channel.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
internal fun ChannelLogo(
    channel: PlaylistChannel,
    epgChannel: com.fortresstv.player.epg.EpgChannel?,
    modifier: Modifier = Modifier.size(48.dp)
) {
    val context = LocalContext.current
    val logoUrl = remember(channel.logoUrl, epgChannel?.iconUrl) { channel.logoUrl ?: epgChannel?.iconUrl }
    val logoRequest = remember(logoUrl) {
        logoUrl?.takeIf(String::isNotBlank)?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .size(96, 96)
                .precision(Precision.INEXACT)
                .allowHardware(true)
                .crossfade(false)
                .build()
        }
    }
    var failed by remember(logoUrl) { mutableStateOf(false) }
    Box(
        modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (logoRequest != null && !failed) {
            AsyncImage(
                model = logoRequest,
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
    preferredAudioLanguage: String = "default",
    preferredSubtitleLanguage: String = "off",
    subtitleFallbackEnglish: Boolean = true,
    onPreferredAudioLanguageChanged: ((String) -> Unit)? = null,
    onPreferredSubtitleLanguageChanged: ((String) -> Unit)? = null,
    onChannelSelected: (PlaylistChannel) -> Unit,
    modifier: Modifier = Modifier,
    compactPreview: Boolean = false,
    playerFocusRequester: FocusRequester? = null,
    channelListFocusRequester: FocusRequester? = null
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val activity = context.findActivity()
    val lifecycleOwner = activity as? LifecycleOwner
    val playbackMode = LocalPlaybackMode.current
    val liveTransferProgress = remember { PlaybackTransferProgress() }
    val liveFrameProgress = remember { PlaybackFrameProgress(android.os.SystemClock.elapsedRealtime()) }
    val telemetryStore = remember { com.fortresstv.player.telemetry.TelemetryStore.get(context.applicationContext) }
    val channelHealthRepo = remember { com.fortresstv.player.telemetry.ChannelHealthRepository.get(context.applicationContext) }
    val healthSnapshots by channelHealthRepo.snapshots.collectAsState()
    var showHealthDialog by remember { mutableStateOf(false) }
    val currentHouseholdId = remember { com.fortresstv.player.account.AccountRepository.get(context.applicationContext).currentUserId() ?: "local" }
    val currentProfileId = remember { com.fortresstv.player.profile.ProfileRepository.get(context.applicationContext).activeProfile.value.id }
    var activeChannel by remember(channel) { mutableStateOf(channel) }
    var attemptedFailoverUrls by remember(channel) { mutableStateOf(setOf(channel.streamUrl)) }
    var failoverMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(failoverMessage) {
        if (failoverMessage != null) {
            delay(4500)
            failoverMessage = null
        }
    }
    val liveChannelId = remember(activeChannel) { activeChannel.tvgId?.takeIf { it.isNotBlank() } ?: activeChannel.name }
    val currentHealth = healthSnapshots[liveChannelId]
    val adaptiveLoadControl = remember {
        com.fortresstv.player.player.AdaptiveLoadControl(
            initialHealthState = currentHealth?.state ?: com.fortresstv.player.telemetry.ConnectionHealthState.UNKNOWN
        )
    }
    val player = remember(playbackMode) {
        FortressPlayerFactory.create(
            context = context,
            type = PlaybackContentType.LIVE,
            mode = playbackMode,
            transferProgress = liveTransferProgress,
            frameProgress = liveFrameProgress,
            customLoadControl = adaptiveLoadControl
        ).apply {
            playWhenReady = true
        }
    }
    LaunchedEffect(liveChannelId, currentHealth?.state) {
        adaptiveLoadControl.updateHealthState(currentHealth?.state ?: com.fortresstv.player.telemetry.ConnectionHealthState.UNKNOWN)
    }
    val tracker = remember(activeChannel.streamUrl, player) {
        com.fortresstv.player.telemetry.PlaybackHealthTracker(
            householdId = currentHouseholdId,
            profileId = currentProfileId,
            channelId = liveChannelId,
            streamKind = com.fortresstv.player.telemetry.StreamKindWire.LIVE,
            providerId = activeChannel.sourceId,
            onActiveMetricsUpdated = { startMs, stalls, stallMs, errCode, reg ->
                channelHealthRepo.recordActiveMetrics(
                    channelId = liveChannelId,
                    startupMs = startMs,
                    stallCount = stalls,
                    totalStallMs = stallMs,
                    errorCode = errCode,
                    region = reg
                )
            },
            onFinalized = { summary ->
                channelHealthRepo.recordSummary(summary)
                telemetryStore.enqueue(summary)
            }
        )
    }
    DisposableEffect(activeChannel.streamUrl, player) {
        player.addAnalyticsListener(tracker)
        onDispose {
            tracker.finalizeSession()
            player.removeAnalyticsListener(tracker)
        }
    }
    val liveNetworkGeneration = rememberPlaybackNetworkGeneration()
    val configuration = LocalConfiguration.current
    val isTv = LocalFormFactor.current.isTelevision
    val isCompact = configuration.screenWidthDp < 600
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var isFreezeReconnecting by remember { mutableStateOf(false) }
    var consecutiveStallRecoveries by remember(channel.streamUrl) { mutableIntStateOf(0) }
    var manualFullscreen by rememberSaveable { mutableStateOf(autoEnterFullscreen) }
    var landscapeFullscreenDismissed by rememberSaveable(isPortrait) { mutableStateOf(false) }
    val fullscreen = manualFullscreen || (isCompact && !isPortrait && !landscapeFullscreenDismissed)
    var wasFullscreen by remember { mutableStateOf(fullscreen) }

    LaunchedEffect(fullscreen) {
        if (wasFullscreen && !fullscreen) {
            if (isTv && channelListFocusRequester != null) {
                runCatching { channelListFocusRequester.requestFocus() }
                for (attempt in 0..12) {
                    delay(25)
                    val success = runCatching {
                        channelListFocusRequester.requestFocus()
                    }.isSuccess
                    if (success && attempt >= 2) break
                }
            }
        }
        wasFullscreen = fullscreen
    }

    val inPictureInPicture = (activity as? MainActivity)?.pictureInPictureMode == true
    var previousWatchedChannel by remember { mutableStateOf<PlaylistChannel?>(null) }
    var currentTrackedChannel by remember { mutableStateOf(channel) }

    LaunchedEffect(liveNetworkGeneration) {
        if (liveNetworkGeneration > 0 && player.currentMediaItem != null &&
            (player.playbackState == Player.STATE_BUFFERING || player.playerError != null || isFreezeReconnecting)
        ) {
            val recoveringItem = player.currentMediaItem
            delay(750)
            if (!player.playWhenReady || player.currentMediaItem != recoveringItem ||
                (player.playbackState != Player.STATE_BUFFERING && player.playerError == null && !isFreezeReconnecting)
            ) return@LaunchedEffect
            playbackError = null
            isFreezeReconnecting = false
            liveFrameProgress.recordFrame(android.os.SystemClock.elapsedRealtime())
            player.prepare()
            player.play()
        }
    }

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
        val unregisterSleepTimerPlayer = (activity as? MainActivity)?.registerSleepTimerPlayer(player) {
            manualFullscreen = false
            landscapeFullscreenDismissed = true
            player.clearMediaItems()
        }
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
                if (state == Player.STATE_BUFFERING) {
                    isFreezeReconnecting = false
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    consecutiveStallRecoveries = 0
                    liveFrameProgress.recordFrame(android.os.SystemClock.elapsedRealtime())
                    isFreezeReconnecting = false
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isFreezeReconnecting = false
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    playbackError = null
                    player.seekToDefaultPosition()
                    player.prepare()
                    player.play()
                } else {
                    val candidates = com.fortresstv.player.player.ChannelFailoverResolver.findFailoverCandidates(
                        failedChannel = activeChannel,
                        allChannels = channels,
                        attemptedUrls = attemptedFailoverUrls,
                        healthRepo = channelHealthRepo
                    )
                    val fallback = candidates.firstOrNull()
                    if (fallback != null) {
                        Log.i("FortressTV.Failover", "Failing over from ${activeChannel.name} to ${fallback.name} (${fallback.streamUrl})")
                        attemptedFailoverUrls = attemptedFailoverUrls + fallback.streamUrl
                        playbackError = null
                        failoverMessage = if (fallback.sourceId.isNotBlank() && fallback.sourceId != channel.sourceId) {
                            strings.failoverSourceSwitched(fallback.sourceId)
                        } else {
                            strings.failoverBackupSwitched
                        }
                        activeChannel = fallback
                    } else {
                        playbackError = error.message ?: strings.playbackError
                    }
                }
            }
        }
        val lifecycleObserver = object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                tracker.finalizeSession()
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
            unregisterSleepTimerPlayer?.invoke()
            player.release()
        }
    }

    LaunchedEffect(player, activeChannel.streamUrl) {
        if (currentTrackedChannel != activeChannel) {
            previousWatchedChannel = currentTrackedChannel
            currentTrackedChannel = activeChannel
        }
        playbackError = null
        isFreezeReconnecting = false
        liveFrameProgress.recordFrame(android.os.SystemClock.elapsedRealtime())
        player.setMediaItem(FortressPlayerFactory.mediaItem(activeChannel.streamUrl, PlaybackContentType.LIVE, playbackMode))
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

    LaunchedEffect(player, activeChannel.streamUrl, playbackMode) {
        val intervalMs = when (playbackMode) {
            PlaybackMode.AUTO -> 15_000L
            PlaybackMode.STABLE -> 25_000L
            PlaybackMode.DATA_SAVER -> 18_000L
        }
        fun isStillBuffering() = player.playbackState == Player.STATE_BUFFERING &&
            player.playWhenReady && playbackError == null
        while (isActive) {
            val decision = awaitBufferingStall(intervalMs, sample = {
                if (!isStillBuffering()) null else BufferingSample(
                    player.currentPosition, player.bufferedPosition,
                    liveTransferProgress.bytesObserved, context.isPlaybackNetworkAvailable(),
                    consecutiveStallRecoveries
                )
            })
            when (decision) {
                StallDecision.GIVE_UP -> {
                    val candidates = com.fortresstv.player.player.ChannelFailoverResolver.findFailoverCandidates(
                        failedChannel = activeChannel,
                        allChannels = channels,
                        attemptedUrls = attemptedFailoverUrls,
                        healthRepo = channelHealthRepo
                    )
                    val fallback = candidates.firstOrNull()
                    if (fallback != null) {
                        Log.i("FortressTV.Failover", "Failing over from stall on ${activeChannel.name} to ${fallback.name} (${fallback.streamUrl})")
                        attemptedFailoverUrls = attemptedFailoverUrls + fallback.streamUrl
                        playbackError = null
                        failoverMessage = if (fallback.sourceId.isNotBlank() && fallback.sourceId != channel.sourceId) {
                            strings.failoverSourceSwitched(fallback.sourceId)
                        } else {
                            strings.failoverBackupSwitched
                        }
                        activeChannel = fallback
                    } else {
                        playbackError = strings.playbackError
                        player.stop()
                    }
                }
                StallDecision.RECOVER -> {
                    val backoffMs = 1_000L shl consecutiveStallRecoveries.coerceAtMost(3)
                    delay(backoffMs)
                    // Playback or connectivity may have changed during backoff.
                    if (isStillBuffering() && context.isPlaybackNetworkAvailable()) {
                        consecutiveStallRecoveries++
                        Log.w("FortressTV.Player", "Recovering stalled live playback; attempt=$consecutiveStallRecoveries")
                        player.stop()
                        player.setMediaItem(FortressPlayerFactory.mediaItem(activeChannel.streamUrl, PlaybackContentType.LIVE, playbackMode))
                        player.prepare()
                        player.play()
                    }
                }
                else -> Unit
            }
            delay(1_000L)
        }
    }

    LaunchedEffect(player, activeChannel.streamUrl) {
        var diagnosticTicks = 0
        while (isActive) {
            delay(1_000L)
            if (++diagnosticTicks % 10 == 0) {
                player.videoDecoderCounters?.let { counters ->
                    counters.ensureUpdated()
                    Log.d(
                        "FortressTV.QoE",
                        "type=LIVE rendered=${counters.renderedOutputBufferCount} dropped=${counters.droppedBufferCount} " +
                            "skipped=${counters.skippedOutputBufferCount} maxConsecutiveDropped=${counters.maxConsecutiveDroppedBufferCount}"
                    )
                }
            }
            if (playbackError != null || !player.playWhenReady || playbackState == Player.STATE_BUFFERING) {
                isFreezeReconnecting = false
                continue
            }
            val hasVideo = player.currentTracks.isTypeSelected(C.TRACK_TYPE_VIDEO)
            val millisSinceLastFrame = android.os.SystemClock.elapsedRealtime() - liveFrameProgress.lastFrameRealtimeMs
            val isPlaying = player.isPlaying
            val networkAvailable = context.isPlaybackNetworkAvailable()

            val showNotice = ReadyFreezePolicy.shouldShowNotice(
                isPlaying = isPlaying,
                hasVideo = hasVideo,
                millisSinceLastFrame = millisSinceLastFrame
            )
            isFreezeReconnecting = showNotice

            if (ReadyFreezePolicy.shouldRecover(
                isPlaying = isPlaying,
                hasVideo = hasVideo,
                networkAvailable = networkAvailable,
                millisSinceLastFrame = millisSinceLastFrame
            )) {
                if (consecutiveStallRecoveries >= 3) {
                    val candidates = com.fortresstv.player.player.ChannelFailoverResolver.findFailoverCandidates(
                        failedChannel = activeChannel,
                        allChannels = channels,
                        attemptedUrls = attemptedFailoverUrls,
                        healthRepo = channelHealthRepo
                    )
                    val fallback = candidates.firstOrNull()
                    if (fallback != null) {
                        Log.i("FortressTV.Failover", "Failing over from freeze on ${activeChannel.name} to ${fallback.name} (${fallback.streamUrl})")
                        attemptedFailoverUrls = attemptedFailoverUrls + fallback.streamUrl
                        playbackError = null
                        failoverMessage = if (fallback.sourceId.isNotBlank() && fallback.sourceId != channel.sourceId) {
                            strings.failoverSourceSwitched(fallback.sourceId)
                        } else {
                            strings.failoverBackupSwitched
                        }
                        activeChannel = fallback
                    } else {
                        playbackError = strings.playbackError
                        isFreezeReconnecting = false
                        player.stop()
                    }
                } else {
                    consecutiveStallRecoveries++
                    val backoffMs = 1_000L shl (consecutiveStallRecoveries - 1).coerceAtMost(3)
                    Log.w("FortressTV.Player", "Recovering frozen ready live playback; no frames for ${millisSinceLastFrame}ms attempt=$consecutiveStallRecoveries")
                    delay(backoffMs)
                    if (!context.isPlaybackNetworkAvailable()) continue
                    liveFrameProgress.recordFrame(android.os.SystemClock.elapsedRealtime())
                    player.stop()
                    player.setMediaItem(FortressPlayerFactory.mediaItem(activeChannel.streamUrl, PlaybackContentType.LIVE, playbackMode))
                    player.prepare()
                    player.play()
                }
            }
        }
    }

    if (compactPreview && !fullscreen) {
        Box(modifier.fillMaxSize()) {
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
                isReconnecting = isFreezeReconnecting,
                failoverMessage = failoverMessage,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (fullscreen) {
            FullscreenPlayer(
                player = player,
                playbackState = playbackState,
                playbackError = playbackError,
                channel = activeChannel,
                channels = channels,
                groups = groups,
                favoriteChannelIds = favoriteChannelIds,
                recentChannelIds = recentChannelIds,
                onToggleFavorite = onToggleFavorite,
                previousWatchedChannel = previousWatchedChannel,
                healthSnapshot = currentHealth,
                onOpenHealthDialog = { showHealthDialog = true },
                preferredAudioLanguage = preferredAudioLanguage,
                preferredSubtitleLanguage = preferredSubtitleLanguage,
                subtitleFallbackEnglish = subtitleFallbackEnglish,
                onPreferredAudioLanguageChanged = onPreferredAudioLanguageChanged,
                onPreferredSubtitleLanguageChanged = onPreferredSubtitleLanguageChanged,
                epgGuide = epgGuide,
                epgTimeShiftHours = epgTimeShiftHours,
                onExit = {
                    manualFullscreen = false
                    landscapeFullscreenDismissed = true
                    if (isTv && channelListFocusRequester != null) {
                        runCatching { channelListFocusRequester.requestFocus() }
                    }
                },
                onChannelSelected = ::handleChannelSwitch,
                onRetry = {
                    playbackError = null
                    player.prepare()
                    player.play()
                },
                isReconnecting = isFreezeReconnecting,
                failoverMessage = failoverMessage,
                inPictureInPicture = inPictureInPicture
            )
        }
        return
    }

    Column(modifier) {
        if (!fullscreen) {
            val isTv = LocalFormFactor.current.isTelevision
            var isPlayerFocused by remember { mutableStateOf(false) }
            val borderStroke = if (isPlayerFocused) {
                BorderStroke(3.5.dp, MaterialTheme.colorScheme.primary)
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            }
            Surface(
                onClick = { manualFullscreen = true; landscapeFullscreenDismissed = false },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = borderStroke,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .then(if (playerFocusRequester != null) Modifier.focusRequester(playerFocusRequester) else Modifier)
                    .onFocusChanged { isPlayerFocused = it.isFocused }
                    .onPreviewKeyEvent { keyEvent ->
                        if (isTv && (keyEvent.type == KeyEventType.KeyDown || keyEvent.type == KeyEventType.KeyUp)) {
                            if (keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT) {
                                if (keyEvent.type == KeyEventType.KeyDown && channelListFocusRequester != null) {
                                    runCatching { channelListFocusRequester.requestFocus() }
                                }
                                return@onPreviewKeyEvent true
                            }
                        }
                        false
                    }
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(if (isPlayerFocused) 4.dp else 1.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
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
                        isReconnecting = isFreezeReconnecting,
                        failoverMessage = failoverMessage,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(activeChannel.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.width(10.dp))
                com.fortresstv.player.telemetry.ConnectionHealthBadge(
                    snapshot = currentHealth,
                    onClick = { showHealthDialog = true }
                )
            }
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
            channel = activeChannel,
            epgGuide = epgGuide,
            epgTimeShiftHours = epgTimeShiftHours,
            channels = channels,
            groups = groups,
            favoriteChannelIds = favoriteChannelIds,
            recentChannelIds = recentChannelIds,
            onToggleFavorite = onToggleFavorite,
            previousWatchedChannel = previousWatchedChannel,
            healthSnapshot = currentHealth,
            onOpenHealthDialog = { showHealthDialog = true },
            preferredAudioLanguage = preferredAudioLanguage,
            preferredSubtitleLanguage = preferredSubtitleLanguage,
            subtitleFallbackEnglish = subtitleFallbackEnglish,
            onPreferredAudioLanguageChanged = onPreferredAudioLanguageChanged,
            onPreferredSubtitleLanguageChanged = onPreferredSubtitleLanguageChanged,
            onChannelSelected = ::handleChannelSwitch,
            onExit = {
                manualFullscreen = false
                landscapeFullscreenDismissed = true
                if (isTv && channelListFocusRequester != null) {
                    runCatching { channelListFocusRequester.requestFocus() }
                }
            },
            onRetry = {
                playbackError = null
                player.prepare()
                player.play()
            },
            isReconnecting = isFreezeReconnecting,
            failoverMessage = failoverMessage,
            inPictureInPicture = inPictureInPicture
        )
    }

    if (showHealthDialog) {
        val snap = currentHealth ?: com.fortresstv.player.telemetry.ChannelHealthSnapshot.fromMetrics(
            channelId = liveChannelId,
            startupMs = 0L,
            stallCount = 0,
            totalStallMs = 0L,
            errorCode = null,
            region = com.fortresstv.player.telemetry.PlaybackHealthTracker.resolveRegion(),
            isLiveTesting = true
        )
        com.fortresstv.player.telemetry.ConnectionHealthDialog(
            channelName = activeChannel.name,
            snapshot = snap,
            onDismiss = { showHealthDialog = false }
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
    isReconnecting: Boolean = false,
    failoverMessage: String? = null,
    onCenterSwipeUp: (() -> Unit)? = null,
    onCenterSwipeDown: (() -> Unit)? = null,
    onEdgeSwipeRight: (() -> Unit)? = null,
    onHorizontalSeek: ((distanceX: Float, finished: Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val isTv = LocalFormFactor.current.isTelevision
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
    var isProlongedBuffering by remember { mutableStateOf(false) }
    LaunchedEffect(playbackState) {
        if (playbackState == Player.STATE_BUFFERING) {
            isProlongedBuffering = false
            delay(3_500)
            isProlongedBuffering = true
        } else {
            isProlongedBuffering = false
        }
    }
    Box(
        modifier.background(Color.Black)
    ) {
        AndroidView(
            factory = { viewContext ->
                val view = if (fullscreen) {
                    GesturePlayerView(viewContext).apply {
                        this.onGestureLevelChanged = { gestureLevel = it }
                        this.onSingleTap = { onSurfaceClick() }
                        this.onCenterSwipeUp = onCenterSwipeUp
                        this.onCenterSwipeDown = onCenterSwipeDown
                        this.onEdgeSwipeRight = onEdgeSwipeRight
                        this.onHorizontalSeek = onHorizontalSeek
                    }
                } else {
                    PlayerView(viewContext).apply {
                        isClickable = false
                        isFocusable = false
                        descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    }
                }
                view.apply {
                    this.player = player
                    useController = showTrackSettings
                    controllerAutoShow = showTrackSettings
                    controllerShowTimeoutMs = 3_500
                    this.resizeMode = resizeMode
                    keepScreenOn = true
                    if (isTv) {
                        isClickable = false
                        isFocusable = false
                        isFocusableInTouchMode = false
                        descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    } else if (!fullscreen) {
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
                if (isTv) {
                    it.isClickable = false
                    it.isFocusable = false
                    it.isFocusableInTouchMode = false
                    it.descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
                } else if (it is GesturePlayerView) {
                    it.onGestureLevelChanged = { gestureLevel = it }
                    it.onSingleTap = { onSurfaceClick() }
                    it.onCenterSwipeUp = onCenterSwipeUp
                    it.onCenterSwipeDown = onCenterSwipeDown
                    it.onEdgeSwipeRight = onEdgeSwipeRight
                    it.onHorizontalSeek = onHorizontalSeek
                    it.setOnClickListener(null)
                } else {
                    it.setOnClickListener { onSurfaceClick() }
                }
            },
            modifier = Modifier.fillMaxSize().focusProperties { canFocus = false }
        )
        if (playbackState == Player.STATE_BUFFERING || isReconnecting) {
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                AnimatedVisibility(
                    visible = isProlongedBuffering || isReconnecting,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        if (isReconnecting) strings.reconnecting else strings.buildingPlaybackBuffer,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
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
            visible = failoverMessage != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (fullscreen) 24.dp else 12.dp)
                .zIndex(10f)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.85f),
                contentColor = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = failoverMessage.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = gestureLevel != null,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
            modifier = Modifier
                .align(
                    if (gestureLevel?.control == GestureControl.BRIGHTNESS) Alignment.CenterStart else Alignment.CenterEnd
                )
                .padding(horizontal = 36.dp)
        ) {
            val level = gestureLevel
            if (level != null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.82f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val icon = if (level.control == GestureControl.BRIGHTNESS) {
                            when {
                                level.value < 0.33f -> Icons.Default.BrightnessLow
                                level.value < 0.66f -> Icons.Default.BrightnessMedium
                                else -> Icons.Default.BrightnessHigh
                            }
                        } else {
                            when {
                                level.value <= 0.001f -> Icons.AutoMirrored.Filled.VolumeMute
                                level.value < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                                else -> Icons.AutoMirrored.Filled.VolumeUp
                            }
                        }
                        Icon(
                            icon,
                            contentDescription = if (level.control == GestureControl.BRIGHTNESS) strings.brightness else strings.volume,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(120.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(level.value.coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                        Text(
                            "${(level.value * 100).roundToInt()}%",
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

@Composable
private fun BatteryStatusBadge(contentDescription: String) {
    val context = LocalContext.current
    var percentage by remember { mutableIntStateOf(-1) }

    DisposableEffect(context) {
        fun update(intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            percentage = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else -1
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) = update(intent)
        }
        update(context.registerReceiver(null, filter))
        context.registerReceiver(receiver, filter)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    if (percentage >= 0) {
        val tint = when {
            percentage <= 15 -> Color(0xFFFF6B6B)
            percentage <= 30 -> Color(0xFFFFC15A)
            else -> Color.White
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.BatteryFull,
                    contentDescription = contentDescription,
                    tint = tint,
                    modifier = Modifier.size(17.dp)
                )
                Text("$percentage%", color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
    downTarget: FocusRequester? = null,
    onFocused: (() -> Unit)? = null
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
            .onFocusChanged { if (it.isFocused) onFocused?.invoke() }
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
    healthSnapshot: com.fortresstv.player.telemetry.ChannelHealthSnapshot? = null,
    onOpenHealthDialog: () -> Unit = {},
    preferredAudioLanguage: String = "default",
    preferredSubtitleLanguage: String = "off",
    subtitleFallbackEnglish: Boolean = true,
    onPreferredAudioLanguageChanged: ((String) -> Unit)? = null,
    onPreferredSubtitleLanguageChanged: ((String) -> Unit)? = null,
    onChannelSelected: (PlaylistChannel) -> Unit,
    onExit: () -> Unit,
    onRetry: () -> Unit,
    isReconnecting: Boolean = false,
    failoverMessage: String? = null,
    inPictureInPicture: Boolean
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    var controlsVisible by remember { mutableStateOf(true) }
    var showChannels by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }
    var showTracksDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showQuickZap by remember { mutableStateOf(false) }
    val channelMap = remember(channels) { channels.associateBy { it.libraryId() } }
    val favoriteChannels = remember(channels, favoriteChannelIds) {
        favoriteChannelIds.mapNotNull { channelMap[it] }
    }
    val recentChannels = remember(channels, recentChannelIds) {
        recentChannelIds.mapNotNull { channelMap[it] }
    }
    val sleepTimerActivity = context.findActivity() as? MainActivity
    val sleepTimerMinutes = sleepTimerActivity?.sleepTimerState?.minutes
    val sleepTimerRemainingText = sleepTimerActivity?.sleepTimerState?.remainingMinutes?.let { "${it}m" }
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
    val videoTracks = remember(currentTracks) { extractVideoTracks(currentTracks) }
    var userManuallySelectedTracks by remember(channel) { mutableStateOf(false) }

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

    val isTv = LocalFormFactor.current.isTelevision
    val playerFocusRequester = remember { FocusRequester() }
    val controlsInitialFocusRequester = remember { FocusRequester() }
    val backButtonFocusRequester = remember { FocusRequester() }
    val guideButtonFocusRequester = remember { FocusRequester() }
    val quickZapFocusRequester = remember { FocusRequester() }
    val audioButtonFocusRequester = remember { FocusRequester() }
    val sleepTimerFocusRequester = remember { FocusRequester() }
    val aspectRatioFocusRequester = remember { FocusRequester() }
    val lockButtonFocusRequester = remember { FocusRequester() }
    val centerPrevFocusRequester = remember { FocusRequester() }
    val centerNextFocusRequester = remember { FocusRequester() }
    val favButtonFocusRequester = remember { FocusRequester() }
    val previousWatchedFocusRequester = remember { FocusRequester() }
    val interactionSignal = remember { Channel<Unit>(Channel.CONFLATED) }
    var userNavigated by remember { mutableStateOf(false) }

    var lastFocusedControlRequester by remember { mutableStateOf<FocusRequester?>(null) }

    LaunchedEffect(controlsVisible) {
        if (!controlsVisible) {
            lastFocusedControlRequester = null
        }
        userNavigated = false
    }

    LaunchedEffect(controlsVisible, showChannels, showGuide, showTracksDialog, showSleepTimerDialog, showQuickZap) {
        if (controlsVisible && isTv && !showChannels && !showGuide && !showTracksDialog && !showSleepTimerDialog && !showQuickZap) {
            val target = lastFocusedControlRequester ?: controlsInitialFocusRequester
            userNavigated = false
            delay(100)
            for (attempt in 0..15) {
                if (userNavigated) break
                val ok = runCatching { target.requestFocus() }.isSuccess
                if (ok && attempt >= 2) break
                delay(30)
            }
        } else if (!controlsVisible && isTv && !showChannels && !showGuide && !showTracksDialog && !showSleepTimerDialog && !showQuickZap) {
            lastFocusedControlRequester = null
            userNavigated = false
            delay(50)
            for (attempt in 0..15) {
                val ok = runCatching { playerFocusRequester.requestFocus() }.isSuccess
                if (ok && attempt >= 2) break
                delay(30)
            }
        }
    }

    val isOverlayActive = showChannels || showGuide || showTracksDialog || showSleepTimerDialog || showQuickZap || controlsVisible

    fun dismissTopmostOverlay() {
        when {
            showChannels -> showChannels = false
            showGuide -> showGuide = false
            showTracksDialog -> showTracksDialog = false
            showSleepTimerDialog -> showSleepTimerDialog = false
            showQuickZap -> showQuickZap = false
            controlsVisible -> controlsVisible = false
        }
    }

    BackHandler(enabled = isOverlayActive) {
        dismissTopmostOverlay()
    }

    LaunchedEffect(controlsVisible, showChannels, showGuide, showTracksDialog, showSleepTimerDialog, showQuickZap, locked) {
        if (controlsVisible && !showChannels && !showGuide && !showTracksDialog && !showSleepTimerDialog && !showQuickZap && !locked) {
            val timeoutMs = if (isTv) 10_000L else 3_500L
            while (true) {
                val ping = withTimeoutOrNull(timeoutMs) {
                    interactionSignal.receive()
                }
                if (ping == null) {
                    controlsVisible = false
                    break
                }
            }
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
    val schedule = epgGuide?.snapshot(channel.tvgId, channel.name, scheduleNow, timeShiftHours = epgTimeShiftHours)
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
                .focusProperties {
                    canFocus = !controlsVisible && !isOverlayActive
                }
                .focusable()
                .onPreviewKeyEvent { keyEvent ->
                    val isDpadDirection = when (keyEvent.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_UP,
                        android.view.KeyEvent.KEYCODE_DPAD_DOWN,
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> true
                        else -> false
                    }
                    if (isTv && isOverlayActive && keyEvent.type == KeyEventType.KeyDown) {
                        interactionSignal.trySend(Unit)
                        if (isDpadDirection) {
                            userNavigated = true
                        }
                    }
                    val isBackKey = keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_BACK ||
                        keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ESCAPE
                    if (isTv && isOverlayActive && isBackKey) {
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
                            interactionSignal.trySend(Unit)
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
                            interactionSignal.trySend(Unit)
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
                isReconnecting = isReconnecting,
                failoverMessage = failoverMessage,
                onCenterSwipeUp = {
                    if (!showQuickZap && !showChannels && !showGuide && !locked && !inPictureInPicture) {
                        showQuickZap = true
                        controlsVisible = false
                    }
                },
                onCenterSwipeDown = {
                    if (showQuickZap) {
                        showQuickZap = false
                    }
                },
                onEdgeSwipeRight = {
                    if (!showChannels && !locked && !inPictureInPicture) {
                        showChannels = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // CHANNEL SWITCH HUD TOAST
            AnimatedVisibility(
                visible = channelSwitchNotice != null && !controlsVisible && !showChannels && !showGuide && !showQuickZap && !inPictureInPicture,
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
                visible = controlsVisible && !showChannels && !showGuide && !showQuickZap && !inPictureInPicture,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { controlsVisible = false })
                        }
                ) {
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
                                        downTarget = if (previousChannel != null) centerPrevFocusRequester else if (nextChannel != null) centerNextFocusRequester else favButtonFocusRequester,
                                        onFocused = { lastFocusedControlRequester = backButtonFocusRequester }
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
                                        downTarget = if (previousChannel != null) centerPrevFocusRequester else if (nextChannel != null) centerNextFocusRequester else favButtonFocusRequester,
                                        onFocused = { lastFocusedControlRequester = controlsInitialFocusRequester }
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
                                        rightTarget = quickZapFocusRequester,
                                        downTarget = favButtonFocusRequester,
                                        onFocused = { lastFocusedControlRequester = guideButtonFocusRequester }
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
                            Surface(
                                onClick = { showQuickZap = true },
                                shape = if (isCompact) CircleShape else RoundedCornerShape(20.dp),
                                color = if (showQuickZap) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, if (showQuickZap) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f)),
                                modifier = Modifier
                                    .tvFullscreenControl(
                                        enabled = isTv,
                                        requester = quickZapFocusRequester,
                                        shape = if (isCompact) CircleShape else RoundedCornerShape(20.dp),
                                        scaleOnFocus = 1.08f,
                                        leftTarget = guideButtonFocusRequester,
                                        rightTarget = if (audioTracks.isNotEmpty() || subtitleTracks.isNotEmpty()) audioButtonFocusRequester else sleepTimerFocusRequester,
                                        downTarget = favButtonFocusRequester,
                                        onFocused = { lastFocusedControlRequester = quickZapFocusRequester }
                                    )
                            ) {
                                Row(
                                    Modifier.padding(if (isCompact) PaddingValues(9.dp) else PaddingValues(horizontal = 12.dp, vertical = 7.dp)),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Bolt,
                                        contentDescription = strings.quickZap,
                                        tint = Color(0xFFFFB800),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    if (!isCompact) {
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            strings.quickZap,
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
                                            leftTarget = quickZapFocusRequester,
                                            rightTarget = sleepTimerFocusRequester,
                                            downTarget = favButtonFocusRequester,
                                            onFocused = { lastFocusedControlRequester = audioButtonFocusRequester }
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
                            if (!isTv) {
                                BatteryStatusBadge(contentDescription = strings.battery)
                            }
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
                                        leftTarget = if (audioTracks.isNotEmpty() || subtitleTracks.isNotEmpty()) audioButtonFocusRequester else quickZapFocusRequester,
                                        rightTarget = aspectRatioFocusRequester,
                                        downTarget = if (nextChannel != null) centerNextFocusRequester else if (previousChannel != null) centerPrevFocusRequester else favButtonFocusRequester,
                                        onFocused = { lastFocusedControlRequester = sleepTimerFocusRequester }
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
                                            fontWeight = FontWeight.Bold,
                                            style = TabularDigitsTextStyle
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
                                        downTarget = if (nextChannel != null) centerNextFocusRequester else if (previousChannel != null) centerPrevFocusRequester else favButtonFocusRequester,
                                        onFocused = { lastFocusedControlRequester = aspectRatioFocusRequester }
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
                                        downTarget = if (nextChannel != null) centerNextFocusRequester else if (previousChannel != null) centerPrevFocusRequester else favButtonFocusRequester,
                                        onFocused = { lastFocusedControlRequester = lockButtonFocusRequester }
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
                                    leftTarget = backButtonFocusRequester,
                                    rightTarget = if (nextChannel != null) centerNextFocusRequester else favButtonFocusRequester,
                                    upTarget = controlsInitialFocusRequester,
                                    downTarget = favButtonFocusRequester,
                                    onFocused = { lastFocusedControlRequester = centerPrevFocusRequester }
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
                                    leftTarget = if (previousChannel != null) centerPrevFocusRequester else controlsInitialFocusRequester,
                                    rightTarget = lockButtonFocusRequester,
                                    upTarget = aspectRatioFocusRequester,
                                    downTarget = if (previousWatchedChannel != null) previousWatchedFocusRequester else favButtonFocusRequester,
                                    onFocused = { lastFocusedControlRequester = centerNextFocusRequester }
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
                                                    leftTarget = if (previousChannel != null) centerPrevFocusRequester else backButtonFocusRequester,
                                                    rightTarget = if (previousWatchedChannel != null) previousWatchedFocusRequester else if (nextChannel != null) centerNextFocusRequester else null,
                                                    upTarget = if (previousChannel != null) centerPrevFocusRequester else if (nextChannel != null) centerNextFocusRequester else controlsInitialFocusRequester,
                                                    onFocused = { lastFocusedControlRequester = favButtonFocusRequester }
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
                                        Spacer(Modifier.width(6.dp))
                                        com.fortresstv.player.telemetry.ConnectionHealthBadge(
                                            snapshot = healthSnapshot,
                                            onClick = onOpenHealthDialog
                                        )
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
                                        modifier = Modifier.tvFullscreenControl(
                                            enabled = isTv,
                                            requester = previousWatchedFocusRequester,
                                            shape = RoundedCornerShape(10.dp),
                                            scaleOnFocus = 1.08f,
                                            leftTarget = favButtonFocusRequester,
                                            rightTarget = if (nextChannel != null) centerNextFocusRequester else null,
                                            upTarget = if (nextChannel != null) centerNextFocusRequester else aspectRatioFocusRequester,
                                            onFocused = { lastFocusedControlRequester = previousWatchedFocusRequester }
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
                                        fontSize = 11.sp,
                                        style = TabularDigitsTextStyle
                                    )
                                    Text(
                                        strings.endsAt(formatProgrammeTime(Instant.ofEpochMilli(currentStopEpoch))),
                                        color = Color.White.copy(alpha = .65f),
                                        fontSize = 11.sp,
                                        style = TabularDigitsTextStyle
                                    )
                                }
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = showTracksDialog && !inPictureInPicture,
                enter = slideInHorizontally { it } + fadeIn(),
                exit = slideOutHorizontally { it } + fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                PlayerControlsDrawer(
                    player = player,
                    audioTracks = audioTracks,
                    subtitleTracks = subtitleTracks,
                    videoTracks = videoTracks,
                    isLive = true,
                    onUserTrackSelected = { userManuallySelectedTracks = true },
                    onAudioLanguageSelected = onPreferredAudioLanguageChanged,
                    onSubtitleLanguageSelected = onPreferredSubtitleLanguageChanged,
                    onClose = { showTracksDialog = false }
                )
            }
            if (sleepTimerActivity?.sleepTimerState?.awaitingConfirmation == true) {
                SleepTimerConfirmationDialog(
                    secondsRemaining = sleepTimerActivity.sleepTimerState.confirmationSeconds ?: 30,
                    canContinue = true,
                    canPlayNextThenStop = false,
                    onStop = { sleepTimerActivity.playbackSleepTimer.stopNow() },
                    onContinue = {
                        sleepTimerActivity.playbackSleepTimer.select(null)
                        player.play()
                    },
                    onPlayNextThenStop = {},
                    onSetTimer = { minutes ->
                        sleepTimerActivity.playbackSleepTimer.select(minutes)
                        player.play()
                    }
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
                            Text(strings.sleepTimerHint, style = MaterialTheme.typography.bodySmall)
                            options.forEach { (mins, label) ->
                                val isSelected = sleepTimerMinutes == mins
                                Surface(
                                    onClick = {
                                        sleepTimerActivity?.playbackSleepTimer?.select(
                                            mins,
                                            if (mins == -1) currentStopEpoch?.minus(System.currentTimeMillis()) else null
                                        )
                                        if (mins != null && mins > 0) {
                                            Toast.makeText(context, strings.sleepTimerSet(mins), Toast.LENGTH_SHORT).show()
                                        } else if (mins == -1 && currentStopEpoch != null) {
                                            Toast.makeText(context, strings.endOfProgramme, Toast.LENGTH_SHORT).show()
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
                visible = showQuickZap && !inPictureInPicture,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                QuickZapTray(
                    channels = channels,
                    favoriteChannels = favoriteChannels,
                    recentChannels = recentChannels,
                    currentChannel = channel,
                    epgGuide = epgGuide,
                    epgTimeShiftHours = epgTimeShiftHours,
                    scheduleNow = scheduleNow,
                    onChannelSelected = { next ->
                        onChannelSelected(next)
                    },
                    onClose = { showQuickZap = false }
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
private fun QuickZapTray(
    channels: List<PlaylistChannel>,
    favoriteChannels: List<PlaylistChannel>,
    recentChannels: List<PlaylistChannel>,
    currentChannel: PlaylistChannel,
    epgGuide: EpgGuide?,
    epgTimeShiftHours: Int,
    scheduleNow: Instant,
    onChannelSelected: (PlaylistChannel) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val healthSnapshots by remember { com.fortresstv.player.telemetry.ChannelHealthRepository.get(context.applicationContext).snapshots }.collectAsState()
    val strings = LocalAppStrings.current
    val isTv = LocalFormFactor.current.isTelevision
    val tabFocusRequesters = remember { List(2) { FocusRequester() } }
    val channelFocusRequester = remember { FocusRequester() }
    val closeFocusRequester = remember { FocusRequester() }
    var selectedTab by remember(favoriteChannels.isEmpty(), recentChannels.isEmpty()) {
        mutableIntStateOf(if (favoriteChannels.isNotEmpty()) 0 else if (recentChannels.isNotEmpty()) 1 else 0)
    }
    val activeChannels = when (selectedTab) {
        0 -> if (favoriteChannels.isNotEmpty()) favoriteChannels else if (recentChannels.isEmpty()) channels.take(30) else emptyList()
        else -> if (recentChannels.isNotEmpty()) recentChannels else if (favoriteChannels.isEmpty()) channels.take(30) else emptyList()
    }

    var focusedChannelIndex by remember(selectedTab) {
        val targetIdx = activeChannels.indexOfFirst { it == currentChannel }.takeIf { it >= 0 } ?: 0
        mutableIntStateOf(targetIdx)
    }

    val listState = rememberLazyListState()

    LaunchedEffect(selectedTab) {
        if (activeChannels.isNotEmpty()) {
            val target = focusedChannelIndex.coerceIn(0, activeChannels.lastIndex)
            runCatching { listState.scrollToItem(target) }
        }
    }

    BackHandler {
        onClose()
    }

    LaunchedEffect(Unit) {
        if (isTv) {
            repeat(10) {
                val success = runCatching {
                    tabFocusRequesters[selectedTab.coerceIn(tabFocusRequesters.indices)].requestFocus()
                }.isSuccess
                if (success) return@LaunchedEffect
                delay(30)
            }
        }
    }

    fun tabNavigation(index: Int) = Modifier
        .focusRequester(tabFocusRequesters[index])
        .focusProperties {
            if (isTv) {
                up = FocusRequester.Cancel
                left = if (index == 0) FocusRequester.Cancel else tabFocusRequesters[0]
                right = if (index == 0) tabFocusRequesters[1] else closeFocusRequester
                down = if (activeChannels.isEmpty()) FocusRequester.Cancel else channelFocusRequester
            }
        }

    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = Color(0xFF141414).copy(alpha = 0.94f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        shadowElevation = 16.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color(0xFFFFB800),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        strings.quickZap,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        onClick = {
                            selectedTab = 0
                            if (activeChannels.isNotEmpty()) {
                                runCatching { channelFocusRequester.requestFocus() }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
                        contentColor = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.7f),
                        modifier = tabNavigation(0)
                            .onFocusChanged { state ->
                                if (state.isFocused && isTv) {
                                    selectedTab = 0
                                }
                            }
                            .tvFocusHighlight(shape = RoundedCornerShape(16.dp), scaleOnFocus = 1.08f)
                    ) {
                        Text(
                            "${strings.quickZapFavorites} (${favoriteChannels.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        onClick = {
                            selectedTab = 1
                            if (activeChannels.isNotEmpty()) {
                                runCatching { channelFocusRequester.requestFocus() }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
                        contentColor = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.7f),
                        modifier = tabNavigation(1)
                            .onFocusChanged { state ->
                                if (state.isFocused && isTv) {
                                    selectedTab = 1
                                }
                            }
                            .tvFocusHighlight(shape = RoundedCornerShape(16.dp), scaleOnFocus = 1.08f)
                    ) {
                        Text(
                            "${strings.quickZapRecent} (${recentChannels.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                        .focusRequester(closeFocusRequester)
                        .focusProperties {
                            if (isTv) {
                                left = tabFocusRequesters[1]
                                right = FocusRequester.Cancel
                                up = FocusRequester.Cancel
                                down = if (activeChannels.isEmpty()) FocusRequester.Cancel else channelFocusRequester
                            }
                        }
                        .tvFocusHighlight(shape = CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = strings.close,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            if (activeChannels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (selectedTab == 0) strings.noFavoriteChannels else strings.noRecentlyWatched,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyRow(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(
                        items = activeChannels,
                        key = { _, it -> "zap-${it.position}:${it.streamUrl}" }
                    ) { index, itemChannel ->
                        val isCurrent = itemChannel == currentChannel
                        val schedule = remember(itemChannel, scheduleNow, epgGuide, epgTimeShiftHours) {
                            epgGuide?.snapshot(itemChannel.tvgId, itemChannel.name, scheduleNow, timeShiftHours = epgTimeShiftHours)
                        }
                        val isTargetFocusCard = index == focusedChannelIndex.coerceIn(0, activeChannels.lastIndex)
                        val cardFocusModifier = if (isTargetFocusCard) {
                            Modifier.focusRequester(channelFocusRequester)
                        } else {
                            Modifier
                        }

                        Surface(
                            onClick = { onChannelSelected(itemChannel) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f),
                            border = BorderStroke(
                                1.5.dp,
                                if (isCurrent) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier
                                .width(190.dp)
                                .height(68.dp)
                                .then(cardFocusModifier)
                                .onFocusChanged { state ->
                                    if (state.isFocused) {
                                        focusedChannelIndex = index
                                    }
                                }
                                .focusProperties {
                                    if (isTv) {
                                        up = tabFocusRequesters[selectedTab.coerceIn(tabFocusRequesters.indices)]
                                        down = FocusRequester.Cancel
                                        if (index == 0) left = FocusRequester.Cancel
                                        if (index == activeChannels.lastIndex) right = FocusRequester.Cancel
                                    }
                                }
                                .tvFocusHighlight(
                                    shape = RoundedCornerShape(12.dp),
                                    scaleOnFocus = 1.05f
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ChannelLogo(
                                    channel = itemChannel,
                                    epgChannel = epgGuide?.findChannel(itemChannel.tvgId, itemChannel.name),
                                    modifier = Modifier.size(34.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    val zapHealth = itemChannel.sourceItemId.takeIf { it.isNotBlank() }?.let { healthSnapshots[it] }
                                        ?: itemChannel.tvgId?.takeIf { it.isNotBlank() }?.let { healthSnapshots[it] }
                                        ?: healthSnapshots[itemChannel.name]
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (zapHealth != null) {
                                            com.fortresstv.player.telemetry.ConnectionHealthDot(snapshot = zapHealth)
                                            Spacer(Modifier.width(4.dp))
                                        }
                                        Text(
                                            itemChannel.name,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    val currentProg = schedule?.current
                                    if (currentProg != null) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            currentProg.title,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.65f),
                                            fontSize = 11.sp,
                                            maxLines = 1,
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
                                val current = epgGuide?.snapshot(item.tvgId, item.name, scheduleNow, timeShiftHours = epgTimeShiftHours)?.current
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
                                                    maxLines = 1,
                                                    style = TabularDigitsTextStyle
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

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}


@Preview(showBackground = true, widthDp = 1024, heightDp = 600)
@Composable
private fun WelcomePreview() = FortressTVTheme {
    WelcomeScreen(false, true, null, onRetry = null, onAddSource = {})
}

