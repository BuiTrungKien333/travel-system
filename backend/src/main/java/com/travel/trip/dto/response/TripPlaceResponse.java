package com.travel.trip.dto.response;

import java.time.Instant;
import java.util.UUID;

public record TripPlaceResponse(
        UUID id, UUID tripId, UUID placeId, PlaceResponse place,
        Integer priority, Integer estimatedDuration, String notes,
        Integer plannedOrder, boolean visited, Instant createdAt, Instant updatedAt
) {
    public record PlaceResponse(
            UUID id, String providerPlaceId, String name, String address,
            Double latitude, Double longitude, UUID provinceId, String provinceName,
            String category, Instant createdAt
    ) {}
}
