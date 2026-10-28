# GLZ Hub

Cloudflare Worker portal and API for managing GLZ TV installations at
`https://glzhub.glztech.com`.

## Architecture

- Cloudflare Worker serves the portal and all `/api/v1` endpoints.
- Supabase Auth signs administrators in.
- Supabase PostgreSQL stores enrollments, devices, and versioned configuration.
- TV installations authenticate with random device tokens. Only SHA-256 token
  hashes are stored in the database.
- Supabase secret credentials exist only as encrypted Cloudflare Worker secrets.

## 1. Create the Supabase project

Create a dedicated Supabase project, then run
`supabase/migrations/001_glzhub.sql` in its SQL editor.

Create the first administrator in **Authentication → Users**. Public
self-registration is intentionally not exposed by the portal.

Collect these values from the project:

- Project URL
- Publishable key
- Secret key

Never put the secret key in the web app, Android app, Git, or `.dev.vars.example`.

## 2. Local development

```bash
cd glzhub
npm install
cp .dev.vars.example .dev.vars
```

Fill in `.dev.vars`, then run:

```bash
npm run check
npm run dev
```

The portal is available at `http://localhost:8787`.

## 3. Configure Cloudflare

Log Wrangler into the intended Cloudflare account:

```bash
npx wrangler login
```

Add the four production secrets. `TMDB_API_TOKEN` is the API Read Access Token
from the TMDB account API settings page; it powers current movie metadata lookup.

```bash
npx wrangler secret put SUPABASE_URL
npx wrangler secret put SUPABASE_PUBLISHABLE_KEY
npx wrangler secret put SUPABASE_SECRET_KEY
npx wrangler secret put TMDB_API_TOKEN
```

TMDB requires attribution and may require a commercial API agreement depending
on the deployment. Keep the attribution card in the Hub's System settings and
review the current [TMDB API terms](https://www.themoviedb.org/api-terms-of-use).

Deploy:

```bash
npm run deploy
```

In **Workers & Pages → glzhub → Settings → Domains & Routes**, add the custom
domain `glzhub.glztech.com`. Cloudflare creates the DNS record and certificate.

Apply a Cloudflare rate-limit rule to `POST /api/v1/enrollment` and
`POST /api/v1/enrollment/claim` before broad distribution.

## 4. Pair a television

1. Install a GLZ TV build containing `GlzHubManager`.
2. Open **Settings → GLZ Hub → Generate pairing code**.
3. Sign in at `https://glzhub.glztech.com`.
4. Open **Pair a TV** and enter the code.
5. Configuration reaches an open TV within two minutes. The last valid
   configuration remains stored locally during an outage.

## API surface

| Method | Path | Authentication |
|---|---|---|
| `GET` | `/api/health` | None |
| `POST` | `/api/v1/enrollment` | Installation bootstrap |
| `POST` | `/api/v1/enrollment/claim` | Supabase administrator JWT |
| `GET` | `/api/v1/admin/devices` | Supabase administrator JWT |
| `PATCH` | `/api/v1/admin/devices/:id` | Supabase administrator JWT |
| `GET` | `/api/v1/admin/radio-stations` | Supabase administrator JWT |
| `POST` | `/api/v1/admin/radio-stations` | Supabase administrator JWT |
| `GET` | `/api/v1/admin/playlists` | Supabase administrator JWT |
| `POST` | `/api/v1/admin/playlists` | Supabase administrator JWT |
| `GET` | `/api/v1/radio/stations` | Public GLZ Radio catalog |
| `GET` | `/api/v1/devices/config` | Device token |
| `GET` | `/api/v1/devices/playlist.m3u` | Device token |
| `POST` | `/api/v1/devices/heartbeat` | Device token |

The standalone GLZ Radio app reads `GET /api/v1/radio/stations`. The response
contains only active stations, a content-derived `version`, and an `ETag`.
Clients should retain the `ETag` and send `If-None-Match` on later refreshes;
the Hub returns `304 Not Modified` when the station catalog has not changed.

## Live TV web player and Google Cast

The **Live TV** tab plays the signed-in administrator's published TV playlists.
It uses the existing owner-scoped `/api/v1/admin/playlists` endpoint; hidden
channels and radio entries are excluded. Playlist and group filters, channel
search, and refresh work on phones, tablets, and desktop.

On Android, open `https://glzhub.glztech.com` in Chrome, sign in, open **Live TV**,
choose a channel, then tap **Cast to TV** and select a compatible Vizio SmartCast,
Chromecast, or Google TV device on the same Wi-Fi. You can also connect first and
then choose a channel. Switch channels from the list; use **Pause TV**, **Stop**,
or **Disconnect TV** to control playback. Leaving the tab stops local video while
an existing Cast session continues. Signing out stops playback and ends the Cast
session. iPhone browsers do not support the Google Cast Web Sender SDK.

The **CC · Captions** selector lists text tracks reported by the active Cast
receiver, native video player, or HLS playlist. Choose a language or **Off**;
while casting this changes captions on the TV. Tracks discovered after playback
starts appear automatically. Switching caption languages preserves active
audio/video tracks. The selector is disabled when no compatible captions are
reported. It does not generate captions or transfer phone-rendered text to the TV.

The sender uses Google's Default Media Receiver, so no custom receiver ID,
developer registration, or TV modification is required. Only the media URL and
channel metadata go to the receiver; Hub authorization tokens are never sent.
Receiver support on a particular TV still requires testing on that device.

Phone playback uses the existing HLS.js library or native HLS support, plus direct
MP4/WebM video. DASH URLs can be sent to Cast but are not played locally by this
tab. HTTPS pages require HTTPS streams for local playback. The receiver must be
able to reach the original stream and support its codecs, authentication, and
CORS configuration. Streams needing custom headers/cookies, raw MPEG-TS feeds,
or inaccessible local addresses may fail. This feature does not proxy or
transcode streams. If playback fails, the tab reports an actionable message.

Validation: `npm run check`, `node --check public/live-tv.js`, and
`node --test tests/live-tv.test.mjs`.

## Device and app management

Apply both SQL migrations in order. Migration `002_app_management.sql` adds expanded
device settings, the owner-scoped app library, and the per-device command queue.

The portal can send either:

- a Google Play package, which opens the package listing on the TV; or
- an HTTPS repository APK URL, which opens the device's download/installer flow.

Android requires an on-device install confirmation on ordinary consumer TVs. Silent
installation requires GLZ TV to be provisioned as the Android device owner (managed
or kiosk deployment); the portal intentionally does not bypass that security model.

## Guest Experience

Apply `003_guest_experience.sql` after the earlier migrations. The Guest Experience
editor manages property-wide branding, Wi-Fi and checkout details, notices, front
desk information, and up to 12 HTTPS service tiles. Room number and stay dates remain
per-device settings. TVs persist the latest profile locally so text remains available
when the Hub is temporarily unreachable; remote artwork uses the image cache.

App visibility controls only the apps shown inside GLZ TV. It does not modify
the Android TV system launcher.

## Managed TV playlists

Apply migrations `009_ecosystem_radio_playlists.sql`,
`010_seed_m3u_recovered_channels.sql`, and
`011_device_playlist_assignment.sql`, then `012_playlist_studio_metadata.sql`
in order. Playlist Studio in GLZ Hub imports and exports M3U files, edits full
channel metadata, persists drag-and-drop ordering, manages an XMLTV source URL,
and publishes TV channel lineups. Each paired television can be assigned one lineup. An
unassigned television receives all published TV playlists owned by its Hub
administrator. Radio stations are managed separately and never enter the TV
M3U output.

## Native Roku guide

`GET /api/v1/devices/guide` requires the paired device Bearer token. It returns
now/next summaries only for channels in the device's filtered managed M3U lineup.
`?channel=<tvg-id-or-title>` also returns up to 96 programme entries over the next
24 hours for that assigned channel; an unassigned channel returns 404.

Assigned XMLTV feeds are decompressed and projected on the Worker. Up to four
sources share a 20 MiB decoded limit, and raw source results are cached for 120
seconds by source URL and configured headers. Device credentials never go to
external feeds. Active event metadata provides PPV details when XMLTV is missing.
Feed errors return partial data with `guideError`; playback remains independent.
Deploy this Worker with the Roku 26.X27.2300 update; no SQL migration is needed.
