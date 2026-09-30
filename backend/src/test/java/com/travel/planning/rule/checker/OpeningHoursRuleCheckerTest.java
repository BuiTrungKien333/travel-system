package com.travel.planning.rule.checker;

import com.travel.place.enums.PlaceType;
import com.travel.planning.rule.enums.IssueCategory;
import com.travel.planning.rule.enums.IssueSeverity;
import com.travel.planning.rule.model.DetectedIssue;
import com.travel.planning.rule.model.ScheduleItemContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OpeningHoursRuleCheckerTest {

    private OpeningHoursRuleChecker checker;

    @BeforeEach
    void setUp() {
        checker = new OpeningHoursRuleChecker();
    }

    @Test
    @DisplayName("evaluate with valid schedule within opening hours should return no issues")
    void evaluate_withinOpeningHours_returnsNoIssues() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 10, 1);
        ScheduleItemContext item = new ScheduleItemContext(
                UUID.randomUUID(), "Bảo tàng", 16.0, 108.0, PlaceType.INDOOR,
                LocalTime.of(8, 0), LocalTime.of(17, 0),
                date.atTime(9, 0), 60, 0.0, 0
        );

        // Act
        List<DetectedIssue> issues = checker.evaluate(List.of(item));

        // Assert
        assertThat(issues).isEmpty();
    }

    @Test
    @DisplayName("evaluate with arrival after closing time should return CRITICAL issue")
    void evaluate_arrivalAfterClosing_returnsCriticalIssue() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 10, 1);
        ScheduleItemContext item = new ScheduleItemContext(
                UUID.randomUUID(), "Bảo tàng Chăm", 16.0, 108.0, PlaceType.INDOOR,
                LocalTime.of(8, 0), LocalTime.of(17, 0),
                date.atTime(17, 30), 60, 2.0, 15
        );

        // Act
        List<DetectedIssue> issues = checker.evaluate(List.of(item));

        // Assert
        assertThat(issues).hasSize(1);
        DetectedIssue issue = issues.getFirst();
        assertThat(issue.severity()).isEqualTo(IssueSeverity.CRITICAL);
        assertThat(issue.category()).isEqualTo(IssueCategory.CLOSING_TIME_CONFLICT);
        assertThat(issue.reason()).contains("đóng cửa lúc 17:00");
    }

    @Test
    @DisplayName("evaluate with arrival before opening time should return issue with wait duration")
    void evaluate_arrivalBeforeOpening_returnsIssue() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 10, 1);
        ScheduleItemContext item = new ScheduleItemContext(
                UUID.randomUUID(), "Chùa Linh Ứng", 16.0, 108.0, PlaceType.HYBRID,
                LocalTime.of(8, 0), LocalTime.of(18, 0),
                date.atTime(7, 15), 60, 0.0, 0
        );

        // Act
        List<DetectedIssue> issues = checker.evaluate(List.of(item));

        // Assert
        assertThat(issues).hasSize(1);
        DetectedIssue issue = issues.getFirst();
        assertThat(issue.category()).isEqualTo(IssueCategory.CLOSING_TIME_CONFLICT);
        assertThat(issue.reason()).contains("chờ 45 phút");
    }

    @Test
    @DisplayName("evaluate with stay exceeding closing time should return IMPORTANT issue")
    void evaluate_stayExceedsClosing_returnsImportantIssue() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 10, 1);
        ScheduleItemContext item = new ScheduleItemContext(
                UUID.randomUUID(), "Khu vui chơi", 16.0, 108.0, PlaceType.INDOOR,
                LocalTime.of(8, 0), LocalTime.of(17, 0),
                date.atTime(16, 30), 60, 0.0, 0 // Departure at 17:30, 30 mins truncated
        );

        // Act
        List<DetectedIssue> issues = checker.evaluate(List.of(item));

        // Assert
        assertThat(issues).hasSize(1);
        DetectedIssue issue = issues.getFirst();
        assertThat(issue.severity()).isEqualTo(IssueSeverity.IMPORTANT);
        assertThat(issue.reason()).contains("bị cắt ngắn 30 phút");
    }

    @Test
    @DisplayName("evaluate with overnight opening hours arriving during daytime should return CRITICAL issue")
    void evaluate_overnightPlaceDuringDaytime_returnsCritical() {
        // Arrange (Mở từ 18:00 tối đến 02:00 sáng hôm sau)
        LocalDate date = LocalDate.of(2026, 10, 1);
        ScheduleItemContext item = new ScheduleItemContext(
                UUID.randomUUID(), "Sky Lounge Bar", 16.0, 108.0, PlaceType.INDOOR,
                LocalTime.of(18, 0), LocalTime.of(2, 0),
                date.atTime(14, 0), 60, 0.0, 0
        );

        // Act
        List<DetectedIssue> issues = checker.evaluate(List.of(item));

        // Assert
        assertThat(issues).hasSize(1);
        assertThat(issues.getFirst().severity()).isEqualTo(IssueSeverity.CRITICAL);
    }

    @Test
    @DisplayName("evaluate with UNVERIFIED opening hours should return INFO issue indicating unverified data")
    void evaluate_unverifiedHours_returnsInfoIssue() {
        LocalDate date = LocalDate.of(2026, 10, 1);
        ScheduleItemContext item = new ScheduleItemContext(
                UUID.randomUUID(), "Quán cafe địa phương", 16.0, 108.0, PlaceType.INDOOR,
                date, date, false, date.atTime(10, 0), 60, false, 0,
                com.travel.place.enums.PlaceOpeningStatus.UNVERIFIED,
                null, null, null, 0.0, 0, com.travel.planning.enums.VisitStatus.NOT_VISITED
        );

        List<DetectedIssue> issues = checker.evaluate(List.of(item));

        assertThat(issues).hasSize(1);
        assertThat(issues.getFirst().severity()).isEqualTo(IssueSeverity.INFO);
        assertThat(issues.getFirst().category()).isEqualTo(IssueCategory.UNVERIFIED_HOURS);
    }

    @Test
    @DisplayName("evaluate with Saturday overnight shift arriving Sunday 01:00 should be valid")
    void evaluate_weeklyOvernightShift_arrivingNextDayDawn_valid() {
        // Thứ 7: 18:00 - 02:00 sáng CN
        java.util.Map<java.time.DayOfWeek, com.travel.place.model.DaySchedule> map = new java.util.EnumMap<>(java.time.DayOfWeek.class);
        map.put(java.time.DayOfWeek.SATURDAY, com.travel.place.model.DaySchedule.open(java.time.DayOfWeek.SATURDAY, LocalTime.of(18, 0), LocalTime.of(2, 0)));
        map.put(java.time.DayOfWeek.SUNDAY, com.travel.place.model.DaySchedule.open(java.time.DayOfWeek.SUNDAY, LocalTime.of(10, 0), LocalTime.of(22, 0)));
        com.travel.place.model.WeeklyOpeningHours weekly = new com.travel.place.model.WeeklyOpeningHours(map);

        // Ngày 2026-10-04 là Chủ nhật, đến lúc 01:00 sáng (thuộc ca thứ Bảy 18:00 - 02:00)
        LocalDate sunday = LocalDate.of(2026, 10, 4);
        ScheduleItemContext item = new ScheduleItemContext(
                UUID.randomUUID(), "Night Club", 16.0, 108.0, PlaceType.INDOOR,
                sunday, sunday, false, sunday.atTime(1, 0), 45, false, 0,
                com.travel.place.enums.PlaceOpeningStatus.OPEN,
                LocalTime.of(18, 0), LocalTime.of(2, 0), weekly,
                0.0, 0, com.travel.planning.enums.VisitStatus.NOT_VISITED
        );

        List<DetectedIssue> issues = checker.evaluate(List.of(item));

        assertThat(issues).isEmpty();
    }
}
