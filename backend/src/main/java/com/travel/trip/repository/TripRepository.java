package com.travel.trip.repository;

import com.travel.trip.entity.Trip;
import com.travel.trip.enums.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {

    List<Trip> findByUserIdAndDeletedAtIsNull(UUID userId);

    Optional<Trip> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    Optional<Trip> findByIdAndDeletedAtIsNull(UUID id);

    List<Trip> findByUserIdAndStatusAndDeletedAtIsNull(UUID userId, TripStatus status);

    List<Trip> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDescIdAsc(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trip t where t.id = :id and t.user.id = :userId and t.deletedAt is null")
    Optional<Trip> lockOwnedTrip(UUID id, UUID userId);

    @Query(value = """
            select t.id as tripId,
              (select count(*) from trip_places p where p.trip_id = t.id) as placeCount,
              (select count(*) from trip_places p where p.trip_id = t.id and p.visited) as visitedCount,
              (select count(*) from memories m where m.trip_id = t.id and m.user_id = t.user_id
                and m.deleted_at is null) as memoryCount
            from trips t where t.id in (:ids)
            """, nativeQuery = true)
    List<TripCounts> countResources(Collection<UUID> ids);

    interface TripCounts {
        UUID getTripId();
        long getPlaceCount();
        long getVisitedCount();
        long getMemoryCount();
    }
}
