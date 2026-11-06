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

function ApplyStreamHeaders(headers as Object, raw as String) as Void
    for each pair in raw.Split("&")
        eq = Instr(1, pair, "=")
        if eq > 1
            key = Left(pair, eq - 1).Trim()
            val = Mid(pair, eq + 1).Trim().Replace("%20", " ").Replace("%3A", ":").Replace("%2F", "/")
            if key <> "" and Instr(1, key, ":") = 0 and Instr(1, key, Chr(10)) = 0 and Instr(1, key, Chr(13)) = 0
                if Instr(1, val, Chr(10)) = 0 and Instr(1, val, Chr(13)) = 0
                    headers[key] = val
                end if
            end if
        end if
    end for
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
        else if Left(LCase(line), 46) = "#kodiprop:inputstream.adaptive.stream_headers="
            ApplyStreamHeaders(headers, Mid(line, 47))
        else if Left(line, 1) <> "#" and line <> ""
            if IsWebUrl(line) and Instr(1, line, "|") = 0
                hidden = LCase(Attribute(info, "playlist-studio-hidden")) = "true"
                radio = LCase(Attribute(info, "radio")) = "true"
                if not hidden and not radio
                    if channels.Count() < 1500
                        title = ExtTitle(info)
                        if title = "" then title = "Live channel"
                        channel = { title: title, url: line, group: Attribute(info, "group-title"), number: Attribute(info, "tvg-chno"), logo: Attribute(info, "tvg-logo"), headers: CopyHeaders(headers) }
                        channel["id"] = Attribute(info, "tvg-id")
                        if channel.id = "" then channel["id"] = title
                        channel["eventTitle"] = Attribute(info, "x-event-title")
                        channel["league"] = Attribute(info, "x-event-league")
                        channel["eventStart"] = EventSeconds(Attribute(info, "x-event-start"))
                        channel["eventEnd"] = EventSeconds(Attribute(info, "x-event-end"))
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
    if Right(path, 4) = ".mp4" or Right(path, 4) = ".m4v" or Right(path, 4) = ".mkv" then return "mp4"
    if Right(path, 5) = ".ismc" then return "ism"
    return "hls"
end function

function MovieGenresLabel(genres as Dynamic) as String
    if type(genres) <> "roArray" then return ""
    parts = []
    for each g in genres
        name = Text(g).Trim()
        if name <> "" and parts.Count() < 3 then parts.Push(name)
    end for
    return parts.Join(", ")
end function

function MovieMetaLine(year as Integer, runtime as Integer, genres as String) as String
    parts = []
    if year > 0 then parts.Push(year.ToStr())
    if runtime > 0 then parts.Push(runtime.ToStr() + " min")
    if genres <> "" then parts.Push(genres)
    if parts.Count() = 0 then return "Movie · On Demand"
    return parts.Join(" · ")
end function

function ParseMovies(rawMovies as Dynamic, defaultHeaders = {} as Dynamic) as Object
    movies = []
    if type(rawMovies) <> "roArray" then return movies
    for each item in rawMovies
        if type(item) = "roAssociativeArray"
            url = Text(item.streamUrl).Trim()
            title = Text(item.title).Trim()
            if title <> "" and IsWebUrl(url) and Instr(1, url, "|") = 0
                id = Text(item.id).Trim()
                if id = "" then id = title
                year = 0
                if type(item.releaseYear) = "Integer" or type(item.releaseYear) = "roInt"
                    if item.releaseYear > 0 then year = item.releaseYear
                end if
                runtime = 0
                if type(item.runtimeMinutes) = "Integer" or type(item.runtimeMinutes) = "roInt"
                    if item.runtimeMinutes > 0 then runtime = item.runtimeMinutes
                end if
                genres = MovieGenresLabel(item.genres)
                meta = MovieMetaLine(year, runtime, genres)
                logo = Text(item.posterUrl).Trim()
                if not IsWebUrl(logo) then logo = Text(item.backdropUrl).Trim()
                headers = CopyHeaders(defaultHeaders)
                movieHeaders = CopyHeaders(item.requestHeaders)
                for each key in movieHeaders
                    headers[key] = movieHeaders[key]
                end for
                badge = "VOD"
                if year > 0 then badge = year.ToStr()
                movie = {
                    id: "movie:" + id
                    title: title
                    url: url
                    logo: logo
                    group: meta
                    description: Text(item.description).Trim()
                    number: badge
                    headers: headers
                    isMovie: true
                    eventTitle: ""
                    league: ""
                    eventStart: 0
                    eventEnd: 0
                }
                movies.Push(movie)
            end if
        end if
    end for
    return movies
end function
