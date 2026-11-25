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
class DestinationCatalogIntegrationTest {

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

    private MockMvc mvc;
    private String token;
    private String prefix;
    private final List<UUID> ids = new ArrayList<>();
    private String code;

    @BeforeEach
    void setup() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        var user = users.saveAndFlush(
            User.builder()
                .email(UUID.randomUUID() + "@example.com")
                .fullName("Fixture")
                .passwordHash(passwords.encode("fixture-password"))
                .emailVerifiedAt(clock.instant())
                .build()
        );
        token = data(
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
        prefix = "catalog-" + UUID.randomUUID().toString().substring(0, 8);
        code = UUID.randomUUID().toString().replace("-", "").substring(0, 7);
        for (int i = 0; i < 3; i++) {
            UUID id = UUID.randomUUID();
            ids.add(id);
            jdbc.update(
                "INSERT INTO provinces(id,code,name) VALUES (?,?,?)",
                id,
                code + i,
                prefix + List.of(" Lâm Đồng", " Hà Nội", " 100%_\\poi").get(i)
            );
        }
    }

    private JsonNode data(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString()).path("data");
    }

    private ResultActions list(String query, int page, int size) throws Exception {
        return mvc.perform(
            get("/api/v1/destinations")
                .header("Authorization", "Bearer " + token)
                .param("q", query)
                .param("page", "" + page)
                .param("size", "" + size)
        );
    }

    @Test
    void catalogAllowsGuestDiscovery() throws Exception {
        mvc.perform(get("/api/v1/destinations")).andExpect(status().isOk());
    }

    @Test
    void paginationHasStableOrderBoundedContentAndContext() throws Exception {
        var first = data(list(prefix, 0, 2).andExpect(status().isOk()));
        var second = data(list(prefix, 1, 2).andExpect(status().isOk()));
        assertThat(first.path("totalElements").asInt()).isEqualTo(3);
        assertThat(first.path("totalPages").asInt()).isEqualTo(2);
        assertThat(first.path("content").size()).isEqualTo(2);
        assertThat(first.path("content").get(0).path("id").asText()).isEqualTo(ids.get(0).toString());
        assertThat(second.path("content").size()).isEqualTo(1);
        assertThat(second.path("content").get(0).path("id").asText()).isEqualTo(ids.get(2).toString());
        assertThat(first.path("context").path("evaluatedDate").asText()).isEqualTo(
            LocalDate.now(clock.withZone(ZoneId.of("Asia/Ho_Chi_Minh"))).toString()
        );
        assertThat(first.path("content").get(0).size()).isEqualTo(3);
        list(prefix, 2, 2).andExpect(status().isOk()).andExpect(jsonPath("$.data.content.length()").value(0));
    }

    @Test
    void searchHandlesVietnameseWithoutAccentsCaseCodeAndTrim() throws Exception {
        list("  " + prefix.toUpperCase(Locale.ROOT) + " LAM DONG  ", 0, 20)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].id").value(ids.get(0).toString()))
            .andExpect(jsonPath("$.data.totalElements").value(1));
        list(code + "1", 0, 20)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].id").value(ids.get(1).toString()));
        list(" ".repeat(150) + prefix, 0, 20)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(3));
    }

    @Test
    void wildcardAndBackslashQueriesAreLiteralAndSqlPayloadIsData() throws Exception {
        list(prefix + " 100%_\\", 0, 20)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(1));
        list(prefix + "' OR 1=1 --", 0, 20)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void invalidPagesAndLongQueriesAreRejected() throws Exception {
        for (int size : List.of(0, 101)) list(prefix, 0, size).andExpect(status().isBadRequest());
        list(prefix, -1, 20).andExpect(status().isBadRequest());
        list("a".repeat(121), 0, 20).andExpect(status().isBadRequest());
        mvc.perform(
            get("/api/v1/destinations")
                .header("Authorization", "Bearer " + token)
                .param("page", "oops")
        ).andExpect(status().isBadRequest());
    }

    @Test
    void emptySearchReturnsARealEmptyPageWithoutInventedSuggestions() throws Exception {
        list(UUID.randomUUID().toString(), 0, 20)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content.length()").value(0))
            .andExpect(jsonPath("$.data.totalElements").value(0))
            .andExpect(jsonPath("$.data.totalPages").value(0));
        mvc.perform(get("/api/v1/destinations").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void selectedCatalogIdRoundTripsThroughTripCreateAndEditWhileCustomNamesRemainSupported() throws Exception {
        var option = data(list(code + "0", 0, 20).andExpect(status().isOk()))
            .path("content")
            .get(0);
        String start = LocalDate.now(clock.withZone(ZoneId.of("Asia/Ho_Chi_Minh")))
            .plusDays(1)
            .toString();
        var trip = data(
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
                                    Map.of(
                                        "name",
                                        option.path("name").asText(),
                                        "provinceId",
                                        option.path("id").asText()
                                    ),
                                    "startDate",
                                    start,
                                    "endDate",
                                    start
                                )
                            )
                        )
                )
                .andExpect(status().isCreated())
        ).path("trip");
        assertThat(trip.path("destination").path("provinceId").asText()).isEqualTo(ids.get(0).toString());
        var updated = data(
            mvc
                .perform(
                    patch("/api/v1/trips/" + trip.path("id").asText())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("version", 0, "destination", Map.of("name", "Đà Lạt"))))
                )
                .andExpect(status().isOk())
        ).path("trip");
        assertThat(updated.path("destination").path("name").asText()).isEqualTo("Đà Lạt");
        assertThat(updated.path("destination").path("provinceId").isNull()).isTrue();
    }
}
