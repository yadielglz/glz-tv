// GLZ TV - Modern STB App Logic
// Spectrum/AT&T inspired, modular, and maintainable

// --- Embedded M3U Content (move this to a separate file or fetch from server for production) ---
const EMBEDDED_M3U = `#EXTM3U url-tvg="https://epg.best/1eef8-shw7fc.xml.gz" x-tvg-url="https://epg.best/1eef8-shw7fc.xml.gz"
#EXTINF:-1 tvg-id="WKAQ.us" tvg-name="WKAQ   TELEMUNDO PR" tvg-logo="https://i.ibb.co/gLVK5Swz/TEL.png" tvg-chno="102" channel-id="102" group-title="TV",WKAQ   TELEMUNDO PR
https://nbculocallive.akamaized.net/hls/live/2037499/puertorico/stream1/master_1080.m3u8
#EXTINF:-1 tvg-id="WKAQDT2.us" tvg-name="WKAQ2   PUNTO 2" tvg-logo="https://static.epg.best/us/WKAQDT2.us.png" tvg-chno="103" channel-id="103" group-title="TV",WKAQ2   PUNTO 2
https://nbculocallive.akamaized.net/hls/live/2037499/puertorico/stream2/master_720.m3u8
#EXTINF:-1 tvg-id="UniMas.us" tvg-name="UNIMAS HD" tvg-logo="https://static.epg.best/us/UniMas.us.png" tvg-chno="104" channel-id="104" group-title="TV",UNIMAS HD
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/unimas.network.east.us.m3u8
#EXTINF:-1 tvg-id="WLII.us" tvg-name="WLII   TELEONCE" tvg-logo="https://static.epg.best/us/WLII.us.png" tvg-chno="105" channel-id="105" group-title="TV",WLII   TELEONCE
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/uni.wlii.puerto.rico.us.m3u8
#EXTINF:-1 tvg-id="WIPRTV.us" tvg-name="WIPR   PUERTO RICO TV" tvg-logo="https://static.epg.best/us/WIPRTV.us.png" tvg-chno="106" channel-id="106" group-title="TV",WIPR   PUERTO RICO TV
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/wipr.pr.m3u8
#EXTINF:-1 tvg-id="ESPN.us" tvg-name="ESPN HD" tvg-logo="https://i.ibb.co/mrs1mDXw/ESPN-png.png" tvg-chno="110" channel-id="110" group-title="TV",ESPN HD
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/espn.us.m3u8
#EXTINF:-1 tvg-id="ESPN2.us" tvg-name="ESPN 2 HD" tvg-logo="https://static.epg.best/us/ESPN2.us.png" tvg-chno="111" channel-id="111" group-title="TV",ESPN 2 HD
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/espn2.us.m3u8
#EXTINF:-1 tvg-id="ESPNNews.us" tvg-name="ESPN NEWS HD" tvg-logo="https://static.epg.best/us/ESPNNews.us.png" tvg-chno="112" channel-id="112" group-title="TV",ESPN NEWS HD
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/espn.news.us.m3u8
#EXTINF:-1 tvg-id="ESPNU.us" tvg-name="ESPN U HD" tvg-logo="https://static.epg.best/us/ESPNU.us.png" tvg-chno="113" channel-id="113" group-title="TV",ESPN U HD
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/espn.u.us.m3u8
#EXTINF:-1 tvg-id="FoxSports1.us" tvg-name="FOX SPORTS 1" tvg-logo="https://i.ibb.co/0pqwYytp/FS1.png" tvg-chno="114" channel-id="114" group-title="TV",FOX SPORTS 1
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/fox.sports.1.us.m3u8
#EXTINF:-1 tvg-id="FoxSports2.us" tvg-name="FOX SPORTS 2" tvg-logo="https://i.ibb.co/PGYdrN3r/FS2.png" tvg-chno="115" channel-id="115" group-title="TV",FOX SPORTS 2
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/fox.sports.2.us.m3u8
#EXTINF:-1 tvg-id="MLBNetwork.us" tvg-name="MLB NETWORK" tvg-logo="https://static.epg.best/us/MLBNetwork.us.png" tvg-chno="116" channel-id="116" group-title="TV",MLB NETWORK
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/mlb.network.us.m3u8
#EXTINF:-1 tvg-id="NFLNetwork.us" tvg-name="NFL NETWORK" tvg-logo="https://static.epg.best/us/NFLNetwork.us.png" tvg-chno="117" channel-id="117" group-title="TV",NFL NETWORK
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/nfl.network.us.m3u8
#EXTINF:-1 tvg-id="NBATV.us" tvg-name="NBA TV" tvg-logo="https://static.epg.best/us/NBATV.us.png" tvg-chno="118" channel-id="118" group-title="TV",NBA TV
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/nba.tv.usa.us.m3u8
#EXTINF:-1 tvg-id="YESNetwork.us" tvg-name="YES NETWORK" tvg-logo="https://static.epg.best/us/YESNetwork.us.png" tvg-chno="119" channel-id="119" group-title="TV",YES NETWORK
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/yes.network.us.m3u8
#EXTINF:-1 tvg-id="SportsNetNewYork.us" tvg-name="SPORTSNET NEW YORK" tvg-logo="https://static.epg.best/us/SportsNetNewYork.us.png" tvg-chno="120" channel-id="120" group-title="TV",SPORTSNET NEW YORK
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/sny.sportsnet.new.york.us.m3u8
#EXTINF:-1 tvg-id="WESH.us" tvg-name="WESH   NBC ORLANDO" tvg-logo="https://static.epg.best/us/WESH.us.png" tvg-chno="121" channel-id="121" group-title="TV",WESH   NBC ORLANDO
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/nbc.wesh.winter.park.fl.us.m3u8
#EXTINF:-1 tvg-id="WKMG.us" tvg-name="WKMG   CBS ORLANDO" tvg-logo="https://static.epg.best/us/WKMG.us.png" tvg-chno="122" channel-id="122" group-title="TV",WKMG   CBS ORLANDO
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/cbs.wkmg.orlando.fl.us.m3u8
#EXTINF:-1 tvg-id="WFTV.us" tvg-name="WFTV   ABC ORLANDO" tvg-logo="https://static.epg.best/us/WFTV.us.png" tvg-chno="123" channel-id="123" group-title="TV",WFTV   ABC ORLANDO
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/abc.wftv.orlando.fl.us.m3u8
#EXTINF:-1 tvg-id="WOFL.us" tvg-name="WOFL   FOX ORLANDO" tvg-logo="https://static.epg.best/us/WOFL.us.png" tvg-chno="124" channel-id="124" group-title="TV",WOFL   FOX ORLANDO
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/fox.wofl.orlando.fl.us.m3u8
#EXTINF:-1 tvg-id="WKCF.us" tvg-name="WKCF   CW 18 ORLANDO" tvg-logo="https://i.ibb.co/cK2GwV7r/CW18-Clear.png" tvg-chno="125" channel-id="125" group-title="TV",WKCF   CW 18 ORLANDO
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/cw.wkcf.orlando.fl.us.m3u8
`;

// --- M3U Parser ---
function parseM3U(m3uContent) {
  const channels = [];
  const lines = m3uContent.split('\n');
  let currentChannel = null;
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();
    if (line.startsWith('#EXTINF:')) {
      currentChannel = {};
      const lastCommaIndex = line.lastIndexOf(',');
      if (lastCommaIndex !== -1) {
        currentChannel.name = line.substring(lastCommaIndex + 1).trim();
      }
      const logoMatch = line.match(/tvg-logo="([^"]*)"/);
      if (logoMatch) currentChannel.logo = logoMatch[1];
      const chnoMatch = line.match(/tvg-chno="([^"]*)"/);
      if (chnoMatch) currentChannel.chno = chnoMatch[1];
      const idMatch = line.match(/tvg-id="([^"]*)"/);
      if (idMatch) currentChannel.id = idMatch[1];
      const nameMatch = line.match(/tvg-name="([^"]*)"/);
      if (nameMatch && !currentChannel.name) currentChannel.name = nameMatch[1];
    } else if (line && !line.startsWith('#') && currentChannel) {
      currentChannel.url = line.trim();
      channels.push(currentChannel);
      currentChannel = null;
    }
  }
  return channels;
}

// --- State ---
let channels = [];
let current = 0;
let standby = true;
let hls = null;
let multiviewMode = false;
let multiviewStreams = [];

// --- DOM Elements ---
const video = document.getElementById('video');
const audio = document.getElementById('audio');
const channelNumber = document.getElementById('channel-number');
const channelName = document.getElementById('channel-name');
const timeDisplay = document.getElementById('time-display');
const dateDisplay = document.getElementById('date-display');
const standbyScreen = document.getElementById('standby-screen');
const channelBanner = document.getElementById('channel-banner');
const bannerChannel = document.getElementById('banner-channel');
const bannerName = document.getElementById('banner-name');
const progressBar = document.getElementById('progress-bar');
const channelGuide = document.getElementById('channel-guide');
const channelList = document.getElementById('channel-list');
const closeGuide = document.getElementById('close-guide');
const remoteControl = document.getElementById('remote-control');
const toggleRemote = document.getElementById('toggle-remote');
const powerBtn = document.getElementById('power-btn');
const guideBtn = document.getElementById('guide-btn');
const multiviewBtn = document.getElementById('multiview-btn');
const numpadBtns = document.querySelectorAll('.numpad-btn');
const prevBtn = document.getElementById('prev-btn');
const nextBtn = document.getElementById('next-btn');
const statusMessage = document.getElementById('status-message');
const loadingIndicator = document.getElementById('loading-indicator');
const multiviewGrid = document.getElementById('multiview-grid');
const videoContainer = document.getElementById('video-container');
const helpOverlay = document.getElementById('help-overlay');
const closeHelp = document.getElementById('close-help');
const helpBody = document.getElementById('help-body');
const searchInput = document.getElementById('search-input');

// --- Utility Functions ---
function showStatus(msg, duration = 2000) {
  statusMessage.textContent = msg;
  statusMessage.style.display = '';
  setTimeout(() => {
    statusMessage.style.display = 'none';
  }, duration);
}
function showLoading() {
  loadingIndicator.style.display = 'flex';
}
function hideLoading() {
  loadingIndicator.style.display = 'none';
}
function updateTime() {
  const now = new Date();
  const hours = now.getHours().toString().padStart(2, '0');
  const minutes = now.getMinutes().toString().padStart(2, '0');
  timeDisplay.textContent = `${hours}:${minutes}`;
  dateDisplay.textContent = now.toLocaleDateString(undefined, { month: 'short', day: '2-digit' }).toUpperCase();
}
setInterval(updateTime, 1000);

function updateChannelDisplay(idx = current) {
  if (!channels.length || !channels[idx]) {
    channelNumber.textContent = '000';
    channelName.textContent = 'No Channel';
    return;
  }
  channelNumber.textContent = channels[idx].chno ? channels[idx].chno : (idx + 1).toString().padStart(3, '0');
  channelName.textContent = channels[idx].name;
}

function stopPlayback() {
  if (hls) { hls.destroy(); hls = null; }
  video.pause();
  video.src = '';
  audio.pause();
  audio.src = '';
}

function stopMultiviewStreams() {
  multiviewStreams.forEach(stream => {
    if (stream && stream.destroy) {
      stream.destroy();
    }
  });
  multiviewStreams = [];
  
  // Clear all multiview video elements
  const multiviewVideos = multiviewGrid.querySelectorAll('video');
  multiviewVideos.forEach(video => {
    video.pause();
    video.src = '';
  });
}

function startMultiview() {
  if (standby) {
    showStatus('Power on first.');
    return;
  }
  
  multiviewMode = true;
  stopPlayback(); // Stop single video
  multiviewGrid.style.display = '';
  video.style.display = 'none';
  audio.style.display = 'none';
  
  // Start 4 streams (channels 0-3)
  const startChannel = Math.max(0, current - 1); // Start from current channel - 1
  const multiviewVideos = multiviewGrid.querySelectorAll('video');
  
  for (let i = 0; i < 4; i++) {
    const channelIndex = (startChannel + i) % channels.length;
    const videoElement = multiviewVideos[i];
    const cell = multiviewGrid.children[i];
    
    if (channels[channelIndex] && videoElement) {
      const ch = channels[channelIndex];
      const isAudio = /\.(mp3|aac|m4a|ogg|flac|wav)(\?|$)/i.test(ch.url) || ch.url.includes('icy') || ch.url.includes('audio');
      
      if (isAudio) {
        // Skip audio channels in multiview
        continue;
      }
      
      const isHls = /\.m3u8(\?|$)/i.test(ch.url);
      
      if (isHls && window.Hls) {
        const hlsInstance = new Hls();
        hlsInstance.loadSource(ch.url);
        hlsInstance.attachMedia(videoElement);
        hlsInstance.on(Hls.Events.ERROR, function (event, data) {
          if (data.fatal) {
            console.error(`Multiview stream error for channel ${channelIndex}:`, data);
          }
        });
        multiviewStreams.push(hlsInstance);
      } else {
        videoElement.src = ch.url;
      }
      
      // Update cell overlay
      const overlay = cell.querySelector('.cell-overlay');
      if (overlay) {
        const numberEl = overlay.querySelector('.cell-channel');
        const nameEl = overlay.querySelector('.cell-name');
        if (numberEl) numberEl.textContent = ch.chno || (channelIndex + 1);
        if (nameEl) nameEl.textContent = ch.name;
      }
      
      // Set active state
      cell.classList.toggle('active', channelIndex === current);
      
      // Add click handler to switch to this channel
      cell.onclick = () => {
        current = channelIndex;
        updateChannelDisplay();
        // Update active state
        multiviewGrid.querySelectorAll('.multiview-cell').forEach(c => c.classList.remove('active'));
        cell.classList.add('active');
      };
      
      videoElement.play().catch(e => {
        console.error(`Failed to play multiview stream ${channelIndex}:`, e);
      });
    }
  }
}

function stopMultiview() {
  multiviewMode = false;
  stopMultiviewStreams();
  multiviewGrid.style.display = 'none';
  video.style.display = '';
  audio.style.display = '';
  
  // Resume single channel playback
  if (!standby && channels.length > 0 && current >= 0 && current < channels.length) {
    playChannel(current);
  }
}

function playChannel(idx) {
  if (!channels[idx]) return;
  current = idx;
  
  // If in multiview mode, just update the display and active cell
  if (multiviewMode) {
    updateChannelDisplay();
    multiviewGrid.querySelectorAll('.multiview-cell').forEach((cell, i) => {
      const channelIndex = (Math.max(0, current - 1) + i) % channels.length;
      cell.classList.toggle('active', channelIndex === current);
    });
    return;
  }
  
  stopPlayback();
  const ch = channels[idx];
  const isAudio = /\.(mp3|aac|m4a|ogg|flac|wav)(\?|$)/i.test(ch.url) || ch.url.includes('icy') || ch.url.includes('audio');
  const isHls = /\.m3u8(\?|$)/i.test(ch.url);
  video.style.display = isAudio ? 'none' : '';
  audio.style.display = isAudio ? '' : 'none';
  if (isAudio) {
    audio.src = ch.url;
    audio.play().catch(e => showStatus('Failed to play audio stream'));
  } else if (isHls && window.Hls) {
    hls = new Hls();
    hls.loadSource(ch.url);
    hls.attachMedia(video);
    hls.on(Hls.Events.ERROR, function (event, data) {
      if (data.fatal) showStatus('Stream error.');
    });
    video.play().catch(e => showStatus('Failed to play video stream'));
  } else {
    video.src = ch.url;
    video.play().catch(e => showStatus('Failed to play video stream'));
  }
  updateChannelDisplay();
  showChannelBanner();
}

function setStandby(on) {
  standby = on;
  standbyScreen.style.display = standby ? '' : 'none';
  if (standby) {
    stopPlayback();
    if (multiviewMode) {
      stopMultiview();
    }
    channelNumber.textContent = '-- --';
    channelName.textContent = 'STANDBY';
  } else {
    if (channels.length > 0 && current >= 0 && current < channels.length) {
      playChannel(current);
    } else {
      updateChannelDisplay();
    }
  }
}

function showChannelBanner() {
  if (!channels[current]) return;
  bannerChannel.textContent = channels[current].chno || (current + 1);
  bannerName.textContent = channels[current].name;
  channelBanner.classList.add('show');
  setTimeout(() => channelBanner.classList.remove('show'), 2500);
}

function showGuide() {
  channelGuide.classList.add('open');
  renderChannelList();
  searchInput.value = '';
}
function hideGuide() {
  channelGuide.classList.remove('open');
}
function renderChannelList(filter = '') {
  const filtered = filter
    ? channels.filter(ch => ch.name.toLowerCase().includes(filter.toLowerCase()) || (ch.chno && ch.chno.includes(filter)))
    : channels;
  channelList.innerHTML = filtered.map((ch, i) =>
    `<div class="channel-item${i === current ? ' active' : ''}" data-idx="${i}" tabindex="0">
      <img class="channel-logo" src="${ch.logo || ''}" onerror="this.style.display='none'" />
      <div class="channel-details">
        <div class="channel-number-small">${ch.chno || (i + 1)}</div>
        <div class="channel-name-small">${ch.name}</div>
      </div>
    </div>`
  ).join('');
  channelList.querySelectorAll('.channel-item').forEach(item => {
    item.onclick = () => {
      playChannel(Number(item.dataset.idx));
      hideGuide();
    };
    item.onkeydown = (e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        playChannel(Number(item.dataset.idx));
        hideGuide();
      }
    };
  });
}

// --- Event Listeners ---
powerBtn.onclick = () => setStandby(!standby);
guideBtn.onclick = showGuide;
closeGuide.onclick = hideGuide;
toggleRemote.onclick = () => remoteControl.classList.toggle('visible');
multiviewBtn.onclick = () => {
  if (multiviewMode) {
    stopMultiview();
  } else {
    startMultiview();
  }
};
prevBtn.onclick = () => {
  if (!standby && channels.length > 0) {
    playChannel((current - 1 + channels.length) % channels.length);
  }
};
nextBtn.onclick = () => {
  if (!standby && channels.length > 0) {
    playChannel((current + 1) % channels.length);
  }
};
searchInput.oninput = (e) => renderChannelList(e.target.value);

numpadBtns.forEach(btn => {
  btn.onclick = () => {
    if (standby) {
      showStatus('Power on first.');
      return;
    }
    // Simple channel number entry
    let num = btn.dataset.num;
    let entry = channelNumber.textContent.replace(/[^0-9]/g, '') + num;
    if (entry.length > 3) entry = entry.slice(-3);
    channelNumber.textContent = entry.padStart(3, '0');
    let idx = channels.findIndex(ch => ch.chno === entry);
    if (idx === -1 && parseInt(entry, 10) > 0 && parseInt(entry, 10) <= channels.length) {
      idx = parseInt(entry, 10) - 1;
    }
    if (idx >= 0) {
      setTimeout(() => playChannel(idx), 600);
    } else {
      setTimeout(() => updateChannelDisplay(), 1200);
    }
  };
});

// Keyboard navigation
window.addEventListener('keydown', (e) => {
  if (e.key === 'F1') remoteControl.classList.toggle('visible');
  if (e.key === 'F2') showGuide();
  if (e.key === 'F3') {
    if (multiviewMode) {
      stopMultiview();
    } else {
      startMultiview();
    }
  }
  if (e.key === 'F4') setStandby(!standby);
  if (e.key === 'ArrowUp') showGuide();
  if (e.key === 'ArrowLeft') prevBtn.onclick();
  if (e.key === 'ArrowRight') nextBtn.onclick();
  if (e.key === 'Escape') {
    remoteControl.classList.remove('visible');
    hideGuide();
    helpOverlay.style.display = 'none';
  }
  if (e.key === '?') helpOverlay.style.display = 'flex';
});

closeHelp.onclick = () => helpOverlay.style.display = 'none';

// --- Initialization ---
(function init() {
  channels = parseM3U(EMBEDDED_M3U);
  if (channels.length > 0) current = 0;
  setStandby(true);
  updateTime();
  updateChannelDisplay();
  remoteControl.classList.add('visible');
  // Help overlay content
  helpBody.innerHTML = `
    <div><kbd>F1</kbd>: Toggle Remote</div>
    <div><kbd>F2</kbd>: Show Guide</div>
    <div><kbd>F3</kbd>: Toggle Multiview</div>
    <div><kbd>F4</kbd>: Power On/Off</div>
    <div><kbd>←/→</kbd>: Prev/Next Channel</div>
    <div><kbd>↑</kbd>: Show Guide</div>
    <div><kbd>?</kbd>: Show Help</div>
    <div><kbd>ESC</kbd>: Hide Remote/Guide/Help</div>
  `;
})(); 