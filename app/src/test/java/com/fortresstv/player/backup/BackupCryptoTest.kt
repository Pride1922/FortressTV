package com.fortresstv.player.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupCryptoTest {

    @Test
    fun roundTripEncryptionDecryptionSucceeds() {
        val originalData = "Hello FortressTV Backup and Restore with AES-256-GCM!".toByteArray(Charsets.UTF_8)
        val password = "SuperSecretPassword123!".toCharArray()

        val encrypted = BackupCrypto.encryptToBytes(originalData, password)
        assertTrue(encrypted.size > originalData.size)
        assertEquals(2, encrypted[4].toInt())

        val decrypted = BackupCrypto.decryptFromBytes(encrypted, password)
        assertArrayEquals(originalData, decrypted)
    }

    @Test
    fun restoresLegacyVersionOneBackup() {
        val originalData = "Legacy FortressTV backup".toByteArray()
        val password = "LegacyPassword".toCharArray()
        val legacyBackup = createLegacyVersionOneBackup(originalData, password)

        assertArrayEquals(originalData, BackupCrypto.decryptFromBytes(legacyBackup, password))
    }

    @Test
    fun wrongPasswordThrowsWrongPasswordException() {
        val originalData = "Sensitive IPTV Playlist Data".toByteArray(Charsets.UTF_8)
        val correctPassword = "CorrectPassword".toCharArray()
        val wrongPassword = "WrongPassword".toCharArray()

        val encrypted = BackupCrypto.encryptToBytes(originalData, correctPassword)

        assertThrows(WrongPasswordException::class.java) {
            BackupCrypto.decryptFromBytes(encrypted, wrongPassword)
        }
    }

    @Test
    fun tamperedCiphertextThrowsWrongPasswordOrInvalidFormatException() {
        val originalData = "Some payload data".toByteArray(Charsets.UTF_8)
        val password = "Password".toCharArray()

        val encrypted = BackupCrypto.encryptToBytes(originalData, password)
        // Corrupt the ciphertext payload
        encrypted[encrypted.size - 5] = (encrypted[encrypted.size - 5].toInt() xor 0xFF).toByte()

        assertThrows(Exception::class.java) {
            BackupCrypto.decryptFromBytes(encrypted, password)
        }
    }

    @Test
    fun invalidHeaderThrowsInvalidBackupFormatException() {
        val garbage = byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07)
        val password = "Password".toCharArray()

        val ex = assertThrows(InvalidBackupFormatException::class.java) {
            BackupCrypto.decryptFromBytes(garbage, password)
        }
        assertTrue(ex.message!!.contains("Header mismatch") || ex.message!!.contains("short"))
    }

    private fun createLegacyVersionOneBackup(data: ByteArray, password: CharArray): ByteArray {
        val salt = ByteArray(16) { it.toByte() }
        val iv = ByteArray(12) { (it + 16).toByte() }
        val spec = PBEKeySpec(password, salt, 100_000, 256)
        val key = SecretKeySpec(
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,
            "AES"
        )
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(data)
        return ByteArrayOutputStream().also { bytes ->
            DataOutputStream(bytes).use { output ->
                output.write(byteArrayOf(0x46, 0x54, 0x56, 0x42))
                output.writeByte(1)
                output.write(salt)
                output.write(iv)
                output.writeInt(encrypted.size)
                output.write(encrypted)
            }
        }.toByteArray()
    }
}
