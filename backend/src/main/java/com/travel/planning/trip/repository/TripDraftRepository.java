package com.travel.planning.trip.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TripDraftRepository {

    private final JdbcTemplate jdbc;

    public TripDraftRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Row(
        UUID id,
        UUID tripId,
        UUID userId,
        String requestJson,
        long tripVersion,
        String revision,
        String responseJson,
        Instant expiresAt,
        Instant appliedAt,
        Instant createdAt,
        String failureCode
    ) {}

    public Optional<Row> find(UUID tripId, UUID userId, UUID id, boolean byRequest) {
        return jdbc
            .query(
                "SELECT * FROM trip_planning_drafts WHERE trip_id=? AND user_id=? AND " +
                    (byRequest ? "client_request_id" : "id") +
                    "=?",
                (rs, n) ->
                    new Row(
                        rs.getObject("id", UUID.class),
                        tripId,
                        userId,
                        rs.getString("request_json"),
                        rs.getLong("trip_version"),
                        rs.getString("source_revision"),
                        rs.getString("response_json"),
                        rs.getTimestamp("expires_at").toInstant(),
                        rs.getTimestamp("applied_at") == null ? null : rs.getTimestamp("applied_at").toInstant(),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getString("failure_code")
                    ),
                tripId,
                userId,
                id
            )
            .stream()
            .findFirst();
    }

    public void insert(
        UUID id,
        UUID tripId,
        UUID userId,
        UUID requestId,
        String request,
        long version,
        String revision,
        Instant now
    ) {
        jdbc.update(
            """
            INSERT INTO trip_planning_drafts(id,trip_id,user_id,client_request_id,request_json,
                trip_version,source_revision,created_at,expires_at) VALUES (?,?,?,?,?,?,?,?,?)
            """,
            id,
            tripId,
            userId,
            requestId,
            request,
            version,
            revision,
            Timestamp.from(now),
            Timestamp.from(now.plusSeconds(86400))
        );
    }

    public String revision(UUID tripId) {
        return jdbc.queryForObject(
            """
            SELECT COALESCE((SELECT string_agg(id::text || ':' || updated_at::text, ',' ORDER BY id)
                FROM itineraries WHERE trip_id=?), '') || '|' ||
                COALESCE((SELECT string_agg(id::text || ':' || updated_at::text || ':' || visited::text,
                ',' ORDER BY id) FROM trip_places WHERE trip_id=?), '')
            """,
            String.class,
            tripId,
            tripId
        );
    }

    public int recentRequests(UUID userId, Instant since) {
        return jdbc.queryForObject(
            "SELECT count(*) FROM trip_planning_drafts WHERE user_id=? AND created_at>=?",
            Integer.class,
            userId,
            Timestamp.from(since)
        );
    }

    public void complete(UUID id, String response) {
        jdbc.update(
            "UPDATE trip_planning_drafts SET response_json=? WHERE id=? AND failure_code IS NULL",
            response,
            id
        );
    }

    public void failed(UUID id, String code) {
        jdbc.update("UPDATE trip_planning_drafts SET failure_code=? WHERE id=? AND response_json IS NULL", code, id);
    }

    public Optional<UUID> latest(UUID tripId, UUID userId, Instant now) {
        return jdbc
            .query(
                """
                SELECT id FROM trip_planning_drafts WHERE trip_id=? AND user_id=?
                    AND expires_at>? AND applied_at IS NULL
                ORDER BY created_at DESC, id DESC LIMIT 1
                """,
                (rs, n) -> rs.getObject("id", UUID.class),
                tripId,
                userId,
                Timestamp.from(now)
            )
            .stream()
            .findFirst();
    }

    public void lockOwner(UUID userId) {
        jdbc.queryForObject("SELECT id FROM users WHERE id=? FOR UPDATE", UUID.class, userId);
    }

    public void applied(UUID id, Instant now) {
        jdbc.update("UPDATE trip_planning_drafts SET applied_at=? WHERE id=?", Timestamp.from(now), id);
    }

    public int deleteExpired(Instant expiredBefore, int batchSize) {
        return jdbc.update(
            """
            DELETE FROM trip_planning_drafts
            WHERE id IN (
                SELECT id FROM trip_planning_drafts
                WHERE applied_at IS NULL AND expires_at < ?
                ORDER BY expires_at ASC
                LIMIT ?
            )
            """,
            Timestamp.from(expiredBefore),
            batchSize
        );
    }
}
