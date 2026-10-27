// GLZ Hub Service Worker
const CACHE_NAME = "glzhub-v3.0.4";
const PRECACHE_ASSETS = [
  "/",
  "/index.html",
  "/styles.css?v=3.0.4",
  "/app.js?v=3.0.4",
  "/live-tv.js?v=3.0.4",
  "/manifest.json"
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(PRECACHE_ASSETS)).then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.map((key) => {
        if (key !== CACHE_NAME) return caches.delete(key);
      }))
    ).then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (event) => {
  const url = new URL(event.request.url);

  // Media segments and Google Cast must use their normal network behavior.
  // Never store third-party streams or return the HTML shell for failed media.
  if (event.request.method !== "GET" || url.origin !== self.location.origin ||
      !["/", "/index.html", "/styles.css", "/app.js", "/live-tv.js", "/manifest.json"].includes(url.pathname)) return;

  // Do not cache API or auth calls via Service Worker; let Worker & Edge caching handle them
  if (url.pathname.startsWith("/api/") || url.pathname.startsWith("/auth/")) {
    return;
  }

  // Network-first strategy for app shell assets, falling back to cache
  event.respondWith(
    fetch(event.request)
      .then((response) => {
        if (response && response.status === 200 && event.request.method === "GET") {
          const clone = response.clone();
          caches.open(CACHE_NAME).then((cache) => cache.put(event.request, clone));
        }
        return response;
      })
      .catch(() => caches.match(event.request).then((match) => match || caches.match("/")))
  );
});
