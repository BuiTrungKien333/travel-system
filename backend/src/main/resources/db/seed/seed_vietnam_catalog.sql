-- Curated real Vietnam landmark catalog seed for dev/local RAG and Admin Catalog.
-- Standalone script (not in db/rag) so automated tests expecting 8 R__10 fixtures remain untouched.
-- Idempotent: safe to re-run without overwriting existing place or profile edits.

-- 1. Remap legacy letter province codes (pre-V24) to official 2-digit codes (Decision 19/2025/QD-TTg)
--    so existing places become visible in RAG retrieval and Admin Catalog province filters.
WITH legacy_map(old_code, new_code) AS (
    VALUES
        ('LD',  '68'), -- Lâm Đồng
        ('LC',  '15'), -- Lào Cai
        ('TTH', '46'), -- Huế
        ('QN',  '48'), -- Quảng Nam -> Đà Nẵng
        ('HG',  '08'), -- Hà Giang -> Tuyên Quang
        ('KG',  '91')  -- Kiên Giang -> An Giang
), resolved AS (
    SELECT old_p.id AS old_id, new_p.id AS new_id
    FROM legacy_map m
    JOIN provinces old_p ON old_p.code = m.old_code
    JOIN provinces new_p ON new_p.code = m.new_code
)
UPDATE places p
SET province_id = r.new_id
FROM resolved r
WHERE p.province_id = r.old_id;

WITH legacy_map(old_code, new_code) AS (
    VALUES
        ('LD',  '68'),
        ('LC',  '15'),
        ('TTH', '46'),
        ('QN',  '48'),
        ('HG',  '08'),
        ('KG',  '91')
), resolved AS (
    SELECT old_p.id AS old_id, new_p.id AS new_id
    FROM legacy_map m
    JOIN provinces old_p ON old_p.code = m.old_code
    JOIN provinces new_p ON new_p.code = m.new_code
)
UPDATE trips t
SET destination_province_id = r.new_id
FROM resolved r
WHERE t.destination_province_id = r.old_id;

DELETE FROM provinces p
WHERE p.code IN ('LD', 'LC', 'TTH', 'QN', 'HG', 'KG')
  AND NOT EXISTS (SELECT 1 FROM places pl WHERE pl.province_id = p.id)
  AND NOT EXISTS (SELECT 1 FROM trips t WHERE t.destination_province_id = p.id);

-- 2. Seed 30 real landmarks across 5 major provinces currently missing catalog entries:
--    01 (Hà Nội), 79 (Hồ Chí Minh), 56 (Khánh Hòa), 22 (Quảng Ninh), 37 (Ninh Bình).
WITH catalog(id, province_code, name, address, lat, lon, place_type, category, description, tags, minutes, source_url) AS (
    VALUES
    -- Hà Nội (01)
    ('ca7a0000-0000-4000-8000-000000000101'::uuid, '01', 'Văn Miếu - Quốc Tử Giám',
     '58 Quốc Tử Giám, Đống Đa, Hà Nội', 21.0278, 105.8355, 'HYBRID', 'Historical',
     'Quần thể di tích lịch sử và trường đại học đầu tiên của Việt Nam với Khuê Văn Các, bia Tiến sĩ và không gian kiến trúc cổ kính.',
     ARRAY['HISTORY','CULTURE','ARCHITECTURE','TEMPLE'], 90, 'https://vanmieu.gov.vn/'),
    ('ca7a0000-0000-4000-8000-000000000102'::uuid, '01', 'Hồ Hoàn Kiếm và Đền Ngọc Sơn',
     'Đinh Tiên Hoàng, Hoàn Kiếm, Hà Nội', 21.0288, 105.8525, 'OUTDOOR', 'Landmark',
     'Biểu tượng trung tâm thủ đô với Tháp Rùa, cầu Thê Húc và Đền Ngọc Sơn, phù hợp dạo bộ, ngắm cảnh và tìm hiểu văn hóa Hà Nội.',
     ARRAY['HISTORY','CULTURE','SCENERY','PARK','LOCAL'], 75, 'https://vietnamtourism.gov.vn/'),
    ('ca7a0000-0000-4000-8000-000000000103'::uuid, '01', 'Hoàng thành Thăng Long',
     '19C Hoàng Diệu, Ba Đình, Hà Nội', 21.0352, 105.8403, 'HYBRID', 'Historical',
     'Di sản văn hóa thế giới lưu giữ dấu tích các triều đại phong kiến Việt Nam qua Đoan Môn, Điện Kính Thiên và khu khảo cổ 18 Hoàng Diệu.',
     ARRAY['HISTORY','CULTURE','ARCHITECTURE','MUSEUM'], 90, 'https://hoangthanhthanglong.vn/'),
    ('ca7a0000-0000-4000-8000-000000000104'::uuid, '01', 'Bảo tàng Dân tộc học Việt Nam',
     'Nguyễn Văn Huyên, Cầu Giấy, Hà Nội', 21.0403, 105.7986, 'HYBRID', 'Museum',
     'Bảo tàng giới thiệu đời sống, trang phục, nghi lễ và kiến trúc nhà truyền thống của 54 dân tộc Việt Nam trong không gian trong nhà và ngoài trời.',
     ARRAY['MUSEUM','CULTURE','HISTORY'], 105, 'https://www.vme.org.vn/'),
    ('ca7a0000-0000-4000-8000-000000000105'::uuid, '01', 'Bảo tàng Mỹ thuật Việt Nam',
     '66 Nguyễn Thái Học, Ba Đình, Hà Nội', 21.0306, 105.8371, 'INDOOR', 'Museum',
     'Không gian trưng bày các tác phẩm hội họa, điêu khắc, sơn mài và mỹ thuật cổ truyền Việt Nam qua nhiều thời kỳ.',
     ARRAY['MUSEUM','ART','CULTURE'], 90, 'https://vnfam.vn/'),
    ('ca7a0000-0000-4000-8000-000000000106'::uuid, '01', 'Phố cổ Hà Nội',
     'Khu vực 36 phố phường, Hoàn Kiếm, Hà Nội', 21.0341, 105.8500, 'OUTDOOR', 'Culture',
     'Khu phố lịch sử với kiến trúc nhà ống cổ, phố nghề truyền thống và nhịp sống địa phương đặc trưng của người Hà Nội.',
     ARRAY['LOCAL','CULTURE','ARCHITECTURE','SHOPPING'], 90, 'https://vietnamtourism.gov.vn/'),
    ('ca7a0000-0000-4000-8000-000000000107'::uuid, '01', 'Chợ Đồng Xuân',
     'Đồng Xuân, Hoàn Kiếm, Hà Nội', 21.0381, 105.8495, 'INDOOR', 'Market',
     'Chợ đầu mối lâu đời lớn nhất khu phố cổ Hà Nội, tập trung hàng hóa địa phương, đặc sản khô và trải nghiệm sinh hoạt bản địa.',
     ARRAY['MARKET','SHOPPING','LOCAL'], 60, 'https://vietnamtourism.gov.vn/'),
    ('ca7a0000-0000-4000-8000-000000000108'::uuid, '01', 'Chùa Trấn Quốc',
     'Đường Thanh Niên, Tây Hồ, Hà Nội', 21.0479, 105.8369, 'HYBRID', 'Temple',
     'Ngôi chùa cổ nổi tiếng nằm trên bán đảo nhỏ bên Hồ Tây với bảo tháp lục độ đài sen và cảnh quan thanh tịnh.',
     ARRAY['TEMPLE','HISTORY','SCENERY','ARCHITECTURE'], 60, 'https://vietnamtourism.gov.vn/'),

    -- TP. Hồ Chí Minh (79)
    ('ca7a0000-0000-4000-8000-000000000201'::uuid, '79', 'Dinh Độc Lập',
     '135 Nam Kỳ Khởi Nghĩa, Quận 1, TP. Hồ Chí Minh', 10.7770, 106.6953, 'HYBRID', 'Historical',
     'Di tích lịch sử quốc gia đặc biệt với kiến trúc hiện đại tiêu biểu thập niên 1960, khuôn viên cây xanh và hệ thống phòng khánh tiết, hầm chỉ huy.',
     ARRAY['HISTORY','ARCHITECTURE','MUSEUM'], 90, 'https://dinhdoclap.gov.vn/'),
    ('ca7a0000-0000-4000-8000-000000000202'::uuid, '79', 'Bảo tàng Chứng tích Chiến tranh',
     '28 Võ Văn Tần, Quận 3, TP. Hồ Chí Minh', 10.7795, 106.6921, 'INDOOR', 'Museum',
     'Bảo tàng chuyên đề trưng bày tư liệu, hiện vật và ảnh báo chí về lịch sử chiến tranh Việt Nam và thông điệp hòa bình.',
     ARRAY['MUSEUM','HISTORY'], 90, 'https://baotangchungtichchientranh.vn/'),
    ('ca7a0000-0000-4000-8000-000000000203'::uuid, '79', 'Chợ Bến Thành',
     'Lê Lợi, Quận 1, TP. Hồ Chí Minh', 10.7725, 106.6980, 'INDOOR', 'Market',
     'Ngôi chợ biểu tượng trung tâm Sài Gòn với tháp đồng hồ cửa Nam, nơi mua sắm đặc sản, thủ công mỹ nghệ và khám phá đời sống địa phương.',
     ARRAY['MARKET','SHOPPING','LOCAL'], 60, 'https://visithcmc.vn/'),
    ('ca7a0000-0000-4000-8000-000000000204'::uuid, '79', 'Bưu điện Trung tâm Sài Gòn',
     '02 Công xã Paris, Quận 1, TP. Hồ Chí Minh', 10.7800, 106.6999, 'INDOOR', 'Architecture',
     'Công trình kiến trúc cổ điển cuối thế kỷ 19 nổi bật với mái vòm thép, bản đồ lịch sử và không gian bưu chính vẫn đang hoạt động.',
     ARRAY['ARCHITECTURE','HISTORY','LOCAL'], 45, 'https://visithcmc.vn/'),
    ('ca7a0000-0000-4000-8000-000000000205'::uuid, '79', 'Bảo tàng Mỹ thuật TP. Hồ Chí Minh',
     '97A Phó Đức Chính, Quận 1, TP. Hồ Chí Minh', 10.7699, 106.6991, 'INDOOR', 'Museum',
     'Bảo tàng đặt trong tòa nhà kiến trúc Đông Dương cổ, lưu giữ bộ sưu tập tranh sơn mài, điêu khắc và mỹ thuật đương đại Nam Bộ.',
     ARRAY['MUSEUM','ART','ARCHITECTURE','CULTURE'], 90, 'https://baotangmythuattphcm.com.vn/'),
    ('ca7a0000-0000-4000-8000-000000000206'::uuid, '79', 'Phố đi bộ Nguyễn Huệ',
     'Đường Nguyễn Huệ, Quận 1, TP. Hồ Chí Minh', 10.7744, 106.7037, 'OUTDOOR', 'Landmark',
     'Quảng trường đi bộ sôi động giữa trung tâm thành phố, kết nối trụ sở UBND TP.HCM với bến Bạch Đằng, thích hợp dạo mát và ngắm cảnh đô thị.',
     ARRAY['LOCAL','SCENERY','OUTDOOR','RELAX'], 60, 'https://visithcmc.vn/'),
    ('ca7a0000-0000-4000-8000-000000000207'::uuid, '79', 'Chùa Ngọc Hoàng',
     '73 Mai Thị Lựu, Quận 1, TP. Hồ Chí Minh', 10.7920, 106.6982, 'HYBRID', 'Temple',
     'Ngôi chùa cổ (Phước Hải Tự) nổi tiếng với nghệ thuật điêu khắc gỗ, tượng thờ truyền thống và không gian tín ngưỡng đặc sắc.',
     ARRAY['TEMPLE','CULTURE','ARCHITECTURE'], 45, 'https://visithcmc.vn/'),

    -- Khánh Hòa (56)
    ('ca7a0000-0000-4000-8000-000000000301'::uuid, '56', 'Tháp Bà Ponagar',
     '61 Hai Tháng Tư, Nha Trang, Khánh Hòa', 12.2654, 109.1955, 'OUTDOOR', 'Historical',
     'Quần thể tháp Chăm cổ tọa lạc trên đồi Cù Lao nhìn ra sông Cái, tiêu biểu cho nghệ thuật kiến trúc và điêu khắc Chăm Pa.',
     ARRAY['HISTORY','CULTURE','ARCHITECTURE','TEMPLE'], 75, 'https://nhatrang-travel.com/'),
    ('ca7a0000-0000-4000-8000-000000000302'::uuid, '56', 'Viện Hải dương học Nha Trang',
     '01 Cầu Đá, Nha Trang, Khánh Hòa', 12.2078, 109.2145, 'INDOOR', 'Museum',
     'Cơ sở nghiên cứu và bảo tàng sinh vật biển lâu đời với hàng chục nghìn mẫu vật đại dương và khu bể trưng bày sinh vật sống.',
     ARRAY['MUSEUM','CULTURE','LOCAL'], 90, 'https://vnio.org.vn/'),
    ('ca7a0000-0000-4000-8000-000000000303'::uuid, '56', 'Chùa Long Sơn',
     '22 Đường 23/10, Nha Trang, Khánh Hòa', 12.2506, 109.1806, 'HYBRID', 'Temple',
     'Ngôi cổ tự dưới chân đồi Trại Thủy nổi bật với tượng Kim Thân Phật Tổ trắng trên đỉnh đồi và góc nhìn toàn cảnh thành phố Nha Trang.',
     ARRAY['TEMPLE','CULTURE','SCENERY'], 60, 'https://vietnamtourism.gov.vn/'),
    ('ca7a0000-0000-4000-8000-000000000304'::uuid, '56', 'Bãi biển Nha Trang',
     'Đường Trần Phú, Nha Trang, Khánh Hòa', 12.2388, 109.1967, 'OUTDOOR', 'Beach',
     'Dải bờ biển cát trắng chạy dọc đường Trần Phú với công viên ven biển rộng thoáng, thích hợp ngắm biển, dạo bộ và thư giãn.',
     ARRAY['BEACH','SCENERY','RELAX','OUTDOOR'], 90, 'https://vietnamtourism.gov.vn/'),
    ('ca7a0000-0000-4000-8000-000000000305'::uuid, '56', 'Chợ Đầm Nha Trang',
     'Phan Bội Châu, Nha Trang, Khánh Hòa', 12.2562, 109.1917, 'INDOOR', 'Market',
     'Khu chợ truyền thống lớn nhất Nha Trang, nổi tiếng với hải sản khô, đặc sản Khánh Hòa và không khí mua bán địa phương.',
     ARRAY['MARKET','SHOPPING','LOCAL'], 60, 'https://vietnamtourism.gov.vn/'),

    -- Quảng Ninh (22)
    ('ca7a0000-0000-4000-8000-000000000401'::uuid, '22', 'Bảo tàng Quảng Ninh',
     'Đường Trần Quốc Nghiễn, Hạ Long, Quảng Ninh', 20.9498, 107.0906, 'INDOOR', 'Museum',
     'Công trình kiến trúc kính đen hiện đại trưng bày lịch sử tự nhiên, văn hóa biển đảo và ngành công nghiệp than đặc trưng của Quảng Ninh.',
     ARRAY['MUSEUM','HISTORY','ARCHITECTURE','CULTURE'], 90, 'https://baotangquangninh.vn/'),
    ('ca7a0000-0000-4000-8000-000000000402'::uuid, '22', 'Cảng tàu khách quốc tế Hạ Long',
     'Đường Hạ Long, Bãi Cháy, Hạ Long, Quảng Ninh', 20.9513, 107.0566, 'OUTDOOR', 'Landmark',
     'Điểm ngắm vịnh Hạ Long và cửa ngõ tham quan Di sản Thiên nhiên Thế giới với cảnh quan bờ vịnh và kiến trúc ven biển nổi bật.',
     ARRAY['SCENERY','OUTDOOR','LOCAL'], 60, 'https://vietnamtourism.gov.vn/'),
    ('ca7a0000-0000-4000-8000-000000000403'::uuid, '22', 'Bãi biển Bãi Cháy',
     'Đường Kỳ Quan, Bãi Cháy, Hạ Long, Quảng Ninh', 20.9504, 107.0439, 'OUTDOOR', 'Beach',
     'Bãi biển nhân tạo rộng thoáng hướng ra vịnh Hạ Long, thuận tiện dạo bộ ven biển, ngắm cảnh hoàng hôn và thư giãn ngoài trời.',
     ARRAY['BEACH','RELAX','OUTDOOR','SCENERY'], 75, 'https://vietnamtourism.gov.vn/'),
    ('ca7a0000-0000-4000-8000-000000000404'::uuid, '22', 'Chợ Hạ Long I',
     'Đường Bạch Đằng, Hạ Long, Quảng Ninh', 20.9522, 107.0751, 'INDOOR', 'Market',
     'Chợ trung tâm thành phố Hạ Long chuyên bán hải sản tươi sống, chả mực, đặc sản biển và hàng thủ công địa phương.',
     ARRAY['MARKET','SHOPPING','LOCAL'], 60, 'https://vietnamtourism.gov.vn/'),
    ('ca7a0000-0000-4000-8000-000000000405'::uuid, '22', 'Khu di tích danh thắng Yên Tử',
     'Thượng Yên Công, Uông Bí, Quảng Ninh', 21.1583, 106.7186, 'OUTDOOR', 'Temple',
     'Trung tâm Phật giáo Trúc Lâm nổi tiếng với hệ thống chùa cổ, rừng trúc và cảnh quan núi non hùng vĩ.',
     ARRAY['TEMPLE','HISTORY','SCENERY','OUTDOOR'], 150, 'https://vietnamtourism.gov.vn/'),

    -- Ninh Bình (37)
    ('ca7a0000-0000-4000-8000-000000000501'::uuid, '37', 'Quần thể danh thắng Tràng An',
     'Tràng An, Hoa Lư, Ninh Bình', 20.2506, 105.9154, 'OUTDOOR', 'Scenery',
     'Di sản Văn hóa và Thiên nhiên Thế giới nổi bật với hệ thống hang động xuyên thủy, núi đá vôi trùng điệp và các ngôi đền cổ bên dòng sông Sào Khê.',
     ARRAY['SCENERY','OUTDOOR','CULTURE','HISTORY'], 150, 'https://disantrangan.vn/'),
    ('ca7a0000-0000-4000-8000-000000000502'::uuid, '37', 'Tam Cốc - Bích Động',
     'Ninh Hải, Hoa Lư, Ninh Bình', 20.2156, 105.9367, 'OUTDOOR', 'Scenery',
     'Cảnh quan sông Ngô Đồng uốn lượn qua ba hang đá tự nhiên và cánh đồng lúa yên bình cùng chùa cổ Bích Động.',
     ARRAY['SCENERY','OUTDOOR','RELAX','LOCAL'], 120, 'https://vietnamtourism.gov.vn/'),
    ('ca7a0000-0000-4000-8000-000000000503'::uuid, '37', 'Hang Múa',
     'Ninh Xuân, Hoa Lư, Ninh Bình', 20.2321, 105.9366, 'OUTDOOR', 'Scenery',
     'Điểm ngắm toàn cảnh vùng núi đá vôi và cánh đồng Tam Cốc từ đỉnh Ngọa Long sau hành trình leo gần 500 bậc đá.',
     ARRAY['SCENERY','OUTDOOR','PARK'], 90, 'https://vietnamtourism.gov.vn/'),
    ('ca7a0000-0000-4000-8000-000000000504'::uuid, '37', 'Cố đô Hoa Lư',
     'Trường Yên, Hoa Lư, Ninh Bình', 20.2845, 105.9089, 'HYBRID', 'Historical',
     'Kinh đô đầu tiên của nhà nước phong kiến trung ương tập quyền Việt Nam với đền thờ vua Đinh Tiên Hoàng và vua Lê Đại Hành.',
     ARRAY['HISTORY','CULTURE','ARCHITECTURE','TEMPLE'], 75, 'https://vietnamtourism.gov.vn/'),
    ('ca7a0000-0000-4000-8000-000000000505'::uuid, '37', 'Chùa Bái Đính',
     'Gia Sinh, Gia Viễn, Ninh Bình', 20.2738, 105.8644, 'HYBRID', 'Temple',
     'Quần thể chùa lớn gồm khu chùa cổ trên núi và khu chùa mới với hành lang La Hán, bảo tháp và không gian tâm linh rộng lớn.',
     ARRAY['TEMPLE','ARCHITECTURE','CULTURE','SCENERY'], 120, 'https://vietnamtourism.gov.vn/')
), inserted AS (
    INSERT INTO places (id, name, address, latitude, longitude, province_id, place_type, category, source)
    SELECT c.id, c.name, c.address, c.lat, c.lon, p.id, c.place_type, c.category, 'INTERNAL'
    FROM catalog c
    JOIN provinces p ON p.code = c.province_code
    ON CONFLICT (id) DO NOTHING
    RETURNING id
)
INSERT INTO place_retrieval_profiles (
    place_id, description, tags, estimated_duration_minutes,
    source_url, reviewed_on, verification_scope, coordinates_quality, hours_quality, approved
)
SELECT c.id, c.description, c.tags, c.minutes,
       c.source_url, CURRENT_DATE, 'CURATED_LANDMARK_METADATA', 'APPROXIMATE', 'UNKNOWN', true
FROM catalog c
ON CONFLICT (place_id) DO NOTHING;
