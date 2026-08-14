package com.glztv.app.player

import android.content.SharedPreferences
import org.json.JSONArray

class RecentChannelManager(private val preferences: SharedPreferences) {
    fun record(channelId: String) {
        if (channelId.isBlank()) return
        val updated = update(read(), channelId)
        preferences.edit().putString(KEY, JSONArray(updated).toString()).apply()
    }

    fun recentIds(): List<String> = read()

    fun previousId(currentChannelId: String): String? = previous(read(), currentChannelId)

    private fun read(): List<String> {
        val value = preferences.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(value)
            buildList {
                for (index in 0 until array.length()) {
                    array.optString(index).takeIf(String::isNotBlank)?.let(::add)
                }
            }.take(MAX_RECENT)
        }.getOrDefault(emptyList())
    }

    companion object {
        const val MAX_RECENT = 10
        private const val KEY = "recent_channel_ids"

        internal fun update(history: List<String>, channelId: String): List<String> {
            if (history.firstOrNull() == channelId) return history.take(MAX_RECENT)
            return buildList {
                add(channelId)
                history.filterNot { it == channelId }.forEach(::add)
            }.take(MAX_RECENT)
        }

        internal fun previous(history: List<String>, currentChannelId: String): String? =
            history.firstOrNull { it != currentChannelId }
    }
}
