package com.travel.planning.ai.copilot.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collections;
import java.util.List;

@Schema(description = "Kết quả phản biện và đề xuất từ Trợ lý AI Copilot")
public record AiCopilotAnalysisResult(
        @Schema(description = "Lời chào ân cần và nhắc nhở chung về thời tiết, sức khỏe, hành trang",
                example = "Chào bạn! Hôm nay Đà Nẵng dự báo có mưa rào vào đầu giờ chiều (từ 13:00 - 15:00). Bạn nhớ chuẩn bị sẵn áo mưa hoặc ô dù nhé!")
        String greetingAndCaringReminder,

        @Schema(description = "Danh sách tối đa 3 thẻ đề xuất hành động đã xếp theo thứ tự ưu tiên")
        List<AiRecommendationCard> recommendationCards,

        @Schema(description = "Tên AI Provider đã xử lý (ví dụ: gemini-2.0-flash, mock)")
        String aiProvider
) {
    public AiCopilotAnalysisResult {
        if (recommendationCards == null) {
            recommendationCards = Collections.emptyList();
        } else if (recommendationCards.size() > 3) {
            recommendationCards = recommendationCards.subList(0, 3); // Giới hạn tối đa 3 thẻ
        }
        if (aiProvider == null || aiProvider.isBlank()) {
            aiProvider = "gemini";
        }
    }
}
