package com.fortresstv.player.xtream

import com.fortresstv.player.playlist.PlaylistChannel

data class XtreamCategory(
    val id: String,
    val name: String
)

data class XtreamMovie(
    val id: Int,
    val name: String,
    val categoryId: String?,
    val posterUrl: String?,
    val containerExtension: String,
    val rating: String?,
    val year: String?,
    val plot: String?,
    val ageRating: String? = null
)

data class XtreamMovieDetails(
    val plot: String?,
    val genre: String?,
    val duration: String?,
    val releaseDate: String?,
    val rating: String?,
    val director: String?,
    val cast: String?,
    val country: String?,
    val ageRating: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val trailerUrl: String?
)

data class XtreamSeries(
    val id: Int,
    val name: String,
    val categoryId: String?,
    val coverUrl: String?,
    val rating: String?,
    val year: String?,
    val plot: String?,
    val ageRating: String? = null
)

data class XtreamSeriesDetails(
    val plot: String?,
    val genre: String?,
    val releaseDate: String?,
    val rating: String?,
    val director: String?,
    val cast: String?,
    val duration: String?,
    val country: String?,
    val ageRating: String?,
    val coverUrl: String?,
    val backdropUrl: String?,
    val trailerUrl: String?,
    val episodes: List<XtreamEpisode>
)

data class XtreamEpisode(
    val id: String,
    val title: String,
    val season: Int,
    val episodeNumber: Int,
    val containerExtension: String,
    val posterUrl: String?,
    val plot: String?
)

data class XtreamCatalog(
    val sourceId: String,
    val sourceName: String,
    val liveCategories: List<XtreamCategory> = emptyList(),
    val movieCategories: List<XtreamCategory> = emptyList(),
    val seriesCategories: List<XtreamCategory> = emptyList(),
    val liveChannels: List<PlaylistChannel> = emptyList(),
    val movies: List<XtreamMovie> = emptyList(),
    val series: List<XtreamSeries> = emptyList()
)
