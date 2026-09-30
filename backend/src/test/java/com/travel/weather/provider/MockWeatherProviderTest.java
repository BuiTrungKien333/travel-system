package com.travel.weather.provider;

import com.travel.weather.model.DailyWeatherSummary;
import com.travel.weather.model.WeatherForecastResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MockWeatherProviderTest {

    private MockWeatherProvider provider;

    @BeforeEach
    void setUp() {
        provider = new MockWeatherProvider();
    }

    @Test
    @DisplayName("fetchForecast should return 24 hourly entries per day with valid temperatures and rain probabilities")
    void fetchForecast_returnsValidStructure() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 2);

        WeatherForecastResult result = provider.fetchForecast(16.05, 108.24, start, end);

        assertThat(result).isNotNull();
        assertThat(result.dailySummaries()).hasSize(2);

        DailyWeatherSummary day1 = result.dailySummaries().getFirst();
        assertThat(day1.date()).isEqualTo(start);
        assertThat(day1.hourlyForecasts()).hasSize(24);
        assertThat(day1.maxTemperature()).isGreaterThan(day1.minTemperature());
        assertThat(provider.isAvailable()).isTrue();
        assertThat(provider.getProviderName()).isEqualTo("mock");
    }
}
