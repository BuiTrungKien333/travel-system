package com.travel.memory.repository;

import com.travel.memory.entity.Memory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
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

    @Query("""
        select m.id from Memory m where m.tripId = :tripId and m.userId = :userId
        and m.deletedAt is null and m.experiencedAt >= :from and m.experiencedAt < :until
        """)
    Page<UUID> dayIds(UUID tripId, UUID userId, Instant from, Instant until, Pageable pageable);

    @EntityGraph(attributePaths = {"media"})
    List<Memory> findByIdInAndTripIdAndUserIdAndDeletedAtIsNull(List<UUID> ids, UUID tripId, UUID userId);
}
