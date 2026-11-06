package com.fortresstv.player.profile

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

enum class PinConfigurationState {
    GENUINE_NO_PIN,          // Genuinely unconfigured local installation
    CONFIRMED_NO_PIN,        // Server confirmed that household has no PIN
    CONFIRMED_HAS_PIN,       // PIN is confirmed configured
    UNKNOWN,                 // State not yet confirmed from server, or missing in authenticated household
    CORRUPTED                // Encrypted store corrupted / decrypt failed
}

internal sealed class PinPayloadResult {
    data class Valid(val payload: JSONObject) : PinPayloadResult()
    object UnconfiguredLocal : PinPayloadResult()
    object UnconfirmedAuthenticated : PinPayloadResult()
    object Corrupted : PinPayloadResult()
}

internal interface PinPersistence {
    fun load(): PinPayloadResult
    fun save(json: JSONObject)
    fun switchNamespace(namespace: String)
}

internal class EncryptedPinPersistence(
    private val context: Context,
    initialNamespace: String = "local"
) : PinPersistence {
    private var namespace: String = sanitize(initialNamespace)
    private var preferences = getPrefs(namespace)

    private fun sanitize(raw: String): String = raw.replace(Regex("[^a-zA-Z0-9_-]"), "_")

    private fun getPrefs(ns: String) =
        context.getSharedPreferences("fortress_parental_store_$ns", Context.MODE_PRIVATE)

    override fun switchNamespace(namespace: String) {
        this.namespace = sanitize(namespace)
        preferences = getPrefs(this.namespace)
    }

    override fun load(): PinPayloadResult {
        val raw = preferences.getString("parental_envelope", null)
        if (raw == null) {
            return if (namespace == "local") PinPayloadResult.UnconfiguredLocal
            else PinPayloadResult.UnconfirmedAuthenticated
        }
        return runCatching {
            val bytes = Base64.decode(raw, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey(namespace), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            val json = JSONObject(String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8))
            PinPayloadResult.Valid(json)
        }.getOrElse {
            PinPayloadResult.Corrupted
        }
    }

    override fun save(json: JSONObject) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey(namespace))
        val bytes = cipher.iv + cipher.doFinal(json.toString().toByteArray(Charsets.UTF_8))
        preferences.edit().putString("parental_envelope", Base64.encodeToString(bytes, Base64.NO_WRAP)).apply()
    }

    private fun secretKey(ns: String): SecretKey {
        val keyAlias = "fortress_parental_key_$ns"
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            generateKey()
        }
    }
}

data class PinStatus(
    val hasPin: Boolean,
    val isLocked: Boolean,
    val remainingAttempts: Int,
    val lockoutSeconds: Int,
    val configurationState: PinConfigurationState = if (hasPin) PinConfigurationState.CONFIRMED_HAS_PIN else PinConfigurationState.GENUINE_NO_PIN
) {
    val isProtected: Boolean
        get() = configurationState == PinConfigurationState.CONFIRMED_HAS_PIN ||
                configurationState == PinConfigurationState.UNKNOWN ||
                configurationState == PinConfigurationState.CORRUPTED
}

sealed class PinVerifyResult {
    data class Success(val unlockToken: String?) : PinVerifyResult()
    data class Failed(val remainingAttempts: Int) : PinVerifyResult()
    data class Locked(val lockoutSeconds: Int) : PinVerifyResult()
}

internal class ParentalPinStore(
    private val persistence: PinPersistence
) {
    constructor(context: Context, initialNamespace: String = "local") : this(EncryptedPinPersistence(context, initialNamespace))

    @Volatile
    private var inMemoryAdultUnlockedUntil: Long = 0L

    @Volatile
    private var inMemoryAdultUnlockToken: String? = null

    fun isAdultUnlocked(): Boolean = System.currentTimeMillis() < inMemoryAdultUnlockedUntil

    fun currentUnlockToken(): String? = if (isAdultUnlocked()) inMemoryAdultUnlockToken else null

    fun grantAdultUnlock(token: String? = null, durationMs: Long = 15 * 60 * 1000L) {
        inMemoryAdultUnlockedUntil = System.currentTimeMillis() + durationMs
        inMemoryAdultUnlockToken = token
    }

    fun revokeAdultUnlock() {
        inMemoryAdultUnlockedUntil = 0L
        inMemoryAdultUnlockToken = null
    }

    fun switchNamespace(namespace: String) {
        revokeAdultUnlock()
        persistence.switchNamespace(namespace)
    }

    @Synchronized
    fun getStatus(): PinStatus {
        return when (val result = persistence.load()) {
            is PinPayloadResult.Corrupted -> {
                // Fail closed: corrupt storage is treated as PIN-protected and locked
                PinStatus(
                    configurationState = PinConfigurationState.CORRUPTED,
                    hasPin = true,
                    isLocked = true,
                    remainingAttempts = 0,
                    lockoutSeconds = 900
                )
            }
            is PinPayloadResult.UnconfirmedAuthenticated -> {
                // Authenticated account before server confirmation: must fail closed as protected
                PinStatus(
                    configurationState = PinConfigurationState.UNKNOWN,
                    hasPin = false,
                    isLocked = false,
                    remainingAttempts = 5,
                    lockoutSeconds = 0
                )
            }
            is PinPayloadResult.UnconfiguredLocal -> {
                // Genuinely unconfigured local installation
                PinStatus(
                    configurationState = PinConfigurationState.GENUINE_NO_PIN,
                    hasPin = false,
                    isLocked = false,
                    remainingAttempts = 5,
                    lockoutSeconds = 0
                )
            }
            is PinPayloadResult.Valid -> {
                val payload = result.payload
                val confirmedNoPin = payload.optBoolean("server_confirmed_no_pin", false)
                val storedHash = payload.optString("pin_hash")
                val hasPin = !storedHash.isNullOrBlank()

                val configState = when {
                    hasPin -> PinConfigurationState.CONFIRMED_HAS_PIN
                    confirmedNoPin -> PinConfigurationState.CONFIRMED_NO_PIN
                    else -> PinConfigurationState.UNKNOWN
                }

                val lockedUntil = payload.optLong("locked_until", 0L)
                val now = System.currentTimeMillis()
                val isLocked = lockedUntil > now
                val lockoutSeconds = if (isLocked) ((lockedUntil - now) / 1000).toInt().coerceAtLeast(1) else 0
                val failed = payload.optInt("failed_attempts", 0)
                val remaining = (5 - failed).coerceIn(0, 5)

                PinStatus(
                    configurationState = configState,
                    hasPin = hasPin,
                    isLocked = isLocked,
                    remainingAttempts = remaining,
                    lockoutSeconds = lockoutSeconds
                )
            }
        }
    }

    @Synchronized
    fun verifyPinLocally(pin: String): PinVerifyResult {
        when (val res = persistence.load()) {
            is PinPayloadResult.Corrupted -> {
                return PinVerifyResult.Locked(900)
            }
            is PinPayloadResult.UnconfirmedAuthenticated -> {
                return PinVerifyResult.Failed(0)
            }
            is PinPayloadResult.UnconfiguredLocal -> {
                grantAdultUnlock()
                return PinVerifyResult.Success(null)
            }
            is PinPayloadResult.Valid -> {
                val payload = res.payload
                val storedHash = payload.optString("pin_hash")
                val salt = payload.optString("pin_salt")
                val now = System.currentTimeMillis()
                val lockedUntil = payload.optLong("locked_until", 0L)

                if (lockedUntil > now) {
                    val seconds = ((lockedUntil - now) / 1000).toInt().coerceAtLeast(1)
                    return PinVerifyResult.Locked(seconds)
                }

                if (storedHash.isBlank() || salt.isBlank()) {
                    grantAdultUnlock()
                    return PinVerifyResult.Success(null)
                }

                if (storedHash == "server_managed") {
                    return PinVerifyResult.Failed(0)
                }

                val calculatedHash = runCatching { hashPin(salt, pin) }.getOrNull()
                if (calculatedHash != null && calculatedHash == storedHash) {
                    payload.put("failed_attempts", 0)
                    payload.remove("locked_until")
                    persistence.save(payload)
                    grantAdultUnlock()
                    return PinVerifyResult.Success(null)
                } else {
                    val failed = payload.optInt("failed_attempts", 0) + 1
                    payload.put("failed_attempts", failed)
                    if (failed >= 5) {
                        payload.put("locked_until", now + 15 * 60 * 1000L)
                    }
                    persistence.save(payload)
                    return if (failed >= 5) {
                        PinVerifyResult.Locked(15 * 60)
                    } else {
                        PinVerifyResult.Failed((5 - failed).coerceAtLeast(0))
                    }
                }
            }
        }
    }

    @Synchronized
    fun setPinLocally(newPin: String) {
        val salt = UUID.randomUUID().toString()
        val hash = hashPin(salt, newPin)
        val payload = when (val res = persistence.load()) {
            is PinPayloadResult.Valid -> res.payload
            else -> JSONObject()
        }
        payload.remove("server_confirmed_no_pin")
        payload.put("pin_salt", salt)
        payload.put("pin_hash", hash)
        payload.put("failed_attempts", 0)
        payload.remove("locked_until")
        persistence.save(payload)
        revokeAdultUnlock()
    }

    @Synchronized
    fun removePinLocally() {
        val payload = when (val res = persistence.load()) {
            is PinPayloadResult.Valid -> res.payload
            else -> JSONObject()
        }
        payload.remove("pin_salt")
        payload.remove("pin_hash")
        payload.put("server_confirmed_no_pin", true)
        payload.put("failed_attempts", 0)
        payload.remove("locked_until")
        persistence.save(payload)
        revokeAdultUnlock()
    }

    @Synchronized
    fun syncServerStatus(hasPin: Boolean, isLocked: Boolean, remainingAttempts: Int, lockoutSeconds: Int) {
        val payload = when (val res = persistence.load()) {
            is PinPayloadResult.Valid -> res.payload
            else -> JSONObject()
        }
        if (hasPin) {
            payload.remove("server_confirmed_no_pin")
            if (!payload.has("pin_hash")) {
                payload.put("pin_hash", "server_managed")
                payload.put("pin_salt", "server_managed")
            }
        } else {
            payload.put("server_confirmed_no_pin", true)
            payload.remove("pin_hash")
            payload.remove("pin_salt")
        }
        val failed = (5 - remainingAttempts).coerceIn(0, 5)
        payload.put("failed_attempts", failed)
        if (isLocked && lockoutSeconds > 0) {
            payload.put("locked_until", System.currentTimeMillis() + lockoutSeconds * 1000L)
        } else {
            payload.remove("locked_until")
        }
        persistence.save(payload)
    }

    companion object {
        internal fun hashPin(salt: String, pin: String): String {
            require(pin.trim().length in 4..12 && pin.trim().all { it.isDigit() }) {
                "PIN must be between 4 and 12 digits"
            }
            val digest = MessageDigest.getInstance("SHA-256")
            var current = ("$salt:${pin.trim()}").toByteArray(Charsets.UTF_8)
            for (i in 0 until 10_000) {
                current = digest.digest(current)
            }
            return current.joinToString("") { "%02x".format(it) }
        }
    }
}
