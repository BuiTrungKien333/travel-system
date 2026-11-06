CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE TABLE IF NOT EXISTS place_embeddings (
    place_id UUID NOT NULL REFERENCES place_retrieval_profiles(place_id) ON DELETE CASCADE,
    model VARCHAR(100) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    embedding vector(768) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (place_id, model)
);
-- Exact search is sufficient for eight demo records; no approximate index needed.
