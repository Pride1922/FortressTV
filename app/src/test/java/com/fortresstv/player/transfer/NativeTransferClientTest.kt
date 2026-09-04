package com.fortresstv.player.transfer

import com.fortresstv.player.playlist.PlaylistProfile
import com.fortresstv.player.source.ContentSource
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeTransferClientTest {
    @Test
    fun `serializes remote URL using protocol version one`() {
        val profile = PlaylistProfile("one", "Home", ContentSource.RemoteUrl("https://example.test/list.m3u", "Home"))
        val json = JSONObject(createNativeTransferPayload(profile)!!)

        assertEquals(TRANSFER_PROTOCOL_VERSION, json.getInt("protocolVersion"))
        assertEquals("m3u", json.getString("kind"))
        assertEquals("Home", json.getString("name"))
        assertEquals("https://example.test/list.m3u", json.getString("url"))
    }

    @Test
    fun `serializes Xtream credentials without changing them`() {
        val profile = PlaylistProfile(
            "two",
            "Provider",
            ContentSource.Xtream("https://tv.example.test", "Provider", "user name", "p@ss&word")
        )
        val json = JSONObject(createNativeTransferPayload(profile)!!)

        assertEquals("xtream", json.getString("kind"))
        assertEquals("user name", json.getString("username"))
        assertEquals("p@ss&word", json.getString("password"))
    }

    @Test
    fun `does not serialize device-local files`() {
        val profile = PlaylistProfile("three", "File", ContentSource.LocalFile("content://file", "File"))
        assertNull(createNativeTransferPayload(profile))
    }

    @Test
    fun `local network permission activates only for target and device API 37`() {
        assertTrue(LocalNetworkPermission.isRuntimePermissionRequired(37, 37))
        assertEquals(false, LocalNetworkPermission.isRuntimePermissionRequired(36, 37))
        assertEquals(false, LocalNetworkPermission.isRuntimePermissionRequired(37, 36))
    }
}
