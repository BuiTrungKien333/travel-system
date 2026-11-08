package com.travel.planning.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "Yêu cầu tối ưu lộ trình theo thuật toán Held-Karp")
public record OptimizeRouteRequest(

        @NotEmpty(message = "Danh sách địa điểm không được rỗng")
        @Size(max = 16, message = "Số lượng địa điểm tối đa cho thuật toán Held-Karp là 16")
        @Schema(description = "Danh sách các địa điểm cần tối ưu")
        List<@Valid TspLocationRequest> locations,

        @Schema(description = "Lộ trình khép kín quay lại điểm bắt đầu (mặc định: true)", example = "true")
        Boolean roundTrip,

        @Schema(description = "ID của địa điểm xuất phát (nếu có)", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
        UUID startPlaceId,

        @Schema(description = "ID của địa điểm kết thúc (áp dụng khi roundTrip = false)", example = "b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22")
        UUID endPlaceId,

        @Schema(description = "Vị trí index xuất phát trong danh sách (tính từ 0, dùng khi không truyền startPlaceId)", example = "0")
        Integer startIndex,

        @Schema(description = "Vị trí index kết thúc trong danh sách (khi roundTrip = false)", example = "4")
        Integer endIndex
) {
    public boolean isRoundTrip() {
        return roundTrip == null || roundTrip;
    }
}
