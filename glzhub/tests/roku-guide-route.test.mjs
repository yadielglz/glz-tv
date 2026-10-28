import { test, beforeEach, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { gzipSync } from 'node:zlib';
import ts from 'typescript';
const transpile=s=>ts.transpileModule(s,{compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.ESNext}}).outputText;
const uri=s=>'data:text/javascript;base64,'+Buffer.from(s).toString('base64');
const guide=uri(transpile(readFileSync(new URL('../src/roku-guide.ts',import.meta.url),'utf8')));
const source=transpile(readFileSync(new URL('../src/index.ts',import.meta.url),'utf8')).replace('"./roku-guide"',JSON.stringify(guide));
const worker=(await import(uri(source))).default;
const originalFetch=globalThis.fetch,originalCaches=globalThis.caches;
let sourceCalls,providerStatus,seenHeaders,pending,cache,expired,playlistEpgUrl,seenSourceUrls;
const device={id:'device-one',owner_id:'owner-one',assigned_playlist_id:'playlist-one',request_headers:{'X-Provider':'configured'},channel_policy_mode:'inherit'};
const tokenHash=createHash('sha256').update('test-device-token').digest('hex');
const env={SUPABASE_URL:'https://db.example',SUPABASE_SECRET_KEY:'backend-secret'};
const stamp=ms=>new Date(ms).toISOString().replace(/[-:TZ.]/g,'').slice(0,14)+' +0000';
function req(channel='',token='test-device-token') {return new Request('https://hub.example/api/v1/devices/guide'+(channel?'?channel='+encodeURIComponent(channel):''),{headers:token?{authorization:'Bearer '+token}:{}});}
function run(request) {return worker.fetch(request,env,{waitUntil(p){pending.push(p);}});}
beforeEach(()=>{
 Object.assign(device,{assigned_playlist_id:'playlist-one',epg_url:null,box_group_id:null});
 playlistEpgUrl='https://guide.example/epg.xml.gz';seenSourceUrls=[];
 sourceCalls=0;providerStatus=200;expired=false;seenHeaders=null;pending=[];cache=new Map();
 globalThis.caches={default:{async match(key){return cache.get(key.url)?.clone();},async put(key,response){cache.set(key.url,response);}}};
 globalThis.fetch=async(input,init)=>{
  const url=new URL(typeof input==='string'?input:input.url);
  if(url.hostname==='db.example') {
   let rows=[];
   if(url.pathname==='/rest/v1/devices') rows=url.searchParams.get('token_hash')==='eq.'+tokenHash?[device]:[];
   else if(url.pathname==='/rest/v1/playlists') rows=[{id:'playlist-one',title:'Family',epg_url:playlistEpgUrl,playlist_items:[{id:'a',title:'News',media_url:'https://media.example/a',metadata:{tvg_id:'a'}},{id:'b',title:'Blocked',media_url:'https://media.example/b',metadata:{tvg_id:'b'}}]}];
   else if(url.pathname==='/rest/v1/box_groups') rows=[{id:'main-group',playlist_id:'playlist-one'}];
   else if(url.pathname==='/rest/v1/channel_policy_rules') rows=[{target_type:'device',playlist_item_id:'b',decision:'block'}];
   return Response.json(rows);
  }
  if(['guide.example','override.example','play.glztech.com'].includes(url.hostname)) {
   sourceCalls++;seenSourceUrls.push(url.href);seenHeaders=new Headers(init?.headers);
   const clock=expired?Date.UTC(2026,7,2):Date.now();
   const xml=`<tv><programme channel="a" start="${stamp(clock-60000)}" stop="${stamp(clock+3600000)}"><title>Allowed bulletin</title></programme><programme channel="b" start="${stamp(clock-60000)}" stop="${stamp(clock+3600000)}"><title>Blocked programme</title></programme></tv>`;
   return new Response(providerStatus===200?gzipSync(xml):'offline',{status:providerStatus});
  }
  throw Error('Unexpected upstream '+url.hostname);
 };
});
afterEach(()=>{globalThis.fetch=originalFetch;globalThis.caches=originalCaches;});
test('device guide requires a valid device token before accessing sources',async()=>{
 assert.equal((await run(req('',''))).status,401);assert.equal((await run(req('','bad-token'))).status,401);assert.equal(sourceCalls,0);
});
test('authenticated route decompresses gzip, enforces lineup policies and caches source data',async()=>{
 const result=await run(req('a'));assert.equal(result.status,200);const body=await result.json();
 assert.equal(body.programmes[0].title,'Allowed bulletin');assert.equal(body.summaries.b,undefined);
 assert.equal(seenHeaders.get('authorization'),null);assert.equal(seenHeaders.get('x-provider'),'configured');
 await Promise.all(pending);await run(req('a'));assert.equal(sourceCalls,1);
});
test('unassigned channel cannot be requested even by an authenticated Roku',async()=>{
 assert.equal((await run(req('b'))).status,404);assert.equal(sourceCalls,0);
});
test('guide source failure returns a safe partial response without breaking device playback routes',async()=>{
 providerStatus=503;const response=await run(req());assert.equal(response.status,200);
 const body=await response.json();assert.ok(body.guideError);assert.equal(body.summaries.a.now,null);
});

test('expired feed diagnosis survives the HTTP route and source cache',async()=>{
 expired=true;
 const first=await (await run(req('a'))).json();
 assert.match(first.guideError,/expired.*2026-08-02/);
 assert.deepEqual(first.programmes,[]);
 await Promise.all(pending);
 const again=await (await run(req('a'))).json();
 assert.equal(again.guideError,first.guideError);
 assert.equal(sourceCalls,1);
});

async function androidConfig() {
 const request=new Request('https://hub.example/api/v1/devices/config',{headers:{authorization:'Bearer test-device-token'}});
 return (await run(request)).json();
}
test('Roku and Android share the group playlist EPG source, ahead of device override',async()=>{
 device.assigned_playlist_id=null;device.box_group_id='main-group';device.epg_url='https://override.example/old.xml';
 const config=await androidConfig();
 await run(req('a'));
 assert.equal(config.epgUrl,playlistEpgUrl);assert.deepEqual(seenSourceUrls,[config.epgUrl]);
});
test('Roku follows Android device EPG override without a managed playlist assignment',async()=>{
 device.assigned_playlist_id=null;device.epg_url='https://override.example/current.xml';
 const config=await androidConfig();await run(req('a'));
 assert.equal(config.epgUrl,device.epg_url);assert.deepEqual(seenSourceUrls,[config.epgUrl]);
});
test('blank assigned playlist EPG uses the same built-in fallback as Android',async()=>{
 playlistEpgUrl=null;
 const config=await androidConfig();await run(req('a'));
 assert.equal(config.epgUrl,null);assert.deepEqual(seenSourceUrls,['https://play.glztech.com/epg.xml.gz']);
});
