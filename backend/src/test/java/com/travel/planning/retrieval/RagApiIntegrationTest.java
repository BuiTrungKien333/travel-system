package com.travel.planning.retrieval;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.planning.ai.intent.IntentFixtures;
import com.travel.planning.ai.intent.model.TravelIntent;
import com.travel.planning.ai.intent.provider.GeminiIntentClient;
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
    @MockitoBean GeminiStructuredClient structured;
    @MockitoBean WeatherService weather;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlaceRetrievalService retrieval;
    MockMvc mvc;

    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).defaultRequest(get("/").with(
            org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("authorized-tooling")
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"),
                    new org.springframework.security.core.authority.SimpleGrantedAuthority("planning:generate"),
                    new org.springframework.security.core.authority.SimpleGrantedAuthority("admin:ai:index"))))
            .apply(springSecurity()).build(); }

    @Test void authorizedMemberSeesEightSourcedPlaces() throws Exception {
        mvc.perform(get("/api/v1/public/planning/retrieval/catalog"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(8))
                .andExpect(jsonPath("$.data[0].sourceUrl").isNotEmpty());
    }

    @Test void authorizedAdminCanIndexWithoutCallingRealProvider() throws Exception {
        float[] fixture = new float[768];
        fixture[0] = 1;
        when(embeddings.embed(anyString(), eq(true))).thenReturn(fixture);
        mvc.perform(post("/api/v1/public/planning/retrieval/index"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.failed").value(0));
    }

    @Test void authorizedMemberCanSearch() throws Exception {
        float[] fixture = new float[768];
        fixture[0] = 1;
        when(embeddings.embed(anyString(), eq(false))).thenReturn(fixture);
        mvc.perform(post("/api/v1/public/planning/retrieval/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positiveQuery\":\"bao tang\",\"limit\":3}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.matches.length()").value(3));
    }

    @Test void unrelatedApisStillRequireAuthentication() throws Exception {
        mvc.perform(get("/api/v1/users/me").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/planning/retrieval/index").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous())).andExpect(status().isUnauthorized());
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

    @Test void draftBuildsTwoDaysWithRealSolverAndNoDatabaseWrites() throws Exception {
        prepareDraft(IntentFixtures.extracted());
        int before = jdbc.queryForObject("SELECT count(*) FROM itineraries", Integer.class);
        var result = mvc.perform(post("/api/v1/public/planning/generate-draft").contentType(MediaType.APPLICATION_JSON)
                        .content(draftBody("full draft", "2026-11-02")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("READY_WITH_WARNINGS"))
                .andExpect(jsonPath("$.data.days.length()").value(2))
                .andExpect(jsonPath("$.data.metadata.routeSource").value("ESTIMATED_HAVERSINE_35_KMH"))
                .andExpect(jsonPath("$.data.metadata.allocationSource").value("RULE_BASED_FALLBACK"))
                .andExpect(jsonPath("$.data.weather.status").value("UNKNOWN"))
                .andReturn();
        var root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        var ids = new java.util.HashSet<java.util.UUID>();
        for (var day : root.at("/data/days")) {
            org.assertj.core.api.Assertions.assertThat(java.time.LocalDateTime.parse(day.get("finish").asText()))
                    .isBeforeOrEqualTo(java.time.LocalDate.parse(day.get("date").asText()).atTime(17,0));
            for (var stop : day.get("stops")) {
                var id = java.util.UUID.fromString(stop.at("/place/placeId").asText());
                org.assertj.core.api.Assertions.assertThat(ids.add(id)).isTrue();
                org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT count(*) FROM places WHERE id=?", Integer.class, id)).isEqualTo(1);
                org.assertj.core.api.Assertions.assertThat(stop.at("/place/tags").toString()).doesNotContain("BEACH");
            }
            org.assertj.core.api.Assertions.assertThat(day.get("title").asText()).isNotBlank();
            org.assertj.core.api.Assertions.assertThat(day.get("returnToOrigin").isNull()).isFalse();
        }
        org.assertj.core.api.Assertions.assertThat(ids).isNotEmpty();
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT count(*) FROM itineraries", Integer.class)).isEqualTo(before);
    }

    @Test void draftDoesNotDropImpossibleRequiredMuseums() throws Exception {
        var required = java.util.List.of("Bảo tàng Điêu khắc Chăm", "Bảo tàng Mỹ thuật Đà Nẵng", "Bảo tàng Đà Nẵng");
        prepareDraft(new TravelIntent("Đà Nẵng", 1, java.util.List.of("MUSEUM"), java.util.List.of(), "08:00", "08:30", null,
                required, java.util.List.of(), java.util.Map.of()));
        mvc.perform(post("/api/v1/public/planning/generate-draft").contentType(MediaType.APPLICATION_JSON).content(draftBody("full draft", "2026-11-02")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("INFEASIBLE"))
                .andExpect(jsonPath("$.data.days").isEmpty())
                .andExpect(jsonPath("$.data.issues[0].code").value("REQUIRED_TIME_LOWER_BOUND"));
    }

    @Test void draftRequiresExplicitDailyWindowAndDayCount() throws Exception {
        prepareDraft(IntentFixtures.intent("Đà Nẵng", java.util.List.of("ART"), java.util.List.of(), java.util.List.of(), java.util.List.of()));
        mvc.perform(post("/api/v1/public/planning/generate-draft").contentType(MediaType.APPLICATION_JSON).content(draftBody("full draft", "2026-11-02")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("NEEDS_INPUT"))
                .andExpect(jsonPath("$.data.days").isEmpty());
    }

    @Test void draftRejectsMissingOriginAndInvalidCoordinates() throws Exception {
        mvc.perform(post("/api/v1/public/planning/generate-draft").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Đà Nẵng\",\"startDate\":\"2026-11-02\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/public/planning/generate-draft").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Đà Nẵng\",\"startDate\":\"2026-11-02\",\"origin\":{\"name\":\"x\",\"latitude\":999,\"longitude\":108}}"))
                .andExpect(status().isBadRequest());
    }

    @Test void failedIntentRequestsStructuredInputAndDoesNotInventAnItinerary() throws Exception {
        when(intents.extract("provider unavailable")).thenThrow(new com.travel.planning.ai.resilience.AiCallFailure("QUOTA_EXHAUSTED", 429));
        mvc.perform(post("/api/v1/public/planning/generate-draft").contentType(MediaType.APPLICATION_JSON)
                        .content(draftBody("provider unavailable", "2026-11-02")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("NEEDS_STRUCTURED_INPUT"))
                .andExpect(jsonPath("$.data.analysis").isEmpty()).andExpect(jsonPath("$.data.days").isEmpty())
                .andExpect(jsonPath("$.data.executionReport.overallMode").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.data.executionReport.stages[0].source").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.data.executionReport.stages[0].reason").value("QUOTA_EXHAUSTED"))
                .andExpect(jsonPath("$.data.executionReport.stages[1].source").value("SKIPPED"));
        org.mockito.Mockito.verifyNoInteractions(embeddings, structured, weather);
    }

    @Test void structuredInputWorksWithAllAiUnavailableAndLabelsEveryStageTruthfully() throws Exception {
        when(embeddings.embed(anyString(), anyBoolean())).thenThrow(new com.travel.planning.ai.resilience.AiCallFailure("MISSING_API_KEY", null));
        when(structured.generate(any(com.travel.planning.ai.resilience.AiRequestContext.Stage.class), anyString(), anyString(), anyMap()))
                .thenThrow(new com.travel.planning.ai.resilience.AiCallFailure("MISSING_API_KEY", null));
        int before = jdbc.queryForObject("SELECT count(*) FROM itineraries", Integer.class);
        var result = mvc.perform(post("/api/v1/public/planning/generate-draft").contentType(MediaType.APPLICATION_JSON).content(structuredBody()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("READY_WITH_WARNINGS"))
                .andExpect(jsonPath("$.data.days.length()").value(2))
                .andExpect(jsonPath("$.data.analysis.model").isEmpty())
                .andExpect(jsonPath("$.data.executionReport.overallMode").value("FALLBACK"))
                .andExpect(jsonPath("$.data.executionReport.stages[0].source").value("USER_PROVIDED"))
                .andExpect(jsonPath("$.data.executionReport.stages[1].source").value("FALLBACK"))
                .andExpect(jsonPath("$.data.executionReport.stages[1].reason").value("MISSING_API_KEY"))
                .andExpect(jsonPath("$.data.executionReport.stages[2].source").value("FALLBACK"))
                .andExpect(jsonPath("$.data.executionReport.stages[3].source").value("DETERMINISTIC"))
                .andExpect(jsonPath("$.data.executionReport.stages[4].source").value("FALLBACK"))
                .andReturn();
        var root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        var names = new java.util.ArrayList<String>();
        for (var day : root.at("/data/days")) for (var stop : day.get("stops")) {
            names.add(stop.at("/place/name").asText());
            org.assertj.core.api.Assertions.assertThat(stop.at("/place/tags").toString()).doesNotContain("BEACH");
            org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT count(*) FROM places WHERE id=?", Integer.class,
                    java.util.UUID.fromString(stop.at("/place/placeId").asText()))).isEqualTo(1);
        }
        org.assertj.core.api.Assertions.assertThat(names).contains("Bảo tàng Điêu khắc Chăm");
        org.assertj.core.api.Assertions.assertThat(names.size()).isGreaterThanOrEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT count(*) FROM itineraries", Integer.class)).isEqualTo(before);
        org.mockito.Mockito.verifyNoInteractions(intents);
    }

    @Test void structuredInputRejectsMissingOrConflictingModesInvalidTagsTimesAndDayCount() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var base = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(structuredBody());
        var both = base.deepCopy().put("text", "Đà Nẵng");
        var neither = base.deepCopy(); neither.remove("preferences");
        var invalidTime = base.deepCopy(); ((com.fasterxml.jackson.databind.node.ObjectNode) invalidTime.get("preferences")).put("dailyStartTime", "25:00");
        var invalidTag = base.deepCopy(); ((com.fasterxml.jackson.databind.node.ObjectNode) invalidTag.get("preferences")).putArray("interests").add("INVENTED_TAG");
        var tooManyDays = base.deepCopy(); ((com.fasterxml.jackson.databind.node.ObjectNode) tooManyDays.get("preferences")).put("numberOfDays", 3);
        for (var body : java.util.List.of(both, neither, invalidTime, invalidTag, tooManyDays))
            mvc.perform(post("/api/v1/public/planning/generate-draft").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                    .andExpect(status().isBadRequest());
        org.mockito.Mockito.verifyNoInteractions(intents, embeddings, structured);
    }

    @Test void structuredPreferencesStillEnforceBusinessConflicts() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var root = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(structuredBody());
        ((com.fasterxml.jackson.databind.node.ObjectNode) root.get("preferences")).putArray("excludedTags").add("MUSEUM");
        mvc.perform(post("/api/v1/public/planning/generate-draft").contentType(MediaType.APPLICATION_JSON).content(root.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("NEEDS_INPUT"))
                .andExpect(jsonPath("$.data.days").isEmpty())
                .andExpect(jsonPath("$.data.executionReport.stages[0].source").value("USER_PROVIDED"));
        org.mockito.Mockito.verifyNoInteractions(intents, embeddings, structured);
    }

    @Test void successfulAiAllocationAndThemesAreLabelledAiWhileSolverRemainsDeterministic() throws Exception {
        float[] vector = new float[768]; vector[0] = 1;
        when(embeddings.embed(anyString(), anyBoolean())).thenReturn(vector); retrieval.index();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        when(structured.generate(eq(com.travel.planning.ai.resilience.AiRequestContext.Stage.ALLOCATION), anyString(), anyString(), anyMap()))
                .thenReturn(mapper.readTree("{\"days\":[{\"dayIndex\":1,\"placeRefs\":[\"P1\"],\"theme\":\"MIXED\",\"highlightRefs\":[\"P1\"]},{\"dayIndex\":2,\"placeRefs\":[\"P2\"],\"theme\":\"MIXED\",\"highlightRefs\":[\"P2\"]}]}"));
        mvc.perform(post("/api/v1/public/planning/generate-draft").contentType(MediaType.APPLICATION_JSON).content(structuredBody()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("READY_WITH_WARNINGS"))
                .andExpect(jsonPath("$.data.executionReport.overallMode").value("AI_ASSISTED"))
                .andExpect(jsonPath("$.data.executionReport.stages[0].source").value("USER_PROVIDED"))
                .andExpect(jsonPath("$.data.executionReport.stages[1].source").value("AI"))
                .andExpect(jsonPath("$.data.executionReport.stages[2].source").value("AI"))
                .andExpect(jsonPath("$.data.executionReport.stages[3].source").value("DETERMINISTIC"))
                .andExpect(jsonPath("$.data.executionReport.stages[4].source").value("AI"))
                .andExpect(jsonPath("$.data.days[0].summarySource").value("AI_GROUNDED_TEMPLATE"))
                .andExpect(jsonPath("$.data.executionReport.stages[4].attempts").value(0));
        org.mockito.Mockito.verify(structured, org.mockito.Mockito.never()).generate(eq(com.travel.planning.ai.resilience.AiRequestContext.Stage.THEMES), anyString(), anyString(), anyMap());
        org.mockito.Mockito.verifyNoInteractions(intents);
    }

    private String structuredBody() {
        return """
                {"startDate":"2026-11-02","origin":{"name":"Điểm neo demo","latitude":16.065,"longitude":108.223},
                 "preferences":{"destination":"Đà Nẵng","numberOfDays":2,"dailyStartTime":"08:00","dailyEndTime":"17:00",
                  "interests":["MUSEUM","HISTORY","ART","LOCAL"],"excludedTags":["BEACH"],"travelPace":"RELAXED",
                  "mustVisitNames":["Bảo tàng Điêu khắc Chăm"]}}
                """;
    }

    private void prepareDraft(TravelIntent intent) {
        float[] vector = new float[768]; vector[0] = 1;
        when(embeddings.embed(anyString(), anyBoolean())).thenReturn(vector); retrieval.index();
        when(intents.extract("full draft")).thenReturn(intent);
        when(structured.generate(any(com.travel.planning.ai.resilience.AiRequestContext.Stage.class), anyString(), anyString(), anyMap())).thenThrow(new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE));
    }
    private String draftBody(String text, String date) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("text", text, "startDate", date,
                "origin", java.util.Map.of("name", "Điểm neo demo", "latitude", 16.065, "longitude", 108.223), "roundTrip", true));
    }
}
