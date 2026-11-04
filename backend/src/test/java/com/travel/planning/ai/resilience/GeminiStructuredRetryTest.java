package com.travel.planning.ai.resilience;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.planning.ai.provider.GeminiStructuredClient;
import com.travel.planning.retrieval.config.RagDemoProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.travel.planning.ai.resilience.AiRequestContext.Stage.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GeminiStructuredRetryTest {
    @Test void transportRetriesSameKeyAndSubsequentStageCannotRetryAgain() {
        var properties = new RagDemoProperties(); properties.setApiKey("fixture-key");
        var millis = new AtomicLong();
        var context = new AiRequestContext(properties, () -> millis.get() * 1_000_000);
        var mapper = new ObjectMapper();
        var executor = new AiRetryExecutor(properties, context, mapper, Clock.systemUTC(), millis::addAndGet, () -> 0);
        var builder = RestClient.builder(); var server = MockRestServiceServer.bindTo(builder).build();
        var client = new GeminiStructuredClient(properties, mapper, builder.build(), context, executor);
        String url = properties.getBaseUrl() + "/models/" + properties.getIntentModel() + ":generateContent";
        server.expect(requestTo(url)).andExpect(header("x-goog-api-key", "fixture-key"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo(url)).andExpect(header("x-goog-api-key", "fixture-key"))
                .andRespond(withSuccess(envelope(), MediaType.APPLICATION_JSON));
        server.expect(requestTo(url)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo(url)).andRespond(withSuccess(envelope(), MediaType.APPLICATION_JSON));
        try (var scope = context.ensure()) {
            assertThat(client.generate(INTENT, "instruction", "input", Map.of()).get("ok").asBoolean()).isTrue();
            assertThatThrownBy(() -> client.generate(ALLOCATION, "instruction", "input", Map.of())).isInstanceOf(AiCallFailure.class);
            client.generate(THEMES, "instruction", "input", Map.of());
            var report = context.report();
            assertThat(report.retriesUsed()).isEqualTo(1);
            assertThat(report.stages().get(0).attempts()).isEqualTo(2);
            assertThat(report.stages().get(2).attempts()).isEqualTo(1);
            assertThat(report.stages().get(2).retryDecision()).isEqualTo("REQUEST_RETRY_LIMIT_REACHED");
            assertThat(report.stages().get(4).source()).isEqualTo("AI");
        }
        server.verify();
    }

    String envelope() {
        return """
                {"candidates":[{"finishReason":"STOP","content":{"parts":[{"text":"{\\\"ok\\\":true}"}]}}]}
                """;
    }
}
