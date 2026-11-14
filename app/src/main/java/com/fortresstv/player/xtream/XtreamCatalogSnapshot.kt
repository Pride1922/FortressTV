package com.fortresstv.player.xtream

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * One live stream as listed by the provider, without the stream URL. The URL embeds the account
 * credentials, so it is rebuilt from [streamId] when the catalogue is loaded and never stored.
 */
internal data class LiveStreamRecord(
    /** Index among the object elements of the provider's array (the original channel position). */
    val position: Int,
    val streamId: Int,
    val name: String,
    val categoryId: String?,
    val logoUrl: String?,
    val tvgId: String?,
    val tvgName: String?,
    val containerExtension: String?,
    val hasCatchup: Boolean,
    val catchupDays: Int
)

/**
 * Compact binary cache of the parsed Xtream list endpoints. Reading it at startup replaces
 * re-parsing tens of MB of JSON on every launch. Files are written atomically; any corrupt,
 * truncated or foreign file reads as "no cache" so the catalogue is simply downloaded again.
 * Only model fields are stored: no credentials, no direct_source URLs.
 */
internal object XtreamCatalogSnapshot {
    private const val MAGIC = 0x46545843 // "FTXC"
    private const val VERSION = 2
    private const val KIND_CATEGORIES: Byte = 1
    private const val KIND_LIVE: Byte = 2
    private const val KIND_MOVIES: Byte = 3
    private const val KIND_SERIES: Byte = 4
    private const val MAX_ITEMS = 5_000_000
    private const val MAX_STRING_BYTES = 4 * 1024 * 1024

    fun writeCategories(file: File, categories: List<XtreamCategory>) = write(file, KIND_CATEGORIES) {
        writeInt(categories.size)
        categories.forEach {
            writeString(it.id)
            writeString(it.name)
        }
    }

    fun readCategories(file: File): List<XtreamCategory>? = read(file, KIND_CATEGORIES) {
        List(readCount()) { XtreamCategory(readString(), readString()) }
    }

    /** [scheme] is the protocol that actually served the list (https may have fallen back to http). */
    fun writeLive(file: File, scheme: String, streams: List<LiveStreamRecord>) = write(file, KIND_LIVE) {
        writeString(scheme)
        writeInt(streams.size)
        streams.forEach {
            writeInt(it.position)
            writeInt(it.streamId)
            writeString(it.name)
            writeNullableString(it.categoryId)
            writeNullableString(it.logoUrl)
            writeNullableString(it.tvgId)
            writeNullableString(it.tvgName)
            writeNullableString(it.containerExtension)
            writeBoolean(it.hasCatchup)
            writeInt(it.catchupDays)
        }
    }

    fun readLive(file: File): Pair<String, List<LiveStreamRecord>>? = read(file, KIND_LIVE) {
        val scheme = readString()
        if (scheme != "http" && scheme != "https") throw IOException("Unknown scheme")
        scheme to List(readCount()) {
            LiveStreamRecord(
                position = readInt(),
                streamId = readInt(),
                name = readString(),
                categoryId = readNullableString(),
                logoUrl = readNullableString(),
                tvgId = readNullableString(),
                tvgName = readNullableString(),
                containerExtension = readNullableString(),
                hasCatchup = readBoolean(),
                catchupDays = readInt()
            )
        }
    }

    fun writeMovies(file: File, movies: List<XtreamMovie>) = write(file, KIND_MOVIES) {
        writeInt(movies.size)
        movies.forEach {
            writeInt(it.id)
            writeString(it.name)
            writeNullableString(it.categoryId)
            writeNullableString(it.posterUrl)
            writeString(it.containerExtension)
            writeNullableString(it.rating)
            writeNullableString(it.year)
            writeNullableString(it.plot)
            writeNullableString(it.ageRating)
            writeNullableString(it.tmdbId)
            writeNullableString(it.imdbId)
            writeNullableLong(it.addedEpochMs)
        }
    }

    fun readMovies(file: File): List<XtreamMovie>? = read(file, KIND_MOVIES) { version ->
        List(readCount()) {
            XtreamMovie(
                id = readInt(),
                name = readString(),
                categoryId = readNullableString(),
                posterUrl = readNullableString(),
                containerExtension = readString(),
                rating = readNullableString(),
                year = readNullableString(),
                plot = readNullableString(),
                ageRating = readNullableString(),
                tmdbId = readNullableString(),
                imdbId = readNullableString(),
                addedEpochMs = if (version >= 2) readNullableLong() else null
            )
        }
    }

    fun writeSeries(file: File, series: List<XtreamSeries>) = write(file, KIND_SERIES) {
        writeInt(series.size)
        series.forEach {
            writeInt(it.id)
            writeString(it.name)
            writeNullableString(it.categoryId)
            writeNullableString(it.coverUrl)
            writeNullableString(it.rating)
            writeNullableString(it.year)
            writeNullableString(it.plot)
            writeNullableString(it.ageRating)
            writeNullableString(it.tmdbId)
            writeNullableString(it.imdbId)
            writeNullableLong(it.addedEpochMs)
        }
    }

    fun readSeries(file: File): List<XtreamSeries>? = read(file, KIND_SERIES) { version ->
        List(readCount()) {
            XtreamSeries(
                id = readInt(),
                name = readString(),
                categoryId = readNullableString(),
                coverUrl = readNullableString(),
                rating = readNullableString(),
                year = readNullableString(),
                plot = readNullableString(),
                ageRating = readNullableString(),
                tmdbId = readNullableString(),
                imdbId = readNullableString(),
                addedEpochMs = if (version >= 2) readNullableLong() else null
            )
        }
    }

    private fun write(file: File, kind: Byte, body: DataOutputStream.() -> Unit) {
        val directory = file.parentFile ?: throw IOException("No cache directory")
        val temp = File.createTempFile("xtream_", ".tmp", directory)
        try {
            DataOutputStream(BufferedOutputStream(temp.outputStream(), 64 * 1024)).use { output ->
                output.writeInt(MAGIC)
                output.writeInt(VERSION)
                output.writeByte(kind.toInt())
                output.body()
            }
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            temp.delete()
        }
    }

    private fun <T> read(file: File, kind: Byte, body: DataInputStream.(Int) -> T): T? {
        if (!file.isFile || file.length() < 9L) return null
        return try {
            DataInputStream(BufferedInputStream(file.inputStream(), 64 * 1024)).use { input ->
                if (input.readInt() != MAGIC) return null
                val version = input.readInt()
                if (version !in 1..VERSION || input.readByte() != kind) return null
                val value = input.body(version)
                // Trailing bytes mean the file is not what we wrote.
                if (input.read() != -1) return null
                value
            }
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
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

    private fun DataOutputStream.writeNullableLong(value: Long?) {
        writeBoolean(value != null)
        if (value != null) writeLong(value)
    }

    private fun DataInputStream.readCount(): Int = readInt().also {
        if (it !in 0..MAX_ITEMS) throw IOException("Invalid item count")
    }

    private fun DataInputStream.readString(): String {
        val size = readInt()
        if (size !in 0..MAX_STRING_BYTES) throw IOException("Invalid string length")
        val bytes = ByteArray(size)
        readFully(bytes)
        return String(bytes, Charsets.UTF_8)
    }

    private fun DataInputStream.readNullableString(): String? = if (readBoolean()) readString() else null

    private fun DataInputStream.readNullableLong(): Long? = if (readBoolean()) readLong() else null
}
