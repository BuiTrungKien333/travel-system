-- DEV ONLY. Refresh dates for three reserved demo trips; never touch real user trips.
-- Run explicitly before a presentation. Changes made to these demo dates are reset.
SET LOCAL TIME ZONE 'Asia/Ho_Chi_Minh';

-- Sample suggestions are explicitly curated for DEV, with approximate/unknown quality metadata.
INSERT INTO place_retrieval_profiles (place_id, description, tags, estimated_duration_minutes,
    source_url, reviewed_on, verification_scope, coordinates_quality, hours_quality, approved)
SELECT p.id, 'Địa điểm mẫu phục vụ báo cáo WeGo V1; cần xác minh trước khi dùng thực tế.',
    ARRAY[coalesce(p.category, 'Sightseeing'), 'WEGO_DEV_DEMO'], 45,
    'https://example.invalid/wego-dev-demo', CURRENT_DATE, 'DEV_DEMO_ONLY_NOT_FACT_VERIFIED',
    'APPROXIMATE_DEMO', 'UNKNOWN', true
FROM places p WHERE p.id::text LIKE 'd3200000-%' AND p.provider_place_id LIKE 'demo:%'
ON CONFLICT (place_id) DO NOTHING;

-- A dev database may already have the official province under another code. Avoid duplicate picker entries.
CREATE TEMP TABLE v1_demo_province_aliases ON COMMIT DROP AS
SELECT d.id AS demo_id, min(c.id::text)::uuid AS canonical_id
FROM provinces d JOIN provinces c ON lower(d.name) = lower(c.name) AND c.id::text NOT LIKE 'd3900000-%'
WHERE d.id::text LIKE 'd3900000-%' GROUP BY d.id;
UPDATE places p SET province_id = a.canonical_id FROM v1_demo_province_aliases a
WHERE p.province_id = a.demo_id AND p.id::text LIKE 'd3200000-%';
UPDATE trips t SET destination_province_id = a.canonical_id FROM v1_demo_province_aliases a
WHERE t.destination_province_id = a.demo_id AND t.id::text LIKE 'd3300000-%';
DELETE FROM provinces p USING v1_demo_province_aliases a WHERE p.id = a.demo_id
    AND NOT EXISTS (SELECT 1 FROM places x WHERE x.province_id = p.id)
    AND NOT EXISTS (SELECT 1 FROM trips x WHERE x.destination_province_id = p.id);

CREATE TEMP TABLE v1_demo_dates ON COMMIT DROP AS
SELECT t.id, t.start_date AS old_start,
    CASE WHEN t.id = 'd3300000-0000-0000-0000-000000000002'::uuid
        THEN CURRENT_DATE - 2 ELSE CURRENT_DATE + 3 END AS new_start
FROM trips t
WHERE t.id IN ('d3300000-0000-0000-0000-000000000002'::uuid,
    'd3300000-0000-0000-0000-000000000003'::uuid,
    'd3300000-0000-0000-0000-000000000007'::uuid)
    AND t.user_id::text LIKE 'd3100000-%';

UPDATE itinerary_items x SET
    date = x.date + (d.new_start - d.old_start),
    arrival_at = x.arrival_at + (d.new_start - d.old_start) * INTERVAL '1 day',
    updated_at = CURRENT_TIMESTAMP
FROM itineraries i, v1_demo_dates d
WHERE x.itinerary_id = i.id AND i.trip_id = d.id AND x.id::text LIKE 'd3600000-%';

UPDATE memories m SET experienced_at = m.experienced_at + (d.new_start - d.old_start) * INTERVAL '1 day'
FROM v1_demo_dates d WHERE m.trip_id = d.id AND m.id::text LIKE 'd3700000-%';

UPDATE trips t SET end_date = t.end_date + (d.new_start - d.old_start),
    start_date = d.new_start,
    status = CASE WHEN t.id = 'd3300000-0000-0000-0000-000000000002'::uuid THEN 'ACTIVE' ELSE 'READY' END,
    updated_at = CURRENT_TIMESTAMP
FROM v1_demo_dates d WHERE t.id = d.id;

-- The live fixture has a real next stop on the presentation day, not an empty dashboard.
UPDATE itinerary_items x SET
    arrival_at = x.arrival_at + (CURRENT_DATE - x.date) * INTERVAL '1 day',
    date = CURRENT_DATE
FROM itineraries i, trip_places p
WHERE x.itinerary_id = i.id AND i.trip_id = 'd3300000-0000-0000-0000-000000000002'::uuid
    AND x.id::text LIKE 'd3600000-%' AND p.trip_id = i.trip_id AND p.place_id = x.place_id AND NOT p.visited;
