package com.glztv.app.player

import java.net.SocketTimeoutException
import androidx.media3.common.MimeTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PlaybackDiagnosticsTest {
    @Test fun recognizesHlsEvenWhenTheUrlOrServerCannotExposeAUsefulExtension() {
        assertEquals(
            MimeTypes.APPLICATION_M3U8,
            PlaybackErrorCategorizer.mediaMimeType("https://example.com/movie.m3u8?token=abc")
        )
        assertEquals(
            MimeTypes.APPLICATION_M3U8,
            PlaybackErrorCategorizer.mediaMimeType("https://example.com/playback?id=42", forceHls = true)
        )
        assertEquals(null, PlaybackErrorCategorizer.mediaMimeType("https://example.com/video.mp4"))
    }

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
