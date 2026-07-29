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
}
