package com.travel.planning.timeline;

import com.travel.place.entity.Place;
import com.travel.place.enums.PlaceOpeningStatus;
import com.travel.place.enums.PlaceType;
import com.travel.place.model.DaySchedule;
import com.travel.place.model.WeeklyOpeningHours;
import com.travel.planning.algorithm.DistanceCalculator;
import com.travel.planning.dto.request.TspLocationRequest;
import com.travel.planning.enums.VisitStatus;
import com.travel.planning.rule.model.ScheduleItemContext;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j(topic = "TIMELINE_ENGINE")
public class TimelineEngine {

    /**
     * Dựng trục thời gian chi tiết cho một ngày từ danh sách địa điểm đã được Held-Karp sắp xếp
     */
    public List<ScheduleItemContext> buildTimelineForDay(
        LocalDate dayDate,
        LocalTime startTime,
        TspLocationRequest originPlace,
        List<Place> orderedPlaces,
        Map<UUID, TspLocationRequest> requestMap
    ) {
        return buildTimelineForDay(dayDate, startTime, originPlace, orderedPlaces, requestMap, null);
    }

    /**
     * Dựng trục thời gian chi tiết cho một ngày kèm danh sách RouteLeg từ thuật toán định tuyến
     */
    public List<ScheduleItemContext> buildTimelineForDay(
        LocalDate dayDate,
        LocalTime startTime,
        TspLocationRequest originPlace,
        List<Place> orderedPlaces,
        Map<UUID, TspLocationRequest> requestMap,
        List<com.travel.planning.algorithm.model.RouteLeg> legs
    ) {
        if (orderedPlaces == null || orderedPlaces.isEmpty()) {
            return List.of();
        }

        List<ScheduleItemContext> scheduleItems = new ArrayList<>();
        LocalDateTime originDepartureTime = dayDate.atTime(startTime);
        LocalDateTime currentTime = originDepartureTime;

        for (int i = 0; i < orderedPlaces.size(); i++) {
            Place p = orderedPlaces.get(i);
            TspLocationRequest locReq = requestMap != null ? requestMap.get(p.getId()) : null;

            int duration =
                locReq != null && locReq.durationMinutes() != null && locReq.durationMinutes() > 0
                    ? locReq.durationMinutes()
                    : 60;

            VisitStatus visitStatus =
                locReq != null && locReq.visitStatus() != null ? locReq.visitStatus() : VisitStatus.NOT_VISITED;

            WeeklyOpeningHours weeklyHours =
                locReq != null && locReq.weeklyOpeningHours() != null ? locReq.weeklyOpeningHours() : null;

            double distFromPrev;
            int transitFromPrev;
            LocalDateTime arrivalTime;

            if (i == 0) {
                // Điểm tham quan đầu tiên
                if (originPlace != null) {
                    if (legs != null && !legs.isEmpty()) {
                        com.travel.planning.algorithm.model.RouteLeg leg = legs.get(0);
                        distFromPrev = Math.round((leg.getDistanceMeters() / 1000.0) * 10.0) / 10.0;
                        transitFromPrev = (int) Math.ceil(leg.getDurationSeconds() / 60.0);
                    } else {
                        double distMeters = DistanceCalculator.calculateDistanceMeters(
                            originPlace.latitude(),
                            originPlace.longitude(),
                            p.getLatitude(),
                            p.getLongitude()
                        );
                        distFromPrev = Math.round((distMeters / 1000.0) * 10.0) / 10.0;
                        transitFromPrev = (int) Math.ceil(
                            DistanceCalculator.calculateDurationSeconds(distMeters) / 60.0
                        );
                    }
                    arrivalTime = originDepartureTime.plusMinutes(transitFromPrev);
                } else {
                    distFromPrev = 0.0;
                    transitFromPrev = 0;
                    arrivalTime = originDepartureTime;
                }
            } else {
                int legIndex = originPlace != null ? i : i - 1;
                if (legs != null && legIndex >= 0 && legIndex < legs.size()) {
                    com.travel.planning.algorithm.model.RouteLeg leg = legs.get(legIndex);
                    distFromPrev = Math.round((leg.getDistanceMeters() / 1000.0) * 10.0) / 10.0;
                    transitFromPrev = (int) Math.ceil(leg.getDurationSeconds() / 60.0);
                } else {
                    Place prevPlace = orderedPlaces.get(i - 1);
                    double distMeters = DistanceCalculator.calculateDistanceMeters(
                        prevPlace.getLatitude(),
                        prevPlace.getLongitude(),
                        p.getLatitude(),
                        p.getLongitude()
                    );
                    distFromPrev = Math.round((distMeters / 1000.0) * 10.0) / 10.0;
                    transitFromPrev = (int) Math.ceil(DistanceCalculator.calculateDurationSeconds(distMeters) / 60.0);
                }
                arrivalTime = currentTime.plusMinutes(transitFromPrev);
            }

            // Tính toán ngày hiển thị và thời gian chờ
            LocalDate displayDate = arrivalTime.toLocalDate();
            boolean isSpillover = displayDate.isAfter(dayDate);
            LocalDateTime departureTime = arrivalTime.plusMinutes(duration);
            boolean crossesMidnight = departureTime.toLocalDate().isAfter(displayDate);

            // Kiểm tra trạng thái giờ hoạt động và thời gian chờ
            ScheduleTimingInfo timingInfo = evaluateOpeningStatusAndWaiting(arrivalTime, p, weeklyHours);

            currentTime = departureTime;

            scheduleItems.add(
                new ScheduleItemContext(
                    p.getId(),
                    p.getName(),
                    p.getLatitude(),
                    p.getLongitude(),
                    p.getPlaceType() != null ? p.getPlaceType() : PlaceType.UNSPECIFIED,
                    dayDate,
                    displayDate,
                    isSpillover,
                    arrivalTime,
                    duration,
                    crossesMidnight,
                    timingInfo.waitingMinutes(),
                    timingInfo.status(),
                    timingInfo.effectiveOpen(),
                    timingInfo.effectiveClose(),
                    weeklyHours,
                    distFromPrev,
                    transitFromPrev,
                    visitStatus
                )
            );
        }

        return scheduleItems;
    }

    /**
     * Tái tính toán 100% cự ly, thời gian chặng di chuyển và trục thời gian cho danh sách ScheduleItemContext
     * (Sử dụng sau khi hoán đổi thứ tự SWAP_PLACES hoặc chỉnh sửa lịch trình)
     */
    public List<ScheduleItemContext> recalculateScheduleTimeline(
        List<ScheduleItemContext> items,
        TspLocationRequest originPlace,
        LocalDate dayDate,
        LocalTime startTime
    ) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        List<ScheduleItemContext> recalculated = new ArrayList<>();
        LocalDateTime originDeparture = dayDate.atTime(startTime);
        LocalDateTime currentTime = originDeparture;

        for (int i = 0; i < items.size(); i++) {
            ScheduleItemContext curr = items.get(i);
            double distFromPrev;
            int transitFromPrev;
            LocalDateTime arrivalTime;

            if (i == 0) {
                if (originPlace != null) {
                    double distMeters = DistanceCalculator.calculateDistanceMeters(
                        originPlace.latitude(),
                        originPlace.longitude(),
                        curr.latitude(),
                        curr.longitude()
                    );
                    distFromPrev = Math.round((distMeters / 1000.0) * 10.0) / 10.0;
                    transitFromPrev = (int) Math.ceil(DistanceCalculator.calculateDurationSeconds(distMeters) / 60.0);
                    arrivalTime = originDeparture.plusMinutes(transitFromPrev);
                } else {
                    distFromPrev = 0.0;
                    transitFromPrev = 0;
                    arrivalTime = originDeparture;
                }
            } else {
                ScheduleItemContext prev = items.get(i - 1);
                double distMeters = DistanceCalculator.calculateDistanceMeters(
                    prev.latitude(),
                    prev.longitude(),
                    curr.latitude(),
                    curr.longitude()
                );
                distFromPrev = Math.round((distMeters / 1000.0) * 10.0) / 10.0;
                transitFromPrev = (int) Math.ceil(DistanceCalculator.calculateDurationSeconds(distMeters) / 60.0);
                arrivalTime = currentTime.plusMinutes(transitFromPrev);
            }

            LocalDate displayDate = arrivalTime.toLocalDate();
            boolean isSpillover = displayDate.isAfter(curr.plannedDate() != null ? curr.plannedDate() : dayDate);
            LocalDateTime departureTime = arrivalTime.plusMinutes(curr.durationMinutes());
            boolean crossesMidnight = departureTime.toLocalDate().isAfter(displayDate);

            // Tái đánh giá giờ mở cửa và thời gian chờ
            ScheduleTimingInfo timingInfo = evaluateOpeningStatusAndWaiting(arrivalTime, curr);

            currentTime = departureTime;

            recalculated.add(
                new ScheduleItemContext(
                    curr.placeId(),
                    curr.placeName(),
                    curr.latitude(),
                    curr.longitude(),
                    curr.placeType(),
                    curr.plannedDate() != null ? curr.plannedDate() : dayDate,
                    displayDate,
                    isSpillover,
                    arrivalTime,
                    curr.durationMinutes(),
                    crossesMidnight,
                    timingInfo.waitingMinutes(),
                    timingInfo.status(),
                    timingInfo.effectiveOpen(),
                    timingInfo.effectiveClose(),
                    curr.weeklyOpeningHours(),
                    distFromPrev,
                    transitFromPrev,
                    curr.visitStatus()
                )
            );
        }

        return recalculated;
    }

    private ScheduleTimingInfo evaluateOpeningStatusAndWaiting(
        LocalDateTime arrivalTime,
        Place p,
        WeeklyOpeningHours weeklyHours
    ) {
        if (weeklyHours != null) {
            DaySchedule schedule = weeklyHours.findEffectiveSchedule(arrivalTime);
            return calculateTiming(
                arrivalTime,
                schedule.status(),
                schedule.openTime(),
                schedule.closeTime(),
                schedule.isOvernight()
            );
        }

        // Nếu có giờ mở đóng hàng ngày trên Place
        if (p.getOpeningTime() != null && p.getClosingTime() != null) {
            boolean isOvernight = p.getOpeningTime().isAfter(p.getClosingTime());
            return calculateTiming(
                arrivalTime,
                PlaceOpeningStatus.OPEN,
                p.getOpeningTime(),
                p.getClosingTime(),
                isOvernight
            );
        }

        return new ScheduleTimingInfo(PlaceOpeningStatus.UNVERIFIED, null, null, 0);
    }

    private ScheduleTimingInfo evaluateOpeningStatusAndWaiting(LocalDateTime arrivalTime, ScheduleItemContext item) {
        if (item.weeklyOpeningHours() != null) {
            DaySchedule schedule = item.weeklyOpeningHours().findEffectiveSchedule(arrivalTime);
            return calculateTiming(
                arrivalTime,
                schedule.status(),
                schedule.openTime(),
                schedule.closeTime(),
                schedule.isOvernight()
            );
        }

        if (item.openingTime() != null && item.closingTime() != null) {
            boolean isOvernight = item.openingTime().isAfter(item.closingTime());
            return calculateTiming(
                arrivalTime,
                item.openingStatus() != null ? item.openingStatus() : PlaceOpeningStatus.OPEN,
                item.openingTime(),
                item.closingTime(),
                isOvernight
            );
        }

        return new ScheduleTimingInfo(PlaceOpeningStatus.UNVERIFIED, null, null, 0);
    }

    private ScheduleTimingInfo calculateTiming(
        LocalDateTime arrivalTime,
        PlaceOpeningStatus status,
        LocalTime open,
        LocalTime close,
        boolean isOvernight
    ) {
        if (status != PlaceOpeningStatus.OPEN || open == null || close == null) {
            return new ScheduleTimingInfo(status != null ? status : PlaceOpeningStatus.UNVERIFIED, open, close, 0);
        }

        int waitingMinutes = 0;
        LocalTime arrivalTimeOnly = arrivalTime.toLocalTime();

        if (!isOvernight) {
            // Ca thông thường (ví dụ: 08:00 - 17:00)
            if (arrivalTimeOnly.isBefore(open)) {
                waitingMinutes = (int) Duration.between(arrivalTimeOnly, open).toMinutes();
            }
        } else {
            // Ca qua đêm (ví dụ: 18:00 - 02:00)
            // Nếu đến trong khoảng [close, open), tức là đóng cửa và phải chờ đến open
            if (arrivalTimeOnly.isAfter(close) && arrivalTimeOnly.isBefore(open)) {
                waitingMinutes = (int) Duration.between(arrivalTimeOnly, open).toMinutes();
            }
        }

        return new ScheduleTimingInfo(status, open, close, waitingMinutes);
    }

    private record ScheduleTimingInfo(
        PlaceOpeningStatus status,
        LocalTime effectiveOpen,
        LocalTime effectiveClose,
        int waitingMinutes
    ) {}
}
