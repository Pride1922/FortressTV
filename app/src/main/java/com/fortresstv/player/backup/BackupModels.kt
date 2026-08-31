package com.fortresstv.player.backup

import com.fortresstv.player.playlist.PlaylistProfile
import com.fortresstv.player.source.ContentSource

data class BackupSettings(
    val language: String,
    val playChannelsFullscreen: Boolean,
    val autoPictureInPicture: Boolean,
    val hideAdultContent: Boolean,
    val parentalControlEnabled: Boolean,
    val parentalPin: String?,
    val epgSource: ContentSource?,
    val epgTimeShiftHours: Int,
    val activePlaylistId: String?,
    val preferredAudioLanguage: String = "default",
    val preferredSubtitleLanguage: String = "off",
    val subtitleFallbackEnglish: Boolean = true
)

data class BackupLibrary(
    val favoriteChannelIds: Set<String>,
    val movieWatchlistIds: Set<String>,
    val followedSeriesIds: Set<String>,
    val watchedItemIds: Set<String>,
    val recentChannelIds: List<String>,
    val hiddenCategoriesByProfile: Map<String, Set<String>>,
    val categoryOrderByProfile: Map<String, List<String>>
)

data class BackupPayload(
    val version: Int = CURRENT_VERSION,
    val createdAt: Long = System.currentTimeMillis(),
    val appVersion: String,
    val settings: BackupSettings,
    val playlists: List<PlaylistProfile>,
    val library: BackupLibrary,
    val vodProgress: List<com.fortresstv.player.library.VodProgressEntry>
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

sealed interface BackupExportResult {
    data class Success(val byteCount: Int) : BackupExportResult
    data class Error(val message: String, val cause: Throwable? = null) : BackupExportResult
}

sealed interface BackupRestoreResult {
    data class Success(
        val restoredPlaylistsCount: Int,
        val restoredFavoritesCount: Int,
        val restoredVodProgressCount: Int
    ) : BackupRestoreResult
    data class InvalidPassword(val message: String = "Incorrect backup password") : BackupRestoreResult
    data class InvalidFormat(val message: String = "Invalid or corrupted backup file") : BackupRestoreResult
    data class Error(val message: String, val cause: Throwable? = null) : BackupRestoreResult
}
