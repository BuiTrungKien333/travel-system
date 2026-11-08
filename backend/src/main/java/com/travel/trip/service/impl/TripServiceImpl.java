package com.travel.trip.service.impl;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.itinerary.dto.request.SaveItineraryDayRequest;
import com.travel.itinerary.service.DayScheduleService;
import com.travel.itinerary.service.ItineraryService;
import com.travel.place.entity.Place;
import com.travel.place.repository.PlaceRepository;
import com.travel.place.repository.ProvinceRepository;
import com.travel.trip.dto.request.AddTripPlaceRequest;
import com.travel.trip.dto.request.AddTripPlacesRequest;
import com.travel.trip.dto.request.UpdateTripPlaceRequest;
import com.travel.trip.dto.response.TripPlaceResponse;
import com.travel.trip.entity.Trip;
import com.travel.trip.entity.TripPlace;
import com.travel.trip.enums.TripStatus;
import com.travel.trip.repository.TripPlaceRepository;
import com.travel.trip.repository.TripRepository;
import com.travel.trip.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TripServiceImpl implements TripService {

    private final TripRepository trips;
    private final TripPlaceRepository tripPlaces;
    private final PlaceRepository places;
    private final ProvinceRepository provinces;
    private final ItineraryService itineraryService;
    private final DayScheduleService daySchedules;
    private final PlatformTransactionManager transactions;

    @Override
    @Transactional
    public TripPlaceResponse addPlace(UUID userId, UUID tripId, AddTripPlaceRequest request) {
        Trip trip = lockedTrip(userId, tripId);
        ensureMutable(trip);
        Place place;
        if (request.placeId() != null) {
            place = places.findVisible(request.placeId(), userId)
                    .orElseThrow(() -> new AppException(ErrorCode.PLACE_NOT_FOUND));
        } else {
            place = Place.builder()
                    .source("USER")
                    .ownerId(userId)
                    .name(request.name().trim())
                    .address(trim(request.address()))
                    .providerPlaceId(trim(request.providerPlaceId()))
                    .latitude(request.latitude())
                    .longitude(request.longitude())
                    .category(trim(request.category()))
                    .build();
            if (request.provinceId() != null) {
                place.setProvince(provinces.findById(request.provinceId())
                        .orElseThrow(() -> new AppException(ErrorCode.PROVINCE_NOT_FOUND)));
            }
            place = places.save(place);
        }

        if (tripPlaces.existsByTripIdAndPlaceId(tripId, place.getId())) {
            throw new AppException(ErrorCode.TRIP_PLACE_ALREADY_EXISTS);
        }

        TripPlace link = TripPlace.builder()
                .trip(trip)
                .place(place)
                .priority(request.priority() == null ? 0 : request.priority())
                .estimatedDuration(request.estimatedDuration())
                .notes(trim(request.notes()))
                .build();
        tripPlaces.saveAndFlush(link);
        movePlace(link, request.plannedOrder());
        tripPlaces.flush();
        return placeResponse(link);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<TripPlaceResponse> addPlaces(UUID userId, UUID tripId, AddTripPlacesRequest request) {
        // Resolve route I/O first; append and computed arrivals then commit together.
        var schedule = request.date() == null
                ? null
                : daySchedules.prepareAppend(userId, tripId, request.date(), request.placeIds());
        return new TransactionTemplate(transactions).execute(status -> addPlacesAtomically(userId, tripId, request, schedule));
    }

    private List<TripPlaceResponse> addPlacesAtomically(
            UUID userId,
            UUID tripId,
            AddTripPlacesRequest request,
            com.travel.itinerary.dto.request.SaveItineraryDayRequest schedule
    ) {
        Trip trip = lockedTrip(userId, tripId);
        ensureMutable(trip);
        if (schedule != null && (!Objects.equals(schedule.expectedTripVersion(), trip.getVersion())
                || !Objects.equals(schedule.expectedUpdatedAt(), itineraryService.get(userId, tripId).updatedAt()))) {
            throw new AppException(ErrorCode.ITINERARY_CHANGED);
        }
        if (new HashSet<>(request.placeIds()).size() != request.placeIds().size()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        }

        var visible = places.findVisibleByIds(request.placeIds(), userId).stream()
                .collect(Collectors.toMap(Place::getId, Function.identity()));
        if (visible.size() != request.placeIds().size()) {
            throw new AppException(ErrorCode.PLACE_NOT_FOUND);
        }

        var ordered = orderedPlaces(tripId);
        Map<UUID, TripPlace> links = ordered.stream().collect(Collectors.toMap(item -> item.getPlace().getId(), Function.identity()));
        int nextOrder = ordered.stream().map(TripPlace::getPlannedOrder).filter(Objects::nonNull).max(Integer::compareTo).orElse(-1) + 1;
        List<TripPlace> additions = new ArrayList<>();
        for (UUID id : request.placeIds()) {
            if (!links.containsKey(id)) {
                var link = TripPlace.builder().trip(trip).place(visible.get(id)).plannedOrder(nextOrder++).build();
                additions.add(link);
                links.put(id, link);
            }
        }
        tripPlaces.saveAllAndFlush(additions);

        var selected = request.placeIds().stream().map(links::get).toList();
        if (request.date() != null) {
            itineraryService.appendPlacesToDay(trip, request.date(), selected);
        }
        if (schedule != null) {
            var appended = itineraryService.get(userId, tripId);
            itineraryService.saveDay(userId, tripId, request.date(), new SaveItineraryDayRequest(
                    trip.getVersion(), appended.updatedAt(), schedule.stops(), schedule.settings()));
        }
        return selected.stream().map(this::placeResponse).toList();
    }

    @Override
    public List<TripPlaceResponse> listPlaces(UUID userId, UUID tripId) {
        Trip trip = trips.findByIdAndUserIdAndDeletedAtIsNull(tripId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
        return orderedPlaces(trip.getId()).stream().map(this::placeResponse).toList();
    }

    @Override
    @Transactional
    public TripPlaceResponse updatePlace(UUID userId, UUID tripId, UUID placeId, UpdateTripPlaceRequest request) {
        Trip trip = lockedTrip(userId, tripId);
        ensureMutable(trip);
        TripPlace link = linkedPlace(tripId, placeId);
        if (request.priority() != null) {
            link.setPriority(request.priority());
        }
        if (request.estimatedDuration() != null) {
            link.setEstimatedDuration(request.estimatedDuration());
        }
        if (request.notes() != null) {
            link.setNotes(trim(request.notes()));
        }
        if (request.visited() != null) {
            link.setVisited(request.visited());
        }
        if (request.plannedOrder() != null) {
            movePlace(link, request.plannedOrder());
        }
        tripPlaces.saveAndFlush(link);
        return placeResponse(link);
    }

    @Override
    @Transactional
    public void removePlace(UUID userId, UUID tripId, UUID placeId) {
        ensureMutable(lockedTrip(userId, tripId));
        tripPlaces.delete(linkedPlace(tripId, placeId));
        tripPlaces.flush();
        normalizeOrder(orderedPlaces(tripId));
    }

    private static void ensureMutable(Trip trip) {
        if (trip.getStatus() == TripStatus.CANCELLED) {
            throw new AppException(ErrorCode.TRIP_READ_ONLY);
        }
    }

    private Trip lockedTrip(UUID userId, UUID tripId) {
        // Serialize mutations of the aggregate, including duplicate adds and delete-vs-add.
        return trips.lockOwnedTrip(tripId, userId).orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
    }

    private TripPlace linkedPlace(UUID tripId, UUID placeId) {
        return tripPlaces.findByTripIdAndPlaceId(tripId, placeId)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_PLACE_NOT_FOUND));
    }

    private List<TripPlace> orderedPlaces(UUID tripId) {
        return tripPlaces.findByTripIdOrderByPlannedOrderAscCreatedAtAscIdAsc(tripId);
    }

    private void movePlace(TripPlace link, Integer position) {
        List<TripPlace> ordered = orderedPlaces(link.getTrip().getId());
        ordered.removeIf(item -> item.getId().equals(link.getId()));
        ordered.add(position == null ? ordered.size() : Math.min(position, ordered.size()), link);
        normalizeOrder(ordered);
    }

    private void normalizeOrder(List<TripPlace> ordered) {
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setPlannedOrder(i);
        }
    }

    private TripPlaceResponse placeResponse(TripPlace link) {
        Place place = link.getPlace();
        var province = place.getProvince();
        var details = new TripPlaceResponse.PlaceResponse(
                place.getId(),
                place.getProviderPlaceId(),
                place.getName(),
                place.getAddress(),
                place.getLatitude(),
                place.getLongitude(),
                province == null ? null : province.getId(),
                province == null ? null : province.getName(),
                place.getCategory(),
                place.getCreatedAt()
        );
        return new TripPlaceResponse(
                link.getId(),
                link.getTrip().getId(),
                place.getId(),
                details,
                link.getPriority(),
                link.getEstimatedDuration(),
                link.getNotes(),
                link.getPlannedOrder(),
                link.isVisited(),
                link.getCreatedAt(),
                link.getUpdatedAt()
        );
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
