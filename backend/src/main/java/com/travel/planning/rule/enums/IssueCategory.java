package com.travel.planning.rule.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Phân loại nhóm vấn đề phát hiện trong lịch trình du lịch")
public enum IssueCategory {
    @Schema(description = "Xung đột giờ mở/đóng cửa (đến trước giờ mở hoặc sau giờ đóng cửa)")
    CLOSING_TIME_CONFLICT,

    @Schema(description = "Thời tiết nguy hiểm đến tính mạng hoặc an toàn (dông bão, mưa lớn, nhiệt độ cực đoan)")
    HAZARDOUS_WEATHER,

    @Schema(description = "Thời tiết bất lợi nhẹ (mưa nhỏ/vừa, nắng nóng buổi trưa)")
    ADVERSE_WEATHER,

    @Schema(description = "Thời gian di chuyển không khả thi hoặc quá gấp gáp")
    TIGHT_TRANSIT_TIME,

    @Schema(description = "Lịch trình quá dày đặc, nguy cơ quá tải/kiệt sức")
    OVERLOADED_SCHEDULE
}
