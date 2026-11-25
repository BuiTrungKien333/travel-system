package com.travel.planning.ai.provider;

import com.travel.planning.ai.copilot.fallback.FallbackAiCopilotGenerator;
import com.travel.planning.ai.copilot.model.AiCopilotAnalysisResult;
import com.travel.planning.ai.copilot.model.AiCopilotPromptContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("mockAiProvider")
@RequiredArgsConstructor
@Slf4j
public class MockAiProvider implements AiProvider {

    private final FallbackAiCopilotGenerator fallbackCopilotGenerator;

    @Override
    public AiCopilotAnalysisResult analyzeAndRecommend(AiCopilotPromptContext context) {
        log.info("[MOCK_AI] Generating mock copilot analysis for destination={}", context.destination());
        AiCopilotAnalysisResult fallback = fallbackCopilotGenerator.generateFallback(context);

        return new AiCopilotAnalysisResult(
            "[Mô phỏng Copilot] " + fallback.greetingAndCaringReminder(),
            fallback.recommendationCards(),
            "mock"
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
