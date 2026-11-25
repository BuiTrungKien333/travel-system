import assert from 'node:assert/strict';
import { test } from 'node:test';
import { importOsm, overpassQuery, overpassCameraQuery, overpassEndpoint, assertFreshSnapshot } from './import-osm.mjs';
const now = '2026-11-23T12:00:00Z';
test('SRC-01/03 OSM retains provenance, version and raw tags without approving unknown direction/scope', () => {
  const result = importOsm({ elements: [{ type:'node', id:123, version:2, timestamp:now, lat:16, lon:108,
    tags:{highway:'speed_camera', maxspeed:'50'} }] }, now);
  assert.equal(result.candidates[0].url, 'https://www.openstreetmap.org/node/123');
  assert.equal(result.candidates[0].osmVersion, 2); assert.equal(result.candidates[0].tags.maxspeed, '50');
  assert.equal(result.candidates[0].reviewStatus, 'PENDING');
  assert.equal(result.sources[0].verifiedAt, null); assert.equal(result.sources[0].reviewStatus, 'PENDING');
  assert.deepEqual(result.speedZones, []); assert.deepEqual(result.events, []);
  assert.match(result.sources[0].attribution, /OpenStreetMap/); assert.match(result.sources[0].license, /ODbL/);
});
test('SRC-02 enforcement relation preserves from/device/to and never treats any CCTV as an enforcement camera', () => {
  const result = importOsm({ elements:[{type:'relation',id:7,tags:{type:'enforcement',enforcement:'traffic_signals'},members:[{type:'node',ref:1,role:'from'},{type:'node',ref:2,role:'device'},{type:'node',ref:3,role:'to'}]},
    {type:'node',id:8,lat:16,lon:108,tags:{man_made:'surveillance'}}] },now);
  assert.equal(result.candidates.length,1); assert.equal(result.candidates[0].kind,'RED_LIGHT_CAMERA');
  assert.deepEqual(result.candidates[0].members.map(item => item.role), ['from','device','to']);
});
test('SRC-03 malformed data, incomplete Overpass responses and duplicate IDs are rejected', () => {
  for (const data of [{remark:'timeout',elements:[]},{}, {elements:[{type:'node',id:1,lat:99,lon:108,tags:{highway:'speed_camera'}}]},
    {elements:[{type:'node',id:1,tags:{highway:'speed_camera'}},{type:'node',id:1,tags:{highway:'speed_camera'}}]}]) assert.throws(()=>importOsm(data,now));
});
test('PRIV-01 bounded public-area query contains no account, GPS trace or credentials', () => {
  const query = overpassQuery([16,108,16.1,108.1]);
  assert.match(query, /speed_camera/); assert.match(query, /maxspeed/); assert.match(query, /out meta geom/);
  assert.throws(()=>overpassQuery([7,102,24,114])); assert.throws(()=>overpassQuery([16,108,15,109]));
  assert.match(overpassCameraQuery(), /ISO3166-1.*VN/); assert.match(overpassCameraQuery(), /enforcement/);
  assert.doesNotMatch(overpassCameraQuery(), /way\["maxspeed"\]/);
});
test('SRC-04 same snapshot is idempotent and older data or approved catalogs cannot be overwritten', () => {
  const catalog=importOsm({osm3s:{timestamp_osm_base:now},elements:[{type:'node',id:123,version:2,lat:16,lon:108,tags:{highway:'speed_camera'}}]},now);
  assert.doesNotThrow(()=>assertFreshSnapshot(catalog,structuredClone(catalog)));
  const older=structuredClone(catalog); older.candidates[0].osmVersion=1;
  assert.throws(()=>assertFreshSnapshot(catalog,older)); older.candidates[0].osmVersion=2; older.snapshotAt='2026-11-22T00:00:00Z';
  assert.throws(()=>assertFreshSnapshot(catalog,older));
  const approved=structuredClone(catalog); approved.sources[0].reviewStatus='APPROVED';
  assert.throws(()=>assertFreshSnapshot(approved,catalog));
});
test('SRC-03 fallback server is an explicit free public instance, never an arbitrary credential-bearing URL', () => {
  assert.equal(overpassEndpoint('osm-de'),'https://overpass-api.de/api/interpreter');
  assert.equal(overpassEndpoint('private-coffee'),'https://overpass.private.coffee/api/interpreter');
  assert.throws(()=>overpassEndpoint('https://example.test/?key=secret'));
});
