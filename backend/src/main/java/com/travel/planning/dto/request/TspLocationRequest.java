package com.travel.planning.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Địa điểm cần tối ưu lộ trình")
public record TspLocationRequest(

        @Schema(description = "ID địa điểm (tùy chọn)", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
        UUID id,

        @NotBlank(message = "Tên địa điểm không được để trống")
        @Schema(description = "Tên địa điểm", example = "Cầu Rồng")
        String name,

        @NotNull(message = "Vĩ độ không được để trống")
        @DecimalMin(value = "-90.0", message = "Vĩ độ phải nằm trong khoảng [-90.0, 90.0]")
        @DecimalMax(value = "90.0", message = "Vĩ độ phải nằm trong khoảng [-90.0, 90.0]")
        @Schema(description = "Vĩ độ GPS", example = "16.0610")
        Double latitude,

        @NotNull(message = "Kinh độ không được để trống")
        @DecimalMin(value = "-180.0", message = "Kinh độ phải nằm trong khoảng [-180.0, 180.0]")
        @DecimalMax(value = "180.0", message = "Kinh độ phải nằm trong khoảng [-180.0, 180.0]")
        @Schema(description = "Kinh độ GPS", example = "108.2272")
        Double longitude
) {
}
