package com.fortresstv.player.account

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class AccountSessionStore(context: Context) : SessionPersistence {
    private val preferences = context.getSharedPreferences("fortress_account", Context.MODE_PRIVATE)

    override fun load(): AccountSession? = runCatching {
        val raw = preferences.getString("session", null) ?: return null
        val bytes = Base64.decode(raw, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        val envelope = JSONObject(String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8))
        require(envelope.getString("project") == AccountConfiguration.URL)
        AccountSession.decode(envelope.getJSONObject("session"))
    }.getOrElse {
        clear()
        null
    }

    override fun save(session: AccountSession) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val envelope = JSONObject().put("project", AccountConfiguration.URL).put("session", JSONObject(session.encode()))
        val bytes = cipher.iv + cipher.doFinal(envelope.toString().toByteArray(Charsets.UTF_8))
        check(preferences.edit().putString("session", Base64.encodeToString(bytes, Base64.NO_WRAP)).commit())
    }

    override fun clear() { check(preferences.edit().remove("session").commit()) }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("fortress_account_key", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder("fortress_account_key", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            generateKey()
        }
    }
}
