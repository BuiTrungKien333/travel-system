package com.travel.memory.service.impl;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.memory.dto.request.CreateMemoryRequest;
import com.travel.memory.dto.request.UpdateMemoryRequest;
import com.travel.memory.dto.response.MemoryResponse;
import com.travel.memory.entity.Memory;
import com.travel.memory.mapper.MemoryMapper;
import com.travel.memory.repository.MemoryRepository;
import com.travel.memory.service.MemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemoryServiceImpl implements MemoryService {

    private final MemoryRepository memoryRepository;
    private final MemoryMapper memoryMapper;

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public MemoryResponse createMemory(CreateMemoryRequest request) {
        Memory memory = memoryMapper.toEntity(request);
        Memory saved = memoryRepository.save(memory);
        log.info("Memory created: id={}, userId={}", saved.getId(), saved.getUserId());
        return memoryMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

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
        return memoryRepository
                .findByUserIdAndDeletedAtIsNull(userId, pageable)
                .map(memoryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MemoryResponse> getMemoriesByTrip(UUID tripId, UUID requestUserId, Pageable pageable) {
        // Chỉ trả memories của requestUserId trong trip — không để user xem memories của người khác
        return memoryRepository
                .findByTripIdAndUserIdAndDeletedAtIsNull(tripId, requestUserId, pageable)
                .map(memoryMapper::toResponse);
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public MemoryResponse updateMemory(UUID id, UpdateMemoryRequest request, UUID requestUserId) {
        Memory memory = findActiveMemoryById(id);
        checkOwnership(memory, requestUserId);

        memory.updateContent(request.getContent());
        memory.updateVisitedAt(request.getVisitedAt());

        Memory updated = memoryRepository.save(memory);
        log.info("Memory updated: id={}", updated.getId());
        return memoryMapper.toResponse(updated);
    }

    // -------------------------------------------------------------------------
    // Delete (soft)
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void deleteMemory(UUID id, UUID requestUserId) {
        Memory memory = findActiveMemoryById(id);
        checkOwnership(memory, requestUserId);

        memory.softDelete();
        memoryRepository.save(memory);
        log.info("Memory soft-deleted: id={}, userId={}", id, requestUserId);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

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
