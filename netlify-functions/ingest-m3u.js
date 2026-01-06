const fetch = require('node-fetch');
const { createClient } = require('@supabase/supabase-js');

const SUPABASE_URL = process.env.SUPABASE_URL;
const SUPABASE_SERVICE_ROLE = process.env.SUPABASE_SERVICE_ROLE;
const M3U_SOURCE_URL = process.env.M3U_SOURCE_URL || '';

function parseM3U(text) {
  const lines = text.split('\n');
  const channels = [];
  let current = null;
  for (const raw of lines) {
    const line = raw.trim();
    if (line.startsWith('#EXTINF:')) {
      current = {};
      const logoMatch = line.match(/tvg-logo="([^"]*)"/);
      const chnoMatch = line.match(/tvg-chno="([^"]*)"/);
      const idMatch = line.match(/tvg-id="([^"]*)"/);
      const nameMatch = line.match(/tvg-name="([^"]*)"/);
      const groupMatch = line.match(/group-title="([^"]*)"/);
      const lastComma = line.lastIndexOf(',');
      current.name = lastComma !== -1 ? line.substring(lastComma + 1).trim() : (nameMatch ? nameMatch[1] : '');
      current.logo = logoMatch ? logoMatch[1] : '';
      current.chno = chnoMatch ? chnoMatch[1] : '';
      current.tvg_id = idMatch ? idMatch[1] : '';
      current.tvg_name = nameMatch ? nameMatch[1] : current.name;
      current.group_title = groupMatch ? groupMatch[1] : '';
    } else if (line && !line.startsWith('#') && current) {
      current.stream_url = line;
      channels.push(current);
      current = null;
    }
  }
  return channels;
}

exports.handler = async () => {
  if (!SUPABASE_URL || !SUPABASE_SERVICE_ROLE) {
    return { statusCode: 500, body: 'Missing Supabase configuration' };
  }
  if (!M3U_SOURCE_URL) {
    return { statusCode: 500, body: 'Missing M3U_SOURCE_URL' };
  }

  try {
    const resp = await fetch(M3U_SOURCE_URL, { timeout: 30000 });
    if (!resp.ok) throw new Error(`M3U fetch failed: ${resp.status}`);
    const text = await resp.text();
    const parsed = parseM3U(text);

    const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE);
    const { error } = await supabase.from('channels').upsert(parsed, {
      onConflict: 'tvg_id,stream_url'
    });
    if (error) throw error;

    return {
      statusCode: 200,
      body: JSON.stringify({ inserted: parsed.length })
    };
  } catch (err) {
    console.error('ingest-m3u error', err);
    return { statusCode: 500, body: JSON.stringify({ error: err.message }) };
  }
};

