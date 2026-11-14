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
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ThumbUp
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

internal data class CatalogSeries(val catalog: XtreamCatalog, val series: XtreamSeries)


@Composable
internal fun XtreamSeriesScreen(
    catalogs: List<XtreamCatalog>,
    profiles: List<PlaylistProfile>,
    contentFocusRequester: FocusRequester,
    hideAdultContent: Boolean = false,
    maximumAge: Int? = null,
    blockUnrated: Boolean = false,
    gridState: LazyGridState,
    followedSeriesIds: Set<String>,
    watchedItemIds: Set<String>,
    traktNamespace: String = "default",
    continueWatching: List<VodProgressEntry>,
    progressEntries: List<VodProgressEntry>,
    onContinueWatching: (VodProgressEntry) -> Unit,
    onRemoveContinueWatching: (VodProgressEntry) -> Unit,
    onToggleFollow: (String, XtreamSeries) -> Unit,
    onToggleEpisodeWatched: (String, XtreamEpisode, XtreamSeries?) -> Unit,
    onPlay: (XtreamCatalog, XtreamSeries, XtreamEpisode, String) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val isTv = com.fortresstv.player.ui.LocalFormFactor.current.isTelevision
    val omdbSettings = LocalOmdbSettings.current
    var query by remember { mutableStateOf("") }
    var appliedQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(VodSortOption.ALPHABETICAL) }
    var filterTopRated by remember { mutableStateOf(false) }
    var filterFresh by remember { mutableStateOf(false) }
    var filterFollowed by remember { mutableStateOf(false) }
    var topRatedCount by remember { mutableStateOf(0) }
    var freshCount by remember { mutableStateOf(0) }
    var ratedCount by remember { mutableStateOf(0) }
    var totalCount by remember { mutableStateOf(0) }
    var selectedCategory by remember { mutableStateOf<CatalogCategory?>(null) }
    var selected by remember { mutableStateOf<CatalogSeries?>(null) }
    var allMatchingSeries by remember { mutableStateOf<List<CatalogSeries>>(emptyList()) }
    var isCatalogLoading by remember { mutableStateOf(true) }
    val showSourceNames = catalogs.size > 1
    val categories = remember(catalogs, hideAdultContent) {
        buildVisibleVodCategories(catalogs, hideAdultContent) { it.seriesCategories }
    }
    LaunchedEffect(query) {
        if (query.isNotBlank()) delay(250)
        appliedQuery = query.trim()
    }

    val isDefaultView = appliedQuery.isBlank() &&
        selectedCategory == null &&
        !filterTopRated &&
        !filterFresh &&
        !filterFollowed

    // Trakt Recommendations (series-only) for the default view
    val traktConnected = remember(traktNamespace, context) {
        com.fortresstv.player.trakt.TraktSettingsStore(context.applicationContext, traktNamespace).isConnected()
    }
    var traktRecItems by remember(traktNamespace) {
        mutableStateOf<List<com.fortresstv.player.trakt.TraktRecommendationItem>>(emptyList())
    }
    LaunchedEffect(traktNamespace, traktConnected) {
        if (!traktConnected) {
            traktRecItems = emptyList()
            return@LaunchedEffect
        }
        val recStore = com.fortresstv.player.trakt.TraktRecommendationsStore(context.applicationContext, traktNamespace)
        traktRecItems = withContext(Dispatchers.IO) { recStore.loadRecommendations() }
        com.fortresstv.player.trakt.TraktRecommendationsService.syncRecommendations(context.applicationContext, traktNamespace)
            .onSuccess { traktRecItems = it }
    }
    var recommendedSeries by remember(traktNamespace) { mutableStateOf<List<CatalogSeries>>(emptyList()) }
    LaunchedEffect(isDefaultView, traktConnected, traktRecItems, catalogs, hideAdultContent, maximumAge, blockUnrated) {
        if (!isDefaultView || !traktConnected || traktRecItems.isEmpty() || catalogs.isEmpty()) {
            recommendedSeries = emptyList()
            return@LaunchedEffect
        }
        recommendedSeries = withContext(Dispatchers.Default) {
            com.fortresstv.player.trakt.TraktRecommendationsService.matchRecommendations(
                recommendations = traktRecItems,
                catalogs = catalogs,
                hideAdultContent = hideAdultContent,
                maximumAge = maximumAge,
                blockUnrated = blockUnrated
            ).series
        }
    }

    // Series progress for resumable episode & cached all-episodes-watched status
    val seriesProgressByKey = remember(progressEntries) {
        progressEntries
            .asSequence()
            .filter { it.kind == VodProgressKind.EPISODE && it.seriesId != null && it.progressFraction > 0f }
            .sortedByDescending { it.updatedAtMs }
            .distinctBy { "${it.sourceId}:${it.seriesId}" }
            .associate { "${it.sourceId}:${it.seriesId}" to it.progressFraction }
    }
    var fullyWatchedSeriesKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(catalogs, profiles, watchedItemIds) {
        if (watchedItemIds.isEmpty() || catalogs.isEmpty()) {
            fullyWatchedSeriesKeys = emptySet()
            return@LaunchedEffect
        }
        fullyWatchedSeriesKeys = withContext(Dispatchers.IO) {
            val watchedKeys = HashSet<String>()
            for (catalog in catalogs) {
                val source = profiles.firstOrNull { it.id == catalog.sourceId }?.source as? ContentSource.Xtream ?: continue
                val client = XtreamClient(context.applicationContext, catalog.sourceId, source)
                for (series in catalog.series) {
                    val details = client.loadCachedSeriesDetails(series.id) ?: continue
                    if (details.episodes.isNotEmpty() && details.episodes.all { it.libraryId(catalog.sourceId) in watchedItemIds }) {
                        watchedKeys.add("${catalog.sourceId}:${series.id}")
                    }
                }
            }
            watchedKeys
        }
    }

    val followedMatcher = remember(followedSeriesIds) {
        if (followedSeriesIds.isEmpty()) null else com.fortresstv.player.library.FastIdMatcher(followedSeriesIds)
    }
    val followedCount = remember(allMatchingSeries, followedMatcher) {
        if (followedMatcher == null) 0
        else allMatchingSeries.count { entry ->
            followedMatcher.matches(entry.series.libraryId(entry.catalog.sourceId), entry.series.id.toString())
        }
    }
    val visibleSeries = remember(allMatchingSeries, filterFollowed, followedMatcher) {
        if (!filterFollowed) allMatchingSeries
        else if (followedMatcher == null) emptyList()
        else allMatchingSeries.filter { entry ->
            followedMatcher.matches(entry.series.libraryId(entry.catalog.sourceId), entry.series.id.toString())
        }
    }

    val enrichmentRefreshToken = rememberLowCoverageRatingEnrichment(
        catalogs = catalogs,
        visibleItems = visibleSeries,
        ratedCount = ratedCount,
        totalCount = totalCount,
        omdbSettings = omdbSettings,
        gridState = gridState,
        sourceIdOf = { it.catalog.sourceId },
        ratingOf = { it.series.rating },
        targetOf = { com.fortresstv.player.omdb.OmdbPreFetchTarget("series", it.series.name, it.series.year, it.series.imdbId) }
    )
    LaunchedEffect(catalogs, appliedQuery, selectedCategory, hideAdultContent, maximumAge, blockUnrated, sortOption, filterTopRated, filterFresh, enrichmentRefreshToken) {
        isCatalogLoading = true
        val result = withContext(Dispatchers.Default) {
            val hasOmdb = OmdbMetadataResolver.hasAnyCached()
            filterAndSortVodItems(
                catalogs = catalogs,
                selectedCategory = selectedCategory,
                appliedQuery = appliedQuery,
                hideAdultContent = hideAdultContent,
                maximumAge = maximumAge,
                blockUnrated = blockUnrated,
                sortOption = sortOption,
                filterTopRated = filterTopRated,
                filterFresh = filterFresh,
                categoriesOf = { it.seriesCategories },
                itemsOf = { it.series },
                wrap = ::CatalogSeries,
                categoryIdOf = { it.categoryId },
                nameOf = { it.name },
                yearOf = { it.year },
                ageRatingOf = { it.ageRating },
                providerRatingOf = { it.rating },
                cachedOmdbOf = { series ->
                    if (hasOmdb) OmdbMetadataResolver.getCached(context, "series", series.name, series.year, series.imdbId)
                    else null
                },
                unwrap = { it.series }
            )
        }
        allMatchingSeries = result.items
        topRatedCount = result.topRatedCount
        freshCount = result.freshCount
        ratedCount = result.ratedCount
        totalCount = result.totalCount
        isCatalogLoading = false
    }
    LaunchedEffect(appliedQuery, selectedCategory, sortOption, filterTopRated, filterFresh, filterFollowed) {
        if (gridState.firstVisibleItemIndex > 0) gridState.scrollToItem(0)
    }

    val showContinueWatchingShelf = isDefaultView && continueWatching.isNotEmpty()
    val showRecommendedShelf = isDefaultView && traktConnected && recommendedSeries.isNotEmpty()
    val scrollableHeaderCount = (if (showContinueWatchingShelf) 1 else 0) + (if (showRecommendedShelf) 1 else 0)

    // TV focus tracking: restore focus to the opened card on dialog dismiss, and move focus into grid when a shelf disappears
    var lastOpenedCardKey by remember { mutableStateOf<String?>(null) }
    var wasDialogOpen by remember { mutableStateOf(false) }
    val cardFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    var focusedInContinueWatching by remember { mutableStateOf(false) }
    var focusedInRecommended by remember { mutableStateOf(false) }
    var prevShowCw by remember { mutableStateOf(showContinueWatchingShelf) }
    var prevShowRec by remember { mutableStateOf(showRecommendedShelf) }

    LaunchedEffect(selected) {
        val isOpen = selected != null
        if (wasDialogOpen && !isOpen && isTv) {
            delay(60)
            val requester = lastOpenedCardKey?.let { cardFocusRequesters[it] } ?: contentFocusRequester
            runCatching { requester.requestFocus() }
        }
        wasDialogOpen = isOpen
    }

    LaunchedEffect(showContinueWatchingShelf, showRecommendedShelf) {
        val cwDisappeared = prevShowCw && !showContinueWatchingShelf && focusedInContinueWatching
        val recDisappeared = prevShowRec && !showRecommendedShelf && focusedInRecommended
        prevShowCw = showContinueWatchingShelf
        prevShowRec = showRecommendedShelf
        if (isTv && (cwDisappeared || recDisappeared) && visibleSeries.isNotEmpty()) {
            delay(50)
            runCatching { contentFocusRequester.requestFocus() }
        }
    }

    val ratingCacheVersion = rememberVodOmdbPrefetchVersion(
        gridState = gridState,
        visibleItems = visibleSeries,
        omdbSettings = omdbSettings,
        paused = selected != null
    ) { entry ->
        com.fortresstv.player.omdb.OmdbPreFetchTarget("series", entry.series.name, entry.series.year, entry.series.imdbId)
    }
    val fastScrollNames = remember(visibleSeries, sortOption) {
        if (sortOption == VodSortOption.ALPHABETICAL) visibleSeries.map { it.series.name } else emptyList()
    }
    LibraryCatalogScaffold(
        title = strings.navSeries,
        query = query,
        onQueryChanged = { query = it },
        searchPlaceholder = strings.searchSeries,
        categories = categories,
        showCategorySourceNames = showSourceNames,
        selectedCategory = selectedCategory,
        onSelectCategory = { selectedCategory = it },
        sortOption = sortOption,
        onSelectSortOption = { sortOption = it },
        emptyMessage = strings.noSeriesAvailable,
        itemCount = visibleSeries.size,
        isContentLoading = isCatalogLoading,
        gridState = gridState,
        gridTestTag = "series_grid",
        fastScrollItems = fastScrollNames,
        onFastScrollJump = { targetIndex ->
            gridState.scrollToItem(targetIndex + scrollableHeaderCount)
        },
        header = {
            VodRatingFilterChipsRow(
                filterTopRated = filterTopRated,
                topRatedCount = topRatedCount,
                onToggleTopRated = { filterTopRated = !filterTopRated },
                filterFresh = filterFresh,
                freshCount = freshCount,
                onToggleFresh = { filterFresh = !filterFresh },
                extraChips = {
                    if (followedCount > 0 || filterFollowed) {
                        Surface(
                            onClick = { filterFollowed = !filterFollowed },
                            shape = RoundedCornerShape(8.dp),
                            color = if (filterFollowed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (filterFollowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier.tvFocusHighlight(shape = RoundedCornerShape(8.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = if (filterFollowed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "${strings.followedFilter} · $followedCount",
                                    color = if (filterFollowed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = if (filterFollowed) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            )
        },
        scrollableHeader = if (scrollableHeaderCount > 0) {
            {
                if (showContinueWatchingShelf) {
                    item(key = "series_continue_watching_shelf", span = { GridItemSpan(maxLineSpan) }) {
                        CompactContinueWatchingShelf(
                            entries = continueWatching,
                            onClick = onContinueWatching,
                            onRemove = onRemoveContinueWatching,
                            onShelfFocusChanged = { focusedInContinueWatching = it }
                        )
                    }
                }
                if (showRecommendedShelf) {
                    item(key = "series_recommended_shelf", span = { GridItemSpan(maxLineSpan) }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .onFocusChanged { focusedInRecommended = it.hasFocus }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ThumbUp,
                                    contentDescription = null,
                                    tint = Color(0xFFED1C24),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = strings.traktRecommendations,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(recommendedSeries, key = { "series_rec:${it.catalog.sourceId}:${it.series.id}" }) { entry ->
                                    val seriesKey = "${entry.catalog.sourceId}:${entry.series.id}"
                                    val recCardKey = "rec:$seriesKey"
                                    val cardRequester = remember(recCardKey) {
                                        cardFocusRequesters.getOrPut(recCardKey) { FocusRequester() }
                                    }
                                    val cachedOmdb = remember(entry.series, omdbSettings, ratingCacheVersion) {
                                        if (omdbSettings.enabled && omdbSettings.apiKey.isNotBlank()) {
                                            OmdbMetadataResolver.getCached(context, "series", entry.series.name, entry.series.year, entry.series.imdbId)
                                        } else null
                                    }
                                    val rating = formatEffectiveRatingBadge(cachedOmdb?.imdbRating, entry.series.rating)
                                    val isSeriesWatched = seriesKey in fullyWatchedSeriesKeys
                                    VodPosterCard(
                                        title = entry.series.name,
                                        posterUrl = entry.series.coverUrl,
                                        subtitle = formatVodMetadata(entry.series.year, rating),
                                        imdbRating = rating,
                                        rottenTomatoes = cachedOmdb?.rottenTomatoes,
                                        isWatched = isSeriesWatched,
                                        watchedContentDescription = if (isSeriesWatched) strings.allAvailableEpisodesWatched else null,
                                        progressFraction = seriesProgressByKey[seriesKey],
                                        modifier = Modifier
                                            .width(120.dp)
                                            .focusRequester(cardRequester),
                                        onClick = {
                                            lastOpenedCardKey = recCardKey
                                            selected = entry
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else null
    ) {
        gridItemsIndexed(
            visibleSeries,
            key = { _, entry -> "${entry.catalog.sourceId}:${entry.series.id}" },
            contentType = { _, _ -> "series_poster" }
        ) { index, entry ->
            val seriesKey = "${entry.catalog.sourceId}:${entry.series.id}"
            val cardRequester = remember(seriesKey) {
                cardFocusRequesters.getOrPut(seriesKey) { FocusRequester() }
            }
            val cachedOmdb = remember(entry.series, omdbSettings, ratingCacheVersion) {
                if (omdbSettings.enabled && omdbSettings.apiKey.isNotBlank()) {
                    OmdbMetadataResolver.getCached(context, "series", entry.series.name, entry.series.year, entry.series.imdbId)
                } else null
            }
            val rating = formatEffectiveRatingBadge(cachedOmdb?.imdbRating, entry.series.rating)
            val isSeriesWatched = seriesKey in fullyWatchedSeriesKeys
            VodPosterCard(
                title = entry.series.name,
                posterUrl = entry.series.coverUrl,
                subtitle = formatVodMetadata(entry.series.year, rating),
                imdbRating = rating,
                rottenTomatoes = cachedOmdb?.rottenTomatoes,
                isWatched = isSeriesWatched,
                watchedContentDescription = if (isSeriesWatched) strings.allAvailableEpisodesWatched else null,
                progressFraction = seriesProgressByKey[seriesKey],
                modifier = Modifier
                    .then(if (index == 0) Modifier.focusRequester(contentFocusRequester) else Modifier)
                    .focusRequester(cardRequester),
                onClick = {
                    lastOpenedCardKey = seriesKey
                    selected = entry
                }
            )
        }
    }

    selected?.let { entry ->
        val profile = profiles.firstOrNull { it.id == entry.catalog.sourceId }
        val source = profile?.source as? ContentSource.Xtream
        if (source != null) {
            SeriesEpisodesDialog(
                series = entry.series,
                sourceId = entry.catalog.sourceId,
                client = remember(entry.catalog.sourceId, source) {
                    XtreamClient(context.applicationContext, entry.catalog.sourceId, source)
                },
                isFollowed = entry.series.libraryId(entry.catalog.sourceId) in followedSeriesIds,
                watchedItemIds = watchedItemIds,
                progressEntries = progressEntries,
                onToggleFollow = { onToggleFollow(entry.catalog.sourceId, entry.series) },
                onToggleEpisodeWatched = { onToggleEpisodeWatched(entry.catalog.sourceId, it, entry.series) },
                onDismiss = { selected = null },
                onPlay = { series, episode, url ->
                    selected = null
                    onPlay(entry.catalog, series, episode, url)
                }
            )
        }
    }
}

