const fetch = require('node-fetch');
const zlib = require('zlib');
const { createClient } = require('@supabase/supabase-js');
const { XMLParser } = require('fast-xml-parser');

const SUPABASE_URL = process.env.SUPABASE_URL;
const SUPABASE_SERVICE_ROLE = process.env.SUPABASE_SERVICE_ROLE;
const EPG_SOURCE_URL = process.env.EPG_SOURCE_URL || 'https://epg.best/1eef8-shw7fc.xml.gz';

exports.handler = async () => {
  if (!SUPABASE_URL || !SUPABASE_SERVICE_ROLE) {
    return { statusCode: 500, body: 'Missing Supabase configuration' };
  }
  try {
    const resp = await fetch(EPG_SOURCE_URL, {
      headers: { 'Accept': 'application/xml, text/xml, */*' },
      timeout: 45000
    });
    if (!resp.ok) throw new Error(`EPG fetch failed: ${resp.status}`);
    const compressed = await resp.buffer();
    const xmlBuffer = zlib.gunzipSync(compressed);
    const xml = xmlBuffer.toString('utf8');

    const parser = new XMLParser({
      ignoreAttributes: false,
      attributeNamePrefix: '',
      allowBooleanAttributes: true,
      trimValues: true
    });
    const parsed = parser.parse(xml);
    const programmes = parsed?.tv?.programme || [];

    // Limit to reduce payload if huge
    const maxPrograms = 8000;
    const trimmed = programmes.slice(0, maxPrograms).map(p => ({
      channel_tvg_id: p.channel,
      start_time: p.start,
      end_time: p.stop,
      title: p.title?.['#text'] || p.title || 'Unknown',
      description: p.desc?.['#text'] || p.desc || '',
      category: Array.isArray(p.category) ? p.category[0] : (p.category || ''),
      rating: p.rating?.value || ''
    }));

    const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE);
    const { error } = await supabase.from('programmes').upsert(trimmed, {
      onConflict: 'channel_tvg_id,start_time'
    });
    if (error) throw error;

    return {
      statusCode: 200,
      body: JSON.stringify({ inserted: trimmed.length })
    };
  } catch (err) {
    console.error('ingest-epg error', err);
    return { statusCode: 500, body: JSON.stringify({ error: err.message }) };
  }
};

