package com.travel.memory.repository;

import com.travel.memory.dto.request.CreateMemoryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/** PostgreSQL resolves concurrent retries without aborting the surrounding transaction. */
@Repository
@RequiredArgsConstructor
public class MemoryCreateRepository {
    private final JdbcTemplate jdbc;

    public void insert(UUID id, UUID owner, CreateMemoryRequest request, String hash) {
        Instant now = Instant.now();
        jdbc.update("""
            INSERT INTO memories(id,user_id,trip_id,place_id,note,experienced_at,created_at,updated_at,
              client_request_id,create_request_hash,version)
            VALUES (?,?,?,?,?,?,?,?,?,?,0)
            ON CONFLICT (user_id,client_request_id) DO NOTHING
            """, id, owner, request.getTripId(), request.getPlaceId(), request.getNote(),
                Timestamp.from(request.getExperiencedAt() == null ? now : request.getExperiencedAt()),
                Timestamp.from(now), Timestamp.from(now), request.getClientRequestId(), hash);
    }
}
