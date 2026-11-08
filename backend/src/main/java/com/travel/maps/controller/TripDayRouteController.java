package com.travel.maps.controller;

import com.travel.common.response.ApiResponse;
import com.travel.maps.dto.TripDayRoute;
import com.travel.maps.service.MapboxDirectionsService.Mode;
import com.travel.maps.service.TripDayRoutingService;
import com.travel.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/trips/{tripId}/itinerary/days/{date}/route")
public class TripDayRouteController {
    private final TripDayRoutingService service;

    @GetMapping
    public ApiResponse<TripDayRoute.Response> route(@PathVariable UUID tripId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Mode mode) {
        return ApiResponse.success(service.route(SecurityUtils.getCurrentUserId(), tripId, date, mode));
    }

    @PostMapping("/optimize")
    public ApiResponse<TripDayRoute.Proposal> optimize(@PathVariable UUID tripId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Mode mode) {
        return ApiResponse.success(service.optimize(SecurityUtils.getCurrentUserId(), tripId, date, mode));
    }
}
