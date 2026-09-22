-- ============================================================
-- V4: Add context fields to places table for Rule Evaluation & AI
-- opening_time: Giờ mở cửa hàng ngày
-- closing_time: Giờ đóng cửa hàng ngày
-- place_type: Loại địa điểm (INDOOR, OUTDOOR, HYBRID, UNSPECIFIED)
-- ============================================================

ALTER TABLE places
    ADD COLUMN IF NOT EXISTS opening_time TIME,
    ADD COLUMN IF NOT EXISTS closing_time TIME,
    ADD COLUMN IF NOT EXISTS place_type VARCHAR(20) NOT NULL DEFAULT 'UNSPECIFIED';
