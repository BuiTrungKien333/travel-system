package com.travel.planning.service;

import com.travel.planning.dto.request.ApplyRecommendationRequest;
import com.travel.planning.dto.request.MultiDayOptimizeAndAuditRequest;
import com.travel.planning.dto.request.OptimizeAndAuditRequest;
import com.travel.planning.dto.response.MultiDayOptimizeAndAuditResponse;
import com.travel.planning.dto.response.TripOptimizationResponse;

/**
 * Service đầu não điều phối toàn diện: Held-Karp TSP + Live Weather + Rule Evaluator + Gemini AI Copilot (HITL)
 */
public interface TripOptimizationService {
    /**
     * Tối ưu hóa thứ tự tham quan bằng Held-Karp cho một ngày, đồng bộ thời tiết, thẩm định quy tắc xác định
     * và kích hoạt AI Copilot phản biện nếu phát hiện xung đột (Silent by default nếu All Clear).
     *
     * @param request Yêu cầu tối ưu và thẩm định lịch trình một ngày
     * @return Kết quả lộ trình chi tiết kèm thẻ đề xuất AI (nếu có xung đột)
     */
    TripOptimizationResponse optimizeAndAuditItinerary(OptimizeAndAuditRequest request);

    /**
     * Tối ưu hóa và thẩm định toàn diện cho chuyến đi nhiều ngày (Multi-day):
     * - Tối ưu từng ngày độc lập
     * - Tự động phát hiện các điểm tràn ngày (spillover)
     * - Kiểm tra va chạm chồng lấn thời gian giữa các ngày liên tiếp
     * - Kích hoạt AI Copilot phản biện nếu có xung đột
     *
     * @param request Kế hoạch nhiều ngày
     * @return Lịch trình từng ngày và cảnh báo va chạm toàn cục
     */
    MultiDayOptimizeAndAuditResponse optimizeAndAuditMultiDay(MultiDayOptimizeAndAuditRequest request);

    /**
     * Thực thi 1-click hành động đề xuất từ AI Copilot (SWAP_PLACES, CHANGE_TIME, REPLACE_PLACE)
     * và tự động tái tính toán lại 100% cự ly, thời gian chặng mới và tái thẩm định lại lộ trình (HITL).
     *
     * @param request Yêu cầu áp dụng đề xuất kèm payload hành động
     * @return Lộ trình mới sau khi cập nhật và kết quả thẩm định lại
     */
    TripOptimizationResponse applyCopilotRecommendation(ApplyRecommendationRequest request);
}
