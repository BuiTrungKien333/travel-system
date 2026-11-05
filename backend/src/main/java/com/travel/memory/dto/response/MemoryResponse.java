package com.travel.memory.dto.response;

import com.travel.media.dto.MediaResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryResponse {
    private long version;
    private UUID id;
    private UUID userId;
    private UUID tripId;
    private UUID placeId;
    private String note;
    private Instant experiencedAt;
    private String coverImageUrl;
    private Integer mediaCount;
    private List<MediaResponse> media;
    private Instant createdAt;
    private Instant updatedAt;
}
