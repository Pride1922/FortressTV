package com.fortresstv.player.profile

import com.fortresstv.player.account.AccountConfiguration
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

internal fun interface ProfileTransport {
    fun request(
        method: String,
        path: String,
        body: JSONObject?,
        accessToken: String?,
        headers: Map<String, String>?
    ): String
}

object ServerClockTracker {
    @Volatile
    var serverTimeOffsetMs: Long = 0L
        private set

    fun updateFromDateHeader(dateHeader: String?) {
        if (dateHeader.isNullOrBlank()) return
        runCatching {
            val serverInstant = java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.parse(dateHeader, java.time.Instant::from)
            serverTimeOffsetMs = serverInstant.toEpochMilli() - System.currentTimeMillis()
        }
    }

    fun currentEstimatedServerTimeMs(): Long = System.currentTimeMillis() + serverTimeOffsetMs
}

internal class SupabaseProfileHttpTransport : ProfileTransport {
    override fun request(
        method: String,
        path: String,
        body: JSONObject?,
        accessToken: String?,
        headers: Map<String, String>?
    ): String {
        val url = URI(AccountConfiguration.URL + path).toURL()
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 12_000
            connection.readTimeout = 12_000
            connection.setRequestProperty("apikey", AccountConfiguration.PUBLISHABLE_KEY)
            connection.setRequestProperty("Accept", "application/json")
            accessToken?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            headers?.forEach { (key, value) -> connection.setRequestProperty(key, value) }

            if (body != null) {
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }

            val status = connection.responseCode
            val dateHeader = connection.getHeaderField("Date")
            ServerClockTracker.updateFromDateHeader(dateHeader)

            if (status !in 200..299) {
                val errBody = runCatching {
                    connection.errorStream?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
                throw IOException("HTTP $status: $errBody")
            }

            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}

internal class SupabaseProfileApi(
    private val transport: ProfileTransport = SupabaseProfileHttpTransport()
) {
    fun getProfiles(accessToken: String): List<HouseholdProfile> {
        val text = transport.request("GET", "/rest/v1/household_profiles?select=*&order=created_at.asc", null, accessToken, null)
        val array = JSONArray(text.ifBlank { "[]" })
        val list = mutableListOf<HouseholdProfile>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            list.add(HouseholdProfile.decode(obj))
        }
        return list
    }

    fun getPinStatus(accessToken: String): PinStatus {
        val text = try {
            transport.request("POST", "/rest/v1/rpc/fortress_get_pin_status", JSONObject(), accessToken, null)
        } catch (_: Exception) {
            transport.request("POST", "/rest/v1/rpc/fortress_get_parental_pin_status", JSONObject(), accessToken, null)
        }
        val json = JSONObject(text.ifBlank { "{}" })
        val hasPin = json.optBoolean("has_pin", false)
        return PinStatus(
            configurationState = if (hasPin) PinConfigurationState.CONFIRMED_HAS_PIN else PinConfigurationState.CONFIRMED_NO_PIN,
            hasPin = hasPin,
            isLocked = json.optBoolean("is_locked", false),
            remainingAttempts = json.optInt("remaining_attempts", 5),
            lockoutSeconds = json.optInt("lockout_seconds", json.optInt("lock_seconds", 0))
        )
    }

    fun verifyPin(pin: String, accessToken: String): PinVerifyResult {
        val body = JSONObject().put("p_pin", pin.trim())
        val text = transport.request("POST", "/rest/v1/rpc/fortress_verify_parental_pin", body, accessToken, null)
        val json = JSONObject(text.ifBlank { "{}" })
        val verified = json.optBoolean("verified", false)
        val locked = json.optBoolean("locked", false)
        val remaining = json.optInt("remaining_attempts", 5)
        val lockSeconds = json.optInt("lockout_seconds", json.optInt("lock_seconds", 0))
        val unlockToken = json.optString("unlock_token").takeIf(String::isNotBlank)

        return when {
            verified -> PinVerifyResult.Success(unlockToken)
            locked -> PinVerifyResult.Locked(lockSeconds.coerceAtLeast(1))
            else -> PinVerifyResult.Failed(remaining)
        }
    }

    fun setPin(newPin: String, unlockToken: String?, accessToken: String): Boolean {
        val body = JSONObject().apply {
            put("p_new_pin", newPin.trim())
            unlockToken?.let { put("p_unlock_token", it) }
        }
        val text = transport.request("POST", "/rest/v1/rpc/fortress_set_parental_pin", body, accessToken, null)
        return text.trim() == "true"
    }

    fun removePin(unlockToken: String?, accessToken: String): Boolean {
        val body = JSONObject().apply {
            unlockToken?.let { put("p_unlock_token", it) }
        }
        val text = transport.request("POST", "/rest/v1/rpc/fortress_remove_parental_pin", body, accessToken, null)
        return text.trim() == "true"
    }

    fun resetPinAfterRecovery(newPin: String, accessToken: String): Boolean {
        val body = JSONObject().put("p_new_pin", newPin.trim())
        val text = transport.request("POST", "/rest/v1/rpc/fortress_reset_parental_pin_after_recovery", body, accessToken, null)
        return text.trim() == "true"
    }

    fun createProfile(
        name: String,
        kind: ProfileKind,
        language: String,
        hideAdultContent: Boolean,
        maximumAge: Int?,
        blockUnrated: Boolean,
        unlockToken: String?,
        accessToken: String,
        avatarUrl: String? = null
    ): String {
        val body = JSONObject().apply {
            put("p_name", name.trim())
            put("p_kind", kind.persistedValue)
            put("p_language", language)
            put("p_hide_adult_content", hideAdultContent)
            if (maximumAge != null) put("p_maximum_age", maximumAge) else put("p_maximum_age", JSONObject.NULL)
            put("p_block_unrated", blockUnrated)
            if (avatarUrl != null) put("p_avatar_url", avatarUrl) else put("p_avatar_url", JSONObject.NULL)
            unlockToken?.let { put("p_unlock_token", it) }
        }
        val text = transport.request("POST", "/rest/v1/rpc/fortress_create_profile", body, accessToken, null)
        return text.trim().removeSurrounding("\"")
    }

    fun updateProfile(
        profile: HouseholdProfile,
        unlockToken: String?,
        accessToken: String
    ): Boolean {
        val body = JSONObject().apply {
            put("p_profile_id", profile.id)
            put("p_name", profile.name.trim())
            put("p_kind", profile.kind.persistedValue)
            put("p_language", profile.language)
            put("p_hide_adult_content", profile.hideAdultContent)
            if (profile.maximumAge != null) put("p_maximum_age", profile.maximumAge) else put("p_maximum_age", JSONObject.NULL)
            put("p_block_unrated", profile.blockUnrated)
            if (profile.avatarUrl != null) put("p_avatar_url", profile.avatarUrl) else put("p_avatar_url", JSONObject.NULL)
            unlockToken?.let { put("p_unlock_token", it) }
        }
        val text = transport.request("POST", "/rest/v1/rpc/fortress_update_profile", body, accessToken, null)
        return text.trim() == "true"
    }

    fun setProfileLanguage(
        profileId: String,
        language: String,
        accessToken: String
    ): Boolean {
        val body = JSONObject().apply {
            put("p_profile_id", profileId)
            put("p_language", language)
        }
        val text = transport.request("POST", "/rest/v1/rpc/fortress_set_profile_language", body, accessToken, null)
        return text.trim() == "true"
    }

    fun setProfileHideAdultContent(
        profileId: String,
        hideAdultContent: Boolean,
        unlockToken: String?,
        accessToken: String
    ): Boolean {
        val body = JSONObject().apply {
            put("p_profile_id", profileId)
            put("p_hide_adult_content", hideAdultContent)
            unlockToken?.let { put("p_unlock_token", it) }
        }
        val text = transport.request("POST", "/rest/v1/rpc/fortress_set_profile_hide_adult_content", body, accessToken, null)
        return text.trim() == "true"
    }

    fun deleteProfile(
        profileId: String,
        unlockToken: String?,
        accessToken: String
    ): Boolean {
        val body = JSONObject().apply {
            put("p_profile_id", profileId)
            unlockToken?.let { put("p_unlock_token", it) }
        }
        val text = transport.request("POST", "/rest/v1/rpc/fortress_delete_profile", body, accessToken, null)
        return text.trim() == "true"
    }

    fun openProfileSession(
        profileId: String,
        unlockToken: String?,
        accessToken: String
    ): JSONObject {
        val body = JSONObject().apply {
            put("p_profile_id", profileId)
            unlockToken?.let { put("p_unlock_token", it) }
        }
        val text = transport.request("POST", "/rest/v1/rpc/fortress_open_profile_session", body, accessToken, null)
        return JSONObject(text.ifBlank { "{}" })
    }

    fun leaveAdultMode(accessToken: String, targetAuthSessionId: String? = null): Boolean {
        val body = JSONObject().apply {
            targetAuthSessionId?.let { put("p_target_auth_session_id", it) }
        }
        val text = transport.request("POST", "/rest/v1/rpc/fortress_leave_adult_mode", body, accessToken, null)
        return text.trim() == "true"
    }
}
