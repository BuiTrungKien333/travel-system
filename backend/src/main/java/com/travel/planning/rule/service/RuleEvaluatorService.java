package com.travel.planning.rule.service;

import com.travel.planning.rule.model.ItineraryEvaluationResult;
import com.travel.planning.rule.model.ScheduleItemContext;

import java.util.List;

/**
 * Service gác cổng (Gatekeeper) thẩm định các quy tắc xác định trong lịch trình du lịch
 */
public interface RuleEvaluatorService {

    /**
     * Thẩm định toàn bộ lịch trình qua các quy tắc xác định (giờ mở cửa, thời tiết ngoài trời, di chuyển)
     *
     * @param items Danh sách các điểm dừng trong lịch trình
     * @return Kết quả thẩm định chi tiết kèm cờ shouldTriggerAi (chỉ bật khi có xung đột)
     */
    ItineraryEvaluationResult evaluateItinerary(List<ScheduleItemContext> items);
}
