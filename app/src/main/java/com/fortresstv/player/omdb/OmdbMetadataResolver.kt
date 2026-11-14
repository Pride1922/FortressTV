package com.fortresstv.player.omdb

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

data class OmdbPreFetchTarget(
    val type: String, // "movie" or "series"
    val title: String,
    val year: String? = null,
    val imdbId: String? = null
)

object OmdbMetadataResolver {

    private val NOT_FOUND = OmdbMetadata(imdbId = "", title = "")
    private val memoryCache = ConcurrentHashMap<String, OmdbMetadata>()
    private val sessionRequestedKeys = ConcurrentHashMap.newKeySet<String>()
    private val knownDiskCacheKeys = ConcurrentHashMap.newKeySet<String>()
    @Volatile
    private var isDiskIndexLoaded = false

    private const val CACHE_DIR_NAME = "omdb_cache"
    private const val CACHE_TTL_MS = 14 * 24 * 60 * 60 * 1000L // 14 days
    const val MAX_SESSION_PREFETCH_BUDGET = 50
    const val DEFAULT_PER_SOURCE_ENRICHMENT_BUDGET = 15

    fun clearMemoryCache() {
        memoryCache.clear()
        sessionRequestedKeys.clear()
        knownDiskCacheKeys.clear()
        isDiskIndexLoaded = false
    }

    private fun ensureDiskIndex(context: Context) {
        if (!isDiskIndexLoaded) {
            synchronized(this) {
                if (!isDiskIndexLoaded) {
                    runCatching {
                        val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
                        if (cacheDir.exists()) {
                            val now = System.currentTimeMillis()
                            cacheDir.listFiles()?.forEach { file ->
                                if (file.name.endsWith(".json")) {
                                    if (now - file.lastModified() <= CACHE_TTL_MS) {
                                        knownDiskCacheKeys.add(file.name.removeSuffix(".json"))
                                    } else {
                                        file.delete()
                                    }
                                }
                            }
                        }
                    }
                    isDiskIndexLoaded = true
                }
            }
        }
    }

    /**
     * Fast check whether any OMDb results exist in memory or on disk.
     * Allows skipping thousands of SHA-256 operations if the OMDb cache is empty.
     */
    fun hasAnyCached(): Boolean = memoryCache.isNotEmpty() || knownDiskCacheKeys.isNotEmpty()

    /**
     * Synchronous, zero-latency cache probe for lists and grids.
     * Returns cached [OmdbMetadata] if present in memory or on disk; returns null immediately
     * without making any network requests.
     */
    fun getCached(
        context: Context,
        type: String,
        rawTitle: String,
        year: String? = null,
        imdbId: String? = null
    ): OmdbMetadata? {
        ensureDiskIndex(context)
        val cacheKey = buildCacheKey(type, rawTitle, year, imdbId)
        val cached = memoryCache[cacheKey]
        if (cached != null) {
            return if (cached === NOT_FOUND || cached.imdbId.isNullOrBlank()) null else cached
        }
        if (cacheKey !in knownDiskCacheKeys) {
            return null
        }
        val diskCached = readDiskCache(context, cacheKey)
        if (diskCached != null) {
            memoryCache[cacheKey] = diskCached
            return diskCached
        }
        return null
    }

    fun isDiskCached(context: Context, cacheKey: String): Boolean {
        ensureDiskIndex(context)
        return cacheKey in knownDiskCacheKeys
    }

    suspend fun resolveMovie(
        context: Context,
        settings: OmdbSettings,
        rawTitle: String,
        year: String? = null,
        imdbId: String? = null
    ): OmdbMetadata? = withContext(Dispatchers.IO) {
        if (!settings.enabled || settings.apiKey.isBlank()) return@withContext null

        val cacheKey = buildCacheKey("movie", rawTitle, year, imdbId)
        val cachedMem = memoryCache[cacheKey]
        if (cachedMem != null) {
            return@withContext if (cachedMem === NOT_FOUND || cachedMem.imdbId.isNullOrBlank()) null else cachedMem
        }

        val cachedDisk = readDiskCache(context, cacheKey)
        if (cachedDisk != null) {
            memoryCache[cacheKey] = cachedDisk
            return@withContext cachedDisk
        }

        val client = OmdbClient(settings.apiKey)

        // Priority 1: Exact IMDb ID lookup (if provided by Xtream/source)
        var result: OmdbMetadata? = if (!imdbId.isNullOrBlank()) {
            client.getByImdbId(imdbId)
        } else null

        // Priority 2: Thoroughly normalized title + extracted release year
        val cleanTitle = OmdbClient.cleanSearchTitle(rawTitle)
        val cleanYear = OmdbClient.extractYear(rawTitle, year)

        if (result == null && cleanTitle.isNotBlank()) {
            if (cleanYear != null) {
                result = client.getByTitle(cleanTitle, cleanYear, type = "movie")
            }
            // Priority 3: Fallback search with title only (in case provider release year is off by 1 year)
            if (result == null) {
                result = client.getByTitle(cleanTitle, null, type = "movie")
            }
        }

        if (result != null) {
            writeDiskCache(context, cacheKey, result)
            memoryCache[cacheKey] = result
        } else {
            memoryCache[cacheKey] = NOT_FOUND
        }
        result
    }

    suspend fun resolveSeries(
        context: Context,
        settings: OmdbSettings,
        rawTitle: String,
        year: String? = null,
        imdbId: String? = null
    ): OmdbMetadata? = withContext(Dispatchers.IO) {
        if (!settings.enabled || settings.apiKey.isBlank()) return@withContext null

        val cacheKey = buildCacheKey("series", rawTitle, year, imdbId)
        val cachedMem = memoryCache[cacheKey]
        if (cachedMem != null) {
            return@withContext if (cachedMem === NOT_FOUND || cachedMem.imdbId.isNullOrBlank()) null else cachedMem
        }

        val cachedDisk = readDiskCache(context, cacheKey)
        if (cachedDisk != null) {
            memoryCache[cacheKey] = cachedDisk
            return@withContext cachedDisk
        }

        val client = OmdbClient(settings.apiKey)

        // Priority 1: Exact IMDb ID lookup
        var result: OmdbMetadata? = if (!imdbId.isNullOrBlank()) {
            client.getByImdbId(imdbId)
        } else null

        // Priority 2: Normalized title + extracted release year
        val cleanTitle = OmdbClient.cleanSearchTitle(rawTitle)
        val cleanYear = OmdbClient.extractYear(rawTitle, year)

        if (result == null && cleanTitle.isNotBlank()) {
            if (cleanYear != null) {
                result = client.getByTitle(cleanTitle, cleanYear, type = "series")
            }
            // Priority 3: Fallback search with title only
            if (result == null) {
                result = client.getByTitle(cleanTitle, null, type = "series")
            }
        }

        if (result != null) {
            writeDiskCache(context, cacheKey, result)
            memoryCache[cacheKey] = result
        } else {
            memoryCache[cacheKey] = NOT_FOUND
        }
        result
    }

    /**
     * Pre-fetches OMDb ratings for a small curated batch (Continue Watching, Watchlist, Followed Series).
     * Enforces a strict budget of at most [MAX_SESSION_PREFETCH_BUDGET] network requests per app foreground.
     * Items already cached on disk or in memory consume ZERO budget slots.
     */
    suspend fun prefetchCuratedBatch(
        context: Context,
        settings: OmdbSettings,
        targets: List<OmdbPreFetchTarget>
    ) = withContext(Dispatchers.IO) {
        if (!settings.enabled || settings.apiKey.isBlank()) return@withContext
        var budgetLeft = MAX_SESSION_PREFETCH_BUDGET - sessionRequestedKeys.size
        if (budgetLeft <= 0) return@withContext

        for (target in targets) {
            if (budgetLeft <= 0) break

            val cacheKey = buildCacheKey(target.type, target.title, target.year, target.imdbId)
            // Disk or memory hits do NOT consume network quota
            if (memoryCache.containsKey(cacheKey) || isDiskCached(context, cacheKey)) {
                // Populate memory cache if on disk
                getCached(context, target.type, target.title, target.year, target.imdbId)
                continue
            }

            // If already requested during this app session, skip
            if (!sessionRequestedKeys.add(cacheKey)) {
                continue
            }

            budgetLeft--

            if (target.type == "movie") {
                resolveMovie(context, settings, target.title, target.year, target.imdbId)
            } else {
                resolveSeries(context, settings, target.title, target.year, target.imdbId)
            }

            delay(120) // 120ms gentle throttle between requests
        }
    }

    /**
     * Background enrichment for catalogues with low measured rating coverage.
     * Enforces an independent [perSourceBudget] per source ID and returns the number
     * of newly resolved items with a usable IMDb rating.
     */
    suspend fun prefetchWithPerSourceBudget(
        context: Context,
        settings: OmdbSettings,
        targetsBySource: Map<String, List<OmdbPreFetchTarget>>,
        perSourceBudget: Int = DEFAULT_PER_SOURCE_ENRICHMENT_BUDGET
    ): Int = withContext(Dispatchers.IO) {
        if (!settings.enabled || settings.apiKey.isBlank() || perSourceBudget <= 0) return@withContext 0
        var newlyResolved = 0
        for ((_, targets) in targetsBySource) {
            var sourceBudget = perSourceBudget
            for (target in targets) {
                if (sourceBudget <= 0) break
                val cacheKey = buildCacheKey(target.type, target.title, target.year, target.imdbId)
                if (memoryCache.containsKey(cacheKey) || isDiskCached(context, cacheKey)) {
                    getCached(context, target.type, target.title, target.year, target.imdbId)
                    continue
                }
                if (!sessionRequestedKeys.add(cacheKey)) {
                    continue
                }
                sourceBudget--
                val resolved = if (target.type == "movie") {
                    resolveMovie(context, settings, target.title, target.year, target.imdbId)
                } else {
                    resolveSeries(context, settings, target.title, target.year, target.imdbId)
                }
                if (!resolved?.imdbRating.isNullOrBlank()) {
                    newlyResolved++
                }
                delay(120)
            }
        }
        newlyResolved
    }

    fun buildCacheKey(
        type: String,
        rawTitle: String,
        year: String?,
        imdbId: String?
    ): String {
        val raw = "$type:${rawTitle.trim().lowercase()}:${year.orEmpty()}:${imdbId.orEmpty()}"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun readDiskCache(context: Context, cacheKey: String): OmdbMetadata? {
        return runCatching {
            val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
            if (!cacheDir.exists()) return null
            val file = File(cacheDir, "$cacheKey.json")
            if (!file.exists()) return null
            if (System.currentTimeMillis() - file.lastModified() > CACHE_TTL_MS) {
                file.delete()
                knownDiskCacheKeys.remove(cacheKey)
                return null
            }
            val json = JSONObject(file.readText(Charsets.UTF_8))
            deserializeMetadata(json)
        }.getOrNull()
    }

    private fun writeDiskCache(context: Context, cacheKey: String, metadata: OmdbMetadata) {
        runCatching {
            val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, "$cacheKey.json")
            file.writeText(serializeMetadata(metadata).toString(), Charsets.UTF_8)
            knownDiskCacheKeys.add(cacheKey)
        }
    }

    private fun serializeMetadata(m: OmdbMetadata): JSONObject {
        return JSONObject().apply {
            m.title?.let { put("title", it) }
            m.year?.let { put("year", it) }
            m.rated?.let { put("rated", it) }
            m.released?.let { put("released", it) }
            m.runtime?.let { put("runtime", it) }
            m.genre?.let { put("genre", it) }
            m.director?.let { put("director", it) }
            m.writer?.let { put("writer", it) }
            m.actors?.let { put("actors", it) }
            m.plot?.let { put("plot", it) }
            m.awards?.let { put("awards", it) }
            m.poster?.let { put("poster", it) }
            m.imdbRating?.let { put("imdbRating", it) }
            m.imdbVotes?.let { put("imdbVotes", it) }
            m.imdbId?.let { put("imdbId", it) }
            m.type?.let { put("type", it) }
            m.rottenTomatoes?.let { put("rottenTomatoes", it) }
            m.metascore?.let { put("metascore", it) }
            m.boxOffice?.let { put("boxOffice", it) }
        }
    }

    private fun deserializeMetadata(json: JSONObject): OmdbMetadata {
        return OmdbMetadata(
            title = json.optString("title").takeIf(String::isNotBlank),
            year = json.optString("year").takeIf(String::isNotBlank),
            rated = json.optString("rated").takeIf(String::isNotBlank),
            released = json.optString("released").takeIf(String::isNotBlank),
            runtime = json.optString("runtime").takeIf(String::isNotBlank),
            genre = json.optString("genre").takeIf(String::isNotBlank),
            director = json.optString("director").takeIf(String::isNotBlank),
            writer = json.optString("writer").takeIf(String::isNotBlank),
            actors = json.optString("actors").takeIf(String::isNotBlank),
            plot = json.optString("plot").takeIf(String::isNotBlank),
            awards = json.optString("awards").takeIf(String::isNotBlank),
            poster = json.optString("poster").takeIf(String::isNotBlank),
            imdbRating = json.optString("imdbRating").takeIf(String::isNotBlank),
            imdbVotes = json.optString("imdbVotes").takeIf(String::isNotBlank),
            imdbId = json.optString("imdbId").takeIf(String::isNotBlank),
            type = json.optString("type").takeIf(String::isNotBlank),
            rottenTomatoes = json.optString("rottenTomatoes").takeIf(String::isNotBlank),
            metascore = json.optString("metascore").takeIf(String::isNotBlank),
            boxOffice = json.optString("boxOffice").takeIf(String::isNotBlank)
        )
    }
}
