package com.fortresstv.player.player

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.Consumer
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.extractor.text.SubtitleParser
import org.junit.Assert.*
import org.junit.Test

class OffsetSubtitleParserTest {
    private class Parser(private val start: Long) : SubtitleParser {
        var options: SubtitleParser.OutputOptions? = null
        var resetCount = 0
        override fun getCueReplacementBehavior() = Format.CUE_REPLACEMENT_BEHAVIOR_REPLACE
        override fun reset() { resetCount++ }
        override fun parse(data: ByteArray, offset: Int, length: Int, outputOptions: SubtitleParser.OutputOptions, output: Consumer<CuesWithTiming>) {
            options = outputOptions
            output.accept(CuesWithTiming(emptyList(), start, 2_000_000))
        }
    }

    @Test
    fun shiftsCueStartEarlierAndLaterWithoutChangingDuration() {
        for (offset in listOf(-500_000L, 500_000L)) {
            var result: CuesWithTiming? = null
            OffsetSubtitleParser(Parser(1_000_000), offset).parse(byteArrayOf(), 0, 0, SubtitleParser.OutputOptions.allCues()) { result = it }
            assertEquals(1_000_000 + offset, result!!.startTimeUs)
            assertEquals(2_000_000, result!!.durationUs)
        }
    }

    @Test
    fun sampleRelativeCuesRetainTheirRelativeTiming() {
        var result: CuesWithTiming? = null
        OffsetSubtitleParser(Parser(C.TIME_UNSET), -100_000).parse(byteArrayOf(), 0, 0, SubtitleParser.OutputOptions.allCues()) { result = it }
        assertEquals(-100_000, result!!.startTimeUs)
    }

    @Test
    fun externalSubtitleEntryPointAppliesOffset() {
        var result: CuesWithTiming? = null
        OffsetSubtitleParser(Parser(1_000_000), 100_000).parse(byteArrayOf(), SubtitleParser.OutputOptions.allCues()) { result = it }
        assertEquals(1_100_000, result!!.startTimeUs)
    }

    @Test
    fun seekFilteringUsesOriginalTimelineAndResetReachesParser() {
        val delegate = Parser(1_000_000)
        val parser = OffsetSubtitleParser(delegate, 500_000)
        parser.parse(byteArrayOf(), 0, 0, SubtitleParser.OutputOptions.cuesAfterThenRemainingCuesBefore(2_000_000)) {}
        assertEquals(1_500_000, delegate.options!!.startTimeUs)
        assertTrue(delegate.options!!.outputAllCues)
        parser.reset()
        assertEquals(1, delegate.resetCount)
    }

    @Test
    fun clampsSavedOffsetsToSupportedRange() {
        assertEquals(-5_000, boundedSubtitleOffsetMs(Int.MIN_VALUE))
        assertEquals(5_000, boundedSubtitleOffsetMs(Int.MAX_VALUE))
        assertEquals(100, boundedSubtitleOffsetMs(100))
    }
}
