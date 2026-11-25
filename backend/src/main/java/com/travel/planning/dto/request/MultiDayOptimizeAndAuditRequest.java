package com.travel.planning.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Yêu cầu tối ưu và thẩm định lịch trình cho chuyến đi nhiều ngày (Multi-day)")
public record MultiDayOptimizeAndAuditRequest(
    @Schema(description = "Tên thành phố / điểm đến", example = "Đà Nẵng") String destination,

    @NotEmpty(message = "Danh sách ngày kế hoạch không được để trống")
    @Valid
    @Schema(description = "Danh sách kế hoạch theo từng ngày")
    List<DayPlanRequest> days,

    @Schema(description = "Ghi chú thêm của du khách", example = "Đi cùng gia đình có trẻ nhỏ, ưu tiên nhịp độ thư thả")
    String userNotes,

    @Schema(description = "Có quay về điểm xuất phát chung không (tùy chọn)", example = "false") Boolean isRoundTrip
) {
    public MultiDayOptimizeAndAuditRequest(String destination, List<DayPlanRequest> days, String userNotes) {
        this(destination, days, userNotes, Boolean.FALSE);
    }
}
