package com.fortresstv.player.source

import android.content.Context
import com.fortresstv.player.epg.EpgDiskCache

/** One-time incident cleanup, completed before any source can load or sync. Run on IO. */
internal object SourceIsolationCleanup {
    fun run(context: Context) {
        val settings = SourceSettingsStore(context)
        if (!settings.needsSourceIsolationCleanup()) return
        // Provider caches are replaceable. Keep library, progress and account storage untouched.
        EpgDiskCache.clear(context)
        for (directory in listOf(context.filesDir, context.cacheDir)) {
            directory.listFiles()?.filter {
                it.name.startsWith("parsed_playlist_") ||
                    it.name.startsWith("xtream_") ||
                    it.name == "cached_playlist.dat" || it.name == "cached_epg.dat"
            }?.forEach { file ->
                check(file.deleteRecursively() || !file.exists()) { "Could not clear source cache" }
            }
        }
        settings.clearNetworkSourcesForAccountIsolation()
    }
}
