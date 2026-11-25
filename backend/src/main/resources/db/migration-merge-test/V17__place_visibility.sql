ALTER TABLE places ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'INTERNAL';
ALTER TABLE places ADD COLUMN owner_id UUID REFERENCES users(id);

-- Legacy manually added places must not become public search results.
-- A shared legacy place is assigned to its earliest reference owner; links remain intact.
WITH place_references AS (
    SELECT tp.place_id, t.user_id, t.created_at, t.id FROM trip_places tp JOIN trips t ON t.id = tp.trip_id
    UNION
    SELECT ii.place_id, t.user_id, t.created_at, t.id
    FROM itinerary_items ii JOIN itineraries i ON i.id = ii.itinerary_id JOIN trips t ON t.id = i.trip_id
    UNION
    SELECT m.place_id, m.user_id, m.created_at, m.id FROM memories m WHERE m.place_id IS NOT NULL
), owners AS (
    SELECT DISTINCT ON (place_id) place_id, user_id FROM place_references ORDER BY place_id, created_at, id
)
UPDATE places p SET source = 'USER', owner_id = owners.user_id FROM owners WHERE owners.place_id = p.id;

ALTER TABLE places ADD CONSTRAINT ck_place_source CHECK (
    (source = 'INTERNAL' AND owner_id IS NULL) OR (source = 'USER' AND owner_id IS NOT NULL)
);
CREATE INDEX idx_places_internal_province_name ON places(province_id, name, id) WHERE source = 'INTERNAL';
