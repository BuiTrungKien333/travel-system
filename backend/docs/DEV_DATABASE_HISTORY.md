# Local development database history

Some existing development databases were created with the original `merge-test`
migration numbering. Their V4 is `V4__add_memory_permissions_and_indexes.sql`
(checksum `2000036775`), while the current `develop` V4 adds place context fields
(checksum `21336257`). Both histories are immutable and cannot validate against
each other's migration locations.

`FlywayHistoryConfiguration` runs only with the `dev` profile. When it finds that
exact original V4 script and checksum, it selects `db/migration-merge-test` before
normal Flyway validation. Those SQL files were restored byte-for-byte from the
original local recovery copy. Fresh and canonical databases keep `db/migration`.
Unrecognized checksums still fail validation; production configuration is unchanged.

For these legacy databases, when PostgreSQL has no `vector` extension available,
the configured RAG location uses the original `db/rag-lexical` migration. This
preserves the existing lexical history and avoids requiring pgvector for startup.
Vector indexing still requires pgvector. Fresh database RAG setup is unchanged.

The local database may already contain catalog migrations V25/V26 while the restored
SQL history ends at V24. Flyway's default handling of future migrations preserves
those records and emits a warning. No migration history repair, reset, or checksum
rewrite is performed.

Verification on Java 21:

```powershell
.\mvnw.cmd '-Dapi.version=1.44' '-Dtest=FlywayHistoryConfigurationTest,TravelBackendApplicationTests' test
```

The API version argument is needed for the local Docker engine with the repository's
Testcontainers version. Tests use disposable PostgreSQL 17 containers, not the
development database.

## Verification on 2026-11-23

`feature/directions` combines `develop` (`49ace8c`) and the latest GitHub branch,
`fix/rag-ai-v2` (`b431862`). Neither merge input changes the canonical versioned
or RAG migrations. The merged backend passed all 533 existing tests. After adding
the compatibility configuration, the eight tests in the command above also passed,
including fresh migration, V11 upgrade, legacy checksum preservation, rejection of
unknown checksums, and production profile isolation.

The existing local PostgreSQL 18.4 database was checked with JDBC sessions forced
to `default_transaction_read_only=on`. Canonical locations reproduce checksum and
repeatable-migration validation failures. The compatibility configuration selects
`db/migration-merge-test` and `db/rag-lexical`, and validation succeeds. No local
migration, repair, clean, or history update was run.

V25/V26 remain future Java migrations: their source is absent from this branch,
so their implementations cannot be checked or reproduced here. Flyway's
[default future-migration handling](https://documentation.red-gate.com/fd/flyway-ignore-migration-patterns-setting-277579002.html)
allows validation, but does not verify those missing implementations. Recover their
original source before extending that catalog history. The compatibility behavior
is restricted to `dev`; it does not certify an existing production database.
