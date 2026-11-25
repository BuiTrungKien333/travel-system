-- DEV ONLY. Run explicitly with psql --single-transaction -v ON_ERROR_STOP=1.
-- ============================================================
-- 5. Seed fake / mock data for Provinces, Places, Trips, Memories & Media
-- ============================================================

-- 5.1. Seed provinces
INSERT INTO provinces (id, code, name)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'LC',  'Lào Cai'),
    ('10000000-0000-0000-0000-000000000002', 'QN',  'Quảng Nam'),
    ('10000000-0000-0000-0000-000000000003', 'HG',  'Hà Giang'),
    ('10000000-0000-0000-0000-000000000004', 'KG',  'Kiên Giang'),
    ('10000000-0000-0000-0000-000000000005', 'LD',  'Lâm Đồng'),
    ('10000000-0000-0000-0000-000000000006', 'TTH', 'Thừa Thiên Huế'),
    ('10000000-0000-0000-0000-000000000007', 'DN',  'Đà Nẵng')
ON CONFLICT (id) DO NOTHING;

-- 5.2. Seed places
INSERT INTO places (id, provider_place_id, name, address, latitude, longitude, province_id, created_at)
VALUES
    ('20000000-0000-0000-0000-000000000001', 'place_fansipan_01', 'Đỉnh Fansipan - Sa Pa', 'Thị xã Sa Pa, Tỉnh Lào Cai', 22.3033, 103.7753, '10000000-0000-0000-0000-000000000001', now()),
    ('20000000-0000-0000-0000-000000000002', 'place_hoian_02',    'Phố Cổ Hội An', 'Minh An, Hội An, Tỉnh Quảng Nam', 15.8801, 108.3380, '10000000-0000-0000-0000-000000000002', now()),
    ('20000000-0000-0000-0000-000000000003', 'place_mapileng_03',  'Đèo Mã Pí Lèng & Hẻm Tu Sản', 'Huyện Mèo Vạc, Tỉnh Hà Giang', 23.2389, 105.4167, '10000000-0000-0000-0000-000000000003', now()),
    ('20000000-0000-0000-0000-000000000004', 'place_baisao_04',    'Bãi Sao - Phú Quốc', 'Ấp Bãi Sao, Phường An Thới, TP. Phú Quốc, Kiên Giang', 10.0547, 104.0322, '10000000-0000-0000-0000-000000000004', now()),
    ('20000000-0000-0000-0000-000000000005', 'place_caudat_05',    'Đồi Chè Cầu Đất - Đà Lạt', 'Thôn Trường Thọ, Xã Trạm Hành, TP. Đà Lạt, Lâm Đồng', 11.9056, 108.5447, '10000000-0000-0000-0000-000000000005', now()),
    ('20000000-0000-0000-0000-000000000006', 'place_hue_06',       'Đại Nội Cố Đô Huế', 'Đường 23/8, Phường Thuận Hòa, TP. Huế, Thừa Thiên Huế', 16.4697, 107.5796, '10000000-0000-0000-0000-000000000006', now()),
    ('20000000-0000-0000-0000-000000000007', 'place_banahills_07', 'Cầu Vàng - Sun World Bà Nà Hills', 'Thôn An Sơn, Xã Hòa Ninh, Huyện Hòa Vang, TP. Đà Nẵng', 15.9953, 108.0003, '10000000-0000-0000-0000-000000000007', now())
ON CONFLICT (id) DO NOTHING;

-- 5.3. Seed trips
INSERT INTO trips (id, user_id, name, destination, start_date, end_date, preferences, constraints, status, created_at, updated_at)
VALUES
    -- Trip 1 (User: Sa Pa)
    (
        '30000000-0000-0000-0000-000000000001',
        'b0000000-0000-0000-0000-000000000002',
        'Chuyến đi săn mây Sa Pa - Fansipan',
        'Sa Pa, Lào Cai',
        '2025-10-14',
        '2025-10-17',
        'Trekking, ngắm cảnh, ẩm thực vùng cao',
        'Ngân sách vừa phải, di chuyển bằng xe khách',
        'COMPLETED',
        '2025-10-10 10:00:00+07',
        '2025-10-18 12:00:00+07'
    ),
    -- Trip 2 (User: Hoi An)
    (
        '30000000-0000-0000-0000-000000000002',
        'b0000000-0000-0000-0000-000000000002',
        'Khám phá phố cổ Hội An',
        'Hội An, Quảng Nam',
        '2025-11-19',
        '2025-11-22',
        'Văn hóa, ẩm thực đường phố, chụp ảnh đèn lồng',
        'Đi bộ và xe đạp dạo phố cổ',
        'COMPLETED',
        '2025-11-10 09:00:00+07',
        '2025-11-23 15:00:00+07'
    ),
    -- Trip 3 (User: Ha Giang)
    (
        '30000000-0000-0000-0000-000000000003',
        'b0000000-0000-0000-0000-000000000002',
        'Phượt đèo Mã Pí Lèng - Hà Giang',
        'Đồng Văn - Mèo Vạc, Hà Giang',
        '2025-12-04',
        '2025-12-07',
        'Phượt xe máy, ngắm cảnh thiên nhiên hùng vĩ, chèo thuyền',
        'Tay lái vững, thời tiết khô ráo',
        'COMPLETED',
        '2025-11-28 14:00:00+07',
        '2025-12-08 18:00:00+07'
    ),
    -- Trip 4 (User: Phu Quoc)
    (
        '30000000-0000-0000-0000-000000000004',
        'b0000000-0000-0000-0000-000000000002',
        'Kỳ nghỉ biển Phú Quốc',
        'Phú Quốc, Kiên Giang',
        '2026-01-17',
        '2026-01-20',
        'Nghỉ dưỡng biển, lặn ngắm san hô, hải sản tươi sống',
        'Resort gần biển',
        'COMPLETED',
        '2026-01-05 11:00:00+07',
        '2026-01-21 16:00:00+07'
    ),
    -- Trip 5 (User: Da Lat)
    (
        '30000000-0000-0000-0000-000000000005',
        'b0000000-0000-0000-0000-000000000002',
        'Đà Lạt mùa sương mờ',
        'Đà Lạt, Lâm Đồng',
        '2026-02-09',
        '2026-02-12',
        'Cafe view đồi chè, không khí lạnh se se, săn mây',
        'Thuê xe máy di chuyển',
        'COMPLETED',
        '2026-02-01 08:30:00+07',
        '2026-02-13 14:00:00+07'
    ),
    -- Trip 6 (Admin: Hue)
    (
        '30000000-0000-0000-0000-000000000006',
        'a0000000-0000-0000-0000-000000000001',
        'Chuyến khảo sát Cố đô Huế',
        'Huế, Thừa Thiên Huế',
        '2026-02-21',
        '2026-02-24',
        'Di tích lịch sử, khảo sát di sản văn hóa thế giới',
        'Lịch trình khảo sát thực địa kết hợp họp đối tác',
        'COMPLETED',
        '2026-02-15 09:00:00+07',
        '2026-02-25 10:00:00+07'
    ),
    -- Trip 7 (Admin: Da Nang)
    (
        '30000000-0000-0000-0000-000000000007',
        'a0000000-0000-0000-0000-000000000001',
        'Khảo sát dịch vụ du lịch Đà Nẵng',
        'Đà Nẵng',
        '2026-02-28',
        '2026-03-03',
        'Điểm tham quan nổi tiếng, kiểm tra chất lượng dịch vụ',
        'Làm việc cùng các đơn vị lữ hành địa phương',
        'COMPLETED',
        '2026-02-20 10:00:00+07',
        '2026-03-04 11:00:00+07'
    )
ON CONFLICT (id) DO NOTHING;

-- 5.4. Seed trip_places
INSERT INTO trip_places (id, trip_id, place_id, priority, estimated_duration, notes, planned_order, visited, created_at, updated_at)
VALUES
    ('40000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 1, 240, 'Đi cáp treo lên đỉnh Fansipan buổi sáng sớm', 1, true, now(), now()),
    ('40000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', 1, 180, 'Dạo phố cổ Hội An và thả đèn hoa đăng', 1, true, now(), now()),
    ('40000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000003', 1, 300, 'Ngắm đèo Mã Pí Lèng và chèo thuyền sông Nho Quế', 1, true, now(), now()),
    ('40000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000004', 1, 240, 'Tắm biển Bãi Sao và thưởng thức hải sản', 1, true, now(), now()),
    ('40000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000005', 1, 150, 'Check-in đồi chè Cầu Đất và uống trà nóng', 1, true, now(), now()),
    ('40000000-0000-0000-0000-000000000006', '30000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000006', 1, 240, 'Khảo sát kiến trúc Đại Nội Huế', 1, true, now(), now()),
    ('40000000-0000-0000-0000-000000000007', '30000000-0000-0000-0000-000000000007', '20000000-0000-0000-0000-000000000007', 1, 200, 'Khảo sát cảnh quan Cầu Vàng Bà Nà Hills', 1, true, now(), now())
ON CONFLICT (id) DO NOTHING;

-- 5.5. Seed memories (with trip_id and place_id populated)
INSERT INTO memories (id, user_id, trip_id, place_id, note, experienced_at, created_at, updated_at)
VALUES
    -- Memory 1 (Regular User: Sapa Fansipan)
    (
        'd0000000-0000-0000-0000-000000000001',
        'b0000000-0000-0000-0000-000000000002',
        '30000000-0000-0000-0000-000000000001',
        '20000000-0000-0000-0000-000000000001',
        'Đón bình minh tuyệt đẹp trên đỉnh Fansipan - Sa Pa. Sáng sớm nhiệt độ 10 độ C, biển mây bao phủ bồng bềnh như chốn bồng lai tiên cảnh. Cảm giác chạm tay vào cột mốc 3.143m thật sự tự hào và khó quên!',
        '2025-10-15 06:30:00+07',
        '2025-10-15 08:00:00+07',
        '2025-10-15 08:00:00+07'
    ),
    -- Memory 2 (Regular User: Hoi An)
    (
        'd0000000-0000-0000-0000-000000000002',
        'b0000000-0000-0000-0000-000000000002',
        '30000000-0000-0000-0000-000000000002',
        '20000000-0000-0000-0000-000000000002',
        'Thả đèn hoa đăng trên dòng sông Hoài thơ mộng ở phố cổ Hội An. Không khí buổi tối rực rỡ sắc màu của hàng trăm chiếc đèn lồng lung linh cùng tiếng đàn hát bài chòi rộn ràng.',
        '2025-11-20 19:45:00+07',
        '2025-11-20 21:00:00+07',
        '2025-11-20 21:00:00+07'
    ),
    -- Memory 3 (Regular User: Ha Giang Ma Pi Leng)
    (
        'd0000000-0000-0000-0000-000000000003',
        'b0000000-0000-0000-0000-000000000002',
        '30000000-0000-0000-0000-000000000003',
        '20000000-0000-0000-0000-000000000003',
        'Chinh phục đèo Mã Pí Lèng hùng vĩ và đi thuyền ngắm hẻm vực Tu Sản - Hà Giang. Nước dòng sông Nho Quế xanh ngắt như ngọc bích, cảnh sắc thiên nhiên non nước Việt Nam quá đỗi tráng lệ!',
        '2025-12-05 14:15:00+07',
        '2025-12-05 16:30:00+07',
        '2025-12-05 16:30:00+07'
    ),
    -- Memory 4 (Regular User: Phu Quoc)
    (
        'd0000000-0000-0000-0000-000000000004',
        'b0000000-0000-0000-0000-000000000002',
        '30000000-0000-0000-0000-000000000004',
        '20000000-0000-0000-0000-000000000004',
        'Lặn ngắm rạn san hô và đón hoàng hôn buông xuống tại Bãi Sao - Phú Quốc. Nước biển trong vắt, bãi cát trắng mịn màng và bữa tối hải sản tươi sống bên bờ biển vô cùng lãng mạn.',
        '2026-01-18 17:50:00+07',
        '2026-01-18 19:30:00+07',
        '2026-01-18 19:30:00+07'
    ),
    -- Memory 5 (Regular User: Da Lat Tea Hill)
    (
        'd0000000-0000-0000-0000-000000000005',
        'b0000000-0000-0000-0000-000000000002',
        '30000000-0000-0000-0000-000000000005',
        '20000000-0000-0000-0000-000000000005',
        'Một buổi chiều bình yên tại đồi chè Cầu Đất - Đà Lạt. Thưởng thức ly trà olong thơm lừng giữa tiết trời se lạnh và làn sương mù lãng đãng quanh đồi chè xanh ngát.',
        '2026-02-10 15:30:00+07',
        '2026-02-10 16:45:00+07',
        '2026-02-10 16:45:00+07'
    ),
    -- Memory 6 (Admin User: Hue Imperial City)
    (
        'd0000000-0000-0000-0000-000000000006',
        'a0000000-0000-0000-0000-000000000001',
        '30000000-0000-0000-0000-000000000006',
        '20000000-0000-0000-0000-000000000006',
        'Chuyến khảo sát Cố đô Huế - Tham quan Hoàng Thành Đại Nội, Lăng Khải Định và thưởng thức ca Huế trên sông Hương. Nét đẹp cổ kính và trầm mặc của di sản văn hóa thế giới.',
        '2026-02-22 10:00:00+07',
        '2026-02-22 11:30:00+07',
        '2026-02-22 11:30:00+07'
    ),
    -- Memory 7 (Admin User: Da Nang Golden Bridge)
    (
        'd0000000-0000-0000-0000-000000000007',
        'a0000000-0000-0000-0000-000000000001',
        '30000000-0000-0000-0000-000000000007',
        '20000000-0000-0000-0000-000000000007',
        'Check-in Cầu Vàng - Bà Nà Hills (Đà Nẵng). Đôi bàn tay khổng lồ nâng đỡ cây cầu giữa mây trời tạo nên khung cảnh kiến trúc vô cùng ấn tượng.',
        '2026-03-01 09:15:00+07',
        '2026-03-01 10:45:00+07',
        '2026-03-01 10:45:00+07'
    )
ON CONFLICT (id) DO NOTHING;

-- 5.2. Seed media attached to memories
INSERT INTO media (id, memory_id, url, mime_type, file_size, width, height, display_order, created_at)
VALUES
    -- Media for Memory 1 (Fansipan - Sapa)
    (
        'e0000000-0000-0000-0000-000000000001',
        'd0000000-0000-0000-0000-000000000001',
        'https://images.unsplash.com/photo-1528127269322-539801943592?auto=format&fit=crop&w=1600&q=80',
        'image/jpeg',
        2458000,
        1920,
        1080,
        0,
        '2025-10-15 08:00:00+07'
    ),
    (
        'e0000000-0000-0000-0000-000000000002',
        'd0000000-0000-0000-0000-000000000001',
        'https://images.unsplash.com/photo-1509042239860-f550ce710b93?auto=format&fit=crop&w=1200&q=80',
        'image/jpeg',
        1823000,
        1080,
        1350,
        1,
        '2025-10-15 08:00:00+07'
    ),
    (
        'e0000000-0000-0000-0000-000000000003',
        'd0000000-0000-0000-0000-000000000001',
        'https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=1600&q=80',
        'image/jpeg',
        3120000,
        1920,
        1080,
        2,
        '2025-10-15 08:00:00+07'
    ),

    -- Media for Memory 2 (Hoi An)
    (
        'e0000000-0000-0000-0000-000000000004',
        'd0000000-0000-0000-0000-000000000002',
        'https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?auto=format&fit=crop&w=1600&q=80',
        'image/jpeg',
        2190000,
        1920,
        1280,
        0,
        '2025-11-20 21:00:00+07'
    ),
    (
        'e0000000-0000-0000-0000-000000000005',
        'd0000000-0000-0000-0000-000000000002',
        'https://images.unsplash.com/photo-1569154941061-e231b4725ef1?auto=format&fit=crop&w=1200&q=80',
        'image/jpeg',
        1540000,
        1200,
        1200,
        1,
        '2025-11-20 21:00:00+07'
    ),

    -- Media for Memory 3 (Ha Giang)
    (
        'e0000000-0000-0000-0000-000000000006',
        'd0000000-0000-0000-0000-000000000003',
        'https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=1600&q=80',
        'image/jpeg',
        3410000,
        2048,
        1152,
        0,
        '2025-12-05 16:30:00+07'
    ),
    (
        'e0000000-0000-0000-0000-000000000007',
        'd0000000-0000-0000-0000-000000000003',
        'https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?auto=format&fit=crop&w=1200&q=80',
        'image/jpeg',
        1980000,
        1080,
        1920,
        1,
        '2025-12-05 16:30:00+07'
    ),

    -- Media for Memory 4 (Phu Quoc)
    (
        'e0000000-0000-0000-0000-000000000008',
        'd0000000-0000-0000-0000-000000000004',
        'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1600&q=80',
        'image/jpeg',
        2670000,
        1920,
        1080,
        0,
        '2026-01-18 19:30:00+07'
    ),
    (
        'e0000000-0000-0000-0000-000000000009',
        'd0000000-0000-0000-0000-000000000004',
        'https://images.unsplash.com/photo-1544551763-46a013bb70d5?auto=format&fit=crop&w=1600&q=80',
        'image/jpeg',
        2890000,
        1920,
        1080,
        1,
        '2026-01-18 19:30:00+07'
    ),

    -- Media for Memory 5 (Da Lat)
    (
        'e0000000-0000-0000-0000-000000000010',
        'd0000000-0000-0000-0000-000000000005',
        'https://images.unsplash.com/photo-1501785888041-af3ef285b470?auto=format&fit=crop&w=1600&q=80',
        'image/jpeg',
        2310000,
        1920,
        1200,
        0,
        '2026-02-10 16:45:00+07'
    ),

    -- Media for Memory 6 (Hue)
    (
        'e0000000-0000-0000-0000-000000000011',
        'd0000000-0000-0000-0000-000000000006',
        'https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?auto=format&fit=crop&w=1600&q=80',
        'image/jpeg',
        2750000,
        1920,
        1080,
        0,
        '2026-02-22 11:30:00+07'
    ),
    (
        'e0000000-0000-0000-0000-000000000012',
        'd0000000-0000-0000-0000-000000000006',
        'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?auto=format&fit=crop&w=1200&q=80',
        'image/jpeg',
        1940000,
        1200,
        800,
        1,
        '2026-02-22 11:30:00+07'
    ),

    -- Media for Memory 7 (Da Nang)
    (
        'e0000000-0000-0000-0000-000000000013',
        'd0000000-0000-0000-0000-000000000007',
        'https://images.unsplash.com/photo-1569154941061-e231b4725ef1?auto=format&fit=crop&w=1600&q=80',
        'image/jpeg',
        3200000,
        2048,
        1365,
        0,
        '2026-03-01 10:45:00+07'
    )
ON CONFLICT (id) DO NOTHING;
