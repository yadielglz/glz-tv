package com.glztv.app

import android.content.SharedPreferences
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class RadioStation(
    val code: String,
    val name: String,
    val genre: String,
    val streamUrl: String,
    val logoUrl: String?,
    val epgChannelId: String?,
    val bitrateKbps: Int,
    val requestHeaders: Map<String, String>
)

object RadioCatalogManager {
    const val CATALOG_URL = "https://glzhub.glztech.com/api/v1/radio/stations"
    private const val CATALOG_CACHE = "radio_catalog_json"
    private const val CATALOG_ETAG = "radio_catalog_etag"

    data class Result(val stations: List<RadioStation>, val fromCache: Boolean)

    fun load(prefs: SharedPreferences, client: OkHttpClient): Result {
        val request = Request.Builder().url(CATALOG_URL).apply {
            prefs.getString(CATALOG_ETAG, null)?.takeIf(String::isNotBlank)?.let {
                header("If-None-Match", it)
            }
        }.get().build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (response.code == 304) {
                    return@use Result(parseCatalog(prefs.getString(CATALOG_CACHE, null).orEmpty()), true)
                }
                check(response.isSuccessful) { "GLZ Hub returned ${response.code}" }
                val text = response.body?.string().orEmpty()
                val stations = parseCatalog(text)
                check(stations.isNotEmpty()) { "GLZ Hub returned no radio stations" }
                prefs.edit().putString(CATALOG_CACHE, text).apply {
                    response.header("ETag")?.let { putString(CATALOG_ETAG, it) }
                }.apply()
                Result(stations, false)
            }
        }.getOrElse { error ->
            val cached = parseCatalog(prefs.getString(CATALOG_CACHE, null).orEmpty())
            if (cached.isEmpty()) throw error
            Result(cached, true)
        }
    }

    internal fun parseCatalog(text: String): List<RadioStation> {
        if (text.isBlank()) return emptyList()
        val stations = JSONObject(text).optJSONArray("stations") ?: return emptyList()
        return buildList {
            for (index in 0 until stations.length()) {
                val item = stations.optJSONObject(index) ?: continue
                val streamUrl = item.optString("streamUrl").trim()
                if (streamUrl.isBlank()) continue
                val headers = item.optJSONObject("requestHeaders")?.let { values ->
                    values.keys().asSequence().associateWith { values.optString(it) }
                }.orEmpty()
                add(
                    RadioStation(
                        code = item.optString("code"),
                        name = item.optString("name").ifBlank { item.optString("code", "Radio") },
                        genre = item.optString("genre", "Radio"),
                        streamUrl = streamUrl,
                        logoUrl = item.optString("logoUrl").takeIf(String::isNotBlank),
                        epgChannelId = item.optString("epgChannelId").takeIf(String::isNotBlank),
                        bitrateKbps = item.optInt("bitrateKbps", 128),
                        requestHeaders = headers
                    )
                )
            }
        }
    }
}
