package com.glztv.app.data

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

internal class SourceClient(private val client: OkHttpClient) {
    fun fetchText(url: String, headers: Map<String, String>): String {
        val request = Request.Builder().url(url).apply {
            headers.forEach { (name, value) -> header(name, value) }
        }.build()
        client.newCall(request).execute().use { response ->
            val bytes = response.body?.bytes() ?: ByteArray(0)
            check(response.isSuccessful) { "Source returned ${response.code}" }
            check(bytes.isNotEmpty()) { "Source returned no data" }
            return decodeText(bytes)
        }
    }

    companion object {
        internal fun decodeText(bytes: ByteArray): String {
            val stream = if (bytes.hasGzipSignature()) {
                GZIPInputStream(ByteArrayInputStream(bytes))
            } else {
                ByteArrayInputStream(bytes)
            }
            return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                .removePrefix("\uFEFF")
        }

        private fun ByteArray.hasGzipSignature() =
            size >= 2 && this[0] == 0x1f.toByte() && this[1] == 0x8b.toByte()
    }
}
