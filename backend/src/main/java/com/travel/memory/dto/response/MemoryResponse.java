package com.travel.memory.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class MemoryResponse {

    private UUID id;
    private UUID userId;
    private UUID tripId;
    private UUID placeId;
    private String content;
    private LocalDateTime visitedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
