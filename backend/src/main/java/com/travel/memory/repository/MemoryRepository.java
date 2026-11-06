package com.travel.memory.repository;

import com.travel.memory.entity.Memory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MemoryRepository extends JpaRepository<Memory, UUID> {

    /** Retrieve an active memory by its ID. */
    @EntityGraph(attributePaths = {"media"})
    Optional<Memory> findByIdAndDeletedAtIsNull(UUID id);

    /** Retrieve paginated active memories of a specific user. */
    @EntityGraph(attributePaths = {"media"})
    Page<Memory> findByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    /** Retrieve paginated active memories of a specific trip. */
    @EntityGraph(attributePaths = {"media"})
    Page<Memory> findByTripIdAndDeletedAtIsNull(UUID tripId, Pageable pageable);

    /** Retrieve paginated active memories of a specific user within a specific trip. */
    @EntityGraph(attributePaths = {"media"})
    Page<Memory> findByTripIdAndUserIdAndDeletedAtIsNull(UUID tripId, UUID userId, Pageable pageable);
}
