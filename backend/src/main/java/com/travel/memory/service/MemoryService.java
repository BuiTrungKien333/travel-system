package com.travel.memory.service;

import com.travel.memory.dto.request.CreateMemoryRequest;
import com.travel.memory.dto.request.UpdateMemoryRequest;
import com.travel.memory.dto.response.MemoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MemoryService {

    /**
     * Tạo mới một Memory.
     *
     * @param request dữ liệu tạo memory
     * @return memory vừa được tạo
     */
    MemoryResponse createMemory(CreateMemoryRequest request);

    /**
     * Lấy một Memory theo id. Kiểm tra ownership.
     *
     * @param id            id của memory
     * @param requestUserId userId của người đang request
     * @return memory tương ứng
     */
    MemoryResponse getMemoryById(UUID id, UUID requestUserId);

    /**
     * Lấy danh sách Memory của một user (phân trang).
     *
     * @param userId   userId cần lấy memories
     * @param pageable thông số phân trang
     * @return page memories
     */
    Page<MemoryResponse> getMemoriesByUser(UUID userId, Pageable pageable);

    /**
     * Lấy danh sách Memory của một trip. Kiểm tra ownership qua userId.
     *
     * @param tripId        tripId cần lấy memories
     * @param requestUserId userId của người đang request
     * @param pageable      thông số phân trang
     * @return page memories
     */
    Page<MemoryResponse> getMemoriesByTrip(UUID tripId, UUID requestUserId, Pageable pageable);

    /**
     * Cập nhật một Memory (partial update). Kiểm tra ownership.
     *
     * @param id            id của memory
     * @param request       dữ liệu cập nhật
     * @param requestUserId userId của người đang request
     * @return memory sau khi cập nhật
     */
    MemoryResponse updateMemory(UUID id, UpdateMemoryRequest request, UUID requestUserId);

    /**
     * Xoá mềm một Memory. Kiểm tra ownership.
     *
     * @param id            id của memory
     * @param requestUserId userId của người đang request
     */
    void deleteMemory(UUID id, UUID requestUserId);
}
