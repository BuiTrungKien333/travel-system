package com.travel.planning.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Lịch trình du lịch toàn diện do AI tối ưu và gợi ý")
public record AiItineraryResponse(
        @Schema(description = "Tiêu đề hấp dẫn cho chuyến đi", example = "Hành trình 3 ngày 2 đêm tuyệt tác Đà Nẵng - Hội An")
        String tripTitle,

        @Schema(description = "Tóm tắt tổng quan chuyến đi", example = "Lịch trình kết hợp giữa danh thắng thiên nhiên kỳ vĩ và di sản văn hóa cổ kính...")
        String tripSummary,

        @Schema(description = "Kế hoạch chi tiết từng ngày")
        List<AiDayPlanResponse> dailyPlans,

        @Schema(description = "Các lời khuyên chung (chuẩn bị hành lý, phương tiện, thời tiết)")
        List<String> generalTips,

        @Schema(description = "Ghi chú và dự toán ngân sách gợi ý", example = "Khoảng 3.500.000đ - 5.000.000đ/người cho 3 ngày (chưa gồm vé máy bay)")
        String estimatedBudgetNote
) {
    public AiItineraryResponse {
        dailyPlans = dailyPlans != null ? List.copyOf(dailyPlans) : List.of();
        generalTips = generalTips != null ? List.copyOf(generalTips) : List.of();
    }
}
