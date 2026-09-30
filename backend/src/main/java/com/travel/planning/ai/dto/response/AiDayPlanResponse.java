package com.travel.planning.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Kế hoạch chi tiết lịch trình theo từng ngày")
public record AiDayPlanResponse(
        @Schema(description = "Ngày thứ mấy trong chuyến đi (1, 2, 3...)", example = "1")
        int dayNumber,

        @Schema(description = "Ngày cụ thể theo lịch (YYYY-MM-DD)", example = "2026-10-01")
        String date,

        @Schema(description = "Chủ đề điểm nhấn của ngày", example = "Khám phá Sơn Trà hùng vĩ & Biển Mỹ Khê")
        String dayTheme,

        @Schema(description = "Danh sách các hoạt động trong ngày theo thứ tự thời gian")
        List<AiActivitySlotResponse> activities,

        @Schema(description = "Gợi ý ẩm thực đặc sản cho bữa trưa và tối", example = "Trưa: Bánh tráng cuốn thịt heo Đại Lộc; Tối: Hải sản Năm Đảnh")
        String mealRecommendations,

        @Schema(description = "Lời khuyên kinh nghiệm riêng cho ngày này", example = "Nên thuê xe máy để di chuyển linh hoạt trên đường đèo Sơn Trà")
        String dayTips
) {
    public AiDayPlanResponse {
        activities = activities != null ? List.copyOf(activities) : List.of();
    }
}
