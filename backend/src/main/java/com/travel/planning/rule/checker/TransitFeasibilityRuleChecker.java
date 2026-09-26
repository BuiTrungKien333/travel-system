package com.travel.planning.rule.checker;

import com.travel.planning.rule.enums.IssueCategory;
import com.travel.planning.rule.enums.IssueSeverity;
import com.travel.planning.rule.model.DetectedIssue;
import com.travel.planning.rule.model.ScheduleItemContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Slf4j(topic = "TRANSIT_FEASIBILITY_RULE")
public class TransitFeasibilityRuleChecker implements ScheduleRuleChecker {

    private static final int MAX_RECOMMENDED_PLACES_PER_DAY = 6;

    @Override
    public List<DetectedIssue> evaluate(List<ScheduleItemContext> items) {
        if (items == null || items.size() < 2) {
            return Collections.emptyList();
        }

        List<DetectedIssue> issues = new ArrayList<>();

        for (int i = 1; i < items.size(); i++) {
            ScheduleItemContext prev = items.get(i - 1);
            ScheduleItemContext curr = items.get(i);

            if (prev.departureTime() == null || curr.arrivalTime() == null) {
                continue;
            }

            Duration scheduledGap = Duration.between(prev.departureTime(), curr.arrivalTime());
            long availableMinutes = scheduledGap.toMinutes();
            int requiredMinutes = curr.transitMinutesFromPrev();

            // Ca 1: Thời gian phân bổ ít hơn thời gian di chuyển thực tế (Bất khả thi)
            if (availableMinutes < requiredMinutes) {
                long deficit = requiredMinutes - availableMinutes;
                issues.add(new DetectedIssue(
                        curr.placeId(),
                        curr.placeName(),
                        curr.arrivalTime(),
                        IssueCategory.TIGHT_TRANSIT_TIME,
                        IssueSeverity.CRITICAL,
                        String.format("Thời gian di chuyển từ '%s' sang '%s' không đủ (khoảng cách %.1f km cần tối thiểu %d phút, nhưng lịch trình chỉ dành %d phút, thiếu %d phút).",
                                prev.placeName(), curr.placeName(), curr.transitDistanceKmFromPrev(), requiredMinutes, Math.max(0, availableMinutes), deficit),
                        String.format("Tăng khoảng đệm di chuyển lên ít nhất %d phút hoặc dời giờ đến '%s' sang %s.",
                                requiredMinutes, curr.placeName(), prev.departureTime().plusMinutes(requiredMinutes).toLocalTime()),
                        Map.of(
                                "fromPlace", prev.placeName(),
                                "toPlace", curr.placeName(),
                                "requiredMinutes", requiredMinutes,
                                "availableMinutes", availableMinutes,
                                "distanceKm", curr.transitDistanceKmFromPrev()
                        )
                ));
            }
            // Ca 2: Khoảng đệm quá sát (dư dưới 5 phút cho chặng di chuyển dài trên 20 phút)
            else if (requiredMinutes >= 20 && (availableMinutes - requiredMinutes) < 5) {
                issues.add(new DetectedIssue(
                        curr.placeId(),
                        curr.placeName(),
                        curr.arrivalTime(),
                        IssueCategory.TIGHT_TRANSIT_TIME,
                        IssueSeverity.IMPORTANT,
                        String.format("Khoảng đệm di chuyển giữa '%s' và '%s' rất sát (chỉ dư %d phút), nguy cơ bị trễ nếu kẹt xe hoặc phát sinh sự cố.",
                                prev.placeName(), curr.placeName(), availableMinutes - requiredMinutes),
                        "Nên cộng thêm 10-15 phút dự phòng di chuyển để lịch trình không bị gấp gáp.",
                        Map.of(
                                "fromPlace", prev.placeName(),
                                "toPlace", curr.placeName(),
                                "bufferMinutes", availableMinutes - requiredMinutes
                        )
                ));
            }
        }

        // Kiểm tra quá tải số lượng điểm tham quan trong ngày
        if (items.size() > MAX_RECOMMENDED_PLACES_PER_DAY) {
            ScheduleItemContext lastItem = items.getLast();
            issues.add(new DetectedIssue(
                    null,
                    "Toàn bộ ngày tham quan",
                    lastItem.arrivalTime(),
                    IssueCategory.OVERLOADED_SCHEDULE,
                    IssueSeverity.INFO,
                    String.format("Lịch trình có %d địa điểm trong 1 ngày, mật độ di chuyển dày đặc có thể gây mệt mỏi.", items.size()),
                    "Cân nhắc tinh giản bớt 1-2 điểm tham quan phụ để chuyến đi thong thả và thoải mái hơn.",
                    Map.of("totalPlaces", items.size(), "maxRecommended", MAX_RECOMMENDED_PLACES_PER_DAY)
            ));
        }

        log.debug("[TRANSIT_FEASIBILITY_RULE] Evaluated {} items, found {} transit issues", items.size(), issues.size());
        return issues;
    }

    @Override
    public String getRuleName() {
        return "TransitFeasibilityRule";
    }
}
