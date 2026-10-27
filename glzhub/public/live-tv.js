// Authenticated Hub UI; receivers receive only the selected media URL, never Hub credentials.
export function liveChannels(playlists, playlistId = "") {
  return playlists.filter(p => p.is_published !== false && (!playlistId || p.id === playlistId))
    .flatMap(p => [...(p.playlist_items || [])].sort((a, b) => Number(a.position || 0) - Number(b.position || 0))
      .filter(item => item.metadata?.hidden !== true && item.metadata?.hidden !== "true" && item.metadata?.radio !== true && item.metadata?.radio !== "true")
      .map(item => ({ ...item, key: `${p.id}:${item.id}`, group: item.metadata?.group || p.title || "TV", logo: item.metadata?.tvg_logo || item.metadata?.tvgLogo || p.artwork_url || "" })));
}

export function mediaType(url) {
  const path = new URL(url).pathname.toLowerCase();
  if (path.endsWith(".mpd")) return "application/dash+xml";
  if (path.endsWith(".mp4") || path.endsWith(".m4v")) return "video/mp4";
  if (path.endsWith(".webm")) return "video/webm";
  return "application/x-mpegURL";
}

export function createLiveTV({ api, stopPreview }) {
  const $ = id => document.getElementById(id);
  const video = $("liveVideo");
  let playlists = [], channels = [], selected = null, hls = null, generation = 0, catalogGeneration = 0;
  let context = null, remote = null, controller = null, castLoading = null, visible = false;
  let castBusy = false;
  const status = message => { $("liveStatus").textContent = message; };
  const session = () => context?.getCurrentSession();
  const connected = () => Boolean(session());
  const option = (value, label) => new Option(label, value);

  function stopLocal() {
    generation++;
    hls?.destroy(); hls = null;
    video.pause(); video.removeAttribute("src"); video.load();
  }

  function render() {
    const query = $("liveSearch").value.trim().toLowerCase();
    const group = $("liveGroup").value;
    const shown = channels.filter(c => (!group || c.group === group) &&
      `${c.title} ${c.metadata?.tvg_chno || ""} ${c.group}`.toLowerCase().includes(query));
    const list = $("liveChannels"); list.replaceChildren();
    $("liveCount").textContent = `${shown.length} channels`;
    $("liveEmpty").hidden = shown.length > 0;
    for (const channel of shown) {
      const button = document.createElement("button");
      button.type = "button"; button.className = "live-channel";
      button.setAttribute("aria-pressed", String(selected?.key === channel.key));
      const artwork = document.createElement("span"); artwork.className = "live-artwork";
      artwork.textContent = String(channel.title || "TV").slice(0, 2).toUpperCase();
      try {
        if (channel.logo && new URL(channel.logo).protocol === "https:") {
          const img = document.createElement("img"); img.src = channel.logo; img.alt = ""; img.loading = "lazy"; img.referrerPolicy = "no-referrer";
          img.addEventListener("error", () => img.remove()); artwork.append(img);
        }
      } catch { /* Keep initials for invalid artwork URLs. */ }
      const text = document.createElement("span"); text.className = "live-channel-label";
      const title = document.createElement("strong"); title.textContent = `${channel.metadata?.tvg_chno ? `${channel.metadata.tvg_chno} · ` : ""}${channel.title || "Untitled channel"}`;
      const detail = document.createElement("small"); detail.textContent = channel.group;
      text.append(title, detail); button.append(artwork, text);
      button.addEventListener("click", () => selectChannel(channel)); list.append(button);
    }
  }

  function filterPlaylist() {
    channels = liveChannels(playlists, $("livePlaylist").value);
    $("liveGroup").replaceChildren(option("", "All groups"), ...[...new Set(channels.map(c => c.group))].sort().map(g => option(g, g)));
    render();
  }

  async function load() {
    const request = ++catalogGeneration;
    $("liveReload").disabled = true; status("Loading your published TV playlists…");
    try {
      const result = await api("/api/v1/admin/playlists");
      if (request !== catalogGeneration) return;
      playlists = result.playlists || [];
      const previous = $("livePlaylist").value;
      $("livePlaylist").replaceChildren(option("", "All published playlists"), ...playlists.filter(p => p.is_published !== false).map(p => option(p.id, p.title)));
      if (playlists.some(p => p.id === previous)) $("livePlaylist").value = previous;
      filterPlaylist();
      if (!selected) status(channels.length ? "Choose a channel to watch or cast." : "Publish a TV playlist in Playlist Studio to start watching.");
      else updateCast();
    } catch { if (request === catalogGeneration) status("Could not load channels. Check your sign-in and tap Refresh."); }
    finally { if (request === catalogGeneration) $("liveReload").disabled = false; }
  }

  async function playLocal() {
    stopLocal();
    if (!selected || !visible) return;
    const token = generation;
    let url, type;
    try {
      url = new URL(selected.media_url);
      if (!["http:", "https:"].includes(url.protocol) || url.username || url.password) throw new Error();
      type = mediaType(url.href);
    } catch { status("This channel has an invalid or unsupported stream URL."); return; }
    if (location.protocol === "https:" && url.protocol === "http:") {
      status("This channel uses HTTP. Phone playback needs an HTTPS stream; you can try Cast to play it on the TV."); return;
    }
    if (type === "application/dash+xml") { status("This DASH channel can be tried using Cast. Phone playback supports HLS and direct video."); return; }
    status("Connecting…");
    const play = () => video.play().catch(() => { if (token === generation) status("Tap Play on the video to start."); });
    if (type === "application/x-mpegURL" && !video.canPlayType(type) && window.Hls?.isSupported()) {
      hls = new window.Hls();
      hls.on(window.Hls.Events.MANIFEST_PARSED, () => { if (token === generation) play(); });
      hls.on(window.Hls.Events.ERROR, (_event, data) => {
        if (data.fatal && token === generation) { stopLocal(); status("Unable to play this channel. The stream may be offline, require special headers, or block browser playback (CORS)."); }
      });
      hls.loadSource(url.href); hls.attachMedia(video);
    } else if (type === "application/x-mpegURL" && !video.canPlayType(type)) {
      status("HLS playback could not load. Refresh or try Cast.");
    } else { video.src = url.href; play(); }
  }

  async function selectChannel(channel) {
    selected = channel; stopPreview();
    $("liveNow").textContent = channel.title || "Untitled channel";
    $("liveStop").disabled = false; render();
    if (connected()) await castSelected(); else await playLocal();
  }

  async function castSelected() {
    if (!selected || !session()) return;
    stopLocal(); const token = generation;
    try {
      const url = new URL(selected.media_url);
      if (!["https:", "http:"].includes(url.protocol) || url.username || url.password) throw new Error();
      const media = new chrome.cast.media.MediaInfo(url.href, mediaType(url.href));
      media.streamType = chrome.cast.media.StreamType.LIVE;
      media.metadata = new chrome.cast.media.GenericMediaMetadata();
      media.metadata.title = selected.title || "Glz TV";
      media.metadata.subtitle = selected.group;
      if (selected.logo && new URL(selected.logo).protocol === "https:") media.metadata.images = [new chrome.cast.Image(selected.logo)];
      const request = new chrome.cast.media.LoadRequest(media); request.autoplay = true;
      castBusy = true; $("liveChannels").inert = true; updateCast(); status("Loading channel on TV…");
      await session().loadMedia(request);
      if (token === generation) status(`Casting to ${session()?.getCastDevice().friendlyName || "TV"}. Choose another channel here to switch.`);
    } catch { if (token === generation) status("TV could not load this stream. Check that the TV can reach it and that its format, CORS, and authentication support casting."); }
    finally { castBusy = false; $("liveChannels").inert = false; updateCast(); }
  }

  function updateCast() {
    const isConnected = connected();
    $("liveCast").disabled = !context || castBusy;
    $("liveCast").textContent = isConnected ? `Casting · ${session().getCastDevice().friendlyName}` : "Cast to TV";
    $("liveDisconnect").hidden = !isConnected;
    $("liveRemotePause").hidden = !isConnected;
    $("liveRemotePause").disabled = castBusy || !remote?.isMediaLoaded || !remote?.canPause;
    $("liveRemotePause").textContent = remote?.isPaused ? "Resume TV" : "Pause TV";
  }

  function initCast() {
    if (context || !window.cast?.framework || !window.chrome?.cast) return;
    context = cast.framework.CastContext.getInstance();
    context.setOptions({ receiverApplicationId: chrome.cast.media.DEFAULT_MEDIA_RECEIVER_APP_ID, autoJoinPolicy: chrome.cast.AutoJoinPolicy.ORIGIN_SCOPED });
    remote = new cast.framework.RemotePlayer(); controller = new cast.framework.RemotePlayerController(remote);
    controller.addEventListener(cast.framework.RemotePlayerEventType.ANY_CHANGE, updateCast);
    context.addEventListener(cast.framework.CastContextEventType.SESSION_STATE_CHANGED, event => {
      if (event.sessionState === cast.framework.SessionState.SESSION_STARTED || event.sessionState === cast.framework.SessionState.SESSION_RESUMED) {
        stopLocal(); status(`Connected to ${session()?.getCastDevice().friendlyName || "TV"}. Choose a channel to cast.`);
      } else if (event.sessionState === cast.framework.SessionState.SESSION_ENDED) status("Disconnected from TV. Choose a channel to watch on your phone.");
      updateCast();
    });
    $("liveCast").disabled = false; $("liveCastHelp").textContent = "On Android, open GlzHub in Chrome. Connect your phone and TV to the same Wi-Fi, choose a channel, then tap Cast to TV.";
    updateCast();
  }

  function loadCast() {
    if (castLoading) return;
    window.__onGCastApiAvailable = available => {
      if (available) initCast();
      else $("liveCastHelp").textContent = "Casting is unavailable in this browser. Try Chrome on Android or desktop. iPhone browsers do not support this Cast sender.";
    };
    if (window.cast?.framework) { initCast(); return; }
    const script = document.createElement("script"); script.src = "https://www.gstatic.com/cv/js/sender/v1/cast_sender.js?loadCastFramework=1"; script.async = true;
    script.onerror = () => { castLoading = null; script.remove(); $("liveCastHelp").textContent = "Google Cast could not load. Reopen Live TV to retry; check your connection or content blocker."; };
    castLoading = script; document.head.append(script);
    setTimeout(() => { if (!context) $("liveCastHelp").textContent = "Cast is unavailable. Try Chrome on Android or desktop with your TV on the same Wi-Fi. iPhone browsers do not support this Cast sender."; }, 12000);
  }

  $("liveCast").addEventListener("click", async () => {
    try { if (!connected()) await context.requestSession(); if (selected) await castSelected(); }
    catch (error) { if (error !== "cancel" && error?.code !== "cancel") status("Cast connection did not complete. Check the TV is online and on the same Wi-Fi, then try again."); }
  });
  $("liveDisconnect").addEventListener("click", () => context?.endCurrentSession(true));
  $("liveRemotePause").addEventListener("click", () => controller?.playOrPause());
  $("liveStop").addEventListener("click", () => {
    stopLocal(); if (connected()) controller?.stop();
    selected = null; $("liveNow").textContent = "Choose a channel"; $("liveStop").disabled = true; render(); status("Playback stopped.");
  });
  $("livePlaylist").addEventListener("change", filterPlaylist);
  $("liveGroup").addEventListener("change", render);
  $("liveSearch").addEventListener("input", render);
  $("liveReload").addEventListener("click", load);
  video.addEventListener("playing", () => { if (!connected()) status("Playing on this device."); });
  video.addEventListener("error", () => { if (video.hasAttribute("src")) status("Stream unavailable or unsupported. Try another channel or Cast to TV."); });
  return {
    open() { visible = true; loadCast(); load(); },
    leave() { visible = false; stopLocal(); if (selected && !connected()) status("Phone playback stopped. Choose a channel to play again."); },
    reset() {
      visible = false; catalogGeneration++; stopLocal();
      if (connected()) context.endCurrentSession(true);
      selected = null; playlists = []; channels = [];
      $("livePlaylist").replaceChildren(option("", "All published playlists"));
      $("liveGroup").replaceChildren(option("", "All groups"));
      $("liveSearch").value = ""; $("liveNow").textContent = "Choose a channel";
      $("liveStop").disabled = true; $("liveReload").disabled = false; render(); status("Choose a channel to watch or cast.");
    }
  };
}
