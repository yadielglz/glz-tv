package com.glztv.app

import org.junit.Assert.*
import org.junit.Test

class LogoArtworkProcessorTest {
    private val red = 0xffe60012.toInt()
    private val blue = 0xff0055ff.toInt()
    @Test fun espnWhiteCanvasBecomesRedBadgeWithContrastingInk() {
        val pixels = IntArray(81) { -1 }
        for (y in 2..6) for (x in 2..6) pixels[y*9+x] = red
        val result = LogoArtworkProcessor.process(pixels, 9, 9, ChannelLogoStyle(), "ESPN HD")
        assertEquals(red, result.background)
        assertEquals(0, result.pixels[0])
        assertTrue(LogoArtworkProcessor.contrast(result.background, result.pixels[40]) >= 4.5)
        assertEquals(2, result.left)
        assertArrayEquals(IntArray(81) { if (it/9 in 2..6 && it%9 in 2..6) red else -1 }, pixels)
    }
    @Test fun multicolorKeepsEnclosedWhiteDetails() {
        val pixels = IntArray(81) { -1 }
        for (y in 1..7) for (x in 1..7) pixels[y*9+x] = if (x < 4) red else blue
        pixels[40] = -1
        val result = LogoArtworkProcessor.process(pixels, 9, 9, ChannelLogoStyle(), "Network")
        assertEquals(red, result.pixels[20])
        assertEquals(blue, result.pixels[24])
        assertEquals(-1, result.pixels[40])
        assertEquals(0, result.pixels[0])
    }
    @Test fun manualMonochromeAndCleanupOptOutAreHonored() {
        val result = LogoArtworkProcessor.process(IntArray(9) { red }, 3, 3,
            ChannelLogoStyle("#000000", "#FFFFFF", "monochrome", false), "Channel")
        assertEquals(0xff000000.toInt(), result.background)
        assertTrue(result.pixels.all { it == -1 })
    }
    @Test fun coloredCanvasExpandsToFullBadgeWhileWhiteLetteringSurvives() {
        val pixels = IntArray(81) { red }
        for (y in 3..5) for (x in 3..5) pixels[y*9+x] = -1
        val result = LogoArtworkProcessor.process(pixels, 9, 9, ChannelLogoStyle(), "ESPN")
        assertEquals(red, result.background)
        assertEquals(-1, result.pixels[40])
        assertEquals(0, result.pixels[0])
    }
    @Test fun invalidColorsFallBackAndEmptyArtworkDoesNotCrash() {
        val result = LogoArtworkProcessor.process(IntArray(9), 3, 3, ChannelLogoStyle("bad", "bad"), "Unknown")
        assertEquals(0xff243447.toInt(), result.background)
        assertTrue(result.right < result.left)
    }
    @Test fun removesInsetColoredCanvasInsideTransparentMargins() {
        val pixels = IntArray(121)
        for (y in 1..9) for (x in 1..9) pixels[y*11+x] = red
        for (y in 4..6) for (x in 3..7) pixels[y*11+x] = -1
        val result = LogoArtworkProcessor.process(pixels, 11, 11, ChannelLogoStyle(), "ESPN")
        assertEquals(red, result.background)
        assertEquals(-1, result.pixels[5*11+5])
        assertEquals(0, result.pixels[11+1])
        assertEquals(3, result.left)
    }
    @Test fun translucentCanvasCornersAreRemoved() {
        val pixels = IntArray(81) { red }
        for (i in listOf(0,8,72,80)) pixels[i] = (230 shl 24) or (red and 0xffffff)
        for (y in 3..5) for (x in 3..5) pixels[y*9+x] = -1
        val result = LogoArtworkProcessor.process(pixels, 9, 9, ChannelLogoStyle(), "ESPN")
        assertEquals(0, result.pixels[0])
        assertEquals(0, result.pixels[1])
        assertEquals(-1, result.pixels[40])
    }
}
