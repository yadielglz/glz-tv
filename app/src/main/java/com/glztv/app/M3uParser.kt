package com.glztv.app

import java.net.URI
import java.util.regex.Pattern

object M3uParser {
    private val ATTRIBUTE = Pattern.compile("([\\w-]+)=\"([^\"]*)\"|([\\w-]+)=([^\\s,\"]+)")

    @JvmStatic
    fun parse(
        source: String,
        playlistUrl: String,
        globalHeaders: Map<String, String>
    ): List<Channel> {
        val channels = mutableListOf<Channel>()
        var pendingInfo: String? = null
        var pendingHeaders = mutableMapOf<String, String>()

        source.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach

            if (line.startsWith("#EXTINF:")) {
                pendingInfo = line
                pendingHeaders = globalHeaders.toMutableMap()
                return@forEach
            }
            if (line.startsWith("#EXTVLCOPT:") && pendingInfo != null) {
                addVlcHeader(pendingHeaders, line.substring(11))
                return@forEach
            }
            if (line.startsWith("#KODIPROP:") && pendingInfo != null) {
                addKodiHeader(pendingHeaders, line.substring(10))
                return@forEach
            }
            if (line.startsWith("#") || pendingInfo == null) return@forEach

            val info = pendingInfo ?: return@forEach
            val attributes = parseAttributes(info)
            val comma = info.indexOf(',')
            var name = if (comma >= 0) info.substring(comma + 1).trim() else "Channel"
            if (name.isBlank()) {
                name = attributes["tvg-name"]?.takeIf { it.isNotBlank() } ?: "Channel"
            }
            val id = attributes["tvg-id"]?.takeIf { it.isNotBlank() } ?: name
            val group = attributes["group-title"]?.takeIf { it.isNotBlank() } ?: "Other"
            val number = first(attributes, "tvg-chno", "ch-number", "channel-number", "tvg-num")
            val logoUrl = attributes["tvg-logo"].orEmpty()

            channels.add(
                Channel(
                    id = id,
                    name = name,
                    group = group,
                    number = number,
                    logoUrl = logoUrl,
                    streamUrl = resolveUrl(playlistUrl, line),
                    headers = pendingHeaders.toMap()
                )
            )
            pendingInfo = null
            pendingHeaders = mutableMapOf()
        }
        return channels
    }

    private fun parseAttributes(line: String): Map<String, String> {
        val values = mutableMapOf<String, String>()
        val matcher = ATTRIBUTE.matcher(line)
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                values[matcher.group(1)] = matcher.group(2)
            } else if (matcher.group(3) != null) {
                values[matcher.group(3)] = matcher.group(4)
            }
        }
        return values
    }

    private fun first(values: Map<String, String>, vararg keys: String): String {
        for (key in keys) {
            val value = values[key]
            if (!value.isNullOrBlank()) return value
        }
        return ""
    }

    private fun resolveUrl(base: String, value: String): String {
        return try {
            URI.create(base).resolve(value).toString()
        } catch (_: Exception) {
            value
        }
    }

    private fun addVlcHeader(headers: MutableMap<String, String>, option: String) {
        val separator = option.indexOf('=')
        if (separator < 1) return
        val key = option.substring(0, separator).trim().lowercase()
        val value = option.substring(separator + 1).trim()
        when (key) {
            "http-user-agent" -> headers["User-Agent"] = value
            "http-referrer" -> headers["Referer"] = value
            "http-origin" -> headers["Origin"] = value
            "http-cookie" -> headers["Cookie"] = value
            "http-authorization" -> headers["Authorization"] = value
        }
    }

    private fun addKodiHeader(headers: MutableMap<String, String>, option: String) {
        val separator = option.indexOf('=')
        if (separator < 1) return
        val key = option.substring(0, separator).trim().lowercase()
        val value = option.substring(separator + 1).trim()
        if (key.endsWith("stream_headers")) {
            for (pair in value.split('&')) {
                val equals = pair.indexOf('=')
                if (equals > 0) {
                    headers[pair.substring(0, equals).trim()] = pair.substring(equals + 1).trim()
                }
            }
        }
    }
}
