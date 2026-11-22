import { test, beforeEach, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import ts from 'typescript';

const transpile = (s) => ts.transpileModule(s, {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ESNext }
}).outputText;
const uri = (s) => 'data:text/javascript;base64,' + Buffer.from(s).toString('base64');
const guideUri = uri(transpile(readFileSync(new URL('../src/roku-guide.ts', import.meta.url), 'utf8')));
const workerSource = transpile(readFileSync(new URL('../src/index.ts', import.meta.url), 'utf8'))
  .replace('"./roku-guide"', JSON.stringify(guideUri));
const mod = await import(uri(workerSource));
const worker = mod.default;
const {
  isEventActiveAndValid,
  isLiveEventChannel,
  parseInlineEventTime,
  parseConMeM3u
} = mod;

const originalFetch = globalThis.fetch;
const tokenHash = createHash('sha256').update('test-device-token').digest('hex');
const env = {
  SUPABASE_URL: 'https://db.example',
  SUPABASE_SECRET_KEY: 'backend-secret',
  SUPABASE_PUBLISHABLE_KEY: 'pub-key'
};

afterEach(() => {
  globalThis.fetch = originalFetch;
});

test('isEventActiveAndValid aligns with isLiveEventChannel across MLB, NBA, NFL, NHL, NCAA, UFC, Boxing', () => {
  const now = Date.UTC(2026, 10, 22, 20, 0, 0);
  const base = {
    status: 'active',
    is_online: true,
    auto_ingested: true,
    start_time: new Date(now - 30 * 60_000).toISOString(),
    end_time: new Date(now + 2.5 * 3600_000).toISOString(),
    pre_buffer_hours: 0.5,
    post_buffer_hours: 0.5,
    group_title: 'Major League Sports (Events)'
  };

  for (const [league, title] of [
    ['MLB', 'MLB: Yankees vs Red Sox'],
    ['NBA', 'NBA: Lakers @ Celtics'],
    ['NFL', 'NFL: Chiefs vs Bills'],
    ['NHL', 'NHL: Rangers vs Bruins'],
    ['NCAA', 'NCAAF: Ohio State vs Michigan'],
    ['UFC', 'UFC 310: Main Card'],
    ['BOXING', 'Boxing: Heavyweight Title Fight']
  ]) {
    assert.equal(
      isEventActiveAndValid({ ...base, sport_league: league, title, tvg_id: `${league.toLowerCase()}.1` }, now),
      true,
      `Expected ${league} event to be active and valid`
    );
  }

  // Reject German feeds, placeholders, and duplicate linear networks
  assert.equal(isEventActiveAndValid({ ...base, sport_league: 'NBA', title: 'DE: NBA Lakers vs Heat', tvg_id: 'de.nba' }, now), false);
  assert.equal(isEventActiveAndValid({ ...base, sport_league: 'NFL', title: 'NFL: Stream Offline', tvg_id: 'nfl.off' }, now), false);
  assert.equal(isEventActiveAndValid({ ...base, sport_league: 'NBA', title: 'NBA TV', tvg_id: 'nbatv' }, now), false);
  assert.equal(isLiveEventChannel('Major League Sports (Events)', 'MLB Network', 'mlb.net'), false);
});

test('isEventActiveAndValid supports extended durations up to 12h and buffers up to 6h', () => {
  const start = Date.UTC(2026, 10, 22, 18, 0, 0);
  const longPpv = {
    status: 'active',
    is_online: true,
    auto_ingested: true,
    sport_league: 'UFC',
    title: 'UFC 310 PPV: Full Card',
    group_title: 'PPV Events',
    tvg_id: 'ufc.310',
    start_time: new Date(start).toISOString(),
    end_time: new Date(start + 6 * 3600_000).toISOString(), // 6h PPV (previously broken by 3.5h clamp)
    pre_buffer_hours: 2,
    post_buffer_hours: 2
  };

  // 1.5h before start (inside 2h pre-buffer, previously broken by 1h clamp)
  assert.equal(isEventActiveAndValid(longPpv, start - 1.5 * 3600_000), true);
  // 5h into the 6h event (previously broken by 3.5h duration clamp)
  assert.equal(isEventActiveAndValid(longPpv, start + 5 * 3600_000), true);
  // 1.5h after 6h end (inside 2h post-buffer)
  assert.equal(isEventActiveAndValid(longPpv, start + 7.5 * 3600_000), true);
  // 2.5h after 6h end (outside 2h post-buffer)
  assert.equal(isEventActiveAndValid(longPpv, start + 8.5 * 3600_000), false);
});

test('parseInlineEventTime handles comma dates, Dec->Jan year rollover in ET, and marks Branch 3 fallback', () => {
  const novNow = new Date('2026-11-22T15:00:00Z');
  const commaParsed = parseInlineEventTime('NBA: Lakers vs Celtics | Nov 22, 7:10 PM', novNow);
  assert.equal(commaParsed.hasInlineTime, true);
  // Nov 22 is EST (-5), so 7:10 PM ET = 00:10 UTC on Nov 23
  assert.equal(commaParsed.startTime.toISOString(), '2026-11-23T00:10:00.000Z');

  const decNow = new Date('2026-12-31T20:00:00Z');
  const rollover = parseInlineEventTime('NHL: Winter Classic | Jan 1, 1:00 PM', decNow);
  assert.equal(rollover.hasInlineTime, true);
  assert.equal(rollover.startTime.toISOString(), '2027-01-01T18:00:00.000Z');

  const noTime = parseInlineEventTime('UFC 310 Live Main Event', novNow);
  assert.equal(noTime.hasInlineTime, false);
});

test('parseConMeM3u splits EXTINF comma outside quoted attributes', () => {
  const m3u = [
    '#EXTM3U',
    '#EXTINF:-1 tvg-id="nba.1" tvg-name="NBA, Live" group-title="Sports, Events",NBA: Heat vs Knicks, Nov 22, 7:30 PM',
    'https://stream.example/nba1.m3u8'
  ].join('\n');

  const parsed = parseConMeM3u(m3u);
  assert.equal(parsed.length, 1);
  assert.equal(parsed[0].title, 'NBA: Heat vs Knicks, Nov 22, 7:30 PM');
  assert.equal(parsed[0].groupTitle, 'Sports, Events');
  assert.equal(parsed[0].sportLeague, 'NBA');
});

test('multi-playlist M3U does not overwrite earlier playlists when a 2nd playlist has epg_url', async () => {
  const now = Date.now();
  const device = {
    id: 'dev-multi',
    owner_id: 'owner-1',
    assigned_playlist_id: null,
    box_group_id: null,
    channel_policy_mode: 'inherit'
  };

  globalThis.fetch = async (input) => {
    const url = new URL(typeof input === 'string' ? input : input.url);
    if (url.pathname === '/rest/v1/devices') {
      return Response.json(url.searchParams.get('token_hash') === `eq.${tokenHash}` ? [device] : []);
    }
    if (url.pathname === '/rest/v1/playlists') {
      return Response.json([
        {
          id: 'pl-1',
          title: 'Local Channels',
          epg_url: 'https://epg.example/local.xml.gz',
          playlist_items: [
            { id: 'item-1', position: 1, title: 'WAPA TV', media_url: 'https://stream.example/wapa.m3u8', metadata: { tvg_id: 'wapa.pr' } }
          ]
        },
        {
          id: 'pl-2',
          title: 'Sports Pack',
          epg_url: 'https://epg.example/sports.xml.gz',
          playlist_items: [
            { id: 'item-2', position: 1, title: 'ESPN', media_url: 'https://stream.example/espn.m3u8', metadata: { tvg_id: 'espn.us' } }
          ]
        }
      ]);
    }
    if (url.pathname === '/rest/v1/channel_policy_rules') return Response.json([]);
    if (url.pathname === '/rest/v1/event_channels') {
      return Response.json([
        {
          id: 'ev-nba',
          tvg_id: 'nba.live.1',
          title: 'NBA: Lakers vs Warriors',
          sport_league: 'NBA',
          group_title: 'NBA Events',
          stream_url: 'https://stream.example/nba.m3u8',
          channel_number: '30-01',
          start_time: new Date(now - 30 * 60_000).toISOString(),
          end_time: new Date(now + 4.5 * 3600_000).toISOString(),
          pre_buffer_hours: 0.5,
          post_buffer_hours: 0.5,
          status: 'active',
          is_online: true,
          auto_ingested: true
        }
      ]);
    }
    return Response.json([]);
  };

  const req = new Request('https://hub.example/api/v1/devices/playlist.m3u', {
    headers: { authorization: 'Bearer test-device-token' }
  });
  const res = await worker.fetch(req, env, { waitUntil() {} });
  assert.equal(res.status, 200);
  const text = await res.text();

  // Only one #EXTM3U header at the very top
  assert.equal((text.match(/#EXTM3U/g) || []).length, 1);
  assert.ok(text.startsWith('#EXTM3U x-tvg-url="https://epg.example/local.xml.gz"'));
  // Both playlists' channels AND the active NBA live event are preserved!
  assert.ok(text.includes('WAPA TV'), 'First playlist channel must not be overwritten');
  assert.ok(text.includes('ESPN'), 'Second playlist channel must be present');
  assert.ok(text.includes('NBA: Lakers vs Warriors'), 'Active NBA live event must be delivered');
});

test('DELETE /api/v1/admin/sites/:id deletes an owned property and returns 404 when missing', async () => {
  let deletedId = null;
  globalThis.fetch = async (input, init) => {
    const url = new URL(typeof input === 'string' ? input : input.url);
    if (url.pathname === '/auth/v1/user') {
      return Response.json({ id: 'owner-1', email: 'admin@example.com' });
    }
    if (url.pathname === '/rest/v1/sites') {
      if (init?.method === 'DELETE') {
        deletedId = url.searchParams.get('id');
        return new Response(null, { status: 204 });
      }
      const idParam = url.searchParams.get('id');
      return Response.json(idParam === 'eq.11111111-1111-1111-1111-111111111111' ? [{ id: '11111111-1111-1111-1111-111111111111' }] : []);
    }
    return Response.json([]);
  };

  const delOk = await worker.fetch(
    new Request('https://hub.example/api/v1/admin/sites/11111111-1111-1111-1111-111111111111', {
      method: 'DELETE',
      headers: { authorization: 'Bearer admin-jwt' }
    }),
    env,
    { waitUntil() {} }
  );
  assert.equal(delOk.status, 200);
  assert.equal(deletedId, 'eq.11111111-1111-1111-1111-111111111111');

  const delMissing = await worker.fetch(
    new Request('https://hub.example/api/v1/admin/sites/22222222-2222-2222-2222-222222222222', {
      method: 'DELETE',
      headers: { authorization: 'Bearer admin-jwt' }
    }),
    env,
    { waitUntil() {} }
  );
  assert.equal(delMissing.status, 404);
});
