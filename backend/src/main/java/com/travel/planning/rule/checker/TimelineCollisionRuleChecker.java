package com.travel.planning.rule.checker;

import com.travel.planning.dto.response.DayScheduleResponse;
import com.travel.planning.rule.enums.IssueCategory;
import com.travel.planning.rule.enums.IssueSeverity;
import com.travel.planning.rule.model.DetectedIssue;
import com.travel.planning.rule.model.ScheduleItemContext;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j(topic = "TIMELINE_COLLISION_RULE")
public class TimelineCollisionRuleChecker {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm, dd/MM");
    private static final long MIN_RECOMMENDED_REST_HOURS = 6;

    /**
     * Kiểm tra va chạm chồng lấn thời gian giữa các ngày kế tiếp nhau trong chuyến đi nhiều ngày
     */
    public List<DetectedIssue> evaluateCrossDayCollisions(List<DayScheduleResponse> dailySchedules) {
        if (dailySchedules == null || dailySchedules.size() < 2) {
            return Collections.emptyList();
        }

        List<DetectedIssue> issues = new ArrayList<>();

        for (int i = 0; i < dailySchedules.size() - 1; i++) {
            DayScheduleResponse currentDay = dailySchedules.get(i);
            DayScheduleResponse nextDay = dailySchedules.get(i + 1);

            List<ScheduleItemContext> currentItems = currentDay.scheduleItems();
            List<ScheduleItemContext> nextItems = nextDay.scheduleItems();

            if (currentItems == null || currentItems.isEmpty() || nextItems == null || nextItems.isEmpty()) {
                continue;
            }

            ScheduleItemContext lastCurrentItem = currentItems.getLast();
            ScheduleItemContext firstNextItem = nextItems.getFirst();

            LocalDateTime lastCurrentDeparture = lastCurrentItem.departureTime();
            LocalDateTime firstNextArrival = firstNextItem.arrivalTime();

            if (lastCurrentDeparture == null || firstNextArrival == null) {
                continue;
            }

            // Ca 1: Chồng lấn trực tiếp (lịch ngày trước kết thúc sau khi lịch ngày sau đã bắt đầu)
            if (lastCurrentDeparture.isAfter(firstNextArrival)) {
                long overlapMinutes = Duration.between(firstNextArrival, lastCurrentDeparture).toMinutes();
                issues.add(
                    new DetectedIssue(
                        lastCurrentItem.placeId(),
                        String.format("Va chạm lịch: Ngày %s và Ngày %s", currentDay.dayDate(), nextDay.dayDate()),
                        firstNextArrival,
                        IssueCategory.SCHEDULE_COLLISION,
                        IssueSeverity.CRITICAL,
                        String.format(
                            "Lịch trình ngày %s kết thúc lúc %s (tại '%s'), đè lên giờ bắt đầu ngày %s (đến '%s' lúc %s), bị chồng lấn %d phút.",
                            currentDay.dayDate(),
                            lastCurrentDeparture.format(TIME_FORMAT),
                            lastCurrentItem.placeName(),
                            nextDay.dayDate(),
                            firstNextItem.placeName(),
                            firstNextArrival.format(TIME_FORMAT),
                            overlapMinutes
                        ),
                        String.format(
                            "Lùi giờ xuất phát của ngày %s sang sau %s hoặc tinh giản bớt điểm tham quan ngày %s.",
                            nextDay.dayDate(),
                            lastCurrentDeparture.format(TIME_FORMAT),
                            currentDay.dayDate()
                        ),
                        Map.of(
                            "prevDayLastDeparture",
                            lastCurrentDeparture.toString(),
                            "nextDayFirstArrival",
                            firstNextArrival.toString(),
                            "overlapMinutes",
                            overlapMinutes
                        )
                    )
                );
            }
            // Ca 2: Khoảng cách nghỉ ngơi giữa 2 ngày quá ngắn (dưới 6 tiếng)
            else {
                long restMinutes = Duration.between(lastCurrentDeparture, firstNextArrival).toMinutes();
                long restHours = restMinutes / 60;
                if (restHours < MIN_RECOMMENDED_REST_HOURS) {
                    issues.add(
                        new DetectedIssue(
                            firstNextItem.placeId(),
                            String.format("Nghỉ ngơi giữa Ngày %s và Ngày %s", currentDay.dayDate(), nextDay.dayDate()),
                            firstNextArrival,
                            IssueCategory.INSUFFICIENT_REST_TIME,
                            IssueSeverity.IMPORTANT,
                            String.format(
                                "Khoảng cách giữa lúc kết thúc ngày %s (%s) và lúc bắt đầu ngày %s (%s) chỉ có %d giờ %d phút (dưới mức tối thiểu %d giờ).",
                                currentDay.dayDate(),
                                lastCurrentDeparture.format(TIME_FORMAT),
                                nextDay.dayDate(),
                                firstNextArrival.format(TIME_FORMAT),
                                restHours,
                                restMinutes % 60,
                                MIN_RECOMMENDED_REST_HOURS
                            ),
                            String.format(
                                "Nên lùi giờ xuất phát ngày %s để du khách có đủ giấc ngủ và phục hồi năng lượng.",
                                nextDay.dayDate()
                            ),
                            Map.of(
                                "restHours",
                                restHours,
                                "restMinutes",
                                restMinutes,
                                "minRecommendedHours",
                                MIN_RECOMMENDED_REST_HOURS
                            )
                        )
                    );
                }
            }
        }

        log.debug(
            "[TIMELINE_COLLISION_RULE] Evaluated {} day transitions, found {} collision issues",
            dailySchedules.size() - 1,
            issues.size()
        );
        return issues;
    }
}
