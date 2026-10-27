package com.travel.trip.controller;

import com.travel.common.response.ApiResponse;
import com.travel.security.util.SecurityUtils;
import com.travel.trip.dto.request.*;
import com.travel.trip.dto.response.*;
import com.travel.trip.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
@Tag(name = "Trips", description = "Private trips and their place lists")
public class TripController {
    private final TripService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a trip owned by the authenticated user")
    public ApiResponse<TripResponse> create(@Valid @RequestBody CreateTripRequest request) {
        return ApiResponse.success(service.create(SecurityUtils.getCurrentUserId(), request));
    }

    @GetMapping
    @Operation(summary = "List the current user's active trips, newest first")
    public ApiResponse<List<TripResponse>> list() {
        return ApiResponse.success(service.list(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/{tripId}")
    @Operation(summary = "Get a private trip and its counts")
    public ApiResponse<TripResponse> get(@PathVariable UUID tripId) {
        return ApiResponse.success(service.get(SecurityUtils.getCurrentUserId(), tripId));
    }

    @PutMapping("/{tripId}")
    @Operation(summary = "Update a trip", description = "Omitted/null fields are preserved; dates are validated together with stored dates")
    public ApiResponse<TripResponse> update(@PathVariable UUID tripId, @Valid @RequestBody UpdateTripRequest request) {
        return ApiResponse.success(service.update(SecurityUtils.getCurrentUserId(), tripId, request));
    }

    @DeleteMapping("/{tripId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete a trip", description = "Memories, media, places and itinerary data are preserved")
    public void delete(@PathVariable UUID tripId) {
        service.delete(SecurityUtils.getCurrentUserId(), tripId);
    }

    @PostMapping("/{tripId}/places")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add an existing or manually entered place to a trip")
    public ApiResponse<TripPlaceResponse> addPlace(@PathVariable UUID tripId, @Valid @RequestBody AddTripPlaceRequest request) {
        return ApiResponse.success(service.addPlace(SecurityUtils.getCurrentUserId(), tripId, request));
    }

    @GetMapping("/{tripId}/places")
    @Operation(summary = "List trip places in planned order")
    public ApiResponse<List<TripPlaceResponse>> listPlaces(@PathVariable UUID tripId) {
        return ApiResponse.success(service.listPlaces(SecurityUtils.getCurrentUserId(), tripId));
    }

    @PutMapping("/{tripId}/places/{placeId}")
    @Operation(summary = "Update trip-specific place metadata", description = "Does not modify the shared Place. Omitted/null fields are preserved")
    public ApiResponse<TripPlaceResponse> updatePlace(@PathVariable UUID tripId, @PathVariable UUID placeId,
                                                     @Valid @RequestBody UpdateTripPlaceRequest request) {
        return ApiResponse.success(service.updatePlace(SecurityUtils.getCurrentUserId(), tripId, placeId, request));
    }

    @DeleteMapping("/{tripId}/places/{placeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a place from a trip", description = "Deletes only the link; preserves the shared Place and all memories")
    public void removePlace(@PathVariable UUID tripId, @PathVariable UUID placeId) {
        service.removePlace(SecurityUtils.getCurrentUserId(), tripId, placeId);
    }
}
