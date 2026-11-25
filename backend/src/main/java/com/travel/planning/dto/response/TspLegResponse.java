package com.travel.planning.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Thông tin chặng di chuyển giữa 2 địa điểm liên tiếp")
public record TspLegResponse(
    @Schema(description = "Điểm khởi hành chặng") TspLocationResponse from,

    @Schema(description = "Điểm đến của chặng") TspLocationResponse to,

    @Schema(description = "Khoảng cách di chuyển (mét)", example = "2058.4") double distanceMeters,

    @Schema(description = "Khoảng cách di chuyển (kilômét)", example = "2.06") double distanceKilometers,

    @Schema(description = "Thời gian di chuyển ước tính (giây)", example = "211") long durationSeconds,

    @Schema(description = "Thời gian di chuyển ước tính (phút)", example = "4") long durationMinutes
) {}
