package com.travel.memory.repository;

import com.travel.memory.entity.Memory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MemoryRepository extends JpaRepository<Memory, UUID> {

    /** Lấy tất cả memories theo id, chỉ trả về nếu chưa bị xoá. */
    Optional<Memory> findByIdAndDeletedAtIsNull(UUID id);

    /** Lấy tất cả memories của một user (phân trang). */
    Page<Memory> findByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    /** Lấy tất cả memories của một trip (phân trang). */
    Page<Memory> findByTripIdAndDeletedAtIsNull(UUID tripId, Pageable pageable);

    /** Lấy memories của một user trong một trip cụ thể (phân trang). */
    Page<Memory> findByTripIdAndUserIdAndDeletedAtIsNull(UUID tripId, UUID userId, Pageable pageable);
}
