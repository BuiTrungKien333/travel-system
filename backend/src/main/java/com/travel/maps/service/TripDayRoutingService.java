package com.travel.maps.service;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.itinerary.dto.DaySettings;
import com.travel.maps.dto.RoutePoint;
import com.travel.maps.dto.TripDayRoute;
import com.travel.maps.repository.TripRouteRepository;
import com.travel.maps.service.MapboxDirectionsService.Mode;
import com.travel.trip.service.TripCalendar;
import com.travel.planning.algorithm.HeldKarpSolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class TripDayRoutingService {
    private final TripRouteRepository repository;
    private final MapboxDirectionsService directions;
    private final Clock clock;
    private final HeldKarpSolver solver;

    public TripDayRoute.Response route(UUID owner, UUID tripId, LocalDate date, Mode mode) {
        validateDay(tripId, owner, date);
        // Reads finish before provider I/O; no connection is held while waiting on Mapbox.
        var points = repository.orderedPoints(tripId, owner, date);
        var settings = repository.settings(tripId, owner, date).orElse(null);
        var locations = locations(points, settings, true);
        var route = points.isEmpty() ? TripDayRoute.unavailable(TripDayRoute.Status.TOO_FEW_STOPS)
                : directions.route(locations, selectedMode(settings, mode));
        boolean origin = settings != null && settings.origin() != null;
        boolean returns = origin && settings.returnToStart();
        TripDayRoute.Leg first = null, last = null;
        var body = route.route();
        if (body != null) {
            if (origin) first = body.legs().getFirst();
            if (returns) last = body.legs().getLast();
            body = new TripDayRoute.Route(body.distanceMeters(), body.durationSeconds(), body.encodedPolyline(),
                    body.legs().subList(origin ? 1 : 0, body.legs().size() - (returns ? 1 : 0)));
        }
        return new TripDayRoute.Response(route.status(), body,
                points.stream().map(TripRouteRepository.Waypoint::placeId).toList(), first, last);
    }

    public TripDayRoute.Proposal optimize(UUID owner, UUID tripId, LocalDate date, Mode mode) {
        var dates = validateDay(tripId, owner, date);
        var today = TripCalendar.today(clock);
        String modeOfTrip = TripCalendar.mode(dates.status(), dates.start(), dates.end(), today);
        if (date.isBefore(today) || "CANCELLED".equals(modeOfTrip) || "RECAP".equals(modeOfTrip))
            throw new AppException(ErrorCode.TRIP_READ_ONLY);
        var points = repository.orderedPoints(tripId, owner, date);
        var settings = repository.settings(tripId, owner, date).orElse(null);
        if (points.stream().anyMatch(TripRouteRepository.Waypoint::visited))
            return optimizeRemaining(points, settings, selectedMode(settings, mode));
        boolean origin = settings != null && settings.origin() != null;
        boolean roundtrip = origin && settings.returnToStart();
        var locations = locations(points, settings, roundtrip);
        var matrix = directions.matrix(locations, selectedMode(settings, mode));
        var original = java.util.stream.IntStream.range(0, locations.size()).boxed().toList();
        // Preserve the existing fixed first/last endpoints; include the return leg in both totals.
        var optimized = nonWorseningOrder(matrix, original,
                solver.solveFixedStart(matrix.durations(), locations.size() - 1));
        var before = sum(matrix, original);
        var after = sum(matrix, optimized);
        int limit = locations.size() - (roundtrip ? 1 : 0);
        return new TripDayRoute.Proposal(optimized.stream().filter(index -> index >= (origin ? 1 : 0) && index < limit)
                .map(index -> points.get(index - (origin ? 1 : 0)).placeId()).toList(),
                points.stream().map(TripRouteRepository.Waypoint::placeId).toList(),
                after.distanceMeters(), after.durationSeconds(), List.of(), before.distanceMeters(), before.durationSeconds());
    }

    private TripDayRoute.Proposal optimizeRemaining(List<TripRouteRepository.Waypoint> points, DaySettings settings, Mode mode) {
        if (points.size() > 100) throw new AppException(ErrorCode.INVALID_ITINERARY);
        var visited = points.stream().filter(TripRouteRepository.Waypoint::visited).toList();
        var pending = points.stream().filter(point -> !point.visited()).toList();
        if (pending.isEmpty()) throw new AppException(ErrorCode.INVALID_ITINERARY);
        var locations = new ArrayList<RoutePoint>();
        locations.add(visited.getLast().point());
        pending.forEach(point -> locations.add(point.point()));
        boolean returns = settings != null && settings.origin() != null && settings.returnToStart();
        if (returns) locations.add(new RoutePoint(null, settings.origin().latitude(), settings.origin().longitude()));
        if (locations.size() > 12) throw new AppException(ErrorCode.INVALID_ITINERARY);
        var matrix = directions.matrix(locations, mode);
        Integer terminal = returns ? locations.size() - 1 : null;
        var candidate = solver.solveFixedStart(matrix.durations(), terminal);
        var original = java.util.stream.IntStream.range(0, locations.size()).boxed().toList();
        var optimized = nonWorseningOrder(matrix, original, candidate);
        var order = new ArrayList<UUID>(visited.stream().map(TripRouteRepository.Waypoint::placeId).toList());
        optimized.stream().filter(index -> index > 0 && (!returns || index != terminal))
                .forEach(index -> order.add(pending.get(index - 1).placeId()));
        var before = sum(matrix, original);
        var after = sum(matrix, optimized);
        return new TripDayRoute.Proposal(order, points.stream().map(TripRouteRepository.Waypoint::placeId).toList(),
                after.distanceMeters(), after.durationSeconds(),
                visited.stream().map(TripRouteRepository.Waypoint::placeId).toList(), before.distanceMeters(), before.durationSeconds());
    }

    private List<Integer> nonWorseningOrder(MapboxDirectionsService.RoadMatrix matrix,
            List<Integer> original, List<Integer> candidate) {
        var before = sum(matrix, original);
        var after = sum(matrix, candidate);
        // A faster route can be longer. Do not present either regression as an improvement.
        return after.distanceMeters() <= before.distanceMeters() && after.durationSeconds() <= before.durationSeconds()
                ? candidate : original;
    }

    private TripDayRoute.Leg sum(MapboxDirectionsService.RoadMatrix matrix, List<Integer> order) {
        double distance = 0, duration = 0;
        for (int i = 1; i < order.size(); i++) {
            distance += matrix.distances()[order.get(i - 1)][order.get(i)];
            duration += matrix.durations()[order.get(i - 1)][order.get(i)];
        }
        if (!Double.isFinite(distance) || !Double.isFinite(duration))
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND);
        return new TripDayRoute.Leg(distance, duration);
    }

    private List<RoutePoint> locations(List<TripRouteRepository.Waypoint> stops, DaySettings settings, boolean includeReturn) {
        List<RoutePoint> result = new ArrayList<>();
        if (settings != null && settings.origin() != null) {
            var origin = settings.origin();
            result.add(new RoutePoint(origin.name(), origin.latitude(), origin.longitude()));
        }
        stops.forEach(stop -> result.add(stop.point()));
        if (includeReturn && settings != null && settings.returnToStart() && !stops.isEmpty()) result.add(result.getFirst());
        return result;
    }

    private Mode selectedMode(DaySettings settings, Mode requested) {
        return requested != null ? requested : settings == null ? Mode.DRIVE : Mode.valueOf(settings.travelMode().name());
    }

    private TripRouteRepository.Dates validateDay(UUID tripId, UUID owner, LocalDate date) {
        var dates = repository.ownedDates(tripId, owner).orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
        if (dates.start() == null || dates.end() == null || date.isBefore(dates.start()) || date.isAfter(dates.end()))
            throw new AppException(ErrorCode.INVALID_ITINERARY);
        return dates;
    }
}
