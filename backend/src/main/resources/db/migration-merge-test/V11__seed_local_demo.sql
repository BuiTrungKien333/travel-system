-- Demo login: demo.local@example.com or demo.user02..50@example.com / DemoTravel!2026.
-- IDs d31..d39 identify demo records; insert-only conflicts preserve existing edits.
-- Flyway supplies the transaction and migration lock. Photos and map pins are illustrative.
SET LOCAL TIME ZONE 'Asia/Ho_Chi_Minh';

-- 1. Protect existing accounts and records before inserting anything.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM generate_series(1, 50) AS n
        JOIN users u ON
            u.id = ('d3100000-0000-0000-0000-' || lpad(n::text, 12, '0'))::uuid
            OR lower(u.email) = CASE WHEN n = 1 THEN 'demo.local@example.com'
                ELSE 'demo.user' || lpad(n::text, 2, '0') || '@example.com' END
        WHERE u.id <> ('d3100000-0000-0000-0000-' || lpad(n::text, 12, '0'))::uuid
            OR u.email <> CASE WHEN n = 1 THEN 'demo.local@example.com'
                ELSE 'demo.user' || lpad(n::text, 2, '0') || '@example.com' END
            OR u.role <> 'USER'
    ) THEN
        RAISE EXCEPTION 'Demo account ID/email is occupied; existing accounts will not be overwritten';
    END IF;

    IF EXISTS (SELECT 1 FROM trips WHERE id::text LIKE 'd3300000-%' AND user_id::text NOT LIKE 'd3100000-%')
        OR EXISTS (SELECT 1 FROM places WHERE id::text LIKE 'd3200000-%'
            AND (provider_place_id IS NULL OR provider_place_id NOT LIKE 'demo:%'))
        OR EXISTS (SELECT 1 FROM trip_places p JOIN trips t ON t.id = p.trip_id
            WHERE p.id::text LIKE 'd3400000-%' AND t.user_id::text NOT LIKE 'd3100000-%')
        OR EXISTS (SELECT 1 FROM itineraries i JOIN trips t ON t.id = i.trip_id
            WHERE i.id::text LIKE 'd3500000-%' AND t.user_id::text NOT LIKE 'd3100000-%')
        OR EXISTS (SELECT 1 FROM memories WHERE id::text LIKE 'd3700000-%' AND user_id::text NOT LIKE 'd3100000-%')
    THEN
        RAISE EXCEPTION 'A demo ID namespace is occupied by unrelated data';
    END IF;
END $$;

-- 2. Users (50) and shared historical province codes (7).
INSERT INTO users (id, full_name, email, password_hash, role, email_verified_at, created_at, updated_at)
SELECT
    ('d3100000-0000-0000-0000-' || lpad(n::text, 12, '0'))::uuid,
    CASE WHEN n = 1 THEN 'Nguyễn Minh An — Demo' ELSE
        (ARRAY['Nguyễn','Trần','Lê','Phạm','Hoàng','Huỳnh','Vũ','Đặng','Bùi','Đỗ'])[(n - 2) % 10 + 1] || ' ' ||
        (ARRAY['Minh Anh','Quốc Bảo','Thanh Hà','Gia Huy','Ngọc Linh'])[(n - 2) / 10 + 1] END,
    CASE WHEN n = 1 THEN 'demo.local@example.com'
        ELSE 'demo.user' || lpad(n::text, 2, '0') || '@example.com' END,
    '$2a$10$roUo4O//fHzK/EhC2ZXbV.MzXSMLy1jO1WcxnGVGWgBhTyPy8kKRa',
    'USER',
    CURRENT_TIMESTAMP - CASE WHEN n = 1 THEN INTERVAL '59 days' ELSE INTERVAL '179 days' END,
    CURRENT_TIMESTAMP - CASE WHEN n = 1 THEN INTERVAL '60 days' ELSE INTERVAL '180 days' END,
    CURRENT_TIMESTAMP
FROM generate_series(1, 50) AS n
ON CONFLICT (id) DO NOTHING;

INSERT INTO provinces (id, code, name) VALUES
    ('d3900000-0000-0000-0000-000000000001', 'DN', 'Đà Nẵng'),
    ('d3900000-0000-0000-0000-000000000002', 'LD', 'Lâm Đồng'),
    ('d3900000-0000-0000-0000-000000000003', 'HG', 'Hà Giang'),
    ('d3900000-0000-0000-0000-000000000004', 'LC', 'Lào Cai'),
    ('d3900000-0000-0000-0000-000000000005', 'QN', 'Quảng Nam'),
    ('d3900000-0000-0000-0000-000000000006', 'KG', 'Kiên Giang'),
    ('d3900000-0000-0000-0000-000000000007', 'TTH', 'Thừa Thiên Huế')
ON CONFLICT (code) DO NOTHING;

-- 3. Places (50). Province references use code, not an assumed existing UUID.
WITH catalog(n, reference, name, address, lat, lng, province, category, kind, opens, closes) AS (
    VALUES
    (1, 'my-khe', 'Bãi biển Mỹ Khê', 'Võ Nguyên Giáp, phường An Hải, Đà Nẵng', 16.0544, 108.2473, 'DN', 'Beach', 'OUTDOOR', NULL, NULL),
    (2, 'ngu-hanh-son', 'Danh thắng Ngũ Hành Sơn', '81 Huyền Trân Công Chúa, Đà Nẵng', 16.0036, 108.2631, 'DN', 'Nature', 'OUTDOOR', NULL, NULL),
    (3, 'cham-museum', 'Bảo tàng Điêu khắc Chăm', '02 đường 2 Tháng 9, Đà Nẵng', 16.0605, 108.2234, 'DN', 'Museum', 'INDOOR', '07:30', '17:00'),
    (4, 'cau-dat', 'Đồi chè Cầu Đất', 'Phường Xuân Trường–Đà Lạt, Lâm Đồng', 11.9056, 108.5447, 'LD', 'Nature', 'OUTDOOR', NULL, NULL),
    (5, 'xuan-huong', 'Hồ Xuân Hương', 'Trung tâm Đà Lạt, Lâm Đồng', 11.9404, 108.4419, 'LD', 'Sightseeing', 'OUTDOOR', NULL, NULL),
    (6, 'lam-dong-museum', 'Bảo tàng Lâm Đồng', '04 Hùng Vương, phường Xuân Hương–Đà Lạt, Lâm Đồng', 11.9410, 108.4580, 'LD', 'Museum', 'INDOOR', '07:30', '17:30'),
    (7, 'ma-pi-leng', 'Đèo Mã Pí Lèng', 'Cung đường Đồng Văn–Mèo Vạc, khu vực Hà Giang', 23.2389, 105.4167, 'HG', 'Nature', 'OUTDOOR', NULL, NULL),
    (8, 'vuong-palace', 'Dinh thự họ Vương', 'Sà Phìn, khu vực Hà Giang', 23.2540, 105.2640, 'HG', 'Culture', 'INDOOR', NULL, NULL),
    (9, 'lung-cu', 'Cột cờ Lũng Cú', 'Lũng Cú, khu vực Hà Giang', 23.3644, 105.3157, 'HG', 'Landmark', 'OUTDOOR', NULL, NULL),
    (10, 'linh-ung-son-tra', 'Chùa Linh Ứng Sơn Trà', 'Bán đảo Sơn Trà, Đà Nẵng', 16.1009, 108.2779, 'DN', 'Culture', 'OUTDOOR', NULL, NULL),
    (11, 'dragon-bridge', 'Cầu Rồng', 'Sông Hàn, Đà Nẵng', 16.0611, 108.2276, 'DN', 'Landmark', 'OUTDOOR', NULL, NULL),
    (12, 'ba-na-hills', 'Bà Nà Hills', 'Hòa Vang, Đà Nẵng', 15.9953, 108.0003, 'DN', 'Sightseeing', 'HYBRID', NULL, NULL),
    (13, 'han-market', 'Chợ Hàn', 'Trần Phú, Đà Nẵng', 16.0676, 108.2246, 'DN', 'Market', 'INDOOR', NULL, NULL),
    (14, 'han-river-bridge', 'Cầu Sông Hàn', 'Sông Hàn, Đà Nẵng', 16.0732, 108.2269, 'DN', 'Landmark', 'OUTDOOR', NULL, NULL),
    (15, 'langbiang', 'Núi Langbiang', 'Khu vực Lạc Dương, Lâm Đồng', 12.0463, 108.4415, 'LD', 'Nature', 'OUTDOOR', NULL, NULL),
    (16, 'datanla', 'Thác Datanla', 'Đèo Prenn, Đà Lạt', 11.9039, 108.4497, 'LD', 'Nature', 'OUTDOOR', NULL, NULL),
    (17, 'tuyen-lam', 'Hồ Tuyền Lâm', 'Đà Lạt, Lâm Đồng', 11.8981, 108.4358, 'LD', 'Nature', 'OUTDOOR', NULL, NULL),
    (18, 'bao-dai-palace', 'Dinh III Bảo Đại', 'Triệu Việt Vương, Đà Lạt', 11.9290, 108.4300, 'LD', 'Culture', 'INDOOR', NULL, NULL),
    (19, 'dalat-railway', 'Ga Đà Lạt', 'Quang Trung, Đà Lạt', 11.9420, 108.4557, 'LD', 'Landmark', 'HYBRID', NULL, NULL),
    (20, 'quan-ba', 'Cổng trời Quản Bạ', 'Khu vực Quản Bạ, Hà Giang', 23.0463, 104.9885, 'HG', 'Nature', 'OUTDOOR', NULL, NULL),
    (21, 'dong-van-old-town', 'Phố cổ Đồng Văn', 'Khu vực Đồng Văn, Hà Giang', 23.2780, 105.3629, 'HG', 'Culture', 'OUTDOOR', NULL, NULL),
    (22, 'nho-que', 'Sông Nho Quế', 'Khu vực Mèo Vạc, Hà Giang', 23.2340, 105.4270, 'HG', 'Nature', 'OUTDOOR', NULL, NULL),
    (23, 'pho-bang', 'Phó Bảng', 'Khu vực Đồng Văn, Hà Giang', 23.2489, 105.1814, 'HG', 'Culture', 'OUTDOOR', NULL, NULL),
    (24, 'fansipan', 'Đỉnh Fansipan', 'Sa Pa, Lào Cai', 22.3033, 103.7753, 'LC', 'Nature', 'OUTDOOR', NULL, NULL),
    (25, 'cat-cat', 'Bản Cát Cát', 'Sa Pa, Lào Cai', 22.3280, 103.8320, 'LC', 'Culture', 'OUTDOOR', NULL, NULL),
    (26, 'ham-rong', 'Núi Hàm Rồng', 'Sa Pa, Lào Cai', 22.3321, 103.8445, 'LC', 'Nature', 'OUTDOOR', NULL, NULL),
    (27, 'silver-waterfall', 'Thác Bạc', 'Sa Pa, Lào Cai', 22.3908, 103.7782, 'LC', 'Nature', 'OUTDOOR', NULL, NULL),
    (28, 'ta-van', 'Bản Tả Van', 'Sa Pa, Lào Cai', 22.3034, 103.8907, 'LC', 'Culture', 'OUTDOOR', NULL, NULL),
    (29, 'love-waterfall', 'Thác Tình Yêu', 'Sa Pa, Lào Cai', 22.3526, 103.7492, 'LC', 'Nature', 'OUTDOOR', NULL, NULL),
    (30, 'o-quy-ho', 'Đèo Ô Quy Hồ', 'Khu vực Sa Pa, Lào Cai', 22.3534, 103.7734, 'LC', 'Nature', 'OUTDOOR', NULL, NULL),
    (31, 'hoi-an-old-town', 'Phố cổ Hội An', 'Hội An', 15.8801, 108.3380, 'QN', 'Culture', 'OUTDOOR', NULL, NULL),
    (32, 'japanese-bridge', 'Chùa Cầu', 'Trần Phú, Hội An', 15.8772, 108.3269, 'QN', 'Culture', 'HYBRID', NULL, NULL),
    (33, 'my-son', 'Thánh địa Mỹ Sơn', 'Khu vực Duy Xuyên', 15.7640, 108.1240, 'QN', 'Culture', 'OUTDOOR', NULL, NULL),
    (34, 'an-bang', 'Biển An Bàng', 'Hội An', 15.9143, 108.3384, 'QN', 'Beach', 'OUTDOOR', NULL, NULL),
    (35, 'tra-que', 'Làng rau Trà Quế', 'Hội An', 15.9020, 108.3339, 'QN', 'Culture', 'OUTDOOR', NULL, NULL),
    (36, 'cu-lao-cham', 'Cù Lao Chàm', 'Ngoài khơi Hội An', 15.9511, 108.5269, 'QN', 'Nature', 'OUTDOOR', NULL, NULL),
    (37, 'thanh-ha', 'Làng gốm Thanh Hà', 'Hội An', 15.8792, 108.3013, 'QN', 'Culture', 'HYBRID', NULL, NULL),
    (38, 'bai-sao', 'Bãi Sao', 'Phía nam Phú Quốc', 10.0547, 104.0322, 'KG', 'Beach', 'OUTDOOR', NULL, NULL),
    (39, 'hon-thom', 'Hòn Thơm', 'Quần đảo An Thới, Phú Quốc', 9.9570, 104.0180, 'KG', 'Nature', 'OUTDOOR', NULL, NULL),
    (40, 'phu-quoc-prison', 'Nhà tù Phú Quốc', 'Khu vực An Thới, Phú Quốc', 10.0449, 104.0150, 'KG', 'Culture', 'HYBRID', NULL, NULL),
    (41, 'dinh-cau', 'Dinh Cậu', 'Khu vực Dương Đông, Phú Quốc', 10.2162, 103.9550, 'KG', 'Culture', 'OUTDOOR', NULL, NULL),
    (42, 'suoi-tranh', 'Suối Tranh', 'Phú Quốc', 10.1710, 104.0110, 'KG', 'Nature', 'OUTDOOR', NULL, NULL),
    (43, 'grand-world', 'Grand World Phú Quốc', 'Khu vực Gành Dầu, Phú Quốc', 10.3267, 103.8610, 'KG', 'Entertainment', 'HYBRID', NULL, NULL),
    (44, 'ganh-dau', 'Mũi Gành Dầu', 'Phía bắc Phú Quốc', 10.3694, 103.8458, 'KG', 'Nature', 'OUTDOOR', NULL, NULL),
    (45, 'hue-imperial-city', 'Đại Nội Huế', 'Kinh thành Huế', 16.4697, 107.5796, 'TTH', 'Culture', 'HYBRID', NULL, NULL),
    (46, 'thien-mu', 'Chùa Thiên Mụ', 'Hà Khê, Huế', 16.4532, 107.5448, 'TTH', 'Culture', 'OUTDOOR', NULL, NULL),
    (47, 'tu-duc', 'Lăng Tự Đức', 'Khu vực Thủy Xuân, Huế', 16.4330, 107.5658, 'TTH', 'Culture', 'OUTDOOR', NULL, NULL),
    (48, 'khai-dinh', 'Lăng Khải Định', 'Khu vực Hương Thủy, Huế', 16.3982, 107.5909, 'TTH', 'Culture', 'OUTDOOR', NULL, NULL),
    (49, 'minh-mang', 'Lăng Minh Mạng', 'Khu vực Hương Trà, Huế', 16.3887, 107.5719, 'TTH', 'Culture', 'OUTDOOR', NULL, NULL),
    (50, 'dong-ba', 'Chợ Đông Ba', 'Trần Hưng Đạo, Huế', 16.4723, 107.5870, 'TTH', 'Market', 'INDOOR', NULL, NULL)
)
INSERT INTO places (
    id, provider_place_id, name, address, latitude, longitude, province_id,
    category, place_type, opening_time, closing_time, weekly_opening_hours, created_at
)
SELECT
    ('d3200000-0000-0000-0000-' || lpad(c.n::text, 12, '0'))::uuid,
    'demo:' || c.reference, c.name, c.address, c.lat, c.lng, p.id,
    c.category, c.kind, c.opens::time, c.closes::time,
    CASE WHEN c.n <= 9 THEN jsonb_build_object('schedules', (
        SELECT jsonb_object_agg(day, jsonb_build_object('dayOfWeek', day, 'status', 'UNVERIFIED'))
        FROM unnest(ARRAY['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY']) AS day
    ))::text ELSE '{"schedules":{}}' END,
    CURRENT_TIMESTAMP - CASE WHEN c.n <= 9 THEN INTERVAL '60 days' ELSE INTERVAL '180 days' END
FROM catalog c JOIN provinces p ON p.code = c.province
ON CONFLICT (id) DO NOTHING;

-- 4. Define all 50 trips once; their children reuse these two temporary tables.
CREATE TEMP TABLE demo_seed_trips ON COMMIT DROP AS
WITH featured(n, name, destination, start_offset, end_offset, status, budget, preferences, description, places) AS (
    VALUES
        (1, 'Đà Nẵng — Kỷ niệm mùa biển', 'Đà Nẵng', -30, -28, 'COMPLETED', 3500000, '["Nature","Food","Culture"]', 'Chuyến đi đã hoàn thành, có ảnh và kỷ niệm.', ARRAY[1,2,3]),
        (2, 'Đà Lạt — Cuối tuần xanh', 'Đà Lạt', -2, 1, 'ACTIVE', 2800000, '["Nature","Photography"]', 'Đang khám phá Đà Lạt, đã ghé hai địa điểm.', ARRAY[4,5,6]),
        (3, 'Hà Giang — Hẹn ngày lên đường', 'Hà Giang', 10, 12, 'READY', 4200000, '["Nature","Culture"]', 'Đã sẵn sàng với lịch trình, chưa có Memory.', ARRAY[7,8,9]),
        (4, 'Đà Nẵng — Kế hoạch trở lại', 'Đà Nẵng', 30, 32, 'PLANNING', 3000000, '["Food","Museum"]', 'Bản nháp có địa điểm, chưa có lịch trình và Memory.', ARRAY[1,2,3])
), regions(region_no, destination, places, budget) AS (
    VALUES
        (0, 'Đà Nẵng', ARRAY[1,2,3,10,11,12,13,14], 3500000),
        (1, 'Đà Lạt', ARRAY[4,5,6,15,16,17,18,19], 2800000),
        (2, 'Hà Giang', ARRAY[7,8,9,20,21,22,23], 4200000),
        (3, 'Sa Pa', ARRAY[24,25,26,27,28,29,30], 4500000),
        (4, 'Hội An', ARRAY[31,32,33,34,35,36,37], 3000000),
        (5, 'Phú Quốc', ARRAY[38,39,40,41,42,43,44], 6000000),
        (6, 'Huế', ARRAY[45,46,47,48,49,50], 3200000)
), generated AS (
    SELECT n, r.destination, r.places, r.budget + (n % 5) * 250000 AS budget,
        (ARRAY['Một lần đến ','Cuối tuần ở ','Dạo chơi ','Hẹn ngày về ','Theo dấu kỷ niệm ','Chuyến đi cùng bạn đến ','Mùa khám phá '])[(n - 5) / 7 + 1] || r.destination AS name,
        CASE n % 4 WHEN 1 THEN 'COMPLETED' WHEN 2 THEN 'ACTIVE' WHEN 3 THEN 'READY' ELSE 'PLANNING' END AS status,
        CASE n % 4 WHEN 1 THEN CURRENT_DATE - (15 + n % 20) WHEN 2 THEN CURRENT_DATE - 2
            WHEN 3 THEN CURRENT_DATE + 7 + n / 4 ELSE CURRENT_DATE + 30 + n END AS starts
    FROM generate_series(5, 50) AS n JOIN regions r ON r.region_no = (n - 5) % 7
)
SELECT
    n AS trip_no, 1 AS user_no, name, destination,
    CURRENT_DATE + start_offset AS start_date, CURRENT_DATE + end_offset AS end_date,
    status, 'Car' AS transportation, budget, preferences,
    'Lịch trình minh họa; người dùng tự xác nhận trước khi áp dụng.' AS constraints, description, places,
    LEAST(CURRENT_DATE - 7, CURRENT_DATE + start_offset - 7)::timestamptz AS created_at,
    LEAST(CURRENT_TIMESTAMP, (CURRENT_DATE + end_offset)::timestamptz) AS updated_at
FROM featured
UNION ALL
SELECT
    n, n - 3, name, destination, starts, starts + CASE WHEN status = 'ACTIVE' THEN 3 ELSE 2 END,
    status, CASE WHEN destination IN ('Hà Giang','Sa Pa') THEN 'Motorbike' ELSE 'Car' END,
    budget, '["Nature","Food","Culture"]', 'Ưu tiên nghỉ ngơi giữa các điểm tham quan.',
    'Chuyến đi mẫu với ba ngày tham quan; lịch trình có thể chỉnh trước khi áp dụng.', places,
    CASE WHEN status = 'COMPLETED' THEN (starts - 7)::timestamptz ELSE (CURRENT_DATE - 14)::timestamptz END,
    LEAST(CURRENT_TIMESTAMP, (starts + 2)::timestamptz)
FROM generated;

INSERT INTO trips (
    id, user_id, name, destination, start_date, end_date, status, transportation,
    budget, preferences, constraints, description, cover_image, created_at, updated_at
)
SELECT
    ('d3300000-0000-0000-0000-' || lpad(trip_no::text, 12, '0'))::uuid,
    ('d3100000-0000-0000-0000-' || lpad(user_no::text, 12, '0'))::uuid,
    name, destination, start_date, end_date, status, transportation, budget, preferences, constraints, description,
    'https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?w=1200', created_at, updated_at
FROM demo_seed_trips
ON CONFLICT (id) DO NOTHING;

-- 5. Trip places (150), including visit state and stable Memory IDs.
CREATE TEMP TABLE demo_seed_visits ON COMMIT DROP AS
WITH visits AS (
    SELECT (t.trip_no - 1) * 3 + position + 1 AS link_no, t.trip_no, position,
        t.places[CASE WHEN t.trip_no <= 4 THEN position + 1
            ELSE ((t.trip_no - 5) / 7 + position) % cardinality(t.places) + 1 END] AS place_no,
        t.status = 'COMPLETED' OR (t.status = 'ACTIVE' AND (
            (t.trip_no = 2 AND position < 2) OR (t.trip_no BETWEEN 5 AND 38 AND position = 0)
        )) AS visited
    FROM demo_seed_trips t CROSS JOIN generate_series(0, 2) AS position
)
SELECT *, CASE WHEN visited THEN count(*) FILTER (WHERE visited) OVER (ORDER BY trip_no, position) END AS memory_no
FROM visits;

INSERT INTO trip_places (
    id, trip_id, place_id, priority, estimated_duration, notes, planned_order, visited, created_at, updated_at
)
SELECT
    ('d3400000-0000-0000-0000-' || lpad(v.link_no::text, 12, '0'))::uuid, t.id,
    ('d3200000-0000-0000-0000-' || lpad(v.place_no::text, 12, '0'))::uuid,
    CASE WHEN v.position = 0 THEN 2 ELSE 1 END, 90,
    CASE WHEN v.trip_no <= 4 THEN 'Địa điểm trong bộ demo local' ELSE 'Dành thời gian tìm hiểu và chụp ảnh.' END,
    v.position, v.visited, t.created_at + INTERVAL '1 hour', CURRENT_TIMESTAMP
FROM demo_seed_visits v
JOIN trips t ON t.id = ('d3300000-0000-0000-0000-' || lpad(v.trip_no::text, 12, '0'))::uuid
ON CONFLICT (id) DO NOTHING;

-- 6. Itineraries (38) and one 09:00–10:30 visit per day (114 items).
INSERT INTO itineraries (id, trip_id, name, status, created_at, updated_at)
SELECT
    ('d3500000-0000-0000-0000-' || lpad(s.trip_no::text, 12, '0'))::uuid, t.id,
    CASE WHEN s.trip_no <= 3 THEN 'Lịch trình demo đã xác nhận' ELSE 'Lịch trình khám phá ' || t.destination END,
    'FINAL', t.created_at + INTERVAL '2 hours', CURRENT_TIMESTAMP
FROM demo_seed_trips s
JOIN trips t ON t.id = ('d3300000-0000-0000-0000-' || lpad(s.trip_no::text, 12, '0'))::uuid
WHERE s.status <> 'PLANNING'
ON CONFLICT (id) DO NOTHING;

INSERT INTO itinerary_items (
    id, itinerary_id, place_id, date, start_time, end_time, sequence,
    distance_from_previous, travel_time_from_previous, note, created_at, updated_at
)
SELECT
    ('d3600000-0000-0000-0000-' || lpad(v.link_no::text, 12, '0'))::uuid, i.id,
    ('d3200000-0000-0000-0000-' || lpad(v.place_no::text, 12, '0'))::uuid,
    CASE WHEN t.status = 'ACTIVE' AND v.position = 2 THEN t.end_date ELSE t.start_date + v.position END,
    '09:00'::time, '10:30'::time, 0, 0, 0,
    CASE WHEN v.trip_no <= 3 THEN 'Điểm đầu của ngày tham quan; chưa tính tuyến đường từ nơi lưu trú.'
        ELSE 'Điểm đầu của ngày; chưa mô hình hóa tuyến đường từ nơi lưu trú.' END,
    i.created_at + INTERVAL '1 hour', CURRENT_TIMESTAMP
FROM demo_seed_visits v
JOIN trips t ON t.id = ('d3300000-0000-0000-0000-' || lpad(v.trip_no::text, 12, '0'))::uuid
JOIN itineraries i ON i.trip_id = t.id AND i.id::text LIKE 'd3500000-%'
ON CONFLICT (id) DO NOTHING;

-- 7. Memories (50) match visited places at 09:30 on the same itinerary day.
INSERT INTO memories (id, user_id, trip_id, place_id, note, experienced_at, created_at, updated_at)
SELECT
    ('d3700000-0000-0000-0000-' || lpad(v.memory_no::text, 12, '0'))::uuid, t.user_id, t.id, p.id,
    CASE WHEN v.memory_no <= 5 THEN (ARRAY[
        'Sáng đầu tiên ở Mỹ Khê. Cả nhóm đi bộ dọc bờ biển và chụp ảnh trước khi ăn sáng.',
        'Ngày thứ hai ghé Ngũ Hành Sơn. Dừng nghỉ giữa các bậc thang để ngắm cảnh.',
        'Buổi sáng cuối cùng tìm hiểu nghệ thuật Chăm trước khi về. Giữ lại vài tấm hình làm kỷ niệm.',
        'Ngày đầu đến Cầu Đất, trời mát và nhiều gió. Uống trà rồi đi dạo cùng bạn bè.',
        'Ngày thứ hai đi bộ quanh hồ Xuân Hương. Nghỉ một lúc trước khi tiếp tục khám phá Đà Lạt.'
    ])[v.memory_no::int] ELSE
        (ARRAY['Một buổi sáng đáng nhớ tại ','Cùng bạn bè ghé thăm ','Dành thời gian khám phá ','Lưu lại khoảnh khắc ở ','Đi chậm để cảm nhận '])[(v.memory_no % 5)::int + 1]
        || p.name || '. Ảnh trong bộ demo là ảnh minh họa.' END,
    (t.start_date + v.position + '09:30'::time)::timestamptz,
    (t.start_date + v.position + '09:32'::time)::timestamptz,
    (t.start_date + v.position + '09:32'::time)::timestamptz
FROM demo_seed_visits v
JOIN trips t ON t.id = ('d3300000-0000-0000-0000-' || lpad(v.trip_no::text, 12, '0'))::uuid
JOIN places p ON p.id = ('d3200000-0000-0000-0000-' || lpad(v.place_no::text, 12, '0'))::uuid
WHERE v.visited
ON CONFLICT (id) DO NOTHING;

-- 8. Two illustration images per Memory (100); no fabricated upload metadata.
INSERT INTO media (id, memory_id, url, mime_type, display_order, created_at)
SELECT
    ('d3800000-0000-0000-0000-' || lpad(n::text, 12, '0'))::uuid, m.id,
    CASE WHEN n <= 10 THEN
        CASE WHEN n % 2 = 1 THEN 'https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?w=1200'
            ELSE 'https://images.unsplash.com/photo-1544735716-392fe2489ffa?w=1200' END
    ELSE (ARRAY[
        'https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?w=1200',
        'https://images.unsplash.com/photo-1544735716-392fe2489ffa?w=1200',
        'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200'
    ])[(n % 3) + 1] END,
    'image/jpeg', (n - 1) % 2, m.created_at
FROM generate_series(1, 100) AS n
JOIN memories m ON m.id = ('d3700000-0000-0000-0000-' || lpad(((n - 1) / 2 + 1)::text, 12, '0'))::uuid
ON CONFLICT (id) DO NOTHING;

-- 9. Roll back the migration if counts, ownership or timelines are inconsistent.
DO $$
BEGIN
    IF (SELECT count(*) FROM users WHERE id::text LIKE 'd3100000-%') <> 50
        OR (SELECT count(*) FROM places WHERE id::text LIKE 'd3200000-%') <> 50
        OR (SELECT count(*) FROM trips WHERE id::text LIKE 'd3300000-%') <> 50
        OR (SELECT count(*) FROM trip_places WHERE id::text LIKE 'd3400000-%') <> 150
        OR (SELECT count(*) FROM itineraries WHERE id::text LIKE 'd3500000-%') <> 38
        OR (SELECT count(*) FROM itinerary_items WHERE id::text LIKE 'd3600000-%') <> 114
        OR (SELECT count(*) FROM memories WHERE id::text LIKE 'd3700000-%') <> 50
        OR (SELECT count(*) FROM media WHERE id::text LIKE 'd3800000-%') <> 100
    THEN
        RAISE EXCEPTION 'Demo row counts do not match the intended dataset';
    END IF;

    IF EXISTS (
        SELECT 1 FROM trip_places p JOIN trips t ON t.id = p.trip_id
        WHERE p.id::text LIKE 'd3400000-%'
            AND (t.user_id::text NOT LIKE 'd3100000-%' OR p.place_id::text NOT LIKE 'd3200000-%')
    ) OR EXISTS (
        SELECT 1 FROM itinerary_items x JOIN itineraries i ON i.id = x.itinerary_id JOIN trips t ON t.id = i.trip_id
        WHERE x.id::text LIKE 'd3600000-%' AND (
            i.id::text NOT LIKE 'd3500000-%' OR t.user_id::text NOT LIKE 'd3100000-%'
            OR x.date NOT BETWEEN t.start_date AND t.end_date OR x.end_time <= x.start_time
            OR NOT EXISTS (SELECT 1 FROM trip_places p WHERE p.trip_id = t.id AND p.place_id = x.place_id)
        )
    ) OR EXISTS (
        SELECT 1 FROM memories m JOIN trips t ON t.id = m.trip_id
        WHERE m.id::text LIKE 'd3700000-%' AND (
            m.user_id <> t.user_id OR m.user_id::text NOT LIKE 'd3100000-%'
            OR (m.experienced_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date NOT BETWEEN t.start_date AND t.end_date
            OR m.experienced_at > CURRENT_TIMESTAMP OR m.created_at < t.created_at
            OR NOT EXISTS (SELECT 1 FROM trip_places p WHERE p.trip_id = t.id AND p.place_id = m.place_id AND p.visited)
            OR NOT EXISTS (
                SELECT 1 FROM itinerary_items x JOIN itineraries i ON i.id = x.itinerary_id
                WHERE i.trip_id = t.id AND x.place_id = m.place_id
                    AND x.date = (m.experienced_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
                    AND (m.experienced_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::time BETWEEN x.start_time AND x.end_time
            )
        )
    ) OR EXISTS (
        SELECT 1 FROM media p JOIN memories m ON m.id = p.memory_id
        WHERE p.id::text LIKE 'd3800000-%' AND m.id::text NOT LIKE 'd3700000-%'
    ) THEN
        RAISE EXCEPTION 'Demo ownership, visit status or timeline is inconsistent';
    END IF;
END $$;
