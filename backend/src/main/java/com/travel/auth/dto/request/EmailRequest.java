package com.travel.auth.dto.request;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

public record EmailRequest(
        @NotBlank @Email @Size(max = 255) @Schema(example = "name@example.com") String email) {
}
