package com.glztv.app.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackPreferenceManagerTest {
    @Test fun returnsCompatiblePreferredLanguage() {
        assertEquals("es", TrackPreferenceManager.preferredLanguage(listOf("en", "es"), "es"))
    }

    @Test fun ignoresUnavailablePreferredLanguage() {
        assertNull(TrackPreferenceManager.preferredLanguage(listOf("en", "es"), "fr"))
    }

    @Test fun subtitleOffDoesNotSelectAStoredLanguage() {
        assertNull(TrackPreferenceManager.preferredSubtitle(listOf("en", "es"), false, "es"))
    }

    @Test fun subtitlePreferenceUsesOnlyAnAvailableLanguage() {
        assertEquals("es", TrackPreferenceManager.preferredSubtitle(listOf("en", "es"), true, "es"))
        assertNull(TrackPreferenceManager.preferredSubtitle(listOf("en", "es"), true, "fr"))
    }
}
