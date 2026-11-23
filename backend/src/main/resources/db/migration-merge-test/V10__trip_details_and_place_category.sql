ALTER TABLE trips
    ADD COLUMN transportation VARCHAR(50),
    ADD COLUMN budget NUMERIC(19, 2) CHECK (budget >= 0),
    ADD COLUMN description TEXT,
    ADD COLUMN cover_image VARCHAR(2048);

ALTER TABLE places ADD COLUMN category VARCHAR(100);
