package com.fortresstv.player

import java.util.Locale

private val trailingMovieYear = Regex("""\s*\((\d{4})\)\s*$""")
private val movieTitleWhitespace = Regex("""\s+""")
private val imdbMovieId = Regex("tt[0-9]+")

private data class MovieTitleYear(val title: String, val year: Int)

private class MovieCardGroup(val entry: CatalogMovie, var tmdb: Int?, var imdb: String?)

internal fun deduplicateCatalogMovies(entries: List<CatalogMovie>): List<CatalogMovie> {
    val groups = mutableListOf<MovieCardGroup>()
    val byTmdb = mutableMapOf<Int, MovieCardGroup>()
    val byImdb = mutableMapOf<String, MovieCardGroup>()
    val byTitleYear = mutableMapOf<MovieTitleYear, MutableList<MovieCardGroup>>()
    val seenStreams = mutableSetOf<Pair<String, Int>>()
    for (entry in entries) {
        val movie = entry.movie
        if (!seenStreams.add(entry.catalog.sourceId to movie.id)) continue
        val tmdb = movie.tmdbId?.trim()?.toIntOrNull()?.takeIf { it > 0 }
        val imdb = movie.imdbId?.trim()?.lowercase(Locale.ROOT)?.takeIf(imdbMovieId::matches)
        val suffix = trailingMovieYear.find(movie.name)
        val year = movie.year?.trim()?.toIntOrNull()?.takeIf { it in 1000..9999 }
            ?: suffix?.groupValues?.get(1)?.toIntOrNull()
        val title = (if (suffix != null && suffix.groupValues[1].toIntOrNull() == year) {
            movie.name.substring(0, suffix.range.first)
        } else movie.name).trim().lowercase(Locale.ROOT).replace(movieTitleWhitespace, " ")
        val titleYear = year?.takeIf { title.isNotEmpty() }?.let { MovieTitleYear(title, it) }
        fun compatible(group: MovieCardGroup) =
            (tmdb == null || group.tmdb == null || tmdb == group.tmdb) &&
                (imdb == null || group.imdb == null || imdb == group.imdb)

        val existing = tmdb?.let(byTmdb::get)?.takeIf(::compatible)
            ?: imdb?.let(byImdb::get)?.takeIf(::compatible)
            ?: titleYear?.let(byTitleYear::get)?.filter {
                it.entry.catalog.sourceId != entry.catalog.sourceId && compatible(it)
            }?.singleOrNull()
        val group = existing ?: MovieCardGroup(entry, tmdb, imdb).also { groups.add(it) }
        if (group.tmdb == null) group.tmdb = tmdb
        if (group.imdb == null) group.imdb = imdb
        tmdb?.let { byTmdb.putIfAbsent(it, group) }
        imdb?.let { byImdb.putIfAbsent(it, group) }
        titleYear?.let {
            val matches = byTitleYear.getOrPut(it) { mutableListOf() }
            if (group !in matches) matches.add(group)
        }
    }
    return groups.map(MovieCardGroup::entry)
}
