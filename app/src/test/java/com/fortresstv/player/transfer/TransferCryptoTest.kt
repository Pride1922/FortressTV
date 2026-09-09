package com.fortresstv.player.transfer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TransferCryptoTest {
    @Test
    fun encryptsAndDecryptsSourcePayload() {
        val token = "00112233445566778899aabbccddeeff"
        val payload = """{"password":"private","url":"http://provider.test/list.m3u"}"""
        assertEquals(payload, TransferCrypto.decrypt(TransferCrypto.encrypt(payload, token), token))
    }

    @Test
    fun wrongTokenCannotDecryptSourcePayload() {
        val encrypted = TransferCrypto.encrypt("secret", "00112233445566778899aabbccddeeff")
        assertThrows(Exception::class.java) {
            TransferCrypto.decrypt(encrypted, "ffeeddccbbaa99887766554433221100")
        }
    }
}
