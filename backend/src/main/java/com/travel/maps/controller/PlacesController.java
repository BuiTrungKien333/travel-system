package com.travel.maps.controller;

import com.travel.common.response.ApiResponse;
import com.travel.maps.dto.PlaceSearchResult;
import com.travel.maps.dto.RouteRequest;
import com.travel.maps.dto.RouteResponse;
import com.travel.maps.service.GeoapifyMapsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class PlacesController {
    private final GeoapifyMapsService mapsService;

    @GetMapping("/places/search")
    public ResponseEntity<ApiResponse<List<PlaceSearchResult>>> search(
            @RequestParam @Size(min = 2, max = 200) String query,
            @RequestParam(required = false) @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
            @RequestParam(required = false) @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
            @RequestParam(required = false) @Min(1000) @Max(50000) Integer radiusMeters) {
        if ((latitude == null) != (longitude == null))
            throw new IllegalArgumentException("latitude and longitude must be provided together");
        return ResponseEntity.ok(ApiResponse.success(mapsService.search(query, latitude, longitude, radiusMeters)));
    }

    @PostMapping("/routes")
    public ResponseEntity<ApiResponse<RouteResponse>> route(@Valid @RequestBody RouteRequest request) {
        return ResponseEntity.ok(ApiResponse.success(mapsService.route(request)));
    }
}
