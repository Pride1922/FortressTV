package com.fortresstv.player.library

/**
 * Answers "does any key end with ':' + value?" with one hash lookup instead of scanning every key.
 *
 * Re-attaching favourites after a catalogue load asks this once per catalogue item (tens of
 * thousands of movies), so the old `keys.any { it.endsWith(":$hash") }` cost catalogue size ×
 * library size. Keys added through [add] are indexed too, so the answers match a scan of the live
 * set even while it grows.
 */
internal class ColonSuffixIndex(private val keys: MutableSet<String>) {
    private val suffixes = HashSet<String>(keys.size * 2)

    init {
        keys.forEach(::index)
    }

    /** Adds [key] to the underlying set and to the index. */
    fun add(key: String) {
        if (keys.add(key)) index(key)
    }

    fun anyEndsWithColonAnd(value: String): Boolean =
        // The index stores the text after the last ':', so it only answers values without one.
        if (value.indexOf(':') >= 0) keys.any { it.endsWith(":$value") } else value in suffixes

    private fun index(key: String) {
        val colon = key.lastIndexOf(':')
        if (colon >= 0) suffixes += key.substring(colon + 1)
    }
}
