package com.fortresstv.player.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TvFocusScaleTest {
    private val maxGrowthPx = 48f // 24 dp at 2x density

    @Test
    fun smallItemsGrowByTheRequestedAmount() {
        // A 300 px poster may grow 48 px (16%), so the 6% request is used as is.
        assertEquals(1.06f, focusScale(1.06f, widthPx = 300, maxGrowthPx = maxGrowthPx), 0.0001f)
    }

    @Test
    fun wideItemsAreCappedSoTheyStayOnScreen() {
        // A 1,920 px full-width row may only grow 48 px in total.
        val scale = focusScale(1.06f, widthPx = 1920, maxGrowthPx = maxGrowthPx)
        assertEquals(1f + 48f / 1920f, scale, 0.0001f)
        assertEquals(48f, 1920 * scale - 1920, 0.01f)
    }

    @Test
    fun noGrowthRequestedOrUnknownWidth() {
        assertEquals(1f, focusScale(1f, widthPx = 300, maxGrowthPx = maxGrowthPx), 0f)
        assertEquals(1f, focusScale(0.98f, widthPx = 300, maxGrowthPx = maxGrowthPx), 0f)
        // Before the first layout the width is unknown; use the request until it is measured.
        assertEquals(1.06f, focusScale(1.06f, widthPx = 0, maxGrowthPx = maxGrowthPx), 0f)
    }
}
