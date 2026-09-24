package com.travel.weather.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Điều kiện thời tiết chuẩn hóa")
public enum WeatherCondition {
    @Schema(description = "Trời quang đãng, nắng đẹp")
    CLEAR_SKY,

    @Schema(description = "Có mây, nắng nhẹ")
    PARTLY_CLOUDY,

    @Schema(description = "Nhiều mây, âm u")
    CLOUDY,

    @Schema(description = "Sương mù")
    FOGGY,

    @Schema(description = "Mưa phùn, mưa nhỏ")
    LIGHT_RAIN,

    @Schema(description = "Mưa rào, mưa vừa")
    RAINY,

    @Schema(description = "Mưa to, mưa lớn diện rộng")
    HEAVY_RAIN,

    @Schema(description = "Dông bão, sấm chớp")
    THUNDERSTORM,

    @Schema(description = "Không xác định")
    UNKNOWN;

    /**
     * Chuyển đổi từ mã WMO Weather Code chuẩn quốc tế
     */
    public static WeatherCondition fromWmoCode(int code) {
        return switch (code) {
            case 0 -> CLEAR_SKY;
            case 1, 2 -> PARTLY_CLOUDY;
            case 3 -> CLOUDY;
            case 45, 48 -> FOGGY;
            case 51, 53, 55, 56, 57 -> LIGHT_RAIN;
            case 61, 63, 80, 81 -> RAINY;
            case 65, 82 -> HEAVY_RAIN;
            case 95, 96, 99 -> THUNDERSTORM;
            default -> UNKNOWN;
        };
    }

    public boolean isRainyOrStormy() {
        return this == LIGHT_RAIN || this == RAINY || this == HEAVY_RAIN || this == THUNDERSTORM;
    }

    public boolean isSevere() {
        return this == HEAVY_RAIN || this == THUNDERSTORM;
    }

    public String getDescription() {
        return switch (this) {
            case CLEAR_SKY -> "Trời quang đãng, nắng đẹp";
            case PARTLY_CLOUDY -> "Có mây, nắng nhẹ";
            case CLOUDY -> "Nhiều mây, âm u";
            case FOGGY -> "Sương mù";
            case LIGHT_RAIN -> "Mưa phùn, mưa nhỏ";
            case RAINY -> "Mưa rào, mưa vừa";
            case HEAVY_RAIN -> "Mưa to, mưa lớn diện rộng";
            case THUNDERSTORM -> "Dông bão, sấm chớp nguy hiểm";
            case UNKNOWN -> "Thời tiết không xác định";
        };
    }
}
