package com.travel.weather.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherRiskLevelTest {

    @Test
    @DisplayName("evaluate should identify HAZARDOUS weather conditions")
    void evaluate_hazardousConditions() {
        assertThat(WeatherRiskLevel.evaluate(WeatherCondition.THUNDERSTORM, 28.0, 90))
                .isEqualTo(WeatherRiskLevel.HAZARDOUS);

        assertThat(WeatherRiskLevel.evaluate(WeatherCondition.HEAVY_RAIN, 25.0, 85))
                .isEqualTo(WeatherRiskLevel.HAZARDOUS);

        assertThat(WeatherRiskLevel.evaluate(WeatherCondition.CLEAR_SKY, 41.0, 10))
                .isEqualTo(WeatherRiskLevel.HAZARDOUS);
    }

    @Test
    @DisplayName("evaluate should identify CAUTION conditions")
    void evaluate_cautionConditions() {
        assertThat(WeatherRiskLevel.evaluate(WeatherCondition.LIGHT_RAIN, 27.0, 40))
                .isEqualTo(WeatherRiskLevel.CAUTION);

        assertThat(WeatherRiskLevel.evaluate(WeatherCondition.RAINY, 26.0, 60))
                .isEqualTo(WeatherRiskLevel.CAUTION);

        assertThat(WeatherRiskLevel.evaluate(WeatherCondition.CLEAR_SKY, 36.5, 10))
                .isEqualTo(WeatherRiskLevel.CAUTION);
    }

    @Test
    @DisplayName("evaluate should identify SAFE conditions")
    void evaluate_safeConditions() {
        assertThat(WeatherRiskLevel.evaluate(WeatherCondition.CLEAR_SKY, 28.0, 10))
                .isEqualTo(WeatherRiskLevel.SAFE);

        assertThat(WeatherRiskLevel.evaluate(WeatherCondition.PARTLY_CLOUDY, 26.0, 20))
                .isEqualTo(WeatherRiskLevel.SAFE);
    }
}
