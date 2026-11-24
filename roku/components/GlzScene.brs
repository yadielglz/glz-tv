sub init()
    m.top.backgroundURI = ""
    m.top.backgroundColor = "0x080E1CFF"
    m.browser = m.top.findNode("browser")
    m.list = m.top.findNode("channelList")
    m.nav = m.top.findNode("sectionNav")
    m.video = m.top.findNode("player")
    m.video.enableUI = false : m.video.enableTrickPlay = false
    m.playerFocus = m.top.findNode("playerFocus")
    m.audio = m.top.findNode("radioPlayer")
    m.task = m.top.findNode("hubTask")
    m.extra = m.top.findNode("extrasTask")
    m.timer = m.top.findNode("syncTimer")
    m.extraTimer = m.top.findNode("extrasTimer")
    m.osd = m.top.findNode("osd")
    m.osdTimer = m.top.findNode("osdTimer")
    m.miniGuide = m.top.findNode("miniGuide")
    m.miniList = m.top.findNode("miniList")
    m.miniTimer = m.top.findNode("miniTimer")
    m.footer = m.top.findNode("footer")
    m.radioMiniBar = m.top.findNode("radioMiniBar")
    m.radioEqTimer = m.top.findNode("radioEqTimer")
    m.radioEqBars = [
        m.top.findNode("eqBar0")
        m.top.findNode("eqBar1")
        m.top.findNode("eqBar2")
        m.top.findNode("eqBar3")
    ]
    m.radioPillBg = m.top.findNode("radioPillBg")
    m.radioPillText = m.top.findNode("radioPillText")
    m.radioMiniTitle = m.top.findNode("radioMiniTitle")
    m.radioMiniGenre = m.top.findNode("radioMiniGenre")
    m.status = m.top.findNode("status")
    m.pairing = m.top.findNode("pairing")
    m.catalog = m.top.findNode("catalog")
    m.welcomeOverlay = m.top.findNode("welcomeOverlay")
    m.welcomeFocus = m.top.findNode("welcomeFocus")
    m.welcomeStep1Group = m.top.findNode("welcomeStep1Group")
    m.welcomeStep2Group = m.top.findNode("welcomeStep2Group")
    m.welcomeStep3Group = m.top.findNode("welcomeStep3Group")
    m.welcomeStepLabels = [
        m.top.findNode("welcomeStep1")
        m.top.findNode("welcomeStep2")
        m.top.findNode("welcomeStep3")
    ]
    m.welcomePairingCode = m.top.findNode("welcomePairingCode")
    m.welcomePairingStatus = m.top.findNode("welcomePairingStatus")
    m.welcomeReadyDevice = m.top.findNode("welcomeReadyDevice")
    m.welcomeReadyCounts = m.top.findNode("welcomeReadyCounts")
    m.welcomeFooterHint = m.top.findNode("welcomeFooterHint")
    m.appVersion = CreateObject("roAppInfo").GetValue("glz_version")
    m.registry = CreateObject("roRegistrySection", "GlzTV")
    m.stateRegistry = CreateObject("roRegistrySection", "GlzTvState")
    m.firstRunDone = Text(m.stateRegistry.Read("first_run_done")).Trim()
    m.token = ""
    if Text(m.registry.Read("paired")).Trim() = "true"
        m.token = Text(m.registry.Read("token")).Trim()
    end if
    m.welcomeVisible = false : m.welcomeStep = 1 : m.welcomeButtonIndex = 0 : m.welcomeOpenedFromSettings = false
    m.pairingCodeValue = "" : m.pairingErrorMessage = ""
    m.favoriteIds = ParseIdList(m.stateRegistry.Read("favorites"))
    m.recentIds = ParseIdList(m.stateRegistry.Read("recents"))
    m.osdTimeout = 5
    savedOsd = m.registry.Read("osdTimeout").ToInt()
    if savedOsd >= 3 and savedOsd <= 15 then m.osdTimeout = savedOsd
    m.osdTimer.duration = m.osdTimeout
    m.startView = m.registry.Read("startView")
    if m.startView <> "live" and m.startView <> "home" then m.startView = "home"
    m.autoResumePref = m.registry.Read("autoResume")
    m.channels = [] : m.movies = [] : m.rows = [] : m.stations = [] : m.summaries = {} : m.schedule = []
    m.config = {} : m.lastChannelUrl = ""
    m.weatherError = "" : m.weather = invalid : m.weatherLocation = "" : m.weatherAge = 15
    m.extraQueue = [] : m.extraId = 0 : m.extraBusy = false : m.guideChannel = invalid : m.focusedIndex = 0
    m.view = m.startView : m.renderedView = "" : m.guideError = "Loading programme guide…"
    m.version = "" : m.refresh = "" : m.playingUrl = "" : m.playingTitle = "" : m.playingChannel = invalid : m.retriedPlayback = false
    m.radioRow = invalid : m.radioTitle = "" : m.radioFormats = [] : m.radioFormatIndex = 0 : m.radioState = "idle"
    m.radioEqFrame = 0 : m.radioEqRunning = false
    m.optionActions = [] : m.optionFavoriteChannel = invalid
    m.lastError = "" : m.autoStarted = false : m.pending = true
    m.syncsSinceCatalog = 0 : m.queuedForce = false : m.queuedAction = "sync"
    m.views = ["home", "favorites", "live", "guide", "movies", "events", "radio", "weather", "settings"]
    root = CreateObject("roSGNode", "ContentNode")
    for each title in ["Home", "Favorites", "Live TV", "Guide", "Movies", "PPV Events", "Radio", "Weather", "Settings"]
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
    m.miniList.ObserveField("itemSelected", "onMiniSelected")
    m.miniList.ObserveField("itemFocused", "onMiniFocused")
    m.osdTimer.ObserveField("fire", "hideOsd")
    m.miniTimer.ObserveField("fire", "hideMiniGuide")
    m.radioEqTimer.ObserveField("fire", "tickRadioEq")
    m.video.ObserveField("state", "videoStateChanged")
    m.audio.ObserveField("state", "audioStateChanged")
    if ShouldShowFirstRun(m.firstRunDone, m.token)
        openWelcomeFlow(false)
    else
        m.top.SetFocus(true)
    end if
    startSync(true)
end sub

sub openWelcomeFlow(fromSettings = false as Boolean)
    closePlayer()
    m.welcomeOpenedFromSettings = fromSettings
    m.welcomeVisible = true
    m.welcomeOverlay.visible = true
    showWelcomeStep(1)
    m.welcomeFocus.SetFocus(true)
end sub

sub showWelcomeStep(stepIndex as Integer)
    if stepIndex < 1 then stepIndex = 1
    if stepIndex > 3 then stepIndex = 3
    m.welcomeStep = stepIndex
    m.welcomeButtonIndex = 0
    m.welcomeStep1Group.visible = (stepIndex = 1)
    m.welcomeStep2Group.visible = (stepIndex = 2)
    m.welcomeStep3Group.visible = (stepIndex = 3)
    for i = 0 to m.welcomeStepLabels.Count() - 1
        lbl = m.welcomeStepLabels[i]
        if lbl <> invalid
            if i + 1 = stepIndex
                lbl.color = "0x38BDF8FF"
            else if i + 1 < stepIndex
                lbl.color = "0xCBD5E1FF"
            else
                lbl.color = "0x64748BFF"
            end if
        end if
    end for
    if stepIndex = 1
        if m.welcomeOpenedFromSettings
            m.welcomeFooterHint.text = "Left / Right  Choose button   •   OK  Select   •   Back  Close"
        else
            m.welcomeFooterHint.text = "Left / Right  Choose button   •   OK  Select"
        end if
    else if stepIndex = 2
        m.welcomeFooterHint.text = "Left / Right  Choose button   •   OK  Select   •   Back  Welcome"
        updateWelcomePairingView()
        if m.token = "" and m.pairingCodeValue = "" then startSync(true)
    else
        m.welcomeFooterHint.text = "OK  Start Watching   •   Welcome to GLZ TV"
        updateWelcomeReadyView()
    end if
    updateWelcomeButtonFocus()
    if m.welcomeVisible then m.welcomeFocus.SetFocus(true)
end sub

sub setWelcomeButtonStyle(bgId as String, labelId as String, focused as Boolean)
    bg = m.top.findNode(bgId)
    lbl = m.top.findNode(labelId)
    if bg = invalid or lbl = invalid then return
    if focused
        bg.color = "0x38BDF8FF"
        lbl.color = "0x080E1CFF"
    else
        bg.color = "0x1E293BFF"
        lbl.color = "0xE2E8F0FF"
    end if
end sub

sub updateWelcomeButtonFocus()
    if m.welcomeStep = 1
        setWelcomeButtonStyle("welcomeBtn1PrimaryBg", "welcomeBtn1PrimaryLabel", m.welcomeButtonIndex = 0)
        setWelcomeButtonStyle("welcomeBtn1SecondaryBg", "welcomeBtn1SecondaryLabel", m.welcomeButtonIndex = 1)
    else if m.welcomeStep = 2
        setWelcomeButtonStyle("welcomeBtn2PrimaryBg", "welcomeBtn2PrimaryLabel", m.welcomeButtonIndex = 0)
        setWelcomeButtonStyle("welcomeBtn2SecondaryBg", "welcomeBtn2SecondaryLabel", m.welcomeButtonIndex = 1)
    else
        setWelcomeButtonStyle("welcomeBtn3PrimaryBg", "welcomeBtn3PrimaryLabel", true)
    end if
end sub

sub updateWelcomePairingView()
    info = FirstRunPairingStatus(m.pairingCodeValue, m.pairingErrorMessage, m.token <> "", m.config.deviceName)
    m.welcomePairingCode.text = info.codeText
    m.welcomePairingStatus.text = info.statusText
end sub

sub updateWelcomeReadyView()
    info = FirstRunReadySummary(m.config.deviceName, m.channels.Count(), m.stations.Count(), m.token <> "")
    m.welcomeReadyDevice.text = info.deviceLine
    m.welcomeReadyCounts.text = info.countsLine
end sub

sub markFirstRunDone()
    m.firstRunDone = "1"
    m.stateRegistry.Write("first_run_done", "1")
    m.stateRegistry.Flush()
end sub

sub closeWelcomeFlow(goHome = true as Boolean)
    m.welcomeVisible = false
    m.welcomeOverlay.visible = false
    if not m.pending
        if goHome
            openSectionByName("home")
        else
            restoreFocus()
        end if
    else
        m.top.SetFocus(true)
    end if
end sub

function handleWelcomeKey(key as String) as Boolean
    if m.welcomeStep = 1
        if key = "left" or key = "right"
            if m.welcomeButtonIndex = 0 then m.welcomeButtonIndex = 1 else m.welcomeButtonIndex = 0
            updateWelcomeButtonFocus()
            return true
        else if LCase(key) = "ok"
            if m.welcomeButtonIndex = 0
                showWelcomeStep(2)
            else
                markFirstRunDone()
                closeWelcomeFlow(not m.welcomeOpenedFromSettings)
            end if
            return true
        else if key = "back"
            if m.welcomeOpenedFromSettings then closeWelcomeFlow(false)
            return true
        end if
        return true
    else if m.welcomeStep = 2
        if key = "left" or key = "right"
            if m.welcomeButtonIndex = 0 then m.welcomeButtonIndex = 1 else m.welcomeButtonIndex = 0
            updateWelcomeButtonFocus()
            return true
        else if LCase(key) = "ok"
            if m.welcomeButtonIndex = 0
                m.token = "" : m.pairingCodeValue = "" : m.pairingErrorMessage = ""
                m.version = "" : m.autoStarted = false
                updateWelcomePairingView()
                startSync(true, "repair")
            else
                if m.token <> ""
                    showWelcomeStep(3)
                else
                    markFirstRunDone()
                    closeWelcomeFlow(not m.welcomeOpenedFromSettings)
                end if
            end if
            return true
        else if key = "back"
            showWelcomeStep(1)
            return true
        end if
        return true
    else
        if LCase(key) = "ok" or key = "back"
            markFirstRunDone()
            closeWelcomeFlow(true)
            return true
        end if
        return true
    end if
end function

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
        m.task.activityType = "channel"
        if m.playingChannel <> invalid and m.playingChannel.isMovie = true then m.task.activityType = "movie"
        m.task.activityLabel = m.playingTitle
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

sub updateHeaderBar()
    now = CreateObject("roDateTime").AsSeconds()
    clock = ClockLabel(now)
    meta = clock
    welcome = GuestWelcomeSummary(m.config, m.weather)
    if welcome.weatherPill <> ""
        meta = welcome.weatherPill + "   •   " + clock
    end if
    m.top.findNode("headerMeta").text = meta
    devLabel = Text(m.config.deviceName, "Roku TV")
    if Text(m.config.guestName) <> "" then devLabel += " · " + Text(m.config.guestName)
    m.top.findNode("deviceName").text = devLabel
end sub

sub updateHomeHero()
    welcome = GuestWelcomeSummary(m.config, m.weather)
    backdrop = m.top.findNode("homeHeroBackdrop")
    scrim = m.top.findNode("homeHeroScrim")
    if IsWebUrl(welcome.heroImage)
        backdrop.uri = welcome.heroImage
        backdrop.visible = true
        scrim.visible = true
    else
        backdrop.uri = ""
        backdrop.visible = false
        scrim.visible = false
    end if
    logo = welcome.logo
    heroLogo = m.top.findNode("homeHeroLogo")
    heroTitle = m.top.findNode("homeHeroTitle")
    heroSub = m.top.findNode("homeHeroSub")
    heroBody = m.top.findNode("homeHeroBody")
    if IsWebUrl(logo)
        heroLogo.uri = logo
        heroLogo.visible = true
        heroTitle.translation = [152, 14]
        heroTitle.width = 510
        heroBody.translation = [152, 52]
        heroBody.width = 804
    else
        heroLogo.uri = ""
        heroLogo.visible = false
        heroTitle.translation = [24, 14]
        heroTitle.width = 640
        heroBody.translation = [24, 52]
        heroBody.width = 932
    end if
    heroTitle.text = welcome.headline
    pill = welcome.weatherPill
    if pill = "" then pill = m.channels.Count().ToStr() + " Live  •  " + m.movies.Count().ToStr() + " Movies"
    heroSub.text = pill
    body = welcome.body
    if body = "Select a destination on the left or choose a quick action to start watching."
        body = "Stream live channels, on-demand movies, PPV sports events, and local radio below. Press D-Pad Right during live playback to open the Mini-Guide."
    end if
    heroBody.text = body
end sub

sub onHubResult()
    result = m.task.result
    if result = invalid then return
    if result.status = "pending"
        closePlayer() : stopRadio(false)
        m.token = "" : m.pairingCodeValue = Text(result.code) : m.pairingErrorMessage = ""
        m.pending = true : m.channels = [] : m.movies = [] : m.rows = [] : m.stations = [] : m.summaries = {} : m.schedule = [] : m.weather = invalid
        m.extraQueue = [] : m.extraBusy = false : m.extraId++ : m.extra.control = "STOP"
        m.catalog.visible = false : m.pairing.visible = true
        m.top.findNode("pairingCode").text = result.code
        m.status.text = "Ready to pair. Each Roku gets its own channel assignment in GlzHub."
        if m.welcomeVisible
            updateWelcomePairingView()
            m.welcomeFocus.SetFocus(true)
        else
            m.top.SetFocus(true)
        end if
        scheduleSync(5)
    else if result.status = "ready"
        m.token = Text(m.registry.Read("token")).Trim()
        if m.token = "" then m.token = "paired"
        m.pairingCodeValue = "" : m.pairingErrorMessage = ""
        m.pending = false : m.pairing.visible = false : m.catalog.visible = true
        config = result.config
        m.config = config
        m.lastChannelUrl = Text(result.lastChannel)
        if m.registry.Read("osdTimeout") = "" and type(config.osdTimeoutSeconds) = "Integer"
            if config.osdTimeoutSeconds >= 3 and config.osdTimeoutSeconds <= 15
                m.osdTimeout = config.osdTimeoutSeconds
                m.osdTimer.duration = m.osdTimeout
            end if
        end if
        m.movies = ParseMovies(config.movies, config.requestHeaders)
        m.version = result.version : m.refresh = result.refresh
        if result.channels <> invalid
            m.channels = result.channels : m.syncsSinceCatalog = 0
            foundPlaying = m.playingUrl = ""
            if m.playingChannel <> invalid and m.playingChannel.isMovie = true then foundPlaying = true
            for each channel in m.channels
                if channel.url = m.playingUrl then foundPlaying = true
            end for
            if not foundPlaying and m.video.visible then closePlayer()
            queueExtra("summary") : queueExtra("radio")
            if m.view = "schedule" and m.guideChannel <> invalid then queueExtra("guide", m.guideChannel.id)
        else
            if m.summaries.Count() = 0 or m.guideError <> "" then queueExtra("summary")
            if m.stations.Count() = 0 then queueExtra("radio")
        end if
        location = Text(config.weatherLocation)
        if location <> m.weatherLocation or m.weatherAge >= 15
            if location <> m.weatherLocation then m.weather = invalid
            m.weatherError = "" : m.weatherLocation = location : m.weatherAge = 0
            queueExtra("weather")
        end if
        welcome = GuestWelcomeSummary(m.config, m.weather)
        m.status.text = welcome.headline
        if result.omitted <> invalid
            if result.omitted > 0 then m.status.text = "Showing the first 1,500 channels. Assign a smaller lineup in GlzHub."
        end if
        updateHeaderBar()
        updateRadioMiniBar()
        renderRows()
        if m.welcomeVisible
            if m.welcomeStep < 3
                showWelcomeStep(3)
            else
                updateWelcomeReadyView()
            end if
            m.welcomeFocus.SetFocus(true)
        else if not m.autoStarted
            m.autoStarted = true
            if m.registry.Read("startView") = "" and LCase(Text(config.startDestination)) = "live_tv"
                openSectionByName("live")
            else
                m.list.SetFocus(true)
            end if
            shouldResume = config.resumeLastChannel = true
            if m.autoResumePref = "true" then shouldResume = true
            if m.autoResumePref = "false" then shouldResume = false
            if shouldResume and m.lastChannelUrl <> ""
                for each channel in m.channels
                    if channel.url = m.lastChannelUrl
                        playItem(channel) : exit for
                    end if
                end for
            end if
        end if
        scheduleSync(60)
    else
        m.pairingErrorMessage = Text(result.message, "Network error. Retrying shortly.")
        m.status.text = m.pairingErrorMessage
        if m.welcomeVisible and m.welcomeStep = 2 then updateWelcomePairingView()
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
    if not m.extraBusy then m.extraTimer.control = "start"
end sub

sub runNextExtra()
    if m.pending or m.extraBusy or m.extraQueue.Count() = 0 then return
    if m.extra.state = "run"
        m.extraTimer.control = "start" : return
    end if
    job = m.extraQueue.Shift() : m.extraId++ : m.extraBusy = true
    m.extra.control = "STOP"
    m.extra.mode = job.mode : m.extra.channel = job.channel
    m.extra.location = m.weatherLocation : m.extra.requestId = m.extraId
    m.extra.control = "RUN"
end sub

sub onExtraResult()
    result = m.extra.result
    if result = invalid or m.pending then return
    if result.requestId <> m.extraId then return
    m.extraBusy = false
    if Text(result.error) <> ""
        if result.mode = "summary" or result.mode = "guide"
            m.guideError = result.error
            renderRows()
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
                    if type(station) = "roAssociativeArray"
                        streamUrl = Text(station.streamUrl).Trim()
                        if IsWebUrl(streamUrl) and Instr(1, streamUrl, "|") = 0
                            m.stations.Push({ title: Text(station.name), number: Text(station.code), url: streamUrl, logo: Text(station.logoUrl), group: Text(station.genre), headers: CopyHeaders(station.requestHeaders), id: Text(station.code) })
                        end if
                    end if
                end for
            end if
            if m.welcomeVisible and m.welcomeStep = 3 then updateWelcomeReadyView()
        else if result.mode = "weather"
            if result.location = m.weatherLocation
                m.weather = data : m.weatherError = ""
                updateHeaderBar()
                if m.view = "home" then updateHomeHero()
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
            if m.osd.visible then refreshOsdContent()
            if m.miniGuide.visible then renderMiniGuide()
        end if
        renderRows()
    end if
    if m.extraQueue.Count() > 0 then m.extraTimer.control = "start"
end sub

sub selectSection()
    openSection(m.nav.itemSelected)
end sub

sub openSection(index as Integer)
    if index < 0 or index >= m.views.Count() then return
    m.view = m.views[index] : m.guideChannel = invalid : m.focusedIndex = 0
    m.nav.jumpToItem = index
    renderRows()
    if m.view = "radio" or (m.view = "home" and m.stations.Count() = 0) then queueExtra("radio")
    if m.view = "guide" or m.view = "home" or m.view = "favorites" then queueExtra("summary")
    if m.view = "weather" and m.weather = invalid then queueExtra("weather")
    if m.view <> "weather" and m.rows.Count() > 0 then m.list.SetFocus(true)
end sub

sub openSectionByName(name as String)
    for i = 0 to m.views.Count() - 1
        if m.views[i] = name
            openSection(i) : return
        end if
    end for
end sub

sub saveFavorites()
    m.stateRegistry.Write("favorites", SerializeIdList(m.favoriteIds))
    m.stateRegistry.Flush()
end sub

sub saveRecents()
    m.stateRegistry.Write("recents", SerializeIdList(m.recentIds))
    m.stateRegistry.Flush()
end sub

sub trackRecentChannel(channel as Dynamic)
    if type(channel) <> "roAssociativeArray" or channel.isMovie = true then return
    key = ChannelKey(channel)
    if key = "" then return
    m.recentIds = RecordRecentChannel(m.recentIds, key, 10)
    saveRecents()
end sub

function channelTitleWithStar(channel as Dynamic, baseTitle = "" as String) as String
    label = baseTitle
    if label = "" and type(channel) = "roAssociativeArray" then label = Text(channel.title)
    if type(channel) = "roAssociativeArray" and channel.isMovie <> true
        if IsFavoriteChannel(m.favoriteIds, ChannelKey(channel)) then return "★ " + label
    end if
    return label
end function

function focusedLiveChannel() as Dynamic
    if m.pending or m.video.visible then return invalid
    if m.view = "schedule" then return m.guideChannel
    if m.view = "movies" or m.view = "radio" or m.view = "weather" or m.view = "settings" then return invalid
    index = m.list.itemFocused
    if index < 0 then index = m.focusedIndex
    if index < 0 or index >= m.rows.Count() then return invalid
    row = m.rows[index]
    if type(row) <> "roAssociativeArray" then return invalid
    if m.view = "home"
        if row.kind = "play" and type(row.target) = "roAssociativeArray" and row.target.isMovie <> true
            return row.target
        end if
        return invalid
    end if
    if row.isMovie = true then return invalid
    return row
end function

sub toggleFavoriteForChannel(channel as Dynamic)
    if type(channel) <> "roAssociativeArray" or channel.isMovie = true then return
    key = ChannelKey(channel)
    if key = "" then return
    wasFav = IsFavoriteChannel(m.favoriteIds, key)
    m.favoriteIds = ToggleFavoriteChannel(m.favoriteIds, key)
    saveFavorites()
    if wasFav
        m.status.text = "Removed " + Text(channel.title, "channel") + " from Favorites"
    else
        m.status.text = "Added ★ " + Text(channel.title, "channel") + " to Favorites"
    end if
    renderRows()
    if m.miniGuide.visible then renderMiniGuide()
    restoreFocus()
end sub

function buildHomeRows(now as Integer) as Object
    rows = []
    seenUrls = {}
    ' 1. Continue Watching (last watched live channel)
    if m.lastChannelUrl <> ""
        for each channel in m.channels
            if channel.url = m.lastChannelUrl
                prog = ProgrammeLine(CurrentProgramme(ChannelSummary(m.summaries, channel.id), now))
                rows.Push({ kind: "play", target: channel, number: "LAST", title: "Continue · " + channelTitleWithStar(channel), description: prog, logo: Text(channel.logo) })
                seenUrls[channel.url] = true
                exit for
            end if
        end for
    end if
    ' 2. Favorites (when populated, up to 6)
    favChannels = ResolveChannelsByKeys(m.channels, m.favoriteIds, 6)
    for each channel in favChannels
        if not seenUrls.DoesExist(channel.url)
            prog = ProgrammeLine(CurrentProgramme(ChannelSummary(m.summaries, channel.id), now))
            rows.Push({ kind: "play", target: channel, number: "★ FAV", title: "★ " + channel.title, description: "Favorites · " + prog, logo: Text(channel.logo) })
            seenUrls[channel.url] = true
        end if
    end for
    ' 3. Recent Channels (when populated, up to 5)
    recentChannels = ResolveChannelsByKeys(m.channels, m.recentIds, 6)
    recentsAdded = 0
    for each channel in recentChannels
        if not seenUrls.DoesExist(channel.url) and recentsAdded < 5
            prog = ProgrammeLine(CurrentProgramme(ChannelSummary(m.summaries, channel.id), now))
            rows.Push({ kind: "play", target: channel, number: "RECENT", title: channelTitleWithStar(channel), description: "Recent Channels · " + prog, logo: Text(channel.logo) })
            seenUrls[channel.url] = true
            recentsAdded++
        end if
    end for
    ' 4. Active Live Sports & PPV Events (up to 4)
    eventsAdded = 0
    for each channel in m.channels
        if Text(channel.eventTitle) <> "" and eventsAdded < 4
            window = channel.league + " · " + ClockLabel(channel.eventStart) + " – " + ClockLabel(channel.eventEnd)
            rows.Push({ kind: "play", target: channel, number: "LIVE", title: channelTitleWithStar(channel, channel.eventTitle), description: window, logo: Text(channel.logo) })
            seenUrls[channel.url] = true
            eventsAdded++
        end if
    end for
    ' 5. On Now: Channels currently airing a live EPG programme (up to 6)
    onNowAdded = 0
    for each channel in m.channels
        if not seenUrls.DoesExist(channel.url) and Text(channel.eventTitle) = "" and onNowAdded < 6
            current = CurrentProgramme(ChannelSummary(m.summaries, channel.id), now)
            if current <> invalid
                badge = Text(channel.number, "ON")
                rows.Push({ kind: "play", target: channel, number: badge, title: channelTitleWithStar(channel), description: ProgrammeLine(current), logo: Text(channel.logo) })
                seenUrls[channel.url] = true
                onNowAdded++
            end if
        end if
    end for
    ' 6. Featured Movies On Demand (up to 6)
    maxMovies = m.movies.Count() - 1
    if maxMovies > 5 then maxMovies = 5
    for i = 0 to maxMovies
        mv = m.movies[i]
        rows.Push({ kind: "movie", target: mv, number: "FILM", title: mv.title, description: mv.group, logo: Text(mv.logo) })
    end for
    ' 7. Top Live Channels fallback (if EPG hasn't populated 6 channels yet)
    for each channel in m.channels
        if not seenUrls.DoesExist(channel.url) and Text(channel.eventTitle) = "" and onNowAdded < 6
            badge = Text(channel.number, "TV")
            prog = ProgrammeLine(CurrentProgramme(ChannelSummary(m.summaries, channel.id), now))
            rows.Push({ kind: "play", target: channel, number: badge, title: channelTitleWithStar(channel), description: prog, logo: Text(channel.logo) })
            seenUrls[channel.url] = true
            onNowAdded++
        end if
    end for
    ' 8. Featured Radio Picks (up to 3)
    maxRadio = m.stations.Count() - 1
    if maxRadio > 2 then maxRadio = 2
    for i = 0 to maxRadio
        st = m.stations[i]
        rows.Push({ kind: "radio", target: st, number: "RADIO", title: st.title, description: "Live Radio · " + Text(st.group), logo: Text(st.logo) })
    end for
    return rows
end function

function buildSettingsRows() as Object
    startLabel = "Home Screen"
    if m.startView = "live" then startLabel = "Live TV"
    resumeEnabled = m.config.resumeLastChannel = true
    if m.autoResumePref = "true" then resumeEnabled = true
    if m.autoResumePref = "false" then resumeEnabled = false
    resumeLabel = "Off"
    if resumeEnabled then resumeLabel = "On"
    radioLabel = "No station playing"
    if m.radioTitle <> "" then radioLabel = "Playing: " + m.radioTitle + " (OK to stop)"
    return [
        { setting: "sync", number: "SYNC", title: "Sync & Refresh Content Now", description: "Reload channels, movies, EPG guide, radio & weather from GlzHub" }
        { setting: "osd", number: m.osdTimeout.ToStr() + "s", title: "Player OSD Banner Timeout", description: "Display duration: " + m.osdTimeout.ToStr() + " seconds · Press OK to cycle (3s / 5s / 8s / 12s)" }
        { setting: "start", number: "BOOT", title: "Default Startup Screen", description: "Opens to: " + startLabel + " · Press OK to toggle" }
        { setting: "resume", number: resumeLabel, title: "Auto-Resume Last Channel", description: "Automatically tune last watched channel on launch: " + resumeLabel }
        { setting: "radio", number: "AUDIO", title: "Background Radio Control", description: radioLabel }
        { setting: "welcome", number: "SETUP", title: "Welcome & Setup Wizard", description: "Re-open the 3-step GLZ TV welcome and GlzHub pairing guide" }
        { setting: "repair", number: "PAIR", title: "Re-pair Roku with GlzHub", description: "Unlink current device token and generate a new pairing code" }
    ]
end function

sub applyViewLayout()
    isHome = m.view = "home"
    isWeather = m.view = "weather"
    m.top.findNode("homeHero").visible = isHome
    m.list.visible = not isWeather
    m.top.findNode("detailPanel").visible = not isWeather
    m.top.findNode("weatherText").visible = isWeather
    titleNode = m.top.findNode("sectionTitle")
    detailPanel = m.top.findNode("detailPanel")
    detailBg = m.top.findNode("detailBg")
    detailGroup = m.top.findNode("detailGroup")
    if isHome
        updateHomeHero()
        titleNode.translation = [264, 260]
        m.list.translation = [264, 300]
        m.list.numRows = 4
        detailPanel.translation = [870, 260]
        detailBg.height = 392
        detailGroup.height = 122
    else
        titleNode.translation = [264, 108]
        m.list.translation = [264, 152]
        m.list.numRows = 6
        detailPanel.translation = [870, 106]
        detailBg.height = 546
        detailGroup.height = 274
    end if
end sub

sub renderRows()
    sameView = (m.renderedView = m.view)
    m.renderedView = m.view
    index = m.focusedIndex
    if sameView and m.browser.visible and m.list.itemFocused >= 0 then index = m.list.itemFocused
    if index < 0 then index = 0
    m.rows = []
    applyViewLayout()
    isWeather = m.view = "weather"
    titles = {
        home: "On Now & Featured · OK Plays Immediately"
        favorites: "Favorites · Press * to add or remove channels"
        live: "Live TV · Up/Down or FF/RW zaps while playing"
        guide: "Programme Guide · OK opens 24-hour channel schedule"
        movies: "Movies On Demand · Instant Cinema"
        events: "PPV / Live Sports Events"
        radio: "Live Radio · Audio continues while browsing"
        weather: "Weather · Forecast location from GlzHub"
        settings: "Settings & Device Diagnostics"
        schedule: "Channel Schedule · Next 24 Hours"
    }
    m.top.findNode("sectionTitle").text = Text(titles[m.view], "Glz TV")
    if isWeather
        m.top.findNode("weatherText").text = WeatherLines(m.weather)
        if m.weather = invalid and m.weatherError <> "" then m.top.findNode("weatherText").text = m.weatherError
        m.top.findNode("channelCount").text = "Forecast updates every 15 minutes · Open-Meteo.com"
        updateFooter() : return
    end if
    now = CreateObject("roDateTime").AsSeconds()
    if m.view = "home"
        m.rows = buildHomeRows(now)
    else if m.view = "favorites"
        m.rows = ResolveChannelsByKeys(m.channels, m.favoriteIds)
    else if m.view = "movies"
        m.rows = m.movies
    else if m.view = "settings"
        m.rows = buildSettingsRows()
    else if m.view = "schedule"
        m.rows = m.schedule
    else if m.view = "radio"
        m.rows = m.stations
    else
        for each channel in m.channels
            if m.view <> "events" or Text(channel.eventTitle) <> "" then m.rows.Push(channel)
        end for
    end if
    root = CreateObject("roSGNode", "ContentNode")
    for i = 0 to m.rows.Count() - 1
        row = m.rows[i] : item = root.CreateChild("ContentNode")
        rowLogo = Text(row.logo)
        if IsWebUrl(rowLogo) then item.hdPosterUrl = rowLogo
        if m.view = "schedule"
            item.title = Text(row.title)
            item.shortDescriptionLine1 = ""
            item.description = ClockLabel(row.start) + " – " + ClockLabel(row.end)
        else if m.view = "home" or m.view = "settings" or m.view = "movies"
            item.title = Text(row.title)
            item.shortDescriptionLine1 = Text(row.number, (i + 1).ToStr())
            if m.view = "movies" then item.description = Text(row.group) else item.description = Text(row.description)
        else
            item.title = row.title : item.shortDescriptionLine1 = Text(row.number)
            if item.shortDescriptionLine1 = "" then item.shortDescriptionLine1 = (i + 1).ToStr()
            if m.view = "events"
                item.title = channelTitleWithStar(row, row.eventTitle)
                item.description = row.league + " · " + ClockLabel(row.eventStart) + " – " + ClockLabel(row.eventEnd)
            else if m.view = "radio"
                item.description = row.group
            else
                item.title = channelTitleWithStar(row)
                item.description = ProgrammeLine(CurrentProgramme(ChannelSummary(m.summaries, row.id), now))
            end if
        end if
    end for
    m.list.content = root
    if index >= m.rows.Count() then index = 0
    m.focusedIndex = index
    if m.rows.Count() > 0 then m.list.jumpToItem = index
    m.top.findNode("channelCount").text = m.rows.Count().ToStr() + " items · * Options"
    if m.rows.Count() = 0
        if m.view = "schedule" or m.view = "guide" then m.status.text = "No programme data yet. " + m.guideError
        if m.view = "favorites" then m.status.text = "No favorites yet. Press * on any channel to add ★."
        if m.view = "movies" then m.status.text = "No movies published in GlzHub yet."
        if m.view = "events" then m.status.text = "No active PPV events in the current lineup."
        if m.view = "radio" then m.status.text = "No radio stations loaded. Manage stations in GlzHub or choose Refresh."
    end if
    if m.view = "guide" or m.view = "schedule"
        if m.guideError <> ""
            m.status.text = m.guideError
        else
            m.status.text = "Guide loaded · Select a channel or programme for details"
        end if
    end if
    showDetailsForIndex(index) : updateFooter()
end sub

function radioStatusBadge(row as Dynamic) as String
    if m.radioRow = invalid or row = invalid then return ""
    if Text(row.url) <> Text(m.radioRow.url) then return ""
    if m.radioState = "playing" then return "LIVE"
    if m.radioState = "buffering" then return "BUFFERING"
    if m.radioState = "paused" then return "PAUSED"
    return ""
end function

sub showDetails()
    index = m.list.itemFocused
    if index < 0 then index = m.focusedIndex
    if m.browser.visible and m.list.itemFocused >= 0 then m.focusedIndex = m.list.itemFocused
    showDetailsForIndex(index)
end sub

sub showDetailsForIndex(index as Integer)
    m.top.findNode("detailMeta").text = ""
    if index < 0 or index >= m.rows.Count()
        if m.view = "movies"
            m.top.findNode("detailTitle").text = "No Movies Available"
            m.top.findNode("detailGroup").text = "Add and publish movies in GlzHub Movie Studio to stream them on demand here."
        else if m.view = "favorites"
            m.top.findNode("detailTitle").text = "No Favorites Yet"
            m.top.findNode("detailGroup").text = "Highlight any live channel in Home, Live TV, or Guide and press * to add it to Favorites."
        else
            m.top.findNode("detailTitle").text = "Nothing to show yet"
            m.top.findNode("detailGroup").text = "Choose a section on the left or press * to Refresh."
        end if
        m.top.findNode("channelLogo").uri = "" : return
    end if
    row = m.rows[index]
    if m.view = "home"
        if row.kind = "movie" and row.target <> invalid
            mv = row.target
            logo = Text(mv.logo)
            if not IsWebUrl(logo) then logo = ""
            m.top.findNode("channelLogo").uri = logo
            m.top.findNode("detailTitle").text = mv.title
            m.top.findNode("detailMeta").text = mv.group
            m.top.findNode("detailGroup").text = Text(mv.description, "Press OK to watch this movie on demand.")
            return
        else if row.kind = "radio" and row.target <> invalid
            st = row.target
            logo = Text(st.logo)
            if not IsWebUrl(logo) then logo = ""
            m.top.findNode("channelLogo").uri = logo
            m.top.findNode("detailTitle").text = st.title
            badge = radioStatusBadge(st)
            meta = "Radio · " + Text(st.group)
            if badge <> "" then meta = badge + " · " + meta
            m.top.findNode("detailMeta").text = meta
            if badge <> ""
                m.top.findNode("detailGroup").text = "Status: " + badge + Chr(10) + "Press Play/Pause to pause or reconnect, or * to stop radio."
            else
                m.top.findNode("detailGroup").text = "Press OK to listen. Audio continues while browsing."
            end if
            return
        else if row.target <> invalid
            row = row.target
        end if
    else if m.view = "settings"
        m.top.findNode("channelLogo").uri = ""
        m.top.findNode("detailTitle").text = Text(m.config.deviceName, "Roku TV")
        m.top.findNode("detailMeta").text = "Version " + m.appVersion + " · Hub v" + Text(m.version, "1")
        epgState = "Active (" + m.summaries.Count().ToStr() + " channels mapped)"
        if m.guideError <> "" then epgState = m.guideError
        diag = "Live Channels: " + m.channels.Count().ToStr() + Chr(10)
        diag += "Favorites: " + m.favoriteIds.Count().ToStr() + " · Recents: " + m.recentIds.Count().ToStr() + Chr(10)
        diag += "Movies On Demand: " + m.movies.Count().ToStr() + Chr(10)
        diag += "Radio Stations: " + m.stations.Count().ToStr() + Chr(10)
        diag += "Weather City: " + Text(m.weatherLocation, "San Juan") + Chr(10)
        diag += "EPG Guide: " + epgState
        m.top.findNode("detailGroup").text = diag
        return
    else if m.view = "movies"
        logo = Text(row.logo)
        if not IsWebUrl(logo) then logo = ""
        m.top.findNode("channelLogo").uri = logo
        m.top.findNode("detailTitle").text = row.title
        m.top.findNode("detailMeta").text = Text(row.group)
        synopsis = Text(row.description, "Streaming on demand from GlzHub.")
        m.top.findNode("detailGroup").text = synopsis + Chr(10) + Chr(10) + "Press OK to play movie."
        return
    end if
    m.top.findNode("detailTitle").text = row.title
    logo = Text(row.logo)
    if not IsWebUrl(logo) then logo = ""
    m.top.findNode("channelLogo").uri = logo
    if m.view = "schedule"
        m.top.findNode("detailMeta").text = ClockLabel(row.start) + " – " + ClockLabel(row.end)
        m.top.findNode("detailGroup").text = Text(row.description, "Press OK to watch this channel live.")
    else if m.view = "events"
        m.top.findNode("detailTitle").text = channelTitleWithStar(row, row.eventTitle)
        m.top.findNode("detailMeta").text = row.league + " · " + ClockLabel(row.eventStart) + " – " + ClockLabel(row.eventEnd)
        m.top.findNode("detailGroup").text = "Press OK to watch live · SPORTS PPV" + Chr(10) + "Press * to toggle Favorite."
    else if m.view = "radio"
        badge = radioStatusBadge(row)
        meta = Text(row.group)
        if badge <> ""
            if meta <> "" then meta = badge + " · " + meta else meta = badge
        end if
        m.top.findNode("detailMeta").text = meta
        if badge <> ""
            m.top.findNode("detailGroup").text = "Status: " + badge + Chr(10) + "Press Play/Pause to pause or reconnect, or * to stop radio."
        else
            m.top.findNode("detailGroup").text = "Press OK to listen. Audio continues while browsing other sections."
        end if
    else
        m.top.findNode("detailTitle").text = channelTitleWithStar(row)
        now = CreateObject("roDateTime").AsSeconds()
        summary = ChannelSummary(m.summaries, row.id)
        current = CurrentProgramme(summary, now)
        upcoming = NextProgramme(summary, now)
        meta = Text(row.group)
        if IsFavoriteChannel(m.favoriteIds, ChannelKey(row))
            if meta <> "" then meta = "★ Favorite · " + meta else meta = "★ Favorite"
        end if
        m.top.findNode("detailMeta").text = meta
        description = "Now: " + ProgrammeLine(current)
        if current = invalid and m.guideError <> "" then description = m.guideError
        if upcoming <> invalid then description += Chr(10) + "Next: " + ProgrammeLine(upcoming)
        if Text(row.eventTitle) <> "" then description = row.eventTitle + Chr(10) + row.league + " · " + ClockLabel(row.eventStart)
        m.top.findNode("detailGroup").text = description
    end if
end sub

sub handleSettingSelection(row as Object)
    key = Text(row.setting)
    if key = "sync"
        m.status.text = "Refreshing all content from GlzHub…"
        startSync(true) : queueExtra("summary") : queueExtra("radio") : queueExtra("weather")
    else if key = "osd"
        if m.osdTimeout = 3 then m.osdTimeout = 5 else if m.osdTimeout = 5 then m.osdTimeout = 8 else if m.osdTimeout = 8 then m.osdTimeout = 12 else m.osdTimeout = 3
        m.osdTimer.duration = m.osdTimeout
        m.registry.Write("osdTimeout", m.osdTimeout.ToStr()) : m.registry.Flush()
        renderRows()
    else if key = "start"
        if m.startView = "home" then m.startView = "live" else m.startView = "home"
        m.registry.Write("startView", m.startView) : m.registry.Flush()
        renderRows()
    else if key = "resume"
        current = m.config.resumeLastChannel = true
        if m.autoResumePref = "true" then current = true
        if m.autoResumePref = "false" then current = false
        if current then m.autoResumePref = "false" else m.autoResumePref = "true"
        m.registry.Write("autoResume", m.autoResumePref) : m.registry.Flush()
        renderRows()
    else if key = "radio"
        if m.radioTitle <> "" then stopRadio() : renderRows() else openSectionByName("radio")
    else if key = "welcome"
        openWelcomeFlow(true)
    else if key = "repair"
        promptRepair()
    end if
end sub

sub selectRow()
    if m.video.visible then return
    index = m.list.itemSelected
    if index < 0 or index >= m.rows.Count() then return
    m.focusedIndex = index
    row = m.rows[index]
    if m.view = "home"
        if row.kind = "radio" and row.target <> invalid
            playRadio(row.target)
        else if row.target <> invalid
            playItem(row.target)
        end if
    else if m.view = "settings"
        handleSettingSelection(row)
    else if m.view = "guide"
        m.guideChannel = row : m.schedule = [] : m.view = "schedule" : m.focusedIndex = 0
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
    stopRadio(false)
    hideMiniGuide()
    for i = 0 to m.rows.Count() - 1
        if Text(m.rows[i].url) = Text(row.url)
            m.focusedIndex = i : exit for
        end if
    end for
    content = PlayerContent(row, StreamFormat(row.url))
    content.live = not (row.isMovie = true)
    m.playingChannel = row : m.retriedPlayback = false
    m.playingUrl = row.url : m.playingTitle = row.title : m.lastError = ""
    if row.isMovie <> true
        m.lastChannelUrl = row.url
        trackRecentChannel(row)
    end if
    m.browser.visible = false : m.video.visible = true
    updateRadioMiniBar()
    m.video.content = content : m.playerFocus.SetFocus(true) : m.video.control = "play"
    showOsd()
    startSync(false)
end sub

sub stepChannel(delta as Integer)
    if not m.video.visible or m.playingChannel = invalid or m.playingChannel.isMovie = true then return
    if m.channels.Count() <= 1
        showOsd() : return
    end if
    nextIndex = AdjacentChannelIndex(m.channels, m.playingUrl, delta)
    if nextIndex < 0 then return
    playItem(m.channels[nextIndex])
end sub

sub refreshOsdContent()
    if m.playingChannel = invalid then return
    now = CreateObject("roDateTime").AsSeconds()
    info = OsdDetails(m.playingChannel, m.summaries, m.guideError, now)
    logo = Text(m.playingChannel.logo)
    if not IsWebUrl(logo) then logo = ""
    m.top.findNode("osdLogo").uri = logo
    m.top.findNode("osdNumber").text = info.number
    m.top.findNode("osdTitle").text = channelTitleWithStar(m.playingChannel, info.title)
    m.top.findNode("osdNow").text = info.nowLine
    m.top.findNode("osdNext").text = info.nextLine
    hint = "OK: Info  •  Right: Guide  •  Up/Down: Zap"
    if m.playingChannel.isMovie = true then hint = "OK: Movie Info  •  Back: Exit to Movies"
    m.top.findNode("osdHint").text = hint
end sub

sub showOsd()
    if not m.video.visible or m.miniGuide.visible or m.playingChannel = invalid then return
    refreshOsdContent()
    m.osdTimer.duration = m.osdTimeout
    m.osd.visible = true
    m.osdTimer.control = "stop" : m.osdTimer.control = "start"
end sub

sub hideOsd()
    m.osdTimer.control = "stop"
    m.osd.visible = false
end sub

sub renderMiniGuide()
    if m.channels.Count() = 0 then return
    focused = m.miniList.itemFocused
    if focused < 0
        focused = 0
        for i = 0 to m.channels.Count() - 1
            if m.channels[i].url = m.playingUrl
                focused = i : exit for
            end if
        end for
    end if
    now = CreateObject("roDateTime").AsSeconds()
    root = CreateObject("roSGNode", "ContentNode")
    for i = 0 to m.channels.Count() - 1
        channel = m.channels[i] : item = root.CreateChild("ContentNode")
        item.title = channelTitleWithStar(channel)
        logo = Text(channel.logo)
        if IsWebUrl(logo) then item.hdPosterUrl = logo
        item.shortDescriptionLine1 = Text(channel.number)
        if item.shortDescriptionLine1 = "" then item.shortDescriptionLine1 = (i + 1).ToStr()
        if Text(channel.eventTitle) <> ""
            item.description = channel.eventTitle + " (" + Text(channel.league, "SPORTS") + ")"
        else
            item.description = ProgrammeLine(CurrentProgramme(ChannelSummary(m.summaries, channel.id), now))
        end if
    end for
    m.miniList.content = root
    if focused >= m.channels.Count() then focused = 0
    m.miniList.jumpToItem = focused
    onMiniFocused()
end sub

sub showMiniGuide()
    if not m.video.visible or m.channels.Count() = 0 then return
    hideOsd()
    if m.summaries.Count() = 0 then queueExtra("summary")
    m.miniGuide.visible = true
    renderMiniGuide()
    m.miniList.SetFocus(true)
    m.miniTimer.control = "stop" : m.miniTimer.control = "start"
end sub

sub hideMiniGuide()
    m.miniTimer.control = "stop"
    if not m.miniGuide.visible then return
    m.miniGuide.visible = false
    if m.video.visible then m.playerFocus.SetFocus(true)
end sub

sub onMiniFocused()
    if not m.miniGuide.visible then return
    m.miniTimer.control = "stop" : m.miniTimer.control = "start"
    index = m.miniList.itemFocused
    if index < 0 or index >= m.channels.Count() then return
    channel = m.channels[index]
    now = CreateObject("roDateTime").AsSeconds()
    info = OsdDetails(channel, m.summaries, m.guideError, now)
    header = channelTitleWithStar(channel, info.title)
    if info.number <> "" then header = info.number + " · " + header
    logo = Text(channel.logo)
    miniLogo = m.top.findNode("miniDetailLogo")
    miniTitle = m.top.findNode("miniDetailTitle")
    miniInfo = m.top.findNode("miniDetailInfo")
    if IsWebUrl(logo)
        miniLogo.uri = logo
        miniLogo.visible = true
        miniTitle.translation = [106, 10]
        miniTitle.width = 464
        miniInfo.translation = [106, 42]
        miniInfo.width = 464
    else
        miniLogo.uri = ""
        miniLogo.visible = false
        miniTitle.translation = [16, 10]
        miniTitle.width = 554
        miniInfo.translation = [16, 42]
        miniInfo.width = 554
    end if
    miniTitle.text = header
    details = info.nowLine
    if info.nextLine <> "" then details += Chr(10) + info.nextLine
    miniInfo.text = details
end sub

sub onMiniSelected()
    index = m.miniList.itemSelected
    if index < 0 or index >= m.channels.Count() then return
    hideMiniGuide()
    playItem(m.channels[index])
end sub

sub startRadioAttempt()
    if m.radioRow = invalid or m.radioFormatIndex < 0 or m.radioFormatIndex >= m.radioFormats.Count() then return
    fmt = m.radioFormats[m.radioFormatIndex]
    content = PlayerContent(m.radioRow, fmt)
    content.live = true
    m.audio.control = "stop"
    m.audio.content = content
    m.audio.control = "play"
end sub

sub setRadioEqBars(heights as Object, active as Boolean)
    barColor = "0x38BDF8FF"
    if not active then barColor = "0x64748BFF"
    for i = 0 to m.radioEqBars.Count() - 1
        bar = m.radioEqBars[i]
        if bar <> invalid and i < heights.Count()
            h = heights[i]
            bar.height = h
            bar.translation = [i * 8, 24 - h]
            bar.color = barColor
        end if
    end for
end sub

sub tickRadioEq()
    if m.radioRow = invalid or m.radioState <> "playing" or m.video.visible or m.pending
        m.radioEqTimer.control = "stop"
        m.radioEqRunning = false
        return
    end if
    frames = [
        [8, 22, 12, 18]
        [20, 10, 24, 12]
        [14, 24, 8, 22]
        [22, 14, 20, 8]
    ]
    m.radioEqFrame = (m.radioEqFrame + 1) mod frames.Count()
    setRadioEqBars(frames[m.radioEqFrame], true)
end sub

sub updateRadioMiniBar()
    show = (m.radioRow <> invalid and m.radioState <> "idle" and m.radioState <> "error" and not m.video.visible and not m.pending)
    m.radioMiniBar.visible = show
    m.footer.visible = not show
    if not show
        if m.radioEqRunning
            m.radioEqTimer.control = "stop"
            m.radioEqRunning = false
        end if
        return
    end if
    m.radioMiniTitle.text = Text(m.radioRow.title, "Live Radio")
    genre = Text(m.radioRow.group).Trim()
    if genre = "" then genre = "Live Radio"
    m.radioMiniGenre.text = genre
    if m.radioState = "playing"
        m.radioPillText.text = "LIVE"
        m.radioPillBg.color = "0x059669FF"
        if not m.radioEqRunning
            setRadioEqBars([14, 22, 10, 18], true)
            m.radioEqTimer.control = "start"
            m.radioEqRunning = true
        end if
    else if m.radioState = "buffering"
        m.radioPillText.text = "BUFFERING"
        m.radioPillBg.color = "0xD97706FF"
        if m.radioEqRunning
            m.radioEqTimer.control = "stop"
            m.radioEqRunning = false
        end if
        setRadioEqBars([8, 12, 8, 10], false)
    else
        m.radioPillText.text = "PAUSED"
        m.radioPillBg.color = "0x475569FF"
        if m.radioEqRunning
            m.radioEqTimer.control = "stop"
            m.radioEqRunning = false
        end if
        setRadioEqBars([6, 8, 6, 6], false)
    end if
end sub

sub playRadio(row as Object)
    url = Text(row.url).Trim()
    if not IsWebUrl(url) or Instr(1, url, "|") > 0
        m.radioState = "error"
        m.lastError = "Invalid radio stream URL"
        m.status.text = "Radio stream URL is invalid. Check the station in GlzHub."
        updateRadioMiniBar() : updateFooter() : showDetails() : return
    end if
    closePlayer()
    headers = CopyHeaders(row.headers)
    hasUa = false
    for each key in headers
        if LCase(key) = "user-agent" then hasUa = true
    end for
    if not hasUa
        ver = Text(m.appVersion)
        if ver = "" then ver = "1.48.3.10"
        headers["User-Agent"] = "GlzTV-Roku/" + ver
    end if
    radioRow = {
        title: Text(row.title, "Live Radio")
        url: url
        headers: headers
        id: Text(row.id)
        group: Text(row.group)
        logo: Text(row.logo)
        number: Text(row.number)
    }
    m.radioRow = radioRow
    m.radioTitle = radioRow.title
    m.radioFormats = RadioFormatFallbacks(url)
    m.radioFormatIndex = 0
    m.radioState = "buffering"
    m.lastError = ""
    startRadioAttempt()
    m.status.text = "Connecting to " + m.radioTitle + "…"
    updateHeaderBar() : updateRadioMiniBar() : updateFooter() : showDetails() : startSync(false)
end sub

sub stopRadio(sync = true as Boolean)
    wasActive = m.radioTitle <> "" or m.radioRow <> invalid
    m.audio.control = "stop"
    m.audio.content = invalid
    m.radioRow = invalid
    m.radioTitle = ""
    m.radioFormats = []
    m.radioFormatIndex = 0
    m.radioState = "idle"
    updateRadioMiniBar() : updateFooter() : showDetails()
    if sync and wasActive then startSync(false)
end sub

sub updateFooter()
    footerText = "Left / Back  Menu   •   OK  Select   •   *  Quick Options / Favorites"
    if m.radioTitle <> ""
        stateTag = "Radio"
        if m.radioState = "playing" then stateTag = "LIVE Radio"
        if m.radioState = "buffering" then stateTag = "Buffering"
        if m.radioState = "paused" then stateTag = "Paused"
        footerText = stateTag + ": " + m.radioTitle + "   •   Play/Pause controls audio   •   * → Stop radio"
    end if
    m.footer.text = footerText
end sub

sub closePlayer()
    if not m.video.visible then return
    hideOsd() : hideMiniGuide()
    m.video.visible = false : m.video.control = "stop" : m.browser.visible = true
    m.playingUrl = "" : m.playingTitle = "" : m.playingChannel = invalid : m.retriedPlayback = false
    updateRadioMiniBar()
    if m.view = "home" or m.view = "favorites" then renderRows()
    restoreFocus()
end sub

sub videoStateChanged()
    if m.video.state = "error"
        if not m.retriedPlayback and m.playingChannel <> invalid
            m.retriedPlayback = true
            content = PlayerContent(m.playingChannel, StreamFormat(m.playingChannel.url))
            content.live = not (m.playingChannel.isMovie = true)
            m.video.control = "stop" : m.video.content = content : m.video.control = "play"
            return
        end if
        m.lastError = "Roku playback error " + m.video.errorCode.ToStr()
        closePlayer()
        m.status.text = "Stream could not play. Try another item or Refresh."
        startSync(false)
    else if m.video.state = "playing"
        m.retriedPlayback = false
    else if m.video.state = "finished"
        closePlayer() : m.status.text = "Playback finished."
    end if
end sub

sub audioStateChanged()
    if m.radioRow = invalid then return
    state = m.audio.state
    if state = "buffering"
        m.radioState = "buffering"
        m.status.text = "Buffering " + m.radioTitle + "…"
        updateRadioMiniBar() : updateFooter() : showDetails()
    else if state = "playing"
        m.radioState = "playing"
        m.lastError = ""
        m.status.text = "Live Radio: " + m.radioTitle
        updateRadioMiniBar() : updateFooter() : showDetails()
    else if state = "paused"
        m.radioState = "paused"
        m.status.text = "Radio paused: " + m.radioTitle
        updateRadioMiniBar() : updateFooter() : showDetails()
    else if state = "stopped"
        if m.radioState <> "buffering" and m.radioState <> "idle" and m.radioState <> "error"
            m.radioState = "idle"
            updateRadioMiniBar() : updateFooter() : showDetails()
        end if
    else if state = "error"
        if m.radioFormatIndex + 1 < m.radioFormats.Count()
            m.radioFormatIndex++
            m.radioState = "buffering"
            m.status.text = "Retrying " + m.radioTitle + "…"
            startRadioAttempt()
            updateRadioMiniBar() : updateFooter() : showDetails()
            return
        end if
        errCode = m.audio.errorCode.ToStr()
        errMsg = Text(m.audio.errorMsg).Trim()
        if errMsg <> ""
            m.lastError = "Roku radio error " + errCode + ": " + errMsg
        else
            m.lastError = "Roku radio error " + errCode
        end if
        failedTitle = m.radioTitle
        m.audio.control = "stop"
        m.audio.content = invalid
        m.radioRow = invalid : m.radioTitle = "" : m.radioFormats = [] : m.radioFormatIndex = 0 : m.radioState = "error"
        m.status.text = "Radio stream failed (" + errCode + "): " + failedTitle + ". Try another station or Refresh."
        updateRadioMiniBar() : updateFooter() : showDetails() : startSync(false)
    else if state = "finished"
        stopRadio() : m.status.text = "Radio stream ended. Select the station to reconnect."
    end if
end sub

sub showOptions()
    dialog = CreateObject("roSGNode", "Dialog")
    dialog.title = "Glz TV Quick Options"
    dialog.message = "Manage Favorites, refresh content, open Settings, or control background radio."
    buttons = []
    actions = []
    m.optionFavoriteChannel = focusedLiveChannel()
    if m.optionFavoriteChannel <> invalid
        chName = Text(m.optionFavoriteChannel.title, "Channel")
        if IsFavoriteChannel(m.favoriteIds, ChannelKey(m.optionFavoriteChannel))
            buttons.Push("★ Remove " + chName + " from Favorites")
        else
            buttons.Push("★ Add " + chName + " to Favorites")
        end if
        actions.Push("toggle_favorite")
    end if
    buttons.Push("Refresh all sections") : actions.Push("refresh")
    buttons.Push("Open Settings") : actions.Push("settings")
    buttons.Push("Stop radio") : actions.Push("stop_radio")
    buttons.Push("Pair again") : actions.Push("repair")
    buttons.Push("Cancel") : actions.Push("cancel")
    m.optionActions = actions
    dialog.buttons = buttons
    dialog.ObserveField("buttonSelected", "onOption") : m.top.dialog = dialog
end sub

sub promptRepair()
    dialog = CreateObject("roSGNode", "Dialog")
    dialog.title = "Pair this Roku again?"
    dialog.message = "This replaces the saved device pairing on this Roku. Your GlzHub playlists stay saved."
    dialog.buttons = ["Generate new pairing code", "Cancel"]
    dialog.ObserveField("buttonSelected", "onRepair") : m.top.dialog = dialog
end sub

sub onOption()
    index = m.top.dialog.buttonSelected : m.top.dialog.close = true
    action = "cancel"
    if index >= 0 and index < m.optionActions.Count() then action = m.optionActions[index]
    if action = "toggle_favorite"
        toggleFavoriteForChannel(m.optionFavoriteChannel)
    else if action = "refresh"
        m.status.text = "Refreshing from GlzHub…"
        startSync(true) : queueExtra("summary") : queueExtra("radio") : queueExtra("weather")
        if m.view = "schedule" and m.guideChannel <> invalid then queueExtra("guide", m.guideChannel.id)
    else if action = "settings"
        closePlayer() : openSectionByName("settings")
    else if action = "stop_radio"
        stopRadio() : restoreFocus()
    else if action = "repair"
        promptRepair()
    else
        restoreFocus()
    end if
end sub

sub onRepair()
    selected = m.top.dialog.buttonSelected : m.top.dialog.close = true
    if selected = 0
        closePlayer() : stopRadio(false) : m.version = "" : m.autoStarted = false
        startSync(true, "repair")
    end if
    restoreFocus()
end sub

sub restoreFocus()
    if m.welcomeVisible
        m.welcomeFocus.SetFocus(true)
    else if m.pending
        m.top.SetFocus(true)
    else if m.video.visible
        if m.miniGuide.visible then m.miniList.SetFocus(true) else m.playerFocus.SetFocus(true)
    else if m.view = "weather" or m.rows.Count() = 0
        m.nav.SetFocus(true)
    else
        if m.focusedIndex >= 0 and m.focusedIndex < m.rows.Count() then m.list.jumpToItem = m.focusedIndex
        m.list.SetFocus(true)
    end if
end sub

function onKeyEvent(key as String, press as Boolean) as Boolean
    if not press then return false
    if m.welcomeVisible then return handleWelcomeKey(key)
    if m.video.visible
        if m.miniGuide.visible
            if key = "left" or key = "back" or key = "right"
                hideMiniGuide() : return true
            else if key = "up" or key = "down"
                m.miniTimer.control = "stop" : m.miniTimer.control = "start"
                return false
            end if
            return false
        end if
        if key = "back"
            if m.osd.visible
                hideOsd() : return true
            end if
            closePlayer() : startSync(false) : return true
        else if key = "right"
            showMiniGuide() : return true
        else if LCase(key) = "ok"
            if m.osd.visible then hideOsd() else showOsd()
            return true
        else if key = "channelup" or key = "up" or key = "rewind"
            stepChannel(-1) : return true
        else if key = "channeldown" or key = "down" or key = "fastforward"
            stepChannel(1) : return true
        else if key = "play"
            if m.video.state = "paused"
                m.video.control = "resume"
            else
                m.video.control = "pause"
            end if
            showOsd()
            return true
        else if key = "left"
            return true
        end if
        return false
    end if
    if key = "options"
        showOptions() : return true
    end if
    if key = "play" and m.radioRow <> invalid
        if m.audio.state = "paused" or m.radioState = "paused" or m.audio.state = "stopped" or m.audio.state = "none" or m.radioState = "error"
            m.radioState = "buffering"
            m.status.text = "Reconnecting to " + m.radioTitle + "…"
            startRadioAttempt()
            updateRadioMiniBar() : updateFooter() : showDetails()
        else
            m.audio.control = "pause"
            m.radioState = "paused"
            m.status.text = "Radio paused: " + m.radioTitle
            updateRadioMiniBar() : updateFooter() : showDetails()
        end if
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
