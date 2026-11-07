package com.fortresstv.player.epg

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class EpgChannelMapping(
    val iptvSourceId: String,
    val iptvChannelId: String,
    val epgSourceId: String?,
    val xmltvChannelId: String,
    val isAvailable: Boolean = true
) {
    fun compositeKey(): String = "${iptvSourceId}:${iptvChannelId}"
}

class EpgMappingStore(
    context: Context? = null,
    private val prefsProvider: ((String) -> SharedPreferences)? = null,
    private val namespace: String = "global"
) {
    private val preferences: SharedPreferences by lazy {
        prefsProvider?.invoke("epg_mappings_$namespace")
            ?: context!!.getSharedPreferences("epg_mappings_$namespace", Context.MODE_PRIVATE)
    }

    @Synchronized
    fun getAllMappings(): Map<String, EpgChannelMapping> {
        val json = preferences.getString("mappings_json", null) ?: return emptyMap()
        return runCatching {
            val array = JSONArray(json)
            val result = mutableMapOf<String, EpgChannelMapping>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val mapping = EpgChannelMapping(
                    iptvSourceId = obj.getString("iptv_source_id"),
                    iptvChannelId = obj.getString("iptv_channel_id"),
                    epgSourceId = obj.optString("epg_source_id").takeIf(String::isNotBlank),
                    xmltvChannelId = obj.getString("xmltv_channel_id"),
                    isAvailable = obj.optBoolean("is_available", true)
                )
                result[mapping.compositeKey()] = mapping
            }
            result
        }.getOrDefault(emptyMap())
    }

    fun getMapping(iptvSourceId: String, iptvChannelId: String): EpgChannelMapping? {
        val key = "${iptvSourceId}:${iptvChannelId}"
        return getAllMappings()[key]
    }

    @Synchronized
    fun saveMapping(mapping: EpgChannelMapping) {
        val current = getAllMappings().toMutableMap()
        current[mapping.compositeKey()] = mapping
        persist(current.values.toList())
    }

    @Synchronized
    fun removeMapping(iptvSourceId: String, iptvChannelId: String): Boolean {
        val current = getAllMappings().toMutableMap()
        val key = "${iptvSourceId}:${iptvChannelId}"
        val removed = current.remove(key) != null
        if (removed) persist(current.values.toList())
        return removed
    }

    @Synchronized
    fun replaceAllMappings(mappings: List<EpgChannelMapping>) {
        persist(mappings)
    }

    @Synchronized
    fun clearAllMappings() {
        preferences.edit().remove("mappings_json").apply()
    }

    private fun persist(list: List<EpgChannelMapping>) {
        val array = JSONArray()
        for (m in list) {
            val obj = JSONObject().apply {
                put("iptv_source_id", m.iptvSourceId)
                put("iptv_channel_id", m.iptvChannelId)
                put("epg_source_id", m.epgSourceId.orEmpty())
                put("xmltv_channel_id", m.xmltvChannelId)
                put("is_available", m.isAvailable)
            }
            array.put(obj)
        }
        preferences.edit().putString("mappings_json", array.toString()).apply()
    }
}
