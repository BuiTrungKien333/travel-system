ALTER TABLE users ADD COLUMN locked_at TIMESTAMPTZ;

INSERT INTO permissions(name, description) VALUES
 ('trip:update','Update own trips'), ('trip:delete','Delete own trips'),
 ('itinerary:manage','Manage own itinerary'), ('planning:generate','Generate AI proposals'),
 ('memory:update','Update own memories'), ('media:upload','Upload own media'),
 ('profile:update','Update own profile'), ('maps:use','Search maps and calculate routes'),
 ('admin:users','Manage accounts'), ('admin:catalog','Review catalog'),
 ('admin:ai:index','Index approved AI catalog'), ('admin:system:read','Read operational information')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions(role,permission_id)
SELECT roles.role,p.id FROM permissions p CROSS JOIN (VALUES ('USER'),('ADMIN')) roles(role)
WHERE p.name IN ('place:read','trip:read','trip:create','trip:update','trip:delete','itinerary:manage',
 'planning:generate','memory:read','memory:create','memory:update','memory:delete','media:upload','media:delete',
 'profile:update','maps:use')
ON CONFLICT (role,permission_id) DO NOTHING;
INSERT INTO role_permissions(role,permission_id)
SELECT 'ADMIN',id FROM permissions WHERE name LIKE 'admin:%'
ON CONFLICT (role,permission_id) DO NOTHING;
DELETE FROM role_permissions WHERE role='GUEST' AND permission_id IN
 (SELECT id FROM permissions WHERE name <> 'place:read');

CREATE TABLE admin_account_events (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 actor_id UUID NOT NULL REFERENCES users(id),
 target_id UUID NOT NULL REFERENCES users(id),
 old_role VARCHAR(20) NOT NULL, new_role VARCHAR(20) NOT NULL,
 was_locked BOOLEAN NOT NULL, is_locked BOOLEAN NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_admin_account_events_created ON admin_account_events(created_at DESC);

CREATE TABLE admin_catalog_events (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), actor_id UUID NOT NULL REFERENCES users(id),
 place_id UUID NOT NULL REFERENCES places(id), was_approved BOOLEAN NOT NULL,
 is_approved BOOLEAN NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
