package com.travel.planning.retrieval;

import com.travel.common.exception.AppException;
import com.travel.planning.retrieval.config.RagDemoProperties;
import com.travel.planning.retrieval.provider.GeminiEmbeddingClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GeminiEmbeddingClientTest {
    @Test
    void embed_usesHeaderAndRetrievalTaskAndChecksDimension() {
        var properties = new RagDemoProperties();
        properties.setApiKey("test-only");
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        float[] vector = new float[768]; vector[0] = 1;
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent"))
                .andExpect(header("x-goog-api-key", "test-only"))
                .andExpect(jsonPath("$.taskType").value("RETRIEVAL_DOCUMENT"))
                .andExpect(jsonPath("$.outputDimensionality").value(768))
                .andRespond(withSuccess("{\"embedding\":{\"values\":" + Arrays.toString(vector) + "}}", MediaType.APPLICATION_JSON));
        assertThat(new GeminiEmbeddingClient(properties, builder.build()).embed("văn hóa", true)).containsExactly(vector);
        server.verify();
    }

    @Test
    void embed_missingKeyDoesNotFabricateVector() {
        assertThatThrownBy(() -> new GeminiEmbeddingClient(new RagDemoProperties()).embed("biển", false))
                .isInstanceOf(AppException.class);
    }

    @Test
    void vectorLiteral_rejectsInvalidVectors() {
        assertThatThrownBy(() -> GeminiEmbeddingClient.vectorLiteral(new float[768])).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GeminiEmbeddingClient.vectorLiteral(new float[3])).isInstanceOf(IllegalArgumentException.class);
        var vector = new float[768]; vector[0] = Float.NaN;
        assertThatThrownBy(() -> GeminiEmbeddingClient.vectorLiteral(vector)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void embed_badProviderOutputIsUnavailable() {
        var properties = new RagDemoProperties(); properties.setApiKey("test-only");
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(anything()).andRespond(withSuccess("{\"embedding\":{\"values\":[1,2]}}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> new GeminiEmbeddingClient(properties, builder.build()).embed("biển", false)).isInstanceOf(AppException.class);
        server.verify();
    }
}
