# Backend Testing Strategy & Coverage Guidelines

## Test Pyramid

```
              ┌──────────────┐
              │  E2E / Smoke │   ← Few, slow, top-level
              │   (@SpringBootTest)  Covers critical auth/API flows
              ├──────────────┤
              │ Integration  │   ← Moderate count
              │    Tests     │      Testcontainers PostgreSQL 17 + Flyway
              │  (@DataJpaTest)     Real database constraints & queries
              ├──────────────┤
              │  Unit Tests  │   ← Many, fast, isolated
              │ (MockMvc &   │      Controllers, Services, Security Filters,
              │   Mockito)   │      Algorithms, Pure Entities (< 100ms each)
              └──────────────┘
```

## Coverage Goals by Layer (Backend)

| Layer | Target | Recommended Test Type | Required Scenarios |
|---|---|---|---|
| **Controller** | 100% of endpoints | Standalone MockMvc | Valid request (200/201/202), input validation errors (400), domain errors (401/403/404/409), rate limit boundaries |
| **Service** | All business logic | Mockito unit test (`@ExtendWith`) | Happy path, every exception branch (`AppException`), boundary conditions, data transformation |
| **Security Filter** | All filter paths | Mockito unit test | Missing Bearer token, expired JWT, invalid signature, `sessionVersion` mismatch, valid authentication |
| **Repository** | Custom queries only | `@DataJpaTest` + Testcontainers | Custom JPQL/native queries, pessimistic locking (`@Lock`), projections (`AuthState`) |
| **Entity / Algorithm** | Builders, enums, logic | Pure unit test (JUnit 5 + AssertJ) | Builder defaults, state transitions, domain algorithms (TSP, distance matrix, weather risk rules) |
| **Integration** | Critical user flows | Full-stack test (`@SpringBootTest`) | Auth registration → verify → login → refresh → logout lifecycle, RBAC, race conditions, CORS |

---

## What Must Be Tested (Non-Negotiable)

### 1. Security-Critical Paths
- **Authentication boundaries**: Login, register, token refresh, logout, password reset.
- **Authorization & RBAC**: Protected endpoints reject unauthenticated or under-privileged requests (401 / 403).
- **Token segregation**: Refresh token cannot be used as an access token and vice versa.
- **Session invalidation**: Password reset increments `sessionVersion` and revokes all refresh tokens immediately.
- **Race conditions**: Concurrent refresh requests produce exactly one winner (one 200, one 401). Concurrent registration of identical email produces one 202 and one 409.
- **Data leakage prevention**: Assert that sensitive fields (`passwordHash`, JWT secret keys) are NEVER exposed in response JSON.

### 2. Data Integrity Paths
- **Unique constraints**: Duplicate email registration throws `USER_ALREADY_EXISTS` (409 Conflict).
- **Cascade operations & revocation**: Password reset deletes all active action tokens and refresh tokens.
- **Account state enforcement**: Soft-deleted or unverified accounts cannot login or refresh tokens.

### 3. API Contract Paths
- **Standard response envelope**: Every API response adheres to `ApiResponse<T>` (`success`, `message`, `data`, `code`, `errors`, `timestamp`).
- **Domain error codes**: Specific `ErrorCode` enum values (e.g., `AUTH_001`, `AUTH_004`, `AUTH_007`) are asserted in the JSON body.

---

## Test Quality Metrics

### Structural Quality
- **Zero flaky tests**: Tests must be completely deterministic and order-independent.
- **Fast feedback**: Unit tests execute in < 100ms each; entire unit suite finishes in seconds.
- **Clear failure diagnostics**: Test name + AssertJ message explains precisely what failed.
- **Maintainable**: Tests adhere to production code style standards, avoiding code bloat.

### Behavioral Quality
- **Boundary value analysis**: Test exact limits (e.g., password length 7 vs 8, 50 vs 51; BCrypt 72-byte UTF-8 limit).
- **Error path coverage**: Every `throw new AppException(...)` has an associated negative test.
- **Concurrency coverage**: Race conditions tested using `CountDownLatch` and `ExecutorService` with strict timeouts.

---

## When to Write Tests

1. **New feature / endpoint**: Tests are part of the Definition of Done.
2. **Bug fix**: Write a failing test reproducing the bug first, then apply the fix.
3. **Refactoring**: Ensure existing tests pass, then expand coverage for untested edge cases.
4. **Security patch**: Add explicit regression tests for the vulnerability.

---

## Test Review Checklist

Before finishing any backend test task:

- [ ] Test names follow standard convention: `methodName_condition_expectedOutcome`
- [ ] Each test verifies exactly one concept
- [ ] Tests run and pass in isolation (`mvn test -Dtest=MyTest`)
- [ ] No `@Disabled` tests without a linked tracker issue
- [ ] No hardcoded passwords, real emails, or production URLs in test code
- [ ] SecurityContext is cleaned up in `@AfterEach` (`SecurityContextHolder.clearContext()`)
- [ ] Sensitive fields (`passwordHash`) are verified to not exist in JSON responses
- [ ] Integration tests use `@ActiveProfiles("test")` and Testcontainers
- [ ] Concurrency tests use synchronization primitives with timeouts, never `Thread.sleep()`
