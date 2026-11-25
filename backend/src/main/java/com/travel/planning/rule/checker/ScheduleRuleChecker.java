package com.travel.planning.rule.checker;

import com.travel.planning.rule.model.DetectedIssue;
import com.travel.planning.rule.model.ScheduleItemContext;
import java.util.List;

/**
 * Hợp đồng chung cho các bộ thẩm định quy tắc xác định trong lịch trình
 */
public interface ScheduleRuleChecker {
    /**
     * Thẩm định danh sách các điểm trong lịch trình và trả về các vấn đề phát hiện được
     *
     * @param items Danh sách các điểm dừng trong lịch trình theo thứ tự thời gian
     * @return Danh sách các xung đột hoặc vấn đề phát hiện (rỗng nếu hoàn toàn hợp lệ)
     */
    List<DetectedIssue> evaluate(List<ScheduleItemContext> items);

    /**
     * Tên định danh của quy tắc (ví dụ: "OpeningHoursRule", "OutdoorWeatherRule")
     */
    String getRuleName();
}
