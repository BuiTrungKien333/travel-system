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
import com.travel.planning.ai.intent.GeminiIntentClient;
import com.travel.planning.ai.intent.IntentFixtures;
import org.springframework.jdbc.core.JdbcTemplate;

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
    @MockitoBean GeminiIntentClient intents;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlaceRetrievalService retrieval;
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

    @Test void naturalTextReturnsValidatedIntentAndDatabaseCandidatesWithExclusions() throws Exception {
        float[] vector = new float[768]; vector[0] = 1;
        when(embeddings.embed(anyString(), anyBoolean())).thenReturn(vector);
        retrieval.index();
        when(intents.extract(IntentFixtures.TEXT)).thenReturn(IntentFixtures.extracted());
        var response = mvc.perform(post("/api/v1/public/planning/retrieval/search-from-text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("text", IntentFixtures.TEXT))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CANDIDATES_FOUND"))
                .andExpect(jsonPath("$.data.analysis.intent.numberOfDays").value(2))
                .andExpect(jsonPath("$.data.analysis.intent.excludedTags[0]").value("BEACH"))
                .andExpect(jsonPath("$.data.candidates.retrievalMode").value("HYBRID"))
                .andReturn();
        var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        for (var match : json.at("/data/candidates/matches")) {
            var id = java.util.UUID.fromString(match.at("/place/placeId").asText());
            org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT count(*) FROM places WHERE id=?", Integer.class, id)).isEqualTo(1);
            org.assertj.core.api.Assertions.assertThat(match.at("/place/tags").toString()).doesNotContain("BEACH");
        }
    }

    @Test void beachPreferenceExcludesAllMuseums() throws Exception {
        float[] vector = new float[768]; vector[0] = 1;
        when(embeddings.embed(anyString(), anyBoolean())).thenReturn(vector); retrieval.index();
        when(intents.extract("beach request")).thenReturn(IntentFixtures.intent("Đà Nẵng", java.util.List.of("BEACH"), java.util.List.of("MUSEUM"), java.util.List.of(), java.util.List.of()));
        mvc.perform(post("/api/v1/public/planning/retrieval/search-from-text").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"beach request\",\"limit\":4}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CANDIDATES_FOUND"))
                .andExpect(jsonPath("$.data.candidates.matches[*].place.tags[*]", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("MUSEUM"))));
    }

    @Test void absentDestinationReturnsNeedsInputAndKeepsNullFields() throws Exception {
        when(intents.extract("xem tranh")).thenReturn(IntentFixtures.intent(null, java.util.List.of("ART"), java.util.List.of(), java.util.List.of(), java.util.List.of()));
        mvc.perform(post("/api/v1/public/planning/retrieval/search-from-text").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"xem tranh\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("NEEDS_INPUT"))
                .andExpect(jsonPath("$.data.analysis.missingFields", org.hamcrest.Matchers.hasItem("destination")))
                .andExpect(jsonPath("$.data.candidates").isEmpty());
    }

    @Test void invalidNaturalTextIsRejectedBeforeCallingProvider() throws Exception {
        mvc.perform(post("/api/v1/public/planning/retrieval/intent").contentType(MediaType.APPLICATION_JSON).content("{\"text\":\" \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/public/planning/retrieval/search-from-text").contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"test\",\"limit\":9}"))
                .andExpect(status().isBadRequest());
    }
}
