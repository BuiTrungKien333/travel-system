package com.travel.planning.draft;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.*;
import com.travel.place.repository.PlaceRepository;
import com.travel.planning.ai.intent.*;
import com.travel.planning.ai.provider.GeminiStructuredClient;
import com.travel.planning.algorithm.*;
import com.travel.planning.retrieval.*;
import com.travel.planning.timeline.TimelineEngine;
import com.travel.weather.model.*;
import com.travel.weather.service.WeatherService;
import org.junit.jupiter.api.Test;
import java.util.*;
import static com.travel.planning.draft.DraftFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DraftServiceTest {
    final TravelIntentService intents = mock(TravelIntentService.class);
    final PlaceRetrievalService retrieval = mock(PlaceRetrievalService.class);
    final PlaceRepository repository = mock(PlaceRepository.class);
    final GeminiStructuredClient ai = mock(GeminiStructuredClient.class);
    final WeatherService weather = mock(WeatherService.class);
    final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    final DraftService service;
    final List<PlaceCandidate> points = List.of(candidate(1), candidate(2), candidate(3));
    DraftServiceTest() throws Exception {
        var advisor = new DraftAiAdvisor(ai, mapper);
        var planner = new DraftTimelinePlanner(new HeldKarpSolver(new HaversineDistanceMatrixService()), new TimelineEngine(), mapper);
        service = new DraftService(intents, retrieval, repository, advisor, planner, weather, new RagDemoProperties());
        when(ai.generate(any(com.travel.planning.ai.resilience.AiRequestContext.Stage.class), anyString(), anyString(), anyMap())).thenThrow(new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE));
        when(retrieval.catalog()).thenReturn(points);
        when(repository.findAllByIdIn(anyCollection())).thenReturn(new ArrayList<>(entities(points).values()));
    }
    private void intent(int days, String end, List<PlaceCandidate> required) {
        var intent = new TravelIntent("Đà Nẵng", days, List.of("CULTURE"), List.of(), "08:00", end, "BALANCED", List.of(), List.of(), Map.of());
        var analysis = new TravelIntentService.IntentAnalysis("READY_FOR_RETRIEVAL", intent, "48", List.of(), List.of(), List.of(), "văn hóa", required, List.of(), "fixture");
        var candidates = new PlaceRetrievalService.SearchResult("HYBRID", "fixture", points.stream().map(p -> new PlaceRetrievalService.Match(p, .02, true, true)).toList());
        when(intents.search("demo", 8)).thenReturn(new TravelIntentService.TextSearchResult("CANDIDATES_FOUND", analysis, candidates));
    }
    private DraftController.Request request() { return new DraftController.Request("demo", DATE, ORIGIN, true, 45); }

    @Test void boundedRepairDropsOnlyOptionalPointsAndRetainsRequired() {
        intent(1, "10:00", List.of(points.getFirst()));
        var result = service.generate(request());
        assertThat(result.status()).isEqualTo("READY_WITH_WARNINGS");
        assertThat(result.days().getFirst().stops()).extracting(s -> s.place().placeId()).contains(points.getFirst().placeId());
        assertThat(result.unassignedPlaces()).hasSize(2).allMatch(p -> p.reason().equals("REMOVED_TO_RESOLVE_CONFLICT"));
        assertThat(Integer.parseInt(result.metadata().get("repairPasses"))).isLessThanOrEqualTo(2);
        verify(repository, never()).save(any());
    }

    @Test void requiredConflictsStayVisibleRatherThanBeingSilentlyDeleted() {
        intent(2, "10:00", points);
        var result = service.generate(request());
        assertThat(result.status()).isEqualTo("CONFLICTS_DETECTED");
        assertThat(result.days().stream().flatMap(d -> d.stops().stream()).map(s -> s.place().placeId()).toList()).hasSize(3).doesNotHaveDuplicates();
        assertThat(result.issues()).anyMatch(DraftModels.Problem::blocking);
    }

    @Test void mockWeatherIsNotPresentedAsRealForecast() {
        intent(1, "17:00", List.of());
        when(weather.getForecast(anyDouble(), anyDouble(), any(), any()))
                .thenReturn(new WeatherForecastResult(16, 108, "Asia/Ho_Chi_Minh", List.of(), "mock"));
        var result = service.generate(request());
        assertThat(result.weather().status()).isEqualTo("UNKNOWN"); assertThat(result.weather().days()).isEmpty();
    }

    @Test void staleRequiredPlaceIsRejectedBeforeAllocation() {
        intent(1, "17:00", List.of(points.getFirst()));
        when(retrieval.catalog()).thenReturn(points.subList(1,3));
        var result = service.generate(request());
        assertThat(result.status()).isEqualTo("NEEDS_INPUT"); assertThat(result.days()).isEmpty();
        assertThat(result.issues()).extracting(DraftModels.Problem::code).contains("STALE_REQUIRED_PLACE");
        verifyNoInteractions(ai);
    }

    @Test void emptyFallbackIsNotReportedAsReady() {
        var intent = new TravelIntent("Đà Nẵng", 1, List.of("BEACH"), List.of(), "08:00", "17:00", null, List.of(), List.of(), Map.of());
        var analysis = new TravelIntentService.IntentAnalysis("READY_FOR_RETRIEVAL", intent, "48", List.of(), List.of(), List.of(), "biển", List.of(), List.of(), "fixture");
        var candidates = new PlaceRetrievalService.SearchResult("HYBRID", "fixture", points.stream().map(p -> new PlaceRetrievalService.Match(p, .02, true, true)).toList());
        when(intents.search("demo", 8)).thenReturn(new TravelIntentService.TextSearchResult("CANDIDATES_FOUND", analysis, candidates));
        var result = service.generate(request());
        assertThat(result.status()).isEqualTo("NO_CANDIDATES"); assertThat(result.days()).isEmpty();
        assertThat(result.issues()).extracting(DraftModels.Problem::code).contains("NO_SCHEDULED_PLACES");
    }
}
