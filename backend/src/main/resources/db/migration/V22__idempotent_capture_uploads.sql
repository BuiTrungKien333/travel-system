ALTER TABLE media ADD COLUMN client_request_id UUID;
ALTER TABLE media ADD COLUMN upload_sha256 VARCHAR(64);
CREATE UNIQUE INDEX uk_media_memory_capture_request ON media (memory_id, client_request_id)
    WHERE client_request_id IS NOT NULL;
