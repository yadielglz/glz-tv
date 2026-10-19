package com.glztv.app.data

import com.glztv.app.Movie
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenSubtitlesRepositoryTest {

    @Test
    fun cleansMovieTitlesProperly() {
        assertEquals("Inception", OpenSubtitlesRepository.cleanMovieTitle("Inception [1080p]"))
        assertEquals("The Matrix", OpenSubtitlesRepository.cleanMovieTitle("The Matrix (1999) [4K HDR]"))
        assertEquals("Interstellar", OpenSubtitlesRepository.cleanMovieTitle("Interstellar [WEB-DL 2160p x265]"))
        assertEquals("Avatar", OpenSubtitlesRepository.cleanMovieTitle("Avatar (2009)"))
    }

    @Test
    fun extractsReleaseYearFromTitle() {
        assertEquals(2010, OpenSubtitlesRepository.extractYear("Inception (2010)"))
        assertEquals(1999, OpenSubtitlesRepository.extractYear("The Matrix 1999 [1080p]"))
        assertEquals(2024, OpenSubtitlesRepository.extractYear("Dune: Part Two 2024"))
        assertNull(OpenSubtitlesRepository.extractYear("Some Random Movie Without Year"))
    }

    @Test
    fun parsesOpenSubtitlesV1JsonResponse() {
        val json = """
        {
            "total_pages": 1,
            "total_count": 2,
            "data": [
                {
                    "id": "1001",
                    "attributes": {
                        "language": "es",
                        "download_count": 4500,
                        "ratings": 4.8,
                        "hearing_impaired": false,
                        "release": "Inception.2010.1080p.BluRay",
                        "files": [
                            {
                                "file_id": 998877,
                                "file_name": "Inception.2010.1080p.BluRay.srt"
                            }
                        ]
                    }
                },
                {
                    "id": "1002",
                    "attributes": {
                        "language": "en",
                        "download_count": 8900,
                        "ratings": 5.0,
                        "hearing_impaired": true,
                        "release": "Inception.2010.720p.WEB",
                        "files": [
                            {
                                "file_id": 998878,
                                "file_name": "Inception.2010.720p.WEB.srt"
                            }
                        ]
                    }
                }
            ]
        }
        """.trimIndent()

        val repo = OpenSubtitlesRepository()
        val results = repo.parseSearchResults(json)

        assertEquals(2, results.size)
        // Should be sorted by downloadCount descending (8900 first, then 4500)
        assertEquals(998878, results[0].fileId)
        assertEquals("en", results[0].language)
        assertEquals(8900, results[0].downloadCount)
        assertTrue(results[0].hearingImpaired)

        assertEquals(998877, results[1].fileId)
        assertEquals("es", results[1].language)
        assertEquals(4500, results[1].downloadCount)
    }

    @Test
    fun parsesMovieWithPreConfiguredSubtitles() {
        val movieJson = JSONObject().apply {
            put("id", "mov-1")
            put("title", "Gladiator")
            put("streamUrl", "https://stream.example.com/gladiator.mp4")
            put("subtitles", JSONArray().apply {
                put(JSONObject().apply {
                    put("language", "es")
                    put("label", "Spanish")
                    put("url", "https://subs.example.com/gladiator-es.srt")
                })
            })
        }

        val array = JSONArray().put(movieJson)
        val prefs = object : android.content.SharedPreferences {
            override fun getAll(): MutableMap<String, *> = mutableMapOf(com.glztv.app.GlzHubManager.MANAGED_MOVIES to array.toString())
            override fun getString(key: String?, defValue: String?): String? = if (key == com.glztv.app.GlzHubManager.MANAGED_MOVIES) array.toString() else defValue
            override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
            override fun getInt(key: String?, defValue: Int): Int = defValue
            override fun getLong(key: String?, defValue: Long): Long = defValue
            override fun getFloat(key: String?, defValue: Float): Float = defValue
            override fun getBoolean(key: String?, defValue: Boolean): Boolean = defValue
            override fun contains(key: String?): Boolean = key == com.glztv.app.GlzHubManager.MANAGED_MOVIES
            override fun edit(): android.content.SharedPreferences.Editor = throw UnsupportedOperationException()
            override fun registerOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}
            override fun unregisterOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}
        }
        val movies = Movie.fromPreferences(prefs)

        assertEquals(1, movies.size)
        val movie = movies.first()
        assertEquals("Gladiator", movie.title)
        assertEquals(1, movie.subtitles.size)
        assertEquals("es", movie.subtitles[0].language)
        assertEquals("https://subs.example.com/gladiator-es.srt", movie.subtitles[0].uri)
    }
}
