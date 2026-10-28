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

function RadioFormat(url as String) as String
    path = LCase(url.Split("?")[0])
    if Instr(1, path, ".m3u8") > 0 then return "hls"
    if Right(path, 4) = ".aac" then return "es.aac-adts"
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
