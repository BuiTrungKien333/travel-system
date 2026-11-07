package com.travel.itinerary.service;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.itinerary.dto.request.UpdateDayScheduleRequest;
import com.travel.itinerary.dto.request.SaveItineraryDayRequest;
import com.travel.itinerary.dto.request.ItineraryStopRequest;
import com.travel.itinerary.dto.response.ItineraryResponse;
import com.travel.maps.dto.RoutePoint;
import com.travel.maps.dto.TripDayRoute;
import com.travel.maps.service.MapboxDirectionsService;
import com.travel.trip.repository.TripRepository;
import com.travel.trip.service.TripCalendar;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Provider I/O runs outside transactions; saveDay rechecks both revisions under the trip lock. */
@Service
@RequiredArgsConstructor
public class DayScheduleService {
    private final ItineraryService itineraries;
    private final TripRepository trips;
    private final MapboxDirectionsService directions;
    private final Clock clock;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ItineraryResponse update(UUID owner, UUID tripId, LocalDate date, UpdateDayScheduleRequest request) {
        var trip = trips.findByIdAndUserIdAndDeletedAtIsNull(tripId, owner)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
        var today = TripCalendar.today(clock);
        String mode = TripCalendar.mode(trip.getStatus().name(), trip.getStartDate(), trip.getEndDate(), today);
        if ("CANCELLED".equals(mode) || "RECAP".equals(mode) || ("LIVE".equals(mode) && date.isBefore(today)))
            throw new AppException(ErrorCode.TRIP_READ_ONLY);
        if (trip.getStartDate() == null || trip.getEndDate() == null || date.isBefore(trip.getStartDate()) || date.isAfter(trip.getEndDate()))
            throw new AppException(ErrorCode.INVALID_ITINERARY);
        var base = itineraries.get(owner, tripId);
        if (!Objects.equals(request.expectedTripVersion(), trip.getVersion()) || !Objects.equals(request.expectedUpdatedAt(), base.updatedAt()))
            throw new AppException(ErrorCode.ITINERARY_CHANGED);
        var stops = base.days().stream().filter(day -> date.equals(day.date())).findFirst()
                .map(ItineraryResponse.ItineraryDayResponse::stops).orElse(List.of());
        Map<UUID, Integer> durations = new HashMap<>();
        for (var duration : request.durations()) {
            if (durations.putIfAbsent(duration.placeId(), duration.minutes()) != null)
                throw new AppException(ErrorCode.INVALID_ITINERARY);
        }
        if (durations.size() != stops.size() || stops.stream().anyMatch(stop -> !durations.containsKey(stop.placeId())))
            throw new AppException(ErrorCode.INVALID_ITINERARY);
        var settings = request.settings();
        List<RoutePoint> points = new ArrayList<>();
        if (settings.origin() != null)
            points.add(new RoutePoint(null, settings.origin().latitude(), settings.origin().longitude()));
        stops.forEach(stop -> points.add(new RoutePoint(null, stop.latitude(), stop.longitude())));
        if (settings.returnToStart() && !stops.isEmpty()) points.add(points.getFirst());
        var route = stops.isEmpty() ? TripDayRoute.unavailable(TripDayRoute.Status.TOO_FEW_STOPS)
                : directions.route(points, settings.travelMode());
        var legs = route.status() == TripDayRoute.Status.READY ? route.route().legs() : List.<TripDayRoute.Leg>of();
        LocalDateTime arrival = date.atTime(settings.departureTime());
        List<ItineraryStopRequest> updated = new ArrayList<>();
        for (int index = 0; index < stops.size(); index++) {
            var stop = stops.get(index);
            int legIndex = settings.origin() == null ? index - 1 : index;
            if (legIndex >= 0)
                arrival = arrival == null || legs.isEmpty() ? null
                        : arrival.plusMinutes((long) Math.ceil(legs.get(legIndex).durationSeconds() / 60));
            if (arrival != null && !date.equals(arrival.toLocalDate()))
                throw new AppException(ErrorCode.INVALID_ITINERARY);
            int minutes = durations.get(stop.placeId());
            updated.add(new ItineraryStopRequest(stop.placeId(), index, arrival, minutes, stop.note()));
            if (arrival != null) arrival = arrival.plusMinutes(minutes);
        }
        return itineraries.saveDay(owner, tripId, date, new SaveItineraryDayRequest(request.expectedTripVersion(),
                request.expectedUpdatedAt(), updated, settings));
    }
}
