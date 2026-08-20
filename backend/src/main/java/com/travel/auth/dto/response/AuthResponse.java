package com.travel.auth.dto.response;

import com.travel.user.dto.response.UserSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Authentication response payload containing JWT tokens")
public class AuthResponse {

    @Schema(description = "JWT Access Token for authenticating API requests", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String accessToken;

    @Schema(description = "JWT Refresh Token for obtaining new access tokens", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String refreshToken;

    @Builder.Default
    @Schema(description = "Authentication token scheme", example = "Bearer")
    String tokenType = "Bearer";

    @Schema(description = "Access token lifetime in milliseconds", example = "1800000")
    long expiresIn;

    @Schema(description = "Summary profile information of the authenticated user")
    UserSummaryResponse user;
}
