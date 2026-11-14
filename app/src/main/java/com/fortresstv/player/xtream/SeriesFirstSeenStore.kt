package com.fortresstv.player.xtream

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks when each VOD item (keyed by kind, source ID, and source-local item ID) was first seen
 * on a post-baseline refresh. The first successful import of a source establishes a baseline
 * with no first-seen recency timestamp so initial catalogues do not flood "Recently Added" shelves.
 */
internal open class VodFirstSeenStore(
    private val directory: File?,
    private val kind: String
) {
    private data class StoreState(
        val baselineEstablished: Boolean,
        val entries: LinkedHashMap<Int, Long>
    )

    fun ensureBaseline(sourceId: String, itemIds: Collection<Int>) {
        val dir = directory ?: return
        synchronized(lockFor(kind, sourceId)) {
            val file = fileFor(dir, sourceId)
            val existing = readState(file)
            if (existing != null && existing.baselineEstablished) return
            val entries = LinkedHashMap<Int, Long>(itemIds.size)
            for (id in itemIds) {
                entries[id] = 0L
            }
            writeState(file, StoreState(baselineEstablished = true, entries = entries))
        }
    }

    fun recordImport(
        sourceId: String,
        itemIds: Collection<Int>,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Map<Int, Long> {
        val dir = directory ?: return emptyMap()
        return synchronized(lockFor(kind, sourceId)) {
            val file = fileFor(dir, sourceId)
            val existing = readState(file)
            if (existing == null || !existing.baselineEstablished) {
                val entries = LinkedHashMap<Int, Long>(itemIds.size)
                for (id in itemIds) {
                    entries[id] = 0L
                }
                writeState(file, StoreState(baselineEstablished = true, entries = entries))
                emptyMap()
            } else {
                val entries = existing.entries
                var changed = false
                for (id in itemIds) {
                    if (!entries.containsKey(id)) {
                        entries[id] = nowEpochMs
                        changed = true
                    }
                }
                if (changed) {
                    writeState(file, StoreState(baselineEstablished = true, entries = entries))
                }
                entries.filterValues { it > 0L }
            }
        }
    }

    fun loadFirstSeen(sourceId: String): Map<Int, Long> {
        val dir = directory ?: return emptyMap()
        return synchronized(lockFor(kind, sourceId)) {
            val state = readState(fileFor(dir, sourceId)) ?: return emptyMap()
            if (!state.baselineEstablished) return emptyMap()
            state.entries.filterValues { it > 0L }
        }
    }

    fun clear(sourceId: String) {
        val dir = directory ?: return
        synchronized(lockFor(kind, sourceId)) {
            fileFor(dir, sourceId).delete()
        }
    }

    private fun readState(file: File): StoreState? {
        if (!file.isFile || file.length() < 9L) return null
        return try {
            DataInputStream(BufferedInputStream(file.inputStream(), 32 * 1024)).use { input ->
                if (input.readInt() != MAGIC || input.readInt() != VERSION) return null
                val baseline = input.readBoolean()
                val count = input.readInt()
                if (count !in 0..MAX_ITEMS) return null
                val entries = LinkedHashMap<Int, Long>(count)
                repeat(count) {
                    val id = input.readInt()
                    val firstSeenMs = input.readLong()
                    entries[id] = firstSeenMs
                }
                if (input.read() != -1) return null
                StoreState(baselineEstablished = baseline, entries = entries)
            }
        } catch (_: IOException) {
            null
        }
    }

    private fun writeState(file: File, state: StoreState) {
        val dir = file.parentFile ?: return
        dir.mkdirs()
        val temp = File.createTempFile("${kind}_seen_", ".tmp", dir)
        try {
            DataOutputStream(BufferedOutputStream(temp.outputStream(), 32 * 1024)).use { output ->
                output.writeInt(MAGIC)
                output.writeInt(VERSION)
                output.writeBoolean(state.baselineEstablished)
                output.writeInt(state.entries.size)
                state.entries.forEach { (id, firstSeenMs) ->
                    output.writeInt(id)
                    output.writeLong(firstSeenMs)
                }
            }
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            temp.delete()
        }
    }

    private fun fileFor(dir: File, sourceId: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(sourceId.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(32)
        return File(dir, "${kind}_first_seen_v1_$digest.dat")
    }

    companion object {
        private const val MAGIC = 0x46545346 // "FTSF"
        private const val VERSION = 1
        private const val MAX_ITEMS = 5_000_000
        private val locks = ConcurrentHashMap<String, Any>()

        private fun lockFor(kind: String, sourceId: String): Any = locks.computeIfAbsent("$kind:$sourceId") { Any() }
    }
}

internal class SeriesFirstSeenStore(directory: File?) : VodFirstSeenStore(directory, "series")

internal class MoviesFirstSeenStore(directory: File?) : VodFirstSeenStore(directory, "movies")
