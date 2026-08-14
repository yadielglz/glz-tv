package com.glztv.app.player

import java.net.SocketTimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PlaybackDiagnosticsTest {
    @Test fun categorizesTimeouts() {
        assertEquals(PlaybackErrorCategory.TIMEOUT, PlaybackErrorCategorizer.categorize(SocketTimeoutException()))
    }

    @Test fun categorizesHttpFailures() {
        assertEquals(PlaybackErrorCategory.HTTP_401, PlaybackErrorCategorizer.httpCategory(401))
        assertEquals(PlaybackErrorCategory.HTTP_403, PlaybackErrorCategorizer.httpCategory(403))
        assertEquals(PlaybackErrorCategory.HTTP_404, PlaybackErrorCategorizer.httpCategory(404))
        assertEquals(PlaybackErrorCategory.HTTP_5XX, PlaybackErrorCategorizer.httpCategory(503))
    }

    @Test fun sanitizesDiagnosticReport() {
        val report = PlaybackErrorCategorizer.sanitizedReport(
            PlaybackErrorCategory.NETWORK_ERROR,
            "News https://example.com/live?token=secret authorization=hidden",
            42
        )
        assertFalse(report.contains("example.com"))
        assertFalse(report.contains("secret"))
        assertFalse(report.contains("hidden"))
        assertEquals("NETWORK_ERROR|42|News", report)
    }
}
