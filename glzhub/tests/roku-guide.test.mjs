import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import ts from 'typescript';
const code=ts.transpileModule(readFileSync(new URL('../src/roku-guide.ts',import.meta.url),'utf8'),{compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.ESNext}}).outputText;
const {lineupChannels,xmltvTime,projectGuide}=await import('data:text/javascript;base64,'+Buffer.from(code).toString('base64'));
const now=xmltvTime('20261027200000 +0000');
const channels=[{id:'a',title:'Local News',eventTitle:'',league:'',start:0,end:0},{id:'e',title:'SPORTS PPV',eventTitle:'Team A vs Team B',league:'MLB',start:now-60,end:now+600}];
const xml=`<tv><channel id="provider-a"><display-name>Local News</display-name></channel>
<programme channel="provider-a" start="20261027190000 +0000" stop="20261027203000 +0000"><title>News &amp; Weather</title><desc>Local bulletin</desc></programme>
<programme channel="provider-a" start="20261027203000 +0000" stop="20261027210000 +0000"><title>Next show</title></programme>
<programme channel="private" start="20261027190000 +0000" stop="20261027210000 +0000"><title>Private channel</title></programme></tv>`;
test('XMLTV time offsets resolve to UTC',()=>{
 assert.equal(xmltvTime('20261027160000 -0400'),now);
 assert.equal(xmltvTime('20261027210000 +0100'),now);
 assert.equal(xmltvTime('invalid'),0);
});
test('managed lineup keeps event identity, time, group and comma-containing titles',()=>{
 const rows=lineupChannels('#EXTM3U\n#EXTINF:-1 tvg-id="e" x-event-title="A, B" x-event-league="MLB" x-event-start="1793131200000" x-event-end="1793141200000" group-title="Sports, PPV",SPORTS PPV\nhttps://example/live\n#EXTINF:-1 radio="true",Radio\nhttps://example/radio');
 assert.equal(rows.length,1); assert.equal(rows[0].id,'e');assert.equal(rows[0].title,'SPORTS PPV');assert.equal(rows[0].eventTitle,'A, B');assert.equal(rows[0].start,1793131200);
});
test('guide matches provider display names, decodes XML, and keeps unassigned channels out',()=>{
 const guide=projectGuide(xml,channels,now);
 assert.equal(guide.summaries.a.now.title,'News & Weather');assert.equal(guide.summaries.a.next.title,'Next show');
 assert.equal(guide.summaries.private,undefined);assert.equal(guide.summaries.e.now.title,'Team A vs Team B');
});
test('selected guide returns only that channel schedule within 24 hours',()=>{
 const guide=projectGuide(xml,channels,now,'a');assert.equal(guide.programmes.length,2);
 assert.ok(guide.programmes.every(p=>p.title!=='Private channel' && p.title!=='Team A vs Team B'));
});
test('summary picks latest current programme while retaining next across overlapping feeds',()=>{
 const overlap=xml+'<programme channel="provider-a" start="20261027193000 +0000" stop="20261027201000 +0000"><title>Updated current</title></programme>';
 const guide=projectGuide(overlap,channels,now);assert.equal(guide.summaries.a.now.title,'Updated current');assert.equal(guide.summaries.a.next.title,'Next show');
});
test('selected schedule is sorted and bounded for Roku memory',()=>{
 let many='<tv>';
 for(let i=100;i>=0;i--){const date=new Date((now+i*60)*1000),end=new Date((now+i*60+60)*1000);const stamp=d=>d.toISOString().replace(/[-:TZ.]/g,'').slice(0,14)+' +0000';many+=`<programme channel="a" start="${stamp(date)}" stop="${stamp(end)}"><title>Show ${i}</title></programme>`;}
 const rows=projectGuide(many+'</tv>',channels,now,'a').programmes;assert.equal(rows.length,96);assert.ok(rows[0].start<rows[1].start);
});
test('shared SPORTS PPV display names never merge distinct event IDs',()=>{
 const list=[{id:'e1',title:'SPORTS PPV',eventTitle:'Game one',league:'MLB',start:now-60,end:now+600},{id:'e2',title:'SPORTS PPV',eventTitle:'Game two',league:'NFL',start:now-60,end:now+600}];
 const guide=projectGuide(`<tv><channel id="e1"><display-name>SPORTS PPV</display-name></channel><channel id="e2"><display-name>SPORTS PPV</display-name></channel><programme channel="e1" start="20261027190000 +0000" stop="20261027203000 +0000"><title>First event only</title></programme></tv>`,list,now);
 assert.notEqual(guide.summaries.e2.now.title,'First event only');
});

test('expired default feed is reported even when PPV metadata is available',()=>{
 const stale=xml.replaceAll('20261027','20260802');
 const guide=projectGuide(stale,channels,now);
 assert.match(guide.guideError,/expired.*2026-08-02/);
 assert.equal(guide.summaries.a.now,null);
 assert.equal(guide.summaries.e.now.title,'Team A vs Team B');
});
test('guide diagnostics distinguish empty, unmatched and future-only feeds',()=>{
 assert.match(projectGuide('<tv/>',channels,now).guideError,/no valid programmes/);
 assert.match(projectGuide(xml.replaceAll('provider-a','different-id').replaceAll('Local News','Different channel'),[channels[0]],now).guideError,/do not match/);
 assert.match(projectGuide(xml.replaceAll('20261027','20261030'),[channels[0]],now).guideError,/next 24 hours/);
 assert.equal(projectGuide(xml,channels,now).guideError,null);
});
