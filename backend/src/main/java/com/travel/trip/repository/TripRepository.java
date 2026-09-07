package com.travel.trip.repository;

import com.travel.trip.entity.Trip;
import com.travel.trip.enums.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {

    List<Trip> findByUserIdAndDeletedAtIsNull(UUID userId);

    Optional<Trip> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    Optional<Trip> findByIdAndDeletedAtIsNull(UUID id);

    List<Trip> findByUserIdAndStatusAndDeletedAtIsNull(UUID userId, TripStatus status);
}
