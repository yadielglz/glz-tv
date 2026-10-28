/** Small, bounded XMLTV projections for memory-constrained Roku clients. */
export type GuideChannel = { id: string; title: string; eventTitle: string; league: string; start: number; end: number };
export type Programme = { title: string; description: string; start: number; end: number };
const normalize = (s: string) => s.toLocaleLowerCase().replace(/\s+/g, ' ').trim();
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
  const m = /^(\d{4})(\d{2})(\d{2})(\d{2})(\d{2})(\d{2})?(?:\s*(Z|[+-]\d{4}))?$/.exec(value.trim());
  if (!m) return 0;
  const zone = m[7] || 'Z'; const sign = zone[0] === '-' ? -1 : 1;
  const offset = zone === 'Z' ? 0 : sign * (Number(zone.slice(1,3))*60 + Number(zone.slice(3,5)))*60;
  const stamp = Date.UTC(+m[1],+m[2]-1,+m[3],+m[4],+m[5],+(m[6] || 0))/1000-offset;
  return Number.isFinite(stamp) ? stamp : 0;
}
export function projectGuide(xml: string, channels: GuideChannel[], now: number, selected = '') {
  const ids = new Map<string,string[]>();
  const add = (alias: string, id: string) => { const key=normalize(alias); if (!key) return; const values=ids.get(key)||[]; if (!values.includes(id)) values.push(id); ids.set(key,values); };
  for (const c of channels) { add(c.id,c.id); add(c.title,c.id); }
  // Match XML channel display names when provider IDs differ from the playlist.
  for (const m of xml.matchAll(/<channel\b([^>]*)>([\s\S]*?)<\/channel>/gi)) {
    const id=attr(m[1],'id'); const matched=new Set<string>(ids.get(normalize(id))||[]);
    if (!matched.size) for (const name of m[2].matchAll(/<display-name\b[^>]*>([\s\S]*?)<\/display-name>/gi)) for (const key of ids.get(normalize(xmlText(name[1])))||[]) matched.add(key);
    for (const key of matched) add(id,key);
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
    const keys=ids.get(normalize(attr(m[1],'channel'))); if (!keys) continue;
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
