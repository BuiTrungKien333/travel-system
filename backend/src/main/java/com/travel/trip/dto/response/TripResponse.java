package com.travel.trip.dto.response;

import com.travel.trip.enums.TripStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TripResponse(
        UUID id, UUID userId, String name, String destination,
        LocalDate startDate, LocalDate endDate, String transportation,
        List<String> preferences, BigDecimal budget, String constraints,
        String description, String coverImage, TripStatus status,
        Instant createdAt, Instant updatedAt,
        long placeCount, long visitedCount, long memoryCount
) {}
