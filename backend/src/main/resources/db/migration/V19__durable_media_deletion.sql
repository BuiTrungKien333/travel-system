-- Receipts outlive media rows so retries/late uploads cannot resurrect deleted photos.
CREATE TABLE media_deletions (
    media_id uuid PRIMARY KEY,
    memory_id uuid NOT NULL REFERENCES memories(id),
    user_id uuid NOT NULL REFERENCES users(id),
    public_id varchar(1024),
    upload_hash varchar(64),
    requested_at timestamptz NOT NULL,
    next_attempt_at timestamptz NOT NULL,
    attempts integer NOT NULL DEFAULT 0,
    completed_at timestamptz
);
CREATE UNIQUE INDEX uq_media_deletion_upload ON media_deletions(memory_id, upload_hash) WHERE upload_hash IS NOT NULL;
CREATE INDEX idx_media_deletion_pending ON media_deletions(next_attempt_at, media_id) WHERE completed_at IS NULL;
CREATE INDEX idx_memories_deleted_cleanup ON memories(id) WHERE deleted_at IS NOT NULL;
