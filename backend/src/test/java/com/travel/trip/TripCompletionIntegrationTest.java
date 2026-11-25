package com.travel.trip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.TestcontainersConfiguration;
import com.travel.auth.mail.AuthEmailSender;
import com.travel.user.entity.User;
import com.travel.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Import({ TestcontainersConfiguration.class, TripCrudIntegrationTest.FixedTime.class })
class TripCompletionIntegrationTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    UserRepository users;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper json;

    @MockitoBean
    AuthEmailSender mail;

    MockMvc mvc;
    String token;

    @BeforeEach
    void setup() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        token = loginNewUser();
    }

    String loginNewUser() throws Exception {
        String password = UUID.randomUUID() + ".Ab1";
        var user = users.saveAndFlush(
            User.builder()
                .email(UUID.randomUUID() + "@example.com")
                .fullName("Trip tester")
                .passwordHash(encoder.encode(password))
                .emailVerifiedAt(Instant.now())
                .build()
        );
        return data(
            mvc
                .perform(
                    post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", user.getEmail(), "password", password)))
                )
                .andExpect(status().isOk())
        )
            .path("accessToken")
            .asText();
    }

    JsonNode data(ResultActions action) throws Exception {
        return json.readTree(action.andReturn().getResponse().getContentAsString()).path("data");
    }

    JsonNode trip(String start) throws Exception {
        return data(
            mvc
                .perform(
                    post("/api/v1/trips")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            json.writeValueAsString(
                                Map.of(
                                    "clientRequestId",
                                    UUID.randomUUID(),
                                    "destination",
                                    Map.of("name", "Đà Lạt"),
                                    "startDate",
                                    start,
                                    "endDate",
                                    "2026-11-24"
                                )
                            )
                        )
                )
                .andExpect(status().isCreated())
        ).path("trip");
    }

    ResultActions complete(String id, String body) throws Exception {
        return mvc.perform(
            post("/api/v1/trips/" + id + "/complete")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        );
    }

    @Test
    void completeLiveTripPreservesDatesPlacesAndDayHistoryAndUpdatesHomeAndList() throws Exception {
        var before = trip("2026-11-02");
        UUID id = UUID.fromString(before.path("id").asText());
        UUID place = UUID.randomUUID();
        jdbc.update("INSERT INTO places(id,name,latitude,longitude) VALUES (?,'Stop',11.94,108.44)", place);
        jdbc.update(
            "INSERT INTO trip_places(trip_id,place_id,visited,estimated_duration,notes) VALUES (?,?,false,60,'Keep this')",
            id,
            place
        );

        var result = data(complete(id.toString(), "{\"version\":0}").andExpect(status().isOk())).path("trip");
        assertThat(result.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(result.path("mode").asText()).isEqualTo("RECAP");
        assertThat(result.path("version").asLong()).isEqualTo(1);
        assertThat(result.path("startDate")).isEqualTo(before.path("startDate"));
        assertThat(result.path("endDate")).isEqualTo(before.path("endDate"));
        assertThat(result.path("days")).isEqualTo(before.path("days"));
        assertThat(
            jdbc.queryForObject(
                "SELECT visited FROM trip_places WHERE trip_id=? AND place_id=?",
                Boolean.class,
                id,
                place
            )
        ).isFalse();
        assertThat(
            jdbc.queryForObject("SELECT notes FROM trip_places WHERE trip_id=? AND place_id=?", String.class, id, place)
        ).isEqualTo("Keep this");
        mvc.perform(get("/api/v1/home/dashboard").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.state").value("RETURNING"));
        var page = data(
            mvc
                .perform(
                    get("/api/v1/trips")
                        .param("mode", "RECAP")
                        .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
        );
        assertThat(page.path("content").get(0).path("id").asText()).isEqualTo(id.toString());
        mvc.perform(
            put("/api/v1/trips/" + id + "/itinerary/days/2026-11-02")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedTripVersion\":1,\"stops\":[]}")
        ).andExpect(status().isConflict());
    }

    @Test
    void retryCompletionWithOriginalVersionDoesNotWriteAgain() throws Exception {
        String id = trip("2026-11-02").path("id").asText();
        var first = data(complete(id, "{\"version\":0}").andExpect(status().isOk()));
        var second = data(complete(id, "{\"version\":0}").andExpect(status().isOk()));
        assertThat(second).isEqualTo(first);
    }

    @Test
    void staleVersionDoesNotComplete() throws Exception {
        String id = trip("2026-11-02").path("id").asText();
        mvc.perform(
            patch("/api/v1/trips/" + id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0,\"title\":\"Updated\"}")
        ).andExpect(status().isOk());
        complete(id, "{\"version\":0}")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("TRIP_002"));
        assertThat(
            jdbc.queryForObject("SELECT status FROM trips WHERE id=?", String.class, UUID.fromString(id))
        ).isEqualTo("PLANNING");
    }

    @Test
    void plannedCancelledAndExpiredTripsCannotBeCompletedEarly() throws Exception {
        String id = trip("2026-11-20").path("id").asText();
        complete(id, "{\"version\":0}")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("TRIP_006"));
        jdbc.update("UPDATE trips SET status='CANCELLED' WHERE id=?", UUID.fromString(id));
        complete(id, "{\"version\":0}").andExpect(status().isConflict());
        jdbc.update(
            "UPDATE trips SET status='PLANNING',start_date='2026-10-30',end_date='2026-11-01' WHERE id=?",
            UUID.fromString(id)
        );
        complete(id, "{\"version\":0}").andExpect(status().isConflict());
    }

    @Test
    void anotherOwnerAndDeletedTripAreNotAccessible() throws Exception {
        String id = trip("2026-11-02").path("id").asText();
        String original = token;
        token = loginNewUser();
        complete(id, "{\"version\":0}").andExpect(status().isNotFound());
        token = original;
        mvc.perform(
            delete("/api/v1/trips/" + id)
                .param("version", "0")
                .header("Authorization", "Bearer " + token)
        ).andExpect(status().isNoContent());
        complete(id, "{\"version\":0}").andExpect(status().isNotFound());
    }

    @Test
    void requestRequiresAuthenticationAndStrictNonnegativeIntegerVersion() throws Exception {
        String id = trip("2026-11-02").path("id").asText();
        mvc.perform(
            post("/api/v1/trips/" + id + "/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0}")
        ).andExpect(status().isUnauthorized());
        for (String body : List.of(
            "{}",
            "{\"version\":null}",
            "{\"version\":-1}",
            "{\"version\":0.5}",
            "{\"version\":\"0\"}",
            "{\"version\":0,\"status\":\"COMPLETED\"}"
        )) {
            complete(id, body).andExpect(status().isBadRequest());
        }
    }

    @Test
    void visitingEveryPlaceDoesNotCompleteTheTrip() throws Exception {
        String id = trip("2026-11-02").path("id").asText();
        UUID place = UUID.randomUUID();
        jdbc.update("INSERT INTO places(id,name,latitude,longitude) VALUES (?,'Stop',11.94,108.44)", place);
        jdbc.update("INSERT INTO trip_places(trip_id,place_id,visited) VALUES (?,?,true)", UUID.fromString(id), place);
        mvc.perform(get("/api/v1/trips/" + id).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.trip.mode").value("LIVE"));
    }
}
