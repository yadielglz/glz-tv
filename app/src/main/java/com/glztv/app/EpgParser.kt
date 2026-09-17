package com.glztv.app

import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.InputSource

data class Programme(
    val channelId: String,
    val startMillis: Long,
    val endMillis: Long,
    val title: String,
    val description: String
)

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
                if (norm.isNotBlank()) putIfAbsent(norm, id)
                val cleanNorm = normalize(cleanChannelName(name))
                if (cleanNorm.isNotBlank()) putIfAbsent(cleanNorm, id)
            }
            allChannelNames.forEach { (id, names) ->
                names.forEach { name ->
                    val norm = normalize(name)
                    if (norm.isNotBlank()) putIfAbsent(norm, id)
                    val cleanNorm = normalize(cleanChannelName(name))
                    if (cleanNorm.isNotBlank()) putIfAbsent(cleanNorm, id)
                }
            }
        }
    }

    companion object {
        val Empty = EpgGuide(emptyMap(), emptyMap(), emptyMap(), emptyMap())
    }

    val programmeCount: Int get() = programmes.values.sumOf(List<Programme>::size)

    private fun findMatchedChannelId(channel: Channel): String? {
        if (channel.id.isNotBlank() && (programmes.containsKey(channel.id) || channelLogos.containsKey(channel.id) || channelNames.containsKey(channel.id))) {
            return channel.id
        }

        val cleanName = cleanChannelName(channel.name)
        val normClean = normalize(cleanName)
        val normRaw = normalize(channel.name)

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
        val cacheKey = "${channel.id}\u0000${channel.name}"
        return synchronized(programmeMatchCache) {
            programmeMatchCache.getOrPut(cacheKey) {
                programmes[channel.id]?.takeIf(List<Programme>::isNotEmpty)
                    ?: findMatchedChannelId(channel)?.let(programmes::get).orEmpty()
            }
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
        val rawReader = java.io.InputStreamReader(stream, Charsets.UTF_8)
        val sanitizingReader = XmlSanitizingReader(rawReader)
        return parse(InputSource(sanitizingReader), cutoffMillis)
    }

    private class XmlSanitizingReader(private val delegate: java.io.Reader) : java.io.Reader() {
        private val buffer = StringBuilder()
        private var isFirstChar = true

        private fun fillBufferIfNeeded(minChars: Int) {
            while (buffer.length < minChars) {
                val c = delegate.read()
                if (c == -1) break
                val ch = c.toChar()
                if (isFirstChar && ch == '\uFEFF') {
                    isFirstChar = false
                    continue
                }
                isFirstChar = false
                if (isInvalidXmlChar(c)) continue
                buffer.append(ch)
            }
        }

        override fun read(cbuf: CharArray, off: Int, len: Int): Int {
            if (len <= 0) return 0
            var written = 0

            while (written < len) {
                fillBufferIfNeeded(64)
                if (buffer.isEmpty()) break

                val c = buffer[0]
                if (c == '&') {
                    sanitizeAmpersand()
                } else if (c == '<') {
                    sanitizeLessThan()
                }

                cbuf[off + written] = buffer[0]
                buffer.deleteCharAt(0)
                written++
            }

            return if (written > 0) written else -1
        }

        private fun sanitizeAmpersand() {
            val entityEnd = buffer.indexOf(';', 1)
            val isValid = entityEnd in 1..32 && isValidEntityStr(buffer.substring(1, entityEnd))
            if (!isValid) {
                buffer.replace(0, 1, "&amp;")
            }
        }

        private fun sanitizeLessThan() {
            if (buffer.length < 2) return
            val next = buffer[1]
            val isValidStart = next == '/' || next == '!' || next == '?' ||
                    next in 'a'..'z' || next in 'A'..'Z' || next == '_' || next == ':'
            if (!isValidStart) {
                buffer.replace(0, 1, "&lt;")
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
        return runCatching {
            val match = xmlTvTimeRegex.find(value.trim()) ?: return null
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
}

private fun cleanChannelName(value: String): String =
    value.replace(Regex("^(?:ch\\s*)?\\d+\\s*[·\\.\\-\\:\\|\\s]\\s*", RegexOption.IGNORE_CASE), "").trim()

private fun normalize(value: String) =
    value.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
