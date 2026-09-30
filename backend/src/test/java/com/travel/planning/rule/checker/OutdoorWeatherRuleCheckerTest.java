package com.travel.planning.rule.checker;

import com.travel.place.enums.PlaceType;
import com.travel.planning.rule.enums.IssueCategory;
import com.travel.planning.rule.enums.IssueSeverity;
import com.travel.planning.rule.model.DetectedIssue;
import com.travel.planning.rule.model.ScheduleItemContext;
import com.travel.weather.enums.WeatherCondition;
import com.travel.weather.enums.WeatherRiskLevel;
import com.travel.weather.model.HourlyWeather;
import com.travel.weather.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutdoorWeatherRuleCheckerTest {

    @Mock
    private WeatherService weatherService;

    private OutdoorWeatherRuleChecker checker;

    @BeforeEach
    void setUp() {
        checker = new OutdoorWeatherRuleChecker(weatherService);
    }

    @Test
    @DisplayName("evaluate should ignore INDOOR places even during stormy weather")
    void evaluate_indoorPlaceDuringStorm_returnsNoIssues() {
        // Arrange
        LocalDateTime time = LocalDateTime.of(2026, 10, 1, 14, 0);
        ScheduleItemContext indoorItem = new ScheduleItemContext(
                UUID.randomUUID(), "Bảo tàng Mỹ thuật", 16.0, 108.0, PlaceType.INDOOR,
                null, null, time, 60, 0.0, 0
        );

        // Act
        List<DetectedIssue> issues = checker.evaluate(List.of(indoorItem));

        // Assert
        assertThat(issues).isEmpty();
    }

    @Test
    @DisplayName("evaluate on OUTDOOR place during THUNDERSTORM should return CRITICAL issue")
    void evaluate_outdoorPlaceDuringThunderstorm_returnsCritical() {
        // Arrange
        LocalDateTime time = LocalDateTime.of(2026, 10, 1, 14, 0);
        ScheduleItemContext outdoorItem = new ScheduleItemContext(
                UUID.randomUUID(), "Đỉnh Bàn Cờ - Sơn Trà", 16.1, 108.2, PlaceType.OUTDOOR,
                null, null, time, 90, 5.0, 20
        );

        HourlyWeather stormWeather = new HourlyWeather(
                time, 28.0, 95, 20.0, 95, WeatherCondition.THUNDERSTORM, WeatherRiskLevel.HAZARDOUS
        );
        when(weatherService.getForecastAt(anyDouble(), anyDouble(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(stormWeather));

        // Act
        List<DetectedIssue> issues = checker.evaluate(List.of(outdoorItem));

        // Assert
        assertThat(issues).hasSize(1);
        DetectedIssue issue = issues.getFirst();
        assertThat(issue.severity()).isEqualTo(IssueSeverity.CRITICAL);
        assertThat(issue.category()).isEqualTo(IssueCategory.HAZARDOUS_WEATHER);
        assertThat(issue.reason()).contains("DÔNG BÃO");
    }

    @Test
    @DisplayName("evaluate on OUTDOOR place with LIGHT_RAIN should return IMPORTANT issue")
    void evaluate_outdoorPlaceWithLightRain_returnsImportant() {
        // Arrange
        LocalDateTime time = LocalDateTime.of(2026, 10, 1, 9, 0);
        ScheduleItemContext outdoorItem = new ScheduleItemContext(
                UUID.randomUUID(), "Bãi biển Mỹ Khê", 16.06, 108.24, PlaceType.OUTDOOR,
                null, null, time, 60, 0.0, 0
        );

        HourlyWeather rainyWeather = new HourlyWeather(
                time, 27.0, 70, 1.2, 51, WeatherCondition.LIGHT_RAIN, WeatherRiskLevel.CAUTION
        );
        when(weatherService.getForecastAt(anyDouble(), anyDouble(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(rainyWeather));

        // Act
        List<DetectedIssue> issues = checker.evaluate(List.of(outdoorItem));

        // Assert
        assertThat(issues).hasSize(1);
        DetectedIssue issue = issues.getFirst();
        assertThat(issue.severity()).isEqualTo(IssueSeverity.IMPORTANT);
        assertThat(issue.category()).isEqualTo(IssueCategory.ADVERSE_WEATHER);
        assertThat(issue.suggestionHint()).contains("ô/dù");
    }

    @Test
    @DisplayName("evaluate on OUTDOOR place with extreme heat >= 38°C should return CRITICAL issue")
    void evaluate_outdoorPlaceExtremeHeat_returnsCritical() {
        // Arrange
        LocalDateTime time = LocalDateTime.of(2026, 10, 1, 13, 0);
        ScheduleItemContext outdoorItem = new ScheduleItemContext(
                UUID.randomUUID(), "Cù Lao Chàm", 15.9, 108.5, PlaceType.OUTDOOR,
                null, null, time, 120, 0.0, 0
        );

        HourlyWeather extremeHeat = new HourlyWeather(
                time, 39.5, 10, 0.0, 0, WeatherCondition.CLEAR_SKY, WeatherRiskLevel.HAZARDOUS
        );
        when(weatherService.getForecastAt(anyDouble(), anyDouble(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(extremeHeat));

        // Act
        List<DetectedIssue> issues = checker.evaluate(List.of(outdoorItem));

        // Assert
        assertThat(issues).hasSize(1);
        DetectedIssue issue = issues.getFirst();
        assertThat(issue.severity()).isEqualTo(IssueSeverity.CRITICAL);
        assertThat(issue.reason()).contains("39.5°C");
    }
}
