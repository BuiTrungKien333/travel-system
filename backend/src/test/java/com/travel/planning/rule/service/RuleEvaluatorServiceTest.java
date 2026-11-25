package com.travel.planning.rule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.travel.planning.rule.checker.ScheduleRuleChecker;
import com.travel.planning.rule.enums.IssueCategory;
import com.travel.planning.rule.enums.IssueSeverity;
import com.travel.planning.rule.model.DetectedIssue;
import com.travel.planning.rule.model.ItineraryEvaluationResult;
import com.travel.planning.rule.model.ScheduleItemContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RuleEvaluatorServiceTest {

    @Mock
    private ScheduleRuleChecker checker1;

    @Mock
    private ScheduleRuleChecker checker2;

    private RuleEvaluatorService service;

    @BeforeEach
    void setUp() {
        service = new RuleEvaluatorService(List.of(checker1, checker2));
    }

    @Test
    @DisplayName("evaluateItinerary with empty list should return all clear and shouldTriggerAi false")
    void evaluateItinerary_emptyList_returnsAllClear() {
        ItineraryEvaluationResult result = service.evaluateItinerary(List.of());

        assertThat(result.hasIssues()).isFalse();
        assertThat(result.shouldTriggerAi()).isFalse();
        assertThat(result.isAllClear()).isTrue();
        assertThat(result.issues()).isEmpty();
    }

    @Test
    @DisplayName("evaluateItinerary when all checkers return empty should return all clear (Silent by Default)")
    void evaluateItinerary_noIssuesFound_returnsAllClear() {
        ScheduleItemContext mockItem = new ScheduleItemContext(
            UUID.randomUUID(),
            "Điểm A",
            16.0,
            108.0,
            null,
            null,
            null,
            LocalDateTime.now(),
            60,
            0,
            0
        );
        when(checker1.evaluate(anyList())).thenReturn(List.of());
        when(checker2.evaluate(anyList())).thenReturn(List.of());

        ItineraryEvaluationResult result = service.evaluateItinerary(List.of(mockItem));

        assertThat(result.hasIssues()).isFalse();
        assertThat(result.shouldTriggerAi()).isFalse();
        assertThat(result.isAllClear()).isTrue();
    }

    @Test
    @DisplayName("evaluateItinerary when issues are found should aggregate and sort by severity (CRITICAL first)")
    void evaluateItinerary_issuesFound_aggregatesAndSorts() {
        ScheduleItemContext mockItem = new ScheduleItemContext(
            UUID.randomUUID(),
            "Điểm A",
            16.0,
            108.0,
            null,
            null,
            null,
            LocalDateTime.now(),
            60,
            0,
            0
        );

        DetectedIssue infoIssue = new DetectedIssue(
            mockItem.placeId(),
            "A",
            mockItem.arrivalTime(),
            IssueCategory.OVERLOADED_SCHEDULE,
            IssueSeverity.INFO,
            "Nhắc nhở nhẹ",
            "Gợi ý",
            Map.of()
        );
        DetectedIssue criticalIssue = new DetectedIssue(
            mockItem.placeId(),
            "A",
            mockItem.arrivalTime(),
            IssueCategory.CLOSING_TIME_CONFLICT,
            IssueSeverity.CRITICAL,
            "Đã đóng cửa",
            "Gợi ý",
            Map.of()
        );

        when(checker1.evaluate(anyList())).thenReturn(List.of(infoIssue));
        when(checker2.evaluate(anyList())).thenReturn(List.of(criticalIssue));

        ItineraryEvaluationResult result = service.evaluateItinerary(List.of(mockItem));

        assertThat(result.hasIssues()).isTrue();
        assertThat(result.shouldTriggerAi()).isTrue();
        assertThat(result.criticalCount()).isEqualTo(1);
        assertThat(result.infoCount()).isEqualTo(1);
        assertThat(result.issues()).hasSize(2);
        // Kiểm tra CRITICAL được đưa lên đầu tiên
        assertThat(result.issues().getFirst().severity()).isEqualTo(IssueSeverity.CRITICAL);
    }
}
