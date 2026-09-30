package com.travel.planning.rule.model;

import com.travel.planning.rule.enums.IssueSeverity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Schema(description = "Kết quả thẩm định toàn bộ lịch trình từ Rule Evaluator")
public record ItineraryEvaluationResult(
        @Schema(description = "Có phát hiện vấn đề nào không", example = "true")
        boolean hasIssues,

        @Schema(description = "Có cần kích hoạt AI phản biện không (chỉ bật khi có xung đột)", example = "true")
        boolean shouldTriggerAi,

        @Schema(description = "Danh sách các vấn đề phát hiện (đã sắp xếp theo mức độ nghiêm trọng)")
        List<DetectedIssue> issues,

        @Schema(description = "Số lượng vấn đề nghiêm trọng (CRITICAL)", example = "1")
        int criticalCount,

        @Schema(description = "Số lượng vấn đề quan trọng (IMPORTANT)", example = "1")
        int importantCount,

        @Schema(description = "Số lượng nhắc nhở (INFO)", example = "0")
        int infoCount,

        @Schema(description = "Tóm tắt kết quả thẩm định",
                example = "Phát hiện 2 vấn đề trong lịch trình (1 Nghiêm trọng, 1 Quan trọng)")
        String summary
) {
    public static ItineraryEvaluationResult of(List<DetectedIssue> rawIssues) {
        if (rawIssues == null || rawIssues.isEmpty()) {
            return new ItineraryEvaluationResult(
                    false,
                    false,
                    Collections.emptyList(),
                    0,
                    0,
                    0,
                    "Lộ trình hoàn hảo! Không phát hiện xung đột về thời gian, thời tiết hay giờ mở cửa."
            );
        }

        // Sắp xếp theo thứ tự ưu tiên: CRITICAL (0) -> IMPORTANT (1) -> INFO (2)
        List<DetectedIssue> sortedIssues = rawIssues.stream()
                .sorted(Comparator.comparing(issue -> issue.severity().ordinal()))
                .toList();

        int critical = 0;
        int important = 0;
        int info = 0;
        for (DetectedIssue detectedIssue : sortedIssues) {
            if (detectedIssue.severity() == IssueSeverity.CRITICAL) {
                critical++;
            } else if (detectedIssue.severity() == IssueSeverity.IMPORTANT) {
                important++;
            } else {
                info++;
            }
        }

        String summary = String.format("Phát hiện %d vấn đề trong lịch trình (%d Nghiêm trọng, %d Quan trọng, %d Nhắc nhở)",
                sortedIssues.size(), critical, important, info);

        return new ItineraryEvaluationResult(
                true,
                true, // Bật cờ gọi AI
                sortedIssues,
                critical,
                important,
                info,
                summary
        );
    }

    /**
     * Kiểm tra xem toàn bộ lịch trình có an toàn, thông suốt không (Silent by default)
     */
    public boolean isAllClear() {
        return !hasIssues;
    }
}
