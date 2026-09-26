package com.travel.planning.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Yêu cầu tối ưu hóa thứ tự và thẩm định toàn diện lịch trình du lịch (Held-Karp + Weather + Rules + AI Copilot)")
public record OptimizeAndAuditRequest(
        @Schema(description = "Tên thành phố / điểm đến", example = "Đà Nẵng")
        String destination,

        @Schema(description = "Ngày tham quan (mặc định hôm nay nếu để trống)", example = "2026-10-01")
        LocalDate tripDate,

        @Schema(description = "Giờ xuất phát chuyến đi (mặc định 08:00)", example = "08:00:00")
        LocalTime startTime,

        @Schema(description = "ID địa điểm xuất phát (nếu có, ví dụ khách sạn)")
        UUID startPlaceId,

        @Schema(description = "Có quay về điểm xuất phát khi kết thúc ngày không", example = "false")
        boolean isRoundTrip,

        @NotEmpty(message = "Danh sách địa điểm không được để trống")
        @Size(min = 2, max = 20, message = "Số lượng địa điểm cần tối ưu phải từ 2 đến 20")
        @Valid
        @Schema(description = "Danh sách các địa điểm cần ghé thăm")
        List<TspLocationRequest> locations,

        @Schema(description = "Ghi chú thêm của du khách", example = "Đi cùng gia đình có trẻ nhỏ, ưu tiên nhịp độ thư thả")
        String userNotes
) {
    public OptimizeAndAuditRequest {
        if (tripDate == null) {
            tripDate = LocalDate.now();
        }
        if (startTime == null) {
            startTime = LocalTime.of(8, 0);
        }
    }
}
