package com.travel.planning.draft;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.planning.ai.intent.TravelIntent;
import com.travel.planning.ai.provider.GeminiStructuredClient;
import com.travel.planning.algorithm.*;
import com.travel.planning.timeline.TimelineEngine;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static com.travel.planning.draft.DraftFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DraftAiAdvisorTest {
    final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    final GeminiStructuredClient ai = mock(GeminiStructuredClient.class);
    final DraftAiAdvisor advisor = new DraftAiAdvisor(ai, mapper);
    DraftAiAdvisorTest() throws Exception {}
    final TravelIntent intent = new TravelIntent("Đà Nẵng", 2, List.of("CULTURE"), List.of(), "08:00", "17:00", "RELAXED", List.of(), List.of(), Map.of());

    @Test void validAllocationCanSelectSubsetButKeepsRequired() throws Exception {
        var points = List.of(candidate(1), candidate(2), candidate(3));
        when(ai.generate(any(com.travel.planning.ai.resilience.AiRequestContext.Stage.class), anyString(), anyString(), anyMap())).thenReturn(mapper.readTree("{\"days\":[{\"dayIndex\":1,\"placeRefs\":[\"P1\"]},{\"dayIndex\":2,\"placeRefs\":[\"P3\"]}]}"));
        var plan = advisor.allocate(points, Set.of(points.get(2).placeId()), intent, 2, DATE);
        assertThat(plan.source()).isEqualTo("AI"); assertThat(plan.days().get(1)).containsExactly(points.get(2).placeId());
    }

    @Test void inventedIdDuplicateAndOmittedRequiredFallBackToRealCandidates() throws Exception {
        var points = List.of(candidate(1), candidate(2));
        for (String refs : List.of("\"UNKNOWN\"", "\"P1\",\"P1\"", "\"P1\"")) {
            when(ai.generate(any(com.travel.planning.ai.resilience.AiRequestContext.Stage.class), anyString(), anyString(), anyMap())).thenReturn(mapper.readTree("{\"days\":[{\"dayIndex\":1,\"placeRefs\":["+refs+"]},{\"dayIndex\":2,\"placeRefs\":[]}]}"));
            var result = advisor.allocate(points, Set.of(points.get(1).placeId()), intent, 2, DATE);
            assertThat(result.source()).isEqualTo("RULE_BASED_FALLBACK");
            assertThat(result.reason()).isEqualTo("INVALID_RESPONSE");
            assertThat(result.days().stream().flatMap(Collection::stream).toList()).contains(points.get(1).placeId()).doesNotHaveDuplicates();
        }
    }

    @Test void modelOutageDoesNotCreateMockPlaces() {
        when(ai.generate(any(com.travel.planning.ai.resilience.AiRequestContext.Stage.class), anyString(), anyString(), anyMap())).thenThrow(new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE));
        var points = List.of(candidate(1), candidate(2));
        assertThat(advisor.allocate(points, Set.of(), intent, 2, DATE).days().stream().flatMap(Collection::stream).toList())
                .containsExactlyInAnyOrder(points.get(0).placeId(), points.get(1).placeId());
    }

    @Test void fallbackDoesNotFillDaysWithUnrelatedOptionalCandidates() {
        when(ai.generate(any(com.travel.planning.ai.resilience.AiRequestContext.Stage.class), anyString(), anyString(), anyMap())).thenThrow(new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE));
        var points = List.of(candidate(1), candidate(2));
        for (var interests : List.of(List.of("BEACH"), List.<String>of())) {
            var request = new TravelIntent("Đà Nẵng", 2, interests, List.of(), "08:00", "17:00", null, List.of(), List.of(), Map.of());
            assertThat(advisor.allocate(points, Set.of(points.getFirst().placeId()), request, 2, DATE).days().stream().flatMap(Collection::stream).toList())
                    .containsExactly(points.getFirst().placeId());
        }
    }

    @Test void dayThemeUsesOnlyFinalScheduledPlacesAndRejectsForeignReferences() throws Exception {
        var point = candidate(1); var planner = new DraftTimelinePlanner(new HeldKarpSolver(new HaversineDistanceMatrixService()), new TimelineEngine(), mapper);
        var day = planner.build(1, DATE, LocalTime.of(8,0), LocalTime.of(17,0), ORIGIN, true, 0, 0, List.of(point), entities(List.of(point)));
        when(ai.generate(any(com.travel.planning.ai.resilience.AiRequestContext.Stage.class), anyString(), anyString(), anyMap())).thenReturn(mapper.readTree("{\"days\":[{\"dayIndex\":1,\"theme\":\"CULTURE_ART\",\"highlightRefs\":[\"P1\"]}]}"));
        var narrated = advisor.narrate(List.of(day)).getFirst();
        assertThat(narrated.summarySource()).isEqualTo("AI_GROUNDED_TEMPLATE"); assertThat(narrated.summary()).contains(point.name());
        when(ai.generate(any(com.travel.planning.ai.resilience.AiRequestContext.Stage.class), anyString(), anyString(), anyMap())).thenReturn(mapper.readTree("{\"days\":[{\"dayIndex\":1,\"theme\":\"SEA_SCENERY\",\"highlightRefs\":[\"P999\"]}]}"));
        var rejected = advisor.narrateWithProvenance(List.of(day));
        assertThat(rejected.days().getFirst().summarySource()).isEqualTo("RULE_BASED_FALLBACK");
        assertThat(rejected.fallbackReason()).isEqualTo("INVALID_RESPONSE");
    }
}
