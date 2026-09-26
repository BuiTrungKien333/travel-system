package com.travel.planning.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.travel.place.enums.PlaceType;
import com.travel.planning.ai.copilot.enums.CopilotActionType;
import com.travel.planning.dto.request.ApplyRecommendationRequest;
import com.travel.planning.dto.request.OptimizeAndAuditRequest;
import com.travel.planning.dto.request.TspLocationRequest;
import com.travel.planning.dto.response.TripOptimizationResponse;
import com.travel.planning.rule.model.ItineraryEvaluationResult;
import com.travel.planning.rule.model.ScheduleItemContext;
import com.travel.planning.service.TripOptimizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ItineraryOptimizationControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private TripOptimizationService tripOptimizationService;

    @InjectMocks
    private ItineraryOptimizationController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("POST /api/v1/public/planning/itinerary/optimize-and-audit should return 200 and optimization response")
    void optimizeAndAudit_validRequest_returns200() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        OptimizeAndAuditRequest request = new OptimizeAndAuditRequest(
                "Đà Nẵng",
                LocalDate.of(2026, 10, 1),
                LocalTime.of(8, 0),
                null,
                false,
                List.of(
                        new TspLocationRequest(id1, "Cầu Rồng", 16.06, 108.22),
                        new TspLocationRequest(id2, "Bãi biển Mỹ Khê", 16.07, 108.24)
                ),
                null
        );

        TripOptimizationResponse mockResponse = new TripOptimizationResponse(
                "ALL_CLEAR", 5.2, 18, List.of(), ItineraryEvaluationResult.of(List.of()), null
        );

        when(tripOptimizationService.optimizeAndAuditItinerary(any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/public/planning/itinerary/optimize-and-audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ALL_CLEAR"))
                .andExpect(jsonPath("$.data.totalDistanceKm").value(5.2));
    }

    @Test
    @DisplayName("POST /api/v1/public/planning/itinerary/apply-recommendation should return 200 and updated response")
    void applyRecommendation_validRequest_returns200() throws Exception {
        ScheduleItemContext item = new ScheduleItemContext(
                UUID.randomUUID(), "Cầu Rồng", 16.06, 108.22, PlaceType.INDOOR,
                null, null, LocalDate.of(2026, 10, 1).atTime(8, 0), 60, 0.0, 0
        );
        ApplyRecommendationRequest request = new ApplyRecommendationRequest(
                "card-123",
                CopilotActionType.DISMISS,
                Map.of(),
                List.of(item)
        );

        TripOptimizationResponse mockResponse = new TripOptimizationResponse(
                "ALL_CLEAR", 5.2, 18, List.of(), ItineraryEvaluationResult.of(List.of()), null
        );

        when(tripOptimizationService.applyCopilotRecommendation(any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/public/planning/itinerary/apply-recommendation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ALL_CLEAR"));
    }
}
