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
internal fun SeriesEpisodesDialog(
    series: XtreamSeries,
    sourceId: String,
    client: XtreamClient,
    isFollowed: Boolean,
    watchedItemIds: Set<String>,
    progressEntries: List<VodProgressEntry>,
    onToggleFollow: () -> Unit,
    onToggleEpisodeWatched: (XtreamEpisode) -> Unit,
    onDismiss: () -> Unit,
    onPlay: (XtreamEpisode, String) -> Unit
) {
    val strings = LocalAppStrings.current
    val isTv = LocalFormFactor.current.isTelevision
    val playNextFocusRequester = remember { FocusRequester() }
    val seriesFollowFocusRequester = remember { FocusRequester() }
    val seriesCloseFocusRequester = remember { FocusRequester() }
    val firstSeasonFocusRequester = remember { FocusRequester() }
    val firstEpisodeFocusRequester = remember { FocusRequester() }
    val seriesRetryFocusRequester = remember { FocusRequester() }
    var focusEpisodeAfterSeasonChange by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val tmdbSettings = LocalTmdbSettings.current
    val omdbSettings = LocalOmdbSettings.current
    var details by remember(series.id) { mutableStateOf<XtreamSeriesDetails?>(null) }
    var loadError by remember(series.id) { mutableStateOf(false) }
    var retryKey by remember(series.id) { mutableIntStateOf(0) }
    var tmdbMetadata by remember(series.id, tmdbSettings) { mutableStateOf<TmdbMetadata?>(null) }
    var omdbMetadata by remember(series.id, omdbSettings) { mutableStateOf<OmdbMetadata?>(null) }

    LaunchedEffect(series.id, tmdbSettings) {
        if (tmdbSettings.enabled && tmdbSettings.apiKey.isNotBlank()) {
            val resolved = TmdbMetadataResolver.resolveSeries(
                context = context,
                settings = tmdbSettings,
                rawTitle = series.name,
                year = series.year,
                tmdbId = series.tmdbId,
                imdbId = series.imdbId
            )
            if (resolved != null) {
                tmdbMetadata = resolved
            }
        }
    }

    LaunchedEffect(series.id, omdbSettings) {
        if (omdbSettings.enabled && omdbSettings.apiKey.isNotBlank()) {
            val resolved = OmdbMetadataResolver.resolveSeries(
                context = context,
                settings = omdbSettings,
                rawTitle = series.name,
                year = series.year,
                imdbId = series.imdbId
            )
            if (resolved != null) {
                omdbMetadata = resolved
            }
        }
    }

    LaunchedEffect(series.id, retryKey) {
        loadError = false
        runCatching { client.loadSeriesDetails(series.id) }
            .onSuccess { details = it }
            .onFailure { loadError = true }
    }
    val episodes = details?.episodes
    val sortedEpisodes = remember(episodes) {
        episodes?.sortedWith(compareBy({ it.season }, { it.episodeNumber })) ?: emptyList()
    }
    val nextUnwatchedEpisode = remember(sortedEpisodes, watchedItemIds, sourceId) {
        sortedEpisodes.firstOrNull { ep ->
            ep.libraryId(sourceId) !in watchedItemIds
        }
    }
    LaunchedEffect(details, nextUnwatchedEpisode) {
        if (details != null && isTv) {
            delay(100)
            runCatching {
                if (nextUnwatchedEpisode != null) playNextFocusRequester.requestFocus()
                else seriesFollowFocusRequester.requestFocus()
            }
        }
    }

    LaunchedEffect(loadError) {
        if (isTv && loadError) {
            delay(100)
            runCatching { seriesRetryFocusRequester.requestFocus() }
        }
    }
    val seasons = remember(episodes) {
        episodes?.map { it.season }?.distinct()?.sorted() ?: emptyList()
    }
    var selectedSeason by remember(seasons) {
        mutableStateOf(seasons.firstOrNull())
    }
    val filteredEpisodes = remember(episodes, selectedSeason) {
        val list = episodes ?: return@remember emptyList()
        if (selectedSeason != null && seasons.size > 1) {
            list.filter { it.season == selectedSeason }
        } else {
            list
        }
    }

    val effectiveTmdbId = tmdbMetadata?.tmdbId ?: series.tmdbId?.toIntOrNull() ?: details?.tmdbId?.toIntOrNull()
    var seasonMetadata by remember(series.id, selectedSeason, effectiveTmdbId) {
        mutableStateOf<TmdbSeasonMetadata?>(null)
    }

    LaunchedEffect(effectiveTmdbId, selectedSeason, tmdbSettings) {
        val tmdbId = effectiveTmdbId
        val season = selectedSeason ?: seasons.firstOrNull() ?: 1
        if (tmdbSettings.enabled && tmdbSettings.apiKey.isNotBlank() && tmdbId != null && tmdbId > 0) {
            val resolved = TmdbMetadataResolver.resolveSeason(
                context = context,
                settings = tmdbSettings,
                tmdbSeriesId = tmdbId,
                seasonNumber = season
            )
            if (resolved != null) {
                seasonMetadata = resolved
            }
        }
    }

    var nextUpSeasonMetadata by remember(series.id, nextUnwatchedEpisode?.season, effectiveTmdbId) {
        mutableStateOf<TmdbSeasonMetadata?>(null)
    }

    LaunchedEffect(effectiveTmdbId, nextUnwatchedEpisode?.season, tmdbSettings) {
        val tmdbId = effectiveTmdbId
        val nextSeason = nextUnwatchedEpisode?.season
        if (tmdbSettings.enabled && tmdbSettings.apiKey.isNotBlank() && tmdbId != null && tmdbId > 0 && nextSeason != null) {
            if (nextSeason == (selectedSeason ?: seasons.firstOrNull())) {
                nextUpSeasonMetadata = seasonMetadata
            } else {
                val resolved = TmdbMetadataResolver.resolveSeason(
                    context = context,
                    settings = tmdbSettings,
                    tmdbSeriesId = tmdbId,
                    seasonNumber = nextSeason
                )
                if (resolved != null) {
                    nextUpSeasonMetadata = resolved
                }
            }
        }
    }
    LaunchedEffect(selectedSeason, focusEpisodeAfterSeasonChange) {
        if (isTv && focusEpisodeAfterSeasonChange) {
            focusEpisodeAfterSeasonChange = false
            delay(100)
            runCatching { firstEpisodeFocusRequester.requestFocus() }
        }
    }

    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600

    BackHandler(onBack = onDismiss)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            shadowElevation = 14.dp,
            modifier = Modifier
                .fillMaxWidth(if (isCompact) 0.96f else 0.90f)
                .fillMaxHeight(if (isCompact) 0.94f else 0.90f)
        ) {
            Box(Modifier.fillMaxSize()) {
                // Background Backdrop Fanart with cinematic gradient overlay
                val effectiveBackdrop = (tmdbMetadata?.backdropUrl ?: details?.backdropUrl)?.takeIf(String::isNotBlank)
                if (effectiveBackdrop != null) {
                    AsyncImage(
                        model = effectiveBackdrop,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.60f)
                            .align(Alignment.TopCenter)
                            .graphicsLayer(alpha = 0.20f)
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.70f)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    0.0f to MaterialTheme.colorScheme.surface.copy(alpha = 0.15f),
                                    0.60f to MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                                    1.0f to MaterialTheme.colorScheme.surface
                                )
                            )
                    )
                }

                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(if (isCompact) 14.dp else 22.dp)
                ) {
                    // Header: Cover, Title, Metadata Badges & Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 40.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val effectiveCover = tmdbMetadata?.posterUrl ?: details?.coverUrl ?: series.coverUrl
                        if (!effectiveCover.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier.size(if (isCompact) 64.dp else 84.dp, if (isCompact) 96.dp else 126.dp)
                            ) {
                                AsyncImage(
                                    model = effectiveCover,
                                    contentDescription = series.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(Modifier.width(if (isCompact) 12.dp else 18.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = series.name,
                                fontSize = if (isCompact) 19.sp else 26.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            val meta = formatVodMetadata(
                                details?.releaseDate?.take(4) ?: series.year,
                                details?.rating ?: series.rating
                            )
                            Spacer(Modifier.height(3.dp))
                            OmdbRatingsRow(
                                metadata = omdbMetadata,
                                fallbackText = meta
                            )
                            OmdbAwardsRow(
                                awards = omdbMetadata?.awards,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            val plot = tmdbMetadata?.overview ?: details?.plot ?: series.plot
                            if (!plot.isNullOrBlank()) {
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    plot,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    maxLines = if (isCompact) 2 else 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            details?.let { info ->
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    info.genre?.takeIf(String::isNotBlank)?.let { g ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = g.split(",").firstOrNull()?.trim() ?: g,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    info.ageRating?.takeIf(String::isNotBlank)?.let { age ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = age,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        val seriesTrailer = tmdbMetadata?.trailerUrl ?: details?.trailerUrl
                        if (!seriesTrailer.isNullOrBlank()) {
                            IconButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(seriesTrailer)))
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = strings.watchTrailer, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(8.dp))
                        }

                        // Follow Toggle Button
                        if (isCompact) {
                            IconButton(
                                onClick = onToggleFollow,
                                modifier = Modifier
                                    .focusRequester(seriesFollowFocusRequester)
                                    .tvFocusHighlight(
                                        shape = CircleShape,
                                        scaleOnFocus = if (isTv) 1.12f else 1f
                                    )
                            ) {
                                Icon(
                                    if (isFollowed) Icons.Default.Star else Icons.Outlined.StarOutline,
                                    contentDescription = if (isFollowed) strings.unfollowSeries else strings.followSeries,
                                    tint = if (isFollowed) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = onToggleFollow,
                                modifier = Modifier
                                    .focusRequester(seriesFollowFocusRequester)
                                    .tvFocusHighlight(
                                        shape = ButtonDefaults.outlinedShape,
                                        scaleOnFocus = if (isTv) 1.05f else 1f
                                    )
                            ) {
                                Icon(
                                    if (isFollowed) Icons.Default.Star else Icons.Outlined.StarOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (isFollowed) strings.unfollowSeries else strings.followSeries)
                            }
                        }
                    }

                    // Next Up Quick Action Hero Banner
                    if (nextUnwatchedEpisode != null) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            onClick = { onPlay(nextUnwatchedEpisode, client.episodeStreamUrl(nextUnwatchedEpisode)) },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(playNextFocusRequester)
                                .tvFocusHighlight(
                                    shape = RoundedCornerShape(14.dp),
                                    scaleOnFocus = if (isTv) 1.03f else 1.0f
                                )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    val nextEpProgress = progressEntries.firstOrNull { it.key == nextUnwatchedEpisode.libraryId(sourceId) }
                                    val isNextEpInProgress = nextEpProgress != null && nextEpProgress.positionMs >= 10_000L
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ) {
                                            Text(
                                                text = if (isNextEpInProgress) strings.resume else strings.nextUpBadge,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            strings.episodeLabel(nextUnwatchedEpisode.season, nextUnwatchedEpisode.episodeNumber),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    val nextEpTmdb = (if (nextUnwatchedEpisode.season == selectedSeason) seasonMetadata else nextUpSeasonMetadata)
                                        ?.episodes?.get(nextUnwatchedEpisode.episodeNumber)
                                    val nextEpPlot = nextEpTmdb?.overview?.takeIf(String::isNotBlank)
                                        ?: nextUnwatchedEpisode.plot?.takeIf(String::isNotBlank)
                                        ?: details?.seasonOverviews?.get(nextUnwatchedEpisode.season)
                                        ?: details?.plot
                                        ?: series.plot
                                    val nextEpTitle = nextEpTmdb?.name?.takeIf { it.isNotBlank() && (nextUnwatchedEpisode.title.startsWith("Episode", ignoreCase = true) || nextUnwatchedEpisode.title.isBlank()) }
                                        ?: nextUnwatchedEpisode.title

                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        nextEpTitle,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!nextEpPlot.isNullOrBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            nextEpPlot,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                val totalUnwatched = sortedEpisodes.count { it.libraryId(sourceId) !in watchedItemIds }
                                if (totalUnwatched > 0) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        strings.unwatchedEpisodesCount(totalUnwatched),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    } else if (sortedEpisodes.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.allEpisodesWatched, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            TextButton(onClick = {
                                val first = sortedEpisodes.firstOrNull()
                                if (first != null) onPlay(first, client.episodeStreamUrl(first))
                            }) {
                                Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.restartSeries, fontSize = 12.sp)
                            }
                        }
                    }

                    // Season Pill Switcher with Unwatched Badges
                    if (seasons.size > 1) {
                        Spacer(Modifier.height(12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(seasons, key = { _, season -> season }) { index, season ->
                                val seasonEpisodes = episodes?.filter { it.season == season } ?: emptyList()
                                val seasonUnwatchedCount = seasonEpisodes.count { it.libraryId(sourceId) !in watchedItemIds }
                                val isSeasonSelected = (selectedSeason ?: seasons.firstOrNull()) == season

                                FilterChip(
                                    selected = isSeasonSelected,
                                    onClick = {
                                        selectedSeason = season
                                        focusEpisodeAfterSeasonChange = true
                                    },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(strings.seasonLabel(season))
                                            if (seasonUnwatchedCount > 0) {
                                                Spacer(Modifier.width(6.dp))
                                                Surface(
                                                    shape = CircleShape,
                                                    color = if (isSeasonSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = if (isSeasonSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                                ) {
                                                    Text(
                                                        text = seasonUnwatchedCount.toString(),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    modifier = (if (index == 0) Modifier.focusRequester(firstSeasonFocusRequester) else Modifier)
                                        .tvFocusHighlight(
                                            shape = FilterChipDefaults.shape,
                                            scaleOnFocus = if (isTv) 1.06f else 1f
                                        )
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Episode Content List
                    when {
                        loadError -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(strings.seriesDetailsUnavailable, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { retryKey++ },
                                    modifier = Modifier
                                        .focusRequester(seriesRetryFocusRequester)
                                        .tvFocusHighlight(
                                            shape = ButtonDefaults.shape,
                                            scaleOnFocus = if (isTv) 1.05f else 1f
                                        )
                                ) { Text(strings.retry) }
                            }
                        }
                        details == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                            Text(strings.loadingSeriesDetails, modifier = Modifier.padding(top = 72.dp))
                        }
                        episodes!!.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(strings.noEpisodesAvailable)
                        }
                        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            itemsIndexed(filteredEpisodes, key = { _, episode -> "${episode.season}:${episode.id}" }) { index, episode ->
                                val epLibraryId = episode.libraryId(sourceId)
                                val isEpisodeWatched = epLibraryId in watchedItemIds
                                val epProgress = progressEntries.firstOrNull { it.key == epLibraryId }?.progressFraction
                                val tmdbEp = seasonMetadata?.episodes?.get(episode.episodeNumber)
                                val epPlot = tmdbEp?.overview?.takeIf(String::isNotBlank)
                                    ?: episode.plot?.takeIf(String::isNotBlank)
                                    ?: details?.seasonOverviews?.get(episode.season)
                                    ?: details?.plot
                                    ?: series.plot
                                val previewUrl = tmdbEp?.stillUrl?.takeIf(String::isNotBlank)
                                    ?: episode.posterUrl?.takeIf(String::isNotBlank)
                                    ?: details?.backdropUrl?.takeIf(String::isNotBlank)
                                    ?: details?.coverUrl
                                val epTitle = tmdbEp?.name?.takeIf { it.isNotBlank() && (episode.title.startsWith("Episode", ignoreCase = true) || episode.title.isBlank()) }
                                    ?: episode.title

                                Surface(
                                    onClick = { onPlay(episode, client.episodeStreamUrl(episode)) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(if (index == 0) Modifier.focusRequester(firstEpisodeFocusRequester) else Modifier)
                                        .onKeyEvent { keyEvent ->
                                            if (isTv && keyEvent.type == KeyEventType.KeyDown &&
                                                (keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_MENU ||
                                                 keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_STAR)
                                            ) {
                                                onToggleEpisodeWatched(episode)
                                                true
                                            } else false
                                        }
                                        .tvFocusHighlight(
                                            shape = RoundedCornerShape(14.dp),
                                            scaleOnFocus = if (isTv) 1.02f else 1.0f
                                        )
                                ) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 16:9 Thumbnail Frame
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier
                                                .width(if (isCompact) 100.dp else 128.dp)
                                                .aspectRatio(16f / 9f)
                                        ) {
                                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                if (!previewUrl.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = previewUrl,
                                                        contentDescription = epTitle,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(
                                                        Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                }
                                                // Progress bar at the bottom edge of the thumbnail
                                                if (epProgress != null && epProgress > 0f && !isEpisodeWatched) {
                                                    LinearProgressIndicator(
                                                        progress = { epProgress },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(3.dp)
                                                            .align(Alignment.BottomCenter),
                                                        color = MaterialTheme.colorScheme.primary,
                                                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(Modifier.width(14.dp))

                                        // Episode Badge, Title, and Plot
                                        Column(Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = strings.episodeLabel(episode.season, episode.episodeNumber),
                                                    color = if (isEpisodeWatched) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = epTitle,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!epPlot.isNullOrBlank()) {
                                                Spacer(Modifier.height(3.dp))
                                                Text(
                                                    text = epPlot,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        // Watched Checkmark Button
                                        IconButton(
                                            onClick = { onToggleEpisodeWatched(episode) },
                                            enabled = !isTv,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .focusable(!isTv)
                                        ) {
                                            Icon(
                                                imageVector = if (isEpisodeWatched) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                                                contentDescription = if (isEpisodeWatched) strings.markUnwatched else strings.markWatched,
                                                tint = if (isEpisodeWatched) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = strings.watchNow,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Close Button in Top-Right
                FilledIconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(if (isCompact) 8.dp else 14.dp)
                        .focusRequester(seriesCloseFocusRequester)
                        .tvFocusHighlight(
                            shape = CircleShape,
                            scaleOnFocus = if (isTv) 1.15f else 1f
                        ),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = strings.close,
                        modifier = Modifier.size(if (isCompact) 18.dp else 22.dp)
                    )
                }
            }
        }
    }
}

internal fun formatPlaybackTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

internal const val NEXT_EPISODE_TRIGGER_REMAINING_MS = 45_000L
internal const val NEXT_EPISODE_COUNTDOWN_SECONDS = 10

internal fun shouldStartNextEpisodeCountdown(
    positionMs: Long,
    durationMs: Long,
    autoPlayEnabled: Boolean,
    dismissed: Boolean,
    nextEpisodeAvailable: Boolean,
    currentCountdown: Int,
    stopAtEnd: Boolean = false
): Boolean = autoPlayEnabled &&
    !stopAtEnd &&
    !dismissed &&
    nextEpisodeAvailable &&
    currentCountdown < 0 &&
    durationMs > NEXT_EPISODE_TRIGGER_REMAINING_MS &&
    positionMs >= durationMs - NEXT_EPISODE_TRIGGER_REMAINING_MS

