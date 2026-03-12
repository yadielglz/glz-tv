import { createServer } from "node:http";
import { Readable } from "node:stream";
import { dirname, extname, join, normalize } from "node:path";
import { fileURLToPath } from "node:url";
import { readFile } from "node:fs/promises";
import { gunzipSync } from "node:zlib";

const isServerlessRuntime = Boolean(
  process.env.NETLIFY ||
  process.env.AWS_LAMBDA_FUNCTION_NAME ||
  process.env.LAMBDA_TASK_ROOT
);
const moduleFilename = isServerlessRuntime ? join(process.cwd(), "server.js") : fileURLToPath(import.meta.url);
const moduleDirname = dirname(moduleFilename);
const publicDir = join(moduleDirname, "public");
const port = Number(process.env.PORT || 3000);
const DEFAULT_PLAYLIST_URL = "https://epg.best/46837-shw7fc.m3u";
const DEFAULT_EPG_URL = "https://epg.best/1eef8-shw7fc.xml.gz";
const EPG_DESCRIPTION_LIMIT = 240;

const cache = new Map();
const BINARY_EXTENSIONS = new Set([
  ".js",
  ".css",
  ".png",
  ".jpg",
  ".jpeg",
  ".svg",
  ".ico",
  ".webp",
  ".woff",
  ".woff2",
  ".ttf"
]);

function send(res, status, body, contentType = "text/plain; charset=utf-8") {
  const payload = typeof body === "string" || body instanceof Uint8Array ? body : JSON.stringify(body);
  res.writeHead(status, {
    "Content-Type": contentType,
    "Cache-Control": "no-store",
    "Access-Control-Allow-Origin": "*"
  });
  res.end(payload);
}

function normalizeChannelKey(value = "") {
  return value.toLowerCase().replace(/[^a-z0-9]/g, "");
}

function parseEnvFile(source) {
  const values = {};
  source
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line && !line.startsWith("#"))
    .forEach((line) => {
      const index = line.indexOf("=");
      if (index === -1) {
        return;
      }
      const key = line.slice(0, index).trim();
      const value = line.slice(index + 1).trim().replace(/^['"]|['"]$/g, "");
      values[key] = value;
    });
  return values;
}

async function loadDotEnv() {
  try {
    const raw = await readFile(join(__dirname, ".env"), "utf8");
    const values = parseEnvFile(raw);
    Object.entries(values).forEach(([key, value]) => {
      if (!(key in process.env)) {
        process.env[key] = value;
      }
    });
  } catch {
    return;
  }
}

let dotenvPromise;

function ensureEnvLoaded() {
  if (!dotenvPromise) {
    dotenvPromise = loadDotEnv();
  }
  return dotenvPromise;
}

function getMimeType(pathname) {
  const extension = extname(pathname).toLowerCase();
  switch (extension) {
    case ".html":
      return "text/html; charset=utf-8";
    case ".css":
      return "text/css; charset=utf-8";
    case ".js":
      return "application/javascript; charset=utf-8";
    case ".json":
      return "application/json; charset=utf-8";
    case ".svg":
      return "image/svg+xml";
    case ".png":
      return "image/png";
    case ".jpg":
    case ".jpeg":
      return "image/jpeg";
    case ".ico":
      return "image/x-icon";
    case ".webp":
      return "image/webp";
    default:
      return "application/octet-stream";
  }
}

function resolveConfigUrl(queryValue, envValue) {
  if (queryValue) {
    return queryValue;
  }
  if (envValue) {
    return envValue;
  }
  return "";
}

function isHttpUrl(value) {
  try {
    const url = new URL(value);
    return url.protocol === "http:" || url.protocol === "https:";
  } catch {
    return false;
  }
}

async function fetchTextWithCache(url, ttlMs = 60_000) {
  const cached = cache.get(url);
  if (cached && cached.expiresAt > Date.now()) {
    return cached.value;
  }

  const response = await fetch(url, {
    headers: {
      "user-agent": "ChannelSurfer/0.1",
      accept: "*/*"
    }
  });

  if (!response.ok) {
    throw new Error(`Upstream request failed with ${response.status}`);
  }

  const value = await response.text();
  cache.set(url, { value, expiresAt: Date.now() + ttlMs });
  return value;
}

function parseM3uAttributes(line) {
  const attributes = {};
  const matches = line.matchAll(/([\w-]+)="([^"]*)"/g);
  for (const match of matches) {
    attributes[match[1]] = match[2];
  }
  return attributes;
}

function resolveChannelNumber(attributes = {}, fallbackValues = []) {
  const candidates = [
    attributes["tvg-chno"],
    attributes["ch-number"],
    attributes["channel-number"],
    attributes["tvg-num"],
    attributes.number,
    ...fallbackValues
  ];

  for (const candidate of candidates) {
    if (typeof candidate === "string" && candidate.trim()) {
      return candidate.trim();
    }
  }

  return "";
}

function parseVlcOption(line) {
  const prefix = "#EXTVLCOPT:";
  if (!line.startsWith(prefix)) {
    return null;
  }

  const remainder = line.slice(prefix.length);
  const separatorIndex = remainder.indexOf("=");
  if (separatorIndex === -1) {
    return null;
  }

  return {
    key: remainder.slice(0, separatorIndex).trim().toLowerCase(),
    value: remainder.slice(separatorIndex + 1).trim()
  };
}

function normalizeHeaderName(value = "") {
  return value.trim().toLowerCase().replace(/_/g, "-");
}

function mapStreamHeaderOption(optionKey, optionValue) {
  const headerMap = {
    "http-user-agent": "user-agent",
    "http-referrer": "referer",
    "http-origin": "origin",
    "http-cookie": "cookie",
    "http-host": "host",
    "http-authorization": "authorization"
  };
  const headerName = headerMap[optionKey];
  if (!headerName || !optionValue) {
    return null;
  }
  return [headerName, optionValue];
}

function mergeMappedHeader(target, key, value) {
  const mapped = mapStreamHeaderOption(normalizeHeaderName(key), value);
  if (mapped) {
    target[mapped[0]] = mapped[1];
    return;
  }

  if (value) {
    target[normalizeHeaderName(key)] = value;
  }
}

function parseKodiProp(line) {
  const prefix = "#KODIPROP:";
  if (!line.startsWith(prefix)) {
    return null;
  }

  const remainder = line.slice(prefix.length);
  const separatorIndex = remainder.indexOf("=");
  if (separatorIndex === -1) {
    return null;
  }

  return {
    key: remainder.slice(0, separatorIndex).trim().toLowerCase(),
    value: remainder.slice(separatorIndex + 1).trim()
  };
}

function applyStreamHeaderString(target, rawValue) {
  const decoded = decodeURIComponent(rawValue || "");
  decoded
    .split(/[&|]/)
    .map((entry) => entry.trim())
    .filter(Boolean)
    .forEach((entry) => {
      const separatorIndex = entry.indexOf("=");
      if (separatorIndex === -1) {
        return;
      }

      const key = entry.slice(0, separatorIndex).trim();
      const value = entry.slice(separatorIndex + 1).trim();
      mergeMappedHeader(target, key, value);
    });
}

function applyKodiPropHeaders(target, option) {
  if (!option) {
    return;
  }

  if (option.key.endsWith("stream_headers")) {
    applyStreamHeaderString(target, option.value);
    return;
  }

  if (option.key.startsWith("http-")) {
    mergeMappedHeader(target, option.key, option.value);
  }
}

function applyExtHttpHeaders(target, rawValue) {
  try {
    const parsed = JSON.parse(rawValue);
    const candidates = parsed.headers && typeof parsed.headers === "object" ? parsed.headers : parsed;
    Object.entries(candidates || {}).forEach(([key, value]) => {
      if (typeof value === "string") {
        mergeMappedHeader(target, key, value);
      }
    });
  } catch {
    return;
  }
}

function encodeStreamHeaders(headers) {
  return Buffer.from(JSON.stringify(headers || {}), "utf8").toString("base64url");
}

function decodeStreamHeaders(value) {
  if (!value) {
    return {};
  }

  try {
    const parsed = JSON.parse(Buffer.from(value, "base64url").toString("utf8"));
    return typeof parsed === "object" && parsed ? parsed : {};
  } catch {
    return {};
  }
}

function buildStreamProxyUrl(streamUrl, requestHeaders = {}) {
  const params = new URLSearchParams({ url: streamUrl });
  if (Object.keys(requestHeaders).length) {
    params.set("headers", encodeStreamHeaders(requestHeaders));
  }
  return `/api/stream?${params.toString()}`;
}

function parseHeaderInput(value = "") {
  if (!value.trim()) {
    return {};
  }

  try {
    const parsed = JSON.parse(value);
    if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) {
      return Object.fromEntries(
        Object.entries(parsed)
          .filter(([, headerValue]) => typeof headerValue === "string" && headerValue)
          .map(([key, headerValue]) => [normalizeHeaderName(key), headerValue])
      );
    }
  } catch {
    // Fall through to line parsing.
  }

  const headers = {};
  value
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .forEach((line) => {
      const separatorIndex = line.includes(":") ? line.indexOf(":") : line.indexOf("=");
      if (separatorIndex === -1) {
        return;
      }

      const key = line.slice(0, separatorIndex).trim();
      const headerValue = line.slice(separatorIndex + 1).trim();
      if (!key || !headerValue) {
        return;
      }

      headers[normalizeHeaderName(key)] = headerValue;
    });

  return headers;
}

function mergeHeaderSets(...sets) {
  return Object.assign({}, ...sets.filter((value) => value && typeof value === "object"));
}

function decodeXmlEntities(value = "") {
  return value
    .replaceAll("&amp;", "&")
    .replaceAll("&lt;", "<")
    .replaceAll("&gt;", ">")
    .replaceAll("&quot;", '"')
    .replaceAll("&apos;", "'");
}

function extractXmlAttribute(source, name) {
  const match = source.match(new RegExp(`${name}="([^"]*)"`, "i"));
  return decodeXmlEntities(match?.[1] || "");
}

function extractXmlNodeText(source, tagName) {
  const match = source.match(new RegExp(`<${tagName}\\b[^>]*>([\\s\\S]*?)</${tagName}>`, "i"));
  if (!match) {
    return "";
  }

  return decodeXmlEntities(match[1].replace(/<!\[CDATA\[([\s\S]*?)\]\]>/g, "$1").replace(/<[^>]+>/g, "").trim());
}

function extractXmlNodeTexts(source, tagName) {
  return Array.from(source.matchAll(new RegExp(`<${tagName}\\b[^>]*>([\\s\\S]*?)</${tagName}>`, "gi"))).map((match) =>
    decodeXmlEntities(match[1].replace(/<!\[CDATA\[([\s\S]*?)\]\]>/g, "$1").replace(/<[^>]+>/g, "").trim())
  );
}

function trimProgramDescription(value = "") {
  if (!value) {
    return "";
  }

  return value.length > EPG_DESCRIPTION_LIMIT ? `${value.slice(0, EPG_DESCRIPTION_LIMIT - 1).trimEnd()}…` : value;
}

function parseXmltvToGuide(xmlText, options = {}) {
  const includedKeys = options.includedKeys instanceof Set ? options.includedKeys : null;
  const guide = new Map();
  const channelAliases = new Map();

  for (const match of xmlText.matchAll(/<channel\b([^>]*)>([\s\S]*?)<\/channel>/gi)) {
    const attributes = match[1] || "";
    const body = match[2] || "";
    const id = normalizeChannelKey(extractXmlAttribute(attributes, "id"));
    if (!id) {
      continue;
    }

    const names = extractXmlNodeTexts(body, "display-name").map((name) => normalizeChannelKey(name)).filter(Boolean);
    channelAliases.set(id, Array.from(new Set([id, ...names])));
  }

  for (const match of xmlText.matchAll(/<programme\b([^>]*)>([\s\S]*?)<\/programme>/gi)) {
    const attributes = match[1] || "";
    const body = match[2] || "";
    const channelId = normalizeChannelKey(extractXmlAttribute(attributes, "channel"));
    if (!channelId) {
      continue;
    }

    const entry = {
      title: extractXmlNodeText(body, "title") || "Untitled",
      desc: extractXmlNodeText(body, "desc"),
      categories: extractXmlNodeTexts(body, "category").filter(Boolean),
      start: extractXmlAttribute(attributes, "start"),
      stop: extractXmlAttribute(attributes, "stop")
    };

    const aliases = channelAliases.get(channelId) || [channelId];
    if (includedKeys && !aliases.some((alias) => includedKeys.has(alias))) {
      continue;
    }

    for (const alias of aliases) {
      if (includedKeys && !includedKeys.has(alias)) {
        continue;
      }
      if (!guide.has(alias)) {
        guide.set(alias, []);
      }
      guide.get(alias).push({
        title: entry.title,
        desc: trimProgramDescription(entry.desc),
        start: entry.start,
        stop: entry.stop
      });
    }
  }

  for (const entries of guide.values()) {
    entries.sort((a, b) => a.start.localeCompare(b.start));
  }

  return Object.fromEntries(guide);
}

async function getPlaylistChannelKeySet(playlistUrl, streamHeaders = "") {
  if (!isHttpUrl(playlistUrl)) {
    return null;
  }

  const playlist = await fetchTextWithCache(playlistUrl, 60_000);
  const channels = parseM3uPlaylist(playlist, parseHeaderInput(streamHeaders || ""));
  const keys = new Set();
  channels.forEach((channel) => {
    (channel.keys || []).forEach((key) => {
      if (key) {
        keys.add(key);
      }
    });
  });
  return keys;
}

function parseM3uPlaylist(content, globalHeaders = {}) {
  const lines = content.split(/\r?\n/);
  const channels = [];
  let pendingMeta = null;
  let pendingHeaders = { ...globalHeaders };

  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed) {
      continue;
    }

    if (trimmed.startsWith("#EXTINF")) {
      const attributes = parseM3uAttributes(trimmed);
      const title = trimmed.includes(",") ? trimmed.slice(trimmed.indexOf(",") + 1).trim() : "Untitled Channel";
      pendingMeta = { attributes, title };
      pendingHeaders = { ...globalHeaders };
      continue;
    }

    if (trimmed.startsWith("#EXTVLCOPT")) {
      const option = parseVlcOption(trimmed);
      if (option) {
        mergeMappedHeader(pendingHeaders, option.key, option.value);
      }
      continue;
    }

    if (trimmed.startsWith("#KODIPROP")) {
      applyKodiPropHeaders(pendingHeaders, parseKodiProp(trimmed));
      continue;
    }

    if (trimmed.startsWith("#EXTHTTP:")) {
      const rawValue = trimmed.slice("#EXTHTTP:".length).trim();
      if (rawValue) {
        applyExtHttpHeaders(pendingHeaders, rawValue);
      }
      continue;
    }

    if (trimmed.startsWith("#")) {
      continue;
    }

    if (!pendingMeta) {
      continue;
    }

    const id = pendingMeta.attributes["tvg-id"] || pendingMeta.attributes["channel-id"] || pendingMeta.title;
    const channelNumber = resolveChannelNumber(pendingMeta.attributes, [pendingMeta.attributes["channel-id"], id]);
    channels.push({
      id,
      channelNumber,
      title: pendingMeta.title,
      logo: pendingMeta.attributes["tvg-logo"] || "",
      group: pendingMeta.attributes["group-title"] || "Unsorted",
      tvgId: pendingMeta.attributes["tvg-id"] || "",
      tvgName: pendingMeta.attributes["tvg-name"] || "",
      radio: pendingMeta.attributes.radio === "true",
      streamUrl: trimmed,
      streamProxyUrl: buildStreamProxyUrl(trimmed, pendingHeaders),
      requestHeaders: pendingHeaders,
      keys: Array.from(
        new Set([
          normalizeChannelKey(id),
          normalizeChannelKey(pendingMeta.title),
          normalizeChannelKey(pendingMeta.attributes["tvg-id"] || ""),
          normalizeChannelKey(pendingMeta.attributes["tvg-name"] || "")
        ].filter(Boolean))
      )
    });

    pendingMeta = null;
    pendingHeaders = { ...globalHeaders };
  }

  return channels;
}

function absolutizeUrl(value, baseUrl) {
  try {
    return new URL(value, baseUrl).toString();
  } catch {
    return value;
  }
}

function proxiedAssetUrl(value, baseUrl) {
  const absolute = absolutizeUrl(value, baseUrl);
  return buildStreamProxyUrl(absolute);
}

function rewriteManifestLine(line, baseUrl, requestHeaders = {}) {
  if (!line || line.startsWith("#")) {
    return line.replace(/URI="([^"]+)"/g, (_, uri) => `URI="${buildStreamProxyUrl(absolutizeUrl(uri, baseUrl), requestHeaders)}"`);
  }
  return buildStreamProxyUrl(absolutizeUrl(line, baseUrl), requestHeaders);
}

function copyHeaderIfPresent(sourceHeaders, targetHeaders, name) {
  const value = sourceHeaders.get(name);
  if (value) {
    targetHeaders[name] = value;
  }
}

async function handlePlaylist(req, res, url) {
  const playlistUrl = resolveConfigUrl(url.searchParams.get("url"), process.env.PLAYLIST_URL || DEFAULT_PLAYLIST_URL);
  const globalStreamHeaders = mergeHeaderSets(
    parseHeaderInput(process.env.STREAM_HEADERS || ""),
    parseHeaderInput(url.searchParams.get("stream_headers") || "")
  );
  if (!isHttpUrl(playlistUrl)) {
    send(res, 400, { error: "Missing or invalid playlist URL. Set PLAYLIST_URL or pass ?url=" }, "application/json; charset=utf-8");
    return;
  }

  try {
    const playlist = await fetchTextWithCache(playlistUrl, 60_000);
    const channels = parseM3uPlaylist(playlist, globalStreamHeaders);
    send(
      res,
      200,
      {
        source: playlistUrl,
        count: channels.length,
        groups: Array.from(new Set(channels.map((channel) => channel.group))).sort(),
        channels
      },
      "application/json; charset=utf-8"
    );
  } catch (error) {
    send(res, 502, { error: error.message }, "application/json; charset=utf-8");
  }
}

async function fetchBuffer(url) {
  const response = await fetch(url, {
    headers: {
      "user-agent": "ChannelSurfer/0.1",
      accept: "*/*"
    }
  });

  if (!response.ok) {
    throw new Error(`Upstream request failed with ${response.status}`);
  }

  return {
    contentType: response.headers.get("content-type") || "",
    buffer: Buffer.from(await response.arrayBuffer())
  };
}

function decodeXmlPayload(buffer, sourceUrl, contentType) {
  const looksGzipped =
    buffer.length > 2 &&
    buffer[0] === 0x1f &&
    buffer[1] === 0x8b;

  const shouldGunzip =
    looksGzipped ||
    sourceUrl.toLowerCase().includes(".gz") ||
    contentType.toLowerCase().includes("gzip");

  const decoded = shouldGunzip ? gunzipSync(buffer).toString("utf8") : buffer.toString("utf8");
  const text = decoded.replace(/^\uFEFF/, "").trim();
  const head = text.slice(0, 500).toLowerCase();

  if (!head.startsWith("<?xml") && !head.startsWith("<tv") && !head.startsWith("<!doctype tv")) {
    throw new Error("EPG source did not return XMLTV. The URL may require cookies/auth or may be serving HTML instead.");
  }

  return text;
}

async function handleEpg(req, res, url) {
  const epgUrl = resolveConfigUrl(url.searchParams.get("url"), process.env.EPG_URL || DEFAULT_EPG_URL);
  const playlistUrl = resolveConfigUrl(url.searchParams.get("playlist_url"), process.env.PLAYLIST_URL || DEFAULT_PLAYLIST_URL);
  if (!isHttpUrl(epgUrl)) {
    send(res, 400, { error: "Missing or invalid EPG URL. Set EPG_URL or pass ?url=" }, "application/json; charset=utf-8");
    return;
  }

  try {
    const includedKeys = await getPlaylistChannelKeySet(playlistUrl, process.env.STREAM_HEADERS || "");
    const cached = cache.get(epgUrl);
    const xml =
      cached && cached.expiresAt > Date.now()
        ? cached.value
        : (() => {
            return null;
          })();

    if (xml) {
      send(res, 200, { source: epgUrl, guide: parseXmltvToGuide(xml, { includedKeys }) }, "application/json; charset=utf-8");
      return;
    }

    const { buffer, contentType } = await fetchBuffer(epgUrl);
    const parsedXml = decodeXmlPayload(buffer, epgUrl, contentType);
    cache.set(epgUrl, { value: parsedXml, expiresAt: Date.now() + 5 * 60_000 });
    send(res, 200, { source: epgUrl, guide: parseXmltvToGuide(parsedXml, { includedKeys }) }, "application/json; charset=utf-8");
  } catch (error) {
    send(res, 502, { error: error.message }, "application/json; charset=utf-8");
  }
}

async function handleConfig(res) {
  send(
    res,
    200,
    {
      defaults: {
        playlistUrl: process.env.PLAYLIST_URL || DEFAULT_PLAYLIST_URL,
        epgUrl: process.env.EPG_URL || DEFAULT_EPG_URL,
        streamHeaders: process.env.STREAM_HEADERS || ""
      }
    },
    "application/json; charset=utf-8"
  );
}

async function handleWeather(req, res, url) {
  const latitude = Number(url.searchParams.get("lat"));
  const longitude = Number(url.searchParams.get("lon"));

  if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
    send(res, 400, { error: "Missing latitude or longitude" }, "application/json; charset=utf-8");
    return;
  }

  try {
    const forecastUrl = new URL("https://api.open-meteo.com/v1/forecast");
    forecastUrl.searchParams.set("latitude", String(latitude));
    forecastUrl.searchParams.set("longitude", String(longitude));
    forecastUrl.searchParams.set("current", "temperature_2m,weather_code,is_day");
    forecastUrl.searchParams.set("temperature_unit", "fahrenheit");
    forecastUrl.searchParams.set("timezone", "auto");

    const locationUrl = new URL("https://geocoding-api.open-meteo.com/v1/reverse");
    locationUrl.searchParams.set("latitude", String(latitude));
    locationUrl.searchParams.set("longitude", String(longitude));
    locationUrl.searchParams.set("count", "1");
    locationUrl.searchParams.set("language", "en");
    locationUrl.searchParams.set("format", "json");

    const [forecastResponse, locationResponse] = await Promise.all([
      fetch(forecastUrl, {
        headers: {
          "user-agent": "ChannelSurfer/0.1",
          accept: "application/json"
        }
      }),
      fetch(locationUrl, {
        headers: {
          "user-agent": "ChannelSurfer/0.1",
          accept: "application/json"
        }
      })
    ]);

    if (!forecastResponse.ok) {
      throw new Error(`Forecast lookup failed with ${forecastResponse.status}`);
    }

    const forecast = await forecastResponse.json();
    const location = locationResponse.ok ? await locationResponse.json() : null;
    const place = location?.results?.[0];
    const locationName = place ? [place.name, place.admin1].filter(Boolean).join(", ") : "";

    send(
      res,
      200,
      {
        current: {
          temperature: forecast.current?.temperature_2m ?? null,
          weatherCode: forecast.current?.weather_code ?? null,
          isDay: Boolean(forecast.current?.is_day)
        },
        location: locationName
      },
      "application/json; charset=utf-8"
    );
  } catch (error) {
    send(res, 502, { error: error.message }, "application/json; charset=utf-8");
  }
}

async function handleStream(req, res, url) {
  const target = url.searchParams.get("url");
  const forwardedHeaders = decodeStreamHeaders(url.searchParams.get("headers"));
  if (!isHttpUrl(target)) {
    send(res, 400, "Invalid stream URL");
    return;
  }

  try {
    const upstreamHeaders = {
      "user-agent": "ChannelSurfer/0.1",
      accept: "*/*",
      referer: new URL(target).origin
    };

    for (const [name, value] of Object.entries(forwardedHeaders)) {
      if (typeof value === "string" && value) {
        upstreamHeaders[name.toLowerCase()] = value;
      }
    }

    if (req.headers.range) {
      upstreamHeaders.range = req.headers.range;
    }

    const upstream = await fetch(target, {
      headers: upstreamHeaders
    });

    if (!upstream.ok) {
      const errorBody = await upstream.text().catch(() => "");
      const detail = errorBody ? ` ${errorBody.slice(0, 240)}` : "";
      send(res, 502, `Upstream stream failed with ${upstream.status}.${detail}`.trim());
      return;
    }

    if (!upstream.body) {
      send(res, 502, "Upstream stream had no response body");
      return;
    }

    const contentType = upstream.headers.get("content-type") || "";
    const isManifest =
      target.includes(".m3u8") ||
      contentType.includes("application/vnd.apple.mpegurl") ||
      contentType.includes("application/x-mpegURL");

    if (isManifest) {
      const manifest = await upstream.text();
      const rewritten = manifest
        .split(/\r?\n/)
        .map((line) => rewriteManifestLine(line, target, forwardedHeaders))
        .join("\n");

      res.writeHead(200, {
        "Content-Type": "application/vnd.apple.mpegurl",
        "Cache-Control": "no-store",
        "Access-Control-Allow-Origin": "*"
      });
      res.end(rewritten);
      return;
    }

    const responseHeaders = {
      "Content-Type": contentType || "application/octet-stream",
      "Cache-Control": "no-store",
      "Access-Control-Allow-Origin": "*"
    };
    copyHeaderIfPresent(upstream.headers, responseHeaders, "accept-ranges");
    copyHeaderIfPresent(upstream.headers, responseHeaders, "content-length");
    copyHeaderIfPresent(upstream.headers, responseHeaders, "content-range");

    res.writeHead(upstream.status, responseHeaders);

    Readable.fromWeb(upstream.body).pipe(res);
  } catch (error) {
    send(res, 502, `Proxy failed: ${error.message}`);
  }
}

async function handleImage(req, res, url) {
  const target = url.searchParams.get("url");
  if (!isHttpUrl(target)) {
    send(res, 400, "Invalid image URL");
    return;
  }

  try {
    const upstream = await fetch(target, {
      headers: {
        "user-agent": "ChannelSurfer/0.1",
        accept: "image/*,*/*"
      }
    });

    if (!upstream.ok || !upstream.body) {
      send(res, 502, `Upstream image failed with ${upstream.status}`);
      return;
    }

    res.writeHead(200, {
      "Content-Type": upstream.headers.get("content-type") || "image/png",
      "Cache-Control": "public, max-age=3600",
      "Access-Control-Allow-Origin": "*"
    });

    Readable.fromWeb(upstream.body).pipe(res);
  } catch (error) {
    send(res, 502, `Image proxy failed: ${error.message}`);
  }
}

async function serveStatic(req, res, url) {
  const pathname = url.pathname === "/" ? "/index.html" : url.pathname;
  const normalizedPath = normalize(join(publicDir, pathname));

  if (!normalizedPath.startsWith(publicDir)) {
    send(res, 403, "Forbidden");
    return;
  }

  try {
    const buffer = await readFile(normalizedPath);
    res.writeHead(200, {
      "Content-Type": getMimeType(normalizedPath),
      "Cache-Control": BINARY_EXTENSIONS.has(extname(normalizedPath).toLowerCase()) ? "public, max-age=86400" : "no-store"
    });
    res.end(buffer);
  } catch {
    send(res, 404, "Not found");
  }
}

async function routeRequest(req, res) {
  await ensureEnvLoaded();

  if (!req.url) {
    send(res, 400, "Bad request");
    return;
  }

  if (req.method === "OPTIONS") {
    res.writeHead(204, {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Headers": "Content-Type",
      "Access-Control-Allow-Methods": "GET, OPTIONS"
    });
    res.end();
    return;
  }

  const url = new URL(req.url, `http://${req.headers.host || "localhost"}`);

  if (url.pathname === "/api/config") {
    await handleConfig(res);
    return;
  }

  if (url.pathname === "/api/playlist") {
    await handlePlaylist(req, res, url);
    return;
  }

  if (url.pathname === "/api/epg") {
    await handleEpg(req, res, url);
    return;
  }

  if (url.pathname === "/api/stream") {
    await handleStream(req, res, url);
    return;
  }

  if (url.pathname === "/api/weather") {
    await handleWeather(req, res, url);
    return;
  }

  if (url.pathname === "/api/image") {
    await handleImage(req, res, url);
    return;
  }

  await serveStatic(req, res, url);
}

if (process.argv[1] && normalize(process.argv[1]) === moduleFilename) {
  if (!isServerlessRuntime) {
    createServer(async (req, res) => {
      await routeRequest(req, res);
    }).listen(port, () => {
      process.stdout.write(`Channel Surfer running at http://localhost:${port}\n`);
    });
  }
}

export { routeRequest };
