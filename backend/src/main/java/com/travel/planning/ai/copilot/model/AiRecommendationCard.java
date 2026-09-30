package com.travel.planning.ai.copilot.model;

import com.travel.planning.ai.copilot.enums.CopilotActionType;
import com.travel.planning.rule.enums.IssueSeverity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Thẻ đề xuất hành động từ Trợ lý AI Copilot (tối đa 3 thẻ được ưu tiên)")
public record AiRecommendationCard(
        @Schema(description = "Mã định danh duy nhất của thẻ đề xuất")
        String cardId,

        @Schema(description = "Mức độ ưu tiên của thẻ (CRITICAL, IMPORTANT, INFO)")
        IssueSeverity priority,

        @Schema(description = "Tiêu đề ngắn gọn, trực diện của vấn đề",
                example = "Bảo tàng Điêu khắc Chăm đóng cửa trước khi bạn đến")
        String title,

        @Schema(description = "Lời giải thích ân cần và dễ hiểu bằng tiếng Việt",
                example = "Bảo tàng đóng cửa lúc 17:00, nhưng lịch trình dự kiến bạn đến lúc 17:30. Nếu đảo thứ tự lên đầu giờ chiều, bạn sẽ có trọn vẹn thời gian tham quan.")
        String explanation,

        @Schema(description = "Loại hành động 1-click")
        CopilotActionType actionType,

        @Schema(description = "Nhãn hiển thị trên nút hành động", example = "Đảo lên trước 16:00")
        String actionButtonText,

        @Schema(description = "Dữ liệu payload chi tiết để thực thi 1-click (ví dụ targetPlaceId, swapWithPlaceId, newTime)")
        Map<String, Object> actionPayload
) {
    public AiRecommendationCard {
        if (cardId == null || cardId.isBlank()) {
            cardId = UUID.randomUUID().toString();
        }
        if (priority == null) {
            priority = IssueSeverity.INFO;
        }
        if (actionType == null) {
            actionType = CopilotActionType.DISMISS;
        }
        if (actionPayload == null) {
            actionPayload = Collections.emptyMap();
        } else {
            actionPayload = Collections.unmodifiableMap(actionPayload);
        }
    }
}
