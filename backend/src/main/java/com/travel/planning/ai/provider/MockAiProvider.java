package com.travel.planning.ai.provider;

import com.travel.planning.ai.context.model.AiItineraryPromptContext;
import com.travel.planning.ai.dto.response.AiItineraryResponse;
import com.travel.planning.ai.fallback.FallbackItineraryGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("mockAiProvider")
@RequiredArgsConstructor
@Slf4j
public class MockAiProvider implements AiProvider {

    private final FallbackItineraryGenerator fallbackGenerator;

    @Override
    public AiItineraryResponse generateItinerary(AiItineraryPromptContext context) {
        log.info("[MOCK_AI] Generating mock itinerary for destination={}", context.destination());
        AiItineraryResponse fallback = fallbackGenerator.generateFallback(context);

        return new AiItineraryResponse(
                "[MOCK] " + fallback.tripTitle(),
                "[Mô phỏng AI Offline] " + fallback.tripSummary(),
                fallback.dailyPlans(),
                fallback.generalTips(),
                fallback.estimatedBudgetNote()
        );
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getProviderName() {
        return "mock";
    }
}
