interface Env {
  ASSETS: Fetcher;
  SUPABASE_URL: string;
  SUPABASE_PUBLISHABLE_KEY: string;
  SUPABASE_SECRET_KEY: string;
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

function validateXmlTv(value: unknown): { xml: string; channelCount: number; programmeCount: number } {
  if (typeof value !== "string" || value.length < 20) throw new Error("Invalid XMLTV guide");
  const size = new TextEncoder().encode(value).byteLength;
  if (size > MAX_XMLTV_BYTES) {
    throw new Error(`XMLTV guide is ${(size / 1024 / 1024).toFixed(1)} MB after decompression; maximum is 40 MB`);
  }
  const xml = value.replace(/^\uFEFF/, "").trim();
  if (!/<tv(?:\s|>)/i.test(xml) || !/<\/tv>/i.test(xml)) throw new Error("Invalid XMLTV guide");
  return {
    xml,
    channelCount: (xml.match(/<channel(?:\s|>)/gi) || []).length,
    programmeCount: (xml.match(/<programme(?:\s|>)/gi) || []).length
  };
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

async function listDevices(request: Request, env: Env): Promise<Response> {
  const user = await adminUser(request, env);
  const devices = await supabaseJson(
    env,
    `/rest/v1/devices?owner_id=eq.${user.id}&select=*&order=created_at.desc`
  ) as Record<string, unknown>[];
  return json({ devices });
}

async function updateDevice(request: Request, env: Env, deviceId: string): Promise<Response> {
  const user = await adminUser(request, env);
  const input = await body(request);
  const currentRows = await supabaseJson(
    env,
    `/rest/v1/devices?id=eq.${encodeURIComponent(deviceId)}&owner_id=eq.${user.id}&select=config_version`
  ) as Record<string, unknown>[];
  if (!currentRows[0]) return json({ error: "Device not found." }, 404);
  const allowed = [
    "name", "guest_name", "playlist_url", "epg_url", "request_headers",
    "visible_apps", "theme_mode", "weather_location", "start_destination",
    "captions_enabled", "captions_language", "auto_start", "resume_last_channel",
    "osd_timeout_seconds", "auto_update", "wifi_only", "keep_awake_home", "home_preview_channel_id",
    "room_number", "arrival_date", "departure_date", "site_id", "assigned_playlist_id", "box_group_id"
  ];
  const patch = Object.fromEntries(Object.entries(input).filter(([key]) => allowed.includes(key)));
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
  patch.config_version = Number(currentRows[0].config_version || 0) + 1;
  patch.updated_at = new Date().toISOString();
  const devices = await supabaseJson(
    env,
    `/rest/v1/devices?id=eq.${encodeURIComponent(deviceId)}&owner_id=eq.${user.id}&select=*`,
    {
      method: "PATCH",
      headers: { prefer: "return=representation" },
      body: JSON.stringify(patch)
    }
  ) as Record<string, unknown>[];
  if (!devices[0]) return json({ error: "Device not found." }, 404);
  return json({ device: devices[0] });
}

async function forceRefreshDevice(request: Request, env: Env, deviceId: string): Promise<Response> {
  const user = await adminUser(request, env);
  const currentRows = await supabaseJson(
    env,
    `/rest/v1/devices?id=eq.${encodeURIComponent(deviceId)}&owner_id=eq.${user.id}&select=config_version`
  ) as Record<string, unknown>[];
  if (!currentRows[0]) return json({ error: "Device not found." }, 404);
  const token = new Date().toISOString();
  const patch = {
    force_refresh_token: token,
    config_version: Number(currentRows[0].config_version || 0) + 1,
    updated_at: token
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
  return json({ ok: true, device: devices[0] });
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

async function deviceConfig(request: Request, env: Env): Promise<Response> {
  const device = await deviceForToken(request, env);
  const origin = new URL(request.url).origin;
  const managedPlaylistUrl = `${origin}/api/v1/devices/playlist.m3u`;
  let managedEpgUrl: unknown = null;
  let groupPlaylistId: unknown = null;
  if (device.box_group_id) {
    const groups = await supabaseJson(env,
      `/rest/v1/box_groups?id=eq.${encodeURIComponent(String(device.box_group_id))}&owner_id=eq.${device.owner_id}&select=playlist_id`
    ) as Record<string, unknown>[];
    groupPlaylistId = groups[0]?.playlist_id ?? null;
  }
  const effectivePlaylistId = device.assigned_playlist_id || groupPlaylistId;
  const hasManagedAssignment = Boolean(effectivePlaylistId);
  const playlistUrl = hasManagedAssignment ? managedPlaylistUrl : (device.playlist_url || managedPlaylistUrl);
  if (hasManagedAssignment || !device.epg_url) {
    const playlistFilter = effectivePlaylistId
      ? `id=eq.${encodeURIComponent(String(effectivePlaylistId))}`
      : `owner_id=eq.${device.owner_id}&target_app=in.(tv,both)&is_published=eq.true&order=created_at.asc&limit=1`;
    const assigned = await supabaseJson(env, `/rest/v1/playlists?${playlistFilter}&select=id,epg_url`) as Record<string, unknown>[];
    if (assigned[0]?.epg_url) managedEpgUrl = assigned[0].epg_url;
  }
  const profiles = device.site_id ? await supabaseJson(env,
    `/rest/v1/guest_experience_profiles?site_id=eq.${device.site_id}&owner_id=eq.${device.owner_id}&select=*`
  ) as Record<string, unknown>[] : [];
  const profile = profiles[0] ?? {};
  return json({
    version: device.config_version,
    experienceVersion: profile.updated_at ?? "default",
    deviceName: device.name,
    guestName: device.guest_name,
    playlistUrl,
    epgUrl: hasManagedAssignment ? managedEpgUrl : (device.epg_url || managedEpgUrl),
    requestHeaders: device.request_headers ?? {},
    visibleApps: device.visible_apps ?? [],
    themeMode: device.theme_mode,
    weatherLocation: device.weather_location,
    startDestination: device.start_destination,
    captionsEnabled: device.captions_enabled,
    captionsLanguage: device.captions_language,
    autoStart: device.auto_start,
    resumeLastChannel: device.resume_last_channel,
    keepAwakeHome: device.keep_awake_home ?? false,
    homePreviewChannelId: device.home_preview_channel_id ?? null,
    osdTimeoutSeconds: device.osd_timeout_seconds ?? 8,
    autoUpdate: device.auto_update ?? true,
    wifiOnly: device.wifi_only ?? false,
    forceRefreshToken: device.force_refresh_token ?? null,
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
  const refreshFilter = targetType === 'device' ? `id=eq.${encodeURIComponent(targetId)}` : `box_group_id=eq.${encodeURIComponent(targetId)}`;
  const affectedDevices = await supabaseJson(env, `/rest/v1/devices?owner_id=eq.${user.id}&${refreshFilter}&select=id,config_version`) as Record<string, unknown>[];
  await Promise.all(affectedDevices.map((device) => supabaseJson(env, `/rest/v1/devices?id=eq.${device.id}&owner_id=eq.${user.id}`, {
    method: "PATCH", body: JSON.stringify({ config_version: Number(device.config_version || 0) + 1, force_refresh_token: crypto.randomUUID() })
  })));
  return json({ ok: true, rules: rules.length });
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
  return json({ profile: profiles[0] });
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

async function listApps(request: Request, env: Env): Promise<Response> {
  const user = await adminUser(request, env);
  const apps = await supabaseJson(env,
    `/rest/v1/app_catalog?owner_id=eq.${user.id}&select=*&order=name.asc`
  ) as Record<string, unknown>[];
  return json({ apps });
}

async function createApp(request: Request, env: Env): Promise<Response> {
  const user = await adminUser(request, env);
  const input = await body(request);
  const sourceType = requiredString(input.source_type, "source type", 20);
  if (!["play_store", "repository"].includes(sourceType)) throw new Error("Invalid source type");
  const sourceUrl = optionalString(input.source_url, "source URL");
  if (sourceType === "repository" && !isHttpsUrl(sourceUrl)) {
    throw new Error("Invalid source URL: repository apps require HTTPS");
  }
  const checksum = optionalString(input.sha256, "SHA-256", 64);
  if (checksum && !/^[a-f0-9]{64}$/i.test(checksum)) throw new Error("Invalid SHA-256");
  const apps = await supabaseJson(env, "/rest/v1/app_catalog?select=*", {
    method: "POST", headers: { prefer: "return=representation" },
    body: JSON.stringify({
      owner_id: user.id,
      name: requiredString(input.name, "name", 100),
      package_name: requiredString(input.package_name, "package name", 180),
      source_type: sourceType,
      source_url: sourceUrl,
      version_name: optionalString(input.version_name, "version", 40),
      sha256: checksum?.toLowerCase() ?? null
    })
  }) as Record<string, unknown>[];
  return json({ app: apps[0] }, 201);
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
  if (activityType && !["idle", "channel", "app"].includes(activityType)) {
    throw new Error("Invalid activity type");
  }
  const patch: Record<string, unknown> = {
    last_seen_at: new Date().toISOString(),
    app_version: typeof input.appVersion === "string" ? input.appVersion.slice(0, 40) : device.app_version,
    last_error: typeof input.lastError === "string" ? input.lastError.slice(0, 500) : null
  };
  if (activityType) {
    patch.activity_type = activityType;
    patch.activity_label = optionalString(activity?.label, "activity label", 160);
    patch.activity_package = optionalString(activity?.packageName, "activity package", 180);
    patch.activity_updated_at = new Date().toISOString();
  }
  await supabaseJson(env, `/rest/v1/devices?id=eq.${device.id}`, {
    method: "PATCH",
    headers: { prefer: "return=minimal" },
    body: JSON.stringify(patch)
  });
  return json({ ok: true, configVersion: device.config_version });
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
  return json({ station: stations[0] });
}

async function deleteRadioStation(request: Request, env: Env, stationId: string): Promise<Response> {
  const user = await adminUser(request, env);
  await supabaseJson(
    env,
    `/rest/v1/radio_stations?id=eq.${encodeURIComponent(stationId)}&owner_id=eq.${user.id}`,
    { method: "DELETE" }
  );
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

async function radioCatalogResponse(request: Request, stations: PublicRadioStation[]): Promise<Response> {
  const serializedStations = JSON.stringify(stations);
  const version = await sha256(serializedStations);
  const etag = `"${version}"`;
  const headers = {
    "access-control-allow-origin": "*",
    "cache-control": "public, max-age=60, stale-while-revalidate=300",
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
  return radioCatalogResponse(request, rows.map(publicRadioStation));
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
  const rows = await supabaseJson(env, "/rest/v1/epg_guides?on_conflict=playlist_id&select=id,name,source_url,channel_count,programme_count,updated_at", {
    method: "POST",
    headers: { prefer: "resolution=merge-duplicates,return=representation" },
    body: JSON.stringify({
      owner_id: user.id, playlist_id: playlistId, name, source_url: sourceUrl,
      xml_content: guide.xml, channel_count: guide.channelCount, programme_count: guide.programmeCount,
      updated_at: new Date().toISOString()
    })
  }) as Record<string, unknown>[];
  const publicUrl = `${new URL(request.url).origin}/api/v1/guides/${playlistId}.xml.gz`;
  await supabaseJson(env, `/rest/v1/playlists?id=eq.${encodeURIComponent(playlistId)}&owner_id=eq.${user.id}`, {
    method: "PATCH", body: JSON.stringify({ epg_url: publicUrl, updated_at: new Date().toISOString() })
  });
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

async function exportManagedGuide(request: Request, env: Env, playlistId: string, gzip: boolean, requireAdmin = false): Promise<Response> {
  let ownerFilter = "";
  if (requireAdmin) {
    const { user } = await ownedPlaylist(request, env, playlistId);
    ownerFilter = `&owner_id=eq.${user.id}`;
  } else {
    const playlists = await supabaseJson(env,
      `/rest/v1/playlists?id=eq.${encodeURIComponent(playlistId)}&is_published=eq.true&select=id`
    ) as Record<string, unknown>[];
    if (!playlists[0]) return json({ error: "Guide not found." }, 404);
  }
  const rows = await supabaseJson(env,
    `/rest/v1/epg_guides?playlist_id=eq.${encodeURIComponent(playlistId)}${ownerFilter}&select=id,name,source_url,xml_content,updated_at`
  ) as Record<string, unknown>[];
  if (!rows[0]) return json({ error: "Guide not found." }, 404);
  const row = rows[0];
  let xmlContent = String(row.xml_content);
  const stale = row.source_url && Date.now() - new Date(String(row.updated_at)).getTime() >= 6 * 60 * 60_000;
  if (stale) {
    try {
      const source = await fetch(String(row.source_url), { headers: { "user-agent": "GLZ-Hub-EPG/1.0", accept: "application/xml,text/xml,application/gzip,*/*" } });
      if (source.ok) {
        const refreshed = validateXmlTv(await xmlTextFromResponse(source));
        xmlContent = refreshed.xml;
        await supabaseJson(env, `/rest/v1/epg_guides?id=eq.${encodeURIComponent(String(row.id))}`, {
          method: "PATCH",
          body: JSON.stringify({ xml_content: refreshed.xml, channel_count: refreshed.channelCount, programme_count: refreshed.programmeCount, updated_at: new Date().toISOString() })
        });
      }
    } catch (error) { console.error("On-demand EPG refresh failed", row.id, error); }
  }
  const headers = new Headers({
    "content-type": "application/xml; charset=utf-8",
    "cache-control": "public, max-age=300",
    "content-disposition": `attachment; filename="guide-${playlistId}.xml${gzip ? ".gz" : ""}"`
  });
  let body: BodyInit = xmlContent;
  if (gzip) {
    headers.set("content-encoding", "gzip");
    body = new Blob([body]).stream().pipeThrough(new CompressionStream("gzip"));
  }
  return new Response(body, { headers });
}

async function pushPlaylist(request: Request, env: Env, playlistId: string): Promise<Response> {
  const { user } = await ownedPlaylist(request, env, playlistId);
  const token = crypto.randomUUID();
  const devices = await supabaseJson(env,
    `/rest/v1/devices?owner_id=eq.${user.id}&or=(assigned_playlist_id.eq.${encodeURIComponent(playlistId)},assigned_playlist_id.is.null)&select=id`
  ) as Record<string, unknown>[];
  await supabaseJson(env,
    `/rest/v1/devices?owner_id=eq.${user.id}&or=(assigned_playlist_id.eq.${encodeURIComponent(playlistId)},assigned_playlist_id.is.null)`,
    { method: "PATCH", body: JSON.stringify({ force_refresh_token: token }) }
  );
  return json({ ok: true, devices: devices.length });
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

  const playlists = await supabaseJson(env, playlistQuery) as Record<string, unknown>[];
  const targetFilters = [`and(target_type.eq.device,target_id.eq.${device.id})`];
  if (group?.id) targetFilters.push(`and(target_type.eq.group,target_id.eq.${group.id})`);
  const rules = await supabaseJson(env,
    `/rest/v1/channel_policy_rules?owner_id=eq.${device.owner_id}&or=(${targetFilters.join(',')})&select=target_type,playlist_item_id,decision`
  ) as Record<string, unknown>[];
  const groupRules = new Map(rules.filter((rule) => rule.target_type === 'group').map((rule) => [String(rule.playlist_item_id), String(rule.decision)]));
  const deviceRules = new Map(rules.filter((rule) => rule.target_type === 'device').map((rule) => [String(rule.playlist_item_id), String(rule.decision)]));

  let m3uContent = '#EXTM3U x-tvg-url="https://play.glztech.com/epg.xml.gz"\n\n';

  const cleanText = (value: unknown): string => String(value ?? "").replace(/[\r\n]+/g, " ").trim();
  const cleanAttribute = (value: unknown): string => cleanText(value).replace(/"/g, "'");

  for (const pl of playlists) {
    if (typeof pl.epg_url === "string" && pl.epg_url) {
      m3uContent = `#EXTM3U x-tvg-url="${cleanAttribute(pl.epg_url)}"\n\n`;
    }
    const groupTitle = cleanAttribute(pl.title || "TV");
    const items = (pl.playlist_items as Record<string, unknown>[]) || [];
    items.sort((a, b) => Number(a.position || 0) - Number(b.position || 0));

    for (const item of items) {
      const title = cleanText(item.title || "Untitled Channel");
      const duration = Number(item.duration_seconds || -1);
      const mediaUrl = String(item.media_url || "");
      const metadata = (item.metadata && typeof item.metadata === "object") ? item.metadata as Record<string, unknown> : {};
      if (metadata.hidden === true || metadata.hidden === "true") continue;
      const deviceMode = String(device.channel_policy_mode || 'inherit');
      const decision = deviceRules.get(String(item.id))
        ?? (deviceMode !== 'inherit' ? deviceMode : undefined)
        ?? (useGroupPolicy ? groupRules.get(String(item.id)) : undefined)
        ?? String(useGroupPolicy ? (group?.default_channel_policy || 'allow') : 'allow');
      if (decision === 'block') continue;

      const tvgId = metadata.tvg_id || metadata.tvgId ? ` tvg-id="${cleanAttribute(metadata.tvg_id || metadata.tvgId)}"` : "";
      const tvgChno = metadata.tvg_chno || metadata.tvgChno ? ` tvg-chno="${cleanAttribute(metadata.tvg_chno || metadata.tvgChno)}"` : "";
      const tvgLogo = metadata.tvg_logo || metadata.tvgLogo || pl.artwork_url ? ` tvg-logo="${cleanAttribute(metadata.tvg_logo || metadata.tvgLogo || pl.artwork_url)}"` : "";
      const isRadio = metadata.radio === true || metadata.radio === "true" ? ' radio="true"' : "";

      const channelGroup = cleanAttribute(metadata.group || groupTitle);
      m3uContent += `#EXTINF:${duration}${tvgId}${tvgChno}${tvgLogo}${isRadio} group-title="${channelGroup}",${title}\n${mediaUrl}\n\n`;
    }
  }

  return new Response(m3uContent, {
    status: 200,
    headers: {
      "content-type": "audio/x-mpegurl; charset=utf-8",
      "cache-control": "no-cache"
    }
  });
}

async function getDeviceRadioStreams(request: Request, env: Env): Promise<Response> {
  const device = await deviceForToken(request, env);
  const rows = await supabaseJson(
    env,
    `/rest/v1/radio_stations?owner_id=eq.${device.owner_id}&is_active=eq.true&select=station_code,name,genre,stream_url,logo_url,epg_channel_id,bitrate,request_headers&order=station_code.asc`
  ) as Record<string, unknown>[];
  return radioCatalogResponse(request, rows.map(publicRadioStation));
}

async function route(request: Request, env: Env): Promise<Response> {
  const url = new URL(request.url);
  const path = url.pathname;

  if (path === "/api/health") return json({ ok: true, service: "glzhub" });
  if (path === "/api/v1/radio/stations" && request.method === "GET") return publicRadioCatalog(request, env);
  const publicGuide = path.match(/^\/api\/v1\/guides\/([0-9a-f-]+)\.xml(\.gz)?$/i);
  if (publicGuide && request.method === "GET") return exportManagedGuide(request, env, publicGuide[1], Boolean(publicGuide[2]));
  if (path === "/api/v1/public-config") {
    return json({ supabaseUrl: env.SUPABASE_URL, publishableKey: env.SUPABASE_PUBLISHABLE_KEY });
  }
  if (path === "/api/v1/enrollment" && request.method === "POST") return enroll(request, env);
  if (path === "/api/v1/enrollment/claim" && request.method === "POST") return claimEnrollment(request, env);
  if (path === "/api/v1/admin/enrollments" && request.method === "GET") return listNearbyEnrollments(request, env);
  if (path === "/api/v1/admin/devices" && request.method === "GET") return listDevices(request, env);
  if (path === "/api/v1/admin/box-groups" && request.method === "GET") return listBoxGroups(request, env);
  if (path === "/api/v1/admin/box-groups" && request.method === "POST") return createBoxGroup(request, env);
  if (path === "/api/v1/admin/sites" && request.method === "GET") return listSites(request, env);
  if (path === "/api/v1/admin/sites" && request.method === "POST") return createSite(request, env);
  if (path === "/api/v1/admin/apps" && request.method === "GET") return listApps(request, env);
  if (path === "/api/v1/admin/apps" && request.method === "POST") return createApp(request, env);
  if (path === "/api/v1/admin/guest-experience" && request.method === "GET") return getGuestExperience(request, env);
  if (path === "/api/v1/admin/guest-experience" && request.method === "PATCH") return updateGuestExperience(request, env);
  const adminDevice = path.match(/^\/api\/v1\/admin\/devices\/([0-9a-f-]+)$/i);
  if (adminDevice && request.method === "PATCH") return updateDevice(request, env, adminDevice[1]);
  if (adminDevice && request.method === "DELETE") return unpairDevice(request, env, adminDevice[1]);
  const forceRefresh = path.match(/^\/api\/v1\/admin\/devices\/([0-9a-f-]+)\/force-refresh$/i);
  if (forceRefresh && request.method === "POST") return forceRefreshDevice(request, env, forceRefresh[1]);
  const adminGroup = path.match(/^\/api\/v1\/admin\/box-groups\/([0-9a-f-]+)$/i);
  if (adminGroup && request.method === "PATCH") return updateBoxGroup(request, env, adminGroup[1]);
  if (adminGroup && request.method === "DELETE") return deleteBoxGroup(request, env, adminGroup[1]);
  const policy = path.match(/^\/api\/v1\/admin\/channel-policy\/(group|device)\/([0-9a-f-]+)$/i);
  if (policy && (request.method === "GET" || request.method === "PUT")) return channelPolicy(request, env, policy[1].toLowerCase(), policy[2]);
  const adminSite = path.match(/^\/api\/v1\/admin\/sites\/([0-9a-f-]+)$/i);
  if (adminSite && request.method === "PATCH") return updateSite(request, env, adminSite[1]);
  const install = path.match(/^\/api\/v1\/admin\/devices\/([0-9a-f-]+)\/commands$/i);
  if (install && request.method === "POST") return queueInstall(request, env, install[1]);
  if (path === "/api/v1/devices/config" && request.method === "GET") return deviceConfig(request, env);
  if (path === "/api/v1/devices/playlist.m3u" && request.method === "GET") return getDeviceM3UPlaylist(request, env);
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
  if (playlistGuideExport && request.method === "GET") return exportManagedGuide(request, env, playlistGuideExport[1], Boolean(playlistGuideExport[2]), true);
  const playlistPush = path.match(/^\/api\/v1\/admin\/playlists\/([0-9a-f-]+)\/push$/i);
  if (playlistPush && request.method === "POST") return pushPlaylist(request, env, playlistPush[1]);
  const playlistItemDelete = path.match(/^\/api\/v1\/admin\/playlists\/([0-9a-f-]+)\/items\/([0-9a-f-]+)$/i);
  if (playlistItemDelete && request.method === "PATCH") return updatePlaylistItem(request, env, playlistItemDelete[1], playlistItemDelete[2]);
  if (playlistItemDelete && request.method === "DELETE") return deletePlaylistItem(request, env, playlistItemDelete[1], playlistItemDelete[2]);

  if (path.startsWith("/api/")) return json({ error: "Not found." }, 404);
  return env.ASSETS.fetch(request);
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    try {
      return await route(request, env);
    } catch (error) {
      if (error instanceof Response) return error;
      console.error(error);
      const message = error instanceof Error ? error.message : "Unexpected error";
      const status = message.startsWith("Invalid") || message.startsWith("Expected") ? 400 : 500;
      return json({ error: status === 500 ? "Service unavailable." : message }, status);
    }
  }
} satisfies ExportedHandler<Env>;
