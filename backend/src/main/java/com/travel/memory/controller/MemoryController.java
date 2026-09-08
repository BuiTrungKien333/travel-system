package com.travel.memory.controller;

import com.travel.common.response.ApiResponse;
import com.travel.memory.dto.request.CreateMemoryRequest;
import com.travel.memory.dto.response.MemoryResponse;
import com.travel.memory.service.MemoryService;
import com.travel.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/journal/memories")
@RequiredArgsConstructor
@Slf4j(topic = "MEMORY_CONTROLLER")
@Tag(name = "Journal Memory Management", description = "Endpoints for managing personal travel memories and journals")
public class MemoryController {

    private final MemoryService memoryService;

    /**
     * Create a new travel memory with optional attached media.
     * POST /api/v1/journal/memories
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create new memory", description = "Creates a new travel memory with optional attached media for the authenticated user")
    public ApiResponse<MemoryResponse> createMemory(
            @Valid @RequestBody CreateMemoryRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        log.info("User [{}] is creating memory for trip [{}]", currentUserId, request.getTripId());

        MemoryResponse response = memoryService.createMemory(currentUserId, request);
        return ApiResponse.success("Memory created successfully", response);
    }

    /**
     * Retrieve detailed memory information by ID via query parameter.
     * GET /api/v1/journal/memories/detail?id=...
     */
    @GetMapping("/detail")
    @Operation(summary = "Get memory details", description = "Retrieves detailed information of a specific memory by its ID query parameter")
    public ApiResponse<MemoryResponse> getMemoryById(
            @RequestParam("memoryId") UUID id
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        MemoryResponse response = memoryService.getMemoryById(id, currentUserId);
        return ApiResponse.success(response);
    }

    /**
     * Retrieve paginated list of memories for the authenticated user.
     * Optionally filter by trip: GET /api/v1/journal/memories?tripId=...
     */
    @GetMapping
    @Operation(summary = "List memories", description = "Retrieves a paginated list of memories for the authenticated user, optionally filtered by tripId")
    public ApiResponse<Page<MemoryResponse>> getMemories(
            @RequestParam(value = "tripId", required = false) UUID tripId,
            @ParameterObject @PageableDefault(size = 20, sort = "experiencedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Page<MemoryResponse> responses;
        if (tripId != null) {
            responses = memoryService.getMemoriesByTrip(tripId, currentUserId, pageable);
        } else {
            responses = memoryService.getMemoriesByUser(currentUserId, pageable);
        }
        return ApiResponse.success(responses);
    }
    /**
     * Soft delete a memory (and all its associated media items).
     * DELETE /api/v1/journal/memories?id=... (alias: /delete-memory)
     */
    @DeleteMapping(value = {"", "/delete-memory"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete memory", description = "Soft deletes a memory by its ID query parameter. Requires ownership.")
    public void deleteMemory(
            @RequestParam("id") UUID id
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        log.info("User [{}] is deleting memory [{}]", currentUserId, id);
        memoryService.deleteMemory(id, currentUserId);
    }
}
