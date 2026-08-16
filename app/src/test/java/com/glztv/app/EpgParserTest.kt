package com.glztv.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EpgParserTest {
    @Test
    fun parsesAndMatchesGlzXmlTvShape() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
              <channel id="WKAQ.us">
                <display-name>Telemundo US</display-name>
              </channel>
              <programme channel="WKAQ.us"
                  start="20260729180000 -0400"
                  stop="20260729190000 -0400">
                <title>Telenoticias</title>
                <desc>Noticias de Puerto Rico.</desc>
              </programme>
            </tv>
        """.trimIndent()

        val guide = EpgParser.parse(xml)
        val channel = Channel(
            "WKAQ.us", "Telemundo PR", "TV", "1", "https://example.com/logo.png",
            "https://example.com/live.m3u8", emptyMap()
        )

        assertEquals(1, guide.programmeCount)
        assertEquals("Telenoticias", guide.forChannel(channel).single().title)
        assertTrue(guide.forChannel(channel).single().endMillis >
            guide.forChannel(channel).single().startMillis)
    }

    @Test
    fun parsesChannelLogosAndResolvesFallback() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
              <channel id="WKAQ.us">
                <display-name>Telemundo PR</display-name>
                <icon src="https://example.com/epg_logo.png" />
              </channel>
            </tv>
        """.trimIndent()

        val guide = EpgParser.parse(xml)
        val channelWithoutLogo = Channel(
            "WKAQ.us", "Telemundo PR", "TV", "1", "",
            "https://example.com/live.m3u8", emptyMap()
        )

        assertEquals("https://example.com/epg_logo.png", guide.logoForChannel(channelWithoutLogo))
    }

    @Test
    fun prefersExactChannelNameMatchOverPrefixMatch() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
              <channel id="hbo2">
                <display-name>HBO 2</display-name>
              </channel>
              <channel id="hbo">
                <display-name>HBO</display-name>
              </channel>
              <programme channel="hbo" start="20260729180000 +0000" stop="20260729190000 +0000">
                <title>HBO Movie</title>
                <desc>Movie</desc>
              </programme>
            </tv>
        """.trimIndent()

        val guide = EpgParser.parse(xml)
        val channel = Channel("", "HBO", "Movies", "1", "", "https://example.com/hbo.m3u8", emptyMap())
        assertEquals("HBO Movie", guide.forChannel(channel).single().title)
    }

    @Test
    fun parsesIsoTimezoneWithColon() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
              <channel id="test">
                <display-name>Test</display-name>
              </channel>
              <programme channel="test" start="20260729180000 +02:00" stop="20260729190000 +02:00">
                <title>Iso Program</title>
                <desc>Desc</desc>
              </programme>
            </tv>
        """.trimIndent()

        val guide = EpgParser.parse(xml)
        val channel = Channel("test", "Test", "TV", "1", "", "https://example.com/live.m3u8", emptyMap())
        assertEquals("Iso Program", guide.forChannel(channel).single().title)
    }
}
