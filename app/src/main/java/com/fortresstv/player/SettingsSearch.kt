package com.fortresstv.player

import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.fortresstv.player.ui.LocalAppStrings
import com.fortresstv.player.ui.LocalFormFactor
import com.fortresstv.player.ui.Strings
import com.fortresstv.player.ui.tvFocusHighlight
import kotlinx.coroutines.delay
import java.text.Normalizer
import java.util.Locale

internal data class SettingsSearchEntry(
    val tab: Int,
    val title: String,
    val keywords: String = "",
    val group: Boolean = false,
    val targetTitle: String = title
)

/** Static labels only: never index user-entered values or private configuration. */
internal fun settingsSearchEntries(s: Strings): List<SettingsSearchEntry> = listOf(
    SettingsSearchEntry(0, s.language, "English Português language idioma", group = true),
    SettingsSearchEntry(0, s.homeStartupScreen, "Home Live TV início arranque"),
    SettingsSearchEntry(0, s.customizeHome, s.customizeHomeDescription),
    SettingsSearchEntry(0, s.playbackMode, "${s.playbackModeDesc} buffer buffering network rede"),
    SettingsSearchEntry(0, s.playChannelsFullscreen, s.playChannelsFullscreenDesc),
    SettingsSearchEntry(0, s.autoPictureInPicture, "${s.autoPictureInPictureDesc} PiP"),
    SettingsSearchEntry(0, s.anonymousTelemetry, s.anonymousTelemetryDesc),
    SettingsSearchEntry(0, s.parentalControl, "${s.parentalControlDesc} PIN"),
    SettingsSearchEntry(0, s.hideAdultContent, s.hideAdultContentSubtitle),
    SettingsSearchEntry(1, s.tabPlaylists, "M3U M3U8 Xtream sources fontes ${s.refreshPlaylist} ${s.editPlaylist}", group = true),
    SettingsSearchEntry(2, s.xmltvSource, "XMLTV EPG ${s.configureXmltv} ${s.refresh}", group = true),
    SettingsSearchEntry(2, s.epgManualMapping, s.epgManualMappingDesc, group = true),
    SettingsSearchEntry(2, s.epgSourceTimezone, s.epgSourceTimezoneDesc, group = true),
    SettingsSearchEntry(2, s.epgScheduleCorrection, s.epgScheduleCorrectionDesc, group = true),
    SettingsSearchEntry(2, s.epgLoadedDays, s.epgLoadedDaysDesc, group = true),
    SettingsSearchEntry(3, s.preferredAudioLanguage, "audio English Português idioma língua"),
    SettingsSearchEntry(3, s.preferredSubtitleLanguage, "subtitles English Português legendas idioma língua"),
    SettingsSearchEntry(3, s.subtitleFallbackEnglish, s.subtitleFallbackEnglishDesc),
    SettingsSearchEntry(3, s.openSubtitles, "${s.openSubtitlesApiKey} ${s.openSubtitlesAutoSearch} ${s.openSubtitlesPreferPtPt}", group = true),
    SettingsSearchEntry(4, s.tmdbHeader, "${s.tmdbApiKey} ${s.tmdbLanguage} metadata posters cartazes", group = true),
    SettingsSearchEntry(4, s.omdbHeader, "${s.omdbApiKey} IMDb ratings avaliações", group = true),
    SettingsSearchEntry(4, s.introDbHeader, "${s.skipIntro} ${s.skipRecap} ${s.skipCredits}", group = true),
    SettingsSearchEntry(4, s.traktHeader, "${s.traktScrobble} ${s.traktWatchlist} ${s.traktRecommendations}", group = true),
    SettingsSearchEntry(5, s.tabAccount, "${s.profiles} ${s.switchProfile} sync sincronização conta", group = true),
    SettingsSearchEntry(6, s.checkForUpdates, "APK OTA version versão atualização", group = true),
    SettingsSearchEntry(6, s.securityTitle, "${s.securityDesc} privacy privacidade", group = true)
)

private val searchMarks = Regex("\\p{M}+")
private fun normaliseSettingsSearch(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
    .replace(searchMarks, "").lowercase(Locale.ROOT)

internal fun searchSettings(entries: List<SettingsSearchEntry>, query: String): List<SettingsSearchEntry> {
    val normalised = normaliseSettingsSearch(query.trim())
    if (normalised.isBlank()) return emptyList()
    val terms = normalised.split(Regex("\\s+"))
    return entries.filter { entry ->
        val searchable = normaliseSettingsSearch("${entry.title} ${entry.keywords}")
        terms.all(searchable::contains)
    }.sortedBy { entry ->
        val title = normaliseSettingsSearch(entry.title)
        when {
            title == normalised -> 0
            title.startsWith(normalised) -> 1
            title.contains(normalised) -> 2
            else -> 3
        }
    }
}

internal class SettingsSearchNavigator {
    var target by mutableStateOf<SettingsSearchEntry?>(null)
    var generation by mutableIntStateOf(0)
    fun open(entry: SettingsSearchEntry) { target = entry; generation++ }
}

internal val LocalSettingsSearchNavigator = staticCompositionLocalOf<SettingsSearchNavigator?> { null }

/** Scrolls and highlights the real setting after a result is selected. */
internal fun Modifier.settingsSearchTarget(title: String, group: Boolean = false): Modifier = composed {
    val navigator = LocalSettingsSearchNavigator.current
    val target = navigator?.target
    val matches = target?.targetTitle == title && target.group == group
    val bring = remember { BringIntoViewRequester() }
    val focus = remember { FocusRequester() }
    val isTv = LocalFormFactor.current.isTelevision
    var width by remember { mutableIntStateOf(1) }
    LaunchedEffect(matches, navigator?.generation) {
        if (matches) {
            withFrameNanos {}
            // Target the top of a group rather than requesting its entire (possibly
            // screen-taller) bounds, which could otherwise scroll past its heading.
            bring.bringIntoView(if (group) Rect(0f, 0f, width.toFloat(), 40f) else null)
            if (isTv) runCatching { focus.requestFocus() }
            delay(3_000)
            navigator?.target = null
        }
    }
    this.onSizeChanged { width = it.width }
        .bringIntoViewRequester(bring)
        .focusRequester(focus)
        .then(if (group) Modifier.focusGroup() else Modifier)
        .then(if (matches) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier)
}

@Composable
internal fun SettingsSearchResults(
    entries: List<SettingsSearchEntry>, query: String, sections: List<String>,
    modifier: Modifier = Modifier, onSelect: (SettingsSearchEntry) -> Unit
) {
    val strings = LocalAppStrings.current
    val results = remember(entries, query) { searchSettings(entries, query) }
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (results.isEmpty()) item { Text(strings.noSettingsFound, Modifier.padding(12.dp)) }
        items(results, key = { "${it.tab}:${it.title}" }) { entry ->
            Surface(onClick = { onSelect(entry) }, modifier = Modifier.fillMaxWidth().tvFocusHighlight(), shape = RoundedCornerShape(10.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(entry.title, style = MaterialTheme.typography.titleSmall)
                    Text(sections[entry.tab], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
