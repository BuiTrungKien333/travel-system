package com.travel.planning.trip.service;

import com.travel.planning.trip.repository.TripDraftRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@Slf4j
public class TripDraftCleanupScheduler {

    static final int BATCH_SIZE = 500;

    private final TripDraftRepository drafts;
    private final Clock clock;
    private final int retentionDays;

    public TripDraftCleanupScheduler(
        TripDraftRepository drafts,
        Clock clock,
        @Value("${application.planning.draft-retention-days:7}") int retentionDays
    ) {
        if (retentionDays < 1 || retentionDays > 365) {
            throw new IllegalArgumentException("Draft retentionDays must be between 1 and 365");
        }
        this.drafts = drafts;
        this.clock = clock;
        this.retentionDays = retentionDays;
    }

    @Scheduled(
        fixedDelayString = "${application.planning.draft-cleanup-delay-ms:3600000}",
        initialDelayString = "${application.planning.draft-cleanup-initial-delay-ms:120000}"
    )
    public void cleanExpired() {
        try {
            Instant cutoff = clock.instant().minus(Duration.ofDays(retentionDays));
            int deleted = drafts.deleteExpired(cutoff, BATCH_SIZE);
            if (deleted > 0) {
                log.info("Cleaned up {} expired unapplied trip planning drafts older than {}", deleted, cutoff);
            }
        } catch (RuntimeException failure) {
            log.warn("Trip planning draft cleanup temporarily unavailable ({})", failure.getClass().getSimpleName());
        }
    }
}
