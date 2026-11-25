package com.travel.memory.repository;

import com.travel.memory.entity.Memory;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface MemoryRepository extends JpaRepository<Memory, UUID> {
    Optional<Memory> findByUserIdAndClientRequestId(UUID userId, UUID clientRequestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Memory m where m.id=:id and m.deletedAt is null")
    Optional<Memory> lockActive(UUID id);

    @Query(
        "select m.id from Memory m where m.userId=:userId and m.deletedAt is null and (:tripId is null or m.tripId=:tripId)"
    )
    Page<UUID> ownerIds(UUID userId, UUID tripId, Pageable pageable);

    @EntityGraph(attributePaths = { "media" })
    List<Memory> findByIdInAndUserIdAndDeletedAtIsNull(List<UUID> ids, UUID userId);

    /** Retrieve an active memory by its ID. */
    @EntityGraph(attributePaths = { "media" })
    Optional<Memory> findByIdAndDeletedAtIsNull(UUID id);

    @Query(
        """
        select m.id from Memory m where m.tripId = :tripId and m.userId = :userId
        and m.deletedAt is null and m.experiencedAt >= :from and m.experiencedAt < :until
        """
    )
    Page<UUID> dayIds(UUID tripId, UUID userId, Instant from, Instant until, Pageable pageable);

    @EntityGraph(attributePaths = { "media" })
    List<Memory> findByIdInAndTripIdAndUserIdAndDeletedAtIsNull(List<UUID> ids, UUID tripId, UUID userId);
}
