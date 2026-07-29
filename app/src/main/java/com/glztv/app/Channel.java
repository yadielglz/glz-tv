package com.glztv.app;

import java.util.Collections;
import java.util.Map;

public final class Channel {
    public final String id;
    public final String name;
    public final String group;
    public final String number;
    public final String logoUrl;
    public final String streamUrl;
    public final Map<String, String> headers;

    public Channel(String id, String name, String group, String number, String logoUrl,
                   String streamUrl,
                   Map<String, String> headers) {
        this.id = id;
        this.name = name;
        this.group = group;
        this.number = number;
        this.logoUrl = logoUrl;
        this.streamUrl = streamUrl;
        this.headers = Collections.unmodifiableMap(headers);
    }
}
