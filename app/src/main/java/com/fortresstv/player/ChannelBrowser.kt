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
    requestedChannelFullscreen: Boolean = false,
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
    var channelSelectedAtMs by remember(playlist) { mutableLongStateOf(0L) }
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
        val selectedAt = if (BuildConfig.BUILD_TYPE == "benchmark") android.os.SystemClock.elapsedRealtime() else 0L
        hideKeyboard()
        if (selectedChannel == channel) {
            fullscreenTrigger++
        } else {
            channelSelectedAtMs = selectedAt
            selectedChannel = channel
        }
        if (!isLandscapeTablet) {
            isSidebarExpanded = false
        }
        onRecordChannelWatched(channel)
    }
    LaunchedEffect(requestedChannel) {
        requestedChannel?.let {
            val alreadySelected = selectedChannel == it
            selectChannel(it)
            if (requestedChannelFullscreen && !alreadySelected) fullscreenTrigger++
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
        movableContentOf { currentChannel: PlaylistChannel, selectedAtMs: Long, playerModifier: Modifier, isCompactPreview: Boolean ->
            PlayerPanel(
                channel = currentChannel,
                channelSelectedAtMs = selectedAtMs,
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
                        movablePlayerPanel(curChannel, channelSelectedAtMs, Modifier.fillMaxSize(), true)
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
                        movablePlayerPanel(selectedChannel!!, channelSelectedAtMs, Modifier.fillMaxWidth().wrapContentHeight(), false)
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
                        movablePlayerPanel(selectedChannel!!, channelSelectedAtMs, Modifier.weight(1f).fillMaxHeight(), false)
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
internal fun SidebarSectionLabel(label: String) {
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

internal data class ChannelEpgPresentation(
    val title: String?,
    val timeRange: String?,
    val progress: Float?
)

@Composable
internal fun ChannelRow(
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


