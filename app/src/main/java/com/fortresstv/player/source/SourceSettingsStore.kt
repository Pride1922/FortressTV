package com.fortresstv.player.source

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.fortresstv.player.playlist.PlaylistProfile
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SourceSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("fortress_sources", Context.MODE_PRIVATE)

    fun getPlaylists(): List<PlaylistProfile> {
        val raw = preferences.getString("playlist_profiles_json", null)?.let(::decrypt)
        if (raw.isNullOrBlank()) {
            val legacy = readSource("playlist")
            if (legacy != null) {
                val profile = PlaylistProfile(
                    id = "default_playlist",
                    name = legacy.displayName.ifBlank { "My Playlist" },
                    source = legacy,
                    lastUpdated = lastLegacyPlaylistUpdate(),
                    channelCount = 0
                )
                val list = listOf(profile)
                savePlaylistsList(list)
                setActivePlaylistId(profile.id)
                return list
            }
            return emptyList()
        }

        return parsePlaylistsJson(raw)
    }

    private fun parsePlaylistsJson(json: String): List<PlaylistProfile> = runCatching {
        val array = JSONArray(json)
        val list = mutableListOf<PlaylistProfile>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val id = obj.getString("id")
            val name = obj.getString("name")
            val type = obj.getString("type")
            val value = obj.getString("value")
            val displayName = obj.optString("displayName", name)
            val lastUpdated = obj.optLong("lastUpdated", 0L)
            val channelCount = obj.optInt("channelCount", 0)
            val source = if (type == "file") ContentSource.LocalFile(value, displayName) else ContentSource.RemoteUrl(value, displayName)
            list.add(PlaylistProfile(id, name, source, lastUpdated, channelCount))
        }
        list
    }.getOrElse { emptyList() }

    fun savePlaylistProfile(profile: PlaylistProfile) {
        val current = getPlaylists().toMutableList()
        val index = current.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            current[index] = profile
        } else {
            current.add(profile)
        }
        savePlaylistsList(current)
        if (getActivePlaylistId() == null) {
            setActivePlaylistId(profile.id)
        }
    }

    fun deletePlaylistProfile(id: String) {
        val current = getPlaylists().filter { it.id != id }
        savePlaylistsList(current)
        if (getActivePlaylistId() == id) {
            val nextActive = current.firstOrNull()?.id
            if (nextActive != null) setActivePlaylistId(nextActive)
            else preferences.edit().remove("active_playlist_id").apply()
        }
    }

    private fun savePlaylistsList(list: List<PlaylistProfile>) {
        val array = JSONArray()
        for (p in list) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("type", if (p.source is ContentSource.LocalFile) "file" else "url")
                put("value", p.source.value)
                put("displayName", p.source.displayName)
                put("lastUpdated", p.lastUpdated)
                put("channelCount", p.channelCount)
            }
            array.put(obj)
        }
        preferences.edit().putString("playlist_profiles_json", encrypt(array.toString())).apply()
    }

    fun getActivePlaylistId(): String? = preferences.getString("active_playlist_id", null)

    fun setActivePlaylistId(id: String) {
        preferences.edit().putString("active_playlist_id", id).apply()
    }

    fun getActivePlaylist(): PlaylistProfile? {
        val playlists = getPlaylists()
        val activeId = getActivePlaylistId()
        return playlists.find { it.id == activeId } ?: playlists.firstOrNull()
    }

    fun epgSource(): ContentSource? = readSource("epg")

    fun saveEpg(source: ContentSource) = saveSource("epg", source)

    fun clearEpg() {
        clearSource("epg")
        preferences.edit().remove("epg_last_update").apply()
    }

    fun lastEpgUpdate(): Long = preferences.getLong("epg_last_update", 0L)

    fun setLastEpgUpdate(time: Long) {
        preferences.edit().putLong("epg_last_update", time).apply()
    }

    fun epgTimeShiftHours(): Int = preferences.getInt("epg_time_shift_hours", 0)

    fun saveEpgTimeShiftHours(hours: Int) {
        preferences.edit().putInt("epg_time_shift_hours", hours).apply()
    }

    fun getLanguage(): String {
        val saved = preferences.getString("app_language", null)
        if (saved != null) return saved
        val systemLang = java.util.Locale.getDefault().language.lowercase()
        return if (systemLang == "pt") "pt" else "en"
    }

    fun setLanguage(lang: String) {
        preferences.edit().putString("app_language", lang).apply()
    }

    fun playChannelsFullscreen(): Boolean =
        preferences.getBoolean("play_channels_fullscreen", false)

    fun setPlayChannelsFullscreen(enabled: Boolean) {
        preferences.edit().putBoolean("play_channels_fullscreen", enabled).apply()
    }

    private fun lastLegacyPlaylistUpdate(): Long = preferences.getLong("playlist_last_update", 0L)

    private fun saveSource(prefix: String, source: ContentSource) {
        preferences.edit()
            .putString("${prefix}_type", if (source is ContentSource.LocalFile) "file" else "url")
            .putString("${prefix}_value", encrypt(source.value))
            .putString("${prefix}_name", encrypt(source.displayName))
            .apply()
    }

    private fun readSource(prefix: String): ContentSource? {
        val type = preferences.getString("${prefix}_type", null) ?: return null
        val value = preferences.getString("${prefix}_value", null)?.let(::decrypt) ?: return null
        val name = preferences.getString("${prefix}_name", null)?.let(::decrypt) ?: return null
        return when (type) {
            "file" -> ContentSource.LocalFile(value, name)
            "url" -> ContentSource.RemoteUrl(value, name)
            else -> null
        }
    }

    private fun clearSource(prefix: String) {
        preferences.edit()
            .remove("${prefix}_type")
            .remove("${prefix}_value")
            .remove("${prefix}_name")
            .apply()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String? = runCatching {
        val payload = Base64.decode(value, Base64.NO_WRAP)
        val iv = payload.copyOfRange(0, IV_SIZE)
        val encrypted = payload.copyOfRange(IV_SIZE, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }.getOrNull()

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            generateKey()
        }
    }

    private companion object {
        const val KEY_ALIAS = "fortress_source_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
    }
}
