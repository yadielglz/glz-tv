const state = {
  config: null, session: null, devices: [], apps: [], sites: [],
  experience: null, selectedSiteId: null, enrollments: [], stations: [], playlists: [], groups: [], selectedDevices: new Set(), previewChannelId: localStorage.getItem("glzhub_preview_channel") || ""
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
  $("#devicesView").classList.toggle("hidden", name !== "devices");
  $("#groupsView").classList.toggle("hidden", name !== "groups");
  $("#sitesView").classList.toggle("hidden", name !== "sites");
  $("#appsView").classList.toggle("hidden", name !== "apps");
  $("#experienceView").classList.toggle("hidden", name !== "experience");
  $("#radioView").classList.toggle("hidden", name !== "radio");
  $("#studioView").classList.toggle("hidden", name !== "studio");
  $("#epgView").classList.toggle("hidden", name !== "epg");
  $("#pairView").classList.toggle("hidden", name !== "pair");
  $("#pairButton").classList.toggle("hidden", name === "pair");
  $("#pageTitle").textContent = name === "pair" ? "Pair a television" :
    name === "apps" ? "App management" : name === "groups" ? "Box Groups" : name === "sites" ? "Properties" :
      name === "experience" ? "Guest experience" : name === "radio" ? "Radio Streams" :
        name === "studio" ? "Playlist Studio" : name === "epg" ? "EPG Studio" : "Your TVs";
  $$(".nav").forEach((button) => button.classList.toggle("active", button.dataset.view === name));
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
  if (device.activity_type === "channel" && device.activity_label) {
    return isOnline(device)
      ? { label: `Watching · ${device.activity_label}`, className: "watching" }
      : { label: `Last watched · ${device.activity_label}`, className: "offline" };
  }
  if (device.activity_type === "app" && device.activity_label) {
    return { label: `App launched · ${device.activity_label}`, className: "app-active" };
  }
  if (!isOnline(device)) return { label: "Offline", className: "offline" };
  return { label: "Home screen", className: "idle" };
}

function escapeHtml(value = "") {
  const div = document.createElement("div");
  div.textContent = value;
  return div.innerHTML;
}

function renderDevices() {
  $("#deviceCount").textContent = state.devices.length;
  const online = state.devices.filter(isOnline).length;
  $("#onlineCount").textContent = online;
  $("#attentionCount").textContent = state.devices.filter((device) => device.last_error).length;
  $("#emptyState").classList.toggle("hidden", state.devices.length > 0);
  state.selectedDevices = new Set([...state.selectedDevices].filter((id) => state.devices.some((device) => device.id === id)));
  $("#deviceGrid").innerHTML = state.devices.length ? `<div class="list-head"><span></span><span>Device</span><span>Property</span><span>Status</span><span>Activity</span><span>App</span><span>Last contact</span></div>` +
    state.devices.map((device) => {
      const activity = deviceActivity(device);
      return `
    <button class="device-row" data-device-id="${device.id}">
      <span class="row-check"><input type="checkbox" data-device-select="${device.id}" ${state.selectedDevices.has(device.id) ? "checked" : ""} aria-label="Select ${escapeHtml(device.name)}"></span>
      <span class="device-identity"><span class="screen-icon"></span><span><strong>${escapeHtml(device.name)}</strong><small>Welcome, ${escapeHtml(device.guest_name)}</small></span></span>
      <span data-label="Property">${escapeHtml(state.sites.find((site) => site.id === device.site_id)?.name || "Unassigned")}</span>
      <span><span class="status ${isOnline(device) ? "" : "offline"}">${isOnline(device) ? "ONLINE" : "OFFLINE"}</span></span>
      <span data-label="Activity"><span class="activity ${activity.className}">${escapeHtml(activity.label)}</span></span>
      <span data-label="App">v${escapeHtml(device.app_version)}</span>
      <span data-label="Last contact">${device.last_seen_at ? new Date(device.last_seen_at).toLocaleString() : "Never"}${device.last_error ? `<small class="attention">${escapeHtml(device.last_error)}</small>` : ""}</span>
    </button>`;
    }).join("") : "";
  $$(".device-row").forEach((row) => row.addEventListener("click", () => openDevice(row.dataset.deviceId)));
  $$('[data-device-select]').forEach((input) => input.addEventListener("click", (event) => event.stopPropagation()));
  $$('[data-device-select]').forEach((input) => input.addEventListener("change", () => { input.checked ? state.selectedDevices.add(input.dataset.deviceSelect) : state.selectedDevices.delete(input.dataset.deviceSelect); updateBulkControls(); }));
  updateBulkControls();
  renderPreviewControls();
}

function updateBulkControls() {
  const count = state.selectedDevices.size;
  $("#bulkSelectionCount").textContent = `${count} selected`;
  $$("#bulkRefresh, #bulkTheme, #bulkPlaylist").forEach((button) => button.disabled = !count);
  $("#selectAllDevices").checked = Boolean(state.devices.length && count === state.devices.length);
  const online = state.devices.filter(isOnline).length;
  const attention = state.devices.filter((device) => device.last_error || !isOnline(device)).length;
  $("#healthSummary").innerHTML = `<div><strong>${online}</strong><span>online now</span></div><div><strong>${state.devices.length - online}</strong><span>offline</span></div><div><strong>${attention}</strong><span>need attention</span></div>`;
}

function previewItems() {
  return state.playlists.flatMap((playlist) => (playlist.playlist_items || []).map((item) => ({ ...item, playlistTitle: playlist.title })));
}

function renderPreviewControls() {
  const select = $("#previewChannel");
  if (!select) return;
  const items = previewItems();
  select.innerHTML = `<option value="">Select a channel…</option>` + items.map((item) => `<option value="${item.id}">${escapeHtml(item.title)} · ${escapeHtml(item.playlistTitle)}</option>`).join("");
  if (items.some((item) => item.id === state.previewChannelId)) select.value = state.previewChannelId;
  const item = items.find((entry) => entry.id === select.value);
  $("#previewEmpty").classList.toggle("hidden", Boolean(item));
  $("#previewVideo").classList.toggle("hidden", !item);
}

async function runBulk(action) {
  const ids = [...state.selectedDevices];
  if (!ids.length) return;
  try {
    if (action === "refresh") {
      await Promise.all(ids.map((id) => api(`/api/v1/admin/devices/${id}/force-refresh`, { method: "POST" })));
      toast(`Refresh queued for ${ids.length} screens.`);
    } else {
      const value = action === "theme" ? $("#bulkTheme").dataset.value : $("#bulkPlaylist").dataset.value;
      const field = action === "theme" ? "theme_mode" : "assigned_playlist_id";
      const previous = ids.map((id) => ({ id, value: state.devices.find((device) => device.id === id)?.[field] || null }));
      await Promise.all(ids.map((id) => api(`/api/v1/admin/devices/${id}`, { method: "PATCH", body: JSON.stringify({ [field]: value || null }) })));
      toast(`${action === "theme" ? "Theme" : "Playlist"} updated on ${ids.length} screens.`, async () => {
        await Promise.all(previous.map((entry) => api(`/api/v1/admin/devices/${entry.id}`, { method: "PATCH", body: JSON.stringify({ [field]: entry.value }) })));
        await loadDevices();
        toast("Bulk change undone.");
      });
      await loadDevices();
    }
  } catch (error) { toast(error.message); }
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
  if (!state.playlists.length) await loadPlaylists();
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

function renderSites() {
  $("#siteEmpty").classList.toggle("hidden", state.sites.length > 0);
  $("#siteList").innerHTML = state.sites.map((site) => {
    const count = state.devices.filter((device) => device.site_id === site.id).length;
    return `<button class="site-row" data-site-id="${site.id}">
      <span class="site-glyph">⌂</span>
      <span><strong>${escapeHtml(site.name)}</strong><small>${escapeHtml(site.address || "No location entered")}</small></span>
      <span>${count} ${count === 1 ? "device" : "devices"}</span>
      <span>Edit</span>
    </button>`;
  }).join("");
  $$(".site-row").forEach((row) => row.addEventListener("click", () => openSite(row.dataset.siteId)));
}

function renderExperience() {
  const profile = state.experience || {};
  $("#propertyName").value = profile.property_name || "GLZ Hotel";
  $("#welcomeMessage").value = profile.welcome_message || "Relax, explore, and enjoy your stay.";
  $("#logoUrl").value = profile.logo_url || "";
  $("#heroImageUrl").value = profile.hero_image_url || "";
  $("#wifiName").value = profile.wifi_name || "";
  $("#wifiInstructions").value = profile.wifi_instructions || "";
  $("#checkoutTime").value = profile.checkout_time || "";
  $("#frontDesk").value = profile.front_desk || "";
  $("#noticeTitle").value = profile.notice_title || "";
  $("#noticeBody").value = profile.notice_body || "";
  $("#guestServices").value = (profile.services || [])
    .map((service) => [service.title, service.subtitle || "", service.actionUrl || ""].join(" | "))
    .join("\n");
}

function openSite(id = "") {
  const site = state.sites.find((item) => item.id === id);
  $("#siteId").value = site?.id || "";
  $("#siteDialogTitle").textContent = site ? "Edit property" : "Add property";
  $("#siteName").value = site?.name || "";
  $("#siteAddress").value = site?.address || "";
  $("#siteError").textContent = "";
  $("#siteDialog").showModal();
}

function serviceRows() {
  return $("#guestServices").value.split("\n").map((line) => line.trim()).filter(Boolean).map((line) => {
    const [title, subtitle = "", actionUrl = ""] = line.split("|").map((part) => part.trim());
    return { title, subtitle: subtitle || null, actionUrl: actionUrl || null };
  });
}

function renderApps() {
  $("#appEmpty").classList.toggle("hidden", state.apps.length > 0);
  $("#appList").innerHTML = state.apps.map((app) => `<article class="app-row">
    <span class="app-glyph">${escapeHtml(app.name.slice(0, 1).toUpperCase())}</span>
    <span><strong>${escapeHtml(app.name)}</strong><small>${escapeHtml(app.package_name)}</small></span>
    <span class="source-badge">${app.source_type === "play_store" ? "PLAY STORE" : "REPOSITORY"}</span>
    <span>${escapeHtml(app.version_name || "Latest")}</span>
  </article>`).join("");
}

function openDevice(id) {
  const device = state.devices.find((item) => item.id === id);
  if (!device) return;
  $("#deviceId").value = device.id;
  $("#configVersion").value = device.config_version;
  $("#dialogTitle").textContent = device.name;
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
  const previewItemsForDevice = state.playlists.flatMap((playlist) => (playlist.playlist_items || []).map((item) => ({ ...item, playlistTitle: playlist.title })));
  $("#homePreviewChannel").innerHTML = `<option value="">No home preview</option>` + previewItemsForDevice.map((item) => {
    const channelId = item.metadata?.tvg_id || item.title;
    return `<option value="${escapeHtml(channelId)}">${escapeHtml(item.title)} · ${escapeHtml(item.playlistTitle)}</option>`;
  }).join("");
  $("#homePreviewChannel").value = device.home_preview_channel_id || "";
  $("#schedulePlaylist").innerHTML = `<option value="">Keep current playlist</option>` + state.playlists.map((playlist) => `<option value="${playlist.id}">${escapeHtml(playlist.title)}</option>`).join("");
  const schedule = device.schedule_rules || {};
  $("#scheduleDays").value = schedule.days || "";
  $("#scheduleStart").value = schedule.start || "";
  $("#scheduleEnd").value = schedule.end || "";
  $("#scheduleTheme").value = schedule.theme || "";
  $("#schedulePlaylist").value = schedule.playlistId || "";
  $("#deviceBoxGroup").innerHTML = `<option value="">No box group</option>` + state.groups.map((group) => `<option value="${group.id}">${escapeHtml(group.name)}</option>`).join("");
  $("#deviceBoxGroup").value = device.box_group_id || "";
  $("#editDeviceChannelPolicy").disabled = !(device.assigned_playlist_id || state.groups.find((group) => group.id === device.box_group_id)?.playlist_id);
  $("#roomNumber").value = device.room_number || "";
  $("#arrivalDate").value = device.arrival_date || "";
  $("#departureDate").value = device.departure_date || "";
  $("#playlistUrl").value = device.playlist_url || "";
  $("#epgUrl").value = device.epg_url || "";
  $("#weatherLocation").value = device.weather_location || "";
  $("#startDestination").value = device.start_destination || "Home";
  $("#themeMode").value = device.theme_mode || "adaptive";
  $("#osdTimeoutSeconds").value = String(device.osd_timeout_seconds || 8);
  $("#captionsEnabled").checked = Boolean(device.captions_enabled);
  $("#captionsLanguage").value = device.captions_language || "en";
  $("#autoStart").checked = Boolean(device.auto_start);
  $("#resumeLastChannel").checked = device.resume_last_channel !== false;
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
  $("#deviceDialog").showModal();
}

function toast(message, undo) {
  const node = $("#toast");
  node.textContent = message;
  if (undo) {
    const button = document.createElement("button");
    button.type = "button"; button.className = "toast-undo"; button.textContent = "Undo";
    button.addEventListener("click", async () => { button.disabled = true; try { await undo(); } catch (error) { toast(error.message); } });
    node.append(" ", button);
  }
  node.classList.add("show");
  clearTimeout(window.__glzToastTimer);
  window.__glzToastTimer = setTimeout(() => node.classList.remove("show"), undo ? 6000 : 2500);
}

// Keep all management screens on the same notification path.
const showToast = toast;

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

$("#deviceForm").addEventListener("submit", async (event) => {
  event.preventDefault();
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
        home_preview_channel_id: $("#homePreviewChannel").value || null,
        box_group_id: $("#deviceBoxGroup").value || null,
        schedule_rules: {
          days: $("#scheduleDays").value.trim() || null,
          start: $("#scheduleStart").value || null,
          end: $("#scheduleEnd").value || null,
          theme: $("#scheduleTheme").value || null,
          playlistId: $("#schedulePlaylist").value || null
        },
        room_number: $("#roomNumber").value || null,
        arrival_date: $("#arrivalDate").value || null,
        departure_date: $("#departureDate").value || null,
        playlist_url: $("#playlistUrl").value || null,
        epg_url: $("#epgUrl").value || null,
        weather_location: $("#weatherLocation").value,
        start_destination: $("#startDestination").value,
        theme_mode: $("#themeMode").value,
        osd_timeout_seconds: Number($("#osdTimeoutSeconds").value || 8),
        captions_enabled: $("#captionsEnabled").checked,
        captions_language: $("#captionsLanguage").value || "en",
        auto_start: $("#autoStart").checked,
        resume_last_channel: $("#resumeLastChannel").checked,
        auto_update: $("#autoUpdate").checked,
        wifi_only: $("#wifiOnly").checked,
        visible_apps: $$("#visibleApps input:checked").map((input) => input.value)
      })
    });
    $("#deviceDialog").close();
    await loadDevices();
    toast("Configuration queued for sync");
  } catch (error) {
    $("#deviceError").textContent = error.message;
  }
});

$("#selectAllDevices")?.addEventListener("change", (event) => {
  state.selectedDevices = event.target.checked ? new Set(state.devices.map((device) => device.id)) : new Set();
  renderDevices();
});
$("#bulkRefresh")?.addEventListener("click", () => runBulk("refresh"));
$("#bulkTheme")?.addEventListener("click", () => {
  const value = prompt("Theme for selected screens (adaptive, dark, light, ocean, sunset, emerald, cyberpunk, midnight):", "adaptive");
  if (value && ["adaptive", "dark", "light", "ocean", "sunset", "emerald", "cyberpunk", "midnight"].includes(value)) { $("#bulkTheme").dataset.value = value; runBulk("theme"); }
});
$("#bulkPlaylist")?.addEventListener("click", () => {
  const options = state.playlists.map((playlist) => `${playlist.id} = ${playlist.title}`).join("\n");
  const value = prompt(`Playlist ID for selected screens:\n${options}`);
  if (value !== null && state.playlists.some((playlist) => playlist.id === value)) { $("#bulkPlaylist").dataset.value = value; runBulk("playlist"); }
});
$("#previewChannel")?.addEventListener("change", (event) => {
  const item = previewItems().find((entry) => entry.id === event.target.value);
  state.previewChannelId = event.target.value;
  localStorage.setItem("glzhub_preview_channel", state.previewChannelId);
  const video = $("#previewVideo");
  video.pause();
  video.src = item?.media_url || item?.mediaUrl || "";
  renderPreviewControls();
});
$("#previewPlay")?.addEventListener("click", () => $("#previewVideo").play().catch(() => toast("This stream cannot autoplay in the browser.")));
$("#previewMute")?.addEventListener("click", () => { const video = $("#previewVideo"); video.muted = !video.muted; $("#previewMute").textContent = video.muted ? "🔇" : "🔊"; });

$("#forceRefreshDevice").addEventListener("click", async () => {
  const id = $("#deviceId").value;
  if (!id) return;
  $("#deviceError").textContent = "";
  try {
    await api(`/api/v1/admin/devices/${id}/force-refresh`, { method: "POST" });
    toast("⚡ Force refresh signal sent to TV");
  } catch (error) {
    $("#deviceError").textContent = error.message;
  }
});

$("#experienceForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  $("#experienceError").textContent = "";
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
        services: serviceRows()
      })
    });
    state.experience = result.profile;
    toast("Guest experience published to this property");
  } catch (error) { $("#experienceError").textContent = error.message; }
});

$("#experienceSite").addEventListener("change", async (event) => {
  state.selectedSiteId = event.target.value || null;
  $("#experienceError").textContent = "";
  try {
    await loadExperience();
    renderExperience();
  } catch (error) {
    $("#experienceError").textContent = error.message;
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
    if (!id) state.selectedSiteId = result.site.id;
    $("#siteDialog").close();
    await loadDevices();
    showView("sites");
    toast(id ? "Property updated" : "Property created");
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
    $("#deviceDialog").close();
    await loadDevices();
    toast("Device pairing removed");
  } catch (error) {
    $("#deviceError").textContent = error.message;
  }
});

$("#addAppButton").addEventListener("click", () => {
  $("#appForm").reset();
  $("#appError").textContent = "";
  $("#appDialog").showModal();
});
$$("[data-close-app]").forEach((button) => button.addEventListener("click", () => $("#appDialog").close()));
$("#appForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  $("#appError").textContent = "";
  try {
    await api("/api/v1/admin/apps", {
      method: "POST", body: JSON.stringify({
        name: $("#appName").value, package_name: $("#appPackage").value,
        source_type: $("#appSource").value, source_url: $("#appUrl").value || null,
        version_name: $("#appVersion").value || null, sha256: $("#appSha").value || null
      })
    });
    $("#appDialog").close();
    await loadDevices();
    toast("App added to library");
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
$("[data-close-dialog]").addEventListener("click", () => $("#deviceDialog").close());

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
                  <div style="display: flex; gap: 8px; justify-content: flex-end;">
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
                <tr><th>Order</th><th>CH #</th><th>Channel name</th><th>Group</th><th>EPG ID</th><th>Stream URL</th><th>Status</th><th></th></tr>
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
                    <td><button type="button" class="secondary edit-playlist-item" data-item-id="${item.id}" data-playlist-id="${pl.id}" style="padding:5px 8px;">Edit logo &amp; details</button></td>
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
      toast(`Playlist and guide pushed to ${result.devices} TV${result.devices === 1 ? "" : "s"}.`);
    } catch (error) { toast(error.message); }
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
  if (!playlist || !playlist.playlist_items || !playlist.playlist_items.length) {
    container.innerHTML = `<div style="color: var(--text-dim, #888); font-size: 0.85rem; text-align: center; padding: 0.8rem 0;">No channels added to this playlist yet.</div>`;
    return;
  }
  const items = [...playlist.playlist_items].sort((a, b) => Number(a.position || 0) - Number(b.position || 0));
  container.innerHTML = items.map((item) => `
    <div style="display: flex; justify-content: space-between; align-items: center; background: rgba(255,255,255,0.05); padding: 0.5rem 0.75rem; border-radius: 6px; font-size: 0.85rem;">
      <div style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 75%;">
        <strong>#${item.position || 0} ${escapeHtml(item.title)}</strong>
        <small style="display: block; opacity: 0.7; overflow: hidden; text-overflow: ellipsis;">${escapeHtml(item.media_url)}</small>
      </div>
      <button type="button" class="danger-button delete-playlist-item" data-item-id="${item.id}" data-playlist-id="${playlist.id}" style="font-size: 0.75rem; padding: 0.2rem 0.5rem;">Delete</button>
    </div>
  `).join("");

  $$(".delete-playlist-item").forEach((btn) => btn.addEventListener("click", () => deletePlaylistItem(btn.dataset.playlistId, btn.dataset.itemId)));
}

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

async function loadBoxGroups() {
  if (!state.playlists.length) await loadPlaylists();
  const result = await api("/api/v1/admin/box-groups");
  state.groups = result.groups || [];
  renderBoxGroups();
}

function renderBoxGroups() {
  const container = $("#boxGroupList"); if (!container) return;
  $("#boxGroupEmpty").classList.toggle("hidden", state.groups.length > 0);
  container.innerHTML = state.groups.map((group) => {
    const playlist = state.playlists.find((item) => item.id === group.playlist_id);
    const devices = group.devices || state.devices.filter((device) => device.box_group_id === group.id);
    return `<article class="group-card"><span class="site-glyph">▦</span><span><h4>${escapeHtml(group.name)}</h4><p>${devices.length} box${devices.length === 1 ? "" : "es"} · ${escapeHtml(playlist?.title || "No playlist")}</p></span><span class="activity">${group.default_channel_policy === "block" ? "ONLY EXPLICITLY ALLOWED" : "ALL EXCEPT BLOCKED"}</span><span class="group-actions"><button class="secondary edit-group-policy" data-id="${group.id}">Channel policy</button><button class="secondary edit-box-group" data-id="${group.id}">Edit group</button></span></article>`;
  }).join("");
  $$(".edit-box-group").forEach((button) => button.addEventListener("click", () => openBoxGroup(button.dataset.id)));
  $$(".edit-group-policy").forEach((button) => button.addEventListener("click", () => openChannelPolicy("group", button.dataset.id)));
}

function openBoxGroup(id = "") {
  const group = state.groups.find((item) => item.id === id);
  $("#boxGroupId").value = group?.id || ""; $("#boxGroupName").value = group?.name || "";
  $("#boxGroupPlaylist").innerHTML = state.playlists.map((playlist) => `<option value="${playlist.id}">${escapeHtml(playlist.title)} · ${(playlist.playlist_items || []).length} channels</option>`).join("");
  $("#boxGroupPlaylist").value = group?.playlist_id || state.playlists[0]?.id || "";
  $("#boxGroupDevices").innerHTML = state.devices.map((device) => `<label><input type="checkbox" value="${device.id}" ${(device.box_group_id === id) ? "checked" : ""}> <span>${escapeHtml(device.name)}<small>${escapeHtml(device.room_number || "No room")}</small></span></label>`).join("");
  $("#boxGroupDialogTitle").textContent = group ? "Edit box group" : "Add box group"; $("#deleteBoxGroup").classList.toggle("hidden", !group); $("#boxGroupError").textContent = ""; $("#boxGroupDialog").showModal();
}

$("#addBoxGroup")?.addEventListener("click", () => openBoxGroup());
$$('[data-close-box-group]').forEach((button) => button.addEventListener("click", () => $("#boxGroupDialog").close()));
$("#boxGroupForm")?.addEventListener("submit", async (event) => {
  event.preventDefault(); const oldId = $("#boxGroupId").value; $("#boxGroupError").textContent = "";
  try {
    const result = await api(oldId ? `/api/v1/admin/box-groups/${oldId}` : "/api/v1/admin/box-groups", { method: oldId ? "PATCH" : "POST", body: JSON.stringify({ name: $("#boxGroupName").value, playlistId: $("#boxGroupPlaylist").value }) });
    const groupId = result.group.id; const selected = new Set($$("#boxGroupDevices input:checked").map((input) => input.value));
    await Promise.all(state.devices.filter((device) => selected.has(device.id) || device.box_group_id === groupId).map((device) => api(`/api/v1/admin/devices/${device.id}`, { method: "PATCH", body: JSON.stringify({ box_group_id: selected.has(device.id) ? groupId : null }) })));
    $("#boxGroupDialog").close(); await loadDevices(); await loadBoxGroups(); toast("Box group saved and queued for sync.");
  } catch (error) { $("#boxGroupError").textContent = error.message; }
});
$("#deleteBoxGroup")?.addEventListener("click", async () => { const id = $("#boxGroupId").value; if (!id || !confirm("Delete this box group? Devices will keep their individual policy rules.")) return; await api(`/api/v1/admin/box-groups/${id}`, { method: "DELETE" }); $("#boxGroupDialog").close(); await loadDevices(); await loadBoxGroups(); toast("Box group deleted."); });

let activePolicy = null;
async function openChannelPolicy(targetType, targetId) {
  if (!state.playlists.length) await loadPlaylists();
  const target = targetType === "group" ? state.groups.find((item) => item.id === targetId) : state.devices.find((item) => item.id === targetId);
  const group = targetType === "device" ? state.groups.find((item) => item.id === target?.box_group_id) : target;
  const playlistId = targetType === "group" ? target?.playlist_id : (target?.assigned_playlist_id || group?.playlist_id);
  const playlist = state.playlists.find((item) => item.id === playlistId);
  if (!target || !playlist) { toast("Assign a master playlist before setting channel policy."); return; }
  const own = await api(`/api/v1/admin/channel-policy/${targetType}/${targetId}`);
  let inherited = { rules: [], defaultPolicy: "allow" };
  if (targetType === "device" && group) inherited = await api(`/api/v1/admin/channel-policy/group/${group.id}`);
  activePolicy = { targetType, targetId, playlist, ownRules: new Map(own.rules.filter((rule) => rule.playlist_id === playlist.id).map((rule) => [rule.playlist_item_id, rule.decision])), ownDefault: own.defaultPolicy || (targetType === "group" ? "allow" : "inherit"), groupRules: new Map(inherited.rules.filter((rule) => rule.playlist_id === playlist.id).map((rule) => [rule.playlist_item_id, rule.decision])), groupDefault: inherited.defaultPolicy || "allow", groupName: group?.name || "" };
  $("#channelPolicyTargetType").value = targetType; $("#channelPolicyTargetId").value = targetId; $("#channelPolicyPlaylistId").value = playlist.id;
  $("#channelPolicyTitle").textContent = `${target.name} channel policy`; $("#channelPolicySubtitle").textContent = `${playlist.title} · Individual box decisions always have final precedence.`;
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

$("#channelPolicySearch")?.addEventListener("input", renderChannelPolicy); $("#channelPolicyGroupFilter")?.addEventListener("change", renderChannelPolicy); $("#channelPolicyDefault")?.addEventListener("change", (event) => { activePolicy.ownDefault = event.target.value; renderChannelPolicy(); });
function setVisiblePolicy(decision) { $$(".policy-channel-row").forEach((row) => decision ? activePolicy.ownRules.set(row.dataset.itemId, decision) : activePolicy.ownRules.delete(row.dataset.itemId)); renderChannelPolicy(); }
$("#policyAllowVisible")?.addEventListener("click", () => setVisiblePolicy("allow")); $("#policyBlockVisible")?.addEventListener("click", () => setVisiblePolicy("block")); $("#policyInheritVisible")?.addEventListener("click", () => setVisiblePolicy(""));
$$('[data-close-channel-policy]').forEach((button) => button.addEventListener("click", () => $("#channelPolicyDialog").close()));
$("#channelPolicyForm")?.addEventListener("submit", async (event) => { event.preventDefault(); $("#channelPolicyError").textContent = ""; try { await api(`/api/v1/admin/channel-policy/${activePolicy.targetType}/${activePolicy.targetId}`, { method: "PUT", body: JSON.stringify({ playlistId: activePolicy.playlist.id, defaultPolicy: activePolicy.ownDefault, rules: [...activePolicy.ownRules].map(([playlistItemId, decision]) => ({ playlistItemId, decision })) }) }); $("#channelPolicyDialog").close(); await loadDevices(); await loadBoxGroups(); toast("Channel policy saved and pushed."); } catch (error) { $("#channelPolicyError").textContent = error.message; } });
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
