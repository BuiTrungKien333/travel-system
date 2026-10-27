---
name: testing-mastery
description: >-
  Enterprise-grade testing skill specialized for Java Spring Boot backend
  (travel-system/backend). Use when writing, reviewing, refactoring, or debugging
  backend tests; setting up test infrastructure; planning test strategy; or when a
  task's definition of done requires tests. Covers unit testing (Mockito, standalone
  MockMvc), integration testing (Testcontainers PostgreSQL 17, Spring Boot Test, Flyway),
  security filter testing, and concurrency/race condition testing with production-ready patterns.
---

# Backend Testing Mastery — Java Spring Boot

Write correct, maintainable, production-grade tests for the Travel System backend (`travel-system/backend`).

## Technology Stack

- **Runtime & Framework**: Java 21 · Spring Boot 4.1
- **Test Framework**: JUnit 5 (`junit-jupiter`) · `@Nested` · `@ParameterizedTest` · `@DisplayName`
- **Mocking & Isolation**: Mockito (`@ExtendWith(MockitoExtension.class)`) · `@MockitoBean` (Spring Boot 3.4+)
- **Assertions**: AssertJ (`assertThat`, `assertThatThrownBy`) · Hamcrest / JSONPath
- **Web Testing**: Spring MockMvc (standalone and context-based)
- **Database & Migration**: Testcontainers PostgreSQL 17 (`@ServiceConnection`) · Flyway
- **Build Tool**: Maven (`mvn test`, Maven Surefire with Mockito Java Agent for JDK 21)

Read the detailed guide in [Backend Testing Reference](./references/backend-testing.md) and [Testing Strategy](./references/testing-strategy.md) before writing tests.

---

## Core Principles

1. **Test behavior, not implementation** — Assert observable outcomes (HTTP status codes, response JSON, state transitions, domain error codes), not internal method invocations.
2. **One concept per test** — Each `@Test` verifies exactly one behavior or boundary. Use clear naming (`methodName_condition_expectedOutcome`) or descriptive `@DisplayName`.
3. **Arrange → Act → Assert** — Keep the three phases visually distinct. Shared setup belongs in `@BeforeEach`, teardown in `@AfterEach`.
4. **Deterministic & isolated** — Zero shared mutable state between tests. No reliance on execution order. Always clean up thread-locals (e.g., `SecurityContextHolder.clearContext()` in `@AfterEach`).
5. **Production parity** — Test with the real database engine (PostgreSQL via Testcontainers), real Flyway migrations, and real security filter chains. Mock only external third-party boundaries (SMTP, external AI/weather APIs).
6. **Honest reporting** — Never claim a test passed without executing it. Report failures, skipped tests, and environment issues truthfully.

---

## Decision Matrix: Choosing the Right Test Type

| Component / Concern | Recommended Test Type | Framework / Annotations | Scope & Notes |
|---|---|---|---|
| **REST Controller** | Controller Unit Test | `@ExtendWith(MockitoExtension.class)` + Standalone `MockMvc` | Tests routing, DTO validation, status codes, JSON response. Fast, no Spring context. |
| **Service Layer** | Service Unit Test | `@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks` | Tests business logic, branching, domain exceptions (`AppException`). |
| **Servlet Filter / Security** | Filter Unit Test | `@ExtendWith(MockitoExtension.class)` | Tests JWT parsing, `sessionVersion` validation, `SecurityContextHolder` population. |
| **Entity / Algorithm / Utility** | Pure Unit Test | Plain JUnit 5 + AssertJ | No mocks needed. Tests builders, default values, algorithms (e.g., Held-Karp TSP). |
| **Custom JPA Query** | Repository Slice Test | `@DataJpaTest` + Testcontainers | Tests JPQL/native SQL, row locking (`@Lock`), projections (`AuthState`). |
| **End-to-End Auth / Workflow** | Integration Test | `@SpringBootTest` + `@ActiveProfiles("test")` + `@Import(TestcontainersConfiguration.class)` | Real PostgreSQL, Flyway, full Spring Security chain. Uses `@MockitoBean` for external boundaries. |
| **Concurrency / Race Condition** | Concurrency Test | `CountDownLatch` + `ExecutorService` (inside Integration Test) | Verifies atomic operations (e.g., token refresh race, duplicate registration). |

---

## Quick Command Reference

```bash
# Navigate to backend
cd backend

# Run entire test suite
mvn test

# Run a specific test class
mvn test -Dtest=AuthControllerTest

# Run a single test method
mvn test -Dtest=AuthControllerTest#login_validCredentials_returns200

# Run all integration tests (requires Docker daemon for Testcontainers)
mvn test -Dtest="*IntegrationTest"

# Run tests in a specific package
mvn test -Dtest="com.travel.auth.**.*Test"
```

---

## Verification Checklist

Before declaring backend tests complete:

- [ ] All new and modified tests pass locally (`mvn test -Dtest=...` exits with code 0)
- [ ] No existing tests broken by code modifications
- [ ] Test names describe behavior clearly (`methodName_condition_expectedOutcome`)
- [ ] SecurityContext is cleaned up in `@AfterEach` (`SecurityContextHolder.clearContext()`)
- [ ] Sensitive fields (`passwordHash`, private keys) are asserted to NOT exist in response bodies
- [ ] Domain error codes (`ErrorCode`) are asserted explicitly, not just generic exceptions
- [ ] Integration tests use `@ActiveProfiles("test")` and `@Import(TestcontainersConfiguration.class)`
- [ ] Concurrency tests use synchronization primitives (`CountDownLatch`), never `Thread.sleep()`
- [ ] No hardcoded passwords, real emails, or external URLs in test assertions
