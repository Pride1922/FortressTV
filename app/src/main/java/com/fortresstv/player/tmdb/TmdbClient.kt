package com.fortresstv.player.tmdb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.util.Locale

class TmdbClient(
    private val apiKey: String,
    private val apiBaseUrl: String = DEFAULT_BASE_URL,
    private val imageBaseUrl: String = DEFAULT_IMAGE_BASE_URL
) {

    private val isBearerToken: Boolean = apiKey.trim().startsWith("eyJ") || apiKey.trim().length > 60

    suspend fun validateApiKey(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "/3/configuration"
            val response = executeRequest(endpoint)
            val json = JSONObject(response)
            if (json.has("images")) {
                true
            } else {
                val statusMessage = json.optString("status_message", "Invalid response")
                error(statusMessage)
            }
        }
    }

    suspend fun getMovieDetails(tmdbId: Int, language: String = "pt-PT"): TmdbMetadata? = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "/3/movie/$tmdbId"
            val queryParams = mapOf(
                "language" to language,
                "append_to_response" to "credits,videos"
            )
            val raw = executeRequest(endpoint, queryParams)
            val json = JSONObject(raw)
            var overview = json.optString("overview").trim().takeIf(String::isNotEmpty)
            if (overview == null && !language.equals("en-US", ignoreCase = true)) {
                overview = fetchFallbackOverview(endpoint)
            }
            parseMovieMetadata(json, overview)
        }.getOrNull()
    }

    suspend fun getSeriesDetails(tmdbId: Int, language: String = "pt-PT"): TmdbMetadata? = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "/3/tv/$tmdbId"
            val queryParams = mapOf(
                "language" to language,
                "append_to_response" to "credits,videos"
            )
            val raw = executeRequest(endpoint, queryParams)
            val json = JSONObject(raw)
            var overview = json.optString("overview").trim().takeIf(String::isNotEmpty)
            if (overview == null && !language.equals("en-US", ignoreCase = true)) {
                overview = fetchFallbackOverview(endpoint)
            }
            parseSeriesMetadata(json, overview)
        }.getOrNull()
    }

    suspend fun findMovieByImdb(imdbId: String, language: String = "pt-PT"): TmdbMetadata? = withContext(Dispatchers.IO) {
        val cleanImdb = normalizeImdbId(imdbId) ?: return@withContext null
        runCatching {
            val endpoint = "/3/find/$cleanImdb"
            val queryParams = mapOf(
                "external_source" to "imdb_id",
                "language" to language
            )
            val raw = executeRequest(endpoint, queryParams)
            val root = JSONObject(raw)
            val movieResults = root.optJSONArray("movie_results")
            if (movieResults != null && movieResults.length() > 0) {
                val tmdbId = movieResults.getJSONObject(0).optInt("id", -1)
                if (tmdbId > 0) {
                    return@withContext getMovieDetails(tmdbId, language)
                }
            }
            null
        }.getOrNull()
    }

    suspend fun findSeriesByImdb(imdbId: String, language: String = "pt-PT"): TmdbMetadata? = withContext(Dispatchers.IO) {
        val cleanImdb = normalizeImdbId(imdbId) ?: return@withContext null
        runCatching {
            val endpoint = "/3/find/$cleanImdb"
            val queryParams = mapOf(
                "external_source" to "imdb_id",
                "language" to language
            )
            val raw = executeRequest(endpoint, queryParams)
            val root = JSONObject(raw)
            val tvResults = root.optJSONArray("tv_results")
            if (tvResults != null && tvResults.length() > 0) {
                val tmdbId = tvResults.getJSONObject(0).optInt("id", -1)
                if (tmdbId > 0) {
                    return@withContext getSeriesDetails(tmdbId, language)
                }
            }
            null
        }.getOrNull()
    }

    suspend fun searchMovie(title: String, year: String? = null, language: String = "pt-PT"): TmdbMetadata? = withContext(Dispatchers.IO) {
        val (cleanTitle, extractedYear) = cleanMediaTitle(title)
        val targetYear = year?.trim()?.takeIf { it.length == 4 && it.all(Char::isDigit) } ?: extractedYear
        if (cleanTitle.isBlank()) return@withContext null

        val resultWithYear = if (targetYear != null) {
            executeSearchMovie(cleanTitle, targetYear, language)
        } else null

        val finalResult = resultWithYear ?: executeSearchMovie(cleanTitle, null, language)
        finalResult?.let { id -> getMovieDetails(id, language) }
    }

    suspend fun searchSeries(title: String, year: String? = null, language: String = "pt-PT"): TmdbMetadata? = withContext(Dispatchers.IO) {
        val (cleanTitle, extractedYear) = cleanMediaTitle(title)
        val targetYear = year?.trim()?.takeIf { it.length == 4 && it.all(Char::isDigit) } ?: extractedYear
        if (cleanTitle.isBlank()) return@withContext null

        val resultWithYear = if (targetYear != null) {
            executeSearchSeries(cleanTitle, targetYear, language)
        } else null

        val finalResult = resultWithYear ?: executeSearchSeries(cleanTitle, null, language)
        finalResult?.let { id -> getSeriesDetails(id, language) }
    }

    private suspend fun executeSearchMovie(cleanTitle: String, year: String?, language: String): Int? = runCatching {
        val endpoint = "/3/search/movie"
        val params = mutableMapOf(
            "query" to cleanTitle,
            "language" to language,
            "include_adult" to "false"
        )
        if (year != null) {
            params["year"] = year
        }
        val raw = executeRequest(endpoint, params)
        val results = JSONObject(raw).optJSONArray("results")
        if (results != null && results.length() > 0) {
            results.getJSONObject(0).optInt("id", -1).takeIf { it > 0 }
        } else null
    }.getOrNull()

    private suspend fun executeSearchSeries(cleanTitle: String, year: String?, language: String): Int? = runCatching {
        val endpoint = "/3/search/tv"
        val params = mutableMapOf(
            "query" to cleanTitle,
            "language" to language,
            "include_adult" to "false"
        )
        if (year != null) {
            params["first_air_date_year"] = year
        }
        val raw = executeRequest(endpoint, params)
        val results = JSONObject(raw).optJSONArray("results")
        if (results != null && results.length() > 0) {
            results.getJSONObject(0).optInt("id", -1).takeIf { it > 0 }
        } else null
    }.getOrNull()

    private suspend fun fetchFallbackOverview(endpoint: String): String? = runCatching {
        val raw = executeRequest(endpoint, mapOf("language" to "en-US"))
        val json = JSONObject(raw)
        json.optString("overview").trim().takeIf(String::isNotEmpty)
    }.getOrNull()

    private fun parseMovieMetadata(json: JSONObject, overviewOverride: String?): TmdbMetadata {
        val id = json.optInt("id")
        val title = json.optString("title").ifBlank { json.optString("original_title") }
        val posterPath = json.optString("poster_path").trim().takeIf(String::isNotEmpty)
        val backdropPath = json.optString("backdrop_path").trim().takeIf(String::isNotEmpty)
        val voteAvg = json.optDouble("vote_average", 0.0)
        val rating = if (voteAvg > 0.0) String.format(Locale.US, "%.1f", voteAvg) else null
        val releaseDate = json.optString("release_date").trim().takeIf(String::isNotEmpty)

        val genres = parseGenres(json.optJSONArray("genres"))
        val credits = json.optJSONObject("credits")
        val director = parseDirector(credits?.optJSONArray("crew"))
        val cast = parseCast(credits?.optJSONArray("cast"))
        val trailerUrl = parseTrailerUrl(json.optJSONObject("videos")?.optJSONArray("results"))

        return TmdbMetadata(
            tmdbId = id,
            title = title,
            overview = overviewOverride ?: json.optString("overview").trim().takeIf(String::isNotEmpty),
            posterUrl = posterPath?.let { "$imageBaseUrl/w500$it" },
            backdropUrl = backdropPath?.let { "$imageBaseUrl/w1280$it" },
            rating = rating,
            releaseDate = releaseDate,
            genres = genres,
            director = director,
            cast = cast,
            trailerUrl = trailerUrl
        )
    }

    private fun parseSeriesMetadata(json: JSONObject, overviewOverride: String?): TmdbMetadata {
        val id = json.optInt("id")
        val title = json.optString("name").ifBlank { json.optString("original_name") }
        val posterPath = json.optString("poster_path").trim().takeIf(String::isNotEmpty)
        val backdropPath = json.optString("backdrop_path").trim().takeIf(String::isNotEmpty)
        val voteAvg = json.optDouble("vote_average", 0.0)
        val rating = if (voteAvg > 0.0) String.format(Locale.US, "%.1f", voteAvg) else null
        val releaseDate = json.optString("first_air_date").trim().takeIf(String::isNotEmpty)

        val genres = parseGenres(json.optJSONArray("genres"))
        val credits = json.optJSONObject("credits")
        val director = parseDirector(credits?.optJSONArray("crew"))
            ?: parseCreatedBy(json.optJSONArray("created_by"))
        val cast = parseCast(credits?.optJSONArray("cast"))
        val trailerUrl = parseTrailerUrl(json.optJSONObject("videos")?.optJSONArray("results"))

        return TmdbMetadata(
            tmdbId = id,
            title = title,
            overview = overviewOverride ?: json.optString("overview").trim().takeIf(String::isNotEmpty),
            posterUrl = posterPath?.let { "$imageBaseUrl/w500$it" },
            backdropUrl = backdropPath?.let { "$imageBaseUrl/w1280$it" },
            rating = rating,
            releaseDate = releaseDate,
            genres = genres,
            director = director,
            cast = cast,
            trailerUrl = trailerUrl
        )
    }

    private fun parseGenres(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val name = item.optString("name").trim()
            if (name.isNotEmpty()) list.add(name)
        }
        return list
    }

    private fun parseDirector(crew: JSONArray?): String? {
        if (crew == null) return null
        for (i in 0 until crew.length()) {
            val member = crew.optJSONObject(i) ?: continue
            if (member.optString("job").equals("Director", ignoreCase = true)) {
                val name = member.optString("name").trim()
                if (name.isNotEmpty()) return name
            }
        }
        return null
    }

    private fun parseCreatedBy(creators: JSONArray?): String? {
        if (creators == null || creators.length() == 0) return null
        return creators.optJSONObject(0)?.optString("name")?.trim()?.takeIf(String::isNotEmpty)
    }

    private fun parseCast(castArray: JSONArray?): List<String> {
        if (castArray == null) return emptyList()
        val list = mutableListOf<String>()
        val max = minOf(castArray.length(), 10)
        for (i in 0 until max) {
            val actor = castArray.optJSONObject(i) ?: continue
            val name = actor.optString("name").trim()
            if (name.isNotEmpty()) list.add(name)
        }
        return list
    }

    private fun parseTrailerUrl(videos: JSONArray?): String? {
        if (videos == null) return null
        var teaserKey: String? = null
        for (i in 0 until videos.length()) {
            val video = videos.optJSONObject(i) ?: continue
            if (!video.optString("site").equals("YouTube", ignoreCase = true)) continue
            val type = video.optString("type")
            val key = video.optString("key").trim()
            if (key.isBlank()) continue
            if (type.equals("Trailer", ignoreCase = true)) {
                return "https://www.youtube.com/watch?v=$key"
            } else if (type.equals("Teaser", ignoreCase = true) && teaserKey == null) {
                teaserKey = key
            }
        }
        return teaserKey?.let { "https://www.youtube.com/watch?v=$it" }
    }

    private fun executeRequest(endpoint: String, params: Map<String, String> = emptyMap()): String {
        val queryParams = mutableMapOf<String, String>()
        if (!isBearerToken) {
            queryParams["api_key"] = apiKey.trim()
        }
        queryParams.putAll(params)

        val queryString = if (queryParams.isNotEmpty()) {
            "?" + queryParams.entries.joinToString("&") { "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}" }
        } else ""

        val url = URI("$apiBaseUrl$endpoint$queryString").toURL()
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6_000
            readTimeout = 10_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "FortressTV/1.0")
            if (isBearerToken) {
                setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
            }
        }

        try {
            val code = connection.responseCode
            val stream: InputStream = if (code in 200..299) connection.inputStream else connection.errorStream ?: connection.inputStream
            val text = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) {
                throw HttpStatusException(code, text)
            }
            return text
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.themoviedb.org"
        const val DEFAULT_IMAGE_BASE_URL = "https://image.tmdb.org/t/p"

        internal fun normalizeImdbId(raw: String): String? {
            val trimmed = raw.trim()
            if (trimmed.isBlank()) return null
            return if (trimmed.startsWith("tt", ignoreCase = true)) {
                trimmed.lowercase(Locale.ROOT)
            } else {
                val digits = trimmed.filter(Char::isDigit)
                if (digits.isNotEmpty()) "tt$digits" else null
            }
        }

        internal fun cleanMediaTitle(rawTitle: String): Pair<String, String?> {
            var title = rawTitle.trim()

            // 1. Extract year if present, e.g. "(2021)" or "[2021]" or " 2021"
            val yearMatch = Regex("(?:\\(|\\s|\\[)([12][0-9]{3})(?:\\)|\\s|\\]|$)").find(title)
            val extractedYear = yearMatch?.groupValues?.getOrNull(1)

            // 2. Remove language tags, country tags, quality tags, and common release suffixes
            val patternsToRemove = listOf(
                Regex("(?i)^\\[?(?:pt-pt|pt-br|pt|en|es|fr|de|it|legendado|dublado|dual|multi|leg\\.?)[\\]:\\s-]+"),
                Regex("(?i)\\[(?:pt-pt|pt-br|pt|en|es|fr|de|it|legendado|dublado|dual|multi|leg|sub|subbed)\\]"),
                Regex("(?i)\\((?:pt-pt|pt-br|pt|en|es|fr|de|it|legendado|dublado|dual|multi|leg|sub|subbed)\\)"),
                Regex("(?i)\\b(?:1080p|720p|4k|uhd|fhd|hd|sd|hevc|h264|h265|x264|x265|10bit|hdr|web-?dl|bluray|web-?rip|bdrip|dvdrip|aac|atmos)\\b"),
                Regex("\\([12][0-9]{3}\\)"),
                Regex("\\[[12][0-9]{3}\\]"),
                Regex("\\[[^\\]]*\\]")
            )

            for (pattern in patternsToRemove) {
                title = title.replace(pattern, " ")
            }

            // Remove non-word punctuation except apostrophe and hyphen, replace with space
            title = title.replace(Regex("[^\\p{L}0-9'\\s-]"), " ")
            title = title.replace(Regex("\\s+"), " ").trim()

            return Pair(title, extractedYear)
        }
    }

    class HttpStatusException(val code: Int, val body: String) : Exception("HTTP $code: $body")
}
