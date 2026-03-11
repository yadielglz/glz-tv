# Channel Surfer TV

A local cable-box style IPTV shell that:

- loads an M3U playlist through a local proxy
- loads XMLTV / EPG data through the same local proxy to avoid browser CORS issues
- plays HLS streams through a rewritten manifest proxy
- plays MP3 and other audio streams in the same player surface

## Run

1. Copy [.env.example](/Users/marcosyadiel/Desktop/NewApp/.env.example) to `.env` if you want server defaults.
2. Set `PLAYLIST_URL` and `EPG_URL`, or leave them blank and enter them in the in-app Sources dialog.
3. Run:

```bash
npm start
```

4. Open `http://localhost:3000`.

## Notes

- The browser no longer fetches XMLTV directly, so common XMLTV CORS blocks are avoided.
- This does not magically bypass provider-side authorization. If an upstream stream rejects server-side proxying or requires special headers/tokens, the proxy may need provider-specific handling.
- EPG matching is based on `tvg-id`, `tvg-name`, channel id, and display names. Best results come from a playlist whose `tvg-id` values line up with your XMLTV feed.
