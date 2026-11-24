package com.fortresstv.player

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fortresstv.player.library.UserLibraryStore.Companion.libraryId
import com.fortresstv.player.profile.ParentalControls
import com.fortresstv.player.playlist.PlaylistChannel
import com.fortresstv.player.player.ChannelFailoverResolver
import com.fortresstv.player.ui.LocalAppStrings
import com.fortresstv.player.ui.LocalFormFactor
import com.fortresstv.player.ui.tvFocusHighlight
import com.fortresstv.player.xtream.XtreamCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** User choices only. Never stores stream URLs, credentials, or catalogue data. */
internal class PlaybackChoiceStore(context: Context, profileId: String) {
    private val prefs = context.applicationContext.getSharedPreferences("playback_choices_$profileId", Context.MODE_PRIVATE)

    fun savedVersion(itemId: String): String? = prefs.getString("version:$itemId", null)

    fun preferredVersion(itemIds: List<String>): String? = itemIds.asSequence()
        .mapNotNull(::savedVersion)
        .firstOrNull { it in itemIds }

    fun saveVersion(itemIds: List<String>, selectedId: String) {
        require(selectedId in itemIds)
        prefs.edit().apply { itemIds.forEach { putString("version:$it", selectedId) } }.apply()
    }
}

internal data class PlaybackVersionOption(val id: String, val title: String, val sourceName: String)

@Composable
internal fun LiveVersionPicker(
    channel: PlaylistChannel,
    channels: List<PlaylistChannel>,
    onDismiss: () -> Unit,
    onSelect: (PlaylistChannel) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val activeProfile by com.fortresstv.player.profile.ProfileRepository.get(context).activeProfile.collectAsState()
    val profile = activeProfile.id
    val household = com.fortresstv.player.account.AccountRepository.get(context).currentUserId()
    val choices = remember(profile, household) { PlaybackChoiceStore(context, com.fortresstv.player.library.profileProgressNamespace(household, profile)) }
    var versions by remember(channel, channels) { mutableStateOf<List<PlaylistChannel>?>(null) }
    var sourceNames by remember { mutableStateOf(emptyMap<String, String>()) }
    LaunchedEffect(channel, channels) {
        sourceNames = withContext(Dispatchers.IO) {
            com.fortresstv.player.source.SourceSettingsStore(context).getPlaylists().associate { it.id to it.name }
        }
        versions = withContext(Dispatchers.Default) {
            channels.filter { it.libraryId() != null && (it == channel || ChannelFailoverResolver.isCandidate(channel, it)) }
                .distinctBy { it.libraryId() }
        }
    }
    val available = versions
    if (available == null) {
        AlertDialog(onDismissRequest = onDismiss, text = { CircularProgressIndicator() }, confirmButton = {})
    } else if (available.isEmpty()) {
        AlertDialog(onDismissRequest = onDismiss, text = { Text(strings.continueWatchingUnavailable) },
            confirmButton = { TextButton(onClick = onDismiss) { Text(strings.close) } })
    } else {
        val options = available.map { PlaybackVersionOption(it.libraryId()!!, it.name, sourceNames[it.sourceId].orEmpty()) }
        val ids = options.map { it.id }
        PlaybackVersionPicker(options, choices.preferredVersion(ids) ?: channel.libraryId(), onDismiss) { id ->
            choices.saveVersion(ids, id)
            onSelect(available.first { it.libraryId() == id })
        }
    }
}

@Composable
internal fun PlaybackVersionPicker(
    options: List<PlaybackVersionOption>,
    preferredId: String?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    var selected by remember(options, preferredId) { mutableStateOf(preferredId?.takeIf { id -> options.any { it.id == id } } ?: options.firstOrNull()?.id) }
    val isTv = LocalFormFactor.current.isTelevision
    val focus = remember { FocusRequester() }
    val initialIndex = options.indexOfFirst { it.id == preferredId }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    LaunchedEffect(isTv) { if (isTv) runCatching { focus.requestFocus() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.chooseVersion) },
        text = {
            Column {
                Text(strings.versionPreferenceHint)
                Spacer(Modifier.height(12.dp))
                LazyColumn(Modifier.heightIn(max = 360.dp), state = listState, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(options, key = { it.id }) { option ->
                        Surface(
                            onClick = { selected = option.id },
                            color = if (selected == option.id) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth().tvFocusHighlight()
                                .then(if (option.id == (preferredId ?: options.firstOrNull()?.id)) Modifier.focusRequester(focus) else Modifier)
                        ) {
                            Row(Modifier.padding(8.dp)) {
                                RadioButton(selected = selected == option.id, onClick = null)
                                Column(Modifier.padding(start = 8.dp).weight(1f)) {
                                    Text(option.title)
                                    Text(option.sourceName, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { selected?.let(onSelect) }, enabled = selected != null) { Text(strings.playNow) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
internal fun MovieVersionPicker(
    requested: CatalogMovie,
    catalogs: List<XtreamCatalog>,
    hideAdultContent: Boolean,
    maximumAge: Int?,
    blockUnrated: Boolean,
    choices: PlaybackChoiceStore,
    onDismiss: () -> Unit,
    onPlay: (CatalogMovie) -> Unit
) {
    var versions by remember(requested) { mutableStateOf<List<CatalogMovie>?>(null) }
    LaunchedEffect(requested, catalogs, hideAdultContent, maximumAge, blockUnrated) {
        val found = withContext(Dispatchers.Default) {
            val allowed = catalogs.flatMap { catalog ->
                val categories = catalog.movieCategories.associate { it.id to it.name }
                catalog.movies.asSequence().filter { movie ->
                    (!hideAdultContent || !isAdultContent(movie.name, categories[movie.categoryId])) &&
                        ParentalControls.isContentAllowedForAge(movie.ageRating, maximumAge, blockUnrated)
                }.map { CatalogMovie(catalog, it) }.toList()
            }
            groupCatalogMovieVersions(allowed).firstOrNull { group ->
                group.any { it.catalog.sourceId == requested.catalog.sourceId && it.movie.id == requested.movie.id }
            }.orEmpty().map { entry ->
                if (entry.catalog.sourceId == requested.catalog.sourceId && entry.movie.id == requested.movie.id) requested else entry
            }
        }
        if (found.size == 1) onPlay(found.first()) else versions = found
    }
    val available = versions
    if (available == null) {
        AlertDialog(onDismissRequest = onDismiss, text = { CircularProgressIndicator() }, confirmButton = {})
    } else if (available.isEmpty()) {
        val strings = LocalAppStrings.current
        AlertDialog(onDismissRequest = onDismiss, text = { Text(strings.continueWatchingUnavailable) },
            confirmButton = { TextButton(onClick = onDismiss) { Text(strings.close) } })
    } else {
        val options = available.map { PlaybackVersionOption(it.movie.libraryId(it.catalog.sourceId), it.movie.name, it.catalog.sourceName) }
        val ids = options.map { it.id }
        PlaybackVersionPicker(options, choices.preferredVersion(ids), onDismiss) { id ->
            choices.saveVersion(ids, id)
            onPlay(available.first { it.movie.libraryId(it.catalog.sourceId) == id })
        }
    }
}
