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
    path = LCase(url.Split("?")[0])
    if Instr(1, path, ".m3u8") > 0 then return "hls"
    if Right(path, 4) = ".aac" or Right(path, 5) = ".aacp" or Instr(1, path, "_icy") > 0 or Instr(1, path, "-icy") > 0 or Instr(1, path, "lamusica.com/") > 0 or Instr(1, path, "server20.servistreaming.com:9022") > 0 or Instr(1, path, "server20.servistreaming.com:9023") > 0 then return "es.aac-adts"
    if Right(path, 4) = ".m4a" or Right(path, 4) = ".mp4" then return "mp4"
    return "mp3"
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
