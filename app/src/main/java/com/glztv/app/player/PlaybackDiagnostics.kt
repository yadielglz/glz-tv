package com.glztv.app.player

import android.net.Uri
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource

enum class PlaybackErrorCategory {
    HTTP_401, HTTP_403, HTTP_404, HTTP_5XX, TIMEOUT, NETWORK_ERROR,
    MANIFEST_ERROR, DECODER_ERROR, SOURCE_ERROR, UNKNOWN
}

data class PlaybackDiagnostics(
    val channelName: String,
    val protocol: String?,
    val resolution: String?,
    val videoCodec: String?,
    val audioCodec: String?,
    val bitrate: Int?,
    val bufferDurationMs: Long,
    val droppedFrames: Long,
    val networkTransport: String?,
    val playbackState: String,
    val lastError: PlaybackErrorCategory?
)

object PlaybackErrorCategorizer {
    fun categorize(error: Throwable): PlaybackErrorCategory {
        val causes = generateSequence(error) { it.cause }.toList()
        val httpCode = causes.filterIsInstance<HttpDataSource.InvalidResponseCodeException>()
            .firstOrNull()?.responseCode
        httpCode?.let(::httpCategory)?.let { return it }
        return when {
            causes.any { it is java.net.SocketTimeoutException } -> PlaybackErrorCategory.TIMEOUT
            error.message?.contains("manifest", ignoreCase = true) == true -> PlaybackErrorCategory.MANIFEST_ERROR
            error is PlaybackException && error.errorCode in 2000..2002 -> PlaybackErrorCategory.NETWORK_ERROR
            error is PlaybackException && error.errorCode in 3000..3999 -> PlaybackErrorCategory.DECODER_ERROR
            error is PlaybackException && error.errorCode in 1000..1999 -> PlaybackErrorCategory.SOURCE_ERROR
            causes.any { it is java.io.IOException } -> PlaybackErrorCategory.NETWORK_ERROR
            else -> PlaybackErrorCategory.UNKNOWN
        }
    }

    internal fun httpCategory(code: Int): PlaybackErrorCategory? = when {
        code == 401 -> PlaybackErrorCategory.HTTP_401
        code == 403 -> PlaybackErrorCategory.HTTP_403
        code == 404 -> PlaybackErrorCategory.HTTP_404
        code >= 500 -> PlaybackErrorCategory.HTTP_5XX
        else -> null
    }

    fun protocol(streamUrl: String): String? {
        val uri = runCatching { Uri.parse(streamUrl) }.getOrNull() ?: return null
        return when {
            uri.lastPathSegment?.contains(".m3u8", true) == true -> "HLS"
            uri.scheme.equals("http", true) || uri.scheme.equals("https", true) -> "HTTP"
            else -> uri.scheme?.uppercase()
        }
    }

    fun sanitizedReport(category: PlaybackErrorCategory, channelName: String, timestamp: Long): String =
        "${category.name}|${timestamp.coerceAtLeast(0)}|${sanitizeLabel(channelName)}"

    private fun sanitizeLabel(value: String): String = value
        .replace(Regex("https?://\\S+", RegexOption.IGNORE_CASE), "")
        .replace(Regex("(?i)(token|authorization|password|cookie)\\s*[:=]\\s*\\S+"), "")
        .filter { !it.isISOControl() }
        .trim()
        .take(120)
}
