-- Approved profiles are the explicitly curated public catalog. V17 conservatively
-- classified every referenced legacy place as private, including this public catalog.
-- Repair only legacy places that existed before V17, never newly entered USER locations.
UPDATE places p SET source = 'INTERNAL', owner_id = NULL
FROM place_retrieval_profiles r
WHERE r.place_id = p.id AND r.approved AND p.source = 'USER'
  AND p.created_at <= (SELECT installed_on FROM flyway_schema_history WHERE version = '17' AND success)
  AND r.reviewed_on <= (SELECT installed_on::date FROM flyway_schema_history WHERE version = '17' AND success);
