package com.travel.itinerary.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record ItineraryStopRequest(
        @NotNull UUID placeId,
        @NotNull @Min(0) Integer sequence,
        LocalDateTime arrivalTime,
        @NotNull @Min(1) @Max(1440) Integer durationMinutes,
        @Size(max = 2000) String note
) {
}
