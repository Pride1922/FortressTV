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

internal data class GuideProgrammeBlock(
    val programme: EpgProgramme,
    val startMinutes: Float,
    val durationMinutes: Float,
    val adjustedStartMillis: Long,
    val adjustedStopMillis: Long,
    val timeLabel: String
)

private const val GUIDE_CHANNEL_BATCH_SIZE = 2

@Composable
internal fun ProgrammeGuideScreen(
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

