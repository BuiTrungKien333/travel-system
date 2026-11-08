package com.travel.planning.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Kế hoạch các địa điểm dự định ghé thăm trong một ngày")
public record DayPlanRequest(
        @NotNull(message = "Ngày tham quan không được để trống")
        @JsonAlias({"date", "tripDate"})
        @Schema(description = "Ngày kế hoạch tham quan", example = "2026-10-01")
        LocalDate dayDate,

        @Schema(description = "Giờ rời điểm xuất phát (mặc định 08:00)", example = "08:00:00")
        LocalTime startTime,

        @Valid
        @Schema(description = "Điểm neo xuất phát của ngày (Khách sạn hoặc vị trí hiện tại)")
        TspLocationRequest originPlace,

        @NotEmpty(message = "Danh sách địa điểm tham quan trong ngày không được để trống")
        @Valid
        @Schema(description = "Danh sách các địa điểm cần tham quan trong ngày")
        List<TspLocationRequest> locations,

        @Schema(description = "Có quay về điểm xuất phát khi kết thúc ngày không", example = "false")
        Boolean isRoundTrip,
        com.travel.planning.enums.PlanningTravelMode travelMode
) {
    public DayPlanRequest {
        if (travelMode == null) travelMode = com.travel.planning.enums.PlanningTravelMode.DRIVE;
        if (startTime == null) {
            startTime = LocalTime.of(8, 0);
        }
        if (isRoundTrip == null) {
            isRoundTrip = Boolean.FALSE;
        }
    }

    public DayPlanRequest(
            LocalDate dayDate,
            LocalTime startTime,
            TspLocationRequest originPlace,
            List<TspLocationRequest> locations
    ) {
        this(dayDate, startTime, originPlace, locations, Boolean.FALSE);
    }
    public DayPlanRequest(LocalDate dayDate, LocalTime startTime, TspLocationRequest originPlace,
            List<TspLocationRequest> locations, Boolean isRoundTrip) {
        this(dayDate, startTime, originPlace, locations, isRoundTrip, com.travel.planning.enums.PlanningTravelMode.DRIVE);
    }

    public boolean roundTrip() {
        return Boolean.TRUE.equals(isRoundTrip);
    }
}
