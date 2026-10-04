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
        if (sourceUrl.isBlank()) return emptyList()
        if (!forceRefresh) cached()?.let { return it }
        val globalHeaders = preferences.requestHeaders
        val sourceHeaders = GlzHubManager.sourceRequestHeaders(
            preferences.sharedPreferences, sourceUrl, globalHeaders
        )
        val sharedPrefs = preferences.sharedPreferences
        val lastEtag = sharedPrefs.getString("playlist_etag_${sourceUrl.hashCode()}", null)
        val lastModified = sharedPrefs.getString("playlist_last_modified_${sourceUrl.hashCode()}", null)

        val result = sourceClient.fetchTextConditional(
            sourceUrl, sourceHeaders, eTag = if (forceRefresh) lastEtag else null, lastModified = if (forceRefresh) lastModified else null
        )
        if (result.isNotModified) {
            return cached() ?: emptyList()
        }
        val text = checkNotNull(result.data)
        return M3uParser.parse(text, sourceUrl, globalHeaders).also {
            check(it.isNotEmpty()) { "Playlist did not contain channels" }
            result.eTag?.let { etag -> sharedPrefs.edit().putString("playlist_etag_${sourceUrl.hashCode()}", etag).apply() }
            result.lastModified?.let { lm -> sharedPrefs.edit().putString("playlist_last_modified_${sourceUrl.hashCode()}", lm).apply() }
            ChannelCache.write(appContext, sourceUrl, it)
            ChannelMemoryCache.set(sourceUrl, it)
        }
    }
}
