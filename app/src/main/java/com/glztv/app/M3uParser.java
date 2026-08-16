package com.glztv.app;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class M3uParser {
    private static final Pattern ATTRIBUTE = Pattern.compile("([\\w-]+)=\"([^\"]*)\"|([\\w-]+)=([^\\s,\"]+)");

    private M3uParser() {}

    public static List<Channel> parse(String source, String playlistUrl,
                                      Map<String, String> globalHeaders) {
        List<Channel> channels = new ArrayList<>();
        String pendingInfo = null;
        Map<String, String> pendingHeaders = new HashMap<>();

        for (String raw : source.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("#EXTINF:")) {
                pendingInfo = line;
                pendingHeaders = new HashMap<>(globalHeaders);
                continue;
            }
            if (line.startsWith("#EXTVLCOPT:") && pendingInfo != null) {
                addVlcHeader(pendingHeaders, line.substring(11));
                continue;
            }
            if (line.startsWith("#KODIPROP:") && pendingInfo != null) {
                addKodiHeader(pendingHeaders, line.substring(10));
                continue;
            }
            if (line.startsWith("#") || pendingInfo == null) continue;

            Map<String, String> attributes = attributes(pendingInfo);
            int comma = pendingInfo.indexOf(',');
            String name = comma >= 0 ? pendingInfo.substring(comma + 1).trim() : "Channel";
            if (name.isEmpty()) name = attributes.getOrDefault("tvg-name", "Channel");
            String id = attributes.getOrDefault("tvg-id", name);
            String group = attributes.getOrDefault("group-title", "Other");
            String number = first(attributes, "tvg-chno", "ch-number", "channel-number", "tvg-num");
            String logoUrl = attributes.getOrDefault("tvg-logo", "");

            channels.add(new Channel(id, name, group, number, logoUrl,
                    resolveUrl(playlistUrl, line), new HashMap<>(pendingHeaders)));
            pendingInfo = null;
            pendingHeaders = new HashMap<>();
        }
        return channels;
    }

    private static Map<String, String> attributes(String line) {
        Map<String, String> values = new HashMap<>();
        Matcher matcher = ATTRIBUTE.matcher(line);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                values.put(matcher.group(1), matcher.group(2));
            } else if (matcher.group(3) != null) {
                values.put(matcher.group(3), matcher.group(4));
            }
        }
        return values;
    }

    private static String first(Map<String, String> values, String... keys) {
        for (String key : keys) {
            String value = values.get(key);
            if (value != null && !value.isBlank()) return value;
        }
        return "";
    }

    private static String resolveUrl(String base, String value) {
        try {
            return URI.create(base).resolve(value).toString();
        } catch (Exception ignored) {
            return value;
        }
    }

    private static void addVlcHeader(Map<String, String> headers, String option) {
        int separator = option.indexOf('=');
        if (separator < 1) return;
        String key = option.substring(0, separator).trim().toLowerCase();
        String value = option.substring(separator + 1).trim();
        switch (key) {
            case "http-user-agent" -> headers.put("User-Agent", value);
            case "http-referrer" -> headers.put("Referer", value);
            case "http-origin" -> headers.put("Origin", value);
            case "http-cookie" -> headers.put("Cookie", value);
            case "http-authorization" -> headers.put("Authorization", value);
        }
    }

    private static void addKodiHeader(Map<String, String> headers, String option) {
        int separator = option.indexOf('=');
        if (separator < 1) return;
        String key = option.substring(0, separator).trim().toLowerCase();
        String value = option.substring(separator + 1).trim();
        if (key.endsWith("stream_headers")) {
            for (String pair : value.split("&")) {
                int equals = pair.indexOf('=');
                if (equals > 0) {
                    headers.put(pair.substring(0, equals).trim(), pair.substring(equals + 1).trim());
                }
            }
        }
    }
}
