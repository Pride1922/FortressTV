package com.fortresstv.player.trakt

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TraktClientTest {

    private class MockTraktClient : TraktClient(
        authHost = "https://auth.trakt.tv",
        apiHost = "https://api.trakt.tv"
    ) {
        var postHandler: ((url: String, body: String, clientId: String?) -> TraktHttpResponse)? = null
        var getHandler: ((url: String, accessToken: String?, clientId: String?) -> TraktHttpResponse)? = null
        var deleteHandler: ((url: String, accessToken: String?, clientId: String?) -> TraktHttpResponse)? = null

        var lastPostUrl: String? = null
        var lastPostBody: String? = null
        var lastPostClientId: String? = null

        override fun executePost(
            urlString: String,
            jsonBody: String,
            accessToken: String?,
            clientId: String?
        ): TraktHttpResponse {
            lastPostUrl = urlString
            lastPostBody = jsonBody
            lastPostClientId = clientId
            return postHandler?.invoke(urlString, jsonBody, clientId)
                ?: TraktHttpResponse(500, "No mock handler", emptyMap())
        }

        override fun executeGet(
            urlString: String,
            accessToken: String?,
            clientId: String?
        ): TraktHttpResponse {
            return getHandler?.invoke(urlString, accessToken, clientId)
                ?: TraktHttpResponse(500, "No mock handler", emptyMap())
        }

        override fun executeDelete(
            urlString: String,
            accessToken: String?,
            clientId: String?
        ): TraktHttpResponse {
            return deleteHandler?.invoke(urlString, accessToken, clientId)
                ?: TraktHttpResponse(500, "No mock handler", emptyMap())
        }
    }

    @Test
    fun parseDeviceCodeFromJson() {
        val json = JSONObject("""
            {
                "device_code": "dev_code_12345",
                "user_code": "ABCD-1234",
                "verification_url": "https://trakt.tv/activate",
                "expires_in": 600,
                "interval": 5
            }
        """.trimIndent())

        val device = TraktDeviceCode.fromJson(json)
        assertEquals("dev_code_12345", device.deviceCode)
        assertEquals("ABCD-1234", device.userCode)
        assertEquals("https://trakt.tv/activate", device.verificationUrl)
        assertEquals(600, device.expiresInSeconds)
        assertEquals(5, device.intervalSeconds)
        assertFalse(device.isExpired)
    }

    @Test
    fun parseTokenFromJson() {
        val json = JSONObject("""
            {
                "access_token": "acc_tok_999",
                "token_type": "bearer",
                "expires_in": 7776000,
                "refresh_token": "ref_tok_888",
                "scope": "public",
                "created_at": 1700000000
            }
        """.trimIndent())

        val token = TraktToken.fromJson(json)
        assertEquals("acc_tok_999", token.accessToken)
        assertEquals("ref_tok_888", token.refreshToken)
        assertEquals("bearer", token.tokenType)
        assertEquals("public", token.scope)
        assertEquals(1700000000000L, token.createdAtEpochMs)
        assertEquals(7776000L, token.expiresInSeconds)
    }

    @Test
    fun parseUserProfileFromJson() {
        val json = JSONObject("""
            {
                "username": "cinephile",
                "name": "Jane Doe",
                "vip": true,
                "images": {
                    "avatar": {
                        "full": "https://walter.trakt.tv/user_avatar.jpg"
                    }
                }
            }
        """.trimIndent())

        val profile = TraktUserProfile.fromJson(json)
        assertEquals("cinephile", profile.username)
        assertEquals("Jane Doe", profile.name)
        assertTrue(profile.isVip)
        assertEquals("https://walter.trakt.tv/user_avatar.jpg", profile.avatarUrl)
    }

    @Test
    fun generateDeviceCodeUsesAuthHostAndPublicClientId() {
        val client = MockTraktClient()
        client.postHandler = { url, body, clientId ->
            assertEquals("https://auth.trakt.tv/oauth/device/code", url)
            assertEquals("fortress_test_client_id", clientId)
            val json = JSONObject(body)
            assertEquals("fortress_test_client_id", json.getString("client_id"))
            assertFalse(json.has("client_secret"))
            TraktHttpResponse(200, """
                {
                    "device_code": "dc_1",
                    "user_code": "UC-1",
                    "verification_url": "https://trakt.tv/activate",
                    "expires_in": 300,
                    "interval": 5
                }
            """.trimIndent(), emptyMap())
        }

        val result = client.generateDeviceCode("fortress_test_client_id")
        assertTrue(result is TraktAuthResult.Success)
        val code = (result as TraktAuthResult.Success).data
        assertEquals("dc_1", code.deviceCode)
        assertEquals("UC-1", code.userCode)
    }

    @Test
    fun pollDeviceTokenHandlesAllTraktStatusCodes() {
        val client = MockTraktClient()

        // 200 -> Success
        client.postHandler = { _, _, _ ->
            TraktHttpResponse(200, """
                {
                    "access_token": "acc_1",
                    "refresh_token": "ref_1",
                    "expires_in": 7776000
                }
            """.trimIndent(), emptyMap())
        }
        val res200 = client.pollDeviceToken("dc_test", "cid")
        assertTrue(res200 is TraktPollResult.Success)
        assertEquals("acc_1", (res200 as TraktPollResult.Success).token.accessToken)

        // 400 -> Pending
        client.postHandler = { _, _, _ -> TraktHttpResponse(400, "", emptyMap()) }
        assertTrue(client.pollDeviceToken("dc_test", "cid") is TraktPollResult.Pending)

        // 410 -> Expired
        client.postHandler = { _, _, _ -> TraktHttpResponse(410, "", emptyMap()) }
        assertTrue(client.pollDeviceToken("dc_test", "cid") is TraktPollResult.Expired)

        // 418 -> Denied
        client.postHandler = { _, _, _ -> TraktHttpResponse(418, "", emptyMap()) }
        assertTrue(client.pollDeviceToken("dc_test", "cid") is TraktPollResult.Denied)

        // 429 -> SlowDown
        client.postHandler = { _, _, _ -> TraktHttpResponse(429, "", mapOf("Retry-After" to "10")) }
        val res429 = client.pollDeviceToken("dc_test", "cid")
        assertTrue(res429 is TraktPollResult.SlowDown)
        assertEquals(10, (res429 as TraktPollResult.SlowDown).newIntervalSeconds)
    }

    @Test
    fun tokenRefreshOmitsClientSecret() {
        val client = MockTraktClient()
        client.postHandler = { url, body, clientId ->
            assertEquals("https://auth.trakt.tv/oauth/token", url)
            val json = JSONObject(body)
            assertEquals("ref_single_use_123", json.getString("refresh_token"))
            assertEquals("cid_456", json.getString("client_id"))
            assertEquals("refresh_token", json.getString("grant_type"))
            assertFalse(json.has("client_secret"))
            TraktHttpResponse(200, """
                {
                    "access_token": "acc_refreshed",
                    "refresh_token": "ref_replacement",
                    "expires_in": 7776000
                }
            """.trimIndent(), emptyMap())
        }

        val result = client.refreshToken("ref_single_use_123", "cid_456")
        assertTrue(result is TraktAuthResult.Success)
        val token = (result as TraktAuthResult.Success).data
        assertEquals("acc_refreshed", token.accessToken)
        assertEquals("ref_replacement", token.refreshToken)
    }

    @Test
    fun pollUntilAuthorizedCancelsWhenExpired() = runBlocking {
        val client = MockTraktClient()
        // Device code that expired 1 second ago
        val expiredDevice = TraktDeviceCode(
            deviceCode = "dc_exp",
            userCode = "UC_EXP",
            verificationUrl = "https://trakt.tv/activate",
            expiresInSeconds = -1,
            intervalSeconds = 1,
            createdAtEpochMs = System.currentTimeMillis() - 2000L
        )

        val result = client.pollUntilAuthorized(expiredDevice, "cid")
        assertTrue(result is TraktPollResult.Expired)
    }

    @Test
    fun pollUntilAuthorizedCancelsWhenJobCancelled() = runBlocking {
        val client = MockTraktClient()
        client.postHandler = { _, _, _ -> TraktHttpResponse(400, "", emptyMap()) }

        val activeDevice = TraktDeviceCode(
            deviceCode = "dc_active",
            userCode = "UC_ACT",
            verificationUrl = "https://trakt.tv/activate",
            expiresInSeconds = 300,
            intervalSeconds = 1
        )

        var pollAttempts = 0
        val job = launch {
            client.pollUntilAuthorized(activeDevice, "cid", onPollAttempt = { pollAttempts++ })
        }

        // Give it a brief moment to run first attempt, then cancel
        kotlinx.coroutines.delay(50)
        job.cancel()
        job.join()

        assertTrue(pollAttempts >= 1)
        assertTrue(job.isCancelled)
    }

    @Test
    fun generateDeviceCodeTrimsWhitespaceAndQuotes() {
        val client = MockTraktClient()
        client.postHandler = { url, body, clientId ->
            assertEquals("clean_client_id", clientId)
            val json = JSONObject(body)
            assertEquals("clean_client_id", json.getString("client_id"))
            TraktHttpResponse(200, """
                {
                    "device_code": "dc_1",
                    "user_code": "UC-1",
                    "verification_url": "https://trakt.tv/activate",
                    "expires_in": 300,
                    "interval": 5
                }
            """.trimIndent(), emptyMap())
        }

        val result = client.generateDeviceCode("  \"clean_client_id\"  \n")
        assertTrue(result is TraktAuthResult.Success)
    }

    @Test
    fun generateDeviceCodeReturnsInformativeMessageOn401InvalidClient() {
        val client = MockTraktClient()
        client.postHandler = { _, _, _ ->
            TraktHttpResponse(401, """
                {
                    "error": "invalid_client",
                    "error_description": "client not found"
                }
            """.trimIndent(), emptyMap())
        }

        val result = client.generateDeviceCode("invalid_key")
        assertTrue(result is TraktAuthResult.Failure)
        val failure = result as TraktAuthResult.Failure
        assertEquals(401, failure.statusCode)
        assertTrue(failure.message.contains("64-character Client ID"))
    }

    @Test
    fun generateDeviceCodeFallsBackToApiHostWhenAuthHostFails() {
        val client = MockTraktClient()
        val visitedUrls = mutableListOf<String>()
        client.postHandler = { url, _, _ ->
            visitedUrls.add(url)
            if (url.startsWith("https://auth.trakt.tv")) {
                TraktHttpResponse(503, "Service Unavailable", emptyMap())
            } else {
                TraktHttpResponse(200, """
                    {
                        "device_code": "dc_fallback",
                        "user_code": "UC-FB",
                        "verification_url": "https://trakt.tv/activate",
                        "expires_in": 300,
                        "interval": 5
                    }
                """.trimIndent(), emptyMap())
            }
        }

        val result = client.generateDeviceCode("my_client_id")
        assertTrue(result is TraktAuthResult.Success)
        assertEquals(2, visitedUrls.size)
        assertEquals("https://auth.trakt.tv/oauth/device/code", visitedUrls[0])
        assertEquals("https://api.trakt.tv/oauth/device/code", visitedUrls[1])
    }

    @Test
    fun fetchWatchlistMoviesParsesItems() {
        val client = MockTraktClient()
        client.getHandler = { url, _, _ ->
            assertEquals("https://api.trakt.tv/sync/watchlist/movies", url)
            TraktHttpResponse(200, """
                [
                    {
                        "listed_at": "2024-01-01T12:00:00.000Z",
                        "type": "movie",
                        "movie": {
                            "title": "Inception",
                            "year": 2010,
                            "ids": {
                                "trakt": 16,
                                "slug": "inception-2010",
                                "imdb": "tt1375666",
                                "tmdb": 27205
                            }
                        }
                    }
                ]
            """.trimIndent(), emptyMap())
        }

        val result = client.fetchWatchlistMovies("token123", "client456")
        assertTrue(result is TraktAuthResult.Success)
        val items = (result as TraktAuthResult.Success).data
        assertEquals(1, items.size)
        val item = items[0]
        assertEquals(TraktWatchlistKind.MOVIE, item.kind)
        assertEquals("Inception", item.title)
        assertEquals(2010, item.year)
        assertEquals("27205", item.tmdbId)
        assertEquals("tt1375666", item.imdbId)
    }

    @Test
    fun fetchWatchlistShowsParsesItems() {
        val client = MockTraktClient()
        client.getHandler = { url, _, _ ->
            assertEquals("https://api.trakt.tv/sync/watchlist/shows", url)
            TraktHttpResponse(200, """
                [
                    {
                        "listed_at": "2024-01-02T12:00:00.000Z",
                        "type": "show",
                        "show": {
                            "title": "Breaking Bad",
                            "year": 2008,
                            "ids": {
                                "trakt": 1,
                                "slug": "breaking-bad",
                                "imdb": "tt0903747",
                                "tmdb": 1396
                            }
                        }
                    }
                ]
            """.trimIndent(), emptyMap())
        }

        val result = client.fetchWatchlistShows("token123", "client456")
        assertTrue(result is TraktAuthResult.Success)
        val items = (result as TraktAuthResult.Success).data
        assertEquals(1, items.size)
        val item = items[0]
        assertEquals(TraktWatchlistKind.SHOW, item.kind)
        assertEquals("Breaking Bad", item.title)
        assertEquals(2008, item.year)
        assertEquals("1396", item.tmdbId)
        assertEquals("tt0903747", item.imdbId)
    }

    @Test
    fun addToWatchlistSendsSyncPayload() {
        val client = MockTraktClient()
        client.postHandler = { url, body, _ ->
            assertEquals("https://api.trakt.tv/sync/watchlist", url)
            val json = JSONObject(body)
            assertTrue(json.has("movies"))
            val movies = json.getJSONArray("movies")
            assertEquals(1, movies.length())
            val movie = movies.getJSONObject(0)
            assertEquals("Inception", movie.getString("title"))
            assertEquals(27205, movie.getJSONObject("ids").getInt("tmdb"))
            TraktHttpResponse(201, """{"added":{"movies":1,"shows":0}}""", emptyMap())
        }

        val item = TraktWatchlistItem(
            kind = TraktWatchlistKind.MOVIE,
            title = "Inception",
            year = 2010,
            tmdbId = "27205",
            imdbId = "tt1375666",
            traktId = null,
            slug = null,
            listedAt = null
        )

        val result = client.addToWatchlist(item, "token123", "client456")
        assertTrue(result is TraktAuthResult.Success)
        assertTrue((result as TraktAuthResult.Success).data)
    }

    @Test
    fun fetchWatchedMoviesParsesResponse() {
        val client = MockTraktClient()
        client.getHandler = { url, accessToken, clientId ->
            assertEquals("https://api.trakt.tv/sync/watched/movies", url)
            assertEquals("token_watched", accessToken)
            assertEquals("client_watched", clientId)
            val json = """
                [
                    {
                        "plays": 1,
                        "last_watched_at": "2024-01-01T00:00:00.000Z",
                        "movie": {
                            "title": "Dune: Part Two",
                            "year": 2024,
                            "ids": {
                                "trakt": 599144,
                                "imdb": "tt15239678",
                                "tmdb": 693134
                            }
                        }
                    }
                ]
            """.trimIndent()
            TraktHttpResponse(200, json, emptyMap())
        }

        val result = client.fetchWatchedMovies("token_watched", "client_watched")
        assertTrue(result is TraktAuthResult.Success)
        val movies = (result as TraktAuthResult.Success).data
        assertEquals(1, movies.size)
        assertEquals("Dune: Part Two", movies[0].title)
        assertEquals(2024, movies[0].year)
        assertEquals("693134", movies[0].tmdbId)
        assertEquals("tt15239678", movies[0].imdbId)
    }

    @Test
    fun fetchWatchedShowsParsesResponse() {
        val client = MockTraktClient()
        client.getHandler = { url, _, _ ->
            assertEquals("https://api.trakt.tv/sync/watched/shows", url)
            val json = """
                [
                    {
                        "plays": 10,
                        "last_watched_at": "2024-01-02T00:00:00.000Z",
                        "show": {
                            "title": "Succession",
                            "year": 2018,
                            "ids": {
                                "trakt": 128509,
                                "imdb": "tt7660850",
                                "tmdb": 76331
                            }
                        },
                        "seasons": [
                            {
                                "number": 1,
                                "episodes": [
                                    { "number": 1, "plays": 1 },
                                    { "number": 2, "plays": 1 }
                                ]
                            }
                        ]
                    }
                ]
            """.trimIndent()
            TraktHttpResponse(200, json, emptyMap())
        }

        val result = client.fetchWatchedShows("tok", "cid")
        assertTrue(result is TraktAuthResult.Success)
        val shows = (result as TraktAuthResult.Success).data
        assertEquals(1, shows.size)
        assertEquals("Succession", shows[0].title)
        assertEquals(setOf(1, 2), shows[0].seasons[1])
    }

    @Test
    fun addToHistoryAndRemoveFromHistory() {
        val client = MockTraktClient()
        var addedCalled = false
        var removedCalled = false

        client.postHandler = { url, body, _ ->
            when (url) {
                "https://api.trakt.tv/sync/history" -> {
                    addedCalled = true
                    TraktHttpResponse(201, """{"added":{"movies":1}}""", emptyMap())
                }
                "https://api.trakt.tv/sync/history/remove" -> {
                    removedCalled = true
                    TraktHttpResponse(200, """{"deleted":{"movies":1}}""", emptyMap())
                }
                else -> TraktHttpResponse(404, "", emptyMap())
            }
        }

        val payload = TraktSyncPayloadBuilder.moviePayload("Test", 2024, "123", "tt123")
        val addRes = client.addToHistory(payload, "tok", "cid")
        assertTrue(addRes is TraktAuthResult.Success)
        assertTrue(addedCalled)

        val remRes = client.removeFromHistory(payload, "tok", "cid")
        assertTrue(remRes is TraktAuthResult.Success)
        assertTrue(removedCalled)
    }

    @Test
    fun fetchRecommendedMoviesParsesList() {
        val client = MockTraktClient()
        client.getHandler = { url, _, _ ->
            assertTrue(url.contains("/recommendations/movies"))
            val json = """
                [
                    {
                        "title": "Challengers",
                        "year": 2024,
                        "ids": {
                            "trakt": 771234,
                            "imdb": "tt16426418",
                            "tmdb": 937287
                        }
                    }
                ]
            """.trimIndent()
            TraktHttpResponse(200, json, emptyMap())
        }

        val result = client.fetchRecommendedMovies("tok", "cid")
        assertTrue(result is TraktAuthResult.Success)
        val movies = (result as TraktAuthResult.Success).data
        assertEquals(1, movies.size)
        assertEquals("Challengers", movies[0].title)
        assertEquals(2024, movies[0].year)
        assertEquals("937287", movies[0].tmdbId)
        assertEquals(TraktWatchlistKind.MOVIE, movies[0].kind)
    }

    @Test
    fun fetchRecommendedShowsParsesList() {
        val client = MockTraktClient()
        client.getHandler = { url, _, _ ->
            assertTrue(url.contains("/recommendations/shows"))
            val json = """
                [
                    {
                        "title": "Shogun",
                        "year": 2024,
                        "ids": {
                            "trakt": 139413,
                            "imdb": "tt2788316",
                            "tmdb": 126308
                        }
                    }
                ]
            """.trimIndent()
            TraktHttpResponse(200, json, emptyMap())
        }

        val result = client.fetchRecommendedShows("tok", "cid")
        assertTrue(result is TraktAuthResult.Success)
        val shows = (result as TraktAuthResult.Success).data
        assertEquals(1, shows.size)
        assertEquals("Shogun", shows[0].title)
        assertEquals("126308", shows[0].tmdbId)
        assertEquals(TraktWatchlistKind.SHOW, shows[0].kind)
    }

    @Test
    fun dismissRecommendationSendsDelete() {
        val client = MockTraktClient()
        var deleteCalledUrl: String? = null
        client.deleteHandler = { url, _, _ ->
            deleteCalledUrl = url
            TraktHttpResponse(204, "", emptyMap())
        }

        val result = client.dismissRecommendation(TraktWatchlistKind.MOVIE, "937287", "tok", "cid")
        assertTrue(result is TraktAuthResult.Success)
        assertEquals("https://api.trakt.tv/recommendations/movies/937287", deleteCalledUrl)
    }

    @Test
    fun hiddenRecommendationsIncludeExistingMovieAndSeriesHides() {
        val client = MockTraktClient()
        client.getHandler = { url, _, _ ->
            assertTrue(url.endsWith("/users/hidden/recommendations?page=1&limit=50"))
            TraktHttpResponse(
                200,
                """[{"type":"movie","movie":{"title":"Example film","ids":{"trakt":42}}},
                    {"type":"show","show":{"title":"Example series","ids":{"trakt":43}}}]""",
                mapOf("x-pagination-page-count" to "2")
            )
        }
        val result = client.fetchHiddenRecommendations(1, "", "") as TraktAuthResult.Success
        assertTrue(result.data.hasMore)
        assertEquals(listOf(TraktWatchlistKind.MOVIE, TraktWatchlistKind.SHOW), result.data.items.map { it.kind })
        assertEquals(listOf(42, 43), result.data.items.map { it.traktId })
    }

    @Test
    fun restoresOnlyTheSelectedRecommendationAndRequiresADeletion() {
        val client = MockTraktClient()
        val item = TraktRecommendationItem(TraktWatchlistKind.MOVIE, "Example film", null, null, null, 42, null)
        client.postHandler = { _, _, _ -> TraktHttpResponse(200, """{"deleted":{"movies":1}}""", emptyMap()) }

        assertTrue(client.restoreRecommendation(item, "", "") is TraktAuthResult.Success)
        assertTrue(client.lastPostUrl.orEmpty().endsWith("/users/hidden/recommendations/remove"))
        val payload = JSONObject(client.lastPostBody!!)
        assertEquals(1, payload.getJSONArray("movies").length())
        assertEquals(42, payload.getJSONArray("movies").getJSONObject(0).getJSONObject("ids").getInt("trakt"))
        assertFalse(payload.has("shows"))

        client.postHandler = { _, _, _ -> TraktHttpResponse(200, """{"deleted":{"movies":0}}""", emptyMap()) }
        assertTrue(client.restoreRecommendation(item, "", "") is TraktAuthResult.Failure)
    }

    @Test
    fun fetchTrendingMoviesParsesWrappedObjects() {
        val client = MockTraktClient()
        client.getHandler = { url, _, _ ->
            assertEquals("https://api.trakt.tv/movies/trending?limit=40", url)
            val json = """
                [
                    {
                        "watchers": 1500,
                        "movie": {
                            "title": "Challengers",
                            "year": 2024,
                            "ids": {
                                "trakt": 937287,
                                "imdb": "tt16426418",
                                "tmdb": 937287
                            }
                        }
                    }
                ]
            """.trimIndent()
            TraktHttpResponse(200, json, emptyMap())
        }

        val result = client.fetchTrendingMovies("cid", "tok")
        assertTrue(result is TraktAuthResult.Success)
        val movies = (result as TraktAuthResult.Success).data
        assertEquals(1, movies.size)
        assertEquals("Challengers", movies[0].title)
        assertEquals(2024, movies[0].year)
        assertEquals("937287", movies[0].tmdbId)
        assertEquals(TraktWatchlistKind.MOVIE, movies[0].kind)
    }

    @Test
    fun fetchTrendingShowsParsesWrappedObjects() {
        val client = MockTraktClient()
        client.getHandler = { url, _, _ ->
            assertEquals("https://api.trakt.tv/shows/trending?limit=40", url)
            val json = """
                [
                    {
                        "watchers": 890,
                        "show": {
                            "title": "Severance",
                            "year": 2022,
                            "ids": {
                                "trakt": 154432,
                                "imdb": "tt11280740",
                                "tmdb": 95396
                            }
                        }
                    }
                ]
            """.trimIndent()
            TraktHttpResponse(200, json, emptyMap())
        }

        val result = client.fetchTrendingShows("cid", "tok")
        assertTrue(result is TraktAuthResult.Success)
        val shows = (result as TraktAuthResult.Success).data
        assertEquals(1, shows.size)
        assertEquals("Severance", shows[0].title)
        assertEquals("95396", shows[0].tmdbId)
        assertEquals(TraktWatchlistKind.SHOW, shows[0].kind)
    }
}
