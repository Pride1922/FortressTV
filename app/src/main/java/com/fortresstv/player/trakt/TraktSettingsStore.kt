package com.fortresstv.player.trakt

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.util.Base64
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class TraktSettingsStore(
    context: Context,
    val profileNamespace: String,
    private val customPrefs: SharedPreferences? = null
) {
    private val preferences: SharedPreferences = customPrefs ?: context.getSharedPreferences(
        "fortress_trakt_${sanitizeNamespace(profileNamespace)}",
        Context.MODE_PRIVATE
    )

    private val refreshMutex = Mutex()

    fun isConnected(): Boolean {
        val token = loadToken()
        return token != null && token.accessToken.isNotBlank() && token.refreshToken.isNotBlank()
    }

    fun loadClientId(): String {
        return preferences.getString(KEY_CLIENT_ID, null)?.let(::decrypt).orEmpty()
    }

    fun saveClientId(clientId: String) {
        if (clientId.isBlank()) {
            preferences.edit().remove(KEY_CLIENT_ID).apply()
        } else {
            preferences.edit().putString(KEY_CLIENT_ID, encrypt(clientId.trim())).apply()
        }
    }

    fun observeClientId(onChanged: (String) -> Unit): () -> Unit {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_CLIENT_ID) onChanged(loadClientId())
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onChanged(loadClientId())
        return { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun isScrobbleEnabled(): Boolean {
        return preferences.getBoolean(KEY_SCROBBLE_ENABLED, true)
    }

    fun setScrobbleEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SCROBBLE_ENABLED, enabled).apply()
    }

    fun loadToken(): TraktToken? {
        val encryptedAccess = preferences.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val encryptedRefresh = preferences.getString(KEY_REFRESH_TOKEN, null) ?: return null

        val accessToken = decrypt(encryptedAccess) ?: return null
        val refreshToken = decrypt(encryptedRefresh) ?: return null

        val createdAt = preferences.getLong(KEY_CREATED_AT, 0L)
        val expiresAt = preferences.getLong(KEY_EXPIRES_AT, 0L)
        val expiresInSeconds = if (expiresAt > createdAt) (expiresAt - createdAt) / 1000L else 7776000L

        return TraktToken(
            accessToken = accessToken,
            refreshToken = refreshToken,
            createdAtEpochMs = createdAt,
            expiresInSeconds = expiresInSeconds
        )
    }

    /**
     * Atomically saves both replacement access and refresh tokens.
     */
    fun saveToken(token: TraktToken) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, encrypt(token.accessToken))
            .putString(KEY_REFRESH_TOKEN, encrypt(token.refreshToken))
            .putLong(KEY_CREATED_AT, token.createdAtEpochMs)
            .putLong(KEY_EXPIRES_AT, token.expiresAtEpochMs)
            .apply()
    }

    fun loadProfile(): TraktUserProfile? {
        val username = preferences.getString(KEY_USERNAME, null) ?: return null
        val name = preferences.getString(KEY_DISPLAY_NAME, username) ?: username
        val isVip = preferences.getBoolean(KEY_IS_VIP, false)
        val avatarUrl = preferences.getString(KEY_AVATAR_URL, null)

        return TraktUserProfile(
            username = username,
            name = name,
            isVip = isVip,
            avatarUrl = avatarUrl
        )
    }

    /**
     * Saves both token and profile together in one atomic preferences commit.
     */
    fun saveConnection(token: TraktToken, profile: TraktUserProfile) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, encrypt(token.accessToken))
            .putString(KEY_REFRESH_TOKEN, encrypt(token.refreshToken))
            .putLong(KEY_CREATED_AT, token.createdAtEpochMs)
            .putLong(KEY_EXPIRES_AT, token.expiresAtEpochMs)
            .putString(KEY_USERNAME, profile.username)
            .putString(KEY_DISPLAY_NAME, profile.name)
            .putBoolean(KEY_IS_VIP, profile.isVip)
            .putString(KEY_AVATAR_URL, profile.avatarUrl)
            .apply()
    }

    /**
     * Completely clears the connection and tokens for this profile.
     */
    fun clearConnection() {
        preferences.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_CREATED_AT)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_USERNAME)
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_IS_VIP)
            .remove(KEY_AVATAR_URL)
            .apply()
    }

    /**
     * Returns a valid access token. If the current token is close to expiry (within bufferSeconds),
     * serializes the refresh using a Mutex so concurrent callers share a single refresh call
     * and do not burn Trakt's single-use refresh token.
     */
    suspend fun getValidAccessToken(
        clientId: String,
        client: TraktClient,
        bufferSeconds: Long = 300L
    ): String? {
        val currentToken = loadToken() ?: return null

        if (!currentToken.isExpired(bufferSeconds)) {
            return currentToken.accessToken
        }

        // Token is expired or expiring soon; acquire mutex to refresh
        return refreshMutex.withLock {
            val freshCheck = loadToken() ?: return@withLock null
            if (!freshCheck.isExpired(bufferSeconds)) {
                return@withLock freshCheck.accessToken
            }

            when (val refreshResult = client.refreshToken(freshCheck.refreshToken, clientId)) {
                is TraktAuthResult.Success -> {
                    saveToken(refreshResult.data)
                    refreshResult.data.accessToken
                }
                is TraktAuthResult.Failure -> {
                    null
                }
            }
        }
    }

    // --- KeyStore Encryption ---

    private fun encrypt(value: String): String {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            Base64.getEncoder().encodeToString(cipher.iv + encrypted)
        } catch (_: Throwable) {
            // Software/test fallback when KeyStore is unavailable
            fallbackObfuscate(value)
        }
    }

    private fun decrypt(value: String): String? {
        return try {
            val payload = Base64.getDecoder().decode(value)
            val iv = payload.copyOfRange(0, IV_SIZE)
            val encrypted = payload.copyOfRange(IV_SIZE, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            cipher.doFinal(encrypted).toString(Charsets.UTF_8)
        } catch (_: Throwable) {
            fallbackDeobfuscate(value)
        }
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
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

    private fun fallbackObfuscate(input: String): String {
        val bytes = input.toByteArray(Charsets.UTF_8)
        val xor = ByteArray(bytes.size) { i -> (bytes[i].toInt() xor 0x5A).toByte() }
        return "raw:" + Base64.getEncoder().encodeToString(xor)
    }

    private fun fallbackDeobfuscate(input: String): String? {
        if (!input.startsWith("raw:")) return null
        val payload = Base64.getDecoder().decode(input.removePrefix("raw:"))
        val xor = ByteArray(payload.size) { i -> (payload[i].toInt() xor 0x5A).toByte() }
        return String(xor, Charsets.UTF_8)
    }

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val KEY_ALIAS = "fortress_trakt_encryption_key"

        private const val KEY_CLIENT_ID = "client_id"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_CREATED_AT = "created_at"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_USERNAME = "username"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_IS_VIP = "is_vip"
        private const val KEY_AVATAR_URL = "avatar_url"
        private const val KEY_SCROBBLE_ENABLED = "scrobble_enabled"

        private fun sanitizeNamespace(ns: String): String {
            return ns.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        }
    }
}
