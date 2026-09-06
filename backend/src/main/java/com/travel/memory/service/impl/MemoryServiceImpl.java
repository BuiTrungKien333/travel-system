package com.travel.memory.service.impl;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.memory.dto.request.CreateMemoryRequest;
import com.travel.memory.dto.response.MemoryResponse;
import com.travel.memory.entity.Media;
import com.travel.memory.entity.Memory;
import com.travel.memory.mapper.MemoryMapper;
import com.travel.memory.repository.MediaRepository;
import com.travel.memory.repository.MemoryRepository;
import com.travel.memory.service.MemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "MEMORY_SERVICE")
public class MemoryServiceImpl implements MemoryService {

    private final MemoryRepository memoryRepository;
    private final MediaRepository mediaRepository;
    private final MemoryMapper memoryMapper;

    @Override
    @Transactional
    public MemoryResponse createMemory(UUID currentUserId, CreateMemoryRequest request) {
        Memory memory = memoryMapper.toEntity(currentUserId, request);
        Memory saved = memoryRepository.save(memory);
        log.info("Memory created: id={}, userId={}, mediaCount={}", saved.getId(), saved.getUserId(), saved.getMedia().size());
        return memoryMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MemoryResponse getMemoryById(UUID id, UUID requestUserId) {
        Memory memory = findActiveMemoryById(id);
        checkOwnership(memory, requestUserId);
        return memoryMapper.toResponse(memory);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MemoryResponse> getMemoriesByUser(UUID userId, Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable);
        return memoryRepository
                .findByUserIdAndDeletedAtIsNull(userId, safePageable)
                .map(memoryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MemoryResponse> getMemoriesByTrip(UUID tripId, UUID requestUserId, Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable);
        return memoryRepository
                .findByTripIdAndUserIdAndDeletedAtIsNull(tripId, requestUserId, safePageable)
                .map(memoryMapper::toResponse);
    }

    private Pageable sanitizePageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "experiencedAt"));
        }
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }

        java.util.Map<String, String> propertyAliasMap = java.util.Map.ofEntries(
                java.util.Map.entry("id", "id"),
                java.util.Map.entry("userid", "userId"),
                java.util.Map.entry("user_id", "userId"),
                java.util.Map.entry("tripid", "tripId"),
                java.util.Map.entry("trip_id", "tripId"),
                java.util.Map.entry("placeid", "placeId"),
                java.util.Map.entry("place_id", "placeId"),
                java.util.Map.entry("note", "note"),
                java.util.Map.entry("experiencedat", "experiencedAt"),
                java.util.Map.entry("experienced_at", "experiencedAt"),
                java.util.Map.entry("createdat", "createdAt"),
                java.util.Map.entry("created_at", "createdAt"),
                java.util.Map.entry("updatedat", "updatedAt"),
                java.util.Map.entry("updated_at", "updatedAt")
        );

        java.util.List<Sort.Order> validOrders = new java.util.ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            String prop = order.getProperty();
            if (prop != null) {
                String cleanProp = prop.replaceAll("[\"\\[\\]]", "").trim().toLowerCase();
                String mappedField = propertyAliasMap.get(cleanProp);
                if (mappedField != null) {
                    validOrders.add(new Sort.Order(order.getDirection(), mappedField));
                }
            }
        }

        if (validOrders.isEmpty()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "experiencedAt"));
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(validOrders));
    }

    @Override
    @Transactional
    public void deleteMemory(UUID id, UUID requestUserId) {
        Memory memory = findActiveMemoryById(id);
        checkOwnership(memory, requestUserId);

        memory.softDelete();
        memoryRepository.save(memory);
        log.info("Memory soft-deleted: id={}, userId={}", id, requestUserId);
    }

    @Override
    @Transactional
    public void deleteMedia(UUID mediaId, UUID requestUserId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDIA_NOT_FOUND));

        Memory memory = media.getMemory();
        if (memory == null || memory.isDeleted() || !memory.getUserId().equals(requestUserId)) {
            throw new AppException(ErrorCode.MEMORY_ACCESS_DENIED);
        }

        memory.removeMedia(media);
        mediaRepository.delete(media);
        log.info("Media deleted: mediaId={}, memoryId={}, userId={}", mediaId, memory.getId(), requestUserId);
    }

    private Memory findActiveMemoryById(UUID id) {
        return memoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEMORY_NOT_FOUND));
    }

    private void checkOwnership(Memory memory, UUID requestUserId) {
        if (!memory.getUserId().equals(requestUserId)) {
            throw new AppException(ErrorCode.MEMORY_ACCESS_DENIED);
        }
    }
}
