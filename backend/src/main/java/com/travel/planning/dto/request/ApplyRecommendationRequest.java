package com.travel.planning.dto.request;

import com.travel.planning.ai.copilot.enums.CopilotActionType;
import com.travel.planning.rule.model.ScheduleItemContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Schema(description = "Yêu cầu thực thi 1-click hành động đề xuất từ AI Copilot (Human-In-The-Loop)")
public record ApplyRecommendationRequest(
        @Schema(description = "ID của thẻ đề xuất được bấm 'Áp dụng'")
        String cardId,

        @NotNull(message = "Loại hành động không được để trống")
        @Schema(description = "Loại hành động được thực thi (SWAP_PLACES, CHANGE_TIME, REPLACE_PLACE, DISMISS)")
        CopilotActionType actionType,

        @Schema(description = "Dữ liệu payload đi kèm hành động (targetPlaceId, swapWithPlaceId...)")
        Map<String, Object> actionPayload,

        @NotEmpty(message = "Danh sách lịch trình hiện tại không được để trống")
        @Schema(description = "Danh sách lịch trình hiện tại trước khi áp dụng")
        List<ScheduleItemContext> currentSchedule
) {
    public ApplyRecommendationRequest {
        if (actionPayload == null) {
            actionPayload = Collections.emptyMap();
        }
    }
}
