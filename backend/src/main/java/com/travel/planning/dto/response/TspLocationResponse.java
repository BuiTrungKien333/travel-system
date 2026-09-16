package com.travel.planning.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Thông tin địa điểm trong lộ trình")
public record TspLocationResponse(
        @Schema(description = "ID địa điểm", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
        UUID id,

        @Schema(description = "Tên địa điểm", example = "Cầu Rồng")
        String name,

        @Schema(description = "Vĩ độ GPS", example = "16.0610")
        Double latitude,

        @Schema(description = "Kinh độ GPS", example = "108.2272")
        Double longitude
) {
}
