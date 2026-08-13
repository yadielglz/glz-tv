package com.glztv.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {
    @Test
    fun parsesAttributesRelativeUrlsAndPerChannelHeaders() {
        val channels = M3uParser.parse(
            """
                #EXTM3U
                #EXTINF:-1 tvg-id="wkaq" tvg-name="Telemundo" tvg-logo="logos/wkaq.png" group-title="Local" tvg-chno="2.1",Telemundo PR
                #EXTVLCOPT:http-user-agent=GLZ TV
                #EXTVLCOPT:http-referrer=https://example.com/player
                streams/wkaq.m3u8
            """.trimIndent(),
            "https://example.com/iptv/list.m3u",
            mapOf("Authorization" to "Bearer token")
        )

        val channel = channels.single()
        assertEquals("wkaq", channel.id)
        assertEquals("Telemundo PR", channel.name)
        assertEquals("Local", channel.group)
        assertEquals("2.1", channel.number)
        assertEquals("logos/wkaq.png", channel.logoUrl)
        assertEquals("https://example.com/iptv/streams/wkaq.m3u8", channel.streamUrl)
        assertEquals("GLZ TV", channel.headers["User-Agent"])
        assertEquals("https://example.com/player", channel.headers["Referer"])
        assertEquals("Bearer token", channel.headers["Authorization"])
    }

    @Test
    fun ignoresMalformedAndUnassociatedLines() {
        val channels = M3uParser.parse(
            """
                not-a-channel
                #EXTINF:-1 tvg-id="valid",Valid
                https://example.com/live.m3u8
                #EXTINF:-1,Broken without stream
            """.trimIndent(),
            "https://example.com/list.m3u",
            emptyMap()
        )

        assertEquals(1, channels.size)
        assertEquals("valid", channels.single().id)
    }

    @Test
    fun preservesDuplicateEntriesForProviderCompatibility() {
        val channels = M3uParser.parse(
            """
                #EXTINF:-1 tvg-id="same",One
                https://example.com/one.m3u8
                #EXTINF:-1 tvg-id="same",Two
                https://example.com/two.m3u8
            """.trimIndent(),
            "https://example.com/list.m3u",
            emptyMap()
        )

        assertEquals(2, channels.size)
        assertTrue(channels.all { it.id == "same" })
    }
}
