package com.fortresstv.player.account

import android.content.Context
import com.fortresstv.player.library.CloudLibrarySyncService
import com.fortresstv.player.library.UserLibraryStore
import com.fortresstv.player.library.VodProgressKind
import com.fortresstv.player.library.VodProgressStore
import com.fortresstv.player.profile.ProfileRepository
import com.fortresstv.player.source.CloudSourceSyncService
import com.fortresstv.player.source.ContentSource
import com.fortresstv.player.source.SourceSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal class LocalDataMigrationService(
    private val context: Context,
    private val settingsStore: SourceSettingsStore = SourceSettingsStore(context),
    private val accountRepository: AccountRepository = AccountRepository.get(context),
    private val profileRepository: ProfileRepository = ProfileRepository.get(context),
    private val sourceSyncService: CloudSourceSyncService = CloudSourceSyncService.get(context),
    private val librarySyncService: CloudLibrarySyncService = CloudLibrarySyncService.get(context),
    private val sourceApi: SupabaseSourceApi = SupabaseSourceApi()
) {
    private val prefs = context.getSharedPreferences("fortress_data_migrations", Context.MODE_PRIVATE)

    fun isMigrationNeeded(householdId: String): Boolean {
        if (prefs.getBoolean("is_migrated_$householdId", false)) return false

        // If the household already has existing cloud profiles, cloud is the source of truth (Netflix-style)
        if (profileRepository.profiles.value.isNotEmpty()) {
            skipMigration(householdId)
            return false
        }

        val localPlaylists = settingsStore.getPlaylists()
        val localLibrary = UserLibraryStore(context, "local")
        val localProgress = VodProgressStore(context, "local")

        val hasLocalSources = localPlaylists.isNotEmpty()
        val hasLocalLibrary = localLibrary.favoriteChannelIds().isNotEmpty() ||
            localLibrary.movieWatchlistIds().isNotEmpty() ||
            localLibrary.followedSeriesIds().isNotEmpty() ||
            localLibrary.watchedItemIds().isNotEmpty()
        val hasLocalProgress = localProgress.entries().isNotEmpty()

        return hasLocalSources || hasLocalLibrary || hasLocalProgress
    }

    suspend fun importToAdultProfile(
        targetAdultProfileId: String,
        unlockToken: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val accountState = accountRepository.resolveAccountState()
        if (accountState !is AccountState.AuthenticatedOnline) {
            return@withContext Result.failure(IllegalStateException("Not authenticated online"))
        }

        val householdId = accountState.userId
        val profiles = profileRepository.profiles.value
        val targetProfile = profiles.firstOrNull { it.id == targetAdultProfileId }
        if (targetProfile == null || targetProfile.isChild) {
            return@withContext Result.failure(IllegalArgumentException("Target profile must be an existing adult profile"))
        }

        val token = unlockToken ?: profileRepository.currentUnlockToken()

        // 1. Restore/check existing cloud sources to prevent duplicates
        val remoteSourcesResult = sourceSyncService.restoreSourcesFromCloud(token)
        val remoteSources = remoteSourcesResult.getOrElse {
            return@withContext Result.failure(it)
        }

        val localPlaylists = settingsStore.getPlaylists()
        for (p in localPlaylists) {
            if (p.source is ContentSource.Xtream) {
                val existsInCloud = remoteSources.any { r ->
                    r.source is ContentSource.Xtream &&
                        r.source.value.trimEnd('/') == p.source.value.trimEnd('/') &&
                        r.source.username == p.source.username
                }
                if (!existsInCloud) {
                    val config = JSONObject().apply {
                        put("server_url", p.source.value)
                        put("username", p.source.username)
                        put("password", p.source.password)
                    }
                    val record = HouseholdSourceRecord(
                        id = p.id,
                        sourceType = "xtream",
                        displayName = p.name,
                        enabled = p.enabled,
                        sourceOrder = p.order,
                        config = config
                    )
                    try {
                        sourceApi.saveHouseholdSource(record, token, accountState.accessToken)
                    } catch (e: Exception) {
                        return@withContext Result.failure(e)
                    }
                }
            }
        }

        // 2. Import local library and progress into target adult profile
        val localLibrary = UserLibraryStore(context, "local")
        val localProgress = VodProgressStore(context, "local")

        val targetLibrary = UserLibraryStore(context, "${householdId}_$targetAdultProfileId")
        val targetProgress = VodProgressStore(context, "${householdId}_$targetAdultProfileId")

        // Channels
        val favChannels = localLibrary.favoriteChannelIds()
        val targetFavChannels = targetLibrary.favoriteChannelIds().toMutableSet()
        for (key in favChannels) {
            targetFavChannels.add(key)
            val parts = key.split(":")
            if (parts.size >= 3) {
                librarySyncService.enqueueLibraryMutation(
                    profileId = targetAdultProfileId,
                    sourceId = parts[1],
                    itemKind = "channel",
                    itemId = parts[2],
                    isFavourite = true,
                    isWatchlisted = false,
                    isWatched = false
                )
            }
        }
        targetLibrary.setFavoriteChannelIds(targetFavChannels)

        // Movie watchlist
        val movieWatchlist = localLibrary.movieWatchlistIds()
        val targetMovieWatchlist = targetLibrary.movieWatchlistIds().toMutableSet()
        for (key in movieWatchlist) {
            targetMovieWatchlist.add(key)
            val parts = key.split(":")
            if (parts.size >= 3) {
                librarySyncService.enqueueLibraryMutation(
                    profileId = targetAdultProfileId,
                    sourceId = parts[1],
                    itemKind = "movie",
                    itemId = parts[2],
                    isFavourite = true,
                    isWatchlisted = true,
                    isWatched = key in localLibrary.watchedItemIds()
                )
            }
        }
        targetLibrary.setMovieWatchlistIds(targetMovieWatchlist)

        // Followed series
        val followedSeries = localLibrary.followedSeriesIds()
        val targetFollowedSeries = targetLibrary.followedSeriesIds().toMutableSet()
        for (key in followedSeries) {
            targetFollowedSeries.add(key)
            val parts = key.split(":")
            if (parts.size >= 3) {
                librarySyncService.enqueueLibraryMutation(
                    profileId = targetAdultProfileId,
                    sourceId = parts[1],
                    itemKind = "series",
                    itemId = parts[2],
                    isFavourite = true,
                    isWatchlisted = true,
                    isWatched = false
                )
            }
        }
        targetLibrary.setFollowedSeriesIds(targetFollowedSeries)

        // Watched items
        val watchedItems = localLibrary.watchedItemIds()
        val targetWatchedItems = targetLibrary.watchedItemIds().toMutableSet()
        for (key in watchedItems) {
            targetWatchedItems.add(key)
            val parts = key.split(":")
            if (parts.size >= 3) {
                librarySyncService.enqueueLibraryMutation(
                    profileId = targetAdultProfileId,
                    sourceId = parts[1],
                    itemKind = parts[0],
                    itemId = parts[2],
                    isFavourite = false,
                    isWatchlisted = key in movieWatchlist || key in followedSeries,
                    isWatched = true
                )
            }
        }
        targetLibrary.setWatchedItemIds(targetWatchedItems)

        // Progress entries
        val progressEntries = localProgress.entries()
        for (entry in progressEntries) {
            targetProgress.save(entry)
            librarySyncService.enqueueProgressMutation(
                profileId = targetAdultProfileId,
                sourceId = entry.sourceId,
                itemKind = if (entry.kind == VodProgressKind.MOVIE) "movie" else "episode",
                itemId = entry.itemId,
                positionMs = entry.positionMs,
                durationMs = entry.durationMs,
                completed = entry.durationMs > 0 && entry.positionMs >= entry.durationMs * 0.95
            )
        }

        // 3. Flush outbox
        val flushed = librarySyncService.flushOutbox()
        if (!flushed) {
            return@withContext Result.failure(IllegalStateException("Failed to sync imported data to cloud"))
        }

        // 4. Mark durable completion
        prefs.edit().putBoolean("is_migrated_$householdId", true).apply()
        Result.success(Unit)
    }

    fun skipMigration(householdId: String) {
        prefs.edit().putBoolean("is_migrated_$householdId", true).apply()
    }
}
