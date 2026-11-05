package com.travel.planning.timeline;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.place.entity.Place;
import com.travel.planning.dto.request.TspLocationRequest;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class PinnedTimelineTest {
    private final TimelineEngine engine = new TimelineEngine();
    private final LocalDate day = LocalDate.of(2026, 11, 20);
    private Place place(String name) {
        return Place.builder().id(UUID.randomUUID()).name(name).latitude(11.94).longitude(108.44).build();
    }
    @Test void arrivingEarlyKeepsTheAppointmentAndStartsTheNextVisitAfterIt() {
        var first = place("Pinned"); var next = place("Next");
        var pinned = new TspLocationRequest(first.getId(), first.getName(), 11.94, 108.44,
                null, null, null, 30, null, null, 0, day.atTime(10, 0));
        var result = engine.buildTimelineForDay(day, LocalTime.of(8, 0), null, List.of(first, next), Map.of(first.getId(), pinned));
        assertThat(result.get(0).arrivalTime()).isEqualTo(day.atTime(10, 0));
        assertThat(result.get(1).arrivalTime()).isEqualTo(day.atTime(10, 30));
    }
    @Test void anUnreachableAppointmentIsReportedInsteadOfMovingThePinOrTravellingBackwardsInTime() {
        var first = place("Pinned");
        var pinned = new TspLocationRequest(first.getId(), first.getName(), 11.94, 108.44,
                null, null, null, 30, null, null, 0, day.atTime(7, 0));
        assertThatThrownBy(() -> engine.buildTimelineForDay(day, LocalTime.of(8, 0), null, List.of(first), Map.of(first.getId(), pinned)))
                .isInstanceOfSatisfying(AppException.class, error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.ITINERARY_PIN_CONFLICT));
    }
}
