CREATE TABLE itinerary_day_settings (
    id UUID PRIMARY KEY,
    itinerary_id UUID NOT NULL REFERENCES itineraries(id) ON DELETE CASCADE,
    day_date DATE NOT NULL,
    origin_name VARCHAR(200),
    origin_address VARCHAR(500),
    origin_latitude DOUBLE PRECISION,
    origin_longitude DOUBLE PRECISION,
    departure_time TIME NOT NULL,
    return_to_start BOOLEAN NOT NULL DEFAULT FALSE,
    travel_mode VARCHAR(20) NOT NULL DEFAULT 'DRIVE',
    CONSTRAINT uq_itinerary_day_settings UNIQUE (itinerary_id, day_date),
    CONSTRAINT ck_day_origin_coordinates CHECK (
        (origin_latitude IS NULL AND origin_longitude IS NULL) OR
        (origin_latitude BETWEEN -90 AND 90 AND origin_longitude BETWEEN -180 AND 180)),
    CONSTRAINT ck_day_origin_pair CHECK ((origin_latitude IS NULL) = (origin_longitude IS NULL)),
    CONSTRAINT ck_day_return_origin CHECK (NOT return_to_start OR origin_name IS NOT NULL),
    CONSTRAINT ck_day_travel_mode CHECK (travel_mode IN ('DRIVE', 'WALK', 'BICYCLE'))
);
