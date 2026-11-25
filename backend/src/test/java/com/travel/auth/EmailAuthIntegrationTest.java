package com.travel.auth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.travel.TestcontainersConfiguration;
import com.travel.auth.mail.*;
import com.travel.auth.token.ActionTokenCodec;
import com.travel.user.repository.UserRepository;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class EmailAuthIntegrationTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    UserRepository users;

    @Autowired
    PlatformTransactionManager transactions;

    @MockitoBean
    AuthEmailSender sender;

    private final ConcurrentMap<String, BlockingQueue<AuthEmail>> emails = new ConcurrentHashMap<>();
    private final ObjectMapper json = new ObjectMapper();
    private MockMvc mvc;
    private static final String PASSWORD = "Password123@";
    private static final String NEW_PASSWORD = "NewPassword123@";

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        doAnswer(invocation -> {
            AuthEmail message = invocation.getArgument(0);
            queue(message.recipient()).add(message);
            return null;
        })
            .when(sender)
            .send(any());
    }

    private BlockingQueue<AuthEmail> queue(String email) {
        return emails.computeIfAbsent(email, ignored -> new LinkedBlockingQueue<>());
    }

    private AuthEmail awaitEmail(String email) throws Exception {
        AuthEmail message = queue(email).poll(5, TimeUnit.SECONDS);
        assertThat(message).isNotNull();
        return message;
    }

    private String email() {
        return UUID.randomUUID() + "@example.com";
    }

    private ResultActions post(String path, Map<String, ?> body) throws Exception {
        return mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/" + path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))
        );
    }

    private AuthEmail register(String email) throws Exception {
        post("register", Map.of("email", email, "password", PASSWORD, "fullName", "Nguyễn Văn An"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.data.verificationRequired").value(true))
            .andExpect(jsonPath("$.data.accessToken").doesNotExist())
            .andExpect(jsonPath("$.data.refreshToken").doesNotExist());
        return awaitEmail(email);
    }

    private JsonNode login(String email, String password) throws Exception {
        String body = post("login", Map.of("email", email, "password", password))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return json.readTree(body).path("data");
    }

    private JsonNode verified(String email) throws Exception {
        post("verify-email", Map.of("token", register(email).token())).andExpect(status().isNoContent());
        return login(email, PASSWORD);
    }

    private AuthEmail forgot(String email) throws Exception {
        post("forgot-password", Map.of("email", email)).andExpect(status().isAccepted());
        return awaitEmail(email);
    }

    private void rejectsSession(JsonNode data) throws Exception {
        mvc.perform(
            get("/api/v1/users/me").header("Authorization", "Bearer " + data.path("accessToken").asText())
        ).andExpect(status().isUnauthorized());
        post("refresh", Map.of("refreshToken", data.path("refreshToken").asText())).andExpect(
            status().isUnauthorized()
        );
    }

    private List<Integer> race(Callable<Integer> first, Callable<Integer> second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2),
            start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Callable<Integer> runFirst = () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new TimeoutException();
                return first.call();
            };
            Callable<Integer> runSecond = () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new TimeoutException();
                return second.call();
            };
            Future<Integer> a = executor.submit(runFirst),
                b = executor.submit(runSecond);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
        }
    }

    @Test
    void registrationRequiresExplicitSingleUseConfirmationAndDoesNotIssueAnySessions() throws Exception {
        String email = email();
        AuthEmail message = register(email);
        var user = users.findByEmail(email).orElseThrow();
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM auth_refresh_tokens WHERE user_id = ?",
                Integer.class,
                user.getId()
            )
        ).isZero();
        assertThat(
            jdbc.queryForObject(
                "SELECT token_hash FROM auth_action_tokens WHERE user_id = ?",
                String.class,
                user.getId()
            )
        ).isEqualTo(ActionTokenCodec.hash(message.token()));
        post("login", Map.of("email", email, "password", PASSWORD))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("AUTH_008"));
        mvc.perform(get("/api/v1/auth/verify-email").param("token", message.token())).andExpect(
            status().isMethodNotAllowed()
        );
        assertThat(users.findByEmail(email).orElseThrow().isEmailVerified()).isFalse();
        post("verify-email", Map.of("token", message.token())).andExpect(status().isNoContent());
        post("verify-email", Map.of("token", message.token()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("AUTH_009"));
        login(email, PASSWORD);
    }

    @Test
    void expiredVerificationIsRejectedWithoutVerifyingAccount() throws Exception {
        String email = email();
        AuthEmail message = register(email);
        jdbc.update(
            "UPDATE auth_action_tokens SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 second' WHERE token_hash = ?",
            ActionTokenCodec.hash(message.token())
        );
        post("verify-email", Map.of("token", message.token())).andExpect(status().isBadRequest());
        assertThat(users.findByEmail(email).orElseThrow().isEmailVerified()).isFalse();
    }

    @Test
    void cooldownAndConcurrentResendsCreateOneNewLinkAndInvalidateOldLink() throws Exception {
        String email = email();
        AuthEmail original = register(email);
        post("resend-verification", Map.of("email", email)).andExpect(status().isAccepted());
        assertThat(queue(email).poll(100, TimeUnit.MILLISECONDS)).isNull();
        jdbc.update(
            "UPDATE auth_action_tokens SET created_at = CURRENT_TIMESTAMP - INTERVAL '61 seconds' WHERE token_hash = ?",
            ActionTokenCodec.hash(original.token())
        );
        Callable<Integer> resend = () ->
            post("resend-verification", Map.of("email", email)).andReturn().getResponse().getStatus();
        assertThat(race(resend, resend)).containsExactly(202, 202);
        AuthEmail replacement = awaitEmail(email);
        assertThat(queue(email).poll(100, TimeUnit.MILLISECONDS)).isNull();
        post("verify-email", Map.of("token", original.token())).andExpect(status().isBadRequest());
        post("verify-email", Map.of("token", replacement.token())).andExpect(status().isNoContent());
    }

    @Test
    void concurrentVerificationHasOneWinner() throws Exception {
        AuthEmail message = register(email());
        Callable<Integer> verify = () ->
            post("verify-email", Map.of("token", message.token())).andReturn().getResponse().getStatus();
        assertThat(race(verify, verify)).containsExactlyInAnyOrder(204, 400);
    }

    @Test
    void forgotResponsesDoNotRevealMissingOrDisabledAccounts() throws Exception {
        String active = email(),
            missing = email(),
            disabled = email();
        verified(active);
        verified(disabled);
        jdbc.update("UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE email = ?", disabled);
        JsonNode reference = null;
        for (String candidate : List.of(active, missing, disabled)) {
            String response = post("forgot-password", Map.of("email", candidate))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.token").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();
            JsonNode value = json.readTree(response);
            if (reference == null) reference = value;
            assertThat(value.path("data")).isEqualTo(reference.path("data"));
            assertThat(value.path("message")).isEqualTo(reference.path("message"));
        }
        awaitEmail(active);
        assertThat(queue(missing)).isEmpty();
        assertThat(queue(disabled)).isEmpty();
    }

    @Test
    void resetRevokesAccessAndRefreshForEveryDeviceAndRejectsOldPasswordAndTokenReuse() throws Exception {
        String email = email();
        JsonNode deviceA = verified(email),
            deviceB = login(email, PASSWORD);
        AuthEmail message = forgot(email);
        post("reset-password", Map.of("token", message.token(), "password", NEW_PASSWORD)).andExpect(
            status().isNoContent()
        );
        rejectsSession(deviceA);
        rejectsSession(deviceB);
        post("login", Map.of("email", email, "password", PASSWORD)).andExpect(status().isUnauthorized());
        login(email, NEW_PASSWORD);
        post("reset-password", Map.of("token", message.token(), "password", PASSWORD)).andExpect(
            status().isBadRequest()
        );
        assertThat(users.findByEmail(email).orElseThrow().getSessionVersion()).isEqualTo(1);
    }

    @Test
    void wrongPurposeAndInvalidPasswordDoNotConsumeVerificationToken() throws Exception {
        String email = email();
        AuthEmail verification = register(email);
        post("reset-password", Map.of("token", verification.token(), "password", NEW_PASSWORD)).andExpect(
            status().isBadRequest()
        );
        post("verify-email", Map.of("token", verification.token())).andExpect(status().isNoContent());
        AuthEmail reset = forgot(email);
        post("verify-email", Map.of("token", reset.token())).andExpect(status().isBadRequest());
        for (String password : new String[] { "1234567", "ệ".repeat(25), "a".repeat(51) }) {
            post("reset-password", Map.of("token", reset.token(), "password", password))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
        }
        post("reset-password", Map.of("token", reset.token(), "password", NEW_PASSWORD)).andExpect(
            status().isNoContent()
        );
    }

    @Test
    void expiredResetAndDisabledAccountCannotChangePassword() throws Exception {
        String email = email();
        verified(email);
        AuthEmail reset = forgot(email);
        jdbc.update(
            "UPDATE auth_action_tokens SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 second' WHERE token_hash = ?",
            ActionTokenCodec.hash(reset.token())
        );
        post("reset-password", Map.of("token", reset.token(), "password", NEW_PASSWORD)).andExpect(
            status().isBadRequest()
        );
        login(email, PASSWORD);
        jdbc.update(
            "UPDATE auth_action_tokens SET created_at = CURRENT_TIMESTAMP - INTERVAL '61 seconds' WHERE token_hash = ?",
            ActionTokenCodec.hash(reset.token())
        );
        AuthEmail next = forgot(email);
        jdbc.update("UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE email = ?", email);
        post("reset-password", Map.of("token", next.token(), "password", NEW_PASSWORD)).andExpect(
            status().isBadRequest()
        );
        assertThat(users.findByEmail(email).orElseThrow().getSessionVersion()).isZero();
    }

    @Test
    void resetCanRecoverAnUnfinishedRegistrationAndInvalidatesVerificationLink() throws Exception {
        String email = email();
        AuthEmail verify = register(email);
        AuthEmail reset = forgot(email);
        post("reset-password", Map.of("token", reset.token(), "password", NEW_PASSWORD)).andExpect(
            status().isNoContent()
        );
        assertThat(users.findByEmail(email).orElseThrow().isEmailVerified()).isTrue();
        post("verify-email", Map.of("token", verify.token())).andExpect(status().isBadRequest());
        login(email, NEW_PASSWORD);
    }

    @Test
    void concurrentResetHasOneWinnerAndIncrementsVersionOnce() throws Exception {
        String email = email();
        verified(email);
        AuthEmail reset = forgot(email);
        Callable<Integer> apply = () ->
            post("reset-password", Map.of("token", reset.token(), "password", NEW_PASSWORD))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(race(apply, apply)).containsExactlyInAnyOrder(204, 400);
        assertThat(users.findByEmail(email).orElseThrow().getSessionVersion()).isEqualTo(1);
        login(email, NEW_PASSWORD);
    }

    @Test
    void concurrentRefreshCannotLeaveAnySessionAliveAfterReset() throws Exception {
        String email = email();
        JsonNode original = verified(email);
        AuthEmail reset = forgot(email);
        java.util.concurrent.atomic.AtomicReference<JsonNode> refreshed =
            new java.util.concurrent.atomic.AtomicReference<>();
        Callable<Integer> rotate = () -> {
            var response = post("refresh", Map.of("refreshToken", original.path("refreshToken").asText()))
                .andReturn()
                .getResponse();
            if (response.getStatus() == 200) refreshed.set(json.readTree(response.getContentAsString()).path("data"));
            return response.getStatus();
        };
        var results = race(rotate, () ->
            post("reset-password", Map.of("token", reset.token(), "password", NEW_PASSWORD))
                .andReturn()
                .getResponse()
                .getStatus()
        );
        assertThat(results.get(0)).isIn(200, 401);
        assertThat(results.get(1)).isEqualTo(204);
        rejectsSession(original);
        if (refreshed.get() != null) rejectsSession(refreshed.get());
    }

    @Test
    void concurrentOldPasswordLoginCannotLeaveAnySessionAliveAfterReset() throws Exception {
        String email = email();
        verified(email);
        AuthEmail reset = forgot(email);
        java.util.concurrent.atomic.AtomicReference<JsonNode> issued =
            new java.util.concurrent.atomic.AtomicReference<>();
        Callable<Integer> signIn = () -> {
            var response = post("login", Map.of("email", email, "password", PASSWORD))
                .andReturn()
                .getResponse();
            if (response.getStatus() == 200) issued.set(json.readTree(response.getContentAsString()).path("data"));
            return response.getStatus();
        };
        var results = race(signIn, () ->
            post("reset-password", Map.of("token", reset.token(), "password", NEW_PASSWORD))
                .andReturn()
                .getResponse()
                .getStatus()
        );
        assertThat(results.get(0)).isIn(200, 401);
        assertThat(results.get(1)).isEqualTo(204);
        if (issued.get() != null) rejectsSession(issued.get());
        login(email, NEW_PASSWORD);
    }

    @Test
    void validationBoundsEmailAndTokenInput() throws Exception {
        for (String path : List.of("forgot-password", "resend-verification")) {
            post(path, Map.of("email", "not-an-email")).andExpect(status().isBadRequest());
            post(path, Map.of("email", "a".repeat(250) + "@example.com")).andExpect(status().isBadRequest());
        }
        post("verify-email", Map.of("token", "")).andExpect(status().isBadRequest());
        post("verify-email", Map.of("token", "a".repeat(4096))).andExpect(status().isBadRequest());
        post("reset-password", Map.of("token", "a".repeat(43), "password", NEW_PASSWORD)).andExpect(
            status().isBadRequest()
        );
    }

    @Test
    void rolledBackRegistrationCannotSendEmail() throws Exception {
        String email = email();
        assertThatThrownBy(() ->
            new TransactionTemplate(transactions).executeWithoutResult(status -> {
                try {
                    post("register", Map.of("email", email, "password", PASSWORD, "fullName", "An")).andExpect(
                        status().isAccepted()
                    );
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
                throw new IllegalStateException("rollback fixture");
            })
        ).isInstanceOf(IllegalStateException.class);
        assertThat(users.existsByEmail(email)).isFalse();
        assertThat(queue(email).poll(100, TimeUnit.MILLISECONDS)).isNull();
    }
}
