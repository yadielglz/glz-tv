package com.glztv.app

import androidx.compose.runtime.Immutable
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.InputSource

@Immutable
data class Programme(
    val channelId: String,
    val startMillis: Long,
    val endMillis: Long,
    val title: String,
    val description: String
)

@Immutable
data class EpgGuide(
    val programmes: Map<String, List<Programme>>,
    val channelNames: Map<String, String>,
    val channelLogos: Map<String, String> = emptyMap(),
    val allChannelNames: Map<String, Set<String>> = emptyMap()
) {
    private val programmeMatchCache = mutableMapOf<String, List<Programme>>()
    private val logoMatchCache = mutableMapOf<String, String?>()
    private val exactNormalizedNameMap: Map<String, String> by lazy {
        buildMap {
            channelNames.forEach { (id, name) ->
                val norm = normalize(name)
                if (norm.isNotBlank() && norm !in GENERIC_EVENT_NAMES) putIfAbsent(norm, id)
                val cleanNorm = normalize(cleanChannelName(name))
                if (cleanNorm.isNotBlank() && cleanNorm !in GENERIC_EVENT_NAMES) putIfAbsent(cleanNorm, id)
            }
            allChannelNames.forEach { (id, names) ->
                names.forEach { name ->
                    val norm = normalize(name)
                    if (norm.isNotBlank() && norm !in GENERIC_EVENT_NAMES) putIfAbsent(norm, id)
                    val cleanNorm = normalize(cleanChannelName(name))
                    if (cleanNorm.isNotBlank() && cleanNorm !in GENERIC_EVENT_NAMES) putIfAbsent(cleanNorm, id)
                }
            }
        }
    }

    companion object {
        val Empty = EpgGuide(emptyMap(), emptyMap(), emptyMap(), emptyMap())
        private val GENERIC_EVENT_NAMES = setOf("sportsppv", "ppv", "liveevent", "liveevents")
    }

    val programmeCount: Int get() = programmes.values.sumOf(List<Programme>::size)

    private fun findMatchedChannelId(channel: Channel): String? {
        if (channel.id.isNotBlank() && (programmes.containsKey(channel.id) || channelLogos.containsKey(channel.id) || channelNames.containsKey(channel.id))) {
            return channel.id
        }

        val cleanName = cleanChannelName(channel.name)
        val normClean = normalize(cleanName)
        val normRaw = normalize(channel.name)

        if (normClean in GENERIC_EVENT_NAMES || normRaw in GENERIC_EVENT_NAMES) {
            return null
        }

        if (normClean.isNotBlank()) {
            exactNormalizedNameMap[normClean]?.let { return it }
        }
        if (normRaw.isNotBlank()) {
            exactNormalizedNameMap[normRaw]?.let { return it }
        }

        if (normClean.length >= 3) {
            exactNormalizedNameMap.entries.firstOrNull { (normKey, _) ->
                normKey.length >= 3 && (normClean.startsWith(normKey) || normKey.startsWith(normClean) || normClean.contains(normKey) || normKey.contains(normClean))
            }?.value?.let { return it }
        }

        if (normRaw.length >= 3) {
            exactNormalizedNameMap.entries.firstOrNull { (normKey, _) ->
                normKey.length >= 3 && (normRaw.startsWith(normKey) || normKey.startsWith(normRaw) || normRaw.contains(normKey) || normKey.contains(normRaw))
            }?.value?.let { return it }
        }

        return null
    }

    fun forChannel(channel: Channel): List<Programme> {
        val cacheKey = "${channel.id}\u0000${channel.name}\u0000${channel.eventTitle}\u0000${channel.eventStartMillis}\u0000${channel.eventEndMillis}"
        return synchronized(programmeMatchCache) {
            programmeMatchCache.getOrPut(cacheKey) {
                programmes[channel.id]?.takeIf(List<Programme>::isNotEmpty)
                    ?: findMatchedChannelId(channel)?.let(programmes::get)?.takeIf(List<Programme>::isNotEmpty)
                    ?: fallbackEventProgramme(channel)
            }
        }
    }

    private fun fallbackEventProgramme(channel: Channel): List<Programme> {
        val title = channel.eventTitle.trim()
        if (title.isEmpty()) return emptyList()
        val now = System.currentTimeMillis()
        val start = if (channel.eventStartMillis > 0L) channel.eventStartMillis else now - 30 * 60_000L
        val end = if (channel.eventEndMillis > start) channel.eventEndMillis else start + 4 * 3600_000L
        val league = channel.eventLeague.ifBlank { "SPORTS" }
        val desc = "Live $league Event · $title"
        return listOf(
            Programme(
                channelId = channel.id.ifBlank { channel.name },
                startMillis = start,
                endMillis = end,
                title = title,
                description = desc
            )
        )
    }

    fun nowProgrammeForChannel(channel: Channel, nowMillis: Long): Programme? {
        val list = forChannel(channel)
        if (list.isEmpty()) return null
        var low = 0
        var high = list.size - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val p = list[mid]
            if (nowMillis < p.startMillis) {
                high = mid - 1
            } else if (nowMillis >= p.endMillis) {
                low = mid + 1
            } else {
                return p
            }
        }
        return null
    }

    fun nextProgrammeForChannel(channel: Channel, afterMillis: Long): Programme? {
        val list = forChannel(channel)
        if (list.isEmpty()) return null
        var low = 0
        var high = list.size - 1
        var candidate: Programme? = null
        while (low <= high) {
            val mid = (low + high) ushr 1
            val p = list[mid]
            if (p.startMillis >= afterMillis) {
                candidate = p
                high = mid - 1
            } else {
                low = mid + 1
            }
        }
        return candidate
    }

    fun prewarm(channels: Collection<Channel>) {
        for (ch in channels) {
            forChannel(ch)
            logoForChannel(ch)
        }
    }

    fun logoForChannel(channel: Channel): String? {
        val cacheKey = "${channel.id}\u0000${channel.name}"
        return synchronized(logoMatchCache) {
            if (logoMatchCache.containsKey(cacheKey)) return@synchronized logoMatchCache[cacheKey]
            val logo = channelLogos[channel.id]?.takeIf(String::isNotBlank)
                ?: findMatchedChannelId(channel)?.let { channelLogos[it] }?.takeIf(String::isNotBlank)
            logoMatchCache[cacheKey] = logo
            logo
        }
    }
}


object EpgParser {
    private val dateFormatTL = ThreadLocal.withInitial {
        SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US).apply { isLenient = false }
    }
    private val xmlTvTimeRegex = Regex("^(\\d{8,14})\\s*([+-]\\d{2}:?\\d{2}|Z|UTC|GMT)?", RegexOption.IGNORE_CASE)

    fun parse(inputSource: InputSource, cutoffMillis: Long = 0L): EpgGuide {
        val handler = GuideHandler(cutoffMillis)
        try {
            SAXParserFactory.newInstance().apply {
                isNamespaceAware = false
                isValidating = false
            }.newSAXParser().parse(inputSource, handler)
        } catch (e: Exception) {
            if (handler.programmes.isEmpty() && handler.channelNames.isEmpty()) {
                throw e
            }
        }

        val prunedProgrammes = handler.programmes.mapValues { (_, list) ->
            list.sortedBy(Programme::startMillis)
        }.filterValues { it.isNotEmpty() }
        return EpgGuide(prunedProgrammes, handler.channelNames, handler.channelLogos, handler.allChannelNames)
    }

    fun parse(xml: String, cutoffMillis: Long = 0L): EpgGuide {
        val sanitizingReader = XmlSanitizingReader(StringReader(xml))
        return parse(InputSource(sanitizingReader), cutoffMillis)
    }

    fun parse(stream: java.io.InputStream, cutoffMillis: Long = 0L): EpgGuide {
        val rawReader = java.io.BufferedReader(java.io.InputStreamReader(stream, Charsets.UTF_8), 32768)
        val sanitizingReader = XmlSanitizingReader(rawReader)
        return parse(InputSource(sanitizingReader), cutoffMillis)
    }

    private class XmlSanitizingReader(private val delegate: java.io.Reader) : java.io.Reader() {
        private val buffer = StringBuilder()
        private var readPos = 0
        private var isFirstChar = true
        private val chunkBuf = CharArray(16384)

        private fun fillBufferIfNeeded(minChars: Int) {
            while ((buffer.length - readPos) < minChars) {
                val n = delegate.read(chunkBuf, 0, chunkBuf.size)
                if (n == -1) break
                for (i in 0 until n) {
                    val ch = chunkBuf[i]
                    if (isFirstChar) {
                        isFirstChar = false
                        if (ch == '\uFEFF') continue
                    }
                    if (isInvalidXmlChar(ch.code)) continue
                    buffer.append(ch)
                }
            }
        }

        override fun read(cbuf: CharArray, off: Int, len: Int): Int {
            if (len <= 0) return 0
            var written = 0

            while (written < len) {
                fillBufferIfNeeded(64)
                if (readPos >= buffer.length) break

                val c = buffer[readPos]
                if (c == '&') {
                    sanitizeAmpersand()
                } else if (c == '<') {
                    sanitizeLessThan()
                }

                cbuf[off + written] = buffer[readPos]
                readPos++
                written++
            }

            if (readPos >= 16384) {
                buffer.delete(0, readPos)
                readPos = 0
            }

            return if (written > 0) written else -1
        }

        private fun sanitizeAmpersand() {
            val entityEnd = buffer.indexOf(';', readPos + 1)
            val isValid = entityEnd in (readPos + 1)..(readPos + 32) &&
                    isValidEntityStr(buffer.substring(readPos + 1, entityEnd))
            if (!isValid) {
                buffer.replace(readPos, readPos + 1, "&amp;")
            }
        }

        private fun sanitizeLessThan() {
            if (readPos + 1 >= buffer.length) return
            val next = buffer[readPos + 1]
            val isValidStart = next == '/' || next == '!' || next == '?' ||
                    next in 'a'..'z' || next in 'A'..'Z' || next == '_' || next == ':'
            if (!isValidStart) {
                buffer.replace(readPos, readPos + 1, "&lt;")
            }
        }

        override fun close() {
            delegate.close()
        }

        companion object {
            private val VALID_NAMED_ENTITIES = setOf("amp", "lt", "gt", "quot", "apos")

            private fun isInvalidXmlChar(code: Int): Boolean {
                if (code < 0) return false
                return !(code == 0x9 || code == 0xA || code == 0xD ||
                        (code in 0x20..0xD7FF) ||
                        (code in 0xE000..0xFFFD) ||
                        (code in 0x10000..0x10FFFF))
            }

            private fun isValidEntityStr(str: String): Boolean {
                if (str in VALID_NAMED_ENTITIES) return true
                if (str.startsWith("#")) {
                    val numStr = str.substring(1)
                    val code = if (numStr.startsWith("x", ignoreCase = true)) {
                        numStr.substring(1).toIntOrNull(16)
                    } else {
                        numStr.toIntOrNull(10)
                    } ?: return false
                    return !isInvalidXmlChar(code)
                }
                return false
            }
        }
    }

    private class GuideHandler(private val cutoffMillis: Long) : DefaultHandler() {
        val channelNames = mutableMapOf<String, String>()
        val allChannelNames = mutableMapOf<String, MutableSet<String>>()
        val channelLogos = mutableMapOf<String, String>()
        val programmes = mutableMapOf<String, MutableList<Programme>>()
        private var channelId: String? = null
        private var programmeId: String? = null
        private var startMillis = 0L
        private var endMillis = 0L
        private var title = ""
        private var description = ""
        private var activeTextTag: String? = null
        private val text = StringBuilder()

        override fun startElement(uri: String?, localName: String?, qName: String, attributes: Attributes) {
            when (qName) {
                "channel" -> channelId = attributes.getValue("id")
                "icon" -> {
                    val src = attributes.getValue("src")
                    if (!src.isNullOrBlank() && channelId != null) {
                        channelLogos[channelId!!] = src
                    }
                }
                "programme" -> {
                    programmeId = attributes.getValue("channel")
                    startMillis = parseXmlTvTime(attributes.getValue("start")) ?: 0L
                    endMillis = parseXmlTvTime(attributes.getValue("stop"))
                        ?: startMillis + 3_600_000L
                    title = "Live programming"
                    description = ""
                }
                "display-name", "title", "desc" -> {
                    activeTextTag = qName
                    text.setLength(0)
                }
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            if (activeTextTag != null) text.append(ch, start, length)
        }

        override fun endElement(uri: String?, localName: String?, qName: String) {
            val value = text.toString().trim()
            when (qName) {
                "display-name" -> channelId?.let { id ->
                    if (value.isNotEmpty()) {
                        channelNames.putIfAbsent(id, value)
                        allChannelNames.getOrPut(id) { mutableSetOf() }.add(value)
                    }
                }
                "title" -> if (programmeId != null && value.isNotEmpty()) title = value
                "desc" -> if (programmeId != null) description = value
                "channel" -> channelId = null
                "programme" -> {
                    val id = programmeId
                    if (id != null && startMillis > 0L && endMillis >= cutoffMillis) {
                        programmes.getOrPut(id) { mutableListOf() }.add(
                            Programme(id, startMillis, endMillis, title, description)
                        )
                    }
                    programmeId = null
                }
            }
            if (qName == activeTextTag) activeTextTag = null
        }
    }

    private fun parseXmlTvTime(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        val trimmed = value.trim()
        val fastResult = fastParseXmlTvTime(trimmed)
        if (fastResult != null) return fastResult

        return runCatching {
            val match = xmlTvTimeRegex.find(trimmed) ?: return null
            val rawDigits = match.groupValues[1].padEnd(14, '0')
            val rawOffset = match.groupValues.getOrNull(2)
            val offset = when {
                rawOffset.isNullOrEmpty() -> "+0000"
                rawOffset.equals("Z", ignoreCase = true) -> "+0000"
                rawOffset.equals("UTC", ignoreCase = true) -> "+0000"
                rawOffset.equals("GMT", ignoreCase = true) -> "+0000"
                else -> {
                    val cleaned = rawOffset.replace(":", "")
                    if (!cleaned.startsWith("+") && !cleaned.startsWith("-")) "+$cleaned" else cleaned
                }
            }
            dateFormatTL.get()!!.parse("$rawDigits $offset")?.time
        }.getOrNull()
    }

    private fun fastParseXmlTvTime(s: String): Long? {
        if (s.length < 8) return null
        var digitEnd = 0
        while (digitEnd < s.length && s[digitEnd] in '0'..'9') {
            digitEnd++
        }
        if (digitEnd < 8 || digitEnd > 14) return null

        val year = s.substring(0, 4).toIntOrNull() ?: return null
        val month = s.substring(4, 6).toIntOrNull() ?: return null
        val day = s.substring(6, 8).toIntOrNull() ?: return null
        if (month !in 1..12 || day !in 1..31) return null

        val hour = if (digitEnd >= 10) s.substring(8, 10).toIntOrNull() ?: return null else 0
        val min = if (digitEnd >= 12) s.substring(10, 12).toIntOrNull() ?: return null else 0
        val sec = if (digitEnd >= 14) s.substring(12, 14).toIntOrNull() ?: return null else 0
        if (hour !in 0..23 || min !in 0..59 || sec !in 0..59) return null

        var pos = digitEnd
        while (pos < s.length && s[pos].isWhitespace()) pos++

        val offsetMillis: Long
        if (pos >= s.length) {
            offsetMillis = 0L
        } else {
            val remainder = s.substring(pos)
            if (remainder.equals("Z", ignoreCase = true) ||
                remainder.equals("UTC", ignoreCase = true) ||
                remainder.equals("GMT", ignoreCase = true)) {
                offsetMillis = 0L
            } else if (remainder.startsWith("+") || remainder.startsWith("-")) {
                val sign = if (remainder.startsWith("+")) 1 else -1
                val offsetDigits = remainder.substring(1).replace(":", "").trim()
                if (offsetDigits.length < 2) return null
                val offHours = offsetDigits.substring(0, 2).toIntOrNull() ?: return null
                val offMins = if (offsetDigits.length >= 4) offsetDigits.substring(2, 4).toIntOrNull() ?: return null else 0
                offsetMillis = sign * (offHours * 3600L + offMins * 60L) * 1000L
            } else {
                return null
            }
        }

        val utcEpochMillis = epochMillisFromUtc(year, month, day, hour, min, sec)
        return utcEpochMillis - offsetMillis
    }

    private fun epochMillisFromUtc(year: Int, month: Int, day: Int, hour: Int, min: Int, sec: Int): Long {
        var y = year.toLong()
        var m = month.toLong()
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val doy = (153 * (m - 3) + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        val days = era * 146097 + doe - 719468
        return (days * 86400L + hour * 3600L + min * 60L + sec) * 1000L
    }
}

private fun cleanChannelName(value: String): String =
    value.replace(Regex("^(?:ch\\s*)?\\d+\\s*[·\\.\\-\\:\\|\\s]\\s*", RegexOption.IGNORE_CASE), "").trim()

private fun normalize(value: String) =
    value.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
