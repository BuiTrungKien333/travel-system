package com.travel.auth.dto.request;

import com.travel.common.validation.Utf8ByteLength;
import jakarta.validation.constraints.*;

public record ResetPasswordRequest(
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token,
    @NotBlank @Size(min = 8, max = 50) @Utf8ByteLength String password
) {}
