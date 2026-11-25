package com.travel.itinerary.dto.request;

import com.travel.planning.dto.response.MultiDayOptimizeAndAuditResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record ApplyItineraryOptimizationRequest(
    @NotNull Instant expectedUpdatedAt,
    @NotNull @Valid MultiDayOptimizeAndAuditResponse proposal
) {}
