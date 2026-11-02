package com.travel.planning.draft.service;

import com.travel.planning.draft.dto.request.GenerateDraftRequest;
import com.travel.planning.retrieval.config.RagDemoProperties;
import com.travel.weather.service.WeatherService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;

import static com.travel.planning.draft.dto.response.DraftResponse.*;

/** Independent advisory I/O. Worker threads never touch the AI request context or database. */
@Service
@Profile("rag-demo")
public class DraftWeatherService {
    private final WeatherService weather;
    private final ExecutorService executor;
    private final RagDemoProperties properties;

    public DraftWeatherService(WeatherService weather, @Qualifier("ragDraftWeatherExecutor") ExecutorService executor,
                               RagDemoProperties properties) {
        this.weather = weather;
        this.executor = executor;
        this.properties = properties;
    }

    public PendingForecast start(GenerateDraftRequest request, int days) {
        long started = System.nanoTime();
        Future<Forecast> future;
        try {
            future = executor.submit(() -> {
                WeatherAdvice advice = fetch(request, days);
                return new Forecast(advice, elapsed(started), "UNKNOWN".equals(advice.status()) ? "REAL_FORECAST_UNAVAILABLE" : null);
            });
        } catch (RejectedExecutionException ex) {
            future = CompletableFuture.completedFuture(new Forecast(unknown(), 0, "WEATHER_EXECUTOR_BUSY"));
        }
        return new PendingForecast(future, started, Math.max(1, properties.getWeatherWaitMillis()));
    }

    private WeatherAdvice fetch(GenerateDraftRequest request, int days) {
        try {
            var forecast = weather.getForecast(request.origin().latitude(), request.origin().longitude(), request.startDate(), request.startDate().plusDays(days - 1));
            if (forecast == null || !"open-meteo".equals(forecast.providerName()) || forecast.dailySummaries() == null) return unknown();
            var summaries = forecast.dailySummaries().stream()
                    .filter(d -> !d.date().isBefore(request.startDate()) && d.date().isBefore(request.startDate().plusDays(days))).toList();
            if (summaries.size() != days) return unknown();
            return new WeatherAdvice("AVAILABLE_ORIGIN_ONLY", forecast.providerName(), "ORIGIN_COORDINATES_ONLY", summaries.stream()
                    .map(d -> new WeatherDay(d.date(), d.minTemperature(), d.maxTemperature(), d.maxPrecipitationProbability(),
                            d.dominantCondition() == null ? "UNKNOWN" : d.dominantCondition().name(),
                            d.highestRiskLevel() == null ? "UNKNOWN" : d.highestRiskLevel().name())).toList());
        } catch (Exception ex) { return unknown(); }
    }

    public static WeatherAdvice unknown() { return new WeatherAdvice("UNKNOWN", "UNAVAILABLE", "ORIGIN_COORDINATES_ONLY", List.of()); }
    private static long elapsed(long started) { return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started); }
    public record Forecast(WeatherAdvice advice, long elapsedMillis, String reason) {}

    public static final class PendingForecast implements AutoCloseable {
        private final Future<Forecast> future;
        private final long started, timeoutMillis;
        private PendingForecast(Future<Forecast> future, long started, long timeoutMillis) {
            this.future = future;
            this.started = started;
            this.timeoutMillis = timeoutMillis;
        }
        public Forecast await() {
            try {
                Forecast forecast = future.get(Math.max(0, timeoutMillis - elapsed(started)), TimeUnit.MILLISECONDS);
                return forecast.elapsedMillis() <= timeoutMillis ? forecast : new Forecast(unknown(), forecast.elapsedMillis(), "WEATHER_DEADLINE_EXCEEDED");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return new Forecast(unknown(), elapsed(started), "INTERRUPTED");
            } catch (TimeoutException ex) {
                return new Forecast(unknown(), elapsed(started), "WEATHER_DEADLINE_EXCEEDED");
            } catch (ExecutionException | CancellationException ex) {
                return new Forecast(unknown(), elapsed(started), "REAL_FORECAST_UNAVAILABLE");
            } finally { close(); }
        }
        @Override public void close() { future.cancel(true); }
    }
}
