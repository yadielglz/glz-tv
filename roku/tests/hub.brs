sub Main()
    m.failed = 0
    ResetTest()
    m.responses = [{code: 201, body: FormatJson({deviceToken: "glz_test_secret", pairingCode: "GLZ-ABC123"})}]
    runSync()
    Assert(m.top.result.status = "pending", "new device gets a pairing code")
    Assert(m.store.data.token = "glz_test_secret", "device token stays in registry")
    Assert(Instr(1, FormatJson(m.top.result), "secret") = 0, "pairing result contains no device token")
    m.responses = [{code: 401, body: ""}]
    m.requests = []
    runSync()
    Assert(m.top.result.status = "pending" and m.requests.Count() = 1, "pending device polls without creating repeated enrollments")

    config = {version: 1, forceRefreshToken: "a", playlistUrl: "https://glzhub.glztech.com/api/v1/devices/playlist.m3u", requestHeaders: {"User-Agent": "ProviderPlayer"}}
    playlist = "#EXTM3U" + Chr(10) + "#EXTINF:-1,Family Channel" + Chr(10) + "https://cdn.example/live.m3u8"
    m.responses = [{code: 200, body: FormatJson(config)}, {code: 200, body: playlist}, {code: 200, body: "{}"}]
    m.requests = []
    runSync()
    Assert(m.top.result.status = "ready" and m.top.result.channels.Count() = 1, "claiming device loads its assigned playlist")
    Assert(m.requests[0].headers.Authorization = "Bearer glz_test_secret", "configuration request authenticates")
    Assert(m.requests[1].headers.Authorization = "Bearer glz_test_secret", "managed playlist request authenticates")
    Assert(m.top.result.channels[0].headers.Authorization = invalid, "video does not inherit Hub device token")
    Assert(m.top.result.channels[0].headers["User-Agent"] = "ProviderPlayer", "provider playback headers are passed separately")
    Assert(m.store.data.paired = "true" and not m.store.data.DoesExist("code"), "successful pairing clears code")
    heartbeat = ParseJson(m.requests[2].payload)
    Assert(heartbeat.sync.status = "complete", "successful sync reports completion to Hub")

    m.top.knownVersion = "1"
    m.top.knownRefresh = "a"
    m.top.force = false
    m.responses = [{code: 200, body: FormatJson(config)}, {code: 200, body: "{}"}]
    m.requests = []
    runSync()
    Assert(m.requests.Count() = 2 and m.top.result.channels = invalid, "unchanged configuration skips catalog download")

    m.top.force = true
    config.playlistUrl = "https://provider.example/list.m3u"
    m.responses = [{code: 200, body: FormatJson(config)}, {code: 200, body: playlist}, {code: 200, body: "{}"}]
    m.requests = []
    runSync()
    Assert(m.requests[1].headers.Authorization = invalid, "external override never receives Hub token")

    m.responses = [{code: 0, body: ""}]
    runSync()
    Assert(m.top.result.status = "error" and m.store.data.token = "glz_test_secret", "network timeout preserves pairing")

    m.responses = [{code: 401, body: ""}, {code: 201, body: FormatJson({deviceToken: "glz_new_secret", pairingCode: "GLZ-NEW123"})}]
    runSync()
    Assert(m.top.result.status = "pending" and m.store.data.token = "glz_new_secret", "revoked pairing creates a fresh enrollment")

    m.store.data.expiry = "999"
    m.responses = [{code: 201, body: FormatJson({deviceToken: "glz_renewed", pairingCode: "GLZ-EXP123"})}]
    m.requests = []
    runSync()
    Assert(m.requests.Count() = 1 and m.top.result.code = "GLZ-EXP123", "expired pairing is renewed")

    m.top.action = "repair"
    m.store.data.paired = "true"
    m.responses = [{code: 201, body: FormatJson({deviceToken: "glz_repair", pairingCode: "GLZ-REP123"})}]
    runSync()
    Assert(m.top.result.status = "pending" and not m.store.data.DoesExist("paired"), "explicit repair clears previous authentication")
    if m.failed > 0 then print "FAILURES: "; m.failed else print "ALL HUB TESTS PASSED"
end sub

sub ResetTest()
    m.top = {action: "sync", force: true, knownVersion: "", knownRefresh: "", lastChannel: "", lastError: "", activityType: "idle", activityLabel: "Channel browser"}
    m.store = {
        data: {}
        Exists: function(key)
            return m.data.DoesExist(key)
        end function
        Read: function(key)
            if m.data.DoesExist(key) then return m.data[key]
            return ""
        end function
        Write: function(key, value)
            m.data[key] = value
        end function
        Delete: function(key)
            m.data.Delete(key)
        end function
        Flush: function()
            return true
        end function
    }
    m.responses = []
    m.requests = []
end sub

function TestHttp(url as String, method as String, headers as Object, payload = "" as String) as Object
    m.requests.Push({url: url, method: method, headers: headers, payload: payload})
    if m.responses.Count() = 0 then return {code: 0, body: ""}
    return m.responses.Shift()
end function

sub Assert(condition as Boolean, label as String)
    if condition
        print "PASS: "; label
    else
        m.failed++
        print "FAIL: "; label
    end if
end sub
