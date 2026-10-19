package com.glztv.app

import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import org.json.JSONObject

@Immutable
data class Movie(
    val id: String,
    val title: String,
    val streamUrl: String,
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val description: String = "",
    val releaseYear: Int? = null,
    val runtimeMinutes: Int? = null,
    val genres: List<String> = emptyList(),
    val headers: Map<String, String> = emptyMap()
) {
    fun asChannel(): Channel = Channel(
        "movie:$id", title, "Movies", "", posterUrl, streamUrl, headers
    )

    companion object {
        fun fromPreferences(prefs: SharedPreferences): List<Movie> {
            val raw = prefs.getString(GlzHubManager.MANAGED_MOVIES, null) ?: return emptyList()
            return runCatching {
                val array = org.json.JSONArray(raw)
                (0 until array.length()).mapNotNull { index ->
                    val item = array.optJSONObject(index) ?: return@mapNotNull null
                    val id = item.optString("id").trim()
                    val title = item.optString("title").trim()
                    val streamUrl = item.optString("streamUrl").trim()
                    if (id.isBlank() || title.isBlank() || streamUrl.isBlank()) return@mapNotNull null
                    val genresJson = item.optJSONArray("genres")
                    val headersJson = item.optJSONObject("requestHeaders") ?: JSONObject()
                    Movie(
                        id = id,
                        title = title,
                        streamUrl = streamUrl,
                        posterUrl = item.optString("posterUrl"),
                        backdropUrl = item.optString("backdropUrl"),
                        description = item.optString("description"),
                        releaseYear = item.optInt("releaseYear").takeIf { item.has("releaseYear") && it > 0 },
                        runtimeMinutes = item.optInt("runtimeMinutes").takeIf { item.has("runtimeMinutes") && it > 0 },
                        genres = if (genresJson == null) emptyList() else (0 until genresJson.length()).mapNotNull {
                            genresJson.optString(it).takeIf(String::isNotBlank)
                        },
                        headers = headersJson.keys().asSequence().associateWith { headersJson.optString(it) }
                    )
                }
            }.getOrDefault(emptyList())
        }
    }
}
