package com.travel.planning.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.planning.ai.config.AiConfigProperties;
import com.travel.planning.ai.copilot.context.AiCopilotContextBuilder;
import com.travel.planning.ai.copilot.fallback.FallbackAiCopilotGenerator;
import com.travel.planning.ai.copilot.model.AiCopilotPromptContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GeminiAiProviderTest {
    private final ObjectMapper json = new ObjectMapper();
    private final AiCopilotPromptContext context = new AiCopilotPromptContext(
            "Đà Nẵng", LocalDate.of(2026, 10, 1), List.of(), List.of(), null, null);
    private AiConfigProperties properties;
    private MockRestServiceServer server;
    private GeminiAiProvider provider;

    @BeforeEach
    void setUp() {
        properties = new AiConfigProperties();
        properties.getGemini().setApiKey("test-gemini-key");
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new GeminiAiProvider(properties, json, new AiCopilotContextBuilder(),
                new FallbackAiCopilotGenerator(), builder.build());
    }

    @Test
    void analyzeAndRecommend_missingKey_usesFallbackWithoutHttp() {
        properties.getGemini().setApiKey("");
        assertThat(provider.isAvailable()).isFalse();
        assertThat(provider.analyzeAndRecommend(context).aiProvider()).isEqualTo("rule-based-fallback");
        server.verify();
    }

    @Test
    void isAvailable_withKey_returnsTrue() {
        assertThat(provider.isAvailable()).isTrue();
        assertThat(provider.getProviderName()).isEqualTo("gemini");
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403})
    void analyzeAndRecommend_rejectedKey_preservesDomainError(int status) {
        server.expect(requestTo(containsString("generateContent")))
                .andRespond(withStatus(HttpStatus.valueOf(status)));
        assertThatThrownBy(() -> provider.analyzeAndRecommend(context))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.AI_API_KEY_INVALID);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 429, 500, 503})
    void analyzeAndRecommend_providerFailure_usesFallback(int status) {
        server.expect(requestTo(containsString("generateContent")))
                .andRespond(withStatus(HttpStatus.valueOf(status)));
        assertThat(provider.analyzeAndRecommend(context).aiProvider()).isEqualTo("rule-based-fallback");
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"candidates\":[]}", "not JSON"})
    void analyzeAndRecommend_unusableResponse_usesFallback(String body) {
        server.expect(requestTo(containsString("generateContent")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertThat(provider.analyzeAndRecommend(context).aiProvider()).isEqualTo("rule-based-fallback");
        server.verify();
    }

    @Test
    void analyzeAndRecommend_validFencedJson_returnsCopilot() throws Exception {
        String answer = "```json\n" + json.writeValueAsString(Map.of(
                "greetingAndCaringReminder", "Chúc bạn có chuyến đi tốt đẹp",
                "recommendationCards", List.of())) + "\n```";
        String body = json.writeValueAsString(Map.of("candidates", List.of(Map.of(
                "content", Map.of("parts", List.of(Map.of("text", answer)), "role", "model")))));
        server.expect(requestTo(containsString("generateContent")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        var response = provider.analyzeAndRecommend(context);
        assertThat(response.greetingAndCaringReminder()).isEqualTo("Chúc bạn có chuyến đi tốt đẹp");
        assertThat(response.recommendationCards()).isEmpty();
        assertThat(response.aiProvider()).isEqualTo(properties.getGemini().getModel());
        server.verify();
    }
}
