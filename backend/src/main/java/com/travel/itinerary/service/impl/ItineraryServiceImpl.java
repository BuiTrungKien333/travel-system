package com.travel.itinerary.service.impl;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.itinerary.dto.request.ApplyItineraryOptimizationRequest;
import com.travel.itinerary.dto.request.ItineraryDayRequest;
import com.travel.itinerary.dto.request.ItineraryStopRequest;
import com.travel.itinerary.dto.request.SaveItineraryDayRequest;
import com.travel.itinerary.dto.request.SaveItineraryRequest;
import com.travel.itinerary.dto.response.ItineraryOptimizationResponse;
import com.travel.itinerary.dto.response.ItineraryResponse;
import com.travel.itinerary.entity.Itinerary;
import com.travel.itinerary.entity.ItineraryDaySettings;
import com.travel.itinerary.entity.ItineraryItem;
import com.travel.itinerary.repository.ItineraryRepository;
import com.travel.itinerary.service.ItineraryService;
import com.travel.place.entity.Place;
import com.travel.planning.dto.request.DayPlanRequest;
import com.travel.planning.dto.request.MultiDayOptimizeAndAuditRequest;
import com.travel.planning.dto.request.TspLocationRequest;
import com.travel.planning.dto.response.DayScheduleResponse;
import com.travel.planning.dto.response.MultiDayOptimizeAndAuditResponse;
import com.travel.planning.rule.model.ScheduleItemContext;
import com.travel.planning.service.TripOptimizationService;
import com.travel.trip.entity.Trip;
import com.travel.trip.entity.TripPlace;
import com.travel.trip.repository.TripPlaceRepository;
import com.travel.trip.repository.TripRepository;
import com.travel.trip.service.TripCalendar;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItineraryServiceImpl implements ItineraryService {

    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(8, 0);

    private final TripRepository trips;
    private final TripPlaceRepository tripPlaces;
    private final ItineraryRepository itineraries;
    private final TripOptimizationService optimizer;
    private final EntityManager entityManager;
    private final Clock clock;

    @Override
    public ItineraryResponse get(UUID userId, UUID tripId) {
        Trip trip = trips
            .findByIdAndUserIdAndDeletedAtIsNull(tripId, userId)
            .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
        return itineraries
            .findFirstByTripIdOrderByUpdatedAtDescIdAsc(tripId)
            .map(this::response)
            .orElseGet(() -> emptyResponse(tripId));
    }

    @Override
    @Transactional
    public ItineraryResponse save(UUID userId, UUID tripId, SaveItineraryRequest request) {
        Trip trip = trips.lockOwnedTrip(tripId, userId).orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));

        List<ItineraryStopRequest> stops = validateAndFlatten(request);
        Set<UUID> requestedPlaceIds = stops.stream().map(ItineraryStopRequest::placeId).collect(Collectors.toSet());
        Map<UUID, TripPlace> allowedPlaces = requestedPlaceIds.isEmpty()
            ? Map.of()
            : tripPlaces
                  .findByTripIdAndPlaceIdIn(tripId, requestedPlaceIds)
                  .stream()
                  .collect(Collectors.toMap(link -> link.getPlace().getId(), link -> link));
        if (allowedPlaces.size() != requestedPlaceIds.size()) {
            throw new AppException(ErrorCode.ITINERARY_PLACE_NOT_IN_TRIP);
        }

        Itinerary itinerary = itineraries
            .findFirstByTripIdOrderByUpdatedAtDescIdAsc(tripId)
            .orElseGet(() -> newItinerary(trip));
        List<ItineraryItem> replacement = new ArrayList<>(stops.size());
        for (ItineraryDayRequest day : request.days()) {
            for (ItineraryStopRequest stop : day.stops()) {
                ItineraryItem item = new ItineraryItem();
                item.setPlace(allowedPlaces.get(stop.placeId()).getPlace());
                item.setSchedule(day.date(), stop.arrivalTime(), stop.durationMinutes());
                item.setSequence(stop.sequence());
                item.setNote(trim(stop.note()));
                replacement.add(item);
            }
        }
        itinerary.setStatus("DRAFT");
        itinerary.replaceItems(replacement);
        return persist(itinerary);
    }

    @Override
    @Transactional
    public ItineraryResponse saveDay(UUID userId, UUID tripId, LocalDate date, SaveItineraryDayRequest request) {
        Trip trip = trips.lockOwnedTrip(tripId, userId).orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
        validateEditableDay(trip, date);
        Itinerary existing = itineraries.findFirstByTripIdOrderByUpdatedAtDescIdAsc(tripId).orElse(null);
        if (
            !Objects.equals(request.expectedTripVersion(), trip.getVersion()) ||
            !Objects.equals(request.expectedUpdatedAt(), existing == null ? null : existing.getUpdatedAt())
        ) {
            throw new AppException(ErrorCode.ITINERARY_CHANGED);
        }
        var stops = validateAndFlatten(
            new SaveItineraryRequest(List.of(new ItineraryDayRequest(date, request.stops())))
        );
        var placeIds = stops.stream().map(ItineraryStopRequest::placeId).collect(Collectors.toSet());
        var membership = placeIds.isEmpty()
            ? Map.<UUID, Place>of()
            : tripPlaces
                  .findByTripIdAndPlaceIdIn(tripId, placeIds)
                  .stream()
                  .collect(Collectors.toMap(link -> link.getPlace().getId(), TripPlace::getPlace));
        if (membership.size() != placeIds.size()) throw new AppException(ErrorCode.ITINERARY_PLACE_NOT_IN_TRIP);
        if (existing == null && stops.isEmpty() && request.settings() == null) return emptyResponse(tripId);

        Itinerary itinerary = existing == null ? newItinerary(trip) : existing;
        if (request.settings() != null) {
            var settings = itinerary
                .getDaySettings()
                .stream()
                .filter(day -> date.equals(day.getDate()))
                .findFirst()
                .orElseGet(() -> {
                    var day = new ItineraryDaySettings(itinerary, date);
                    itinerary.getDaySettings().add(day);
                    return day;
                });
            settings.update(request.settings());
        }
        Map<UUID, ItineraryItem> previous = new HashMap<>();
        for (ItineraryItem item : itinerary.getItems()) {
            if (date.equals(item.getDate())) previous.putIfAbsent(item.getPlace().getId(), item);
        }
        if (request.expectedVisitedPlaceIds() != null) {
            var actual = tripPlaces
                .findByTripIdAndPlaceIdIn(tripId, previous.keySet())
                .stream()
                .filter(TripPlace::isVisited)
                .sorted(Comparator.comparingInt(link -> previous.get(link.getPlace().getId()).getSequence()))
                .map(link -> link.getPlace().getId())
                .toList();
            if (!actual.equals(request.expectedVisitedPlaceIds())) throw new AppException(ErrorCode.ITINERARY_CHANGED);
            var ordered = stops
                .stream()
                .sorted(Comparator.comparingInt(ItineraryStopRequest::sequence))
                .map(ItineraryStopRequest::placeId)
                .toList();
            if (
                !previous.keySet().equals(placeIds) ||
                ordered.size() < actual.size() ||
                !ordered.subList(0, actual.size()).equals(actual)
            ) throw new AppException(ErrorCode.INVALID_ITINERARY);
        }
        if (
            request.expectedVisitedPlaceIds() != null ||
            (date.equals(TripCalendar.today(clock)) &&
                "LIVE".equals(
                    TripCalendar.mode(
                        trip.getStatus().name(),
                        trip.getStartDate(),
                        trip.getEndDate(),
                        TripCalendar.today(clock)
                    )
                ))
        ) {
            var visited = tripPlaces
                .findByTripIdAndPlaceIdIn(tripId, previous.keySet())
                .stream()
                .filter(TripPlace::isVisited)
                .map(link -> link.getPlace().getId())
                .collect(Collectors.toSet());
            var requested = stops.stream().collect(Collectors.toMap(ItineraryStopRequest::placeId, stop -> stop));
            for (UUID id : visited) {
                var old = previous.get(id);
                var next = requested.get(id);
                if (
                    next == null ||
                    (request.expectedVisitedPlaceIds() == null && next.sequence() != old.getSequence()) ||
                    next.durationMinutes() != old.getDurationMinutes() ||
                    !Objects.equals(next.arrivalTime(), old.getArrivalAt())
                ) throw new AppException(ErrorCode.TRIP_READ_ONLY);
            }
        }
        for (ItineraryStopRequest stop : stops) {
            var old = previous.get(stop.placeId());
            if (
                stop.arrivalTime() != null &&
                !date.equals(stop.arrivalTime().toLocalDate()) &&
                (old == null || !Objects.equals(old.getArrivalAt(), stop.arrivalTime()))
            ) {
                throw new AppException(ErrorCode.INVALID_ITINERARY);
            }
        }
        boolean routeChanged =
            request.settings() != null ||
            previous.size() != stops.size() ||
            stops.stream().anyMatch(stop -> {
                var old = previous.get(stop.placeId());
                return old == null || old.getSequence() != stop.sequence();
            });
        itinerary
            .getItems()
            .removeIf(
                item ->
                    date.equals(item.getDate()) &&
                    (!placeIds.contains(item.getPlace().getId()) || item != previous.get(item.getPlace().getId()))
            );
        for (ItineraryStopRequest stop : stops) {
            ItineraryItem item = previous.get(stop.placeId());
            if (item == null) {
                item = new ItineraryItem();
                item.setItinerary(itinerary);
                item.setPlace(membership.get(stop.placeId()));
                itinerary.getItems().add(item);
            }
            item.setSchedule(date, stop.arrivalTime(), stop.durationMinutes());
            item.setSequence(stop.sequence());
            item.setNote(trim(stop.note()));
            if (routeChanged) {
                item.setDistanceFromPrevious(null);
                item.setTravelTimeFromPrevious(null);
            }
        }
        itinerary.setStatus("DRAFT");
        return persist(itinerary);
    }

    private void validateEditableDay(Trip trip, LocalDate date) {
        LocalDate today = TripCalendar.today(clock);
        String mode = TripCalendar.mode(trip.getStatus().name(), trip.getStartDate(), trip.getEndDate(), today);
        if ("RECAP".equals(mode) || "CANCELLED".equals(mode) || ("LIVE".equals(mode) && date.isBefore(today))) {
            throw new AppException(ErrorCode.TRIP_READ_ONLY);
        }
        if (
            TripCalendar.days(trip.getStartDate(), trip.getEndDate()) == 0 ||
            date.isBefore(trip.getStartDate()) ||
            date.isAfter(trip.getEndDate())
        ) {
            throw new AppException(ErrorCode.INVALID_ITINERARY);
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void appendPlacesToDay(Trip trip, LocalDate date, List<TripPlace> places) {
        // The caller holds the trip lock and creates memberships in this same transaction.
        validateEditableDay(trip, date);
        Itinerary itinerary = itineraries
            .findFirstByTripIdOrderByUpdatedAtDescIdAsc(trip.getId())
            .orElseGet(() -> newItinerary(trip));
        var dayItems = itinerary
            .getItems()
            .stream()
            .filter(item -> date.equals(item.getDate()))
            .toList();
        var scheduledIds = dayItems
            .stream()
            .map(item -> item.getPlace().getId())
            .collect(Collectors.toSet());
        var additions = places
            .stream()
            .filter(place -> !scheduledIds.contains(place.getPlace().getId()))
            .toList();
        if (additions.isEmpty()) return;
        if (dayItems.size() + additions.size() > 100) throw new AppException(ErrorCode.INVALID_ITINERARY);
        int sequence = dayItems.stream().mapToInt(ItineraryItem::getSequence).max().orElse(-1) + 1;
        for (TripPlace place : additions) {
            int duration = place.getEstimatedDuration() == null ? 45 : place.getEstimatedDuration();
            if (duration < 1 || duration > 1440) throw new AppException(ErrorCode.INVALID_ITINERARY);
            var item = new ItineraryItem();
            item.setItinerary(itinerary);
            item.setPlace(place.getPlace());
            item.setSchedule(date, null, duration);
            item.setSequence(sequence++);
            item.setNote(place.getNotes());
            itinerary.getItems().add(item);
        }
        itinerary.setStatus("DRAFT");
        persist(itinerary);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ItineraryOptimizationResponse optimize(UUID userId, UUID tripId) {
        Trip trip = trips
            .findByIdAndUserIdAndDeletedAtIsNull(tripId, userId)
            .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
        Itinerary itinerary = itineraries
            .findFirstByTripIdOrderByUpdatedAtDescIdAsc(tripId)
            .orElseThrow(() -> new AppException(ErrorCode.ITINERARY_NOT_FOUND));
        if (itinerary.getItems().isEmpty()) throw new AppException(ErrorCode.INVALID_ITINERARY);

        Map<LocalDate, List<ItineraryItem>> byDate = itinerary
            .getItems()
            .stream()
            .sorted(Comparator.comparing(ItineraryItem::getDate).thenComparingInt(ItineraryItem::getSequence))
            .collect(Collectors.groupingBy(ItineraryItem::getDate, TreeMap::new, Collectors.toList()));
        List<DayPlanRequest> days = new ArrayList<>(byDate.size());
        for (var entry : byDate.entrySet()) {
            List<ItineraryItem> items = entry.getValue();
            LocalTime startTime = items
                .stream()
                .map(ItineraryItem::getArrivalAt)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalTime)
                .findFirst()
                .orElse(DEFAULT_START_TIME);
            List<TspLocationRequest> locations = items
                .stream()
                .map(item -> toLocation(item.getPlace(), item.getDurationMinutes()))
                .toList();
            days.add(new DayPlanRequest(entry.getKey(), startTime, null, locations, false));
        }

        String notes =
            trip.getConstraints() != null && !trip.getConstraints().isBlank()
                ? trip.getConstraints()
                : trip.getDescription();
        MultiDayOptimizeAndAuditResponse proposal = optimizer.optimizeAndAuditMultiDay(
            new MultiDayOptimizeAndAuditRequest(trip.getDestination(), days, notes, false)
        );
        return new ItineraryOptimizationResponse(tripId, itinerary.getUpdatedAt(), proposal);
    }

    @Override
    @Transactional
    public ItineraryResponse applyOptimization(UUID userId, UUID tripId, ApplyItineraryOptimizationRequest request) {
        trips.lockOwnedTrip(tripId, userId).orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
        Itinerary itinerary = itineraries
            .findFirstByTripIdOrderByUpdatedAtDescIdAsc(tripId)
            .orElseThrow(() -> new AppException(ErrorCode.ITINERARY_NOT_FOUND));
        if (
            request == null ||
            request.expectedUpdatedAt() == null ||
            request.proposal() == null ||
            !request.expectedUpdatedAt().equals(itinerary.getUpdatedAt())
        ) {
            throw new AppException(ErrorCode.ITINERARY_CHANGED);
        }
        MultiDayOptimizeAndAuditResponse result = request.proposal();
        if (result.dailySchedules() == null || result.dailySchedules().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ITINERARY);
        }

        Map<PlannedStopKey, ItineraryItem> currentItems = itinerary
            .getItems()
            .stream()
            .collect(
                Collectors.toMap(item -> new PlannedStopKey(item.getDate(), item.getPlace().getId()), item -> item)
            );
        Map<PlannedStopKey, ScheduleItemContext> optimizedItems = new LinkedHashMap<>();
        for (DayScheduleResponse day : result.dailySchedules()) {
            if (day == null || day.dayDate() == null || day.scheduleItems() == null) {
                throw new AppException(ErrorCode.INVALID_ITINERARY);
            }
            for (ScheduleItemContext item : day.scheduleItems()) {
                if (
                    item == null ||
                    item.placeId() == null ||
                    item.arrivalTime() == null ||
                    item.durationMinutes() < 1 ||
                    item.durationMinutes() > 1440 ||
                    !Double.isFinite(item.transitDistanceKmFromPrev()) ||
                    item.transitDistanceKmFromPrev() < 0 ||
                    item.transitMinutesFromPrev() < 0
                ) {
                    throw new AppException(ErrorCode.INVALID_ITINERARY);
                }
                LocalDate plannedDate = item.plannedDate() == null ? day.dayDate() : item.plannedDate();
                PlannedStopKey key = new PlannedStopKey(plannedDate, item.placeId());
                if (optimizedItems.putIfAbsent(key, item) != null) {
                    throw new AppException(ErrorCode.INVALID_ITINERARY);
                }
            }
        }
        if (!optimizedItems.keySet().equals(currentItems.keySet())) {
            throw new AppException(ErrorCode.INVALID_ITINERARY);
        }

        Map<LocalDate, List<ItineraryItem>> replacementByDate = new TreeMap<>();
        for (var entry : optimizedItems.entrySet()) {
            LocalDate plannedDate = entry.getKey().plannedDate();
            ScheduleItemContext optimized = entry.getValue();
            ItineraryItem previous = currentItems.get(entry.getKey());
            ItineraryItem item = new ItineraryItem();
            item.setPlace(previous.getPlace());
            item.setSchedule(plannedDate, optimized.arrivalTime(), optimized.durationMinutes());
            item.setDistanceFromPrevious(optimized.transitDistanceKmFromPrev());
            item.setTravelTimeFromPrevious(optimized.transitMinutesFromPrev());
            item.setNote(previous.getNote());
            replacementByDate.computeIfAbsent(plannedDate, ignored -> new ArrayList<>()).add(item);
        }
        List<ItineraryItem> replacement = new ArrayList<>();
        for (List<ItineraryItem> dayItems : replacementByDate.values()) {
            for (int sequence = 0; sequence < dayItems.size(); sequence++) {
                dayItems.get(sequence).setSequence(sequence);
                replacement.add(dayItems.get(sequence));
            }
        }
        itinerary.setStatus("FINAL");
        itinerary.replaceItems(replacement);
        return persist(itinerary);
    }

    private ItineraryResponse persist(Itinerary itinerary) {
        // Force a parent revision even for an edit that only changes child rows.
        itinerary.setUpdatedAt(Instant.now());
        Itinerary saved = itineraries.saveAndFlush(itinerary);
        entityManager.refresh(saved);
        return response(saved);
    }

    private List<ItineraryStopRequest> validateAndFlatten(SaveItineraryRequest request) {
        if (request == null || request.days() == null) throw new AppException(ErrorCode.INVALID_ITINERARY);
        Set<LocalDate> dates = new HashSet<>();
        List<ItineraryStopRequest> stops = new ArrayList<>();
        for (ItineraryDayRequest day : request.days()) {
            if (day == null || day.date() == null || day.stops() == null || !dates.add(day.date())) {
                throw new AppException(ErrorCode.INVALID_ITINERARY);
            }
            boolean[] sequences = new boolean[day.stops().size()];
            Set<UUID> dayPlaceIds = new HashSet<>();
            for (ItineraryStopRequest stop : day.stops()) {
                if (
                    stop == null ||
                    stop.placeId() == null ||
                    stop.sequence() == null ||
                    stop.sequence() < 0 ||
                    stop.sequence() >= sequences.length ||
                    sequences[stop.sequence()] ||
                    stop.durationMinutes() == null ||
                    stop.durationMinutes() < 1 ||
                    stop.durationMinutes() > 1440 ||
                    !dayPlaceIds.add(stop.placeId())
                ) {
                    throw new AppException(ErrorCode.INVALID_ITINERARY);
                }
                sequences[stop.sequence()] = true;
                stops.add(stop);
            }
            for (boolean present : sequences) {
                if (!present) throw new AppException(ErrorCode.INVALID_ITINERARY);
            }
        }
        return stops;
    }

    private Itinerary newItinerary(Trip trip) {
        Itinerary itinerary = new Itinerary();
        itinerary.setTrip(trip);
        itinerary.setName("Main");
        itinerary.setStatus("DRAFT");
        return itinerary;
    }

    private ItineraryResponse response(Itinerary itinerary) {
        Map<LocalDate, List<ItineraryResponse.ItineraryStopResponse>> byDate = new TreeMap<>();
        itinerary
            .getItems()
            .stream()
            .sorted(
                Comparator.comparing(ItineraryItem::getDate)
                    .thenComparingInt(ItineraryItem::getSequence)
                    .thenComparing(ItineraryItem::getId)
            )
            .forEach(item -> {
                Place place = item.getPlace();
                byDate
                    .computeIfAbsent(item.getDate(), ignored -> new ArrayList<>())
                    .add(
                        new ItineraryResponse.ItineraryStopResponse(
                            item.getId(),
                            place.getId(),
                            place.getName(),
                            place.getAddress(),
                            place.getLatitude(),
                            place.getLongitude(),
                            place.getCategory(),
                            item.getArrivalAt(),
                            item.getDurationMinutes(),
                            item.getSequence(),
                            item.getNote(),
                            item.getDistanceFromPrevious(),
                            item.getTravelTimeFromPrevious()
                        )
                    );
            });
        var settings = itinerary
            .getDaySettings()
            .stream()
            .filter(
                day ->
                    itinerary.getTrip().getStartDate() != null &&
                    itinerary.getTrip().getEndDate() != null &&
                    !day.getDate().isBefore(itinerary.getTrip().getStartDate()) &&
                    !day.getDate().isAfter(itinerary.getTrip().getEndDate())
            )
            .collect(Collectors.toMap(ItineraryDaySettings::getDate, ItineraryDaySettings::snapshot));
        settings.keySet().forEach(date -> byDate.computeIfAbsent(date, ignored -> new ArrayList<>()));
        List<ItineraryResponse.ItineraryDayResponse> days = byDate
            .entrySet()
            .stream()
            .map(entry ->
                new ItineraryResponse.ItineraryDayResponse(
                    entry.getKey(),
                    entry.getValue(),
                    settings.get(entry.getKey())
                )
            )
            .toList();
        return new ItineraryResponse(
            itinerary.getId(),
            itinerary.getTrip().getId(),
            itinerary.getName(),
            itinerary.getStatus(),
            days,
            itinerary.getUpdatedAt()
        );
    }

    private ItineraryResponse emptyResponse(UUID tripId) {
        return new ItineraryResponse(null, tripId, "Main", "DRAFT", List.of(), null);
    }

    private TspLocationRequest toLocation(Place place, int durationMinutes) {
        return new TspLocationRequest(
            place.getId(),
            place.getName(),
            place.getLatitude(),
            place.getLongitude(),
            place.getPlaceType(),
            place.getOpeningTime(),
            place.getClosingTime(),
            durationMinutes
        );
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record PlannedStopKey(LocalDate plannedDate, UUID placeId) {}
}
