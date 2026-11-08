package com.fortresstv.player.library

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.fortresstv.player.account.AccountRepository
import com.fortresstv.player.account.AccountState
import com.fortresstv.player.profile.ProfileRepository
import com.fortresstv.player.profile.ServerClockTracker
import com.fortresstv.player.telemetry.TelemetryStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

class CloudLibrarySyncService internal constructor(
    private val context: Context,
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,
    private val syncApi: SupabaseSyncApi = SupabaseSyncApi(),
    private var outbox: SyncOutboxStore = SyncOutboxStore(context, accountRepository.currentUserId() ?: "local"),
    private val telemetryStore: TelemetryStore = TelemetryStore.get(context)
) {
    companion object {
        @Volatile
        private var shared: CloudLibrarySyncService? = null

        fun get(context: Context): CloudLibrarySyncService = shared ?: synchronized(this) {
            shared ?: CloudLibrarySyncService(
                context.applicationContext,
                AccountRepository.get(context),
                ProfileRepository.get(context)
            ).also { shared = it }
        }
    }

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var progressDebounceJob: kotlinx.coroutines.Job? = null

    init {
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                triggerSync()
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                ) {
                    triggerSync()
                }
            }
        }
        runCatching {
            manager.registerDefaultNetworkCallback(callback)
        }
    }

    fun onAccountChanged(userId: String?) {
        outbox = SyncOutboxStore(context, userId ?: "local")
        if (userId != null) {
            triggerSync()
        }
    }

    suspend fun enqueueLibraryMutation(
        profileId: String,
        sourceId: String,
        itemKind: String,
        itemId: String,
        isFavourite: Boolean,
        isWatchlisted: Boolean,
        isWatched: Boolean,
        isDeleted: Boolean = false,
        clientTimeMs: Long = ServerClockTracker.currentEstimatedServerTimeMs()
    ) {
        val hashedId = CloudIdentityHasher.hashItemId(itemId)
        val payload = JSONObject().apply {
            put("is_favourite", isFavourite)
            put("is_watchlisted", isWatchlisted)
            put("is_watched", isWatched)
            put("client_updated_at_ms", clientTimeMs)
            if (isDeleted) put("deleted_at", java.time.Instant.now().toString())
        }
        outbox.enqueue(
            PendingSyncMutation(
                actionType = SyncActionType.UPSERT_LIBRARY,
                profileId = profileId,
                sourceId = sourceId,
                itemKind = itemKind,
                itemId = hashedId,
                payload = payload,
                clientUpdatedAtMs = clientTimeMs
            )
        )
    }

    fun recordLibraryMutation(
        profileId: String,
        sourceId: String,
        itemKind: String,
        itemId: String,
        isFavourite: Boolean,
        isWatchlisted: Boolean,
        isWatched: Boolean,
        isDeleted: Boolean = false
    ) {
        scope.launch {
            enqueueLibraryMutation(
                profileId = profileId,
                sourceId = sourceId,
                itemKind = itemKind,
                itemId = itemId,
                isFavourite = isFavourite,
                isWatchlisted = isWatchlisted,
                isWatched = isWatched,
                isDeleted = isDeleted
            )
            triggerSync()
        }
    }

    suspend fun enqueueProgressMutation(
        profileId: String,
        sourceId: String,
        itemKind: String,
        itemId: String,
        positionMs: Long,
        durationMs: Long,
        completed: Boolean,
        isDeleted: Boolean = false,
        clientTimeMs: Long = ServerClockTracker.currentEstimatedServerTimeMs()
    ) {
        val hashedId = CloudIdentityHasher.hashItemId(itemId)
        val payload = JSONObject().apply {
            put("position_ms", positionMs)
            put("duration_ms", durationMs)
            put("completed", completed)
            put("client_updated_at_ms", clientTimeMs)
            if (isDeleted) put("deleted_at", java.time.Instant.now().toString())
        }
        outbox.enqueue(
            PendingSyncMutation(
                actionType = SyncActionType.UPSERT_PROGRESS,
                profileId = profileId,
                sourceId = sourceId,
                itemKind = itemKind,
                itemId = hashedId,
                payload = payload,
                clientUpdatedAtMs = clientTimeMs
            )
        )
    }

    fun recordProgressMutation(
        profileId: String,
        sourceId: String,
        itemKind: String,
        itemId: String,
        positionMs: Long,
        durationMs: Long,
        completed: Boolean,
        isDeleted: Boolean = false
    ) {
        scope.launch {
            enqueueProgressMutation(
                profileId = profileId,
                sourceId = sourceId,
                itemKind = itemKind,
                itemId = itemId,
                positionMs = positionMs,
                durationMs = durationMs,
                completed = completed,
                isDeleted = isDeleted
            )
            progressDebounceJob?.cancel()
            progressDebounceJob = scope.launch {
                kotlinx.coroutines.delay(2_000)
                flushOutbox()
            }
        }
    }

    fun recordCategoriesMutation(
        profileId: String,
        sourceId: String,
        hiddenCategories: Set<String>,
        categoryOrder: List<String>
    ) {
        scope.launch {
            val payload = JSONObject().apply {
                put("hidden_categories", org.json.JSONArray(hiddenCategories.toList()))
                put("category_order", org.json.JSONArray(categoryOrder))
            }
            outbox.enqueue(
                PendingSyncMutation(
                    actionType = SyncActionType.UPSERT_CATEGORIES,
                    profileId = profileId,
                    sourceId = sourceId,
                    itemKind = "categories",
                    itemId = sourceId,
                    payload = payload
                )
            )
            triggerSync()
        }
    }

    fun recordPreferencesMutation(
        profileId: String,
        audioLang: String,
        subtitleLang: String,
        subFallback: Boolean,
        startupDestination: String = "home",
        epgTimeShiftHours: Int = 0
    ) {
        scope.launch {
            val payload = JSONObject().apply {
                put("audio_lang", audioLang)
                put("subtitle_lang", subtitleLang)
                put("sub_fallback", subFallback)
                put("startup_destination", startupDestination)
                put("epg_time_shift_hours", epgTimeShiftHours)
            }
            outbox.enqueue(
                PendingSyncMutation(
                    actionType = SyncActionType.UPSERT_PREFERENCES,
                    profileId = profileId,
                    sourceId = "prefs",
                    itemKind = "preferences",
                    itemId = "prefs",
                    payload = payload
                )
            )
            triggerSync()
        }
    }

    fun recordRecentChannelsMutation(
        profileId: String,
        recentChannels: List<String>
    ) {
        scope.launch {
            val payload = JSONObject().apply {
                put("recent_channels", org.json.JSONArray(recentChannels.take(20)))
            }
            outbox.enqueue(
                PendingSyncMutation(
                    actionType = SyncActionType.UPSERT_RECENT_CHANNELS,
                    profileId = profileId,
                    sourceId = "recent",
                    itemKind = "recent_channels",
                    itemId = "recent",
                    payload = payload
                )
            )
            triggerSync()
        }
    }

    fun triggerSync() {
        scope.launch {
            flushOutbox()
        }
    }

    private fun toValidUuid(sourceId: String): String = runCatching {
        java.util.UUID.fromString(sourceId).toString()
    }.getOrElse {
        java.util.UUID.nameUUIDFromBytes(sourceId.toByteArray(Charsets.UTF_8)).toString()
    }

    suspend fun flushOutbox(): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val accountState = accountRepository.resolveAccountState()
            if (accountState !is AccountState.AuthenticatedOnline) return@withLock false

            flushTelemetryInternal(accountState.accessToken)

            val sessionToken = profileRepository.getOrOpenActiveSessionToken() ?: return@withLock false

            val pending = outbox.loadPending()
            if (pending.isEmpty()) return@withLock true

            for (mutation in pending) {
                val success = runCatching {
                    when (mutation.actionType) {
                        SyncActionType.UPSERT_LIBRARY -> {
                            val validSourceUuid = toValidUuid(mutation.sourceId)
                            val item = CloudLibraryItem(
                                householdId = accountState.userId,
                                profileId = mutation.profileId,
                                sourceId = validSourceUuid,
                                itemKind = mutation.itemKind,
                                itemId = mutation.itemId,
                                isFavourite = mutation.payload.optBoolean("is_favourite", false),
                                isWatchlisted = mutation.payload.optBoolean("is_watchlisted", false),
                                isWatched = mutation.payload.optBoolean("is_watched", false),
                                clientUpdatedAtMs = mutation.clientUpdatedAtMs,
                                deletedAt = mutation.payload.optString("deleted_at").takeIf(String::isNotBlank)
                            )
                            syncApi.upsertProfileLibrary(listOf(item), sessionToken, accountState.accessToken)
                        }
                        SyncActionType.UPSERT_PROGRESS -> {
                            val validSourceUuid = toValidUuid(mutation.sourceId)
                            val item = CloudWatchProgressItem(
                                householdId = accountState.userId,
                                profileId = mutation.profileId,
                                sourceId = validSourceUuid,
                                itemKind = mutation.itemKind,
                                itemId = mutation.itemId,
                                positionMs = mutation.payload.optLong("position_ms", 0L),
                                durationMs = mutation.payload.optLong("duration_ms", 0L),
                                completed = mutation.payload.optBoolean("completed", false),
                                clientUpdatedAtMs = mutation.clientUpdatedAtMs,
                                deletedAt = mutation.payload.optString("deleted_at").takeIf(String::isNotBlank)
                            )
                            syncApi.upsertProfileWatchProgress(listOf(item), sessionToken, accountState.accessToken)
                        }
                        SyncActionType.UPSERT_CATEGORIES -> {
                            val hiddenArr = mutation.payload.optJSONArray("hidden_categories") ?: org.json.JSONArray()
                            val hiddenSet = mutableSetOf<String>()
                            for (i in 0 until hiddenArr.length()) hiddenSet.add(hiddenArr.getString(i))
                            val orderArr = mutation.payload.optJSONArray("category_order") ?: org.json.JSONArray()
                            val orderList = mutableListOf<String>()
                            for (i in 0 until orderArr.length()) orderList.add(orderArr.getString(i))

                            val validSourceUuid = runCatching { java.util.UUID.fromString(mutation.sourceId).toString() }.getOrNull()
                                ?: java.util.UUID.nameUUIDFromBytes("source_${mutation.sourceId}".toByteArray()).toString()
                            syncApi.saveProfileCategories(
                                profileId = mutation.profileId,
                                sourceId = validSourceUuid,
                                hiddenCategories = hiddenSet,
                                categoryOrder = orderList,
                                accessToken = accountState.accessToken
                            )
                        }
                        SyncActionType.UPSERT_PREFERENCES -> {
                            syncApi.saveProfilePreferences(
                                profileId = mutation.profileId,
                                audioLang = mutation.payload.optString("audio_lang", "default"),
                                subtitleLang = mutation.payload.optString("subtitle_lang", "off"),
                                subFallback = mutation.payload.optBoolean("sub_fallback", true),
                                startupDestination = mutation.payload.optString("startup_destination", "home"),
                                epgTimeShiftHours = mutation.payload.optInt("epg_time_shift_hours", 0),
                                accessToken = accountState.accessToken
                            )
                        }
                        SyncActionType.UPSERT_RECENT_CHANNELS -> {
                            val channelsArr = mutation.payload.optJSONArray("recent_channels") ?: org.json.JSONArray()
                            val channels = mutableListOf<String>()
                            for (i in 0 until channelsArr.length()) channels.add(channelsArr.getString(i))
                            syncApi.saveProfileRecentChannels(
                                profileId = mutation.profileId,
                                recentChannels = channels,
                                accessToken = accountState.accessToken
                            )
                        }
                    }
                    true
                }.getOrDefault(false)

                if (success) {
                    outbox.remove(mutation.id)
                } else {
                    return@withLock false
                }
            }
            flushTelemetryInternal(accountState.accessToken)
            true
        }
    }

    private fun flushTelemetryInternal(accessToken: String) {
        val batch = telemetryStore.pendingBatch(50)
        if (batch.isNotEmpty()) {
            val acknowledged = syncApi.uploadPlaybackHealthSummaries(batch, accessToken)
            if (acknowledged.isNotEmpty()) {
                telemetryStore.acknowledge(acknowledged)
            }
        }
        val aggregates = syncApi.fetchChannelHealthAggregates(accessToken)
        if (aggregates.isNotEmpty()) {
            com.fortresstv.player.telemetry.ChannelHealthRepository.get(context).setAggregateSnapshots(aggregates)
        }
    }

    suspend fun flushTelemetry(): Unit = withContext(Dispatchers.IO) {
        val accountState = accountRepository.resolveAccountState()
        if (accountState is AccountState.AuthenticatedOnline) {
            flushTelemetryInternal(accountState.accessToken)
        }
    }

    suspend fun pullRemoteLibrary(profileId: String): List<CloudLibraryItem> = withContext(Dispatchers.IO) {
        val accountState = accountRepository.resolveAccountState()
        if (accountState !is AccountState.AuthenticatedOnline) return@withContext emptyList()
        val sessionToken = profileRepository.getOrOpenActiveSessionToken() ?: return@withContext emptyList()
        runCatching {
            syncApi.fetchAllProfileLibrary(profileId, sessionToken, accountState.accessToken)
        }.getOrDefault(emptyList())
    }

    suspend fun pullRemoteProgress(profileId: String): List<CloudWatchProgressItem> = withContext(Dispatchers.IO) {
        val accountState = accountRepository.resolveAccountState()
        if (accountState !is AccountState.AuthenticatedOnline) return@withContext emptyList()
        val sessionToken = profileRepository.getOrOpenActiveSessionToken() ?: return@withContext emptyList()
        runCatching {
            syncApi.fetchAllProfileWatchProgress(profileId, sessionToken, accountState.accessToken)
        }.getOrDefault(emptyList())
    }

    private fun removeMatching(set: MutableSet<String>, kind: String, sourceId: String, hashedItemId: String) {
        set.removeAll { entry ->
            val parts = entry.split(":")
            if (parts.size >= 3 && parts[0] == kind && parts[1] == sourceId) {
                parts[2] == hashedItemId || CloudIdentityHasher.hashItemId(parts[2]) == hashedItemId
            } else {
                false
            }
        }
    }

    suspend fun pullRemoteCategories(profileId: String): List<CloudCategoryCustomization> = withContext(Dispatchers.IO) {
        val accountState = accountRepository.resolveAccountState()
        if (accountState !is AccountState.AuthenticatedOnline) return@withContext emptyList()
        runCatching {
            syncApi.getProfileCategories(profileId, accountState.accessToken)
        }.getOrDefault(emptyList())
    }

    suspend fun pullRemotePreferences(profileId: String): CloudProfilePreferences? = withContext(Dispatchers.IO) {
        val accountState = accountRepository.resolveAccountState()
        if (accountState !is AccountState.AuthenticatedOnline) return@withContext null
        runCatching {
            syncApi.getProfilePreferences(profileId, accountState.accessToken)
        }.getOrNull()
    }

    suspend fun pullRemoteRecentChannels(profileId: String): List<String> = withContext(Dispatchers.IO) {
        val accountState = accountRepository.resolveAccountState()
        if (accountState !is AccountState.AuthenticatedOnline) return@withContext emptyList()
        runCatching {
            syncApi.getProfileRecentChannels(profileId, accountState.accessToken)
        }.getOrDefault(emptyList())
    }

    fun clearRecentChannels(profileId: String) {
        scope.launch {
            val accountState = accountRepository.resolveAccountState()
            if (accountState is AccountState.AuthenticatedOnline) {
                runCatching {
                    syncApi.clearProfileRecentChannels(profileId, accountState.accessToken)
                }
            }
        }
    }

    suspend fun pullAndApplyProfileData(
        profileId: String,
        userLibrary: UserLibraryStore,
        progressStore: VodProgressStore,
        settingsStore: com.fortresstv.player.source.SourceSettingsStore? = null,
        onUpdated: (() -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        coroutineContext.ensureActive()
        val remoteLibrary = pullRemoteLibrary(profileId)
        coroutineContext.ensureActive()
        val remoteProgress = pullRemoteProgress(profileId)
        coroutineContext.ensureActive()
        val remoteCategories = pullRemoteCategories(profileId)
        coroutineContext.ensureActive()
        val remoteRecent = pullRemoteRecentChannels(profileId)
        coroutineContext.ensureActive()
        val remotePrefs = if (settingsStore != null) pullRemotePreferences(profileId) else null
        coroutineContext.ensureActive()

        if (remoteCategories.isNotEmpty()) {
            for (custom in remoteCategories) {
                userLibrary.setHiddenCategories(custom.sourceId, custom.hiddenCategories)
                userLibrary.setCategoryOrder(custom.sourceId, custom.categoryOrder)
            }
        }

        if (remoteRecent.isNotEmpty()) {
            userLibrary.setRecentChannelIds(remoteRecent)
        }

        if (remotePrefs != null && settingsStore != null) {
            settingsStore.setPreferredAudioLanguage(remotePrefs.preferredAudioLanguage, profileId)
            settingsStore.setPreferredSubtitleLanguage(remotePrefs.preferredSubtitleLanguage, profileId)
            settingsStore.setSubtitleFallbackEnglish(remotePrefs.subtitleFallbackEnglish, profileId)
            settingsStore.setStartupDestination(
                com.fortresstv.player.source.StartupDestination.fromPersisted(remotePrefs.startupDestination),
                profileId
            )
            settingsStore.saveEpgTimeShiftHours(remotePrefs.epgTimeShiftHours, profileId)
        }

        if (remoteLibrary.isNotEmpty()) {
            val favs = userLibrary.favoriteChannelIds().toMutableSet()
            val watchlist = userLibrary.movieWatchlistIds().toMutableSet()
            val followed = userLibrary.followedSeriesIds().toMutableSet()
            val watched = userLibrary.watchedItemIds().toMutableSet()

            for (item in remoteLibrary) {
                val key = "${item.itemKind}:${item.sourceId}:${item.itemId}"
                val isDeleted = item.deletedAt != null
                when (item.itemKind) {
                    "channel" -> {
                        removeMatching(favs, "channel", item.sourceId, item.itemId)
                        if (!isDeleted && item.isFavourite) {
                            favs.add(key)
                        }
                    }
                    "movie" -> {
                        removeMatching(watchlist, "movie", item.sourceId, item.itemId)
                        if (!isDeleted && (item.isWatchlisted || item.isFavourite)) {
                            watchlist.add(key)
                        }

                        removeMatching(watched, "movie", item.sourceId, item.itemId)
                        if (!isDeleted && item.isWatched) {
                            watched.add(key)
                        }
                    }
                    "series" -> {
                        removeMatching(followed, "series", item.sourceId, item.itemId)
                        if (!isDeleted && (item.isWatchlisted || item.isFavourite)) {
                            followed.add(key)
                        }
                    }
                    "episode" -> {
                        removeMatching(watched, "episode", item.sourceId, item.itemId)
                        if (!isDeleted && item.isWatched) {
                            watched.add(key)
                        }
                    }
                }
            }
            userLibrary.setFavoriteChannelIds(favs)
            userLibrary.setMovieWatchlistIds(watchlist)
            userLibrary.setFollowedSeriesIds(followed)
            userLibrary.setWatchedItemIds(watched)
        }

        if (remoteProgress.isNotEmpty()) {
            val entries = progressStore.entries().toMutableList()
            for (prog in remoteProgress) {
                val key = "${prog.itemKind}:${prog.sourceId}:${prog.itemId}"
                val kind = VodProgressKind.fromWireValue(prog.itemKind)
                if (prog.deletedAt != null) {
                    entries.removeAll {
                        it.key == key || (it.sourceId == prog.sourceId && (it.itemId == prog.itemId || CloudIdentityHasher.hashItemId(it.itemId) == prog.itemId))
                    }
                } else {
                    val existingIndex = entries.indexOfFirst {
                        it.key == key || (it.sourceId == prog.sourceId && (it.itemId == prog.itemId || CloudIdentityHasher.hashItemId(it.itemId) == prog.itemId))
                    }
                    val entry = VodProgressEntry(
                        key = key,
                        kind = kind,
                        sourceId = prog.sourceId,
                        itemId = prog.itemId,
                        seriesId = null,
                        title = entries.getOrNull(existingIndex)?.title.orEmpty(),
                        posterUrl = entries.getOrNull(existingIndex)?.posterUrl,
                        containerExtension = entries.getOrNull(existingIndex)?.containerExtension ?: "mp4",
                        season = entries.getOrNull(existingIndex)?.season,
                        episode = entries.getOrNull(existingIndex)?.episode,
                        positionMs = prog.positionMs,
                        durationMs = prog.durationMs,
                        updatedAtMs = prog.clientUpdatedAtMs
                    )
                    if (existingIndex >= 0) {
                        entries[existingIndex] = entry
                    } else if (prog.positionMs > 0) {
                        entries.add(entry)
                    }
                }
            }
            progressStore.setAll(entries)
        }

        withContext(Dispatchers.Main) {
            onUpdated?.invoke()
        }
    }
}
