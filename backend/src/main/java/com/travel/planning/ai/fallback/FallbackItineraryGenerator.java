package com.travel.planning.ai.fallback;

import com.travel.planning.ai.context.model.AiItineraryPromptContext;
import com.travel.planning.ai.context.model.PlaceContext;
import com.travel.planning.ai.dto.response.AiActivitySlotResponse;
import com.travel.planning.ai.dto.response.AiDayPlanResponse;
import com.travel.planning.ai.dto.response.AiItineraryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class FallbackItineraryGenerator {

    private static final String[] DEFAULT_TIME_SLOTS = {
            "08:30 - 11:30",
            "14:00 - 16:30",
            "17:00 - 19:00",
            "19:30 - 21:30"
    };

    /**
     * Tạo lịch trình dự phòng phân bổ đều các địa điểm TSP vào K ngày
     */
    public AiItineraryResponse generateFallback(AiItineraryPromptContext context) {
        log.info("[FALLBACK_ITINERARY] Generating heuristic fallback itinerary for destination={} across {} days",
                context.destination(), context.totalDays());

        List<PlaceContext> places = context.orderedPlaces();
        int totalDays = Math.max(1, context.totalDays());
        int totalPlaces = places.size();

        List<AiDayPlanResponse> dailyPlans = new ArrayList<>();
        LocalDate startDate = context.startDate() != null ? context.startDate() : LocalDate.now();

        if (totalPlaces == 0) {
            dailyPlans.add(new AiDayPlanResponse(
                    1,
                    startDate.toString(),
                    "Tự do khám phá " + (context.destination() != null ? context.destination() : ""),
                    List.of(),
                    "Thưởng thức ẩm thực tự do tại trung tâm thành phố",
                    "Nghỉ ngơi và dạo phố nhẹ nhàng"
            ));
        } else {
            // Chia N địa điểm vào K ngày theo thuật toán chia đều
            int baseSize = totalPlaces / totalDays;
            int remainder = totalPlaces % totalDays;
            int currentIndex = 0;

            for (int day = 1; day <= totalDays; day++) {
                int daySize = baseSize + (day <= remainder ? 1 : 0);
                List<AiActivitySlotResponse> activities = new ArrayList<>();
                LocalDate currentDate = startDate.plusDays(day - 1);

                for (int i = 0; i < daySize && currentIndex < totalPlaces; i++) {
                    PlaceContext place = places.get(currentIndex);
                    String timeSlot = i < DEFAULT_TIME_SLOTS.length ? DEFAULT_TIME_SLOTS[i] : "Trong ngày";

                    activities.add(new AiActivitySlotResponse(
                            place.id(),
                            place.name(),
                            timeSlot,
                            "VISIT",
                            "Tham quan và trải nghiệm tại " + place.name(),
                            place.estimatedDurationMinutes() != null ? place.estimatedDurationMinutes() + " phút" : "90 - 120 phút",
                            "Di chuyển theo lộ trình tối ưu đã được tính toán"
                    ));
                    currentIndex++;
                }

                String theme = activities.isEmpty()
                        ? "Nghỉ ngơi và trải nghiệm văn hóa địa phương"
                        : "Khám phá " + activities.getFirst().placeName() + " và các điểm lân cận";

                dailyPlans.add(new AiDayPlanResponse(
                        day,
                        currentDate.toString(),
                        theme,
                        activities,
                        "Trưa: Thưởng thức đặc sản vùng miền gần điểm tham quan; Tối: Trải nghiệm ẩm thực địa phương",
                        "Mang theo mũ nón, nước uống và kiểm tra thời tiết trước khi khởi hành"
                ));
            }
        }

        String title = "Lịch trình " + totalDays + " ngày khám phá " +
                (context.destination() != null ? context.destination() : "điểm đến");
        String summary = "Lịch trình được tối ưu hóa đường đi ngắn nhất kết hợp phân bổ khoa học các điểm tham quan.";

        List<String> generalTips = List.of(
                "Chuẩn bị giấy tờ tùy thân và trang phục phù hợp với thời tiết",
                "Nên khởi hành vào buổi sáng sớm để tận hưởng không khí mát mẻ",
                "Lưu lại số điện thoại hỗ trợ du lịch địa phương khi cần thiết"
        );

        return new AiItineraryResponse(
                title,
                summary,
                dailyPlans,
                generalTips,
                "Dự toán chi phí linh hoạt tùy thuộc vào phương tiện và nhu cầu cá nhân"
        );
    }
}
