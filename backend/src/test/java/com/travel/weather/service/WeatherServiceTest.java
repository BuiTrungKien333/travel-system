package com.travel.weather.service;

import com.travel.weather.enums.WeatherCondition;
import com.travel.weather.enums.WeatherRiskLevel;
import com.travel.weather.model.DailyWeatherSummary;
import com.travel.weather.model.HourlyWeather;
import com.travel.weather.model.WeatherForecastResult;
import com.travel.weather.provider.WeatherProvider;
import com.travel.weather.provider.WeatherProviderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private WeatherProviderFactory providerFactory;

    @Mock
    private WeatherProvider weatherProvider;

    private WeatherServiceImpl weatherService;

    @BeforeEach
    void setUp() {
        weatherService = new WeatherServiceImpl(providerFactory);
    }

    @Test
    @DisplayName("isOutdoorHazardous should return true when risk level is HAZARDOUS")
    void isOutdoorHazardous_hazardousWeather_returnsTrue() {
        when(providerFactory.getProvider()).thenReturn(weatherProvider);

        LocalDate date = LocalDate.of(2026, 10, 1);
        LocalDateTime time = date.atTime(14, 0);

        HourlyWeather hourly = new HourlyWeather(
                time, 28.0, 90, 15.0, 95, WeatherCondition.THUNDERSTORM, WeatherRiskLevel.HAZARDOUS
        );

        DailyWeatherSummary daily = new DailyWeatherSummary(
                date, 24.0, 30.0, 90, 15.0, WeatherCondition.THUNDERSTORM, WeatherRiskLevel.HAZARDOUS, List.of(hourly)
        );

        WeatherForecastResult forecastResult = new WeatherForecastResult(
                16.05, 108.24, "Asia/Ho_Chi_Minh", List.of(daily), "mock"
        );

        when(weatherProvider.fetchForecast(anyDouble(), anyDouble(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(forecastResult);

        boolean hazardous = weatherService.isOutdoorHazardous(16.05, 108.24, time);

        assertThat(hazardous).isTrue();
    }

    @Test
    @DisplayName("isOutdoorHazardous should return false when weather is SAFE")
    void isOutdoorHazardous_safeWeather_returnsFalse() {
        when(providerFactory.getProvider()).thenReturn(weatherProvider);

        LocalDate date = LocalDate.of(2026, 10, 1);
        LocalDateTime time = date.atTime(8, 0);

        HourlyWeather hourly = new HourlyWeather(
                time, 27.0, 10, 0.0, 0, WeatherCondition.CLEAR_SKY, WeatherRiskLevel.SAFE
        );

        DailyWeatherSummary daily = new DailyWeatherSummary(
                date, 24.0, 32.0, 10, 0.0, WeatherCondition.CLEAR_SKY, WeatherRiskLevel.SAFE, List.of(hourly)
        );

        WeatherForecastResult forecastResult = new WeatherForecastResult(
                16.05, 108.24, "Asia/Ho_Chi_Minh", List.of(daily), "mock"
        );

        when(weatherProvider.fetchForecast(anyDouble(), anyDouble(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(forecastResult);

        boolean hazardous = weatherService.isOutdoorHazardous(16.05, 108.24, time);

        assertThat(hazardous).isFalse();
    }

    @Test
    @DisplayName("getForecastAt with null dateTime should return empty")
    void getForecastAt_nullDateTime_returnsEmpty() {
        Optional<HourlyWeather> result = weatherService.getForecastAt(16.05, 108.24, null);

        assertThat(result).isEmpty();
    }
}
