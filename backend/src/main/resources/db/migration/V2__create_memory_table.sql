-- V2: Create memory table
CREATE TABLE memory
(
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL,
    trip_id    UUID        NOT NULL,
    place_id   UUID        NOT NULL,
    content    TEXT        NOT NULL,
    visited_at TIMESTAMP   NOT NULL,
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP   NOT NULL,
    deleted_at TIMESTAMP,

    CONSTRAINT pk_memory PRIMARY KEY (id)
);

CREATE INDEX idx_memory_user_id ON memory (user_id);
CREATE INDEX idx_memory_trip_id ON memory (trip_id);
CREATE INDEX idx_memory_user_deleted ON memory (user_id, deleted_at);
CREATE INDEX idx_memory_trip_deleted ON memory (trip_id, deleted_at);
