package com.travel.planning.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Chi tiết một hoạt động hoặc điểm dừng chân trong ngày")
public record AiActivitySlotResponse(
        @Schema(description = "ID của địa điểm (nếu là điểm tham quan đã chọn)", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
        UUID placeId,

        @Schema(description = "Tên địa điểm hoặc tên hoạt động", example = "Chùa Linh Ứng (Bán đảo Sơn Trà)")
        String placeName,

        @Schema(description = "Khung giờ gợi ý", example = "08:30 - 10:30")
        String timeSlot,

        @Schema(description = "Loại hoạt động: VISIT, MEAL, REST, TRANSIT", example = "VISIT")
        String activityType,

        @Schema(description = "Mô tả trải nghiệm nổi bật", example = "Chiêm ngưỡng tượng Phật Quan Âm 67m và ngắm toàn cảnh vịnh Đà Nẵng")
        String description,

        @Schema(description = "Thời lượng lưu trú gợi ý", example = "90 phút")
        String recommendedDuration,

        @Schema(description = "Lời khuyên di chuyển hoặc lưu ý trang phục, thời tiết", example = "Nên đi buổi sáng sớm để tránh nắng gắt và chụp ảnh đẹp nhất")
        String travelNote
) {
}
