package com.travel.planning.rule.checker;

import com.travel.place.enums.PlaceType;
import com.travel.planning.dto.response.DayScheduleResponse;
import com.travel.planning.rule.enums.IssueCategory;
import com.travel.planning.rule.enums.IssueSeverity;
import com.travel.planning.rule.model.DetectedIssue;
import com.travel.planning.rule.model.ItineraryEvaluationResult;
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

class TimelineCollisionRuleCheckerTest {

    private TimelineCollisionRuleChecker checker;

    @BeforeEach
    void setUp() {
        checker = new TimelineCollisionRuleChecker();
    }

    @Test
    @DisplayName("evaluateCrossDayCollisions with no collision and sufficient rest returns empty list")
    void evaluate_noCollision_returnsEmpty() {
        LocalDate day1 = LocalDate.of(2026, 10, 1);
        LocalDate day2 = LocalDate.of(2026, 10, 2);

        ScheduleItemContext itemDay1 = createItem(day1, day1.atTime(18, 0), 120); // Ends at 20:00
        ScheduleItemContext itemDay2 = createItem(day2, day2.atTime(8, 0), 60);   // Starts at 08:00 next day (12h rest)

        DayScheduleResponse day1Resp = new DayScheduleResponse(day1, "ALL_CLEAR", 10.0, 30, List.of(itemDay1), ItineraryEvaluationResult.of(List.of()));
        DayScheduleResponse day2Resp = new DayScheduleResponse(day2, "ALL_CLEAR", 12.0, 35, List.of(itemDay2), ItineraryEvaluationResult.of(List.of()));

        List<DetectedIssue> issues = checker.evaluateCrossDayCollisions(List.of(day1Resp, day2Resp));

        assertThat(issues).isEmpty();
    }

    @Test
    @DisplayName("evaluateCrossDayCollisions with day 1 ending after day 2 starts returns CRITICAL collision")
    void evaluate_directCollision_returnsCritical() {
        LocalDate day1 = LocalDate.of(2026, 10, 1);
        LocalDate day2 = LocalDate.of(2026, 10, 2);

        // Ngày 1 bị kéo dài sang ngày hôm sau và kết thúc lúc 09:30 ngày 2/10
        ScheduleItemContext itemDay1 = createItem(day1, day2.atTime(8, 30), 60); // Ends at 09:30 2/10
        // Ngày 2 bắt đầu lúc 08:00 ngày 2/10 -> Bị đè lên 90 phút!
        ScheduleItemContext itemDay2 = createItem(day2, day2.atTime(8, 0), 60);

        DayScheduleResponse day1Resp = new DayScheduleResponse(day1, "CONFLICTS_DETECTED", 15.0, 45, List.of(itemDay1), ItineraryEvaluationResult.of(List.of()));
        DayScheduleResponse day2Resp = new DayScheduleResponse(day2, "ALL_CLEAR", 10.0, 25, List.of(itemDay2), ItineraryEvaluationResult.of(List.of()));

        List<DetectedIssue> issues = checker.evaluateCrossDayCollisions(List.of(day1Resp, day2Resp));

        assertThat(issues).hasSize(1);
        DetectedIssue issue = issues.getFirst();
        assertThat(issue.severity()).isEqualTo(IssueSeverity.CRITICAL);
        assertThat(issue.category()).isEqualTo(IssueCategory.SCHEDULE_COLLISION);
        assertThat(issue.reason()).contains("chồng lấn");
    }

    @Test
    @DisplayName("evaluateCrossDayCollisions with rest gap under 6 hours returns IMPORTANT rest issue")
    void evaluate_shortRestGap_returnsImportant() {
        LocalDate day1 = LocalDate.of(2026, 10, 1);
        LocalDate day2 = LocalDate.of(2026, 10, 2);

        // Ngày 1 kết thúc lúc 03:00 sáng ngày 2/10
        ScheduleItemContext itemDay1 = createItem(day1, day2.atTime(2, 0), 60); // Ends at 03:00
        // Ngày 2 bắt đầu lúc 07:00 sáng ngày 2/10 -> Chỉ được ngủ 4 tiếng
        ScheduleItemContext itemDay2 = createItem(day2, day2.atTime(7, 0), 60);

        DayScheduleResponse day1Resp = new DayScheduleResponse(day1, "ALL_CLEAR", 10.0, 30, List.of(itemDay1), ItineraryEvaluationResult.of(List.of()));
        DayScheduleResponse day2Resp = new DayScheduleResponse(day2, "ALL_CLEAR", 10.0, 30, List.of(itemDay2), ItineraryEvaluationResult.of(List.of()));

        List<DetectedIssue> issues = checker.evaluateCrossDayCollisions(List.of(day1Resp, day2Resp));

        assertThat(issues).hasSize(1);
        DetectedIssue issue = issues.getFirst();
        assertThat(issue.severity()).isEqualTo(IssueSeverity.IMPORTANT);
        assertThat(issue.category()).isEqualTo(IssueCategory.INSUFFICIENT_REST_TIME);
        assertThat(issue.reason()).contains("4 giờ");
    }

    private ScheduleItemContext createItem(LocalDate plannedDate, LocalDateTime arrival, int duration) {
        return new ScheduleItemContext(
                UUID.randomUUID(), "Điểm tham quan", 16.0, 108.0, PlaceType.INDOOR,
                plannedDate, arrival.toLocalDate(), arrival.toLocalDate().isAfter(plannedDate),
                arrival, duration, false, 0,
                com.travel.place.enums.PlaceOpeningStatus.OPEN,
                LocalTime.of(8, 0), LocalTime.of(23, 0), null,
                5.0, 15, com.travel.planning.enums.VisitStatus.NOT_VISITED
        );
    }
}
