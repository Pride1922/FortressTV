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
internal fun ImmersiveSystemBars() {
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

