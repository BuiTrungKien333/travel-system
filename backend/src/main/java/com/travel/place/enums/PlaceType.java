package com.travel.place.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Phân loại không gian của địa điểm du lịch")
public enum PlaceType {
    @Schema(description = "Trong nhà (bảo tàng, trung tâm thương mại, nhà hát...)")
    INDOOR,

    @Schema(description = "Ngoài trời (bãi biển, công viên, leo núi, đèo...)")
    OUTDOOR,

    @Schema(description = "Hỗn hợp cả trong nhà và ngoài trời (resort, khu du lịch sinh thái...)")
    HYBRID,

    @Schema(description = "Chưa xác định")
    UNSPECIFIED,
}
