package com.travel.planning.service;

import com.travel.planning.dto.request.ApplyRecommendationRequest;
import com.travel.planning.dto.request.OptimizeAndAuditRequest;
import com.travel.planning.dto.response.TripOptimizationResponse;

/**
 * Service đầu não điều phối toàn diện: Held-Karp TSP + Live Weather + Rule Evaluator + Gemini AI Copilot (HITL)
 */
public interface TripOptimizationService {

    /**
     * Tối ưu hóa thứ tự tham quan bằng Held-Karp, đồng bộ thời tiết, thẩm định quy tắc xác định
     * và kích hoạt AI Copilot phản biện nếu phát hiện xung đột (Silent by default nếu All Clear).
     *
     * @param request Yêu cầu tối ưu và thẩm định lịch trình
     * @return Kết quả lộ trình chi tiết kèm thẻ đề xuất AI (nếu có xung đột)
     */
    TripOptimizationResponse optimizeAndAuditItinerary(OptimizeAndAuditRequest request);

    /**
     * Thực thi 1-click hành động đề xuất từ AI Copilot (SWAP_PLACES, CHANGE_TIME, REPLACE_PLACE)
     * và tự động tái thẩm định lại lộ trình sau khi cập nhật (Human-In-The-Loop).
     *
     * @param request Yêu cầu áp dụng đề xuất kèm payload hành động
     * @return Lộ trình mới sau khi cập nhật và kết quả thẩm định lại
     */
    TripOptimizationResponse applyCopilotRecommendation(ApplyRecommendationRequest request);
}
