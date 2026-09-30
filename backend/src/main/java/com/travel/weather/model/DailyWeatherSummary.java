package com.travel.weather.model;

import com.travel.weather.enums.WeatherCondition;
import com.travel.weather.enums.WeatherRiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Tổng kết dự báo thời tiết theo ngày")
public record DailyWeatherSummary(
        @Schema(description = "Ngày dự báo")
        LocalDate date,

        @Schema(description = "Nhiệt độ thấp nhất trong ngày (°C)", example = "24.0")
        double minTemperature,

        @Schema(description = "Nhiệt độ cao nhất trong ngày (°C)", example = "33.5")
        double maxTemperature,

        @Schema(description = "Xác suất có mưa cao nhất trong ngày (%)", example = "75")
        int maxPrecipitationProbability,

        @Schema(description = "Tổng lượng mưa trong ngày (mm)", example = "12.0")
        double totalRainMm,

        @Schema(description = "Điều kiện thời tiết chủ đạo trong ngày")
        WeatherCondition dominantCondition,

        @Schema(description = "Mức độ rủi ro cao nhất trong ngày")
        WeatherRiskLevel highestRiskLevel,

        @Schema(description = "Chi tiết dự báo theo từng giờ của ngày")
        List<HourlyWeather> hourlyForecasts
) {
    public DailyWeatherSummary {
        hourlyForecasts = hourlyForecasts != null ? List.copyOf(hourlyForecasts) : List.of();
    }
}
