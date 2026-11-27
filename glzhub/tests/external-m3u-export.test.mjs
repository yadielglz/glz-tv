import { test, beforeEach, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
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
const { signExportM3uToken, verifyExportM3uToken } = mod;

const originalFetch = globalThis.fetch;
const env = {
  SUPABASE_URL: 'https://db.example',
  SUPABASE_SECRET_KEY: 'backend-secret-key',
  SUPABASE_PUBLISHABLE_KEY: 'pub-key'
};

let state;

beforeEach(() => {
  const now = Date.now();
  state = {
    playlists: [
      {
        id: '11111111-1111-1111-1111-111111111111',
        owner_id: 'owner-1',
        title: 'Main Lineup',
        target_app: 'tv',
        is_published: true,
        epg_url: null,
        artwork_url: 'https://cdn.example/main.png',
        playlist_items: [
          {
            id: 'ch-1',
            title: 'WAPA TV',
            media_url: 'https://streams.example/wapa.m3u8',
            duration_seconds: -1,
            position: 1,
            metadata: { tvg_id: 'wapa.pr', tvg_chno: '4', group: 'Local' }
          },
          {
            id: 'ch-hidden',
            title: 'Hidden Test Feed',
            media_url: 'https://streams.example/hidden.m3u8',
            duration_seconds: -1,
            position: 2,
            metadata: { hidden: true }
          }
        ]
      }
    ],
    guides: [
      { playlist_id: '11111111-1111-1111-1111-111111111111' }
    ],
    events: [
      {
        id: 'ev-live',
        tvg_id: 'nba.lakers.celtics',
        title: 'Lakers vs Celtics',
        sport_league: 'NBA',
        group_title: 'Live Sports',
        stream_url: 'https://streams.example/nba1.m3u8',
        logo_url: 'https://cdn.example/nba.png',
        channel_number: '30-01',
        start_time: new Date(now - 10 * 60_000).toISOString(),
        end_time: new Date(now + 90 * 60_000).toISOString(),
        status: 'active',
        is_online: true,
        auto_ingested: false
      },
      {
        id: 'ev-expired',
        tvg_id: 'mlb.old.game',
        title: 'Yankees vs Red Sox',
        sport_league: 'MLB',
        group_title: 'Live Sports',
        stream_url: 'https://streams.example/mlb-old.m3u8',
        start_time: new Date(now - 12 * 3600_000).toISOString(),
        end_time: new Date(now - 8 * 3600_000).toISOString(),
        status: 'active',
        is_online: true,
        auto_ingested: false
      }
    ]
  };

  globalThis.fetch = async (url, init = {}) => {
    const u = new URL(typeof url === 'string' ? url : url.url);
    if (u.pathname === '/auth/v1/user') {
      const auth = new Headers(init.headers).get('authorization') || '';
      if (auth === 'Bearer admin-jwt') {
        return new Response(JSON.stringify({ id: 'owner-1', email: 'admin@glz.tv' }), {
          status: 200,
          headers: { 'content-type': 'application/json' }
        });
      }
      return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401 });
    }
    if (u.pathname === '/rest/v1/playlists') {
      const idFilter = u.searchParams.get('id');
      let rows = state.playlists;
      if (idFilter?.startsWith('eq.')) {
        const wanted = idFilter.slice(3);
        rows = rows.filter((p) => p.id === wanted);
      }
      return new Response(JSON.stringify(rows), {
        status: 200,
        headers: { 'content-type': 'application/json' }
      });
    }
    if (u.pathname === '/rest/v1/event_channels') {
      return new Response(JSON.stringify(state.events), {
        status: 200,
        headers: { 'content-type': 'application/json' }
      });
    }
    if (u.pathname === '/rest/v1/epg_guides') {
      return new Response(JSON.stringify(state.guides), {
        status: 200,
        headers: { 'content-type': 'application/json' }
      });
    }
    return new Response(JSON.stringify([]), {
      status: 200,
      headers: { 'content-type': 'application/json' }
    });
  };
});

afterEach(() => {
  globalThis.fetch = originalFetch;
});

test('signExportM3uToken and verifyExportM3uToken round-trip and reject tampered tokens', async () => {
  const token = await signExportM3uToken(env, {
    v: 1,
    ownerId: 'owner-1',
    playlistId: null,
    includeEvents: true
  });
  const verified = await verifyExportM3uToken(env, token);
  assert.deepEqual(verified, {
    v: 1,
    ownerId: 'owner-1',
    playlistId: null,
    includeEvents: true
  });

  const tampered = token.slice(0, -4) + 'AAAA';
  assert.equal(await verifyExportM3uToken(env, tampered), null);
});

test('POST /api/v1/admin/export-m3u-url generates active M3U URL and GET adapts with Live Events', async () => {
  const createRes = await worker.fetch(new Request('https://hub.example/api/v1/admin/export-m3u-url', {
    method: 'POST',
    headers: {
      authorization: 'Bearer admin-jwt',
      'content-type': 'application/json'
    },
    body: JSON.stringify({ playlistId: null, includeEvents: true })
  }), env, { waitUntil() {} });

  assert.equal(createRes.status, 200);
  const meta = await createRes.json();
  assert.match(meta.url, /^https:\/\/hub\.example\/api\/v1\/export\/m3u\/[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.m3u$/);
  assert.equal(meta.epgUrl, 'https://hub.example/api/v1/guides/11111111-1111-1111-1111-111111111111.xml.gz');
  assert.equal(meta.scope, 'working');
  assert.equal(meta.channelCount, 1);
  assert.equal(meta.activeEventsCount, 1);

  // Fetch the live M3U URL as an external IPTV app (no Authorization header!)
  const m3uRes = await worker.fetch(new Request(meta.url, { method: 'GET' }), env, { waitUntil() {} });
  assert.equal(m3uRes.status, 200);
  assert.equal(m3uRes.headers.get('cache-control'), 'no-cache');
  assert.equal(m3uRes.headers.get('access-control-allow-origin'), '*');
  const etag1 = m3uRes.headers.get('etag');
  assert.ok(etag1);

  const m3uText = await m3uRes.text();
  assert.ok(m3uText.includes('#EXTM3U x-tvg-url="https://hub.example/api/v1/guides/11111111-1111-1111-1111-111111111111.xml.gz"'));
  assert.ok(m3uText.includes('WAPA TV'));
  assert.ok(!m3uText.includes('Hidden Test Feed'));
  // External apps get readable event titles instead of generic "SPORTS PPV"
  assert.ok(m3uText.includes(',NBA: Lakers vs Celtics'));
  assert.ok(m3uText.includes('https://streams.example/nba1.m3u8'));
  // Expired events are automatically omitted
  assert.ok(!m3uText.includes('Yankees vs Red Sox'));

  // Conditional GET returns 304 when unchanged
  const res304 = await worker.fetch(new Request(meta.url, {
    method: 'GET',
    headers: { 'if-none-match': etag1 }
  }), env, { waitUntil() {} });
  assert.equal(res304.status, 304);

  // HEAD request succeeds with empty body
  const headRes = await worker.fetch(new Request(meta.url, { method: 'HEAD' }), env, { waitUntil() {} });
  assert.equal(headRes.status, 200);
  assert.equal(await headRes.text(), '');

  // When a new Live Event becomes active, the EXACT same URL adapts immediately with a new ETag
  const now = Date.now();
  state.events.push({
    id: 'ev-new',
    tvg_id: 'ufc.main.event',
    title: 'UFC 310 Main Event',
    sport_league: 'UFC',
    group_title: 'Live Sports',
    stream_url: 'https://streams.example/ufc.m3u8',
    channel_number: '30-02',
    start_time: new Date(now - 5 * 60_000).toISOString(),
    end_time: new Date(now + 120 * 60_000).toISOString(),
    status: 'active',
    is_online: true,
    auto_ingested: false
  });

  const updatedRes = await worker.fetch(new Request(meta.url, {
    method: 'GET',
    headers: { 'if-none-match': etag1 }
  }), env, { waitUntil() {} });
  assert.equal(updatedRes.status, 200);
  assert.notEqual(updatedRes.headers.get('etag'), etag1);
  const updatedText = await updatedRes.text();
  assert.ok(updatedText.includes(',UFC 310 Main Event'));
  assert.ok(updatedText.includes('https://streams.example/ufc.m3u8'));
});

test('external M3U export with includeEvents=false omits Live Events and rejects invalid tokens', async () => {
  const createRes = await worker.fetch(new Request('https://hub.example/api/v1/admin/export-m3u-url', {
    method: 'POST',
    headers: {
      authorization: 'Bearer admin-jwt',
      'content-type': 'application/json'
    },
    body: JSON.stringify({
      playlistId: '11111111-1111-1111-1111-111111111111',
      includeEvents: false
    })
  }), env, { waitUntil() {} });
  assert.equal(createRes.status, 200);
  const meta = await createRes.json();
  assert.equal(meta.activeEventsCount, 0);

  const m3uRes = await worker.fetch(new Request(meta.url), env, { waitUntil() {} });
  const text = await m3uRes.text();
  assert.ok(text.includes('WAPA TV'));
  assert.ok(!text.includes('Lakers vs Celtics'));

  const badRes = await worker.fetch(new Request('https://hub.example/api/v1/export/m3u/invalid.token.m3u'), env, { waitUntil() {} });
  assert.equal(badRes.status, 404);
});

test('M3U export sorts playlist channels numerically by channel number including sub-channels', async () => {
  state.playlists[0].playlist_items = [
    { id: 'c-unnum', title: 'Unnumbered Feed', media_url: 'https://streams.example/u.m3u8', duration_seconds: -1, position: 1, metadata: {} },
    { id: 'c-10', title: 'Ch Ten', media_url: 'https://streams.example/10.m3u8', duration_seconds: -1, position: 2, metadata: { tvg_chno: '10' } },
    { id: 'c-2-1', title: 'Ch Two Sub', media_url: 'https://streams.example/2-1.m3u8', duration_seconds: -1, position: 3, metadata: { tvg_chno: '2-1' } },
    { id: 'c-2', title: 'Ch Two', media_url: 'https://streams.example/2.m3u8', duration_seconds: -1, position: 4, metadata: { tvg_chno: '2' } }
  ];

  const createRes = await worker.fetch(new Request('https://hub.example/api/v1/admin/export-m3u-url', {
    method: 'POST',
    headers: {
      authorization: 'Bearer admin-jwt',
      'content-type': 'application/json'
    },
    body: JSON.stringify({
      playlistId: '11111111-1111-1111-1111-111111111111',
      includeEvents: false
    })
  }), env, { waitUntil() {} });
  assert.equal(createRes.status, 200);
  const meta = await createRes.json();
  const m3uRes = await worker.fetch(new Request(meta.url), env, { waitUntil() {} });
  const text = await m3uRes.text();

  const idx2 = text.indexOf(',Ch Two\n');
  const idx21 = text.indexOf(',Ch Two Sub\n');
  const idx10 = text.indexOf(',Ch Ten\n');
  const idxUnnum = text.indexOf(',Unnumbered Feed\n');
  assert.ok(idx2 > 0 && idx21 > idx2 && idx10 > idx21 && idxUnnum > idx10, `Unexpected order in M3U:\n${text}`);
});
