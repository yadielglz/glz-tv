sub Main()
    m.failed = 0
    Assert(EventSeconds("1793131200123") = 1793131200, "event millisecond timestamps keep second precision")
    Assert(EventSeconds("") = 0, "missing event time is safe")
    Assert(RadioFormat("https://radio.example/live.m3u8?token=x") = "hls", "HLS radio format")
    Assert(RadioFormat("https://radio.example/live.aac") = "es.aac-adts", "AAC radio format")
    Assert(RadioFormat("https://liveaudio.lamusica.com/PR_WMEG_icy") = "es.aac-adts", "LaMusica ICY AAC+ radio format")
    Assert(RadioFormat("https://televicentro.streamguys1.com/wkaqfm-icy?key=abc") = "es.aac-adts", "StreamGuys ICY AAC+ radio format")
    Assert(RadioFormat("https://server20.servistreaming.com:9023/stream?type=.mp3&_=1") = "es.aac-adts", "ServiStreaming port 9023 AAC+ radio format")
    Assert(RadioFormat("https://radio.example/live.m4a") = "mp4", "M4A radio format")
    Assert(RadioFormat("https://radio.example/live") = "mp3", "MP3 radio default")
    Assert(RadioFormat("https://radio.example/stream?type=.mp3") = "mp3", "MP3 query hint radio format")
    Assert(RadioFormat("https://radio.example/stream?format=aac") = "es.aac-adts", "AAC query hint radio format")
    Assert(RadioFormat("https://radio.example/stream?format=hls") = "hls", "HLS query hint radio format")
    Assert(RadioFormat("https://radio.example/listen?mp=/stream") = "mp3", "Mountpoint query hint radio format")
    Assert(RadioFormat("https://radio.example/listen?mp=/stream.aac") = "es.aac-adts", "AAC mountpoint query hint radio format")
    fbMp3 = RadioFormatFallbacks("https://radio.example/live")
    Assert(fbMp3.Count() = 4 and fbMp3[0] = "mp3" and fbMp3[1] = "es.aac-adts" and fbMp3[2] = "aac" and fbMp3[3] = "hls", "RadioFormatFallbacks puts primary mp3 first with unique fallbacks")
    fbAac = RadioFormatFallbacks("https://radio.example/stream?format=aac")
    Assert(fbAac.Count() = 4 and fbAac[0] = "es.aac-adts" and fbAac[1] = "aac" and fbAac[2] = "mp3" and fbAac[3] = "hls", "RadioFormatFallbacks puts primary es.aac-adts first without duplicates")
    fbHls = RadioFormatFallbacks("https://radio.example/live.m3u8")
    Assert(fbHls.Count() = 4 and fbHls[0] = "hls" and fbHls[1] = "es.aac-adts" and fbHls[2] = "aac" and fbHls[3] = "mp3", "RadioFormatFallbacks puts primary hls first without duplicates")
    Assert(WeatherCondition(95) = "Thunderstorms", "weather conditions decode WMO codes")
    Assert(WeatherCondition(invalid) = "Conditions unavailable", "missing weather code does not crash UI")
    Assert(WeatherNumber(invalid, "°F") = "--°F", "missing temperatures stay unknown")
    Assert(WeatherNumber(80.6, "°F") = "81°F", "temperature rounding")
    forecast = {place: "San Juan, Puerto Rico", current: {temperature_2m: 81, apparent_temperature: 85, relative_humidity_2m: 70, wind_speed_10m: 5, weather_code: 3}, daily: {time: ["2026-10-27"], temperature_2m_max: [86], temperature_2m_min: [75], precipitation_probability_max: [60], weather_code: [61]}}
    lines = WeatherLines(forecast)
    Assert(Instr(1, lines, "San Juan, Puerto Rico") > 0, "weather displays resolved location")
    Assert(Instr(1, lines, "2026-10-27") > 0 and Instr(1, lines, "60%") > 0, "daily forecast and rain probability")
    Assert(Instr(1, lines, "Open-Meteo.com") > 0, "weather attribution")
    summary = {"now": {title: "Old", start: 100, end: 200}, "next": {title: "Current", start: 200, end: 300}}
    Assert(CurrentProgramme(summary, 250).title = "Current", "now/next advances locally before next server refresh")
    Assert(NextProgramme(summary, 250) = invalid, "promoted next programme is not duplicated as upcoming")
    Assert(NextProgramme(summary, 150).title = "Current", "future next programme is returned while now is active")
    Assert(CurrentProgramme(summary, 350) = invalid, "expired programme is not shown as current")
    Assert(CurrentProgramme(invalid, 250) = invalid, "missing guide is safe")
    summaries = {"cnn.us": summary}
    Assert(ChannelSummary(summaries, "CNN.US") <> invalid, "ChannelSummary resolves mixed-case tvg-id across SceneGraph boundary")
    Assert(ChannelSummary(summaries, "missing") = invalid, "ChannelSummary returns invalid when channel is absent")
    lineup = [{url: "https://a.example/1.m3u8"}, {url: "https://a.example/2.m3u8"}, {url: "https://a.example/3.m3u8"}]
    Assert(AdjacentChannelIndex(lineup, "https://a.example/1.m3u8", -1) = 2, "channel up wraps to end of lineup")
    Assert(AdjacentChannelIndex(lineup, "https://a.example/3.m3u8", 1) = 0, "channel down wraps to start of lineup")
    osd = OsdDetails({id: "CNN.US", number: "12", title: "CNN", group: "News", eventTitle: ""}, summaries, "", 150)
    Assert(osd.number = "CH 12" and osd.title = "CNN", "OSD formats channel number and title")
    Assert(Instr(1, osd.nowLine, "Old") > 0 and Instr(1, osd.nextLine, "Current") > 0, "OSD populates Now and Next programme lines")
    eventOsd = OsdDetails({id: "e1", number: "30-01", title: "SPORTS PPV", eventTitle: "Yankees vs Red Sox", league: "MLB", eventStart: 100, eventEnd: 300}, {}, "", 150)
    Assert(Instr(1, eventOsd.nowLine, "Yankees vs Red Sox") > 0 and Instr(1, eventOsd.nextLine, "MLB") > 0, "OSD formats PPV event details")
    movieOsd = OsdDetails({id: "movie:m1", number: "2024", title: "Dune", group: "2024 · 166 min · Sci-Fi", description: "Arrakis story", isMovie: true}, {}, "", 150)
    Assert(movieOsd.number = "2024" and Instr(1, movieOsd.nowLine, "166 min") > 0 and movieOsd.nextLine = "Arrakis story", "OSD formats Movie metadata and synopsis")
    welcome = GuestWelcomeSummary({guestName: "Marcos", guestExperience: {propertyName: "Condado Suite", welcomeMessage: "Enjoy your stay", roomNumber: "402", wifiName: "GLZ-Guest"}}, forecast)
    Assert(Instr(1, welcome.headline, "Condado Suite") > 0 and Instr(1, welcome.headline, "Marcos") > 0, "Home welcome card includes property and guest name")
    Assert(Instr(1, welcome.body, "Room 402") > 0 and Instr(1, welcome.body, "GLZ-Guest") > 0 and Instr(1, welcome.weatherPill, "81°F") > 0, "Home welcome card formats room, Wi-Fi and weather pill")
    q = Chr(34)
    catalog = ParseCatalog("#EXTM3U" + Chr(10) + "#EXTINF:-1 tvg-id=" + q + "event.one" + q + " x-event-title=" + q + "Team A vs Team B" + q + " x-event-league=" + q + "MLB" + q + " x-event-start=" + q + "1793131200000" + q + " x-event-end=" + q + "1793141200000" + q + ",SPORTS PPV" + Chr(10) + "https://example/live.m3u8", {})
    Assert(catalog.channels[0].id = "event.one", "EPG uses playlist tvg-id")
    Assert(catalog.channels[0].eventTitle = "Team A vs Team B" and catalog.channels[0].league = "MLB", "PPV event metadata carries through parser")
    Assert(catalog.channels[0].eventStart = 1793131200, "PPV event start time")

    ' Favorites & Recent Channels persistence and lookup helpers
    Assert(ChannelKey({id: "CNN.US", url: "https://a.example/cnn.m3u8", title: "CNN"}) = "cnn.us", "ChannelKey normalizes tvg-id to lowercase")
    Assert(ChannelKey({id: "", url: "https://a.example/2.m3u8", title: "Ch 2"}) = "https://a.example/2.m3u8", "ChannelKey falls back to url when id is empty")
    Assert(ChannelKey(invalid) = "", "ChannelKey handles invalid safely")

    favs = []
    favs = ToggleFavoriteChannel(favs, "CNN.US")
    favs = ToggleFavoriteChannel(favs, "espn.us")
    Assert(favs.Count() = 2 and IsFavoriteChannel(favs, "cnn.us") and IsFavoriteChannel(favs, "ESPN.US"), "ToggleFavoriteChannel adds and IsFavoriteChannel matches case-insensitively")
    favs = ToggleFavoriteChannel(favs, "cnn.us")
    Assert(favs.Count() = 1 and not IsFavoriteChannel(favs, "CNN.US") and IsFavoriteChannel(favs, "espn.us"), "ToggleFavoriteChannel removes existing favorite on second toggle")
    favs = ToggleFavoriteChannel(favs, "   ")
    Assert(favs.Count() = 1, "ToggleFavoriteChannel ignores blank keys")

    serialized = SerializeIdList(["CNN.US", "espn.us", "cnn.us", ""])
    reparsed = ParseIdList(serialized)
    Assert(reparsed.Count() = 2 and reparsed[0] = "cnn.us" and reparsed[1] = "espn.us", "SerializeIdList and ParseIdList round-trip JSON arrays with deduplication")
    csvParsed = ParseIdList("Ch1, ch2 , CH1, ")
    Assert(csvParsed.Count() = 2 and csvParsed[0] = "ch1" and csvParsed[1] = "ch2", "ParseIdList also supports comma-separated lists")
    Assert(ParseIdList("").Count() = 0 and ParseIdList(invalid).Count() = 0, "ParseIdList handles empty and invalid inputs safely")

    recents = []
    for i = 1 to 12
        recents = RecordRecentChannel(recents, "ch." + i.ToStr(), 10)
    end for
    Assert(recents.Count() = 10 and recents[0] = "ch.12" and recents[9] = "ch.3", "RecordRecentChannel caps at max 10 items with most recent first")
    recents = RecordRecentChannel(recents, "CH.5", 10)
    Assert(recents.Count() = 10 and recents[0] = "ch.5" and recents[1] = "ch.12", "RecordRecentChannel deduplicates existing channel by moving it to front")
    recents = RecordRecentChannel(recents, "", 10)
    Assert(recents.Count() = 10 and recents[0] = "ch.5", "RecordRecentChannel ignores blank keys")

    sampleChannels = [
        {id: "ch.1", url: "https://a.example/1.m3u8", title: "Channel 1"}
        {id: "ch.2", url: "https://a.example/2.m3u8", title: "Channel 2"}
        {id: "ch.3", url: "https://a.example/3.m3u8", title: "Channel 3"}
        {id: "movie:m1", url: "https://a.example/m1.mp4", title: "Movie 1", isMovie: true}
    ]
    resolved = ResolveChannelsByKeys(sampleChannels, ["ch.3", "missing", "CH.1", "movie:m1", "ch.2"], 2)
    Assert(resolved.Count() = 2 and resolved[0].id = "ch.3" and resolved[1].id = "ch.1", "ResolveChannelsByKeys preserves key order, skips missing/movies, and honors max cap")

    ' First-Run Welcome Flow helpers
    Assert(ShouldShowFirstRun("", "") = true, "ShouldShowFirstRun launches when first_run_done is empty and unpaired")
    Assert(ShouldShowFirstRun(invalid, invalid) = true, "ShouldShowFirstRun handles invalid values safely")
    Assert(ShouldShowFirstRun("1", "") = false, "ShouldShowFirstRun skips when first_run_done is 1")
    Assert(ShouldShowFirstRun("", "tok-abc123") = false, "ShouldShowFirstRun skips existing paired devices")
    Assert(ShouldShowFirstRun("1", "tok-abc123") = false, "ShouldShowFirstRun skips when both done and paired")

    pairLoading = FirstRunPairingStatus("", "", false, "")
    Assert(pairLoading.codeText = "Loading…" and Instr(1, pairLoading.statusText, "Requesting") > 0, "FirstRunPairingStatus shows loading before code arrives")
    pairWaiting = FirstRunPairingStatus("GLZ-8421", "", false, "")
    Assert(pairWaiting.codeText = "GLZ-8421" and pairWaiting.statusText = "Waiting for approval on GLZ Hub…", "FirstRunPairingStatus shows live code and auto-polling status")
    pairErr = FirstRunPairingStatus("GLZ-8421", "Network timeout", false, "")
    Assert(pairErr.codeText = "GLZ-8421" and pairErr.statusText = "Network timeout", "FirstRunPairingStatus surfaces error while preserving code")
    pairDone = FirstRunPairingStatus("", "", true, "Living Room Roku")
    Assert(pairDone.codeText = "PAIRED" and Instr(1, pairDone.statusText, "Living Room Roku") > 0, "FirstRunPairingStatus handles already-paired device")

    readySummary = FirstRunReadySummary("Living Room Roku", 42, 8, true)
    Assert(readySummary.deviceLine = "Connected to GLZ Hub · Living Room Roku", "FirstRunReadySummary shows paired device name")
    Assert(Instr(1, readySummary.countsLine, "42 Live Channels") > 0 and Instr(1, readySummary.countsLine, "8 Radio Stations") > 0, "FirstRunReadySummary shows loaded channel and radio counts")
    readySingular = FirstRunReadySummary("", 1, 1, true)
    Assert(readySingular.deviceLine = "Connected to GLZ Hub · Roku TV" and Instr(1, readySingular.countsLine, "1 Live Channel") > 0 and Instr(1, readySingular.countsLine, "1 Radio Station") > 0, "FirstRunReadySummary handles singular counts and default device name")
    readyUnpaired = FirstRunReadySummary("", 0, 0, false)
    Assert(Instr(1, readyUnpaired.deviceLine, "Not yet paired") > 0, "FirstRunReadySummary handles unpaired state")

    ' ProgrammeProgress helper for DirecTV HD progress bars
    Assert(ProgrammeProgress(invalid, 1500) = 0.0, "ProgrammeProgress returns 0 for invalid programme")
    Assert(ProgrammeProgress({ start: 1000, end: 1000 }, 1000) = 0.0, "ProgrammeProgress guards zero duration")
    Assert(ProgrammeProgress({ start: 1000, end: 2000 }, 900) = 0.0, "ProgrammeProgress clamps before start to 0")
    Assert(ProgrammeProgress({ start: 1000, end: 2000 }, 1500) = 0.5, "ProgrammeProgress computes midpoint accurately")
    Assert(ProgrammeProgress({ start: 1000, end: 2000 }, 2500) = 1.0, "ProgrammeProgress clamps after end to 1")

    if m.failed > 0 then print "FAILURES: "; m.failed else print "ALL FEATURE TESTS PASSED"
end sub
sub Assert(condition as Boolean, label as String)
    if condition then print "PASS: "; label else m.failed++ : print "FAIL: "; label
end sub
