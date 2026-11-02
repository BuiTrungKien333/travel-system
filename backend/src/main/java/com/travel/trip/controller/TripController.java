package com.travel.trip.controller;

import com.travel.common.response.ApiResponse;
import com.travel.trip.dto.CreateTripRequest;
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
}
