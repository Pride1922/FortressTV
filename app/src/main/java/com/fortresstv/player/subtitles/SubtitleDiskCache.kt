package com.fortresstv.player.subtitles

import android.content.Context
import java.io.File
import java.io.InputStream

class SubtitleDiskCache(private val cacheDir: File) {

    constructor(context: Context) : this(File(context.cacheDir, "subtitles"))

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    fun subtitleFile(cacheKey: String): File {
        val safeKey = cacheKey.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(cacheDir, "$safeKey.srt")
    }

    fun hasSubtitle(cacheKey: String): Boolean {
        val file = subtitleFile(cacheKey)
        return file.exists() && file.length() > 0
    }

    fun saveSubtitle(cacheKey: String, content: String): File {
        val file = subtitleFile(cacheKey)
        file.writeText(content, Charsets.UTF_8)
        return file
    }

    fun saveSubtitle(cacheKey: String, inputStream: InputStream): File {
        val file = subtitleFile(cacheKey)
        file.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        return file
    }

    fun clearCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }
}
