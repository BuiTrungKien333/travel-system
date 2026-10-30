package com.travel.planning.retrieval;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@Profile("rag-demo")
public class GeminiEmbeddingClient {
    public static final int DIMENSIONS = 768;
    private final RagDemoProperties properties;
    private final RestClient client;

    @Autowired
    public GeminiEmbeddingClient(RagDemoProperties properties) {
        this(properties, buildClient());
    }

    public GeminiEmbeddingClient(RagDemoProperties properties, RestClient client) {
        this.properties = properties;
        this.client = client;
    }

    private static RestClient buildClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(15));
        return RestClient.builder().requestFactory(factory).build();
    }

    public float[] embed(String text, boolean document) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
        try {
            var body = Map.of("model", "models/" + properties.getEmbeddingModel(),
                    "content", Map.of("parts", List.of(Map.of("text", text))),
                    "taskType", document ? "RETRIEVAL_DOCUMENT" : "RETRIEVAL_QUERY",
                    "outputDimensionality", DIMENSIONS);
            EmbeddingResponse response = client.post()
                    .uri(properties.getBaseUrl() + "/models/{model}:embedContent", properties.getEmbeddingModel())
                    .header("x-goog-api-key", properties.getApiKey())
                    .body(body).retrieve().body(EmbeddingResponse.class);
            if (response == null || response.embedding() == null) throw new IllegalArgumentException("Missing embedding");
            float[] values = response.embedding().values();
            vectorLiteral(values); // Validate size, finite values and non-zero norm.
            return values;
        } catch (Exception ex) {
            // Do not log HTTP exception messages: they can include sensitive request data.
            throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    public static String vectorLiteral(float[] values) {
        if (values == null || values.length != DIMENSIONS) throw new IllegalArgumentException("Invalid dimension");
        StringBuilder result = new StringBuilder("[");
        double norm = 0;
        for (int i = 0; i < values.length; i++) {
            if (!Float.isFinite(values[i])) throw new IllegalArgumentException("Non-finite vector");
            norm += (double) values[i] * values[i];
            if (i > 0) result.append(',');
            result.append(values[i]);
        }
        if (norm == 0) throw new IllegalArgumentException("Zero vector");
        return result.append(']').toString();
    }

    public record EmbeddingResponse(Embedding embedding) {}
    public record Embedding(float[] values) {}
}
