package com.travel.memory.mapper;

import com.travel.media.dto.MediaResponse;
import com.travel.media.entity.Media;
import com.travel.memory.dto.request.CreateMemoryRequest;
import com.travel.memory.dto.response.MemoryResponse;
import com.travel.memory.entity.Memory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class MemoryMapper {

    /**
     * Map CreateMemoryRequest to Memory entity with bidirectional Media mapping.
     */
    public Memory toEntity(UUID userId, CreateMemoryRequest request) {
        Memory memory = Memory.create(
                userId,
                request.getTripId(),
                request.getPlaceId(),
                request.getNote(),
                request.getExperiencedAt()
        );

        return memory;
    }

    /**
     * Map Memory entity to MemoryResponse DTO.
     */
    public MemoryResponse toResponse(Memory memory) {
        if (memory == null) {
            return null;
        }

        List<MediaResponse> mediaResponses = (memory.getMedia() != null)
                ? memory.getMedia().stream()
                .map(this::toMediaResponse)
                .collect(Collectors.toList())
                : Collections.emptyList();

        String coverImageUrl = !mediaResponses.isEmpty() ? mediaResponses.getFirst().getUrl() : null;

        return MemoryResponse.builder()
                .id(memory.getId())
                .version(memory.getVersion())
                .userId(memory.getUserId())
                .tripId(memory.getTripId())
                .placeId(memory.getPlaceId())
                .note(memory.getNote())
                .experiencedAt(memory.getExperiencedAt())
                .coverImageUrl(coverImageUrl)
                .mediaCount(mediaResponses.size())
                .media(mediaResponses)
                .createdAt(memory.getCreatedAt())
                .updatedAt(memory.getUpdatedAt())
                .build();
    }

    /**
     * Map Media entity to MediaResponse DTO.
     */
    public MediaResponse toMediaResponse(Media media) {
        if (media == null) {
            return null;
        }
        return MediaResponse.builder()
                .id(media.getId())
                .url(media.getUrl())
                .mimeType(media.getMimeType())
                .fileSize(media.getFileSize())
                .width(media.getWidth())
                .height(media.getHeight())
                .displayOrder(media.getDisplayOrder())
                .createdAt(media.getCreatedAt())
                .build();
    }
}
