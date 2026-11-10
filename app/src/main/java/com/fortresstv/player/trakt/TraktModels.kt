package com.fortresstv.player.trakt

import org.json.JSONObject
import java.time.Instant

object TraktEndpoints {
    /**
     * Trakt OAuth host. Starting February 1, 2027, Trakt enforces auth.trakt.tv
     * for all authorization and token flows.
     */
    const val AUTH_HOST = "https://auth.trakt.tv"

    /**
     * Trakt API host for scrobbling, sync, and user profile data.
     */
    const val API_HOST = "https://api.trakt.tv"

    const val API_VERSION = "2"
    const val REDIRECT_URI = "fortresstv://trakt/callback"
}

data class TraktDeviceCode(
    val deviceCode: String,
    val userCode: String,
    val verificationUrl: String,
    val expiresInSeconds: Int,
    val intervalSeconds: Int,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    val expiresAtEpochMs: Long
        get() = createdAtEpochMs + (expiresInSeconds * 1000L)

    val isExpired: Boolean
        get() = System.currentTimeMillis() >= expiresAtEpochMs

    companion object {
        fun fromJson(json: JSONObject): TraktDeviceCode {
            return TraktDeviceCode(
                deviceCode = json.getString("device_code"),
                userCode = json.getString("user_code"),
                verificationUrl = json.optString("verification_url").ifBlank { "https://trakt.tv/activate" },
                expiresInSeconds = json.optInt("expires_in", 600),
                intervalSeconds = json.optInt("interval", 5)
            )
        }
    }
}

data class TraktToken(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "bearer",
    val scope: String = "public",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val expiresInSeconds: Long = 7776000L // Trakt tokens default to 3 months (~90 days)
) {
    val expiresAtEpochMs: Long
        get() = createdAtEpochMs + (expiresInSeconds * 1000L)

    /**
     * Returns true if token is expired or close to expiry (within a safety window of 10 minutes).
     */
    fun isExpired(bufferSeconds: Long = 600L): Boolean {
        val now = System.currentTimeMillis()
        val expiryWithBuffer = expiresAtEpochMs - (bufferSeconds * 1000L)
        return now >= expiryWithBuffer
    }

    companion object {
        fun fromJson(json: JSONObject, receivedAtEpochMs: Long = System.currentTimeMillis()): TraktToken {
            val createdAt = json.optLong("created_at", 0L).let {
                if (it > 0L) it * 1000L else receivedAtEpochMs
            }
            val expiresIn = json.optLong("expires_in", 7776000L)
            return TraktToken(
                accessToken = json.getString("access_token"),
                refreshToken = json.getString("refresh_token"),
                tokenType = json.optString("token_type", "bearer"),
                scope = json.optString("scope", "public"),
                createdAtEpochMs = createdAt,
                expiresInSeconds = expiresIn
            )
        }
    }
}

data class TraktUserProfile(
    val username: String,
    val name: String,
    val isVip: Boolean,
    val avatarUrl: String?
) {
    companion object {
        fun fromJson(json: JSONObject): TraktUserProfile {
            val user = json.optJSONObject("user") ?: json
            val ids = user.optJSONObject("ids")
            val username = user.optString("username", ids?.optString("slug", "trakt_user") ?: "trakt_user")
            val name = user.optString("name", username)
            val isVip = user.optBoolean("vip", false) || user.optBoolean("vip_ep", false)
            val images = user.optJSONObject("images")
            val avatar = images?.optJSONObject("avatar")?.optString("full")?.takeIf { it.isNotBlank() }

            return TraktUserProfile(
                username = username,
                name = name,
                isVip = isVip,
                avatarUrl = avatar
            )
        }
    }
}

sealed class TraktPollResult {
    data class Success(val token: TraktToken) : TraktPollResult()
    object Pending : TraktPollResult()
    data class SlowDown(val newIntervalSeconds: Int) : TraktPollResult()
    object Expired : TraktPollResult()
    object Denied : TraktPollResult()
    data class Error(val statusCode: Int, val message: String) : TraktPollResult()
}

sealed class TraktAuthResult<out T> {
    data class Success<T>(val data: T) : TraktAuthResult<T>()
    data class Failure(val message: String, val statusCode: Int? = null, val cause: Throwable? = null) : TraktAuthResult<Nothing>()
}

enum class TraktWatchlistKind {
    MOVIE,
    SHOW
}

data class TraktWatchlistItem(
    val kind: TraktWatchlistKind,
    val title: String,
    val year: Int?,
    val tmdbId: String?,
    val imdbId: String?,
    val traktId: Int?,
    val slug: String?,
    val listedAt: String?
) {
    fun toSyncPayload(): JSONObject {
        val ids = JSONObject()
        tmdbId?.toIntOrNull()?.let { ids.put("tmdb", it) }
        imdbId?.takeIf { it.isNotBlank() }?.let { ids.put("imdb", it) }
        traktId?.let { ids.put("trakt", it) }

        val itemObj = JSONObject().apply {
            put("title", title)
            year?.let { put("year", it) }
            put("ids", ids)
        }

        val array = org.json.JSONArray().apply { put(itemObj) }
        return JSONObject().apply {
            if (kind == TraktWatchlistKind.MOVIE) {
                put("movies", array)
            } else {
                put("shows", array)
            }
        }
    }

    companion object {
        fun fromJson(json: JSONObject, forcedKind: TraktWatchlistKind? = null): TraktWatchlistItem? {
            val typeStr = json.optString("type")
            val kind = forcedKind ?: when (typeStr) {
                "movie" -> TraktWatchlistKind.MOVIE
                "show" -> TraktWatchlistKind.SHOW
                else -> {
                    if (json.has("movie")) TraktWatchlistKind.MOVIE
                    else if (json.has("show")) TraktWatchlistKind.SHOW
                    else return null
                }
            }
            val media = if (kind == TraktWatchlistKind.MOVIE) {
                json.optJSONObject("movie") ?: json
            } else {
                json.optJSONObject("show") ?: json
            }
            val title = media.optString("title").ifBlank { return null }
            val year = media.optInt("year").takeIf { it > 1800 }
            val ids = media.optJSONObject("ids")
            val tmdbId = ids?.optInt("tmdb")?.takeIf { it > 0 }?.toString()
                ?: ids?.optString("tmdb")?.takeIf { it.isNotBlank() && it != "0" }
            val imdbId = ids?.optString("imdb")?.takeIf { it.isNotBlank() }
            val traktId = ids?.optInt("trakt")?.takeIf { it > 0 }
            val slug = ids?.optString("slug")?.takeIf { it.isNotBlank() }
            val listedAt = json.optString("listed_at").takeIf { it.isNotBlank() }

            return TraktWatchlistItem(
                kind = kind,
                title = title,
                year = year,
                tmdbId = tmdbId,
                imdbId = imdbId,
                traktId = traktId,
                slug = slug,
                listedAt = listedAt
            )
        }
    }
}

data class TraktWatchedMovie(
    val title: String,
    val year: Int?,
    val tmdbId: String?,
    val imdbId: String?,
    val traktId: Int?,
    val plays: Int = 1,
    val lastWatchedAt: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("title", title)
        year?.let { put("year", it) }
        tmdbId?.let { put("tmdb", it) }
        imdbId?.let { put("imdb", it) }
        traktId?.let { put("trakt", it) }
        put("plays", plays)
        lastWatchedAt?.let { put("last_watched_at", it) }
    }

    companion object {
        fun fromJson(json: JSONObject): TraktWatchedMovie? {
            val movie = json.optJSONObject("movie") ?: json
            val title = movie.optString("title").ifBlank { return null }
            val year = movie.optInt("year").takeIf { it > 1800 }
            val ids = movie.optJSONObject("ids")
            val tmdbId = ids?.optInt("tmdb")?.takeIf { it > 0 }?.toString()
                ?: ids?.optString("tmdb")?.takeIf { it.isNotBlank() && it != "0" }
                ?: json.optString("tmdb").takeIf { it.isNotBlank() }
            val imdbId = ids?.optString("imdb")?.takeIf { it.isNotBlank() }
                ?: json.optString("imdb").takeIf { it.isNotBlank() }
            val traktId = ids?.optInt("trakt")?.takeIf { it > 0 }
                ?: json.optInt("trakt").takeIf { it > 0 }
            val plays = json.optInt("plays", 1)
            val lastWatchedAt = json.optString("last_watched_at").takeIf { it.isNotBlank() }

            return TraktWatchedMovie(
                title = title,
                year = year,
                tmdbId = tmdbId,
                imdbId = imdbId,
                traktId = traktId,
                plays = plays,
                lastWatchedAt = lastWatchedAt
            )
        }
    }
}

data class TraktWatchedShow(
    val title: String,
    val year: Int?,
    val tmdbId: String?,
    val imdbId: String?,
    val traktId: Int?,
    val seasons: Map<Int, Set<Int>> = emptyMap(), // seasonNumber -> Set of episode numbers
    val plays: Int = 1,
    val lastWatchedAt: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("title", title)
        year?.let { put("year", it) }
        tmdbId?.let { put("tmdb", it) }
        imdbId?.let { put("imdb", it) }
        traktId?.let { put("trakt", it) }
        put("plays", plays)
        lastWatchedAt?.let { put("last_watched_at", it) }
        val seasonsObj = JSONObject()
        for ((sNum, eps) in seasons) {
            val epsArr = org.json.JSONArray()
            eps.forEach { epsArr.put(it) }
            seasonsObj.put(sNum.toString(), epsArr)
        }
        put("seasons_map", seasonsObj)
    }

    companion object {
        fun fromJson(json: JSONObject): TraktWatchedShow? {
            val show = json.optJSONObject("show") ?: json
            val title = show.optString("title").ifBlank { return null }
            val year = show.optInt("year").takeIf { it > 1800 }
            val ids = show.optJSONObject("ids")
            val tmdbId = ids?.optInt("tmdb")?.takeIf { it > 0 }?.toString()
                ?: ids?.optString("tmdb")?.takeIf { it.isNotBlank() && it != "0" }
                ?: json.optString("tmdb").takeIf { it.isNotBlank() }
            val imdbId = ids?.optString("imdb")?.takeIf { it.isNotBlank() }
                ?: json.optString("imdb").takeIf { it.isNotBlank() }
            val traktId = ids?.optInt("trakt")?.takeIf { it > 0 }
                ?: json.optInt("trakt").takeIf { it > 0 }
            val plays = json.optInt("plays", 1)
            val lastWatchedAt = json.optString("last_watched_at").takeIf { it.isNotBlank() }

            val seasonsMap = mutableMapOf<Int, MutableSet<Int>>()

            // Check if serialized from toJson() with seasons_map
            val seasonsMapObj = json.optJSONObject("seasons_map")
            if (seasonsMapObj != null) {
                val keys = seasonsMapObj.keys()
                while (keys.hasNext()) {
                    val sKey = keys.next()
                    val sNum = sKey.toIntOrNull() ?: continue
                    val epsArr = seasonsMapObj.optJSONArray(sKey) ?: continue
                    val epSet = seasonsMap.getOrPut(sNum) { mutableSetOf() }
                    for (i in 0 until epsArr.length()) {
                        epSet.add(epsArr.optInt(i))
                    }
                }
            } else {
                // Parse standard Trakt API seasons array: [{"number": 1, "episodes": [{"number": 1}]}]
                val seasonsArr = json.optJSONArray("seasons")
                if (seasonsArr != null) {
                    for (sIdx in 0 until seasonsArr.length()) {
                        val sObj = seasonsArr.optJSONObject(sIdx) ?: continue
                        val sNum = sObj.optInt("number", -1)
                        if (sNum < 0) continue
                        val epSet = seasonsMap.getOrPut(sNum) { mutableSetOf() }
                        val epsArr = sObj.optJSONArray("episodes")
                        if (epsArr != null) {
                            for (eIdx in 0 until epsArr.length()) {
                                val epObj = epsArr.optJSONObject(eIdx) ?: continue
                                val epNum = epObj.optInt("number", -1)
                                if (epNum >= 0) epSet.add(epNum)
                            }
                        }
                    }
                }
            }

            return TraktWatchedShow(
                title = title,
                year = year,
                tmdbId = tmdbId,
                imdbId = imdbId,
                traktId = traktId,
                seasons = seasonsMap,
                plays = plays,
                lastWatchedAt = lastWatchedAt
            )
        }
    }
}

object TraktSyncPayloadBuilder {
    fun moviePayload(
        title: String,
        year: Int?,
        tmdbId: String?,
        imdbId: String?
    ): JSONObject {
        val ids = JSONObject()
        tmdbId?.toIntOrNull()?.let { ids.put("tmdb", it) }
        imdbId?.takeIf { it.isNotBlank() }?.let { ids.put("imdb", it) }

        val movieObj = JSONObject().apply {
            put("title", title)
            year?.let { put("year", it) }
            put("ids", ids)
        }
        val arr = org.json.JSONArray().apply { put(movieObj) }
        return JSONObject().apply { put("movies", arr) }
    }

    fun episodePayload(
        showTitle: String,
        showYear: Int?,
        showTmdbId: String?,
        showImdbId: String?,
        seasonNumber: Int,
        episodeNumber: Int
    ): JSONObject {
        val ids = JSONObject()
        showTmdbId?.toIntOrNull()?.let { ids.put("tmdb", it) }
        showImdbId?.takeIf { it.isNotBlank() }?.let { ids.put("imdb", it) }

        val episodeObj = JSONObject().apply {
            put("number", episodeNumber)
        }
        val episodesArr = org.json.JSONArray().apply { put(episodeObj) }

        val seasonObj = JSONObject().apply {
            put("number", seasonNumber)
            put("episodes", episodesArr)
        }
        val seasonsArr = org.json.JSONArray().apply { put(seasonObj) }

        val showObj = JSONObject().apply {
            put("title", showTitle)
            showYear?.let { put("year", it) }
            put("ids", ids)
            put("seasons", seasonsArr)
        }
        val showsArr = org.json.JSONArray().apply { put(showObj) }

        return JSONObject().apply { put("shows", showsArr) }
    }
}

data class TraktRecommendationItem(
    val kind: TraktWatchlistKind,
    val title: String,
    val year: Int?,
    val tmdbId: String?,
    val imdbId: String?,
    val traktId: Int?,
    val slug: String?
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("type", if (kind == TraktWatchlistKind.MOVIE) "movie" else "show")
        put("title", title)
        year?.let { put("year", it) }
        val ids = JSONObject()
        tmdbId?.let { ids.put("tmdb", it) }
        imdbId?.let { ids.put("imdb", it) }
        traktId?.let { ids.put("trakt", it) }
        slug?.let { ids.put("slug", it) }
        put("ids", ids)
    }

    companion object {
        fun fromJson(json: JSONObject, forcedKind: TraktWatchlistKind? = null): TraktRecommendationItem? {
            val typeStr = json.optString("type")
            val kind = forcedKind ?: when (typeStr) {
                "movie" -> TraktWatchlistKind.MOVIE
                "show" -> TraktWatchlistKind.SHOW
                else -> {
                    if (json.has("movie")) TraktWatchlistKind.MOVIE
                    else if (json.has("show")) TraktWatchlistKind.SHOW
                    else TraktWatchlistKind.MOVIE
                }
            }
            val media = if (kind == TraktWatchlistKind.MOVIE) {
                json.optJSONObject("movie") ?: json
            } else {
                json.optJSONObject("show") ?: json
            }
            val title = media.optString("title").ifBlank { return null }
            val year = media.optInt("year").takeIf { it > 1800 }
            val ids = media.optJSONObject("ids")
            val tmdbId = ids?.optInt("tmdb")?.takeIf { it > 0 }?.toString()
                ?: ids?.optString("tmdb")?.takeIf { it.isNotBlank() && it != "0" }
                ?: json.optString("tmdb").takeIf { it.isNotBlank() }
            val imdbId = ids?.optString("imdb")?.takeIf { it.isNotBlank() }
                ?: json.optString("imdb").takeIf { it.isNotBlank() }
            val traktId = ids?.optInt("trakt")?.takeIf { it > 0 }
                ?: json.optInt("trakt").takeIf { it > 0 }
            val slug = ids?.optString("slug")?.takeIf { it.isNotBlank() }

            return TraktRecommendationItem(
                kind = kind,
                title = title,
                year = year,
                tmdbId = tmdbId,
                imdbId = imdbId,
                traktId = traktId,
                slug = slug
            )
        }
    }
}
