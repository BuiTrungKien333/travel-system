package com.travel.trip.repository;

import com.travel.trip.entity.TripPlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripPlaceRepository extends JpaRepository<TripPlace, UUID> {

    List<TripPlace> findByTripId(UUID tripId);

    Optional<TripPlace> findByTripIdAndPlaceId(UUID tripId, UUID placeId);

    boolean existsByTripIdAndPlaceId(UUID tripId, UUID placeId);

    @EntityGraph(attributePaths = {"place", "place.province"})
    List<TripPlace> findByTripIdOrderByPlannedOrderAscCreatedAtAscIdAsc(UUID tripId);

    @EntityGraph(attributePaths = {"place", "place.province"})
    List<TripPlace> findByTripIdAndPlaceIdIn(UUID tripId, Collection<UUID> placeIds);

}
