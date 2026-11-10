package com.fortresstv.player.tmdb

import com.fortresstv.player.extractYouTubeVideoId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TmdbClientTest {

    @Test
    fun testNormalizeImdbId() {
        assertEquals("tt1234567", TmdbClient.normalizeImdbId("tt1234567"))
        assertEquals("tt1234567", TmdbClient.normalizeImdbId("TT1234567"))
        assertEquals("tt0944947", TmdbClient.normalizeImdbId("0944947"))
        assertEquals("tt903747", TmdbClient.normalizeImdbId(" 903747 "))
        assertNull(TmdbClient.normalizeImdbId(""))
        assertNull(TmdbClient.normalizeImdbId("   "))
    }

    @Test
    fun testCleanMediaTitleExtraction() {
        val (title1, year1) = TmdbClient.cleanMediaTitle("PT: Glória (2021) [1080p]")
        assertEquals("Glória", title1)
        assertEquals("2021", year1)

        val (title2, year2) = TmdbClient.cleanMediaTitle("[PT-PT] Oppenheimer [4K UHD HDR]")
        assertEquals("Oppenheimer", title2)
        assertNull(year2)

        val (title3, year3) = TmdbClient.cleanMediaTitle("Dune: Part Two (2024) [LEGENDADO] 1080p HEVC")
        assertEquals("Dune Part Two", title3)
        assertEquals("2024", year3)

        val (title4, year4) = TmdbClient.cleanMediaTitle("Stranger Things [DUBLADO] [FHD]")
        assertEquals("Stranger Things", title4)
        assertNull(year4)
    }

    @Test
    fun testTmdbLanguages() {
        assertEquals("Auto (Match Profile)", TmdbLanguages.labelFor(TmdbLanguages.AUTO))
        assertEquals("Português (Portugal)", TmdbLanguages.labelFor("pt-PT"))
        assertEquals("Português (Brasil)", TmdbLanguages.labelFor("pt-BR"))
        assertEquals("English (US)", TmdbLanguages.labelFor("en-US"))
        assertEquals("custom-LANG", TmdbLanguages.labelFor("custom-LANG"))
        assertTrue(TmdbLanguages.supported.any { it.first == "pt-PT" })
        assertTrue(TmdbLanguages.supported.any { it.first == TmdbLanguages.AUTO })
    }

    @Test
    fun testTmdbMetadataDefaults() {
        val meta = TmdbMetadata(
            tmdbId = 123,
            title = "Inception",
            overview = "Um ladrão que rouba segredos corporativos...",
            posterUrl = "https://image.tmdb.org/t/p/w500/poster.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/backdrop.jpg",
            rating = "8.8",
            releaseDate = "2010-07-16",
            genres = listOf("Ação", "Ficção Científica"),
            director = "Christopher Nolan",
            cast = listOf("Leonardo DiCaprio", "Joseph Gordon-Levitt"),
            trailerUrl = "https://www.youtube.com/watch?v=YoHD9XEInc0"
        )

        assertEquals(123, meta.tmdbId)
        assertEquals("Inception", meta.title)
        assertEquals("Christopher Nolan", meta.director)
        assertEquals(2, meta.cast.size)
        assertEquals("https://www.youtube.com/watch?v=YoHD9XEInc0", meta.trailerUrl)
    }

    @Test
    fun testTmdbSettingsDefaults() {
        val settings = TmdbSettings()
        assertEquals(false, settings.enabled)
        assertEquals("", settings.apiKey)
        assertEquals(TmdbLanguages.AUTO, settings.language)
    }

    @Test
    fun testParseSeasonMetadata() = kotlinx.coroutines.runBlocking {
        val client = TmdbClient("dummy-key")
        val json = org.json.JSONObject("""{
            "season_number": 1,
            "name": "Temporada 1",
            "overview": "Sinopse da temporada",
            "episodes": [
                {
                    "episode_number": 1,
                    "name": "Piloto",
                    "overview": "Patrick Jane e a equipa investigam...",
                    "still_path": "/still1.jpg",
                    "vote_average": 8.2
                },
                {
                    "episode_number": 2,
                    "name": "Cabelo Vermelho",
                    "overview": "Uma nova pista surge...",
                    "still_path": "/still2.jpg",
                    "vote_average": 7.8
                }
            ]
        }""")

        val season = client.parseSeasonMetadata(
            showId = 5920,
            json = json,
            seasonOverview = "Sinopse da temporada",
            language = "pt-PT"
        )

        assertEquals(1, season.seasonNumber)
        assertEquals("Temporada 1", season.name)
        assertEquals("Sinopse da temporada", season.overview)
        assertEquals(2, season.episodes.size)

        val ep1 = season.episodes[1]
        assertNotNull(ep1)
        assertEquals(1, ep1?.episodeNumber)
        assertEquals("Piloto", ep1?.name)
        assertEquals("Patrick Jane e a equipa investigam...", ep1?.overview)
        assertEquals("https://image.tmdb.org/t/p/w500/still1.jpg", ep1?.stillUrl)
        assertEquals("8.2", ep1?.rating)

        val ep2 = season.episodes[2]
        assertNotNull(ep2)
        assertEquals(2, ep2?.episodeNumber)
        assertEquals("Cabelo Vermelho", ep2?.name)
        assertEquals("Uma nova pista surge...", ep2?.overview)
        assertEquals("https://image.tmdb.org/t/p/w500/still2.jpg", ep2?.stillUrl)
        assertEquals("7.8", ep2?.rating)
    }

    @Test
    fun testParseTrailerUrlPrioritizesProfileLanguage() {
        val client = TmdbClient("dummy-key")
        val videosJson = org.json.JSONArray("""[
            {
                "site": "YouTube",
                "type": "Trailer",
                "key": "enTrailer01",
                "iso_639_1": "en",
                "official": true
            },
            {
                "site": "YouTube",
                "type": "Trailer",
                "key": "esTrailer01",
                "iso_639_1": "es",
                "official": true
            },
            {
                "site": "YouTube",
                "type": "Trailer",
                "key": "ptTrailer01",
                "iso_639_1": "pt",
                "iso_3166_1": "PT",
                "official": true
            }
        ]""")

        // Portuguese profile prioritizes pt-PT
        val ptUrl = client.parseTrailerUrl(videosJson, preferredLanguage = "pt-PT")
        assertEquals("https://www.youtube.com/watch?v=ptTrailer01", ptUrl)

        // Spanish profile prioritizes es
        val esUrl = client.parseTrailerUrl(videosJson, preferredLanguage = "es-ES")
        assertEquals("https://www.youtube.com/watch?v=esTrailer01", esUrl)
    }

    @Test
    fun testParseTrailerUrlFallsBackToEnglishWhenNoProfileLanguageTrailer() {
        val client = TmdbClient("dummy-key")
        val videosJson = org.json.JSONArray("""[
            {
                "site": "YouTube",
                "type": "Trailer",
                "key": "enTrailer02",
                "iso_639_1": "en"
            },
            {
                "site": "YouTube",
                "type": "Trailer",
                "key": "enTrailer01",
                "iso_639_1": "en",
                "official": true
            }
        ]""")

        // Profile is French, but only English trailers exist -> falls back to English
        val url = client.parseTrailerUrl(videosJson, preferredLanguage = "fr-FR")
        assertEquals("https://www.youtube.com/watch?v=enTrailer01", url)
    }

    @Test
    fun testExtractYouTubeVideoId() {
        val id = "abcDEfg12_-"
        assertEquals(id, extractYouTubeVideoId(id))
        assertEquals(id, extractYouTubeVideoId("https://www.youtube.com/watch?feature=shared&v=$id&t=10s"))
        assertEquals(id, extractYouTubeVideoId("https://youtu.be/$id"))
        assertEquals(id, extractYouTubeVideoId("https://www.youtube-nocookie.com/embed/$id"))
        assertEquals(id, extractYouTubeVideoId("https://www.youtube.com/shorts/$id"))
        assertNull(extractYouTubeVideoId(""))
        assertNull(extractYouTubeVideoId(null))
        assertNull(extractYouTubeVideoId("https://example.com/watch?v=$id"))
        assertNull(extractYouTubeVideoId("https://www.youtube.com/watch?v=${id}extra"))
    }
}
