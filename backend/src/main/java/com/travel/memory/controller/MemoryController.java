package com.travel.memory.controller;

import com.travel.common.response.ApiResponse;
import com.travel.memory.dto.request.CreateMemoryRequest;
import com.travel.memory.dto.request.UpdateMemoryRequest;
import com.travel.memory.dto.response.MemoryResponse;
import com.travel.memory.service.MemoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/memories")
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryService memoryService;

    /**
     * Tạo mới một Memory.
     *
     * @param request body chứa dữ liệu memory
     * @return 201 Created với memory vừa tạo
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemoryResponse> createMemory(
            @Valid @RequestBody CreateMemoryRequest request
    ) {
        MemoryResponse response = memoryService.createMemory(request);
        return ApiResponse.success("Memory created successfully", response);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/memories/{id}
    // -------------------------------------------------------------------------

    /**
     * Lấy chi tiết một Memory theo id. Yêu cầu ownership.
     *
     * @param id        UUID của memory
     * @param userId    UUID của user đang request (tạm dùng header)
     * @return 200 OK với memory tương ứng
     */
    @GetMapping("/{id}")
    public ApiResponse<MemoryResponse> getMemoryById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        MemoryResponse response = memoryService.getMemoryById(id, userId);
        return ApiResponse.success(response);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/memories/user/{userId}
    // -------------------------------------------------------------------------

    /**
     * Lấy danh sách Memory của một user (phân trang).
     *
     * @param userId   UUID của user cần lấy memories
     * @param pageable thông số phân trang (page, size, sort)
     * @return 200 OK với page memories
     */
    @GetMapping("/user/{userId}")
    public ApiResponse<Page<MemoryResponse>> getMemoriesByUser(
            @PathVariable UUID userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<MemoryResponse> responses = memoryService.getMemoriesByUser(userId, pageable);
        return ApiResponse.success(responses);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/memories/trip/{tripId}
    // -------------------------------------------------------------------------

    /**
     * Lấy danh sách Memory của user hiện tại trong một trip (phân trang).
     *
     * @param tripId   UUID của trip
     * @param userId   UUID của user đang request (tạm dùng header)
     * @param pageable thông số phân trang (page, size, sort)
     * @return 200 OK với page memories
     */
    @GetMapping("/trip/{tripId}")
    public ApiResponse<Page<MemoryResponse>> getMemoriesByTrip(
            @PathVariable UUID tripId,
            @RequestHeader("X-User-Id") UUID userId,
            @PageableDefault(size = 20, sort = "visitedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<MemoryResponse> responses = memoryService.getMemoriesByTrip(tripId, userId, pageable);
        return ApiResponse.success(responses);
    }

    // -------------------------------------------------------------------------
    // PATCH /api/v1/memories/{id}
    // -------------------------------------------------------------------------

    /**
     * Cập nhật một Memory (partial update). Yêu cầu ownership.
     *
     * @param id      UUID của memory
     * @param userId  UUID của user đang request (tạm dùng header)
     * @param request body chứa fields cần cập nhật (null = giữ nguyên)
     * @return 200 OK với memory sau khi cập nhật
     */
    @PatchMapping("/{id}")
    public ApiResponse<MemoryResponse> updateMemory(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody UpdateMemoryRequest request
    ) {
        MemoryResponse response = memoryService.updateMemory(id, request, userId);
        return ApiResponse.success("Memory updated successfully", response);
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/memories/{id}
    // -------------------------------------------------------------------------

    /**
     * Xoá mềm một Memory. Yêu cầu ownership.
     *
     * @param id     UUID của memory
     * @param userId UUID của user đang request (tạm dùng header)
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMemory(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        memoryService.deleteMemory(id, userId);
    }
}
