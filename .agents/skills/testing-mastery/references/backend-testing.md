# Backend Testing Reference — Java Spring Boot

## Project Test Stack

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-webmvc-test` | MockMvc, controller slice testing |
| `spring-boot-starter-data-jpa-test` | `@DataJpaTest`, repository slice testing |
| `spring-boot-starter-security-test` | `springSecurity()` MockMvc configurer |
| `spring-boot-starter-validation-test` | Bean validation in test context |
| `spring-boot-starter-flyway-test` | Flyway migration support in tests |
| `spring-boot-starter-actuator-test` | Actuator endpoint testing |
| JUnit 5 (`junit-jupiter`) | Test engine, `@Nested`, `@ParameterizedTest`, `@DisplayName` |
| Mockito + `@ExtendWith(MockitoExtension.class)` | Unit-level mocking with `@Mock`, `@InjectMocks`, `@Captor` |
| `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`) | Integration-level bean replacement. Import from `spring-test`, NOT the deprecated `@MockBean` from `spring-boot-test-mock`. |
| AssertJ | Fluent assertions: `assertThat(x).isEqualTo(y)` |
| MockMvc | HTTP endpoint testing without full server startup |
| Testcontainers PostgreSQL 17 (BOM `1.20.4`) | Real database for integration tests via `@ServiceConnection` |
| Flyway | Schema migrations run against Testcontainers DB |
| `application-test.yaml` | Test profile configuration (JWT keys, mock AI/weather providers) |
| Maven Surefire + Mockito agent | JDK 21 self-attach for Mockito byte-buddy |

## Test Configuration

The test profile lives at `src/main/resources/application-test.yaml` and is activated
via `@ActiveProfiles("test")`. It configures:
- Flyway migrations against the Testcontainers PostgreSQL
- Test-only JWT signing keys (Base64-encoded)
- Mock providers for AI and weather services
- Email sending enabled with example.com URLs

Testcontainers setup at `src/test/java/com/travel/TestcontainersConfiguration.java`:
```java
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {
    @Bean
    @ServiceConnection
    @ConditionalOnProperty(name = "test.database.external", havingValue = "false", matchIfMissing = true)
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:17-alpine");
    }
}
```

---

## Test Patterns & Templates

### Pattern 1: Controller Unit Test (Standalone MockMvc)

**When to use**: Testing a `@RestController` in isolation — HTTP routing, request
validation, response structure, status codes. No Spring context loaded.

```java
@ExtendWith(MockitoExtension.class)
class UserControllerTest {
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private UserService userService;
    @Mock private Translator translator;
    @InjectMocks private UserController userController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler(translator))
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/users/me should return 200 OK and UserProfileResponse")
    void getCurrentProfile_success_returns200Ok() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserProfileResponse response = UserProfileResponse.builder()
                .id(userId).email("user@travel.com").fullName("Nguyen Van A")
                .role(Role.USER).build();
        when(userService.getCurrentProfile()).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/me").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.email").value("user@travel.com"));
    }

    @Test
    @DisplayName("GET /api/v1/users/me should return 401 when unauthenticated")
    void getCurrentProfile_unauthenticated_returns401() throws Exception {
        when(userService.getCurrentProfile()).thenThrow(new AppException(ErrorCode.UNAUTHORIZED));

        mockMvc.perform(get("/api/v1/users/me").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"));
    }
}
```

**Key conventions**:
- `MockMvcBuilders.standaloneSetup(controller)` — fastest, no Spring context.
- Always attach `.setControllerAdvice(new GlobalExceptionHandler(translator))` for
  exception mapping. `Translator` is the project's i18n helper — mock it with
  `@Mock Translator translator` in every controller test since `GlobalExceptionHandler`
  requires it as a constructor argument.
- Test name pattern: `methodName_condition_expectedOutcome`.
- Use `@DisplayName` for human-readable names in reports.
- Verify JSON structure via `jsonPath()`.

### Pattern 2: Service Unit Test (Mockito)

**When to use**: Testing business logic in a `@Service` class. Mock all repositories
and external dependencies.

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private UserRepository userRepository;
    @InjectMocks private UserServiceImpl userService;

    private UUID userId;
    private User mockUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        mockUser = User.builder()
                .fullName("Nguyen Van A").email("user@travel.com")
                .passwordHash("hashed").role(Role.USER).build();

        // Simulate authenticated user via SecurityContext
        CustomUserDetails userDetails = new CustomUserDetails(userId, "user@travel.com", Role.USER, List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext(); // CRITICAL: prevent cross-test leakage
    }

    @Test
    @DisplayName("getCurrentProfile should return profile when user is active")
    void getCurrentProfile_success_returnsResponse() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        UserProfileResponse response = userService.getCurrentProfile();

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("user@travel.com");
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("getCurrentProfile should throw USER_NOT_FOUND when user absent")
    void getCurrentProfile_notFound_throwsAppException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentProfile())
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
```

**Key conventions**:
- Always clear `SecurityContextHolder` in `@AfterEach`.
- Use `assertThatThrownBy()` (AssertJ) for exception testing.
- Use `.extracting("errorCode")` to verify specific error codes.
- `verify()` only when the interaction is part of the contract, not for every mock call.

### Pattern 3: Filter/Security Unit Test

**When to use**: Testing servlet filters, security components, or JWT processing.

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {
    @Mock private JwtUtils jwtUtils;
    @Mock private UserRepository users;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;
    @Mock private Claims claims;
    @InjectMocks private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() { SecurityContextHolder.clearContext(); }

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    @Nested
    @DisplayName("When no Bearer token is provided")
    class NoTokenTests {
        @Test
        @DisplayName("Should skip authentication and continue filter chain")
        void skipsAuth() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/v1/trips");
            when(request.getHeader("Authorization")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtUtils);
        }
    }

    @Nested
    @DisplayName("When valid JWT is provided")
    class ValidTokenTests {
        @Test
        @DisplayName("Should authenticate and set CustomUserDetails as principal")
        void authenticatesUser() throws Exception {
            UUID userId = UUID.randomUUID();
            when(request.getRequestURI()).thenReturn("/api/v1/trips");
            when(request.getHeader("Authorization")).thenReturn("Bearer valid_token");
            when(jwtUtils.extractAllClaims("valid_token", TokenType.ACCESS_TOKEN)).thenReturn(claims);
            when(claims.getSubject()).thenReturn("user@travel.com");
            when(claims.get("userId", String.class)).thenReturn(userId.toString());
            when(claims.get("sessionVersion", Long.class)).thenReturn(0L);
            UserRepository.AuthState state = mock(UserRepository.AuthState.class);
            when(state.getEmail()).thenReturn("user@travel.com");
            when(state.getEmailVerifiedAt()).thenReturn(Instant.now());
            when(users.findAuthStateById(userId)).thenReturn(Optional.of(state));
            when(claims.get("authorities", List.class)).thenReturn(List.of("ROLE_USER"));

            filter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.isAuthenticated()).isTrue();
            assertThat(((CustomUserDetails) auth.getPrincipal()).getId()).isEqualTo(userId);
        }
    }
}
```

**Key conventions**:
- Use `@Nested` to group scenarios (no token / invalid token / valid token).
- Always verify the `SecurityContextHolder` state as the observable outcome.
- Test both the happy path and all rejection branches.

### Pattern 4: Integration Test (Full Stack)

**When to use**: Testing cross-cutting behavior — HTTP through security filters,
through service layer, through real database. Use for auth flows, RBAC, data integrity,
race conditions, and CORS.

```java
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AuthIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean AuthEmailSender sender; // Mock only external SMTP boundary

    private MockMvc mvc;
    private final ObjectMapper json = new ObjectMapper();
    private final ConcurrentMap<String, BlockingQueue<AuthEmail>> emails = new ConcurrentHashMap<>();

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        // Capture emails sent during tests
        doAnswer(invocation -> {
            AuthEmail email = invocation.getArgument(0);
            emails.computeIfAbsent(email.recipient(), k -> new LinkedBlockingQueue<>()).add(email);
            return null;
        }).when(sender).send(any());
    }
}
```

**Key conventions**:
- `@SpringBootTest` + `@Import(TestcontainersConfiguration.class)` for real PostgreSQL.
- `@ActiveProfiles("test")` to load test-specific configuration.
- `apply(springSecurity())` to include the full Spring Security filter chain.
- `@MockitoBean` only for boundaries you cannot/should not call (SMTP, external APIs).
- Use `JdbcTemplate` for direct DB assertions and state manipulation.
- Test real Flyway migrations run against the test database.

### Pattern 5: Concurrency / Race Condition Test

**When to use**: Verifying that concurrent operations are handled correctly
(e.g., refresh token rotation, duplicate registration).

```java
@Test
void concurrentRefreshHasExactlyOneWinner() throws Exception {
    String content = refreshBody(register(email()));

    assertThat(race(() -> mvc.perform(post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON).content(content))
            .andReturn().getResponse().getStatus()))
            .containsExactlyInAnyOrder(200, 401);
}

// Reusable race helper
private List<Integer> race(Callable<Integer> request) throws Exception {
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
        Callable<Integer> runner = () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) throw new TimeoutException();
            return request.call();
        };
        Future<Integer> first = executor.submit(runner);
        Future<Integer> second = executor.submit(runner);
        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        return List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
    }
}
```

**Key conventions**:
- Use `CountDownLatch` to synchronize thread start for maximum contention.
- Assert on the *set* of results (one winner, one loser), not on ordering.
- Always use timeout on `await()` and `get()` to prevent hanging tests.
- Use `try-with-resources` on `ExecutorService` for cleanup (Java 21).

### Pattern 6: Parameterized / Validation Tests

**When to use**: Testing input validation boundaries, edge cases, and multiple
similar scenarios.

```java
@Nested
@DisplayName("Input validation")
class ValidationTests {
    @ParameterizedTest(name = "email=\"{0}\" should fail validation")
    @NullAndEmptySource
    @ValueSource(strings = {"not-an-email", "a@", "@b.com"})
    void login_invalidEmail_returns400(String email) throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email(email).password("Password123@").build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }
}
```

### Pattern 7: Entity / Pure Unit Test

**When to use**: Testing JPA entity builders, default values, enums, and pure
utility functions.

```java
@DisplayName("HeldKarpSolver")
class HeldKarpSolverTest {
    @Test
    @DisplayName("Single place returns zero-distance route")
    void singlePlace_returnsZeroDistance() {
        Place p = Place.builder().id(UUID.randomUUID()).name("P1")
                .latitude(16.0).longitude(108.0).build();
        // ... test algorithm with real objects, no mocks
        assertThat(result.totalDistance()).isEqualTo(0.0);
    }
}
```

### Pattern 8: Clock-Dependent Service Test

**When to use**: Testing services that depend on `java.time.Clock` for time-sensitive
logic (token expiry, cooldown periods). `Clock` is a `final` class — Mockito cannot
mock it. Use `Clock.fixed()` and construct the service manually.

```java
@ExtendWith(MockitoExtension.class)
class EmailActionServiceTest {
    @Mock private UserRepository users;
    @Mock private ActionTokenRepository tokens;
    @Mock private ApplicationEventPublisher events;
    // ... other mocks

    // Clock.fixed() — deterministic, no Mockito needed
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-06-15T10:00:00Z"), ZoneOffset.UTC);

    @InjectMocks private EmailActionService service;

    @BeforeEach
    void setUp() {
        // Rebuild service with real Clock (Mockito cannot inject final classes)
        service = new EmailActionService(
                users, tokens, refreshTokens, passwords, properties, events, clock);
    }

    @Test
    @DisplayName("Should skip token issuance when previous token is within cooldown")
    void request_withinCooldown_skipsIssuance() {
        // Token created 30s ago, cooldown is 60s → still within cooldown
        ActionToken recentToken = new ActionToken(userId, ActionPurpose.VERIFY_EMAIL, "hash",
                clock.instant().minusSeconds(30), clock.instant().plusHours(24));
        when(tokens.findByUserIdAndPurpose(userId, ActionPurpose.VERIFY_EMAIL))
                .thenReturn(Optional.of(recentToken));
        // ...
        verify(tokens, never()).save(any()); // No new token issued
    }
}
```

**Key conventions**:
- Use `Clock.fixed(instant, zone)` for deterministic time assertions.
- Reconstruct the service in `@BeforeEach` since `@InjectMocks` cannot inject `final Clock`.
- Assert time-relative behavior using `clock.instant()` arithmetic (`minusSeconds`, `plusHours`).

### Pattern 9: Reflection-Based ID Assignment for `@GeneratedValue` Entities

**When to use**: Unit-testing services that reference entity IDs generated by JPA.
Since unit tests don't run JPA, entity IDs are `null` after `builder().build()`.
Use reflection to set the ID field manually.

```java
@BeforeEach
void setUp() throws Exception {
    userId = UUID.randomUUID();
    testUser = User.builder()
            .fullName("An").email("an@example.com").passwordHash("hashed").build();

    // JPA @GeneratedValue ID is null in unit tests — set it via reflection
    java.lang.reflect.Field idField = testUser.getClass().getDeclaredField("id");
    idField.setAccessible(true);
    idField.set(testUser, userId);
}
```

**Key conventions**:
- Use this pattern **only in unit tests** where JPA is not available.
- In integration tests (`@SpringBootTest`), let JPA generate the ID naturally.
- Keep the reflection in `@BeforeEach`, not scattered across individual tests.

### Pattern 10: GlobalExceptionHandler Test (Inline Test Controller)

**When to use**: Testing the `GlobalExceptionHandler` directly by creating a
minimal `@RestController` that throws specific exceptions.

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {
    @Mock private Translator translator;
    private MockMvc mockMvc;

    // Inline test-only controller — throws exceptions on demand
    @RestController
    @RequestMapping("/test")
    static class TestController {
        @GetMapping("/app-exception")
        ResponseEntity<Void> appException() {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        @GetMapping("/runtime")
        ResponseEntity<Void> runtime() {
            throw new RuntimeException("boom");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler(translator))
                .build();
    }

    @Test
    @DisplayName("AppException returns the status code configured in ErrorCode")
    void handleAppException_returnsConfiguredStatus() throws Exception {
        mockMvc.perform(get("/test/app-exception"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Unexpected RuntimeException returns 500 Internal Server Error")
    void handleRuntimeException_returns500() throws Exception {
        mockMvc.perform(get("/test/runtime"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }
}
```

**Key conventions**:
- Define a `static class TestController` inside the test class with endpoints that
  trigger specific exception types.
- Use `MockMvcBuilders.standaloneSetup(new TestController())` — not
  `@SpringBootTest`.
- Test each `ErrorCode` maps to the correct HTTP status.

### Pattern 11: Context Integration Test (Lightweight `@SpringBootTest`)

**When to use**: Testing a service or provider that requires real Spring beans
(e.g., `@ConfigurationProperties`, `RestClient`, AI providers) but does not need
the full HTTP layer. Named `*ContextTest` in this project.

```java
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class GeminiAiProviderContextTest {

    @Autowired private AiProviderFactory providerFactory;

    @Test
    @DisplayName("Mock provider is selected when active-provider=mock in test profile")
    void testProfile_usesMockProvider() {
        // application-test.yaml sets: application.ai.active-provider: mock
        assertThat(providerFactory.getActiveProvider())
                .isInstanceOf(MockAiProvider.class);
    }
}
```

**Key conventions**:
- Use when verifying `@ConfigurationProperties` wiring, provider factories, or
  conditional beans (`@ConditionalOnProperty`).
- Lighter than full HTTP integration tests — no MockMvc, no security filter chain.
- Still uses `@ActiveProfiles("test")` and Testcontainers for DB-dependent beans.
- File naming: `<ClassName>ContextTest.java` to distinguish from pure unit tests.

---

## File Placement & Naming

```
src/test/java/com/travel/
├── TestcontainersConfiguration.java          # Shared Testcontainers config
├── TravelBackendApplicationTests.java        # Smoke test
├── auth/
│   ├── AuthIntegrationTest.java              # Full-stack integration
│   ├── controller/
│   │   └── AuthControllerTest.java           # Controller unit test
│   ├── service/
│   │   ├── AuthServiceTest.java              # Service unit test
│   │   └── EmailActionServiceTest.java
│   └── mail/
│       └── SmtpAuthEmailSenderTest.java
├── security/
│   ├── filter/
│   │   └── JwtAuthenticationFilterTest.java
│   └── jwt/
│       └── JwtUtilsTest.java
├── user/
│   ├── controller/
│   │   └── UserControllerTest.java
│   └── service/
│       └── UserServiceTest.java
└── ...
```

**Naming rules**:
- Test class: `<ClassName>Test.java` (unit) or `<Feature>IntegrationTest.java` (integration).
- Test method: `methodName_condition_expectedOutcome` or descriptive sentence in `@DisplayName`.
- Mirror the source package structure under `src/test/java/`.

---

## Assertion Library Guide

Use **AssertJ** as the primary assertion library:

```java
// Basic
assertThat(result).isNotNull();
assertThat(result.getEmail()).isEqualTo("user@travel.com");

// Collections
assertThat(list).hasSize(3).contains("a", "b");
assertThat(list).containsExactlyInAnyOrder(200, 401);

// Exceptions
assertThatThrownBy(() -> service.doSomething())
        .isInstanceOf(AppException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.USER_NOT_FOUND);

// Soft assertions (check all, report all failures at once)
SoftAssertions.assertSoftly(softly -> {
    softly.assertThat(response.getEmail()).isEqualTo("user@travel.com");
    softly.assertThat(response.getRole()).isEqualTo(Role.USER);
});
```

Use **JSONPath** (via `jsonPath()`) for MockMvc response assertions:
```java
.andExpect(jsonPath("$.success").value(true))
.andExpect(jsonPath("$.data.id").value(userId.toString()))
.andExpect(jsonPath("$.data.email").value("user@travel.com"))
.andExpect(jsonPath("$.data.passwordHash").doesNotExist()) // Security: never expose
```

---

## Anti-Patterns to Avoid

| Anti-Pattern | Do Instead |
|---|---|
| `@SpringBootTest` for testing a single controller | Use standalone `MockMvcBuilders.standaloneSetup()` |
| H2 in-memory DB pretending to be PostgreSQL | Use Testcontainers `PostgreSQLContainer` |
| Mocking the repository you're trying to test | Use `@DataJpaTest` with Testcontainers |
| `Thread.sleep()` in tests | Use `CountDownLatch`, `CompletableFuture`, or `Awaitility` |
| Catching exceptions manually in tests | Use `assertThatThrownBy()` or `assertThrows()` |
| Sharing mutable state between `@Test` methods | Create fresh state in `@BeforeEach` |
| Testing private methods | Test through public API |
| `@Order` on tests | Each test must be independently runnable |
| `any()` in assertions | Assert specific values |
| Verifying all mock interactions | Verify only contract-significant interactions |
| Using `@Autowired` in unit tests | Use `@Mock` / `@InjectMocks` |
| Ignoring `@AfterEach` SecurityContext cleanup | Always `SecurityContextHolder.clearContext()` |

---

## Security Testing Checklist

- [ ] Tokens are never returned in error responses or logs
- [ ] Password hashes are never exposed in API responses (`$.data.passwordHash` does not exist)
- [ ] Protected endpoints return 401 without valid `Authorization` header
- [ ] Refresh tokens cannot be used as access tokens (and vice versa)
- [ ] Disabled/deleted users cannot login, refresh, or access protected endpoints
- [ ] Concurrent refresh produces exactly one winner (race condition test)
- [ ] Concurrent registration of same email produces one 202 and one 409
- [ ] Email verification is required before login (403 with `AUTH_008`)
- [ ] Session version invalidation works after password reset
- [ ] CORS preflight allows expected headers
