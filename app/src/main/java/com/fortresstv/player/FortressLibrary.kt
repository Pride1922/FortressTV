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
import androidx.activity.compose.ReportDrawn
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

internal enum class LibraryDestination { HOME, LIVE_TV, MOVIES, SERIES, FAVORITES }

private fun StartupDestination.toLibraryDestination() = when (this) {
    StartupDestination.HOME -> LibraryDestination.HOME
    StartupDestination.LIVE_TV -> LibraryDestination.LIVE_TV
}

internal data class VodPlaybackRequest(
    val key: String,
    val kind: VodProgressKind,
    val sourceId: String,
    val itemId: String,
    val seriesId: Int? = null,
    val seriesName: String? = null,
    val title: String,
    val posterUrl: String?,
    val containerExtension: String,
    val season: Int? = null,
    val episode: Int? = null,
    val streamUrl: String,
    val tmdbId: String? = null,
    val imdbId: String? = null
)

internal fun XtreamMovie.toPlaybackRequest(sourceId: String, streamUrl: String) = VodPlaybackRequest(
    key = libraryId(sourceId), kind = VodProgressKind.MOVIE, sourceId = sourceId,
    itemId = id.toString(), title = name, posterUrl = posterUrl, containerExtension = containerExtension,
    streamUrl = streamUrl, tmdbId = tmdbId, imdbId = imdbId
)

internal suspend fun resolveVodPlayback(
    entry: VodProgressEntry,
    catalogs: List<XtreamCatalog>,
    profiles: List<PlaylistProfile>,
    context: Context,
    channels: List<PlaylistChannel> = emptyList()
): VodPlaybackRequest? {
    val source = profiles.firstOrNull { it.id == entry.sourceId }?.source as? ContentSource.Xtream ?: return null
    val client = XtreamClient(context.applicationContext, entry.sourceId, source)
    val catalog = catalogs.firstOrNull { it.sourceId == entry.sourceId }
    val movie = if (entry.kind == VodProgressKind.MOVIE) {
        catalog?.movies?.firstOrNull { it.id.toString() == entry.itemId }
    } else null
    val restoredEpisode = if (entry.kind == VodProgressKind.EPISODE && entry.itemId.matches(Regex("[0-9a-f]{64}"))) {
        val seriesId = entry.seriesId ?: return null
        client.loadSeriesDetails(seriesId).episodes.firstOrNull {
            com.fortresstv.player.library.CloudIdentityHasher.hashItemId(it.id) == entry.itemId
        } ?: return null
    } else null
    val itemId = restoredEpisode?.id ?: entry.itemId
    val extension = restoredEpisode?.containerExtension?.ifBlank { entry.containerExtension } ?: entry.containerExtension
    val streamUrl = when (entry.kind) {
        VodProgressKind.MOVIE -> client.movieStreamUrl(movie ?: return null)
        VodProgressKind.EPISODE -> client.episodeStreamUrl(
            XtreamEpisode(
                id = itemId,
                title = entry.title,
                season = entry.season ?: 0,
                episodeNumber = entry.episode ?: 0,
                containerExtension = extension,
                posterUrl = entry.posterUrl,
                plot = null
            )
        )
        VodProgressKind.CATCHUP -> {
            val catchupKey = CatchupProgressKey.fromItemId(entry.itemId)
            val matchedChannel = if (catchupKey != null) {
                channels.firstOrNull { ch ->
                    ch.sourceId == entry.sourceId && (
                        ch.tvgId?.equals(catchupKey.channelId, ignoreCase = true) == true ||
                        ch.name.trim().equals(catchupKey.channelId, ignoreCase = true) ||
                        ch.sourceItemId == catchupKey.channelId
                    )
                }
            } else null
            val rawStreamId = matchedChannel?.sourceItemId?.toIntOrNull()
                ?: entry.seriesId
                ?: catchupKey?.channelId?.toIntOrNull()
                ?: 0
            if (rawStreamId > 0) {
                val durationMins = if (entry.durationMs > 0) (entry.durationMs / 60_000L).toInt().coerceAtLeast(1) else 60
                val startInstant = catchupKey?.programStartUtc ?: java.time.Instant.ofEpochMilli(entry.updatedAtMs)
                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd:HH-mm").withZone(java.time.ZoneOffset.UTC)
                client.timeshiftStreamUrl(rawStreamId, durationMins, formatter.format(startInstant), entry.containerExtension)
            } else {
                matchedChannel?.streamUrl ?: client.liveStreamUrl(rawStreamId, entry.containerExtension)
            }
        }
    }
    val series = if (entry.kind == VodProgressKind.EPISODE && entry.seriesId != null) {
        catalog?.series?.firstOrNull { it.id == entry.seriesId }
    } else null
    val seriesName = series?.name
    return VodPlaybackRequest(
        key = restoredEpisode?.let { "episode:${entry.sourceId}:$itemId" } ?: entry.key,
        kind = entry.kind, sourceId = entry.sourceId, itemId = itemId,
        seriesId = entry.seriesId, seriesName = seriesName, title = entry.title, posterUrl = entry.posterUrl,
        containerExtension = extension, season = restoredEpisode?.season ?: entry.season,
        episode = restoredEpisode?.episodeNumber ?: entry.episode,
        streamUrl = streamUrl, tmdbId = movie?.tmdbId ?: series?.tmdbId, imdbId = movie?.imdbId ?: series?.imdbId
    )
}

internal suspend fun resolveNextEpisodePlayback(
    current: VodPlaybackRequest,
    profiles: List<PlaylistProfile>,
    context: Context
): VodPlaybackRequest? {
    val seriesId = current.seriesId ?: return null
    val source = profiles.firstOrNull { it.id == current.sourceId }?.source as? ContentSource.Xtream ?: return null
    val client = XtreamClient(context.applicationContext, current.sourceId, source)
    val details = client.loadSeriesDetails(seriesId)
    val episodes = details.episodes
    var currentIndex = episodes.indexOfFirst {
        it.id == current.itemId || com.fortresstv.player.library.CloudIdentityHasher.hashItemId(it.id) == current.itemId
    }
    if (currentIndex == -1 && current.season != null && current.episode != null) {
        currentIndex = episodes.indexOfFirst { it.season == current.season && it.episodeNumber == current.episode }
    }
    if (currentIndex == -1) return null
    val next = episodes.getOrNull(currentIndex + 1) ?: return null
    val strings = if (java.util.Locale.getDefault().language.lowercase().startsWith("pt")) {
        com.fortresstv.player.ui.PortugueseStrings
    } else {
        com.fortresstv.player.ui.EnglishStrings
    }
    val episodeTitle = "${strings.episodeLabel(next.season, next.episodeNumber)} · ${next.title}"
    return VodPlaybackRequest(
        key = "episode:${current.sourceId}:${next.id}",
        kind = VodProgressKind.EPISODE,
        sourceId = current.sourceId,
        itemId = next.id,
        seriesId = seriesId,
        seriesName = current.seriesName,
        title = episodeTitle,
        posterUrl = next.posterUrl ?: current.posterUrl,
        containerExtension = next.containerExtension,
        season = next.season,
        episode = next.episodeNumber,
        streamUrl = client.episodeStreamUrl(next),
        tmdbId = details.tmdbId ?: current.tmdbId,
        imdbId = details.imdbId ?: current.imdbId
    )
}

internal suspend fun resolveAllSeriesEpisodesPlayback(
    current: VodPlaybackRequest,
    profiles: List<PlaylistProfile>,
    context: Context
): List<VodPlaybackRequest> {
    val seriesId = current.seriesId ?: return emptyList()
    val source = profiles.firstOrNull { it.id == current.sourceId }?.source as? ContentSource.Xtream ?: return emptyList()
    val client = XtreamClient(context.applicationContext, current.sourceId, source)
    val details = client.loadSeriesDetails(seriesId)
    val episodes = details.episodes
    val strings = if (java.util.Locale.getDefault().language.lowercase().startsWith("pt")) {
        com.fortresstv.player.ui.PortugueseStrings
    } else {
        com.fortresstv.player.ui.EnglishStrings
    }
    return episodes.map { episode ->
        VodPlaybackRequest(
            key = "episode:${current.sourceId}:${episode.id}",
            kind = VodProgressKind.EPISODE,
            sourceId = current.sourceId,
            itemId = episode.id,
            seriesId = seriesId,
            seriesName = current.seriesName,
            title = "${strings.episodeLabel(episode.season, episode.episodeNumber)} · ${episode.title}",
            posterUrl = episode.posterUrl ?: current.posterUrl,
            containerExtension = episode.containerExtension,
            season = episode.season,
            episode = episode.episodeNumber,
            streamUrl = client.episodeStreamUrl(episode),
            tmdbId = details.tmdbId ?: current.tmdbId,
            imdbId = details.imdbId ?: current.imdbId
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun <T> loadHomeShelf(visible: Boolean, vararg inputs: Any?, load: () -> List<T>): List<T> {
    val shelf = remember { mutableStateOf(emptyList<T>()) }
    val lastLoadedInputs = remember { mutableStateOf<List<Any?>?>(null) }
    val inputKey = inputs.toList()
    LaunchedEffect(visible, *inputs) {
        if (visible && lastLoadedInputs.value != inputKey) {
            shelf.value = withContext(Dispatchers.Default) { load() }
            lastLoadedInputs.value = inputKey
        }
    }
    return shelf.value
}

@Composable
internal fun FortressLibrary(
    playlist: Playlist,
    activeProfile: PlaylistProfile,
    startupDestination: StartupDestination = StartupDestination.HOME,
    startupNavigationRequest: StartupDestination? = null,
    onStartupNavigationHandled: () -> Unit = {},
    enabledSourceCount: Int,
    enabledSources: List<PlaylistProfile>,
    epgGuide: EpgGuide?,
    epgSourceName: String?,
    epgTimeShiftHours: Int,
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
    openSubtitlesSettings: OpenSubtitlesSettings = OpenSubtitlesSettings(),
    hideAdultContent: Boolean = false,
    onHideAdultContentChanged: ((Boolean) -> Unit)? = null,
    loading: Boolean,
    error: String?,
    userLibrary: UserLibraryStore,
    progressStore: VodProgressStore,
    traktNamespace: String,
    favoriteChannelIds: Set<String>,
    recentChannelIds: List<String>,
    hiddenCategories: Set<String> = emptySet(),
    categoryOrder: List<String> = emptyList(),
    onCategoriesChanged: (Set<String>, List<String>) -> Unit = { _, _ -> },
    onRecordChannelWatched: (PlaylistChannel) -> Unit,
    movieWatchlistIds: Set<String>,
    followedSeriesIds: Set<String>,
    watchedItemIds: Set<String>,
    homeHiddenSections: Set<String> = emptySet(),
    homeSectionOrder: List<String> = com.fortresstv.player.ui.HomeSectionId.defaultOrder,
    hiddenSportsFilters: Set<String> = emptySet(),
    onToggleSportsFilter: ((String) -> Unit)? = null,
    onOpenCustomizeHome: () -> Unit = {},
    xtreamCatalogs: List<XtreamCatalog>,
    xtreamProfiles: List<PlaylistProfile>,
    onToggleFavorite: (PlaylistChannel) -> Unit,
    onToggleMovieWatchlist: (String, XtreamMovie) -> Unit,
    onToggleMovieWatched: (String, XtreamMovie) -> Unit,
    onToggleSeriesFollow: (String, XtreamSeries) -> Unit,
    onToggleEpisodeWatched: (String, XtreamEpisode, XtreamSeries?) -> Unit,
    onMarkItemWatched: (String, Boolean) -> Unit,
    onRetryLoad: (() -> Unit)?,
    maximumAge: Int? = null,
    blockUnrated: Boolean = false,
    activeHouseholdProfile: HouseholdProfile? = null,
    onOpenProfiles: (() -> Unit)? = null,
    profileSwitcher: com.fortresstv.player.profile.ProfileSwitcher? = null,
    onOpenSettings: (Int) -> Unit,
    onSaveMapping: (PlaylistChannel, com.fortresstv.player.epg.EpgChannel, String?) -> Unit = { _, _, _ -> },
    onResetMapping: (PlaylistChannel, String?) -> Unit = { _, _ -> },
    onRequestAdultAuth: (((String?) -> Unit)) -> Unit = {},
    progressRefreshTrigger: Int = 0
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var progressEntries by remember { mutableStateOf(emptyList<VodProgressEntry>()) }
    LaunchedEffect(activeHouseholdProfile?.id, progressRefreshTrigger) {
        withContext(Dispatchers.IO) {
            progressStore.entries()
        }.let {
            progressEntries = it
        }
    }
    var destination by rememberSaveable { mutableStateOf(startupDestination.toLibraryDestination()) }
    LaunchedEffect(startupNavigationRequest) {
        if (startupNavigationRequest != null) {
            destination = startupNavigationRequest.toLibraryDestination()
            onStartupNavigationHandled()
        }
    }
    var requestedChannel by remember { mutableStateOf<PlaylistChannel?>(null) }
    var requestedChannelFullscreen by remember { mutableStateOf(false) }
    var vodPlayback by remember { mutableStateOf<VodPlaybackRequest?>(null) }
    var openHomeSearch by remember { mutableStateOf(false) }
    // Keep catalogue positions and tab state alive while the user moves between primary destinations.
    val homeListState = rememberLazyListState()
    val movieGridState = rememberLazyGridState()
    val seriesGridState = rememberLazyGridState()
    val movieContinueWatching = remember(progressEntries) {
        progressEntries.filter { it.kind == VodProgressKind.MOVIE }
    }
    val seriesContinueWatching = remember(progressEntries) {
        progressEntries.filter { it.kind == VodProgressKind.EPISODE }
    }
    val shelvesState = remember(traktNamespace, scope) {
        HomeShelvesStateHolder(context.applicationContext, traktNamespace, scope)
    }
    val seriesTabState = remember(traktNamespace) { SeriesTabState() }
    val moviesTabState = remember(traktNamespace) { MoviesTabState() }
    val sharedTraktConnected = shelvesState.traktConnected
    LaunchedEffect(shelvesState, sharedTraktConnected) {
        shelvesState.ensureCoreTraktSynced()
    }
    LaunchedEffect(shelvesState, sharedTraktConnected, xtreamCatalogs, hideAdultContent, maximumAge, blockUnrated, shelvesState.traktRecItems) {
        shelvesState.recomputeRecommendations(
            showTraktRecs = sharedTraktConnected,
            catalogs = xtreamCatalogs,
            hideAdultContent = hideAdultContent,
            maximumAge = maximumAge,
            blockUnrated = blockUnrated
        )
    }

    val effectivePlaylist = remember(playlist, hideAdultContent) {
        if (!hideAdultContent) playlist
        else {
            val filtered = playlist.channels.filterNot { isAdultContent(it.name, it.group) }
            Playlist(filtered)
        }
    }
    val isTv = LocalFormFactor.current.isTelevision
    val compactHeader = !isTv && LocalConfiguration.current.screenWidthDp < 600
    var headerHasFocus by remember { mutableStateOf(false) }
    val homeFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isTv, destination) {
        if (isTv && !openHomeSearch) {
            delay(150)
            try {
                homeFocusRequester.requestFocus()
            } catch (_: Throwable) {}
        }
    }

    BackHandler(destination != LibraryDestination.HOME || (isTv && !headerHasFocus)) {
        if (destination != LibraryDestination.HOME) destination = LibraryDestination.HOME
        else runCatching { homeFocusRequester.requestFocus() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(FortressBackgroundBrush)
            .systemBarsPadding()
    ) {
        if (destination != LibraryDestination.HOME) {
            LibraryHomeHeader(
                onHome = { destination = LibraryDestination.HOME },
                onSwitchProfile = onOpenProfiles,
                profileSwitcher = profileSwitcher,
                onSettings = { onOpenSettings(0) },
                onCloseApp = { context.findActivity()?.finishAndRemoveTask() },
                profile = activeHouseholdProfile,
                modifier = Modifier.padding(horizontal = if (compactHeader) 12.dp else 20.dp, vertical = if (compactHeader) 6.dp else 8.dp),
                homeModifier = Modifier.focusRequester(homeFocusRequester).focusProperties { down = contentFocusRequester }
            ) {
                LibrarySearchButton(onClick = {
                    openHomeSearch = true
                    destination = LibraryDestination.HOME
                })
            }
        }
        val onResumeEntry: (VodProgressEntry) -> Unit = { entry ->
            scope.launch {
                val request = try {
                    resolveVodPlayback(entry, xtreamCatalogs, xtreamProfiles, context, playlist.channels)
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
                if (request != null) vodPlayback = request
                else Toast.makeText(context, strings.continueWatchingUnavailable, Toast.LENGTH_SHORT).show()
            }
        }
        val onRemoveContinueWatchingEntry: (VodProgressEntry) -> Unit = { entry ->
            progressEntries = progressStore.remove(entry.key, entry.seriesId)
            if (activeHouseholdProfile != null && activeHouseholdProfile.id != "restricted_default") {
                val cloudSync = com.fortresstv.player.library.CloudLibrarySyncService.get(context)
                cloudSync.recordProgressMutation(
                    profileId = activeHouseholdProfile.id,
                    sourceId = entry.sourceId,
                    itemKind = entry.kind.wireValue,
                    itemId = entry.itemId,
                    positionMs = 0L,
                    durationMs = entry.durationMs,
                    completed = false,
                    isDeleted = true,
                    seriesId = entry.seriesId,
                    season = entry.season,
                    episode = entry.episode
                )
            }
        }

        fun showHomeSection(section: String): Boolean =
            destination == LibraryDestination.HOME && section !in homeHiddenSections

        val homeFavoriteChannels = loadHomeShelf(
            showHomeSection(com.fortresstv.player.ui.HomeSectionId.FAVORITE_CHANNELS),
            effectivePlaylist.channels, favoriteChannelIds
        ) {
            HomeLibraryShelfBuilder.buildFavoriteChannels(effectivePlaylist.channels, favoriteChannelIds)
        }

        val homeRecentChannels = loadHomeShelf(
            showHomeSection(com.fortresstv.player.ui.HomeSectionId.RECENT_CHANNELS),
            effectivePlaylist.channels, recentChannelIds
        ) {
            HomeLibraryShelfBuilder.buildRecentChannels(effectivePlaylist.channels, recentChannelIds)
        }

        val homeFollowedSeries = loadHomeShelf(
            showHomeSection(com.fortresstv.player.ui.HomeSectionId.FOLLOWED_SERIES),
            xtreamCatalogs, followedSeriesIds, hideAdultContent, maximumAge, blockUnrated
        ) {
            HomeLibraryShelfBuilder.buildFollowedSeries(
                catalogs = xtreamCatalogs,
                followedSeriesIds = followedSeriesIds,
                hideAdultContent = hideAdultContent,
                maximumAge = maximumAge,
                blockUnrated = blockUnrated
            )
        }

        val homeOmdbSettings = LocalOmdbSettings.current
        LaunchedEffect(destination, progressEntries, homeFollowedSeries, homeOmdbSettings) {
            if (destination == LibraryDestination.HOME && homeOmdbSettings.enabled && homeOmdbSettings.apiKey.isNotBlank()) {
                val targets = mutableListOf<com.fortresstv.player.omdb.OmdbPreFetchTarget>()
                // 1. Continue watching items (most recent first)
                progressEntries.take(20).forEach { entry ->
                    val type = if (entry.kind == VodProgressKind.EPISODE) "series" else "movie"
                    targets.add(com.fortresstv.player.omdb.OmdbPreFetchTarget(type = type, title = entry.title))
                }
                // 2. Followed series
                homeFollowedSeries.take(15).forEach { entry ->
                    targets.add(com.fortresstv.player.omdb.OmdbPreFetchTarget(type = "series", title = entry.series.name, year = entry.series.year, imdbId = entry.series.imdbId))
                }
                OmdbMetadataResolver.prefetchCuratedBatch(context, homeOmdbSettings, targets)
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth().testTag("startup_destination_ready")) {
            ReportDrawn()
            when (destination) {
                LibraryDestination.HOME -> HomeScreen(
                    traktNamespace = traktNamespace,
                    channels = effectivePlaylist.channels,
                    catalogs = xtreamCatalogs,
                    profiles = xtreamProfiles,
                    homeFocusRequester = homeFocusRequester,
                    onHeaderFocusChanged = { headerHasFocus = it },
                    onSwitchProfile = onOpenProfiles,
                    profileSwitcher = profileSwitcher,
                    onOpenSettings = { onOpenSettings(0) },
                    onCloseApp = { context.findActivity()?.finishAndRemoveTask() },
                    openSearchOnEntry = openHomeSearch,
                    onSearchOpened = { openHomeSearch = false },
                    epgGuide = epgGuide,
                    epgRepository = epgRepository,
                    epgTimeShiftHours = epgTimeShiftHours,
                    epgScheduleCorrectionMinutes = epgScheduleCorrectionMinutes,
                    epgMappings = epgMappings,
                    favoriteChannels = homeFavoriteChannels,
                    recentChannelsList = homeRecentChannels,
                    followedSeries = homeFollowedSeries,
                    followedSeriesIds = followedSeriesIds,
                    watchedItemIds = watchedItemIds,
                    movieWatchlistIds = movieWatchlistIds,
                    progressEntries = progressEntries,
                    hideAdultContent = hideAdultContent,
                    maximumAge = maximumAge,
                    blockUnrated = blockUnrated,
                    onContinueWatching = onResumeEntry,
                    onRemoveContinueWatching = onRemoveContinueWatchingEntry,
                    onToggleMovieWatchlist = onToggleMovieWatchlist,
                    onToggleMovieWatched = { sourceId, movie ->
                        val key = movie.libraryId(sourceId)
                        val willBeWatched = key !in watchedItemIds
                        if (willBeWatched) progressEntries = progressStore.remove(key)
                        onToggleMovieWatched(sourceId, movie)
                    },
                    onToggleSeriesFollow = onToggleSeriesFollow,
                    onToggleEpisodeWatched = { sourceId, ep, series ->
                        val key = ep.libraryId(sourceId)
                        val willBeWatched = key !in watchedItemIds
                        if (willBeWatched) progressEntries = progressStore.remove(key)
                        onToggleEpisodeWatched(sourceId, ep, series)
                    },
                    onPlayMovie = { catalog, movie ->
                        val source = xtreamProfiles.firstOrNull { it.id == catalog.sourceId }?.source as? ContentSource.Xtream
                        if (source != null) {
                            val url = XtreamClient(context.applicationContext, catalog.sourceId, source).movieStreamUrl(movie)
                            vodPlayback = movie.toPlaybackRequest(catalog.sourceId, url)
                        }
                    },
                    onPlayEpisode = { catalog, series, episode, url ->
                        vodPlayback = VodPlaybackRequest(
                            key = "episode:${catalog.sourceId}:${episode.id}", kind = VodProgressKind.EPISODE,
                            sourceId = catalog.sourceId, itemId = episode.id, seriesId = series.id,
                            seriesName = series.name,
                            title = "${strings.episodeLabel(episode.season, episode.episodeNumber)} · ${episode.title}",
                            posterUrl = episode.posterUrl ?: series.coverUrl, containerExtension = episode.containerExtension,
                            season = episode.season, episode = episode.episodeNumber, streamUrl = url,
                            tmdbId = series.tmdbId, imdbId = series.imdbId
                        )
                    },
                    onWatchChannel = { channel ->
                        requestedChannelFullscreen = true
                        requestedChannel = channel
                        destination = LibraryDestination.LIVE_TV
                    },
                    onNavigateToDestination = { target ->
                        destination = target
                    },
                    homeHiddenSections = homeHiddenSections,
                    homeSectionOrder = homeSectionOrder,
                    hiddenSportsFilters = hiddenSportsFilters,
                    onToggleSportsFilter = onToggleSportsFilter,
                    onOpenCustomizeHome = onOpenCustomizeHome,
                    sharedShelvesState = shelvesState,
                    homeListState = homeListState
                )
                LibraryDestination.LIVE_TV -> ChannelBrowser(
                    playlist = if (activeHouseholdProfile?.isChild == true) effectivePlaylist else playlist,
                    activeProfile = activeProfile,
                    contentFocusRequester = contentFocusRequester,
                    enabledSourceCount = enabledSourceCount,
                    enabledSources = enabledSources,
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
                    loading = loading,
                    error = error,
                    onRetryLoad = onRetryLoad,
                    favoriteChannelIds = favoriteChannelIds,
                    recentChannelIds = recentChannelIds,
                    onRecordChannelWatched = onRecordChannelWatched,
                    hiddenCategories = hiddenCategories,
                    categoryOrder = categoryOrder,
                    hideAdultContent = hideAdultContent,
                    onToggleCategoryHidden = { category ->
                        val targetSourceId = playlist.channels.firstOrNull { it.group == category }?.sourceId ?: activeProfile.id
                        val isAdult = isAdultContent(null, category)
                        val isHidden = category in hiddenCategories || (hideAdultContent && isAdult)
                        val updatedHidden = if (isHidden) {
                            hiddenCategories - category
                        } else {
                            hiddenCategories + category
                        }
                        userLibrary.setCategoryHidden(targetSourceId, category, !isHidden)
                        if (isAdult && isHidden && hideAdultContent) {
                            onHideAdultContentChanged?.invoke(false)
                        }
                        onCategoriesChanged(updatedHidden, categoryOrder)
                    },
                    onMoveCategory = { category, moveUp ->
                        val targetSourceId = playlist.channels.firstOrNull { it.group == category }?.sourceId ?: activeProfile.id
                        val list = (if (categoryOrder.isEmpty()) playlist.groups else categoryOrder).toMutableList()
                        if (category !in list) list.add(category)
                        val index = list.indexOf(category)
                        if (moveUp && index > 0) {
                            list.removeAt(index)
                            list.add(index - 1, category)
                        } else if (!moveUp && index < list.size - 1) {
                            list.removeAt(index)
                            list.add(index + 1, category)
                        }
                        val updatedOrder = userLibrary.setCategoryOrder(targetSourceId, list)
                        onCategoriesChanged(hiddenCategories, updatedOrder)
                    },
                    onUnhideAllCategories = {
                        val updatedHidden = userLibrary.unhideAllCategories(activeProfile.id)
                        onCategoriesChanged(updatedHidden, categoryOrder)
                    },
                    onHideAllCategories = { cats ->
                        val updatedHidden = userLibrary.hideAllCategories(activeProfile.id, cats)
                        onCategoriesChanged(updatedHidden, categoryOrder)
                    },
                    onResetCategoryOrder = {
                        val updatedOrder = userLibrary.resetCategoryOrder(activeProfile.id)
                        onCategoriesChanged(hiddenCategories, updatedOrder)
                    },
                    onToggleFavorite = onToggleFavorite,
                    requestedChannel = requestedChannel,
                    requestedChannelFullscreen = requestedChannelFullscreen,
                    onRequestedChannelHandled = {
                        requestedChannel = null
                        requestedChannelFullscreen = false
                    },
                    onOpenSettings = onOpenSettings,
                    onSaveMapping = onSaveMapping,
                    onResetMapping = onResetMapping,
                    onRequestAdultAuth = onRequestAdultAuth,
                    progressEntries = progressEntries,
                    onWatchCatchup = { channel, programme ->
                        val source = enabledSources.firstOrNull { it.id == channel.sourceId }?.source as? ContentSource.Xtream
                        val streamId = channel.sourceItemId.toIntOrNull() ?: 0
                        val durationMs = ((programme.stopEpochMilli ?: (programme.startEpochMilli + 3600_000L)) - programme.startEpochMilli).coerceAtLeast(60_000L)
                        val durationMinutes = (durationMs / 60_000L).toInt().coerceAtLeast(1)
                        val instant = java.time.Instant.ofEpochMilli(programme.startEpochMilli)
                        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd:HH-mm").withZone(java.time.ZoneOffset.UTC)
                        val streamUrl = if (source != null && streamId > 0) {
                            val client = XtreamClient(context.applicationContext, channel.sourceId, source)
                            client.timeshiftStreamUrl(streamId, durationMinutes, formatter.format(instant), "ts")
                        } else {
                            channel.streamUrl
                        }
                        val canonicalChannel = channel.tvgId?.trim()?.takeIf { it.isNotEmpty() }
                            ?: channel.name.trim().lowercase().takeIf { it.isNotEmpty() }
                            ?: channel.sourceItemId
                        val catchupKey = CatchupProgressKey(canonicalChannel, instant)
                        vodPlayback = VodPlaybackRequest(
                            key = "catchup:${channel.sourceId}:${canonicalChannel}:${programme.startEpochMilli}",
                            kind = VodProgressKind.CATCHUP,
                            sourceId = channel.sourceId,
                            itemId = catchupKey.toItemId(),
                            seriesId = streamId,
                            title = "${channel.name} · ${programme.title}",
                            posterUrl = channel.logoUrl,
                            containerExtension = "ts",
                            streamUrl = streamUrl
                        )
                    }
                )
                LibraryDestination.MOVIES -> XtreamMoviesScreen(
                    catalogs = xtreamCatalogs,
                    profiles = xtreamProfiles,
                    contentFocusRequester = contentFocusRequester,
                    continueWatching = movieContinueWatching,
                    progressEntries = progressEntries,
                    watchedItemIds = watchedItemIds,
                    onContinueWatching = onResumeEntry,
                    onRemoveContinueWatching = onRemoveContinueWatchingEntry,
                    hideAdultContent = hideAdultContent,
                    maximumAge = maximumAge,
                    blockUnrated = blockUnrated,
                    gridState = movieGridState,
                    movieWatchlistIds = movieWatchlistIds,
                    traktNamespace = traktNamespace,
                    onToggleWatchlist = onToggleMovieWatchlist,
                    onToggleMovieWatched = { sourceId, movie ->
                        val key = movie.libraryId(sourceId)
                        val willBeWatched = key !in watchedItemIds
                        if (willBeWatched) progressEntries = progressStore.remove(key)
                        onToggleMovieWatched(sourceId, movie)
                    },
                    onPlay = { catalog, movie ->
                        val source = xtreamProfiles.firstOrNull { it.id == catalog.sourceId }?.source as? ContentSource.Xtream
                        if (source != null) {
                            val url = XtreamClient(context.applicationContext, catalog.sourceId, source).movieStreamUrl(movie)
                            vodPlayback = movie.toPlaybackRequest(catalog.sourceId, url)
                        }
                    },
                    tabState = moviesTabState,
                    sharedRecommendedMovies = shelvesState.matchedTraktRecs.movies,
                    sharedTraktConnected = sharedTraktConnected
                )
                LibraryDestination.SERIES -> XtreamSeriesScreen(
                    catalogs = xtreamCatalogs,
                    profiles = xtreamProfiles,
                    contentFocusRequester = contentFocusRequester,
                    hideAdultContent = hideAdultContent,
                    maximumAge = maximumAge,
                    blockUnrated = blockUnrated,
                    gridState = seriesGridState,
                    followedSeriesIds = followedSeriesIds,
                    watchedItemIds = watchedItemIds,
                    traktNamespace = traktNamespace,
                    continueWatching = seriesContinueWatching,
                    progressEntries = progressEntries,
                    onContinueWatching = onResumeEntry,
                    onRemoveContinueWatching = onRemoveContinueWatchingEntry,
                    onToggleFollow = onToggleSeriesFollow,
                    onToggleEpisodeWatched = { sourceId, ep, series ->
                        val key = ep.libraryId(sourceId)
                        val willBeWatched = key !in watchedItemIds
                        if (willBeWatched) progressEntries = progressStore.remove(key)
                        onToggleEpisodeWatched(sourceId, ep, series)
                    },
                    onPlay = { catalog, series, episode, url ->
                        vodPlayback = VodPlaybackRequest(
                            key = "episode:${catalog.sourceId}:${episode.id}",
                            kind = VodProgressKind.EPISODE,
                            sourceId = catalog.sourceId,
                            itemId = episode.id,
                            seriesId = series.id,
                            seriesName = series.name,
                            title = "${strings.episodeLabel(episode.season, episode.episodeNumber)} · ${episode.title}",
                            posterUrl = episode.posterUrl ?: series.coverUrl,
                            containerExtension = episode.containerExtension,
                            season = episode.season,
                            episode = episode.episodeNumber,
                            streamUrl = url,
                            tmdbId = series.tmdbId,
                            imdbId = series.imdbId
                        )
                    },
                    tabState = seriesTabState,
                    sharedRecommendedSeries = shelvesState.matchedTraktRecs.series,
                    sharedTraktConnected = sharedTraktConnected
                )
                LibraryDestination.FAVORITES -> FavoriteLibraryScreen(
                    channels = homeFavoriteChannels,
                    catalogs = xtreamCatalogs,
                    profiles = xtreamProfiles,
                    userLibrary = userLibrary,
                    contentFocusRequester = contentFocusRequester,
                    epgGuide = epgGuide,
                    epgTimeShiftHours = epgTimeShiftHours,
                    epgScheduleCorrectionMinutes = epgScheduleCorrectionMinutes,
                    epgMappings = epgMappings,
                    favoriteChannelIds = favoriteChannelIds,
                    movieWatchlistIds = movieWatchlistIds,
                    followedSeriesIds = followedSeriesIds,
                    watchedItemIds = watchedItemIds,
                    progressEntries = progressEntries,
                    hideAdultContent = hideAdultContent,
                    maximumAge = maximumAge,
                    blockUnrated = blockUnrated,
                    onToggleFavorite = onToggleFavorite,
                    onToggleMovieWatchlist = onToggleMovieWatchlist,
                    onToggleMovieWatched = { sourceId, movie ->
                        val key = movie.libraryId(sourceId)
                        val willBeWatched = key !in watchedItemIds
                        if (willBeWatched) progressEntries = progressStore.remove(key)
                        onToggleMovieWatched(sourceId, movie)
                    },
                    onToggleSeriesFollow = onToggleSeriesFollow,
                    onToggleEpisodeWatched = { sourceId, ep, series ->
                        val key = ep.libraryId(sourceId)
                        val willBeWatched = key !in watchedItemIds
                        if (willBeWatched) progressEntries = progressStore.remove(key)
                        onToggleEpisodeWatched(sourceId, ep, series)
                    },
                    onPlayMovie = { catalog, movie ->
                        val source = xtreamProfiles.firstOrNull { it.id == catalog.sourceId }?.source as? ContentSource.Xtream
                        if (source != null) {
                            val url = XtreamClient(context.applicationContext, catalog.sourceId, source).movieStreamUrl(movie)
                            vodPlayback = movie.toPlaybackRequest(catalog.sourceId, url)
                        }
                    },
                    onPlayEpisode = { catalog, series, episode, url ->
                        vodPlayback = VodPlaybackRequest(
                            key = "episode:${catalog.sourceId}:${episode.id}", kind = VodProgressKind.EPISODE,
                            sourceId = catalog.sourceId, itemId = episode.id, seriesId = series.id,
                            seriesName = series.name,
                            title = "${strings.episodeLabel(episode.season, episode.episodeNumber)} · ${episode.title}",
                            posterUrl = episode.posterUrl ?: series.coverUrl, containerExtension = episode.containerExtension,
                            season = episode.season, episode = episode.episodeNumber, streamUrl = url,
                            tmdbId = series.tmdbId, imdbId = series.imdbId
                        )
                    },
                    onWatch = { channel ->
                        requestedChannel = channel
                        destination = LibraryDestination.LIVE_TV
                    }
                )
            }
        }
        vodPlayback?.let { request ->
            val previous = progressEntries.firstOrNull { it.key == request.key } ?: progressStore.entries().firstOrNull { it.key == request.key }
            VodPlayerDialog(
                traktNamespace = traktNamespace,
                request = request,
                initialPositionMs = previous?.positionMs ?: 0L,
                preferredAudioLanguage = preferredAudioLanguage,
                preferredSubtitleLanguage = preferredSubtitleLanguage,
                subtitleFallbackEnglish = subtitleFallbackEnglish,
                onPreferredAudioLanguageChanged = onPreferredAudioLanguageChanged,
                onPreferredSubtitleLanguageChanged = onPreferredSubtitleLanguageChanged,
                openSubtitlesSettings = openSubtitlesSettings,
                loadNextEpisode = if (request.kind == VodProgressKind.EPISODE) {
                    suspend { resolveNextEpisodePlayback(request, xtreamProfiles, context) }
                } else null,
                loadAllEpisodes = if (request.kind == VodProgressKind.EPISODE) {
                    suspend { resolveAllSeriesEpisodesPlayback(request, xtreamProfiles, context) }
                } else null,
                onPlayNext = { next -> vodPlayback = next },
                onSaveProgress = { positionMs, durationMs ->
                    val isCompleted = durationMs > 0 && positionMs >= durationMs * 0.95
                    if (isCompleted) {
                        onMarkItemWatched(request.key, true)
                    }
                    progressEntries = progressStore.save(
                        VodProgressEntry(
                            key = request.key, kind = request.kind, sourceId = request.sourceId,
                            itemId = request.itemId, seriesId = request.seriesId, title = request.title,
                            posterUrl = request.posterUrl, containerExtension = request.containerExtension,
                            season = request.season, episode = request.episode, positionMs = positionMs,
                            durationMs = durationMs, updatedAtMs = System.currentTimeMillis()
                        )
                    )
                    if (activeHouseholdProfile != null && activeHouseholdProfile.id != "restricted_default") {
                        val cloudSync = com.fortresstv.player.library.CloudLibrarySyncService.get(context)
                        cloudSync.recordProgressMutation(
                            profileId = activeHouseholdProfile.id,
                            sourceId = request.sourceId,
                            itemKind = request.kind.wireValue,
                            itemId = request.itemId,
                            positionMs = positionMs,
                            durationMs = durationMs,
                            completed = isCompleted,
                            seriesId = request.seriesId,
                            season = request.season,
                            episode = request.episode
                        )
                    }
                    if (isCompleted && request.kind == VodProgressKind.EPISODE && request.seriesId != null) {
                        scope.launch(Dispatchers.IO) {
                            val nextRequest = resolveNextEpisodePlayback(request, xtreamProfiles, context)
                            if (nextRequest != null) {
                                val seriesName = request.seriesName
                                val formattedTitle = if (!seriesName.isNullOrBlank() && !nextRequest.title.startsWith(seriesName)) {
                                    "$seriesName · ${nextRequest.title}"
                                } else nextRequest.title
                                val nextEntry = VodProgressEntry(
                                    key = nextRequest.key,
                                    kind = VodProgressKind.EPISODE,
                                    sourceId = nextRequest.sourceId,
                                    itemId = nextRequest.itemId,
                                    seriesId = nextRequest.seriesId,
                                    title = formattedTitle,
                                    posterUrl = nextRequest.posterUrl ?: request.posterUrl,
                                    containerExtension = nextRequest.containerExtension.ifBlank { "mp4" },
                                    season = nextRequest.season,
                                    episode = nextRequest.episode,
                                    positionMs = 0L,
                                    durationMs = 0L,
                                    updatedAtMs = System.currentTimeMillis()
                                )
                                withContext(Dispatchers.Main) {
                                    progressEntries = progressStore.saveNextEpisode(nextEntry)
                                }
                                if (activeHouseholdProfile != null && activeHouseholdProfile.id != "restricted_default") {
                                    val cloudSync = com.fortresstv.player.library.CloudLibrarySyncService.get(context)
                                    cloudSync.recordProgressMutation(
                                        profileId = activeHouseholdProfile.id,
                                        sourceId = nextRequest.sourceId,
                                        itemKind = nextRequest.kind.wireValue,
                                        itemId = nextRequest.itemId,
                                        positionMs = 0L,
                                        durationMs = 0L,
                                        completed = false,
                                        seriesId = nextEntry.seriesId,
                                        season = nextEntry.season,
                                        episode = nextEntry.episode
                                    )
                                }
                            }
                        }
                    }
                },
                onDismiss = { vodPlayback = null }
            )
        }
    }
}


