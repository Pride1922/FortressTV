package com.fortresstv.player.playlist

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PlaylistDiskCacheTest {
    @Test
    fun roundTripPreservesChannelIdentityAndOrder() {
        val playlist = Playlist(
            listOf(
                PlaylistChannel(
                    position = 0,
                    name = "One",
                    streamUrl = "https://example.test/one",
                    tvgId = "one.pt",
                    tvgName = "One PT",
                    logoUrl = "https://example.test/one.png",
                    group = "Portugal",
                    sourceId = "source",
                    sourcePosition = 17,
                    sourceItemId = "item-one"
                ),
                PlaylistChannel(1, "Two", "https://example.test/two")
            )
        )
        val output = ByteArrayOutputStream()

        PlaylistCacheCodec.write(output, "fingerprint", playlist)
        val restored = PlaylistCacheCodec.read(ByteArrayInputStream(output.toByteArray()), "fingerprint")

        assertEquals(playlist, restored)
        assertEquals(listOf("Portugal"), restored.groups)
    }

    @Test
    fun rejectsStaleFingerprint() {
        val output = ByteArrayOutputStream()
        PlaylistCacheCodec.write(output, "old", Playlist(emptyList()))

        assertThrows(IllegalArgumentException::class.java) {
            PlaylistCacheCodec.read(ByteArrayInputStream(output.toByteArray()), "new")
        }
    }
}
