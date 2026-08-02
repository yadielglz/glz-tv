package com.glztv.app

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

object GlzBackgroundSync {
    private const val UNIQUE_WORK = "glz-periodic-content-sync"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<GlzSyncWorker>(30, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresStorageNotLow(true)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}

class GlzSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = applicationContext.getSharedPreferences("glz_tv", Context.MODE_PRIVATE)
        val client = OkHttpClient()
        runCatching {
            GlzHubManager.sync(prefs, client)
            GlzHubManager.heartbeat(prefs, client)
            refreshSources(prefs, client)
            GithubUpdateManager.check(client)?.let { update ->
                prefs.edit()
                    .putString("background_update_version", update.version)
                    .putString("background_update_url", update.releaseUrl)
                    .apply()
            }
        }.fold(
            onSuccess = { Result.success() },
            onFailure = {
                if (runAttemptCount < 4) Result.retry()
                else Result.failure()
            }
        )
    }

    private fun refreshSources(prefs: android.content.SharedPreferences, client: OkHttpClient) {
        val playlistUrl = prefs.getString("playlist_url", "http://play.glztech.com/list.m3u")
            .orEmpty().ifBlank { "http://play.glztech.com/list.m3u" }
        val epgUrl = prefs.getString("epg_url", "https://play.glztech.com/epg.xml.gz")
            .orEmpty().ifBlank { "https://play.glztech.com/epg.xml.gz" }
        val headers = parseHeaders(prefs.getString("request_headers", "").orEmpty())
        val sourceHeaders = GlzHubManager.sourceRequestHeaders(prefs, playlistUrl, headers)
        val channels = M3uParser.parse(fetch(client, playlistUrl, sourceHeaders), playlistUrl, headers)
        check(channels.isNotEmpty()) { "Playlist did not contain channels" }
        ChannelCache.write(applicationContext, playlistUrl, channels)
        if (epgUrl.isNotBlank()) {
            val guide = EpgParser.parse(fetch(client, epgUrl, headers))
            check(guide.programmeCount > 0) { "EPG did not contain programmes" }
            EpgCache.write(applicationContext, epgUrl, guide)
        }
    }

    private fun fetch(client: OkHttpClient, url: String, headers: Map<String, String>): String {
        val request = Request.Builder().url(url).apply {
            headers.forEach { (name, value) -> header(name, value) }
            header("Accept-Encoding", "gzip")
        }.build()
        val response = client.newCall(request).execute()
        val bytes: ByteArray = response.body?.bytes() ?: ByteArray(0)
        check(response.isSuccessful) { "Source returned ${response.code}" }
        check(bytes.isNotEmpty()) { "Source returned no data" }
        val gzip = response.header("Content-Encoding").equals("gzip", true) ||
            url.endsWith(".gz", true) ||
            (bytes.size > 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte())
        return (if (gzip) GZIPInputStream(ByteArrayInputStream(bytes)) else ByteArrayInputStream(bytes))
            .bufferedReader().use { it.readText() }
    }

    private fun parseHeaders(source: String): Map<String, String> = buildMap {
        source.lineSequence().forEach { line ->
            val separator = line.indexOf(':')
            if (separator > 0) put(line.take(separator).trim(), line.drop(separator + 1).trim())
        }
    }
}
