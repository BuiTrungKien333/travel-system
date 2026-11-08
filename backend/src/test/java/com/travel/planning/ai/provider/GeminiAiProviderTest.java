package com.travel.planning.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.planning.ai.config.AiConfigProperties;
import com.travel.planning.ai.context.AiContextBuilder;
import com.travel.planning.ai.context.model.AiItineraryPromptContext;
import com.travel.planning.ai.dto.response.AiItineraryResponse;
import com.travel.planning.ai.fallback.FallbackItineraryGenerator;
import com.travel.planning.ai.validator.AiResponseValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.hamcrest.Matchers.containsString;

class GeminiAiProviderTest {

    private AiConfigProperties properties;
    private AiContextBuilder contextBuilder;
    private AiResponseValidator validator;
    private FallbackItineraryGenerator fallbackGenerator;
    private ObjectMapper objectMapper;

    private RestClient restClient;
    private MockRestServiceServer mockServer;
    private GeminiAiProvider provider;

    @BeforeEach
    void setUp() {
        properties = new AiConfigProperties();
        properties.getGemini().setApiKey("test-gemini-key");
        contextBuilder = new AiContextBuilder();
        validator = new AiResponseValidator();
        fallbackGenerator = new FallbackItineraryGenerator();
        objectMapper = new ObjectMapper();

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();

        provider = new GeminiAiProvider(properties, contextBuilder, validator, fallbackGenerator, objectMapper, restClient);
    }

    @Test
    @DisplayName("When apiKey is missing, generateItinerary should gracefully use fallback without calling API")
    void generateItinerary_missingApiKey_usesFallback() {
        properties.getGemini().setApiKey(""); // Không có API key
        GeminiAiProvider noKeyProvider = new GeminiAiProvider(properties, contextBuilder, validator, fallbackGenerator, objectMapper);

        AiItineraryPromptContext context = new AiItineraryPromptContext(
                "Đà Nẵng", LocalDate.now(), LocalDate.now(), 1, null,
                List.of(), List.of(), 0.0, 0
        );

        AiItineraryResponse response = noKeyProvider.generateItinerary(context);

        assertThat(response).isNotNull();
        assertThat(response.dailyPlans()).hasSize(1);
    }

    @Test
    @DisplayName("When apiKey is present, isAvailable should return true")
    void isAvailable_withApiKey_returnsTrue() {
        assertThat(provider.isAvailable()).isTrue();
        assertThat(provider.getProviderName()).isEqualTo("gemini");
    }

    @Test
    @DisplayName("When Gemini API returns 401 Unauthorized, should throw AI_API_KEY_INVALID")
    void generateItinerary_unauthorized_throwsAppException() {
        mockServer.expect(requestTo(containsString("generateContent")))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        AiItineraryPromptContext context = new AiItineraryPromptContext(
                "Đà Nẵng", LocalDate.now(), LocalDate.now(), 1, null,
                List.of(), List.of(), 0.0, 0
        );

        assertThatThrownBy(() -> provider.generateItinerary(context))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_API_KEY_INVALID);

        mockServer.verify();
    }

    @Test
    @DisplayName("When Gemini API hits 429 Too Many Requests, should gracefully activate fallback")
    void generateItinerary_rateLimit_activatesFallback() {
        mockServer.expect(requestTo(containsString("generateContent")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        AiItineraryPromptContext context = new AiItineraryPromptContext(
                "Đà Nẵng", LocalDate.now(), LocalDate.now(), 1, null,
                List.of(), List.of(), 0.0, 0
        );

        AiItineraryResponse response = provider.generateItinerary(context);

        assertThat(response).isNotNull();
        assertThat(response.dailyPlans()).isNotEmpty();
        mockServer.verify();
    }

    @Test
    @DisplayName("When Gemini API returns valid JSON response, should parse and validate successfully")
    void generateItinerary_validResponse_returnsParsedItinerary() {
        String mockGeminiJson = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"tripTitle\\":\\"Khám phá Đà Nẵng\\",\\"tripSummary\\":\\"Hành trình đáng nhớ\\",\\"dailyPlans\\":[{\\"dayNumber\\":1,\\"date\\":\\"2026-10-01\\",\\"dayTheme\\":\\"Ngày 1\\",\\"activities\\":[],\\"mealRecommendations\\":\\"Mì Quảng\\",\\"dayTips\\":\\"Đi sớm\\"}],\\"generalTips\\":[\\"Uống nhiều nước\\"],\\"estimatedBudgetNote\\":\\"Hợp lý\\"}"
                          }
                        ],
                        "role": "model"
                      },
                      "finishReason": "STOP"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(containsString("generateContent")))
                .andRespond(withSuccess(mockGeminiJson, MediaType.APPLICATION_JSON));

        AiItineraryPromptContext context = new AiItineraryPromptContext(
                "Đà Nẵng", LocalDate.now(), LocalDate.now(), 1, null,
                List.of(), List.of(), 0.0, 0
        );

        AiItineraryResponse response = provider.generateItinerary(context);

        assertThat(response).isNotNull();
        assertThat(response.tripTitle()).isEqualTo("Khám phá Đà Nẵng");
        assertThat(response.dailyPlans()).hasSize(1);
        mockServer.verify();
    }
}
