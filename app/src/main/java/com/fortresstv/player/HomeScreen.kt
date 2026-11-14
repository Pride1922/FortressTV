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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.ThumbUp
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
import com.fortresstv.player.trakt.FeaturedMoviesShelf
import com.fortresstv.player.trakt.FeaturedMoviesShelfResolver
import com.fortresstv.player.trakt.FeaturedSeriesMode
import com.fortresstv.player.trakt.FeaturedSeriesShelf
import com.fortresstv.player.trakt.FeaturedSeriesShelfResolver
import com.fortresstv.player.trakt.TraktPopularMoviesService
import com.fortresstv.player.trakt.TraktPopularMoviesStore
import com.fortresstv.player.trakt.TraktPopularShowsService
import com.fortresstv.player.trakt.TraktPopularShowsStore
import com.fortresstv.player.trakt.TraktSettingsStore
import com.fortresstv.player.trakt.TraktWatchlistStore
import com.fortresstv.player.trakt.TraktWatchlistService
import com.fortresstv.player.trakt.TraktMatchedWatchlist
import com.fortresstv.player.trakt.TraktRecommendationItem
import com.fortresstv.player.trakt.TraktRecommendationsService
import com.fortresstv.player.trakt.TraktWatchlistItem
import com.fortresstv.player.trakt.TraktWatchlistKind
import com.fortresstv.player.xtream.MoviesFirstSeenStore
import com.fortresstv.player.xtream.SeriesFirstSeenStore
import com.fortresstv.player.library.UserLibraryStore.Companion.libraryId
import androidx.compose.material.icons.filled.Bookmark
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
internal fun LibraryPlaceholder(title: String, message: String) {
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

internal data class HomeSearchResults(
    val channels: List<PlaylistChannel> = emptyList(),
    val movies: List<CatalogMovie> = emptyList(),
    val series: List<CatalogSeries> = emptyList()
)

@Composable
private fun HomeShortcut(destination: LibraryDestination, title: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, compact: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        color = colors.surfaceContainer,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = modifier.testTag("library_destination_${destination.name.lowercase()}").tvFocusHighlight(shape = shape, scaleOnFocus = 1.02f)
    ) {
        if (compact) {
            Column(
                Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 2.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
            ) {
                Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                Text(title, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        } else Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun HomeScreen(
    traktNamespace: String,
    channels: List<PlaylistChannel>,
    catalogs: List<XtreamCatalog>,
    profiles: List<PlaylistProfile>,
    homeFocusRequester: FocusRequester,
    onHeaderFocusChanged: (Boolean) -> Unit,
    onSwitchProfile: (() -> Unit)?,
    profileSwitcher: com.fortresstv.player.profile.ProfileSwitcher? = null,
    onOpenSettings: () -> Unit,
    onCloseApp: () -> Unit,
    openSearchOnEntry: Boolean,
    onSearchOpened: () -> Unit,
    sidebarFocusRequester: FocusRequester? = null,
    epgGuide: EpgGuide?,
    epgTimeShiftHours: Int = 0,
    epgScheduleCorrectionMinutes: Int = epgTimeShiftHours * 60,
    epgMappings: Map<String, EpgChannelMapping> = emptyMap(),
    epgRepository: com.fortresstv.player.epg.EpgRepository? = null,
    favoriteChannels: List<PlaylistChannel>,
    recentChannelsList: List<PlaylistChannel>,
    followedSeries: List<CatalogSeries>,
    followedSeriesIds: Set<String>,
    watchedItemIds: Set<String>,
    movieWatchlistIds: Set<String>,
    progressEntries: List<VodProgressEntry>,
    hideAdultContent: Boolean,
    maximumAge: Int? = null,
    blockUnrated: Boolean = false,
    onContinueWatching: (VodProgressEntry) -> Unit,
    onRemoveContinueWatching: (VodProgressEntry) -> Unit,
    onToggleMovieWatchlist: (String, XtreamMovie) -> Unit,
    onToggleMovieWatched: (String, XtreamMovie) -> Unit,
    onToggleSeriesFollow: (String, XtreamSeries) -> Unit,
    onToggleEpisodeWatched: (String, XtreamEpisode, XtreamSeries?) -> Unit,
    onPlayMovie: (XtreamCatalog, XtreamMovie) -> Unit,
    onPlayEpisode: (XtreamCatalog, XtreamSeries, XtreamEpisode, String) -> Unit,
    onWatchChannel: (PlaylistChannel) -> Unit,
    onNavigateToDestination: (LibraryDestination) -> Unit,
    homeHiddenSections: Set<String> = emptySet(),
    homeSectionOrder: List<String> = com.fortresstv.player.ui.HomeSectionId.defaultOrder,
    onOpenCustomizeHome: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val isTv = LocalFormFactor.current.isTelevision
    val compactSearch = !isTv && LocalConfiguration.current.screenWidthDp < 600
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    var isTvSearchActive by remember { mutableStateOf(false) }
    var compactSearchExpanded by remember { mutableStateOf(false) }
    val searchBarFocusRequester = remember { FocusRequester() }
    val searchTextFieldFocusRequester = remember { FocusRequester() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedMovie by remember { mutableStateOf<CatalogMovie?>(null) }
    var selectedSeries by remember { mutableStateOf<CatalogSeries?>(null) }

    var scheduleNow by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(epgGuide) {
        while (true) {
            scheduleNow = Instant.now()
            delay(60_000)
        }
    }

    val sportsRepo = remember(context) { com.fortresstv.player.sports.SportsRepository.get(context) }
    val sportsFixtures by sportsRepo.fixtures.collectAsState()
    LaunchedEffect(sportsRepo) {
        while (true) {
            sportsRepo.refresh()
            delay(30_000L)
        }
    }
    var now by remember { mutableStateOf(java.time.Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = java.time.Instant.now()
            delay(30_000L)
        }
    }
    val liveMatches by androidx.compose.runtime.produceState(
        initialValue = emptyList<com.fortresstv.player.sports.SportsEpgMatch>(),
        sportsFixtures, epgGuide, channels, epgMappings, epgRepository, epgScheduleCorrectionMinutes, now
    ) {
        val guide = epgGuide
        value = if (guide == null) emptyList() else withContext(Dispatchers.Default) {
            val until = now.plusSeconds(86_400)
            val manifest = guide.manifest
            val guides = if (manifest != null && epgRepository != null && epgRepository.manifest.value == manifest) {
                // Includes the previous day, so a match that started before midnight stays in the row.
                com.fortresstv.player.sports.epgDaysForMatches(
                    now, until, epgScheduleCorrectionMinutes, java.time.ZoneId.of(manifest.timezoneId)
                ).mapNotNull { day ->
                    if (day == guide.epochDay) guide else epgRepository.loadDay(day)?.let {
                        guide.copy(programmes = it.programmes, epochDay = day)
                    }
                }.ifEmpty { listOf(guide) }
            } else listOf(guide)
            // Guide events first (parsed once per loaded day), ESPN fixtures as enrichment.
            com.fortresstv.player.sports.matchesOnTv(
                sportsFixtures, guides, channels, epgMappings, now, epgScheduleCorrectionMinutes, until
            )
        }
    }
    val progressByKey = remember(progressEntries) { progressEntries.associateBy { it.key } }

    val currentProfile by remember(context) {
        ProfileRepository.get(context.applicationContext).activeProfile
    }.collectAsState()
    val currentProfileId = currentProfile.id
    val traktStore = remember(traktNamespace) { TraktSettingsStore(context.applicationContext, traktNamespace) }
    val traktWatchlistStore = remember(traktNamespace) { TraktWatchlistStore(context.applicationContext, traktNamespace) }
    val traktRecStore = remember(traktNamespace) { com.fortresstv.player.trakt.TraktRecommendationsStore(context.applicationContext, traktNamespace) }
    val traktPopularStore = remember(traktNamespace) { TraktPopularShowsStore(context.applicationContext, traktNamespace) }
    val traktPopularMoviesStore = remember(traktNamespace) { TraktPopularMoviesStore(context.applicationContext, traktNamespace) }
    val omdbSettings = com.fortresstv.player.omdb.LocalOmdbSettings.current
    var ratingCacheVersion by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    val traktConnected = traktStore.isConnected()
    val showFeaturedSeries = com.fortresstv.player.ui.HomeSectionId.FEATURED_SERIES !in homeHiddenSections
    val showFeaturedMovies = com.fortresstv.player.ui.HomeSectionId.RECENT_MOVIES !in homeHiddenSections
    var traktWatchlistItems by remember(traktNamespace) { mutableStateOf(emptyList<TraktWatchlistItem>()) }
    var traktRecItems by remember(traktNamespace) { mutableStateOf(emptyList<TraktRecommendationItem>()) }
    var traktPopularItems by remember(traktNamespace) { mutableStateOf(emptyList<TraktRecommendationItem>()) }
    var traktPopularMovieItems by remember(traktNamespace) { mutableStateOf(emptyList<TraktRecommendationItem>()) }
    val recommendationLock = remember(traktNamespace) { Mutex() }
    var showHiddenRecommendations by remember(traktNamespace) { mutableStateOf(false) }
    var recommendationToHide by remember(traktNamespace) { mutableStateOf<TraktRecommendationItem?>(null) }
    var hidingRecommendation by remember(traktNamespace) { mutableStateOf(false) }
    var hideFailed by remember(traktNamespace) { mutableStateOf(false) }

    fun confirmHideRecommendation(item: TraktRecommendationItem) {
        hideFailed = false
        recommendationToHide = item
    }

    LaunchedEffect(traktNamespace, traktConnected) {
        if (traktConnected) {
            val (watchlist, recommendations) = withContext(Dispatchers.IO) {
                traktWatchlistStore.loadWatchlist() to traktRecStore.loadRecommendations()
            }
            traktWatchlistItems = watchlist
            traktRecItems = recommendations
            launch {
                TraktWatchlistService.syncWatchlist(context, traktNamespace)
                    .onSuccess { traktWatchlistItems = it }
            }
            recommendationLock.withLock {
                TraktRecommendationsService.syncRecommendations(context, traktNamespace)
                    .onSuccess { traktRecItems = it }
            }
        }
    }

    LaunchedEffect(traktNamespace, traktConnected, showFeaturedSeries) {
        if (traktConnected && showFeaturedSeries) {
            val cachedPopular = withContext(Dispatchers.IO) { traktPopularStore.loadPopularShows() }
            traktPopularItems = cachedPopular
            TraktPopularShowsService.syncPopularShows(context, traktNamespace)
                .onSuccess { traktPopularItems = it }
        } else {
            traktPopularItems = emptyList()
        }
    }

    LaunchedEffect(traktNamespace, traktConnected, showFeaturedMovies) {
        if (traktConnected && showFeaturedMovies) {
            val cachedPopularMovies = withContext(Dispatchers.IO) { traktPopularMoviesStore.loadPopularMovies() }
            traktPopularMovieItems = cachedPopularMovies
            TraktPopularMoviesService.syncPopularMovies(context, traktNamespace)
                .onSuccess { traktPopularMovieItems = it }
        } else {
            traktPopularMovieItems = emptyList()
        }
    }

    val showTraktWatchlist = traktConnected && com.fortresstv.player.ui.HomeSectionId.TRAKT_WATCHLIST !in homeHiddenSections
    val showTraktRecs = traktConnected && com.fortresstv.player.ui.HomeSectionId.TRAKT_RECOMMENDATIONS !in homeHiddenSections
    val canManageHiddenRecommendations = showTraktRecs && !currentProfile.isChild && currentProfileId != "restricted_default"
    val matchedWatchlistState = remember(traktNamespace, showTraktWatchlist, catalogs) {
        mutableStateOf(TraktMatchedWatchlist())
    }
    val matchedTraktWatchlist by matchedWatchlistState
    LaunchedEffect(matchedWatchlistState, traktWatchlistItems) {
        matchedWatchlistState.value = if (showTraktWatchlist && traktWatchlistItems.isNotEmpty()) {
            withContext(Dispatchers.Default) { TraktWatchlistService.matchWatchlist(traktWatchlistItems, catalogs) }
        } else TraktMatchedWatchlist()
    }

    val matchedRecsState = remember(traktNamespace, showTraktRecs, catalogs, hideAdultContent, maximumAge, blockUnrated) {
        mutableStateOf(com.fortresstv.player.trakt.TraktMatchedRecommendations())
    }
    val matchedTraktRecs by matchedRecsState
    LaunchedEffect(matchedRecsState, traktRecItems, catalogs) {
        matchedRecsState.value = if (showTraktRecs && traktRecItems.isNotEmpty()) withContext(Dispatchers.Default) {
            com.fortresstv.player.trakt.TraktRecommendationsService.matchRecommendations(
                recommendations = traktRecItems,
                catalogs = catalogs,
                hideAdultContent = hideAdultContent,
                maximumAge = maximumAge,
                blockUnrated = blockUnrated
            )
        } else com.fortresstv.player.trakt.TraktMatchedRecommendations()
    }

    val featuredSeriesShelfState = remember(traktNamespace, showFeaturedSeries, traktConnected, catalogs, hideAdultContent, maximumAge, blockUnrated) {
        mutableStateOf(FeaturedSeriesShelf())
    }
    val featuredSeriesShelf by featuredSeriesShelfState
    LaunchedEffect(featuredSeriesShelfState, traktPopularItems) {
        if (!showFeaturedSeries || catalogs.isEmpty()) {
            featuredSeriesShelfState.value = FeaturedSeriesShelf()
            return@LaunchedEffect
        }
        featuredSeriesShelfState.value = withContext(Dispatchers.Default) {
            val firstSeenStore = SeriesFirstSeenStore(context.applicationContext.filesDir)
            val firstSeenBySource = withContext(Dispatchers.IO) {
                catalogs.associate { it.sourceId to firstSeenStore.loadFirstSeen(it.sourceId) }
            }
            FeaturedSeriesShelfResolver.resolve(
                traktConnected = traktConnected,
                popularShows = traktPopularItems,
                catalogs = catalogs,
                firstSeenBySource = firstSeenBySource,
                hideAdultContent = hideAdultContent,
                maximumAge = maximumAge,
                blockUnrated = blockUnrated
            )
        }
    }

    val featuredMoviesShelfState = remember(traktNamespace, showFeaturedMovies, traktConnected, catalogs, hideAdultContent, maximumAge, blockUnrated) {
        mutableStateOf(FeaturedMoviesShelf())
    }
    val featuredMoviesShelf by featuredMoviesShelfState
    LaunchedEffect(featuredMoviesShelfState, traktPopularMovieItems) {
        if (!showFeaturedMovies || catalogs.isEmpty()) {
            featuredMoviesShelfState.value = FeaturedMoviesShelf()
            return@LaunchedEffect
        }
        featuredMoviesShelfState.value = withContext(Dispatchers.Default) {
            val firstSeenStore = MoviesFirstSeenStore(context.applicationContext.filesDir)
            val firstSeenBySource = withContext(Dispatchers.IO) {
                catalogs.associate { it.sourceId to firstSeenStore.loadFirstSeen(it.sourceId) }
            }
            FeaturedMoviesShelfResolver.resolve(
                traktConnected = traktConnected,
                popularMovies = traktPopularMovieItems,
                catalogs = catalogs,
                firstSeenBySource = firstSeenBySource,
                hideAdultContent = hideAdultContent,
                maximumAge = maximumAge,
                blockUnrated = blockUnrated
            )
        }
    }

    LaunchedEffect(featuredMoviesShelf.items, featuredSeriesShelf.items, omdbSettings) {
        ratingCacheVersion++
        if (!omdbSettings.enabled || omdbSettings.apiKey.isBlank()) return@LaunchedEffect
        val targets = buildList {
            featuredMoviesShelf.items.take(15).forEach { entry ->
                add(com.fortresstv.player.omdb.OmdbPreFetchTarget("movie", entry.movie.name, entry.movie.year, entry.movie.imdbId))
            }
            featuredSeriesShelf.items.take(15).forEach { entry ->
                add(com.fortresstv.player.omdb.OmdbPreFetchTarget("series", entry.series.name, entry.series.year, entry.series.imdbId))
            }
        }
        if (targets.isNotEmpty()) {
            OmdbMetadataResolver.prefetchCuratedBatch(context, omdbSettings, targets)
            ratingCacheVersion++
        }
    }

    val visibleShelves = homeSectionOrder.filter { section ->
        section !in homeHiddenSections && when (section) {
            com.fortresstv.player.ui.HomeSectionId.CONTINUE_WATCHING -> progressEntries.any { it.title.isNotBlank() }
            com.fortresstv.player.ui.HomeSectionId.SPORTS -> liveMatches.isNotEmpty()
            com.fortresstv.player.ui.HomeSectionId.FAVORITE_CHANNELS -> favoriteChannels.isNotEmpty()
            com.fortresstv.player.ui.HomeSectionId.RECENT_CHANNELS -> recentChannelsList.isNotEmpty()
            com.fortresstv.player.ui.HomeSectionId.FOLLOWED_SERIES -> followedSeries.isNotEmpty()
            com.fortresstv.player.ui.HomeSectionId.TRAKT_WATCHLIST -> showTraktWatchlist && !matchedTraktWatchlist.isEmpty
            com.fortresstv.player.ui.HomeSectionId.TRAKT_RECOMMENDATIONS -> showTraktRecs && (!matchedTraktRecs.isEmpty || canManageHiddenRecommendations)
            com.fortresstv.player.ui.HomeSectionId.RECENT_MOVIES -> featuredMoviesShelf.items.isNotEmpty()
            com.fortresstv.player.ui.HomeSectionId.FEATURED_SERIES -> featuredSeriesShelf.items.isNotEmpty()
            else -> false
        }
    }
    val homeRows = buildList {
        addAll(visibleShelves)
        add(if (visibleShelves.isEmpty()) 0 else 1, "shortcuts")
        if (visibleShelves.isEmpty()) add("empty")
        if (compactSearch) add("customize")
    }
    val homeListState = rememberLazyListState()
    LaunchedEffect(homeSectionOrder) {
        homeListState.requestScrollToItem(0)
    }

    fun openSearch() {
        if (compactSearch) compactSearchExpanded = true
        if (isTv) isTvSearchActive = true
        coroutineScope.launch {
            delay(50)
            runCatching { searchTextFieldFocusRequester.requestFocus() }
            keyboardController?.show()
        }
    }
    LaunchedEffect(openSearchOnEntry) {
        if (openSearchOnEntry) {
            openSearch()
            onSearchOpened()
        }
    }

    BackHandler(enabled = isTvSearchActive || compactSearchExpanded || searchQuery.isNotBlank()) {
        isTvSearchActive = false
        compactSearchExpanded = false
        searchQuery = ""
        keyboardController?.hide()
        runCatching { searchBarFocusRequester.requestFocus() }
    }

    val isSearching = searchQuery.trim().isNotEmpty()
    val searchTrimmed = searchQuery.trim()

    var searchResults by remember { mutableStateOf(HomeSearchResults()) }
    LaunchedEffect(channels, catalogs, searchTrimmed, hideAdultContent, maximumAge, blockUnrated) {
        if (!isSearching) {
            searchResults = HomeSearchResults()
            return@LaunchedEffect
        }
        searchResults = HomeSearchResults()
        delay(200)
        searchResults = withContext(Dispatchers.Default) {
            val matchingChannels = channels.searchChannels(searchTrimmed).take(25)
            val matchingMovies = catalogs.asSequence().flatMap { catalog ->
                val categoryNames = catalog.movieCategories.associate { it.id to it.name }
                catalog.movies.asSequence().filter {
                    it.name.contains(searchTrimmed, ignoreCase = true) &&
                        (!hideAdultContent || !isAdultContent(it.name, categoryNames[it.categoryId])) &&
                        ParentalControls.isContentAllowedForAge(it.ageRating, maximumAge, blockUnrated)
                }.map { CatalogMovie(catalog, it) }
            }.take(25).toList()
            val matchingSeries = catalogs.asSequence().flatMap { catalog ->
                val categoryNames = catalog.seriesCategories.associate { it.id to it.name }
                catalog.series.asSequence().filter {
                    it.name.contains(searchTrimmed, ignoreCase = true) &&
                        (!hideAdultContent || !isAdultContent(it.name, categoryNames[it.categoryId])) &&
                        ParentalControls.isContentAllowedForAge(it.ageRating, maximumAge, blockUnrated)
                }.map { CatalogSeries(catalog, it) }
            }.take(25).toList()
            HomeSearchResults(matchingChannels, matchingMovies, matchingSeries)
        }
    }
    val searchMatchingChannels = searchResults.channels
    val searchMatchingMovies = searchResults.movies
    val searchMatchingSeries = searchResults.series

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = if (compactSearch) 12.dp else 20.dp, vertical = if (compactSearch) 6.dp else 14.dp)
        ) {
        val searchField: @Composable () -> Unit = {
            Surface(
                onClick = ::openSearch,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (compactSearch) Modifier else Modifier.focusRequester(searchBarFocusRequester))
                    .tvFocusHighlight(shape = RoundedCornerShape(12.dp), scaleOnFocus = if (isTv) 1.02f else 1.0f)
            ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            if (isTv) {
                                isTvSearchActive = false
                                keyboardController?.hide()
                                runCatching { searchBarFocusRequester.requestFocus() }
                            }
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(searchTextFieldFocusRequester)
                        .focusProperties {
                            canFocus = !isTv || isTvSearchActive
                        }
                        .onPreviewKeyEvent { keyEvent ->
                            if (isTv && isTvSearchActive && keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN) {
                                if (keyEvent.type == KeyEventType.KeyUp) {
                                    isTvSearchActive = false
                                    keyboardController?.hide()
                                    focusManager.moveFocus(FocusDirection.Down)
                                }
                                true
                            } else false
                        },
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                strings.homeUnifiedSearchPlaceholder,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            searchQuery = ""
                            if (isTv) {
                                isTvSearchActive = false
                                keyboardController?.hide()
                                runCatching { searchBarFocusRequester.requestFocus() }
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            }
        }
        if (compactSearch && compactSearchExpanded) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    compactSearchExpanded = false
                    searchQuery = ""
                    keyboardController?.hide()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                }
                Box(Modifier.weight(1f)) { searchField() }
            }
        } else LibraryHomeHeader(
            onHome = {
                searchQuery = ""
                compactSearchExpanded = false
                isTvSearchActive = false
                keyboardController?.hide()
                homeListState.requestScrollToItem(0)
            },
            onSwitchProfile = onSwitchProfile,
            profileSwitcher = profileSwitcher,
            onSettings = onOpenSettings,
            onCloseApp = onCloseApp,
            profile = currentProfile,
            modifier = Modifier.onFocusChanged { onHeaderFocusChanged(it.hasFocus) },
            homeModifier = Modifier.focusRequester(homeFocusRequester)
        ) {
            if (compactSearch) LibrarySearchButton(::openSearch, Modifier.focusRequester(searchBarFocusRequester))
            else searchField()
        }

        Spacer(Modifier.height(16.dp))

        if (isSearching) {
            val totalResults = searchMatchingChannels.size + searchMatchingMovies.size + searchMatchingSeries.size
            if (totalResults == 0) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            strings.homeNothingFound,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    if (searchMatchingChannels.isNotEmpty()) {
                        item {
                            Text(
                                "${strings.homeChannelsCategory} (${searchMatchingChannels.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(searchMatchingChannels, key = { "search:channel:${it.sourceId}:${it.sourceItemId}" }) { channel ->
                                    Surface(
                                        onClick = { onWatchChannel(channel) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                        modifier = Modifier
                                            .width(200.dp)
                                            .height(64.dp)
                                            .tvFocusHighlight(shape = RoundedCornerShape(12.dp), scaleOnFocus = 1.04f)
                                    ) {
                                        Row(
                                            Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            ChannelLogo(channel, epgGuide?.findChannel(channel.tvgId, channel.name), modifier = Modifier.size(36.dp))
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                channel.name,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (searchMatchingMovies.isNotEmpty()) {
                        item {
                            Text(
                                "${strings.homeMoviesCategory} (${searchMatchingMovies.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(searchMatchingMovies, key = { "search:movie:${it.catalog.sourceId}:${it.movie.id}" }) { entry ->
                                    val libraryId = entry.movie.libraryId(entry.catalog.sourceId)
                                    val cachedOmdb = remember(entry.movie, omdbSettings, ratingCacheVersion) {
                                        if (omdbSettings.enabled && omdbSettings.apiKey.isNotBlank()) {
                                            OmdbMetadataResolver.getCached(context, "movie", entry.movie.name, entry.movie.year, entry.movie.imdbId)
                                        } else null
                                    }
                                    val movieRating = cachedOmdb?.imdbRating ?: entry.movie.rating
                                    VodPosterCard(
                                        title = entry.movie.name,
                                        posterUrl = entry.movie.posterUrl,
                                        subtitle = formatVodMetadata(entry.movie.year, movieRating),
                                        imdbRating = movieRating?.takeIf { it.isNotBlank() && it != "0" && it != "0.0" },
                                        rottenTomatoes = cachedOmdb?.rottenTomatoes,
                                        isWatched = libraryId in watchedItemIds,
                                        progressFraction = progressByKey[libraryId]?.progressFraction,
                                        onClick = { selectedMovie = entry },
                                        modifier = Modifier.width(120.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (searchMatchingSeries.isNotEmpty()) {
                        item {
                            Text(
                                "${strings.homeSeriesCategory} (${searchMatchingSeries.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(searchMatchingSeries, key = { "search:series:${it.catalog.sourceId}:${it.series.id}" }) { entry ->
                                    val cachedOmdb = remember(entry.series, omdbSettings, ratingCacheVersion) {
                                        if (omdbSettings.enabled && omdbSettings.apiKey.isNotBlank()) {
                                            OmdbMetadataResolver.getCached(context, "series", entry.series.name, entry.series.year, entry.series.imdbId)
                                        } else null
                                    }
                                    val seriesRating = cachedOmdb?.imdbRating ?: entry.series.rating
                                    VodPosterCard(
                                        title = entry.series.name,
                                        posterUrl = entry.series.coverUrl,
                                        subtitle = formatVodMetadata(entry.series.year, seriesRating),
                                        imdbRating = seriesRating?.takeIf { it.isNotBlank() && it != "0" && it != "0.0" },
                                        rottenTomatoes = cachedOmdb?.rottenTomatoes,
                                        onClick = { selectedSeries = entry },
                                        modifier = Modifier.width(120.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("home_shelves"),
                state = homeListState,
                verticalArrangement = Arrangement.spacedBy(22.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(homeRows, key = { it }) { section ->
                when (section) {
                    com.fortresstv.player.ui.HomeSectionId.CONTINUE_WATCHING -> {
                        HomeContinueWatchingShelf(
                            entries = progressEntries,
                            onClick = onContinueWatching,
                            onRemove = onRemoveContinueWatching,
                            sidebarFocusRequester = sidebarFocusRequester
                        )
                    }

                    "shortcuts" -> {
                        val shortcuts = listOf(
                            Triple(LibraryDestination.LIVE_TV, strings.navLiveTv, Icons.Default.LiveTv),
                            Triple(LibraryDestination.MOVIES, strings.navMovies, Icons.Default.Movie),
                            Triple(LibraryDestination.SERIES, strings.navSeries, Icons.Default.VideoLibrary),
                            Triple(LibraryDestination.FAVORITES, strings.navFavorites, Icons.Default.Star)
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(if (compactSearch) 4.dp else 8.dp)) {
                                    shortcuts.forEach { (destination, label, icon) ->
                                        HomeShortcut(destination, label, icon, { onNavigateToDestination(destination) }, Modifier.weight(1f), compactSearch)
                                    }
                                }
                            }
                            if (!compactSearch) IconButton(onClick = onOpenCustomizeHome) {
                                Icon(Icons.Default.Tune, contentDescription = strings.customizeHome)
                            }
                        }
                    }
                    "customize" -> TextButton(onClick = onOpenCustomizeHome) { Text(strings.customizeHome) }

                    "empty" -> {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                                modifier = Modifier.widthIn(max = 480.dp)
                            ) {
                                Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    ShieldMark(Modifier.size(64.dp))
                                    Spacer(Modifier.height(14.dp))
                                    Text(strings.appName, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        strings.homeNoContent,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    com.fortresstv.player.ui.HomeSectionId.FAVORITE_CHANNELS -> {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    strings.homeFavoriteChannelsNow,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "(${favoriteChannels.size})",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            val favRowState = rememberLazyListState()
                            LazyRow(state = favRowState, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                itemsIndexed(favoriteChannels, key = { index, it -> "fav_now:${it.sourceId}:${it.sourceItemId}_$index" }) { index, channel ->
                                    val manualXmltvId = epgMappings["${channel.sourceId}:${channel.sourceItemId}"]?.xmltvChannelId
                                    val snapshot = remember(channel.tvgId, channel.name, epgGuide, scheduleNow, epgScheduleCorrectionMinutes, manualXmltvId) {
                                        epgGuide?.snapshot(channel.tvgId, channel.name, scheduleNow, correctionMinutes = epgScheduleCorrectionMinutes, manualXmltvId = manualXmltvId)
                                    }
                                    val currentProg = snapshot?.current
                                    val nextProg = snapshot?.next
                                    val shiftMillis = epgScheduleCorrectionMinutes * 60_000L
                                    val adjustedStart = currentProg?.startEpochMilli?.plus(shiftMillis)
                                    val adjustedStop = currentProg?.stopEpochMilli?.plus(shiftMillis)
                                    val progProgress = if (adjustedStart != null && adjustedStop != null && adjustedStop > adjustedStart) {
                                        ((scheduleNow.toEpochMilli() - adjustedStart).toFloat() /
                                            (adjustedStop - adjustedStart).toFloat()).coerceIn(0f, 1f)
                                    } else null

                                    Surface(
                                        onClick = { onWatchChannel(channel) },
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .width(240.dp)
                                            .height(116.dp)
                                            .focusProperties {
                                                if (index == 0 && sidebarFocusRequester != null) {
                                                    left = sidebarFocusRequester
                                                }
                                            }
                                            .onPreviewKeyEvent { keyEvent ->
                                                if (isTv && index == 0 && keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT && sidebarFocusRequester != null) {
                                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                                        val handled = runCatching { sidebarFocusRequester.requestFocus() }.isSuccess
                                                        if (handled) return@onPreviewKeyEvent true
                                                    }
                                                }
                                                false
                                            }
                                            .tvFocusHighlight(shape = RoundedCornerShape(14.dp), scaleOnFocus = if (isTv) 1.04f else 1.0f)
                                    ) {
                                        Column(Modifier.fillMaxSize().padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                ChannelLogo(channel, epgGuide?.findChannel(channel.tvgId, channel.name), modifier = Modifier.size(36.dp))
                                                Spacer(Modifier.width(10.dp))
                                                Text(
                                                    channel.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(Modifier.weight(1f))
                                            Text(
                                                currentProg?.title ?: "",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                                            )
                                            if (progProgress != null) {
                                                Spacer(Modifier.height(4.dp))
                                                LinearProgressIndicator(
                                                    progress = { progProgress },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(3.dp)
                                                        .clip(RoundedCornerShape(1.5.dp)),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                                )
                                            }
                                            if (nextProg != null) {
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    nextProg.title,
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

                    com.fortresstv.player.ui.HomeSectionId.RECENT_CHANNELS -> {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Replay, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    strings.homeRecentChannels,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            val recentRowState = rememberLazyListState()
                            val programmeTimeFormatter = remember(strings.languageTag) {
                                DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT)
                                    .withLocale(java.util.Locale.forLanguageTag(strings.languageTag))
                                    .withZone(java.time.ZoneId.systemDefault())
                            }
                            LazyRow(state = recentRowState, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                itemsIndexed(recentChannelsList, key = { index, it -> "recent:${it.sourceId}:${it.sourceItemId}_$index" }) { index, channel ->
                                    val manualXmltvId = epgMappings["${channel.sourceId}:${channel.sourceItemId}"]?.xmltvChannelId
                                    val currentProgramme = remember(
                                        channel.tvgId, channel.name, epgGuide, scheduleNow,
                                        epgScheduleCorrectionMinutes, manualXmltvId
                                    ) {
                                        epgGuide?.snapshot(
                                            channel.tvgId, channel.name, scheduleNow,
                                            correctionMinutes = epgScheduleCorrectionMinutes,
                                            manualXmltvId = manualXmltvId
                                        )?.current
                                    }
                                    Surface(
                                        onClick = { onWatchChannel(channel) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                        modifier = Modifier
                                            .width(240.dp)
                                            .height(112.dp)
                                            .focusProperties {
                                                if (index == 0 && sidebarFocusRequester != null) {
                                                    left = sidebarFocusRequester
                                                }
                                            }
                                            .onPreviewKeyEvent { keyEvent ->
                                                if (isTv && index == 0 && keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT && sidebarFocusRequester != null) {
                                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                                        val handled = runCatching { sidebarFocusRequester.requestFocus() }.isSuccess
                                                        if (handled) return@onPreviewKeyEvent true
                                                    }
                                                }
                                                false
                                            }
                                            .tvFocusHighlight(shape = RoundedCornerShape(12.dp), scaleOnFocus = if (isTv) 1.04f else 1.0f)
                                    ) {
                                        Column(Modifier.fillMaxSize().padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                ChannelLogo(channel, epgGuide?.findChannel(channel.tvgId, channel.name), modifier = Modifier.size(32.dp))
                                                Spacer(Modifier.width(10.dp))
                                                Text(
                                                    channel.name,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (currentProgramme != null) {
                                                Spacer(Modifier.height(8.dp))
                                                Text(
                                                    currentProgramme.title,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                currentProgramme.stopEpochMilli?.let { stop ->
                                                    val shiftMillis = epgScheduleCorrectionMinutes * 60_000L
                                                    val startTime = programmeTimeFormatter.format(
                                                        Instant.ofEpochMilli(currentProgramme.startEpochMilli + shiftMillis)
                                                    )
                                                    val endTime = programmeTimeFormatter.format(Instant.ofEpochMilli(stop + shiftMillis))
                                                    Text(
                                                        "$startTime – $endTime",
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    com.fortresstv.player.ui.HomeSectionId.FOLLOWED_SERIES -> {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    strings.homeNextUp,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            val followedRowState = rememberLazyListState()
                            LazyRow(state = followedRowState, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                itemsIndexed(followedSeries, key = { index, it -> "home_series:${it.catalog.sourceId}:${it.series.id}_$index" }) { index, entry ->
                                    val cachedOmdb = remember(entry.series.id) {
                                        OmdbMetadataResolver.getCached(context, "series", entry.series.name, entry.series.year, entry.series.imdbId)
                                    }
                                    VodPosterCard(
                                        title = entry.series.name,
                                        posterUrl = entry.series.coverUrl,
                                        subtitle = formatVodMetadata(entry.series.year, entry.series.rating),
                                        imdbRating = cachedOmdb?.imdbRating,
                                        rottenTomatoes = cachedOmdb?.rottenTomatoes,
                                        onClick = { selectedSeries = entry },
                                        modifier = Modifier
                                            .width(120.dp)
                                            .focusProperties {
                                                if (index == 0 && sidebarFocusRequester != null) {
                                                    left = sidebarFocusRequester
                                                }
                                            }
                                            .onPreviewKeyEvent { keyEvent ->
                                                if (isTv && index == 0 && keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT && sidebarFocusRequester != null) {
                                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                                        val handled = runCatching { sidebarFocusRequester.requestFocus() }.isSuccess
                                                        if (handled) return@onPreviewKeyEvent true
                                                    }
                                                }
                                                false
                                            }
                                    )
                                }
                            }
                        }
                    }

                    com.fortresstv.player.ui.HomeSectionId.SPORTS -> {
                        com.fortresstv.player.sports.SportsHubSection(
                            playableFixtures = liveMatches,
                            onWatchChannel = onWatchChannel
                        )
                    }

                    com.fortresstv.player.ui.HomeSectionId.TRAKT_WATCHLIST -> {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = Color(0xFFED1C24),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    strings.traktWatchlist,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "(${matchedTraktWatchlist.totalCount})",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            val traktRowState = rememberLazyListState()
                            LazyRow(state = traktRowState, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                itemsIndexed(matchedTraktWatchlist.movies, key = { index, it -> "trakt_movie:${it.catalog.sourceId}:${it.movie.id}_$index" }) { index, entry ->
                                    val libraryId = entry.movie.libraryId(entry.catalog.sourceId)
                                    val cachedOmdb = remember(entry.movie, omdbSettings, ratingCacheVersion) {
                                        if (omdbSettings.enabled && omdbSettings.apiKey.isNotBlank()) {
                                            OmdbMetadataResolver.getCached(context, "movie", entry.movie.name, entry.movie.year, entry.movie.imdbId)
                                        } else null
                                    }
                                    val movieRating = cachedOmdb?.imdbRating ?: entry.movie.rating
                                    VodPosterCard(
                                        title = entry.movie.name,
                                        posterUrl = entry.movie.posterUrl,
                                        subtitle = formatVodMetadata(entry.movie.year, movieRating),
                                        imdbRating = movieRating?.takeIf { it.isNotBlank() && it != "0" && it != "0.0" },
                                        rottenTomatoes = cachedOmdb?.rottenTomatoes,
                                        isWatched = libraryId in watchedItemIds,
                                        progressFraction = progressByKey[libraryId]?.progressFraction,
                                        onClick = { selectedMovie = entry },
                                        modifier = Modifier.width(120.dp)
                                    )
                                }
                                itemsIndexed(matchedTraktWatchlist.series, key = { index, it -> "trakt_series:${it.catalog.sourceId}:${it.series.id}_$index" }) { index, entry ->
                                    val cachedOmdb = remember(entry.series.id) {
                                        OmdbMetadataResolver.getCached(context, "series", entry.series.name, entry.series.year, entry.series.imdbId)
                                    }
                                    VodPosterCard(
                                        title = entry.series.name,
                                        posterUrl = entry.series.coverUrl,
                                        subtitle = formatVodMetadata(entry.series.year, entry.series.rating),
                                        imdbRating = cachedOmdb?.imdbRating,
                                        rottenTomatoes = cachedOmdb?.rottenTomatoes,
                                        onClick = { selectedSeries = entry },
                                        modifier = Modifier.width(120.dp)
                                    )
                                }
                            }
                        }
                    }

                    com.fortresstv.player.ui.HomeSectionId.TRAKT_RECOMMENDATIONS -> {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ThumbUp,
                                    contentDescription = null,
                                    tint = Color(0xFFED1C24),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    strings.traktRecommendations,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "(${matchedTraktRecs.totalCount})",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (canManageHiddenRecommendations) {
                                TextButton(onClick = { showHiddenRecommendations = true }) {
                                    Text(strings.traktHiddenRecommendations)
                                }
                            }
                            if (matchedTraktRecs.includesTrending) {
                                Text(strings.traktIncludesTrending, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(10.dp))
                            if (matchedTraktRecs.isEmpty) {
                                Text(strings.traktRecommendationsEmpty, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (matchedTraktRecs.movies.isNotEmpty()) {
                                Text(strings.navMovies, style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(matchedTraktRecs.movies, key = { "trakt_rec_movie:${it.catalog.sourceId}:${it.movie.id}" }) { entry ->
                                        val libraryId = entry.movie.libraryId(entry.catalog.sourceId)
                                        val cachedOmdb = remember(entry.movie.id) {
                                            OmdbMetadataResolver.getCached(context, "movie", entry.movie.name, entry.movie.year, entry.movie.imdbId)
                                        }
                                        VodPosterCard(
                                            title = entry.movie.name,
                                            posterUrl = entry.movie.posterUrl,
                                            subtitle = formatVodMetadata(entry.movie.year, entry.movie.rating),
                                            imdbRating = cachedOmdb?.imdbRating ?: entry.movie.rating?.takeIf { it.isNotBlank() && it != "0" && it != "0.0" },
                                            rottenTomatoes = cachedOmdb?.rottenTomatoes,
                                            isWatched = libraryId in watchedItemIds,
                                            progressFraction = progressByKey[libraryId]?.progressFraction,
                                            onClick = { selectedMovie = entry },
                                            modifier = Modifier.width(120.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                            if (matchedTraktRecs.series.isNotEmpty()) {
                                Text(
                                    if (matchedTraktRecs.seriesAreTrending) strings.traktTrendingSeries else strings.navSeries,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(matchedTraktRecs.series, key = { "trakt_rec_series:${it.catalog.sourceId}:${it.series.id}" }) { entry ->
                                        val cachedOmdb = remember(entry.series.id) {
                                            OmdbMetadataResolver.getCached(context, "series", entry.series.name, entry.series.year, entry.series.imdbId)
                                        }
                                        VodPosterCard(
                                            title = entry.series.name,
                                            posterUrl = entry.series.coverUrl,
                                            subtitle = formatVodMetadata(entry.series.year, entry.series.rating),
                                            imdbRating = cachedOmdb?.imdbRating,
                                            rottenTomatoes = cachedOmdb?.rottenTomatoes,
                                            onClick = { selectedSeries = entry },
                                            modifier = Modifier.width(120.dp)
                                        )
                                    }
                                }
                            } else if (matchedTraktRecs.movies.isNotEmpty()) {
                                Text(strings.traktSeriesRecommendationsEmpty, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    com.fortresstv.player.ui.HomeSectionId.RECENT_MOVIES -> {
                        val featuredMoviesTitle = when (featuredMoviesShelf.mode) {
                            FeaturedSeriesMode.TRAKT_POPULAR -> strings.homePopularOnTraktMovies
                            FeaturedSeriesMode.RECENTLY_ADDED -> strings.homeFeaturedMovies
                            FeaturedSeriesMode.HIDDEN -> strings.homeFeaturedMovies
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    featuredMoviesTitle,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "(${featuredMoviesShelf.items.size})",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            val moviesRowState = rememberLazyListState()
                            LazyRow(state = moviesRowState, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                itemsIndexed(featuredMoviesShelf.items, key = { index, it -> "home_movie:${it.catalog.sourceId}:${it.movie.id}_$index" }) { index, entry ->
                                    val libraryId = entry.movie.libraryId(entry.catalog.sourceId)
                                    val client = remember(entry.catalog.sourceId) {
                                        val source = profiles.firstOrNull { it.id == entry.catalog.sourceId }?.source as? ContentSource.Xtream
                                        source?.let { XtreamClient(context.applicationContext, entry.catalog.sourceId, it) }
                                    }
                                    val cachedOmdb = remember(entry.movie, omdbSettings, ratingCacheVersion) {
                                        if (omdbSettings.enabled && omdbSettings.apiKey.isNotBlank()) {
                                            OmdbMetadataResolver.getCached(context, "movie", entry.movie.name, entry.movie.year, entry.movie.imdbId)
                                        } else null
                                    }
                                    val movieRating = cachedOmdb?.imdbRating ?: entry.movie.rating
                                    VodPosterCard(
                                        title = entry.movie.name,
                                        posterUrl = entry.movie.posterUrl,
                                        subtitle = formatVodMetadata(entry.movie.year, movieRating),
                                        imdbRating = movieRating?.takeIf { it.isNotBlank() && it != "0" && it != "0.0" },
                                        rottenTomatoes = cachedOmdb?.rottenTomatoes,
                                        isWatched = libraryId in watchedItemIds,
                                        progressFraction = progressByKey[libraryId]?.progressFraction,
                                        loadPosterFallback = if (entry.movie.posterUrl.isNullOrBlank()) {
                                            client?.let { c -> { c.loadMovieDetails(entry.movie.id).posterUrl } }
                                        } else null,
                                        onClick = { selectedMovie = entry },
                                        modifier = Modifier.width(120.dp)
                                    )
                                }
                            }
                        }
                    }

                    com.fortresstv.player.ui.HomeSectionId.FEATURED_SERIES -> {
                        val featuredSeriesTitle = when (featuredSeriesShelf.mode) {
                            FeaturedSeriesMode.TRAKT_POPULAR -> strings.homePopularOnTraktSeries
                            FeaturedSeriesMode.RECENTLY_ADDED -> strings.homeRecentlyAddedSeries
                            FeaturedSeriesMode.HIDDEN -> strings.homeFeaturedSeries
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    featuredSeriesTitle,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "(${featuredSeriesShelf.items.size})",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            val seriesRowState = rememberLazyListState()
                            LazyRow(state = seriesRowState, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                itemsIndexed(featuredSeriesShelf.items, key = { index, it -> "home_feat_series:${it.catalog.sourceId}:${it.series.id}_$index" }) { index, entry ->
                                    val cachedOmdb = remember(entry.series, omdbSettings, ratingCacheVersion) {
                                        if (omdbSettings.enabled && omdbSettings.apiKey.isNotBlank()) {
                                            OmdbMetadataResolver.getCached(context, "series", entry.series.name, entry.series.year, entry.series.imdbId)
                                        } else null
                                    }
                                    val seriesRating = cachedOmdb?.imdbRating ?: entry.series.rating
                                    VodPosterCard(
                                        title = entry.series.name,
                                        posterUrl = entry.series.coverUrl,
                                        subtitle = formatVodMetadata(entry.series.year, seriesRating),
                                        imdbRating = seriesRating?.takeIf { it.isNotBlank() && it != "0" && it != "0.0" },
                                        rottenTomatoes = cachedOmdb?.rottenTomatoes,
                                        onClick = { selectedSeries = entry },
                                        modifier = Modifier.width(120.dp)
                                    )
                                }
                            }
                        }
                    }

                    else -> Unit
                }
                }
            }
        }
    }

    selectedMovie?.let { entry ->
        val source = profiles.firstOrNull { it.id == entry.catalog.sourceId }?.source as? ContentSource.Xtream
        val matchingRec = remember(entry.movie.id, traktRecItems) {
            val normName = com.fortresstv.player.trakt.TraktRecommendationsService.normalizeTitle(entry.movie.name)
            val cleanName = com.fortresstv.player.trakt.TraktRecommendationsService.normalizeTitle(com.fortresstv.player.tmdb.TmdbClient.cleanMediaTitle(entry.movie.name).first)
            traktRecItems.firstOrNull { it.kind == TraktWatchlistKind.MOVIE && (
                (it.tmdbId != null && it.tmdbId == entry.movie.tmdbId) ||
                (it.imdbId != null && it.imdbId.equals(entry.movie.imdbId, ignoreCase = true)) ||
                (com.fortresstv.player.trakt.TraktRecommendationsService.normalizeTitle(it.title).let { t -> t == normName || t == cleanName })
            ) }
        }
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
            onHideOnTrakt = matchingRec?.takeIf { canManageHiddenRecommendations }?.let { rec -> { confirmHideRecommendation(rec) } },
            onWatch = { movie ->
                selectedMovie = null
                onPlayMovie(entry.catalog, movie)
            }
        )
    }

    selectedSeries?.let { entry ->
        val source = profiles.firstOrNull { it.id == entry.catalog.sourceId }?.source as? ContentSource.Xtream
        if (source != null) {
            val matchingRec = remember(entry.series.id, traktRecItems) {
                val normName = com.fortresstv.player.trakt.TraktRecommendationsService.normalizeTitle(entry.series.name)
                val cleanName = com.fortresstv.player.trakt.TraktRecommendationsService.normalizeTitle(com.fortresstv.player.tmdb.TmdbClient.cleanMediaTitle(entry.series.name).first)
                traktRecItems.firstOrNull { it.kind == TraktWatchlistKind.SHOW && (
                    (it.tmdbId != null && it.tmdbId == entry.series.tmdbId) ||
                    (it.imdbId != null && it.imdbId.equals(entry.series.imdbId, ignoreCase = true)) ||
                    (com.fortresstv.player.trakt.TraktRecommendationsService.normalizeTitle(it.title).let { t -> t == normName || t == cleanName })
                ) }
            }
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
                onToggleEpisodeWatched = { onToggleEpisodeWatched(entry.catalog.sourceId, it, entry.series) },
                traktNamespace = traktNamespace,
                onHideOnTrakt = matchingRec?.takeIf { canManageHiddenRecommendations }?.let { rec -> { confirmHideRecommendation(rec) } },
                onDismiss = { selectedSeries = null },
                onPlay = { series, episode, url ->
                    selectedSeries = null
                    onPlayEpisode(entry.catalog, series, episode, url)
                }
            )
        }
    }

    recommendationToHide?.takeIf { canManageHiddenRecommendations }?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!hidingRecommendation) recommendationToHide = null },
            title = { Text(strings.traktHideRecommendation) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.title, fontWeight = FontWeight.SemiBold)
                    Text(strings.traktHideRecommendationConfirmation)
                    if (hidingRecommendation) CircularProgressIndicator(Modifier.size(24.dp))
                    if (hideFailed) Text(strings.traktRecommendationActionFailed, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !hidingRecommendation,
                    onClick = {
                        hidingRecommendation = true
                        hideFailed = false
                        coroutineScope.launch {
                            try {
                                recommendationLock.withLock {
                                    TraktRecommendationsService.dismissRecommendation(context, traktNamespace, item)
                                        .onSuccess {
                                            traktRecItems = it
                                            recommendationToHide = null
                                            selectedMovie = null
                                            selectedSeries = null
                                        }.onFailure { hideFailed = true }
                                }
                            } finally {
                                hidingRecommendation = false
                            }
                        }
                    }
                ) { Text(strings.traktHideRecommendation) }
            },
            dismissButton = {
                TextButton(enabled = !hidingRecommendation, onClick = { recommendationToHide = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showHiddenRecommendations && canManageHiddenRecommendations) {
        HiddenTraktRecommendationsDialog(
            loadPage = { page ->
                TraktRecommendationsService.fetchHiddenRecommendations(context, traktNamespace, page).map { result ->
                    if (!hideAdultContent) result
                    else result.copy(items = result.items.filterNot { isAdultContent(it.title, null) })
                }
            },
            restore = { item ->
                recommendationLock.withLock {
                    TraktRecommendationsService.restoreRecommendation(context, traktNamespace, item)
                        .onSuccess { traktRecItems = it }
                        .isSuccess
                }
            },
            onDismiss = { showHiddenRecommendations = false }
        )
    }
}
}

