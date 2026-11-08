package com.travel.planning.rule.checker;

import com.travel.place.enums.PlaceOpeningStatus;
import com.travel.place.model.DaySchedule;
import com.travel.planning.rule.enums.IssueCategory;
import com.travel.planning.rule.enums.IssueSeverity;
import com.travel.planning.rule.model.DetectedIssue;
import com.travel.planning.rule.model.ScheduleItemContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Slf4j(topic = "OPENING_HOURS_RULE")
public class OpeningHoursRuleChecker implements ScheduleRuleChecker {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public List<DetectedIssue> evaluate(List<ScheduleItemContext> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        List<DetectedIssue> issues = new ArrayList<>();

        for (ScheduleItemContext item : items) {
            if (item.arrivalTime() == null) {
                continue;
            }

            // 1. Kiểm tra trường hợp CHƯA CÓ SỐ LIỆU (UNVERIFIED)
            if (item.openingStatus() == PlaceOpeningStatus.UNVERIFIED) {
                issues.add(new DetectedIssue(
                        item.placeId(),
                        item.placeName(),
                        item.arrivalTime(),
                        IssueCategory.UNVERIFIED_HOURS,
                        IssueSeverity.INFO,
                        String.format("Địa điểm '%s' chưa có số liệu xác minh giờ hoạt động vào ngày %s.",
                                item.placeName(), item.displayDate()),
                        "Chưa xác minh được giờ hoạt động. Du khách nên kiểm tra lại thông tin trên trang chính thức hoặc liên hệ trước khi đến.",
                        Map.of("status", PlaceOpeningStatus.UNVERIFIED.name())
                ));
                continue;
            }

            // 2. Kiểm tra trường hợp ĐÓNG CỬA (CLOSED)
            if (item.openingStatus() == PlaceOpeningStatus.CLOSED) {
                issues.add(new DetectedIssue(
                        item.placeId(),
                        item.placeName(),
                        item.arrivalTime(),
                        IssueCategory.CLOSING_TIME_CONFLICT,
                        IssueSeverity.CRITICAL,
                        String.format("Địa điểm '%s' đóng cửa cả ngày vào %s (%s).",
                                item.placeName(), item.displayDate().getDayOfWeek(), item.displayDate()),
                        "Thay thế bằng địa điểm khác hoặc dời lịch tham quan sang ngày có mở cửa.",
                        Map.of("status", PlaceOpeningStatus.CLOSED.name(), "date", item.displayDate().toString())
                ));
                continue;
            }

            // 3. Kiểm tra trường hợp MỞ CỬA (OPEN)
            // Lấy khung giờ hoạt động hiệu lực tại thời điểm đến
            LocalTime open = item.openingTime();
            LocalTime close = item.closingTime();
            boolean isOvernight = false;

            if (item.weeklyOpeningHours() != null) {
                DaySchedule effectiveSchedule = item.weeklyOpeningHours().findEffectiveSchedule(item.arrivalTime());
                if (effectiveSchedule.status() == PlaceOpeningStatus.CLOSED) {
                    issues.add(new DetectedIssue(
                            item.placeId(),
                            item.placeName(),
                            item.arrivalTime(),
                            IssueCategory.CLOSING_TIME_CONFLICT,
                            IssueSeverity.CRITICAL,
                            String.format("Địa điểm '%s' đóng cửa vào %s.", item.placeName(), item.arrivalTime().getDayOfWeek()),
                            "Thay đổi thứ tự hoặc dời sang ngày khác.",
                            Map.of("status", PlaceOpeningStatus.CLOSED.name())
                    ));
                    continue;
                } else if (effectiveSchedule.status() == PlaceOpeningStatus.UNVERIFIED) {
                    issues.add(new DetectedIssue(
                            item.placeId(),
                            item.placeName(),
                            item.arrivalTime(),
                            IssueCategory.UNVERIFIED_HOURS,
                            IssueSeverity.INFO,
                            String.format("Địa điểm '%s' chưa xác minh được giờ hoạt động cho %s.", item.placeName(), item.arrivalTime().getDayOfWeek()),
                            "Nên kiểm tra lại trước khi đến.",
                            Map.of("status", PlaceOpeningStatus.UNVERIFIED.name())
                    ));
                    continue;
                }
                open = effectiveSchedule.openTime();
                close = effectiveSchedule.closeTime();
                isOvernight = effectiveSchedule.isOvernight();
            } else if (open != null && close != null) {
                isOvernight = open.isAfter(close);
            }

            if (open == null || close == null) {
                continue;
            }

            LocalTime arrival = item.arrivalTime().toLocalTime();
            LocalTime departure = item.departureTime() != null ? item.departureTime().toLocalTime() : arrival;

            // Xử lý khung giờ thông thường trong ngày (ví dụ: 08:00 - 17:00)
            if (!isOvernight) {
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
                                    item.placeName(), close.format(TIME_FORMAT), arrival.format(TIME_FORMAT), minutesLate),
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
                                    arrival.format(TIME_FORMAT), item.placeName(), open.format(TIME_FORMAT), waitMinutes),
                            "Lùi giờ xuất phát hoặc thêm thời gian nghỉ ngơi trước khi đến.",
                            Map.of("openingTime", open.toString(), "closingTime", close.toString(), "waitMinutes", waitMinutes)
                    ));
                }
                // Ca 3: Đến khi mở cửa nhưng thời gian tham quan vượt quá giờ đóng cửa (bị cắt ngắn)
                else if (departure.isAfter(close)) {
                    long truncatedMinutes = Duration.between(close, departure).toMinutes();
                    issues.add(new DetectedIssue(
                            item.placeId(),
                            item.placeName(),
                            item.arrivalTime(),
                            IssueCategory.CLOSING_TIME_CONFLICT,
                            IssueSeverity.IMPORTANT,
                            String.format("Thời gian tham quan dự kiến kéo dài đến %s, nhưng '%s' đóng cửa lúc %s (bị cắt ngắn %d phút trải nghiệm).",
                                    departure.format(TIME_FORMAT), item.placeName(), close.format(TIME_FORMAT), truncatedMinutes),
                            String.format("Rút ngắn thời lượng tham quan hoặc đến sớm hơn trước %s để có trọn vẹn %d phút trải nghiệm.",
                                    close.minusMinutes(item.durationMinutes()).format(TIME_FORMAT), item.durationMinutes()),
                            Map.of("openingTime", open.toString(), "closingTime", close.toString(), "departureTime", departure.toString())
                    ));
                }
            }
            // Xử lý ca qua đêm (ví dụ: Thứ Bảy 18:00 - Chủ Nhật 02:00)
            else {
                // Đang ngoài giờ mở cửa nếu rơi vào ban ngày giữa [close, open) (ví dụ 02:00 -> 18:00)
                boolean isOpen = arrival.isAfter(open) || arrival.isBefore(close);
                if (!isOpen) {
                    issues.add(new DetectedIssue(
                            item.placeId(),
                            item.placeName(),
                            item.arrivalTime(),
                            IssueCategory.CLOSING_TIME_CONFLICT,
                            IssueSeverity.CRITICAL,
                            String.format("Địa điểm '%s' chỉ mở từ %s đến %s hôm sau, thời điểm đến lúc %s đang ngoài giờ hoạt động.",
                                    item.placeName(), open.format(TIME_FORMAT), close.format(TIME_FORMAT), arrival.format(TIME_FORMAT)),
                            "Sắp xếp ghé thăm địa điểm này vào khung giờ mở cửa buổi tối.",
                            Map.of("openingTime", open.toString(), "closingTime", close.toString(), "arrivalTime", arrival.toString())
                    ));
                } else if (arrival.isBefore(close) && departure.isAfter(close)) {
                    // Đến trước lúc đóng cửa rạng sáng nhưng tham quan quá giờ đóng cửa
                    long truncatedMinutes = Duration.between(close, departure).toMinutes();
                    issues.add(new DetectedIssue(
                            item.placeId(),
                            item.placeName(),
                            item.arrivalTime(),
                            IssueCategory.CLOSING_TIME_CONFLICT,
                            IssueSeverity.IMPORTANT,
                            String.format("Thời gian tham quan tại '%s' kéo dài đến %s, nhưng ca qua đêm kết thúc lúc %s (bị cắt ngắn %d phút).",
                                    item.placeName(), departure.format(TIME_FORMAT), close.format(TIME_FORMAT), truncatedMinutes),
                            "Đến sớm hơn trong ca qua đêm để trọn vẹn trải nghiệm.",
                            Map.of("openingTime", open.toString(), "closingTime", close.toString(), "departureTime", departure.toString())
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
