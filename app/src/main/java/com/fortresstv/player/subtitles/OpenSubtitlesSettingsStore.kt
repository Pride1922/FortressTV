package com.fortresstv.player.subtitles

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class OpenSubtitlesSettingsStore(context: Context) {

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadSettings(): OpenSubtitlesSettings {
        val enabled = preferences.getBoolean(KEY_ENABLED, false)
        val apiKey = preferences.getString(KEY_API_KEY, null)?.let(::decrypt).orEmpty()
        val username = preferences.getString(KEY_USERNAME, null)?.let(::decrypt).orEmpty()
        val password = preferences.getString(KEY_PASSWORD, null)?.let(::decrypt).orEmpty()
        val preferPtPt = preferences.getBoolean(KEY_PREFER_PT_PT, true)
        val autoSearch = preferences.getBoolean(KEY_AUTO_SEARCH, true)

        return OpenSubtitlesSettings(
            enabled = enabled,
            apiKey = apiKey,
            username = username,
            password = password,
            preferPtPt = preferPtPt,
            autoSearch = autoSearch
        )
    }

    fun saveSettings(settings: OpenSubtitlesSettings) {
        preferences.edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putString(KEY_API_KEY, if (settings.apiKey.isNotBlank()) encrypt(settings.apiKey) else null)
            .putString(KEY_USERNAME, if (settings.username.isNotBlank()) encrypt(settings.username) else null)
            .putString(KEY_PASSWORD, if (settings.password.isNotBlank()) encrypt(settings.password) else null)
            .putBoolean(KEY_PREFER_PT_PT, settings.preferPtPt)
            .putBoolean(KEY_AUTO_SEARCH, settings.autoSearch)
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
        const val PREFS_NAME = "fortress_opensubtitles_settings"
        const val KEY_ALIAS = "fortress_opensubtitles_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12

        const val KEY_ENABLED = "enabled"
        const val KEY_API_KEY = "api_key"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
        const val KEY_PREFER_PT_PT = "prefer_pt_pt"
        const val KEY_AUTO_SEARCH = "auto_search"
    }
}
