package com.travel.memory.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.media.service.MediaCleanupQueue;
import com.travel.memory.dto.request.CreateMemoryRequest;
import com.travel.memory.dto.request.UpdateMemoryRequest;
import com.travel.memory.dto.response.MemoryResponse;
import com.travel.memory.entity.Memory;
import com.travel.memory.mapper.MemoryMapper;
import com.travel.memory.repository.MemoryRepository;
import com.travel.memory.service.MemoryAccess;
import com.travel.memory.service.MemoryService;
import com.travel.trip.repository.TripPlaceRepository;
import com.travel.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MemoryServiceImpl implements MemoryService {
    private final MemoryRepository memoryRepository;
    private final MemoryMapper memoryMapper;
    private final TripRepository tripRepository;
    private final TripPlaceRepository tripPlaceRepository;
    private final MemoryAccess access;
    private final MediaCleanupQueue cleanup;
    private final ObjectMapper json;
    private final Clock clock;

    @Override
    @Transactional
    public MemoryResponse createMemory(UUID owner, CreateMemoryRequest request) {
        if (request.getMedia() != null && !request.getMedia().isEmpty()) invalid("media");
        if (request.getNote() != null && request.getNote().length() > 2000) invalid("note");
        if (request.getExperiencedAt() != null && request.getExperiencedAt().isAfter(clock.instant())) invalid("experiencedAt");
        String hash = request.getClientRequestId() == null ? null : requestHash(request);
        if (hash != null) {
            var existing = memoryRepository.findByUserIdAndClientRequestId(owner, request.getClientRequestId());
            if (existing.isPresent()) return replay(existing.get(), hash);
        }
        if (request.getTripId() != null) {
            tripRepository.lockOwnedTrip(request.getTripId(), owner)
                    .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
            if (request.getPlaceId() != null && !tripPlaceRepository.existsByTripIdAndPlaceId(request.getTripId(), request.getPlaceId()))
                throw new AppException(ErrorCode.TRIP_PLACE_NOT_FOUND);
        } else if (request.getPlaceId() != null) invalid("placeId");
        request.setNote(trim(request.getNote()));
        if (hash != null) {
            memoryRepository.insertIdempotent(UUID.randomUUID(), owner, request.getTripId(), request.getPlaceId(), request.getNote(),
                    request.getExperiencedAt() == null ? clock.instant() : request.getExperiencedAt(), request.getClientRequestId(), hash);
            return replay(memoryRepository.findByUserIdAndClientRequestId(owner, request.getClientRequestId()).orElseThrow(), hash);
        }
        Memory memory = memoryMapper.toEntity(owner, request);
        return memoryMapper.toResponse(memoryRepository.saveAndFlush(memory));
    }

    private MemoryResponse replay(Memory memory, String hash) {
        if (!Objects.equals(memory.getCreateRequestHash(), hash)) throw new AppException(ErrorCode.MEMORY_REQUEST_CONFLICT);
        if (memory.isDeleted()) throw new AppException(ErrorCode.MEMORY_NOT_FOUND);
        return memoryMapper.toResponse(access.read(memory.getId(), memory.getUserId(), null));
    }

    private String requestHash(CreateMemoryRequest request) {
        try {
            byte[] bytes = json.writeValueAsBytes(Arrays.asList(request.getTripId(), request.getPlaceId(), trim(request.getNote()), request.getExperiencedAt()));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (JsonProcessingException | NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    @Override
    @Transactional(readOnly = true)
    public MemoryResponse getMemoryById(UUID id, UUID owner) {
        return memoryMapper.toResponse(access.read(id, owner, null));
    }

    @Override
    @Transactional
    public MemoryResponse updateMemory(UUID id, UUID owner, UpdateMemoryRequest request) {
        Memory memory = access.lock(id, owner, null);
        if (request.getVersion() == null || request.getVersion() != memory.getVersion())
            throw new AppException(ErrorCode.MEMORY_VERSION_CONFLICT);
        if (!request.isNoteProvided() && request.getExperiencedAt() == null && request.getMediaOrder() == null) invalid("body");
        if (request.isNoteProvided()) memory.updateNote(trim(request.getNote()));
        memory.updateExperiencedAt(request.getExperiencedAt());
        if (request.getMediaOrder() != null) {
            var order = request.getMediaOrder();
            var ids = memory.getMedia().stream().map(m -> m.getId()).collect(java.util.stream.Collectors.toSet());
            if (order.size() != ids.size() || !new HashSet<>(order).equals(ids)) invalid("mediaOrder");
            var byId = memory.getMedia().stream().collect(java.util.stream.Collectors.toMap(m -> m.getId(), m -> m));
            for (int i = 0; i < order.size(); i++) byId.get(order.get(i)).setDisplayOrder(i);
            memory.getMedia().sort(Comparator.comparingInt(m -> m.getDisplayOrder()));
        }
        memory.markChanged();
        memoryRepository.flush();
        return memoryMapper.toResponse(memory);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MemoryResponse> getMemoriesByUser(UUID owner, Pageable pageable) {
        return memoryRepository.findByUserIdAndDeletedAtIsNull(owner, sanitizePageable(pageable)).map(memoryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MemoryResponse> getMemoriesByTrip(UUID tripId, UUID owner, Pageable pageable) {
        tripRepository.findByIdAndUserIdAndDeletedAtIsNull(tripId, owner).orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
        return memoryRepository.findByTripIdAndUserIdAndDeletedAtIsNull(tripId, owner, sanitizePageable(pageable)).map(memoryMapper::toResponse);
    }

    private Pageable sanitizePageable(Pageable pageable) {
        if (pageable == null) return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "experiencedAt", "id"));
        if (pageable.isUnpaged() || pageable.getPageSize() > 50) invalid("size");
        var aliases = Map.of("id", "id", "experiencedat", "experiencedAt", "experienced_at", "experiencedAt",
                "createdat", "createdAt", "created_at", "createdAt", "updatedat", "updatedAt", "updated_at", "updatedAt");
        List<Sort.Order> orders = new ArrayList<>();
        for (var order : pageable.getSort()) {
            String field = aliases.get(order.getProperty().toLowerCase(Locale.ROOT));
            if (field == null) invalid("sort");
            orders.add(new Sort.Order(order.getDirection(), field));
        }
        if (orders.isEmpty()) orders.add(Sort.Order.desc("experiencedAt"));
        if (orders.stream().noneMatch(o -> o.getProperty().equals("id"))) orders.add(Sort.Order.desc("id"));
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
    }

    @Override
    @Transactional
    public void deleteMemory(UUID id, UUID owner) {
        Memory memory = access.lock(id, owner, null);
        for (var media : memory.getMedia()) cleanup.enqueue(media.getPublicId());
        memory.softDelete();
        memoryRepository.flush();
    }

    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private void invalid(String field) { throw new AppException(ErrorCode.VALIDATION_ERROR, Map.of(field, "Invalid value")); }
}
