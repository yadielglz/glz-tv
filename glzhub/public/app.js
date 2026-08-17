const state = {
  config: null, session: null, devices: [], apps: [], sites: [],
  experience: null, selectedSiteId: null, enrollments: [], stations: [], playlists: [], groups: []
};
const epgState = { doc: null, playlistId: "", dirty: false, sourceUrl: "" };
const inviteParams = new URLSearchParams(location.hash.replace(/^#/, ""));
const inviteToken = inviteParams.get("type") === "invite" ? inviteParams.get("access_token") : null;
const APP_CATALOG = [
  ["YouTube", "com.google.android.youtube.tv"],
  ["Netflix", "com.netflix.ninja"],
  ["MLB", "com.bamnetworks.mobile.android.gameday.atbat"],
  ["OleadaTV", "com.android.mgsandroid"],
  ["GLZ Radio", "com.glztech.radiostream"],
  ["GeeSports", "com.live.geesports"],
  ["Paramount+", "com.cbs.ott"],
  ["Disney+", "com.disney.disneyplus"],
  ["Peacock", "com.peacocktv.peacockandroid"],
  ["Spectrum TV", "com.TWCableTV"]
];
const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => [...document.querySelectorAll(selector)];

async function api(path, options = {}) {
  const headers = { "content-type": "application/json", ...(options.headers || {}) };
  if (state.session?.access_token) headers.authorization = `Bearer ${state.session.access_token}`;
  const response = await fetch(path, { ...options, headers });
  const result = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(result.error || `Request failed (${response.status})`);
  return result;
}

async function loadPublicConfig() {
  state.config = await api("/api/v1/public-config");
}

async function signIn(email, password) {
  const response = await fetch(`${state.config.supabaseUrl}/auth/v1/token?grant_type=password`, {
    method: "POST",
    headers: { "content-type": "application/json", apikey: state.config.publishableKey },
    body: JSON.stringify({ email, password })
  });
  const result = await response.json();
  if (!response.ok) throw new Error(result.error_description || result.msg || "Sign-in failed.");
  state.session = result;
  localStorage.setItem("glzhub_session", JSON.stringify(result));
}

function restoreSession() {
  try {
    const session = JSON.parse(localStorage.getItem("glzhub_session"));
    if (session?.access_token && session.expires_at * 1000 > Date.now()) state.session = session;
  } catch { }
}

function showApp() {
  $("#inviteView").classList.toggle("hidden", !inviteToken);
  $("#authView").classList.toggle("hidden", Boolean(state.session) || Boolean(inviteToken));
  $("#appView").classList.toggle("hidden", !state.session || Boolean(inviteToken));
  $("#signOut").classList.toggle("hidden", !state.session);
  $("#accountEmail").textContent = state.session?.user?.email || "Not signed in";
}

$("#inviteForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  $("#inviteError").textContent = "";
  const password = $("#invitePassword").value;
  if (password !== $("#invitePasswordConfirm").value) {
    $("#inviteError").textContent = "Passwords do not match.";
    return;
  }
  try {
    const response = await fetch(`${state.config.supabaseUrl}/auth/v1/user`, {
      method: "PUT",
      headers: {
        "content-type": "application/json",
        apikey: state.config.publishableKey,
        authorization: `Bearer ${inviteToken}`
      },
      body: JSON.stringify({ password })
    });
    const result = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(result.msg || result.message || "Could not set password.");
    history.replaceState(null, "", location.pathname);
    location.reload();
  } catch (error) {
    $("#inviteError").textContent = error.message;
  }
});

function showView(name) {
  toggleMobileNav(false);
  $("#mainAppHeader").classList.toggle("hidden", name === "device-editor");
  $("#devicesView").classList.toggle("hidden", name !== "devices");
  $("#deviceEditorView").classList.toggle("hidden", name !== "device-editor");
  $("#groupsView").classList.toggle("hidden", name !== "groups");
  $("#sitesView").classList.toggle("hidden", name !== "sites");
  $("#appsView").classList.toggle("hidden", name !== "apps");
  $("#experienceView").classList.toggle("hidden", name !== "experience");
  $("#radioView").classList.toggle("hidden", name !== "radio");
  $("#studioView").classList.toggle("hidden", name !== "studio");
  $("#epgView").classList.toggle("hidden", name !== "epg");
  $("#pairView").classList.toggle("hidden", name !== "pair");
  $("#pairButton").classList.toggle("hidden", name === "pair" || name === "device-editor");
  $("#pageTitle").textContent = name === "pair" ? "Pair a television" :
    name === "apps" ? "App management" : name === "groups" ? "Box Groups" : name === "sites" ? "Properties" :
      name === "experience" ? "Guest experience" : name === "radio" ? "Radio Streams" :
        name === "studio" ? "Playlist Studio" : name === "epg" ? "EPG Studio" : "Your TVs";
  $$(".nav").forEach((button) => button.classList.toggle("active", button.dataset.view === (name === "device-editor" ? "devices" : name)));
  if (name === "pair") loadPairingRequests();
  if (name === "radio") loadRadioStations();
  if (name === "studio") loadPlaylists();
  if (name === "epg") openEpgStudio();
  if (name === "groups") loadBoxGroups();
}

function isOnline(device) {
  return device.last_seen_at && Date.now() - new Date(device.last_seen_at).getTime() < 10 * 60_000;
}

function deviceActivity(device) {
  if (!isOnline(device)) return { label: "Offline", className: "offline" };
  if (["queued", "syncing"].includes(device.sync_status)) {
    return {
      label: device.sync_message || (device.sync_status === "queued" ? "Waiting for TV" : "Syncing"),
      className: "syncing",
      progress: Math.max(0, Math.min(100, Number(device.sync_progress) || 0))
    };
  }
  if (device.sync_status === "failed") {
    return { label: device.sync_message || "Sync failed", className: "sync-failed" };
  }
  if (device.activity_type === "channel" && device.activity_label) {
    return { label: `Watching · ${device.activity_label}`, className: "watching" };
  }
  if (device.activity_type === "radio" && device.activity_label) {
    return { label: `Listening · ${device.activity_label}`, className: "listening" };
  }
  if (device.activity_type === "app" && device.activity_label) {
    return { label: `App launched · ${device.activity_label}`, className: "app-active" };
  }
  return { label: "Home screen", className: "idle" };
}

function escapeHtml(value = "") {
  const div = document.createElement("div");
  div.textContent = value;
  return div.innerHTML;
}

let activeDeviceFilter = "all";
let deviceSearchQuery = "";

function renderDevices() {
  const total = state.devices.length;
  const onlineCount = state.devices.filter(isOnline).length;
  const syncingCount = state.devices.filter((d) => isOnline(d) && ["queued", "syncing"].includes(d.sync_status)).length;
  const attentionCount = state.devices.filter((d) => Boolean(d.last_error)).length;

  $("#deviceCount").textContent = total;
  $("#onlineCount").textContent = onlineCount;
  $("#attentionCount").textContent = attentionCount;

  const fleetScore = total > 0 ? Math.round((onlineCount / total) * 100) : 100;
  const badge = $("#fleetScoreBadge");
  if (badge) {
    badge.textContent = `${fleetScore}% HEALTH`;
    badge.style.color = fleetScore >= 80 ? "var(--accent-cyan)" : (fleetScore >= 50 ? "var(--amber)" : "var(--danger)");
  }

  const query = deviceSearchQuery.trim().toLowerCase();
  const filtered = state.devices.filter((device) => {
    const isDevOnline = isOnline(device);
    const isDevSyncing = isDevOnline && ["queued", "syncing"].includes(device.sync_status);

    if (activeDeviceFilter === "online" && !isDevOnline) return false;
    if (activeDeviceFilter === "syncing" && !isDevSyncing) return false;
    if (activeDeviceFilter === "offline" && isDevOnline) return false;
    if (activeDeviceFilter === "attention" && !device.last_error) return false;

    if (!query) return true;
    const siteName = state.sites.find((s) => s.id === device.site_id)?.name || "";
    const text = [device.name, device.guest_name, device.room_number, device.model, siteName, device.platform].filter(Boolean).join(" ").toLowerCase();
    return text.includes(query);
  });

  $("#emptyState").classList.toggle("hidden", total > 0);

  if (total === 0) {
    $("#deviceGrid").innerHTML = "";
    return;
  }

  if (filtered.length === 0) {
    $("#deviceGrid").innerHTML = `<div style="padding:40px;text-align:center;color:var(--text-muted);font-size:14px;">No screens match the current filter.</div>`;
    return;
  }

  $("#deviceGrid").innerHTML = `<div class="list-head"><span>Device</span><span>Property</span><span>Status</span><span>Activity</span><span>App</span><span>Last contact</span></div>` +
    filtered.map((device) => {
      const activity = deviceActivity(device);
      const isDevOnline = isOnline(device);
      const statusClass = isDevOnline ? (device.sync_status === "syncing" ? "syncing" : "") : "offline";
      const statusLabel = isDevOnline ? (device.sync_status === "syncing" ? "SYNCING" : "ONLINE") : "OFFLINE";

      return `
    <button class="device-row" data-device-id="${device.id}">
      <span class="device-identity">
        <span class="screen-icon"></span>
        <span>
          <strong>${escapeHtml(device.name)}</strong>
          <small>Welcome, ${escapeHtml(device.guest_name)}${device.room_number ? ` · Rm ${escapeHtml(device.room_number)}` : ""}</small>
        </span>
      </span>
      <span data-label="Property">${escapeHtml(state.sites.find((site) => site.id === device.site_id)?.name || "Unassigned")}</span>
      <span><span class="status ${statusClass}">${statusLabel}</span></span>
      <span data-label="Activity"><span class="activity ${activity.className}">${escapeHtml(activity.label)}${activity.progress == null ? "" : ` · ${activity.progress}%`}</span>${activity.progress == null ? "" : `<span class="sync-track"><span style="width:${activity.progress}%"></span></span>`}</span>
      <span data-label="App">v${escapeHtml(device.app_version || "1.0")}</span>
      <span data-label="Last contact">${device.last_seen_at ? new Date(device.last_seen_at).toLocaleString() : "Never"}${device.last_error ? `<small class="attention" style="color:var(--danger);display:block;margin-top:2px;">${escapeHtml(device.last_error)}</small>` : ""}</span>
    </button>`;
    }).join("");

  $$(".device-row").forEach((row) => row.addEventListener("click", () => openDevice(row.dataset.deviceId)));
}

let deviceActivityRefresh = false;
async function refreshDeviceActivity() {
  if (!state.session || $("#devicesView").classList.contains("hidden") || deviceActivityRefresh) return;
  deviceActivityRefresh = true;
  try {
    const result = await api("/api/v1/admin/devices");
    state.devices = result.devices;
    renderDevices();
  } finally {
    deviceActivityRefresh = false;
  }
}

setInterval(() => refreshDeviceActivity().catch(() => { }), 5_000);

async function loadDevices() {
  const [deviceResult, appResult, siteResult, groupResult] = await Promise.all([
    api("/api/v1/admin/devices"), api("/api/v1/admin/apps"), api("/api/v1/admin/sites"), api("/api/v1/admin/box-groups")
  ]);
  state.devices = deviceResult.devices;
  state.apps = appResult.apps;
  state.sites = siteResult.sites;
  state.groups = groupResult.groups || [];
  if (!state.sites.some((site) => site.id === state.selectedSiteId)) {
    state.selectedSiteId = state.sites[0]?.id || null;
  }
  await loadExperience();
  renderDevices();
  renderApps();
  renderSites();
  renderSiteSelectors();
  renderExperience();
  renderBoxGroups();
}

async function loadPairingRequests() {
  if (!state.session || $("#pairView").classList.contains("hidden")) return;
  try {
    const result = await api("/api/v1/admin/enrollments");
    state.enrollments = result.enrollments || [];
    $("#pairingScanStatus").textContent = state.enrollments.length
      ? `${state.enrollments.length} waiting`
      : "No requests yet";
    $("#pairingRequestList").innerHTML = state.enrollments.map((enrollment) => `
      <div class="pairing-request">
        <span class="pairing-request-info">
          <strong>${escapeHtml(enrollment.model || "Android TV")}</strong>
          <small>${escapeHtml(enrollment.platform || "Android TV")} · v${escapeHtml(enrollment.app_version || "unknown")} · requested ${new Date(enrollment.created_at).toLocaleTimeString()}</small>
        </span>
        <button type="button" class="primary" data-pairing-code="${escapeHtml(enrollment.pairing_code)}">Pair</button>
      </div>`).join("");
    $$("[data-pairing-code]").forEach((button) => button.addEventListener("click", () => {
      $("#pairingCode").value = button.dataset.pairingCode;
      $("#pairDeviceName").value ||= state.enrollments.find(
        (item) => item.pairing_code === button.dataset.pairingCode
      )?.model || "New TV";
      $("#pairDeviceName").focus();
    }));
  } catch (error) {
    $("#pairingScanStatus").textContent = "Discovery unavailable";
  }
}

setInterval(loadPairingRequests, 3_000);

async function loadExperience() {
  if (!state.selectedSiteId) {
    state.experience = null;
    return;
  }
  const result = await api(`/api/v1/admin/guest-experience?siteId=${encodeURIComponent(state.selectedSiteId)}`);
  state.experience = result.profile;
}

function renderSiteSelectors() {
  const options = state.sites.map((site) =>
    `<option value="${site.id}">${escapeHtml(site.name)}</option>`
  ).join("");
  $("#experienceSite").innerHTML = options;
  $("#experienceSite").value = state.selectedSiteId || "";
  $("#pairSite").innerHTML = `<option value="">Unassigned</option>${options}`;
}

let siteSearchQuery = "";
$("#siteSearchInput")?.addEventListener("input", (e) => {
  siteSearchQuery = e.target.value.trim().toLowerCase();
  renderSites();
});

function renderSites() {
  const container = $("#siteList");
  if (!container) return;
  const filtered = state.sites.filter((site) => {
    if (!siteSearchQuery) return true;
    return [site.name, site.address].filter(Boolean).join(" ").toLowerCase().includes(siteSearchQuery);
  });

  $("#siteEmpty").classList.toggle("hidden", state.sites.length > 0);
  if (!filtered.length && state.sites.length > 0) {
    container.innerHTML = `<div style="text-align:center;color:var(--text-muted);padding:30px;">No properties match "${escapeHtml(siteSearchQuery)}".</div>`;
    return;
  }

  container.innerHTML = filtered.map((site) => {
    const devices = state.devices.filter((device) => device.site_id === site.id);
    const onlineCount = devices.filter(isOnline).length;
    return `
      <article class="site-card" data-site-id="${site.id}">
        <div class="card-main-info">
          <div class="site-glyph-lg">⌂</div>
          <div class="card-details">
            <h4>${escapeHtml(site.name)}</h4>
            <p>${escapeHtml(site.address || "No address entered")}</p>
            <div class="card-meta-pills">
              <span class="meta-pill">${devices.length} Screen${devices.length === 1 ? "" : "s"}</span>
              <span class="status ${onlineCount > 0 ? "" : "offline"}">${onlineCount} ONLINE</span>
            </div>
          </div>
        </div>
        <div class="card-action-bar">
          <button type="button" class="secondary open-site-experience" data-site-id="${site.id}" title="Open guest portal & branding">✦ Guest Portal</button>
          <button type="button" class="secondary filter-site-tvs" data-site-id="${site.id}" title="View screens for this property">📺 Screens</button>
          <button type="button" class="secondary edit-site-btn" data-site-id="${site.id}">Edit</button>
        </div>
      </article>
    `;
  }).join("");

  $$(".open-site-experience").forEach((btn) => btn.addEventListener("click", async (e) => {
    e.stopPropagation();
    state.selectedSiteId = btn.dataset.siteId;
    $("#experienceSite").value = state.selectedSiteId;
    await loadExperience();
    renderExperience();
    showView("experience");
  }));

  $$(".filter-site-tvs").forEach((btn) => btn.addEventListener("click", (e) => {
    e.stopPropagation();
    const site = state.sites.find((s) => s.id === btn.dataset.siteId);
    if (site) {
      deviceSearchQuery = site.name;
      const devSearchInput = $("#deviceSearch");
      if (devSearchInput) devSearchInput.value = site.name;
      renderDevices();
      showView("devices");
    }
  }));

  $$(".edit-site-btn").forEach((btn) => btn.addEventListener("click", (e) => {
    e.stopPropagation();
    openSite(btn.dataset.siteId);
  }));
}

function updateBrandingPreview() {
  const name = $("#propertyName").value.trim() || "GLZ Hotel & Resort";
  const msg = $("#welcomeMessage").value.trim() || "Relax, explore, and enjoy your stay.";
  const logo = $("#logoUrl").value.trim();
  const hero = $("#heroImageUrl").value.trim();

  $("#previewPropertyName").textContent = name;
  $("#previewWelcomeMsg").textContent = msg;

  const logoImg = $("#logoPreviewImg");
  if (logoImg) {
    if (logo) {
      logoImg.src = logo;
      logoImg.classList.remove("hidden");
    } else {
      logoImg.classList.add("hidden");
    }
  }

  const heroBackdrop = $("#heroPreviewBackdrop");
  if (heroBackdrop) {
    if (hero) {
      heroBackdrop.style.backgroundImage = `linear-gradient(135deg, rgba(15,32,56,0.6) 0%, rgba(6,12,22,0.9) 100%), url('${hero}')`;
    } else {
      heroBackdrop.style.backgroundImage = "";
    }
  }
}

function renderServiceTiles(services = []) {
  const container = $("#serviceTilesList");
  if (!container) return;
  if (!services.length) {
    container.innerHTML = `<div style="grid-column:1/-1;text-align:center;color:var(--text-muted);padding:24px;border:1px dashed var(--line);border-radius:var(--radius-md);font-size:13px;">No interactive guest service tiles configured. Click "＋ Add Service Tile" above to create amenities, spa, dining, or room service cards.</div>`;
    return;
  }
  container.innerHTML = services.map((s, idx) => `
    <div class="service-tile-card" data-index="${idx}">
      <div class="service-tile-header">
        <input class="service-tile-title-input service-input-title" value="${escapeHtml(s.title || "")}" placeholder="Service Name (e.g. Room Service)" maxlength="100">
        <button type="button" class="danger-button delete-service-tile small-btn" data-index="${idx}">Delete</button>
      </div>
      <div class="service-tile-field">
        <label>Short Description</label>
        <input class="service-input-subtitle" value="${escapeHtml(s.subtitle || "")}" placeholder="e.g. View dining menu & order" maxlength="180">
      </div>
      <div class="service-tile-field">
        <label>Action Link (HTTPS / Internal)</label>
        <div style="display:flex;gap:8px;">
          <input class="service-input-url" value="${escapeHtml(s.actionUrl || "")}" placeholder="https://hotel.example/menu" style="flex:1;">
          ${s.actionUrl ? `<button type="button" class="secondary test-service-url small-btn" data-url="${escapeHtml(s.actionUrl)}">↗ Test</button>` : ""}
        </div>
      </div>
    </div>
  `).join("");

  $$(".delete-service-tile").forEach((btn) => btn.addEventListener("click", () => {
    const current = getServiceTilesFromUI();
    current.splice(Number(btn.dataset.index), 1);
    renderServiceTiles(current);
  }));

  $$(".test-service-url").forEach((btn) => btn.addEventListener("click", () => {
    if (btn.dataset.url) window.open(btn.dataset.url, "_blank", "noopener,noreferrer");
  }));
}

function getServiceTilesFromUI() {
  return $$(".service-tile-card").map((card) => ({
    title: card.querySelector(".service-input-title")?.value.trim() || "Service",
    subtitle: card.querySelector(".service-input-subtitle")?.value.trim() || null,
    actionUrl: card.querySelector(".service-input-url")?.value.trim() || null
  })).filter((s) => s.title);
}

function renderExperience() {
  const profile = state.experience || {};
  const currentSite = state.sites.find((s) => s.id === state.selectedSiteId);
  const assignedDevices = state.devices.filter((d) => d.site_id === state.selectedSiteId);
  const onlineAssigned = assignedDevices.filter(isOnline).length;

  $("#experiencePropertyTitle").textContent = currentSite ? `${currentSite.name} Experience` : "Property Guest Experience";
  const screensCountEl = $("#experienceScreensCount");
  if (screensCountEl) screensCountEl.textContent = `${assignedDevices.length} Screen${assignedDevices.length === 1 ? "" : "s"} Assigned`;
  const onlineCountEl = $("#experienceOnlineCount");
  if (onlineCountEl) {
    onlineCountEl.textContent = `${onlineAssigned} ONLINE`;
    onlineCountEl.className = `status ${onlineAssigned > 0 ? "" : "offline"}`;
  }

  $("#propertyName").value = profile.property_name || (currentSite ? currentSite.name : "GLZ Hotel");
  $("#welcomeMessage").value = profile.welcome_message || "Relax, explore, and enjoy your stay.";
  $("#logoUrl").value = profile.logo_url || "";
  $("#heroImageUrl").value = profile.hero_image_url || "";
  $("#wifiName").value = profile.wifi_name || "";
  $("#wifiInstructions").value = profile.wifi_instructions || "";
  $("#checkoutTime").value = profile.checkout_time || "";
  $("#frontDesk").value = profile.front_desk || "";
  $("#noticeTitle").value = profile.notice_title || "";
  $("#noticeBody").value = profile.notice_body || "";
  
  updateBrandingPreview();
  renderServiceTiles(profile.services || []);
}

function renderApps() {
  $("#appEmpty").classList.toggle("hidden", state.apps.length > 0);
  $("#appList").innerHTML = state.apps.map((app) => `<article class="app-row" data-app-id="${app.id}">
    <span class="app-glyph">${escapeHtml(app.name.slice(0, 1).toUpperCase())}</span>
    <span><strong>${escapeHtml(app.name)}</strong><small>${escapeHtml(app.package_name)}</small></span>
    <span class="source-badge">${app.source_type === "play_store" ? "PLAY STORE" : "REPOSITORY"}</span>
    <span>${escapeHtml(app.version_name || "Latest")}</span>
    <button type="button" class="secondary edit-app" data-app-id="${app.id}">Edit</button>
  </article>`).join("");
  $$(".edit-app").forEach((button) => button.addEventListener("click", () => openApp(button.dataset.appId)));
}

function openApp(id = "") {
  const app = state.apps.find((item) => item.id === id);
  $("#appForm").reset();
  $("#appId").value = app?.id || "";
  $("#appDialogTitle").textContent = app ? "Edit managed app" : "Add a managed app";
  $("#appName").value = app?.name || "";
  $("#appPackage").value = app?.package_name || "";
  $("#appSource").value = app?.source_type || "play_store";
  $("#appVersion").value = app?.version_name || "";
  $("#appUrl").value = app?.source_url || "";
  $("#appSha").value = app?.sha256 || "";
  $("#deleteApp").classList.toggle("hidden", !app);
  $("#saveApp").textContent = app ? "Save changes" : "Add to library";
  $("#appError").textContent = "";
  $("#appDialog").showModal();
}

function openDevice(id) {
  const device = state.devices.find((item) => item.id === id);
  if (!device) return;
  $("#deviceId").value = device.id;
  $("#configVersion").value = device.config_version;

  $("#editorDeviceTitle").textContent = device.name || "Untitled Screen";
  const isDevOnline = isOnline(device);
  const statusEl = $("#editorDeviceStatus");
  if (statusEl) {
    statusEl.className = `status ${isDevOnline ? (device.sync_status === "syncing" ? "syncing" : "") : "offline"}`;
    statusEl.textContent = isDevOnline ? (device.sync_status === "syncing" ? "SYNCING" : "ONLINE") : "OFFLINE";
  }
  const modelEl = $("#editorDeviceModel");
  if (modelEl) modelEl.textContent = device.model || "Android TV";
  const siteEl = $("#editorDeviceSite");
  const siteObj = state.sites.find((s) => s.id === device.site_id);
  if (siteEl) siteEl.textContent = siteObj?.name || "Unassigned Property";
  const versionEl = $("#editorDeviceVersion");
  if (versionEl) versionEl.textContent = `App v${device.app_version || "1.0"}`;

  $("#deviceName").value = device.name || "";
  $("#guestName").value = device.guest_name || "";
  $("#deviceSite").innerHTML = `<option value="">Unassigned</option>` + state.sites.map((site) =>
    `<option value="${site.id}">${escapeHtml(site.name)}</option>`
  ).join("");
  $("#deviceSite").value = device.site_id || "";
  $("#deviceAssignedPlaylist").innerHTML = `<option value="">Default (All Published TV Playlists)</option>` + (state.playlists || []).map((pl) =>
    `<option value="${pl.id}">${escapeHtml(pl.title)} (${(pl.playlist_items || []).length} channels)</option>`
  ).join("");
  $("#deviceAssignedPlaylist").value = device.assigned_playlist_id || "";
  const assignedPlaylistId = device.assigned_playlist_id || state.groups.find((group) => group.id === device.box_group_id)?.playlist_id;
  const previewPlaylists = assignedPlaylistId
    ? state.playlists.filter((playlist) => playlist.id === assignedPlaylistId)
    : state.playlists;
  const previewChannels = previewPlaylists.flatMap((playlist) => (playlist.playlist_items || []).map((item) => {
    const metadata = item.metadata || {};
    const channelId = metadata.tvg_id || item.title;
    const label = `${metadata.tvg_chno ? `${metadata.tvg_chno} · ` : ""}${item.title}`;
    return `<option value="${escapeHtml(channelId)}">${escapeHtml(label)}</option>`;
  }));
  $("#homePreviewChannel").innerHTML = `<option value="">No live preview</option>${previewChannels.join("")}`;
  $("#homePreviewChannel").value = device.home_preview_channel_id || "";
  $("#deviceBoxGroup").innerHTML = `<option value="">No box group</option>` + state.groups.map((group) => `<option value="${group.id}">${escapeHtml(group.name)}</option>`).join("");
  $("#deviceBoxGroup").value = device.box_group_id || "";
  $("#editDeviceChannelPolicy").disabled = !(device.assigned_playlist_id || state.groups.find((group) => group.id === device.box_group_id)?.playlist_id);
  $("#roomNumber").value = device.room_number || "";
  $("#arrivalDate").value = device.arrival_date || "";
  $("#departureDate").value = device.departure_date || "";
  $("#playlistUrl").value = device.playlist_url || "";
  $("#epgUrl").value = device.epg_url || "";
  $("#weatherLocation").value = device.weather_location || "";
  $("#customConnectionLabel").value = device.custom_connection_label || "";
  $("#customIspName").value = device.custom_isp_name || "";
  $("#startDestination").value = device.start_destination || "Home";
  $("#themeMode").value = device.theme_mode || "adaptive";
  $("#osdTimeoutSeconds").value = String(device.osd_timeout_seconds || 8);
  $("#captionsEnabled").checked = Boolean(device.captions_enabled);
  $("#captionsLanguage").value = device.captions_language || "en";
  $("#autoStart").checked = Boolean(device.auto_start);
  $("#resumeLastChannel").checked = device.resume_last_channel !== false;
  $("#keepAwakeHome").checked = Boolean(device.keep_awake_home);
  $("#autoUpdate").checked = device.auto_update !== false;
  $("#wifiOnly").checked = Boolean(device.wifi_only);
  const enabledApps = new Set(
    (device.visible_apps || []).map((app) => typeof app === "string" ? app : app.packageName)
  );
  $$("#visibleApps input").forEach((input) => {
    input.checked = enabledApps.has(input.value);
  });
  $("#deployApp").innerHTML = state.apps.length
    ? state.apps.map((app) => `<option value="${app.id}">${escapeHtml(app.name)} · ${escapeHtml(app.package_name)}</option>`).join("")
    : `<option value="">Add an app to the library first</option>`;
  $("#deployButton").disabled = !state.apps.length;
  $("#deviceError").textContent = "";

  const refreshBtn = $("#forceRefreshDevice");
  if (refreshBtn) {
    refreshBtn.disabled = !isDevOnline;
    refreshBtn.title = isDevOnline ? "Trigger an instant EPG & M3U reload on the TV" : "Device is offline (Power on TV to sync)";
    refreshBtn.style.opacity = isDevOnline ? "1" : "0.5";
  }

  // Reset to first tab
  $$(".editor-tab").forEach((t) => t.classList.remove("active"));
  $$(".tab-pane").forEach((pane) => pane.classList.remove("active"));
  const firstTab = $(".editor-tab[data-tab='tab-general']");
  if (firstTab) firstTab.classList.add("active");
  const firstPane = $("#tab-general");
  if (firstPane) firstPane.classList.add("active");

  showView("device-editor");
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function showToast(message, type = "info", duration = 3000) {
  const toastEl = $("#toast");
  const msgEl = $("#toastMessage");
  const iconEl = $("#toastIcon");
  if (!toastEl) return;

  if (msgEl) msgEl.textContent = message;
  else toastEl.textContent = message;

  if (iconEl) {
    iconEl.textContent = type === "success" ? "✓" : (type === "error" ? "✕" : (type === "warning" ? "⚠" : "ℹ"));
  }

  toastEl.className = `toast toast-${type} show`;
  clearTimeout(toastEl._timer);
  toastEl._timer = setTimeout(() => {
    toastEl.classList.remove("show");
  }, duration);
}
const toast = (msg) => showToast(msg, "info");

let currentHls = null;
function stopStreamPreview() {
  const video = $("#previewVideo");
  const audio = $("#previewAudio");
  if (currentHls) {
    currentHls.destroy();
    currentHls = null;
  }
  if (video) {
    video.pause();
    video.removeAttribute("src");
    video.load();
  }
  if (audio) {
    audio.pause();
    audio.removeAttribute("src");
    audio.load();
  }
  const dialog = $("#streamPreviewDialog");
  if (dialog?.open) dialog.close();
}

function previewStream(url, title = "Live Stream Preview", isAudio = false) {
  if (!url) {
    showToast("No stream URL provided.", "error");
    return;
  }
  stopStreamPreview();
  const dialog = $("#streamPreviewDialog");
  if (!dialog) return;

  $("#streamPreviewTitle").textContent = title;
  $("#streamPreviewSubtitle").textContent = isAudio ? "GLZ Audio Stream" : "Live Video Stream";
  $("#streamPreviewUrl").value = url;
  $("#statFormat").textContent = url.includes(".m3u8") ? "HLS (.m3u8)" : (isAudio ? "Audio Feed" : "Direct Stream");
  $("#statResolution").textContent = isAudio ? "Audio Only" : "Detecting…";
  $("#statStatus").textContent = "Connecting…";
  $("#statStatus").className = "text-emerald";
  $("#statBuffer").textContent = "0.0s";

  const video = $("#previewVideo");
  const audio = $("#previewAudio");

  video.classList.toggle("hidden", isAudio);
  audio.classList.toggle("hidden", !isAudio);

  const targetMedia = isAudio ? audio : video;

  if (url.includes(".m3u8") && window.Hls && window.Hls.isSupported()) {
    currentHls = new window.Hls({ enableWorker: true, lowLatencyMode: true });
    currentHls.loadSource(url);
    currentHls.attachMedia(targetMedia);
    currentHls.on(window.Hls.Events.MANIFEST_PARSED, (_, data) => {
      $("#statStatus").textContent = "Streaming";
      if (!isAudio && data.levels && data.levels[0]) {
        const level = data.levels[0];
        $("#statResolution").textContent = `${level.width || 1920}×${level.height || 1080}`;
      }
      targetMedia.play().catch(() => {});
    });
    currentHls.on(window.Hls.Events.ERROR, (_, data) => {
      if (data.fatal) {
        $("#statStatus").textContent = "Stream Error";
        $("#statStatus").className = "text-amber";
      }
    });
  } else {
    targetMedia.src = url;
    targetMedia.addEventListener("loadedmetadata", () => {
      $("#statStatus").textContent = "Streaming";
      if (!isAudio && video.videoWidth) {
        $("#statResolution").textContent = `${video.videoWidth}×${video.videoHeight}`;
      }
    }, { once: true });
    targetMedia.play().catch(() => {});
  }

  // Update buffer monitor
  const bufferInterval = setInterval(() => {
    if (!dialog.open) {
      clearInterval(bufferInterval);
      return;
    }
    if (targetMedia.buffered.length > 0) {
      const bufferedEnd = targetMedia.buffered.end(targetMedia.buffered.length - 1);
      const remaining = Math.max(0, bufferedEnd - targetMedia.currentTime);
      $("#statBuffer").textContent = `${remaining.toFixed(1)}s`;
    }
  }, 1000);

  dialog.showModal();
}

$("#copyStreamUrlBtn")?.addEventListener("click", () => {
  const url = $("#streamPreviewUrl").value;
  if (url) {
    navigator.clipboard?.writeText(url).then(() => showToast("Stream URL copied to clipboard", "success"));
  }
});

$("#openExternalStreamBtn")?.addEventListener("click", () => {
  const url = $("#streamPreviewUrl").value;
  if (url) window.open(url, "_blank");
});

$("#closeStreamPreview")?.addEventListener("click", stopStreamPreview);
$("#stopAndCloseStreamBtn")?.addEventListener("click", stopStreamPreview);

function toggleMobileNav(open) {
  const rail = $("#mainRail");
  const backdrop = $("#railBackdrop");
  if (!rail || !backdrop) return;
  const shouldOpen = open ?? !rail.classList.contains("open");
  rail.classList.toggle("open", shouldOpen);
  backdrop.classList.toggle("hidden", !shouldOpen);
}

$("#mobileNavToggle")?.addEventListener("click", () => toggleMobileNav(true));
$("#closeRailButton")?.addEventListener("click", () => toggleMobileNav(false));
$("#railBackdrop")?.addEventListener("click", () => toggleMobileNav(false));
$("#mobileRefreshBtn")?.addEventListener("click", () => {
  refreshDeviceActivity();
  showToast("Refreshing status…", "info");
});

$("#deviceSearchInput")?.addEventListener("input", (event) => {
  deviceSearchQuery = event.target.value;
  renderDevices();
});

$$("#deviceFilterPills .filter-pill").forEach((pill) => {
  pill.addEventListener("click", () => {
    $$("#deviceFilterPills .filter-pill").forEach((p) => p.classList.remove("active"));
    pill.classList.add("active");
    activeDeviceFilter = pill.dataset.filter || "all";
    renderDevices();
  });
});

$("#refreshDashboardButton")?.addEventListener("click", () => {
  refreshDeviceActivity();
  showToast("Dashboard refreshed", "info");
});

if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => {
    navigator.serviceWorker.register("/sw.js").catch(() => {});
  });
}

$("#authForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  $("#authError").textContent = "";
  try {
    await signIn($("#email").value, $("#password").value);
    showApp();
    await loadDevices();
  } catch (error) {
    $("#authError").textContent = error.message;
  }
});

$("#pairForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  $("#pairError").textContent = "";
  try {
    await api("/api/v1/enrollment/claim", {
      method: "POST",
      body: JSON.stringify({
        pairingCode: $("#pairingCode").value,
        name: $("#pairDeviceName").value || "New TV",
        guestName: $("#pairGuestName").value || "Guest",
        siteId: $("#pairSite").value || null
      })
    });
    event.target.reset();
    await loadDevices();
    showView("devices");
    toast("Television paired");
  } catch (error) {
    $("#pairError").textContent = error.message;
  }
});

async function saveDeviceSettings(syncToTv = true) {
  $("#deviceError").textContent = "";
  try {
    const id = $("#deviceId").value;
    await api(`/api/v1/admin/devices/${id}`, {
      method: "PATCH",
      body: JSON.stringify({
        config_version: Number($("#configVersion").value),
        name: $("#deviceName").value,
        guest_name: $("#guestName").value,
        site_id: $("#deviceSite").value || null,
        assigned_playlist_id: $("#deviceAssignedPlaylist").value || null,
        box_group_id: $("#deviceBoxGroup").value || null,
        room_number: $("#roomNumber").value || null,
        arrival_date: $("#arrivalDate").value || null,
        departure_date: $("#departureDate").value || null,
        playlist_url: $("#playlistUrl").value || null,
        epg_url: $("#epgUrl").value || null,
        home_preview_channel_id: $("#homePreviewChannel").value || null,
        weather_location: $("#weatherLocation").value,
        custom_connection_label: $("#customConnectionLabel").value || null,
        custom_isp_name: $("#customIspName").value || null,
        start_destination: $("#startDestination").value,
        theme_mode: $("#themeMode").value,
        osd_timeout_seconds: Number($("#osdTimeoutSeconds").value || 8),
        captions_enabled: $("#captionsEnabled").checked,
        captions_language: $("#captionsLanguage").value || "en",
        auto_start: $("#autoStart").checked,
        resume_last_channel: $("#resumeLastChannel").checked,
        keep_awake_home: $("#keepAwakeHome").checked,
        auto_update: $("#autoUpdate").checked,
        wifi_only: $("#wifiOnly").checked,
        visible_apps: $$("#visibleApps input:checked").map((input) => input.value),
        queue_sync: syncToTv
      })
    });
    showToast(syncToTv ? "Configuration saved & queued for TV sync" : "Configuration saved (TV sync postponed)", "success");
    await loadDevices();
    showView("devices");
  } catch (error) {
    $("#deviceError").textContent = error.message;
    showToast(error.message, "error");
  }
}

$("#deviceForm").addEventListener("submit", (event) => {
  event.preventDefault();
  saveDeviceSettings(true);
});

$("#saveDeviceNoSyncBtn")?.addEventListener("click", (event) => {
  event.preventDefault();
  saveDeviceSettings(false);
});

$("#saveDeviceNoSyncTopBtn")?.addEventListener("click", (event) => {
  event.preventDefault();
  saveDeviceSettings(false);
});

$("#saveDeviceTopBtn")?.addEventListener("click", () => saveDeviceSettings(true));

$("#forceRefreshDevice").addEventListener("click", async () => {
  const id = $("#deviceId").value;
  if (!id) return;
  $("#deviceError").textContent = "";
  try {
    await api(`/api/v1/admin/devices/${id}/force-refresh`, { method: "POST" });
    showToast("⚡ Force refresh signal sent to TV", "success");
  } catch (error) {
    $("#deviceError").textContent = error.message;
    showToast(error.message, "error");
  }
});

$("#backToDevicesBtn")?.addEventListener("click", () => showView("devices"));
$("#cancelDeviceEditorBtn")?.addEventListener("click", () => showView("devices"));

$$(".editor-tab").forEach((tab) => {
  tab.addEventListener("click", () => {
    $$(".editor-tab").forEach((t) => t.classList.remove("active"));
    $$(".tab-pane").forEach((pane) => pane.classList.remove("active"));
    tab.classList.add("active");
    const target = $(`#${tab.dataset.tab}`);
    if (target) target.classList.add("active");
  });
});

function openSite(id = "") {
  const site = state.sites.find((item) => item.id === id);
  $("#siteId").value = site?.id || "";
  $("#siteDialogTitle").textContent = site ? `Edit Property · ${site.name}` : "Add New Property";
  $("#siteName").value = site?.name || "";
  $("#siteAddress").value = site?.address || "";
  $("#siteError").textContent = "";

  const deleteBtn = $("#deleteSiteBtn");
  if (deleteBtn) deleteBtn.classList.toggle("hidden", !site);

  const devicesContainer = $("#siteDevicesList");
  if (devicesContainer) {
    devicesContainer.innerHTML = state.devices.map((device) => {
      const isChecked = device.site_id === id;
      const isDevOnline = isOnline(device);
      return `
        <label class="site-device-option" data-search="${escapeHtml([device.name, device.room_number].filter(Boolean).join(' ').toLowerCase())}">
          <input type="checkbox" value="${device.id}" ${isChecked ? "checked" : ""}>
          <span><strong>${escapeHtml(device.name || "Untitled")}</strong> <small>${escapeHtml(device.room_number ? `Room ${device.room_number}` : "No room")} · <span style="color:${isDevOnline ? "var(--accent-cyan)" : "var(--text-muted)"}">${isDevOnline ? "ONLINE" : "OFFLINE"}</span></small></span>
        </label>
      `;
    }).join("");
  }

  $("#siteDialog").showModal();
}

$("#siteDeviceSearch")?.addEventListener("input", (e) => {
  const q = e.target.value.trim().toLowerCase();
  $$("#siteDevicesList .site-device-option").forEach((opt) => {
    opt.classList.toggle("hidden", q && !opt.dataset.search.includes(q));
  });
});

$("#selectAllSiteBoxesBtn")?.addEventListener("click", () => {
  $$("#siteDevicesList input[type='checkbox']").forEach((cb) => {
    if (!cb.closest(".site-device-option")?.classList.contains("hidden")) cb.checked = true;
  });
});

$("#clearAllSiteBoxesBtn")?.addEventListener("click", () => {
  $$("#siteDevicesList input[type='checkbox']").forEach((cb) => {
    if (!cb.closest(".site-device-option")?.classList.contains("hidden")) cb.checked = false;
  });
});

$("#deleteSiteBtn")?.addEventListener("click", async () => {
  const id = $("#siteId").value;
  if (!id || !confirm("Delete this property? Assigned screens will be set to Unassigned.")) return;
  try {
    await api(`/api/v1/admin/sites/${id}`, { method: "DELETE" });
    $("#siteDialog").close();
    await loadDevices();
    showToast("Property deleted.", "success");
  } catch (err) {
    $("#siteError").textContent = err.message;
  }
});

["propertyName", "welcomeMessage", "logoUrl", "heroImageUrl"].forEach((id) => {
  $(`#${id}`)?.addEventListener("input", updateBrandingPreview);
});

$("#addServiceTileBtn")?.addEventListener("click", () => {
  const current = getServiceTilesFromUI();
  current.push({ title: "New Service", subtitle: "Hours & details", actionUrl: "https://" });
  renderServiceTiles(current);
});

$$(".exp-tab").forEach((tab) => {
  tab.addEventListener("click", () => {
    $$(".exp-tab").forEach((t) => t.classList.remove("active"));
    $$(".exp-pane").forEach((pane) => pane.classList.remove("active"));
    tab.classList.add("active");
    const target = $(`#${tab.dataset.tab}`);
    if (target) target.classList.add("active");
  });
});

function openExperienceSimulator() {
  const propName = $("#propertyName").value || "GLZ Hotel & Resort";
  const welcomeMsg = $("#welcomeMessage").value || "Relax, explore, and enjoy your stay.";
  const logo = $("#logoUrl").value.trim();
  const hero = $("#heroImageUrl").value.trim();
  const wifi = $("#wifiName").value.trim();
  const wifiInfo = $("#wifiInstructions").value.trim();
  const frontDesk = $("#frontDesk").value.trim();
  const checkout = $("#checkoutTime").value.trim();
  const noticeTitle = $("#noticeTitle").value.trim();
  const noticeBody = $("#noticeBody").value.trim();
  const services = getServiceTilesFromUI();

  $("#simPropertyName").textContent = propName;
  $("#simWelcomeMsg").textContent = welcomeMsg;
  const simLogo = $("#simLogo");
  if (simLogo) {
    if (logo) {
      simLogo.src = logo;
      simLogo.classList.remove("hidden");
    } else {
      simLogo.classList.add("hidden");
    }
  }

  const heroBg = $("#simHeroBg");
  if (heroBg) {
    if (hero) {
      heroBg.style.backgroundImage = `linear-gradient(180deg, rgba(0,0,0,0.2) 0%, rgba(5,10,18,0.95) 100%), url('${hero}')`;
    } else {
      heroBg.style.backgroundImage = "";
    }
  }

  $("#simWifiName").textContent = wifi || "GLZ-Resort-Guest";
  $("#simWifiInfo").textContent = wifiInfo || "Connect with your room number";
  $("#simFrontDesk").textContent = frontDesk || "Dial 0";
  $("#simCheckout").textContent = checkout ? `Checkout: ${checkout}` : "Checkout: 11:00 AM";

  const noticeCard = $("#simNoticeCard");
  if (noticeCard) {
    if (noticeTitle || noticeBody) {
      noticeCard.style.display = "flex";
      $("#simNoticeTitle").textContent = noticeTitle || "Guest Notice";
      $("#simNoticeBody").textContent = noticeBody || "";
    } else {
      noticeCard.style.display = "none";
    }
  }

  const servicesGrid = $("#simServicesGrid");
  if (servicesGrid) {
    if (services.length) {
      servicesGrid.innerHTML = services.map((s) => `
        <div class="tv-sim-tile">
          <strong>${escapeHtml(s.title)}</strong>
          <span>${escapeHtml(s.subtitle || "Service")}</span>
        </div>
      `).join("");
    } else {
      servicesGrid.innerHTML = `<span style="font-size:10px;opacity:0.6;">No service tiles added yet</span>`;
    }
  }

  $("#experienceSimulatorDialog").showModal();
}

$("#previewExperienceBtn")?.addEventListener("click", openExperienceSimulator);
$("#closeSimDialogBtn")?.addEventListener("click", () => $("#experienceSimulatorDialog").close());
$("#closeSimDialogBtnBottom")?.addEventListener("click", () => $("#experienceSimulatorDialog").close());

async function saveGuestExperience(syncToTvs = true) {
  $("#experienceError").textContent = "";
  if (!state.selectedSiteId) {
    showToast("Please select a property first.", "warning");
    return;
  }
  try {
    const result = await api("/api/v1/admin/guest-experience", {
      method: "PATCH",
      body: JSON.stringify({
        site_id: state.selectedSiteId,
        property_name: $("#propertyName").value,
        welcome_message: $("#welcomeMessage").value,
        logo_url: $("#logoUrl").value || null,
        hero_image_url: $("#heroImageUrl").value || null,
        wifi_name: $("#wifiName").value || null,
        wifi_instructions: $("#wifiInstructions").value || null,
        checkout_time: $("#checkoutTime").value || null,
        front_desk: $("#frontDesk").value || null,
        notice_title: $("#noticeTitle").value || null,
        notice_body: $("#noticeBody").value || null,
        services: getServiceTilesFromUI(),
        queue_sync: syncToTvs
      })
    });
    state.experience = result.profile;
    if (syncToTvs) {
      const count = result.pushedDevices || 0;
      showToast(count > 0 ? `Guest experience published & queued on ${count} online TV${count === 1 ? "" : "s"}.` : `Guest experience saved (no online TVs currently assigned to property).`, "success");
    } else {
      showToast("Guest experience saved as draft (TV sync postponed)", "success");
    }
  } catch (error) {
    $("#experienceError").textContent = error.message;
    showToast(error.message, "error");
  }
}

$("#experienceForm").addEventListener("submit", (event) => {
  event.preventDefault();
  saveGuestExperience(true);
});

$("#saveExperienceTopBtn")?.addEventListener("click", () => saveGuestExperience(true));
$("#saveExperienceDraftTopBtn")?.addEventListener("click", () => saveGuestExperience(false));
$("#saveExperienceDraftBtn")?.addEventListener("click", () => saveGuestExperience(false));

$("#experienceSite").addEventListener("change", async (event) => {
  state.selectedSiteId = event.target.value || null;
  $("#experienceError").textContent = "";
  try {
    await loadExperience();
    renderExperience();
  } catch (error) {
    $("#experienceError").textContent = error.message;
    showToast(error.message, "error");
  }
});

$("#addSiteButton").addEventListener("click", () => openSite());
$$("[data-close-site]").forEach((button) =>
  button.addEventListener("click", () => $("#siteDialog").close())
);
$("#siteForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  $("#siteError").textContent = "";
  const id = $("#siteId").value;
  try {
    const result = await api(id ? `/api/v1/admin/sites/${id}` : "/api/v1/admin/sites", {
      method: id ? "PATCH" : "POST",
      body: JSON.stringify({
        name: $("#siteName").value,
        address: $("#siteAddress").value || null
      })
    });
    const targetSiteId = id || result.site?.id;
    if (!id) state.selectedSiteId = targetSiteId;

    // Update screen assignments
    const selectedBoxes = new Set($$("#siteDevicesList input:checked").map((input) => input.value));
    const devicesToUpdate = state.devices.filter((device) => {
      const isSelected = selectedBoxes.has(device.id);
      const isCurrentSite = device.site_id === targetSiteId;
      return (isSelected && !isCurrentSite) || (!isSelected && isCurrentSite && id);
    });

    if (devicesToUpdate.length) {
      await Promise.all(devicesToUpdate.map((device) => api(`/api/v1/admin/devices/${device.id}`, {
        method: "PATCH",
        body: JSON.stringify({
          site_id: selectedBoxes.has(device.id) ? targetSiteId : null,
          queue_sync: false
        })
      })));
    }

    $("#siteDialog").close();
    await loadDevices();
    showView("sites");
    showToast(id ? "Property & screen assignments updated." : "Property created.", "success");
  } catch (error) {
    $("#siteError").textContent = error.message;
  }
});

$("#unpairDevice").addEventListener("click", async () => {
  const id = $("#deviceId").value;
  const device = state.devices.find((item) => item.id === id);
  if (!device || !confirm(`Remove pairing for ${device.name}? The TV will need a new pairing code to reconnect.`)) return;
  $("#deviceError").textContent = "";
  try {
    await api(`/api/v1/admin/devices/${id}`, { method: "DELETE" });
    showToast("Device pairing removed", "success");
    await loadDevices();
    showView("devices");
  } catch (error) {
    $("#deviceError").textContent = error.message;
    showToast(error.message, "error");
  }
});

$("#addAppButton").addEventListener("click", () => openApp());
$$("[data-close-app]").forEach((button) => button.addEventListener("click", () => $("#appDialog").close()));
$("#appForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  $("#appError").textContent = "";
  try {
    const id = $("#appId").value;
    await api(id ? `/api/v1/admin/apps/${id}` : "/api/v1/admin/apps", {
      method: id ? "PATCH" : "POST", body: JSON.stringify({
        name: $("#appName").value, package_name: $("#appPackage").value,
        source_type: $("#appSource").value, source_url: $("#appUrl").value || null,
        version_name: $("#appVersion").value || null, sha256: $("#appSha").value || null
      })
    });
    $("#appDialog").close();
    await loadDevices();
    toast(id ? "App entry updated" : "App added to library");
  } catch (error) { $("#appError").textContent = error.message; }
});
$("#deleteApp").addEventListener("click", async () => {
  const id = $("#appId").value;
  const app = state.apps.find((item) => item.id === id);
  if (!app || !confirm(`Delete ${app.name} from the app library? Existing install requests are not affected.`)) return;
  $("#appError").textContent = "";
  try {
    await api(`/api/v1/admin/apps/${id}`, { method: "DELETE" });
    $("#appDialog").close();
    await loadDevices();
    toast("App removed from library");
  } catch (error) { $("#appError").textContent = error.message; }
});
$("#deployButton").addEventListener("click", async () => {
  const deviceId = $("#deviceId").value;
  const appId = $("#deployApp").value;
  if (!deviceId || !appId) return;
  $("#deviceError").textContent = "";
  try {
    await api(`/api/v1/admin/devices/${deviceId}/commands`, {
      method: "POST", body: JSON.stringify({ appId })
    });
    toast("Install request queued for the TV");
  } catch (error) { $("#deviceError").textContent = error.message; }
});

$("#signOut").addEventListener("click", () => {
  localStorage.removeItem("glzhub_session");
  state.session = null;
  showApp();
});
$("#refreshDashboardButton").addEventListener("click", async () => {
  try {
    await loadDevices();
    toast("Dashboard refreshed");
  } catch (error) {
    toast(error.message);
  }
});
$("#pairButton").addEventListener("click", () => showView("pair"));
$$("[data-open-pair]").forEach((button) => button.addEventListener("click", () => showView("pair")));
$$("[data-view-button]").forEach((button) => button.addEventListener("click", () => showView(button.dataset.viewButton)));
$$(".nav").forEach((button) => button.addEventListener("click", () => showView(button.dataset.view)));

$("#visibleApps").innerHTML = APP_CATALOG.map(([name, packageName]) => `
  <label class="app-option">
    <input type="checkbox" value="${packageName}">
    <span><strong>${name}</strong><small>${packageName}</small></span>
  </label>
`).join("");

await loadPublicConfig();
restoreSession();
showApp();
if (state.session) {
  loadDevices().catch(() => { });
  loadPlaylists().catch(() => { });
}

async function loadRadioStations() {
  try {
    const { stations } = await api("/api/v1/admin/radio-stations");
    state.stations = stations || [];
    renderRadioStations();
  } catch (error) {
    showToast(error.message);
  }
}

function renderRadioStations() {
  const container = $("#radioList");
  const empty = $("#radioEmpty");
  container.innerHTML = "";
  empty.classList.toggle("hidden", state.stations.length > 0);

  if (state.stations.length > 0) {
    const card = document.createElement("article");
    card.style.cssText = "border: 1px solid var(--line); border-radius: 16px; background: #0c1827; overflow: hidden;";

    card.innerHTML = `
      <div style="padding: 16px 20px; background: var(--panel-2); border-bottom: 1px solid var(--line); display: flex; justify-content: space-between; align-items: center;">
        <div style="display: flex; align-items: center; gap: 12px;">
          <div style="width: 38px; height: 38px; border-radius: 10px; background: var(--lime); color: var(--ink); display: grid; place-items: center; font-weight: 900; font-size: 18px;">📻</div>
          <div>
            <h4 style="margin: 0; font-size: 16px; color: var(--text);">Active Radio Broadcast Streams (${state.stations.length} stations)</h4>
            <small style="color: var(--muted);">Live audio streams broadcasted to GLZ Radio mobile and web engines</small>
          </div>
        </div>
      </div>
      <div style="max-height: 540px; overflow-y: auto;">
        <table style="width: 100%; border-collapse: collapse; text-align: left; font-size: 13px;">
          <thead>
            <tr style="border-bottom: 1px solid var(--line); color: var(--muted); font-size: 11px; text-transform: uppercase; letter-spacing: 0.08em; background: #091321;">
              <th style="padding: 10px 18px;">Station Name</th>
              <th style="padding: 10px 18px; width: 140px;">Station Code</th>
              <th style="padding: 10px 18px; width: 130px;">Genre</th>
              <th style="padding: 10px 18px;">Audio Stream URL</th>
              <th style="padding: 10px 18px; width: 140px; text-align: right;">Actions</th>
            </tr>
          </thead>
          <tbody>
            ${state.stations.map((station) => `
              <tr style="border-bottom: 1px solid rgba(38,58,82,0.5); transition: background 0.15s;" onmouseover="this.style.background='#132438'" onmouseout="this.style.background='transparent'">
                <td style="padding: 12px 18px; font-weight: 700; color: var(--text);">
                  <div style="display: flex; align-items: center; gap: 10px;">
                    ${station.logo_url ? `<img src="${escapeHtml(station.logo_url)}" style="width: 26px; height: 26px; object-fit: contain; border-radius: 4px;" onerror="this.style.display='none'">` : ''}
                    <span>${escapeHtml(station.name)}</span>
                  </div>
                </td>
                <td style="padding: 12px 18px; color: var(--orange); font-family: monospace; font-weight: 850;">${escapeHtml(station.station_code)}</td>
                <td style="padding: 12px 18px;"><span class="source-badge" style="background: rgba(198,250,77,0.12); padding: 3px 8px; border-radius: 6px; border: 1px solid var(--lime);">${escapeHtml(station.genre)}</span></td>
                <td style="padding: 12px 18px; color: var(--muted); font-family: monospace; font-size: 11px; max-width: 320px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                  <a href="${escapeHtml(station.stream_url)}" target="_blank" style="color: #60a5fa; text-decoration: none;">${escapeHtml(station.stream_url)}</a>
                </td>
                <td style="padding: 12px 18px; text-align: right;">
                  <div style="display: flex; gap: 8px; justify-content: flex-end; align-items: center;">
                    <button type="button" class="secondary preview-radio-stream" data-url="${escapeHtml(station.stream_url)}" data-name="${escapeHtml(station.name)}" style="padding: 4px 10px; font-size: 11px;">▶ Listen</button>
                    <button type="button" class="secondary edit-radio" data-id="${station.id}" style="padding: 4px 10px; font-size: 11px;">Edit</button>
                    <button type="button" class="danger-button delete-radio" data-id="${station.id}" style="padding: 4px 10px; font-size: 11px; margin: 0;">Delete</button>
                  </div>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    `;
    container.appendChild(card);
  }

  $$(".preview-radio-stream").forEach((btn) => btn.addEventListener("click", () => previewStream(btn.dataset.url, btn.dataset.name, true)));
  $$(".edit-radio").forEach((btn) => btn.addEventListener("click", () => openRadioDialog(btn.dataset.id)));
  $$(".delete-radio").forEach((btn) => btn.addEventListener("click", () => deleteRadioStation(btn.dataset.id)));
}

function openRadioDialog(id = null) {
  const dialog = $("#radioDialog");
  const station = state.stations?.find((s) => s.id === id);
  $("#radioId").value = station ? station.id : "";
  $("#radioName").value = station ? station.name : "";
  $("#radioCode").value = station ? station.station_code : "";
  $("#radioGenre").value = station ? station.genre : "";
  $("#radioBitrate").value = station ? station.bitrate : 128;
  $("#radioStreamUrl").value = station ? station.stream_url : "";
  $("#radioLogoUrl").value = station ? station.logo_url || "" : "";
  $("#radioDialogTitle").textContent = station ? "Edit radio station" : "Add radio station";
  $("#radioError").textContent = "";
  dialog.showModal();
}

$("#addRadioStationButton")?.addEventListener("click", () => openRadioDialog());
$("[data-close-radio]")?.addEventListener("click", () => $("#radioDialog").close());

$("#radioForm")?.addEventListener("submit", async (event) => {
  event.preventDefault();
  $("#radioError").textContent = "";
  const id = $("#radioId").value;
  const payload = {
    name: $("#radioName").value,
    stationCode: $("#radioCode").value,
    genre: $("#radioGenre").value,
    bitrate: Number($("#radioBitrate").value),
    streamUrl: $("#radioStreamUrl").value,
    logoUrl: $("#radioLogoUrl").value || null
  };
  try {
    if (id) {
      await api(`/api/v1/admin/radio-stations/${id}`, { method: "PATCH", body: JSON.stringify(payload) });
    } else {
      await api("/api/v1/admin/radio-stations", { method: "POST", body: JSON.stringify(payload) });
    }
    $("#radioDialog").close();
    toast("Radio station saved.");
    await loadRadioStations();
  } catch (error) {
    $("#radioError").textContent = error.message;
  }
});

async function deleteRadioStation(id) {
  if (!confirm("Are you sure you want to delete this radio station?")) return;
  try {
    await api(`/api/v1/admin/radio-stations/${id}`, { method: "DELETE" });
    toast("Radio station deleted.");
    await loadRadioStations();
  } catch (error) {
    showToast(error.message);
  }
}

async function loadPlaylists() {
  try {
    const { playlists } = await api("/api/v1/admin/playlists");
    state.playlists = playlists || [];
    renderPlaylists();
  } catch (error) {
    showToast(error.message);
  }
}

function renderPlaylists() {
  const container = $("#playlistList");
  const empty = $("#studioEmpty");
  const pageScroll = { x: window.scrollX, y: window.scrollY };
  const tableScroll = new Map($$(".studio-playlist-card[data-playlist-id]").map((card) => {
    const wrap = card.querySelector(".studio-table-wrap");
    return [card.dataset.playlistId, { top: wrap?.scrollTop || 0, left: wrap?.scrollLeft || 0 }];
  }));
  container.innerHTML = "";
  empty.classList.toggle("hidden", state.playlists.length > 0);

  state.playlists.forEach((pl) => {
    const card = document.createElement("article");
    card.className = "studio-playlist-card";
    card.dataset.playlistId = pl.id;
    const items = [...(pl.playlist_items || [])].sort((a, b) => Number(a.position || 0) - Number(b.position || 0));
    card.innerHTML = `
      <div class="studio-card-head">
        <div style="display: flex; align-items: center; gap: 14px;">
          <div style="width: 42px; height: 42px; border-radius: 10px; background: var(--orange); color: var(--ink); display: grid; place-items: center; font-weight: 900; font-size: 18px;">📺</div>
          <div>
            <div style="display: flex; align-items: center; gap: 10px;">
              <h4 style="margin: 0; font-size: 18px; color: var(--text);">${escapeHtml(pl.title)}</h4>
              <span class="studio-badge ${pl.is_published ? "published" : ""}">${pl.is_published ? "PUBLISHED" : "DRAFT"}</span>
              <span class="studio-badge">${escapeHtml(pl.category).toUpperCase()}</span>
            </div>
            <small style="color: var(--muted); margin-top: 4px; display: block;">${escapeHtml(pl.description || "No description provided")} • <strong>${items.length} channels</strong>${pl.epg_url ? " • EPG connected" : ""}</small>
          </div>
        </div>
        <div class="studio-card-actions">
          <button type="button" class="secondary import-playlist" data-id="${pl.id}">⇧ Import M3U</button>
          <button type="button" class="secondary export-playlist" data-id="${pl.id}">⇩ Export M3U</button>
          <button type="button" class="secondary edit-guide" data-id="${pl.id}">▤ EPG editor</button>
          <button type="button" class="secondary push-playlist" data-id="${pl.id}">↻ Push to TVs</button>
          <button type="button" class="secondary toggle-playlist" data-id="${pl.id}">${pl.is_published ? "Unpublish" : "Publish"}</button>
          <button type="button" class="secondary edit-playlist" data-id="${pl.id}">Settings</button>
          <button type="button" class="primary add-channel-item" data-id="${pl.id}">＋ Channel</button>
          <button type="button" class="danger-button delete-playlist" data-id="${pl.id}" style="margin:0;">Delete</button>
        </div>
      </div>
      <div class="studio-filter"><input class="playlist-filter" placeholder="Search channels, groups, EPG IDs or URLs…"></div>
      <div>
        ${items.length === 0 ? `
          <div style="padding: 30px; text-align: center; color: var(--muted); font-size: 14px;">
            No channels yet. Import an M3U file or add the first channel manually.
          </div>
        ` : `
          <div class="studio-table-wrap">
            <table class="studio-table">
              <thead>
                <tr><th>Order</th><th>CH #</th><th>Channel name</th><th>Group</th><th>EPG ID</th><th>Stream URL</th><th>Status</th><th>Actions</th></tr>
              </thead>
              <tbody>
                ${items.map((item) => {
                  const meta = item.metadata || {};
                  const searchable = [item.title, item.media_url, meta.group, meta.tvg_id, meta.tvg_chno].filter(Boolean).join(" ").toLowerCase();
                  return `<tr class="studio-channel-row" draggable="true" data-playlist-id="${pl.id}" data-item-id="${item.id}" data-search="${escapeHtml(searchable)}">
                    <td style="color:var(--orange);font-weight:900;">↕ ${item.position || 0}</td>
                    <td><input class="studio-inline-field inline-channel-number" data-playlist-id="${pl.id}" data-item-id="${item.id}" value="${escapeHtml(meta.tvg_chno || "")}" aria-label="Channel number" placeholder="—" maxlength="20"></td>
                    <td><div class="studio-channel-title">${meta.tvg_logo ? `<img src="${escapeHtml(meta.tvg_logo)}" alt="">` : "<span>▣</span>"}<input class="studio-inline-field inline-channel-name" data-playlist-id="${pl.id}" data-item-id="${item.id}" value="${escapeHtml(item.title)}" aria-label="Channel name" maxlength="120"></div></td>
                    <td><input class="studio-inline-field inline-channel-group" data-playlist-id="${pl.id}" data-item-id="${item.id}" value="${escapeHtml(meta.group || "")}" aria-label="Channel group" placeholder="—" maxlength="100"></td>
                    <td><input class="studio-inline-field inline-channel-epg" data-playlist-id="${pl.id}" data-item-id="${item.id}" value="${escapeHtml(meta.tvg_id || "")}" aria-label="EPG ID" placeholder="—" maxlength="120"></td>
                    <td><input class="studio-inline-field inline-channel-url" data-playlist-id="${pl.id}" data-item-id="${item.id}" value="${escapeHtml(item.media_url)}" aria-label="Stream URL" maxlength="2048"></td>
                    <td><button type="button" class="studio-badge channel-visibility ${meta.hidden ? "hidden-channel" : "published"}" data-playlist-id="${pl.id}" data-item-id="${item.id}" data-hidden="${meta.hidden === true}">${meta.hidden ? "HIDDEN" : "VISIBLE"}</button></td>
                    <td>
                      <div style="display:flex;gap:6px;align-items:center;">
                        <button type="button" class="secondary preview-channel-stream" data-url="${escapeHtml(item.media_url)}" data-title="${escapeHtml(item.title)}" style="padding:5px 8px;" title="Test Stream">▶ Test</button>
                        <button type="button" class="secondary edit-playlist-item" data-item-id="${item.id}" data-playlist-id="${pl.id}" style="padding:5px 8px;">Edit</button>
                      </div>
                    </td>
                  </tr>`;
                }).join("")}
              </tbody>
            </table>
          </div>
        `}
      </div>
    `;
    container.appendChild(card);
  });

  tableScroll.forEach((position, playlistId) => {
    const wrap = container.querySelector(`.studio-playlist-card[data-playlist-id="${playlistId}"] .studio-table-wrap`);
    if (wrap) { wrap.scrollTop = position.top; wrap.scrollLeft = position.left; }
  });
  requestAnimationFrame(() => window.scrollTo(pageScroll.x, pageScroll.y));

  $$(".preview-channel-stream").forEach((btn) => btn.addEventListener("click", (e) => {
    e.stopPropagation();
    previewStream(btn.dataset.url, btn.dataset.title, false);
  }));
  $$(".edit-playlist").forEach((btn) => btn.addEventListener("click", () => openPlaylistDialog(btn.dataset.id)));
  $$(".add-channel-item").forEach((btn) => btn.addEventListener("click", () => {
    $("#playlistId").value = btn.dataset.id;
    openPlaylistItemDialog();
  }));
  $$(".edit-playlist-item").forEach((btn) => btn.addEventListener("click", () => openPlaylistItemDialog(btn.dataset.playlistId, btn.dataset.itemId)));
  $$(".studio-inline-field").forEach((input) => {
    input.dataset.original = input.value;
    input.addEventListener("pointerdown", (event) => event.stopPropagation());
    input.addEventListener("keydown", (event) => {
      if (event.key === "Enter") { event.preventDefault(); input.blur(); }
      if (event.key === "Escape") { input.value = input.dataset.original; input.blur(); }
    });
    input.addEventListener("blur", () => saveInlineChannelField(input));
  });
  $$(".channel-visibility").forEach((btn) => btn.addEventListener("click", async () => {
    const playlist = state.playlists.find((entry) => entry.id === btn.dataset.playlistId);
    const item = playlist?.playlist_items?.find((entry) => entry.id === btn.dataset.itemId);
    if (!item) return;
    btn.disabled = true;
    try {
      await api(`/api/v1/admin/playlists/${playlist.id}/items/${item.id}`, {
        method: "PATCH",
        body: JSON.stringify({ metadata: { ...(item.metadata || {}), hidden: btn.dataset.hidden !== "true" } })
      });
      toast(btn.dataset.hidden === "true" ? "Channel is now visible." : "Channel hidden from GLZ TV.");
      await loadPlaylists();
    } catch (error) {
      btn.disabled = false;
      toast(error.message);
    }
  }));
  $$(".import-playlist").forEach((btn) => btn.addEventListener("click", () => {
    state.importPlaylistId = btn.dataset.id;
    $("#m3uFileInput").click();
  }));
  $$(".export-playlist").forEach((btn) => btn.addEventListener("click", () => exportPlaylist(btn.dataset.id)));
  $$(".edit-guide").forEach((btn) => btn.addEventListener("click", () => {
    epgState.playlistId = btn.dataset.id;
    showView("epg");
  }));
  $$(".push-playlist").forEach((btn) => btn.addEventListener("click", async () => {
    btn.disabled = true;
    try {
      const result = await api(`/api/v1/admin/playlists/${btn.dataset.id}/push`, { method: "POST" });
      const count = result.pushedDevices || 0;
      showToast(count > 0 ? `Lineup queued for sync on ${count} online TV${count === 1 ? "" : "s"}.` : `No online TVs assigned to this lineup to sync.`, count > 0 ? "success" : "warning");
    } catch (error) { showToast(error.message, "error"); }
    finally { btn.disabled = false; }
  }));
  $$(".toggle-playlist").forEach((btn) => btn.addEventListener("click", async () => {
    const playlist = state.playlists.find((item) => item.id === btn.dataset.id);
    if (!playlist) return;
    await api(`/api/v1/admin/playlists/${playlist.id}`, { method: "PATCH", body: JSON.stringify({ is_published: !playlist.is_published }) });
    toast(playlist.is_published ? "Lineup unpublished." : "Lineup published to GLZ TV.");
    await loadPlaylists();
  }));
  $$(".playlist-filter").forEach((input) => input.addEventListener("input", () => {
    const query = input.value.trim().toLowerCase();
    input.closest(".studio-playlist-card").querySelectorAll(".studio-channel-row").forEach((row) => {
      row.classList.toggle("hidden", !row.dataset.search.includes(query));
    });
  }));
  $$(".delete-playlist").forEach((btn) => btn.addEventListener("click", () => deletePlaylist(btn.dataset.id)));
  wirePlaylistDragging();
}

async function saveInlineChannelField(input) {
  if (input.value === input.dataset.original) return;
  const playlist = state.playlists.find((entry) => entry.id === input.dataset.playlistId);
  const item = playlist?.playlist_items?.find((entry) => entry.id === input.dataset.itemId);
  if (!item) return;
  const value = input.value.trim();
  if (input.classList.contains("inline-channel-name") && !value) {
    input.value = input.dataset.original;
    toast("Channel name cannot be empty.");
    return;
  }
  input.disabled = true;
  try {
    let payload;
    if (input.classList.contains("inline-channel-name")) payload = { title: value };
    else if (input.classList.contains("inline-channel-url")) payload = { mediaUrl: value };
    else {
      const key = input.classList.contains("inline-channel-number") ? "tvg_chno"
        : input.classList.contains("inline-channel-epg") ? "tvg_id" : "group";
      payload = { metadata: { ...(item.metadata || {}), [key]: value || null } };
    }
    const result = await api(`/api/v1/admin/playlists/${playlist.id}/items/${item.id}`, { method: "PATCH", body: JSON.stringify(payload) });
    if (result.item) Object.assign(item, result.item);
    input.dataset.original = value;
    input.disabled = false;
    const row = input.closest(".studio-channel-row");
    if (row) {
      const meta = item.metadata || {};
      row.dataset.search = [item.title, item.media_url, meta.group, meta.tvg_id, meta.tvg_chno].filter(Boolean).join(" ").toLowerCase();
    }
    toast("Channel field saved.");
  } catch (error) {
    input.value = input.dataset.original;
    input.disabled = false;
    toast(error.message);
  }
}

async function openGuideDialog(playlistId) {
  const playlist = state.playlists.find((entry) => entry.id === playlistId);
  if (!playlist) return;
  $("#guideForm").reset();
  $("#guidePlaylistId").value = playlistId;
  $("#guideDialogTitle").textContent = `${playlist.title} · EPG editor`;
  $("#guideSourceUrl").value = playlist.epg_url?.includes("/api/v1/guides/") ? "" : (playlist.epg_url || "");
  $("#guideStatus").textContent = "Checking for a managed guide…";
  $("#guideError").textContent = "";
  $("#guideDialog").showModal();
  try {
    const result = await api(`/api/v1/admin/playlists/${playlistId}/guide`);
    const guide = result.guide;
    $("#guideName").value = guide?.name || `${playlist.title} Guide`;
    $("#guideSourceUrl").value = guide?.source_url || $("#guideSourceUrl").value;
    $("#guideStatus").textContent = guide
      ? `${guide.channel_count} channels · ${guide.programme_count} programs · updated ${new Date(guide.updated_at).toLocaleString()}`
      : "No managed guide yet. Upload XML/XML.GZ or fetch one from an HTTPS source such as epg.best.";
    $("#downloadGuideXml").disabled = !guide;
    $("#downloadGuideGz").disabled = !guide;
  } catch (error) { $("#guideStatus").textContent = error.message; }
}

async function readGuideFile(file) {
  if (!file) return null;
  if (file.name.toLowerCase().endsWith(".gz")) {
    if (!("DecompressionStream" in window)) throw new Error("This browser cannot open gzip files. Upload XML or use a source URL.");
    return new Response(file.stream().pipeThrough(new DecompressionStream("gzip"))).text();
  }
  return file.text();
}

async function downloadGuide(playlistId, gzip) {
  const response = await fetch(`/api/v1/admin/playlists/${playlistId}/guide.xml${gzip ? ".gz" : ""}`, {
    headers: { authorization: `Bearer ${state.session.access_token}` }
  });
  if (!response.ok) throw new Error("Could not export guide.");
  let blob = await response.blob();
  let suffix = ".xml";
  if (gzip) {
    if (!("CompressionStream" in window)) throw new Error("This browser cannot create a gzip download.");
    blob = await new Response(blob.stream().pipeThrough(new CompressionStream("gzip"))).blob();
    suffix = ".xml.gz";
  }
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url; link.download = `glz-guide${suffix}`; link.click(); URL.revokeObjectURL(url);
}

$$('[data-close-guide]').forEach((button) => button.addEventListener("click", () => $("#guideDialog").close()));
$("#guideForm")?.addEventListener("submit", async (event) => {
  event.preventDefault();
  const playlistId = $("#guidePlaylistId").value;
  const sourceUrl = $("#guideSourceUrl").value.trim();
  const file = $("#guideFile").files?.[0];
  $("#guideError").textContent = "";
  if (!sourceUrl && !file) { $("#guideError").textContent = "Choose an XML/XML.GZ file or enter an HTTPS guide URL."; return; }
  const submit = event.submitter;
  if (submit) submit.disabled = true;
  try {
    const xml = sourceUrl ? null : await readGuideFile(file);
    const result = await api(`/api/v1/admin/playlists/${playlistId}/guide`, {
      method: "PUT",
      body: JSON.stringify({ name: $("#guideName").value || null, sourceUrl: sourceUrl || null, xml })
    });
    $("#guideStatus").textContent = `${result.guide.channel_count} channels · ${result.guide.programme_count} programs · published now`;
    $("#downloadGuideXml").disabled = false;
    $("#downloadGuideGz").disabled = false;
    toast("EPG imported and published to the lineup.");
    await loadPlaylists();
  } catch (error) { $("#guideError").textContent = error.message; }
  finally { if (submit) submit.disabled = false; }
});
$("#downloadGuideXml")?.addEventListener("click", () => downloadGuide($("#guidePlaylistId").value, false).catch((error) => toast(error.message)));
$("#downloadGuideGz")?.addEventListener("click", () => downloadGuide($("#guidePlaylistId").value, true).catch((error) => toast(error.message)));

function parseM3u(text) {
  const lines = text.replace(/^\uFEFF/, "").split(/\r?\n/);
  const items = [];
  let pending = null;
  for (const rawLine of lines) {
    const line = rawLine.trim();
    if (line.startsWith("#EXTINF:")) {
      const comma = line.indexOf(",");
      const attributes = comma >= 0 ? line.slice(0, comma) : line;
      const metadata = {};
      for (const match of attributes.matchAll(/([\w-]+)="([^"]*)"/g)) {
        const key = match[1].toLowerCase();
        if (key === "tvg-id") metadata.tvg_id = match[2];
        if (key === "tvg-chno") metadata.tvg_chno = match[2];
        if (key === "tvg-logo") metadata.tvg_logo = match[2];
        if (key === "group-title") metadata.group = match[2];
        if (key === "playlist-studio-hidden") metadata.hidden = match[2] === "true";
      }
      const duration = Number(line.slice(8).split(/[ ,]/, 1)[0]);
      pending = {
        title: (comma >= 0 ? line.slice(comma + 1) : "Untitled channel").trim() || "Untitled channel",
        duration_seconds: Number.isFinite(duration) ? duration : -1,
        metadata
      };
    } else if (pending && line && !line.startsWith("#")) {
      if (/^https:\/\//i.test(line)) items.push({ ...pending, mediaUrl: line });
      pending = null;
    }
  }
  if (!items.length) throw new Error("This file contains no valid HTTPS M3U channels.");
  return items;
}

function playlistM3u(playlist) {
  const epgUrl = playlist.epg_url || "https://play.glztech.com/epg.xml.gz";
  const quote = (value) => String(value || "").replace(/[\r\n]+/g, " ").replace(/"/g, "'");
  const lines = [`#EXTM3U x-tvg-url="${quote(epgUrl)}"`, ""];
  [...(playlist.playlist_items || [])]
    .sort((a, b) => Number(a.position || 0) - Number(b.position || 0))
    .forEach((item) => {
      const meta = item.metadata || {};
      const attributes = [
        meta.tvg_id && `tvg-id="${quote(meta.tvg_id)}"`,
        meta.tvg_chno && `tvg-chno="${quote(meta.tvg_chno)}"`,
        meta.tvg_logo && `tvg-logo="${quote(meta.tvg_logo)}"`,
        meta.group && `group-title="${quote(meta.group)}"`,
        meta.hidden && 'playlist-studio-hidden="true"'
      ].filter(Boolean).join(" ");
      lines.push(`#EXTINF:${Number(item.duration_seconds ?? -1)}${attributes ? ` ${attributes}` : ""},${quote(item.title)}`);
      lines.push(item.media_url, "");
    });
  return lines.join("\n");
}

function exportPlaylist(id) {
  const playlist = state.playlists.find((item) => item.id === id);
  if (!playlist) return;
  const blob = new Blob([playlistM3u(playlist)], { type: "audio/x-mpegurl;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `${playlist.title.replace(/[^a-z0-9]+/gi, "-").replace(/^-|-$/g, "").toLowerCase() || "playlist"}.m3u`;
  link.click();
  URL.revokeObjectURL(url);
  toast("M3U exported.");
}

$("#m3uFileInput")?.addEventListener("change", async (event) => {
  const file = event.target.files?.[0];
  const playlistId = state.importPlaylistId;
  event.target.value = "";
  if (!file || !playlistId) return;
  try {
    const items = parseM3u(await file.text());
    const playlist = state.playlists.find((item) => item.id === playlistId);
    if (!confirm(`Replace the ${playlist?.playlist_items?.length || 0} existing channels in “${playlist?.title || "this playlist"}” with ${items.length} imported channels?`)) return;
    const result = await api(`/api/v1/admin/playlists/${playlistId}/import`, {
      method: "POST",
      body: JSON.stringify({ items, replace: true })
    });
    toast(`${result.imported} channels imported.`);
    await loadPlaylists();
  } catch (error) {
    toast(error.message);
  }
});

let draggedChannel = null;
function wirePlaylistDragging() {
  $$(".studio-channel-row").forEach((row) => {
    row.addEventListener("dragstart", () => {
      draggedChannel = { playlistId: row.dataset.playlistId, itemId: row.dataset.itemId };
      row.classList.add("dragging");
    });
    row.addEventListener("dragend", () => {
      draggedChannel = null;
      $$(".studio-channel-row").forEach((item) => item.classList.remove("dragging", "drop-target"));
    });
    row.addEventListener("dragover", (event) => {
      if (draggedChannel?.playlistId !== row.dataset.playlistId) return;
      event.preventDefault();
      row.classList.add("drop-target");
    });
    row.addEventListener("dragleave", () => row.classList.remove("drop-target"));
    row.addEventListener("drop", async (event) => {
      event.preventDefault();
      if (!draggedChannel || draggedChannel.playlistId !== row.dataset.playlistId || draggedChannel.itemId === row.dataset.itemId) return;
      const playlist = state.playlists.find((item) => item.id === row.dataset.playlistId);
      const ordered = [...(playlist?.playlist_items || [])].sort((a, b) => Number(a.position || 0) - Number(b.position || 0));
      const from = ordered.findIndex((item) => item.id === draggedChannel.itemId);
      const to = ordered.findIndex((item) => item.id === row.dataset.itemId);
      const [moved] = ordered.splice(from, 1);
      ordered.splice(to, 0, moved);
      try {
        await api(`/api/v1/admin/playlists/${playlist.id}/reorder`, { method: "PATCH", body: JSON.stringify({ itemIds: ordered.map((item) => item.id) }) });
        toast("Channel order saved.");
        await loadPlaylists();
      } catch (error) {
        toast(error.message);
      }
    });
  });
}

function renderPlaylistItems(playlist) {
  const container = $("#playlistItemsList");
  const countEl = $("#playlistItemCount");
  if (!playlist || !playlist.playlist_items) {
    if (countEl) countEl.textContent = "0";
    if (container) container.innerHTML = `<div style="color: var(--text-dim, #888); font-size: 0.85rem; text-align: center; padding: 0.8rem 0;">No channels added to this playlist yet.</div>`;
    return;
  }
  const query = $("#playlistItemFilter")?.value.trim().toLowerCase() || "";
  const allItems = [...playlist.playlist_items].sort((a, b) => Number(a.position || 0) - Number(b.position || 0));
  if (countEl) countEl.textContent = String(allItems.length);

  const items = query ? allItems.filter((it) => {
    const meta = it.metadata || {};
    return [it.title, it.media_url, meta.group, meta.tvg_id, meta.tvg_chno].filter(Boolean).join(" ").toLowerCase().includes(query);
  }) : allItems;

  if (!items.length) {
    container.innerHTML = `<div style="color: var(--text-dim, #888); font-size: 0.85rem; text-align: center; padding: 0.8rem 0;">${query ? "No channels match filter." : "No channels added to this playlist yet."}</div>`;
    return;
  }

  container.innerHTML = items.map((item) => {
    const meta = item.metadata || {};
    return `
    <div class="channel-item-row">
      ${meta.tvg_logo ? `<img class="channel-logo-thumb" src="${escapeHtml(meta.tvg_logo)}" alt="">` : `<span class="channel-logo-thumb" style="display:grid;place-items:center;font-size:11px;">📺</span>`}
      <div class="channel-info-meta">
        <strong>#${item.position || 0} ${escapeHtml(item.title)}</strong>
        <small><span>${escapeHtml(meta.group || "Ungrouped")}</span>${meta.tvg_id ? `<span>EPG: ${escapeHtml(meta.tvg_id)}</span>` : ""}</small>
      </div>
      <button type="button" class="secondary preview-item-stream small-btn" data-url="${escapeHtml(item.media_url)}" data-title="${escapeHtml(item.title)}">▶ Test</button>
      <button type="button" class="danger-button delete-playlist-item small-btn" data-item-id="${item.id}" data-playlist-id="${playlist.id}">Delete</button>
    </div>
  `;
  }).join("");

  $$(".preview-item-stream").forEach((btn) => btn.addEventListener("click", () => previewStream(btn.dataset.url, btn.dataset.title, false)));
  $$(".delete-playlist-item").forEach((btn) => btn.addEventListener("click", () => deletePlaylistItem(btn.dataset.playlistId, btn.dataset.itemId)));
}

$("#playlistItemFilter")?.addEventListener("input", () => {
  const currentPl = state.playlists.find((p) => p.id === $("#playlistId").value);
  if (currentPl) renderPlaylistItems(currentPl);
});

function openPlaylistDialog(id = null) {
  const dialog = $("#playlistDialog");
  const pl = state.playlists?.find((p) => p.id === id);
  $("#playlistId").value = pl ? pl.id : "";
  $("#playlistTitle").value = pl ? pl.title : "";
  $("#playlistCategory").value = pl ? pl.category : "";
  $("#playlistTargetApp").value = "tv";
  $("#playlistDescription").value = pl ? pl.description || "" : "";
  $("#playlistArtworkUrl").value = pl ? pl.artwork_url || "" : "";
  $("#playlistEpgUrl").value = pl ? pl.epg_url || "" : "";
  $("#playlistPublished").checked = pl ? pl.is_published !== false : true;
  $("#playlistDialogTitle").textContent = pl ? "Edit playlist & channels" : "Add playlist";
  $("#playlistError").textContent = "";

  const itemsSection = $("#playlistItemsSection");
  if (pl) {
    itemsSection.style.display = "block";
    renderPlaylistItems(pl);
  } else {
    itemsSection.style.display = "none";
  }
  dialog.showModal();
}

$("#addPlaylistButton")?.addEventListener("click", () => openPlaylistDialog());
$("[data-close-playlist]")?.addEventListener("click", () => $("#playlistDialog").close());

$("#playlistForm")?.addEventListener("submit", async (event) => {
  event.preventDefault();
  $("#playlistError").textContent = "";
  const id = $("#playlistId").value;
  const payload = {
    title: $("#playlistTitle").value,
    category: $("#playlistCategory").value,
    targetApp: $("#playlistTargetApp").value,
    description: $("#playlistDescription").value || null,
    artworkUrl: $("#playlistArtworkUrl").value || null,
    epgUrl: $("#playlistEpgUrl").value || null,
    is_published: $("#playlistPublished").checked
  };
  try {
    if (id) {
      await api(`/api/v1/admin/playlists/${id}`, { method: "PATCH", body: JSON.stringify(payload) });
    } else {
      await api("/api/v1/admin/playlists", { method: "POST", body: JSON.stringify(payload) });
    }
    $("#playlistDialog").close();
    toast("TV playlist saved.");
    await loadPlaylists();
  } catch (error) {
    $("#playlistError").textContent = error.message;
  }
});

async function deletePlaylist(id) {
  if (!confirm("Are you sure you want to delete this playlist?")) return;
  try {
    await api(`/api/v1/admin/playlists/${id}`, { method: "DELETE" });
    showToast("Playlist deleted.");
    loadPlaylists();
  } catch (error) {
    showToast(error.message);
  }
}

function openPlaylistItemDialog(playlistId = $("#playlistId").value, itemId = null) {
  if (!playlistId) return;
  const playlist = state.playlists.find((entry) => entry.id === playlistId);
  const item = playlist?.playlist_items?.find((entry) => entry.id === itemId);
  const metadata = item?.metadata || {};
  $("#itemPlaylistId").value = playlistId;
  $("#itemId").value = item?.id || "";
  $("#itemTitle").value = item?.title || "";
  $("#itemTvgId").value = metadata.tvg_id || "";
  $("#itemTvgChno").value = metadata.tvg_chno || "";
  $("#itemGroup").value = metadata.group || "";
  $("#itemMediaUrl").value = item?.media_url || "";
  $("#itemTvgLogo").value = metadata.tvg_logo || "";
  $("#itemHidden").checked = metadata.hidden === true;
  $("#itemPosition").value = String(item?.position || (playlist?.playlist_items?.length || 0) + 1);
  $("#playlistItemDialogTitle").textContent = item ? "Edit channel" : "Add channel to playlist";
  $("#deleteCurrentChannel").classList.toggle("hidden", !item);
  $("#itemError").textContent = "";
  $("#playlistItemDialog").showModal();
}

$("#addPlaylistItemButton")?.addEventListener("click", () => openPlaylistItemDialog());
$("[data-close-playlist-item]")?.addEventListener("click", () => $("#playlistItemDialog").close());
$("#previewCurrentItemBtn")?.addEventListener("click", () => {
  const url = $("#itemMediaUrl").value;
  const title = $("#itemTitle").value || "Channel Preview";
  previewStream(url, title, false);
});

$("#playlistItemForm")?.addEventListener("submit", async (event) => {
  event.preventDefault();
  $("#itemError").textContent = "";
  const playlistId = $("#itemPlaylistId").value;
  const itemId = $("#itemId").value;
  const payload = {
    title: $("#itemTitle").value,
    mediaUrl: $("#itemMediaUrl").value,
    position: Number($("#itemPosition").value || 0),
    metadata: {
      tvg_id: $("#itemTvgId").value || null,
      tvg_chno: $("#itemTvgChno").value || null,
      tvg_logo: $("#itemTvgLogo").value || null,
      group: $("#itemGroup").value || null,
      hidden: $("#itemHidden").checked
    },
    duration_seconds: -1
  };
  try {
    const path = itemId
      ? `/api/v1/admin/playlists/${playlistId}/items/${itemId}`
      : `/api/v1/admin/playlists/${playlistId}/items`;
    await api(path, { method: itemId ? "PATCH" : "POST", body: JSON.stringify(payload) });
    $("#playlistItemDialog").close();
    toast(itemId ? "Channel updated." : "Channel added to playlist.");
    await loadPlaylists();
    const updatedPl = state.playlists?.find((p) => p.id === playlistId);
    if (updatedPl) renderPlaylistItems(updatedPl);
  } catch (error) {
    $("#itemError").textContent = error.message;
  }
});

$("#deleteCurrentChannel")?.addEventListener("click", async () => {
  const playlistId = $("#itemPlaylistId").value;
  const itemId = $("#itemId").value;
  if (!playlistId || !itemId) return;
  $("#playlistItemDialog").close();
  await deletePlaylistItem(playlistId, itemId);
});

async function deletePlaylistItem(playlistId, itemId) {
  if (!confirm("Remove this channel from the lineup?")) return;
  try {
    await api(`/api/v1/admin/playlists/${playlistId}/items/${itemId}`, { method: "DELETE" });
    showToast("Channel removed from lineup.");
    await loadPlaylists();
    const updatedPl = state.playlists?.find((p) => p.id === playlistId);
    if (updatedPl) renderPlaylistItems(updatedPl);
  } catch (error) {
    showToast(error.message);
  }
}

let groupSearchQuery = "";
$("#groupSearchInput")?.addEventListener("input", (e) => {
  groupSearchQuery = e.target.value.trim().toLowerCase();
  renderBoxGroups();
});

function renderBoxGroups() {
  const container = $("#boxGroupList");
  if (!container) return;
  const filtered = state.groups.filter((group) => {
    if (!groupSearchQuery) return true;
    const pl = state.playlists.find((item) => item.id === group.playlist_id);
    return [group.name, pl?.title].filter(Boolean).join(" ").toLowerCase().includes(groupSearchQuery);
  });

  $("#boxGroupEmpty").classList.toggle("hidden", state.groups.length > 0);
  if (!filtered.length && state.groups.length > 0) {
    container.innerHTML = `<div style="text-align:center;color:var(--text-muted);padding:30px;">No box groups match "${escapeHtml(groupSearchQuery)}".</div>`;
    return;
  }

  container.innerHTML = filtered.map((group) => {
    const playlist = state.playlists.find((item) => item.id === group.playlist_id);
    const devices = state.devices.filter((device) => device.box_group_id === group.id);
    const onlineCount = devices.filter(isOnline).length;
    const isBlockPolicy = group.default_channel_policy === "block";

    return `
      <article class="group-card" data-id="${group.id}">
        <div class="card-main-info">
          <div class="group-glyph">▦</div>
          <div class="card-details">
            <h4>${escapeHtml(group.name)}</h4>
            <p>${escapeHtml(playlist ? `${playlist.title} (${(playlist.playlist_items || []).length} channels)` : "No master playlist")}</p>
            <div class="card-meta-pills">
              <span class="meta-pill">${devices.length} Box${devices.length === 1 ? "" : "es"}</span>
              <span class="status ${onlineCount > 0 ? "" : "offline"}">${onlineCount} ONLINE</span>
              <span class="policy-badge ${isBlockPolicy ? "block" : "allow"}">${isBlockPolicy ? "ONLY EXPLICITLY ALLOWED" : "ALL CHANNELS UNLESS BLOCKED"}</span>
            </div>
          </div>
        </div>
        <div class="card-action-bar">
          <button type="button" class="secondary edit-group-policy" data-id="${group.id}">🔒 Channel Policy</button>
          <button type="button" class="secondary push-group-boxes" data-id="${group.id}" ${onlineCount === 0 ? "disabled title='No online boxes to sync'" : ""}>↻ Push to TVs</button>
          <button type="button" class="secondary edit-box-group" data-id="${group.id}">Edit</button>
        </div>
      </article>
    `;
  }).join("");

  $$(".edit-box-group").forEach((button) => button.addEventListener("click", () => openBoxGroup(button.dataset.id)));
  $$(".edit-group-policy").forEach((button) => button.addEventListener("click", () => openChannelPolicy("group", button.dataset.id)));
  $$(".push-group-boxes").forEach((btn) => btn.addEventListener("click", async () => {
    btn.disabled = true;
    try {
      const group = state.groups.find((g) => g.id === btn.dataset.id);
      if (!group) return;
      const groupOnlineBoxes = state.devices.filter((d) => d.box_group_id === group.id && isOnline(d));
      if (!groupOnlineBoxes.length) {
        showToast("No online boxes in this group to sync.", "warning");
        return;
      }
      const token = new Date().toISOString();
      await Promise.all(groupOnlineBoxes.map((d) => api(`/api/v1/admin/devices/${d.id}`, {
        method: "PATCH",
        body: JSON.stringify({
          force_refresh_token: token,
          sync_status: "queued",
          sync_message: "Updating lineup",
          queue_sync: true
        })
      })));
      showToast(`Lineup refresh pushed to ${groupOnlineBoxes.length} online TV${groupOnlineBoxes.length === 1 ? "" : "s"}.`, "success");
      await loadDevices();
    } catch (err) {
      showToast(err.message, "error");
    } finally {
      btn.disabled = false;
    }
  }));
}

function openBoxGroup(id = "") {
  const group = state.groups.find((item) => item.id === id);
  $("#boxGroupId").value = group?.id || "";
  $("#boxGroupName").value = group?.name || "";
  $("#boxGroupPlaylist").innerHTML = state.playlists.map((playlist) =>
    `<option value="${playlist.id}">${escapeHtml(playlist.title)} · ${(playlist.playlist_items || []).length} channels</option>`
  ).join("");
  $("#boxGroupPlaylist").value = group?.playlist_id || state.playlists[0]?.id || "";
  
  const defPolicySelect = $("#boxGroupDefaultPolicy");
  if (defPolicySelect) defPolicySelect.value = group?.default_channel_policy || "allow";

  const devicesContainer = $("#boxGroupDevices");
  if (devicesContainer) {
    devicesContainer.innerHTML = state.devices.map((device) => {
      const isChecked = device.box_group_id === id;
      const isDevOnline = isOnline(device);
      return `
        <label class="group-device-option" data-search="${escapeHtml([device.name, device.room_number].filter(Boolean).join(' ').toLowerCase())}">
          <input type="checkbox" value="${device.id}" ${isChecked ? "checked" : ""}>
          <span><strong>${escapeHtml(device.name || "Untitled")}</strong> <small>${escapeHtml(device.room_number ? `Room ${device.room_number}` : "No room")} · <span style="color:${isDevOnline ? "var(--accent-cyan)" : "var(--text-muted)"}">${isDevOnline ? "ONLINE" : "OFFLINE"}</span></small></span>
        </label>
      `;
    }).join("");
  }

  $("#boxGroupDialogTitle").textContent = group ? `Edit Box Group · ${group.name}` : "Add New Box Group";
  $("#deleteBoxGroup").classList.toggle("hidden", !group);
  $("#boxGroupError").textContent = "";
  $("#boxGroupDialog").showModal();
}

$("#boxGroupDeviceSearch")?.addEventListener("input", (e) => {
  const q = e.target.value.trim().toLowerCase();
  $$("#boxGroupDevices .group-device-option").forEach((opt) => {
    opt.classList.toggle("hidden", q && !opt.dataset.search.includes(q));
  });
});

$("#selectAllBoxesBtn")?.addEventListener("click", () => {
  $$("#boxGroupDevices input[type='checkbox']").forEach((cb) => {
    if (!cb.closest(".group-device-option")?.classList.contains("hidden")) cb.checked = true;
  });
});

$("#clearAllBoxesBtn")?.addEventListener("click", () => {
  $$("#boxGroupDevices input[type='checkbox']").forEach((cb) => {
    if (!cb.closest(".group-device-option")?.classList.contains("hidden")) cb.checked = false;
  });
});

async function saveBoxGroupData(syncToBoxes = true) {
  const oldId = $("#boxGroupId").value;
  $("#boxGroupError").textContent = "";
  try {
    const result = await api(oldId ? `/api/v1/admin/box-groups/${oldId}` : "/api/v1/admin/box-groups", {
      method: oldId ? "PATCH" : "POST",
      body: JSON.stringify({
        name: $("#boxGroupName").value,
        playlistId: $("#boxGroupPlaylist").value,
        defaultPolicy: $("#boxGroupDefaultPolicy")?.value || "allow"
      })
    });
    const groupId = result.group.id;
    const selected = new Set($$("#boxGroupDevices input:checked").map((input) => input.value));
    
    // Update member box assignments
    const affectedDevices = state.devices.filter((device) => selected.has(device.id) || device.box_group_id === groupId);
    if (affectedDevices.length) {
      await Promise.all(affectedDevices.map((device) => api(`/api/v1/admin/devices/${device.id}`, {
        method: "PATCH",
        body: JSON.stringify({
          box_group_id: selected.has(device.id) ? groupId : null,
          queue_sync: syncToBoxes && isOnline(device)
        })
      })));
    }

    $("#boxGroupDialog").close();
    await loadDevices();
    await loadBoxGroups();
    showToast(syncToBoxes ? "Box group saved and pushed to online screens." : "Box group saved as draft (No sync).", "success");
  } catch (error) {
    $("#boxGroupError").textContent = error.message;
    showToast(error.message, "error");
  }
}

$("#boxGroupForm")?.addEventListener("submit", (event) => {
  event.preventDefault();
  saveBoxGroupData(true);
});

$("#saveBoxGroupSyncBtn")?.addEventListener("click", (e) => {
  e.preventDefault();
  saveBoxGroupData(true);
});

$("#saveBoxGroupNoSyncBtn")?.addEventListener("click", (e) => {
  e.preventDefault();
  saveBoxGroupData(false);
});

$("#addBoxGroup")?.addEventListener("click", () => openBoxGroup());
$$('[data-close-box-group]').forEach((button) => button.addEventListener("click", () => $("#boxGroupDialog").close()));

$("#deleteBoxGroup")?.addEventListener("click", async () => {
  const id = $("#boxGroupId").value;
  if (!id || !confirm("Delete this box group? Devices will keep their individual policy rules.")) return;
  await api(`/api/v1/admin/box-groups/${id}`, { method: "DELETE" });
  $("#boxGroupDialog").close();
  await loadDevices();
  await loadBoxGroups();
  showToast("Box group deleted.", "success");
});

let activePolicy = null;
async function openChannelPolicy(targetType, targetId) {
  if (!state.playlists.length) await loadPlaylists();
  const target = targetType === "group" ? state.groups.find((item) => item.id === targetId) : state.devices.find((item) => item.id === targetId);
  const group = targetType === "device" ? state.groups.find((item) => item.id === target?.box_group_id) : target;
  const playlistId = targetType === "group" ? target?.playlist_id : (target?.assigned_playlist_id || group?.playlist_id);
  const playlist = state.playlists.find((item) => item.id === playlistId);
  if (!target || !playlist) { showToast("Assign a master playlist before setting channel policy.", "warning"); return; }
  const own = await api(`/api/v1/admin/channel-policy/${targetType}/${targetId}`);
  let inherited = { rules: [], defaultPolicy: "allow" };
  if (targetType === "device" && group) inherited = await api(`/api/v1/admin/channel-policy/group/${group.id}`);
  activePolicy = { targetType, targetId, playlist, ownRules: new Map(own.rules.filter((rule) => rule.playlist_id === playlist.id).map((rule) => [rule.playlist_item_id, rule.decision])), ownDefault: own.defaultPolicy || (targetType === "group" ? "allow" : "inherit"), groupRules: new Map(inherited.rules.filter((rule) => rule.playlist_id === playlist.id).map((rule) => [rule.playlist_item_id, rule.decision])), groupDefault: inherited.defaultPolicy || "allow", groupName: group?.name || "" };
  $("#channelPolicyTargetType").value = targetType; $("#channelPolicyTargetId").value = targetId; $("#channelPolicyPlaylistId").value = playlist.id;
  $("#channelPolicyTitle").textContent = `${target.name} Channel Policy`; $("#channelPolicySubtitle").textContent = `${playlist.title} · Individual box decisions always have final precedence.`;
  $("#channelPolicyDefault").innerHTML = targetType === "group" ? `<option value="allow">Allow all unless blocked</option><option value="block">Block all unless allowed</option>` : `<option value="inherit">Inherit group/default</option><option value="allow">Allow all unless this box blocks</option><option value="block">Block all unless this box allows</option>`;
  $("#channelPolicyDefault").value = activePolicy.ownDefault; $("#channelPolicySearch").value = "";
  const groups = [...new Set((playlist.playlist_items || []).map((item) => item.metadata?.group).filter(Boolean))].sort();
  $("#channelPolicyGroupFilter").innerHTML = `<option value="">All channel groups</option>` + groups.map((name) => `<option>${escapeHtml(name)}</option>`).join("");
  renderChannelPolicy(); $("#channelPolicyError").textContent = ""; $("#channelPolicyDialog").showModal();
}

function policyEffective(itemId) {
  const own = activePolicy.ownRules.get(itemId); if (own) return { decision: own, reason: activePolicy.targetType === "device" ? "This box" : "This group" };
  if (activePolicy.ownDefault !== "inherit") return { decision: activePolicy.ownDefault, reason: "Default policy" };
  const group = activePolicy.groupRules.get(itemId); if (group) return { decision: group, reason: activePolicy.groupName || "Box group" };
  return { decision: activePolicy.groupDefault || "allow", reason: activePolicy.groupName ? `${activePolicy.groupName} default` : "Playlist default" };
}

function renderChannelPolicy() {
  const query = $("#channelPolicySearch").value.trim().toLowerCase(); const groupFilter = $("#channelPolicyGroupFilter").value;
  const items = [...(activePolicy.playlist.playlist_items || [])].sort((a, b) => Number(a.position) - Number(b.position)).filter((item) => { const meta = item.metadata || {}; return (!groupFilter || meta.group === groupFilter) && [item.title, meta.tvg_chno, meta.group, meta.tvg_id].filter(Boolean).join(" ").toLowerCase().includes(query); });
  let allowed = 0; let blocked = 0;
  $("#channelPolicyList").innerHTML = items.map((item) => { const meta = item.metadata || {}; const effective = policyEffective(item.id); effective.decision === "block" ? blocked++ : allowed++; return `<div class="policy-channel-row" data-item-id="${item.id}"><strong>CH ${escapeHtml(meta.tvg_chno || item.position || "—")}</strong><span><strong>${escapeHtml(item.title)}</strong><small>${escapeHtml(meta.tvg_id || "No EPG ID")}</small></span><span>${escapeHtml(meta.group || "Ungrouped")}</span><span class="policy-result ${effective.decision === "block" ? "blocked" : "allowed"}">${effective.decision.toUpperCase()}<small>${escapeHtml(effective.reason)}</small></span><select class="policy-rule"><option value="">Inherit</option><option value="allow">Allow</option><option value="block">Block</option></select></div>`; }).join("");
  $$(".policy-channel-row").forEach((row) => { const select = row.querySelector("select"); select.value = activePolicy.ownRules.get(row.dataset.itemId) || ""; select.addEventListener("change", () => { select.value ? activePolicy.ownRules.set(row.dataset.itemId, select.value) : activePolicy.ownRules.delete(row.dataset.itemId); renderChannelPolicy(); }); });
  $("#channelPolicySummary").textContent = `${allowed} allowed · ${blocked} blocked · ${items.length} visible in this filter`;
}

$("#channelPolicySearch")?.addEventListener("input", renderChannelPolicy);
$("#channelPolicyGroupFilter")?.addEventListener("change", renderChannelPolicy);
$("#channelPolicyDefault")?.addEventListener("change", (event) => { activePolicy.ownDefault = event.target.value; renderChannelPolicy(); });
function setVisiblePolicy(decision) { $$(".policy-channel-row").forEach((row) => decision ? activePolicy.ownRules.set(row.dataset.itemId, decision) : activePolicy.ownRules.delete(row.dataset.itemId)); renderChannelPolicy(); }
$("#policyAllowVisible")?.addEventListener("click", () => setVisiblePolicy("allow"));
$("#policyBlockVisible")?.addEventListener("click", () => setVisiblePolicy("block"));
$("#policyInheritVisible")?.addEventListener("click", () => setVisiblePolicy(""));
$$('[data-close-channel-policy]').forEach((button) => button.addEventListener("click", () => $("#channelPolicyDialog").close()));

async function saveChannelPolicyData(syncToDevices = true) {
  $("#channelPolicyError").textContent = "";
  try {
    const result = await api(`/api/v1/admin/channel-policy/${activePolicy.targetType}/${activePolicy.targetId}`, {
      method: "PUT",
      body: JSON.stringify({
        playlistId: activePolicy.playlist.id,
        defaultPolicy: activePolicy.ownDefault,
        rules: [...activePolicy.ownRules].map(([playlistItemId, decision]) => ({ playlistItemId, decision })),
        queue_sync: syncToDevices
      })
    });
    $("#channelPolicyDialog").close();
    await loadDevices();
    await loadBoxGroups();
    const count = result.pushedDevices || 0;
    if (syncToDevices) {
      showToast(count > 0 ? `Channel policy saved and pushed to ${count} online screen${count === 1 ? "" : "s"}.` : `Channel policy saved.`, "success");
    } else {
      showToast("Channel policy saved as draft (No sync).", "success");
    }
  } catch (error) {
    $("#channelPolicyError").textContent = error.message;
    showToast(error.message, "error");
  }
}

$("#channelPolicyForm")?.addEventListener("submit", (e) => {
  e.preventDefault();
  saveChannelPolicyData(true);
});

$("#savePolicyDraftBtn")?.addEventListener("click", (e) => {
  e.preventDefault();
  saveChannelPolicyData(false);
});
$("#editDeviceChannelPolicy")?.addEventListener("click", () => openChannelPolicy("device", $("#deviceId").value));
$("#deviceAssignedPlaylist")?.addEventListener("change", () => $("#editDeviceChannelPolicy").disabled = !($("#deviceAssignedPlaylist").value || state.groups.find((group) => group.id === $("#deviceBoxGroup").value)?.playlist_id));
$("#deviceBoxGroup")?.addEventListener("change", () => $("#editDeviceChannelPolicy").disabled = !($("#deviceAssignedPlaylist").value || state.groups.find((group) => group.id === $("#deviceBoxGroup").value)?.playlist_id));

async function openEpgStudio() {
  if (!state.playlists.length) await loadPlaylists();
  const select = $("#epgPlaylistSelect");
  const current = select.value || epgState.playlistId;
  select.innerHTML = `<option value="">Select a TV playlist…</option>` + state.playlists.map((playlist) =>
    `<option value="${playlist.id}">${escapeHtml(playlist.title)} · ${(playlist.playlist_items || []).length} channels</option>`
  ).join("");
  if (current && state.playlists.some((playlist) => playlist.id === current)) select.value = current;
  if (select.value && (!epgState.doc || epgState.playlistId !== select.value)) await loadManagedEpg(select.value);
}

function epgNotice(message, error = false) {
  $("#epgNotice").textContent = message;
  $("#epgNotice").classList.toggle("error", error);
}

function parseXmlTv(text) {
  const doc = new DOMParser().parseFromString(text, "application/xml");
  if (doc.querySelector("parsererror") || doc.documentElement?.tagName.toLowerCase() !== "tv") throw new Error("This is not a valid XMLTV guide.");
  return doc;
}

function serializeEpg() {
  if (!epgState.doc) throw new Error("Load an XMLTV guide first.");
  return `<?xml version="1.0" encoding="UTF-8"?>\n${new XMLSerializer().serializeToString(epgState.doc.documentElement)}`;
}

function xmlTvDate(value) {
  const match = String(value || "").match(/^(\d{4})(\d{2})(\d{2})(\d{2})(\d{2})(\d{2})(?:\s*([+-])(\d{2})(\d{2}))?/);
  if (!match) return null;
  let time = Date.UTC(+match[1], +match[2] - 1, +match[3], +match[4], +match[5], +match[6]);
  if (match[7]) time -= (match[7] === "+" ? 1 : -1) * (+match[8] * 60 + +match[9]) * 60_000;
  return new Date(time);
}

function xmlTvStamp(date) {
  const part = (value) => String(value).padStart(2, "0");
  return `${date.getUTCFullYear()}${part(date.getUTCMonth() + 1)}${part(date.getUTCDate())}${part(date.getUTCHours())}${part(date.getUTCMinutes())}${part(date.getUTCSeconds())} +0000`;
}

function localInputValue(date) {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 16);
}

function setEpgDocument(doc, message) {
  epgState.doc = doc;
  epgState.dirty = false;
  const programmeDates = [...doc.querySelectorAll("programme")].flatMap((node) => [xmlTvDate(node.getAttribute("start")), xmlTvDate(node.getAttribute("stop"))]).filter(Boolean).sort((a, b) => a - b);
  const now = new Date();
  const initialDate = programmeDates.length && (now < programmeDates[0] || now > programmeDates.at(-1)) ? programmeDates[0] : now;
  const localInitialDate = new Date(initialDate.getTime() - initialDate.getTimezoneOffset() * 60_000).toISOString().slice(0, 10);
  $("#epgScheduleDate").value = localInitialDate;
  ["#epgPublish", "#epgExportXml", "#epgExportGz", "#epgAddChannel", "#epgAddProgramme"].forEach((id) => $(id).disabled = false);
  renderEpgTimeline();
  const dates = [...doc.querySelectorAll("programme")].flatMap((node) => [xmlTvDate(node.getAttribute("start")), xmlTvDate(node.getAttribute("stop"))]).filter(Boolean).sort((a, b) => a - b);
  const coverage = dates.length ? ` Coverage: ${dates[0].toLocaleString()} through ${dates.at(-1).toLocaleString()}.` : " This guide contains no programmes.";
  epgNotice(message + coverage);
}

async function loadManagedEpg(playlistId) {
  epgState.playlistId = playlistId;
  if (!playlistId) {
    epgState.doc = null;
    $("#epgTimeline").innerHTML = `<div class="epg-empty">Select a TV playlist to begin.</div>`;
    return;
  }
  epgNotice("Loading managed guide…");
  try {
    const meta = await api(`/api/v1/admin/playlists/${playlistId}/guide`);
    if (!meta.guide) {
      epgState.doc = document.implementation.createDocument(null, "tv");
      epgState.sourceUrl = "";
      epgState.doc.documentElement.setAttribute("generator-info-name", "GLZ Hub EPG Studio");
      setEpgDocument(epgState.doc, "No managed guide yet. Open XMLTV, fetch a URL, or start adding channels and programmes.");
      return;
    }
    epgState.sourceUrl = meta.guide.source_url || "";
    $("#epgFetchUrl").value = epgState.sourceUrl;
    const response = await fetch(`/api/v1/admin/playlists/${playlistId}/guide.xml`, { headers: { authorization: `Bearer ${state.session.access_token}` } });
    if (!response.ok) throw new Error("Could not load the managed guide.");
    setEpgDocument(parseXmlTv(await response.text()), `${meta.guide.channel_count} channels · ${meta.guide.programme_count} programmes · last published ${new Date(meta.guide.updated_at).toLocaleString()}`);
  } catch (error) { epgNotice(error.message, true); }
}

function renderEpgTimeline() {
  if (!epgState.doc) return;
  const dateText = $("#epgScheduleDate").value || new Date().toISOString().slice(0, 10);
  const dayStart = new Date(`${dateText}T00:00:00`);
  const dayEnd = new Date(dayStart.getTime() + 86_400_000);
  const query = $("#epgChannelFilter").value.trim().toLowerCase();
  const allProgrammes = [...epgState.doc.querySelectorAll("programme")];
  const guideChannels = [...epgState.doc.querySelectorAll("channel")];
  const guideById = new Map(guideChannels.map((channel) => [channel.getAttribute("id"), channel]));
  const playlist = state.playlists.find((item) => item.id === epgState.playlistId);
  const channelRows = playlist ? [...(playlist.playlist_items || [])]
    .sort((a, b) => String(a.metadata?.tvg_chno || a.position || "").localeCompare(String(b.metadata?.tvg_chno || b.position || ""), undefined, { numeric: true }))
    .map((item) => ({ channel: guideById.get(item.metadata?.tvg_id), item, id: item.metadata?.tvg_id || "" }))
    : guideChannels.map((channel) => ({ channel, item: null, id: channel.getAttribute("id") }));
  const channels = channelRows.filter(({ channel, item, id }) => {
    const label = `${id} ${item?.title || channel?.querySelector("display-name")?.textContent || ""} ${item?.metadata?.tvg_chno || ""}`.toLowerCase();
    return label.includes(query);
  });
  const hours = Array.from({ length: 24 }, (_, hour) => `<span class="epg-hour" style="left:${hour * 100}px">${String(hour).padStart(2, "0")}:00</span>`).join("");
  const rows = channels.map(({ channel, item, id }) => {
    const name = item?.title || channel?.querySelector("display-name")?.textContent || id || "Unmapped channel";
    const number = item?.metadata?.tvg_chno || item?.position || "—";
    const icon = item?.metadata?.tvg_logo || channel?.querySelector("icon")?.getAttribute("src");
    const programmes = allProgrammes.map((node, index) => ({ node, index })).filter(({ node }) => {
      const start = xmlTvDate(node.getAttribute("start")); const stop = xmlTvDate(node.getAttribute("stop"));
      return node.getAttribute("channel") === id && start && stop && start < dayEnd && stop > dayStart;
    });
    return `<div class="epg-channel-line">
      <button class="epg-channel-label" data-channel-id="${escapeHtml(channel ? id : "")}" ${channel ? "" : "disabled"}>${icon ? `<img src="${escapeHtml(icon)}" alt="">` : "<span>▣</span>"}<span><strong>CH ${escapeHtml(number)} · ${escapeHtml(name)}</strong><small>${channel ? escapeHtml(id) : `No guide match · set tvg-id`}</small></span></button>
      <div class="epg-programmes">${programmes.map(({ node, index }) => {
        const start = xmlTvDate(node.getAttribute("start")); const stop = xmlTvDate(node.getAttribute("stop"));
        const clippedStart = Math.max(start.getTime(), dayStart.getTime()); const clippedStop = Math.min(stop.getTime(), dayEnd.getTime());
        const left = (clippedStart - dayStart.getTime()) / 36_000; const width = Math.max(28, (clippedStop - clippedStart) / 36_000);
        const title = node.querySelector("title")?.textContent || "Untitled programme";
        return `<button class="epg-programme" data-programme-index="${index}" style="left:${left}px;width:${width}px" title="${escapeHtml(title)}"><strong>${escapeHtml(title)}</strong><small>${start.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}–${stop.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}</small></button>`;
      }).join("")}</div>
    </div>`;
  }).join("");
  const now = new Date();
  const nowLine = now >= dayStart && now < dayEnd ? `<div class="epg-now-line" style="left:${170 + (now - dayStart) / 36_000}px"></div>` : "";
  $("#epgTimeline").innerHTML = channels.length ? `<div class="epg-grid">${nowLine}<div class="epg-hours"><div class="epg-corner">${playlist ? "ASSIGNED CH" : "CHANNEL"}</div><div class="epg-hour-scale">${hours}</div></div>${rows}</div>` : `<div class="epg-empty">No channels match this filter.</div>`;
  $$(".epg-channel-label[data-channel-id]:not([disabled])").forEach((button) => button.addEventListener("click", () => openEpgChannelEditor(button.dataset.channelId)));
  $$(".epg-programme").forEach((button) => button.addEventListener("click", () => openEpgProgrammeEditor(Number(button.dataset.programmeIndex))));
}

function markEpgDirty() { epgState.dirty = true; epgNotice("Unsaved guide changes · Publish EPG when ready."); renderEpgTimeline(); }

function openEpgChannelEditor(channelId = "") {
  const channel = channelId ? [...epgState.doc.querySelectorAll("channel")].find((node) => node.getAttribute("id") === channelId) : null;
  $("#epgOriginalChannelId").value = channelId;
  $("#epgChannelId").value = channelId;
  $("#epgChannelName").value = channel?.querySelector("display-name")?.textContent || "";
  $("#epgChannelIcon").value = channel?.querySelector("icon")?.getAttribute("src") || "";
  $("#epgChannelDialogTitle").textContent = channel ? "Edit EPG channel" : "Add EPG channel";
  $("#epgDeleteChannel").classList.toggle("hidden", !channel);
  $("#epgChannelError").textContent = "";
  $("#epgChannelDialog").showModal();
}

function openEpgProgrammeEditor(index = -1) {
  const programmes = [...epgState.doc.querySelectorAll("programme")];
  const node = index >= 0 ? programmes[index] : null;
  const channels = [...epgState.doc.querySelectorAll("channel")];
  $("#epgProgrammeIndex").value = String(index);
  $("#epgProgrammeChannel").innerHTML = channels.map((channel) => `<option value="${escapeHtml(channel.getAttribute("id"))}">${escapeHtml(channel.querySelector("display-name")?.textContent || channel.getAttribute("id"))}</option>`).join("");
  $("#epgProgrammeChannel").value = node?.getAttribute("channel") || channels[0]?.getAttribute("id") || "";
  $("#epgProgrammeTitle").value = node?.querySelector("title")?.textContent || "";
  const defaultStart = new Date(`${$("#epgScheduleDate").value}T12:00:00`);
  $("#epgProgrammeStart").value = localInputValue(xmlTvDate(node?.getAttribute("start")) || defaultStart);
  $("#epgProgrammeEnd").value = localInputValue(xmlTvDate(node?.getAttribute("stop")) || new Date(defaultStart.getTime() + 3_600_000));
  $("#epgProgrammeCategory").value = node?.querySelector("category")?.textContent || "";
  $("#epgProgrammeSubtitle").value = node?.querySelector("sub-title")?.textContent || "";
  $("#epgProgrammeDescription").value = node?.querySelector("desc")?.textContent || "";
  $("#epgProgrammeDialogTitle").textContent = node ? "Edit programme" : "Add programme";
  $("#epgDeleteProgramme").classList.toggle("hidden", !node);
  $("#epgProgrammeError").textContent = "";
  if (!channels.length) { toast("Add an EPG channel first."); return; }
  $("#epgProgrammeDialog").showModal();
}

function replaceChildText(parent, tag, value) {
  parent.querySelectorAll(`:scope > ${tag}`).forEach((node) => node.remove());
  if (!value) return;
  const node = epgState.doc.createElement(tag); node.setAttribute("lang", "en"); node.textContent = value; parent.appendChild(node);
}

function downloadEpgDocument(gzip) {
  let blob = new Blob([serializeEpg()], { type: "application/xml;charset=utf-8" });
  const finish = (download, suffix) => { const url = URL.createObjectURL(download); const link = document.createElement("a"); link.href = url; link.download = `glz-guide.${suffix}`; link.click(); URL.revokeObjectURL(url); };
  if (!gzip) return finish(blob, "xml");
  if (!("CompressionStream" in window)) return toast("This browser cannot create gzip files.");
  new Response(blob.stream().pipeThrough(new CompressionStream("gzip"))).blob().then((result) => finish(result, "xml.gz"));
}

$("#epgPlaylistSelect")?.addEventListener("change", (event) => loadManagedEpg(event.target.value));
$("#epgScheduleDate")?.addEventListener("change", renderEpgTimeline);
$("#epgChannelFilter")?.addEventListener("input", renderEpgTimeline);
$("#epgOpenButton")?.addEventListener("click", () => $("#epgOpenInput").click());
$("#epgOpenInput")?.addEventListener("change", async (event) => { const file = event.target.files?.[0]; event.target.value = ""; if (!file) return; try { epgState.sourceUrl = ""; setEpgDocument(parseXmlTv(await readGuideFile(file)), `Loaded ${file.name} · review and publish when ready.`); } catch (error) { epgNotice(error.message, true); } });
$("#epgFetchButton")?.addEventListener("click", async () => {
  const url = $("#epgFetchUrl").value.trim(); if (!url) return epgNotice("Enter an HTTPS XMLTV URL.", true);
  epgNotice("Fetching and validating guide…");
  try { const response = await fetch(`/api/v1/admin/epg/fetch?url=${encodeURIComponent(url)}`, { headers: { authorization: `Bearer ${state.session.access_token}` } }); if (!response.ok) { const result = await response.json().catch(() => ({})); throw new Error(result.error || "Could not fetch guide."); } epgState.sourceUrl = url; setEpgDocument(parseXmlTv(await response.text()), `Guide loaded from ${new URL(url).hostname} · review and publish when ready. GLZ Hub will refresh this source when accessed after six hours.`); } catch (error) { epgNotice(error.message, true); }
});
$("#epgPublish")?.addEventListener("click", async () => {
  if (!epgState.playlistId || !epgState.doc) return epgNotice("Select a playlist and load a guide first.", true);
  $("#epgPublish").disabled = true;
  try { const result = await api(`/api/v1/admin/playlists/${epgState.playlistId}/guide`, { method: "PUT", body: JSON.stringify({ name: `${state.playlists.find((p) => p.id === epgState.playlistId)?.title || "GLZ TV"} Guide`, sourceUrl: epgState.sourceUrl || null, xml: serializeEpg() }) }); const pushed = await api(`/api/v1/admin/playlists/${epgState.playlistId}/push`, { method: "POST" }); epgState.dirty = false; epgNotice(`${result.guide.channel_count} channels · ${result.guide.programme_count} programmes published and pushed to ${pushed.devices} TV${pushed.devices === 1 ? "" : "s"}.${epgState.sourceUrl ? " Automatic source refresh is active." : ""}`); toast("EPG published."); } catch (error) { epgNotice(error.message, true); } finally { $("#epgPublish").disabled = false; }
});
$("#epgExportXml")?.addEventListener("click", () => downloadEpgDocument(false));
$("#epgExportGz")?.addEventListener("click", () => downloadEpgDocument(true));
$("#epgAddChannel")?.addEventListener("click", () => openEpgChannelEditor());
$("#epgAddProgramme")?.addEventListener("click", () => openEpgProgrammeEditor());
$$('[data-close-epg-channel]').forEach((button) => button.addEventListener("click", () => $("#epgChannelDialog").close()));
$$('[data-close-epg-programme]').forEach((button) => button.addEventListener("click", () => $("#epgProgrammeDialog").close()));

$("#epgChannelForm")?.addEventListener("submit", (event) => {
  event.preventDefault(); const oldId = $("#epgOriginalChannelId").value; const newId = $("#epgChannelId").value.trim();
  let channel = oldId ? [...epgState.doc.querySelectorAll("channel")].find((node) => node.getAttribute("id") === oldId) : null;
  if (!channel) { channel = epgState.doc.createElement("channel"); epgState.doc.documentElement.insertBefore(channel, epgState.doc.querySelector("programme")); }
  channel.setAttribute("id", newId); replaceChildText(channel, "display-name", $("#epgChannelName").value.trim()); channel.querySelectorAll(":scope > icon").forEach((node) => node.remove());
  const iconUrl = $("#epgChannelIcon").value.trim(); if (iconUrl) { const icon = epgState.doc.createElement("icon"); icon.setAttribute("src", iconUrl); channel.appendChild(icon); }
  if (oldId && oldId !== newId) [...epgState.doc.querySelectorAll("programme")].filter((node) => node.getAttribute("channel") === oldId).forEach((node) => node.setAttribute("channel", newId));
  $("#epgChannelDialog").close(); markEpgDirty();
});
$("#epgDeleteChannel")?.addEventListener("click", () => { const id = $("#epgOriginalChannelId").value; if (!id || !confirm(`Delete ${id} and all of its programmes?`)) return; [...epgState.doc.querySelectorAll("channel")].find((node) => node.getAttribute("id") === id)?.remove(); [...epgState.doc.querySelectorAll("programme")].filter((node) => node.getAttribute("channel") === id).forEach((node) => node.remove()); $("#epgChannelDialog").close(); markEpgDirty(); });

$("#epgProgrammeForm")?.addEventListener("submit", (event) => {
  event.preventDefault(); const index = Number($("#epgProgrammeIndex").value); let node = [...epgState.doc.querySelectorAll("programme")][index];
  const start = new Date($("#epgProgrammeStart").value); const stop = new Date($("#epgProgrammeEnd").value); if (!(stop > start)) { $("#epgProgrammeError").textContent = "End time must be after start time."; return; }
  if (!node) { node = epgState.doc.createElement("programme"); epgState.doc.documentElement.appendChild(node); }
  node.setAttribute("channel", $("#epgProgrammeChannel").value); node.setAttribute("start", xmlTvStamp(start)); node.setAttribute("stop", xmlTvStamp(stop));
  replaceChildText(node, "title", $("#epgProgrammeTitle").value.trim()); replaceChildText(node, "sub-title", $("#epgProgrammeSubtitle").value.trim()); replaceChildText(node, "category", $("#epgProgrammeCategory").value.trim()); replaceChildText(node, "desc", $("#epgProgrammeDescription").value.trim());
  $("#epgProgrammeDialog").close(); markEpgDirty();
});
$("#epgDeleteProgramme")?.addEventListener("click", () => { const node = [...epgState.doc.querySelectorAll("programme")][Number($("#epgProgrammeIndex").value)]; if (!node || !confirm("Delete this programme?")) return; node.remove(); $("#epgProgrammeDialog").close(); markEpgDirty(); });
