package com.travel.planning.rule.dto;

import com.travel.planning.rule.model.ScheduleItemContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "Yêu cầu thẩm định các quy tắc xác định trong lịch trình")
public record EvaluateItineraryRequest(
    @NotEmpty(message = "Danh sách điểm dừng trong lịch trình không được để trống")
    @Valid
    @Schema(description = "Danh sách các điểm dừng trong lịch trình theo thứ tự thời gian")
    List<ScheduleItemContext> items
) {}
