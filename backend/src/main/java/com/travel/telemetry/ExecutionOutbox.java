package com.travel.telemetry;

import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class ExecutionOutbox {

    private final JdbcTemplate jdbc;

    public record Event(UUID id, String payload, UUID lease, int attempts) {}

    public record State(long pending, Instant oldestPending, long retried) {}

    @Transactional
    public List<Event> claim() {
        UUID lease = UUID.randomUUID();
        return jdbc.query(
            """
            WITH ready AS (SELECT id FROM ai_execution_outbox WHERE delivered_at IS NULL
                AND next_attempt_at<=now() AND (lease_until IS NULL OR lease_until<now())
                ORDER BY next_attempt_at,id LIMIT 5 FOR UPDATE SKIP LOCKED)
            UPDATE ai_execution_outbox e SET lease_token=?,lease_until=now()+interval '60 seconds'
            FROM ready WHERE e.id=ready.id RETURNING e.id,e.payload::text,e.attempts
            """,
            (rs, n) -> new Event(rs.getObject(1, UUID.class), rs.getString(2), lease, rs.getInt(3)),
            lease
        );
    }

    public void delivered(Event event) {
        jdbc.update(
            "UPDATE ai_execution_outbox SET delivered_at=now(),last_error_code=NULL,lease_token=NULL,lease_until=NULL WHERE id=? AND lease_token=? AND delivered_at IS NULL",
            event.id(),
            event.lease()
        );
    }

    public void retry(Event event) {
        long delay = Math.min(3600, 15L << Math.min(event.attempts(), 8));
        jdbc.update(
            "UPDATE ai_execution_outbox SET attempts=least(attempts+1,1000000),next_attempt_at=now()+make_interval(secs=>?),last_error_code='MONGO_UNAVAILABLE',lease_token=NULL,lease_until=NULL WHERE id=? AND lease_token=? AND delivered_at IS NULL",
            delay,
            event.id(),
            event.lease()
        );
    }

    public State state() {
        return jdbc.queryForObject(
            "SELECT count(*),min(created_at),count(*) FILTER(WHERE attempts>0) FROM ai_execution_outbox WHERE delivered_at IS NULL",
            (rs, n) ->
                new State(
                    rs.getLong(1),
                    rs.getTimestamp(2) == null ? null : rs.getTimestamp(2).toInstant(),
                    rs.getLong(3)
                )
        );
    }

    @Transactional
    public void maintain() {
        // Ten minutes is above the current request deadline; never resurrect interrupted drafts.
        jdbc.update(
            "UPDATE trip_planning_drafts SET failure_code='INTERRUPTED' WHERE response_json IS NULL AND failure_code IS NULL AND created_at<now()-interval '10 minutes'"
        );
        jdbc.update("DELETE FROM ai_execution_outbox WHERE delivered_at<now()-interval '7 days'");
    }
}
