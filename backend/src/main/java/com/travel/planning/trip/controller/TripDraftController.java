package com.travel.planning.trip.controller;

import com.travel.common.response.ApiResponse;
import com.travel.itinerary.dto.response.ItineraryResponse;
import com.travel.planning.trip.dto.*;
import com.travel.planning.trip.service.TripDraftService;
import com.travel.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile({ "dev", "rag-demo", "planning-ai" })
@RequestMapping("/api/v1/trips/{tripId}/planning/drafts")
public class TripDraftController {

    private final TripDraftService service;

    public TripDraftController(TripDraftService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(
        summary = "Generate and retain an AI/fallback proposal for an empty planning trip",
        description = "Owner only; requires a supported province, 1–3 days, and an empty itinerary. No itinerary changes. Reuse clientRequestId for retries."
    )
    public ApiResponse<TripDraftResponse> create(
        @PathVariable UUID tripId,
        @Valid @RequestBody CreateTripDraftRequest request
    ) {
        return ApiResponse.success(service.create(SecurityUtils.getCurrentUserId(), tripId, request));
    }

    @GetMapping("/{draftId}")
    @Operation(summary = "Read an owned planning draft; proposals expire after 24 hours")
    public ApiResponse<TripDraftResponse> get(@PathVariable UUID tripId, @PathVariable UUID draftId) {
        return ApiResponse.success(service.get(SecurityUtils.getCurrentUserId(), tripId, draftId));
    }

    @GetMapping("/latest")
    @Operation(summary = "Read the latest available draft, or null when none exists")
    public ApiResponse<TripDraftResponse> latest(@PathVariable UUID tripId) {
        return ApiResponse.success(service.latest(SecurityUtils.getCurrentUserId(), tripId));
    }

    @PostMapping("/{draftId}/apply")
    @Operation(summary = "Atomically apply a stored proposal once, with ownership and revision checks")
    public ApiResponse<ItineraryResponse> apply(@PathVariable UUID tripId, @PathVariable UUID draftId) {
        return ApiResponse.success(service.apply(SecurityUtils.getCurrentUserId(), tripId, draftId));
    }
}
