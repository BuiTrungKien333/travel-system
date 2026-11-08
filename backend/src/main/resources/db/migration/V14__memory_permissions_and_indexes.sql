-- ============================================================
-- V14: Add permissions and composite indexes for Memory & Media
-- ============================================================

-- 1. Insert new permissions for Memory and Media operations
INSERT INTO permissions (name, description) VALUES
    ('memory:read',   'Xem danh sách và chi tiết kỷ niệm cá nhân'),
    ('memory:delete', 'Xoá kỷ niệm cá nhân'),
    ('media:delete',  'Xoá hình ảnh hoặc tệp media trong kỷ niệm')
ON CONFLICT (name) DO NOTHING;

-- 2. Assign permissions to USER role
INSERT INTO role_permissions (role, permission_id)
SELECT 'USER', id FROM permissions WHERE name IN (
    'memory:read',
    'memory:delete',
    'media:delete'
)
ON CONFLICT (role, permission_id) DO NOTHING;

-- 3. Assign permissions to ADMIN role
INSERT INTO role_permissions (role, permission_id)
SELECT 'ADMIN', id FROM permissions WHERE name IN (
    'memory:read',
    'memory:delete',
    'media:delete'
)
ON CONFLICT (role, permission_id) DO NOTHING;

-- 4. Create composite indexes for optimal soft-delete and ordering queries
CREATE INDEX IF NOT EXISTS idx_memories_user_deleted ON memories (user_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_memories_trip_deleted ON memories (trip_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_memories_trip_user_deleted ON memories (trip_id, user_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_media_memory_display_order ON media (memory_id, display_order);

