package com.travel.planning.retrieval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.planning.ai.resilience.AiCallFailure;
import com.travel.planning.ai.provider.GeminiStructuredClient;
import com.travel.planning.retrieval.provider.GeminiEmbeddingClient;
import com.travel.planning.retrieval.service.PlaceRetrievalService;
import com.travel.weather.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles({"test", "dev"})
class RagDevIntegrationTest {
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:0.8.2-pg17").asCompatibleSubstituteFor("postgres"));
    static { POSTGRES.start(); }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("application.rag.api-key", () -> "");
        registry.add("application.rag.embedding-model", () -> "dev-test-embedding");
    }

    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlaceRetrievalService retrieval;
    @MockitoBean GeminiEmbeddingClient embeddings;
    @MockitoBean GeminiStructuredClient structured;
    @MockitoBean WeatherService weather;
    MockMvc mvc;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test void devCreatesSharedRagSchemaAndCuratedCatalogInItsOwnDatasource() throws Exception {
        assertThat(jdbc.queryForObject("SELECT current_database()", String.class)).isEqualTo(POSTGRES.getDatabaseName());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_extension WHERE extname IN ('vector','unaccent')", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM place_retrieval_profiles WHERE approved", Integer.class)).isEqualTo(8);
        mvc.perform(get("/api/v1/public/planning/retrieval/catalog").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("member").roles("USER").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"), new org.springframework.security.core.authority.SimpleGrantedAuthority("planning:generate"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(8));
    }

    @Test void devCanGenerateGroundedFallbackDraftForAuthorizedMemberWithoutWritingItinerary() throws Exception {
        when(embeddings.embed(anyString(), anyBoolean())).thenThrow(new AiCallFailure("MISSING_API_KEY", null));
        when(structured.generate(any(), anyString(), anyString(), anyMap())).thenThrow(new AiCallFailure("MISSING_API_KEY", null));
        int before = jdbc.queryForObject("SELECT count(*) FROM itineraries", Integer.class);
        var response = mvc.perform(post("/api/v1/public/planning/generate-draft").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("member").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"),new org.springframework.security.core.authority.SimpleGrantedAuthority("planning:generate"))).contentType(MediaType.APPLICATION_JSON).content("""
                {"preferences":{"destination":"Đà Nẵng","numberOfDays":2,"interests":["MUSEUM","HISTORY","ART","LOCAL"],
                "excludedTags":["BEACH"],"dailyStartTime":"08:00","dailyEndTime":"17:00","travelPace":"RELAXED",
                "mustVisitNames":["Bảo tàng Điêu khắc Chăm"]},"startDate":"2026-11-06",
                "origin":{"name":"Điểm xuất phát dev","latitude":16.065,"longitude":108.223}}
                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("READY_WITH_WARNINGS"))
                .andExpect(jsonPath("$.data.days.length()").value(2))
                .andExpect(jsonPath("$.data.executionReport.overallMode").value("FALLBACK"))
                .andExpect(jsonPath("$.data.executionReport.stages[0].source").value("USER_PROVIDED"))
                .andExpect(jsonPath("$.data.executionReport.stages[3].source").value("DETERMINISTIC"))
                .andReturn();
        var data = new ObjectMapper().readTree(response.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
        for (var day : data.path("days")) for (var stop : day.path("stops")) {
            UUID id = UUID.fromString(stop.path("place").path("placeId").asText());
            assertThat(jdbc.queryForObject("SELECT count(*) FROM places WHERE id=?", Integer.class, id)).isEqualTo(1);
            assertThat(stop.path("place").path("tags").toString()).doesNotContain("BEACH");
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM itineraries", Integer.class)).isEqualTo(before);
    }

    @Test void devCanIndexAndSearchVectorsInPostgres() throws Exception {
        float[] vector = new float[768]; vector[0] = 1;
        when(embeddings.embed(anyString(), anyBoolean())).thenReturn(vector);
        assertThat(retrieval.index().failed()).isZero();
        mvc.perform(post("/api/v1/public/planning/retrieval/search").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("member").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"),new org.springframework.security.core.authority.SimpleGrantedAuthority("planning:generate"))).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positiveQuery\":\"bảo tàng\",\"excludedTags\":[\"BEACH\"],\"limit\":4}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.retrievalMode").value("HYBRID"))
                .andExpect(jsonPath("$.data.matches.length()").value(4));
    }

    @Test void knowledgeTracksCurrentAndStaleEmbeddingsUsingTheSameContentHashAsRetrieval() throws Exception {
        float[] vector=new float[768];vector[0]=1;
        when(embeddings.embed(anyString(),anyBoolean())).thenReturn(vector);
        retrieval.index();
        var admin=org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("index-admin")
            .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"),new org.springframework.security.core.authority.SimpleGrantedAuthority("admin:catalog"));
        mvc.perform(get("/api/v1/admin/knowledge").with(admin)).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[?(@.indexStatus == 'CURRENT')]").value(org.hamcrest.Matchers.hasSize(8)));
        jdbc.update("UPDATE place_embeddings SET content_hash='stale' WHERE place_id=(SELECT place_id FROM place_embeddings LIMIT 1)");
        mvc.perform(get("/api/v1/admin/knowledge").with(admin)).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[?(@.indexStatus == 'STALE')]").value(org.hamcrest.Matchers.hasSize(1)));
    }

    @Test void devKeepsUnrelatedAuthenticatedApisProtected() throws Exception {
        mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
    }
}
