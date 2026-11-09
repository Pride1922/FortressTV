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
internal fun FullscreenPlayer(
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

internal fun formatProgrammeTime(instant: Instant): String = programmeTimeFormatter.format(instant)

@Composable
internal fun QuickZapTray(
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
internal fun FullscreenGuideDrawer(
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

internal fun formatProgrammeRange(startEpochMilli: Long, stopEpochMilli: Long?, timeShiftHours: Int = 0): String {
    val formatter = DateTimeFormatter.ofPattern("EEE d MMM · HH:mm")
    val shiftMillis = timeShiftHours * 3_600_000L
    val startText = Instant.ofEpochMilli(startEpochMilli + shiftMillis).atZone(ZoneId.systemDefault()).format(formatter)
    val stopText = stopEpochMilli?.let {
        Instant.ofEpochMilli(it + shiftMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
    }
    return if (stopText == null) startText else "$startText – $stopText"
}

@Composable
internal fun FullscreenChannelDrawer(
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

