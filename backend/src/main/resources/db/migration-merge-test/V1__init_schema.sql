-- ============================================================
-- V1: Initial schema
-- ============================================================

-- 1. users
CREATE TABLE users (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name       VARCHAR(100)    NOT NULL,
    email           VARCHAR(255)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    role            VARCHAR(20)     NOT NULL DEFAULT 'USER',
    avatar_url      VARCHAR(1000),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,

    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE INDEX idx_users_deleted_at ON users (deleted_at);
CREATE INDEX idx_users_role ON users (role);

-- 2. provinces (master data — 63 tỉnh/thành)
CREATE TABLE provinces (
    id      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    code    VARCHAR(10)     NOT NULL,
    name    VARCHAR(100)    NOT NULL,

    CONSTRAINT uk_provinces_code UNIQUE (code)
);

-- 3. places
CREATE TABLE places (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_place_id   VARCHAR(255),
    name                VARCHAR(255)    NOT NULL,
    address             TEXT,
    latitude            DOUBLE PRECISION NOT NULL,
    longitude           DOUBLE PRECISION NOT NULL,
    province_id         UUID,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT fk_places_province FOREIGN KEY (province_id) REFERENCES provinces (id)
);

CREATE INDEX idx_places_provider_place_id ON places (provider_place_id);
CREATE INDEX idx_places_province_id ON places (province_id);

-- 4. trips
CREATE TABLE trips (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID            NOT NULL,
    name                VARCHAR(255)    NOT NULL,
    destination         VARCHAR(255),
    start_date          DATE,
    end_date            DATE,
    preferences         TEXT,
    constraints         TEXT,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PLANNING',
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ,

    CONSTRAINT fk_trips_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_trips_user_id ON trips (user_id);
CREATE INDEX idx_trips_status ON trips (status);
CREATE INDEX idx_trips_deleted_at ON trips (deleted_at);

-- 5. trip_places
CREATE TABLE trip_places (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id             UUID            NOT NULL,
    place_id            UUID            NOT NULL,
    priority            INTEGER         DEFAULT 0,
    estimated_duration  INTEGER,
    notes               TEXT,
    planned_order       INTEGER,
    visited             BOOLEAN         NOT NULL DEFAULT false,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT fk_trip_places_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_trip_places_place FOREIGN KEY (place_id) REFERENCES places (id),
    CONSTRAINT uk_trip_places UNIQUE (trip_id, place_id)
);

CREATE INDEX idx_trip_places_trip_id ON trip_places (trip_id);
CREATE INDEX idx_trip_places_place_id ON trip_places (place_id);

-- 6. itineraries
CREATE TABLE itineraries (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id     UUID            NOT NULL,
    name        VARCHAR(255)    NOT NULL DEFAULT 'Default',
    status      VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT fk_itineraries_trip FOREIGN KEY (trip_id) REFERENCES trips (id)
);

CREATE INDEX idx_itineraries_trip_id ON itineraries (trip_id);

-- 7. itinerary_items
CREATE TABLE itinerary_items (
    id                          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    itinerary_id                UUID            NOT NULL,
    place_id                    UUID            NOT NULL,
    date                        DATE            NOT NULL,
    start_time                  TIME,
    end_time                    TIME,
    sequence                    INTEGER         NOT NULL,
    distance_from_previous      DOUBLE PRECISION,
    travel_time_from_previous   INTEGER,
    note                        TEXT,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT fk_itinerary_items_itinerary FOREIGN KEY (itinerary_id) REFERENCES itineraries (id),
    CONSTRAINT fk_itinerary_items_place FOREIGN KEY (place_id) REFERENCES places (id)
);

CREATE INDEX idx_itinerary_items_itinerary_id ON itinerary_items (itinerary_id);
CREATE INDEX idx_itinerary_items_date ON itinerary_items (date);

-- 8. memories
CREATE TABLE memories (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL,
    trip_id         UUID,
    place_id        UUID,
    note            VARCHAR(2000),
    experienced_at  TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,

    CONSTRAINT fk_memories_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_memories_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_memories_place FOREIGN KEY (place_id) REFERENCES places (id)
);

CREATE INDEX idx_memories_user_id ON memories (user_id);
CREATE INDEX idx_memories_trip_id ON memories (trip_id);
CREATE INDEX idx_memories_place_id ON memories (place_id);
CREATE INDEX idx_memories_deleted_at ON memories (deleted_at);

-- 9. media
CREATE TABLE media (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    memory_id       UUID            NOT NULL,
    url             VARCHAR(2048)   NOT NULL,
    mime_type       VARCHAR(50),
    file_size       BIGINT,
    width           INTEGER,
    height          INTEGER,
    display_order   INTEGER         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT fk_media_memory FOREIGN KEY (memory_id) REFERENCES memories (id)
);

CREATE INDEX idx_media_memory_id ON media (memory_id);
