package com.glztv.app.data

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

fun createPermissiveOkHttpClient(): OkHttpClient {
    return try {
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
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    } catch (_: Exception) {
        OkHttpClient()
    }
}

internal class SourceClient(private val client: OkHttpClient) {
    fun fetchText(url: String, headers: Map<String, String>): String {
        val request = Request.Builder().url(url).apply {
            headers.forEach { (name, value) -> header(name, value) }
        }.build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Source returned ${response.code}" }
            val bytes = response.body?.bytes() ?: ByteArray(0)
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
