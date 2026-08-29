package com.fortresstv.player.source

import android.content.Context
import android.net.Uri
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI

object SourceReader {
    fun open(context: Context, source: ContentSource): InputStream = when (source) {
        is ContentSource.LocalFile -> context.contentResolver.openInputStream(Uri.parse(source.value))
            ?: error("The saved file is no longer available")
        is ContentSource.RemoteUrl -> openUrl(source.value)
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
            error("Server returned HTTP $responseCode")
        }
        return BufferedInputStream(connection.inputStream)
    }
}
