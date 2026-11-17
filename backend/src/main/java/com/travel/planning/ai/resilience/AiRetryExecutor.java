package com.travel.planning.ai.resilience;

import com.fasterxml.jackson.databind.*;
import com.travel.planning.retrieval.config.RagProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

import java.math.*;
import java.net.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.*;

import static com.travel.planning.ai.resilience.AiRequestContext.Stage;

@Component
@Profile({"dev", "rag-demo", "planning-ai"})
public class AiRetryExecutor {

    @FunctionalInterface
    public interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    private record Failure(String reason, Integer status, boolean retryable, long delayMillis) {
    }

    private final RagProperties properties;
    private final AiRequestContext context;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final Sleeper sleeper;
    private final LongSupplier jitter;

    @Autowired
    public AiRetryExecutor(RagProperties properties, AiRequestContext context, ObjectMapper mapper) {
        this(properties, context, mapper, Clock.systemUTC(), Thread::sleep, () -> ThreadLocalRandom.current().nextLong(251));
    }

    public AiRetryExecutor(RagProperties properties, AiRequestContext context, ObjectMapper mapper,
                           Clock clock, Sleeper sleeper, LongSupplier jitter) {
        this.properties = properties;
        this.context = context;
        this.mapper = mapper;
        this.clock = clock;
        this.sleeper = sleeper;
        this.jitter = jitter;
    }

    public <T> T execute(Stage stage, String model, Supplier<T> operation) {
        try (var scope = context.ensure()) {
            boolean retry = false;
            while (true) {
                if (context.remainingMillis() <= 0) {
                    context.failure(stage, "DEADLINE_EXCEEDED", null);
                    context.retryDecision(stage, "DEADLINE_EXCEEDED");
                    throw new AiCallFailure("DEADLINE_EXCEEDED", null);
                }
                if (retry && !context.claimRetry()) throw new IllegalStateException("AI retry reservation changed");
                context.attempt(stage, model, retry);
                try {
                    T result = operation.get();
                    if (context.remainingMillis() <= 0) throw new AiCallFailure("DEADLINE_EXCEEDED", null);
                    context.record(stage, "AI", "GEMINI", retry ? "RECOVERED_AFTER_RETRY" : null, null);
                    if (retry) context.retryDecision(stage, "RETRY_SUCCEEDED");
                    return result;
                } catch (AiCallFailure | RestClientException ex) {
                    Failure failure = classify(ex);
                    context.failure(stage, failure.reason(), failure.status());
                    if (!failure.retryable()) {
                        context.retryDecision(stage, "NOT_RETRYABLE");
                        throw new AiCallFailure(failure.reason(), failure.status());
                    }
                    if (!context.canRetry()) {
                        context.retryDecision(stage, "REQUEST_RETRY_LIMIT_REACHED");
                        throw new AiCallFailure(failure.reason(), failure.status());
                    }
                    long backoff = Math.max(0, Math.min(properties.getRetryBackoffMillis(), 5000)) + Math.max(0, Math.min(jitter.getAsLong(), 250));
                    long delay = Math.max(backoff, failure.delayMillis());
                    if (delay >= context.remainingMillis()) {
                        context.retryDecision(stage, "RETRY_WAIT_EXCEEDS_DEADLINE");
                        throw new AiCallFailure(failure.reason(), failure.status());
                    }
                    try {
                        sleeper.sleep(delay);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        context.failure(stage, "INTERRUPTED", null);
                        context.retryDecision(stage, "INTERRUPTED");
                        throw new AiCallFailure("INTERRUPTED", null);
                    }
                    retry = true;
                }
            }
        }
    }

    private Failure classify(RuntimeException ex) {
        if (ex instanceof AiCallFailure failure)
            return new Failure(failure.reason(), failure.providerStatus(), false, 0);
        if (ex instanceof RestClientResponseException response) {
            int status = response.getStatusCode().value();
            JsonNode body = null;
            try {
                body = mapper.readTree(response.getResponseBodyAsString());
            } catch (Exception ignored) { /* Optional provider diagnostics; classification still uses status. */ }
            boolean quota = false;
            long retryInfo = 0;
            if (body != null) for (JsonNode detail : body.path("error").path("details")) {
                for (JsonNode violation : detail.path("violations")) {
                    String id = (violation.path("quotaId").asText() + " " + violation.path("quotaMetric").asText()).toLowerCase(Locale.ROOT);
                    quota |= id.contains("perday") || id.contains("per_day") || id.contains("daily") || id.contains("permonth")
                            || id.contains("per_month") || id.contains("spend") || violation.path("quotaValue").asText().equals("0");
                }
                if (detail.path("@type").asText().endsWith("google.rpc.RetryInfo"))
                    retryInfo = Math.max(retryInfo, seconds(detail.path("retryDelay").asText()));
            }
            long headerWait = 0;
            if (response.getResponseHeaders() != null) {
                String value = response.getResponseHeaders().getFirst("Retry-After");
                if (value != null) {
                    try {
                        headerWait = seconds(value + "s");
                    } catch (Exception ignored) { /* Date-form Retry-After is handled below. */ }
                    if (headerWait == 0) try {
                        headerWait = Math.max(0, Duration.between(clock.instant(), ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()).toMillis());
                    } catch (
                            Exception ignored) { /* Invalid optional header; use backoff rather than guessing a server delay. */ }
                }
            }
            String reason = status == 429 ? quota ? "QUOTA_EXHAUSTED" : "RATE_LIMITED"
                    : status == 401 || status == 403 ? "KEY_OR_PERMISSION_REJECTED"
                    : status == 404 ? "MODEL_NOT_AVAILABLE" : status >= 500 ? "PROVIDER_UNAVAILABLE" : "PROVIDER_REQUEST_REJECTED";
            return new Failure(reason, status, !quota && (status == 429 || status == 500 || status == 502 || status == 503 || status == 504), Math.max(headerWait, retryInfo));
        }
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException || cause instanceof java.net.http.HttpTimeoutException)
                return new Failure("TIMEOUT", null, true, 0);
        }
        return new Failure("NETWORK_OR_CLIENT_ERROR", null, false, 0);
    }

    private long seconds(String value) {
        if (!value.endsWith("s")) return 0;
        try {
            BigDecimal millis = new BigDecimal(value.substring(0, value.length() - 1).trim()).multiply(BigDecimal.valueOf(1000));
            return millis.signum() <= 0 ? 0 : millis.min(BigDecimal.valueOf(Long.MAX_VALUE)).setScale(0, RoundingMode.CEILING).longValueExact();
        } catch (NumberFormatException | ArithmeticException ex) {
            return 0;
        }
    }
}
