package com.fortresstv.player.epg

import android.content.Context
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.zip.Deflater
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object EpgDiskCache {
    private const val EPG_DIR = "epg"
    private const val CURRENT_FILE = "epg_current.bin"
    private const val MANIFEST_PREFIX = "epg_manifest_"
    private const val DAY_PREFIX = "epg_day_"

    fun getEpgDir(context: Context): File {
        val dir = File(context.filesDir, EPG_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun readCurrentManifest(context: Context, expectedFingerprint: String? = null): EpgManifest? {
        val epgDir = getEpgDir(context)
        val currentFile = File(epgDir, CURRENT_FILE)
        if (!currentFile.isFile || currentFile.length() == 0L) return null

        return runCatching {
            val genId = currentFile.readText(Charsets.UTF_8).trim()
            if (genId.isBlank()) return null
            val manifestFile = File(epgDir, "$MANIFEST_PREFIX$genId.bin")
            if (!manifestFile.isFile || manifestFile.length() == 0L) return null

            manifestFile.inputStream().use { stream ->
                EpgCacheCodec.readManifest(stream, expectedFingerprint)
            }
        }.getOrNull()
    }

    fun loadDayPartition(context: Context, manifest: EpgManifest, epochDay: Long): EpgDay? {
        val partitionMeta = manifest.partitions[epochDay] ?: return null
        val epgDir = getEpgDir(context)
        val file = File(epgDir, partitionMeta.filename)
        if (!file.isFile || file.length() == 0L) return null

        return runCatching {
            file.inputStream().use { stream ->
                EpgCacheCodec.readDayPartition(stream, manifest.generationId, epochDay)
            }
        }.getOrNull()
    }

    fun loadTodayGuide(context: Context, expectedFingerprint: String?): EpgGuide? {
        val manifest = readCurrentManifest(context, expectedFingerprint) ?: return null
        val zone = runCatching { java.time.ZoneId.of(manifest.timezoneId) }.getOrElse { java.time.ZoneId.systemDefault() }
        val currentTodayEpoch = java.time.LocalDate.now(zone).toEpochDay()
        val targetDay = if (manifest.partitions.containsKey(currentTodayEpoch)) {
            currentTodayEpoch
        } else {
            manifest.todayEpochDay
        }
        val todayDay = loadDayPartition(context, manifest, targetDay)
        return EpgGuide(
            channels = manifest.channels,
            programmes = todayDay?.programmes.orEmpty(),
            manifest = manifest
        )
    }

    fun createStagingSession(
        context: Context,
        generationId: String,
        fingerprint: String,
        timezoneId: String,
        loadedDays: Int,
        todayEpochDay: Long
    ): StagingSession {
        val epgDir = getEpgDir(context)
        val stagingDir = File(epgDir, "staging_$generationId")
        if (!stagingDir.exists()) stagingDir.mkdirs()
        return StagingSession(context, epgDir, stagingDir, generationId, fingerprint, timezoneId, loadedDays, todayEpochDay)
    }

    fun clear(context: Context) {
        val epgDir = getEpgDir(context)
        epgDir.listFiles()?.forEach { it.deleteRecursively() }
        File(context.filesDir, "parsed_epg_v4.bin").delete()
        File(context.filesDir, "parsed_epg_v4.tmp").delete()
        File(context.filesDir, "parsed_epg_v3.bin").delete()
        File(context.filesDir, "parsed_epg_v3.tmp").delete()
        File(context.filesDir, "parsed_epg_v2.bin").delete()
        File(context.filesDir, "parsed_epg_v2.tmp").delete()
        File(context.filesDir, "parsed_epg_v1.bin").delete()
        File(context.filesDir, "parsed_epg_v1.tmp").delete()
    }

    class StagingSession internal constructor(
        private val context: Context? = null,
        private val epgDir: File,
        private val stagingDir: File,
        val generationId: String,
        val fingerprint: String,
        val timezoneId: String,
        val loadedDays: Int,
        val todayEpochDay: Long
    ) {
        init {
            if (!epgDir.exists()) epgDir.mkdirs()
            if (!stagingDir.exists()) stagingDir.mkdirs()
        }

        private val daySpools = mutableMapOf<Long, DaySpoolWriter>()
        private var isClosed = false

        @Synchronized
        fun appendProgramme(epochDay: Long, programme: EpgProgramme) {
            if (isClosed) return
            val spool = daySpools.getOrPut(epochDay) {
                val tempFile = File(stagingDir, "day_$epochDay.tmp")
                DaySpoolWriter(epochDay, tempFile)
            }
            spool.writeProgramme(programme)
        }

        @Synchronized
        fun commit(channels: Map<String, EpgChannel>): EpgManifest {
            require(!isClosed) { "StagingSession already closed" }
            isClosed = true

            val partitions = mutableMapOf<Long, EpgPartitionMetadata>()

            try {
                // 1. Flush, close, and finalize every day partition
                for ((epochDay, spool) in daySpools) {
                    spool.close()
                    val partitionFilename = "$DAY_PREFIX${generationId}_$epochDay.bin"
                    val finalFile = File(epgDir, partitionFilename)

                    // Write validated partition format from spool
                    val storedProgrammeCount = writeFinalPartition(
                        spool.tempFile,
                        finalFile,
                        generationId,
                        epochDay,
                        spool.programmeCount
                    )

                    val checksum = computeChecksum(finalFile)
                    partitions[epochDay] = EpgPartitionMetadata(
                        epochDay = epochDay,
                        filename = partitionFilename,
                        programmeCount = storedProgrammeCount,
                        compressedSize = finalFile.length(),
                        checksum = checksum
                    )
                }

                // 2. Write generation manifest
                val manifest = EpgManifest(
                    generationId = generationId,
                    sourceFingerprint = fingerprint,
                    timezoneId = timezoneId,
                    loadedDays = loadedDays,
                    todayEpochDay = todayEpochDay,
                    createdAtMillis = System.currentTimeMillis(),
                    channels = channels,
                    partitions = partitions
                )

                val manifestFilename = "$MANIFEST_PREFIX$generationId.bin"
                val manifestTempFile = File(stagingDir, manifestFilename)
                val manifestFinalFile = File(epgDir, manifestFilename)

                manifestTempFile.outputStream().use { stream ->
                    EpgCacheCodec.writeManifest(stream, manifest)
                }

                Files.move(
                    manifestTempFile.toPath(),
                    manifestFinalFile.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                )

                // 3. Atomically update epg_current.bin
                val currentTemp = File(stagingDir, "current.tmp")
                currentTemp.writeText(generationId, Charsets.UTF_8)
                val currentFinal = File(epgDir, CURRENT_FILE)
                Files.move(
                    currentTemp.toPath(),
                    currentFinal.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                )

                // 4. Purge older generations
                purgeOlderGenerations(epgDir, generationId)

                return manifest
            } finally {
                stagingDir.deleteRecursively()
            }
        }

        @Synchronized
        fun abort() {
            if (isClosed) return
            isClosed = true
            daySpools.values.forEach { it.closeQuietly() }
            stagingDir.deleteRecursively()
        }

        private fun writeFinalPartition(
            spoolFile: File,
            targetFile: File,
            genId: String,
            epochDay: Long,
            count: Int
        ): Int {
            val programmesByChannel = LinkedHashMap<String, MutableList<EpgProgramme>>()
            if (spoolFile.exists() && spoolFile.length() > 0L) {
                DataInputStream(BufferedInputStream(FileInputStream(spoolFile))).use { input ->
                    repeat(count) {
                        val channelId = input.readUTF()
                        val title = input.readUTF()
                        val description = if (input.readBoolean()) input.readUTF() else null
                        val category = if (input.readBoolean()) input.readUTF() else null
                        val start = input.readLong()
                        val stop = if (input.readBoolean()) input.readLong() else null
                        programmesByChannel.getOrPut(channelId) { mutableListOf() } += EpgProgramme(
                            channelId, title, description, category, start, stop
                        )
                    }
                }
            }

            val sortedMap = programmesByChannel.mapValues { (_, list) ->
                list.distinct().sortedBy { it.startEpochMilli }
            }
            val day = EpgDay(epochDay, sortedMap)

            targetFile.outputStream().use { output ->
                EpgCacheCodec.writeDayPartition(output, genId, day)
            }
            return sortedMap.values.sumOf { it.size }
        }

        private fun computeChecksum(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { stream ->
                val buffer = ByteArray(8192)
                var read: Int
                while (stream.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        private fun purgeOlderGenerations(epgDir: File, keepGenerationId: String) {
            runCatching {
                epgDir.listFiles()?.forEach { file ->
                    val name = file.name
                    if (name.startsWith(MANIFEST_PREFIX) && !name.contains(keepGenerationId)) {
                        file.delete()
                    } else if (name.startsWith(DAY_PREFIX) && !name.contains(keepGenerationId)) {
                        file.delete()
                    } else if (name.startsWith("staging_") && !name.contains(keepGenerationId)) {
                        file.deleteRecursively()
                    }
                }
            }
        }
    }

    private class DaySpoolWriter(val epochDay: Long, val tempFile: File) {
        private val output = DataOutputStream(BufferedOutputStream(FileOutputStream(tempFile)))
        var programmeCount = 0
            private set

        fun writeProgramme(programme: EpgProgramme) {
            output.writeUTF(programme.channelId)
            output.writeUTF(programme.title)
            output.writeBoolean(programme.description != null)
            if (programme.description != null) output.writeUTF(programme.description)
            output.writeBoolean(programme.category != null)
            if (programme.category != null) output.writeUTF(programme.category)
            output.writeLong(programme.startEpochMilli)
            output.writeBoolean(programme.stopEpochMilli != null)
            programme.stopEpochMilli?.let(output::writeLong)
            programmeCount++
        }

        fun close() {
            output.flush()
            output.close()
        }

        fun closeQuietly() {
            runCatching { output.close() }
        }
    }
}

internal object EpgCacheCodec {
    private const val MANIFEST_MAGIC = 0x46544D46 // FTMF
    private const val PARTITION_MAGIC = 0x46545046 // FTPF
    private const val VERSION = 5
    private const val MAX_CHANNELS = 100_000
    private const val MAX_PROGRAMMES_PER_CHANNEL = 100_000
    private const val MAX_STRING_BYTES = 4 * 1024 * 1024

    fun writeManifest(output: OutputStream, manifest: EpgManifest) {
        DataOutputStream(FastGzipOutputStream(BufferedOutputStream(output))).use { data ->
            data.writeInt(MANIFEST_MAGIC)
            data.writeInt(VERSION)
            data.writeString(manifest.generationId)
            data.writeString(manifest.sourceFingerprint)
            data.writeString(manifest.timezoneId)
            data.writeInt(manifest.loadedDays)
            data.writeLong(manifest.todayEpochDay)
            data.writeLong(manifest.createdAtMillis)

            // Channels
            data.writeInt(manifest.channels.size)
            manifest.channels.values.forEach { channel ->
                data.writeString(channel.id)
                data.writeNullableString(channel.displayName)
                data.writeNullableString(channel.iconUrl)
            }

            // Partitions metadata
            data.writeInt(manifest.partitions.size)
            manifest.partitions.values.forEach { part ->
                data.writeLong(part.epochDay)
                data.writeString(part.filename)
                data.writeInt(part.programmeCount)
                data.writeLong(part.compressedSize)
                data.writeString(part.checksum)
            }
        }
    }

    fun readManifest(input: InputStream, expectedFingerprint: String?): EpgManifest {
        DataInputStream(GZIPInputStream(BufferedInputStream(input))).use { data ->
            require(data.readInt() == MANIFEST_MAGIC) { "Invalid EPG manifest" }
            val version = data.readInt()
            require(version == VERSION) { "Unsupported EPG manifest version $version" }
            val generationId = data.readString()
            val sourceFingerprint = data.readString()
            if (expectedFingerprint != null) {
                require(sourceFingerprint == expectedFingerprint) { "Stale EPG manifest" }
            }
            val timezoneId = data.readString()
            val loadedDays = data.readInt()
            val todayEpochDay = data.readLong()
            val createdAtMillis = data.readLong()

            val channelCount = data.readBoundedCount(MAX_CHANNELS)
            val channels = LinkedHashMap<String, EpgChannel>(channelCount)
            repeat(channelCount) {
                val id = data.readString()
                channels[id] = EpgChannel(id, data.readNullableString(), data.readNullableString())
            }

            val partitionCount = data.readBoundedCount(MAX_CHANNELS)
            val partitions = LinkedHashMap<Long, EpgPartitionMetadata>(partitionCount)
            repeat(partitionCount) {
                val epochDay = data.readLong()
                val filename = data.readString()
                val count = data.readInt()
                val size = data.readLong()
                val checksum = data.readString()
                partitions[epochDay] = EpgPartitionMetadata(epochDay, filename, count, size, checksum)
            }

            return EpgManifest(
                schemaVersion = version,
                generationId = generationId,
                sourceFingerprint = sourceFingerprint,
                timezoneId = timezoneId,
                loadedDays = loadedDays,
                todayEpochDay = todayEpochDay,
                createdAtMillis = createdAtMillis,
                channels = channels,
                partitions = partitions
            )
        }
    }

    fun writeDayPartition(output: OutputStream, generationId: String, day: EpgDay) {
        DataOutputStream(FastGzipOutputStream(BufferedOutputStream(output))).use { data ->
            data.writeInt(PARTITION_MAGIC)
            data.writeInt(VERSION)
            data.writeString(generationId)
            data.writeLong(day.epochDay)

            data.writeInt(day.programmes.size)
            day.programmes.forEach { (channelId, programmes) ->
                data.writeString(channelId)
                data.writeInt(programmes.size)
                programmes.forEach { prog ->
                    data.writeString(prog.title)
                    data.writeNullableString(prog.description)
                    data.writeNullableString(prog.category)
                    data.writeLong(prog.startEpochMilli)
                    data.writeBoolean(prog.stopEpochMilli != null)
                    prog.stopEpochMilli?.let(data::writeLong)
                }
            }
        }
    }

    fun readDayPartition(input: InputStream, expectedGenerationId: String, expectedEpochDay: Long): EpgDay {
        DataInputStream(GZIPInputStream(BufferedInputStream(input))).use { data ->
            require(data.readInt() == PARTITION_MAGIC) { "Invalid EPG day partition" }
            require(data.readInt() == VERSION) { "Unsupported EPG partition version" }
            require(data.readString() == expectedGenerationId) { "Mismatched partition generation" }
            val epochDay = data.readLong()
            require(epochDay == expectedEpochDay) { "Mismatched partition epoch day" }

            val channelCount = data.readBoundedCount(MAX_CHANNELS)
            val programmes = LinkedHashMap<String, List<EpgProgramme>>(channelCount)
            repeat(channelCount) {
                val channelId = data.readString()
                val programmeCount = data.readBoundedCount(MAX_PROGRAMMES_PER_CHANNEL)
                programmes[channelId] = List(programmeCount) {
                    val title = data.readString()
                    val description = data.readNullableString()
                    val category = data.readNullableString()
                    val startEpoch = data.readLong()
                    val stopEpoch = if (data.readBoolean()) data.readLong() else null
                    EpgProgramme(channelId, title, description, category, startEpoch, stopEpoch)
                }
            }
            return EpgDay(epochDay, programmes)
        }
    }

    private fun DataOutputStream.writeString(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        writeInt(bytes.size)
        write(bytes)
    }

    private fun DataOutputStream.writeNullableString(value: String?) {
        writeBoolean(value != null)
        if (value != null) writeString(value)
    }

    private fun DataInputStream.readString(): String {
        val size = readInt()
        require(size in 0..MAX_STRING_BYTES) { "Invalid parsed EPG string size" }
        return ByteArray(size).also(::readFully).toString(Charsets.UTF_8)
    }

    private fun DataInputStream.readNullableString(): String? = if (readBoolean()) readString() else null

    private fun DataInputStream.readBoundedCount(maximum: Int): Int = readInt().also {
        require(it in 0..maximum) { "Invalid parsed EPG entry count" }
    }
}

private class FastGzipOutputStream(output: OutputStream) : GZIPOutputStream(output) {
    init {
        def.setLevel(Deflater.BEST_SPEED)
    }
}
