package com.travel.memory.service.impl;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.memory.dto.request.CreateMemoryRequest;
import com.travel.memory.dto.request.UpdateMemoryRequest;
import com.travel.memory.repository.MemoryCreateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final MemoryCreateRepository createRepository;
    private final ObjectMapper json;

    @Override
    @Transactional
    public MemoryResponse createMemory(UUID currentUserId, CreateMemoryRequest request) {
        if (request.getMedia() != null && !request.getMedia().isEmpty())
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        String hash = request.getClientRequestId() == null ? null : createHash(request);
        if (request.getClientRequestId() != null) {
            var existing = memoryRepository.findByUserIdAndClientRequestId(currentUserId, request.getClientRequestId());
            if (existing.isPresent()) return replay(existing.get(), hash);
        }
        if (request.getTripId() != null) {
            tripRepository.lockOwnedTrip(request.getTripId(), currentUserId)
                    .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
            if (request.getPlaceId() != null
                    && !tripPlaceRepository.existsByTripIdAndPlaceId(request.getTripId(), request.getPlaceId())) {
                throw new AppException(ErrorCode.TRIP_PLACE_NOT_FOUND);
            }
        } else if (request.getPlaceId() != null) {
            throw new AppException(ErrorCode.TRIP_NOT_FOUND);
        }
        if (request.getClientRequestId() != null) {
            createRepository.insert(UUID.randomUUID(), currentUserId, request, hash);
            Memory result = memoryRepository.findByUserIdAndClientRequestId(currentUserId, request.getClientRequestId())
                    .orElseThrow(() -> new IllegalStateException("Memory create result missing"));
            return replay(result, hash);
        }
        Memory memory = memoryMapper.toEntity(currentUserId, request);
        Memory saved = memoryRepository.save(memory);
        log.info("Memory created: id={}, userId={}, mediaCount={}", saved.getId(), saved.getUserId(), saved.getMedia().size());
        return memoryMapper.toResponse(saved);
    }

    private String createHash(CreateMemoryRequest request) {
        try {
            byte[] payload = json.writeValueAsBytes(new Object[]{request.getTripId(), request.getPlaceId(),
                    request.getNote(), request.getExperiencedAt() == null ? null : request.getExperiencedAt().toString()});
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (com.fasterxml.jackson.core.JsonProcessingException | java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException("Cannot fingerprint memory request", error);
        }
    }

    private MemoryResponse replay(Memory memory, String hash) {
        if (memory.isDeleted() || !Objects.equals(memory.getCreateRequestHash(), hash))
            throw new AppException(ErrorCode.MEMORY_REQUEST_CONFLICT);
        return memoryMapper.toResponse(memory);
    }

    @Override
    @Transactional
    public MemoryResponse updateNote(UUID id, UUID userId, UpdateMemoryRequest request) {
        Memory memory = memoryRepository.lockActive(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEMORY_NOT_FOUND));
        checkOwnership(memory, userId);
        if (memory.getVersion() != request.version()) throw new AppException(ErrorCode.MEMORY_VERSION_CONFLICT);
        memory.updateNote(request.note());
        memoryRepository.flush();
        return memoryMapper.toResponse(memory);
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
        return ownerPage(userId, null, safePageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MemoryResponse> getMemoriesByTrip(UUID tripId, UUID requestUserId, Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable);
        tripRepository.findByIdAndUserIdAndDeletedAtIsNull(tripId, requestUserId)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
        return ownerPage(requestUserId, tripId, safePageable);
    }

    private Page<MemoryResponse> ownerPage(UUID owner, UUID trip, Pageable pageable) {
        var ids = memoryRepository.ownerIds(owner, trip, pageable);
        if (ids.isEmpty()) return new PageImpl<>(java.util.List.of(), pageable, ids.getTotalElements());
        var records = memoryRepository.findByIdInAndUserIdAndDeletedAtIsNull(ids.getContent(), owner).stream()
                .collect(Collectors.toMap(Memory::getId, Function.identity()));
        var content = ids.getContent().stream().map(records::get).filter(Objects::nonNull).map(memoryMapper::toResponse).toList();
        return new PageImpl<>(content, pageable, ids.getTotalElements());
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
        pageable = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 50), pageable.getSort());

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
            validOrders.add(Sort.Order.desc("experiencedAt"));
        }
        if (validOrders.stream().noneMatch(order -> order.getProperty().equals("id"))) validOrders.add(Sort.Order.desc("id"));
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
