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

    // Today stays pinned and the remaining configured guide window is cached. XMLTV
    // ingestion is already bounded to seven days, so retaining those partitions avoids
    // decoding the same small files every time the user changes guide days.
    private val cacheMutex = Mutex()
    private var pinnedTodayDay: EpgDay? = null
    private val cachedDays = ConcurrentHashMap<Long, EpgDay>()

    // In-flight load deduplication
    private val inFlightLoads = ConcurrentHashMap<DayLoadKey, Deferred<EpgDay?>>()

    private fun resolveActiveTodayEpoch(manifest: EpgManifest?): Long? {
        if (manifest == null) return null
        val zone = runCatching { java.time.ZoneId.of(manifest.timezoneId) }.getOrElse { java.time.ZoneId.systemDefault() }
        val currentTodayEpoch = java.time.LocalDate.now(zone).toEpochDay()
        return if (manifest.partitions.containsKey(currentTodayEpoch)) {
            currentTodayEpoch
        } else {
            manifest.todayEpochDay
        }
    }

    override fun setGuide(guide: EpgGuide?) {
        val generationChanged = _manifest.value?.generationId != guide?.manifest?.generationId
        if (generationChanged) {
            pinnedTodayDay = null
            cachedDays.clear()
        }
        _guide.value = guide
        _manifest.value = guide?.manifest
        val todayEpoch = resolveActiveTodayEpoch(guide?.manifest)
        if (guide != null && todayEpoch != null && guide.programmes.isNotEmpty()) {
            val todayDay = EpgDay(todayEpoch, guide.programmes)
            pinnedTodayDay = todayDay
            _today.value = todayDay
        }
    }

    override fun cachedDay(epochDay: Long): EpgDay? {
        val currentPinned = pinnedTodayDay
        if (currentPinned?.epochDay == epochDay) return currentPinned
        return cachedDays[epochDay]
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
                cacheMutex.withLock {
                    // Do not publish a stale partition if the guide changed while it was loading.
                    if (_manifest.value?.generationId == currentManifest.generationId) {
                        val activeTodayEpoch = resolveActiveTodayEpoch(currentManifest)
                        if (epochDay == activeTodayEpoch) {
                            pinnedTodayDay = loaded
                            _today.value = loaded
                        } else {
                            cachedDays[epochDay] = loaded
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
        val activeTodayEpoch = resolveActiveTodayEpoch(currentManifest) ?: currentManifest.todayEpochDay
        val tomorrowEpochDay = activeTodayEpoch + 1
        if (cachedDay(tomorrowEpochDay) == null && currentManifest.partitions.containsKey(tomorrowEpochDay)) {
            withContext(Dispatchers.IO) {
                loadDay(tomorrowEpochDay)
            }
        }
    }

    private data class DayLoadKey(val generationId: String, val epochDay: Long)
}
