package com.travel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class TravelBackendApplicationTests {

    private static final int LATEST_SCHEMA_VERSION = 26;
    // Recorded from the released V11 dev database, not calculated from today's files.
    private static final Map<String, Integer> RELEASED = Map.ofEntries(
        Map.entry("V1__init_schema.sql", 2030714601),
        Map.entry("V2__create_permissions.sql", -245121979),
        Map.entry("V3__seed_initial_users.sql", 1767179567),
        Map.entry("V4__add_place_context_fields.sql", 21336257),
        Map.entry("V5__add_weekly_opening_hours_to_places.sql", -269474230),
        Map.entry("V6__auth_refresh_tokens.sql", 630945778),
        Map.entry("V7__email_verification_and_password_reset.sql", 1481330679),
        Map.entry("V8__google_authentication.sql", 1028264627),
        Map.entry("V9__place_retrieval_profiles.sql", -275121748),
        Map.entry("V10__trip_metadata_and_days.sql", 606970459),
        Map.entry("V11__retain_removed_trip_schedule.sql", 1649251421)
    );

    @Autowired
    DataSource dataSource;

    @Test
    void contextLoads() {}

    @Test
    void approvedLegacyCatalogRemainsPublicWithoutPublishingNewPrivateLocations() throws Exception {
        String schema = isolatedSchema();
        Flyway legacy = migration(schema, MigrationVersion.fromVersion("14"));
        try {
            legacy.migrate();
            try (var connection = dataSource.getConnection()) {
                connection.setSchema(schema);
                try (var statement = connection.createStatement()) {
                    statement.execute(
                        """
                        INSERT INTO places(id,name,latitude,longitude,created_at) VALUES
                          ('11000000-0000-4000-8000-000000000001','Approved catalog',16,108,'2020-01-01'),
                          ('11000000-0000-4000-8000-000000000002','Legacy home',16,108,'2020-01-01');
                        INSERT INTO trips(id,user_id,name,destination,status) VALUES
                          ('21000000-0000-4000-8000-000000000001','b0000000-0000-0000-0000-000000000002','Legacy trip','Test','PLANNING');
                        INSERT INTO trip_places(trip_id,place_id) VALUES
                          ('21000000-0000-4000-8000-000000000001','11000000-0000-4000-8000-000000000001'),
                          ('21000000-0000-4000-8000-000000000001','11000000-0000-4000-8000-000000000002');
                        INSERT INTO place_retrieval_profiles(place_id,description,tags,estimated_duration_minutes,source_url,reviewed_on,verification_scope,approved)
                          VALUES ('11000000-0000-4000-8000-000000000001','Reviewed',ARRAY['test'],60,'https://example.com','2020-01-01','TEST',true);
                        """
                    );
                }
            }
            migration(schema, MigrationVersion.fromVersion("15")).migrate();
            try (var connection = dataSource.getConnection()) {
                connection.setSchema(schema);
                try (var statement = connection.createStatement()) {
                    statement.execute(
                        """
                        INSERT INTO places(id,name,latitude,longitude,source,owner_id,created_at)
                          SELECT '11000000-0000-4000-8000-000000000003','New private home',16,108,'USER',
                            'b0000000-0000-0000-0000-000000000002',installed_on+interval '1 second'
                          FROM flyway_schema_history WHERE version='15';
                        INSERT INTO place_retrieval_profiles(place_id,description,tags,estimated_duration_minutes,source_url,reviewed_on,verification_scope,approved)
                          VALUES ('11000000-0000-4000-8000-000000000003','Must stay private',ARRAY['test'],60,'https://example.com','2020-01-01','TEST',true);
                        """
                    );
                }
            }
            Flyway latest = migration(schema, MigrationVersion.LATEST);
            assertThat(latest.migrate().migrationsExecuted).isEqualTo(LATEST_SCHEMA_VERSION - 15);
            latest.validate();
            try (var connection = dataSource.getConnection()) {
                connection.setSchema(schema);
                try (
                    var statement = connection.createStatement();
                    var rows = statement.executeQuery(
                        """
                        SELECT id::text,source,owner_id::text FROM places WHERE id::text LIKE '11000000-%' ORDER BY id
                        """
                    )
                ) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString("source")).isEqualTo("INTERNAL");
                    assertThat(rows.getString("owner_id")).isNull();
                    for (int i = 0; i < 2; i++) {
                        assertThat(rows.next()).isTrue();
                        assertThat(rows.getString("source")).isEqualTo("USER");
                        assertThat(rows.getString("owner_id")).isEqualTo("b0000000-0000-0000-0000-000000000002");
                    }
                    assertThat(rows.next()).isFalse();
                }
                try (
                    var statement = connection.createStatement();
                    var rows = statement.executeQuery(
                        "SELECT count(*) FROM trip_places WHERE trip_id='21000000-0000-4000-8000-000000000001'"
                    )
                ) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getInt(1)).isEqualTo(2);
                }
            }
        } finally {
            legacy.clean();
        }
    }

    @Test
    void freshDatabaseHasLatestSchemaWithoutAutomaticallyCreatingDemoTrips() throws Exception {
        String schema = isolatedSchema();
        Flyway flyway = migration(schema, MigrationVersion.LATEST);
        try {
            flyway.migrate();
            flyway.validate();
            assertThat(flyway.info().current().getVersion().toString()).isEqualTo(
                String.valueOf(LATEST_SCHEMA_VERSION)
            );
            try (var connection = dataSource.getConnection()) {
                connection.setSchema(schema);
                try (
                    var statement = connection.createStatement();
                    var rows = statement.executeQuery(
                        """
                        SELECT (SELECT count(*) FROM trips) AS trips,
                               (SELECT count(*) FROM memories) AS memories,
                               (SELECT count(*) FROM users WHERE id::text LIKE 'd3100000-%') AS demo_users,
                               (SELECT count(*) FROM information_schema.columns
                                WHERE table_schema=current_schema() AND table_name='itinerary_items'
                                  AND column_name IN ('arrival_at','duration_minutes','removed_at')) AS schedule_columns,
                               (SELECT count(*) FROM permissions WHERE name IN
                                ('memory:read','memory:delete','media:delete')) AS media_permissions
                        """
                    )
                ) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getInt("trips")).isZero();
                    assertThat(rows.getInt("memories")).isZero();
                    assertThat(rows.getInt("demo_users")).isZero();
                    assertThat(rows.getInt("schedule_columns")).isEqualTo(3);
                    assertThat(rows.getInt("media_permissions")).isEqualTo(3);
                }
            }
        } finally {
            flyway.clean();
        }
    }

    @Test
    void upgradesReleasedV11WithoutChangingHistoryOrLosingAnOvernightVisit() throws Exception {
        String schema = isolatedSchema();
        Flyway legacy = migration(schema, MigrationVersion.fromVersion("11"));
        try {
            legacy.migrate();
            var applied = Arrays.stream(legacy.info().applied())
                .filter(info -> info.getVersion() != null)
                .toArray(MigrationInfo[]::new);
            assertThat(applied)
                .hasSize(RELEASED.size())
                .allSatisfy(info -> assertThat(info.getChecksum()).isEqualTo(RELEASED.get(info.getScript())));
            var before = Arrays.stream(applied).map(this::fingerprint).toList();
            try (var connection = dataSource.getConnection()) {
                connection.setSchema(schema);
                try (var statement = connection.createStatement()) {
                    statement.execute(
                        """
                        INSERT INTO places(id,name,latitude,longitude) VALUES ('10000000-0000-4000-8000-000000000001','Retained place',0,0);
                        INSERT INTO trips(id,user_id,name,destination,start_date,end_date,status)
                        VALUES ('20000000-0000-4000-8000-000000000001','b0000000-0000-0000-0000-000000000002',
                                'Retained trip','Test destination','2026-10-20','2026-10-21','PLANNING');
                        INSERT INTO itineraries(id,trip_id) VALUES
                                ('30000000-0000-4000-8000-000000000001','20000000-0000-4000-8000-000000000001');
                        INSERT INTO itinerary_items(id,itinerary_id,place_id,date,start_time,end_time,sequence,note)
                        VALUES ('40000000-0000-4000-8000-000000000001','30000000-0000-4000-8000-000000000001',
                                '10000000-0000-4000-8000-000000000001','2026-10-20','23:30','00:30',0,'Keep this note');
                        """
                    );
                }
            }
            Flyway latest = migration(schema, MigrationVersion.LATEST);
            assertThat(latest.migrate().migrationsExecuted).isEqualTo(LATEST_SCHEMA_VERSION - 11);
            latest.validate();
            assertThat(
                Arrays.stream(latest.info().applied())
                    .filter(info -> info.getVersion() != null)
                    .limit(before.size())
                    .map(this::fingerprint)
                    .toList()
            ).isEqualTo(before);
            try (var connection = dataSource.getConnection()) {
                connection.setSchema(schema);
                try (
                    var statement = connection.createStatement();
                    var rows = statement.executeQuery(
                        """
                        SELECT note,duration_minutes,arrival_at::text,start_time::text,end_time::text
                        FROM itinerary_items WHERE id='40000000-0000-4000-8000-000000000001'
                        """
                    )
                ) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString("note")).isEqualTo("Keep this note");
                    assertThat(rows.getInt("duration_minutes")).isEqualTo(60);
                    assertThat(rows.getString("arrival_at")).isEqualTo("2026-10-20 23:30:00");
                    assertThat(rows.getString("start_time")).isEqualTo("23:30:00");
                    assertThat(rows.getString("end_time")).isEqualTo("00:30:00");
                    assertThat(rows.next()).isFalse();
                }
                try (
                    var statement = connection.createStatement();
                    var rows = statement.executeQuery(
                        "SELECT source,owner_id::text FROM places WHERE id='10000000-0000-4000-8000-000000000001'"
                    )
                ) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString("source")).isEqualTo("USER");
                    assertThat(rows.getString("owner_id")).isEqualTo("b0000000-0000-0000-0000-000000000002");
                }
            }
        } finally {
            legacy.clean();
        }
    }

    private String fingerprint(MigrationInfo info) {
        return info.getScript() + ":" + info.getVersion() + ":" + info.getChecksum();
    }

    private Flyway migration(String schema, MigrationVersion target) {
        return Flyway.configure()
            .dataSource(dataSource)
            .schemas(schema)
            .defaultSchema(schema)
            .locations("classpath:db/migration")
            .target(target)
            .cleanDisabled(false)
            .load();
    }

    private AutoCloseable resetSchema(java.sql.Connection connection) throws java.sql.SQLException {
        String previous = connection.getSchema();
        return () -> connection.setSchema(previous != null ? previous : "public");
    }

    private String isolatedSchema() {
        return "migration_qa_" + UUID.randomUUID().toString().replace("-", "");
    }
}
