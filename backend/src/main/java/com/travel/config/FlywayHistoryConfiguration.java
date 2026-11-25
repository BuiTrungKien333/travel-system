package com.travel.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

/** Preserve the immutable migration history of existing merge-test development databases. */
@Configuration(proxyBeanMethods = false)
@Profile("dev")
@Slf4j
public class FlywayHistoryConfiguration implements FlywayConfigurationCustomizer {
    static final String CANONICAL = "classpath:db/migration";
    static final String MERGE_TEST = "classpath:db/migration-merge-test";
    static final String RAG = "classpath:db/rag";
    static final String LEXICAL_RAG = "classpath:db/rag-lexical";

    @Override
    public void customize(FluentConfiguration configuration) {
        if (Arrays.stream(configuration.getLocations()).noneMatch(location ->
                CANONICAL.equals(location.getDescriptor()))) return;

        try (Connection connection = configuration.getDataSource().getConnection()) {
            String schema = configuration.getDefaultSchema();
            if (schema == null && configuration.getSchemas().length > 0) schema = configuration.getSchemas()[0];
            if (schema == null) schema = connection.getSchema();
            String history = quote(schema) + "." + quote(configuration.getTable());
            if (!hasMergeTestHistory(connection, history)) return;

            boolean vectorAvailable;
            try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                    "SELECT EXISTS(SELECT 1 FROM pg_available_extensions WHERE name='vector')")) {
                rows.next();
                vectorAvailable = rows.getBoolean(1);
            }
            configuration.locations(Arrays.stream(configuration.getLocations()).map(location -> {
                String path = location.getDescriptor();
                if (CANONICAL.equals(path)) return MERGE_TEST;
                if (!vectorAvailable && RAG.equals(path)) return LEXICAL_RAG;
                return path;
            }).toArray(String[]::new));
            // Bridge V24.1-V24.7 can follow catalog V25-V29 already installed locally.
            configuration.outOfOrder(true);
            log.info("Using original merge-test Flyway migrations; existing checksums are preserved");
            if (!vectorAvailable) log.warn("pgvector is unavailable; using the existing lexical RAG schema");
        } catch (SQLException error) {
            throw new IllegalStateException("Could not identify the development database migration history", error);
        }
    }

    private boolean hasMergeTestHistory(Connection connection, String history) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT to_regclass(?)")) {
            statement.setString(1, history);
            try (var rows = statement.executeQuery()) {
                if (!rows.next() || rows.getString(1) == null) return false;
            }
        }
        try (var statement = connection.prepareStatement(
                "SELECT script, checksum FROM " + history + " WHERE version='4' AND success")) {
            try (var rows = statement.executeQuery()) {
                return rows.next() && "V4__add_memory_permissions_and_indexes.sql".equals(rows.getString(1))
                        && rows.getInt(2) == 2000036775;
            }
        }
    }

    private String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
