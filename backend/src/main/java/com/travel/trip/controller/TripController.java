package com.travel.trip.controller;

import com.travel.common.response.ApiResponse;
import com.travel.security.util.SecurityUtils;
import com.travel.trip.dto.request.AddTripPlaceRequest;
import com.travel.trip.dto.request.UpdateTripPlaceRequest;
import com.travel.trip.dto.response.TripPlaceResponse;
import com.travel.trip.service.TripService;
import org.springframework.http.HttpStatus;
import java.util.List;
import com.travel.trip.dto.CreateTripRequest;
import com.travel.trip.dto.UpdateTripRequest;
import com.travel.trip.dto.TripDateChangeRequest;
import com.travel.trip.dto.TripResponses;
import com.travel.trip.service.TripCrudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
@Tag(name="Trips")
@SecurityRequirement(name="bearerAuth")
public class TripController {
    private final TripCrudService service;
    private final TripService placeService;
    @PostMapping
    @Operation(summary="Create a trip; replaying the same clientRequestId does not duplicate it")
    public ResponseEntity<ApiResponse<TripResponses.Read>> create(@Valid @RequestBody CreateTripRequest request) {
        var result=service.create(request);
        var response=ApiResponse.success(result.result());
        return result.created() ? ResponseEntity.created(URI.create("/api/v1/trips/"+result.result().trip().id())).body(response)
            : ResponseEntity.ok(response);
    }
    @GetMapping
    public ApiResponse<TripResponses.Page<TripResponses.Summary>> list(
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size,
            @RequestParam(required=false) String mode, @RequestParam(required=false) String q,
            @RequestParam(defaultValue="createdAt,desc") String sort) {
        return ApiResponse.success(service.list(page,size,mode,q,sort));
    }
    @GetMapping("/{tripId}")
    public ApiResponse<TripResponses.Read> get(@PathVariable UUID tripId) { return ApiResponse.success(service.get(tripId)); }

    @PatchMapping("/{tripId}")
    public ApiResponse<TripResponses.Read> update(@PathVariable UUID tripId, @Valid @RequestBody UpdateTripRequest request) {
        return ApiResponse.success(service.update(tripId, request));
    }
    @PostMapping("/{tripId}/date-change-preview")
    public ApiResponse<TripResponses.DateImpact> preview(@PathVariable UUID tripId, @Valid @RequestBody TripDateChangeRequest request) {
        return ApiResponse.success(service.previewDates(tripId, request));
    }
    @DeleteMapping("/{tripId}")
    public ResponseEntity<Void> delete(@PathVariable UUID tripId, @RequestParam long version) {
        service.delete(tripId, version);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{tripId}/places")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add an existing or manually entered place to a trip")
    public ApiResponse<TripPlaceResponse> addPlace(@PathVariable UUID tripId, @Valid @RequestBody AddTripPlaceRequest request) {
        return ApiResponse.success(placeService.addPlace(SecurityUtils.getCurrentUserId(), tripId, request));
    }

    @GetMapping("/{tripId}/places")
    @Operation(summary = "List trip places in planned order")
    public ApiResponse<List<TripPlaceResponse>> listPlaces(@PathVariable UUID tripId) {
        return ApiResponse.success(placeService.listPlaces(SecurityUtils.getCurrentUserId(), tripId));
    }

    @PutMapping("/{tripId}/places/{placeId}")
    @Operation(summary = "Update trip-specific place metadata", description = "Does not modify the shared Place. Omitted/null fields are preserved")
    public ApiResponse<TripPlaceResponse> updatePlace(@PathVariable UUID tripId, @PathVariable UUID placeId,
                                                     @Valid @RequestBody UpdateTripPlaceRequest request) {
        return ApiResponse.success(placeService.updatePlace(SecurityUtils.getCurrentUserId(), tripId, placeId, request));
    }

    @DeleteMapping("/{tripId}/places/{placeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a place from a trip", description = "Deletes only the link; preserves the shared Place and all memories")
    public void removePlace(@PathVariable UUID tripId, @PathVariable UUID placeId) {
        placeService.removePlace(SecurityUtils.getCurrentUserId(), tripId, placeId);
    }
}
