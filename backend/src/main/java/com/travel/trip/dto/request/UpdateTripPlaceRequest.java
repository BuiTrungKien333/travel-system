package com.travel.trip.dto.request;

import jakarta.validation.constraints.*;

public record UpdateTripPlaceRequest(
    @PositiveOrZero Integer priority,
    @Positive Integer estimatedDuration,
    @Size(max = 2000) String notes,
    @Min(0) @Max(1000000) Integer plannedOrder,
    Boolean visited
) {}
