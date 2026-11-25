package com.travel.auth.dto.request;

import jakarta.validation.constraints.*;

public record ActionTokenRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token) {}
