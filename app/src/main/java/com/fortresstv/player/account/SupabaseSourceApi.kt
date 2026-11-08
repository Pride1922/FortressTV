package com.fortresstv.player.account

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

data class HouseholdSourceRecord(
    val id: String = UUID.randomUUID().toString(),
    val sourceType: String, // "xtream" or "custom_epg"
    val displayName: String,
    val enabled: Boolean = true,
    val sourceOrder: Int = 0,
    val config: JSONObject,
    val parentSourceId: String? = null,
    val deletedAt: String? = null,
    val updatedAt: String? = null
)

internal class SupabaseSourceApi(
    private val transport: com.fortresstv.player.profile.ProfileTransport = com.fortresstv.player.profile.SupabaseProfileHttpTransport()
) {
    fun getHouseholdSources(unlockToken: String?, accessToken: String): List<HouseholdSourceRecord> {
        val body = JSONObject().apply {
            unlockToken?.let { put("p_unlock_token", it) }
        }
        val text = transport.request("POST", "/rest/v1/rpc/fortress_get_household_sources", body, accessToken, null)
        val array = JSONArray(text.ifBlank { "[]" })
        val list = mutableListOf<HouseholdSourceRecord>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            list.add(
                HouseholdSourceRecord(
                    id = obj.getString("id"),
                    sourceType = obj.getString("source_type"),
                    displayName = obj.getString("display_name"),
                    enabled = obj.optBoolean("enabled", true),
                    sourceOrder = obj.optInt("source_order", 0),
                    config = obj.optJSONObject("config") ?: JSONObject(),
                    parentSourceId = obj.optString("parent_source_id").takeIf { it.isNotBlank() && it != "null" },
                    deletedAt = obj.optString("deleted_at").takeIf { it.isNotBlank() && it != "null" },
                    updatedAt = obj.optString("updated_at").takeIf { it.isNotBlank() && it != "null" }
                )
            )
        }
        return list
    }

    fun saveHouseholdSource(
        source: HouseholdSourceRecord,
        unlockToken: String?,
        accessToken: String
    ): String {
        val body = JSONObject().apply {
            put("p_source_id", source.id)
            put("p_source_type", source.sourceType)
            put("p_display_name", source.displayName.trim())
            put("p_enabled", source.enabled)
            put("p_source_order", source.sourceOrder)
            put("p_config", source.config)
            if (source.parentSourceId != null) put("p_parent_source_id", source.parentSourceId)
            else put("p_parent_source_id", JSONObject.NULL)
            unlockToken?.let { put("p_unlock_token", it) }
        }
        val text = transport.request("POST", "/rest/v1/rpc/fortress_save_household_source", body, accessToken, null)
        return text.trim().removeSurrounding("\"")
    }

    fun deleteHouseholdSource(
        sourceId: String,
        unlockToken: String?,
        accessToken: String
    ): Boolean {
        val body = JSONObject().apply {
            put("p_source_id", sourceId)
            unlockToken?.let { put("p_unlock_token", it) }
        }
        val text = transport.request("POST", "/rest/v1/rpc/fortress_delete_household_source", body, accessToken, null)
        return text.trim() == "true"
    }

    fun saveTmdb(
        settings: com.fortresstv.player.tmdb.TmdbSettings,
        unlockToken: String?,
        accessToken: String
    ): Boolean {
        val body = JSONObject().apply {
            put("p_enabled", settings.enabled)
            put("p_api_key", settings.apiKey.trim())
            put("p_language", settings.language.ifBlank { "pt-PT" })
            unlockToken?.let { put("p_unlock_token", it) }
        }
        val text = transport.request("POST", "/rest/v1/rpc/fortress_save_tmdb", body, accessToken, null)
        val json = JSONObject(text.ifBlank { "{}" })
        return json.optBoolean("success", false)
    }

    fun getTmdb(
        unlockToken: String?,
        accessToken: String
    ): com.fortresstv.player.tmdb.TmdbSettings? {
        val body = JSONObject().apply {
            unlockToken?.let { put("p_unlock_token", it) }
        }
        val text = transport.request("POST", "/rest/v1/rpc/fortress_get_tmdb", body, accessToken, null)
        val json = JSONObject(text.ifBlank { "{}" })
        if (!json.optBoolean("configured", false)) return null
        return com.fortresstv.player.tmdb.TmdbSettings(
            enabled = json.optBoolean("enabled", false),
            apiKey = json.optString("api_key", ""),
            language = json.optString("language", "pt-PT")
        )
    }

    fun deleteTmdb(
        unlockToken: String?,
        accessToken: String
    ): Boolean {
        val body = JSONObject().apply {
            unlockToken?.let { put("p_unlock_token", it) }
        }
        val text = transport.request("POST", "/rest/v1/rpc/fortress_delete_tmdb", body, accessToken, null)
        return text.trim() == "true"
    }

    fun saveOpenSubtitles(
        settings: com.fortresstv.player.subtitles.OpenSubtitlesSettings,
        unlockToken: String?,
        accessToken: String
    ): Boolean {
        val body = JSONObject().apply {
            put("p_enabled", settings.enabled)
            put("p_api_key", settings.apiKey.trim())
            put("p_username", settings.username.trim())
            put("p_password", settings.password)
            put("p_prefer_pt_pt", settings.preferPtPt)
            put("p_auto_search", settings.autoSearch)
            unlockToken?.let { put("p_unlock_token", it) }
        }
        val text = transport.request("POST", "/rest/v1/rpc/fortress_save_opensubtitles", body, accessToken, null)
        val json = JSONObject(text.ifBlank { "{}" })
        return json.optBoolean("success", false)
    }

    fun getOpenSubtitles(
        unlockToken: String?,
        accessToken: String
    ): com.fortresstv.player.subtitles.OpenSubtitlesSettings? {
        val body = JSONObject().apply {
            unlockToken?.let { put("p_unlock_token", it) }
        }
        val text = transport.request("POST", "/rest/v1/rpc/fortress_get_opensubtitles", body, accessToken, null)
        val json = JSONObject(text.ifBlank { "{}" })
        if (!json.optBoolean("configured", false)) return null
        return com.fortresstv.player.subtitles.OpenSubtitlesSettings(
            enabled = json.optBoolean("enabled", false),
            apiKey = json.optString("api_key", ""),
            username = json.optString("username", ""),
            password = json.optString("password", ""),
            preferPtPt = json.optBoolean("prefer_pt_pt", true),
            autoSearch = json.optBoolean("auto_search", true)
        )
    }

    fun deleteOpenSubtitles(
        unlockToken: String?,
        accessToken: String
    ): Boolean {
        val body = JSONObject().apply {
            unlockToken?.let { put("p_unlock_token", it) }
        }
        val text = transport.request("POST", "/rest/v1/rpc/fortress_delete_opensubtitles", body, accessToken, null)
        return text.trim() == "true"
    }

    fun saveEpgMapping(
        mapping: com.fortresstv.player.epg.EpgChannelMapping,
        unlockToken: String?,
        accessToken: String
    ): Boolean {
        val body = JSONObject().apply {
            put("p_iptv_source_id", mapping.iptvSourceId)
            put("p_iptv_channel_id", mapping.iptvChannelId.trim())
            mapping.epgSourceId?.let { put("p_epg_source_id", it) }
            put("p_xmltv_channel_id", mapping.xmltvChannelId.trim())
            unlockToken?.let { put("p_unlock_token", it) }
        }
        val text = transport.request("POST", "/rest/v1/rpc/fortress_save_epg_mapping", body, accessToken, null)
        val json = JSONObject(text.ifBlank { "{}" })
        return json.optBoolean("success", false)
    }

    fun deleteEpgMapping(
        iptvSourceId: String,
        iptvChannelId: String,
        unlockToken: String?,
        accessToken: String
    ): Boolean {
        val body = JSONObject().apply {
            put("p_iptv_source_id", iptvSourceId)
            put("p_iptv_channel_id", iptvChannelId.trim())
            unlockToken?.let { put("p_unlock_token", it) }
        }
        val text = transport.request("POST", "/rest/v1/rpc/fortress_delete_epg_mapping", body, accessToken, null)
        return text.trim() == "true"
    }

    fun getEpgMappings(
        accessToken: String
    ): List<com.fortresstv.player.epg.EpgChannelMapping> {
        val text = transport.request("POST", "/rest/v1/rpc/fortress_get_epg_mappings", JSONObject(), accessToken, null)
        val array = org.json.JSONArray(text.ifBlank { "[]" })
        val list = mutableListOf<com.fortresstv.player.epg.EpgChannelMapping>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                com.fortresstv.player.epg.EpgChannelMapping(
                    iptvSourceId = obj.getString("iptv_source_id"),
                    iptvChannelId = obj.getString("iptv_channel_id"),
                    epgSourceId = obj.optString("epg_source_id").takeIf(String::isNotBlank),
                    xmltvChannelId = obj.getString("xmltv_channel_id"),
                    isAvailable = true
                )
            )
        }
        return list
    }

    fun resetAllEpgMappings(
        unlockToken: String?,
        accessToken: String
    ): Boolean {
        val body = JSONObject().apply {
            unlockToken?.let { put("p_unlock_token", it) }
        }
        val text = transport.request("POST", "/rest/v1/rpc/fortress_reset_all_epg_mappings", body, accessToken, null)
        return text.trim() == "true"
    }
}
