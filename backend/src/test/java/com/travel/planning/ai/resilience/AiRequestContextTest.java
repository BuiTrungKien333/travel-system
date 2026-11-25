package com.travel.planning.ai.resilience;

import static com.travel.planning.ai.resilience.AiRequestContext.Stage.*;
import static org.assertj.core.api.Assertions.*;

import com.travel.planning.retrieval.config.RagProperties;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class AiRequestContextTest {

    @Test
    void stageDurationIsRecordedEvenWhenOperationFails() {
        var nanos = new AtomicLong();
        var context = new AiRequestContext(new RagProperties(), nanos::get);
        try (var scope = context.ensure()) {
            assertThatThrownBy(() ->
                context.measure(INTENT, () -> {
                    nanos.addAndGet(2_000_000);
                    throw new AiCallFailure("INVALID_RESPONSE", null);
                })
            ).isInstanceOf(AiCallFailure.class);
            assertThat(context.report().stages().getFirst().elapsedMillis()).isEqualTo(2);
        }
    }

    @Test
    void reusedAiThemeDoesNotInventAnotherAttemptAndMixedSourcesReachOverallReport() {
        var context = new AiRequestContext(new RagProperties());
        try (var scope = context.ensure()) {
            context.attempt(ALLOCATION, "fixture-model", false);
            context.reuseModel(THEMES, ALLOCATION);
            context.record(
                THEMES,
                "AI_WITH_FALLBACK",
                "ALLOCATION_HINTS_AND_GROUNDED_TEMPLATE",
                "THEME_HINT_INVALID_FOR_FINAL_SCHEDULE",
                null
            );
            var report = context.report();
            assertThat(report.overallMode()).isEqualTo("AI_WITH_FALLBACK");
            assertThat(report.stages().get(4).attempts()).isZero();
            assertThat(report.stages().get(4).model()).isEqualTo("fixture-model");
        }
    }
}
