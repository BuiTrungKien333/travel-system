package com.travel.itinerary.repository;

import com.travel.itinerary.entity.Itinerary;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, UUID> {
    @EntityGraph(attributePaths = { "trip", "items", "items.place", "items.place.province" })
    Optional<Itinerary> findFirstByTripIdOrderByUpdatedAtDescIdAsc(UUID tripId);
}
