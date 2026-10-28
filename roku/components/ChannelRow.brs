sub showItem()
    item = m.top.itemContent
    if item = invalid then return
    m.top.findNode("channelNumber").text = item.shortDescriptionLine1
    m.top.findNode("channelTitle").text = item.title
    m.top.findNode("programmeTitle").text = item.description
end sub

sub showFocus()
    focused = m.top.focusPercent > 0.5
    m.top.findNode("focusBar").visible = focused
    color = "0x101C30FF"
    if focused then color = "0x183F5CFF"
    m.top.findNode("rowBackground").color = color
end sub
