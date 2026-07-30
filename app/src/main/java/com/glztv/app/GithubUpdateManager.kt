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
        val response = client.newCall(
            Request.Builder()
                .url(update.downloadUrl)
                .header("User-Agent", "GLZ-TV/${BuildConfig.VERSION_NAME}")
                .build()
        ).execute()
        check(response.isSuccessful) { "Download failed (${response.code})" }
        val directory = File(context.cacheDir, "updates").apply {
            deleteRecursively()
            mkdirs()
        }
        val apk = File(directory, "Glz-TV-${update.version}.apk")
        response.body?.byteStream()?.use { input ->
            apk.outputStream().use { output -> input.copyTo(output) }
        } ?: error("GitHub returned an empty APK")
        check(apk.length() > 1_000_000) { "Downloaded APK is incomplete" }
        val packageInfo = context.packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
            ?: error("Downloaded file is not a valid APK")
        check(packageInfo.packageName == context.packageName) {
            "Downloaded APK has the wrong package name"
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
