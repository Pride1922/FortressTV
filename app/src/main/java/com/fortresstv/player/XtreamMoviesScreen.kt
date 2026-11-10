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
    onToggleWatchlist: (String, XtreamMovie) -> Unit,
    onToggleMovieWatched: (String, XtreamMovie) -> Unit,
    onPlay: (XtreamCatalog, XtreamMovie) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var appliedQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(VodSortOption.ALPHABETICAL) }
    var filterTopRated by remember { mutableStateOf(false) }
    var filterFresh by remember { mutableStateOf(false) }
    var topRatedCount by remember { mutableStateOf(0) }
    var freshCount by remember { mutableStateOf(0) }
    var selectedCategory by remember { mutableStateOf<CatalogCategory?>(null) }
    var selectedMovie by remember { mutableStateOf<CatalogMovie?>(null) }
    var visibleMovies by remember { mutableStateOf<List<CatalogMovie>>(emptyList()) }
    var isCatalogLoading by remember { mutableStateOf(true) }
    val progressByKey = remember(progressEntries) { progressEntries.associateBy(VodProgressEntry::key) }
    val showSourceNames = catalogs.size > 1
    val categories = remember(catalogs, hideAdultContent) {
        catalogs.flatMap { catalog ->
            catalog.movieCategories
                .filter { !hideAdultContent || !isAdultContent(it.name, null) }
                .map { CatalogCategory(catalog.sourceId, catalog.sourceName, it) }
        }.distinctBy { it.sourceId to it.category.id }
    }
    LaunchedEffect(query) {
        if (query.isNotBlank()) delay(250)
        appliedQuery = query.trim()
    }
    LaunchedEffect(catalogs, appliedQuery, selectedCategory, hideAdultContent, maximumAge, blockUnrated, sortOption, filterTopRated, filterFresh) {
        isCatalogLoading = true
        visibleMovies = withContext(Dispatchers.Default) {
            val baseFiltered = catalogs.flatMap { catalog ->
                val categoryNames = catalog.movieCategories.associate { it.id to it.name }
                catalog.movies.asSequence()
                    .filter {
                        selectedCategory == null ||
                            (catalog.sourceId == selectedCategory?.sourceId && it.categoryId == selectedCategory?.category?.id)
                    }
                    .filter {
                        (!hideAdultContent || !isAdultContent(it.name, categoryNames[it.categoryId])) &&
                            ParentalControls.isContentAllowedForAge(it.ageRating, maximumAge, blockUnrated)
                    }
                    .filter { appliedQuery.isBlank() || it.name.contains(appliedQuery, ignoreCase = true) }
                    .map { CatalogMovie(catalog, it) }
                    .toList()
            }
            val hasOmdb = OmdbMetadataResolver.hasAnyCached()
            val scoredItems = baseFiltered.map { entry ->
                val cached = if (hasOmdb) {
                    OmdbMetadataResolver.getCached(context, "movie", entry.movie.name, entry.movie.year, entry.movie.imdbId)
                } else null
                val imdbScore = cached?.imdbRating?.toDoubleOrNull() ?: entry.movie.rating?.toDoubleOrNull() ?: 0.0
                val rtScore = cached?.rottenTomatoes?.removeSuffix("%")?.trim()?.toIntOrNull() ?: 0
                Triple(entry, imdbScore, rtScore)
            }
            topRatedCount = scoredItems.count { it.second >= 7.5 }
            freshCount = scoredItems.count { it.third >= 60 }

            val filtered = scoredItems
                .filter { (!filterTopRated || it.second >= 7.5) && (!filterFresh || it.third >= 60) }
                .let { list ->
                    when (sortOption) {
                        VodSortOption.DEFAULT -> list.map { it.first }
                        VodSortOption.RATING -> list.sortedWith(
                            compareByDescending<Triple<CatalogMovie, Double, Int>> { it.second > 0.0 }
                                .thenByDescending { it.second }
                        ).map { it.first }
                        VodSortOption.YEAR -> list.sortedByDescending { it.first.movie.year?.toIntOrNull() ?: 0 }.map { it.first }
                        VodSortOption.ALPHABETICAL -> list.sortedBy { it.first.movie.name.lowercase() }.map { it.first }
                    }
                }
            filtered
        }
        isCatalogLoading = false
    }
    LaunchedEffect(appliedQuery, selectedCategory, sortOption, filterTopRated, filterFresh) {
        if (gridState.firstVisibleItemIndex > 0) gridState.scrollToItem(0)
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
            gridState.scrollToItem(targetIndex)
        },
        header = {
            ContinueWatchingRow(continueWatching, onContinueWatching, onRemoveContinueWatching)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { filterTopRated = !filterTopRated },
                    shape = RoundedCornerShape(8.dp),
                    color = if (filterTopRated) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, if (filterTopRated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.tvFocusHighlight(shape = RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (filterTopRated) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFFFFB800),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${strings.topRatedFilter} · $topRatedCount",
                            color = if (filterTopRated) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = if (filterTopRated) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                if (freshCount > 0 || filterFresh) {
                    Surface(
                        onClick = { filterFresh = !filterFresh },
                        shape = RoundedCornerShape(8.dp),
                        color = if (filterFresh) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, if (filterFresh) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.tvFocusHighlight(shape = RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🍅", fontSize = 12.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${strings.freshFilter} · $freshCount",
                                color = if (filterFresh) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = if (filterFresh) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    ) {
        gridItemsIndexed(
            visibleMovies,
            key = { _, entry -> "${entry.catalog.sourceId}:${entry.movie.id}" },
            contentType = { _, _ -> "movie_poster" }
        ) { index, entry ->
            val libraryId = entry.movie.libraryId(entry.catalog.sourceId)
            val client = remember(entry.catalog.sourceId) {
                val source = profiles.firstOrNull { it.id == entry.catalog.sourceId }?.source as? ContentSource.Xtream
                source?.let { XtreamClient(context.applicationContext, entry.catalog.sourceId, it) }
            }
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
                loadPosterFallback = if (entry.movie.posterUrl.isNullOrBlank()) {
                    client?.let { c -> { c.loadMovieDetails(entry.movie.id).posterUrl } }
                } else null,
                modifier = if (index == 0) Modifier.focusRequester(contentFocusRequester) else Modifier,
                onClick = { selectedMovie = entry }
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

