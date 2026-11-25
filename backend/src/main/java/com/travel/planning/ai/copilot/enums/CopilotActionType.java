package com.travel.planning.ai.copilot.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Loại hành động 1-click do AI Copilot đề xuất")
public enum CopilotActionType {
    @Schema(description = "Đảo thứ tự 2 địa điểm tham quan để kịp giờ mở cửa hoặc tránh thời tiết xấu")
    SWAP_PLACES,

    @Schema(description = "Điều chỉnh giờ đến hoặc giờ xuất phát")
    CHANGE_TIME,

    @Schema(
        description = "Thay thế một địa điểm bằng địa điểm khác phù hợp hơn (ví dụ thay ngoài trời bằng trong nhà khi mưa)"
    )
    REPLACE_PLACE,

    @Schema(description = "Bỏ qua lời nhắc (chỉ mang tính thông tin tham khảo)")
    DISMISS,
}
