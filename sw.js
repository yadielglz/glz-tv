// GLZ TV Service Worker
// Version: 1.0.0
// Provides offline functionality and caching for PWA features

const CACHE_NAME = 'glz-tv-v1.0.0';
const STATIC_CACHE_NAME = 'glz-tv-static-v1.0.0';
const DYNAMIC_CACHE_NAME = 'glz-tv-dynamic-v1.0.0';

// Static assets to cache immediately
const STATIC_ASSETS = [
  '/',
  '/index.html',
  '/styles.css',
  '/app.js',
  '/manifest.json',
  'https://fonts.googleapis.com/css2?family=Google+Sans:wght@300;400;500;700&display=swap',
  'https://cdn.jsdelivr.net/npm/hls.js@1.5.7/dist/hls.min.js'
];

// Assets to cache on demand
const DYNAMIC_ASSETS = [
  '/api/epg',
  '/api/health'
];

// Install event - cache static assets
self.addEventListener('install', (event) => {
  console.log('Service Worker: Installing...');
  
  event.waitUntil(
    caches.open(STATIC_CACHE_NAME)
      .then((cache) => {
        console.log('Service Worker: Caching static assets');
        return cache.addAll(STATIC_ASSETS);
      })
      .then(() => {
        console.log('Service Worker: Static assets cached successfully');
        return self.skipWaiting();
      })
      .catch((error) => {
        console.error('Service Worker: Failed to cache static assets', error);
      })
  );
});

// Activate event - clean up old caches
self.addEventListener('activate', (event) => {
  console.log('Service Worker: Activating...');
  
  event.waitUntil(
    caches.keys()
      .then((cacheNames) => {
        return Promise.all(
          cacheNames.map((cacheName) => {
            if (cacheName !== STATIC_CACHE_NAME && 
                cacheName !== DYNAMIC_CACHE_NAME &&
                cacheName.startsWith('glz-tv-')) {
              console.log('Service Worker: Deleting old cache', cacheName);
              return caches.delete(cacheName);
            }
          })
        );
      })
      .then(() => {
        console.log('Service Worker: Activated successfully');
        return self.clients.claim();
      })
  );
});

// Fetch event - serve from cache with network fallback
self.addEventListener('fetch', (event) => {
  const { request } = event;
  const url = new URL(request.url);
  
  // Skip non-GET requests
  if (request.method !== 'GET') {
    return;
  }
  
  // Skip chrome-extension and other non-http requests
  if (!url.protocol.startsWith('http')) {
    return;
  }
  
  event.respondWith(
    handleFetch(request)
  );
});

async function handleFetch(request) {
  const url = new URL(request.url);
  
  try {
    // Strategy 1: Never cache video segments - always fetch fresh
    if (isVideoSegment(url)) {
      console.log('Service Worker: Fetching video segment fresh (no cache):', url.pathname);
      return await fetch(request);
    }
    
    // Strategy 2: Cache First for static assets
    if (isStaticAsset(url)) {
      return await cacheFirst(request, STATIC_CACHE_NAME);
    }
    
    // Strategy 3: Network First for API calls
    if (isApiCall(url)) {
      return await networkFirst(request, DYNAMIC_CACHE_NAME);
    }
    
    // Strategy 4: Stale While Revalidate for other requests
    return await staleWhileRevalidate(request, DYNAMIC_CACHE_NAME);
    
  } catch (error) {
    console.error('Service Worker: Fetch error', error);
    
    // For video segments, don't fallback to cache - let the error propagate
    if (isVideoSegment(url)) {
      throw error;
    }
    
    // Fallback: try to serve from cache for non-video requests
    const cachedResponse = await caches.match(request);
    if (cachedResponse) {
      return cachedResponse;
    }
    
    // Last resort: return offline page for navigation requests
    if (request.mode === 'navigate') {
      return await caches.match('/index.html');
    }
    
    throw error;
  }
}

// Cache First Strategy
async function cacheFirst(request, cacheName) {
  const cachedResponse = await caches.match(request);
  if (cachedResponse) {
    return cachedResponse;
  }
  
  const networkResponse = await fetch(request);
  if (networkResponse.ok) {
    const cache = await caches.open(cacheName);
    cache.put(request, networkResponse.clone());
  }
  
  return networkResponse;
}

// Network First Strategy
async function networkFirst(request, cacheName) {
  try {
    const networkResponse = await fetch(request);
    if (networkResponse.ok) {
      const cache = await caches.open(cacheName);
      cache.put(request, networkResponse.clone());
    }
    return networkResponse;
  } catch (error) {
    const cachedResponse = await caches.match(request);
    if (cachedResponse) {
      return cachedResponse;
    }
    throw error;
  }
}

// Stale While Revalidate Strategy
async function staleWhileRevalidate(request, cacheName) {
  const cachedResponse = await caches.match(request);
  
  const fetchPromise = fetch(request).then((networkResponse) => {
    if (networkResponse.ok) {
      const cache = caches.open(cacheName);
      cache.then(c => c.put(request, networkResponse.clone()));
    }
    return networkResponse;
  }).catch(() => {
    // Network failed, return cached if available
    return cachedResponse;
  });
  
  return cachedResponse || fetchPromise;
}

// Helper functions
function isStaticAsset(url) {
  return STATIC_ASSETS.some(asset => url.href.includes(asset)) ||
         url.pathname.endsWith('.css') ||
         url.pathname.endsWith('.js') ||
         url.pathname.endsWith('.html') ||
         url.pathname.endsWith('.json') ||
         url.hostname === 'fonts.googleapis.com' ||
         url.hostname === 'cdn.jsdelivr.net';
}

function isVideoSegment(url) {
  return url.pathname.includes('.m3u8') ||
         url.pathname.includes('.ts') ||
         url.pathname.includes('.mp4') ||
         url.pathname.includes('.webm') ||
         url.pathname.includes('.mp3') ||
         url.pathname.includes('.aac') ||
         url.hostname.includes('starlite.best') ||
         url.hostname.includes('stream') ||
         url.hostname.includes('livetv');
}

function isApiCall(url) {
  return url.pathname.startsWith('/api/') ||
         url.pathname.includes('epg') ||
         url.pathname.includes('weather');
}

// Background sync for offline actions
self.addEventListener('sync', (event) => {
  console.log('Service Worker: Background sync triggered', event.tag);
  
  if (event.tag === 'background-sync') {
    event.waitUntil(doBackgroundSync());
  }
});

async function doBackgroundSync() {
  try {
    // Sync any pending actions when back online
    console.log('Service Worker: Performing background sync');
    
    // You can add specific sync logic here
    // For example, sync EPG data, user preferences, etc.
    
  } catch (error) {
    console.error('Service Worker: Background sync failed', error);
  }
}

// Push notifications (if needed in the future)
self.addEventListener('push', (event) => {
  console.log('Service Worker: Push notification received');
  
  const options = {
    body: event.data ? event.data.text() : 'GLZ TV Update',
    icon: '/manifest.json',
    badge: '/manifest.json',
    vibrate: [100, 50, 100],
    data: {
      dateOfArrival: Date.now(),
      primaryKey: 1
    },
    actions: [
      {
        action: 'explore',
        title: 'Open App',
        icon: '/manifest.json'
      },
      {
        action: 'close',
        title: 'Close',
        icon: '/manifest.json'
      }
    ]
  };
  
  event.waitUntil(
    self.registration.showNotification('GLZ TV', options)
  );
});

// Notification click handler
self.addEventListener('notificationclick', (event) => {
  console.log('Service Worker: Notification clicked');
  
  event.notification.close();
  
  if (event.action === 'explore') {
    event.waitUntil(
      clients.openWindow('/')
    );
  }
});

// Message handler for communication with main thread
self.addEventListener('message', (event) => {
  console.log('Service Worker: Message received', event.data);
  
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }
  
  if (event.data && event.data.type === 'GET_VERSION') {
    event.ports[0].postMessage({ version: CACHE_NAME });
  }
  
  if (event.data && event.data.type === 'CLEAR_VIDEO_CACHE') {
    console.log('Service Worker: Clearing video cache...');
    clearVideoCache();
  }
});

// Function to clear video-related cache entries
async function clearVideoCache() {
  try {
    const cacheNames = await caches.keys();
    const videoCacheNames = cacheNames.filter(name => 
      name.includes('video') || 
      name.includes('segment') || 
      name.includes('hls')
    );
    
    await Promise.all(videoCacheNames.map(cacheName => {
      console.log('Service Worker: Deleting video cache:', cacheName);
      return caches.delete(cacheName);
    }));
    
    console.log('Service Worker: Video cache cleared successfully');
  } catch (error) {
    console.error('Service Worker: Failed to clear video cache:', error);
  }
}

console.log('Service Worker: Loaded successfully');
