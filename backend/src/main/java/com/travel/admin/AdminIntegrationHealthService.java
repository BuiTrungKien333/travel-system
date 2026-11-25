package com.travel.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoTimeoutException;
import com.travel.maps.service.MapboxDirectionsService;
import com.travel.planning.ai.config.AiConfigProperties;
import com.travel.planning.retrieval.config.RagProperties;
import com.travel.telemetry.MongoExecutionStore;
import com.travel.weather.provider.WeatherProviderFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Profile("!test")
@Slf4j
public class AdminIntegrationHealthService {

    public record Check(
        String key,
        String name,
        String status,
        Instant checkedAt,
        Instant lastSuccessAt,
        long durationMillis,
        String detailCode,
        boolean stale
    ) {}

    public record Snapshot(Instant asOf, long intervalMillis, boolean checking, List<Check> integrations) {}

    public record Trigger(boolean started, boolean checking) {}

    private record Integration(String key, String name) {}

    @FunctionalInterface
    private interface Probe {
        ProbeResult run() throws Exception;
    }

    private record ProbeResult(String status, String detailCode) {}

    private static final List<Integration> INTEGRATIONS = List.of(
        new Integration("postgresql", "PostgreSQL"),
        new Integration("mongodb", "MongoDB · telemetry"),
        new Integration("gemini", "Gemini · lập lịch"),
        new Integration("embedding", "Gemini · embedding RAG"),
        new Integration("mapbox", "Mapbox · định tuyến"),
        new Integration("weather", "Dịch vụ thời tiết"),
        new Integration("cloudinary", "Cloudinary · ảnh")
    );

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final MongoExecutionStore mongo;
    private final MapboxDirectionsService mapbox;
    private final WeatherProviderFactory weather;
    private final ObjectProvider<RagProperties> ragProperties;
    private final AiConfigProperties aiProperties;
    private final RestClient client;
    private final String mapboxToken;
    private final String cloudinaryName;
    private final String cloudinaryKey;
    private final String cloudinarySecret;
    private final long intervalMillis;
    private final AtomicBoolean checking = new AtomicBoolean();

    public AdminIntegrationHealthService(
        JdbcTemplate jdbc,
        ObjectMapper objectMapper,
        Clock clock,
        MongoExecutionStore mongo,
        MapboxDirectionsService mapbox,
        WeatherProviderFactory weather,
        ObjectProvider<RagProperties> ragProperties,
        AiConfigProperties aiProperties,
        @Value("${application.maps.mapbox.access-token:}") String mapboxToken,
        @Value("${app.cloudinary.cloud-name:}") String cloudinaryName,
        @Value("${app.cloudinary.api-key:}") String cloudinaryKey,
        @Value("${app.cloudinary.api-secret:}") String cloudinarySecret,
        @Value("${application.integration-health.interval-ms:1800000}") long intervalMillis
    ) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.mongo = mongo;
        this.mapbox = mapbox;
        this.weather = weather;
        this.ragProperties = ragProperties;
        this.aiProperties = aiProperties;
        this.mapboxToken = clean(mapboxToken);
        this.cloudinaryName = clean(cloudinaryName);
        this.cloudinaryKey = clean(cloudinaryKey);
        this.cloudinarySecret = clean(cloudinarySecret);
        if (intervalMillis < 60_000) throw new IllegalArgumentException(
            "Integration health interval must be at least one minute"
        );
        this.intervalMillis = intervalMillis;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    @Scheduled(
        fixedDelayString = "${application.integration-health.interval-ms:1800000}",
        initialDelayString = "${application.integration-health.initial-delay-ms:15000}"
    )
    public void scheduleChecks() {
        startChecks();
    }

    public Trigger triggerChecks() {
        return new Trigger(startChecks(), true);
    }

    private boolean startChecks() {
        if (!checking.compareAndSet(false, true)) return false;
        CompletableFuture.runAsync(this::runChecks).whenComplete((ignored, failure) -> {
            checking.set(false);
            if (failure != null) log.warn("Integration health checks stopped ({})", failure.getClass().getSimpleName());
        });
        return true;
    }

    private void runChecks() {
        List<CompletableFuture<Void>> checks = List.of(
            checkAsync("postgresql", () -> {
                jdbc.queryForObject("SELECT 1", Integer.class);
                return up();
            }),
            checkAsync("mongodb", this::checkMongo),
            checkAsync("gemini", () -> checkGemini(false)),
            checkAsync("embedding", () -> checkGemini(true)),
            checkAsync("mapbox", this::checkMapbox),
            checkAsync("weather", this::checkWeather),
            checkAsync("cloudinary", this::checkCloudinary)
        );
        CompletableFuture.allOf(checks.toArray(CompletableFuture[]::new)).join();
    }

    private CompletableFuture<Void> checkAsync(String key, Probe probe) {
        return CompletableFuture.runAsync(() -> checkOne(key, probe));
    }

    private ProbeResult checkMongo() {
        if (!mongo.enabled()) return notConfigured();
        mongo.ping();
        return up();
    }

    private ProbeResult checkGemini(boolean embedding) {
        RagProperties rag = ragProperties.getIfAvailable();
        String key = rag != null ? clean(rag.getApiKey()) : clean(aiProperties.getGemini().getApiKey());
        if (key == null) return notConfigured();
        String model = embedding
            ? rag == null
                ? null
                : clean(rag.getEmbeddingModel())
            : rag == null
              ? clean(aiProperties.getGemini().getModel())
              : clean(rag.getIntentModel());
        if (model == null) return notConfigured();
        String baseUrl = rag == null ? aiProperties.getGemini().getBaseUrl() : rag.getBaseUrl();
        URI uri = UriComponentsBuilder.fromUriString(baseUrl).pathSegment("models", model).build().encode().toUri();
        String raw = client.get().uri(uri).header("x-goog-api-key", key).retrieve().body(String.class);
        JsonNode response = parseJson(raw);
        String method = embedding ? "embedContent" : "generateContent";
        JsonNode methods = response == null ? null : response.path("supportedGenerationMethods");
        if (
            response == null || !response.path("name").asText().endsWith(model) || methods == null || !methods.isArray()
        ) return new ProbeResult("DOWN", "INVALID_RESPONSE");
        boolean supported = false;
        for (JsonNode value : methods) if (method.equals(value.asText())) supported = true;
        if (!supported) return new ProbeResult("DOWN", "MODEL_METHOD_UNSUPPORTED");
        return up();
    }

    private ProbeResult checkMapbox() {
        if (mapboxToken == null) return notConfigured();
        mapbox.checkConnection();
        return up();
    }

    private ProbeResult checkWeather() {
        var provider = weather.getProvider();
        if ("mock".equalsIgnoreCase(provider.getProviderName())) return new ProbeResult("MOCK", "MOCK_PROVIDER_ACTIVE");
        if (provider.fetchCurrentWeather(16.0544, 108.2022) == null) return new ProbeResult("DOWN", "INVALID_RESPONSE");
        return up();
    }

    private ProbeResult checkCloudinary() {
        if (cloudinaryName == null || cloudinaryKey == null || cloudinarySecret == null) return notConfigured();
        URI uri = UriComponentsBuilder.fromUriString("https://api.cloudinary.com/v1_1")
            .pathSegment(cloudinaryName, "ping")
            .build()
            .encode()
            .toUri();
        String raw = client
            .get()
            .uri(uri)
            .headers(headers -> headers.setBasicAuth(cloudinaryKey, cloudinarySecret))
            .retrieve()
            .body(String.class);
        JsonNode response = parseJson(raw);
        if (response == null || !"ok".equalsIgnoreCase(response.path("status").asText())) return new ProbeResult(
            "DOWN",
            "INVALID_RESPONSE"
        );
        return up();
    }

    private void checkOne(String key, Probe probe) {
        long started = System.nanoTime();
        Instant checkedAt = clock.instant();
        ProbeResult result;
        try {
            result = probe.run();
        } catch (RestClientResponseException error) {
            log.warn("Integration health check rejected for {} (HTTP {})", key, error.getStatusCode().value());
            result = new ProbeResult("DOWN", "HTTP_" + error.getStatusCode().value());
        } catch (MongoTimeoutException error) {
            log.warn("Integration health check timed out for {} (MongoDB)", key);
            result = new ProbeResult("DOWN", "MONGODB_TIMEOUT");
        } catch (ResourceAccessException error) {
            String code = networkCode(error);
            log.warn("Integration health check network failure for {} ({})", key, code);
            result = new ProbeResult("DOWN", code);
        } catch (Exception error) {
            String type = error.getClass().getSimpleName().replaceAll("[^A-Za-z0-9]", "").toUpperCase();
            String code = "UNEXPECTED_" + type;
            if (code.length() > 64) code = code.substring(0, 64);
            log.warn("Integration health check failed unexpectedly for {} ({})", key, type);
            result = new ProbeResult("DOWN", code);
        }
        long duration = Math.max(0, Duration.ofNanos(System.nanoTime() - started).toMillis());
        try {
            jdbc.update(
                """
                INSERT INTO integration_health_checks(integration_key,status,checked_at,last_success_at,duration_ms,detail_code)
                VALUES(?,?,?,?,?,?) ON CONFLICT(integration_key) DO UPDATE SET
                  status=EXCLUDED.status, checked_at=EXCLUDED.checked_at,
                  last_success_at=COALESCE(EXCLUDED.last_success_at,integration_health_checks.last_success_at),
                  duration_ms=EXCLUDED.duration_ms, detail_code=EXCLUDED.detail_code
                """,
                key,
                result.status(),
                Timestamp.from(checkedAt),
                "UP".equals(result.status()) ? Timestamp.from(checkedAt) : null,
                duration,
                result.detailCode()
            );
        } catch (Exception persistenceFailure) {
            log.warn("Could not persist health check for {} ({})", key, persistenceFailure.getClass().getSimpleName());
        }
    }

    public Snapshot snapshot() {
        Map<String, Check> saved = jdbc
            .query(
                "SELECT integration_key,status,checked_at,last_success_at,duration_ms,detail_code FROM integration_health_checks",
                (rs, row) ->
                    new Check(
                        rs.getString("integration_key"),
                        null,
                        rs.getString("status"),
                        rs.getTimestamp("checked_at").toInstant(),
                        rs.getTimestamp("last_success_at") == null
                            ? null
                            : rs.getTimestamp("last_success_at").toInstant(),
                        rs.getLong("duration_ms"),
                        rs.getString("detail_code"),
                        false
                    )
            )
            .stream()
            .collect(java.util.stream.Collectors.toMap(Check::key, check -> check));
        Instant now = clock.instant();
        Instant staleAfter = now.minusMillis(intervalMillis * 2);
        List<Check> checks = INTEGRATIONS.stream()
            .map(integration -> {
                Check check = saved.get(integration.key());
                if (check == null) return new Check(
                    integration.key(),
                    integration.name(),
                    "NOT_CHECKED",
                    null,
                    null,
                    0,
                    null,
                    false
                );
                return new Check(
                    check.key(),
                    integration.name(),
                    check.status(),
                    check.checkedAt(),
                    check.lastSuccessAt(),
                    check.durationMillis(),
                    check.detailCode(),
                    check.checkedAt().isBefore(staleAfter)
                );
            })
            .toList();
        return new Snapshot(now, intervalMillis, checking.get(), checks);
    }

    private String networkCode(ResourceAccessException error) {
        Throwable cause = error;
        while (cause != null) {
            if (
                cause instanceof SocketTimeoutException || cause.getClass().getSimpleName().contains("Timeout")
            ) return "TIMEOUT";
            if (cause instanceof UnknownHostException) return "DNS_ERROR";
            if (cause instanceof ConnectException) return "CONNECTION_REFUSED";
            cause = cause.getCause();
        }
        return "NETWORK_ERROR";
    }

    private JsonNode parseJson(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            return objectMapper.readTree(body);
        } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
            return null;
        }
    }

    private static ProbeResult up() {
        return new ProbeResult("UP", null);
    }

    private static ProbeResult notConfigured() {
        return new ProbeResult("NOT_CONFIGURED", null);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
