package com.glztv.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLDecoder

object GithubUpdateManager {
    private const val RELEASES =
        "https://api.github.com/repos/yadielglz/glz-tv/releases?per_page=30"
    private const val APK_MIME = "application/vnd.android.package-archive"

    /**
     * Which GitHub release ring this install follows. The [marker] is matched against the
     * lowercased tag suffix (the part after the first `-`). Stable releases (no suffix) are
     * offered on every channel; a channel additionally accepts its own pre-release tags.
     */
    enum class UpdateChannel(val id: String, val label: String, val marker: String?) {
        PRODUCTION("production", "Production", null),
        FAMILY("family", "Family", "family"),
        BETA("beta", "Beta", "beta"),
        RELEASE_CANDIDATE("rc", "Release Candidate", "rc");

        companion object {
            fun from(id: String?): UpdateChannel =
                values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: PRODUCTION
        }
    }

    /**
     * The first release on the Production ring. Every stable release tagged before this one
     * belongs to the Family ring, so Production installs are never offered an older build.
     */
    internal const val PRODUCTION_FLOOR = "26.902.05"

    data class UpdateInfo(
        val version: String,
        val downloadUrl: String,
        val releaseUrl: String,
        val notes: String
    )

    fun check(client: OkHttpClient, channel: UpdateChannel = UpdateChannel.PRODUCTION): UpdateInfo? {
        val response = client.newCall(
            Request.Builder()
                .url(RELEASES)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "GLZ-TV/${BuildConfig.VERSION_NAME}")
                .build()
        ).execute()
        val text = response.body?.string().orEmpty()
        check(response.isSuccessful) { "GitHub returned ${response.code}" }
        val releases = JSONArray(text)
        val currentBase = BuildConfig.VERSION_NAME.substringBefore("-")
        val currentTag = "v${BuildConfig.VERSION_NAME}"

        // The newest published release on the selected channel. published_at is ISO-8601,
        // so lexical comparison matches chronological order.
        val newest = (0 until releases.length())
            .map { releases.getJSONObject(it) }
            .filterNot { it.optBoolean("draft", false) }
            .filter { matchesChannel(it.getString("tag_name"), channel) }
            .maxByOrNull { it.optString("published_at") }
            ?: return null

        val tag = newest.getString("tag_name")
        val version = tag.removePrefix("v")
        if (tag.equals(currentTag, ignoreCase = true)) return null
        // Never step onto an older base version (e.g. a Beta user must not be pulled back
        // to an older stable). Equal base is allowed so rc1 -> rc2 -> final still flows.
        if (compareVersions(version.substringBefore("-"), currentBase) < 0) return null
        val assets = newest.optJSONArray("assets") ?: return null
        val asset = (0 until assets.length())
            .map { assets.getJSONObject(it) }
            .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
            ?: return null
        return UpdateInfo(
            version = version,
            downloadUrl = asset.getString("browser_download_url"),
            releaseUrl = newest.optString("html_url"),
            notes = newest.optString("body").take(1_200)
        )
    }

    internal fun matchesChannel(tag: String, channel: UpdateChannel): Boolean {
        val version = tag.removePrefix("v")
        val base = version.substringBefore("-")
        val suffix = version.substringAfter("-", "").lowercase()
        val stable = suffix.isEmpty()
        return when (channel) {
            // Production: stable tags from the floor release onward only.
            UpdateChannel.PRODUCTION -> stable && compareVersions(base, PRODUCTION_FLOOR) >= 0
            // Family: every stable tag (including the pre-floor history) plus -family builds.
            UpdateChannel.FAMILY -> stable || suffix.startsWith("family")
            UpdateChannel.BETA -> stable || suffix.startsWith("beta")
            UpdateChannel.RELEASE_CANDIDATE -> stable || suffix.startsWith("rc")
        }
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
        val leftParts = left.split('.').map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val rightParts = right.split('.').map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        return (0 until maxOf(leftParts.size, rightParts.size))
            .firstNotNullOfOrNull { index ->
                val comparison = (leftParts.getOrNull(index) ?: 0)
                    .compareTo(rightParts.getOrNull(index) ?: 0)
                comparison.takeIf { it != 0 }
            } ?: 0
    }
}
