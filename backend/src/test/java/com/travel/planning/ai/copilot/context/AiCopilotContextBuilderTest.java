package com.travel.planning.ai.copilot.context;

import com.travel.place.enums.PlaceType;
import com.travel.planning.ai.copilot.model.AiCopilotPromptContext;
import com.travel.planning.rule.enums.IssueCategory;
import com.travel.planning.rule.enums.IssueSeverity;
import com.travel.planning.rule.model.DetectedIssue;
import com.travel.planning.rule.model.ScheduleItemContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiCopilotContextBuilderTest {

    private AiCopilotContextBuilder contextBuilder;

    @BeforeEach
    void setUp() {
        contextBuilder = new AiCopilotContextBuilder();
    }

    @Test
    @DisplayName("buildSystemInstruction should mandate valid JSON and HITL constraints")
    void buildSystemInstruction_containsMandatoryConstraints() {
        String instruction = contextBuilder.buildSystemInstruction();

        assertThat(instruction).contains("Tripzy Travel Copilot");
        assertThat(instruction).contains("Human-In-The-Loop");
        assertThat(instruction).contains("greetingAndCaringReminder");
        assertThat(instruction).contains("recommendationCards");
        assertThat(instruction).contains("SWAP_PLACES");
    }

    @Test
    @DisplayName("buildUserPrompt should format schedule, issues, and weather into prompt")
    void buildUserPrompt_formatsContextCorrectly() {
        LocalDate date = LocalDate.of(2026, 10, 1);
        UUID p1Id = UUID.randomUUID();
        ScheduleItemContext item1 = new ScheduleItemContext(
                p1Id, "Bảo tàng Chăm", 16.05, 108.22, PlaceType.INDOOR,
                LocalTime.of(8, 0), LocalTime.of(17, 0),
                date.atTime(17, 30), 60, 0.0, 0
        );

        DetectedIssue issue = new DetectedIssue(
                p1Id, "Bảo tàng Chăm", date.atTime(17, 30),
                IssueCategory.CLOSING_TIME_CONFLICT, IssueSeverity.CRITICAL,
                "Đến sau giờ đóng cửa", "Đảo thứ tự lên trước", Map.of()
        );

        AiCopilotPromptContext context = new AiCopilotPromptContext(
                "Đà Nẵng", date, List.of(item1), List.of(issue),
                "Nhiệt độ 28-32°C, có mưa rào chiều", "Đi cùng gia đình"
        );

        String prompt = contextBuilder.buildUserPrompt(context);

        assertThat(prompt).contains("Đà Nẵng");
        assertThat(prompt).contains("01/10/2026");
        assertThat(prompt).contains("Bảo tàng Chăm");
        assertThat(prompt).contains("CLOSING_TIME_CONFLICT");
        assertThat(prompt).contains("CRITICAL");
        assertThat(prompt).contains("Đi cùng gia đình");
    }
}
