package com.travel.planning.rule.checker;

import static org.assertj.core.api.Assertions.assertThat;

import com.travel.place.enums.PlaceType;
import com.travel.planning.rule.enums.IssueCategory;
import com.travel.planning.rule.enums.IssueSeverity;
import com.travel.planning.rule.model.DetectedIssue;
import com.travel.planning.rule.model.ScheduleItemContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TransitFeasibilityRuleCheckerTest {

    private TransitFeasibilityRuleChecker checker;

    @BeforeEach
    void setUp() {
        checker = new TransitFeasibilityRuleChecker();
    }

    @Test
    @DisplayName("evaluate with sufficient transit buffer should return no transit issues")
    void evaluate_sufficientBuffer_returnsNoIssues() {
        // Arrange: Place 1 departs at 09:00, Place 2 arrives at 09:30 (30 mins gap, needs 15 mins)
        LocalDate date = LocalDate.of(2026, 10, 1);
        ScheduleItemContext p1 = new ScheduleItemContext(
            UUID.randomUUID(),
            "Điểm 1",
            16.0,
            108.0,
            PlaceType.INDOOR,
            null,
            null,
            date.atTime(8, 0),
            60,
            0.0,
            0
        );
        ScheduleItemContext p2 = new ScheduleItemContext(
            UUID.randomUUID(),
            "Điểm 2",
            16.05,
            108.05,
            PlaceType.INDOOR,
            null,
            null,
            date.atTime(9, 30),
            60,
            4.0,
            15
        );

        // Act
        List<DetectedIssue> issues = checker.evaluate(List.of(p1, p2));

        // Assert
        assertThat(issues).isEmpty();
    }

    @Test
    @DisplayName("evaluate with transit deficit should return CRITICAL issue")
    void evaluate_transitDeficit_returnsCritical() {
        // Arrange: Place 1 departs at 09:00, Place 2 arrives at 09:10 (10 mins gap, but needs 30 mins)
        LocalDate date = LocalDate.of(2026, 10, 1);
        ScheduleItemContext p1 = new ScheduleItemContext(
            UUID.randomUUID(),
            "Điểm 1",
            16.0,
            108.0,
            PlaceType.INDOOR,
            null,
            null,
            date.atTime(8, 0),
            60,
            0.0,
            0
        );
        ScheduleItemContext p2 = new ScheduleItemContext(
            UUID.randomUUID(),
            "Điểm 2",
            16.15,
            108.2,
            PlaceType.INDOOR,
            null,
            null,
            date.atTime(9, 10),
            60,
            15.0,
            30
        );

        // Act
        List<DetectedIssue> issues = checker.evaluate(List.of(p1, p2));

        // Assert
        assertThat(issues).hasSize(1);
        DetectedIssue issue = issues.getFirst();
        assertThat(issue.severity()).isEqualTo(IssueSeverity.CRITICAL);
        assertThat(issue.category()).isEqualTo(IssueCategory.TIGHT_TRANSIT_TIME);
        assertThat(issue.reason()).contains("thiếu 20 phút");
    }

    @Test
    @DisplayName("evaluate with schedule exceeding 6 places per day should return INFO issue")
    void evaluate_moreThanSixPlaces_returnsInfoIssue() {
        // Arrange: 7 places in one day
        LocalDate date = LocalDate.of(2026, 10, 1);
        List<ScheduleItemContext> items = new ArrayList<>();
        LocalDateTime time = date.atTime(7, 0);

        for (int i = 1; i <= 7; i++) {
            items.add(
                new ScheduleItemContext(
                    UUID.randomUUID(),
                    "Điểm " + i,
                    16.0,
                    108.0,
                    PlaceType.INDOOR,
                    null,
                    null,
                    time,
                    60,
                    2.0,
                    15
                )
            );
            time = time.plusMinutes(75); // 60 mins visit + 15 mins transit
        }

        // Act
        List<DetectedIssue> issues = checker.evaluate(items);

        // Assert
        assertThat(issues).anyMatch(
            i -> i.category() == IssueCategory.OVERLOADED_SCHEDULE && i.severity() == IssueSeverity.INFO
        );
    }
}
