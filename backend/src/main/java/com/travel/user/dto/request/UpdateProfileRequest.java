package com.travel.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request payload for updating user profile")
public class UpdateProfileRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    @Schema(description = "Full display name of the user", example = "Nguyen Van B")
    String fullName;

    @Size(max = 1000, message = "Avatar URL must not exceed 1000 characters")
    @Schema(description = "Avatar image URL", example = "https://example.com/new-avatar.jpg")
    String avatarUrl;
}
