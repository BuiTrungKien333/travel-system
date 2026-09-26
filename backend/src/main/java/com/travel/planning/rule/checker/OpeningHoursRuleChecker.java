package com.travel.planning.rule.checker;

import com.travel.planning.rule.enums.IssueCategory;
import com.travel.planning.rule.enums.IssueSeverity;
import com.travel.planning.rule.model.DetectedIssue;
import com.travel.planning.rule.model.ScheduleItemContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Slf4j(topic = "OPENING_HOURS_RULE")
public class OpeningHoursRuleChecker implements ScheduleRuleChecker {

    @Override
    public List<DetectedIssue> evaluate(List<ScheduleItemContext> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        List<DetectedIssue> issues = new ArrayList<>();

        for (ScheduleItemContext item : items) {
            if (!item.hasFixedHours() || item.arrivalTime() == null) {
                continue;
            }

            LocalTime open = item.openingTime();
            LocalTime close = item.closingTime();
            LocalTime arrival = item.arrivalTime().toLocalTime();
            LocalTime departure = item.departureTime() != null ? item.departureTime().toLocalTime() : arrival;

            // Xử lý khung giờ thông thường (ví dụ: 08:00 - 17:00)
            if (open.isBefore(close)) {
                // Ca 1: Đến sau giờ đóng cửa
                if (arrival.isAfter(close)) {
                    long minutesLate = Duration.between(close, arrival).toMinutes();
                    issues.add(new DetectedIssue(
                            item.placeId(),
                            item.placeName(),
                            item.arrivalTime(),
                            IssueCategory.CLOSING_TIME_CONFLICT,
                            IssueSeverity.CRITICAL,
                            String.format("Địa điểm '%s' đóng cửa lúc %s, nhưng thời gian đến dự kiến là %s (đến muộn %d phút).",
                                    item.placeName(), close, arrival, minutesLate),
                            "Đảo thứ tự tham quan lên sớm hơn trước giờ đóng cửa hoặc thay thế bằng địa điểm mở cửa buổi tối.",
                            Map.of("openingTime", open.toString(), "closingTime", close.toString(), "arrivalTime", arrival.toString())
                    ));
                }
                // Ca 2: Đến trước giờ mở cửa
                else if (arrival.isBefore(open)) {
                    long waitMinutes = Duration.between(arrival, open).toMinutes();
                    IssueSeverity severity = waitMinutes > 30 ? IssueSeverity.IMPORTANT : IssueSeverity.INFO;
                    issues.add(new DetectedIssue(
                            item.placeId(),
                            item.placeName(),
                            item.arrivalTime(),
                            IssueCategory.CLOSING_TIME_CONFLICT,
                            severity,
                            String.format("Thời gian đến dự kiến (%s) trước giờ mở cửa của '%s' (%s). Du khách sẽ phải chờ %d phút.",
                                    arrival, item.placeName(), open, waitMinutes),
                            "Lùi giờ xuất phát hoặc thêm thời gian ăn sáng/nghỉ ngơi trước khi đến.",
                            Map.of("openingTime", open.toString(), "closingTime", close.toString(), "waitMinutes", waitMinutes)
                    ));
                }
                // Ca 3: Đến khi mở cửa nhưng thời gian tham quan vượt quá giờ đóng cửa
                else if (departure.isAfter(close)) {
                    long truncatedMinutes = Duration.between(close, departure).toMinutes();
                    issues.add(new DetectedIssue(
                            item.placeId(),
                            item.placeName(),
                            item.arrivalTime(),
                            IssueCategory.CLOSING_TIME_CONFLICT,
                            IssueSeverity.IMPORTANT,
                            String.format("Thời gian tham quan dự kiến kéo dài đến %s, nhưng '%s' đóng cửa lúc %s (bị cắt ngắn %d phút trải nghiệm).",
                                    departure, item.placeName(), close, truncatedMinutes),
                            String.format("Rút ngắn thời lượng tham quan hoặc đến sớm hơn trước %s để có trọn vẹn %d phút trải nghiệm.",
                                    close.minusMinutes(item.durationMinutes()), item.durationMinutes()),
                            Map.of("openingTime", open.toString(), "closingTime", close.toString(), "departureTime", departure.toString())
                    ));
                }
            }
            // Xử lý khung giờ qua đêm (ví dụ: 18:00 - 02:00 sáng hôm sau)
            else if (open.isAfter(close)) {
                boolean isOpen = !arrival.isBefore(open) || !arrival.isAfter(close);
                if (!isOpen) {
                    issues.add(new DetectedIssue(
                            item.placeId(),
                            item.placeName(),
                            item.arrivalTime(),
                            IssueCategory.CLOSING_TIME_CONFLICT,
                            IssueSeverity.CRITICAL,
                            String.format("Địa điểm '%s' chỉ mở từ %s đến %s hôm sau, thời điểm đến lúc %s đang ngoài giờ hoạt động.",
                                    item.placeName(), open, close, arrival),
                            "Sắp xếp ghé thăm địa điểm này vào khung giờ mở cửa buổi tối.",
                            Map.of("openingTime", open.toString(), "closingTime", close.toString(), "arrivalTime", arrival.toString())
                    ));
                }
            }
        }

        log.debug("[OPENING_HOURS_RULE] Evaluated {} items, found {} issues", items.size(), issues.size());
        return issues;
    }

    @Override
    public String getRuleName() {
        return "OpeningHoursRule";
    }
}
