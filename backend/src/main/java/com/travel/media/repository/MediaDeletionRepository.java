package com.travel.media.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MediaDeletionRepository {

    private final JdbcTemplate jdbc;

    public record Deletion(
        UUID mediaId,
        UUID memoryId,
        UUID userId,
        String publicId,
        String hash,
        Instant completedAt
    ) {}

    public Optional<Deletion> find(UUID id) {
        return jdbc
            .query(
                "SELECT * FROM media_deletions WHERE media_id=?",
                (rs, row) ->
                    new Deletion(
                        rs.getObject("media_id", UUID.class),
                        rs.getObject("memory_id", UUID.class),
                        rs.getObject("user_id", UUID.class),
                        rs.getString("public_id"),
                        rs.getString("upload_hash"),
                        rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toInstant()
                    ),
                id
            )
            .stream()
            .findFirst();
    }

    public Optional<Deletion> lockOwnedPhoto(UUID id, UUID owner) {
        return jdbc
            .query(
                """
                SELECT p.id,p.memory_id,m.user_id,p.public_id,p.upload_hash,p.url
                FROM media p JOIN memories m ON m.id=p.memory_id
                WHERE p.id=? AND m.user_id=? AND m.deleted_at IS NULL FOR UPDATE OF m
                """,
                (rs, row) -> {
                    String publicId = rs.getString("public_id");
                    // Never guess provider IDs. Old external links have no owned Cloudinary asset.
                    if (publicId == null && rs.getString("url").contains("res.cloudinary.com/")) publicId = "";
                    return new Deletion(
                        rs.getObject("id", UUID.class),
                        rs.getObject("memory_id", UUID.class),
                        rs.getObject("user_id", UUID.class),
                        publicId,
                        rs.getString("upload_hash"),
                        null
                    );
                },
                id,
                owner
            )
            .stream()
            .findFirst();
    }

    public void reserve(Deletion photo, Instant now) {
        jdbc.update(
            """
            INSERT INTO media_deletions(media_id,memory_id,user_id,public_id,upload_hash,requested_at,next_attempt_at)
            VALUES (?,?,?,?,?,?,?) ON CONFLICT (media_id) DO NOTHING
            """,
            photo.mediaId(),
            photo.memoryId(),
            photo.userId(),
            photo.publicId(),
            photo.hash(),
            Timestamp.from(now),
            Timestamp.from(now.plusSeconds(60))
        );
    }

    public boolean uploadWasRemoved(UUID memory, String hash) {
        return Boolean.TRUE.equals(
            jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM media_deletions WHERE memory_id=? AND upload_hash=?)",
                Boolean.class,
                memory,
                hash
            )
        );
    }

    public Optional<UUID> removedUpload(UUID memory, String hash) {
        return jdbc
            .query(
                "SELECT media_id FROM media_deletions WHERE memory_id=? AND upload_hash=?",
                (rs, row) -> rs.getObject(1, UUID.class),
                memory,
                hash
            )
            .stream()
            .findFirst();
    }

    /** Called inside a short transaction when an upload finishes after its parent was archived. */
    public Optional<UUID> reserveArchivedUpload(UUID owner, UUID memory, String hash, String publicId, Instant now) {
        var archived = jdbc.query(
            "SELECT id FROM memories WHERE id=? AND user_id=? AND deleted_at IS NOT NULL FOR UPDATE",
            (rs, row) -> rs.getObject(1, UUID.class),
            memory,
            owner
        );
        if (archived.isEmpty()) return Optional.empty();
        var existing = removedUpload(memory, hash);
        if (existing.isPresent()) return existing;
        UUID receipt = UUID.randomUUID();
        reserve(new Deletion(receipt, memory, owner, publicId, hash, null), now);
        return Optional.of(receipt);
    }

    public void finish(Deletion job, Instant now) {
        // Same aggregate lock as upload finalization; provider I/O has already completed.
        jdbc.query("SELECT id FROM memories WHERE id=? FOR UPDATE", (rs, row) -> rs.getObject(1), job.memoryId());
        int removed = jdbc.update("DELETE FROM media WHERE id=? AND memory_id=?", job.mediaId(), job.memoryId());
        if (removed > 0) {
            jdbc.update(
                """
                UPDATE memories SET version=version+1,updated_at=?,
                    deleted_at=CASE WHEN deleted_at IS NULL AND trim(coalesce(note,''))='' AND NOT EXISTS
                        (SELECT 1 FROM media WHERE memory_id=memories.id) THEN ? ELSE deleted_at END
                WHERE id=?
                """,
                Timestamp.from(now),
                Timestamp.from(now),
                job.memoryId()
            );
        }
        jdbc.update("UPDATE media_deletions SET completed_at=? WHERE media_id=?", Timestamp.from(now), job.mediaId());
    }

    public void retryLater(UUID id, Instant now) {
        jdbc.update(
            """
            UPDATE media_deletions SET completed_at=NULL,attempts=attempts+1,
                next_attempt_at=CAST(? AS timestamptz) + least(3600,60 * power(2,least(attempts,6))) * interval '1 second'
            WHERE media_id=?
            """,
            Timestamp.from(now),
            id
        );
    }

    public void reopen(UUID id, Instant now) {
        jdbc.update(
            "UPDATE media_deletions SET completed_at=NULL,next_attempt_at=? WHERE media_id=?",
            Timestamp.from(now),
            id
        );
    }

    public List<UUID> due(Instant now) {
        return jdbc.query(
            """
            SELECT media_id FROM media_deletions WHERE completed_at IS NULL AND next_attempt_at<=?
            ORDER BY next_attempt_at,media_id LIMIT 10
            """,
            (rs, row) -> rs.getObject(1, UUID.class),
            Timestamp.from(now)
        );
    }

    /** Cleanup after existing soft-delete APIs archive a memory or trip. */
    public void reserveArchived(Instant now) {
        jdbc.update(
            """
            INSERT INTO media_deletions(media_id,memory_id,user_id,public_id,upload_hash,requested_at,next_attempt_at)
            SELECT p.id,m.id,m.user_id,
                CASE WHEN p.public_id IS NULL AND p.url LIKE '%res.cloudinary.com/%' THEN '' ELSE p.public_id END,
                p.upload_hash,?,?
            FROM memories m JOIN media p ON p.memory_id=m.id
            WHERE m.deleted_at IS NOT NULL AND NOT EXISTS (SELECT 1 FROM media_deletions d WHERE d.media_id=p.id)
            ORDER BY m.id,p.id LIMIT 10 ON CONFLICT (media_id) DO NOTHING
            """,
            Timestamp.from(now),
            Timestamp.from(now)
        );
    }
}
