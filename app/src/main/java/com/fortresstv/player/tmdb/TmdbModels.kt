package com.fortresstv.player.tmdb

import androidx.compose.runtime.compositionLocalOf

data class TmdbSettings(
    val enabled: Boolean = false,
    val apiKey: String = "",
    val language: String = TmdbLanguages.AUTO
)

val LocalTmdbSettings = compositionLocalOf { TmdbSettings() }

data class TmdbMetadata(
    val tmdbId: Int,
    val title: String,
    val overview: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val rating: String? = null,
    val releaseDate: String? = null,
    val genres: List<String> = emptyList(),
    val director: String? = null,
    val cast: List<String> = emptyList(),
    val trailerUrl: String? = null
)

data class TmdbEpisodeMetadata(
    val episodeNumber: Int,
    val name: String? = null,
    val overview: String? = null,
    val stillUrl: String? = null,
    val rating: String? = null
)

data class TmdbSeasonMetadata(
    val seasonNumber: Int,
    val name: String? = null,
    val overview: String? = null,
    val episodes: Map<Int, TmdbEpisodeMetadata> = emptyMap()
)

object TmdbLanguages {
    const val AUTO = "auto"

    val supported = listOf(
        AUTO to "Auto (Match Profile)",
        "pt-PT" to "Português (Portugal)",
        "en-US" to "English (US)",
        "pt-BR" to "Português (Brasil)",
        "es-ES" to "Español",
        "fr-FR" to "Français",
        "de-DE" to "Deutsch",
        "it-IT" to "Italiano"
    )

    fun labelFor(code: String): String =
        supported.firstOrNull { it.first.equals(code, ignoreCase = true) }?.second ?: code
}
