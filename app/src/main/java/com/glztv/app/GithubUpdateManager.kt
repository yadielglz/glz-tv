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
import java.util.Calendar
import java.util.TimeZone

object GithubUpdateManager {
    private const val RELEASES =
        "https://api.github.com/repos/yadielglz/glz-tv/releases?per_page=30"
    private const val APK_MIME = "application/vnd.android.package-archive"

    /**
     * Which GitHub release ring this install follows. The [markers] are matched against the
     * lowercased tag suffix (the part after the first `-`). Stable releases (no suffix) are
     * offered on every channel; a channel additionally accepts its own pre-release tags.
     */
    enum class UpdateChannel(
        val id: String,
        val label: String,
        val markers: List<String>,
        val legacyIds: List<String> = emptyList()
    ) {
        PRODUCTION(
            id = "production",
            label = "Production Stable",
            markers = emptyList(),
            legacyIds = listOf("prod")
        ),
        WEEKLY(
            id = "weekly",
            label = "Weekly",
            markers = listOf("weekly", "family"),
            legacyIds = listOf("family")
        ),
        BIWEEKLY(
            id = "biweekly",
            label = "Bi-Weekly",
            markers = listOf("biweekly", "rc"),
            legacyIds = listOf("rc")
        ),
        MONTHLY(
            id = "monthly",
            label = "Stable (Monthly)",
            markers = listOf("monthly")
        ),
        TESTING(
            id = "testing",
            label = "Testing",
            markers = listOf("testing", "beta"),
            legacyIds = listOf("beta")
        );

        val marker: String? get() = markers.firstOrNull()

        companion object {
            // Legacy enum aliases to preserve compatibility with existing code and tests
            val FAMILY get() = WEEKLY
            val BETA get() = TESTING
            val RELEASE_CANDIDATE get() = BIWEEKLY

            fun from(id: String?): UpdateChannel =
                values().firstOrNull { channel ->
                    channel.id.equals(id, ignoreCase = true) ||
                    channel.legacyIds.any { it.equals(id, ignoreCase = true) }
                } ?: PRODUCTION
        }
    }

    /**
     * The first release on the Production ring. Every stable release tagged before this one
     * belongs to the legacy Family ring, so Production installs are never offered an older build.
     */
    internal const val PRODUCTION_FLOOR = "26.902.05"

    data class MilestoneStatus(
        val milestoneDeadlineMillis: Long,
        val forceDeadlineMillis: Long,
        val isPastMilestone: Boolean,
        val isForceRequired: Boolean
    )

    data class UpdateInfo(
        val version: String,
        val downloadUrl: String,
        val releaseUrl: String,
        val notes: String,
        val publishedAt: String? = null
    )

    /**
     * Parse release date from a version name such as "2026.1016.15" or "2026.1016.05-family".
     * Returns timestamp in millis at start of day in GMT-5, or null if unparseable.
     */
    fun parseReleaseDate(versionName: String): Long? {
        val match = Regex("""^(?:v)?(\d{4})\.(\d{2})(\d{2})""").find(versionName) ?: return null
        val (yStr, mStr, dStr) = match.destructured
        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-5")).apply {
            set(Calendar.YEAR, yStr.toInt())
            set(Calendar.MONTH, mStr.toInt() - 1)
            set(Calendar.DAY_OF_MONTH, dStr.toInt())
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Calculates the monthly milestone deadline for a release on the Monthly channel.
     * The milestone deadline is the 9th of the month or the 2nd Friday of the month
     * at 23:59:00 GMT-5, whichever comes first in the cycle, ensuring a ~30-day
     * rolling period (e.g. 2026.1016.15 released 10/16 is valid to 11/13 23:59:00 GMT-5).
     */
    fun getMonthlyMilestoneDeadline(releaseDateMillis: Long): Long {
        val tz = TimeZone.getTimeZone("GMT-5")
        val cal = Calendar.getInstance(tz).apply { timeInMillis = releaseDateMillis }
        val releaseYear = cal.get(Calendar.YEAR)
        val releaseMonth = cal.get(Calendar.MONTH)

        // Check subsequent month
        var targetYear = releaseYear
        var targetMonth = releaseMonth + 1
        if (targetMonth > 11) {
            targetMonth = 0
            targetYear += 1
        }

        val d9 = Calendar.getInstance(tz).apply {
            set(Calendar.YEAR, targetYear)
            set(Calendar.MONTH, targetMonth)
            set(Calendar.DAY_OF_MONTH, 9)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val f2 = Calendar.getInstance(tz).apply {
            set(Calendar.YEAR, targetYear)
            set(Calendar.MONTH, targetMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            val firstDow = get(Calendar.DAY_OF_WEEK)
            val daysToFriday = (Calendar.FRIDAY - firstDow + 7) % 7
            val firstFriday = 1 + daysToFriday
            val secondFriday = firstFriday + 7
            set(Calendar.DAY_OF_MONTH, secondFriday)
        }.timeInMillis

        val earlier = minOf(d9, f2)
        val later = maxOf(d9, f2)

        // If the earlier date provides at least 25 days of life, use it.
        // If not (e.g. released 10/16 where Nov 9 is only 24 days away), use the 2nd Friday (11/13)
        // so it stays within the 30 rolling days milestone period.
        val twentyFiveDays = 25L * 24 * 60 * 60 * 1000L
        return if ((earlier - releaseDateMillis) >= twentyFiveDays) {
            earlier
        } else {
            later
        }
    }

    /**
     * Checks whether the current build on the Monthly channel is past its milestone deadline,
     * and if it is 7+ days overdue (triggering a mandatory force update).
     */
    fun checkMilestoneStatus(
        versionName: String = BuildConfig.VERSION_NAME,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): MilestoneStatus {
        val releaseDate = parseReleaseDate(versionName) ?: currentTimeMillis
        val milestoneDeadline = getMonthlyMilestoneDeadline(releaseDate)
        val sevenDays = 7L * 24 * 60 * 60 * 1000L
        val forceDeadline = milestoneDeadline + sevenDays

        val isPastMilestone = currentTimeMillis >= milestoneDeadline
        val isForceRequired = currentTimeMillis >= forceDeadline

        return MilestoneStatus(
            milestoneDeadlineMillis = milestoneDeadline,
            forceDeadlineMillis = forceDeadline,
            isPastMilestone = isPastMilestone,
            isForceRequired = isForceRequired
        )
    }

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
            notes = newest.optString("body").take(1_200),
            publishedAt = newest.optString("published_at").ifBlank { null }
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
            // Monthly: tags with -monthly, or stable tags from the floor onward
            UpdateChannel.MONTHLY -> suffix.startsWith("monthly") || (stable && compareVersions(base, PRODUCTION_FLOOR) >= 0)
            // Pre-release and cadence rings: accept their own markers (and legacy markers) OR any newer stable release
            UpdateChannel.WEEKLY,
            UpdateChannel.BIWEEKLY,
            UpdateChannel.TESTING -> stable || channel.markers.any { suffix.startsWith(it) }
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
