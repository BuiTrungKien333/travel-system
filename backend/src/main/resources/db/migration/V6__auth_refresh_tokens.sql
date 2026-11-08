CREATE TABLE auth_refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_auth_refresh_tokens_hash UNIQUE (token_hash)
);
CREATE INDEX idx_auth_refresh_tokens_user_id ON auth_refresh_tokens(user_id);
CREATE INDEX idx_auth_refresh_tokens_expires_at ON auth_refresh_tokens(expires_at);

-- V3 đã phát hành: không sửa checksum. Chỉ vô hiệu các tài khoản demo còn nguyên mật khẩu mặc định.
UPDATE users SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
WHERE id IN ('a0000000-0000-0000-0000-000000000001',
             'b0000000-0000-0000-0000-000000000002',
             'c0000000-0000-0000-0000-000000000003')
  AND password_hash = '$2a$10$71iAveyKYFu0t2qhxc91UuFh1HfNYqzmzAwyfWDtxN3Pv1ZuDv7Tm'
  AND deleted_at IS NULL;
