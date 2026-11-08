package com.travel.itinerary.service;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.itinerary.dto.DaySettings;
import com.travel.itinerary.dto.request.UpdateDayScheduleRequest;
import com.travel.itinerary.dto.request.SaveItineraryDayRequest;
import com.travel.itinerary.dto.request.ItineraryStopRequest;
import com.travel.itinerary.dto.response.ItineraryResponse;
import com.travel.itinerary.dto.response.ItineraryResponse.ItineraryStopResponse;
import com.travel.maps.dto.RoutePoint;
import com.travel.maps.dto.TripDayRoute;
import com.travel.maps.service.MapboxDirectionsService;
import com.travel.place.repository.PlaceRepository;
import com.travel.trip.entity.Trip;
import com.travel.trip.entity.TripPlace;
import com.travel.trip.repository.TripPlaceRepository;
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
import java.util.stream.Collectors;

/** Provider I/O runs outside transactions; writes recheck revisions under the trip lock. */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class DayScheduleService {
    private final ItineraryService itineraries;
    private final TripRepository trips;
    private final TripPlaceRepository tripPlaces;
    private final PlaceRepository places;
    private final MapboxDirectionsService directions;
    private final Clock clock;

    public ItineraryResponse update(UUID owner, UUID tripId, LocalDate date, UpdateDayScheduleRequest request) {
        Trip trip = editableTrip(owner, tripId, date);
        var base = itineraries.get(owner, tripId);
        checkRevision(trip, base, request.expectedTripVersion(), request.expectedUpdatedAt());
        var stops = dayStops(base, date);
        Map<UUID, Integer> durations = new HashMap<>();
        for (var duration : request.durations()) {
            if (durations.putIfAbsent(duration.placeId(), duration.minutes()) != null)
                throw new AppException(ErrorCode.INVALID_ITINERARY);
        }
        if (durations.size() != stops.size() || stops.stream().anyMatch(stop -> !durations.containsKey(stop.placeId())))
            throw new AppException(ErrorCode.INVALID_ITINERARY);
        var proposed = stops.stream().map(stop -> new ItineraryStopRequest(stop.placeId(), stop.sequence(),
                stop.arrivalTime(), durations.get(stop.placeId()), stop.note(), stop.pinned())).toList();
        return itineraries.saveDay(owner, tripId, date, new SaveItineraryDayRequest(request.expectedTripVersion(),
                request.expectedUpdatedAt(), calculate(trip, date, stops, proposed, request.settings(), false, false), request.settings()));
    }

    /** Normal day edits reuse saved departure settings, without routing note-only changes. */
    public ItineraryResponse saveDay(UUID owner, UUID tripId, LocalDate date, SaveItineraryDayRequest request) {
        Trip trip = editableTrip(owner, tripId, date);
        var base = itineraries.get(owner, tripId);
        checkRevision(trip, base, request.expectedTripVersion(), request.expectedUpdatedAt());
        var before = dayStops(base, date);
        var savedSettings = settings(base, date);
        var proposed = request.stops().stream().sorted(Comparator.comparingInt(ItineraryStopRequest::sequence)).toList();
        var settings = request.settings() != null ? request.settings() : savedSettings != null ? savedSettings
                : inferredSettings(date, proposed);
        if (settings == null && request.expectedVisitedPlaceIds() != null) {
            settings = inferredSettings(date, proposed.stream()
                    .filter(stop -> request.expectedVisitedPlaceIds().contains(stop.placeId()) && stop.arrivalTime() != null).toList());
        }
        boolean changed = request.expectedVisitedPlaceIds() != null || !Objects.equals(settings, savedSettings) || before.size() != proposed.size();
        if (!changed) {
            for (int i = 0; i < before.size(); i++) {
                var old = before.get(i);
                var next = proposed.get(i);
                if (!old.placeId().equals(next.placeId()) || old.durationMinutes() != next.durationMinutes()
                        || !Objects.equals(old.arrivalTime(), next.arrivalTime())) {
                    changed = true;
                    break;
                }
            }
        }
        if (settings == null || !changed) {
            return itineraries.saveDay(owner, tripId, date, request);
        }
        var calculated = calculate(trip, date, before, proposed, settings, true, request.expectedVisitedPlaceIds() != null);
        return itineraries.saveDay(owner, tripId, date, new SaveItineraryDayRequest(request.expectedTripVersion(),
                request.expectedUpdatedAt(), calculated, savedSettings == null ? settings : request.settings(), request.expectedVisitedPlaceIds()));
    }

    /** Prepare before the batch transaction; a timed first stop can supply the departure anchor. */
    public SaveItineraryDayRequest prepareAppend(UUID owner, UUID tripId, LocalDate date, List<UUID> placeIds) {
        Trip trip = editableTrip(owner, tripId, date);
        var base = itineraries.get(owner, tripId);
        var before = dayStops(base, date);
        var savedSettings = settings(base, date);
        var settings = savedSettings != null ? savedSettings : inferredSettings(date, before.stream()
                .map(stop -> new ItineraryStopRequest(stop.placeId(), stop.sequence(), stop.arrivalTime(),
                        stop.durationMinutes(), stop.note(), stop.pinned())).toList());
        if (settings == null) return null;
        Set<UUID> scheduled = before.stream().map(ItineraryStopResponse::placeId).collect(Collectors.toSet());
        var additions = placeIds.stream().filter(id -> !scheduled.contains(id)).toList();
        if (additions.isEmpty()) return null;
        if (before.size() + additions.size() > 100) throw new AppException(ErrorCode.INVALID_ITINERARY);
        var visible = places.findVisibleByIds(additions, owner).stream()
                .collect(Collectors.toMap(com.travel.place.entity.Place::getId, place -> place));
        if (visible.size() != additions.size()) throw new AppException(ErrorCode.PLACE_NOT_FOUND);
        var links = tripPlaces.findByTripIdAndPlaceIdIn(tripId, additions).stream()
                .collect(Collectors.toMap(link -> link.getPlace().getId(), link -> link));
        List<ItineraryStopResponse> coordinates = new ArrayList<>(before);
        List<ItineraryStopRequest> proposed = new ArrayList<>();
        before.forEach(stop -> proposed.add(new ItineraryStopRequest(stop.placeId(), stop.sequence(), stop.arrivalTime(),
                stop.durationMinutes(), stop.note(), stop.pinned())));
        for (UUID id : additions) {
            var place = visible.get(id);
            var link = links.get(id);
            int duration = link == null || link.getEstimatedDuration() == null ? 45 : link.getEstimatedDuration();
            String note = link == null ? null : link.getNotes();
            int sequence = proposed.size();
            proposed.add(new ItineraryStopRequest(id, sequence, null, duration, note));
            coordinates.add(new ItineraryStopResponse(null, id, place.getName(), place.getAddress(), place.getLatitude(),
                    place.getLongitude(), place.getCategory(), null, duration, sequence, note, null, null));
        }
        return new SaveItineraryDayRequest(trip.getVersion(), base.updatedAt(),
                calculate(trip, date, coordinates, proposed, settings, false, false), savedSettings == null ? settings : null);
    }

    private List<ItineraryStopRequest> calculate(Trip trip, LocalDate date, List<ItineraryStopResponse> before,
            List<ItineraryStopRequest> proposed, DaySettings settings, boolean manualEdits, boolean regroupVisited) {
        Map<UUID, ItineraryStopResponse> previous = before.stream()
                .collect(Collectors.toMap(ItineraryStopResponse::placeId, stop -> stop));
        var links = proposed.isEmpty() ? List.<TripPlace>of() : tripPlaces.findByTripIdAndPlaceIdIn(trip.getId(),
                proposed.stream().map(ItineraryStopRequest::placeId).toList());
        var membership = links.stream().collect(Collectors.toMap(link -> link.getPlace().getId(), link -> link));
        boolean liveToday = date.equals(TripCalendar.today(clock)) && "LIVE".equals(TripCalendar.mode(
                trip.getStatus().name(), trip.getStartDate(), trip.getEndDate(), TripCalendar.today(clock)));
        var frozen = (liveToday || regroupVisited) ? before.stream()
                .filter(stop -> stop.id() != null && membership.containsKey(stop.placeId()) && membership.get(stop.placeId()).isVisited())
                .map(ItineraryStopResponse::placeId).collect(Collectors.toSet()) : Set.<UUID>of();
        int prefix = 0;
        while (prefix < proposed.size() && frozen.contains(proposed.get(prefix).placeId())) prefix++;
        int remainingStart = prefix > 0 && prefix == frozen.size() ? prefix - 1 : -1;
        // New batch members are visible but are not linked until the atomic append transaction.
        var points = new ArrayList<RoutePoint>();
        if (remainingStart < 0 && settings.origin() != null)
            points.add(new RoutePoint(null, settings.origin().latitude(), settings.origin().longitude()));
        for (int i = Math.max(0, remainingStart); i < proposed.size(); i++) {
            var stop = proposed.get(i);
            var old = previous.get(stop.placeId());
            var link = membership.get(stop.placeId());
            if (old == null && link == null) throw new AppException(ErrorCode.ITINERARY_PLACE_NOT_IN_TRIP);
            points.add(old != null ? new RoutePoint(null, old.latitude(), old.longitude())
                    : new RoutePoint(null, link.getPlace().getLatitude(), link.getPlace().getLongitude()));
        }
        if (settings.returnToStart() && settings.origin() != null && !proposed.isEmpty())
            points.add(new RoutePoint(null, settings.origin().latitude(), settings.origin().longitude()));
        var route = proposed.isEmpty() ? TripDayRoute.unavailable(TripDayRoute.Status.TOO_FEW_STOPS)
                : directions.route(points, MapboxDirectionsService.Mode.valueOf(settings.travelMode().name()));
        var legs = route.status() == TripDayRoute.Status.READY ? route.route().legs() : List.<TripDayRoute.Leg>of();
        boolean sameOrder = before.size() == proposed.size();
        for (int i = 0; sameOrder && i < before.size(); i++)
            sameOrder = before.get(i).placeId().equals(proposed.get(i).placeId());
        LocalDateTime arrival = date.atTime(settings.departureTime());
        List<ItineraryStopRequest> updated = new ArrayList<>();
        for (int index = 0; index < proposed.size(); index++) {
            var stop = proposed.get(index);
            var old = previous.get(stop.placeId());
            boolean visited = frozen.contains(stop.placeId());
            int legIndex = remainingStart >= 0 ? index - remainingStart - 1 : settings.origin() == null ? index - 1 : index;
            if (visited) {
                if ((!regroupVisited && old.sequence() != stop.sequence()) || old.durationMinutes() != stop.durationMinutes())
                    throw new AppException(ErrorCode.TRIP_READ_ONLY);
                if (manualEdits && !Objects.equals(old.arrivalTime(), stop.arrivalTime()))
                    throw new AppException(ErrorCode.TRIP_READ_ONLY);
                arrival = old.arrivalTime();
            } else {
                if (legIndex >= 0) arrival = arrival == null || legIndex >= legs.size() ? null
                        : arrival.plusMinutes((long) Math.ceil(legs.get(legIndex).durationSeconds() / 60));
                if (manualEdits && sameOrder && old != null && !Objects.equals(old.arrivalTime(), stop.arrivalTime()))
                    arrival = stop.arrivalTime();
            }
            boolean pinned = stop.pinned() != null ? stop.pinned() : old != null && old.pinned();
            if (pinned) {
                LocalDateTime appointment = stop.arrivalTime() != null ? stop.arrivalTime() : old == null ? null : old.arrivalTime();
                if (appointment == null || (arrival != null && arrival.isAfter(appointment)))
                    throw new AppException(ErrorCode.ITINERARY_PIN_CONFLICT);
                arrival = appointment;
            }
            if (arrival != null && !date.equals(arrival.toLocalDate())) throw new AppException(ErrorCode.INVALID_ITINERARY);
            updated.add(new ItineraryStopRequest(stop.placeId(), stop.sequence(), arrival, stop.durationMinutes(), stop.note(), pinned));
            if (arrival != null) arrival = arrival.plusMinutes(stop.durationMinutes());
        }
        return updated;
    }

    private Trip editableTrip(UUID owner, UUID tripId, LocalDate date) {
        var trip = trips.findByIdAndUserIdAndDeletedAtIsNull(tripId, owner)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
        var today = TripCalendar.today(clock);
        String mode = TripCalendar.mode(trip.getStatus().name(), trip.getStartDate(), trip.getEndDate(), today);
        if ("CANCELLED".equals(mode) || "RECAP".equals(mode) || ("LIVE".equals(mode) && date.isBefore(today)))
            throw new AppException(ErrorCode.TRIP_READ_ONLY);
        if (trip.getStartDate() == null || trip.getEndDate() == null || date.isBefore(trip.getStartDate()) || date.isAfter(trip.getEndDate()))
            throw new AppException(ErrorCode.INVALID_ITINERARY);
        return trip;
    }

    private DaySettings inferredSettings(LocalDate date, List<ItineraryStopRequest> stops) {
        if (stops.isEmpty() || stops.getFirst().arrivalTime() == null
                || !date.equals(stops.getFirst().arrivalTime().toLocalDate())) return null;
        return new DaySettings(null, stops.getFirst().arrivalTime().toLocalTime().withSecond(0).withNano(0),
                false, DaySettings.TravelMode.DRIVE);
    }

    private List<ItineraryStopResponse> dayStops(ItineraryResponse base, LocalDate date) {
        return base.days().stream().filter(day -> date.equals(day.date())).findFirst()
                .map(ItineraryResponse.ItineraryDayResponse::stops).orElse(List.of());
    }
    private DaySettings settings(ItineraryResponse base, LocalDate date) {
        return base.days().stream().filter(day -> date.equals(day.date())).findFirst()
                .map(ItineraryResponse.ItineraryDayResponse::settings).orElse(null);
    }
    private void checkRevision(Trip trip, ItineraryResponse base, Long version, java.time.Instant updatedAt) {
        if (!Objects.equals(version, trip.getVersion()) || !Objects.equals(updatedAt, base.updatedAt()))
            throw new AppException(ErrorCode.ITINERARY_CHANGED);
    }
}
