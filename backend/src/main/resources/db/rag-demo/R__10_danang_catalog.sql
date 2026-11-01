-- Opt-in demo only. Existence/description reviewed from cited sources on 2026-10-30.
-- Coordinates are approximate demo map anchors, NOT verified entrances.
-- Durations are planning estimates, NOT provider facts. No ticket prices/events inferred.
-- Re-running never overwrites existing place/profile data.
INSERT INTO provinces(id, code, name)
VALUES ('dada0000-0000-4000-8000-000000000048', '48', 'Đà Nẵng')
ON CONFLICT (code) DO NOTHING;

WITH data(id, name, address, lat, lon, kind, description, tags, minutes, source) AS (
VALUES
('dada0000-0000-4000-8000-000000000001'::uuid, 'Bảo tàng Điêu khắc Chăm', '02 đường 2/9, Đà Nẵng', 16.0603, 108.2233, 'INDOOR',
 'Bảo tàng giới thiệu văn hóa Chăm qua các tác phẩm điêu khắc. Điểm tham quan dành cho nhu cầu tìm hiểu lịch sử và di sản nghệ thuật.',
 ARRAY['MUSEUM','HISTORY','CULTURE','ART'], 90, 'https://danangfantasticity.com/wp-content/uploads/2026/01/Ho-Chieu-Di-san-Da-Nang.pdf'),
('dada0000-0000-4000-8000-000000000002'::uuid, 'Bảo tàng Mỹ thuật Đà Nẵng', '78 Lê Duẩn, Đà Nẵng', 16.0727, 108.2176, 'INDOOR',
 'Không gian trưng bày mỹ thuật hiện đại, tranh dân gian và sản phẩm mỹ nghệ truyền thống của Đà Nẵng và miền Trung. Dành cho nhu cầu xem tranh, khám phá nghệ thuật.',
 ARRAY['MUSEUM','ART','CULTURE'], 75, 'https://baotangdanang.vn/gioi-thieu-ve-bao-tang-my-thuat-da-nang'),
('dada0000-0000-4000-8000-000000000003'::uuid, 'Bảo tàng Đà Nẵng', '31 Trần Phú, Đà Nẵng', 16.0738, 108.2241, 'INDOOR',
 'Bảo tàng giới thiệu tự nhiên và lịch sử xã hội Đà Nẵng. Dành cho nhu cầu tìm hiểu thành phố và văn hóa địa phương.',
 ARRAY['MUSEUM','HISTORY','CULTURE','LOCAL'], 90, 'https://www.baotangdanang.vn/'),
('dada0000-0000-4000-8000-000000000004'::uuid, 'Chợ Hàn', '119 Trần Phú, Đà Nẵng', 16.0677, 108.2240, 'HYBRID',
 'Chợ ở trung tâm Đà Nẵng, có hàng thực phẩm, sản phẩm địa phương và hàng may mặc. Dành cho nhu cầu đi chợ, mua sắm và khám phá đời sống địa phương.',
 ARRAY['MARKET','SHOPPING','LOCAL'], 60, 'https://trademap.danang.gov.vn/HuongDanSuDung.pdf'),
('dada0000-0000-4000-8000-000000000005'::uuid, 'Bãi biển Mỹ Khê', 'Đường Võ Nguyên Giáp, Đà Nẵng', 16.0545, 108.2472, 'OUTDOOR',
 'Bãi biển với bờ cát và không gian ven biển rộng mở. Dành cho nhu cầu ngắm biển, dạo bộ ven bờ và thư giãn ngoài trời; không mặc định điều kiện an toàn để tắm biển.',
 ARRAY['BEACH','SCENERY','RELAX','OUTDOOR'], 75, 'https://danangfantasticity.com/en/bien-vi/bien-my-khe'),
('dada0000-0000-4000-8000-000000000006'::uuid, 'Công viên Biển Đông', 'Đường Võ Nguyên Giáp, Đà Nẵng', 16.0708, 108.2454, 'OUTDOOR',
 'Công viên ven biển gần Mỹ Khê với không gian ngoài trời và quảng trường. Dành cho nhu cầu dạo bộ, ngắm cảnh và chụp ảnh, không mặc định có sự kiện.',
 ARRAY['PARK','SCENERY','RELAX','OUTDOOR'], 45, 'https://visitdanang.travel/vi/cong-vien-bien-dong'),
('dada0000-0000-4000-8000-000000000007'::uuid, 'Cầu Rồng - điểm ngắm từ bờ sông', 'Bờ sông Hàn gần Cầu Rồng, Đà Nẵng', 16.0610, 108.2272, 'OUTDOOR',
 'Điểm quan sát kiến trúc Cầu Rồng và cảnh sông Hàn từ bờ sông. Dành cho nhu cầu ngắm cảnh đô thị, chụp ảnh; không bao gồm cam kết xem biểu diễn phun lửa.',
 ARRAY['ARCHITECTURE','SCENERY','OUTDOOR'], 30, 'https://cttdt.danangportal.gov.vn/vi/w/cau-rong-i'),
('dada0000-0000-4000-8000-000000000008'::uuid, 'Chùa Linh Ứng Bãi Bụt - Sơn Trà', 'Bãi Bụt, bán đảo Sơn Trà, Đà Nẵng', 16.1003, 108.2778, 'HYBRID',
 'Ngôi chùa tại Bãi Bụt trên bán đảo Sơn Trà, có cảnh quan hướng biển. Dành cho nhu cầu tìm hiểu không gian Phật giáo và ngắm cảnh; cần tuân thủ nội quy trang phục.',
 ARRAY['TEMPLE','CULTURE','SCENERY'], 75, 'https://chualinhung.vn/')
), inserted AS (
 INSERT INTO places(id, name, address, latitude, longitude, province_id, place_type)
 SELECT d.id, d.name, d.address, d.lat, d.lon, p.id, d.kind
 FROM data d CROSS JOIN provinces p WHERE p.code='48'
 ON CONFLICT (id) DO NOTHING
 RETURNING id
)
INSERT INTO place_retrieval_profiles(place_id, description, tags, estimated_duration_minutes,
 source_url, reviewed_on, verification_scope, approved)
SELECT id, description, tags, minutes, source, DATE '2026-10-30',
 'EXISTENCE_AND_DESCRIPTION_ONLY; duration and coordinates are demo estimates; hours unknown', true
FROM data
ON CONFLICT (place_id) DO NOTHING;
