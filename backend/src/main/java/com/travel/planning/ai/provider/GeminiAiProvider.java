package com.travel.planning.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.planning.ai.config.AiConfigProperties;
import com.travel.planning.ai.context.AiContextBuilder;
import com.travel.planning.ai.context.model.AiItineraryPromptContext;
import com.travel.planning.ai.dto.request.gemini.GeminiContent;
import com.travel.planning.ai.dto.request.gemini.GeminiGenerationConfig;
import com.travel.planning.ai.dto.request.gemini.GeminiRequest;
import com.travel.planning.ai.dto.request.gemini.GeminiResponse;
import com.travel.planning.ai.dto.response.AiItineraryResponse;
import com.travel.planning.ai.fallback.FallbackItineraryGenerator;
import com.travel.planning.ai.validator.AiResponseValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

@Component("geminiAiProvider")
@Slf4j
public class GeminiAiProvider implements AiProvider {

    private final AiConfigProperties aiConfig;
    private final AiContextBuilder contextBuilder;
    private final AiResponseValidator validator;
    private final FallbackItineraryGenerator fallbackGenerator;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GeminiAiProvider(
            AiConfigProperties aiConfig,
            AiContextBuilder contextBuilder,
            AiResponseValidator validator,
            FallbackItineraryGenerator fallbackGenerator,
            ObjectMapper objectMapper
    ) {
        this.aiConfig = aiConfig;
        this.contextBuilder = contextBuilder;
        this.validator = validator;
        this.fallbackGenerator = fallbackGenerator;
        this.objectMapper = objectMapper;

        int timeoutSeconds = aiConfig.getGemini().getTimeoutSeconds();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds > 0 ? timeoutSeconds : 30));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    // Constructor phục vụ riêng cho Unit Test khi cần mock RestClient
    public GeminiAiProvider(
            AiConfigProperties aiConfig,
            AiContextBuilder contextBuilder,
            AiResponseValidator validator,
            FallbackItineraryGenerator fallbackGenerator,
            ObjectMapper objectMapper,
            RestClient restClient
    ) {
        this.aiConfig = aiConfig;
        this.contextBuilder = contextBuilder;
        this.validator = validator;
        this.fallbackGenerator = fallbackGenerator;
        this.objectMapper = objectMapper;
        this.restClient = restClient;
    }

    @Override
    public AiItineraryResponse generateItinerary(AiItineraryPromptContext context) {
        AiConfigProperties.GeminiConfig config = aiConfig.getGemini();
        String apiKey = config.getApiKey();

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[GEMINI] API Key is missing or empty. Activating fallback itinerary generator.");
            return fallbackGenerator.generateFallback(context);
        }

        String model = config.getModel() != null ? config.getModel() : "gemini-2.0-flash";
        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : "https://generativelanguage.googleapis.com/v1beta";
        String endpoint = String.format("%s/models/%s:generateContent?key=%s", baseUrl, model, apiKey);

        log.info("[GEMINI] Requesting itinerary generation using model={}, destination={}, totalDays={}",
                model, context.destination(), context.totalDays());

        GeminiRequest requestPayload = new GeminiRequest(
                GeminiContent.system(contextBuilder.buildSystemInstruction()),
                List.of(GeminiContent.user(contextBuilder.buildUserPrompt(context))),
                GeminiGenerationConfig.jsonConfig(config.getTemperature(), config.getMaxOutputTokens())
        );

        try {
            ResponseEntity<GeminiResponse> response = restClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestPayload)
                    .retrieve()
                    .toEntity(GeminiResponse.class);

            GeminiResponse geminiResponse = response.getBody();
            if (geminiResponse == null) {
                log.warn("[GEMINI] Null response body from Gemini. Using fallback.");
                return fallbackGenerator.generateFallback(context);
            }

            String rawJson = geminiResponse.extractText();
            if (rawJson == null || rawJson.isBlank()) {
                log.warn("[GEMINI] Empty content parts in Gemini response. Using fallback.");
                return fallbackGenerator.generateFallback(context);
            }

            String cleanedJson = cleanJson(rawJson);
            AiItineraryResponse parsedResponse = objectMapper.readValue(cleanedJson, AiItineraryResponse.class);

            // Xác thực và tự động bù đắp nếu AI bỏ sót địa điểm
            return validator.validateAndEnforce(parsedResponse, context);

        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED || ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.error("[GEMINI] Gemini API key rejected ({}): {}", ex.getStatusCode(), ex.getMessage());
                throw new AppException(ErrorCode.AI_API_KEY_INVALID);
            }
            if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn("[GEMINI] Rate limit or quota exceeded (429). Activating graceful fallback: {}", ex.getMessage());
                return fallbackGenerator.generateFallback(context);
            }
            log.error("[GEMINI] Client error calling Gemini API: status={}, message={}. Activating graceful fallback.",
                    ex.getStatusCode(), ex.getMessage());
            return fallbackGenerator.generateFallback(context);
        } catch (Exception ex) {
            log.error("[GEMINI] Exception calling Gemini API: {}. Activating graceful fallback.", ex.getMessage(), ex);
            return fallbackGenerator.generateFallback(context);
        }
    }

    @Override
    public boolean isAvailable() {
        String key = aiConfig.getGemini().getApiKey();
        return key != null && !key.isBlank();
    }

    @Override
    public String getProviderName() {
        return "gemini";
    }

    /**
     * Loại bỏ markdown fences ```json ... ``` nếu mô hình lỡ trả về kèm định dạng
     */
    private String cleanJson(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}
