package com.travel.media.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class MediaCleanupScheduler {
    private final MediaDeletionService deletion;

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void clean() {
        try { deletion.retryPending(); }
        catch (RuntimeException failure) {
            log.warn("Media cleanup temporarily unavailable ({})", failure.getClass().getSimpleName());
        }
    }
}
