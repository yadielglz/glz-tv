package com.glztv.app.player

import android.content.SharedPreferences

class TrackPreferenceManager(private val preferences: SharedPreferences) {
    var audioLanguage: String?
        get() = preferences.getString(AUDIO_LANGUAGE, null)
        set(value) = preferences.edit().putString(AUDIO_LANGUAGE, value).apply()

    var subtitlesEnabled: Boolean
        get() = preferences.getBoolean(SUBTITLES_ENABLED, false)
        set(value) = preferences.edit().putBoolean(SUBTITLES_ENABLED, value).apply()

    var subtitleLanguage: String?
        get() = preferences.getString(SUBTITLE_LANGUAGE, null)
        set(value) = preferences.edit().putString(SUBTITLE_LANGUAGE, value).apply()

    companion object {
        private const val AUDIO_LANGUAGE = "preferred_audio_language"
        private const val SUBTITLES_ENABLED = "captions_enabled"
        private const val SUBTITLE_LANGUAGE = "captions_language"

        internal fun preferredLanguage(available: List<String?>, preferred: String?): String? =
            preferred?.takeIf { wanted -> available.any { it.equals(wanted, ignoreCase = true) } }

        internal fun preferredSubtitle(
            available: List<String?>,
            enabled: Boolean,
            preferred: String?
        ): String? = if (enabled) preferredLanguage(available, preferred) else null
    }
}
