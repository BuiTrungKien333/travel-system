package com.travel.planning.ai.intent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.planning.ai.intent.provider.GeminiIntentClient;
import com.travel.planning.ai.intent.validator.TravelIntentValidator;
import com.travel.planning.retrieval.config.RagProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GeminiIntentClientTest {
    final ObjectMapper mapper = new ObjectMapper();
    RagProperties properties;
    MockRestServiceServer server;
    GeminiIntentClient client;

    @BeforeEach void setup() throws Exception {
        properties = new RagProperties(); properties.setApiKey("test-only");
        var builder = RestClient.builder(); server = MockRestServiceServer.bindTo(builder).build();
        client = new GeminiIntentClient(properties, mapper, new TravelIntentValidator(mapper), builder.build());
    }

    @Test void extract_sendsSchemaAndRawUserDataSeparately() throws Exception {
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"))
                .andExpect(header("x-goog-api-key", "test-only"))
                .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
                .andExpect(jsonPath("$.generationConfig.thinkingConfig.thinkingLevel").value("LOW"))
                .andExpect(jsonPath("$.generationConfig.responseJsonSchema.additionalProperties").value(false))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value(IntentFixtures.TEXT))
                .andRespond(withSuccess(response("STOP", IntentFixtures.json().toString()), MediaType.APPLICATION_JSON));
        assertThat(client.extract(IntentFixtures.TEXT).excludedTags()).containsExactly("BEACH"); server.verify();
    }

    @Test void missingKeyDoesNotFabricateIntentOrCallProvider() {
        properties.setApiKey("");
        assertThatThrownBy(() -> client.extract(IntentFixtures.TEXT)).isInstanceOfSatisfying(AppException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AI_SERVICE_UNAVAILABLE));
        server.verify();
    }

    @Test void truncatedOutputIsRejected() throws Exception {
        server.expect(anything()).andRespond(withSuccess(response("MAX_TOKENS", IntentFixtures.json().toString()), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.extract(IntentFixtures.TEXT)).isInstanceOfSatisfying(AppException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AI_VALIDATION_FAILED)); server.verify();
    }

    @Test void quotaFailureIsExplicitAndNotSilentlyReplacedWithMock() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"details\":[{\"violations\":[{\"quotaId\":\"GenerateRequestsPerDayPerProject\"}]}]}}"));
        assertThatThrownBy(() -> client.extract(IntentFixtures.TEXT)).isInstanceOfSatisfying(AppException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AI_RATE_LIMIT_EXCEEDED)); server.verify();
    }

    @Test void malformedJsonIsRejected() throws Exception {
        server.expect(anything()).andRespond(withSuccess(response("STOP", "not JSON"), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.extract(IntentFixtures.TEXT)).isInstanceOf(AppException.class); server.verify();
    }

    String response(String finish, String json) throws Exception {
        var root = mapper.createObjectNode(); var candidate = root.putArray("candidates").addObject();
        candidate.put("finishReason", finish); candidate.putObject("content").putArray("parts").addObject().put("text", json);
        return mapper.writeValueAsString(root);
    }
}
