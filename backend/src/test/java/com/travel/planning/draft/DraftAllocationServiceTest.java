package com.travel.planning.draft;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.planning.ai.intent.model.TravelIntent;
import com.travel.planning.ai.provider.GeminiStructuredClient;
import com.travel.planning.algorithm.*;
import com.travel.planning.draft.provider.GeminiDraftClient;
import com.travel.planning.draft.service.DraftAllocationService;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

import static com.travel.planning.draft.DraftFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DraftAllocationServiceTest {
    final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    final GeminiStructuredClient ai = mock(GeminiStructuredClient.class);
    final DraftAllocationService advisor = new DraftAllocationService(new GeminiDraftClient(ai, mapper));
    DraftAllocationServiceTest() throws Exception {}
    final TravelIntent intent = new TravelIntent("Đà Nẵng", 2, List.of("CULTURE"), List.of(), "08:00", "17:00", "RELAXED", List.of(), List.of(), Map.of());

    @Test void validAllocationCanSelectSubsetButKeepsRequired() throws Exception {
        var points = List.of(candidate(1), candidate(2), candidate(3));
        when(ai.generate(any(com.travel.planning.ai.resilience.AiRequestContext.Stage.class), anyString(), anyString(), anyMap())).thenReturn(mapper.readTree("{\"days\":[{\"dayIndex\":1,\"placeRefs\":[\"P1\"]},{\"dayIndex\":2,\"placeRefs\":[\"P3\"]}]}"));
        var plan = advisor.allocate(points, Set.of(points.get(2).placeId()), intent, DATE);
        assertThat(plan.source()).isEqualTo("AI"); assertThat(plan.days().get(1)).containsExactly(points.get(2).placeId());
    }

    @Test void inventedIdDuplicateAndOmittedRequiredFallBackToRealCandidates() throws Exception {
        var points = List.of(candidate(1), candidate(2));
        for (String refs : List.of("\"UNKNOWN\"", "\"P1\",\"P1\"", "\"P1\"")) {
            when(ai.generate(any(com.travel.planning.ai.resilience.AiRequestContext.Stage.class), anyString(), anyString(), anyMap())).thenReturn(mapper.readTree("{\"days\":[{\"dayIndex\":1,\"placeRefs\":["+refs+"]},{\"dayIndex\":2,\"placeRefs\":[]}]}"));
            var result = advisor.allocate(points, Set.of(points.get(1).placeId()), intent, DATE);
            assertThat(result.source()).isEqualTo("RULE_BASED_FALLBACK");
            assertThat(result.reason()).isEqualTo("INVALID_RESPONSE");
            assertThat(result.days().stream().flatMap(Collection::stream).toList()).contains(points.get(1).placeId()).doesNotHaveDuplicates();
        }
    }

    @Test void modelOutageDoesNotCreateMockPlaces() {
        when(ai.generate(any(com.travel.planning.ai.resilience.AiRequestContext.Stage.class), anyString(), anyString(), anyMap())).thenThrow(new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE));
        var points = List.of(candidate(1), candidate(2));
        assertThat(advisor.allocate(points, Set.of(), intent, DATE).days().stream().flatMap(Collection::stream).toList())
                .containsExactlyInAnyOrder(points.get(0).placeId(), points.get(1).placeId());
    }

    @Test void fallbackDoesNotFillDaysWithUnrelatedOptionalCandidates() {
        when(ai.generate(any(com.travel.planning.ai.resilience.AiRequestContext.Stage.class), anyString(), anyString(), anyMap())).thenThrow(new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE));
        var points = List.of(candidate(1), candidate(2));
        for (var interests : List.of(List.of("BEACH"), List.<String>of())) {
            var request = new TravelIntent("Đà Nẵng", 2, interests, List.of(), "08:00", "17:00", null, List.of(), List.of(), Map.of());
            assertThat(advisor.allocate(points, Set.of(points.getFirst().placeId()), request, DATE).days().stream().flatMap(Collection::stream).toList())
                    .containsExactly(points.getFirst().placeId());
        }
    }

    @Test void allocationIncludesValidatedThemesAndCompactGroundedContextInOneCall() throws Exception {
        var point = candidate(1);
        when(ai.generate(any(com.travel.planning.ai.resilience.AiRequestContext.Stage.class), anyString(), anyString(), anyMap()))
                .thenReturn(mapper.readTree("{\"days\":[{\"dayIndex\":1,\"placeRefs\":[\"P1\"],\"theme\":\"CULTURE_ART\",\"highlightRefs\":[\"P1\"]},{\"dayIndex\":2,\"placeRefs\":[],\"theme\":\"FREE_TIME\",\"highlightRefs\":[]}]}"));
        var result = advisor.allocate(List.of(point), Set.of(point.placeId()), intent, DATE);
        assertThat(result.themes().get(1).highlights()).containsExactly(point.placeId());
        var input = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(ai, times(1)).generate(eq(com.travel.planning.ai.resilience.AiRequestContext.Stage.ALLOCATION), anyString(), input.capture(), anyMap());
        var context = mapper.readTree(input.getValue());
        assertThat(context.path("preferences").path("interests").get(0).asText()).isEqualTo("CULTURE");
        assertThat(context.has("intent")).isFalse();
        assertThat(context.path("candidates").get(0).path("description").asText()).isEqualTo(point.description());
        assertThat(context.path("candidates").get(0).path("required").asBoolean()).isTrue();
        assertThat(context.path("candidates").get(0).has("openingTime")).isTrue();
    }
}
