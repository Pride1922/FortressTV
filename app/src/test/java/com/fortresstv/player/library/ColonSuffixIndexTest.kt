package com.fortresstv.player.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ColonSuffixIndexTest {
    @Test
    fun answersLikeScanningEveryKey() {
        val random = Random(42)
        val parts = listOf("movie", "series", "channel", "src", "a1", "b2", "ff", "00", "", "x:y")
        fun piece() = parts.random(random)

        repeat(500) {
            val keys = LinkedHashSet<String>()
            repeat(random.nextInt(0, 40)) {
                keys += when (random.nextInt(4)) {
                    0 -> piece() // no colon at all
                    1 -> "${piece()}:${piece()}"
                    else -> "${piece()}:${piece()}:${piece()}"
                }
            }
            val scanned = LinkedHashSet(keys)
            val index = ColonSuffixIndex(keys)

            repeat(60) {
                val value = if (random.nextInt(10) == 0) "${piece()}:${piece()}" else piece()
                val expected = scanned.any { it.endsWith(":$value") }
                assertEquals("value <$value> in $scanned", expected, index.anyEndsWithColonAnd(value))
                // Grow the set during the loop, as the favourite re-attachment does.
                if (random.nextBoolean()) {
                    val added = "${piece()}:${piece()}"
                    scanned += added
                    index.add(added)
                }
            }
            assertEquals(scanned, keys)
        }
    }

    @Test
    fun keyWithoutColonDoesNotMatchItsWholeText() {
        val index = ColonSuffixIndex(mutableSetOf("abc"))
        assertFalse(index.anyEndsWithColonAnd("abc"))
        assertTrue(ColonSuffixIndex(mutableSetOf("movie:src:abc")).anyEndsWithColonAnd("abc"))
    }
}
