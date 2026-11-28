package com.travel.planning.trip;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.maps.service.MapboxDirectionsService;
import com.travel.planning.ai.provider.GeminiStructuredClient;
import com.travel.planning.ai.resilience.AiCallFailure;
import com.travel.planning.retrieval.provider.GeminiEmbeddingClient;
import com.travel.security.model.CustomUserDetails;
import com.travel.trip.entity.Trip;
import com.travel.trip.repository.TripRepository;
import com.travel.user.entity.User;
import com.travel.user.enums.Role;
import com.travel.user.repository.UserRepository;
import com.travel.weather.service.WeatherService;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles({ "test", "planning-ai" })
class TripDraftIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:0.8.2-pg17").asCompatibleSubstituteFor("postgres")
    );

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    WebApplicationContext context;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper json;

    @Autowired
    UserRepository users;

    @Autowired
    TripRepository trips;

    @MockitoBean
    GeminiEmbeddingClient embeddings;

    @MockitoBean
    GeminiStructuredClient structured;

    @MockitoBean
    WeatherService weather;

    @MockitoBean
    MapboxDirectionsService maps;

    MockMvc mvc;
    User owner;
    Trip trip;
    LocalDate start;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = users.saveAndFlush(
            User.builder()
                .email(UUID.randomUUID() + "@example.com")
                .fullName("Draft owner")
                .passwordHash("unused-test-hash")
                .role(Role.USER)
                .emailVerifiedAt(Instant.now())
                .build()
        );
        start = LocalDate.now(com.travel.trip.service.TripCalendar.ZONE).plusDays(30);
        trip = trips.saveAndFlush(
            Trip.builder()
                .user(owner)
                .name("AI trip")
                .destination("Đà Nẵng")
                .startDate(start)
                .endDate(start.plusDays(1))
                .build()
        );
        when(embeddings.embed(anyString(), anyBoolean())).thenThrow(new AiCallFailure("TEST_UNAVAILABLE", null));
        when(structured.generate(any(), anyString(), anyString(), anyMap())).thenThrow(
            new AiCallFailure("TEST_UNAVAILABLE", null)
        );
        when(maps.matrix(anyList(), any())).thenThrow(
            new com.travel.common.exception.AppException(
                com.travel.common.exception.ErrorCode.PLACE_PROVIDER_UNAVAILABLE
            )
        );
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void previewReopensWithoutMutatingTripAndApplyIsIdempotent() throws Exception {
        String body = body(UUID.randomUUID());
        JsonNode draft = create(body);
        java.nio.file.Files.writeString(
            java.nio.file.Path.of("target/trip-draft-contract.json"),
            json.writeValueAsString(draft)
        );
        assertThat(draft.path("canApply").asBoolean()).isTrue();
        assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM itinerary_items i JOIN itineraries t ON t.id=i.itinerary_id WHERE t.trip_id=?",
                Integer.class,
                trip.getId()
            )
        ).isZero();
        assertThat(
            jdbc.queryForObject("SELECT count(*) FROM trip_places WHERE trip_id=?", Integer.class, trip.getId())
        ).isZero();
        send(get(path() + "/latest"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(draft.path("id").asText()));
        assertThat(create(body).path("id").asText()).isEqualTo(draft.path("id").asText());
        String apply = path() + "/" + draft.path("id").asText() + "/apply";
        var saved = data(send(post(apply)).andExpect(status().isOk()));
        assertThat(saved.path("days")).hasSize(2);
        assertThat(saved.at("/days/0/settings/origin/name").asText()).isEqualTo("Origin");
        assertThat(saved.at("/days/0/stops/0/arrivalTime").asText()).isNotBlank();
        assertThat(data(send(post(apply)).andExpect(status().isOk())).path("id")).isEqualTo(saved.path("id"));
        assertThat(
            jdbc.queryForObject("SELECT count(*) FROM itineraries WHERE trip_id=?", Integer.class, trip.getId())
        ).isEqualTo(1);
        assertThat(
            jdbc.queryForObject(
                "SELECT applied_at IS NOT NULL FROM trip_planning_drafts WHERE id=?",
                Boolean.class,
                UUID.fromString(draft.path("id").asText())
            )
        ).isTrue();
        send(get("/api/v1/trips/" + trip.getId() + "/itinerary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.days[0].stops[0].placeId").isNotEmpty());
    }

    @Test
    void rejectsAnonymousAndOtherOwnerWithoutExposingDraft() throws Exception {
        mvc.perform(post(path()).contentType(MediaType.APPLICATION_JSON).content(body(UUID.randomUUID()))).andExpect(
            status().isUnauthorized()
        );
        var draft = create(body(UUID.randomUUID()));
        var outsider = new CustomUserDetails(
            UUID.randomUUID(),
            "outsider@example.com",
            Role.USER,
            List.of(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"),
                new org.springframework.security.core.authority.SimpleGrantedAuthority("planning:generate")
            )
        );
        mvc.perform(get(path() + "/" + draft.path("id").asText()).with(user(outsider))).andExpect(
            status().isNotFound()
        );
        mvc.perform(post(path() + "/" + draft.path("id").asText() + "/apply").with(user(outsider))).andExpect(
            status().isNotFound()
        );
    }

    @Test
    void roadMetricsSurvivePreviewReopenAndApply() throws Exception {
        doAnswer(call -> {
            int count = ((List<?>) call.getArgument(0)).size();
            double[][] distance = new double[count][count],
                duration = new double[count][count];
            for (int from = 0; from < count; from++) for (int to = 0; to < count; to++) if (from != to) {
                distance[from][to] = 1400;
                duration[from][to] = 420;
            }
            return new MapboxDirectionsService.RoadMatrix(distance, duration);
        })
            .when(maps)
            .matrix(anyList(), any());
        var draft = create(body(UUID.randomUUID()));
        assertThat(draft.at("/proposal/metadata/routeSource").asText()).isEqualTo("MAPBOX_DRIVING");
        assertThat(draft.at("/proposal/metadata/optimizationScope").asText()).isEqualTo(
            "HELD_KARP_PER_DAY_ROAD_DURATION"
        );
        assertThat(draft.at("/proposal/days/0/stops/0/transitMinutes").asInt()).isEqualTo(7);
        assertThat(draft.at("/proposal/days/0/returnToOrigin/minutes").asInt()).isEqualTo(7);
        send(get(path() + "/latest")).andExpect(
            jsonPath("$.data.proposal.metadata.routeSource").value("MAPBOX_DRIVING")
        );
        send(post(path() + "/" + draft.path("id").asText() + "/apply"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.days[0].stops[0].travelMinutesFromPrevious").value(7))
            .andExpect(jsonPath("$.data.days[0].stops[0].distanceFromPreviousKm").value(1.4));
        verify(maps, times(1)).matrix(anyList(), any());
    }

    @Test
    void aMissingRoadRouteStaysBlockingInTheStoredDraft() throws Exception {
        doThrow(new com.travel.common.exception.AppException(com.travel.common.exception.ErrorCode.RESOURCE_NOT_FOUND))
            .when(maps)
            .matrix(anyList(), any());
        var draft = create(body(UUID.randomUUID()));
        assertThat(draft.path("canApply").asBoolean()).isFalse();
        assertThat(draft.at("/proposal/status").asText()).isEqualTo("CONFLICTS_DETECTED");
        assertThat(draft.at("/proposal/metadata/routeStatus").asText()).isEqualTo("UNREACHABLE");
        assertThat(draft.at("/proposal/metadata/routeSource").asText()).isEqualTo("MAPBOX_DRIVING");
        send(post(path() + "/" + draft.path("id").asText() + "/apply")).andExpect(status().isUnprocessableEntity());
        assertThat(
            jdbc.queryForObject("SELECT count(*) FROM trip_places WHERE trip_id=?", Integer.class, trip.getId())
        ).isZero();
    }

    @Test
    void profileDoesNotExposePublicDemoControllers() throws Exception {
        send(
            post("/api/v1/public/planning/generate-draft").contentType(MediaType.APPLICATION_JSON).content("{}")
        ).andExpect(status().isNotFound());
    }

    @Test
    void onlyAnAdministratorCanPrepareTheProductionRagIndex() throws Exception {
        String endpoint = "/api/v1/admin/planning/retrieval/index";
        mvc.perform(post(endpoint)).andExpect(status().isUnauthorized());
        send(post(endpoint)).andExpect(status().isForbidden());
        float[] vector = new float[768];
        vector[0] = 1;
        doReturn(vector).when(embeddings).embed(anyString(), eq(true));
        var admin = new CustomUserDetails(
            owner.getId(),
            owner.getEmail(),
            Role.ADMIN,
            List.of(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"),
                new org.springframework.security.core.authority.SimpleGrantedAuthority("admin:ai:index")
            )
        );
        mvc.perform(post(endpoint).with(user(admin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.failed").value(0));
        clearInvocations(embeddings);
        mvc.perform(post(endpoint).with(user(admin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.updated").value(0));
        verifyNoInteractions(embeddings);
    }

    @Test
    void rejectsChangedTripAndExpiredDraftWithoutPartialWrites() throws Exception {
        var draft = create(body(UUID.randomUUID()));
        jdbc.update("UPDATE trips SET version=version+1 WHERE id=?", trip.getId());
        send(post(path() + "/" + draft.path("id").asText() + "/apply"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("TRIP_008"));
        assertThat(
            jdbc.queryForObject("SELECT count(*) FROM trip_places WHERE trip_id=?", Integer.class, trip.getId())
        ).isZero();
        jdbc.update(
            "UPDATE trip_planning_drafts SET expires_at=now()-interval '1 minute' WHERE id=?",
            UUID.fromString(draft.path("id").asText())
        );
        send(post(path() + "/" + draft.path("id").asText() + "/apply"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DRAFT_002"));
    }

    @Test
    void revokedCatalogEntryCannotBeApplied() throws Exception {
        var draft = create(body(UUID.randomUUID()));
        UUID placeId = UUID.fromString(draft.at("/proposal/days/0/stops/0/place/placeId").asText());
        try {
            jdbc.update("UPDATE place_retrieval_profiles SET approved=false WHERE place_id=?", placeId);
            send(post(path() + "/" + draft.path("id").asText() + "/apply")).andExpect(status().isConflict());
            assertThat(
                jdbc.queryForObject("SELECT count(*) FROM trip_places WHERE trip_id=?", Integer.class, trip.getId())
            ).isZero();
        } finally {
            jdbc.update("UPDATE place_retrieval_profiles SET approved=true WHERE place_id=?", placeId);
        }
    }

    @Test
    void concurrentAppliesCreateOneItinerary() throws Exception {
        var draft = create(body(UUID.randomUUID()));
        String uri = path() + "/" + draft.path("id").asText() + "/apply";
        var gate = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Integer> apply = () -> {
                gate.await(5, TimeUnit.SECONDS);
                return send(post(uri)).andReturn().getResponse().getStatus();
            };
            var one = pool.submit(apply);
            var two = pool.submit(apply);
            gate.countDown();
            assertThat(one.get(20, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(two.get(20, TimeUnit.SECONDS)).isEqualTo(200);
        }
        assertThat(
            jdbc.queryForObject("SELECT count(*) FROM itineraries WHERE trip_id=?", Integer.class, trip.getId())
        ).isEqualTo(1);
    }

    @Test
    void changedScheduleDuringGenerationIsNotOverwritten() throws Exception {
        doAnswer(call -> {
            jdbc.update("UPDATE trips SET version=version+1 WHERE id=?", trip.getId());
            throw new AiCallFailure("TEST_UNAVAILABLE", null);
        })
            .when(structured)
            .generate(any(), anyString(), anyString(), anyMap());
        send(post(path()).contentType(MediaType.APPLICATION_JSON).content(body(UUID.randomUUID()))).andExpect(
            status().isConflict()
        );
        assertThat(
            jdbc.queryForObject("SELECT count(*) FROM trip_places WHERE trip_id=?", Integer.class, trip.getId())
        ).isZero();
    }

    @Test
    void reusedRequestWithDifferentInputAndInvalidDatesAreRejected() throws Exception {
        UUID requestId = UUID.randomUUID();
        create(body(requestId));
        send(
            post(path()).contentType(MediaType.APPLICATION_JSON).content(body(requestId).replace("17:00", "18:00"))
        ).andExpect(status().isConflict());
        send(
            post(path())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(UUID.randomUUID()).replace(start.toString(), start.plusDays(1).toString()))
        ).andExpect(status().isBadRequest());
    }

    @Test
    void existingScheduleIsReplacedOnlyAfterExplicitConfirmation() throws Exception {
        var draft = create(body(UUID.randomUUID()));
        send(post(path() + "/" + draft.path("id").asText() + "/apply")).andExpect(status().isOk());
        String itineraryPath = "/api/v1/trips/" + trip.getId() + "/itinerary";
        var before = data(send(get(itineraryPath)).andExpect(status().isOk()));
        var replacement = create(body(UUID.randomUUID()).replace("\"name\":\"Origin\"", "\"name\":\"New origin\""));
        assertThat(replacement.path("canApply").asBoolean()).isTrue();
        assertThat(data(send(get(itineraryPath)).andExpect(status().isOk()))).isEqualTo(before);

        String apply = path() + "/" + replacement.path("id").asText() + "/apply";
        send(post(apply)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DRAFT_006"));
        assertThat(data(send(get(itineraryPath)).andExpect(status().isOk()))).isEqualTo(before);

        var saved = data(send(post(apply).param("replaceExisting", "true")).andExpect(status().isOk()));
        assertThat(saved.path("id")).isEqualTo(before.path("id"));
        assertThat(saved.at("/days/0/settings/origin/name").asText()).isEqualTo("New origin");
        assertThat(saved.at("/days/0/stops").size()).isEqualTo(replacement.at("/proposal/days/0/stops").size());
    }

    @Test
    void infeasibleRequiredPlacesCannotBeApplied() throws Exception {
        var draft = create(body(UUID.randomUUID()).replace("17:00", "08:30"));
        assertThat(draft.path("canApply").asBoolean()).isFalse();
        send(post(path() + "/" + draft.path("id").asText() + "/apply")).andExpect(status().isUnprocessableEntity());
        assertThat(
            jdbc.queryForObject("SELECT count(*) FROM trip_places WHERE trip_id=?", Integer.class, trip.getId())
        ).isZero();
    }

    @Test
    void generationBudgetAndPendingRetriesDoNotCallProviderAgain() throws Exception {
        UUID request = UUID.randomUUID();
        create(body(request));
        jdbc.update("UPDATE trip_planning_drafts SET response_json=NULL WHERE trip_id=?", trip.getId());
        clearInvocations(embeddings, structured);
        send(post(path()).contentType(MediaType.APPLICATION_JSON).content(body(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DRAFT_003"));
        verifyNoInteractions(embeddings, structured);
        create(body(UUID.randomUUID()));
        create(body(UUID.randomUUID()));
        clearInvocations(embeddings, structured);
        send(post(path()).contentType(MediaType.APPLICATION_JSON).content(body(UUID.randomUUID())))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value("AI_003"));
        verifyNoInteractions(embeddings, structured);
    }

    @Test
    void reopeningTracksAnInFlightRequestAndAnAbandonedRequestDoesNotStayPendingForever() throws Exception {
        UUID request = UUID.randomUUID();
        create(body(request));
        jdbc.update("UPDATE trip_planning_drafts SET response_json=NULL WHERE trip_id=?", trip.getId());
        send(get(path() + "/latest"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DRAFT_003"));
        jdbc.update(
            "UPDATE trip_planning_drafts SET created_at=now()-interval '3 minutes' WHERE trip_id=?",
            trip.getId()
        );
        send(get(path() + "/latest"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DRAFT_005"));
        clearInvocations(embeddings, structured);
        send(post(path()).contentType(MediaType.APPLICATION_JSON).content(body(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DRAFT_005"));
        verifyNoInteractions(embeddings, structured);
    }

    @Test
    void unexpectedGenerationFailureIsRecordedAndCannotRestartOnAnAutomaticRetry() throws Exception {
        UUID request = UUID.randomUUID();
        doThrow(new RuntimeException("controlled test failure"))
            .when(structured)
            .generate(any(), anyString(), anyString(), anyMap());
        send(post(path()).contentType(MediaType.APPLICATION_JSON).content(body(request))).andExpect(
            status().isInternalServerError()
        );
        assertThat(
            jdbc.queryForObject(
                "SELECT failure_code FROM trip_planning_drafts WHERE trip_id=?",
                String.class,
                trip.getId()
            )
        ).isEqualTo("INTERNAL_SERVER_ERROR");
        clearInvocations(embeddings, structured);
        send(post(path()).contentType(MediaType.APPLICATION_JSON).content(body(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DRAFT_005"));
        verifyNoInteractions(embeddings, structured);
        assertThat(
            jdbc.queryForObject("SELECT count(*) FROM trip_places WHERE trip_id=?", Integer.class, trip.getId())
        ).isZero();
    }

    @Test
    void databaseFailureRollsBackNewMembershipAndItineraryTogether() throws Exception {
        var draft = create(body(UUID.randomUUID()));
        jdbc.execute(
            "ALTER TABLE itinerary_items ADD CONSTRAINT draft_rollback_test CHECK (duration_minutes < 0) NOT VALID"
        );
        try {
            send(post(path() + "/" + draft.path("id").asText() + "/apply")).andExpect(status().is5xxServerError());
            assertThat(
                jdbc.queryForObject("SELECT count(*) FROM trip_places WHERE trip_id=?", Integer.class, trip.getId())
            ).isZero();
            assertThat(
                jdbc.queryForObject("SELECT count(*) FROM itineraries WHERE trip_id=?", Integer.class, trip.getId())
            ).isZero();
            assertThat(
                jdbc.queryForObject(
                    "SELECT applied_at IS NULL FROM trip_planning_drafts WHERE id=?",
                    Boolean.class,
                    UUID.fromString(draft.path("id").asText())
                )
            ).isTrue();
        } finally {
            jdbc.execute("ALTER TABLE itinerary_items DROP CONSTRAINT draft_rollback_test");
        }
        send(post(path() + "/" + draft.path("id").asText() + "/apply")).andExpect(status().isOk());
    }

    String path() {
        return "/api/v1/trips/" + trip.getId() + "/planning/drafts";
    }

    String body(UUID requestId) {
        return """
        {"clientRequestId":"%s","expectedTripVersion":0,"input":{
         "startDate":"%s","origin":{"name":"Origin","latitude":16.065,"longitude":108.223},
         "roundTrip":true,"restMinutes":45,"preferences":{"destination":"Đà Nẵng","numberOfDays":2,
         "interests":["MUSEUM","HISTORY","ART","LOCAL"],"excludedTags":["BEACH"],
         "dailyStartTime":"08:00","dailyEndTime":"17:00","travelPace":"RELAXED",
         "mustVisitNames":["Bảo tàng Điêu khắc Chăm"],"excludedPlaceNames":[]}}}
        """.formatted(requestId, start);
    }

    JsonNode create(String body) throws Exception {
        return data(
            send(post(path()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk())
        );
    }

    ResultActions send(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(
            request.with(user(new CustomUserDetails(owner, java.util.Set.of("planning:generate", "itinerary:manage"))))
        );
    }

    JsonNode data(ResultActions result) throws Exception {
        return json
            .readTree(result.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
            .path("data");
    }
}
