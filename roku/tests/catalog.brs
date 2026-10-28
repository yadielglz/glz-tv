sub Main()
    m.failed = 0
    q = Chr(34)
    playlist = "#EXTM3U" + Chr(10)
    playlist += "#EXTINF:-1 tvg-id=" + q + "first" + q + " tvg-chno=" + q + "2" + q + " group-title=" + q + "News, Local" + q + ",Telemundo, PR" + Chr(13) + Chr(10)
    playlist += "https://cdn.example/live.m3u8?token=channel" + Chr(10)
    playlist += "#EXTINF:-1 radio=" + q + "true" + q + ",Radio" + Chr(10) + "https://cdn.example/radio.m3u8" + Chr(10)
    playlist += "#EXTINF:-1 playlist-studio-hidden=" + q + "true" + q + ",Hidden" + Chr(10) + "https://cdn.example/hidden.m3u8" + Chr(10)
    playlist += "#EXTINF:-1 group-title='Sports',Sports" + Chr(10)
    playlist += "#EXTVLCOPT:http-user-agent=GlzPlayer" + Chr(10)
    playlist += "#EXTVLCOPT:http-referrer=https://provider.example/" + Chr(10)
    playlist += "https://cdn.example/sports.m3u8" + Chr(10)
    playlist += "#EXTINF:-1,Invalid" + Chr(10) + "javascript:alert(1)" + Chr(10)
    playlist += "#EXTINF:-1,Unsupported pipe" + Chr(10) + "https://cdn.example/x|token=private" + Chr(10)
    catalog = ParseCatalog(playlist, { "X-Provider": "source" })
    Assert(catalog.channels.Count() = 2, "hidden, radio and invalid URLs are excluded")
    Assert(catalog.channels[0].title = "Telemundo, PR", "commas in titles and attributes are preserved")
    Assert(catalog.channels[0].group = "News, Local", "group is parsed")
    Assert(catalog.channels[0].number = "2", "channel number is parsed")
    Assert(catalog.channels[1].headers["User-Agent"] = "GlzPlayer", "per-channel user agent")
    Assert(catalog.channels[1].headers["Referer"] = "https://provider.example/", "per-channel referrer")
    Assert(catalog.channels[0].headers["User-Agent"] = invalid, "headers do not leak between channels")
    Assert(catalog.channels[1].headers["X-Provider"] = "source", "source headers are retained")
    own = PlaylistHeaders("https://glzhub.glztech.com/api/v1/devices/playlist.m3u", "test-secret", {})
    external = PlaylistHeaders("https://other.example/list.m3u", "test-secret", { "User-Agent": "test" })
    lookalike = PlaylistHeaders("https://glzhub.glztech.com.other.example/api/v1/devices/playlist.m3u", "test-secret", {})
    Assert(own.Authorization = "Bearer test-secret", "managed request authenticates")
    Assert(external.Authorization = invalid and lookalike.Authorization = invalid, "Hub credentials never go to external or lookalike hosts")
    unsafe = CopyHeaders({ "Safe": "yes", "Injected": "a" + Chr(10) + "X-Evil: true" })
    Assert(unsafe.Safe = "yes" and unsafe.Injected = invalid, "header injection is discarded")
    Assert(StreamFormat("https://x.example/file.MPD?x=1") = "dash", "DASH format")
    Assert(StreamFormat("https://x.example/file.mp4") = "mp4", "MP4 format")
    Assert(StreamFormat("https://x.example/live") = "hls", "extensionless streams use HLS")
    large = "#EXTM3U" + Chr(10)
    for i = 0 to 1501
        large += "#EXTINF:-1,Channel " + i.ToStr() + Chr(10) + "https://cdn.example/" + i.ToStr() + ".m3u8" + Chr(10)
    end for
    limited = ParseCatalog(large, {})
    Assert(limited.channels.Count() = 1500 and limited.omitted = 2, "large lists are bounded for older TV memory")
    if m.failed > 0
        print "FAILURES: "; m.failed
    else
        print "ALL CATALOG TESTS PASSED"
    end if
end sub

sub Assert(condition as Boolean, label as String)
    if condition
        print "PASS: "; label
    else
        m.failed++
        print "FAIL: "; label
    end if
end sub
