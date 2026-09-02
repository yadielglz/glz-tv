package com.glztv.app

import com.glztv.app.data.PreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class PreferencesRepositoryTest {
    @Test
    fun parsesLegacyMultilineRequestHeaders() {
        assertEquals(
            mapOf("User-Agent" to "GLZ TV", "Referer" to "https://example.com/a:b"),
            PreferencesRepository.parseRequestHeaders(
                "User-Agent: GLZ TV\ninvalid\nReferer: https://example.com/a:b"
            )
        )
    }

    @Test
    fun parsesChannelFallbacksWithSingleAndMultipleUrls() {
        assertEquals(
            mapOf(
                "2" to listOf("https://a.example/wkaq.m3u8"),
                "wkaq" to listOf("https://a.example/wkaq.m3u8", "https://b.example/wkaq.m3u8")
            ),
            PreferencesRepository.parseChannelFallbacks(
                """
                    2 = https://a.example/wkaq.m3u8

                    wkaq = https://a.example/wkaq.m3u8 , https://b.example/wkaq.m3u8
                    no-equals-line
                    3 =
                """.trimIndent()
            )
        )
    }
}
