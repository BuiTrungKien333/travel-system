package com.travel.weather.provider;

import com.travel.weather.config.WeatherConfigProperties;
import com.travel.weather.enums.WeatherCondition;
import com.travel.weather.model.DailyWeatherSummary;
import com.travel.weather.model.WeatherForecastResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenMeteoWeatherProviderTest {

    private WeatherConfigProperties properties;
    private final com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();
    private RestClient restClient;
    private MockRestServiceServer mockServer;
    private OpenMeteoWeatherProvider provider;

    @BeforeEach
    void setUp() {
        properties = new WeatherConfigProperties();

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();

        provider = new OpenMeteoWeatherProvider(properties, restClient, json);
    }

    @Test
    @DisplayName("fetchForecast with successful Open-Meteo response should parse hourly and daily correctly")
    void fetchForecast_successResponse_parsesCorrectly() {
        String mockJson = """
                {
                  "latitude": 16.05,
                  "longitude": 108.24,
                  "timezone": "Asia/Ho_Chi_Minh",
                  "hourly": {
                    "time": ["2026-10-01T08:00", "2026-10-01T14:00"],
                    "temperature_2m": [28.5, 34.0],
                    "precipitation_probability": [10, 75],
                    "rain": [0.0, 5.2],
                    "weather_code": [0, 65]
                  }
                }
                """;

        mockServer.expect(requestTo(containsString("/forecast")))
                .andRespond(withSuccess(mockJson, MediaType.APPLICATION_JSON));

        LocalDate date = LocalDate.of(2026, 10, 1);
        WeatherForecastResult result = provider.fetchForecast(16.05, 108.24, date, date);

        assertThat(result).isNotNull();
        assertThat(result.providerName()).isEqualTo("open-meteo");
        assertThat(result.dailySummaries()).hasSize(1);

        DailyWeatherSummary summary = result.dailySummaries().getFirst();
        assertThat(summary.date()).isEqualTo(date);
        assertThat(summary.hourlyForecasts()).hasSize(2);
        assertThat(summary.hourlyForecasts().get(0).condition()).isEqualTo(WeatherCondition.CLEAR_SKY);
        assertThat(summary.hourlyForecasts().get(1).condition()).isEqualTo(WeatherCondition.HEAVY_RAIN);
        assertThat(summary.maxPrecipitationProbability()).isEqualTo(75);

        mockServer.verify();
    }

    @Test
    @DisplayName("fetchForecast on API error should report unavailable without fabricating weather")
    void fetchForecast_apiError_reportsUnavailable() {
        mockServer.expect(requestTo(containsString("/forecast")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        LocalDate date = LocalDate.of(2026, 10, 1);
        assertThatThrownBy(() -> provider.fetchForecast(16.05, 108.24, date, date))
                .isInstanceOfSatisfying(AppException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.WEATHER_UNAVAILABLE));

        mockServer.verify();
    }

    @Test
    @DisplayName("fetchCurrentWeather with successful Open-Meteo response should parse current weather correctly")
    void fetchCurrentWeather_successResponse_parsesCorrectly() {
        String mockJson = """
                {
                  "latitude": 16.06,
                  "longitude": 108.25,
                  "timezone": "Asia/Ho_Chi_Minh",
                  "current": {
                    "time": "2026-09-23T16:30",
                    "temperature_2m": 29.5,
                    "relative_humidity_2m": 78,
                    "apparent_temperature": 33.8,
                    "is_day": 1,
                    "precipitation": 0.0,
                    "rain": 0.0,
                    "weather_code": 1,
                    "wind_speed_10m": 12.5
                  }
                }
                """;

        mockServer.expect(requestTo(containsString("/forecast")))
                .andRespond(withSuccess(mockJson, MediaType.APPLICATION_JSON));

        var current = provider.fetchCurrentWeather(16.06, 108.25);

        assertThat(current).isNotNull();
        assertThat(current.temperature()).isEqualTo(29.5);
        assertThat(current.apparentTemperature()).isEqualTo(33.8);
        assertThat(current.humidity()).isEqualTo(78);
        assertThat(current.rainMm()).isEqualTo(0.0);
        assertThat(current.windSpeedKmH()).isEqualTo(12.5);
        assertThat(current.isDay()).isTrue();
        assertThat(current.condition()).isEqualTo(WeatherCondition.PARTLY_CLOUDY);
        assertThat(current.riskLevel()).isNotNull();
        assertThat(current.summary()).contains("29.5°C");

        mockServer.verify();
    }

    @Test
    @DisplayName("fetchCurrentWeather on API error should report unavailable without fabricating weather")
    void fetchCurrentWeather_apiError_reportsUnavailable() {
        mockServer.expect(requestTo(containsString("/forecast")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> provider.fetchCurrentWeather(16.06, 108.25))
                .isInstanceOfSatisfying(AppException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.WEATHER_UNAVAILABLE));

        mockServer.verify();
    }
    @Test
    void missingMeasurementsDoNotBecomeSunnyDefaults() {
        mockServer.expect(requestTo(containsString("/forecast"))).andRespond(withSuccess("""
            {"timezone":"Asia/Ho_Chi_Minh","hourly":{"time":["2026-10-01T08:00"],"weather_code":[0]}}
            """, MediaType.APPLICATION_JSON));
        var date = LocalDate.of(2026,10,1);
        assertThatThrownBy(() -> provider.fetchForecast(16,108,date,date)).isInstanceOfSatisfying(AppException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.WEATHER_INVALID_RESPONSE));
        mockServer.verify();
    }

    @Test
    void missingCurrentMeasurementsDoNotBecomeDefaultTemperature() {
        mockServer.expect(requestTo(containsString("/forecast"))).andRespond(withSuccess("""
            {"timezone":"Asia/Ho_Chi_Minh","current":{"time":"2026-10-01T08:00","weather_code":0}}
            """, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> provider.fetchCurrentWeather(16,108)).isInstanceOfSatisfying(AppException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.WEATHER_INVALID_RESPONSE));
        mockServer.verify();
    }

    @Test
    void partiallyMissingHoursDoNotDiscardAvailableForecast() {
        mockServer.expect(requestTo(containsString("/forecast"))).andRespond(withSuccess("""
            {"timezone":"Asia/Ho_Chi_Minh","hourly":{"time":["2026-10-01T08:00","2026-10-01T09:00"],
             "weather_code":[61,null],"temperature_2m":[22.3,null],"rain":[2.0,null],"precipitation_probability":[70,null]}}
            """, MediaType.APPLICATION_JSON));
        var date = LocalDate.of(2026, 10, 1);
        var result = provider.fetchForecast(16, 108, date, date);
        var day = result.dailySummaries().getFirst();
        assertThat(day.hourlyForecasts()).hasSize(1);
        assertThat(day.maxTemperature()).isEqualTo(22.3);
        assertThat(day.maxPrecipitationProbability()).isEqualTo(70);
        mockServer.verify();
    }
}
