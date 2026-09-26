package com.travel.planning.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.travel.planning.ai.copilot.model.AiCopilotAnalysisResult;
import com.travel.planning.rule.model.ItineraryEvaluationResult;
import com.travel.planning.rule.model.ScheduleItemContext;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collections;
import java.util.List;

@Schema(description = "Kết quả tối ưu hóa lộ trình toàn diện kèm thẩm định quy tắc và đề xuất AI Copilot")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TripOptimizationResponse(
        @Schema(description = "Trạng thái tổng thể: ALL_CLEAR (hoàn hảo), CONFLICTS_DETECTED (có xung đột), UPDATED (đã cập nhật)",
                example = "ALL_CLEAR")
        String status,

        @Schema(description = "Tổng quãng đường di chuyển ước tính (km)", example = "18.5")
        double totalDistanceKm,

        @Schema(description = "Tổng thời gian di chuyển ước tính (phút)", example = "45")
        long totalTransitMinutes,

        @Schema(description = "Lộ trình chi tiết theo thứ tự tối ưu kèm thời gian đến/rời đi")
        List<ScheduleItemContext> schedule,

        @Schema(description = "Kết quả thẩm định các quy tắc xác định (giờ mở cửa, thời tiết, di chuyển)")
        ItineraryEvaluationResult evaluation,

        @Schema(description = "Phản biện và đề xuất từ Trợ lý AI Copilot (chỉ có khi phát hiện xung đột, null khi ALL_CLEAR)")
        AiCopilotAnalysisResult copilot
) {
    public TripOptimizationResponse {
        if (schedule == null) {
            schedule = Collections.emptyList();
        }
    }
}
