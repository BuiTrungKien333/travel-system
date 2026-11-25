// Offline snapshot builder. Inputs are collected explicitly; never run by application startup.
import fs from 'node:fs';
import path from 'node:path';
import { createHash } from 'node:crypto';
import { fileURLToPath } from 'node:url';

const backend = fileURLToPath(new URL('../../', import.meta.url));
const research = path.resolve(process.argv[2] ?? path.join(backend, 'target/catalog-research'));
const read = name => JSON.parse(fs.readFileSync(path.join(research, name), 'utf8'));
const previous = JSON.parse(fs.readFileSync(path.join(backend, 'src/main/resources/db/catalog/vietnam-tourism-v1.json'), 'utf8'));
const fold = value => value.normalize('NFD').replace(/\p{M}/gu, '').replace(/[đĐ]/g, 'd').toLowerCase().replace(/\bcity\b/g, '').replace(/[^a-z0-9]/g, '');
const provinceNames = new Map(previous.provinces.map(province => [fold(province.name), province]));
const boundaries = read('historical-provinces.geojson').features.map(feature => {
  const province = provinceNames.get(fold(feature.properties.Name));
  if (!province) throw new Error(`Unknown historical province: ${feature.properties.Name}`);
  const polygons = feature.geometry.type === 'Polygon' ? [feature.geometry.coordinates] : feature.geometry.coordinates;
  return { ...province, polygons: polygons.map(rings => {
    const xs = rings[0].map(point => point[0]), ys = rings[0].map(point => point[1]);
    return { rings, bounds: [Math.min(...xs), Math.min(...ys), Math.max(...xs), Math.max(...ys)] };
  }) };
});
function inside(x, y, ring) {
  let result = false;
  for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
    const [xi, yi] = ring[i], [xj, yj] = ring[j];
    if ((yi > y) !== (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) result = !result;
  }
  return result;
}
function locate(longitude, latitude) {
  return boundaries.find(province => province.polygons.some(({ rings, bounds: b }) =>
    longitude >= b[0] && longitude <= b[2] && latitude >= b[1] && latitude <= b[3]
    && inside(longitude, latitude, rings[0]) && !rings.slice(1).some(ring => inside(longitude, latitude, ring))));
}
const distance = (a, b) => Math.hypot((a.latitude - b.latitude) * 111,
  (a.longitude - b.longitude) * 111 * Math.cos(a.latitude * Math.PI / 180));
const aliases = new Map([
  ['daudang', 'thacdaudang'], ['huamacave', 'donghuama'], ['diemdulichdonghuama', 'donghuama'],
  ['honam', 'khudulichhonam'], ['khuvuichoiHonam', 'khudulichhonam'],
].map(([from, to]) => [fold(from), fold(to)]));
const identity = name => aliases.get(fold(name)) ?? fold(name);
function uuid(reference) {
  const bytes = createHash('md5').update('wego:catalog:' + reference).digest();
  bytes[6] = (bytes[6] & 15) | 48; bytes[8] = (bytes[8] & 63) | 128;
  const value = bytes.toString('hex');
  return `${value.slice(0,8)}-${value.slice(8,12)}-${value.slice(12,16)}-${value.slice(16,20)}-${value.slice(20)}`;
}
function classification(kind, name, tags = {}) {
  if (kind === 'museum' || kind === 'gallery') return ['MUSEUM', 'INDOOR', ['MUSEUM', 'CULTURE'], 90, 'Không gian trưng bày'];
  if (kind === 'worship' || /^(Chùa|Đền|Đình|Nhà thờ|Thánh thất|Thiền viện)\b/i.test(name))
    return ['RELIGIOUS_SITE', 'HYBRID', ['CULTURE'], 60, 'Địa điểm văn hóa, tín ngưỡng'];
  if (kind === 'historic') return ['HERITAGE', 'HYBRID', ['HISTORY', 'CULTURE'], 60, 'Di tích hoặc công trình tưởng niệm'];
  if (['park', 'garden', 'nature_reserve'].includes(kind)) return ['PARK', 'OUTDOOR', ['PARK', 'SCENERY'], 90, 'Không gian cảnh quan ngoài trời'];
  if (kind === 'beach') return ['BEACH', 'OUTDOOR', ['BEACH', 'SCENERY'], 90, 'Cảnh quan bãi biển'];
  if (['peak', 'cave_entrance', 'waterfall', 'spring', 'island', 'lake', 'reservoir'].includes(kind)
      || /^(Hồ|Thác|Động|Hang|Đỉnh|Thảo nguyên) /i.test(name))
    return ['NATURE', 'OUTDOOR', ['NATURE', 'SCENERY'], 120, 'Điểm cảnh quan tự nhiên'];
  if (['theme_park', 'water_park', 'zoo'].includes(kind)) return ['ATTRACTION', 'HYBRID', ['RELAX'], 120, 'Điểm vui chơi, tham quan'];
  return ['ATTRACTION', 'HYBRID', ['CULTURE', 'SCENERY'], 60, 'Điểm tham quan'];
}
const additions = [], seen = previous.places.map(place => ({ ...place, identity: identity(place.name) }));
function add(record, osm = false) {
  const province = locate(record.longitude, record.latitude);
  if (!province) return;
  if (!Number.isFinite(record.latitude) || !Number.isFinite(record.longitude)) throw new Error('Invalid source coordinate');
  const name = record.name.normalize('NFC').trim().replace(/\s+/g, ' ');
  if (name.length < 3 || name.length > 255) return;
  const reference = osm ? `osm:${record.type}:${record.id}` : record.sourceUrl;
  const identityName = identity(name);
  if (seen.some(place => place.provinceCode === province.code &&
    ((place.identity === identityName && distance(place, record) < 2)
      || (record.tags?.wikidata && place.reference === 'wikidata:' + record.tags.wikidata)))) return;
  const [category, environment, tags, durationMinutes, label] = classification(record.kind, name, record.tags);
  const street = osm ? [record.tags['addr:housenumber'], record.tags['addr:street'], record.tags['addr:suburb']].filter(Boolean).join(', ') : '';
  const place = {
    id: uuid(reference), provinceCode: province.code, reference, name,
    address: [street, province.name].filter(Boolean).join(', '),
    latitude: Number(record.latitude.toFixed(7)), longitude: Number(record.longitude.toFixed(7)),
    category, environment, tags, durationMinutes,
    description: `${label}: ${name}, thuộc phạm vi ${province.name} trong danh mục 63 tỉnh/thành của dự án.`
      + (osm ? ' Nguồn dữ liệu: © OpenStreetMap contributors, ODbL 1.0 (https://www.openstreetmap.org/copyright).' : ''),
    sourceUrl: osm ? `https://www.openstreetmap.org/${record.type}/${record.id}` : record.sourceUrl,
    sourceLicense: osm ? 'ODbL-1.0; © OpenStreetMap contributors' : 'Published place-name and coordinate facts; no article prose or images copied',
    reviewedOn: '2026-11-09',
    verificationScope: (osm ? 'AUTOMATED_OSM_TAG_AND_GEOMETRY_FILTER; not individually field verified' : 'PUBLISHED_DESTINATION_NAME_AND_COORDINATE; no field verification')
      + '; historic province assigned by point-in-polygon; duration is a planning estimate; entrance, access, opening hours, admission and current operation not verified',
    coordinatesQuality: osm ? (record.type === 'node' ? 'OSM_POINT' : 'OSM_AREA_REFERENCE') : 'PUBLISHED_REFERENCE',
  };
  additions.push(place); seen.push({ ...place, identity: identityName });
}
// Supplement sparsely mapped provinces from published destination pages, not invented coordinates.
for (const record of [...read('portal-pois.json'), ...read('baclieu-supplements.json')]) {
  const province = locate(record.longitude, record.latitude);
  if (!province || !['06', '95'].includes(province.code) || /Các bản văn hóa/.test(record.name)) continue;
  add(record);
}
for (const record of read('mapped-pois.json')) {
  if (record.tags.landuse === 'cemetery' || record.tags.amenity === 'grave_yard' || /^nghiatrang/.test(fold(record.name))) continue;
  if (['caycau1500', 'doancailuongcaovanlau'].includes(fold(record.name))) continue;
  add(record, true);
}
additions.sort((a, b) => a.provinceCode.localeCompare(b.provinceCode) || a.name.localeCompare(b.name, 'vi') || a.id.localeCompare(b.id));
const counts = previous.provinces.map(province => ({ ...province,
  previousCount: previous.places.filter(place => place.provinceCode === province.code).length,
  addedCount: additions.filter(place => place.provinceCode === province.code).length,
}));
if (counts.length !== 63 || counts.some(province => province.previousCount + province.addedCount < 20))
  throw new Error('The expanded catalog does not meet the 20-per-province minimum: ' + JSON.stringify(counts.filter(p => p.previousCount + p.addedCount < 20)));
if (new Set(additions.map(place => place.id)).size !== additions.length) throw new Error('Duplicate snapshot identities');
const dataset = {
  schemaVersion: 2, administrativeScope: previous.administrativeScope, reviewedOn: '2026-11-09',
  license: 'ODbL-1.0', attribution: '© OpenStreetMap contributors', licenseUrl: 'https://www.openstreetmap.org/copyright',
  sources: {
    osmExtract: 'https://download.geofabrik.de/asia/vietnam-latest.osm.pbf',
    osmExtractRetrievedOn: '2026-11-09', osmExtractLastModified: '2026-11-08T00:41:39Z',
    osmExtractSha256: createHash('sha256').update(fs.readFileSync(path.join(research, 'vietnam-20261108.osm.pbf'))).digest('hex'),
    historicalBoundary: read('boundary-source.json'),
    supplementaryDestinations: ['https://mythainguyen.vn/vi/places', 'https://tanitour.vn/'],
  },
  minimumPlacesPerProvince: 20, provinces: counts, places: additions,
};
const output = process.argv[3] ? path.resolve(process.argv[3]) : path.join(backend, 'src/main/resources/db/catalog/vietnam-tourism-v2.json');
if (fs.existsSync(output)) throw new Error('A released snapshot must not be overwritten. Choose a new output path.');
fs.writeFileSync(output, JSON.stringify(dataset, null, 2) + '\n');
console.log(JSON.stringify({ added: additions.length, total: previous.places.length + additions.length,
  smallest: [...counts].sort((a,b) => a.addedCount + a.previousCount - b.addedCount - b.previousCount).slice(0,6), output }, null, 2));
