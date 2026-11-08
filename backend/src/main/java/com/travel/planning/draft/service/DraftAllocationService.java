package com.travel.planning.draft.service;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.planning.ai.intent.model.TravelIntent;
import com.travel.planning.ai.resilience.AiCallFailure;
import com.travel.planning.draft.model.DraftAllocation;
import com.travel.planning.draft.provider.GeminiDraftClient;
import com.travel.planning.retrieval.model.PlaceCandidate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@Profile({"dev", "rag-demo"})
public class DraftAllocationService {

    private final GeminiDraftClient client;

    public DraftAllocationService(GeminiDraftClient client) {
        this.client = client;
    }

    public DraftAllocation allocate(List<PlaceCandidate> candidates, Set<UUID> required, TravelIntent intent, LocalDate startDate) {
        try {
            return client.allocate(candidates, required, intent, startDate);
        } catch (AppException ex) {
            String reason = ex instanceof AiCallFailure failure ? failure.reason()
                    : ex.getErrorCode() == ErrorCode.AI_VALIDATION_FAILED ? "INVALID_RESPONSE" : "AI_UNAVAILABLE";

            return new DraftAllocation(fallback(candidates, required, intent), Map.of(), "RULE_BASED_FALLBACK", reason);
        }
    }

    private List<List<UUID>> fallback(List<PlaceCandidate> candidates, Set<UUID> required, TravelIntent intent) {
        List<List<UUID>> groups = new ArrayList<>();
        int[] load = new int[intent.numberOfDays()];
        for (int i = 0; i < load.length; i++)
            groups.add(new ArrayList<>());

        List<PlaceCandidate> ranked = new ArrayList<>(candidates);
        ranked.sort(Comparator.comparing(p -> !required.contains(p.placeId())));
        int target = "RELAXED".equals(intent.travelPace()) ? 3 : "ACTIVE".equals(intent.travelPace()) ? 5 : 4;

        for (var place : ranked) {
            boolean mandatory = required.contains(place.placeId());
            if (!mandatory && Collections.disjoint(place.tags(), intent.interests())) continue;
            int best = -1;
            for (int day = 0; day < load.length; day++) {
                if ((mandatory || groups.get(day).size() < target) && (best < 0 || load[day] < load[best]))
                    best = day;
            }

            if (best >= 0) {
                groups.get(best).add(place.placeId());
                load[best] += place.estimatedDurationMinutes() + 15;
            }
        }

        return groups.stream().map(List::copyOf).toList();
    }
}
