package com.fortresstv.player.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
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
import java.security.MessageDigest

data class AppUpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val releaseNotes: String,
    val downloadUrl: String,
    val sha256: String,
    val htmlUrl: String = ""
)

object UpdateManager {
    private const val INSTALL_STATE = "fortresstv_update_install"
    private const val PENDING_APK_PATH = "pending_apk_path"
    private const val INSTALLER_LAUNCHED = "installer_launched"

    const val GITHUB_VERSION_URL = "https://raw.githubusercontent.com/Pride1922/FortressTV/main/version.json"
    const val GITHUB_RELEASE_VERSION_URL = "https://github.com/Pride1922/FortressTV/releases/latest/download/version.json"

    var updateEndpointUrl: String = GITHUB_VERSION_URL
    var fallbackEndpointUrl: String = GITHUB_RELEASE_VERSION_URL

    suspend fun checkForUpdates(
        currentVersionCode: Int = BuildConfig.VERSION_CODE,
        endpointUrl: String = updateEndpointUrl
    ): AppUpdateInfo? = withContext(Dispatchers.IO) {
        val primary = queryEndpoint(endpointUrl, currentVersionCode)
        if (primary != null) return@withContext primary
        if (endpointUrl == updateEndpointUrl && fallbackEndpointUrl.isNotBlank() && fallbackEndpointUrl != endpointUrl) {
            return@withContext queryEndpoint(fallbackEndpointUrl, currentVersionCode)
        }
        null
    }

    private fun queryEndpoint(endpointUrl: String, currentVersionCode: Int): AppUpdateInfo? {
        return runCatching {
            val url = URL(endpointUrl)
            require(url.protocol.equals("https", true)) { "Update checks require HTTPS." }
            var connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("User-Agent", "FortressTV-App")
                instanceFollowRedirects = true
            }

            var redirectCount = 0
            while (connection.responseCode in listOf(301, 302, 303, 307, 308) && redirectCount < 5) {
                val newUrl = URL(connection.url, connection.getHeaderField("Location"))
                require(newUrl.protocol.equals("https", true)) { "Update redirects must use HTTPS." }
                connection.disconnect()
                connection = (newUrl.openConnection() as HttpURLConnection).apply {
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
            val targetVersionCode = json.optInt("versionCode", -1)
            val releaseNotes = json.optString("notes", json.optString("body", "")).trim()
            val htmlUrl = json.optString("html_url", "")
            val sha256 = json.optString("sha256", json.optString("apkSha256", "")).trim().lowercase()

            if (tagName.isBlank() || targetVersionCode <= currentVersionCode) {
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
                versionCode = targetVersionCode,
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                sha256 = sha256,
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
        expectedSha256: String,
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val updateDir = context.cacheDir.resolve("updates").apply { mkdirs() }
        val apkFile = File(updateDir, "FortressTV-update.apk").apply { delete() }
        runCatching {
            val url = URL(downloadUrl)
            require(url.protocol.equals("https", true)) { "Updates must be downloaded over HTTPS." }
            require(expectedSha256.matches(Regex("[0-9a-fA-F]{64}"))) {
                "The update metadata does not contain a valid SHA-256 checksum."
            }
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
                val newUrl = URL(finalConnection.url, finalConnection.getHeaderField("Location"))
                require(newUrl.protocol.equals("https", true)) { "Update redirects must use HTTPS." }
                finalConnection.disconnect()
                finalConnection = (newUrl.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "FortressTV-App")
                }
                redirectCount++
            }

            require(finalConnection.responseCode == HttpURLConnection.HTTP_OK) {
                "Update download failed with HTTP ${finalConnection.responseCode}."
            }
            val totalBytes = finalConnection.contentLengthLong
            val maxUpdateBytes = 100L * 1024L * 1024L
            require(totalBytes <= 0L || totalBytes <= maxUpdateBytes) { "The update file is too large." }
            finalConnection.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        require(totalRead <= maxUpdateBytes) { "The update file is too large." }
                        if (totalBytes > 0) {
                            val progress = (totalRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            withContext(Dispatchers.Main) { onProgress(progress) }
                        }
                    }
                }
            }

            val actualSha256 = apkFile.inputStream().use { input ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
            require(actualSha256.equals(expectedSha256, true)) {
                apkFile.delete()
                "The update checksum does not match. The file was deleted."
            }
            verifyDownloadedApk(context, apkFile)

            withContext(Dispatchers.Main) {
                installApk(context, apkFile)
            }
        }.onFailure { apkFile.delete() }
    }

    @Suppress("DEPRECATION")
    private fun verifyDownloadedApk(context: Context, apkFile: File) {
        val packageManager = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Some Android 9 TV package managers leave SigningInfo empty for archive APKs.
            // Request the legacy signatures too so the same certificate can still be verified.
            PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_SIGNATURES
        } else {
            PackageManager.GET_SIGNATURES
        }
        val archiveInfo = packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags)
            ?: run {
                apkFile.delete()
                error("The downloaded file is not a valid APK.")
            }
        require(archiveInfo.packageName == context.packageName) {
            apkFile.delete()
            "The update belongs to a different application. The file was deleted."
        }
        val installedInfo = packageManager.getPackageInfo(context.packageName, flags)
        require(versionCode(archiveInfo) > versionCode(installedInfo)) {
            apkFile.delete()
            "The downloaded APK is not newer than the installed version."
        }
        val archiveCertificates = signingCertificates(archiveInfo)
        val installedCertificates = signingCertificates(installedInfo)
        require(archiveCertificates.isNotEmpty() && archiveCertificates == installedCertificates) {
            apkFile.delete()
            "The update signature does not match FortressTV. The file was deleted."
        }
    }

    @Suppress("DEPRECATION")
    private fun versionCode(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()

    @Suppress("DEPRECATION")
    private fun signingCertificates(info: PackageInfo): Set<String> {
        val modernSignatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners.orEmpty()
        } else emptyArray()
        val signatures = modernSignatures.takeIf { it.isNotEmpty() } ?: info.signatures.orEmpty()
        return signatures.mapTo(mutableSetOf()) { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }
    }

    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) return

        savePendingInstall(context, apkFile, installerLaunched = false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                return
            }
        }

        launchPendingInstaller(context, apkFile)
    }

    fun resumePendingInstall(context: Context) {
        val apkFile = findPendingInstallToResume(context) ?: return
        launchResumedInstall(context, apkFile)
    }

    /**
     * The disk half of [resumePendingInstall]: returns the downloaded update that is still
     * waiting for the installer, already verified, or null. Safe to call off the main thread.
     */
    fun findPendingInstallToResume(context: Context): File? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) return null

        val preferences = context.getSharedPreferences(INSTALL_STATE, Context.MODE_PRIVATE)
        val pendingPath = preferences.getString(PENDING_APK_PATH, null) ?: return null
        if (preferences.getBoolean(INSTALLER_LAUNCHED, false)) return null
        val apkFile = File(pendingPath)
        if (!apkFile.exists()) {
            clearPendingInstall(context)
            return null
        }

        return runCatching {
            verifyDownloadedApk(context, apkFile)
            apkFile
        }.getOrElse {
            clearPendingInstall(context)
            apkFile.delete()
            null
        }
    }

    /** The UI half of [resumePendingInstall]: opens the installer for a file from [findPendingInstallToResume]. */
    fun launchResumedInstall(context: Context, apkFile: File) {
        runCatching {
            launchPendingInstaller(context, apkFile)
        }.onFailure {
            clearPendingInstall(context)
            apkFile.delete()
        }
    }

    fun retryPendingInstall(context: Context): Result<Unit> = runCatching {
        val preferences = context.getSharedPreferences(INSTALL_STATE, Context.MODE_PRIVATE)
        val pendingPath = preferences.getString(PENDING_APK_PATH, null)
            ?: error("The downloaded update is no longer available. Please download it again.")
        val apkFile = File(pendingPath)
        require(apkFile.exists()) { "The downloaded update is no longer available. Please download it again." }
        verifyDownloadedApk(context, apkFile)
        installApk(context, apkFile)
    }.onFailure {
        if (it !is SecurityException) {
            clearPendingInstall(context)
        }
    }

    fun hasPendingInstall(context: Context): Boolean {
        val preferences = context.getSharedPreferences(INSTALL_STATE, Context.MODE_PRIVATE)
        val pendingPath = preferences.getString(PENDING_APK_PATH, null) ?: return false
        val apkFile = File(pendingPath)
        return runCatching {
            require(apkFile.exists())
            verifyDownloadedApk(context, apkFile)
            true
        }.getOrElse {
            clearPendingInstall(context)
            apkFile.delete()
            false
        }
    }

    private fun savePendingInstall(context: Context, apkFile: File, installerLaunched: Boolean) {
        context.getSharedPreferences(INSTALL_STATE, Context.MODE_PRIVATE)
            .edit()
            .putString(PENDING_APK_PATH, apkFile.absolutePath)
            .putBoolean(INSTALLER_LAUNCHED, installerLaunched)
            .apply()
    }

    private fun launchPendingInstaller(context: Context, apkFile: File) {
        savePendingInstall(context, apkFile, installerLaunched = true)
        launchInstaller(context, apkFile)
    }

    private fun clearPendingInstall(context: Context) {
        context.getSharedPreferences(INSTALL_STATE, Context.MODE_PRIVATE)
            .edit().remove(PENDING_APK_PATH).remove(INSTALLER_LAUNCHED).apply()
    }

    private fun launchInstaller(context: Context, apkFile: File) {
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
