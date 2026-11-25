package com.travel.planning.draft.service;

import static com.travel.planning.draft.dto.response.DraftResponse.*;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.maps.dto.RoutePoint;
import com.travel.maps.service.MapboxDirectionsService;
import com.travel.place.entity.Place;
import com.travel.planning.ai.intent.model.TravelIntent;
import com.travel.planning.draft.dto.request.GenerateDraftRequest;
import com.travel.planning.draft.model.DraftAllocation;
import com.travel.planning.draft.model.DraftTravelMatrix;
import com.travel.planning.retrieval.model.PlaceCandidate;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({ "dev", "rag-demo", "planning-ai" })
public class DraftSchedulingService {

    private final DraftTimelinePlanner planner;
    private final MapboxDirectionsService directions;

    public DraftSchedulingService(DraftTimelinePlanner planner, MapboxDirectionsService directions) {
        this.planner = planner;
        this.directions = directions;
    }

    public Schedule schedule(
        GenerateDraftRequest request,
        TravelIntent intent,
        DraftAllocation allocation,
        List<PlaceCandidate> candidates,
        Map<UUID, Place> entities,
        Set<UUID> required
    ) {
        Map<UUID, PlaceCandidate> byId = candidates
            .stream()
            .collect(Collectors.toMap(PlaceCandidate::placeId, Function.identity()));
        List<List<UUID>> groups = copy(allocation.days());
        DraftTravelMatrix travel = travelMatrix(request, candidates);
        // Request-local memoization: unchanged days need not run Held–Karp again during repair.
        Map<DayKey, Day> cache = new HashMap<>();
        List<Day> built = build(groups, request, intent, byId, entities, cache, travel);
        List<Day> initialBuilt = built;
        Map<UUID, String> removed = new HashMap<>();
        int repairPasses = 0;
        for (int pass = 0; pass < 2; pass++) {
            int bad = -1;
            for (int day = 0; day < built.size(); day++) if (built.get(day).blocked()) {
                bad = day;
                break;
            }
            if (bad < 0) break;
            Day badDay = built.get(bad);
            UUID target = badDay
                .issues()
                .stream()
                .filter(Problem::blocking)
                .map(Problem::placeId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
            if (target == null) target = groups
                .get(bad)
                .stream()
                .filter(id -> !required.contains(id))
                .reduce((a, b) -> b)
                .orElse(groups.get(bad).getLast());
            boolean moved = false;
            for (int other = 0; other < groups.size(); other++) {
                if (other == bad) continue;
                List<List<UUID>> trial = copy(groups);
                trial.get(bad).remove(target);
                trial.get(other).add(target);
                List<Day> next = build(trial, request, intent, byId, entities, cache, travel);
                if (!next.get(other).blocked() && blockingCount(next.get(bad)) < blockingCount(badDay)) {
                    groups = trial;
                    built = next;
                    moved = true;
                    break;
                }
            }
            if (!moved) {
                if (required.contains(target)) break;
                groups.get(bad).remove(target);
                removed.put(
                    target,
                    badDay
                        .issues()
                        .stream()
                        .anyMatch(p -> "ROUTE_UNAVAILABLE".equals(p.code()))
                        ? "ROAD_ROUTE_UNAVAILABLE"
                        : "REMOVED_TO_RESOLVE_CONFLICT"
                );
                built = build(groups, request, intent, byId, entities, cache, travel);
            }
            repairPasses++;
        }
        // Dropping every unreachable optional stop must not erase the original routing failure.
        if (
            built.stream().allMatch(d -> d.stops().isEmpty()) &&
            initialBuilt
                .stream()
                .flatMap(d -> d.issues().stream())
                .anyMatch(p -> "ROUTE_UNAVAILABLE".equals(p.code()))
        ) {
            built = initialBuilt;
            groups = copy(allocation.days());
        }
        Set<UUID> unreachable = new HashSet<>();
        for (int day = 0; day < built.size(); day++) if (
            built
                .get(day)
                .issues()
                .stream()
                .anyMatch(p -> "ROUTE_UNAVAILABLE".equals(p.code()))
        ) unreachable.addAll(groups.get(day));
        Set<UUID> assigned = built
            .stream()
            .flatMap(d -> d.stops().stream())
            .map(s -> s.place().placeId())
            .collect(Collectors.toSet());
        if (!assigned.containsAll(required) && built.stream().noneMatch(Day::blocked)) throw new IllegalStateException(
            "Draft lost a required place"
        );
        List<Unassigned> unassigned = candidates
            .stream()
            .filter(p -> !assigned.contains(p.placeId()))
            .map(p ->
                new Unassigned(
                    p,
                    required.contains(p.placeId())
                        ? "REQUIRED_PLACE_UNREACHABLE"
                        : unreachable.contains(p.placeId())
                          ? "ROAD_ROUTE_UNAVAILABLE"
                          : removed.getOrDefault(p.placeId(), "NOT_SELECTED")
                )
            )
            .toList();
        return new Schedule(List.copyOf(built), unassigned, repairPasses, travel.source(), travel.fallbackReason());
    }

    private DraftTravelMatrix travelMatrix(GenerateDraftRequest request, List<PlaceCandidate> candidates) {
        var points = new ArrayList<RoutePoint>();
        points.add(new RoutePoint(request.origin().name(), request.origin().latitude(), request.origin().longitude()));
        candidates.forEach(p -> points.add(new RoutePoint(p.name(), p.latitude(), p.longitude())));
        try {
            return new DraftTravelMatrix(
                DraftTravelMatrix.indices(candidates),
                directions.matrix(points, MapboxDirectionsService.Mode.DRIVE),
                DraftTravelMatrix.ROAD,
                null
            );
        } catch (AppException ex) {
            if (ex.getErrorCode() == ErrorCode.RESOURCE_NOT_FOUND) {
                double[][] unknown = new double[points.size()][points.size()];
                for (int i = 0; i < unknown.length; i++) {
                    Arrays.fill(unknown[i], Double.POSITIVE_INFINITY);
                    unknown[i][i] = 0;
                }
                return new DraftTravelMatrix(
                    DraftTravelMatrix.indices(candidates),
                    new MapboxDirectionsService.RoadMatrix(unknown, unknown),
                    DraftTravelMatrix.ROAD,
                    "NO_ROAD_ROUTE"
                );
            }
            if (
                ex.getErrorCode() != ErrorCode.PLACE_PROVIDER_UNAVAILABLE &&
                ex.getErrorCode() != ErrorCode.PLACE_PROVIDER_INVALID_RESPONSE
            ) throw ex;
            return DraftTravelMatrix.estimated(request.origin(), candidates, ex.getErrorCode().name());
        }
    }

    private List<Day> build(
        List<List<UUID>> groups,
        GenerateDraftRequest request,
        TravelIntent intent,
        Map<UUID, PlaceCandidate> candidates,
        Map<UUID, Place> entities,
        Map<DayKey, Day> cache,
        DraftTravelMatrix travel
    ) {
        List<Day> result = new ArrayList<>();
        for (int day = 0; day < groups.size(); day++) {
            DayKey key = new DayKey(day + 1, List.copyOf(groups.get(day)));
            result.add(
                cache.computeIfAbsent(key, k ->
                    planner.build(
                        k.index(),
                        request.startDate().plusDays(k.index() - 1),
                        LocalTime.parse(intent.dailyStartTime()),
                        LocalTime.parse(intent.dailyEndTime()),
                        request.origin(),
                        request.roundTrip(),
                        request.restMinutes(),
                        bufferMinutes(intent),
                        k.ids().stream().map(candidates::get).toList(),
                        entities,
                        travel
                    )
                )
            );
        }
        return result;
    }

    public int bufferMinutes(TravelIntent intent) {
        return "RELAXED".equals(intent.travelPace()) ? 15 : "ACTIVE".equals(intent.travelPace()) ? 5 : 10;
    }

    private long blockingCount(Day day) {
        return day.issues().stream().filter(Problem::blocking).count();
    }

    private List<List<UUID>> copy(List<List<UUID>> groups) {
        return groups
            .stream()
            .map(g -> (List<UUID>) new ArrayList<>(g))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private record DayKey(int index, List<UUID> ids) {}

    public record Schedule(
        List<Day> days,
        List<Unassigned> unassigned,
        int repairPasses,
        String routeSource,
        String fallbackReason
    ) {}
}
