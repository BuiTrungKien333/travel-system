package com.travel.auth;

import com.fasterxml.jackson.databind.*;
import com.travel.TestcontainersConfiguration;
import com.travel.auth.mail.*;
import com.travel.auth.service.GoogleIdTokenVerifier;
import com.travel.auth.token.ActionTokenCodec;
import com.travel.user.entity.User;
import com.travel.user.enums.Role;
import com.travel.user.repository.UserRepository;
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
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class GoogleAuthIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @MockitoBean GoogleIdTokenVerifier verifier; // Google boundary is separately tested with real RSA JWTs.
    @MockitoBean AuthEmailSender sender;
    private MockMvc mvc;
    private final ObjectMapper json = new ObjectMapper();
    private final ConcurrentMap<String, BlockingQueue<AuthEmail>> emails = new ConcurrentHashMap<>();
    @BeforeEach void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        doAnswer(call -> { AuthEmail email = call.getArgument(0); emails.computeIfAbsent(email.recipient(), key -> new LinkedBlockingQueue<>()).add(email); return null; }).when(sender).send(any());
    }
    private String email() { return UUID.randomUUID() + "@gmail.com"; }
    private ResultActions post(String path, Map<String, ?> body) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/" + path)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }
    private JsonNode challenge() throws Exception {
        return json.readTree(post("google/challenge", Map.of()).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");
    }
    private String googleToken(JsonNode challenge, String subject, String email, boolean authoritative) {
        String token = UUID.randomUUID().toString();
        when(verifier.verify(token, ActionTokenCodec.hash(challenge.path("nonce").asText()))).thenReturn(
                new GoogleIdTokenVerifier.Identity(subject, email, "Google User", "https://example.com/avatar.png", authoritative));
        return token;
    }
    private ResultActions login(JsonNode challenge, String token) throws Exception {
        return post("google", Map.of("challengeId", challenge.path("challengeId").asText(), "idToken", token));
    }
    private JsonNode signIn(String subject, String email, boolean authoritative) throws Exception {
        var challenge = challenge(); String token = googleToken(challenge, subject, email, authoritative);
        return json.readTree(login(challenge, token).andExpect(status().is(authoritative ? 200 : 202)).andReturn().getResponse().getContentAsString()).path("data");
    }
    private User local(String email, boolean verified) {
        return users.saveAndFlush(User.builder().email(email).fullName("Local User").passwordHash(passwords.encode("Password123@"))
                .role(Role.USER).emailVerifiedAt(verified ? Instant.now() : null).build());
    }
    private AuthEmail awaitEmail(String email) throws Exception {
        AuthEmail message = emails.computeIfAbsent(email, key -> new LinkedBlockingQueue<>()).poll(5, TimeUnit.SECONDS);
        assertThat(message).isNotNull(); return message;
    }
    @Test void newGmailUserGetsNormalTokensWithoutPasswordOrEmailDelivery() throws Exception {
        String email = email(), subject = UUID.randomUUID().toString(); var outcome = signIn(subject, email, true);
        assertThat(outcome.path("status").asText()).isEqualTo("AUTHENTICATED");
        var user = users.findByEmail(email).orElseThrow(); assertThat(user.getGoogleSubject()).isEqualTo(subject);
        assertThat(user.getPasswordHash()).isNull(); assertThat(user.isEmailVerified()).isTrue(); assertThat(user.getRole()).isEqualTo(Role.USER);
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + outcome.path("auth").path("accessToken").asText())).andExpect(status().isOk());
        post("refresh", Map.of("refreshToken", outcome.path("auth").path("refreshToken").asText())).andExpect(status().isOk());
        post("login", Map.of("email", email, "password", "Password123@")).andExpect(status().isUnauthorized());
        assertThat(emails.get(email)).isNull();
    }
    @Test void verifiedLocalAccountLinksAutomaticallyAndPreservesPasswordAndUserId() throws Exception {
        String email = email(), subject = UUID.randomUUID().toString(); var local = local(email, true);
        var outcome = signIn(subject, email, true);
        assertThat(outcome.path("auth").path("user").path("id").asText()).isEqualTo(local.getId().toString());
        var updated = users.findById(local.getId()).orElseThrow(); assertThat(updated.getGoogleSubject()).isEqualTo(subject);
        assertThat(passwords.matches("Password123@", updated.getPasswordHash())).isTrue();
        post("login", Map.of("email", email, "password", "Password123@")).andExpect(status().isOk());
    }
    @Test void unfinishedRegistrationCannotKeepAnAttackersPlantedPasswordAfterAutoLink() throws Exception {
        String email = email(); var local = local(email, false); signIn(UUID.randomUUID().toString(), email, true);
        var updated = users.findById(local.getId()).orElseThrow(); assertThat(updated.getPasswordHash()).isNull();
        assertThat(updated.isEmailVerified()).isTrue(); assertThat(updated.getSessionVersion()).isEqualTo(1);
        post("login", Map.of("email", email, "password", "Password123@")).andExpect(status().isUnauthorized());
    }
    @Test void returningIdentityUsesGoogleSubjectEvenIfGoogleEmailChanged() throws Exception {
        String subject = UUID.randomUUID().toString(), email = email(); var first = signIn(subject, email, true);
        var next = signIn(subject, email(), true);
        assertThat(next.path("auth").path("user").path("id")).isEqualTo(first.path("auth").path("user").path("id"));
        assertThat(next.path("auth").path("user").path("email").asText()).isEqualTo(email);
    }
    @Test void challengeIsHashedSingleUseAndExpires() throws Exception {
        var challenge = challenge(); String email = email(); String token = googleToken(challenge, UUID.randomUUID().toString(), email, true);
        assertThat(jdbc.queryForObject("SELECT nonce_hash FROM auth_google_challenges WHERE id = ?", String.class,
                UUID.fromString(challenge.path("challengeId").asText()))).isEqualTo(ActionTokenCodec.hash(challenge.path("nonce").asText()));
        login(challenge, token).andExpect(status().isOk()); login(challenge, token).andExpect(status().isUnauthorized());
        var expired = challenge(); String stale = googleToken(expired, UUID.randomUUID().toString(), email(), true);
        jdbc.update("UPDATE auth_google_challenges SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 second' WHERE id = ?", UUID.fromString(expired.path("challengeId").asText()));
        login(expired, stale).andExpect(status().isUnauthorized());
    }
    @Test void disabledAndAlreadyDifferentlyLinkedAccountsAreNotReactivatedOrOverwritten() throws Exception {
        String disabled = email(); var user = local(disabled, true); jdbc.update("UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?", user.getId());
        var a = challenge(); login(a, googleToken(a, UUID.randomUUID().toString(), disabled, true)).andExpect(status().isForbidden());
        String email = email(); signIn(UUID.randomUUID().toString(), email, true);
        var b = challenge(); login(b, googleToken(b, UUID.randomUUID().toString(), email, true)).andExpect(status().isConflict());
    }
    @Test void thirdPartyEmailDoesNotLinkOrIssueSessionsUntilExplicitEmailConfirmation() throws Exception {
        String email = UUID.randomUUID() + "@example.com", subject = UUID.randomUUID().toString(); var user = local(email, true);
        var outcome = signIn(subject, email, false); assertThat(outcome.path("status").asText()).isEqualTo("VERIFICATION_REQUIRED");
        assertThat(outcome.has("auth")).isFalse(); assertThat(users.findById(user.getId()).orElseThrow().getGoogleSubject()).isNull();
        var mail = awaitEmail(email); assertThat(mail.purpose()).isEqualTo(com.travel.auth.entity.ActionPurpose.GOOGLE_LINK);
        mvc.perform(get("/api/v1/auth/google/verify-link").param("token", mail.token())).andExpect(status().isMethodNotAllowed());
        post("google/verify-link", Map.of("token", mail.token())).andExpect(status().isOk()).andExpect(jsonPath("$.data.accessToken").isNotEmpty());
        assertThat(users.findById(user.getId()).orElseThrow().getGoogleSubject()).isEqualTo(subject);
        post("google/verify-link", Map.of("token", mail.token())).andExpect(status().isBadRequest());
    }
    @Test void resettingPasswordInvalidatesOutstandingGoogleEmailLink() throws Exception {
        String email = UUID.randomUUID() + "@example.com"; local(email, true); signIn(UUID.randomUUID().toString(), email, false); var googleMail = awaitEmail(email);
        post("forgot-password", Map.of("email", email)).andExpect(status().isAccepted()); var resetMail = awaitEmail(email);
        post("reset-password", Map.of("token", resetMail.token(), "password", "NewPassword123@")).andExpect(status().isNoContent());
        post("google/verify-link", Map.of("token", googleMail.token())).andExpect(status().isBadRequest());
    }
    @Test void concurrentFirstLoginsCreateOneUserAndBothReturnTheSameIdentity() throws Exception {
        String email = email(), subject = UUID.randomUUID().toString(); var a = challenge(); var b = challenge();
        String ta = googleToken(a, subject, email, true), tb = googleToken(b, subject, email, true);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<String> ca = () -> { ready.countDown(); start.await(); return login(a, ta).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(); };
            Callable<String> cb = () -> { ready.countDown(); start.await(); return login(b, tb).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(); };
            var fa = executor.submit(ca); var fb = executor.submit(cb); assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue(); start.countDown();
            assertThat(json.readTree(fa.get(20, TimeUnit.SECONDS)).path("data").path("auth").path("user").path("id"))
                    .isEqualTo(json.readTree(fb.get(20, TimeUnit.SECONDS)).path("data").path("auth").path("user").path("id"));
        }
    }
    @Test void validatesUntrustedInputBeforeProviderVerification() throws Exception {
        post("google", Map.of("challengeId", UUID.randomUUID(), "idToken", "a".repeat(8193))).andExpect(status().isBadRequest());
        post("google", Map.of("idToken", "token")).andExpect(status().isBadRequest());
        post("google/verify-link", Map.of("token", "too-short")).andExpect(status().isBadRequest());
        verifyNoInteractions(verifier);
    }
}
