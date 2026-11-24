package com.fortresstv.player.player

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.Consumer
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.extractor.text.DefaultSubtitleParserFactory
import androidx.media3.extractor.text.SubtitleParser

internal fun boundedSubtitleOffsetMs(value: Int): Int = value.coerceIn(-5_000, 5_000)

/** Shifts parsed cues before Media3 queues them; audio/video timestamps are untouched. */
@UnstableApi
internal class OffsetSubtitleParser(
    private val delegate: SubtitleParser,
    private val offsetUs: Long
) : SubtitleParser by delegate {
    override fun reset() = delegate.reset()

    override fun parse(data: ByteArray, outputOptions: SubtitleParser.OutputOptions, output: Consumer<CuesWithTiming>) {
        parse(data, 0, data.size, outputOptions, output)
    }

    override fun parse(
        data: ByteArray, offset: Int, length: Int,
        outputOptions: SubtitleParser.OutputOptions,
        output: Consumer<CuesWithTiming>
    ) {
        val options = if (outputOptions.startTimeUs == C.TIME_UNSET) outputOptions else {
            val originalStart = outputOptions.startTimeUs - offsetUs
            if (outputOptions.outputAllCues) SubtitleParser.OutputOptions.cuesAfterThenRemainingCuesBefore(originalStart)
            else SubtitleParser.OutputOptions.onlyCuesAfter(originalStart)
        }
        delegate.parse(data, offset, length, options) { cues ->
            // TIME_UNSET means a sample-relative cue with no explicit timestamp.
            // Giving it a relative offset preserves its container sample time.
            val start = if (cues.startTimeUs == C.TIME_UNSET) {
                if (offsetUs == 0L) C.TIME_UNSET else offsetUs
            } else cues.startTimeUs + offsetUs
            output.accept(CuesWithTiming(cues.cues, start, cues.durationUs))
        }
    }
}

@UnstableApi
internal class OffsetSubtitleParserFactory(
    offsetMs: Int,
    private val delegate: SubtitleParser.Factory = DefaultSubtitleParserFactory()
) : SubtitleParser.Factory by delegate {
    private val offsetUs = boundedSubtitleOffsetMs(offsetMs) * 1_000L
    override fun create(format: Format): SubtitleParser = OffsetSubtitleParser(delegate.create(format), offsetUs)
}
