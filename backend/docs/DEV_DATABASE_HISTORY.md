# Development database migration compatibility

## Cause

The local PostgreSQL `travel_db` was created with the original `merge-test`
numbering. Its V4 is `V4__add_memory_permissions_and_indexes.sql` with checksum
`2000036775`; canonical V4 is `V4__add_place_context_fields.sql` with checksum
`21336257`. These histories cannot validate against the same location.

## Recovery

`FlywayHistoryConfiguration` applies only to `dev`. It recognizes the exact legacy
V4 script and checksum and selects `db/migration-merge-test`. V1-V24 and the lexical
RAG script are restored byte-for-byte from commit `cb3d8e4`. Fresh and canonical
databases keep `db/migration`. Unknown checksums still fail validation. Production
configuration is unchanged. No repair, baseline, clean, checksum rewrite, or
Hibernate automatic schema update is used.

When pgvector is unavailable on that legacy database, `db/rag-lexical` preserves
the original lexical retrieval migration. Vector indexing still requires pgvector.

## Current application schema

Restoring V1-V24 alone fixes Flyway validation but leaves the legacy schema missing
new application fields and tables, starting with `users.locked_at`. V24.1-V24.7
copy canonical V20-V26 unchanged, adding planning drafts, authorization/audit,
execution outbox, admin catalog metadata, sample photo arrays, and integration
health checks. Province names are updated by code as in canonical V24; province
IDs and existing foreign keys are retained.

The legacy database already has catalog/navigation/photo versions V25-V29.
Their original implementations are absent from this checkout. These records stay
untouched and remain future migrations under Flyway's default validation rules;
this does not verify their missing implementations. Bridge versions V24.1-V24.7
sit after the recovered V24, before those reserved versions. Only the recognized
legacy configuration enables `outOfOrder` so these additions can run after V29.
Repeated startup applies no bridge migration twice. Recover the original V25-V29
sources before extending or rebuilding that catalog history.

## Verification

Run on Java 21 with Docker available:

```powershell
.\mvnw.cmd '-Dapi.version=1.44' '-Dtest=FlywayHistoryConfigurationTest,TravelBackendApplicationTests' test
```

Tests use disposable PostgreSQL 17 containers. They cover legacy upgrade with a
future catalog history entry, preserved media/history checksums, repeat startup,
rejection of unknown checksums, fresh canonical migrations, canonical V11 upgrade,
and production profile isolation.

For local startup verification, back up the database first and restore it into a
separate PostgreSQL database. Start with `--spring.profiles.active=dev` and override
`spring.datasource.url` to the copy. Require Flyway validation, Hibernate validation,
and successful startup before applying the same migrations to `travel_db`.

## Verified on 2026-11-25

- Java 21: 8 tests passed, no failures/errors/skips, using the command above.
- All 25 restored historical SQL resources match commit `cb3d8e4` byte-for-byte;
  all seven bridge files match their canonical source byte-for-byte.
- A PostgreSQL 18 copy restored from the pre-upgrade database backup applied seven
  bridge migrations, passed Hibernate validation, and started on port 18089.
- The existing local backend on port 8080 also applied the bridge migrations.
  Postman assertions against `/actuator/health` returned HTTP 200 and `status=UP`
  for both databases (four assertions passed, exit code 0).
- The original 30 Flyway history rows are identical between the live database and
  the restored copy. Fingerprints match across 26 compared data tables, excluding
  the new columns and the actively updated integration-health cache. Existing
  permissions and province names receive the intended canonical updates.
- A pre-upgrade dump is retained locally under
  `logs/db-backups/travel_db_before_flyway_20261125.dump` (Git-ignored).
- Full backend suite and provider integrations were not rerun. Missing V25-V29
  implementations remain a reproducibility limitation, not a startup blocker.
