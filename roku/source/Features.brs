function EventSeconds(value as String) as Integer
    if Len(value) <= 3 then return 0
    return Left(value, Len(value) - 3).ToInt()
end function

function ClockLabel(seconds as Integer) as String
    if seconds <= 0 then return ""
    date = CreateObject("roDateTime")
    date.FromSeconds(seconds)
    date.ToLocalTime()
    hour = date.GetHours()
    suffix = " AM"
    if hour >= 12 then suffix = " PM"
    hour = hour mod 12
    if hour = 0 then hour = 12
    minute = date.GetMinutes().ToStr()
    if Len(minute) = 1 then minute = "0" + minute
    return hour.ToStr() + ":" + minute + suffix
end function

function ProgrammeLine(programme as Dynamic) as String
    if type(programme) <> "roAssociativeArray" then return "No programme information"
    return ClockLabel(programme.start) + " · " + Text(programme.title)
end function

function CurrentProgramme(summary as Dynamic, now as Integer) as Dynamic
    if type(summary) <> "roAssociativeArray" then return invalid
    for each key in ["now", "next"]
        programme = summary[key]
        if type(programme) = "roAssociativeArray"
            if programme.start <= now and programme.end > now then return programme
        end if
    end for
    return invalid
end function

function NextProgramme(summary as Dynamic, now as Integer) as Dynamic
    if type(summary) <> "roAssociativeArray" then return invalid
    upcoming = summary["next"]
    if type(upcoming) = "roAssociativeArray"
        if upcoming.start > now then return upcoming
    end if
    return invalid
end function

function ProgrammeProgress(programme as Dynamic, now as Integer) as Float
    if type(programme) <> "roAssociativeArray" then return 0.0
    startTime = programme.start
    endTime = programme.end
    if (type(startTime) <> "Integer" and type(startTime) <> "roInt") or (type(endTime) <> "Integer" and type(endTime) <> "roInt") then return 0.0
    if endTime <= startTime then return 0.0
    if now <= startTime then return 0.0
    if now >= endTime then return 1.0
    return (now - startTime) * 1.0 / (endTime - startTime)
end function

function ChannelSummary(summaries as Dynamic, id as String) as Dynamic
    if type(summaries) <> "roAssociativeArray" or id = "" then return invalid
    if summaries.DoesExist(id) then return summaries[id]
    lower = LCase(id)
    if summaries.DoesExist(lower) then return summaries[lower]
    for each key in summaries
        if LCase(key) = lower then return summaries[key]
    end for
    return invalid
end function

function AdjacentChannelIndex(channels as Object, currentUrl as String, delta as Integer) as Integer
    if type(channels) <> "roArray" or channels.Count() = 0 then return -1
    current = 0
    for i = 0 to channels.Count() - 1
        if Text(channels[i].url) = currentUrl
            current = i : exit for
        end if
    end for
    target = (current + delta) mod channels.Count()
    if target < 0 then target += channels.Count()
    return target
end function

function OsdDetails(channel as Dynamic, summaries as Dynamic, guideError as String, now as Integer) as Object
    if type(channel) <> "roAssociativeArray"
        return { number: "", title: "Live TV", nowLine: "No programme information", nextLine: "" }
    end if
    title = Text(channel.title, "Live channel")
    if channel.isMovie = true
        badge = "MOVIE"
        if Text(channel.number) <> "" and Text(channel.number) <> "VOD" then badge = Text(channel.number)
        nowLine = Text(channel.group, "Movie · On Demand")
        nextLine = Text(channel.description, "Streaming on demand from GlzHub")
        return { number: badge, title: title, nowLine: nowLine, nextLine: nextLine }
    end if
    number = Text(channel.number)
    if number <> "" then number = "CH " + number
    if Text(channel.eventTitle) <> ""
        nowLine = "Live Event: " + Text(channel.eventTitle)
        nextLine = Text(channel.league, "SPORTS")
        window = ClockLabel(channel.eventStart)
        if window <> "" then nextLine += " · " + window + " – " + ClockLabel(channel.eventEnd)
        return { number: number, title: title, nowLine: nowLine, nextLine: nextLine }
    end if
    summary = ChannelSummary(summaries, Text(channel.id))
    current = CurrentProgramme(summary, now)
    upcoming = NextProgramme(summary, now)
    nowLine = "Now: " + ProgrammeLine(current)
    if current = invalid and guideError <> "" then nowLine = guideError
    nextLine = Text(channel.group)
    if upcoming <> invalid then nextLine = "Next: " + ProgrammeLine(upcoming)
    return { number: number, title: title, nowLine: nowLine, nextLine: nextLine }
end function

function GuestWelcomeSummary(config as Dynamic, weather as Dynamic) as Object
    headline = "Welcome to Glz TV"
    body = "Select a destination on the left or choose a quick action to start watching."
    logo = ""
    heroImage = ""
    locationLabel = ""
    if type(config) = "roAssociativeArray"
        locationLabel = Text(config.weatherLocation).Trim()
        guestName = Text(config.guestName).Trim()
        ge = config.guestExperience
        if type(ge) = "roAssociativeArray"
            prop = Text(ge.propertyName).Trim()
            if prop <> "" then headline = prop
            if guestName <> "" then headline += " · Welcome, " + guestName
            welcome = Text(ge.welcomeMessage).Trim()
            if welcome <> "" then body = welcome
            details = []
            if Text(ge.roomNumber).Trim() <> "" then details.Push("Room " + Text(ge.roomNumber).Trim())
            if Text(ge.wifiName).Trim() <> ""
                wifi = "Wi-Fi: " + Text(ge.wifiName).Trim()
                if Text(ge.wifiInstructions).Trim() <> "" then wifi += " (" + Text(ge.wifiInstructions).Trim() + ")"
                details.Push(wifi)
            end if
            if Text(ge.frontDesk).Trim() <> "" then details.Push("Front Desk: " + Text(ge.frontDesk).Trim())
            if Text(ge.checkoutTime).Trim() <> "" then details.Push("Checkout: " + Text(ge.checkoutTime).Trim())
            if details.Count() > 0 then body += Chr(10) + Chr(10) + details.Join("  •  ")
            if Text(ge.noticeTitle).Trim() <> "" or Text(ge.noticeBody).Trim() <> ""
                body += Chr(10) + Chr(10) + Text(ge.noticeTitle, "Notice") + ": " + Text(ge.noticeBody)
            end if
            logo = Text(ge.logoUrl).Trim()
            heroImage = Text(ge.heroImageUrl).Trim()
        end if
    end if
    if type(weather) = "roAssociativeArray" and Text(weather.place) <> ""
        city = Text(weather.place).Split(",")[0].Trim()
        if city <> "" then locationLabel = city
    end if
    if locationLabel = "" then locationLabel = "San Juan"
    weatherPill = ""
    if type(weather) = "roAssociativeArray" and type(weather.current) = "roAssociativeArray"
        weatherPill = WeatherNumber(weather.current.temperature_2m, "°F") + " · " + locationLabel
    end if
    return { headline: headline, body: body, logo: logo, heroImage: heroImage, weatherPill: weatherPill }
end function

function RadioFormat(url as String) as String
    lower = LCase(url.Trim())
    parts = lower.Split("?")
    path = parts[0]
    query = ""
    if parts.Count() > 1 then query = parts[1]
    if Instr(1, path, ".m3u8") > 0 then return "hls"
    if Right(path, 4) = ".aac" or Right(path, 5) = ".aacp" or Instr(1, path, "_icy") > 0 or Instr(1, path, "-icy") > 0 or Instr(1, path, "lamusica.com/") > 0 or Instr(1, path, "server20.servistreaming.com:9022") > 0 or Instr(1, path, "server20.servistreaming.com:9023") > 0 then return "es.aac-adts"
    if Right(path, 4) = ".m4a" or Right(path, 4) = ".mp4" then return "mp4"
    if Right(path, 4) = ".mp3" then return "mp3"
    if query <> ""
        if Instr(1, query, ".m3u8") > 0 or Instr(1, query, "format=hls") > 0 or Instr(1, query, "type=hls") > 0 then return "hls"
        if Instr(1, query, "format=aac") > 0 or Instr(1, query, "type=aac") > 0 or Instr(1, query, "type=.aac") > 0 or Instr(1, query, "codec=aac") > 0 or Instr(1, query, ".aac") > 0 then return "es.aac-adts"
        if Instr(1, query, "format=mp4") > 0 or Instr(1, query, "format=m4a") > 0 or Instr(1, query, ".m4a") > 0 then return "mp4"
        if Instr(1, query, "type=.mp3") > 0 or Instr(1, query, "type=mp3") > 0 or Instr(1, query, "format=mp3") > 0 or Instr(1, query, ".mp3") > 0 then return "mp3"
        if Instr(1, query, "mp=/stream") > 0 then return "mp3"
    end if
    return "mp3"
end function

function RadioFormatFallbacks(url as String) as Object
    primary = RadioFormat(url)
    candidates = [primary, "es.aac-adts", "aac", "mp3", "hls"]
    unique = []
    seen = {}
    for each fmt in candidates
        if fmt <> "" and not seen.DoesExist(fmt)
            seen[fmt] = true
            unique.Push(fmt)
        end if
    end for
    return unique
end function

function WeatherCondition(code as Dynamic) as String
    if code = invalid then return "Conditions unavailable"
    if code = 0 then return "Clear sky"
    if code <= 3 then return "Partly cloudy / cloudy"
    if code = 45 or code = 48 then return "Fog"
    if code >= 51 and code <= 57 then return "Drizzle"
    if code >= 61 and code <= 67 then return "Rain"
    if code >= 71 and code <= 77 then return "Snow"
    if code >= 80 and code <= 82 then return "Rain showers"
    if code = 85 or code = 86 then return "Snow showers"
    if code >= 95 then return "Thunderstorms"
    return "Conditions unavailable"
end function

function WeatherNumber(value as Dynamic, unit as String) as String
    if value = invalid then return "--" + unit
    return Int(value + 0.5).ToStr() + unit
end function

function WeatherLines(data as Dynamic) as String
    if type(data) <> "roAssociativeArray" then return "Loading weather…"
    current = data.current
    if type(current) <> "roAssociativeArray" then return "Weather unavailable. Check the location in GlzHub."
    output = Text(data.place) + Chr(10) + Chr(10)
    output += WeatherNumber(current.temperature_2m, "°F") + "   " + WeatherCondition(current.weather_code) + Chr(10)
    output += "Feels like " + WeatherNumber(current.apparent_temperature, "°F") + "   Humidity " + WeatherNumber(current.relative_humidity_2m, "%") + "   Wind " + WeatherNumber(current.wind_speed_10m, " mph") + Chr(10) + Chr(10)
    daily = data.daily
    if type(daily) = "roAssociativeArray"
        if type(daily.time) = "roArray"
            for i = 0 to daily.time.Count() - 1
                if i >= 5 then exit for
                output += Text(daily.time[i]) + "   " + WeatherNumber(daily.temperature_2m_max[i], "°") + " / " + WeatherNumber(daily.temperature_2m_min[i], "°")
                output += "   Rain " + WeatherNumber(daily.precipitation_probability_max[i], "%") + "   " + WeatherCondition(daily.weather_code[i]) + Chr(10)
            end for
        end if
    end if
    return output + Chr(10) + "Forecast: Open-Meteo.com · Times and dates follow the forecast location."
end function

function ChannelKey(channel as Dynamic) as String
    if type(channel) <> "roAssociativeArray" then return ""
    id = Text(channel.id).Trim()
    if id <> "" then return LCase(id)
    url = Text(channel.url).Trim()
    if url <> "" then return LCase(url)
    return LCase(Text(channel.title).Trim())
end function

function ParseIdList(raw as Dynamic) as Object
    result = []
    seen = {}
    source = Text(raw).Trim()
    if source = "" then return result
    items = invalid
    if Left(source, 1) = "["
        parsed = ParseJson(source)
        if type(parsed) = "roArray" then items = parsed
    end if
    if items = invalid then items = source.Split(",")
    for each entry in items
        key = LCase(Text(entry).Trim())
        if key <> "" and not seen.DoesExist(key)
            seen[key] = true
            result.Push(key)
        end if
    end for
    return result
end function

function SerializeIdList(list as Dynamic) as String
    clean = []
    seen = {}
    if type(list) = "roArray"
        for each entry in list
            key = LCase(Text(entry).Trim())
            if key <> "" and not seen.DoesExist(key)
                seen[key] = true
                clean.Push(key)
            end if
        end for
    end if
    return FormatJson(clean)
end function

function IsFavoriteChannel(favorites as Dynamic, key as String) as Boolean
    norm = LCase(Text(key).Trim())
    if norm = "" or type(favorites) <> "roArray" then return false
    for each entry in favorites
        if LCase(Text(entry).Trim()) = norm then return true
    end for
    return false
end function

function ToggleFavoriteChannel(favorites as Dynamic, key as String) as Object
    norm = LCase(Text(key).Trim())
    result = []
    seen = {}
    removed = false
    if type(favorites) = "roArray"
        for each entry in favorites
            val = LCase(Text(entry).Trim())
            if val <> "" and not seen.DoesExist(val)
                seen[val] = true
                if val = norm
                    removed = true
                else
                    result.Push(val)
                end if
            end if
        end for
    end if
    if norm <> "" and not removed then result.Push(norm)
    return result
end function

function RecordRecentChannel(recents as Dynamic, key as String, maxItems = 10 as Integer) as Object
    if maxItems <= 0 then maxItems = 10
    norm = LCase(Text(key).Trim())
    result = []
    seen = {}
    if norm <> ""
        result.Push(norm)
        seen[norm] = true
    end if
    if type(recents) = "roArray"
        for each entry in recents
            if result.Count() >= maxItems then exit for
            val = LCase(Text(entry).Trim())
            if val <> "" and not seen.DoesExist(val)
                seen[val] = true
                result.Push(val)
            end if
        end for
    end if
    return result
end function

function ResolveChannelsByKeys(channels as Dynamic, keys as Dynamic, maxItems = 0 as Integer) as Object
    result = []
    if type(channels) <> "roArray" or type(keys) <> "roArray" then return result
    byKey = {}
    for each ch in channels
        if type(ch) = "roAssociativeArray" and ch.isMovie <> true
            k = ChannelKey(ch)
            if k <> "" and not byKey.DoesExist(k) then byKey[k] = ch
            urlKey = LCase(Text(ch.url).Trim())
            if urlKey <> "" and not byKey.DoesExist(urlKey) then byKey[urlKey] = ch
        end if
    end for
    addedUrls = {}
    addedKeys = {}
    for each rawKey in keys
        if maxItems > 0 and result.Count() >= maxItems then exit for
        norm = LCase(Text(rawKey).Trim())
        if norm <> "" and byKey.DoesExist(norm) and not addedKeys.DoesExist(norm)
            ch = byKey[norm]
            urlKey = LCase(Text(ch.url).Trim())
            if urlKey = "" or not addedUrls.DoesExist(urlKey)
                result.Push(ch)
                addedKeys[norm] = true
                if urlKey <> "" then addedUrls[urlKey] = true
            end if
        end if
    end for
    return result
end function

function ShouldShowFirstRun(firstRunDone as Dynamic, token as Dynamic) as Boolean
    if Text(firstRunDone).Trim() = "1" then return false
    if Text(token).Trim() <> "" then return false
    return true
end function

function FirstRunPairingStatus(code as Dynamic, errorMessage as Dynamic, isPaired as Boolean, deviceName = "" as Dynamic) as Object
    if isPaired
        name = Text(deviceName).Trim()
        if name = "" then name = "Roku TV"
        return {
            codeText: "PAIRED"
            statusText: "Already paired with GLZ Hub as " + name + ". Choose Refresh Code to pair again, or Skip to continue."
        }
    end if
    cleanCode = Text(code).Trim()
    cleanErr = Text(errorMessage).Trim()
    if cleanErr <> ""
        if cleanCode = "" then cleanCode = "…"
        return { codeText: cleanCode, statusText: cleanErr }
    end if
    if cleanCode <> ""
        return { codeText: cleanCode, statusText: "Waiting for approval on GLZ Hub…" }
    end if
    return { codeText: "Loading…", statusText: "Requesting pairing code from GLZ Hub…" }
end function

function FirstRunReadySummary(deviceName as Dynamic, channelCount as Integer, radioCount as Integer, isPaired as Boolean) as Object
    if not isPaired
        return {
            deviceLine: "Not yet paired with GLZ Hub"
            countsLine: "You can pair this Roku anytime from Settings."
        }
    end if
    name = Text(deviceName).Trim()
    if name = "" then name = "Roku TV"
    if channelCount < 0 then channelCount = 0
    if radioCount < 0 then radioCount = 0
    chLabel = "Live Channels"
    if channelCount = 1 then chLabel = "Live Channel"
    radioLabel = "Radio Stations"
    if radioCount = 1 then radioLabel = "Radio Station"
    return {
        deviceLine: "Connected to GLZ Hub · " + name
        countsLine: channelCount.ToStr() + " " + chLabel + "  ·  " + radioCount.ToStr() + " " + radioLabel + " loaded"
    }
end function

function OsdButtonOrder() as Object
    return ["Guide", "Info", "CC", "Audio", "Apps", "Multi-View", "Favorite", "Sleep"]
end function

function ChannelSurfDwellSeconds() as Float
    return 7.0
end function

function WrapChannelIndex(currentIndex as Integer, delta as Integer, total as Integer) as Integer
    if total <= 0 then return -1
    if currentIndex < 0 or currentIndex >= total then currentIndex = 0
    target = (currentIndex + delta) mod total
    if target < 0 then target += total
    return target
end function

function NextSleepTimerMinutes(currentMinutes as Integer) as Integer
    if currentMinutes < 15 then return 15
    if currentMinutes < 30 then return 30
    if currentMinutes < 60 then return 60
    if currentMinutes < 90 then return 90
    return 0
end function

function SleepTimerLabel(minutes as Integer) as String
    if minutes <= 0 then return "Sleep"
    return "Sleep " + minutes.ToStr() + "m"
end function

function NormalizeRokuHexColor(raw as Dynamic, fallback as String) as String
    val = Text(raw).Trim()
    if val = "" then return fallback
    if Left(val, 1) = "#" then val = Mid(val, 2)
    if Left(LCase(val), 2) = "0x" then val = Mid(val, 3)
    if Len(val) = 6 then return "0x" + UCase(val) + "FF"
    if Len(val) = 8 then return "0x" + UCase(val)
    return fallback
end function

function DefaultEntertainmentApps() as Object
    return [
        { id: "netflix", name: "Netflix", subtitle: "Movies & Series", badge: "STREAMING", accent: "0xE50914FF", artwork: "", channelId: "12" }
        { id: "youtube", name: "YouTube", subtitle: "Videos & Live", badge: "VIDEO", accent: "0xCC0000FF", artwork: "", channelId: "837" }
        { id: "disney", name: "Disney+", subtitle: "Disney · Pixar · Marvel", badge: "STREAMING", accent: "0x113CCFFF", artwork: "", channelId: "291097" }
        { id: "prime", name: "Prime Video", subtitle: "Amazon Originals", badge: "STREAMING", accent: "0x00A8E1FF", artwork: "", channelId: "13" }
        { id: "max", name: "Max", subtitle: "HBO · Warner Bros.", badge: "STREAMING", accent: "0x002BE7FF", artwork: "", channelId: "61322" }
        { id: "hulu", name: "Hulu", subtitle: "TV & Originals", badge: "STREAMING", accent: "0x1CE783FF", artwork: "", channelId: "2285" }
        { id: "appletv", name: "Apple TV", subtitle: "Apple Originals", badge: "STREAMING", accent: "0x1E293BFF", artwork: "", channelId: "551012" }
        { id: "paramount", name: "Paramount+", subtitle: "Live Sports & Movies", badge: "STREAMING", accent: "0x0064FFFF", artwork: "", channelId: "31440" }
        { id: "peacock", name: "Peacock", subtitle: "NBC · Live Sports", badge: "STREAMING", accent: "0x1E1B4BFF", artwork: "", channelId: "593099" }
        { id: "spotify", name: "Spotify", subtitle: "Music & Podcasts", badge: "MUSIC", accent: "0x1DB954FF", artwork: "", channelId: "22297" }
    ]
end function

function ResolveEntertainmentApps(config as Dynamic) as Object
    defaults = DefaultEntertainmentApps()
    if type(config) <> "roAssociativeArray" then return defaults
    rawApps = config.entertainmentApps
    if type(rawApps) <> "roArray" or rawApps.Count() = 0 then return defaults
    palette = ["0xE50914FF", "0x113CCFFF", "0x00A8E1FF", "0x002BE7FF", "0x1CE783FF", "0x0064FFFF", "0x1DB954FF", "0x7C3AEDFF"]
    resolved = []
    idx = 0
    for each entry in rawApps
        if type(entry) = "roAssociativeArray"
            name = Text(entry.name).Trim()
            if name = "" then name = Text(entry.title).Trim()
            if name <> ""
                artwork = Text(entry.bannerUrl).Trim()
                if not IsWebUrl(artwork) then artwork = Text(entry.artworkUrl).Trim()
                if not IsWebUrl(artwork) then artwork = Text(entry.posterUrl).Trim()
                if not IsWebUrl(artwork) then artwork = Text(entry.logoUrl).Trim()
                if not IsWebUrl(artwork) then artwork = Text(entry.iconUrl).Trim()
                if not IsWebUrl(artwork) then artwork = ""
                subtitle = Text(entry.subtitle).Trim()
                if subtitle = "" then subtitle = Text(entry.category, "Entertainment App").Trim()
                badge = UCase(Text(entry.badge, "APP").Trim())
                fallbackAccent = palette[idx mod palette.Count()]
                accent = NormalizeRokuHexColor(entry.accentColor, NormalizeRokuHexColor(entry.accent, NormalizeRokuHexColor(entry.color, fallbackAccent)))
                chId = Text(entry.rokuChannelId).Trim()
                if chId = "" then chId = Text(entry.channelId).Trim()
                resolved.Push({
                    id: LCase(Text(entry.id, name))
                    name: name
                    subtitle: subtitle
                    badge: badge
                    accent: accent
                    artwork: artwork
                    channelId: chId
                    streamUrl: Text(entry.streamUrl).Trim()
                })
                idx++
            end if
        end if
    end for
    if resolved.Count() = 0 then return defaults
    return resolved
end function

function SportsTeamDisplay(rank as Integer, shortName as String) as String
    if rank >= 1 and rank <= 25 then return "#" + rank.ToStr() + " " + shortName
    return shortName
end function

function ScoreboardRowHeadline(game as Dynamic) as String
    if type(game) <> "roAssociativeArray" then return ""
    awayLabel = SportsTeamDisplay(game.awayRank, Text(game.awayName, Text(game.awayAbbr)))
    homeLabel = SportsTeamDisplay(game.homeRank, Text(game.homeName, Text(game.homeAbbr)))
    if Text(game.state) = "pre"
        return awayLabel + " @ " + homeLabel
    end if
    return awayLabel + " " + Text(game.awayScore, "0") + " – " + homeLabel + " " + Text(game.homeScore, "0")
end function

function ParseEspnScoreboard(rawJson as String, league as String, subLeague as String) as Object
    games = []
    if rawJson = "" then return games
    root = ParseJson(rawJson)
    if type(root) <> "roAssociativeArray" then return games
    events = root.events
    if type(events) <> "roArray" then return games
    for each ev in events
        if type(ev) = "roAssociativeArray" and type(ev.competitions) = "roArray" and ev.competitions.Count() > 0
            comp = ev.competitions[0]
            if type(comp) = "roAssociativeArray" and type(comp.competitors) = "roArray" and comp.competitors.Count() >= 2
                away = invalid
                home = invalid
                for each c in comp.competitors
                    if type(c) = "roAssociativeArray"
                        if Text(c.homeAway) = "away" then away = c
                        if Text(c.homeAway) = "home" then home = c
                    end if
                end for
                if away = invalid then away = comp.competitors[1]
                if home = invalid then home = comp.competitors[0]
                if type(away) = "roAssociativeArray" and type(home) = "roAssociativeArray"
                    stState = "pre"
                    stDetail = ""
                    if type(ev.status) = "roAssociativeArray" and type(ev.status.type) = "roAssociativeArray"
                        stState = LCase(Text(ev.status.type.state, "pre"))
                        stDetail = Text(ev.status.type.shortDetail, Text(ev.status.type.detail))
                    end if
                    broadcasts = []
                    seenNet = {}
                    if type(comp.broadcasts) = "roArray"
                        for each b in comp.broadcasts
                            if type(b) = "roAssociativeArray" and type(b.names) = "roArray"
                                for each bName in b.names
                                    clean = Text(bName).Trim()
                                    if clean <> "" and not seenNet.DoesExist(UCase(clean))
                                        seenNet[UCase(clean)] = true
                                        broadcasts.Push(clean)
                                    end if
                                end for
                            end if
                        end for
                    end if
                    if type(comp.geoBroadcasts) = "roArray"
                        for each gb in comp.geoBroadcasts
                            if type(gb) = "roAssociativeArray" and type(gb.media) = "roAssociativeArray"
                                clean = Text(gb.media.shortName).Trim()
                                if clean <> "" and not seenNet.DoesExist(UCase(clean))
                                    seenNet[UCase(clean)] = true
                                    broadcasts.Push(clean)
                                end if
                            end if
                        end for
                    end if
                    awayTeam = away.team
                    homeTeam = home.team
                    if type(awayTeam) = "roAssociativeArray" and type(homeTeam) = "roAssociativeArray"
                        awayRank = 0
                        if type(away.curatedRank) = "roAssociativeArray" and away.curatedRank.current <> invalid
                            rVal = away.curatedRank.current
                            if rVal >= 1 and rVal <= 25 then awayRank = rVal
                        end if
                        homeRank = 0
                        if type(home.curatedRank) = "roAssociativeArray" and home.curatedRank.current <> invalid
                            rVal = home.curatedRank.current
                            if rVal >= 1 and rVal <= 25 then homeRank = rVal
                        end if
                        primaryB = ""
                        if broadcasts.Count() > 0 then primaryB = broadcasts[0]
                        game = {
                            id: subLeague + ":" + Text(ev.id)
                            league: league
                            subLeague: subLeague
                            state: stState
                            statusDetail: stDetail
                            awayName: Text(awayTeam.shortDisplayName, Text(awayTeam.name, Text(awayTeam.abbreviation)))
                            awayAbbr: Text(awayTeam.abbreviation)
                            awayFull: Text(awayTeam.displayName, Text(awayTeam.location))
                            awayScore: Text(away.score, "0")
                            awayLogo: Text(awayTeam.logo)
                            awayRank: awayRank
                            homeName: Text(homeTeam.shortDisplayName, Text(homeTeam.name, Text(homeTeam.abbreviation)))
                            homeAbbr: Text(homeTeam.abbreviation)
                            homeFull: Text(homeTeam.displayName, Text(homeTeam.location))
                            homeScore: Text(home.score, "0")
                            homeLogo: Text(homeTeam.logo)
                            homeRank: homeRank
                            broadcasts: broadcasts
                            primaryBroadcast: primaryB
                        }
                        game["headline"] = ScoreboardRowHeadline(game)
                        games.Push(game)
                    end if
                end if
            end if
        end if
    end for
    return games
end function

function SportsGamePriority(game as Dynamic) as Integer
    if type(game) <> "roAssociativeArray" then return 0
    score = 0
    st = Text(game.state)
    if st = "in"
        score += 1000
    else if st = "pre"
        score += 500
    else
        score += 100
    end if
    if Text(game.primaryBroadcast) <> "" then score += 50
    if game.awayRank > 0 or game.homeRank > 0 then score += 30
    return score
end function

function SortSportsGames(games as Object) as Object
    if type(games) <> "roArray" then return []
    sorted = []
    for each g in games
        sorted.Push(g)
    end for
    ' Simple insertion sort (small list, deterministic across Roku & brs)
    for i = 1 to sorted.Count() - 1
        cur = sorted[i]
        curP = SportsGamePriority(cur)
        j = i - 1
        while j >= 0 and SportsGamePriority(sorted[j]) < curP
            sorted[j + 1] = sorted[j]
            j--
        end while
        sorted[j + 1] = cur
    end for
    return sorted
end function

function IsLiveEventChannel(channel as Dynamic) as Boolean
    if type(channel) <> "roAssociativeArray" then return false
    if Text(channel.eventTitle).Trim() <> "" then return true
    grp = UCase(Text(channel.group))
    if Instr(1, grp, "EVENT") > 0 or Instr(1, grp, "PPV") > 0 then return true
    num = Text(channel.number)
    if Left(num, 3) = "31-" or Left(num, 3) = "30-" or Left(num, 3) = "29-" then return true
    return false
end function

function IsPlaceholderEventText(rawText as String) as Boolean
    t = UCase(rawText.Trim())
    if t = "" then return true
    for each bad in ["NO EVENT", "OFFLINE", "NO SIGNAL", "COMING SOON", "STANDBY", "TEST FEED", "2098-12-31", "2099-12-31", "24/7", "TBD", "TBA"]
        if Instr(1, t, bad) > 0 then return true
    end for
    for each exact in ["SPORTS PPV", "LIVE EVENT", "LIVE EVENTS", "PPV", "NBA", "NFL", "MLB", "NHL", "ESPN+", "UFC"]
        if t = exact then return true
    end for
    ' Catch numbered placeholders like "NBA 13", "MLB 04", "SPORTS PPV 2"
    for each prefix in ["NBA ", "NFL ", "MLB ", "NHL ", "MLS ", "NCAA ", "ESPN+ ", "PPV ", "EVENT ", "SPORTS PPV "]
        if Left(t, Len(prefix)) = prefix
            rest = Mid(t, Len(prefix) + 1).Trim()
            if rest <> "" and Val(rest) > 0 and Instr(1, rest, "VS") = 0 and Instr(1, rest, "@") = 0 and Instr(1, rest, ":") = 0
                return true
            end if
        end if
    end for
    return false
end function

function IsValidLiveEventChannel(channel as Dynamic, now as Integer) as Boolean
    if not IsLiveEventChannel(channel) then return false
    if Text(channel.url).Trim() = "" then return false
    if channel.eventEnd <> invalid and channel.eventEnd > 0 and channel.eventEnd < now - 1800
        return false
    end if
    evTitle = Text(channel.eventTitle).Trim()
    chTitle = Text(channel.title).Trim()
    if evTitle <> ""
        if IsPlaceholderEventText(evTitle) then return false
        return true
    end if
    if IsPlaceholderEventText(chTitle) then return false
    return true
end function

function SportsWordContains(haystackUpper as String, tokenUpper as String) as Boolean
    if tokenUpper = "" or Len(tokenUpper) < 2 then return false
    hitIdx = Instr(1, haystackUpper, tokenUpper)
    while hitIdx > 0
        beforeOk = (hitIdx = 1)
        if not beforeOk
            bc = Mid(haystackUpper, hitIdx - 1, 1)
            beforeOk = not ((bc >= "A" and bc <= "Z") or (bc >= "0" and bc <= "9"))
        end if
        afterIdx = hitIdx + Len(tokenUpper)
        afterOk = (afterIdx > Len(haystackUpper))
        if not afterOk
            ac = Mid(haystackUpper, afterIdx, 1)
            afterOk = not ((ac >= "A" and ac <= "Z") or (ac >= "0" and ac <= "9"))
        end if
        if beforeOk and afterOk then return true
        hitIdx = Instr(hitIdx + 1, haystackUpper, tokenUpper)
    end while
    return false
end function

function SportsGameTeamsMatch(textUpper as String, game as Dynamic) as Boolean
    if type(game) <> "roAssociativeArray" then return false
    awayHit = SportsWordContains(textUpper, UCase(Text(game.awayName))) or SportsWordContains(textUpper, UCase(Text(game.awayFull)))
    if not awayHit and Len(Text(game.awayAbbr)) >= 3 then awayHit = SportsWordContains(textUpper, UCase(Text(game.awayAbbr)))
    homeHit = SportsWordContains(textUpper, UCase(Text(game.homeName))) or SportsWordContains(textUpper, UCase(Text(game.homeFull)))
    if not homeHit and Len(Text(game.homeAbbr)) >= 3 then homeHit = SportsWordContains(textUpper, UCase(Text(game.homeAbbr)))
    return awayHit and homeHit
end function

function SportsChannelMatchesNetwork(channel as Dynamic, broadcastName as String) as Boolean
    if type(channel) <> "roAssociativeArray" then return false
    b = UCase(broadcastName.Trim())
    if b = "" then return false
    chText = UCase(Text(channel.title) + " " + Text(channel.callSign) + " " + Text(channel.id))
    if b = "ESPN2" or b = "ESPN 2"
        return SportsWordContains(chText, "ESPN2") or SportsWordContains(chText, "ESPN 2")
    else if b = "ESPNU" or b = "ESPN U"
        return SportsWordContains(chText, "ESPNU") or SportsWordContains(chText, "ESPN U")
    else if b = "ESPNEWS" or b = "ESPN NEWS"
        return SportsWordContains(chText, "ESPNEWS") or SportsWordContains(chText, "ESPN NEWS")
    else if b = "ESPN"
        if Instr(1, chText, "ESPN2") > 0 or Instr(1, chText, "ESPN 2") > 0 or Instr(1, chText, "ESPNU") > 0 or Instr(1, chText, "ESPNEWS") > 0 or Instr(1, chText, "ESPN+") > 0 or Instr(1, chText, "DEPORTES") > 0 then return false
        return SportsWordContains(chText, "ESPN")
    else if b = "FS1" or b = "FOX SPORTS 1"
        return SportsWordContains(chText, "FS1") or SportsWordContains(chText, "FOX SPORTS 1")
    else if b = "FS2" or b = "FOX SPORTS 2"
        return SportsWordContains(chText, "FS2") or SportsWordContains(chText, "FOX SPORTS 2")
    else if b = "FOX"
        if Instr(1, chText, "FOX NEWS") > 0 or Instr(1, chText, "FOX BUSINESS") > 0 or Instr(1, chText, "FOX WEATHER") > 0 or Instr(1, chText, "FS1") > 0 or Instr(1, chText, "FS2") > 0 or Instr(1, chText, "FOX SPORTS") > 0 or Instr(1, chText, "DEPORTES") > 0 then return false
        return SportsWordContains(chText, "FOX") or SportsWordContains(chText, "WOFL") or SportsWordContains(chText, "WNYW") or SportsWordContains(chText, "KTTV")
    else if b = "ABC"
        if Instr(1, chText, "ABC NEWS") > 0 or Instr(1, chText, "SPARK") > 0 then return false
        return SportsWordContains(chText, "ABC") or SportsWordContains(chText, "WFTV") or SportsWordContains(chText, "WABC") or SportsWordContains(chText, "KABC")
    else if b = "CBSSN" or b = "CBS SPORTS NETWORK"
        return SportsWordContains(chText, "CBSSN") or SportsWordContains(chText, "CBS SPORTS")
    else if b = "CBS"
        if Instr(1, chText, "CBS NEWS") > 0 or Instr(1, chText, "CBS SPORTS") > 0 or Instr(1, chText, "CBSSN") > 0 then return false
        return SportsWordContains(chText, "CBS") or SportsWordContains(chText, "WKMG") or SportsWordContains(chText, "WCBS") or SportsWordContains(chText, "KCBS")
    else if b = "NBC"
        if Instr(1, chText, "CNBC") > 0 or Instr(1, chText, "MSNBC") > 0 or Instr(1, chText, "NBC NEWS") > 0 or Instr(1, chText, "NBCSN") > 0 then return false
        return SportsWordContains(chText, "NBC") or SportsWordContains(chText, "WESH") or SportsWordContains(chText, "WNBC") or SportsWordContains(chText, "KNBC")
    else if b = "TNT"
        return SportsWordContains(chText, "TNT")
    else if b = "TBS"
        return SportsWordContains(chText, "TBS")
    else if b = "NBA TV" or b = "NBATV"
        return SportsWordContains(chText, "NBA TV") or SportsWordContains(chText, "NBATV")
    else if b = "NFL NETWORK" or b = "NFLN"
        return SportsWordContains(chText, "NFL NETWORK") or SportsWordContains(chText, "NFLN")
    else if b = "MLB NETWORK" or b = "MLBN"
        return SportsWordContains(chText, "MLB NETWORK") or SportsWordContains(chText, "MLBN")
    else if b = "BTN" or b = "BIG TEN NETWORK"
        return SportsWordContains(chText, "BTN") or SportsWordContains(chText, "BIG TEN")
    else if b = "SEC NETWORK" or b = "SECN"
        return SportsWordContains(chText, "SEC NETWORK") or SportsWordContains(chText, "SECN")
    else if b = "ACC NETWORK" or b = "ACCN"
        return SportsWordContains(chText, "ACC NETWORK") or SportsWordContains(chText, "ACCN")
    else if b = "CW" or b = "THE CW"
        return SportsWordContains(chText, "CW")
    end if
    return false
end function

function MatchScoreboardChannel(game as Dynamic, channels as Object, summaries as Dynamic, now as Integer) as Dynamic
    if type(game) <> "roAssociativeArray" or type(channels) <> "roArray" then return invalid
    ' Priority 1: Exact team matchup in live event channel or EPG title
    for each ch in channels
        if Text(ch.url).Trim() <> ""
            if not IsLiveEventChannel(ch) or IsValidLiveEventChannel(ch, now)
                prog = CurrentProgramme(ChannelSummary(summaries, Text(ch.id)), now)
                progTitle = ""
                if type(prog) = "roAssociativeArray" then progTitle = Text(prog.title)
                combined = UCase(Text(ch.eventTitle) + " " + Text(ch.title) + " " + progTitle)
                if SportsGameTeamsMatch(combined, game) then return ch
            end if
        end if
    end for
    ' Priority 2: Linear broadcast channel matching ESPN network + EPG confirmation bonus
    if type(game.broadcasts) <> "roArray" or game.broadcasts.Count() = 0 then return invalid
    bestCh = invalid
    bestScore = -999
    for bIdx = 0 to game.broadcasts.Count() - 1
        net = Text(game.broadcasts[bIdx])
        baseBonus = (10 - bIdx) * 10
        if baseBonus < 10 then baseBonus = 10
        for each ch in channels
            if Text(ch.url).Trim() <> "" and not IsLiveEventChannel(ch)
                if SportsChannelMatchesNetwork(ch, net)
                    score = baseBonus
                    prog = CurrentProgramme(ChannelSummary(summaries, Text(ch.id)), now)
                    if type(prog) = "roAssociativeArray"
                        pUpper = UCase(Text(prog.title))
                        if SportsGameTeamsMatch(pUpper, game) then score += 120
                        if Instr(1, pUpper, "FOOTBALL") > 0 or Instr(1, pUpper, "BASKETBALL") > 0 or Instr(1, pUpper, "BASEBALL") > 0 or Instr(1, pUpper, "NBA") > 0 or Instr(1, pUpper, "NFL") > 0 or Instr(1, pUpper, "MLB") > 0 or Instr(1, pUpper, "BIG NOON") > 0
                            score += 50
                        end if
                        if Text(game.state) = "in" and (Instr(1, pUpper, "PAID PROGRAM") > 0 or Instr(1, pUpper, "LOCAL NEWS") > 0)
                            score -= 60
                        end if
                    end if
                    if score > bestScore
                        bestScore = score
                        bestCh = ch
                    end if
                end if
            end if
        end for
    end for
    return bestCh
end function

function ResolveChannelShowLine(channel as Dynamic, summaries as Dynamic, games as Object, now as Integer) as String
    if type(channel) <> "roAssociativeArray" then return "No programme information"
    current = CurrentProgramme(ChannelSummary(summaries, Text(channel.id)), now)
    rawTitle = ""
    if type(current) = "roAssociativeArray" then rawTitle = Text(current.title).Trim()
    if rawTitle = "" then rawTitle = Text(channel.eventTitle).Trim()
    if type(games) = "roArray" and games.Count() > 0
        inspectUpper = UCase(Text(channel.eventTitle) + " " + Text(channel.title) + " " + rawTitle)
        for each g in games
            if Text(g.state) <> "post" and SportsGameTeamsMatch(inspectUpper, g)
                detail = Text(g.statusDetail)
                if detail <> "" then return g.headline + " · " + detail
                return g.headline
            end if
        end for
        rawUpper = UCase(rawTitle)
        isGeneric = (rawTitle = "" or Instr(1, rawUpper, "COLLEGE FOOTBALL") > 0 or Instr(1, rawUpper, "COLLEGE BASKETBALL") > 0 or Instr(1, rawUpper, "NBA BASKETBALL") > 0 or Instr(1, rawUpper, "NFL FOOTBALL") > 0 or Instr(1, rawUpper, "MLB BASEBALL") > 0 or Instr(1, rawUpper, "BIG NOON") > 0 or IsPlaceholderEventText(rawTitle))
        if isGeneric
            for each g in games
                if Text(g.state) = "in" and type(g.broadcasts) = "roArray"
                    for each b in g.broadcasts
                        if SportsChannelMatchesNetwork(channel, Text(b))
                            detail = Text(g.statusDetail)
                            if detail <> "" then return g.headline + " · " + detail
                            return g.headline
                        end if
                    end for
                end if
            end for
        end if
    end if
    if type(current) = "roAssociativeArray" then return ProgrammeLine(current)
    if rawTitle <> "" then return rawTitle
    return "No programme information"
end function


