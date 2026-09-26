package com.travel.planning.rule.model;

import com.travel.place.enums.PlaceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Schema(description = "Thông tin ngữ cảnh của một điểm dừng trong lịch trình cần đánh giá")
public record ScheduleItemContext(
        @Schema(description = "ID địa điểm")
        UUID placeId,

        @Schema(description = "Tên địa điểm", example = "Bảo tàng Điêu khắc Chăm")
        String placeName,

        @Schema(description = "Vĩ độ GPS", example = "16.0592")
        double latitude,

        @Schema(description = "Kinh độ GPS", example = "108.2272")
        double longitude,

        @Schema(description = "Phân loại không gian (INDOOR, OUTDOOR, HYBRID, UNSPECIFIED)")
        PlaceType placeType,

        @Schema(description = "Giờ mở cửa hàng ngày", example = "07:30:00")
        LocalTime openingTime,

        @Schema(description = "Giờ đóng cửa hàng ngày", example = "17:00:00")
        LocalTime closingTime,

        @Schema(description = "Thời điểm đến dự kiến")
        LocalDateTime arrivalTime,

        @Schema(description = "Thời gian lưu lại/tham quan dự kiến (phút)", example = "60")
        int durationMinutes,

        @Schema(description = "Khoảng cách di chuyển từ điểm trước (km)", example = "3.5")
        double transitDistanceKmFromPrev,

        @Schema(description = "Thời gian di chuyển ước tính từ điểm trước (phút)", example = "15")
        int transitMinutesFromPrev
) {
    public ScheduleItemContext {
        if (placeType == null) {
            placeType = PlaceType.UNSPECIFIED;
        }
        if (durationMinutes <= 0) {
            durationMinutes = 60; // Mặc định 60 phút tham quan nếu không truyền
        }
    }

    /**
     * Thời điểm rời đi dự kiến
     */
    public LocalDateTime departureTime() {
        return arrivalTime != null ? arrivalTime.plusMinutes(durationMinutes) : null;
    }

    /**
     * Kiểm tra xem địa điểm có giờ mở/đóng cửa cố định không
     */
    public boolean hasFixedHours() {
        return openingTime != null && closingTime != null;
    }
}
