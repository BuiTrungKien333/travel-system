package com.travel.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.travel.user.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Detailed user profile response payload")
public class UserProfileResponse {

    @Schema(description = "Unique identifier of the user", example = "a0000000-0000-0000-0000-000000000001")
    UUID id;

    @Schema(description = "Registered email address", example = "user@travel.com")
    String email;

    @Schema(description = "Full display name of the user", example = "Nguyen Van A")
    String fullName;

    @Schema(description = "Role assigned to the user", example = "USER")
    Role role;

    @Schema(description = "Avatar image URL", example = "https://example.com/avatar.jpg")
    String avatarUrl;

    @Schema(description = "Account creation timestamp", example = "2026-08-20T12:00:00Z")
    Instant createdAt;

    @Schema(description = "Last profile update timestamp", example = "2026-08-20T12:00:00Z")
    Instant updatedAt;
}
