-- ============================================================
-- V3: Seed initial users for development and testing
-- Default password for all seed accounts: 123456
-- BCrypt Hash: $2a$10$71iAveyKYFu0t2qhxc91UuFh1HfNYqzmzAwyfWDtxN3Pv1ZuDv7Tm
-- ============================================================

INSERT INTO users (id, full_name, email, password_hash, role, created_at, updated_at)
VALUES
    -- 1. System Administrator
    ('a0000000-0000-0000-0000-000000000001', 'Travel Administrator', 'admin@travel.com', '$2a$10$71iAveyKYFu0t2qhxc91UuFh1HfNYqzmzAwyfWDtxN3Pv1ZuDv7Tm', 'ADMIN', now(), now()),

    -- 2. Standard Registered User
    ('b0000000-0000-0000-0000-000000000002', 'Regular User', 'user@travel.com', '$2a$10$71iAveyKYFu0t2qhxc91UuFh1HfNYqzmzAwyfWDtxN3Pv1ZuDv7Tm', 'USER', now(), now()),

    -- 3. Guest User
    ('c0000000-0000-0000-0000-000000000003', 'Guest User', 'guest@travel.com', '$2a$10$71iAveyKYFu0t2qhxc91UuFh1HfNYqzmzAwyfWDtxN3Pv1ZuDv7Tm', 'GUEST', now(), now())
ON CONFLICT (email) DO NOTHING;
