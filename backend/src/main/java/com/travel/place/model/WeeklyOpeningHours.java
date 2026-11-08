package com.travel.place.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.Map;

@Schema(description = "Lịch hoạt động cả tuần của địa điểm (Thứ Hai đến Chủ Nhật)")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WeeklyOpeningHours(
        @Schema(description = "Bản đồ lịch hoạt động theo từng thứ trong tuần")
        Map<DayOfWeek, DaySchedule> schedules
) {
    public WeeklyOpeningHours {
        schedules = schedules != null ? new EnumMap<>(schedules) : new EnumMap<>(DayOfWeek.class);
    }

    public DaySchedule getSchedule(DayOfWeek dayOfWeek) {
        if (schedules == null || dayOfWeek == null) {
            return DaySchedule.unverified(dayOfWeek);
        }
        return schedules.getOrDefault(dayOfWeek, DaySchedule.unverified(dayOfWeek));
    }

    /**
     * Tìm ca hoạt động có hiệu lực tại thời điểm dateTime.
     * Xét cả ca qua đêm bắt đầu từ ngày hôm trước.
     */
    public DaySchedule findEffectiveSchedule(LocalDateTime dateTime) {
        if (dateTime == null) {
            return DaySchedule.unverified(DayOfWeek.MONDAY);
        }

        LocalDate date = dateTime.toLocalDate();
        LocalTime time = dateTime.toLocalTime();
        DayOfWeek today = date.getDayOfWeek();
        DayOfWeek yesterday = today.minus(1);

        // 1. Kiểm tra xem ca qua đêm của ngày hôm trước có đang tiếp diễn vào rạng sáng hôm nay không
        DaySchedule yesterdaySchedule = getSchedule(yesterday);
        if (yesterdaySchedule.isOvernight()) {
            // Ví dụ hôm qua mở 18:00 - 02:00 sáng hôm sau. Nếu time < 02:00 -> thuộc ca hôm qua
            if (time.isBefore(yesterdaySchedule.closeTime())) {
                return yesterdaySchedule;
            }
        }

        // 2. Trả về lịch của ngày hôm nay
        return getSchedule(today);
    }
}
