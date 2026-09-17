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
        var lastError: Throwable? = null
        repeat(if (forceRefresh) 3 else 1) { attempt ->
            runCatching {
                val cutoff = System.currentTimeMillis() - 2L * 3600_000L
                sourceClient.fetchStream(sourceUrl, preferences.requestHeaders) { stream ->
                    EpgParser.parse(stream, cutoffMillis = cutoff).also {
                        check(it.programmeCount > 0) { "EPG did not contain programmes" }
                    }
                }
            }.onSuccess { guide ->
                EpgCache.write(appContext, sourceUrl, guide)
                EpgMemoryCache.set(sourceUrl, guide)
                return guide
            }.onFailure { error ->
                lastError = error
                if (attempt < 2) Thread.sleep(350L * (attempt + 1))
            }
        }
        if (!forceRefresh) cached()?.let { return it }
        throw checkNotNull(lastError) { "EPG refresh failed" }
    }
}
