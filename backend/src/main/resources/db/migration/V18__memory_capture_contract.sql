ALTER TABLE memories ADD COLUMN version bigint NOT NULL DEFAULT 0;
ALTER TABLE memories ADD COLUMN client_request_id uuid;
ALTER TABLE memories ADD COLUMN create_request_hash varchar(64);
CREATE UNIQUE INDEX uq_memory_create_request ON memories(user_id, client_request_id);

ALTER TABLE media ADD COLUMN upload_hash varchar(64);
ALTER TABLE media ADD COLUMN public_id varchar(1024);
CREATE UNIQUE INDEX uq_media_memory_upload ON media(memory_id, upload_hash);

CREATE INDEX idx_memories_owner_experienced ON memories(user_id, experienced_at DESC, id DESC)
    WHERE deleted_at IS NULL;
