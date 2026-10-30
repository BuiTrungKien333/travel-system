package com.travel.itinerary.dto.response;

import com.travel.planning.dto.response.MultiDayOptimizeAndAuditResponse;

import java.time.Instant;
import java.util.UUID;

/** Carries the itinerary version used to create the proposal for stale-write checks. */
public record ItineraryOptimizationResponse(
        UUID tripId,
        Instant sourceUpdatedAt,
        MultiDayOptimizeAndAuditResponse proposal
) {
}
