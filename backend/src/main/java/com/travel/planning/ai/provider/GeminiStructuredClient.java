package com.travel.planning.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.planning.retrieval.RagDemoProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Shared transport for the demo's intent, allocation and day-theme tasks. */
@Component
@Profile("rag-demo")
@Slf4j
public class GeminiStructuredClient {
    private final RagDemoProperties properties;
    private final ObjectMapper mapper;
    private final RestClient client;

    @Autowired
    public GeminiStructuredClient(RagDemoProperties properties, ObjectMapper mapper) {
        this(properties, mapper, buildClient());
    }

    public GeminiStructuredClient(RagDemoProperties properties, ObjectMapper mapper, RestClient client) {
        this.properties = properties; this.mapper = mapper; this.client = client;
    }

    private static RestClient buildClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5)); factory.setReadTimeout(Duration.ofSeconds(30));
        return RestClient.builder().requestFactory(factory).build();
    }

    public JsonNode generate(String instruction, String input, Map<String, Object> schema) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        String raw;
        try {
            raw = client.post().uri(properties.getBaseUrl() + "/models/{model}:generateContent", properties.getIntentModel())
                    .header("x-goog-api-key", properties.getApiKey())
                    .body(Map.of("systemInstruction", Map.of("parts", List.of(Map.of("text", instruction))),
                            "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", input)))),
                            "generationConfig", Map.of("temperature", 0, "maxOutputTokens", 4096,
                                    "responseMimeType", "application/json", "responseJsonSchema", schema)))
                    .retrieve().body(String.class);
        } catch (HttpClientErrorException ex) {
            log.warn("Structured AI request rejected: model={}, status={}", properties.getIntentModel(), ex.getStatusCode().value());
            throw new AppException(ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS ? ErrorCode.AI_RATE_LIMIT_EXCEEDED : ErrorCode.AI_SERVICE_UNAVAILABLE);
        } catch (RestClientException ex) {
            log.warn("Structured AI unavailable: model={}, failure={}", properties.getIntentModel(), ex.getClass().getSimpleName());
            throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
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

    private AppException invalid() { return new AppException(ErrorCode.AI_VALIDATION_FAILED); }
}
