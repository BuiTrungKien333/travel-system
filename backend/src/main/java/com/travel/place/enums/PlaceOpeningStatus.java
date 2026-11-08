package com.travel.place.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Trạng thái giờ hoạt động của địa điểm trong ngày")
public enum PlaceOpeningStatus {
    @Schema(description = "Mở cửa theo khung giờ xác định")
    OPEN,

    @Schema(description = "Đóng cửa cả ngày")
    CLOSED,

    @Schema(description = "Chưa có số liệu / chưa xác minh được giờ hoạt động")
    UNVERIFIED
}
