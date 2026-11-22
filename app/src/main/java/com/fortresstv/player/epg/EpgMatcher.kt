package com.fortresstv.player.epg

import java.text.Normalizer

object EpgMatcher {

    // Common IPTV prefixes: Country codes, numbers, brackets, pipes, separators
    // e.g. "PT:", "PT|", "|PT|", "PT -", "[PT]", "(PT)", "PT ◉", "ES:", "UK:"
    private val prefixPattern = Regex(
        """^(?:\|\s*[a-zA-Z0-9]{2,5}\s*\|\s*|\[?[a-zA-Z0-9]{2,5}\]?\s*[:|–\-•◉]\s*|\(\s*[a-zA-Z0-9]{2,5}\s*\)\s*)"""
    )

    // Common resolution/quality/codec tags and parentheses:
    // e.g. "FHD", "UHD", "4K", "8K", "HD", "SD", "HEVC", "H.265", "H265", "RAW", "1080p", "720p", "50fps", "60fps", "VIP", "PREMIUM", "BACKUP", "AUTO"
    private val qualityTagPattern = Regex(
        """\b(?:fhd|uhd|4kuhd|4k|8k|hd|sd|hevc|h\.?265|h\.?264|x265|x264|raw|1080p|1080i|720p|50fps|60fps|vip|premium|backup|auto|multi)\b""",
        RegexOption.IGNORE_CASE
    )

    // Unicode superscript quality tags used by some IPTV providers (e.g. "RTP 1 ᴴᴰ", "Sport TV 1 ᶠᴴᴰ", "⁴ᴷ")
    private val superscriptQualityPattern = Regex("""[ᴴᶠᵁˢᴰᴷᴱⱽᶜ⁵⁰⁶⁰ᵖˢᴿᴬᵂ⁴⁸]+""")

    // Trailing standalone country/language tags at the end of a channel name (e.g. "RTP 1 PT", "RTP 1 | PT", "Sport TV 1 - PT")
    private val trailingCountryPattern = Regex("""(?:\s*[:|–\-]\s*|\s+)(?:pt-pt|pt|por)$""", RegexOption.IGNORE_CASE)

    // Remove text inside parentheses/brackets like "(HD)", "[1080p]"
    private val bracketPattern = Regex("""[\(\[\{].*?[\)\]\}]""")

    // Strip non-alphanumeric except '+'
    private val nonAlphaNumPattern = Regex("""[^a-z0-9+]""")

    private val digitPattern = Regex("""\d+""")

    // Accent marks left after NFD decomposition. Compiled once: normalize() runs for every guide channel.
    private val combiningMarkPattern = Regex("""\p{M}""")

    /**
     * Normalizes a channel name or EPG identifier for smart matching.
     * Examples:
     * - "PT: S+" -> "s+"
     * - "PT | SPORT TV 1 FHD" -> "sporttv1"
     * - "PT: RTP 1 HD" -> "rtp1"
     * - "SIC Notícias (HD)" -> "sicnoticias"
     * - "PT: DAZN 2 HD" -> "dazn2"
     * - "PT: DAZN 4 HD" -> "dazn4"
     */
    fun normalize(name: String?): String {
        if (name.isNullOrBlank()) return ""
        var cleaned = superscriptQualityPattern.replace(name.trim(), " ").lowercase()

        // Strip up to 4 stacked prefixes (e.g. "PT | FHD | RTP 1", "|PT| Sport TV 1")
        repeat(4) {
            val next = prefixPattern.replace(cleaned, "")
            if (next == cleaned) return@repeat
            cleaned = next
        }

        // Strip bracketed info
        cleaned = bracketPattern.replace(cleaned, " ")

        // Strip quality/codec tags
        cleaned = qualityTagPattern.replace(cleaned, " ")

        // Strip trailing country/language suffix after trimming
        cleaned = trailingCountryPattern.replace(cleaned.trim(), "")

        // Normalize accents (e.g. é -> e, ã -> a, ç -> c)
        cleaned = Normalizer.normalize(cleaned, Normalizer.Form.NFD)
        cleaned = combiningMarkPattern.replace(cleaned, "")

        // Strip non-alphanumeric except '+'
        cleaned = nonAlphaNumPattern.replace(cleaned, "")

        return cleaned
    }

    fun extractDigits(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val withoutQuality = qualityTagPattern.replace(
            superscriptQualityPattern.replace(text, " "),
            " "
        )
        return digitPattern.findAll(withoutQuality).joinToString("") { it.value }
    }
}
