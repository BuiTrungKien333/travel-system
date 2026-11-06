package com.travel.itinerary.repository;

import com.travel.itinerary.entity.Itinerary;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, UUID> {

    @EntityGraph(attributePaths = {"trip", "items", "items.place", "items.place.province"})
    Optional<Itinerary> findFirstByTripIdOrderByUpdatedAtDescIdAsc(UUID tripId);
}
