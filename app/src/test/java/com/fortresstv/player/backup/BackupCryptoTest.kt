package com.fortresstv.player.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCryptoTest {

    @Test
    fun roundTripEncryptionDecryptionSucceeds() {
        val originalData = "Hello FortressTV Backup and Restore with AES-256-GCM!".toByteArray(Charsets.UTF_8)
        val password = "SuperSecretPassword123!".toCharArray()

        val encrypted = BackupCrypto.encryptToBytes(originalData, password)
        assertTrue(encrypted.size > originalData.size)

        val decrypted = BackupCrypto.decryptFromBytes(encrypted, password)
        assertArrayEquals(originalData, decrypted)
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
}
