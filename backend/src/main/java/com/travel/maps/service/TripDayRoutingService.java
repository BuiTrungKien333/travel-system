package com.travel.maps.service;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.itinerary.dto.DaySettings;
import com.travel.maps.dto.RoutePoint;
import com.travel.maps.dto.TripDayRoute;
import com.travel.maps.repository.TripRouteRepository;
import com.travel.maps.service.MapboxDirectionsService.Mode;
import com.travel.trip.service.TripCalendar;
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
        if (points.stream().anyMatch(TripRouteRepository.Waypoint::visited)) throw new AppException(ErrorCode.TRIP_READ_ONLY);
        var settings = repository.settings(tripId, owner, date).orElse(null);
        boolean origin = settings != null && settings.origin() != null;
        boolean roundtrip = origin && settings.returnToStart();
        var proposal = directions.optimize(locations(points, settings, false), selectedMode(settings, mode), roundtrip);
        return new TripDayRoute.Proposal(proposal.order().stream().filter(index -> !origin || index > 0)
                .map(index -> points.get(index - (origin ? 1 : 0)).placeId()).toList(),
                points.stream().map(TripRouteRepository.Waypoint::placeId).toList(),
                proposal.distanceMeters(), proposal.durationSeconds());
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
