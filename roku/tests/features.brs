sub Main()
    m.failed = 0
    Assert(EventSeconds("1793131200123") = 1793131200, "event millisecond timestamps keep second precision")
    Assert(EventSeconds("") = 0, "missing event time is safe")
    Assert(RadioFormat("https://radio.example/live.m3u8?token=x") = "hls", "HLS radio format")
    Assert(RadioFormat("https://radio.example/live.aac") = "es.aac-adts", "AAC radio format")
    Assert(RadioFormat("https://radio.example/live.m4a") = "mp4", "M4A radio format")
    Assert(RadioFormat("https://radio.example/live") = "mp3", "MP3 radio default")
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
    Assert(CurrentProgramme(summary, 350) = invalid, "expired programme is not shown as current")
    Assert(CurrentProgramme(invalid, 250) = invalid, "missing guide is safe")
    q = Chr(34)
    catalog = ParseCatalog("#EXTM3U" + Chr(10) + "#EXTINF:-1 tvg-id=" + q + "event.one" + q + " x-event-title=" + q + "Team A vs Team B" + q + " x-event-league=" + q + "MLB" + q + " x-event-start=" + q + "1793131200000" + q + " x-event-end=" + q + "1793141200000" + q + ",SPORTS PPV" + Chr(10) + "https://example/live.m3u8", {})
    Assert(catalog.channels[0].id = "event.one", "EPG uses playlist tvg-id")
    Assert(catalog.channels[0].eventTitle = "Team A vs Team B" and catalog.channels[0].league = "MLB", "PPV event metadata carries through parser")
    Assert(catalog.channels[0].eventStart = 1793131200, "PPV event start time")
    if m.failed > 0 then print "FAILURES: "; m.failed else print "ALL FEATURE TESTS PASSED"
end sub
sub Assert(condition as Boolean, label as String)
    if condition then print "PASS: "; label else m.failed++ : print "FAIL: "; label
end sub
