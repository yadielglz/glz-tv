sub showItem()
    item = m.top.itemContent
    if item = invalid then return
    m.top.findNode("channelNumber").text = item.shortDescriptionLine1
    logo = item.hdPosterUrl
    icon = m.top.findNode("channelIcon")
    title = m.top.findNode("channelTitle")
    prog = m.top.findNode("programmeTitle")
    if logo <> ""
        icon.uri = logo
        icon.visible = true
        title.translation = [148, 7]
        title.width = 422
        prog.translation = [148, 39]
        prog.width = 422
    else
        icon.uri = ""
        icon.visible = false
        title.translation = [86, 7]
        title.width = 484
        prog.translation = [86, 39]
        prog.width = 484
    end if
    title.text = item.title
    prog.text = item.description
end sub

sub showFocus()
    focused = m.top.focusPercent > 0.5
    m.top.findNode("focusBar").visible = focused
    color = "0x101C30FF"
    if focused then color = "0x183F5CFF"
    m.top.findNode("rowBackground").color = color
end sub
