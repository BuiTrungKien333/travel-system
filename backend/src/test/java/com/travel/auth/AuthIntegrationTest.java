package com.travel.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.TestcontainersConfiguration;
import com.travel.auth.service.impl.AuthServiceImpl;
import com.travel.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** PostgreSQL/Flyway, BCrypt/JWT thật và toàn bộ Spring Security filter chain. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AuthIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    private MockMvc mvc;
    private final ObjectMapper json = new ObjectMapper();
    private static final String PASSWORD = "Password123@";

    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }
    private String email() { return UUID.randomUUID() + "@example.com"; }
    private String body(Map<String, ?> values) throws Exception { return json.writeValueAsString(values); }
    private JsonNode register(String email) throws Exception {
        String response = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("fullName", "Nguyễn Văn An", "email", email, "password", PASSWORD))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).path("data");
    }
    private String refreshBody(JsonNode data) throws Exception { return body(Map.of("refreshToken", data.path("refreshToken").asText())); }

    @Test void registerLoginProfileRefreshAndLogout() throws Exception {
        String email = email();
        JsonNode registered = register(email);
        assertThat(encoder.matches(PASSWORD, users.findByEmail(email).orElseThrow().getPasswordHash())).isTrue();
        assertThat(jdbc.queryForObject("SELECT token_hash FROM auth_refresh_tokens WHERE user_id = ?", String.class,
                UUID.fromString(registered.path("user").path("id").asText())))
                .isEqualTo(AuthServiceImpl.hashToken(registered.path("refreshToken").asText()));
        String loginResponse = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", email.toUpperCase(java.util.Locale.ROOT), "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode login = json.readTree(loginResponse).path("data");
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + login.path("accessToken").asText()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.email").value(email));
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + login.path("refreshToken").asText()))
                .andExpect(status().isUnauthorized());
        String refreshed = mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody(login)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode next = json.readTree(refreshed).path("data");
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody(login)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTH_005"));
        mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON).content(refreshBody(next)))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON).content(refreshBody(next)))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody(next)))
                .andExpect(status().isUnauthorized());
        // Đăng xuất một thiết bị không thu hồi refresh token thiết bị khác.
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody(registered)))
                .andExpect(status().isOk());
    }

    @Test void protectedProfileRequiresAuthenticationAndPublicWhitelistIsNarrow() throws Exception {
        mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTH_001"));
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/internal")).andExpect(status().isUnauthorized());
    }

    @Test void invalidCredentialsDuplicateEmailAndPasswordLimits() throws Exception {
        String email = email();
        register(email);
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("fullName", "An", "email", email, "password", PASSWORD))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("AUTH_003"));
        for (String password : new String[]{"1234567", "a".repeat(51), "ệ".repeat(25)}) {
            mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("fullName", "An", "email", email(), "password", password))))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.password").exists());
        }
        for (String attemptedEmail : new String[]{email, email()}) {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("email", attemptedEmail, "password", "wrongpassword"))))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTH_004"));
        }
    }

    @Test void disabledUserCannotLoginRefreshOrAccessProfile() throws Exception {
        String email = email();
        JsonNode data = register(email);
        jdbc.update("UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE email = ?", email);
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("AUTH_007"));
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody(data)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + data.path("accessToken").asText()))
                .andExpect(status().isForbidden());
    }

    @Test void refreshRejectsAccessTokenAndDatabaseExpiredToken() throws Exception {
        JsonNode data = register(email());
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("refreshToken", data.path("accessToken").asText()))))
                .andExpect(status().isUnauthorized());
        jdbc.update("UPDATE auth_refresh_tokens SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 minute' WHERE token_hash = ?",
                AuthServiceImpl.hashToken(data.path("refreshToken").asText()));
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody(data)))
                .andExpect(status().isUnauthorized());
    }

    @Test void concurrentRefreshHasExactlyOneWinner() throws Exception {
        String content = refreshBody(register(email()));
        assertThat(race(() -> mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(content)).andReturn().getResponse().getStatus()))
                .containsExactlyInAnyOrder(200, 401);
    }

    @Test void concurrentRegistrationHasOneCreatedAndOneConflict() throws Exception {
        String content = body(Map.of("fullName", "An", "email", email(), "password", PASSWORD));
        assertThat(race(() -> mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(content)).andReturn().getResponse().getStatus()))
                .containsExactlyInAnyOrder(201, 409);
    }

    @Test void demoAccountsWithUnchangedDefaultPasswordAreDisabled() {
        assertThat(users.findByEmail("admin@travel.com").orElseThrow().isDeleted()).isTrue();
    }

    @Test void expoWebCorsPreflightAllowsLanguageAndAuthorization() throws Exception {
        mvc.perform(options("/api/v1/auth/login").header("Origin", "http://localhost:8081")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type,Accept-Language,Authorization"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8081"));
    }

    private java.util.List<Integer> race(Callable<Integer> request) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Callable<Integer> runner = () -> { ready.countDown(); if (!start.await(10, TimeUnit.SECONDS)) throw new TimeoutException(); return request.call(); };
            Future<Integer> first = executor.submit(runner);
            Future<Integer> second = executor.submit(runner);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return java.util.List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
        }
    }
}
