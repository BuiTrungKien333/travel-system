package com.travel.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.travel.TestcontainersConfiguration;
import com.travel.auth.mail.AuthEmailSender;
import com.travel.user.entity.User;
import com.travel.user.repository.UserRepository;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class PlaceDiscoveryIntegrationTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    UserRepository users;

    @Autowired
    PasswordEncoder passwords;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper json;

    @Autowired
    Clock clock;

    @MockitoBean
    AuthEmailSender sender;

    MockMvc mvc;
    String token, other;
    UUID owner, province, place, second, privatePlace, trip;

    @BeforeEach
    void setup() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        User user = account();
        owner = user.getId();
        token = login(user);
        User stranger = account();
        other = login(stranger);
        province = UUID.randomUUID();
        jdbc.update(
            "INSERT INTO provinces(id,code,name) VALUES (?,?,?)",
            province,
            UUID.randomUUID().toString().substring(0, 8),
            "Place QA"
        );
        place = insert("Hồ Xuân Hương", "Đường Trần Quốc Toản", "INTERNAL", null);
        second = insert("Quảng trường Lâm Viên", "Trần Quốc Toản, Đà Lạt", "INTERNAL", null);
        privatePlace = insert("Nhà riêng", "100/25 Quang Trung", "USER", owner);
        review(place, true);
        review(second, true);
        trip = UUID.fromString(create(token, List.of()).path("trip").path("id").asText());
    }

    User account() {
        return users.saveAndFlush(
            User.builder()
                .email(UUID.randomUUID() + "@example.com")
                .fullName("QA Places")
                .passwordHash(passwords.encode("fixture-password"))
                .emailVerifiedAt(clock.instant())
                .build()
        );
    }

    String login(User user) throws Exception {
        return data(
            mvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            json.writeValueAsString(Map.of("email", user.getEmail(), "password", "fixture-password"))
                        )
                )
                .andExpect(status().isOk())
        )
            .path("accessToken")
            .asText();
    }

    UUID insert(String name, String address, String source, UUID user) {
        UUID id = UUID.randomUUID();
        jdbc.update(
            "INSERT INTO places(id,name,address,latitude,longitude,province_id,source,owner_id) VALUES (?,?,?,11.94,108.44,?,?,?)",
            id,
            name,
            address,
            province,
            source,
            user
        );
        return id;
    }

    void review(UUID id, boolean approved) {
        jdbc.update(
            """
            INSERT INTO place_retrieval_profiles(place_id,description,tags,estimated_duration_minutes,source_url,reviewed_on,verification_scope,approved)
            VALUES (?,'Curated fixture',ARRAY['test'],60,'https://example.com/review',CURRENT_DATE,'TEST',?)
            """,
            id,
            approved
        );
    }

    JsonNode data(ResultActions action) throws Exception {
        return json.readTree(action.andReturn().getResponse().getContentAsString()).path("data");
    }

    String createBody(List<UUID> places) throws Exception {
        LocalDate start = LocalDate.now(clock.withZone(ZoneId.of("Asia/Ho_Chi_Minh"))).plusDays(3);
        return json.writeValueAsString(
            Map.of(
                "clientRequestId",
                UUID.randomUUID(),
                "title",
                "QA Places",
                "destination",
                Map.of("name", "Đà Lạt", "provinceId", province),
                "startDate",
                start.toString(),
                "endDate",
                start.plusDays(2).toString(),
                "timeZone",
                "Asia/Ho_Chi_Minh",
                "preferences",
                List.of(),
                "initialPlaceIds",
                places
            )
        );
    }

    JsonNode create(String auth, List<UUID> places) throws Exception {
        var result = data(
            mvc
                .perform(
                    post("/api/v1/trips")
                        .header("Authorization", "Bearer " + auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(places))
                )
                .andExpect(status().isCreated())
        );
        return result;
    }

    ResultActions suggestions(String auth) throws Exception {
        return mvc.perform(
            get("/api/v1/places/suggestions")
                .header("Authorization", "Bearer " + auth)
                .param("tripId", trip.toString())
        );
    }

    ResultActions batch(String auth, List<UUID> ids) throws Exception {
        return mvc.perform(
            post("/api/v1/trips/{id}/places/batch", trip)
                .header("Authorization", "Bearer " + auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("placeIds", ids)))
        );
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/places/suggestions")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/places/{id}", place)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/places/addresses").param("q", "Quang Trung")).andExpect(status().isUnauthorized());
    }

    @Test
    void unconfiguredMapboxReturnsActionableFailureWithoutCachingOrWrites() throws Exception {
        long before = jdbc.queryForObject("SELECT count(*) FROM places", Long.class);
        mvc.perform(
            get("/api/v1/places/addresses")
                .header("Authorization", "Bearer " + token)
                .param("q", "100/25 Quang Trung")
        )
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("PLACE_004"))
            .andExpect(header().string("Cache-Control", "private, no-store"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM places", Long.class)).isEqualTo(before);
    }

    @Test
    void provinceSuggestionsAreBoundedAndDoNotExposePrivatePlaces() throws Exception {
        var page = data(
            mvc
                .perform(
                    get("/api/v1/places/suggestions")
                        .header("Authorization", "Bearer " + token)
                        .param("provinceId", province.toString())
                        .param("size", "1")
                )
                .andExpect(status().isOk())
        );
        assertThat(page.path("content").size()).isEqualTo(1);
        assertThat(page.path("totalElements").asInt()).isEqualTo(2);
        assertThat(page.path("content").get(0).path("id").asText()).isEqualTo(place.toString());
        assertThat(page.path("content").get(0).has("ownerId")).isFalse();
    }

    @Test
    void suggestionsOnlyUseApprovedPublicProfilesAndRecheckRevocations() throws Exception {
        UUID pending = insert("Pending public place", "Pending", "INTERNAL", null);
        review(pending, false);
        insert("No profile", "Not reviewed", "INTERNAL", null);
        review(privatePlace, true); // A profile must not override a newly entered private place's visibility.
        suggestions(token).andExpect(jsonPath("$.data.totalElements").value(2));
        jdbc.update("UPDATE place_retrieval_profiles SET approved=false WHERE place_id=?", place);
        suggestions(token)
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.content[0].id").value(second.toString()));
        jdbc.update("DELETE FROM place_retrieval_profiles WHERE place_id=?", second);
        suggestions(token)
            .andExpect(jsonPath("$.data.totalElements").value(0))
            .andExpect(jsonPath("$.data.reason").value("EMPTY_CATALOG"));
    }

    @Test
    void accentInsensitiveLiteralSearchAndPagingValidation() throws Exception {
        mvc.perform(
            get("/api/v1/places/suggestions")
                .header("Authorization", "Bearer " + token)
                .param("provinceId", province.toString())
                .param("q", "  HO XUAN HUONG  ")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(1));
        for (String text : List.of("%", "_", "\\"))
            mvc.perform(
                get("/api/v1/places/suggestions")
                    .header("Authorization", "Bearer " + token)
                    .param("provinceId", province.toString())
                    .param("q", text)
            ).andExpect(jsonPath("$.data.totalElements").value(0));
        mvc.perform(
            get("/api/v1/places/suggestions")
                .header("Authorization", "Bearer " + token)
                .param("size", "101")
        ).andExpect(status().isBadRequest());
        mvc.perform(
            get("/api/v1/places/suggestions")
                .header("Authorization", "Bearer " + token)
                .param("locale", "fr")
        ).andExpect(status().isBadRequest());
    }

    @Test
    void ownerAndDeletedTripAreEnforcedBeforeReadingCatalog() throws Exception {
        suggestions(other).andExpect(status().isNotFound());
        jdbc.update("UPDATE trips SET deleted_at=now() WHERE id=?", trip);
        suggestions(token).andExpect(status().isNotFound());
    }

    @Test
    void unlinkedDestinationIsAnExplicitEmptyResult() throws Exception {
        jdbc.update("UPDATE trips SET destination_province_id=null WHERE id=?", trip);
        suggestions(token)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reason").value("DESTINATION_NOT_LINKED"));
    }

    @Test
    void unknownDetailStaysUnknownAndPrivateDetailDoesNotLeak() throws Exception {
        mvc.perform(get("/api/v1/places/{id}", place).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.openingStatus").value("UNKNOWN"))
            .andExpect(jsonPath("$.data.admission").isEmpty())
            .andExpect(jsonPath("$.data.photos.length()").value(0));
        mvc.perform(get("/api/v1/places/{id}", privatePlace).header("Authorization", "Bearer " + token)).andExpect(
            status().isOk()
        );
        mvc.perform(get("/api/v1/places/{id}", privatePlace).header("Authorization", "Bearer " + other)).andExpect(
            status().isNotFound()
        );
        mvc.perform(get("/api/v1/places/{id}", UUID.randomUUID()).header("Authorization", "Bearer " + other)).andExpect(
            status().isNotFound()
        );
    }

    @Test
    void initialPlacesPersistAndForeignPrivateReferenceRollsBack() throws Exception {
        var created = create(token, List.of(place, privatePlace));
        assertThat(created.path("trip").path("readiness").path("unscheduledCount").asInt()).isEqualTo(2);
        mvc.perform(
            post("/api/v1/trips")
                .header("Authorization", "Bearer " + other)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody(List.of(privatePlace)))
        ).andExpect(status().isBadRequest());
    }

    @Test
    void batchIsAtomicRetryableAndExcludesAlreadySavedPlaces() throws Exception {
        batch(token, List.of(place, UUID.randomUUID())).andExpect(status().isNotFound());
        assertThat(
            jdbc.queryForObject("SELECT count(*) FROM trip_places WHERE trip_id=?", Integer.class, trip)
        ).isZero();
        var first = data(batch(token, List.of(place, second)).andExpect(status().isOk()));
        var replay = data(batch(token, List.of(place, second)).andExpect(status().isOk()));
        for (int i = 0; i < 2; i++) {
            assertThat(replay.get(i).path("id")).isEqualTo(first.get(i).path("id"));
            assertThat(replay.get(i).path("plannedOrder")).isEqualTo(first.get(i).path("plannedOrder"));
        }
        assertThat(
            jdbc.queryForObject("SELECT count(*) FROM trip_places WHERE trip_id=?", Integer.class, trip)
        ).isEqualTo(2);
        suggestions(token).andExpect(jsonPath("$.data.totalElements").value(0));
        batch(other, List.of(place)).andExpect(status().isNotFound());
        batch(token, List.of(place, place)).andExpect(status().isBadRequest());
        mvc.perform(
            post("/api/v1/trips/{id}/places/batch", trip)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("placeIds", List.of(place), "ownerId", UUID.randomUUID())))
        ).andExpect(status().isBadRequest());
    }

    @Test
    void cancelledTripsCannotBeChangedByPlaceEndpoints() throws Exception {
        batch(token, List.of(place)).andExpect(status().isOk());
        jdbc.update("UPDATE trips SET status='CANCELLED' WHERE id=?", trip);
        batch(token, List.of(second)).andExpect(status().isConflict());
        mvc.perform(
            post("/api/v1/trips/{id}/places", trip)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("placeId", second)))
        ).andExpect(status().isConflict());
        mvc.perform(
            put("/api/v1/trips/{trip}/places/{place}", trip, place)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"notes\":\"No edit\"}")
        ).andExpect(status().isConflict());
        mvc.perform(
            delete("/api/v1/trips/{trip}/places/{place}", trip, place).header("Authorization", "Bearer " + token)
        ).andExpect(status().isConflict());
    }

    @Test
    void legacyManualWritesArePrivateAndCannotBeAddedByAnotherOwner() throws Exception {
        var created = data(
            mvc
                .perform(
                    post("/api/v1/trips/{id}/places", trip)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"My home\",\"latitude\":10.8,\"longitude\":106.6}")
                )
                .andExpect(status().isCreated())
        );
        UUID id = UUID.fromString(created.path("placeId").asText());
        assertThat(jdbc.queryForObject("SELECT source FROM places WHERE id=?", String.class, id)).isEqualTo("USER");
        UUID theirs = UUID.fromString(create(other, List.of()).path("trip").path("id").asText());
        mvc.perform(
            post("/api/v1/trips/{id}/places", theirs)
                .header("Authorization", "Bearer " + other)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("placeId", id)))
        ).andExpect(status().isNotFound());
    }
}
