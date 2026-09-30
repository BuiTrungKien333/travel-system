package com.travel.planning.ai.context.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Sở thích và phong cách du lịch của người dùng")
public record UserPreferencesContext(
        @Schema(description = "Nhịp độ du lịch (RELAXED, MODERATE, FAST_PACED)", example = "MODERATE")
        String travelPace,

        @Schema(description = "Đối tượng đồng hành (SOLO, COUPLE, FAMILY, FRIENDS)", example = "FRIENDS")
        String companionType,

        @Schema(description = "Hạng mức ngân sách (BUDGET, STANDARD, LUXURY)", example = "STANDARD")
        String budgetTier,

        @Schema(description = "Danh sách sở thích du lịch (ẩm thực, văn hóa, check-in, thiên nhiên...)")
        List<String> interests,

        @Schema(description = "Ghi chú hoặc yêu cầu đặc biệt của người dùng")
        String notes
) {
    public UserPreferencesContext {
        if (interests == null) {
            interests = List.of();
        } else {
            interests = List.copyOf(interests);
        }
    }
}
