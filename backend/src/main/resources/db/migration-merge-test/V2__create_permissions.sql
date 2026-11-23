-- ============================================================
-- V2: Create permissions and role_permissions tables with seed data
-- Roles supported: GUEST, USER, ADMIN
-- ============================================================

-- 1. Bảng permissions: lưu danh mục quyền trong hệ thống
CREATE TABLE permissions (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50)     NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT uk_permissions_name UNIQUE (name)
);

CREATE INDEX idx_permissions_name ON permissions (name);

-- 2. Bảng role_permissions: liên kết quyền với từng Role
CREATE TABLE role_permissions (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    role            VARCHAR(20)     NOT NULL,
    permission_id   UUID            NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE,
    CONSTRAINT uk_role_permissions UNIQUE (role, permission_id)
);

CREATE INDEX idx_role_permissions_role ON role_permissions (role);

-- ============================================================
-- 3. Seed dữ liệu quyền cơ bản (Basic Permissions)
-- ============================================================

INSERT INTO permissions (name, description) VALUES
    ('place:read',    'Xem thông tin địa điểm và tỉnh thành'),
    ('trip:read',     'Xem danh sách và chi tiết chuyến đi'),
    ('trip:create',   'Tạo mới và quản lý chuyến đi cá nhân'),
    ('memory:create', 'Lưu kỷ niệm và hình ảnh trải nghiệm'),
    ('admin:manage',  'Toàn quyền quản trị hệ thống');

-- ============================================================
-- 4. Phân bổ quyền cho 3 Role chính: GUEST, USER, ADMIN
-- ============================================================

-- Quyền cho GUEST (Khách vãng lai: chỉ xem địa điểm và chuyến đi công khai)
INSERT INTO role_permissions (role, permission_id)
SELECT 'GUEST', id FROM permissions WHERE name IN (
    'place:read',
    'trip:read'
);

-- Quyền cho USER (Người dùng đã đăng nhập: xem, tạo chuyến đi, lưu kỷ niệm)
INSERT INTO role_permissions (role, permission_id)
SELECT 'USER', id FROM permissions WHERE name IN (
    'place:read',
    'trip:read',
    'trip:create',
    'memory:create'
);

-- Quyền cho ADMIN (Quản trị viên: toàn bộ quyền người dùng + quyền quản trị)
INSERT INTO role_permissions (role, permission_id)
SELECT 'ADMIN', id FROM permissions WHERE name IN (
    'place:read',
    'trip:read',
    'trip:create',
    'memory:create',
    'admin:manage'
);
