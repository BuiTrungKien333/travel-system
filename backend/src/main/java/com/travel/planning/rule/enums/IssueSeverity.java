package com.travel.planning.rule.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Mức độ nghiêm trọng của vấn đề phát hiện trong lịch trình")
public enum IssueSeverity {
    @Schema(description = "Nghiêm trọng: Bắt buộc phải xử lý (đóng cửa, bão lũ nguy hiểm, bất khả thi)")
    CRITICAL,

    @Schema(description = "Quan trọng: Nên điều chỉnh để chuyến đi trọn vẹn (mưa rào, nắng gắt, di chuyển sát giờ)")
    IMPORTANT,

    @Schema(description = "Thông tin / Nhắc nhở: Lời khuyên hữu ích (mang ô/áo mưa, bù nước, lưu ý)")
    INFO
}
