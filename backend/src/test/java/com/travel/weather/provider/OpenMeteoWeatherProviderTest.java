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
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenMeteoWeatherProviderTest {

    private WeatherConfigProperties properties;
    private MockWeatherProvider fallbackProvider;
    private RestClient restClient;
    private MockRestServiceServer mockServer;
    private OpenMeteoWeatherProvider provider;

    @BeforeEach
    void setUp() {
        properties = new WeatherConfigProperties();
        fallbackProvider = new MockWeatherProvider();

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();

        provider = new OpenMeteoWeatherProvider(properties, fallbackProvider, restClient);
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
    @DisplayName("fetchForecast on API error should activate mock fallback without throwing exception")
    void fetchForecast_apiError_activatesFallback() {
        mockServer.expect(requestTo(containsString("/forecast")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        LocalDate date = LocalDate.of(2026, 10, 1);
        WeatherForecastResult result = provider.fetchForecast(16.05, 108.24, date, date);

        assertThat(result).isNotNull();
        assertThat(result.providerName()).isEqualTo("mock");
        assertThat(result.dailySummaries()).isNotEmpty();

        mockServer.verify();
    }
}
