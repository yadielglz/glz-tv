package com.glztv.app.data

import android.content.Context
import com.glztv.app.Channel
import com.glztv.app.ChannelCache
import com.glztv.app.GlzHubManager
import com.glztv.app.M3uParser
import okhttp3.OkHttpClient

class PlaylistRepository(
    context: Context,
    private val preferences: PreferencesRepository,
    client: OkHttpClient
) {
    private val appContext = context.applicationContext
    private val sourceClient = SourceClient(client)

    fun cached(): List<Channel>? {
        val url = preferences.playlistUrl
        if (url.isBlank()) return null
        ChannelMemoryCache.get(url)?.let { return it }
        return ChannelCache.read(appContext, url)?.also {
            ChannelMemoryCache.set(url, it)
        }
    }

    fun load(forceRefresh: Boolean = false): List<Channel> {
        val sourceUrl = preferences.playlistUrl
        val existingCache = cached()
        if (!forceRefresh && existingCache != null) return existingCache
        val globalHeaders = preferences.requestHeaders
        val sourceHeaders = GlzHubManager.sourceRequestHeaders(
            preferences.sharedPreferences, sourceUrl, globalHeaders
        )
        val etagKey = "playlist_etag_${sourceUrl.hashCode()}"
        val savedEtag = if (existingCache != null) {
            preferences.sharedPreferences.getString(etagKey, null)
        } else null

        return when (val result = sourceClient.fetchTextIfModified(sourceUrl, sourceHeaders, savedEtag)) {
            ConditionalFetchResult.NotModified -> existingCache ?: loadWithoutEtag(sourceUrl, sourceHeaders, globalHeaders, etagKey)
            is ConditionalFetchResult.Modified -> {
                M3uParser.parse(result.value, sourceUrl, globalHeaders).also { channels ->
                    check(channels.isNotEmpty()) { "Playlist did not contain channels" }
                    ChannelCache.write(appContext, sourceUrl, channels)
                    ChannelMemoryCache.set(sourceUrl, channels)
                    if (!result.etag.isNullOrBlank()) {
                        preferences.sharedPreferences.edit().putString(etagKey, result.etag).apply()
                    }
                }
            }
        }
    }

    private fun loadWithoutEtag(
        sourceUrl: String,
        sourceHeaders: Map<String, String>,
        globalHeaders: Map<String, String>,
        etagKey: String
    ): List<Channel> {
        val text = sourceClient.fetchText(sourceUrl, sourceHeaders)
        return M3uParser.parse(text, sourceUrl, globalHeaders).also { channels ->
            check(channels.isNotEmpty()) { "Playlist did not contain channels" }
            ChannelCache.write(appContext, sourceUrl, channels)
            ChannelMemoryCache.set(sourceUrl, channels)
            preferences.sharedPreferences.edit().remove(etagKey).apply()
        }
    }
}
