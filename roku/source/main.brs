sub Main(args as Dynamic)
    screen = CreateObject("roSGScreen")
    port = CreateObject("roMessagePort")
    screen.SetMessagePort(port)
    screen.CreateScene("GlzScene")
    screen.Show()
    while true
        event = wait(0, port)
        if type(event) = "roSGScreenEvent"
            if event.IsScreenClosed() then return
        end if
    end while
end sub
