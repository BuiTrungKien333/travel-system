package com.travel.planning.service;

import com.travel.place.entity.Place;
import com.travel.place.enums.PlaceType;
import com.travel.place.repository.PlaceRepository;
import com.travel.planning.ai.copilot.enums.CopilotActionType;
import com.travel.planning.ai.copilot.model.AiCopilotAnalysisResult;
import com.travel.planning.ai.copilot.model.AiRecommendationCard;
import com.travel.planning.ai.provider.AiProvider;
import com.travel.planning.ai.provider.AiProviderFactory;
import com.travel.planning.algorithm.HeldKarpSolver;
import com.travel.planning.algorithm.model.RouteLeg;
import com.travel.planning.algorithm.model.TspRequest;
import com.travel.planning.algorithm.model.TspResult;
import com.travel.planning.dto.request.ApplyRecommendationRequest;
import com.travel.planning.dto.request.OptimizeAndAuditRequest;
import com.travel.planning.dto.request.TspLocationRequest;
import com.travel.planning.dto.response.TripOptimizationResponse;
import com.travel.planning.rule.enums.IssueCategory;
import com.travel.planning.rule.enums.IssueSeverity;
import com.travel.planning.rule.model.DetectedIssue;
import com.travel.planning.rule.model.ItineraryEvaluationResult;
import com.travel.planning.rule.model.ScheduleItemContext;
import com.travel.planning.rule.service.RuleEvaluatorService;
import com.travel.weather.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripOptimizationServiceTest {

    @Mock
    private HeldKarpSolver heldKarpSolver;

    @Mock
    private WeatherService weatherService;

    @Mock
    private RuleEvaluatorService ruleEvaluatorService;

    @Mock
    private AiProviderFactory aiProviderFactory;

    @Mock
    private AiProvider aiProvider;

    @Mock
    private PlaceRepository placeRepository;

    private TripOptimizationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TripOptimizationServiceImpl(
                heldKarpSolver, weatherService, ruleEvaluatorService, aiProviderFactory, placeRepository
        );
    }

    @Test
    @DisplayName("optimizeAndAuditItinerary when itinerary is ALL CLEAR should remain SILENT (0 AI tokens used)")
    void optimizeAndAudit_allClear_aiRemainsSilent() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        Place p1 = Place.builder().id(id1).name("Điểm 1").latitude(16.0).longitude(108.0).placeType(PlaceType.INDOOR).build();
        Place p2 = Place.builder().id(id2).name("Điểm 2").latitude(16.1).longitude(108.1).placeType(PlaceType.INDOOR).build();

        RouteLeg leg = RouteLeg.builder().fromPlace(p1).toPlace(p2).distanceMeters(5000).durationSeconds(600).build();
        TspResult tspResult = TspResult.builder()
                .orderedPlaces(List.of(p1, p2))
                .legs(List.of(leg))
                .totalDistanceMeters(5000)
                .totalDurationSeconds(600)
                .build();

        when(heldKarpSolver.solve(any(TspRequest.class))).thenReturn(tspResult);
        when(ruleEvaluatorService.evaluateItinerary(anyList())).thenReturn(ItineraryEvaluationResult.of(List.of()));

        OptimizeAndAuditRequest request = new OptimizeAndAuditRequest(
                "Đà Nẵng", LocalDate.of(2026, 10, 1), LocalTime.of(8, 0), null, false,
                List.of(
                        new TspLocationRequest(id1, "Điểm 1", 16.0, 108.0),
                        new TspLocationRequest(id2, "Điểm 2", 16.1, 108.1)
                ),
                null
        );

        TripOptimizationResponse response = service.optimizeAndAuditItinerary(request);

        assertThat(response.status()).isEqualTo("ALL_CLEAR");
        assertThat(response.copilot()).isNull(); // AI hoàn toàn im lặng
        assertThat(response.schedule()).hasSize(2);
        assertThat(response.totalDistanceKm()).isEqualTo(5.0);

        // Đảm bảo AI Provider KHÔNG HỀ ĐƯỢC GỌI
        verifyNoInteractions(aiProviderFactory);
    }

    @Test
    @DisplayName("optimizeAndAuditItinerary when conflicts exist should trigger AI Copilot critique")
    void optimizeAndAudit_conflictsDetected_triggersAiCopilot() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        Place p1 = Place.builder().id(id1).name("Bảo tàng").latitude(16.0).longitude(108.0).placeType(PlaceType.INDOOR).build();
        Place p2 = Place.builder().id(id2).name("Bãi biển").latitude(16.1).longitude(108.1).placeType(PlaceType.OUTDOOR).build();

        RouteLeg leg = RouteLeg.builder().fromPlace(p1).toPlace(p2).distanceMeters(5000).durationSeconds(600).build();
        TspResult tspResult = TspResult.builder()
                .orderedPlaces(List.of(p1, p2))
                .legs(List.of(leg))
                .totalDistanceMeters(5000)
                .totalDurationSeconds(600)
                .build();

        when(heldKarpSolver.solve(any(TspRequest.class))).thenReturn(tspResult);

        DetectedIssue issue = new DetectedIssue(
                id2, "Bãi biển", LocalDateTime.of(2026, 10, 1, 14, 0),
                IssueCategory.HAZARDOUS_WEATHER, IssueSeverity.CRITICAL,
                "Dông bão ngoài trời", "Đổi sang điểm trong nhà", Map.of()
        );
        when(ruleEvaluatorService.evaluateItinerary(anyList())).thenReturn(ItineraryEvaluationResult.of(List.of(issue)));

        when(aiProviderFactory.getProvider()).thenReturn(aiProvider);
        AiRecommendationCard card = new AiRecommendationCard(
                "card-1", IssueSeverity.CRITICAL, "Dông bão tại bãi biển",
                "Nên đổi sang bảo tàng", CopilotActionType.REPLACE_PLACE, "Đổi sang điểm trong nhà", Map.of()
        );
        AiCopilotAnalysisResult copilotResult = new AiCopilotAnalysisResult(
                "Chào bạn! Hôm nay trời bão...", List.of(card), "gemini-2.0-flash"
        );
        when(aiProvider.analyzeAndRecommend(any())).thenReturn(copilotResult);

        OptimizeAndAuditRequest request = new OptimizeAndAuditRequest(
                "Đà Nẵng", LocalDate.of(2026, 10, 1), LocalTime.of(8, 0), null, false,
                List.of(
                        new TspLocationRequest(id1, "Bảo tàng", 16.0, 108.0),
                        new TspLocationRequest(id2, "Bãi biển", 16.1, 108.1)
                ),
                null
        );

        TripOptimizationResponse response = service.optimizeAndAuditItinerary(request);

        assertThat(response.status()).isEqualTo("CONFLICTS_DETECTED");
        assertThat(response.copilot()).isNotNull();
        assertThat(response.copilot().recommendationCards()).hasSize(1);
        assertThat(response.copilot().recommendationCards().getFirst().title()).isEqualTo("Dông bão tại bãi biển");

        verify(aiProvider).analyzeAndRecommend(any());
    }

    @Test
    @DisplayName("applyCopilotRecommendation with SWAP_PLACES should swap schedule order and re-evaluate")
    void applyCopilotRecommendation_swapPlaces_updatesSchedule() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        LocalDateTime t1 = LocalDateTime.of(2026, 10, 1, 8, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 10, 1, 9, 30);

        ScheduleItemContext item1 = new ScheduleItemContext(
                id1, "Điểm 1", 16.0, 108.0, PlaceType.INDOOR, null, null, t1, 60, 0, 0
        );
        ScheduleItemContext item2 = new ScheduleItemContext(
                id2, "Điểm 2", 16.1, 108.1, PlaceType.INDOOR, null, null, t2, 60, 5.0, 15
        );

        when(ruleEvaluatorService.evaluateItinerary(anyList())).thenReturn(ItineraryEvaluationResult.of(List.of()));

        ApplyRecommendationRequest request = new ApplyRecommendationRequest(
                "card-123",
                CopilotActionType.SWAP_PLACES,
                Map.of("targetPlaceId", id1.toString(), "swapWithPlaceId", id2.toString()),
                List.of(item1, item2)
        );

        TripOptimizationResponse response = service.applyCopilotRecommendation(request);

        assertThat(response.status()).isEqualTo("ALL_CLEAR");
        assertThat(response.schedule()).hasSize(2);
        // Kiểm tra thứ tự đã bị đảo ngược
        assertThat(response.schedule().get(0).placeId()).isEqualTo(id2);
        assertThat(response.schedule().get(1).placeId()).isEqualTo(id1);
    }
}
