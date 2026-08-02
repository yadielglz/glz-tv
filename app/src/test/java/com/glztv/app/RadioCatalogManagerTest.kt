package com.glztv.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioCatalogManagerTest {
    @Test
    fun parsesHubStationCatalog() {
        val stations = RadioCatalogManager.parseCatalog(
            """{"stations":[{"code":"RADIO_WORO","name":"FM 92.5 | RADIO ORO","genre":"FM Radio","streamUrl":"https://example.com/live","logoUrl":"https://example.com/logo.png","bitrateKbps":128,"requestHeaders":{"Referer":"https://example.com"}}]}"""
        )
        assertEquals(1, stations.size)
        assertEquals("RADIO_WORO", stations.single().code)
        assertEquals("https://example.com/live", stations.single().streamUrl)
        assertEquals("https://example.com", stations.single().requestHeaders["Referer"])
    }

    @Test
    fun ignoresStationsWithoutStreams() {
        val stations = RadioCatalogManager.parseCatalog("""{"stations":[{"code":"EMPTY"}]}""")
        assertTrue(stations.isEmpty())
    }
}
