package com.travel.planning.ai.context.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Chặng di chuyển giữa 2 địa điểm liên tiếp trong chuỗi TSP")
public record RouteLegContext(
        String fromPlaceName,
        String toPlaceName,
        double distanceKm,
        long durationMinutes
) {
}
