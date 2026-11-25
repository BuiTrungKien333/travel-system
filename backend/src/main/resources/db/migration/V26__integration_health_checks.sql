CREATE TABLE integration_health_checks (
    integration_key VARCHAR(50) PRIMARY KEY,
    status VARCHAR(24) NOT NULL CHECK (status IN ('UP', 'DOWN', 'NOT_CONFIGURED', 'MOCK')),
    checked_at TIMESTAMPTZ NOT NULL,
    last_success_at TIMESTAMPTZ,
    duration_ms BIGINT NOT NULL DEFAULT 0 CHECK (duration_ms >= 0),
    detail_code VARCHAR(64)
);
