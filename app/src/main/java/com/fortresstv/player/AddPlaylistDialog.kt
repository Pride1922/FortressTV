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
internal fun AddPlaylistDialog(
    onDismiss: () -> Unit,
    onAddUrl: (String, String) -> Unit,
    onAddXtream: (String, String, String, String) -> Unit,
    onPickFile: () -> Unit,
    onSendFromPhone: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val isTv = LocalFormFactor.current.isTelevision
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var mode by remember { mutableIntStateOf(-1) }
    var isConnecting by remember { mutableStateOf(false) }
    var connectionError by remember { mutableStateOf<String?>(null) }
    var pendingHttpServerUrl by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val serverFocusRequester = remember { FocusRequester() }
    val usernameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val sourceMethodFocusRequester = remember { FocusRequester() }
    val playlistNameFocusRequester = remember { FocusRequester() }

    fun hideKeyboard() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    LaunchedEffect(isTv, mode) {
        if (isTv) {
            delay(100)
            runCatching {
                if (mode < 0) sourceMethodFocusRequester.requestFocus()
                else playlistNameFocusRequester.requestFocus()
            }
        }
    }

    BackHandler(onBack = {
        hideKeyboard()
        if (isTv && mode >= 0) {
            connectionError = null
            pendingHttpServerUrl = null
            mode = -1
        } else {
            onDismiss()
        }
    })

    Dialog(
        onDismissRequest = { hideKeyboard() },
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
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
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(0.92f)
                .padding(16.dp)
                .then(
                    if (!isTv) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { hideKeyboard() }
                    } else Modifier
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    if (mode < 0) {
                        if (strings.languageTag == "pt-PT") "Onde está o seu conteúdo?" else "Where is your content?"
                    } else strings.addPlaylist,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (mode < 0) {
                        if (strings.languageTag == "pt-PT") "Escolha os dados que recebeu do fornecedor ou proprietário da lista." else "Choose the details supplied by your provider or playlist owner."
                    } else {
                        when (mode) {
                            0 -> if (strings.languageTag == "pt-PT") "Cole o endereço da lista ou escolha um ficheiro neste dispositivo." else "Paste the playlist address or choose a file on this device."
                            else -> if (strings.languageTag == "pt-PT") "Introduza os dados exatamente como foram fornecidos." else "Enter the credentials exactly as supplied."
                        }
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))

                if (mode < 0) {
                    SourceMethodCard(
                        icon = Icons.Default.Key,
                        title = strings.xtreamCodes,
                        description = if (strings.languageTag == "pt-PT") "Servidor, nome de utilizador e palavra-passe" else "Server, username and password",
                        modifier = Modifier.focusRequester(sourceMethodFocusRequester),
                        onClick = { mode = 2 }
                    )
                    Spacer(Modifier.height(10.dp))
                    SourceMethodCard(
                        icon = Icons.Default.Link,
                        title = if (strings.languageTag == "pt-PT") "Lista M3U" else "M3U playlist",
                        description = if (strings.languageTag == "pt-PT") "Adicionar por endereço ou ficheiro local" else "Add by web address or local file",
                        onClick = { mode = 0 }
                    )
                    Spacer(Modifier.height(10.dp))
                    SourceMethodCard(
                        icon = Icons.Default.PhoneAndroid,
                        title = strings.transferSendFromPhone,
                        description = strings.transferSendFromPhoneDesc,
                        onClick = {
                            onDismiss()
                            onSendFromPhone()
                        }
                    )
                    Spacer(Modifier.height(18.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.End)
                            .tvFocusHighlight(
                                shape = ButtonDefaults.textShape,
                                scaleOnFocus = if (isTv) 1.05f else 1.0f
                            )
                    ) {
                        Text(strings.cancel)
                    }
                    return@Column
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.playlistName) },
                    placeholder = { Text(strings.playlistNamePlaceholder) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { serverFocusRequester.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(playlistNameFocusRequester)
                        .tvFocusHighlight(
                            shape = OutlinedTextFieldDefaults.shape,
                            scaleOnFocus = if (isTv) 1.02f else 1f
                        )
                )
                Spacer(Modifier.height(12.dp))
                if (mode == 0) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it; connectionError = null; pendingHttpServerUrl = null },
                        label = { Text(strings.playlistM3u) },
                        placeholder = { Text(strings.enterM3uUrl) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { hideKeyboard() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(serverFocusRequester)
                            .tvFocusHighlight(
                                shape = OutlinedTextFieldDefaults.shape,
                                scaleOnFocus = if (isTv) 1.02f else 1f
                            )
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            hideKeyboard()
                            onPickFile()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvFocusHighlight(
                                shape = ButtonDefaults.outlinedShape,
                                scaleOnFocus = if (isTv) 1.02f else 1.0f
                            )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.selectFile)
                    }
                } else {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text(strings.xtreamServer) },
                        placeholder = { Text("http://server.example:port") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { usernameFocusRequester.requestFocus() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(serverFocusRequester)
                            .tvFocusHighlight(
                                shape = OutlinedTextFieldDefaults.shape,
                                scaleOnFocus = if (isTv) 1.02f else 1f
                            )
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; connectionError = null },
                        label = { Text(strings.username) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(usernameFocusRequester)
                            .tvFocusHighlight(
                                shape = OutlinedTextFieldDefaults.shape,
                                scaleOnFocus = if (isTv) 1.02f else 1f
                            )
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; connectionError = null },
                        label = { Text(strings.password) },
                        singleLine = true,
                        visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { hideKeyboard() }),
                        trailingIcon = {
                            IconButton(
                                onClick = { showPassword = !showPassword },
                                modifier = Modifier.focusable(!isTv)
                            ) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFocusRequester)
                            .tvFocusHighlight(
                                shape = OutlinedTextFieldDefaults.shape,
                                scaleOnFocus = if (isTv) 1.02f else 1f
                            )
                    )
                }

                connectionError?.let { message ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            hideKeyboard()
                            connectionError = null
                            pendingHttpServerUrl = null
                            mode = -1
                        },
                        modifier = Modifier.tvFocusHighlight(
                            shape = ButtonDefaults.textShape,
                            scaleOnFocus = if (isTv) 1.05f else 1.0f
                        )
                    ) {
                        Text(strings.back)
                    }
                    Spacer(Modifier.width(8.dp))
                    if (mode == 0) {
                        Button(
                            onClick = {
                                hideKeyboard()
                                onAddUrl(name.trim(), url.trim())
                            },
                            enabled = url.startsWith("http://", true) || url.startsWith("https://", true),
                            modifier = Modifier.tvFocusHighlight(
                                shape = ButtonDefaults.shape,
                                scaleOnFocus = if (isTv) 1.05f else 1.0f
                            )
                        ) { Text(strings.addPlaylist) }
                    } else {
                        Button(
                            onClick = {
                                hideKeyboard()
                                val confirmedHttpUrl = pendingHttpServerUrl
                                if (confirmedHttpUrl != null) {
                                    onAddXtream(name.trim(), confirmedHttpUrl, username.trim(), password)
                                    return@Button
                                }
                                isConnecting = true
                                connectionError = null
                                scope.launch {
                                    val result = runCatching {
                                        withContext(Dispatchers.IO) {
                                            validateXtreamConnection(
                                                url.trim(),
                                                name.trim().ifBlank { "Xtream" },
                                                username.trim(),
                                                password
                                            )
                                        }
                                    }
                                    isConnecting = false
                                    result.onSuccess { check ->
                                        if (check.requiresHttpConfirmation) {
                                            pendingHttpServerUrl = check.resolvedServerUrl
                                            connectionError = if (strings.languageTag == "pt-PT") {
                                                "Este servidor não suporta HTTPS. Utilize HTTP apenas se confiar no fornecedor; as credenciais serão enviadas sem encriptação."
                                            } else {
                                                "This server does not support HTTPS. Use HTTP only if you trust the provider; credentials will be sent without encryption."
                                            }
                                        } else {
                                            onAddXtream(name.trim(), check.resolvedServerUrl, username.trim(), password)
                                        }
                                    }.onFailure { failure ->
                                        connectionError = xtreamConnectionErrorMessage(failure, strings)
                                    }
                                }
                            },
                            enabled = !isConnecting &&
                                    (url.startsWith("http://", true) || url.startsWith("https://", true)) &&
                                    username.isNotBlank() && password.isNotBlank(),
                            modifier = Modifier.tvFocusHighlight(
                                shape = ButtonDefaults.shape,
                                scaleOnFocus = if (isTv) 1.05f else 1.0f
                            )
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                if (pendingHttpServerUrl != null) {
                                    if (strings.languageTag == "pt-PT") "Utilizar HTTP" else "Use HTTP"
                                } else if (isConnecting) {
                                    if (strings.languageTag == "pt-PT") "A ligar…" else "Connecting…"
                                } else strings.connect
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

internal data class XtreamConnectionCheck(
    val resolvedServerUrl: String,
    val requiresHttpConfirmation: Boolean
)

internal suspend fun validateXtreamConnection(
    serverUrl: String,
    displayName: String,
    username: String,
    password: String
) : XtreamConnectionCheck {
    val source = ContentSource.Xtream(serverUrl, displayName, username, password)
    val client = XtreamClient(null, "connection_test", source)
    client.authenticate()
    val normalizedInput = XtreamClient.normalizeServerUrl(serverUrl)
    val resolved = client.resolvedServerUrl()
    return XtreamConnectionCheck(
        resolvedServerUrl = resolved,
        requiresHttpConfirmation = normalizedInput.startsWith("https://", true) &&
                resolved.startsWith("http://", true)
    )
}

internal fun xtreamConnectionErrorMessage(failure: Throwable, strings: Strings): String {
    val causes = generateSequence(failure) { it.cause }.toList()
    val message = causes.firstNotNullOfOrNull { it.message?.takeIf(String::isNotBlank) }.orEmpty()
    val portuguese = strings.languageTag == "pt-PT"
    return when {
        message.contains("account is", ignoreCase = true) -> if (portuguese) {
            when {
                message.contains("expired", true) -> "A conta Xtream expirou."
                message.contains("disabled", true) -> "A conta Xtream está desativada."
                message.contains("banned", true) -> "A conta Xtream está bloqueada."
                else -> "A conta Xtream não está ativa."
            }
        } else {
            when {
                message.contains("expired", true) -> "The Xtream account has expired."
                message.contains("disabled", true) -> "The Xtream account is disabled."
                message.contains("banned", true) -> "The Xtream account is banned."
                else -> "The Xtream account is not active."
            }
        }
        message.contains("authentication failed", true) ->
            if (portuguese) "Nome de utilizador ou palavra-passe incorretos." else "Incorrect username or password."
        causes.any { it is java.net.UnknownHostException } ->
            if (portuguese) "Não foi possível encontrar o servidor." else "The server could not be found."
        causes.any { it is java.net.SocketTimeoutException } ->
            if (portuguese) "A ligação ao servidor excedeu o tempo limite." else "The server connection timed out."
        causes.any { it is javax.net.ssl.SSLException } ->
            if (portuguese) "Não foi possível estabelecer uma ligação segura ao servidor." else "A secure connection to the server could not be established."
        message.contains("HTTP 401") || message.contains("HTTP 403") ->
            if (portuguese) "O servidor recusou as credenciais ($message)." else "The server rejected the credentials ($message)."
        message.contains("HTTP ") ->
            if (portuguese) "O servidor recusou a ligação ($message)." else "The server rejected the connection ($message)."
        message.contains("Invalid Xtream response", true) || message.contains("JSON", true) ->
            if (portuguese) "O servidor devolveu uma resposta Xtream inválida." else "The server returned an invalid Xtream response."
        message.contains("catalogue is empty", true) ->
            if (portuguese) "A ligação foi estabelecida, mas o servidor não devolveu canais, filmes ou séries." else "Connected, but the server returned no channels, movies, or series."
        else -> if (portuguese) {
            "Não foi possível carregar o catálogo Xtream. Verifique o servidor e as credenciais."
        } else {
            "The Xtream catalogue could not be loaded. Check the server and credentials."
        }
    }
}


