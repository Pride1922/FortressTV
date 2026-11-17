package com.fortresstv.player.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamQualityTest {

    @Test
    fun classifiesCommonStreamResolutions() {
        assertNull(classifyStreamQuality(0, 0))
        assertEquals(StreamQualityTier.SD, classifyStreamQuality(720, 576))
        assertEquals(StreamQualityTier.SD, classifyStreamQuality(854, 480))
        assertEquals(StreamQualityTier.HD, classifyStreamQuality(1280, 720))
        assertEquals(StreamQualityTier.FHD, classifyStreamQuality(1920, 1080))
        assertEquals(StreamQualityTier.QHD_2K, classifyStreamQuality(2560, 1440))
        assertEquals(StreamQualityTier.UHD_4K, classifyStreamQuality(3840, 2160))
    }
}
