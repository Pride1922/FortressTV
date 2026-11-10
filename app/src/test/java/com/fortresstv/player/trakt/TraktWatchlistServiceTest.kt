package com.fortresstv.player.trakt

import com.fortresstv.player.xtream.XtreamCatalog
import com.fortresstv.player.xtream.XtreamMovie
import com.fortresstv.player.xtream.XtreamSeries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TraktWatchlistServiceTest {

    @Test
    fun matchesMoviesAndSeriesByTmdbId() {
        val movie = XtreamMovie(
            id = 101,
            name = "Inception (2010)",
            categoryId = "1",
            posterUrl = null,
            containerExtension = "mp4",
            rating = "8.8",
            year = "2010",
            plot = null,
            tmdbId = "27205",
            imdbId = null
        )

        val series = XtreamSeries(
            id = 201,
            name = "Breaking Bad",
            categoryId = "2",
            coverUrl = null,
            rating = "9.5",
            year = "2008",
            plot = null,
            tmdbId = "1396",
            imdbId = null
        )

        val catalog = XtreamCatalog(
            sourceId = "src1",
            sourceName = "Source 1",
            movies = listOf(movie),
            series = listOf(series)
        )

        val watchlist = listOf(
            TraktWatchlistItem(
                kind = TraktWatchlistKind.MOVIE,
                title = "Inception",
                year = 2010,
                tmdbId = "27205",
                imdbId = null,
                traktId = null,
                slug = null,
                listedAt = null
            ),
            TraktWatchlistItem(
                kind = TraktWatchlistKind.SHOW,
                title = "Breaking Bad",
                year = 2008,
                tmdbId = "1396",
                imdbId = null,
                traktId = null,
                slug = null,
                listedAt = null
            ),
            TraktWatchlistItem(
                kind = TraktWatchlistKind.MOVIE,
                title = "Unmatched Movie",
                year = 2023,
                tmdbId = "999999",
                imdbId = null,
                traktId = null,
                slug = null,
                listedAt = null
            )
        )

        val matched = TraktWatchlistService.matchWatchlist(watchlist, listOf(catalog))
        assertEquals(1, matched.movies.size)
        assertEquals(1, matched.series.size)
        assertEquals(101, matched.movies[0].movie.id)
        assertEquals(201, matched.series[0].series.id)
    }

    @Test
    fun matchesByImdbIdFallback() {
        val movie = XtreamMovie(
            id = 102,
            name = "Interstellar",
            categoryId = "1",
            posterUrl = null,
            containerExtension = "mp4",
            rating = "8.6",
            year = "2014",
            plot = null,
            tmdbId = null,
            imdbId = "tt0816692"
        )

        val catalog = XtreamCatalog(
            sourceId = "src1",
            sourceName = "Source 1",
            movies = listOf(movie),
            series = emptyList()
        )

        val watchlist = listOf(
            TraktWatchlistItem(
                kind = TraktWatchlistKind.MOVIE,
                title = "Interstellar",
                year = 2014,
                tmdbId = "157336",
                imdbId = "tt0816692",
                traktId = null,
                slug = null,
                listedAt = null
            )
        )

        val matched = TraktWatchlistService.matchWatchlist(watchlist, listOf(catalog))
        assertEquals(1, matched.movies.size)
        assertEquals(102, matched.movies[0].movie.id)
    }

    @Test
    fun matchesByTitleAndYearWhenNoIdsPresent() {
        val movie = XtreamMovie(
            id = 103,
            name = "Gladiator",
            categoryId = "1",
            posterUrl = null,
            containerExtension = "mp4",
            rating = "8.5",
            year = "2000",
            plot = null,
            tmdbId = null,
            imdbId = null
        )

        val catalog = XtreamCatalog(
            sourceId = "src1",
            sourceName = "Source 1",
            movies = listOf(movie),
            series = emptyList()
        )

        val watchlist = listOf(
            TraktWatchlistItem(
                kind = TraktWatchlistKind.MOVIE,
                title = "Gladiator",
                year = 2000,
                tmdbId = "98",
                imdbId = "tt0172495",
                traktId = null,
                slug = null,
                listedAt = null
            )
        )

        val matched = TraktWatchlistService.matchWatchlist(watchlist, listOf(catalog))
        assertEquals(1, matched.movies.size)
        assertEquals(103, matched.movies[0].movie.id)
    }
}
