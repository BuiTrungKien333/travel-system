package com.travel.memory.service.impl;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.memory.dto.request.CreateMemoryRequest;
import com.travel.memory.dto.response.MemoryResponse;
import com.travel.memory.entity.Memory;
import com.travel.memory.mapper.MemoryMapper;
import com.travel.memory.repository.MemoryRepository;
import com.travel.memory.service.MemoryService;
import com.travel.trip.repository.TripPlaceRepository;
import com.travel.trip.repository.TripRepository;
import com.travel.trip.service.TripCalendar;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageImpl;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "MEMORY_SERVICE")
public class MemoryServiceImpl implements MemoryService {

    private final MemoryRepository memoryRepository;
    private final MemoryMapper memoryMapper;
    private final TripRepository tripRepository;
    private final TripPlaceRepository tripPlaceRepository;

    @Override
    @Transactional
    public MemoryResponse createMemory(UUID currentUserId, CreateMemoryRequest request) {
        if (request.getTripId() != null) {
            tripRepository.findByIdAndUserIdAndDeletedAtIsNull(request.getTripId(), currentUserId)
                    .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
            if (request.getPlaceId() != null
                    && !tripPlaceRepository.existsByTripIdAndPlaceId(request.getTripId(), request.getPlaceId())) {
                throw new AppException(ErrorCode.TRIP_PLACE_NOT_FOUND);
            }
        } else if (request.getPlaceId() != null) {
            throw new AppException(ErrorCode.TRIP_NOT_FOUND);
        }
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

    @Override
    @Transactional(readOnly = true)
    public Page<MemoryResponse> getMemoriesByTripDay(UUID tripId, UUID requestUserId, LocalDate date, Pageable pageable) {
        tripRepository.findByIdAndUserIdAndDeletedAtIsNull(tripId, requestUserId)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
        Pageable page = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 50),
                Sort.by(Sort.Order.desc("experiencedAt"), Sort.Order.desc("id")));
        var ids = memoryRepository.dayIds(tripId, requestUserId, date.atStartOfDay(TripCalendar.ZONE).toInstant(),
                date.plusDays(1).atStartOfDay(TripCalendar.ZONE).toInstant(), page);
        if (ids.isEmpty()) return new PageImpl<>(java.util.List.of(), page, ids.getTotalElements());
        var records = memoryRepository.findByIdInAndTripIdAndUserIdAndDeletedAtIsNull(ids.getContent(), tripId, requestUserId)
                .stream().collect(Collectors.toMap(Memory::getId, Function.identity()));
        var content = ids.getContent().stream().map(records::get).filter(Objects::nonNull).map(memoryMapper::toResponse).toList();
        return new PageImpl<>(content, page, ids.getTotalElements());
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
