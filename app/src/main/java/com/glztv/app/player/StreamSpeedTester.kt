package com.glztv.app.player

import com.glztv.app.data.createPermissiveOkHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
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
    private val SPEED_TEST_CDN_URLS = listOf(
        "https://speed.cloudflare.com/__down?bytes=50000000",
        "https://cachefly.cachefly.net/100mb.test"
    )
    private const val PING_URL = "https://1.1.1.1"
    private const val PING_FALLBACK_URL = "https://www.google.com/generate_204"

    suspend fun runTest(
        client: OkHttpClient = createPermissiveOkHttpClient(),
        targetStreamUrl: String? = null,
        onProgress: (Double) -> Unit = {}
    ): SpeedTestResult = withContext(Dispatchers.IO) {
        // 1. Latency & Jitter test
        val pings = mutableListOf<Long>()
        for (i in 0 until 3) {
            val start = System.currentTimeMillis()
            val ok = runCatching {
                val req = Request.Builder()
                    .url(PING_URL)
                    .header("User-Agent", "GLZ-TV-SpeedTest/1.0")
                    .head()
                    .build()
                client.newCall(req).execute().use { it.isSuccessful }
            }.getOrDefault(false)

            if (ok) {
                pings.add(System.currentTimeMillis() - start)
            } else {
                val fbStart = System.currentTimeMillis()
                runCatching {
                    val req = Request.Builder()
                        .url(PING_FALLBACK_URL)
                        .header("User-Agent", "GLZ-TV-SpeedTest/1.0")
                        .head()
                        .build()
                    client.newCall(req).execute().use { }
                    pings.add(System.currentTimeMillis() - fbStart)
                }
            }
        }

        val avgPing = if (pings.isNotEmpty()) pings.average().toLong() else 35L
        val jitter = if (pings.size > 1) {
            val diffs = (0 until pings.size - 1).map { abs(pings[it + 1] - pings[it]) }
            diffs.average().toLong()
        } else 3L

        // 2. Resolve target candidate URLs for continuous stream throughput testing
        val testUrls = mutableListOf<String>()
        if (!targetStreamUrl.isNullOrBlank()) {
            val resolvedStreamUrls = resolveStreamUrls(client, targetStreamUrl)
            testUrls.addAll(resolvedStreamUrls)
        }
        testUrls.addAll(SPEED_TEST_CDN_URLS)

        // 3. Measure download throughput across test window (3.5 - 4.5 seconds)
        var totalBytes = 0L
        val testDurationMs = 4000L
        val startTime = System.currentTimeMillis()
        var streamStartTime = startTime
        var firstByteReceived = false
        var lastReportTime = startTime
        val buffer = ByteArray(64 * 1024)

        try {
            for (url in testUrls) {
                val timeRemaining = testDurationMs - (System.currentTimeMillis() - startTime)
                if (timeRemaining <= 200) break

                runCatching {
                    val req = Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0 (Android TV; GLZ-TV-SpeedTest/1.0)")
                        .header("Accept", "*/*")
                        .build()

                    client.newCall(req).execute().use { response ->
                        if (!response.isSuccessful) return@use
                        val body = response.body ?: return@use
                        val stream = body.byteStream()
                        var bytesRead: Int

                        while (stream.read(buffer).also { bytesRead = it } != -1) {
                            if (!firstByteReceived) {
                                firstByteReceived = true
                                streamStartTime = System.currentTimeMillis()
                            }
                            totalBytes += bytesRead
                            val elapsedSinceFirstByte = System.currentTimeMillis() - streamStartTime
                            val now = System.currentTimeMillis()

                            if (elapsedSinceFirstByte > 100 && now - lastReportTime >= 120) {
                                val currentMbps = (totalBytes * 8.0) / (elapsedSinceFirstByte / 1000.0) / 1_000_000.0
                                onProgress((currentMbps * 10).toInt() / 10.0)
                                lastReportTime = now
                            }

                            if (now - startTime >= testDurationMs) break
                        }
                    }
                }

                if (System.currentTimeMillis() - startTime >= testDurationMs) break
            }

            val elapsedMeasurable = (System.currentTimeMillis() - streamStartTime).coerceAtLeast(100)
            val finalMbps = if (totalBytes > 0) {
                (totalBytes * 8.0) / (elapsedMeasurable / 1000.0) / 1_000_000.0
            } else 0.0
            val roundedMbps = (finalMbps * 10).toInt() / 10.0

            val rating = when {
                roundedMbps >= 25.0 -> "Optimal for 4K Ultra HD"
                roundedMbps >= 12.0 -> "Great for 1080p Full HD"
                roundedMbps >= 6.0 -> "Good for HD Streaming"
                roundedMbps >= 3.0 -> "Fair for SD / 720p"
                else -> "Buffering Risk · Slow Connection"
            }

            SpeedTestResult(
                pingMs = avgPing,
                jitterMs = jitter,
                speedMbps = roundedMbps,
                rating = rating,
                isSuccess = roundedMbps > 0.0
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

    private fun resolveStreamUrls(client: OkHttpClient, streamUrl: String): List<String> {
        val urls = mutableListOf<String>()
        if (streamUrl.contains(".m3u8", ignoreCase = true)) {
            runCatching {
                val req = Request.Builder()
                    .url(streamUrl)
                    .header("User-Agent", "GLZ-TV-SpeedTest/1.0")
                    .build()
                client.newCall(req).execute().use { response ->
                    if (response.isSuccessful) {
                        val text = response.body?.string().orEmpty()
                        val baseUri = URI(streamUrl)
                        val lines = text.lines()
                            .map { it.trim() }
                            .filter { it.isNotEmpty() && !it.startsWith("#") }

                        // Extract first few segment / variant URLs
                        for (line in lines.take(3)) {
                            val resolved = if (line.startsWith("http://") || line.startsWith("https://")) {
                                line
                            } else {
                                baseUri.resolve(line).toString()
                            }
                            // If this was a nested variant playlist, try one level deeper
                            if (resolved.contains(".m3u8", ignoreCase = true)) {
                                val nested = resolveStreamUrls(client, resolved)
                                urls.addAll(nested)
                            } else {
                                urls.add(resolved)
                            }
                        }
                    }
                }
            }
        } else {
            urls.add(streamUrl)
        }
        return urls.distinct()
    }
}
