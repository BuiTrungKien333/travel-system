package com.travel.memory.mapper;

import com.travel.memory.dto.request.CreateMemoryRequest;
import com.travel.memory.dto.response.MemoryResponse;
import com.travel.memory.entity.Memory;
import org.springframework.stereotype.Component;

@Component
public class MemoryMapper {

    public Memory toEntity(CreateMemoryRequest request) {
        return Memory.create(
                request.getUserId(),
                request.getTripId(),
                request.getPlaceId(),
                request.getContent(),
                request.getVisitedAt()
        );
    }

    public MemoryResponse toResponse(Memory memory) {
        return MemoryResponse.builder()
                .id(memory.getId())
                .userId(memory.getUserId())
                .tripId(memory.getTripId())
                .placeId(memory.getPlaceId())
                .content(memory.getContent())
                .visitedAt(memory.getVisitedAt())
                .createdAt(memory.getCreatedAt())
                .updatedAt(memory.getUpdatedAt())
                .build();
    }
}
