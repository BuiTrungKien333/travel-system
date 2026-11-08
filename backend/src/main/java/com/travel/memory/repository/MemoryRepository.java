package com.travel.memory.repository;

import com.travel.memory.entity.Memory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface MemoryRepository extends JpaRepository<Memory, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Memory m where m.id=:id and m.deletedAt is null")
    Optional<Memory> lockActive(UUID id);

    /** Retrieve an active memory by its ID. */
    @EntityGraph(attributePaths = {"media"})
    Optional<Memory> findByIdAndDeletedAtIsNull(UUID id);

    /** Retrieve paginated active memories of a specific user. */
    Page<Memory> findByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    /** Retrieve paginated active memories of a specific trip. */
    Page<Memory> findByTripIdAndDeletedAtIsNull(UUID tripId, Pageable pageable);

    /** Retrieve paginated active memories of a specific user within a specific trip. */
    Page<Memory> findByTripIdAndUserIdAndDeletedAtIsNull(UUID tripId, UUID userId, Pageable pageable);

    interface Reference { UUID getUserId(); UUID getTripId(); }
    @Query("select m.userId as userId, m.tripId as tripId from Memory m where m.id=:id and m.deletedAt is null")
    Optional<Reference> reference(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Memory m where m.id=:id and m.userId=:owner and m.deletedAt is null")
    Optional<Memory> lockActive(@Param("id") UUID id, @Param("owner") UUID owner);

    @EntityGraph(attributePaths = "media")
    Optional<Memory> findByUserIdAndClientRequestId(UUID userId, UUID clientRequestId);

    @Modifying
    @Query(value = """
        INSERT INTO memories(id,user_id,trip_id,place_id,note,experienced_at,client_request_id,create_request_hash)
        VALUES (:id,:owner,:trip,:place,:note,:experienced,:request,:hash)
        ON CONFLICT (user_id,client_request_id) WHERE client_request_id IS NOT NULL DO NOTHING
        """, nativeQuery = true)
    int insertIdempotent(@Param("id") UUID id, @Param("owner") UUID owner, @Param("trip") UUID trip,
                         @Param("place") UUID place, @Param("note") String note, @Param("experienced") Instant experienced,
                         @Param("request") UUID request, @Param("hash") String hash);
    @Query("select m.id from Memory m where m.userId=:userId and m.deletedAt is null and (:tripId is null or m.tripId=:tripId)")
    Page<UUID> ownerIds(UUID userId, UUID tripId, Pageable pageable);

    @EntityGraph(attributePaths = {"media"})
    List<Memory> findByIdInAndUserIdAndDeletedAtIsNull(List<UUID> ids, UUID userId);

    @Query("""
        select m.id from Memory m where m.tripId = :tripId and m.userId = :userId
        and m.deletedAt is null and m.experiencedAt >= :from and m.experiencedAt < :until
        """)
    Page<UUID> dayIds(UUID tripId, UUID userId, Instant from, Instant until, Pageable pageable);

    @EntityGraph(attributePaths = {"media"})
    List<Memory> findByIdInAndTripIdAndUserIdAndDeletedAtIsNull(List<UUID> ids, UUID tripId, UUID userId);

}