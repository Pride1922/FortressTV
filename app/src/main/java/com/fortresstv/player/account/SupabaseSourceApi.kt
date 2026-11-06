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
}
