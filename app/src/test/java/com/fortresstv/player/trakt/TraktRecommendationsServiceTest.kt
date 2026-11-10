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
}
