ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE users ADD COLUMN google_subject VARCHAR(255);
ALTER TABLE users ADD CONSTRAINT uk_users_google_subject UNIQUE (google_subject);

CREATE TABLE auth_google_challenges (
    id UUID PRIMARY KEY,
    nonce_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_auth_google_challenges_expiry ON auth_google_challenges(expires_at);

CREATE TABLE auth_google_link_tickets (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    google_subject VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    session_version BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_auth_google_links_expiry ON auth_google_link_tickets(expires_at);
CREATE INDEX idx_auth_google_links_subject ON auth_google_link_tickets(google_subject);
ALTER TABLE auth_google_link_tickets ADD CONSTRAINT uk_auth_google_link_user_subject UNIQUE (user_id, google_subject);
