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

@UnstableApi
@Composable
internal fun PlayerPanel(
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
        val unregisterSleepTimerPlayer = PlaybackOrchestrationWorkflow.registerSleepTimer(context, player) {
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
internal fun VideoSurface(
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
internal fun BatteryStatusBadge(contentDescription: String) {
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
internal fun Modifier.tvFullscreenControl(
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


