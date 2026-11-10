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

@Composable
internal fun SettingsDialog(
    initialTab: Int = 0,
    onAccountSignedOut: () -> Unit = {},
    onAccountSignedIn: () -> Unit = {},
    playlists: List<PlaylistProfile>,
    activeProfile: PlaylistProfile?,
    appLanguage: AppLanguage,
    onLanguageChanged: (AppLanguage) -> Unit,
    startupDestination: StartupDestination = StartupDestination.HOME,
    onStartupDestinationChanged: (StartupDestination) -> Unit = {},
    playChannelsFullscreen: Boolean,
    onPlayChannelsFullscreenChanged: (Boolean) -> Unit,
    autoPictureInPicture: Boolean,
    onAutoPictureInPictureChanged: (Boolean) -> Unit,
    telemetryEnabled: Boolean = true,
    onTelemetryEnabledChanged: (Boolean) -> Unit = {},
    playbackMode: PlaybackMode,
    onPlaybackModeChanged: (PlaybackMode) -> Unit,
    preferredAudioLanguage: String = "default",
    onPreferredAudioLanguageChanged: (String) -> Unit = {},
    preferredSubtitleLanguage: String = "off",
    onPreferredSubtitleLanguageChanged: (String) -> Unit = {},
    subtitleFallbackEnglish: Boolean = true,
    onSubtitleFallbackEnglishChanged: (Boolean) -> Unit = {},
    openSubtitlesSettings: OpenSubtitlesSettings = OpenSubtitlesSettings(),
    onOpenSubtitlesSettingsChanged: (OpenSubtitlesSettings) -> Unit = {},
    tmdbSettings: TmdbSettings = TmdbSettings(),
    onTmdbSettingsChanged: (TmdbSettings) -> Unit = {},
    introDbSettings: TheIntroDbSettings = TheIntroDbSettings(),
    onIntroDbSettingsChanged: (TheIntroDbSettings) -> Unit = {},
    omdbSettings: OmdbSettings = OmdbSettings(),
    onOmdbSettingsChanged: (OmdbSettings) -> Unit = {},
    hideAdultContent: Boolean,
    isChildProfile: Boolean = false,
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
    epgScheduleCorrectionMinutes: Int = epgTimeShiftHours * 60,
    epgSourceTimezone: String = "",
    epgMappings: Map<String, EpgChannelMapping> = emptyMap(),
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
    onSendPlaylistToTv: (PlaylistProfile) -> Unit,
    onEpgUrl: () -> Unit,
    onEpgFile: () -> Unit,
    onRefreshEpg: () -> Unit,
    onRemoveEpg: () -> Unit,
    onEpgTimeShiftChanged: (Int) -> Unit = {},
    onEpgScheduleCorrectionChanged: (Int) -> Unit = {},
    onEpgSourceTimezoneChanged: (String) -> Unit = {},
    onSaveMapping: (PlaylistChannel, com.fortresstv.player.epg.EpgChannel, String?) -> Unit = { _, _, _ -> },
    onResetMapping: (PlaylistChannel, String?) -> Unit = { _, _ -> },
    onRequestAdultAuth: (((String?) -> Unit)) -> Unit = {},
    onEpgLoadedDaysChanged: (Int) -> Unit,
    onOpenCustomizeHome: (() -> Unit)? = null,
    traktNamespace: String = "local",
    onSyncWatchedHistory: (suspend () -> Result<Int>)? = null
) {
    val strings = LocalAppStrings.current
    val configuration = LocalConfiguration.current
    val isTwoPane = configuration.screenWidthDp >= 640
    val isTv = LocalFormFactor.current.isTelevision

    val tabItems = listOf(
        Pair(strings.tabGeneral, Icons.Default.Settings),
        Pair(strings.tabPlaylists, Icons.Default.FolderOpen),
        Pair(strings.tabEpg, Icons.Default.DateRange),
        Pair(strings.tabSubtitles, Icons.Default.Subtitles),
        Pair(strings.tabApiKeys, Icons.Default.Key),
        Pair(strings.tabAccount, Icons.Default.AccountCircle),
        Pair(strings.tabAbout, Icons.Default.Info)
    )

    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, (tabItems.size - 1).coerceAtLeast(0))) }
    val tabFocusRequesters = remember(tabItems.size) { List(tabItems.size) { FocusRequester() } }
    var settingsContentHasFocus by remember { mutableStateOf(false) }

    LaunchedEffect(isTv) {
        if (isTv) {
            delay(150)
            try {
                tabFocusRequesters.getOrNull(initialTab.coerceIn(tabFocusRequesters.indices))?.requestFocus()
            } catch (_: Throwable) {}
        }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    fun hideKeyboard() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    BackHandler(onBack = {
        hideKeyboard()
        if (isTv && settingsContentHasFocus) {
            runCatching { tabFocusRequesters.getOrNull(selectedTab)?.requestFocus() }
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
                                        .then(
                                            tabFocusRequesters.getOrNull(index)?.let { Modifier.focusRequester(it) } ?: Modifier
                                        )
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
                                    startupDestination = startupDestination,
                                    onStartupDestinationChanged = onStartupDestinationChanged,
                                    playChannelsFullscreen = playChannelsFullscreen,
                                    onPlayChannelsFullscreenChanged = onPlayChannelsFullscreenChanged,
                                    autoPictureInPicture = autoPictureInPicture,
                                    onAutoPictureInPictureChanged = onAutoPictureInPictureChanged,
                                    telemetryEnabled = telemetryEnabled,
                                    onTelemetryEnabledChanged = onTelemetryEnabledChanged,
                                    playbackMode = playbackMode,
                                    onPlaybackModeChanged = onPlaybackModeChanged,
                                    preferredAudioLanguage = preferredAudioLanguage,
                                    onPreferredAudioLanguageChanged = onPreferredAudioLanguageChanged,
                                    hideAdultContent = hideAdultContent,
                                    isChildProfile = isChildProfile,
                                    onHideAdultContentChanged = onHideAdultContentChanged,
                                    parentalControlEnabled = parentalControlEnabled,
                                    hasParentalPin = hasParentalPin,
                                    onParentalControlToggled = onParentalControlToggled,
                                    onChangeParentalPin = onChangeParentalPin,
                                    onRemoveParentalPin = onRemoveParentalPin,
                                    onOpenCustomizeHome = onOpenCustomizeHome
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
                                    onDeletePlaylist = onDeletePlaylist,
                                    onSendPlaylistToTv = onSendPlaylistToTv
                                )
                                2 -> EpgTab(
                                    epgSource = epgSource,
                                    epgSourceName = epgSourceName,
                                    epgLastUpdate = epgLastUpdate,
                                    epgTimeShiftHours = epgTimeShiftHours,
                                    epgScheduleCorrectionMinutes = epgScheduleCorrectionMinutes,
                                    epgSourceTimezone = epgSourceTimezone,
                                    epgLoadedDays = epgLoadedDays,
                                    playlist = playlist,
                                    epgGuide = epgGuide,
                                    epgMappings = epgMappings,
                                    hideAdultContent = hideAdultContent,
                                    hiddenCategories = hiddenCategories,
                                    loading = loading,
                                    onEpgUrl = onEpgUrl,
                                    onEpgFile = onEpgFile,
                                    onRefreshEpg = onRefreshEpg,
                                    onRemoveEpg = onRemoveEpg,
                                    onEpgScheduleCorrectionChanged = onEpgScheduleCorrectionChanged,
                                    onEpgSourceTimezoneChanged = onEpgSourceTimezoneChanged,
                                    onSaveMapping = onSaveMapping,
                                    onResetMapping = onResetMapping,
                                    onRequestAdultAuth = onRequestAdultAuth,
                                    onEpgTimeShiftChanged = onEpgTimeShiftChanged,
                                    onEpgLoadedDaysChanged = onEpgLoadedDaysChanged
                                )
                                3 -> GeneralTab(
                                    appLanguage = appLanguage,
                                    onLanguageChanged = onLanguageChanged,
                                    playChannelsFullscreen = playChannelsFullscreen,
                                    onPlayChannelsFullscreenChanged = onPlayChannelsFullscreenChanged,
                                    autoPictureInPicture = autoPictureInPicture,
                                    onAutoPictureInPictureChanged = onAutoPictureInPictureChanged,
                                    telemetryEnabled = telemetryEnabled,
                                    onTelemetryEnabledChanged = onTelemetryEnabledChanged,
                                    playbackMode = playbackMode,
                                    onPlaybackModeChanged = onPlaybackModeChanged,
                                    preferredAudioLanguage = preferredAudioLanguage,
                                    onPreferredAudioLanguageChanged = onPreferredAudioLanguageChanged,
                                    preferredSubtitleLanguage = preferredSubtitleLanguage,
                                    onPreferredSubtitleLanguageChanged = onPreferredSubtitleLanguageChanged,
                                    subtitleFallbackEnglish = subtitleFallbackEnglish,
                                    onSubtitleFallbackEnglishChanged = onSubtitleFallbackEnglishChanged,
                                    openSubtitlesSettings = openSubtitlesSettings,
                                    onOpenSubtitlesSettingsChanged = onOpenSubtitlesSettingsChanged,
                                    hideAdultContent = hideAdultContent,
                                    isChildProfile = isChildProfile,
                                    onHideAdultContentChanged = onHideAdultContentChanged,
                                    parentalControlEnabled = parentalControlEnabled,
                                    hasParentalPin = hasParentalPin,
                                    onParentalControlToggled = onParentalControlToggled,
                                    onChangeParentalPin = onChangeParentalPin,
                                    onRemoveParentalPin = onRemoveParentalPin,
                                    subtitlesOnly = true
                                )
                                4 -> ApiKeysTab(
                                    tmdbSettings = tmdbSettings,
                                    onTmdbSettingsChanged = onTmdbSettingsChanged,
                                    introDbSettings = introDbSettings,
                                    onIntroDbSettingsChanged = onIntroDbSettingsChanged,
                                    omdbSettings = omdbSettings,
                                    onOmdbSettingsChanged = onOmdbSettingsChanged,
                                    traktNamespace = traktNamespace,
                                    onSyncWatchedHistory = onSyncWatchedHistory
                                )
                                5 -> AccountTab(
                                    showHeader = false,
                                    onSignedOut = onAccountSignedOut,
                                    onSignedIn = onAccountSignedIn
                                )
                                6 -> AboutTab()
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
                                startupDestination = startupDestination,
                                onStartupDestinationChanged = onStartupDestinationChanged,
                                playChannelsFullscreen = playChannelsFullscreen,
                                onPlayChannelsFullscreenChanged = onPlayChannelsFullscreenChanged,
                                autoPictureInPicture = autoPictureInPicture,
                                onAutoPictureInPictureChanged = onAutoPictureInPictureChanged,
                                playbackMode = playbackMode,
                                onPlaybackModeChanged = onPlaybackModeChanged,
                                preferredAudioLanguage = preferredAudioLanguage,
                                onPreferredAudioLanguageChanged = onPreferredAudioLanguageChanged,
                                hideAdultContent = hideAdultContent,
                                isChildProfile = isChildProfile,
                                onHideAdultContentChanged = onHideAdultContentChanged,
                                parentalControlEnabled = parentalControlEnabled,
                                hasParentalPin = hasParentalPin,
                                onParentalControlToggled = onParentalControlToggled,
                                onChangeParentalPin = onChangeParentalPin,
                                onRemoveParentalPin = onRemoveParentalPin,
                                onOpenCustomizeHome = onOpenCustomizeHome
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
                                onDeletePlaylist = onDeletePlaylist,
                                onSendPlaylistToTv = onSendPlaylistToTv
                            )
                            2 -> EpgTab(
                                epgSource = epgSource,
                                epgSourceName = epgSourceName,
                                epgLastUpdate = epgLastUpdate,
                                epgTimeShiftHours = epgTimeShiftHours,
                                epgScheduleCorrectionMinutes = epgScheduleCorrectionMinutes,
                                epgSourceTimezone = epgSourceTimezone,
                                epgLoadedDays = epgLoadedDays,
                                playlist = playlist,
                                epgGuide = epgGuide,
                                epgMappings = epgMappings,
                                hideAdultContent = hideAdultContent,
                                hiddenCategories = hiddenCategories,
                                loading = loading,
                                onEpgUrl = onEpgUrl,
                                onEpgFile = onEpgFile,
                                onRefreshEpg = onRefreshEpg,
                                onRemoveEpg = onRemoveEpg,
                                onEpgScheduleCorrectionChanged = onEpgScheduleCorrectionChanged,
                                onEpgSourceTimezoneChanged = onEpgSourceTimezoneChanged,
                                onSaveMapping = onSaveMapping,
                                onResetMapping = onResetMapping,
                                onRequestAdultAuth = onRequestAdultAuth,
                                onEpgTimeShiftChanged = onEpgTimeShiftChanged,
                                onEpgLoadedDaysChanged = onEpgLoadedDaysChanged
                            )
                            3 -> GeneralTab(
                                appLanguage = appLanguage,
                                onLanguageChanged = onLanguageChanged,
                                playChannelsFullscreen = playChannelsFullscreen,
                                onPlayChannelsFullscreenChanged = onPlayChannelsFullscreenChanged,
                                autoPictureInPicture = autoPictureInPicture,
                                onAutoPictureInPictureChanged = onAutoPictureInPictureChanged,
                                playbackMode = playbackMode,
                                onPlaybackModeChanged = onPlaybackModeChanged,
                                preferredAudioLanguage = preferredAudioLanguage,
                                onPreferredAudioLanguageChanged = onPreferredAudioLanguageChanged,
                                preferredSubtitleLanguage = preferredSubtitleLanguage,
                                onPreferredSubtitleLanguageChanged = onPreferredSubtitleLanguageChanged,
                                subtitleFallbackEnglish = subtitleFallbackEnglish,
                                onSubtitleFallbackEnglishChanged = onSubtitleFallbackEnglishChanged,
                                openSubtitlesSettings = openSubtitlesSettings,
                                onOpenSubtitlesSettingsChanged = onOpenSubtitlesSettingsChanged,
                                hideAdultContent = hideAdultContent,
                                isChildProfile = isChildProfile,
                                onHideAdultContentChanged = onHideAdultContentChanged,
                                parentalControlEnabled = parentalControlEnabled,
                                hasParentalPin = hasParentalPin,
                                onParentalControlToggled = onParentalControlToggled,
                                onChangeParentalPin = onChangeParentalPin,
                                onRemoveParentalPin = onRemoveParentalPin,
                                subtitlesOnly = true
                            )
                            4 -> ApiKeysTab(
                                tmdbSettings = tmdbSettings,
                                onTmdbSettingsChanged = onTmdbSettingsChanged,
                                introDbSettings = introDbSettings,
                                onIntroDbSettingsChanged = onIntroDbSettingsChanged,
                                omdbSettings = omdbSettings,
                                onOmdbSettingsChanged = onOmdbSettingsChanged,
                                traktNamespace = traktNamespace,
                                onSyncWatchedHistory = onSyncWatchedHistory
                            )
                            5 -> AccountTab(
                                showHeader = false,
                                onSignedOut = onAccountSignedOut,
                                onSignedIn = onAccountSignedIn
                            )
                            6 -> AboutTab()
                        }
                    }
                }
            }
        }
    }
    }
}


