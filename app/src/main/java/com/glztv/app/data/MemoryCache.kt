package com.glztv.app.data

import com.glztv.app.Channel
import com.glztv.app.EpgGuide

object ChannelMemoryCache {
    @Volatile
    private var cachedSourceUrl: String? = null
    @Volatile
    private var memoryChannels: List<Channel>? = null

    fun get(sourceUrl: String): List<Channel>? {
        if (sourceUrl.isNotBlank() && cachedSourceUrl == sourceUrl) {
            return memoryChannels
        }
        return null
    }

    fun set(sourceUrl: String, channels: List<Channel>) {
        if (sourceUrl.isNotBlank()) {
            cachedSourceUrl = sourceUrl
            memoryChannels = channels
        }
    }

    fun clear() {
        cachedSourceUrl = null
        memoryChannels = null
    }
}

object EpgMemoryCache {
    @Volatile
    private var cachedSourceUrl: String? = null
    @Volatile
    private var memoryGuide: EpgGuide? = null

    fun get(sourceUrl: String): EpgGuide? {
        if (sourceUrl.isNotBlank() && cachedSourceUrl == sourceUrl) {
            return memoryGuide
        }
        return null
    }

    fun set(sourceUrl: String, guide: EpgGuide) {
        if (sourceUrl.isNotBlank()) {
            cachedSourceUrl = sourceUrl
            memoryGuide = guide
        }
    }

    fun clear() {
        cachedSourceUrl = null
        memoryGuide = null
    }
}

fun clearAllMemoryCaches() {
    ChannelMemoryCache.clear()
    EpgMemoryCache.clear()
}
