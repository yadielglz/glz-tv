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
}
