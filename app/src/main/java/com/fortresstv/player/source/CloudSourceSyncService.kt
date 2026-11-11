package com.fortresstv.player.source

import android.content.Context
import com.fortresstv.player.account.AccountRepository
import com.fortresstv.player.account.AccountState
import com.fortresstv.player.account.HouseholdSourceRecord
import com.fortresstv.player.account.SupabaseSourceApi
import com.fortresstv.player.playlist.PlaylistProfile
import com.fortresstv.player.profile.ProfileRepository
import com.fortresstv.player.subtitles.OpenSubtitlesSettings
import java.util.UUID
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

    suspend fun syncSourcesToCloud(unlockToken: String?): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val accountState = accountRepository.resolveAccountState()
            android.util.Log.i("CloudSourceSync", "syncSourcesToCloud: accountState is ${accountState::class.simpleName}, token=$unlockToken")
            if (accountState !is AccountState.AuthenticatedOnline) {
                return@withLock Result.failure(IllegalStateException("Not authenticated online (${accountState::class.simpleName})"))
            }
            val token = unlockToken ?: profileRepository.currentUnlockToken()

            val playlists = settingsStore.getPlaylists()
            android.util.Log.i("CloudSourceSync", "Syncing ${playlists.size} playlists to cloud with token=$token")
            var adultAuthFailure: Throwable? = null
            for (p in playlists) {
                if (p.source is ContentSource.Xtream) {
                    val config = JSONObject().apply {
                        put("server_url", p.source.value)
                        put("username", p.source.username)
                        put("password", p.source.password)
                    }
                    val validId = runCatching { UUID.fromString(p.id).toString() }.getOrNull()
                        ?: UUID.nameUUIDFromBytes("source_${p.id}".toByteArray()).toString()
                    val record = HouseholdSourceRecord(
                        id = validId,
                        sourceType = "xtream",
                        displayName = p.name,
                        enabled = p.enabled,
                        sourceOrder = p.order,
                        config = config
                    )
                    runCatching {
                        sourceApi.saveHouseholdSource(record, token, accountState.accessToken)
                    }.onFailure { err ->
                        android.util.Log.w("CloudSourceSync", "Failed to sync Xtream source ${p.name} to cloud", err)
                        if (err.message?.contains("Adult authorization required", ignoreCase = true) == true) {
                            adultAuthFailure = err
                        }
                    }
                }
            }

            val customEpg = settingsStore.epgSource()
            if (customEpg != null && customEpg is ContentSource.RemoteUrl) {
                val epgConfig = JSONObject().apply {
                    put("url", customEpg.value)
                    put("correction_minutes", settingsStore.epgScheduleCorrectionMinutes())
                    put("source_timezone", settingsStore.epgSourceTimezone())
                }
                val epgId = UUID.nameUUIDFromBytes("custom_epg_${customEpg.value.trim()}".toByteArray()).toString()
                val epgRecord = HouseholdSourceRecord(
                    id = epgId,
                    sourceType = "custom_epg",
                    displayName = customEpg.displayName,
                    enabled = true,
                    config = epgConfig
                )
                runCatching {
                    sourceApi.saveHouseholdSource(epgRecord, token, accountState.accessToken)
                }.onFailure { err ->
                    android.util.Log.w("CloudSourceSync", "Failed to sync custom EPG to cloud", err)
                    if (err.message?.contains("Adult authorization required", ignoreCase = true) == true) {
                        adultAuthFailure = err
                    }
                }
            }

            if (adultAuthFailure != null) {
                Result.failure(adultAuthFailure!!)
            } else {
                Result.success(Unit)
            }
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
                        val correction = src.config.optInt("correction_minutes", 0)
                        settingsStore.saveEpgScheduleCorrectionMinutes(correction)
                        val tz = src.config.optString("source_timezone", "")
                        if (tz.isNotBlank()) {
                            settingsStore.saveEpgSourceTimezone(tz)
                        }
                    }
                }
            }

            settingsStore.replacePlaylists(currentLocalPlaylists)
            Result.success(restoredPlaylists)
        }
    }

    suspend fun syncOpenSubtitlesToCloud(
        settings: OpenSubtitlesSettings,
        unlockToken: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val accountState = accountRepository.resolveAccountState()
            if (accountState !is AccountState.AuthenticatedOnline) {
                return@withLock Result.failure(IllegalStateException("Not authenticated"))
            }
            val token = unlockToken ?: profileRepository.currentUnlockToken()
            runCatching {
                val ok = sourceApi.saveOpenSubtitles(settings, token, accountState.accessToken)
                if (!ok) error("Failed to save OpenSubtitles credentials")
            }
        }
    }

    suspend fun restoreOpenSubtitlesFromCloud(unlockToken: String? = null): OpenSubtitlesSettings? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val accountState = accountRepository.resolveAccountState()
            if (accountState !is AccountState.AuthenticatedOnline) return@withLock null
            val token = unlockToken ?: profileRepository.currentUnlockToken()
            runCatching {
                sourceApi.getOpenSubtitles(token, accountState.accessToken)
            }.getOrNull()
        }
    }

    suspend fun deleteOpenSubtitlesFromCloud(unlockToken: String? = null): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val accountState = accountRepository.resolveAccountState()
            if (accountState !is AccountState.AuthenticatedOnline) return@withLock false
            val token = unlockToken ?: profileRepository.currentUnlockToken()
            runCatching {
                sourceApi.deleteOpenSubtitles(token, accountState.accessToken)
            }.getOrDefault(false)
        }
    }

    suspend fun syncTmdbToCloud(
        settings: com.fortresstv.player.tmdb.TmdbSettings,
        unlockToken: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val accountState = accountRepository.resolveAccountState()
            if (accountState !is AccountState.AuthenticatedOnline) {
                return@withLock Result.failure(IllegalStateException("Not authenticated"))
            }
            val token = unlockToken ?: profileRepository.currentUnlockToken()
            runCatching {
                val ok = sourceApi.saveTmdb(settings, token, accountState.accessToken)
                if (!ok) error("Failed to save TMDB settings")
            }
        }
    }

    suspend fun reconcileTmdb(local: com.fortresstv.player.tmdb.TmdbSettings): com.fortresstv.player.tmdb.TmdbSettings? =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val account = accountRepository.resolveAccountState()
                if (account !is AccountState.AuthenticatedOnline) return@withLock null
                val token = profileRepository.currentUnlockToken()
                runCatching {
                    val remote = sourceApi.getTmdb(token, account.accessToken)
                    if (remote?.apiKey.isNullOrBlank() && local.apiKey.isNotBlank()) {
                        check(sourceApi.saveTmdb(local, token, account.accessToken))
                        return@runCatching local
                    }
                    remote
                }.getOrNull()
            }
        }

    suspend fun reconcileTraktClientId(profileId: String, localClientId: String): String? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val account = accountRepository.resolveAccountState()
            if (account !is AccountState.AuthenticatedOnline) return@withLock null
            runCatching {
                val remote = sourceApi.getTraktClientId(profileId, account.accessToken)
                if (remote == null && localClientId.length in 20..256) {
                    sourceApi.saveTraktClientId(profileId, localClientId, account.accessToken)
                }
                remote
            }.getOrNull()
        }
    }

    suspend fun reconcileOmdb(local: com.fortresstv.player.omdb.OmdbSettings): com.fortresstv.player.omdb.OmdbSettings? =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val account = accountRepository.resolveAccountState()
                if (account !is AccountState.AuthenticatedOnline) return@withLock null
                val token = profileRepository.currentUnlockToken()
                runCatching {
                    val remote = sourceApi.getOmdb(token, account.accessToken)
                    if (remote?.apiKey.isNullOrBlank() && local.apiKey.isNotBlank()) {
                        check(sourceApi.saveOmdb(local, token, account.accessToken))
                        return@runCatching local
                    }
                    remote
                }.getOrNull()
            }
        }

    suspend fun deleteTmdbFromCloud(unlockToken: String? = null): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val accountState = accountRepository.resolveAccountState()
            if (accountState !is AccountState.AuthenticatedOnline) return@withLock false
            val token = unlockToken ?: profileRepository.currentUnlockToken()
            runCatching {
                sourceApi.deleteTmdb(token, accountState.accessToken)
            }.getOrDefault(false)
        }
    }

    suspend fun syncOmdbToCloud(
        settings: com.fortresstv.player.omdb.OmdbSettings,
        unlockToken: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val accountState = accountRepository.resolveAccountState()
            if (accountState !is AccountState.AuthenticatedOnline) {
                return@withLock Result.failure(IllegalStateException("Not authenticated"))
            }
            val token = unlockToken ?: profileRepository.currentUnlockToken()
            runCatching {
                val ok = sourceApi.saveOmdb(settings, token, accountState.accessToken)
                if (!ok) error("Failed to save OMDb settings")
            }
        }
    }

    suspend fun restoreOmdbFromCloud(unlockToken: String? = null): com.fortresstv.player.omdb.OmdbSettings? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val accountState = accountRepository.resolveAccountState()
            if (accountState !is AccountState.AuthenticatedOnline) return@withLock null
            val token = unlockToken ?: profileRepository.currentUnlockToken()
            runCatching {
                sourceApi.getOmdb(token, accountState.accessToken)
            }.getOrNull()
        }
    }

    suspend fun deleteOmdbFromCloud(unlockToken: String? = null): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val accountState = accountRepository.resolveAccountState()
            if (accountState !is AccountState.AuthenticatedOnline) return@withLock false
            val token = unlockToken ?: profileRepository.currentUnlockToken()
            runCatching {
                sourceApi.deleteOmdb(token, accountState.accessToken)
            }.getOrDefault(false)
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

    suspend fun deleteCustomEpgFromCloud(unlockToken: String? = null): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val accountState = accountRepository.resolveAccountState()
            if (accountState !is AccountState.AuthenticatedOnline) return@withLock false
            val token = unlockToken ?: profileRepository.currentUnlockToken()
            val remoteSources = runCatching {
                sourceApi.getHouseholdSources(token, accountState.accessToken)
            }.getOrNull() ?: return@withLock false
            var allOk = true
            for (src in remoteSources) {
                if (src.sourceType == "custom_epg") {
                    val ok = runCatching {
                        sourceApi.deleteHouseholdSource(src.id, token, accountState.accessToken)
                    }.getOrDefault(false)
                    if (!ok) allOk = false
                }
            }
            allOk
        }
    }

    suspend fun syncEpgMappingToCloud(
        mapping: com.fortresstv.player.epg.EpgChannelMapping,
        unlockToken: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val accountState = accountRepository.resolveAccountState()
            if (accountState !is AccountState.AuthenticatedOnline) {
                return@withLock Result.failure(IllegalStateException("Not authenticated"))
            }
            val token = unlockToken ?: profileRepository.currentUnlockToken()
            runCatching {
                val ok = sourceApi.saveEpgMapping(mapping, token, accountState.accessToken)
                if (!ok) error("Failed to save channel mapping")
            }
        }
    }

    suspend fun deleteEpgMappingFromCloud(
        iptvSourceId: String,
        iptvChannelId: String,
        unlockToken: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val accountState = accountRepository.resolveAccountState()
            if (accountState !is AccountState.AuthenticatedOnline) {
                return@withLock Result.failure(IllegalStateException("Not authenticated"))
            }
            val token = unlockToken ?: profileRepository.currentUnlockToken()
            runCatching {
                val ok = sourceApi.deleteEpgMapping(iptvSourceId, iptvChannelId, token, accountState.accessToken)
                if (!ok) error("Failed to delete channel mapping")
            }
        }
    }

    suspend fun restoreEpgMappingsFromCloud(): List<com.fortresstv.player.epg.EpgChannelMapping> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val accountState = accountRepository.resolveAccountState()
            if (accountState !is AccountState.AuthenticatedOnline) return@withLock emptyList()
            runCatching {
                sourceApi.getEpgMappings(accountState.accessToken)
            }.getOrDefault(emptyList())
        }
    }

    suspend fun resetAllEpgMappingsInCloud(unlockToken: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val accountState = accountRepository.resolveAccountState()
            if (accountState !is AccountState.AuthenticatedOnline) {
                return@withLock Result.failure(IllegalStateException("Not authenticated"))
            }
            val token = unlockToken ?: profileRepository.currentUnlockToken()
            runCatching {
                val ok = sourceApi.resetAllEpgMappings(token, accountState.accessToken)
                if (!ok) error("Failed to reset channel mappings")
            }
        }
    }
}
