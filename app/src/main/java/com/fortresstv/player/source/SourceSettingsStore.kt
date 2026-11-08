package com.fortresstv.player.source

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.fortresstv.player.playlist.PlaylistProfile
import com.fortresstv.player.playlist.SourceRole
import com.fortresstv.player.player.PlaybackMode
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

enum class StartupDestination(val persistedValue: String) {
    HOME("home"),
    LIVE_TV("live_tv");

    companion object {
        fun fromPersisted(value: String?): StartupDestination = when (value) {
            "live_tv" -> LIVE_TV
            else -> HOME
        }
    }
}

class SourceSettingsStore internal constructor(
    private val preferences: SharedPreferences
) {
    constructor(context: Context) : this(context.getSharedPreferences("fortress_sources", Context.MODE_PRIVATE))

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

        val profiles = parsePlaylistsJson(raw)
        if (!raw.contains("\"enabled\"") || !raw.contains("\"role\"") || !raw.contains("\"order\"")) {
            savePlaylistsList(profiles)
        }
        return profiles
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
            val source = when (type) {
                "file" -> ContentSource.LocalFile(value, displayName)
                "xtream" -> ContentSource.Xtream(
                    value = value,
                    displayName = displayName,
                    username = obj.optString("username"),
                    password = obj.optString("password")
                )
                else -> ContentSource.RemoteUrl(value, displayName)
            }
            val activeId = getActivePlaylistId()
            val enabled = if (obj.has("enabled")) obj.optBoolean("enabled", true)
            else activeId == null || activeId == id
            val role = runCatching {
                SourceRole.valueOf(obj.optString("role", SourceRole.LIVE_TV.name))
            }.getOrDefault(SourceRole.LIVE_TV)
            val order = obj.optInt("order", i)
            list.add(PlaylistProfile(id, name, source, lastUpdated, channelCount, enabled, role, order))
        }
        list
    }.getOrElse { emptyList() }

    @Synchronized
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

    fun replacePlaylists(profiles: List<PlaylistProfile>) {
        savePlaylistsList(profiles)
        clearSource("playlist")
        preferences.edit().remove("active_playlist_id").apply()
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
                put("type", when (p.source) {
                    is ContentSource.LocalFile -> "file"
                    is ContentSource.RemoteUrl -> "url"
                    is ContentSource.Xtream -> "xtream"
                })
                put("value", p.source.value)
                put("displayName", p.source.displayName)
                put("lastUpdated", p.lastUpdated)
                put("channelCount", p.channelCount)
                put("enabled", p.enabled)
                put("role", p.role.name)
                put("order", p.order)
                if (p.source is ContentSource.Xtream) {
                    put("username", p.source.username)
                    put("password", p.source.password)
                }
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

    fun getEnabledPlaylists(): List<PlaylistProfile> = getPlaylists()
        .filter { it.enabled }
        .sortedWith(compareBy<PlaylistProfile> { it.order }.thenBy { it.name.lowercase() })

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

    fun epgTimeShiftHours(profileId: String? = null): Int {
        if (!profileId.isNullOrBlank() && preferences.contains("epg_time_shift_hours_$profileId")) {
            return preferences.getInt("epg_time_shift_hours_$profileId", 0)
        }
        return preferences.getInt("epg_time_shift_hours", 0)
    }

    fun saveEpgTimeShiftHours(hours: Int, profileId: String? = null) {
        val editor = preferences.edit()
            .putInt("epg_time_shift_hours", hours)
            .putInt("epg_correction_minutes", hours * 60)
        if (!profileId.isNullOrBlank()) {
            editor.putInt("epg_time_shift_hours_$profileId", hours)
            editor.putInt("epg_correction_minutes_$profileId", hours * 60)
        }
        editor.apply()
    }

    fun epgScheduleCorrectionMinutes(profileId: String? = null): Int {
        if (!profileId.isNullOrBlank() && preferences.contains("epg_correction_minutes_$profileId")) {
            return preferences.getInt("epg_correction_minutes_$profileId", 0)
        }
        return preferences.getInt("epg_correction_minutes", epgTimeShiftHours(profileId) * 60)
    }

    fun saveEpgScheduleCorrectionMinutes(minutes: Int, profileId: String? = null) {
        val editor = preferences.edit()
            .putInt("epg_correction_minutes", minutes)
            .putInt("epg_time_shift_hours", minutes / 60)
        if (!profileId.isNullOrBlank()) {
            editor.putInt("epg_correction_minutes_$profileId", minutes)
            editor.putInt("epg_time_shift_hours_$profileId", minutes / 60)
        }
        editor.apply()
    }

    fun epgSourceTimezone(): String? = preferences.getString("epg_source_timezone", null)

    fun saveEpgSourceTimezone(timezoneId: String?): Boolean {
        val current = epgSourceTimezone()
        if (current == timezoneId) return false
        preferences.edit().apply {
            if (timezoneId.isNullOrBlank()) remove("epg_source_timezone")
            else putString("epg_source_timezone", timezoneId)
            // Invalidate cached EPG so it re-parses with the new timezone
            remove("epg_last_update")
        }.apply()
        return true
    }

    fun epgLoadedDays(): Int = preferences.getInt("epg_loaded_days", 3).coerceIn(1, 7)

    fun saveEpgLoadedDays(days: Int) {
        preferences.edit().putInt("epg_loaded_days", days.coerceIn(1, 7)).apply()
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

    fun telemetryEnabled(): Boolean =
        preferences.getBoolean("telemetry_enabled", true)

    fun setTelemetryEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("telemetry_enabled", enabled).apply()
    }

    fun startupDestination(profileId: String? = null): StartupDestination {
        if (!profileId.isNullOrBlank()) {
            val v = preferences.getString("startup_destination_$profileId", null)
            if (v != null) return StartupDestination.fromPersisted(v)
        }
        return StartupDestination.fromPersisted(preferences.getString("startup_destination", "home"))
    }

    fun setStartupDestination(destination: StartupDestination, profileId: String? = null) {
        val editor = preferences.edit().putString("startup_destination", destination.persistedValue)
        if (!profileId.isNullOrBlank()) {
            editor.putString("startup_destination_$profileId", destination.persistedValue)
        }
        editor.apply()
    }

    fun autoPictureInPicture(): Boolean =
        preferences.getBoolean("auto_picture_in_picture", true)

    fun setAutoPictureInPicture(enabled: Boolean) {
        preferences.edit().putBoolean("auto_picture_in_picture", enabled).apply()
    }

    fun playbackMode(): PlaybackMode =
        PlaybackMode.fromPersisted(preferences.getString("playback_mode", null))

    fun setPlaybackMode(mode: PlaybackMode) {
        preferences.edit().putString("playback_mode", mode.persistedValue).apply()
    }

    fun preferredAudioLanguage(profileId: String? = null): String {
        if (!profileId.isNullOrBlank()) {
            val v = preferences.getString("preferred_audio_language_$profileId", null)
            if (v != null) return v
        }
        return preferences.getString("preferred_audio_language", "default") ?: "default"
    }

    fun setPreferredAudioLanguage(lang: String, profileId: String? = null) {
        val editor = preferences.edit().putString("preferred_audio_language", lang)
        if (!profileId.isNullOrBlank()) {
            editor.putString("preferred_audio_language_$profileId", lang)
        }
        editor.apply()
    }

    fun preferredSubtitleLanguage(profileId: String? = null): String {
        if (!profileId.isNullOrBlank()) {
            val v = preferences.getString("preferred_subtitle_language_$profileId", null)
            if (v != null) return v
        }
        return preferences.getString("preferred_subtitle_language", "off") ?: "off"
    }

    fun setPreferredSubtitleLanguage(lang: String, profileId: String? = null) {
        val editor = preferences.edit().putString("preferred_subtitle_language", lang)
        if (!profileId.isNullOrBlank()) {
            editor.putString("preferred_subtitle_language_$profileId", lang)
        }
        editor.apply()
    }

    fun subtitleFallbackEnglish(profileId: String? = null): Boolean {
        if (!profileId.isNullOrBlank() && preferences.contains("subtitle_fallback_english_$profileId")) {
            return preferences.getBoolean("subtitle_fallback_english_$profileId", true)
        }
        return preferences.getBoolean("subtitle_fallback_english", true)
    }

    fun setSubtitleFallbackEnglish(enabled: Boolean, profileId: String? = null) {
        val editor = preferences.edit().putBoolean("subtitle_fallback_english", enabled)
        if (!profileId.isNullOrBlank()) {
            editor.putBoolean("subtitle_fallback_english_$profileId", enabled)
        }
        editor.apply()
    }

    fun hideAdultContent(): Boolean =
        preferences.getBoolean("hide_adult_content", false)

    fun setHideAdultContent(enabled: Boolean) {
        preferences.edit().putBoolean("hide_adult_content", enabled).apply()
    }

    fun getParentalPin(): String? =
        preferences.getString("parental_pin", null)?.let(::decrypt)

    fun setParentalPin(pin: String?) {
        if (pin.isNullOrBlank()) {
            preferences.edit().remove("parental_pin").apply()
        } else {
            preferences.edit().putString("parental_pin", encrypt(pin)).apply()
        }
    }

    fun isParentalPinSet(): Boolean = !getParentalPin().isNullOrBlank()

    fun verifyParentalPin(pin: String): Boolean = getParentalPin() == pin

    fun parentalControlEnabled(): Boolean =
        preferences.getBoolean("parental_control_enabled", false)

    fun setParentalControlEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("parental_control_enabled", enabled).apply()
    }

    fun getRecentChannelIds(): List<String> {
        val raw = preferences.getString("recent_channel_ids", null) ?: return emptyList()
        return raw.split(",").map(String::trim).filter(String::isNotEmpty)
    }

    fun addRecentChannelId(id: String) {
        if (id.isBlank()) return
        val current = getRecentChannelIds().toMutableList()
        current.remove(id)
        current.add(0, id)
        val trimmed = current.take(15)
        preferences.edit().putString("recent_channel_ids", trimmed.joinToString(",")).apply()
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

    fun hasSeenCloudOnboardingPrompt(): Boolean =
        preferences.getBoolean("seen_cloud_onboarding_v2", false)

    fun setSeenCloudOnboardingPrompt(seen: Boolean) {
        preferences.edit().putBoolean("seen_cloud_onboarding_v2", seen).apply()
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
