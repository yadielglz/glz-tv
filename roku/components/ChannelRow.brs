sub showItem()
    item = m.top.itemContent
    if item = invalid then return
    numNode = m.top.findNode("channelNumber")
    badge = item.shortDescriptionLine1
    numNode.text = badge
    if UCase(badge) = "LIVE"
        numNode.color = "0xEF4444FF"
    else
        numNode.color = "0x4DA8FFFF"
    end if
    logo = item.hdPosterUrl
    icon = m.top.findNode("channelIcon")
    title = m.top.findNode("channelTitle")
    prog = m.top.findNode("programmeTitle")
    if logo <> ""
        icon.uri = logo
        icon.visible = true
        title.translation = [150, 7]
        title.width = 484
        prog.translation = [150, 36]
        prog.width = 484
    else
        icon.uri = ""
        icon.visible = false
        title.translation = [92, 7]
        title.width = 542
        prog.translation = [92, 36]
        prog.width = 542
    end if
    title.text = item.title
    prog.text = item.description
end sub

sub showFocus()
    focused = m.top.focusPercent > 0.5
    m.top.findNode("focusLeft").visible = focused
    m.top.findNode("focusTop").visible = focused
    m.top.findNode("focusBottom").visible = focused
    m.top.findNode("focusRight").visible = focused
    m.top.findNode("rowDivider").visible = not focused
    bg = "0x12161EFF"
    progColor = "0x94A3B8FF"
    if focused
        bg = "0x000000FF"
        progColor = "0xCBD5E1FF"
    end if
    m.top.findNode("rowBackground").color = bg
    m.top.findNode("programmeTitle").color = progColor
end sub
