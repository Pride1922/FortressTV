package com.fortresstv.player

import com.fortresstv.player.tmdb.TmdbClient
import java.util.Locale

private val imdbMovieId = Regex("tt[0-9]+")

private class VodCardGroup<T>(
    val entry: T,
    var tmdb: Int?,
    var imdb: String?,
    var year: Int?
)

internal fun deduplicateCatalogMovies(entries: List<CatalogMovie>): List<CatalogMovie> =
    deduplicateMovieEntries(entries)

internal fun groupCatalogMovieVersions(entries: List<CatalogMovie>): List<List<CatalogMovie>> {
    val versions = linkedMapOf<Pair<String, Int>, MutableList<CatalogMovie>>()
    deduplicateMovieEntries(entries) { representative, entry ->
        versions.getOrPut(representative.catalog.sourceId to representative.movie.id) { mutableListOf() }.add(entry)
    }
    return versions.values.toList()
}

private fun deduplicateMovieEntries(
    entries: List<CatalogMovie>,
    onGrouped: ((CatalogMovie, CatalogMovie) -> Unit)? = null
): List<CatalogMovie> =
    groupVodEntries(
        entries = entries,
        sourceIdOf = { it.catalog.sourceId },
        itemIdOf = { it.movie.id },
        nameOf = { it.movie.name },
        yearOf = { it.movie.year },
        tmdbIdOf = { it.movie.tmdbId },
        imdbIdOf = { it.movie.imdbId },
        onGrouped = onGrouped
    )

internal fun deduplicateCatalogSeries(entries: List<CatalogSeries>): List<CatalogSeries> =
    groupVodEntries(
        entries = entries,
        sourceIdOf = { it.catalog.sourceId },
        itemIdOf = { it.series.id },
        nameOf = { it.series.name },
        yearOf = { it.series.year },
        tmdbIdOf = { it.series.tmdbId },
        imdbIdOf = { it.series.imdbId }
    )

private fun <T> groupVodEntries(
    entries: List<T>,
    sourceIdOf: (T) -> String,
    itemIdOf: (T) -> Int,
    nameOf: (T) -> String,
    yearOf: (T) -> String?,
    tmdbIdOf: (T) -> String?,
    imdbIdOf: (T) -> String?,
    onGrouped: ((T, T) -> Unit)? = null
): List<T> {
    val groups = mutableListOf<VodCardGroup<T>>()
    val byTmdb = mutableMapOf<Int, VodCardGroup<T>>()
    val byImdb = mutableMapOf<String, VodCardGroup<T>>()
    val byTitle = mutableMapOf<String, MutableList<VodCardGroup<T>>>()
    val seenStreams = mutableSetOf<Pair<String, Int>>()

    for (entry in entries) {
        val sourceId = sourceIdOf(entry)
        if (!seenStreams.add(sourceId to itemIdOf(entry))) continue

        val tmdb = tmdbIdOf(entry)?.trim()?.toIntOrNull()?.takeIf { it > 0 }
        val imdb = imdbIdOf(entry)?.trim()?.lowercase(Locale.ROOT)?.takeIf(imdbMovieId::matches)
        val (cleanedTitle, extractedYear) = TmdbClient.cleanMediaTitle(nameOf(entry))
        val year = yearOf(entry)?.trim()?.toIntOrNull()?.takeIf { it in 1000..9999 }
            ?: extractedYear?.toIntOrNull()?.takeIf { it in 1000..9999 }
        val normTitle = cleanedTitle.lowercase(Locale.ROOT)

        fun compatible(group: VodCardGroup<T>): Boolean =
            (tmdb == null || group.tmdb == null || tmdb == group.tmdb) &&
                (imdb == null || group.imdb == null || imdb == group.imdb) &&
                (year == null || group.year == null || year == group.year)

        val existing = tmdb?.let(byTmdb::get)?.takeIf(::compatible)
            ?: imdb?.let(byImdb::get)?.takeIf(::compatible)
            ?: normTitle.takeIf { it.isNotEmpty() }?.let(byTitle::get)?.firstOrNull { candidate ->
                compatible(candidate) && (year != null || candidate.year != null || sourceId == sourceIdOf(candidate.entry))
            }

        val group = existing ?: VodCardGroup(entry, tmdb, imdb, year).also { groups.add(it) }
        onGrouped?.invoke(group.entry, entry)
        if (group.tmdb == null) group.tmdb = tmdb
        if (group.imdb == null) group.imdb = imdb
        if (group.year == null) group.year = year
        tmdb?.let { byTmdb.putIfAbsent(it, group) }
        imdb?.let { byImdb.putIfAbsent(it, group) }
        if (normTitle.isNotEmpty()) {
            val matches = byTitle.getOrPut(normTitle) { mutableListOf() }
            if (group !in matches) matches.add(group)
        }
    }
    return groups.map(VodCardGroup<T>::entry)
}
