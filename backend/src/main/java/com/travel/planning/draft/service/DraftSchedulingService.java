package com.travel.planning.draft.service;

import com.travel.place.entity.Place;
import com.travel.planning.ai.intent.model.TravelIntent;
import com.travel.planning.draft.dto.request.GenerateDraftRequest;
import com.travel.planning.draft.model.DraftAllocation;
import com.travel.planning.retrieval.model.PlaceCandidate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.travel.planning.draft.dto.response.DraftResponse.*;

@Service
@Profile("rag-demo")
public class DraftSchedulingService {

    private final DraftTimelinePlanner planner;

    public DraftSchedulingService(DraftTimelinePlanner planner) {
        this.planner = planner;
    }

    public Schedule schedule(GenerateDraftRequest request, TravelIntent intent, DraftAllocation allocation,
                             List<PlaceCandidate> candidates, Map<UUID, Place> entities, Set<UUID> required) {
        Map<UUID, PlaceCandidate> byId = candidates.stream().collect(Collectors.toMap(PlaceCandidate::placeId, Function.identity()));
        List<List<UUID>> groups = copy(allocation.days());

        // Request-local memoization: unchanged days need not run Held–Karp again during repair.
        Map<DayKey, Day> cache = new HashMap<>();
        List<Day> built = build(groups, request, intent, byId, entities, cache);
        Map<UUID, String> removed = new HashMap<>();
        int repairPasses = 0;

        for (int pass = 0; pass < 2; pass++) {
            int bad = -1;
            for (int day = 0; day < built.size(); day++)
                if (built.get(day).blocked()) {
                    bad = day;
                    break;
                }
            if (bad < 0) break;
            Day badDay = built.get(bad);
            UUID target = badDay.issues().stream().filter(Problem::blocking).map(Problem::placeId).filter(Objects::nonNull).findFirst().orElse(null);
            if (target == null)
                target = groups.get(bad).stream().filter(id -> !required.contains(id)).reduce((a, b) -> b).orElse(groups.get(bad).getLast());

            boolean moved = false;
            for (int other = 0; other < groups.size(); other++) {
                if (other == bad) continue;
                List<List<UUID>> trial = copy(groups);
                trial.get(bad).remove(target);
                trial.get(other).add(target);
                List<Day> next = build(trial, request, intent, byId, entities, cache);
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
                removed.put(target, "REMOVED_TO_RESOLVE_CONFLICT");
                built = build(groups, request, intent, byId, entities, cache);
            }
            repairPasses++;
        }
        Set<UUID> assigned = built.stream().flatMap(d -> d.stops().stream()).map(s -> s.place().placeId()).collect(Collectors.toSet());
        if (!assigned.containsAll(required)) throw new IllegalStateException("Draft lost a required place");
        List<Unassigned> unassigned = candidates.stream().filter(p -> !assigned.contains(p.placeId()))
                .map(p -> new Unassigned(p, removed.getOrDefault(p.placeId(), "NOT_SELECTED"))).toList();

        return new Schedule(List.copyOf(built), unassigned, repairPasses);
    }

    private List<Day> build(List<List<UUID>> groups, GenerateDraftRequest request, TravelIntent intent,
                            Map<UUID, PlaceCandidate> candidates, Map<UUID, Place> entities, Map<DayKey, Day> cache) {
        List<Day> result = new ArrayList<>();
        for (int day = 0; day < groups.size(); day++) {
            DayKey key = new DayKey(day + 1, List.copyOf(groups.get(day)));
            result.add(cache.computeIfAbsent(key, k -> planner.build(k.index(), request.startDate().plusDays(k.index() - 1),
                    LocalTime.parse(intent.dailyStartTime()), LocalTime.parse(intent.dailyEndTime()), request.origin(),
                    request.roundTrip(), request.restMinutes(), bufferMinutes(intent),
                    k.ids().stream().map(candidates::get).toList(), entities)));
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
        return groups.stream().map(g -> (List<UUID>) new ArrayList<>(g)).collect(Collectors.toCollection(ArrayList::new));
    }

    private record DayKey(int index, List<UUID> ids) {
    }

    public record Schedule(List<Day> days, List<Unassigned> unassigned, int repairPasses) {
    }
}
