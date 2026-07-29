# GLZ TV for Android

GLZ TV is a native, adaptive Android IPTV player. It loads M3U playlists and
XMLTV programme guides directly on the device and plays live streams with
Android Media3.

## Features

- Premium Android TV Guest Hub home experience
- Android TV launcher, banner, D-pad focus, and no-touchscreen support
- Native Android TV home-screen "Live now" recommendations
- Direct launcher-card deep links into live channel playback
- MediaSession playback metadata and remote/system integration
- External entertainment app discovery and install fallbacks:
  YouTube, Netflix, MLB, OleadaTV, GLZ Radio, Paramount+, Disney+, Peacock,
  and Spectrum TV
- Best-effort launch after device restart
- Configurable Home, Live TV, or Guide startup destination
- Resume-last-channel behavior
- M3U and M3U8 playlist loading over HTTP or HTTPS
- XMLTV EPG loading with channel-ID and display-name matching
- Now/next programme schedule with descriptions and local times
- HLS, progressive video, and audio playback
- Channel search, groups, favorites, and saved source settings
- Per-playlist and per-channel request headers
- Material 3 interface that adapts to portrait, landscape, tablets, and TV
- No local Node server or web proxy required

Only use playlists and streams you are authorized to access.

## Build

1. Install Android Studio or the Android SDK (API 36).
2. Open this directory as an Android project.
3. Run the `app` configuration, or build from a terminal:

```powershell
.\gradlew.bat assembleDebug
```

The debug APK is written to `app\build\outputs\apk\debug\app-debug.apk`.

## Use

Open **TV sources**, enter an HTTP(S) M3U URL and an optional XMLTV EPG URL,
then tap **Save & load**.
Providers that require request headers can be configured in the optional
headers field, one `Name: value` pair per line.

Fresh installations use these GLZ defaults:

- Playlist: `http://play.glztech.com/list.m3u`
- EPG: `https://play.glztech.com/epg.xml.gz`

The app detects and decompresses raw gzip XMLTV responses automatically.

## Android TV notes

The app is packaged with both standard and Leanback launcher entries. Its TV
banner is at `app/src/main/res/drawable-xhdpi/tv_banner.png`.

Automatic launch after restart is best effort on consumer Android TV devices
because the operating system and device manufacturer can block background
activity launches. Fully reliable boot launch requires GLZ TV to be provisioned
as a managed-device, kiosk, or home-launcher application.

The Android TV home-screen row is published only on devices that expose the
Leanback feature and run Android 8.0 or newer.
