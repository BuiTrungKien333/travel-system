-- A trip's itinerary items are grouped by `date` and ordered by `sequence`.
-- `arrival_at` is local wall-clock time at the destination; it intentionally has no timezone.
ALTER TABLE itinerary_items
    ADD COLUMN arrival_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN duration_minutes INTEGER;

UPDATE itinerary_items
SET arrival_at = CASE
        WHEN start_time IS NOT NULL THEN date + start_time
        ELSE NULL
    END,
    duration_minutes = CASE
        WHEN start_time IS NOT NULL AND end_time IS NOT NULL THEN
            GREATEST(1, ROUND(EXTRACT(EPOCH FROM (end_time - start_time)) / 60.0)::INTEGER
                + CASE WHEN end_time < start_time THEN 1440 ELSE 0 END)
        ELSE 60
    END;

ALTER TABLE itinerary_items
    ALTER COLUMN duration_minutes SET DEFAULT 60,
    ALTER COLUMN duration_minutes SET NOT NULL,
    ADD CONSTRAINT chk_itinerary_item_duration_minutes
        CHECK (duration_minutes BETWEEN 1 AND 1440),
    ADD CONSTRAINT chk_itinerary_item_sequence_nonnegative
        CHECK (sequence >= 0),
    ADD CONSTRAINT chk_itinerary_item_distance_nonnegative
        CHECK (distance_from_previous IS NULL OR distance_from_previous >= 0),
    ADD CONSTRAINT chk_itinerary_item_travel_minutes_nonnegative
        CHECK (travel_time_from_previous IS NULL OR travel_time_from_previous >= 0);

CREATE INDEX idx_itinerary_items_schedule_order
    ON itinerary_items (itinerary_id, date, sequence, id);
