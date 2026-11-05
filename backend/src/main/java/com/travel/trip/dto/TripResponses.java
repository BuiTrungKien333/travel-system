package com.travel.trip.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class TripResponses {
    private TripResponses() { }
    public record Context(Instant serverTime, LocalDate evaluatedDate) { }
    public record Readiness(boolean hasDates, boolean hasPlaces, long unscheduledCount) { }
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Day(UUID id, LocalDate date, int dayNumber, String startLocation,
                      String startTime, boolean returnToStart, String transport) { }
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Destination(String name, UUID provinceId) { }
    public record Budget(long amount, String currency) { }
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Summary(UUID id, String title, Destination destination,
            LocalDate startDate, LocalDate endDate, String timeZone, String status, String mode,
            Readiness readiness, int dayCount, long stopCount, long memoryCount, String coverImageUrl,
            long version, Instant createdAt, Instant updatedAt) { }
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Detail(UUID id, String title, Destination destination,
            LocalDate startDate, LocalDate endDate, String timeZone, String status, String mode,
            Readiness readiness, int dayCount, long stopCount, long memoryCount, String coverImageUrl,
            long version, Instant createdAt, Instant updatedAt, Budget budget,
            List<String> preferences, String accommodation, String notes, List<Day> days) { }
    public record Read(Detail trip, Context context) { }
    public record Page<T>(List<T> content, int page, int size, long totalElements, long totalPages, Context context) { }
    public record Created(Read result, boolean created) { }
    public record DateImpact(long baseVersion, LocalDate startDate, LocalDate endDate, int dayCount,
                             List<LocalDate> removedDates, List<LocalDate> addedDates,
                             long movedToPoolCount, boolean requiresAcknowledgement) { }
}
