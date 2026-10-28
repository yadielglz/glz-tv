sub init()
    m.top.functionName = "runSync"
end sub

function Http(url as String, method as String, headers as Object, payload = "" as String) as Object
    if not IsWebUrl(url) then return { code: 0, body: "" }
    transfer = CreateObject("roUrlTransfer")
    port = CreateObject("roMessagePort")
    transfer.SetPort(port)
    transfer.SetCertificatesFile("common:/certs/ca-bundle.crt")
    transfer.InitClientCertificates()
    transfer.SetUrl(url)
    for each key in headers
        transfer.AddHeader(key, headers[key])
    end for
    if method = "POST"
        transfer.AddHeader("Content-Type", "application/json")
        started = transfer.AsyncPostFromString(payload)
    else
        started = transfer.AsyncGetToString()
    end if
    if not started then return { code: 0, body: "" }
    event = wait(15000, port)
    if type(event) = "roUrlEvent"
        return { code: event.GetResponseCode(), body: event.GetString() }
    end if
    transfer.AsyncCancel()
    return { code: 0, body: "" }
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
        m.top.result = { status: "error", message: "Cannot reach GlzHub. Check the internet connection. Retrying shortly." }
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
    if response.code <> 201
        m.top.result = { status: "error", message: "Pairing could not start. Check the internet connection and try again." }
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
