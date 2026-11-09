package com.fortresstv.player.omdb

import androidx.compose.runtime.compositionLocalOf

data class OmdbSettings(
    val enabled: Boolean = false,
    val apiKey: String = ""
)

val LocalOmdbSettings = compositionLocalOf { OmdbSettings() }

data class OmdbMetadata(
    val title: String? = null,
    val year: String? = null,
    val rated: String? = null,
    val released: String? = null,
    val runtime: String? = null,
    val genre: String? = null,
    val director: String? = null,
    val writer: String? = null,
    val actors: String? = null,
    val plot: String? = null,
    val awards: String? = null,
    val poster: String? = null,
    val imdbRating: String? = null,
    val imdbVotes: String? = null,
    val imdbId: String? = null,
    val type: String? = null,
    val rottenTomatoes: String? = null,
    val metascore: String? = null,
    val boxOffice: String? = null
) {
    val hasRatings: Boolean
        get() = (!imdbRating.isNullOrBlank() && imdbRating != "N/A") ||
            (!rottenTomatoes.isNullOrBlank() && rottenTomatoes != "N/A") ||
            (!metascore.isNullOrBlank() && metascore != "N/A")

    val hasAwards: Boolean
        get() = !awards.isNullOrBlank() && !awards.equals("N/A", ignoreCase = true)
}
