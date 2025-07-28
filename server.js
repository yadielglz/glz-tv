const express = require('express');
const cors = require('cors');
const fetch = require('node-fetch');
const path = require('path');

const app = express();
const PORT = 3000;

// Enable CORS for all routes
app.use(cors());

// Serve static files
app.use(express.static('.'));

// EPG Proxy endpoint
app.get('/api/epg', async (req, res) => {
  try {
    const epgUrl = 'https://epg.best/1eef8-shw7fc.xml.gz';
    
    console.log('Fetching EPG from:', epgUrl);
    
    const response = await fetch(epgUrl, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        'Accept': 'application/xml, text/xml, */*'
      },
      timeout: 30000
    });
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    const xmlData = await response.text();
    
    res.setHeader('Content-Type', 'application/xml');
    res.setHeader('Cache-Control', 'public, max-age=1800'); // Cache for 30 minutes
    res.send(xmlData);
    
    console.log('EPG data sent successfully, length:', xmlData.length);
    
  } catch (error) {
    console.error('EPG fetch error:', error);
    res.status(500).json({ error: 'Failed to fetch EPG data', details: error.message });
  }
});

// Health check endpoint
app.get('/api/health', (req, res) => {
  res.json({ status: 'OK', timestamp: new Date().toISOString() });
});

app.listen(PORT, () => {
  console.log(`🚀 EPG Proxy Server running on http://localhost:${PORT}`);
  console.log(`📺 EPG endpoint: http://localhost:${PORT}/api/epg`);
  console.log(`🏥 Health check: http://localhost:${PORT}/api/health`);
}); 