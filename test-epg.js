const fetch = require('node-fetch');
const zlib = require('zlib');
const { promisify } = require('util');

const gunzip = promisify(zlib.gunzip);

async function testEPG() {
  try {
    const epgUrl = 'https://epg.best/1eef8-shw7fc.xml.gz';
    
    console.log('Testing EPG fetch from:', epgUrl);
    
    const response = await fetch(epgUrl, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        'Accept': 'application/xml, text/xml, */*'
      }
    });
    
    console.log('Response status:', response.status);
    console.log('Response headers:', Object.fromEntries(response.headers.entries()));
    
    const buffer = await response.arrayBuffer();
    console.log('Received buffer size:', buffer.byteLength);
    
    try {
      const decompressed = await gunzip(Buffer.from(buffer));
      console.log('Decompressed size:', decompressed.length);
      console.log('First 200 chars:', decompressed.toString('utf8').substring(0, 200));
    } catch (decompressError) {
      console.log('Decompression failed:', decompressError.message);
      console.log('Trying as plain text...');
      const text = Buffer.from(buffer).toString('utf8');
      console.log('As text (first 200 chars):', text.substring(0, 200));
    }
    
  } catch (error) {
    console.error('Test failed:', error);
  }
}

testEPG(); 