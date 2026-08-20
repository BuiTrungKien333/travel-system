package com.travel.memory.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class UpdateMemoryRequest {

    /**
     * Nếu null → giữ nguyên content cũ.
     */
    @Size(max = 10000, message = "content must not exceed 10000 characters")
    private String content;

    /**
     * Nếu null → giữ nguyên visitedAt cũ.
     */
    private LocalDateTime visitedAt;
}
