package com.travel.planning.ai.context.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Thông tin địa điểm trong ngữ cảnh gửi cho AI")
public record PlaceContext(
        @Schema(description = "ID của địa điểm")
        UUID id,

        @Schema(description = "Tên địa điểm")
        String name,

        @Schema(description = "Địa chỉ")
        String address,

        @Schema(description = "Vĩ độ GPS")
        Double latitude,

        @Schema(description = "Kinh độ GPS")
        Double longitude,

        @Schema(description = "Thời gian tham quan ước lượng (phút)")
        Integer estimatedDurationMinutes,

        @Schema(description = "Ghi chú về địa điểm")
        String notes
) {
}
