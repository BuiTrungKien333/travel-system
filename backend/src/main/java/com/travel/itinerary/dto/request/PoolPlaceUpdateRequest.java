package com.travel.itinerary.dto.request;

import jakarta.validation.constraints.*;
import java.util.UUID;

/** Metadata saved atomically when a visit is returned to the unscheduled pool. */
public record PoolPlaceUpdateRequest(
        @NotNull UUID placeId,
        @NotNull @Min(5) @Max(480) Integer durationMinutes,
        @Size(max = 2000) String note
) {}
