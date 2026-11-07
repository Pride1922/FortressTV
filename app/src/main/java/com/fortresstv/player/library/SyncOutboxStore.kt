package com.fortresstv.player.library

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

enum class SyncActionType {
    UPSERT_LIBRARY,
    UPSERT_PROGRESS,
    UPSERT_CATEGORIES,
    UPSERT_PREFERENCES,
    UPSERT_RECENT_CHANNELS
}

data class PendingSyncMutation(
    val id: String = java.util.UUID.randomUUID().toString(),
    val actionType: SyncActionType,
    val profileId: String,
    val sourceId: String,
    val itemKind: String,
    val itemId: String, // 64-char hex sha256 of raw source local ID
    val payload: JSONObject,
    val clientUpdatedAtMs: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("action_type", actionType.name)
        put("profile_id", profileId)
        put("source_id", sourceId)
        put("item_kind", itemKind)
        put("item_id", itemId)
        put("payload", payload)
        put("client_updated_at_ms", clientUpdatedAtMs)
    }

    companion object {
        fun fromJson(json: JSONObject): PendingSyncMutation? = runCatching {
            PendingSyncMutation(
                id = json.getString("id"),
                actionType = SyncActionType.valueOf(json.getString("action_type")),
                profileId = json.getString("profile_id"),
                sourceId = json.getString("source_id"),
                itemKind = json.getString("item_kind"),
                itemId = json.getString("item_id"),
                payload = json.getJSONObject("payload"),
                clientUpdatedAtMs = json.optLong("client_updated_at_ms", System.currentTimeMillis())
            )
        }.getOrNull()
    }
}

class SyncOutboxStore internal constructor(
    private val prefsProvider: (String) -> SharedPreferences,
    namespace: String = "local"
) {
    constructor(context: Context, namespace: String = "local") : this(
        prefsProvider = { ns ->
            val sanitized = ns.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            context.getSharedPreferences("fortress_sync_outbox_$sanitized", Context.MODE_PRIVATE)
        },
        namespace = namespace
    )

    private val preferences = prefsProvider(namespace)
    private val mutex = Mutex()

    suspend fun enqueue(mutation: PendingSyncMutation) = mutex.withLock {
        val list = loadAllInternal().toMutableList()
        // Deduplicate mutations on the same item key
        list.removeAll {
            it.profileId == mutation.profileId &&
                it.sourceId == mutation.sourceId &&
                it.itemKind == mutation.itemKind &&
                it.itemId == mutation.itemId &&
                it.actionType == mutation.actionType
        }
        list.add(mutation)
        saveAllInternal(list)
    }

    suspend fun loadPending(): List<PendingSyncMutation> = mutex.withLock {
        loadAllInternal()
    }

    suspend fun remove(mutationId: String) = mutex.withLock {
        val list = loadAllInternal().filterNot { it.id == mutationId }
        saveAllInternal(list)
    }

    suspend fun clear() = mutex.withLock {
        preferences.edit().remove("mutations").apply()
    }

    private fun loadAllInternal(): List<PendingSyncMutation> {
        val raw = preferences.getString("mutations", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                array.optJSONObject(i)?.let { PendingSyncMutation.fromJson(it) }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveAllInternal(list: List<PendingSyncMutation>) {
        val array = JSONArray()
        list.forEach { array.put(it.toJson()) }
        preferences.edit().putString("mutations", array.toString()).apply()
    }
}
