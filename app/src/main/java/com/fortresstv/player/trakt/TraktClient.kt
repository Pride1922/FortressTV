package com.fortresstv.player.trakt

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlin.coroutines.coroutineContext

data class TraktHttpResponse(val code: Int, val body: String, val headers: Map<String, String>)

open class TraktClient(
    val authHost: String = TraktEndpoints.AUTH_HOST,
    val apiHost: String = TraktEndpoints.API_HOST,
    private val connectTimeoutMs: Int = 15_000,
    private val readTimeoutMs: Int = 15_000
) {

    /**
     * Generates a device code and user code for the Device Code Flow.
     */
    open fun generateDeviceCode(clientId: String): TraktAuthResult<TraktDeviceCode> {
        val cleanClientId = clientId.trim().trim('"', '\'')
        val payload = JSONObject().apply {
            put("client_id", cleanClientId)
        }

        val hosts = listOf(authHost, apiHost).distinct()
        var lastFailure: TraktAuthResult.Failure? = null

        for (host in hosts) {
            val url = "${host.trimEnd('/')}/oauth/device/code"
            try {
                val response = executePost(url, payload.toString(), clientId = cleanClientId)
                if (response.code in 200..299) {
                    val json = JSONObject(response.body)
                    return TraktAuthResult.Success(TraktDeviceCode.fromJson(json))
                } else {
                    val message = parseErrorDetail(response.code, response.body, "device code")
                    lastFailure = TraktAuthResult.Failure(message, statusCode = response.code)
                    if (response.code == 401) {
                        return lastFailure
                    }
                }
            } catch (e: Throwable) {
                lastFailure = TraktAuthResult.Failure("Network error requesting device code: ${e.message}", cause = e)
            }
        }

        return lastFailure ?: TraktAuthResult.Failure("Failed to request device code: unknown error")
    }

    /**
     * Polls the device token endpoint once.
     */
    open fun pollDeviceToken(deviceCode: String, clientId: String): TraktPollResult {
        val cleanClientId = clientId.trim().trim('"', '\'')
        val payload = JSONObject().apply {
            put("code", deviceCode)
            put("client_id", cleanClientId)
        }
        val url = "${authHost.trimEnd('/')}/oauth/device/token"

        return try {
            val response = executePost(url, payload.toString(), clientId = cleanClientId)
            when (response.code) {
                200 -> {
                    val json = JSONObject(response.body)
                    TraktPollResult.Success(TraktToken.fromJson(json))
                }
                400 -> TraktPollResult.Pending
                404 -> TraktPollResult.Error(404, "Invalid device code")
                409 -> TraktPollResult.Error(409, "Already approved")
                410 -> TraktPollResult.Expired
                418 -> TraktPollResult.Denied
                429 -> {
                    // Slow down: header or default +5 seconds
                    val retryAfter = response.headers["Retry-After"]?.toIntOrNull() ?: 5
                    TraktPollResult.SlowDown(retryAfter)
                }
                else -> {
                    val message = parseErrorDetail(response.code, response.body, "token polling")
                    TraktPollResult.Error(response.code, message)
                }
            }
        } catch (e: Throwable) {
            TraktPollResult.Error(-1, e.message ?: "Connection error")
        }
    }

    /**
     * Suspends and polls until authorization completes, is cancelled, or expires.
     */
    suspend fun pollUntilAuthorized(
        device: TraktDeviceCode,
        clientId: String,
        onPollAttempt: ((Int) -> Unit)? = null
    ): TraktPollResult {
        var intervalSeconds = device.intervalSeconds.coerceAtLeast(1)
        var attempt = 0

        while (coroutineContext.isActive) {
            if (device.isExpired) {
                return TraktPollResult.Expired
            }

            attempt++
            onPollAttempt?.invoke(attempt)

            when (val result = pollDeviceToken(device.deviceCode, clientId)) {
                is TraktPollResult.Success -> return result
                is TraktPollResult.Expired -> return result
                is TraktPollResult.Denied -> return result
                is TraktPollResult.SlowDown -> {
                    intervalSeconds = (intervalSeconds + result.newIntervalSeconds).coerceAtLeast(1)
                    delay(intervalSeconds * 1000L)
                }
                is TraktPollResult.Pending -> {
                    delay(intervalSeconds * 1000L)
                }
                is TraktPollResult.Error -> {
                    // For transient network hiccups, retry; for permanent error, return
                    if (result.statusCode == 404 || result.statusCode == 409) {
                        return result
                    }
                    delay(intervalSeconds * 1000L)
                }
            }
        }
        return TraktPollResult.Error(-1, "Polling cancelled")
    }

    /**
     * Exchanges an authorization code obtained via PKCE flow for access & refresh tokens.
     */
    open fun exchangeAuthorizationCode(
        code: String,
        codeVerifier: String,
        redirectUri: String,
        clientId: String
    ): TraktAuthResult<TraktToken> {
        val cleanClientId = clientId.trim().trim('"', '\'')
        val payload = JSONObject().apply {
            put("code", code)
            put("client_id", cleanClientId)
            put("code_verifier", codeVerifier)
            put("redirect_uri", redirectUri)
            put("grant_type", "authorization_code")
        }
        val url = "${authHost.trimEnd('/')}/oauth/token"

        return try {
            val response = executePost(url, payload.toString(), clientId = cleanClientId)
            if (response.code in 200..299) {
                val json = JSONObject(response.body)
                TraktAuthResult.Success(TraktToken.fromJson(json))
            } else {
                val message = parseErrorDetail(response.code, response.body, "token exchange")
                TraktAuthResult.Failure(message, statusCode = response.code)
            }
        } catch (e: Throwable) {
            TraktAuthResult.Failure("Token exchange network error: ${e.message}", cause = e)
        }
    }

    /**
     * Refreshes an access token using a single-use refresh token (without client secret).
     */
    open fun refreshToken(refreshToken: String, clientId: String): TraktAuthResult<TraktToken> {
        val cleanClientId = clientId.trim().trim('"', '\'')
        val payload = JSONObject().apply {
            put("refresh_token", refreshToken)
            put("client_id", cleanClientId)
            put("grant_type", "refresh_token")
        }
        val url = "${authHost.trimEnd('/')}/oauth/token"

        return try {
            val response = executePost(url, payload.toString(), clientId = cleanClientId)
            if (response.code in 200..299) {
                val json = JSONObject(response.body)
                TraktAuthResult.Success(TraktToken.fromJson(json))
            } else {
                val message = parseErrorDetail(response.code, response.body, "token refresh")
                TraktAuthResult.Failure(message, statusCode = response.code)
            }
        } catch (e: Throwable) {
            TraktAuthResult.Failure("Token refresh network error: ${e.message}", cause = e)
        }
    }

    protected open fun parseErrorDetail(code: Int, body: String, action: String): String {
        val json = runCatching { JSONObject(body) }.getOrNull()
        val error = json?.optString("error").orEmpty()
        val desc = json?.optString("error_description").orEmpty()

        return when {
            code == 401 && (error == "invalid_client" || desc.contains("not found", ignoreCase = true) || desc.contains("client", ignoreCase = true)) ->
                "Invalid Trakt Client ID (401: client not found). Trakt requires the 64-character Client ID from your app at trakt.tv/oauth/applications."
            code == 401 -> "Trakt authorization error (401): ${desc.ifBlank { error.ifBlank { "Unauthorized" } }}"
            desc.isNotBlank() -> "Trakt error ($code): $desc"
            error.isNotBlank() -> "Trakt error ($code): $error"
            body.isNotBlank() -> "Failed to request $action ($code): ${body.take(120)}"
            else -> "Failed to request $action: HTTP $code"
        }
    }

    /**
     * Revokes a token upon disconnect.
     */
    open fun revokeToken(token: String, clientId: String): Boolean {
        val payload = JSONObject().apply {
            put("token", token)
            put("client_id", clientId)
        }
        val url = "${authHost.trimEnd('/')}/oauth/revoke"

        return try {
            val response = executePost(url, payload.toString(), clientId = clientId)
            response.code in 200..299
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Fetches the current user's Trakt profile.
     */
    open fun fetchUserProfile(accessToken: String, clientId: String): TraktAuthResult<TraktUserProfile> {
        val url = "${apiHost.trimEnd('/')}/users/me?extended=full"
        return try {
            val response = executeGet(url, accessToken = accessToken, clientId = clientId)
            if (response.code in 200..299) {
                val json = JSONObject(response.body)
                TraktAuthResult.Success(TraktUserProfile.fromJson(json))
            } else {
                TraktAuthResult.Failure("Failed to fetch user profile: ${response.code}", statusCode = response.code)
            }
        } catch (e: Throwable) {
            TraktAuthResult.Failure("Error fetching profile: ${e.message}", cause = e)
        }
    }

    /**
     * Sends a scrobble action (start, pause, or stop) to Trakt.
     */
    open fun scrobble(
        action: String,
        payload: JSONObject,
        accessToken: String,
        clientId: String
    ): TraktAuthResult<JSONObject> {
        val url = "${apiHost.trimEnd('/')}/scrobble/${action.lowercase()}"
        return try {
            val response = executePost(url, payload.toString(), accessToken = accessToken, clientId = clientId)
            if (response.code in 200..299) {
                val json = if (response.body.isNotBlank()) JSONObject(response.body) else JSONObject()
                TraktAuthResult.Success(json)
            } else {
                TraktAuthResult.Failure("Scrobble $action returned HTTP ${response.code}", statusCode = response.code)
            }
        } catch (e: Throwable) {
            TraktAuthResult.Failure("Scrobble $action error: ${e.message}", cause = e)
        }
    }

    /**
     * Fetches movies from the user's Trakt Watchlist.
     */
    open fun fetchWatchlistMovies(accessToken: String, clientId: String): TraktAuthResult<List<TraktWatchlistItem>> {
        val url = "${apiHost.trimEnd('/')}/sync/watchlist/movies"
        return try {
            val response = executeGet(url, accessToken = accessToken, clientId = clientId)
            if (response.code in 200..299) {
                val array = org.json.JSONArray(response.body)
                val items = mutableListOf<TraktWatchlistItem>()
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    TraktWatchlistItem.fromJson(obj, TraktWatchlistKind.MOVIE)?.let { items.add(it) }
                }
                TraktAuthResult.Success(items)
            } else {
                TraktAuthResult.Failure("Failed to fetch watchlist movies: ${response.code}", statusCode = response.code)
            }
        } catch (e: Throwable) {
            TraktAuthResult.Failure("Error fetching watchlist movies: ${e.message}", cause = e)
        }
    }

    /**
     * Fetches TV shows from the user's Trakt Watchlist.
     */
    open fun fetchWatchlistShows(accessToken: String, clientId: String): TraktAuthResult<List<TraktWatchlistItem>> {
        val url = "${apiHost.trimEnd('/')}/sync/watchlist/shows"
        return try {
            val response = executeGet(url, accessToken = accessToken, clientId = clientId)
            if (response.code in 200..299) {
                val array = org.json.JSONArray(response.body)
                val items = mutableListOf<TraktWatchlistItem>()
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    TraktWatchlistItem.fromJson(obj, TraktWatchlistKind.SHOW)?.let { items.add(it) }
                }
                TraktAuthResult.Success(items)
            } else {
                TraktAuthResult.Failure("Failed to fetch watchlist shows: ${response.code}", statusCode = response.code)
            }
        } catch (e: Throwable) {
            TraktAuthResult.Failure("Error fetching watchlist shows: ${e.message}", cause = e)
        }
    }

    /**
     * Adds an item to the user's Trakt Watchlist.
     */
    open fun addToWatchlist(item: TraktWatchlistItem, accessToken: String, clientId: String): TraktAuthResult<Boolean> {
        val url = "${apiHost.trimEnd('/')}/sync/watchlist"
        val payload = item.toSyncPayload()
        return try {
            val response = executePost(url, payload.toString(), accessToken = accessToken, clientId = clientId)
            if (response.code in 200..299) {
                TraktAuthResult.Success(true)
            } else {
                TraktAuthResult.Failure("Failed to add to watchlist: ${response.code}", statusCode = response.code)
            }
        } catch (e: Throwable) {
            TraktAuthResult.Failure("Error adding to watchlist: ${e.message}", cause = e)
        }
    }

    /**
     * Removes an item from the user's Trakt Watchlist.
     */
    open fun removeFromWatchlist(item: TraktWatchlistItem, accessToken: String, clientId: String): TraktAuthResult<Boolean> {
        val url = "${apiHost.trimEnd('/')}/sync/watchlist/remove"
        val payload = item.toSyncPayload()
        return try {
            val response = executePost(url, payload.toString(), accessToken = accessToken, clientId = clientId)
            if (response.code in 200..299) {
                TraktAuthResult.Success(true)
            } else {
                TraktAuthResult.Failure("Failed to remove from watchlist: ${response.code}", statusCode = response.code)
            }
        } catch (e: Throwable) {
            TraktAuthResult.Failure("Error removing from watchlist: ${e.message}", cause = e)
        }
    }

    /**
     * Fetches all watched movies for the authenticated user from Trakt.
     */
    open fun fetchWatchedMovies(accessToken: String, clientId: String): TraktAuthResult<List<TraktWatchedMovie>> {
        val url = "${apiHost.trimEnd('/')}/sync/watched/movies"
        return try {
            val response = executeGet(url, accessToken = accessToken, clientId = clientId)
            if (response.code in 200..299) {
                val array = org.json.JSONArray(response.body)
                val list = ArrayList<TraktWatchedMovie>(array.length())
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    TraktWatchedMovie.fromJson(obj)?.let { list.add(it) }
                }
                TraktAuthResult.Success(list)
            } else {
                TraktAuthResult.Failure("Failed to fetch watched movies: ${response.code}", statusCode = response.code)
            }
        } catch (e: Throwable) {
            TraktAuthResult.Failure("Error fetching watched movies: ${e.message}", cause = e)
        }
    }

    /**
     * Fetches all watched shows for the authenticated user from Trakt (including seasons & episodes).
     */
    open fun fetchWatchedShows(accessToken: String, clientId: String): TraktAuthResult<List<TraktWatchedShow>> {
        val url = "${apiHost.trimEnd('/')}/sync/watched/shows"
        return try {
            val response = executeGet(url, accessToken = accessToken, clientId = clientId)
            if (response.code in 200..299) {
                val array = org.json.JSONArray(response.body)
                val list = ArrayList<TraktWatchedShow>(array.length())
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    TraktWatchedShow.fromJson(obj)?.let { list.add(it) }
                }
                TraktAuthResult.Success(list)
            } else {
                TraktAuthResult.Failure("Failed to fetch watched shows: ${response.code}", statusCode = response.code)
            }
        } catch (e: Throwable) {
            TraktAuthResult.Failure("Error fetching watched shows: ${e.message}", cause = e)
        }
    }

    /**
     * Adds items (movies or episodes) to the user's Trakt watch history.
     */
    open fun addToHistory(payload: JSONObject, accessToken: String, clientId: String): TraktAuthResult<Boolean> {
        val url = "${apiHost.trimEnd('/')}/sync/history"
        return try {
            val response = executePost(url, payload.toString(), accessToken = accessToken, clientId = clientId)
            if (response.code in 200..299) {
                TraktAuthResult.Success(true)
            } else {
                TraktAuthResult.Failure("Failed to add to watch history: ${response.code}", statusCode = response.code)
            }
        } catch (e: Throwable) {
            TraktAuthResult.Failure("Error adding to watch history: ${e.message}", cause = e)
        }
    }

    /**
     * Removes items (movies or episodes) from the user's Trakt watch history.
     */
    open fun removeFromHistory(payload: JSONObject, accessToken: String, clientId: String): TraktAuthResult<Boolean> {
        val url = "${apiHost.trimEnd('/')}/sync/history/remove"
        return try {
            val response = executePost(url, payload.toString(), accessToken = accessToken, clientId = clientId)
            if (response.code in 200..299) {
                TraktAuthResult.Success(true)
            } else {
                TraktAuthResult.Failure("Failed to remove from watch history: ${response.code}", statusCode = response.code)
            }
        } catch (e: Throwable) {
            TraktAuthResult.Failure("Error removing from watch history: ${e.message}", cause = e)
        }
    }

    /**
     * Fetches personalized movie recommendations for the user.
     */
    open fun fetchRecommendedMovies(
        accessToken: String,
        clientId: String,
        limit: Int = 40
    ): TraktAuthResult<List<TraktRecommendationItem>> {
        val url = "${apiHost.trimEnd('/')}/recommendations/movies?limit=$limit"
        return try {
            val response = executeGet(url, accessToken = accessToken, clientId = clientId)
            if (response.code in 200..299) {
                val array = org.json.JSONArray(response.body)
                val list = ArrayList<TraktRecommendationItem>(array.length())
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    TraktRecommendationItem.fromJson(obj, TraktWatchlistKind.MOVIE)?.let { list.add(it) }
                }
                TraktAuthResult.Success(list)
            } else {
                TraktAuthResult.Failure("Failed to fetch movie recommendations: ${response.code}", statusCode = response.code)
            }
        } catch (e: Throwable) {
            TraktAuthResult.Failure("Error fetching movie recommendations: ${e.message}", cause = e)
        }
    }

    /**
     * Fetches personalized show recommendations for the user.
     */
    open fun fetchRecommendedShows(
        accessToken: String,
        clientId: String,
        limit: Int = 40
    ): TraktAuthResult<List<TraktRecommendationItem>> {
        val url = "${apiHost.trimEnd('/')}/recommendations/shows?limit=$limit"
        return try {
            val response = executeGet(url, accessToken = accessToken, clientId = clientId)
            if (response.code in 200..299) {
                val array = org.json.JSONArray(response.body)
                val list = ArrayList<TraktRecommendationItem>(array.length())
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    TraktRecommendationItem.fromJson(obj, TraktWatchlistKind.SHOW)?.let { list.add(it) }
                }
                TraktAuthResult.Success(list)
            } else {
                TraktAuthResult.Failure("Failed to fetch show recommendations: ${response.code}", statusCode = response.code)
            }
        } catch (e: Throwable) {
            TraktAuthResult.Failure("Error fetching show recommendations: ${e.message}", cause = e)
        }
    }

    /**
     * Dismisses (hides) a recommendation on Trakt so it will not appear again.
     */
    open fun dismissRecommendation(
        kind: TraktWatchlistKind,
        traktIdOrSlug: String,
        accessToken: String,
        clientId: String
    ): TraktAuthResult<Boolean> {
        val path = if (kind == TraktWatchlistKind.MOVIE) "movies" else "shows"
        val url = "${apiHost.trimEnd('/')}/recommendations/$path/$traktIdOrSlug"
        return try {
            val response = executeDelete(url, accessToken = accessToken, clientId = clientId)
            if (response.code in 200..299 || response.code == 204) {
                TraktAuthResult.Success(true)
            } else {
                TraktAuthResult.Failure("Failed to dismiss recommendation: ${response.code}", statusCode = response.code)
            }
        } catch (e: Throwable) {
            TraktAuthResult.Failure("Error dismissing recommendation: ${e.message}", cause = e)
        }
    }

    // --- HTTP Helpers using HttpURLConnection ---

    open fun executePost(
        urlString: String,
        jsonBody: String,
        accessToken: String? = null,
        clientId: String? = null
    ): TraktHttpResponse {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("trakt-api-version", TraktEndpoints.API_VERSION)
            if (!clientId.isNullOrBlank()) {
                setRequestProperty("trakt-api-key", clientId)
            }
            if (!accessToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
        }

        OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
            writer.write(jsonBody)
            writer.flush()
        }

        return readResponse(connection)
    }

    open fun executeGet(
        urlString: String,
        accessToken: String? = null,
        clientId: String? = null
    ): TraktHttpResponse {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("trakt-api-version", TraktEndpoints.API_VERSION)
            if (!clientId.isNullOrBlank()) {
                setRequestProperty("trakt-api-key", clientId)
            }
            if (!accessToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
        }

        return readResponse(connection)
    }

    open fun executeDelete(
        urlString: String,
        accessToken: String? = null,
        clientId: String? = null
    ): TraktHttpResponse {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("trakt-api-version", TraktEndpoints.API_VERSION)
            if (!clientId.isNullOrBlank()) {
                setRequestProperty("trakt-api-key", clientId)
            }
            if (!accessToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
        }

        return readResponse(connection)
    }

    private fun readResponse(connection: HttpURLConnection): TraktHttpResponse {
        val code = connection.responseCode
        val stream: InputStream? = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.let {
            BufferedReader(InputStreamReader(it, StandardCharsets.UTF_8)).use { reader ->
                reader.readText()
            }
        }.orEmpty()

        val headers = mutableMapOf<String, String>()
        for ((key, value) in connection.headerFields) {
            if (key != null && value.isNotEmpty()) {
                headers[key] = value[0]
            }
        }

        return TraktHttpResponse(code, body, headers)
    }
}
