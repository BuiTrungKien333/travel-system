ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN session_version BIGINT NOT NULL DEFAULT 0;

-- Compatibility for accounts that existed before verification was introduced.
-- This is a migration policy, not proof that these legacy addresses were verified.
UPDATE users SET email_verified_at = CURRENT_TIMESTAMP;

CREATE TABLE auth_action_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    purpose VARCHAR(20) NOT NULL CHECK (purpose IN ('VERIFY_EMAIL', 'PASSWORD_RESET')),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_auth_action_tokens_user_purpose UNIQUE (user_id, purpose)
);
CREATE INDEX idx_auth_action_tokens_expiry ON auth_action_tokens(expires_at);
