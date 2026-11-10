package com.fortresstv.player.trakt

import com.fortresstv.player.xtream.XtreamCatalog
import com.fortresstv.player.xtream.XtreamEpisode
import com.fortresstv.player.xtream.XtreamMovie
import com.fortresstv.player.xtream.XtreamSeries
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TraktWatchedSyncServiceTest {

    @Test
    fun parseWatchedMovieFromJson() {
        val json = JSONObject("""
            {
                "plays": 2,
                "last_watched_at": "2024-03-01T12:00:00.000Z",
                "movie": {
                    "title": "Gladiator II",
                    "year": 2024,
                    "ids": {
                        "trakt": 9991,
                        "slug": "gladiator-ii-2024",
                        "imdb": "tt2104996",
                        "tmdb": 558449
                    }
                }
            }
        """.trimIndent())

        val movie = TraktWatchedMovie.fromJson(json)
        assertNotNull(movie)
        assertEquals("Gladiator II", movie?.title)
        assertEquals(2024, movie?.year)
        assertEquals("558449", movie?.tmdbId)
        assertEquals("tt2104996", movie?.imdbId)
        assertEquals(9991, movie?.traktId)
        assertEquals(2, movie?.plays)
        assertEquals("2024-03-01T12:00:00.000Z", movie?.lastWatchedAt)
    }

    @Test
    fun parseWatchedShowWithSeasonsAndEpisodes() {
        val json = JSONObject("""
            {
                "plays": 12,
                "last_watched_at": "2024-02-15T20:30:00.000Z",
                "show": {
                    "title": "Severance",
                    "year": 2022,
                    "ids": {
                        "trakt": 156321,
                        "slug": "severance",
                        "imdb": "tt11280740",
                        "tmdb": 95396
                    }
                },
                "seasons": [
                    {
                        "number": 1,
                        "episodes": [
                            { "number": 1, "plays": 1 },
                            { "number": 2, "plays": 2 },
                            { "number": 3, "plays": 1 }
                        ]
                    },
                    {
                        "number": 2,
                        "episodes": [
                            { "number": 1, "plays": 1 }
                        ]
                    }
                ]
            }
        """.trimIndent())

        val show = TraktWatchedShow.fromJson(json)
        assertNotNull(show)
        assertEquals("Severance", show?.title)
        assertEquals(2022, show?.year)
        assertEquals("95396", show?.tmdbId)
        assertEquals("tt11280740", show?.imdbId)
        assertEquals(156321, show?.traktId)
        assertEquals(2, show?.seasons?.size)
        assertEquals(setOf(1, 2, 3), show?.seasons?.get(1))
        assertEquals(setOf(1), show?.seasons?.get(2))

        // Test roundtrip serialization to and from JSON
        val serialized = show!!.toJson()
        val restored = TraktWatchedShow.fromJson(serialized)
        assertNotNull(restored)
        assertEquals(show.title, restored?.title)
        assertEquals(show.year, restored?.year)
        assertEquals(show.tmdbId, restored?.tmdbId)
        assertEquals(show.seasons, restored?.seasons)
    }

    @Test
    fun buildMovieAndEpisodePayloads() {
        val moviePayload = TraktSyncPayloadBuilder.moviePayload(
            title = "Oppenheimer",
            year = 2023,
            tmdbId = "872585",
            imdbId = "tt15398776"
        )
        val moviesArr = moviePayload.getJSONArray("movies")
        assertEquals(1, moviesArr.length())
        val movieObj = moviesArr.getJSONObject(0)
        assertEquals("Oppenheimer", movieObj.getString("title"))
        assertEquals(2023, movieObj.getInt("year"))
        assertEquals(872585, movieObj.getJSONObject("ids").getInt("tmdb"))
        assertEquals("tt15398776", movieObj.getJSONObject("ids").getString("imdb"))

        val epPayload = TraktSyncPayloadBuilder.episodePayload(
            showTitle = "The Last of Us",
            showYear = 2023,
            showTmdbId = "100088",
            showImdbId = "tt3581920",
            seasonNumber = 1,
            episodeNumber = 3
        )
        val showsArr = epPayload.getJSONArray("shows")
        assertEquals(1, showsArr.length())
        val showObj = showsArr.getJSONObject(0)
        assertEquals("The Last of Us", showObj.getString("title"))
        val seasonsArr = showObj.getJSONArray("seasons")
        assertEquals(1, seasonsArr.length())
        assertEquals(1, seasonsArr.getJSONObject(0).getInt("number"))
        val epsArr = seasonsArr.getJSONObject(0).getJSONArray("episodes")
        assertEquals(1, epsArr.length())
        assertEquals(3, epsArr.getJSONObject(0).getInt("number"))
    }

    @Test
    fun normalizeTitleRemovesPunctuationAndSpacing() {
        assertEquals("breakingbad", TraktWatchedSyncService.normalizeTitle("Breaking Bad"))
        assertEquals("marvelsdaredevil", TraktWatchedSyncService.normalizeTitle("Marvel's Daredevil!"))
        assertEquals("topgunmaverick", TraktWatchedSyncService.normalizeTitle("Top Gun: Maverick"))
    }
}
