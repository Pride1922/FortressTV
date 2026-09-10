package com.fortresstv.player.backup

import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fortresstv.player.library.UserLibraryStore
import com.fortresstv.player.library.VodProgressStore
import com.fortresstv.player.playlist.PlaylistProfile
import com.fortresstv.player.source.ContentSource
import com.fortresstv.player.source.SourceSettingsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class BackupReplacementTest {
    @Test
    fun replacementRemovesOldSourcesAndOrganizationAndSupportsEmptyBackup() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val prefix = "backup-test-${UUID.randomUUID()}-"
        val preferenceNames = mutableSetOf<String>()
        val context = object : ContextWrapper(base) {
            override fun getSharedPreferences(name: String, mode: Int): android.content.SharedPreferences {
                preferenceNames += prefix + name
                return base.getSharedPreferences(prefix + name, mode)
            }
        }
        try {
            val settings = SourceSettingsStore(context)
            val library = UserLibraryStore(context)
            val manager = BackupManager(settings, library, VodProgressStore(context))
            val old = PlaylistProfile("old", "Old", ContentSource.RemoteUrl("https://example.invalid/old", "Old"))
            val replacement = PlaylistProfile("new", "New", ContentSource.RemoteUrl("https://example.invalid/new", "New"))
            settings.savePlaylistProfile(old)
            library.hideAllCategories(old.id, setOf("Hidden"))
            library.setCategoryOrder(old.id, listOf("Hidden"))
            library.setFavoriteChannelIds(setOf("channel:old:test"))
            val backup = manager.createPayload().copy(
                playlists = listOf(replacement),
                library = BackupLibrary(emptySet(), emptySet(), emptySet(), emptySet(), emptyList(), emptyMap(), emptyMap())
            )
            assertTrue(manager.applyPayload(backup) is BackupRestoreResult.Success)
            assertEquals(listOf("new"), settings.getPlaylists().map { it.id })
            assertEquals("new", settings.getActivePlaylistId())
            assertTrue(library.favoriteChannelIds().isEmpty())
            assertTrue(library.hiddenCategories("old").isEmpty())
            assertTrue(library.categoryOrder("old").isEmpty())
            manager.applyPayload(backup.copy(playlists = emptyList()))
            assertTrue(settings.getPlaylists().isEmpty())
            assertEquals(null, settings.getActivePlaylistId())
        } finally {
            preferenceNames.forEach(base::deleteSharedPreferences)
        }
    }
}
