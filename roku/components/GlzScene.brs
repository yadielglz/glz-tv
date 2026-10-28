sub init()
    m.top.backgroundURI = ""
    m.top.backgroundColor = "0x080E1CFF"
    m.browser = m.top.findNode("browser")
    m.list = m.top.findNode("channelList")
    m.nav = m.top.findNode("sectionNav")
    m.video = m.top.findNode("player")
    m.audio = m.top.findNode("radioPlayer")
    m.task = m.top.findNode("hubTask")
    m.extra = m.top.findNode("extrasTask")
    m.timer = m.top.findNode("syncTimer")
    m.extraTimer = m.top.findNode("extrasTimer")
    m.status = m.top.findNode("status")
    m.pairing = m.top.findNode("pairing")
    m.catalog = m.top.findNode("catalog")
    m.channels = [] : m.rows = [] : m.stations = [] : m.summaries = {} : m.schedule = []
    m.weatherError = "" : m.weather = invalid : m.weatherLocation = "" : m.weatherAge = 15
    m.extraQueue = [] : m.extraId = 0 : m.guideChannel = invalid
    m.view = "live" : m.guideError = "Loading programme guide…"
    m.version = "" : m.refresh = "" : m.playingUrl = "" : m.playingTitle = "" : m.radioTitle = ""
    m.lastError = "" : m.autoStarted = false : m.pending = true
    m.syncsSinceCatalog = 0 : m.queuedForce = false : m.queuedAction = "sync"
    root = CreateObject("roSGNode", "ContentNode")
    for each title in ["Live TV", "Guide", "PPV Events", "Radio", "Weather"]
        item = root.CreateChild("ContentNode") : item.title = title
    end for
    m.nav.content = root
    m.nav.ObserveField("itemSelected", "selectSection")
    m.task.ObserveField("result", "onHubResult")
    m.extra.ObserveField("result", "onExtraResult")
    m.extraTimer.ObserveField("fire", "runNextExtra")
    m.timer.ObserveField("fire", "syncTick")
    m.list.ObserveField("itemSelected", "selectRow")
    m.list.ObserveField("itemFocused", "showDetails")
    m.video.ObserveField("state", "videoStateChanged")
    m.audio.ObserveField("state", "audioStateChanged")
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
    m.queuedForce = false : m.queuedAction = "sync"
    m.timer.control = "stop"
    m.task.action = action : m.task.force = force
    m.task.knownVersion = m.version : m.task.knownRefresh = m.refresh
    m.task.activityType = "idle" : m.task.activityLabel = "Roku " + m.view
    if m.radioTitle <> ""
        m.task.activityType = "radio" : m.task.activityLabel = m.radioTitle
    end if
    if m.video.visible
        m.task.activityType = "channel" : m.task.activityLabel = m.playingTitle
    end if
    m.task.lastChannel = m.playingUrl : m.task.lastError = m.lastError
    m.task.control = "RUN"
end sub

sub syncTick()
    m.syncsSinceCatalog++ : m.weatherAge++
    startSync(m.syncsSinceCatalog >= 5)
end sub

sub scheduleSync(seconds as Integer)
    if m.queuedForce or m.queuedAction = "repair" then seconds = 1
    m.timer.duration = seconds : m.timer.control = "start"
end sub

sub onHubResult()
    result = m.task.result
    if result = invalid then return
    if result.status = "pending"
        closePlayer() : stopRadio()
        m.pending = true : m.channels = [] : m.rows = [] : m.stations = [] : m.summaries = {} : m.schedule = [] : m.weather = invalid
        m.extraQueue = [] : m.extraId++ : m.extra.control = "STOP"
        m.catalog.visible = false : m.pairing.visible = true
        m.top.findNode("pairingCode").text = result.code
        m.status.text = "Ready to pair. Each Roku gets its own channel assignment in GlzHub."
        m.top.SetFocus(true) : scheduleSync(5)
    else if result.status = "ready"
        m.pending = false : m.pairing.visible = false : m.catalog.visible = true
        config = result.config
        m.top.findNode("deviceName").text = Text(config.deviceName, "Roku TV")
        m.version = result.version : m.refresh = result.refresh
        if result.channels <> invalid
            m.channels = result.channels : m.syncsSinceCatalog = 0
            foundPlaying = m.playingUrl = ""
            for each channel in m.channels
                if channel.url = m.playingUrl then foundPlaying = true
            end for
            if not foundPlaying and m.video.visible then closePlayer()
            queueExtra("summary") : queueExtra("radio")
            if m.view = "schedule" and m.guideChannel <> invalid then queueExtra("guide", m.guideChannel.id)
        end if
        location = Text(config.weatherLocation)
        if location <> m.weatherLocation or m.weatherAge >= 15
            if location <> m.weatherLocation then m.weather = invalid
            m.weatherError = "" : m.weatherLocation = location : m.weatherAge = 0
            queueExtra("weather")
        end if
        renderRows()
        m.status.text = "GlzHub connected · Select a section on the left"
        if result.omitted <> invalid
            if result.omitted > 0 then m.status.text = "Showing the first 1,500 channels. Assign a smaller lineup in GlzHub."
        end if
        if not m.autoStarted
            m.autoStarted = true : m.list.SetFocus(true)
            if config.resumeLastChannel = true and Text(result.lastChannel) <> ""
                for each channel in m.channels
                    if channel.url = result.lastChannel
                        playItem(channel) : exit for
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

sub queueExtra(mode as String, channel = "" as String)
    if m.pending then return
    for each queued in m.extraQueue
        if queued.mode = mode
            queued.channel = channel : return
        end if
    end for
    m.extraQueue.Push({ mode: mode, channel: channel })
    m.extraTimer.control = "start"
end sub

sub runNextExtra()
    if m.pending or m.extraQueue.Count() = 0 then return
    if m.extra.state = "run"
        m.extraTimer.control = "start" : return
    end if
    job = m.extraQueue.Shift() : m.extraId++
    m.extra.mode = job.mode : m.extra.channel = job.channel
    m.extra.location = m.weatherLocation : m.extra.requestId = m.extraId
    m.extra.control = "RUN"
end sub

sub onExtraResult()
    result = m.extra.result
    if result = invalid or m.pending then return
    if result.requestId <> m.extraId then return
    if Text(result.error) <> ""
        if result.mode = "summary" or result.mode = "guide"
            m.guideError = result.error
            if m.view = "guide" or m.view = "schedule" then m.status.text = result.error
        else if result.mode = "weather"
            if result.location = m.weatherLocation
                m.weatherError = result.error
                if m.view = "weather" then m.top.findNode("weatherText").text = result.error
            end if
        else if result.mode = "radio"
            if m.view = "radio" then m.status.text = result.error
        end if
    else
        data = result.data
        if result.mode = "radio"
            m.stations = []
            if type(data.stations) = "roArray"
                for each station in data.stations
                    if IsWebUrl(station.streamUrl)
                        m.stations.Push({ title: Text(station.name), number: Text(station.code), url: station.streamUrl, logo: Text(station.logoUrl), group: Text(station.genre), headers: CopyHeaders(station.requestHeaders), id: Text(station.code) })
                    end if
                end for
            end if
        else if result.mode = "weather"
            if result.location = m.weatherLocation
                m.weather = data : m.weatherError = ""
            end if
        else
            if type(data.summaries) = "roAssociativeArray" then m.summaries = data.summaries
            m.guideError = Text(data.guideError)
            if result.mode = "guide" and m.guideChannel <> invalid
                if result.channel = m.guideChannel.id
                    m.schedule = []
                    if type(data.programmes) = "roArray" then m.schedule = data.programmes
                end if
            end if
        end if
        renderRows()
    end if
    if m.extraQueue.Count() > 0 then m.extraTimer.control = "start"
end sub

sub selectSection()
    openSection(m.nav.itemSelected)
end sub

sub openSection(index as Integer)
    views = ["live", "guide", "events", "radio", "weather"]
    if index < 0 or index >= views.Count() then return
    m.view = views[index] : m.guideChannel = invalid
    renderRows()
    if m.view = "radio" then queueExtra("radio")
    if m.view = "guide" then queueExtra("summary")
    if m.view = "weather" and m.weather = invalid then queueExtra("weather")
    if m.view <> "weather" and m.rows.Count() > 0 then m.list.SetFocus(true)
end sub

sub renderRows()
    index = m.list.itemFocused
    if index < 0 then index = 0
    m.rows = []
    isWeather = m.view = "weather"
    m.list.visible = not isWeather
    m.top.findNode("detailPanel").visible = not isWeather
    m.top.findNode("weatherText").visible = isWeather
    titles = { live: "Live TV", guide: "Programme guide · OK opens a channel's next 24 hours", events: "PPV / Live events", radio: "Radio · Play/Pause controls audio", weather: "Weather · Forecast location from GlzHub", schedule: "Channel schedule · Next 24 hours" }
    m.top.findNode("sectionTitle").text = titles[m.view]
    if isWeather
        m.top.findNode("weatherText").text = WeatherLines(m.weather)
        if m.weather = invalid and m.weatherError <> "" then m.top.findNode("weatherText").text = m.weatherError
        m.top.findNode("channelCount").text = "Forecast updates every 15 minutes · Open-Meteo.com"
        updateFooter() : return
    end if
    if m.view = "schedule"
        m.rows = m.schedule
    else if m.view = "radio"
        m.rows = m.stations
    else
        for each channel in m.channels
            if m.view <> "events" or Text(channel.eventTitle) <> "" then m.rows.Push(channel)
        end for
    end if
    now = CreateObject("roDateTime").AsSeconds()
    root = CreateObject("roSGNode", "ContentNode")
    for i = 0 to m.rows.Count() - 1
        row = m.rows[i] : item = root.CreateChild("ContentNode")
        if m.view = "schedule"
            item.title = Text(row.title)
            item.shortDescriptionLine1 = ""
            item.description = ClockLabel(row.start) + " – " + ClockLabel(row.end)
        else
            item.title = row.title : item.shortDescriptionLine1 = Text(row.number)
            if item.shortDescriptionLine1 = "" then item.shortDescriptionLine1 = (i + 1).ToStr()
            if m.view = "events"
                item.title = row.eventTitle
                item.description = row.league + " · " + ClockLabel(row.eventStart) + " – " + ClockLabel(row.eventEnd)
            else if m.view = "radio"
                item.description = row.group
            else
                item.description = ProgrammeLine(CurrentProgramme(m.summaries[row.id], now))
            end if
        end if
    end for
    m.list.content = root
    if index >= m.rows.Count() then index = 0
    if m.rows.Count() > 0 then m.list.jumpToItem = index
    m.top.findNode("channelCount").text = m.rows.Count().ToStr() + " items · * Refresh / Options"
    if m.rows.Count() = 0
        if m.view = "schedule" or m.view = "guide" then m.status.text = "No programme data yet. " + m.guideError
        if m.view = "events" then m.status.text = "No active PPV events in the current lineup."
        if m.view = "radio" then m.status.text = "No radio stations loaded. Manage stations in GlzHub or choose Refresh."
    end if
    showDetails() : updateFooter()
end sub

sub showDetails()
    index = m.list.itemFocused
    if index < 0 or index >= m.rows.Count()
        m.top.findNode("detailTitle").text = "Nothing to show yet"
        m.top.findNode("detailGroup").text = "Choose a section or Refresh."
        m.top.findNode("channelLogo").uri = "" : return
    end if
    row = m.rows[index]
    m.top.findNode("detailTitle").text = row.title
    logo = Text(row.logo)
    if not IsWebUrl(logo) then logo = ""
    m.top.findNode("channelLogo").uri = logo
    if m.view = "schedule"
        m.top.findNode("detailGroup").text = ClockLabel(row.start) + " – " + ClockLabel(row.end) + Chr(10) + Text(row.description)
    else if m.view = "events"
        m.top.findNode("detailTitle").text = row.eventTitle
        m.top.findNode("detailGroup").text = row.league + Chr(10) + ClockLabel(row.eventStart) + " – " + ClockLabel(row.eventEnd) + Chr(10) + "OK to watch · SPORTS PPV"
    else if m.view = "radio"
        m.top.findNode("detailGroup").text = row.group + Chr(10) + "OK to listen. Audio continues while browsing other sections."
    else
        summary = m.summaries[row.id]
        description = "Now: " + ProgrammeLine(CurrentProgramme(summary, CreateObject("roDateTime").AsSeconds()))
        if type(summary) = "roAssociativeArray"
            if summary.next <> invalid then description += Chr(10) + "Next: " + ProgrammeLine(summary.next)
        end if
        if Text(row.eventTitle) <> "" then description = row.eventTitle + Chr(10) + row.league + " · " + ClockLabel(row.eventStart)
        m.top.findNode("detailGroup").text = description
    end if
end sub

sub selectRow()
    index = m.list.itemSelected
    if index < 0 or index >= m.rows.Count() then return
    row = m.rows[index]
    if m.view = "guide"
        m.guideChannel = row : m.schedule = [] : m.view = "schedule"
        m.status.text = "Loading schedule for " + row.title + "…"
        renderRows() : queueExtra("guide", row.id)
    else if m.view = "schedule"
        dialog = CreateObject("roSGNode", "Dialog")
        dialog.title = row.title
        dialog.message = ClockLabel(row.start) + " – " + ClockLabel(row.end) + Chr(10) + Text(row.description)
        dialog.buttons = ["Watch this channel live", "Back"]
        dialog.ObserveField("buttonSelected", "watchGuideChannel") : m.top.dialog = dialog
    else if m.view = "radio"
        playRadio(row)
    else
        playItem(row)
    end if
end sub

sub watchGuideChannel()
    selected = m.top.dialog.buttonSelected : m.top.dialog.close = true
    if selected = 0 and m.guideChannel <> invalid then playItem(m.guideChannel)
end sub

function PlayerContent(row as Object, format as String) as Object
    content = CreateObject("roSGNode", "ContentNode")
    content.url = row.url : content.title = row.title : content.streamFormat = format
    content.HttpCertificatesFile = "common:/certs/ca-bundle.crt"
    headers = []
    for each key in row.headers
        headers.Push(key + ": " + row.headers[key])
    end for
    content.HttpHeaders = headers
    return content
end function

sub playItem(row as Object)
    stopRadio()
    content = PlayerContent(row, StreamFormat(row.url)) : content.live = true
    m.playingUrl = row.url : m.playingTitle = row.title : m.lastError = ""
    m.browser.visible = false : m.video.visible = true
    m.video.content = content : m.video.SetFocus(true) : m.video.control = "play"
    startSync(false)
end sub

sub playRadio(row as Object)
    closePlayer()
    m.audio.control = "stop"
    m.radioTitle = row.title
    m.audio.content = PlayerContent(row, RadioFormat(row.url))
    m.audio.control = "play" : updateFooter() : startSync(false)
end sub

sub stopRadio()
    m.audio.control = "stop" : m.radioTitle = "" : updateFooter()
end sub

sub updateFooter()
    footerText = "Left / Back  Sections   •   OK  Select   •   *  Options / Captions while watching"
    if m.radioTitle <> "" then footerText = "Radio: " + m.radioTitle + "   •   Play/Pause controls audio   •   * → Stop radio"
    m.top.findNode("footer").text = footerText
end sub

sub closePlayer()
    if not m.video.visible then return
    m.video.visible = false : m.video.control = "stop" : m.browser.visible = true
    m.playingUrl = "" : m.playingTitle = "" : restoreFocus()
end sub

sub videoStateChanged()
    if m.video.state = "error"
        m.lastError = "Roku playback error " + m.video.errorCode.ToStr()
        closePlayer()
        m.status.text = "Channel could not play. Try another channel or Refresh."
        startSync(false)
    else if m.video.state = "finished"
        closePlayer() : m.status.text = "The live stream ended. Select the channel to try again."
    end if
end sub

sub audioStateChanged()
    if m.audio.state = "error"
        stopRadio() : m.status.text = "Radio stream could not play. Try another station or Refresh."
    else if m.audio.state = "finished"
        stopRadio() : m.status.text = "Radio stream ended. Select the station to reconnect."
    end if
end sub

sub showOptions()
    dialog = CreateObject("roSGNode", "Dialog")
    dialog.title = "Glz TV"
    dialog.message = "Refresh the lineup, guide, radio, and weather. Captions are available in the native * menu while watching."
    dialog.buttons = ["Refresh all sections", "Stop radio", "Pair again", "Cancel"]
    dialog.ObserveField("buttonSelected", "onOption") : m.top.dialog = dialog
end sub

sub onOption()
    index = m.top.dialog.buttonSelected : m.top.dialog.close = true
    if index = 0
        m.status.text = "Refreshing from GlzHub…"
        startSync(true) : queueExtra("weather")
        if m.view = "schedule" and m.guideChannel <> invalid then queueExtra("guide", m.guideChannel.id)
    else if index = 1
        stopRadio() : restoreFocus()
    else if index = 2
        dialog = CreateObject("roSGNode", "Dialog")
        dialog.title = "Pair this Roku again?"
        dialog.message = "This replaces the saved device pairing on this Roku. Your GlzHub playlists stay saved."
        dialog.buttons = ["Generate new pairing code", "Cancel"]
        dialog.ObserveField("buttonSelected", "onRepair") : m.top.dialog = dialog
    else
        restoreFocus()
    end if
end sub

sub onRepair()
    selected = m.top.dialog.buttonSelected : m.top.dialog.close = true
    if selected = 0
        closePlayer() : stopRadio() : m.version = "" : m.autoStarted = false
        startSync(true, "repair")
    end if
    restoreFocus()
end sub

sub restoreFocus()
    if m.pending
        m.top.SetFocus(true)
    else if m.view = "weather" or m.rows.Count() = 0
        m.nav.SetFocus(true)
    else
        m.list.SetFocus(true)
    end if
end sub

function onKeyEvent(key as String, press as Boolean) as Boolean
    if not press then return false
    if m.video.visible
        if key = "back"
            closePlayer() : startSync(false) : return true
        end if
        return false
    end if
    if key = "options"
        showOptions() : return true
    end if
    if key = "play" and m.radioTitle <> ""
        if m.audio.state = "paused" then m.audio.control = "resume" else m.audio.control = "pause"
        return true
    end if
    if m.pending then return false
    if key = "right" and m.nav.IsInFocusChain()
        openSection(m.nav.itemFocused) : return true
    end if
    if key = "left"
        m.nav.SetFocus(true) : return true
    end if
    if key = "back"
        if m.view = "schedule"
            m.view = "guide" : m.guideChannel = invalid : renderRows() : restoreFocus() : return true
        end if
        if not m.nav.IsInFocusChain()
            m.nav.SetFocus(true) : return true
        end if
    end if
    return false
end function
