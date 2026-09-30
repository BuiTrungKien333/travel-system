package com.travel.place.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.travel.place.enums.PlaceOpeningStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Schema(description = "Lịch hoạt động của địa điểm trong một ngày trong tuần")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DaySchedule(
        @Schema(description = "Thứ trong tuần (MONDAY đến SUNDAY)")
        DayOfWeek dayOfWeek,

        @Schema(description = "Trạng thái hoạt động (OPEN, CLOSED, UNVERIFIED)")
        PlaceOpeningStatus status,

        @Schema(description = "Giờ mở cửa", example = "08:00:00")
        LocalTime openTime,

        @Schema(description = "Giờ đóng cửa", example = "17:00:00")
        LocalTime closeTime
) {
    public DaySchedule {
        if (status == null) {
            status = (openTime != null && closeTime != null) ? PlaceOpeningStatus.OPEN : PlaceOpeningStatus.UNVERIFIED;
        }
    }

    /**
     * Kiểm tra xem ca làm việc có qua đêm không (ví dụ: 18:00 đến 02:00 hôm sau)
     */
    public boolean isOvernight() {
        return status == PlaceOpeningStatus.OPEN && openTime != null && closeTime != null && openTime.isAfter(closeTime);
    }

    public static DaySchedule open(DayOfWeek dow, LocalTime open, LocalTime close) {
        return new DaySchedule(dow, PlaceOpeningStatus.OPEN, open, close);
    }

    public static DaySchedule closed(DayOfWeek dow) {
        return new DaySchedule(dow, PlaceOpeningStatus.CLOSED, null, null);
    }

    public static DaySchedule unverified(DayOfWeek dow) {
        return new DaySchedule(dow, PlaceOpeningStatus.UNVERIFIED, null, null);
    }
}
