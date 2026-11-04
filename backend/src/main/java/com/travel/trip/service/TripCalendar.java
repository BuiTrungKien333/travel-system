package com.travel.trip.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/** Pure calendar logic; tests pass a fixed Clock, never the fixture date. */
public final class TripCalendar {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private TripCalendar() { }
    public static LocalDate today(Clock clock) { return LocalDate.now(clock.withZone(ZONE)); }
    public static int days(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) return 0;
        long count = ChronoUnit.DAYS.between(start, end) + 1;
        return count > 365 ? 0 : Math.toIntExact(count);
    }
    public static String mode(String status, LocalDate start, LocalDate end, LocalDate today) {
        if ("CANCELLED".equals(status)) return "CANCELLED";
        if ("COMPLETED".equals(status)) return "RECAP";
        if (days(start, end) == 0 || today.isBefore(start)) return "PLANNING";
        return today.isAfter(end) ? "RECAP" : "LIVE";
    }
}
