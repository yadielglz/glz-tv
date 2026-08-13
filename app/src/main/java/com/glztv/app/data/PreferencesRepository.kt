package com.glztv.app.data

import android.content.Context
import android.content.SharedPreferences

/** Single entry point for the legacy preference file and source-related keys. */
class PreferencesRepository(context: Context) {
    val sharedPreferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    val playlistUrl: String
        get() = sharedPreferences.getString(PLAYLIST_URL, DEFAULT_PLAYLIST_URL)
            .orEmpty().ifBlank { DEFAULT_PLAYLIST_URL }

    val epgUrl: String
        get() = sharedPreferences.getString(EPG_URL, DEFAULT_EPG_URL)
            .orEmpty().ifBlank { DEFAULT_EPG_URL }

    val requestHeaders: Map<String, String>
        get() = parseRequestHeaders(sharedPreferences.getString(REQUEST_HEADERS, "").orEmpty())

    companion object {
        const val FILE_NAME = "glz_tv"
        const val PLAYLIST_URL = "playlist_url"
        const val EPG_URL = "epg_url"
        const val REQUEST_HEADERS = "request_headers"
        const val DEFAULT_PLAYLIST_URL = "http://play.glztech.com/list.m3u"
        const val DEFAULT_EPG_URL = "https://play.glztech.com/epg.xml.gz"

        fun parseRequestHeaders(source: String): Map<String, String> = buildMap {
            source.lineSequence().forEach { line ->
                val separator = line.indexOf(':')
                if (separator > 0) {
                    put(line.take(separator).trim(), line.drop(separator + 1).trim())
                }
            }
        }
    }
}
