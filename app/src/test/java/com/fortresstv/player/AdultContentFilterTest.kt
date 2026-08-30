package com.fortresstv.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdultContentFilterTest {

    @Test
    fun detectsAdultContentAccurately() {
        assertTrue(isAdultContent("Channel XXX HD", "Entertainment"))
        assertTrue(isAdultContent("Venus TV", "Adults"))
        assertTrue(isAdultContent("Penthouse Gold", null))
        assertTrue(isAdultContent("Regular Show", "XXX Adultos"))
        assertTrue(isAdultContent("Movie 18+", "Cinema"))
        assertTrue(isAdultContent("Special +18", "Live"))
        assertTrue(isAdultContent("Brazzers TV", "General"))
        assertTrue(isAdultContent("Redlight HD", null))
        assertTrue(isAdultContent("Dorcel TV", "Movies"))
        assertTrue(isAdultContent("Secret", "Erotico"))
        assertTrue(isAdultContent("Late Night", "Erotic"))

        assertFalse(isAdultContent("BBC News", "News"))
        assertFalse(isAdultContent("Discovery Channel", "Documentaries"))
        assertFalse(isAdultContent("Fox Sports 1", "Sports"))
        assertFalse(isAdultContent("Cartoon Network", "Kids"))
        assertFalse(isAdultContent(null, null))
    }
}
