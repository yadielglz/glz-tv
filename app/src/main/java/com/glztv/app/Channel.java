package com.glztv.app;

import androidx.compose.runtime.Immutable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

@Immutable
public final class Channel {
    public final String id;
    public final String name;
    public final String group;
    public final String number;
    public final String logoUrl;
    public final String streamUrl;
    public final Map<String, String> headers;
    public final String monogram;
    public final String eventTitle;
    public final String eventLeague;
    public final long eventStartMillis;
    public final long eventEndMillis;

    public Channel(String id, String name, String group, String number, String logoUrl,
                   String streamUrl,
                   Map<String, String> headers) {
        this(id, name, group, number, logoUrl, streamUrl, headers, "", "", 0L, 0L);
    }

    public Channel(String id, String name, String group, String number, String logoUrl,
                   String streamUrl,
                   Map<String, String> headers,
                   String eventTitle, String eventLeague,
                   long eventStartMillis, long eventEndMillis) {
        this.id = id != null ? id : "";
        this.name = name != null ? name : "";
        this.group = group != null ? group : "";
        this.number = number != null ? number : "";
        this.logoUrl = logoUrl != null ? logoUrl : "";
        this.streamUrl = streamUrl != null ? streamUrl : "";
        this.headers = headers != null ? Collections.unmodifiableMap(headers) : Collections.emptyMap();
        this.monogram = computeMonogram(this.name);
        this.eventTitle = eventTitle != null ? eventTitle.trim() : "";
        this.eventLeague = eventLeague != null ? eventLeague.trim() : "";
        this.eventStartMillis = Math.max(0L, eventStartMillis);
        this.eventEndMillis = Math.max(0L, eventEndMillis);
    }

    public boolean isLiveEvent() {
        if (!eventTitle.isEmpty()) return true;
        String groupUpper = group.toUpperCase();
        if (groupUpper.contains("EVENTS") || groupUpper.contains("PPV")) return true;
        return number.startsWith("31-") || number.startsWith("30-") || number.startsWith("29-");
    }

    private static String computeMonogram(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "TV";
        }
        String[] words = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                if (Character.isLetterOrDigit(c)) {
                    sb.append(Character.toUpperCase(c));
                    break;
                }
            }
            if (sb.length() >= 2) break;
        }
        if (sb.length() == 1 && words.length == 1 && words[0].length() > 1) {
            for (int i = 1; i < words[0].length(); i++) {
                char c = words[0].charAt(i);
                if (Character.isLetterOrDigit(c)) {
                    sb.append(Character.toUpperCase(c));
                    break;
                }
            }
        }
        return sb.length() > 0 ? sb.toString() : "TV";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Channel channel = (Channel) o;
        return eventStartMillis == channel.eventStartMillis &&
               eventEndMillis == channel.eventEndMillis &&
               Objects.equals(id, channel.id) &&
               Objects.equals(name, channel.name) &&
               Objects.equals(group, channel.group) &&
               Objects.equals(number, channel.number) &&
               Objects.equals(logoUrl, channel.logoUrl) &&
               Objects.equals(streamUrl, channel.streamUrl) &&
               Objects.equals(eventTitle, channel.eventTitle) &&
               Objects.equals(eventLeague, channel.eventLeague);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, group, number, logoUrl, streamUrl, eventTitle, eventLeague, eventStartMillis, eventEndMillis);
    }
}

