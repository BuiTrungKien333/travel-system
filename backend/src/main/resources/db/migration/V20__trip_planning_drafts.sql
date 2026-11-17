CREATE TABLE trip_planning_drafts (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_request_id UUID NOT NULL,
    request_json TEXT NOT NULL,
    trip_version BIGINT NOT NULL,
    source_revision TEXT NOT NULL,
    response_json TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    applied_at TIMESTAMPTZ,
    UNIQUE (trip_id, client_request_id)
);
CREATE INDEX ix_trip_planning_drafts_owner_created ON trip_planning_drafts(user_id, created_at);
