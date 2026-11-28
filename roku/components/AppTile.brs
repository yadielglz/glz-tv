sub showItem()
    item = m.top.itemContent
    if item = invalid then return

    isCompact = (LCase(item.rating) = "compact")
    w = 228 : h = 150
    if isCompact
        w = 204 : h = 118
    end if

    tileFill = m.top.findNode("tileFill")
    tileSheen = m.top.findNode("tileSheen")
    tileArtwork = m.top.findNode("tileArtwork")
    tileMonogram = m.top.findNode("tileMonogram")
    tileScrim = m.top.findNode("tileScrim")
    tileBadge = m.top.findNode("tileBadge")
    tileTitle = m.top.findNode("tileTitle")
    tileSubtitle = m.top.findNode("tileSubtitle")
    idleTop = m.top.findNode("idleBorderTop")
    idleBottom = m.top.findNode("idleBorderBottom")
    fTop = m.top.findNode("focusTop")
    fBottom = m.top.findNode("focusBottom")
    fLeft = m.top.findNode("focusLeft")
    fRight = m.top.findNode("focusRight")

    tileFill.width = w : tileFill.height = h
    tileSheen.width = w : tileSheen.height = Int(h * 0.38)
    tileArtwork.width = w : tileArtwork.height = h
    idleTop.width = w
    idleBottom.width = w : idleBottom.translation = [0, h - 1]
    fTop.width = w
    fBottom.width = w : fBottom.translation = [0, h - 4]
    fLeft.height = h
    fRight.height = h : fRight.translation = [w - 4, 0]

    accent = item.shortDescriptionLine2
    if accent = invalid or accent = "" then accent = "0x1E293BFF"
    tileFill.color = accent

    if isCompact
        tileMonogram.translation = [10, 8] : tileMonogram.width = w - 20 : tileMonogram.height = 52
        tileScrim.translation = [0, 60] : tileScrim.width = w : tileScrim.height = 58
        tileBadge.translation = [10, 62] : tileBadge.width = w - 20 : tileBadge.height = 16
        tileTitle.translation = [10, 76] : tileTitle.width = w - 20 : tileTitle.height = 22
        tileSubtitle.translation = [10, 96] : tileSubtitle.width = w - 20 : tileSubtitle.height = 18
    else
        tileMonogram.translation = [14, 14] : tileMonogram.width = w - 28 : tileMonogram.height = 68
        tileScrim.translation = [0, 82] : tileScrim.width = w : tileScrim.height = 68
        tileBadge.translation = [12, 86] : tileBadge.width = w - 24 : tileBadge.height = 18
        tileTitle.translation = [12, 102] : tileTitle.width = w - 24 : tileTitle.height = 24
        tileSubtitle.translation = [12, 126] : tileSubtitle.width = w - 24 : tileSubtitle.height = 18
    end if

    artworkUrl = item.hdPosterUrl
    if artworkUrl <> invalid and artworkUrl <> ""
        tileArtwork.uri = artworkUrl
        tileArtwork.visible = true
        tileMonogram.visible = false
        tileSheen.visible = false
    else
        tileArtwork.uri = ""
        tileArtwork.visible = false
        tileMonogram.text = item.title
        tileMonogram.visible = true
        tileSheen.visible = true
    end if

    tileBadge.text = item.shortDescriptionLine1
    tileTitle.text = item.title
    tileSubtitle.text = item.description
end sub

sub showFocus()
    focused = m.top.focusPercent > 0.5
    m.top.findNode("focusTop").visible = focused
    m.top.findNode("focusBottom").visible = focused
    m.top.findNode("focusLeft").visible = focused
    m.top.findNode("focusRight").visible = focused
    m.top.findNode("idleBorderTop").visible = not focused
    m.top.findNode("idleBorderBottom").visible = not focused
    scrim = m.top.findNode("tileScrim")
    if scrim <> invalid
        if focused
            scrim.color = "0x06080BE0"
        else
            scrim.color = "0x06080BC4"
        end if
    end if
end sub
