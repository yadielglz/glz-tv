package com.glztv.app.data

import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Single shared permissive OkHttpClient across the entire app so we never leak
 * duplicate Dispatcher thread pools, ConnectionPools, or SSLContexts per screen/card/worker.
 */
private val sharedPermissiveOkHttpClient: OkHttpClient by lazy {
    try {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())
        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier(HostnameVerifier { _, _ -> true })
            .connectionPool(ConnectionPool(5, 30, TimeUnit.SECONDS))
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    } catch (_: Exception) {
        OkHttpClient.Builder()
            .connectionPool(ConnectionPool(5, 30, TimeUnit.SECONDS))
            .build()
    }
}

fun createPermissiveOkHttpClient(): OkHttpClient = sharedPermissiveOkHttpClient

internal sealed class ConditionalFetchResult<out T> {
    data object NotModified : ConditionalFetchResult<Nothing>()
    data class Modified<T>(val value: T, val etag: String?) : ConditionalFetchResult<T>()
}

internal class SourceClient(private val client: OkHttpClient) {
    fun <T> fetchStream(url: String, headers: Map<String, String>, block: (InputStream) -> T): T {
        return when (val result = fetchStreamIfModified(url, headers, null, block)) {
            is ConditionalFetchResult.Modified -> result.value
            ConditionalFetchResult.NotModified -> error("Unexpected 304 without If-None-Match")
        }
    }

    fun <T> fetchStreamIfModified(
        url: String,
        headers: Map<String, String>,
        ifNoneMatch: String?,
        block: (InputStream) -> T
    ): ConditionalFetchResult<T> {
        val request = Request.Builder().url(url).apply {
            headers.forEach { (name, value) -> header(name, value) }
            if (!ifNoneMatch.isNullOrBlank()) {
                header("If-None-Match", ifNoneMatch)
            }
        }.build()
        client.newCall(request).execute().use { response ->
            if (response.code == 304 && !ifNoneMatch.isNullOrBlank()) {
                return ConditionalFetchResult.NotModified
            }
            check(response.isSuccessful) { "Source returned ${response.code}" }
            val body = checkNotNull(response.body) { "Source returned no body" }
            val rawStream = body.byteStream()
            val buffered = BufferedInputStream(rawStream)
            val isGzip = response.header("Content-Encoding")?.equals("gzip", ignoreCase = true) == true ||
                buffered.isGzipHeader()
            val finalStream = if (isGzip) GZIPInputStream(buffered) else buffered
            val etag = response.header("ETag")?.trim()?.takeIf(String::isNotBlank)
            return ConditionalFetchResult.Modified(block(finalStream), etag)
        }
    }

    fun fetchText(url: String, headers: Map<String, String>): String {
        return fetchStream(url, headers) { stream ->
            InputStreamReader(stream, Charsets.UTF_8).use { it.readText() }.removePrefix("\uFEFF")
        }
    }

    fun fetchTextIfModified(
        url: String,
        headers: Map<String, String>,
        ifNoneMatch: String?
    ): ConditionalFetchResult<String> {
        return fetchStreamIfModified(url, headers, ifNoneMatch) { stream ->
            InputStreamReader(stream, Charsets.UTF_8).use { it.readText() }.removePrefix("\uFEFF")
        }
    }

    companion object {
        private fun BufferedInputStream.isGzipHeader(): Boolean {
            mark(2)
            val b1 = read()
            val b2 = read()
            reset()
            return b1 == 0x1f && b2 == 0x8b
        }

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
