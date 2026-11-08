package com.travel.trip.controller;

import com.travel.common.response.ApiResponse;
import com.travel.security.util.SecurityUtils;
import com.travel.trip.dto.CreateTripRequest;
import com.travel.trip.dto.TripDateChangeRequest;
import com.travel.trip.dto.TripResponses;
import com.travel.trip.dto.UpdateTripRequest;
import com.travel.trip.dto.request.AddTripPlaceRequest;
import com.travel.trip.dto.request.UpdateTripPlaceRequest;
import com.travel.trip.dto.response.TripPlaceResponse;
import com.travel.trip.service.TripCrudService;
import com.travel.trip.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
@Tag(name = "Trips", description = "Private trips and their place lists")
@SecurityRequirement(name = "bearerAuth")
public class TripController {

    private final TripCrudService tripCrudService;
    private final TripService tripPlacesService;

    @PostMapping
    @Operation(summary = "Create a trip; replaying the same clientRequestId does not duplicate it")
    public ResponseEntity<ApiResponse<TripResponses.Read>> create(
            @Valid @RequestBody CreateTripRequest request) {
        var result = tripCrudService.create(request);
        var response = ApiResponse.success(result.result());
        if (result.created()) {
            URI location = URI.create("/api/v1/trips/" + result.result().trip().id());
            return ResponseEntity.created(location).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List the current user's trips with pagination and filters")
    public ApiResponse<TripResponses.Page<TripResponses.Summary>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ApiResponse.success(tripCrudService.list(page, size, mode, q, sort));
    }

    @GetMapping("/{tripId}")
    @Operation(summary = "Get a private trip and its current context")
    public ApiResponse<TripResponses.Read> get(@PathVariable UUID tripId) {
        return ApiResponse.success(tripCrudService.get(tripId));
    }

    @PatchMapping("/{tripId}")
    @Operation(summary = "Update a trip using its current version")
    public ApiResponse<TripResponses.Read> update(
            @PathVariable UUID tripId,
            @Valid @RequestBody UpdateTripRequest request) {
        return ApiResponse.success(tripCrudService.update(tripId, request));
    }

    @PostMapping("/{tripId}/date-change-preview")
    @Operation(summary = "Preview the impact of changing trip dates")
    public ApiResponse<TripResponses.DateImpact> preview(
            @PathVariable UUID tripId,
            @Valid @RequestBody TripDateChangeRequest request) {
        return ApiResponse.success(tripCrudService.previewDates(tripId, request));
    }

    @DeleteMapping("/{tripId}")
    @Operation(summary = "Delete a trip using its current version")
    public ResponseEntity<Void> delete(
            @PathVariable UUID tripId,
            @RequestParam long version) {
        tripCrudService.delete(tripId, version);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{tripId}/places")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add an existing or manually entered place to a trip")
    public ApiResponse<TripPlaceResponse> addPlace(
            @PathVariable UUID tripId,
            @Valid @RequestBody AddTripPlaceRequest request) {
        return ApiResponse.success(tripPlacesService.addPlace(
                SecurityUtils.getCurrentUserId(), tripId, request));
    }

    @GetMapping("/{tripId}/places")
    @Operation(summary = "List trip places in planned order")
    public ApiResponse<List<TripPlaceResponse>> listPlaces(@PathVariable UUID tripId) {
        return ApiResponse.success(tripPlacesService.listPlaces(
                SecurityUtils.getCurrentUserId(), tripId));
    }

    @PutMapping("/{tripId}/places/{placeId}")
    @Operation(summary = "Update trip-specific place metadata",
            description = "Does not modify the shared Place. Omitted/null fields are preserved")
    public ApiResponse<TripPlaceResponse> updatePlace(
            @PathVariable UUID tripId,
            @PathVariable UUID placeId,
            @Valid @RequestBody UpdateTripPlaceRequest request) {
        return ApiResponse.success(tripPlacesService.updatePlace(
                SecurityUtils.getCurrentUserId(), tripId, placeId, request));
    }

    @DeleteMapping("/{tripId}/places/{placeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a place from a trip",
            description = "Deletes only the link; preserves the shared Place and all memories")
    public void removePlace(@PathVariable UUID tripId, @PathVariable UUID placeId) {
        tripPlacesService.removePlace(SecurityUtils.getCurrentUserId(), tripId, placeId);
    }
}
