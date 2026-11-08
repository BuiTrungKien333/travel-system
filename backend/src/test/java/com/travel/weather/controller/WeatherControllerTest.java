package com.travel.weather.controller;

import com.travel.weather.enums.WeatherCondition;
import com.travel.weather.enums.WeatherRiskLevel;
import com.travel.weather.model.CurrentWeather;
import com.travel.weather.model.DailyWeatherSummary;
import com.travel.weather.model.HourlyWeather;
import com.travel.weather.model.WeatherForecastResult;
import com.travel.weather.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WeatherControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WeatherService weatherService;

    @Mock
    private com.travel.common.util.Translator translator;

    @InjectMocks
    private WeatherController weatherController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(weatherController)
                .setControllerAdvice(new com.travel.common.exception.GlobalExceptionHandler(translator))
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/public/weather/current should return 200 and current weather details")
    void getCurrentWeather_success_returns200() throws Exception {
        CurrentWeather mockWeather = new CurrentWeather(
                16.0592,
                108.2464,
                LocalDateTime.of(2026, 9, 23, 16, 0),
                29.5,
                33.8,
                78,
                0.0,
                12.5,
                true,
                1,
                WeatherCondition.PARTLY_CLOUDY,
                WeatherRiskLevel.SAFE,
                "Có mây, nắng nhẹ, 29.5°C (cảm giác như 33.8°C)"
        );

        when(weatherService.getCurrentWeather(16.0592, 108.2464)).thenReturn(mockWeather);

        mockMvc.perform(get("/api/v1/public/weather/current")
                        .param("latitude", "16.0592")
                        .param("longitude", "108.2464")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.temperature").value(29.5))
                .andExpect(jsonPath("$.data.apparentTemperature").value(33.8))
                .andExpect(jsonPath("$.data.humidity").value(78))
                .andExpect(jsonPath("$.data.condition").value("PARTLY_CLOUDY"))
                .andExpect(jsonPath("$.data.riskLevel").value("SAFE"))
                .andExpect(jsonPath("$.data.isDay").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/public/weather/forecast default should return compact daily summary without hourly forecasts")
    void getWeatherForecast_defaultCompact_omitsHourlyForecasts() throws Exception {
        LocalDate today = LocalDate.of(2026, 10, 1);
        HourlyWeather hourly = new HourlyWeather(
                today.atTime(10, 0),
                28.0,
                10,
                0.0,
                0,
                WeatherCondition.CLEAR_SKY,
                WeatherRiskLevel.SAFE
        );
        DailyWeatherSummary summary = new DailyWeatherSummary(
                today,
                24.0,
                32.0,
                20,
                0.0,
                WeatherCondition.CLEAR_SKY,
                WeatherRiskLevel.SAFE,
                List.of(hourly)
        );

        WeatherForecastResult forecastResult = new WeatherForecastResult(
                16.0592,
                108.2464,
                "Asia/Ho_Chi_Minh",
                List.of(summary),
                "open-meteo"
        );

        when(weatherService.getForecast(eq(16.0592), eq(108.2464), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(forecastResult);

        mockMvc.perform(get("/api/v1/public/weather/forecast")
                        .param("latitude", "16.0592")
                        .param("longitude", "108.2464")
                        .param("startDate", "2026-10-01")
                        .param("endDate", "2026-10-01")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.providerName").value("open-meteo"))
                .andExpect(jsonPath("$.data.dailySummaries").isArray())
                .andExpect(jsonPath("$.data.dailySummaries[0].dominantCondition").value("CLEAR_SKY"))
                .andExpect(jsonPath("$.data.dailySummaries[0].hourlyForecasts").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/public/weather/forecast with includeHourly=true should include hourly forecasts")
    void getWeatherForecast_withIncludeHourlyTrue_returnsHourlyForecasts() throws Exception {
        LocalDate today = LocalDate.of(2026, 10, 1);
        HourlyWeather hourly = new HourlyWeather(
                today.atTime(10, 0),
                28.0,
                10,
                0.0,
                0,
                WeatherCondition.CLEAR_SKY,
                WeatherRiskLevel.SAFE
        );
        DailyWeatherSummary summary = new DailyWeatherSummary(
                today,
                24.0,
                32.0,
                20,
                0.0,
                WeatherCondition.CLEAR_SKY,
                WeatherRiskLevel.SAFE,
                List.of(hourly)
        );

        WeatherForecastResult forecastResult = new WeatherForecastResult(
                16.0592,
                108.2464,
                "Asia/Ho_Chi_Minh",
                List.of(summary),
                "open-meteo"
        );

        when(weatherService.getForecast(eq(16.0592), eq(108.2464), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(forecastResult);

        mockMvc.perform(get("/api/v1/public/weather/forecast")
                        .param("latitude", "16.0592")
                        .param("longitude", "108.2464")
                        .param("startDate", "2026-10-01")
                        .param("endDate", "2026-10-01")
                        .param("includeHourly", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.providerName").value("open-meteo"))
                .andExpect(jsonPath("$.data.dailySummaries").isArray())
                .andExpect(jsonPath("$.data.dailySummaries[0].dominantCondition").value("CLEAR_SKY"))
                .andExpect(jsonPath("$.data.dailySummaries[0].hourlyForecasts").isArray())
                .andExpect(jsonPath("$.data.dailySummaries[0].hourlyForecasts[0].temperature").value(28.0));
    }

    @Test
    @DisplayName("GET /api/v1/public/weather/current with invalid longitude type (e.g. 'a') returns 400 with formatted error response")
    void getCurrentWeather_invalidLongitudeType_returns400FormattedError() throws Exception {
        when(translator.toLocale(anyString())).thenReturn("Tham số không đúng định dạng hoặc kiểu dữ liệu yêu cầu");

        mockMvc.perform(get("/api/v1/public/weather/current")
                        .param("latitude", "16.0592")
                        .param("longitude", "a")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_008"))
                .andExpect(jsonPath("$.message").value("Tham số không đúng định dạng hoặc kiểu dữ liệu yêu cầu"))
                .andExpect(jsonPath("$.errors.longitude").exists())
                .andExpect(jsonPath("$.errors.longitude").value(org.hamcrest.Matchers.containsString("longitude")))
                .andExpect(jsonPath("$.errors.longitude").value(org.hamcrest.Matchers.containsString("double")));
    }

    @Test
    @DisplayName("GET /api/v1/public/weather/current with missing latitude returns 400 with formatted error response")
    void getCurrentWeather_missingLatitude_returns400FormattedError() throws Exception {
        when(translator.toLocale(anyString())).thenReturn("Thiếu tham số bắt buộc trong yêu cầu");

        mockMvc.perform(get("/api/v1/public/weather/current")
                        .param("longitude", "108.2464")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_009"))
                .andExpect(jsonPath("$.message").value("Thiếu tham số bắt buộc trong yêu cầu"))
                .andExpect(jsonPath("$.errors.latitude").exists());
    }

    @Test
    @DisplayName("GET /api/v1/public/weather/current with out-of-range latitude (> 90.0) returns 400 validation error")
    void getCurrentWeather_outOfRangeLatitude_returns400ValidationError() throws Exception {
        when(translator.toLocale(anyString())).thenReturn("Tham số hoặc trạng thái yêu cầu không hợp lệ");

        mockMvc.perform(get("/api/v1/public/weather/current")
                        .param("latitude", "120.0")
                        .param("longitude", "108.2464")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_011"))
                .andExpect(jsonPath("$.errors.error").exists())
                .andExpect(jsonPath("$.errors.error").value(org.hamcrest.Matchers.containsString("90.0")));
    }
}
