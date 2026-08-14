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

    fun cached(): EpgGuide? = preferences.epgUrl.takeIf(String::isNotBlank)
        ?.let { EpgCache.read(appContext, it) }

    fun load(forceRefresh: Boolean = false): EpgGuide {
        val sourceUrl = preferences.epgUrl
        if (sourceUrl.isBlank()) return EpgGuide.Empty
        if (!forceRefresh) cached()?.let { return it }
        val requestUrl = if (forceRefresh) {
            "$sourceUrl${if ('?' in sourceUrl) '&' else '?'}glz_refresh=${System.currentTimeMillis()}"
        } else sourceUrl
        var lastError: Throwable? = null
        repeat(if (forceRefresh) 3 else 1) { attempt ->
            runCatching {
                EpgParser.parse(
                    sourceClient.fetchText(requestUrl, preferences.requestHeaders)
                ).also {
                    check(it.programmeCount > 0) { "EPG did not contain programmes" }
                }
            }.onSuccess { guide ->
                EpgCache.write(appContext, sourceUrl, guide)
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
