package com.travel.planning.retrieval;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles({"test", "rag-demo"})
class RagApiIntegrationTest {
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:0.8.2-pg17").asCompatibleSubstituteFor("postgres"));
    static { POSTGRES.start(); }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("application.rag.api-key", () -> "");
    }

    @Autowired WebApplicationContext context;
    @MockitoBean GeminiEmbeddingClient embeddings;
    MockMvc mvc;

    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }

    @Test void anonymousSeesEightSourcedPlaces() throws Exception {
        mvc.perform(get("/api/v1/public/planning/retrieval/catalog"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(8))
                .andExpect(jsonPath("$.data[0].sourceUrl").isNotEmpty());
    }

    @Test void anonymousCanIndexWithoutCallingRealProvider() throws Exception {
        float[] fixture = new float[768];
        fixture[0] = 1;
        when(embeddings.embed(anyString(), eq(true))).thenReturn(fixture);
        mvc.perform(post("/api/v1/public/planning/retrieval/index"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.failed").value(0));
    }

    @Test void anonymousCanSearchWithoutToken() throws Exception {
        float[] fixture = new float[768];
        fixture[0] = 1;
        when(embeddings.embed(anyString(), eq(false))).thenReturn(fixture);
        mvc.perform(post("/api/v1/public/planning/retrieval/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positiveQuery\":\"bao tang\",\"limit\":3}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.matches.length()").value(3));
    }

    @Test void unrelatedApisStillRequireAuthentication() throws Exception {
        mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/planning/retrieval/index")).andExpect(status().isUnauthorized());
    }

    @Test void searchRejectsEmptyQueryAndExcessiveLimit() throws Exception {
        mvc.perform(post("/api/v1/public/planning/retrieval/search")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"positiveQuery\":\"\",\"limit\":99}"))
                .andExpect(status().isBadRequest());
    }
}
