package com.fortresstv.player.source

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SourceReader {
    const val CACHE_EXPIRY_MILLIS = 24 * 60 * 60 * 1000L // 24 hours

    private fun cacheFile(context: Context, type: String): File {
        return File(context.filesDir, "cached_$type.dat")
    }

    fun hasCache(context: Context, type: String): Boolean {
        val file = cacheFile(context, type)
        return file.exists() && file.length() > 0
    }

    fun isCacheFresh(context: Context, type: String, lastUpdate: Long): Boolean {
        if (!hasCache(context, type)) return false
        val now = System.currentTimeMillis()
        return (now - lastUpdate) < CACHE_EXPIRY_MILLIS
    }

    fun openCached(context: Context, type: String): InputStream {
        val file = cacheFile(context, type)
        if (!file.exists() || file.length() == 0L) error("No cached data for $type")
        return BufferedInputStream(FileInputStream(file))
    }

    suspend fun fetchAndCache(context: Context, source: ContentSource, type: String): InputStream = withContext(Dispatchers.IO) {
        when (source) {
            is ContentSource.LocalFile -> {
                open(context, source)
            }
            is ContentSource.RemoteUrl -> {
                val targetFile = cacheFile(context, type)
                val tempFile = File(context.filesDir, "temp_$type.dat")
                retryTransientNetwork {
                    openUrl(source.value).use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
                BufferedInputStream(FileInputStream(targetFile))
            }
            is ContentSource.Xtream -> error("Xtream sources use the catalogue API")
        }
    }

    fun deleteCache(context: Context, type: String) {
        cacheFile(context, type).delete()
    }

    /** A cheap identity for cache invalidation; null means the provider cannot be trusted. */
    fun fingerprint(context: Context, source: ContentSource, type: String): String? = when (source) {
        is ContentSource.RemoteUrl -> cacheFile(context, type).takeIf { it.exists() && it.length() > 0L }?.let {
            "remote:${source.value}:${it.length()}:${it.lastModified()}"
        }
        is ContentSource.LocalFile -> runCatching {
            val uri = Uri.parse(source.value)
            context.contentResolver.query(
                uri,
                arrayOf(DocumentsContract.Document.COLUMN_SIZE, DocumentsContract.Document.COLUMN_LAST_MODIFIED),
                null,
                null,
                null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null
                val modified = if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) cursor.getLong(modifiedIndex) else null
                if (size == null || modified == null || modified <= 0L) null else "local:${source.value}:$size:$modified"
            }
        }.getOrNull()
        is ContentSource.Xtream -> null
    }

    suspend fun open(context: Context, source: ContentSource): InputStream = withContext(Dispatchers.IO) {
        when (source) {
            is ContentSource.LocalFile -> context.contentResolver.openInputStream(Uri.parse(source.value))
                ?: error("The saved file is no longer available")
            is ContentSource.RemoteUrl -> openUrl(source.value)
            is ContentSource.Xtream -> error("Xtream sources use the catalogue API")
        }
    }

    private fun openUrl(value: String): InputStream {
        val uri = URI(value)
        require(uri.scheme.equals("https", true) || uri.scheme.equals("http", true)) {
            "Only HTTP and HTTPS URLs are supported"
        }
        val connection = uri.toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("Accept-Encoding", "gzip")
        connection.connect()
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            connection.disconnect()
            throw HttpStatusException(responseCode)
        }
        return BufferedInputStream(connection.inputStream)
    }
}
