package com.fortresstv.player.epg

import android.content.Context
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.Deflater
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object EpgDiskCache {
    private const val FILE_NAME = "parsed_epg_v4.bin"
    private const val TEMP_FILE_NAME = "parsed_epg_v4.tmp"

    fun load(context: Context, fingerprint: String?): EpgGuide? {
        if (fingerprint == null) return null
        val file = File(context.filesDir, FILE_NAME)
        if (!file.isFile || file.length() == 0L) return null
        return runCatching {
            file.inputStream().use { EpgCacheCodec.read(it, fingerprint) }
        }.getOrElse {
            file.delete()
            null
        }
    }

    fun save(context: Context, fingerprint: String?, guide: EpgGuide) {
        if (fingerprint == null) return
        val target = File(context.filesDir, FILE_NAME)
        val temporary = File(context.filesDir, TEMP_FILE_NAME)
        runCatching {
            temporary.outputStream().use { EpgCacheCodec.write(it, fingerprint, guide) }
            Files.move(
                temporary.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
            File(context.filesDir, "parsed_epg_v1.bin").delete()
            File(context.filesDir, "parsed_epg_v1.tmp").delete()
            File(context.filesDir, "parsed_epg_v2.bin").delete()
            File(context.filesDir, "parsed_epg_v2.tmp").delete()
            File(context.filesDir, "parsed_epg_v3.bin").delete()
            File(context.filesDir, "parsed_epg_v3.tmp").delete()
        }.onFailure {
            temporary.delete()
        }
    }

    fun clear(context: Context) {
        File(context.filesDir, FILE_NAME).delete()
        File(context.filesDir, TEMP_FILE_NAME).delete()
        File(context.filesDir, "parsed_epg_v1.bin").delete()
        File(context.filesDir, "parsed_epg_v1.tmp").delete()
        File(context.filesDir, "parsed_epg_v2.bin").delete()
        File(context.filesDir, "parsed_epg_v2.tmp").delete()
        File(context.filesDir, "parsed_epg_v3.bin").delete()
        File(context.filesDir, "parsed_epg_v3.tmp").delete()
    }
}

internal object EpgCacheCodec {
    private const val MAGIC = 0x46544550 // FTEP
    private const val VERSION = 4
    private const val MAX_CHANNELS = 100_000
    private const val MAX_PROGRAMMES_PER_CHANNEL = 100_000
    private const val MAX_STRING_BYTES = 4 * 1024 * 1024

    fun write(output: OutputStream, fingerprint: String, guide: EpgGuide) {
        DataOutputStream(FastGzipOutputStream(BufferedOutputStream(output))).use { data ->
            data.writeInt(MAGIC)
            data.writeInt(VERSION)
            data.writeString(fingerprint)
            data.writeInt(guide.channels.size)
            guide.channels.values.forEach { channel ->
                data.writeString(channel.id)
                data.writeNullableString(channel.displayName)
                data.writeNullableString(channel.iconUrl)
            }
            data.writeInt(guide.programmes.size)
            guide.programmes.forEach { (channelId, programmes) ->
                data.writeString(channelId)
                data.writeInt(programmes.size)
                programmes.forEach { programme ->
                    data.writeString(programme.title)
                    data.writeNullableString(programme.description)
                    data.writeNullableString(programme.category)
                    data.writeLong(programme.startEpochMilli)
                    data.writeBoolean(programme.stopEpochMilli != null)
                    programme.stopEpochMilli?.let(data::writeLong)
                }
            }
        }
    }

    fun read(input: InputStream, expectedFingerprint: String): EpgGuide {
        DataInputStream(GZIPInputStream(BufferedInputStream(input))).use { data ->
            require(data.readInt() == MAGIC) { "Invalid parsed EPG cache" }
            require(data.readInt() == VERSION) { "Unsupported parsed EPG cache version" }
            require(data.readString() == expectedFingerprint) { "Stale parsed EPG cache" }
            val channelCount = data.readBoundedCount(MAX_CHANNELS)
            val channels = LinkedHashMap<String, EpgChannel>(channelCount)
            repeat(channelCount) {
                val id = data.readString()
                channels[id] = EpgChannel(id, data.readNullableString(), data.readNullableString())
            }
            val scheduleCount = data.readBoundedCount(MAX_CHANNELS)
            val programmes = LinkedHashMap<String, List<EpgProgramme>>(scheduleCount)
            repeat(scheduleCount) {
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
            return EpgGuide(channels, programmes)
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
