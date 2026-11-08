-- Removing a trip date unschedules appointments without discarding legacy notes/times.
-- TripPlace remains the canonical membership/pool record. Archived appointments are
-- excluded from active schedule reads and retained for the later itinerary migration.
ALTER TABLE itinerary_items ADD COLUMN removed_at TIMESTAMPTZ;
CREATE INDEX idx_itinerary_items_active ON itinerary_items(itinerary_id, date) WHERE removed_at IS NULL;
