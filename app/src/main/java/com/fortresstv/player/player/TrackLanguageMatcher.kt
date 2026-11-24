package com.fortresstv.player.player

import androidx.media3.common.C
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import java.util.Locale

@UnstableApi
data class PlaybackTrackItem(
    val id: String,
    val type: Int,
    val label: String,
    val language: String?,
    val isSelected: Boolean,
    val group: Tracks.Group,
    val trackIndex: Int
)

@UnstableApi
object TrackLanguageMatcher {

    fun preferredLanguages(language: String, fallbackEnglish: Boolean): List<String> {
        val preferred = language.trim().lowercase(Locale.ROOT).replace('_', '-')
        if (preferred.isBlank() || preferred == "off" || preferred == "default") return emptyList()
        return if (fallbackEnglish && !matchesLanguage(preferred, null, "en")) {
            listOf(preferred, "en")
        } else listOf(preferred)
    }

    fun extractAudioTracks(currentTracks: Tracks?): List<PlaybackTrackItem> {
        val list = mutableListOf<PlaybackTrackItem>()
        currentTracks?.groups?.filter { it.type == C.TRACK_TYPE_AUDIO }?.forEach { group ->
            for (i in 0 until group.length) {
                if (group.isTrackSupported(i)) {
                    val format = group.getTrackFormat(i)
                    val lang = format.language?.takeIf(String::isNotBlank)
                    val label = format.label?.takeIf(String::isNotBlank)
                        ?: lang?.let { Locale.forLanguageTag(it).displayLanguage.takeIf(String::isNotBlank) }
                        ?: "Audio Track ${list.size + 1}"
                    list.add(
                        PlaybackTrackItem(
                            id = "audio_${group.mediaTrackGroup.id}_$i",
                            type = C.TRACK_TYPE_AUDIO,
                            label = label,
                            language = lang,
                            isSelected = group.isTrackSelected(i),
                            group = group,
                            trackIndex = i
                        )
                    )
                }
            }
        }
        return list
    }

    fun extractSubtitleTracks(currentTracks: Tracks?): List<PlaybackTrackItem> {
        val list = mutableListOf<PlaybackTrackItem>()
        currentTracks?.groups?.filter { it.type == C.TRACK_TYPE_TEXT }?.forEach { group ->
            for (i in 0 until group.length) {
                if (group.isTrackSupported(i)) {
                    val format = group.getTrackFormat(i)
                    val lang = format.language?.takeIf(String::isNotBlank)
                    val label = format.label?.takeIf(String::isNotBlank)
                        ?: lang?.let { Locale.forLanguageTag(it).displayLanguage.takeIf(String::isNotBlank) }
                        ?: "Subtitle ${list.size + 1}"
                    list.add(
                        PlaybackTrackItem(
                            id = "sub_${group.mediaTrackGroup.id}_$i",
                            type = C.TRACK_TYPE_TEXT,
                            label = label,
                            language = lang,
                            isSelected = group.isTrackSelected(i),
                            group = group,
                            trackIndex = i
                        )
                    )
                }
            }
        }
        return list
    }

    fun matchesLanguage(trackLang: String?, trackLabel: String?, targetLangCode: String): Boolean {
        val target = targetLangCode.trim().lowercase(Locale.ROOT)
        if (target.isBlank() || target == "off" || target == "default") return false

        val lang = trackLang?.trim()?.lowercase(Locale.ROOT)?.replace('_', '-')
        val label = trackLabel?.trim()?.lowercase(Locale.ROOT)?.replace('_', '-')

        // 1. Check ISO 639-1 / 639-2 / BCP-47 language tags
        if (lang != null) {
            when (target) {
                "pt" -> if (lang == "pt" || lang == "por" || lang.startsWith("pt-") || lang.startsWith("por-")) return true
                "en" -> if (lang == "en" || lang == "eng" || lang.startsWith("en-") || lang.startsWith("eng-")) return true
                "es" -> if (lang == "es" || lang == "spa" || lang.startsWith("es-") || lang.startsWith("spa-")) return true
                "fr" -> if (lang == "fr" || lang == "fra" || lang == "fre" || lang.startsWith("fr-") || lang.startsWith("fra-")) return true
                "de" -> if (lang == "de" || lang == "deu" || lang == "ger" || lang.startsWith("de-") || lang.startsWith("deu-")) return true
                "it" -> if (lang == "it" || lang == "ita" || lang.startsWith("it-") || lang.startsWith("ita-")) return true
                else -> if (lang == target || lang.startsWith("$target-")) return true
            }
        }

        // 2. Check label strings
        if (label != null) {
            // Some sources supply only a language code alongside codec/channel
            // information, such as "ENG · AAC 5.1". Match whole tokens so "en"
            // cannot accidentally match an unrelated word.
            val languageCodes = when (target) {
                "pt" -> listOf("pt", "por")
                "en" -> listOf("en", "eng")
                "es" -> listOf("es", "spa")
                "fr" -> listOf("fr", "fra", "fre")
                "de" -> listOf("de", "deu", "ger")
                "it" -> listOf("it", "ita")
                else -> emptyList()
            }
            if (languageCodes.any { Regex("(?<![\\p{L}\\p{N}])$it(?![\\p{L}\\p{N}])").containsMatchIn(label) }) return true
            when (target) {
                "pt" -> if (label.contains("portugu") || label.contains("pt-pt") || label.contains("pt-br") || label == "pt" || label == "por") return true
                "en" -> if (label.contains("english") || label.contains("inglês") || label.contains("ingles") || label.contains("en-us") || label.contains("en-gb") || label == "en" || label == "eng") return true
                "es" -> if (label.contains("spanish") || label.contains("español") || label.contains("espanol") || label == "es" || label == "spa") return true
                "fr" -> if (label.contains("french") || label.contains("français") || label.contains("francês") || label.contains("frances") || label == "fr" || label == "fra" || label == "fre") return true
                "de" -> if (label.contains("german") || label.contains("deutsch") || label.contains("alemão") || label.contains("alemao") || label == "de" || label == "deu" || label == "ger") return true
                "it" -> if (label.contains("italian") || label.contains("italiano") || label == "it" || label == "ita") return true
                else -> if (label.contains(target)) return true
            }
        }

        return false
    }

    fun detectLanguageCode(trackLang: String?, trackLabel: String?): String? {
        val supported = listOf("pt", "en", "es", "fr", "de", "it")
        val matched = supported.firstOrNull { matchesLanguage(trackLang, trackLabel, it) }
        if (matched != null) return matched
        val code = trackLang?.trim()?.lowercase()
        if (!code.isNullOrBlank()) {
            return supported.firstOrNull { code == it || code.startsWith("$it-") || code.startsWith("${it}_") }
                ?: code.take(2)
        }
        return null
    }

    fun detectLanguageCode(track: PlaybackTrackItem): String? =
        detectLanguageCode(track.language, track.label)

    fun findBestAudioTrack(tracks: List<PlaybackTrackItem>, preferredLang: String): PlaybackTrackItem? {
        if (preferredLang == "default" || preferredLang.isBlank()) return null
        // 1. Try preferred language
        val preferred = tracks.firstOrNull { matchesLanguage(it.language, it.label, preferredLang) }
        if (preferred != null) return preferred

        // 2. If preferred was not English, fallback to English if available
        if (preferredLang != "en") {
            return tracks.firstOrNull { matchesLanguage(it.language, it.label, "en") }
        }
        return null
    }

    fun findBestSubtitleTrack(
        tracks: List<PlaybackTrackItem>,
        preferredLang: String,
        fallbackEnglish: Boolean
    ): PlaybackTrackItem? {
        if (preferredLang == "off" || preferredLang.isBlank()) return null

        // 1. Try preferred language
        val preferred = tracks.firstOrNull { matchesLanguage(it.language, it.label, preferredLang) }
        if (preferred != null) return preferred

        // 2. If preferred was not English, fallback to English if enabled
        if (fallbackEnglish && preferredLang != "en") {
            return tracks.firstOrNull { matchesLanguage(it.language, it.label, "en") }
        }
        return null
    }

    fun extractVideoTracks(currentTracks: Tracks?): List<PlaybackVideoTrackItem> {
        val list = mutableListOf<PlaybackVideoTrackItem>()
        currentTracks?.groups?.filter { it.type == C.TRACK_TYPE_VIDEO }?.forEach { group ->
            for (i in 0 until group.length) {
                if (group.isTrackSupported(i)) {
                    val format = group.getTrackFormat(i)
                    val width = format.width
                    val height = format.height
                    val bitrate = format.bitrate
                    val frameRate = format.frameRate
                    val resLabel = if (height > 0) "${height}p" else if (width > 0) "${width}w" else ""
                    val bitLabel = if (bitrate > 0) {
                        val mbps = bitrate / 1_000_000f
                        if (mbps >= 1f) String.format(Locale.ROOT, "%.1f Mbps", mbps)
                        else "${bitrate / 1_000} kbps"
                    } else ""
                    val label = listOfNotNull(resLabel.takeIf { it.isNotBlank() }, bitLabel.takeIf { it.isNotBlank() })
                        .joinToString(" · ")
                        .ifBlank { "Track ${list.size + 1}" }
                    list.add(
                        PlaybackVideoTrackItem(
                            id = "video_${group.mediaTrackGroup.id}_$i",
                            label = label,
                            width = width,
                            height = height,
                            bitrate = bitrate,
                            frameRate = frameRate,
                            isSelected = group.isTrackSelected(i),
                            group = group,
                            trackIndex = i
                        )
                    )
                }
            }
        }
        return list.sortedByDescending { it.height.takeIf { h -> h > 0 } ?: it.bitrate }
    }
}

@UnstableApi
data class PlaybackVideoTrackItem(
    val id: String,
    val label: String,
    val width: Int,
    val height: Int,
    val bitrate: Int,
    val frameRate: Float,
    val isSelected: Boolean,
    val group: Tracks.Group,
    val trackIndex: Int
)

enum class StreamQualityTier(val label: String) {
    UHD_4K("4K"),
    QHD_2K("2K"),
    FHD("FHD"),
    HD("HD"),
    SD("SD")
}

fun classifyStreamQuality(width: Int, height: Int): StreamQualityTier? {
    if (width <= 0 && height <= 0) return null
    val longSide = maxOf(width, height)
    val shortSide = if (width > 0 && height > 0) minOf(width, height) else height
    return when {
        shortSide >= 2160 || longSide >= 3840 -> StreamQualityTier.UHD_4K
        shortSide >= 1440 || longSide >= 2560 -> StreamQualityTier.QHD_2K
        shortSide >= 1080 || longSide >= 1920 -> StreamQualityTier.FHD
        shortSide >= 720 || longSide >= 1280 -> StreamQualityTier.HD
        else -> StreamQualityTier.SD
    }
}

