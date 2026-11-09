package com.fortresstv.player

import android.content.Context
import com.fortresstv.player.account.AccountRepository
import com.fortresstv.player.account.AccountState
import com.fortresstv.player.epg.EpgChannel
import com.fortresstv.player.epg.EpgChannelMapping
import com.fortresstv.player.epg.EpgDiskCache
import com.fortresstv.player.epg.EpgGuide
import com.fortresstv.player.epg.EpgMappingStore
import com.fortresstv.player.epg.EpgRepository
import com.fortresstv.player.epg.XmlTvParser
import com.fortresstv.player.playlist.PlaylistChannel
import com.fortresstv.player.source.CloudSourceSyncService
import com.fortresstv.player.source.ContentSource
import com.fortresstv.player.source.SourceReader
import com.fortresstv.player.source.SourceSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.ZoneId
import java.util.zip.GZIPInputStream

/**
 * Workflow owner for XMLTV parsing, EPG multi-day sliding window,
 * disk caching, and custom channel-to-EPG mapping persistence.
 */
internal class EpgSyncWorkflow(
    private val context: Context,
    private val settings: SourceSettingsStore,
    private val epgRepository: EpgRepository,
    private val epgMappingStore: EpgMappingStore,
    private val accountRepository: AccountRepository,
    private val cloudSourceSyncService: CloudSourceSyncService,
    private val scope: CoroutineScope
) {
    suspend fun parseEpg(
        stream: InputStream,
        loadedDays: Int
    ): EpgGuide = withContext(Dispatchers.Default) {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
        BufferedInputStream(stream).use { buffered ->
            buffered.mark(2)
            val first = buffered.read()
            val second = buffered.read()
            buffered.reset()
            val decompressed = if (first == 0x1f && second == 0x8b) GZIPInputStream(buffered) else buffered
            XmlTvParser.parse(
                SizeLimitedInputStream(decompressed, 250L * 1024L * 1024L, "Decompressed EPG"),
                loadedDays = loadedDays
            )
        }
    }

    suspend fun parseEpgWithCache(
        source: ContentSource,
        stream: InputStream,
        allowCachedResult: Boolean,
        loadedDays: Int,
        epgSourceTimezone: String,
        onPartialGuideLoaded: (EpgGuide) -> Unit = {}
    ): EpgGuide = withContext(Dispatchers.IO) {
        val fallbackZone = runCatching { ZoneId.of(epgSourceTimezone) }.getOrDefault(ZoneId.systemDefault())
        val baseFingerprint = SourceReader.fingerprint(context, source, "epg")
        val fingerprint = if (baseFingerprint != null) "$baseFingerprint#days=$loadedDays#tz=${fallbackZone.id}" else null
        if (allowCachedResult && fingerprint != null) {
            val manifest = EpgDiskCache.readCurrentManifest(context, fingerprint)
            val zone = manifest?.let { runCatching { ZoneId.of(it.timezoneId) }.getOrNull() } ?: ZoneId.systemDefault()
            val currentTodayEpoch = LocalDate.now(zone).toEpochDay()
            val hasFullWindow = manifest != null &&
                manifest.todayEpochDay == currentTodayEpoch &&
                (0 until loadedDays).all { manifest.partitions.containsKey(currentTodayEpoch + it) }

            if (hasFullWindow) {
                EpgDiskCache.loadTodayGuide(context, fingerprint)?.let { cached ->
                    stream.close()
                    epgRepository.setGuide(cached)
                    return@withContext cached
                }
            } else if (manifest != null && manifest.partitions.isNotEmpty()) {
                EpgDiskCache.loadTodayGuide(context, fingerprint)?.let { partialCached ->
                    epgRepository.setGuide(partialCached)
                    withContext(Dispatchers.Main) { onPartialGuideLoaded(partialCached) }
                }
            }
        }
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
        val manifest = BufferedInputStream(stream).use { buffered ->
            buffered.mark(2)
            val first = buffered.read()
            val second = buffered.read()
            buffered.reset()
            val decompressed = if (first == 0x1f && second == 0x8b) GZIPInputStream(buffered) else buffered
            XmlTvParser.parseToDisk(
                input = SizeLimitedInputStream(decompressed, 250L * 1024L * 1024L, "Decompressed EPG"),
                context = context,
                fingerprint = fingerprint.orEmpty(),
                loadedDays = loadedDays,
                fallbackSourceZone = fallbackZone
            )
        }
        val guide = EpgDiskCache.loadTodayGuide(context, fingerprint) ?: EpgGuide(channels = manifest.channels, manifest = manifest)
        epgRepository.setGuide(guide)
        guide
    }

    fun saveMapping(
        channel: PlaylistChannel,
        xmltvChannel: EpgChannel,
        epgSourceDisplayName: String?,
        unlockToken: String?
    ): Map<String, EpgChannelMapping> {
        val mapping = EpgChannelMapping(
            iptvSourceId = channel.sourceId,
            iptvChannelId = channel.sourceItemId,
            epgSourceId = epgSourceDisplayName,
            xmltvChannelId = xmltvChannel.id,
            isAvailable = true
        )
        epgMappingStore.saveMapping(mapping)
        val allMappings = epgMappingStore.getAllMappings()
        val accountState = accountRepository.getAccountState()
        if (accountState is AccountState.AuthenticatedOnline) {
            scope.launch(Dispatchers.IO) {
                cloudSourceSyncService.syncEpgMappingToCloud(mapping, unlockToken)
            }
        }
        return allMappings
    }

    fun resetMapping(
        channel: PlaylistChannel,
        unlockToken: String?
    ): Map<String, EpgChannelMapping> {
        epgMappingStore.removeMapping(channel.sourceId, channel.sourceItemId)
        val allMappings = epgMappingStore.getAllMappings()
        val accountState = accountRepository.getAccountState()
        if (accountState is AccountState.AuthenticatedOnline) {
            scope.launch(Dispatchers.IO) {
                cloudSourceSyncService.deleteEpgMappingFromCloud(channel.sourceId, channel.sourceItemId, unlockToken)
            }
        }
        return allMappings
    }
}
