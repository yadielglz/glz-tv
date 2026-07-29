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
    val channelNames: Map<String, String>
) {
    companion object {
        val Empty = EpgGuide(emptyMap(), emptyMap())
    }

    val programmeCount: Int get() = programmes.values.sumOf(List<Programme>::size)

    fun forChannel(channel: Channel): List<Programme> {
        programmes[channel.id]?.takeIf(List<Programme>::isNotEmpty)?.let { return it }
        val normalizedName = normalize(channel.name)
        val matchedId = channelNames.entries.firstOrNull {
            normalize(it.value) == normalizedName ||
                normalizedName.startsWith(normalize(it.value)) ||
                normalize(it.value).startsWith(normalizedName)
        }?.key
        return matchedId?.let(programmes::get).orEmpty()
    }
}

object EpgParser {
    fun parse(xml: String): EpgGuide {
        val handler = GuideHandler()
        SAXParserFactory.newInstance().apply {
            isNamespaceAware = false
            isValidating = false
        }.newSAXParser().parse(InputSource(StringReader(xml)), handler)

        handler.programmes.values.forEach { it.sortBy(Programme::startMillis) }
        return EpgGuide(handler.programmes, handler.channelNames)
    }

    private class GuideHandler : DefaultHandler() {
        val channelNames = mutableMapOf<String, String>()
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
                "display-name" -> channelId?.let { channelNames[it] = value }
                "title" -> if (programmeId != null && value.isNotEmpty()) title = value
                "desc" -> if (programmeId != null) description = value
                "channel" -> channelId = null
                "programme" -> {
                    val id = programmeId
                    if (id != null && startMillis > 0L) {
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
            val parts = value.trim().split(Regex("\\s+"))
            val local = parts[0].take(14).padEnd(14, '0')
            val offset = parts.getOrNull(1) ?: "+0000"
            SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US).apply {
                isLenient = false
            }.parse("$local $offset")!!.time
        }.getOrNull()
    }
}

private fun normalize(value: String) =
    value.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
