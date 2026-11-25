package com.travel.planning.trip.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.travel.planning.trip.repository.TripDraftRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TripDraftCleanupSchedulerTest {

    @Mock
    private TripDraftRepository drafts;

    @Test
    void cleanExpired_deletesDraftsPastRetentionCutoff() {
        Instant now = Instant.parse("2026-11-25T10:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        var scheduler = new TripDraftCleanupScheduler(drafts, clock, 7);
        when(drafts.deleteExpired(now.minus(Duration.ofDays(7)), TripDraftCleanupScheduler.BATCH_SIZE)).thenReturn(3);

        scheduler.cleanExpired();

        verify(drafts).deleteExpired(now.minus(Duration.ofDays(7)), TripDraftCleanupScheduler.BATCH_SIZE);
    }

    @Test
    void cleanExpired_swallowsRepositoryRuntimeFailure() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-25T10:00:00Z"), ZoneOffset.UTC);
        var scheduler = new TripDraftCleanupScheduler(drafts, clock, 7);
        when(drafts.deleteExpired(any(), anyInt())).thenThrow(new IllegalStateException("DB down"));

        assertThatCode(scheduler::cleanExpired).doesNotThrowAnyException();
    }

    @Test
    void constructor_rejectsInvalidRetentionDays() {
        Clock clock = Clock.systemUTC();
        assertThatThrownBy(() -> new TripDraftCleanupScheduler(drafts, clock, 0)).isInstanceOf(
            IllegalArgumentException.class
        );
        assertThatThrownBy(() -> new TripDraftCleanupScheduler(drafts, clock, 366)).isInstanceOf(
            IllegalArgumentException.class
        );
    }
}
