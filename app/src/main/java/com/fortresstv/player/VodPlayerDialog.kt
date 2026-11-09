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
import com.fortresstv.player.trakt.TraktScrobbleService
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

@Composable
internal fun SleepTimerConfirmationDialog(
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
    val playbackNamespace = remember(request.key) { profileProgressNamespace(currentHouseholdId, currentProfileId) }
    val traktScrobbler = remember(playbackNamespace, request.key) {
        TraktScrobbleService(
            context = context.applicationContext,
            profileNamespace = playbackNamespace
        )
    }
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
        val unregisterSleepTimerPlayer = PlaybackOrchestrationWorkflow.registerSleepTimer(context, player, onDismiss)
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
                if (isPlaying) {
                    traktScrobbler.onPlaybackStarted(
                        positionMs = player.currentPosition.coerceAtLeast(0L),
                        durationMs = player.duration.coerceAtLeast(0L),
                        request = request
                    )
                } else {
                    val isBuffering = player.playbackState == Player.STATE_BUFFERING
                    traktScrobbler.onPlaybackPaused(
                        positionMs = player.currentPosition.coerceAtLeast(0L),
                        durationMs = player.duration.coerceAtLeast(0L),
                        request = request,
                        isBuffering = isBuffering
                    )
                    onSaveProgress(player.currentPosition.coerceAtLeast(0L), player.duration.coerceAtLeast(0L))
                }
            }
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                if (state == Player.STATE_ENDED) {
                    traktScrobbler.onPlaybackStopped(
                        positionMs = player.duration.coerceAtLeast(0L),
                        durationMs = player.duration.coerceAtLeast(0L),
                        request = request
                    )
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
            PlaybackOrchestrationWorkflow.updatePiPState(context = context, autoPiPEnabled = false, isFullscreen = false, isPlaying = false)
            mainActivity?.setOnPictureInPictureDismissedListener(null)
            val pos = maxOf(player.currentPosition.coerceAtLeast(0L), currentPositionMs)
            val dur = maxOf(player.duration.coerceAtLeast(0L), totalDurationMs)
            traktScrobbler.onPlaybackStopped(pos, dur, request)
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
internal fun extractAudioTracks(currentTracks: Tracks?): List<PlaybackTrackItem> =
    TrackLanguageMatcher.extractAudioTracks(currentTracks)

@UnstableApi
internal fun extractSubtitleTracks(currentTracks: Tracks?): List<PlaybackTrackItem> =
    TrackLanguageMatcher.extractSubtitleTracks(currentTracks)

@UnstableApi
internal fun extractVideoTracks(currentTracks: Tracks?): List<PlaybackVideoTrackItem> =
    TrackLanguageMatcher.extractVideoTracks(currentTracks)

@UnstableApi
@Composable
internal fun PlayerControlsDrawer(
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
internal fun PlaybackTrackDropdown(
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
internal fun localizedPlaybackTrackLabel(track: PlaybackTrackItem, strings: Strings): String = when {
    track.label.startsWith("OpenSubtitles") -> track.label
    TrackLanguageMatcher.matchesLanguage(track.language, track.label, "pt") -> strings.langPortuguese
    TrackLanguageMatcher.matchesLanguage(track.language, track.label, "en") -> strings.langEnglish
    TrackLanguageMatcher.matchesLanguage(track.language, track.label, "es") -> strings.langSpanish
    TrackLanguageMatcher.matchesLanguage(track.language, track.label, "fr") -> strings.langFrench
    TrackLanguageMatcher.matchesLanguage(track.language, track.label, "de") -> strings.langGerman
    TrackLanguageMatcher.matchesLanguage(track.language, track.label, "it") -> strings.langItalian
    else -> track.label
}




