package com.travel.planning.ai.copilot.model;

import com.travel.planning.rule.model.DetectedIssue;
import com.travel.planning.rule.model.ScheduleItemContext;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Schema(description = "Ngữ cảnh đầy đủ của lịch trình và các xung đột gửi sang AI Copilot")
public record AiCopilotPromptContext(
    String destination,
    LocalDate tripDate,
    List<ScheduleItemContext> scheduledItems,
    List<DetectedIssue> detectedIssues,
    String weatherOverview,
    String userNotes
) {
    public AiCopilotPromptContext {
        if (scheduledItems == null) {
            scheduledItems = Collections.emptyList();
        }
        if (detectedIssues == null) {
            detectedIssues = Collections.emptyList();
        }
        if (weatherOverview == null) {
            weatherOverview = "Không có thông tin thời tiết đặc biệt.";
        }
    }
}
