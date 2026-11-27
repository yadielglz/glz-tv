import { lineupChannels, projectGuide } from "./roku-guide";

interface Env {
  ASSETS: Fetcher;
  EPG_BUCKET: R2Bucket;
  SUPABASE_URL: string;
  SUPABASE_PUBLISHABLE_KEY: string;
  SUPABASE_SECRET_KEY: string;
  TMDB_API_TOKEN: string;
}

type JsonValue = Record<string, unknown> | unknown[];

const JSON_HEADERS = { "content-type": "application/json; charset=utf-8" };
const CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
const DEFAULT_VISIBLE_APPS = [
  "com.google.android.youtube.tv",
  "com.netflix.ninja",
  "com.bamnetworks.mobile.android.gameday.atbat",
  "com.android.mgsandroid",
  "com.glztech.radiostream",
  "com.live.geesports",
  "com.cbs.ott",
  "com.disney.disneyplus",
  "com.peacocktv.peacockandroid",
  "com.TWCableTV"
];

function json(value: JsonValue, status = 200, extraHeaders: HeadersInit = {}): Response {
  return new Response(JSON.stringify(value), {
    status,
    headers: { ...JSON_HEADERS, "cache-control": "no-store", ...extraHeaders }
  });
}

function randomText(length: number, alphabet: string): string {
  const bytes = crypto.getRandomValues(new Uint8Array(length));
  return Array.from(bytes, (byte) => alphabet[byte % alphabet.length]).join("");
}

async function sha256(value: string): Promise<string> {
  const bytes = new TextEncoder().encode(value);
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

export interface ExportM3uTokenPayload {
  v: 1;
  ownerId: string;
  playlistId: string | null;
  includeEvents: boolean;
}

function toBase64Url(bytes: Uint8Array): string {
  let binary = "";
  for (let i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i]);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

function fromBase64Url(value: string): Uint8Array | null {
  try {
    const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
    const padded = normalized + "=".repeat((4 - (normalized.length % 4)) % 4);
    const binary = atob(padded);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
    return bytes;
  } catch {
    return null;
  }
}

async function importExportSigningKey(env: Env): Promise<CryptoKey> {
  const secret = `glzhub-m3u-export-v1:${env.SUPABASE_SECRET_KEY || ""}`;
  return crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign", "verify"]
  );
}

export async function signExportM3uToken(env: Env, payload: ExportM3uTokenPayload): Promise<string> {
  const encodedPayload = toBase64Url(new TextEncoder().encode(JSON.stringify(payload)));
  const key = await importExportSigningKey(env);
  const sig = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(encodedPayload));
  return `${encodedPayload}.${toBase64Url(new Uint8Array(sig))}`;
}

export async function verifyExportM3uToken(env: Env, token: string): Promise<ExportM3uTokenPayload | null> {
  const dot = token.indexOf(".");
  if (dot <= 0 || dot >= token.length - 1) return null;
  const encodedPayload = token.slice(0, dot);
  const encodedSig = token.slice(dot + 1);
  const sigBytes = fromBase64Url(encodedSig);
  const payloadBytes = fromBase64Url(encodedPayload);
  if (!sigBytes || !payloadBytes) return null;
  const key = await importExportSigningKey(env);
  const valid = await crypto.subtle.verify("HMAC", key, sigBytes, new TextEncoder().encode(encodedPayload));
  if (!valid) return null;
  try {
    const parsed = JSON.parse(new TextDecoder().decode(payloadBytes)) as Record<string, unknown>;
    if (parsed.v !== 1 || typeof parsed.ownerId !== "string" || !parsed.ownerId.trim()) return null;
    const playlistId = typeof parsed.playlistId === "string" && parsed.playlistId.trim()
      ? parsed.playlistId.trim()
      : null;
    return {
      v: 1,
      ownerId: parsed.ownerId.trim(),
      playlistId,
      includeEvents: parsed.includeEvents !== false
    };
  } catch {
    return null;
  }
}

function clientAddress(request: Request): string {
  return request.headers.get("cf-connecting-ip")
    ?? request.headers.get("x-forwarded-for")?.split(",")[0]?.trim()
    ?? "local";
}

async function body(request: Request): Promise<Record<string, unknown>> {
  const contentType = request.headers.get("content-type") ?? "";
  if (!contentType.includes("application/json")) throw new Error("Expected application/json");
  return request.json<Record<string, unknown>>();
}

function requiredString(value: unknown, name: string, max = 2048): string {
  if (typeof value !== "string" || !value.trim() || value.length > max) {
    throw new Error(`Invalid ${name}`);
  }
  return value.trim();
}

function optionalString(value: unknown, name: string, max = 2048): string | null {
  if (value === null || value === undefined || value === "") return null;
  return requiredString(value, name, max);
}

function isHttpsUrl(value: string | null): boolean {
  if (!value) return false;
  try { return new URL(value).protocol === "https:"; } catch { return false; }
}

const MAX_XMLTV_BYTES = 40 * 1024 * 1024;

function managedGuideObjectKey(playlistId: string): string {
  return `guides/${playlistId}.xml`;
}

function validateXmlTv(value: unknown): { xml: string; channelCount: number; programmeCount: number } {
  if (typeof value !== "string" || value.length < 20) throw new Error("Invalid XMLTV guide");
  // Measure UTF-8 bytes without allocating another guide-sized Uint8Array.
  const size = new Blob([value]).size;
  if (size > MAX_XMLTV_BYTES) {
    throw new Error(`XMLTV guide is ${(size / 1024 / 1024).toFixed(1)} MB after decompression; maximum is 40 MB`);
  }
  const xml = value.replace(/^\uFEFF/, "").trim();
  if (!/<tv(?:\s|>)/i.test(xml) || !/<\/tv>/i.test(xml)) throw new Error("Invalid XMLTV guide");
  let channelCount = 0;
  let programmeCount = 0;
  for (const _match of xml.matchAll(/<channel(?:\s|>)/gi)) channelCount++;
  for (const _match of xml.matchAll(/<programme(?:\s|>)/gi)) programmeCount++;
  return { xml, channelCount, programmeCount };
}

async function xmlTextFromResponse(response: Response): Promise<string> {
  const bytes = new Uint8Array(await response.arrayBuffer());
  if (bytes[0] === 0x1f && bytes[1] === 0x8b) {
    const decompressed = new Blob([bytes]).stream().pipeThrough(new DecompressionStream("gzip"));
    return new Response(decompressed).text();
  }
  return new TextDecoder().decode(bytes);
}

function guestServices(value: unknown): Record<string, string | null>[] {
  if (!Array.isArray(value) || value.length > 12) throw new Error("Invalid services");
  return value.map((item, index) => {
    if (!item || typeof item !== "object" || Array.isArray(item)) {
      throw new Error(`Invalid service ${index + 1}`);
    }
    const service = item as Record<string, unknown>;
    const actionUrl = optionalString(service.actionUrl, `service ${index + 1} URL`);
    if (actionUrl && !isHttpsUrl(actionUrl)) throw new Error(`Invalid service ${index + 1} URL`);
    return {
      title: requiredString(service.title, `service ${index + 1} title`, 60),
      subtitle: optionalString(service.subtitle, `service ${index + 1} subtitle`, 120),
      actionUrl
    };
  });
}

async function supabase(
  env: Env,
  path: string,
  init: RequestInit = {},
  usePublishableKey = false
): Promise<Response> {
  const key = usePublishableKey ? env.SUPABASE_PUBLISHABLE_KEY : env.SUPABASE_SECRET_KEY;
  const headers = new Headers(init.headers);
  headers.set("apikey", key);
  if (!headers.has("authorization")) headers.set("authorization", `Bearer ${key}`);
  if (init.body && !headers.has("content-type")) headers.set("content-type", "application/json");
  return fetch(`${env.SUPABASE_URL}${path}`, { ...init, headers });
}

async function supabaseJson(
  env: Env,
  path: string,
  init: RequestInit = {}
): Promise<unknown> {
  const response = await supabase(env, path, init);
  if (!response.ok) {
    const detail = await response.text();
    throw new Error(`Database request failed (${response.status}): ${detail.slice(0, 300)}`);
  }
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

async function adminUser(request: Request, env: Env): Promise<{ id: string; email?: string }> {
  const authorization = request.headers.get("authorization");
  if (!authorization?.startsWith("Bearer ")) throw new Response("Unauthorized", { status: 401 });
  const response = await supabase(
    env,
    "/auth/v1/user",
    { headers: { authorization } },
    true
  );
  if (!response.ok) throw new Response("Unauthorized", { status: 401 });
  return response.json<{ id: string; email?: string }>();
}

async function deviceForToken(request: Request, env: Env): Promise<Record<string, unknown>> {
  const token = request.headers.get("authorization")?.replace(/^Bearer\s+/i, "");
  if (!token) throw new Response("Unauthorized", { status: 401 });
  const tokenHash = await sha256(token);
  const rows = await supabaseJson(
    env,
    `/rest/v1/devices?token_hash=eq.${tokenHash}&select=*`
  ) as Record<string, unknown>[];
  if (!rows[0]) throw new Response("Unauthorized", { status: 401 });
  return rows[0];
}

async function enroll(request: Request, env: Env): Promise<Response> {
  const input = await body(request);
  const installationId = requiredString(input.installationId, "installationId", 128);
  const platform = requiredString(input.platform, "platform", 32);
  const model = typeof input.model === "string" ? input.model.slice(0, 160) : "Unknown TV";
  const appVersion = typeof input.appVersion === "string" ? input.appVersion.slice(0, 40) : "unknown";
  const deviceToken = `glz_${randomText(48, CODE_ALPHABET.toLowerCase())}`;
  const pairingCode = `GLZ-${randomText(6, CODE_ALPHABET)}`;
  const tokenHash = await sha256(deviceToken);

  await supabaseJson(env, "/rest/v1/enrollments", {
    method: "POST",
    headers: { prefer: "resolution=merge-duplicates,return=minimal" },
    body: JSON.stringify({
      installation_id: installationId,
      pairing_code: pairingCode,
      token_hash: tokenHash,
      platform,
      model,
      app_version: appVersion,
      request_network_hash: await sha256(clientAddress(request)),
      expires_at: new Date(Date.now() + 60 * 60_000).toISOString()
    })
  });
  return json({
    pairingCode,
    deviceToken,
    expiresInSeconds: 3600,
    pairUrl: "https://glzhub.glztech.com/pair"
  }, 201);
}

async function listNearbyEnrollments(request: Request, env: Env): Promise<Response> {
  await adminUser(request, env);
  const networkHash = await sha256(clientAddress(request));
  const now = new Date().toISOString();
  const enrollments = await supabaseJson(
    env,
    `/rest/v1/enrollments?request_network_hash=eq.${networkHash}` +
      `&expires_at=gt.${encodeURIComponent(now)}` +
      "&select=pairing_code,platform,model,app_version,created_at,expires_at" +
      "&order=created_at.desc&limit=12"
  ) as Record<string, unknown>[];
  return json({ enrollments });
}

async function claimEnrollment(request: Request, env: Env): Promise<Response> {
  const user = await adminUser(request, env);
  const input = await body(request);
  const siteId = optionalString(input.siteId, "siteId", 64);
  if (siteId) {
    const sites = await supabaseJson(
      env,
      `/rest/v1/sites?id=eq.${encodeURIComponent(siteId)}&owner_id=eq.${user.id}&select=id`
    ) as Record<string, unknown>[];
    if (!sites[0]) return json({ error: "Property not found." }, 404);
  }
  const pairingCode = requiredString(input.pairingCode, "pairingCode", 16).toUpperCase();
  const rows = await supabaseJson(
    env,
    `/rest/v1/enrollments?pairing_code=eq.${encodeURIComponent(pairingCode)}&select=*`
  ) as Record<string, unknown>[];
  const enrollment = rows[0];
  if (!enrollment || new Date(String(enrollment.expires_at)).getTime() < Date.now()) {
    return json({ error: "Pairing code is invalid or expired." }, 404);
  }

  const created = await supabaseJson(env, "/rest/v1/devices?on_conflict=installation_id&select=*", {
    method: "POST",
    headers: { prefer: "resolution=merge-duplicates,return=representation" },
    body: JSON.stringify({
      owner_id: user.id,
      installation_id: enrollment.installation_id,
      token_hash: enrollment.token_hash,
      platform: enrollment.platform,
      model: enrollment.model,
      app_version: enrollment.app_version,
      site_id: siteId,
      name: input.name || "New TV",
      guest_name: input.guestName || "Guest",
      visible_apps: DEFAULT_VISIBLE_APPS,
      config_version: 1,
      last_seen_at: new Date().toISOString()
    })
  }) as Record<string, unknown>[];
  await supabaseJson(
    env,
    `/rest/v1/enrollments?pairing_code=eq.${encodeURIComponent(pairingCode)}`,
    { method: "DELETE" }
  );
  return json({ device: created[0] }, 201);
}

const VALID_UPDATE_CHANNELS = new Set(["production", "weekly", "biweekly", "monthly", "testing"]);

function normalizeUpdateChannel(value: unknown): string {
  const raw = String(value ?? "").trim().toLowerCase();
  if (raw === "family") return "weekly";
  if (raw === "rc") return "biweekly";
  if (raw === "beta") return "testing";
  return VALID_UPDATE_CHANNELS.has(raw) ? raw : "production";
}

function normalizeAppLanguage(value: unknown): string {
  const raw = String(value ?? "").trim().toLowerCase();
  return raw.startsWith("es") ? "es" : "en";
}

function enrichDeviceRecord(d: Record<string, unknown>): Record<string, unknown> {
  const rules = (d.schedule_rules && typeof d.schedule_rules === "object" && !Array.isArray(d.schedule_rules))
    ? d.schedule_rules as Record<string, unknown>
    : {};
  const updateChannel = normalizeUpdateChannel(d.update_channel ?? rules.update_channel ?? "production");
  const appLanguage = normalizeAppLanguage(d.app_language ?? rules.app_language ?? "en");
  return {
    ...d,
    update_channel: updateChannel,
    app_language: appLanguage
  };
}

async function listDevices(request: Request, env: Env): Promise<Response> {
  const user = await adminUser(request, env);
  const devices = await supabaseJson(
    env,
    `/rest/v1/devices?owner_id=eq.${user.id}&select=*&order=created_at.desc`
  ) as Record<string, unknown>[];
  const tenMinutesAgo = Date.now() - 10 * 60_000;
  const sanitized = devices.map((raw) => {
    const d = enrichDeviceRecord(raw);
    const isOnline = d.last_seen_at && new Date(String(d.last_seen_at)).getTime() > tenMinutesAgo;
    if (!isOnline && d.sync_status === "syncing") {
      return { ...d, sync_status: "queued", sync_message: "Interrupted (will resume on connect)", sync_progress: 0 };
    }
    return d;
  });
  return json({ devices: sanitized });
}

async function syncAllDevices(request: Request, env: Env): Promise<Response> {
  const user = await adminUser(request, env);
  const devices = await supabaseJson(
    env,
    `/rest/v1/devices?owner_id=eq.${user.id}&select=id,config_version,last_seen_at`
  ) as Record<string, unknown>[];
  if (!Array.isArray(devices) || devices.length === 0) {
    return json({ ok: true, syncedCount: 0, onlineCount: 0, totalDevices: 0 });
  }
  const token = crypto.randomUUID();
  const pushedAt = new Date().toISOString();
  const tenMinutesAgo = Date.now() - 10 * 60_000;
  let onlineCount = 0;
  await Promise.all(devices.map((device) => {
    const isOnline = device.last_seen_at && new Date(String(device.last_seen_at)).getTime() > tenMinutesAgo;
    if (isOnline) onlineCount++;
    return supabaseJson(env, `/rest/v1/devices?id=eq.${encodeURIComponent(String(device.id))}&owner_id=eq.${user.id}`, {
      method: "PATCH",
      body: JSON.stringify({
        config_version: Number(device.config_version || 0) + 1,
        force_refresh_token: token,
        sync_status: "queued",
        sync_progress: 0,
        sync_message: isOnline ? "Waiting for TV sync" : "Queued (TV offline)",
        sync_updated_at: pushedAt
      })
    });
  }));
  return json({ ok: true, syncedCount: devices.length, onlineCount, totalDevices: devices.length });
}

async function updateDevice(request: Request, env: Env, deviceId: string): Promise<Response> {
  const user = await adminUser(request, env);
  const input = await body(request);
  const currentRows = await supabaseJson(
    env,
    `/rest/v1/devices?id=eq.${encodeURIComponent(deviceId)}&owner_id=eq.${user.id}&select=config_version,last_seen_at,sync_status,schedule_rules`
  ) as Record<string, unknown>[];
  if (!currentRows[0]) return json({ error: "Device not found." }, 404);
  const allowed = [
    "name", "guest_name", "playlist_url", "epg_url", "request_headers",
    "visible_apps", "theme_mode", "weather_location", "start_destination",
    "captions_enabled", "captions_language", "auto_start", "resume_last_channel",
    "osd_timeout_seconds", "auto_update", "wifi_only", "keep_awake_home", "home_preview_channel_id",
    "custom_connection_label", "custom_isp_name", "sports_bar_kiosk_enabled",
    "update_channel", "app_language",
    "room_number", "arrival_date", "departure_date", "site_id", "assigned_playlist_id", "box_group_id"
  ];
  const patch = Object.fromEntries(Object.entries(input).filter(([key]) => allowed.includes(key)));
  const existingRules = (currentRows[0].schedule_rules && typeof currentRows[0].schedule_rules === "object" && !Array.isArray(currentRows[0].schedule_rules))
    ? { ...(currentRows[0].schedule_rules as Record<string, unknown>) }
    : {};
  if ("update_channel" in input) {
    const normalizedRing = normalizeUpdateChannel(input.update_channel);
    patch.update_channel = normalizedRing;
    existingRules.update_channel = normalizedRing;
    patch.schedule_rules = existingRules;
  }
  if ("app_language" in input) {
    const normalizedLang = normalizeAppLanguage(input.app_language);
    patch.app_language = normalizedLang;
    existingRules.app_language = normalizedLang;
    patch.schedule_rules = existingRules;
  }
  if ("site_id" in input) {
    const siteId = optionalString(input.site_id, "siteId", 64);
    if (siteId) {
      const sites = await supabaseJson(
        env,
        `/rest/v1/sites?id=eq.${encodeURIComponent(siteId)}&owner_id=eq.${user.id}&select=id`
      ) as Record<string, unknown>[];
      if (!sites[0]) return json({ error: "Property not found." }, 404);
    }
    patch.site_id = siteId;
  }
  if ("assigned_playlist_id" in input) {
    const playlistId = optionalString(input.assigned_playlist_id, "assignedPlaylistId", 64);
    if (playlistId) {
      const playlists = await supabaseJson(
        env,
        `/rest/v1/playlists?id=eq.${encodeURIComponent(playlistId)}&owner_id=eq.${user.id}&target_app=in.(tv,both)&select=id`
      ) as Record<string, unknown>[];
      if (!playlists[0]) return json({ error: "TV playlist not found." }, 404);
    }
    patch.assigned_playlist_id = playlistId;
  }
  if ("box_group_id" in input) {
    const groupId = optionalString(input.box_group_id, "boxGroupId", 64);
    if (groupId) {
      const groups = await supabaseJson(env,
        `/rest/v1/box_groups?id=eq.${encodeURIComponent(groupId)}&owner_id=eq.${user.id}&select=id`
      ) as Record<string, unknown>[];
      if (!groups[0]) return json({ error: "Box group not found." }, 404);
    }
    patch.box_group_id = groupId;
  }

  const tenMinutesAgo = Date.now() - 10 * 60_000;
  const isOnline = currentRows[0].last_seen_at && (new Date(String(currentRows[0].last_seen_at)).getTime() > tenMinutesAgo);
  const shouldSync = input.queue_sync !== false && input.sync !== false;

  if (shouldSync) {
    patch.config_version = Number(currentRows[0].config_version || 0) + 1;
    patch.sync_status = "queued";
    patch.sync_progress = 0;
    patch.sync_message = isOnline ? "Waiting for TV sync" : "Queued (TV offline, will sync on boot)";
  }

  patch.updated_at = new Date().toISOString();
  patch.sync_updated_at = patch.updated_at;
  let devices: Record<string, unknown>[];
  try {
    devices = await supabaseJson(
      env,
      `/rest/v1/devices?id=eq.${encodeURIComponent(deviceId)}&owner_id=eq.${user.id}&select=*`,
      {
        method: "PATCH",
        headers: { prefer: "return=representation" },
        body: JSON.stringify(patch)
      }
    ) as Record<string, unknown>[];
  } catch (err) {
    // Fallback if migration 028 (update_channel / app_language columns) hasn't been applied yet
    const fallbackPatch = { ...patch };
    delete fallbackPatch.update_channel;
    delete fallbackPatch.app_language;
    devices = await supabaseJson(
      env,
      `/rest/v1/devices?id=eq.${encodeURIComponent(deviceId)}&owner_id=eq.${user.id}&select=*`,
      {
        method: "PATCH",
        headers: { prefer: "return=representation" },
        body: JSON.stringify(fallbackPatch)
      }
    ) as Record<string, unknown>[];
  }
  if (!devices[0]) return json({ error: "Device not found." }, 404);
  return json({ device: enrichDeviceRecord(devices[0]), isOnline: Boolean(isOnline) });
}

async function forceRefreshDevice(request: Request, env: Env, deviceId: string): Promise<Response> {
  const user = await adminUser(request, env);
  const currentRows = await supabaseJson(
    env,
    `/rest/v1/devices?id=eq.${encodeURIComponent(deviceId)}&owner_id=eq.${user.id}&select=config_version,last_seen_at`
  ) as Record<string, unknown>[];
  if (!currentRows[0]) return json({ error: "Device not found." }, 404);

  const tenMinutesAgo = Date.now() - 10 * 60_000;
  const isOnline = currentRows[0].last_seen_at && (new Date(String(currentRows[0].last_seen_at)).getTime() > tenMinutesAgo);

  const token = new Date().toISOString();
  const patch = {
    force_refresh_token: token,
    config_version: Number(currentRows[0].config_version || 0) + 1,
    updated_at: token,
    sync_status: "queued",
    sync_progress: 0,
    sync_message: isOnline ? "Waiting for TV" : "Force refresh queued (TV offline)",
    sync_updated_at: token
  };
  const devices = await supabaseJson(
    env,
    `/rest/v1/devices?id=eq.${encodeURIComponent(deviceId)}&owner_id=eq.${user.id}&select=*`,
    {
      method: "PATCH",
      headers: { prefer: "return=representation" },
      body: JSON.stringify(patch)
    }
  ) as Record<string, unknown>[];
  await supabaseJson(env, "/rest/v1/device_commands?select=*", {
    method: "POST", headers: { prefer: "return=representation" },
    body: JSON.stringify({
      owner_id: user.id, device_id: deviceId, action: "force_refresh",
      payload: { timestamp: token }
    })
  });
  return json({ ok: true, device: enrichDeviceRecord(devices[0]), isOnline: Boolean(isOnline) });
}

async function checkUpdateDevice(request: Request, env: Env, deviceId: string): Promise<Response> {
  const user = await adminUser(request, env);
  const currentRows = await supabaseJson(
    env,
    `/rest/v1/devices?id=eq.${encodeURIComponent(deviceId)}&owner_id=eq.${user.id}&select=config_version,last_seen_at`
  ) as Record<string, unknown>[];
  if (!currentRows[0]) return json({ error: "Device not found." }, 404);

  const tenMinutesAgo = Date.now() - 10 * 60_000;
  const isOnline = currentRows[0].last_seen_at && (new Date(String(currentRows[0].last_seen_at)).getTime() > tenMinutesAgo);
  if (!isOnline) {
    return json({ error: "Cannot check for updates: Device is currently offline." }, 400);
  }

  const nowIso = new Date().toISOString();
  await supabaseJson(env, "/rest/v1/device_commands?select=*", {
    method: "POST", headers: { prefer: "return=representation" },
    body: JSON.stringify({
      owner_id: user.id, device_id: deviceId, action: "check_update",
      payload: { timestamp: nowIso }
    })
  }).catch(async () => {
    // Fallback if migration 028 check constraint hasn't been applied yet
    await supabaseJson(env, "/rest/v1/device_commands?select=*", {
      method: "POST", headers: { prefer: "return=representation" },
      body: JSON.stringify({
        owner_id: user.id, device_id: deviceId, action: "force_refresh",
        payload: { timestamp: nowIso, checkUpdate: true }
      })
    });
  });

  return json({ ok: true });
}

async function unpairDevice(request: Request, env: Env, deviceId: string): Promise<Response> {
  const user = await adminUser(request, env);
  const devices = await supabaseJson(
    env,
    `/rest/v1/devices?id=eq.${encodeURIComponent(deviceId)}&owner_id=eq.${user.id}&select=installation_id`
  ) as Record<string, unknown>[];
  if (!devices[0]) return json({ error: "Device not found." }, 404);
  await supabaseJson(
    env,
    `/rest/v1/enrollments?installation_id=eq.${encodeURIComponent(String(devices[0].installation_id))}`,
    { method: "DELETE" }
  );
  await supabaseJson(
    env,
    `/rest/v1/devices?id=eq.${encodeURIComponent(deviceId)}&owner_id=eq.${user.id}`,
    { method: "DELETE" }
  );
  return json({ ok: true });
}

// Shared by Android configuration and Roku guide projection to keep source selection identical.
async function deviceSources(request: Request, env: Env, device: Record<string, unknown>) {
  const origin = new URL(request.url).origin;
  const managedPlaylistUrl = `${origin}/api/v1/devices/playlist.m3u`;
  let managedEpgUrl: unknown = null;
  let groupPlaylistId: unknown = null;
  let playlistUpdatedAt = "";
  if (device.box_group_id) {
    const groups = await supabaseJson(env,
      `/rest/v1/box_groups?id=eq.${encodeURIComponent(String(device.box_group_id))}&owner_id=eq.${device.owner_id}&select=playlist_id`
    ) as Record<string, unknown>[];
    groupPlaylistId = groups[0]?.playlist_id ?? null;
  }
  const effectivePlaylistId = device.assigned_playlist_id || groupPlaylistId || null;
  const hasManagedAssignment = Boolean(effectivePlaylistId);
  const playlistUrl = hasManagedAssignment ? managedPlaylistUrl : (device.playlist_url || managedPlaylistUrl);
  if (hasManagedAssignment || !device.epg_url || playlistUrl === managedPlaylistUrl) {
    const playlistFilter = effectivePlaylistId
      ? `owner_id=eq.${device.owner_id}&id=eq.${encodeURIComponent(String(effectivePlaylistId))}`
      : `owner_id=eq.${device.owner_id}&target_app=in.(tv,both)&is_published=eq.true&order=created_at.asc`;
    const assigned = await supabaseJson(env, `/rest/v1/playlists?${playlistFilter}&select=id,epg_url,updated_at`) as Record<string, unknown>[];
    for (const pl of assigned) {
      if (typeof pl.epg_url === "string" && pl.epg_url.trim() && !managedEpgUrl) {
        managedEpgUrl = pl.epg_url.trim();
      }
      if (typeof pl.updated_at === "string" && pl.updated_at > playlistUpdatedAt) {
        playlistUpdatedAt = pl.updated_at;
      }
    }
  }
  return {
    playlistUrl,
    epgUrl: hasManagedAssignment ? (managedEpgUrl || device.epg_url || null) : (device.epg_url || managedEpgUrl || null),
    effectivePlaylistId: effectivePlaylistId ? String(effectivePlaylistId) : null,
    playlistUpdatedAt
  };
}

async function deviceConfig(request: Request, env: Env): Promise<Response> {
  const device = await deviceForToken(request, env);
  if (device.sync_status === "queued") {
    supabaseJson(env, `/rest/v1/devices?id=eq.${device.id}`, {
      method: "PATCH",
      headers: { prefer: "return=minimal" },
      body: JSON.stringify({
        sync_status: "syncing",
        sync_progress: 25,
        sync_message: "TV applying configuration",
        sync_updated_at: new Date().toISOString()
      })
    }).catch(() => null);
  }
  const [
    sources,
    siteProfiles,
    fallbackProfiles,
    movies,
    commands,
    radioRows,
    eventRows,
    epgRows
  ] = await Promise.all([
    deviceSources(request, env, device),
    device.site_id
      ? supabaseJson(env,
          `/rest/v1/guest_experience_profiles?site_id=eq.${device.site_id}&owner_id=eq.${device.owner_id}&select=*`
        ) as Promise<Record<string, unknown>[]>
      : Promise.resolve([] as Record<string, unknown>[]),
    // Fallback to the owner's most recently updated guest experience so branding, hero image
    // and welcome copy still reach the screen when the device has no site or site profile.
    supabaseJson(env,
      `/rest/v1/guest_experience_profiles?owner_id=eq.${device.owner_id}&select=*&order=updated_at.desc&limit=1`
    ) as Promise<Record<string, unknown>[]>,
    supabaseJson(env,
      `/rest/v1/movies?owner_id=eq.${device.owner_id}&is_active=eq.true&show_on_home=eq.true&select=id,title,stream_url,poster_url,backdrop_url,description,release_year,runtime_minutes,genres,request_headers&order=sort_order.asc,created_at.desc`
    ) as Promise<Record<string, unknown>[]>,
    supabaseJson(env,
      `/rest/v1/device_commands?device_id=eq.${device.id}&status=eq.pending&select=*&order=created_at.asc&limit=10`
    ).catch(() => []) as Promise<Record<string, unknown>[]>,
    supabaseJson(env,
      `/rest/v1/radio_stations?owner_id=eq.${device.owner_id}&is_active=eq.true&select=id,updated_at&order=id.asc`
    ).catch(() => []) as Promise<Record<string, unknown>[]>,
    supabaseJson(env,
      `/rest/v1/event_channels?status=neq.disabled&select=id,tvg_id,title,stream_url,backup_stream_url,channel_number,start_time,end_time,status,is_online&order=id.asc`
    ).catch(() => []) as Promise<Record<string, unknown>[]>,
    supabaseJson(env,
      `/rest/v1/epg_guides?owner_id=eq.${device.owner_id}&select=playlist_id,updated_at`
    ).catch(() => []) as Promise<Record<string, unknown>[]>
  ]);

  const { playlistUrl, epgUrl, effectivePlaylistId, playlistUpdatedAt } = sources;
  const profile = siteProfiles[0] ?? fallbackProfiles[0] ?? {};
  const pendingCommandList = Array.isArray(commands) ? commands : [];
  if (pendingCommandList.length) {
    const ids = pendingCommandList.map((command) => command.id).join(",");
    await supabaseJson(env, `/rest/v1/device_commands?id=in.(${ids})`, {
      method: "PATCH",
      headers: { prefer: "return=minimal" },
      body: JSON.stringify({ status: "delivered", delivered_at: new Date().toISOString() })
    }).catch(() => null);
  }

  const eventSignature = Array.isArray(eventRows)
    ? computeEventChannelsFingerprint(eventRows)
    : "";
  const matchingEpgRows = Array.isArray(epgRows)
    ? (effectivePlaylistId
        ? epgRows.filter((row) => String(row.playlist_id || "") === effectivePlaylistId)
        : epgRows)
    : [];
  let epgGuideUpdatedAt = "";
  for (const row of matchingEpgRows) {
    if (typeof row.updated_at === "string" && row.updated_at > epgGuideUpdatedAt) {
      epgGuideUpdatedAt = row.updated_at;
    }
  }
  const radioList = Array.isArray(radioRows) ? radioRows : [];

  const [playlistRevision, epgRevision, radioVersion] = await Promise.all([
    sha256([
      String(playlistUrl ?? ""),
      playlistUpdatedAt,
      String(device.assigned_playlist_id ?? ""),
      String(device.box_group_id ?? ""),
      String(device.channel_policy_mode ?? ""),
      String(device.force_refresh_token ?? ""),
      eventSignature
    ].join("::")),
    sha256([
      String(epgUrl ?? ""),
      epgGuideUpdatedAt,
      String(device.force_refresh_token ?? ""),
      eventSignature
    ].join("::")),
    radioList.length
      ? sha256(radioList.map((row) => `${row.id ?? ""}:${row.updated_at ?? ""}`).join("|"))
      : Promise.resolve("empty")
  ]);

  const enrichedDevice = enrichDeviceRecord(device);
  return json({
    version: device.config_version,
    experienceVersion: profile.updated_at ?? "default",
    playlistRevision,
    epgRevision,
    radioVersion,
    deviceName: device.name,
    guestName: device.guest_name,
    playlistUrl,
    epgUrl,
    requestHeaders: device.request_headers ?? {},
    visibleApps: device.visible_apps ?? [],
    themeMode: device.theme_mode,
    weatherLocation: device.weather_location,
    customConnectionLabel: device.custom_connection_label ?? null,
    customIspName: device.custom_isp_name ?? null,
    startDestination: device.start_destination,
    captionsEnabled: device.captions_enabled,
    captionsLanguage: device.captions_language,
    appLanguage: enrichedDevice.app_language ?? "en",
    autoStart: device.auto_start,
    resumeLastChannel: device.resume_last_channel,
    keepAwakeHome: device.keep_awake_home ?? false,
    homePreviewChannelId: device.home_preview_channel_id ?? null,
    sportsBarKioskEnabled: device.sports_bar_kiosk_enabled ?? false,
    osdTimeoutSeconds: device.osd_timeout_seconds ?? 8,
    autoUpdate: device.auto_update ?? true,
    wifiOnly: device.wifi_only ?? false,
    updateChannel: enrichedDevice.update_channel ?? "production",
    forceRefreshToken: device.force_refresh_token ?? null,
    commands: pendingCommandList,
    movies: movies.map((movie) => ({
      id: movie.id,
      title: movie.title,
      streamUrl: movie.stream_url,
      posterUrl: movie.poster_url ?? null,
      backdropUrl: movie.backdrop_url ?? null,
      description: movie.description ?? "",
      releaseYear: movie.release_year ?? null,
      runtimeMinutes: movie.runtime_minutes ?? null,
      genres: movie.genres ?? [],
      requestHeaders: movie.request_headers ?? {}
    })),
    guestExperience: {
      propertyName: profile.property_name ?? "",
      welcomeMessage: profile.welcome_message ?? "",
      logoUrl: profile.logo_url ?? null,
      heroImageUrl: profile.hero_image_url ?? null,
      wifiName: profile.wifi_name ?? null,
      wifiInstructions: profile.wifi_instructions ?? null,
      checkoutTime: profile.checkout_time ?? null,
      frontDesk: profile.front_desk ?? null,
      noticeTitle: profile.notice_title ?? null,
      noticeBody: profile.notice_body ?? null,
      services: profile.services ?? [],
      roomNumber: device.room_number ?? null,
      arrivalDate: device.arrival_date ?? null,
      departureDate: device.departure_date ?? null
    }
  });
}

async function listBoxGroups(request: Request, env: Env): Promise<Response> {
  const user = await adminUser(request, env);
  const groups = await supabaseJson(env,
    `/rest/v1/box_groups?owner_id=eq.${user.id}&select=*,devices(id,name)&order=name.asc`
  ) as Record<string, unknown>[];
  return json({ groups });
}

async function createBoxGroup(request: Request, env: Env): Promise<Response> {
  const user = await adminUser(request, env); const input = await body(request);
  const playlistId = optionalString(input.playlistId || input.playlist_id, "playlistId", 64);
  if (playlistId) {
    const playlists = await supabaseJson(env, `/rest/v1/playlists?id=eq.${encodeURIComponent(playlistId)}&owner_id=eq.${user.id}&select=id`) as Record<string, unknown>[];
    if (!playlists[0]) return json({ error: "Playlist not found." }, 404);
  }
  const defaultPolicy = input.defaultPolicy === 'block' ? 'block' : 'allow';
  const rows = await supabaseJson(env, "/rest/v1/box_groups?select=*", { method: "POST", headers: { prefer: "return=representation" }, body: JSON.stringify({ owner_id: user.id, name: requiredString(input.name, "group name", 100), playlist_id: playlistId, default_channel_policy: defaultPolicy }) }) as Record<string, unknown>[];
  return json({ group: rows[0] }, 201);
}

async function updateBoxGroup(request: Request, env: Env, groupId: string): Promise<Response> {
  const user = await adminUser(request, env); const input = await body(request); const patch: Record<string, unknown> = { updated_at: new Date().toISOString() };
  if (input.name !== undefined) patch.name = requiredString(input.name, "group name", 100);
  if (input.defaultPolicy !== undefined) patch.default_channel_policy = input.defaultPolicy === 'block' ? 'block' : 'allow';
  if (input.playlistId !== undefined || input.playlist_id !== undefined) {
    const playlistId = optionalString(input.playlistId || input.playlist_id, "playlistId", 64);
    if (playlistId) {
      const playlists = await supabaseJson(env, `/rest/v1/playlists?id=eq.${encodeURIComponent(playlistId)}&owner_id=eq.${user.id}&select=id`) as Record<string, unknown>[];
      if (!playlists[0]) return json({ error: "Playlist not found." }, 404);
    }
    patch.playlist_id = playlistId;
  }
  const rows = await supabaseJson(env, `/rest/v1/box_groups?id=eq.${encodeURIComponent(groupId)}&owner_id=eq.${user.id}&select=*`, { method: "PATCH", headers: { prefer: "return=representation" }, body: JSON.stringify(patch) }) as Record<string, unknown>[];
  if (!rows[0]) return json({ error: "Box group not found." }, 404);
  return json({ group: rows[0] });
}

async function deleteBoxGroup(request: Request, env: Env, groupId: string): Promise<Response> {
  const user = await adminUser(request, env);
  await supabaseJson(env, `/rest/v1/box_groups?id=eq.${encodeURIComponent(groupId)}&owner_id=eq.${user.id}`, { method: "DELETE" });
  return json({ ok: true });
}

async function channelPolicy(request: Request, env: Env, targetType: string, targetId: string): Promise<Response> {
  const user = await adminUser(request, env);
  if (!['group', 'device'].includes(targetType)) throw new Error("Invalid policy target");
  const targetTable = targetType === 'group' ? 'box_groups' : 'devices';
  const targets = await supabaseJson(env, `/rest/v1/${targetTable}?id=eq.${encodeURIComponent(targetId)}&owner_id=eq.${user.id}&select=id`) as Record<string, unknown>[];
  if (!targets[0]) return json({ error: "Policy target not found." }, 404);
  if (request.method === "GET") {
    const rules = await supabaseJson(env, `/rest/v1/channel_policy_rules?target_type=eq.${targetType}&target_id=eq.${encodeURIComponent(targetId)}&owner_id=eq.${user.id}&select=playlist_item_id,decision,playlist_id`) as Record<string, unknown>[];
    const settings = await supabaseJson(env, `/rest/v1/${targetTable}?id=eq.${encodeURIComponent(targetId)}&owner_id=eq.${user.id}&select=${targetType === 'group' ? 'default_channel_policy' : 'channel_policy_mode'}`) as Record<string, unknown>[];
    return json({ rules, defaultPolicy: targetType === 'group' ? settings[0]?.default_channel_policy : settings[0]?.channel_policy_mode });
  }
  const input = await body(request); const playlistId = requiredString(input.playlistId || input.playlist_id, "playlistId", 64);
  const playlists = await supabaseJson(env, `/rest/v1/playlists?id=eq.${encodeURIComponent(playlistId)}&owner_id=eq.${user.id}&select=id,playlist_items(id)`) as Record<string, unknown>[];
  if (!playlists[0]) return json({ error: "Playlist not found." }, 404);
  const validIds = new Set(((playlists[0].playlist_items as Record<string, unknown>[]) || []).map((item) => String(item.id)));
  if (!Array.isArray(input.rules) || input.rules.length > 3000) throw new Error("Invalid channel policy");
  const rules = input.rules.map((value, index) => {
    if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error(`Invalid policy rule ${index + 1}`);
    const rule = value as Record<string, unknown>; const itemId = requiredString(rule.playlistItemId || rule.playlist_item_id, `policy channel ${index + 1}`, 64);
    const decision = requiredString(rule.decision, `policy decision ${index + 1}`, 10);
    if (!validIds.has(itemId) || !['allow', 'block'].includes(decision)) throw new Error(`Invalid policy rule ${index + 1}`);
    return { owner_id: user.id, playlist_id: playlistId, target_type: targetType, target_id: targetId, playlist_item_id: itemId, decision };
  });
  await supabaseJson(env, `/rest/v1/channel_policy_rules?target_type=eq.${targetType}&target_id=eq.${encodeURIComponent(targetId)}`, { method: "DELETE" });
  if (rules.length) await supabaseJson(env, "/rest/v1/channel_policy_rules", { method: "POST", body: JSON.stringify(rules) });
  const defaultPolicy = targetType === 'group'
    ? (input.defaultPolicy === 'block' ? 'block' : 'allow')
    : (['inherit', 'allow', 'block'].includes(String(input.defaultPolicy)) ? String(input.defaultPolicy) : 'inherit');
  await supabaseJson(env, `/rest/v1/${targetTable}?id=eq.${encodeURIComponent(targetId)}&owner_id=eq.${user.id}`, {
    method: 'PATCH', body: JSON.stringify(targetType === 'group' ? { default_channel_policy: defaultPolicy } : { channel_policy_mode: defaultPolicy })
  });
  const shouldSync = input.queue_sync !== false && input.sync !== false;
  let pushedDevicesCount = 0;
  if (shouldSync) {
    const tenMinutesAgo = Date.now() - 10 * 60_000;
    const refreshFilter = targetType === 'device' ? `id=eq.${encodeURIComponent(targetId)}` : `box_group_id=eq.${encodeURIComponent(targetId)}`;
    const affectedDevices = await supabaseJson(env, `/rest/v1/devices?owner_id=eq.${user.id}&${refreshFilter}&select=id,config_version,last_seen_at`) as Record<string, unknown>[];
    pushedDevicesCount = affectedDevices.length;
    const token = crypto.randomUUID();
    const tokenTime = new Date().toISOString();
    await Promise.all(affectedDevices.map((device) => {
      const isOnline = device.last_seen_at && new Date(String(device.last_seen_at)).getTime() > tenMinutesAgo;
      return supabaseJson(env, `/rest/v1/devices?id=eq.${device.id}&owner_id=eq.${user.id}`, {
        method: "PATCH", body: JSON.stringify({
          config_version: Number(device.config_version || 0) + 1,
          force_refresh_token: token,
          sync_status: "queued",
          sync_progress: 0,
          sync_message: isOnline ? "Updating channel policy" : "Queued (TV offline)",
          sync_updated_at: tokenTime
        })
      });
    }));
  }
  return json({ ok: true, rules: rules.length, pushedDevices: pushedDevicesCount });
}

async function getGuestExperience(request: Request, env: Env): Promise<Response> {
  const user = await adminUser(request, env);
  const siteId = optionalString(new URL(request.url).searchParams.get("siteId"), "siteId", 64);
  if (!siteId) return json({ profile: null });
  const profiles = await supabaseJson(env,
    `/rest/v1/guest_experience_profiles?owner_id=eq.${user.id}&site_id=eq.${encodeURIComponent(siteId)}&select=*`
  ) as Record<string, unknown>[];
  return json({ profile: profiles[0] ?? null });
}

async function updateGuestExperience(request: Request, env: Env): Promise<Response> {
  const user = await adminUser(request, env);
  const input = await body(request);
  const siteId = requiredString(input.site_id, "siteId", 64);
  const sites = await supabaseJson(
    env,
    `/rest/v1/sites?id=eq.${encodeURIComponent(siteId)}&owner_id=eq.${user.id}&select=id`
  ) as Record<string, unknown>[];
  if (!sites[0]) return json({ error: "Property not found." }, 404);
  const profile = {
    owner_id: user.id,
    site_id: siteId,
    property_name: requiredString(input.property_name, "property name", 100),
    welcome_message: requiredString(input.welcome_message, "welcome message", 180),
    logo_url: optionalString(input.logo_url, "logo URL"),
    hero_image_url: optionalString(input.hero_image_url, "hero image URL"),
    wifi_name: optionalString(input.wifi_name, "Wi-Fi name", 100),
    wifi_instructions: optionalString(input.wifi_instructions, "Wi-Fi instructions", 300),
    checkout_time: optionalString(input.checkout_time, "checkout time", 40),
    front_desk: optionalString(input.front_desk, "front desk", 100),
    notice_title: optionalString(input.notice_title, "notice title", 100),
    notice_body: optionalString(input.notice_body, "notice body", 500),
    services: guestServices(input.services ?? []),
    updated_at: new Date().toISOString()
  };
  for (const key of ["logo_url", "hero_image_url"] as const) {
    if (profile[key] && !isHttpsUrl(profile[key])) throw new Error(`Invalid ${key.replace("_", " ")}`);
  }
  const profiles = await supabaseJson(env,
    "/rest/v1/guest_experience_profiles?on_conflict=site_id&select=*",
    {
      method: "POST",
      headers: { prefer: "resolution=merge-duplicates,return=representation" },
      body: JSON.stringify(profile)
    }
  ) as Record<string, unknown>[];

  const shouldSync = input.queue_sync !== false && input.sync !== false;
  let pushedCount = 0;
  if (shouldSync) {
    const tenMinutesAgo = Date.now() - 10 * 60_000;
    const siteDevices = await supabaseJson(env,
      `/rest/v1/devices?owner_id=eq.${user.id}&site_id=eq.${encodeURIComponent(siteId)}&select=id,config_version,last_seen_at`
    ) as Record<string, unknown>[];
    pushedCount = siteDevices.length;
    const token = new Date().toISOString();
    await Promise.all(siteDevices.map((d) => {
      const isOnline = d.last_seen_at && new Date(String(d.last_seen_at)).getTime() > tenMinutesAgo;
      return supabaseJson(env,
        `/rest/v1/devices?id=eq.${encodeURIComponent(String(d.id))}&owner_id=eq.${user.id}`,
        { method: "PATCH", body: JSON.stringify({
          config_version: Number(d.config_version || 0) + 1,
          force_refresh_token: token,
          sync_status: "queued",
          sync_progress: 0,
          sync_message: isOnline ? "Updating guest experience" : "Queued (TV offline)",
          sync_updated_at: token
        }) }
      );
    }));
  }

  return json({ profile: profiles[0], pushedDevices: pushedCount });
}

async function listSites(request: Request, env: Env): Promise<Response> {
  const user = await adminUser(request, env);
  const sites = await supabaseJson(
    env,
    `/rest/v1/sites?owner_id=eq.${user.id}&select=*&order=name.asc`
  ) as Record<string, unknown>[];
  return json({ sites });
}

async function createSite(request: Request, env: Env): Promise<Response> {
  const user = await adminUser(request, env);
  const input = await body(request);
  const sites = await supabaseJson(env, "/rest/v1/sites?select=*", {
    method: "POST",
    headers: { prefer: "return=representation" },
    body: JSON.stringify({
      owner_id: user.id,
      name: requiredString(input.name, "property name", 100),
      address: optionalString(input.address, "address", 240)
    })
  }) as Record<string, unknown>[];
  const site = sites[0];
  if (!site) throw new Error("Database did not return the new property.");
  await supabaseJson(env, "/rest/v1/guest_experience_profiles?select=*", {
    method: "POST",
    headers: { prefer: "return=minimal" },
    body: JSON.stringify({
      owner_id: user.id,
      site_id: site.id,
      property_name: site.name,
      welcome_message: "Relax, explore, and enjoy your stay."
    })
  });
  return json({ site }, 201);
}

async function updateSite(request: Request, env: Env, siteId: string): Promise<Response> {
  const user = await adminUser(request, env);
  const input = await body(request);
  const patch: Record<string, unknown> = { updated_at: new Date().toISOString() };
  if ("name" in input) patch.name = requiredString(input.name, "property name", 100);
  if ("address" in input) patch.address = optionalString(input.address, "address", 240);
  const sites = await supabaseJson(
    env,
    `/rest/v1/sites?id=eq.${encodeURIComponent(siteId)}&owner_id=eq.${user.id}&select=*`,
    {
      method: "PATCH",
      headers: { prefer: "return=representation" },
      body: JSON.stringify(patch)
    }
  ) as Record<string, unknown>[];
  if (!sites[0]) return json({ error: "Property not found." }, 404);
  return json({ site: sites[0] });
}

async function deleteSite(request: Request, env: Env, siteId: string): Promise<Response> {
  const user = await adminUser(request, env);
  const existing = await supabaseJson(
    env,
    `/rest/v1/sites?id=eq.${encodeURIComponent(siteId)}&owner_id=eq.${user.id}&select=id`
  ) as Record<string, unknown>[];
  if (!existing[0]) return json({ error: "Property not found." }, 404);
  await supabaseJson(
    env,
    `/rest/v1/sites?id=eq.${encodeURIComponent(siteId)}&owner_id=eq.${user.id}`,
    { method: "DELETE" }
  );
  return json({ ok: true });
}

async function listApps(request: Request, env: Env): Promise<Response> {
  const user = await adminUser(request, env);
  const apps = await supabaseJson(env,
    `/rest/v1/app_catalog?owner_id=eq.${user.id}&select=*&order=name.asc`
  ) as Record<string, unknown>[];
  return json({ apps });
}

function appInput(input: Record<string, unknown>): Record<string, unknown> {
  const sourceType = requiredString(input.source_type, "source type", 20);
  if (!["play_store", "repository"].includes(sourceType)) throw new Error("Invalid source type");
  const sourceUrl = optionalString(input.source_url, "source URL");
  if (sourceType === "repository" && !isHttpsUrl(sourceUrl)) {
    throw new Error("Invalid source URL: repository apps require HTTPS");
  }
  const checksum = optionalString(input.sha256, "SHA-256", 64);
  if (checksum && !/^[a-f0-9]{64}$/i.test(checksum)) throw new Error("Invalid SHA-256");
  return {
    name: requiredString(input.name, "name", 100),
    package_name: requiredString(input.package_name, "package name", 180),
    source_type: sourceType,
    source_url: sourceUrl,
    version_name: optionalString(input.version_name, "version", 40),
    sha256: checksum?.toLowerCase() ?? null,
    updated_at: new Date().toISOString()
  };
}

async function createApp(request: Request, env: Env): Promise<Response> {
  const user = await adminUser(request, env);
  const input = await body(request);
  const apps = await supabaseJson(env, "/rest/v1/app_catalog?select=*", {
    method: "POST", headers: { prefer: "return=representation" },
    body: JSON.stringify({
      owner_id: user.id,
      ...appInput(input)
    })
  }) as Record<string, unknown>[];
  return json({ app: apps[0] }, 201);
}

async function updateApp(request: Request, env: Env, appId: string): Promise<Response> {
  const user = await adminUser(request, env);
  const apps = await supabaseJson(
    env,
    `/rest/v1/app_catalog?id=eq.${encodeURIComponent(appId)}&owner_id=eq.${user.id}&select=*`,
    {
      method: "PATCH",
      headers: { prefer: "return=representation" },
      body: JSON.stringify(appInput(await body(request)))
    }
  ) as Record<string, unknown>[];
  if (!apps[0]) return json({ error: "App not found." }, 404);
  return json({ app: apps[0] });
}

async function deleteApp(request: Request, env: Env, appId: string): Promise<Response> {
  const user = await adminUser(request, env);
  const apps = await supabaseJson(
    env,
    `/rest/v1/app_catalog?id=eq.${encodeURIComponent(appId)}&owner_id=eq.${user.id}&select=id`,
    { method: "DELETE", headers: { prefer: "return=representation" } }
  ) as Record<string, unknown>[];
  if (!apps[0]) return json({ error: "App not found." }, 404);
  return json({ ok: true });
}

async function queueInstall(request: Request, env: Env, deviceId: string): Promise<Response> {
  const user = await adminUser(request, env);
  const input = await body(request);
  const appId = requiredString(input.appId, "appId", 64);
  const [devices, apps] = await Promise.all([
    supabaseJson(env, `/rest/v1/devices?id=eq.${encodeURIComponent(deviceId)}&owner_id=eq.${user.id}&select=id`),
    supabaseJson(env, `/rest/v1/app_catalog?id=eq.${encodeURIComponent(appId)}&owner_id=eq.${user.id}&select=*`)
  ]) as [Record<string, unknown>[], Record<string, unknown>[]];
  if (!devices[0] || !apps[0]) return json({ error: "Device or app not found." }, 404);
  const app = apps[0];
  const commands = await supabaseJson(env, "/rest/v1/device_commands?select=*", {
    method: "POST", headers: { prefer: "return=representation" },
    body: JSON.stringify({
      owner_id: user.id, device_id: deviceId, action: "install_app",
      payload: {
        name: app.name, packageName: app.package_name, sourceType: app.source_type,
        sourceUrl: app.source_url, versionName: app.version_name, sha256: app.sha256
      }
    })
  }) as Record<string, unknown>[];
  return json({ command: commands[0] }, 201);
}

async function pendingCommands(request: Request, env: Env): Promise<Response> {
  const device = await deviceForToken(request, env);
  const commands = await supabaseJson(env,
    `/rest/v1/device_commands?device_id=eq.${device.id}&status=eq.pending&select=*&order=created_at.asc&limit=10`
  ) as Record<string, unknown>[];
  if (commands.length) {
    const ids = commands.map((command) => command.id).join(",");
    await supabaseJson(env, `/rest/v1/device_commands?id=in.(${ids})`, {
      method: "PATCH", headers: { prefer: "return=minimal" },
      body: JSON.stringify({ status: "delivered", delivered_at: new Date().toISOString() })
    });
  }
  return json({ commands });
}

async function commandResult(request: Request, env: Env, commandId: string): Promise<Response> {
  const device = await deviceForToken(request, env);
  const input = await body(request);
  const status = input.status === "completed" ? "completed" : "failed";
  await supabaseJson(env,
    `/rest/v1/device_commands?id=eq.${encodeURIComponent(commandId)}&device_id=eq.${device.id}`,
    { method: "PATCH", headers: { prefer: "return=minimal" }, body: JSON.stringify({
      status, completed_at: new Date().toISOString(),
      result_message: typeof input.message === "string" ? input.message.slice(0, 500) : null
    }) }
  );
  return json({ ok: true });
}

async function heartbeat(request: Request, env: Env): Promise<Response> {
  const device = await deviceForToken(request, env);
  const input = await body(request);
  const activity = input.activity && typeof input.activity === "object" && !Array.isArray(input.activity)
    ? input.activity as Record<string, unknown>
    : null;
  const activityType = activity
    ? requiredString(activity.type, "activity type", 20)
    : null;
  if (activityType && !["idle", "channel", "radio", "sports_bar", "app", "movie"].includes(activityType)) {
    throw new Error("Invalid activity type");
  }
  const patch: Record<string, unknown> = {
    last_seen_at: new Date().toISOString(),
    app_version: typeof input.appVersion === "string" ? input.appVersion.slice(0, 40) : device.app_version,
    last_error: typeof input.lastError === "string" ? input.lastError.slice(0, 500) : null
  };
  const sync = input.sync && typeof input.sync === "object" && !Array.isArray(input.sync)
    ? input.sync as Record<string, unknown>
    : null;
  if (sync) {
    const syncStatus = requiredString(sync.status, "sync status", 20);
    if (!["syncing", "complete", "failed"].includes(syncStatus)) {
      throw new Error("Invalid sync status");
    }
    const syncProgress = Number(sync.progress);
    if (!Number.isInteger(syncProgress) || syncProgress < 0 || syncProgress > 100) {
      throw new Error("Invalid sync progress");
    }
    patch.sync_status = syncStatus;
    patch.sync_progress = syncProgress;
    patch.sync_message = optionalString(sync.message, "sync message", 160);
    patch.sync_updated_at = new Date().toISOString();
  }
  if (activityType) {
    patch.activity_type = activityType;
    patch.activity_label = optionalString(activity?.label, "activity label", 160);
    patch.activity_package = optionalString(activity?.packageName, "activity package", 180);
    patch.activity_updated_at = new Date().toISOString();
  }
  const [, pendingRows] = await Promise.all([
    supabaseJson(env, `/rest/v1/devices?id=eq.${device.id}`, {
      method: "PATCH",
      headers: { prefer: "return=minimal" },
      body: JSON.stringify(patch)
    }),
    supabaseJson(
      env,
      `/rest/v1/device_commands?device_id=eq.${device.id}&status=eq.pending&select=id&limit=10`
    ).catch(() => []) as Promise<Record<string, unknown>[]>
  ]);
  const pendingCommandsCount = Array.isArray(pendingRows) ? pendingRows.length : 0;
  return json({ ok: true, configVersion: device.config_version, pendingCommandsCount });
}

async function listRadioStations(request: Request, env: Env): Promise<Response> {
  const user = await adminUser(request, env);
  const stations = await supabaseJson(
    env,
    `/rest/v1/radio_stations?owner_id=eq.${user.id}&select=*&order=created_at.desc`
  ) as Record<string, unknown>[];
  return json({ stations });
}

async function createRadioStation(request: Request, env: Env): Promise<Response> {
  const user = await adminUser(request, env);
  const input = await body(request);
  const stationCode = requiredString(input.station_code || input.stationCode, "station code", 32).toUpperCase();
  const name = requiredString(input.name, "station name", 100);
  const streamUrl = requiredString(input.stream_url || input.streamUrl, "stream URL", 2048);
  if (!isHttpsUrl(streamUrl)) throw new Error("Invalid stream URL");

  const stations = await supabaseJson(env, "/rest/v1/radio_stations?select=*", {
    method: "POST",
    headers: { prefer: "return=representation" },
    body: JSON.stringify({
      owner_id: user.id,
      station_code: stationCode,
      name,
      genre: optionalString(input.genre, "genre", 50) ?? "Variety",
      stream_url: streamUrl,
      epg_channel_id: optionalString(input.epg_channel_id || input.epgChannelId, "EPG channel ID", 64),
      logo_url: optionalString(input.logo_url || input.logoUrl, "logo URL"),
      bitrate: typeof input.bitrate === "number" ? input.bitrate : 128,
      is_active: input.is_active ?? true
    })
  }) as Record<string, unknown>[];
  await pushConfigUpdateToAllDevices(env, { syncMessage: "Syncing radio catalog", bumpForceRefresh: false });
  return json({ station: stations[0] }, 201);
}

async function updateRadioStation(request: Request, env: Env, stationId: string): Promise<Response> {
  const user = await adminUser(request, env);
  const input = await body(request);
  const patch: Record<string, unknown> = { updated_at: new Date().toISOString() };
  if (input.name) patch.name = requiredString(input.name, "name", 100);
  if (input.station_code || input.stationCode) {
    patch.station_code = requiredString(input.station_code || input.stationCode, "station code", 32).toUpperCase();
  }
  if (input.genre) patch.genre = requiredString(input.genre, "genre", 50);
  if (input.stream_url || input.streamUrl) {
    const streamUrl = requiredString(input.stream_url || input.streamUrl, "stream URL");
    if (!isHttpsUrl(streamUrl)) throw new Error("Invalid stream URL");
    patch.stream_url = streamUrl;
  }
  if (input.logo_url || input.logoUrl !== undefined) patch.logo_url = optionalString(input.logo_url || input.logoUrl, "logo URL");
  if (input.epg_channel_id !== undefined || input.epgChannelId !== undefined) {
    patch.epg_channel_id = optionalString(input.epg_channel_id || input.epgChannelId, "EPG channel ID", 64);
  }
  if (typeof input.bitrate === "number" && input.bitrate >= 32 && input.bitrate <= 320) patch.bitrate = input.bitrate;
  if (typeof input.is_active === "boolean") patch.is_active = input.is_active;

  const stations = await supabaseJson(
    env,
    `/rest/v1/radio_stations?id=eq.${encodeURIComponent(stationId)}&owner_id=eq.${user.id}&select=*`,
    {
      method: "PATCH",
      headers: { prefer: "return=representation" },
      body: JSON.stringify(patch)
    }
  ) as Record<string, unknown>[];
  if (!stations[0]) return json({ error: "Radio station not found." }, 404);
  await pushConfigUpdateToAllDevices(env, { syncMessage: "Syncing radio catalog", bumpForceRefresh: false });
  return json({ station: stations[0] });
}

async function deleteRadioStation(request: Request, env: Env, stationId: string): Promise<Response> {
  const user = await adminUser(request, env);
  await supabaseJson(
    env,
    `/rest/v1/radio_stations?id=eq.${encodeURIComponent(stationId)}&owner_id=eq.${user.id}`,
    { method: "DELETE" }
  );
  await pushConfigUpdateToAllDevices(env, { syncMessage: "Syncing radio catalog", bumpForceRefresh: false });
  return json({ ok: true });
}

type PublicRadioStation = {
  code: string;
  name: string;
  genre: string;
  streamUrl: string;
  logoUrl: string | null;
  epgChannelId: string | null;
  bitrateKbps: number;
  requestHeaders: Record<string, string>;
};

function publicRadioStation(row: Record<string, unknown>): PublicRadioStation {
  const rawHeaders = row.request_headers;
  const requestHeaders = rawHeaders && typeof rawHeaders === "object" && !Array.isArray(rawHeaders)
    ? Object.fromEntries(Object.entries(rawHeaders).filter((entry): entry is [string, string] => typeof entry[1] === "string"))
    : {};
  return {
    code: String(row.station_code || ""),
    name: String(row.name || ""),
    genre: String(row.genre || "Variety"),
    streamUrl: String(row.stream_url || ""),
    logoUrl: typeof row.logo_url === "string" ? row.logo_url : null,
    epgChannelId: typeof row.epg_channel_id === "string" ? row.epg_channel_id : null,
    bitrateKbps: Number(row.bitrate || 128),
    requestHeaders
  };
}

async function radioCatalogResponse(
  request: Request,
  stations: PublicRadioStation[],
  cacheControl = "public, no-cache"
): Promise<Response> {
  const serializedStations = JSON.stringify(stations);
  const version = await sha256(serializedStations);
  const etag = `"${version}"`;
  const headers = {
    "access-control-allow-origin": "*",
    "cache-control": cacheControl,
    etag
  };
  if (request.headers.get("if-none-match") === etag) return new Response(null, { status: 304, headers });
  const payload = JSON.stringify({
    version,
    generatedAt: new Date().toISOString(),
    stations
  });
  return new Response(payload, {
    headers: {
      ...JSON_HEADERS,
      ...headers
    }
  });
}

async function publicRadioCatalog(request: Request, env: Env): Promise<Response> {
  const rows = await supabaseJson(
    env,
    "/rest/v1/radio_stations?is_active=eq.true&select=station_code,name,genre,stream_url,logo_url,epg_channel_id,bitrate,request_headers&order=station_code.asc"
  ) as Record<string, unknown>[];
  return radioCatalogResponse(request, rows.map(publicRadioStation), "public, no-cache");
}

function moviePayload(input: Record<string, unknown>, partial = false): Record<string, unknown> {
  const patch: Record<string, unknown> = {};
  if (!partial || input.title !== undefined) patch.title = requiredString(input.title, "title", 180);
  if (!partial || input.streamUrl !== undefined || input.stream_url !== undefined) {
    const streamUrl = requiredString(input.streamUrl ?? input.stream_url, "stream URL");
    let protocol = "";
    try { protocol = new URL(streamUrl).protocol; } catch { throw new Error("Invalid stream URL"); }
    if (protocol !== "https:" && protocol !== "http:") throw new Error("Invalid stream URL");
    patch.stream_url = streamUrl;
  }
  for (const [camel, snake, max] of [
    ["posterUrl", "poster_url", 2048], ["backdropUrl", "backdrop_url", 2048],
    ["description", "description", 2000], ["externalId", "external_id", 100]
  ] as const) {
    if (!partial || input[camel] !== undefined || input[snake] !== undefined) {
      const value = optionalString(input[camel] ?? input[snake], camel, max);
      if (value && (camel === "posterUrl" || camel === "backdropUrl") && !isHttpsUrl(value)) throw new Error(`Invalid ${camel}`);
      patch[snake] = value;
    }
  }
  const integer = (camel: string, snake: string, min: number, max: number) => {
    if (partial && input[camel] === undefined && input[snake] === undefined) return;
    const raw = input[camel] ?? input[snake];
    if (raw === null || raw === "" || raw === undefined) { patch[snake] = null; return; }
    const value = Number(raw);
    if (!Number.isInteger(value) || value < min || value > max) throw new Error(`Invalid ${camel}`);
    patch[snake] = value;
  };
  integer("releaseYear", "release_year", 1888, 2200);
  integer("runtimeMinutes", "runtime_minutes", 1, 10000);
  if (!partial && input.sortOrder === undefined && input.sort_order === undefined) patch.sort_order = 0;
  else integer("sortOrder", "sort_order", -100000, 100000);
  if (!partial || input.genres !== undefined) {
    const genres = input.genres ?? [];
    if (!Array.isArray(genres)) throw new Error("Invalid genres");
    patch.genres = genres.map((genre) => requiredString(genre, "genre", 60)).slice(0, 20);
  }
  if (!partial || input.requestHeaders !== undefined || input.request_headers !== undefined) {
    const headers = input.requestHeaders ?? input.request_headers ?? {};
    if (!headers || typeof headers !== "object" || Array.isArray(headers)) throw new Error("Invalid request headers");
    patch.request_headers = headers;
  }
  for (const [camel, snake, fallback] of [["isActive", "is_active", true], ["showOnHome", "show_on_home", true]] as const) {
    if (!partial || input[camel] !== undefined || input[snake] !== undefined) {
      const value = input[camel] ?? input[snake] ?? fallback;
      if (typeof value !== "boolean") throw new Error(`Invalid ${camel}`);
      patch[snake] = value;
    }
  }
  return patch;
}

async function bumpMovieConfigVersions(env: Env, ownerId: string): Promise<void> {
  const devices = await supabaseJson(env,
    `/rest/v1/devices?owner_id=eq.${ownerId}&select=id,config_version`
  ) as Record<string, unknown>[];
  await Promise.all(devices.map((device) => supabaseJson(env,
    `/rest/v1/devices?id=eq.${encodeURIComponent(String(device.id))}`,
    { method: "PATCH", body: JSON.stringify({ config_version: Number(device.config_version ?? 0) + 1 }) }
  )));
}

async function listMovies(request: Request, env: Env): Promise<Response> {
  const user = await adminUser(request, env);
  const movies = await supabaseJson(env,
    `/rest/v1/movies?owner_id=eq.${user.id}&select=*&order=sort_order.asc,created_at.desc`
  ) as Record<string, unknown>[];
  return json({ movies });
}

async function createMovie(request: Request, env: Env): Promise<Response> {
  const user = await adminUser(request, env);
  const payload = moviePayload(await body(request));
  const rows = await supabaseJson(env, "/rest/v1/movies?select=*", {
    method: "POST", headers: { prefer: "return=representation" },
    body: JSON.stringify({ owner_id: user.id, ...payload })
  }) as Record<string, unknown>[];
  await bumpMovieConfigVersions(env, user.id);
  return json({ movie: rows[0] }, 201);
}

async function updateMovie(request: Request, env: Env, movieId: string): Promise<Response> {
  const user = await adminUser(request, env);
  const patch = { ...moviePayload(await body(request), true), updated_at: new Date().toISOString() };
  const rows = await supabaseJson(env,
    `/rest/v1/movies?id=eq.${encodeURIComponent(movieId)}&owner_id=eq.${user.id}&select=*`,
    { method: "PATCH", headers: { prefer: "return=representation" }, body: JSON.stringify(patch) }
  ) as Record<string, unknown>[];
  if (!rows[0]) return json({ error: "Movie not found." }, 404);
  await bumpMovieConfigVersions(env, user.id);
  return json({ movie: rows[0] });
}

async function deleteMovie(request: Request, env: Env, movieId: string): Promise<Response> {
  const user = await adminUser(request, env);
  await supabaseJson(env, `/rest/v1/movies?id=eq.${encodeURIComponent(movieId)}&owner_id=eq.${user.id}`, { method: "DELETE" });
  await bumpMovieConfigVersions(env, user.id);
  return json({ ok: true });
}

async function searchMovieMetadata(request: Request, env: Env): Promise<Response> {
  await adminUser(request, env);
  const query = requiredString(new URL(request.url).searchParams.get("q"), "search query", 120);
  if (!env.TMDB_API_TOKEN) return json({ error: "Movie metadata lookup is not configured." }, 503);

  type TmdbMovie = {
    id: number;
    title?: string;
    overview?: string;
    release_date?: string;
    poster_path?: string | null;
    backdrop_path?: string | null;
    runtime?: number | null;
    genres?: Array<{ name?: string }>;
  };

  const headers = {
    accept: "application/json",
    authorization: `Bearer ${env.TMDB_API_TOKEN}`
  };
  const searchUrl = new URL("https://api.themoviedb.org/3/search/movie");
  searchUrl.search = new URLSearchParams({
    query,
    language: "en-US",
    region: "US",
    include_adult: "false",
    page: "1"
  }).toString();
  const searchResponse = await fetch(searchUrl, { headers });
  if (!searchResponse.ok) {
    console.error(JSON.stringify({ message: "TMDB movie search failed", status: searchResponse.status }));
    return json({ error: "Movie metadata provider unavailable." }, 502);
  }
  const searchData = await searchResponse.json<{ results?: TmdbMovie[] }>();
  const candidates = (searchData.results ?? []).slice(0, 8);
  const details = await Promise.all(candidates.map(async (candidate): Promise<TmdbMovie> => {
    const detailUrl = new URL(`https://api.themoviedb.org/3/movie/${candidate.id}`);
    detailUrl.searchParams.set("language", "en-US");
    const response = await fetch(detailUrl, { headers });
    if (!response.ok) {
      console.error(JSON.stringify({ message: "TMDB movie details failed", movieId: candidate.id, status: response.status }));
      return candidate;
    }
    return response.json<TmdbMovie>();
  }));
  const imageUrl = (path: string | null | undefined, size: "w500" | "w1280") =>
    path ? `https://image.tmdb.org/t/p/${size}${path}` : null;
  const results = details.map((movie) => ({
    externalId: `tmdb:${movie.id}`,
    title: movie.title ?? query,
    description: movie.overview ?? "",
    releaseYear: /^\d{4}-/.test(movie.release_date ?? "") ? Number(movie.release_date?.slice(0, 4)) : null,
    runtimeMinutes: Number.isInteger(movie.runtime) && Number(movie.runtime) > 0 ? movie.runtime : null,
    genres: (movie.genres ?? []).map((genre) => genre.name).filter((name): name is string => Boolean(name)),
    posterUrl: imageUrl(movie.poster_path, "w500"),
    backdropUrl: imageUrl(movie.backdrop_path, "w1280")
  }));
  return json({ provider: "TMDB", results });
}

async function listPlaylists(request: Request, env: Env): Promise<Response> {
  const user = await adminUser(request, env);
  const playlists = await supabaseJson(
    env,
    `/rest/v1/playlists?owner_id=eq.${user.id}&target_app=in.(tv,both)&select=*,playlist_items(*)&order=created_at.desc`
  ) as Record<string, unknown>[];
  return json({ playlists });
}

async function getPlaylist(request: Request, env: Env, playlistId: string): Promise<Response> {
  const user = await adminUser(request, env);
  const playlists = await supabaseJson(
    env,
    `/rest/v1/playlists?id=eq.${encodeURIComponent(playlistId)}&owner_id=eq.${user.id}&select=*,playlist_items(*)`
  ) as Record<string, unknown>[];
  if (!playlists[0]) return json({ error: "Playlist not found." }, 404);
  return json({ playlist: playlists[0] });
}

async function createPlaylist(request: Request, env: Env): Promise<Response> {
  const user = await adminUser(request, env);
  const input = await body(request);
  const title = requiredString(input.title, "title", 120);
  const epgUrl = optionalString(input.epg_url || input.epgUrl, "EPG URL");
  if (epgUrl && !isHttpsUrl(epgUrl)) throw new Error("Invalid EPG URL");

  const playlists = await supabaseJson(env, "/rest/v1/playlists?select=*", {
    method: "POST",
    headers: { prefer: "return=representation" },
    body: JSON.stringify({
      owner_id: user.id,
      title,
      description: optionalString(input.description, "description", 500),
      artwork_url: optionalString(input.artwork_url || input.artworkUrl, "artwork URL"),
      epg_url: epgUrl,
      category: optionalString(input.category, "category", 50) ?? "general",
      target_app: "tv",
      is_published: input.is_published ?? true
    })
  }) as Record<string, unknown>[];
  return json({ playlist: playlists[0] }, 201);
}

async function updatePlaylist(request: Request, env: Env, playlistId: string): Promise<Response> {
  const user = await adminUser(request, env);
  const input = await body(request);
  const patch: Record<string, unknown> = { updated_at: new Date().toISOString() };
  if (input.title) patch.title = requiredString(input.title, "title", 120);
  if (input.category !== undefined) patch.category = requiredString(input.category, "category", 50);
  if (input.description !== undefined) patch.description = optionalString(input.description, "description", 500);
  if (input.artwork_url !== undefined || input.artworkUrl !== undefined) patch.artwork_url = optionalString(input.artwork_url || input.artworkUrl, "artwork URL");
  if (input.epg_url !== undefined || input.epgUrl !== undefined) {
    const epgUrl = optionalString(input.epg_url || input.epgUrl, "EPG URL");
    if (epgUrl && !isHttpsUrl(epgUrl)) throw new Error("Invalid EPG URL");
    patch.epg_url = epgUrl;
  }
  if (typeof input.is_published === "boolean") patch.is_published = input.is_published;

  const playlists = await supabaseJson(
    env,
    `/rest/v1/playlists?id=eq.${encodeURIComponent(playlistId)}&owner_id=eq.${user.id}&select=*`,
    {
      method: "PATCH",
      headers: { prefer: "return=representation" },
      body: JSON.stringify(patch)
    }
  ) as Record<string, unknown>[];
  if (!playlists[0]) return json({ error: "Playlist not found." }, 404);
  return json({ playlist: playlists[0] });
}

async function deletePlaylist(request: Request, env: Env, playlistId: string): Promise<Response> {
  const user = await adminUser(request, env);
  await supabaseJson(
    env,
    `/rest/v1/playlists?id=eq.${encodeURIComponent(playlistId)}&owner_id=eq.${user.id}`,
    { method: "DELETE" }
  );
  return json({ ok: true });
}

async function addPlaylistItem(request: Request, env: Env, playlistId: string): Promise<Response> {
  const user = await adminUser(request, env);
  const input = await body(request);
  const playlists = await supabaseJson(
    env,
    `/rest/v1/playlists?id=eq.${encodeURIComponent(playlistId)}&owner_id=eq.${user.id}&select=id`
  ) as Record<string, unknown>[];
  if (!playlists[0]) return json({ error: "Playlist not found." }, 404);

  const metadata = (input.metadata && typeof input.metadata === "object") ? input.metadata as Record<string, unknown> : {};
  if (input.tvgId || input.tvg_id) metadata.tvg_id = input.tvgId || input.tvg_id;
  if (input.tvgChno || input.tvg_chno) metadata.tvg_chno = input.tvgChno || input.tvg_chno;
  if (input.tvgLogo || input.tvg_logo) metadata.tvg_logo = input.tvgLogo || input.tvg_logo;
  if (input.callsign || input.x_callsign) metadata.callsign = input.callsign || input.x_callsign;
  if (input.isRadio || input.radio) metadata.radio = true;

  const mediaUrl = requiredString(input.media_url || input.mediaUrl, "media URL", 2048);
  if (!isHttpsUrl(mediaUrl)) throw new Error("Invalid media URL");
  const items = await supabaseJson(env, "/rest/v1/playlist_items?select=*", {
    method: "POST",
    headers: { prefer: "return=representation" },
    body: JSON.stringify({
      playlist_id: playlistId,
      title: requiredString(input.title, "item title", 120),
      artist: optionalString(input.artist, "artist", 120),
      media_url: mediaUrl,
      duration_seconds: typeof input.duration_seconds === "number" ? input.duration_seconds : -1,
      position: typeof input.position === "number" ? input.position : 0,
      metadata
    })
  }) as Record<string, unknown>[];
  return json({ item: items[0] }, 201);
}

async function updatePlaylistItem(
  request: Request,
  env: Env,
  playlistId: string,
  itemId: string
): Promise<Response> {
  const user = await adminUser(request, env);
  const input = await body(request);
  const playlists = await supabaseJson(
    env,
    `/rest/v1/playlists?id=eq.${encodeURIComponent(playlistId)}&owner_id=eq.${user.id}&select=id`
  ) as Record<string, unknown>[];
  if (!playlists[0]) return json({ error: "Playlist not found." }, 404);

  const patch: Record<string, unknown> = {};
  if (input.title !== undefined) patch.title = requiredString(input.title, "item title", 120);
  if (input.media_url !== undefined || input.mediaUrl !== undefined) {
    const mediaUrl = requiredString(input.media_url || input.mediaUrl, "media URL", 2048);
    if (!isHttpsUrl(mediaUrl)) throw new Error("Invalid media URL");
    patch.media_url = mediaUrl;
  }
  if (typeof input.position === "number") patch.position = Math.max(0, Math.trunc(input.position));
  if (input.metadata && typeof input.metadata === "object" && !Array.isArray(input.metadata)) {
    patch.metadata = input.metadata;
  }
  if (!Object.keys(patch).length) throw new Error("Invalid playlist item update");

  const items = await supabaseJson(
    env,
    `/rest/v1/playlist_items?id=eq.${encodeURIComponent(itemId)}&playlist_id=eq.${encodeURIComponent(playlistId)}&select=*`,
    { method: "PATCH", headers: { prefer: "return=representation" }, body: JSON.stringify(patch) }
  ) as Record<string, unknown>[];
  if (!items[0]) return json({ error: "Channel not found." }, 404);
  return json({ item: items[0] });
}

type ImportedPlaylistItem = {
  title: string;
  media_url: string;
  duration_seconds: number;
  position: number;
  metadata: Record<string, unknown>;
};

function importedPlaylistItem(value: unknown, position: number): ImportedPlaylistItem {
  if (!value || typeof value !== "object" || Array.isArray(value)) throw new Error(`Invalid channel ${position}`);
  const item = value as Record<string, unknown>;
  const mediaUrl = requiredString(item.media_url || item.mediaUrl, `channel ${position} URL`, 2048);
  if (!isHttpsUrl(mediaUrl)) throw new Error(`Invalid channel ${position} URL`);
  return {
    title: requiredString(item.title, `channel ${position} title`, 120),
    media_url: mediaUrl,
    duration_seconds: typeof item.duration_seconds === "number" ? Math.trunc(item.duration_seconds) : -1,
    position,
    metadata: item.metadata && typeof item.metadata === "object" && !Array.isArray(item.metadata)
      ? item.metadata as Record<string, unknown>
      : {}
  };
}

async function importPlaylistItems(request: Request, env: Env, playlistId: string): Promise<Response> {
  const user = await adminUser(request, env);
  const input = await body(request);
  if (!Array.isArray(input.items) || input.items.length === 0 || input.items.length > 2000) {
    throw new Error("Invalid M3U channel list");
  }
  const playlists = await supabaseJson(
    env,
    `/rest/v1/playlists?id=eq.${encodeURIComponent(playlistId)}&owner_id=eq.${user.id}&select=id`
  ) as Record<string, unknown>[];
  if (!playlists[0]) return json({ error: "Playlist not found." }, 404);
  const imported = input.items.map((item, index) => ({
    playlist_id: playlistId,
    ...importedPlaylistItem(item, index + 1)
  }));

  if (input.replace === true) {
    const inserted = await supabaseJson(env, "/rest/v1/rpc/replace_playlist_items", {
      method: "POST",
      body: JSON.stringify({ p_owner_id: user.id, p_playlist_id: playlistId, p_items: imported })
    }) as number;
    return json({ imported: inserted }, 201);
  }
  const created = await supabaseJson(env, "/rest/v1/playlist_items?select=*", {
    method: "POST",
    headers: { prefer: "return=representation" },
    body: JSON.stringify(imported)
  }) as Record<string, unknown>[];
  return json({ imported: created.length }, 201);
}

async function reorderPlaylistItems(request: Request, env: Env, playlistId: string): Promise<Response> {
  const user = await adminUser(request, env);
  const input = await body(request);
  if (!Array.isArray(input.itemIds) || input.itemIds.length > 2000) throw new Error("Invalid channel order");
  const itemIds = input.itemIds.map((id, index) => requiredString(id, `channel ${index + 1} ID`, 64));
  if (new Set(itemIds).size !== itemIds.length) throw new Error("Invalid channel order");
  const playlists = await supabaseJson(
    env,
    `/rest/v1/playlists?id=eq.${encodeURIComponent(playlistId)}&owner_id=eq.${user.id}&select=id`
  ) as Record<string, unknown>[];
  if (!playlists[0]) return json({ error: "Playlist not found." }, 404);
  const existing = await supabaseJson(
    env,
    `/rest/v1/playlist_items?playlist_id=eq.${encodeURIComponent(playlistId)}&select=id`
  ) as Record<string, unknown>[];
  const existingIds = new Set(existing.map((item) => String(item.id)));
  if (itemIds.length !== existingIds.size || itemIds.some((id) => !existingIds.has(id))) {
    throw new Error("Invalid channel order");
  }
  const updated = await supabaseJson(env, "/rest/v1/rpc/reorder_playlist_items", {
    method: "POST", body: JSON.stringify({ p_owner_id: user.id, p_playlist_id: playlistId, p_item_ids: itemIds })
  }) as number;
  return json({ ok: true, updated });
}

async function deletePlaylistItem(request: Request, env: Env, playlistId: string, itemId: string): Promise<Response> {
  const user = await adminUser(request, env);
  const playlists = await supabaseJson(
    env,
    `/rest/v1/playlists?id=eq.${encodeURIComponent(playlistId)}&owner_id=eq.${user.id}&select=id`
  ) as Record<string, unknown>[];
  if (!playlists[0]) return json({ error: "Playlist not found." }, 404);

  await supabaseJson(
    env,
    `/rest/v1/playlist_items?id=eq.${encodeURIComponent(itemId)}&playlist_id=eq.${encodeURIComponent(playlistId)}`,
    { method: "DELETE" }
  );
  return json({ ok: true });
}

async function ownedPlaylist(request: Request, env: Env, playlistId: string): Promise<{ user: { id: string }; playlist: Record<string, unknown> }> {
  const user = await adminUser(request, env);
  const rows = await supabaseJson(env,
    `/rest/v1/playlists?id=eq.${encodeURIComponent(playlistId)}&owner_id=eq.${user.id}&select=id,title,is_published`
  ) as Record<string, unknown>[];
  if (!rows[0]) throw new Response("Playlist not found", { status: 404 });
  return { user, playlist: rows[0] };
}

async function getManagedGuide(request: Request, env: Env, playlistId: string): Promise<Response> {
  const { user } = await ownedPlaylist(request, env, playlistId);
  const rows = await supabaseJson(env,
    `/rest/v1/epg_guides?playlist_id=eq.${encodeURIComponent(playlistId)}&owner_id=eq.${user.id}&select=id,name,source_url,channel_count,programme_count,updated_at`
  ) as Record<string, unknown>[];
  return json({ guide: rows[0] ?? null });
}

async function saveManagedGuide(request: Request, env: Env, playlistId: string): Promise<Response> {
  const { user, playlist } = await ownedPlaylist(request, env, playlistId);
  const input = await body(request);
  let sourceUrl = optionalString(input.sourceUrl || input.source_url, "EPG source URL");
  let rawXml = input.xml;
  if (sourceUrl && typeof rawXml !== "string") {
    if (!isHttpsUrl(sourceUrl)) throw new Error("Invalid EPG source URL");
    const source = await fetch(sourceUrl, { headers: { "user-agent": "GLZ-Hub-EPG/1.0", accept: "application/xml,text/xml,application/gzip,*/*" } });
    if (!source.ok) throw new Error(`Invalid EPG source response (${source.status})`);
    rawXml = await xmlTextFromResponse(source);
  }
  const guide = validateXmlTv(rawXml);
  const name = optionalString(input.name, "guide name", 120) || `${String(playlist.title)} Guide`;
  const objectKey = managedGuideObjectKey(playlistId);
  await env.EPG_BUCKET.put(objectKey, guide.xml, {
    httpMetadata: { contentType: "application/xml; charset=utf-8" }
  });
  let rows: Record<string, unknown>[];
  try {
    rows = await supabaseJson(env, "/rest/v1/epg_guides?on_conflict=playlist_id&select=id,name,source_url,channel_count,programme_count,updated_at", {
      method: "POST",
      headers: { prefer: "resolution=merge-duplicates,return=representation" },
      body: JSON.stringify({
        owner_id: user.id, playlist_id: playlistId, name, source_url: sourceUrl,
        xml_content: null, object_key: objectKey,
        channel_count: guide.channelCount, programme_count: guide.programmeCount,
        updated_at: new Date().toISOString(), refresh_started_at: null, refresh_error: null
      })
    }) as Record<string, unknown>[];
  } catch (error) {
    const detail = error instanceof Error ? error.message : String(error);
    throw new Error(`EPG publish failed: ${detail}`);
  }
  const publicUrl = `${new URL(request.url).origin}/api/v1/guides/${playlistId}.xml.gz`;
  await supabaseJson(env, `/rest/v1/playlists?id=eq.${encodeURIComponent(playlistId)}&owner_id=eq.${user.id}`, {
    method: "PATCH", body: JSON.stringify({ epg_url: publicUrl, updated_at: new Date().toISOString() })
  });
  await invalidateManagedGuideCache(new URL(request.url).origin, playlistId);
  return json({ guide: rows[0], url: publicUrl });
}

async function fetchGuidePreview(request: Request, env: Env): Promise<Response> {
  await adminUser(request, env);
  const sourceUrl = new URL(request.url).searchParams.get("url");
  if (!sourceUrl || !isHttpsUrl(sourceUrl)) throw new Error("Invalid EPG source URL");
  const source = await fetch(sourceUrl, { headers: { "user-agent": "GLZ-Hub-EPG/1.0", accept: "application/xml,text/xml,application/gzip,*/*" } });
  if (!source.ok) throw new Error(`Invalid EPG source response (${source.status})`);
  const guide = validateXmlTv(await xmlTextFromResponse(source));
  return new Response(guide.xml, { headers: { "content-type": "application/xml; charset=utf-8", "cache-control": "no-store" } });
}

async function invalidateManagedGuideCache(origin: string, playlistId: string): Promise<void> {
  await Promise.all([
    caches.default.delete(`${origin}/api/v1/guides/${playlistId}.xml`),
    caches.default.delete(`${origin}/api/v1/guides/${playlistId}.xml.gz`)
  ]);
}

async function refreshManagedGuideSource(env: Env, row: Record<string, unknown>, origin: string): Promise<void> {
  const guideId = encodeURIComponent(String(row.id));
  try {
    // Conditional update provides a cross-isolate lock. Abandoned locks expire.
    const lockBefore = new Date(Date.now() - 10 * 60_000).toISOString();
    const claimed = await supabaseJson(env,
      `/rest/v1/epg_guides?id=eq.${guideId}&or=(refresh_started_at.is.null,refresh_started_at.lt.${encodeURIComponent(lockBefore)})&select=id`,
      {
        method: "PATCH",
        headers: { prefer: "return=representation" },
        body: JSON.stringify({ refresh_started_at: new Date().toISOString(), refresh_error: null })
      }
    ) as Record<string, unknown>[];
    if (!claimed[0]) return;

    const source = await fetch(String(row.source_url), {
      headers: {
        "user-agent": "GLZ-Hub-EPG/1.0",
        accept: "application/xml,text/xml,application/gzip,*/*"
      }
    });
    if (!source.ok) throw new Error(`EPG source returned ${source.status}`);
    const refreshed = validateXmlTv(await xmlTextFromResponse(source));
    const objectKey = managedGuideObjectKey(String(row.playlist_id));
    await env.EPG_BUCKET.put(objectKey, refreshed.xml, {
      httpMetadata: { contentType: "application/xml; charset=utf-8" }
    });
    await supabaseJson(env, `/rest/v1/epg_guides?id=eq.${guideId}`, {
      method: "PATCH",
      body: JSON.stringify({
        xml_content: null,
        object_key: objectKey,
        channel_count: refreshed.channelCount,
        programme_count: refreshed.programmeCount,
        updated_at: new Date().toISOString(),
        refresh_started_at: null,
        refresh_error: null
      })
    });
    await invalidateManagedGuideCache(origin, String(row.playlist_id));
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    await supabaseJson(env, `/rest/v1/epg_guides?id=eq.${guideId}`, {
      method: "PATCH",
      body: JSON.stringify({ refresh_started_at: null, refresh_error: message.slice(0, 500) })
    }).catch(() => undefined);
    console.error(JSON.stringify({
      event: "managed_epg_background_refresh_failed",
      guideId: String(row.id),
      error: message
    }));
  }
}

function managedGuideResponse(body: string | ReadableStream, playlistId: string, gzip: boolean, etag?: string): Response {
  const headers = new Headers({
    "content-type": "application/xml; charset=utf-8",
    "cache-control": "public, no-cache",
    "content-disposition": `attachment; filename="guide-${playlistId}.xml${gzip ? ".gz" : ""}"`
  });
  if (etag) {
    headers.set("etag", etag);
  }
  let responseBody: BodyInit = body;
  if (gzip) {
    headers.set("content-encoding", "gzip");
    const stream = body instanceof ReadableStream ? body : new Blob([body]).stream();
    responseBody = stream.pipeThrough(new CompressionStream("gzip"));
  }
  return new Response(responseBody, { headers });
}

function cleanText(value: unknown): string {
  return String(value ?? "").replace(/[\r\n]+/g, " ").trim();
}

function cleanAttribute(value: unknown): string {
  return cleanText(value).replace(/"/g, "'");
}

function formatXmlTvDate(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${date.getUTCFullYear()}${pad(date.getUTCMonth() + 1)}${pad(date.getUTCDate())}${pad(date.getUTCHours())}${pad(date.getUTCMinutes())}${pad(date.getUTCSeconds())} +0000`;
}

export function clampHealthTimeoutMs(value: unknown, fallback = 3500): number {
  const parsed = Number(value);
  if (!Number.isFinite(parsed) || parsed <= 0) return fallback;
  return Math.min(15_000, Math.max(2_000, Math.round(parsed)));
}

export function getEventBackupStreamUrl(event: Record<string, unknown>): string {
  const metadata = event.metadata && typeof event.metadata === "object"
    ? event.metadata as Record<string, unknown>
    : null;
  const candidates = [
    event.backup_stream_url,
    event.backupStreamUrl,
    metadata?.backup_stream_url,
    metadata?.backupStreamUrl
  ];
  for (const candidate of candidates) {
    if (typeof candidate !== "string") continue;
    const trimmed = candidate.trim();
    if (/^https?:\/\//i.test(trimmed)) return trimmed;
  }
  return "";
}

export function resolveEventDeliveryStreamUrl(event: Record<string, unknown>): { url: string; usingBackup: boolean } {
  const primary = typeof event.stream_url === "string" ? event.stream_url.trim() : "";
  const backup = getEventBackupStreamUrl(event);
  const primaryOffline = event.is_online === false || String(event.is_online) === "false";
  if (primaryOffline && backup) {
    return { url: backup, usingBackup: true };
  }
  if (!primary && backup) {
    return { url: backup, usingBackup: true };
  }
  return { url: primary, usingBackup: false };
}

async function checkSingleStreamHealth(streamUrl: string, timeoutMs = 3500): Promise<{ ok: boolean; statusText: string }> {
  const effectiveTimeout = clampHealthTimeoutMs(timeoutMs, 3500);
  if (!streamUrl || !/^https?:\/\//i.test(streamUrl.trim())) {
    return { ok: false, statusText: "Offline (Invalid URL)" };
  }
  try {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), effectiveTimeout);

    let res = await fetch(streamUrl, {
      method: "HEAD",
      signal: controller.signal,
      headers: {
        "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
      }
    }).catch(() => null);

    if (!res || !res.ok) {
      const controller2 = new AbortController();
      const timeout2 = setTimeout(() => controller2.abort(), effectiveTimeout);
      res = await fetch(streamUrl, {
        method: "GET",
        signal: controller2.signal,
        headers: {
          "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
          "range": "bytes=0-512"
        }
      }).catch(() => null);
      clearTimeout(timeout2);
    }
    clearTimeout(timeout);

    if (!res) return { ok: false, statusText: "Offline (Timeout)" };
    if (res.ok || res.status === 206 || res.status === 302 || res.status === 301) {
      return { ok: true, statusText: `Online (${res.status})` };
    }
    return { ok: false, statusText: `Offline (HTTP ${res.status})` };
  } catch (err) {
    return { ok: false, statusText: `Offline (${(err as Error).message})` };
  }
}

async function checkEventRowHealth(row: Record<string, unknown>, timeoutMs: number): Promise<{
  isOnline: boolean;
  effectiveOnline: boolean;
  usingBackup: boolean;
  statusText: string;
}> {
  const primaryUrl = String(row.stream_url || "").trim();
  const backupUrl = getEventBackupStreamUrl(row);
  const primaryHealth = await checkSingleStreamHealth(primaryUrl, timeoutMs);
  if (primaryHealth.ok) {
    return { isOnline: true, effectiveOnline: true, usingBackup: false, statusText: primaryHealth.statusText };
  }
  if (backupUrl) {
    const backupHealth = await checkSingleStreamHealth(backupUrl, timeoutMs);
    if (backupHealth.ok) {
      return {
        isOnline: false,
        effectiveOnline: true,
        usingBackup: true,
        statusText: `Failover (Backup ${backupHealth.statusText})`
      };
    }
    return {
      isOnline: false,
      effectiveOnline: false,
      usingBackup: false,
      statusText: "Offline (Primary + Backup)"
    };
  }
  return { isOnline: false, effectiveOnline: false, usingBackup: false, statusText: primaryHealth.statusText };
}

async function checkEventChannelsHealth(request: Request, env: Env): Promise<Response> {
  await adminUser(request, env);
  const reqUrl = new URL(request.url);
  const bodyData = await request.json().catch(() => ({})) as Record<string, unknown>;
  const rawTimeout = reqUrl.searchParams.get("timeoutMs") ?? bodyData.timeoutMs;
  const timeoutMs = clampHealthTimeoutMs(rawTimeout, 3500);
  const eventId = typeof bodyData.eventId === "string" && bodyData.eventId.trim()
    ? bodyData.eventId.trim()
    : (reqUrl.searchParams.get("eventId") || "").trim();

  const queryPath = eventId
    ? `/rest/v1/event_channels?id=eq.${encodeURIComponent(eventId)}&select=*`
    : `/rest/v1/event_channels?status=neq.disabled&select=*`;

  const rows = await supabaseJson(env, queryPath).catch(() => null) as Record<string, unknown>[] | null;

  if (!Array.isArray(rows) || rows.length === 0) {
    return json({ ok: true, checkedCount: 0, onlineCount: 0, offlineCount: 0, failoverCount: 0, timeoutMs });
  }

  let onlineCount = 0;
  let offlineCount = 0;
  let failoverCount = 0;
  const nowIso = new Date().toISOString();

  for (let i = 0; i < rows.length; i += 5) {
    const chunk = rows.slice(i, i + 5);
    await Promise.all(chunk.map(async (row) => {
      const id = String(row.id);
      const health = await checkEventRowHealth(row, timeoutMs);
      if (health.usingBackup) failoverCount++;
      if (health.effectiveOnline) onlineCount++; else offlineCount++;

      await supabaseJson(env, `/rest/v1/event_channels?id=eq.${encodeURIComponent(id)}`, {
        method: "PATCH",
        body: JSON.stringify({
          is_online: health.isOnline,
          health_status: health.statusText,
          last_checked_at: nowIso,
          updated_at: nowIso
        })
      }).catch(() => undefined);
    }));
  }

  await pushConfigUpdateToAllDevices(env);
  return json({ ok: true, checkedCount: rows.length, onlineCount, offlineCount, failoverCount, timeoutMs });
}

function escapeXml(value: unknown): string {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&apos;")
    .replace(/[\r\n]+/g, " ")
    .trim();
}

const DEFAULT_EVENT_DURATION_MS = 3 * 3600_000;
const MAX_EVENT_DURATION_MS = 12 * 3600_000;
const MAX_EVENT_BUFFER_HOURS = 6;

function clampEventBufferHours(value: unknown, fallback = 0.5): number {
  const parsed = Number(value ?? fallback);
  if (!Number.isFinite(parsed) || parsed < 0) return fallback;
  return Math.min(parsed, MAX_EVENT_BUFFER_HOURS);
}

function resolveEventWindow(event: Record<string, unknown>, now = Date.now()): {
  startMs: number;
  endMs: number;
  winStartMs: number;
  winEndMs: number;
  hasValidWindow: boolean;
} {
  const startTime = new Date(String(event.start_time || "")).getTime();
  const rawEndTime = new Date(String(event.end_time || "")).getTime();
  const hasValidWindow = Number.isFinite(startTime) && Number.isFinite(rawEndTime);
  const startMs = Number.isFinite(startTime) ? startTime : now;
  const rawDuration = rawEndTime - startMs;
  const endMs = (Number.isFinite(rawEndTime) && rawDuration > 0 && rawDuration <= MAX_EVENT_DURATION_MS)
    ? rawEndTime
    : startMs + DEFAULT_EVENT_DURATION_MS;
  const preBufferMs = clampEventBufferHours(event.pre_buffer_hours, 0.5) * 3600_000;
  const postBufferMs = clampEventBufferHours(event.post_buffer_hours, 0.5) * 3600_000;
  return {
    startMs,
    endMs,
    winStartMs: startMs - preBufferMs,
    winEndMs: endMs + postBufferMs,
    hasValidWindow
  };
}

export function isEventActiveAndValid(event: Record<string, unknown>, now = Date.now()): boolean {
  const primaryOffline = event.is_online === false || String(event.is_online) === "false";
  if (primaryOffline) {
    const backupUrl = getEventBackupStreamUrl(event);
    const bothOffline = String(event.health_status || "").toLowerCase().includes("primary + backup");
    if (!backupUrl || bothOffline) return false;
  }
  const status = String(event.status || "active");
  if (status === "disabled" || status === "expired") return false;

  const { winStartMs, winEndMs, hasValidWindow } = resolveEventWindow(event, now);
  if (hasValidWindow && (now < winStartMs || now > winEndMs)) {
    return false; // Event has ended or is outside active window
  }

  const title = String(event.title || "");
  const group = String(event.group_title || "");
  const tvgId = String(event.tvg_id || "");
  const tvgName = String(event.tvg_name || "");
  const sportLeague = String(event.sport_league || "");

  // For auto-ingested channels, align with isLiveEventChannel (MLB, NBA, NFL, NHL, NCAA, UFC, Boxing).
  // Manually created channels (auto_ingested === false) bypass league filtering.
  if (event.auto_ingested !== false) {
    if (!isLiveEventChannel(group, title, tvgId, tvgName, sportLeague)) return false;
  } else {
    const fullText = `${sportLeague} ${title} ${group} ${tvgId}`.toUpperCase();
    const isGerman = /\b(DE|DEUTSCHLAND|GERMANY)\b/i.test(group) ||
                     /\b(DE|DEUTSCHLAND|GERMANY)\b/i.test(tvgId) ||
                     /^(DE|GER):/i.test(title) ||
                     /\[DE\]|\(DE\)|\|DE\|/i.test(fullText);
    if (isGerman) return false;

    const isPlaceholder = /\b(WILL START SOON|OFFLINE|OFF-LINE|NO EVENT|STREAM UNAVAILABLE|TEST|EMPTY|FEED OFFLINE|STANDBY|CHANNEL UNAVAILABLE|TEMPORARILY OFFLINE|NOT AVAILABLE|NO BROADCAST|NO SIGNAL|STREAM DOWN|OFF AIR|SIGN OFF|CHANNEL OFFLINE|STREAMING SOON|EVENT ENDED|FEED DOWN|TBD)\b/i.test(fullText);
    if (isPlaceholder) return false;
  }

  return true;
}

async function injectEventChannelsXmlTv(env: Env, xmlText: string): Promise<string> {
  if (!xmlText || !xmlText.includes("</tv>")) return xmlText;

  const eventChannels = await supabaseJson(env,
    `/rest/v1/event_channels?status=neq.disabled&select=*`
  ).catch(() => null) as Record<string, unknown>[] | null;

  if (!Array.isArray(eventChannels) || eventChannels.length === 0) return xmlText;
  const sortedEvents = sortEventChannels(eventChannels);

  let channelNodes = "";
  let programmeNodes = "";
  let index = 1;
  const now = Date.now();

  for (const event of sortedEvents) {
    if (!isEventActiveAndValid(event, now)) continue;

    const tvgId = escapeXml(String(event.tvg_id || `event.channel.${index}`));
    const channelNumber = escapeXml(String(event.channel_number || `30-${String(index).padStart(2, "0")}`));
    const eventTitle = escapeXml(cleanText(String(event.title || "Live Sports Event")));
    const sportLeague = escapeXml(cleanText(String(event.sport_league || "SPORTS")));
    const logoUrl = event.logo_url ? escapeXml(String(event.logo_url)) : "";

    const { startMs, endMs, winStartMs, winEndMs } = resolveEventWindow(event, now);
    const startTime = new Date(startMs);
    const endTime = new Date(endMs);
    const winStart = new Date(winStartMs);
    const winEnd = new Date(winEndMs);

    channelNodes += `  <channel id="${tvgId}">\n`;
    channelNodes += `    <display-name>${tvgId}</display-name>\n`;
    channelNodes += `    <display-name>CH ${channelNumber}</display-name>\n`;
    if (logoUrl) {
      channelNodes += `    <icon src="${logoUrl}" />\n`;
    }
    channelNodes += `  </channel>\n`;

    if (winStart.getTime() < startTime.getTime()) {
      programmeNodes += `  <programme start="${formatXmlTvDate(winStart)}" stop="${formatXmlTvDate(startTime)}" channel="${tvgId}">\n`;
      programmeNodes += `    <title lang="en">Pre-Game · ${eventTitle}</title>\n`;
      programmeNodes += `    <desc lang="en">Pre-game coverage for ${sportLeague} - ${eventTitle}</desc>\n`;
      programmeNodes += `    <category lang="en">Sports</category>\n`;
      programmeNodes += `  </programme>\n`;
    }

    programmeNodes += `  <programme start="${formatXmlTvDate(startTime)}" stop="${formatXmlTvDate(endTime)}" channel="${tvgId}">\n`;
    programmeNodes += `    <title lang="en">${eventTitle}</title>\n`;
    programmeNodes += `    <desc lang="en">Live ${sportLeague} Event - ${eventTitle}</desc>\n`;
    programmeNodes += `    <category lang="en">Sports</category>\n`;
    programmeNodes += `    <category lang="en">Pay Per View</category>\n`;
    programmeNodes += `  </programme>\n`;

    if (winEnd.getTime() > endTime.getTime()) {
      programmeNodes += `  <programme start="${formatXmlTvDate(endTime)}" stop="${formatXmlTvDate(winEnd)}" channel="${tvgId}">\n`;
      programmeNodes += `    <title lang="en">Post-Game · ${eventTitle}</title>\n`;
      programmeNodes += `    <desc lang="en">Post-game wrap-up for ${sportLeague} - ${eventTitle}</desc>\n`;
      programmeNodes += `    <category lang="en">Sports</category>\n`;
      programmeNodes += `  </programme>\n`;
    }

    index++;
  }

  let updatedXml = xmlText;
  const firstProgIndex = updatedXml.indexOf("<programme");
  if (firstProgIndex !== -1) {
    updatedXml = updatedXml.slice(0, firstProgIndex) + channelNodes + updatedXml.slice(firstProgIndex);
  } else {
    updatedXml = updatedXml.replace("</tv>", channelNodes + "</tv>");
  }

  updatedXml = updatedXml.replace("</tv>", programmeNodes + "</tv>");
  return updatedXml;
}

async function exportManagedGuide(
  request: Request,
  env: Env,
  ctx: ExecutionContext,
  playlistId: string,
  gzip: boolean,
  requireAdmin = false,
  ownerId = ""
): Promise<Response> {
  let ownerFilter = "";
  if (requireAdmin) {
    const { user } = await ownedPlaylist(request, env, playlistId);
    ownerFilter = `&owner_id=eq.${user.id}`;
  } else if (ownerId) {
    ownerFilter = `&owner_id=eq.${encodeURIComponent(ownerId)}`;
  } else {
    const playlists = await supabaseJson(env,
      `/rest/v1/playlists?id=eq.${encodeURIComponent(playlistId)}&is_published=eq.true&select=id`
    ) as Record<string, unknown>[];
    if (!playlists[0]) return json({ error: "Guide not found." }, 404);
  }
  const rows = await supabaseJson(env,
    `/rest/v1/epg_guides?playlist_id=eq.${encodeURIComponent(playlistId)}${ownerFilter}&select=id,playlist_id,name,source_url,object_key,xml_content,updated_at,refresh_started_at`
  ) as Record<string, unknown>[];
  if (!rows[0]) return json({ error: "Guide not found." }, 404);
  const row = rows[0];
  const stale = row.source_url && Date.now() - new Date(String(row.updated_at)).getTime() >= 6 * 60 * 60_000;
  if (stale) ctx.waitUntil(refreshManagedGuideSource(env, row, new URL(request.url).origin));

  let guideBody: string | ReadableStream;
  if (row.object_key) {
    const object = await env.EPG_BUCKET.get(String(row.object_key));
    if (!object) throw new Error("Managed EPG object is missing from storage");
    guideBody = object.body;
  } else {
    guideBody = String(row.xml_content);
  }

  let rawXml = typeof guideBody === "string" ? guideBody : await new Response(guideBody).text();
  const guideXml = await injectEventChannelsXmlTv(env, rawXml);
  const etag = `"${await sha256(guideXml)}"`;
  if (request.headers.get("if-none-match") === etag) {
    return new Response(null, {
      status: 304,
      headers: {
        "content-type": "application/xml; charset=utf-8",
        "cache-control": "public, no-cache",
        "etag": etag,
        "x-glzhub-cache": "REVALIDATED"
      }
    });
  }
  const response = managedGuideResponse(guideXml, playlistId, gzip, etag);
  response.headers.set("x-glzhub-cache", "BYPASS");
  return response;
}

async function pushPlaylist(request: Request, env: Env, playlistId: string): Promise<Response> {
  const { user } = await ownedPlaylist(request, env, playlistId);
  const token = crypto.randomUUID();
  const groups = await supabaseJson(env,
    `/rest/v1/box_groups?owner_id=eq.${user.id}&playlist_id=eq.${encodeURIComponent(playlistId)}&select=id`
  ).catch(() => []) as Record<string, unknown>[];
  const groupIds = (groups || []).map((g) => String(g.id)).filter(Boolean);
  const orFilters = [
    `assigned_playlist_id.eq.${encodeURIComponent(playlistId)}`,
    `assigned_playlist_id.is.null`
  ];
  if (groupIds.length > 0) {
    orFilters.push(`box_group_id.in.(${groupIds.map((id) => encodeURIComponent(id)).join(",")})`);
  }
  const devices = await supabaseJson(env,
    `/rest/v1/devices?owner_id=eq.${user.id}&or=(${orFilters.join(",")})&select=id,config_version,last_seen_at`
  ) as Record<string, unknown>[];
  const pushedAt = new Date().toISOString();
  const tenMinutesAgo = Date.now() - 10 * 60_000;
  let onlineCount = 0;
  await Promise.all(devices.map((device) => {
    const isOnline = device.last_seen_at && new Date(String(device.last_seen_at)).getTime() > tenMinutesAgo;
    if (isOnline) onlineCount++;
    return supabaseJson(env,
      `/rest/v1/devices?id=eq.${encodeURIComponent(String(device.id))}&owner_id=eq.${user.id}`,
      { method: "PATCH", body: JSON.stringify({
        config_version: Number(device.config_version || 0) + 1,
        force_refresh_token: token,
        sync_status: "queued",
        sync_progress: 0,
        sync_message: isOnline ? "Waiting for TV" : "Queued (TV offline)",
        sync_updated_at: pushedAt
      }) }
    );
  }));
  return json({ ok: true, pushedDevices: devices.length, onlineDevices: onlineCount, totalDevices: devices.length });
}

interface ParsedEventChannel {
  tvgId: string;
  tvgName: string;
  title: string;
  sportLeague: string;
  groupTitle: string;
  logoUrl: string;
  streamUrl: string;
  channelNumber: string;
  startTime: string;
  endTime: string;
  hasInlineTime?: boolean;
}

function parseSubChannelNumber(chno: unknown): number {
  const str = String(chno ?? "").trim();
  const match = str.match(/(\d+)\s*[-._:]?\s*(\d+)?/);
  if (!match) return 999999;
  const major = parseInt(match[1], 10) || 0;
  const minor = match[2] ? parseInt(match[2], 10) : 0;
  return major * 100000 + minor;
}

function sortEventChannels<T extends { channel_number?: unknown; channelNumber?: unknown; start_time?: unknown; startTime?: unknown }>(events: T[]): T[] {
  return [...events].sort((a, b) => {
    const numA = parseSubChannelNumber(a.channel_number || a.channelNumber);
    const numB = parseSubChannelNumber(b.channel_number || b.channelNumber);
    if (numA !== numB) return numA - numB;
    const timeA = new Date(String(a.start_time || a.startTime || 0)).getTime();
    const timeB = new Date(String(b.start_time || b.startTime || 0)).getTime();
    return timeA - timeB;
  });
}

function getEasternOffsetHours(date: Date): number {
  try {
    const parts = new Intl.DateTimeFormat('en-US', {
      timeZone: 'America/New_York',
      timeZoneName: 'shortOffset'
    }).formatToParts(date);
    const offsetStr = parts.find(p => p.type === 'timeZoneName')?.value || '-4';
    const match = offsetStr.match(/GMT([+-]\d+)/);
    if (match) return parseInt(match[1], 10);
  } catch {
    const month = date.getUTCMonth();
    return (month >= 2 && month <= 10) ? -4 : -5;
  }
  return -4;
}

function getEasternDateParts(date: Date): { year: number; month: number; day: number; hour: number } {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: 'America/New_York',
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
    hour: 'numeric',
    hour12: false
  }).formatToParts(date);
  const year = parseInt(parts.find(p => p.type === 'year')?.value || String(date.getUTCFullYear()), 10);
  const month = parseInt(parts.find(p => p.type === 'month')?.value || String(date.getUTCMonth() + 1), 10) - 1;
  const day = parseInt(parts.find(p => p.type === 'day')?.value || String(date.getUTCDate()), 10);
  const rawHour = parseInt(parts.find(p => p.type === 'hour')?.value || String(date.getUTCHours()), 10);
  const hour = rawHour === 24 ? 0 : rawHour;
  return { year, month, day, hour };
}

function easternWallTimeToUtc(year: number, month: number, day: number, hours: number, minutes: number): Date {
  // Estimate UTC using standard EST (-5) then refine with the exact America/New_York offset for that date.
  const approxUtc = new Date(Date.UTC(year, month, day, hours + 5, minutes));
  const targetOffset = getEasternOffsetHours(approxUtc);
  return new Date(Date.UTC(year, month, day, hours - targetOffset, minutes));
}

export function parseInlineEventTime(title: string, now = new Date()): { startTime: Date; endTime: Date; hasInlineTime: boolean } {
  const months: Record<string, number> = {
    jan: 0, feb: 1, mar: 2, apr: 3, may: 4, jun: 5,
    jul: 6, aug: 7, sep: 8, oct: 9, nov: 10, dec: 11
  };
  const etNow = getEasternDateParts(now);

  // 1. Matches `@ Sep 16 7:10 PM`, `Nov 22, 7:10 PM`, `Oct 18 7:10PM`, `10/18, 7:10 PM`
  const matchWithMonth = title.match(/(?:@|\||-|\(|\[|\b)\s*(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|\d{1,2}[\/-]\d{1,2})\.?(?:\s+(\d{1,2}))?,?\s+(\d{1,2}):(\d{2})\s*(AM|PM)?/i);
  if (matchWithMonth) {
    const monthPart = matchWithMonth[1].toLowerCase();
    const dayPart = matchWithMonth[2];
    let hours = parseInt(matchWithMonth[3], 10);
    const minutes = parseInt(matchWithMonth[4], 10);
    const ampm = matchWithMonth[5]?.toUpperCase();

    if (ampm === "PM" && hours < 12) hours += 12;
    else if (ampm === "AM" && hours === 12) hours = 0;
    else if (!ampm && hours >= 1 && hours <= 11) hours += 12;

    let month = etNow.month;
    let day = etNow.day;

    if (monthPart in months) {
      month = months[monthPart];
      if (dayPart) day = parseInt(dayPart, 10);
    } else if (monthPart.includes("/") || monthPart.includes("-")) {
      const [m, d] = monthPart.split(/[\/-]/).map(n => parseInt(n, 10));
      if (m > 0 && m <= 12) month = m - 1;
      if (d > 0 && d <= 31) day = d;
    }

    let year = etNow.year;
    if (etNow.month === 11 && month === 0) year += 1;
    else if (etNow.month === 0 && month === 11) year -= 1;

    const start = easternWallTimeToUtc(year, month, day, hours, minutes);
    const end = new Date(start.getTime() + DEFAULT_EVENT_DURATION_MS);
    return { startTime: start, endTime: end, hasInlineTime: true };
  }

  // 2. Matches `7:10 PM ET`, `(7:10 PM)`, `@ 7:10 PM`, `7:10PM`
  const matchTimeOnly = title.match(/(?:@|\(|\b)\s*(\d{1,2}):(\d{2})\s*(AM|PM)?\s*(?:ET|EDT|EST)?(?:\)|\b)/i);
  if (matchTimeOnly) {
    let hours = parseInt(matchTimeOnly[1], 10);
    const minutes = parseInt(matchTimeOnly[2], 10);
    const ampm = matchTimeOnly[3]?.toUpperCase();

    if (ampm === "PM" && hours < 12) hours += 12;
    else if (ampm === "AM" && hours === 12) hours = 0;
    else if (!ampm && hours >= 1 && hours <= 11) hours += 12;

    const start = easternWallTimeToUtc(etNow.year, etNow.month, etNow.day, hours, minutes);
    const end = new Date(start.getTime() + DEFAULT_EVENT_DURATION_MS);
    return { startTime: start, endTime: end, hasInlineTime: true };
  }

  // 3. Fallback when title does not state an explicit time: align start to Eastern Time for today
  const start = easternWallTimeToUtc(etNow.year, etNow.month, etNow.day, etNow.hour, 0);
  const end = new Date(start.getTime() + DEFAULT_EVENT_DURATION_MS);
  return { startTime: start, endTime: end, hasInlineTime: false };
}

function detectSportLeague(tvgId: string, groupTitle: string, title: string): string {
  const text = `${tvgId} ${groupTitle} ${title}`.toUpperCase();
  if (text.includes("MLB")) return "MLB";
  if (/\b(NFL|SUNDAY\s+TICKET|RED\s*ZONE|TNF|SNF|MNF|(?:THURSDAY|SUNDAY|MONDAY)\s+NIGHT\s+FOOTBALL)\b/.test(text)) return "NFL";
  if (text.includes("NBA")) return "NBA";
  if (text.includes("NCAA") || text.includes("NCAAF") || text.includes("COLLEGE FOOTBALL") || text.includes("CFB")) return "NCAA";
  if (text.includes("NHL")) return "NHL";
  if (text.includes("BOXING")) return "BOXING";
  if (text.includes("UFC")) return "UFC";
  if (text.includes("MLS")) return "MLS";
  if (text.includes("MMA") || text.includes("PFL")) return "MMA";
  if (text.includes("TENNIS") || text.includes("ATP") || text.includes("WTA") || text.includes("WIMBLEDON")) return "TENNIS";
  return "SPORTS";
}

const DEFAULT_EVENT_LOGOS: Record<string, string> = {
  MLB: "https://i.ibb.co/P0Gr6v6/mlb.png",
  NFL: "https://i.ibb.co/WNJnTdTH/nfl.png",
  UFC: "https://i.ibb.co/zW8RRPKF/specevent.png",
  BOXING: "https://i.ibb.co/zW8RRPKF/specevent.png"
};

// Named channels that keep their own scraped logo instead of the generic MLB default
const MLB_LOGO_EXCLUSIONS = ["MLB NETWORK", "STRIKE ZONE", "STRIKEZONE"];

function resolveEventLogoUrl(sportLeague: string, title: string, tvgName: string): string | null {
  const nameText = `${title} ${tvgName}`.toUpperCase();
  if (sportLeague === "MLB" && MLB_LOGO_EXCLUSIONS.some(exclusion => nameText.includes(exclusion))) {
    return null;
  }
  return DEFAULT_EVENT_LOGOS[sportLeague] ?? null;
}

function isDuplicateMainSportsNetwork(groupTitle: string, title: string, tvgId: string, tvgName = ""): boolean {
  const fullText = `${groupTitle} ${title} ${tvgId} ${tvgName}`.toUpperCase();
  return /\bNBA\s*(?:TV|TELEVISION)\b/.test(fullText) ||
         /\bMLB\s*(?:TV|NETWORK)\b/.test(fullText) ||
         /\bNFL\s*NETWORK\b/.test(fullText);
}

export function isLiveEventChannel(groupTitle: string, title: string, tvgId: string, tvgName = "", sportLeague = ""): boolean {
  const groupUpper = groupTitle.toUpperCase();
  const titleUpper = title.toUpperCase();
  const tvgIdUpper = tvgId.toUpperCase();
  const leagueUpper = sportLeague.toUpperCase();
  const fullText = `${leagueUpper} ${groupUpper} ${titleUpper} ${tvgIdUpper} ${tvgName.toUpperCase()}`;

  // 1. Strictly exclude DE (German) channels & non-US / foreign German feeds
  const isGerman = /\b(DE|DEUTSCHLAND|GERMANY)\b/i.test(groupUpper) ||
                   /\b(DE|DEUTSCHLAND|GERMANY)\b/i.test(tvgIdUpper) ||
                   /^(DE|GER):/i.test(titleUpper) ||
                   /\[DE\]|\(DE\)|\|DE\|/i.test(fullText);
  if (isGerman) return false;

  // 2. Exclude empty or unadvertised placeholder / offline feeds
  const isPlaceholder = /\b(WILL START SOON|OFFLINE|OFF-LINE|NO EVENT|STREAM UNAVAILABLE|TEST|EMPTY|FEED OFFLINE|STANDBY|CHANNEL UNAVAILABLE|TEMPORARILY OFFLINE|NOT AVAILABLE|NO BROADCAST|NO SIGNAL|STREAM DOWN|OFF AIR|SIGN OFF|CHANNEL OFFLINE|STREAMING SOON|EVENT ENDED|FEED DOWN|TBD)\b/i.test(fullText);
  if (isPlaceholder) return false;

  // 3. Linear sports networks already live in the main playlist and must never
  // be duplicated as temporary event channels.
  if (isDuplicateMainSportsNetwork(groupTitle, title, tvgId, tvgName)) return false;

  // 4. Auto-ingest only the approved US leagues and premium combat events.
  // Avoid generic "football" because that also catches unrelated international feeds.
  const isApprovedLeague = /\b(MLB|NBA|NFL|SUNDAY\s+TICKET|RED\s*ZONE|TNF|SNF|MNF|(?:THURSDAY|SUNDAY|MONDAY)\s+NIGHT\s+FOOTBALL|NHL|NCAA|NCAAF|CFB|COLLEGE\s+FOOTBALL|UFC|BOXING)\b/i.test(fullText);
  if (!isApprovedLeague) return false;

  // 5. Require event context rather than treating every league-branded stream
  // as a temporary game. Provider event groups and matchup titles both qualify.
  const isEventGroup = groupUpper.includes("EVENTS") || groupUpper.includes("PPV") || groupUpper.includes("SPORTS");
  const containsMatchup = /\b(vs\.?|@)\b/i.test(titleUpper) || /\b(vs\.?|@)\b/i.test(tvgIdUpper);

  return containsMatchup || isEventGroup;
}

function extractExtInfTitle(extInfLine: string): string {
  let inSingle = false;
  let inDouble = false;
  for (let i = 0; i < extInfLine.length; i++) {
    const ch = extInfLine[i];
    if (ch === '"' && !inSingle) inDouble = !inDouble;
    else if (ch === "'" && !inDouble) inSingle = !inSingle;
    else if (ch === "," && !inSingle && !inDouble) {
      return extInfLine.substring(i + 1).trim() || "Live Event";
    }
  }
  return "Live Event";
}

export function parseConMeM3u(m3uText: string): ParsedEventChannel[] {
  const lines = m3uText.split(/\r?\n/);
  const events: ParsedEventChannel[] = [];
  let pendingExtInf: string | null = null;

  for (const raw of lines) {
    const line = raw.trim();
    if (!line) continue;
    if (line.startsWith("#EXTINF:")) {
      pendingExtInf = line;
      continue;
    }
    if (line.startsWith("#") || !pendingExtInf) continue;

    const streamUrl = line;
    const tvgIdMatch = pendingExtInf.match(/tvg-id="([^"]+)"/i);
    const tvgNameMatch = pendingExtInf.match(/tvg-name="([^"]+)"/i);
    const groupTitleMatch = pendingExtInf.match(/group-title="([^"]+)"/i);
    const tvgLogoMatch = pendingExtInf.match(/tvg-logo="([^"]+)"/i);
    const title = extractExtInfTitle(pendingExtInf);

    const tvgId = tvgIdMatch ? tvgIdMatch[1] : `event.${events.length + 1}`;
    const tvgName = tvgNameMatch ? tvgNameMatch[1] : tvgId;
    const groupTitle = groupTitleMatch ? groupTitleMatch[1] : "Major League Sports (Events)";
    const scrapedLogoUrl = tvgLogoMatch ? tvgLogoMatch[1] : "";

    // Strictly filter: only ingest live event and live sports streams!
    if (!isLiveEventChannel(groupTitle, title, tvgId, tvgName)) {
      pendingExtInf = null;
      continue;
    }

    const parsedTimes = parseInlineEventTime(title);
    const sportLeague = detectSportLeague(tvgId, groupTitle, title);
    const logoUrl = resolveEventLogoUrl(sportLeague, title, tvgName) ?? scrapedLogoUrl;

    events.push({
      tvgId,
      tvgName,
      title,
      sportLeague,
      groupTitle,
      logoUrl,
      streamUrl,
      channelNumber: "",
      startTime: parsedTimes.startTime.toISOString(),
      endTime: parsedTimes.endTime.toISOString(),
      hasInlineTime: parsedTimes.hasInlineTime
    });

    pendingExtInf = null;
  }

  return assignCategoryChannelNumbers(events);
}

export interface EventChannelRanges {
  mode: "dashed" | "sequential";
  mlb: number;
  major: number;
  ppv: number;
}

export const DEFAULT_EVENT_CHANNEL_RANGES: EventChannelRanges = {
  mode: "dashed",
  mlb: 31,
  major: 30,
  ppv: 29
};

export function normalizeEventChannelRanges(raw: unknown): EventChannelRanges {
  const obj = (raw && typeof raw === "object") ? raw as Record<string, unknown> : {};
  const mode = String(obj.mode || "dashed").toLowerCase() === "sequential" ? "sequential" : "dashed";
  const clamp = (val: unknown, fallback: number): number => {
    const n = Number(val);
    if (!Number.isFinite(n)) return fallback;
    return Math.min(9999, Math.max(1, Math.round(n)));
  };
  return {
    mode,
    mlb: clamp(obj.mlb, DEFAULT_EVENT_CHANNEL_RANGES.mlb),
    major: clamp(obj.major, DEFAULT_EVENT_CHANNEL_RANGES.major),
    ppv: clamp(obj.ppv, DEFAULT_EVENT_CHANNEL_RANGES.ppv)
  };
}

export function formatEventChannelNumber(base: number, index: number, mode: "dashed" | "sequential"): string {
  if (mode === "sequential") {
    return String(base + index);
  }
  return `${base}-${String(index + 1).padStart(2, "0")}`;
}

async function loadEventChannelRanges(env: Env): Promise<EventChannelRanges> {
  try {
    const rows = await supabaseJson(
      env,
      "/rest/v1/hub_settings?key=eq.live_event_channel_ranges&select=value&limit=1"
    ) as Array<{ value?: unknown }>;
    if (Array.isArray(rows) && rows.length > 0 && rows[0]?.value) {
      return normalizeEventChannelRanges(rows[0].value);
    }
  } catch {
    // Table may not exist yet before migration 032 is applied; fall back safely to defaults.
  }
  return { ...DEFAULT_EVENT_CHANNEL_RANGES };
}

async function saveEventChannelRanges(env: Env, ranges: EventChannelRanges): Promise<EventChannelRanges> {
  const normalized = normalizeEventChannelRanges(ranges);
  await supabaseJson(env, "/rest/v1/hub_settings?on_conflict=key&select=key,value", {
    method: "POST",
    headers: { Prefer: "resolution=merge-duplicates,return=representation" },
    body: JSON.stringify({
      key: "live_event_channel_ranges",
      value: normalized,
      updated_at: new Date().toISOString()
    })
  });
  return normalized;
}

function getEventChannelTier(sportLeague: string, title: string, groupTitle: string, tvgId: string): "mlb" | "major" | "ppv" {
  const fullText = `${sportLeague} ${title} ${groupTitle} ${tvgId}`.toUpperCase();

  // Tier 1: MLB Events
  if (sportLeague === "MLB" || /\b(MLB|BASEBALL)\b/i.test(fullText)) {
    return "mlb";
  }

  // Tier 2: NBA, NFL, college football, and NHL events
  const isUSMajor = /\b(NBA|NFL|SUNDAY\s+TICKET|RED\s*ZONE|TNF|SNF|MNF|NCAA|NCAAF|CFB|COLLEGE\s*FOOTBALL|NHL)\b/i.test(fullText);
  if (isUSMajor || ["NBA", "NFL", "NCAA", "NHL"].includes(sportLeague)) {
    return "major";
  }

  // Tier 3: UFC, boxing, and manually selected PPV events
  return "ppv";
}

export function assignCategoryChannelNumbers(
  events: ParsedEventChannel[],
  ranges: EventChannelRanges = DEFAULT_EVENT_CHANNEL_RANGES
): ParsedEventChannel[] {
  const mlbEvents: ParsedEventChannel[] = [];
  const usMajorEvents: ParsedEventChannel[] = [];
  const ppvEvents: ParsedEventChannel[] = [];

  for (const item of events) {
    const tier = getEventChannelTier(item.sportLeague, item.title, item.groupTitle, item.tvgId);
    if (tier === "mlb") {
      mlbEvents.push(item);
    } else if (tier === "major") {
      usMajorEvents.push(item);
    } else {
      ppvEvents.push(item);
    }
  }

  const result: ParsedEventChannel[] = [];

  mlbEvents.sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());
  mlbEvents.forEach((item, idx) => {
    result.push({ ...item, channelNumber: formatEventChannelNumber(ranges.mlb, idx, ranges.mode) });
  });

  usMajorEvents.sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());
  usMajorEvents.forEach((item, idx) => {
    result.push({ ...item, channelNumber: formatEventChannelNumber(ranges.major, idx, ranges.mode) });
  });

  ppvEvents.sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());
  ppvEvents.forEach((item, idx) => {
    result.push({ ...item, channelNumber: formatEventChannelNumber(ranges.ppv, idx, ranges.mode) });
  });

  return sortEventChannels(result);
}

async function getEventChannelRangesHandler(request: Request, env: Env): Promise<Response> {
  await adminUser(request, env);
  const ranges = await loadEventChannelRanges(env);
  return json({ ok: true, ranges });
}

async function updateEventChannelRangesHandler(request: Request, env: Env): Promise<Response> {
  await adminUser(request, env);
  try {
    const body = await request.json().catch(() => ({})) as Record<string, unknown>;
    const normalized = normalizeEventChannelRanges(body);
    await saveEventChannelRanges(env, normalized);

    // Immediately renumber all existing auto-ingested live events while preserving manual overrides
    const rows = await supabaseJson(
      env,
      "/rest/v1/event_channels?select=id,tvg_id,title,sport_league,group_title,start_time,auto_ingested,channel_number"
    ) as Array<Record<string, unknown>>;

    const autoRows = (rows || []).filter((r) => r.auto_ingested !== false);
    const mlbRows: Array<Record<string, unknown>> = [];
    const majorRows: Array<Record<string, unknown>> = [];
    const ppvRows: Array<Record<string, unknown>> = [];

    for (const row of autoRows) {
      const tier = getEventChannelTier(
        String(row.sport_league || ""),
        String(row.title || ""),
        String(row.group_title || ""),
        String(row.tvg_id || "")
      );
      if (tier === "mlb") mlbRows.push(row);
      else if (tier === "major") majorRows.push(row);
      else ppvRows.push(row);
    }

    const byStart = (a: Record<string, unknown>, b: Record<string, unknown>) =>
      new Date(String(a.start_time || 0)).getTime() - new Date(String(b.start_time || 0)).getTime();
    mlbRows.sort(byStart);
    majorRows.sort(byStart);
    ppvRows.sort(byStart);

    const updates: Array<{ id: string; channelNumber: string }> = [];
    mlbRows.forEach((row, idx) => {
      const next = formatEventChannelNumber(normalized.mlb, idx, normalized.mode);
      if (String(row.channel_number || "") !== next) updates.push({ id: String(row.id), channelNumber: next });
    });
    majorRows.forEach((row, idx) => {
      const next = formatEventChannelNumber(normalized.major, idx, normalized.mode);
      if (String(row.channel_number || "") !== next) updates.push({ id: String(row.id), channelNumber: next });
    });
    ppvRows.forEach((row, idx) => {
      const next = formatEventChannelNumber(normalized.ppv, idx, normalized.mode);
      if (String(row.channel_number || "") !== next) updates.push({ id: String(row.id), channelNumber: next });
    });

    await Promise.all(updates.map(({ id, channelNumber }) =>
      supabaseJson(env, `/rest/v1/event_channels?id=eq.${encodeURIComponent(id)}`, {
        method: "PATCH",
        body: JSON.stringify({ channel_number: channelNumber, updated_at: new Date().toISOString() })
      })
    ));

    await pushConfigUpdateToAllDevices(env);
    return json({ ok: true, ranges: normalized, renumberedCount: updates.length });
  } catch (err) {
    return json({ ok: false, error: (err as Error).message }, 400);
  }
}

export interface ScheduledSportsGame {
  league: "NFL" | "NBA" | "MLB" | "NHL" | "NCAA";
  gameId: string;
  shortName: string;
  name: string;
  awayTokens: string[];
  homeTokens: string[];
  startTime: string;
  endTime: string;
  state: "in" | "pre" | "post";
  broadcasts: string[];
}

export interface EspnScheduleSnapshot {
  games: ScheduledSportsGame[];
  verifiedLeagues: Set<ScheduledSportsGame["league"]>;
}

const ESPN_SCOREBOARD_FEEDS: Array<{ league: ScheduledSportsGame["league"]; url: string; durationMs: number }> = [
  { league: "NFL", url: "https://site.api.espn.com/apis/site/v2/sports/football/nfl/scoreboard", durationMs: 3.5 * 60 * 60 * 1000 },
  { league: "NBA", url: "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/scoreboard", durationMs: 3 * 60 * 60 * 1000 },
  { league: "MLB", url: "https://site.api.espn.com/apis/site/v2/sports/baseball/mlb/scoreboard", durationMs: 3.5 * 60 * 60 * 1000 },
  { league: "NHL", url: "https://site.api.espn.com/apis/site/v2/sports/hockey/nhl/scoreboard", durationMs: 3 * 60 * 60 * 1000 },
  { league: "NCAA", url: "https://site.api.espn.com/apis/site/v2/sports/football/college-football/scoreboard", durationMs: 3.5 * 60 * 60 * 1000 }
];

function extractTeamTokens(team: Record<string, unknown> | undefined): string[] {
  if (!team) return [];
  const raw = [
    team.displayName,
    team.shortDisplayName,
    team.name,
    team.location,
    team.abbreviation
  ];
  const tokens = new Set<string>();
  for (const val of raw) {
    const cleaned = String(val || "").trim().toUpperCase();
    if (cleaned.length >= 2) tokens.add(cleaned);
  }
  return Array.from(tokens);
}

export async function fetchEspnSportsSchedules(): Promise<EspnScheduleSnapshot> {
  const verifiedLeagues = new Set<ScheduledSportsGame["league"]>();
  const results = await Promise.allSettled(
    ESPN_SCOREBOARD_FEEDS.map(async (feed) => {
      const controller = new AbortController();
      const timer = setTimeout(() => controller.abort(), 4500);
      try {
        const res = await fetch(feed.url, {
          signal: controller.signal,
          headers: { "user-agent": "GlzHub/2.2" }
        });
        if (!res.ok) return { league: feed.league, ok: false, games: [] as ScheduledSportsGame[] };
        const data = await res.json() as { events?: Array<Record<string, unknown>> };
        const games: ScheduledSportsGame[] = [];
        for (const ev of data.events || []) {
          const dateStr = String(ev.date || "").trim();
          const startMs = Date.parse(dateStr);
          if (!Number.isFinite(startMs)) continue;
          const status = ev.status as { type?: { state?: string } } | undefined;
          const rawState = String(status?.type?.state || "pre").toLowerCase();
          const state: "in" | "pre" | "post" = rawState === "in" ? "in" : rawState === "post" ? "post" : "pre";
          const comp = Array.isArray(ev.competitions) ? ev.competitions[0] as Record<string, unknown> : undefined;
          const competitors = Array.isArray(comp?.competitors) ? comp.competitors as Array<Record<string, unknown>> : [];
          const homeComp = competitors.find((c) => c.homeAway === "home") ?? competitors[0];
          const awayComp = competitors.find((c) => c.homeAway === "away") ?? competitors[1];
          const homeTokens = extractTeamTokens(homeComp?.team as Record<string, unknown> | undefined);
          const awayTokens = extractTeamTokens(awayComp?.team as Record<string, unknown> | undefined);
          if (homeTokens.length === 0 || awayTokens.length === 0) continue;
          const broadcasts: string[] = [];
          if (Array.isArray(comp?.broadcasts)) {
            for (const b of comp.broadcasts as Array<{ names?: string[] }>) {
              if (Array.isArray(b.names)) broadcasts.push(...b.names.map(String));
            }
          }
          games.push({
            league: feed.league,
            gameId: `${feed.league}:${String(ev.id || ev.shortName || startMs)}`,
            shortName: String(ev.shortName || ""),
            name: String(ev.name || ev.shortName || ""),
            awayTokens,
            homeTokens,
            startTime: new Date(startMs).toISOString(),
            endTime: new Date(startMs + feed.durationMs).toISOString(),
            state,
            broadcasts
          });
        }
        return { league: feed.league, ok: true, games };
      } catch {
        return { league: feed.league, ok: false, games: [] as ScheduledSportsGame[] };
      } finally {
        clearTimeout(timer);
      }
    })
  );

  const games: ScheduledSportsGame[] = [];
  for (const r of results) {
    if (r.status === "fulfilled" && r.value.ok) {
      verifiedLeagues.add(r.value.league);
      games.push(...r.value.games);
    }
  }
  return { games, verifiedLeagues };
}

export function isGenericNumberedPlaceholder(title: string): boolean {
  const t = title.trim();
  if (/^\(?((?:MLB|NBA|NFL|NHL|MLS|NCAA|NCAAF|ESPN|UFC|PPV)\s*#?\d{1,4})\)?(\s*\|.*)?$/i.test(t) &&
      !/\b(vs\.?|@)\b/i.test(t)) {
    return true;
  }
  // Reject far-future dummy placeholder timestamps (e.g. "(NBA 13) | (2098-12-31 08:00:01)")
  if (/\b209\d-\d{2}-\d{2}\b/.test(t)) return true;
  // Reject 24/7 linear channels and studio/pregame/highlight shows
  if (/\b24\/7\b/i.test(t)) return true;
  if (/\b(COUNTDOWN|SCOREBOARD|GAMEDAY|GAME\s+DAY|BIG\s+NOON\s+KICKOFF|PREGAME|POSTGAME|STUDIO\s+SHOW|REPLAY|ENCORE)\b/i.test(t)) {
    return true;
  }
  return false;
}

function tokenMatchesText(textUpper: string, token: string): boolean {
  if (token.length <= 3) {
    return new RegExp(`\\b${token.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}\\b`).test(textUpper);
  }
  return textUpper.includes(token);
}

function matchEventToSchedule(event: ParsedEventChannel, schedules: ScheduledSportsGame[]): ScheduledSportsGame | null {
  if (schedules.length === 0) return null;
  // For multi-game bulletin titles like "NHL: PIT vs. DAL • CHI vs. VAN", evaluate the primary matchup only
  const primaryTitle = event.title.split("•")[0].trim();
  const textUpper = `${primaryTitle} ${event.tvgName}`.toUpperCase();
  const league = event.sportLeague.toUpperCase();
  for (const game of schedules) {
    if (league !== "SPORTS" && league !== game.league) continue;
    const awayHit = game.awayTokens.some((tok) => tokenMatchesText(textUpper, tok));
    const homeHit = game.homeTokens.some((tok) => tokenMatchesText(textUpper, tok));
    if (awayHit && homeHit) return game;
  }
  return null;
}

/**
 * Strictly verifies provider live event channels against official ESPN game schedules:
 * - Drops empty numbered/2098 placeholders, 24/7 linear feeds, and studio shows.
 * - For leagues with a live ESPN scoreboard (NFL, NBA, MLB, NHL, NCAA), rejects any
 *   stale or mismatched provider channel that does NOT match an active/upcoming real game.
 * - Drops completed ("post") games and games more than 24h in the future.
 * - Deduplicates alternate arena feeds for the same verified game (keeping one clean entry).
 * - Sorts LIVE NOW ("in") games first, then upcoming ("pre") games chronologically.
 */
export function applyLiveSchedulesToEvents(
  events: ParsedEventChannel[],
  snapshot: EspnScheduleSnapshot | ScheduledSportsGame[],
  nowMs = Date.now()
): ParsedEventChannel[] {
  const schedules = Array.isArray(snapshot) ? snapshot : snapshot.games;
  const verifiedLeagues = Array.isArray(snapshot)
    ? new Set(snapshot.map((g) => g.league))
    : snapshot.verifiedLeagues;

  const enriched: Array<{ item: ParsedEventChannel; priority: number; startMs: number; gameKey: string | null }> = [];
  const staleCutoffMs = nowMs - 6 * 60 * 60 * 1000;
  const futureCutoffMs = nowMs + 24 * 60 * 60 * 1000;

  for (const item of events) {
    if (isGenericNumberedPlaceholder(item.title)) continue;

    const leagueUpper = item.sportLeague.toUpperCase() as ScheduledSportsGame["league"];
    const matched = matchEventToSchedule(item, schedules);

    if (matched) {
      // Drop games ESPN already marked final
      if (matched.state === "post") continue;
      const startMs = Date.parse(matched.startTime);
      // Drop games scheduled more than 24h out so Live Events stays focused on today/tonight
      if (matched.state === "pre" && Number.isFinite(startMs) && startMs > futureCutoffMs) continue;

      enriched.push({
        item: {
          ...item,
          sportLeague: matched.league,
          title: `${matched.league}: ${matched.name}`,
          startTime: matched.startTime,
          endTime: matched.endTime,
          hasInlineTime: true
        },
        priority: matched.state === "in" ? 0 : 1,
        startMs: Number.isFinite(startMs) ? startMs : nowMs,
        gameKey: matched.gameId
      });
    } else {
      // STRICT VERIFICATION: If ESPN gave us a live schedule for this league, reject any
      // unverified/stale provider matchup (e.g. yesterday's game still stuck on nba.011.us)
      if (verifiedLeagues.has(leagueUpper)) continue;

      const startMs = Date.parse(item.startTime);
      // Drop stale dated events from previous days (e.g. Nov 22 leftovers)
      if (item.hasInlineTime && Number.isFinite(startMs) && startMs < staleCutoffMs) continue;
      enriched.push({
        item,
        priority: 2,
        startMs: Number.isFinite(startMs) ? startMs : nowMs,
        gameKey: null
      });
    }
  }

  enriched.sort((a, b) => {
    if (a.priority !== b.priority) return a.priority - b.priority;
    return a.startMs - b.startMs;
  });

  // Deduplicate multiple provider feeds pointing to the exact same verified ESPN game
  const seenGames = new Set<string>();
  const deduped: ParsedEventChannel[] = [];
  for (const entry of enriched) {
    if (entry.gameKey) {
      if (seenGames.has(entry.gameKey)) continue;
      seenGames.add(entry.gameKey);
    }
    deduped.push(entry.item);
  }

  return deduped;
}

async function listEventChannels(request: Request, env: Env): Promise<Response> {
  await adminUser(request, env);
  const rows = await supabaseJson(env,
    "/rest/v1/event_channels?select=*"
  ) as Record<string, unknown>[];
  const visibleRows = (rows || []).filter((event) => event.auto_ingested !== true || isLiveEventChannel(
    String(event.group_title || ""),
    String(event.title || ""),
    String(event.tvg_id || ""),
    String(event.tvg_name || ""),
    String(event.sport_league || "")
  ));
  return json({ events: sortEventChannels(visibleRows) });
}

async function saveIngestedEventChannels(
  env: Env,
  renumbered: ParsedEventChannel[],
  defaultPreBufferHours = 0.5,
  defaultPostBufferHours = 0.5
): Promise<unknown> {
  const safeDefaultPre = clampEventBufferHours(defaultPreBufferHours, 0.5);
  const safeDefaultPost = clampEventBufferHours(defaultPostBufferHours, 0.5);
  const existingRows = await supabaseJson(env,
    "/rest/v1/event_channels?select=id,tvg_id,status,pre_buffer_hours,post_buffer_hours,start_time,end_time,channel_number,auto_ingested"
  ).catch(() => []) as Record<string, unknown>[];

  const existingMap = new Map<string, Record<string, unknown>>();
  if (Array.isArray(existingRows)) {
    for (const row of existingRows) {
      if (row.tvg_id && row.id) existingMap.set(String(row.tvg_id), row);
    }
  }

  const nowIso = new Date().toISOString();
  const toUpdate: { id: string; body: Record<string, unknown> }[] = [];
  const toInsert: Record<string, unknown>[] = [];

  for (const item of renumbered) {
    const existing = existingMap.get(item.tvgId);
    let startTime = item.startTime;
    let endTime = item.endTime;
    let preBufferHours = safeDefaultPre;
    let postBufferHours = safeDefaultPost;
    let status = "active";
    let channelNumber = item.channelNumber;
    let autoIngested = true;

    if (existing) {
      if (typeof existing.status === "string" && existing.status) {
        status = existing.status;
      }
      if (Number.isFinite(Number(existing.pre_buffer_hours))) {
        preBufferHours = Number(existing.pre_buffer_hours);
      }
      if (Number.isFinite(Number(existing.post_buffer_hours))) {
        postBufferHours = Number(existing.post_buffer_hours);
      }
      if (typeof existing.channel_number === "string" && existing.channel_number.trim()) {
        channelNumber = existing.channel_number;
      }
      if (existing.auto_ingested === false) {
        autoIngested = false;
      }

      const existingStartStr = typeof existing.start_time === "string" ? existing.start_time : "";
      const existingEndStr = typeof existing.end_time === "string" ? existing.end_time : "";
      const existingStartMs = existingStartStr ? new Date(existingStartStr).getTime() : NaN;
      const existingEndMs = existingEndStr ? new Date(existingEndStr).getTime() : NaN;
      const parsedEndMs = new Date(item.endTime).getTime();

      if (item.hasInlineTime === false) {
        // Branch 3 (no inline time): preserve existing start/end so cron does not roll window forward indefinitely.
        if (Number.isFinite(existingStartMs)) startTime = existingStartStr;
        if (Number.isFinite(existingEndMs)) endTime = existingEndStr;
      } else if (Number.isFinite(existingEndMs)) {
        const hasCustomDuration = Number.isFinite(existingStartMs) &&
          Math.abs((existingEndMs - existingStartMs) - DEFAULT_EVENT_DURATION_MS) > 60_000;
        if (hasCustomDuration || (Number.isFinite(parsedEndMs) && existingEndMs > parsedEndMs)) {
          endTime = existingEndStr;
        }
      }
    }

    const payload = {
      tvg_id: item.tvgId,
      tvg_name: item.tvgName,
      title: item.title,
      sport_league: item.sportLeague,
      group_title: item.groupTitle,
      logo_url: item.logoUrl,
      stream_url: item.streamUrl,
      channel_number: channelNumber,
      start_time: startTime,
      end_time: endTime,
      pre_buffer_hours: preBufferHours,
      post_buffer_hours: postBufferHours,
      status,
      auto_ingested: autoIngested,
      updated_at: nowIso
    };

    if (existing?.id) {
      toUpdate.push({ id: String(existing.id), body: payload });
    } else {
      toInsert.push(payload);
    }
  }

  // 1. Try fast PostgREST bulk upsert (if unique constraint exists)
  const fullPayload = [...toUpdate.map(u => u.body), ...toInsert];
  try {
    return await supabaseJson(env, "/rest/v1/event_channels?on_conflict=tvg_id&select=*", {
      method: "POST",
      headers: { prefer: "resolution=merge-duplicates,return=representation" },
      body: JSON.stringify(fullPayload)
    });
  } catch (_err) {
    // 2. Fail-safe fallback: perform explicit PATCH for existing rows and POST for new rows
    const results: unknown[] = [];
    if (toUpdate.length > 0) {
      await Promise.all(toUpdate.map(async ({ id, body }) => {
        const res = await supabaseJson(env, `/rest/v1/event_channels?id=eq.${id}&select=*`, {
          method: "PATCH",
          headers: { prefer: "return=representation" },
          body: JSON.stringify(body)
        }).catch(() => null);
        if (res && Array.isArray(res)) results.push(...res);
      }));
    }
    if (toInsert.length > 0) {
      const res = await supabaseJson(env, "/rest/v1/event_channels?select=*", {
        method: "POST",
        headers: { prefer: "return=representation" },
        body: JSON.stringify(toInsert)
      }).catch(() => null);
      if (res && Array.isArray(res)) results.push(...res);
    }
    return results;
  }
}

async function pruneAutoIngestedEventChannels(env: Env, retainedTvgIds: Set<string>): Promise<void> {
  const existingRows = await supabaseJson(env,
    "/rest/v1/event_channels?auto_ingested=eq.true&select=id,tvg_id"
  ).catch(() => []) as Record<string, unknown>[];

  const staleIds = existingRows
    .filter((row) => !retainedTvgIds.has(String(row.tvg_id || "")))
    .map((row) => String(row.id || ""))
    .filter(Boolean);

  await Promise.all(staleIds.map((id) =>
    supabaseJson(env, `/rest/v1/event_channels?id=eq.${encodeURIComponent(id)}`, {
      method: "DELETE"
    }).catch((error) => console.error(`Could not prune stale event channel ${id}:`, error))
  ));
}

async function ingestEventChannels(request: Request, env: Env): Promise<Response> {
  await adminUser(request, env);
  try {
    const bodyData = await request.json().catch(() => ({})) as Record<string, unknown>;
    const providerUrl = typeof bodyData.url === "string" && bodyData.url.startsWith("http")
      ? bodyData.url
      : "https://starlite.best/api/list/mygbb8/167848";
    const defaultPreBufferHours = clampEventBufferHours(bodyData.preBufferHours, 0.5);
    const defaultPostBufferHours = clampEventBufferHours(bodyData.postBufferHours, 0.5);

    const response = await fetch(providerUrl, {
      headers: {
        "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
      }
    });
    if (!response.ok) throw new Error(`Provider feed returned HTTP status ${response.status}`);
    const text = await response.text();
    const parsed = parseConMeM3u(text);
    const scheduleSnapshot = await fetchEspnSportsSchedules().catch(() => ({ games: [], verifiedLeagues: new Set<ScheduledSportsGame["league"]>() }));
    const scheduled = applyLiveSchedulesToEvents(parsed, scheduleSnapshot);

    if (scheduled.length === 0) {
      await pruneAutoIngestedEventChannels(env, new Set());
      await pushConfigUpdateToAllDevices(env);
      return json({ ok: true, ingestedCount: 0, scheduleGamesFound: scheduleSnapshot.games.length, events: [] });
    }

    // Deduplicate by tvgId in memory to prevent Postgres payload conflict errors
    const uniqueMap = new Map<string, typeof scheduled[0]>();
    for (const item of scheduled) {
      if (!uniqueMap.has(item.tvgId)) {
        uniqueMap.set(item.tvgId, item);
      }
    }
    const deduplicated = Array.from(uniqueMap.values());
    const ranges = await loadEventChannelRanges(env);
    const renumbered = assignCategoryChannelNumbers(deduplicated, ranges);

    const result = await saveIngestedEventChannels(env, renumbered, defaultPreBufferHours, defaultPostBufferHours);
    await pruneAutoIngestedEventChannels(env, new Set(renumbered.map((item) => item.tvgId)));
    await pushConfigUpdateToAllDevices(env);
    return json({ ok: true, ingestedCount: deduplicated.length, scheduleGamesFound: scheduleSnapshot.games.length, events: result });
  } catch (err) {
    return json({ ok: false, error: (err as Error).message }, 400);
  }
}

export function parseAllProviderStreams(m3uText: string): ParsedEventChannel[] {
  const lines = m3uText.split(/\r?\n/);
  const events: ParsedEventChannel[] = [];
  let pendingExtInf: string | null = null;

  for (const raw of lines) {
    const line = raw.trim();
    if (!line) continue;
    if (line.startsWith("#EXTINF:")) {
      pendingExtInf = line;
      continue;
    }
    if (line.startsWith("#") || !pendingExtInf) continue;

    const streamUrl = line;
    const tvgIdMatch = pendingExtInf.match(/tvg-id="([^"]+)"/i);
    const tvgNameMatch = pendingExtInf.match(/tvg-name="([^"]+)"/i);
    const groupTitleMatch = pendingExtInf.match(/group-title="([^"]+)"/i);
    const tvgLogoMatch = pendingExtInf.match(/tvg-logo="([^"]+)"/i);
    const title = extractExtInfTitle(pendingExtInf);

    const tvgId = tvgIdMatch ? tvgIdMatch[1] : `event.${events.length + 1}`;
    const tvgName = tvgNameMatch ? tvgNameMatch[1] : tvgId;
    const groupTitle = groupTitleMatch ? groupTitleMatch[1] : "Major League Sports (Events)";
    const scrapedLogoUrl = tvgLogoMatch ? tvgLogoMatch[1] : "";

    const fullText = `${groupTitle} ${title} ${tvgId} ${tvgName}`.toUpperCase();
    const isGerman = /\b(DE|DEUTSCHLAND|GERMANY)\b/i.test(fullText) || /^(DE|GER):/i.test(title);
    if (isGerman || isDuplicateMainSportsNetwork(groupTitle, title, tvgId, tvgName)) {
      pendingExtInf = null;
      continue;
    }

    const parsedTimes = parseInlineEventTime(title);
    const sportLeague = detectSportLeague(tvgId, groupTitle, title);
    const logoUrl = resolveEventLogoUrl(sportLeague, title, tvgName) ?? scrapedLogoUrl;

    events.push({
      tvgId,
      tvgName,
      title,
      sportLeague,
      groupTitle,
      logoUrl,
      streamUrl,
      channelNumber: "",
      startTime: parsedTimes.startTime.toISOString(),
      endTime: parsedTimes.endTime.toISOString(),
      hasInlineTime: parsedTimes.hasInlineTime
    });

    pendingExtInf = null;
  }

  return assignCategoryChannelNumbers(events);
}

async function fetchProviderFeed(request: Request, env: Env): Promise<Response> {
  await adminUser(request, env);
  const response = await fetch("https://starlite.best/api/list/mygbb8/167848", {
    headers: {
      "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
  });
  if (!response.ok) throw new Error(`Provider feed returned HTTP status ${response.status}`);
  const text = await response.text();
  const allParsed = parseAllProviderStreams(text);

  const existing = await supabaseJson(env, "/rest/v1/event_channels?select=tvg_id").catch(() => []) as Record<string, unknown>[];
  const existingSet = new Set((existing || []).map(row => String(row.tvg_id)));

  const streams = allParsed.map(item => ({
    ...item,
    alreadyInjected: existingSet.has(item.tvgId)
  }));

  return json({ ok: true, count: streams.length, streams });
}

async function injectSingleStream(request: Request, env: Env): Promise<Response> {
  await adminUser(request, env);
  const input = await body(request);
  const stream = input.stream as ParsedEventChannel;
  if (!stream || !stream.title || !stream.streamUrl) {
    throw new Error("Invalid stream payload");
  }

  if (!stream.channelNumber) {
    const ranges = await loadEventChannelRanges(env);
    const tier = getEventChannelTier(stream.sportLeague || "SPORTS", stream.title, stream.groupTitle || "", stream.tvgId || "");
    const base = tier === "mlb" ? ranges.mlb : tier === "major" ? ranges.major : ranges.ppv;
    stream.channelNumber = formatEventChannelNumber(base, 0, ranges.mode);
  }

  const preBufferHours = clampEventBufferHours(input.preBufferHours, 0.5);
  const postBufferHours = clampEventBufferHours(input.postBufferHours, 0.5);

  const payload = {
    tvg_id: stream.tvgId,
    title: stream.title,
    sport_league: stream.sportLeague || "SPORTS",
    group_title: stream.groupTitle || "Major League Sports (Events)",
    logo_url: stream.logoUrl || null,
    stream_url: stream.streamUrl,
    channel_number: stream.channelNumber,
    start_time: stream.startTime || new Date().toISOString(),
    end_time: stream.endTime || new Date(Date.now() + 3 * 3600_000).toISOString(),
    pre_buffer_hours: preBufferHours,
    post_buffer_hours: postBufferHours,
    status: "active",
    auto_ingested: false,
    is_online: true,
    updated_at: new Date().toISOString()
  };

  const rows = await supabaseJson(env, "/rest/v1/event_channels?on_conflict=tvg_id&select=*", {
    method: "POST",
    headers: { prefer: "resolution=merge-duplicates,return=representation" },
    body: JSON.stringify([payload])
  }).catch(async () => {
    return await supabaseJson(env, "/rest/v1/event_channels?select=*", {
      method: "POST",
      headers: { prefer: "return=representation" },
      body: JSON.stringify([payload])
    });
  }) as Record<string, unknown>[];

  await pushConfigUpdateToAllDevices(env);
  return json({ ok: true, event: rows[0] || payload });
}

async function createEventChannel(request: Request, env: Env): Promise<Response> {
  await adminUser(request, env);
  const input = await body(request);
  const title = requiredString(input.title, "title", 180);
  const streamUrl = requiredString(input.streamUrl, "streamUrl", 2048);
  const backupStreamUrl = optionalString(input.backupStreamUrl, "backupStreamUrl", 2048);
  const tvgId = optionalString(input.tvgId, "tvgId", 120) || `custom.${Date.now()}`;
  const sportLeague = optionalString(input.sportLeague, "sportLeague", 40) || "SPORTS";
  const groupTitle = optionalString(input.groupTitle, "groupTitle", 120) || "Major League Sports (Events)";
  const logoUrl = optionalString(input.logoUrl, "logoUrl", 2048);
  let channelNumber = optionalString(input.channelNumber, "channelNumber", 20);
  if (!channelNumber) {
    const ranges = await loadEventChannelRanges(env);
    const tier = getEventChannelTier(sportLeague, title, groupTitle, tvgId);
    const base = tier === "mlb" ? ranges.mlb : tier === "major" ? ranges.major : ranges.ppv;
    channelNumber = formatEventChannelNumber(base, 0, ranges.mode);
  }
  const startTime = optionalString(input.startTime, "startTime", 60) || new Date().toISOString();
  const endTime = optionalString(input.endTime, "endTime", 60) || new Date(Date.now() + 3 * 3600_000).toISOString();
  const preBufferHours = clampEventBufferHours(input.preBufferHours, 0.5);
  const postBufferHours = clampEventBufferHours(input.postBufferHours, 0.5);

  const insertBody: Record<string, unknown> = {
    tvg_id: tvgId,
    title,
    sport_league: sportLeague,
    group_title: groupTitle,
    logo_url: logoUrl,
    stream_url: streamUrl,
    channel_number: channelNumber,
    start_time: startTime,
    end_time: endTime,
    pre_buffer_hours: preBufferHours,
    post_buffer_hours: postBufferHours,
    status: "active",
    auto_ingested: false,
    updated_at: new Date().toISOString()
  };
  if (backupStreamUrl) insertBody.backup_stream_url = backupStreamUrl;

  const rows = await supabaseJson(env, "/rest/v1/event_channels?select=*", {
    method: "POST",
    headers: { prefer: "return=representation" },
    body: JSON.stringify(insertBody)
  }).catch(async (err) => {
    if (!("backup_stream_url" in insertBody)) throw err;
    const { backup_stream_url: _omitted, ...fallbackBody } = insertBody;
    return await supabaseJson(env, "/rest/v1/event_channels?select=*", {
      method: "POST",
      headers: { prefer: "return=representation" },
      body: JSON.stringify(fallbackBody)
    });
  }) as Record<string, unknown>[];

  await pushConfigUpdateToAllDevices(env);
  return json({ event: rows[0] }, 201);
}

async function updateEventChannel(request: Request, env: Env, eventId: string): Promise<Response> {
  await adminUser(request, env);
  const input = await body(request);
  const updates: Record<string, unknown> = { updated_at: new Date().toISOString() };
  if (typeof input.title === "string") updates.title = input.title.trim();
  if (typeof input.channelNumber === "string") updates.channel_number = input.channelNumber.trim();
  if (typeof input.sportLeague === "string") updates.sport_league = input.sportLeague.trim() || "SPORTS";
  if (typeof input.groupTitle === "string") updates.group_title = input.groupTitle.trim() || "Major League Sports (Events)";
  if (typeof input.streamUrl === "string") updates.stream_url = requiredString(input.streamUrl, "streamUrl", 2048);
  if (input.backupStreamUrl !== undefined) updates.backup_stream_url = optionalString(input.backupStreamUrl, "backupStreamUrl", 2048);
  if (input.logoUrl !== undefined) updates.logo_url = optionalString(input.logoUrl, "logoUrl", 2048);
  if (typeof input.startTime === "string") updates.start_time = input.startTime;
  if (typeof input.endTime === "string") updates.end_time = input.endTime;
  if (typeof input.preBufferHours === "number") updates.pre_buffer_hours = clampEventBufferHours(input.preBufferHours, 0.5);
  if (typeof input.postBufferHours === "number") updates.post_buffer_hours = clampEventBufferHours(input.postBufferHours, 0.5);
  if (typeof input.status === "string" && ["scheduled", "active", "expired", "disabled"].includes(input.status)) {
    updates.status = input.status;
  }

  const rows = await supabaseJson(env,
    `/rest/v1/event_channels?id=eq.${encodeURIComponent(eventId)}&select=*`,
    {
      method: "PATCH",
      headers: { prefer: "return=representation" },
      body: JSON.stringify(updates)
    }
  ).catch(async (err) => {
    if (!("backup_stream_url" in updates)) throw err;
    const { backup_stream_url: _omitted, ...fallbackUpdates } = updates;
    return await supabaseJson(env,
      `/rest/v1/event_channels?id=eq.${encodeURIComponent(eventId)}&select=*`,
      {
        method: "PATCH",
        headers: { prefer: "return=representation" },
        body: JSON.stringify(fallbackUpdates)
      }
    );
  }) as Record<string, unknown>[];

  await pushConfigUpdateToAllDevices(env);
  return json({ event: rows[0] });
}

async function deleteEventChannel(request: Request, env: Env, eventId: string): Promise<Response> {
  await adminUser(request, env);
  await supabaseJson(env, `/rest/v1/event_channels?id=eq.${encodeURIComponent(eventId)}`, {
    method: "DELETE"
  });
  await pushConfigUpdateToAllDevices(env);
  return json({ ok: true });
}

function formatExternalEventTitle(event: Record<string, unknown>): string {
  const rawTitle = String(event.title || "Live Sports Event").replace(/[\r\n]+/g, " ").trim();
  const league = String(event.sport_league || "").replace(/[\r\n]+/g, " ").trim();
  if (!league) return rawTitle;
  const upperTitle = rawTitle.toUpperCase();
  const upperLeague = league.toUpperCase();
  if (upperTitle.startsWith(upperLeague)) return rawTitle;
  return `${league}: ${rawTitle}`;
}

function buildM3uDocument(options: {
  playlists: Record<string, unknown>[];
  eventChannels: Record<string, unknown>[] | null;
  includeEvents?: boolean;
  externalEventTitles?: boolean;
  fallbackEpgUrl?: string;
  shouldIncludeItem?: (item: Record<string, unknown>) => boolean;
}): { m3uContent: string; channelCount: number; activeEventsCount: number; headerEpgUrl: string } {
  const {
    playlists,
    eventChannels,
    includeEvents = true,
    externalEventTitles = false,
    fallbackEpgUrl = "https://play.glztech.com/epg.xml.gz",
    shouldIncludeItem
  } = options;
  const cleanText = (value: unknown): string => String(value ?? "").replace(/[\r\n]+/g, " ").trim();
  const cleanAttribute = (value: unknown): string => cleanText(value).replace(/"/g, "'");

  const firstPlaylistEpg = playlists.find((pl) => typeof pl.epg_url === "string" && pl.epg_url.trim())?.epg_url;
  const headerEpgUrl = typeof firstPlaylistEpg === "string" && firstPlaylistEpg.trim()
    ? cleanAttribute(firstPlaylistEpg)
    : cleanAttribute(fallbackEpgUrl);
  let m3uContent = `#EXTM3U x-tvg-url="${headerEpgUrl}"\n\n`;
  let channelCount = 0;

  const parsePlaylistChannelNumber = (raw: unknown): [number, number] | null => {
    const str = String(raw ?? "").trim();
    if (!str) return null;
    const match = str.match(/^(\d+)(?:\s*[-._:]\s*(\d+))?/);
    if (!match) return null;
    const major = parseInt(match[1], 10);
    const minor = match[2] !== undefined ? parseInt(match[2], 10) : -1;
    return [major, minor];
  };
  const comparePlaylistItemsByChannelNumber = (a: Record<string, unknown>, b: Record<string, unknown>): number => {
    const metaA = (a.metadata && typeof a.metadata === "object") ? a.metadata as Record<string, unknown> : {};
    const metaB = (b.metadata && typeof b.metadata === "object") ? b.metadata as Record<string, unknown> : {};
    const keyA = parsePlaylistChannelNumber(metaA.tvg_chno ?? metaA.tvgChno);
    const keyB = parsePlaylistChannelNumber(metaB.tvg_chno ?? metaB.tvgChno);
    if (keyA && keyB) {
      if (keyA[0] !== keyB[0]) return keyA[0] - keyB[0];
      if (keyA[1] !== keyB[1]) return keyA[1] - keyB[1];
    } else if (keyA) {
      return -1;
    } else if (keyB) {
      return 1;
    }
    const posDiff = Number(a.position || 0) - Number(b.position || 0);
    if (posDiff !== 0) return posDiff;
    return String(a.title || "").localeCompare(String(b.title || ""), undefined, { numeric: true, sensitivity: "base" });
  };

  for (const pl of playlists) {
    const groupTitle = cleanAttribute(pl.title || "TV");
    const items = [...((pl.playlist_items as Record<string, unknown>[]) || [])];
    items.sort(comparePlaylistItemsByChannelNumber);

    for (const item of items) {
      const title = cleanText(item.title || "Untitled Channel");
      const duration = Number(item.duration_seconds || -1);
      const mediaUrl = String(item.media_url || "");
      const metadata = (item.metadata && typeof item.metadata === "object") ? item.metadata as Record<string, unknown> : {};
      if (metadata.hidden === true || metadata.hidden === "true") continue;
      if (shouldIncludeItem && !shouldIncludeItem(item)) continue;

      const tvgId = metadata.tvg_id || metadata.tvgId ? ` tvg-id="${cleanAttribute(metadata.tvg_id || metadata.tvgId)}"` : "";
      const tvgChno = metadata.tvg_chno || metadata.tvgChno ? ` tvg-chno="${cleanAttribute(metadata.tvg_chno || metadata.tvgChno)}"` : "";
      const callsignVal = cleanAttribute(metadata.callsign || metadata.callSign || metadata.x_callsign || "");
      const xCallsign = callsignVal ? ` x-callsign="${callsignVal}"` : "";
      const tvgLogo = metadata.tvg_logo || metadata.tvgLogo || pl.artwork_url ? ` tvg-logo="${cleanAttribute(metadata.tvg_logo || metadata.tvgLogo || pl.artwork_url)}"` : "";
      const isRadio = metadata.radio === true || metadata.radio === "true" ? ' radio="true"' : "";

      const channelGroup = cleanAttribute(metadata.group || groupTitle);
      m3uContent += `#EXTINF:${duration}${tvgId}${tvgChno}${xCallsign}${tvgLogo}${isRadio} group-title="${channelGroup}",${title}\n${mediaUrl}\n\n`;
      channelCount++;
    }
  }

  let activeEventsCount = 0;
  // Inject active temporary event channels (within start - pre_buffer to end + post_buffer)
  if (includeEvents && Array.isArray(eventChannels)) {
    const sortedEvents = sortEventChannels(eventChannels);
    const now = Date.now();
    let activeIndex = 1;
    for (const event of sortedEvents) {
      if (!isEventActiveAndValid(event, now)) continue;
      if (event.auto_ingested === true && !isLiveEventChannel(
        String(event.group_title || ""),
        String(event.title || ""),
        String(event.tvg_id || ""),
        String(event.tvg_name || ""),
        String(event.sport_league || "")
      )) continue;
      const { url: mediaUrl } = resolveEventDeliveryStreamUrl(event);
      if (!mediaUrl) continue;
      const channelDisplayName = externalEventTitles
        ? cleanText(formatExternalEventTitle(event))
        : "SPORTS PPV";
      const tvgIdVal = String(event.tvg_id || `event.channel.${activeIndex}`);
      const tvgId = ` tvg-id="${cleanAttribute(tvgIdVal)}"`;
      const tvgName = ` tvg-name="${cleanAttribute(channelDisplayName)}"`;
      const chnoValue = String(event.channel_number || `30-${String(activeIndex).padStart(2, '0')}`);
      const tvgChno = ` tvg-chno="${cleanAttribute(chnoValue)}"`;
      const tvgLogo = event.logo_url ? ` tvg-logo="${cleanAttribute(event.logo_url)}"` : "";
      const channelGroup = cleanAttribute(event.group_title || "Major League Sports (Events)");
      const eventTitleAttr = ` x-event-title="${cleanAttribute(event.title || "Live Sports Event")}"`;
      const eventLeagueAttr = ` x-event-league="${cleanAttribute(event.sport_league || "SPORTS")}"`;
      const { startMs: gameStartMs, endMs: gameEndMs } = resolveEventWindow(event, now);
      const eventStartAttr = ` x-event-start="${Math.round(gameStartMs)}"`;
      const eventEndAttr = ` x-event-end="${Math.round(gameEndMs)}"`;
      m3uContent += `#EXTINF:-1${tvgId}${tvgName}${tvgChno}${tvgLogo}${eventTitleAttr}${eventLeagueAttr}${eventStartAttr}${eventEndAttr} group-title="${channelGroup}",${channelDisplayName}\n${mediaUrl}\n\n`;
      activeIndex++;
      activeEventsCount++;
    }
  }

  return { m3uContent, channelCount, activeEventsCount, headerEpgUrl };
}

async function getDeviceM3UPlaylist(request: Request, env: Env): Promise<Response> {
  const device = await deviceForToken(request, env);

  let playlistQuery = `/rest/v1/playlists?owner_id=eq.${device.owner_id}&target_app=in.(tv,both)&is_published=eq.true&select=*,playlist_items(*)&order=created_at.asc`;
  let group: Record<string, unknown> | null = null;
  if (device.box_group_id) {
    const groups = await supabaseJson(env, `/rest/v1/box_groups?id=eq.${encodeURIComponent(String(device.box_group_id))}&owner_id=eq.${device.owner_id}&select=id,playlist_id,default_channel_policy`) as Record<string, unknown>[];
    group = groups[0] ?? null;
  }
  const effectivePlaylistId = device.assigned_playlist_id || group?.playlist_id;
  const useGroupPolicy = Boolean(group && (!device.assigned_playlist_id || String(device.assigned_playlist_id) === String(group.playlist_id)));
  if (effectivePlaylistId) {
    playlistQuery = `/rest/v1/playlists?id=eq.${encodeURIComponent(String(effectivePlaylistId))}&owner_id=eq.${device.owner_id}&select=*,playlist_items(*)`;
  }

  const targetFilters = [`and(target_type.eq.device,target_id.eq.${device.id})`];
  if (group?.id) targetFilters.push(`and(target_type.eq.group,target_id.eq.${group.id})`);
  const [playlists, rules, eventChannels] = await Promise.all([
    supabaseJson(env, playlistQuery) as Promise<Record<string, unknown>[]>,
    supabaseJson(env,
      `/rest/v1/channel_policy_rules?owner_id=eq.${device.owner_id}&or=(${targetFilters.join(',')})&select=target_type,playlist_item_id,decision`
    ) as Promise<Record<string, unknown>[]>,
    supabaseJson(env,
      `/rest/v1/event_channels?status=neq.disabled&select=*`
    ).catch(() => null) as Promise<Record<string, unknown>[] | null>
  ]);
  const groupRules = new Map(rules.filter((rule) => rule.target_type === 'group').map((rule) => [String(rule.playlist_item_id), String(rule.decision)]));
  const deviceRules = new Map(rules.filter((rule) => rule.target_type === 'device').map((rule) => [String(rule.playlist_item_id), String(rule.decision)]));
  const deviceMode = String(device.channel_policy_mode || 'inherit');

  const { m3uContent } = buildM3uDocument({
    playlists,
    eventChannels,
    includeEvents: true,
    externalEventTitles: false,
    shouldIncludeItem: (item) => {
      const decision = deviceRules.get(String(item.id))
        ?? (deviceMode !== 'inherit' ? deviceMode : undefined)
        ?? (useGroupPolicy ? groupRules.get(String(item.id)) : undefined)
        ?? String(useGroupPolicy ? (group?.default_channel_policy || 'allow') : 'allow');
      return decision !== 'block';
    }
  });

  const etag = `"${await sha256(m3uContent)}"`;
  if (request.headers.get("if-none-match") === etag) {
    return new Response(null, {
      status: 304,
      headers: {
        "content-type": "audio/x-mpegurl; charset=utf-8",
        "etag": etag,
        "cache-control": "private, no-cache"
      }
    });
  }

  return new Response(m3uContent, {
    status: 200,
    headers: {
      "content-type": "audio/x-mpegurl; charset=utf-8",
      "etag": etag,
      "cache-control": "private, no-cache"
    }
  });
}

async function resolveExternalExportData(
  env: Env,
  origin: string,
  ownerId: string,
  playlistId: string | null,
  includeEvents: boolean
) {
  const playlistQuery = playlistId
    ? `/rest/v1/playlists?id=eq.${encodeURIComponent(playlistId)}&owner_id=eq.${encodeURIComponent(ownerId)}&select=*,playlist_items(*)`
    : `/rest/v1/playlists?owner_id=eq.${encodeURIComponent(ownerId)}&target_app=in.(tv,both)&is_published=eq.true&select=*,playlist_items(*)&order=created_at.asc`;

  const [playlists, eventChannels, guides] = await Promise.all([
    supabaseJson(env, playlistQuery) as Promise<Record<string, unknown>[]>,
    includeEvents
      ? (supabaseJson(env, `/rest/v1/event_channels?status=neq.disabled&select=*`).catch(() => null) as Promise<Record<string, unknown>[] | null>)
      : Promise.resolve(null),
    supabaseJson(env, `/rest/v1/epg_guides?owner_id=eq.${encodeURIComponent(ownerId)}&select=playlist_id`).catch(() => []) as Promise<Record<string, unknown>[]>
  ]);

  const managedGuideIds = new Set((guides || []).map((g) => String(g.playlist_id || "")));
  const firstManagedPlaylist = playlists.find((pl) => managedGuideIds.has(String(pl.id || "")));
  const fallbackEpgUrl = firstManagedPlaylist
    ? `${origin}/api/v1/guides/${encodeURIComponent(String(firstManagedPlaylist.id))}.xml.gz`
    : "https://play.glztech.com/epg.xml.gz";

  const built = buildM3uDocument({
    playlists,
    eventChannels,
    includeEvents,
    externalEventTitles: true,
    fallbackEpgUrl
  });

  return { playlists, ...built };
}

async function createExternalM3uUrl(request: Request, env: Env): Promise<Response> {
  const user = await adminUser(request, env);
  const input = await body(request).catch(() => ({} as Record<string, unknown>));
  const playlistId = optionalString(input.playlistId ?? input.playlist_id, "playlistId", 80);
  const includeEvents = input.includeEvents !== false;

  let playlistTitle = "All Published TV Playlists";
  if (playlistId) {
    const owned = await ownedPlaylist(request, env, playlistId);
    playlistTitle = String(owned.playlist.title || "TV Playlist");
  }

  const origin = new URL(request.url).origin;
  const token = await signExportM3uToken(env, {
    v: 1,
    ownerId: user.id,
    playlistId,
    includeEvents
  });
  const data = await resolveExternalExportData(env, origin, user.id, playlistId, includeEvents);

  return json({
    url: `${origin}/api/v1/export/m3u/${token}.m3u`,
    epgUrl: data.headerEpgUrl,
    scope: playlistId ? "playlist" : "working",
    playlistId,
    playlistTitle,
    includeEvents,
    channelCount: data.channelCount,
    activeEventsCount: data.activeEventsCount
  });
}

async function getExternalM3UPlaylist(request: Request, env: Env, token: string): Promise<Response> {
  const payload = await verifyExportM3uToken(env, token);
  if (!payload) return json({ error: "Invalid or expired M3U export URL." }, 404);

  const origin = new URL(request.url).origin;
  const { playlists, m3uContent } = await resolveExternalExportData(
    env,
    origin,
    payload.ownerId,
    payload.playlistId,
    payload.includeEvents
  );
  if (payload.playlistId && playlists.length === 0) {
    return json({ error: "Playlist not found." }, 404);
  }

  const filename = payload.playlistId && playlists[0]?.title
    ? `${String(playlists[0].title).replace(/[^a-z0-9]+/gi, "-").replace(/^-|-$/g, "").toLowerCase() || "glz-tv"}-live.m3u`
    : "glz-tv-live.m3u";
  const etag = `"${await sha256(m3uContent)}"`;
  const headers: Record<string, string> = {
    "content-type": "audio/x-mpegurl; charset=utf-8",
    "content-disposition": `inline; filename="${filename}"`,
    "etag": etag,
    "cache-control": "no-cache",
    "access-control-allow-origin": "*"
  };

  if (request.headers.get("if-none-match") === etag) {
    return new Response(null, { status: 304, headers });
  }
  if (request.method === "HEAD") {
    return new Response(null, { status: 200, headers });
  }
  return new Response(m3uContent, { status: 200, headers });
}

// Return a bounded JSON guide instead of making older Roku TVs inflate XMLTV.
async function getRokuGuide(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
  const device = await deviceForToken(request, env);
  // Reuse the exact managed lineup so device/group policies apply to guide access.
  const m3uText = await (await getDeviceM3UPlaylist(request, env)).text();
  const lineup = lineupChannels(m3uText);
  const selected = new URL(request.url).searchParams.get("channel") || "";
  if (selected && !lineup.some(c => c.id === selected)) return json({ error: "Channel not assigned to this Roku." }, 404);
  const { epgUrl } = await deviceSources(request, env, device);
  const m3uEpgUrl = /#EXTM3U[^\r\n]*?\bx-tvg-url=["']([^"']+)["']/i.exec(m3uText)?.[1]?.trim() || "";
  // Android uses this same config value, then its built-in default when unset.
  const sourceUrl = typeof epgUrl === "string" && epgUrl.trim()
    ? epgUrl.trim()
    : (m3uEpgUrl || "https://play.glztech.com/epg.xml.gz");
  const urls = [...new Set(sourceUrl.split(",").map(u => u.trim()).filter(u => /^https?:\/\//i.test(u)))].slice(0, 4);
  if (!urls.length) urls.push("https://play.glztech.com/epg.xml.gz");
  let xml = "", failures = 0;
  // Cache decompressed guides briefly; no device bearer token is sent upstream.
  const sourceHeaders = device.request_headers && typeof device.request_headers === "object" ? device.request_headers as Record<string,string> : {};
  for (const url of urls) {
    const cacheId = await sha256(url + JSON.stringify(sourceHeaders));
    const cacheKey = new Request(`${new URL(request.url).origin}/__internal/roku-guide/${cacheId}`);
    try {
      const cached = await caches.default.match(cacheKey);
      let part = "";
      if (cached) {
        part = await cached.text();
      } else {
        const local = new URL(url).origin === new URL(request.url).origin ? new URL(url).pathname.match(/^\/api\/v1\/guides\/([0-9a-f-]+)\.xml(?:\.gz)?$/i) : null;
        const source = local ? await exportManagedGuide(request, env, ctx, local[1], false, false, String(device.owner_id || "")) : await fetch(url, { headers: { ...sourceHeaders, "user-agent":"GlzHub-RokuGuide/1.0", accept:"application/xml,text/xml,application/gzip,*/*" }, signal: AbortSignal.timeout(15000) });
        if (!source.ok) throw new Error("Guide unavailable");
        part = await xmlTextFromResponse(source);
        if (part.length + xml.length > MAX_XMLTV_BYTES || !/<tv(?:\s|>)/i.test(part)) throw new Error("Invalid guide");
        ctx.waitUntil(caches.default.put(cacheKey, new Response(part, {headers:{"content-type":"application/xml", "cache-control":"max-age=120"}})).catch(()=>undefined));
      }
      if (part.length + xml.length > MAX_XMLTV_BYTES) throw new Error("Guide too large");
      xml += part;
    } catch { failures++; }
  }
  const guide = projectGuide(xml,lineup,Math.floor(Date.now()/1000),selected);
  const guideError = [guide.guideError, failures ? "Some EPG sources could not load. Check the playlist's guide URL and try Refresh." : null].filter(Boolean).join(" ") || null;
  return json({ ...guide, guideError });
}

async function getDeviceRadioStreams(request: Request, env: Env): Promise<Response> {
  const device = await deviceForToken(request, env);
  const rows = await supabaseJson(
    env,
    `/rest/v1/radio_stations?owner_id=eq.${device.owner_id}&is_active=eq.true&select=station_code,name,genre,stream_url,logo_url,epg_channel_id,bitrate,request_headers&order=station_code.asc`
  ) as Record<string, unknown>[];
  return radioCatalogResponse(request, rows.map(publicRadioStation), "private, no-cache");
}

async function route(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
  const url = new URL(request.url);
  const path = url.pathname;

  if (path === "/api/health") return json({ ok: true, service: "glzhub" });
  if (path === "/api/v1/radio/stations" && request.method === "GET") return publicRadioCatalog(request, env);
  const publicGuide = path.match(/^\/api\/v1\/guides\/([0-9a-f-]+)\.xml(\.gz)?$/i);
  if (publicGuide && request.method === "GET") return exportManagedGuide(request, env, ctx, publicGuide[1], Boolean(publicGuide[2]));
  const externalM3u = path.match(/^\/api\/v1\/export\/m3u\/([A-Za-z0-9_-]+\.[A-Za-z0-9_-]+)(?:\.m3u8?)?$/i);
  if (externalM3u && (request.method === "GET" || request.method === "HEAD")) {
    return getExternalM3UPlaylist(request, env, externalM3u[1]);
  }
  if (path === "/api/v1/public-config") {
    return json({ supabaseUrl: env.SUPABASE_URL, publishableKey: env.SUPABASE_PUBLISHABLE_KEY });
  }
  if (path === "/api/v1/enrollment" && request.method === "POST") return enroll(request, env);
  if (path === "/api/v1/enrollment/claim" && request.method === "POST") return claimEnrollment(request, env);
  if (path === "/api/v1/admin/enrollments" && request.method === "GET") return listNearbyEnrollments(request, env);
  if (path === "/api/v1/admin/devices" && request.method === "GET") return listDevices(request, env);
  if (path === "/api/v1/admin/devices/sync-all" && request.method === "POST") return syncAllDevices(request, env);
  if (path === "/api/v1/admin/box-groups" && request.method === "GET") return listBoxGroups(request, env);
  if (path === "/api/v1/admin/box-groups" && request.method === "POST") return createBoxGroup(request, env);
  if (path === "/api/v1/admin/sites" && request.method === "GET") return listSites(request, env);
  if (path === "/api/v1/admin/sites" && request.method === "POST") return createSite(request, env);
  if (path === "/api/v1/admin/apps" && request.method === "GET") return listApps(request, env);
  if (path === "/api/v1/admin/apps" && request.method === "POST") return createApp(request, env);
  const adminApp = path.match(/^\/api\/v1\/admin\/apps\/([0-9a-f-]+)$/i);
  if (adminApp && request.method === "PATCH") return updateApp(request, env, adminApp[1]);
  if (adminApp && request.method === "DELETE") return deleteApp(request, env, adminApp[1]);
  if (path === "/api/v1/admin/guest-experience" && request.method === "GET") return getGuestExperience(request, env);
  if (path === "/api/v1/admin/guest-experience" && request.method === "PATCH") return updateGuestExperience(request, env);
  const adminDevice = path.match(/^\/api\/v1\/admin\/devices\/([0-9a-f-]+)$/i);
  if (adminDevice && request.method === "PATCH") return updateDevice(request, env, adminDevice[1]);
  if (adminDevice && request.method === "DELETE") return unpairDevice(request, env, adminDevice[1]);
  const forceRefresh = path.match(/^\/api\/v1\/admin\/devices\/([0-9a-f-]+)\/force-refresh$/i);
  if (forceRefresh && request.method === "POST") return forceRefreshDevice(request, env, forceRefresh[1]);
  const checkUpdate = path.match(/^\/api\/v1\/admin\/devices\/([0-9a-f-]+)\/check-update$/i);
  if (checkUpdate && request.method === "POST") return checkUpdateDevice(request, env, checkUpdate[1]);
  const adminGroup = path.match(/^\/api\/v1\/admin\/box-groups\/([0-9a-f-]+)$/i);
  if (adminGroup && request.method === "PATCH") return updateBoxGroup(request, env, adminGroup[1]);
  if (adminGroup && request.method === "DELETE") return deleteBoxGroup(request, env, adminGroup[1]);
  const policy = path.match(/^\/api\/v1\/admin\/channel-policy\/(group|device)\/([0-9a-f-]+)$/i);
  if (policy && (request.method === "GET" || request.method === "PUT")) return channelPolicy(request, env, policy[1].toLowerCase(), policy[2]);
  const adminSite = path.match(/^\/api\/v1\/admin\/sites\/([0-9a-f-]+)$/i);
  if (adminSite && request.method === "PATCH") return updateSite(request, env, adminSite[1]);
  if (adminSite && request.method === "DELETE") return deleteSite(request, env, adminSite[1]);
  const install = path.match(/^\/api\/v1\/admin\/devices\/([0-9a-f-]+)\/commands$/i);
  if (install && request.method === "POST") return queueInstall(request, env, install[1]);
  if (path === "/api/v1/devices/config" && request.method === "GET") return deviceConfig(request, env);
  if (path === "/api/v1/devices/playlist.m3u" && request.method === "GET") return getDeviceM3UPlaylist(request, env);
  if (path === "/api/v1/devices/guide" && request.method === "GET") return getRokuGuide(request, env, ctx);
  if (path === "/api/v1/devices/radio-streams" && request.method === "GET") return getDeviceRadioStreams(request, env);
  if (path === "/api/v1/devices/commands" && request.method === "GET") return pendingCommands(request, env);
  const result = path.match(/^\/api\/v1\/devices\/commands\/([0-9a-f-]+)\/result$/i);
  if (result && request.method === "POST") return commandResult(request, env, result[1]);
  if (path === "/api/v1/devices/heartbeat" && request.method === "POST") return heartbeat(request, env);
  // Radio Stations & Playlists API endpoints
  if (path === "/api/v1/admin/radio-stations" && request.method === "GET") return listRadioStations(request, env);
  if (path === "/api/v1/admin/radio-stations" && request.method === "POST") return createRadioStation(request, env);
  const adminRadio = path.match(/^\/api\/v1\/admin\/radio-stations\/([0-9a-f-]+)$/i);
  if (adminRadio && request.method === "PATCH") return updateRadioStation(request, env, adminRadio[1]);
  if (adminRadio && request.method === "DELETE") return deleteRadioStation(request, env, adminRadio[1]);

  // Live Event Channels API endpoints
  if (path === "/api/v1/admin/event-channels" && request.method === "GET") return listEventChannels(request, env);
  if (path === "/api/v1/admin/event-channels/provider-feed" && request.method === "GET") return fetchProviderFeed(request, env);
  if (path === "/api/v1/admin/event-channels/inject-single" && request.method === "POST") return injectSingleStream(request, env);
  if (path === "/api/v1/admin/event-channels/ranges" && request.method === "GET") return getEventChannelRangesHandler(request, env);
  if (path === "/api/v1/admin/event-channels/ranges" && request.method === "PUT") return updateEventChannelRangesHandler(request, env);
  if (path === "/api/v1/admin/event-channels/ingest" && request.method === "POST") return ingestEventChannels(request, env);
  if (path === "/api/v1/admin/event-channels/check-health" && request.method === "POST") return checkEventChannelsHealth(request, env);
  if (path === "/api/v1/admin/event-channels" && request.method === "POST") return createEventChannel(request, env);
  const adminEvent = path.match(/^\/api\/v1\/admin\/event-channels\/([0-9a-f-]+)$/i);
  if (adminEvent && request.method === "PATCH") return updateEventChannel(request, env, adminEvent[1]);
  if (adminEvent && request.method === "DELETE") return deleteEventChannel(request, env, adminEvent[1]);

  if (path === "/api/v1/admin/movies/metadata-search" && request.method === "GET") return searchMovieMetadata(request, env);
  if (path === "/api/v1/admin/movies" && request.method === "GET") return listMovies(request, env);
  if (path === "/api/v1/admin/movies" && request.method === "POST") return createMovie(request, env);
  const adminMovie = path.match(/^\/api\/v1\/admin\/movies\/([0-9a-f-]+)$/i);
  if (adminMovie && request.method === "PATCH") return updateMovie(request, env, adminMovie[1]);
  if (adminMovie && request.method === "DELETE") return deleteMovie(request, env, adminMovie[1]);

  if (path === "/api/v1/admin/export-m3u-url" && request.method === "POST") return createExternalM3uUrl(request, env);
  if (path === "/api/v1/admin/playlists" && request.method === "GET") return listPlaylists(request, env);
  if (path === "/api/v1/admin/epg/fetch" && request.method === "GET") return fetchGuidePreview(request, env);
  if (path === "/api/v1/admin/playlists" && request.method === "POST") return createPlaylist(request, env);
  const adminPlaylist = path.match(/^\/api\/v1\/admin\/playlists\/([0-9a-f-]+)$/i);
  if (adminPlaylist && request.method === "GET") return getPlaylist(request, env, adminPlaylist[1]);
  if (adminPlaylist && request.method === "PATCH") return updatePlaylist(request, env, adminPlaylist[1]);
  if (adminPlaylist && request.method === "DELETE") return deletePlaylist(request, env, adminPlaylist[1]);
  const playlistItems = path.match(/^\/api\/v1\/admin\/playlists\/([0-9a-f-]+)\/items$/i);
  if (playlistItems && request.method === "POST") return addPlaylistItem(request, env, playlistItems[1]);
  const playlistImport = path.match(/^\/api\/v1\/admin\/playlists\/([0-9a-f-]+)\/import$/i);
  if (playlistImport && request.method === "POST") return importPlaylistItems(request, env, playlistImport[1]);
  const playlistReorder = path.match(/^\/api\/v1\/admin\/playlists\/([0-9a-f-]+)\/reorder$/i);
  if (playlistReorder && request.method === "PATCH") return reorderPlaylistItems(request, env, playlistReorder[1]);
  const playlistGuide = path.match(/^\/api\/v1\/admin\/playlists\/([0-9a-f-]+)\/guide$/i);
  if (playlistGuide && request.method === "GET") return getManagedGuide(request, env, playlistGuide[1]);
  if (playlistGuide && request.method === "PUT") return saveManagedGuide(request, env, playlistGuide[1]);
  const playlistGuideExport = path.match(/^\/api\/v1\/admin\/playlists\/([0-9a-f-]+)\/guide\.xml(\.gz)?$/i);
  if (playlistGuideExport && request.method === "GET") return exportManagedGuide(request, env, ctx, playlistGuideExport[1], Boolean(playlistGuideExport[2]), true);
  const playlistPush = path.match(/^\/api\/v1\/admin\/playlists\/([0-9a-f-]+)\/push$/i);
  if (playlistPush && request.method === "POST") return pushPlaylist(request, env, playlistPush[1]);
  const playlistItemDelete = path.match(/^\/api\/v1\/admin\/playlists\/([0-9a-f-]+)\/items\/([0-9a-f-]+)$/i);
  if (playlistItemDelete && request.method === "PATCH") return updatePlaylistItem(request, env, playlistItemDelete[1], playlistItemDelete[2]);
  if (playlistItemDelete && request.method === "DELETE") return deletePlaylistItem(request, env, playlistItemDelete[1], playlistItemDelete[2]);

  if (path.startsWith("/api/")) return json({ error: "Not found." }, 404);
  return env.ASSETS.fetch(request);
}

export function computeEventChannelsFingerprint(rows: Record<string, unknown>[]): string {
  const normalized = rows
    .map((row) => ({
      tvg_id: String(row.tvg_id ?? ""),
      tvg_name: String(row.tvg_name ?? ""),
      title: String(row.title ?? ""),
      sport_league: String(row.sport_league ?? ""),
      group_title: String(row.group_title ?? ""),
      logo_url: String(row.logo_url ?? ""),
      stream_url: String(row.stream_url ?? ""),
      backup_stream_url: String(row.backup_stream_url ?? ""),
      channel_number: String(row.channel_number ?? ""),
      start_time: String(row.start_time ?? ""),
      end_time: String(row.end_time ?? ""),
      pre_buffer_hours: row.pre_buffer_hours ?? null,
      post_buffer_hours: row.post_buffer_hours ?? null,
      status: String(row.status ?? ""),
      is_online: row.is_online ?? null,
      auto_ingested: row.auto_ingested ?? null
    }))
    .sort((a, b) =>
      a.tvg_id.localeCompare(b.tvg_id)
      || a.channel_number.localeCompare(b.channel_number)
      || a.title.localeCompare(b.title)
    );
  return JSON.stringify(normalized);
}

async function pushConfigUpdateToAllDevices(
  env: Env,
  options: { syncMessage?: string; bumpForceRefresh?: boolean } = {}
): Promise<number> {
  try {
    const { syncMessage = "Auto-syncing live event guide", bumpForceRefresh = true } = options;
    const devices = await supabaseJson(env,
      "/rest/v1/devices?select=id,owner_id,config_version"
    ).catch(() => null) as Record<string, unknown>[] | null;

    if (!Array.isArray(devices) || devices.length === 0) return 0;

    const token = crypto.randomUUID();
    const pushedAt = new Date().toISOString();

    await Promise.all(devices.map((device) => {
      const patch: Record<string, unknown> = {
        config_version: Number(device.config_version || 0) + 1,
        sync_status: "queued",
        sync_progress: 0,
        sync_message: syncMessage,
        sync_updated_at: pushedAt
      };
      if (bumpForceRefresh) {
        patch.force_refresh_token = token;
      }
      return supabaseJson(env, `/rest/v1/devices?id=eq.${encodeURIComponent(String(device.id))}`, {
        method: "PATCH",
        body: JSON.stringify(patch)
      }).catch(() => undefined);
    }));

    return devices.length;
  } catch (err) {
    console.error("Failed to push config update to devices:", err);
    return 0;
  }
}

export async function performAutoIngestAndHealthCheck(
  env: Env
): Promise<{ ingested: number; online: number; offline: number; pushed: boolean }> {
  const beforeRows = await supabaseJson(env,
    `/rest/v1/event_channels?status=neq.disabled&select=*`
  ).catch(() => null) as Record<string, unknown>[] | null;
  const beforeFingerprint = Array.isArray(beforeRows) ? computeEventChannelsFingerprint(beforeRows) : null;

  let ingested = 0;
  try {
    const providerUrl = "https://starlite.best/api/list/mygbb8/167848";
    const response = await fetch(providerUrl, {
      headers: {
        "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
      }
    });
    if (response.ok) {
      const text = await response.text();
      const parsed = parseConMeM3u(text);
      const scheduleSnapshot = await fetchEspnSportsSchedules().catch(() => ({ games: [], verifiedLeagues: new Set<ScheduledSportsGame["league"]>() }));
      const scheduled = applyLiveSchedulesToEvents(parsed, scheduleSnapshot);
      if (scheduled.length > 0) {
        const uniqueMap = new Map<string, typeof scheduled[0]>();
        for (const item of scheduled) {
          if (!uniqueMap.has(item.tvgId)) uniqueMap.set(item.tvgId, item);
        }
        const deduplicated = Array.from(uniqueMap.values());
        const ranges = await loadEventChannelRanges(env);
        const renumbered = assignCategoryChannelNumbers(deduplicated, ranges);
        await saveIngestedEventChannels(env, renumbered).catch((err) => console.error("Auto-ingest save error:", err));
        await pruneAutoIngestedEventChannels(env, new Set(renumbered.map((item) => item.tvgId)));
        ingested = deduplicated.length;
      } else {
        await pruneAutoIngestedEventChannels(env, new Set());
      }
    }
  } catch (err) {
    console.error("Auto-ingest background error:", err);
  }

  let online = 0;
  let offline = 0;
  let afterRows: Record<string, unknown>[] | null = null;
  try {
    const rows = await supabaseJson(env,
      `/rest/v1/event_channels?status=neq.disabled&select=*`
    ).catch(() => null) as Record<string, unknown>[] | null;
    afterRows = rows;
    if (Array.isArray(rows) && rows.length > 0) {
      const nowIso = new Date().toISOString();
      for (let i = 0; i < rows.length; i += 5) {
        const chunk = rows.slice(i, i + 5);
        await Promise.all(chunk.map(async (row) => {
          const id = String(row.id);
          const previousOnline = row.is_online;
          const health = await checkEventRowHealth(row, 3500);
          if (health.effectiveOnline) online++; else offline++;
          row.is_online = health.isOnline;
          const patch: Record<string, unknown> = {
            is_online: health.isOnline,
            health_status: health.statusText,
            last_checked_at: nowIso
          };
          if (Boolean(previousOnline) !== Boolean(health.isOnline)) {
            patch.updated_at = nowIso;
          }
          await supabaseJson(env, `/rest/v1/event_channels?id=eq.${encodeURIComponent(id)}`, {
            method: "PATCH",
            body: JSON.stringify(patch)
          }).catch(() => undefined);
        }));
      }
    }
  } catch (err) {
    console.error("Auto health check background error:", err);
  }

  const afterFingerprint = Array.isArray(afterRows) ? computeEventChannelsFingerprint(afterRows) : null;
  const changed = beforeFingerprint !== afterFingerprint;

  // Only push fleet-wide config update when active event channels actually changed
  let pushed = false;
  if (changed) {
    await pushConfigUpdateToAllDevices(env);
    pushed = true;
  }

  return { ingested, online, offline, pushed };
}

export default {
  async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    try {
      return await route(request, env, ctx);
    } catch (error) {
      if (error instanceof Response) return error;
      console.error(error);
      const message = error instanceof Error ? error.message : "Unexpected error";
      const isInputError = message.startsWith("Invalid") || message.startsWith("Expected");
      const isPublishError = message.startsWith("EPG publish failed:");
      const status = isInputError ? 400 : isPublishError ? 502 : 500;
      return json({ error: status === 500 ? "Service unavailable." : message }, status);
    }
  },
  async scheduled(controller: ScheduledController, env: Env, ctx: ExecutionContext): Promise<void> {
    ctx.waitUntil((async () => {
      console.log("Running 15-minute scheduled event sync & stream health check...");
      await performAutoIngestAndHealthCheck(env);
    })());
  }
} satisfies ExportedHandler<Env>;
