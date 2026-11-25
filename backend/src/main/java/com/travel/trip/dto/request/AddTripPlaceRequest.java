package com.travel.trip.dto.request;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record AddTripPlaceRequest(
    UUID placeId,
    @Size(max = 255) String providerPlaceId,
    @Size(max = 255) String name,
    @Size(max = 2000) String address,
    @DecimalMin("-90") @DecimalMax("90") Double latitude,
    @DecimalMin("-180") @DecimalMax("180") Double longitude,
    UUID provinceId,
    @Size(max = 100) String category,
    @PositiveOrZero Integer priority,
    @Positive Integer estimatedDuration,
    @Size(max = 2000) String notes,
    @Min(0) @Max(1000000) Integer plannedOrder
) {
    @AssertTrue(message = "Provide placeId or a name and valid latitude/longitude")
    public boolean isPlaceProvided() {
        return (
            placeId != null ||
            (name != null &&
                !name.isBlank() &&
                latitude != null &&
                Double.isFinite(latitude) &&
                longitude != null &&
                Double.isFinite(longitude))
        );
    }
}
