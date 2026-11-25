package com.travel.planning.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.GlobalExceptionHandler;
import com.travel.common.util.Translator;
import com.travel.place.entity.Place;
import com.travel.planning.algorithm.HeldKarpSolver;
import com.travel.planning.algorithm.model.RouteLeg;
import com.travel.planning.algorithm.model.TspRequest;
import com.travel.planning.algorithm.model.TspResult;
import com.travel.planning.dto.request.OptimizeRouteRequest;
import com.travel.planning.dto.request.TspLocationRequest;
import com.travel.planning.service.TspOptimizationService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TspOptimizationControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private HeldKarpSolver heldKarpSolver;

    @Mock
    private Translator translator;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
            new TspOptimizationController(new TspOptimizationService(heldKarpSolver))
        )
            .setControllerAdvice(new GlobalExceptionHandler(translator))
            .build();
    }

    @Test
    @DisplayName("POST /api/v1/public/planning/tsp/optimize with valid locations should return 200 and optimized route")
    void optimizeRoute_validRequest_returns200() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Place p1 = Place.builder().id(id1).name("Place 1").latitude(16.0).longitude(108.0).build();
        Place p2 = Place.builder().id(id2).name("Place 2").latitude(16.1).longitude(108.1).build();

        RouteLeg leg1 = RouteLeg.builder()
            .fromPlace(p1)
            .toPlace(p2)
            .distanceMeters(1000.0)
            .durationSeconds(120)
            .build();
        RouteLeg leg2 = RouteLeg.builder()
            .fromPlace(p2)
            .toPlace(p1)
            .distanceMeters(1000.0)
            .durationSeconds(120)
            .build();

        TspResult mockResult = TspResult.builder()
            .orderedPlaces(List.of(p1, p2, p1))
            .legs(List.of(leg1, leg2))
            .totalDistanceMeters(2000.0)
            .totalDurationSeconds(240)
            .roundTrip(true)
            .build();

        when(heldKarpSolver.solve(any(TspRequest.class))).thenReturn(mockResult);

        OptimizeRouteRequest request = new OptimizeRouteRequest(
            List.of(
                new TspLocationRequest(id1, "Place 1", 16.0, 108.0),
                new TspLocationRequest(id2, "Place 2", 16.1, 108.1)
            ),
            true,
            id1,
            null,
            null,
            null
        );

        mockMvc
            .perform(
                post("/api/v1/public/planning/tsp/optimize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.orderedPlaces").isArray())
            .andExpect(jsonPath("$.data.orderedPlaces.length()").value(3))
            .andExpect(jsonPath("$.data.legs.length()").value(2))
            .andExpect(jsonPath("$.data.totalDistanceMeters").value(2000.0))
            .andExpect(jsonPath("$.data.totalDistanceKilometers").value(2.0))
            .andExpect(jsonPath("$.data.roundTrip").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/public/planning/tsp/optimize with empty locations should return 400 Validation Error")
    void optimizeRoute_emptyLocations_returns400() throws Exception {
        when(translator.toLocale("error.validation.fail")).thenReturn("Validation failed");

        String invalidJson = """
        {
            "locations": []
        }
        """;

        mockMvc
            .perform(
                post("/api/v1/public/planning/tsp/optimize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("COMMON_003"));
    }
}
