package com.fortresstv.player.library

import android.content.Context
import android.content.SharedPreferences
import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject

enum class VodProgressKind {
    MOVIE, EPISODE, CATCHUP;

    val wireValue: String
        get() = when (this) {
            MOVIE -> "movie"
            EPISODE -> "episode"
            CATCHUP -> "catchup"
        }

    companion object {
        fun fromWireValue(value: String): VodProgressKind = when (value.trim().lowercase()) {
            "movie" -> MOVIE
            "catchup" -> CATCHUP
            else -> EPISODE
        }
    }
}

data class CatchupProgressKey(
    val channelId: String,
    val programStartUtc: Instant
) {
    fun toItemId(): String = "catchup:${channelId.trim().lowercase()}:${programStartUtc.toEpochMilli()}"

    companion object {
        fun fromItemId(itemId: String): CatchupProgressKey? {
            val parts = itemId.split(":")
            if (parts.size >= 3 && parts[0] == "catchup") {
                val chId = parts[1]
                val epochMilli = parts[2].toLongOrNull() ?: return null
                return CatchupProgressKey(chId, Instant.ofEpochMilli(epochMilli))
            }
            val underscoreParts = itemId.split("_")
            if (underscoreParts.size == 2) {
                val streamOrChannel = underscoreParts[0]
                val epochMilli = underscoreParts[1].toLongOrNull() ?: return null
                return CatchupProgressKey(streamOrChannel, Instant.ofEpochMilli(epochMilli))
            }
            return null
        }
    }
}

fun profileProgressNamespace(householdId: String?, profileId: String): String {
    return if (profileId.isBlank() || profileId == "restricted_default") {
        "restricted_default"
    } else if (!householdId.isNullOrBlank()) {
        "${householdId}_$profileId"
    } else {
        "profile_$profileId"
    }
}

data class VodProgressEntry(
    val key: String,
    val kind: VodProgressKind,
    val sourceId: String,
    val itemId: String,
    val seriesId: Int?,
    val title: String,
    val posterUrl: String?,
    val containerExtension: String,
    val season: Int?,
    val episode: Int?,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtMs: Long
) {
    val progressFraction: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

class VodProgressStore internal constructor(
    private val prefsProvider: (String) -> SharedPreferences,
    initialNamespace: String = "local"
) {
    constructor(context: Context, initialNamespace: String = "local") : this(
        prefsProvider = { ns ->
            if (ns == "local" || ns == "default") {
                context.getSharedPreferences("fortress_vod_progress", Context.MODE_PRIVATE)
            } else {
                context.getSharedPreferences("fortress_vod_progress_$ns", Context.MODE_PRIVATE)
            }
        },
        initialNamespace = initialNamespace
    )

    private val lock = Any()
    @Volatile
    private var namespace = sanitize(initialNamespace)
    @Volatile
    private var preferences: SharedPreferences = prefsProvider(namespace)

    private fun sanitize(raw: String): String = raw.replace(Regex("[^a-zA-Z0-9_-]"), "_")

    fun currentNamespace(): String = synchronized(lock) { namespace }

    fun switchNamespace(newNamespace: String) = synchronized(lock) {
        val sanitized = sanitize(newNamespace)
        if (namespace != sanitized) {
            namespace = sanitized
            preferences = prefsProvider(namespace)
        }
    }

    fun entries(): List<VodProgressEntry> = synchronized(lock) {
        decode(preferences.getString(KEY_ENTRIES, null))
    }

    fun findProgress(kind: VodProgressKind, sourceId: String, rawItemId: String): VodProgressEntry? = synchronized(lock) {
        val hash = CloudIdentityHasher.hashItemId(rawItemId)
        entries().firstOrNull {
            it.kind == kind && it.sourceId == sourceId && (it.itemId == rawItemId || it.itemId == hash)
        }
    }

    fun isDismissed(key: String, seriesId: String? = null): Boolean = synchronized(lock) {
        val dismissed = preferences.getStringSet(KEY_DISMISSED, emptySet()).orEmpty()
        if (dismissed.contains(key)) return true
        if (seriesId != null && dismissed.contains("series:$seriesId")) return true
        val parts = key.split(":")
        if (parts.size >= 3) {
            val hash = CloudIdentityHasher.hashItemId(parts[2])
            if (dismissed.contains("${parts[0]}:${parts[1]}:$hash")) return true
        }
        return false
    }

    fun dismiss(key: String, seriesId: String? = null) = synchronized(lock) {
        val dismissed = preferences.getStringSet(KEY_DISMISSED, emptySet()).orEmpty().toMutableSet()
        dismissed.add(key)
        if (seriesId != null) {
            dismissed.add("series:$seriesId")
        }
        preferences.edit().putStringSet(KEY_DISMISSED, dismissed).apply()
    }

    fun undismiss(key: String, seriesId: String? = null) = synchronized(lock) {
        val dismissed = preferences.getStringSet(KEY_DISMISSED, emptySet()).orEmpty().toMutableSet()
        var changed = dismissed.remove(key)
        if (seriesId != null) {
            changed = dismissed.remove("series:$seriesId") || changed
        }
        val parts = key.split(":")
        if (parts.size >= 3) {
            val hash = CloudIdentityHasher.hashItemId(parts[2])
            changed = dismissed.remove("${parts[0]}:${parts[1]}:$hash") || changed
        }
        if (changed) {
            preferences.edit().putStringSet(KEY_DISMISSED, dismissed).apply()
        }
    }

    fun saveNextEpisode(entry: VodProgressEntry): List<VodProgressEntry> = synchronized(lock) {
        val hash = CloudIdentityHasher.hashItemId(entry.itemId)
        undismiss(entry.key, entry.seriesId?.toString())
        val updated = entries().filterNot {
            it.key == entry.key ||
            (it.kind == entry.kind && it.sourceId == entry.sourceId && (
                it.itemId == entry.itemId ||
                it.itemId == hash ||
                (entry.seriesId != null && it.seriesId != null && it.seriesId == entry.seriesId)
            ))
        }.toMutableList()
        updated.add(0, entry)
        val limited = updated.sortedByDescending(VodProgressEntry::updatedAtMs).take(MAX_ENTRIES)
        preferences.edit().putString(KEY_ENTRIES, encode(limited)).apply()
        limited
    }

    fun save(entry: VodProgressEntry): List<VodProgressEntry> = synchronized(lock) {
        val hash = CloudIdentityHasher.hashItemId(entry.itemId)
        undismiss(entry.key, entry.seriesId?.toString())
        val existing = entries().firstOrNull {
            it.key == entry.key || (it.kind == entry.kind && it.sourceId == entry.sourceId && (
                it.itemId == entry.itemId ||
                it.itemId == hash ||
                (entry.seriesId != null && it.seriesId != null && it.seriesId == entry.seriesId)
            ))
        }
        if (entry.positionMs < MINIMUM_PROGRESS_MS) {
            if (existing != null && (existing.positionMs >= MINIMUM_PROGRESS_MS || existing.positionMs == 0L)) {
                return entries()
            }
        }
        val updated = entries().filterNot {
            it.key == entry.key || (it.kind == entry.kind && it.sourceId == entry.sourceId && (
                it.itemId == entry.itemId ||
                it.itemId == hash ||
                (entry.seriesId != null && it.seriesId != null && it.seriesId == entry.seriesId)
            ))
        }.toMutableList()
        if (entry.positionMs >= MINIMUM_PROGRESS_MS &&
            (entry.durationMs <= 0 || entry.positionMs < entry.durationMs * COMPLETED_FRACTION)
        ) {
            updated += entry
        }
        val limited = updated.sortedByDescending(VodProgressEntry::updatedAtMs).take(MAX_ENTRIES)
        preferences.edit().putString(KEY_ENTRIES, encode(limited)).apply()
        limited
    }

    fun remove(key: String, seriesId: Int? = null): List<VodProgressEntry> = synchronized(lock) {
        dismiss(key, seriesId?.toString())
        val parts = key.split(":")
        val hashedKey = if (parts.size >= 3) {
            "${parts[0]}:${parts[1]}:${CloudIdentityHasher.hashItemId(parts[2])}"
        } else null
        val updated = entries().filterNot {
            it.key == key || (hashedKey != null && it.key == hashedKey) ||
            (seriesId != null && it.kind == VodProgressKind.EPISODE && it.seriesId == seriesId)
        }
        preferences.edit().putString(KEY_ENTRIES, encode(updated)).apply()
        updated
    }

    fun setAll(entries: List<VodProgressEntry>): List<VodProgressEntry> = synchronized(lock) {
        val limited = distinctResumeEntries(entries).take(MAX_ENTRIES)
        preferences.edit().putString(KEY_ENTRIES, encode(limited)).apply()
        limited
    }

    fun clear(): List<VodProgressEntry> = synchronized(lock) {
        preferences.edit().remove(KEY_ENTRIES).remove(KEY_DISMISSED).apply()
        emptyList()
    }

    companion object {
        private const val KEY_ENTRIES = "entries_json"
        private const val KEY_DISMISSED = "dismissed_keys"
        private const val MINIMUM_PROGRESS_MS = 10_000L
        private const val COMPLETED_FRACTION = 0.95
        private const val MAX_ENTRIES = 100
        private val CLOUD_ITEM_ID = Regex("[0-9a-f]{64}")

        private fun distinctResumeEntries(entries: List<VodProgressEntry>): List<VodProgressEntry> {
            val items = mutableSetOf<Triple<VodProgressKind, String, String>>()
            val series = mutableSetOf<Pair<String, Int>>()
            return entries.sortedByDescending(VodProgressEntry::updatedAtMs).filter { entry ->
                val itemId = entry.itemId
                val canonicalId = when {
                    itemId.isBlank() -> entry.key
                    CLOUD_ITEM_ID.matches(itemId) -> itemId
                    else -> CloudIdentityHasher.hashItemId(itemId)
                }
                val seriesKey = entry.seriesId?.takeIf { entry.kind == VodProgressKind.EPISODE }
                    ?.let { entry.sourceId to it }
                val newItem = items.add(Triple(entry.kind, entry.sourceId, canonicalId))
                newItem && (seriesKey == null || series.add(seriesKey))
            }
        }

        internal fun encode(entries: List<VodProgressEntry>): String = JSONArray().apply {
            entries.forEach { entry ->
                put(JSONObject().apply {
                    put("key", entry.key)
                    put("kind", entry.kind.name)
                    put("sourceId", entry.sourceId)
                    put("itemId", entry.itemId)
                    put("seriesId", entry.seriesId ?: JSONObject.NULL)
                    put("title", entry.title)
                    put("posterUrl", entry.posterUrl ?: JSONObject.NULL)
                    put("containerExtension", entry.containerExtension)
                    put("season", entry.season ?: JSONObject.NULL)
                    put("episode", entry.episode ?: JSONObject.NULL)
                    put("positionMs", entry.positionMs)
                    put("durationMs", entry.durationMs)
                    put("updatedAtMs", entry.updatedAtMs)
                })
            }
        }.toString()

        internal fun decode(json: String?): List<VodProgressEntry> = runCatching {
            val array = JSONArray(json ?: "[]")
            val entries = (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val key = item.optString("key").takeIf(String::isNotBlank) ?: return@mapNotNull null
                val sourceId = item.optString("sourceId").takeIf(String::isNotBlank) ?: return@mapNotNull null
                val title = item.optString("title")
                VodProgressEntry(
                    key = key,
                    kind = runCatching { VodProgressKind.valueOf(item.optString("kind")) }.getOrNull() ?: return@mapNotNull null,
                    sourceId = sourceId,
                    itemId = item.optString("itemId"),
                    seriesId = item.optInt("seriesId").takeIf { !item.isNull("seriesId") },
                    title = title,
                    posterUrl = item.optString("posterUrl").takeIf { !item.isNull("posterUrl") && it.isNotBlank() },
                    containerExtension = item.optString("containerExtension").ifBlank { "mp4" },
                    season = item.optInt("season").takeIf { !item.isNull("season") },
                    episode = item.optInt("episode").takeIf { !item.isNull("episode") },
                    positionMs = item.optLong("positionMs").coerceAtLeast(0),
                    durationMs = item.optLong("durationMs").coerceAtLeast(0),
                    updatedAtMs = item.optLong("updatedAtMs")
                )
            }
            distinctResumeEntries(entries)
        }.getOrDefault(emptyList())
    }
}
