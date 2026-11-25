-- Opt-in curated metadata. Existing places are not automatically verified.
CREATE TABLE place_retrieval_profiles (
    place_id UUID PRIMARY KEY REFERENCES places(id) ON DELETE CASCADE,
    description TEXT NOT NULL CHECK (length(description) > 0),
    tags TEXT[] NOT NULL,
    estimated_duration_minutes INTEGER NOT NULL CHECK (estimated_duration_minutes > 0),
    source_url TEXT NOT NULL,
    reviewed_on DATE NOT NULL,
    verification_scope TEXT NOT NULL,
    coordinates_quality VARCHAR(30) NOT NULL DEFAULT 'APPROXIMATE_DEMO',
    hours_quality VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN',
    approved BOOLEAN NOT NULL DEFAULT false,
    content_version INTEGER NOT NULL DEFAULT 1 CHECK (content_version > 0)
);
