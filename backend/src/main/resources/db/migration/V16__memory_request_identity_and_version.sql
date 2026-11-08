ALTER TABLE memories ADD COLUMN client_request_id UUID;
ALTER TABLE memories ADD COLUMN create_request_hash VARCHAR(64);
ALTER TABLE memories ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
CREATE UNIQUE INDEX uq_memories_user_request ON memories (user_id, client_request_id)
    WHERE client_request_id IS NOT NULL;
