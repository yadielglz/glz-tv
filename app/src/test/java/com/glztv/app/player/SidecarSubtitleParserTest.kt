package com.glztv.app.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SidecarSubtitleParserTest {
    @Test
    fun parsesSrtAndStripsFormattingTags() {
        val srt = """
            1
            00:00:01,500 --> 00:00:04,000
            <i>Hello</i> world!

            2
            00:00:05,000 --> 00:00:08,250
            {\an8}Line one
            Line two
        """.trimIndent()

        val cues = SidecarSubtitleParser.parse(srt)
        assertEquals(2, cues.size)
        assertEquals("Hello world!", SidecarSubtitleParser.activeCueText(cues, 2_000L))
        assertNull(SidecarSubtitleParser.activeCueText(cues, 4_500L))
        assertEquals("Line one\nLine two", SidecarSubtitleParser.activeCueText(cues, 6_000L))
    }

    @Test
    fun parsesWebVttWithoutHours() {
        val vtt = """
            WEBVTT

            00:10.500 --> 00:12.000
            Welcome to GLZ TV
        """.trimIndent()

        val cues = SidecarSubtitleParser.parse(vtt)
        assertEquals(1, cues.size)
        assertEquals("Welcome to GLZ TV", SidecarSubtitleParser.activeCueText(cues, 11_000L))
    }
}
