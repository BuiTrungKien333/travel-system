package com.travel.trip.repository;

import com.travel.trip.entity.TripPlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripPlaceRepository extends JpaRepository<TripPlace, UUID> {

    List<TripPlace> findByTripIdOrderByPlannedOrderAsc(UUID tripId);

    List<TripPlace> findByTripId(UUID tripId);

    Optional<TripPlace> findByTripIdAndPlaceId(UUID tripId, UUID placeId);

    boolean existsByTripIdAndPlaceId(UUID tripId, UUID placeId);

    void deleteByTripIdAndPlaceId(UUID tripId, UUID placeId);
}
