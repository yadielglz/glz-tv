/** Small, bounded XMLTV projections for memory-constrained Roku clients. */
export type GuideChannel = { id: string; title: string; eventTitle: string; league: string; start: number; end: number };
export type Programme = { title: string; description: string; start: number; end: number };
// Match the Android EpgGuide's name cleanup and normalization.
const normalize = (s: string) => s.toLowerCase().replace(/[^a-z0-9]/g, '');
const cleanName = (s: string) => s.replace(/^(?:ch\s*)?\d+\s*[·.\-:|\s]\s*/i, '').trim();
const genericNames = new Set(['sportsppv', 'ppv', 'liveevent', 'liveevents']);
export function xmlText(s: string): string {
  return s.replace(/<!\[CDATA\[([\s\S]*?)\]\]>/g, '$1').replace(/<[^>]+>/g, '')
    .replace(/&#(x[\da-f]+|\d+);/gi, (_m, n: string) => { const c = n[0].toLowerCase() === 'x' ? parseInt(n.slice(1),16) : Number(n); return c > 0 && c <= 0x10ffff ? String.fromCodePoint(c) : ''; })
    .replace(/&(?:amp|lt|gt|quot|apos);/g, v => ({'&amp;':'&','&lt;':'<','&gt;':'>','&quot;':'"','&apos;':"'"}[v] || v));
}
function attr(s: string, key: string): string { return xmlText(new RegExp(`(?:^|\\s)${key}=["']([^"']*)["']`, 'i').exec(s)?.[1] || ''); }
export function lineupChannels(m3u: string): GuideChannel[] {
  const channels: GuideChannel[] = []; let info = '';
  for (const line of m3u.split(/\r?\n/)) {
    if (line.startsWith('#EXTINF:')) info = line;
    else if (/^https?:\/\//i.test(line) && info) {
      const title = /,(?=(?:[^"']|"[^"]*"|'[^']*')*$)(.*)$/.exec(info)?.[1]?.trim() || 'Live channel';
      if (attr(info,'radio').toLowerCase()==='true') { info=''; continue; }
      channels.push({ id: attr(info, 'tvg-id') || title, title, eventTitle: attr(info, 'x-event-title'), league: attr(info, 'x-event-league'), start: Number(attr(info,'x-event-start')) / 1000, end: Number(attr(info,'x-event-end')) / 1000 });
      info = '';
    }
  }
  return channels.slice(0,1500);
}
export function xmltvTime(value: string): number {
  const m = /^(\d{8}(?:\d{2}){0,3})(?:\s*(Z|UTC|GMT|[+-]\d{2}(?::?\d{2})?))?$/i.exec(value.trim());
  if (!m) return 0;
  const digits=m[1].padEnd(14,'0');
  const year=+digits.slice(0,4), month=+digits.slice(4,6), day=+digits.slice(6,8);
  const hour=+digits.slice(8,10), minute=+digits.slice(10,12), second=+digits.slice(12,14);
  if (month<1 || month>12 || day<1 || day>31 || hour>23 || minute>59 || second>59) return 0;
  const utc=Date.UTC(year,month-1,day,hour,minute,second);
  if (new Date(utc).getUTCDate()!==day) return 0;
  const zone=(m[2]||'Z').replace(':','');
  const hours=/^[+-]/.test(zone)?+zone.slice(1,3):0, minutes=zone.length>3?+zone.slice(3,5):0;
  if (hours>23 || minutes>59) return 0;
  const offset=(zone[0]==='-'?-1:1)*(hours*60+minutes)*60;
  return utc/1000-offset;
}
export function projectGuide(xml: string, channels: GuideChannel[], now: number, selected = '') {
  const ids = new Map<string,string[]>();
  const add = (alias: string, id: string) => { if (!alias) return; const values=ids.get(alias)||[]; if (!values.includes(id)) values.push(id); ids.set(alias,values); };
  const guideIds = new Set<string>();
  const names = new Map<string,string>();
  const addName = (name: string, id: string) => {
    const key=normalize(name);
    if (key && !genericNames.has(key) && !names.has(key)) names.set(key,id);
  };
  for (const m of xml.matchAll(/<channel\b([^>]*)>([\s\S]*?)<\/channel>/gi)) {
    const id=attr(m[1],'id'); guideIds.add(id);
    for (const name of m[2].matchAll(/<display-name\b[^>]*>([\s\S]*?)<\/display-name>/gi)) {
      const text=xmlText(name[1]); addName(text,id); addName(cleanName(text),id);
    }
  }
  for (const m of xml.matchAll(/<programme\b([^>]*)>/gi)) guideIds.add(attr(m[1],'channel'));
  for (const c of channels) {
    // Exact tvg-id always wins; name fallback follows Android's order.
    if (guideIds.has(c.id)) { add(c.id,c.id); continue; }
    const clean=normalize(cleanName(c.title)), raw=normalize(c.title);
    if (genericNames.has(clean) || genericNames.has(raw)) continue;
    let match=names.get(clean)||names.get(raw);
    if (!match) for (const key of [clean,raw]) {
      if (key.length<3) continue;
      for (const [name,id] of names) {
        if (name.length>=3 && (key.includes(name)||name.includes(key))) { match=id; break; }
      }
      if (match) break;
    }
    if (match) add(match,c.id);
  }
  const rows = new Map<string,Programme[]>();
  for (const c of channels) rows.set(c.id,[]);
  const insert = (id: string, p: Programme) => {
    if (!p.start || p.end <= now || p.start > now+86400 || p.end <= p.start) return;
    const list=rows.get(id); if (!list) return;
    if (list.some(x=>x.start===p.start && x.end===p.end && x.title===p.title)) return;
    list.push(p); list.sort((a,b)=>a.start-b.start || a.end-b.end);
    // Summary needs current + next; a selected channel gets a browsable day.
    if (selected===id) { if (list.length>96) list.length=96; }
    else { const current=list.filter(p=>p.start<=now && p.end>now).at(-1); const next=list.find(p=>p.start>now); list.splice(0,list.length,...(current?[current]:[]),...(next?[next]:[])); }
  };
  let programmeCount = 0, latestEnd = 0, matchedProgrammes = 0;
  for (const m of xml.matchAll(/<programme\b([^>]*)>([\s\S]*?)<\/programme>/gi)) {
    const start=xmltvTime(attr(m[1],'start')), end=xmltvTime(attr(m[1],'stop'));
    if (!start || end <= start) continue;
    programmeCount++; latestEnd = Math.max(latestEnd,end);
    const keys=ids.get(attr(m[1],'channel')); if (!keys) continue;
    matchedProgrammes++;
    if (end<=now || start>now+86400) continue;
    const title=xmlText(/<title\b[^>]*>([\s\S]*?)<\/title>/i.exec(m[2])?.[1] || 'Programme').slice(0,200);
    const description=xmlText(/<desc\b[^>]*>([\s\S]*?)<\/desc>/i.exec(m[2])?.[1] || '').slice(0,selected?1200:200);
    for (const key of keys) insert(key,{title,description,start,end});
  }
  for (const c of channels) if (c.eventTitle) insert(c.id,{title:c.eventTitle,description:`${c.league || 'Sports'} live event`,start:c.start,end:c.end});
  const summaries: Record<string,{now:Programme|null;next:Programme|null}> = Object.create(null);
  for (const [id,list] of rows) summaries[id]={now:list.filter(p=>p.start<=now && p.end>now).at(-1)||null,next:list.find(p=>p.start>now)||null};
  const hasListings = Object.values(summaries).some(s => s.now || s.next);
  let guideError: string | null = null;
  if (channels.length && !programmeCount) guideError = "The EPG feed contains no valid programmes. Update the assigned playlist's XMLTV source in GlzHub.";
  else if (programmeCount && latestEnd <= now) guideError = `The EPG feed has expired (last programme ends ${new Date(latestEnd*1000).toISOString().slice(0,10)} UTC). Refresh or replace the playlist's XMLTV source in GlzHub.`;
  else if (programmeCount && channels.length && !matchedProgrammes) guideError = "EPG channel IDs and names do not match this lineup. Check channel mappings in GlzHub.";
  else if (programmeCount && channels.length && !hasListings) guideError = "The EPG has no listings for this lineup in the next 24 hours. Update its XMLTV source in GlzHub.";
  return { generatedAt:now, summaries, programmes: selected ? rows.get(selected)||[] : [], guideError };
}
