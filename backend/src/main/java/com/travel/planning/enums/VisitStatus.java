package com.travel.planning.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Trạng thái ghé thăm của địa điểm trong hành trình")
public enum VisitStatus {
    @Schema(description = "Chưa đến / chưa ghé thăm (được tham gia tối ưu lại)")
    NOT_VISITED,

    @Schema(description = "Đang ở địa điểm (làm điểm neo cho phần lịch còn lại)")
    IN_PROGRESS,

    @Schema(description = "Đã hoàn thành lượt ghé (giữ nguyên mốc thời gian thực tế)")
    COMPLETED,

    @Schema(description = "Đã bỏ qua (loại khỏi lộ trình hiện tại)")
    SKIPPED
}
