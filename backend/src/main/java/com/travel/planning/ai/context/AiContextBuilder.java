package com.travel.planning.ai.context;

import com.travel.planning.ai.context.model.AiItineraryPromptContext;
import com.travel.planning.ai.context.model.PlaceContext;
import com.travel.planning.ai.context.model.RouteLegContext;
import com.travel.planning.ai.context.model.UserPreferencesContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AiContextBuilder {

    /**
     * Tạo System Instruction cho AI đóng vai trò chuyên gia du lịch
     */
    public String buildSystemInstruction() {
        return """
                Bạn là một chuyên gia lập kế hoạch và hướng dẫn viên du lịch bản địa hàng đầu tại Việt Nam.
                Nhiệm vụ của bạn là nhận danh sách các địa điểm tham quan đã được tối ưu hóa quãng đường ngắn nhất bằng thuật toán Held-Karp, kết hợp với các sở thích, nhịp độ và thời gian của người dùng để tạo ra một lịch trình du lịch nhiều ngày chi tiết, sống động và tối ưu nhất.

                QUY TẮC BẮT BUỘC:
                1. TÔN TRỌNG THỨ TỰ ĐỊA ĐIỂM: Các địa điểm được cung cấp đã được sắp xếp tối ưu quãng đường. Hãy phân bổ các địa điểm này vào các ngày liên tiếp theo đúng luồng di chuyển đó để không làm người dùng phải di chuyển vòng vèo.
                2. ĐẦY ĐỦ 100% ĐỊA ĐIỂM: Tuyệt đối không được bỏ sót bất kỳ địa điểm nào trong danh sách đầu vào. Tất cả địa điểm được cung cấp phải được gán vào ít nhất một hoạt động cụ thể (activityType = "VISIT").
                3. GIỮ NGUYÊN PLACE_ID: Với mỗi địa điểm từ danh sách đầu vào, bạn PHẢI giữ chính xác trường "placeId" tương ứng trong JSON kết quả. Không được tự tạo hoặc thay đổi placeId.
                4. PHÂN BỔ THEO NHỊP ĐỘ (travelPace):
                   - RELAXED: 2-3 điểm/ngày, nhiều thời gian thư giãn, nghỉ ngơi.
                   - MODERATE: 3-4 điểm/ngày, cân bằng hợp lý giữa tham quan và trải nghiệm.
                   - FAST_PACED: 4-5 điểm/ngày, tận dụng tối đa thời gian.
                5. ẨM THỰC ĐỊA PHƯƠNG: Đưa ra gợi ý món ăn đặc sản địa phương (mealRecommendations) cho bữa trưa và bữa tối gần các điểm tham quan của ngày đó.
                6. ĐỊNH DẠNG JSON: Trả về duy nhất JSON hợp lệ, tuân thủ chặt chẽ cấu trúc schema được yêu cầu.
                """;
    }

    /**
     * Tạo User Prompt chi tiết từ ngữ cảnh chuyến đi và chuỗi TSP
     */
    public String buildUserPrompt(AiItineraryPromptContext context) {
        StringBuilder sb = new StringBuilder();

        sb.append("### THÔNG TIN CHUYẾN ĐI\n");
        sb.append("- Điểm đến: ").append(context.destination() != null ? context.destination() : "Chưa xác định").append("\n");
        sb.append("- Tổng số ngày: ").append(context.totalDays()).append(" ngày\n");
        if (context.startDate() != null) {
            sb.append("- Thời gian: Từ ").append(context.startDate());
            if (context.endDate() != null) {
                sb.append(" đến ").append(context.endDate());
            }
            sb.append("\n");
        }

        UserPreferencesContext prefs = context.preferences();
        if (prefs != null) {
            sb.append("\n### SỞ THÍCH VÀ PHONG CÁCH DU LỊCH\n");
            sb.append("- Nhịp độ du lịch (travelPace): ").append(prefs.travelPace() != null ? prefs.travelPace() : "MODERATE").append("\n");
            sb.append("- Đối tượng đi cùng: ").append(prefs.companionType() != null ? prefs.companionType() : "Bạn bè/Gia đình").append("\n");
            sb.append("- Mức ngân sách: ").append(prefs.budgetTier() != null ? prefs.budgetTier() : "STANDARD").append("\n");
            if (!prefs.interests().isEmpty()) {
                sb.append("- Sở thích: ").append(String.join(", ", prefs.interests())).append("\n");
            }
            if (prefs.notes() != null && !prefs.notes().isBlank()) {
                sb.append("- Yêu cầu đặc biệt: ").append(prefs.notes()).append("\n");
            }
        }

        List<PlaceContext> places = context.orderedPlaces();
        sb.append("\n### DANH SÁCH ĐỊA ĐIỂM THEO THỨ TỰ TỐI ƯU HELD-KARP (Tổng quãng đường: ")
                .append(String.format("%.2f km", context.totalDistanceKm()))
                .append(", Thời gian di chuyển: ")
                .append(context.totalTravelTimeMinutes())
                .append(" phút)\n");

        for (int i = 0; i < places.size(); i++) {
            PlaceContext p = places.get(i);
            sb.append(String.format("%d. [ID: %s] %s", i + 1, p.id() != null ? p.id().toString() : "N/A", p.name()));
            if (p.address() != null && !p.address().isBlank()) {
                sb.append(" - Địa chỉ: ").append(p.address());
            }
            if (p.estimatedDurationMinutes() != null) {
                sb.append(" (Thời lượng gợi ý: ").append(p.estimatedDurationMinutes()).append(" phút)");
            }
            sb.append("\n");
        }

        List<RouteLegContext> legs = context.routeLegs();
        if (!legs.isEmpty()) {
            sb.append("\n### CÁC CHẶNG DI CHUYỂN GIỮA CÁC ĐỊA ĐIỂM:\n");
            for (int i = 0; i < legs.size(); i++) {
                RouteLegContext leg = legs.get(i);
                sb.append(String.format("- Chặng %d: %s ➔ %s (%.2f km, ~%d phút)\n",
                        i + 1, leg.fromPlaceName(), leg.toPlaceName(), leg.distanceKm(), leg.durationMinutes()));
            }
        }

        sb.append("""

                ### YÊU CẦU ĐẦU RA:
                Hãy tạo kế hoạch lịch trình chi tiết phân bổ cho các ngày theo cấu trúc JSON:
                {
                  "tripTitle": "Tiêu đề chuyến đi",
                  "tripSummary": "Tóm tắt hành trình",
                  "dailyPlans": [
                    {
                      "dayNumber": 1,
                      "date": "YYYY-MM-DD",
                      "dayTheme": "Chủ đề của ngày",
                      "activities": [
                        {
                          "placeId": "UUID của địa điểm từ danh sách trên (hoặc null nếu là hoạt động ẩm thực/nghỉ ngơi tự do)",
                          "placeName": "Tên địa điểm",
                          "timeSlot": "08:30 - 10:30",
                          "activityType": "VISIT | MEAL | REST | TRANSIT",
                          "description": "Mô tả trải nghiệm",
                          "recommendedDuration": "90 phút",
                          "travelNote": "Lưu ý di chuyển/thời tiết"
                        }
                      ],
                      "mealRecommendations": "Gợi ý bữa trưa và tối",
                      "dayTips": "Lời khuyên kinh nghiệm riêng cho ngày"
                    }
                  ],
                  "generalTips": ["Lời khuyên 1", "Lời khuyên 2"],
                  "estimatedBudgetNote": "Dự toán kinh phí"
                }
                """);

        return sb.toString();
    }
}
