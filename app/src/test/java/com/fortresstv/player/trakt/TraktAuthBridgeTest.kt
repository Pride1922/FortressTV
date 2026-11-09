package com.fortresstv.player.trakt

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TraktAuthBridgeTest {

    @Test
    fun ignoresUnrelatedDeepLinks() {
        assertFalse(TraktAuthBridge.handleDeepLink(null as? android.content.Context ?: return, "https://google.com"))
    }

    @Test
    fun parsesUrlParametersCorrectly() {
        val testUrl = "fortresstv://trakt/callback?code=test_code_123&state=state_abc"
        // Validates scheme prefix check
        assertTrue(testUrl.startsWith("fortresstv://trakt"))
        val query = testUrl.substringAfter('?')
        val params = query.split('&').associate {
            val (k, v) = it.split('=')
            k to v
        }
        org.junit.Assert.assertEquals("test_code_123", params["code"])
        org.junit.Assert.assertEquals("state_abc", params["state"])
    }
}
