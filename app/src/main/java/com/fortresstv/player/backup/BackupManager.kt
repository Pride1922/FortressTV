package com.fortresstv.player.backup

import android.content.Context
import com.fortresstv.player.library.UserLibraryStore
import com.fortresstv.player.library.VodProgressEntry
import com.fortresstv.player.library.VodProgressKind
import com.fortresstv.player.library.VodProgressStore
import com.fortresstv.player.playlist.PlaylistProfile
import com.fortresstv.player.playlist.SourceRole
import com.fortresstv.player.source.ContentSource
import com.fortresstv.player.source.SourceSettingsStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

class BackupManager(
    private val sourceSettingsStore: SourceSettingsStore,
    private val userLibraryStore: UserLibraryStore,
    private val vodProgressStore: VodProgressStore
) {
    constructor(context: Context) : this(
        sourceSettingsStore = SourceSettingsStore(context),
        userLibraryStore = UserLibraryStore(context),
        vodProgressStore = VodProgressStore(context)
    )

    fun createPayload(appVersion: String = "1.0.0"): BackupPayload {
        val playlists = sourceSettingsStore.getPlaylists()
        val epgSource = sourceSettingsStore.epgSource()

        val settings = BackupSettings(
            language = sourceSettingsStore.getLanguage(),
            playChannelsFullscreen = sourceSettingsStore.playChannelsFullscreen(),
            autoPictureInPicture = sourceSettingsStore.autoPictureInPicture(),
            hideAdultContent = sourceSettingsStore.hideAdultContent(),
            parentalControlEnabled = sourceSettingsStore.parentalControlEnabled(),
            parentalPin = sourceSettingsStore.getParentalPin(),
            epgSource = epgSource,
            epgTimeShiftHours = sourceSettingsStore.epgTimeShiftHours(),
            activePlaylistId = sourceSettingsStore.getActivePlaylistId(),
            preferredAudioLanguage = sourceSettingsStore.preferredAudioLanguage(),
            preferredSubtitleLanguage = sourceSettingsStore.preferredSubtitleLanguage(),
            subtitleFallbackEnglish = sourceSettingsStore.subtitleFallbackEnglish(),
            epgLoadedDays = sourceSettingsStore.epgLoadedDays()
        )

        val hiddenCategories = mutableMapOf<String, Set<String>>()
        val categoryOrders = mutableMapOf<String, List<String>>()

        playlists.forEach { profile ->
            val hidden = userLibraryStore.hiddenCategories(profile.id)
            if (hidden.isNotEmpty()) {
                hiddenCategories[profile.id] = hidden
            }
            val order = userLibraryStore.categoryOrder(profile.id)
            if (order.isNotEmpty()) {
                categoryOrders[profile.id] = order
            }
        }

        val library = BackupLibrary(
            favoriteChannelIds = userLibraryStore.favoriteChannelIds(),
            movieWatchlistIds = userLibraryStore.movieWatchlistIds(),
            followedSeriesIds = userLibraryStore.followedSeriesIds(),
            watchedItemIds = userLibraryStore.watchedItemIds(),
            recentChannelIds = userLibraryStore.recentChannelIds(),
            hiddenCategoriesByProfile = hiddenCategories,
            categoryOrderByProfile = categoryOrders
        )

        val vodProgress = vodProgressStore.entries()

        return BackupPayload(
            version = BackupPayload.CURRENT_VERSION,
            createdAt = System.currentTimeMillis(),
            appVersion = appVersion,
            settings = settings,
            playlists = playlists,
            library = library,
            vodProgress = vodProgress
        )
    }

    fun exportBackup(
        password: CharArray,
        outputStream: OutputStream,
        appVersion: String = "1.0.0"
    ): BackupExportResult = runCatching {
        val payload = createPayload(appVersion)
        val json = encodePayloadToJson(payload)
        val plainBytes = json.toByteArray(Charsets.UTF_8)
        BackupCrypto.encrypt(plainBytes, password, outputStream)
        BackupExportResult.Success(plainBytes.size)
    }.getOrElse { e ->
        BackupExportResult.Error(e.message ?: "Failed to create backup", e)
    }

    fun restoreBackup(
        password: CharArray,
        inputStream: InputStream,
        merge: Boolean = false
    ): BackupRestoreResult = runCatching {
        val plainBytes = BackupCrypto.decrypt(inputStream, password)
        val json = plainBytes.toString(Charsets.UTF_8)
        val payload = decodePayloadFromJson(json)
        applyPayload(payload, merge)
    }.getOrElse { e ->
        when (e) {
            is WrongPasswordException -> BackupRestoreResult.InvalidPassword(e.message ?: "Incorrect password")
            is InvalidBackupFormatException, is UnsupportedBackupVersionException ->
                BackupRestoreResult.InvalidFormat(e.message ?: "Invalid backup format")
            else -> BackupRestoreResult.Error(e.message ?: "Failed to restore backup", e)
        }
    }

    fun applyPayload(payload: BackupPayload, merge: Boolean = false): BackupRestoreResult {
        // 1. Settings
        val s = payload.settings
        sourceSettingsStore.setLanguage(s.language)
        sourceSettingsStore.setPlayChannelsFullscreen(s.playChannelsFullscreen)
        sourceSettingsStore.setAutoPictureInPicture(s.autoPictureInPicture)
        sourceSettingsStore.setHideAdultContent(s.hideAdultContent)
        sourceSettingsStore.setParentalControlEnabled(s.parentalControlEnabled)
        sourceSettingsStore.setParentalPin(s.parentalPin)
        sourceSettingsStore.setPreferredAudioLanguage(s.preferredAudioLanguage)
        sourceSettingsStore.setPreferredSubtitleLanguage(s.preferredSubtitleLanguage)
        sourceSettingsStore.setSubtitleFallbackEnglish(s.subtitleFallbackEnglish)
        if (s.epgSource != null) {
            sourceSettingsStore.saveEpg(s.epgSource)
        } else if (!merge) {
            sourceSettingsStore.clearEpg()
        }
        sourceSettingsStore.saveEpgTimeShiftHours(s.epgTimeShiftHours)
        sourceSettingsStore.saveEpgLoadedDays(s.epgLoadedDays)

        // 2. Playlists
        val existingPlaylists = if (merge) sourceSettingsStore.getPlaylists() else emptyList()
        val combinedPlaylists = if (merge) {
            val existingIds = existingPlaylists.map { it.id }.toSet()
            existingPlaylists + payload.playlists.filterNot { it.id in existingIds }
        } else {
            payload.playlists
        }

        // Save playlists
        if (merge) {
            combinedPlaylists.forEach(sourceSettingsStore::savePlaylistProfile)
        } else {
            sourceSettingsStore.replacePlaylists(combinedPlaylists)
            userLibraryStore.clearForRestore()
        }

        if (s.activePlaylistId != null && combinedPlaylists.any { it.id == s.activePlaylistId }) {
            sourceSettingsStore.setActivePlaylistId(s.activePlaylistId)
        } else if (combinedPlaylists.isNotEmpty()) {
            sourceSettingsStore.setActivePlaylistId(combinedPlaylists.first().id)
        }

        // 3. User Library
        val lib = payload.library
        if (merge) {
            userLibraryStore.setFavoriteChannelIds(userLibraryStore.favoriteChannelIds() + lib.favoriteChannelIds)
            userLibraryStore.setMovieWatchlistIds(userLibraryStore.movieWatchlistIds() + lib.movieWatchlistIds)
            userLibraryStore.setFollowedSeriesIds(userLibraryStore.followedSeriesIds() + lib.followedSeriesIds)
            userLibraryStore.setWatchedItemIds(userLibraryStore.watchedItemIds() + lib.watchedItemIds)
            val combinedRecents = (userLibraryStore.recentChannelIds() + lib.recentChannelIds).distinct().take(20)
            userLibraryStore.setRecentChannelIds(combinedRecents)
        } else {
            userLibraryStore.setFavoriteChannelIds(lib.favoriteChannelIds)
            userLibraryStore.setMovieWatchlistIds(lib.movieWatchlistIds)
            userLibraryStore.setFollowedSeriesIds(lib.followedSeriesIds)
            userLibraryStore.setWatchedItemIds(lib.watchedItemIds)
            userLibraryStore.setRecentChannelIds(lib.recentChannelIds)
        }

        // Hidden categories and orders
        lib.hiddenCategoriesByProfile.forEach { (profileId, hiddenSet) ->
            if (merge) {
                val currentHidden = userLibraryStore.hiddenCategories(profileId)
                userLibraryStore.hideAllCategories(profileId, currentHidden + hiddenSet)
            } else {
                userLibraryStore.hideAllCategories(profileId, hiddenSet)
            }
        }

        lib.categoryOrderByProfile.forEach { (profileId, orderList) ->
            userLibraryStore.setCategoryOrder(profileId, orderList)
        }

        // 4. VOD Progress
        val combinedVod = if (merge) {
            val existing = vodProgressStore.entries()
            val existingKeys = existing.map { it.key }.toSet()
            existing + payload.vodProgress.filterNot { it.key in existingKeys }
        } else {
            payload.vodProgress
        }
        vodProgressStore.setAll(combinedVod)

        return BackupRestoreResult.Success(
            restoredPlaylistsCount = payload.playlists.size,
            restoredFavoritesCount = lib.favoriteChannelIds.size,
            restoredVodProgressCount = payload.vodProgress.size
        )
    }

    companion object {
        fun encodePayloadToJson(payload: BackupPayload): String {
            val root = JSONObject()
            root.put("version", payload.version)
            root.put("createdAt", payload.createdAt)
            root.put("appVersion", payload.appVersion)

            // Settings
            val settingsObj = JSONObject().apply {
                put("language", payload.settings.language)
                put("playChannelsFullscreen", payload.settings.playChannelsFullscreen)
                put("autoPictureInPicture", payload.settings.autoPictureInPicture)
                put("hideAdultContent", payload.settings.hideAdultContent)
                put("parentalControlEnabled", payload.settings.parentalControlEnabled)
                put("parentalPin", payload.settings.parentalPin ?: JSONObject.NULL)
                put("epgTimeShiftHours", payload.settings.epgTimeShiftHours)
                put("activePlaylistId", payload.settings.activePlaylistId ?: JSONObject.NULL)
                put("preferredAudioLanguage", payload.settings.preferredAudioLanguage)
                put("preferredSubtitleLanguage", payload.settings.preferredSubtitleLanguage)
                put("subtitleFallbackEnglish", payload.settings.subtitleFallbackEnglish)
                put("epgLoadedDays", payload.settings.epgLoadedDays)

                if (payload.settings.epgSource != null) {
                    val epgObj = JSONObject().apply {
                        put("type", if (payload.settings.epgSource is ContentSource.LocalFile) "file" else "url")
                        put("value", payload.settings.epgSource.value)
                        put("displayName", payload.settings.epgSource.displayName)
                    }
                    put("epgSource", epgObj)
                } else {
                    put("epgSource", JSONObject.NULL)
                }
            }
            root.put("settings", settingsObj)

            // Playlists
            val playlistsArray = JSONArray()
            payload.playlists.forEach { p ->
                val pObj = JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("type", when (p.source) {
                        is ContentSource.LocalFile -> "file"
                        is ContentSource.RemoteUrl -> "url"
                        is ContentSource.Xtream -> "xtream"
                    })
                    put("value", p.source.value)
                    put("displayName", p.source.displayName)
                    put("lastUpdated", p.lastUpdated)
                    put("channelCount", p.channelCount)
                    put("enabled", p.enabled)
                    put("role", p.role.name)
                    put("order", p.order)
                    if (p.source is ContentSource.Xtream) {
                        put("username", p.source.username)
                        put("password", p.source.password)
                    }
                }
                playlistsArray.put(pObj)
            }
            root.put("playlists", playlistsArray)

            // Library
            val libObj = JSONObject().apply {
                put("favorites", JSONArray(payload.library.favoriteChannelIds))
                put("movieWatchlist", JSONArray(payload.library.movieWatchlistIds))
                put("followedSeries", JSONArray(payload.library.followedSeriesIds))
                put("watchedItems", JSONArray(payload.library.watchedItemIds))
                put("recentChannels", JSONArray(payload.library.recentChannelIds))

                val hiddenObj = JSONObject()
                payload.library.hiddenCategoriesByProfile.forEach { (k, v) ->
                    hiddenObj.put(k, JSONArray(v))
                }
                put("hiddenCategories", hiddenObj)

                val orderObj = JSONObject()
                payload.library.categoryOrderByProfile.forEach { (k, v) ->
                    orderObj.put(k, JSONArray(v))
                }
                put("categoryOrders", orderObj)
            }
            root.put("library", libObj)

            // VOD Progress
            val vodArray = JSONArray()
            payload.vodProgress.forEach { entry ->
                vodArray.put(JSONObject().apply {
                    put("key", entry.key)
                    put("kind", entry.kind.name)
                    put("sourceId", entry.sourceId)
                    put("itemId", entry.itemId)
                    put("seriesId", entry.seriesId ?: JSONObject.NULL)
                    put("title", entry.title)
                    put("posterUrl", entry.posterUrl ?: JSONObject.NULL)
                    put("containerExtension", entry.containerExtension)
                    put("season", entry.season ?: JSONObject.NULL)
                    put("episode", entry.episode ?: JSONObject.NULL)
                    put("positionMs", entry.positionMs)
                    put("durationMs", entry.durationMs)
                    put("updatedAtMs", entry.updatedAtMs)
                })
            }
            root.put("vodProgress", vodArray)

            return root.toString()
        }

        fun decodePayloadFromJson(jsonStr: String): BackupPayload {
            val root = JSONObject(jsonStr)
            val version = root.optInt("version", 1)
            val createdAt = root.optLong("createdAt", System.currentTimeMillis())
            val appVersion = root.optString("appVersion", "1.0.0")

            // Settings
            val settingsObj = root.getJSONObject("settings")
            val epgSource = if (settingsObj.has("epgSource") && !settingsObj.isNull("epgSource")) {
                val epg = settingsObj.getJSONObject("epgSource")
                val type = epg.optString("type")
                val value = epg.optString("value")
                val name = epg.optString("displayName", "EPG")
                if (type == "file") ContentSource.LocalFile(value, name) else ContentSource.RemoteUrl(value, name)
            } else null

            val settings = BackupSettings(
                language = settingsObj.optString("language", "en"),
                playChannelsFullscreen = settingsObj.optBoolean("playChannelsFullscreen", false),
                autoPictureInPicture = settingsObj.optBoolean("autoPictureInPicture", true),
                hideAdultContent = settingsObj.optBoolean("hideAdultContent", false),
                parentalControlEnabled = settingsObj.optBoolean("parentalControlEnabled", false),
                parentalPin = if (settingsObj.isNull("parentalPin")) null else settingsObj.optString("parentalPin"),
                epgSource = epgSource,
                epgTimeShiftHours = settingsObj.optInt("epgTimeShiftHours", 0),
                activePlaylistId = if (settingsObj.isNull("activePlaylistId")) null else settingsObj.optString("activePlaylistId"),
                preferredAudioLanguage = settingsObj.optString("preferredAudioLanguage", "default"),
                preferredSubtitleLanguage = settingsObj.optString("preferredSubtitleLanguage", "off"),
                subtitleFallbackEnglish = settingsObj.optBoolean("subtitleFallbackEnglish", true),
                epgLoadedDays = settingsObj.optInt("epgLoadedDays", 3).coerceIn(1, 7)
            )

            // Playlists
            val playlistsArray = root.optJSONArray("playlists") ?: JSONArray()
            val playlists = mutableListOf<PlaylistProfile>()
            for (i in 0 until playlistsArray.length()) {
                val obj = playlistsArray.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                val type = obj.optString("type", "url")
                val value = obj.optString("value", "")
                val displayName = obj.optString("displayName", name)
                val lastUpdated = obj.optLong("lastUpdated", 0L)
                val channelCount = obj.optInt("channelCount", 0)
                val enabled = obj.optBoolean("enabled", true)
                val role = runCatching {
                    SourceRole.valueOf(obj.optString("role", SourceRole.LIVE_TV.name))
                }.getOrDefault(SourceRole.LIVE_TV)
                val order = obj.optInt("order", i)

                val source = when (type) {
                    "file" -> ContentSource.LocalFile(value, displayName)
                    "xtream" -> ContentSource.Xtream(
                        value = value,
                        displayName = displayName,
                        username = obj.optString("username", ""),
                        password = obj.optString("password", "")
                    )
                    else -> ContentSource.RemoteUrl(value, displayName)
                }
                playlists.add(PlaylistProfile(id, name, source, lastUpdated, channelCount, enabled, role, order))
            }

            // Library
            val libObj = root.optJSONObject("library") ?: JSONObject()
            val favorites = parseStringSet(libObj.optJSONArray("favorites"))
            val watchlist = parseStringSet(libObj.optJSONArray("movieWatchlist"))
            val followed = parseStringSet(libObj.optJSONArray("followedSeries"))
            val watched = parseStringSet(libObj.optJSONArray("watchedItems"))
            val recents = parseStringList(libObj.optJSONArray("recentChannels"))

            val hiddenObj = libObj.optJSONObject("hiddenCategories")
            val hiddenMap = mutableMapOf<String, Set<String>>()
            hiddenObj?.keys()?.forEach { key ->
                hiddenMap[key] = parseStringSet(hiddenObj.optJSONArray(key))
            }

            val orderObj = libObj.optJSONObject("categoryOrders")
            val orderMap = mutableMapOf<String, List<String>>()
            orderObj?.keys()?.forEach { key ->
                orderMap[key] = parseStringList(orderObj.optJSONArray(key))
            }

            val library = BackupLibrary(
                favoriteChannelIds = favorites,
                movieWatchlistIds = watchlist,
                followedSeriesIds = followed,
                watchedItemIds = watched,
                recentChannelIds = recents,
                hiddenCategoriesByProfile = hiddenMap,
                categoryOrderByProfile = orderMap
            )

            // VOD Progress
            val vodArray = root.optJSONArray("vodProgress") ?: JSONArray()
            val vodList = mutableListOf<VodProgressEntry>()
            for (i in 0 until vodArray.length()) {
                val item = vodArray.optJSONObject(i) ?: continue
                val key = item.optString("key").takeIf(String::isNotBlank) ?: continue
                val sourceId = item.optString("sourceId").takeIf(String::isNotBlank) ?: continue
                val kind = runCatching { VodProgressKind.valueOf(item.optString("kind")) }.getOrNull() ?: continue
                vodList.add(
                    VodProgressEntry(
                        key = key,
                        kind = kind,
                        sourceId = sourceId,
                        itemId = item.optString("itemId"),
                        seriesId = item.optInt("seriesId").takeIf { !item.isNull("seriesId") },
                        title = item.optString("title"),
                        posterUrl = item.optString("posterUrl").takeIf { !item.isNull("posterUrl") && it.isNotBlank() },
                        containerExtension = item.optString("containerExtension").ifBlank { "mp4" },
                        season = item.optInt("season").takeIf { !item.isNull("season") },
                        episode = item.optInt("episode").takeIf { !item.isNull("episode") },
                        positionMs = item.optLong("positionMs").coerceAtLeast(0),
                        durationMs = item.optLong("durationMs").coerceAtLeast(0),
                        updatedAtMs = item.optLong("updatedAtMs")
                    )
                )
            }

            return BackupPayload(
                version = version,
                createdAt = createdAt,
                appVersion = appVersion,
                settings = settings,
                playlists = playlists,
                library = library,
                vodProgress = vodList
            )
        }

        private fun parseStringSet(array: JSONArray?): Set<String> {
            if (array == null) return emptySet()
            val set = mutableSetOf<String>()
            for (i in 0 until array.length()) {
                set.add(array.getString(i))
            }
            return set
        }

        private fun parseStringList(array: JSONArray?): List<String> {
            if (array == null) return emptyList()
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            return list
        }
    }
}
