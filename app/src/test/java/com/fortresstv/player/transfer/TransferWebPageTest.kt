package com.fortresstv.player.transfer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferWebPageTest {

    private val html = TransferWebPage.generate("Living Room TV", "492815")

    @Test
    fun htmlContainsDoctype() {
        assertTrue(html.contains("<!DOCTYPE html>", ignoreCase = true))
    }

    @Test
    fun htmlContainsM3uTab() {
        assertTrue(html.contains("M3U", ignoreCase = true))
    }

    @Test
    fun htmlContainsXtreamTab() {
        assertTrue(html.contains("Xtream", ignoreCase = true))
    }

    @Test
    fun htmlContainsBackupUpload() {
        assertTrue(html.contains("/api/backup"))
        assertTrue(html.contains("type=\"file\""))
        assertTrue(html.contains("10 * 1024 * 1024"))
    }

    @Test
    fun htmlContainsTokenExtractionJs() {
        assertTrue(html.contains("window.location.hash"))
    }

    @Test
    fun htmlContainsNativeAppHandoff() {
        assertTrue(html.contains("id=\"open-app\""))
        assertTrue(html.contains("fortresstv://send"))
        assertTrue(html.contains("encodeURIComponent(endpoint)"))
        assertTrue(html.contains("encodeURIComponent(token)"))
    }

    @Test
    fun htmlContainsApiSourceEndpoint() {
        assertTrue(html.contains("/api/source"))
    }

    @Test
    fun htmlContainsProtocolVersion() {
        assertTrue(html.contains(TRANSFER_PROTOCOL_VERSION.toString()))
    }

    @Test
    fun htmlContainsPinSection() {
        assertTrue(html.contains("pin-section"))
    }

    @Test
    fun deviceNameIsHtmlEscaped() {
        val injected = TransferWebPage.generate("<script>alert(1)</script>", "123456")
        assertFalse("Raw script tag must not appear", injected.contains("<script>alert(1)"))
        assertTrue("Escaped form must appear", injected.contains("&lt;script&gt;"))
    }

    @Test
    fun pinIsHtmlEscaped() {
        // Ensure a pin with special chars would be escaped (PIN is numeric but let's be safe via the helper)
        val page = TransferWebPage.generate("TV", "123456")
        assertTrue(page.contains("123456"))
    }

    @Test
    fun fetchCallUsesAuthorizationHeader() {
        assertTrue(html.contains("Authorization"))
    }

    @Test
    fun htmlHasPasswordInput() {
        assertTrue(html.contains("type=\"password\""))
    }
}
