ALTER TABLE media
    ADD COLUMN storage_deleted_at TIMESTAMPTZ;
CREATE UNIQUE INDEX uk_media_public_id ON media(public_id) WHERE public_id IS NOT NULL;
CREATE INDEX idx_memory_page ON memories(user_id, experienced_at DESC, id DESC) WHERE deleted_at IS NULL;

-- Also records upload intent before calling storage, so a process crash can be cleaned up.
CREATE TABLE media_cleanup_jobs (
    public_id VARCHAR(255) PRIMARY KEY,
    run_after TIMESTAMPTZ NOT NULL DEFAULT now(),
    attempts INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_media_cleanup_due ON media_cleanup_jobs(run_after);
