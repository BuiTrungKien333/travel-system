package com.travel.memory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class CreateMemoryRequest {

    @NotNull(message = "userId is required")
    private UUID userId;

    @NotNull(message = "tripId is required")
    private UUID tripId;

    @NotNull(message = "placeId is required")
    private UUID placeId;

    @NotBlank(message = "content must not be blank")
    @Size(max = 10000, message = "content must not exceed 10000 characters")
    private String content;

    @NotNull(message = "visitedAt is required")
    private LocalDateTime visitedAt;
}
