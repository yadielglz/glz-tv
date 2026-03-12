const STORAGE_KEY = "channel-surfer-sources";
const SIDEBAR_STORAGE_KEY = "channel-surfer-sidebar";
const WEATHER_STORAGE_KEY = "channel-surfer-weather";
const DEFAULT_PLAYLIST_URL = "https://epg.best/46837-shw7fc.m3u";

const state = {
  channels: [],
  filteredChannels: [],
  groups: [],
  selectedIndex: 0,
  activeIndex: 0,
  currentGroup: "All",
  searchTerm: "",
  clockTimerId: null,
  hls: null,
  mpegts: null,
  dash: null,
  refreshTimerId: null,
  recoveryInFlight: false,
  playback: {
    channelIdentity: "",
    forceProxy: false
  },
  userActivated: false,
  sources: {
    playlistUrl: "",
    streamHeaders: ""
  }
};

const ui = {
  channelList: document.getElementById("channelList"),
  channelCount: document.getElementById("channelCount"),
  channelSearch: document.getElementById("channelSearch"),
  groupFilters: document.getElementById("groupFilters"),
  channelBadge: document.getElementById("channelBadge"),
  channelTitle: document.getElementById("channelTitle"),
  channelMeta: document.getElementById("channelMeta"),
  clockTime: document.getElementById("clockTime"),
  clockDate: document.getElementById("clockDate"),
  weatherTemp: document.getElementById("weatherTemp"),
  weatherSummary: document.getElementById("weatherSummary"),
  weatherLocation: document.getElementById("weatherLocation"),
  channelDetails: document.getElementById("channelDetails"),
  settingsDialog: document.getElementById("settingsDialog"),
  settingsToggle: document.getElementById("settingsToggle"),
  searchDialog: document.getElementById("searchDialog"),
  searchToggle: document.getElementById("searchToggle"),
  searchOverlayInput: document.getElementById("searchOverlayInput"),
  searchResults: document.getElementById("searchResults"),
  searchResultCount: document.getElementById("searchResultCount"),
  closeSearch: document.getElementById("closeSearch"),
  tipsDialog: document.getElementById("tipsDialog"),
  tipsToggle: document.getElementById("tipsToggle"),
  closeTips: document.getElementById("closeTips"),
  railToggle: document.getElementById("railToggle"),
  settingsForm: document.getElementById("settingsForm"),
  playlistUrl: document.getElementById("playlistUrl"),
  streamHeaders: document.getElementById("streamHeaders"),
  saveSources: document.getElementById("saveSources"),
  playerFrame: document.getElementById("playerFrame"),
  videoPlayer: document.getElementById("videoPlayer"),
  audioFallback: document.getElementById("audioFallback"),
  audioLabel: document.getElementById("audioLabel"),
  statusToast: document.getElementById("statusToast"),
  playPause: document.getElementById("playPause"),
  prevChannel: document.getElementById("prevChannel"),
  nextChannel: document.getElementById("nextChannel"),
  fullscreenToggle: document.getElementById("fullscreenToggle")
};

function readSidebarPreference() {
  return localStorage.getItem(SIDEBAR_STORAGE_KEY) === "collapsed";
}

function writeSidebarPreference(collapsed) {
  localStorage.setItem(SIDEBAR_STORAGE_KEY, collapsed ? "collapsed" : "expanded");
}

function readSavedWeather() {
  try {
    return JSON.parse(localStorage.getItem(WEATHER_STORAGE_KEY) || "null");
  } catch {
    return null;
  }
}

function writeSavedWeather(payload) {
  localStorage.setItem(WEATHER_STORAGE_KEY, JSON.stringify(payload));
}

function readSavedSources() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || "{}");
  } catch {
    return {};
  }
}

function writeSavedSources() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state.sources));
}

function normalizeKey(value = "") {
  return value.toLowerCase().replace(/[^a-z0-9]/g, "");
}

function getDisplayChannelNumber(channel, fallbackIndex = 0) {
  return channel?.channelNumber || String(fallbackIndex + 1).padStart(3, "0");
}

function showToast(message) {
  ui.statusToast.textContent = message;
  ui.statusToast.classList.add("visible");
  clearTimeout(showToast.timeoutId);
  showToast.timeoutId = setTimeout(() => {
    ui.statusToast.classList.remove("visible");
  }, 1800);
}

function getActiveChannel() {
  return state.filteredChannels[state.activeIndex] || null;
}

function getChannelSearchHaystack(channel) {
  return `${channel.title} ${channel.group} ${channel.tvgName}`.toLowerCase();
}

function channelMatchesSearch(channel, search) {
  return !search || getChannelSearchHaystack(channel).includes(search);
}

function setSearchInputs(value) {
  ui.channelSearch.value = value;
  ui.searchOverlayInput.value = value;
}

function setSearchTerm(value) {
  state.searchTerm = value;
  setSearchInputs(value);
  applyFilters();
}

function formatDate(date) {
  return new Intl.DateTimeFormat([], {
    weekday: "short",
    month: "short",
    day: "numeric"
  }).format(date);
}

function updateClock() {
  const now = new Date();
  ui.clockTime.textContent = new Intl.DateTimeFormat([], {
    hour: "numeric",
    minute: "2-digit"
  }).format(now);
  ui.clockDate.textContent = formatDate(now);
}

function startClock() {
  updateClock();
  clearInterval(state.clockTimerId);
  state.clockTimerId = setInterval(updateClock, 1000);
}

function describeWeatherCode(code, isDay) {
  const map = {
    0: isDay ? "Clear" : "Clear night",
    1: "Mostly clear",
    2: "Partly cloudy",
    3: "Overcast",
    45: "Fog",
    48: "Icy fog",
    51: "Light drizzle",
    53: "Drizzle",
    55: "Heavy drizzle",
    61: "Light rain",
    63: "Rain",
    65: "Heavy rain",
    71: "Light snow",
    73: "Snow",
    75: "Heavy snow",
    80: "Rain showers",
    81: "Heavy showers",
    82: "Violent showers",
    95: "Thunderstorm"
  };
  return map[code] || "Weather";
}

function renderWeather(payload) {
  if (!payload) {
    ui.weatherTemp.textContent = "--";
    ui.weatherSummary.textContent = "Weather unavailable";
    ui.weatherLocation.textContent = "Location not set";
    return;
  }

  ui.weatherTemp.textContent = `${Math.round(payload.temperature)}F`;
  ui.weatherSummary.textContent = payload.summary;
  ui.weatherLocation.textContent = payload.location || "Local forecast";
}

async function fetchWeather(latitude, longitude) {
  const params = new URLSearchParams({
    lat: latitude,
    lon: longitude
  });
  const response = await fetch(`/api/weather?${params.toString()}`);
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.error || "Weather unavailable");
  }

  const payload = {
    temperature: data.current.temperature,
    summary: describeWeatherCode(data.current.weatherCode, data.current.isDay),
    location: data.location,
    latitude,
    longitude,
    savedAt: Date.now()
  };

  writeSavedWeather(payload);
  renderWeather(payload);
}

function initWeather() {
  const saved = readSavedWeather();
  if (saved) {
    renderWeather(saved);
  } else {
    renderWeather(null);
    ui.weatherSummary.textContent = "Detecting location";
    ui.weatherLocation.textContent = "Allow location for local weather";
  }

  if (!navigator.geolocation) {
    ui.weatherSummary.textContent = "Weather unavailable";
    ui.weatherLocation.textContent = "Geolocation not supported";
    return;
  }

  navigator.geolocation.getCurrentPosition(
    async ({ coords }) => {
      try {
        await fetchWeather(coords.latitude, coords.longitude);
      } catch (error) {
        ui.weatherSummary.textContent = "Weather unavailable";
        ui.weatherLocation.textContent = error.message;
      }
    },
    () => {
      if (!saved) {
        ui.weatherSummary.textContent = "Weather unavailable";
        ui.weatherLocation.textContent = "Location permission denied";
      }
    },
    { enableHighAccuracy: false, timeout: 8000, maximumAge: 30 * 60 * 1000 }
  );
}

function createPlaceholderLogo(channel) {
  const initials = (channel.title || "?")
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join("")
    .toUpperCase();
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 96 96"><rect width="96" height="96" rx="18" fill="#09111f"/><text x="50%" y="54%" fill="#eff5fb" font-family="Arial, sans-serif" font-size="28" font-weight="700" text-anchor="middle">${initials}</text></svg>`;
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
}

function getLogoUrl(channel) {
  if (!channel.logo) {
    return createPlaceholderLogo(channel);
  }

  return `/api/image?url=${encodeURIComponent(channel.logo)}`;
}

function isAudioChannel(channel) {
  return getStreamType(channel) === "audio";
}

function isHlsChannel(channel) {
  return getStreamType(channel) === "hls";
}

function isTsChannel(channel) {
  return getStreamType(channel) === "mpegts";
}

function getStreamPath(channel) {
  try {
    return new URL(channel.streamUrl).pathname.toLowerCase();
  } catch {
    return channel.streamUrl.toLowerCase().split("?")[0];
  }
}

function getStreamType(channel) {
  const path = getStreamPath(channel);

  if (channel.radio || /\.(mp3|aac|m4a|flac|wav|ogg|oga)(\?|$)/i.test(path)) {
    return "audio";
  }
  if (/\.m3u8(\?|$)/i.test(path)) {
    return "hls";
  }
  if (/\.mpd(\?|$)/i.test(path)) {
    return "dash";
  }
  if (/\.(ts|mts|m2ts)(\?|$)/i.test(path)) {
    return "mpegts";
  }
  if (/\.(mp4|m4v|webm|ogv|mov)(\?|$)/i.test(path)) {
    return "video";
  }
  return "unknown";
}

function getStreamTypeLabel(channel) {
  const type = getStreamType(channel);
  switch (type) {
    case "audio":
      return "Audio";
    case "hls":
      return "HLS";
    case "dash":
      return "DASH";
    case "mpegts":
      return "MPEG-TS";
    case "video":
      return "Video";
    default:
      return "Live";
  }
}

function streamLooksSigned(channel) {
  return /(token=|expired=|timestamp=|content_auth2=|content_license2=|session_id=|auth_id=)/i.test(channel.streamUrl);
}

function getChannelIdentity(channel) {
  return channel?.keys?.find(Boolean) || normalizeKey(channel?.id || channel?.title || "");
}

function channelHasCustomHeaders(channel) {
  return Boolean(channel?.requestHeaders && Object.keys(channel.requestHeaders).length);
}

function findChannelIndexByIdentity(channels, identity) {
  if (!identity) {
    return -1;
  }

  return channels.findIndex((channel) => (channel.keys || []).includes(identity));
}

function schedulePlaylistRefresh() {
  clearTimeout(state.refreshTimerId);

  if (!state.channels.some(streamLooksSigned)) {
    return;
  }

  state.refreshTimerId = setTimeout(() => {
    refreshCurrentChannelStream("Refreshing expired stream tokens");
  }, 4 * 60 * 1000);
}

async function refreshCurrentChannelStream(reason = "Refreshing stream") {
  const currentChannel = state.filteredChannels[state.activeIndex];
  if (!currentChannel || state.recoveryInFlight || !state.sources.playlistUrl) {
    return;
  }

  state.recoveryInFlight = true;
  const identity = getChannelIdentity(currentChannel);
  showToast(reason);

  try {
    await loadPlaylist({ preserveIdentity: identity, autoTune: true });
    const refreshedChannel = state.filteredChannels[state.activeIndex];
    if (!refreshedChannel) {
      showToast("Playlist refreshed but channel was not found");
    }
  } catch (error) {
    showToast(error.message || "Failed to refresh stream");
  } finally {
    state.recoveryInFlight = false;
  }
}

function stopHls() {
  if (state.hls) {
    state.hls.destroy();
    state.hls = null;
  }
}

function stopMpegTs() {
  if (state.mpegts) {
    state.mpegts.pause();
    state.mpegts.unload();
    state.mpegts.detachMediaElement();
    state.mpegts.destroy();
    state.mpegts = null;
  }
}

function stopDash() {
  if (state.dash) {
    state.dash.reset();
    state.dash = null;
  }
}

function getPlaybackSource(channel, forceProxy = false) {
  if (forceProxy || channelHasCustomHeaders(channel)) {
    return channel.streamProxyUrl || channel.streamUrl;
  }
  return channel.streamUrl;
}

function retryWithProxy(channel, playbackOptions, reason) {
  if (playbackOptions.forceProxy || channelHasCustomHeaders(channel)) {
    return false;
  }

  showToast(reason);
  attachStream(channel, { forceProxy: true });
  return true;
}

function handleHlsError(channel, data) {
  const details = data?.details || "unknown error";
  const fatal = Boolean(data?.fatal);
  const playbackOptions = state.playback;

  if (!fatal) {
    if (details.includes("frag") && streamLooksSigned(channel)) {
      showToast("Fragment expired, refreshing playlist");
      refreshCurrentChannelStream("Fragment expired, refreshing playlist");
      return;
    }

    if (!details.includes("frag")) {
      showToast(`Playback issue: ${details}`);
    }
    return;
  }

  if (!state.hls) {
    return;
  }

  switch (data.type) {
    case window.Hls.ErrorTypes.NETWORK_ERROR:
      if (retryWithProxy(channel, playbackOptions, "Direct stream failed, retrying via proxy")) {
        return;
      }
      if (streamLooksSigned(channel) || details.includes("frag")) {
        showToast("Stream token expired, refreshing playlist");
        refreshCurrentChannelStream("Stream token expired, refreshing playlist");
      } else {
        showToast("Network hiccup, reconnecting stream");
        state.hls.startLoad();
      }
      break;
    case window.Hls.ErrorTypes.MEDIA_ERROR:
      showToast("Recovering media playback");
      state.hls.recoverMediaError();
      break;
    default:
      showToast(`Playback issue: ${details}`);
      if (streamLooksSigned(channel)) {
        refreshCurrentChannelStream("Stream failed, refreshing playlist");
      }
      break;
  }
}

function updatePlayPauseLabel() {
  ui.playPause.textContent = ui.videoPlayer.paused ? "Play" : "Pause";
}

function activatePlayback() {
  state.userActivated = true;
  ui.videoPlayer.muted = false;
}

function showReadyToast(message) {
  showToast(`Ready: ${message.replace(/^(Playing|Tuned to)\s+/, "")}. Press Play to start.`);
}

function startPlayback(player, message, options = {}) {
  const { audioOnly = false, useMpegts = false } = options;
  const mutedAutoplay = !state.userActivated && !audioOnly;
  player.muted = mutedAutoplay;

  const playAttempt = useMpegts && state.mpegts ? state.mpegts.play() : player.play();
  Promise.resolve(playAttempt)
    .then(() => {
      showToast(mutedAutoplay ? `${message} (muted)` : message);
    })
    .catch(() => {
      if (state.userActivated) {
        showToast("Playback blocked or unsupported");
      } else {
        showReadyToast(message);
      }
    })
    .finally(() => {
      updatePlayPauseLabel();
    });
}

function attachStream(channel, options = {}) {
  stopHls();
  stopMpegTs();
  stopDash();

  const playbackOptions = {
    forceProxy: Boolean(options.forceProxy)
  };
  const player = ui.videoPlayer;
  const source = getPlaybackSource(channel, playbackOptions.forceProxy);
  const streamType = getStreamType(channel);
  const audioMode = streamType === "audio";
  const hlsMode = streamType === "hls";
  const dashMode = streamType === "dash";
  const tsMode = streamType === "mpegts";
  state.playback = {
    channelIdentity: getChannelIdentity(channel),
    forceProxy: playbackOptions.forceProxy
  };

  ui.audioFallback.hidden = !audioMode;
  ui.audioLabel.textContent = `${channel.title} audio stream`;
  player.muted = !state.userActivated;
  player.pause();
  player.removeAttribute("src");
  player.load();

  if (audioMode) {
    player.src = source;
    startPlayback(player, `Playing ${channel.title}`, { audioOnly: true });
    return;
  }

  if (hlsMode && window.Hls?.isSupported()) {
    state.hls = new window.Hls({
      lowLatencyMode: true,
      backBufferLength: 30
    });
    state.hls.loadSource(source);
    state.hls.attachMedia(player);
    state.hls.on(window.Hls.Events.MANIFEST_PARSED, () => {
      startPlayback(player, `Tuned to ${channel.title}`);
    });
    state.hls.on(window.Hls.Events.ERROR, (_, data) => {
      handleHlsError(channel, data);
    });
    return;
  }

  if (dashMode && window.dashjs?.MediaPlayer) {
    state.dash = window.dashjs.MediaPlayer().create();
    state.dash.initialize(player, source, false);
    state.dash.on(window.dashjs.MediaPlayer.events.STREAM_INITIALIZED, () => {
      startPlayback(player, `Tuned to ${channel.title}`);
    });
    state.dash.on(window.dashjs.MediaPlayer.events.ERROR, () => {
      if (retryWithProxy(channel, playbackOptions, "Direct DASH failed, retrying via proxy")) {
        return;
      }
      showToast(`Playback issue: DASH stream failed`);
      if (streamLooksSigned(channel)) {
        refreshCurrentChannelStream("Stream expired, refreshing playlist");
      }
    });
    return;
  }

  if (tsMode && window.mpegts?.getFeatureList?.().mseLivePlayback) {
    state.mpegts = window.mpegts.createPlayer(
      {
        type: "mpegts",
        isLive: true,
        url: source
      },
      {
        enableWorker: true,
        liveBufferLatencyChasing: true,
        liveBufferLatencyMaxLatency: 3,
        liveBufferLatencyMinRemain: 0.5,
        autoCleanupSourceBuffer: true
      }
    );
    state.mpegts.attachMediaElement(player);
    state.mpegts.load();
    startPlayback(player, `Tuned to ${channel.title}`, { useMpegts: true });
    state.mpegts.on(window.mpegts.Events.ERROR, (_, details, info) => {
      if (retryWithProxy(channel, playbackOptions, "Direct stream failed, retrying via proxy")) {
        return;
      }
      showToast(`Playback issue: ${details || info?.msg || "unknown error"}`);
      if (streamLooksSigned(channel)) {
        refreshCurrentChannelStream("Stream expired, refreshing playlist");
      }
    });
    return;
  }

  player.src = source;
  startPlayback(player, `Tuned to ${channel.title}`);
}

function renderChannelDetails(channel) {
  const usesHeaders = channelHasCustomHeaders(channel);
  const detailCard = document.createElement("article");
  detailCard.className = "channel-details-shell";
  detailCard.innerHTML = `
    <div class="channel-details-header">
      <p class="eyebrow">Channel Status</p>
      <span class="signal-pill">${isAudioChannel(channel) ? "AUDIO" : "LIVE"}</span>
    </div>
    <div class="detail-pills">
      <span class="detail-pill">${channel.group}</span>
      <span class="detail-pill">${getStreamTypeLabel(channel)}</span>
      <span class="detail-pill">${usesHeaders ? "Proxy Headers" : "Direct Stream"}</span>
      <span class="detail-pill">${streamLooksSigned(channel) ? "Token Refresh" : "Always On"}</span>
    </div>
    <div class="detail-grid">
      <div class="detail-block accent">
        <p class="program-time">Channel</p>
        <h3>${channel.title}</h3>
        <p class="program-copy">Use the channel tray or search to jump around the lineup fast on mobile.</p>
      </div>
      <div class="detail-block">
        <p class="program-time">Playback</p>
        <h3>${usesHeaders ? "Proxy Ready" : "Direct Ready"}</h3>
        <p class="program-copy">${usesHeaders ? "Custom request headers are attached for this source." : "This source can play without extra request headers."}</p>
      </div>
    </div>
  `;
  ui.channelDetails.innerHTML = "";
  ui.channelDetails.appendChild(detailCard);
}

function renderChannelView(channel) {
  updateHero(channel);
  renderChannelDetails(channel);
}

function clearChannelDetails(message = "Add your playlist to begin") {
  ui.channelDetails.innerHTML = `
    <article class="channel-details-shell">
      <div class="channel-details-header">
        <p class="eyebrow">Channel Status</p>
        <span class="signal-pill offline">READY</span>
      </div>
      <div class="detail-grid single">
        <div class="detail-block accent">
          <p class="program-time">Setup</p>
          <h3>${message}</h3>
          <p class="program-copy">Load a playlist, then use the touch-friendly channel tray to surf live TV on mobile.</p>
        </div>
      </div>
    </article>
  `;
}

function renderChannels() {
  const activeChannel = state.filteredChannels[state.activeIndex];
  const selectedChannel = state.filteredChannels[state.selectedIndex];

  ui.channelList.innerHTML = "";
  ui.channelCount.textContent = `${state.filteredChannels.length}`;

  state.filteredChannels.forEach((channel, index) => {
    const row = document.createElement("button");
    row.type = "button";
    row.className = `channel-row${activeChannel === channel ? " active" : ""}${selectedChannel === channel ? " selected" : ""}`;
    row.innerHTML = `
      <div class="channel-tile-top">
        <span class="channel-number">${getDisplayChannelNumber(channel, index)}</span>
        <img class="channel-logo" src="${getLogoUrl(channel)}" alt="" />
      </div>
      <div class="channel-tile-copy">
        <strong class="channel-title">${channel.title}</strong>
        <span class="channel-subtitle">${channel.group}</span>
      </div>
    `;

    row.addEventListener("click", () => {
      activatePlayback();
      tuneChannel(index);
    });
    row.addEventListener("focus", () => {
      state.selectedIndex = index;
      renderChannels();
    });
    ui.channelList.appendChild(row);
  });
}

function updateHero(channel) {
  ui.channelBadge.textContent = getDisplayChannelNumber(channel, state.activeIndex);
  ui.channelTitle.textContent = channel.title;
  ui.channelMeta.textContent = `${channel.group} • ${getStreamTypeLabel(channel)} • ${channelHasCustomHeaders(channel) ? "Proxy headers enabled" : "Tap to surf"}`;
}

function applyFilters() {
  const search = state.searchTerm.trim().toLowerCase();
  state.filteredChannels = state.channels.filter((channel) => {
    const matchesGroup = state.currentGroup === "All" || channel.group === state.currentGroup;
    const matchesSearch = channelMatchesSearch(channel, search);
    return matchesGroup && matchesSearch;
  });

  state.selectedIndex = Math.min(state.selectedIndex, Math.max(state.filteredChannels.length - 1, 0));
  state.activeIndex = Math.min(state.activeIndex, Math.max(state.filteredChannels.length - 1, 0));
  renderChannels();

  if (state.filteredChannels[state.activeIndex]) {
    renderChannelView(state.filteredChannels[state.activeIndex]);
  } else {
    clearChannelDetails(state.channels.length ? "No channels matched your filters" : "Load a playlist to begin");
  }

  if (ui.searchDialog.open) {
    renderSearchResults();
  }
}

function renderSearchResults() {
  const search = state.searchTerm.trim().toLowerCase();
  const matchedChannels = state.channels.filter((channel) => channelMatchesSearch(channel, search));
  const visibleChannels = search ? matchedChannels : matchedChannels.slice(0, 24);

  ui.searchResults.innerHTML = "";
  ui.searchResultCount.textContent = `${matchedChannels.length} ${matchedChannels.length === 1 ? "match" : "matches"}`;

  if (!state.channels.length) {
    const empty = document.createElement("p");
    empty.className = "search-empty";
    empty.textContent = "Load a playlist to search channels.";
    ui.searchResults.appendChild(empty);
    return;
  }

  if (!visibleChannels.length) {
    const empty = document.createElement("p");
    empty.className = "search-empty";
    empty.textContent = "No channels matched that search.";
    ui.searchResults.appendChild(empty);
    return;
  }

  visibleChannels.forEach((channel) => {
    const identity = getChannelIdentity(channel);
    const row = document.createElement("button");
    row.type = "button";
    row.className = "search-result";
    row.innerHTML = `
      <div class="search-result-main">
        <span class="channel-number">${getDisplayChannelNumber(channel, state.channels.indexOf(channel))}</span>
        <img class="channel-logo" src="${getLogoUrl(channel)}" alt="" />
        <div class="search-result-copy">
          <strong>${channel.title}</strong>
          <span>${channel.group}</span>
        </div>
      </div>
      <span class="search-result-action">Tune</span>
    `;
    row.addEventListener("click", () => {
      activatePlayback();
      state.currentGroup = "All";
      setSearchTerm("");
      renderGroupFilters(state.groups);
      const nextIndex = findChannelIndexByIdentity(state.filteredChannels, identity);
      if (nextIndex >= 0) {
        tuneChannel(nextIndex);
      }
      ui.searchDialog.close();
    });
    ui.searchResults.appendChild(row);
  });
}

function updateFullscreenButton() {
  ui.fullscreenToggle.textContent = document.fullscreenElement === ui.playerFrame ? "Exit Fullscreen" : "Fullscreen";
}

function renderGroupFilters(groups) {
  ui.groupFilters.innerHTML = "";
  ["All", ...groups].forEach((group) => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = `group-filter${state.currentGroup === group ? " active" : ""}`;
    button.textContent = group;
    button.addEventListener("click", () => {
      state.currentGroup = group;
      state.selectedIndex = 0;
      state.activeIndex = 0;
      renderGroupFilters(groups);
      applyFilters();
    });
    ui.groupFilters.appendChild(button);
  });
}

function tuneChannel(index) {
  const channel = state.filteredChannels[index];
  if (!channel) {
    return;
  }

  state.activeIndex = index;
  state.selectedIndex = index;
  renderChannels();
  renderChannelView(channel);
  attachStream(channel);
}

function stepChannel(direction) {
  if (!state.filteredChannels.length) {
    return;
  }
  const next = (state.activeIndex + direction + state.filteredChannels.length) % state.filteredChannels.length;
  tuneChannel(next);
}

async function loadConfigDefaults() {
  let defaults = {
    playlistUrl: DEFAULT_PLAYLIST_URL,
    streamHeaders: ""
  };

  try {
    const response = await fetch("/api/config");
    const data = await response.json().catch(() => ({}));
    if (response.ok && data?.defaults) {
      defaults = {
        playlistUrl: data.defaults.playlistUrl || DEFAULT_PLAYLIST_URL,
        streamHeaders: data.defaults.streamHeaders || ""
      };
    } else {
      showToast("Config API unavailable, using default sources");
    }
  } catch {
    showToast("Config API unavailable, using default sources");
  }

  const saved = readSavedSources();
  state.sources = {
    playlistUrl: saved.playlistUrl || defaults.playlistUrl || "",
    streamHeaders: saved.streamHeaders || defaults.streamHeaders || ""
  };

  ui.playlistUrl.value = state.sources.playlistUrl;
  ui.streamHeaders.value = state.sources.streamHeaders;
}

async function loadPlaylist(options = {}) {
  const { preserveIdentity = "", autoTune = true } = options;
  const params = new URLSearchParams();
  if (state.sources.playlistUrl) {
    params.set("url", state.sources.playlistUrl);
  }
  if (state.sources.streamHeaders.trim()) {
    params.set("stream_headers", state.sources.streamHeaders.trim());
  }

  const response = await fetch(`/api/playlist?${params.toString()}`);
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.error || "Failed to load playlist");
  }

  state.channels = data.channels;
  state.groups = data.groups;
  state.filteredChannels = data.channels;
  renderGroupFilters(state.groups);
  applyFilters();
  if (ui.searchDialog.open) {
    renderSearchResults();
  }
  schedulePlaylistRefresh();

  if (state.filteredChannels.length && autoTune) {
    const nextIndex = preserveIdentity ? findChannelIndexByIdentity(state.filteredChannels, preserveIdentity) : 0;
    tuneChannel(nextIndex >= 0 ? nextIndex : 0);
  }
}

async function reloadSources() {
  try {
    await loadPlaylist();
    if (state.filteredChannels[state.activeIndex]) {
      renderChannelView(state.filteredChannels[state.activeIndex]);
    }
    showToast("Sources refreshed");
  } catch (error) {
    showToast(error.message);
  }
}

function bindEvents() {
  document.querySelector(".app-shell").classList.toggle("rail-collapsed", readSidebarPreference());
  updatePlayPauseLabel();
  updateFullscreenButton();
  setSearchInputs(state.searchTerm);

  ui.channelSearch.addEventListener("input", (event) => {
    setSearchTerm(event.target.value);
  });

  ui.railToggle.addEventListener("click", () => {
    const shell = document.querySelector(".app-shell");
    const collapsed = !shell.classList.contains("rail-collapsed");
    shell.classList.toggle("rail-collapsed", collapsed);
    writeSidebarPreference(collapsed);
  });

  ui.settingsToggle.addEventListener("click", () => {
    ui.settingsDialog.showModal();
  });

  ui.searchToggle.addEventListener("click", () => {
    setSearchInputs(state.searchTerm);
    renderSearchResults();
    ui.searchDialog.showModal();
    requestAnimationFrame(() => {
      ui.searchOverlayInput.focus();
      ui.searchOverlayInput.select();
    });
  });

  ui.tipsToggle.addEventListener("click", () => {
    ui.tipsDialog.showModal();
  });

  ui.searchOverlayInput.addEventListener("input", (event) => {
    setSearchTerm(event.target.value);
  });

  ui.closeSearch.addEventListener("click", () => {
    ui.searchDialog.close();
  });

  ui.closeTips.addEventListener("click", () => {
    ui.tipsDialog.close();
  });

  ui.saveSources.addEventListener("click", async () => {
    state.sources.playlistUrl = ui.playlistUrl.value.trim();
    state.sources.streamHeaders = ui.streamHeaders.value.trim();
    writeSavedSources();
    ui.settingsDialog.close();
    await reloadSources();
  });

  ui.playPause.addEventListener("click", () => {
    activatePlayback();
    if (ui.videoPlayer.paused) {
      const playAttempt = state.mpegts ? state.mpegts.play() : ui.videoPlayer.play();
      Promise.resolve(playAttempt).catch(() => {
        showToast("Playback blocked or unsupported");
      });
      return;
    }
    if (state.mpegts) {
      state.mpegts.pause();
    } else {
      ui.videoPlayer.pause();
    }
  });

  ui.prevChannel.addEventListener("click", () => {
    activatePlayback();
    stepChannel(-1);
  });

  ui.nextChannel.addEventListener("click", () => {
    activatePlayback();
    stepChannel(1);
  });
  ui.fullscreenToggle.addEventListener("click", async () => {
    try {
      if (document.fullscreenElement === ui.playerFrame) {
        await document.exitFullscreen();
      } else {
        await ui.playerFrame.requestFullscreen();
      }
    } catch {
      showToast("Fullscreen unavailable");
    }
  });

  document.addEventListener("fullscreenchange", updateFullscreenButton);
  ui.videoPlayer.addEventListener("play", updatePlayPauseLabel);
  ui.videoPlayer.addEventListener("pause", updatePlayPauseLabel);

  ui.videoPlayer.addEventListener("error", () => {
    const channel = state.filteredChannels[state.activeIndex];
    if (channel && streamLooksSigned(channel)) {
      if (retryWithProxy(channel, state.playback, "Direct stream failed, retrying via proxy")) {
        return;
      }
      refreshCurrentChannelStream("Stream failed, refreshing playlist");
    }
  });

  document.addEventListener("keydown", (event) => {
    if (event.target instanceof HTMLInputElement || event.target instanceof HTMLTextAreaElement) {
      return;
    }

    if (ui.settingsDialog.open || ui.tipsDialog.open || ui.searchDialog.open) {
      return;
    }

    activatePlayback();

    if (event.key === "ArrowDown") {
      event.preventDefault();
      stepChannel(1);
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      stepChannel(-1);
    } else if (event.key === "Enter") {
      tuneChannel(state.selectedIndex);
    } else if (/^[1-9]$/.test(event.key)) {
      const index = Number(event.key) - 1;
      if (index < state.filteredChannels.length) {
        tuneChannel(index);
      }
    }
  });
}

async function init() {
  bindEvents();
  startClock();
  initWeather();
  await loadConfigDefaults();

  if (!state.sources.playlistUrl) {
    ui.settingsDialog.showModal();
    clearChannelDetails("Add your playlist to begin");
    return;
  }

  await reloadSources();
}

if (document.readyState === "complete") {
  init();
} else {
  window.addEventListener("load", () => {
    init();
  }, { once: true });
}
