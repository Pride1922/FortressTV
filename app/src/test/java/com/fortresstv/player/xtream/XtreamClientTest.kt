package com.fortresstv.player.xtream

import com.fortresstv.player.source.ContentSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class XtreamClientTest {
    @Test
    fun sanitizesCatalogCacheWithoutDroppingEpgFallbackId() {
        val client = XtreamClient(
            sourceId = "test-source",
            source = ContentSource.Xtream("https://example.com", "Test", "user", "pass")
        )

        val sanitized = client.sanitizeCatalogResponse(
            """[{"stream_id":1,"custom_sid":"rtp1.pt","direct_source":"https://secret.example/live"}]"""
        )

        assertNotNull(sanitized)
        val item = org.json.JSONArray(sanitized).getJSONObject(0)
        assertEquals("rtp1.pt", item.getString("custom_sid"))
        assertEquals(false, item.has("direct_source"))
        assertNull(client.sanitizeCatalogResponse("not-json"))
    }

    @Test
    fun sanitizesNestedVodDetailsBeforeCaching() {
        val client = XtreamClient(
            sourceId = "test-source",
            source = ContentSource.Xtream("https://example.com", "Test", "user", "pass")
        )
        val sanitized = client.sanitizeCachedResponse(
            """{"info":{"plot":"Story"},"movie_data":{"stream_id":42,"direct_source":"https://secret.example/movie"}}"""
        )

        assertNotNull(sanitized)
        val root = org.json.JSONObject(sanitized!!)
        assertEquals("Story", root.getJSONObject("info").getString("plot"))
        assertEquals(false, root.getJSONObject("movie_data").has("direct_source"))
    }

    @Test
    fun normalizesServerAndPlayerApiUrls() {
        assertEquals("https://example.com:8443", XtreamClient.normalizeServerUrl("https://example.com:8443/"))
        assertEquals(
            "https://example.com:8443",
            XtreamClient.normalizeServerUrl("https://example.com:8443/player_api.php?username=hidden&password=hidden")
        )
        assertEquals("http://iptv.provider.tv", XtreamClient.normalizeServerUrl("http://iptv.provider.tv:80/player_api.php?foo=bar").substringBeforeLast(":80"))
    }

    @Test
    fun generatesStreamUrlsCorrectly() {
        val client = XtreamClient(
            sourceId = "test-source",
            source = ContentSource.Xtream("https://example.com:8443", "Test Provider", "testuser", "testpass")
        )
        val movie = XtreamMovie(
            id = 42,
            name = "Test Movie",
            categoryId = "1",
            posterUrl = "https://example.com/poster.jpg",
            containerExtension = "mkv",
            rating = "8.5",
            year = "2024",
            plot = "A test movie synopsis"
        )
        assertEquals("https://example.com:8443/movie/testuser/testpass/42.mkv", client.movieStreamUrl(movie))

        val episode = XtreamEpisode(
            id = "1001",
            title = "Pilot",
            season = 1,
            episodeNumber = 1,
            containerExtension = "mp4",
            posterUrl = null,
            plot = "Pilot episode"
        )
        assertEquals("https://example.com:8443/series/testuser/testpass/1001.mp4", client.episodeStreamUrl(episode))
    }

    @Test
    fun parsesCategoriesCorrectly() {
        val client = XtreamClient(
            sourceId = "test-source",
            source = ContentSource.Xtream("https://example.com", "Test", "user", "pass")
        )
        val json = """[{"category_id":"1","category_name":"Action"},{"category_id":"2","category_name":"Comedy"}]"""
        val categories = client.parseCategories(json)
        assertEquals(2, categories.size)
        assertEquals("1", categories[0].id)
        assertEquals("Action", categories[0].name)
        assertEquals("2", categories[1].id)
        assertEquals("Comedy", categories[1].name)
    }

    @Test
    fun parsesMoviesCorrectly() {
        val client = XtreamClient(
            sourceId = "test-source",
            source = ContentSource.Xtream("https://example.com", "Test", "user", "pass")
        )
        val json = """[
            {
                "stream_id": 100,
                "name": "Sample Movie",
                "category_id": "5",
                "stream_icon": "https://img.example/100.jpg",
                "container_extension": "mp4",
                "rating": "7.8",
                "year": "2023",
                "plot": "An exciting sample movie"
            }
        ]"""
        val movies = client.parseMovies(json)
        assertEquals(1, movies.size)
        assertEquals(100, movies[0].id)
        assertEquals("Sample Movie", movies[0].name)
        assertEquals("5", movies[0].categoryId)
        assertEquals("https://img.example/100.jpg", movies[0].posterUrl)
        assertEquals("mp4", movies[0].containerExtension)
        assertEquals("7.8", movies[0].rating)
        assertEquals("2023", movies[0].year)
        assertEquals("An exciting sample movie", movies[0].plot)
    }

    @Test
    fun parsesDetailedMovieInformation() {
        val client = XtreamClient(
            sourceId = "test-source",
            source = ContentSource.Xtream("https://example.com", "Test", "user", "pass")
        )
        val details = client.parseMovieDetails(
            """{
                "info": {
                    "plot": "A detailed synopsis",
                    "genre": "Adventure, Family",
                    "duration": "01:42:00",
                    "releasedate": "2024-03-15",
                    "rating": "7.446",
                    "director": "Test Director",
                    "actors": "Actor One, Actor Two",
                    "country": "Portugal",
                    "mpaa_rating": "M/12",
                    "movie_image": "https://img.example/poster.jpg",
                    "backdrop_path": ["https://img.example/backdrop.jpg"],
                    "youtube_trailer": "trailer-id"
                },
                "movie_data": {"stream_id": 42}
            }"""
        )

        assertEquals("A detailed synopsis", details.plot)
        assertEquals("Adventure, Family", details.genre)
        assertEquals("01:42:00", details.duration)
        assertEquals("2024-03-15", details.releaseDate)
        assertEquals("7.446", details.rating)
        assertEquals("Test Director", details.director)
        assertEquals("Actor One, Actor Two", details.cast)
        assertEquals("Portugal", details.country)
        assertEquals("M/12", details.ageRating)
        assertEquals("https://img.example/poster.jpg", details.posterUrl)
        assertEquals("https://img.example/backdrop.jpg", details.backdropUrl)
        assertEquals("trailer-id", details.trailerUrl)
    }

    @Test
    fun parsesSeriesAndEpisodesCorrectly() {
        val client = XtreamClient(
            sourceId = "test-source",
            source = ContentSource.Xtream("https://example.com", "Test", "user", "pass")
        )
        val seriesJson = """[
            {
                "series_id": 200,
                "name": "Sample Series",
                "category_id": "3",
                "cover": "https://img.example/series200.jpg",
                "rating": "9.1",
                "releaseDate": "2022",
                "plot": "A sample drama series"
            }
        ]"""
        val series = client.parseSeries(seriesJson)
        assertEquals(1, series.size)
        assertEquals(200, series[0].id)
        assertEquals("Sample Series", series[0].name)
        assertEquals("3", series[0].categoryId)
        assertEquals("https://img.example/series200.jpg", series[0].coverUrl)
        assertEquals("9.1", series[0].rating)
        assertEquals("2022", series[0].year)
        assertEquals("A sample drama series", series[0].plot)

        val episodesJson = """{
            "episodes": {
                "1": [
                    {
                        "id": "501",
                        "episode_num": 1,
                        "title": "Chapter 1",
                        "container_extension": "mkv",
                        "info": {
                            "plot": "The journey begins",
                            "movie_image": "https://img.example/ep1.jpg"
                        }
                    },
                    {
                        "id": "502",
                        "episode_num": 2,
                        "title": "Chapter 2",
                        "container_extension": "mkv",
                        "info": {
                            "plot": "The story continues"
                        }
                    }
                ],
                "2": [
                    {
                        "id": "601",
                        "episode_num": 1,
                        "title": "Season 2 Opener",
                        "container_extension": "mp4"
                    }
                ]
            }
        }"""
        val episodes = client.parseEpisodes(episodesJson)
        assertEquals(3, episodes.size)
        assertEquals("501", episodes[0].id)
        assertEquals(1, episodes[0].season)
        assertEquals(1, episodes[0].episodeNumber)
        assertEquals("Chapter 1", episodes[0].title)
        assertEquals("mkv", episodes[0].containerExtension)
        assertEquals("The journey begins", episodes[0].plot)

        assertEquals("502", episodes[1].id)
        assertEquals(1, episodes[1].season)
        assertEquals(2, episodes[1].episodeNumber)

        assertEquals("601", episodes[2].id)
        assertEquals(2, episodes[2].season)
        assertEquals(1, episodes[2].episodeNumber)
        assertEquals("Season 2 Opener", episodes[2].title)
    }

    @Test
    fun parsesSeriesMetadataAndEpisodesFromOneResponse() {
        val client = XtreamClient(
            sourceId = "test-source",
            source = ContentSource.Xtream("https://example.com", "Test", "user", "pass")
        )
        val details = client.parseSeriesDetails(
            """{
                "info": {
                    "name": "Sample Series",
                    "plot": "A complete series synopsis",
                    "genre": "Drama",
                    "releaseDate": "2023-09-10",
                    "rating": "8.276",
                    "director": "Series Director",
                    "cast": "Actor A, Actor B",
                    "episode_run_time": "48",
                    "country": "Portugal",
                    "age": "M/12",
                    "cover": "https://img.example/cover.jpg",
                    "backdrop_path": ["https://img.example/series-backdrop.jpg"]
                },
                "episodes": {
                    "1": [{
                        "id": "701",
                        "episode_num": 1,
                        "title": "Pilot",
                        "container_extension": "mkv",
                        "info": {"plot": "The story begins"}
                    }]
                }
            }"""
        )

        assertEquals("A complete series synopsis", details.plot)
        assertEquals("Drama", details.genre)
        assertEquals("2023-09-10", details.releaseDate)
        assertEquals("8.276", details.rating)
        assertEquals("Series Director", details.director)
        assertEquals("Actor A, Actor B", details.cast)
        assertEquals("48", details.duration)
        assertEquals("Portugal", details.country)
        assertEquals("M/12", details.ageRating)
        assertEquals("https://img.example/cover.jpg", details.coverUrl)
        assertEquals("https://img.example/series-backdrop.jpg", details.backdropUrl)
        assertEquals(1, details.episodes.size)
        assertEquals("701", details.episodes.single().id)
    }

    @Test
    fun parsesLiveChannelsCorrectly() {
        val client = XtreamClient(
            sourceId = "test-source",
            source = ContentSource.Xtream("https://example.com", "Test Provider", "user1", "pass1")
        )
        val categories = listOf(
            XtreamCategory("10", "Sports"),
            XtreamCategory("20", "News")
        )
        val liveJson = """[
            {
                "num": 1,
                "name": "Sports Channel 1",
                "stream_type": "live",
                "stream_id": 555,
                "stream_icon": "https://img.example/555.png",
                "epg_channel_id": "sports.1",
                "category_id": "10"
            },
            {
                "num": 2,
                "name": "News 24",
                "stream_type": "live",
                "stream_id": 777,
                "category_id": "20",
                "container_extension": "m3u8"
            }
        ]"""
        val channels = client.parseLiveChannels(liveJson, categories)
        assertEquals(2, channels.size)

        assertEquals("Sports Channel 1", channels[0].name)
        assertEquals("https://example.com/live/user1/pass1/555.ts", channels[0].streamUrl)
        assertEquals("sports.1", channels[0].tvgId)
        assertEquals("https://img.example/555.png", channels[0].logoUrl)
        assertEquals("Sports", channels[0].group)
        assertEquals("test-source", channels[0].sourceId)
        assertEquals("555", channels[0].sourceItemId)

        assertEquals("News 24", channels[1].name)
        assertEquals("https://example.com/live/user1/pass1/777.m3u8", channels[1].streamUrl)
        assertEquals("News", channels[1].group)
    }
}
