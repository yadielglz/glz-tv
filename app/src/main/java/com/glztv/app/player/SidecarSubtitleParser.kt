package com.glztv.app.player

import android.content.Context
import android.net.Uri
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class SubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

/**
 * Lightweight SRT / WebVTT sidecar parser used to render external subtitles
 * (e.g. OpenSubtitles) as a Compose overlay without calling ExoPlayer.setMediaItem()
 * or interrupting/reloading the active video stream.
 */
object SidecarSubtitleParser {
    private val TIMESTAMP_REGEX = Regex(
        """(?:(\d{1,2}):)?(\d{2}):(\d{2})[.,](\d{1,3})\s*-->\s*(?:(\d{1,2}):)?(\d{2}):(\d{2})[.,](\d{1,3})"""
    )
    private val HTML_TAG_REGEX = Regex("""<[^>]+>""")
    private val ASS_TAG_REGEX = Regex("""\{\\[^}]+\}""")

    fun loadFromUri(context: Context, uriString: String): List<SubtitleCue> {
        val raw = runCatching { readText(context, uriString) }.getOrNull() ?: return emptyList()
        return parse(raw)
    }

    private fun readText(context: Context, uriString: String): String? {
        val trimmed = uriString.trim()
        if (trimmed.isEmpty()) return null
        return when {
            trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) -> {
                val conn = (URL(trimmed).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8_000
                    readTimeout = 10_000
                    setRequestProperty("User-Agent", "GLZ-TV/2.0")
                }
                try {
                    if (conn.responseCode in 200..299) {
                        conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    } else null
                } finally {
                    conn.disconnect()
                }
            }
            trimmed.startsWith("file://", ignoreCase = true) -> {
                val path = Uri.parse(trimmed).path ?: return null
                File(path).takeIf { it.exists() }?.readText(Charsets.UTF_8)
            }
            trimmed.startsWith("content://", ignoreCase = true) -> {
                context.contentResolver.openInputStream(Uri.parse(trimmed))
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
            }
            else -> {
                File(trimmed).takeIf { it.exists() }?.readText(Charsets.UTF_8)
            }
        }
    }

    fun parse(content: String): List<SubtitleCue> {
        if (content.isBlank()) return emptyList()
        val normalized = content
            .removePrefix("\uFEFF")
            .replace("\r\n", "\n")
            .replace('\r', '\n')

        val cues = ArrayList<SubtitleCue>(256)
        val lines = normalized.split('\n')
        var i = 0
        while (i < lines.size) {
            val match = TIMESTAMP_REGEX.find(lines[i])
            if (match == null) {
                i++
                continue
            }
            val startMs = toMillis(
                match.groupValues[1],
                match.groupValues[2],
                match.groupValues[3],
                match.groupValues[4]
            )
            val endMs = toMillis(
                match.groupValues[5],
                match.groupValues[6],
                match.groupValues[7],
                match.groupValues[8]
            )
            i++
            val textBuilder = StringBuilder()
            while (i < lines.size && lines[i].isNotBlank()) {
                val cleaned = lines[i]
                    .replace(ASS_TAG_REGEX, "")
                    .replace(HTML_TAG_REGEX, "")
                    .trim()
                if (cleaned.isNotEmpty()) {
                    if (textBuilder.isNotEmpty()) textBuilder.append('\n')
                    textBuilder.append(cleaned)
                }
                i++
            }
            if (textBuilder.isNotEmpty() && endMs > startMs) {
                cues.add(SubtitleCue(startMs, endMs, textBuilder.toString()))
            }
        }
        cues.sortBy { it.startMs }
        return cues
    }

    fun activeCueText(cues: List<SubtitleCue>, positionMs: Long): String? {
        if (cues.isEmpty() || positionMs < 0L) return null
        // Binary search for the last cue whose startMs <= positionMs
        var low = 0
        var high = cues.lastIndex
        var candidate = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (cues[mid].startMs <= positionMs) {
                candidate = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        if (candidate < 0) return null
        // Scan backwards slightly to support overlapping cues
        val active = mutableListOf<String>()
        var idx = candidate
        while (idx >= 0 && idx >= candidate - 4) {
            val cue = cues[idx]
            if (positionMs in cue.startMs..cue.endMs) {
                active.add(0, cue.text)
            }
            idx--
        }
        return active.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }

    private fun toMillis(h: String, m: String, s: String, ms: String): Long {
        val hours = h.toLongOrNull() ?: 0L
        val minutes = m.toLongOrNull() ?: 0L
        val seconds = s.toLongOrNull() ?: 0L
        val paddedMs = when (ms.length) {
            1 -> (ms.toLongOrNull() ?: 0L) * 100L
            2 -> (ms.toLongOrNull() ?: 0L) * 10L
            else -> ms.take(3).toLongOrNull() ?: 0L
        }
        return ((hours * 3600L + minutes * 60L + seconds) * 1000L) + paddedMs
    }
}
