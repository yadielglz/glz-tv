sub init()
    m.top.backgroundURI = ""
    m.top.backgroundColor = "0x080E1CFF"
    m.browser = m.top.findNode("browser")
    m.list = m.top.findNode("channelList")
    m.video = m.top.findNode("player")
    m.task = m.top.findNode("hubTask")
    m.timer = m.top.findNode("syncTimer")
    m.status = m.top.findNode("status")
    m.pairing = m.top.findNode("pairing")
    m.catalog = m.top.findNode("catalog")
    m.channels = []
    m.version = ""
    m.refresh = ""
    m.playingUrl = ""
    m.playingTitle = ""
    m.lastError = ""
    m.autoStarted = false
    m.pending = true
    m.syncsSinceCatalog = 0
    m.queuedForce = false
    m.queuedAction = "sync"
    m.task.ObserveField("result", "onHubResult")
    m.timer.ObserveField("fire", "syncTick")
    m.list.ObserveField("itemSelected", "playSelected")
    m.list.ObserveField("itemFocused", "showDetails")
    m.video.ObserveField("state", "videoStateChanged")
    m.top.SetFocus(true)
    startSync(true)
end sub

sub startSync(force = false as Boolean, action = "sync" as String)
    if m.task.state = "run"
        if force then m.queuedForce = true
        if action = "repair" then m.queuedAction = "repair"
        return
    end if
    if m.queuedForce then force = true
    if m.queuedAction = "repair" then action = "repair"
    m.queuedForce = false
    m.queuedAction = "sync"
    m.timer.control = "stop"
    m.task.action = action
    m.task.force = force
    m.task.knownVersion = m.version
    m.task.knownRefresh = m.refresh
    m.task.activityType = "idle"
    m.task.activityLabel = "Channel browser"
    if m.video.visible
        m.task.activityType = "channel"
        m.task.activityLabel = m.playingTitle
    end if
    m.task.lastChannel = m.playingUrl
    m.task.lastError = m.lastError
    m.task.control = "RUN"
end sub

sub syncTick()
    m.syncsSinceCatalog++
    startSync(m.syncsSinceCatalog >= 5)
end sub

sub scheduleSync(seconds as Integer)
    if m.queuedForce or m.queuedAction = "repair" then seconds = 1
    m.timer.duration = seconds
    m.timer.control = "start"
end sub

sub onHubResult()
    result = m.task.result
    if result = invalid then return
    if result.status = "pending"
        closePlayer()
        m.pending = true
        m.channels = []
        m.catalog.visible = false
        m.pairing.visible = true
        m.top.findNode("pairingCode").text = result.code
        m.status.text = "Ready to pair. Each Roku gets its own channel assignment in GlzHub."
        m.top.SetFocus(true)
        scheduleSync(5)
    else if result.status = "ready"
        m.pending = false
        m.pairing.visible = false
        m.catalog.visible = true
        config = result.config
        m.top.findNode("deviceName").text = Text(config.deviceName, "Roku TV")
        m.version = result.version
        m.refresh = result.refresh
        if result.channels <> invalid then updateChannels(result.channels)
        m.status.text = "Your managed live channels · Updated from GlzHub"
        if m.channels.Count() = 0 then m.status.text = "No channels assigned. Choose a TV playlist for this Roku in GlzHub, then press * → Refresh."
        if result.omitted <> invalid
            if result.omitted > 0 then m.status.text = "Showing the first 1,500 channels. Assign a smaller lineup to this Roku in GlzHub."
        end if
        if not m.video.visible then m.list.SetFocus(true)
        if not m.autoStarted
            m.autoStarted = true
            if config.resumeLastChannel = true and Text(result.lastChannel) <> ""
                for i = 0 to m.channels.Count() - 1
                    if m.channels[i].url = result.lastChannel
                        playChannel(i)
                        exit for
                    end if
                end for
            end if
        end if
        scheduleSync(60)
    else
        m.status.text = Text(result.message, "Network error. Retrying shortly.")
        retry = 15
        if result.retryAfter <> invalid then retry = result.retryAfter
        scheduleSync(retry)
    end if
end sub

sub updateChannels(channels as Object)
    m.syncsSinceCatalog = 0
    m.channels = channels
    root = CreateObject("roSGNode", "ContentNode")
    foundPlaying = m.playingUrl = ""
    focusIndex = 0
    for i = 0 to channels.Count() - 1
        channel = channels[i]
        item = root.CreateChild("ContentNode")
        item.title = channel.title
        item.shortDescriptionLine1 = channel.number
        if item.shortDescriptionLine1 = "" then item.shortDescriptionLine1 = (i + 1).ToStr()
        if channel.url = m.playingUrl
            foundPlaying = true
            focusIndex = i
        end if
    end for
    m.list.content = root
    if channels.Count() > 0 then m.list.jumpToItem = focusIndex
    m.top.findNode("channelCount").text = channels.Count().ToStr() + " channels · Settings sync every minute · Lineup refresh every 5 minutes"
    if not foundPlaying and m.video.visible then closePlayer()
    showDetails()
end sub

sub showDetails()
    index = m.list.itemFocused
    if index < 0 or index >= m.channels.Count()
        m.top.findNode("detailTitle").text = "No channels yet"
        m.top.findNode("detailGroup").text = "Assign a playlist in GlzHub"
        m.top.findNode("channelLogo").uri = ""
        return
    end if
    channel = m.channels[index]
    m.top.findNode("detailTitle").text = channel.title
    m.top.findNode("detailGroup").text = channel.group
    logo = ""
    if IsWebUrl(channel.logo) then logo = channel.logo
    m.top.findNode("channelLogo").uri = logo
end sub

sub playSelected()
    playChannel(m.list.itemSelected)
end sub

sub playChannel(index as Integer)
    if index < 0 or index >= m.channels.Count() then return
    channel = m.channels[index]
    content = CreateObject("roSGNode", "ContentNode")
    content.url = channel.url
    content.title = channel.title
    content.streamFormat = StreamFormat(channel.url)
    content.live = true
    content.HttpCertificatesFile = "common:/certs/ca-bundle.crt"
    headers = []
    for each key in channel.headers
        headers.Push(key + ": " + channel.headers[key])
    end for
    content.HttpHeaders = headers
    m.playingUrl = channel.url
    m.playingTitle = channel.title
    m.lastError = ""
    m.browser.visible = false
    m.video.visible = true
    m.video.content = content
    m.video.SetFocus(true)
    m.video.control = "play"
    startSync(false)
end sub

sub closePlayer()
    if not m.video.visible then return
    m.video.visible = false
    m.video.control = "stop"
    m.browser.visible = true
    m.playingUrl = ""
    m.playingTitle = ""
    m.list.SetFocus(true)
end sub

sub videoStateChanged()
    state = m.video.state
    if state = "error"
        m.lastError = "Roku playback error " + m.video.errorCode.ToStr()
        closePlayer()
        m.status.text = "This channel could not play. Try another channel or Refresh. The stream format may not be supported by this Roku."
        startSync(false)
    else if state = "finished"
        closePlayer()
        m.status.text = "The live stream ended. Select the channel to try again."
    end if
end sub

sub showOptions()
    dialog = CreateObject("roSGNode", "Dialog")
    dialog.title = "Glz TV"
    dialog.message = "Refresh your lineup or pair this Roku again. While watching, press * for Roku captions and audio options."
    dialog.buttons = ["Refresh channels", "Pair again", "Cancel"]
    dialog.ObserveField("buttonSelected", "onOption")
    m.top.dialog = dialog
end sub

sub onOption()
    index = m.top.dialog.buttonSelected
    m.top.dialog.close = true
    if index = 0
        m.status.text = "Refreshing from GlzHub…"
        startSync(true)
    else if index = 1
        dialog = CreateObject("roSGNode", "Dialog")
        dialog.title = "Pair this Roku again?"
        dialog.message = "This replaces the saved device pairing on this Roku. Your GlzHub playlists stay saved."
        dialog.buttons = ["Generate new pairing code", "Cancel"]
        dialog.ObserveField("buttonSelected", "onRepair")
        m.top.dialog = dialog
    else
        restoreFocus()
    end if
end sub

sub onRepair()
    index = m.top.dialog.buttonSelected
    m.top.dialog.close = true
    if index = 0
        closePlayer()
        m.version = ""
        m.autoStarted = false
        startSync(true, "repair")
    end if
    restoreFocus()
end sub

sub restoreFocus()
    if m.pending then m.top.SetFocus(true) else m.list.SetFocus(true)
end sub

function onKeyEvent(key as String, press as Boolean) as Boolean
    if not press then return false
    if m.video.visible
        if key = "back"
            closePlayer()
            startSync(false)
            return true
        end if
        ' Leave * to Roku's native video options, including available captions.
        return false
    end if
    if key = "options"
        showOptions()
        return true
    end if
    return false
end function
