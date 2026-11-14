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

internal data class CatalogMovie(val catalog: XtreamCatalog, val movie: XtreamMovie)


@Composable
internal fun XtreamMoviesScreen(
    catalogs: List<XtreamCatalog>,
    profiles: List<PlaylistProfile>,
    contentFocusRequester: FocusRequester,
    continueWatching: List<VodProgressEntry>,
    progressEntries: List<VodProgressEntry>,
    watchedItemIds: Set<String>,
    onContinueWatching: (VodProgressEntry) -> Unit,
    onRemoveContinueWatching: (VodProgressEntry) -> Unit,
    hideAdultContent: Boolean = false,
    maximumAge: Int? = null,
    blockUnrated: Boolean = false,
    gridState: LazyGridState,
    movieWatchlistIds: Set<String>,
    traktNamespace: String = "default",
    onToggleWatchlist: (String, XtreamMovie) -> Unit,
    onToggleMovieWatched: (String, XtreamMovie) -> Unit,
    onPlay: (XtreamCatalog, XtreamMovie) -> Unit,
    tabState: MoviesTabState = remember { MoviesTabState() },
    sharedRecommendedMovies: List<CatalogMovie>? = null,
    sharedTraktConnected: Boolean? = null
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val isTv = com.fortresstv.player.ui.LocalFormFactor.current.isTelevision
    val omdbSettings = LocalOmdbSettings.current
    var query by tabState.filters.queryState
    var appliedQuery by tabState.filters.appliedQueryState
    var sortOption by tabState.filters.sortOptionState
    var filterTopRated by tabState.filters.filterTopRatedState
    var filterFresh by tabState.filters.filterFreshState
    var filterWatchlist by tabState.filters.filterPersonalState
    var topRatedCount by tabState.topRatedCountState
    var freshCount by tabState.freshCountState
    var ratedCount by tabState.ratedCountState
    var totalCount by tabState.totalCountState
    var selectedCategory by tabState.filters.selectedCategoryState
    var selectedMovie by remember { mutableStateOf<CatalogMovie?>(null) }
    var allMatchingMovies by tabState.allMatchingMoviesState
    var isCatalogLoading by tabState.isCatalogLoadingState
    val progressByKey = remember(progressEntries) { progressEntries.associateBy(VodProgressEntry::key) }
    val showSourceNames = catalogs.size > 1
    val categories = remember(catalogs, hideAdultContent) {
        buildVisibleVodCategories(catalogs, hideAdultContent) { it.movieCategories }
    }
    LaunchedEffect(query) {
        if (query.isNotBlank()) delay(250)
        appliedQuery = query.trim()
    }

    val isDefaultView = appliedQuery.isBlank() &&
        selectedCategory == null &&
        !filterTopRated &&
        !filterFresh &&
        !filterWatchlist

    // Trakt Recommendations (movies-only) for the default view
    val traktConnected = sharedTraktConnected ?: remember(traktNamespace, context) {
        com.fortresstv.player.trakt.TraktSettingsStore(context.applicationContext, traktNamespace).isConnected()
    }
    var localTraktRecItems by remember(traktNamespace) {
        mutableStateOf<List<com.fortresstv.player.trakt.TraktRecommendationItem>>(emptyList())
    }
    LaunchedEffect(traktNamespace, traktConnected, sharedRecommendedMovies) {
        if (sharedRecommendedMovies != null || !traktConnected) {
            localTraktRecItems = emptyList()
            return@LaunchedEffect
        }
        val recStore = com.fortresstv.player.trakt.TraktRecommendationsStore(context.applicationContext, traktNamespace)
        localTraktRecItems = withContext(Dispatchers.IO) { recStore.loadRecommendations() }
        com.fortresstv.player.trakt.TraktRecommendationsService.syncRecommendations(context.applicationContext, traktNamespace)
            .onSuccess { localTraktRecItems = it }
    }
    var localRecommendedMovies by remember(traktNamespace) { mutableStateOf<List<CatalogMovie>>(emptyList()) }
    LaunchedEffect(isDefaultView, traktConnected, localTraktRecItems, catalogs, hideAdultContent, maximumAge, blockUnrated, sharedRecommendedMovies) {
        if (sharedRecommendedMovies != null || !isDefaultView || !traktConnected || localTraktRecItems.isEmpty() || catalogs.isEmpty()) {
            localRecommendedMovies = emptyList()
            return@LaunchedEffect
        }
        localRecommendedMovies = withContext(Dispatchers.Default) {
            com.fortresstv.player.trakt.TraktRecommendationsService.matchRecommendations(
                recommendations = localTraktRecItems,
                catalogs = catalogs,
                hideAdultContent = hideAdultContent,
                maximumAge = maximumAge,
                blockUnrated = blockUnrated
            ).movies
        }
    }
    val recommendedMovies = if (!isDefaultView || !traktConnected) {
        emptyList()
    } else {
        sharedRecommendedMovies ?: localRecommendedMovies
    }

    var watchlistCount by tabState.watchlistCountState
    var watchlistMovies by tabState.watchlistMoviesState
    LaunchedEffect(allMatchingMovies, movieWatchlistIds, tabState) {
        if (!tabState.needsWatchlistRecompute(allMatchingMovies, movieWatchlistIds)) return@LaunchedEffect
        if (movieWatchlistIds.isEmpty() || allMatchingMovies.isEmpty()) {
            watchlistCount = 0
            watchlistMovies = emptyList()
            tabState.markWatchlistComputed(allMatchingMovies, movieWatchlistIds)
            return@LaunchedEffect
        }
        val filtered = withContext(Dispatchers.Default) {
            val matcher = com.fortresstv.player.library.FastIdMatcher(movieWatchlistIds)
            allMatchingMovies.filter { entry ->
                matcher.matches(entry.movie.libraryId(entry.catalog.sourceId), entry.movie.id.toString())
            }
        }
        watchlistMovies = filtered
        watchlistCount = filtered.size
        tabState.markWatchlistComputed(allMatchingMovies, movieWatchlistIds)
    }
    val visibleMovies = if (filterWatchlist) watchlistMovies else allMatchingMovies

    val enrichmentRefreshToken = rememberLowCoverageRatingEnrichment(
        catalogs = catalogs,
        visibleItems = allMatchingMovies,
        ratedCount = ratedCount,
        totalCount = totalCount,
        omdbSettings = omdbSettings,
        gridState = gridState,
        sourceIdOf = { it.catalog.sourceId },
        ratingOf = { it.movie.rating },
        targetOf = { com.fortresstv.player.omdb.OmdbPreFetchTarget("movie", it.movie.name, it.movie.year, it.movie.imdbId) }
    )
    LaunchedEffect(catalogs, appliedQuery, selectedCategory, hideAdultContent, maximumAge, blockUnrated, sortOption, filterTopRated, filterFresh, enrichmentRefreshToken, tabState) {
        val computeParams = listOf(appliedQuery, selectedCategory, hideAdultContent, maximumAge, blockUnrated, sortOption, filterTopRated, filterFresh, enrichmentRefreshToken)
        if (!tabState.needsRecompute(catalogs, computeParams)) return@LaunchedEffect
        if (allMatchingMovies.isEmpty()) isCatalogLoading = true
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
                categoriesOf = { it.movieCategories },
                itemsOf = { it.movies },
                wrap = ::CatalogMovie,
                categoryIdOf = { it.categoryId },
                nameOf = { it.name },
                yearOf = { it.year },
                ageRatingOf = { it.ageRating },
                providerRatingOf = { it.rating },
                cachedOmdbOf = { movie ->
                    if (hasOmdb) OmdbMetadataResolver.getCached(context, "movie", movie.name, movie.year, movie.imdbId)
                    else null
                },
                transformFiltered = ::deduplicateCatalogMovies,
                unwrap = { it.movie }
            )
        }
        allMatchingMovies = result.items
        topRatedCount = result.topRatedCount
        freshCount = result.freshCount
        ratedCount = result.ratedCount
        totalCount = result.totalCount
        isCatalogLoading = false
        tabState.markComputed(catalogs, computeParams)
    }
    LaunchedEffect(appliedQuery, selectedCategory, sortOption, filterTopRated, filterFresh, filterWatchlist, tabState) {
        val scrollParams = listOf(appliedQuery, selectedCategory, sortOption, filterTopRated, filterFresh, filterWatchlist)
        if (tabState.consumeScrollReset(scrollParams) && gridState.firstVisibleItemIndex > 0) {
            gridState.scrollToItem(0)
        }
    }

    val showContinueWatchingShelf = isDefaultView && continueWatching.isNotEmpty()
    val showRecommendedShelf = isDefaultView && traktConnected && recommendedMovies.isNotEmpty()
    val scrollableHeaderCount = (if (showContinueWatchingShelf) 1 else 0) + (if (showRecommendedShelf) 1 else 0)

    // TV focus tracking: restore focus to the opened card on dialog dismiss, and move focus into grid when a shelf disappears
    var lastOpenedCardKey by remember { mutableStateOf<String?>(null) }
    var wasDialogOpen by remember { mutableStateOf(false) }
    val cardFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    var focusedInContinueWatching by remember { mutableStateOf(false) }
    var focusedInRecommended by remember { mutableStateOf(false) }
    var prevShowCw by remember { mutableStateOf(showContinueWatchingShelf) }
    var prevShowRec by remember { mutableStateOf(showRecommendedShelf) }

    LaunchedEffect(selectedMovie) {
        val isOpen = selectedMovie != null
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
        if (isTv && (cwDisappeared || recDisappeared) && visibleMovies.isNotEmpty()) {
            delay(50)
            runCatching { contentFocusRequester.requestFocus() }
        }
    }

    val ratingCacheVersion = rememberVodOmdbPrefetchVersion(
        gridState = gridState,
        visibleItems = visibleMovies,
        omdbSettings = omdbSettings,
        paused = selectedMovie != null
    ) { entry ->
        com.fortresstv.player.omdb.OmdbPreFetchTarget("movie", entry.movie.name, entry.movie.year, entry.movie.imdbId)
    }
    val fastScrollNames = remember(visibleMovies, sortOption) {
        if (sortOption == VodSortOption.ALPHABETICAL) visibleMovies.map { it.movie.name } else emptyList()
    }
    LibraryCatalogScaffold(
        title = strings.navMovies,
        query = query,
        onQueryChanged = { query = it },
        searchPlaceholder = strings.searchMovies,
        categories = categories,
        showCategorySourceNames = showSourceNames,
        selectedCategory = selectedCategory,
        onSelectCategory = { selectedCategory = it },
        sortOption = sortOption,
        onSelectSortOption = { sortOption = it },
        emptyMessage = strings.noMoviesAvailable,
        itemCount = visibleMovies.size,
        isContentLoading = isCatalogLoading,
        gridState = gridState,
        gridTestTag = "movies_grid",
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
                    if (watchlistCount > 0 || filterWatchlist) {
                        Surface(
                            onClick = { filterWatchlist = !filterWatchlist },
                            shape = RoundedCornerShape(8.dp),
                            color = if (filterWatchlist) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (filterWatchlist) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier.tvFocusHighlight(shape = RoundedCornerShape(8.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = if (filterWatchlist) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "${strings.watchlistFilter} · $watchlistCount",
                                    color = if (filterWatchlist) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = if (filterWatchlist) FontWeight.Bold else FontWeight.Medium
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
                    item(key = "movies_continue_watching_shelf", span = { GridItemSpan(maxLineSpan) }) {
                        CompactContinueWatchingShelf(
                            entries = continueWatching,
                            onClick = onContinueWatching,
                            onRemove = onRemoveContinueWatching,
                            onShelfFocusChanged = { focusedInContinueWatching = it }
                        )
                    }
                }
                if (showRecommendedShelf) {
                    item(key = "movies_recommended_shelf", span = { GridItemSpan(maxLineSpan) }) {
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
                                items(recommendedMovies, key = { "movie_rec:${it.catalog.sourceId}:${it.movie.id}" }) { entry ->
                                    val movieKey = "${entry.catalog.sourceId}:${entry.movie.id}"
                                    val recCardKey = "rec:$movieKey"
                                    val cardRequester = remember(recCardKey) {
                                        cardFocusRequesters.getOrPut(recCardKey) { FocusRequester() }
                                    }
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
                                    val rating = formatEffectiveRatingBadge(cachedOmdb?.imdbRating, entry.movie.rating)
                                    VodPosterCard(
                                        title = entry.movie.name,
                                        posterUrl = entry.movie.posterUrl,
                                        subtitle = formatVodMetadata(entry.movie.year, rating),
                                        imdbRating = rating,
                                        rottenTomatoes = cachedOmdb?.rottenTomatoes,
                                        isWatched = libraryId in watchedItemIds,
                                        progressFraction = progressByKey[libraryId]?.progressFraction,
                                        loadPosterFallback = if (entry.movie.posterUrl.isNullOrBlank()) {
                                            client?.let { c -> { c.loadMovieDetails(entry.movie.id).posterUrl } }
                                        } else null,
                                        modifier = Modifier
                                            .width(120.dp)
                                            .focusRequester(cardRequester),
                                        onClick = {
                                            lastOpenedCardKey = recCardKey
                                            selectedMovie = entry
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
            visibleMovies,
            key = { _, entry -> "${entry.catalog.sourceId}:${entry.movie.id}" },
            contentType = { _, _ -> "movie_poster" }
        ) { index, entry ->
            val movieKey = "${entry.catalog.sourceId}:${entry.movie.id}"
            val cardRequester = remember(movieKey) {
                cardFocusRequesters.getOrPut(movieKey) { FocusRequester() }
            }
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
            val rating = formatEffectiveRatingBadge(cachedOmdb?.imdbRating, entry.movie.rating)
            VodPosterCard(
                title = entry.movie.name,
                posterUrl = entry.movie.posterUrl,
                subtitle = formatVodMetadata(entry.movie.year, rating),
                imdbRating = rating,
                rottenTomatoes = cachedOmdb?.rottenTomatoes,
                isWatched = libraryId in watchedItemIds,
                progressFraction = progressByKey[libraryId]?.progressFraction,
                loadPosterFallback = if (entry.movie.posterUrl.isNullOrBlank()) {
                    client?.let { c -> { c.loadMovieDetails(entry.movie.id).posterUrl } }
                } else null,
                modifier = Modifier
                    .then(if (index == 0) Modifier.focusRequester(contentFocusRequester) else Modifier)
                    .focusRequester(cardRequester),
                onClick = {
                    lastOpenedCardKey = movieKey
                    selectedMovie = entry
                }
            )
        }
    }
    selectedMovie?.let { entry ->
        val source = profiles.firstOrNull { it.id == entry.catalog.sourceId }?.source as? ContentSource.Xtream
        val client = source?.let {
            remember(entry.catalog.sourceId, it) {
                XtreamClient(context.applicationContext, entry.catalog.sourceId, it)
            }
        }
        val libraryId = entry.movie.libraryId(entry.catalog.sourceId)
        MovieDetailsDialog(
            movie = entry.movie,
            client = client,
            sourceName = entry.catalog.sourceName.takeIf { showSourceNames },
            isWatchlisted = libraryId in movieWatchlistIds,
            isWatched = libraryId in watchedItemIds,
            onDismiss = { selectedMovie = null },
            onToggleWatchlist = { onToggleWatchlist(entry.catalog.sourceId, entry.movie) },
            onToggleWatched = { onToggleMovieWatched(entry.catalog.sourceId, entry.movie) },
            onWatch = { movie ->
                selectedMovie = null
                onPlay(entry.catalog, movie)
            }
        )
    }
}

