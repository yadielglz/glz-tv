package com.glztv.app

import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import org.json.JSONObject

@Immutable
data class SubtitleTrack(
    val id: String,
    val language: String,
    val label: String,
    val uri: String,
    val mimeType: String = "application/x-subrip"
)

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
    val headers: Map<String, String> = emptyMap(),
    val subtitles: List<SubtitleTrack> = emptyList()
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
                    val subtitlesJson = item.optJSONArray("subtitles")
                    val parsedSubtitles = if (subtitlesJson == null) emptyList() else (0 until subtitlesJson.length()).mapNotNull { i ->
                        val subObj = subtitlesJson.optJSONObject(i) ?: return@mapNotNull null
                        val uri = subObj.optString("url").ifBlank { subObj.optString("uri") }.trim()
                        val lang = subObj.optString("language").ifBlank { subObj.optString("lang") }.trim()
                        val label = subObj.optString("label").ifBlank { lang.uppercase() }.trim()
                        if (uri.isBlank() || lang.isBlank()) null
                        else SubtitleTrack(
                            id = subObj.optString("id").ifBlank { "$id-$lang-$i" },
                            language = lang,
                            label = label,
                            uri = uri,
                            mimeType = subObj.optString("mimeType").ifBlank { "application/x-subrip" }
                        )
                    }
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
                        headers = headersJson.keys().asSequence().associateWith { headersJson.optString(it) },
                        subtitles = parsedSubtitles
                    )
                }
            }.getOrDefault(emptyList())
        }
    }
}
