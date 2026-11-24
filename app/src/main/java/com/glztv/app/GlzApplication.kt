package com.glztv.app

import android.app.Application
import android.content.ComponentCallbacks2
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.glztv.app.data.clearAllMemoryCaches
import com.glztv.app.data.createPermissiveOkHttpClient

/**
 * Custom Application class that caps Coil 3 image memory/disk caches for low-RAM TV boxes,
 * shares the single OkHttpClient instance, and releases caches under system memory pressure.
 */
class GlzApplication : Application(), SingletonImageLoader.Factory {

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { createPermissiveOkHttpClient() }))
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.14)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_image_cache"))
                    .maxSizeBytes(24L * 1024L * 1024L)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            runCatching { SingletonImageLoader.get(this).memoryCache?.clear() }
        }
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            clearAllMemoryCaches()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        runCatching { SingletonImageLoader.get(this).memoryCache?.clear() }
        clearAllMemoryCaches()
    }
}
