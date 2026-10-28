' Pure helpers shared by the network Task and the offline tests.
function Text(value as Dynamic, fallback = "" as String) as String
    if type(value) = "String" or type(value) = "roString" then return value
    return fallback
end function

function IsWebUrl(value as Dynamic) as Boolean
    url = LCase(Text(value))
    return Left(url, 8) = "https://" or Left(url, 7) = "http://"
end function

function Attribute(line as String, key as String) as String
    quotes = Chr(34)
    pattern = "(?:^|\s)" + key + "=" + quotes + "([^" + quotes + "]*)" + quotes
    found = CreateObject("roRegex", pattern, "i").Match(line)
    if found.Count() > 1 then return found[1]
    pattern = "(?:^|\s)" + key + "='([^']*)'"
    found = CreateObject("roRegex", pattern, "i").Match(line)
    if found.Count() > 1 then return found[1]
    return ""
end function

function ExtTitle(line as String) as String
    quoted = false
    quoteChar = ""
    for i = 1 to Len(line)
        char = Mid(line, i, 1)
        if char = Chr(34) or char = "'"
            if not quoted
                quoted = true
                quoteChar = char
            else if char = quoteChar
                quoted = false
            end if
        else if char = "," and not quoted
            return Mid(line, i + 1).Trim()
        end if
    end for
    return "Live channel"
end function

function CopyHeaders(value as Dynamic) as Object
    result = {}
    if type(value) <> "roAssociativeArray" then return result
    for each key in value
        val = Text(value[key])
        if Instr(1, key, Chr(13)) = 0 and Instr(1, key, Chr(10)) = 0 and Instr(1, key, ":") = 0
            if Instr(1, val, Chr(13)) = 0 and Instr(1, val, Chr(10)) = 0
                result[key] = val
            end if
        end if
    end for
    return result
end function

function PlaylistHeaders(url as String, token as String, configured as Dynamic) as Object
    if url = "https://glzhub.glztech.com/api/v1/devices/playlist.m3u"
        return { Authorization: "Bearer " + token }
    end if
    ' Hub device credentials must never reach an external playlist or video host.
    return CopyHeaders(configured)
end function

function ParseCatalog(data as String, defaultHeaders as Dynamic) as Object
    channels = []
    dropped = 0
    info = ""
    headers = CopyHeaders(defaultHeaders)
    lines = data.Replace(Chr(13), "").Split(Chr(10))
    for each raw in lines
        line = raw.Trim()
        if Left(line, 8) = "#EXTINF:"
            info = line
            headers = CopyHeaders(defaultHeaders)
        else if Left(LCase(line), 27) = "#extvlcopt:http-user-agent="
            headers["User-Agent"] = Mid(line, 28)
        else if Left(LCase(line), 25) = "#extvlcopt:http-referrer="
            headers["Referer"] = Mid(line, 26)
        else if Left(line, 1) <> "#" and line <> ""
            if IsWebUrl(line) and Instr(1, line, "|") = 0
                hidden = LCase(Attribute(info, "playlist-studio-hidden")) = "true"
                radio = LCase(Attribute(info, "radio")) = "true"
                if not hidden and not radio
                    if channels.Count() < 1500
                        title = ExtTitle(info)
                        if title = "" then title = "Live channel"
                        channel = { title: title, url: line, group: Attribute(info, "group-title"), number: Attribute(info, "tvg-chno"), logo: Attribute(info, "tvg-logo"), headers: CopyHeaders(headers) }
                        channels.Push(channel)
                    else
                        dropped++
                    end if
                end if
            end if
            info = ""
            headers = CopyHeaders(defaultHeaders)
        end if
    end for
    return { channels: channels, omitted: dropped }
end function

function StreamFormat(url as String) as String
    path = LCase(url.Split("?")[0])
    if Right(path, 4) = ".mpd" then return "dash"
    if Right(path, 4) = ".mp4" or Right(path, 4) = ".m4v" then return "mp4"
    if Right(path, 5) = ".ismc" then return "ism"
    return "hls"
end function
