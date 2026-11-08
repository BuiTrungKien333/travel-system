package com.travel.planning.ai.copilot.context;

import com.travel.place.enums.PlaceOpeningStatus;
import com.travel.planning.ai.copilot.model.AiCopilotPromptContext;
import com.travel.planning.rule.model.DetectedIssue;
import com.travel.planning.rule.model.ScheduleItemContext;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class AiCopilotContextBuilder {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * System instruction chuẩn cho Gemini Copilot
     */
    public String buildSystemInstruction() {
        return """
                Bạn là Tripzy Travel Copilot - một trợ lý phản biện và đồng hành du lịch ân cần, tinh tế và tôn trọng quyền tự quyết của người dùng (Human-In-The-Loop).
                
                NGUYÊN TẮC HOẠT ĐỘNG:
                1. BẠN KHÔNG TỰ QUYẾT ĐỊNH hay tự ý thay đổi lịch trình của du khách.
                2. BẠN PHÂN TÍCH các vấn đề thực tế (giờ mở/đóng cửa, ca qua đêm, thời tiết mưa bão, thời gian di chuyển quá gấp, địa điểm tràn ngày hoặc va chạm giữa các ngày) đã được hệ thống máy tính phát hiện.
                3. ĐẶC BIỆT LƯU Ý: Địa điểm có trạng thái 'Chưa xác minh được giờ hoạt động' (UNVERIFIED) là chưa có số liệu, không được tự suy đoán là mở cửa hay đóng cửa.
                4. BẠN GIẢI THÍCH các vấn đề bằng ngôn ngữ tiếng Việt tự nhiên, ấm áp, ân cần và dễ hiểu.
                5. BẠN ĐỀ XUẤT TỐI ĐA 3 THẺ HÀNH ĐỘNG (Recommendation Cards) xếp theo thứ tự ưu tiên: CRITICAL -> IMPORTANT -> INFO.
                6. Mỗi thẻ hành động PHẢI có actionType cụ thể (SWAP_PLACES, CHANGE_TIME, REPLACE_PLACE, DISMISS) kèm actionPayload dạng JSON chứa ID địa điểm liên quan để người dùng có thể bấm 'Áp dụng' 1-click.
                
                QUY ĐỊNH BẮT BUỘC VỀ ĐỊNH DẠNG:
                - Chỉ trả về duy nhất một chuỗi JSON hợp lệ 100%, TUYỆT ĐỐI KHÔNG kèm bất kỳ văn bản chào hỏi hay markdown fences nào bên ngoài JSON.
                - Cấu trúc JSON bắt buộc:
                {
                  "greetingAndCaringReminder": "Lời chào ân cần kèm lời nhắc về thời tiết, chuẩn bị ô dù/áo mưa hoặc giữ sức khỏe (khoảng 2-3 câu ngắn)",
                  "recommendationCards": [
                    {
                      "priority": "CRITICAL",
                      "title": "Tiêu đề ngắn gọn phản ánh vấn đề (dưới 10 từ)",
                      "explanation": "Lời giải thích rõ ràng lý do và lợi ích nếu điều chỉnh (2-3 câu)",
                      "actionType": "SWAP_PLACES",
                      "actionButtonText": "Đảo lên trước 16:00",
                      "actionPayload": {
                        "targetPlaceId": "uuid-địa-điểm-cần-xử-lý",
                        "swapWithPlaceId": "uuid-địa-điểm-hoán-đổi"
                      }
                    }
                  ]
                }
                """;
    }

    /**
     * Dựng User Prompt chứa đầy đủ dữ liệu thực tế từ TSP, Weather và Rule Evaluator
     */
    public String buildUserPrompt(AiCopilotPromptContext context) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== THÔNG TIN CHUYẾN ĐI ===\n");
        sb.append(String.format("- Điểm đến: %s\n", context.destination() != null ? context.destination() : "Việt Nam"));
        if (context.tripDate() != null) {
            sb.append(String.format("- Ngày tham quan: %s\n", context.tripDate().format(DATE_FORMAT)));
        }
        sb.append(String.format("- Tình hình thời tiết tổng quan: %s\n", context.weatherOverview()));
        if (context.userNotes() != null && !context.userNotes().isBlank()) {
            sb.append(String.format("- Ghi chú của du khách: %s\n", context.userNotes()));
        }

        sb.append("\n=== LỊCH TRÌNH DỰ KIẾN (ĐÃ TỐI ƯU THEO HELD-KARP) ===\n");
        for (int i = 0; i < context.scheduledItems().size(); i++) {
            ScheduleItemContext item = context.scheduledItems().get(i);
            String arrival = item.arrivalTime() != null ? item.arrivalTime().format(TIME_FORMAT) : "N/A";
            String departure = item.departureTime() != null ? item.departureTime().format(TIME_FORMAT) : "N/A";

            String hoursInfo;
            if (item.openingStatus() == PlaceOpeningStatus.UNVERIFIED) {
                hoursInfo = " (Chưa có số liệu xác minh giờ hoạt động)";
            } else if (item.openingStatus() == PlaceOpeningStatus.CLOSED) {
                hoursInfo = " (Đóng cửa cả ngày)";
            } else if (item.openingTime() != null && item.closingTime() != null) {
                hoursInfo = String.format(" (Mở: %s - Đóng: %s)", item.openingTime().format(TIME_FORMAT), item.closingTime().format(TIME_FORMAT));
            } else {
                hoursInfo = " (Mở cả ngày)";
            }

            String spilloverText = item.isSpillover()
                    ? String.format(" [TRÀN TỪ KẾ HOẠCH NGÀY %s]", item.plannedDate() != null ? item.plannedDate().format(DATE_FORMAT) : "trước")
                    : "";
            String midnightText = item.crossesMidnight() ? " (+1 ngày)" : "";
            String waitingText = item.waitingMinutes() > 0 ? String.format(" (Chờ %d phút trước giờ mở cửa)", item.waitingMinutes()) : "";

            sb.append(String.format("%d. [%s] '%s' (%s)%s%s | Đến: %s -> Rời đi: %s%s%s (Tham quan %d phút) | Di chuyển từ điểm trước: %.1f km (%d phút)\n",
                    i + 1, item.placeId(), item.placeName(), item.placeType(), hoursInfo, spilloverText,
                    arrival, departure, midnightText, waitingText, item.durationMinutes(),
                    item.transitDistanceKmFromPrev(), item.transitMinutesFromPrev()));
        }

        sb.append("\n=== CÁC VẤN ĐỀ VÀ XUNG ĐỘT CỨNG ĐÃ PHÁT HIỆN BỞI RULE EVALUATOR ===\n");
        if (context.detectedIssues().isEmpty()) {
            sb.append("(Không có xung đột cứng nào được phát hiện)\n");
        } else {
            for (int i = 0; i < context.detectedIssues().size(); i++) {
                DetectedIssue issue = context.detectedIssues().get(i);
                sb.append(String.format("- Vấn đề %d [%s - %s]: Tại '%s' (ID: %s)\n",
                        i + 1, issue.category(), issue.severity(), issue.placeName(), issue.placeId()));
                sb.append(String.format("  Lý do: %s\n", issue.reason()));
                sb.append(String.format("  Gợi ý xử lý xác định: %s\n", issue.suggestionHint()));
            }
        }

        sb.append("\nYÊU CẦU CHO COPILOT:\n");
        sb.append("Hãy viết lời chào và nhắc nhở ân cần (đặc biệt về thời tiết, mang áo mưa/ô nếu có mưa), sau đó chọn lọc và xuất tối đa 3 thẻ đề xuất hành động ưu tiên nhất theo đúng định dạng JSON.");

        return sb.toString();
    }
}
