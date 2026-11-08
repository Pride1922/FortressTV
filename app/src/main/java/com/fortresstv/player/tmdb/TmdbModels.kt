package com.fortresstv.player.tmdb

import androidx.compose.runtime.compositionLocalOf

data class TmdbSettings(
    val enabled: Boolean = false,
    val apiKey: String = "",
    val language: String = "pt-PT"
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

object TmdbLanguages {
    val supported = listOf(
        "pt-PT" to "Português (Portugal)",
        "pt-BR" to "Português (Brasil)",
        "en-US" to "English (US)",
        "es-ES" to "Español",
        "fr-FR" to "Français",
        "de-DE" to "Deutsch",
        "it-IT" to "Italiano"
    )

    fun labelFor(code: String): String =
        supported.firstOrNull { it.first.equals(code, ignoreCase = true) }?.second ?: code
}
