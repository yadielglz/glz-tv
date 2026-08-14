package com.glztv.app.data

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

internal class SourceClient(private val client: OkHttpClient) {
    fun fetchText(url: String, headers: Map<String, String>, bypassCache: Boolean = false): String {
        val request = Request.Builder().url(url).apply {
            headers.forEach { (name, value) -> header(name, value) }
            header("Accept-Encoding", "gzip")
            if (bypassCache) {
                header("Cache-Control", "no-cache, no-store")
                header("Pragma", "no-cache")
            }
        }.build()
        client.newCall(request).execute().use { response ->
            val bytes = response.body?.bytes() ?: ByteArray(0)
            check(response.isSuccessful) { "Source returned ${response.code}" }
            check(bytes.isNotEmpty()) { "Source returned no data" }
            val gzip = response.header("Content-Encoding").equals("gzip", true) ||
                url.endsWith(".gz", true) ||
                (bytes.size > 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte())
            val stream = if (gzip) GZIPInputStream(ByteArrayInputStream(bytes))
                else ByteArrayInputStream(bytes)
            return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                .removePrefix("\uFEFF")
        }
    }
}
