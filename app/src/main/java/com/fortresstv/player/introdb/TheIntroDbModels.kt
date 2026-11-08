package com.fortresstv.player.introdb

enum class SegmentType {
    INTRO,
    RECAP,
    CREDITS,
    PREVIEW
}

data class MediaSegment(
    val type: SegmentType,
    val startMs: Long,
    val endMs: Long?
) {
    /**
     * Determines whether the current playback position falls within this segment.
     */
    fun isActive(positionMs: Long, totalDurationMs: Long = Long.MAX_VALUE): Boolean {
        if (positionMs < startMs) return false
        val effectiveEnd = endMs ?: totalDurationMs.takeIf { it > startMs } ?: (startMs + 180_000L)
        return positionMs in startMs..effectiveEnd
    }
}

data class EpisodeSegments(
    val season: Int,
    val episode: Int,
    val intro: List<MediaSegment> = emptyList(),
    val recap: List<MediaSegment> = emptyList(),
    val credits: List<MediaSegment> = emptyList(),
    val preview: List<MediaSegment> = emptyList()
) {
    val allSegments: List<MediaSegment> = recap + intro + credits + preview

    /**
     * Finds the active segment for the given playback position.
     * Priority: RECAP, INTRO, CREDITS, PREVIEW.
     */
    fun activeSegment(positionMs: Long, totalDurationMs: Long = Long.MAX_VALUE): MediaSegment? {
        recap.firstOrNull { it.isActive(positionMs, totalDurationMs) }?.let { return it }
        intro.firstOrNull { it.isActive(positionMs, totalDurationMs) }?.let { return it }
        credits.firstOrNull { it.isActive(positionMs, totalDurationMs) }?.let { return it }
        preview.firstOrNull { it.isActive(positionMs, totalDurationMs) }?.let { return it }
        return null
    }
}
