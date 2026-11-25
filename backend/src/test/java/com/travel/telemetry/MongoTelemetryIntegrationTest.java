package com.travel.telemetry;

import static org.assertj.core.api.Assertions.*;

import com.mongodb.*;
import com.mongodb.client.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.bson.Document;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.*;
import org.testcontainers.utility.DockerImageName;

class MongoTelemetryIntegrationTest {

    static PostgreSQLContainer<?> pg;
    static GenericContainer<?> mongo;
    static JdbcTemplate jdbc;
    static DriverManagerDataSource source;
    static MongoClient client;
    static MongoExecutionStore store;
    ExecutionOutbox outbox;
    UUID trip;

    @BeforeAll
    static void databases() {
        pg = new PostgreSQLContainer<>("postgres:17-alpine");
        pg.start();
        source = new DriverManagerDataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(source);
        mongo = new GenericContainer<>(DockerImageName.parse("mongo:8.0")).withExposedPorts(27017);
        mongo.start();
        client = MongoClients.create(
            MongoClientSettings.builder()
                .applyConnectionString(
                    new ConnectionString("mongodb://" + mongo.getHost() + ":" + mongo.getMappedPort(27017))
                )
                .applyToClusterSettings(s -> s.serverSelectionTimeout(1, TimeUnit.SECONDS))
                .applyToSocketSettings(s -> s.readTimeout(1, TimeUnit.SECONDS))
                .retryReads(false)
                .retryWrites(false)
                .build()
        );
        var beans = new StaticListableBeanFactory(Map.of("client", client));
        store = new MongoExecutionStore(
            beans.getBeanProvider(MongoClient.class),
            "telemetry_test",
            90,
            Clock.systemUTC()
        );
        store.collection();
    }

    @AfterAll
    static void close() {
        if (client != null) client.close();
        if (mongo != null) mongo.stop();
        if (pg != null) pg.stop();
    }

    @BeforeEach
    void setup() {
        jdbc.update("DELETE FROM ai_execution_outbox");
        jdbc.update("DELETE FROM trip_planning_drafts");
        store.collection().deleteMany(new Document());
        outbox = new ExecutionOutbox(jdbc);
        trip = UUID.randomUUID();
        jdbc.update(
            "INSERT INTO trips(id,user_id,name,status) VALUES (?,'b0000000-0000-0000-0000-000000000002','Private test trip','PLANNING')",
            trip
        );
    }

    UUID draft() {
        UUID id = UUID.randomUUID();
        jdbc.update(
            "INSERT INTO trip_planning_drafts(id,trip_id,user_id,client_request_id,request_json,trip_version,source_revision,created_at,expires_at) VALUES (?,?,'b0000000-0000-0000-0000-000000000002',?,'PRIVATE_PROMPT_AND_LOCATION',0,'test',?,?)",
            id,
            trip,
            UUID.randomUUID(),
            java.sql.Timestamp.from(Instant.now().minusSeconds(5)),
            java.sql.Timestamp.from(Instant.now().plus(Duration.ofDays(1)))
        );
        return id;
    }

    void complete(UUID id) {
        jdbc.update(
            """
            UPDATE trip_planning_drafts SET response_json=? WHERE id=?
            """,
            """
            {"status":"READY_WITH_WARNINGS","privateText":"DO_NOT_COPY","metadata":{"routeSource":"MAPBOX_DRIVING"},
            "executionReport":{"overallMode":"AI_ASSISTED","stages":[{"stage":"INTENT","source":"AI","method":"GEMINI_STRUCTURED_EXTRACTION","model":"gemini-test","attempts":2,"retries":1,"elapsedMillis":123,"reason":"https://private.example/?key=DO_NOT_COPY","rawPrompt":"DO_NOT_COPY"}]}}
            """,
            id
        );
    }

    ExecutionPublisher publisher() {
        var p = new ExecutionPublisher(outbox, store);
        ReflectionTestUtils.setField(p, "publishEnabled", true);
        return p;
    }

    @Test
    void snapshotIsAtomicSanitizedAndIdempotentAcrossRedelivery() {
        UUID id = draft();
        complete(id);
        var events = outbox.claim();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().payload()).doesNotContain(
            "DO_NOT_COPY",
            "PRIVATE_PROMPT",
            "private.example",
            "tripId",
            "userId"
        );
        store.publish(events.getFirst().payload());
        store.publish(events.getFirst().payload());
        outbox.delivered(events.getFirst());
        assertThat(store.collection().countDocuments()).isEqualTo(1);
        assertThat(outbox.state().pending()).isZero();
        var document = store.collection().find().first();
        assertThat(document.get("startedAt")).isInstanceOf(Date.class);
        assertThat(document.get("expiresAt")).isInstanceOf(Date.class);
        assertThat(store.read(30, 0).total()).isEqualTo(1);
        assertThat(store.read(30, 0).p95Ms()).isNotNull();
        jdbc.update("UPDATE trip_planning_drafts SET response_json=response_json WHERE id=?", id);
        assertThat(
            jdbc.queryForObject("SELECT count(*) FROM ai_execution_outbox WHERE id=?", Integer.class, id)
        ).isEqualTo(1);
    }

    @Test
    void rollbackDoesNotLeaveAnOutboxEvent() {
        UUID id = draft();
        new TransactionTemplate(new DataSourceTransactionManager(source)).executeWithoutResult(tx -> {
            complete(id);
            tx.setRollbackOnly();
        });
        assertThat(outbox.state().pending()).isZero();
        assertThat(
            jdbc.queryForObject("SELECT response_json FROM trip_planning_drafts WHERE id=?", String.class, id)
        ).isNull();
    }

    @Test
    void failedAndInterruptedDraftsProduceTerminalEventsWithoutResurrection() {
        UUID failed = draft();
        jdbc.update("UPDATE trip_planning_drafts SET failure_code='AI_SERVICE_UNAVAILABLE' WHERE id=?", failed);
        UUID interrupted = draft();
        jdbc.update("UPDATE trip_planning_drafts SET created_at=now()-interval '11 minutes' WHERE id=?", interrupted);
        outbox.maintain();
        assertThat(outbox.state().pending()).isEqualTo(2);
        publisher().tick();
        assertThat(store.read(30, 0).statuses()).containsExactly(new MongoExecutionStore.Point("FAILED", 2));
        assertThat(
            jdbc.queryForObject("SELECT failure_code FROM trip_planning_drafts WHERE id=?", String.class, interrupted)
        ).isEqualTo("INTERRUPTED");
    }

    @Test
    void leasePreventsConcurrentClaimsAndRejectsStaleAcknowledgement() {
        UUID id = draft();
        complete(id);
        var first = outbox.claim().getFirst();
        assertThat(outbox.claim()).isEmpty();
        jdbc.update("UPDATE ai_execution_outbox SET lease_until=now()-interval '1 second' WHERE id=?", id);
        var second = outbox.claim().getFirst();
        outbox.delivered(first);
        assertThat(outbox.state().pending()).isEqualTo(1);
        outbox.delivered(second);
        assertThat(outbox.state().pending()).isZero();
    }

    @Test
    @Timeout(40)
    void mongoOutageDoesNotBlockPostgresAndRecoveryDrainsTheQueue() {
        UUID id = draft();
        mongo.getDockerClient().pauseContainerCmd(mongo.getContainerId()).exec();
        try {
            complete(id);
            publisher().tick();
            assertThat(outbox.state().pending()).isEqualTo(1);
            assertThat(
                jdbc.queryForObject("SELECT attempts FROM ai_execution_outbox WHERE id=?", Integer.class, id)
            ).isEqualTo(1);
            var response = new TelemetryController(store, outbox, Clock.systemUTC()).list(30, 0).getData();
            assertThat(response.status()).isEqualTo("UNAVAILABLE");
            assertThat(response.data()).isNull();
        } finally {
            mongo.getDockerClient().unpauseContainerCmd(mongo.getContainerId()).exec();
        }
        jdbc.update("UPDATE ai_execution_outbox SET next_attempt_at=now() WHERE id=?", id);
        publisher().tick();
        assertThat(outbox.state().pending()).isZero();
        assertThat(store.collection().countDocuments()).isEqualTo(1);
    }

    @Test
    void validatorAndTtlIndexExistAndExpiredSnapshotsAreNotRepublished() {
        assertThatThrownBy(() -> store.collection().insertOne(new Document("_id", "invalid"))).isInstanceOf(
            MongoWriteException.class
        );
        assertThat(store.collection().listIndexes().into(new ArrayList<>())).anySatisfy(i -> {
            assertThat(i.get("expireAfterSeconds")).isInstanceOf(Number.class);
            assertThat(((Number) i.get("expireAfterSeconds")).longValue()).isZero();
        });
        UUID id = draft();
        jdbc.update("UPDATE trip_planning_drafts SET created_at=now()-interval '100 days' WHERE id=?", id);
        complete(id);
        var payload = Document.parse(
            jdbc.queryForObject("SELECT payload::text FROM ai_execution_outbox WHERE id=?", String.class, id)
        );
        payload.put("finishedAt", Instant.now().minus(Duration.ofDays(100)).toEpochMilli());
        store.publish(payload.toJson());
        assertThat(store.collection().countDocuments()).isZero();
    }
}
