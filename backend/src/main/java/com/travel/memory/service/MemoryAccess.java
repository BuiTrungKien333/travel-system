package com.travel.memory.service;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.memory.entity.Memory;
import com.travel.memory.repository.MemoryRepository;
import com.travel.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

/** Shared ownership checks and Trip -> Memory lock order for upload/edit/delete races. */
@Component
@RequiredArgsConstructor
public class MemoryAccess {
    private final MemoryRepository memories;
    private final TripRepository trips;

    public Memory read(UUID id, UUID owner, UUID requestedTrip) {
        Memory memory = memories.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEMORY_NOT_FOUND));
        validate(memory.getUserId(), memory.getTripId(), owner, requestedTrip);
        if (memory.getTripId() != null) trips.findByIdAndUserIdAndDeletedAtIsNull(memory.getTripId(), owner)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
        return memory;
    }

    public Memory lock(UUID id, UUID owner, UUID requestedTrip) {
        var ref = memories.reference(id).orElseThrow(() -> new AppException(ErrorCode.MEMORY_NOT_FOUND));
        validate(ref.getUserId(), ref.getTripId(), owner, requestedTrip);
        if (ref.getTripId() != null) trips.lockOwnedTrip(ref.getTripId(), owner)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
        return memories.lockActive(id, owner).orElseThrow(() -> new AppException(ErrorCode.MEMORY_NOT_FOUND));
    }

    private void validate(UUID actualOwner, UUID actualTrip, UUID owner, UUID requestedTrip) {
        if (!actualOwner.equals(owner) || (requestedTrip != null && !Objects.equals(requestedTrip, actualTrip)))
            throw new AppException(ErrorCode.MEMORY_ACCESS_DENIED);
    }
}
