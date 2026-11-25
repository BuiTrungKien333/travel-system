-- V16 already created request identity, edit versions, cleanup jobs and content hashes.
-- Keep those values and expose the same upload identity used by the merged backend.
ALTER TABLE media RENAME COLUMN content_hash TO upload_hash;
ALTER TABLE media ALTER COLUMN public_id TYPE VARCHAR(1024);
ALTER TABLE media_cleanup_jobs ALTER COLUMN public_id TYPE VARCHAR(1024);

-- The JDBC create path uses ON CONFLICT without a partial-index predicate.
-- PostgreSQL still allows multiple NULL request IDs in this unique index.
CREATE UNIQUE INDEX uq_memory_create_request ON memories(user_id, client_request_id);
