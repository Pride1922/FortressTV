package com.fortresstv.player.trakt

import com.fortresstv.player.ui.EnglishStrings
import com.fortresstv.player.ui.PortugueseStrings
import com.fortresstv.player.xtream.XtreamCatalog
import com.fortresstv.player.xtream.XtreamCategory
import com.fortresstv.player.xtream.XtreamMovie
import com.fortresstv.player.xtream.XtreamSeries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeaturedSeriesShelfResolverTest {

    private fun traktShow(
        title: String,
        tmdbId: String? = null,
        imdbId: String? = null,
        year: Int? = 2022
    ) = TraktRecommendationItem(
        kind = TraktWatchlistKind.SHOW,
        title = title,
        year = year,
        tmdbId = tmdbId,
        imdbId = imdbId,
        traktId = null,
        slug = null
    )

    private fun series(
        id: Int,
        name: String,
        categoryId: String? = "1",
        ageRating: String? = "TV-14",
        tmdbId: String? = null,
        imdbId: String? = null,
        addedEpochMs: Long? = null
    ) = XtreamSeries(
        id = id,
        name = name,
        categoryId = categoryId,
        coverUrl = null,
        rating = "8.0",
        year = "2022",
        plot = null,
        ageRating = ageRating,
        tmdbId = tmdbId,
        imdbId = imdbId,
        addedEpochMs = addedEpochMs
    )

    private fun catalog(
        sourceId: String,
        series: List<XtreamSeries>,
        categories: List<XtreamCategory> = listOf(XtreamCategory("1", "Drama"), XtreamCategory("2", "XXX Adult"))
    ) = XtreamCatalog(
        sourceId = sourceId,
        sourceName = sourceId,
        seriesCategories = categories,
        series = series
    )

    @Test
    fun connectedTraktMatchesByTmdbOrImdbDeduplicatesAcrossSourcesAndPreservesOrder() {
        val popular = listOf(
            traktShow("First Popular", tmdbId = "101", imdbId = "tt101"),
            traktShow("Missing Locally", tmdbId = "999", imdbId = "tt999"),
            traktShow("Second Popular", tmdbId = "202", imdbId = "tt202"),
            traktShow("Title Only Should Not Match", tmdbId = "303", imdbId = "tt303")
        )
        val sourceA = catalog(
            "sourceA",
            listOf(
                series(1, "Second Popular A", imdbId = "TT202"),
                series(2, "Title Only Should Not Match", tmdbId = null, imdbId = null),
                series(3, "First Popular A", tmdbId = "101", imdbId = "tt101")
            )
        )
        val sourceB = catalog(
            "sourceB",
            listOf(
                series(10, "First Popular B", tmdbId = "101", imdbId = "tt101"),
                series(11, "Second Popular B", tmdbId = "202", imdbId = "tt202")
            )
        )

        val shelf = FeaturedSeriesShelfResolver.resolve(
            traktConnected = true,
            popularShows = popular,
            catalogs = listOf(sourceA, sourceB)
        )

        assertEquals(FeaturedSeriesMode.TRAKT_POPULAR, shelf.mode)
        assertEquals(listOf("First Popular A", "Second Popular B"), shelf.items.map { it.series.name })
        assertEquals(listOf("sourceA", "sourceB"), shelf.items.map { it.catalog.sourceId })
    }

    @Test
    fun connectedTraktAppliesAdultAndAgeFiltersBeforeMatching() {
        val popular = listOf(
            traktShow("Adult Show", tmdbId = "1"),
            traktShow("Mature Show", tmdbId = "2"),
            traktShow("Unrated Show", tmdbId = "3"),
            traktShow("Family Show", tmdbId = "4")
        )
        val cat = catalog(
            "sourceA",
            listOf(
                series(1, "Adult Show", categoryId = "2", ageRating = "TV-MA", tmdbId = "1"),
                series(2, "Mature Show", categoryId = "1", ageRating = "18+", tmdbId = "2"),
                series(3, "Unrated Show", categoryId = "1", ageRating = null, tmdbId = "3"),
                series(4, "Family Show", categoryId = "1", ageRating = "TV-PG", tmdbId = "4")
            )
        )

        val shelf = FeaturedSeriesShelfResolver.resolve(
            traktConnected = true,
            popularShows = popular,
            catalogs = listOf(cat),
            hideAdultContent = true,
            maximumAge = 14,
            blockUnrated = true
        )

        assertEquals(FeaturedSeriesMode.TRAKT_POPULAR, shelf.mode)
        assertEquals(listOf("Family Show"), shelf.items.map { it.series.name })
    }

    @Test
    fun fallsBackToRecentlyAddedWhenTraktDisconnectedUnavailableOrNoEligibleMatches() {
        val older = 1_700_000_000_000L
        val newer = 1_710_000_000_000L
        val newest = 1_720_000_000_000L
        val sourceA = catalog(
            "sourceA",
            listOf(
                series(1, "Older Added", addedEpochMs = older, tmdbId = "500"),
                series(2, "No Date At All", addedEpochMs = null),
                series(3, "First Seen Later", addedEpochMs = null)
            )
        )
        val sourceB = catalog(
            "sourceB",
            listOf(
                series(10, "Newest Added", addedEpochMs = newest),
                series(11, "Adult Filtered", categoryId = "2", addedEpochMs = newest + 1000L)
            )
        )
        val firstSeen = mapOf("sourceA" to mapOf(3 to newer))

        // 1. Disconnected
        val disconnected = FeaturedSeriesShelfResolver.resolve(
            traktConnected = false,
            popularShows = listOf(traktShow("Some Show", tmdbId = "500")),
            catalogs = listOf(sourceA, sourceB),
            firstSeenBySource = firstSeen,
            hideAdultContent = true
        )
        assertEquals(FeaturedSeriesMode.RECENTLY_ADDED, disconnected.mode)
        assertEquals(listOf("Newest Added", "First Seen Later", "Older Added"), disconnected.items.map { it.series.name })

        // 2. Connected but Trakt unavailable (empty popular list)
        val unavailable = FeaturedSeriesShelfResolver.resolve(
            traktConnected = true,
            popularShows = emptyList(),
            catalogs = listOf(sourceA, sourceB),
            firstSeenBySource = firstSeen,
            hideAdultContent = true
        )
        assertEquals(FeaturedSeriesMode.RECENTLY_ADDED, unavailable.mode)
        assertEquals(listOf("Newest Added", "First Seen Later", "Older Added"), unavailable.items.map { it.series.name })

        // 3. Connected with popular list, but zero local matches
        val noMatches = FeaturedSeriesShelfResolver.resolve(
            traktConnected = true,
            popularShows = listOf(traktShow("Unmatched", tmdbId = "9999")),
            catalogs = listOf(sourceA, sourceB),
            firstSeenBySource = firstSeen,
            hideAdultContent = true
        )
        assertEquals(FeaturedSeriesMode.RECENTLY_ADDED, noMatches.mode)
        assertEquals(listOf("Newest Added", "First Seen Later", "Older Added"), noMatches.items.map { it.series.name })
    }

    @Test
    fun hidesShelfWhenNoEligibleSeriesHaveUsableRecencyDate() {
        val cat = catalog(
            "sourceA",
            listOf(
                series(1, "Baseline Series A", addedEpochMs = null),
                series(2, "Adult Only With Date", categoryId = "2", addedEpochMs = 1_720_000_000_000L)
            )
        )

        val shelf = FeaturedSeriesShelfResolver.resolve(
            traktConnected = false,
            popularShows = emptyList(),
            catalogs = listOf(cat),
            firstSeenBySource = emptyMap(),
            hideAdultContent = true
        )

        assertEquals(FeaturedSeriesMode.HIDDEN, shelf.mode)
        assertTrue(shelf.items.isEmpty())
    }

    @Test
    fun boundsShelfSizeToExistingLimit() {
        val manySeries = (1..40).map { i ->
            series(i, "Series $i", addedEpochMs = 1_700_000_000_000L + i * 1000L, tmdbId = i.toString())
        }
        val popular = (1..40).map { i -> traktShow("Series $i", tmdbId = i.toString()) }
        val cat = catalog("sourceA", manySeries)

        val traktShelf = FeaturedSeriesShelfResolver.resolve(
            traktConnected = true,
            popularShows = popular,
            catalogs = listOf(cat)
        )
        assertEquals(25, traktShelf.items.size)
        assertEquals("Series 1", traktShelf.items.first().series.name)

        val recentShelf = FeaturedSeriesShelfResolver.resolve(
            traktConnected = false,
            popularShows = emptyList(),
            catalogs = listOf(cat)
        )
        assertEquals(25, recentShelf.items.size)
        assertEquals("Series 40", recentShelf.items.first().series.name)
        assertEquals("Series 16", recentShelf.items.last().series.name)
    }

    @Test
    fun bilingualShelfTitlesMatchEnglishAndPtPtRequirements() {
        assertEquals("Popular on Trakt", EnglishStrings.homePopularOnTraktSeries)
        assertEquals("Séries populares no Trakt", PortugueseStrings.homePopularOnTraktSeries)
        assertEquals("Recently Added Series", EnglishStrings.homeRecentlyAddedSeries)
        assertEquals("Séries adicionadas recentemente", PortugueseStrings.homeRecentlyAddedSeries)
        assertEquals("Popular on Trakt", EnglishStrings.homePopularOnTraktMovies)
        assertEquals("Filmes populares no Trakt", PortugueseStrings.homePopularOnTraktMovies)
        assertEquals("Recently Added Movies", EnglishStrings.homeFeaturedMovies)
        assertEquals("Filmes adicionados recentemente", PortugueseStrings.homeFeaturedMovies)
    }

    @Test
    fun featuredMoviesShelfMatchesTraktPopularDeduplicatesAndFallsBackToRecentlyAdded() {
        val traktMovies = listOf(
            TraktRecommendationItem(TraktWatchlistKind.MOVIE, "Movie 1", 2024, "101", "tt101", null, null),
            TraktRecommendationItem(TraktWatchlistKind.MOVIE, "Movie 2", 2024, "202", "tt202", null, null)
        )
        val movieCats = listOf(XtreamCategory("1", "Action"), XtreamCategory("2", "XXX"))
        fun movie(id: Int, name: String, cat: String = "1", tmdb: String? = null, imdb: String? = null, added: Long? = null) =
            XtreamMovie(id, name, cat, null, "mkv", "7.5", "2024", null, "PG-13", tmdb, imdb, added)

        val srcA = XtreamCatalog(
            sourceId = "srcA",
            sourceName = "A",
            movieCategories = movieCats,
            movies = listOf(
                movie(1, "Movie 1 A", tmdb = "101", added = 1_700_000_000_000L),
                movie(2, "Movie No Date", added = null)
            )
        )
        val srcB = XtreamCatalog(
            sourceId = "srcB",
            sourceName = "B",
            movieCategories = movieCats,
            movies = listOf(
                movie(10, "Movie 1 B", tmdb = "101", added = 1_720_000_000_000L),
                movie(11, "Movie 2 B", imdb = "TT202", added = 1_710_000_000_000L),
                movie(12, "Movie Adult", cat = "2", added = 1_730_000_000_000L)
            )
        )

        val traktShelf = FeaturedMoviesShelfResolver.resolve(
            traktConnected = true,
            popularMovies = traktMovies,
            catalogs = listOf(srcA, srcB),
            hideAdultContent = true
        )
        assertEquals(FeaturedSeriesMode.TRAKT_POPULAR, traktShelf.mode)
        assertEquals(listOf("Movie 1 A", "Movie 2 B"), traktShelf.items.map { it.movie.name })

        val recentShelf = FeaturedMoviesShelfResolver.resolve(
            traktConnected = false,
            popularMovies = traktMovies,
            catalogs = listOf(srcA, srcB),
            firstSeenBySource = mapOf("srcA" to mapOf(2 to 1_725_000_000_000L)),
            hideAdultContent = true
        )
        assertEquals(FeaturedSeriesMode.RECENTLY_ADDED, recentShelf.mode)
        assertEquals(
            listOf("Movie No Date", "Movie 1 B", "Movie 2 B", "Movie 1 A"),
            recentShelf.items.map { it.movie.name }
        )

        val hiddenShelf = FeaturedMoviesShelfResolver.resolve(
            traktConnected = false,
            popularMovies = emptyList(),
            catalogs = listOf(srcA.copy(movies = listOf(movie(2, "Movie No Date", added = null)))),
            firstSeenBySource = emptyMap()
        )
        assertEquals(FeaturedSeriesMode.HIDDEN, hiddenShelf.mode)
        assertTrue(hiddenShelf.items.isEmpty())
    }
}
