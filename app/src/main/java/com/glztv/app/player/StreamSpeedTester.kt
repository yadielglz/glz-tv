package com.glztv.app.player

import com.glztv.app.data.createPermissiveOkHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlin.math.abs

data class SpeedTestResult(
    val pingMs: Long,
    val jitterMs: Long,
    val speedMbps: Double,
    val rating: String,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

object StreamSpeedTester {
    private const val SPEED_TEST_URL_FALLBACK = "https://speed.cloudflare.com/__down?bytes=15000000"
    private const val PING_URL_FALLBACK = "https://1.1.1.1"

    suspend fun runTest(
        client: OkHttpClient = createPermissiveOkHttpClient(),
        targetStreamUrl: String? = null,
        onProgress: (Double) -> Unit = {}
    ): SpeedTestResult = withContext(Dispatchers.IO) {
        val pingUrl = if (!targetStreamUrl.isNullOrBlank()) targetStreamUrl else PING_URL_FALLBACK
        val downloadUrl = if (!targetStreamUrl.isNullOrBlank()) targetStreamUrl else SPEED_TEST_URL_FALLBACK

        val pings = mutableListOf<Long>()
        // 1. Ping test (3 quick sample requests)
        for (i in 0 until 3) {
            val start = System.currentTimeMillis()
            val ok = runCatching {
                val req = Request.Builder().url(pingUrl).head().build()
                client.newCall(req).execute().use { it.isSuccessful }
            }.getOrDefault(false)

            if (ok) {
                pings.add(System.currentTimeMillis() - start)
            } else {
                // Fallback ping
                val fbStart = System.currentTimeMillis()
                runCatching {
                    val req = Request.Builder().url(PING_URL_FALLBACK).head().build()
                    client.newCall(req).execute().use { }
                    pings.add(System.currentTimeMillis() - fbStart)
                }
            }
        }

        val avgPing = if (pings.isNotEmpty()) pings.average().toLong() else 45L
        val jitter = if (pings.size > 1) {
            val diffs = (0 until pings.size - 1).map { abs(pings[it + 1] - pings[it]) }
            diffs.average().toLong()
        } else 4L

        // 2. Download bandwidth test
        var totalBytes = 0L
        val testDurationMs = 4000L // 4 seconds of streaming test
        val startTime = System.currentTimeMillis()
        var lastReportTime = startTime

        try {
            val req = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "GLZ-TV-SpeedTest/1.0")
                .build()

            client.newCall(req).execute().use { response ->
                val body = response.body ?: throw IOException("Empty response body")
                val stream = body.byteStream()
                val buffer = ByteArray(32 * 1024)
                var bytesRead: Int

                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    totalBytes += bytesRead
                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed > 0 && System.currentTimeMillis() - lastReportTime >= 150) {
                        val currentMbps = (totalBytes * 8.0) / (elapsed / 1000.0) / 1_000_000.0
                        onProgress((currentMbps * 10).toInt() / 10.0)
                        lastReportTime = System.currentTimeMillis()
                    }
                    if (elapsed >= testDurationMs) break
                }
            }

            val elapsedTotal = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
            val finalMbps = ((totalBytes * 8.0) / (elapsedTotal / 1000.0) / 1_000_000.0)
            val roundedMbps = (finalMbps * 10).toInt() / 10.0

            val rating = when {
                roundedMbps >= 25.0 -> "Optimal for 4K Ultra HD"
                roundedMbps >= 12.0 -> "Great for 1080p Full HD"
                roundedMbps >= 5.0 -> "Fair for 720p HD"
                else -> "Buffering Risk · Slow Connection"
            }

            SpeedTestResult(
                pingMs = avgPing,
                jitterMs = jitter,
                speedMbps = roundedMbps,
                rating = rating,
                isSuccess = true
            )
        } catch (e: Exception) {
            SpeedTestResult(
                pingMs = avgPing,
                jitterMs = jitter,
                speedMbps = 0.0,
                rating = "Test Incomplete",
                isSuccess = false,
                errorMessage = e.message ?: "Connection error"
            )
        }
    }
}
