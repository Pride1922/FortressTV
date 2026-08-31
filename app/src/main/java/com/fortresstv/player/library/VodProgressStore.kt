package com.fortresstv.player.library

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class VodProgressKind { MOVIE, EPISODE }

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

class VodProgressStore(context: Context) {
    private val preferences = context.getSharedPreferences("fortress_vod_progress", Context.MODE_PRIVATE)

    fun entries(): List<VodProgressEntry> = decode(preferences.getString(KEY_ENTRIES, null))

    fun save(entry: VodProgressEntry): List<VodProgressEntry> {
        val updated = entries().filterNot { it.key == entry.key }.toMutableList()
        if (entry.positionMs >= MINIMUM_PROGRESS_MS &&
            (entry.durationMs <= 0 || entry.positionMs < entry.durationMs * COMPLETED_FRACTION)
        ) {
            updated += entry
        }
        val limited = updated.sortedByDescending(VodProgressEntry::updatedAtMs).take(MAX_ENTRIES)
        preferences.edit().putString(KEY_ENTRIES, encode(limited)).apply()
        return limited
    }

    fun remove(key: String): List<VodProgressEntry> {
        val updated = entries().filterNot { it.key == key }
        preferences.edit().putString(KEY_ENTRIES, encode(updated)).apply()
        return updated
    }

    fun setAll(entries: List<VodProgressEntry>): List<VodProgressEntry> {
        val limited = entries.sortedByDescending(VodProgressEntry::updatedAtMs).take(MAX_ENTRIES)
        preferences.edit().putString(KEY_ENTRIES, encode(limited)).apply()
        return limited
    }

    companion object {
        private const val KEY_ENTRIES = "entries_json"
        private const val MINIMUM_PROGRESS_MS = 10_000L
        private const val COMPLETED_FRACTION = 0.95
        private const val MAX_ENTRIES = 100

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
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val key = item.optString("key").takeIf(String::isNotBlank) ?: return@mapNotNull null
                val sourceId = item.optString("sourceId").takeIf(String::isNotBlank) ?: return@mapNotNull null
                VodProgressEntry(
                    key = key,
                    kind = runCatching { VodProgressKind.valueOf(item.optString("kind")) }.getOrNull() ?: return@mapNotNull null,
                    sourceId = sourceId,
                    itemId = item.optString("itemId"),
                    seriesId = item.optInt("seriesId").takeIf { !item.isNull("seriesId") },
                    title = item.optString("title"),
                    posterUrl = item.optString("posterUrl").takeIf { !item.isNull("posterUrl") && it.isNotBlank() },
                    containerExtension = item.optString("containerExtension").ifBlank { "mp4" },
                    season = item.optInt("season").takeIf { !item.isNull("season") },
                    episode = item.optInt("episode").takeIf { !item.isNull("episode") },
                    positionMs = item.optLong("positionMs").coerceAtLeast(0),
                    durationMs = item.optLong("durationMs").coerceAtLeast(0),
                    updatedAtMs = item.optLong("updatedAtMs")
                )
            }.sortedByDescending(VodProgressEntry::updatedAtMs)
        }.getOrDefault(emptyList())
    }
}
