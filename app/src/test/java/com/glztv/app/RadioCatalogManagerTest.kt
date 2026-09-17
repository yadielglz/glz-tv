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
    fun parsesMultipleStationsIncludingDuplicatesOrMissingCodes() {
        val json = """{"stations":[
            {"code":"DUPLICATE","name":"Station 1","streamUrl":"https://example.com/1"},
            {"code":"DUPLICATE","name":"Station 2","streamUrl":"https://example.com/2"},
            {"name":"No Code Station","streamUrl":"https://example.com/3"}
        ]}"""
        val stations = RadioCatalogManager.parseCatalog(json)
        assertEquals(3, stations.size)
        assertEquals("DUPLICATE", stations[0].code)
        assertEquals("DUPLICATE", stations[1].code)
        assertEquals("", stations[2].code)
    }
}

