# Glz TV for Roku — family test build

Native BrightScript / SceneGraph app connected to the existing GlzHub APIs.
The first hardware test target is a TCL 32S301 (8102X) running Roku OS 16.0.

## What this build does

- Generates its own GlzHub pairing code; no administrator password goes on the TV.
- Receives the paired device's managed playlist, including device/group channel
  policies and currently active event channels provided by GlzHub's M3U endpoint.
- Lists channels in published order with channel numbers, groups, and optional logos.
- Plays HLS, DASH and MP4 streams through Roku's native player, subject to the
  TV's supported codecs and the stream's authentication requirements.
- Uses Roku's native `*` playback menu for available captions and audio tracks,
  respecting the Roku's own caption preferences.
- Reports online/activity status and checks configuration every minute while open;
  re-downloads the lineup at least every five minutes for time-based events and
  externally updated M3U files, or sooner when Hub settings change.
- Remembers its pairing and can resume the last channel when that Hub setting is enabled.
- Refreshes the list through `*` → **Refresh channels**, and allows re-pairing after
  a local confirmation. Pairing codes expire after one hour and are renewed.

This is an installable development build, not a public Streaming Store app. It
has passed compilation and offline parser/security tests. Physical playback,
network pairing, and the exact 8102X remote/UI behavior still require on-device
verification. No GlzHub deployment or database migration is needed for this build.

## One-time TV setup

1. Enroll the Roku account in the [Roku Developer Program](https://developer.roku.com/).
2. From the Roku Home screen, press these remote buttons in sequence:
   **Home ×3, Up ×2, Right, Left, Right, Left, Right**.
3. Choose **Enable installer and restart**, accept the developer agreement, and
   set a developer password. Keep that password locally; it is not a GlzHub login.
4. Note the TV's IP address, also shown under **Settings → Network → About**.
5. Use a computer on the same local network as that TV. A remote cloud PC will
   not reach the TV's private IP without a separately configured network path.

Roku maintains one sideloaded development app at a time. Installing this build
replaces any existing development app; normal installed store apps remain.

## Install the ZIP

1. Download **Glz-TV-Roku.zip**. Do **not** unzip it for the TV installer.
2. On the computer, open `http://YOUR_ROKU_IP` in a browser.
3. Sign in as **rokudev**, using the developer password from the TV.
4. Under **Development Application Installer**, choose the ZIP with **Upload**,
   then **Install** (or **Replace with zip** / **Install**, depending on the page).
5. The app should launch and display a `GLZ-…` pairing code.
6. On your phone/computer, open [GlzHub](https://glzhub.glztech.com), sign in,
   select **Pair a TV**, and enter that code. Give the Roku a recognizable name.
7. Assign its TV playlist in the device's settings. The TV checks automatically;
   press `*` → **Refresh channels** to fetch it immediately.

Repeat installation and pairing separately on each family Roku. This development
ZIP is not installed through a shared public channel code. Store/beta publishing
is a separate later step; Roku beta apps are time-limited.

## Remote controls

- **Up / Down:** Browse the channel list.
- **OK:** Play the selected channel.
- **Back while watching:** Return to the channel list.
- **Play/Pause while watching:** Native playback controls, where supported.
- **`*` while watching:** Roku video options, including captions when available.
- **`*` on the channel list:** Refresh channels or pair again.
- **Home:** Exit the app.

## Build or update from the repository

From PowerShell in the repository root:

```powershell
cd roku
npm.cmd ci
npm.cmd run check
npm.cmd test
npm.cmd run build
```

The installer is `roku/dist/Glz-TV-Roku.zip`. Upload the updated ZIP using the TV's
developer installer. The device token stays in Roku's app registry between ordinary
updates when the developer signing identity is unchanged. If the TV asks to pair
again after an update, use its newly displayed code.

## Scope and troubleshooting

- Guide/EPG browsing, movies, app launching, Android APK updates, and GlzHub's
  phone Cast controls are not part of this first Roku build. Open and control it
  using the Roku remote.
- Native streams only; no server-side transcoding or DRM integration is added.
  Old TVs may reject HEVC/AV1, raw transport-stream URLs, or incompatible audio.
- Source request headers and common M3U `EXTVLCOPT` user-agent/referrer directives
  are supported. URLs with pipe-delimited header suffixes are not supported.
- The list is bounded to 1,500 channels to protect older-TV memory. The screen
  reports when this limit is reached; assign a smaller lineup in GlzHub.
- Hub device credentials authenticate only the Hub's own API calls. They are
  never placed in video requests or forwarded to external playlist servers.
- If the TV says **Cannot reach GlzHub**, check Wi-Fi and verify GlzHub opens on
  another device. If a channel fails, try another channel first and report the
  channel name and displayed error (no stream token or account password needed).
- Build `26.X27.2201` reports pairing HTTP/network codes instead of a generic
  connection message. `403` indicates server access rules, `429` rate limiting,
  `5xx` a Hub/backend error, and negative codes a Roku transport failure. It keeps
  TLS server verification enabled, omits unnecessary Roku client certificates,
  retains safe validation responses, and uses a descriptive app User-Agent.
- For a launch/compile failure, the developer installer shows the error.
  Runtime logs are available on the TV's developer console at TCP port 8085.
  Do not publish logs that contain private provider URLs.

## References

- [Roku development setup](https://developer.roku.com/dev/docs/development-setup)
- [Roku native Video node and captions](https://developer.roku.com/dev/docs/video)
- [Roku app publishing and beta limits](https://developer.roku.com/dev/docs/channel-publishing-guide)
