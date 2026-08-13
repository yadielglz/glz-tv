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
        return runCatching {
            EpgParser.parse(sourceClient.fetchText(sourceUrl, preferences.requestHeaders)).also {
                check(it.programmeCount > 0) { "EPG did not contain programmes" }
            }
        }.getOrElse { cached() ?: throw it }
            .also { EpgCache.write(appContext, sourceUrl, it) }
    }
}
