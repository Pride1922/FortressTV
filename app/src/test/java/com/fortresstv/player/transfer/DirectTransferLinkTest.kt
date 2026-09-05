package com.fortresstv.player.transfer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DirectTransferLinkTest {
    private val token = "0123456789abcdef0123456789abcdef"

    @Test
    fun acceptsPrivateLanEndpoint() {
        val target = DirectTransferLink.parse(
            "fortresstv://send?endpoint=http%3A%2F%2F192.168.1.50%3A8080&token=$token"
        )

        assertNotNull(target)
        assertEquals("192.168.1.50", target?.addresses?.single()?.hostAddress)
        assertEquals(8080, target?.port)
        assertEquals(token, target?.sessionToken)
    }

    @Test
    fun rejectsPublicEndpoint() {
        assertNull(DirectTransferLink.parse(
            "fortresstv://send?endpoint=http%3A%2F%2F8.8.8.8%3A8080&token=$token"
        ))
    }

    @Test
    fun rejectsHttpsEndpoint() {
        assertNull(DirectTransferLink.parse(
            "fortresstv://send?endpoint=https%3A%2F%2F192.168.1.50%3A8080&token=$token"
        ))
    }

    @Test
    fun rejectsInvalidToken() {
        assertNull(DirectTransferLink.parse(
            "fortresstv://send?endpoint=http%3A%2F%2F192.168.1.50%3A8080&token=1234"
        ))
    }
}
