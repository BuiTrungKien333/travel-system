package com.travel.memory.service;

import com.travel.memory.dto.request.CreateMemoryRequest;
import com.travel.memory.dto.response.MemoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;
import java.time.LocalDate;

public interface MemoryService {

    /**
     * Create a new Memory along with its attached media list.
     *
     * @param currentUserId the ID of the requesting user
     * @param request       the memory creation request payload
     * @return the created memory response DTO
     */
    MemoryResponse createMemory(UUID currentUserId, CreateMemoryRequest request);

    /**
     * Retrieve a memory by ID. Validates ownership against the requesting user.
     *
     * @param id            the memory ID
     * @param requestUserId the ID of the requesting user
     * @return the memory response DTO
     */
    MemoryResponse getMemoryById(UUID id, UUID requestUserId);

    /**
     * Retrieve paginated memories of a user.
     *
     * @param userId   the user ID
     * @param pageable pagination parameters
     * @return paginated memory response DTOs
     */
    Page<MemoryResponse> getMemoriesByUser(UUID userId, Pageable pageable);

    /**
     * Retrieve paginated memories of the requesting user within a trip.
     *
     * @param tripId        the trip ID
     * @param requestUserId the ID of the requesting user
     * @param pageable      pagination parameters
     * @return paginated memory response DTOs
     */
    Page<MemoryResponse> getMemoriesByTrip(UUID tripId, UUID requestUserId, Pageable pageable);
    Page<MemoryResponse> getMemoriesByTripDay(UUID tripId, UUID requestUserId, LocalDate date, Pageable pageable);

    /**
     * Soft delete a Memory (and dissociate its media). Validates ownership.
     *
     * @param id            the memory ID
     * @param requestUserId the ID of the requesting user
     */
    void deleteMemory(UUID id, UUID requestUserId);
}
