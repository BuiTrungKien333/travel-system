package com.travel.planning.rule.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.travel.place.enums.PlaceType;
import com.travel.planning.rule.dto.EvaluateItineraryRequest;
import com.travel.planning.rule.model.ItineraryEvaluationResult;
import com.travel.planning.rule.model.ScheduleItemContext;
import com.travel.planning.rule.service.RuleEvaluatorService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RuleEvaluationControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private RuleEvaluatorService ruleEvaluatorService;

    @InjectMocks
    private RuleEvaluationController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("POST /api/v1/public/planning/rules/evaluate should return 200 and evaluation result")
    void evaluateItinerary_validRequest_returns200() throws Exception {
        LocalDate date = LocalDate.of(2026, 10, 1);
        ScheduleItemContext item = new ScheduleItemContext(
                UUID.randomUUID(), "Bảo tàng Chăm", 16.05, 108.22, PlaceType.INDOOR,
                LocalTime.of(8, 0), LocalTime.of(17, 0),
                date.atTime(9, 0), 60, 0.0, 0
        );

        EvaluateItineraryRequest request = new EvaluateItineraryRequest(List.of(item));
        ItineraryEvaluationResult mockResult = ItineraryEvaluationResult.of(List.of());

        when(ruleEvaluatorService.evaluateItinerary(anyList())).thenReturn(mockResult);

        mockMvc.perform(post("/api/v1/public/planning/rules/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.hasIssues").value(false))
                .andExpect(jsonPath("$.data.shouldTriggerAi").value(false));
    }
}
