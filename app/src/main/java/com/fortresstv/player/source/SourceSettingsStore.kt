package com.fortresstv.player.source

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SourceSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("fortress_sources", Context.MODE_PRIVATE)

    fun playlistSource(): ContentSource? = readSource("playlist")

    fun epgSource(): ContentSource? = readSource("epg")

    fun savePlaylist(source: ContentSource) = saveSource("playlist", source)

    fun saveEpg(source: ContentSource) = saveSource("epg", source)

    fun clearPlaylist() = clearSource("playlist")

    fun clearEpg() = clearSource("epg")

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
