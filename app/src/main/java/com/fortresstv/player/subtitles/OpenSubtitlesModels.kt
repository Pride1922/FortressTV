package com.fortresstv.player.subtitles

data class OpenSubtitlesItem(
    val id: String,
    val fileId: Int,
    val fileName: String,
    val language: String,
    val release: String,
    val downloads: Int,
    val rating: Double,
    val hearingImpaired: Boolean
)

data class OpenSubtitlesSettings(
    val enabled: Boolean = false,
    val apiKey: String = "",
    val username: String = "",
    val password: String = "",
    val preferPtPt: Boolean = true,
    val autoSearch: Boolean = true
)

internal fun openSubtitlesLanguageQuery(preferredLanguage: String, preferPtPt: Boolean): String? =
    when (preferredLanguage.trim().lowercase()) {
        "off", "", "default" -> null
        "pt" -> if (preferPtPt) "pt-PT,pt,pt-BR" else "pt,pt-PT,pt-BR"
        else -> preferredLanguage.trim().lowercase()
    }

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun shouldAutoSearchOpenSubtitles(
    enabled: Boolean,
    autoSearch: Boolean,
    apiKey: String,
    username: String,
    password: String,
    preferredLanguage: String,
    availableTracks: List<Pair<String?, String?>>
): Boolean = enabled &&
    autoSearch &&
    apiKey.isNotBlank() &&
    username.isNotBlank() &&
    password.isNotBlank() &&
    openSubtitlesLanguageQuery(preferredLanguage, preferPtPt = true) != null &&
    availableTracks.none { (language, label) ->
        com.fortresstv.player.player.TrackLanguageMatcher.matchesLanguage(language, label, preferredLanguage)
    }
