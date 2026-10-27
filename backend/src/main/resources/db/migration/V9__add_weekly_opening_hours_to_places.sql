-- ============================================================
-- V5: Add weekly_opening_hours JSON field to places table
-- ============================================================

ALTER TABLE places
    ADD COLUMN IF NOT EXISTS weekly_opening_hours TEXT;
