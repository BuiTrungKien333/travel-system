package com.travel.planning.ai.validator;

import com.travel.planning.ai.context.model.AiItineraryPromptContext;
import com.travel.planning.ai.context.model.PlaceContext;
import com.travel.planning.ai.dto.response.AiActivitySlotResponse;
import com.travel.planning.ai.dto.response.AiDayPlanResponse;
import com.travel.planning.ai.dto.response.AiItineraryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AiResponseValidator {

    /**
     * Xác thực và tự động khắc phục nếu AI bỏ sót địa điểm hoặc dữ liệu không nhất quán
     */
    public AiItineraryResponse validateAndEnforce(AiItineraryResponse response, AiItineraryPromptContext context) {
        if (response == null || response.dailyPlans() == null || response.dailyPlans().isEmpty()) {
            log.warn("[AI_VALIDATOR] Response or dailyPlans is empty. Returning response as-is.");
            return response;
        }

        List<PlaceContext> inputPlaces = context.orderedPlaces();
        if (inputPlaces == null || inputPlaces.isEmpty()) {
            return response;
        }

        // Tập hợp các placeId mà AI đã trả về
        Set<UUID> assignedPlaceIds = response.dailyPlans().stream()
                .flatMap(day -> day.activities().stream())
                .map(AiActivitySlotResponse::placeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Tìm các địa điểm đầu vào bị bỏ sót
        List<PlaceContext> missingPlaces = inputPlaces.stream()
                .filter(p -> p.id() != null && !assignedPlaceIds.contains(p.id()))
                .toList();

        if (missingPlaces.isEmpty()) {
            log.debug("[AI_VALIDATOR] All {} input places are successfully covered in AI response.", inputPlaces.size());
            return response;
        }

        log.warn("[AI_VALIDATOR] AI omitted {} places: {}. Auto-healing itinerary...",
                missingPlaces.size(), missingPlaces.stream().map(PlaceContext::name).toList());

        // Auto-heal: Tìm ngày có số lượng hoạt động ít nhất để chèn địa điểm bị thiếu vào
        List<AiDayPlanResponse> healedDays = new ArrayList<>();
        for (AiDayPlanResponse day : response.dailyPlans()) {
            healedDays.add(new AiDayPlanResponse(
                    day.dayNumber(),
                    day.date(),
                    day.dayTheme(),
                    new ArrayList<>(day.activities()),
                    day.mealRecommendations(),
                    day.dayTips()
            ));
        }

        for (PlaceContext missing : missingPlaces) {
            // Tìm ngày có ít hoạt động nhất
            AiDayPlanResponse targetDay = healedDays.stream()
                    .min(Comparator.comparingInt(d -> d.activities().size()))
                    .orElse(healedDays.getFirst());

            List<AiActivitySlotResponse> updatedActivities = new ArrayList<>(targetDay.activities());
            updatedActivities.add(new AiActivitySlotResponse(
                    missing.id(),
                    missing.name(),
                    "Buổi chiều (Bổ sung)",
                    "VISIT",
                    "Tham quan và khám phá " + missing.name(),
                    missing.estimatedDurationMinutes() != null ? missing.estimatedDurationMinutes() + " phút" : "90 phút",
                    "Điểm dừng chân tự động bổ sung theo lộ trình tối ưu"
            ));

            int targetIndex = healedDays.indexOf(targetDay);
            healedDays.set(targetIndex, new AiDayPlanResponse(
                    targetDay.dayNumber(),
                    targetDay.date(),
                    targetDay.dayTheme(),
                    updatedActivities,
                    targetDay.mealRecommendations(),
                    targetDay.dayTips()
            ));
        }

        return new AiItineraryResponse(
                response.tripTitle(),
                response.tripSummary(),
                healedDays,
                response.generalTips(),
                response.estimatedBudgetNote()
        );
    }
}
