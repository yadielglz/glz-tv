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

    fun cached(): List<Channel>? = ChannelCache.read(appContext, preferences.playlistUrl)

    fun load(forceRefresh: Boolean = false): List<Channel> {
        val sourceUrl = preferences.playlistUrl
        if (!forceRefresh) cached()?.let { return it }
        val globalHeaders = preferences.requestHeaders
        val sourceHeaders = GlzHubManager.sourceRequestHeaders(
            preferences.sharedPreferences, sourceUrl, globalHeaders
        )
        return M3uParser.parse(
            sourceClient.fetchText(sourceUrl, sourceHeaders), sourceUrl, globalHeaders
        ).also {
            check(it.isNotEmpty()) { "Playlist did not contain channels" }
            ChannelCache.write(appContext, sourceUrl, it)
        }
    }
}
