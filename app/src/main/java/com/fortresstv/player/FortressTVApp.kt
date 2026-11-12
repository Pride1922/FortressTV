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
import android.os.Trace
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
import kotlinx.coroutines.flow.collectLatest
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
internal fun FortressTVApp(
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
    var libraryNamespace by remember { mutableStateOf(initialNs) }
    val userLibrary = remember { UserLibraryStore(context.applicationContext, initialNs) }
    val vodProgressStore = remember { VodProgressStore(context.applicationContext, initialNs) }
    val sourceSyncWorkflow = remember(context, settings, userLibrary, vodProgressStore, scope) {
        SourceSyncWorkflow(context.applicationContext, settings, userLibrary, vodProgressStore, scope)
    }
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
    var startupDestination by remember { mutableStateOf(settings.startupDestination(initialProfileId)) }
    var startupNavigationRequest by remember { mutableStateOf<StartupDestination?>(null) }
    var homeHiddenSections by remember { mutableStateOf(settings.homeHiddenSections(initialProfileId)) }
    var homeSectionOrder by remember { mutableStateOf(settings.homeSectionOrder(initialProfileId)) }
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
    val loadedPlaylists = remember { mutableStateMapOf<String, Playlist>() }
    val playlistRetryJobs = remember { mutableMapOf<String, Job>() }
    val xtreamCatalogs = remember { mutableStateMapOf<String, XtreamCatalog>() }
    val xtreamLoadJobs = remember { mutableStateMapOf<String, Job>() }
    val playlist by produceState<Playlist?>(null) {
        snapshotFlow {
            val live = loadedPlaylists.toMap()
            val enabled = playlists.filter { it.enabled }
            if (enabled.none { it.id in live || it.id in xtreamCatalogs }) null
            else enabled.map { it.copy(lastUpdated = 0L, channelCount = 0) } to live
        }.collectLatest { inputs ->
            value = inputs?.let { (profiles, live) ->
                withContext(Dispatchers.Default) {
                    Trace.beginSection("FortressTV.mergeLivePlaylists")
                    try {
                        mergeLivePlaylists(profiles, live)
                    } finally {
                        Trace.endSection()
                    }
                }
            }
        }
    }
    var favoriteChannelIds by remember { mutableStateOf(userLibrary.favoriteChannelIds()) }
    var recentChannelIds by remember { mutableStateOf(userLibrary.recentChannelIds()) }
    var movieWatchlistIds by remember { mutableStateOf(userLibrary.movieWatchlistIds()) }
    var followedSeriesIds by remember { mutableStateOf(userLibrary.followedSeriesIds()) }
    var watchedItemIds by remember { mutableStateOf(userLibrary.watchedItemIds()) }
    var progressRefreshTrigger by remember { mutableStateOf(0) }
    var hiddenCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var categoryOrder by remember { mutableStateOf<List<String>>(emptyList()) }

    var epgGuide by remember { mutableStateOf<EpgGuide?>(null) }
    var epgSource by remember { mutableStateOf<ContentSource?>(null) }
    var epgSourceName by remember { mutableStateOf<String?>(null) }
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

    var showSettings by remember { mutableStateOf(false) }
    var playbackResetGeneration by remember { mutableIntStateOf(0) }
    // Quick switching from the profile menu. It lives here, beside the profile picker, not in the
    // menu: the library screen (and its header) is replaced while the new profile loads, which would
    // cancel a switch still running there. A switch then resets playback, like the picker does.
    val headerProfileSwitcher = com.fortresstv.player.profile.rememberProfileSwitcher { playbackResetGeneration++ }
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

    val securityLockWorkflow = remember(profileRepository) { SecurityLockWorkflow(profileRepository) }
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
    val epgSyncWorkflow = remember(context, settings, epgRepository, epgMappingStore, accountRepository, cloudSourceSyncService, scope) {
        EpgSyncWorkflow(context.applicationContext, settings, epgRepository, epgMappingStore, accountRepository, cloudSourceSyncService, scope)
    }

    fun requestAdultAction(action: (String?) -> Unit) {
        securityLockWorkflow.requestAdultAction(activeHouseholdProfile, action) {
            pendingAdultTokenAction = action
        }
    }

    fun handleSaveEpgMapping(channel: PlaylistChannel, xmltvChannel: com.fortresstv.player.epg.EpgChannel, unlockToken: String?) {
        epgMappings = epgSyncWorkflow.saveMapping(channel, xmltvChannel, epgSource?.displayName, unlockToken)
    }

    fun handleResetEpgMapping(channel: PlaylistChannel, unlockToken: String?) {
        epgMappings = epgSyncWorkflow.resetMapping(channel, unlockToken)
    }

    var showLocalImportDialog by remember { mutableStateOf(false) }

    val pinStatus = remember(currentUserId, householdProfiles, requireProfileSelection) { profileRepository.getPinStatus() }
    val shouldGateWithProfilePicker = requireProfileSelection

    LaunchedEffect(inactivityLockTriggered) {
        securityLockWorkflow.handleInactivityLock(inactivityLockTriggered) {
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

    val reconciliationLock = remember { Mutex() }

    suspend fun reconnectFavouritesAndProgress(sourceId: String, catalog: XtreamCatalog) {
        val namespace = libraryNamespace
        val generation = playbackResetGeneration
        reconciliationLock.withLock {
            val stillCurrent = {
                libraryNamespace == namespace && playbackResetGeneration == generation &&
                    xtreamCatalogs[sourceId] === catalog
            }
            if (!stillCurrent()) return@withLock
            sourceSyncWorkflow.reconnectFavouritesAndProgress(
                sourceId, catalog, playlists, stillCurrent, inferNextEpisodes = currentUserId == null
            ) {
                favoriteChannelIds = userLibrary.favoriteChannelIds()
                recentChannelIds = userLibrary.recentChannelIds()
                movieWatchlistIds = userLibrary.movieWatchlistIds()
                followedSeriesIds = userLibrary.followedSeriesIds()
                watchedItemIds = userLibrary.watchedItemIds()
                progressRefreshTrigger++
            }
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
        homeSectionOrder = settings.homeSectionOrder(targetProfileId)
        epgScheduleCorrectionMinutes = settings.epgScheduleCorrectionMinutes(targetProfileId)
        libraryNamespace = ns

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
                        completed = entry.progressFraction >= 0.95f,
                        seriesId = entry.seriesId,
                        season = entry.season,
                        episode = entry.episode
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
            // Upload watchlist/followed items the cloud doesn't know yet. An item whose hashed key is
            // already in the set came from (or went through) the cloud, so it is skipped instead of being
            // re-sent on every start. The server overwrites all flags, so send the real watched state:
            // a hard-coded "not watched" here used to reset watched watchlist movies on each launch.
            val currentWatched = userLibrary.watchedItemIds()
            suspend fun uploadIfLocalOnly(items: Set<String>, itemKind: String) {
                for (item in items) {
                    val parts = item.split(":")
                    if (parts.size < 3 || parts[2].length >= 64) continue
                    val hashedKey = "${parts[0]}:${parts[1]}:${com.fortresstv.player.library.CloudIdentityHasher.hashItemId(parts[2])}"
                    if (hashedKey in items) continue
                    cloudLibrarySyncService.enqueueLibraryMutation(
                        profileId = targetProfileId,
                        sourceId = parts[1],
                        itemKind = itemKind,
                        itemId = parts[2],
                        isFavourite = true,
                        isWatchlisted = true,
                        isWatched = userLibrary.isItemWatched(item, currentWatched)
                    )
                }
            }
            uploadIfLocalOnly(userLibrary.followedSeriesIds(), "series")
            uploadIfLocalOnly(userLibrary.movieWatchlistIds(), "movie")
            cloudLibrarySyncService.triggerSync()
            val profileLibrary = UserLibraryStore(context.applicationContext, ns)
            val profileProgress = VodProgressStore(context.applicationContext, ns)
            cloudLibrarySyncService.pullAndApplyProfileData(
                profileId = targetProfileId,
                userLibrary = profileLibrary,
                progressStore = profileProgress,
                onUpdated = {
                    if (activeHouseholdProfile.id == targetProfileId) {
                        scope.launch(Dispatchers.Default) {
                            xtreamCatalogs.toMap().forEach { (sourceId, catalog) ->
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
                        homeHiddenSections = settings.homeHiddenSections(targetProfileId)
                        homeSectionOrder = settings.homeSectionOrder(targetProfileId)
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
                    profileRepository.setProfileLanguage(current.id, profileLang)
                }
            }
        }
    }

    val onPreferredAudioLanguageChanged: (String) -> Unit = { lang ->
        preferredAudioLanguage = lang
        settings.setPreferredAudioLanguage(lang, activeHouseholdProfile.id)
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
        settings.setPreferredSubtitleLanguage(lang, activeHouseholdProfile.id)
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

    suspend fun parsePlaylist(stream: java.io.InputStream): Playlist = sourceSyncWorkflow.parsePlaylist(stream)

    suspend fun parsePlaylistWithCache(
        profile: PlaylistProfile,
        cacheType: String,
        stream: java.io.InputStream,
        allowCachedResult: Boolean
    ): Playlist = sourceSyncWorkflow.parsePlaylistWithCache(profile, cacheType, stream, allowCachedResult)

    suspend fun parseEpg(stream: java.io.InputStream, loadedDays: Int = epgLoadedDays): EpgGuide =
        epgSyncWorkflow.parseEpg(stream, loadedDays)

    suspend fun parseEpgWithCache(
        source: ContentSource,
        stream: java.io.InputStream,
        allowCachedResult: Boolean,
        loadedDays: Int = epgLoadedDays
    ): EpgGuide = epgSyncWorkflow.parseEpgWithCache(
        source = source,
        stream = stream,
        allowCachedResult = allowCachedResult,
        loadedDays = loadedDays,
        epgSourceTimezone = epgSourceTimezone,
        onPartialGuideLoaded = { epgGuide = it }
    )

    fun loadPlaylistProfile(
        profile: PlaylistProfile,
        forceRefresh: Boolean = false,
        startupScope: CoroutineScope? = null
    ): Job {
        loading = true
        error = null
        retryLoad = null
        return (startupScope ?: scope).launch(Dispatchers.IO) {
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
                val allPlaylists = if (startupScope == null) {
                    settings.savePlaylistProfile(updatedProfile)
                    settings.getPlaylists()
                } else {
                    settings.updatePlaylistStats(profile, now, parsed.channels.size)
                    null
                }
                withContext(Dispatchers.Main) {
                    if (startupScope != null && playlists.none { it.id == profile.id && it.enabled && it.source == profile.source }) {
                        return@withContext
                    }
                    loadedPlaylists[updatedProfile.id] = parsed
                    playlists = allPlaylists ?: playlists.map {
                        if (it.id == profile.id) it.copy(lastUpdated = now, channelCount = parsed.channels.size) else it
                    }
                    val enabled = playlists.filter { it.enabled }
                    activeProfile = enabled.minByOrNull { it.order }

                    if (startupScope != null) return@withContext
                    val favs = userLibrary.favoriteChannelIds().toMutableSet()
                    if (favs.none { it.startsWith("channel:${updatedProfile.id}:") }) return@withContext
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
                if (err is CancellationException) throw err
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
            }
        }
    }



    fun loadXtreamProfile(profile: PlaylistProfile, forceRefresh: Boolean = false) {
        val source = profile.source as? ContentSource.Xtream ?: return
        if (forceRefresh) xtreamLoadJobs.remove(profile.id)?.cancel()
        else if (profile.id in xtreamLoadJobs) return
        if (playlists.none { it.id == profile.id }) {
            settings.savePlaylistProfile(profile)
            playlists = settings.getPlaylists()
        }

        fun currentProfile() = playlists.firstOrNull { it.id == profile.id && it.enabled && it.source == source }

        xtreamLoadJobs[profile.id] = scope.launch(Dispatchers.IO) {
            // True once a cached catalogue is on screen; a failed refresh then keeps it instead of erroring.
            var showingCachedCatalog = false
            try {
                val client = XtreamClient(context.applicationContext, profile.id, source)

                suspend fun publish(catalog: XtreamCatalog, live: Playlist, updatedAt: Long?): Boolean {
                    val count = catalog.liveChannels.size.takeIf { it > 0 } ?: (catalog.movies.size + catalog.series.size)
                    if (updatedAt != null) settings.updatePlaylistStats(profile, updatedAt, count)
                    val published = withContext(Dispatchers.Main) {
                        val current = currentProfile() ?: return@withContext false
                        val updated = current.copy(lastUpdated = updatedAt ?: current.lastUpdated, channelCount = count)
                        xtreamCatalogs[profile.id] = catalog
                        if (catalog.liveChannels.isEmpty()) loadedPlaylists.remove(profile.id)
                        else loadedPlaylists[profile.id] = live
                        playlists = playlists.map { if (it.id == profile.id) updated else it }
                        if (activeProfile?.id == profile.id || activeProfile == null) activeProfile = updated
                        true
                    }
                    if (published) reconnectFavouritesAndProgress(profile.id, catalog)
                    return published
                }

                // Cache first: show what this device already has without waiting for the provider's
                // login, so a slow or unreachable provider never leaves Movies and Series empty.
                val cached = if (forceRefresh) null else client.loadCachedCatalog()
                if (cached != null) {
                    showingCachedCatalog = publish(cached.catalog, Playlist(cached.catalog.liveChannels), updatedAt = null)
                    // Under 24 h old: nothing to download.
                    if (cached.fresh) return@launch
                }

                var livePlaylist: Playlist? = null
                val catalog = client.loadCatalog(
                    forceRefresh = forceRefresh || cached != null,
                    onMoviesLoaded = { movies ->
                        withContext(Dispatchers.Main) {
                            currentProfile()?.let { current ->
                                val previous = xtreamCatalogs[profile.id] ?: movies
                                xtreamCatalogs[profile.id] = previous.copy(
                                    movieCategories = movies.movieCategories,
                                    movies = movies.movies
                                )
                                if (activeProfile == null && current.role.contributesMovies()) activeProfile = current
                            }
                        }
                    },
                    onSeriesLoaded = { series ->
                        withContext(Dispatchers.Main) {
                            currentProfile()?.let { current ->
                                val previous = xtreamCatalogs[profile.id] ?: series
                                xtreamCatalogs[profile.id] = previous.copy(
                                    seriesCategories = series.seriesCategories,
                                    series = series.series
                                )
                                if (activeProfile == null && current.role.contributesSeries()) activeProfile = current
                            }
                        }
                    }
                ) { live ->
                    val partial = Playlist(live.liveChannels)
                    livePlaylist = partial
                    withContext(Dispatchers.Main) {
                        if (currentProfile()?.role?.contributesLiveTv() == true) {
                            loadedPlaylists[profile.id] = partial
                            if (activeProfile == null) activeProfile = currentProfile()
                        }
                    }
                }
                val live = livePlaylist?.takeIf { it.channels === catalog.liveChannels } ?: Playlist(catalog.liveChannels)
                publish(catalog, live, updatedAt = System.currentTimeMillis())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                withContext(Dispatchers.Main) {
                    // The cached catalogue stays usable; the next start or a manual refresh tries again.
                    if (showingCachedCatalog) return@withContext
                    currentProfile()?.let { current ->
                        error = "${current.name}: ${failure.message ?: "could not load Xtream catalogue"}"
                        retryLoad = { loadXtreamProfile(current, forceRefresh = true) }
                    }
                }
            } finally {
                val job = coroutineContext.job
                withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Main) {
                    if (xtreamLoadJobs[profile.id] === job) xtreamLoadJobs.remove(profile.id)
                }
            }
        }
    }

    val epgLoadMutex = remember { Mutex() }
    var startupEpgRestoring by remember { mutableStateOf(false) }
    var automaticEpgJob by remember { mutableStateOf<Job?>(null) }

    fun loadEpg(
        source: ContentSource,
        save: Boolean,
        forceRefresh: Boolean = false,
        background: Boolean = false,
        refreshIfDue: Boolean = false
    ): Job {
        if (!background) {
            loading = true
            error = null
            retryLoad = null
        }
        return scope.launch(Dispatchers.IO) {
            epgLoadMutex.withLock {
                if (background && withContext(Dispatchers.Main) { epgSource != source }) return@withLock
                val hasCache = SourceReader.hasCache(context, "epg")
                val dailyRefreshDue = refreshIfDue && withContext(Dispatchers.Main) {
                    val zone = ZoneId.systemDefault()
                    val today = LocalDate.now(zone).toEpochDay()
                    val partitions = epgGuide?.manifest?.partitions
                    val coversWindow = partitions != null && (0 until epgLoadedDays).all { today + it in partitions }
                    partitions.isNullOrEmpty() ||
                        isEpgRefreshDue(settings.lastEpgUpdate(), System.currentTimeMillis(), zone, coversWindow)
                }
                val fetchRemote = source is ContentSource.RemoteUrl && (
                    forceRefresh || !hasCache || dailyRefreshDue
                )
                if (refreshIfDue && !fetchRemote) return@withLock

                runCatching {
                    if (source is ContentSource.RemoteUrl && fetchRemote) {
                        epgSyncWorkflow.refreshRemoteGuide(source, epgLoadedDays, epgSourceTimezone)
                    } else {
                        val stream = if (source is ContentSource.LocalFile) SourceReader.open(context, source)
                        else SourceReader.openCached(context, "epg")
                        parseEpgWithCache(source, stream, allowCachedResult = !forceRefresh)
                    }
                }.onSuccess { guide ->
                    withContext(Dispatchers.Main) {
                        if (background && epgSource != source) return@withContext
                        if (fetchRemote) epgRepository.setGuide(guide)
                        if (fetchRemote || (source is ContentSource.LocalFile && (forceRefresh || save))) {
                            val now = System.currentTimeMillis()
                            settings.setLastEpgUpdate(now)
                            epgLastUpdate = now
                        }
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
                    if (err is CancellationException) throw err
                    if (!background) withContext(Dispatchers.Main) {
                        error = if (err is com.fortresstv.player.epg.EmptyEpgDownloadException) strings.noEpgData
                        else err.message ?: strings.noEpgData
                        retryLoad = { loadEpg(source, save = save, forceRefresh = true) }
                    }
                }
            }
            if (!background) withContext(Dispatchers.Main) {
                loading = false
            }
        }
    }

    fun advanceEpgWindow(source: ContentSource, epochDay: Long) {
        scope.launch {
            val manifest = epgRepository.manifest.value
            val cachedTodayGuide = withContext(Dispatchers.IO) {
                val day = epgRepository.loadDay(epochDay)
                if (manifest != null && day != null) {
                    EpgGuide(
                        channels = manifest.channels,
                        programmes = day.programmes,
                        manifest = manifest,
                        epochDay = epochDay
                    ).warmUp()
                } else null
            }
            if (epgSource != source || epgRepository.manifest.value != manifest) return@launch
            if (cachedTodayGuide != null) {
                epgRepository.setGuide(cachedTodayGuide)
                epgGuide = cachedTodayGuide
            }

            if (source is ContentSource.LocalFile && cachedTodayGuide == null) {
                loadEpg(source, save = false, background = true)
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

    LaunchedEffect(foregroundGeneration, currentUserId) {
        val userId = currentUserId ?: return@LaunchedEffect
        launch {
            val local = tmdbSettings
            val remote = cloudSourceSyncService.reconcileTmdb(local)
            if (remote != null && remote.apiKey.isNotBlank() && tmdbSettings == local && currentUserId == userId) {
                tmdbStore.saveSettings(remote)
                tmdbSettings = remote
            }
        }
        launch {
            val local = omdbSettings
            val remote = cloudSourceSyncService.reconcileOmdb(local)
            if (remote != null && remote.apiKey.isNotBlank() && omdbSettings == local && currentUserId == userId) {
                omdbStore.saveSettings(remote)
                omdbSettings = remote
            }
        }
    }

    LaunchedEffect(foregroundGeneration, activeHouseholdProfile.id, currentUserId) {
        val userId = currentUserId ?: return@LaunchedEffect
        val profileId = activeHouseholdProfile.id
        if (profileId == "restricted_default") return@LaunchedEffect
        val store = com.fortresstv.player.trakt.TraktSettingsStore(context.applicationContext, profileProgressNamespace(userId, profileId))
        val localClientId = store.loadClientId()
        val remoteClientId = cloudSourceSyncService.reconcileTraktClientId(profileId, localClientId)
        if (remoteClientId != null && store.loadClientId().isBlank()) store.saveClientId(remoteClientId)
    }

    LaunchedEffect(foregroundGeneration, activeHouseholdProfile.id, currentUserId) {
        val profileId = activeHouseholdProfile.id
        if (currentUserId == null || profileId == "restricted_default") return@LaunchedEffect
        val progress = VodProgressStore(context.applicationContext, profileProgressNamespace(currentUserId, profileId))
        launch {
            cloudLibrarySyncService.refreshProgress(profileId, progress)
            if (activeHouseholdProfile.id == profileId) {
                val unresolved = progress.entries().filter { it.kind == VodProgressKind.EPISODE && it.seriesId == null }
                    .mapTo(mutableSetOf()) { it.key }
                xtreamCatalogs.toMap().forEach { (sourceId, catalog) ->
                    reconnectFavouritesAndProgress(sourceId, catalog)
                }
                if (progress.entries().any { it.key in unresolved && it.seriesId != null }) {
                    cloudLibrarySyncService.refreshProgress(profileId, progress)
                }
                progressRefreshTrigger++
            }
        }
        fun localPreferences() = listOf(
            settings.preferredAudioLanguage(profileId), settings.preferredSubtitleLanguage(profileId),
            settings.subtitleFallbackEnglish(profileId), settings.startupDestination(profileId),
            settings.epgScheduleCorrectionMinutes(profileId)
        )
        val before = localPreferences()
        val remote = cloudLibrarySyncService.pullRemotePreferences(profileId) ?: return@LaunchedEffect
        if (profileId != activeHouseholdProfile.id || before != localPreferences()) return@LaunchedEffect

        settings.setPreferredAudioLanguage(remote.preferredAudioLanguage, profileId)
        settings.setPreferredSubtitleLanguage(remote.preferredSubtitleLanguage, profileId)
        settings.setSubtitleFallbackEnglish(remote.subtitleFallbackEnglish, profileId)
        settings.setStartupDestination(StartupDestination.fromPersisted(remote.startupDestination), profileId)
        if (settings.epgTimeShiftHours(profileId) != remote.epgTimeShiftHours) {
            settings.saveEpgTimeShiftHours(remote.epgTimeShiftHours, profileId)
        }
        preferredAudioLanguage = settings.preferredAudioLanguage(profileId)
        preferredSubtitleLanguage = settings.preferredSubtitleLanguage(profileId)
        subtitleFallbackEnglish = settings.subtitleFallbackEnglish(profileId)
        startupDestination = settings.startupDestination(profileId)
        epgScheduleCorrectionMinutes = settings.epgScheduleCorrectionMinutes(profileId)
    }

    LaunchedEffect(currentAppEpochDay) {
        if (currentAppEpochDay != activeEpgEpochDay) {
            activeEpgEpochDay = currentAppEpochDay
            val source = withContext(Dispatchers.IO) { settings.epgSource() }
            if (source != null) advanceEpgWindow(source, currentAppEpochDay)
        }
    }

    LaunchedEffect(epgSource, foregroundGeneration, currentAppEpochDay, startupEpgRestoring) {
        val source = epgSource as? ContentSource.RemoteUrl ?: return@LaunchedEffect
        if (!startupEpgRestoring && automaticEpgJob?.isActive != true) {
            automaticEpgJob = loadEpg(source, save = false, background = true, refreshIfDue = true)
        }
    }

    var startupSourcesLoading by remember { mutableStateOf(true) }

    LaunchedEffect(shouldGateWithProfilePicker, activeHouseholdProfile.id, currentUserId) {
        if (shouldGateWithProfilePicker) return@LaunchedEffect

        data class StartupSettings(
            val playlists: List<PlaylistProfile>,
            val legacyActiveId: String?,
            val epg: ContentSource?,
            val language: AppLanguage,
            val epgCorrection: Int,
            val epgTimezone: String,
            val epgDays: Int,
            val epgLastUpdate: Long,
            val fullscreen: Boolean,
            val pictureInPicture: Boolean,
            val telemetry: Boolean,
            val playbackMode: PlaybackMode
        )

        startupSourcesLoading = true
        val startupProfileId = activeHouseholdProfile.id
        val saved = withContext(Dispatchers.IO) {
            Trace.beginSection("FortressTV.readStartupSettings")
            try {
                StartupSettings(
                    playlists = settings.getPlaylists(),
                    legacyActiveId = settings.getActivePlaylistId(),
                    epg = settings.epgSource(),
                    language = AppLanguage.fromCode(settings.getLanguage()),
                    epgCorrection = settings.epgScheduleCorrectionMinutes(startupProfileId),
                    epgTimezone = settings.epgSourceTimezone().orEmpty(),
                    epgDays = settings.epgLoadedDays(),
                    epgLastUpdate = settings.lastEpgUpdate(),
                    fullscreen = settings.playChannelsFullscreen(),
                    pictureInPicture = settings.autoPictureInPicture(),
                    telemetry = settings.telemetryEnabled(),
                    playbackMode = settings.playbackMode()
                )
            } finally {
                Trace.endSection()
            }
        }
        val enabledPlaylists = settings.getEnabledPlaylists(saved.playlists)
        playlists = saved.playlists
        activeProfile = enabledPlaylists.firstOrNull()
        appLanguage = saved.language
        epgScheduleCorrectionMinutes = saved.epgCorrection
        epgSourceTimezone = saved.epgTimezone
        epgLoadedDays = saved.epgDays
        epgLastUpdate = saved.epgLastUpdate
        playChannelsFullscreen = saved.fullscreen
        autoPictureInPicture = saved.pictureInPicture
        telemetryEnabled = saved.telemetry
        playbackMode = saved.playbackMode
        val savedEpg = saved.epg
        startupEpgRestoring = savedEpg != null
        epgSource = savedEpg
        epgSourceName = savedEpg?.displayName

        enabledPlaylists.filter { it.source is ContentSource.Xtream }.forEach { loadXtreamProfile(it) }

        launch {
            try {
                coroutineScope {
                    for (profile in enabledPlaylists) {
                        if (profile.source is ContentSource.Xtream) continue
                        val cacheKey = "playlist_${profile.id}"
                        val cached = try {
                            withContext(Dispatchers.IO) {
                                if (profile.source is ContentSource.LocalFile) {
                                    val stream = SourceReader.open(context, profile.source)
                                    return@withContext parsePlaylistWithCache(profile, cacheKey, stream, true) to true
                                }
                                val existingKey = when {
                                    SourceReader.hasCache(context, cacheKey) -> cacheKey
                                    profile.id == saved.legacyActiveId && SourceReader.hasCache(context, "playlist") -> "playlist"
                                    else -> return@withContext null
                                }
                                val stream = SourceReader.openCached(context, existingKey)
                                val parsed = parsePlaylistWithCache(profile, existingKey, stream, true)
                                parsed to SourceReader.isCacheFresh(context, existingKey, profile.lastUpdated)
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (failure: Exception) {
                            if (profile.source is ContentSource.LocalFile) {
                                error = "${profile.name}: ${failure.message ?: "could not be loaded"}"
                            }
                            null
                        }
                        if (playlists.none { it.id == profile.id && it.enabled && it.source == profile.source }) continue
                        if (cached != null && cached.first.channels.isNotEmpty()) {
                            loadedPlaylists[profile.id] = cached.first
                        }
                        if (profile.source is ContentSource.RemoteUrl && (cached?.second != true || cached.first.channels.isEmpty())) {
                            loadPlaylistProfile(profile, forceRefresh = true, startupScope = this)
                        }
                    }
                }
            } finally {
                startupSourcesLoading = false
            }
        }

        val startupEpgJob = if (savedEpg != null) {
            launch {
                try {
                    epgLoadMutex.withLock {
                        val cachedGuide = withContext(Dispatchers.IO) {
                            val stream = when {
                                savedEpg is ContentSource.LocalFile -> SourceReader.open(context, savedEpg)
                                SourceReader.hasCache(context, "epg") -> SourceReader.openCached(context, "epg")
                                else -> return@withContext null
                            }
                            parseEpgWithCache(savedEpg, stream, allowCachedResult = true)
                        }
                        if (epgSource == savedEpg && cachedGuide != null) epgGuide = cachedGuide
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Keep source restoration independent of a missing or corrupt guide.
                } finally {
                    startupEpgRestoring = false
                }
            }
        } else null

        // Restore cloud sources for authenticated households
        val cloudSourceService = CloudSourceSyncService.get(context)
        val authenticatedUid = currentUserId ?: AccountRepository.get(context).currentUserId()
        if (authenticatedUid != null) {
            launch(Dispatchers.IO) {
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
                    startupEpgJob?.join()
                    withContext(Dispatchers.Main) {
                        val sourceChanged = epgSource != restoredEpg
                        val needsLoad = sourceChanged || epgGuide == null || epgGuide?.programmes.isNullOrEmpty()
                        epgSource = restoredEpg
                        epgSourceName = restoredEpg.displayName
                        epgSourceTimezone = settings.epgSourceTimezone().orEmpty()
                        epgScheduleCorrectionMinutes = settings.epgScheduleCorrectionMinutes()
                        if (needsLoad) {
                            loadEpg(restoredEpg, save = false, forceRefresh = sourceChanged, background = true)
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
            if (currentUserId == null) {
                com.fortresstv.player.account.OnboardingDialog(
                    onComplete = {
                        val uid = accountRepository.currentUserId() ?: return@OnboardingDialog
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
                            playbackResetGeneration++
                        },
                        onDismiss = {}
                    )
                }
            } else if (libraryNamespace != profileProgressNamespace(currentUserId, activeHouseholdProfile.id) ||
                (playlist == null && (startupSourcesLoading || xtreamLoadJobs.isNotEmpty() || loading ||
                    playlists.any { it.enabled && (it.id in loadedPlaylists || it.id in xtreamCatalogs) }))
            ) {
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
            } else key(playbackResetGeneration) {
                FortressLibrary(
                    traktNamespace = libraryNamespace,
                    playlist = playlist!!,
                    activeProfile = activeProfile!!,
                    startupDestination = startupDestination,
                    startupNavigationRequest = startupNavigationRequest,
                    onStartupNavigationHandled = { startupNavigationRequest = null },
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
                    homeSectionOrder = homeSectionOrder,
                    onOpenCustomizeHome = { showCustomizeHomeDialog = true },
                    xtreamCatalogs = playlists.filter { it.enabled }.sortedBy { it.order }.mapNotNull { profile ->
                        val catalog = xtreamCatalogs[profile.id] ?: return@mapNotNull null
                        catalog.copy(
                            movies = if (profile.role.contributesMovies()) catalog.movies else emptyList(),
                            movieCategories = if (profile.role.contributesMovies()) catalog.movieCategories else emptyList(),
                            series = if (profile.role.contributesSeries()) catalog.series else emptyList(),
                            seriesCategories = if (profile.role.contributesSeries()) catalog.seriesCategories else emptyList()
                        )
                    },
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
                        val currentTraktNs = profileProgressNamespace(currentUserId, activeHouseholdProfile.id)
                        scope.launch(Dispatchers.IO) {
                            com.fortresstv.player.trakt.TraktWatchlistService.syncItemWatchlist(
                                context = context,
                                profileNamespace = currentTraktNs,
                                isAdd = newWatchlisted,
                                kind = com.fortresstv.player.trakt.TraktWatchlistKind.MOVIE,
                                title = movie.name,
                                year = movie.year?.toIntOrNull(),
                                tmdbId = movie.tmdbId,
                                imdbId = movie.imdbId
                            )
                        }
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
                        val currentTraktNs = profileProgressNamespace(currentUserId, activeHouseholdProfile.id)
                        scope.launch(Dispatchers.IO) {
                            com.fortresstv.player.trakt.TraktWatchedSyncService.syncMovieWatched(
                                context = context,
                                profileNamespace = currentTraktNs,
                                movie = movie,
                                watched = willBeWatched
                            )
                        }
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
                        val currentTraktNs = profileProgressNamespace(currentUserId, activeHouseholdProfile.id)
                        scope.launch(Dispatchers.IO) {
                            com.fortresstv.player.trakt.TraktWatchlistService.syncItemWatchlist(
                                context = context,
                                profileNamespace = currentTraktNs,
                                isAdd = newFollowed,
                                kind = com.fortresstv.player.trakt.TraktWatchlistKind.SHOW,
                                title = series.name,
                                year = series.year?.toIntOrNull(),
                                tmdbId = series.tmdbId,
                                imdbId = series.imdbId
                            )
                        }
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
                    onToggleEpisodeWatched = { sourceId, episode, series ->
                        val key = episode.libraryId(sourceId)
                        val willBeWatched = key !in watchedItemIds
                        watchedItemIds = userLibrary.setItemWatched(key, willBeWatched)
                        val currentTraktNs = profileProgressNamespace(currentUserId, activeHouseholdProfile.id)
                        scope.launch(Dispatchers.IO) {
                            com.fortresstv.player.trakt.TraktWatchedSyncService.syncEpisodeWatched(
                                context = context,
                                profileNamespace = currentTraktNs,
                                series = series,
                                episode = episode,
                                watched = willBeWatched
                            )
                        }
                        if (currentUserId != null && activeHouseholdProfile.id != "restricted_default") {
                            cloudLibrarySyncService.recordLibraryMutation(
                                profileId = activeHouseholdProfile.id,
                                sourceId = sourceId,
                                itemKind = "episode",
                                itemId = episode.id,
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
                    profileSwitcher = headerProfileSwitcher,
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
                    settingsInitialTab = 5
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
                    settings.setStartupDestination(destination, activeHouseholdProfile.id)
                    startupNavigationRequest = destination
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
                    val userId = currentUserId
                    scope.launch {
                        if (userId != null && cloudSourceSyncService.syncTmdbToCloud(newTmdb).isFailure) return@launch
                        if (currentUserId != userId) return@launch
                        tmdbStore.saveSettings(newTmdb)
                        tmdbSettings = newTmdb
                    }
                },
                introDbSettings = introDbSettings,
                onIntroDbSettingsChanged = { newIntroDb ->
                    introDbSettings = newIntroDb
                    introDbStore.saveSettings(newIntroDb)
                },
                omdbSettings = omdbSettings,
                onOmdbSettingsChanged = { newOmdb ->
                    val userId = currentUserId
                    scope.launch {
                        if (userId != null && cloudSourceSyncService.syncOmdbToCloud(newOmdb).isFailure) return@launch
                        if (currentUserId != userId) return@launch
                        omdbStore.saveSettings(newOmdb)
                        omdbSettings = newOmdb
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
                            xtreamLoadJobs.remove(updated.id)?.cancel()
                            loadedPlaylists.remove(updated.id)
                            xtreamCatalogs.remove(updated.id)
                            activeProfile = all.filter { it.enabled }.minByOrNull { it.order }
                        }
                    }
                },
                onSourceRoleChanged = { profile, role ->
                    val updated = profile.copy(role = role)
                    settings.savePlaylistProfile(updated)
                    val all = settings.getPlaylists()
                    playlists = all
                    if (updated.source is ContentSource.Xtream && currentUserId != null) {
                        scope.launch {
                            cloudSourceSyncService.syncSourcesToCloud(profileRepository.currentUnlockToken())
                        }
                    }
                    if (updated.source is ContentSource.Xtream && (loadedPlaylists[updated.id] == null || updated.role.contributesLiveTv())) {
                        loadXtreamProfile(updated)
                    } else {
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
                onOpenCustomizeHome = { showCustomizeHomeDialog = true },
                traktNamespace = profileProgressNamespace(currentUserId, activeHouseholdProfile.id),
                onSyncWatchedHistory = {
                    val currentTraktNs = profileProgressNamespace(currentUserId, activeHouseholdProfile.id)
                    val result = com.fortresstv.player.trakt.TraktWatchedSyncService.syncWatchedHistory(
                        context = context,
                        profileNamespace = currentTraktNs,
                        catalogs = xtreamCatalogs.values.toList(),
                        userLibrary = userLibrary
                    )
                    if (result.isSuccess) {
                        watchedItemIds = userLibrary.watchedItemIds()
                    }
                    result
                }
            )
        }

        if (showCustomizeHomeDialog) {
            com.fortresstv.player.ui.CustomizeHomeDialog(
                hiddenSections = homeHiddenSections,
                sectionOrder = homeSectionOrder,
                onOrderChange = { updated ->
                    homeSectionOrder = updated
                    settings.setHomeSectionOrder(updated, activeHouseholdProfile.id)
                },
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
                    xtreamLoadJobs.remove(profile.id)?.cancel()
                    val remaining = settings.getPlaylists()
                    playlists = remaining
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
                        epgGuide = null
                        epgSource = null
                        epgSourceName = null
                        epgLastUpdate = 0L
                        scope.launch(Dispatchers.IO) {
                            epgLoadMutex.withLock {
                                SourceReader.deleteCache(context, "epg")
                                EpgDiskCache.clear(context)
                                epgRepository.setGuide(null)
                            }
                        }
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
                    if (sourceChanged) {
                        xtreamLoadJobs.remove(profile.id)?.cancel()
                        loadedPlaylists.remove(profile.id)
                        xtreamCatalogs.remove(profile.id)
                    }
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
                            activeProfile = all.filter { it.enabled }.minByOrNull { it.order }
                        }
                    } else if (sourceChanged) {
                        loadPlaylistProfile(updated, forceRefresh = true)
                    } else {
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

