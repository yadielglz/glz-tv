package com.glztv.app.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.glztv.app.SubtitleTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder

data class OpenSubtitlesSearchResult(
    val id: String,
    val fileId: Int,
    val language: String,
    val label: String,
    val downloadCount: Int,
    val ratings: Double,
    val hearingImpaired: Boolean,
    val releaseName: String
)

class OpenSubtitlesRepository(
    private val client: OkHttpClient = OkHttpClient(),
    private val prefs: SharedPreferences? = null
) {
    companion object {
        const val PREF_KEY_OPENSUBTITLES_API_KEY = "opensubtitles_api_key"
        const val DEFAULT_API_KEY = "70OBgTrCm2VXk8KnE4j5SCmXF3DwoJOv"
        private const val BASE_URL = "https://api.opensubtitles.com/api/v1"
        private const val USER_AGENT = "GLZ-TV v1.43"

        /**
         * Cleans movie titles from playlist streams, removing tags like [1080p], 4K, etc.
         */
        fun cleanMovieTitle(title: String): String {
            return title
                .replace(Regex("\\[.*?\\]"), " ")
                .replace(Regex("\\(.*?\\)"), " ")
                .replace(Regex("(?i)\\b(1080p|720p|480p|4k|2160p|bluray|blu-ray|web-dl|webrip|hdrip|x264|x265|hevc|aac|dts)\\b"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        /**
         * Attempts to parse a 4-digit year from the raw title if not explicitly provided.
         */
        fun extractYear(title: String): Int? {
            val match = Regex("\\b(19\\d\\d|20\\d\\d)\\b").findAll(title).lastOrNull()
            return match?.value?.toIntOrNull()
        }

        fun subtitleDirectory(context: Context): File {
            val dir = File(context.cacheDir, "subtitles")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }
    }

    val apiKey: String
        get() = prefs?.getString(PREF_KEY_OPENSUBTITLES_API_KEY, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_API_KEY

    /**
     * Searches OpenSubtitles for subtitles matching the title, year, and language.
     * E.g. language = "es" for Spanish, "en" for English.
     */
    suspend fun search(
        title: String,
        year: Int? = null,
        language: String? = null
    ): List<OpenSubtitlesSearchResult> = withContext(Dispatchers.IO) {
        runCatching {
            val cleaned = cleanMovieTitle(title)
            val effectiveYear = year ?: extractYear(title)
            val languagesParam = language?.trim()?.takeIf { it.isNotEmpty() } ?: "es,en"
            val queryParam = URLEncoder.encode(cleaned, "UTF-8")
            val urlBuilder = StringBuilder("$BASE_URL/subtitles?languages=$languagesParam&query=$queryParam")
            if (effectiveYear != null && effectiveYear > 1900) {
                urlBuilder.append("&year=$effectiveYear")
            }

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .header("Api-Key", apiKey)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@runCatching emptyList()

            val body = response.body?.string().orEmpty()
            parseSearchResults(body)
        }.getOrDefault(emptyList())
    }

    /**
     * Parses the JSON response from OpenSubtitles REST API v1.
     */
    internal fun parseSearchResults(jsonString: String): List<OpenSubtitlesSearchResult> {
        return runCatching {
            val json = JSONObject(jsonString)
            val data = json.optJSONArray("data") ?: return emptyList()
            val results = mutableListOf<OpenSubtitlesSearchResult>()

            for (i in 0 until data.length()) {
                val item = data.optJSONObject(i) ?: continue
                val id = item.optString("id")
                val attributes = item.optJSONObject("attributes") ?: continue
                val language = attributes.optString("language", "en")
                val downloadCount = attributes.optInt("download_count", 0)
                val ratings = attributes.optDouble("ratings", 0.0)
                val hearingImpaired = attributes.optBoolean("hearing_impaired", false)
                val release = attributes.optString("release", "")
                val files = attributes.optJSONArray("files")
                val firstFile = files?.optJSONObject(0) ?: continue
                val fileId = firstFile.optInt("file_id", 0)
                val fileName = firstFile.optString("file_name", release).ifBlank { release }

                if (fileId > 0) {
                    val langLabel = when (language.lowercase()) {
                        "es" -> "Spanish (Español)"
                        "en" -> "English"
                        "fr" -> "French (Français)"
                        "pt" -> "Portuguese"
                        "it" -> "Italian"
                        else -> language.uppercase()
                    }
                    results.add(
                        OpenSubtitlesSearchResult(
                            id = id,
                            fileId = fileId,
                            language = language,
                            label = "$langLabel · $fileName",
                            downloadCount = downloadCount,
                            ratings = ratings,
                            hearingImpaired = hearingImpaired,
                            releaseName = fileName
                        )
                    )
                }
            }
            results.sortedByDescending { it.downloadCount }
        }.getOrDefault(emptyList())
    }

    /**
     * Downloads the given subtitle file and saves it to local cache.
     */
    suspend fun downloadSubtitle(
        context: Context,
        movieId: String,
        result: OpenSubtitlesSearchResult
    ): File? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = subtitleDirectory(context)
            val sanitizedMovieId = movieId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val targetFile = File(dir, "${sanitizedMovieId}_${result.language}_${result.fileId}.srt")
            if (targetFile.exists() && targetFile.length() > 0) {
                return@runCatching targetFile
            }

            val requestBody = JSONObject().put("file_id", result.fileId).toString()
                .toRequestBody("application/json".toMediaType())

            val downloadReq = Request.Builder()
                .url("$BASE_URL/download")
                .header("Api-Key", apiKey)
                .header("User-Agent", USER_AGENT)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .post(requestBody)
                .build()

            val downloadResp = client.newCall(downloadReq).execute()
            if (!downloadResp.isSuccessful) return@runCatching null

            val downloadJson = JSONObject(downloadResp.body?.string().orEmpty())
            val srtUrl = downloadJson.optString("link")
            if (srtUrl.isBlank()) return@runCatching null

            val srtReq = Request.Builder()
                .url(srtUrl)
                .header("User-Agent", USER_AGENT)
                .get()
                .build()

            val srtResp = client.newCall(srtReq).execute()
            if (!srtResp.isSuccessful) return@runCatching null

            val tempFile = File(dir, "${targetFile.name}.tmp")
            tempFile.outputStream().use { out ->
                srtResp.body?.byteStream()?.copyTo(out)
            }
            if (tempFile.exists() && tempFile.length() > 0) {
                tempFile.renameTo(targetFile)
                targetFile
            } else {
                tempFile.delete()
                null
            }
        }.getOrNull()
    }

    /**
     * Checks if a cached subtitle exists for the given movie and language.
     */
    fun getCachedSubtitle(context: Context, movieId: String, language: String): SubtitleTrack? {
        val dir = subtitleDirectory(context)
        val sanitizedMovieId = movieId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val prefix = "${sanitizedMovieId}_${language.lowercase()}_"
        val matchingFile = dir.listFiles()?.firstOrNull { file ->
            file.name.startsWith(prefix) && file.name.endsWith(".srt") && file.length() > 0
        } ?: return null

        val label = if (language.equals("es", ignoreCase = true)) "Spanish (Español)" else "English"
        return SubtitleTrack(
            id = "cached_${matchingFile.nameWithoutExtension}",
            language = language.lowercase(),
            label = "$label (Cached)",
            uri = Uri.fromFile(matchingFile).toString()
        )
    }

    /**
     * Returns all cached subtitles found for this movie.
     */
    fun getAllCachedSubtitles(context: Context, movieId: String): List<SubtitleTrack> {
        val dir = subtitleDirectory(context)
        val sanitizedMovieId = movieId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val prefix = "${sanitizedMovieId}_"
        val files = dir.listFiles()?.filter {
            it.name.startsWith(prefix) && it.name.endsWith(".srt") && it.length() > 0
        } ?: return emptyList()

        return files.map { file ->
            val parts = file.nameWithoutExtension.removePrefix(prefix).split("_")
            val lang = parts.firstOrNull() ?: "en"
            val label = if (lang.equals("es", ignoreCase = true)) "Spanish (Español)" else "English"
            SubtitleTrack(
                id = "cached_${file.nameWithoutExtension}",
                language = lang.lowercase(),
                label = "$label (Cached)",
                uri = Uri.fromFile(file).toString()
            )
        }
    }
}
