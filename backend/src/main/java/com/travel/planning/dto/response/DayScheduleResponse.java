package com.travel.planning.dto.response;

import com.travel.planning.rule.model.ItineraryEvaluationResult;
import com.travel.planning.rule.model.ScheduleItemContext;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Kết quả tối ưu và lịch trình chi tiết của một ngày")
public record DayScheduleResponse(
        @Schema(description = "Ngày kế hoạch", example = "2026-10-01")
        LocalDate dayDate,

        @Schema(description = "Trạng thái thẩm định ngày (ALL_CLEAR, CONFLICTS_DETECTED, UPDATED)")
        String status,

        @Schema(description = "Tổng quãng đường di chuyển trong ngày (km)", example = "15.4")
        double totalDistanceKm,

        @Schema(description = "Tổng thời gian di chuyển trong ngày (phút)", example = "45")
        long totalTransitMinutes,

        @Schema(description = "Lịch trình các lượt ghé theo thời gian thực")
        List<ScheduleItemContext> scheduleItems,

        @Schema(description = "Kết quả thẩm định quy tắc cứng của ngày")
        ItineraryEvaluationResult evaluationResult
) {
}
