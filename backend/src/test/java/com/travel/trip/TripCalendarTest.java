package com.travel.trip;

import com.travel.trip.service.TripCalendar;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.assertj.core.api.Assertions.assertThat;

class TripCalendarTest {
    @Test void vietnamMidnightAndInclusiveBoundaries() {
        Clock clock=Clock.fixed(Instant.parse("2026-11-01T17:00:00Z"),ZoneOffset.UTC);
        LocalDate today=TripCalendar.today(clock);
        assertThat(today).isEqualTo(LocalDate.of(2026,11,2));
        assertThat(TripCalendar.mode("PLANNING",today,today,today)).isEqualTo("LIVE");
        assertThat(TripCalendar.mode("ACTIVE",today,today,today.plusDays(1))).isEqualTo("RECAP");
        assertThat(TripCalendar.mode("COMPLETED",today.plusDays(1),today.plusDays(2),today)).isEqualTo("RECAP");
        assertThat(TripCalendar.mode("CANCELLED",today,today,today)).isEqualTo("CANCELLED");
        assertThat(TripCalendar.mode("PLANNING",null,null,today)).isEqualTo("PLANNING");
        assertThat(TripCalendar.days(today,today.plusDays(364))).isEqualTo(365);
        assertThat(TripCalendar.days(today,today.plusDays(365))).isZero();
        assertThat(TripCalendar.mode("PLANNING",today,today.plusDays(365),today)).isEqualTo("PLANNING");
    }
}
