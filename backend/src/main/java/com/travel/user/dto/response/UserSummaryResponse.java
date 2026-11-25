package com.travel.user.dto.response;

import com.travel.user.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Safe summary representation of user profile")
public class UserSummaryResponse {

    @Schema(description = "Unique user identifier (UUID)", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID id;

    @Schema(description = "User email address", example = "user@travel.com")
    String email;

    @Schema(description = "User full name", example = "John Doe")
    String fullName;

    @Schema(description = "User role in the system", example = "USER")
    Role role;

    @Schema(description = "URL to user's profile avatar image", example = "https://example.com/avatar.jpg")
    String avatarUrl;
}
