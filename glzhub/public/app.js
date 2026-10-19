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
  if (response.status === 401) {
    localStorage.removeItem("glzhub_session");
    state.session = null;
    showApp();
  }
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
  const authed = Boolean(state.session) && !inviteToken;
  const shell = $(".shell");
  if (shell) shell.classList.toggle("unauthed", !authed);
  $("#inviteView")?.classList.toggle("hidden", !inviteToken);
  $("#authView")?.classList.toggle("hidden", authed || Boolean(inviteToken));
  $("#appView")?.classList.toggle("hidden", !authed);
  $("#mainRail")?.classList.toggle("hidden", !authed);
  $(".mobile-top-bar")?.classList.toggle("hidden", !authed);
  $("#signOut")?.classList.toggle("hidden", !authed);
  const accountEmail = $("#accountEmail");
  if (accountEmail) accountEmail.textContent = state.session?.user?.email || "Not signed in";
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
  $("#eventsView")?.classList.toggle("hidden", name !== "events");
  $("#studioView").classList.toggle("hidden", name !== "studio");
  $("#epgView").classList.toggle("hidden", name !== "epg");
  $("#pairView").classList.toggle("hidden", name !== "pair");
  $("#settingsView")?.classList.toggle("hidden", name !== "settings");
  $("#pairButton").classList.toggle("hidden", name === "pair" || name === "device-editor" || name === "settings");

  const pageTitles = {
    pair: "Pair a television",
    apps: "App management",
    groups: "Box Groups",
    sites: "Properties",
    experience: "Guest experience",
    radio: "Radio Streams",
    events: "Live Event Channels",
    studio: "Playlist Studio",
    epg: "EPG Studio",
    settings: "Hub Settings & Administration",
    devices: "Your TVs"
  };
  $("#pageTitle").textContent = pageTitles[name] || "Your TVs";

  const breadcrumbSections = {
    devices: { section: "Fleet Management", current: "Devices" },
    groups: { section: "Fleet Management", current: "Box Groups" },
    sites: { section: "Fleet Management", current: "Properties" },
    pair: { section: "Fleet Management", current: "Pair a TV" },
    studio: { section: "Broadcast & Channels", current: "TV Playlists" },
    events: { section: "Broadcast & Channels", current: "Live Events" },
    epg: { section: "Broadcast & Channels", current: "EPG Studio" },
    radio: { section: "Broadcast & Channels", current: "Radio Streams" },
    experience: { section: "Hospitality & Apps", current: "Guest Experience" },
    apps: { section: "Hospitality & Apps", current: "App Library" },
    settings: { section: "System", current: "Hub Settings & Administration" }
  };
  const bc = breadcrumbSections[name] || { section: "Fleet Management", current: "Your TVs" };
  const bcSectionEl = $("#bcSection");
  const bcCurrentEl = $("#bcCurrent");
  if (bcSectionEl) bcSectionEl.textContent = bc.section;
  if (bcCurrentEl) bcCurrentEl.textContent = bc.current;

  $$(".nav").forEach((button) => button.classList.toggle("active", button.dataset.view === (name === "device-editor" ? "devices" : name)));
  if (name === "pair") loadPairingRequests();
  if (name === "radio") loadRadioStations();
  if (name === "events") loadEventChannels();
  if (name === "studio") loadPlaylists();
  if (name === "epg") openEpgStudio();
  if (name === "groups") loadBoxGroups();
  if (name === "settings") renderSettings();
  updateNavBadges();
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

function formatRelativeTime(isoDate) {
  if (!isoDate) return "Never";
  const diffSec = Math.floor((Date.now() - new Date(isoDate).getTime()) / 1000);
  if (diffSec < 15) return "Just now";
  if (diffSec < 60) return `${diffSec}s ago`;
  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) return `${diffMin}m ago`;
  const diffHours = Math.floor(diffMin / 60);
  if (diffHours < 24) return `${diffHours}h ago`;
  const diffDays = Math.floor(diffHours / 24);
  if (diffDays === 1) return "Yesterday";
  if (diffDays < 7) return `${diffDays}d ago`;
  return new Date(isoDate).toLocaleDateString([], { month: "short", day: "numeric" });
}

let activeDeviceFilter = "all";
let deviceSearchQuery = "";

function renderDevices() {
  const total = state.devices.length;
  const onlineCount = state.devices.filter(isOnline).length;
  const syncingCount = state.devices.filter((d) => ["queued", "syncing"].includes(d.sync_status)).length;
  const attentionCount = state.devices.filter((d) => Boolean(d.last_error)).length;

  const devCountEl = $("#deviceCount");
  if (devCountEl) devCountEl.textContent = total;
  const onlineCountEl = $("#onlineCount");
  if (onlineCountEl) onlineCountEl.textContent = onlineCount;
  const syncingCountEl = $("#syncingCount");
  if (syncingCountEl) syncingCountEl.textContent = syncingCount;
  const attentionCountEl = $("#attentionCount");
  if (attentionCountEl) attentionCountEl.textContent = attentionCount;

  const fleetBadge = $("#fleetScoreBadge");
  if (fleetBadge) {
    fleetBadge.textContent = total > 0 ? `${onlineCount} / ${total} ONLINE` : "NO SCREENS";
    fleetBadge.style.color = onlineCount === total && total > 0 ? "var(--success)" : "var(--text-muted)";
  }

  const syncBadge = $("#syncingBadge");
  if (syncBadge) {
    syncBadge.textContent = syncingCount > 0 ? `${syncingCount} IN PROGRESS` : "IN SYNC";
    syncBadge.style.color = syncingCount > 0 ? "var(--accent)" : "var(--text-muted)";
  }

  const attentionBadge = $("#attentionIndicator");
  if (attentionBadge) {
    attentionBadge.textContent = attentionCount > 0 ? `${attentionCount} ATTENTION` : "ALL CLEAR";
    attentionBadge.style.color = attentionCount > 0 ? "var(--danger)" : "var(--success)";
  }

  const query = deviceSearchQuery.trim().toLowerCase();
  const filtered = state.devices.filter((device) => {
    const isDevOnline = isOnline(device);
    const isDevSyncing = ["queued", "syncing"].includes(device.sync_status);

    if (activeDeviceFilter === "online" && !isDevOnline) return false;
    if (activeDeviceFilter === "syncing" && !isDevSyncing) return false;
    if (activeDeviceFilter === "offline" && isDevOnline) return false;
    if (activeDeviceFilter === "attention" && !device.last_error) return false;

    if (!query) return true;
    const siteName = state.sites.find((s) => s.id === device.site_id)?.name || "";
    const text = [device.name, device.guest_name, device.room_number, device.model, siteName, device.platform].filter(Boolean).join(" ").toLowerCase();
    return text.includes(query);
  });

  $("#emptyState")?.classList.toggle("hidden", total > 0);

  if (total === 0) {
    $("#deviceGrid").innerHTML = "";
    return;
  }

  if (filtered.length === 0) {
    $("#deviceGrid").innerHTML = `<div style="padding:40px;text-align:center;color:var(--text-muted);font-size:13.5px;">No screens match the current filter.</div>`;
    return;
  }

  $("#deviceGrid").innerHTML = `<div class="list-head"><span>Device</span><span>Property</span><span>Status</span><span>Activity</span><span>App</span><span>Last contact</span></div>` +
    filtered.map((device) => {
      const activity = deviceActivity(device);
      const isDevOnline = isOnline(device);
      let statusPillClass = "offline";
      let statusLabel = "OFFLINE";
      if (isDevOnline) {
        if (device.sync_status === "syncing") {
          statusPillClass = "syncing";
          statusLabel = "SYNCING";
        } else if (device.sync_status === "queued") {
          statusPillClass = "queued";
          statusLabel = "QUEUED";
        } else if (device.last_error) {
          statusPillClass = "attention";
          statusLabel = "ALERT";
        } else {
          statusPillClass = "online";
          statusLabel = "ONLINE";
        }
      } else if (device.sync_status === "queued") {
        statusPillClass = "queued";
        statusLabel = "QUEUED";
      }

      const ring = String(device.update_channel || "production").toLowerCase();
      const ringBadge = ring === "beta"
        ? ` <span class="meta-pill" style="font-size:10px;padding:1px 5px;color:#f59e0b;border-color:rgba(245,158,11,0.35);">BETA</span>`
        : ring === "weekly"
          ? ` <span class="meta-pill" style="font-size:10px;padding:1px 5px;color:#38bdf8;border-color:rgba(56,189,248,0.35);">WEEKLY</span>`
          : "";
      return `
    <button class="device-row" data-device-id="${device.id}">
      <span class="device-identity">
        <span class="device-screen-badge ${isDevOnline ? "online" : ""}">
          <svg class="icon"><use href="#icon-screens"/></svg>
        </span>
        <span>
          <strong>${escapeHtml(device.name)}</strong>
          <small>Welcome, ${escapeHtml(device.guest_name)}${device.room_number ? ` · Rm ${escapeHtml(device.room_number)}` : ""}</small>
        </span>
      </span>
      <span data-label="Property">${escapeHtml(state.sites.find((site) => site.id === device.site_id)?.name || "Unassigned")}</span>
      <span><span class="status-pill ${statusPillClass}"><span class="status-dot"></span>${statusLabel}</span></span>
      <span data-label="Activity"><span class="activity ${activity.className}">${escapeHtml(activity.label)}${activity.progress == null ? "" : ` · ${activity.progress}%`}</span>${activity.progress == null ? "" : `<span class="sync-track"><span style="width:${activity.progress}%"></span></span>`}</span>
      <span data-label="App">v${escapeHtml(device.app_version || "1.0")}${ringBadge}</span>
      <span data-label="Last contact" title="${device.last_seen_at ? new Date(device.last_seen_at).toLocaleString() : ''}">${formatRelativeTime(device.last_seen_at)}${device.last_error ? `<small class="attention" style="color:var(--danger);display:block;margin-top:2px;">${escapeHtml(device.last_error)}</small>` : ""}</span>
    </button>`;
    }).join("");

  $$(".device-row").forEach((row) => row.addEventListener("click", () => openDevice(row.dataset.deviceId)));
  updateNavBadges();
}

let deviceActivityRefresh = false;
async function refreshDeviceActivity() {
  if (!state.session || $("#devicesView")?.classList.contains("hidden") || deviceActivityRefresh) return;
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
  updateNavBadges();
}

async function loadBoxGroups() {
  return loadDevices();
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
          <div class="card-glyph-badge">
            <svg class="icon"><use href="#icon-property"/></svg>
          </div>
          <div class="card-details">
            <h4>${escapeHtml(site.name)}</h4>
            <p>${escapeHtml(site.address || "No address entered")}</p>
            <div class="card-meta-pills">
              <span class="meta-pill">${devices.length} Screen${devices.length === 1 ? "" : "s"}</span>
              <span class="status-pill ${onlineCount > 0 ? "online" : "offline"}"><span class="status-dot"></span>${onlineCount} ONLINE</span>
            </div>
          </div>
        </div>
        <div class="card-action-bar">
          <button type="button" class="secondary open-site-experience" data-site-id="${site.id}" title="Open guest portal & branding">Guest Experience</button>
          <button type="button" class="secondary filter-site-tvs" data-site-id="${site.id}" title="View screens for this property">Screens</button>
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
  updateNavBadges();
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
    container.innerHTML = `<div style="grid-column:1/-1;text-align:center;color:var(--text-muted);padding:24px;border:1px dashed var(--line);border-radius:var(--radius-md);font-size:13px;">No interactive guest service tiles configured. Click "+ Add Service Tile" above to create amenities, spa, dining, or room service cards.</div>`;
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
  $("#appEmpty")?.classList.toggle("hidden", state.apps.length > 0);
  const container = $("#appList");
  if (!container) return;
  container.innerHTML = state.apps.map((app) => `
    <article class="group-card" data-app-id="${app.id}">
      <div class="card-main-info">
        <div class="card-glyph-badge">
          <svg class="icon"><use href="#icon-apps"/></svg>
        </div>
        <div class="card-details">
          <h4>${escapeHtml(app.name)}</h4>
          <p style="font-family:var(--font-mono); font-size:11.5px;">${escapeHtml(app.package_name)}</p>
          <div class="card-meta-pills">
            <span class="source-badge meta-pill">${app.source_type === "play_store" ? "GOOGLE PLAY STORE" : "HTTPS REPOSITORY"}</span>
            <span class="meta-pill">v${escapeHtml(app.version_name || "Latest")}</span>
          </div>
        </div>
      </div>
      <div class="card-action-bar">
        <button type="button" class="secondary edit-app" data-app-id="${app.id}">Edit</button>
      </div>
    </article>
  `).join("");
  $$(".edit-app").forEach((button) => button.addEventListener("click", () => openApp(button.dataset.appId)));
  updateNavBadges();
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

async function openDevice(id) {
  // A freshly paired device is usually opened before Playlist Studio (or even
  // the dashboard) has finished loading, so state.playlists / sites / groups can
  // still be empty — which left the "Channel lineup" selector with no options
  // until the operator reloaded the portal. Lazy-load whatever is missing.
  const pending = [];
  if (!state.playlists.length) pending.push(loadPlaylists().catch(() => {}));
  if (!state.sites.length || !state.groups.length || !state.apps.length) {
    pending.push(loadDevices().catch(() => {}));
  }
  if (pending.length) await Promise.all(pending);

  const device = state.devices.find((item) => item.id === id);
  if (!device) return;
  $("#deviceId").value = device.id;
  $("#configVersion").value = device.config_version;

  $("#editorDeviceTitle").textContent = device.name || "Untitled Screen";
  const isDevOnline = isOnline(device);
  const statusEl = $("#editorDeviceStatus");
  if (statusEl) {
    let statusClass = "offline";
    let statusLabel = "OFFLINE";
    if (isDevOnline) {
      if (device.sync_status === "syncing") {
        statusClass = "syncing";
        statusLabel = "SYNCING";
      } else if (device.sync_status === "queued") {
        statusClass = "queued";
        statusLabel = "QUEUED";
      } else if (device.last_error) {
        statusClass = "attention";
        statusLabel = "ALERT";
      } else {
        statusClass = "online";
        statusLabel = "ONLINE";
      }
    } else if (device.sync_status === "queued") {
      statusClass = "queued";
      statusLabel = "QUEUED";
    }
    statusEl.className = `status-pill ${statusClass}`;
    statusEl.innerHTML = `<span class="status-dot"></span>${statusLabel}`;
  }
  const modelEl = $("#editorDeviceModel");
  if (modelEl) modelEl.textContent = device.model || "Android TV";
  const siteEl = $("#editorDeviceSite");
  const siteObj = state.sites.find((s) => s.id === device.site_id);
  if (siteEl) siteEl.textContent = siteObj?.name || "Unassigned Property";
  const versionEl = $("#editorDeviceVersion");
  if (versionEl) versionEl.textContent = `App v${device.app_version || "1.0"}`;
  const ringEl = $("#editorDeviceUpdateRing");
  if (ringEl) {
    const ch = String(device.update_channel || "production").toLowerCase();
    ringEl.textContent = ch === "beta" ? "Ring: Beta" : ch === "weekly" ? "Ring: Weekly" : "Ring: Production";
  }

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
  if ($("#appLanguage")) $("#appLanguage").value = device.app_language || "en";
  $("#captionsEnabled").checked = Boolean(device.captions_enabled);
  $("#captionsLanguage").value = device.captions_language || "en";
  $("#autoStart").checked = Boolean(device.auto_start);
  $("#resumeLastChannel").checked = device.resume_last_channel !== false;
  $("#keepAwakeHome").checked = Boolean(device.keep_awake_home);
  $("#sportsBarKioskEnabled").checked = Boolean(device.sports_bar_kiosk_enabled);
  if ($("#updateChannel")) $("#updateChannel").value = device.update_channel || "production";
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

  const checkUpdateBtn = $("#checkUpdateDevice");
  if (checkUpdateBtn) {
    checkUpdateBtn.disabled = !isDevOnline;
    checkUpdateBtn.title = isDevOnline ? "Prompt TV to check for OTA APK updates on its assigned ring" : "Device is offline (Power on TV to check)";
    checkUpdateBtn.style.opacity = isDevOnline ? "1" : "0.5";
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
    iconEl.textContent = type === "success" ? "✓" : (type === "error" ? "✕" : (type === "warning" ? "!" : "i"));
  }

  toastEl.className = `toast ${type} toast-${type} show`;
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

/* ----- Design System, Settings & Theme Engine ----- */
const SETTINGS_KEY = "glzhub_settings";
const DEFAULT_SETTINGS = {
  orgName: "GLZ TV Hospitality Network",
  adminContact: "admin@glztech.com",
  timezone: "America/Puerto_Rico",
  weatherUnit: "fahrenheit",
  refreshInterval: 30,
  soundsEnabled: true,
  alertOverdue: true,
  startDestination: "Home",
  themeMode: "dark",
  osdTimeout: 8,
  captionsLanguage: "en",
  appLanguage: "en",
  updateChannel: "production",
  autoStart: true,
  resumeLastChannel: true,
  keepAwakeHome: false,
  sportsBarKiosk: false,
  autoUpdate: true,
  wifiOnly: false,
  providerFeedUrl: "https://starlite.best/api/list/mygbb8",
  providerPreBuffer: 0.5,
  providerPostBuffer: 0.5,
  masterEpgUrl: "https://play.glztech.com/epg.xml.gz",
  streamHealthTimeout: 6,
  defaultBoxGroupPolicy: "allow",
  hlsLowLatency: true,
  hlsBufferLength: 60,
  hubTheme: "dark",
  hubAccent: "sapphire",
  hubDensity: "comfortable",
  animationsEnabled: true
};

function getSettings() {
  try {
    const raw = localStorage.getItem(SETTINGS_KEY);
    if (!raw) return { ...DEFAULT_SETTINGS };
    return { ...DEFAULT_SETTINGS, ...JSON.parse(raw) };
  } catch {
    return { ...DEFAULT_SETTINGS };
  }
}

function saveSettings(partial) {
  const current = getSettings();
  const updated = { ...current, ...partial };
  try {
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(updated));
  } catch {}
  return updated;
}

function applyTheme(theme, accent, density) {
  const curSettings = getSettings();
  const chosenTheme = theme || curSettings.hubTheme || "dark";
  const chosenAccent = accent || curSettings.hubAccent || "sapphire";
  const chosenDensity = density || curSettings.hubDensity || "comfortable";

  document.documentElement.setAttribute("data-theme", chosenTheme);
  document.documentElement.setAttribute("data-accent", chosenAccent);
  document.documentElement.setAttribute("data-density", chosenDensity);

  try {
    localStorage.setItem("glzhub-theme", chosenTheme);
    localStorage.setItem("glzhub-accent", chosenAccent);
  } catch {}

  const meta = $("#themeColorMeta");
  if (meta) {
    meta.setAttribute("content", chosenTheme === "light" ? "#f8fafc" : (chosenTheme === "midnight" ? "#000000" : "#080d1a"));
  }

  $$("#themeToggle, #themeToggleMobile").forEach((btn) => {
    btn.innerHTML = chosenTheme === "dark" || chosenTheme === "midnight"
      ? `<svg class="icon"><use href="#icon-sun"/></svg>`
      : `<svg class="icon"><use href="#icon-moon"/></svg>`;
    btn.title = chosenTheme === "dark" || chosenTheme === "midnight"
      ? "Switch to light theme"
      : "Switch to dark theme";
  });

  saveSettings({ hubTheme: chosenTheme, hubAccent: chosenAccent, hubDensity: chosenDensity });

  $$(".theme-choice-card").forEach((c) => {
    c.classList.toggle("active", c.dataset.themePick === chosenTheme);
  });
  $$(".accent-pill-btn").forEach((b) => {
    b.classList.toggle("active", b.dataset.accentPick === chosenAccent);
  });
}

function toggleTheme() {
  const current = document.documentElement.getAttribute("data-theme");
  const next = current === "light" ? "dark" : "light";
  applyTheme(next);
}

function updateNavBadges() {
  const onlineCount = state.devices.filter(isOnline).length;
  const total = state.devices.length;
  const devBadge = $("#navCountDevices");
  if (devBadge) devBadge.textContent = total > 0 ? `${onlineCount}/${total}` : "0";

  const groupBadge = $("#navCountGroups");
  if (groupBadge) groupBadge.textContent = String(state.groups.length);

  const siteBadge = $("#navCountSites");
  if (siteBadge) siteBadge.textContent = String(state.sites.length);

  const plBadge = $("#navCountPlaylists");
  if (plBadge) plBadge.textContent = String(state.playlists.length);

  const radioBadge = $("#navCountRadio");
  if (radioBadge) radioBadge.textContent = String(state.stations.length);

  const appBadge = $("#navCountApps");
  if (appBadge) appBadge.textContent = String(state.apps.length);

  const fleetText = $("#fleetHealthText");
  if (fleetText) {
    fleetText.textContent = total > 0
      ? `${onlineCount} of ${total} Online · Edge v3.0.1`
      : "Fleet Ready · Cloudflare Edge";
  }
}

function renderSettings() {
  const s = getSettings();
  if ($("#hubOrgName")) $("#hubOrgName").value = s.orgName;
  if ($("#hubAdminContact")) $("#hubAdminContact").value = s.adminContact;
  if ($("#hubTimezone")) $("#hubTimezone").value = s.timezone;
  if ($("#hubWeatherUnit")) $("#hubWeatherUnit").value = s.weatherUnit;
  if ($("#hubRefreshInterval")) $("#hubRefreshInterval").value = String(s.refreshInterval);
  if ($("#hubSoundsEnabled")) $("#hubSoundsEnabled").checked = Boolean(s.soundsEnabled);
  if ($("#hubAlertOverdue")) $("#hubAlertOverdue").checked = Boolean(s.alertOverdue);

  if ($("#defaultStartDestination")) $("#defaultStartDestination").value = s.startDestination;
  if ($("#defaultThemeMode")) $("#defaultThemeMode").value = s.themeMode;
  if ($("#defaultOsdTimeout")) $("#defaultOsdTimeout").value = String(s.osdTimeout);
  if ($("#defaultCaptionsLanguage")) $("#defaultCaptionsLanguage").value = s.captionsLanguage;
  if ($("#defaultAppLanguage")) $("#defaultAppLanguage").value = s.appLanguage || "en";
  if ($("#defaultUpdateChannel")) $("#defaultUpdateChannel").value = s.updateChannel || "production";
  if ($("#defaultAutoStart")) $("#defaultAutoStart").checked = Boolean(s.autoStart);
  if ($("#defaultResumeLastChannel")) $("#defaultResumeLastChannel").checked = Boolean(s.resumeLastChannel);
  if ($("#defaultKeepAwakeHome")) $("#defaultKeepAwakeHome").checked = Boolean(s.keepAwakeHome);
  if ($("#defaultSportsBarKiosk")) $("#defaultSportsBarKiosk").checked = Boolean(s.sportsBarKiosk);
  if ($("#defaultAutoUpdate")) $("#defaultAutoUpdate").checked = Boolean(s.autoUpdate);
  if ($("#defaultWifiOnly")) $("#defaultWifiOnly").checked = Boolean(s.wifiOnly);

  if ($("#providerFeedUrl")) $("#providerFeedUrl").value = s.providerFeedUrl;
  if ($("#providerPreBuffer")) $("#providerPreBuffer").value = s.providerPreBuffer;
  if ($("#providerPostBuffer")) $("#providerPostBuffer").value = s.providerPostBuffer;
  if ($("#masterEpgUrl")) $("#masterEpgUrl").value = s.masterEpgUrl;
  if ($("#streamHealthTimeout")) $("#streamHealthTimeout").value = s.streamHealthTimeout;
  if ($("#defaultBoxGroupPolicy")) $("#defaultBoxGroupPolicy").value = s.defaultBoxGroupPolicy;
  if ($("#hlsLowLatency")) $("#hlsLowLatency").checked = Boolean(s.hlsLowLatency);
  if ($("#hlsBufferLength")) $("#hlsBufferLength").value = String(s.hlsBufferLength);

  if ($("#hubDensityMode")) $("#hubDensityMode").value = s.hubDensity;
  if ($("#enableAnimations")) $("#enableAnimations").checked = Boolean(s.animationsEnabled);

  $$(".theme-choice-card").forEach((card) => {
    card.classList.toggle("active", card.dataset.themePick === s.hubTheme);
  });
  $$(".accent-pill-btn").forEach((btn) => {
    btn.classList.toggle("active", btn.dataset.accentPick === s.hubAccent);
  });
}

/* ----- Command Palette System ----- */
let selectedCommandIndex = 0;
let currentCommandItems = [];

function openCommandPalette() {
  const dialog = $("#commandPaletteDialog");
  const input = $("#commandSearchInput");
  if (!dialog) return;
  selectedCommandIndex = 0;
  dialog.showModal();
  if (input) {
    input.value = "";
    input.focus();
  }
  renderCommandResults("");
}

function closeCommandPalette() {
  const dialog = $("#commandPaletteDialog");
  if (dialog?.open) dialog.close();
}

function renderCommandResults(query = "") {
  const container = $("#commandResultsList");
  if (!container) return;
  const q = query.trim().toLowerCase();
  const items = [];

  const navViews = [
    { id: "devices", title: "Devices Fleet", sub: "View and manage all active televisions", icon: "screens", category: "Navigation" },
    { id: "settings", title: "Hub Settings", sub: "Fleet defaults, streaming engine, appearance, health", icon: "settings", category: "Navigation" },
    { id: "studio", title: "TV Playlists", sub: "Manage M3U channels and channel lineups", icon: "studio", category: "Navigation" },
    { id: "events", title: "Live Event Channels", sub: "Auto-synced temporary live sports channels", icon: "events", category: "Navigation" },
    { id: "experience", title: "Guest Experience", sub: "Resort branding, Wi-Fi info, service tiles", icon: "experience", category: "Navigation" },
    { id: "epg", title: "EPG Studio", sub: "XMLTV schedules, programmes, and guides", icon: "epg", category: "Navigation" },
    { id: "groups", title: "Box Groups", sub: "Group box assignments and channel policies", icon: "groups", category: "Navigation" },
    { id: "sites", title: "Properties", sub: "Locations, resorts, and site screen grouping", icon: "property", category: "Navigation" },
    { id: "radio", title: "Radio Streams", sub: "Public radio station catalog", icon: "radio", category: "Navigation" },
    { id: "apps", title: "App Library", sub: "Managed Play Store and APK apps", icon: "apps", category: "Navigation" },
    { id: "pair", title: "Pair a TV", sub: "Enroll a new television onto GLZ Hub", icon: "pair", category: "Navigation" }
  ];

  navViews.forEach((v) => {
    if (!q || v.title.toLowerCase().includes(q) || v.sub.toLowerCase().includes(q)) {
      items.push({
        type: "view",
        title: v.title,
        sub: v.sub,
        icon: v.icon,
        category: v.category,
        action: () => { closeCommandPalette(); showView(v.id); }
      });
    }
  });

  const actions = [
    { title: "Sync Entire Fleet", sub: "Push latest configurations to all online TVs", icon: "upload", category: "Actions", action: () => { closeCommandPalette(); syncAllDevices(); } },
    { title: "Refresh Dashboard", sub: "Query latest device telemetry and heartbeats", icon: "refresh", category: "Actions", action: () => { closeCommandPalette(); loadDevices(); toast("Fleet refreshed"); } },
    { title: "Toggle Dark / Light Theme", sub: "Switch color scheme", icon: "sun", category: "Actions", action: () => { toggleTheme(); closeCommandPalette(); } },
    { title: "Export Hub Backup", sub: "Download JSON archive of all settings & lineups", icon: "download", category: "Actions", action: () => { closeCommandPalette(); exportHubBackup(); } }
  ];

  actions.forEach((a) => {
    if (!q || a.title.toLowerCase().includes(q) || a.sub.toLowerCase().includes(q)) {
      items.push({ ...a, type: "action" });
    }
  });

  (state.devices || []).forEach((d) => {
    const text = `${d.name} ${d.guest_name} ${d.room_number || ""} ${d.model || ""}`.toLowerCase();
    if (q && text.includes(q)) {
      const site = state.sites.find((s) => s.id === d.site_id)?.name || "Unassigned";
      items.push({
        type: "device",
        title: d.name || "Untitled Screen",
        sub: `Rm ${d.room_number || "—"} · ${site} (${isOnline(d) ? "Online" : "Offline"})`,
        icon: "screens",
        category: "Televisions",
        badge: isOnline(d) ? "ONLINE" : "OFFLINE",
        action: () => { closeCommandPalette(); openDevice(d.id); }
      });
    }
  });

  (state.playlists || []).forEach((pl) => {
    const text = `${pl.title} ${pl.category || ""}`.toLowerCase();
    if (q && text.includes(q)) {
      items.push({
        type: "playlist",
        title: pl.title,
        sub: `${(pl.playlist_items || []).length} channels · ${pl.category || "TV"}`,
        icon: "studio",
        category: "Playlists",
        action: () => { closeCommandPalette(); showView("studio"); }
      });
    }
  });

  (state.events || []).forEach((ev) => {
    const text = `${ev.title} ${ev.sport_league || ""}`.toLowerCase();
    if (q && text.includes(q)) {
      items.push({
        type: "event",
        title: ev.title,
        sub: `${ev.sport_league} · CH ${ev.channel_number || "30-01"}`,
        icon: "events",
        category: "Live Events",
        badge: isEventActive(ev) ? "LIVE" : "SCHEDULED",
        action: () => { closeCommandPalette(); showView("events"); }
      });
    }
  });

  currentCommandItems = items;
  if (selectedCommandIndex >= items.length) selectedCommandIndex = 0;

  if (items.length === 0) {
    container.innerHTML = `<div style="padding:32px; text-align:center; color:var(--text-muted); font-size:13px;">No results found for "${escapeHtml(query)}"</div>`;
    return;
  }

  let html = "";
  let lastCat = "";
  items.forEach((item, index) => {
    if (item.category !== lastCat) {
      lastCat = item.category;
      html += `<div class="command-group-heading">${escapeHtml(lastCat)}</div>`;
    }
    const isSelected = index === selectedCommandIndex;
    html += `
      <button type="button" class="command-item ${isSelected ? "selected" : ""}" data-idx="${index}">
        <span class="command-item-main">
          <svg class="icon"><use href="#icon-${item.icon}"/></svg>
          <span class="command-item-title">${escapeHtml(item.title)}</span>
          <span class="command-item-sub">${escapeHtml(item.sub)}</span>
        </span>
        ${item.badge ? `<span class="command-item-badge">${escapeHtml(item.badge)}</span>` : ""}
      </button>
    `;
  });

  container.innerHTML = html;
  $$(".command-item").forEach((btn) => {
    btn.addEventListener("click", () => {
      const idx = Number(btn.dataset.idx);
      if (currentCommandItems[idx]) currentCommandItems[idx].action();
    });
  });
}

function exportHubBackup() {
  const settings = getSettings();
  const backup = {
    version: "3.0.0",
    exported_at: new Date().toISOString(),
    settings,
    sites: state.sites,
    groups: state.groups,
    apps: state.apps,
    playlists: (state.playlists || []).map((pl) => ({
      id: pl.id,
      title: pl.title,
      category: pl.category,
      description: pl.description,
      artwork_url: pl.artwork_url,
      epg_url: pl.epg_url,
      is_published: pl.is_published,
      items: pl.playlist_items || []
    })),
    experience: state.experience
  };
  const jsonStr = JSON.stringify(backup, null, 2);
  const blob = new Blob([jsonStr], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  const dateStr = new Date().toISOString().split("T")[0];
  a.download = `glzhub-backup-${dateStr}.json`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
  showToast("Hub configuration backup exported", "success");
}

function importHubBackup(file) {
  if (!file) return;
  const reader = new FileReader();
  reader.onload = (e) => {
    try {
      const data = JSON.parse(e.target.result);
      if (!data || typeof data !== "object") throw new Error("Invalid backup format");
      if (data.settings) {
        saveSettings(data.settings);
        renderSettings();
      }
      showToast("Hub configuration restored from backup", "success");
    } catch (err) {
      showToast(`Import failed: ${err.message}`, "error");
    }
  };
  reader.readAsText(file);
}

async function pingGateway() {
  const el = $("#pingHealthResult");
  if (el) el.textContent = "Pinging…";
  const start = performance.now();
  try {
    const res = await fetch("/api/health");
    const dur = Math.round(performance.now() - start);
    const data = await res.json().catch(() => ({}));
    if (res.ok && data.ok) {
      if (el) el.textContent = `${dur}ms · Healthy (${data.service})`;
      showToast(`Gateway ping: ${dur}ms (Healthy)`, "success");
    } else {
      if (el) el.textContent = `Error ${res.status}`;
      showToast(`Gateway returned status ${res.status}`, "warning");
    }
  } catch (err) {
    if (el) el.textContent = "Offline / Failed";
    showToast(`Gateway ping failed: ${err.message}`, "error");
  }
}

async function applyDefaultsToFleet() {
  const s = getSettings();
  const count = state.devices.length;
  if (!count) {
    showToast("No televisions connected to apply baseline to.", "warning");
    return;
  }
  if (!confirm(`Apply provisioning baseline across all ${count} connected televisions?`)) {
    return;
  }
  showToast(`Applying baseline to ${count} televisions…`, "info");
  let updatedCount = 0;
  for (const device of state.devices) {
    try {
      await api(`/api/v1/admin/devices/${device.id}`, {
        method: "PATCH",
        body: JSON.stringify({
          start_destination: s.startDestination,
          theme_mode: s.themeMode,
          osd_timeout_seconds: Number(s.osdTimeout),
          captions_language: s.captionsLanguage,
          app_language: s.appLanguage || "en",
          update_channel: s.updateChannel || "production",
          auto_start: s.autoStart,
          resume_last_channel: s.resumeLastChannel,
          keep_awake_home: s.keepAwakeHome,
          sports_bar_kiosk_enabled: s.sportsBarKiosk,
          auto_update: s.autoUpdate,
          wifi_only: s.wifiOnly
        })
      });
      updatedCount++;
    } catch (err) {
      console.error(`Failed to update device ${device.id}:`, err);
    }
  }
  await loadDevices();
  showToast(`Baseline provisioned to ${updatedCount} televisions!`, "success");
}

async function syncAllDevices() {
  const btn = $("#syncAllScreensButton");
  if (btn) btn.disabled = true;
  try {
    const res = await api("/api/v1/admin/devices/sync-all", { method: "POST" });
    showToast(`Broadcast sync queued for ${res.queued ?? state.devices.length} screens`, "success");
    await loadDevices();
  } catch (error) {
    showToast(error.message, "error");
  } finally {
    if (btn) btn.disabled = false;
  }
}

applyTheme(localStorage.getItem("glzhub-theme") || "dark", localStorage.getItem("glzhub-accent") || "sapphire");
$$("#themeToggle, #themeToggleMobile").forEach((btn) => btn.addEventListener("click", toggleTheme));

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

$("#syncAllScreensButton")?.addEventListener("click", async () => {
  const btn = $("#syncAllScreensButton");
  if (!btn) return;
  btn.disabled = true;
  try {
    const res = await api("/api/v1/admin/devices/sync-all", { method: "POST" });
    showToast(`Fleet sync broadcasted to ${res.syncedCount || 0} screens`, "success");
    await refreshDeviceActivity();
  } catch (err) {
    showToast(err.message, "error");
  } finally {
    btn.disabled = false;
  }
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
        app_language: $("#appLanguage") ? $("#appLanguage").value : "en",
        captions_enabled: $("#captionsEnabled").checked,
        captions_language: $("#captionsLanguage").value || "en",
        auto_start: $("#autoStart").checked,
        resume_last_channel: $("#resumeLastChannel").checked,
        keep_awake_home: $("#keepAwakeHome").checked,
        sports_bar_kiosk_enabled: $("#sportsBarKioskEnabled").checked,
        update_channel: $("#updateChannel") ? $("#updateChannel").value : "production",
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
    showToast("Force refresh signal sent to TV", "success");
  } catch (error) {
    $("#deviceError").textContent = error.message;
    showToast(error.message, "error");
  }
});

$("#checkUpdateDevice")?.addEventListener("click", async () => {
  const id = $("#deviceId").value;
  if (!id) return;
  $("#deviceError").textContent = "";
  try {
    const channel = $("#updateChannel") ? $("#updateChannel").value : "production";
    await api(`/api/v1/admin/devices/${id}/check-update`, {
      method: "POST",
      body: JSON.stringify({ updateChannel: channel })
    });
    showToast(`OTA update check (${channel}) queued for TV`, "success");
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
          <span><strong>${escapeHtml(device.name || "Untitled")}</strong> <small>${escapeHtml(device.room_number ? `Room ${device.room_number}` : "No room")} · <span style="color:${isDevOnline ? "var(--success)" : "var(--text-muted)"};font-weight:700">${isDevOnline ? "ONLINE" : "OFFLINE"}</span></small></span>
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
$$("[data-view-jump]").forEach((button) => button.addEventListener("click", () => showView(button.dataset.viewJump)));
$$(".nav").forEach((button) => button.addEventListener("click", () => showView(button.dataset.view)));

// User Profile & Settings navigation
$("#userChipBtn")?.addEventListener("click", () => showView("settings"));
$("#userChipBtn")?.addEventListener("keydown", (e) => {
  if (e.key === "Enter" || e.key === " ") {
    e.preventDefault();
    showView("settings");
  }
});

// Command Palette Navigation & Search
$("#globalSearchTriggerBtn")?.addEventListener("click", openCommandPalette);
$("#mobileSearchBtn")?.addEventListener("click", openCommandPalette);
$("#closeCommandPaletteBtn")?.addEventListener("click", closeCommandPalette);
$("#commandSearchInput")?.addEventListener("input", (e) => renderCommandResults(e.target.value));

$("#commandSearchInput")?.addEventListener("keydown", (e) => {
  const items = $$(".command-item");
  if (!items.length) return;
  if (e.key === "ArrowDown") {
    e.preventDefault();
    selectedCommandIndex = (selectedCommandIndex + 1) % items.length;
    items.forEach((item, idx) => item.classList.toggle("selected", idx === selectedCommandIndex));
    items[selectedCommandIndex]?.scrollIntoView({ block: "nearest" });
  } else if (e.key === "ArrowUp") {
    e.preventDefault();
    selectedCommandIndex = (selectedCommandIndex - 1 + items.length) % items.length;
    items.forEach((item, idx) => item.classList.toggle("selected", idx === selectedCommandIndex));
    items[selectedCommandIndex]?.scrollIntoView({ block: "nearest" });
  } else if (e.key === "Enter") {
    e.preventDefault();
    if (currentCommandItems[selectedCommandIndex]) {
      currentCommandItems[selectedCommandIndex].action();
    }
  } else if (e.key === "Escape") {
    closeCommandPalette();
  }
});

document.addEventListener("keydown", (e) => {
  if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") {
    e.preventDefault();
    const dialog = $("#commandPaletteDialog");
    if (dialog?.open) {
      closeCommandPalette();
    } else {
      openCommandPalette();
    }
  }
});

// Settings Center Event Listeners
$$(".settings-tab").forEach((tab) => {
  tab.addEventListener("click", () => {
    $$(".settings-tab").forEach((t) => t.classList.remove("active"));
    $$(".settings-pane").forEach((p) => p.classList.add("hidden"));
    tab.classList.add("active");
    const target = $(`#${tab.dataset.setTab}`);
    if (target) target.classList.remove("hidden");
  });
});

$$(".theme-choice-card").forEach((card) => {
  card.addEventListener("click", () => {
    const theme = card.dataset.themePick;
    applyTheme(theme);
    showToast(`Theme switched to ${theme === "dark" ? "Dark Luxury" : (theme === "midnight" ? "Midnight OLED" : "Clean Light")}`, "info");
  });
});

$$(".accent-pill-btn").forEach((btn) => {
  btn.addEventListener("click", () => {
    const accent = btn.dataset.accentPick;
    const curTheme = document.documentElement.getAttribute("data-theme") || "dark";
    applyTheme(curTheme, accent);
    showToast(`Accent palette updated to ${accent}`, "info");
  });
});

$("#detectTimezoneBtn")?.addEventListener("click", () => {
  try {
    const tz = Intl.DateTimeFormat().resolvedOptions().timeZone;
    if (tz) {
      let optionExists = false;
      const select = $("#hubTimezone");
      if (select) {
        for (let i = 0; i < select.options.length; i++) {
          if (select.options[i].value === tz) {
            select.selectedIndex = i;
            optionExists = true;
            break;
          }
        }
        if (!optionExists) {
          const opt = document.createElement("option");
          opt.value = tz;
          opt.textContent = `${tz} (Detected)`;
          select.appendChild(opt);
          select.value = tz;
        }
        showToast(`Detected timezone: ${tz}`, "success");
      }
    }
  } catch {
    showToast("Could not detect timezone", "warning");
  }
});

$("#saveAllSettingsBtn")?.addEventListener("click", () => {
  const updated = {
    orgName: $("#hubOrgName")?.value || DEFAULT_SETTINGS.orgName,
    adminContact: $("#hubAdminContact")?.value || DEFAULT_SETTINGS.adminContact,
    timezone: $("#hubTimezone")?.value || DEFAULT_SETTINGS.timezone,
    weatherUnit: $("#hubWeatherUnit")?.value || DEFAULT_SETTINGS.weatherUnit,
    refreshInterval: Number($("#hubRefreshInterval")?.value || 30),
    soundsEnabled: Boolean($("#hubSoundsEnabled")?.checked),
    alertOverdue: Boolean($("#hubAlertOverdue")?.checked),

    startDestination: $("#defaultStartDestination")?.value || DEFAULT_SETTINGS.startDestination,
    themeMode: $("#defaultThemeMode")?.value || DEFAULT_SETTINGS.themeMode,
    osdTimeout: Number($("#defaultOsdTimeout")?.value || 8),
    captionsLanguage: $("#defaultCaptionsLanguage")?.value || "en",
    appLanguage: $("#defaultAppLanguage")?.value || "en",
    updateChannel: $("#defaultUpdateChannel")?.value || "production",
    autoStart: Boolean($("#defaultAutoStart")?.checked),
    resumeLastChannel: Boolean($("#defaultResumeLastChannel")?.checked),
    keepAwakeHome: Boolean($("#defaultKeepAwakeHome")?.checked),
    sportsBarKiosk: Boolean($("#defaultSportsBarKiosk")?.checked),
    autoUpdate: Boolean($("#defaultAutoUpdate")?.checked),
    wifiOnly: Boolean($("#defaultWifiOnly")?.checked),

    providerFeedUrl: $("#providerFeedUrl")?.value || DEFAULT_SETTINGS.providerFeedUrl,
    providerPreBuffer: Number($("#providerPreBuffer")?.value ?? 0.5),
    providerPostBuffer: Number($("#providerPostBuffer")?.value ?? 0.5),
    masterEpgUrl: $("#masterEpgUrl")?.value || DEFAULT_SETTINGS.masterEpgUrl,
    streamHealthTimeout: Number($("#streamHealthTimeout")?.value || 6),
    defaultBoxGroupPolicy: $("#defaultBoxGroupPolicy")?.value || "allow",
    hlsLowLatency: Boolean($("#hlsLowLatency")?.checked),
    hlsBufferLength: Number($("#hlsBufferLength")?.value || 60),

    hubDensity: $("#hubDensityMode")?.value || "comfortable",
    animationsEnabled: Boolean($("#enableAnimations")?.checked)
  };
  saveSettings(updated);
  showToast("All Hub settings saved successfully!", "success");
});

$("#pingHealthBtn")?.addEventListener("click", pingGateway);
$("#applyDefaultsToFleetBtn")?.addEventListener("click", applyDefaultsToFleet);
$("#exportSettingsBtn")?.addEventListener("click", exportHubBackup);
$("#exportSettingsBackupBtn")?.addEventListener("click", exportHubBackup);
$("#importSettingsBtn")?.addEventListener("click", () => $("#importSettingsFileInput")?.click());
$("#importSettingsBackupBtn")?.addEventListener("click", () => $("#importSettingsFileInput")?.click());
$("#importSettingsFileInput")?.addEventListener("change", (e) => {
  const file = e.target.files?.[0];
  if (file) importHubBackup(file);
});

$("#testToastSuccessBtn")?.addEventListener("click", () => showToast("Success: Configuration synced cleanly across all displays", "success"));
$("#testToastWarnBtn")?.addEventListener("click", () => showToast("Warning: Television Rm 302 heartbeat delayed >10m", "warning"));
$("#testToastErrorBtn")?.addEventListener("click", () => showToast("Error: Provider feed timeout after 6s", "error"));
$("#clearCacheBtn")?.addEventListener("click", () => {
  if (confirm("Reset local storage and restore default preferences?")) {
    localStorage.removeItem(SETTINGS_KEY);
    localStorage.removeItem("glzhub-theme");
    localStorage.removeItem("glzhub-accent");
    location.reload();
  }
});

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
  loadDevices().catch((err) => {
    console.warn("Session expired or rejected:", err);
    localStorage.removeItem("glzhub_session");
    state.session = null;
    showApp();
  });
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
    card.className = "editor-card";
    card.style.padding = "0";
    card.style.overflow = "hidden";

    card.innerHTML = `
      <div style="padding: 14px 20px; background: var(--panel-header); border-bottom: 1px solid var(--line); display: flex; justify-content: space-between; align-items: center;">
        <div style="display: flex; align-items: center; gap: 10px;">
          <div class="card-glyph-badge" style="width:34px; height:34px;">
            <svg class="icon"><use href="#icon-radio"/></svg>
          </div>
          <div>
            <h4 style="margin: 0; font-size: 14px;">Active Radio Broadcast Streams (${state.stations.length})</h4>
            <small style="color: var(--text-muted); font-size: 11.5px;">Live audio streams broadcasted to GLZ Radio mobile and web engines</small>
          </div>
        </div>
      </div>
      <div style="max-height: 520px; overflow-y: auto;">
        <table class="studio-table">
          <thead>
            <tr>
              <th>Station Name</th>
              <th style="width: 140px;">Station Code</th>
              <th style="width: 130px;">Genre</th>
              <th>Audio Stream URL</th>
              <th style="width: 150px; text-align: right;">Actions</th>
            </tr>
          </thead>
          <tbody>
            ${state.stations.map((station) => `
              <tr>
                <td style="font-weight: 600; color: var(--text-main);">
                  <div style="display: flex; align-items: center; gap: 10px;">
                    ${station.logo_url ? `<img src="${escapeHtml(station.logo_url)}" style="width: 24px; height: 24px; object-fit: contain; border-radius: 4px;" onerror="this.style.display='none'">` : ''}
                    <span>${escapeHtml(station.name)}</span>
                  </div>
                </td>
                <td><code style="color: var(--accent); font-weight: 700;">${escapeHtml(station.station_code)}</code></td>
                <td><span class="meta-pill">${escapeHtml(station.genre)}</span></td>
                <td>
                  <a href="${escapeHtml(station.stream_url)}" target="_blank" style="font-family: var(--font-mono); font-size: 11.5px; max-width: 300px; display: inline-block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${escapeHtml(station.stream_url)}</a>
                </td>
                <td style="text-align: right;">
                  <div style="display: flex; gap: 6px; justify-content: flex-end; align-items: center;">
                    <button type="button" class="secondary small-btn preview-radio-stream" data-url="${escapeHtml(station.stream_url)}" data-name="${escapeHtml(station.name)}">Listen</button>
                    <button type="button" class="secondary small-btn edit-radio" data-id="${station.id}">Edit</button>
                    <button type="button" class="danger-button small-btn delete-radio" data-id="${station.id}">Delete</button>
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
  updateNavBadges();
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
          <div class="card-glyph-badge"><svg class="icon"><use href="#icon-playlists"/></svg></div>
          <div>
            <div style="display: flex; align-items: center; gap: 10px;">
              <h4 style="margin: 0; font-size: 16px; font-weight: 700; color: var(--text-main);">${escapeHtml(pl.title)}</h4>
              <span class="status-pill ${pl.is_published ? "online" : "offline"}">${pl.is_published ? "Published" : "Draft"}</span>
              <span class="meta-pill font-mono">${escapeHtml(pl.category).toUpperCase()}</span>
            </div>
            <small style="color: var(--text-muted); margin-top: 4px; display: block;">${escapeHtml(pl.description || "No description provided")} • <strong>${items.length} channels</strong>${pl.epg_url ? " • EPG connected" : ""}</small>
          </div>
        </div>
        <div class="studio-card-actions">
          <button type="button" class="secondary import-playlist" data-id="${pl.id}">Import M3U</button>
          <button type="button" class="secondary export-playlist" data-id="${pl.id}">Export M3U</button>
          <button type="button" class="secondary edit-guide" data-id="${pl.id}">EPG Studio</button>
          <button type="button" class="secondary push-playlist" data-id="${pl.id}">Push to Fleet</button>
          <button type="button" class="secondary toggle-playlist" data-id="${pl.id}">${pl.is_published ? "Unpublish" : "Publish"}</button>
          <button type="button" class="secondary edit-playlist" data-id="${pl.id}">Settings</button>
          <button type="button" class="primary add-channel-item" data-id="${pl.id}">+ Add Channel</button>
          <button type="button" class="danger-button delete-playlist" data-id="${pl.id}" style="margin:0;">Delete</button>
        </div>
      </div>
      <div class="studio-filter"><input class="playlist-filter" placeholder="Search channels, groups, EPG IDs or URLs…"></div>
      <div>
        ${items.length === 0 ? `
          <div style="padding: 30px; text-align: center; color: var(--text-muted); font-size: 13px;">
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
                    <td style="color:var(--text-muted);font-weight:600;font-variant-numeric:tabular-nums;text-align:center;">#${item.position || 0}</td>
                    <td><input class="studio-inline-field inline-channel-number" data-playlist-id="${pl.id}" data-item-id="${item.id}" value="${escapeHtml(meta.tvg_chno || "")}" aria-label="Channel number" placeholder="—" maxlength="20"></td>
                    <td><div class="studio-channel-title">${meta.tvg_logo ? `<img class="channel-logo-thumb" src="${escapeHtml(meta.tvg_logo)}" alt="">` : `<span class="channel-logo-thumb" style="display:inline-grid;place-items:center;"><svg class="icon" style="width:14px;height:14px;color:var(--text-muted);"><use href="#icon-screens"/></svg></span>`}<input class="studio-inline-field inline-channel-name" data-playlist-id="${pl.id}" data-item-id="${item.id}" value="${escapeHtml(item.title)}" aria-label="Channel name" maxlength="120"></div></td>
                    <td><input class="studio-inline-field inline-channel-group" data-playlist-id="${pl.id}" data-item-id="${item.id}" value="${escapeHtml(meta.group || "")}" aria-label="Channel group" placeholder="—" maxlength="100"></td>
                    <td><input class="studio-inline-field inline-channel-epg" data-playlist-id="${pl.id}" data-item-id="${item.id}" value="${escapeHtml(meta.tvg_id || "")}" aria-label="EPG ID" placeholder="—" maxlength="120"></td>
                    <td><input class="studio-inline-field inline-channel-url" data-playlist-id="${pl.id}" data-item-id="${item.id}" value="${escapeHtml(item.media_url)}" aria-label="Stream URL" maxlength="2048"></td>
                    <td><button type="button" class="status-pill ${meta.hidden ? "offline" : "online"} channel-visibility" data-playlist-id="${pl.id}" data-item-id="${item.id}" data-hidden="${meta.hidden === true}">${meta.hidden ? "Hidden" : "Visible"}</button></td>
                    <td>
                      <div style="display:flex;gap:6px;align-items:center;">
                        <button type="button" class="secondary preview-channel-stream" data-url="${escapeHtml(item.media_url)}" data-title="${escapeHtml(item.title)}" style="padding:4px 8px;" title="Test Stream">Test</button>
                        <button type="button" class="secondary edit-playlist-item" data-item-id="${item.id}" data-playlist-id="${pl.id}" style="padding:4px 8px;">Edit</button>
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
      const count = result.totalDevices || result.pushedDevices || 0;
      const onlineCount = result.onlineDevices || 0;
      showToast(count > 0 ? `Lineup queued for sync on ${count} TV${count === 1 ? "" : "s"} (${onlineCount} active now).` : `No TVs assigned to this lineup.`, count > 0 ? "success" : "warning");
      await loadDevices();
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
  updateNavBadges();
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
      ${meta.tvg_logo ? `<img class="channel-logo-thumb" src="${escapeHtml(meta.tvg_logo)}" alt="">` : `<span class="channel-logo-thumb" style="display:inline-grid;place-items:center;"><svg class="icon" style="width:14px;height:14px;color:var(--text-muted);"><use href="#icon-screens"/></svg></span>`}
      <div class="channel-info-meta">
        <strong>#${item.position || 0} ${escapeHtml(item.title)}</strong>
        <small><span>${escapeHtml(meta.group || "Ungrouped")}</span>${meta.tvg_id ? `<span>EPG: ${escapeHtml(meta.tvg_id)}</span>` : ""}</small>
      </div>
      <button type="button" class="secondary preview-item-stream small-btn" data-url="${escapeHtml(item.media_url)}" data-title="${escapeHtml(item.title)}">Test</button>
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
          <div class="card-glyph-badge"><svg class="icon"><use href="#icon-groups"/></svg></div>
          <div class="card-details">
            <h4>${escapeHtml(group.name)}</h4>
            <p>${escapeHtml(playlist ? `${playlist.title} (${(playlist.playlist_items || []).length} channels)` : "No master playlist")}</p>
            <div class="card-meta-pills">
              <span class="meta-pill">${devices.length} Box${devices.length === 1 ? "" : "es"}</span>
              <span class="status-pill ${onlineCount > 0 ? "online" : "offline"}">${onlineCount} Online</span>
              <span class="policy-badge ${isBlockPolicy ? "block" : "allow"}">${isBlockPolicy ? "Strict Whitelist" : "Open Lineup"}</span>
            </div>
          </div>
        </div>
        <div class="card-action-bar">
          <button type="button" class="secondary edit-group-policy" data-id="${group.id}">Channel Policy</button>
          <button type="button" class="secondary push-group-boxes" data-id="${group.id}" ${devices.length === 0 ? "disabled title='No boxes assigned'" : ""}>Push to Fleet</button>
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
      const groupBoxes = state.devices.filter((d) => d.box_group_id === group.id);
      if (!groupBoxes.length) {
        showToast("No boxes assigned to this group.", "warning");
        return;
      }
      const token = new Date().toISOString();
      const onlineBoxes = groupBoxes.filter(isOnline);
      await Promise.all(groupBoxes.map((d) => api(`/api/v1/admin/devices/${d.id}`, {
        method: "PATCH",
        body: JSON.stringify({
          force_refresh_token: token,
          sync_status: "queued",
          sync_message: isOnline(d) ? "Updating lineup" : "Queued (TV offline)",
          queue_sync: true
        })
      })));
      showToast(`Lineup refresh queued for ${groupBoxes.length} TV${groupBoxes.length === 1 ? "" : "s"} (${onlineBoxes.length} active now).`, "success");
      await loadDevices();
    } catch (err) {
      showToast(err.message, "error");
    } finally {
      btn.disabled = false;
    }
  }));
  updateNavBadges();
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
          <span><strong>${escapeHtml(device.name || "Untitled")}</strong> <small>${escapeHtml(device.room_number ? `Room ${device.room_number}` : "No room")} · <span style="color:${isDevOnline ? "var(--success)" : "var(--text-muted)"};font-weight:700">${isDevOnline ? "ONLINE" : "OFFLINE"}</span></small></span>
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
  try {
    if (!state.playlists.length) await loadPlaylists();
    const target = targetType === "group" ? state.groups.find((item) => item.id === targetId) : state.devices.find((item) => item.id === targetId);
    const group = targetType === "device" ? state.groups.find((item) => item.id === target?.box_group_id) : target;
    const playlistId = targetType === "group" ? target?.playlist_id : (target?.assigned_playlist_id || group?.playlist_id);
    const playlist = state.playlists.find((item) => item.id === playlistId);
    if (!target) { showToast("That box group could not be found — reload and try again.", "warning"); return; }
    if (!playlist) { showToast("Assign a master TV playlist to this group before setting channel policy.", "warning"); return; }
    const own = await api(`/api/v1/admin/channel-policy/${targetType}/${targetId}`);
    let inherited = { rules: [], defaultPolicy: "allow" };
    if (targetType === "device" && group) inherited = await api(`/api/v1/admin/channel-policy/group/${group.id}`);
    activePolicy = { targetType, targetId, playlist, ownRules: new Map((own.rules || []).filter((rule) => rule.playlist_id === playlist.id).map((rule) => [rule.playlist_item_id, rule.decision])), ownDefault: own.defaultPolicy || (targetType === "group" ? "allow" : "inherit"), groupRules: new Map((inherited.rules || []).filter((rule) => rule.playlist_id === playlist.id).map((rule) => [rule.playlist_item_id, rule.decision])), groupDefault: inherited.defaultPolicy || "allow", groupName: group?.name || "" };
    $("#channelPolicyTargetType").value = targetType; $("#channelPolicyTargetId").value = targetId; $("#channelPolicyPlaylistId").value = playlist.id;
    $("#channelPolicyTitle").textContent = `${target.name} Channel Policy`; $("#channelPolicySubtitle").textContent = `${playlist.title} · Individual box decisions always have final precedence.`;
    $("#channelPolicyDefault").innerHTML = targetType === "group" ? `<option value="allow">Allow all unless blocked</option><option value="block">Block all unless allowed</option>` : `<option value="inherit">Inherit group/default</option><option value="allow">Allow all unless this box blocks</option><option value="block">Block all unless this box allows</option>`;
    $("#channelPolicyDefault").value = activePolicy.ownDefault; $("#channelPolicySearch").value = "";
    const groups = [...new Set((playlist.playlist_items || []).map((item) => item.metadata?.group).filter(Boolean))].sort();
    $("#channelPolicyGroupFilter").innerHTML = `<option value="">All channel groups</option>` + groups.map((name) => `<option>${escapeHtml(name)}</option>`).join("");
    renderChannelPolicy(); $("#channelPolicyError").textContent = "";
    $("#channelPolicyDialog").showModal();
  } catch (error) {
    console.error("openChannelPolicy failed", error);
    showToast(`Could not open channel policy: ${error.message}`, "error");
  }
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
  const headHtml = `<div class="policy-channel-head"><span>CH</span><span>Channel</span><span>Group</span><span>Effective</span><span>Rule</span></div>`;
  $("#channelPolicyList").innerHTML = headHtml + (items.length ? items.map((item) => { const meta = item.metadata || {}; const effective = policyEffective(item.id); effective.decision === "block" ? blocked++ : allowed++; return `<div class="policy-channel-row" data-item-id="${item.id}"><strong>${escapeHtml(meta.tvg_chno || item.position || "—")}</strong><span><strong>${escapeHtml(item.title)}</strong><small>${escapeHtml(meta.tvg_id || "No EPG ID")}</small></span><span>${escapeHtml(meta.group || "Ungrouped")}</span><span class="policy-result ${effective.decision === "block" ? "blocked" : "allowed"}">${effective.decision.toUpperCase()}<small>${escapeHtml(effective.reason)}</small></span><select class="policy-rule"><option value="">Inherit</option><option value="allow">Allow</option><option value="block">Block</option></select></div>`; }).join("") : `<div class="policy-empty">No channels match this filter.</div>`);
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
  const nowLine = now >= dayStart && now < dayEnd ? `<div class="epg-now-line" style="left:${240 + (now - dayStart) / 36_000}px"></div>` : "";
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
  try {
    const payload = {
      name: `${state.playlists.find((p) => p.id === epgState.playlistId)?.title || "GLZ TV"} Guide`,
      sourceUrl: epgState.sourceUrl || null,
      // Let the Worker fetch an unchanged remote guide directly, avoiding a
      // second decompressed copy in the browser request and Worker JSON parser.
      ...(!epgState.sourceUrl || epgState.dirty ? { xml: serializeEpg() } : {})
    };
    const result = await api(`/api/v1/admin/playlists/${epgState.playlistId}/guide`, { method: "PUT", body: JSON.stringify(payload) });
    const pushed = await api(`/api/v1/admin/playlists/${epgState.playlistId}/push`, { method: "POST" });
    epgState.dirty = false;
    epgNotice(`${result.guide.channel_count} channels · ${result.guide.programme_count} programmes published and pushed to ${pushed.pushedDevices} TV${pushed.pushedDevices === 1 ? "" : "s"}.${epgState.sourceUrl ? " Automatic source refresh is active." : ""}`);
    toast("EPG published.");
  } catch (error) { epgNotice(error.message, true); } finally { $("#epgPublish").disabled = false; }
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

// Event Channels UI logic
let eventSearchQuery = "";
let activeSportFilter = "ALL";
let activeProvSport = "ALL";
let providerStreamsData = [];
let providerSearchQuery = "";

$("#eventSearchInput")?.addEventListener("input", (e) => {
  eventSearchQuery = e.target.value.trim().toLowerCase();
  renderEventChannels();
});

$$("[data-sport-filter]").forEach((pill) => {
  pill.addEventListener("click", () => {
    $$("[data-sport-filter]").forEach((p) => {
      p.classList.remove("active-sport-pill");
      p.style.opacity = "0.7";
      p.style.border = "none";
    });
    pill.classList.add("active-sport-pill");
    pill.style.opacity = "1";
    pill.style.border = "1px solid var(--accent)";
    activeSportFilter = pill.dataset.sportFilter;
    renderEventChannels();
  });
});

async function loadEventChannels() {
  try {
    const data = await api("/api/v1/admin/event-channels");
    state.events = data.events || [];
    renderEventChannels();
  } catch (error) {
    showToast(`Failed to load event channels: ${error.message}`, "error");
  }
}

function isEventActive(event) {
  if (event.status === "disabled" || event.status === "expired") return false;
  const start = new Date(event.start_time).getTime();
  const end = new Date(event.end_time).getTime();
  const preBuffer = (Number.isFinite(Number(event.pre_buffer_hours)) ? Number(event.pre_buffer_hours) : 0.5) * 3600_000;
  const postBuffer = (Number.isFinite(Number(event.post_buffer_hours)) ? Number(event.post_buffer_hours) : 0.5) * 3600_000;
  const now = Date.now();
  return now >= (start - preBuffer) && now <= (end + postBuffer);
}

function renderEventChannels() {
  const container = $("#eventsList");
  if (!container) return;

  const query = eventSearchQuery.toLowerCase();
  const filtered = (state.events || []).filter((event) => {
    if (activeSportFilter !== "ALL") {
      const sp = String(event.sport_league || "").toUpperCase();
      const title = String(event.title || "").toUpperCase();
      if (activeSportFilter === "MLB" && !sp.includes("MLB") && !title.includes("MLB")) return false;
      if (activeSportFilter === "NFL" && !sp.includes("NFL") && !title.includes("NFL")) return false;
      if (activeSportFilter === "NBA" && !sp.includes("NBA") && !title.includes("NBA")) return false;
      if (activeSportFilter === "NCAA" && !["NCAA", "NCAAF", "CFB", "COLLEGE FOOTBALL"].some(k => sp.includes(k) || title.includes(k))) return false;
      if (activeSportFilter === "NHL" && !sp.includes("NHL") && !title.includes("NHL")) return false;
      if (activeSportFilter === "UFC" && !["UFC", "MMA", "PFL", "BOXING"].some(k => sp.includes(k) || title.includes(k))) return false;
      if (activeSportFilter === "PPV" && !sp.includes("PPV") && !title.includes("PPV")) return false;
      if (activeSportFilter === "CUSTOM" && event.auto_ingested !== false) return false;
    }
    if (!query) return true;
    const searchStr = `${event.title} ${event.sport_league} ${event.tvg_id} ${event.group_title}`.toLowerCase();
    return searchStr.includes(query);
  });

  const activeCount = (state.events || []).filter(isEventActive).length;
  if ($("#eventCount")) $("#eventCount").textContent = state.events.length;
  if ($("#activeEventCount")) $("#activeEventCount").textContent = activeCount;

  if ($("#eventsEmpty")) $("#eventsEmpty").classList.toggle("hidden", state.events.length > 0);
  if (!filtered.length && state.events.length > 0) {
    container.innerHTML = `<div style="text-align:center;color:var(--text-muted);padding:30px;">No live events match "${escapeHtml(eventSearchQuery)}".</div>`;
    return;
  }

  container.innerHTML = filtered.map((event) => {
    const active = isEventActive(event);
    const startDate = new Date(event.start_time);
    const endDate = new Date(event.end_time);

    let statusBadge = `<span class="status-pill online">Active on TVs</span>`;
    if (event.status === "disabled") {
      statusBadge = `<span class="status-pill offline">Disabled</span>`;
    } else if (event.status === "expired" || Date.now() > endDate.getTime() + (event.post_buffer_hours * 3600_000)) {
      statusBadge = `<span class="status-pill offline">Expired</span>`;
    } else if (!active) {
      statusBadge = `<span class="status-pill queued">Scheduled</span>`;
    }

    const healthText = event.health_status || (event.is_online === false ? "Offline" : "Live");
    const healthClass = event.is_online !== false ? "status-pill online" : "status-pill offline";
    const healthBadge = `<span class="${healthClass}">${escapeHtml(healthText)}</span>`;

    const leagueBadge = `<span class="meta-pill font-mono">${escapeHtml(event.sport_league || "SPORTS")}</span>`;
    const chnoBadge = `<span class="meta-pill font-mono">CH ${escapeHtml(event.channel_number || "30-01")}</span>`;

    return `
      <article class="group-card" data-id="${event.id}">
        <div class="card-main-info">
          <div class="card-glyph-badge"><svg class="icon"><use href="#icon-events"/></svg></div>
          <div class="card-details">
            <h4>${chnoBadge} ${escapeHtml(event.title)} ${leagueBadge}</h4>
            <p>${escapeHtml(event.group_title || "Major League Sports (Events)")} · tvg-id: <code>${escapeHtml(event.tvg_id)}</code></p>
            <div class="card-meta-pills" style="margin-top:6px;">
              ${statusBadge}
              ${healthBadge}
              <span class="meta-pill">Game: ${startDate.toLocaleDateString()} ${startDate.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })} - ${endDate.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}</span>
              <span class="meta-pill">Window: -${event.pre_buffer_hours}h / +${event.post_buffer_hours}h</span>
            </div>
          </div>
        </div>
        <div class="card-action-bar">
          <button type="button" class="secondary extend-event-btn" data-id="${event.id}">+1h Window</button>
          <button type="button" class="secondary edit-event-btn" data-id="${event.id}">Edit</button>
          <button type="button" class="danger-button delete-event-btn" data-id="${event.id}">Delete</button>
        </div>
      </article>
    `;
  }).join("");

  $$(".edit-event-btn").forEach((btn) => btn.addEventListener("click", () => openEventDialog(btn.dataset.id)));
  $$(".delete-event-btn").forEach((btn) => btn.addEventListener("click", () => deleteEventChannel(btn.dataset.id)));
  $$(".extend-event-btn").forEach((btn) => btn.addEventListener("click", () => extendEventWindow(btn.dataset.id)));
}

async function openBrowseProviderDialog() {
  const dialog = $("#browseProviderDialog");
  if (!dialog) return;
  $("#providerStreamsList").innerHTML = `<div style="text-align:center; padding:30px; color:var(--text-muted);">Loading provider feed from starlite.best…</div>`;
  dialog.showModal();

  try {
    const res = await api("/api/v1/admin/event-channels/provider-feed");
    providerStreamsData = res.streams || [];
    renderProviderStreams();
  } catch (err) {
    $("#providerStreamsList").innerHTML = `<div style="text-align:center; padding:30px; color:var(--danger,#ef4444);">Failed to load provider feed: ${escapeHtml(err.message)}</div>`;
  }
}

function getVisibleProviderStreams() {
  const query = providerSearchQuery.toLowerCase();
  return providerStreamsData.filter((stream) => {
    if (activeProvSport !== "ALL") {
      const sp = String(stream.sportLeague || "").toUpperCase();
      const title = String(stream.title || "").toUpperCase();
      if (activeProvSport === "MLB" && !sp.includes("MLB") && !title.includes("MLB")) return false;
      if (activeProvSport === "NFL" && !sp.includes("NFL") && !title.includes("NFL")) return false;
      if (activeProvSport === "NBA" && !sp.includes("NBA") && !title.includes("NBA")) return false;
      if (activeProvSport === "NCAA" && !["NCAA", "NCAAF", "CFB", "COLLEGE FOOTBALL"].some(k => sp.includes(k) || title.includes(k))) return false;
      if (activeProvSport === "NHL" && !sp.includes("NHL") && !title.includes("NHL")) return false;
      if (activeProvSport === "UFC" && !["UFC", "MMA", "PFL", "BOXING"].some(k => sp.includes(k) || title.includes(k))) return false;
      if (activeProvSport === "OTHER" && ["MLB", "NFL", "NBA", "NCAA", "NCAAF", "CFB", "COLLEGE FOOTBALL", "NHL", "UFC", "MMA", "PFL", "BOXING"].some(k => sp.includes(k) || title.includes(k))) return false;
    }
    if (!query) return true;
    return `${stream.title} ${stream.sportLeague} ${stream.tvgId} ${stream.groupTitle}`.toLowerCase().includes(query);
  });
}

function renderProviderStreams() {
  const container = $("#providerStreamsList");
  if (!container) return;

  const filtered = getVisibleProviderStreams();
  const existingTvgIds = new Set((state.events || []).map(e => e.tvg_id));

  if (!filtered.length) {
    container.innerHTML = `<div style="text-align:center; padding:30px; color:var(--text-muted);">No provider streams match this filter.</div>`;
    return;
  }

  container.innerHTML = filtered.map((stream, idx) => {
    const alreadyInjected = stream.alreadyInjected || existingTvgIds.has(stream.tvgId);
    const startDate = new Date(stream.startTime);

    const leagueBadge = `<span class="meta-pill font-mono">${escapeHtml(stream.sportLeague || "SPORTS")}</span>`;
    const chnoBadge = `<span class="meta-pill font-mono">CH ${escapeHtml(stream.channelNumber || "30-01")}</span>`;

    const injectBtn = alreadyInjected
      ? `<button type="button" class="secondary small-btn" disabled>Injected</button>`
      : `<button type="button" class="primary small-btn inject-single-btn" data-idx="${idx}">Inject Stream</button>`;

    return `
      <div class="channel-item-row" style="padding:10px 14px; background:var(--panel-card); border:1px solid var(--line); border-radius:var(--radius-sm);">
        <div class="channel-info-meta" style="flex:1;">
          <strong style="font-size:13.5px;">${chnoBadge} ${escapeHtml(stream.title)} ${leagueBadge}</strong>
          <small style="margin-top:4px;">
            <span>${escapeHtml(stream.groupTitle)}</span> · 
            <span>tvg-id: <code>${escapeHtml(stream.tvgId)}</code></span> · 
            <span>Game: ${startDate.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })} ET</span>
          </small>
        </div>
        <div style="display:flex; gap:8px; align-items:center;">
          <button type="button" class="secondary small-btn preview-prov-stream" data-url="${escapeHtml(stream.streamUrl)}" data-title="${escapeHtml(stream.title)}">Test</button>
          ${injectBtn}
        </div>
      </div>
    `;
  }).join("");

  $$(".preview-prov-stream").forEach((btn) => btn.addEventListener("click", () => previewStream(btn.dataset.url, btn.dataset.title, false)));
  $$(".inject-single-btn").forEach((btn) => {
    btn.addEventListener("click", async () => {
      const idx = Number(btn.dataset.idx);
      const stream = filtered[idx];
      if (!stream) return;
      btn.disabled = true;
      btn.textContent = "Injecting…";
      try {
        await api("/api/v1/admin/event-channels/inject-single", {
          method: "POST",
          body: JSON.stringify({ stream })
        });
        showToast(`Successfully injected "${stream.title}" into live channels!`, "success");
        stream.alreadyInjected = true;
        renderProviderStreams();
        await loadEventChannels();
      } catch (err) {
        showToast(`Injection failed: ${err.message}`, "error");
        btn.disabled = false;
        btn.textContent = "Inject Stream";
      }
    });
  });
}

$("#providerStreamSearch")?.addEventListener("input", (e) => {
  providerSearchQuery = e.target.value.trim().toLowerCase();
  renderProviderStreams();
});

$$("[data-prov-sport]").forEach((pill) => {
  pill.addEventListener("click", () => {
    $$("[data-prov-sport]").forEach((p) => p.classList.remove("active-prov-pill"));
    pill.classList.add("active-prov-pill");
    activeProvSport = pill.dataset.provSport;
    renderProviderStreams();
  });
});

$("#injectAllVisibleBtn")?.addEventListener("click", async () => {
  const visible = getVisibleProviderStreams();
  const existingTvgIds = new Set((state.events || []).map(e => e.tvg_id));
  const toInject = visible.filter(s => !s.alreadyInjected && !existingTvgIds.has(s.tvgId));

  if (!toInject.length) {
    showToast("All visible streams in this filter are already injected!", "info");
    return;
  }

  const btn = $("#injectAllVisibleBtn");
  if (btn) btn.disabled = true;
  showToast(`Injecting ${toInject.length} streams...`, "info");

  let successCount = 0;
  for (const stream of toInject) {
    try {
      await api("/api/v1/admin/event-channels/inject-single", {
        method: "POST",
        body: JSON.stringify({ stream })
      });
      stream.alreadyInjected = true;
      successCount++;
    } catch (err) {
      console.error("Single stream injection failed:", err);
    }
  }

  showToast(`Successfully injected ${successCount} streams into GLZ TV!`, "success");
  if (btn) btn.disabled = false;
  renderProviderStreams();
  await loadEventChannels();
});

async function ingestConMeFeed() {
  const btn = $("#syncConMeFeedBtn");
  if (btn) btn.disabled = true;
  showToast("Syncing provider feed from starlite.best…", "info");
  try {
    const res = await api("/api/v1/admin/event-channels/ingest", { method: "POST", body: JSON.stringify({}) });
    showToast(`Successfully ingested ${res.ingestedCount} live event channels from provider!`, "success");
    await loadEventChannels();
  } catch (err) {
    showToast(`Sync failed: ${err.message}`, "error");
  } finally {
    if (btn) btn.disabled = false;
  }
}

async function checkAllStreamHealth() {
  const btn = $("#checkStreamHealthBtn");
  if (btn) btn.disabled = true;
  showToast("Checking live stream health across channels…", "info");
  try {
    const res = await api("/api/v1/admin/event-channels/check-health", { method: "POST", body: JSON.stringify({}) });
    showToast(`Stream Health Check Complete: ${res.onlineCount} Online, ${res.offlineCount} Offline`, res.offlineCount > 0 ? "warning" : "success");
    await loadEventChannels();
  } catch (err) {
    showToast(`Health check failed: ${err.message}`, "error");
  } finally {
    if (btn) btn.disabled = false;
  }
}

async function extendEventWindow(eventId) {
  const event = state.events.find((e) => e.id === eventId);
  if (!event) return;
  const currentEnd = new Date(event.end_time);
  const newEnd = new Date(currentEnd.getTime() + 60 * 60_000).toISOString();
  try {
    await api(`/api/v1/admin/event-channels/${eventId}`, {
      method: "PATCH",
      body: JSON.stringify({ endTime: newEnd, status: "active" })
    });
    showToast("Event window extended by 1 hour.", "success");
    await loadEventChannels();
  } catch (err) {
    showToast(err.message, "error");
  }
}

function openEventDialog(id = "") {
  const event = state.events.find((e) => e.id === id);
  $("#eventId").value = event?.id || "";
  $("#eventTitle").value = event?.title || "";
  $("#eventSportLeague").value = event?.sport_league || "MLB";
  $("#eventGroupTitle").value = event?.group_title || "Major League Sports (Events)";
  if ($("#eventChannelNumber")) $("#eventChannelNumber").value = event?.channel_number || `30-${String((state.events || []).length + 1).padStart(2, "0")}`;
  $("#eventStreamUrl").value = event?.stream_url || "";
  $("#eventLogoUrl").value = event?.logo_url || "";

  const now = new Date();
  const defaultStart = event?.start_time ? new Date(event.start_time) : now;
  const defaultEnd = event?.end_time ? new Date(event.end_time) : new Date(now.getTime() + 3 * 3600_000);

  $("#eventStartTime").value = localInputValue(defaultStart);
  $("#eventEndTime").value = localInputValue(defaultEnd);
  $("#eventPreBuffer").value = event?.pre_buffer_hours ?? 0.5;
  $("#eventPostBuffer").value = event?.post_buffer_hours ?? 0.5;
  $("#eventStatus").value = event?.status || "active";

  $("#eventDialogTitle").textContent = event ? "Edit Event Channel" : "Add Custom Event Channel";
  $("#deleteEventBtn").classList.toggle("hidden", !event);
  $("#eventError").textContent = "";
  $("#eventDialog").showModal();
}

async function saveEventChannel(e) {
  e.preventDefault();
  const id = $("#eventId").value;
  const payload = {
    title: $("#eventTitle").value.trim(),
    sportLeague: $("#eventSportLeague").value,
    groupTitle: $("#eventGroupTitle").value.trim(),
    channelNumber: $("#eventChannelNumber") ? $("#eventChannelNumber").value.trim() : "30-01",
    streamUrl: $("#eventStreamUrl").value.trim(),
    logoUrl: $("#eventLogoUrl").value.trim(),
    startTime: new Date($("#eventStartTime").value).toISOString(),
    endTime: new Date($("#eventEndTime").value).toISOString(),
    preBufferHours: Number($("#eventPreBuffer").value),
    postBufferHours: Number($("#eventPostBuffer").value),
    status: $("#eventStatus").value
  };

  try {
    if (id) {
      await api(`/api/v1/admin/event-channels/${id}`, { method: "PATCH", body: JSON.stringify(payload) });
      showToast("Event channel updated.", "success");
    } else {
      await api("/api/v1/admin/event-channels", { method: "POST", body: JSON.stringify(payload) });
      showToast("Event channel added.", "success");
    }
    $("#eventDialog").close();
    await loadEventChannels();
  } catch (err) {
    $("#eventError").textContent = err.message;
  }
}

async function deleteEventChannel(id) {
  if (!confirm("Delete this temporary event channel?")) return;
  try {
    await api(`/api/v1/admin/event-channels/${id}`, { method: "DELETE" });
    showToast("Event channel deleted.", "success");
    if ($("#eventDialog").open) $("#eventDialog").close();
    await loadEventChannels();
  } catch (err) {
    showToast(err.message, "error");
  }
}

$("#browseProviderStreamsBtn")?.addEventListener("click", openBrowseProviderDialog);
$("#browseEventsEmptyBtn")?.addEventListener("click", openBrowseProviderDialog);
$$("[data-close-browse-provider]").forEach((btn) => btn.addEventListener("click", () => $("#browseProviderDialog").close()));

$("#syncConMeFeedBtn")?.addEventListener("click", ingestConMeFeed);
$("#checkStreamHealthBtn")?.addEventListener("click", checkAllStreamHealth);
$("#syncEventsEmptyBtn")?.addEventListener("click", ingestConMeFeed);
$("#addEventChannelBtn")?.addEventListener("click", () => openEventDialog());
$$("[data-close-event]").forEach((btn) => btn.addEventListener("click", () => $("#eventDialog").close()));
$("#eventForm")?.addEventListener("submit", saveEventChannel);
$("#deleteEventBtn")?.addEventListener("click", () => deleteEventChannel($("#eventId").value));


