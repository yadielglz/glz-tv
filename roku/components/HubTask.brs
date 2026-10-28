sub init()
    m.top.functionName = "runSync"
end sub

function Http(url as String, method as String, headers as Object, payload = "" as String) as Object
    if not IsWebUrl(url) then return { code: -3, body: "", reason: "Invalid URL" }
    transfer = CreateObject("roUrlTransfer")
    port = CreateObject("roMessagePort")
    transfer.SetPort(port)
    transfer.SetCertificatesFile("common:/certs/ca-bundle.crt")
    ' Public HTTPS needs server certificate validation, not Roku client mTLS.
    ' Client certificates are deprecated on OS 16 and unsuitable for sideloads.
    transfer.RetainBodyOnError(true)
    transfer.EnableEncodings(true)
    transfer.SetUrl(url)
    if not headers.DoesExist("User-Agent")
        transfer.AddHeader("User-Agent", "GlzTV-Roku/" + Text(m.version, "26.X27.2201"))
    end if
    for each key in headers
        transfer.AddHeader(key, headers[key])
    end for
    if method = "POST"
        transfer.AddHeader("Content-Type", "application/json")
        started = transfer.AsyncPostFromString(payload)
    else
        started = transfer.AsyncGetToString()
    end if
    if not started then return { code: 0, body: "", reason: transfer.GetFailureReason() }
    clock = CreateObject("roTimespan")
    clock.Mark()
    while clock.TotalMilliseconds() < 30000
        event = wait(30000 - clock.TotalMilliseconds(), port)
        if event = invalid then exit while
        if type(event) = "roUrlEvent"
            if event.GetSourceIdentity() = transfer.GetIdentity() and event.GetInt() = 1
                return { code: event.GetResponseCode(), body: event.GetString(), reason: event.GetFailureReason() }
            end if
        end if
    end while
    transfer.AsyncCancel()
    return { code: -28, body: "", reason: "Request timed out" }
end function

function HttpFailure(response as Object, operation as String) as String
    code = response.code
    prefix = operation + " (" + code.ToStr() + "): "
    if code = -6 then return prefix + "The Roku cannot resolve glzhub.glztech.com. Check its DNS/network settings."
    if code = -7 then return prefix + "The Roku could not connect to the server."
    if code = -28 or code = 0 then return prefix + "The request timed out or could not start. Check the Roku connection."
    if code = -35 or code = -51 or code = -60 or code = -77
        return prefix + "Secure connection/certificate error. Check the TV's date/time and Roku system updates."
    end if
    if code = 403 then return prefix + "Server access denied. Check Cloudflare/Access rules for the Hub API."
    if code = 404 then return prefix + "Hub API not found. Check the deployed GlzHub version."
    if code = 429 then return prefix + "Too many requests. Waiting before retrying."
    if code >= 500 then return prefix + "GlzHub server/database error."
    data = ParseJson(Text(response.body))
    if type(data) = "roAssociativeArray"
        detail = Text(data.error)
        ' Only show known safe validation errors, never raw server bodies or tokens.
        if Left(detail, 8) = "Invalid " or detail = "Expected application/json"
            return prefix + Left(detail, 100)
        end if
    end if
    return prefix + "Request failed. Report this code so we can identify the cause."
end function

function HubHeaders() as Object
    return { Authorization: "Bearer " + m.token }
end function

sub ReportHeartbeat(config as Object, syncState as String, message as String)
    progress = 100
    if syncState = "failed" then progress = 0
    payload = {
        appVersion: m.version
        lastError: m.top.lastError
        activity: { type: m.top.activityType, label: m.top.activityLabel }
        sync: { status: syncState, progress: progress, message: message }
    }
    Http(m.hub + "/api/v1/devices/heartbeat", "POST", HubHeaders(), FormatJson(payload))
end sub

sub runSync()
    m.hub = "https://glzhub.glztech.com"
    m.version = CreateObject("roAppInfo").GetValue("glz_version")
    m.registry = CreateObject("roRegistrySection", "GlzTV")
    if m.top.action = "repair"
        m.registry.Delete("token")
        m.registry.Delete("paired")
        m.registry.Delete("code")
        m.registry.Flush()
    end if
    if not m.registry.Exists("installation")
        m.registry.Write("installation", "roku-" + CreateObject("roDeviceInfo").GetChannelClientId())
        m.registry.Flush()
    end if
    m.token = m.registry.Read("token")
    expiry = m.registry.Read("expiry").ToInt()
    now = CreateObject("roDateTime").AsSeconds()
    paired = m.registry.Read("paired") = "true"
    if m.token = "" or (not paired and now >= expiry)
        BeginEnrollment()
        return
    end if
    response = Http(m.hub + "/api/v1/devices/config", "GET", HubHeaders())
    if response.code = 401 or response.code = 403
        if paired
            m.registry.Delete("token")
            m.registry.Delete("paired")
            m.registry.Flush()
            BeginEnrollment()
        else
            m.top.result = { status: "pending", code: m.registry.Read("code"), expires: expiry }
        end if
        return
    end if
    if response.code <> 200
        m.top.result = { status: "error", message: HttpFailure(response, "Hub sync failed") }
        return
    end if
    config = ParseJson(response.body)
    if type(config) <> "roAssociativeArray"
        m.top.result = { status: "error", message: "GlzHub returned an unreadable configuration." }
        return
    end if
    m.registry.Write("paired", "true")
    m.registry.Delete("code")
    m.registry.Flush()
    if m.top.lastChannel <> ""
        m.registry.Write("lastChannel", m.top.lastChannel)
        m.registry.Flush()
    end if
    version = FormatJson(config.version)
    refresh = Text(config.forceRefreshToken)
    result = { status: "ready", config: config, version: version, refresh: refresh, lastChannel: m.registry.Read("lastChannel") }
    if m.top.force or m.top.knownVersion <> version or m.top.knownRefresh <> refresh
        playlistUrl = Text(config.playlistUrl)
        response = Http(playlistUrl, "GET", PlaylistHeaders(playlistUrl, m.token, config.requestHeaders))
        if response.code <> 200
            ReportHeartbeat(config, "failed", "Roku could not download its playlist")
            m.top.result = { status: "error", message: "Playlist could not load. Check the assigned lineup in GlzHub, then try Refresh." }
            return
        end if
        if Instr(1, response.body, "#EXTM3U") = 0
            ReportHeartbeat(config, "failed", "Roku playlist response was not M3U")
            m.top.result = { status: "error", message: "This playlist is not an M3U channel list. Check its URL in GlzHub." }
            return
        end if
        catalog = ParseCatalog(response.body, config.requestHeaders)
        result.channels = catalog.channels
        result.omitted = catalog.omitted
    end if
    ReportHeartbeat(config, "complete", "Roku configuration applied")
    m.top.result = result
end sub

sub BeginEnrollment()
    payload = {
        installationId: m.registry.Read("installation")
        platform: "Roku TV"
        model: CreateObject("roDeviceInfo").GetModel()
        appVersion: m.version
    }
    response = Http(m.hub + "/api/v1/enrollment", "POST", {}, FormatJson(payload))
    if response.code <> 201 and response.code <> 200
        retry = 15
        if response.code = 429 or response.code >= 500 then retry = 60
        m.top.result = { status: "error", message: HttpFailure(response, "Pairing failed"), retryAfter: retry }
        return
    end if
    data = ParseJson(response.body)
    if type(data) <> "roAssociativeArray"
        m.top.result = { status: "error", message: "GlzHub returned an unreadable pairing response." }
        return
    end if
    if Text(data.deviceToken) = "" or Text(data.pairingCode) = ""
        m.top.result = { status: "error", message: "GlzHub did not return a pairing code. Try again." }
        return
    end if
    expiry = CreateObject("roDateTime").AsSeconds() + 3600
    m.registry.Write("token", data.deviceToken)
    m.registry.Write("code", data.pairingCode)
    m.registry.Write("expiry", expiry.ToStr())
    m.registry.Flush()
    m.top.result = { status: "pending", code: data.pairingCode, expires: expiry }
end sub
