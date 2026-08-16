package com.glztv.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.net.URLDecoder

object GithubUpdateManager {
    private const val LATEST_RELEASE =
        "https://api.github.com/repos/yadielglz/glz-tv/releases/latest"
    private const val APK_MIME = "application/vnd.android.package-archive"

    data class UpdateInfo(
        val version: String,
        val downloadUrl: String,
        val releaseUrl: String,
        val notes: String
    )

    fun check(client: OkHttpClient): UpdateInfo? {
        val response = client.newCall(
            Request.Builder()
                .url(LATEST_RELEASE)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "GLZ-TV/${BuildConfig.VERSION_NAME}")
                .build()
        ).execute()
        val text = response.body?.string().orEmpty()
        check(response.isSuccessful) { "GitHub returned ${response.code}" }
        val release = JSONObject(text)
        val version = release.getString("tag_name").removePrefix("v")
        if (compareVersions(version, BuildConfig.VERSION_NAME.substringBefore("-")) <= 0) return null
        val assets = release.getJSONArray("assets")
        val asset = (0 until assets.length())
            .map { assets.getJSONObject(it) }
            .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
            ?: error("Release $version does not include an APK")
        return UpdateInfo(
            version = version,
            downloadUrl = asset.getString("browser_download_url"),
            releaseUrl = release.optString("html_url"),
            notes = release.optString("body").take(1_200)
        )
    }

    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()

    fun requestInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                )
            )
        }
    }

    fun download(context: Context, client: OkHttpClient, update: UpdateInfo): File {
        return downloadApk(context, client, update.downloadUrl, context.packageName,
            "Glz-TV-${update.version}.apk")
    }

    fun downloadApk(
        context: Context,
        client: OkHttpClient,
        downloadUrl: String,
        expectedPackageName: String,
        suggestedName: String? = null,
        requestHeaders: Map<String, String> = emptyMap(),
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): File {
        val request = Request.Builder()
            .url(downloadUrl)
            .header("User-Agent", "GLZ-TV/${BuildConfig.VERSION_NAME}")
            .apply { requestHeaders.forEach { (name, value) -> header(name, value) } }
            .build()
        val apk = client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Download failed (${response.code})" }
            val directory = File(context.cacheDir, "updates").apply {
                deleteRecursively()
                mkdirs()
            }
            val remoteName = response.header("Content-Disposition")
                ?.substringAfter("filename=", "")
                ?.trim(' ', '"')
                ?.takeIf(String::isNotBlank)
                ?: runCatching {
                    URLDecoder.decode(response.request.url.pathSegments.lastOrNull(), "UTF-8")
                }.getOrNull()
            val safeName = (suggestedName ?: remoteName ?: "managed-app.apk")
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
                .let { if (it.endsWith(".apk", true)) it else "$it.apk" }
            val target = File(directory, safeName)
            val body = response.body ?: error("Server returned an empty APK")
            val totalBytes = body.contentLength()
            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var bytesRead = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        bytesRead += count
                        onProgress(bytesRead, totalBytes)
                    }
                }
            }
            target
        }
        check(apk.length() > 0) { "Downloaded APK is empty" }
        val packageInfo = context.packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
            ?: error("Downloaded file is not a valid APK")
        check(packageInfo.packageName == expectedPackageName) {
            "Downloaded APK is ${packageInfo.packageName}, expected $expectedPackageName"
        }
        return apk
    }

    fun launchInstaller(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.updates",
            apk
        )
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, APK_MIME)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    internal fun compareVersions(left: String, right: String): Int {
        val leftParts = left.split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        val rightParts = right.split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        return (0 until maxOf(leftParts.size, rightParts.size))
            .firstNotNullOfOrNull { index ->
                val comparison = (leftParts.getOrNull(index) ?: 0)
                    .compareTo(rightParts.getOrNull(index) ?: 0)
                comparison.takeIf { it != 0 }
            } ?: 0
    }
}
