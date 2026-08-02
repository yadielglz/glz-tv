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

Add the three production secrets:

```bash
npx wrangler secret put SUPABASE_URL
npx wrangler secret put SUPABASE_PUBLISHABLE_KEY
npx wrangler secret put SUPABASE_SECRET_KEY
```

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
