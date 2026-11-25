// Offline enrichment: only the catalog's exact Wikidata identity and Commons reuse metadata.
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { createHash } from 'node:crypto';
const root = new URL('../../src/main/resources/db/', import.meta.url);
const catalog = JSON.parse(fs.readFileSync(new URL('catalog/vietnam-tourism-v1.json', root)));
const headers = { 'User-Agent': 'WnGoTravelCatalog/1.0 (local development; attributed catalog photos)' };
async function json(url) {
  const cache = path.join(os.tmpdir(), `travel-photo-${createHash('sha256').update(url).digest('hex')}.json`);
  if (fs.existsSync(cache)) return JSON.parse(fs.readFileSync(cache, 'utf8'));
  let response;
  for (let attempt = 0; attempt < 3; attempt++) {
    response = await fetch(url, { headers, signal: AbortSignal.timeout(30000) });
    if (response.status !== 429) break;
    const retry = response.headers.get('retry-after');
    const seconds = retry && /^\d+$/.test(retry) ? Number(retry) : retry ? Math.max(1, (Date.parse(retry) - Date.now()) / 1000) : 20;
    console.log(`Metadata rate limited; waiting ${Math.ceil(seconds)} seconds.`);
    await new Promise(resolve => setTimeout(resolve, seconds * 1000));
  }
  if (!response.ok) throw new Error(`Photo metadata HTTP ${response.status}`);
  const data = await response.json();
  if (data.error) throw new Error(data.error.info);
  fs.writeFileSync(cache, JSON.stringify(data));
  return data;
}
const places = catalog.places.filter(p => p.reference.startsWith('wikidata:'));
const files = [];
for (let start = 0; start < places.length; start += 40) {
  const batch = places.slice(start, start + 40);
  const data = await json('https://www.wikidata.org/w/api.php?' + new URLSearchParams({
    action: 'wbgetentities', format: 'json', props: 'claims', ids: batch.map(p => p.reference.slice(9)).join('|'),
  }));
  for (const place of batch) {
    const file = data.entities[place.reference.slice(9)]?.claims?.P18?.find(c => c.rank !== 'deprecated')?.mainsnak?.datavalue?.value;
    if (typeof file === 'string') files.push({ ...place, file: 'File:' + file });
  }
}
const photos = [];
console.log(`Found ${files.length} Commons file identities.`);
const plain = value => String(value ?? '').replace(/<[^>]*>/g, '').replace(/&amp;/g, '&').replace(/&#(?:160|xA0);/g, ' ').trim();
for (let start = 0; start < files.length; start += 5) {
  await new Promise(resolve => setTimeout(resolve, 1500));
  const batch = files.slice(start, start + 5);
  const data = await json('https://commons.wikimedia.org/w/api.php?' + new URLSearchParams({
    action: 'query', format: 'json', redirects: '1', prop: 'imageinfo', titles: batch.map(p => p.file).join('|'),
    iiprop: 'url|mime|extmetadata', iiurlwidth: '640', iiextmetadatafilter: 'Artist|LicenseShortName|LicenseUrl',
  }));
  for (const page of Object.values(data.query.pages)) {
    const info = page.imageinfo?.[0];
    const license = plain(info?.extmetadata?.LicenseShortName?.value);
    const artist = plain(info?.extmetadata?.Artist?.value);
    if (!info?.mime?.startsWith('image/') || !artist || !/^(CC BY|CC0|Public domain)/i.test(license)) continue;
    const aliases = [...(data.query.normalized ?? []), ...(data.query.redirects ?? [])];
    const resolved = title => {
      for (const alias of aliases) if (alias.from === title) title = alias.to;
      return title.replaceAll('_', ' ');
    };
    const file = batch.find(p => resolved(p.file) === page.title.replaceAll('_', ' '));
    const url = info.thumburl ?? info.url;
    if (!file || !url || !['upload.wikimedia.org', 'thumb.wikimedia.org'].includes(new URL(url).hostname)) continue;
    photos.push({ id: file.id, name: file.name, url, sourceUrl: info.descriptionurl,
      attribution: artist + ' · ' + license, license, reference: file.reference });
  }
}
let sql = '-- Exact-place Commons photos; author/license attribution must accompany display.\n';
sql += 'ALTER TABLE places ADD COLUMN photo_url text, ADD COLUMN photo_source_url text, ADD COLUMN photo_attribution text;\n';
fs.writeFileSync(new URL('migration/V21__place_photos.sql', root), sql);
fs.writeFileSync(new URL('migration-merge-test/V28__place_photos.sql', root), sql);
fs.writeFileSync(new URL('catalog/place-photos.json', root), JSON.stringify({ retrievedOn: new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date()), photos }, null, 2) + '\n');
console.log(`Prepared ${photos.length} attributed photos from ${places.length} exact catalog identities.`);
