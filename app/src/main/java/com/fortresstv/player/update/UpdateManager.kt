package com.fortresstv.player.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.fortresstv.player.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val htmlUrl: String = ""
)

object UpdateManager {
    // URL pointing to your Nextcloud version.json
    var updateEndpointUrl: String = "https://cloud.ameyerocha.com/s/2TYXawD6kCQ62iN/download"

    suspend fun checkForUpdates(
        currentVersion: String = BuildConfig.VERSION_NAME,
        endpointUrl: String = updateEndpointUrl
    ): AppUpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(endpointUrl)
            var connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("User-Agent", "FortressTV-App")
                instanceFollowRedirects = true
            }

            var redirectCount = 0
            while (connection.responseCode in listOf(301, 302, 303, 307, 308) && redirectCount < 5) {
                val newUrl = connection.getHeaderField("Location")
                connection.disconnect()
                connection = (URL(newUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("User-Agent", "FortressTV-App")
                }
                redirectCount++
            }

            if (connection.responseCode != 200) return@runCatching null

            val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonText)

            val tagName = json.optString("version", json.optString("tag_name", "")).removePrefix("v").removePrefix("V").trim()
            val releaseNotes = json.optString("notes", json.optString("body", "")).trim()
            val htmlUrl = json.optString("html_url", "")

            if (tagName.isBlank() || !isNewerVersion(currentVersion, tagName)) {
                return@runCatching null
            }

            var downloadUrl = json.optString("downloadUrl", json.optString("apkUrl", "")).trim()
            if (downloadUrl.isBlank()) {
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val assetName = asset.optString("name", "")
                        if (assetName.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = asset.optString("browser_download_url", "")
                            if (assetName.contains("release", ignoreCase = true)) break
                        }
                    }
                }
            }

            if (downloadUrl.isBlank()) {
                downloadUrl = htmlUrl
            }

            AppUpdateInfo(
                versionName = tagName,
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                htmlUrl = htmlUrl
            )
        }.getOrNull()
    }

    fun isNewerVersion(current: String, target: String): Boolean {
        val currentParts = current.split("-", "_")[0].split(".").mapNotNull { it.toIntOrNull() }
        val targetParts = target.split("-", "_")[0].split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(currentParts.size, targetParts.size)
        for (i in 0 until maxLen) {
            val cur = currentParts.getOrElse(i) { 0 }
            val tgt = targetParts.getOrElse(i) { 0 }
            if (tgt > cur) return true
            if (tgt < cur) return false
        }
        return false
    }

    suspend fun downloadAndInstall(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                setRequestProperty("User-Agent", "FortressTV-App")
                instanceFollowRedirects = true
            }

            // Handle manual redirect if needed
            var finalConnection = connection
            var redirectCount = 0
            while (finalConnection.responseCode in listOf(301, 302, 303, 307, 308) && redirectCount < 5) {
                val newUrl = finalConnection.getHeaderField("Location")
                finalConnection.disconnect()
                finalConnection = (URL(newUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "FortressTV-App")
                }
                redirectCount++
            }

            val totalBytes = finalConnection.contentLengthLong
            val updateDir = context.cacheDir.resolve("updates").apply { mkdirs() }
            val apkFile = File(updateDir, "FortressTV-update.apk")
            if (apkFile.exists()) apkFile.delete()

            finalConnection.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            val progress = (totalRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            withContext(Dispatchers.Main) { onProgress(progress) }
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                installApk(context, apkFile)
            }
        }
    }

    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
            }
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }
}
