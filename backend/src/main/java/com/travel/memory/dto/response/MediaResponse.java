package com.travel.memory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponse {
    private UUID id;
    private String url;
    private String mimeType;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private Integer displayOrder;
    private Instant createdAt;
}
