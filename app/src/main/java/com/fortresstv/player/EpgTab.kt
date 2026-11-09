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
internal fun EpgTab(
    epgSource: ContentSource?,
    epgSourceName: String?,
    epgLastUpdate: Long,
    epgTimeShiftHours: Int,
    epgScheduleCorrectionMinutes: Int = epgTimeShiftHours * 60,
    epgSourceTimezone: String = "",
    epgLoadedDays: Int = 3,
    playlist: Playlist?,
    epgGuide: EpgGuide?,
    epgMappings: Map<String, EpgChannelMapping> = emptyMap(),
    hideAdultContent: Boolean = false,
    hiddenCategories: Set<String> = emptySet(),
    loading: Boolean,
    onEpgUrl: () -> Unit,
    onEpgFile: () -> Unit,
    onRefreshEpg: () -> Unit,
    onRemoveEpg: () -> Unit,
    onEpgScheduleCorrectionChanged: (Int) -> Unit = {},
    onEpgSourceTimezoneChanged: (String) -> Unit = {},
    onSaveMapping: (PlaylistChannel, com.fortresstv.player.epg.EpgChannel, String?) -> Unit = { _, _, _ -> },
    onResetMapping: (PlaylistChannel, String?) -> Unit = { _, _ -> },
    onRequestAdultAuth: (((String?) -> Unit)) -> Unit = {},
    onEpgTimeShiftChanged: (Int) -> Unit = {},
    onEpgLoadedDaysChanged: (Int) -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val effectiveChannels = remember(playlist, hideAdultContent, hiddenCategories) {
        if (playlist == null) emptyList()
        else {
            val base = if (hiddenCategories.isEmpty()) playlist.channels else playlist.channels.filter { (it.group.orEmpty()) !in hiddenCategories }
            if (hideAdultContent) base.filterNot { isAdultContent(it.name, it.group) } else base
        }
    }
    val matchedCount = remember(effectiveChannels, epgGuide, epgMappings) {
        if (epgGuide == null) 0 else effectiveChannels.count {
            val manual = epgMappings["${it.sourceId}:${it.sourceItemId}"]?.xmltvChannelId
            epgGuide.resolveChannelId(it.tvgId, it.name, manual) != null
        }
    }
    val unmatchedChannels = remember(effectiveChannels, epgGuide, epgMappings) {
        if (epgGuide == null) effectiveChannels
        else effectiveChannels.filter {
            val manual = epgMappings["${it.sourceId}:${it.sourceItemId}"]?.xmltvChannelId
            epgGuide.resolveChannelId(it.tvgId, it.name, manual) == null
        }
    }
    var showUnmatchedInSettings by remember { mutableStateOf(false) }

    val currentUnmatched by rememberUpdatedState(unmatchedChannels)
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            val channelsToExport = currentUnmatched
            val text = generateUnmatchedChannelsText(channelsToExport)
            val success = writeTextToUri(context, uri, text)
            if (success) {
                Toast.makeText(context, "${strings.channelsListSaved} (${channelsToExport.size})", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Error: could not write to selected location", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun shareList() {
        val text = generateUnmatchedChannelsText(currentUnmatched)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_TITLE, strings.unmatchedEpgChannels)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, strings.shareChannelsList)
        context.startActivity(shareIntent)
    }

    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // EPG Source
        SettingsGroup(title = strings.xmltvSource) {
            if (isCompact) {
                Column(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (epgSource is ContentSource.RemoteUrl) Icons.Default.Link else Icons.Default.DateRange,
                            contentDescription = null,
                            tint = if (epgSource != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                epgSourceName ?: strings.noEpgConfiguredMsg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (epgSource != null) {
                                Text(
                                    formatLastUpdated(epgLastUpdate, strings),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (epgSource is ContentSource.RemoteUrl) {
                            IconButton(onClick = onRefreshEpg, enabled = !loading, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = strings.refresh, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                        if (epgSource != null) {
                            IconButton(onClick = onRemoveEpg, enabled = !loading, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = strings.removeEpg, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onEpgUrl,
                            enabled = !loading,
                            modifier = Modifier.weight(1f).height(34.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (epgSource is ContentSource.RemoteUrl) strings.edit else strings.remoteUrl, fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = onEpgFile,
                            enabled = !loading,
                            modifier = Modifier.weight(1f).height(34.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (epgSource is ContentSource.LocalFile) strings.edit else strings.localFile, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (epgSource is ContentSource.RemoteUrl) Icons.Default.Link else Icons.Default.DateRange,
                        contentDescription = null,
                        tint = if (epgSource != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            epgSourceName ?: strings.noEpgConfiguredMsg,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (epgSource != null) {
                            Text(
                                formatLastUpdated(epgLastUpdate, strings),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (epgSource is ContentSource.RemoteUrl) {
                            IconButton(onClick = onRefreshEpg, enabled = !loading, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = strings.refresh, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                        OutlinedButton(
                            onClick = onEpgUrl,
                            enabled = !loading,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(if (epgSource is ContentSource.RemoteUrl) strings.edit else strings.remoteUrl, fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = onEpgFile,
                            enabled = !loading,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(if (epgSource is ContentSource.LocalFile) strings.edit else strings.localFile, fontSize = 11.sp)
                        }
                        if (epgSource != null) {
                            IconButton(onClick = onRemoveEpg, enabled = !loading, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = strings.removeEpg, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Channel Matching Status
        if (effectiveChannels.isNotEmpty()) {
            SettingsGroup(title = "EPG Matching") {
                if (isCompact) {
                    Column(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        if (epgGuide != null) strings.epgMatched(matchedCount, effectiveChannels.size) else strings.unmatchedEpgChannels,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (epgGuide == null || unmatchedChannels.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                    if (epgGuide != null && effectiveChannels.isNotEmpty()) {
                                        Spacer(Modifier.width(6.dp))
                                        val pct = (matchedCount * 100) / effectiveChannels.size
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (pct == 100) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                                        ) {
                                            Text(
                                                "$pct%",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (pct == 100) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    if (epgGuide != null && unmatchedChannels.isEmpty()) strings.allEpgMatched else strings.unmatchedEpgCount(unmatchedChannels.size),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (unmatchedChannels.isNotEmpty()) {
                                IconButton(onClick = ::shareList, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Share, contentDescription = strings.shareChannelsList, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        if (unmatchedChannels.isNotEmpty()) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { saveFileLauncher.launch("channels_without_epg.txt") },
                                    modifier = Modifier.weight(1f).height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.saveChannelsList, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { showUnmatchedInSettings = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(1f).height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.viewUnmatchedChannels, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (epgGuide != null) strings.epgMatched(matchedCount, effectiveChannels.size) else strings.unmatchedEpgChannels,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (epgGuide == null || unmatchedChannels.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                if (epgGuide != null && effectiveChannels.isNotEmpty()) {
                                    Spacer(Modifier.width(6.dp))
                                    val pct = (matchedCount * 100) / effectiveChannels.size
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (pct == 100) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                                    ) {
                                        Text(
                                            "$pct%",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (pct == 100) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                if (epgGuide != null && unmatchedChannels.isEmpty()) strings.allEpgMatched else strings.unmatchedEpgCount(unmatchedChannels.size),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (unmatchedChannels.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = ::shareList, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.Share, contentDescription = strings.shareChannelsList, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                                OutlinedButton(
                                    onClick = { saveFileLauncher.launch("channels_without_epg.txt") },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.saveChannelsList, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { showUnmatchedInSettings = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.viewUnmatchedChannels, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            if (showUnmatchedInSettings) {
                UnmatchedEpgDialog(
                    unmatchedChannels = unmatchedChannels,
                    allChannels = effectiveChannels,
                    epgGuide = epgGuide,
                    epgMappings = epgMappings,
                    onSaveMapping = onSaveMapping,
                    onResetMapping = onResetMapping,
                    onRequestAdultAuth = onRequestAdultAuth,
                    onDismiss = { showUnmatchedInSettings = false },
                    onSelectChannel = { showUnmatchedInSettings = false }
                )
            }
        }

        var showTimezoneDialog by remember { mutableStateOf(false) }
        var showDirectCorrectionDialog by remember { mutableStateOf(false) }

        // Source Timezone Fallback (when XMLTV omits timezone)
        SettingsGroup(title = strings.epgSourceTimezone) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(strings.epgSourceTimezone, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(strings.epgSourceTimezoneDesc, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    onClick = { showTimezoneDialog = true },
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val displayTz = if (epgSourceTimezone.isBlank()) {
                            "${strings.timezoneAutoDevice} (${java.time.ZoneId.systemDefault().id})"
                        } else {
                            epgSourceTimezone
                        }
                        Text(displayTz, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // EPG Schedule Correction
        SettingsGroup(title = strings.epgScheduleCorrection) {
            Column(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(strings.epgScheduleCorrection, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(strings.epgScheduleCorrectionDesc, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)

                // Steppers and direct entry
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(
                        onClick = { if (epgScheduleCorrectionMinutes > -1440) onEpgScheduleCorrectionChanged(epgScheduleCorrectionMinutes - 60) },
                        enabled = epgScheduleCorrectionMinutes > -1440 && !loading,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text("-1 h", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    FilledTonalButton(
                        onClick = { if (epgScheduleCorrectionMinutes > -1440) onEpgScheduleCorrectionChanged(epgScheduleCorrectionMinutes - 15) },
                        enabled = epgScheduleCorrectionMinutes > -1440 && !loading,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1.1f).height(36.dp)
                    ) {
                        Text("-15 m", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        onClick = { showDirectCorrectionDialog = true },
                        modifier = Modifier.weight(1.6f).height(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val correctionDisplay = when {
                                epgScheduleCorrectionMinutes > 0 -> "+$epgScheduleCorrectionMinutes min"
                                epgScheduleCorrectionMinutes < 0 -> "$epgScheduleCorrectionMinutes min"
                                else -> "0 min"
                            }
                            Text(
                                correctionDisplay,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    FilledTonalButton(
                        onClick = { if (epgScheduleCorrectionMinutes < 1440) onEpgScheduleCorrectionChanged(epgScheduleCorrectionMinutes + 15) },
                        enabled = epgScheduleCorrectionMinutes < 1440 && !loading,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1.1f).height(36.dp)
                    ) {
                        Text("+15 m", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    FilledTonalButton(
                        onClick = { if (epgScheduleCorrectionMinutes < 1440) onEpgScheduleCorrectionChanged(epgScheduleCorrectionMinutes + 60) },
                        enabled = epgScheduleCorrectionMinutes < 1440 && !loading,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text("+1 h", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    if (epgScheduleCorrectionMinutes != 0) {
                        TextButton(
                            onClick = { onEpgScheduleCorrectionChanged(0) },
                            enabled = !loading,
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(strings.retry, fontSize = 11.sp)
                        }
                    }
                }

                // Live Preview Card
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val baseTime = remember { LocalTime.of(20, 0) }
                    val correctedTime = remember(epgScheduleCorrectionMinutes) {
                        baseTime.plusMinutes(epgScheduleCorrectionMinutes.toLong())
                    }
                    val correctedStr = "%02d:%02d".format(correctedTime.hour, correctedTime.minute)
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            strings.epgPreview("20:00", correctedStr),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            strings.epgScheduleCorrectionDesc,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (showTimezoneDialog) {
            SourceTimezoneDialog(
                currentTimezone = epgSourceTimezone,
                onDismiss = { showTimezoneDialog = false },
                onSelectTimezone = { selected ->
                    showTimezoneDialog = false
                    onEpgSourceTimezoneChanged(selected)
                }
            )
        }

        if (showDirectCorrectionDialog) {
            DirectCorrectionDialog(
                initialMinutes = epgScheduleCorrectionMinutes,
                onDismiss = { showDirectCorrectionDialog = false },
                onConfirm = { minutes ->
                    showDirectCorrectionDialog = false
                    onEpgScheduleCorrectionChanged(minutes)
                }
            )
        }

        // EPG Days to Load (1 to 7 days)
        SettingsGroup(title = strings.epgLoadedDays) {
            if (isCompact) {
                Column(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(strings.epgLoadedDays, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(strings.epgLoadedDaysDesc, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { if (epgLoadedDays > 1) onEpgLoadedDaysChanged(epgLoadedDays - 1) },
                            enabled = epgLoadedDays > 1 && !loading,
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("-1", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.weight(1.5f).height(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                Text(
                                    strings.epgDaysCount(epgLoadedDays),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = { if (epgLoadedDays < 7) onEpgLoadedDaysChanged(epgLoadedDays + 1) },
                            enabled = epgLoadedDays < 7 && !loading,
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("+1", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(strings.epgLoadedDays, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(strings.epgLoadedDaysDesc, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilledTonalButton(
                            onClick = { if (epgLoadedDays > 1) onEpgLoadedDaysChanged(epgLoadedDays - 1) },
                            enabled = epgLoadedDays > 1 && !loading,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("-1", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.widthIn(min = 70.dp).height(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                Text(
                                    strings.epgDaysCount(epgLoadedDays),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = { if (epgLoadedDays < 7) onEpgLoadedDaysChanged(epgLoadedDays + 1) },
                            enabled = epgLoadedDays < 7 && !loading,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("+1", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}


