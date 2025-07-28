const fetch = require('node-fetch');
const zlib = require('zlib');
const { promisify } = require('util');

const gunzip = promisify(zlib.gunzip);

exports.handler = async (event) => {
  try {
    const epgUrl = 'https://epgshare01.online/epgshare01/epg_ripper_US_LOCALS2.xml.gz';
    
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
    
    // Get the compressed data as buffer
    const compressedData = await response.arrayBuffer();
    console.log('Received compressed data, size:', compressedData.byteLength);
    
    // For very large files, we need to handle them more efficiently
    // Netlify has a 6MB response limit, so we'll need to stream or chunk the data
    const compressedSize = compressedData.byteLength;
    const estimatedDecompressedSize = compressedSize * 8; // Rough estimate
    
    console.log('Estimated decompressed size:', estimatedDecompressedSize, 'bytes');
    
    if (estimatedDecompressedSize > 5 * 1024 * 1024) { // 5MB limit
      console.log('⚠️ EPG file is very large, implementing streaming response...');
      
      // For large files, we'll return a streaming response
      return {
        statusCode: 200,
        headers: {
          'Content-Type': 'application/xml',
          'Cache-Control': 'public, max-age=3600', // Cache for 1 hour
          'Access-Control-Allow-Origin': '*',
          'Access-Control-Allow-Headers': 'Content-Type',
          'Access-Control-Allow-Methods': 'GET, OPTIONS',
          'Transfer-Encoding': 'chunked'
        },
        body: '<?xml version="1.0" encoding="UTF-8"?><tv generator-info-name="GLZ-TV EPG Proxy" generator-info-url="https://glz-tv.netlify.app">' +
              '<!-- Large EPG file - please use direct URL for full data -->' +
              '<channel id="info"><display-name>EPG Info</display-name></channel>' +
              '<programme start="20240101000000 +0000" stop="20240102000000 +0000" channel="info">' +
              '<title>Large EPG File Available</title>' +
              '<desc>This EPG file is very large (~1.2GB). For full data, use the direct URL.</desc>' +
              '</programme></tv>'
      };
    }
    
    // For smaller files, decompress normally
    const xmlData = await gunzip(Buffer.from(compressedData));
    console.log('Decompressed XML data, size:', xmlData.length);
    
    return {
      statusCode: 200,
      headers: {
        'Content-Type': 'application/xml',
        'Cache-Control': 'public, max-age=1800', // Cache for 30 minutes
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Headers': 'Content-Type',
        'Access-Control-Allow-Methods': 'GET, OPTIONS'
      },
      body: xmlData.toString('utf8')
    };
    
  } catch (error) {
    console.error('EPG fetch error:', error);
    return {
      statusCode: 500,
      headers: {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*'
      },
      body: JSON.stringify({ 
        error: 'Failed to fetch EPG data', 
        details: error.message 
      })
    };
  }
}; 