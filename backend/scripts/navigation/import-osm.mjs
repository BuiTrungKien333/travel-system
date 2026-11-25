import { readFile, writeFile, mkdir, stat } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const maximumBytes = 10_000_000;
export function overpassEndpoint(server = 'osm-de') {
  if (server === 'osm-de') return 'https://overpass-api.de/api/interpreter';
  if (server === 'private-coffee') return 'https://overpass.private.coffee/api/interpreter';
  throw new Error('Choose osm-de or private-coffee; no arbitrary or paid endpoints');
}
export function overpassQuery(bbox) {
  if (!Array.isArray(bbox) || bbox.length !== 4 || !bbox.every(Number.isFinite)) throw new Error('Use south,west,north,east');
  const [south,west,north,east] = bbox;
  if (south < 7 || north > 24 || west < 102 || east > 114 || north <= south || east <= west || north-south > .25 || east-west > .25)
    throw new Error('Choose a Vietnam area no larger than 0.25 x 0.25 degrees; do not query the whole country');
  const area = bbox.join(',');
  return `[out:json][timeout:25];(nwr["highway"="speed_camera"](${area});rel["type"="enforcement"](${area});nwr["traffic_sign"](${area});way["maxspeed"](${area});way["maxspeed:forward"](${area});way["maxspeed:backward"](${area});way["highway"="construction"](${area});nwr["hazard"](${area}););out meta geom;`;
}
export function overpassCameraQuery() {
  return '[out:json][timeout:25];area["ISO3166-1"="VN"]["admin_level"="2"]->.vn;(nwr["highway"="speed_camera"](area.vn);rel["type"="enforcement"](area.vn););out meta geom;';
}
function kind(tags) {
  if (tags.type === 'enforcement' && tags.enforcement === 'traffic_signals') return 'RED_LIGHT_CAMERA';
  if (tags.highway === 'speed_camera' || tags.type === 'enforcement' && tags.enforcement === 'maxspeed') return 'SPEED_CAMERA';
  if (tags.highway === 'construction') return 'ROADWORKS';
  if (tags.hazard) return 'OBSTACLE';
  if (tags.traffic_sign || Object.keys(tags).some(key => /^maxspeed(?::|$)/.test(key))) return 'SPEED_SIGN_OR_ROAD_LIMIT';
  return null;
}
function point(value) {
  if (!Number.isFinite(value.lat) || !Number.isFinite(value.lon) || value.lat < 7 || value.lat > 24 || value.lon < 102 || value.lon > 114)
    throw new Error('Invalid or unsupported OSM geometry');
  return { latitude:value.lat, longitude:value.lon };
}
export function importOsm(raw, fetchedAt = new Date().toISOString()) {
  if (!raw || raw.remark || !Array.isArray(raw.elements) || raw.elements.length > 10000 || !Number.isFinite(Date.parse(fetchedAt)))
    throw new Error('Incomplete or oversized Overpass response');
  const candidates = [], ids = new Set();
  for (const element of raw.elements) {
    const category = kind(element.tags ?? {});
    if (!category) continue;
    if (!['node','way','relation'].includes(element.type) || !Number.isSafeInteger(element.id) || element.id <= 0) throw new Error('Invalid OSM identity');
    const id = `osm:${element.type}:${element.id}`;
    if (ids.has(id)) throw new Error('Duplicate OSM identity');
    ids.add(id);
    const geometry = element.type === 'node' && element.lat !== undefined ? [point(element)] : (element.geometry ?? []).map(point);
    candidates.push({ id, kind:category, url:`https://www.openstreetmap.org/${element.type}/${element.id}`,
      osmVersion:element.version ?? null, osmTimestamp:element.timestamp ?? null, fetchedAt,
      tags:element.tags, geometry, members:element.members ?? [], reviewStatus:'PENDING',
      reviewRequired:['directed road geometry','vehicle applicability','current evidence/check date','validity window',
        ...(category === 'SPEED_SIGN_OR_ROAD_LIMIT' ? ['explicit speed in km/h','start and end scope evidence','conditional/lane restrictions resolved'] : [])] });
  }
  // Download time is not a field-verification date. A way endpoint is not proof that a legal limit ends there.
  return { version:1, fetchedAt, snapshotAt:raw.osm3s?.timestamp_osm_base ?? null, sources:[{id:'osm',title:'OpenStreetMap',url:'https://www.openstreetmap.org/copyright',
    license:'ODbL-1.0',attribution:'© OpenStreetMap contributors · ODbL 1.0',verifiedAt:null,reviewStatus:'PENDING',
    licenseAllowsCache:true,licenseAllowsRedistribution:true}],speedZones:[],events:[],candidates };
}
export function assertFreshSnapshot(previous,next) {
  if (previous.sources?.some(source=>source.reviewStatus==='APPROVED') || previous.speedZones?.length || previous.events?.length)
    throw new Error('Never overwrite a reviewed catalog; use a separate candidates file');
  if (previous.snapshotAt && (!next.snapshotAt || Date.parse(next.snapshotAt)<Date.parse(previous.snapshotAt))) throw new Error('Older or undated OSM snapshot');
  const versions=new Map((previous.candidates ?? []).map(item=>[item.id,item.osmVersion]));
  for (const row of next.candidates) if (versions.get(row.id) != null && (row.osmVersion == null || row.osmVersion<versions.get(row.id))) throw new Error('Older OSM element version');
}
async function download(query, server) {
  const response = await fetch(overpassEndpoint(server),{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded',
    'User-Agent':'TravelSystem-OSM-Safety-Import/1.0'},body:new URLSearchParams({data:query}),signal:AbortSignal.timeout(35000)});
  if (!response.ok || !response.body) throw new Error(`Overpass unavailable (HTTP ${response.status}); retry later manually`);
  if (Number(response.headers.get('content-length')) > maximumBytes) throw new Error('Overpass response too large');
  const reader = response.body.getReader(), chunks=[]; let bytes=0;
  try { while (true) { const {done,value}=await reader.read(); if(done) break; bytes+=value.length;
    if(bytes>maximumBytes) { await reader.cancel(); throw new Error('Overpass response too large'); } chunks.push(value); } }
  finally { reader.releaseLock(); }
  return JSON.parse(Buffer.concat(chunks).toString('utf8'));
}
async function main(args) {
  const options = {};
  for (let i=0;i<args.length;i+=2) {
    if (!['--input','--output','--bbox','--scope','--server'].includes(args[i]) || !args[i+1] || options[args[i]]) throw new Error('Use --input file OR --bbox south,west,north,east OR --scope vn-cameras, plus --output file and optional --server osm-de/private-coffee');
    options[args[i]]=args[i+1];
  }
  if (!options['--output'] || ['--input','--bbox','--scope'].filter(key=>options[key]).length!==1 || options['--scope'] && options['--scope']!=='vn-cameras') throw new Error('Choose one supported input and an output');
  let raw;
  if (options['--input']) {
    if ((await stat(options['--input'])).size>maximumBytes) throw new Error('Input too large');
    raw=JSON.parse(await readFile(options['--input'],'utf8'));
  } else raw=await download(options['--scope'] ? overpassCameraQuery() : overpassQuery(options['--bbox'].split(',').map(Number)),options['--server']);
  const catalog=importOsm(raw), output=resolve(options['--output']);
  try { assertFreshSnapshot(JSON.parse(await readFile(output,'utf8')),catalog); }
  catch(error) { if(error.code!=='ENOENT') throw error; }
  await mkdir(dirname(output),{recursive:true}); await writeFile(output,JSON.stringify(catalog,null,2)+'\n','utf8');
  console.log(JSON.stringify({output,candidates:catalog.candidates.length,activeSpeedZones:0,activeEvents:0,reviewStatus:'PENDING'}));
}
if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href)
  main(process.argv.slice(2)).catch(error=>{console.error(error.message);process.exitCode=1;});
