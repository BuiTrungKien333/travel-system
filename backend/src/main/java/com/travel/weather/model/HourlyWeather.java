package com.travel.weather.model;

import com.travel.weather.enums.WeatherCondition;
import com.travel.weather.enums.WeatherRiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Dự báo thời tiết chi tiết theo giờ")
public record HourlyWeather(
        @Schema(description = "Thời điểm dự báo")
        LocalDateTime time,

        @Schema(description = "Nhiệt độ (°C)", example = "29.5")
        double temperature,

        @Schema(description = "Xác suất có mưa (%)", example = "65")
        int precipitationProbability,

        @Schema(description = "Lượng mưa dự kiến (mm)", example = "2.4")
        double rainMm,

        @Schema(description = "Mã thời tiết WMO", example = "61")
        int weatherCode,

        @Schema(description = "Điều kiện thời tiết chuẩn hóa")
        WeatherCondition condition,

        @Schema(description = "Mức độ rủi ro du lịch")
        WeatherRiskLevel riskLevel
) {
    public HourlyWeather {
        if (riskLevel == null && condition != null) {
            riskLevel = WeatherRiskLevel.evaluate(condition, temperature, precipitationProbability);
        }
    }
}
