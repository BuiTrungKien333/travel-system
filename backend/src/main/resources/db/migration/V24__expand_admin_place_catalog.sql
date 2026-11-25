ALTER TABLE place_retrieval_profiles
    ADD COLUMN archived_at TIMESTAMPTZ;

UPDATE permissions SET description = 'Manage place catalog' WHERE name = 'admin:catalog';

CREATE INDEX idx_place_retrieval_profiles_active
    ON place_retrieval_profiles (approved, place_id)
    WHERE archived_at IS NULL;

CREATE TABLE admin_catalog_audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID NOT NULL REFERENCES users(id),
    place_id UUID NOT NULL REFERENCES places(id),
    action VARCHAR(20) NOT NULL CHECK (action IN ('CREATE', 'UPDATE', 'REVIEW', 'ARCHIVE', 'RESTORE')),
    before_data JSONB,
    after_data JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_admin_catalog_audit_place_created
    ON admin_catalog_audit_events (place_id, created_at DESC);

-- Current administrative province codes and names, per Decision 19/2025/QD-TTg.
-- Existing province IDs are retained when a code already exists so foreign keys stay valid.
INSERT INTO provinces (code, name) VALUES
    ('01', 'Hà Nội'),
    ('04', 'Cao Bằng'),
    ('08', 'Tuyên Quang'),
    ('11', 'Điện Biên'),
    ('12', 'Lai Châu'),
    ('14', 'Sơn La'),
    ('15', 'Lào Cai'),
    ('19', 'Thái Nguyên'),
    ('20', 'Lạng Sơn'),
    ('22', 'Quảng Ninh'),
    ('24', 'Bắc Ninh'),
    ('25', 'Phú Thọ'),
    ('31', 'Hải Phòng'),
    ('33', 'Hưng Yên'),
    ('37', 'Ninh Bình'),
    ('38', 'Thanh Hóa'),
    ('40', 'Nghệ An'),
    ('42', 'Hà Tĩnh'),
    ('44', 'Quảng Trị'),
    ('46', 'Huế'),
    ('48', 'Đà Nẵng'),
    ('51', 'Quảng Ngãi'),
    ('52', 'Gia Lai'),
    ('56', 'Khánh Hòa'),
    ('66', 'Đắk Lắk'),
    ('68', 'Lâm Đồng'),
    ('75', 'Đồng Nai'),
    ('79', 'Hồ Chí Minh'),
    ('80', 'Tây Ninh'),
    ('82', 'Đồng Tháp'),
    ('86', 'Vĩnh Long'),
    ('91', 'An Giang'),
    ('92', 'Cần Thơ'),
    ('96', 'Cà Mau')
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name;
