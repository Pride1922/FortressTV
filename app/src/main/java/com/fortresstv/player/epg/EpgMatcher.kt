package com.fortresstv.player.epg

import java.text.Normalizer

object EpgMatcher {

    // Common IPTV prefixes: Country codes, numbers, brackets, separators
    // e.g. "PT:", "PT|", "PT -", "[PT]", "PT - ", "ES:", "UK:", "US:", "FR:", "DE:", "IT:", "BR:"
    private val prefixPattern = Regex("""^(?:\[?[a-zA-Z0-9]{2,4}\]?\s*[:|–\-]\s*|\(\s*[a-zA-Z0-9]{2,4}\s*\)\s*)""")

    // Common resolution/quality/codec tags and parentheses:
    // e.g. "FHD", "UHD", "4K", "8K", "HD", "SD", "HEVC", "H.265", "H265", "RAW", "1080p", "720p", "50fps", "60fps", "VIP", "PREMIUM", "BACKUP", "AUTO"
    private val qualityTagPattern = Regex("""\b(?:fhd|uhd|hd|sd|hevc|h\.?265|h\.?264|raw|1080p|720p|50fps|60fps|vip|premium|backup|auto)\b""", RegexOption.IGNORE_CASE)

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
        var cleaned = name.trim().lowercase()

        // Strip prefix (e.g. "PT: ", "PT| ")
        cleaned = prefixPattern.replace(cleaned, "")

        // Strip bracketed info
        cleaned = bracketPattern.replace(cleaned, " ")

        // Strip quality/codec tags
        cleaned = qualityTagPattern.replace(cleaned, " ")

        // Normalize accents (e.g. é -> e, ã -> a, ç -> c)
        cleaned = Normalizer.normalize(cleaned, Normalizer.Form.NFD)
        cleaned = combiningMarkPattern.replace(cleaned, "")

        // Strip non-alphanumeric except '+'
        cleaned = nonAlphaNumPattern.replace(cleaned, "")

        return cleaned
    }

    fun extractDigits(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return digitPattern.findAll(text).joinToString("") { it.value }
    }
}
