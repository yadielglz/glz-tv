const state = {
  config: null, session: null, devices: [], apps: [], sites: [],
  experience: null, selectedSiteId: null, enrollments: []
};
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
  } catch {}
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
  $("#sitesView").classList.toggle("hidden", name !== "sites");
  $("#appsView").classList.toggle("hidden", name !== "apps");
  $("#experienceView").classList.toggle("hidden", name !== "experience");
  $("#radioView").classList.toggle("hidden", name !== "radio");
  $("#studioView").classList.toggle("hidden", name !== "studio");
  $("#pairView").classList.toggle("hidden", name !== "pair");
  $("#pairButton").classList.toggle("hidden", name === "pair");
  $("#pageTitle").textContent = name === "pair" ? "Pair a television" :
    name === "apps" ? "App management" : name === "sites" ? "Properties" :
    name === "experience" ? "Guest experience" : name === "radio" ? "Radio Streams" :
    name === "studio" ? "Playlist Studio" : "Your TVs";
  $$(".nav").forEach((button) => button.classList.toggle("active", button.dataset.view === name));
  if (name === "pair") loadPairingRequests();
  if (name === "radio") loadRadioStations();
  if (name === "studio") loadPlaylists();
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
  $("#deviceGrid").innerHTML = state.devices.length ? `<div class="list-head"><span>Device</span><span>Property</span><span>Status</span><span>Activity</span><span>App</span><span>Last contact</span></div>` +
    state.devices.map((device) => {
      const activity = deviceActivity(device);
      return `
    <button class="device-row" data-device-id="${device.id}">
      <span class="device-identity"><span class="screen-icon"></span><span><strong>${escapeHtml(device.name)}</strong><small>Welcome, ${escapeHtml(device.guest_name)}</small></span></span>
      <span data-label="Property">${escapeHtml(state.sites.find((site) => site.id === device.site_id)?.name || "Unassigned")}</span>
      <span><span class="status ${isOnline(device) ? "" : "offline"}">${isOnline(device) ? "ONLINE" : "OFFLINE"}</span></span>
      <span data-label="Activity"><span class="activity ${activity.className}">${escapeHtml(activity.label)}</span></span>
      <span data-label="App">v${escapeHtml(device.app_version)}</span>
      <span data-label="Last contact">${device.last_seen_at ? new Date(device.last_seen_at).toLocaleString() : "Never"}${device.last_error ? `<small class="attention">${escapeHtml(device.last_error)}</small>` : ""}</span>
    </button>`;
    }).join("") : "";
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

setInterval(() => refreshDeviceActivity().catch(() => {}), 5_000);

async function loadDevices() {
  const [deviceResult, appResult, siteResult] = await Promise.all([
    api("/api/v1/admin/devices"), api("/api/v1/admin/apps"), api("/api/v1/admin/sites")
  ]);
  state.devices = deviceResult.devices;
  state.apps = appResult.apps;
  state.sites = siteResult.sites;
  if (!state.sites.some((site) => site.id === state.selectedSiteId)) {
    state.selectedSiteId = state.sites[0]?.id || null;
  }
  await loadExperience();
  renderDevices();
  renderApps();
  renderSites();
  renderSiteSelectors();
  renderExperience();
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

function toast(message) {
  $("#toast").textContent = message;
  $("#toast").classList.add("show");
  setTimeout(() => $("#toast").classList.remove("show"), 2500);
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
    await api("/api/v1/admin/apps", { method: "POST", body: JSON.stringify({
      name: $("#appName").value, package_name: $("#appPackage").value,
      source_type: $("#appSource").value, source_url: $("#appUrl").value || null,
      version_name: $("#appVersion").value || null, sha256: $("#appSha").value || null
    }) });
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
if (state.session) loadDevices().catch(() => {});

async function loadRadioStations() {
  try {
    const { stations } = await api("/api/v1/radio/stations");
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

  state.stations.forEach((station) => {
    const card = document.createElement("article");
    card.className = "management-card";
    card.innerHTML = `
      <div>
        <div class="eyebrow">${station.genre} • ${station.bitrate} kbps</div>
        <h4>${station.name}</h4>
        <small>Code: ${station.station_code} | Stream: ${station.stream_url}</small>
      </div>
      <div class="form-actions">
        <button class="secondary edit-radio" data-id="${station.id}">Edit</button>
        <button class="danger-button delete-radio" data-id="${station.id}">Delete</button>
      </div>
    `;
    container.appendChild(card);
  });

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
  const payload = {
    id: $("#radioId").value || null,
    name: $("#radioName").value,
    stationCode: $("#radioCode").value,
    genre: $("#radioGenre").value,
    bitrate: Number($("#radioBitrate").value),
    streamUrl: $("#radioStreamUrl").value,
    logoUrl: $("#radioLogoUrl").value || null
  };
  try {
    await api("/api/v1/radio/stations", { method: "POST", body: JSON.stringify(payload) });
    $("#radioDialog").close();
    showToast("Radio station saved.");
    loadRadioStations();
  } catch (error) {
    $("#radioError").textContent = error.message;
  }
});

async function deleteRadioStation(id) {
  if (!confirm("Are you sure you want to delete this radio station?")) return;
  try {
    await api(`/api/v1/radio/stations/${id}`, { method: "DELETE" });
    showToast("Radio station deleted.");
    loadRadioStations();
  } catch (error) {
    showToast(error.message);
  }
}

async function loadPlaylists() {
  try {
    const { playlists } = await api("/api/v1/studio/playlists");
    state.playlists = playlists || [];
    renderPlaylists();
  } catch (error) {
    showToast(error.message);
  }
}

function renderPlaylists() {
  const container = $("#playlistList");
  const empty = $("#studioEmpty");
  container.innerHTML = "";
  empty.classList.toggle("hidden", state.playlists.length > 0);

  state.playlists.forEach((pl) => {
    const card = document.createElement("article");
    card.className = "management-card";
    card.innerHTML = `
      <div>
        <div class="eyebrow">${pl.category} • Target: ${pl.target_app}</div>
        <h4>${pl.title}</h4>
        <small>${pl.description || "No description"} (${(pl.playlist_items || []).length} items)</small>
      </div>
      <div class="form-actions">
        <button class="secondary edit-playlist" data-id="${pl.id}">Edit</button>
        <button class="danger-button delete-playlist" data-id="${pl.id}">Delete</button>
      </div>
    `;
    container.appendChild(card);
  });

  $$(".edit-playlist").forEach((btn) => btn.addEventListener("click", () => openPlaylistDialog(btn.dataset.id)));
  $$(".delete-playlist").forEach((btn) => btn.addEventListener("click", () => deletePlaylist(btn.dataset.id)));
}

function openPlaylistDialog(id = null) {
  const dialog = $("#playlistDialog");
  const pl = state.playlists?.find((p) => p.id === id);
  $("#playlistId").value = pl ? pl.id : "";
  $("#playlistTitle").value = pl ? pl.title : "";
  $("#playlistCategory").value = pl ? pl.category : "";
  $("#playlistTargetApp").value = pl ? pl.target_app : "both";
  $("#playlistDescription").value = pl ? pl.description || "" : "";
  $("#playlistArtworkUrl").value = pl ? pl.artwork_url || "" : "";
  $("#playlistDialogTitle").textContent = pl ? "Edit playlist" : "Add playlist";
  $("#playlistError").textContent = "";
  dialog.showModal();
}

$("#addPlaylistButton")?.addEventListener("click", () => openPlaylistDialog());
$("[data-close-playlist]")?.addEventListener("click", () => $("#playlistDialog").close());

$("#playlistForm")?.addEventListener("submit", async (event) => {
  event.preventDefault();
  $("#playlistError").textContent = "";
  const payload = {
    id: $("#playlistId").value || null,
    title: $("#playlistTitle").value,
    category: $("#playlistCategory").value,
    targetApp: $("#playlistTargetApp").value,
    description: $("#playlistDescription").value || null,
    artworkUrl: $("#playlistArtworkUrl").value || null
  };
  try {
    await api("/api/v1/studio/playlists", { method: "POST", body: JSON.stringify(payload) });
    $("#playlistDialog").close();
    showToast("Playlist saved.");
    loadPlaylists();
  } catch (error) {
    $("#playlistError").textContent = error.message;
  }
});

async function deletePlaylist(id) {
  if (!confirm("Are you sure you want to delete this playlist?")) return;
  try {
    await api(`/api/v1/studio/playlists/${id}`, { method: "DELETE" });
    showToast("Playlist deleted.");
    loadPlaylists();
  } catch (error) {
    showToast(error.message);
  }
}
