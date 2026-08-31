package com.fortresstv.player.epg

import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

interface EpgRepository {
    val manifest: StateFlow<EpgManifest?>
    val guide: StateFlow<EpgGuide?>
    val today: StateFlow<EpgDay?>

    fun setGuide(guide: EpgGuide?)
    suspend fun loadDay(epochDay: Long): EpgDay?
    fun cachedDay(epochDay: Long): EpgDay?
    suspend fun preloadTomorrowNearMidnight()
}

class DefaultEpgRepository(
    private val context: Context? = null,
    private val epgDir: java.io.File? = null
) : EpgRepository {
    private val _manifest = MutableStateFlow<EpgManifest?>(null)
    override val manifest: StateFlow<EpgManifest?> = _manifest.asStateFlow()

    private val _guide = MutableStateFlow<EpgGuide?>(null)
    override val guide: StateFlow<EpgGuide?> = _guide.asStateFlow()

    private val _today = MutableStateFlow<EpgDay?>(null)
    override val today: StateFlow<EpgDay?> = _today.asStateFlow()

    // Pinned today partition + 1-entry LRU for active past/future day
    private val lruMutex = Mutex()
    private var pinnedTodayDay: EpgDay? = null
    private var activeLruDay: EpgDay? = null

    // In-flight load deduplication
    private val inFlightLoads = ConcurrentHashMap<DayLoadKey, Deferred<EpgDay?>>()

    override fun setGuide(guide: EpgGuide?) {
        _guide.value = guide
        _manifest.value = guide?.manifest
        val todayEpoch = guide?.manifest?.todayEpochDay
        if (todayEpoch != null && guide.programmes.isNotEmpty()) {
            val todayDay = EpgDay(todayEpoch, guide.programmes)
            pinnedTodayDay = todayDay
            _today.value = todayDay
        }
    }

    override fun cachedDay(epochDay: Long): EpgDay? {
        val currentPinned = pinnedTodayDay
        if (currentPinned?.epochDay == epochDay) return currentPinned
        val currentLru = activeLruDay
        if (currentLru?.epochDay == epochDay) return currentLru
        return null
    }

    override suspend fun loadDay(epochDay: Long): EpgDay? {
        cachedDay(epochDay)?.let { return it }

        val currentManifest = _manifest.value ?: return null
        if (!currentManifest.partitions.containsKey(epochDay)) return null

        val loadKey = DayLoadKey(currentManifest.generationId, epochDay)
        val newLoad = CompletableDeferred<EpgDay?>()
        val existingLoad = inFlightLoads.putIfAbsent(loadKey, newLoad)
        if (existingLoad != null) return existingLoad.await()

        try {
            val loaded = withContext(Dispatchers.IO) {
                if (context != null) {
                    EpgDiskCache.loadDayPartition(context, currentManifest, epochDay)
                } else if (epgDir != null) {
                    val partitionMeta = currentManifest.partitions[epochDay]
                    val file = partitionMeta?.let { java.io.File(epgDir, it.filename) }
                    if (file?.isFile == true) {
                        runCatching {
                            file.inputStream().use { stream ->
                                EpgCacheCodec.readDayPartition(stream, currentManifest.generationId, epochDay)
                            }
                        }.getOrNull()
                    } else null
                } else null
            }

            if (loaded != null) {
                lruMutex.withLock {
                    // Do not publish a stale partition if the guide changed while it was loading.
                    if (_manifest.value?.generationId == currentManifest.generationId) {
                        if (epochDay == currentManifest.todayEpochDay) {
                            pinnedTodayDay = loaded
                            _today.value = loaded
                        } else {
                            activeLruDay = loaded
                        }
                    }
                }
            }
            newLoad.complete(loaded)
            return loaded
        } catch (t: Throwable) {
            newLoad.completeExceptionally(t)
            throw t
        } finally {
            inFlightLoads.remove(loadKey, newLoad)
        }
    }

    override suspend fun preloadTomorrowNearMidnight() {
        val currentManifest = _manifest.value ?: return
        val tomorrowEpochDay = currentManifest.todayEpochDay + 1
        if (cachedDay(tomorrowEpochDay) == null && currentManifest.partitions.containsKey(tomorrowEpochDay)) {
            withContext(Dispatchers.IO) {
                loadDay(tomorrowEpochDay)
            }
        }
    }

    private data class DayLoadKey(val generationId: String, val epochDay: Long)
}
