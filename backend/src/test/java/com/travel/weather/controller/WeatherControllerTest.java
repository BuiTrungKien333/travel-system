package com.travel.weather.controller;

import com.travel.weather.enums.WeatherCondition;
import com.travel.weather.enums.WeatherRiskLevel;
import com.travel.weather.model.CurrentWeather;
import com.travel.weather.model.DailyWeatherSummary;
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

    @InjectMocks
    private WeatherController weatherController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(weatherController).build();
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
    @DisplayName("GET /api/v1/public/weather/forecast should return 200 and forecast summaries")
    void getWeatherForecast_success_returns200() throws Exception {
        LocalDate today = LocalDate.of(2026, 10, 1);
        DailyWeatherSummary summary = new DailyWeatherSummary(
                today,
                24.0,
                32.0,
                20,
                0.0,
                WeatherCondition.CLEAR_SKY,
                WeatherRiskLevel.SAFE,
                List.of()
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
                .andExpect(jsonPath("$.data.dailySummaries[0].dominantCondition").value("CLEAR_SKY"));
    }
}
