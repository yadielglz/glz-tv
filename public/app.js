const STORAGE_KEY = "channel-surfer-sources";
const SIDEBAR_STORAGE_KEY = "channel-surfer-sidebar";
const WEATHER_STORAGE_KEY = "channel-surfer-weather";
const GUIDE_SLOT_MINUTES = 30;
const GUIDE_SLOT_COUNT = 4;
const DEFAULT_PLAYLIST_URL = "https://epg.best/46837-shw7fc.m3u";
const DEFAULT_EPG_URL = "https://epg.best/1eef8-shw7fc.xml.gz";

const state = {
  channels: [],
  filteredChannels: [],
  groups: [],
  guide: new Map(),
  selectedIndex: 0,
  activeIndex: 0,
  guideOffset: 0,
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
    epgUrl: "",
    streamHeaders: ""
  }
};

const ui = {
  channelList: document.getElementById("channelList"),
  channelCount: document.getElementById("channelCount"),
  channelSearch: document.getElementById("channelSearch"),
  groupFilters: document.getElementById("groupFilters"),
  guideTimeline: document.getElementById("guideTimeline"),
  guideStatus: document.getElementById("guideStatus"),
  channelBadge: document.getElementById("channelBadge"),
  channelTitle: document.getElementById("channelTitle"),
  channelMeta: document.getElementById("channelMeta"),
  clockTime: document.getElementById("clockTime"),
  clockDate: document.getElementById("clockDate"),
  weatherTemp: document.getElementById("weatherTemp"),
  weatherSummary: document.getElementById("weatherSummary"),
  weatherLocation: document.getElementById("weatherLocation"),
  nowNext: document.getElementById("nowNext"),
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
  epgUrl: document.getElementById("epgUrl"),
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

function formatTime(date) {
  return new Intl.DateTimeFormat([], {
    hour: "numeric",
    minute: "2-digit"
  }).format(date);
}

function formatDate(date) {
  return new Intl.DateTimeFormat([], {
    weekday: "short",
    month: "short",
    day: "numeric"
  }).format(date);
}

function formatTimeCompact(date) {
  return new Intl.DateTimeFormat([], {
    hour: "numeric"
  }).format(date).replace(":00", "");
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

function parseXmltvDate(value) {
  const match = value?.match(/^(\d{4})(\d{2})(\d{2})(\d{2})(\d{2})(\d{2})\s*([+\-]\d{4})?/);
  if (!match) {
    return null;
  }

  const [, year, month, day, hour, minute, second, offset] = match;
  const iso = `${year}-${month}-${day}T${hour}:${minute}:${second}${offset ? `${offset.slice(0, 3)}:${offset.slice(3)}` : "Z"}`;
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? null : date;
}

function parseEpg(xmlText) {
  const programsByKey = new Map();

  Object.entries(xmlText || {}).forEach(([key, entries]) => {
    programsByKey.set(
      normalizeKey(key),
      (entries || [])
        .map((entry) => ({
          title: entry.title || "Untitled",
          desc: entry.desc || "",
          categories: Array.isArray(entry.categories) ? entry.categories : [],
          start: parseXmltvDate(entry.start),
          stop: parseXmltvDate(entry.stop)
        }))
        .sort((a, b) => (a.start?.getTime() || 0) - (b.start?.getTime() || 0))
    );
  });

  return programsByKey;
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

function getProgramsForChannel(channel) {
  for (const key of channel.keys || []) {
    if (state.guide.has(key)) {
      return state.guide.get(key);
    }
  }
  return [];
}

function getCurrentProgram(programs) {
  const now = Date.now();
  return programs.find((program) => {
    const start = program.start?.getTime() || 0;
    const stop = program.stop?.getTime() || 0;
    return start <= now && stop >= now;
  });
}

function getUpcomingPrograms(programs) {
  const now = Date.now();
  return programs.filter((program) => (program.stop?.getTime() || 0) >= now).slice(state.guideOffset, state.guideOffset + 5);
}

function addMinutes(date, minutes) {
  return new Date(date.getTime() + minutes * 60_000);
}

function startOfGuideWindow() {
  const now = new Date();
  now.setSeconds(0, 0);
  now.setMinutes(now.getMinutes() < 30 ? 0 : 30);
  return addMinutes(now, state.guideOffset * GUIDE_SLOT_MINUTES);
}

function getVisibleGuideChannels() {
  if (!state.filteredChannels.length) {
    return [];
  }

  return state.filteredChannels;
}

function getProgramsInWindow(programs, windowStart, windowEnd) {
  return programs.filter((program) => {
    if (!program.start || !program.stop) {
      return false;
    }

    const start = program.start.getTime();
    const stop = program.stop.getTime();
    return stop > windowStart.getTime() && start < windowEnd.getTime();
  });
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

function renderGuide(channel) {
  ui.guideTimeline.innerHTML = "";
  ui.nowNext.innerHTML = "";
  const programs = getProgramsForChannel(channel);
  const upcoming = getUpcomingPrograms(programs);
  const current = getCurrentProgram(programs);
  const guideChannels = getVisibleGuideChannels();
  const windowStart = startOfGuideWindow();
  const windowEnd = addMinutes(windowStart, GUIDE_SLOT_MINUTES * GUIDE_SLOT_COUNT);

  if (!guideChannels.length) {
    ui.guideStatus.textContent = "No channels available";
    return;
  }

  const header = document.createElement("div");
  header.className = "guide-grid-header";
  header.innerHTML = `
    <div class="guide-channel-header">Ch</div>
    <div class="guide-time-header">
      ${Array.from({ length: GUIDE_SLOT_COUNT }, (_, index) => {
        const slotStart = addMinutes(windowStart, index * GUIDE_SLOT_MINUTES);
        return `<span>${formatTimeCompact(slotStart)}</span>`;
      }).join("")}
    </div>
  `;
  ui.guideTimeline.appendChild(header);

  let matchedRows = 0;
  guideChannels.forEach((guideChannel, index) => {
    const row = document.createElement("div");
    const rowPrograms = getProgramsForChannel(guideChannel);
    const windowPrograms = getProgramsInWindow(rowPrograms, windowStart, windowEnd);
    if (windowPrograms.length) {
      matchedRows += 1;
    }

    row.className = `guide-grid-row${guideChannel === channel ? " active" : ""}`;

    const track = document.createElement("div");
    track.className = "guide-track";

    windowPrograms.forEach((program) => {
      const clippedStart = Math.max(program.start.getTime(), windowStart.getTime());
      const clippedEnd = Math.min(program.stop.getTime(), windowEnd.getTime());
      const totalMinutes = GUIDE_SLOT_MINUTES * GUIDE_SLOT_COUNT;
      const leftPercent = ((clippedStart - windowStart.getTime()) / 60_000 / totalMinutes) * 100;
      const widthPercent = ((clippedEnd - clippedStart) / 60_000 / totalMinutes) * 100;
      const block = document.createElement("article");
      const isCurrent = current === program || (program.start <= new Date() && program.stop >= new Date());
      block.className = `guide-block${isCurrent ? " current" : ""}`;
      block.style.left = `${leftPercent}%`;
      block.style.width = `${Math.max(widthPercent, 10)}%`;
      block.innerHTML = `
        <strong>${program.title}</strong>
        <span>${formatTime(program.start)} - ${formatTime(program.stop)}</span>
      `;
      track.appendChild(block);
    });

    if (!windowPrograms.length) {
      const empty = document.createElement("div");
      empty.className = "guide-empty";
      empty.textContent = "No listings";
      track.appendChild(empty);
    }

    row.innerHTML = `
      <div class="guide-grid-channel">
        <span class="guide-grid-number">${getDisplayChannelNumber(guideChannel, state.filteredChannels.indexOf(guideChannel))}</span>
        <div>
          <strong>${guideChannel.title}</strong>
          <span>${guideChannel.group}</span>
        </div>
      </div>
    `;
    row.appendChild(track);
    ui.guideTimeline.appendChild(row);
  });

  ui.guideStatus.textContent = `${matchedRows}/${guideChannels.length} channels • ${formatTime(windowStart)} - ${formatTime(windowEnd)}`;

  const nextProgram = upcoming.find((program) => program !== current);
  const infoStrip = document.createElement("article");
  infoStrip.className = "now-next-strip";
  infoStrip.innerHTML = `
    <div class="now-next-segment current">
      <p class="program-time">Now</p>
      <h3>${current?.title || "No current listing"}</h3>
      <p class="program-copy">${current?.desc || "No description"}</p>
    </div>
    <div class="now-next-segment">
      <p class="program-time">Next</p>
      <h3>${nextProgram?.title || "No next listing"}</h3>
      <p class="program-copy">${nextProgram ? `${formatTime(nextProgram.start)} start` : "No future listing"}</p>
    </div>
  `;
  ui.nowNext.appendChild(infoStrip);
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
  const programs = getProgramsForChannel(channel);
  const current = getCurrentProgram(programs);

  ui.channelBadge.textContent = getDisplayChannelNumber(channel, state.activeIndex);
  ui.channelTitle.textContent = channel.title;
  ui.channelMeta.textContent = current
    ? `${channel.group} • ${current.title} until ${formatTime(current.stop)}`
    : `${channel.group} • No live guide data`;
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
    updateHero(state.filteredChannels[state.activeIndex]);
    renderGuide(state.filteredChannels[state.activeIndex]);
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
  updateHero(channel);
  renderGuide(channel);
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
    epgUrl: DEFAULT_EPG_URL,
    streamHeaders: ""
  };

  try {
    const response = await fetch("/api/config");
    const data = await response.json().catch(() => ({}));
    if (response.ok && data?.defaults) {
      defaults = {
        playlistUrl: data.defaults.playlistUrl || DEFAULT_PLAYLIST_URL,
        epgUrl: data.defaults.epgUrl || DEFAULT_EPG_URL,
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
    epgUrl: saved.epgUrl || defaults.epgUrl || "",
    streamHeaders: saved.streamHeaders || defaults.streamHeaders || ""
  };

  ui.playlistUrl.value = state.sources.playlistUrl;
  ui.epgUrl.value = state.sources.epgUrl;
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

async function loadEpg() {
  if (!state.sources.epgUrl) {
    state.guide.clear();
    ui.guideStatus.textContent = "No XMLTV URL configured";
    return;
  }

  const params = new URLSearchParams({ url: state.sources.epgUrl });
  if (state.sources.playlistUrl) {
    params.set("playlist_url", state.sources.playlistUrl);
  }
  const response = await fetch(`/api/epg?${params.toString()}`);

  if (!response.ok) {
    const data = await response.json();
    throw new Error(data.error || "Failed to load EPG");
  }

  const data = await response.json();
  state.guide = parseEpg(data.guide);
}

async function reloadSources() {
  ui.guideStatus.textContent = "Loading sources...";

  try {
    await Promise.all([loadPlaylist(), loadEpg()]);
    if (state.filteredChannels[state.activeIndex]) {
      renderGuide(state.filteredChannels[state.activeIndex]);
      updateHero(state.filteredChannels[state.activeIndex]);
    }
    showToast("Sources refreshed");
  } catch (error) {
    ui.guideStatus.textContent = error.message;
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
    state.sources.epgUrl = ui.epgUrl.value.trim();
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
    } else if (event.key === "ArrowRight") {
      state.guideOffset += 1;
      const current = state.filteredChannels[state.activeIndex];
      if (current) {
        renderGuide(current);
      }
    } else if (event.key === "ArrowLeft") {
      state.guideOffset = Math.max(0, state.guideOffset - 1);
      const current = state.filteredChannels[state.activeIndex];
      if (current) {
        renderGuide(current);
      }
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

  if (!state.sources.playlistUrl && !state.sources.epgUrl) {
    ui.settingsDialog.showModal();
    ui.guideStatus.textContent = "Add your sources to begin";
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
