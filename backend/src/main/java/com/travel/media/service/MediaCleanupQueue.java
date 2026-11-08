package com.travel.media.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaCleanupQueue {
    private final JdbcTemplate jdbc;
    private final CloudinaryStorage storage;

    // Committed before the network call. Successful attachment cancels this job in its DB transaction.
    public void track(String publicId) {
        jdbc.update("INSERT INTO media_cleanup_jobs(public_id,run_after) VALUES (?,now()+interval '1 hour')", publicId);
    }

    public void retain(String publicId) {
        jdbc.update("DELETE FROM media_cleanup_jobs WHERE public_id=?", publicId);
    }

    public void enqueue(String publicId) {
        if (publicId == null || publicId.isBlank()) return;
        jdbc.update("""
            INSERT INTO media_cleanup_jobs(public_id) VALUES (?) ON CONFLICT (public_id)
            DO UPDATE SET run_after=LEAST(media_cleanup_jobs.run_after,now())
            """, publicId);
    }

    public void drain() {
        // Includes memories soft-deleted through Trip CRUD's native SQL.
        jdbc.update("""
            INSERT INTO media_cleanup_jobs(public_id)
            SELECT d.public_id FROM media d JOIN memories m ON m.id=d.memory_id
            WHERE m.deleted_at IS NOT NULL AND d.public_id IS NOT NULL AND d.storage_deleted_at IS NULL
              AND NOT EXISTS (SELECT 1 FROM media_cleanup_jobs j WHERE j.public_id=d.public_id)
            LIMIT 1000
            ON CONFLICT (public_id) DO NOTHING
            """);
        for (int i = 0; i < 20; i++) {
            // Claim one job at a time: no DB lock is held during Cloudinary I/O; leases survive worker crashes.
            List<Job> jobs = jdbc.query("""
                UPDATE media_cleanup_jobs SET run_after=now()+interval '2 minutes', attempts=attempts+1
                WHERE public_id IN (SELECT public_id FROM media_cleanup_jobs WHERE run_after<=now()
                    ORDER BY run_after FOR UPDATE SKIP LOCKED LIMIT 1)
                RETURNING public_id,attempts
                """, (rs, n) -> new Job(rs.getString(1), rs.getInt(2)));
            if (jobs.isEmpty()) break;
            Job job = jobs.getFirst();
            try {
                // A lost commit acknowledgement must not turn compensation into deletion of a saved photo.
                boolean attached = Boolean.TRUE.equals(jdbc.queryForObject("""
                    SELECT EXISTS(SELECT 1 FROM media d JOIN memories m ON m.id=d.memory_id
                        WHERE d.public_id=? AND m.deleted_at IS NULL)
                    """, Boolean.class, job.publicId()));
                if (attached) { retain(job.publicId()); continue; }
                storage.destroy(job.publicId());
                jdbc.update("UPDATE media SET storage_deleted_at=now() WHERE public_id=?", job.publicId());
                retain(job.publicId());
            } catch (Exception e) {
                int delay = Math.min(3600, 30 * (1 << Math.min(job.attempts(), 7)));
                jdbc.update("UPDATE media_cleanup_jobs SET run_after=now()+(? * interval '1 second') WHERE public_id=?",
                        delay, job.publicId());
                log.warn("Storage cleanup will retry; attempt={}, cause={}", job.attempts(), e.getClass().getSimpleName());
            }
        }
    }

    private record Job(String publicId, int attempts) { }

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    @ConditionalOnProperty(name = "app.media.cleanup-enabled", havingValue = "true", matchIfMissing = true)
    static class Scheduling {
        private final MediaCleanupQueue queue;
        Scheduling(MediaCleanupQueue queue) { this.queue = queue; }
        @Scheduled(fixedDelayString = "${app.media.cleanup-interval:30000}")
        void cleanup() { queue.drain(); }
    }
}
