-- Terminal snapshots are captured atomically with the PostgreSQL draft outcome.
CREATE TABLE ai_execution_outbox (
 id UUID PRIMARY KEY, payload JSONB NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 attempts INTEGER NOT NULL DEFAULT 0, delivered_at TIMESTAMPTZ,
 lease_token UUID, lease_until TIMESTAMPTZ, last_error_code VARCHAR(40)
);
CREATE INDEX ix_ai_execution_outbox_pending ON ai_execution_outbox(next_attempt_at) WHERE delivered_at IS NULL;
CREATE FUNCTION capture_ai_execution() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE proposal JSONB; stages JSONB; finished TIMESTAMPTZ := clock_timestamp();
BEGIN
 IF NEW.response_json IS NULL AND NEW.failure_code IS NULL THEN RETURN NEW; END IF;
 IF OLD.response_json IS NOT NULL OR OLD.failure_code IS NOT NULL THEN RETURN NEW; END IF;
 proposal := coalesce(NEW.response_json::jsonb,'{}'::jsonb);
 SELECT coalesce(jsonb_agg(jsonb_build_object(
  'stage',left(item->>'stage',32),'source',left(item->>'source',32),
  'method',CASE WHEN item->>'method' ~ '^[A-Z0-9_]{1,100}$' THEN item->>'method' ELSE NULL END,
  'model',CASE WHEN item->>'model' ~ '^([a-zA-Z0-9._-]+/)?[a-zA-Z0-9._-]{1,100}$' THEN item->>'model' ELSE NULL END,
  'attempts',greatest(0,coalesce((item->>'attempts')::int,0)),
  'retries',greatest(0,coalesce((item->>'retries')::int,0)),
  'elapsedMillis',greatest(0,coalesce((item->>'elapsedMillis')::bigint,0)),
  'reason',CASE WHEN item->>'reason' ~ '^[A-Z0-9_]{1,100}$' THEN item->>'reason' ELSE NULL END,
  'lastProviderStatus',item->'lastProviderStatus'
 )),'[]'::jsonb) INTO stages FROM (
  SELECT value item FROM jsonb_array_elements(coalesce(proposal->'executionReport'->'stages','[]'::jsonb)) LIMIT 16
 ) filtered;
 INSERT INTO ai_execution_outbox(id,payload) VALUES (NEW.id,jsonb_build_object(
  '_id',NEW.id::text,'draftId',NEW.id::text,'schemaVersion',1,'scope','TRIP_DRAFT',
  'startedAt',(extract(epoch FROM NEW.created_at)*1000)::bigint,
  'finishedAt',(extract(epoch FROM finished)*1000)::bigint,
  'durationMs',greatest(0,(extract(epoch FROM finished-NEW.created_at)*1000)::bigint),
  'status',CASE WHEN NEW.failure_code IS NOT NULL THEN 'FAILED' ELSE coalesce(proposal->>'status','UNKNOWN') END,
  'overallMode',coalesce(proposal->'executionReport'->>'overallMode','UNAVAILABLE'),
  'failureCode',NEW.failure_code,'stages',stages,
  'routeSource',proposal->'metadata'->>'routeSource'
 )) ON CONFLICT(id) DO NOTHING;
 RETURN NEW;
END $$;
CREATE TRIGGER trip_draft_execution_outbox AFTER UPDATE OF response_json,failure_code ON trip_planning_drafts
 FOR EACH ROW EXECUTE FUNCTION capture_ai_execution();
