package com.travel.itinerary.dto.request;

import com.travel.itinerary.dto.DaySettings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UpdateDayScheduleRequest(
    @NotNull @Min(0) Long expectedTripVersion,
    Instant expectedUpdatedAt,
    @NotNull @Valid DaySettings settings,
    @NotNull @Size(max = 100) List<@NotNull @Valid VisitDuration> durations
) {
    public record VisitDuration(@NotNull UUID placeId, @Min(5) @Max(480) int minutes) {}
}
