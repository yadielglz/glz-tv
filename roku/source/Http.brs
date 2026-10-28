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
