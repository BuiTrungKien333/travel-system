package com.travel.weather.model;

import com.travel.weather.enums.WeatherCondition;
import com.travel.weather.enums.WeatherRiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Thông tin thời tiết thực tế tại vị trí và thời điểm hiện tại")
public record CurrentWeather(
    @Schema(description = "Vĩ độ GPS", example = "16.0592") double latitude,

    @Schema(description = "Kinh độ GPS", example = "108.2464") double longitude,

    @Schema(description = "Thời điểm cập nhật dữ liệu") LocalDateTime time,

    @Schema(description = "Nhiệt độ hiện tại (°C)", example = "29.5") double temperature,

    @Schema(description = "Nhiệt độ cảm nhận thực tế (°C)", example = "33.8") double apparentTemperature,

    @Schema(description = "Độ ẩm không khí (%)", example = "78") int humidity,

    @Schema(description = "Lượng mưa hiện tại (mm)", example = "0.0") double rainMm,

    @Schema(description = "Tốc độ gió (km/h)", example = "12.5") double windSpeedKmH,

    @Schema(description = "Thời điểm là ban ngày hay ban đêm", example = "true") boolean isDay,

    @Schema(description = "Mã thời tiết WMO", example = "1") int weatherCode,

    @Schema(description = "Điều kiện thời tiết chuẩn hóa") WeatherCondition condition,

    @Schema(description = "Mức độ rủi ro du lịch") WeatherRiskLevel riskLevel,

    @Schema(description = "Mô tả ngắn gọn thời tiết", example = "Nắng nhẹ, nhiệt độ 29.5°C (cảm giác như 33.8°C)")
    String summary
) {
    public CurrentWeather {
        if (riskLevel == null && condition != null) {
            riskLevel = WeatherRiskLevel.evaluate(condition, temperature, rainMm > 0 ? 80 : 10);
        }
        if (summary == null || summary.isBlank()) {
            String desc = condition != null ? condition.getDescription() : "Thời tiết hiện tại";
            summary = String.format("%s, %.1f°C (cảm giác như %.1f°C)", desc, temperature, apparentTemperature);
        }
    }
}
