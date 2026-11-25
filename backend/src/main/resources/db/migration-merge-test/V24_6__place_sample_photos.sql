ALTER TABLE places
    ADD COLUMN sample_photo_urls TEXT[] NOT NULL DEFAULT '{}';

ALTER TABLE places
    ADD CONSTRAINT ck_places_sample_photo_urls_count
    CHECK (cardinality(sample_photo_urls) <= 8);
