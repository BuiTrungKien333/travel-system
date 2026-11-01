package com.travel.planning.ai.resilience;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.planning.retrieval.RagDemoProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.*;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.assertj.core.api.Assertions.*;
import static com.travel.planning.ai.resilience.AiRequestContext.Stage.*;

class AiRetryExecutorTest {
    final RagDemoProperties properties = new RagDemoProperties();
    final AtomicLong millis = new AtomicLong();
    final AiRequestContext context = new AiRequestContext(properties, () -> millis.get() * 1_000_000);
    final List<Long> sleeps = new ArrayList<>();
    final Clock clock = Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"), ZoneOffset.UTC);
    final AiRetryExecutor executor = new AiRetryExecutor(properties, context, new ObjectMapper(), clock,
            delay -> { sleeps.add(delay); millis.addAndGet(delay); }, () -> 100);

    @Test void transientFailureRecoversAndOnlyOneRetryIsSharedAcrossStages() {
        try (var scope = context.ensure()) {
            AtomicInteger calls = new AtomicInteger();
            assertThat(executor.execute(INTENT, "fixture", () -> {
                if (calls.incrementAndGet() == 1) throw http(503, null, "");
                return "valid intent";
            })).isEqualTo("valid intent");
            assertThat(sleeps).containsExactly(600L);
            for (var stage : List.of(RETRIEVAL, ALLOCATION, THEMES)) {
                AtomicInteger otherCalls = new AtomicInteger();
                assertThatThrownBy(() -> executor.execute(stage, "fixture", () -> {
                    otherCalls.incrementAndGet(); throw http(503, null, "");
                })).isInstanceOf(AiCallFailure.class);
                assertThat(otherCalls).hasValue(1);
                assertThat(result(stage).retryDecision()).isEqualTo("REQUEST_RETRY_LIMIT_REACHED");
            }
            assertThat(context.report().retriesUsed()).isEqualTo(1);
            assertThat(result(INTENT).attempts()).isEqualTo(2);
            assertThat(result(INTENT).retries()).isEqualTo(1);
            assertThat(result(INTENT).reason()).isEqualTo("RECOVERED_AFTER_RETRY");
            assertThat(result(INTENT).lastProviderStatus()).isEqualTo(503);
        }
    }

    @Test void respectsMaximumOfRetryAfterAndFractionalGoogleRetryInfo() {
        var headers = new HttpHeaders(); headers.set("Retry-After", "2");
        AtomicInteger calls = new AtomicInteger();
        executor.execute(RETRIEVAL, "fixture", () -> {
            if (calls.incrementAndGet() == 1) throw http(429, headers,
                    "{\"error\":{\"details\":[{\"@type\":\"type.googleapis.com/google.rpc.RetryInfo\",\"retryDelay\":\"3.000000001s\"}]}}");
            return "vector";
        });
        assertThat(sleeps).containsExactly(3001L);
        assertThat(calls).hasValue(2);
    }

    @Test void respectsHttpDateRetryAfter() {
        var headers = new HttpHeaders(); headers.set("Retry-After", "Sun, 1 Nov 2026 00:00:04 GMT");
        AtomicInteger calls = new AtomicInteger();
        executor.execute(INTENT, "fixture", () -> {
            if (calls.incrementAndGet() == 1) throw http(503, headers, "");
            return true;
        });
        assertThat(sleeps).containsExactly(4000L);
    }

    @Test void dailyQuotaIsTerminalEvenWhenProviderSuggestsShortRetry() {
        try (var scope = context.ensure()) {
            assertThatThrownBy(() -> executor.execute(INTENT, "fixture", () -> { throw http(429, null,
                    "{\"error\":{\"details\":[{\"violations\":[{\"quotaId\":\"GenerateRequestsPerDayPerProjectPerModel\",\"quotaValue\":\"50\"}]},"
                    + "{\"@type\":\"type.googleapis.com/google.rpc.RetryInfo\",\"retryDelay\":\"1s\"}]}}");
            })).isInstanceOfSatisfying(AiCallFailure.class, e -> assertThat(e.reason()).isEqualTo("QUOTA_EXHAUSTED"));
            assertThat(sleeps).isEmpty();
            assertThat(context.report().retriesUsed()).isZero();
            assertThat(result(INTENT).attempts()).isEqualTo(1);
        }
    }

    @ParameterizedTest @ValueSource(ints = {400, 401, 403, 404})
    void permanentHttpErrorsNeverRetryOrDiscloseProviderBody(int status) {
        AtomicInteger calls = new AtomicInteger();
        assertThatThrownBy(() -> executor.execute(INTENT, "fixture", () -> {
            calls.incrementAndGet(); throw http(status, null, "sensitive-key-and-prompt");
        })).isInstanceOf(AiCallFailure.class).hasMessageNotContaining("sensitive");
        assertThat(calls).hasValue(1); assertThat(sleeps).isEmpty();
    }

    @Test void timeoutCanRecover() {
        AtomicInteger calls = new AtomicInteger();
        assertThat(executor.execute(INTENT, "fixture", () -> {
            if (calls.incrementAndGet() == 1) throw new ResourceAccessException("timeout", new SocketTimeoutException());
            return true;
        })).isTrue();
        assertThat(calls).hasValue(2);
    }

    @Test void refusesRetryWhoseWaitWouldExceedDeadline() {
        properties.setRequestDeadlineMillis(2000);
        var headers = new HttpHeaders(); headers.set("Retry-After", "3");
        try (var scope = context.ensure()) {
            assertThatThrownBy(() -> executor.execute(INTENT, "fixture", () -> { throw http(429, headers, ""); }))
                    .isInstanceOf(AiCallFailure.class);
            assertThat(sleeps).isEmpty();
            assertThat(result(INTENT).retryDecision()).isEqualTo("RETRY_WAIT_EXCEEDS_DEADLINE");
            assertThat(context.report().retriesUsed()).isZero();
        }
    }

    @Test void deadlineIsSharedAndExpiredStagesDoNotCallProvider() {
        properties.setRequestDeadlineMillis(1000);
        try (var scope = context.ensure()) {
            executor.execute(INTENT, "fixture", () -> { millis.addAndGet(900); return true; });
            millis.addAndGet(100);
            AtomicInteger calls = new AtomicInteger();
            assertThatThrownBy(() -> executor.execute(ALLOCATION, "fixture", () -> calls.incrementAndGet()))
                    .isInstanceOfSatisfying(AiCallFailure.class, e -> assertThat(e.reason()).isEqualTo("DEADLINE_EXCEEDED"));
            assertThat(calls).hasValue(0); assertThat(result(ALLOCATION).attempts()).isZero();
        }
    }

    @Test void resultArrivingAfterDeadlineIsNotAccepted() {
        properties.setRequestDeadlineMillis(1000);
        assertThatThrownBy(() -> executor.execute(INTENT, "fixture", () -> { millis.addAndGet(1000); return true; }))
                .isInstanceOfSatisfying(AiCallFailure.class, e -> assertThat(e.reason()).isEqualTo("DEADLINE_EXCEEDED"));
        assertThat(sleeps).isEmpty();
    }

    @Test void invalidJsonOrSemanticOutputNeverRetries() {
        assertThatThrownBy(() -> executor.execute(INTENT, "fixture", () -> { throw new AiCallFailure("INVALID_RESPONSE", null); }))
                .isInstanceOf(AiCallFailure.class);
        assertThat(sleeps).isEmpty();
    }

    @Test void closingScopeAfterFailureRemovesBudgetAndDiagnosticsFromReusedThread() {
        try (var scope = context.ensure()) {
            assertThatThrownBy(() -> executor.execute(INTENT, "fixture", () -> { throw http(503, null, ""); }))
                    .isInstanceOf(AiCallFailure.class);
            assertThat(context.report().retriesUsed()).isEqualTo(1);
        }
        try (var scope = context.ensure()) {
            assertThat(context.report().retriesUsed()).isZero();
            assertThat(result(INTENT).attempts()).isZero();
            assertThat(result(INTENT).source()).isEqualTo("SKIPPED");
        }
    }

    AiRequestContext.StageResult result(AiRequestContext.Stage stage) {
        return context.report().stages().stream().filter(s -> s.stage() == stage).findFirst().orElseThrow();
    }
    RestClientResponseException http(int status, HttpHeaders headers, String body) {
        return new RestClientResponseException("provider failure", status, "error", headers,
                body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}
