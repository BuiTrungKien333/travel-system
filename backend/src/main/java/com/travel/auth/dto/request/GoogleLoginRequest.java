package com.travel.auth.dto.request;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record GoogleLoginRequest(@NotNull UUID challengeId, @NotBlank @Size(max = 8192) String idToken) {
}
