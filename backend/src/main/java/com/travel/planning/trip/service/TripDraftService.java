package com.travel.planning.trip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.*;
import com.travel.itinerary.dto.DaySettings;
import com.travel.itinerary.dto.request.*;
import com.travel.itinerary.dto.response.ItineraryResponse;
import com.travel.itinerary.entity.ItineraryDaySettings;
import com.travel.itinerary.repository.ItineraryRepository;
import com.travel.itinerary.service.ItineraryService;
import com.travel.place.repository.PlaceRepository;
import com.travel.planning.draft.dto.request.GenerateDraftRequest;
import com.travel.planning.draft.dto.response.DraftResponse;
import com.travel.planning.draft.model.DraftLimits;
import com.travel.planning.draft.service.DraftService;
import com.travel.planning.retrieval.model.PlaceCandidate;
import com.travel.planning.retrieval.service.PlaceRetrievalService;
import com.travel.planning.trip.dto.*;
import com.travel.planning.trip.repository.TripDraftRepository;
import com.travel.trip.entity.*;
import com.travel.trip.repository.*;
import com.travel.trip.service.TripCalendar;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Profile({ "dev", "rag-demo", "planning-ai" })
public class TripDraftService {

    private final TripRepository trips;
    private final TripPlaceRepository links;
    private final ItineraryRepository itineraries;
    private final PlaceRepository places;
    private final ItineraryService itineraryService;
    private final DraftService generator;
    private final PlaceRetrievalService retrieval;
    private final TripDraftRepository drafts;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final TransactionTemplate tx;

    public TripDraftService(
        TripRepository trips,
        TripPlaceRepository links,
        ItineraryRepository itineraries,
        PlaceRepository places,
        ItineraryService itineraryService,
        DraftService generator,
        PlaceRetrievalService retrieval,
        TripDraftRepository drafts,
        ObjectMapper mapper,
        Clock clock,
        PlatformTransactionManager manager
    ) {
        this.trips = trips;
        this.links = links;
        this.itineraries = itineraries;
        this.places = places;
        this.itineraryService = itineraryService;
        this.generator = generator;
        this.retrieval = retrieval;
        this.drafts = drafts;
        this.mapper = mapper;
        this.clock = clock;
        this.tx = new TransactionTemplate(manager);
    }

    public TripDraftResponse create(UUID owner, UUID tripId, CreateTripDraftRequest request) {
        String json = write(request);
        boolean[] created = { false };
        var row = Objects.requireNonNull(
            tx.execute(s -> {
                drafts.lockOwner(owner); // Serializes the per-owner generation budget across trips/instances.
                var trip = owned(owner, tripId);
                var previous = drafts.find(tripId, owner, request.clientRequestId(), true);
                if (previous.isPresent()) {
                    if (!previous.get().requestJson().equals(json)) throw new AppException(
                        ErrorCode.TRIP_REQUEST_CONFLICT
                    );
                    return previous.get();
                }
                eligible(trip);
                if (trip.getVersion() != request.expectedTripVersion()) throw new AppException(
                    ErrorCode.ITINERARY_CHANGED
                );
                validateInput(trip, request.input());
                if (drafts.recentRequests(owner, clock.instant().minusSeconds(60)) >= 3) throw new AppException(
                    ErrorCode.AI_RATE_LIMIT_EXCEEDED
                );
                UUID id = UUID.randomUUID();
                drafts.insert(
                    id,
                    tripId,
                    owner,
                    request.clientRequestId(),
                    json,
                    trip.getVersion(),
                    drafts.revision(tripId),
                    clock.instant()
                );
                created[0] = true;
                return drafts.find(tripId, owner, id, false).orElseThrow();
            })
        );
        if (!created[0]) return response(row);

        // Provider work intentionally runs without a database transaction or row lock.
        try {
            DraftResponse proposal = generator.generate(request.input());
            return Objects.requireNonNull(
                tx.execute(s -> {
                    var trip = owned(owner, tripId);
                    unchanged(trip, row);
                    eligible(trip);
                    validateProposal(trip, proposal);
                    drafts.complete(row.id(), write(proposal));
                    return response(drafts.find(tripId, owner, row.id(), false).orElseThrow());
                })
            );
        } catch (RuntimeException failure) {
            try {
                tx.executeWithoutResult(s ->
                    drafts.failed(
                        row.id(),
                        failure instanceof AppException app
                            ? app.getErrorCode().name()
                            : ErrorCode.INTERNAL_SERVER_ERROR.name()
                    )
                );
            } catch (RuntimeException persistenceFailure) {
                failure.addSuppressed(persistenceFailure);
            }
            throw failure;
        }
    }

    public TripDraftResponse get(UUID owner, UUID tripId, UUID id) {
        return tx.execute(s -> {
            owned(owner, tripId);
            return response(find(owner, tripId, id));
        });
    }

    public TripDraftResponse latest(UUID owner, UUID tripId) {
        return tx.execute(s -> {
            owned(owner, tripId);
            return drafts
                .latest(tripId, owner, clock.instant())
                .map(id -> response(find(owner, tripId, id)))
                .orElse(null);
        });
    }

    public ItineraryResponse apply(UUID owner, UUID tripId, UUID id) {
        return tx.execute(s -> {
            var trip = owned(owner, tripId);
            var row = find(owner, tripId, id);
            if (row.appliedAt() != null) return itineraryService.get(owner, tripId);
            var draft = response(row);
            unchanged(trip, row);
            eligible(trip);
            if (!draft.canApply()) throw new AppException(ErrorCode.AI_VALIDATION_FAILED);
            var proposal = draft.proposal();
            String provinceCode = tripProvinceCode(trip);
            Map<UUID, PlaceCandidate> current = retrieval
                .catalog(provinceCode)
                .stream()
                .collect(Collectors.toMap(PlaceCandidate::placeId, p -> p));
            var stops = proposal
                .days()
                .stream()
                .flatMap(d -> d.stops().stream())
                .toList();
            for (var stop : stops) {
                if (!stop.place().equals(current.get(stop.place().placeId()))) throw new AppException(
                    ErrorCode.ITINERARY_CHANGED
                );
            }
            validateProposal(trip, proposal);
            var ids = stops
                .stream()
                .map(stop -> stop.place().placeId())
                .collect(Collectors.toSet());
            var entities = places
                .findAllByIdIn(ids)
                .stream()
                .collect(Collectors.toMap(p -> p.getId(), p -> p));
            if (entities.size() != ids.size()) throw new AppException(ErrorCode.ITINERARY_CHANGED);
            Set<UUID> existing = links
                .findByTripId(tripId)
                .stream()
                .map(p -> p.getPlace().getId())
                .collect(Collectors.toSet());
            links.saveAll(
                ids
                    .stream()
                    .filter(placeId -> !existing.contains(placeId))
                    .map(placeId -> TripPlace.builder().trip(trip).place(entities.get(placeId)).build())
                    .toList()
            );
            links.flush();
            List<ItineraryDayRequest> days = new ArrayList<>();
            for (var day : proposal.days()) {
                List<ItineraryStopRequest> savedStops = new ArrayList<>();
                for (var stop : day.stops())
                    savedStops.add(
                        new ItineraryStopRequest(
                            stop.place().placeId(),
                            savedStops.size(),
                            stop.visitStart(),
                            (int) Duration.between(stop.visitStart(), stop.departure()).toMinutes(),
                            null
                        )
                    );
                days.add(new ItineraryDayRequest(day.date(), savedStops));
            }
            itineraryService.save(owner, tripId, new SaveItineraryRequest(days));
            var itinerary = itineraries.findFirstByTripIdOrderByUpdatedAtDescIdAsc(tripId).orElseThrow();
            for (var day : proposal.days()) {
                var settings = itinerary
                    .getDaySettings()
                    .stream()
                    .filter(d -> d.getDate().equals(day.date()))
                    .findFirst()
                    .orElseGet(() -> {
                        var added = new ItineraryDaySettings(itinerary, day.date());
                        itinerary.getDaySettings().add(added);
                        return added;
                    });
                var origin = draft.input().origin();
                settings.update(
                    new DaySettings(
                        new DaySettings.Origin(origin.name(), null, origin.latitude(), origin.longitude()),
                        day.start().toLocalTime(),
                        Boolean.TRUE.equals(draft.input().roundTrip()),
                        DaySettings.TravelMode.DRIVE
                    )
                );
                for (var item : itinerary.getItems())
                    if (item.getDate().equals(day.date())) {
                        var stop = day
                            .stops()
                            .stream()
                            .filter(p -> p.place().placeId().equals(item.getPlace().getId()))
                            .findFirst()
                            .orElseThrow();
                        item.setDistanceFromPrevious(stop.distanceKm());
                        item.setTravelTimeFromPrevious(stop.transitMinutes());
                    }
            }
            itineraries.saveAndFlush(itinerary);
            drafts.applied(id, clock.instant());
            return itineraryService.get(owner, tripId);
        });
    }

    private Trip owned(UUID owner, UUID tripId) {
        return trips.lockOwnedTrip(tripId, owner).orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
    }

    private TripDraftRepository.Row find(UUID owner, UUID tripId, UUID id) {
        return drafts
            .find(tripId, owner, id, false)
            .orElseThrow(() -> new AppException(ErrorCode.PLANNING_DRAFT_NOT_FOUND));
    }

    private void unchanged(Trip trip, TripDraftRepository.Row row) {
        if (
            trip.getVersion() != row.tripVersion() || !drafts.revision(trip.getId()).equals(row.revision())
        ) throw new AppException(ErrorCode.ITINERARY_CHANGED);
    }

    private void eligible(Trip trip) {
        if (
            trip.getStatus() == com.travel.trip.enums.TripStatus.ACTIVE ||
            !"PLANNING".equals(
                TripCalendar.mode(
                    trip.getStatus().name(),
                    trip.getStartDate(),
                    trip.getEndDate(),
                    TripCalendar.today(clock)
                )
            )
        ) throw new AppException(ErrorCode.TRIP_READ_ONLY);
        if (
            tripProvinceCode(trip) == null ||
            trip.getStartDate() == null ||
            trip.getEndDate() == null ||
            numberOfDays(trip) < 1 ||
            numberOfDays(trip) > DraftLimits.MAX_DAYS
        ) throw new AppException(ErrorCode.PLANNING_UNSUPPORTED_TRIP);
        if (
            itineraries
                .findFirstByTripIdOrderByUpdatedAtDescIdAsc(trip.getId())
                .stream()
                .anyMatch(i -> !i.getItems().isEmpty())
        ) throw new AppException(ErrorCode.ITINERARY_CHANGED);
    }

    private long numberOfDays(Trip trip) {
        return java.time.temporal.ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate()) + 1;
    }

    private String tripProvinceCode(Trip trip) {
        String provinceCode = retrieval.provinceCode(trip.getDestinationProvinceId());
        return provinceCode != null ? provinceCode : retrieval.resolveProvinceCode(trip.getDestination());
    }

    private void validateInput(Trip trip, GenerateDraftRequest input) {
        if (
            !trip.getStartDate().equals(input.startDate()) ||
            (input.preferences() != null && input.preferences().numberOfDays() != numberOfDays(trip))
        ) throw new AppException(ErrorCode.INVALID_ITINERARY);
        if (
            input.preferences() != null &&
            !Objects.equals(tripProvinceCode(trip), retrieval.resolveProvinceCode(input.preferences().destination()))
        ) throw new AppException(ErrorCode.INVALID_ITINERARY);
    }

    private void validateProposal(Trip trip, DraftResponse proposal) {
        String expectedProvince = tripProvinceCode(trip);
        if (
            expectedProvince == null ||
            proposal.analysis() == null ||
            !Objects.equals(expectedProvince, proposal.analysis().provinceCode())
        ) throw new AppException(ErrorCode.INVALID_ITINERARY);
        Set<UUID> allowedPlaceIds = retrieval
            .catalog(expectedProvince)
            .stream()
            .map(PlaceCandidate::placeId)
            .collect(Collectors.toSet());
        if (
            proposal.analysis() != null &&
            proposal.analysis().intent() != null &&
            proposal.analysis().intent().numberOfDays() != null &&
            proposal.analysis().intent().numberOfDays() != numberOfDays(trip)
        ) throw new AppException(ErrorCode.INVALID_ITINERARY);
        Set<LocalDate> dates = new HashSet<>();
        Set<UUID> ids = new HashSet<>();
        for (var day : proposal.days()) {
            if (
                day.date().isBefore(trip.getStartDate()) ||
                day.date().isAfter(trip.getEndDate()) ||
                !dates.add(day.date())
            ) throw new AppException(ErrorCode.INVALID_ITINERARY);
            for (var stop : day.stops())
                if (
                    !allowedPlaceIds.contains(stop.place().placeId()) ||
                    !ids.add(stop.place().placeId()) ||
                    (!day.blocked() && !stop.visitStart().toLocalDate().equals(day.date())) ||
                    !stop.departure().isAfter(stop.visitStart())
                ) throw new AppException(ErrorCode.INVALID_ITINERARY);
        }
    }

    private TripDraftResponse response(TripDraftRepository.Row row) {
        if (row.appliedAt() == null && !row.expiresAt().isAfter(clock.instant())) throw new AppException(
            ErrorCode.PLANNING_DRAFT_EXPIRED
        );
        if (
            row.failureCode() != null ||
            (row.responseJson() == null && !row.createdAt().plusSeconds(120).isAfter(clock.instant()))
        ) throw new AppException(ErrorCode.PLANNING_DRAFT_FAILED);
        if (row.responseJson() == null) throw new AppException(ErrorCode.PLANNING_DRAFT_PENDING);
        var proposal = read(row.responseJson(), DraftResponse.class);
        boolean canApply =
            row.appliedAt() == null &&
            "READY_WITH_WARNINGS".equals(proposal.status()) &&
            proposal.issues().stream().noneMatch(DraftResponse.Problem::blocking) &&
            proposal.days().stream().noneMatch(DraftResponse.Day::blocked) &&
            proposal
                .days()
                .stream()
                .anyMatch(d -> !d.stops().isEmpty());
        return new TripDraftResponse(
            row.id(),
            row.tripId(),
            row.expiresAt(),
            row.appliedAt() != null,
            canApply,
            read(row.requestJson(), CreateTripDraftRequest.class).input(),
            proposal
        );
    }

    private String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot serialize planning draft", ex);
        }
    }

    private <T> T read(String value, Class<T> type) {
        try {
            return mapper.readValue(value, type);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot read planning draft", ex);
        }
    }
}
