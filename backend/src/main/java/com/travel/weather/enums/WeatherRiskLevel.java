package com.travel.weather.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Mức độ rủi ro thời tiết đối với các hoạt động du lịch")
public enum WeatherRiskLevel {
    @Schema(description = "An toàn, thời tiết lý tưởng")
    SAFE,

    @Schema(description = "Cần lưu ý: nắng gắt hoặc mưa nhỏ rải rác")
    CAUTION,

    @Schema(description = "Nguy hiểm: mưa to, dông sét, bão, không thích hợp hoạt động ngoài trời")
    HAZARDOUS;

    /**
     * Đánh giá mức độ rủi ro dựa trên điều kiện thời tiết, nhiệt độ và tỉ lệ mưa
     */
    public static WeatherRiskLevel evaluate(WeatherCondition condition, double temperature, int rainProbability) {
        if (condition.isSevere() || rainProbability >= 80 || temperature >= 40.0) {
            return HAZARDOUS;
        }
        if (condition == WeatherCondition.RAINY || condition == WeatherCondition.LIGHT_RAIN
                || rainProbability >= 50 || temperature >= 35.0 || temperature <= 10.0) {
            return CAUTION;
        }
        return SAFE;
    }
}
