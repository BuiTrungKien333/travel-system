package com.travel.maps.controller;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.common.response.ApiResponse;
import com.travel.maps.dto.TripDayRoute;
import com.travel.maps.repository.TripRouteRepository;
import com.travel.maps.service.MapboxDirectionsService;
import com.travel.maps.service.MapboxDirectionsService.Mode;
import com.travel.security.util.SecurityUtils;
import com.travel.trip.service.TripCalendar;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/trips/{tripId}/itinerary/days/{date}/route")
public class TripDayRouteController {
    private final TripRouteRepository repository;
    private final MapboxDirectionsService directions;
    private final Clock clock;

    public record DayRouteResponse(TripDayRoute.Status status, TripDayRoute.Route route, List<UUID> placeIds) { }

    @GetMapping
    public ApiResponse<DayRouteResponse> route(@PathVariable UUID tripId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "DRIVE") Mode mode) {
        UUID owner = SecurityUtils.getCurrentUserId();
        validateDay(tripId, owner, date);
        // Repository calls finish before provider I/O, so no DB connection is held waiting on Mapbox.
        var points = repository.orderedPoints(tripId, owner, date);
        var route = directions.route(points.stream().map(TripRouteRepository.Waypoint::point).toList(), mode);
        return ApiResponse.success(new DayRouteResponse(route.status(), route.route(),
                points.stream().map(TripRouteRepository.Waypoint::placeId).toList()));
    }

    public record Proposal(List<UUID> order, List<UUID> originalOrder, double distanceMeters, double durationSeconds) { }

    @PostMapping("/optimize")
    public ApiResponse<Proposal> optimize(@PathVariable UUID tripId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "DRIVE") Mode mode) {
        UUID owner = SecurityUtils.getCurrentUserId();
        var dates = validateDay(tripId, owner, date);
        var today = TripCalendar.today(clock);
        String modeOfTrip = TripCalendar.mode(dates.status(), dates.start(), dates.end(), today);
        if (date.isBefore(today) || "CANCELLED".equals(modeOfTrip) || "RECAP".equals(modeOfTrip))
            throw new AppException(ErrorCode.TRIP_READ_ONLY);
        var points = repository.orderedPoints(tripId, owner, date);
        if (points.stream().anyMatch(TripRouteRepository.Waypoint::visited)) throw new AppException(ErrorCode.TRIP_READ_ONLY);
        var proposal = directions.optimize(points.stream().map(TripRouteRepository.Waypoint::point).toList(), mode);
        return ApiResponse.success(new Proposal(proposal.order().stream().map(index -> points.get(index).placeId()).toList(),
                points.stream().map(TripRouteRepository.Waypoint::placeId).toList(),
                proposal.distanceMeters(), proposal.durationSeconds()));
    }

    private TripRouteRepository.Dates validateDay(UUID tripId, UUID owner, LocalDate date) {
        var dates = repository.ownedDates(tripId, owner).orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
        if (dates.start() == null || dates.end() == null || date.isBefore(dates.start()) || date.isAfter(dates.end()))
            throw new AppException(ErrorCode.INVALID_ITINERARY);
        return dates;
    }
}
