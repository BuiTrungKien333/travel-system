package com.travel.planning.rule.model;

import com.travel.place.enums.PlaceOpeningStatus;
import com.travel.place.enums.PlaceType;
import com.travel.place.model.WeeklyOpeningHours;
import com.travel.planning.enums.VisitStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
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

        @Schema(description = "Ngày kế hoạch đầu vào được người dùng gán", example = "2026-10-01")
        LocalDate plannedDate,

        @Schema(description = "Ngày hiển thị thực tế (xác định bởi arrivalTime)", example = "2026-10-01")
        LocalDate displayDate,

        @Schema(description = "Lượt ghé có bị tràn sang từ kế hoạch của ngày trước không", example = "false")
        boolean isSpillover,

        @Schema(description = "Thời điểm đến dự kiến đầy đủ ngày giờ")
        LocalDateTime arrivalTime,

        @Schema(description = "Thời gian lưu lại/tham quan dự kiến (phút)", example = "60")
        int durationMinutes,

        @Schema(description = "Lượt ghé có kéo dài qua nửa đêm sang ngày hôm sau không", example = "false")
        boolean crossesMidnight,

        @Schema(description = "Thời gian chờ nếu đến trước giờ mở cửa (phút)", example = "0")
        int waitingMinutes,

        @Schema(description = "Trạng thái giờ hoạt động của địa điểm (OPEN, CLOSED, UNVERIFIED)")
        PlaceOpeningStatus openingStatus,

        @Schema(description = "Giờ mở cửa trong ngày", example = "07:30:00")
        LocalTime openingTime,

        @Schema(description = "Giờ đóng cửa trong ngày", example = "17:00:00")
        LocalTime closingTime,

        @Schema(description = "Lịch hoạt động cả tuần (nếu có)")
        WeeklyOpeningHours weeklyOpeningHours,

        @Schema(description = "Khoảng cách di chuyển từ điểm trước (km)", example = "3.5")
        double transitDistanceKmFromPrev,

        @Schema(description = "Thời gian di chuyển ước tính từ điểm trước (phút)", example = "15")
        int transitMinutesFromPrev,

        @Schema(description = "Trạng thái ghé thăm (NOT_VISITED, IN_PROGRESS, COMPLETED, SKIPPED)")
        VisitStatus visitStatus
) {
    public ScheduleItemContext {
        if (placeType == null) {
            placeType = PlaceType.UNSPECIFIED;
        }
        if (durationMinutes <= 0) {
            durationMinutes = 60;
        }
        if (visitStatus == null) {
            visitStatus = VisitStatus.NOT_VISITED;
        }
        if (displayDate == null && arrivalTime != null) {
            displayDate = arrivalTime.toLocalDate();
        }
        if (plannedDate == null && displayDate != null) {
            plannedDate = displayDate;
        }
        if (displayDate != null && plannedDate != null) {
            isSpillover = displayDate.isAfter(plannedDate);
        }
        if (arrivalTime != null) {
            LocalDateTime departure = arrivalTime.plusMinutes(durationMinutes);
            crossesMidnight = displayDate != null && departure.toLocalDate().isAfter(displayDate);
        }
        if (openingStatus == null) {
            openingStatus = (openingTime != null && closingTime != null)
                    ? PlaceOpeningStatus.OPEN
                    : PlaceOpeningStatus.UNVERIFIED;
        }
    }

    /**
     * Constructor tương thích ngược 11 tham số cũ
     */
    public ScheduleItemContext(
            UUID placeId,
            String placeName,
            double latitude,
            double longitude,
            PlaceType placeType,
            LocalTime openingTime,
            LocalTime closingTime,
            LocalDateTime arrivalTime,
            int durationMinutes,
            double transitDistanceKmFromPrev,
            int transitMinutesFromPrev
    ) {
        this(
                placeId,
                placeName,
                latitude,
                longitude,
                placeType,
                arrivalTime != null ? arrivalTime.toLocalDate() : LocalDate.now(),
                arrivalTime != null ? arrivalTime.toLocalDate() : LocalDate.now(),
                false,
                arrivalTime,
                durationMinutes,
                false,
                0,
                (openingTime != null && closingTime != null) ? PlaceOpeningStatus.OPEN : PlaceOpeningStatus.UNVERIFIED,
                openingTime,
                closingTime,
                null,
                transitDistanceKmFromPrev,
                transitMinutesFromPrev,
                VisitStatus.NOT_VISITED
        );
    }

    /**
     * Thời điểm rời đi dự kiến
     */
    public LocalDateTime departureTime() {
        return arrivalTime != null ? arrivalTime.plusMinutes(durationMinutes) : null;
    }
}
