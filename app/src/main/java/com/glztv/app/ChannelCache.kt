package com.glztv.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

private const val CHANNEL_CACHE_FILE = "channels-v1.json"
private const val CHANNEL_CACHE_MAX_AGE_MS = 24L * 60L * 60L * 1000L

object ChannelCache {
    fun read(context: Context, sourceUrl: String): List<Channel>? = runCatching {
        val file = context.filesDir.resolve(CHANNEL_CACHE_FILE)
        if (!file.isFile) return null
        val root = JSONObject(file.readText())
        if (root.optString("source") != sourceUrl) return null
        if (System.currentTimeMillis() - root.optLong("savedAt") > CHANNEL_CACHE_MAX_AGE_MS) return null
        val entries = root.getJSONArray("channels")
        buildList {
            for (index in 0 until entries.length()) {
                val item = entries.getJSONObject(index)
                val headerJson = item.optJSONObject("headers") ?: JSONObject()
                val headers = buildMap {
                    headerJson.keys().forEach { key -> put(key, headerJson.optString(key)) }
                }
                add(
                    Channel(
                        item.optString("id"),
                        item.optString("name"),
                        item.optString("group"),
                        item.optString("number"),
                        item.optString("logoUrl"),
                        item.optString("streamUrl"),
                        headers
                    )
                )
            }
        }.takeIf { it.isNotEmpty() }
    }.getOrNull()

    fun write(context: Context, sourceUrl: String, channels: List<Channel>) {
        runCatching {
            val entries = JSONArray()
            channels.forEach { channel ->
                val headers = JSONObject()
                channel.headers.forEach(headers::put)
                entries.put(
                    JSONObject()
                        .put("id", channel.id)
                        .put("name", channel.name)
                        .put("group", channel.group)
                        .put("number", channel.number)
                        .put("logoUrl", channel.logoUrl)
                        .put("streamUrl", channel.streamUrl)
                        .put("headers", headers)
                )
            }
            val root = JSONObject()
                .put("source", sourceUrl)
                .put("savedAt", System.currentTimeMillis())
                .put("channels", entries)
            val target = context.filesDir.resolve(CHANNEL_CACHE_FILE)
            val temporary = context.filesDir.resolve("$CHANNEL_CACHE_FILE.tmp")
            temporary.writeText(root.toString())
            target.delete()
            temporary.renameTo(target)
        }
    }
}
