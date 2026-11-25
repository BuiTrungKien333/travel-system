package com.travel.planning.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Kết quả tối ưu lộ trình du lịch (Held-Karp TSP)")
public record TspOptimizeResponse(
    @Schema(description = "Danh sách địa điểm theo thứ tự tối ưu nhất") List<TspLocationResponse> orderedPlaces,

    @Schema(description = "Chi tiết các chặng di chuyển") List<TspLegResponse> legs,

    @Schema(description = "Tổng quãng đường di chuyển (mét)", example = "72591.2") double totalDistanceMeters,

    @Schema(description = "Tổng quãng đường di chuyển (kilômét)", example = "72.59") double totalDistanceKilometers,

    @Schema(description = "Tổng thời gian di chuyển ước tính (giây)", example = "7466") long totalDurationSeconds,

    @Schema(description = "Tổng thời gian di chuyển ước tính (phút)", example = "124") long totalDurationMinutes,

    @Schema(description = "Lộ trình khứ hồi (quay về điểm xuất phát)", example = "true") boolean roundTrip
) {}
