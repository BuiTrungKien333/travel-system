package com.travel.weather.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherConditionTest {

    @Test
    @DisplayName("fromWmoCode should correctly map standard WMO codes")
    void fromWmoCode_mapsCorrectly() {
        assertThat(WeatherCondition.fromWmoCode(0)).isEqualTo(WeatherCondition.CLEAR_SKY);
        assertThat(WeatherCondition.fromWmoCode(1)).isEqualTo(WeatherCondition.PARTLY_CLOUDY);
        assertThat(WeatherCondition.fromWmoCode(3)).isEqualTo(WeatherCondition.CLOUDY);
        assertThat(WeatherCondition.fromWmoCode(45)).isEqualTo(WeatherCondition.FOGGY);
        assertThat(WeatherCondition.fromWmoCode(51)).isEqualTo(WeatherCondition.LIGHT_RAIN);
        assertThat(WeatherCondition.fromWmoCode(61)).isEqualTo(WeatherCondition.RAINY);
        assertThat(WeatherCondition.fromWmoCode(65)).isEqualTo(WeatherCondition.HEAVY_RAIN);
        assertThat(WeatherCondition.fromWmoCode(95)).isEqualTo(WeatherCondition.THUNDERSTORM);
        assertThat(WeatherCondition.fromWmoCode(999)).isEqualTo(WeatherCondition.UNKNOWN);
    }

    @Test
    @DisplayName("isRainyOrStormy and isSevere should return correct flags")
    void rainyAndSevereFlags() {
        assertThat(WeatherCondition.CLEAR_SKY.isRainyOrStormy()).isFalse();
        assertThat(WeatherCondition.CLEAR_SKY.isSevere()).isFalse();

        assertThat(WeatherCondition.LIGHT_RAIN.isRainyOrStormy()).isTrue();
        assertThat(WeatherCondition.LIGHT_RAIN.isSevere()).isFalse();

        assertThat(WeatherCondition.RAINY.isRainyOrStormy()).isTrue();
        assertThat(WeatherCondition.RAINY.isSevere()).isFalse();

        assertThat(WeatherCondition.HEAVY_RAIN.isRainyOrStormy()).isTrue();
        assertThat(WeatherCondition.HEAVY_RAIN.isSevere()).isTrue();

        assertThat(WeatherCondition.THUNDERSTORM.isRainyOrStormy()).isTrue();
        assertThat(WeatherCondition.THUNDERSTORM.isSevere()).isTrue();
    }
}
