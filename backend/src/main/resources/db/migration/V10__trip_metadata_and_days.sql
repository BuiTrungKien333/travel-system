-- Trip CRUD: extend existing tables without rewriting historical migrations.
ALTER TABLE trips
    ADD COLUMN time_zone VARCHAR(64) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN destination_province_id UUID REFERENCES provinces(id),
    ADD COLUMN budget_amount BIGINT,
    ADD COLUMN budget_currency VARCHAR(3),
    ADD COLUMN accommodation VARCHAR(200),
    ADD COLUMN notes VARCHAR(1000),
    ADD COLUMN client_request_id UUID,
    ADD COLUMN create_request_hash VARCHAR(64),
    ADD CONSTRAINT uk_trip_create_request UNIQUE(user_id, client_request_id),
    ADD CONSTRAINT ck_trip_budget CHECK (
        (budget_amount IS NULL AND budget_currency IS NULL) OR
        (budget_amount IS NOT NULL AND budget_amount BETWEEN 0 AND 1000000000000
            AND budget_currency IS NOT NULL AND budget_currency = 'VND')),
    ADD CONSTRAINT ck_trip_timezone CHECK (time_zone = 'Asia/Ho_Chi_Minh');

-- Enforce new writes; legacy invalid ranges must be audited rather than silently rewritten.
ALTER TABLE trips ADD CONSTRAINT ck_trip_date_range CHECK (
    (start_date IS NULL AND end_date IS NULL) OR
    (start_date IS NOT NULL AND end_date IS NOT NULL
        AND end_date >= start_date AND end_date - start_date < 365)
) NOT VALID;

CREATE INDEX idx_owned_trip_page ON trips(user_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE trip_days (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id UUID NOT NULL REFERENCES trips(id),
    date DATE NOT NULL,
    start_place_id UUID REFERENCES places(id),
    start_location VARCHAR(200),
    start_time TIME,
    return_to_start BOOLEAN NOT NULL DEFAULT false,
    transport VARCHAR(20) NOT NULL DEFAULT 'CAR',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_trip_day UNIQUE(trip_id, date)
);

-- Only backfill bounded, valid legacy ranges; do not invent missing dates.
INSERT INTO trip_days(trip_id, date)
SELECT t.id, d::date FROM trips t
CROSS JOIN LATERAL generate_series(t.start_date::timestamp, t.end_date::timestamp, interval '1 day') d
WHERE t.start_date IS NOT NULL AND t.end_date >= t.start_date
    AND t.end_date - t.start_date < 365;
