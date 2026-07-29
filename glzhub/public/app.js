const state = { config: null, session: null, devices: [] };
const inviteParams = new URLSearchParams(location.hash.replace(/^#/, ""));
const inviteToken = inviteParams.get("type") === "invite" ? inviteParams.get("access_token") : null;
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
  $("#pairView").classList.toggle("hidden", name !== "pair");
  $("#pairButton").classList.toggle("hidden", name === "pair");
  $("#pageTitle").textContent = name === "pair" ? "Pair a television" : "Your TVs";
  $$(".nav").forEach((button) => button.classList.toggle("active", button.dataset.view === name));
}

function isOnline(device) {
  return device.last_seen_at && Date.now() - new Date(device.last_seen_at).getTime() < 10 * 60_000;
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
  $("#deviceGrid").innerHTML = state.devices.map((device) => `
    <button class="device-card" data-device-id="${device.id}">
      <div class="device-top"><span class="screen-icon"></span>
        <span class="status ${isOnline(device) ? "" : "offline"}">${isOnline(device) ? "ONLINE" : "OFFLINE"}</span>
      </div>
      <h3>${escapeHtml(device.name)}</h3>
      <p>Welcome, ${escapeHtml(device.guest_name)}</p>
      <div class="device-meta">
        <span>${escapeHtml(device.platform)} · ${escapeHtml(device.model)}</span>
        <span>v${escapeHtml(device.app_version)}</span>
      </div>
    </button>`).join("");
  $$(".device-card").forEach((card) => card.addEventListener("click", () => openDevice(card.dataset.deviceId)));
}

async function loadDevices() {
  const result = await api("/api/v1/admin/devices");
  state.devices = result.devices;
  renderDevices();
}

function openDevice(id) {
  const device = state.devices.find((item) => item.id === id);
  if (!device) return;
  $("#deviceId").value = device.id;
  $("#configVersion").value = device.config_version;
  $("#dialogTitle").textContent = device.name;
  $("#deviceName").value = device.name || "";
  $("#guestName").value = device.guest_name || "";
  $("#playlistUrl").value = device.playlist_url || "";
  $("#epgUrl").value = device.epg_url || "";
  $("#weatherLocation").value = device.weather_location || "";
  $("#startDestination").value = device.start_destination || "Home";
  $("#themeMode").value = device.theme_mode || "adaptive";
  $("#visibleApps").value = (device.visible_apps || []).map((app) => typeof app === "string" ? app : app.packageName).join("\n");
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
        guestName: $("#pairGuestName").value || "Guest"
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
        playlist_url: $("#playlistUrl").value || null,
        epg_url: $("#epgUrl").value || null,
        weather_location: $("#weatherLocation").value,
        start_destination: $("#startDestination").value,
        theme_mode: $("#themeMode").value,
        visible_apps: $("#visibleApps").value.split(/\s+/).filter(Boolean)
      })
    });
    $("#deviceDialog").close();
    await loadDevices();
    toast("Configuration queued for sync");
  } catch (error) {
    $("#deviceError").textContent = error.message;
  }
});

$("#signOut").addEventListener("click", () => {
  localStorage.removeItem("glzhub_session");
  state.session = null;
  showApp();
});
$("#pairButton").addEventListener("click", () => showView("pair"));
$$("[data-open-pair]").forEach((button) => button.addEventListener("click", () => showView("pair")));
$$("[data-view-button]").forEach((button) => button.addEventListener("click", () => showView(button.dataset.viewButton)));
$$(".nav").forEach((button) => button.addEventListener("click", () => showView(button.dataset.view)));
$("[data-close-dialog]").addEventListener("click", () => $("#deviceDialog").close());

await loadPublicConfig();
restoreSession();
showApp();
if (state.session) loadDevices().catch(() => {
  localStorage.removeItem("glzhub_session");
  state.session = null;
  showApp();
});
