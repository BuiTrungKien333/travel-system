package com.travel.planning.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.travel.planning.ai.copilot.model.AiCopilotAnalysisResult;
import com.travel.planning.rule.model.DetectedIssue;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Kết quả tối ưu và thẩm định chuyến đi nhiều ngày (Multi-day)")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MultiDayOptimizeAndAuditResponse(
        @Schema(description = "Trạng thái tổng thể chuyến đi (ALL_CLEAR, CONFLICTS_DETECTED)", example = "ALL_CLEAR")
        String status,

        @Schema(description = "Tổng quãng đường di chuyển toàn bộ chuyến đi (km)", example = "42.8")
        double totalTripDistanceKm,

        @Schema(description = "Tổng thời gian di chuyển toàn bộ chuyến đi (phút)", example = "135")
        long totalTripTransitMinutes,

        @Schema(description = "Lịch trình chi tiết theo từng ngày")
        List<DayScheduleResponse> dailySchedules,

        @Schema(description = "Các xung đột toàn cục (bao gồm cả va chạm lịch trình giữa các ngày liên tiếp)")
        List<DetectedIssue> globalIssues,

        @Schema(description = "Đề xuất phản biện và thẻ hành động từ AI Copilot (chỉ có khi phát hiện xung đột)")
        AiCopilotAnalysisResult copilotRecommendations
) {
}
