package com.travel.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record EmailRequest(@NotBlank @Email @Size(max = 255) @Schema(example = "name@example.com") String email) {}
