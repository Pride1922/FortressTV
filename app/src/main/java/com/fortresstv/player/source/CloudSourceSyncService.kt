package com.fortresstv.player.source

import android.content.Context
import com.fortresstv.player.account.AccountRepository
import com.fortresstv.player.account.AccountState
import com.fortresstv.player.account.HouseholdSourceRecord
import com.fortresstv.player.account.SupabaseSourceApi
import com.fortresstv.player.playlist.PlaylistProfile
import com.fortresstv.player.profile.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

class CloudSourceSyncService internal constructor(
    private val context: Context,
    private val settingsStore: SourceSettingsStore,
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,
    private val sourceApi: SupabaseSourceApi = SupabaseSourceApi()
) {
    companion object {
        @Volatile
        private var shared: CloudSourceSyncService? = null

        fun get(context: Context): CloudSourceSyncService = shared ?: synchronized(this) {
            shared ?: CloudSourceSyncService(
                context.applicationContext,
                SourceSettingsStore(context.applicationContext),
                AccountRepository.get(context),
                ProfileRepository.get(context)
            ).also { shared = it }
        }
    }

    private val mutex = Mutex()

    suspend fun syncSourcesToCloud(unlockToken: String?): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val accountState = accountRepository.resolveAccountState()
            if (accountState !is AccountState.AuthenticatedOnline) return@withLock false
            val token = unlockToken ?: profileRepository.currentUnlockToken()

            val playlists = settingsStore.getPlaylists()
            for (p in playlists) {
                if (p.source is ContentSource.Xtream) {
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
                    sourceApi.saveHouseholdSource(record, token, accountState.accessToken)
                }
            }

            val customEpg = settingsStore.epgSource()
            if (customEpg != null && customEpg is ContentSource.RemoteUrl) {
                val epgConfig = JSONObject().apply {
                    put("url", customEpg.value)
                }
                val epgRecord = HouseholdSourceRecord(
                    sourceType = "custom_epg",
                    displayName = customEpg.displayName,
                    enabled = true,
                    config = epgConfig
                )
                sourceApi.saveHouseholdSource(epgRecord, token, accountState.accessToken)
            }

            true
        }
    }

    suspend fun restoreSourcesFromCloud(unlockToken: String?): Result<List<PlaylistProfile>> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val accountState = accountRepository.resolveAccountState()
            if (accountState !is AccountState.AuthenticatedOnline) {
                return@withLock Result.failure(IllegalStateException("Not authenticated online"))
            }

            val token = unlockToken ?: profileRepository.currentUnlockToken()
            val remoteSources = try {
                sourceApi.getHouseholdSources(token, accountState.accessToken)
            } catch (e: Exception) {
                return@withLock Result.failure(e)
            }

            val currentLocalPlaylists = settingsStore.getPlaylists().toMutableList()
            val restoredPlaylists = mutableListOf<PlaylistProfile>()

            for (src in remoteSources) {
                if (src.sourceType == "xtream") {
                    val serverUrl = src.config.optString("server_url")
                    val username = src.config.optString("username")
                    val password = src.config.optString("password")

                    val contentSource = ContentSource.Xtream(
                        value = serverUrl,
                        displayName = src.displayName,
                        username = username,
                        password = password
                    )
                    val profile = PlaylistProfile(
                        id = src.id,
                        name = src.displayName,
                        source = contentSource,
                        enabled = src.enabled,
                        order = src.sourceOrder
                    )
                    val existingIndex = currentLocalPlaylists.indexOfFirst { it.id == src.id }
                    if (existingIndex >= 0) {
                        currentLocalPlaylists[existingIndex] = profile
                    } else {
                        currentLocalPlaylists.add(profile)
                    }
                    restoredPlaylists.add(profile)
                } else if (src.sourceType == "custom_epg") {
                    val url = src.config.optString("url")
                    if (url.isNotBlank()) {
                        val epgSource = ContentSource.RemoteUrl(value = url, displayName = src.displayName)
                        settingsStore.saveEpg(epgSource)
                    }
                }
            }

            settingsStore.replacePlaylists(currentLocalPlaylists)
            Result.success(restoredPlaylists)
        }
    }

    suspend fun deleteSourceFromCloud(sourceId: String, unlockToken: String?): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val accountState = accountRepository.resolveAccountState()
            if (accountState !is AccountState.AuthenticatedOnline) return@withLock false
            runCatching {
                sourceApi.deleteHouseholdSource(sourceId, unlockToken, accountState.accessToken)
            }.getOrDefault(false)
        }
    }
}
