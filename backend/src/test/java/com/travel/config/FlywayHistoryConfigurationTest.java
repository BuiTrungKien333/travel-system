package com.travel.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlywayHistoryConfigurationTest {
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @BeforeAll
    static void startDatabase() {
        POSTGRES.start();
    }

    @AfterAll
    static void stopDatabase() {
        POSTGRES.stop();
    }

    @Test
    void existingMergeHistoryValidatesWithoutChangingDataOrChecksums() throws Exception {
        String schema = schema();
        Flyway original = configuration(schema).locations(FlywayHistoryConfiguration.MERGE_TEST,
                FlywayHistoryConfiguration.LEXICAL_RAG).load();
        original.migrate();
        List<String> before = fingerprints(original);
        long mediaCount = mediaCount(original);
        assertThat(mediaCount).isPositive();
        assertThat(Arrays.stream(original.info().applied()).filter(info -> info.getVersion() != null
                && "4".equals(info.getVersion().toString())).findFirst().orElseThrow().getChecksum())
                .isEqualTo(2000036775);

        FluentConfiguration selected = configuration(schema).locations(FlywayHistoryConfiguration.CANONICAL,
                FlywayHistoryConfiguration.RAG);
        new FlywayHistoryConfiguration().customize(selected);
        Flyway restored = selected.load();

        restored.validate();
        assertThat(restored.migrate().migrationsExecuted).isZero();
        assertThat(fingerprints(restored)).isEqualTo(before);
        assertThat(mediaCount(restored)).isEqualTo(mediaCount);
    }

    @Test
    void freshDatabaseUsesTheDevelopHistory() {
        FluentConfiguration selected = configuration(schema()).locations(FlywayHistoryConfiguration.CANONICAL);
        new FlywayHistoryConfiguration().customize(selected);
        Flyway fresh = selected.load();

        fresh.migrate();
        fresh.validate();
        assertThat(fresh.info().current().getVersion().toString()).isEqualTo("19");
        assertThat(Arrays.stream(fresh.info().applied()).filter(info -> info.getVersion() != null
                && "4".equals(info.getVersion().toString())).findFirst().orElseThrow().getChecksum())
                .isEqualTo(21336257);
    }

    @Test
    void unknownChecksumsStillFailValidation() throws Exception {
        String schema = schema();
        Flyway original = configuration(schema).locations(FlywayHistoryConfiguration.MERGE_TEST).load();
        original.migrate();
        try (var connection = original.getConfiguration().getDataSource().getConnection();
             var statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE " + schema + ".flyway_schema_history SET checksum=0 WHERE version='4'");
        }

        FluentConfiguration selected = configuration(schema).locations(FlywayHistoryConfiguration.CANONICAL);
        new FlywayHistoryConfiguration().customize(selected);

        assertThatThrownBy(() -> selected.load().validate()).hasMessageContaining("checksum mismatch");
    }

    @Test
    void compatibilityIsNotEnabledInProduction() {
        new ApplicationContextRunner().withPropertyValues("spring.profiles.active=prod")
                .withUserConfiguration(FlywayHistoryConfiguration.class)
                .run(context -> assertThat(context).doesNotHaveBean(FlywayHistoryConfiguration.class));
    }

    private FluentConfiguration configuration(String schema) {
        return Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(schema).defaultSchema(schema);
    }

    private List<String> fingerprints(Flyway flyway) {
        return Arrays.stream(flyway.info().applied()).map(info ->
                info.getVersion() + ":" + info.getScript() + ":" + info.getChecksum()).toList();
    }

    private long mediaCount(Flyway flyway) throws Exception {
        try (var connection = flyway.getConfiguration().getDataSource().getConnection();
             var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT count(*) FROM " + flyway.getConfiguration().getDefaultSchema() + ".media")) {
            rows.next();
            return rows.getLong(1);
        }
    }

    private String schema() {
        return "history_" + UUID.randomUUID().toString().replace("-", "");
    }
}
