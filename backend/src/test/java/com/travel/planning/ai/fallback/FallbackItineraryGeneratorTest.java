package com.travel.planning.ai.fallback;

import com.travel.planning.ai.context.model.AiItineraryPromptContext;
import com.travel.planning.ai.context.model.PlaceContext;
import com.travel.planning.ai.dto.response.AiActivitySlotResponse;
import com.travel.planning.ai.dto.response.AiItineraryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackItineraryGeneratorTest {

    private FallbackItineraryGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new FallbackItineraryGenerator();
    }

    @Test
    @DisplayName("generateFallback with empty places should return empty activities itinerary")
    void generateFallback_emptyPlaces_returnsSafePlan() {
        AiItineraryPromptContext context = new AiItineraryPromptContext(
                "Đà Nẵng",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 1),
                1,
                null,
                List.of(),
                List.of(),
                0.0,
                0
        );

        AiItineraryResponse response = generator.generateFallback(context);

        assertThat(response).isNotNull();
        assertThat(response.dailyPlans()).hasSize(1);
        assertThat(response.dailyPlans().getFirst().activities()).isEmpty();
    }

    @Test
    @DisplayName("generateFallback with 5 places and 2 days should distribute places evenly (3 and 2)")
    void generateFallback_fivePlacesTwoDays_distributesCorrectly() {
        List<PlaceContext> places = List.of(
                new PlaceContext(UUID.randomUUID(), "P1", "Address 1", 16.0, 108.0, 60, null),
                new PlaceContext(UUID.randomUUID(), "P2", "Address 2", 16.1, 108.1, 60, null),
                new PlaceContext(UUID.randomUUID(), "P3", "Address 3", 16.2, 108.2, 60, null),
                new PlaceContext(UUID.randomUUID(), "P4", "Address 4", 16.3, 108.3, 60, null),
                new PlaceContext(UUID.randomUUID(), "P5", "Address 5", 16.4, 108.4, 60, null)
        );

        AiItineraryPromptContext context = new AiItineraryPromptContext(
                "Đà Nẵng",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 2),
                2,
                null,
                places,
                List.of(),
                50.0,
                100
        );

        AiItineraryResponse response = generator.generateFallback(context);

        assertThat(response.dailyPlans()).hasSize(2);
        assertThat(response.dailyPlans().get(0).activities()).hasSize(3);
        assertThat(response.dailyPlans().get(1).activities()).hasSize(2);

        // Kiểm tra tất cả 5 địa điểm đều có mặt theo đúng thứ tự
        List<String> visitedNames = response.dailyPlans().stream()
                .flatMap(d -> d.activities().stream())
                .map(AiActivitySlotResponse::placeName)
                .toList();

        assertThat(visitedNames).containsExactly("P1", "P2", "P3", "P4", "P5");
    }
}
