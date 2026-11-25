package com.travel.planning.draft;

import static com.travel.planning.draft.DraftFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.*;
import com.travel.maps.service.MapboxDirectionsService;
import com.travel.place.repository.PlaceRepository;
import com.travel.planning.ai.intent.dto.response.IntentAnalysisResponse;
import com.travel.planning.ai.intent.dto.response.IntentSearchResponse;
import com.travel.planning.ai.intent.model.TravelIntent;
import com.travel.planning.ai.intent.service.TravelIntentService;
import com.travel.planning.ai.provider.GeminiStructuredClient;
import com.travel.planning.ai.resilience.AiRequestContext;
import com.travel.planning.algorithm.*;
import com.travel.planning.draft.dto.request.GenerateDraftRequest;
import com.travel.planning.draft.dto.response.DraftResponse;
import com.travel.planning.draft.model.DraftLimits;
import com.travel.planning.draft.provider.GeminiDraftClient;
import com.travel.planning.draft.service.*;
import com.travel.planning.draft.service.DraftService;
import com.travel.planning.draft.service.DraftTimelinePlanner;
import com.travel.planning.retrieval.config.RagProperties;
import com.travel.planning.retrieval.dto.response.PlaceSearchResponse;
import com.travel.planning.retrieval.model.PlaceCandidate;
import com.travel.planning.retrieval.service.PlaceRetrievalService;
import com.travel.planning.timeline.TimelineEngine;
import com.travel.weather.model.*;
import com.travel.weather.service.WeatherService;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DraftServiceTest {

    final TravelIntentService intents = mock(TravelIntentService.class);
    final PlaceRetrievalService retrieval = mock(PlaceRetrievalService.class);
    final PlaceRepository repository = mock(PlaceRepository.class);
    final GeminiStructuredClient ai = mock(GeminiStructuredClient.class);
    final WeatherService weather = mock(WeatherService.class);
    final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    final ExecutorService weatherExecutor = Executors.newSingleThreadExecutor();
    final DraftService service;
    final List<PlaceCandidate> points = List.of(candidate(1), candidate(2), candidate(3));

    DraftServiceTest() throws Exception {
        var advisor = new DraftAllocationService(new GeminiDraftClient(ai, mapper));
        var planner = new DraftTimelinePlanner(
            new HeldKarpSolver(new HaversineDistanceMatrixService()),
            new TimelineEngine(),
            mapper
        );
        var properties = new RagProperties();
        var maps = mock(MapboxDirectionsService.class);
        when(maps.matrix(anyList(), any())).thenThrow(new AppException(ErrorCode.PLACE_PROVIDER_UNAVAILABLE));
        service = new DraftService(
            intents,
            retrieval,
            repository,
            advisor,
            new DraftSchedulingService(planner, maps),
            new DraftThemeService(),
            new DraftWeatherService(weather, weatherExecutor, properties),
            properties,
            new AiRequestContext(properties)
        );
        when(
            ai.generate(
                any(com.travel.planning.ai.resilience.AiRequestContext.Stage.class),
                anyString(),
                anyString(),
                anyMap()
            )
        ).thenThrow(new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE));
        when(retrieval.catalog("48")).thenReturn(points);
        when(retrieval.catalog()).thenReturn(points);
        when(repository.findAllByIdIn(anyCollection())).thenReturn(new ArrayList<>(entities(points).values()));
    }

    @AfterEach
    void closeExecutor() {
        weatherExecutor.shutdownNow();
    }

    private void intent(int days, String end, List<PlaceCandidate> required) {
        var intent = new TravelIntent(
            "Đà Nẵng",
            days,
            List.of("CULTURE"),
            List.of(),
            "08:00",
            end,
            "BALANCED",
            List.of(),
            List.of(),
            Map.of()
        );
        var analysis = new IntentAnalysisResponse(
            "READY_FOR_RETRIEVAL",
            intent,
            "48",
            List.of(),
            List.of(),
            List.of(),
            "văn hóa",
            required,
            List.of(),
            "fixture"
        );
        var candidates = new PlaceSearchResponse(
            "HYBRID",
            "fixture",
            points
                .stream()
                .map(p -> new PlaceSearchResponse.Match(p, .02, true, true))
                .toList()
        );
        when(intents.analyze("demo")).thenReturn(analysis);
        when(intents.search(analysis, DraftLimits.CANDIDATE_LIMIT)).thenReturn(
            new IntentSearchResponse("CANDIDATES_FOUND", analysis, candidates)
        );
    }

    private GenerateDraftRequest request() {
        return new GenerateDraftRequest("demo", DATE, ORIGIN, true, 45);
    }

    @Test
    void boundedRepairDropsOnlyOptionalPointsAndRetainsRequired() {
        intent(1, "10:00", List.of(points.getFirst()));
        var result = service.generate(request());
        assertThat(result.status()).isEqualTo("READY_WITH_WARNINGS");
        assertThat(result.days().getFirst().stops())
            .extracting(s -> s.place().placeId())
            .contains(points.getFirst().placeId());
        assertThat(result.unassignedPlaces())
            .hasSize(2)
            .allMatch(p -> p.reason().equals("REMOVED_TO_RESOLVE_CONFLICT"));
        assertThat(Integer.parseInt(result.metadata().get("repairPasses"))).isLessThanOrEqualTo(2);
        verify(repository, never()).save(any());
    }

    @Test
    void requiredConflictsStayVisibleRatherThanBeingSilentlyDeleted() {
        intent(2, "10:00", points);
        var result = service.generate(request());
        assertThat(result.status()).isEqualTo("CONFLICTS_DETECTED");
        assertThat(
            result
                .days()
                .stream()
                .flatMap(d -> d.stops().stream())
                .map(s -> s.place().placeId())
                .toList()
        )
            .hasSize(3)
            .doesNotHaveDuplicates();
        assertThat(result.issues()).anyMatch(DraftResponse.Problem::blocking);
    }

    @Test
    void mockWeatherIsNotPresentedAsRealForecast() {
        intent(1, "17:00", List.of());
        when(weather.getForecast(anyDouble(), anyDouble(), any(), any())).thenReturn(
            new WeatherForecastResult(16, 108, "Asia/Ho_Chi_Minh", List.of(), "mock")
        );
        var result = service.generate(request());
        assertThat(result.weather().status()).isEqualTo("UNKNOWN");
        assertThat(result.weather().days()).isEmpty();
    }

    @Test
    void staleRequiredPlaceIsRejectedBeforeAllocation() {
        intent(1, "17:00", List.of(points.getFirst()));
        when(retrieval.catalog()).thenReturn(points.subList(1, 3));
        var result = service.generate(request());
        assertThat(result.status()).isEqualTo("NEEDS_INPUT");
        assertThat(result.days()).isEmpty();
        assertThat(result.issues()).extracting(DraftResponse.Problem::code).contains("STALE_REQUIRED_PLACE");
        verifyNoInteractions(ai);
    }

    @Test
    void emptyFallbackIsNotReportedAsReady() {
        var intent = new TravelIntent(
            "Đà Nẵng",
            1,
            List.of("BEACH"),
            List.of(),
            "08:00",
            "17:00",
            null,
            List.of(),
            List.of(),
            Map.of()
        );
        var analysis = new IntentAnalysisResponse(
            "READY_FOR_RETRIEVAL",
            intent,
            "48",
            List.of(),
            List.of(),
            List.of(),
            "biển",
            List.of(),
            List.of(),
            "fixture"
        );
        var candidates = new PlaceSearchResponse(
            "HYBRID",
            "fixture",
            points
                .stream()
                .map(p -> new PlaceSearchResponse.Match(p, .02, true, true))
                .toList()
        );
        when(intents.analyze("demo")).thenReturn(analysis);
        when(intents.search(analysis, DraftLimits.CANDIDATE_LIMIT)).thenReturn(
            new IntentSearchResponse("CANDIDATES_FOUND", analysis, candidates)
        );
        var result = service.generate(request());
        assertThat(result.status()).isEqualTo("NO_CANDIDATES");
        assertThat(result.days()).isEmpty();
        assertThat(result.issues()).extracting(DraftResponse.Problem::code).contains("NO_SCHEDULED_PLACES");
    }

    @Test
    void weatherRunsAlongsideAllocationAndThemesReuseItsSingleGeneration() throws Exception {
        intent(1, "17:00", List.of());
        var weatherStarted = new CountDownLatch(1);
        var allocationStarted = new CountDownLatch(1);
        when(weather.getForecast(anyDouble(), anyDouble(), any(), any())).thenAnswer(invocation -> {
            weatherStarted.countDown();
            assertThat(allocationStarted.await(2, TimeUnit.SECONDS)).isTrue();
            return null;
        });
        when(
            ai.generate(
                eq(com.travel.planning.ai.resilience.AiRequestContext.Stage.ALLOCATION),
                anyString(),
                anyString(),
                anyMap()
            )
        ).thenAnswer(invocation -> {
            allocationStarted.countDown();
            assertThat(weatherStarted.await(2, TimeUnit.SECONDS)).isTrue();
            return mapper.readTree(
                "{\"days\":[{\"dayIndex\":1,\"placeRefs\":[\"P1\"],\"theme\":\"CULTURE_ART\",\"highlightRefs\":[\"P1\"]}]}"
            );
        });
        var result = service.generate(request());
        assertThat(result.status()).isEqualTo("READY_WITH_WARNINGS");
        assertThat(result.days().getFirst().summarySource()).isEqualTo("AI_GROUNDED_TEMPLATE");
        assertThat(result.executionReport().stages().get(4).attempts()).isZero();
        verify(ai, times(1)).generate(
            any(com.travel.planning.ai.resilience.AiRequestContext.Stage.class),
            anyString(),
            anyString(),
            anyMap()
        );
    }

    @Test
    void missingWindowDoesNotSpendAnEmbeddingOrStartWeather() {
        var intent = new TravelIntent(
            "Đà Nẵng",
            null,
            List.of("ART"),
            List.of(),
            null,
            null,
            null,
            List.of(),
            List.of(),
            Map.of()
        );
        var analysis = new IntentAnalysisResponse(
            "READY_FOR_RETRIEVAL",
            intent,
            "48",
            List.of("numberOfDays"),
            List.of(),
            List.of(),
            "nghệ thuật",
            List.of(),
            List.of(),
            "fixture"
        );
        when(intents.analyze("demo")).thenReturn(analysis);
        var result = service.generate(request());
        assertThat(result.status()).isEqualTo("NEEDS_INPUT");
        assertThat(result.executionReport().stages().get(1).source()).isEqualTo("SKIPPED");
        verify(intents, never()).search(any(IntentAnalysisResponse.class), anyInt());
        verifyNoInteractions(retrieval, repository, ai, weather);
    }
}
