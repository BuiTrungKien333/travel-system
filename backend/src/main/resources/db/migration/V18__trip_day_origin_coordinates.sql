ALTER TABLE trip_days ADD COLUMN start_latitude DOUBLE PRECISION, ADD COLUMN start_longitude DOUBLE PRECISION;
ALTER TABLE trip_days ADD CONSTRAINT ck_trip_day_origin_coordinates CHECK (
    (start_latitude IS NULL AND start_longitude IS NULL) OR
    (start_latitude IS NOT NULL AND start_longitude IS NOT NULL
        AND start_latitude BETWEEN -90 AND 90 AND start_longitude BETWEEN -180 AND 180)
);
