package com.fortresstv.player.playlist

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
import java.security.MessageDigest
import java.util.zip.Deflater
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object PlaylistDiskCache {
    fun load(context: Context, sourceId: String, fingerprint: String?): Playlist? {
        if (fingerprint == null) return null
        val file = cacheFile(context, sourceId)
        if (!file.isFile || file.length() == 0L) return null
        return runCatching {
            file.inputStream().use { PlaylistCacheCodec.read(it, fingerprint) }
        }.getOrElse {
            file.delete()
            null
        }
    }

    fun save(context: Context, sourceId: String, fingerprint: String?, playlist: Playlist) {
        if (fingerprint == null) return
        val target = cacheFile(context, sourceId)
        val temporary = File(target.parentFile, "${target.name}.tmp")
        runCatching {
            temporary.outputStream().use { PlaylistCacheCodec.write(it, fingerprint, playlist) }
            Files.move(
                temporary.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        }.onFailure { temporary.delete() }
    }

    fun clear(context: Context, sourceId: String) {
        val target = cacheFile(context, sourceId)
        target.delete()
        File(target.parentFile, "${target.name}.tmp").delete()
    }

    private fun cacheFile(context: Context, sourceId: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(sourceId.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return File(context.filesDir, "parsed_playlist_v1_$digest.bin")
    }
}

internal object PlaylistCacheCodec {
    private const val MAGIC = 0x4654504C // FTPL
    private const val VERSION = 2
    private const val MAX_CHANNELS = 250_000
    private const val MAX_STRING_BYTES = 4 * 1024 * 1024

    fun write(output: OutputStream, fingerprint: String, playlist: Playlist) {
        DataOutputStream(PlaylistGzipOutputStream(BufferedOutputStream(output))).use { data ->
            data.writeInt(MAGIC)
            data.writeInt(VERSION)
            data.writeString(fingerprint)
            data.writeInt(playlist.channels.size)
            playlist.channels.forEach { channel ->
                data.writeInt(channel.position)
                data.writeString(channel.name)
                data.writeString(channel.streamUrl)
                data.writeNullableString(channel.tvgId)
                data.writeNullableString(channel.tvgName)
                data.writeNullableString(channel.logoUrl)
                data.writeNullableString(channel.group)
                data.writeString(channel.sourceId)
                data.writeInt(channel.sourcePosition)
                data.writeString(channel.sourceItemId)
                data.writeBoolean(channel.hasCatchup)
                data.writeInt(channel.catchupDays)
            }
        }
    }

    fun read(input: InputStream, expectedFingerprint: String): Playlist {
        DataInputStream(GZIPInputStream(BufferedInputStream(input))).use { data ->
            require(data.readInt() == MAGIC) { "Invalid parsed playlist cache" }
            require(data.readInt() == VERSION) { "Unsupported parsed playlist cache version" }
            require(data.readString() == expectedFingerprint) { "Stale parsed playlist cache" }
            val channelCount = data.readInt().also {
                require(it in 0..MAX_CHANNELS) { "Invalid parsed playlist channel count" }
            }
            return Playlist(List(channelCount) {
                PlaylistChannel(
                    position = data.readInt(),
                    name = data.readString(),
                    streamUrl = data.readString(),
                    tvgId = data.readNullableString(),
                    tvgName = data.readNullableString(),
                    logoUrl = data.readNullableString(),
                    group = data.readNullableString(),
                    sourceId = data.readString(),
                    sourcePosition = data.readInt(),
                    sourceItemId = data.readString(),
                    hasCatchup = data.readBoolean(),
                    catchupDays = data.readInt()
                )
            })
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
        require(size in 0..MAX_STRING_BYTES) { "Invalid parsed playlist string size" }
        return ByteArray(size).also(::readFully).toString(Charsets.UTF_8)
    }

    private fun DataInputStream.readNullableString(): String? = if (readBoolean()) readString() else null
}

private class PlaylistGzipOutputStream(output: OutputStream) : GZIPOutputStream(output) {
    init {
        def.setLevel(Deflater.BEST_SPEED)
    }
}
