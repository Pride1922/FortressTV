package com.fortresstv.player.trakt

import com.fortresstv.player.xtream.XtreamCategory
import com.fortresstv.player.xtream.XtreamCatalog
import com.fortresstv.player.xtream.XtreamMovie
import com.fortresstv.player.xtream.XtreamSeries
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TraktRecommendationsServiceTest {

    @Test
    fun emptySeriesRecommendationsFallBackToTrendingSeries() {
        val series = TraktRecommendationItem(
            kind = TraktWatchlistKind.SHOW, title = "Sample Series", year = 2024,
            tmdbId = "456", imdbId = null, traktId = 12, slug = null
        )
        val result = TraktRecommendationsService.withTrendingFallback(TraktAuthResult.Success(emptyList())) {
            TraktAuthResult.Success(listOf(series))
        } as TraktAuthResult.Success

        val trending = series.copy(isTrending = true)
        assertEquals(listOf(trending), result.data)
        assertEquals(trending, TraktRecommendationItem.fromJson(trending.toJson()))
    }

    @Test
    fun temporaryRecommendationFailureDoesNotReplaceCachedCategoryWithTrending() {
        val failure = TraktAuthResult.Failure("Rate limited", statusCode = 429)
        assertEquals(failure, TraktRecommendationsService.withTrendingFallback(failure) {
            error("Trending should wait until the next refresh")
        })
    }

    @Test
    fun parseRecommendationFromJson() {
        val json = JSONObject("""
            {
                "title": "Challengers",
                "year": 2024,
                "ids": {
                    "trakt": 771234,
                    "slug": "challengers-2024",
                    "imdb": "tt16426418",
                    "tmdb": 937287
                }
            }
        """.trimIndent())

        val item = TraktRecommendationItem.fromJson(json, TraktWatchlistKind.MOVIE)
        assertNotNull(item)
        assertEquals("Challengers", item?.title)
        assertEquals(2024, item?.year)
        assertEquals("937287", item?.tmdbId)
        assertEquals("tt16426418", item?.imdbId)
        assertEquals(771234, item?.traktId)
        assertEquals(TraktWatchlistKind.MOVIE, item?.kind)

        val serialized = item!!.toJson()
        val restored = TraktRecommendationItem.fromJson(serialized)
        assertNotNull(restored)
        assertEquals(item.title, restored?.title)
        assertEquals(item.year, restored?.year)
        assertEquals(item.tmdbId, restored?.tmdbId)
        assertEquals(item.imdbId, restored?.imdbId)
        assertEquals(item.kind, restored?.kind)
    }

    @Test
    fun matchesRecommendationsToCatalogs() {
        val movie = XtreamMovie(
            id = 501,
            name = "Challengers",
            categoryId = "1",
            posterUrl = null,
            containerExtension = "mp4",
            rating = "7.8",
            year = "2024",
            plot = null,
            tmdbId = "937287"
        )
        val series = XtreamSeries(
            id = 601,
            name = "Shogun",
            categoryId = "2",
            coverUrl = null,
            rating = "8.7",
            year = "2024",
            plot = null,
            tmdbId = "126308"
        )
        val catalog = XtreamCatalog(
            sourceId = "src1",
            sourceName = "Source 1",
            movies = listOf(movie),
            series = listOf(series)
        )

        val recs = listOf(
            TraktRecommendationItem(
                kind = TraktWatchlistKind.MOVIE,
                title = "Challengers",
                year = 2024,
                tmdbId = "937287",
                imdbId = null,
                traktId = 1,
                slug = null
            ),
            TraktRecommendationItem(
                kind = TraktWatchlistKind.SHOW,
                title = "Shogun",
                year = 2024,
                tmdbId = "126308",
                imdbId = null,
                traktId = 2,
                slug = null
            ),
            TraktRecommendationItem(
                kind = TraktWatchlistKind.MOVIE,
                title = "Unmatched Recommendation",
                year = 2025,
                tmdbId = "999999",
                imdbId = null,
                traktId = 3,
                slug = null
            )
        )

        val matched = TraktRecommendationsService.matchRecommendations(recs, listOf(catalog))
        assertEquals(1, matched.movies.size)
        assertEquals(1, matched.series.size)
        assertEquals(501, matched.movies[0].movie.id)
        assertEquals(601, matched.series[0].series.id)

        val trendingSeries = recs[1].copy(isTrending = true)
        val unavailableSeries = recs[1].copy(title = "Unavailable Series", tmdbId = "999999")
        val fallback = TraktRecommendationsService.matchRecommendations(
            listOf(recs[0], unavailableSeries, trendingSeries), listOf(catalog)
        )
        assertEquals(matched.movies, fallback.movies)
        assertEquals(matched.series, fallback.series)
        assertTrue(fallback.includesTrending)
        assertTrue(fallback.seriesAreTrending)

        val trendingMovie = recs[0].copy(isTrending = true)
        val movieFallback = TraktRecommendationsService.matchRecommendations(
            listOf(recs[2], recs[1], trendingMovie), listOf(catalog)
        )
        assertEquals(matched.movies, movieFallback.movies)
        assertEquals(matched.series, movieFallback.series)
        assertTrue(movieFallback.includesTrending)

        val preferred = TraktRecommendationsService.matchRecommendations(listOf(trendingSeries) + recs, listOf(catalog))
        assertEquals(matched, preferred)
    }

    @Test
    fun filtersOutAdultContentAndParentalRestrictedItems() {
        val adultMovie = XtreamMovie(
            id = 701,
            name = "Adult Video XXX",
            categoryId = "adult_cat",
            posterUrl = null,
            containerExtension = "mp4",
            rating = "5.0",
            year = "2020",
            plot = null,
            tmdbId = "100"
        )
        val matureMovie = XtreamMovie(
            id = 702,
            name = "Deadpool",
            categoryId = "action_cat",
            posterUrl = null,
            containerExtension = "mp4",
            rating = "8.0",
            year = "2016",
            plot = null,
            ageRating = "18+",
            tmdbId = "200"
        )
        val kidsMovie = XtreamMovie(
            id = 703,
            name = "Finding Nemo",
            categoryId = "kids_cat",
            posterUrl = null,
            containerExtension = "mp4",
            rating = "8.2",
            year = "2003",
            plot = null,
            ageRating = "G",
            tmdbId = "300"
        )

        val catalog = XtreamCatalog(
            sourceId = "src1",
            sourceName = "Source 1",
            movieCategories = listOf(
                XtreamCategory("adult_cat", "XXX Adults"),
                XtreamCategory("action_cat", "Action"),
                XtreamCategory("kids_cat", "Kids & Family")
            ),
            movies = listOf(adultMovie, matureMovie, kidsMovie)
        )

        val recs = listOf(
            TraktRecommendationItem(TraktWatchlistKind.MOVIE, "Adult Video XXX", 2020, "100", null, 1, null),
            TraktRecommendationItem(TraktWatchlistKind.MOVIE, "Deadpool", 2016, "200", null, 2, null),
            TraktRecommendationItem(TraktWatchlistKind.MOVIE, "Finding Nemo", 2003, "300", null, 3, null)
        )

        // Child profile: hide adult content + max age 10
        val matchedForChild = TraktRecommendationsService.matchRecommendations(
            recommendations = recs,
            catalogs = listOf(catalog),
            hideAdultContent = true,
            maximumAge = 10,
            blockUnrated = true
        )

        assertEquals(1, matchedForChild.movies.size)
        assertEquals(703, matchedForChild.movies[0].movie.id)
    }

    @Test
    fun matchesTaggedVODTitlesWithCleanMediaTitle() {
        val taggedMovie = XtreamMovie(
            id = 801,
            name = "[EN] Top Gun: Maverick (2022) 4K UHD",
            categoryId = "1",
            posterUrl = null,
            containerExtension = "mp4",
            rating = "8.3",
            year = null, // Xtream often omits the year field when in the title
            plot = null,
            tmdbId = null // and often has no tmdb id
        )

        val catalog = XtreamCatalog(
            sourceId = "src1",
            sourceName = "Source 1",
            movies = listOf(taggedMovie)
        )

        val recs = listOf(
            TraktRecommendationItem(
                kind = TraktWatchlistKind.MOVIE,
                title = "Top Gun: Maverick",
                year = 2022,
                tmdbId = "361743",
                imdbId = "tt1745960",
                traktId = 555,
                slug = "top-gun-maverick-2022"
            )
        )

        val matched = TraktRecommendationsService.matchRecommendations(recs, listOf(catalog))
        assertEquals(1, matched.movies.size)
        assertEquals(801, matched.movies[0].movie.id)
    }

    @Test
    fun personalizedErrorFallsBackToTrending() {
        val trendingMovie = TraktRecommendationItem(
            kind = TraktWatchlistKind.MOVIE, title = "Gladiator II", year = 2024,
            tmdbId = "123", imdbId = null, traktId = 45, slug = null
        )
        // Non-429 failure (e.g. 500, 401, timeout) falls back to trending
        val failure500 = TraktAuthResult.Failure("Internal server error", statusCode = 500)
        val result500 = TraktRecommendationsService.withTrendingFallback(failure500) {
            TraktAuthResult.Success(listOf(trendingMovie))
        } as TraktAuthResult.Success
        assertEquals(listOf(trendingMovie.copy(isTrending = true)), result500.data)

        val failureNetwork = TraktAuthResult.Failure("Network timeout")
        val resultNetwork = TraktRecommendationsService.withTrendingFallback(failureNetwork) {
            TraktAuthResult.Success(listOf(trendingMovie))
        } as TraktAuthResult.Success
        assertEquals(listOf(trendingMovie.copy(isTrending = true)), resultNetwork.data)
    }

    @Test
    fun matchingHandlesDiacriticsAndYearTolerance() {
        val accentedMovie = XtreamMovie(
            id = 901,
            name = "Corações de Ferro",
            categoryId = "1",
            posterUrl = null,
            containerExtension = "mp4",
            rating = "7.6",
            year = "2024",
            plot = null,
            tmdbId = null
        )
        val yearDiffMovie = XtreamMovie(
            id = 902,
            name = "Dune: Part Two",
            categoryId = "1",
            posterUrl = null,
            containerExtension = "mp4",
            rating = "8.6",
            year = "2023", // Catalog lists 2023
            plot = null,
            tmdbId = null
        )
        val catalog = XtreamCatalog(
            sourceId = "src1",
            sourceName = "Source 1",
            movies = listOf(accentedMovie, yearDiffMovie)
        )

        val recs = listOf(
            // Trakt has unaccented title or same accented title
            TraktRecommendationItem(
                kind = TraktWatchlistKind.MOVIE,
                title = "Coracoes de Ferro",
                year = 2024,
                tmdbId = "999",
                imdbId = null,
                traktId = 1,
                slug = null
            ),
            // Trakt lists 2024 for release
            TraktRecommendationItem(
                kind = TraktWatchlistKind.MOVIE,
                title = "Dune: Part Two",
                year = 2024,
                tmdbId = "888",
                imdbId = null,
                traktId = 2,
                slug = null
            )
        )

        val matched = TraktRecommendationsService.matchRecommendations(recs, listOf(catalog))
        assertEquals(2, matched.movies.size)
        assertEquals(setOf(901, 902), matched.movies.map { it.movie.id }.toSet())
    }
}
