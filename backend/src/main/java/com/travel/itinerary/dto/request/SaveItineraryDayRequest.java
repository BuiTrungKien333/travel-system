package com.travel.itinerary.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import com.travel.itinerary.dto.DaySettings;

public record SaveItineraryDayRequest(
        @NotNull @Min(0) Long expectedTripVersion,
        Instant expectedUpdatedAt,
        @NotNull @Size(max = 100) List<@NotNull @Valid ItineraryStopRequest> stops,
        @Valid DaySettings settings
) {
    public SaveItineraryDayRequest(Long version, Instant updatedAt, List<ItineraryStopRequest> stops) {
        this(version, updatedAt, stops, null);
    }
}
