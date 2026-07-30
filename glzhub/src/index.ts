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
      expires_at: new Date(Date.now() + 15 * 60_000).toISOString()
    })
  });
  return json({
    pairingCode,
    deviceToken,
    expiresInSeconds: 900,
    pairUrl: "https://glzhub.glztech.com/pair"
  }, 201);
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
    "captions_enabled", "captions_language", "auto_start", "resume_last_channel"
    , "room_number", "arrival_date", "departure_date", "site_id"
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
  const profiles = device.site_id ? await supabaseJson(env,
    `/rest/v1/guest_experience_profiles?site_id=eq.${device.site_id}&owner_id=eq.${device.owner_id}&select=*`
  ) as Record<string, unknown>[] : [];
  const profile = profiles[0] ?? {};
  return json({
    version: device.config_version,
    experienceVersion: profile.updated_at ?? "default",
    deviceName: device.name,
    guestName: device.guest_name,
    playlistUrl: device.playlist_url,
    epgUrl: device.epg_url,
    requestHeaders: device.request_headers ?? {},
    visibleApps: device.visible_apps ?? [],
    themeMode: device.theme_mode,
    weatherLocation: device.weather_location,
    startDestination: device.start_destination,
    captionsEnabled: device.captions_enabled,
    captionsLanguage: device.captions_language,
    autoStart: device.auto_start,
    resumeLastChannel: device.resume_last_channel,
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
  await supabaseJson(env, `/rest/v1/devices?id=eq.${device.id}`, {
    method: "PATCH",
    headers: { prefer: "return=minimal" },
    body: JSON.stringify({
      last_seen_at: new Date().toISOString(),
      app_version: typeof input.appVersion === "string" ? input.appVersion.slice(0, 40) : device.app_version,
      last_error: typeof input.lastError === "string" ? input.lastError.slice(0, 500) : null
    })
  });
  return json({ ok: true, configVersion: device.config_version });
}

async function route(request: Request, env: Env): Promise<Response> {
  const url = new URL(request.url);
  const path = url.pathname;

  if (path === "/api/health") return json({ ok: true, service: "glzhub" });
  if (path === "/api/v1/public-config") {
    return json({ supabaseUrl: env.SUPABASE_URL, publishableKey: env.SUPABASE_PUBLISHABLE_KEY });
  }
  if (path === "/api/v1/enrollment" && request.method === "POST") return enroll(request, env);
  if (path === "/api/v1/enrollment/claim" && request.method === "POST") return claimEnrollment(request, env);
  if (path === "/api/v1/admin/devices" && request.method === "GET") return listDevices(request, env);
  if (path === "/api/v1/admin/sites" && request.method === "GET") return listSites(request, env);
  if (path === "/api/v1/admin/sites" && request.method === "POST") return createSite(request, env);
  if (path === "/api/v1/admin/apps" && request.method === "GET") return listApps(request, env);
  if (path === "/api/v1/admin/apps" && request.method === "POST") return createApp(request, env);
  if (path === "/api/v1/admin/guest-experience" && request.method === "GET") return getGuestExperience(request, env);
  if (path === "/api/v1/admin/guest-experience" && request.method === "PATCH") return updateGuestExperience(request, env);
  const adminDevice = path.match(/^\/api\/v1\/admin\/devices\/([0-9a-f-]+)$/i);
  if (adminDevice && request.method === "PATCH") return updateDevice(request, env, adminDevice[1]);
  if (adminDevice && request.method === "DELETE") return unpairDevice(request, env, adminDevice[1]);
  const adminSite = path.match(/^\/api\/v1\/admin\/sites\/([0-9a-f-]+)$/i);
  if (adminSite && request.method === "PATCH") return updateSite(request, env, adminSite[1]);
  const install = path.match(/^\/api\/v1\/admin\/devices\/([0-9a-f-]+)\/commands$/i);
  if (install && request.method === "POST") return queueInstall(request, env, install[1]);
  if (path === "/api/v1/devices/config" && request.method === "GET") return deviceConfig(request, env);
  if (path === "/api/v1/devices/commands" && request.method === "GET") return pendingCommands(request, env);
  const result = path.match(/^\/api\/v1\/devices\/commands\/([0-9a-f-]+)\/result$/i);
  if (result && request.method === "POST") return commandResult(request, env, result[1]);
  if (path === "/api/v1/devices/heartbeat" && request.method === "POST") return heartbeat(request, env);
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
