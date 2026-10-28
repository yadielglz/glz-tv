sub init()
    m.top.functionName = "runExtra"
end sub

sub ExtraError(message as String)
    m.top.result = { mode: m.top.mode, channel: m.top.channel, location: m.top.location, requestId: m.top.requestId, error: message }
end sub

sub runExtra()
    m.version = CreateObject("roAppInfo").GetValue("glz_version")
    hub = "https://glzhub.glztech.com"
    if m.top.mode = "weather"
        location = m.top.location.Trim()
        if location = "" then location = "San Juan"
        escape = CreateObject("roUrlTransfer")
        geo = Http("https://geocoding-api.open-meteo.com/v1/search?name=" + escape.Escape(location) + "&count=1&language=en&format=json", "GET", {})
        if geo.code <> 200
            ExtraError("Weather location lookup failed. Try Refresh and check the city in GlzHub.")
            return
        end if
        data = ParseJson(geo.body)
        if type(data) <> "roAssociativeArray"
            ExtraError("Weather location response was unreadable.")
            return
        end if
        if type(data.results) <> "roArray"
            ExtraError("Weather location not found. Use a city name in GlzHub.")
            return
        end if
        if data.results.Count() = 0
            ExtraError("Weather location not found. Use a city name in GlzHub.")
            return
        end if
        place = data.results[0]
        url = "https://api.open-meteo.com/v1/forecast?latitude=" + place.latitude.ToStr() + "&longitude=" + place.longitude.ToStr()
        url += "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max&temperature_unit=fahrenheit&wind_speed_unit=mph&forecast_days=5&timezone=auto"
        response = Http(url, "GET", {})
        if response.code <> 200
            ExtraError("Weather forecast could not load. Radio and Live TV remain available.")
            return
        end if
        forecast = ParseJson(response.body)
        if type(forecast) <> "roAssociativeArray"
            ExtraError("Weather forecast response was unreadable.")
            return
        end if
        forecast["place"] = Text(place.name, location) + ", " + Text(place.country)
        m.top.result = { mode: "weather", location: m.top.location, requestId: m.top.requestId, data: forecast }
        return
    end if
    token = CreateObject("roRegistrySection", "GlzTV").Read("token")
    if token = ""
        ExtraError("Pair this Roku with GlzHub first.")
        return
    end if
    headers = { "Authorization": "Bearer " + token }
    url = hub + "/api/v1/devices/guide"
    if m.top.mode = "radio" then url = hub + "/api/v1/devices/radio-streams"
    if m.top.mode = "guide"
        escape = CreateObject("roUrlTransfer")
        url += "?channel=" + escape.Escape(m.top.channel)
    end if
    response = Http(url, "GET", headers)
    if response.code <> 200
        message = HttpFailure(response, "Section unavailable")
        if response.code = 404 and m.top.mode <> "radio" then message = "Guide API not available. Deploy the updated GlzHub, then choose Refresh in the Roku app."
        ExtraError(message)
        return
    end if
    data = ParseJson(response.body)
    if type(data) <> "roAssociativeArray"
        ExtraError("GlzHub returned an unreadable response for this section.")
        return
    end if
    m.top.result = { mode: m.top.mode, channel: m.top.channel, requestId: m.top.requestId, data: data }
end sub
