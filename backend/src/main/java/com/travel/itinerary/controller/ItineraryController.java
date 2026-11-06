package com.travel.itinerary.controller;

import com.travel.common.response.ApiResponse;
import com.travel.itinerary.dto.request.SaveItineraryRequest;
import com.travel.itinerary.dto.request.ApplyItineraryOptimizationRequest;
import com.travel.itinerary.dto.response.ItineraryResponse;
import com.travel.itinerary.dto.response.ItineraryOptimizationResponse;
import com.travel.itinerary.service.ItineraryService;
import com.travel.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/itinerary")
@RequiredArgsConstructor
@Tag(name = "Trip Itinerary", description = "Read, save, optimize and explicitly apply a trip itinerary")
public class ItineraryController {

    private final ItineraryService service;

    @GetMapping
    @Operation(summary = "Get the current multi-day itinerary", description = "Days are grouped by planned date; each stop has an explicit sequence, local arrival timestamp, duration and note.")
    public ApiResponse<ItineraryResponse> get(@PathVariable UUID tripId) {
        return ApiResponse.success(service.get(SecurityUtils.getCurrentUserId(), tripId));
    }

    @PutMapping
    @Operation(summary = "Replace the current itinerary", description = "The submitted days and stops replace the saved schedule atomically. Use an empty days list to clear it.")
    public ApiResponse<ItineraryResponse> save(@PathVariable UUID tripId,
                                               @Valid @RequestBody SaveItineraryRequest request) {
        return ApiResponse.success(service.save(SecurityUtils.getCurrentUserId(), tripId, request));
    }

    @PostMapping("/optimize")
    @Operation(summary = "Optimize the saved itinerary", description = "Runs the existing multi-day optimization pipeline and returns a proposal without changing the saved itinerary.")
    public ApiResponse<ItineraryOptimizationResponse> optimize(@PathVariable UUID tripId) {
        return ApiResponse.success(service.optimize(SecurityUtils.getCurrentUserId(), tripId));
    }

    @PostMapping("/apply-optimization")
    @Operation(summary = "Apply an optimization proposal", description = "Persists the submitted proposal only after validating that it contains exactly the saved places on their original planned dates.")
    public ApiResponse<ItineraryResponse> applyOptimization(
            @PathVariable UUID tripId,
            @Valid @RequestBody ApplyItineraryOptimizationRequest request) {
        return ApiResponse.success(service.applyOptimization(SecurityUtils.getCurrentUserId(), tripId, request));
    }
}
