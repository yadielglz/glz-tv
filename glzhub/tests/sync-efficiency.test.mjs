import { test, beforeEach, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import ts from 'typescript';

const transpile = (s) => ts.transpileModule(s, {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ESNext }
}).outputText;
const uri = (s) => 'data:text/javascript;base64,' + Buffer.from(s).toString('base64');
const guide = uri(transpile(readFileSync(new URL('../src/roku-guide.ts', import.meta.url), 'utf8')));
const source = transpile(readFileSync(new URL('../src/index.ts', import.meta.url), 'utf8'))
  .replace('"./roku-guide"', JSON.stringify(guide));
const mod = await import(uri(source));
const worker = mod.default;
const { computeEventChannelsFingerprint, performAutoIngestAndHealthCheck } = mod;

const originalFetch = globalThis.fetch;
const tokenHash = createHash('sha256').update('test-device-token').digest('hex');
const env = {
  SUPABASE_URL: 'https://db.example',
  SUPABASE_SECRET_KEY: 'backend-secret',
  SUPABASE_PUBLISHABLE_KEY: 'pub-key'
};

let state;

beforeEach(() => {
  state = {
    device: {
      id: 'device-1',
      owner_id: 'owner-1',
      name: 'Living Room TV',
      config_version: 5,
      assigned_playlist_id: '11111111-1111-1111-1111-111111111111',
      channel_policy_mode: 'inherit',
      force_refresh_token: 'tok-initial',
      app_version: '1.48.3.10'
    },
    pendingCommands: [
      { id: 'cmd-1', device_id: 'device-1', action: 'force_refresh', status: 'pending', payload: {} }
    ],
    deliveredCommandIds: [],
    radioStations: [
      {
        id: '22222222-2222-2222-2222-222222222222',
        owner_id: 'owner-1',
        station_code: 'GLZ1',
        name: 'GLZ Hits',
        genre: 'Pop',
        stream_url: 'https://stream.example/rad1',
        bitrate: 128,
        is_active: true,
        updated_at: '2026-11-24T10:00:00Z'
      }
    ],
    eventChannels: [],
    epgGuides: [
      {
        id: 'epg-1',
        owner_id: 'owner-1',
        playlist_id: '11111111-1111-1111-1111-111111111111',
        name: 'Main Guide',
        xml_content: '<tv><channel id="a"><display-name>News</display-name></channel></tv>',
        updated_at: '2026-11-24T09:00:00Z'
      }
    ],
    devicePatches: [],
    providerM3u: '#EXTM3U\n',
    streamHealthOk: true
  };

  globalThis.fetch = async (input, init) => {
    const url = new URL(typeof input === 'string' ? input : input.url);
    const method = (init?.method || 'GET').toUpperCase();

    if (url.hostname === 'db.example') {
      if (url.pathname === '/auth/v1/user') {
        return Response.json({ id: 'owner-1', email: 'admin@example.com' });
      }
      if (url.pathname === '/rest/v1/devices') {
        if (method === 'PATCH') {
          const patch = JSON.parse(String(init?.body || '{}'));
          state.devicePatches.push(patch);
          Object.assign(state.device, patch);
          return new Response(null, { status: 204 });
        }
        if (url.searchParams.get('token_hash')) {
          return Response.json(url.searchParams.get('token_hash') === `eq.${tokenHash}` ? [state.device] : []);
        }
        return Response.json([state.device]);
      }
      if (url.pathname === '/rest/v1/device_commands') {
        if (method === 'PATCH') {
          const idParam = url.searchParams.get('id') || '';
          state.deliveredCommandIds.push(idParam);
          state.pendingCommands = [];
          return new Response(null, { status: 204 });
        }
        return Response.json(state.pendingCommands);
      }
      if (url.pathname === '/rest/v1/playlists') {
        return Response.json([{
          id: '11111111-1111-1111-1111-111111111111',
          title: 'Main',
          is_published: true,
          epg_url: 'https://hub.example/api/v1/guides/11111111-1111-1111-1111-111111111111.xml',
          updated_at: '2026-11-24T08:00:00Z',
          playlist_items: [{ id: 'ch-1', title: 'News', media_url: 'https://media.example/1', position: 0, metadata: { tvg_id: 'a' } }]
        }]);
      }
      if (url.pathname === '/rest/v1/epg_guides') {
        return Response.json(state.epgGuides);
      }
      if (url.pathname === '/rest/v1/radio_stations') {
        if (method === 'POST') {
          const created = { id: '33333333-3333-3333-3333-333333333333', ...JSON.parse(String(init?.body || '{}')), updated_at: '2026-11-24T12:00:00Z' };
          state.radioStations.push(created);
          return Response.json([created], { status: 201 });
        }
        if (method === 'PATCH') {
          const patch = JSON.parse(String(init?.body || '{}'));
          Object.assign(state.radioStations[0], patch);
          return Response.json([state.radioStations[0]]);
        }
        if (method === 'DELETE') {
          state.radioStations.pop();
          return new Response(null, { status: 204 });
        }
        return Response.json(state.radioStations);
      }
      if (url.pathname === '/rest/v1/event_channels') {
        if (method === 'PATCH') {
          const idEq = (url.searchParams.get('id') || '').replace(/^eq\./, '');
          const patch = JSON.parse(String(init?.body || '{}'));
          const row = state.eventChannels.find((e) => String(e.id) === idEq);
          if (row) Object.assign(row, patch);
          return new Response(null, { status: 204 });
        }
        if (method === 'DELETE') {
          const idEq = (url.searchParams.get('id') || '').replace(/^eq\./, '');
          state.eventChannels = state.eventChannels.filter((e) => String(e.id) !== idEq);
          return new Response(null, { status: 204 });
        }
        if (url.searchParams.get('auto_ingested') === 'eq.true') {
          return Response.json(state.eventChannels.filter((e) => e.auto_ingested === true));
        }
        return Response.json(state.eventChannels);
      }
      return Response.json([]);
    }

    if (url.hostname === 'starlite.best') {
      return new Response(state.providerM3u, { status: 200 });
    }
    if (url.hostname === 'event.example') {
      return new Response('#EXTM3U\n#EXT-X-VERSION:3\n', { status: state.streamHealthOk ? 200 : 503 });
    }
    throw new Error(`Unexpected fetch to ${url.href}`);
  };
});

afterEach(() => {
  globalThis.fetch = originalFetch;
});

function run(request) {
  return worker.fetch(request, env, { waitUntil() {} });
}

test('heartbeat returns pendingCommandsCount alongside configVersion', async () => {
  const res = await run(new Request('https://hub.example/api/v1/devices/heartbeat', {
    method: 'POST',
    headers: { authorization: 'Bearer test-device-token', 'content-type': 'application/json' },
    body: JSON.stringify({ appVersion: '1.48.3.10' })
  }));
  assert.equal(res.status, 200);
  const body = await res.json();
  assert.equal(body.ok, true);
  assert.equal(body.configVersion, 5);
  assert.equal(body.pendingCommandsCount, 1);
});

test('device config returns inline commands and granular revisions, marking commands delivered', async () => {
  const res = await run(new Request('https://hub.example/api/v1/devices/config', {
    headers: { authorization: 'Bearer test-device-token' }
  }));
  assert.equal(res.status, 200);
  const first = await res.json();
  assert.equal(first.commands.length, 1);
  assert.equal(first.commands[0].id, 'cmd-1');
  assert.equal(state.deliveredCommandIds.length, 1);
  assert.ok(first.playlistRevision && typeof first.playlistRevision === 'string');
  assert.ok(first.epgRevision && typeof first.epgRevision === 'string');
  assert.ok(first.radioVersion && typeof first.radioVersion === 'string');

  // Updating a radio station changes radioVersion without altering playlistRevision or epgRevision
  state.radioStations[0].updated_at = '2026-11-24T15:00:00Z';
  const res2 = await run(new Request('https://hub.example/api/v1/devices/config', {
    headers: { authorization: 'Bearer test-device-token' }
  }));
  const second = await res2.json();
  assert.equal(second.commands.length, 0);
  assert.equal(second.playlistRevision, first.playlistRevision);
  assert.equal(second.epgRevision, first.epgRevision);
  assert.notEqual(second.radioVersion, first.radioVersion);
});

test('radio station CRUD bumps device config_version without rotating force_refresh_token', async () => {
  const headers = { authorization: 'Bearer admin-jwt', 'content-type': 'application/json' };
  const createRes = await run(new Request('https://hub.example/api/v1/admin/radio-stations', {
    method: 'POST',
    headers,
    body: JSON.stringify({ station_code: 'GLZ2', name: 'GLZ Chill', stream_url: 'https://stream.example/rad2' })
  }));
  assert.equal(createRes.status, 201);
  assert.equal(state.device.config_version, 6);
  assert.equal(state.device.force_refresh_token, 'tok-initial');

  const patchRes = await run(new Request('https://hub.example/api/v1/admin/radio-stations/22222222-2222-2222-2222-222222222222', {
    method: 'PATCH',
    headers,
    body: JSON.stringify({ name: 'GLZ Hits HD' })
  }));
  assert.equal(patchRes.status, 200);
  assert.equal(state.device.config_version, 7);
  assert.equal(state.device.force_refresh_token, 'tok-initial');

  const delRes = await run(new Request('https://hub.example/api/v1/admin/radio-stations/33333333-3333-3333-3333-333333333333', {
    method: 'DELETE',
    headers
  }));
  assert.equal(delRes.status, 200);
  assert.equal(state.device.config_version, 8);
  assert.equal(state.device.force_refresh_token, 'tok-initial');
});

test('playlist, radio, and managed guide endpoints use no-cache and support ETag 304', async () => {
  const endpoints = [
    { url: 'https://hub.example/api/v1/devices/playlist.m3u', headers: { authorization: 'Bearer test-device-token' }, expectedCache: 'private, no-cache' },
    { url: 'https://hub.example/api/v1/devices/radio-streams', headers: { authorization: 'Bearer test-device-token' }, expectedCache: 'private, no-cache' },
    { url: 'https://hub.example/api/v1/radio/stations', headers: {}, expectedCache: 'public, no-cache' },
    { url: 'https://hub.example/api/v1/guides/11111111-1111-1111-1111-111111111111.xml', headers: {}, expectedCache: 'public, no-cache' }
  ];

  for (const ep of endpoints) {
    const first = await run(new Request(ep.url, { headers: ep.headers }));
    assert.equal(first.status, 200, `expected 200 for ${ep.url}`);
    assert.equal(first.headers.get('cache-control'), ep.expectedCache, `cache-control for ${ep.url}`);
    const etag = first.headers.get('etag');
    assert.ok(etag, `expected etag for ${ep.url}`);

    const second = await run(new Request(ep.url, { headers: { ...ep.headers, 'if-none-match': etag } }));
    assert.equal(second.status, 304, `expected 304 for ${ep.url}`);
    assert.equal(second.headers.get('etag'), etag);
  }
});

test('performAutoIngestAndHealthCheck skips fleet push when events are unchanged and pushes when online status changes', async () => {
  state.eventChannels = [
    {
      id: 'ev-1',
      tvg_id: 'nba.1',
      tvg_name: 'NBA 1',
      title: 'Lakers vs Celtics',
      sport_league: 'NBA',
      group_title: 'NBA Events',
      logo_url: '',
      stream_url: 'https://event.example/nba1.m3u8',
      backup_stream_url: '',
      channel_number: '30-01',
      start_time: '2026-11-24T20:00:00Z',
      end_time: '2026-11-24T23:00:00Z',
      pre_buffer_hours: 0.5,
      post_buffer_hours: 0.5,
      status: 'active',
      is_online: true,
      auto_ingested: false
    }
  ];

  // Unchanged run: stream stays online -> no fleet-wide push
  const unchanged = await performAutoIngestAndHealthCheck(env);
  assert.equal(unchanged.pushed, false);
  assert.equal(state.devicePatches.length, 0);

  // Stream goes offline -> fingerprint changes -> fleet-wide push triggered
  state.streamHealthOk = false;
  const changed = await performAutoIngestAndHealthCheck(env);
  assert.equal(changed.pushed, true);
  assert.equal(state.devicePatches.length, 1);
  assert.equal(state.device.config_version, 6);
});

test('computeEventChannelsFingerprint ignores updated_at/last_checked_at noise', () => {
  const a = computeEventChannelsFingerprint([{ tvg_id: 'ev1', title: 'Game', is_online: true, updated_at: '2026-01-01T00:00:00Z', last_checked_at: '2026-01-01T00:00:00Z' }]);
  const b = computeEventChannelsFingerprint([{ tvg_id: 'ev1', title: 'Game', is_online: true, updated_at: '2026-11-24T12:00:00Z', last_checked_at: '2026-11-24T12:00:00Z' }]);
  const c = computeEventChannelsFingerprint([{ tvg_id: 'ev1', title: 'Game', is_online: false, updated_at: '2026-11-24T12:00:00Z' }]);
  assert.equal(a, b);
  assert.notEqual(a, c);
});
