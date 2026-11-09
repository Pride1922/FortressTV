package com.fortresstv.player.omdb

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class OmdbMetadataResolverTest {

    @Test
    fun buildCacheKey_isConsistentAndCaseInsensitive() {
        val key1 = OmdbMetadataResolver.buildCacheKey("movie", "The Mentalist", "2008", null)
        val key2 = OmdbMetadataResolver.buildCacheKey("movie", "the mentalist", "2008", null)
        assertEquals(key1, key2)

        val key3 = OmdbMetadataResolver.buildCacheKey("series", "the mentalist", "2008", null)
        assertNotEquals(key1, key3)
    }

    @Test
    fun clearMemoryCache_executesCleanly() {
        OmdbMetadataResolver.clearMemoryCache()
    }
}
