package com.fortresstv.player.transfer

import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Application-layer encryption for source credentials sent over the local HTTP transport. */
internal object TransferCrypto {
    private const val VERSION = 1
    private const val IV_SIZE = 12
    private const val TAG_SIZE_BITS = 128
    private const val KEY_CONTEXT = "FortressTV local source transfer v1\u0000"
    private val secureRandom = SecureRandom()

    fun encrypt(plainText: String, sessionToken: String): String {
        val iv = ByteArray(IV_SIZE).apply { secureRandom.nextBytes(this) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(sessionToken), GCMParameterSpec(TAG_SIZE_BITS, iv))
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return JSONObject().apply {
            put("version", VERSION)
            put("iv", Base64.getEncoder().encodeToString(iv))
            put("ciphertext", Base64.getEncoder().encodeToString(encrypted))
        }.toString()
    }

    fun decrypt(envelope: String, sessionToken: String): String {
        val json = JSONObject(envelope)
        require(json.optInt("version", 0) == VERSION) { "Unsupported encrypted transfer version" }
        val iv = Base64.getDecoder().decode(json.getString("iv"))
        val encrypted = Base64.getDecoder().decode(json.getString("ciphertext"))
        require(iv.size == IV_SIZE) { "Invalid encrypted transfer IV" }
        require(encrypted.size >= TAG_SIZE_BITS / 8) { "Invalid encrypted transfer payload" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(sessionToken), GCMParameterSpec(TAG_SIZE_BITS, iv))
        return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }

    private fun key(sessionToken: String): SecretKeySpec {
        require(sessionToken.matches(Regex("[0-9a-fA-F]{32}"))) { "Invalid transfer token" }
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest((KEY_CONTEXT + sessionToken.lowercase()).toByteArray(Charsets.UTF_8))
        return SecretKeySpec(bytes, "AES")
    }
}
