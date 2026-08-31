package com.fortresstv.player.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class WrongPasswordException(message: String = "Incorrect backup password") : Exception(message)
class InvalidBackupFormatException(message: String = "Invalid backup format or corrupted file") : Exception(message)
class UnsupportedBackupVersionException(val fileVersion: Int) : Exception("Unsupported backup version: $fileVersion")

object BackupCrypto {
    private val MAGIC_HEADER = byteArrayOf(0x46, 0x54, 0x56, 0x42) // "FTVB"
    private const val FILE_VERSION = 1
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12
    private const val KEY_SIZE_BITS = 256
    private const val PBKDF2_ITERATIONS = 100_000
    private const val TAG_SIZE_BITS = 128

    private val secureRandom = SecureRandom()

    fun encrypt(plainTextBytes: ByteArray, passwordChars: CharArray, outputStream: OutputStream) {
        val salt = ByteArray(SALT_SIZE).apply { secureRandom.nextBytes(this) }
        val iv = ByteArray(IV_SIZE).apply { secureRandom.nextBytes(this) }

        val secretKey = deriveKey(passwordChars, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_SIZE_BITS, iv))
        val cipherText = cipher.doFinal(plainTextBytes)

        val dataOut = DataOutputStream(outputStream)
        dataOut.write(MAGIC_HEADER)
        dataOut.writeByte(FILE_VERSION)
        dataOut.write(salt)
        dataOut.write(iv)
        dataOut.writeInt(cipherText.size)
        dataOut.write(cipherText)
        dataOut.flush()
    }

    fun encryptToBytes(plainTextBytes: ByteArray, passwordChars: CharArray): ByteArray {
        val bout = ByteArrayOutputStream()
        encrypt(plainTextBytes, passwordChars, bout)
        return bout.toByteArray()
    }

    fun decrypt(inputStream: InputStream, passwordChars: CharArray): ByteArray {
        val dataIn = DataInputStream(inputStream)

        val header = ByteArray(MAGIC_HEADER.size)
        try {
            dataIn.readFully(header)
        } catch (e: Exception) {
            throw InvalidBackupFormatException("File is too short or empty")
        }

        if (!Arrays.equals(header, MAGIC_HEADER)) {
            throw InvalidBackupFormatException("Header mismatch: not a FortressTV backup file")
        }

        val version = dataIn.readByte().toInt()
        if (version != FILE_VERSION) {
            throw UnsupportedBackupVersionException(version)
        }

        val salt = ByteArray(SALT_SIZE)
        val iv = ByteArray(IV_SIZE)
        dataIn.readFully(salt)
        dataIn.readFully(iv)

        val cipherTextLength = dataIn.readInt()
        if (cipherTextLength <= 0 || cipherTextLength > 50 * 1024 * 1024) { // safety limit 50MB
            throw InvalidBackupFormatException("Invalid payload size: $cipherTextLength")
        }

        val cipherText = ByteArray(cipherTextLength)
        dataIn.readFully(cipherText)

        val secretKey = deriveKey(passwordChars, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_SIZE_BITS, iv))

        return try {
            cipher.doFinal(cipherText)
        } catch (e: AEADBadTagException) {
            throw WrongPasswordException("Incorrect backup password or corrupted data")
        } catch (e: Exception) {
            throw InvalidBackupFormatException("Decryption failed: ${e.message}")
        }
    }

    fun decryptFromBytes(encryptedBytes: ByteArray, passwordChars: CharArray): ByteArray {
        return decrypt(ByteArrayInputStream(encryptedBytes), passwordChars)
    }

    private fun deriveKey(passwordChars: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passwordChars, salt, PBKDF2_ITERATIONS, KEY_SIZE_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
