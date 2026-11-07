package com.fortresstv.player.library

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

data class CloudLibraryItem(
    val householdId: String,
    val profileId: String,
    val sourceId: String,
    val itemKind: String, // "channel", "movie", "series", "episode"
    val itemId: String, // 64-character lowercase hex sha256 of the raw local ID
    val isFavourite: Boolean = false,
    val isWatchlisted: Boolean = false,
    val isWatched: Boolean = false,
    val clientUpdatedAtMs: Long = System.currentTimeMillis(),
    val deletedAt: String? = null,
    val updatedAt: String? = null
)

data class CloudWatchProgressItem(
    val householdId: String,
    val profileId: String,
    val sourceId: String,
    val itemKind: String, // "movie", "episode"
    val itemId: String, // 64-character lowercase hex sha256 of the raw local ID
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val completed: Boolean = false,
    val clientUpdatedAtMs: Long = System.currentTimeMillis(),
    val deletedAt: String? = null,
    val updatedAt: String? = null
)

data class CloudCategoryCustomization(
    val sourceId: String,
    val hiddenCategories: Set<String>,
    val categoryOrder: List<String>
)

data class CloudProfilePreferences(
    val preferredAudioLanguage: String,
    val preferredSubtitleLanguage: String,
    val subtitleFallbackEnglish: Boolean,
    val startupDestination: String = "home",
    val epgTimeShiftHours: Int = 0
)

object CloudIdentityHasher {
    private val HEX_CHARS = "0123456789abcdef".toCharArray()
    private val cache = java.util.concurrent.ConcurrentHashMap<String, String>(2048)

    /**
     * Preserves exact raw source item identity without extra prefixes.
     * Raw input is UTF-8 encoded and hashed with SHA-256 to a 64-character lowercase hex string.
     * Uses a fast hex converter and an in-memory cache to eliminate repetitive allocations.
     */
    fun hashItemId(rawSourceItemId: String): String {
        val trimmed = rawSourceItemId.trim()
        val cached = cache[trimmed]
        if (cached != null) return cached

        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(trimmed.toByteArray(Charsets.UTF_8))
        val chars = CharArray(bytes.size * 2)
        var i = 0
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            chars[i++] = HEX_CHARS[v ushr 4]
            chars[i++] = HEX_CHARS[v and 0x0F]
        }
        val result = String(chars)
        if (cache.size < 30000) {
            cache[trimmed] = result
        }
        return result
    }
}

internal class SupabaseSyncApi(
    private val transport: com.fortresstv.player.profile.ProfileTransport = com.fortresstv.player.profile.SupabaseProfileHttpTransport()
) {
    fun fetchProfileLibrary(
        profileId: String,
        profileSessionToken: String,
        accessToken: String,
        sinceUpdatedAt: String? = null,
        offset: Int = 0,
        limit: Int = 100
    ): List<CloudLibraryItem> {
        val headers = mapOf("x-profile-session" to profileSessionToken)
        val query = StringBuilder("/rest/v1/profile_library?profile_id=eq.$profileId")
        if (!sinceUpdatedAt.isNullOrBlank()) {
            query.append("&updated_at=gt.$sinceUpdatedAt")
        }
        query.append("&order=updated_at.asc&offset=$offset&limit=$limit")

        val text = transport.request("GET", query.toString(), null, accessToken, headers)
        val array = JSONArray(text.ifBlank { "[]" })
        val list = mutableListOf<CloudLibraryItem>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            list.add(
                CloudLibraryItem(
                    householdId = obj.getString("household_id"),
                    profileId = obj.getString("profile_id"),
                    sourceId = obj.getString("source_id"),
                    itemKind = obj.getString("item_kind"),
                    itemId = obj.getString("item_id"),
                    isFavourite = obj.optBoolean("is_favourite", false),
                    isWatchlisted = obj.optBoolean("is_watchlisted", false),
                    isWatched = obj.optBoolean("is_watched", false),
                    clientUpdatedAtMs = obj.optLong("client_updated_at_ms", 0L),
                    deletedAt = obj.optString("deleted_at").takeIf { it.isNotBlank() && it != "null" },
                    updatedAt = obj.optString("updated_at").takeIf { it.isNotBlank() && it != "null" }
                )
            )
        }
        return list
    }

    fun fetchAllProfileLibrary(
        profileId: String,
        profileSessionToken: String,
        accessToken: String,
        pageSize: Int = 100
    ): List<CloudLibraryItem> {
        val allItems = mutableListOf<CloudLibraryItem>()
        var offset = 0
        while (true) {
            val page = fetchProfileLibrary(
                profileId = profileId,
                profileSessionToken = profileSessionToken,
                accessToken = accessToken,
                offset = offset,
                limit = pageSize
            )
            allItems.addAll(page)
            if (page.size < pageSize) break
            offset += page.size
        }
        return allItems
    }

    fun upsertProfileLibrary(
        items: List<CloudLibraryItem>,
        profileSessionToken: String,
        accessToken: String
    ) {
        if (items.isEmpty()) return
        val headers = mapOf(
            "x-profile-session" to profileSessionToken,
            "Prefer" to "return=minimal"
        )
        for (item in items) {
            val rpcBody = JSONObject().apply {
                put("p_profile_id", item.profileId)
                put("p_source_id", item.sourceId)
                put("p_item_kind", item.itemKind)
                put("p_item_id", item.itemId)
                put("p_is_favourite", item.isFavourite)
                put("p_is_watchlisted", item.isWatchlisted)
                put("p_is_watched", item.isWatched)
                put("p_client_updated_at_ms", item.clientUpdatedAtMs)
                if (item.deletedAt != null) put("p_deleted_at", item.deletedAt) else put("p_deleted_at", JSONObject.NULL)
            }
            transport.request("POST", "/rest/v1/rpc/fortress_sync_profile_library", rpcBody, accessToken, headers)
        }
    }

    fun fetchProfileWatchProgress(
        profileId: String,
        profileSessionToken: String,
        accessToken: String,
        sinceUpdatedAt: String? = null,
        offset: Int = 0,
        limit: Int = 100
    ): List<CloudWatchProgressItem> {
        val headers = mapOf("x-profile-session" to profileSessionToken)
        val query = StringBuilder("/rest/v1/profile_watch_progress?profile_id=eq.$profileId")
        if (!sinceUpdatedAt.isNullOrBlank()) {
            query.append("&updated_at=gt.$sinceUpdatedAt")
        }
        query.append("&order=updated_at.asc&offset=$offset&limit=$limit")

        val text = transport.request("GET", query.toString(), null, accessToken, headers)
        val array = JSONArray(text.ifBlank { "[]" })
        val list = mutableListOf<CloudWatchProgressItem>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            list.add(
                CloudWatchProgressItem(
                    householdId = obj.getString("household_id"),
                    profileId = obj.getString("profile_id"),
                    sourceId = obj.getString("source_id"),
                    itemKind = obj.getString("item_kind"),
                    itemId = obj.getString("item_id"),
                    positionMs = obj.optLong("position_ms", 0L),
                    durationMs = obj.optLong("duration_ms", 0L),
                    completed = obj.optBoolean("completed", false),
                    clientUpdatedAtMs = obj.optLong("client_updated_at_ms", 0L),
                    deletedAt = obj.optString("deleted_at").takeIf { it.isNotBlank() && it != "null" },
                    updatedAt = obj.optString("updated_at").takeIf { it.isNotBlank() && it != "null" }
                )
            )
        }
        return list
    }

    fun fetchAllProfileWatchProgress(
        profileId: String,
        profileSessionToken: String,
        accessToken: String,
        pageSize: Int = 100
    ): List<CloudWatchProgressItem> {
        val allItems = mutableListOf<CloudWatchProgressItem>()
        var offset = 0
        while (true) {
            val page = fetchProfileWatchProgress(
                profileId = profileId,
                profileSessionToken = profileSessionToken,
                accessToken = accessToken,
                offset = offset,
                limit = pageSize
            )
            allItems.addAll(page)
            if (page.size < pageSize) break
            offset += page.size
        }
        return allItems
    }

    fun upsertProfileWatchProgress(
        items: List<CloudWatchProgressItem>,
        profileSessionToken: String,
        accessToken: String
    ) {
        if (items.isEmpty()) return
        val headers = mapOf(
            "x-profile-session" to profileSessionToken,
            "Prefer" to "return=minimal"
        )
        for (item in items) {
            val rpcBody = JSONObject().apply {
                put("p_profile_id", item.profileId)
                put("p_source_id", item.sourceId)
                put("p_item_kind", item.itemKind)
                put("p_item_id", item.itemId)
                put("p_position_ms", item.positionMs)
                put("p_duration_ms", item.durationMs)
                put("p_completed", item.completed)
                put("p_client_updated_at_ms", item.clientUpdatedAtMs)
                if (item.deletedAt != null) put("p_deleted_at", item.deletedAt) else put("p_deleted_at", JSONObject.NULL)
            }
            transport.request("POST", "/rest/v1/rpc/fortress_sync_profile_watch_progress", rpcBody, accessToken, headers)
        }
    }

    fun saveProfileCategories(
        profileId: String,
        sourceId: String,
        hiddenCategories: Set<String>,
        categoryOrder: List<String>,
        accessToken: String
    ) {
        val rpcBody = JSONObject().apply {
            put("p_profile_id", profileId)
            put("p_source_id", sourceId)
            put("p_hidden_categories", JSONArray(hiddenCategories.toList()))
            put("p_category_order", JSONArray(categoryOrder))
        }
        transport.request("POST", "/rest/v1/rpc/fortress_save_profile_categories", rpcBody, accessToken, null)
    }

    fun getProfileCategories(
        profileId: String,
        accessToken: String
    ): List<CloudCategoryCustomization> {
        val rpcBody = JSONObject().apply {
            put("p_profile_id", profileId)
        }
        return runCatching {
            val text = transport.request("POST", "/rest/v1/rpc/fortress_get_profile_categories", rpcBody, accessToken, null)
            val arr = JSONArray(text)
            val list = mutableListOf<CloudCategoryCustomization>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val sourceId = obj.getString("source_id")
                val hiddenArr = obj.optJSONArray("hidden_categories") ?: JSONArray()
                val hiddenSet = mutableSetOf<String>()
                for (j in 0 until hiddenArr.length()) {
                    hiddenSet.add(hiddenArr.getString(j))
                }
                val orderArr = obj.optJSONArray("category_order") ?: JSONArray()
                val orderList = mutableListOf<String>()
                for (j in 0 until orderArr.length()) {
                    orderList.add(orderArr.getString(j))
                }
                list.add(CloudCategoryCustomization(sourceId, hiddenSet, orderList))
            }
            list
        }.getOrDefault(emptyList())
    }

    fun saveProfilePreferences(
        profileId: String,
        audioLang: String,
        subtitleLang: String,
        subFallback: Boolean,
        startupDestination: String = "home",
        epgTimeShiftHours: Int = 0,
        accessToken: String
    ) {
        val rpcBody = JSONObject().apply {
            put("p_profile_id", profileId)
            put("p_audio_lang", audioLang)
            put("p_subtitle_lang", subtitleLang)
            put("p_sub_fallback", subFallback)
            put("p_startup_destination", startupDestination)
            put("p_epg_time_shift_hours", epgTimeShiftHours)
        }
        transport.request("POST", "/rest/v1/rpc/fortress_save_profile_preferences", rpcBody, accessToken, null)
    }

    fun getProfilePreferences(
        profileId: String,
        accessToken: String
    ): CloudProfilePreferences? {
        val rpcBody = JSONObject().apply {
            put("p_profile_id", profileId)
        }
        return runCatching {
            val text = transport.request("POST", "/rest/v1/rpc/fortress_get_profile_preferences", rpcBody, accessToken, null)
            val obj = JSONObject(text)
            CloudProfilePreferences(
                preferredAudioLanguage = obj.optString("preferred_audio_language", "default"),
                preferredSubtitleLanguage = obj.optString("preferred_subtitle_language", "off"),
                subtitleFallbackEnglish = obj.optBoolean("subtitle_fallback_english", true),
                startupDestination = obj.optString("startup_destination", "home"),
                epgTimeShiftHours = obj.optInt("epg_time_shift_hours", 0)
            )
        }.getOrNull()
    }

    fun saveProfileRecentChannels(
        profileId: String,
        recentChannels: List<String>,
        accessToken: String
    ) {
        val rpcBody = JSONObject().apply {
            put("p_profile_id", profileId)
            put("p_recent_channels", JSONArray(recentChannels.take(20)))
        }
        transport.request("POST", "/rest/v1/rpc/fortress_save_profile_recent_channels", rpcBody, accessToken, null)
    }

    fun clearProfileRecentChannels(
        profileId: String,
        accessToken: String
    ) {
        val rpcBody = JSONObject().apply {
            put("p_profile_id", profileId)
        }
        transport.request("POST", "/rest/v1/rpc/fortress_clear_profile_recent_channels", rpcBody, accessToken, null)
    }

    fun getProfileRecentChannels(
        profileId: String,
        accessToken: String
    ): List<String> {
        val rpcBody = JSONObject().apply {
            put("p_profile_id", profileId)
        }
        return runCatching {
            val text = transport.request("POST", "/rest/v1/rpc/fortress_get_profile_recent_channels", rpcBody, accessToken, null)
            val arr = JSONArray(text)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        }.getOrDefault(emptyList())
    }
}
