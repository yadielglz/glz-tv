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
        if (!forceRefresh) cached()?.let { return it }
        val sharedPrefs = preferences.sharedPreferences
        val lastEtag = sharedPrefs.getString("epg_etag_${sourceUrl.hashCode()}", null)
        val lastModified = sharedPrefs.getString("epg_last_modified_${sourceUrl.hashCode()}", null)
        var lastError: Throwable? = null

        repeat(if (forceRefresh) 2 else 1) { attempt ->
            runCatching {
                val cutoff = System.currentTimeMillis() - 2L * 3600_000L
                val result = sourceClient.fetchStreamConditional(
                    sourceUrl,
                    preferences.requestHeaders,
                    eTag = if (forceRefresh) lastEtag else null,
                    lastModified = if (forceRefresh) lastModified else null
                ) { stream ->
                    EpgParser.parse(stream, cutoffMillis = cutoff).also {
                        check(it.programmeCount > 0) { "EPG did not contain programmes" }
                    }
                }
                if (result.isNotModified) {
                    return cached() ?: EpgGuide.Empty
                }
                val guide = checkNotNull(result.data)
                result.eTag?.let { etag -> sharedPrefs.edit().putString("epg_etag_${sourceUrl.hashCode()}", etag).apply() }
                result.lastModified?.let { lm -> sharedPrefs.edit().putString("epg_last_modified_${sourceUrl.hashCode()}", lm).apply() }
                EpgCache.write(appContext, sourceUrl, guide)
                EpgMemoryCache.set(sourceUrl, guide)
                return guide
            }.onFailure { error ->
                lastError = error
                if (attempt < 1) Thread.sleep(250L)
            }
        }
        cached()?.let { return it }
        if (forceRefresh) return EpgGuide.Empty
        throw checkNotNull(lastError) { "EPG refresh failed" }
    }
}
