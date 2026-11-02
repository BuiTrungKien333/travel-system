package com.travel.planning.draft;

import com.travel.planning.draft.dto.request.GenerateDraftRequest;
import com.travel.planning.draft.service.DraftWeatherService;
import com.travel.planning.retrieval.config.RagDemoProperties;
import com.travel.weather.model.WeatherForecastResult;
import com.travel.weather.service.WeatherService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.*;

import static com.travel.planning.draft.DraftFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DraftWeatherServiceTest {
    final WeatherService provider = mock(WeatherService.class);
    final ExecutorService executor = Executors.newSingleThreadExecutor();
    final RagDemoProperties properties = new RagDemoProperties();
    final DraftWeatherService service = new DraftWeatherService(provider, executor, properties);
    final GenerateDraftRequest request = new GenerateDraftRequest("demo", DATE, ORIGIN, true, 45);
    @AfterEach void close() { executor.shutdownNow(); }

    @Test void mockForecastIsAlwaysUnknown() {
        when(provider.getForecast(anyDouble(), anyDouble(), any(), any())).thenReturn(new WeatherForecastResult(16, 108, "Asia/Ho_Chi_Minh", List.of(), "mock"));
        try (var pending = service.start(request, 2)) {
            var result = pending.await();
            assertThat(result.advice().status()).isEqualTo("UNKNOWN");
            assertThat(result.advice().days()).isEmpty();
            assertThat(result.reason()).isEqualTo("REAL_FORECAST_UNAVAILABLE");
        }
    }

    @Test void slowAdvisoryIsBoundedAndCancelledWithoutBlockingTheItinerary() throws Exception {
        properties.setWeatherWaitMillis(1);
        var entered = new CountDownLatch(1); var interrupted = new CountDownLatch(1);
        when(provider.getForecast(anyDouble(), anyDouble(), any(), any())).thenAnswer(invocation -> {
            entered.countDown();
            try { new CountDownLatch(1).await(5, TimeUnit.SECONDS); }
            catch (InterruptedException ex) { interrupted.countDown(); throw ex; }
            return null;
        });
        try (var pending = service.start(request, 2)) {
            assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
            var result = pending.await();
            assertThat(result.advice().status()).isEqualTo("UNKNOWN");
            assertThat(result.reason()).isEqualTo("WEATHER_DEADLINE_EXCEEDED");
            assertThat(interrupted.await(2, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test void saturatedOrStoppedExecutorDegradesToUnknown() {
        executor.shutdownNow();
        try (var pending = service.start(request, 2)) {
            assertThat(pending.await().reason()).isEqualTo("WEATHER_EXECUTOR_BUSY");
        }
        verifyNoInteractions(provider);
    }
}
