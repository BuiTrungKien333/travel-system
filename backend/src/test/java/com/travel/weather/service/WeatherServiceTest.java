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
    @DisplayName("getForecastAt should return the forecast for the requested hour")
    void getForecastAt_matchingHour_returnsHourlyForecast() {
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

        assertThat(weatherService.getForecastAt(16.05, 108.24, time)).contains(hourly);
    }

    @Test
    @DisplayName("getForecastAt with null dateTime should return empty")
    void getForecastAt_nullDateTime_returnsEmpty() {
        Optional<HourlyWeather> result = weatherService.getForecastAt(16.05, 108.24, null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getCurrentWeather should delegate to current WeatherProvider")
    void getCurrentWeather_delegatesToProvider() {
        when(providerFactory.getProvider()).thenReturn(weatherProvider);

        com.travel.weather.model.CurrentWeather mockCurrent = new com.travel.weather.model.CurrentWeather(
                16.05, 108.24, LocalDateTime.now(), 29.0, 33.0, 75, 0.0, 10.0, true, 1,
                WeatherCondition.PARTLY_CLOUDY, WeatherRiskLevel.SAFE, null
        );

        when(weatherProvider.fetchCurrentWeather(16.05, 108.24)).thenReturn(mockCurrent);

        var result = weatherService.getCurrentWeather(16.05, 108.24);

        assertThat(result).isNotNull();
        assertThat(result.temperature()).isEqualTo(29.0);
        assertThat(result.condition()).isEqualTo(WeatherCondition.PARTLY_CLOUDY);
    }
}
