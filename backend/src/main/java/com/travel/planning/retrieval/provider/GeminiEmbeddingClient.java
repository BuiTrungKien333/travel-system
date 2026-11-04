package com.travel.planning.retrieval.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.planning.ai.resilience.*;
import com.travel.planning.ai.resilience.AiRequestContext.Stage;
import com.travel.planning.retrieval.config.RagDemoProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@Profile("rag-demo")
public class GeminiEmbeddingClient {

    public static final int DIMENSIONS = 768;
    private final RagDemoProperties properties;
    private final RestClient client;
    private final AiRequestContext context;
    private final AiRetryExecutor retries;

    @Autowired
    public GeminiEmbeddingClient(RagDemoProperties properties, AiRequestContext context, AiRetryExecutor retries) {
        this.properties = properties;
        this.context = context;
        this.retries = retries;
        this.client = RestClient.builder().requestFactory(new AiDeadlineRequestFactory(context, properties)).build();
    }

    public GeminiEmbeddingClient(RagDemoProperties properties, RestClient client) {
        this.properties = properties;
        this.client = client;
        this.context = new AiRequestContext(properties);
        this.retries = new AiRetryExecutor(properties, context, new ObjectMapper());
    }

    public GeminiEmbeddingClient(RagDemoProperties properties) {
        this.properties = properties;
        this.context = new AiRequestContext(properties);
        this.retries = new AiRetryExecutor(properties, context, new ObjectMapper());
        this.client = RestClient.builder().requestFactory(new AiDeadlineRequestFactory(context, properties)).build();
    }

    public float[] embed(String text, boolean document) {
        try (var scope = context.ensure()) {
            if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
                context.failure(Stage.RETRIEVAL, "MISSING_API_KEY", null);
                throw new AiCallFailure("MISSING_API_KEY", null);
            }
            return retries.execute(Stage.RETRIEVAL, properties.getEmbeddingModel(), () -> {
                var body = Map.of("model", "models/" + properties.getEmbeddingModel(),
                        "content", Map.of("parts", List.of(Map.of("text", text))),
                        "taskType", document ? "RETRIEVAL_DOCUMENT" : "RETRIEVAL_QUERY",
                        "outputDimensionality", DIMENSIONS);

                EmbeddingResponse response = client.post()
                        .uri(properties.getBaseUrl() + "/models/{model}:embedContent", properties.getEmbeddingModel())
                        .header("x-goog-api-key", properties.getApiKey())
                        .body(body).retrieve().body(EmbeddingResponse.class);

                if (response == null || response.embedding() == null) throw new AiCallFailure("INVALID_RESPONSE", null);
                float[] values = response.embedding().values();
                try {
                    vectorLiteral(values);
                } catch (IllegalArgumentException ex) {
                    throw new AiCallFailure("INVALID_RESPONSE", null);
                }
                return values;
            });
        }
    }

    public static String vectorLiteral(float[] values) {
        if (values == null || values.length != DIMENSIONS)
            throw new IllegalArgumentException("Invalid dimension");

        StringBuilder result = new StringBuilder("[");
        double norm = 0;
        for (int i = 0; i < values.length; i++) {
            if (!Float.isFinite(values[i])) throw new IllegalArgumentException("Non-finite vector");
            norm += (double) values[i] * values[i];
            if (i > 0) result.append(',');
            result.append(values[i]);
        }

        if (norm == 0)
            throw new IllegalArgumentException("Zero vector");

        return result.append(']').toString();
    }

    public record EmbeddingResponse(Embedding embedding) {
    }

    public record Embedding(float[] values) {
    }
}
