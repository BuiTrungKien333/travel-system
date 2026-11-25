package com.travel.planning.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.planning.ai.config.AiConfigProperties;
import com.travel.planning.ai.copilot.context.AiCopilotContextBuilder;
import com.travel.planning.ai.copilot.fallback.FallbackAiCopilotGenerator;
import com.travel.planning.ai.copilot.model.AiCopilotAnalysisResult;
import com.travel.planning.ai.copilot.model.AiCopilotPromptContext;
import com.travel.planning.ai.dto.request.gemini.GeminiContent;
import com.travel.planning.ai.dto.request.gemini.GeminiGenerationConfig;
import com.travel.planning.ai.dto.request.gemini.GeminiRequest;
import com.travel.planning.ai.dto.request.gemini.GeminiResponse;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

@Component("geminiAiProvider")
@Slf4j
public class GeminiAiProvider implements AiProvider {

    private final AiConfigProperties aiConfig;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final AiCopilotContextBuilder copilotContextBuilder;
    private final FallbackAiCopilotGenerator fallbackCopilotGenerator;

    @Autowired
    public GeminiAiProvider(
        AiConfigProperties aiConfig,
        ObjectMapper objectMapper,
        AiCopilotContextBuilder copilotContextBuilder,
        FallbackAiCopilotGenerator fallbackCopilotGenerator
    ) {
        this.aiConfig = aiConfig;
        this.objectMapper = objectMapper;
        this.copilotContextBuilder = copilotContextBuilder;
        this.fallbackCopilotGenerator = fallbackCopilotGenerator;

        int timeoutSeconds = aiConfig.getGemini().getTimeoutSeconds();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds > 0 ? timeoutSeconds : 30));

        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    // Constructor phục vụ riêng cho Unit Test khi cần mock RestClient
    public GeminiAiProvider(
        AiConfigProperties aiConfig,
        ObjectMapper objectMapper,
        AiCopilotContextBuilder copilotContextBuilder,
        FallbackAiCopilotGenerator fallbackCopilotGenerator,
        RestClient restClient
    ) {
        this.aiConfig = aiConfig;
        this.objectMapper = objectMapper;
        this.copilotContextBuilder = copilotContextBuilder;
        this.fallbackCopilotGenerator = fallbackCopilotGenerator;
        this.restClient = restClient;
    }

    @Override
    public AiCopilotAnalysisResult analyzeAndRecommend(AiCopilotPromptContext context) {
        AiConfigProperties.GeminiConfig config = aiConfig.getGemini();
        String apiKey = config.getApiKey();

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[GEMINI_COPILOT] API Key is missing or empty. Activating fallback copilot generator.");
            return fallbackCopilotGenerator.generateFallback(context);
        }

        String model = config.getModel() != null ? config.getModel() : "gemini-3.5-flash";
        String baseUrl =
            config.getBaseUrl() != null ? config.getBaseUrl() : "https://generativelanguage.googleapis.com/v1beta";
        String endpoint = String.format("%s/models/%s:generateContent?key=%s", baseUrl, model, apiKey);

        log.info(
            "[GEMINI_COPILOT] Requesting AI copilot critique using model={}, destination={}, issuesCount={}",
            model,
            context.destination(),
            context.detectedIssues().size()
        );

        GeminiRequest requestPayload = new GeminiRequest(
            GeminiContent.system(copilotContextBuilder.buildSystemInstruction()),
            List.of(GeminiContent.user(copilotContextBuilder.buildUserPrompt(context))),
            GeminiGenerationConfig.jsonConfig(config.getTemperature(), config.getMaxOutputTokens())
        );

        try {
            ResponseEntity<GeminiResponse> response = restClient
                .post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestPayload)
                .retrieve()
                .toEntity(GeminiResponse.class);

            GeminiResponse geminiResponse = response.getBody();
            if (geminiResponse == null) {
                log.warn("[GEMINI_COPILOT] Null response body from Gemini. Using fallback.");
                return fallbackCopilotGenerator.generateFallback(context);
            }

            String rawJson = geminiResponse.extractText();
            if (rawJson == null || rawJson.isBlank()) {
                log.warn("[GEMINI_COPILOT] Empty content parts in Gemini response. Using fallback.");
                return fallbackCopilotGenerator.generateFallback(context);
            }

            String cleanedJson = cleanJson(rawJson);
            AiCopilotAnalysisResult parsedResponse = objectMapper.readValue(cleanedJson, AiCopilotAnalysisResult.class);

            return new AiCopilotAnalysisResult(
                parsedResponse.greetingAndCaringReminder(),
                parsedResponse.recommendationCards(),
                model
            );
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED || ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.error("[GEMINI_COPILOT] Gemini API key rejected ({}): {}", ex.getStatusCode(), ex.getMessage());
                throw new AppException(ErrorCode.AI_API_KEY_INVALID);
            }
            if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn(
                    "[GEMINI_COPILOT] Rate limit or quota exceeded (429). Activating graceful fallback: {}",
                    ex.getMessage()
                );
                return fallbackCopilotGenerator.generateFallback(context);
            }
            log.error(
                "[GEMINI_COPILOT] Client error calling Gemini API: status={}, message={}. Activating graceful fallback.",
                ex.getStatusCode(),
                ex.getMessage()
            );
            return fallbackCopilotGenerator.generateFallback(context);
        } catch (HttpServerErrorException ex) {
            log.warn(
                "[GEMINI_COPILOT] Gemini server temporarily unavailable ({}) - high demand/spikes. Activating graceful fallback.",
                ex.getStatusCode()
            );
            return fallbackCopilotGenerator.generateFallback(context);
        } catch (Exception ex) {
            log.error(
                "[GEMINI_COPILOT] Exception calling Gemini API: {}. Activating graceful fallback.",
                ex.getMessage(),
                ex
            );
            return fallbackCopilotGenerator.generateFallback(context);
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
