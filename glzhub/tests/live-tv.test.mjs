import { test, beforeEach, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { JSDOM } from 'jsdom';
import { createLiveTV, liveChannels, mediaType } from '../public/live-tv.js';

const html = await readFile(new URL('../public/index.html', import.meta.url), 'utf8');
const item = (id, title, metadata = {}, media_url = 'https://media.example/live.m3u8') => ({ id, title, metadata, media_url, position: Number(id) || 0 });
const playlists = [
  { id: 'main', title: 'Main', is_published: true, playlist_items: [item('2', 'News', { group: 'News', tvg_chno: '2' }), item('1', 'Sports', { group: 'Sports' }), item('3', 'Hidden', { hidden: 'true' }), item('4', 'Radio', { radio: true })] },
  { id: 'draft', title: 'Draft', is_published: false, playlist_items: [item('5', 'Draft channel')] },
  { id: 'extra', title: 'Extra', is_published: true, playlist_items: [item('6', '<img src=x onerror=alert(1)>')] }
];
let dom, player, loads, events, mediaEvents, remote, activeSession, previewStops;
const tick = () => new Promise(resolve => setTimeout(resolve, 0));
const $ = id => dom.window.document.getElementById(id);

beforeEach(() => {
  dom = new JSDOM(html, { url: 'https://hub.example/' });
  globalThis.window = dom.window; globalThis.document = dom.window.document; globalThis.location = dom.window.location; globalThis.Option = dom.window.Option;
  dom.window.HTMLMediaElement.prototype.pause = function () {};
  dom.window.HTMLMediaElement.prototype.load = function () {};
  dom.window.HTMLMediaElement.prototype.play = function () { return Promise.resolve(); };
  loads = []; events = {}; mediaEvents = {}; activeSession = null; previewStops = 0;
  const castSession = { getCastDevice: () => ({ friendlyName: 'Vizio Test' }), loadMedia: async request => { loads.push(request); } };
  const context = {
    setOptions() {}, getCurrentSession: () => activeSession,
    addEventListener: (_type, cb) => { events.session = cb; },
    requestSession: async () => { activeSession = castSession; events.session({ sessionState: 'started' }); },
    endCurrentSession: () => { activeSession = null; events.session({ sessionState: 'ended' }); }
  };
  const framework = {
    CastContext: { getInstance: () => context }, CastContextEventType: { SESSION_STATE_CHANGED: 'session' },
    SessionState: { SESSION_STARTED: 'started', SESSION_RESUMED: 'resumed', SESSION_ENDED: 'ended' },
    RemotePlayerEventType: { ANY_CHANGE: 'change' },
    RemotePlayer: class { constructor() { this.isMediaLoaded = true; this.canPause = true; this.isPaused = false; remote = this; } },
    RemotePlayerController: class {
      addEventListener(_type, callback) { mediaEvents.change = callback; }
      playOrPause() { remote.isPaused = !remote.isPaused; mediaEvents.change(); }
      stop() { remote.isMediaLoaded = false; mediaEvents.change(); }
    }
  };
  const chrome = { cast: {
    AutoJoinPolicy: { ORIGIN_SCOPED: 'origin' }, Image: class { constructor(url) { this.url = url; } },
    media: { DEFAULT_MEDIA_RECEIVER_APP_ID: 'CC1AD845', StreamType: { LIVE: 'LIVE' },
      MediaInfo: class { constructor(contentId, contentType) { Object.assign(this, { contentId, contentType }); } },
      GenericMediaMetadata: class {}, LoadRequest: class { constructor(media) { this.media = media; } } }
  } };
  globalThis.cast = dom.window.cast = { framework }; globalThis.chrome = dom.window.chrome = chrome;
});
afterEach(() => { player?.reset(); dom.window.close(); player = null; });
function makePlayer(api = async () => ({ playlists })) {
  player = createLiveTV({ api, stopPreview: () => { previewStops++; } });
  return player;
}

test('published catalog excludes hidden/radio entries, preserves order, and filters playlist', () => {
  assert.deepEqual(liveChannels(playlists).map(c => c.title), ['Sports', 'News', '<img src=x onerror=alert(1)>']);
  assert.equal(liveChannels(playlists, 'extra').length, 1);
  assert.equal(mediaType('https://media.example/STREAM.M3U8?token=abc'), 'application/x-mpegURL');
  assert.equal(mediaType('https://media.example/live.mpd'), 'application/dash+xml');
  assert.equal(mediaType('https://media.example/video.mp4'), 'video/mp4');
});

test('phone list supports search, group, playlist filters and safe channel labels', async () => {
  makePlayer().open(); await tick();
  assert.equal(document.querySelectorAll('.live-channel').length, 3);
  assert.equal(document.querySelector('.live-channel-label img'), null);
  $('liveSearch').value = '2'; $('liveSearch').dispatchEvent(new window.Event('input'));
  assert.equal(document.querySelectorAll('.live-channel').length, 1);
  $('liveSearch').value = ''; $('liveGroup').value = 'Sports'; $('liveGroup').dispatchEvent(new window.Event('change'));
  assert.equal(document.querySelectorAll('.live-channel').length, 1);
  $('livePlaylist').value = 'extra'; $('livePlaylist').dispatchEvent(new window.Event('change'));
  assert.equal(document.querySelectorAll('.live-channel').length, 1);
  assert.equal($('liveGroup').value, '');
});

test('Cast sends media without Hub credentials, switches channels and controls receiver', async () => {
  makePlayer().open(); await tick();
  document.querySelector('.live-channel').click(); await tick();
  $('liveCast').click(); await tick();
  assert.equal(loads.length, 1);
  assert.equal(loads[0].media.contentId, 'https://media.example/live.m3u8');
  assert.equal(loads[0].media.metadata.title, 'Sports');
  assert.equal(loads[0].media.streamType, 'LIVE');
  assert.ok(!JSON.stringify(loads[0]).includes('authorization'));
  document.querySelectorAll('.live-channel')[1].click(); await tick();
  assert.equal(loads[1].media.metadata.title, 'News');
  $('liveRemotePause').click(); assert.equal($('liveRemotePause').textContent, 'Resume TV');
  assert.equal(previewStops, 2);
  player.leave(); assert.ok(activeSession, 'Cast continues when switching tabs');
  player.reset(); assert.equal(activeSession, null); assert.equal(document.querySelectorAll('.live-channel').length, 0);
});

test('sign-out discards an in-flight playlist response', async () => {
  let resolve; makePlayer(() => new Promise(r => { resolve = r; })).open();
  player.reset(); resolve({ playlists }); await tick();
  assert.equal(document.querySelectorAll('.live-channel').length, 0);
});

test('HTTP local playback gives guidance; Cast can still attempt receiver playback', async () => {
  makePlayer(async () => ({ playlists: [{ ...playlists[0], playlist_items: [item('1', 'HTTP channel', {}, 'http://media.example/live.m3u8')] }] })).open();
  await tick(); document.querySelector('.live-channel').click(); await tick();
  assert.match($('liveStatus').textContent, /Phone playback needs an HTTPS/);
  assert.equal($('liveVideo').getAttribute('src'), null);
  $('liveCast').click(); await tick(); assert.equal(loads[0].media.contentId, 'http://media.example/live.m3u8');
});

test('failed playlist fetch has a retry and never renders a stale catalog', async () => {
  makePlayer(async () => { throw new Error('offline'); }).open(); await tick();
  assert.match($('liveStatus').textContent, /Could not load channels/);
  assert.equal($('liveReload').disabled, false);
});

test('HLS playback switches cleanly and releases the player when leaving Live TV', async () => {
  const instances = [];
  class Hls {
    static Events = { MANIFEST_PARSED: 'manifest', ERROR: 'error' };
    static isSupported() { return true; }
    constructor() { this.events = {}; this.destroyed = false; instances.push(this); }
    on(name, cb) { this.events[name] = cb; }
    loadSource(url) { this.url = url; }
    attachMedia(video) { this.video = video; }
    destroy() { this.destroyed = true; }
  }
  window.Hls = Hls; makePlayer().open(); await tick();
  document.querySelector('.live-channel').click(); await tick();
  assert.equal(instances.length, 1);
  assert.equal(instances[0].url, 'https://media.example/live.m3u8');
  document.querySelectorAll('.live-channel')[1].click(); await tick();
  assert.ok(instances[0].destroyed);
  assert.equal(instances.length, 2);
  player.leave(); assert.ok(instances[1].destroyed);
});

test('receiver load failure restores channel selection and reports the error', async () => {
  makePlayer().open(); await tick();
  $('liveCast').click(); await tick();
  activeSession.loadMedia = async () => { throw new Error('receiver unavailable'); };
  document.querySelector('.live-channel').click(); await tick();
  assert.match($('liveStatus').textContent, /TV could not load this stream/);
  assert.equal($('liveChannels').inert, false);
  assert.equal($('liveCast').disabled, false);
});

test('relative or malformed optional artwork cannot prevent casting', async () => {
  makePlayer(async () => ({ playlists: [{ ...playlists[0], playlist_items: [
    item('1', 'Relative logo', { tvg_logo: '/logos/channel.png' }),
    item('2', 'Invalid logo', { tvg_logo: 'https://[' })
  ] }] })).open();
  await tick(); $('liveCast').click(); await tick();
  document.querySelector('.live-channel').click(); await tick();
  document.querySelectorAll('.live-channel')[1].click(); await tick();
  assert.equal(loads.length, 2);
  assert.equal(loads[0].media.metadata.title, 'Relative logo');
  assert.equal(loads[1].media.metadata.title, 'Invalid logo');
  assert.equal(loads[0].media.metadata.images, undefined);
});
