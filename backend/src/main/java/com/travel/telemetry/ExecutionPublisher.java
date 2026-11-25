package com.travel.telemetry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExecutionPublisher {
    private final ExecutionOutbox outbox;
    private final MongoExecutionStore store;
    @Value("${telemetry.publish-enabled:true}")
    private boolean publishEnabled;

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void initialize() {
        if (!store.enabled()) return;
        try {
            store.collection();
            log.info("AI telemetry MongoDB schema and indexes are ready.");
        } catch (RuntimeException unavailable) {
            log.warn("AI telemetry MongoDB initialization deferred; core remains available.");
        }
    }

    @Scheduled(fixedDelayString = "${telemetry.publish-delay-ms:5000}")
    public void tick() {
        if (!publishEnabled || !store.enabled()) return;
        try {
            outbox.maintain();
            for (var event : outbox.claim()) {
                try {
                    store.publish(event.payload());
                    outbox.delivered(event);
                } catch (RuntimeException unavailable) {
                    outbox.retry(event);
                }
            }
        } catch (RuntimeException failure) {
            log.warn("AI telemetry publisher deferred; PostgreSQL draft requests remain independent.");
        }
    }
}
