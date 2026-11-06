package com.fortresstv.player.profile

object ParentalControls {
    private val ADULT_CONTENT_PATTERNS = listOf(
        "xxx", "adult", "adulto", "adultos", "porn", "porno", "hardcore",
        "erotic", "erotico", "erótica", "erotica", "erótico", "18+", "+18",
        "brazzers", "playboy", "penthouse", "hustler", "dorcel", "redlight",
        "venus", "sextreme", "vivid", "private tv", "fetish", "for adults", "para adultos"
    )

    fun isAdultPattern(name: String?, groupOrCategory: String?): Boolean {
        val text = "${name.orEmpty()} ${groupOrCategory.orEmpty()}".lowercase()
        if (text.isBlank()) return false
        return ADULT_CONTENT_PATTERNS.any { pattern -> text.contains(pattern) }
    }

    /**
     * Parses an age rating string into an age ceiling (e.g. 0, 7, 12, 16, 18).
     * Returns null if unrated, unknown, or missing.
     */
    fun parseAgeRating(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        val clean = raw.trim().uppercase()
        return when {
            // General / All ages
            clean in listOf("G", "TV-G", "TV-Y", "U", "PARA TODOS", "AL", "TP") -> 0
            // Children 6-7
            clean in listOf("TV-Y7", "TV-Y7-FV", "6", "6+", "+6", "M/6", "M6") -> 6
            // Older children 8-10
            clean in listOf("PG", "TV-PG", "8", "8+", "+8", "10", "10+", "+10", "M/10", "M10") -> 10
            // Teens 12-13
            clean in listOf("PG-13", "PG13", "12", "12+", "+12", "13", "13+", "+13", "M/12", "M12") -> 12
            // Older teens 14-15
            clean in listOf("TV-14", "TV14", "14", "14+", "+14", "15", "15+", "+15", "M/14", "M14") -> 14
            // Young adults 16
            clean in listOf("16", "16+", "+16", "M/16", "M16") -> 16
            // Adults 18+
            clean in listOf("R", "NC-17", "NC17", "TV-MA", "TVMA", "18", "18+", "+18", "M/18", "M18", "XXX") -> 18
            // Unrated / not rated
            clean in listOf("NR", "UR", "UNRATED", "NOT RATED", "N/A", "NONE") -> null
            // Portuguese M/ prefix
            clean.startsWith("M/") -> clean.removePrefix("M/").trim().toIntOrNull()?.coerceIn(0, 18)
            clean.startsWith("M") && clean.drop(1).all { it.isDigit() } -> clean.drop(1).toIntOrNull()?.coerceIn(0, 18)
            else -> {
                // If the rating contains explicit digits, extract and parse them if 1-2 digits
                val digits = clean.filter { it.isDigit() }
                if (digits.isNotEmpty() && digits.length <= 2) {
                    digits.toIntOrNull()?.coerceIn(0, 18)
                } else null
            }
        }
    }

    /**
     * Determines whether content with a given age rating is permitted under profile restrictions.
     * Content ratings fail closed for children: unrated/unknown content is blocked when blockUnrated is true.
     */
    fun isContentAllowedForAge(
        ageRating: String?,
        maximumAge: Int?,
        blockUnrated: Boolean
    ): Boolean {
        if (maximumAge == null && !blockUnrated) return true
        val parsedAge = parseAgeRating(ageRating)
        if (parsedAge == null) {
            // Fail closed if unrated/unknown rating
            return !blockUnrated
        }
        if (maximumAge != null && parsedAge > maximumAge) {
            return false
        }
        return true
    }

    /**
     * Evaluates full content eligibility against profile restrictions.
     */
    fun isItemPermitted(
        name: String?,
        categoryOrGroup: String?,
        ageRating: String?,
        hideAdultContent: Boolean,
        maximumAge: Int?,
        blockUnrated: Boolean
    ): Boolean {
        if (hideAdultContent && isAdultPattern(name, categoryOrGroup)) {
            return false
        }
        return isContentAllowedForAge(ageRating, maximumAge, blockUnrated)
    }
}
