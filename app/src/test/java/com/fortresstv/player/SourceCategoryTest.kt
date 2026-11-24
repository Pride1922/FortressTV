package com.fortresstv.player

import android.content.SharedPreferences
import com.fortresstv.player.library.UserLibraryStore
import com.fortresstv.player.library.categoryCloudSourceId
import com.fortresstv.player.playlist.PlaylistChannel
import java.lang.reflect.Proxy
import org.junit.Assert.*
import org.junit.Test

class SourceCategoryTest {
    @Test fun sameNamesInTwoSourcesKeepIndependentVisibilityAcrossReloads() {
        val prefs = preferences()
        val store = UserLibraryStore({ prefs })
        store.saveCategoryCustomization("first", setOf("News"), listOf("News", "Sport"))
        store.saveCategoryCustomization("second", setOf("Sport"), listOf("Sport", "News"))
        val restored = UserLibraryStore({ prefs })
        val hidden = restored.scopedHiddenCategories(listOf("first", "second"))
        fun channel(source: String, name: String) = PlaylistChannel(position = 0, name = "Example", streamUrl = "https://example.invalid/live", sourceId = source, group = name)
        assertTrue(channel("first", "News").hasHiddenCategory(hidden))
        assertFalse(channel("second", "News").hasHiddenCategory(hidden))
        assertTrue(channel("second", "Sport").hasHiddenCategory(hidden))
        restored.saveCategoryCustomization("second", emptySet(), emptyList())
        assertEquals(setOf("News"), restored.hiddenCategories("first"))
        assertTrue(restored.hiddenCategories("second").isEmpty())
    }

    @Test fun olderCloudPullCannotOverwriteAnEditFromAnotherStoreInstance() {
        val prefs = preferences()
        val pullStore = UserLibraryStore({ prefs })
        val uiStore = UserLibraryStore({ prefs })
        val revision = pullStore.categoryRevision()
        uiStore.saveCategoryCustomization("second", setOf("News"), emptyList())
        assertFalse(pullStore.applyRemoteCategoryCustomization("second", emptySet(), emptyList(), revision))
        assertEquals(setOf("News"), uiStore.hiddenCategories("second"))
        assertTrue(pullStore.applyRemoteCategoryCustomization("second", setOf("Sport"), listOf("Sport"), pullStore.categoryRevision()))
        assertEquals(setOf("Sport"), uiStore.hiddenCategories("second"))
    }

    @Test fun identitiesRoundTripAndDefaultOrderingPreservesSourceOrder() {
        val a = sourceCategoryKey("a:1", "Z | Notícias")
        val b = sourceCategoryKey("a:1", "A")
        val c = sourceCategoryKey("other", "A")
        assertEquals("a:1" to "Z | Notícias", sourceCategoryParts(a))
        assertEquals(listOf(a, b, c), orderedSourceCategories(listOf(a, b, c), emptyList()))
        assertEquals(listOf(b, a, c), orderedSourceCategories(listOf(a, b, c), listOf(c, b, a)))
        val uuid = "00000000-0000-0000-0000-000000000001"
        assertEquals(uuid, categoryCloudSourceId(uuid))
        assertEquals(java.util.UUID.nameUUIDFromBytes("source_legacy".toByteArray()).toString(), categoryCloudSourceId("legacy"))
        assertNotEquals(categoryCloudSourceId("first"), categoryCloudSourceId("second"))
    }

    private fun preferences(): SharedPreferences {
        val values = mutableMapOf<String, Any?>()
        val editor = Proxy.newProxyInstance(SharedPreferences.Editor::class.java.classLoader, arrayOf(SharedPreferences.Editor::class.java)) { proxy, method, args ->
            when (method.name) {
                "putString", "putStringSet", "putLong" -> { values[args[0] as String] = args[1]; proxy }
                "apply" -> null
                else -> error(method.name)
            }
        } as SharedPreferences.Editor
        return Proxy.newProxyInstance(SharedPreferences::class.java.classLoader, arrayOf(SharedPreferences::class.java)) { _, method, args ->
            when (method.name) {
                "getString", "getStringSet", "getLong" -> values[args[0] as String] ?: args[1]
                "edit" -> editor
                else -> error(method.name)
            }
        } as SharedPreferences
    }
}
