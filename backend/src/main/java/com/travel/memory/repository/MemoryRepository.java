package com.travel.memory.repository;

import com.travel.memory.entity.Memory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface MemoryRepository extends JpaRepository<Memory, UUID> {
    Optional<Memory> findByUserIdAndClientRequestId(UUID userId, UUID clientRequestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Memory m where m.id = :id and m.deletedAt is null")
    Optional<Memory> lockActiveById(UUID id);

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
