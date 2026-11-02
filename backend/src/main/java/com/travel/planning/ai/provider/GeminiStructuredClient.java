package com.travel.planning.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.AppException;
import com.travel.planning.ai.resilience.*;
import com.travel.planning.ai.resilience.AiRequestContext.Stage;
import com.travel.planning.retrieval.config.RagDemoProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Shared transport for the demo's intent, allocation and day-theme tasks. */
@Component
@Profile("rag-demo")
public class GeminiStructuredClient {
    private final RagDemoProperties properties;
    private final ObjectMapper mapper;
    private final RestClient client;
    private final AiRequestContext context;
    private final AiRetryExecutor retries;

    @Autowired
    public GeminiStructuredClient(RagDemoProperties properties, ObjectMapper mapper, AiRequestContext context, AiRetryExecutor retries) {
        this.properties = properties; this.mapper = mapper; this.context = context; this.retries = retries;
        this.client = RestClient.builder().requestFactory(new AiDeadlineRequestFactory(context, properties)).build();
    }

    public GeminiStructuredClient(RagDemoProperties properties, ObjectMapper mapper, RestClient client) {
        this.properties = properties; this.mapper = mapper; this.client = client;
        this.context = new AiRequestContext(properties);
        this.retries = new AiRetryExecutor(properties, context, mapper);
    }

    public GeminiStructuredClient(RagDemoProperties properties, ObjectMapper mapper, RestClient client,
                                  AiRequestContext context, AiRetryExecutor retries) {
        this.properties = properties; this.mapper = mapper; this.client = client; this.context = context; this.retries = retries;
    }

    public JsonNode generate(String instruction, String input, Map<String, Object> schema) {
        return generate(Stage.INTENT, instruction, input, schema);
    }

    public JsonNode generate(Stage stage, String instruction, String input, Map<String, Object> schema) {
        try (var scope = context.ensure()) {
            if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
                context.failure(stage, "MISSING_API_KEY", null);
                throw new AiCallFailure("MISSING_API_KEY", null);
            }
            return retries.execute(stage, properties.getIntentModel(), () -> {
                String raw = client.post().uri(properties.getBaseUrl() + "/models/{model}:generateContent", properties.getIntentModel())
                        .header("x-goog-api-key", properties.getApiKey())
                        .body(Map.of("systemInstruction", Map.of("parts", List.of(Map.of("text", instruction))),
                                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", input)))),
                                "generationConfig", Map.of("temperature", 0, "maxOutputTokens", 4096,
                                        "responseMimeType", "application/json", "responseJsonSchema", schema)))
                        .retrieve().body(String.class);
                return parse(raw);
            });
        }
    }

    private JsonNode parse(String raw) {
        try {
            JsonNode response = raw == null ? null : mapper.readTree(raw);
            if (response == null) throw invalid();
            JsonNode candidate = response.path("candidates").path(0);
            if (!"STOP".equals(candidate.path("finishReason").asText())) throw invalid();
            StringBuilder json = new StringBuilder();
            for (JsonNode part : candidate.path("content").path("parts")) {
                if (!part.path("thought").asBoolean(false) && part.path("text").isTextual()) json.append(part.path("text").asText());
            }
            JsonNode result = mapper.reader().with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(json.toString());
            if (result == null || !result.isObject()) throw invalid();
            return result;
        } catch (IOException ex) { throw invalid(); }
    }

    private AppException invalid() { return new AiCallFailure("INVALID_RESPONSE", null); }
}
