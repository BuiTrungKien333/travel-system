package com.travel.planning.ai.context.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Ngữ cảnh đầy đủ để AI phân tích và lập kế hoạch lịch trình")
public record AiItineraryPromptContext(
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        int totalDays,
        UserPreferencesContext preferences,
        List<PlaceContext> orderedPlaces,
        List<RouteLegContext> routeLegs,
        double totalDistanceKm,
        long totalTravelTimeMinutes
) {
    public AiItineraryPromptContext {
        orderedPlaces = orderedPlaces != null ? List.copyOf(orderedPlaces) : List.of();
        routeLegs = routeLegs != null ? List.copyOf(routeLegs) : List.of();
        if (totalDays <= 0) {
            totalDays = 1;
        }
    }
}
