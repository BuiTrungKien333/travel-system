package com.travel.seed;

import com.travel.TestcontainersConfiguration;
import org.flywaydb.core.Flyway;
import org.springframework.core.io.ClassPathResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class DemoDataIntegrationTest {
    private static final String DEMO_USER = "d3100000-0000-0000-0000-000000000001";
    private static final String FIRST_TRIP = "d3300000-0000-0000-0000-000000000001";

    @Autowired DataSource dataSource;
    @Autowired PasswordEncoder passwords;
    private String schema;
    private Flyway flyway;

    @BeforeEach
    void createIsolatedSchema() throws SQLException {
        schema = "demo_seed_" + UUID.randomUUID().toString().replace("-", "");
        flyway = Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration").cleanDisabled(false).load();
        flyway.migrate();
        assertThat(flyway.info().current().getVersion().toString()).isEqualTo("19");
        assertThat(count("SELECT count(*) FROM users WHERE id::text LIKE 'd3100000-%'")).isZero();
    }

    @AfterEach
    void dropIsolatedSchema() {
        if (flyway != null) flyway.clean();
    }

    @Test
    void seedIsCoherentAndRerunsPreserveBothExistingAndEditedDemoData() throws Exception {
        execute("""
                INSERT INTO trips (id,user_id,name,destination,status)
                VALUES ('aab00000-0000-0000-0000-000000000001',
                'b0000000-0000-0000-0000-000000000002','Existing personal trip','Keep this','PLANNING')
                """);
        String personalBefore = scalar("SELECT row_to_json(t)::text FROM trips t WHERE name='Existing personal trip'");

        seed();

        assertCounts();
        assertThat(count("SELECT count(*) FROM flyway_schema_history WHERE script LIKE '%seed_local_demo%'"))
                .isZero();
        assertThat(scalar("SELECT email FROM users WHERE id='" + DEMO_USER + "'"))
                .isEqualTo("demo.local@example.com");
        assertThat(passwords.matches("DemoTravel!2026", scalar("SELECT password_hash FROM users WHERE id='" + DEMO_USER + "'")))
                .isTrue();
        assertThat(count("""
                SELECT count(*) FROM users WHERE id::text LIKE 'd3100000-%'
                  AND email_verified_at IS NOT NULL AND deleted_at IS NULL AND role='USER'
                """)).isEqualTo(50);
        assertThat(count("""
                SELECT count(*) FROM trips WHERE id::text LIKE 'd3300000-%'
                AND (user_id::text NOT LIKE 'd3100000-%'
                    OR start_date > end_date OR budget < 0 OR jsonb_typeof(preferences::jsonb) <> 'array')
                """)).isZero();
        assertThat(count("SELECT count(DISTINCT status) FROM trips WHERE id::text LIKE 'd3300000-%'"))
                .isEqualTo(4);
        assertThat(count("""
                SELECT count(*) FROM trips t WHERE t.id::text LIKE 'd3300000-%' AND (
                  (t.status='COMPLETED' AND t.end_date >= (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh')::date)
                  OR (t.status='ACTIVE' AND (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
                    NOT BETWEEN t.start_date AND t.end_date)
                  OR (t.status IN ('READY','PLANNING') AND
                    (t.start_date <= (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
                      OR EXISTS (SELECT 1 FROM memories m WHERE m.trip_id=t.id)))
                  OR (SELECT count(*) FROM itineraries i WHERE i.trip_id=t.id)
                    <> CASE WHEN t.status='PLANNING' THEN 0 ELSE 1 END)
                """)).isZero();
        assertThat(count("""
                SELECT count(*) FROM (
                  SELECT trip_id FROM trip_places WHERE id::text LIKE 'd3400000-%' GROUP BY trip_id
                  HAVING count(*) <> 3 OR min(planned_order) <> 0 OR max(planned_order) <> 2
                    OR count(DISTINCT planned_order) <> 3
                ) invalid
                """)).isZero();
        assertThat(count("SELECT count(*) FROM trip_places WHERE id::text LIKE 'd3400000-%' AND visited"))
                .isEqualTo(50);
        assertThat(count("""
                SELECT count(*) FROM trip_places p WHERE p.id::text LIKE 'd3400000-%'
                  AND p.visited <> EXISTS (SELECT 1 FROM memories m WHERE m.trip_id=p.trip_id AND m.place_id=p.place_id)
                """)).isZero();
        assertThat(count("""
                SELECT count(*) FROM itinerary_items i JOIN itineraries schedule ON schedule.id=i.itinerary_id
                JOIN trips t ON t.id=schedule.trip_id WHERE i.id::text LIKE 'd3600000-%'
                  AND (i.date < t.start_date OR i.date > t.end_date OR i.end_time <= i.start_time
                    OR NOT EXISTS (SELECT 1 FROM trip_places p WHERE p.trip_id=t.id AND p.place_id=i.place_id))
                """)).isZero();
        assertThat(count("""
                SELECT count(*) FROM memories m JOIN trips t ON t.id=m.trip_id
                WHERE m.id::text LIKE 'd3700000-%' AND (m.user_id<>t.user_id
                  OR (m.experienced_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date<t.start_date
                  OR (m.experienced_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date>t.end_date
                  OR NOT EXISTS (SELECT 1 FROM trip_places p WHERE p.trip_id=t.id AND p.place_id=m.place_id AND p.visited))
                """)).isZero();
        assertThat(count("""
                SELECT count(*) FROM memories m WHERE m.id::text LIKE 'd3700000-%'
                  AND (m.experienced_at > CURRENT_TIMESTAMP OR NOT EXISTS (
                    SELECT 1 FROM itinerary_items item JOIN itineraries schedule ON schedule.id=item.itinerary_id
                    WHERE schedule.trip_id=m.trip_id AND item.place_id=m.place_id
                      AND item.date=(m.experienced_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
                      AND (m.experienced_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::time
                        BETWEEN item.start_time AND item.end_time))
                """)).isZero();
        assertThat(count("""
                SELECT count(*) FROM (
                  SELECT 1 FROM trips t JOIN users u ON u.id=t.user_id
                    WHERE t.id::text LIKE 'd3300000-%' AND t.created_at<u.created_at
                  UNION ALL
                  SELECT 1 FROM trip_places p JOIN trips t ON t.id=p.trip_id JOIN places place ON place.id=p.place_id
                    WHERE p.id::text LIKE 'd3400000-%' AND (p.created_at<t.created_at OR p.created_at<place.created_at)
                  UNION ALL
                  SELECT 1 FROM itineraries schedule JOIN trips t ON t.id=schedule.trip_id
                    WHERE schedule.id::text LIKE 'd3500000-%' AND schedule.created_at<t.created_at
                  UNION ALL
                  SELECT 1 FROM itinerary_items item JOIN itineraries schedule ON schedule.id=item.itinerary_id
                    WHERE item.id::text LIKE 'd3600000-%' AND item.created_at<schedule.created_at
                  UNION ALL
                  SELECT 1 FROM memories m JOIN trips t ON t.id=m.trip_id
                    WHERE m.id::text LIKE 'd3700000-%' AND (m.created_at<t.created_at OR m.created_at<m.experienced_at)
                  UNION ALL
                  SELECT 1 FROM media photo JOIN memories m ON m.id=photo.memory_id
                    WHERE photo.id::text LIKE 'd3800000-%' AND photo.created_at<m.created_at
                ) invalid
                """)).isZero();
        assertThat(count("""
                SELECT count(*) FROM memories m WHERE m.id::text LIKE 'd3700000-%'
                  AND (SELECT count(*) FROM media photo WHERE photo.memory_id=m.id) <> 2
                """)).isZero();
        assertThat(count("""
                SELECT count(*) FROM (
                  SELECT memory_id FROM media WHERE id::text LIKE 'd3800000-%' GROUP BY memory_id
                  HAVING count(*)<>2 OR min(display_order)<>0 OR max(display_order)<>1
                ) invalid
                """)).isZero();
        assertThat(count("SELECT count(*) FROM auth_refresh_tokens WHERE user_id::text LIKE 'd3100000-%'"))
                .isZero();
        assertThat(count("SELECT count(*) FROM auth_action_tokens WHERE user_id::text LIKE 'd3100000-%'"))
                .isZero();

        execute("UPDATE trips SET name='My edited demo trip' WHERE id='" + FIRST_TRIP + "'");
        seed();

        assertCounts();
        assertThat(scalar("SELECT name FROM trips WHERE id='" + FIRST_TRIP + "'"))
                .isEqualTo("My edited demo trip");
        assertThat(scalar("SELECT row_to_json(t)::text FROM trips t WHERE name='Existing personal trip'"))
                .isEqualTo(personalBefore);
    }

    @Test
    void occupiedDemoUserIdRollsBackWithoutOverwritingAccountOrAddingChildren() throws Exception {
        execute("""
                INSERT INTO users (id,full_name,email,password_hash,role)
                VALUES ('d3100000-0000-0000-0000-000000000001','Existing User',
                  'occupied@example.com','preserve-this-hash','USER')
                """);

        assertThatThrownBy(this::seed).isInstanceOf(SQLException.class);

        assertThat(scalar("SELECT email FROM users WHERE id='" + DEMO_USER + "'"))
                .isEqualTo("occupied@example.com");
        assertThat(scalar("SELECT password_hash FROM users WHERE id='" + DEMO_USER + "'"))
                .isEqualTo("preserve-this-hash");
        assertThat(count("SELECT count(*) FROM trips WHERE id::text LIKE 'd3300000-%'"))
                .isZero();
        assertThat(count("SELECT count(*) FROM places WHERE id::text LIKE 'd3200000-%'"))
                .isZero();
    }

    @Test
    void occupiedDemoEmailRollsBackWithoutTakingOverExistingUser() throws Exception {
        execute("""
                INSERT INTO users (id,full_name,email,password_hash,role)
                VALUES ('aab00000-0000-0000-0000-000000000002','Existing User',
                  'demo.local@example.com','preserve-this-hash','USER')
                """);

        assertThatThrownBy(this::seed).isInstanceOf(SQLException.class);

        assertThat(scalar("SELECT id::text FROM users WHERE email='demo.local@example.com'"))
                .isEqualTo("aab00000-0000-0000-0000-000000000002");
        assertThat(count("SELECT count(*) FROM users WHERE id='" + DEMO_USER + "'"))
                .isZero();
        assertThat(count("SELECT count(*) FROM trips WHERE id::text LIKE 'd3300000-%'"))
                .isZero();
    }

    @Test
    void occupiedExpandedDemoEmailRollsBackWholeSeedWithoutAddingDemoAccounts() throws Exception {
        execute("""
                INSERT INTO users (id,full_name,email,password_hash,role)
                VALUES ('aab00000-0000-0000-0000-000000000003','Existing User',
                  'demo.user02@example.com','preserve-this-hash','USER')
                """);

        assertThatThrownBy(this::seed).isInstanceOf(SQLException.class);

        assertThat(scalar("SELECT password_hash FROM users WHERE email='demo.user02@example.com'"))
                .isEqualTo("preserve-this-hash");
        assertThat(count("SELECT count(*) FROM users WHERE id::text LIKE 'd3100000-%'"))
                .isZero();
        assertThat(count("SELECT count(*) FROM trips WHERE id::text LIKE 'd3300000-%'"))
                .isZero();
        assertThat(count("SELECT count(*) FROM flyway_schema_history WHERE script LIKE '%seed_local_demo%'"))
                .isZero();
    }

    private void assertCounts() throws SQLException {
        for (var entry : Map.of("users", "d3100000-", "places", "d3200000-", "trips", "d3300000-",
                "trip_places", "d3400000-", "itineraries", "d3500000-", "itinerary_items", "d3600000-",
                "memories", "d3700000-", "media", "d3800000-").entrySet()) {
            long expected = switch (entry.getKey()) {
                case "users", "places", "trips", "memories" -> 50;
                case "trip_places" -> 150;
                case "itineraries" -> 38;
                case "itinerary_items" -> 114;
                case "media" -> 100;
                default -> throw new IllegalStateException("Unknown demo table");
            };
            assertThat(count("SELECT count(*) FROM " + entry.getKey() + " WHERE id::text LIKE '" + entry.getValue() + "%'")).isEqualTo(expected);
        }
    }

    private void seed() throws SQLException, IOException {
        String sql = new ClassPathResource("db/demo/seed_local_demo.sql").getContentAsString(StandardCharsets.UTF_8);
        try (Connection connection = dataSource.getConnection()) {
            connection.setSchema(schema);
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                statement.execute(sql);
                connection.commit();
            } catch (SQLException failure) {
                connection.rollback();
                throw failure;
            }
        }
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setSchema(schema);
            try (var statement = connection.createStatement()) {
                try {
                    statement.execute(sql);
                } catch (SQLException failure) {
                    statement.execute("ROLLBACK");
                    throw failure;
                }
            }
        }
    }

    private long count(String sql) throws SQLException {
        return Long.parseLong(scalar(sql));
    }

    private String scalar(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setSchema(schema);
            try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
                assertThat(rows.next()).isTrue();
                return rows.getString(1);
            }
        }
    }
}
