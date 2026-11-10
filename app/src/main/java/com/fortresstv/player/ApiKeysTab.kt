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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Sync
import coil.compose.AsyncImage
import com.fortresstv.player.trakt.TraktDeviceCodeDialog
import com.fortresstv.player.trakt.TraktSettingsStore
import com.fortresstv.player.trakt.TraktWatchlistStore
import com.fortresstv.player.trakt.TraktWatchlistService
import androidx.compose.material.icons.filled.Bookmark

@Composable
internal fun ApiKeysTab(
    tmdbSettings: TmdbSettings,
    onTmdbSettingsChanged: (TmdbSettings) -> Unit,
    introDbSettings: TheIntroDbSettings,
    onIntroDbSettingsChanged: (TheIntroDbSettings) -> Unit,
    omdbSettings: OmdbSettings = OmdbSettings(),
    onOmdbSettingsChanged: (OmdbSettings) -> Unit = {},
    traktNamespace: String = "local",
    onSyncWatchedHistory: (suspend () -> Result<Int>)? = null
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionTestResult by remember { mutableStateOf<Result<Boolean>?>(null) }
    var isTestingOmdbConnection by remember { mutableStateOf(false) }
    var omdbConnectionTestResult by remember { mutableStateOf<Result<Boolean>?>(null) }
    val traktStore = remember(context, traktNamespace) {
        TraktSettingsStore(context.applicationContext, traktNamespace)
    }
    var traktConnected by remember(traktStore) { mutableStateOf(traktStore.isConnected()) }
    var traktProfile by remember(traktStore) { mutableStateOf(traktStore.loadProfile()) }
    var traktClientId by remember(traktStore) { mutableStateOf(traktStore.loadClientId()) }
    var traktScrobbleEnabled by remember(traktStore) { mutableStateOf(traktStore.isScrobbleEnabled()) }
    var showTraktPairingDialog by remember { mutableStateOf(false) }
    var traktAuthErrorMessage by remember { mutableStateOf<String?>(null) }
    val isTelevision = LocalFormFactor.current.isTelevision

    LaunchedEffect(traktNamespace) {
        com.fortresstv.player.trakt.TraktAuthBridge.authEvents.collect { result ->
            when (result) {
                is com.fortresstv.player.trakt.TraktAuthResult.Success -> {
                    traktConnected = true
                    traktProfile = result.data
                    traktAuthErrorMessage = null
                }
                is com.fortresstv.player.trakt.TraktAuthResult.Failure -> {
                    traktAuthErrorMessage = result.message
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsGroup(title = strings.tmdbHeader) {
            SettingsToggleRow(
                title = strings.tmdbEnabled,
                subtitle = "",
                icon = Icons.Default.Movie,
                checked = tmdbSettings.enabled,
                onCheckedChange = { checked ->
                    onTmdbSettingsChanged(tmdbSettings.copy(enabled = checked))
                }
            )

            if (tmdbSettings.enabled) {
                HorizontalDivider(Modifier.padding(horizontal = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    var apiKeyText by remember(tmdbSettings.apiKey) { mutableStateOf(tmdbSettings.apiKey) }
                    var apiKeyVisible by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = {
                            apiKeyText = it
                            connectionTestResult = null
                            onTmdbSettingsChanged(tmdbSettings.copy(apiKey = it))
                        },
                        label = { Text(strings.tmdbApiKey) },
                        supportingText = { Text(strings.tmdbApiKeyHint, fontSize = 11.sp) },
                        visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                Icon(
                                    if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (apiKeyVisible) strings.hidePassword else strings.showPassword
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    SettingsDropdownPickerRow(
                        title = strings.tmdbLanguage,
                        subtitle = strings.tmdbLanguageDesc,
                        icon = Icons.Default.Translate,
                        selectedValue = tmdbSettings.language,
                        options = TmdbLanguages.supported.map { (code, label) ->
                            code to if (code == TmdbLanguages.AUTO) strings.tmdbLanguageAuto else label
                        },
                        onValueSelected = { newLang ->
                            onTmdbSettingsChanged(tmdbSettings.copy(language = newLang))
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (apiKeyText.isNotBlank()) {
                                    isTestingConnection = true
                                    connectionTestResult = null
                                    coroutineScope.launch {
                                        val res = TmdbClient(apiKeyText.trim()).validateApiKey()
                                        connectionTestResult = res
                                        isTestingConnection = false
                                    }
                                }
                            },
                            enabled = apiKeyText.isNotBlank() && !isTestingConnection
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(strings.tmdbTestConnection, fontSize = 13.sp)
                        }

                        connectionTestResult?.let { result ->
                            if (result.isSuccess) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        strings.tmdbConnectionSuccess,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        result.exceptionOrNull()?.message?.takeIf(String::isNotBlank) ?: strings.tmdbConnectionFailed,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    strings.tmdbInfoHelp,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    strings.tmdbInfoLink,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable {
                                        runCatching {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(strings.tmdbInfoLink))
                                            context.startActivity(intent)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        SettingsGroup(title = strings.omdbHeader) {
            SettingsToggleRow(
                title = strings.omdbEnabled,
                subtitle = "",
                icon = Icons.Default.Star,
                checked = omdbSettings.enabled,
                onCheckedChange = { checked ->
                    onOmdbSettingsChanged(omdbSettings.copy(enabled = checked))
                }
            )

            if (omdbSettings.enabled) {
                HorizontalDivider(Modifier.padding(horizontal = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    var omdbKeyText by remember(omdbSettings.apiKey) { mutableStateOf(omdbSettings.apiKey) }
                    var omdbKeyVisible by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = omdbKeyText,
                        onValueChange = {
                            omdbKeyText = it
                            omdbConnectionTestResult = null
                            onOmdbSettingsChanged(omdbSettings.copy(apiKey = it))
                        },
                        label = { Text(strings.omdbApiKey) },
                        supportingText = { Text(strings.omdbApiKeyHint, fontSize = 11.sp) },
                        visualTransformation = if (omdbKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { omdbKeyVisible = !omdbKeyVisible }) {
                                Icon(
                                    if (omdbKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (omdbKeyVisible) strings.hidePassword else strings.showPassword
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (omdbKeyText.isNotBlank()) {
                                    isTestingOmdbConnection = true
                                    omdbConnectionTestResult = null
                                    coroutineScope.launch {
                                        val res = OmdbClient(omdbKeyText.trim()).validateApiKey()
                                        omdbConnectionTestResult = res
                                        isTestingOmdbConnection = false
                                    }
                                }
                            },
                            enabled = omdbKeyText.isNotBlank() && !isTestingOmdbConnection
                        ) {
                            if (isTestingOmdbConnection) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(strings.omdbTestConnection, fontSize = 13.sp)
                        }

                        omdbConnectionTestResult?.let { result ->
                            if (result.isSuccess) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        strings.omdbConnectionSuccess,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        result.exceptionOrNull()?.message?.takeIf(String::isNotBlank) ?: strings.omdbConnectionFailed,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    strings.omdbInfoHelp,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    strings.omdbInfoLink,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable {
                                        runCatching {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(strings.omdbInfoLink))
                                            context.startActivity(intent)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        SettingsGroup(title = strings.introDbHeader) {
            SettingsToggleRow(
                title = strings.introDbEnabled,
                subtitle = strings.introDbEnabledDesc,
                icon = Icons.Default.FastForward,
                checked = introDbSettings.enabled,
                onCheckedChange = { checked ->
                    onIntroDbSettingsChanged(introDbSettings.copy(enabled = checked))
                }
            )

            if (introDbSettings.enabled) {
                HorizontalDivider(Modifier.padding(horizontal = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                SettingsToggleRow(
                    title = strings.introDbAutoSkipIntro,
                    subtitle = strings.introDbAutoSkipIntroDesc,
                    icon = Icons.Default.FastForward,
                    checked = introDbSettings.autoSkipIntro,
                    onCheckedChange = { checked ->
                        onIntroDbSettingsChanged(introDbSettings.copy(autoSkipIntro = checked))
                    }
                )

                HorizontalDivider(Modifier.padding(horizontal = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                SettingsToggleRow(
                    title = strings.introDbAutoSkipRecap,
                    subtitle = strings.introDbAutoSkipRecapDesc,
                    icon = Icons.Default.FastForward,
                    checked = introDbSettings.autoSkipRecap,
                    onCheckedChange = { checked ->
                        onIntroDbSettingsChanged(introDbSettings.copy(autoSkipRecap = checked))
                    }
                )
            }
        }
        SettingsGroup(title = strings.traktHeader) {
            if (traktConnected && traktProfile != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (!traktProfile?.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(traktProfile?.avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "@" + (traktProfile?.username ?: ""),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (traktProfile?.isVip == true) {
                                    Spacer(Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = strings.traktVipBadge,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                            }
                            Text(
                                text = traktProfile?.name.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            traktStore.clearConnection()
                            traktConnected = false
                            traktProfile = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(strings.traktDisconnect)
                    }
                }

                HorizontalDivider(Modifier.padding(horizontal = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                SettingsToggleRow(
                    title = strings.traktScrobble,
                    subtitle = strings.traktScrobbleDesc,
                    icon = Icons.Default.Sync,
                    checked = traktScrobbleEnabled,
                    onCheckedChange = { checked ->
                        traktScrobbleEnabled = checked
                        traktStore.setScrobbleEnabled(checked)
                    }
                )

                HorizontalDivider(Modifier.padding(horizontal = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                var isSyncingWatchlist by remember { mutableStateOf(false) }
                var watchlistSyncStatus by remember { mutableStateOf<String?>(null) }
                val watchlistStore = remember(traktNamespace) { TraktWatchlistStore(context.applicationContext, traktNamespace) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.traktWatchlist,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val lastSyncMs = watchlistStore.lastSyncedEpochMs()
                        val subtitle = if (isSyncingWatchlist) {
                            strings.traktSyncingWatchlist
                        } else if (watchlistSyncStatus != null) {
                            watchlistSyncStatus!!
                        } else if (lastSyncMs > 0L) {
                            strings.traktWatchlistSynced.format(watchlistStore.loadWatchlist().size)
                        } else {
                            strings.traktWatchlistEmpty
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            isSyncingWatchlist = true
                            watchlistSyncStatus = null
                            coroutineScope.launch {
                                val result = TraktWatchlistService.syncWatchlist(context, traktNamespace)
                                isSyncingWatchlist = false
                                watchlistSyncStatus = if (result.isSuccess) {
                                    strings.traktWatchlistSynced.format(result.getOrDefault(emptyList()).size)
                                } else {
                                    result.exceptionOrNull()?.message ?: "Sync error"
                                }
                            }
                        },
                        enabled = !isSyncingWatchlist
                    ) {
                        if (isSyncingWatchlist) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(strings.traktSyncWatchlist)
                    }
                }

                HorizontalDivider(Modifier.padding(horizontal = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                var isSyncingWatchedHistory by remember { mutableStateOf(false) }
                var watchedHistorySyncStatus by remember { mutableStateOf<String?>(null) }
                val watchedStore = remember(traktNamespace) { com.fortresstv.player.trakt.TraktWatchedStore(context.applicationContext, traktNamespace) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.traktWatchedHistory,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val lastSyncMs = watchedStore.lastSyncedEpochMs()
                        val subtitle = if (isSyncingWatchedHistory) {
                            strings.traktSyncingWatchedHistory
                        } else if (watchedHistorySyncStatus != null) {
                            watchedHistorySyncStatus!!
                        } else if (lastSyncMs > 0L) {
                            val count = watchedStore.loadWatchedMovies().size + watchedStore.loadWatchedShows().size
                            strings.traktWatchedHistorySynced.format(count)
                        } else {
                            strings.traktWatchedHistoryEmpty
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            isSyncingWatchedHistory = true
                            watchedHistorySyncStatus = null
                            coroutineScope.launch {
                                val result = if (onSyncWatchedHistory != null) {
                                    onSyncWatchedHistory()
                                } else {
                                    com.fortresstv.player.trakt.TraktWatchedSyncService.syncWatchedHistory(context, traktNamespace)
                                }
                                isSyncingWatchedHistory = false
                                watchedHistorySyncStatus = if (result.isSuccess) {
                                    strings.traktWatchedHistorySynced.format(result.getOrDefault(0))
                                } else {
                                    result.exceptionOrNull()?.message ?: "Sync error"
                                }
                            }
                        },
                        enabled = !isSyncingWatchedHistory
                    ) {
                        if (isSyncingWatchedHistory) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(strings.traktSyncWatchedHistory)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.traktDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    var clientIdText by remember(traktClientId) { mutableStateOf(traktClientId) }
                    val cleanClientId = clientIdText.trim().trim('"', '\'')
                    val isInvalidLength = cleanClientId.isNotEmpty() && cleanClientId.length < 20

                    OutlinedTextField(
                        value = clientIdText,
                        onValueChange = {
                            clientIdText = it
                            val sanitized = it.trim().trim('"', '\'')
                            traktClientId = sanitized
                            traktStore.saveClientId(sanitized)
                        },
                        label = { Text(strings.traktClientId) },
                        isError = isInvalidLength,
                        supportingText = {
                            if (isInvalidLength) {
                                Text(
                                    strings.traktClientIdLengthWarning.format(cleanClientId.length),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Text(strings.traktClientIdHint, fontSize = 11.sp)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            traktAuthErrorMessage = null
                            if (!isTelevision) {
                                com.fortresstv.player.trakt.TraktAuthBridge.startAuthorization(
                                    context = context,
                                    clientId = cleanClientId,
                                    profileNamespace = traktNamespace
                                )
                            } else {
                                showTraktPairingDialog = true
                            }
                        },
                        enabled = cleanClientId.isNotBlank() && !isInvalidLength,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.traktConnect)
                    }

                    if (traktAuthErrorMessage != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = traktAuthErrorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                strings.traktHelp,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                strings.traktHelpLink,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    runCatching {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(strings.traktHelpLink))
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTraktPairingDialog && traktClientId.isNotBlank()) {
        TraktDeviceCodeDialog(
            clientId = traktClientId,
            onSuccess = { token, profile ->
                traktStore.saveConnection(token, profile)
                traktConnected = true
                traktProfile = profile
                showTraktPairingDialog = false
            },
            onDismiss = {
                showTraktPairingDialog = false
            }
        )
    }

}


