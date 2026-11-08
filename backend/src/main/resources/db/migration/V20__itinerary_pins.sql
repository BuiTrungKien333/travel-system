ALTER TABLE itinerary_items ADD COLUMN pinned BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE itinerary_items ADD CONSTRAINT ck_itinerary_pin_time CHECK (NOT pinned OR arrival_at IS NOT NULL);
