package com.glztv.app.data

import android.content.Context
import com.glztv.app.EpgCache
import com.glztv.app.EpgGuide
import com.glztv.app.EpgParser
import okhttp3.OkHttpClient

class EpgRepository(
    context: Context,
    private val preferences: PreferencesRepository,
    client: OkHttpClient
) {
    private val appContext = context.applicationContext
    private val sourceClient = SourceClient(client)

    fun cached(): EpgGuide? {
        val url = preferences.epgUrl.takeIf(String::isNotBlank) ?: return null
        EpgMemoryCache.get(url)?.let { return it }
        return EpgCache.read(appContext, url)?.also {
            EpgMemoryCache.set(url, it)
        }
    }

    fun load(forceRefresh: Boolean = false): EpgGuide {
        val sourceUrl = preferences.epgUrl
        if (sourceUrl.isBlank()) return EpgGuide.Empty
        val existingCache = cached()
        if (!forceRefresh && existingCache != null) return existingCache

        val etagKey = "epg_etag_${sourceUrl.hashCode()}"
        val savedEtag = if (existingCache != null) {
            preferences.sharedPreferences.getString(etagKey, null)
        } else null

        repeat(if (forceRefresh) 2 else 1) { attempt ->
            runCatching {
                val cutoff = System.currentTimeMillis() - 2L * 3600_000L
                sourceClient.fetchStreamIfModified(sourceUrl, preferences.requestHeaders, savedEtag) { stream ->
                    EpgParser.parse(stream, cutoffMillis = cutoff)
                }
            }.onSuccess { result ->
                return when (result) {
                    ConditionalFetchResult.NotModified -> existingCache ?: EpgGuide.Empty
                    is ConditionalFetchResult.Modified -> {
                        EpgCache.write(appContext, sourceUrl, result.value)
                        EpgMemoryCache.set(sourceUrl, result.value)
                        if (!result.etag.isNullOrBlank()) {
                            preferences.sharedPreferences.edit().putString(etagKey, result.etag).apply()
                        }
                        result.value
                    }
                }
            }.onFailure {
                if (attempt == 0 && forceRefresh) Thread.sleep(300L)
            }
        }
        return existingCache ?: EpgGuide.Empty
    }
}
