package com.glztv.app.data

import android.content.Context
import android.content.SharedPreferences
import com.glztv.app.Channel
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class ChannelOverride(
    val customNumber: String? = null,
    val customName: String? = null,
    val isHidden: Boolean = false
)

class ChannelCustomizationManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "glz_channel_customizations"
        private const val KEY_NUMBER_OVERRIDES = "number_overrides"
        private const val KEY_NAME_OVERRIDES = "name_overrides"
        private const val KEY_HIDDEN_IDS = "hidden_ids"

        fun channelNumberValue(value: String): Double {
            val trimmed = value.trim()
            return trimmed.toDoubleOrNull()
                ?: Regex("\\d+(?:\\.\\d+)?").find(trimmed)?.value?.toDoubleOrNull()
                ?: Double.MAX_VALUE
        }
    }

    private fun getNumberOverrides(): Map<String, String> {
        val raw = prefs.getString(KEY_NUMBER_OVERRIDES, null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            val map = mutableMapOf<String, String>()
            json.keys().forEach { key ->
                map[key] = json.getString(key)
            }
            map
        }.getOrElse { emptyMap() }
    }

    private fun getNameOverrides(): Map<String, String> {
        val raw = prefs.getString(KEY_NAME_OVERRIDES, null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            val map = mutableMapOf<String, String>()
            json.keys().forEach { key ->
                map[key] = json.getString(key)
            }
            map
        }.getOrElse { emptyMap() }
    }

    private fun getHiddenIds(): Set<String> {
        val raw = prefs.getString(KEY_HIDDEN_IDS, null) ?: return emptySet()
        return runCatching {
            val array = JSONArray(raw)
            val set = mutableSetOf<String>()
            for (i in 0 until array.length()) {
                set.add(array.getString(i))
            }
            set
        }.getOrElse { emptySet() }
    }

    fun getOverride(channelId: String): ChannelOverride {
        val numbers = getNumberOverrides()
        val names = getNameOverrides()
        val hidden = getHiddenIds()
        return ChannelOverride(
            customNumber = numbers[channelId],
            customName = names[channelId],
            isHidden = hidden.contains(channelId)
        )
    }

    fun isHidden(channelId: String): Boolean {
        return getHiddenIds().contains(channelId)
    }

    fun hasCustomizations(): Boolean {
        return getNumberOverrides().isNotEmpty() ||
            getNameOverrides().isNotEmpty() ||
            getHiddenIds().isNotEmpty()
    }

    fun setChannelNumber(channelId: String, newNumber: String) {
        val current = getNumberOverrides().toMutableMap()
        val trimmed = newNumber.trim()
        if (trimmed.isNotBlank()) {
            current[channelId] = trimmed
        } else {
            current.remove(channelId)
        }
        val json = JSONObject(current as Map<*, *>)
        prefs.edit().putString(KEY_NUMBER_OVERRIDES, json.toString()).apply()
    }

    fun setChannelName(channelId: String, newName: String) {
        val current = getNameOverrides().toMutableMap()
        val trimmed = newName.trim()
        if (trimmed.isNotBlank()) {
            current[channelId] = trimmed
        } else {
            current.remove(channelId)
        }
        val json = JSONObject(current as Map<*, *>)
        prefs.edit().putString(KEY_NAME_OVERRIDES, json.toString()).apply()
    }

    fun toggleHidden(channelId: String): Boolean {
        val current = getHiddenIds().toMutableSet()
        val isNowHidden = if (current.contains(channelId)) {
            current.remove(channelId)
            false
        } else {
            current.add(channelId)
            true
        }
        val array = JSONArray(current)
        prefs.edit().putString(KEY_HIDDEN_IDS, array.toString()).apply()
        return isNowHidden
    }

    fun setHidden(channelId: String, hidden: Boolean) {
        val current = getHiddenIds().toMutableSet()
        if (hidden) {
            current.add(channelId)
        } else {
            current.remove(channelId)
        }
        val array = JSONArray(current)
        prefs.edit().putString(KEY_HIDDEN_IDS, array.toString()).apply()
    }

    fun renumberSequentially(channels: List<Channel>, startFrom: Int = 1) {
        val current = getNumberOverrides().toMutableMap()
        channels.forEachIndexed { index, channel ->
            current[channel.id] = (startFrom + index).toString()
        }
        val json = JSONObject(current as Map<*, *>)
        prefs.edit().putString(KEY_NUMBER_OVERRIDES, json.toString()).apply()
    }

    fun resetChannel(channelId: String) {
        val numbers = getNumberOverrides().toMutableMap().apply { remove(channelId) }
        val names = getNameOverrides().toMutableMap().apply { remove(channelId) }
        val hidden = getHiddenIds().toMutableSet().apply { remove(channelId) }

        prefs.edit()
            .putString(KEY_NUMBER_OVERRIDES, JSONObject(numbers as Map<*, *>).toString())
            .putString(KEY_NAME_OVERRIDES, JSONObject(names as Map<*, *>).toString())
            .putString(KEY_HIDDEN_IDS, JSONArray(hidden).toString())
            .apply()
    }

    fun resetAll() {
        prefs.edit()
            .remove(KEY_NUMBER_OVERRIDES)
            .remove(KEY_NAME_OVERRIDES)
            .remove(KEY_HIDDEN_IDS)
            .apply()
    }

    fun apply(channels: List<Channel>, includeHidden: Boolean = false): List<Channel> {
        val numberMap = getNumberOverrides()
        val nameMap = getNameOverrides()
        val hiddenSet = getHiddenIds()

        val transformed = channels.mapNotNull { ch ->
            val isChannelHidden = hiddenSet.contains(ch.id)
            if (!includeHidden && isChannelHidden) return@mapNotNull null

            val effectiveNumber = numberMap[ch.id] ?: ch.number
            val effectiveName = nameMap[ch.id] ?: ch.name

            if (effectiveNumber != ch.number || effectiveName != ch.name) {
                ch.copy(number = effectiveNumber, name = effectiveName)
            } else {
                ch
            }
        }

        return transformed.sortedWith(
            compareBy<Channel> { channelNumberValue(it.number) }
                .thenBy { it.number }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )
    }
}
