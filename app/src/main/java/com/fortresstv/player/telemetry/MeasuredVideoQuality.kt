package com.fortresstv.player.telemetry

import com.fortresstv.player.player.classifyStreamQuality
import org.json.JSONObject
import java.security.MessageDigest

private val variantIdPattern = Regex("^[0-9a-f]{64}$")
internal fun validStreamVariantId(value: String?): String? = value?.takeIf(variantIdPattern::matches)

/** Opaque source/item/stream identity. Raw URLs and credentials never enter the payload. */
internal fun playbackStreamVariantId(sourceId: String, itemId: String, streamUrl: String): String? {
    if (sourceId.isBlank() || itemId.isBlank() || streamUrl.isBlank()) return null
    val identity = listOf(sourceId, itemId, streamUrl.trim()).joinToString("\u0000")
    return MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

data class MeasuredVideoQuality(val width: Int, val height: Int, val observedAtEpochMs: Long) {
    init {
        require(width in 1..32_768 && height in 1..32_768)
        require(observedAtEpochMs > 0)
    }

    val tier: String get() = classifyStreamQuality(width, height)!!.label

    companion object {
        internal fun fromJson(json: JSONObject): MeasuredVideoQuality? {
            val width = json.optInt("video_width", 0)
            val height = json.optInt("video_height", 0)
            val observedAt = json.optLong("quality_observed_at_ms", 0L)
            return if (width in 1..32_768 && height in 1..32_768 && observedAt > 0) {
                MeasuredVideoQuality(width, height, observedAt)
            } else null
        }
    }
}
